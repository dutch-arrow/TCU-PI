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
public class MistRulesTest {

	private static Terrarium terrarium;

	@BeforeAll
	public static void beforeAll () {
	}

	@BeforeEach
	public void before () throws IOException {
		String json = Files.readString(Paths.get("src/test/resources/settings_misttest.json"));
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
	public void testMistRuleIdealTemperature() {
    // Set temperature to ideal temperature
		terrarium.setSensors(21, 26);
		{
			// Time: 05:00:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(5, 0, 0)));
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
			// Time: 10:35:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(10, 35, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			// fan_in and fan_out should be switched on by timer
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "fan_in":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
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
			// Time: 11:00:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(11, 0, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			// Mist should be switched on permanently and fan_in and fan_out should be off controlled by mist rule
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
			// Time: 11:45:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(11, 45, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			// All should be switched off and uncontrolled
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
	public void testMistRuleLowTemperature() {
		terrarium.setSensors(21, 21);
		{
			// Time: 05:00:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(5, 0, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			// All should be switched off and uncontrolled
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          default:
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_OFF);
            checkControlledBy(dev.getName(), DeviceState.NOT_CONTROLLED);
        }
      }
		}
		{
			// Time: 10:30:00, rules are active, fan_in should be switched on (-2)
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(10, 30, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			// Fan_in should be switched on and controlled by Temperature rule 1
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
			// Time: 10:35:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(10, 35, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			// fan_in and fan_out should be switched on by timer
      for (Device dev : Terrarium.getInstance().getDevices()) {
        switch (dev.getName()) {
          case "fan_in":
            checkDeviceState(dev.getName(), DeviceState.ENDTIME_INDEFINITE);
            checkControlledBy(dev.getName(), DeviceState.CONTROLLED_BY_TIMER);
            break;
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
			// Time: 11:00:00, mist is switched on by timer, fan_in and fan_out should be switched off
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(11, 0, 0)));
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
			// Time: 11:45:00
			terrarium.setNow(LocalDateTime.of(LocalDate.of(2021, 1, 8), LocalTime.of(11, 45, 0)));
			terrarium.checkDevices();
			terrarium.checkTimers();
			terrarium.checkSprayerRule();
			terrarium.checkTemperatureRules();
			// Mist should be switched off and fan_in should be switched on by tempetature rule 5
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
	}

	private void checkDeviceState(String device, long state) {
		assertEquals(state, Terrarium.getInstance().getDevStates().get(device).getOnPeriod(),"Unexpected state for '" + device + "'");
	}

	private void checkControlledBy(String device, int controlledBy) {
		assertEquals(controlledBy, Terrarium.getInstance().getDevStates().get(device).getControlledBy(),"Unexpected contolledBy for '" + device + "'");
	}

}
