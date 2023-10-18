/*
 * Copyright © 2023 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 02 May 2023.
 */

package nl.das.tcu.objects;

import java.util.ArrayList;
import java.util.List;

import nl.das.tcu.Util;

/**
 *
 */
public class RuleState {

    public static final int SPRAYER_RULENR = -1;

    private int rulenr = 0;
    private long delayed = 0;
    private List<String> devices = new ArrayList<>();

    public int getRulenr() {
        return this.rulenr;
    }

    public void setRulenr(int rulenr) {
        this.rulenr = rulenr;
    }

    public long getDelayed() {
        return this.delayed;
    }

    public void setDelayed(long delay) {
        this.delayed = delay;
    }

    public List<String> getDevices() {
        return devices;
    }

    public void setDevices(List<String> devices) {
        this.devices = devices;
    }

    public void addDevice(String dev) {
        if (!devices.contains(dev)) {
            devices.add(dev);
        }
    }

    public void removeDevice(String dev) {
        devices.remove(dev);
    }

    public void reset() {
        this.rulenr = 0;
        this.delayed = 0;
        this.devices = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "RuleState: rulenr=" + this.rulenr + " delayed=" + Util.cvtTimeToString(this.delayed) + " devices="
                + this.devices;
    }
}
