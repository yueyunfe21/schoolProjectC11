package com.bot.dhxy.driver.fakerinput;

/** Availability state of the machine-wide FakerInput virtual HID device. */
public enum FakerInputDeviceState {
    DRIVER_MISSING,
    DRIVER_VERSION_UNSUPPORTED,
    DRIVER_UNAVAILABLE,
    DRIVER_READY
}
