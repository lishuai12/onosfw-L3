package org.onosproject.net;

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

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * The class representing a driver name. This class is immutable.
 */
public final class DriverName {

    private final String name;

    /**
     * Constructor from a String driver name.
     *
     * @param value the port name to use
     */
    public DriverName(String name) {
        checkNotNull(name, "name is not null");
        this.name = name;
    }

    /**
     * Gets the value of the driver name.
     *
     * @return the value of the driver name
     */
    public String name() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DriverName) {
            final DriverName driverName = (DriverName) obj;
            return Objects.equals(this.name, driverName.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("name", name).toString();
    }
}
