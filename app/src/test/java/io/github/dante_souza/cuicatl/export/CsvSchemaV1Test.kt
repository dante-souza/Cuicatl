// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvSchemaV1Test {
    @Test
    fun schemaOneContainsTheValidatedPortableMeasurementContext() {
        val columns = CsvSchemaV1.columns

        assertEquals("1", CsvSchemaV1.VERSION)
        assertTrue(columns.contains("session_id"))
        assertTrue(columns.contains("duration_ms"))
        assertTrue(columns.contains("timing_quality"))
        assertTrue(columns.contains("session_start_utc"))
        assertTrue(columns.contains("session_timezone_offset"))
        assertTrue(columns.contains("interval_start_utc_estimate"))
        assertTrue(columns.contains("audio_source"))
        assertTrue(columns.contains("signal_state"))
        assertTrue(columns.contains("clipped_sample_count"))
        assertTrue(columns.contains("quality_flags"))
        assertTrue(columns.contains("session_elapsed_ms"))
        assertTrue(columns.contains("session_captured_ms"))
        assertTrue(columns.contains("interruption_reason"))
        assertFalse(columns.contains("segment_id"))
    }

    @Test
    fun schemaOneHasNoDuplicateColumns() {
        assertEquals(CsvSchemaV1.columns.size, CsvSchemaV1.columns.distinct().size)
    }
}
