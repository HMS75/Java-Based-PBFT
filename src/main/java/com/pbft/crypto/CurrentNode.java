package com.pbft.crypto;

/**
 * Simple static holder for the running node's ID.
 * Set once at startup by NodeConfig; read by SignatureScheme.sign().
 */
public class CurrentNode {
    public static String id = "node-0";
}