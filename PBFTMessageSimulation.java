public class PBFTMessageSimulation {

    public static void main(String[] args) throws Exception {

        // Create NodeA (it automatically generates its public/private keys)
        Node nodeA = new Node("NodeA");

        // Create NodeB (another node in the network)
        Node nodeB = new Node("NodeB");

        // Message that NodeA wants to send
        String message = "PREPARE: Block#100";

        // Print message to console
        System.out.println("NodeA signing message...");

        // NodeA signs the message using its PRIVATE key
        // The result is a digital signature stored as bytes
        byte[] signature = nodeA.signMessage(message);

        // NodeB will now verify the message
        System.out.println("NodeB verifying message...");

        // Verify the signature using NodeA's PUBLIC key
        boolean isValid = SignatureUtil.verifySignature(
                message,        // original message
                signature,      // digital signature
                nodeA.getPublicKey() // NodeA's public key
        );

        // If the signature matches the message
        if (isValid) {
            System.out.println("Signature VALID. Message accepted.");
        } 
        // If the signature is invalid
        else {
            System.out.println("Signature INVALID. Message rejected.");
        }
    }
}