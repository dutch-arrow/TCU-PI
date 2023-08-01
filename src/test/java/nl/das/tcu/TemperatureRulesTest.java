/*
 * Copyright © 2021 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 23 Aug 2021.
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
import nl.das.tcu.objects.TemperatureRule;
import nl.das.tcu.objects.Terrarium;

/**
 *
 */
public class TemperatureRulesTest {

	private static Terrarium terrarium;

	@BeforeAll
	public static void beforeAll () {
	}

	@BeforeEach
	public void before () throws IOException {
		String json = Files.readString(Paths.get("src/test/resources/settings_temprules.json"));
		terrarium = Terrarium.getInstance(json);
		assertNotNull(terrarium, "Terrarium object cannot be null");
		terrarium.setNow(LocalDateTime.now());
		terrarium.initMockDevices();
		terrarium.initDeviceState();
		terrarium.initSensors(true);
		terrarium.initRules();
		terrarium.setTrace(false);
	}

	@AfterEach
	public void after () {
	}

	@AfterAll
	public static void afterAll () {
	}

	@Test
	public void testRulesTemperature21 () {
		for (int i = 0; i < Terrarium.cfg.getRules().length; i++) {
			TemperatureRule r = Terrarium.cfg.getRules()[i];
			System.out.print(r.getTemp_threshold() + " ");
		}
		System.out.println();
		terrarium.setSensors(21, 21);
		{
			// Time: 05:00:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(5, 0, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      // no devices should be on
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time: 10:30:00, temperature rules are active.
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(10, 30, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      // Because temperature is 21 Temperature rule 4 is activated: fan_in is switched on and light6 (a heat-source) for 15 minutes
			long tm = Util.now(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(10, 45, 0)));
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "fan_in":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_UNTIL_IDEAL);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TEMPRULE_4);
            break;
          case "light6":
            checkDeviceState(dev.getName(), tm);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TEMPRULE_4);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time: 10:45:00, light6 is switched off
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(10, 45, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "fan_in":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_UNTIL_IDEAL);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TEMPRULE_4);
            break;
          case "light6":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time: 13:30:00, temperature is ideal again
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(13, 30, 0)));
			terrarium.setSensors(21, 26);
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
			// Time: 22:15:00, Temperature rules are not valid anymore
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(22, 15, 0)));
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
	}

	@Test
	public void testRulesTemperature24 () {
		terrarium.setSensors(21, 24);
		{
			// Time: 05:00:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(5, 0, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      // no devices should be on
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time: 10:30:00, temperature rules are active.
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(10, 30, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      // Because temperature is 24 Temperature rule 5 is activated: fan_in is switched on
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "fan_in":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_UNTIL_IDEAL);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TEMPRULE_5);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time: 13:30:00, temperature is ideal again
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(13, 30, 0)));
			terrarium.setSensors(21, 26);
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
			// Time: 22:15:00, Temperature rules are not valid anymore
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(22, 15, 0)));
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
	}

	@Test
	public void testRulesTemperature29 () {
		terrarium.setSensors(21, 29);
		{
			// Time: 05:00:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(5, 0, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      // no devices should be on
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time: 10:30:00, temperature rules are active.
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(10, 30, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      // Because temperature is 29 Temperature rule 3 is activated: fan_out is switched on
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
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
			// Time: 13:30:00, temperature is ideal again
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(13, 30, 0)));
			terrarium.setSensors(21, 26);
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
			// Time: 22:15:00, Temperature rules are not valid anymore
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(22, 15, 0)));
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
	}

	@Test
	public void testRulesTemperature31 () {
		terrarium.setSensors(21, 31);
		{
			// Time: 05:00:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(5, 0, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      // no devices should be on
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time: 10:30:00, temperature rules are active.
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(10, 30, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      // Because temperature is 31 Temperature rule 2 is activated: fan_out is switched on, fan_in on for 15 min and sprayer of 20 sec
      // The check has a delay of 30 min.
			long tm1 = Util.now(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(10, 45, 0)));
			long tm2 = Util.now(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(10, 30, 20)));
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "fan_out":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_UNTIL_IDEAL);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TEMPRULE_2);
            break;
          case "fan_in":
            checkDeviceState(dev.getName(), tm1);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TEMPRULE_2);
            break;
          case "sprayer":
            checkDeviceState(dev.getName(), tm2);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TEMPRULE_2);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time: 10:30:20, sprayer is switched off
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(10, 30, 20)));
			terrarium.checkDevices();
			long tm1 = Util.now(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(10, 45, 0)));
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "fan_out":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_UNTIL_IDEAL);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TEMPRULE_2);
            break;
          case "fan_in":
            checkDeviceState(dev.getName(), tm1);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TEMPRULE_2);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time: 10:45:00, fan_in is switched off
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(10, 45, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "fan_out":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_UNTIL_IDEAL);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TEMPRULE_2);
            break;
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time: 13:30:00, temperature is ideal again
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(13, 30, 0)));
			terrarium.setSensors(21, 26);
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
			// Time: 22:15:00, Temperature rules are not valid anymore
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(22, 15, 0)));
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
	}

	private void checkDeviceState(String device, long state) {
		assertEquals(state, Terrarium.getInstance().getDevStates().get(device).getOnPeriod(),"Unexpected state for '" + device + "'");
	}

	private void checkControlledBy(String device, int controlledBy) {
		assertEquals(controlledBy, Terrarium.getInstance().getDevStates().get(device).getControlledBy(),"Unexpected contolledBy for '" + device + "'");
	}

}
