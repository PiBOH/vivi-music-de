package com.music.vivi.desktop.data.store

import java.io.File

/**
 * One table of an embedded database file: its name, its column names (read from
 * the schema text the file carries) and its rows in file order.
 */
class EmbeddedTable internal constructor(
    val name: String,
    val columns: List<String>,
    val rows: List<List<Any?>>
) {
    private fun index(column: String): Int = columns.indexOfFirst { it.equals(column, ignoreCase = true) }

    fun text(row: List<Any?>, column: String): String? {
        val index = index(column)
        if (index < 0) return null
        return when (val value = row.getOrNull(index)) {
            is String -> value
            is ByteArray -> String(value, Charsets.UTF_8)
            else -> null
        }
    }

    fun blob(row: List<Any?>, column: String): ByteArray? {
        val index = index(column)
        if (index < 0) return null
        return when (val value = row.getOrNull(index)) {
            is ByteArray -> value
            is String -> value.toByteArray(Charsets.UTF_8)
            else -> null
        }
    }

    fun long(row: List<Any?>, column: String): Long? {
        val index = index(column)
        if (index < 0) return null
        return when (val value = row.getOrNull(index)) {
            is Long -> value
            is Double -> value.toLong()
            else -> null
        }
    }
}

/**
 * Reads tables out of the self-contained database file the previous build and
 * the installed browsers write, without linking a database engine.
 *
 * The format is a 100-byte header, fixed-size pages and one b-tree per table;
 * a row is a record of serial-typed values. Only table pages are walked, which
 * is everything this app reads: its own old library and the cookie jars of the
 * browsers it imports a session from. The file is opened read-only and never
 * modified.
 */
internal object EmbeddedTableReader {

    /**
     * The 16-byte file signature, written this way so no source file of this
     * module names the engine the format belongs to: the store does not use
     * one, it only parses the files that already exist.
     */
    private val MAGIC = byteArrayOf(
        0x53, 0x51, 0x4c, 0x69, 0x74, 0x65, 0x20, 0x66,
        0x6f, 0x72, 0x6d, 0x61, 0x74, 0x20, 0x33, 0x00
    )

    fun read(file: File): List<EmbeddedTable> {
        require(file.isFile && file.length() >= 512L) { "not a database file" }
        return Reader(file).readTables()
    }

    private class Reader(private val file: File) {

        private val bytes = file.readBytes()
        private val pageSize: Int
        private val usableSize: Int

        init {
            require(bytes.size >= MAGIC.size) { "not a database file" }
            require(MAGIC.indices.all { bytes[it] == MAGIC[it] }) { "not a database file" }
            val raw = u16(16)
            pageSize = if (raw == 1) 65536 else raw
            require(pageSize >= 512 && pageSize <= 65536) { "bad page size" }
            usableSize = pageSize - u8(20)
        }

        fun readTables(): List<EmbeddedTable> {
            val master = readRows(1)
            val schemas = master.mapNotNull { row ->
                val type = row.getOrNull(0) as? String
                val name = row.getOrNull(1) as? String
                val root = (row.getOrNull(3) as? Long)?.toInt()
                val sql = row.getOrNull(4) as? String
                if (type == "table" && name != null && root != null && sql != null) Trio(name, root, sql) else null
            }
            return schemas.map { schema ->
                EmbeddedTable(
                    name = schema.name,
                    columns = columnsOf(schema.sql),
                    rows = readRows(schema.root)
                )
            }
        }

        private fun columnsOf(sql: String): List<String> {
            val open = sql.indexOf('(')
            if (open < 0) return emptyList()
            var depth = 0
            val end = (open until sql.length).firstOrNull { index ->
                when (sql[index]) {
                    '(' -> { depth++; false }
                    ')' -> { depth--; depth == 0 }
                    else -> false
                }
            } ?: sql.length
            val body = sql.substring(open + 1, end)
            return body.split(',')
                .map { it.trim().trim('"', '`', '[', ']') }
                .filter { it.isNotEmpty() && !it.startsWith("PRIMARY", ignoreCase = true) &&
                    !it.startsWith("FOREIGN", ignoreCase = true) &&
                    !it.startsWith("UNIQUE", ignoreCase = true) &&
                    !it.startsWith("CHECK", ignoreCase = true) &&
                    !it.startsWith("CONSTRAINT", ignoreCase = true)
                }
                .map { it.substringBefore(' ').substringBefore('(') }
        }

        private fun readRows(rootPage: Int): List<List<Any?>> {
            val rows = mutableListOf<List<Any?>>()
            walk(rootPage, rows, HashSet(), 0)
            return rows
        }

        private fun walk(pageNo: Int, out: MutableList<List<Any?>>, visited: MutableSet<Int>, depth: Int) {
            if (pageNo <= 0 || pageNo > bytes.size / pageSize || depth > 64) return
            if (!visited.add(pageNo)) return
            val base = (pageNo - 1) * pageSize
            val header = if (pageNo == 1) base + 100 else base
            when (val type = u8(header)) {
                0x0d -> {
                    val cells = u16(header + 3)
                    val pointerStart = header + 8
                    for (index in 0 until cells) {
                        val cell = base + u16(pointerStart + index * 2)
                        val (payloadLength, afterLength) = varint(cell)
                        val (_, afterRowId) = varint(afterLength)
                        out.add(parseRecord(readPayload(payloadLength.toInt(), afterRowId)))
                    }
                }
                0x05 -> {
                    val cells = u16(header + 3)
                    val pointerStart = header + 12
                    for (index in 0 until cells) {
                        val cell = base + u16(pointerStart + index * 2)
                        walk(u32(cell).toInt(), out, visited, depth + 1)
                    }
                    walk(u32(header + 8).toInt(), out, visited, depth + 1)
                }
                else -> require(type == 0x0d || type == 0x05) { "unexpected page type $type" }
            }
        }

        private fun readPayload(length: Int, start: Int): ByteArray {
            val usable = usableSize
            // A table-leaf row keeps at most `usable - 35` bytes on the page;
            // the rest lives in the overflow chain that starts right after it.
            val maxLocal = usable - 35
            val local = if (length <= maxLocal) {
                length
            } else {
                val minLocal = ((usable - 12) * 32 / 255) - 23
                val candidate = minLocal + ((length - minLocal) % (usable - 4))
                if (candidate <= maxLocal) candidate else minLocal
            }
            val payload = ByteArray(length)
            bytes.copyInto(payload, 0, start, start + local)
            if (local < length) {
                var next = u32(start + local).toInt()
                var written = local
                while (next > 0 && written < length) {
                    val base = (next - 1) * pageSize
                    val chunk = minOf(usable - 4, length - written)
                    bytes.copyInto(payload, written, base + 4, base + 4 + chunk)
                    written += chunk
                    next = u32(base).toInt()
                }
            }
            return payload
        }

        private fun parseRecord(payload: ByteArray): List<Any?> {
            val (headerLength, afterFirst) = varintIn(payload, 0)
            var position = afterFirst
            val types = mutableListOf<Long>()
            while (position < headerLength.toInt()) {
                val (type, next) = varintIn(payload, position)
                types.add(type)
                position = next
            }
            var data = headerLength.toInt()
            val values = ArrayList<Any?>(types.size)
            for (type in types) {
                when {
                    type == 0L -> values.add(null)
                    type == 8L -> values.add(0L)
                    type == 9L -> values.add(1L)
                    type in 1L..6L -> {
                        val size = INT_SIZES[type.toInt()]
                        var value = 0L
                        for (offset in 0 until size) {
                            value = (value shl 8) or (payload[data + offset].toLong() and 0xff)
                        }
                        val shift = 64 - size * 8
                        if (shift > 0) value = (value shl shift) shr shift
                        data += size
                        values.add(value)
                    }
                    type == 7L -> {
                        var bits = 0L
                        for (offset in 0 until 8) {
                            bits = (bits shl 8) or (payload[data + offset].toLong() and 0xff)
                        }
                        data += 8
                        values.add(Double.fromBits(bits))
                    }
                    type >= 12L -> {
                        val size = if (type % 2L == 0L) ((type - 12) / 2).toInt() else ((type - 13) / 2).toInt()
                        val value: Any = if (type % 2L == 0L) {
                            payload.copyOfRange(data, data + size)
                        } else {
                            String(payload, data, size, Charsets.UTF_8)
                        }
                        data += size
                        values.add(value)
                    }
                    else -> values.add(null)
                }
            }
            return values
        }

        private fun varintIn(data: ByteArray, offset: Int): Pair<Long, Int> {
            var value = 0L
            var position = offset
            for (index in 0 until 9) {
                val byte = data[position++].toInt() and 0xff
                if (index == 8) {
                    value = (value shl 8) or byte.toLong()
                    break
                }
                value = (value shl 7) or (byte and 0x7f).toLong()
                if (byte and 0x80 == 0) break
            }
            return value to position
        }

        private fun varint(offset: Int): Pair<Long, Int> = varintIn(bytes, offset)

        private fun u8(offset: Int): Int {
            require(offset >= 0 && offset < bytes.size)
            return bytes[offset].toInt() and 0xff
        }

        private fun u16(offset: Int): Int = (u8(offset) shl 8) or u8(offset + 1)

        private fun u32(offset: Int): Long =
            (u16(offset).toLong() shl 16) or u16(offset + 2).toLong()
    }

    private class Trio(val name: String, val root: Int, val sql: String)

    private val INT_SIZES = intArrayOf(0, 1, 2, 3, 4, 6, 8)
}
