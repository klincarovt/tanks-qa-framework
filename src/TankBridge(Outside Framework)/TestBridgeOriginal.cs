using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Globalization;
using System.Net;
using System.Net.Sockets;
using System.Reflection;
using System.Text;
using System.Threading;
using UnityEngine;

// ============================================================
//  TestBridge
//
//  Attach this script to any GameObject in your Unity scene.
//  It opens a TCP server on port 13000 and waits for commands
//  from the Java test framework.
//
//  How it works (simplified):
//
//    Java test                    Unity
//    ─────────                    ──────
//    send "PING"   ──TCP──>   receive "PING"
//                             process it
//                 <──TCP──   send "PONG"
//    connection closed
//
//  One command in → one response out → connection closes.
// ============================================================
public class TestBridge : MonoBehaviour
{
    private const int PORT = 13000;

    private TcpListener _listener;
    private Thread      _thread;
    private bool        _running = false;

    // ============================================================
    //  THE THREADING PROBLEM AND ITS SOLUTION
    //
    //  The TCP listener runs on a background thread (not the main
    //  Unity thread). But Unity's API — things like finding objects
    //  or moving them — can ONLY be called from the main thread.
    //
    //  If you call FindObjectsOfType() from the background thread,
    //  Unity throws an error and crashes.
    //
    //  Solution: use a queue as a hand-off point.
    //
    //  Background thread:
    //    1. Accepts the TCP connection
    //    2. Reads the command string
    //    3. Puts it in the queue (thread-safe)
    //    4. Goes back to waiting for the next connection
    //
    //  Main thread (Update runs every frame):
    //    1. Checks if anything is in the queue
    //    2. Takes it out
    //    3. Calls Unity API safely
    //    4. Writes the response back to the client
    //
    //  ConcurrentQueue is a queue that is safe to use from
    //  multiple threads at the same time.
    // ============================================================
    private readonly ConcurrentQueue<PendingCommand> _queue
        = new ConcurrentQueue<PendingCommand>();

    // Holds everything we need to process one command and reply
    private class PendingCommand
    {
        public string        Command;
        public NetworkStream Stream;  // used to write the response back
        public TcpClient     Client;  // used to close the connection after replying
    }

    // ============================================================
    //  Unity lifecycle
    // ============================================================

    void Start()
    {
        _running = true;
        _thread = new Thread(ListenLoop) { IsBackground = true };
        _thread.Start();
        Debug.Log("[TestBridge] Listening on port " + PORT);
    }

    // Update() is called by Unity on the main thread every frame.
    // This is where it is safe to call Unity API.
    void Update()
    {
        // Process every command that arrived since the last frame
        while (_queue.TryDequeue(out var pending))
        {
            string response = HandleCommand(pending.Command);

            // Send the response back, then close the connection
            byte[] bytes = Encoding.UTF8.GetBytes(response + "\n");
            try
            {
                pending.Stream.Write(bytes, 0, bytes.Length);
            }
            catch (Exception e)
            {
                Debug.LogError("[TestBridge] Could not send response: " + e.Message);
            }
            finally
            {
                pending.Client.Close();
            }
        }
    }

    void OnDestroy()
    {
        _running = false;
        _listener?.Stop();
    }

    // ============================================================
    //  Background thread — only reads bytes, never touches Unity API
    // ============================================================

    private void ListenLoop()
    {
        _listener = new TcpListener(IPAddress.Loopback, PORT);
        _listener.Start();

        while (_running)
        {
            try
            {
                // Wait here until a Java test connects
                var client = _listener.AcceptTcpClient();
                var stream = client.GetStream();

                // Read the command bytes and convert to a string
                byte[] buffer = new byte[1024];
                int bytesRead = stream.Read(buffer, 0, buffer.Length);
                string command = Encoding.UTF8.GetString(buffer, 0, bytesRead).Trim();

                Debug.Log("[TestBridge] Received: " + command);

                // Hand off to the main thread via the queue
                _queue.Enqueue(new PendingCommand
                {
                    Command = command,
                    Stream  = stream,
                    Client  = client
                });
            }
            catch (Exception e)
            {
                if (_running) Debug.LogError("[TestBridge] Error: " + e.Message);
            }
        }
    }

    // ============================================================
    //  Command handlers — runs on main thread, Unity API is safe here
    //
    //  To add a new command:
    //    1. Add a new "if" block below following the same pattern
    //    2. Add the matching method to BridgeClient.java in the framework
    //    3. Expose it through TankPage.java
    //    4. Write your test
    // ============================================================

    private string HandleCommand(string command)
    {
        // ── PING ─────────────────────────────────────────────────
        if (command == "PING")
            return "PONG";

        // ── FIND_TANK ─────────────────────────────────────────────
        if (command == "FIND_TANK")
        {
            var tank = FindFirstTank();
            return tank != null ? "FOUND:" + tank.name : "NOT_FOUND";
        }

        // ── FIND_ALL_TANKS ────────────────────────────────────────
        if (command == "FIND_ALL_TANKS")
        {
            var tanks = FindAllTanks();
            if (tanks.Count == 0) return "NOT_FOUND";
            return "TANKS:" + string.Join(",", tanks.ConvertAll(t => t.name));
        }

        // ── MOVE_TANK:{name}:{x},{y},{z} ──────────────────────────
        if (command.StartsWith("MOVE_TANK:"))
        {
            // Parse the command string into its parts
            // Example: "MOVE_TANK:Tank1:1.000,0.000,5.000"
            var rest   = command.Substring("MOVE_TANK:".Length); // "Tank1:1.000,0.000,5.000"
            int colon  = rest.IndexOf(':');
            var name   = rest.Substring(0, colon);               // "Tank1"
            var coords = rest.Substring(colon + 1).Split(',');   // ["1.000", "0.000", "5.000"]

            var tank = FindTankByName(name);
            if (tank == null) return "NOT_FOUND";

            // InvariantCulture ensures "1.5" is always read as one-point-five,
            // not one-comma-five (which some European locales use)
            float x = float.Parse(coords[0], CultureInfo.InvariantCulture);
            float y = float.Parse(coords[1], CultureInfo.InvariantCulture);
            float z = float.Parse(coords[2], CultureInfo.InvariantCulture);

            tank.transform.position = new Vector3(x, y, z);
            return "MOVED:" + name;
        }

        // ── AIM_AT:{shooter}:{target} ─────────────────────────────
        if (command.StartsWith("AIM_AT:"))
        {
            // Parse: "AIM_AT:Tank1:Tank2"
            var rest    = command.Substring("AIM_AT:".Length);
            int colon   = rest.IndexOf(':');
            var shooter = FindTankByName(rest.Substring(0, colon));
            var target  = FindTankByName(rest.Substring(colon + 1));

            if (shooter == null || target == null) return "NOT_FOUND";

            // LookAt rotates the shooter so its forward direction points at the target
            shooter.transform.LookAt(target.transform);
            return "AIMED";
        }

        // ── SHOOT:{name} ──────────────────────────────────────────
        if (command.StartsWith("SHOOT:"))
        {
            var tank     = FindTankByName(command.Substring("SHOOT:".Length));
            var shooting = GetComponentByName(tank, "TankShooting");
            if (tank == null || shooting == null) return "NOT_FOUND";

            // TankShooting.Fire() is a private method — we cannot call it normally
            // from outside the class. Reflection is a way to access private
            // members at runtime. It is not ideal, but it avoids modifying
            // the original game scripts.
            var flags = BindingFlags.Instance | BindingFlags.NonPublic | BindingFlags.Public;

            // Reset the "already fired" flag so the tank is allowed to fire again
            shooting.GetType()
                    .GetField("m_Fired", flags)
                    ?.SetValue(shooting, false);

            // Set the launch force to 60% of max.
            // Without this, m_CurrentLaunchForce might be 0 and the shell
            // would drop straight down instead of flying toward the target.
            var maxField     = shooting.GetType().GetField("m_MaxLaunchForce", flags);
            var currentField = shooting.GetType().GetField("m_CurrentLaunchForce", flags);
            float maxForce   = maxField != null ? (float)maxField.GetValue(shooting) : 30f;
            currentField?.SetValue(shooting, maxForce * 0.6f);

            // Call the private Fire() method
            shooting.GetType()
                    .GetMethod("Fire", flags)
                    ?.Invoke(shooting, null);

            return "SHOT:" + tank.name;
        }

        // ── GET_HEALTH:{name} ─────────────────────────────────────
        if (command.StartsWith("GET_HEALTH:"))
        {
            var tank   = FindTankByName(command.Substring("GET_HEALTH:".Length));
            var health = GetComponentByName(tank, "TankHealth");
            if (tank == null || health == null) return "NOT_FOUND";

            // m_CurrentHealth is a public float on TankHealth, but we still use
            // GetField so we do not need to reference the TankHealth type directly
            // (it lives in a different assembly)
            var field = health.GetType().GetField("m_CurrentHealth",
                BindingFlags.Instance | BindingFlags.Public | BindingFlags.NonPublic);

            if (field == null) return "NOT_FOUND";

            float value = (float)field.GetValue(health);
            return "HEALTH:" + value.ToString(CultureInfo.InvariantCulture);
        }

        return "UNKNOWN_COMMAND";
    }

    // ============================================================
    //  Tank lookup helpers
    //
    //  We find tanks by looking for the TankHealth component rather
    //  than by tag. This works regardless of what tag the tanks have
    //  in the scene — the tag kept returning NOT_FOUND in testing.
    // ============================================================

    // Returns the first GameObject that has a TankHealth component
    private GameObject FindFirstTank()
    {
        foreach (var mb in FindObjectsOfType<MonoBehaviour>())
            if (mb.GetType().Name == "TankHealth")
                return mb.gameObject;
        return null;
    }

    // Returns all GameObjects that have a TankHealth component
    private List<GameObject> FindAllTanks()
    {
        var result = new List<GameObject>();
        foreach (var mb in FindObjectsOfType<MonoBehaviour>())
        {
            // Avoid adding the same GameObject twice
            if (mb.GetType().Name == "TankHealth" && !result.Contains(mb.gameObject))
                result.Add(mb.gameObject);
        }
        return result;
    }

    // Returns the tank with the given name, or null if not found
    private GameObject FindTankByName(string name)
    {
        foreach (var mb in FindObjectsOfType<MonoBehaviour>())
            if (mb.GetType().Name == "TankHealth" && mb.gameObject.name == name)
                return mb.gameObject;
        return null;
    }

    // Returns a component on a GameObject looked up by its type name as a string.
    // Used because TankShooting and TankHealth are in a different assembly —
    // we cannot write GetComponent<TankShooting>() from here.
    private MonoBehaviour GetComponentByName(GameObject go, string typeName)
    {
        if (go == null) return null;
        foreach (var comp in go.GetComponents<MonoBehaviour>())
            if (comp.GetType().Name == typeName) return comp;
        return null;
    }
}
