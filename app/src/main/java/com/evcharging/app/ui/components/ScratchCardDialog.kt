package com.evcharging.app.ui.components

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ScratchCardDialog(
    amount: Double,
    onDismiss: () -> Unit,
    onClaim: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                
                // 1. Result Layer (Hidden underneath)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (amount > 0) {
                         Text(
                            text = "Congratulations!",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "You won",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "₹${amount.toInt()}",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Better Luck Next Time!",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = onClaim) {
                        Text("Claim & Close")
                    }
                }

                // 2. Scratch Layer (Canvas)
                var currentPath by remember { mutableStateOf(Path()) }
                // We'll use a hacky way to "erase". 
                // Jetpack Compose Canvas doesn't support easy PorterDuff Clear on a single layer without View Interop easily.
                // Simpler approach: Grey overlay that we "draw transparent lines" on? 
                // Actually, standard Canvas 'BlendMode.Clear' works if we use a Layer.
                
                val scratchState = remember { mutableStateOf(0f) } // 0 to 100% scratched

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .pointerInteropFilter {
                            when (it.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    currentPath.moveTo(it.x, it.y)
                                    true
                                }
                                MotionEvent.ACTION_MOVE -> {
                                    currentPath.lineTo(it.x, it.y)
                                    scratchState.value += 0.5f // rough approximation
                                    true
                                }
                                else -> false
                            }
                        }
                        .graphicsLayer {
                            // This unlocks BlendMode.Clear
                            alpha = 0.99f 
                        }
                ) {
                    // Draw the Scratch Card Cover
                    drawRect(
                        color = Color.Gray,
                        size = size
                    )
                    
                    // "Erase" path
                    drawPath(
                        path = currentPath,
                        color = Color.Transparent,
                        style = Stroke(width = 60.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                        blendMode = BlendMode.Clear
                    )
                }
                
                // Hint Text on top of Scratch Layer (if not scratched much)
                if (scratchState.value < 10f) {
                    Text("Scratch Here!", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
