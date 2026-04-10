package com.tanksqa.driver;

import com.tanksqa.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Locale;

/**
 * Sends commands to the Unity TestBridge over TCP and returns the responses.
 *
 * Unity closes the connection after every response, so each sendCommand()
 * call opens a fresh socket, sends one command, reads one response, then closes.
 *
 * This is the lowest layer of the framework — only TankPage should call it.
 * Tests never use this class directly.
 */
public class BridgeClient {

    private static final Logger log = LoggerFactory.getLogger(BridgeClient.class);

    private final String host;
    private final int    port;

    public BridgeClient() {
        this.host = Config.bridgeHost();
        this.port = Config.bridgePort();
    }

    /**
     * Opens a socket to check the game is running before the test starts.
     * If the game is not running this throws immediately — you get a clear
     * ERROR in the report rather than a confusing FAILURE later.
     */
    public void connect() throws IOException {
        log.info("Connecting to TestBridge at {}:{}", host, port);
        try (Socket probe = new Socket(host, port)) {
            // probe opens, confirms the port is listening, then closes immediately
        }
    }

    /** No socket to close — kept so BaseTest teardown compiles cleanly. */
    public void disconnect() { }

    /**
     * Sends one command and returns one response line from Unity.
     * Every call opens and closes its own socket.
     */
    public String sendCommand(String command) throws IOException {
        log.debug("--> {}", command);
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in  = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()))) {
            out.println(command);
            String response = in.readLine();
            log.debug("<-- {}", response);
            return response;
        }
    }

    // ------------------------------------------------------------------
    // One method per supported command
    // ------------------------------------------------------------------

    public boolean ping() throws IOException {
        return "PONG".equals(sendCommand("PING"));
    }

    /** @return first tank name in the scene, or null if none found */
    public String findTank() throws IOException {
        String r = sendCommand("FIND_TANK");
        return r != null && r.startsWith("FOUND:") ? r.substring(6) : null;
    }

    /** @return names of all tanks in the scene, or null if none found */
    public String[] findAllTanks() throws IOException {
        String r = sendCommand("FIND_ALL_TANKS");
        return r != null && r.startsWith("TANKS:") ? r.substring(6).split(",") : null;
    }

    /**
     * Teleports a tank to a world-space position.
     * Locale.US ensures the decimal point is always '.' regardless of the OS locale.
     */
    public boolean moveTank(String name, float x, float y, float z) throws IOException {
        String cmd = String.format(Locale.US, "MOVE_TANK:%s:%.3f,%.3f,%.3f", name, x, y, z);
        String r = sendCommand(cmd);
        return r != null && r.startsWith("MOVED:");
    }

    /** Rotates the shooter tank to face the target tank. */
    public boolean aimAt(String shooter, String target) throws IOException {
        return "AIMED".equals(sendCommand("AIM_AT:" + shooter + ":" + target));
    }

    /** Fires the weapon on the named tank. */
    public boolean shoot(String name) throws IOException {
        String r = sendCommand("SHOOT:" + name);
        return r != null && r.startsWith("SHOT:");
    }

    /**
     * Reads the current health of the named tank.
     *
     * @return health as a Float, or null if the tank was not found
     */
    public Float getHealth(String name) throws IOException {
        String r = sendCommand("GET_HEALTH:" + name);
        if (r != null && r.startsWith("HEALTH:")) {
            return Float.parseFloat(r.substring(7));
        }
        return null;
    }
}
