/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples;

import java.util.Objects;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

@DataType()
public final class Owner {

    @Property()
    private String ownerID;

    @Property()
    private String ownerName;


    public String getOwnerID() {
        return ownerID;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public Owner(@JsonProperty("ownerID") final String ownerID) {
        this.ownerID = ownerID;
        if ("Org1MSP".equals(ownerID)) {
            this.ownerName = "Pittaluga & fratelli";
        } else if ("Org2MSP".equals(ownerID)) {
            this.ownerName = "Supermarket";
        } else {
            this.ownerName = "Unknown Organisation";
        }
    }
    
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        Owner other = (Owner) obj;

        return Objects.equals(ownerID, other.ownerID)
                && Objects.equals(ownerName, other.ownerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerID, ownerName);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
                + "@" + Integer.toHexString(hashCode())
                + " [ownerID=" + ownerID
                + ", ownerName=" + ownerName + "]";
    }
}
