/*
 * SPDX-License-Identifier: Apache-2.0
 */

package org.hyperledger.fabric.samples;

import java.util.Objects;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

@DataType()
public final class BasilLeg {

    @Property()
    private Long timestamp;

    @Property()
    private String gpsPosition;


    public Long getTimestamp() {
        return timestamp;
    }

    public String getGpsPosition() {
        return gpsPosition;
    }

    public BasilLeg(@JsonProperty("timestamp") final Long timestamp,
                    @JsonProperty("gpsPosition") final String gpsPosition) {
        this.timestamp = timestamp;
        this.gpsPosition = gpsPosition;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }

        BasilLeg other = (BasilLeg) obj;

        return Objects.equals(timestamp, other.timestamp)
                && Objects.equals(gpsPosition, other.gpsPosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, gpsPosition);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
                + "@" + Integer.toHexString(hashCode())
                + " [timestamp=" + timestamp
                + ", gpsPosition=" + gpsPosition + "]";
    }
}
