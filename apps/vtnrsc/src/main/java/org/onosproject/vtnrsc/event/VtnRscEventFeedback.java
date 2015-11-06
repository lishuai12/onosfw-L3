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
package org.onosproject.vtnrsc.event;

import org.onosproject.vtnrsc.FloatingIp;
import org.onosproject.vtnrsc.Router;
import org.onosproject.vtnrsc.RouterInterface;

public class VtnRscEventFeedback {
    private final FloatingIp floaingtIp;
    private final Router router;
    private final RouterInterface routerInterface;

    public VtnRscEventFeedback(FloatingIp floatingIp) {
        this.floaingtIp = floatingIp;
        this.router = null;
        this.routerInterface = null;
    }

    public VtnRscEventFeedback(Router router) {
        this.floaingtIp = null;
        this.router = router;
        this.routerInterface = null;
    }

    public VtnRscEventFeedback(RouterInterface routerInterface) {
        this.floaingtIp = null;
        this.router = null;
        this.routerInterface = routerInterface;
    }

    public FloatingIp floatingIp() {
        return floaingtIp;
    }

    public Router router() {
        return router;
    }

    public RouterInterface routerInterface() {
        return routerInterface;
    }
}
