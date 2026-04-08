package com.example.pbft.service;

import com.example.pbft.config.NodeConfig;
import com.example.pbft.model.Message;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * ConsensusService contains the PBFT-style protocol logic.
 *
 * Simplified flow:
 * 1. Initiator sends PRE_PREPARE
 * 2. Receivers verify and send PREPARE
 * 3. If enough PREPARE messages arrive, send COMMIT
 * 4. If enough COMMIT messages arrive, consensus is reached
 *
 * This is intentionally simplified for learning and experimentation.
 * It demonstrates:
 * - message broadcasting
 * - signature verification
 * - 3f+1 system model
 * - 2f+1 quorum
 */

@Service
public class ConsensusService {

    private final CryptoService cryptoService;
    private final NodeClientService nodeClientService;
    private final NodeConfig nodeConfig;
    private final KeyPair keyPair;

    /*
     * These maps store how many PREPARE and COMMIT messages
     * have been seen for a given piece of data.
     *
     * key   = transaction/data string
     * value = count of valid messages received
     */
    
    private final Map<String, Integer> prepareCount = new ConcurrentHashMap<>();
    private final Map<String, Integer> commitCount = new ConcurrentHashMap<>();

    public ConsensusService(CryptoService cryptoService,
                            NodeClientService nodeClientService,
                            NodeConfig nodeConfig,
                            KeyPair keyPair) {
        this.cryptoService = cryptoService;
        this.nodeClientService = nodeClientService;
        this.nodeConfig = nodeConfig;
        this.keyPair = keyPair;
    }

    /*
     * Starts the protocol by broadcasting a PRE_PREPARE message.
     * In real PBFT, only the primary/leader does this.
     */

    public void initiateConsensus(String data) throws Exception {
        Message message = createSignedMessage(data, "PRE_PREPARE");
        broadcast(message);
        System.out.println("[" + nodeConfig.getNodeId() + "] Sent PRE_PREPARE for data: " + data);
    }

    /*
     * Called when this node receives any message.
     * This is the core state transition function.
     */

    public String handleMessage(Message msg) {
        try {
            /*
             * Step 1: Verify incoming signature.
             * If invalid, reject message.
             */
            boolean valid = cryptoService.verify(
                    msg.getData(),
                    msg.getSignature(),
                    msg.getPublicKey()
            );

            if (!valid) {
                return "INVALID SIGNATURE";
            }

            System.out.println("[" + nodeConfig.getNodeId() + "] Verified " + msg.getType()
                    + " from " + msg.getSenderId());

            /*
             * Step 2: Based on message type, move to next PBFT phase.
             */
            switch (msg.getType()) {
                case "PRE_PREPARE":
                    sendPrepare(msg.getData());
                    break;

                case "PREPARE":
                    collectPrepare(msg.getData());
                    break;

                case "COMMIT":
                    collectCommit(msg.getData());
                    break;

                default:
                    return "UNKNOWN MESSAGE TYPE";
            }

            return "OK";

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: " + e.getMessage();
        }
    }

    /*
     * After receiving a valid PRE_PREPARE, this node broadcasts PREPARE.
     */

    private void sendPrepare(String data) throws Exception {
        Message prepareMsg = createSignedMessage(data, "PREPARE");
        broadcast(prepareMsg);
        System.out.println("[" + nodeConfig.getNodeId() + "] Broadcasting PREPARE for: " + data);
    }

    /*
     * Count valid PREPARE messages.
     * Once quorum is reached, move to COMMIT phase.
     */

    private void collectPrepare(String data) throws Exception {
        int count = prepareCount.getOrDefault(data, 0) + 1;
        prepareCount.put(data, count);

        System.out.println("[" + nodeConfig.getNodeId() + "] PREPARE count for '" + data + "' = " + count);

        if (count >= nodeConfig.getQuorum()) {
            sendCommit(data);
        }
    }

    /*
     * After enough PREPARE messages, broadcast COMMIT.
     */

    private void sendCommit(String data) throws Exception {
        Message commitMsg = createSignedMessage(data, "COMMIT");
        broadcast(commitMsg);
        System.out.println("[" + nodeConfig.getNodeId() + "] Broadcasting COMMIT for: " + data);
    }

    /*
     * Count valid COMMIT messages.
     * Once quorum is reached, consensus is considered achieved.
     */

    private void collectCommit(String data) {
        int count = commitCount.getOrDefault(data, 0) + 1;
        commitCount.put(data, count);

        System.out.println("[" + nodeConfig.getNodeId() + "] COMMIT count for '" + data + "' = " + count);

        if (count >= nodeConfig.getQuorum()) {
            System.out.println("[" + nodeConfig.getNodeId() + "] Consensus reached for data: " + data);
        }
    }

    /*
     * Utility to create a signed message using this node's private key.
     */

    private Message createSignedMessage(String data, String type) throws Exception {
        String signature = cryptoService.sign(data, keyPair.getPrivate());
        String publicKey = cryptoService.encodePublicKey(keyPair.getPublic());

        return new Message(
                data,
                type,
                nodeConfig.getNodeId(),
                signature,
                publicKey
        );
    }

    /*
     * Sends a message to all peers.
     *
     * For simplicity, this example sends to all listed URLs.
     * In practice, you may skip self URL if needed.
     */
    
    private void broadcast(Message message) {
        for (String peer : nodeConfig.getPeerNodes()) {
            nodeClientService.sendMessage(peer, message);
        }
    }
}