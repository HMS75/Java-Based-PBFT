package com.example.pbft.model;

/*
 * This is the message object exchanged between nodes.
 *
 * In PBFT-style communication, nodes exchange messages for different phases:
 * PRE_PREPARE, PREPARE, COMMIT.
 *
 * Each message contains:
 * - data: the payload / transaction
 * - type: which consensus phase this message belongs to
 * - senderId: which node sent it
 * - signature: digital signature over the data
 * - publicKey: sender's public key so that receiver can verify signature
 */

public class Message {

    private String data;
    private String type;
    private String senderId;
    private String signature;
    private String publicKey;

    public Message() {}

    public Message(String data, String type, String senderId, String signature, String publicKey) {
        this.data = data;
        this.type = type;
        this.senderId = senderId;
        this.signature = signature;
        this.publicKey = publicKey;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}