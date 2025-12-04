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
        // 缓存数据
        var cachedStocks: List<StockModel> = emptyList()
        var lastUpdate: String = "点击加载"
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
                .background(GlanceTheme.colors.background)
                .padding(12.dp)
        ) {
            // 顶部状态栏
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    "自选行情",
                    style = TextStyle(
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = GlanceTheme.colors.onSurface
                    )
                )
                
                Spacer(modifier = GlanceModifier.defaultWeight())
                
                // 刷新按钮
                Text(
                    text = "↻ $lastUpdate",
                    style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.secondary),
                    modifier = GlanceModifier.clickable(onClick = actionRunCallback<RefreshAction>())
                )
            }

            if (stockList.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "点击刷新 或 去APP设置", 
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
        val upColor = Color(0xFFF53F3F)
        val downColor = Color(0xFF00B42A)
        val displayColor = if (stock.isUp) upColor else downColor

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(GlanceTheme.colors.surface)
                .padding(vertical = 10.dp, horizontal = 12.dp)
                .clickable(onClick = actionRunCallback<RefreshAction>()),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = stock.name,
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = GlanceTheme.colors.onSurface)
                )
                Text(
                    text = stock.code,
                    style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.secondary)
                )
            }
            Text(
                text = stock.price,
                style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ColorProvider(displayColor)),
                modifier = GlanceModifier.padding(end = 12.dp)
            )
            Box(
                modifier = GlanceModifier
                    .background(displayColor.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
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

// 刷新逻辑
class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // 1. 先把UI状态改为“更新中...”，给用户反馈
        StockWidget.lastUpdate = "更新中..."
        StockWidget().updateAll(context)

        // 2. 去后台下载数据
        val codes = Prefs.getSavedCodes(context)
        val data = StockRepository.getStockList(codes)
        
        // 3. 更新数据并显示时间
        if (data.isNotEmpty()) {
            StockWidget.cachedStocks = data
            val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            StockWidget.lastUpdate = timeFormat.format(java.util.Date())
        } else {
            // 如果没网或失败
            StockWidget.lastUpdate = "获取失败"
        }
        
        // 4. 最终刷新界面
        StockWidget().updateAll(context)
    }
}
