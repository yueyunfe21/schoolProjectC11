package com.bot.dhxy.driver.fakerinput;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FakerInputDeviceTest {

    @Test
    void missingProbeDoesNotPublishInputReport() {
        FakeFactory factory = new FakeFactory(null);
        FakerInputDevice device = new FakerInputDevice(1, factory);

        FakerInputDeviceStatus status = device.probe();

        assertEquals(FakerInputDeviceState.DRIVER_MISSING, status.state());
        assertEquals(0, factory.reportCount());
    }

    @Test
    void readyProbePerformsVersionHandshakeButNoInputReport() {
        FakeConnection connection = new FakeConnection(1, 2, true);
        FakerInputDevice device = new FakerInputDevice(1, new FakeFactory(connection));

        FakerInputDeviceStatus status = device.probe();

        assertEquals(FakerInputDeviceState.DRIVER_READY, status.state());
        assertEquals(1, status.apiVersion());
        assertEquals(2, status.driverVersion());
        assertTrue(connection.apiVerified);
        assertTrue(connection.closed);
        assertEquals(0, connection.reports.size());
    }

    @Test
    void unsupportedDriverIsClosedAndCannotAcceptInput() {
        FakeConnection connection = new FakeConnection(0, 2, true);
        FakerInputDevice device = new FakerInputDevice(1, new FakeFactory(connection));

        FakerInputDeviceStatus status = device.connect();

        assertEquals(FakerInputDeviceState.DRIVER_VERSION_UNSUPPORTED, status.state());
        assertTrue(connection.closed);
        assertThrows(IllegalStateException.class, () -> device.updateKeyboard(0, (byte) 0x04));
    }

    @Test
    void endpointOpenFailureIsUnavailableRatherThanMissing() {
        FakerInputDevice device = new FakerInputDevice(1, () -> {
            throw new IllegalStateException("device present but access denied");
        });

        FakerInputDeviceStatus status = device.probe();

        assertEquals(FakerInputDeviceState.DRIVER_UNAVAILABLE, status.state());
        assertTrue(status.detail().contains("access denied"));
    }

    @Test
    void connectedDeviceEncodesKeyboardMouseAndReleaseAllReports() {
        FakeConnection connection = new FakeConnection(1, 2, true);
        FakerInputDevice device = new FakerInputDevice(1, new FakeFactory(connection));
        assertEquals(FakerInputDeviceState.DRIVER_READY, device.connect().state());

        device.updateKeyboard(0x04, (byte) 0x04, (byte) 0x1E);
        device.updateRelativeMouse(0x01, 300, -120, -1, 2);
        device.updateAbsoluteMouse(0x02, 16384, 8192, 1);
        device.releaseAll();

        assertEquals(5, connection.reports.size());
        assertReportPrefix(connection.reports.get(0),
                0x40, 9, 0x01, 0x04, 0, 0x04, 0x1E, 0, 0, 0, 0);
        assertReportPrefix(connection.reports.get(1),
                0x40, 8, 0x03, 0x01, 0x2C, 0x01, 0x88, 0xFF, 0xFF, 0x02);
        assertReportPrefix(connection.reports.get(2),
                0x40, 7, 0x04, 0x02, 0x00, 0x40, 0x00, 0x20, 0x01);
        assertReportPrefix(connection.reports.get(3),
                0x40, 9, 0x01, 0, 0, 0, 0, 0, 0, 0, 0);
        assertReportPrefix(connection.reports.get(4),
                0x40, 8, 0x03, 0, 0, 0, 0, 0, 0, 0);
    }

    @Test
    void keyboardAndMouseBoundsFailBeforePublishing() {
        FakeConnection connection = new FakeConnection(1, 2, true);
        FakerInputDevice device = new FakerInputDevice(1, new FakeFactory(connection));
        device.connect();

        assertThrows(IllegalArgumentException.class,
                () -> device.updateKeyboard(0, new byte[7]));
        assertThrows(IllegalArgumentException.class,
                () -> device.updateRelativeMouse(0, 40_000, 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> device.updateAbsoluteMouse(0, 32_768, 0, 0));
        assertEquals(0, connection.reports.size());
    }

    private static void assertReportPrefix(byte[] actual, int... expectedUnsignedBytes) {
        byte[] expected = new byte[expectedUnsignedBytes.length];
        for (int i = 0; i < expectedUnsignedBytes.length; i++) {
            expected[i] = (byte) expectedUnsignedBytes[i];
        }
        assertArrayEquals(expected, Arrays.copyOf(actual, expected.length));
        assertEquals(FakerInputDevice.CONTROL_REPORT_SIZE, actual.length);
    }

    private static final class FakeFactory implements FakerInputDevice.NativeConnectionFactory {
        private final FakeConnection connection;

        private FakeFactory(FakeConnection connection) {
            this.connection = connection;
        }

        @Override
        public FakerInputDevice.NativeConnection open() {
            return connection;
        }

        private int reportCount() {
            return connection == null ? 0 : connection.reports.size();
        }
    }

    private static final class FakeConnection implements FakerInputDevice.NativeConnection {
        private final int apiVersion;
        private final int driverVersion;
        private final boolean acceptsApi;
        private final List<byte[]> reports = new ArrayList<>();
        private boolean apiVerified;
        private boolean closed;

        private FakeConnection(int apiVersion, int driverVersion, boolean acceptsApi) {
            this.apiVersion = apiVersion;
            this.driverVersion = driverVersion;
            this.acceptsApi = acceptsApi;
        }

        @Override
        public int driverVersion() {
            return driverVersion;
        }

        @Override
        public boolean verifyClientApi(int apiVersion) {
            apiVerified = true;
            return acceptsApi;
        }

        @Override
        public int apiVersion() {
            return apiVersion;
        }

        @Override
        public void writeControlReport(byte[] report) {
            reports.add(report.clone());
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
