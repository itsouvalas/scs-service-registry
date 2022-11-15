package com.starkandwayne.serviceregistry;


import java.util.LinkedList;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;


@SpringBootApplication
@EnableEurekaServer
public class ServiceRegistryApplication {

	public static CloudFoundrySessionData SessionData;
	public static SpringApplication Application;
	private static ConfigurableApplicationContext ApplicationContext;
	public static LinkedList<ScsPeerInfo> CurrentLoadedPeers;

	public static String ServerPort;
	public static String[] CurrentArgs;

	public static void main(String[] args) {

		CurrentArgs = args;
		Application = new SpringApplication(ServiceRegistryApplication.class);
		ApplicationContext = Application.run(args);

	}


	@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}

}
