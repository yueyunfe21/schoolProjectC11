package com.bot.dhxy.cloud.remote;

/**
 * Typed observation marker mirrored from the cloud request context.
 *
 * <p>Absent (key missing) means a normal mechanical command with unchanged canonical bytes and
 * authorization. {@code PAUSED_READ_ONLY} permits only WINDOW_FACT/CAPTURE reads against the
 * exact current PAUSED registration revision; it is rejected for EXECUTE_INPUT_BUNDLE by strict
 * schema and by the local pre-side-effect gate. An explicit {@code null} or an unknown value
 * fails Jackson deserialization and is rejected as the transport's existing DESERIALIZATION
 * typed failure.</p>
 */
public enum RemoteObservationMode {
    PAUSED_READ_ONLY
}
