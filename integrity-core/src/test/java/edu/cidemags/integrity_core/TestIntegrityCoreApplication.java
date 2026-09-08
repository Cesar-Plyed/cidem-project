package edu.cidemags.integrity_core;

import org.springframework.boot.SpringApplication;

public class TestIntegrityCoreApplication {

	public static void main(String[] args) {
		SpringApplication.from(IntegrityCoreApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
