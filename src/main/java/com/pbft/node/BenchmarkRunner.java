package com.pbft.node;

import com.pbft.crypto.*;
import com.pbft.message.PBFTMessage;
import com.pbft.message.PBFTMessage.Type;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.bouncycastle.pqc.jcajce.spec.DilithiumParameterSpec;

import java.security.*;
import java.security.spec.ECGenParameterSpec;


/**
 * Phase 6 + Phase 7 – Standalone Benchmark
 *
 * Runs sign + verify N times for both ECDSA and Dilithium
 * and prints a comparison table.
 *
 * Added measurements:
 *
 * 1. Latency:
 *    - Measures total time taken for one PBFT signature operation
 *      (sign + verify)
 *
 * 2. Throughput:
 *    - Measures how many signature operations can be completed
 *      per second
 *
 *
 * Run with:
 *
 * mvn exec:java -Dexec.mainClass=com.pbft.node.BenchmarkRunner
 *
 *
 * (Does not need Spring Boot – no network or REST required.)
 */
public class BenchmarkRunner {


    // Register standard cryptography provider
    static {
        Security.addProvider(new BouncyCastleProvider());
    }


    // Register post-quantum cryptography provider
    static {
        Security.addProvider(new BouncyCastlePQCProvider());
    }



    /*
     * Number of benchmark iterations.
     *
     * Increased from 50 to 10000 because:
     *
     * - More rounds reduce timing noise
     * - Gives more accurate latency and throughput values
     */
    private static final int ROUNDS = 10000;



    public static void main(String[] args) throws Exception {


        System.out.println("=== PBFT Signature Benchmark ===");
        System.out.println("Rounds per scheme: " + ROUNDS);
        System.out.println();



        benchmark(
                "ECDSA",
                ecdsaKeyPair(),
                "SHA256withECDSA",
                "BC"
        );


        benchmark(
                "Dilithium",
                dilithiumKeyPair(),
                "Dilithium",
                "BCPQC"
        );

    }





    // ── Core benchmark logic ──────────────────────────────────────────────────


    private static void benchmark(
            String name,
            KeyPair kp,
            String algo,
            String provider
    ) throws Exception {



        // Build a representative PBFT message
        //
        // In a real PBFT system this represents:
        //
        // PRE-PREPARE / PREPARE / COMMIT messages
        //
        PBFTMessage msg = new PBFTMessage(
                Type.PREPARE,
                1,
                1,
                "abc123hash",
                "node-0",
                null
        );


        byte[] data = msg.signingBytes();



        long totalSign = 0;

        long totalVerify = 0;


        /*
         * Added for latency measurement.
         *
         * Measures:
         *
         * sign + verify
         *
         * This represents the complete cryptographic
         * processing delay for a PBFT message.
         */
        long totalLatency = 0;



        int sigSize = 0;



        /*
         * Start throughput timer.
         *
         * Throughput =
         *
         * completed operations / total time
         *
         */
        long benchmarkStart = System.nanoTime();





        for (int i = 0; i < ROUNDS; i++) {



            // ── Sign ──


            Signature signer =
                    Signature.getInstance(
                            algo,
                            provider
                    );


            signer.initSign(
                    kp.getPrivate()
            );


            signer.update(data);



            /*
             * Start measuring signature generation latency
             */
            long t0 = System.nanoTime();



            byte[] sig = signer.sign();



            long signEnd = System.nanoTime();



            totalSign += signEnd - t0;



            sigSize = sig.length;





            // ── Verify ──


            Signature verifier =
                    Signature.getInstance(
                            algo,
                            provider
                    );


            verifier.initVerify(
                    kp.getPublic()
            );


            verifier.update(data);



            /*
             * Start measuring verification latency
             */
            long t1 = System.nanoTime();



            verifier.verify(sig);



            long verifyEnd = System.nanoTime();



            totalVerify += verifyEnd - t1;




            /*
             * End-to-end latency
             *
             * Represents:
             *
             * Signature creation
             * +
             * Signature verification
             *
             */
            totalLatency +=
                    (verifyEnd - t0);

        }




        long benchmarkEnd =
                System.nanoTime();




        double avgSign =
                totalSign
                        / 1_000_000.0
                        / ROUNDS;



        double avgVerify =
                totalVerify
                        / 1_000_000.0
                        / ROUNDS;




        /*
         * Average PBFT cryptographic latency
         *
         * Converted from nanoseconds to milliseconds
         */
        double avgLatency =
                totalLatency
                        / 1_000_000.0
                        / ROUNDS;




        /*
         * Throughput calculation
         *
         * Operations completed per second
         */
        double seconds =
                (benchmarkEnd - benchmarkStart)
                        / 1_000_000_000.0;



        double throughput =
                ROUNDS / seconds;



        int pubKeyLen =
                kp.getPublic()
                        .getEncoded()
                        .length;




        System.out.printf(
                "%n%-12s%n",
                name
        );


        System.out.printf(
                "avg-sign       : %.4f ms%n",
                avgSign
        );


        System.out.printf(
                "avg-verify     : %.4f ms%n",
                avgVerify
        );


        System.out.printf(
                "avg-latency    : %.4f ms%n",
                avgLatency
        );


        System.out.printf(
                "throughput     : %.2f ops/sec%n",
                throughput
        );


        System.out.printf(
                "sig-size       : %d B%n",
                sigSize
        );


        System.out.printf(
                "pub-key        : %d B%n",
                pubKeyLen
        );


    }





    // ── Key generation helpers ────────────────────────────────────────────────



    private static KeyPair ecdsaKeyPair()
            throws Exception {


        KeyPairGenerator kpg =
                KeyPairGenerator.getInstance(
                        "EC",
                        "BC"
                );


        kpg.initialize(
                new ECGenParameterSpec(
                        "secp256k1"
                )
        );


        return kpg.generateKeyPair();

    }






    private static KeyPair dilithiumKeyPair()
            throws Exception {


        KeyPairGenerator kpg =
                KeyPairGenerator.getInstance(
                        "Dilithium",
                        "BCPQC"
                );


        kpg.initialize(
                DilithiumParameterSpec.dilithium2
        );


        return kpg.generateKeyPair();

    }


}