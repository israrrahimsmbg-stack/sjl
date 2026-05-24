package com.example.ui.tournament

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Match
import com.example.data.model.Team
import com.example.viewmodel.PointsTableEntry
import com.example.viewmodel.ScoringViewModel
import com.example.viewmodel.TournamentViewModel
import java.util.Locale
import androidx.compose.ui.window.Dialog

import com.example.viewmodel.PlayerViewModel

@Composable
fun TournamentScreen(
    tournamentViewModel: TournamentViewModel,
    scoringViewModel: ScoringViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToScoring: () -> Unit,
    onNavigateToScorecard: (Int) -> Unit
) {
    val matches by tournamentViewModel.allMatches.collectAsState()
    val teams by tournamentViewModel.allTeams.collectAsState()
    val tableEntries by tournamentViewModel.pointsTable.collectAsState()

    var activeTab by remember { mutableIntStateOf(1) } // 1: Points Table, 2: Fixtures, 3: Playoffs
    var showTeamsManageDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F9F5))
    ) {
        // TAB BAR SWITCHER
        TabRow(
            selectedTabIndex = activeTab - 1,
            containerColor = Color(0xFF0D3A1F), // Rich Pakistani Deep Emerald Green
            contentColor = Color(0xFFF6C344) // Star Yellow Gold
        ) {
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                Text("RANKINGS 🥇", color = if (activeTab == 1) Color(0xFFF6C344) else Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
            }
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                Text("FIXTURES 📅", color = if (activeTab == 2) Color(0xFFF6C344) else Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
            }
            Tab(selected = activeTab == 3, onClick = { activeTab = 3 }) {
                Text("PLAYOFFS 🏆", color = if (activeTab == 3) Color(0xFFF6C344) else Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
            }
        }

        when (activeTab) {
            1 -> PointsTableTab(tableEntries, onEditTeamsClick = { showTeamsManageDialog = true })
            2 -> FixturesTab(matches, teams, scoringViewModel, onNavigateToScoring, onNavigateToScorecard)
            3 -> PlayoffsTab(tableEntries)
        }
    }

    if (showTeamsManageDialog) {
        ManageTeamsDialog(
            teams = teams,
            onModifyTeam = { id, name, short, color, ground ->
                playerViewModel.modifyTeam(id, name, short, color, ground)
            },
            onDismiss = { showTeamsManageDialog = false }
        )
    }
}

// 1. POINTS TABLE VIEW
@Composable
fun PointsTableTab(
    entries: List<PointsTableEntry>,
    onEditTeamsClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 SJL POINTS & RANKINGS",
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF104C2E), // Modern Deep Emerald Green
                    fontSize = 14.sp,
                    letterSpacing = 1.sp
                )

                // Manage Teams button
                Button(
                    onClick = onEditTeamsClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF104C2E)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("✏️ EDIT TEAMS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }

        // Table headers Row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1F13))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("TEAM", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White, modifier = Modifier.weight(1.8f))
                    Text("M", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                    Text("W", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                    Text("L", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                    Text("PTS", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color(0xFFD4A017), modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                    Text("NRR", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
                }
            }
        }

        // Team rows
        items(entries.size) { index ->
            val entry = entries[index]
            val countRank = index + 1
            val teamColor = try {
                Color(android.graphics.Color.parseColor(entry.colorHex))
            } catch (e: Exception) {
                Color(0xFF1A5C2E)
            }

            // Top 4 qualified have a Gold Left Border, bottom 4 have a Red Left Border
            val borderSideColor = if (countRank <= 4) Color(0xFFD4A017) else Color(0xFFD32F2F)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)), RoundedCornerShape(10.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Qualifier sidebar border
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(6.dp)
                            .background(borderSideColor)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rank medals or numbers
                        val medal = when (countRank) {
                            1 -> "🥇 "
                            2 -> "🥈 "
                            3 -> "🥉 "
                            else -> " "
                        }

                        Row(modifier = Modifier.width(24.dp), horizontalArrangement = Arrangement.Center) {
                            if (countRank <= 3) {
                                Text(medal, fontSize = 13.sp)
                            } else {
                                Text(countRank.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            }
                        }

                        // Team short name and full name details
                        Column(modifier = Modifier.weight(1.8f)) {
                            Text(entry.name, fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF0D1F13))
                            Text("Ground: Samarbagh Oval", fontSize = 9.sp, color = Color.Gray)
                        }

                        Text(entry.played.toString(), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                        Text(entry.won.toString(), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                        Text(entry.lost.toString(), fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)

                        Text(
                            text = entry.points.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFD4A017),
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = String.format(Locale.US, "%+.3f", entry.nrr),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (entry.nrr >= 0.0) Color(0xFF1A5C2E) else Color(0xFFD32F2F),
                            modifier = Modifier.width(52.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

// 2. FIXTURES VIEW
@Composable
fun FixturesTab(
    matches: List<Match>,
    teams: List<Team>,
    scoringViewModel: ScoringViewModel,
    onNavigateToScoring: () -> Unit,
    onNavigateToScorecard: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "📅 SJL ROUND-ROBIN LEAGUE MATCHES",
                fontWeight = FontWeight.Black,
                color = Color(0xFF1A5C2E),
                fontSize = 14.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        items(matches) { match ->
            val t1 = teams.find { it.id == match.team1Id }
            val t2 = teams.find { it.id == match.team2Id }
            val isLive = match.status == "live"
            val isComplete = match.status == "complete"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isComplete) {
                            onNavigateToScorecard(match.id)
                        } else {
                            scoringViewModel.onSquadSelected(match.team1Id, match.team2Id)
                            onNavigateToScoring()
                        }
                    },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isLive) Color.Red else Color.LightGray.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(match.date, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Box(
                            modifier = Modifier
                                .background(
                                    when (match.status) {
                                        "live" -> Color.Red
                                        "complete" -> Color(0xFF1A5C2E)
                                        else -> Color.DarkGray
                                    },
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = match.status.uppercase(Locale.US),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(t1?.name ?: "Team A", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("vs", color = Color(0xFFD4A017), fontSize = 11.sp, fontWeight = FontWeight.Thin)
                            Text(t2?.name ?: "Team B", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        if (isComplete) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "TAP FOR CARD",
                                    color = Color(0xFF1A5C2E),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = match.resultDescription,
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.End,
                                    maxLines = 1
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFD4A017), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isLive) "LIVE SCORER 🎯" else "START SCORE 🏏",
                                    color = Color(0xFF0D1F13),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. PLAYOFFS TAB (VISUAL CANVAS DRAWS)
@Composable
fun PlayoffsTab(tableEntries: List<PointsTableEntry>) {
    val totalMatchesPlayed = tableEntries.sumOf { it.played }
    val isLeagueFinished = totalMatchesPlayed >= 56 // 28 matches * 2 entries each = 56 matches played altogether

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🏆 SJL FINALS BRACKET & PLAYOFFS",
            fontWeight = FontWeight.Black,
            color = Color(0xFF1A5C2E),
            fontSize = 15.sp,
            letterSpacing = 1.sp
        )

        // If tournament is not complete, we display qualified top 4 mock nodes
        val top4 = if (tableEntries.size >= 4) tableEntries.take(4) else emptyList()

        val rank1 = top4.getOrNull(0)?.shortName ?: "RANK 1"
        val rank2 = top4.getOrNull(1)?.shortName ?: "RANK 2"
        val rank3 = top4.getOrNull(2)?.shortName ?: "RANK 3"
        val rank4 = top4.getOrNull(3)?.shortName ?: "RANK 4"

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "VISUAL FINALS PATHWAY",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Playoff Nodes with Custom Canvas Bracket connections
                PlayoffBracketWidget(rank1, rank2, rank3, rank4)
            }
        }

        if (!isLeagueFinished) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)),
                border = BorderStroke(1.dp, Color(0xFFD4A017))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "⚠️ LEAGUE STAGE RUNNING",
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFD4A017),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Playoff bracket values of ($rank1, $rank2, $rank3, $rank4) are currently live-projected based on active rankings. Brackets lock once all round-robin fixtures are marked complete.",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

@Composable
fun PlayoffBracketWidget(r1: String, r2: String, r3: String, r4: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw connection lines inside canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val leftLineX = width * 0.28f
            val rightLineX = width * 0.72f

            // Left side joins (Semi Final 1)
            drawLine(
                color = Color(0xFF1E5C2E),
                start = Offset(leftLineX, height * 0.2f),
                end = Offset(leftLineX + 30f, height * 0.4f),
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0xFF1E5C2E),
                start = Offset(leftLineX, height * 0.6f),
                end = Offset(leftLineX + 30f, height * 0.4f),
                strokeWidth = 3f
            )

            // Right side joins (Semi Final 2)
            drawLine(
                color = Color(0xFF1E5C2E),
                start = Offset(rightLineX, height * 0.2f),
                end = Offset(rightLineX - 30f, height * 0.4f),
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0xFF1E5C2E),
                start = Offset(rightLineX, height * 0.6f),
                end = Offset(rightLineX - 30f, height * 0.4f),
                strokeWidth = 3f
            )

            // Joint finals connectors
            drawLine(
                color = Color(0xFFD4A017),
                start = Offset(leftLineX + 30f, height * 0.4f),
                end = Offset(width * 0.5f, height * 0.8f),
                strokeWidth = 4f
            )
            drawLine(
                color = Color(0xFFD4A017),
                start = Offset(rightLineX - 30f, height * 0.4f),
                end = Offset(width * 0.5f, height * 0.8f),
                strokeWidth = 4f
            )
        }

        // Left side player nodes
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 10.dp, start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            PlayoffNodeBox(text = "R1: $r1")
            PlayoffNodeBox(text = "R4: $r4")
        }

        // Right side player nodes
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            PlayoffNodeBox(text = "R2: $r2")
            PlayoffNodeBox(text = "R3: $r3")
        }

        // Center Finalists Nodes
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(40.dp)
        ) {
            PlayoffNodeBox(text = "SF1 winner", color = Color(0xFF1A5C2E))
            PlayoffNodeBox(text = "SF2 winner", color = Color(0xFF1A5C2E))
        }

        // Bottom Champion Node
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        ) {
            PlayoffNodeBox(text = "🏆 CHAMPION", color = Color(0xFFD4A017), textColor = Color.Black)
        }
    }
}

@Composable
fun PlayoffNodeBox(
    text: String,
    color: Color = Color.White,
    textColor: Color = Color(0xFF0D1F13)
) {
    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF1A5C2E), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
    }
}

@Composable
fun ManageTeamsDialog(
    teams: List<Team>,
    onModifyTeam: (Int, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var editingTeam by remember { mutableStateOf<Team?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "✏️ MANAGE TEAM NAMES",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color(0xFF104C2E)
                )
                Text(
                    text = "Tap on any team below to customize its name, code, colors, or home ground.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                LazyColumn(
                    modifier = Modifier
                        .height(280.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(teams) { team ->
                        val teamColor = try {
                            Color(android.graphics.Color.parseColor(team.colorHex))
                        } catch (e: Exception) {
                            Color(0xFF104C2E)
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingTeam = team },
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAF7)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(teamColor, RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${team.name} (${team.shortName})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF0D1F13)
                                    )
                                    Text(
                                        text = "📍 Home: ${team.homeGround}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                                Text("Edit ➔", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF104C2E))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CLOSE", fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                }
            }
        }
    }

    if (editingTeam != null) {
        val team = editingTeam!!
        var nameInput by remember(team) { mutableStateOf(team.name) }
        var shortNameInput by remember(team) { mutableStateOf(team.shortName) }
        var colorHexInput by remember(team) { mutableStateOf(team.colorHex) }
        var homeGroundInput by remember(team) { mutableStateOf(team.homeGround) }

        val presetColors = listOf("#01411C", "#F6C344", "#0A9396", "#1B4965", "#AE2012", "#4F1D7A", "#E07A5F")

        Dialog(onDismissRequest = { editingTeam = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🎨 EDIT ${team.name} DETAILS",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = Color(0xFF104C2E)
                    )

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Team Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = shortNameInput,
                        onValueChange = { shortNameInput = it.take(4).uppercase(Locale.US) },
                        label = { Text("Short Code (e.g. SST)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = homeGroundInput,
                        onValueChange = { homeGroundInput = it },
                        label = { Text("Home Ground") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Theme Color (Hex or Preset):", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetColors.forEach { hexColor ->
                            val c = Color(android.graphics.Color.parseColor(hexColor))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(c, RoundedCornerShape(6.dp))
                                    .border(
                                        width = if (colorHexInput.lowercase(Locale.US) == hexColor.lowercase(Locale.US)) 2.dp else 0.dp,
                                        color = Color.Black,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { colorHexInput = hexColor }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = colorHexInput,
                        onValueChange = { colorHexInput = it },
                        label = { Text("Custom Color Hex (e.g. #01411C)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { editingTeam = null }) {
                            Text("CANCEL", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (nameInput.isNotBlank() && shortNameInput.isNotBlank()) {
                                    val safeColor = if (colorHexInput.startsWith("#") && (colorHexInput.length == 7 || colorHexInput.length == 9)) {
                                        colorHexInput
                                    } else {
                                        "#01411C"
                                    }
                                    onModifyTeam(team.id, nameInput.trim(), shortNameInput.trim(), safeColor, homeGroundInput.trim())
                                    editingTeam = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF104C2E))
                        ) {
                            Text("SAVE", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
