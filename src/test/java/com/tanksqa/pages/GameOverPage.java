package com.tanksqa.pages;

import com.alttester.AltDriver;
import com.alttester.AltObject;
import com.alttester.Commands.FindObject.AltFindObjectsParams;
import com.tanksqa.utils.WaitUtils;

public class GameOverPage {

    private AltDriver altDriver;
    private WaitUtils waitUtils;

    public GameOverPage(AltDriver altDriver) {
        this.altDriver = altDriver;
        this.waitUtils = new WaitUtils(altDriver);
    }

    public boolean isGameOverVisible() {
        return waitUtils.isObjectPresent("MessageText");
    }

    public AltObject getWinnerText() {
        AltFindObjectsParams params = new AltFindObjectsParams
                .Builder(AltDriver.By.NAME, "MessageText").build();
        return altDriver.findObject(params);
    }

    public String getWinnerMessage() {
        return getWinnerText().getText();
    }

    public boolean isRestartButtonVisible() {
        return waitUtils.isObjectPresent("RestartButton");
    }
}