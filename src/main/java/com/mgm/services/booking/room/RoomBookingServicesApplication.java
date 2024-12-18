package com.mgm.services.booking.room;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Random;


/**
 * Entry point for the spring boot application providing all capabilities for
 * room booking.
 *
 */
@SpringBootApplication
@EnableFeignClients
@Configuration
@EnableScheduling
@ComponentScan(basePackages = { "com.mgm.services.booking.room", "com.mgm.services.common" })
public class RoomBookingServicesApplication {

	/**
	 * Entry method for the application
	 * 
	 * @param args Application arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(RoomBookingServicesApplication.class, args);
	}

	static class DynamicScheduleCalculator {
		public String calc() {
			int minVal = 1500;
			int maxVal = 1620;
			long delay = new Random().nextInt((maxVal - minVal) + 1) + minVal;
			return String.valueOf(delay*1000);
		}
	}
	@Bean("scheduleCalculator")
	public DynamicScheduleCalculator createScheduleCalculator() {
		return new DynamicScheduleCalculator();
	}
}
