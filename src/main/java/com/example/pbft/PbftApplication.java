package com.example.pbft;

// Import Spring Boot core class used to launch the application
import org.springframework.boot.SpringApplication;

// Marks this class as a Spring Boot application (combines @Configuration, @EnableAutoConfiguration, and @ComponentScan)
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Main entry point of the Spring Boot application
@SpringBootApplication
public class PbftApplication {

    // The main method is the starting point when the application is run
    public static void main(String[] args) {

        // Launches the Spring Boot application
        // - Creates the application context
        // - Starts the embedded server (e.g., Tomcat)
        // - Performs component scanning and auto-configuration
        SpringApplication.run(PbftApplication.class, args);
    }
}