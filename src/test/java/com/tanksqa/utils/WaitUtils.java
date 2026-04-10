package com.tanksqa.utils;

import com.tanksqa.config.Config;
import com.tanksqa.driver.BridgeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Polling helpers that keep asking Unity for a value until it's ready
 * or a timeout is reached.
 *
 * Why polling? Game state changes asynchronously — a tank doesn't lose
 * health the moment a shell is fired. We keep asking every 500ms until
 * the expected state appears or we give up after 10 seconds.
 *
 * Every waitFor method follows the same pattern:
 *   1. Record a deadline (now + timeout)
 *   2. Ask the game for the value
 *   3. If the condition is met, return the value
 *   4. Otherwise sleep 500ms and try again
 *   5. If still not met by the deadline, return null and log a warning
 */
public class WaitUtils {

    private static final Logger log = LoggerFactory.getLogger(WaitUtils.class);

    private final BridgeClient client;
    private final long timeoutMs;
    private final long intervalMs;

    public WaitUtils(BridgeClient client) {
        this.client      = client;
        this.timeoutMs   = Config.waitTimeoutMs();
        this.intervalMs  = Config.waitIntervalMs();
    }

    /**
     * Keeps asking FIND_TANK until a tank appears in the scene.
     *
     * @return tank name, or null if no tank appeared within the timeout
     */
    public String waitForTank() {
        log.debug("Waiting for a tank (timeout {}ms)", timeoutMs);
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                String name = client.findTank();
                if (name != null) {
                    log.debug("Tank found: {}", name);
                    return name;
                }
                Thread.sleep(intervalMs);
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("waitForTank stopped early: {}", e.getMessage());
                return null;
            }
        }
        log.warn("waitForTank timed out after {}ms", timeoutMs);
        return null;
    }

    /**
     * Keeps asking GET_HEALTH until the tank's health drops below the threshold.
     *
     * Call this after shooting to confirm damage was dealt.
     *
     * @param tankName  name of the tank to watch
     * @param threshold the health value to drop below
     * @return the health reading once it drops, or null on timeout
     */
    public Float waitForHealthBelow(String tankName, float threshold) {
        log.debug("Waiting for {} health < {} (timeout {}ms)", tankName, threshold, timeoutMs);
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                Float health = client.getHealth(tankName);
                if (health != null && health < threshold) {
                    log.debug("{} health dropped to {}", tankName, health);
                    return health;
                }
                Thread.sleep(intervalMs);
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("waitForHealthBelow stopped early: {}", e.getMessage());
                return null;
            }
        }
        log.warn("waitForHealthBelow timed out — {} health never dropped below {}", tankName, threshold);
        return null;
    }
}
