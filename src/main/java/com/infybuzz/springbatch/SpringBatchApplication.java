package com.infybuzz.springbatch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableBatchProcessing
@ComponentScan({"com.infybuzz.config", "com.infybuzz.service", "com.infybuzz.listner",
	"com.infybuzz.reader","com.infybuzz.processor","com.infybuzz.writer","com.infybuzz.controller"})
@EnableAsync
//@EnableScheduling
public class SpringBatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBatchApplication.class, args);
	}

}
