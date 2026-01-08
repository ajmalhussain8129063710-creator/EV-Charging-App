package com.evcharging.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Car3DViewer(
    modelUrl: String,
    carColor: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(carColor) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // RGB Helper
    fun Color.toJsRgba(): String {
        return "[${this.red}, ${this.green}, ${this.blue}, 1.0]"
    }

    LaunchedEffect(selectedColor) {
        webViewRef?.evaluateJavascript(
            """
            try {
                const viewer = document.querySelector('model-viewer');
                if (viewer && viewer.model) {
                    viewer.model.materials.forEach(mat => {
                        mat.pbrMetallicRoughness.setBaseColorFactor(${selectedColor.toJsRgba()});
                    });
                }
            } catch(e) { console.error(e); }
            """.trimIndent(), null
        )
    }

    Box(modifier = modifier) {
        // 3D Scene via WebView (Model Viewer)
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            android.util.Log.d("Car3DViewer", consoleMessage?.message() ?: "")
                            return true
                        }
                    }
                    
                    loadDataWithBaseURL(
                        null, 
                        """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="utf-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1">
                            <script type="module" src="https://ajax.googleapis.com/ajax/libs/model-viewer/3.3.0/model-viewer.min.js"></script>
                            <style>
                                body { margin: 0; background: transparent; overflow: hidden; height: 100vh; width: 100vw; }
                                model-viewer { width: 100%; height: 100%; --poster-color: transparent; }
                                #loading { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); color: #00f0ff; font-family: sans-serif; font-weight: bold; background: rgba(0,0,0,0.5); padding: 10px; border-radius: 8px; }
                            </style>
                        </head>
                        <body>
                            <div id="loading">Loading Car Model...</div>
                            <model-viewer 
                                src="$modelUrl" 
                                camera-controls 
                                auto-rotate 
                                ar
                                shadow-intensity="1"
                                camera-orbit="45deg 55deg 4m"
                                field-of-view="30deg"
                                exposure="1.0"
                                on-load="document.getElementById('loading').style.display = 'none';">
                            </model-viewer>
                            
                            <script>
                                const viewer = document.querySelector('model-viewer');
                                const targetColor = ${selectedColor.toJsRgba()};
                                
                                viewer.addEventListener('load', () => {
                                    console.log('Model Loaded Successfully');
                                    // Apply initial color
                                    applyColor(targetColor);
                                });
                                
                                function applyColor(rgba) {
                                    if(viewer && viewer.model) {
                                        try {
                                            viewer.model.materials.forEach(mat => {
                                                // Target car paint materials specifically if possible, else all
                                                if (mat.name.toLowerCase().includes('paint') || mat.name.toLowerCase().includes('body')) {
                                                    mat.pbrMetallicRoughness.setBaseColorFactor(rgba);
                                                } else {
                                                    // Fallback: try setting base color on everything if generic
                                                     mat.pbrMetallicRoughness.setBaseColorFactor(rgba);
                                                }
                                            });
                                        } catch(e) { console.error('Color set error: ' + e); }
                                    }
                                }

                                viewer.addEventListener('error', (error) => {
                                    console.error('Model Viewer Error: ' + error.detail);
                                    document.getElementById('loading').innerText = 'Error loading model.';
                                    document.getElementById('loading').style.color = 'red';
                                });
                            </script>
                        </body>
                        </html>
                        """.trimIndent(),
                        "text/html",
                        "UTF-8",
                        null
                    )
                }.also { webViewRef = it }
            },
            update = { view ->
                // Update URL if changed (re-load)
                 // For now assumes modelUrl doesn't change often or whole composable recomposes
            },
            modifier = Modifier
                .fillMaxSize()
                .clickable { showColorPicker = !showColorPicker } // WebView consumes clicks, might need overlay
        )
        
        // Touch Overlay to capture clicks specifically for the picker toggle
        // Since WebView consumes touch, we might want a button or rely on area outside model?
        // Actually, let's put an invisible box on top? No, that blocks rotation.
        // We add a "Customize" button or handle clicks via JS interface?
        // For simplicity: Add a small floating "Paint" button.
        
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                .clickable { showColorPicker = !showColorPicker },
            contentAlignment = Alignment.Center
        ) {
            Text("🎨", style = MaterialTheme.typography.bodyLarge)
        }

        // Color Picker Overlay
        if (showColorPicker) {
            ColorPickerWheel(
                selectedColor = selectedColor,
                onColorSelected = { 
                    selectedColor = it
                    onColorChange(it)
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(top = 50.dp, end = 16.dp)
            )
            
            // Car Details Overlay
            GlassCard(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                 Text("EV Model X", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                 Text("Charging: 85%", style = MaterialTheme.typography.bodyMedium, color = com.evcharging.app.ui.theme.NeonGreen)
            }
        }
    }
}

@Composable
fun ColorPickerWheel(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    // Simplified Color Picker (List of nice car colors)
    val colors = listOf(
        Color.Red, Color.Blue, Color.Black, Color.White, Color.Gray, 
        com.evcharging.app.ui.theme.NeonCyan, com.evcharging.app.ui.theme.NeonPurple
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onColorSelected(color) }
                        .then(
                            if (color == selectedColor) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier
                        )
                )
            }
        }
    }
}
