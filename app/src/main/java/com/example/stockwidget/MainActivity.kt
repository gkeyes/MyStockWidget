package com.example.stockwidget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

        // 澎湃OS风格背景色
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F3F5)) // 浅灰背景
                .padding(16.dp)
        ) {
            // 标题栏
            Text(
                "股票小部件设置",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                modifier = Modifier.padding(vertical = 24.dp, horizontal = 8.dp)
            )

            // 白色卡片容器
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        "输入股票代码",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "规则：sh代表上海，sz代表深圳，用英文逗号隔开。\n例如：sh000001,sh600519,sz002594",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 输入框
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("例如: sh000001,sz000858") },
                        minLines = 3
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 大圆角蓝色按钮
                    Button(
                        onClick = {
                            Prefs.saveCodes(this@MainActivity, textValue)
                            scope.launch {
                                // 强制刷新
                                StockWidget().updateAll(this@MainActivity)
                                Toast.makeText(this@MainActivity, "保存成功！桌面组件正在刷新...", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3575F0)), // 小米蓝
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text("保存并刷新", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 底部提示
            Text(
                "提示：如果桌面组件未刷新，请手动点击组件右上角。",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
