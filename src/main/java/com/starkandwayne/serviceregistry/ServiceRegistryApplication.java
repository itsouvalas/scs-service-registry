package com.starkandwayne.serviceregistry;

import com.starkandwayne.serviceregistry.peers.Peers;

import java.util.Properties;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.ConfigurableApplicationContext;


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
		setupConfiguration();
		Application.setDefaultProperties(SetApplicationConfigruation( CurrentLoadedPeers.GetPeersAsString()));
		ApplicationContext = Application.run(args);

	}

	public static void RestartApplication() {
	

		Thread thread = new Thread(() -> {
			
            ApplicationContext.close();
			Application = new SpringApplication(ServiceRegistryApplication.class);
			Application.setDefaultProperties(SetApplicationConfigruation( CurrentLoadedPeers.GetPeersAsString()));
            ApplicationContext = Application.run(CurrentArgs);
		});
		thread.setDaemon(false);
        thread.start();
	}

	public static Properties SetApplicationConfigruation(String peersJSON) {
		Properties properties = new Properties();

		if (CurrentLoadedPeers.CurrentPeers.size() > 0) {
			System.out.println("Peers Detected");
			properties.setProperty("eureka.client.serviceUrl.defaultZone",peersJSON);
			properties.setProperty("eureka.client.registerWithEureka", "true");
			properties.setProperty("eureka.client.fetchRegistry", "true");
			System.out.println(properties);
		}
		else
		{
			System.out.println("No Peers Detected");
			properties.setProperty("eureka.client.serviceUrl.defaultZone", "http://localhost:8080/eureka");
			properties.setProperty("eureka.client.registerWithEureka", "false");
			properties.setProperty("eureka.client.fetchRegistry", "false");
			System.out.println(properties);
		}
		String index = "0";
		if (SessionData.INSTANCE_INDEX != null) {
			index = SessionData.INSTANCE_INDEX;
		}
		else
		{
			System.out.println("Value  INDEX " + SessionData.INSTANCE_INDEX);
		}
		ServerPort = "8761";
		System.out.println("Attempting to host on port " + ServerPort);
		properties.setProperty("server.port", ServerPort);
		return properties;
		
	}

	public static void setupConfiguration() {
		SessionData = CloudFoundrySessionData.GetEnvironment();
		CurrentLoadedPeers = Peers.LoadPeersFromENV();
	}

}
