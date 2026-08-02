package com.pbft.crypto;

import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.DilithiumParameterSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.security.*;

/**
 * Phase 7 – Dilithium Post-Quantum Signature Scheme
 *
 * FIX: Uses a fixed seed per node-id so every JVM process
 * generates the SAME Dilithium key pair for the same node.
 * Cross-machine verification works because node-2 on Acer
 * derives the same node-0 public key as node-0 on Lenovo.
 *
 * Seed formula: "pbft-dilithium-node-" + nodeIndex
 */
public class DilithiumSignatureScheme implements SignatureScheme {

    static {
        Security.addProvider(new BouncyCastlePQCProvider());
    }

    private static final String ALGORITHM = "Dilithium";

    @Autowired private KeyRegistry keyRegistry;
    @Value("${node.total:4}") private int totalNodes;

    public void generateKeysForAllNodes() throws Exception {
        for (int i = 0; i < totalNodes; i++) {
            String nodeId = "node-" + i;
            if (!keyRegistry.has(nodeId)) {
                // Deterministic seed: same seed → same key pair on every machine
                byte[] seed = ("pbft-dilithium-node-" + i).getBytes();
                SecureRandom rng = SecureRandom.getInstance("SHA1PRNG");
                rng.setSeed(seed);

                KeyPairGenerator kpg = KeyPairGenerator.getInstance(ALGORITHM, "BCPQC");
                kpg.initialize(DilithiumParameterSpec.dilithium2, rng);

                KeyPair kp = kpg.generateKeyPair();
                keyRegistry.register(nodeId, kp);
            }
        }
        System.out.println("[Dilithium] Deterministic keys generated for " + totalNodes + " nodes");
    }

    @Override
    public byte[] sign(byte[] data) throws Exception {
        KeyPair kp = keyRegistry.getKeyPair(CurrentNode.id);
        Signature sig = Signature.getInstance(ALGORITHM, "BCPQC");
        sig.initSign(kp.getPrivate());
        sig.update(data);
        return sig.sign();
    }

    @Override
    public boolean verify(byte[] data, byte[] signatureBytes, String senderId) throws Exception {
        KeyPair kp = keyRegistry.getKeyPair(senderId);
        if (kp == null) return false;
        Signature sig = Signature.getInstance(ALGORITHM, "BCPQC");
        sig.initVerify(kp.getPublic());
        sig.update(data);
        return sig.verify(signatureBytes);
    }

    @Override
    public String schemeName() { return "DILITHIUM"; }
}