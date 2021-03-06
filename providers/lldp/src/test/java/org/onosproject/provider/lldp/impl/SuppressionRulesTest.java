package org.onosproject.provider.lldp.impl;

import static org.junit.Assert.*;
import static org.onosproject.net.DeviceId.deviceId;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class SuppressionRulesTest {

    private static final DeviceId NON_SUPPRESSED_DID = deviceId("of:1111000000000000");
    private static final DeviceId SUPPRESSED_DID = deviceId("of:2222000000000000");
    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final String MFR = "whitebox";
    private static final String HW = "1.1.x";
    private static final String SW1 = "3.8.1";
    private static final String SN = "43311-12345";
    private static final ChassisId CID = new ChassisId();

    private static final PortNumber P1 = PortNumber.portNumber(1);

    private SuppressionRules rules;

    @Before
    public void setUp() throws Exception {
        rules = new SuppressionRules(ImmutableSet.of(SUPPRESSED_DID),
                               ImmutableSet.of(Device.Type.ROADM),
                               ImmutableMap.of("no-lldp", SuppressionRules.ANY_VALUE,
                                               "sendLLDP", "false"));
    }

    @Test
    public void testSuppressedDeviceId() {
        Device device = new DefaultDevice(PID,
                                          SUPPRESSED_DID,
                                          Device.Type.SWITCH,
                                          MFR, HW, SW1, SN, CID);
        assertTrue(rules.isSuppressed(device));
    }

    @Test
    public void testSuppressedDeviceType() {
        Device device = new DefaultDevice(PID,
                                          NON_SUPPRESSED_DID,
                                          Device.Type.ROADM,
                                          MFR, HW, SW1, SN, CID);
        assertTrue(rules.isSuppressed(device));
    }

    @Test
    public void testSuppressedDeviceAnnotation() {
        Annotations annotation = DefaultAnnotations.builder()
                .set("no-lldp", "random")
                .build();

        Device device = new DefaultDevice(PID,
                                          NON_SUPPRESSED_DID,
                                          Device.Type.SWITCH,
                                          MFR, HW, SW1, SN, CID, annotation);
        assertTrue(rules.isSuppressed(device));
    }

    @Test
    public void testSuppressedDeviceAnnotationExact() {
        Annotations annotation = DefaultAnnotations.builder()
                .set("sendLLDP", "false")
                .build();

        Device device = new DefaultDevice(PID,
                                          NON_SUPPRESSED_DID,
                                          Device.Type.SWITCH,
                                          MFR, HW, SW1, SN, CID, annotation);
        assertTrue(rules.isSuppressed(device));
    }

    @Test
    public void testNotSuppressedDevice() {
        Device device = new DefaultDevice(PID,
                                          NON_SUPPRESSED_DID,
                                          Device.Type.SWITCH,
                                          MFR, HW, SW1, SN, CID);
        assertFalse(rules.isSuppressed(device));
    }

    @Test
    public void testSuppressedPortOnSuppressedDevice() {
        Device device = new DefaultDevice(PID,
                                          SUPPRESSED_DID,
                                          Device.Type.SWITCH,
                                          MFR, HW, SW1, SN, CID);
        Port port = new DefaultPort(device, P1, true);

        assertTrue(rules.isSuppressed(port));
    }

    @Test
    public void testSuppressedPortAnnotation() {
        Annotations annotation = DefaultAnnotations.builder()
                .set("no-lldp", "random")
                .build();
        Device device = new DefaultDevice(PID,
                                          NON_SUPPRESSED_DID,
                                          Device.Type.SWITCH,
                                          MFR, HW, SW1, SN, CID);
        Port port = new DefaultPort(device, P1, true, annotation);

        assertTrue(rules.isSuppressed(port));
    }

    @Test
    public void testSuppressedPortAnnotationExact() {
        Annotations annotation = DefaultAnnotations.builder()
                .set("sendLLDP", "false")
                .build();
        Device device = new DefaultDevice(PID,
                                          NON_SUPPRESSED_DID,
                                          Device.Type.SWITCH,
                                          MFR, HW, SW1, SN, CID);
        Port port = new DefaultPort(device, P1, true, annotation);

        assertTrue(rules.isSuppressed(port));
    }

    @Test
    public void testNotSuppressedPort() {
        Device device = new DefaultDevice(PID,
                                          NON_SUPPRESSED_DID,
                                          Device.Type.SWITCH,
                                          MFR, HW, SW1, SN, CID);
        Port port = new DefaultPort(device, P1, true);

        assertFalse(rules.isSuppressed(port));
    }
}
