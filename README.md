# Tanks QA Automation Framework

A Java test automation framework for Unity's **Tanks!** sample game.
Built to show that game testing follows the same principles as web or mobile automation.

> "Game testing is just QA. The application happens to be a game."

---

## What This Framework Does

It connects to a running Unity game over TCP, sends commands, and asserts on the responses.
No third-party automation SDK is needed on either side — just a socket and plain strings.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Game Engine | Unity 6 LTS |
| Game | Unity Tanks! (official sample) |
| Driver | Custom TestBridge (TCP port 13000) |
| Language | Java 11 |
| Test Framework | JUnit 5 |
| Build Tool | Maven 3.9 |
| Logging | SLF4J + Logback |

---

## Project Structure

```
src/test/java/com/tanksqa/
│
├── config/
│   └── Config.java              Reads settings from config.properties
│
├── driver/
│   └── BridgeClient.java        Sends TCP commands to Unity, returns responses
│
├── base/
│   └── BaseTest.java            Connects before each test, disconnects after
│
├── pages/
│   └── TankPage.java            Page Object — the only class tests call
│
├── utils/
│   └── WaitUtils.java           Polling helpers (wait for tank, wait for health drop)
│
└── tests/
    └── TanksSmokeTest.java      The actual test suite

src/test/resources/
├── config.properties            Host, port, and timeout settings
└── logback-test.xml             Log format and level

Unity project:
Assets/_Tanks/Scripts/Tests/
└── TestBridge.cs                TCP server that runs inside Unity
```

---

## How the Layers Connect

```
TanksSmokeTest
    │  calls
    ▼
TankPage              ← your tests only talk to this layer
    │  calls
    ▼
BridgeClient          ← handles the TCP protocol
    │  sends over TCP
    ▼
TestBridge.cs         ← runs inside Unity, executes Unity API, sends back response
```

**Rule:** tests call `TankPage`. `TankPage` calls `BridgeClient`. Nothing else.
This means if a command string changes, you fix it in `BridgeClient` — not in every test.

---

## How the TestBridge Works

### The threading problem

Unity's API (`FindObjectsOfType`, `transform.position`, etc.) can only be called from the **main thread**. But the TCP listener runs on a background thread. Calling Unity API from the background thread causes this error:

```
FindGameObjectWithTag can only be called from the main thread.
```

### The fix: a queue between threads

```
Background thread          Main thread (Unity's Update loop)
─────────────────          ─────────────────────────────────
Accept TCP connection  →   Dequeue command
Read command string    →   Call Unity API (safe here)
Enqueue command        →   Write response back to client
                           Close connection
```

The background thread only touches bytes. The main thread does all Unity work.

### Why connection-per-command

Unity closes the connection after writing each response. The original design kept one socket open — the second command in a wait loop hit a closed stream and threw an exception immediately, breaking the polling.

The fix: `BridgeClient.sendCommand()` opens a fresh socket for every call. One command, one socket, one response, close.

### Why tanks are found by component, not by tag

`FindGameObjectsWithTag("Player")` returned nothing because the tanks weren't tagged that way. Instead, `TestBridge.cs` scans for `MonoBehaviour` components whose type name is `"TankHealth"` — any object that has that component is a tank, regardless of its tag.

---

## Supported Commands

| Command | Response |
|---|---|
| `PING` | `PONG` |
| `FIND_TANK` | `FOUND:{name}` or `NOT_FOUND` |
| `FIND_ALL_TANKS` | `TANKS:{n1},{n2},...` or `NOT_FOUND` |
| `MOVE_TANK:{name}:{x},{y},{z}` | `MOVED:{name}` or `NOT_FOUND` |
| `AIM_AT:{shooter}:{target}` | `AIMED` or `NOT_FOUND` |
| `SHOOT:{name}` | `SHOT:{name}` or `NOT_FOUND` |
| `GET_HEALTH:{name}` | `HEALTH:{value}` or `NOT_FOUND` |

---

## Running the Tests

### Prerequisites
- Java 11+
- Maven 3.9+
- Unity Tanks! project with `TestBridge.cs` attached to a scene GameObject

### Setup
```bash
git clone https://github.com/klincarovt/unity-tanks-qa-automation.git
cd tanks-qa-framework
mvn install
```

### Run

```bash
# Quick check — game must be running, any screen
mvn test -Dgroups=bridge

# Full suite — start a round first, then run
mvn test -Dgroups=gameplay

# Everything
mvn test
```

### Test tags

| Tag | Needs |
|---|---|
| `bridge` | Game running, any screen |
| `gameplay` | Round in progress, tanks spawned |

---

## How to Add a New Command

Follow this checklist every time you extend the protocol.

**Example: add `GET_POSITION:{name}` → `POSITION:{x},{y},{z}`**

### Step 1 — Unity: add the command to `TestBridge.cs`

Add a new `if` block inside `HandleCommand`. This runs on Unity's main thread, so Unity API is safe.

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

### Step 2 — Java: add the method to `BridgeClient.java`

One method per command. Parse the response and return a sensible Java type.

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

### Step 3 — Java: expose it in `TankPage.java`

Tests call `TankPage`, not `BridgeClient` directly.

```java
public float[] getPosition(String name) throws IOException {
    return client.getPosition(name);
}
```

### Step 4 — Write the test in `TanksSmokeTest.java`

```java
@Tag("gameplay")
@Test
@DisplayName("Tank moves to the correct position")
public void testTankMovesToPosition() throws Exception {
    String[] tanks = tankPage.findAllTanks();
    assertNotNull(tanks);

    tankPage.moveTank(tanks[0], 10f, 0f, 10f);
    float[] pos = tankPage.getPosition(tanks[0]);

    assertNotNull(pos);
    assertEquals(10f, pos[0], 0.1f, "X position should be ~10");
}
```

### Step 5 — Add a `waitFor` to `WaitUtils.java` only if you need polling

Polling is only needed when the game takes time to reach a state — health dropping after a shot, a tank spawning after a scene load. For instant reads like position, you do not need a wait helper.

```java
// Only add this if getPosition result changes asynchronously and you need to wait for it
public float[] waitForPositionNear(String tankName, float targetX, float targetZ) {
    long deadline = System.currentTimeMillis() + timeoutMs;
    while (System.currentTimeMillis() < deadline) {
        try {
            float[] pos = client.getPosition(tankName);
            if (pos != null && Math.abs(pos[0] - targetX) < 0.5f) return pos;
            Thread.sleep(intervalMs);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    return null;
}
```

---

## Key Concepts

| Concept | What it means here |
|---|---|
| Page Object Model | `TankPage` wraps every game action — tests never form raw command strings |
| Driver | `BridgeClient` is the WebDriver equivalent — it handles the protocol |
| Wait / polling | `WaitUtils` replaces `WebDriverWait` — game state changes asynchronously |
| Separation of concerns | Each class has one job; changing the protocol touches `BridgeClient` only |
| Config over hardcoding | All settings live in `config.properties`; no recompile needed to change them |

---

## Related Repository

Unity Tanks! game project:
`https://github.com/klincarovt/unity-tanks-qa-automation`
