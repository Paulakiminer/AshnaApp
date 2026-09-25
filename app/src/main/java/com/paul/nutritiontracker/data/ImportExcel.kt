package com.paul.nutritiontracker.data

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

data class ImportedSheet(val name: String, val rows: List<List<String>>)

/**
 * Minimal .xlsx reader: an xlsx file is a zip of XML parts. We read
 * xl/workbook.xml (sheet names + order), xl/_rels/workbook.xml.rels
 * (id -> file path), xl/sharedStrings.xml (string table), and each
 * xl/worksheets/sheetN.xml (actual cell data). No external library needed.
 */
object XlsxReader {

    fun read(context: Context, uri: Uri): List<ImportedSheet> {
        val entries = mutableMapOf<String, ByteArray>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val out = ByteArrayOutputStream()
                        val buf = ByteArray(8192)
                        var n: Int
                        while (zip.read(buf).also { n = it } >= 0) {
                            if (n > 0) out.write(buf, 0, n)
                        }
                        entries[entry.name] = out.toByteArray()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: throw IllegalStateException("Could not open file")

        val workbookXml = entries["xl/workbook.xml"] ?: throw IllegalStateException("Not a valid .xlsx file")
        val relsXml = entries["xl/_rels/workbook.xml.rels"]

        val sharedStrings = entries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()
        val ridToPath = if (relsXml != null) parseRels(relsXml) else emptyMap()
        val sheetsMeta = parseWorkbookSheets(workbookXml) // list of (name, rId)

        val sheets = mutableListOf<ImportedSheet>()
        sheetsMeta.forEachIndexed { idx, (name, rId) ->
            val path = ridToPath[rId]?.let { "xl/$it" } ?: "xl/worksheets/sheet${idx + 1}.xml"
            val sheetBytes = entries[path] ?: entries["xl/worksheets/sheet${idx + 1}.xml"]
            if (sheetBytes != null) {
                val rows = parseSheet(sheetBytes, sharedStrings)
                sheets.add(ImportedSheet(name, rows))
            }
        }
        return sheets
    }

    private fun colIndexFromRef(ref: String): Int {
        val letters = ref.takeWhile { it.isLetter() }
        var idx = 0
        for (ch in letters) {
            idx = idx * 26 + (ch.uppercaseChar() - 'A' + 1)
        }
        return idx - 1
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")
        var event = parser.eventType
        var current: StringBuilder? = null
        var depthInSi = false
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "si") { depthInSi = true; current = StringBuilder() }
                }
                XmlPullParser.TEXT -> {
                    if (depthInSi) current?.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "si") {
                        result.add(current?.toString() ?: "")
                        depthInSi = false
                        current = null
                    }
                }
            }
            event = parser.next()
        }
        return result
    }

    private fun parseRels(bytes: ByteArray): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "Relationship") {
                val id = parser.getAttributeValue(null, "Id")
                val target = parser.getAttributeValue(null, "Target")
                if (id != null && target != null) map[id] = target.removePrefix("/xl/")
            }
            event = parser.next()
        }
        return map
    }

    /** Returns list of (sheetName, relationshipId) in workbook order. */
    private fun parseWorkbookSheets(bytes: ByteArray): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "sheet") {
                val name = parser.getAttributeValue(null, "name") ?: "Sheet"
                var rId: String? = null
                for (i in 0 until parser.attributeCount) {
                    if (parser.getAttributeName(i).endsWith("id")) {
                        rId = parser.getAttributeValue(i)
                    }
                }
                result.add(name to (rId ?: ""))
            }
            event = parser.next()
        }
        return result
    }

    private fun parseSheet(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(bytes.inputStream(), "UTF-8")
        var event = parser.eventType

        var currentRow: MutableList<String>? = null
        var cellType: String? = null
        var cellColIndex = -1
        var cellValue = StringBuilder()
        var inValueTag = false
        var inInlineStr = false

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> currentRow = mutableListOf()
                        "c" -> {
                            cellType = parser.getAttributeValue(null, "t")
                            val ref = parser.getAttributeValue(null, "r") ?: ""
                            cellColIndex = if (ref.isNotEmpty()) colIndexFromRef(ref) else (currentRow?.size ?: 0)
                            cellValue = StringBuilder()
                        }
                        "v" -> inValueTag = true
                        "is" -> inInlineStr = true
                        "t" -> if (inInlineStr) inValueTag = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inValueTag) cellValue.append(parser.text)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> inValueTag = false
                        "t" -> if (inInlineStr) inValueTag = false
                        "is" -> inInlineStr = false
                        "c" -> {
                            val raw = cellValue.toString()
                            val resolved = when (cellType) {
                                "s" -> raw.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: ""
                                "str", "inlineStr" -> raw
                                "b" -> if (raw == "1") "TRUE" else "FALSE"
                                else -> raw
                            }
                            currentRow?.let { row ->
                                while (row.size <= cellColIndex) row.add("")
                                if (cellColIndex >= 0) row[cellColIndex] = resolved
                            }
                        }
                        "row" -> {
                            currentRow?.let { rows.add(it) }
                            currentRow = null
                        }
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }
}
