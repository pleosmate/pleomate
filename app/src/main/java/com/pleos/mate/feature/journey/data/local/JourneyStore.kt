package com.pleos.mate.feature.journey.data.local

import com.pleos.mate.feature.journey.data.JourneyDataSource
import com.pleos.mate.feature.journey.domain.Journey
import com.pleos.mate.feature.journey.domain.JourneyArchive
import com.pleos.mate.feature.journey.domain.OdometerSource
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Pending interval, saved memories and consumed IDs are committed in one atomic record. */
class JourneyStore(directory: File) : JourneyDataSource {
    private val record = File(directory, "journeys-v1")

    override fun read(): JourneyArchive {
        if (!record.exists()) return JourneyArchive()
        return DataInputStream(record.inputStream().buffered()).use { input ->
            val version = input.readInt().also { require(it in 1..5) }
            val pending = if (input.readBoolean()) input.journey(version) else null
            val memories = List(input.count()) { input.journey(version) }
            val handled = List(input.count()) { input.readUTF() }.toSet()
            val firstSavedAt = if (version >= 2) {
                if (input.readBoolean()) input.readLong() else null
            } else memories.minOfOrNull { it.endedAt }
            require(input.read() == -1)
            require(firstSavedAt == null || firstSavedAt > 0)
            require(memories.map { it.id }.distinct().size == memories.size)
            require(memories.all { it.id in handled } && (pending == null || pending.id !in handled))
            JourneyArchive(pending, memories, handled, firstSavedAt)
        }
    }

    override fun save(archive: JourneyArchive) {
        val pending = File(record.parentFile, "journeys-v1.pending")
        pending.outputStream().use { stream ->
            val output = DataOutputStream(stream)
            output.writeInt(5)
            output.writeBoolean(archive.pending != null)
            archive.pending?.let { output.journey(it) }
            output.writeInt(archive.memories.size)
            archive.memories.forEach { output.journey(it) }
            output.writeInt(archive.handledIds.size)
            archive.handledIds.forEach(output::writeUTF)
            output.writeBoolean(archive.firstSavedAt != null)
            archive.firstSavedAt?.let(output::writeLong)
            output.flush()
            stream.fd.sync()
        }
        Files.move(pending.toPath(), record.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun DataInputStream.count(): Int = readInt().also { require(it in 0..100_000) }
    private fun DataInputStream.journey(version: Int): Journey {
        val id = readUTF(); val start = readLong(); val end = readLong(); val meters = readDouble(); val duration = readInt()
        val theme = if (version >= 3) readUTF() else ""
        val memo = if (version >= 3) readUTF() else ""
        val source = if (version >= 4) OdometerSource.valueOf(readUTF()) else OdometerSource.TRIP
        return Journey(id, start, end, meters, if (version >= 4 && duration == -1) null else duration, theme, memo, source)
    }
    private fun DataOutputStream.journey(value: Journey) {
        writeUTF(value.id); writeLong(value.startedAt); writeLong(value.endedAt); writeDouble(value.meters); writeInt(value.minutes ?: -1); writeUTF(value.theme); writeUTF(value.memo); writeUTF(value.source.name)
    }
}
