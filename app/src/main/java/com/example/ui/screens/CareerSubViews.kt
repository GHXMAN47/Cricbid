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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BattingStyle
import com.example.data.model.BowlingStyle
import com.example.data.model.CareerDifficulty
import com.example.data.model.CurrencyFormatter
import com.example.data.model.HallOfFameLegend
import com.example.data.model.InternationalNations
import com.example.data.model.PlayerRole
import com.example.engine.CareerEngine
import com.example.engine.IplAuctionDraftOutcome
import com.example.ui.components.IndianPlayerAvatar
import com.example.ui.components.IndianPlayerAvatars
import com.example.ui.components.RolePillBadge
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
internal fun RookieCreatorContent(
    onCreateCareer: (
        name: String,
        age: Int,
        nationality: String,
        countryCode: String,
        avatarId: String,
        role: PlayerRole,
        battingStyle: BattingStyle,
        bowlingStyle: BowlingStyle,
        difficulty: CareerDifficulty
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by rememberSaveable { mutableStateOf("Aarav Sharma") }
    var startingAge by rememberSaveable { mutableIntStateOf(18) }
    var countryCode by rememberSaveable { mutableStateOf("IND") }
    var avatarId by rememberSaveable { mutableStateOf(IndianPlayerAvatars.DEFAULT_ID) }
    var role by rememberSaveable { mutableStateOf(PlayerRole.BATTER) }
    var battingStyle by rememberSaveable { mutableStateOf(BattingStyle.RIGHT_HAND_BAT) }
    var bowlingStyle by rememberSaveable { mutableStateOf(BowlingStyle.OCCASIONAL_MEDIUM) }
    var difficulty by rememberSaveable { mutableStateOf(CareerDifficulty.NORMAL) }

    val selectedNation = InternationalNations.all.firstOrNull { it.code == countryCode } ?: InternationalNations.all.first()
    val preview = CareerEngine.createRookie(
        name = name,
        startingAge = startingAge,
        nationality = selectedNation.name,
        countryCode = selectedNation.code,
        avatarId = avatarId,
        role = role,
        battingStyle = battingStyle,
        bowlingStyle = bowlingStyle,
        difficulty = difficulty
    )

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.2.dp, AuctionGold.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IndianPlayerAvatar(avatarId = avatarId, size = 58.dp, selected = true)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name.trim().ifBlank { "Rookie Star" },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = BroadcastWhite
                        )
                        Text(
                            text = "Age $startingAge • ${selectedNation.name} • ${role.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BroadcastSilver
                        )
                        Text(
                            text = "BAT ${preview.attributes.battingRating} • BOWL ${preview.attributes.bowlingRating} • FLD ${preview.attributes.fieldingRating}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = BoundarySky,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    Surface(
                        color = AuctionGold.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AuctionGold)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("OVR", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = AuctionGold)
                            Text(
                                "${preview.overallRating}",
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.ExtraBold),
                                color = AuctionGold
                            )
                        }
                    }
                }
            }
        }

        // Name & Starting Age
        item {
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NavyOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("CREATE YOUR ROOKIE", style = MaterialTheme.typography.labelMedium, color = TurfEmerald)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 28) name = it },
                        label = { Text("Player Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TurfEmerald,
                            unfocusedBorderColor = NavyOutline,
                            focusedTextColor = BroadcastWhite,
                            unfocusedTextColor = BroadcastWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("career_name_input")
                    )
                    Text("STARTING AGE", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(17, 18, 19, 20).forEach { ageOpt ->
                            CareerSelectChip(
                                label = "$ageOpt yrs",
                                selected = startingAge == ageOpt,
                                accent = TurfEmerald,
                                onClick = { startingAge = ageOpt },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Avatar Picker
        item {
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NavyOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PLAYER AVATAR", style = MaterialTheme.typography.labelSmall, color = AuctionGold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(IndianPlayerAvatars.all, key = { it.id }) { av ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { avatarId = av.id }
                                    .padding(4.dp)
                            ) {
                                IndianPlayerAvatar(
                                    avatarId = av.id,
                                    size = 48.dp,
                                    selected = avatarId == av.id,
                                    showJerseyPill = false
                                )
                                Text(
                                    text = av.playerName.substringAfterLast(" "),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (avatarId == av.id) AuctionGold else BroadcastMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Nationality & Role & Styles
        item {
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NavyOutline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("NATIONALITY", style = MaterialTheme.typography.labelSmall, color = BoundarySky)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(InternationalNations.all, key = { it.code }) { nation ->
                            CareerSelectChip(
                                label = "${nation.code} • ${nation.name}",
                                selected = countryCode == nation.code,
                                accent = BoundarySky,
                                onClick = { countryCode = nation.code }
                            )
                        }
                    }

                    Text("PRIMARY ROLE (STARTS 55–62 OVR)", style = MaterialTheme.typography.labelSmall, color = TurfEmerald)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PlayerRole.entries) { r ->
                            val roleLabel = when (r) {
                                PlayerRole.SEAMER -> "Fast Bowler"
                                else -> r.displayName
                            }
                            CareerSelectChip(
                                label = roleLabel,
                                selected = role == r,
                                accent = TurfEmerald,
                                onClick = {
                                    role = r
                                    bowlingStyle = when (r) {
                                        PlayerRole.SEAMER -> BowlingStyle.RIGHT_ARM_FAST
                                        PlayerRole.SPINNER -> BowlingStyle.RIGHT_ARM_LEGSPIN
                                        PlayerRole.ALL_ROUNDER -> BowlingStyle.RIGHT_ARM_FAST
                                        else -> BowlingStyle.OCCASIONAL_MEDIUM
                                    }
                                }
                            )
                        }
                    }

                    Text("BATTING STYLE", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BattingStyle.entries.forEach { bs ->
                            CareerSelectChip(
                                label = bs.label,
                                selected = battingStyle == bs,
                                accent = AuctionGold,
                                onClick = { battingStyle = bs },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text("BOWLING STYLE", style = MaterialTheme.typography.labelSmall, color = BroadcastMuted)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(BowlingStyle.entries) { bw ->
                            CareerSelectChip(
                                label = bw.label,
                                selected = bowlingStyle == bw,
                                accent = BoundarySky,
                                onClick = { bowlingStyle = bw }
                            )
                        }
                    }
                }
            }
        }

        // Career Difficulty Selector (Easy / Normal / Hard)
        item {
            Surface(
                color = NavySurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, careerDifficultyAccent(difficulty).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CAREER DIFFICULTY", style = MaterialTheme.typography.labelSmall, color = AuctionGold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CareerDifficulty.entries.forEach { diff ->
                            CareerSelectChip(
                                label = diff.label,
                                selected = difficulty == diff,
                                accent = careerDifficultyAccent(diff),
                                onClick = { difficulty = diff },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("career_create_difficulty_${diff.name.lowercase()}")
                            )
                        }
                    }
                    Text(
                        text = difficulty.summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = BroadcastSilver
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    onCreateCareer(
                        name.trim().ifBlank { "Rookie Star" },
                        startingAge,
                        selectedNation.name,
                        selectedNation.code,
                        avatarId,
                        role,
                        battingStyle,
                        bowlingStyle,
                        difficulty
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TurfEmerald, contentColor = OnEmeraldDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("start_career_button")
            ) {
                Icon(Icons.Filled.SportsCricket, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Begin Road to Legend (${difficulty.label})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
internal fun IplAuctionDraftSummaryCard(
    draft: IplAuctionDraftOutcome,
    onDismiss: () -> Unit
) {
    Surface(
        color = AuctionGold.copy(alpha = 0.14f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, AuctionGold),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("career_ipl_draft_card")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Gavel, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "IPL AUCTION RESULT — SOLD!",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = DisplayFontFamily, color = AuctionGold)
                )
            }
            Text(
                text = "${draft.winningFranchise} • ${CurrencyFormatter.formatLakhs(draft.finalBidLakhs)}",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = BroadcastWhite
            )
            draft.biddingHighlights.forEach { line ->
                Text("• $line", style = MaterialTheme.typography.bodySmall, color = BroadcastSilver)
            }
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AuctionGold, contentColor = OnGoldDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Join ${draft.winningFranchise}", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun RetirementSummaryCard(
    legend: HallOfFameLegend,
    onContinueToHallOfFame: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                color = AuctionGold.copy(alpha = 0.16f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, AuctionGold),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("career_retirement_summary")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(40.dp))
                    Text(
                        text = "CAREER RETIREMENT • HALL OF FAME",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                        color = AuctionGold
                    )
                    IndianPlayerAvatar(avatarId = legend.avatarId, size = 72.dp, selected = true)
                    Text(
                        text = "${legend.name}${if (legend.wasCaptain) " (C)" else ""}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = DisplayFontFamily, color = BroadcastWhite)
                    )
                    Text(
                        text = "${legend.nationality} • Retired Age ${legend.retiredAge} • ${legend.seasonsPlayed} Seasons • Peak ${legend.peakOverall} OVR • ${legend.difficulty.shortBadge}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BroadcastSilver
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendStatColumn("MATCHES", "${legend.matchesPlayed}", BoundarySky)
                        LegendStatColumn("RUNS", "${legend.totalRuns}", TurfEmerald)
                        LegendStatColumn("100s", "${legend.centuries}", AuctionGold)
                        LegendStatColumn("WKTS", "${legend.totalWickets}", BoundarySky)
                        LegendStatColumn("TROPHIES", "${legend.totalTrophies}", AuctionGold)
                    }
                    if (legend.awards.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = legend.awards.joinToString("  •  "),
                            style = MaterialTheme.typography.labelSmall,
                            color = TurfEmerald
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Inducted into the CricBid Legends Pool! ${legend.name} is now eligible to appear in future IPL Auctions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BroadcastWhite
                    )
                    Button(
                        onClick = onContinueToHallOfFame,
                        colors = ButtonDefaults.buttonColors(containerColor = AuctionGold, contentColor = OnGoldDark),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("view_hall_of_fame_button")
                    ) {
                        Text("View Hall of Fame & Start New Career", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
internal fun HallOfFameListContent(
    legends: List<HallOfFameLegend>,
    onToggleAuctionPool: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (legends.isEmpty()) {
            item {
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NavyOutline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = AuctionGold, modifier = Modifier.size(34.dp))
                        Text("No Hall of Fame Legends Yet", style = MaterialTheme.typography.titleMedium, color = BroadcastWhite)
                        Text(
                            text = "Complete a Career Mode journey and retire your player to enshrine them here and unlock them in future IPL Auctions!",
                            style = MaterialTheme.typography.bodySmall,
                            color = BroadcastSilver
                        )
                    }
                }
            }
        } else {
            items(legends, key = { it.id }) { legend ->
                Surface(
                    color = NavySurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AuctionGold.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hof_legend_${legend.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IndianPlayerAvatar(avatarId = legend.avatarId, size = 48.dp, selected = true, showJerseyPill = false)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "${legend.name}${if (legend.wasCaptain) " (C)" else ""}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = BroadcastWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    RolePillBadge(role = legend.role)
                                }
                                Text(
                                    text = "${legend.nationality} • Peak ${legend.peakOverall} OVR • ${legend.seasonsPlayed} Seasons • ${legend.difficulty.shortBadge}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AuctionGold
                                )
                            }
                        }
                        Text(
                            text = "${legend.matchesPlayed}M • ${legend.totalRuns} Runs (HS ${legend.highestScore}, ${legend.centuries}x100) • ${legend.totalWickets} Wkts • ${legend.totalTrophies} Trophies",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                            color = BroadcastSilver
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Include in Future Auctions",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = TurfEmerald
                            )
                            Switch(
                                checked = legend.includeInAuctions,
                                onCheckedChange = { onToggleAuctionPool(legend.id, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = OnEmeraldDark,
                                    checkedTrackColor = TurfEmerald
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CareerSelectChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) accent.copy(alpha = 0.22f) else NavySurfaceHigh,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.2.dp, if (selected) accent else NavyOutline),
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) accent else BroadcastSilver
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

internal fun careerDifficultyAccent(difficulty: CareerDifficulty): Color = when (difficulty) {
    CareerDifficulty.EASY -> TurfEmerald
    CareerDifficulty.NORMAL -> BoundarySky
    CareerDifficulty.HARD -> WicketCrimson
}

@Composable
private fun LegendStatColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold), color = color)
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = BroadcastMuted)
    }
}
