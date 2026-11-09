package com.jasavast;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Optional;

@SpringBootApplication
@ConfigurationPropertiesScan
@Slf4j
public class BoilerPlateApplication {

	public static void main(String[] args) {
		Environment env = SpringApplication.run(BoilerPlateApplication.class, args).getEnvironment();
		logApplicationStartup(env);
	}

	private static void logApplicationStartup(Environment env) {
		String protocol = Optional.ofNullable(env.getProperty("server.ssl.key-store")).map(key -> "https").orElse("http");
		// local.server.port is the port actually bound (also correct for server.port=0)
		String serverPort = env.getProperty("local.server.port", env.getProperty("server.port", "8080"));
		String basePath = Optional.ofNullable(env.getProperty("spring.webflux.base-path"))
				.filter(StringUtils::isNotBlank)
				.orElse("/");
		String hostAddress = "localhost";
		try {
			hostAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			log.warn("The host name could not be determined, using `localhost` as fallback");
		}
		log.info("""

						----------------------------------------------------------
						\tApplication '{}' is running! Access URLs:
						\tLocal: \t\t{}://localhost:{}{}
						\tExternal: \t{}://{}:{}{}
						\tProfile(s): \t{}
						----------------------------------------------------------""",
				env.getProperty("spring.application.name"),
				protocol, serverPort, basePath,
				protocol, hostAddress, serverPort, basePath,
				Arrays.toString(env.getActiveProfiles()));
	}
}
