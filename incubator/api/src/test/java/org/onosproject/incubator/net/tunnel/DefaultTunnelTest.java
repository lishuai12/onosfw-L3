package org.onosproject.incubator.net.tunnel;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.net.provider.ProviderId;

import com.google.common.testing.EqualsTester;

/**
 * Test of the default tunnel model entity.
 */
public class DefaultTunnelTest {
    /**
     * Checks that the Order class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultTunnel.class);
    }

    @Test
    public void testEquality() {
        TunnelEndPoint src = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                .valueOf(23423));
        TunnelEndPoint dst = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                .valueOf(32421));
        DefaultGroupId groupId = new DefaultGroupId(92034);
        TunnelName tunnelName = TunnelName.tunnelName("TunnelName");
        TunnelId tunnelId = TunnelId.valueOf(41654654);
        ProviderId producerName1 = new ProviderId("producer1", "13");
        ProviderId producerName2 = new ProviderId("producer2", "13");
        Tunnel p1 = new DefaultTunnel(producerName1, src, dst, Tunnel.Type.VXLAN,
                                      Tunnel.State.ACTIVE, groupId, tunnelId,
                                      tunnelName, null);
        Tunnel p2 = new DefaultTunnel(producerName1, src, dst, Tunnel.Type.VXLAN,
                                      Tunnel.State.ACTIVE, groupId, tunnelId,
                                      tunnelName, null);
        Tunnel p3 = new DefaultTunnel(producerName2, src, dst, Tunnel.Type.OCH,
                                      Tunnel.State.ACTIVE, groupId, tunnelId,
                                      tunnelName, null);
        new EqualsTester().addEqualityGroup(p1, p2).addEqualityGroup(p3)
                .testEquals();
    }
}
