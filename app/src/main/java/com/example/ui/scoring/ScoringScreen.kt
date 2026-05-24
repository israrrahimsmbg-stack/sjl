package com.example.ui.scoring

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Ball
import com.example.data.model.Innings
import com.example.data.model.Match
import com.example.data.model.Player
import com.example.data.model.Team
import com.example.ui.components.BatIcon
import com.example.ui.components.BallIcon
import com.example.viewmodel.ScoringViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoringScreen(
    viewModel: ScoringViewModel,
    onNavigateHome: () -> Unit,
    onNavigateToScorecard: (Int) -> Unit
) {
    val liveMatch by viewModel.liveMatch.collectAsState()
    val activeInnings by viewModel.activeInnings.collectAsState()
    val firstInnings by viewModel.firstInnings.collectAsState()
    val balls by viewModel.balls.collectAsState()

    val teams by viewModel.allTeams.collectAsState(initial = emptyList())
    val team1Squad by viewModel.team1Squad.collectAsState()
    val team2Squad by viewModel.team2Squad.collectAsState()

    val selectedT1XI by viewModel.selectedTeam1XIIds.collectAsState()
    val selectedT2XI by viewModel.selectedTeam2XIIds.collectAsState()

    val strikerId by viewModel.strikerId.collectAsState()
    val nonStrikerId by viewModel.nonStrikerId.collectAsState()
    val currentBowlerId by viewModel.currentBowlerId.collectAsState()

    val showWicketDialog by viewModel.showWicketDialog.collectAsState()
    val showBowlerSelectDialog by viewModel.showBowlerSelectDialog.collectAsState()

    var showSetup by remember { mutableStateOf(liveMatch == null) }

    LaunchedEffect(liveMatch) {
         showSetup = liveMatch == null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SJL Cricket Scorer", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1F13)),
                navigationIcon = {
                    IconButton(onClick = onNavigateHome) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (liveMatch != null) {
                        IconButton(onClick = { viewModel.clearLiveMatch() }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Match", tint = Color.White)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF4F9F5))
        ) {
            if (showSetup) {
                // MATCH SETUP PANEL (STEP 1)
                MatchSetupWizard(
                    teams = teams,
                    team1Squad = team1Squad,
                    team2Squad = team2Squad,
                    selectedT1XI = selectedT1XI,
                    selectedT2XI = selectedT2XI,
                    onSquadSelected = { t1, t2 -> viewModel.onSquadSelected(t1, t2) },
                    togglePlayerSelection = { id, tNum -> viewModel.togglePlayerSelection(id, tNum) },
                    onStartMatch = { t1, t2, overs, venue, tossWin, tossChoice, striker, nonStriker, bowler ->
                        viewModel.startMatch(t1, t2, overs, venue, tossWin, tossChoice, striker, nonStriker, bowler)
                    }
                )
            } else {
                val match = liveMatch!!
                val innings = activeInnings

                if (match.status == "complete") {
                    // MATCH COMPLETE SCREEN
                    MatchCompletedScreen(
                        match = match,
                        teams = teams,
                        onViewScorecard = { onNavigateToScorecard(match.id) },
                        onClear = {
                            viewModel.clearLiveMatch()
                            onNavigateHome()
                        }
                    )
                } else if (innings != null) {
                    // SCORING INTERFACE (STEP 2)
                    ScoringInterface(
                        match = match,
                        innings = innings,
                        firstInnings = firstInnings,
                        balls = balls,
                        teams = teams,
                        team1Squad = team1Squad,
                        team2Squad = team2Squad,
                        selectedT1XI = selectedT1XI,
                        selectedT2XI = selectedT2XI,
                        strikerId = strikerId,
                        nonStrikerId = nonStrikerId,
                        currentBowlerId = currentBowlerId,
                        onBallEntered = { runs, extraType -> viewModel.recordBall(runs, extraType) },
                        onUndo = { viewModel.undoLastBall() },
                        onWicketClicked = { viewModel.openWicketDialog() },
                        onSwapCrease = { viewModel.swapBatsmen() },
                        onResetSession = { viewModel.clearLiveMatch() },
                        onEndMatchEarly = { viewModel.completeMatchExplicitly() },
                        onBowlerClicked = { viewModel.openBowlerSelectDialog() }
                    )
                }
            }

            // Wicket dismissals dialog
            if (showWicketDialog) {
                val innings = activeInnings
                if (innings != null) {
                    val battingSquad = if (innings.battingTeamId == liveMatch?.team1Id) team1Squad else team2Squad
                    val battingXI = battingSquad.filter {
                        val xiIds = if (innings.battingTeamId == liveMatch?.team1Id) selectedT1XI else selectedT2XI
                        xiIds.contains(it.id)
                    }
                    val currentOutId = strikerId ?: 0

                    WicketBottomSheetDialog(
                        battingTeamId = innings.battingTeamId,
                        battingXI = battingXI,
                        outBatsmanId = currentOutId,
                        balls = balls,
                        onConfirm = { type, fielder, nextBatsmanId ->
                            viewModel.recordWicket(type, fielder, nextBatsmanId)
                        },
                        onDismiss = { viewModel.closeWicketDialog() }
                    )
                }
            }

            // Next Over Bowler dialog
            if (showBowlerSelectDialog) {
                val innings = activeInnings
                if (innings != null) {
                    val bowlingSquad = if (innings.bowlingTeamId == liveMatch?.team1Id) team1Squad else team2Squad
                    val bowlingXI = bowlingSquad.filter {
                        val xiIds = if (innings.bowlingTeamId == liveMatch?.team1Id) selectedT1XI else selectedT2XI
                        xiIds.contains(it.id)
                    }

                    BowlerPickerDialog(
                        bowlingXI = bowlingXI,
                        currentBowlerId = currentBowlerId ?: 0,
                        onSelect = { id -> viewModel.selectBowler(id) },
                        onDismiss = { viewModel.closeBowlerSelectDialog() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSetupWizard(
    teams: List<Team>,
    team1Squad: List<Player>,
    team2Squad: List<Player>,
    selectedT1XI: Set<Int>,
    selectedT2XI: Set<Int>,
    onSquadSelected: (Int, Int) -> Unit,
    togglePlayerSelection: (Int, Int) -> Unit,
    onStartMatch: (Int, Int, Int, String, Int, String, Int, Int, Int) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }

    var selectedT1Id by remember { mutableIntStateOf(0) }
    var selectedT2Id by remember { mutableIntStateOf(0) }
    var totalOvers by remember { mutableStateOf("15") }
    var venue by remember { mutableStateOf("Samarbagh Ground") }
    var tossWinnerId by remember { mutableIntStateOf(0) }
    var tossChoice by remember { mutableStateOf("bat") } // "bat" or "bowl"

    // Opening Crease choices
    var strikerId by remember { mutableIntStateOf(0) }
    var nonStrikerId by remember { mutableIntStateOf(0) }
    var openingBowlerId by remember { mutableIntStateOf(0) }

    // Dropdowns expanded states
    var t1Expanded by remember { mutableStateOf(false) }
    var t2Expanded by remember { mutableStateOf(false) }

    // Auto-select first two teams when teams database list loaded
    LaunchedEffect(teams) {
        if (teams.isNotEmpty()) {
            if (selectedT1Id == 0) selectedT1Id = teams.getOrNull(0)?.id ?: 0
            if (selectedT2Id == 0) selectedT2Id = teams.getOrNull(1)?.id ?: 0
        }
    }

    // Re-trigger squad load
    LaunchedEffect(selectedT1Id, selectedT2Id) {
        onSquadSelected(selectedT1Id, selectedT2Id)
        if (selectedT1Id != 0) {
            tossWinnerId = selectedT1Id
        }
    }

    LaunchedEffect(team1Squad, team2Squad) {
        strikerId = team1Squad.getOrNull(0)?.id ?: 0
        nonStrikerId = team1Squad.getOrNull(1)?.id ?: 0
        openingBowlerId = team2Squad.getOrNull(0)?.id ?: 0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "⚡ SJL TOURNAMENT SETUP (STEP $step/3)",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSystemInDarkTheme()) Color(0xFFD4A017) else Color(0xFF1A5C2E)
            )
        }

        if (step == 1) {
            // STEP 1: TEAMS & OVERS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Teams Selection", 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Team 1 dropdown
                        ExposedDropdownMenuBox(
                            expanded = t1Expanded,
                            onExpandedChange = { t1Expanded = !t1Expanded }
                        ) {
                            val activeTeam = teams.find { it.id == selectedT1Id }
                            TextField(
                                readOnly = true,
                                value = activeTeam?.name ?: "Select Batting Team",
                                onValueChange = {},
                                label = { Text("Batting Team (Team 1)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = t1Expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = t1Expanded,
                                onDismissRequest = { t1Expanded = false }
                            ) {
                                teams.forEach { team ->
                                    DropdownMenuItem(
                                        text = { Text(team.name) },
                                        onClick = {
                                            selectedT1Id = team.id
                                            t1Expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Team 2 dropdown
                        ExposedDropdownMenuBox(
                            expanded = t2Expanded,
                            onExpandedChange = { t2Expanded = !t2Expanded }
                        ) {
                            val activeTeam = teams.find { it.id == selectedT2Id }
                            TextField(
                                readOnly = true,
                                value = activeTeam?.name ?: "Select Bowling Team",
                                onValueChange = {},
                                label = { Text("Bowling Team (Team 2)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = t2Expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = t2Expanded,
                                onDismissRequest = { t2Expanded = false }
                            ) {
                                teams.forEach { team ->
                                    DropdownMenuItem(
                                        text = { Text(team.name) },
                                        onClick = {
                                            selectedT2Id = team.id
                                            t2Expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = totalOvers,
                            onValueChange = { totalOvers = it.filter { c -> c.isDigit() } },
                            label = { Text("Overs per Innings", fontWeight = FontWeight.Bold) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = if (isSystemInDarkTheme()) Color(0xFFD4A017) else Color(0xFF0D5E35),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = venue,
                            onValueChange = { venue = it },
                            label = { Text("Venue Location", fontWeight = FontWeight.Bold) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = if (isSystemInDarkTheme()) Color(0xFFD4A017) else Color(0xFF0D5E35),
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Toss Decision", 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Toss Winner:", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                            Row {
                                val t1 = teams.find { it.id == selectedT1Id }
                                val t2 = teams.find { it.id == selectedT2Id }
                                FilterChip(
                                    selected = tossWinnerId == selectedT1Id,
                                    onClick = { tossWinnerId = selectedT1Id },
                                    label = { Text(t1?.shortName ?: "T1") }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = tossWinnerId == selectedT2Id,
                                    onClick = { tossWinnerId = selectedT2Id },
                                    label = { Text(t2?.shortName ?: "T2") }
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Toss Choice:", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                            Row {
                                FilterChip(
                                    selected = tossChoice == "bat",
                                    onClick = { tossChoice = "bat" },
                                    label = { Text("BAT FIRST") }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = tossChoice == "bowl",
                                    onClick = { tossChoice = "bowl" },
                                    label = { Text("BOWL FIRST") }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { step = 2 },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isSystemInDarkTheme()) Color(0xFFD4A017) else Color(0xFF1A5C2E))
                ) {
                    Text("Select Playing Squads Selection ➔", color = if (isSystemInDarkTheme()) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (step == 2) {
            // STEP 2: PLAYING XI SELECTION (PICK 11)
            item {
                val t1Obj = teams.find { it.id == selectedT1Id }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${t1Obj?.name ?: "Team A"} (Playing XI) (Selected: ${selectedT1XI.size}/11)",
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSystemInDarkTheme()) Color(0xFF6EE7B7) else Color(0xFF1A5C2E),
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val filteredT1 = team1Squad.filter { it.specialRole != "Coach" && it.specialRole != "Support Staff" }
                        filteredT1.forEach { player ->
                            val isSelected = selectedT1XI.contains(player.id)
                            val cardBg = if (isSelected) {
                                if (isSystemInDarkTheme()) Color(0xFF0F2D1A) else Color(0xFFE8F5E9)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            }
                            val cardBorder = if (isSelected) {
                                if (isSystemInDarkTheme()) Color(0xFF34D399) else Color(0xFF2E7D32)
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { togglePlayerSelection(player.id, 1) },
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, cardBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    color = if (isSelected) {
                                                        if (isSystemInDarkTheme()) Color(0xFF34D399) else Color(0xFF2E7D32)
                                                    } else {
                                                        Color.LightGray.copy(alpha = 0.3f)
                                                    },
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "#${player.jerseyNumber}",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                color = if (isSelected) Color.White else Color.DarkGray
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "${player.name} ${if (player.isCaptain) "(C)" else ""} ${if (player.isWicketKeeper) "(WK)" else ""}",
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (isSelected) {
                                        Text(
                                            text = "🏏 SELECTED",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            color = if (isSystemInDarkTheme()) Color(0xFF34D399) else Color(0xFF2E7D32)
                                        )
                                    } else {
                                        Text(
                                            text = "+ OMT",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                val t2Obj = teams.find { it.id == selectedT2Id }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${t2Obj?.name ?: "Team B"} (Playing XI) (Selected: ${selectedT2XI.size}/11)",
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSystemInDarkTheme()) Color(0xFF6EE7B7) else Color(0xFF1A5C2E),
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val filteredT2 = team2Squad.filter { it.specialRole != "Coach" && it.specialRole != "Support Staff" }
                        filteredT2.forEach { player ->
                            val isSelected = selectedT2XI.contains(player.id)
                            val cardBg = if (isSelected) {
                                if (isSystemInDarkTheme()) Color(0xFF0F2D1A) else Color(0xFFE8F5E9)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            }
                            val cardBorder = if (isSelected) {
                                if (isSystemInDarkTheme()) Color(0xFF34D399) else Color(0xFF2E7D32)
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { togglePlayerSelection(player.id, 2) },
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                border = BorderStroke(1.dp, cardBorder),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(
                                                    color = if (isSelected) {
                                                        if (isSystemInDarkTheme()) Color(0xFF34D399) else Color(0xFF2E7D32)
                                                    } else {
                                                        Color.LightGray.copy(alpha = 0.3f)
                                                    },
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "#${player.jerseyNumber}",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                color = if (isSelected) Color.White else Color.DarkGray
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "${player.name} ${if (player.isCaptain) "(C)" else ""} ${if (player.isWicketKeeper) "(WK)" else ""}",
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (isSelected) {
                                        Text(
                                            text = "🏏 SELECTED",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 10.sp,
                                            color = if (isSystemInDarkTheme()) Color(0xFF34D399) else Color(0xFF2E7D32)
                                        )
                                    } else {
                                        Text(
                                            text = "+ OMT",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { step = 1 },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back", color = if (isSystemInDarkTheme()) Color.White else Color(0xFF1A5C2E))
                    }
                    Button(
                        onClick = { step = 3 },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSystemInDarkTheme()) Color(0xFFD4A017) else Color(0xFF1A5C2E))
                    ) {
                        Text("Opening Lineup ➔", color = if (isSystemInDarkTheme()) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (step == 3) {
            // STEP 3: CREASE SELECT
            val firstBattingTeamId = if (tossChoice == "bat") tossWinnerId else {
                if (tossWinnerId == selectedT1Id) selectedT2Id else selectedT1Id
            }
            val firstBowlingTeamId = if (firstBattingTeamId == selectedT1Id) selectedT2Id else selectedT1Id

            val battingXI = if (firstBattingTeamId == selectedT1Id) team1Squad.filter { selectedT1XI.contains(it.id) }
                            else team2Squad.filter { selectedT2XI.contains(it.id) }

            val bowlingXI = if (firstBowlingTeamId == selectedT1Id) team1Squad.filter { selectedT1XI.contains(it.id) }
                            else team2Squad.filter { selectedT2XI.contains(it.id) }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Crease Selections", 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Striker Choose
                        var b1Expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = b1Expanded,
                            onExpandedChange = { b1Expanded = !b1Expanded }
                        ) {
                            val active = battingXI.find { it.id == strikerId }
                            TextField(
                                readOnly = true,
                                value = active?.name ?: "Choose Opening Striker",
                                onValueChange = {},
                                label = { Text("Striker *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = b1Expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = b1Expanded,
                                onDismissRequest = { b1Expanded = false }
                            ) {
                                battingXI.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("#${p.jerseyNumber} ${p.name}") },
                                        onClick = {
                                            strikerId = p.id
                                            b1Expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Non-striker choose
                        var b2Expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = b2Expanded,
                            onExpandedChange = { b2Expanded = !b2Expanded }
                        ) {
                            val active = battingXI.find { it.id == nonStrikerId }
                            TextField(
                                readOnly = true,
                                value = active?.name ?: "Choose Opening Non-Striker",
                                onValueChange = {},
                                label = { Text("Non-Striker") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = b2Expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = b2Expanded,
                                onDismissRequest = { b2Expanded = false }
                            ) {
                                battingXI.filter { it.id != strikerId }.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("#${p.jerseyNumber} ${p.name}") },
                                        onClick = {
                                            nonStrikerId = p.id
                                            b2Expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Opening Bowler choose
                        var bowlExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = bowlExpanded,
                            onExpandedChange = { bowlExpanded = !bowlExpanded }
                        ) {
                            val active = bowlingXI.find { it.id == openingBowlerId }
                            TextField(
                                readOnly = true,
                                value = active?.name ?: "Choose Opening Bowler",
                                onValueChange = {},
                                label = { Text("Opening Bowler") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bowlExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = TextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = bowlExpanded,
                                onDismissRequest = { bowlExpanded = false }
                            ) {
                                bowlingXI.forEach { p ->
                                    DropdownMenuItem(
                                        text = { Text("#${p.jerseyNumber} ${p.name}") },
                                        onClick = {
                                            openingBowlerId = p.id
                                            bowlExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { step = 2 },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back", color = if (isSystemInDarkTheme()) Color.White else Color(0xFF1A5C2E))
                    }
                    Button(
                        onClick = {
                            val ov = totalOvers.toIntOrNull() ?: 15
                            onStartMatch(
                                selectedT1Id, selectedT2Id, ov, venue, tossWinnerId, tossChoice,
                                strikerId, nonStrikerId, openingBowlerId
                            )
                        },
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isSystemInDarkTheme()) Color(0xFFD4A017) else Color(0xFF1A5C2E))
                    ) {
                        Text("START MATCH 🏏", color = if (isSystemInDarkTheme()) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ScoringInterface(
    match: Match,
    innings: Innings,
    firstInnings: Innings?,
    balls: List<Ball>,
    teams: List<Team>,
    team1Squad: List<Player>,
    team2Squad: List<Player>,
    selectedT1XI: Set<Int>,
    selectedT2XI: Set<Int>,
    strikerId: Int?,
    nonStrikerId: Int?,
    currentBowlerId: Int?,
    onBallEntered: (Int, String) -> Unit,
    onUndo: () -> Unit,
    onWicketClicked: () -> Unit,
    onSwapCrease: () -> Unit,
    onResetSession: () -> Unit,
    onEndMatchEarly: () -> Unit,
    onBowlerClicked: () -> Unit
) {
    val battingTeam = teams.find { it.id == innings.battingTeamId }
    val bowlingTeam = teams.find { it.id == innings.bowlingTeamId }

    var showWideDialog by remember { mutableStateOf(false) }
    var showNoBallDialog by remember { mutableStateOf(false) }
    var showByeDialog by remember { mutableStateOf(false) }
    var showLegByeDialog by remember { mutableStateOf(false) }

    val battingSquad = if (innings.battingTeamId == match.team1Id) team1Squad else team2Squad
    val bowlingSquad = if (innings.bowlingTeamId == match.team1Id) team1Squad else team2Squad

    val strikerObj = battingSquad.find { it.id == strikerId }
    val nonStrikerObj = battingSquad.find { it.id == nonStrikerId }
    val bowlerObj = bowlingSquad.find { it.id == currentBowlerId }

    // Aggregate striker and non-striker score on-the-fly for real-time validation
    val strikerBalls = balls.filter { it.batsmanId == strikerId }
    val strikerRuns = strikerBalls.sumOf { it.runs }
    val strikerBallsFaced = strikerBalls.count { it.extraType != "wide" }

    val nonStrikerBalls = balls.filter { it.batsmanId == nonStrikerId }
    val nonStrikerRuns = nonStrikerBalls.sumOf { it.runs }
    val nonStrikerBallsFaced = nonStrikerBalls.count { it.extraType != "wide" }

    // Bowler over figures compilation in real-time
    val bowlerBallsList = balls.filter { it.bowlerId == currentBowlerId }
    val bowlerRunsConceded = bowlerBallsList.sumOf {
        if (it.extraType == "wide" || it.extraType == "noball") it.runs + it.extras else it.runs
    }
    val bowlerWickets = bowlerBallsList.count { it.isWicket && it.wicketType != "runout" }
    val bowlerLegalBalls = bowlerBallsList.count { it.isLegalDelivery }
    val bowlerOvers = "${bowlerLegalBalls / 6}.${bowlerLegalBalls % 6}"

    val totalInningsLegalBalls = balls.count { it.isLegalDelivery }

    val crr = if (innings.oversBowled > 0.0f) {
        if (totalInningsLegalBalls > 0) (innings.totalRuns.toFloat() / totalInningsLegalBalls) * 6 else 0f
    } else 0f

    val isSecondInnings = innings.inningsNumber == 2
    val target = (firstInnings?.totalRuns ?: 0) + 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. HERO CARD (SCOREBOARD DISPLAY)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1F13)),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = battingTeam?.name?.uppercase(Locale.US) ?: "BATTING",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFD4A017), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "INN ${innings.inningsNumber}",
                            color = Color(0xFF0D1F13),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = bowlingTeam?.name?.uppercase(Locale.US) ?: "BOWLING",
                        color = Color.White.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${innings.totalRuns} / ${innings.wickets}",
                    color = Color(0xFFD4A017),
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )

                Text(
                    text = "OVERS BOWLED: ${innings.oversBowled} / ${match.totalOvers}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "CRR: %.2f", crr),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isSecondInnings) {
                        Spacer(modifier = Modifier.width(10.dp))
                        val remainingBalls = (match.totalOvers * 6) - totalInningsLegalBalls
                        val runsNeeded = target - innings.totalRuns
                        val rrr = if (remainingBalls > 0) (runsNeeded.toFloat() / remainingBalls) * 6 else 0f

                        Box(
                            modifier = Modifier
                                .background(Color(0xFFD4A017).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFD4A017), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "REQ: %.2f", rrr),
                                color = Color(0xFFD4A017),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                if (isSecondInnings) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val ballsLeft = (match.totalOvers * 6) - totalInningsLegalBalls
                    Text(
                        text = "TARGET: $target | NEED: ${target - innings.totalRuns} RUNS OFF $ballsLeft BALLS",
                        color = Color(0xFFD4A017),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 2. LAST 6 BALLS SCROLL ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val currentOverLegalBowled = totalInningsLegalBalls % 6
            Text(
                text = "THIS OVER ($currentOverLegalBowled/6 legal): ",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.width(6.dp))
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Get balls of the current over based on overall innings progress, keeping the finished over on screen until the first ball of the new over is entered
                val currentOverNum = if (totalInningsLegalBalls > 0 && totalInningsLegalBalls % 6 == 0 && balls.none { it.overNumber == (totalInningsLegalBalls / 6) }) {
                    (totalInningsLegalBalls / 6) - 1
                } else {
                    totalInningsLegalBalls / 6
                }
                val overBalls = balls.filter { it.overNumber == currentOverNum }

                if (overBalls.isEmpty()) {
                    item { Text("Ready.", fontSize = 12.sp, color = Color.Gray) }
                } else {
                    items(overBalls) { ball ->
                        BallChip(ball)
                    }
                }
            }
        }

        // 3. BATSMEN AT CREASE CARD (GOLD ACCENT)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDF5)),
            border = BorderStroke(1.dp, Color(0xFFD4A017).copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Striker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSwapCrease() }
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BatIcon(modifier = Modifier.size(16.dp), color = Color(0xFFA16207))
                        Text(
                            text = "${strikerObj?.name ?: "Striker"}*",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0D1F13),
                            fontSize = 15.sp
                        )
                    }
                    val sr = if (strikerBallsFaced > 0) (strikerRuns.toFloat() / strikerBallsFaced) * 100f else 0f
                    Text(
                        text = "$strikerRuns ($strikerBallsFaced) SR: ${String.format(Locale.US, "%.1f", sr)}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A5C2E)
                    )
                }

                Divider(color = Color(0xFFD4A017).copy(alpha = 0.15f))

                // Non-Striker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BatIcon(modifier = Modifier.size(14.dp), color = Color(0xFFA16207).copy(alpha = 0.5f))
                        Text(
                            text = nonStrikerObj?.name ?: "Non-Striker",
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                    val srNon = if (nonStrikerBallsFaced > 0) (nonStrikerRuns.toFloat() / nonStrikerBallsFaced) * 100f else 0f
                    Text(
                        text = "$nonStrikerRuns ($nonStrikerBallsFaced) SR: ${String.format(Locale.US, "%.1f", srNon)}",
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                }
            }
        }

        // 4. CURRENT BOWLER CHIP (ELEGANTLY INTERACTIVE)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBowlerClicked() },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            border = BorderStroke(1.dp, Color(0xFF81C784)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BallIcon(modifier = Modifier.size(12.dp))
                    Column {
                        Text(
                            text = bowlerObj?.name ?: "Tap to Assign Bowler 🏏",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D1F13),
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Tap to change bowler at any time",
                            color = Color(0xFF186A3B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Overs: $bowlerOvers | Run: $bowlerRunsConceded | Wkt: $bowlerWickets",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A5C2E)
                )
            }
        }

        // EXTRAS SUMMARY BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXTRAS: Wd: ${innings.wides} | Nb: ${innings.noBalls} | B: ${innings.byes} | Lb: ${innings.legByes} | Total: ${innings.extras}",
                fontSize = 11.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Force End",
                color = Color.Red,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.clickable { onEndMatchEarly() }
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 5. GRID INPUT DECKS (Min 56dp height and color styled)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Rows 1: Runs 0, 1, 2, 3
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ButtonInput(text = "0", color = Color.Gray, modifier = Modifier.weight(1f)) { onBallEntered(0, "") }
                ButtonInput(text = "1", color = Color(0xFF81C784), modifier = Modifier.weight(1f)) { onBallEntered(1, "") }
                ButtonInput(text = "2", color = Color(0xFF4CAF50), modifier = Modifier.weight(1f)) { onBallEntered(2, "") }
                ButtonInput(text = "3", color = Color(0xFF2E7D32), modifier = Modifier.weight(1f)) { onBallEntered(3, "") }
            }

            // Row 2: Boundary 4, 5, 6 and WICKET
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ButtonInput(text = "4 🏏", color = Color(0xFFD4A017), modifier = Modifier.weight(1f)) { onBallEntered(4, "") }
                ButtonInput(text = "5 🏃", color = Color(0xFF1B5E20), modifier = Modifier.weight(0.8f)) { onBallEntered(5, "") }
                ButtonInput(text = "6 🚀", color = Color(0xFFD4A017), isBold = true, modifier = Modifier.weight(1f)) { onBallEntered(6, "") }
                ButtonInput(text = "WICKET ❌", color = Color.Red, modifier = Modifier.weight(1.3f)) { onWicketClicked() }
            }

            // Row 3: Extras wide, noball, bye, legbye
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ButtonInput(text = "Wd ➕", color = Color(0xFF1976D2), modifier = Modifier.weight(1f)) { showWideDialog = true }
                ButtonInput(text = "Nb ➕", color = Color(0xFF1976D2), modifier = Modifier.weight(1f)) { showNoBallDialog = true }
                ButtonInput(text = "Bye ➕", color = Color(0xFF7B1FA2), modifier = Modifier.weight(1f)) { showByeDialog = true }
                ButtonInput(text = "L-Bye ➕", color = Color(0xFF7B1FA2), modifier = Modifier.weight(1f)) { showLegByeDialog = true }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // UNDO BALL FAB
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            FilledIconButton(
                onClick = onUndo,
                modifier = Modifier.size(52.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFD4A017))
            ) {
                Text("↩", fontSize = 24.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        // --- EXTRAS DYNAMIC PICKERS (FULL & FUNCTIONAL SCORING ENHANCEMENT) ---
        if (showWideDialog) {
            Dialog(onDismissRequest = { showWideDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏏 WIDE BALL EXTRA", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF1976D2))
                        Text("A wide adds +1 extra. Select any additional runs scored (e.g. keeper miss, run):", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)

                        val choices = listOf(
                            "0 runs (1 Wd total)" to 0,
                            "1 run (2 Wds total)" to 1,
                            "2 runs (3 Wds total)" to 2,
                            "3 runs (4 Wds total)" to 3,
                            "4 runs (Boundary) (5 Wds total)" to 4
                        )

                        choices.forEach { (label, runs) ->
                            Button(
                                onClick = {
                                    onBallEntered(runs, "wide")
                                    showWideDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(onClick = { showWideDialog = false }) {
                            Text("CANCEL", color = Color.Gray)
                        }
                    }
                }
            }
        }

        if (showNoBallDialog) {
            Dialog(onDismissRequest = { showNoBallDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🚨 NO BALL EXTRA", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF1976D2))
                        Text("No ball adds +1 extra. Select runs scored off the bat:", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)

                        val choices = listOf(
                            "0 runs (1 Nb total)" to 0,
                            "1 run (2 runs total)" to 1,
                            "2 runs (3 runs total)" to 2,
                            "3 runs (4 runs total)" to 3,
                            "4 Runs (FOUR!)" to 4,
                            "6 Runs (SIXR!)" to 6
                        )

                        choices.forEach { (label, runs) ->
                            Button(
                                onClick = {
                                    onBallEntered(runs, "noball")
                                    showNoBallDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(onClick = { showNoBallDialog = false }) {
                            Text("CANCEL", color = Color.Gray)
                        }
                    }
                }
            }
        }

        if (showByeDialog) {
            Dialog(onDismissRequest = { showByeDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏷️ BYES LOGGER", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF7B1FA2))
                        Text("Select how many byes were run by the batsmen:", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)

                        (1..4).forEach { runs ->
                            Button(
                                onClick = {
                                    onBallEntered(runs, "bye")
                                    showByeDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("$runs ${if (runs == 1) "Bye" else "Byes"}", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(onClick = { showByeDialog = false }) {
                            Text("CANCEL", color = Color.Gray)
                        }
                    }
                }
            }
        }

        if (showLegByeDialog) {
            Dialog(onDismissRequest = { showLegByeDialog = false }) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏃 LEG BYES LOGGER", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF7B1FA2))
                        Text("Select how many leg byes were run by the batsmen:", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)

                        (1..4).forEach { runs ->
                            Button(
                                onClick = {
                                    onBallEntered(runs, "legbye")
                                    showLegByeDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("$runs ${if (runs == 1) "Leg Bye" else "Leg Byes"}", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(onClick = { showLegByeDialog = false }) {
                            Text("CANCEL", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ButtonInput(
    text: String,
    color: Color,
    isBold: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = if (isBold) FontWeight.Black else FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BallChip(ball: Ball) {
    val text = if (ball.isWicket) "W" else {
        when (ball.extraType) {
            "wide" -> if (ball.extras > 1) "${ball.extras - 1}+Wd" else "Wd"
            "noball" -> if (ball.runs > 0) "${ball.runs}+Nb" else "Nb"
            "bye" -> if (ball.extras > 0) "${ball.extras}B" else "B"
            "legbye" -> if (ball.extras > 0) "${ball.extras}Lb" else "Lb"
            else -> ball.runs.toString()
        }
    }

    val color = if (ball.isWicket) Color.Red else {
        when (ball.extraType) {
            "wide", "noball" -> Color(0xFF1976D2)
            "bye", "legbye" -> Color(0xFF7B1FA2)
            else -> {
                when (ball.runs) {
                    4, 6 -> Color(0xFFD4A017)
                    0 -> Color.Gray
                    else -> Color(0xFF4CAF50)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .height(28.dp)
            .widthIn(min = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            softWrap = false
        )
    }
}

// 6. WICKET REGISTRATION DIALOG (BOTTOM SHEET OR STANDARD DIALOG FOR RELIABILITY)
@Composable
fun WicketBottomSheetDialog(
    battingTeamId: Int,
    battingXI: List<Player>,
    outBatsmanId: Int,
    balls: List<Ball>,
    onConfirm: (String, String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var dismissalType by remember { mutableStateOf("bowled") }
    var fielderName by remember { mutableStateOf("") }
    var selectedNextId by remember { mutableIntStateOf(0) }

    // Deduce who has already batted in this innings to show remaining batsmen (DNB)
    val alreadyBattedIds = balls.map { it.batsmanId }.toSet()
    val availableNextBatsmen = battingXI.filter { !alreadyBattedIds.contains(it.id) && it.id != outBatsmanId }

    LaunchedEffect(availableNextBatsmen) {
         selectedNextId = availableNextBatsmen.firstOrNull()?.id ?: 0
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "❌ WICKET DETAILS LOGGER",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color.Red
                )

                // Wicket type radios
                Text("Dismissal Type:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                val typesList = listOf("bowled", "caught", "lbw", "runout", "stumped", "caughtbowled")
                Column {
                    typesList.chunked(2).forEach { rowTypes ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowTypes.forEach { type ->
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { dismissalType = type }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = dismissalType == type, onClick = { dismissalType = type })
                                    Text(type.uppercase(Locale.US), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (dismissalType == "caught" || dismissalType == "runout" || dismissalType == "stumped") {
                    OutlinedTextField(
                        value = fielderName,
                        onValueChange = { fielderName = it },
                        label = { Text("Fielder Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Next batsman Choose
                if (availableNextBatsmen.isNotEmpty()) {
                    Text("Select Next Batsman:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        val selectedName = availableNextBatsmen.find { it.id == selectedNextId }?.name ?: "Choose Batsman"
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedName, color = Color.Black)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            availableNextBatsmen.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("#${p.jerseyNumber} ${p.name}") },
                                    onClick = {
                                        selectedNextId = p.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text("Last Wicket! Batting team all-out.", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Button(
                        onClick = {
                            onConfirm(dismissalType, fielderName, selectedNextId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Confirm Out 🛑", color = Color.White)
                    }
                }
            }
        }
    }
}

// 7. BOWLER SELECT DIALOG
@Composable
fun BowlerPickerDialog(
    bowlingXI: List<Player>,
    currentBowlerId: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🏏 SELECT NEXT OVER BOWLER",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color(0xFF1A5C2E)
                )
                Text(
                    text = "Rule: Consecutive overs cannot be bowled by the same bowler.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    val candidateBowlers = bowlingXI.filter { it.id != currentBowlerId }
                    items(candidateBowlers) { p ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(p.id) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F9F5)),
                            border = BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("#${p.jerseyNumber} ${p.name}", fontWeight = FontWeight.Bold)
                                Text("Assign ➔", color = Color(0xFF1A5C2E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 8. MATCH COMPLETED ACTION SCREEN
@Composable
fun MatchCompletedScreen(
    match: Match,
    teams: List<Team>,
    onViewScorecard: () -> Unit,
    onClear: () -> Unit
) {
    val team1Obj = teams.find { it.id == match.team1Id }
    val team2Obj = teams.find { it.id == match.team2Id }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("🏆 MATCH COMPLETED", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFFD4A017))

                Text(
                    text = "${team1Obj?.name} vs ${team2Obj?.name}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = match.resultDescription,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A5C2E),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onViewScorecard,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A5C2E))
                ) {
                    Text("Go to Full Scorecard 📋", color = Color.White)
                }

                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Scoring Session 🔄", color = Color.Gray)
                }
            }
        }
    }
}
