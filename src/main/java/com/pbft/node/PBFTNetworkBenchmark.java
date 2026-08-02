package com.pbft.node;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * PBFT Network Benchmark — measures TRUE consensus latency
 *
 * Each transaction measures the full pipeline:
 *
 *   Client sends /propose
 *         ↓
 *   Primary signs PRE-PREPARE → broadcasts to all replicas
 *         ↓
 *   Replicas verify → broadcast PREPARE
 *         ↓
 *   PREPARE quorum (≥3) → broadcast COMMIT
 *         ↓
 *   COMMIT quorum (≥3) → block finalized
 *         ↓
 *   /propose HTTP response returns "COMMITTED"
 *         ↓
 *   Benchmark records latency
 *
 * The /propose endpoint BLOCKS until COMMIT quorum is reached.
 * So the HTTP response time = true end-to-end PBFT consensus time.
 *
 * Run AFTER starting all 4 nodes:
 *   mvn exec:java -Dexec.mainClass=com.pbft.node.PBFTNetworkBenchmark
 *
 * Change SCHEME below to match what your nodes are running.
 */
public class PBFTNetworkBenchmark {

    private static final int    TRANSACTIONS  = 100;
    private static final String PRIMARY_URL   = "http://localhost:8080/propose";

    // Must match --node.signature-scheme on all running nodes
    private static final String SCHEME        = "ECDSA";   // or DILITHIUM

    // Timeout per transaction — must be > node.commit-timeout (default 10s)
    private static final int    TIMEOUT_SEC   = 15;

    public static void main(String[] args) throws Exception {

        System.out.println("=== PBFT Network Benchmark ===");
        System.out.println("Scheme       : " + SCHEME);
        System.out.println("Transactions : " + TRANSACTIONS);
        System.out.println("Primary      : " + PRIMARY_URL);
        System.out.println();
        System.out.println("Waiting 2s for nodes to be ready...");
        Thread.sleep(2000);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        List<Double> latencies = new ArrayList<>();
        int committed = 0;
        int timedOut  = 0;
        int errors    = 0;

        long benchmarkStart = System.nanoTime();

        for (int i = 0; i < TRANSACTIONS; i++) {

            String json = "{\"data\":\"tx-" + SCHEME + "-" + i + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PRIMARY_URL))
                    .timeout(Duration.ofSeconds(TIMEOUT_SEC))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            long txStart = System.nanoTime();

            try {
                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString()
                );
                long txEnd     = System.nanoTime();
                double latency = (txEnd - txStart) / 1_000_000.0;  // ms

                String body = response.body();

                if (response.statusCode() == 200 && body.startsWith("COMMITTED")) {
                    // Full consensus completed
                    latencies.add(latency);
                    committed++;
                    System.out.printf("TX %3d ✅ COMMITTED  latency=%.2f ms%n", i, latency);

                } else if (response.statusCode() == 504) {
                    timedOut++;
                    System.out.printf("TX %3d ⏱ TIMEOUT    (consensus did not complete)%n", i);

                } else {
                    errors++;
                    System.out.printf("TX %3d ❌ ERROR  HTTP=%d  body=%s%n",
                                      i, response.statusCode(), body);
                }

            } catch (Exception ex) {
                errors++;
                System.out.printf("TX %3d ❌ EXCEPTION  %s%n", i, ex.getMessage());
            }
        }

        long benchmarkEnd  = System.nanoTime();
        double totalSec    = (benchmarkEnd - benchmarkStart) / 1_000_000_000.0;

        // ── Aggregate metrics ─────────────────────────────────────────────────

        double avgLatency = latencies.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        double minLatency = latencies.stream()
                .mapToDouble(Double::doubleValue)
                .min()
                .orElse(0);

        double maxLatency = latencies.stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0);

        // Throughput = only successfully committed transactions / total wall time
        double throughput = committed / totalSec;

        System.out.println();
        System.out.println("========== PBFT BENCHMARK RESULTS ==========");
        System.out.printf("Scheme            : %s%n",     SCHEME);
        System.out.printf("Total TX sent     : %d%n",     TRANSACTIONS);
        System.out.printf("Committed (✅)    : %d%n",     committed);
        System.out.printf("Timed out  (⏱)   : %d%n",     timedOut);
        System.out.printf("Errors     (❌)   : %d%n",     errors);
        System.out.println("--------------------------------------------");
        System.out.printf("Avg Latency       : %.4f ms%n", avgLatency);
        System.out.printf("Min Latency       : %.4f ms%n", minLatency);
        System.out.printf("Max Latency       : %.4f ms%n", maxLatency);
        System.out.printf("Throughput        : %.2f TPS%n", throughput);
        System.out.printf("Total Wall Time   : %.2f s%n",  totalSec);
        System.out.println("============================================");

        if (timedOut > 0) {
            System.out.println();
            System.out.println("⚠ Timeouts detected. Check that:");
            System.out.println("  1. All 4 nodes are running with the SAME --node.signature-scheme");
            System.out.println("  2. mvn clean package was run after the key-seed fix");
            System.out.println("  3. node.commit-timeout in application.properties is set (default 10000ms)");
        }
    }
}