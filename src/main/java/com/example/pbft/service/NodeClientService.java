package com.example.pbft.service;

import com.example.pbft.model.Message;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/*
 * NodeClientService is the outbound communication layer.
 *
 * ConsensusService decides WHAT to send.
 * NodeClientService handles HOW to send it over HTTP.
 */

@Service
public class NodeClientService {

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendMessage(String nodeUrl, Message message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Message> request = new HttpEntity<>(message, headers);

            restTemplate.postForObject(nodeUrl + "/node/receive", request, String.class);
        } catch (Exception e) {
            System.out.println("Failed to send message to " + nodeUrl + ": " + e.getMessage());
        }
    }
}