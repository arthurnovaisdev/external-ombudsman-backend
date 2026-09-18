package com.mbfreire.employee_reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EmployeeReportingApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmployeeReportingApplication.class, args);
	}

}
