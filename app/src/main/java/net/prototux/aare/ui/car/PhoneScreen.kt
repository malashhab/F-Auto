package net.prototux.aare.ui.car

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.prototux.aare.R
import net.prototux.aare.car.Calls
import net.prototux.aare.host.CarRuntime
import net.prototux.aare.media.Pill
import net.prototux.aare.media.Placeholder
import net.prototux.aare.ui.theme.LocalCarDimens

/** One number to call: a recent call or a contact. */
private class Callee(val name: String, val number: String, val detail: String, val type: Int = 0)

private enum class PhoneTab(val labelRes: Int) { RECENTS(R.string.phone_tab_recents), CONTACTS(R.string.phone_tab_contacts), KEYPAD(R.string.phone_tab_keypad) }

private val PERMISSIONS = listOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG,
    Manifest.permission.READ_CONTACTS, Manifest.permission.READ_PHONE_STATE)

/**
 * Calls from the car, like Android Auto's phone app: recent calls, favourite contacts and a
 * keypad (parked only). Calls are placed by Android's telephony, so their sound goes through the
 * car's Bluetooth hands-free link as usual.
 *
 * [duskLayout] shows a row of favourite contacts over a two-column recents grid — matching
 * `CarUI/dusk-8in.html`'s Phone screen — instead of the Classic layout's tabs. That mockup drops
 * the keypad tab entirely (favourites cover dialling a known contact); a small keypad button next
 * to "Recents" keeps dialling an arbitrary number reachable without it, while parked.
 */
@Composable
internal fun PhoneScreen(runtime: CarRuntime, duskLayout: Boolean = false, modifier: Modifier) {
    val context = LocalContext.current
    val driving = runtime.ui.drivingRestricted
    if (PERMISSIONS.any { context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }) {
        Placeholder(stringResource(R.string.phone_permissions_placeholder), loading = false,
            icon = Icons.Rounded.Call, modifier = modifier.background(MaterialTheme.colorScheme.background))
        return
    }
    var tab by remember { mutableStateOf(PhoneTab.RECENTS) }
    var reload by remember { mutableIntStateOf(0) }
    // While a call is on, show it with a hang-up button; poll: no listener survives the screen anyway.
    val inCall by produceState(false) {
        while (true) { value = Calls.state(context) != TelephonyManager.CALL_STATE_IDLE; delay(1_500) }
    }
    LaunchedEffect(inCall) { if (!inCall) reload++ }
    val callingTitle = context.getString(R.string.phone_notice_calling_title)
    val call: (Callee) -> Unit = { c ->
        // An empty tel: places no call; it would only show a "Calling" banner with no name.
        if (c.number.isNotBlank()) {
            Calls.place(context, c.number)
            runtime.notice(callingTitle, c.name.ifBlank { c.number })
        }
    }

    if (duskLayout) {
        var showKeypad by remember { mutableStateOf(false) }
        Column(modifier.background(MaterialTheme.colorScheme.background).padding(horizontal = 24.dp, vertical = 16.dp)) {
            if (inCall && Calls.canEnd) InCallCard { Calls.end(context) }
            if (showKeypad && !driving) {
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.phone_tab_keypad), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Pill(stringResource(R.string.phone_tab_recents), selected = false, onClick = { showKeypad = false })
                }
                Box(Modifier.weight(1f).fillMaxWidth()) { Keypad { number -> call(Callee("", number, "")) } }
            } else {
                val contacts by produceState<List<Callee>?>(null, reload) { value = withContext(Dispatchers.IO) { contacts(context) } }
                val recents by produceState<List<Callee>?>(null, reload) { value = withContext(Dispatchers.IO) { recents(context) } }
                DuskFavoritesRow(contacts, call)
                Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.phone_tab_recents), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    if (!driving) {
                        Box(Modifier.size(LocalCarDimens.current.touchTarget).clip(CircleShape).clickable { showKeypad = true }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Dialpad, stringResource(R.string.phone_tab_keypad), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) { CalleeGrid(recents, stringResource(R.string.phone_no_recent_calls), call) }
            }
        }
        return
    }

    Column(modifier.background(MaterialTheme.colorScheme.background).padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PhoneTab.entries.forEach { t ->
                if (t != PhoneTab.KEYPAD || !driving) Pill(stringResource(t.labelRes), selected = tab == t, onClick = { tab = t })
            }
        }
        if (inCall && Calls.canEnd) InCallCard { Calls.end(context) }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (if (driving && tab == PhoneTab.KEYPAD) PhoneTab.RECENTS else tab) {
                PhoneTab.RECENTS -> {
                    val recents by produceState<List<Callee>?>(null, reload) { value = withContext(Dispatchers.IO) { recents(context) } }
                    CalleeList(recents, stringResource(R.string.phone_no_recent_calls), call)
                }
                PhoneTab.CONTACTS -> {
                    val contacts by produceState<List<Callee>?>(null, reload) { value = withContext(Dispatchers.IO) { contacts(context) } }
                    CalleeList(contacts, stringResource(R.string.phone_no_contacts), call)
                }
                PhoneTab.KEYPAD -> Keypad { number -> call(Callee("", number, "")) }
            }
        }
    }
}

/** A row of favourite (starred) contacts as round avatars — the Dusk layout's top section, like `dusk-8in.html`'s Favorites row. */
@Composable
private fun DuskFavoritesRow(contacts: List<Callee>?, onCall: (Callee) -> Unit) {
    if (contacts.isNullOrEmpty()) return
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        contacts.take(8).forEach { c ->
            Column(
                Modifier.width(76.dp).clip(RoundedCornerShape(16.dp)).clickable { onCall(c) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(68.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    Text(c.name.ifBlank { c.number }.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Spacer(Modifier.height(8.dp))
                Text(c.name.ifBlank { c.number }, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Two calls per row, like `dusk-8in.html`'s Recents grid — reuses [CalleeRow] in pairs instead of [CalleeList]'s single column. */
@Composable
private fun CalleeGrid(list: List<Callee>?, empty: String, onCall: (Callee) -> Unit) {
    when {
        list == null -> Placeholder(stringResource(R.string.common_loading), loading = true)
        list.isEmpty() -> Placeholder(empty, loading = false, icon = Icons.Rounded.Person)
        else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(list.chunked(2)) { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    pair.forEach { c -> CalleeRow(c, Modifier.weight(1f), onCall) }
                    if (pair.size < 2) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun InCallCard(onHangUp: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Call, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.width(14.dp))
        Text(stringResource(R.string.phone_call_in_progress), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
        Box(Modifier.size(LocalCarDimens.current.touchTarget).clip(CircleShape).background(Color(0xFFD32F2F)).clickable(onClick = onHangUp),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.CallEnd, stringResource(R.string.phone_action_hang_up), tint = Color.White)
        }
    }
}

@Composable
private fun CalleeList(list: List<Callee>?, empty: String, onCall: (Callee) -> Unit) {
    when {
        list == null -> Placeholder(stringResource(R.string.common_loading), loading = true)
        list.isEmpty() -> Placeholder(empty, loading = false, icon = Icons.Rounded.Person)
        else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(list) { c -> CalleeRow(c, Modifier.fillMaxWidth(), onCall) }
        }
    }
}

@Composable
private fun CalleeRow(c: Callee, modifier: Modifier, onCall: (Callee) -> Unit) {
    Row(
        modifier.heightIn(min = LocalCarDimens.current.touchTarget).clip(RoundedCornerShape(20.dp))
            .clickable { onCall(c) }.padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val icon = when (c.type) {
            CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE -> Icons.AutoMirrored.Rounded.CallMissed
            CallLog.Calls.OUTGOING_TYPE -> Icons.AutoMirrored.Rounded.CallMade
            CallLog.Calls.INCOMING_TYPE -> Icons.AutoMirrored.Rounded.CallReceived
            else -> Icons.Rounded.Person
        }
        val missed = c.type == CallLog.Calls.MISSED_TYPE
        Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (missed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(c.name.ifBlank { c.number }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (missed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            if (c.detail.isNotBlank()) Text(c.detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Icon(Icons.Rounded.Call, stringResource(R.string.common_call), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 12.dp))
    }
}

@Composable
private fun Keypad(onCall: (String) -> Unit) {
    var number by remember { mutableStateOf("") }
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(number.ifEmpty { " " }, style = MaterialTheme.typography.displaySmall, maxLines = 1)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(LocalCarDimens.current.touchTarget).clip(CircleShape).clickable { number = number.dropLast(1) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.Backspace, stringResource(R.string.phone_keypad_delete))
                }
                Box(Modifier.size(LocalCarDimens.current.touchTarget + 8.dp).clip(CircleShape).background(Color(0xFF2E7D32))
                    .clickable(enabled = number.isNotEmpty()) { onCall(number) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Call, stringResource(R.string.common_call), tint = Color.White)
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            keys.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { k ->
                        Box(Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { if (number.length < 20) number += k }, contentAlignment = Alignment.Center) {
                            Text(k, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- call history and contacts

private fun recents(context: Context): List<Callee> = runCatching {
    val out = ArrayList<Callee>()
    val seen = HashSet<String>()
    context.contentResolver.query(CallLog.Calls.CONTENT_URI,
        arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE),
        null, null, "${CallLog.Calls.DATE} DESC")?.use { c ->
        while (c.moveToNext() && out.size < 30) {
            // Withheld and unknown callers have an empty number, not null: nothing to call back.
            val number = c.getString(0)?.takeIf { it.isNotBlank() } ?: continue
            if (!seen.add(number)) continue
            val ago = DateUtils.getRelativeTimeSpanString(c.getLong(3), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
            out += Callee(c.getString(1).orEmpty(), number, ago, c.getInt(2))
        }
    }
    out
}.getOrDefault(emptyList())

/** Favourite (starred) contacts, or all of them if there are none. */
private fun contacts(context: Context): List<Callee> = runCatching {
    fun query(starred: Boolean): List<Callee> {
        val out = ArrayList<Callee>()
        val seen = HashSet<String>()
        context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.LABEL),
            if (starred) "${ContactsContract.CommonDataKinds.Phone.STARRED} = 1" else null, null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC")?.use { c ->
            while (c.moveToNext() && out.size < 200) {
                val name = c.getString(0).orEmpty()
                val number = c.getString(1)?.takeIf { it.isNotBlank() } ?: continue
                if (!seen.add(name + number.filter(Char::isDigit))) continue
                val type = ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.resources, c.getInt(2), c.getString(3)).toString()
                out += Callee(name, number, "$type · $number")
            }
        }
        return out
    }
    query(starred = true).ifEmpty { query(starred = false) }
}.getOrDefault(emptyList())
