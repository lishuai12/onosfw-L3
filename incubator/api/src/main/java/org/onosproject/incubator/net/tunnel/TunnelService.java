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
package org.onosproject.incubator.net.tunnel;

import java.util.Collection;

import com.google.common.annotations.Beta;
import org.onosproject.core.ApplicationId;
import org.onosproject.event.ListenerService;
import org.onosproject.incubator.net.tunnel.Tunnel.Type;
import org.onosproject.net.Annotations;
import org.onosproject.net.DeviceId;

/**
 * Service for interacting with the inventory of tunnels.
 */
@Beta
public interface TunnelService
    extends ListenerService<TunnelEvent, TunnelListener> {

    /**
     * Borrows a specific tunnel. Annotations parameter is reserved.If there
     * is no tunnel in the store, returns a "null" object, and record the tunnel subscription.
     * Where tunnel is created, ONOS notifies this consumer actively.
     *
     * @param consumerId a tunnel consumer
     * @param tunnelId tunnel identify generated by onos
     * @param annotations Annotations
     * @return Tunnel subscribed tunnel
     */
    Tunnel borrowTunnel(ApplicationId consumerId, TunnelId tunnelId,
                           Annotations... annotations);

    /**
     * Borrows a specific tunnel by tunnelName. Annotations parameter is reserved.If there
     * is no tunnel in the store, return a "null" object, and record the tunnel subscription.
     * Where tunnel is created, ONOS notifies this consumer actively.
     *
     * @param consumerId a tunnel consumer
     * @param tunnelName tunnel name
     * @param annotations Annotations
     * @return collection of subscribed Tunnels
     */
    Collection<Tunnel> borrowTunnel(ApplicationId consumerId, TunnelName tunnelName,
                           Annotations... annotations);

    /**
     * Borrows all tunnels between source and destination. Annotations
     * parameter is reserved.If there is no any tunnel in the store, return a
     * empty collection,and record the tunnel subscription. Where tunnel is created, ONOS
     * notifies this consumer actively. Otherwise ONOS core returns all the
     * tunnels, consumer determined which one to use.
     *
     * @param consumerId a tunnel consumer
     * @param src a source point of tunnel.
     * @param dst a destination point of tunnel
     * @param annotations Annotations
     * @return collection of subscribed Tunnels
     */
    Collection<Tunnel> borrowTunnel(ApplicationId consumerId, TunnelEndPoint src,
                                       TunnelEndPoint dst, Annotations... annotations);

    /**
     * Borrows all specified type tunnels between source and destination.
     * Annotations parameter is reserved.If there is no any tunnel in the store,
     * return a empty collection, and record the tunnel subscription. Where tunnel is
     * created, ONOS notifies this consumer actively. Otherwise,ONOS core returns
     * all available tunnels, consumer determined which one to use.
     *
     * @param consumerId a tunnel consumer
     * @param src a source point of tunnel.
     * @param dst a destination point of tunnel
     * @param type tunnel type
     * @param annotations Annotations
     * @return collection of available Tunnels
     */
    Collection<Tunnel> borrowTunnel(ApplicationId consumerId, TunnelEndPoint src,
                                       TunnelEndPoint dst, Type type,
                                       Annotations... annotations);

    /**
     * Returns back a specific tunnel to store.
     *
     * @param consumerId a tunnel consumer
     * @param tunnelId tunnel identify generated by ONOS
     * @param annotations Annotations
     * @return success or fail
     */
    boolean returnTunnel(ApplicationId consumerId, TunnelId tunnelId,
                              Annotations... annotations);

    /**
     * Returns all specific name tunnel back store. Annotations parameter is reserved.if there
     * is no tunnel in the store, return a "null" object, and record the tunnel subscription.
     * Where tunnel is created, ONOS notifies this consumer actively.
     *
     * @param consumerId a tunnel consumer
     * @param tunnelName tunnel name
     * @param annotations Annotations
     * @return boolean
     */
    boolean returnTunnel(ApplicationId consumerId, TunnelName tunnelName,
                           Annotations... annotations);

    /**
     * Returns all specific type tunnels between source and destination back
     * store. Annotations parameter is reserved.
     *
     * @param consumerId a tunnel consumer
     * @param src a source point of tunnel.
     * @param dst a destination point of tunnel
     * @param type tunnel type
     * @param annotations Annotations
     * @return success or fail
     */
    boolean returnTunnel(ApplicationId consumerId, TunnelEndPoint src,
                              TunnelEndPoint dst, Type type,
                              Annotations... annotations);

    /**
     * Returns all tunnels between source and destination back the store.
     * Annotations parameter is reserved.
     *
     * @param consumerId a tunnel consumer
     * @param src a source point of tunnel.
     * @param dst a destination point of tunnel.
     * @param annotations Annotations
     * @return success or fail
     */
    boolean returnTunnel(ApplicationId consumerId, TunnelEndPoint src,
                              TunnelEndPoint dst, Annotations... annotations);

    /**
     * Returns a tunnel by a specific tunnel identity.
     *
     * @param tunnelId tunnel identify generated by tunnel producer
     * @return Tunnel
     */
    Tunnel queryTunnel(TunnelId tunnelId);

    /**
     * Returns all tunnel subscription record by consumer.
     *
     * @param consumerId consumer identity
     * @return Collection of TunnelSubscription
     */
    Collection<TunnelSubscription> queryTunnelSubscription(ApplicationId consumerId);

    /**
     * Returns all specified type tunnels.
     *
     * @param type tunnel type
     * @return Collection of tunnels
     */
    Collection<Tunnel> queryTunnel(Type type);

    /**
     * Returns all tunnels between source point and destination point.
     *
     * @param src a source point of tunnel.
     * @param dst a destination point of tunnel.
     * @return Collection of tunnels
     */
    Collection<Tunnel> queryTunnel(TunnelEndPoint src, TunnelEndPoint dst);

    /**
     * Returns all tunnels.
     *
     * @return Collection of tunnels
     */
    Collection<Tunnel> queryAllTunnels();

    /**
     * Returns all tunnels.
     *
     * @return all tunnels
     */
    int tunnelCount();

    /**
     * Returns the collection of tunnels applied on the specified device.
     *
     * @param deviceId device identifier
     * @return collection of tunnels
     */
    Iterable<Tunnel> getTunnels(DeviceId deviceId);

}
