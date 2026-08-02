package com.pbft.consensus;

import com.pbft.crypto.CurrentNode;
import com.pbft.crypto.SignatureScheme;
import com.pbft.message.PBFTMessage;
import com.pbft.message.PBFTMessage.Type;
import com.pbft.network.MessageBroadcaster;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * PBFT Consensus Engine
 *
 * Tracks PREPARE and COMMIT votes per sequence number.
 * When COMMIT quorum is reached, unblocks any thread waiting
 * on awaitFinalized(seq) — used by /propose to return only
 * after full consensus completes.
 *
 * Quorum = 2f + 1  where  n = 3f + 1
 */
@Service
public class ConsensusEngine {

    @Autowired private SignatureScheme    scheme;
    @Autowired private MessageBroadcaster broadcaster;

    @Value("${node.total:4}") private int totalNodes;

    // seqNo → senderIds that sent PREPARE
    private final Map<Integer, Set<String>> prepareVotes    = new ConcurrentHashMap<>();
    // seqNo → senderIds that sent COMMIT
    private final Map<Integer, Set<String>> commitVotes     = new ConcurrentHashMap<>();
    // seqNo → finalized block hash
    private final Map<Integer, String>      finalizedBlocks = new ConcurrentHashMap<>();

    // seqNo → latch released when that seq reaches COMMIT quorum
    // The /propose thread blocks on this latch to wait for true consensus.
    private final Map<Integer, CountDownLatch> commitLatches = new ConcurrentHashMap<>();

    // ── PRE-PREPARE ───────────────────────────────────────────────────────────

    public void handlePrePrepare(PBFTMessage msg) {
        System.out.println("[Consensus] PRE-PREPARE received " + msg);

        PBFTMessage prepare = new PBFTMessage(
            Type.PREPARE,
            msg.getViewNumber(),
            msg.getSequenceNumber(),
            msg.getBlockHash(),
            CurrentNode.id,
            null
        );
        signAndBroadcast(prepare);

        // Primary also counts its own PREPARE vote
        handlePrepare(prepare);
    }

    // ── PREPARE ───────────────────────────────────────────────────────────────

    public void handlePrepare(PBFTMessage msg) {
        int seq = msg.getSequenceNumber();
        prepareVotes.computeIfAbsent(seq, k -> ConcurrentHashMap.newKeySet())
                    .add(msg.getSenderId());

        int votes  = prepareVotes.get(seq).size();
        int quorum = quorumSize();

        System.out.println("[Consensus] PREPARE from " + msg.getSenderId()
                         + " | votes=" + votes + "/" + quorum);

        // Only send COMMIT once per node per sequence
        if (votes >= quorum) {
            commitVotes.computeIfAbsent(seq, k -> ConcurrentHashMap.newKeySet());
            if (!commitVotes.get(seq).contains(CurrentNode.id)) {
                PBFTMessage commit = new PBFTMessage(
                    Type.COMMIT,
                    msg.getViewNumber(),
                    seq,
                    msg.getBlockHash(),
                    CurrentNode.id,
                    null
                );
                signAndBroadcast(commit);
                // Count own COMMIT vote immediately
                handleCommit(commit);
            }
        }
    }

    // ── COMMIT ────────────────────────────────────────────────────────────────

    public void handleCommit(PBFTMessage msg) {
        int seq = msg.getSequenceNumber();
        commitVotes.computeIfAbsent(seq, k -> ConcurrentHashMap.newKeySet())
                   .add(msg.getSenderId());

        int votes  = commitVotes.get(seq).size();
        int quorum = quorumSize();

        System.out.println("[Consensus] COMMIT from " + msg.getSenderId()
                         + " | votes=" + votes + "/" + quorum);

        if (votes >= quorum && !finalizedBlocks.containsKey(seq)) {
            finalizedBlocks.put(seq, msg.getBlockHash());
            System.out.println("✅ [Consensus] BLOCK FINALIZED  seq=" + seq
                             + "  hash=" + msg.getBlockHash()
                             + "  scheme=" + scheme.schemeName());

            // Unblock any thread waiting for this sequence to finalize
            CountDownLatch latch = commitLatches.get(seq);
            if (latch != null) latch.countDown();
        }
    }

    // ── Blocking wait used by /propose ────────────────────────────────────────

    /**
     * Registers a latch for this sequence number and blocks until
     * COMMIT quorum is reached or timeoutMs elapses.
     *
     * Returns true if finalized within timeout, false if timed out.
     */
    public boolean awaitFinalized(int seq, long timeoutMs) throws InterruptedException {
        // If already finalized (race-free check)
        if (finalizedBlocks.containsKey(seq)) return true;

        CountDownLatch latch = new CountDownLatch(1);
        commitLatches.put(seq, latch);

        // Re-check after registering latch to avoid missing the signal
        if (finalizedBlocks.containsKey(seq)) {
            latch.countDown();
        }

        boolean completed = latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        commitLatches.remove(seq);
        return completed;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void signAndBroadcast(PBFTMessage msg) {
        try {
            long t0       = System.nanoTime();
            byte[] sig    = scheme.sign(msg.signingBytes());
            long signMs   = (System.nanoTime() - t0) / 1_000_000;

            msg.setSignature(Base64.getEncoder().encodeToString(sig));
            msg.setSchemeUsed(scheme.schemeName());

            System.out.println("[Metrics] Sign time=" + signMs + "ms  sigSize="
                             + sig.length + "B  scheme=" + scheme.schemeName());
            broadcaster.broadcast(msg);
        } catch (Exception ex) {
            System.err.println("[Consensus] Signing failed: " + ex.getMessage());
        }
    }

    /** 2f + 1  where f = (n-1)/3 */
    private int quorumSize() {
        int f = (totalNodes - 1) / 3;
        return 2 * f + 1;
    }

    public Map<Integer, String> getFinalizedBlocks() { return finalizedBlocks; }
}