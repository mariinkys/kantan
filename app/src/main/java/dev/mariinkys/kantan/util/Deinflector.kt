package dev.mariinkys.kantan.util

object Deinflector {

    data class Candidate(val baseForm: String)

    private val rules = listOf(
        // Ichidan
        r("ない", "る"),
        r("なかった", "る"),
        r("なくて", "る"),
        r("ます", "る"),
        r("ません", "る"),
        r("ました", "る"),
        r("ませんでした", "る"),
        r("られる", "る"),
        r("られない", "る"),
        r("させる", "る"),
        r("させない", "る"),
        r("させられる", "る"),
        r("て", "る"),
        r("た", "る"),

        // Godan -ku (v5k)
        r("かない", "く"), r("きます", "く"), r("きました", "く"),
        r("いて", "く"), r("いた", "く"), r("ける", "く"),
        r("かれる", "く"), r("かせる", "く"),

        // Godan -gu (v5g)
        r("がない", "ぐ"), r("ぎます", "ぐ"), r("ぎました", "ぐ"),
        r("いで", "ぐ"), r("いだ", "ぐ"), r("げる", "ぐ"),

        // Godan -su (v5s)
        r("さない", "す"), r("します", "す"), r("しました", "す"),
        r("して", "す"), r("した", "す"), r("せる", "す"),

        // Godan -tsu (v5t)
        r("たない", "つ"), r("ちます", "つ"), r("ちました", "つ"),
        r("てる", "つ"),

        // Godan -nu (v5n)
        r("なない", "ぬ"), r("にます", "ぬ"), r("ねる", "ぬ"),

        // Godan -bu (v5b)
        r("ばない", "ぶ"), r("びます", "ぶ"), r("びました", "ぶ"),
        r("べる", "ぶ"),

        // Godan -mu (v5m)
        r("まない", "む"), r("みます", "む"), r("みました", "む"),
        r("める", "む"),

        // Godan -ru (v5r)
        r("らない", "る"), r("ります", "る"), r("りました", "る"),
        r("れる", "る"), r("らせる", "る"),

        // Godan -u (v5u)
        r("わない", "う"), r("います", "う"), r("いました", "う"),
        r("える", "う"),

        // Ambiguous shared endings (multiple godan rows)
        r("って", "う"), r("って", "る"), r("って", "つ"),
        r("った", "う"), r("った", "る"), r("った", "つ"),
        r("んで", "ぬ"), r("んで", "ぶ"), r("んで", "む"),
        r("んだ", "ぬ"), r("んだ", "ぶ"), r("んだ", "む"),

        // する compounds
        r("しない", "する"), r("します", "する"),
        r("しました", "する"), r("して", "する"),
        r("した", "する"), r("できる", "する"),
        r("される", "する"), r("させる", "する"),

        // くる
        r("こない", "くる"), r("きます", "くる"),
        r("きました", "くる"), r("きて", "くる"),
        r("きた", "くる"), r("こられる", "くる"),
    )

    private fun r(suffix: String, replacement: String) = Pair(suffix, replacement)

    /** Returns candidate dictionary/base forms for [word], deduplicated. */
    fun deinflect(word: String): List<Candidate> =
        rules
            .filter { (suffix, _) -> word.length > suffix.length && word.endsWith(suffix) }
            .map { (suffix, replacement) -> Candidate(word.dropLast(suffix.length) + replacement) }
            .distinctBy { it.baseForm }
            .filter { it.baseForm.length >= 2 }
}