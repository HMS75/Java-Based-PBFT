import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class PBFTMessageSimulation {

    public static void main(String[] args) throws Exception {

        // Scanner object to take user input
        Scanner scanner = new Scanner(System.in);

        // Ask user for number of nodes
        System.out.print("Enter number of nodes in PBFT network: ");
        int totalNodes = scanner.nextInt();

        // Store nodes dynamically
        List<Node> nodes = new ArrayList<>();

        // Create nodes automatically
        for (int i = 0; i < totalNodes; i++) {
            nodes.add(new Node("Node" + i));
        }

        // Node0 is the primary
        Node primary = nodes.get(0);

        String message = "PRE-PREPARE: Block#100";

        // -------------------------
        // PRE-PREPARE PHASE
        // -------------------------

        byte[] signature = primary.prePrepare(message);

        // Replicas verify the message
        for (int i = 1; i < nodes.size(); i++) {

            Node replica = nodes.get(i);

            if (SignatureUtil.verifySignature(message, signature, primary.getPublicKey())) {
                System.out.println(replica.getNodeId() +
                        " verified PRE-PREPARE from " + primary.getNodeId());
            }
        }

        // -------------------------
        // PREPARE PHASE
        // -------------------------

        String prepareMsg = "PREPARE: Block#100";
        List<byte[]> prepareSignatures = new ArrayList<>();

        for (int i = 1; i < nodes.size(); i++) {

            Node replica = nodes.get(i);

            byte[] sig = replica.prepare(prepareMsg);

            prepareSignatures.add(sig);
        }

        // Primary verifies prepare messages
        System.out.println("\nPrimary verifying PREPARE messages");

        for (int i = 1; i < nodes.size(); i++) {

            Node replica = nodes.get(i);

            if (SignatureUtil.verifySignature(
                    prepareMsg,
                    prepareSignatures.get(i - 1),
                    replica.getPublicKey())) {

                System.out.println(primary.getNodeId() +
                        " verified PREPARE from " + replica.getNodeId());
            }
        }

        // -------------------------
        // COMMIT PHASE
        // -------------------------

        System.out.println("\nEntering COMMIT phase");

        for (Node node : nodes) {
            node.commit("Block#100");
        }

        System.out.println("\nBlock committed successfully");

        scanner.close();
    }
}