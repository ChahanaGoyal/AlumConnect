package com.alumconnect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Basic tests to verify project foundation and testing setup.
 */
class AlumConnectAppTest {

    @Test
    @DisplayName("Application metadata should be configured")
    void testAppMetadata() {
        assertEquals("AlumConnect", AlumConnectApp.APP_NAME);
        assertNotNull(AlumConnectApp.VERSION);
    }
}
