package dev.mariinkys.kantan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class KanaRow(
    val group: String,
    val chars: List<String?>
)

private val HIRAGANA_ROWS = listOf(
    KanaRow("–", listOf("あ", "い", "う", "え", "お")),
    KanaRow("k", listOf("か", "き", "く", "け", "こ")),
    KanaRow("s", listOf("さ", "し", "す", "せ", "そ")),
    KanaRow("t", listOf("た", "ち", "つ", "て", "と")),
    KanaRow("n", listOf("な", "に", "ぬ", "ね", "の")),
    KanaRow("h", listOf("は", "ひ", "ふ", "へ", "ほ")),
    KanaRow("m", listOf("ま", "み", "む", "め", "も")),
    KanaRow("y", listOf("や", null, "ゆ", null, "よ")),
    KanaRow("r", listOf("ら", "り", "る", "れ", "ろ")),
    KanaRow("w", listOf("わ", null, null, null, "を")),
    KanaRow("n", listOf("ん", null, null, null, null)),
    KanaRow("g", listOf("が", "ぎ", "ぐ", "げ", "ご")),
    KanaRow("z", listOf("ざ", "じ", "ず", "ぜ", "ぞ")),
    KanaRow("d", listOf("だ", "ぢ", "づ", "で", "ど")),
    KanaRow("b", listOf("ば", "び", "ぶ", "べ", "ぼ")),
    KanaRow("p", listOf("ぱ", "ぴ", "ぷ", "ぺ", "ぽ")),
    KanaRow("ky", listOf("きゃ", null, "きゅ", null, "きょ")),
    KanaRow("sh", listOf("しゃ", null, "しゅ", null, "しょ")),
    KanaRow("ch", listOf("ちゃ", null, "ちゅ", null, "ちょ")),
    KanaRow("ny", listOf("にゃ", null, "にゅ", null, "にょ")),
    KanaRow("hy", listOf("ひゃ", null, "ひゅ", null, "ひょ")),
    KanaRow("my", listOf("みゃ", null, "みゅ", null, "みょ")),
    KanaRow("ry", listOf("りゃ", null, "りゅ", null, "りょ")),
    KanaRow("gy", listOf("ぎゃ", null, "ぎゅ", null, "ぎょ")),
    KanaRow("j", listOf("じゃ", null, "じゅ", null, "じょ")),
    KanaRow("by", listOf("びゃ", null, "びゅ", null, "びょ")),
    KanaRow("py", listOf("ぴゃ", null, "ぴゅ", null, "ぴょ")),
)

private val KATAKANA_ROWS = listOf(
    KanaRow("–", listOf("ア", "イ", "ウ", "エ", "オ")),
    KanaRow("k", listOf("カ", "キ", "ク", "ケ", "コ")),
    KanaRow("s", listOf("サ", "シ", "ス", "セ", "ソ")),
    KanaRow("t", listOf("タ", "チ", "ツ", "テ", "ト")),
    KanaRow("n", listOf("ナ", "ニ", "ヌ", "ネ", "ノ")),
    KanaRow("h", listOf("ハ", "ヒ", "フ", "ヘ", "ホ")),
    KanaRow("m", listOf("マ", "ミ", "ム", "メ", "モ")),
    KanaRow("y", listOf("ヤ", null, "ユ", null, "ヨ")),
    KanaRow("r", listOf("ラ", "リ", "ル", "レ", "ロ")),
    KanaRow("w", listOf("ワ", null, null, null, "ヲ")),
    KanaRow("n", listOf("ン", null, null, null, null)),
    KanaRow("g", listOf("ガ", "ギ", "グ", "ゲ", "ゴ")),
    KanaRow("z", listOf("ザ", "ジ", "ズ", "ゼ", "ゾ")),
    KanaRow("d", listOf("ダ", "ヂ", "ヅ", "デ", "ド")),
    KanaRow("b", listOf("バ", "ビ", "ブ", "ベ", "ボ")),
    KanaRow("p", listOf("パ", "ピ", "プ", "ペ", "ポ")),
    KanaRow("ky", listOf("キャ", null, "キュ", null, "キョ")),
    KanaRow("sh", listOf("シャ", null, "シュ", null, "ショ")),
    KanaRow("ch", listOf("チャ", null, "チュ", null, "チョ")),
    KanaRow("ny", listOf("ニャ", null, "ニュ", null, "ニョ")),
    KanaRow("hy", listOf("ヒャ", null, "ヒュ", null, "ヒョ")),
    KanaRow("my", listOf("ミャ", null, "ミュ", null, "ミョ")),
    KanaRow("ry", listOf("リャ", null, "リュ", null, "リョ")),
    KanaRow("gy", listOf("ギャ", null, "ギュ", null, "ギョ")),
    KanaRow("j", listOf("ジャ", null, "ジュ", null, "ジョ")),
    KanaRow("by", listOf("ビャ", null, "ビュ", null, "ビョ")),
    KanaRow("py", listOf("ピャ", null, "ピュ", null, "ピョ")),
)

private val VOWEL_HEADERS = listOf("a", "i", "u", "e", "o")

private val SECTION_BREAKS = setOf(
    "g",   // start of dakuten block
    "ky",  // start of combination block
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KanaTablesScreen(
    onMenuClick: () -> Unit,
    modifier: Modifier
) {
    val tabs = listOf("Hiragana", "Katakana")
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Open Menu")
            }
            Text("Kana Tables", style = MaterialTheme.typography.titleLarge)
        }

        HorizontalDivider()

        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = pagerState.currentPage == i,
                    onClick = { scope.launch { pagerState.animateScrollToPage(i) } },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top
        ) { page ->
            KanaTable(rows = if (page == 0) HIRAGANA_ROWS else KATAKANA_ROWS)
        }
    }
}

@Composable
private fun KanaTable(rows: List<KanaRow>) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val headerBg = MaterialTheme.colorScheme.surfaceVariant
    val altBg = MaterialTheme.colorScheme.surface
    val sectionBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val cellHeight = 52.dp
    val romajiWidth = 36.dp

    SelectionContainer {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .size(width = romajiWidth, height = cellHeight)
                            .background(headerBg)
                            .border(0.5.dp, borderColor)
                    )
                    VOWEL_HEADERS.forEach { vowel ->
                        Box(
                            Modifier
                                .weight(1f)
                                .height(cellHeight)
                                .background(headerBg)
                                .border(0.5.dp, borderColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                vowel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            rows.forEachIndexed { index, kanaRow ->
                item(key = "${kanaRow.group}_$index") {
                    if (kanaRow.group in SECTION_BREAKS) {
                        HorizontalDivider(
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    val rowBg = if (index % 2 == 0) altBg else sectionBg

                    Row(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier
                                .size(width = romajiWidth, height = cellHeight)
                                .background(headerBg)
                                .border(0.5.dp, borderColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                kanaRow.group,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        kanaRow.chars.forEach { kana ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(cellHeight)
                                    .background(rowBg)
                                    .border(0.5.dp, borderColor),
                                contentAlignment = Alignment.Center
                            ) {
                                if (kana != null) {
                                    Text(
                                        kana,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Normal,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}