// Import cryptography classes like Signature, PublicKey, etc.
import java.security.*;

// Utility class used for signature verification
public class SignatureUtil {

    // static → method can be used without creating an object
    // boolean → method returns true or false
    public static boolean verifySignature(String message, byte[] signatureBytes, PublicKey publicKey) throws Exception {

        // Create a Signature object using SHA256 hashing with RSA encryption
        Signature signature = Signature.getInstance("SHA256withRSA");

        // Initialize the signature object for verification using the sender's public key
        signature.initVerify(publicKey);

        // Provide the original message that was signed
        signature.update(message.getBytes());

        // Verify if the signature matches the message using the public key
        // returns true if valid, false if tampered or invalid
        return signature.verify(signatureBytes);
    }
}