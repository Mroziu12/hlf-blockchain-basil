/*
 * Copyright IBM Corp. All Rights Reserved.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;

public final class App {

    // ANSI Color Codes for better CLI presentation
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_PURPLE = "\u001B[35m";

    // path to your test-network directory included, e.g.: Paths.get("..", "..", "test-network")
    private static final Path PATH_TO_TEST_NETWORK = Paths.get("..","..", "fabric-samples", "test-network");

    private static final String CHANNEL_NAME = System.getenv().getOrDefault("CHANNEL_NAME", "mychannel");
    private static final String CHAINCODE_NAME = System.getenv().getOrDefault("CHAINCODE_NAME", "basic");

    // Gateway peer end point.
    private static final String PEER_ENDPOINT = "localhost:7051";
    private static final String OVERRIDE_AUTH = "peer0.org1.example.com";

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(final String[] args) throws Exception {

        System.out.println(ANSI_CYAN + "==========================================================" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "🌱 Basil Traceability Application - Fabric Gateway Client" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "==========================================================" + ANSI_RESET);

        ManagedChannel channel = null;
        try {
            ChannelCredentials credentials = TlsChannelCredentials.newBuilder()
                    .trustManager(PATH_TO_TEST_NETWORK.resolve(Paths.get(
                            "organizations/peerOrganizations/org1.example.com/" +
                            "peers/peer0.org1.example.com/tls/ca.crt"))
                            .toFile())
                    .build();
            // The gRPC client connection should be shared by all Gateway connections to
            // this endpoint.
            channel = Grpc.newChannelBuilder(PEER_ENDPOINT, credentials)
                    .overrideAuthority(OVERRIDE_AUTH)
                    .build();
            
            // --- Gateway Setup for Org1 (Pittaluga & fratelli) ---
            Gateway.Builder builderOrg1 = Gateway.newInstance()
                    .identity(new X509Identity("Org1MSP",
                            Identities.readX509Certificate(
                                Files.newBufferedReader(
                                    PATH_TO_TEST_NETWORK.resolve(Paths.get(
                                        "organizations/peerOrganizations/org1.example.com/" +
                                        "users/User1@org1.example.com/msp/signcerts/cert.pem"
                                    ))
                                )
                            )
                        ))
                    .signer(
                        Signers.newPrivateKeySigner(
                            Identities.readPrivateKey(
                                Files.newBufferedReader(
                                    Files.list(PATH_TO_TEST_NETWORK.resolve(
                                        Paths.get(
                                            "organizations/peerOrganizations/org1.example.com/" +
                                            "users/User1@org1.example.com/msp/keystore")
                                        )
                                    ).findFirst().orElseThrow()
                                )
                            )
                        )
                    )
                    .connection(channel)
                    // Default timeouts for different gRPC calls
                    .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                    .endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
                    .submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                    .commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));

            // --- Gateway Setup for Org2 (Supermarket) ---
            Gateway.Builder builderOrg2 = Gateway.newInstance()
                    .identity(new X509Identity("Org2MSP",
                            Identities.readX509Certificate(Files.newBufferedReader(PATH_TO_TEST_NETWORK.resolve(Paths.get(
                                "organizations/peerOrganizations/org2.example.com/users/User1@org2.example.com/msp/signcerts/cert.pem"))))))
                    .signer(Signers.newPrivateKeySigner(Identities.readPrivateKey(Files.newBufferedReader(Files
                        .list(PATH_TO_TEST_NETWORK.resolve(Paths
                                .get("organizations/peerOrganizations/org2.example.com/users/User1@org2.example.com/msp/keystore")))
                        .findFirst().orElseThrow()))))
                    .connection(channel)
                    .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                    .endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
                    .submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                    .commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));
            
            try (Gateway gatewayOrg1 = builderOrg1.connect();
                    Gateway gatewayOrg2 = builderOrg2.connect()) {
                
                Contract contractOrg1 = gatewayOrg1
                    .getNetwork(CHANNEL_NAME)
                    .getContract(CHAINCODE_NAME);
                
                Contract contractOrg2 = gatewayOrg2
                    .getNetwork(CHANNEL_NAME)
                    .getContract(CHAINCODE_NAME);
                
                var scanner = new Scanner(System.in);
                
                while (true) {
                    
                    // --- Organization Selection Menu ---
                    System.out.println(ANSI_BLUE + "\n--- Select Interacting Organization ---" + ANSI_RESET);
                    System.out.println(ANSI_YELLOW + "  0:" + ANSI_RESET + " Pittaluga & fratelli (Org1MSP)");
                    System.out.println(ANSI_YELLOW + "  1:" + ANSI_RESET + " Supermarket (Org2MSP)");
                    System.out.print(ANSI_BLUE + "Enter organization index (0 or 1): " + ANSI_RESET);
                    
                    String orgIndex = scanner.nextLine().trim();
                    Contract interactingContract;
                    String orgName;
                    
                    switch (orgIndex) {
                        case "0": // Org1MSP
                            interactingContract = contractOrg1;
                            orgName = "Pittaluga & fratelli (Org1MSP)";
                            break;
                        case "1": // Org2MSP
                            interactingContract = contractOrg2;
                            orgName = "Supermarket (Org2MSP)";
                            break;
                        default:
                            System.out.println(ANSI_RED + "\n❌ ERROR: Wrong organization index. Please try again." + ANSI_RESET);
                            continue;
                    }
                    
                    System.out.println(ANSI_GREEN + "\n✅ Selected Organization: " + orgName + ANSI_RESET);

                    // --- Transaction Selection Menu ---
                    System.out.println(ANSI_BLUE + "\n--- Select Transaction to Execute ---" + ANSI_RESET);
                    System.out.println(ANSI_YELLOW + "  0:" + ANSI_RESET + " QueryBasil (Read)");
                    System.out.println(ANSI_YELLOW + "  1:" + ANSI_RESET + " CreateBasil (Write)");
                    System.out.println(ANSI_YELLOW + "  2:" + ANSI_RESET + " UpdateBasil (Write)");
                    System.out.println(ANSI_YELLOW + "  3:" + ANSI_RESET + " TransferBasil (Write)");
                    System.out.println(ANSI_YELLOW + "  4:" + ANSI_RESET + " GetHistoryBasil (Read)");
                    System.out.println(ANSI_YELLOW + "  5:" + ANSI_RESET + " DeleteBasil (Write)");
                    System.out.print(ANSI_BLUE + "Enter transaction index (0-5): " + ANSI_RESET);
                    
                    String txIndex = scanner.nextLine().trim();
                    byte[] result;
                    String basilQR, extraInfo, gpsLocation, newOwnerIndex, newOwner;

                    try {
                        switch (txIndex) {
                            case "0": // QueryBasil (Read)
                                System.out.print(ANSI_PURPLE + "\nInput Basil QR Code: " + ANSI_RESET);
                                basilQR = scanner.nextLine().trim();
                                System.out.println(ANSI_CYAN + "Executing QueryBasil..." + ANSI_RESET);
                                result = interactingContract.evaluateTransaction("QueryBasil", basilQR);
                                System.out.println(ANSI_GREEN + "\n*** QUERY RESULT ***" + ANSI_RESET);
                                System.out.println(new App().prettyJson(result)); // Use pretty print for JSON
                                System.out.println(ANSI_GREEN + "********************" + ANSI_RESET);
                                break;

                            case "1": // CreateBasil (Write)
                                System.out.print(ANSI_PURPLE + "\nInput Basil QR Code: " + ANSI_RESET);
                                basilQR = scanner.nextLine().trim();
                                System.out.print(ANSI_PURPLE + "Input Extra Info: " + ANSI_RESET);
                                extraInfo = scanner.nextLine().trim();
                                
                                System.out.println(ANSI_CYAN + "Submitting CreateBasil transaction..." + ANSI_RESET);
                                result = interactingContract.submitTransaction("CreateBasil", basilQR, extraInfo);
                                String resultString = new String(result).trim();
                                    
                                    // --- UPDATED FAILURE CHECK ---
                                    if (resultString.contains("Not authorized") || resultString.contains("exists")) {
                                        System.out.println(ANSI_RED + "\n❌ TRANSACTION FAILED (Chaincode Logic Error)" + ANSI_RESET);
                                        System.out.println(ANSI_RED + "Chaincode Response: " + resultString + ANSI_RESET);
                                    } else {
                                        System.out.println(ANSI_GREEN + "\n✅ TRANSACTION SUCCESSFUL (TxID: " + resultString + ")" + ANSI_RESET);
                                    }
                                break;

                            case "2": // UpdateBasil (Write)
                                System.out.print(ANSI_PURPLE + "\nInput Basil QR Code: " + ANSI_RESET);
                                basilQR = scanner.nextLine().trim();
                                System.out.print(ANSI_PURPLE + "Input NEW Extra Info: " + ANSI_RESET);
                                extraInfo = scanner.nextLine().trim();
                                System.out.print(ANSI_PURPLE + "Input NEW GPS Location: " + ANSI_RESET);
                                gpsLocation = scanner.nextLine().trim();
                                
                                System.out.println(ANSI_CYAN + "Submitting UpdateBasil transaction..." + ANSI_RESET);
                                result = interactingContract.submitTransaction("UpdateBasil", basilQR, extraInfo, gpsLocation);
                                resultString = new String(result).trim();
                                    
                                    // --- UPDATED FAILURE CHECK ---
                                    if (resultString.contains("Not authorized") || resultString.contains("exists")) {
                                        System.out.println(ANSI_RED + "\n❌ TRANSACTION FAILED (Chaincode Logic Error)" + ANSI_RESET);
                                        System.out.println(ANSI_RED + "Chaincode Response: " + resultString + ANSI_RESET);
                                    } else {
                                        System.out.println(ANSI_GREEN + "\n✅ TRANSACTION SUCCESSFUL (TxID: " + resultString + ")" + ANSI_RESET);
                                    }
                                break;

                            case "3": // TransferBasil (Write)
                                System.out.print(ANSI_PURPLE + "\nInput Basil QR Code: " + ANSI_RESET);
                                basilQR = scanner.nextLine().trim();
                                
                                System.out.println(ANSI_BLUE + "\n--- Select New Owner Organization ---" + ANSI_RESET);
                                System.out.println(ANSI_YELLOW + "  0:" + ANSI_RESET + " Pittaluga & fratelli (Org1MSP)");
                                System.out.println(ANSI_YELLOW + "  1:" + ANSI_RESET + " Supermarket (Org2MSP)");
                                System.out.print(ANSI_BLUE + "Enter new owner index (0 or 1): " + ANSI_RESET);
                                
                                newOwnerIndex = scanner.nextLine().trim();
                                switch (newOwnerIndex) {
                                    case "0": newOwner = "Org1MSP"; break;
                                    case "1": newOwner = "Org2MSP"; break;
                                    default: throw new RuntimeException("Wrong new owner organization index");
                                }
                                
                                System.out.println(ANSI_CYAN + "Submitting TransferBasil transaction to " + newOwner + "..." + ANSI_RESET);
                                result = interactingContract.submitTransaction("TransferBasil", basilQR, newOwner);

                                resultString = new String(result).trim();
                                    // --- UPDATED FAILURE CHECK ---
                                    if (resultString.contains("Not authorized") || resultString.contains("exists")) {
                                        System.out.println(ANSI_RED + "\n❌ TRANSACTION FAILED (Chaincode Logic Error)" + ANSI_RESET);
                                        System.out.println(ANSI_RED + "Chaincode Response: " + resultString + ANSI_RESET);
                                    } else {
                                        System.out.println(ANSI_GREEN + "\n✅ TRANSACTION SUCCESSFUL (TxID: " + resultString + ")" + ANSI_RESET);
                                    }
                                break;

                            case "4": // GetHistoryOfBasil (Read)
                                System.out.print(ANSI_PURPLE + "\nInput Basil QR Code: " + ANSI_RESET);
                                basilQR = scanner.nextLine().trim();
                                
                                System.out.println(ANSI_CYAN + "Executing GetHistoryOfBasil..." + ANSI_RESET);
                                result = interactingContract.evaluateTransaction("GetHistoryOfBasil", basilQR);
                                System.out.println(ANSI_GREEN + "\n*** HISTORY RESULT ***" + ANSI_RESET);
                                System.out.println(new App().prettyJson(result));
                                System.out.println(ANSI_GREEN + "**********************" + ANSI_RESET);
                                break;
                                
                            case "5": // DeleteBasil (Write)
                                System.out.print(ANSI_PURPLE + "\nInput Basil QR Code to delete: " + ANSI_RESET);
                                basilQR = scanner.nextLine().trim();
                                
                                System.out.println(ANSI_CYAN + "Submitting DeleteBasil transaction..." + ANSI_RESET);
                                result = interactingContract.submitTransaction("DeleteBasil", basilQR);
                                resultString = new String(result).trim();
                                    
                                    // --- UPDATED FAILURE CHECK ---
                                    if (resultString.contains("Not authorized") || resultString.contains("exists")) {
                                        System.out.println(ANSI_RED + "\n❌ TRANSACTION FAILED (Chaincode Logic Error)" + ANSI_RESET);
                                        System.out.println(ANSI_RED + "Chaincode Response: " + resultString + ANSI_RESET);
                                    } else {
                                        System.out.println(ANSI_GREEN + "\n✅ TRANSACTION SUCCESSFUL (TxID: " + resultString + ")" + ANSI_RESET);
                                    }
                                break;
                                
                            default:
                                System.out.println(ANSI_RED + "\n❌ ERROR: Invalid transaction index. Please enter a value between 0 and 5." + ANSI_RESET);
                                break;
                        }
                    } catch (GatewayException e) {
                        System.out.println(ANSI_RED + "\n❌ FABRIC ERROR: " + e.getMessage() + ANSI_RESET);
                    } catch (Exception e) {
                        System.out.println(ANSI_RED + "\n❌ APPLICATION ERROR: " + e.getMessage() + ANSI_RESET);
                    }

                    System.out.println(ANSI_YELLOW + "\n==========================================================" + ANSI_RESET);
                    System.out.print(ANSI_YELLOW + "Press ENTER to continue to the main menu..." + ANSI_RESET);
                    scanner.nextLine(); // Pause before next iteration
                }

            } finally {
                if (channel != null) {
                    channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
                }
            }

        } finally {
            if (channel != null) {
                channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            }
        }
    }

    private String prettyJson(final byte[] json) {
        return prettyJson(new String(json, StandardCharsets.UTF_8));
    }

    private String prettyJson(final String json) {
        // Handle cases where the chaincode returns a simple string, not JSON
        if (json.startsWith("{") || json.startsWith("[")) {
            try {
                var parsedJson = JsonParser.parseString(json);
                return gson.toJson(parsedJson);
            } catch (Exception e) {
                // If parsing fails, just return the original string
                return json;
            }
        }
        return json;
    }
}