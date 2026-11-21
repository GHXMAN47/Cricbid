package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CareerAchievementId
import com.example.data.model.CareerSaveState
import com.example.data.model.CareerSkillCategory
import com.example.data.model.CareerSkillStat
import com.example.ui.theme.AuctionGold
import com.example.ui.theme.BoundarySky
import com.example.ui.theme.BroadcastMuted
import com.example.ui.theme.BroadcastSilver
import com.example.ui.theme.BroadcastWhite
import com.example.ui.theme.DisplayFontFamily
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.NavyMidnight
import com.example.ui.theme.NavyOutline
import com.example.ui.theme.NavySurface
import com.example.ui.theme.NavySurfaceHigh
import com.example.ui.theme.OnEmeraldDark
import com.example.ui.theme.OnGoldDark
import com.example.ui.theme.TurfEmerald
import com.example.ui.theme.WicketCrimson

@Composable
internal fun CareerObjectiveAndSelectionCard(
    career: CareerSaveState,
    onRunSelectionTrial: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selColor = when {
        career.isDropped -> WicketCrimson
        career.selectionMeter >= 76 -> TurfEmerald
        career.selectionMeter >= 45 -> BoundarySky
        else -> WicketCrimson
    }
    val obj = career.currentObjective

    Surface(
        color = NavySurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.2.dp, selColor.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("career_objective_selection_card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Selection Status Header & Meter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TEAM SELECTION METER",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = BroadcastSilver
                )
                Surface(
                    color = selColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, selColor.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "${career.selectionStatusLabel} • ${career.selectionMeter}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = MonoFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            color = selColor
                        ),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { (career.selectionMeter / 100f).coerceIn(0f, 1f) },
                color = selColor,
                trackColor = NavySurfaceHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            if (career.isDropped) {
                Surface(
                    color = WicketCrimson.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, WicketCrimson.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = WicketCrimson, modifier = Modifier.size(18.dp))
                            Text(
                                text = "DROPPED FROM THE PLAYING XI",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = WicketCrimson
                            )
                        }
                        Text(
                            text = "Recent poor form cost your spot in the XI. Complete a Selection Net Trial to prove your fitness and earn an immediate recall.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BroadcastSilver
                        )
                        Button(
                            onClick = onRunSelectionTrial,
                            colors = ButtonDefaults.buttonColors(containerColor = AuctionGold, contentColor = OnGoldDark),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("career_selection_trial_button")
                        ) {
                            Text("Complete Selection Net Trial (Reclaim XI)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (obj != null) {
                Surface(
                    color = NavySurfaceHigh,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Flag,
                            contentDescription = "Match Objective",
                            tint = AuctionGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "MATCH OBJECTIVE • ${obj.title}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = AuctionGold
                            )
                            Text(
                                text = obj.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = BroadcastSilver
                            )
                            Text(
                                text = "Reward: +${obj.rewardSkillPoints} Skill Pt • +${obj.rewardSelectionBonus}% Selection",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                                color = TurfEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CareerSkillsAndBadgesContent(
    career: CareerSaveState,
    onSpendSkillPoint: (CareerSkillStat) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by rememberSaveable { mutableStateOf(CareerSkillCategory.BATTING) }
    val statsForCategory = CareerSkillStat.entries.filter { it.category == selectedCategory }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Skill Points Header & Upgrade Tree
        item(key = "skill_tree_card") {
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.2.dp, TurfEmerald.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PLAYER DEVELOPMENT",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = DisplayFontFamily,
                                    letterSpacing = 1.sp,
                                    color = BroadcastWhite
                                )
                            )
                            Text(
                                text = "Spend Skill Points earned from matches & objectives (+2 per point)",
                                style = MaterialTheme.typography.labelSmall,
                                color = BroadcastSilver
                            )
                        }
                        Surface(
                            color = if (career.skillPoints > 0) AuctionGold else NavySurfaceHigh,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AuctionGold)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${career.skillPoints} SP",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = MonoFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (career.skillPoints > 0) OnGoldDark else AuctionGold
                                    )
                                )
                                Text(
                                    text = "AVAILABLE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                    color = if (career.skillPoints > 0) OnGoldDark else BroadcastMuted
                                )
                            }
                        }
                    }

                    // Category Tabs: Batting / Bowling / Fielding
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CareerSkillCategory.entries.forEach { cat ->
                            val ratingVal = when (cat) {
                                CareerSkillCategory.BATTING -> career.attributes.battingRating
                                CareerSkillCategory.BOWLING -> career.attributes.bowlingRating
                                CareerSkillCategory.FIELDING -> career.attributes.fieldingRating
                            }
                            val accent = when (cat) {
                                CareerSkillCategory.BATTING -> TurfEmerald
                                CareerSkillCategory.BOWLING -> BoundarySky
                                CareerSkillCategory.FIELDING -> AuctionGold
                            }
                            CareerSelectChip(
                                label = "${cat.label} ($ratingVal)",
                                selected = selectedCategory == cat,
                                accent = accent,
                                onClick = { selectedCategory = cat },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("career_skill_tab_${cat.name.lowercase()}")
                            )
                        }
                    }

                    // Granular 3 Sub-skills for selected category
                    statsForCategory.forEach { stat ->
                        val value = career.attributes.valueOf(stat)
                        val canUpgrade = career.skillPoints > 0 && value < 99
                        val accent = when (stat.category) {
                            CareerSkillCategory.BATTING -> TurfEmerald
                            CareerSkillCategory.BOWLING -> BoundarySky
                            CareerSkillCategory.FIELDING -> AuctionGold
                        }
                        Surface(
                            color = NavySurfaceHigh,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NavyOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stat.label,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = BroadcastWhite
                                        )
                                        Text(
                                            text = "$value / 99",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = MonoFontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = accent
                                            )
                                        )
                                    }
                                    Text(
                                        text = stat.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BroadcastMuted
                                    )
                                    LinearProgressIndicator(
                                        progress = { (value / 99f).coerceIn(0f, 1f) },
                                        color = accent,
                                        trackColor = NavyMidnight,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                    )
                                }
                                Surface(
                                    color = if (canUpgrade) TurfEmerald else NavyMidnight,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, if (canUpgrade) TurfEmerald else NavyOutline),
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = canUpgrade) { onSpendSkillPoint(stat) }
                                        .testTag("career_upgrade_${stat.name.lowercase()}")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = "Upgrade ${stat.label}",
                                            tint = if (canUpgrade) OnEmeraldDark else BroadcastMuted,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Career Achievements Tracker (All 6 Badges)
        item(key = "achievements_card") {
            val unlockedCount = CareerAchievementId.entries.count { career.hasAchievement(it) }
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.2.dp, AuctionGold.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("career_achievements_section")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(20.dp))
                            Text(
                                text = "CAREER ACHIEVEMENTS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = DisplayFontFamily,
                                    letterSpacing = 1.sp,
                                    color = BroadcastWhite
                                )
                            )
                        }
                        Surface(
                            color = AuctionGold.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "$unlockedCount / ${CareerAchievementId.entries.size} UNLOCKED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = AuctionGold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    CareerAchievementId.entries.forEach { badge ->
                        val unlocked = career.hasAchievement(badge)
                        val borderColor = if (unlocked) AuctionGold.copy(alpha = 0.7f) else NavyOutline
                        Surface(
                            color = if (unlocked) AuctionGold.copy(alpha = 0.12f) else NavySurfaceHigh,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("career_achievement_${badge.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    color = if (unlocked) AuctionGold else NavyMidnight,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (unlocked) Icons.Filled.Star else Icons.Filled.Lock,
                                            contentDescription = if (unlocked) "Unlocked ${badge.title}" else "Locked ${badge.title}",
                                            tint = if (unlocked) OnGoldDark else BroadcastMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = badge.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (unlocked) AuctionGold else BroadcastWhite
                                        )
                                        Surface(
                                            color = if (unlocked) TurfEmerald.copy(alpha = 0.2f) else NavyMidnight,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = badge.badgeTag,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontFamily = MonoFontFamily,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (unlocked) TurfEmerald else BroadcastMuted
                                                ),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = badge.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BroadcastSilver
                                    )
                                }
                                if (unlocked) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = TurfEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
