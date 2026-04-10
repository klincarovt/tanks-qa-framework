# TestBridge — How It Works

`TestBridge.cs` is a Unity script that lets the Java test framework control the game over a network connection. Attach it to any GameObject in your scene and press Play — it starts a TCP server on port 13000 and waits for commands.

---

## The Big Picture

```
Java Test (your PC)              Unity Game (same PC)
───────────────────              ────────────────────
send "FIND_TANK"   ──TCP──>   receive "FIND_TANK"
                               look for a tank
                               send back the name
                  <──TCP──   "FOUND:Tank1"
connection closed
```

One command goes in. One response comes out. The connection closes. That is the entire protocol.

---

## Why a TCP Server?

The test framework is Java. The game is Unity (C#). They are two separate programs running at the same time. TCP is a standard way for two programs to talk to each other over a network — even when they are on the same machine.

---

## The Threading Problem

This is the most important concept to understand in the whole file.

**The problem:**

The TCP server runs on a **background thread** — a separate worker that runs alongside Unity. Unity's API (finding objects, moving them, reading health) can **only** be called from Unity's **main thread**. If you call `FindObjectsOfType()` from the background thread, Unity crashes with:

```
FindGameObjectWithTag can only be called from the main thread.
```

**The solution — a queue:**

```
Background thread              Main thread (Update, runs every frame)
─────────────────              ──────────────────────────────────────
Accept TCP connection    →
Read command bytes       →
Put command in queue     →     Read command from queue
Go back to waiting             Call Unity API  ← safe here
                               Write response back
                               Close connection
```

The background thread never touches Unity. The main thread never touches TCP directly. The queue (`ConcurrentQueue`) is the hand-off point — it is safe to use from both threads at the same time.

---

## The Code Structure

```
TestBridge.cs
│
├── Start()          — starts the background TCP listener thread
│
├── Update()         — runs every frame on the main thread
│                      reads from the queue, calls HandleCommand,
│                      writes the response back
│
├── ListenLoop()     — runs on the background thread
│                      accepts connections, reads commands,
│                      puts them in the queue
│
├── HandleCommand()  — one "if" block per supported command
│                      this is where the game logic lives
│
└── Helpers
    ├── FindFirstTank()
    ├── FindAllTanks()
    ├── FindTankByName()
    └── GetComponentByName()
```

---

## How Commands Are Parsed

Commands are plain strings. A command with arguments uses `:` as a separator.

**Example: `MOVE_TANK:Tank1:1.000,0.000,5.000`**

```csharp
var rest   = command.Substring("MOVE_TANK:".Length);
// rest = "Tank1:1.000,0.000,5.000"

int colon  = rest.IndexOf(':');
var name   = rest.Substring(0, colon);
// name = "Tank1"

var coords = rest.Substring(colon + 1).Split(',');
// coords = ["1.000", "0.000", "5.000"]
```

Step by step:
1. Strip the command name from the front
2. Find the next `:` to split name from data
3. Split the remaining data by `,`

---

## Why We Use Reflection for SHOOT

`TankShooting.Fire()` is a `private` method in the original game scripts. Private means only that class can call it. Since `TestBridge` is a different class, we cannot write `tankShooting.Fire()` directly.

**Reflection** is a built-in C# feature that lets you access private methods and fields at runtime, bypassing the normal access rules. It is not something you use in everyday code, but it is the right tool here because it avoids modifying the original game scripts.

```csharp
// Normal (won't compile — Fire() is private):
shooting.Fire();

// With reflection (works at runtime):
var flags = BindingFlags.Instance | BindingFlags.NonPublic | BindingFlags.Public;
shooting.GetType().GetMethod("Fire", flags)?.Invoke(shooting, null);
```

We also need to set `m_CurrentLaunchForce` before calling Fire, because it starts at 0 and the shell would drop straight down with no force behind it.

---

## Why Tanks Are Found by Component, Not by Tag

The obvious approach is `GameObject.FindGameObjectsWithTag("Player")`. This failed because the tanks in the scene were not tagged `"Player"`.

Instead, we look for any `MonoBehaviour` whose type name is `"TankHealth"`. Any object with a `TankHealth` component is a tank — regardless of its tag or name in the scene.

```csharp
foreach (var mb in FindObjectsOfType<MonoBehaviour>())
    if (mb.GetType().Name == "TankHealth")
        return mb.gameObject;
```

We use the type name as a string (`"TankHealth"`) rather than the type itself (`TankHealth`) because `TestBridge.cs` is in a different assembly from the game scripts and cannot reference `TankHealth` directly.

---

## How to Add a New Command

Follow this pattern every time.

**Example: `GET_POSITION:Tank1` → `POSITION:0.0,0.0,5.0`**

### Step 1 — Add a handler block in `HandleCommand()`

```csharp
if (command.StartsWith("GET_POSITION:"))
{
    var tank = FindTankByName(command.Substring("GET_POSITION:".Length));
    if (tank == null) return "NOT_FOUND";

    var pos = tank.transform.position;
    return string.Format(CultureInfo.InvariantCulture,
        "POSITION:{0},{1},{2}", pos.x, pos.y, pos.z);
}
```

### Step 2 — Add the method to `BridgeClient.java`

```java
public float[] getPosition(String name) throws IOException {
    String r = sendCommand("GET_POSITION:" + name);
    if (r != null && r.startsWith("POSITION:")) {
        String[] parts = r.substring(9).split(",");
        return new float[]{
            Float.parseFloat(parts[0]),
            Float.parseFloat(parts[1]),
            Float.parseFloat(parts[2])
        };
    }
    return null;
}
```

### Step 3 — Expose it in `TankPage.java`

```java
public float[] getPosition(String name) throws IOException {
    return client.getPosition(name);
}
```

### Step 4 — Write the test

```java
@Test
@DisplayName("Tank moves to the correct position")
public void testTankMovesToPosition() throws Exception {
    String[] tanks = tankPage.findAllTanks();
    tankPage.moveTank(tanks[0], 10f, 0f, 10f);
    float[] pos = tankPage.getPosition(tanks[0]);
    assertNotNull(pos);
    assertEquals(10f, pos[0], 0.1f);
}
```

---

## Supported Commands Reference

| Command | Response | What it does |
|---|---|---|
| `PING` | `PONG` | Check the bridge is alive |
| `FIND_TANK` | `FOUND:{name}` or `NOT_FOUND` | Find the first tank in the scene |
| `FIND_ALL_TANKS` | `TANKS:{n1},{n2},...` or `NOT_FOUND` | Find all tanks |
| `MOVE_TANK:{name}:{x},{y},{z}` | `MOVED:{name}` or `NOT_FOUND` | Teleport a tank |
| `AIM_AT:{shooter}:{target}` | `AIMED` or `NOT_FOUND` | Rotate shooter to face target |
| `SHOOT:{name}` | `SHOT:{name}` or `NOT_FOUND` | Fire the tank's weapon |
| `GET_HEALTH:{name}` | `HEALTH:{value}` or `NOT_FOUND` | Read current health |
