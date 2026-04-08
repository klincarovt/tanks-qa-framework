package com.tanksqa.base;

import com.alttester.AltDriver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class BaseTest {

    protected AltDriver altDriver;

    @BeforeEach
    public void setUp() throws Exception {
        altDriver = new AltDriver();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (altDriver != null) {
            altDriver.stop();
        }
    }
}