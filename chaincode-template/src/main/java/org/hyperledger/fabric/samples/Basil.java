/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples;

import java.util.Objects;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

@DataType()
public final class Basil {

    @Property()
    private String qr;

    @Property()
    private String extraInfo;

    @Property()
    private Owner owner;

    @Property()
    private BasilLeg basilLeg;

    public String getQr() {
        return qr;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public Owner getOwner() {
        return owner;
    }
    
    public BasilLeg getBasilLeg() {
        return basilLeg;
    }

    public Basil(@JsonProperty("qr") final String qr,
                 @JsonProperty("extraInfo") final String extraInfo,
                 @JsonProperty("owner") final Owner owner)
                  {
        this.qr = qr;
        this.extraInfo = extraInfo;
        this.owner = owner;
    }

    public Basil(@JsonProperty("qr") final String qr,
             @JsonProperty("extraInfo") final String extraInfo,
             @JsonProperty("owner") final Owner owner,
             @JsonProperty("basilLeg") final BasilLeg basilLeg) {
    this.qr = qr;
    this.extraInfo = extraInfo;
    this.owner = owner;
    this.basilLeg = basilLeg;
}

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        Basil other = (Basil) obj;

        return Objects.equals(qr, other.qr)
                && Objects.equals(extraInfo, other.extraInfo)
                && Objects.equals(owner, other.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qr, extraInfo, owner);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
                + "@" + Integer.toHexString(hashCode())
                + " [qr=" + qr
                + ", extraInfo=" + extraInfo
                + ", owner=" + owner  
                + ", basilLeg=" + basilLeg + "]";
    }
}
