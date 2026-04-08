package com.tanksqa.tests;

import com.tanksqa.base.BaseTest;
import com.tanksqa.pages.MainMenuPage;
import com.tanksqa.pages.GameplayPage;
import com.tanksqa.pages.GameOverPage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GameplayTests extends BaseTest {

    @Test
    public void testGameplayUIVisible() {
        MainMenuPage mainMenu = new MainMenuPage(altDriver);
        mainMenu.clickPlay();
        GameplayPage gameplay = new GameplayPage(altDriver);
        assertTrue(gameplay.isGameplayVisible(),
            "Gameplay UI should be visible after clicking play");
    }

    @Test
    public void testRoundTextExists() {
        MainMenuPage mainMenu = new MainMenuPage(altDriver);
        mainMenu.clickPlay();
        GameplayPage gameplay = new GameplayPage(altDriver);
        assertNotNull(gameplay.getRoundText(),
            "Round text should exist during gameplay");
    }

    @Test
    public void testGameOverScreenAppears() {
        GameOverPage gameOver = new GameOverPage(altDriver);
        assertTrue(gameOver.isGameOverVisible(),
            "Game over screen should be visible at end of game");
    }
}