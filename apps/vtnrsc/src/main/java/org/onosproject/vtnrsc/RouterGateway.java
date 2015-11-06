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
 * Representation of a Router gateway.
 */
public final class RouterGateway {

    private final TenantNetworkId networkId;
    private final boolean enableSnat;
    private final Iterable<FixedIp> externalFixedIps;
    /**
     * create router gateway object.
     *
     * @param networkId network identifier
     * @param enableSnat snat enable or not
     * @param externalFixedIps external fixed Ip
     */
    public RouterGateway(TenantNetworkId networkId, boolean enableSnat,
                         Iterable<FixedIp> externalFixedIps) {
        this.networkId = networkId;
        this.enableSnat = enableSnat;
        this.externalFixedIps = externalFixedIps;
    }

    /**
     *return network identifier.
     *
     * @return networkId
     */
    public TenantNetworkId networkId() {
        return networkId;
    }

    /**
     * return snat enable or not.
     *
     * @return enableSnat
     */
    public boolean enableSnat() {
        return enableSnat;
    }

    /**
     * return external fixed Ip.
     *
     * @return externalFixedIps
     */
    public Iterable<FixedIp> externalFixedIps() {
        return externalFixedIps;
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkId, enableSnat, externalFixedIps);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof RouterGateway) {
            final RouterGateway that = (RouterGateway) obj;
            return Objects.equals(this.networkId, that.networkId)
                    && Objects.equals(this.enableSnat, that.enableSnat)
                    && Objects.equals(this.externalFixedIps, that.externalFixedIps);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("networkId", networkId).add("enableSnat", enableSnat)
                .add("externalFixedIps", externalFixedIps).toString();
    }
}
