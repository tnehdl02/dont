package com.example.binancephonescanner

import java.io.Serializable

enum class MarketType { FUTURES, SPOT, BOTH }

enum class TimeFrame(val code: String) {
    M1("1m"), M5("5m"), M15("15m"), H1("1h"), H4("4h"), D1("1d")
}

enum class ConditionType(val title: String) {
    VOL_MULTIPLE("거래량 증가 (배율)"),
    VOL_PERCENT("거래량 증가 (%)"),
    MA_ALIGNMENT("이동평균선 정배열 (SMA/EMA)"),
    MA_CROSS("이동평균선 비교 (크로스)"),
    PRICE_CHANGE("가격 등락률 (%)")
}

data class ScanTask(
    val id: String,
    val name: String,
    val timeFrame: TimeFrame,
    val conditionType: ConditionType,
    val targetValue: Double
) : Serializable
