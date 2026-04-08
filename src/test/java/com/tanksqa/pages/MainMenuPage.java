package com.tanksqa.pages;

import com.alttester.AltDriver;
import com.alttester.AltObject;
import com.alttester.Commands.FindObject.AltFindObjectsParams;
import com.tanksqa.utils.WaitUtils;

public class MainMenuPage {

    private AltDriver altDriver;
    private WaitUtils waitUtils;

    public MainMenuPage(AltDriver altDriver) {
        this.altDriver = altDriver;
        this.waitUtils = new WaitUtils(altDriver);
    }

    public AltObject getPlayButton() {
        AltFindObjectsParams params = new AltFindObjectsParams
                .Builder(AltDriver.By.NAME, "PlayButton").build();
        return altDriver.findObject(params);
    }

    public void clickPlay() {
        getPlayButton().click();
    }

    public boolean isMainMenuVisible() {
        return waitUtils.isObjectPresent("PlayButton");
    }
}