package com.pbft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for a single PBFT node.
 *
 * Run multiple nodes by overriding properties on the command line:
 *   java -jar pbft.jar --node.id=0 --server.port=8080 --node.tcp-port=9080
 *   java -jar pbft.jar --node.id=1 --server.port=8081 --node.tcp-port=9081
 *   java -jar pbft.jar --node.id=2 --server.port=8082 --node.tcp-port=9082
 *   java -jar pbft.jar --node.id=3 --server.port=8083 --node.tcp-port=9083
 */
@SpringBootApplication
public class PbftApplication {

    public static void main(String[] args) {
        SpringApplication.run(PbftApplication.class, args);
    }
}