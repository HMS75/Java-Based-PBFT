package com.pbft.message;

/**
 * Phase 2 – Signed PBFT Message Model
 *
 * One class covers PRE-PREPARE, PREPARE, and COMMIT.
 * The 'signature' field (Base64-encoded) is attached after signing.
 */
public class PBFTMessage {

    // ── PBFT phase ──────────────────────────────
    public enum Type { PRE_PREPARE, PREPARE, COMMIT }

    private Type    type;
    private int     viewNumber;
    private int     sequenceNumber;
    private String  blockHash;      // SHA-256 hash of the block data
    private String  senderId;       // "node-0", "node-1", …
    private String  payload;        // arbitrary block data (for PRE_PREPARE)
    private String  signature;      // Base64-encoded bytes from SignatureScheme
    private String  schemeUsed;     // "ECDSA" or "DILITHIUM" (for metrics)

    // ── Constructors ─────────────────────────────
    public PBFTMessage() {}

    public PBFTMessage(Type type, int viewNumber, int sequenceNumber,
                       String blockHash, String senderId, String payload) {
        this.type           = type;
        this.viewNumber     = viewNumber;
        this.sequenceNumber = sequenceNumber;
        this.blockHash      = blockHash;
        this.senderId       = senderId;
        this.payload        = payload;
    }

    // ── Helpers ──────────────────────────────────

    /** Returns the bytes that will be signed / verified. */
    public byte[] signingBytes() {
        String raw = type + "|" + viewNumber + "|" + sequenceNumber
                   + "|" + blockHash + "|" + senderId;
        return raw.getBytes();
    }

    // ── Getters / Setters ─────────────────────────
    public Type   getType()            { return type; }
    public void   setType(Type t)      { this.type = t; }

    public int    getViewNumber()              { return viewNumber; }
    public void   setViewNumber(int v)         { this.viewNumber = v; }

    public int    getSequenceNumber()          { return sequenceNumber; }
    public void   setSequenceNumber(int s)     { this.sequenceNumber = s; }

    public String getBlockHash()               { return blockHash; }
    public void   setBlockHash(String h)       { this.blockHash = h; }

    public String getSenderId()                { return senderId; }
    public void   setSenderId(String id)       { this.senderId = id; }

    public String getPayload()                 { return payload; }
    public void   setPayload(String p)         { this.payload = p; }

    public String getSignature()               { return signature; }
    public void   setSignature(String s)       { this.signature = s; }

    public String getSchemeUsed()              { return schemeUsed; }
    public void   setSchemeUsed(String s)      { this.schemeUsed = s; }

    @Override
    public String toString() {
        return "[" + senderId + " | " + type + " | seq=" + sequenceNumber
             + " | block=" + blockHash + " | scheme=" + schemeUsed + "]";
    }
}