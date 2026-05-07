package dev.mariinkys.kantan.util

data class ConjugationTable(val rows: List<ConjugationRow>)

data class ConjugationRow(
    val label: String,
    val affirmative: String,
    val negative: String
)

object VerbConjugator {

    private val VERB_TAGS = setOf(
        "v1", "v1-s", "vz",
        "v5", "v5aru", "v5b", "v5g", "v5k", "v5k-s",
        "v5m", "v5n", "v5r", "v5r-i", "v5s", "v5t", "v5u", "v5u-s", "v5uru",
        "vk", "vs", "vs-i", "vs-s", "vs-c", "vn", "vr"
    )

    /**
     * Returns a [ConjugationTable] if [posTags] contains a recognized verb tag,
     * or null if the entry is not a verb (so the UI can hide the tab).
     */

    fun conjugate(expression: String, posTags: List<String>): ConjugationTable? {
        val tag = posTags.firstOrNull { it in VERB_TAGS } ?: return null
        return when {
            tag == "vk"
                    || expression == "くる"
                    || expression == "来る" -> conjugateKuru(expression)

            tag == "vs-i"
                    || tag == "vs"
                    || expression.endsWith("する") -> conjugateSuru(expression)

            tag == "v1"
                    || tag == "v1-s"
                    || tag == "vz" -> conjugateIchidan(expression)

            tag.startsWith("v5") -> conjugateGodan(expression, tag)

            else -> null
        }
    }

    // Ichidan

    private fun conjugateIchidan(expression: String): ConjugationTable {
        // Drop the final る
        val stem = expression.dropLast(1)
        return table(
            row("Non-past", "${stem}る", "${stem}ない"),
            row("Non-past, polite", "${stem}ます", "${stem}ません"),
            row("Past", "${stem}た", "${stem}なかった"),
            row("Past, polite", "${stem}ました", "${stem}ませんでした"),
            row("Te-form", "${stem}て", "${stem}なくて"),
            row("Potential", "${stem}られる", "${stem}られない"),
            row("Passive", "${stem}られる", "${stem}られない"),
            row("Causative", "${stem}させる", "${stem}させない"),
            row("Causative Passive", "${stem}させられる", "${stem}させられない"),
            row("Imperative", "${stem}ろ", "${stem}るな"),
        )
    }

    // Godan

    private data class GodanForms(
        val aStem: String,   // negative / passive / causative base
        val iStem: String,   // polite base
        val eStem: String,   // potential / imperative base
        val teForm: String,  // full te-form (stem + connector)
        val taForm: String,  // full past plain form
    )

    private fun godanForms(stem: String, tag: String): GodanForms = when (tag) {
        "v5u", "v5u-s" -> GodanForms(
            "${stem}わ",
            "${stem}い",
            "${stem}え",
            "${stem}って",
            "${stem}った"
        )

        "v5k" -> GodanForms("${stem}か", "${stem}き", "${stem}け", "${stem}いて", "${stem}いた")
        "v5k-s" -> GodanForms(
            "${stem}か",
            "${stem}き",
            "${stem}け",
            "${stem}って",
            "${stem}った"
        ) // いく irregular te
        "v5g" -> GodanForms("${stem}が", "${stem}ぎ", "${stem}げ", "${stem}いで", "${stem}いだ")
        "v5s" -> GodanForms("${stem}さ", "${stem}し", "${stem}せ", "${stem}して", "${stem}した")
        "v5t" -> GodanForms("${stem}た", "${stem}ち", "${stem}て", "${stem}って", "${stem}った")
        "v5n" -> GodanForms("${stem}な", "${stem}に", "${stem}ね", "${stem}んで", "${stem}んだ")
        "v5b" -> GodanForms("${stem}ば", "${stem}び", "${stem}べ", "${stem}んで", "${stem}んだ")
        "v5m" -> GodanForms("${stem}ま", "${stem}み", "${stem}め", "${stem}んで", "${stem}んだ")
        "v5r", "v5r-i",
        "v5aru" -> GodanForms("${stem}ら", "${stem}り", "${stem}れ", "${stem}って", "${stem}った")

        "v5uru" -> GodanForms("${stem}ら", "${stem}り", "${stem}れ", "${stem}って", "${stem}った")
        else -> GodanForms("${stem}わ", "${stem}い", "${stem}え", "${stem}って", "${stem}った")
    }

    private fun conjugateGodan(expression: String, tag: String): ConjugationTable {
        val stem = expression.dropLast(1)
        val f = godanForms(stem, tag)
        return table(
            row("Non-past", expression, "${f.aStem}ない"),
            row("Non-past, polite", "${f.iStem}ます", "${f.iStem}ません"),
            row("Past", f.taForm, "${f.aStem}なかった"),
            row("Past, polite", "${f.iStem}ました", "${f.iStem}ませんでした"),
            row("Te-form", f.teForm, "${f.aStem}なくて"),
            row("Potential", "${f.eStem}る", "${f.eStem}ない"),
            row("Passive", "${f.aStem}れる", "${f.aStem}れない"),
            row("Causative", "${f.aStem}せる", "${f.aStem}せない"),
            row("Causative Passive", "${f.aStem}せられる", "${f.aStem}せられない"),
            row("Imperative", f.eStem, "${expression}な"),
        )
    }

    // する (Group 3 exception)

    private fun conjugateSuru(expression: String): ConjugationTable {
        // Works for bare する and compounds like 勉強する, 運動する
        val prefix = if (expression == "する") "" else expression.dropLast(2)
        return table(
            row("Non-past", "${prefix}する", "${prefix}しない"),
            row("Non-past, polite", "${prefix}します", "${prefix}しません"),
            row("Past", "${prefix}した", "${prefix}しなかった"),
            row("Past, polite", "${prefix}しました", "${prefix}しませんでした"),
            row("Te-form", "${prefix}して", "${prefix}しなくて"),
            row("Potential", "${prefix}できる", "${prefix}できない"),
            row("Passive", "${prefix}される", "${prefix}されない"),
            row("Causative", "${prefix}させる", "${prefix}させない"),
            row("Causative Passive", "${prefix}させられる", "${prefix}させられない"),
            row("Imperative", "${prefix}しろ", "${prefix}するな"),
        )
    }

    // くる / 来る (Group 3 exception)

    private fun conjugateKuru(expression: String): ConjugationTable {
        // くる and 来る have the same spoken forms; kanji reading changes with conjugation
        val kanji = expression == "来る"
        fun k(hiragana: String, kanjiForm: String) = if (kanji) kanjiForm else hiragana
        return table(
            row("Non-past", k("くる", "来る"), k("こない", "来ない")),
            row("Non-past, polite", k("きます", "来ます"), k("きません", "来ません")),
            row("Past", k("きた", "来た"), k("こなかった", "来なかった")),
            row("Past, polite", k("きました", "来ました"), k("きませんでした", "来ませんでした")),
            row("Te-form", k("きて", "来て"), k("こなくて", "来なくて")),
            row("Potential", k("こられる", "来られる"), k("こられない", "来られない")),
            row("Passive", k("こられる", "来られる"), k("こられない", "来られない")),
            row("Causative", k("こさせる", "来させる"), k("こさせない", "来させない")),
            row(
                "Causative Passive",
                k("こさせられる", "来させられる"),
                k("こさせられない", "来させられない")
            ),
            row("Imperative", k("こい", "来い"), k("くるな", "来るな")),
        )
    }


    private fun row(label: String, affirmative: String, negative: String) =
        ConjugationRow(label, affirmative, negative)

    private fun table(vararg rows: ConjugationRow) = ConjugationTable(rows.toList())
}