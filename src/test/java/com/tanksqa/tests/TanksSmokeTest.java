package com.tanksqa.tests;

import com.tanksqa.base.BaseTest;
import com.tanksqa.pages.TankPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke tests for the Tanks game.
 *
 * Smoke tests are the first tests you run — they verify the basics work
 * before running deeper tests. If any smoke test fails, something fundamental
 * is broken.
 *
 * Two tags control which tests run:
 *
 *   bridge   — game running, any screen
 *   gameplay — a round must be in progress with tanks spawned
 *
 * Commands:
 *   mvn test -Dgroups=bridge     (quick sanity check)
 *   mvn test -Dgroups=gameplay   (run after starting a round)
 *   mvn test                     (run everything)
 */
@DisplayName("Tanks Smoke Tests")
public class TanksSmokeTest extends BaseTest {

    private TankPage tankPage;

    @BeforeEach
    public void initPage() {
        tankPage = new TankPage(client);
    }

    // ------------------------------------------------------------------
    // Bridge
    // ------------------------------------------------------------------

    @Tag("bridge")
    @Test
    @DisplayName("Bridge responds PONG to PING")
    public void testBridgeIsAlive() throws Exception {
        assertTrue(tankPage.isBridgeAlive(),
            "TestBridge should respond PONG to a PING command");
    }

    // ------------------------------------------------------------------
    // Scene presence
    // ------------------------------------------------------------------

    @Tag("gameplay")
    @Test
    @DisplayName("At least one tank is present in the scene")
    public void testTankIsFoundInScene() {
        String tankName = tankPage.waitForTank();
        assertNotNull(tankName, "No tank found — is a round running?");
    }

    @Tag("gameplay")
    @Test
    @DisplayName("Tank name is not blank")
    public void testTankNameIsNotBlank() {
        String tankName = tankPage.waitForTank();
        assertNotNull(tankName, "No tank found — is a round running?");
        assertFalse(tankName.isBlank(), "Tank name should not be blank");
    }

    // ------------------------------------------------------------------
    // Combat
    // ------------------------------------------------------------------

    @Tag("gameplay")
    @Test
    @DisplayName("Tank takes damage after being shot")
    public void testTankTakesDamageAfterBeingShot() throws Exception {
        // 1. Find both tanks
        String[] tanks = tankPage.findAllTanks();
        assertNotNull(tanks, "No tanks found — is a round running?");
        assertTrue(tanks.length >= 2, "Need at least 2 tanks, found: " + tanks.length);

        String shooter = tanks[0];
        String target  = tanks[1];

        // 2. Move them 5 units apart so the blast radius reaches
        assertTrue(tankPage.moveTank(shooter, 0f, 0f, 0f), "Could not move shooter");
        assertTrue(tankPage.moveTank(target,  5f, 0f, 0f), "Could not move target");

        // 3. Rotate shooter to face target
        assertTrue(tankPage.aimAt(shooter, target), "Could not aim shooter at target");

        // 4. Record health before the shot
        Float healthBefore = tankPage.getHealth(target);
        assertNotNull(healthBefore, "Could not read target health");
        assertTrue(healthBefore > 0, "Target health should be positive before being shot");

        // 5. Fire
        assertTrue(tankPage.shoot(shooter), "Shoot command failed");

        // 6. Wait for the health to drop
        Float healthAfter = tankPage.waitForHealthBelow(target, healthBefore);
        assertNotNull(healthAfter,
            "Health did not drop after shot (stuck at " + healthBefore + ")");
        assertTrue(healthAfter < healthBefore,
            "Expected health < " + healthBefore + ", got " + healthAfter);
    }
}
