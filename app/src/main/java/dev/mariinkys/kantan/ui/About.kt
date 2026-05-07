package dev.mariinkys.kantan.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import dev.mariinkys.kantan.R

@Composable
fun AboutScreen(
    onMenuClick: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val versionName = remember {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
    }

    @Composable
    fun LinkedText(fullText: String, linkText: String, url: String) {
        val annotatedString = buildAnnotatedString {
            val startIndex = fullText.indexOf(linkText)
            val endIndex = startIndex + linkText.length
            append(fullText.substring(0, startIndex))

            withLink(
                LinkAnnotation.Url(
                    url = url,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ),
                    linkInteractionListener = {
                        uriHandler.openUri(url)
                    }
                )
            ) {
                append(linkText)
            }

            append(fullText.substring(endIndex))
        }

        Text(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }

    SelectionContainer {
        Column(
            modifier = modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                }
                Text("About", style = MaterialTheme.typography.titleLarge)
            }

            HorizontalDivider()

            Icon(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier.size(124.dp),
                tint = Color.Unspecified
            )
            Text("Kantan Japanese Dictionary", style = MaterialTheme.typography.headlineSmall)

            TextButton(onClick = {
                uriHandler.openUri("https://github.com/mariinkys/Kantan/releases")
            }) {
                Text(
                    text = "Version $versionName",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                LinkedText(
                    fullText = "The dictionary words and kanji come from the JMDict and KANJIDIC files owned by the Electronic Dictionary Research and Development Group (www.edrdg.org), and are used in conformance with the Group's license.",
                    linkText = "www.edrdg.org",
                    url = "http://www.edrdg.org"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "The example sentences come from the projects Tatoeba and Tanaka Corpus.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinkedText(
                    fullText = "The Kanji strokes information are from KanjiVG provided by Ulrich Apel at kanjivg.tagaini.net",
                    linkText = "kanjivg.tagaini.net",
                    url = "http://kanjivg.tagaini.net"
                )

                Spacer(modifier = Modifier.height(16.dp))

                LinkedText(
                    fullText = "Kantan specifically uses the JMDict and KANJIDIC files provided by Yomitan (https://github.com/yomidevs/jmdict-yomitan)",
                    linkText = "https://github.com/yomidevs/jmdict-yomitan",
                    url = "https://github.com/yomidevs/jmdict-yomitan"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = {
                uriHandler.openUri("https://github.com/mariinkys")
            }) {
                Text(
                    text = "mariinkys",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            TextButton(onClick = {
                uriHandler.openUri("https://github.com/mariinkys/kantan/issues")
            }) {
                Text(
                    text = "Issues / Suggestions",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
