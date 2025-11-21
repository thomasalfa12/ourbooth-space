package com.thomasalfa.photobooth.utils.camera

import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.jiangdg.ausbc.CameraClient
import com.jiangdg.ausbc.widget.AspectRatioTextureView
import com.jiangdg.ausbc.widget.IAspectRatio

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier, // Biarkan modifier dari luar bekerja (seperti fillMaxSize)
    cameraClient: CameraClient?,
    onViewCreated: (IAspectRatio) -> Unit
) {
    AndroidView(
        // [FIX] Hapus .aspectRatio(4f/3f) di sini agar tidak konflik dengan parent.
        // AspectRatioTextureView akan otomatis menyesuaikan kontennya (fitting)
        modifier = modifier,
        factory = { context ->
            AspectRatioTextureView(context).apply {
                // --- FITUR MIRRORING ---
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