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

import java.util.HashMap;

/**
 * Representation of a RouterInterfaceStore.
 */
public interface RouterInterfaceStore {
    /**
     * return the hash map of router interface.
     *
     * @return interfaces the hash map of router interface
     */
    HashMap<SubnetId, RouterInterface> interfaces();
    /**
     * add router interface to current router interface store.
     *
     * @param subnetId subnet identify
     * @param routerInterface the router interface need to be add.
     */
    public void addInterface(SubnetId subnetId, RouterInterface routerInterface);
    /**
     * remove interface from current router interface store.
     *
     * @param subnetId subnet identify
     */
    public void removeInterface(SubnetId subnetId);
}
