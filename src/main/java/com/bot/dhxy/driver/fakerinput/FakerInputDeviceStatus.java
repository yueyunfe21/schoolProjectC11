package com.bot.dhxy.driver.fakerinput;

/**
 * Result of a no-keyboard/no-mouse FakerInput readiness probe.
 *
 * @param state classified driver state
 * @param apiVersion driver API version, or zero when it could not be read
 * @param driverVersion HID driver version number, or zero when it could not be read
 * @param detail diagnostic detail suitable for logs and a future installation UI
 */
public record FakerInputDeviceStatus(
        FakerInputDeviceState state,
        int apiVersion,
        int driverVersion,
        String detail) {
}
