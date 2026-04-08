package com.tanksqa.pages;

import com.alttester.AltDriver;
import com.alttester.AltObject;
import com.alttester.Commands.FindObject.AltFindObjectsParams;
import com.alttester.Commands.ObjectCommand.AltGetComponentPropertyParams;
import com.tanksqa.utils.WaitUtils;

public class GameplayPage {

    private AltDriver altDriver;
    private WaitUtils waitUtils;

    public GameplayPage(AltDriver altDriver) {
        this.altDriver = altDriver;
        this.waitUtils = new WaitUtils(altDriver);
    }

    public AltObject getHealthSlider(String playerName) {
        AltFindObjectsParams params = new AltFindObjectsParams
                .Builder(AltDriver.By.NAME, playerName + "HealthSlider").build();
        return altDriver.findObject(params);
    }

    public float getHealthValue(String playerName) {
        AltObject slider = getHealthSlider(playerName);
        AltGetComponentPropertyParams params = new AltGetComponentPropertyParams
                .Builder("UnityEngine.UI.Slider", "value", "UnityEngine")
                .build();
        return slider.getComponentProperty(params, Float.class);
    }

    public boolean isGameplayVisible() {
        return waitUtils.isObjectPresent("UICanvas");
    }

    public AltObject getRoundText() {
        AltFindObjectsParams params = new AltFindObjectsParams
                .Builder(AltDriver.By.NAME, "RoundText").build();
        return altDriver.findObject(params);
    }
}