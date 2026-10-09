package com.example.paseape

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.paseape.ui.theme.BackgroundOffWhite
import com.example.paseape.ui.theme.CardSurface
import com.example.paseape.ui.theme.PrimaryDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SplashScreen(onTimeout = {
                    startActivity(Intent(this@MainActivity, AccountActivity::class.java))
                    finish()
                })
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val steps = 20
        for (i in 1..steps) {
            delay(100)
            progress = i / steps.toFloat()
        }
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundOffWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.perritologoapp),
                contentDescription = "Logo PaseaPE",
                modifier = Modifier.size(250.dp) // Ajustado a un tamaño equilibrado
            )
            Text(
                text = "Paseos seguros en Lima",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(180.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PrimaryDark,
                trackColor = CardSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
    }
}