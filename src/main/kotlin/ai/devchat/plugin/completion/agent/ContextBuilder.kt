package ai.devchat.plugin.completion.agent

const val MAX_CONTEXT_TOKENS = 6000
const val LINE_SEPARATOR = '\n'


fun String.tokenCount(): Int {
    var count = 0
    var isPrevWhiteSpace = true
    for (char in this) {
        if (char.isWhitespace()) {
            isPrevWhiteSpace = true
        } else {
            if (isPrevWhiteSpace) count++
            isPrevWhiteSpace = false
        }
    }
    return count
}


fun String.lineSequence(offset: Int? = null) = sequence {
    val str = this@lineSequence
    var prev = offset ?: 0
    while (prev < str.length) {
        val cur =  str.indexOf(LINE_SEPARATOR, startIndex = prev).let {
            if (it == -1) str.length else it + 1
        }
        val range = prev until cur
        yield(Pair(range, str.substring(range)))
        prev = cur
    }
}

fun String.lineSequenceReversed(offset: Int? = null) = sequence {
    val str = this@lineSequenceReversed
    var prev = offset ?: str.length
    while (prev > 0) {
        val cur = if (prev <= 1) 0 else {
            str.lastIndexOf(
                LINE_SEPARATOR,
                startIndex = if (str[prev-1] == LINE_SEPARATOR) {
                    prev - 2
                } else {
                    prev - 1
                }
            ) + 1
        }
        val range = cur until prev
        yield(Pair(range, str.substring(range)))
        prev = cur
    }
}

class ContextBuilder(val filepath: String, val content: String, val offset: Int) {
    private fun buildFileContext(): Pair<String, String> {
        val maxTokens = MAX_CONTEXT_TOKENS * 0.35

        val maxPrefixTokens = (maxTokens * 0.7).toInt()
        var prefixTokens = 0
        val prefixStart = content.lineSequenceReversed(offset).takeWhile {(_, line) ->
            val numTokens = line.tokenCount()
            if (prefixTokens + numTokens > maxPrefixTokens) return@takeWhile false
            prefixTokens += numTokens
            true
        }.lastOrNull()?.first?.first ?: 0

        val maxSuffixTokens = maxTokens - prefixTokens
        var suffixTokens = 0
        val suffixEnd = content.lineSequence(offset).takeWhile {(_, line) ->
            val numTokens = line.tokenCount()
            if (suffixTokens + numTokens > maxSuffixTokens) return@takeWhile false
            suffixTokens += numTokens
            true
        }.lastOrNull()?.first?.last ?: content.length

        return Pair(
            content.substring(prefixStart, offset),
            content.substring(offset, suffixEnd)
        )
    }

    fun createPrompt(model: String?): String {
        val (prefix, suffix) = buildFileContext()
        return  if (!model.isNullOrEmpty() && model.contains("deepseek"))
            "<｜fim▁begin｜>$filepath\n\n$prefix<｜fim▁hole｜>$suffix<｜fim▁end｜>"
        else
            "<fim_prefix>$filepath\n\n$prefix<fim_suffix>$suffix<fim_middle>"
    }
}