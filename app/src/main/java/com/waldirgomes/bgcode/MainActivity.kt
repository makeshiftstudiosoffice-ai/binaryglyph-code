package com.waldirgomes.bgcode

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.waldirgomes.bgcode.ui.theme.BGCodeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.Executors

/**
 * Módulo 2: LÓGICA DE SEGURANÇA (Perfis)
 */
enum class UserRole(val label: String) {
    MOTORISTA("Motorista"),
    CLIENTE("Cliente Dono"),
    CURIOSO("Curioso")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BGCodeTheme {
                BGCodeApp()
            }
        }
    }
}

/**
 * Módulo 1: TABELA DE GLIFOS E MAPEAMENTO REVERSO
 * A geração agora é feita na Nuvem (Firebase/AWS) para proteção de IP.
 */
// Mapeamento reverso permanece local para OCR rápido offline
val tabelaBinaryGlyphLocal: Map<Char, String> = mapOf(
    'A' to "♔", 'B' to "♕", 'C' to "♖", 'D' to "♗", 'E' to "♘", 'F' to "♙", 'G' to "♚", 'H' to "♛", 'I' to "♜",
    'J' to "♝", 'K' to "♞", 'L' to "♟", 'M' to "⚡", 'N' to "☀", 'O' to "☁", 'P' to "☂", 'Q' to "☃", 'R' to "☄",
    'S' to "★", 'T' to "☆", 'U' to "☎", 'V' to "☏", 'W' to "✈", 'X' to "✉", 'Y' to "✏", 'Z' to "⚓",
    'a' to "♠", 'b' to "♣", 'c' to "♥", 'd' to "♦", 'e' to "♩", 'f' to "♪", 'g' to "♫", 'h' to "♬", 'i' to "⛏",
    'j' to "⚒", 'k' to "⚔", 'l' to "⚖", 'm' to "⚙", 'n' to "⚗", 'o' to "⚰", 'p' to "⚱", 'q' to "⚲", 'r' to "⚳",
    's' to "⚴", 't' to "⚵", 'u' to "⚶", 'v' to "⚷", 'w' to "⚸", 'x' to "⚛", 'y' to "⚜", 'z' to "⚝",
    '0' to "⚀", '1' to "⚁", '2' to "⚂", '3' to "⚃", '4' to "⚄", '5' to "⚅", '6' to "⚆", '7' to "⚇", '8' to "⚈", '9' to "⚉",
    ' ' to "░", ',' to "⁕", '.' to "✦", ':' to "⁚", '-' to "—", '\n' to "\n"
)

val tabelaReversaBG: Map<String, Char> = tabelaBinaryGlyphLocal.entries.associate { it.value to it.key }

// URL da sua Cloud Function (Placeholder para GitHub)
private const val CLOUD_FUNCTION_URL = "https://seu-projeto.cloudfunctions.net/gerarEtiquetaBG"

fun gerarEtiquetaBGNuvem(texto: String, onResult: (String) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val client = OkHttpClient()
            val json = JSONObject().apply { put("texto", texto) }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            
            val request = Request.Builder()
                .url(CLOUD_FUNCTION_URL)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) { onResult("ERRO NA CONEXÃO") }
                    return@launch
                }
                
                val responseBody = response.body?.string()
                val jsonRes = JSONObject(responseBody ?: "{}")
                val resultado = jsonRes.optString("resultado", "ERRO NA RESPOSTA")
                
                withContext(Dispatchers.Main) {
                    onResult(resultado)
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult("OFFLINE OU ERRO API")
            }
        }
    }
}

fun decodificarGlifosParaTexto(glifos: String): String {
    var resultado = ""
    glifos.forEach { char ->
        val original = tabelaReversaBG[char.toString()]
        if (original != null) resultado += original
    }
    return resultado
}

fun decodificarEtiquetaBG(role: UserRole, originalText: String): String {
    return when (role) {
        UserRole.CLIENTE -> originalText
        UserRole.MOTORISTA -> {
            val regex = "Endereço: ([^,]+)".toRegex()
            val match = regex.find(originalText)
            match?.groupValues?.get(1) ?: "Endereço não identificado nos dados brutos."
        }
        UserRole.CURIOSO -> "MERCADO LIVRE: O melhor está chegando!"
    }
}

/**
 * Módulo 3: INTERFACE VISUAL E NAVEGAÇÃO
 */
@Composable
fun BGCodeApp() {
    var showCamera by remember { mutableStateOf(false) }
    var currentRole by remember { mutableStateOf(UserRole.MOTORISTA) }
    var rawInput by remember { mutableStateOf("Nome: [NOME], CPF: 000.000.000-00, Endereço: [RUA/NÚMERO], Item: [PRODUTO]") }
    var glyphOutput by remember { mutableStateOf("") }
    var scanResult by remember { mutableStateOf("") }

    if (showCamera) {
        CameraScreen(
            role = currentRole,
            onClose = { showCamera = false },
            onResult = { result ->
                scanResult = result
                showCamera = false
            }
        )
    } else {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            MainScreen(
                modifier = Modifier.padding(innerPadding),
                rawInput = rawInput,
                onRawInputChange = { rawInput = it },
                glyphOutput = glyphOutput,
                onGerarClick = { 
                    gerarEtiquetaBGNuvem(rawInput) { resultado ->
                        glyphOutput = resultado
                    }
                },
                currentRole = currentRole,
                onRoleChange = { currentRole = it },
                scanResult = scanResult,
                onScanClick = { showCamera = true },
                onClear = {
                    rawInput = ""
                    glyphOutput = ""
                    scanResult = ""
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    modifier: Modifier,
    rawInput: String,
    onRawInputChange: (String) -> Unit,
    glyphOutput: String,
    onGerarClick: () -> Unit,
    currentRole: UserRole,
    onRoleChange: (UserRole) -> Unit,
    scanResult: String,
    onScanClick: () -> Unit,
    onClear: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("BinaryGlyph Code", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

        Text("Perfil de Acesso:", style = MaterialTheme.typography.titleMedium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            UserRole.entries.forEach { role ->
                FilterChip(selected = currentRole == role, onClick = { onRoleChange(role) }, label = { Text(role.label) })
            }
        }

        OutlinedTextField(value = rawInput, onValueChange = onRawInputChange, label = { Text("Dados do Pacote") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

        Button(onClick = onGerarClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Text("GERAR BINARYGLYPH")
        }

        if (glyphOutput.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.DarkGray)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = glyphOutput,
                        modifier = Modifier.padding(16.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = Color.Green,
                        textAlign = TextAlign.Center
                    )
                    
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    Button(
                        onClick = {
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(glyphOutput))
                            Toast.makeText(context, "Glifos copiados!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("COPIAR GLIFOS", fontSize = 10.sp)
                    }
                }
            }
        }

        Button(onClick = onScanClick, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary), shape = MaterialTheme.shapes.medium) {
            Text("ESCANEAR COM CÂMERA (IA)")
        }

        if (scanResult.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Resultado Decodificado (${currentRole.label}):", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = scanResult, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.combinedClickable(
                onClick = { Toast.makeText(context, "Segure para apagar", Toast.LENGTH_SHORT).show() },
                onLongClick = { onClear(); Toast.makeText(context, "Dados removidos!", Toast.LENGTH_SHORT).show() }
            ),
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text("APAGAR DADOS (LONG PRESS)", modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    recognizer: com.google.mlkit.vision.text.TextRecognizer,
    role: UserRole,
    onResult: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val detectedText = visionText.text
                if (detectedText.isNotEmpty()) {
                    val rawText = decodificarGlifosParaTexto(detectedText)
                    if (rawText.isNotEmpty()) {
                        val finalResult = decodificarEtiquetaBG(role, rawText)
                        onResult(finalResult)
                    }
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}

/**
 * Módulo CÂMERA (CameraX + ML Kit)
 */
@Composable
fun CameraScreen(role: UserRole, onClose: () -> Unit, onResult: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Animação da linha de scanner
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLine"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    processImageProxy(imageProxy, recognizer, role, onResult)
                                }
                            }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                        } catch (e: Exception) {
                            Log.e("CameraX", "Erro ao iniciar câmera", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // UI Overlay (Lens Style)
            Canvas(modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            ) {
                val boxSize = size.width * 0.75f
                val left = (size.width - boxSize) / 2
                val top = (size.height - boxSize) / 2
                val rect = Size(boxSize, boxSize)
                
                // 1. Fundo escurecido com recorte central
                drawRect(color = Color.Black.copy(alpha = 0.6f))
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = rect,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    blendMode = BlendMode.Clear
                )

                // 2. Moldura Neon Brilhante (Ciano)
                val neonColor = Color.Cyan
                drawRoundRect(
                    color = neonColor,
                    topLeft = Offset(left, top),
                    size = rect,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx())
                )
                
                // Cantos reforçados para dar estilo "Lens"
                val lineLen = 40.dp.toPx()
                val strokeWidth = 6.dp.toPx()
                // Top Left
                drawLine(neonColor, Offset(left, top), Offset(left + lineLen, top), strokeWidth)
                drawLine(neonColor, Offset(left, top), Offset(left, top + lineLen), strokeWidth)
                // Top Right
                drawLine(neonColor, Offset(left + boxSize, top), Offset(left + boxSize - lineLen, top), strokeWidth)
                drawLine(neonColor, Offset(left + boxSize, top), Offset(left + boxSize, top + lineLen), strokeWidth)
                // Bottom Left
                drawLine(neonColor, Offset(left, top + boxSize), Offset(left + lineLen, top + boxSize), strokeWidth)
                drawLine(neonColor, Offset(left, top + boxSize), Offset(left, top + boxSize - lineLen), strokeWidth)
                // Bottom Right
                drawLine(neonColor, Offset(left + boxSize, top + boxSize), Offset(left + boxSize - lineLen, top + boxSize), strokeWidth)
                drawLine(neonColor, Offset(left + boxSize, top + boxSize), Offset(left + boxSize, top + boxSize - lineLen), strokeWidth)

                // 3. Linha de Varredura (Scanning Animation)
                val scanY = top + (boxSize * scanLineProgress)
                drawLine(
                    color = neonColor.copy(alpha = 0.7f),
                    start = Offset(left + 10.dp.toPx(), scanY),
                    end = Offset(left + boxSize - 10.dp.toPx(), scanY),
                    strokeWidth = 2.dp.toPx()
                )
            }
            
            Column(
                modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RECONHECENDO BINARYGLYPHS",
                    color = Color.Cyan,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "Mantenha a etiqueta dentro da moldura",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
        }

        // Botão Fechar
        IconButton(
            onClick = onClose,
            modifier = Modifier.padding(top = 48.dp, start = 16.dp).align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}
