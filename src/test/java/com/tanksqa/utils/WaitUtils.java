package com.tanksqa.utils;

import com.alttester.AltDriver;
import com.alttester.AltObject;
import com.alttester.Commands.FindObject.AltFindObjectsParams;
import com.alttester.Commands.FindObject.AltWaitForObjectsParams;

public class WaitUtils {

    private AltDriver altDriver;

    public WaitUtils(AltDriver altDriver) {
        this.altDriver = altDriver;
    }

    public AltObject waitForObject(String name) {
        AltFindObjectsParams findParams = new AltFindObjectsParams
                .Builder(AltDriver.By.NAME, name).build();
        AltWaitForObjectsParams waitParams = new AltWaitForObjectsParams
                .Builder(findParams).build();
        return altDriver.waitForObject(waitParams);
    }

    public boolean isObjectPresent(String name) {
        try {
            AltFindObjectsParams params = new AltFindObjectsParams
                    .Builder(AltDriver.By.NAME, name).build();
            altDriver.findObject(params);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}