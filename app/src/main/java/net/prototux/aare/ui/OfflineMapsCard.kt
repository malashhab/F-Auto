package net.prototux.aare.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.prototux.aare.R
import net.prototux.aare.Settings
import net.prototux.aare.map.MapDownload
import net.prototux.aare.ui.car.OfflineMaps

/**
 * The "Offline maps" settings page: toggle the offline-map nav option, manage downloaded country
 * maps, and browse download.mapsforge.org (continent → country) to download more. One download runs
 * at a time, resumable, with live progress from [MapDownload.state].
 */
@Composable
internal fun OfflineMapsCard(settings: Settings) {
    val context = LocalContext.current
    val progress by MapDownload.state.collectAsState()
    var offer by remember { mutableStateOf(settings.offlineMapApp) }
    var downloaded by remember { mutableStateOf(OfflineMaps.downloaded(context)) }

    // Browser state.
    var folder by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<MapDownload.Entry>?>(null) }
    var listError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(folder) {
        entries = null; listError = null
        runCatching { MapDownload.list(folder) }
            .onSuccess { entries = it }
            .onFailure { listError = it.message ?: it.toString() }
    }
    // Refresh the on-disk list whenever a download finishes.
    LaunchedEffect(progress.finishedName, progress.running) {
        downloaded = OfflineMaps.downloaded(context)
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        // Offer-as-nav-option toggle.
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.offline_map_offer_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.offline_map_offer_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = offer, onCheckedChange = { offer = it; settings.offlineMapApp = it })
        }

        // Downloaded maps.
        if (downloaded.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.offline_map_downloaded), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            downloaded.forEach { file ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(file.name.removeSuffix(".map").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(MapDownload.mb(file.length()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { file.delete(); downloaded = OfflineMaps.downloaded(context) }) {
                        Icon(Icons.Rounded.Delete, null, Modifier.width(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.offline_map_delete))
                    }
                }
            }
        }

        // Active download.
        if (progress.running) {
            Spacer(Modifier.height(12.dp))
            Column(Modifier.fillMaxWidth()) {
                Text(progress.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { progress.percent / 100f }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text(
                    "${progress.percent}% · ${MapDownload.mb(progress.done)} / ${MapDownload.mb(progress.total)}" +
                        (if (progress.speed > 0) " · ${progress.speed / 1024} KB/s" else "") +
                        (progress.error?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { MapDownload.cancel(delete = false) }) { Text(stringResource(R.string.offline_map_pause)) }
                    OutlinedButton(onClick = { MapDownload.cancel(delete = true) }) { Text(stringResource(R.string.offline_map_cancel)) }
                }
            }
        }

        // Browser.
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (folder.isNotEmpty()) {
                OutlinedButton(onClick = { folder = parentFolder(folder) }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null, Modifier.width(18.dp))
                    Text(stringResource(R.string.offline_map_back))
                }
                Spacer(Modifier.width(10.dp))
            }
            Text(if (folder.isEmpty()) stringResource(R.string.offline_map_browse_hint) else prettyFolder(folder),
                style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))

        when {
            listError != null -> Text(listError!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            entries == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.offline_map_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> entries!!.forEach { e ->
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = e.dir || !progress.running) {
                        if (e.dir) folder = e.path else MapDownload.start(context, e)
                    }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(e.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!e.dir) Text(
                            (if (e.size > 0) MapDownload.mb(e.size) + " · " else "") + e.date,
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (e.dir) Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    else Button(onClick = { MapDownload.start(context, e) }, enabled = !progress.running) { Text(stringResource(R.string.offline_map_download)) }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.offline_map_source), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun parentFolder(folder: String): String {
    val trimmed = folder.trimEnd('/')
    val slash = trimmed.lastIndexOf('/')
    return if (slash < 0) "" else trimmed.substring(0, slash + 1)
}

private fun prettyFolder(folder: String): String = folder.trimEnd('/').substringAfterLast('/')
    .split('-', '_').filter { it.isNotEmpty() }.joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
