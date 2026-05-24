package com.example.ui.scorecard

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Ball
import com.example.data.model.Innings
import com.example.data.model.Match
import com.example.data.model.Player
import com.example.data.model.Team
import com.example.viewmodel.PlayerViewModel
import com.example.viewmodel.ScoringViewModel
import com.example.viewmodel.TournamentViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScorecardScreen(
    scoringViewModel: ScoringViewModel,
    tournamentViewModel: TournamentViewModel,
    playerViewModel: PlayerViewModel,
    selectedMatchId: Int? = null
) {
    val matches by tournamentViewModel.allMatches.collectAsState()
    val teams by tournamentViewModel.allTeams.collectAsState()
    val players by playerViewModel.allPlayers.collectAsState()

    var activeMatchId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(selectedMatchId, matches) {
        if (selectedMatchId != null) {
            activeMatchId = selectedMatchId
        } else if (activeMatchId == null && matches.isNotEmpty()) {
            activeMatchId = matches.first().id
        }
    }

    val match = matches.find { it.id == activeMatchId }

    var inningsList by remember { mutableStateOf<List<Innings>>(emptyList()) }
    var firstInningsBalls by remember { mutableStateOf<List<Ball>>(emptyList()) }
    var secondInningsBalls by remember { mutableStateOf<List<Ball>>(emptyList()) }

    LaunchedEffect(activeMatchId) {
        if (activeMatchId != null) {
            val list = scoringViewModel.repository.getInningsForMatch(activeMatchId!!)
            inningsList = list
            val inn1 = list.find { it.inningsNumber == 1 }
            val inn2 = list.find { it.inningsNumber == 2 }
            if (inn1 != null) {
                firstInningsBalls = scoringViewModel.repository.getBallsForInnings(inn1.id)
            }
            if (inn2 != null) {
                secondInningsBalls = scoringViewModel.repository.getBallsForInnings(inn2.id)
            }
        }
    }

    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val screenBg = if (isDark) Color(0xFF101B13) else Color(0xFFF4F9F5)
    val themeCardBg = MaterialTheme.colorScheme.surface
    val themeTextPrimary = MaterialTheme.colorScheme.onSurface
    val themeHeaderBg = if (isDark) Color(0xFF1E2D24) else Color(0xFFE8F2EA)
    val themeSubCardBg = if (isDark) Color(0xFF152A1C) else Color(0xFFF0FDF4)
    val themeHeaderText = if (isDark) Color(0xFF81C784) else Color(0xFF0D1F13)
    val themeHeadingGreen = if (isDark) Color(0xFF6EE7B7) else Color(0xFF1A5C2E)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
    ) {
        // MATCH SELECTOR BAR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0D1F13))
                .padding(12.dp)
        ) {
            var expanded by remember { mutableStateOf(false) }
            val matchDesc = if (match != null) {
                val t1 = teams.find { it.id == match.team1Id }
                val t2 = teams.find { it.id == match.team2Id }
                "${t1?.shortName ?: "T1"} vs ${t2?.shortName ?: "T2"} (${match.date})"
            } else "Select Match Scorecard"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFFD4A017), RoundedCornerShape(8.dp))
                    .clickable { expanded = true }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$matchDesc ▾",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    matches.forEach { m ->
                        val t1Obj = teams.find { it.id == m.team1Id }
                        val t2Obj = teams.find { it.id == m.team2Id }
                        DropdownMenuItem(
                            text = { Text("${t1Obj?.shortName} vs ${t2Obj?.shortName} (${m.date}) - ${m.status.uppercase(Locale.US)}") },
                            onClick = {
                                activeMatchId = m.id
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        if (match == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No match selected or available.", color = Color.Gray)
            }
        } else {
            val team1 = teams.find { it.id == match.team1Id }
            val team2 = teams.find { it.id == match.team2Id }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                item {
                    // Match outcome statement
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = themeCardBg,
                            contentColor = themeTextPrimary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${team1?.name} vs ${team2?.name}",
                                fontWeight = FontWeight.Bold,
                                color = themeTextPrimary,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = match.resultDescription,
                                fontWeight = FontWeight.ExtraBold,
                                color = themeHeadingGreen,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Venue: ${match.venue} | Overs format: ${match.totalOvers} Ov",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                // Innings loops
                inningsList.forEach { innings ->
                    val isInn1 = innings.inningsNumber == 1
                    val inningsBalls = if (isInn1) firstInningsBalls else secondInningsBalls
                    val battingTeamObj = teams.find { it.id == innings.battingTeamId }
                    val bowlingTeamObj = teams.find { it.id == innings.bowlingTeamId }

                    item {
                        Text(
                            text = "${battingTeamObj?.name?.uppercase(Locale.US)} INNINGS scorecard (${innings.totalRuns}/${innings.wickets})",
                            fontWeight = FontWeight.Black,
                            color = themeHeadingGreen,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // BATTING TABLE
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = themeCardBg,
                                contentColor = themeTextPrimary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column {
                                // Table Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(themeHeaderBg)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("BATSMAN", fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.weight(2.2f), color = themeHeaderText)
                                    Text("R", fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, color = themeHeaderText)
                                    Text("B", fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, color = themeHeaderText)
                                    Text("4s", fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center, color = themeHeaderText)
                                    Text("6s", fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center, color = themeHeaderText)
                                    Text("SR", fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End, color = themeHeaderText)
                                }

                                val teamSquad = players.filter { it.teamId == innings.battingTeamId }

                                // Identify who has batted: any batsman who is in balls list, or was marked out
                                val battingBallsGroup = inningsBalls.groupBy { it.batsmanId }

                                teamSquad.forEach { player ->
                                    val batsmanBalls = battingBallsGroup[player.id] ?: emptyList()
                                    val hasBatted = batsmanBalls.isNotEmpty()

                                    if (hasBatted) {
                                        val runs = batsmanBalls.sumOf { it.runs }
                                        val ballsFaced = batsmanBalls.count { it.extraType != "wide" }
                                        val fours = batsmanBalls.count { it.runs == 4 }
                                        val sixes = batsmanBalls.count { it.runs == 6 }
                                        val sr = if (ballsFaced > 0) (runs.toFloat() / ballsFaced) * 100f else 0f

                                        // Deduce dismissal status.
                                        val outBall = inningsBalls.find { it.batsmanId == player.id && it.isWicket }
                                        val dismissalText = if (outBall != null) {
                                            when (outBall.wicketType) {
                                                "bowled" -> "b ${players.find { it.id == outBall.bowlerId }?.name ?: "bowler"}"
                                                "caught" -> "c ${outBall.fielderName} b ${players.find { it.id == outBall.bowlerId }?.name ?: "bowler"}"
                                                "lbw" -> "lbw b ${players.find { it.id == outBall.bowlerId }?.name ?: "bowler"}"
                                                "runout" -> "run out (${outBall.fielderName})"
                                                "stumped" -> "stumped c ${outBall.fielderName} b ${players.find { it.id == outBall.bowlerId }?.name ?: "bowler"}"
                                                "caughtbowled" -> "c & b ${players.find { it.id == outBall.bowlerId }?.name ?: "bowler"}"
                                                else -> "out"
                                            }
                                        } else "not out"

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(2.2f)) {
                                                Text(
                                                    text = player.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = themeTextPrimary
                                                )
                                                Text(
                                                    text = dismissalText,
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }

                                            Text(runs.toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, color = themeTextPrimary)
                                            Text(ballsFaced.toString(), fontSize = 13.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, color = Color.Gray)
                                            Text(fours.toString(), fontSize = 13.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center, color = Color.Gray)
                                            Text(sixes.toString(), fontSize = 13.sp, modifier = Modifier.weight(0.6f), textAlign = TextAlign.Center, color = Color.Gray)
                                            Text(String.format(Locale.US, "%.1f", sr), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1.1f), textAlign = TextAlign.End, color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF1A5C2E))
                                        }
                                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    }
                                }

                                // DNB Section below
                                val DNBList = teamSquad.filter { !battingBallsGroup.containsKey(it.id) }
                                if (DNBList.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "DNB: ",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = DNBList.joinToString { it.name },
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // BOWLING TABLE
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = themeCardBg,
                                contentColor = themeTextPrimary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(themeHeaderBg)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("BOWLER", fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.weight(2f), color = themeHeaderText)
                                    Text("O", fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, color = themeHeaderText)
                                    Text("R", fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, color = themeHeaderText)
                                    Text("W", fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, color = themeHeaderText)
                                    Text("ECO", fontWeight = FontWeight.Black, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = themeHeaderText)
                                }

                                val bowlingSquadPlayers = players.filter { it.teamId == innings.bowlingTeamId }
                                val bowlingBallsGroup = inningsBalls.groupBy { it.bowlerId }

                                bowlingSquadPlayers.forEach { player ->
                                    val bowlerBalls = bowlingBallsGroup[player.id] ?: emptyList()
                                    if (bowlerBalls.isNotEmpty()) {
                                        val runsConceded = bowlerBalls.sumOf {
                                            if (it.extraType == "wide" || it.extraType == "noball") it.runs + it.extras else it.runs
                                        }
                                        val wickets = bowlerBalls.count { it.isWicket && it.wicketType != "runout" }
                                        val legalBalls = bowlerBalls.count { it.isLegalDelivery }
                                        val overs = "${legalBalls / 6}.${legalBalls % 6}"
                                        val eco = if (legalBalls > 0) (runsConceded.toFloat() / legalBalls) * 6 else 0f

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(player.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(2f), color = themeTextPrimary)
                                            Text(overs, fontSize = 13.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, color = themeTextPrimary)
                                            Text(runsConceded.toString(), fontSize = 13.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, color = themeTextPrimary)
                                            Text(wickets.toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(0.7f), textAlign = TextAlign.Center, color = Color.Red)
                                            Text(String.format(Locale.US, "%.2f", eco), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End, color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF1A5C2E))
                                        }
                                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                    }
                                }
                            }
                        }
                    }

                    // EXTRAS & TEAM SUMMARY
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = themeSubCardBg),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("EXTRAS:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)
                                    Text(
                                        "Wd: ${innings.wides} | Nb: ${innings.noBalls} | B: ${innings.byes} | Lb: ${innings.legByes} | Total: ${innings.extras}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = themeTextPrimary
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("TOTAL SCORE:", fontWeight = FontWeight.Black, fontSize = 14.sp, color = themeHeadingGreen)
                                    Text("${innings.totalRuns} / ${innings.wickets} (${innings.oversBowled} ov)", fontWeight = FontWeight.Black, fontSize = 14.sp, color = themeHeadingGreen)
                                }
                            }
                        }
                    }

                    // INTERACTIVE TIMELINE AND FILTER CHIPS
                    item {
                        var activeFilter by remember { mutableStateOf("All") }

                        val filteredBalls = remember(inningsBalls, activeFilter) {
                            when (activeFilter) {
                                "Zeros Color 0" -> inningsBalls.filter { it.runs == 0 && !it.isWicket && it.extraType.isEmpty() }
                                "Ones Click 1" -> inningsBalls.filter { it.runs == 1 && it.extraType.isEmpty() }
                                "Twos Click 2" -> inningsBalls.filter { it.runs == 2 && it.extraType.isEmpty() }
                                "Threes Click 3" -> inningsBalls.filter { it.runs == 3 && it.extraType.isEmpty() }
                                "Fours Click 4" -> inningsBalls.filter { it.runs == 4 && it.extraType.isEmpty() }
                                "Sixes Click 6" -> inningsBalls.filter { it.runs == 6 && it.extraType.isEmpty() }
                                "Wickets Out" -> inningsBalls.filter { it.isWicket }
                                "Wides (Extra)" -> inningsBalls.filter { it.extraType == "wide" }
                                "No Balls (Extra)" -> inningsBalls.filter { it.extraType == "noball" }
                                "Byes (Extra)" -> inningsBalls.filter { it.extraType == "bye" || it.extraType == "legbye" }
                                else -> inningsBalls
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = themeCardBg,
                                contentColor = themeTextPrimary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "🔍 INTERACTIVE BALL TIMELINE FILTER",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    color = themeHeadingGreen,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Scrollable Row of Filter Buttons
                                val filters = listOf(
                                    "All" to "All",
                                    "Zeros" to "Zeros Color 0",
                                    "Ones" to "Ones Click 1",
                                    "Twos" to "Twos Click 2",
                                    "Threes" to "Threes Click 3",
                                    "Fours" to "Fours Click 4",
                                    "Sixes" to "Sixes Click 6",
                                    "Wickets" to "Wickets Out",
                                    "Wides" to "Wides (Extra)",
                                    "No-Balls" to "No Balls (Extra)",
                                    "Byes" to "Byes (Extra)"
                                )

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(filters) { (label, filtKey) ->
                                        val isSelected = activeFilter == filtKey
                                        val btnColor = if (isSelected) themeHeadingGreen else themeHeaderBg
                                        val textColor = if (isSelected) Color.White else themeHeaderText

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(btnColor)
                                                .clickable { activeFilter = filtKey }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Display count
                                Text(
                                    text = "Showing ${filteredBalls.size} deliveries",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Scrollable Row of individual deliveries matching
                                if (filteredBalls.isEmpty()) {
                                    Text(
                                        text = "No deliveries match this filter.",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(filteredBalls) { ball ->
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            ) {
                                                // Short name of batsman and bowler
                                                val batsmanInitial = players.find { it.id == ball.batsmanId }?.name?.split(" ")?.lastOrNull()?.take(5) ?: "Bat"
                                                val bowlerInitial = players.find { it.id == ball.bowlerId }?.name?.split(" ")?.lastOrNull()?.take(5) ?: "Bowl"

                                                Text(
                                                    text = "${ball.overNumber}.${(ball.ballNumber - 1) % 6 + 1}",
                                                    fontSize = 9.sp,
                                                    color = Color.Gray,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(18.dp))
                                                        .background(getBallColorForScorecard(ball)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = getBallTextForScorecard(ball),
                                                        fontSize = 11.sp,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Black
                                                    )
                                                }

                                                Text(
                                                    text = "$batsmanInitial v $bowlerInitial",
                                                    fontSize = 8.sp,
                                                    color = themeTextPrimary.copy(alpha = 0.7f),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // MANHATTAN CHART
                    item {
                        Column(modifier = Modifier.fillMaxWidth().background(themeCardBg).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).padding(12.dp)) {
                            Text("📊 OVER-BY-OVER MANHATTAN CHART", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = themeHeadingGreen, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            ManhattanChartCanvas(inningsBalls = inningsBalls, totalOvers = match.totalOvers)
                        }
                    }
                }

                // COMPREHENSIVE RUN RATE PROGRESSION (LINE CHART)
                if (firstInningsBalls.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth().background(themeCardBg).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).padding(12.dp)) {
                            Text("📈 RUN RATE PROGRESSION COMPILER", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = themeHeadingGreen, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            RunRateProgressionCanvas(
                                firstInningsBalls = firstInningsBalls,
                                secondInningsBalls = secondInningsBalls,
                                totalOvers = match.totalOvers
                            )
                        }
                    }
                }

                // SHARE TO WHATSAPP ACTION BUTTON
                item {
                    Button(
                        onClick = {
                            val plainTextScorecard = compileScorecardText(match, team1, team2, inningsList, players)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, plainTextScorecard)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Scorecard via")
                            context.startActivity(shareIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFFD4A017) else Color(0xFF1A5C2E)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Text("Share Elegant Scorecard to WhatsApp 🏆", color = if (isDark) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun ManhattanChartCanvas(inningsBalls: List<Ball>, totalOvers: Int) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(vertical = 4.dp)
    ) {
        val width = size.width
        val height = size.height

        // Group runs per over
        val legalBalls = inningsBalls.filter { it.isLegalDelivery }
        val runsPerOverList = mutableListOf<Int>()
        val wicketPerOverList = mutableListOf<Boolean>()

        for (o in 0 until totalOvers) {
            val overBalls = inningsBalls.filter { it.overNumber == o }
            if (overBalls.isNotEmpty()) {
                val runs = overBalls.sumOf { it.runs + it.extras }
                val hasWicket = overBalls.any { it.isWicket }
                runsPerOverList.add(runs)
                wicketPerOverList.add(hasWicket)
            } else {
                runsPerOverList.add(0)
                wicketPerOverList.add(false)
            }
        }

        val maxRuns = runsPerOverList.maxOrNull()?.coerceAtLeast(6) ?: 6
        val xUnit = width / totalOvers
        val yUnit = height / maxRuns

        // Draw grids
        for (i in 1..4) {
            val y = height - (height * i / 4f)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(0f, y),
                end = Offset(width, y)
            )
        }

        // Draw bars
        for (o in 0 until totalOvers) {
            val r = runsPerOverList[o]
            val hasWkt = wicketPerOverList[o]
            val barHeight = r * yUnit
            val barWidth = xUnit * 0.7f
            val xOffset = o * xUnit + xUnit * 0.15f
            val yOffset = height - barHeight

            val color = if (hasWkt) Color(0xFFD32F2F) else Color(0xFF1A5C2E)

            drawRect(
                color = color,
                topLeft = Offset(xOffset, yOffset),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )

            // Draw over number below
            if (o % 2 == 0) {
                // simple numbers inside canvas is skipped to avoid text measurements, or drawn as clean accents
            }
        }
    }
}

@Composable
fun RunRateProgressionCanvas(
    firstInningsBalls: List<Ball>,
    secondInningsBalls: List<Ball>,
    totalOvers: Int
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
    ) {
        val width = size.width
        val height = size.height

        // First Innings cumulative curves
        var runningInn1 = 0
        val inn1Points = mutableListOf<Offset>()
        for (o in 0..totalOvers) {
            val overBalls = firstInningsBalls.filter { it.overNumber == o - 1 }
            if (o > 0 && overBalls.isNotEmpty()) {
                val runs = overBalls.sumOf { it.runs + it.extras }
                runningInn1 += runs
            }
            if (o == 0 || overBalls.isNotEmpty()) {
                inn1Points.add(Offset(o.toFloat(), runningInn1.toFloat()))
            }
        }

        // Second Innings cumulative curves
        var runningInn2 = 0
        val inn2Points = mutableListOf<Offset>()
        for (o in 0..totalOvers) {
            val overBalls = secondInningsBalls.filter { it.overNumber == o - 1 }
            if (o > 0 && overBalls.isNotEmpty()) {
                val runs = overBalls.sumOf { it.runs + it.extras }
                runningInn2 += runs
            }
            if (o == 0 || overBalls.isNotEmpty()) {
                inn2Points.add(Offset(o.toFloat(), runningInn2.toFloat()))
            }
        }

        val targetRuns = runningInn1 + 1
        val maxRunsPlot = targetRuns.coerceAtLeast(runningInn2).coerceAtLeast(20)

        val xUnit = width / totalOvers
        val yUnit = height / maxRunsPlot

        // Draw horizontal grid lines
        for (i in 1..4) {
            val y = height - (height * i / 4f)
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(0f, y),
                end = Offset(width, y)
            )
        }

        // Draw first innings progress line (Forest Green)
        for (i in 0 until inn1Points.size - 1) {
            val p1Obj = inn1Points[i]
            val p2Obj = inn1Points[i + 1]
            drawLine(
                color = Color(0xFF1A5C2E),
                start = Offset(p1Obj.x * xUnit, height - (p1Obj.y * yUnit)),
                end = Offset(p2Obj.x * xUnit, height - (p2Obj.y * yUnit)),
                strokeWidth = 4f
            )
        }

        // Draw second innings progress line (Gold)
        for (i in 0 until inn2Points.size - 1) {
            val p1Obj = inn2Points[i]
            val p2Obj = inn2Points[i + 1]
            drawLine(
                color = Color(0xFFD4A017),
                start = Offset(p1Obj.x * xUnit, height - (p1Obj.y * yUnit)),
                end = Offset(p2Obj.x * xUnit, height - (p2Obj.y * yUnit)),
                strokeWidth = 5f
            )
        }

        // Draw required rate target line (Dashed Gold dot)
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        drawLine(
            color = Color(0xFFD4A017),
            start = Offset(0f, height - (targetRuns * yUnit)),
            end = Offset(width, height - (targetRuns * yUnit)),
            strokeWidth = 2f,
            pathEffect = pathEffect
        )
    }
}

// FORMAT PLAIN TEXT SCORECARD GENERATOR
fun compileScorecardText(
    match: Match,
    team1: Team?,
    team2: Team?,
    inningsList: List<Innings>,
    players: List<Player>
): String {
    val builder = StringBuilder()
    builder.append("🏏 *SAMARBAGH JUNIOR LEAGUE (SJL)* 🏆\n")
    builder.append("-----------------------------\n")
    builder.append("📍 *Venue:* ${match.venue}\n")
    builder.append("📅 *Date:* ${match.date}\n")
    builder.append("⚔️ *Match:* ${team1?.name} vs ${team2?.name}\n")
    builder.append("✨ *Winner Statement:* ${match.resultDescription}\n")
    builder.append("-----------------------------\n\n")

    for (innings in inningsList) {
        val battingTeam = if (innings.battingTeamId == team1?.id) team1 else team2
        builder.append("🟢 *Batting:* ${battingTeam?.name}\n")
        builder.append("👉 *Total Runs:* ${innings.totalRuns}/${innings.wickets} (${innings.oversBowled} ov)\n")
        builder.append("🎁 *Extras:* ${innings.extras} (Wd:${innings.wides} Nb:${innings.noBalls})\n")
        builder.append("-----------------------------\n")
    }

    builder.append("\n_Created offline via Samarbagh Junior League scorer app_ ⛰️")
    return builder.toString()
}

private fun getBallTextForScorecard(ball: Ball): String {
    return if (ball.isWicket) "W" else {
        when (ball.extraType) {
            "wide" -> if (ball.extras > 1) "${ball.extras - 1}+Wd" else "Wd"
            "noball" -> if (ball.runs > 0) "${ball.runs}+Nb" else "Nb"
            "bye" -> if (ball.extras > 0) "${ball.extras}B" else "B"
            "legbye" -> if (ball.extras > 0) "${ball.extras}Lb" else "Lb"
            else -> ball.runs.toString()
        }
    }
}

private fun getBallColorForScorecard(ball: Ball): Color {
    return if (ball.isWicket) Color(0xFFD32F2F) else {
        when (ball.extraType) {
            "wide", "noball" -> Color(0xFF1976D2)
            "bye", "legbye" -> Color(0xFF7B1FA2)
            else -> {
                when (ball.runs) {
                    4 -> Color(0xFFD4A017)
                    6 -> Color(0xFF6A1B9A) // elegant distinct deep purple / gold for sixes!
                    0 -> Color.Gray
                    else -> Color(0xFF4CAF50)
                }
            }
        }
    }
}
