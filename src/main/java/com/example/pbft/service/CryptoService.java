package com.example.pbft.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/*
 * CryptoService handles digital signature operations.
 *
 * Current role:
 * - sign outgoing messages
 * - verify incoming messages
 *
 * For research comparison:
 * this is the exact place where ECDSA can later be replaced
 * with a post-quantum scheme such as Dilithium.
 */

@Service
public class CryptoService {

    public String sign(String data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] signedBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signedBytes);
    }

    public boolean verify(String data, String base64Signature, String base64PublicKey) throws Exception {
        byte[] signatureBytes = Base64.getDecoder().decode(base64Signature);
        byte[] publicKeyBytes = Base64.getDecoder().decode(base64PublicKey);

        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initVerify(publicKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));

        return signature.verify(signatureBytes);
    }

    public String encodePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
}