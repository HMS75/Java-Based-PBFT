package com.pbft.crypto;

/**
 * Phase 6 & 7 – Common Signature Interface
 *
 * Both ECDSA (classical) and Dilithium (post-quantum) implement this.
 * Swapping schemes requires only changing the Spring bean, not consensus code.
 */
public interface SignatureScheme {

    /** Generate key pairs for all configured nodes and register them. */
    void generateKeysForAllNodes() throws Exception;

    /** Sign arbitrary bytes. Returns raw signature bytes. */
    byte[] sign(byte[] data) throws Exception;

    /** Verify raw signature bytes against the data and the claimed sender's key. */
    boolean verify(byte[] data, byte[] signatureBytes, String senderId) throws Exception;

    /** Human-readable name used in metrics output. */
    String schemeName();
}