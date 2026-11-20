package online.capitalai.devicecare.core.quality

import java.util.GregorianCalendar
import java.util.TimeZone

/** SECURITY_RESEARCH@1: deterministic offline metadata classification; never a malware verdict. */
object ResearchDeduplication {
    enum class Outcome { NEW_PUBLICATION, MATERIAL_REVISION, DUPLICATE }
    data class Publication(
        val title: String,
        val doi: String? = null,
        val arxivId: String? = null,
        val publishedOn: String,
        val materialChange: Boolean = false
    )
    data class Identity(val doi: String?, val arxivBaseId: String?, val arxivVersion: Int?)
    data class Result(val outcome: Outcome, val identity: Identity)

    private val doiPattern = Regex("^10\\.[0-9]{4,9}/[^\\s]+$", RegexOption.IGNORE_CASE)
    private val arxivPattern = Regex("^(?:arxiv:)?([0-9]{4}\\.[0-9]{4,5})(?:v([1-9][0-9]*))?$", RegexOption.IGNORE_CASE)
    private val datePattern = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")

    private fun normalizeDoi(raw: String?): String? {
        if (raw == null) return null
        val value = raw.trim().replace(Regex("^https?://(?:dx\\.)?doi\\.org/", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^doi:", RegexOption.IGNORE_CASE), "").lowercase()
        require(doiPattern.matches(value)) { "Invalid DOI" }
        return value
    }
    fun identity(p: Publication): Identity {
        require(p.title.isNotBlank()) { "Title required" }
        require(datePattern.matches(p.publishedOn)) { "Invalid publication date" }
        val year = p.publishedOn.substring(0, 4).toInt()
        val month = p.publishedOn.substring(5, 7).toInt()
        val day = p.publishedOn.substring(8, 10).toInt()
        val calendar = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            isLenient = false
            clear()
            set(year, month - 1, day)
        }
        calendar.timeInMillis // reject impossible dates
        val doi = normalizeDoi(p.doi)
        val arxiv = p.arxivId?.trim()?.let {
            val match = arxivPattern.matchEntire(it) ?: throw IllegalArgumentException("Invalid arXiv ID")
            val version = match.groupValues[2].takeIf(String::isNotEmpty)?.toIntOrNull()
            require(match.groupValues[2].isEmpty() || version != null) { "Invalid arXiv version" }
            match.groupValues[1] to version
        }
        require(doi != null || arxiv != null) { "DOI or arXiv ID required" }
        return Identity(doi, arxiv?.first, arxiv?.second)
    }
    fun classify(candidate: Publication, prior: List<Publication>): Result {
        val key = identity(candidate)
        val matches = prior.map { it to identity(it) }.filter { (_, old) ->
            (key.doi != null && key.doi == old.doi) ||
                (key.arxivBaseId != null && key.arxivBaseId == old.arxivBaseId)
        }
        if (matches.isEmpty()) return Result(Outcome.NEW_PUBLICATION, key)
        val newestDate = matches.maxOf { it.first.publishedOn }
        val highestVersion = matches.mapNotNull { it.second.arxivVersion }.maxOrNull()
        val newerVersion = key.arxivVersion != null &&
            (highestVersion == null || key.arxivVersion > highestVersion)
        val newerDate = candidate.publishedOn > newestDate
        val revision = candidate.materialChange && (newerVersion || newerDate)
        return Result(if (revision) Outcome.MATERIAL_REVISION else Outcome.DUPLICATE, key)
    }
}
