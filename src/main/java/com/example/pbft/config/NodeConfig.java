package com.example.pbft.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/*
 * NodeConfig stores node-level configuration.
 *
 * In a distributed setup, each node needs:
 * - its own ID
 * - its own key pair
 * - the list of peer nodes
 *
 * For PBFT:
 * totalNodes = 3f + 1
 * quorum = 2f + 1
 *
 * Example:
 * totalNodes = 4
 * f = 1
 * quorum = 3
 */

@Configuration
public class NodeConfig {

    // Change this for each node instance
    private final String nodeId = "node1";

    // Exclude self if desired when broadcasting
    private final List<String> peerNodes = List.of(
            "http://localhost:8081",
            "http://localhost:8082",
            "http://localhost:8083",
            "http://localhost:8084"
    );

    private final int totalNodes = 4;

    public String getNodeId() {
        return nodeId;
    }

    public List<String> getPeerNodes() {
        return peerNodes;
    }

    public int getTotalNodes() {
        return totalNodes;
    }

    public int getF() {
        return (totalNodes - 1) / 3;
    }

    public int getQuorum() {
        return 2 * getF() + 1;
    }

    @Bean
    public KeyPair keyPair() throws NoSuchAlgorithmException {
        /*
         * For now we use ECDSA-compatible EC key generation.
         * Later, for post-quantum crypto, this bean can be replaced.
         */
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        return keyPairGenerator.generateKeyPair();
    }
}