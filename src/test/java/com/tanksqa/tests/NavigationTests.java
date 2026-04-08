package com.tanksqa.tests;

import com.tanksqa.base.BaseTest;
import com.tanksqa.pages.MainMenuPage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NavigationTests extends BaseTest {

    @Test
    public void testMainMenuLoads() {
        MainMenuPage mainMenu = new MainMenuPage(altDriver);
        assertTrue(mainMenu.isMainMenuVisible(), 
            "Main menu should be visible on game launch");
    }

    @Test
    public void testClickPlayStartsGame() {
        MainMenuPage mainMenu = new MainMenuPage(altDriver);
        assertTrue(mainMenu.isMainMenuVisible(), 
            "Main menu should be visible before clicking play");
        mainMenu.clickPlay();
    }
}