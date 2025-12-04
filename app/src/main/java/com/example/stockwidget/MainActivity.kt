package com.example.stockwidget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                StockConfigScreen()
            }
        }
    }

    @Composable
    fun StockConfigScreen() {
        var textValue by remember { mutableStateOf(Prefs.getSavedCodes(this)) }
        val scope = rememberCoroutineScope()

        Column(modifier = Modifier.padding(24.dp)) {
            Text("设置股票代码", style = MaterialTheme.typography.headlineMedium)
            Text("格式：sh000001,sz000858 (英文逗号分隔)", style = MaterialTheme.typography.bodySmall)
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = textValue,
                onValueChange = { textValue = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("代码列表") },
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    Prefs.saveCodes(this@MainActivity, textValue)
                    scope.launch {
                        StockWidget().updateAll(this@MainActivity)
                        Toast.makeText(this@MainActivity, "保存成功，请查看桌面组件", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存并刷新组件")
            }
        }
    }
}
