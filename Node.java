// Import all classes from the java.security package
// This package contains classes for cryptography like KeyPair, Signature, etc.
import java.security.*;

// Node class represents a participant in a system (for example, a node in a blockchain network).
public class Node {

    // KeyPair is a built in class that stores two keys:
    // 1. Public Key  -> shared with others
    // 2. Private Key -> kept secret by the owner
    private KeyPair keyPair;

    // A unique identifier for this node
    private String nodeId;

    public Node(String nodeId) throws Exception {

        this.nodeId = nodeId;

        // Generate a new public/private key pair for this node
        this.keyPair = generateKeyPair();
    }

    // This method generates a public/private RSA key pair
    // private -> can only be used inside this class
    // throws Exception -> indicates that errors may occur during cryptographic operations
    private KeyPair generateKeyPair() throws Exception {

        // KeyPairGenerator is used to generate cryptographic key pairs
        // "RSA" is the algorithm used for encryption/signing
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");

        // Set the key size to 2048 bits
        // Larger key size = more secure but slightly slower
        keyGen.initialize(2048);

        // Generate and return the key pair (public + private)
        return keyGen.generateKeyPair();
    }

    // This method signs a message using the node's private key
    // A digital signature proves that the message came from this node
    public byte[] signMessage(String message) throws Exception {

        // Signature class is used for creating and verifying digital signatures
        // "SHA256withRSA" means:
        // 1. SHA-256 -> hashing algorithm
        // 2. RSA     -> encryption/signing algorithm
        Signature signature = Signature.getInstance("SHA256withRSA");

        // Initialize the signature object for signing
        // We use the PRIVATE key because only the owner should sign messages
        signature.initSign(keyPair.getPrivate());

        // Convert the message into bytes and give it to the signature object
        // update() feeds the data that needs to be signed
        signature.update(message.getBytes());

        // Generate the digital signature and return it as a byte array
        return signature.sign();
    }

    // Getter method to retrieve the public key
    // Other nodes can use this public key to verify signatures from this node
    public PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    // Getter method to retrieve the node's ID
    public String getNodeId() {
        return nodeId;
    }
}