package com.pbft.crypto;

import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 1 – Basic Cryptographic Node Authentication
 *
 * In a real system every node holds only its OWN private key.
 * For this simulation, all nodes run in the same JVM (or share
 * a pre-generated key set), so we store every node's KeyPair here.
 *
 * The registry is populated by each SignatureScheme implementation
 * when it starts up.
 */
@Component
public class KeyRegistry {

    // nodeId  →  KeyPair
    private final Map<String, KeyPair> keys = new HashMap<>();

    public void register(String nodeId, KeyPair keyPair) {
        keys.put(nodeId, keyPair);
        System.out.println("[KeyRegistry] Registered key for " + nodeId);
    }

    /** Returns the full KeyPair (used by the owning node to sign). */
    public KeyPair getKeyPair(String nodeId) {
        return keys.get(nodeId);
    }

    public boolean has(String nodeId) {
        return keys.containsKey(nodeId);
    }
}