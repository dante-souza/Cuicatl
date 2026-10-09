// SPDX-License-Identifier: AGPL-3.0-or-later
package io.github.dante_souza.cuicatl.export

object CsvFormat {
    fun field(value: String?): String {
        if (value == null) return ""
        val escaped = value.replace("\"", "\"\"")
        return if (
            escaped.contains(',') ||
            escaped.contains('"') ||
            escaped.contains('\n') ||
            escaped.contains('\r')
        ) {
            "\"$escaped\""
        } else {
            escaped
        }
    }

    /**
     * Draft Phase 1 spreadsheet-safety rule for user-controlled text.
     *
     * A leading apostrophe is added when common spreadsheet formula prefixes
     * are detected. Schema 1 remains provisional until a real exported fixture
     * is inspected externally.
     */
    fun spreadsheetSafeText(value: String): String =
        if (value.firstOrNull() in setOf('=', '+', '-', '@')) "'$value" else value
}
