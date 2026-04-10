package com.tanksqa.pages;

import com.tanksqa.driver.BridgeClient;
import com.tanksqa.utils.WaitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Page Object for the Tanks game.
 *
 * This is the only class your tests should call.
 * It turns raw protocol commands into readable method names.
 *
 * Example — instead of: client.sendCommand("SHOOT:Tank1")
 * Your test calls:       tankPage.shoot("Tank1")
 *
 * If you need to add a new game action:
 *   1. Add the command to TestBridge.cs in Unity
 *   2. Add the method to BridgeClient.java
 *   3. Add a method here that calls it
 *   4. Write your test
 */
public class TankPage {

    private static final Logger log = LoggerFactory.getLogger(TankPage.class);

    private final BridgeClient client;
    private final WaitUtils    waitUtils;

    public TankPage(BridgeClient client) {
        this.client    = client;
        this.waitUtils = new WaitUtils(client);
    }

    // ------------------------------------------------------------------
    // Connection
    // ------------------------------------------------------------------

    /** Returns true if the Unity TestBridge responds to a PING. */
    public boolean isBridgeAlive() throws IOException {
        return client.ping();
    }

    // ------------------------------------------------------------------
    // Finding tanks
    // ------------------------------------------------------------------

    /**
     * Waits up to the configured timeout for a tank to appear in the scene.
     *
     * @return tank name, or null if none appeared in time
     */
    public String waitForTank() {
        return waitUtils.waitForTank();
    }

    /** Asks once, no waiting. @return tank name, or null if none in scene */
    public String findTank() throws IOException {
        return client.findTank();
    }

    /**
     * Finds all tanks currently in the scene.
     *
     * @return array of tank names, or null if no tanks found
     */
    public String[] findAllTanks() throws IOException {
        String[] tanks = client.findAllTanks();
        log.debug("findAllTanks → {}", tanks != null ? String.join(", ", tanks) : "none");
        return tanks;
    }

    // ------------------------------------------------------------------
    // Combat
    // ------------------------------------------------------------------

    /** Teleports the named tank to a position in the world. */
    public boolean moveTank(String name, float x, float y, float z) throws IOException {
        return client.moveTank(name, x, y, z);
    }

    /** Rotates the shooter tank so it faces the target tank. */
    public boolean aimAt(String shooter, String target) throws IOException {
        return client.aimAt(shooter, target);
    }

    /** Fires the weapon on the named tank. */
    public boolean shoot(String name) throws IOException {
        return client.shoot(name);
    }

    // ------------------------------------------------------------------
    // Health
    // ------------------------------------------------------------------

    /**
     * Reads the current health of the named tank.
     *
     * Returns null if the tank was not found — callers should check for null
     * rather than checking for a magic number like -1.
     */
    public Float getHealth(String name) throws IOException {
        return client.getHealth(name);
    }

    /**
     * Waits until the tank's health drops below the given value, or times out.
     *
     * @return the health reading once it drops, or null on timeout
     */
    public Float waitForHealthBelow(String name, float threshold) {
        return waitUtils.waitForHealthBelow(name, threshold);
    }
}
