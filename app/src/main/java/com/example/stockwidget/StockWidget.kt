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
            // 使用 GlanceTheme 自动适配深色模式
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    @Composable
    fun WidgetContent() {
        val stockList = cachedStocks
        
        // 动态背景色：浅色模式下是浅灰，深色模式下是深灰
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .padding(12.dp)
        ) {
            // 顶部栏
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    "自选行情",
                    style = TextStyle(
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = GlanceTheme.colors.onSurface // 自动变黑/白
                    )
                )
                
                Spacer(modifier = GlanceModifier.defaultWeight())
                
                Text(
                    text = "↻ $lastUpdate",
                    style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.secondary),
                    modifier = GlanceModifier.clickable(onClick = actionRunCallback<RefreshAction>())
                )
            }

            if (stockList.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "点击刷新或去APP设置", 
                        style = TextStyle(color = GlanceTheme.colors.onSurface),
                        modifier = GlanceModifier.clickable(onClick = actionRunCallback<RefreshAction>())
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(stockList) { stock ->
                        StockItemRow(stock)
                        Spacer(modifier = GlanceModifier.height(6.dp))
                    }
                }
            }
        }
    }

    @Composable
    fun StockItemRow(stock: StockModel) {
        val upColor = Color(0xFFF53F3F) // 红
        val downColor = Color(0xFF00B42A) // 绿
        val displayColor = if (stock.isUp) upColor else downColor

        // 卡片行
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.background) // 卡片背景自动变色
                .padding(vertical = 10.dp, horizontal = 12.dp)
                .clickable(onClick = actionRunCallback<RefreshAction>()),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            // 股票名称
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = stock.name,
                    style = TextStyle(
                        fontSize = 13.sp, 
                        fontWeight = FontWeight.Medium, 
                        color = GlanceTheme.colors.onSurface
                    )
                )
                // 显示代码，看起来更专业
                Text(
                    text = stock.code,
                    style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.secondary)
                )
            }
            
            // 价格
            Text(
                text = stock.price,
                style = TextStyle(
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = ColorProvider(displayColor)
                ),
                modifier = GlanceModifier.padding(end = 12.dp)
            )
            
            // 涨跌幅块
            Box(
                modifier = GlanceModifier
                    .background(displayColor.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stock.percent,
                    style = TextStyle(
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = ColorProvider(displayColor)
                    )
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
