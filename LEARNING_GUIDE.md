# Learning Guide — Understanding This Framework

This guide walks you through the framework from scratch.
Each step builds on the last. By the end you will be able to read any test, understand why it works, and add your own.

---

## Before You Start

You do not need to know all of Java or C# to follow this. You need:
- A basic understanding of what a class and a method are
- Knowing how to run `mvn test` in a terminal
- Unity open with the Tanks game and `TestBridge.cs` attached to a GameObject

---

## Step 1 — Understand What You Are Testing

Open Unity and press Play. You will see the Tanks game. Your job as a QA engineer is to write automated tests that verify the game behaves correctly — without clicking anything yourself.

The challenge: the tests are Java programs. The game is a Unity C# program. They are two separate things. The framework solves the problem of getting them to communicate.

---

## Step 2 — Understand the TestBridge (Unity side)

Read [`TESTBRIDGE.md`](TESTBRIDGE.md) before going further.

The short version:
- `TestBridge.cs` runs inside Unity as a TCP server on port 13000
- It listens for string commands from the Java tests
- It executes them inside Unity (find a tank, move it, shoot, read health)
- It sends a string response back
- The connection closes after each command

This is the **only** file you need to touch on the Unity side when adding new functionality.

---

## Step 3 — Understand the Java Layer by Layer

Open the project in your IDE. The framework has four layers. Read them in this order.

### Layer 1: `Config.java`
**Location:** `config/Config.java`

This reads `config.properties` and gives the rest of the framework access to settings like the host address, port number, and timeout values.

**Why it exists:** if the port or host is hardcoded in five different places and you need to change it, you have to find all five. With `Config`, you change it in one properties file.

**Key thing to notice:** the `static` block at the top runs once when the class is first used. It loads the file. If the file is missing, it fails immediately with a clear error.

---

### Layer 2: `BridgeClient.java`
**Location:** `driver/BridgeClient.java`

This is the Java equivalent of a TV remote. It has one button per command. You press the button, it sends the command to Unity over TCP and gives you back the response.

```java
// How one command works inside sendCommand():
Socket socket = new Socket(host, port);   // open connection
out.println(command);                      // send the command
String response = in.readLine();           // read the response
socket.close();                            // close connection
return response;
```

Every named method (`ping()`, `findTank()`, `shoot()`, etc.) calls `sendCommand()` and parses the response into a useful Java type.

**Key thing to notice:** each call opens and closes its own connection. Unity closes the connection after each response — so we open a fresh one every time.

---

### Layer 3: `TankPage.java`
**Location:** `pages/TankPage.java`

This is the **Page Object**. It wraps `BridgeClient` and gives your tests readable method names.

Without it, a test would look like:
```java
String r = client.sendCommand("SHOOT:Tank1");
assertTrue(r != null && r.startsWith("SHOT:"));
```

With it, a test looks like:
```java
assertTrue(tankPage.shoot("Tank1"), "Shoot command failed");
```

`TankPage` is the **only class your tests should call**. If you ever find yourself calling `BridgeClient` directly from a test, move that call into `TankPage` first.

**Key thing to notice:** `TankPage` also holds a `WaitUtils` instance. When a test needs to wait for something (`waitForTank`, `waitForHealthBelow`), it goes through here.

---

### Layer 4: `WaitUtils.java`
**Location:** `utils/WaitUtils.java`

Game state changes asynchronously. A tank does not lose health the instant a shell is fired — there is travel time, explosion time, damage calculation time. `WaitUtils` keeps asking the game for a value until it changes or the timeout runs out.

Every method follows the same shape:

```java
long deadline = System.currentTimeMillis() + timeoutMs;
while (System.currentTimeMillis() < deadline) {
    // ask the game
    // if the condition is met, return the value
    // otherwise sleep 500ms and try again
}
// return null — condition never met in time
```

**Key thing to notice:** if the game takes more than 10 seconds (the default timeout), `waitForTank()` returns `null`. The test then fails with a clear assertion message. You can increase the timeout in `config.properties`.

---

### Layer 5: `BaseTest.java`
**Location:** `base/BaseTest.java`

Every test class extends `BaseTest`. It does two things automatically:

- `@BeforeEach` — creates a `BridgeClient` and connects to Unity before each test runs
- `@AfterEach` — disconnects after each test, whether it passed or failed

This means you never have to write setup/teardown in your own test classes.

---

## Step 4 — Read a Test

Open `TanksSmokeTest.java`. Read `testTankTakesDamageAfterBeingShot` — the most complete test in the suite.

Notice the numbered comments:

```java
// 1. Find both tanks
// 2. Move them 5 units apart
// 3. Rotate shooter to face target
// 4. Record health before the shot
// 5. Fire
// 6. Wait for the health to drop
```

Every test should tell a story. The reader should be able to understand what is being tested just by reading the comments and assertion messages — without needing to understand the framework internals.

---

## Step 5 — Write Your First New Command

The best way to learn is to add something yourself. Here is a guided exercise.

**Goal:** add a command that reads a tank's position from Unity.

**Command:** `GET_POSITION:Tank1` → `POSITION:0.0,0.0,5.0`

### In Unity — `TestBridge.cs`

Add this block inside `HandleCommand()`, before the final `return "UNKNOWN_COMMAND"`:

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

### In Java — `BridgeClient.java`

Add this method:

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

### In Java — `TankPage.java`

Add this method:

```java
public float[] getPosition(String name) throws IOException {
    return client.getPosition(name);
}
```

### In Java — `TanksSmokeTest.java`

Add this test:

```java
@Tag("gameplay")
@Test
@DisplayName("Tank moves to the correct position")
public void testTankMovesToPosition() throws Exception {
    String[] tanks = tankPage.findAllTanks();
    assertNotNull(tanks, "No tanks found — is a round running?");

    assertTrue(tankPage.moveTank(tanks[0], 10f, 0f, 10f), "Move failed");

    float[] pos = tankPage.getPosition(tanks[0]);
    assertNotNull(pos, "Could not read position");
    assertEquals(10f, pos[0], 0.1f, "X position should be ~10");
    assertEquals(10f, pos[2], 0.1f, "Z position should be ~10");
}
```

Run it:
```bash
mvn test -Dgroups=gameplay
```

---

## Step 6 — What to Study Next

Once you are comfortable with the framework, these are the concepts worth learning more about:

| Topic | Why it matters here |
|---|---|
| Java generics and collections | `String[]`, `List<>`, `Float` vs `float` |
| Java exceptions and `try-with-resources` | `sendCommand()` uses both |
| OOP — encapsulation | Why tests call `TankPage`, not `BridgeClient` directly |
| TCP / sockets | What is actually happening when `new Socket(host, port)` runs |
| JUnit 5 annotations | `@Test`, `@BeforeEach`, `@Tag`, `@DisplayName` |
| Threading | Why `ConcurrentQueue` exists in `TestBridge.cs` |
| Reflection (C#/Java) | Why we use it for `SHOOT` and `GET_HEALTH` |
| The Page Object Model | The same pattern used in Selenium, Appium, Playwright |

---

## Quick Reference — What Each File Does

| File | One sentence |
|---|---|
| `TestBridge.cs` | Runs inside Unity; receives commands and executes them |
| `Config.java` | Reads settings from `config.properties` |
| `BridgeClient.java` | Sends TCP commands to Unity and parses responses |
| `BaseTest.java` | Connects before each test, disconnects after |
| `TankPage.java` | The only class tests call; wraps all game actions |
| `WaitUtils.java` | Keeps polling until a game state condition is met |
| `TanksSmokeTest.java` | The actual tests |
| `config.properties` | Host, port, timeout values |

---

## Common Questions

**Q: Why does my test fail with `Connection refused`?**
The game is not running. Open Unity and press Play before running the tests.

**Q: Why does `testTankIsFoundInScene` time out?**
The game is on the player select screen, not in a round. Start a round, then run the test.

**Q: The shell fires but health never drops — why?**
The shell might be missing the target. Check that `AIM_AT` runs before `SHOOT`, and that the tanks are not too close or too far apart. 5 units is a reliable distance.

**Q: How do I increase the wait timeout?**
Open `src/test/resources/config.properties` and increase `wait.timeout.ms`.
