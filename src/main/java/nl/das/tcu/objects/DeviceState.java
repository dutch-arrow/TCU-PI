/*
 * Copyright © 2021 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 08 Aug 2021.
 */


package nl.das.tcu.objects;

import nl.das.tcu.Util;

/**
 *
 */
public class DeviceState {

//	private static Logger log = LoggerFactory.getLogger(DeviceState.class);

  public static final int CONTROLLED_BY_TEMPRULE_1 = 1;
  public static final int CONTROLLED_BY_TEMPRULE_2 = 2;
  public static final int CONTROLLED_BY_TEMPRULE_3 = 3;
  public static final int CONTROLLED_BY_TEMPRULE_4 = 4;
  public static final int CONTROLLED_BY_TEMPRULE_5 = 5;
  public static final int NOT_CONTROLLED = 0;
  public static final int CONTROLLED_BY_SPRAYER_RULE = -1;
  public static final int CONTROLLED_BY_MIST_RULE = -2;
  public static final int CONTROLLED_BY_TIMER = -3;

  public static final long ENDTIME_UNTIL_IDEAL = -2L;
  public static final long ENDTIME_INDEFINITE = -1L;
  public static final long ENDTIME_OFF = 0L;

	private String name;
	private long onPeriod; // 0 off, -1 indefinite, -2 until ideal value has been reached, >0 endtime in Epoch-seconds
	private int lifetime; // in hours
	private boolean manual;
  private int controlledBy; // > 0: temperature rulenr, 0: not controlled, -1: sprayer rule, -2: mist rule, -3: timer

	public DeviceState() { }

	public DeviceState(String name) {
		this.name = name;
		this.onPeriod = 0;
		this.lifetime = 0;
		this.manual = false;
    this.controlledBy = NOT_CONTROLLED;
	}

	public void decreaseLifetime(int nrOfHours) {
		this.lifetime -= nrOfHours;
	}

	public String getName () {
		return this.name;
	}

	public void setName (String name) {
		this.name = name;
	}

	public int getLifetime () {
		return this.lifetime;
	}

	public void setLifetime (int lifetime) {
		this.lifetime = lifetime;
	}

	public long getOnPeriod () {
		return this.onPeriod;
	}

	public void setOnPeriod (long onPeriod) {
		this.onPeriod = onPeriod;
	}

	public boolean isManual () {
		return this.manual;
	}

	public void setManual (boolean manual) {
		this.manual = manual;
	}


  public int getControlledBy() {
    return controlledBy;
  }

  public void setControlledBy(int controlledBy) {
    this.controlledBy = controlledBy;
  }

	public String toJson() {
		String state = "";
		if (this.onPeriod == 0) {
			state = String.format("{\"device\":\"%s\",\"state\":\"off\",\"controlledby\":\"%s\",\"hours_on\":%d,\"manual\":\"%s\"}",
					this.name, getControlledByText(this.controlledBy),this.lifetime, this.manual ? "yes" : "no");
    } else {
			state = String.format("{\"device\":\"%s\",\"state\":\"on\",\"controlledby\":\"%s\",\"end_time\":\"%s\",\"hours_on\":%d,\"manual\":\"%s\"}",
					this.name, getControlledByText(this.controlledBy), getEndtimeText(this.onPeriod),this.lifetime, this.manual ? "yes" : "no");
		}
		return state;
	}

  public static String getControlledByText(int code) {
    String text = "";
    switch (code) {
      case CONTROLLED_BY_TIMER:
        text = "Timer";
        break;
      case CONTROLLED_BY_MIST_RULE:
        text = "Mist Rule";
        break;
      case CONTROLLED_BY_SPRAYER_RULE:
        text = "Sprayer Rule";
        break;
      case NOT_CONTROLLED:
        text = "free";
        break;
      case CONTROLLED_BY_TEMPRULE_1:
        text = "Temp Rule 1";
        break;
      case CONTROLLED_BY_TEMPRULE_2:
        text = "Temp Rule 2";
        break;
      case CONTROLLED_BY_TEMPRULE_3:
        text = "Temp Rule 3";
        break;
      case CONTROLLED_BY_TEMPRULE_4:
        text = "Temp Rule 4";
        break;
      case CONTROLLED_BY_TEMPRULE_5:
        text = "Temp Rule 5";
        break;
      default:
        text = "Unknown code: " + code;
    }
    return text;
  }

  public static String getEndtimeText(long endtime) {
    String text = "";
    if (endtime == ENDTIME_UNTIL_IDEAL) {
      text = "until Ideal Temperature is reached";
    } else if (endtime == ENDTIME_INDEFINITE) {
      text = "indefinitely";
    } else if (endtime == ENDTIME_OFF) {
      text = "off";
    } else if (endtime > 0) {
      text = Util.cvtTimeToString(endtime);
    } else {
      text = "Unknown endtime: " + endtime;
    }
    return text;
  }
}
