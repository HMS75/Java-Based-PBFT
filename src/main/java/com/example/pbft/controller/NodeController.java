package com.example.pbft.controller;

import com.example.pbft.model.Message;
import com.example.pbft.service.ConsensusService;
import org.springframework.web.bind.annotation.*;

/*
 * NodeController is the HTTP entry point.
 *
 * /receive  -> used by peer nodes to deliver PBFT messages
 * /start    -> used to manually trigger a new consensus round
 */

@RestController
@RequestMapping("/node")
public class NodeController {

    private final ConsensusService consensusService;

    public NodeController(ConsensusService consensusService) {
        this.consensusService = consensusService;
    }

    @PostMapping("/receive")
    public String receiveMessage(@RequestBody Message message) {
        return consensusService.handleMessage(message);
    }

    @PostMapping("/start")
    public String startConsensus(@RequestParam String data) {
        try {
            consensusService.initiateConsensus(data);
            return "Consensus initiated for: " + data;
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to initiate consensus: " + e.getMessage();
        }
    }
}