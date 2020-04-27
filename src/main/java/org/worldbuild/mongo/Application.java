package org.worldbuild.mongo;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.worldbuild.core.config.CoreConfiguration;


@Log4j2
@SpringBootApplication
@Import({CoreConfiguration.class})
public class Application implements CommandLineRunner {

	public static void main(String[] args) {
		log.info("Application is initializing..........");
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		log.info("Triggering init ..............");
	}
}
