package com.example.brokerfi.notification.update;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UpdateReminderClientTest {
    @Test
    public void newerSemanticVersionIsAvailable() {
        assertTrue(UpdateReminderClient.isNewerVersion("2.0.5", "2.0.4"));
        assertTrue(UpdateReminderClient.isNewerVersion("V2.1.0", "2.0.9"));
        assertTrue(UpdateReminderClient.isNewerVersion("2.1", "2.0.9"));
    }

    @Test
    public void equalOrOlderVersionIsNotAvailable() {
        assertFalse(UpdateReminderClient.isNewerVersion("2.0.4", "2.0.4"));
        assertFalse(UpdateReminderClient.isNewerVersion("2.0.3", "2.0.4"));
        assertFalse(UpdateReminderClient.isNewerVersion("2.0.4.0", "2.0.4"));
    }

    @Test
    public void malformedVersionDoesNotTriggerUpdate() {
        assertFalse(UpdateReminderClient.isNewerVersion("latest", "2.0.4"));
        assertFalse(UpdateReminderClient.isNewerVersion("2.0.5", "unknown"));
        assertFalse(UpdateReminderClient.isNewerVersion("release-2.0.5", "2.0.4"));
        assertFalse(UpdateReminderClient.isNewerVersion("2..5", "2.0.4"));
    }
}
