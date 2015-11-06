/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.vtnrsc;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
/**
 * Immutable representation of a floatingIp identifier.
 */
public final class FloatingIpId {
    private final String floatingIpId;

    // Public construction is prohibited
    private FloatingIpId(String floatingIpId) {
        checkNotNull(floatingIpId, "floatingIpId cannot be null");
        this.floatingIpId = floatingIpId;
    }

    /**
     * Creates a floatingIp identifier.
     *
     * @param floatingIpId the floatingIp identifier
     * @return the floatingIp identifier
     */
    public static FloatingIpId floatingIpId(String floatingIpId) {
        return new FloatingIpId(floatingIpId);
    }

    /**
     * Returns the floatingIp identifier.
     *
     * @return the floatingIp identifier
     */
    public String floatingIpId() {
        return floatingIpId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(floatingIpId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FloatingIpId) {
            final FloatingIpId that = (FloatingIpId) obj;
            return this.getClass() == that.getClass()
                    && Objects.equals(this.floatingIpId, that.floatingIpId);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("floatingIpId", floatingIpId).toString();
    }
}
