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
package org.onosproject.vtnrsc.router;

import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.vtnrsc.Router;
import org.onosproject.vtnrsc.RouterId;

/**
 * Service for interacting with the inventory of Routers.
 */
public interface RouterService {
    /**
     * Returns the Router with the specified identifier.
     *
     * @param RouterId router identifier
     * @return true or false
     */
    boolean exists(RouterId routerId);

    /**
     * Returns a collection of the currently known Routers.
     *
     * @return iterable collection of Routers
     */
    Iterable<Router> getRouters();

    /**
     * Returns the Router with the specified identifier.
     *
     * @param routerId Router identifier
     * @return Router or null if one with the given identifier is not known
     */
    Router getRouter(RouterId routerId);

    /**
     * Creates new Routers.
     *
     * @param routers the iterable collection of Routers
     * @return true if the identifier Router has been created right
     */
    boolean createRouters(Iterable<Router> routers);

    /**
     * Updates existing Routers.
     *
     * @param routers the iterable collection of Routers
     * @return true if all Routers were updated successfully
     */
    boolean updateRouters(Iterable<Router> routers);

    /**
     * Administratively removes the specified Routers from the store.
     *
     * @param routerIds the iterable collection of Routers identifier
     * @return true if remove identifier Routers successfully
     */
    boolean removeRouters(Iterable<RouterId> routerIds);
    /**
     * Add the specified listener to Router map store.
     *
     * @param listener Router listener
     */
    void addListener(EventuallyConsistentMapListener<RouterId, Router> listener);
    /**
     * Remove the specified listener to Router map store.
     *
     * @param listener Router listener
     */
    void removeListener(EventuallyConsistentMapListener<RouterId, Router> listener);
}
