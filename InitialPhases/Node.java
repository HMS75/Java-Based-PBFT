// Import cryptography classes
import java.security.*;

// Node class represents a participant in the PBFT network
public class Node {

    // Stores public and private keys
    private KeyPair keyPair;

    // Unique identifier for the node
    private String nodeId;

    // Constructor to create a node with its ID
    public Node(String nodeId) throws Exception {

        this.nodeId = nodeId;

        // Generate cryptographic key pair for this node
        this.keyPair = generateKeyPair();
    }

    // Generates RSA public/private key pair
    private KeyPair generateKeyPair() throws Exception {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");

        // 2048-bit RSA key for strong security
        keyGen.initialize(2048);

        return keyGen.generateKeyPair();
    }

    // Signs a message using the node's private key
    public byte[] signMessage(String message) throws Exception {

        Signature signature = Signature.getInstance("SHA256withRSA");

        // Use private key to sign message
        signature.initSign(keyPair.getPrivate());

        signature.update(message.getBytes());

        return signature.sign();
    }

    // Returns the node's public key (used by others to verify signatures)
    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    // Returns the node ID
    public String getNodeId() {
        return nodeId;
    }

    // ------------------------------------------------
    // PBFT PHASE 1 : PRE-PREPARE
    // Primary node creates and signs the block/message
    // ------------------------------------------------
    public byte[] prePrepare(String message) throws Exception {

        System.out.println(nodeId + " created PRE-PREPARE message");

        // Sign the message using this node's private key
        return signMessage(message);
    }

    // ------------------------------------------------
    // PBFT PHASE 2 : PREPARE
    // Replica nodes broadcast agreement
    // ------------------------------------------------
    public byte[] prepare(String message) throws Exception {

        System.out.println(nodeId + " broadcasting PREPARE");

        // Sign the prepare message
        return signMessage(message);
    }

    // ------------------------------------------------
    // PBFT PHASE 3 : COMMIT
    // Nodes finalize the block
    // ------------------------------------------------
    public void commit(String block) {

        System.out.println(nodeId + " committed " + block);
    }
}