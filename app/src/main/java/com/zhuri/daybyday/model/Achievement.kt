package com.zhuri.daybyday.model

import androidx.compose.runtime.Immutable

/** 奖杯材质：铜 / 银 / 金 / 玉 / 铂 */
enum class TrophyTier { BRONZE, SILVER, GOLD, JADE, PLATINUM }

@Immutable
data class Achievement(
    val id: String,
    val days: Int,
    val name: String,
    val motto: String,
    val tier: TrophyTier
)

/**
 * 十座奖杯，按连续完成天数解锁。
 * id 一旦发布就不要改，它写进 JSON 的 achievements 数组里。
 */
val AchievementCatalog: List<Achievement> = listOf(
    Achievement("d1", 1, "启程", "第一天，先迈出去", TrophyTier.BRONZE),
    Achievement("d7", 7, "七日之约", "一周没有断，习惯开始长出来", TrophyTier.BRONZE),
    Achievement("d14", 14, "习惯成形", "两周之后，它不再靠意志力", TrophyTier.SILVER),
    Achievement("d30", 30, "月满", "整整一个月，你把它做成了日常", TrophyTier.SILVER),
    Achievement("d60", 60, "双月同辉", "六十天，足够改变一个人的节奏", TrophyTier.GOLD),
    Achievement("d100", 100, "百日成金", "一百天，已经不是坚持，是生活", TrophyTier.GOLD),
    Achievement("d180", 180, "半载不辍", "半年风雨，你都在", TrophyTier.JADE),
    Achievement("d365", 365, "岁岁常青", "一整年，四季都替你作证", TrophyTier.JADE),
    Achievement("d500", 500, "五百日", "五百次选择，才走到这里", TrophyTier.PLATINUM),
    Achievement("d1000", 1000, "千日之约", "千日之后，你早已不是当初的你", TrophyTier.PLATINUM)
)
