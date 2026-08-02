package com.pbft.network;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbft.message.PBFTMessage;
import com.pbft.message.PBFTMessage.Type;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.List;

/**
 * Phase 4 (REST) + Phase 5 (TCP) – Message Broadcaster
 *
 * Sends a PBFTMessage to every peer node.
 * Strategy is selected by node.transport property:
 *   REST (default) – HTTP POST to /preprepare | /prepare | /commit
 *   TCP            – raw JSON over a Socket connection
 */
@Component
public class MessageBroadcaster {

    @Value("#{'${node.peers}'.split(',')}")
    private List<String> peers;           // e.g. ["http://localhost:8080", ...]

    @Value("${node.id:0}")      private int    myId;
    @Value("${node.transport:REST}") private String transport;
    @Value("${node.tcp-port:9080}")  private int    tcpBasePort; // node-0 = 9080, node-1 = 9081 …

    private final ObjectMapper json = new ObjectMapper();

    // ── Public API ────────────────────────────────────────────────────────────

    public void broadcast(PBFTMessage msg) {
        for (int i = 0; i < peers.size(); i++) {
            if (i == myId) continue;   // don't send to self

            if ("TCP".equalsIgnoreCase(transport)) {
                sendViaTcp(msg, i);
            } else {
                sendViaRest(msg, peers.get(i));
            }
        }
    }

    // ── Phase 4: REST ─────────────────────────────────────────────────────────

    private void sendViaRest(PBFTMessage msg, String peerBaseUrl) {
        try {
            String endpoint = peerBaseUrl + "/" + endpointFor(msg.getType());
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            byte[] body = json.writeValueAsBytes(msg);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body);
            }
            int code = conn.getResponseCode();
            System.out.println("[REST] → " + endpoint + " | HTTP " + code);
            conn.disconnect();
        } catch (Exception ex) {
            System.err.println("[REST] Failed to reach peer: " + ex.getMessage());
        }
    }

    // ── Phase 5: TCP ──────────────────────────────────────────────────────────

    private void sendViaTcp(PBFTMessage msg, int targetNodeId) {
        int port = tcpBasePort + targetNodeId;
        try (Socket socket = new Socket("localhost", port);
             OutputStream os = socket.getOutputStream()) {

            byte[] data = json.writeValueAsBytes(msg);
            // Simple framing: 4-byte big-endian length prefix + payload
            byte[] frame = new byte[4 + data.length];
            frame[0] = (byte) (data.length >> 24);
            frame[1] = (byte) (data.length >> 16);
            frame[2] = (byte) (data.length >> 8);
            frame[3] = (byte) (data.length);
            System.arraycopy(data, 0, frame, 4, data.length);

            os.write(frame);
            os.flush();
            System.out.println("[TCP] → localhost:" + port + " | " + data.length + " bytes");
        } catch (Exception ex) {
            System.err.println("[TCP] Failed to reach node-" + targetNodeId + ": " + ex.getMessage());
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String endpointFor(Type type) {
        return switch (type) {
            case PRE_PREPARE -> "preprepare";
            case PREPARE     -> "prepare";
            case COMMIT      -> "commit";
        };
    }
}