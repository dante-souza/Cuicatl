// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvSchemaTest {
    @Test
    fun schemaOneContainsPortableMeasurementContext() {
        assertEquals(1, CsvSchema.VERSION)
        assertTrue(CsvSchema.measurementColumns.contains("session_id"))
        assertTrue(CsvSchema.measurementColumns.contains("duration_ms"))
        assertTrue(CsvSchema.measurementColumns.contains("sample_rate_hz"))
        assertTrue(CsvSchema.measurementColumns.contains("signal_state"))
        assertTrue(CsvSchema.measurementColumns.contains("quality_flags"))
    }
}
