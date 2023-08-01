/*
 * Copyright © 2021 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 14 Aug 2021.
 */

package nl.das.tcu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.das.tcu.objects.Device;
import nl.das.tcu.objects.DeviceState;
import nl.das.tcu.objects.Terrarium;

/**
 * The settings.json has the following timeline:
 * 06:00 mist on
 * 06:45 mist off
 * 09:00 light1 on
 * 09:30 light2 on
 * 10:00 light3 on
 * 10:05 sprayer on till 10:05:30
 * 10:15 light4 on, fan_out on and fan_in on
 * 10:20 sprayer rule delay has ended so fan_in and fan_out on for 15 minutes
 * 10:25 timer of fan_in and fan_out ended but sprayer rule is active so nothing happens
 * 10:35 sprayer rule has ended so fan_in and fan_out off
 * 11:15 uvlight on
 * 15:00 fan_out on
 * 15:15 mist on
 * 15:30 fan_in on
 * 15:40 fan_in off
 * 15:45 fan_out off
 * 16:00 mist off
 * 20:00 pump is on
 * 20:05 fan_in on and fan_out on
 * 20:15 fan_in off, fan_out off and pump off
 * 21:00 light1 off
 * 21:30 light2 off
 * 22:00 light3 off and uvlight off
 * 22:15 light4 off anf light6 on
 * 09:00 light6 off
 *
 */
public class TimersTooHighTemperatureTest {

	public static Terrarium terrarium;

	@BeforeAll
	public static void beforeAll () {
	}

	@BeforeEach
	public void before () throws IOException {
		Terrarium.traceFolder = "src/test/resources/tracefiles";
		String json = Files.readString(Paths.get("src/test/resources/settings.json"));
		terrarium = Terrarium.getInstance(json);
		assertNotNull(terrarium, "Terrarium object cannot be null");
		terrarium.setNow(LocalDateTime.now());
		terrarium.initMockDevices();
		terrarium.initDeviceState();
		terrarium.initSensors(true);
		terrarium.initRules();
		terrarium.setTrace(false);
		terrarium.setSensors(21, 31); // Too high temperature, so temperature rules will be activated
	}

	@AfterEach
	public void after () {
	}

	@AfterAll
	public static void afterAll () {
	}

	@Test
	public void testAllTimers() throws InterruptedException {
		{
			// Time: 05:00:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(5, 0, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			// No device should be switched on
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time: 06:00:00, mist is switched on
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(6, 0, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "mist":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "fan_in":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_MIST_RULE);
            break;
          case "fan_out":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_MIST_RULE);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time: 06:45:00, mist is switched off
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(6, 45, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 09:00:00, light1 switched on
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(9, 0, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 09:30:00, light2 is switched on
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(9, 30, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 10:00:00, light3 is switched on
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(10, 0, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 10:05:00, sprayer is switched on for 30 sec
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(10, 5, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			long tm = Util.now(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(10, 5, 30)));
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "sprayer":
            checkDeviceState(dev.getName(), tm);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "fan_in":
          case "fan_out":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_SPRAYER_RULE);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 10:05:30, sprayer is switched off
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(10, 5, 30)));
			terrarium.checkDevices();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "fan_in":
          case "fan_out":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_SPRAYER_RULE);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 10:15:00, light4 is switched on, fan_in and fan_out timer are active now, but should be ignored because of sprayer rule
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(10, 15, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
          case "light4":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "fan_in":
          case "fan_out":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_SPRAYER_RULE);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 10:20:00
			// The delay is over, so sprayer rule actions must be activated
			// fan_in and fan_out are switched on for 15 minutes
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(10, 20, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			long tm = Util.now(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(10, 35, 0)));
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
          case "light4":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "fan_in":
          case "fan_out":
            checkDeviceState(dev.getName(), tm);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_SPRAYER_RULE);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 10:25:00, fan_in and fan_out timer will try to switched them off but are ignored because of sprayer rule,
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(10, 25, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			long tm = Util.now(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(10, 35, 0)));
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
          case "light4":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "fan_in":
          case "fan_out":
            checkDeviceState(dev.getName(), tm);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_SPRAYER_RULE);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 10:30:00, temperature rules are active
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(10, 30, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      // Temperature is 31 so fan_out, fan_in for 15 min and sprayer for 20 sec should be switched on
      // but nothing happens because all are under sprayer rule control.
			long tm = Util.now(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(10, 35, 0)));
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
          case "light4":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "fan_in":
          case "fan_out":
            checkDeviceState(dev.getName(), tm);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_SPRAYER_RULE);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 10:35:00, sprayer rule has ended so fan_in and fan_out are switched off
      // but now the temperature rule kicks in:
      // Because temperature is 31 Temperature rule 3 is activated: fan_out is switched on, fan_in on for 15 min and sprayer of 20 sec
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(10, 35, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			long tm1 = Util.now(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(10, 50, 0)));
			long tm2 = Util.now(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(10, 35, 20)));
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
          case "light4":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "fan_out":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_UNTIL_IDEAL);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TEMPRULE_3);
            break;
          case "fan_in":
            checkDeviceState(dev.getName(), tm1);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TEMPRULE_3);
            break;
          case "sprayer":
            checkDeviceState(dev.getName(), tm2);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TEMPRULE_3);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time: 10:35:20, sprayer is switched off
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(10, 35, 20)));
			terrarium.checkDevices();
			long tm1 = Util.now(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(10, 50, 0)));
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
          case "light4":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "fan_out":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_UNTIL_IDEAL);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TEMPRULE_3);
            break;
          case "fan_in":
            checkDeviceState(dev.getName(), tm1);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TEMPRULE_3);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time: 10:50:00, fan_in is switched off
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(10, 50, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
          case "light4":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "fan_out":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_UNTIL_IDEAL);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TEMPRULE_3);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time: 11:00:00, temperature is ideal again
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(11, 0, 0)));
			terrarium.setSensors(21, 26);
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
          case "light4":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 11:15:00, uvlight is switched on
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(11, 15, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
          case "light4":
          case "uvlight":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 15:00:00, fan_in and fan_out is switched on by timer
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(15, 0, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
          case "light4":
          case "uvlight":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "fan_in":
          case "fan_out":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 15:15:00, mist is switched on
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(15, 15, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
          case "light4":
          case "uvlight":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "mist":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "fan_in":
          case "fan_out":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_MIST_RULE);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 15:30:00, mist is switched off, so fan_in and fan_out are off but not controlled anymore
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(15, 30, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
          case "light4":
          case "uvlight":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 15:45:00, fan_in and fan_out timer will try to switched off, but there are already off
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(15, 45, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
          case "light4":
          case "uvlight":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 20:00:00, pump is switched on
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(20, 0, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
          case "light4":
          case "uvlight":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "pump":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 20:05:00, fan_in and fan_out are switched on
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(20, 5, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
          case "light4":
          case "uvlight":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "fan_in":
          case "fan_out":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "pump":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 20:15:00, fan_in, fan_out and pump are switched off
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(20, 15, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
          case "light2":
          case "light3":
          case "light4":
          case "uvlight":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 21:00:00, light1 is switched off
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(21, 0, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light2":
          case "light3":
          case "light4":
          case "uvlight":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 21:30:00, light2 is switched off
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(21, 30, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light3":
          case "light4":
          case "uvlight":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 22:00:00, light3 and uvlight are switched off
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(22, 0, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light4":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 22:15:00, light4 is switched off and light6 is switched on
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(22, 15, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light6":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time 23:00:00, light6 is still on
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 1), LocalTime.of(23, 0, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light6":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Next day, time 09:00:00, light6 is switched off and light1 is swithed on
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 8, 2), LocalTime.of(9, 0, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "light1":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}

	}

	private void checkDeviceState(String device, long state) {
		assertEquals(state, Terrarium.getInstance().getDevStates().get(device).getOnPeriod(),"Unexpected state for '" + device + "'");
	}

	private void checkControlledBy(String device, int controlledBy) {
		assertEquals(controlledBy, Terrarium.getInstance().getDevStates().get(device).getControlledBy(),"Unexpected contolledBy for '" + device + "'");
	}
}
