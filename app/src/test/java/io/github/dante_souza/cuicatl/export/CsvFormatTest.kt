// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.export

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvFormatTest {
    @Test
    fun quotesCommaQuoteAndNewline() {
        assertEquals("\"a,b\"", CsvFormat.field("a,b"))
        assertEquals("\"a\"\"b\"", CsvFormat.field("a\"b"))
        assertEquals("\"a\nb\"", CsvFormat.field("a\nb"))
    }

    @Test
    fun keepsNumericZeroDistinctFromEmpty() {
        assertEquals("0", CsvFormat.field("0"))
        assertEquals("", CsvFormat.field(null))
    }

    @Test
    fun protectsCommonSpreadsheetFormulaPrefixesInUserText() {
        assertEquals("'=1+1", CsvFormat.spreadsheetSafeText("=1+1"))
        assertEquals("field recording", CsvFormat.spreadsheetSafeText("field recording"))
    }
}
