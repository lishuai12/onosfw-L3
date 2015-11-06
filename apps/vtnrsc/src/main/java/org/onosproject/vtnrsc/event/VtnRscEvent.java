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

import org.onosproject.event.AbstractEvent;


/**
 * Describes network vtnrsc event.
 */
public class VtnRscEvent
        extends AbstractEvent<VtnRscEvent.Type, VtnRscEventFeedback> {

    /**
     * Type of vtnrsc events.
     */
    public enum Type {
        /**
         * Signifies that floating IP has create.
         */
        FLOATINGIP_PUT,
        /**
         * Signifies that floating IP has.
         */
        FLOATINGIP_DELETE,
        /**
         * Signifies that router has create.
         */
        ROUTER_PUT,
        /**
         * Signifies that router has delete.
         */
        ROUTER_DELETE,
        /**
         * Signifies that router interface has add.
         */
        ROUTER_INTERFACE_PUT,
        /**
         * Signifies that router interface has remove.
         */
        ROUTER_INTERFACE_DELETE
    }

    /**
     * Creates an event of a given type and for the specified vtn event feedbak.
     *
     *
     * @param type Vtnrsc event type
     * @param vtnFeedback event VtnrscEventFeedback subject
     * @param reasons list of events that triggered Vtnrsc event
     */
    public VtnRscEvent(Type type, VtnRscEventFeedback vtnFeedback) {
        super(type, vtnFeedback);
    }

    /**
     * Creates an event of a given type and for the specified vtn event feedbak.
     *
     *
     * @param type link event type
     * @param vtnFeedback event VtnrscEventFeedback subject
     * @param reasons list of events that triggered Vtnrsc event
     * @param time occurrence time
     */
    public VtnRscEvent(Type type, VtnRscEventFeedback vtnFeedback, long time) {
        super(type, vtnFeedback, time);
    }
}
