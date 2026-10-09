// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.export

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProvisionalExportFieldsTest {
    @Test
    fun candidateFieldsCarryContextWithoutPretendingSchemaOneIsFinal() {
        val columns = ProvisionalExportFields.candidateColumns

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
        assertFalse(columns.contains("segment_id"))
    }
}
