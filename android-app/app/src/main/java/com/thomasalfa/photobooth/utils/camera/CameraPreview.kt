package com.thomasalfa.photobooth.utils.camera

import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.jiangdg.ausbc.CameraClient
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraClient: CameraClient?,
    onViewCreated: (IAspectRatio) -> Unit
) {
    AndroidView(
        // Pastikan rasio container UI sesuai dengan request kamera (4:3)
        modifier = modifier.aspectRatio(4f / 3f),
        factory = { context ->
            AspectRatioTextureView(context).apply {
                // --- FITUR MIRRORING ---
                // Membalik view secara horizontal (Kiri jadi Kanan)
                this.scaleX = -1f
                // -----------------------

                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                        onViewCreated(this@apply)
                    }
                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        }
    )
}