package com.example.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SjlLogo
import com.example.data.model.Match
import com.example.viewmodel.PlayerViewModel
import com.example.viewmodel.ScoringViewModel
import com.example.viewmodel.TournamentViewModel

@Composable
fun HomeScreen(
    scoringViewModel: ScoringViewModel,
    tournamentViewModel: TournamentViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToScoring: () -> Unit,
    onNavigateToScorecard: (Int) -> Unit
) {
    val matches by tournamentViewModel.allMatches.collectAsState()
    val teams by tournamentViewModel.allTeams.collectAsState()
    val leaderboardRuns by playerViewModel.top10Runs.collectAsState()
    val leaderboardWickets by playerViewModel.top10Wickets.collectAsState()

    val liveMatch = matches.find { it.status == "live" }
    val upcomingMatches = matches.filter { it.status == "upcoming" }.take(3)

    // Calculate aggregate league metrics
    val totalPlayed = matches.count { it.status == "complete" }
    val bestRuns = if (leaderboardRuns.isNotEmpty()) leaderboardRuns.first().value else 0
    val bestWkts = if (leaderboardWickets.isNotEmpty()) leaderboardWickets.first().value else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // SJL Mountain Sports Header
        SJLHeader()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // LIVE Match Card
            if (liveMatch != null) {
                item {
                    val battingInnings by scoringViewModel.activeInnings.collectAsState()
                    val t1 = teams.find { it.id == liveMatch.team1Id }
                    val t2 = teams.find { it.id == liveMatch.team2Id }
                    val currentBattingTeam = teams.find { it.id == battingInnings?.battingTeamId }

                    Text(
                        text = "🔴 LIVE SCORES",
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LiveMatchCard(
                        match = liveMatch,
                        team1Name = t1?.name ?: "Team A",
                        team2Name = t2?.name ?: "Team B",
                        battingTeamShortName = currentBattingTeam?.shortName ?: "BAT",
                        runs = battingInnings?.totalRuns ?: 0,
                        wickets = battingInnings?.wickets ?: 0,
                        overs = battingInnings?.oversBowled ?: 0.0f,
                        onClick = onNavigateToScoring
                    )
                }
            }

            // Stat Cards Grid (League summary row)
            item {
                Column {
                    Text(
                        text = "LEAGUE SUMMARIES",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatChip(
                            label = "Played",
                            value = totalPlayed.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            label = "Max Runs",
                            value = bestRuns.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        StatChip(
                            label = "Max Wkts",
                            value = bestWkts.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Top Performers Gallery Scroll
            item {
                Column {
                    Text(
                        text = "👑 TOURNAMENT LEADERS",
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        letterSpacing = 1.2.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val topBatsman = leaderboardRuns.firstOrNull()
                        val topBowler = leaderboardWickets.firstOrNull()

                        LeaderCard(
                            title = "Orange Cap (Most Runs)",
                            name = topBatsman?.name ?: "Abid Khan",
                            team = topBatsman?.teamName ?: "SST",
                            score = "${topBatsman?.value ?: 0} Runs",
                            colorHex = topBatsman?.teamColor ?: "#1a5c2e"
                        )

                        LeaderCard(
                            title = "Purple Cap (Most Wkts)",
                            name = topBowler?.name ?: "Shaheen Afridi",
                            team = topBowler?.teamName ?: "JDJ",
                            score = "${topBowler?.value ?: 0} Wkts",
                            colorHex = topBowler?.teamColor ?: "#E25822"
                        )
                    }
                }
            }

            // Upcoming Fixtures
            item {
                Text(
                    text = "🏆 UPCOMING MATCHES",
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (upcomingMatches.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No upcoming matches scheduled.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(upcomingMatches) { match ->
                    val t1 = teams.find { it.id == match.team1Id }
                    val t2 = teams.find { it.id == match.team2Id }
                    FixtureCard(
                        match = match,
                        team1Name = t1?.name ?: "Team A",
                        team2Name = t2?.name ?: "Team B",
                        team1Short = t1?.shortName ?: "T1",
                        team1Color = t1?.colorHex ?: "#1a5c2e",
                        team2Short = t2?.shortName ?: "T2",
                        team2Color = t2?.colorHex ?: "#d4a017",
                        onClick = {
                            scoringViewModel.onSquadSelected(match.team1Id, match.team2Id)
                            onNavigateToScoring()
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SJLHeader() {
    val headerBg = Color(0xFF061C0F) // Premium near-black Deep Emerald Green
    val gold = Color(0xFFFBBF24) // Gold Accent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBg)
            .padding(top = 18.dp, bottom = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Official tournament crest logo
                SjlLogo(modifier = Modifier.size(54.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "SAMARBAGH JUNIOR LEAGUE",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "RISING FROM THE MOUNTAINS • SEASON 2",
                        color = gold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = gold, thickness = 2.dp, modifier = Modifier.fillMaxWidth(0.92f))
        }
    }
}

@Composable
fun SJLMinilogo() {
    Canvas(modifier = Modifier.size(34.dp)) {
        val width = size.width
        val height = size.height

        // Mountains path
        val mountPath = Path().apply {
            moveTo(0f, height * 0.8f)
            lineTo(width * 0.35f, height * 0.25f)
            lineTo(width * 0.6f, height * 0.6f)
            lineTo(width * 0.8f, height * 0.35f)
            lineTo(width, height * 0.8f)
            close()
        }
        drawPath(mountPath, color = Color(0xFF01411C)) // Pakistani Pantone Emerald Green

        // Golden Crescent Star
        drawCircle(
            color = Color(0xFFF6C344),
            radius = width * 0.14f,
            center = Offset(width * 0.8f, height * 0.22f)
        )
        // Subtracted circle to form crescent
        drawCircle(
            color = Color(0xFF061C0F),
            radius = width * 0.12f,
            center = Offset(width * 0.74f, height * 0.19f)
        )

        // Bat crossing
        drawLine(
            color = Color.White,
            start = Offset(width * 0.15f, height * 0.85f),
            end = Offset(width * 0.85f, height * 0.15f),
            strokeWidth = 3f
        )
    }
}

@Composable
fun LiveMatchCard(
    match: Match,
    team1Name: String,
    team2Name: String,
    battingTeamShortName: String,
    runs: Int,
    wickets: Int,
    overs: Float,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "livePulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF142C1B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE SCORING",
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = match.venue,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = team1Name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "vs",
                        color = Color(0xFFD4A017),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Thin
                    )
                    Text(
                        text = team2Name,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$battingTeamShortName $runs/$wickets",
                        color = Color(0xFFD4A017),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "($overs / ${match.totalOvers} ov)",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Divider(
                color = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Toss won by: ${if (match.tossWinnerId == match.team1Id) team1Name else team2Name} (${match.tossChoice})",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )

                Text(
                    text = "Tap to view Scorer ➔",
                    color = Color(0xFFD4A017),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1A5C2E)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun LeaderCard(
    title: String,
    name: String,
    team: String,
    score: String,
    colorHex: String
) {
    val teamColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        Color(0xFF1A5C2E)
    }

    Card(
        modifier = Modifier
            .width(220.dp)
            .height(105.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Team indicator sidebar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(teamColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = teamColor,
                    letterSpacing = 0.8.sp
                )

                Text(
                    text = name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D1F13),
                    maxLines = 1
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .background(teamColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = team,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = teamColor
                        )
                    }

                    Text(
                        text = score,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFD4A017)
                    )
                }
            }
        }
    }
}

@Composable
fun FixtureCard(
    match: Match,
    team1Name: String,
    team2Name: String,
    team1Short: String,
    team1Color: String,
    team2Short: String,
    team2Color: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.5.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = match.date,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = team1Name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1F13)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(team1Color))
                                } catch (e: Exception) {
                                    Color.Green
                                }
                            )
                    )
                }
                Text(
                    text = "vs",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Thin,
                    color = Color.LightGray,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = team2Name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D1F13)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                try {
                                    Color(android.graphics.Color.parseColor(team2Color))
                                } catch (e: Exception) {
                                    Color.Yellow
                                }
                            )
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${match.totalOvers} Overs",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A5C2E)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .background(Color(0xFF142C1B), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SCORE ➔",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFD4A017)
                    )
                }
            }
        }
    }
}
