package nl.das.tcu.objects;

import java.time.LocalDateTime;

import javax.json.bind.annotation.JsonbTransient;

import nl.das.tcu.Util;

public class TemperatureRule {

	private String active;
	private String from;
	private String to;
	private int temp_ideal;
	private int temp_threshold;
	private int delay;
	private Action[] actions;

	public TemperatureRule() { }

	public TemperatureRule(boolean active, String from, String to, int tempIdeal, int tempThreshold, int delay, Action[] actions) {
		this.active = active ? "yes" : "no";
		this.from = from;
		this.to = to;
		this.temp_ideal = tempIdeal;
		this.temp_threshold = tempThreshold;
		this.delay = delay;
		this.actions = actions;
	}
	@JsonbTransient
	public void makeActive() {
		this.active = "yes";
	}

	@JsonbTransient
	public void makeInactive() {
		this.active = "no";
	}

	@JsonbTransient
	/*
	 * return: 0 = active, -1 = before active, 1 = after active
	 */
	public int active(LocalDateTime now) {
		long nowMinutes = (now.getHour() * 60L) + now.getMinute();
//    Util.println("Active? " + this.active + " nowMinutes=" + nowMinutes + " from=" + Util.cvtStringToMinutes(this.from) + " to=" + Util.cvtStringToMinutes(this.to));
		if (this.active.equalsIgnoreCase("yes")
				&& ((nowMinutes >= Util.cvtStringToMinutes(this.from)) && (nowMinutes < Util.cvtStringToMinutes(this.to)))) {
			return 0;
		}
		if (nowMinutes < Util.cvtStringToMinutes(this.from)) {
			return -1;
		}
		if (nowMinutes > Util.cvtStringToMinutes(this.to)) {
			return 1;
		}
		return -2;
	}

	public String getActive () {
		return this.active;
	}

	public void setActive (String active) {
		this.active = active;
	}

	public String getFrom () {
		return this.from;
	}

	public void setFrom (String from) {
		this.from = from;
	}

	public String getTo () {
		return this.to;
	}

	public void setTo (String to) {
		this.to = to;
	}

	public int getTemp_ideal () {
		return this.temp_ideal;
	}

	public void setTemp_ideal (int temp_ideal) {
		this.temp_ideal = temp_ideal;
	}

	public int getTemp_threshold() {
		return this.temp_threshold;
	}

	public void setTemp_threshold (int tempThreshold) {
		this.temp_threshold = tempThreshold;
	}

	public int getDelay () {
		return this.delay;
	}

	public void setDelay (int delay) {
		this.delay = delay;
	}

	public Action[] getActions() {
		return this.actions;
	}

	public void setActions (Action[] actions) {
		this.actions = actions;
	}
}