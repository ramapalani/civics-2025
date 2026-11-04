package com.ramapalani.civics2025.domain

object AnswerMatcher {
    private val stopWords = setOf(
        "a", "an", "the", "of", "to", "in", "for", "on", "at", "by",
        "from", "with", "as", "or", "is", "are", "was", "were", "be",
        "that", "this", "it", "and",
    )

    private val aliases = listOf(
        "united states of america" to "united states",
        "u s a" to "united states",
        "usa" to "united states",
        "u s" to "united states",
        "us " to "united states ",
        "constitution based" to "constitutionbased",
        "vice president" to "vicepresident",
        "vice-president" to "vicepresident",
        "commander in chief" to "commanderin chief",
        "world war ii" to "world war 2",
        "world war 2" to "world war 2",
        "world war i" to "world war 1",
        "wwii" to "world war 2",
        "ww2" to "world war 2",
        "wwi" to "world war 1",
        "ww1" to "world war 1",
        "ussr" to "soviet union",
        "j d vance" to "jd vance",
        "j.d. vance" to "jd vance",
        "donald j trump" to "donald trump",
        "donald john trump" to "donald trump",
        "john g roberts jr" to "john roberts",
        "john roberts junior" to "john roberts",
        "washington dc" to "washington dc",
        "washington d c" to "washington dc",
        "fourth of july" to "independence day",
        "4th of july" to "independence day",
        "house of reps" to "house of representatives",
        "sec of " to "secretary of ",
        "judiciary" to "judicial",
        "mlk day" to "martin luther king jr day",
        "martin luther king day" to "martin luther king jr day",
        "xmas" to "christmas",
        "9 11 2001" to "september 11 2001",
        "sept 11 2001" to "september 11 2001",
        "nine eleven" to "september 11 2001",
    )

    private val stateAbbreviations = mapOf(
        "al" to "alabama", "ak" to "alaska", "az" to "arizona", "ar" to "arkansas",
        "ca" to "california", "co" to "colorado", "ct" to "connecticut", "de" to "delaware",
        "fl" to "florida", "ga" to "georgia", "hi" to "hawaii", "id" to "idaho",
        "il" to "illinois", "in" to "indiana", "ia" to "iowa", "ks" to "kansas",
        "ky" to "kentucky", "la" to "louisiana", "me" to "maine", "md" to "maryland",
        "ma" to "massachusetts", "mi" to "michigan", "mn" to "minnesota", "ms" to "mississippi",
        "mo" to "missouri", "mt" to "montana", "ne" to "nebraska", "nv" to "nevada",
        "nh" to "new hampshire", "nj" to "new jersey", "nm" to "new mexico", "ny" to "new york",
        "nc" to "north carolina", "nd" to "north dakota", "oh" to "ohio", "ok" to "oklahoma",
        "or" to "oregon", "pa" to "pennsylvania", "ri" to "rhode island", "sc" to "south carolina",
        "sd" to "south dakota", "tn" to "tennessee", "tx" to "texas", "ut" to "utah",
        "vt" to "vermont", "va" to "virginia", "wa" to "washington", "wv" to "west virginia",
        "wi" to "wisconsin", "wy" to "wyoming",
    )

    /** State codes that are also ordinary words; the word reading wins. */
    private val ambiguousStateCodes = setOf("in", "or", "me", "la", "oh", "hi", "ok", "id")

    private val initialisms = mapOf(
        "fdr" to "franklin roosevelt",
        "jfk" to "john f kennedy",
        "lbj" to "lyndon b johnson",
        "teddy roosevelt" to "theodore roosevelt",
        "ike" to "dwight d eisenhower",
    )

    /** Words that rule a capitalized phrase out of being a personal name. */
    private val nonNameWords = setOf(
        "day", "birthday", "year", "years", "eve", "war", "court", "house", "senate",
        "state", "states", "united", "america", "american", "constitution", "congress",
        "president", "party", "city", "island", "north", "south", "east", "west", "new",
        "bill", "rights", "act", "amendment", "flag", "banner", "independence", "memorial",
        "labor", "christmas", "thanksgiving", "veterans",
    )

    /** Guards on the stem fallback, kept strict so only inflections of one word match. */
    private const val STEM_MIN_LENGTH = 6
    private const val STEM_MIN_PREFIX = 5
    private const val STEM_MIN_PREFIX_RATIO = 0.85
    private const val STEM_MIN_DICE = 0.60
    private const val STEM_MATCH_SCORE = 0.85

    private val genericTokens = setOf("day", "birthday", "jr", "junior", "sr", "senior")

    private val weakAlone = setOf(
        "people", "law", "government", "states", "united", "president", "court",
        "right", "rights", "war", "bill", "branch", "house", "senate", "state",
        "citizens", "vote", "day", "flag", "america", "american", "country",
        "one", "two", "three", "part", "parts",
    )

    private val ones = mapOf(
        "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10, "eleven" to 11,
        "twelve" to 12, "thirteen" to 13, "fourteen" to 14, "fifteen" to 15, "sixteen" to 16,
        "seventeen" to 17, "eighteen" to 18, "nineteen" to 19,
        "first" to 1, "second" to 2, "third" to 3, "fourth" to 4, "fifth" to 5,
        "sixth" to 6, "seventh" to 7, "eighth" to 8, "ninth" to 9, "tenth" to 10,
        "eleventh" to 11, "twelfth" to 12, "thirteenth" to 13, "fourteenth" to 14,
        "fifteenth" to 15, "sixteenth" to 16, "seventeenth" to 17, "eighteenth" to 18,
        "nineteenth" to 19,
    )

    private val tens = mapOf(
        "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
        "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90,
        "twentieth" to 20, "thirtieth" to 30, "fortieth" to 40, "fiftieth" to 50,
    )

    private val months = listOf(
        "", "january", "february", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december",
    )

    fun match(
        question: CivicsQuestion,
        userInput: String,
        officials: LocalOfficials = LocalOfficials(),
    ): MatchResult {
        val accepted = acceptedFor(question, officials)
        if (userInput.isBlank()) {
            // Keep the score's meaning tied to the question, not to how the answer failed.
            return when (question.kind) {
                AnswerKind.ANY_ONE -> MatchResult(false, emptyList(), 0.0)
                AnswerKind.ALL_N ->
                    MatchResult(false, emptyList(), 0.0, question.minRequired, ScoreKind.COVERAGE)
            }
        }

        return when (question.kind) {
            AnswerKind.ANY_ONE -> matchAnyOne(accepted, userInput)
            AnswerKind.ALL_N -> matchAllN(accepted, userInput, question.minRequired)
        }
    }

    fun acceptedFor(question: CivicsQuestion, officials: LocalOfficials): List<String> {
        if (question.currentOfficial) {
            val override = officials.federalName(question.stateField).trim()
            if (override.isNotBlank()) {
                return nameVariants(override)
            }
        }
        val extras = mutableListOf<String>()
        if (question.stateSpecific) {
            when (question.stateField) {
                "senator" -> if (officials.senator.isNotBlank()) extras += officials.senator
                "representative" -> if (officials.representative.isNotBlank()) extras += officials.representative
                "governor" -> if (officials.governor.isNotBlank()) extras += officials.governor
                "capital" -> if (officials.stateCapital.isNotBlank()) extras += officials.stateCapital
            }
        }
        return question.acceptedAnswers + extras
    }

    internal fun nameVariants(name: String): List<String> {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return emptyList()
        val skip = setOf("jr", "jr.", "junior", "sr", "sr.", "senior", "ii", "iii", "iv")
        val parts = trimmed
            .replace(",", " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it.lowercase() !in skip }
        val variants = mutableListOf(trimmed)
        if (parts.size >= 2) {
            variants += parts.last()
        }
        return variants.distinct()
    }

    private fun matchAnyOne(accepted: List<String>, userInput: String): MatchResult {
        val probes = (listOf(userInput) + splitAnswers(userInput)).distinct()
        var best = MatchResult(false, emptyList(), 0.0)
        for (probe in probes) {
            val userNorm = normalize(probe)
            for (answer in accepted) {
                val score = similarity(userNorm, normalize(answer), expandVariants(answer))
                if (score > best.similarity) {
                    val hit = score >= MatchResult.ACCEPT_THRESHOLD
                    best = MatchResult(hit, if (hit) listOf(answer) else emptyList(), score)
                }
            }
        }
        return best
    }

    private fun matchAllN(accepted: List<String>, userInput: String, minRequired: Int): MatchResult {
        val fromParts = collectMatches(accepted, splitAnswers(userInput))
        val fromBag = collectMatchesFromBag(accepted, userInput)
        val matched = uniqueFamilies(fromParts + fromBag)
        val correct = matched.size >= minRequired
        // Report real coverage ("2 of 3") rather than a flat pass/fail, so a partial answer is visible.
        val coverage = if (minRequired <= 0) 0.0 else {
            (matched.size.toDouble() / minRequired).coerceAtMost(1.0)
        }
        return MatchResult(correct, matched, coverage, minRequired, ScoreKind.COVERAGE)
    }

    private fun collectMatches(accepted: List<String>, parts: List<String>): List<String> {
        val used = mutableSetOf<Int>()
        val matched = mutableListOf<String>()
        for (part in parts) {
            val hit = bestAccepted(accepted, part, used) ?: continue
            used += hit.first
            markFamily(accepted, hit.second, used)
            matched += hit.second
        }
        return matched
    }

    private fun collectMatchesFromBag(accepted: List<String>, userInput: String): List<String> {
        val userTokens = tokens(normalize(userInput))
        if (userTokens.isEmpty()) return emptyList()
        val usedTokens = BooleanArray(userTokens.size)
        val usedAnswers = mutableSetOf<Int>()
        val matched = mutableListOf<String>()
        val order = accepted.indices.sortedByDescending { tokens(normalize(accepted[it])).size }
        for (index in order) {
            if (index in usedAnswers) continue
            val answer = accepted[index]
            val placed = placeAnswer(userTokens, usedTokens, answer) ?: continue
            placed.forEach { usedTokens[it] = true }
            usedAnswers += index
            markFamily(accepted, answer, usedAnswers)
            matched += answer
        }
        return matched
    }

    private fun bestAccepted(
        accepted: List<String>,
        part: String,
        used: Set<Int>,
    ): Pair<Int, String>? {
        var bestIdx = -1
        var bestScore = 0.0
        accepted.forEachIndexed { index, answer ->
            if (index in used) return@forEachIndexed
            val score = similarity(normalize(part), normalize(answer), expandVariants(answer))
            if (score > bestScore) {
                bestScore = score
                bestIdx = index
            }
        }
        return if (bestIdx >= 0 && bestScore >= MatchResult.ACCEPT_THRESHOLD) bestIdx to accepted[bestIdx] else null
    }

    private fun markFamily(accepted: List<String>, chosen: String, used: MutableSet<Int>) {
        accepted.forEachIndexed { index, answer ->
            if (index in used) return@forEachIndexed
            val score = similarity(normalize(chosen), normalize(answer), expandVariants(answer))
            if (score >= MatchResult.ACCEPT_THRESHOLD) used += index
        }
    }

    private fun uniqueFamilies(matched: List<String>): List<String> {
        val unique = mutableListOf<String>()
        for (answer in matched) {
            val already = unique.any { existing ->
                similarity(normalize(existing), normalize(answer), expandVariants(answer)) >= MatchResult.ACCEPT_THRESHOLD
            }
            if (!already) unique += answer
        }
        return unique
    }

    private fun placeAnswer(user: List<String>, used: BooleanArray, answer: String): List<Int>? {
        val official = tokens(normalize(answer))
        val content = dropGeneric(official)
        if (content.isEmpty()) return null
        placeOrdered(user, used, content)?.let { return it }
        return placeUnordered(user, used, content)
    }

    private fun placeOrdered(user: List<String>, used: BooleanArray, content: List<String>): List<Int>? {
        for (start in user.indices) {
            if (used[start]) continue
            val consumed = mutableListOf<Int>()
            var ai = 0
            var ui = start
            while (ai < content.size && ui < user.size) {
                if (used[ui]) {
                    ui += 1
                    continue
                }
                if (tokenPairScore(user[ui], content[ai]) >= 0.82) {
                    consumed += ui
                    ai += 1
                    ui += 1
                } else {
                    break
                }
            }
            if (ai == content.size) return consumed
        }
        return null
    }

    private fun placeUnordered(user: List<String>, used: BooleanArray, content: List<String>): List<Int>? {
        val local = used.copyOf()
        val consumed = mutableListOf<Int>()
        for (need in content) {
            val idx = user.indices.firstOrNull {
                !local[it] && tokenPairScore(user[it], need) >= 0.82
            } ?: return null
            local[idx] = true
            consumed += idx
        }
        return consumed
    }

    fun splitAnswers(raw: String): List<String> {
        return raw
            .replace(Regex("""^\s*[-•·*]+\s*""", RegexOption.MULTILINE), "")
            .replace(Regex("""^\s*\d+[.)]\s*""", RegexOption.MULTILINE), "")
            .split(Regex("""\s*(?:,|;|/|\n|\r|\band\b|\bthen\b|\+)\s*""", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    fun normalize(raw: String): String {
        var text = raw.lowercase().trim()
        text = text.replace("&", " and ")
        text = text.replace(Regex("""\b(u\.s\.a\.|u\.s\.a|u\.s\.|u\.s)\b"""), "united states")
        text = text.replace(Regex("""(\d+)(st|nd|rd|th)\b"""), "$1")
        text = text.replace(Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{4})\b""")) { match ->
            val month = match.groupValues[1].toInt()
            val day = match.groupValues[2]
            val year = match.groupValues[3]
            val monthName = months.getOrNull(month) ?: match.groupValues[1]
            "$monthName $day $year"
        }
        aliases.forEach { (from, to) ->
            text = text.replace(from, to)
        }
        text = text.replace(Regex("[^a-z0-9\\s]"), " ")
        text = text.replace(Regex("\\s+"), " ").trim()
        text = text.replace(Regex("""\bd c\b"""), "dc")
        text = reorderDates(text)
        // Runs after punctuation is stripped so the 1776 guard sees "july 4 1776", not "july 4, 1776".
        text = text.replace(Regex("""\bjuly 4(?!\s+1776)\b"""), "independence day")
        text = expandAbbreviations(text)
        return collapseNumberWords(text)
    }

    /** Rewrites "4 july 1776" to "july 4 1776" so day-first input matches the official month-first wording. */
    private fun reorderDates(text: String): String {
        val monthNames = months.drop(1).joinToString("|")
        return text.replace(Regex("""\b(\d{1,2})\s+($monthNames)\b(\s+\d{4})?""")) { match ->
            val day = match.groupValues[1]
            val month = match.groupValues[2]
            val year = match.groupValues[3]
            "$month $day$year"
        }
    }

    /**
     * Expands the whole input when it is nothing but a state code or initialism. Codes that are also
     * ordinary English words are skipped, since "in" or "me" is far more likely to be the word.
     */
    private fun expandAbbreviations(text: String): String {
        initialisms[text]?.let { return it }
        if (text in ambiguousStateCodes) return text
        stateAbbreviations[text]?.let { return it }
        return text
    }

    private fun collapseNumberWords(text: String): String {
        val parts = text.split(" ").filter { it.isNotBlank() }
        val out = mutableListOf<String>()
        var i = 0
        while (i < parts.size) {
            val parsed = parseNumberAt(parts, i)
            if (parsed != null) {
                out += parsed.first.toString()
                i = parsed.second
            } else {
                out += parts[i]
                i += 1
            }
        }
        return out.joinToString(" ")
    }

    private fun parseNumberAt(parts: List<String>, start: Int): Pair<Int, Int>? {
        var i = start
        var value = 0
        var consumed = false
        if (i < parts.size && parts[i] in ones && i + 1 < parts.size && parts[i + 1] == "hundred") {
            value += (ones[parts[i]] ?: 0) * 100
            i += 2
            consumed = true
            if (i < parts.size && parts[i] == "and") i += 1
        }
        if (i < parts.size && parts[i] in tens) {
            value += tens.getValue(parts[i])
            i += 1
            consumed = true
            if (i < parts.size && parts[i] in ones && ones.getValue(parts[i]) < 10) {
                value += ones.getValue(parts[i])
                i += 1
            }
        } else if (i < parts.size && parts[i] in ones) {
            value += ones.getValue(parts[i])
            i += 1
            consumed = true
        }
        return if (consumed) value to i else null
    }

    private fun expandVariants(answer: String): List<String> {
        val withoutParens = answer.replace(Regex("""\([^)]*\)"""), " ")
        val parenGroups = Regex("""\(([^)]*)\)""").findAll(answer).map { it.groupValues[1] }.toList()
        // "Automobile (cars, internal combustion engine)" lists alternatives, so each is its own answer.
        val parenParts = parenGroups.flatMap { it.split(",") }
        val surnames = surnameVariants(withoutParens)
        return (listOf(answer, withoutParens) + parenGroups + parenParts + surnames)
            .map { normalize(it) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    /**
     * A personal name also answers to its surname alone ("Mott" for "Lucretia Mott"). Only applies to
     * capitalized multi-word phrases with no common noun, so "New Year's Day" is not read as a name.
     */
    private fun surnameVariants(answer: String): List<String> {
        val words = answer.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size < 2 || words.size > 4) return emptyList()
        if (!words.all { it.first().isUpperCase() || it.endsWith(".") }) return emptyList()
        if (words.any { it.trimEnd('.', ',').lowercase() in nonNameWords }) return emptyList()
        val skip = setOf("jr", "jr.", "junior", "sr", "sr.", "senior", "ii", "iii", "iv")
        val last = words.last { it.lowercase() !in skip }.trimEnd('.', ',')
        return if (last.length >= 3) listOf(last) else emptyList()
    }

    internal fun similarity(user: String, official: String, variants: List<String>): Double {
        if (user.isBlank()) return 0.0
        val candidates = (listOf(official) + variants).distinct()
        return candidates.maxOf { candidate ->
            if (user == candidate) 1.0
            else tokenSimilarity(tokens(user), tokens(candidate))
        }
    }

    private fun tokens(text: String): List<String> {
        return text.split(" ").filter { it.isNotBlank() && it !in stopWords && (it.length > 1 || it.any { ch -> ch.isDigit() }) }
    }

    private fun dropGeneric(tokens: List<String>): List<String> {
        var trimmed = tokens
        while (trimmed.isNotEmpty() && trimmed.last() in genericTokens) {
            trimmed = trimmed.dropLast(1)
        }
        return trimmed.ifEmpty { tokens }
    }

    private fun tokenSimilarity(user: List<String>, official: List<String>): Double {
        if (official.isEmpty() || user.isEmpty()) return 0.0
        val officialScore = maxOf(coverage(user, official), coverage(user, dropGeneric(official)).let { if (dropGeneric(official).isEmpty()) 0.0 else it })
        val userScore = coverage(official, user)
        if (isDistinctive(user) && userScore >= 0.85) {
            return maxOf(officialScore, 0.86)
        }
        return officialScore
    }

    private fun isDistinctive(user: List<String>): Boolean {
        val content = user.filter { it !in weakAlone }
        if (content.any { it.any(Char::isDigit) }) return true
        if (content.any { it.length >= 5 }) return true
        return content.size >= 2 && content.all { it.length >= 3 }
    }

    private fun coverage(user: List<String>, official: List<String>): Double {
        if (official.isEmpty()) return 0.0
        val used = BooleanArray(user.size)
        var matched = 0
        for (need in official) {
            var bestIdx = -1
            var best = 0.0
            user.forEachIndexed { index, got ->
                if (used[index]) return@forEachIndexed
                val score = tokenPairScore(got, need)
                if (score > best) {
                    best = score
                    bestIdx = index
                }
            }
            if (bestIdx >= 0 && best >= 0.82) {
                used[bestIdx] = true
                matched += 1
            }
        }
        return matched.toDouble() / official.size
    }

    private fun tokenPairScore(a: String, b: String): Double {
        if (a == b) return 1.0
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 1.0
        val dist = levenshtein(a, b)
        val allowed = if (maxLen <= 4) 1 else 2
        val prefix = a.take(2) == b.take(2) || a.first() == b.first()
        if (dist <= allowed && (prefix || dist <= 1)) {
            return 1.0 - (dist.toDouble() / maxLen)
        }
        return stemPairScore(a, b)
    }

    /**
     * Rescues inflections of the same word, which edit distance misses because the endings differ by
     * several characters ("naturalize" and "naturalization" are four edits apart). Both words must be
     * long, share a long common prefix, and differ only past that prefix, so unrelated words that
     * merely start alike ("constitution" and "constitutional" yes, "president" and "presidency" yes,
     * but "senate" and "senator" only because they genuinely share a stem) are the intended hits.
     */
    private fun stemPairScore(a: String, b: String): Double {
        if (a.length < STEM_MIN_LENGTH || b.length < STEM_MIN_LENGTH) return 0.0
        val shared = a.commonPrefixWith(b).length
        if (shared < STEM_MIN_PREFIX) return 0.0
        // The stem must be nearly all of the shorter word. This is what separates a true inflection
        // ("govern" in "government", ratio 1.0) from a coincidental prefix ("contin-ent" vs
        // "contin-ue", ratio 0.75), which trigram overlap alone scores almost identically.
        if (shared.toDouble() / minOf(a.length, b.length) < STEM_MIN_PREFIX_RATIO) return 0.0
        if (trigramDice(a, b) < STEM_MIN_DICE) return 0.0
        // A stem match is a deliberate near-equal, scored just above the coverage bar rather than
        // by raw overlap, which for these pairs sits below it.
        return STEM_MATCH_SCORE
    }

    /** Dice coefficient over character trigrams: 2*shared / total, robust to differing suffixes. */
    private fun trigramDice(a: String, b: String): Double {
        val left = trigrams(a)
        val right = trigrams(b)
        if (left.isEmpty() || right.isEmpty()) return 0.0
        val counts = HashMap<String, Int>()
        left.forEach { counts[it] = (counts[it] ?: 0) + 1 }
        var shared = 0
        right.forEach { gram ->
            val remaining = counts[gram] ?: 0
            if (remaining > 0) {
                counts[gram] = remaining - 1
                shared += 1
            }
        }
        return (2.0 * shared) / (left.size + right.size)
    }

    private fun trigrams(text: String): List<String> {
        val padded = "  $text "
        if (padded.length < 3) return emptyList()
        return (0..padded.length - 3).map { padded.substring(it, it + 3) }
    }

    internal fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(curr[j - 1] + 1, prev[j] + 1, prev[j - 1] + cost)
            }
            for (j in curr.indices) prev[j] = curr[j]
        }
        return prev[b.length]
    }
}
