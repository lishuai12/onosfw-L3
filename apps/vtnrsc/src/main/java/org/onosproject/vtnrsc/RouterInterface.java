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

import java.util.Objects;

/**
 * Representation of a Router interface.
 */
public final class RouterInterface {
    private final SubnetId subnetId;
    private final VirtualPortId portId;
    private final RouterId routerId;
    private final TenantId tenantId;

    /**
     * create router interface object.
     *
     * @param subnetId subnet identifier
     * @param portId port identifier
     * @param routerId router identifier
     * @param tenantId tenant identifier
     */
    public RouterInterface(SubnetId subnetId, VirtualPortId portId,
                                  RouterId routerId, TenantId tenantId) {
        this.subnetId = subnetId;
        this.portId = portId;
        this.routerId = routerId;
        this.tenantId = tenantId;
    }

    public SubnetId subnetId() {
        return subnetId;
    }

    public VirtualPortId portId() {
        return portId;
    }

    public RouterId routerId() {
        return routerId;
    }

    public TenantId tenantId() {
        return tenantId;
    }
    @Override
    public int hashCode() {
        return Objects.hash(subnetId, portId, routerId, tenantId);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RouterInterface) {
            final RouterInterface that = (RouterInterface) obj;
            return Objects.equals(this.subnetId, that.subnetId)
                    && Objects.equals(this.portId, that.portId)
                    && Objects.equals(this.routerId, that.routerId)
                    && Objects.equals(this.tenantId, that.tenantId);
        }
        return false;
    }
    @Override
    public String toString() {
        return toStringHelper(this).add("subnetId", subnetId)
                .add("portId", portId).add("routerId", routerId)
                .add("tenantId", tenantId).toString();
    }
}
