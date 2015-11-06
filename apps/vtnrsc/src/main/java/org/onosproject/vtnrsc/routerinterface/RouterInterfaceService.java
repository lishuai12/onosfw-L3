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
package org.onosproject.vtnrsc.routerinterface;

import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.RouterInterface;
import org.onosproject.vtnrsc.RouterInterfaceStore;
import org.onosproject.vtnrsc.SubnetId;

/**
 * Service for interacting with the inventory of Router interface.
 */
public interface RouterInterfaceService {
    /**
     * Returns the Router interface with the specified router identifier.
     *
     * @param routerId router identifier
     * @return true or false
     */
    boolean exists(RouterId routerId);

    /**
     * Returns a collection of the currently known Router interface.
     *
     * @return iterable collection of RouterInterface
     */
    Iterable<RouterInterface> getRouterInterfaces();

    /**
     * Returns the Router with the specified identifier.
     *
     * @param routerId router identifier
     * @return RouterInterface or null if one with the given identifier is not
     *         known
     */
    RouterInterfaceStore getRouterInterfaces(RouterId routerId);

    /**
     * Administratively add the specified interface to specified Router
     * identifier.
     *
     * @param routerInterface the interface add to router
     * @return true if add router interface successfully
     */
    boolean addRouterInterface(RouterInterface routerInterface);

    /**
     * Administratively remove the specified interface to specified Router
     * identifier.
     *
     * @param routerInterface the interface remove from router
     * @return true if remove router interface successfully
     */
    boolean removeRouterInterface(RouterInterface routerInterface);
    /**
     * Add the specified listener to Router Interface map store.
     *
     * @param listener Router Interface listener
     */
    void addListener(EventuallyConsistentMapListener<SubnetId, RouterInterface> listener);
    /**
     * Remove the specified listener to RouterInterface map store.
     *
     * @param listener Router Interface listener
     */
    void removeListener(EventuallyConsistentMapListener<SubnetId, RouterInterface> listener);
}
