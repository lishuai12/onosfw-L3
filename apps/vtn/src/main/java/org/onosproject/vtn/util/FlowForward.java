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
package org.onosproject.vtn.util;

import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.flowobjective.ForwardingObjective;

/**
 * Flow forward utility class.
 */
public final class FlowForward {

    /**
     * Constructs a flow forward object. Utility classes should not have a
     * public or default constructor, otherwise IDE will compile unsuccessfully.
     * This class should not be instantiated.
     */
    private FlowForward() {
    }

    /**
     * Applies flow rule to the device.
     *
     * @param pipeLiner the Pipeliner of a device
     * @param forwardingObjective a forwarding objective
     */
    public static void flowServiceForward(Pipeliner pipeLiner,
                                          ForwardingObjective forwardingObjective) {
        if (pipeLiner != null) {
            pipeLiner.forward(forwardingObjective);
        }
    }
}
