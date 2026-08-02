package com.pbft.controller;

import com.pbft.consensus.ConsensusEngine;
import com.pbft.crypto.CurrentNode;
import com.pbft.crypto.SignatureScheme;
import com.pbft.crypto.SignatureVerifier;
import com.pbft.message.PBFTMessage;
import com.pbft.message.PBFTMessage.Type;
import com.pbft.network.MessageBroadcaster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

/**
 * PBFT REST Controller
 *
 * /propose  — BLOCKS until full COMMIT quorum is reached (or timeout).
 *             This is what makes the benchmark measure true consensus
 *             latency, not just HTTP submission speed.
 *
 * /preprepare /prepare /commit — receive and process PBFT messages
 * /status                      — observability
 */
@RestController
public class PBFTController {

    @Autowired private ConsensusEngine    consensus;
    @Autowired private SignatureVerifier  verifier;
    @Autowired private SignatureScheme    scheme;
    @Autowired private MessageBroadcaster broadcaster;

    @Value("${node.id:0}")              private int  nodeId;
    @Value("${node.commit-timeout:10000}") private long commitTimeoutMs;

    // ── /propose — blocks until COMMIT quorum ────────────────────────────────

    @PostMapping("/propose")
    public ResponseEntity<String> propose(@RequestBody Map<String, String> body) {
        if (nodeId != 0) {
            return ResponseEntity.badRequest()
                                 .body("Only node-0 (primary) can propose");
        }

        String data      = body.getOrDefault("data", "block-" + System.currentTimeMillis());
        String blockHash = sha256(data);
        int    seq       = nextSeq();

        PBFTMessage prePrepare = new PBFTMessage(
            Type.PRE_PREPARE, 1, seq, blockHash, CurrentNode.id, data
        );

        try {
            byte[] sigBytes = scheme.sign(prePrepare.signingBytes());
            prePrepare.setSignature(Base64.getEncoder().encodeToString(sigBytes));
            prePrepare.setSchemeUsed(scheme.schemeName());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                                 .body("Signing failed: " + ex.getMessage());
        }

        // Broadcast to all replicas
        broadcaster.broadcast(prePrepare);

        // Primary processes its own PRE-PREPARE
        consensus.handlePrePrepare(prePrepare);

        // ── BLOCK HERE until COMMIT quorum or timeout ──────────────────────
        try {
            boolean finalized = consensus.awaitFinalized(seq, commitTimeoutMs);
            if (finalized) {
                return ResponseEntity.ok(
                    "COMMITTED seq=" + seq + " hash=" + blockHash
                );
            } else {
                return ResponseEntity.status(504).body(
                    "TIMEOUT waiting for commit: seq=" + seq
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500).body("Interrupted");
        }
    }

    // ── PBFT message endpoints ────────────────────────────────────────────────

    @PostMapping("/preprepare")
    public ResponseEntity<String> receivePrePrepare(@RequestBody PBFTMessage msg) {
        if (!verifier.verify(msg)) return ResponseEntity.badRequest().body("Invalid signature");
        consensus.handlePrePrepare(msg);
        return ResponseEntity.ok("Accepted");
    }

    @PostMapping("/prepare")
    public ResponseEntity<String> receivePrepare(@RequestBody PBFTMessage msg) {
        if (!verifier.verify(msg)) return ResponseEntity.badRequest().body("Invalid signature");
        consensus.handlePrepare(msg);
        return ResponseEntity.ok("Accepted");
    }

    @PostMapping("/commit")
    public ResponseEntity<String> receiveCommit(@RequestBody PBFTMessage msg) {
        if (!verifier.verify(msg)) return ResponseEntity.badRequest().body("Invalid signature");
        consensus.handleCommit(msg);
        return ResponseEntity.ok("Accepted");
    }

    // ── Status ────────────────────────────────────────────────────────────────

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
            "nodeId",          CurrentNode.id,
            "schemeUsed",      scheme.schemeName(),
            "finalizedBlocks", consensus.getFinalizedBlocks()
        ));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int seqCounter = 1;
    private synchronized int nextSeq() { return seqCounter++; }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash).substring(0, 16);
        } catch (Exception ex) { return "hash-error"; }
    }
}