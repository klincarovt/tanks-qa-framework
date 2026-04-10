package com.tanksqa.base;

import com.tanksqa.driver.BridgeClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class that every test class extends.
 *
 * It handles two things automatically:
 *   1. Opens the connection to Unity before each test (@BeforeEach)
 *   2. Closes it after each test (@AfterEach), even if the test fails
 *
 * Your test class just extends this and uses the `client` field:
 *
 *   public class MyTest extends BaseTest {
 *       @Test
 *       public void myTest() {
 *           // client is already connected here
 *       }
 *   }
 */
public abstract class BaseTest {

    private static final Logger log = LoggerFactory.getLogger(BaseTest.class);

    // Protected so subclasses (your test classes) can access it directly
    protected BridgeClient client;

    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        log.info("=== START: {} ===", testInfo.getDisplayName());
        client = new BridgeClient();
        client.connect();
    }

    @AfterEach
    public void tearDown(TestInfo testInfo) {
        if (client != null) {
            client.disconnect();
        }
        log.info("=== END:   {} ===", testInfo.getDisplayName());
    }
}
