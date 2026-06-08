package com.nexio.nexio;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class NexioApplication {

	public static void main(String[] args) {

        Dotenv dotenv=Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach((d)->System.setProperty(d.getKey(),d.getValue()));
        SpringApplication.run(NexioApplication.class, args);
	}

}
