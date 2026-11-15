package net.prototux.aare.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.prototux.aare.R
import net.prototux.aare.car.CarPhoto

/**
 * The "Vehicle photo" control (Customize tab): pick a photo of your car and F-Auto removes its
 * background (StradaOS's edge flood-fill, ported) so it can sit on the dashboard. Shows a live
 * preview of the cut-out and lets you change or remove it. Processing runs off the main thread.
 */
@Composable
internal fun VehiclePhotoCard() {
    val context = LocalContext.current
    var photo by remember { mutableStateOf(CarPhoto.load(context)) }
    var processing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            processing = true
            scope.launch {
                val result = withContext(Dispatchers.Default) { CarPhoto.fromUri(context, uri) }
                if (result != null) { CarPhoto.save(context, result); photo = result }
                processing = false
            }
        }
    }
    fun pick() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.vehicle_photo_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 7f).clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            val p = photo
            when {
                processing -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.vehicle_photo_processing), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                p != null -> Image(p.asImageBitmap(), null, Modifier.fillMaxWidth().padding(10.dp), contentScale = ContentScale.Fit)
                else -> Text(stringResource(R.string.vehicle_photo_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { pick() }, enabled = !processing) {
                Text(stringResource(if (photo != null) R.string.vehicle_photo_change else R.string.vehicle_photo_add))
            }
            if (photo != null) OutlinedButton(onClick = { CarPhoto.delete(context); photo = null }, enabled = !processing) {
                Text(stringResource(R.string.vehicle_photo_remove))
            }
        }
    }
}
