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
                val request = Request.Builder()
                    .url("https://hq.sinajs.cn/list=$codes")
                    .header("Referer", "https://finance.sina.com.cn")
                    .build()

                val response = client.newCall(request).execute()
                val bytes = response.body?.bytes() ?: return@withContext emptyList()
                val text = String(bytes, Charset.forName("GBK")) // 新浪接口编码

                val lines = text.split(";")
                val requestedCodes = codes.split(",")

                requestedCodes.forEach { code ->
                    val cleanCode = code.trim()
                    val line = lines.find { it.contains("hq_str_$cleanCode") }
                    if (line != null && line.contains("\"")) {
                        val content = line.substringAfter("\"").substringBeforeLast("\"")
                        if (content.isNotBlank()) {
                            val parts = content.split(",")
                            if (parts.size > 3) {
                                val name = parts[0]
                                val currentPrice = parts[3].toDouble()
                                val prevClose = parts[2].toDouble()
                                
                                if(prevClose > 0.0) {
                                    val changePercent = (currentPrice - prevClose) / prevClose * 100
                                    val isUp = changePercent >= 0
                                    
                                    list.add(StockModel(
                                        code = cleanCode,
                                        name = name,
                                        price = String.format("%.2f", currentPrice),
                                        percent = (if(isUp) "+" else "") + String.format("%.2f%%", changePercent),
                                        isUp = isUp
                                    ))
                                } else {
                                     list.add(StockModel(cleanCode, name, "停牌", "0.00%", true))
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
