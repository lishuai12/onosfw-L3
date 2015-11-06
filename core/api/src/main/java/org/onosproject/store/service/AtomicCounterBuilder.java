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
package org.onosproject.store.service;

/**
 * Builder for AtomicCounter.
 */
public interface AtomicCounterBuilder {

    /**
     * Sets the name for the atomic counter.
     * <p>
     * Each atomic counter is identified by a unique name.
     * </p>
     * <p>
     * Note: This is a mandatory parameter.
     * </p>
     *
     * @param name name of the atomic counter
     * @return this AtomicCounterBuilder
     */
    AtomicCounterBuilder withName(String name);

    /**
     * Creates this counter on the partition that spans the entire cluster.
     * <p>
     * When partitioning is disabled, the counter state will be
     * ephemeral and does not survive a full cluster restart.
     * </p>
     * <p>
     * Note: By default partitions are enabled.
     * </p>
     * @return this AtomicCounterBuilder
     */
    AtomicCounterBuilder withPartitionsDisabled();

    /**
     * Instantiates Metering service to gather usage and performance metrics.
     * By default, usage data will be stored.
     *
     * @return this AtomicCounterBuilder
     */
    AtomicCounterBuilder withMeteringDisabled();

    /**
     * Builds a AtomicCounter based on the configuration options
     * supplied to this builder.
     *
     * @return new AtomicCounter
     * @throws java.lang.RuntimeException if a mandatory parameter is missing
     */
    AtomicCounter build();

    /**
     * Builds a AsyncAtomicCounter based on the configuration options
     * supplied to this builder.
     *
     * @return new AsyncAtomicCounter
     * @throws java.lang.RuntimeException if a mandatory parameter is missing
     */
    AsyncAtomicCounter buildAsyncCounter();
}
