package com.example.stockwidget

import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.Charset

data class StockModel(
    val code: String,
    val name: String,
    val price: String,
    val percent: String,
    val isUp: Boolean
)

object StockRepository {
    private val client = OkHttpClient()

    suspend fun getStockList(codes: String): List<StockModel> {
        return withContext(Dispatchers.IO) {
            val list = mutableListOf<StockModel>()
            try {
                // 1. 发起请求
                val request = Request.Builder()
                    .url("https://hq.sinajs.cn/list=$codes")
                    .header("Referer", "https://finance.sina.com.cn")
                    .build()

                val response = client.newCall(request).execute()
                val bytes = response.body?.bytes() ?: return@withContext emptyList()
                val text = String(bytes, Charset.forName("GBK"))

                val lines = text.split(";")
                val requestedCodes = codes.split(",")

                // 2. 逐个解析 (增加容错，输错一个不影响其他)
                requestedCodes.forEach { rawCode ->
                    val code = rawCode.trim()
                    if (code.isNotEmpty()) {
                        // 模糊匹配，防止用户搞错 sh/sz 大小写
                        val line = lines.find { it.contains("hq_str_$code", ignoreCase = true) }
                        
                        if (line != null && line.contains("\"")) {
                            val content = line.substringAfter("\"").substringBeforeLast("\"")
                            if (content.isNotBlank() && content.length > 10) { // 确保数据有效
                                try {
                                    val parts = content.split(",")
                                    val name = parts[0]
                                    val currentPrice = parts[3].toDouble()
                                    val prevClose = parts[2].toDouble()

                                    // 只有价格有效才添加
                                    if (currentPrice > 0 && prevClose > 0) {
                                        val changePercent = (currentPrice - prevClose) / prevClose * 100
                                        val isUp = changePercent >= 0
                                        
                                        list.add(StockModel(
                                            code = code,
                                            name = name,
                                            price = String.format("%.2f", currentPrice),
                                            percent = (if (isUp) "+" else "") + String.format("%.2f%%", changePercent),
                                            isUp = isUp
                                        ))
                                    }
                                } catch (e: Exception) {
                                    // 单个解析失败，忽略，继续下一个
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list
        }
    }
}
