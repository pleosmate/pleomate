package com.pleos.mate.feature.journey.domain

enum class OdometerSource { TRIP, TOTAL, MANUAL }

/** SDK distance in meters; unavailable driving time stays null. */
data class OdometerSample(val meters: Double, val minutes: Int?, val wallMillis: Long, val elapsedMillis: Long, val source: OdometerSource = OdometerSource.TRIP) {
    val valid: Boolean get() = source != OdometerSource.MANUAL && meters.isFinite() && meters >= 0 && (minutes == null || minutes >= 0) && wallMillis > 0 && elapsedMillis >= 0
}

data class Journey(val id: String, val startedAt: Long, val endedAt: Long, val meters: Double, val minutes: Int?, val theme: String = "", val memo: String = "", val source: OdometerSource = OdometerSource.TRIP) {
    init {
        require(id.isNotBlank() && startedAt > 0 && endedAt >= startedAt)
        require(if (source == OdometerSource.MANUAL) meters == 0.0 && minutes == null else meters.isFinite() && meters > 0 && (minutes == null || minutes >= 0))
        require(theme.length <= 30 && memo.length <= 200)
    }
}

/** Keeps only one explicitly observed interval. A gap/reset ends it before the unknown interval. */
class JourneyTracker(private val id: String) {
    private var first: OdometerSample? = null
    private var last: OdometerSample? = null
    var segment: Journey? = null
        private set
    var ended = false
        private set

    fun accept(sample: OdometerSample): Boolean {
        if (ended) return false
        val previous = last
        if (!sample.valid || (previous != null &&
                (sample.elapsedMillis <= previous.elapsedMillis || sample.elapsedMillis - previous.elapsedMillis > 15_000 ||
                    sample.source != previous.source || (sample.minutes == null) != (previous.minutes == null) ||
                    sample.wallMillis < previous.wallMillis || sample.meters < previous.meters ||
                    (sample.minutes != null && previous.minutes != null && sample.minutes < previous.minutes)))) {
            ended = true
            return false
        }
        if (first == null) first = sample
        last = sample
        val baseline = requireNotNull(first)
        if (sample.meters > baseline.meters) {
            segment = Journey(id, baseline.wallMillis, sample.wallMillis, sample.meters - baseline.meters, if (sample.minutes != null && baseline.minutes != null) sample.minutes - baseline.minutes else null, source = sample.source)
        }
        return true
    }
}

enum class JourneyEditorMode(val storageValue: String) {
    PENDING("pending"), EDIT("edit"), MANUAL("manual");

    companion object {
        fun fromStorage(value: String?) = entries.firstOrNull { it.storageValue == value } ?: MANUAL
    }
}

data class JourneyArchive(
    val pending: Journey? = null,
    val memories: List<Journey> = emptyList(),
    val handledIds: Set<String> = emptySet(),
    val firstSavedAt: Long? = null
) {
    fun observe(segment: Journey): JourneyArchive {
        if (segment.id in handledIds) return this
        require(pending == null || pending.id == segment.id)
        return copy(pending = segment)
    }

    fun savePending(id: String): JourneyArchive {
        val draft = pending?.takeIf { it.id == id && id !in handledIds } ?: return this
        return copy(
            pending = null,
            memories = memories + draft,
            handledIds = handledIds + id,
            firstSavedAt = firstSavedAt ?: draft.endedAt.takeUnless { draft.source == OdometerSource.MANUAL }
        )
    }

    fun skipPending(id: String): JourneyArchive = if (pending?.id == id) {
        copy(pending = null, handledIds = handledIds + id)
    } else this

    fun deleteMemory(id: String): JourneyArchive = copy(memories = memories.filterNot { it.id == id })

    fun editMemory(editor: JourneyEditor): JourneyArchive {
        require(editor.mode == JourneyEditorMode.EDIT)
        if (memories.none { it.id == editor.id }) return this
        return copy(memories = memories.map { memory ->
            if (memory.id == editor.id) memory.copy(theme = editor.theme, memo = editor.memo.trim()) else memory
        })
    }

    fun saveManual(editor: JourneyEditor, now: Long): JourneyArchive {
        val memory = editor.toManualJourney(now)
        if (memory.id in handledIds) return this
        return copy(memories = memories + memory, handledIds = handledIds + memory.id)
    }
}

/** Only user-authored metadata is editable; measured values and identity stay intact. */
data class JourneyEditor(val id: String, val mode: JourneyEditorMode, val theme: String = "일상", val memo: String = "") {
    init {
        require(id.isNotBlank())
        require(theme.length <= 30 && memo.length <= 200)
    }

    fun updateText(theme: String, memo: String) = copy(theme = theme.take(30), memo = memo.take(200))

    fun toManualJourney(now: Long): Journey {
        require(mode == JourneyEditorMode.MANUAL)
        return Journey(id, now, now, 0.0, null, theme, memo.trim(), OdometerSource.MANUAL)
    }
}

