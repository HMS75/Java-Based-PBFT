package com.pbft.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbft.consensus.ConsensusEngine;
import com.pbft.crypto.SignatureVerifier;
import com.pbft.message.PBFTMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Phase 5 – Custom TCP Server
 *
 * Listens on node.tcp-port.
 * Each message is framed as:  [4-byte length][JSON bytes]
 * After reading, it verifies the signature and hands the message
 * to ConsensusEngine exactly like the REST controller does.
 */
@Component
public class TcpServer {

    @Autowired private SignatureVerifier verifier;
    @Autowired @Lazy private ConsensusEngine consensus;
    
    private final ObjectMapper json = new ObjectMapper();

    public void start(int port) {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[TCP] Listening on port " + port);
            while (true) {
                Socket client = server.accept();
                // Handle each connection in its own thread
                new Thread(() -> handle(client), "tcp-handler").start();
            }
        } catch (IOException ex) {
            System.err.println("[TCP] Server error: " + ex.getMessage());
        }
    }

    private void handle(Socket socket) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
            // Read 4-byte length prefix
            int length = in.readInt();
            byte[] data = new byte[length];
            in.readFully(data);

            PBFTMessage msg = json.readValue(data, PBFTMessage.class);
            System.out.println("[TCP] Received " + msg);

            if (!verifier.verify(msg)) {
                System.err.println("[TCP] Signature invalid – discarding " + msg.getSenderId());
                return;
            }
            route(msg);
        } catch (Exception ex) {
            System.err.println("[TCP] Handler error: " + ex.getMessage());
        }
    }

    private void route(PBFTMessage msg) {
        switch (msg.getType()) {
            case PRE_PREPARE -> consensus.handlePrePrepare(msg);
            case PREPARE     -> consensus.handlePrepare(msg);
            case COMMIT      -> consensus.handleCommit(msg);
        }
    }
}