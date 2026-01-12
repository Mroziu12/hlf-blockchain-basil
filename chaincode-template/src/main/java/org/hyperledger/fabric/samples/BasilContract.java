/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.time.Instant;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.hyperledger.fabric.shim.ledger.KeyModification;

import com.owlike.genson.Genson;

@Contract(
        name = "basic",
        info = @Info(
                title = "My Smart contract",
                description = "The hyperlegendary asset transfer",
                version = "1.0",
                license = @License(
                        name = "Apache 2.0 License",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"),
                contact = @Contact(
                        email = "a.transfer@example.com",
                        name = "Adrian Transfer",
                        url = "https://hyperledger.example.com")))
@Default
public final class BasilContract implements ContractInterface {

    private final Genson genson = new Genson();




    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String QueryBasil(final Context ctx, final String qr) {
        ChaincodeStub stub = ctx.getStub();
        return stub.getStringState(qr);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String CreateBasil(final Context ctx, final String qr, final String extraInfo) {

        ChaincodeStub stub = ctx.getStub();

        String ownerId = ctx.getClientIdentity().getMSPID();

        if("Org1MSP".equals(ownerId)){
            //its okay because org1 is the supplier and can create basils
            
            String basilJSON = QueryBasil(ctx, qr);
            if (basilJSON != null && !basilJSON.isEmpty()) {
                // if basil exists
                return "Basil already exists";
            }


            Owner owner = new Owner(ownerId);

            Basil basil = new Basil(qr, extraInfo, owner);
            basilJSON = genson.serialize(basil);
            stub.putStringState(qr, basilJSON);
            return basilJSON;

        }else{
            return "Not authorized. Only supplier can create basils";
        }
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String DeleteBasil(final Context ctx, final String qr) {

        ChaincodeStub stub = ctx.getStub();

        String basilJSON = QueryBasil(ctx, qr);
        if (basilJSON == null || basilJSON.isEmpty()) {
            // if basil does not exist
            return "Basil does not exist";
        }
        // set the owner to the calling MSP
        String callingMSP = ctx.getClientIdentity().getMSPID();

        Basil basil = genson.deserialize(basilJSON, Basil.class);

        if (!callingMSP.contentEquals(basil.getOwner().getOwnerID())) {
            // only the owner can delete the basil
            return "Not authorized. Only the owner can delete the basil";
        }

        stub.delState(qr);
        return "Basil " + qr + " deleted";
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String UpdateBasil(final Context ctx, final String qr, final String extraInfo, final String gpsPosition) {

        ChaincodeStub stub = ctx.getStub();

                // set the owner to the calling MSP
        String callingMSP = ctx.getClientIdentity().getMSPID();

        if("Org1MSP".equals(callingMSP)){
            //its okay because org1 is the supplier and can update basils
            String basilJSON = QueryBasil(ctx, qr);
            if (basilJSON == null || basilJSON.isEmpty()) {
                // if basil does not exist
                return "Basil does not exist";
            }


            Basil basil = genson.deserialize(basilJSON, Basil.class);

            if (!callingMSP.contentEquals(basil.getOwner().getOwnerID())) {
                // only the owner can update the basil
                return "Not authorized. Only the owner can update the basil";
            }

            Instant timestamp = stub.getTxTimestamp();
            Long timestampSeconds = timestamp.getEpochSecond();

            Basil newBasil = new Basil(qr, extraInfo, basil.getOwner(), new BasilLeg(timestampSeconds, gpsPosition));
            basilJSON = genson.serialize(newBasil);
            stub.putStringState(qr, basilJSON);
            return basilJSON;
        }else{
            return "Not authorized. Only supplier can update basils";
        }


    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String TransferBasil(final Context ctx, final String qr, final String newOwner) {

        ChaincodeStub stub = ctx.getStub();

        // set the owner to the calling MSP
        String callingMSP = ctx.getClientIdentity().getMSPID();

        if("Org1MSP".equals(callingMSP)){
            //its okay because org1 is the supplier and can transfer basils";
            String basilJSON = QueryBasil(ctx, qr);
            if (basilJSON == null || basilJSON.isEmpty()) {
                // if basil does not exist
                return "Basil does not exist";
            }


            Basil basil = genson.deserialize(basilJSON, Basil.class);

            if (!callingMSP.contentEquals(basil.getOwner().getOwnerID())) {
                // only the owner can transfer the basil
                return "Not authorized. Only the owner can transfer the basil";
            }

            Basil newBasil = new Basil(qr, basil.getExtraInfo(), new Owner(newOwner), basil.getBasilLeg());
            basilJSON = genson.serialize(newBasil);
            stub.putStringState(qr, basilJSON);
            return basilJSON;
        }else{
            return "Not authorized. Only supplier can transfer basils";
        }
    }

    // /**
    //  * Retrieves all assets from the ledger.
    //  *
    //  * @param ctx the transaction context
    //  * @return array of assets found on the ledger
    //  */
    // @Transaction(intent = Transaction.TYPE.EVALUATE)
    // public String GetAllBills(final Context ctx) {
    //     ChaincodeStub stub = ctx.getStub();

    //     List<BillOfLading> queryResults = new ArrayList<BillOfLading>();

    //     // To retrieve all assets from the ledger use getStateByRange with empty startKey & endKey.
    //     // Giving empty startKey & endKey is interpreted as all the keys from beginning to end.
    //     // As another example, if you use startKey = 'asset0', endKey = 'asset9' ,
    //     // then getStateByRange will retrieve asset with keys between asset0 (inclusive) and asset9 (exclusive) in lexical order.
    //     QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "");

    //     for (KeyValue result: results) {
    //         BillOfLading bill = genson.deserialize(result.getStringValue(), BillOfLading.class);
    //         queryResults.add(bill);
    //         System.out.println(bill.toString());
    //     }

    //     final String response = genson.serialize(queryResults);

    //     return response;
    // }


    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetHistoryOfBasil(final Context ctx, String basilID) {
        ChaincodeStub stub = ctx.getStub();
        
        // Używamy ogólnego iteratora - to musi działać w 2.5.0
        QueryResultsIterator<KeyModification> results = null; 
        List<Map<String, Object>> historyRecords = new ArrayList<>();

        try {
            // Pobierz iterator z historią
            results = stub.getHistoryForKey(basilID);

            // Przetwarzaj każdy historyczny rekord
            for (KeyModification modification : results) {
                Map<String, Object> record = new HashMap<>();
                
                // 1. Dodaj metadane transakcji
                record.put("txId", modification.getTxId());
                record.put("timestamp", modification.getTimestamp().toEpochMilli()); 
                record.put("isDeleted", modification.isDeleted());

                // 2. Dodaj stan zasobu
                if (modification.isDeleted()) {
                    record.put("asset", "DELETED");
                } else {
                    String jsonValue = modification.getStringValue();
                    Basil historicalBasil = genson.deserialize(jsonValue, Basil.class);
                    record.put("asset", historicalBasil);
                }
                
                historyRecords.add(record);
            }

        } catch (Exception e) {
            // Jeśli cokolwiek pójdzie źle, rzuć prosty błąd
            throw new RuntimeException("Nie udało się pobrać historii dla " + basilID + ": " + e.getMessage());
            
        } finally {
            // 3. ZAWSZE zamknij iterator w finally, żeby uniknąć wycieków zasobów
            if (results != null) {
                try {
                    results.close();
                } catch (Exception e) {
                    // Po prostu zignoruj błąd zamykania, ale wypisz ostrzeżenie
                    System.err.println("Błąd zamykania iteratora: " + e.getMessage());
                }
            }
        }

        // 4. Zwróć zserializowaną listę
        return genson.serialize(historyRecords);
    }

    // @Transaction(intent = Transaction.TYPE.EVALUATE)
    // public String GetAllBillsOfOrganisation(final Context ctx, String organisationID) {
    //     ChaincodeStub stub = ctx.getStub();

    //     List<BillOfLading> queryResults = new ArrayList<BillOfLading>();

    //     QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "");

    //     for (KeyValue result: results) {
    //         BillOfLading bill = genson.deserialize(result.getStringValue(), BillOfLading.class);
    //         if (!bill.getOwner().contentEquals(organisationID)) {
    //             continue;
    //         }
    //         queryResults.add(bill);
    //         System.out.println(bill.toString());
    //     }

    //     final String response = genson.serialize(queryResults);

    //     return response;
    // }
}
