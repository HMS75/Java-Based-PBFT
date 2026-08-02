package com.pbft.crypto;

import com.pbft.message.PBFTMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Phase 1 – Cryptographic Message Verification
 *
 * Decodes the Base64 signature from a PBFTMessage and delegates to
 * whichever SignatureScheme is active (ECDSA or Dilithium).
 */
@Component
public class SignatureVerifier {

    @Autowired private SignatureScheme scheme;

    /**
     * Returns true if the message's signature is valid.
     * Prints timing metrics to stdout for later comparison.
     */
    public boolean verify(PBFTMessage msg) {
        if (msg.getSignature() == null || msg.getSenderId() == null) {
            System.err.println("[Verifier] Message has no signature or senderId");
            return false;
        }
        try {
            byte[] sigBytes = Base64.getDecoder().decode(msg.getSignature());

            long t0    = System.nanoTime();
            boolean ok = scheme.verify(msg.signingBytes(), sigBytes, msg.getSenderId());
            long verifMs = (System.nanoTime() - t0) / 1_000_000;

            System.out.println("[Metrics] Verify time=" + verifMs + "ms"
                             + "  valid=" + ok
                             + "  sender=" + msg.getSenderId()
                             + "  scheme=" + scheme.schemeName());
            return ok;
        } catch (Exception ex) {
            System.err.println("[Verifier] Exception: " + ex.getMessage());
            return false;
        }
    }
}