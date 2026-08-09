package com.bot.dhxy.driver.fakerinput;

import com.bot.dhxy.config.InputBackendProperties;
import com.sun.jna.Library;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.SetupApi;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.W32APIOptions;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Owns the connection and report protocol for the FakerInput virtual HID device.
 *
 * <p>The public probe performs only the driver's API handshake and feature-version read. It never sends a
 * keyboard or mouse input report. Real reports are possible only after an explicit {@link #connect()} call,
 * which is not invoked during normal application startup while {@code bot.input.backend=WIN_API}.</p>
 */
@Slf4j
@Component
public class FakerInputDevice {

    static final int REPORT_ID_KEYBOARD = 0x01;
    static final int REPORT_ID_RELATIVE_MOUSE = 0x03;
    static final int REPORT_ID_ABSOLUTE_MOUSE = 0x04;
    static final int REPORT_ID_CONTROL = 0x40;
    static final int CONTROL_REPORT_SIZE = 0x41;
    static final int KEYBOARD_REPORT_SIZE = 9;
    static final int RELATIVE_MOUSE_REPORT_SIZE = 8;
    static final int ABSOLUTE_MOUSE_REPORT_SIZE = 7;

    private final int requiredApiVersion;
    private final NativeConnectionFactory connectionFactory;
    private NativeConnection connection;
    private FakerInputDeviceStatus connectedStatus;

    @Autowired
    public FakerInputDevice(InputBackendProperties properties) {
        this(properties.getFakerInputRequiredApiVersion(), new JnaNativeConnectionFactory());
    }

    FakerInputDevice(int requiredApiVersion, NativeConnectionFactory connectionFactory) {
        if (requiredApiVersion <= 0) {
            throw new IllegalArgumentException("requiredApiVersion must be positive");
        }
        this.requiredApiVersion = requiredApiVersion;
        this.connectionFactory = connectionFactory;
    }

    /**
     * Probe driver readiness without publishing keyboard or mouse HID reports.
     *
     * @return classified availability and version information
     */
    public synchronized FakerInputDeviceStatus probe() {
        if (connection != null && connectedStatus != null) {
            return connectedStatus;
        }
        try (NativeConnection probeConnection = connectionFactory.open()) {
            if (probeConnection == null) {
                return new FakerInputDeviceStatus(
                        FakerInputDeviceState.DRIVER_MISSING, 0, 0, "FakerInput HID endpoints were not found");
            }
            return verify(probeConnection);
        } catch (Exception e) {
            return new FakerInputDeviceStatus(
                    FakerInputDeviceState.DRIVER_UNAVAILABLE, 0, 0,
                    e.getClass().getSimpleName() + ": " + safeMessage(e));
        }
    }

    /**
     * Open and retain the machine-wide FakerInput connection for later explicit input reports.
     *
     * @return readiness result; only {@link FakerInputDeviceState#DRIVER_READY} retains the connection
     */
    public synchronized FakerInputDeviceStatus connect() {
        if (connection != null && connectedStatus != null) {
            return connectedStatus;
        }
        NativeConnection candidate = null;
        try {
            candidate = connectionFactory.open();
            if (candidate == null) {
                return new FakerInputDeviceStatus(
                        FakerInputDeviceState.DRIVER_MISSING, 0, 0, "FakerInput HID endpoints were not found");
            }
            FakerInputDeviceStatus status = verify(candidate);
            if (status.state() != FakerInputDeviceState.DRIVER_READY) {
                candidate.close();
                return status;
            }
            connection = candidate;
            connectedStatus = status;
            log.info("FakerInput connected: apiVersion={} driverVersion={}",
                    status.apiVersion(), status.driverVersion());
            return status;
        } catch (Exception e) {
            closeQuietly(candidate);
            return new FakerInputDeviceStatus(
                    FakerInputDeviceState.DRIVER_UNAVAILABLE, 0, 0,
                    e.getClass().getSimpleName() + ": " + safeMessage(e));
        }
    }

    /**
     * Publish one standard six-key keyboard HID report.
     *
     * @param modifierFlags USB HID modifier bitmap
     * @param keyUsages zero to six USB HID keyboard usage codes
     */
    public synchronized void updateKeyboard(int modifierFlags, byte... keyUsages) {
        requireConnected().writeControlReport(encodeKeyboardReport(modifierFlags, keyUsages));
    }

    /**
     * Publish one relative mouse HID report.
     *
     * @param buttonFlags HID button bitmap
     * @param deltaX relative horizontal movement in physical pixels, in signed 16-bit range
     * @param deltaY relative vertical movement in physical pixels, in signed 16-bit range
     * @param wheel vertical wheel delta in signed 8-bit HID units
     * @param horizontalWheel horizontal wheel delta in signed 8-bit HID units
     */
    public synchronized void updateRelativeMouse(
            int buttonFlags,
            int deltaX,
            int deltaY,
            int wheel,
            int horizontalWheel) {
        requireConnected().writeControlReport(
                encodeRelativeMouseReport(buttonFlags, deltaX, deltaY, wheel, horizontalWheel));
    }

    /**
     * Publish one absolute mouse HID report using FakerInput's normalized screen coordinates.
     *
     * @param buttonFlags HID button bitmap
     * @param absoluteX normalized horizontal coordinate in {@code [0, 32767]}
     * @param absoluteY normalized vertical coordinate in {@code [0, 32767]}
     * @param wheel vertical wheel delta in signed 8-bit HID units
     */
    public synchronized void updateAbsoluteMouse(int buttonFlags, int absoluteX, int absoluteY, int wheel) {
        requireConnected().writeControlReport(
                encodeAbsoluteMouseReport(buttonFlags, absoluteX, absoluteY, wheel));
    }

    /** Release all keyboard modifiers/keys and mouse buttons before disconnecting or aborting input. */
    public synchronized void releaseAll() {
        if (connection == null) {
            return;
        }
        connection.writeControlReport(encodeKeyboardReport(0, new byte[0]));
        connection.writeControlReport(encodeRelativeMouseReport(0, 0, 0, 0, 0));
    }

    /** Close the retained HID handles. This method does not install or remove the driver. */
    @PreDestroy
    public synchronized void disconnect() {
        if (connection == null) {
            return;
        }
        try {
            releaseAll();
        } finally {
            closeQuietly(connection);
            connection = null;
            connectedStatus = null;
        }
    }

    static byte[] encodeKeyboardReport(int modifierFlags, byte[] keyUsages) {
        if ((modifierFlags & ~0xFF) != 0) {
            throw new IllegalArgumentException("modifierFlags must fit in one byte");
        }
        if (keyUsages != null && keyUsages.length > 6) {
            throw new IllegalArgumentException("FakerInput keyboard reports support at most six simultaneous keys");
        }
        byte[] report = new byte[CONTROL_REPORT_SIZE];
        report[0] = (byte) REPORT_ID_CONTROL;
        report[1] = (byte) KEYBOARD_REPORT_SIZE;
        report[2] = (byte) REPORT_ID_KEYBOARD;
        report[3] = (byte) modifierFlags;
        if (keyUsages != null) {
            System.arraycopy(keyUsages, 0, report, 5, keyUsages.length);
        }
        return report;
    }

    static byte[] encodeRelativeMouseReport(
            int buttonFlags,
            int deltaX,
            int deltaY,
            int wheel,
            int horizontalWheel) {
        requireByte("buttonFlags", buttonFlags, 0, 0xFF);
        requireByte("deltaX", deltaX, Short.MIN_VALUE, Short.MAX_VALUE);
        requireByte("deltaY", deltaY, Short.MIN_VALUE, Short.MAX_VALUE);
        requireByte("wheel", wheel, Byte.MIN_VALUE, Byte.MAX_VALUE);
        requireByte("horizontalWheel", horizontalWheel, Byte.MIN_VALUE, Byte.MAX_VALUE);

        byte[] report = new byte[CONTROL_REPORT_SIZE];
        report[0] = (byte) REPORT_ID_CONTROL;
        report[1] = (byte) RELATIVE_MOUSE_REPORT_SIZE;
        report[2] = (byte) REPORT_ID_RELATIVE_MOUSE;
        report[3] = (byte) buttonFlags;
        putLittleEndianShort(report, 4, deltaX);
        putLittleEndianShort(report, 6, deltaY);
        report[8] = (byte) wheel;
        report[9] = (byte) horizontalWheel;
        return report;
    }

    static byte[] encodeAbsoluteMouseReport(int buttonFlags, int absoluteX, int absoluteY, int wheel) {
        requireByte("buttonFlags", buttonFlags, 0, 0xFF);
        requireByte("absoluteX", absoluteX, 0, Short.MAX_VALUE);
        requireByte("absoluteY", absoluteY, 0, Short.MAX_VALUE);
        requireByte("wheel", wheel, Byte.MIN_VALUE, Byte.MAX_VALUE);

        byte[] report = new byte[CONTROL_REPORT_SIZE];
        report[0] = (byte) REPORT_ID_CONTROL;
        report[1] = (byte) ABSOLUTE_MOUSE_REPORT_SIZE;
        report[2] = (byte) REPORT_ID_ABSOLUTE_MOUSE;
        report[3] = (byte) buttonFlags;
        putLittleEndianShort(report, 4, absoluteX);
        putLittleEndianShort(report, 6, absoluteY);
        report[8] = (byte) wheel;
        return report;
    }

    private FakerInputDeviceStatus verify(NativeConnection candidate) {
        int driverVersion = candidate.driverVersion();
        if (!candidate.verifyClientApi(requiredApiVersion)) {
            return new FakerInputDeviceStatus(
                    FakerInputDeviceState.DRIVER_VERSION_UNSUPPORTED, 0, driverVersion,
                    "Driver rejected required API version " + requiredApiVersion);
        }
        int apiVersion = candidate.apiVersion();
        if (apiVersion < requiredApiVersion) {
            return new FakerInputDeviceStatus(
                    FakerInputDeviceState.DRIVER_VERSION_UNSUPPORTED, apiVersion, driverVersion,
                    "Driver API " + apiVersion + " is older than required API " + requiredApiVersion);
        }
        return new FakerInputDeviceStatus(
                FakerInputDeviceState.DRIVER_READY, apiVersion, driverVersion, "FakerInput is ready");
    }

    private NativeConnection requireConnected() {
        if (connection == null || connectedStatus == null
                || connectedStatus.state() != FakerInputDeviceState.DRIVER_READY) {
            throw new IllegalStateException("FakerInput is not connected and ready");
        }
        return connection;
    }

    private static void putLittleEndianShort(byte[] target, int offset, int value) {
        target[offset] = (byte) (value & 0xFF);
        target[offset + 1] = (byte) ((value >>> 8) & 0xFF);
    }

    private static void requireByte(String name, int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(name + " is outside [" + minimum + ", " + maximum + "]: " + value);
        }
    }

    private static String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? "no detail" : message;
    }

    private static void closeQuietly(NativeConnection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (Exception e) {
            log.warn("FakerInput disconnect failed: {}", e.getMessage());
        }
    }

    interface NativeConnectionFactory {
        NativeConnection open();
    }

    interface NativeConnection extends AutoCloseable {
        int driverVersion();

        boolean verifyClientApi(int apiVersion);

        int apiVersion();

        void writeControlReport(byte[] report);

        @Override
        void close();
    }

    private static final class JnaNativeConnectionFactory implements NativeConnectionFactory {

        private static final int FAKER_INPUT_VENDOR_ID = 0xFE0F;
        private static final int FAKER_INPUT_PRODUCT_ID = 0x00FF;
        private static final int VENDOR_USAGE_PAGE = 0xFF00;
        private static final int CONTROL_USAGE = 0x0001;
        private static final int METHOD_USAGE = 0x0002;

        @Override
        public NativeConnection open() {
            HANDLE control = openEndpoint(VENDOR_USAGE_PAGE, CONTROL_USAGE);
            if (control == null) {
                return null;
            }
            HANDLE method = openEndpoint(VENDOR_USAGE_PAGE, METHOD_USAGE);
            if (method == null) {
                Kernel32.INSTANCE.CloseHandle(control);
                throw new IllegalStateException("FakerInput API endpoint is unavailable");
            }
            return new JnaNativeConnection(control, method);
        }

        private static HANDLE openEndpoint(int expectedUsagePage, int expectedUsage) {
            GUID hidGuid = new GUID();
            HidLibrary.INSTANCE.HidD_GetHidGuid(hidGuid);
            HANDLE deviceInfo = SetupApi.INSTANCE.SetupDiGetClassDevs(
                    hidGuid,
                    null,
                    null,
                    SetupApi.DIGCF_PRESENT | SetupApi.DIGCF_DEVICEINTERFACE);
            if (isInvalid(deviceInfo)) {
                throw new IllegalStateException("SetupDiGetClassDevs failed: " + Kernel32.INSTANCE.GetLastError());
            }
            boolean fakerInputPathSeen = false;
            try {
                for (int index = 0; ; index++) {
                    SetupApi.SP_DEVICE_INTERFACE_DATA interfaceData = new SetupApi.SP_DEVICE_INTERFACE_DATA();
                    interfaceData.cbSize = interfaceData.size();
                    interfaceData.write();
                    if (!SetupApi.INSTANCE.SetupDiEnumDeviceInterfaces(
                            deviceInfo, null, hidGuid, index, interfaceData)) {
                        break;
                    }
                    String path = readDevicePath(deviceInfo, interfaceData);
                    if (path == null) {
                        continue;
                    }
                    boolean fakerInputPath = isFakerInputPath(path);
                    fakerInputPathSeen |= fakerInputPath;
                    HANDLE handle = Kernel32.INSTANCE.CreateFile(
                            path,
                            WinNT.GENERIC_READ | WinNT.GENERIC_WRITE,
                            WinNT.FILE_SHARE_READ | WinNT.FILE_SHARE_WRITE,
                            null,
                            WinNT.OPEN_EXISTING,
                            0,
                            null);
                    if (isInvalid(handle)) {
                        continue;
                    }
                    if (matches(handle, expectedUsagePage, expectedUsage)) {
                        return handle;
                    }
                    Kernel32.INSTANCE.CloseHandle(handle);
                }
                if (fakerInputPathSeen) {
                    throw new IllegalStateException(
                            "FakerInput HID device is present but the required endpoint cannot be opened");
                }
                return null;
            } finally {
                SetupApi.INSTANCE.SetupDiDestroyDeviceInfoList(deviceInfo);
            }
        }

        private static boolean isFakerInputPath(String path) {
            String normalized = path.toLowerCase(Locale.ROOT);
            return normalized.contains("vid_fe0f") && normalized.contains("pid_00ff");
        }

        private static String readDevicePath(
                HANDLE deviceInfo,
                SetupApi.SP_DEVICE_INTERFACE_DATA interfaceData) {
            IntByReference requiredLength = new IntByReference();
            SetupApi.INSTANCE.SetupDiGetDeviceInterfaceDetail(
                    deviceInfo, interfaceData, null, 0, requiredLength, null);
            int byteLength = requiredLength.getValue();
            if (byteLength <= 0) {
                return null;
            }
            Memory detail = new Memory(byteLength);
            detail.clear();
            detail.setInt(0, Native.POINTER_SIZE == 8 ? 8 : 6);
            if (!SetupApi.INSTANCE.SetupDiGetDeviceInterfaceDetail(
                    deviceInfo, interfaceData, detail, byteLength, requiredLength, null)) {
                return null;
            }
            return detail.getWideString(4);
        }

        private static boolean matches(HANDLE handle, int expectedUsagePage, int expectedUsage) {
            HiddAttributes attributes = new HiddAttributes();
            attributes.size = attributes.size();
            attributes.write();
            if (!HidLibrary.INSTANCE.HidD_GetAttributes(handle, attributes)) {
                return false;
            }
            attributes.read();
            if (Short.toUnsignedInt(attributes.vendorId) != FAKER_INPUT_VENDOR_ID
                    || Short.toUnsignedInt(attributes.productId) != FAKER_INPUT_PRODUCT_ID) {
                return false;
            }
            PointerByReference preparsedData = new PointerByReference();
            if (!HidLibrary.INSTANCE.HidD_GetPreparsedData(handle, preparsedData)) {
                return false;
            }
            try {
                HidpCaps caps = new HidpCaps();
                int status = HidLibrary.INSTANCE.HidP_GetCaps(preparsedData.getValue(), caps);
                caps.read();
                return status >= 0
                        && Short.toUnsignedInt(caps.usagePage) == expectedUsagePage
                        && Short.toUnsignedInt(caps.usage) == expectedUsage;
            } finally {
                HidLibrary.INSTANCE.HidD_FreePreparsedData(preparsedData.getValue());
            }
        }

        private static boolean isInvalid(HANDLE handle) {
            return handle == null
                    || handle.getPointer() == null
                    || Pointer.nativeValue(handle.getPointer()) == Pointer.nativeValue(WinBase.INVALID_HANDLE_VALUE.getPointer());
        }
    }

    private static final class JnaNativeConnection implements NativeConnection {

        private static final int REPORT_ID_CHECK_API_VERSION = 0x41;
        private static final int REPORT_ID_API_VERSION_FEATURE = 0x42;

        private HANDLE control;
        private HANDLE method;

        private JnaNativeConnection(HANDLE control, HANDLE method) {
            this.control = control;
            this.method = method;
        }

        @Override
        public int driverVersion() {
            HiddAttributes attributes = new HiddAttributes();
            attributes.size = attributes.size();
            attributes.write();
            if (!HidLibrary.INSTANCE.HidD_GetAttributes(control, attributes)) {
                return 0;
            }
            attributes.read();
            return Short.toUnsignedInt(attributes.versionNumber);
        }

        @Override
        public boolean verifyClientApi(int apiVersion) {
            byte[] report = new byte[CONTROL_REPORT_SIZE];
            report[0] = (byte) REPORT_ID_CHECK_API_VERSION;
            putLittleEndianInt(report, 4, apiVersion);
            return write(method, report);
        }

        @Override
        public int apiVersion() {
            byte[] report = new byte[CONTROL_REPORT_SIZE];
            report[0] = (byte) REPORT_ID_API_VERSION_FEATURE;
            if (!HidLibrary.INSTANCE.HidD_GetFeature(method, report, report.length)) {
                return 0;
            }
            return readLittleEndianInt(report, 4);
        }

        @Override
        public void writeControlReport(byte[] report) {
            if (report == null || report.length != CONTROL_REPORT_SIZE) {
                throw new IllegalArgumentException("FakerInput control report must contain 65 bytes");
            }
            // Match the official FakerInput client: HIDCLASS may report the inner payload length even though
            // the complete 65-byte output report was accepted, so WriteFile success is authoritative here.
            if (!write(control, report)) {
                throw new IllegalStateException("FakerInput HID report write failed");
            }
        }

        @Override
        public void close() {
            if (!JnaNativeConnectionFactory.isInvalid(control)) {
                Kernel32.INSTANCE.CloseHandle(control);
            }
            if (!JnaNativeConnectionFactory.isInvalid(method)) {
                Kernel32.INSTANCE.CloseHandle(method);
            }
            control = null;
            method = null;
        }

        private static boolean write(HANDLE handle, byte[] report) {
            IntByReference written = new IntByReference();
            return Kernel32.INSTANCE.WriteFile(handle, report, report.length, written, null);
        }

        private static void putLittleEndianInt(byte[] target, int offset, int value) {
            target[offset] = (byte) (value & 0xFF);
            target[offset + 1] = (byte) ((value >>> 8) & 0xFF);
            target[offset + 2] = (byte) ((value >>> 16) & 0xFF);
            target[offset + 3] = (byte) ((value >>> 24) & 0xFF);
        }

        private static int readLittleEndianInt(byte[] source, int offset) {
            return Byte.toUnsignedInt(source[offset])
                    | (Byte.toUnsignedInt(source[offset + 1]) << 8)
                    | (Byte.toUnsignedInt(source[offset + 2]) << 16)
                    | (Byte.toUnsignedInt(source[offset + 3]) << 24);
        }
    }

    private interface HidLibrary extends Library {
        HidLibrary INSTANCE = Native.load("hid", HidLibrary.class, W32APIOptions.DEFAULT_OPTIONS);

        void HidD_GetHidGuid(GUID hidGuid);

        boolean HidD_GetAttributes(HANDLE handle, HiddAttributes attributes);

        boolean HidD_GetPreparsedData(HANDLE handle, PointerByReference preparsedData);

        boolean HidD_FreePreparsedData(Pointer preparsedData);

        int HidP_GetCaps(Pointer preparsedData, HidpCaps capabilities);

        boolean HidD_GetFeature(HANDLE handle, byte[] report, int reportLength);
    }

    @Structure.FieldOrder({"size", "vendorId", "productId", "versionNumber"})
    public static final class HiddAttributes extends Structure {
        public int size;
        public short vendorId;
        public short productId;
        public short versionNumber;
    }

    @Structure.FieldOrder({
            "usage", "usagePage", "inputReportByteLength", "outputReportByteLength", "featureReportByteLength",
            "reserved", "numberLinkCollectionNodes", "numberInputButtonCaps", "numberInputValueCaps",
            "numberInputDataIndices", "numberOutputButtonCaps", "numberOutputValueCaps", "numberOutputDataIndices",
            "numberFeatureButtonCaps", "numberFeatureValueCaps", "numberFeatureDataIndices"
    })
    public static final class HidpCaps extends Structure {
        public short usage;
        public short usagePage;
        public short inputReportByteLength;
        public short outputReportByteLength;
        public short featureReportByteLength;
        public short[] reserved = new short[17];
        public short numberLinkCollectionNodes;
        public short numberInputButtonCaps;
        public short numberInputValueCaps;
        public short numberInputDataIndices;
        public short numberOutputButtonCaps;
        public short numberOutputValueCaps;
        public short numberOutputDataIndices;
        public short numberFeatureButtonCaps;
        public short numberFeatureValueCaps;
        public short numberFeatureDataIndices;
    }
}
