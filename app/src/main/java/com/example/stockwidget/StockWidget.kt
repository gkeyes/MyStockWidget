package com.example.stockwidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.* import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback

class StockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StockWidget()
}

class StockWidget : GlanceAppWidget() {

    companion object {
        var cachedStocks: List<StockModel> = emptyList()
        var lastUpdate: String = "点击刷新"
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    @Composable
    fun WidgetContent() {
        val stockList = cachedStocks
        
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFFF2F3F5))
                .padding(12.dp)
        ) {
            // 【核心修复点】
            // 之前的错误是因为用了 horizontalArrangement（小部件不支持）。
            // 现在改为：用 Spacer(Modifier.defaultWeight()) 来自动撑开左右两边的文字。
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    "自选行情",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Color.Black))
                )
                
                // 这是一个“弹簧”，会自动把右边的内容顶到最右边
                Spacer(modifier = GlanceModifier.defaultWeight())
                
                Text(
                    text = "↻ $lastUpdate",
                    style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color.Gray)),
                    modifier = GlanceModifier.clickable(onClick = actionRunCallback<RefreshAction>())
                )
            }

            if (stockList.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "点击空白处刷新", 
                        style = TextStyle(color = ColorProvider(Color.Gray)),
                        modifier = GlanceModifier.clickable(onClick = actionRunCallback<RefreshAction>())
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(stockList) { stock ->
                        StockItemRow(stock)
                        Spacer(modifier = GlanceModifier.height(8.dp))
                    }
                }
            }
        }
    }

    @Composable
    fun StockItemRow(stock: StockModel) {
        val upColor = Color(0xFFF53F3F)
        val downColor = Color(0xFF00B42A)
        val displayColor = if (stock.isUp) upColor else downColor

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(10.dp)
                .clickable(onClick = actionRunCallback<RefreshAction>()),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = stock.name,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ColorProvider(Color.Black))
                )
            }
            Text(
                text = stock.price,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorProvider(displayColor)),
                modifier = GlanceModifier.padding(end = 12.dp)
            )
            Box(
                modifier = GlanceModifier
                    .background(displayColor.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stock.percent,
                    style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorProvider(displayColor))
                )
            }
        }
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val codes = Prefs.getSavedCodes(context)
        val data = StockRepository.getStockList(codes)
        
        if (data.isNotEmpty()) {
            StockWidget.cachedStocks = data
            val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            StockWidget.lastUpdate = timeFormat.format(java.util.Date())
        }
        StockWidget().updateAll(context)
    }
}
