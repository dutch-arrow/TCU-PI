/*
 * Copyright © 2021 Dutch Arrow Software - All Rights Reserved
 * You may use, distribute and modify this code under the
 * terms of the Apache Software License 2.0.
 *
 * Created 11 Aug 2021.
 */


package nl.das.tcu.objects;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.json.bind.annotation.JsonbTransient;

import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.w1.W1Master;

import nl.das.tcu.Util;
import nl.das.tcu.hw.DHT22;
import nl.das.tcu.hw.DS18B20;

/**
 *
 */
public class Sensors {

	@SuppressWarnings("unused")
	private String clock;
	private Sensor[] sensors = new Sensor[3];
	// Initialize the W1 bus (connected on GPIO 7)
	@JsonbTransient private W1Master w1Master = new W1Master();
	// Initialize the Temperature sensor
	@JsonbTransient private DS18B20 terrarium = new DS18B20(this.w1Master);
	@JsonbTransient private DHT22 room = new DHT22(RaspiPin.GPIO_27);
	@JsonbTransient private boolean roomConnected = true;

	public Sensors() {
		this.clock = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-y HH:mm"));
		this.sensors[0] = new Sensor("room", 0, 0);
		this.sensors[1] = new Sensor("terrarium", 0, 0);
		this.sensors[2] = new Sensor("cpu", 0, 0);
	}

	public void readSensorValues() {
		if (this.roomConnected) {
			try {
				this.roomConnected = this.room.doReadLoop();
				this.sensors[0].setTemperature((int) Math.round(this.room.getTemperature()));
				this.sensors[0].setHumidity((int) Math.round(this.room.getHumidity()));
			} catch (InterruptedException e) {
				Util.println("Error read room sensor values");
			} catch (IOException e) {
				Util.println("Error read room sensor values: " + e.getClass().getName());
				this.roomConnected = false;
			}
		} else {
			this.sensors[0].setTemperature(0);
			this.sensors[0].setHumidity(0);
		}
		this.sensors[1].setTemperature((int) Math.round(this.terrarium.getTemperature()));
		this.sensors[2].setTemperature(getCpuTemp());
		this.clock = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-y HH:mm"));
	}

	@JsonbTransient
	public int getCpuTemp() {
		Future<Integer> future = new CpuTemp().getCpuTemp();

		while(!future.isDone()) {
		    try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
			}
		}
		try {
			Integer result = future.get();
			return (int) Math.round(result / 1000.0);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return 0;
	}

	@JsonbTransient
	public int getTerrariumTemp() {
		return this.sensors[1].getTemperature();
	}

	@JsonbTransient
	public int getRoomTemp() {
		return this.sensors[0].getTemperature();
	}

	public String getClock () {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-y HH:mm"));
	}

	public void setClock (String clock) {
		this.clock = clock;
	}

	public Sensor[] getSensors () {
		return this.sensors;
	}

	public void setSensors (Sensor[] sensors) {
		this.sensors = sensors;
	}

	public class Sensor {
		private String location;
		private int temperature;
		private int humidity;

		public Sensor(String loc, int t, int h) {
			this.location = loc;
			this.temperature = t;
			this.humidity = h;
		}

		public String getLocation () {
			return this.location;
		}
		public void setLocation (String location) {
			this.location = location;
		}
		public int getTemperature () {
			return this.temperature;
		}
		public void setTemperature (int temperature) {
			this.temperature = temperature;
		}
		public int getHumidity () {
			return this.humidity;
		}
		public void setHumidity (int humidity) {
			this.humidity = humidity;
		}
	}

	private static class CpuTemp {

	    private ExecutorService executor = Executors.newSingleThreadExecutor();

	    public Future<Integer> getCpuTemp() {
			ProcessBuilder builder = new ProcessBuilder();
			builder.command("sh", "-c", "cat /sys/class/thermal/thermal_zone0/temp");
			try {
				Process process = builder.start();
				return this.executor.submit(() -> {
					BufferedReader rdr = new BufferedReader(new InputStreamReader(process.getInputStream()));
				    String line;
				    int cputemp = 0;
				    try {
						while ((line = rdr.readLine()) != null) {
						    if (line.length() > 2) {
						    	cputemp = Integer.parseInt(line);
						    }
						}
						rdr.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				    return cputemp;
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
	    }
	}
}
