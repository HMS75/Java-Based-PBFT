package com.pbft.config;

import com.pbft.crypto.*;
import com.pbft.network.TcpServer;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Phase 3 + Phase 4 – Node Configuration
 *
 * Reads node.id, node.total, node.signature-scheme from application.properties
 * (or command-line overrides) and:
 *   1. Sets CurrentNode.id so signing works
 *   2. Creates the right SignatureScheme bean
 *   3. Generates keys for all nodes
 *   4. Starts the TCP server thread (Phase 5)
 */
@Configuration
public class NodeConfig {

    @Value("${node.id:0}")               private int    nodeId;
    @Value("${node.total:4}")            private int    totalNodes;
    @Value("${node.signature-scheme:ECDSA}") private String schemeChoice;
    @Value("${node.tcp-port:9080}")      private int    tcpPort;

    @Autowired private KeyRegistry    keyRegistry;
    @Autowired private TcpServer      tcpServer;

    @PostConstruct
    public void init() throws Exception {
        // 1. Tell CurrentNode who we are
        CurrentNode.id = "node-" + nodeId;
        System.out.println("=== PBFT Node starting: " + CurrentNode.id
                         + " | scheme=" + schemeChoice
                         + " | total=" + totalNodes + " ===");

        // 2. Generate keys via the selected scheme
        signatureScheme().generateKeysForAllNodes();  // calls ECDSA or Dilithium impl

        // FIX: Each PBFT node must bind to a unique TCP port
        // This prevents "Address already in use" and ensures proper peer-to-peer transport isolation
        int actualTcpPort = tcpPort + nodeId;

        System.out.println("[TCP] Starting server on port " + actualTcpPort
                + " (base=" + tcpPort + ", nodeId=" + nodeId + ")");

        new Thread(
                () -> tcpServer.start(actualTcpPort),
                "tcp-server"
        ).start();
            }

    // ── Signature Scheme Bean (Phase 6 vs 7) ─────────────────────────────────

    @Bean
    public SignatureScheme signatureScheme() {
        if ("DILITHIUM".equalsIgnoreCase(schemeChoice)) {
            DilithiumSignatureScheme d = new DilithiumSignatureScheme();
            // Inject dependencies manually (beans not yet ready via @Autowired in @Bean)
            injectDependencies(d);
            return d;
        }
        ECDSASignatureScheme e = new ECDSASignatureScheme();
        injectDependencies(e);
        return e;
    }

    // Spring @Bean methods run before @Autowired fields are set,
    // so we push the keyRegistry + totalNodes values in manually.
    private void injectDependencies(ECDSASignatureScheme s) {
        setField(s, "keyRegistry", keyRegistry);
        setField(s, "totalNodes",  totalNodes);
    }
    private void injectDependencies(DilithiumSignatureScheme s) {
        setField(s, "keyRegistry", keyRegistry);
        setField(s, "totalNodes",  totalNodes);
    }

    // Reflection helper – avoids needing public setters on scheme classes
    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            // Try superclass fields
            try {
                var field = target.getClass().getSuperclass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
            } catch (Exception e2) {
                throw new RuntimeException("Cannot inject " + fieldName, e2);
            }
        }
    }

    // ── Expose config values as beans ────────────────────────────────────────

    @Bean public int nodeId()     { return nodeId; }
    @Bean public int totalNodes() { return totalNodes; }
}