package com.pbft.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.security.*;
import java.security.spec.ECGenParameterSpec;

/**
 * Phase 6 – ECDSA Signature Scheme (classical baseline)
 *
 * FIX: Uses a fixed seed per node-id so every JVM process
 * generates the SAME key pair for the same node.
 * This means node-2 on Acer produces the same node-0 public key
 * as node-0 on Lenovo — so cross-machine verification works.
 *
 * Seed formula: "pbft-node-" + nodeIndex (deterministic, shared secret)
 */
public class ECDSASignatureScheme implements SignatureScheme {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String ALGORITHM = "SHA256withECDSA";
    private static final String CURVE     = "secp256k1";

    @Autowired private KeyRegistry keyRegistry;
    @Value("${node.total:4}") private int totalNodes;

    public void generateKeysForAllNodes() throws Exception {
        for (int i = 0; i < totalNodes; i++) {
            String nodeId = "node-" + i;
            if (!keyRegistry.has(nodeId)) {
                // Deterministic seed: same seed → same key pair on every machine
                byte[] seed = ("pbft-ecdsa-node-" + i).getBytes();
                SecureRandom rng = SecureRandom.getInstance("SHA1PRNG");
                rng.setSeed(seed);

                KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", "BC");
                kpg.initialize(new ECGenParameterSpec(CURVE), rng);

                KeyPair kp = kpg.generateKeyPair();
                keyRegistry.register(nodeId, kp);
            }
        }
        System.out.println("[ECDSA] Deterministic keys generated for " + totalNodes + " nodes");
    }

    @Override
    public byte[] sign(byte[] data) throws Exception {
        KeyPair kp = keyRegistry.getKeyPair(CurrentNode.id);
        Signature sig = Signature.getInstance(ALGORITHM, "BC");
        sig.initSign(kp.getPrivate());
        sig.update(data);
        return sig.sign();
    }

    @Override
    public boolean verify(byte[] data, byte[] signatureBytes, String senderId) throws Exception {
        KeyPair kp = keyRegistry.getKeyPair(senderId);
        if (kp == null) return false;
        Signature sig = Signature.getInstance(ALGORITHM, "BC");
        sig.initVerify(kp.getPublic());
        sig.update(data);
        return sig.verify(signatureBytes);
    }

    @Override
    public String schemeName() { return "ECDSA"; }
}