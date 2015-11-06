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

import java.util.List;

/**
 * Representation of a Router.
 */
public interface Router {

    /**
     * Coarse classification of the type of the Router.
     */
    public enum RouterStatus {
        /**
         * Signifies that a router is currently active.
         */
        ACTIVE,
        /**
         * Signifies that a router is currently inactive.
         */
        INACTIVE
    }

    /**
     * Returns the router identifier.
     *
     * @return identifier
     */
    RouterId routerId();

    /**
     * Returns the router Name.
     *
     * @return routerName
     */
    String routerName();

    /**
     * Returns the router admin state.
     *
     * @return true or false
     */
    boolean adminStateUp();

    /**
     * Returns the status of router.
     *
     * @return RouterStatus
     */
    RouterStatus status();

    /**
     * Returns the status of router distributed.
     *
     * @return true or false
     */
    boolean distributed();

    /**
     * Returns the RouterGateway of router.
     *
     * @return routerGateway
     */
    RouterGateway externalGatewayInfo();

    /**
     * Returns the gatewayPortid of router.
     *
     * @return virtualPortId
     */
    VirtualPortId gatewayPortid();

    /**
     * Returns the tenantId of router.
     *
     * @return tenantId
     */
    TenantId tenantId();

    /**
     * Returns the router list of router.
     *
     * @return routes
     */
    List<String> routes();
}
