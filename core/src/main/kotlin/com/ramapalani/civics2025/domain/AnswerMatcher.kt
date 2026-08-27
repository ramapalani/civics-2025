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
        "star spangled banner" to "starspangled banner",
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
            return MatchResult(false, emptyList(), 0.0)
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
                    best = MatchResult(score >= 0.78, if (score >= 0.78) listOf(answer) else emptyList(), score)
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
        return MatchResult(correct, matched, if (matched.isEmpty()) 0.0 else 1.0)
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
        return if (bestIdx >= 0 && bestScore >= 0.78) bestIdx to accepted[bestIdx] else null
    }

    private fun markFamily(accepted: List<String>, chosen: String, used: MutableSet<Int>) {
        accepted.forEachIndexed { index, answer ->
            if (index in used) return@forEachIndexed
            val score = similarity(normalize(chosen), normalize(answer), expandVariants(answer))
            if (score >= 0.78) used += index
        }
    }

    private fun uniqueFamilies(matched: List<String>): List<String> {
        val unique = mutableListOf<String>()
        for (answer in matched) {
            val already = unique.any { existing ->
                similarity(normalize(existing), normalize(answer), expandVariants(answer)) >= 0.78
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
        text = text.replace(Regex("""\bd c\b"""), "dc")
        text = text.replace(Regex("""\bjuly 4(?!\s*1776)\b"""), "independence day")
        text = text.replace(Regex("[^a-z0-9\\s]"), " ")
        text = text.replace(Regex("\\s+"), " ").trim()
        return collapseNumberWords(text)
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
        val onlyParens = Regex("""\(([^)]*)\)""").findAll(answer).map { it.groupValues[1] }.toList()
        return (listOf(answer, withoutParens) + onlyParens).map { normalize(it) }.filter { it.isNotBlank() }.distinct()
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
        if (dist > allowed) return 0.0
        val prefix = a.take(2) == b.take(2) || a.first() == b.first()
        if (!prefix && dist > 1) return 0.0
        return 1.0 - (dist.toDouble() / maxLen)
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
