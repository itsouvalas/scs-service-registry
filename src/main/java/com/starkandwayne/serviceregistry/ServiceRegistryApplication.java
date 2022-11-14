package com.starkandwayne.serviceregistry;

import com.starkandwayne.serviceregistry.peers.Peers;

import java.util.Properties;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
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
	public static Peers CurrentLoadedPeers;
	public static String ServerPort;
	public static String[] CurrentArgs;

	public static void main(String[] args) {

		CurrentArgs = args;
		Application = new SpringApplication(ServiceRegistryApplication.class);
		ApplicationContext = Application.run(args);

	}

	public static void RestartApplication() {
	
		Thread thread = new Thread(() -> {
	       ApplicationContext.close();
			Application = new SpringApplication(ServiceRegistryApplication.class);
            ApplicationContext = Application.run(CurrentArgs);
		});
		thread.setDaemon(false);
        thread.start();
	}

	@Bean
public RestTemplate restTemplate() {
    return new RestTemplate();
}

}
