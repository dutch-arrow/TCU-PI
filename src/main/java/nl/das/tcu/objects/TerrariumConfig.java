/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 28 Jul 2022.
 */


package nl.das.tcu.objects;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.json.bind.annotation.JsonbTransient;

/**
 *
 */
public class TerrariumConfig {
	private String[] deviceList;
	private int[] timersPerDevice;
	private Timer[] timers;
	private TemperatureRule[] rules;
	private SprayerRule sprayerRule;

	public String[] getDeviceList () {
		return this.deviceList;
	}
	public void setDeviceList (String[] deviceList) {
		this.deviceList = deviceList;
	}
	public int[] getTimersPerDevice () {
		return this.timersPerDevice;
	}
	public void setTimersPerDevice (int[] timersPerDevice) {
		this.timersPerDevice = timersPerDevice;
	}
	public Timer[] getTimers () {
		return this.timers;
	}
	public void setTimers (Timer[] timers) {
		this.timers = timers;
	}
	@JsonbTransient
	public void setTimer(int ix, Timer t) {
		this.timers[ix] = t;
	}
	public TemperatureRule[] getRules () {
		return this.rules;
	}
	public void setRules (TemperatureRule[] rules) {
		List<TemperatureRule> thresholds = Arrays.asList(rules);
		Collections.sort(thresholds, new Comparator<TemperatureRule>() {
			@Override
			public int compare (TemperatureRule o1, TemperatureRule o2) {
				if ((o1 != null) && (o2 != null)) {
//					if (o1.getTemp_threshold() > 0) {
						if( o1.getTemp_threshold() > o2.getTemp_threshold()) {
							return -1;
						}
						if( o1.getTemp_threshold() == o2.getTemp_threshold()) {
							return 0;
						} else {
							return 1;
						}
//					}
//					if( o1.getTemp_threshold() < o2.getTemp_threshold()) {
//						return -1;
//					} else if( o1.getTemp_threshold() == o2.getTemp_threshold()) {
//						return 0;
//					} else {
//						return 1;
//					}
				}
				return 0;
			}
		});
		this.rules = thresholds.toArray(new TemperatureRule[0]);
	}
	@JsonbTransient
	public void setRule(int ix, TemperatureRule r) {
		this.rules[ix] = r;
	}
	public SprayerRule getSprayerRule () {
		return this.sprayerRule;
	}
	public void setSprayerRule (SprayerRule sprayerRule) {
		this.sprayerRule = sprayerRule;
	}

}
