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
            
            // 1. 智能预处理：不管用户输什么，提取数字并自动匹配 sh/sz
            val rawList = codes.split(",")
            val smartCodes = rawList.mapNotNull { raw ->
                val number = raw.filter { it.isDigit() }
                if (number.length == 6) {
                    // 6开头是上海(sh)，0/3开头是深圳(sz)，4/8是北交所(bj)
                    val prefix = when {
                        number.startsWith("6") -> "sh"
                        number.startsWith("0") || number.startsWith("3") -> "sz"
                        number.startsWith("4") || number.startsWith("8") -> "bj"
                        else -> "sh" // 默认
                    }
                    prefix + number
                } else {
                    null // 忽略格式不对的
                }
            }.joinToString(",")

            if (smartCodes.isEmpty()) return@withContext emptyList()

            try {
                // 2. 发起请求
                val request = Request.Builder()
                    .url("https://hq.sinajs.cn/list=$smartCodes")
                    .header("Referer", "https://finance.sina.com.cn")
                    .build()

                val response = client.newCall(request).execute()
                val bytes = response.body?.bytes() ?: return@withContext emptyList()
                val text = String(bytes, Charset.forName("GBK"))
                
                val lines = text.split(";")

                // 3. 解析数据
                smartCodes.split(",").forEach { code ->
                    val line = lines.find { it.contains("hq_str_$code", ignoreCase = true) }
                    if (line != null && line.contains("\"")) {
                        val content = line.substringAfter("\"").substringBeforeLast("\"")
                        if (content.length > 10) {
                            try {
                                val parts = content.split(",")
                                val name = parts[0]
                                val currentPrice = parts[3].toDouble()
                                val prevClose = parts[2].toDouble()

                                if (currentPrice > 0.0) {
                                    val changePercent = (currentPrice - prevClose) / prevClose * 100
                                    val isUp = changePercent >= 0
                                    
                                    list.add(StockModel(
                                        code = code, // 显示修正后的代码
                                        name = name,
                                        price = String.format("%.2f", currentPrice),
                                        percent = (if (isUp) "+" else "") + String.format("%.2f%%", changePercent),
                                        isUp = isUp
                                    ))
                                }
                            } catch (e: Exception) {
                                // 忽略解析错误的个股
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
