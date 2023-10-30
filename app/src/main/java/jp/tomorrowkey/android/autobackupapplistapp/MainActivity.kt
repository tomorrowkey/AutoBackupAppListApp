package jp.tomorrowkey.android.autobackupapplistapp

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.ImageRequest
import coil.request.Options
import coil.size.Dimension
import coil.size.pxOrElse
import jp.tomorrowkey.android.autobackupapplistapp.ui.theme.AutoBackupAppListAppTheme

class MainActivity : ComponentActivity() {
    @SuppressLint("QueryPermissionsNeeded")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AutoBackupAppListAppTheme(darkTheme = false) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val apps = remember { getApps() }
                    val countOfBackupAllowedApps = apps.count { it.isBackupAllowed }
                    val imageLoader =   ImageLoader.Builder(context = LocalContext.current)
                        .components {
                            add(AppIconFetcher.Factory())
                        }
                        .build()

                    LazyColumn {
                        item {
                            Column {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Count of apps with AutoBackups allowed",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        text = "$countOfBackupAllowedApps / ${apps.count()} Apps (${(countOfBackupAllowedApps.toFloat() / apps.count() * 100).roundTo(2)}%)",
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                }
                                Divider()
                            }
                        }

                        items(apps) { app ->
                            val backgroundColor = if (app.isBackupAllowed) Color.White else Color(0xEE, 0xEE, 0xEE)
                            val textColor = if (app.isBackupAllowed) Color(0x33, 0x33, 0x33) else Color(0x88, 0x88, 0x88)

                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(color = backgroundColor)
                                    .padding(all = 8.dp)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current).data(app).size(64).build(),
                                    imageLoader = imageLoader,
                                    contentDescription = null,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(
                                    modifier = Modifier.weight(weight = 1f, fill = true),
                                ) {
                                    Text(
                                        text = app.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = textColor,
                                    )
                                    Text(
                                        text = app.packageName,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodySmall,
                                        overflow = TextOverflow.Ellipsis,
                                        color = textColor,
                                    )

                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (app.isBackupAllowed) "Allow" else "Disallow",
                                    color = textColor,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getApps(): List<App> {
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .mapNotNull { applicationInfo -> App(packageManager, applicationInfo).takeIf { it.isInstalledFromGooglePlay } }
            .filter { it.packageName != packageName }
            .sortedBy { it.name }
    }
}

class App(
    private val packageManager: PackageManager,
    private val applicationInfo: ApplicationInfo,
) {
    val packageName: String = applicationInfo.packageName

    val name: String =
        applicationInfo.labelRes.takeIf { it != 0 }?.let { packageManager.getText(packageName, it, applicationInfo) }?.toString() ?: applicationInfo.nonLocalizedLabel?.toString() ?: "null"

    private val installerPackageName: String? = packageManager.getInstallerPackageName(packageName)

    val isInstalledFromGooglePlay: Boolean = installerPackageName == "com.android.vending"

    val isBackupAllowed: Boolean = applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0

    val applicationIcon: Drawable = packageManager.getApplicationIcon(packageName)
}

fun Float.roundTo(n: Int): Float = "%.${n}f".format(this).toFloat()

class AppIconFetcher(
    private val app: App,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val originalDrawable = app.applicationIcon
        val sampledDrawable = originalDrawable.toBitmap(
            width = options.size.width.pxOrElse { originalDrawable.intrinsicWidth },
            height = options.size.height.pxOrElse { originalDrawable.intrinsicHeight },
            Bitmap.Config.ARGB_8888
        ).toDrawable(options.context.resources)
        return DrawableResult(
            drawable = sampledDrawable,
            isSampled = true,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory : Fetcher.Factory<App> {
        override fun create(data: App, options: Options, imageLoader: ImageLoader): Fetcher {
            return AppIconFetcher(data, options)
        }
    }
}
