public class PBFTMessageSimulation {
    // In a PBFT system with four nodes, Node0 acts as the primary (leader) and the other three nodes (Node1, Node2, Node3) are replicas. 
    // In the PRE-PREPARE phase, Node0 creates a block or message and signs it with its private key, then sends it to all replicas. 
    // Each replica verifies the signature to ensure the message indeed came from Node0. Next, in the PREPARE phase, the replicas broadcast 
    // their agreement on the received message to all other nodes, confirming that they all saw the same block. 
    // Node0 then verifies the PREPARE messages from each replica to ensure consensus. Once enough nodes have agreed, 
    // the system enters the COMMIT phase, where the nodes finalize the decision and the block is added to the ledger. 
    // This process ensures that all non-faulty nodes agree on the same block even if some nodes behave incorrectly, maintaining safety 
    // and consistency across the network.
    
    public static void main(String[] args) throws Exception {

        // Create 4 nodes 
        Node node0 = new Node("Node0");
        Node node1 = new Node("Node1");
        Node node2 = new Node("Node2");
        Node node3 = new Node("Node3");

        // Message that NodeA wants to send
        String message = "PRE-PREPARE: Block#100";

        // Print message to console
        System.out.println("Node0 created PRE-PREPARE message");

        // NodeA signs the message using its PRIVATE key
        // The result is a digital signature stored as bytes
        byte[] signature = node0.signMessage(message);

        // Other nodes (1,2,3) will verify the Pre-Prepare 
        if(SignatureUtil.verifySignature(message, signature, node0.getPublicKey()))
            System.out.println("Node1 verified PRE-PREPARE from Node0");

        if(SignatureUtil.verifySignature(message, signature, node0.getPublicKey()))
            System.out.println("Node2 verified PRE-PREPARE from Node0");

        if(SignatureUtil.verifySignature(message, signature, node0.getPublicKey()))
            System.out.println("Node3 verified PRE-PREPARE from Node0");

        // Prepare Phase - where the nodes broadcast 
        System.out.println("\nNode1 broadcasting PREPARE");
        System.out.println("Node2 broadcasting PREPARE");
        System.out.println("Node3 broadcasting PREPARE");

        // Node0 Verifies PREPARE Messages
        // Each node signs the prepare message.

        String prepareMsg = "PREPARE: Block#100";

        byte[] sig1 = node1.signMessage(prepareMsg);
        byte[] sig2 = node2.signMessage(prepareMsg);
        byte[] sig3 = node3.signMessage(prepareMsg);

        if(SignatureUtil.verifySignature(prepareMsg, sig1, node1.getPublicKey()))
            System.out.println("\nNode0 verified PREPARE from Node1");

        if(SignatureUtil.verifySignature(prepareMsg, sig2, node2.getPublicKey()))
            System.out.println("Node0 verified PREPARE from Node2");

        if(SignatureUtil.verifySignature(prepareMsg, sig3, node3.getPublicKey()))
            System.out.println("Node0 verified PREPARE from Node3");

        // Commit Phase 

        System.out.println("\nNodes entering COMMIT phase");
        System.out.println("Block committed successfully");
    }
}