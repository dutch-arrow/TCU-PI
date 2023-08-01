/*
 * Copyright © 2022 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 16 Jul 2022.
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
 *
 */
public class SpayerRulesTest {

	private static Terrarium terrarium;

	@BeforeAll
	public static void beforeAll () {
	}

	@BeforeEach
	public void before () throws IOException {
		String json = Files.readString(Paths.get("src/test/resources/settings_sprayertest.json"));
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
	public void testSprayerRuleWithIdealTemperature() {
		terrarium.setSensors(21, 26); // ideal temparature
		{
			// Time: 05:00:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(5, 0, 0)));
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
			// Time: 10:30:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(10, 30, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			// rules are switched on
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time: 11:00:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(11, 0, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			// sprayer should be switched on for 30 seconds
			long tm = Util.now(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(11, 0, 30)));
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "sprayer":
            checkDeviceState(dev.getName(), tm);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "fan_in":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_SPRAYER_RULE);
            break;
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
			// Time: 11:00:30
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(11, 0, 30)));
			terrarium.checkDevices();
			// Don't check timers and rules because there is not a minute passed.
			// sprayer should be switched off
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "sprayer":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
            break;
          case "fan_in":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_SPRAYER_RULE);
            break;
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
			// Time: 11:15:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(11, 15, 00)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			// fan_in and fan_out should be switched on for 15 minutes
			long tm = Util.now(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(11, 30, 0)));
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "fan_in":
            checkDeviceState(dev.getName(), tm);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_SPRAYER_RULE);
            break;
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
			// Time: 11:30:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(11, 30, 00)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			// no device should be on
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time: 22:15:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(22, 15, 00)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			// no device should be on
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		/*
		 */
	}

	@Test
	public void testSprayerRuleWithLowTemperature() {
		terrarium.setSensors(21, 21); // temparature too low
		{
			// Time: 05:00:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(5, 0, 0)));
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
			// Time: 10:30:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(10, 30, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			// rules are switched on, fan_in should be switched on by rule and fan_out should be switched off
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
			// Time: 11:00:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(11, 0, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			// sprayer should be switched on for 30 seconds and fan_in and fan_out should be switched off
			long tm = Util.now(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(11, 0, 30)));
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "sprayer":
            checkDeviceState(dev.getName(), tm);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
          case "fan_in":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_SPRAYER_RULE);
            break;
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
			// Time: 11:00:30
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(11, 0, 30)));
			terrarium.checkDevices();
			// Don't check timers and rules because there is not a minute passed.
			// sprayer should be switched off
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "sprayer":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
            break;
          case "fan_in":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_SPRAYER_RULE);
            break;
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
			// Time: 11:15:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(11, 15, 00)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			// fan_in and fan_out should be switched on for 15 minutes
			long tm = Util.now(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(11, 30, 0)));
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "fan_in":
            checkDeviceState(dev.getName(), tm);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_SPRAYER_RULE);
            break;
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
			// Time: 11:30:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(11, 30, 00)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			// fan_in should be on because of temperature rule 5
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
			// Time: 22:15:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(22, 15, 00)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			// Temerature rules are not active anymore, so no device should be on
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
