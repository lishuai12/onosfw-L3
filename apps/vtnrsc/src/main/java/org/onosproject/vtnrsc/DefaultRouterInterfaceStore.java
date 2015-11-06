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

import java.util.HashMap;
import java.util.Objects;

/**
 * Representation of a Router interface.
 */
public class DefaultRouterInterfaceStore implements RouterInterfaceStore {
    private final HashMap<SubnetId, RouterInterface> interfaces;

    /**
     * create router interface object.
     *
     * @param routerId router identifier
     * @param interfaces interfaces of router
     */
    public DefaultRouterInterfaceStore(HashMap<SubnetId, RouterInterface> interfaces) {
        this.interfaces = interfaces;
    }

    @Override
    public HashMap<SubnetId, RouterInterface> interfaces() {
        return interfaces;
    }

    @Override
    public int hashCode() {
        return Objects.hash(interfaces);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultRouterInterfaceStore) {
            final DefaultRouterInterfaceStore that = (DefaultRouterInterfaceStore) obj;
            return Objects.equals(this.interfaces, that.interfaces);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("interfaces", interfaces).toString();
    }

    @Override
    public void addInterface(SubnetId subnetId, RouterInterface routerInterface) {
        this.interfaces.put(subnetId, routerInterface);
    }

    @Override
    public void removeInterface(SubnetId subnetId) {
        this.interfaces.remove(subnetId);
    }
}
