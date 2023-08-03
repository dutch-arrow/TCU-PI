/*
 * Copyright © 2021 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 08 Aug 2021.
 */


package nl.das.tcu.objects;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.wiringpi.Gpio;

import nl.das.tcu.Util;

/**
 * Pi 3B+ - pi4j pin (device)
 * ==========================
 * pin 11 - GPIO-00  (light1)
 * pin 12 - GPIO-01  (light2)
 * pin 13 - GPIO-02  (light3)
 * pin 15 - GPIO-03  (light4)
 * pin 16 - GPIO-04  (uvlight)
 * pin 18 - GPIO-05  (light6)
 * pin 22 - GPIO-06  (spare)
 * pin 29 - GPIO-21  (pump)
 * pin 31 - GPIO-22  (sprayer)
 * pin 33 - GPIO-23  (mist)
 * pin 35 - GPIO-24  (fan_in)
 * pin 37 - GPIO-25  (fan_out)
 * pin 36 - GPIO-27  (temperature external DHT22)
 * pin  7 - GPIO-07  (temperature internal DS18B20)
 * pin  3 - GPIO-08  (LCD SDA)
 * pin  5 - GPIO-09  (LCD SCL)
 *
 */
public class Terrarium {

	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(Terrarium.class);

	private static DateTimeFormatter dtfmt = DateTimeFormatter.ofPattern("HH:mm:ss");

	public static int NR_OF_DEVICES = 0;
	public static final int NR_OF_RULES = 5;
	public static final int NR_OF_ACTIONS_PER_RULE = 5;
	public static final int NR_OF_ACTIONS_PER_SPRAYERRULE = 4;
	public static int maxNrOfTraceDays = 30;

	public static TerrariumConfig cfg = new TerrariumConfig();
	private static Map<String, Pin> devicePin;
	private static List<Device> devices = new ArrayList<>();
	private static Map<String, DeviceState> devStates = new HashMap<>();
  private RuleState ruleState = new RuleState();
	private static boolean test = false;
	private Sensors sensors = new Sensors();
	private static LocalDateTime now;
	private boolean traceOn = false;
	private long traceStartTime;
	private static Terrarium instance = null;

	public static String traceFolder = "tracefiles";
	public static String traceStateFilename;
	public static String traceTempFilename;

	static {
        Map<String, Pin> aMap = new HashMap<>();
        aMap.put("light1",  RaspiPin.GPIO_00);
        aMap.put("light2",  RaspiPin.GPIO_01);
        aMap.put("light3",  RaspiPin.GPIO_02);
        aMap.put("light4",  RaspiPin.GPIO_03);
        aMap.put("uvlight", RaspiPin.GPIO_04);
        aMap.put("light6",  RaspiPin.GPIO_05);
        aMap.put("spare",   RaspiPin.GPIO_06);
        aMap.put("pump",    RaspiPin.GPIO_21);
        aMap.put("sprayer", RaspiPin.GPIO_22);
        aMap.put("mist",    RaspiPin.GPIO_23);
        aMap.put("fan_in",  RaspiPin.GPIO_24);
        aMap.put("fan_out", RaspiPin.GPIO_25);
        devicePin = Collections.unmodifiableMap(aMap);
    };

	private Terrarium() {
		String[] deviceList   = {"light1", "light2", "light3", "light4", "uvlight", "light6", "pump", "sprayer", "mist", "fan_in", "fan_out", "spare"};
		int[] timersPerDevice = {1,         1,        1,        1,        1,         1,        5,      5,         5,      5,        5,         5};
		Terrarium.cfg.setDeviceList(deviceList);
		Terrarium.cfg.setTimersPerDevice(timersPerDevice);
		Terrarium.cfg.setRules(new TemperatureRule[NR_OF_RULES]);
	}

	public static Terrarium getInstance() {
		if (instance == null) {
			instance = new Terrarium();
		}
		return instance;
	}

	public static Terrarium getInstance(String json) {
		instance = new Terrarium();
		Jsonb jsonb = JsonbBuilder.create();
		cfg = jsonb.fromJson(json, TerrariumConfig.class);
		NR_OF_DEVICES = cfg.getDeviceList().length;
		return instance;
	}

	/******************************** Special methods ******************************************/

	public void setNow(LocalDateTime now) {
		Terrarium.now = now;
	}

	public static LocalDateTime getNow() {
		if (!test) {
			return LocalDateTime.now();
		}
		return now;
	}

	public void init() {
		// Count total number of timers
		int nrOfTimers = 0;
		for (int i : Terrarium.cfg.getTimersPerDevice()) {
			nrOfTimers += i;
		}
		Terrarium.cfg.setTimers(new Timer[nrOfTimers]);
		// Initialize Timers
		int timerIndex = 0;
		for (int i = 0; i < NR_OF_DEVICES; i++) {
			for (int dix = 0; dix < Terrarium.cfg.getTimersPerDevice()[i]; dix++) {
				Terrarium.cfg.setTimer(timerIndex, new Timer(Terrarium.cfg.getDeviceList()[i], dix + 1, "00:00", "00:00", 0, 0));
				timerIndex++;
			}
		}
		// Initialize temperature rules
		Terrarium.cfg.setRules(
			new TemperatureRule[] {
				new TemperatureRule(false, "", "", 0, 0, 0, new Action[] {
						new Action("no device", 0), new Action("no device", 0), new Action("no device", 0), new Action("no device", 0), new Action("no device", 0) }),
				new TemperatureRule(false, "", "", 0, 0, 0, new Action[] {
						new Action("no device", 0), new Action("no device", 0), new Action("no device", 0), new Action("no device", 0), new Action("no device", 0) }),
				new TemperatureRule(false, "", "", 0, 0, 0, new Action[] {
						new Action("no device", 0), new Action("no device", 0), new Action("no device", 0), new Action("no device", 0), new Action("no device", 0) }),
				new TemperatureRule(false, "", "", 0, 0, 0, new Action[] {
						new Action("no device", 0), new Action("no device", 0), new Action("no device", 0), new Action("no device", 0), new Action("no device", 0) }),
				new TemperatureRule(false, "", "", 0, 0, 0, new Action[] {
						new Action("no device", 0), new Action("no device", 0), new Action("no device", 0), new Action("no device", 0), new Action("no device", 0) })
			}
		);
		// Initialize sprayerrule
		Terrarium.cfg.setSprayerRule(new SprayerRule(0, new Action[] {
				new Action("no device", 0),
				new Action("no device", 0),
				new Action("no device", 0),
				new Action("no device", 0)
			}
		));
		saveSettings();
	}

	public void initDevices() {
		Gpio.wiringPiSetup();
		// Initialize devices
		for (int i = 0; i < Terrarium.cfg.getDeviceList().length; i++) {
			String nm = Terrarium.cfg.getDeviceList()[i];
			Terrarium.devices.add(new Device(nm, devicePin.get(nm), PinState.LOW, nm.equalsIgnoreCase("uvlight")));
		}
	}

	public void initMockDevices() {
		// Initialize devices
		for (int i = 0; i < NR_OF_DEVICES; i++) {
			String nm = Terrarium.cfg.getDeviceList()[i];
			Terrarium.devices.add(new Device(nm, nm.equalsIgnoreCase("uvlight")));
		}
	}

	public String getProperties() {
		String json = "";
		json += "{\"nr_of_timers\":" + Terrarium.cfg.getTimers().length + ",\"nr_of_programs\":" + NR_OF_RULES + ",";
		json += "\"devices\": [";
		for (int i = 0; i < NR_OF_DEVICES; i++) {
			json += "{\"device\":\"" + Terrarium.devices.get(i).getName() + "\", \"nr_of_timers\":" + Terrarium.cfg.getTimersPerDevice()[i] + ", \"lc_counted\":";
			json += (Terrarium.devices.get(i).hasLifetime() ? "true}" : "false}");
			if (i != (NR_OF_DEVICES - 1)) {
				json += ",";
			}
		}
		json += "]}";
		return json;
	}

	public void saveSettings(String settingsPath) {
		Jsonb jsonb = JsonbBuilder.create(new JsonbConfig().withFormatting(true).withNullValues(true));
		try {
			Files.deleteIfExists(Paths.get(settingsPath));
			Files.writeString(Paths.get(settingsPath), jsonb.toJson(Terrarium.cfg), StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveSettings() {
		saveSettings("settings.json");
	}

	public void saveLifecycleCounters() {
		try {
			String json = "";
			Files.deleteIfExists(Paths.get("lifecycle.txt"));
			for (int i = 0; i < NR_OF_DEVICES; i++) {
				if (Terrarium.devices.get(i).hasLifetime()) {
					String nm = Terrarium.devices.get(i).getName();
					json += nm + "=" + Terrarium.devStates.get(nm).getLifetime() + "\n";
				}
			}
			Files.writeString(Paths.get("lifecycle.txt"), json, StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setLifecycleCounter(String device, int value) {
		Terrarium.devStates.get(device).setLifetime(value);
		saveLifecycleCounters();
	}

	public void setTrace(boolean on) {
		if (on) {
			this.traceOn = on;
			this.traceStartTime = Util.now(now);
			traceStateFilename = Util.createStateTraceFile(traceFolder, now);
			traceTempFilename  = Util.createTemperatureTraceFile(traceFolder, now);
			Util.traceState(traceFolder + "/" + traceStateFilename, now, "start");
			Util.traceTemperature(traceFolder + "/" + traceTempFilename, now, "start");
			for (String d : Terrarium.cfg.getDeviceList()) {
				Util.traceState(traceFolder + "/" + traceStateFilename, now, "%s %s %d", d, isDeviceOn(d) ? "1" : "0", Terrarium.devStates.get(d).getOnPeriod());
			}
		} else if (this.traceOn) {
			Util.traceState(traceFolder + "/" + traceStateFilename, now, "stop");
			Util.traceTemperature(traceFolder + "/" + traceTempFilename, now, "stop");
			this.traceOn = on;
		}
	}

	public boolean isTraceOn() {
		return this.traceOn;
	}

	public void checkTrace () {
		// Max one day of tracing
		if ((Util.now(now)  >= (this.traceStartTime + (1440 * 60))) && isTraceOn()) {
			setTrace(false);
			setTrace(true);
		}

	}

	/********************************************* Sensors *********************************************/

	public void initSensors () {
		initSensors(false);
	}

	public void initSensors (boolean tst) {
		test = tst;
		this.sensors = new Sensors();
		if (!test) {
			this.sensors.readSensorValues();
		}
	}

	public void readSensorValues() {
		if (!test) {
			this.sensors.readSensorValues();
		}
	}

	public Sensors getSensors() {
		if (!test) {
			this.sensors.readSensorValues();
		}
		return this.sensors;
	}

	public void setSensors(int troom, int tterrarium) {
		this.sensors.getSensors()[0].setTemperature(troom);
		this.sensors.getSensors()[1].setTemperature(tterrarium);
		test = true;
	}

	public void setTestOff () {
		test = false;
	}

	public int getRoomTemperature() {
		return this.sensors.getSensors()[0].getTemperature();
	}

	public int getTerrariumTemperature() {
		return this.sensors.getSensors()[1].getTemperature();
	}

	/********************************************* Timers *********************************************/

	public Timer[] getTimersForDevice (String device) {
		Timer[] tmrs;
		if (device == "") {
			tmrs = Terrarium.cfg.getTimers();
		} else {
			int nr = Terrarium.cfg.getTimersPerDevice()[getDeviceIndex(device)];
			tmrs = new Timer[nr];
			int i = 0;
			for (Timer t : Terrarium.cfg.getTimers()) {
				if (t.getDevice().equalsIgnoreCase(device)) {
					tmrs[i] = t;
					i++;
				}
			}
		}
		return tmrs;
	}

	public void replaceTimers(Timer[] tmrs) {
		for (Timer tnew : tmrs) {
			for (int i = 0; i < Terrarium.cfg.getTimers().length; i++) {
				Timer told = Terrarium.cfg.getTimers()[i];
				if (told.getDevice().equalsIgnoreCase(tnew.getDevice()) && (told.getIndex() == tnew.getIndex())) {
					Terrarium.cfg.setTimer(i, tnew);
				}
			}
		}
	}

	public void initTimers(LocalDateTime now) {
		for (Timer t : Terrarium.cfg.getTimers()) {
			if (t.getRepeat() != 0) {
				int timerMinutesOn = (t.getHour_on() * 60) + t.getMinute_on();
				int timerMinutesOff = (t.getHour_off() * 60) + t.getMinute_off();
				int curMinutes = (now.getHour() * 60) + now.getMinute();
				if ((curMinutes >= timerMinutesOn) && (curMinutes <= timerMinutesOff)) {
					setDeviceOn(t.getDevice(), DeviceState.ENDTIME_INDEFINITE, DeviceState.CONTROLLED_BY_TIMER);
				}
			}
		}
	}

	/**
	 * Check the timers if a device needs to be switched on or off.
	 * These need to be executed every minute.
	 *
	 * A device can be switched on by a rule. If its is and it should now be switched on
	 * because of a timer then the rule should not interfere, so the rule should be
	 * deactivated until the device is switched off by the timer.
	 * Then the rule should be activated again.
	 */
	public void checkTimers() {
//		Util.println("Timers are checked. " + ruleState.toString());
		for (Timer t : Terrarium.cfg.getTimers()) {
			if (t.getRepeat() != 0) { // Timer is not active
				if (t.getPeriod() == 0) { // Timer has an on and off
					int timerMinutesOn = (t.getHour_on() * 60) + t.getMinute_on();
					int timerMinutesOff = (t.getHour_off() * 60) + t.getMinute_off();
					int curMinutes = (now.getHour() * 60) + now.getMinute();
					if (curMinutes == timerMinutesOn) {
						if (
              getDeviceState(t.getDevice()).getControlledBy() == DeviceState.NOT_CONTROLLED ||
              getDeviceState(t.getDevice()).getControlledBy() > 0 // controlled by any temperature rule
            ) {
							setDeviceOn(t.getDevice(), DeviceState.ENDTIME_INDEFINITE, DeviceState.CONTROLLED_BY_TIMER);
							if (t.getDevice().equalsIgnoreCase("mist")) {
								setDeviceOff("fan_in", DeviceState.CONTROLLED_BY_MIST_RULE);
								setDeviceOff("fan_out", DeviceState.CONTROLLED_BY_MIST_RULE);
							}
						}
					} else if ((timerMinutesOff != 0) && (curMinutes == timerMinutesOff)) {
            if (isDeviceOn(t.getDevice()) && getDeviceState(t.getDevice()).getControlledBy() == DeviceState.CONTROLLED_BY_TIMER) {
              setDeviceOff(t.getDevice(), DeviceState.NOT_CONTROLLED);
            }
					}
				} else { // Timer has an on and period
					int timerMinutesOn = (t.getHour_on() * 60) + t.getMinute_on();
					int curMinutes = (now.getHour() * 60) + now.getMinute();
					long endtime = Util.now(now) + t.getPeriod();
					if (curMinutes == timerMinutesOn) {
						if (!isDeviceOn(t.getDevice()) && getDeviceState(t.getDevice()).getControlledBy() == DeviceState.NOT_CONTROLLED) {
							setDeviceOn(t.getDevice(), endtime, DeviceState.CONTROLLED_BY_TIMER);
						}
					}
				}
			}
		}
	}

	/**************************************************** Rules ******************************************************/

	public TemperatureRule getRule(int nr) {
		return Terrarium.cfg.getRules()[nr - 1];
	}

	public void replaceRule(int nr, TemperatureRule rule) {
		Terrarium.cfg.setRule(nr - 1, rule);
	}

	public void initRules() {
    ruleState.reset();
	}

	/**
	 * Check the temperature rules.
	 * These need to be executed every minute.
	 */
	public void checkTemperatureRules() {
//		Util.println("Temperature rules are checked. " + ruleState.toString());
    long endtime = 0L;
		if (ruleState.getRulenr() != RuleState.SPRAYER_RULENR ) { // only if the sprayer rule is not active
			for (int rulenr = 0; rulenr < Terrarium.cfg.getRules().length; rulenr++) {
				TemperatureRule r = Terrarium.cfg.getRules()[rulenr];
				if (r.active(now) == 0) { // Is this temperature rule now valid?
//					Util.println("Rule " + (rulenr + 1) + " Threshold=" + r.getTemp_threshold() + " Ideal=" + r.getTemp_ideal() + " Delay=" + r.getDelay());
					// Temperature below threshold
					if (r.getTemp_threshold() < 0) {
						if (getTerrariumTemperature() < -r.getTemp_threshold()) {
							ruleState.setRulenr(rulenr + 1);
							long tm = Util.now(now);
							if (ruleState.getDelayed() == 0) { // no delay so exectute actions
//								System.out.println("Actions executed");
								for (Action a : r.getActions()) {
									if (!a.getDevice().equalsIgnoreCase("no device")) {
                    if (getDeviceState(a.getDevice()).getControlledBy() == DeviceState.NOT_CONTROLLED) {
                      endtime = a.getOn_period() > 0 ? Util.now(now) + a.getOn_period() : a.getOn_period();
				              setDeviceOn(a.getDevice(), endtime, rulenr + 1);
                      ruleState.addDevice(a.getDevice());
                    }
									}
								}
								// Start delay if needed
								if (r.getDelay() > 0) {
									ruleState.setDelayed(tm + r.getDelay());
//									System.out.println("Delayed " + Util.cvtPeriodToString(ruleDelayedTill[i]));
								}
							} else if (tm >= ruleState.getDelayed()) {
//								System.out.println("Delay is finished");
								ruleState.setDelayed(0);
							} else {
//								System.out.println("In delay...");
							}
							break;
						}
						if (getTerrariumTemperature() == r.getTemp_ideal()) {
              // Terrarium temperature has reached ideal temperature, so switch all related devices off
							for (Action a : r.getActions()) {
								if (!a.getDevice().equalsIgnoreCase("no device") && isDeviceOn(a.getDevice())
										&& (Terrarium.devStates.get(a.getDevice()).getControlledBy() == (rulenr + 1))) {
									setDeviceOff(a.getDevice(), DeviceState.NOT_CONTROLLED);
								}
							}
              ruleState.reset();
						}
					// Temperature above threshold
					} else if (r.getTemp_threshold() > 0) {
//            Util.println("getTerrariumTemperature()=" + getTerrariumTemperature() + " r.getTemp_threshold()=" + r.getTemp_threshold());
						if (getTerrariumTemperature() > r.getTemp_threshold()) {
							ruleState.setRulenr(rulenr + 1);
							long tm = Util.now(now);
//              Util.println("temprulestate.delayed=" + temprulestate.getDelayed());
							if (ruleState.getDelayed() == 0) { // no delay so exectute actions
//								System.out.println("Actions executed");
								for (Action a : r.getActions()) {
									if (!a.getDevice().equalsIgnoreCase("no device")) {
//                    Util.println("checkTemperatureRules(): Device " + a.getDevice() + " controlled by " + getDeviceState(a.getDevice()).getControlledBy());
                    if (getDeviceState(a.getDevice()).getControlledBy() == DeviceState.NOT_CONTROLLED) {
                      endtime = a.getOn_period() > 0 ? Util.now(now) + a.getOn_period() : a.getOn_period();
				              setDeviceOn(a.getDevice(), endtime, rulenr + 1);
                      ruleState.addDevice(a.getDevice());
                    }
									}
								}
								// Start delay if needed
								if (r.getDelay() > 0) {
									ruleState.setDelayed(tm + r.getDelay());
//									System.out.println("Delayed " + Util.cvtPeriodToString(ruleDelayedTill[i]));
								}
							} else if (tm >= ruleState.getDelayed()) {
//								System.out.println("Delay is finished");
								ruleState.setDelayed(0);
							} else {
//								System.out.println("In delay...");
							}
							break;
						}
						if (getTerrariumTemperature() == r.getTemp_ideal()) {
              // Terrarium temperature has reached ideal temperature, so switch all related devices off
							for (Action a : r.getActions()) {
								if (!a.getDevice().equalsIgnoreCase("no device") && isDeviceOn(a.getDevice())
										&& (Terrarium.devStates.get(a.getDevice()).getControlledBy() == rulenr + 1)) {
									setDeviceOff(a.getDevice(), DeviceState.NOT_CONTROLLED);
								}
							}
              ruleState.reset();
						}
					}
				} else if (r.getActive().equalsIgnoreCase("yes")) { // Rule is not valid anymore
					for (Action a : r.getActions()) {
						if (!a.getDevice().equalsIgnoreCase("no device") && 
                isDeviceOn(a.getDevice()) && 
                Terrarium.devStates.get(a.getDevice()).getControlledBy() == rulenr + 1) 
            {
//              Util.println("checkTemperatureRules() - rule invalid : " + r.active(now) + "->" + now.toString());
							setDeviceOff(a.getDevice(), DeviceState.NOT_CONTROLLED);
						}
					}
          ruleState.reset();
				}
			}
		}
	}

	public SprayerRule getSprayerRule () {
		return Terrarium.cfg.getSprayerRule();
	}

	public void setSprayerRule (SprayerRule sprayerRule) {
		Terrarium.cfg.setSprayerRule(sprayerRule);
	}

	/**
	 * Execute the rules as defined in sprayerrule.
	 * These need to be executed every minute.
	 */
	public void checkSprayerRule() {
//		Util.println("Sprayer rule is checked. " + ruleState.toString());
		if (isSprayerRuleActive()) {
			if (Util.now(now) == ruleState.getDelayed()) {
				for (Action a : Terrarium.cfg.getSprayerRule().getActions()) {
          if (!a.getDevice().equalsIgnoreCase("no device")) {
            long endtime = a.getOn_period() > 0 ? Util.now(now) + a.getOn_period() : a.getOn_period();
            setDeviceOn(a.getDevice(), endtime, DeviceState.CONTROLLED_BY_SPRAYER_RULE);
            ruleState.addDevice(a.getDevice());
          }
				}
        ruleState.setDelayed(0L);
			}
      // Check if all devices have finished
      int n = ruleState.getDevices().size();
      for (String dev : ruleState.getDevices()) {
        if (!isDeviceOn(dev) && (ruleState.getDelayed() == 0L)) {
          n--;
        }
      }
      if (n == 0) {
        // All devices have finished, so clear rule state
        ruleState.reset();
      }
		}
	}

	public boolean isSprayerRuleActive() {
		return ruleState.getRulenr() == RuleState.SPRAYER_RULENR;
	}

	/**************************************************** Device ******************************************************/

	public void initDeviceState() {
		// Initialize device states
		for (int i = 0; i< NR_OF_DEVICES; i++) {
			String nm = Terrarium.cfg.getDeviceList()[i];
			Terrarium.devStates.put(nm, new DeviceState(nm));
		}
		updateLifecycle();
	}

	Device getDevice(String device) {
		for (Device d : Terrarium.devices) {
			if (d.getName().equalsIgnoreCase(device)) {
				return d;
			}
		}
		return null;
	}

  DeviceState getDeviceState(String device) {
    return Terrarium.devStates.get(device);
  }

	public void updateLifecycle() {
		// Retrieve the lifecycle values from disk
		try {
			String json = new String(Files.readAllBytes(Paths.get("lifecycle.txt")));
			String lns[] = json.split("\n");
			for (String ln : lns) {
				String lp[] = ln.split("=");
				Terrarium.getInstance().setDeviceLifecycle(lp[0], Integer.parseInt(lp[1]));
			}
		} catch (NoSuchFileException e) {
		} catch (IOException e) {
//			Util.println(Util.getDateTimeString() + e.getMessage());
			e.printStackTrace();
		}

	}
	public boolean isDeviceOn(String device) {
		return Terrarium.devStates.get(device).getOnPeriod() != 0L;
	}

	/**
	 * @param device
	 * @param endtime in Epoch seconds or -1 or -2
   * @param ruleId 1=Temperature rule, 2=Sprayer rule, 3=Mist rule, 4=Timer, 5=Manual
	 */
	public void setDeviceOn(String device, long endtime, int controlledBy) {
		getDevice(device).switchOn();
    Terrarium.devStates.get(device).setOnPeriod(endtime);
    Terrarium.devStates.get(device).setControlledBy(controlledBy);
		if (endtime > 0L) {
//		  Util.println("Device " + device + " is switched on till " + Util.cvtTimeToString(endtime) + 
//        (controlledBy != DeviceState.NOT_CONTROLLED ? " and is controlled by " + DeviceState.getControlledByText(controlledBy) : ""));
			String dt = Util.ofEpochSecond(endtime).format(dtfmt);
			Util.traceState(traceFolder + "/" + traceStateFilename, now, "%s 1 %s %d", device, dt, controlledBy);
			if (device.equalsIgnoreCase("sprayer") && (
          controlledBy == DeviceState.CONTROLLED_BY_TIMER ||
          controlledBy == DeviceState.NOT_CONTROLLED
      )) {
        ruleState.setRulenr(RuleState.SPRAYER_RULENR);
				// Set sprayerRuleDelayEndtime = start time in minutes + delay in minutes
        ruleState.setDelayed(Util.now(now) + Terrarium.cfg.getSprayerRule().getDelay() * 60);
				// and switch fan_out and fan_in off and put them under control of the Sprayer Rule
				setDeviceOff("fan_in", DeviceState.CONTROLLED_BY_SPRAYER_RULE);
        ruleState.addDevice("fan_in");
				setDeviceOff("fan_out", DeviceState.CONTROLLED_BY_SPRAYER_RULE);
        ruleState.addDevice("fan_out");
			}
		} else {
//		  Util.println("Device " + device + " is switched on till " + DeviceState.getEndtimeText(endtime) + 
//        (controlledBy != DeviceState.NOT_CONTROLLED ? " and is controlled by " + DeviceState.getControlledByText(controlledBy) : ""));
			Util.traceState(traceFolder + "/" + traceStateFilename, now, "%s 1 %d %d", device, endtime, controlledBy);
		}
	}

	/**
	 * @param device
   * @param controlledBy See DeviceState
	 */
	public void setDeviceOff(String device, int controlledBy) {
//		Util.println("Device " + device + " is switched off" + 
//      (controlledBy != 0 ? " and is controlled by " + DeviceState.getControlledByText(controlledBy) : ""));
		getDevice(device).switchOff();
    Terrarium.devStates.get(device).setOnPeriod(DeviceState.ENDTIME_OFF);
    Terrarium.devStates.get(device).setControlledBy(controlledBy);
		Util.traceState(traceFolder + "/" + traceStateFilename, now, "%s 0 %d", device, controlledBy);
    if (device.equalsIgnoreCase("mist")) {
      setDeviceOff("fan_in", DeviceState.NOT_CONTROLLED);
      setDeviceOff("fan_out", DeviceState.NOT_CONTROLLED);
    }
	}

	public void setDeviceManualOn(String device) {
		Terrarium.devStates.get(device).setManual(true);
	}

	public void setDeviceManualOff(String device) {
		Terrarium.devStates.get(device).setManual(false);
	}

	public void setDeviceLifecycle(String device, int value) {
		Terrarium.devStates.get(device).setLifetime(value);
	}

	public void decreaseLifetime(int nrOfHours) {
		for (Device d : Terrarium.devices) {
			if (d.hasLifetime() && Terrarium.getInstance().isDeviceOn(d.getName())) {
				Terrarium.devStates.get(d.getName()).decreaseLifetime(nrOfHours);
				saveLifecycleCounters();
			}
		}
	}

	public String getState() {
		Terrarium.getInstance().updateLifecycle();
		String json = "{\"trace\":\"" +  (this.traceOn ? "on" : "off") + "\",\"state\": [";
		for (int i = 0; i < Terrarium.devStates.size(); i++) {
			String devnm = Terrarium.devices.get(i).getName();
			json += Terrarium.devStates.get(devnm).toJson() + ",";
		}
		json = json.substring(0,  json.length() - 1) + "]}";
		return json;
	}

	public int getDeviceIndex(String device) {
		int ix = -1;
		for (int i = 0; i < NR_OF_DEVICES; i++) {
			if (Terrarium.cfg.getDeviceList()[i].equalsIgnoreCase(device)) {
				ix = i;
				break;
			}
		}
		return ix;
	}

	/**
	 * Check if a device needs to be switched off when it has a onPeriod > 0
	 * This check needs to be done every second since the onPeriod is defined in Epoch-seconds.
	 */
	public void checkDevices() {
		for (String devnm : Terrarium.devStates.keySet()) {
			DeviceState d = Terrarium.devStates.get(devnm);
			if (d.getOnPeriod() > 0) {
				// Device has an end time defined
//        Util.println("now=" + Util.now(now) + " onPeriod=" + d.getOnPeriod());
				if (Util.now(now) >= Math.abs(d.getOnPeriod())) {
//          Util.println("rule id=" + rulestate.getRulenr() + " device=" + d.getName());
					setDeviceOff(d.getName(), DeviceState.NOT_CONTROLLED);
				}
			}
		}
	}

	public Map<String, Pin> getDevicePin () {
		return devicePin;
	}

	public void setDevicePin (Map<String, Pin> devicePin) {
		Terrarium.devicePin = devicePin;
	}

	public List<Device> getDevices () {
		return devices;
	}

	public void setDevices (List<Device> devices) {
		Terrarium.devices = devices;
	}

	public Map<String,DeviceState> getDevStates () {
		updateLifecycle();
		return devStates;
	}

	public void setDevStates (Map<String,DeviceState> devStates) {
		Terrarium.devStates = devStates;
	}
}
