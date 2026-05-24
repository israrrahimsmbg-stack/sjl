package com.example.ui.players

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Player
import com.example.data.model.Team
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.SJLGold
import com.example.ui.theme.DarkForestBg
import com.example.ui.theme.LightEmeraldBg
import com.example.ui.theme.BorderSlate
import com.example.ui.theme.DarkGrey
import com.example.ui.theme.LightGrey
import com.example.ui.components.BatIcon
import com.example.ui.components.BallIcon
import com.example.viewmodel.BattingStats
import com.example.viewmodel.BowlingStats
import com.example.viewmodel.PlayerMatchHistoryEntry
import com.example.viewmodel.PlayerViewModel
import java.util.Locale

val Player.role: String
    get() = if (specialRole != "Player" && specialRole.isNotBlank()) {
                specialRole
            } else if (isWicketKeeper) {
                "Wicketkeeper"
            } else if (id % 3 == 0) {
                "All-rounder"
            } else if (id % 2 == 0) {
                "Bowler"
            } else {
                "Batsman"
            }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayersScreen(
    playerViewModel: PlayerViewModel
) {
    val players by playerViewModel.allPlayers.collectAsState()
    val teams by playerViewModel.allTeams.collectAsState()

    var activeViewTab by remember { mutableIntStateOf(1) } // 1: Team rosters, 2: Global Search

    // Franchise Tab filter (defaults to first team)
    val firstTeamId = teams.firstOrNull()?.id ?: 0
    var selectedFranchiseId by remember(teams) { mutableIntStateOf(firstTeamId) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("all") } 

    // Detailed dialog selection state
    var selectedPlayerForProfile by remember { mutableStateOf<Player?>(null) }
    var selectedPlayerForEdit by remember { mutableStateOf<Player?>(null) }
    var showAddPlayerDialog by remember { mutableStateOf(false) }

    val filteredPlayers = players.filter { p ->
        val matchesName = p.name.contains(searchQuery, ignoreCase = true)
        val matchesRole = if (selectedRoleFilter == "all") {
            true
        } else {
            p.role.lowercase(Locale.US).replace("-", "").replace(" ", "") == 
                selectedRoleFilter.lowercase(Locale.US).replace("-", "").replace(" ", "")
        }
        matchesName && matchesRole
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddPlayerDialog = true },
                containerColor = ForestGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(12.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Member", tint = SJLGold) },
                text = { Text("ADD ROSTER MEMBER", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LightEmeraldBg)
        ) {
            // MAIN VIEW TABS (Sleek Segment Switcher)
            TabRow(
                selectedTabIndex = activeViewTab - 1,
                containerColor = DarkForestBg,
                contentColor = SJLGold
            ) {
                Tab(selected = activeViewTab == 1, onClick = { activeViewTab = 1 }) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("TEAM SQUADS 🛡️", color = if (activeViewTab == 1) SJLGold else Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
                Tab(selected = activeViewTab == 2, onClick = { activeViewTab = 2 }) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("GLOBAL REGISTRY 🔍", color = if (activeViewTab == 2) SJLGold else Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            }

            if (activeViewTab == 1) {
                // VIEW 1: TEAM ROSTERS VIEW (Organized franchise squads with cells and direct editable properties)
                Column(modifier = Modifier.fillMaxSize()) {
                    // Team badges row
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkForestBg.copy(alpha = 0.95f))
                            .padding(vertical = 12.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(teams) { team ->
                            val isSel = selectedFranchiseId == team.id
                            val teamColor = try {
                                Color(android.graphics.Color.parseColor(team.colorHex))
                            } catch (e: Exception) {
                                ForestGreen
                            }

                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSel) teamColor else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = if (isSel) 2.dp else 1.dp,
                                        color = if (isSel) SJLGold else Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedFranchiseId = team.id }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isSel) Color.White else teamColor)
                                    )
                                    Text(
                                        text = team.shortName,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    val currentTeam = teams.find { it.id == selectedFranchiseId }
                    val franchiseColor = try {
                        Color(android.graphics.Color.parseColor(currentTeam?.colorHex ?: "#0D5E35"))
                    } catch (e: Exception) {
                        ForestGreen
                    }

                    if (currentTeam != null) {
                        // Team Info Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(franchiseColor.copy(alpha = 0.12f))
                                .border(BorderStroke(1.dp, franchiseColor.copy(alpha = 0.2f)))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = currentTeam.name.uppercase(Locale.US),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = franchiseColor
                                    )
                                    Text(
                                        text = "📍 Home Arena: ${currentTeam.homeGround}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.DarkGray
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(franchiseColor, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    val count = players.count { it.teamId == currentTeam.id }
                                    Text(
                                        text = "$count TOTAL ROSTER",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // SQUAD ROSTER TABLE LISTING (With visible editable options)
                        val teamPlayers = players.filter { it.teamId == currentTeam.id }

                        if (teamPlayers.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No squad playing staff configured for this franchise.", color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(teamPlayers) { player ->
                                    val rName = player.role.lowercase(Locale.US)
                                    
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        border = BorderStroke(1.dp, BorderSlate),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 1. Jersey circle & Role badge/icon
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(franchiseColor.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                                    Text(
                                                        text = "#${player.jerseyNumber}",
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 13.sp,
                                                        color = franchiseColor
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // 2. Play name & designations
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Text(
                                                        text = player.name,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    if (player.isCaptain) {
                                                        Box(
                                                            modifier = Modifier
                                                                .background(SJLGold, CircleShape)
                                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                                        ) {
                                                            Text("C", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                                        }
                                                    }
                                                    if (player.isWicketKeeper) {
                                                        Box(
                                                            modifier = Modifier
                                                                .background(Color(0xFF3B82F6), CircleShape)
                                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                                        ) {
                                                            Text("WK", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(2.dp))

                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    // Proper batting/bowling icons based on roles
                                                    if (rName.contains("batsman") || rName.contains("rounder") || rName.contains("keeper")) {
                                                        BatIcon(modifier = Modifier.size(14.dp), color = Color(0xFFA16207))
                                                    }
                                                    if (rName.contains("bowler") || rName.contains("rounder")) {
                                                        BallIcon(modifier = Modifier.size(10.dp))
                                                    }
                                                    
                                                    Text(
                                                        text = player.role.uppercase(Locale.US),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = franchiseColor
                                                    )
                                                }
                                            }

                                            // 3. Cells actions (Tap to edit details, Tap to view profile)
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // View profile trigger
                                                IconButton(
                                                    onClick = { selectedPlayerForProfile = player },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Text("📊", fontSize = 16.sp)
                                                }

                                                // Direct Edit Cell (Sleek pencil edit dialogue trigger)
                                                Button(
                                                    onClick = { selectedPlayerForEdit = player },
                                                    colors = ButtonDefaults.buttonColors(containerColor = LightGrey),
                                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                                    modifier = Modifier.height(32.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Cell", tint = ForestGreen, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("EDIT", fontSize = 10.sp, fontWeight = FontWeight.Black, color = ForestGreen)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // VIEW 2: GLOBAL REGISTRY SEARCH & ROLES FILTER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkForestBg)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search Samarbagh / IPL cricketers...", color = Color.White.copy(alpha = 0.5f), fontSize = 14.sp) },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = SJLGold) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color.LightGray)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = SJLGold,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Role Filter row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val roles = listOf(
                                "all" to "ALL SQUAD", 
                                "batsman" to "BATSMEN 🏏", 
                                "bowler" to "BOWLERS ⚾", 
                                "allrounder" to "ALL-ROUNDERS 🔄", 
                                "wicketkeeper" to "KEEPERS 🧤",
                                "coach" to "HEAD COACHES 👔",
                                "support staff" to "SUPPORT STAFF 💼",
                                "concussion sub" to "CONCUSSION SUBS 🚑",
                                "impact player" to "IMPACT PLAYERS ⚡"
                            )
                            roles.forEach { (key, display) ->
                                val isSelected = selectedRoleFilter == key
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) SJLGold else Color.White.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedRoleFilter = key }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = display,
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredPlayers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No matching player or squad member found.", color = Color.Gray)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredPlayers) { player ->
                            val teamObj = teams.find { it.id == player.teamId }
                            val rName = player.role.lowercase(Locale.US)
                            val roleBadgeColor = when {
                                rName == "batsman" -> Color(0xFFE8F5E9)
                                rName == "bowler" -> Color(0xFFE3F2FD)
                                rName.contains("rounder") -> Color(0xFFFFF3E0)
                                rName.contains("keeper") -> Color(0xFFF3E5F5)
                                rName == "coach" -> Color(0xFFEFEBE9)
                                rName.contains("staff") -> Color(0xFFECEFF1)
                                rName.contains("concussion") -> Color(0xFFFFFDE7)
                                rName.contains("impact") -> Color(0xFFFBE9E7)
                                else -> Color(0xFFF5F5F5)
                            }
                            val roleTextColor = when {
                                rName == "batsman" -> Color(0xFF2E7D32)
                                rName == "bowler" -> Color(0xFF1565C0)
                                rName.contains("rounder") -> Color(0xFFE65100)
                                rName.contains("keeper") -> Color(0xFF6A1B9A)
                                rName == "coach" -> Color(0xFF4E342E)
                                rName.contains("staff") -> Color(0xFF37474F)
                                rName.contains("concussion") -> Color(0xFFF57F17)
                                rName.contains("impact") -> Color(0xFFD84315)
                                else -> Color(0xFF555555)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPlayerForProfile = player },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, BorderSlate),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE2E8F0)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "#${player.jerseyNumber}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color.Black
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (player.isCaptain) {
                                                Badge(containerColor = SJLGold) {
                                                    Text("C", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                                }
                                            }
                                            IconButton(
                                                onClick = { selectedPlayerForEdit = player },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Player", tint = Color.Gray, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (rName.contains("batsman") || rName.contains("rounder") || rValueIcon(rName)) {
                                            BatIcon(modifier = Modifier.size(12.dp), color = Color(0xFFA16207))
                                        }
                                        Text(
                                            text = player.name,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            color = DarkGrey,
                                            maxLines = 1
                                        )
                                    }

                                    val teamColor = try {
                                        Color(android.graphics.Color.parseColor(teamObj?.colorHex ?: "#0D5E35"))
                                    } catch (e: Exception) {
                                        Color(0xFF0D5E35)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(teamColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .border(1.dp, teamColor.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = teamObj?.name?.uppercase(Locale.US) ?: "SAMARBAGH CRICKET",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            color = teamColor,
                                            maxLines = 1
                                        )
                                    }

                                    // Role tag bubble
                                    Box(
                                        modifier = Modifier
                                            .background(roleBadgeColor, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = player.role.uppercase(Locale.US),
                                            color = roleTextColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- ADD ROSTER DIALOG FLOW ---
            if (showAddPlayerDialog) {
                Dialog(onDismissRequest = { showAddPlayerDialog = false }) {
                    var nameInput by remember { mutableStateOf("") }
                    var jerseyInput by remember { mutableStateOf("") }
                    var selectedTeamId by remember { mutableIntStateOf(teams.firstOrNull()?.id ?: 0) }
                    var selectedRoleInput by remember { mutableStateOf("Player") } 
                    var isCaptainInput by remember { mutableStateOf(false) }
                    var isWKInput by remember { mutableStateOf(false) }

                    val roleOptions = listOf("Player", "Coach", "Support Staff", "Concussion Sub", "Impact Player")

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
                                text = "➕ ADD ROSTER MEMBER (IPL STYLE)",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = ForestGreen
                            )

                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Enter Member Name", color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedBorderColor = ForestGreen
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = jerseyInput,
                                onValueChange = { jerseyInput = it.filter { ch -> ch.isDigit() }.take(3) },
                                label = { Text("Jersey Number (e.g. 7, 45, 18)", color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedBorderColor = ForestGreen
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Team Selector
                            Text("Select Team affiliation:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(teams) { team ->
                                    val isSel = selectedTeamId == team.id
                                    val teamColor = try {
                                        Color(android.graphics.Color.parseColor(team.colorHex))
                                    } catch (e: Exception) {
                                        ForestGreen
                                    }
                                    Card(
                                        modifier = Modifier.clickable { selectedTeamId = team.id },
                                        border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) teamColor else Color.LightGray),
                                        colors = CardDefaults.cardColors(containerColor = if (isSel) teamColor.copy(alpha = 0.15f) else Color.White)
                                    ) {
                                        Text(
                                            text = team.shortName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            color = if (isSel) teamColor else Color.Black
                                        )
                                    }
                                }
                            }

                            // Role Selector
                            Text("Select IPL Designation:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                roleOptions.forEach { opt ->
                                    val isSel = selectedRoleInput == opt
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (isSel) ForestGreen else Color(0xFFF1F5F9),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedRoleInput = opt }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = opt.uppercase(Locale.US),
                                            color = if (isSel) Color.White else Color.Black,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Captain & Keeper options
                            if (selectedRoleInput == "Player" || selectedRoleInput == "Impact Player" || selectedRoleInput == "Concussion Sub") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Checkbox(checked = isCaptainInput, onCheckedChange = { isCaptainInput = it })
                                        Text("Captain", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Checkbox(checked = isWKInput, onCheckedChange = { isWKInput = it })
                                        Text("Wicketkeeper", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showAddPlayerDialog = false }) {
                                    Text("CANCEL", color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (nameInput.isNotBlank()) {
                                            val jNo = jerseyInput.toIntOrNull() ?: 1
                                            playerViewModel.addPlayerToTeam(
                                                teamId = selectedTeamId,
                                                name = nameInput.trim(),
                                                jerseyNumber = jNo,
                                                isCaptain = isCaptainInput && (selectedRoleInput == "Player" || selectedRoleInput == "Impact Player" || selectedRoleInput == "Concussion Sub"),
                                                isWicketKeeper = isWKInput && (selectedRoleInput == "Player" || selectedRoleInput == "Impact Player" || selectedRoleInput == "Concussion Sub"),
                                                specialRole = selectedRoleInput
                                            )
                                            showAddPlayerDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                                ) {
                                    Text("SAVE", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // --- EDIT PLAYER CLINIC DIALOG (Cell Editing Solution) ---
            if (selectedPlayerForEdit != null) {
                val player = selectedPlayerForEdit!!
                Dialog(onDismissRequest = { selectedPlayerForEdit = null }) {
                    var editNameInput by remember(player) { mutableStateOf(player.name) }
                    var editJerseyInput by remember(player) { mutableStateOf(player.jerseyNumber.toString()) }
                    var editSelectedTeamId by remember(player) { mutableIntStateOf(player.teamId) }
                    var editSelectedRoleInput by remember(player) { mutableStateOf(player.specialRole.ifBlank { "Player" }) }
                    var editIsCaptainInput by remember(player) { mutableStateOf(player.isCaptain) }
                    var editIsWKInput by remember(player) { mutableStateOf(player.isWicketKeeper) }

                    val roleOptions = listOf("Player", "Coach", "Support Staff", "Concussion Sub", "Impact Player")

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
                                text = "✏️ EDIT PLAYER DETAILS",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = ForestGreen
                            )

                            OutlinedTextField(
                                value = editNameInput,
                                onValueChange = { editNameInput = it },
                                label = { Text("Player Name", color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedBorderColor = ForestGreen
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = editJerseyInput,
                                onValueChange = { editJerseyInput = it.filter { ch -> ch.isDigit() }.take(3) },
                                label = { Text("Jersey Number", color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedBorderColor = ForestGreen
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Team selector
                            Text("Team affiliation:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(teams) { team ->
                                    val isSel = editSelectedTeamId == team.id
                                    val teamColor = try {
                                        Color(android.graphics.Color.parseColor(team.colorHex))
                                    } catch (e: Exception) {
                                        ForestGreen
                                    }
                                    Card(
                                        modifier = Modifier.clickable { editSelectedTeamId = team.id },
                                        border = BorderStroke(if (isSel) 2.dp else 1.dp, if (isSel) teamColor else Color.LightGray),
                                        colors = CardDefaults.cardColors(containerColor = if (isSel) teamColor.copy(alpha = 0.15f) else Color.White)
                                    ) {
                                        Text(
                                            text = team.shortName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            color = if (isSel) teamColor else Color.Black
                                        )
                                    }
                                }
                            }

                            // Role designation
                            Text("Designation:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                roleOptions.forEach { opt ->
                                    val isSel = editSelectedRoleInput == opt
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (isSel) ForestGreen else Color(0xFFF1F5F9),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { editSelectedRoleInput = opt }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = opt.uppercase(Locale.US),
                                            color = if (isSel) Color.White else Color.Black,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            // Captain / Wicket keeper selectors
                            if (editSelectedRoleInput == "Player" || editSelectedRoleInput == "Impact Player" || editSelectedRoleInput == "Concussion Sub") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Checkbox(checked = editIsCaptainInput, onCheckedChange = { editIsCaptainInput = it })
                                        Text("Captain", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Checkbox(checked = editIsWKInput, onCheckedChange = { editIsWKInput = it })
                                        Text("Wicketkeeper", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { selectedPlayerForEdit = null }) {
                                    Text("CANCEL", color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (editNameInput.isNotBlank()) {
                                            playerViewModel.updatePlayer(
                                                playerId = player.id,
                                                teamId = editSelectedTeamId,
                                                name = editNameInput.trim(),
                                                jerseyNumber = editJerseyInput.toIntOrNull() ?: player.jerseyNumber,
                                                isCaptain = editIsCaptainInput && (editSelectedRoleInput == "Player" || editSelectedRoleInput == "Impact Player" || editSelectedRoleInput == "Concussion Sub"),
                                                isWicketKeeper = editIsWKInput && (editSelectedRoleInput == "Player" || editSelectedRoleInput == "Impact Player" || editSelectedRoleInput == "Concussion Sub"),
                                                specialRole = editSelectedRoleInput
                                            )
                                            selectedPlayerForEdit = null
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
                                ) {
                                    Text("SAVE", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // CAREER STATS PROFILE DIALOG
            if (selectedPlayerForProfile != null) {
                val player = selectedPlayerForProfile!!
                val teamObj = teams.find { it.id == player.teamId }

                val battingStats by playerViewModel.getPlayerBattingStats(player.id).collectAsState()
                val bowlingStats by playerViewModel.getPlayerBowlingStats(player.id).collectAsState()
                val historyTimeline by playerViewModel.getPlayerMatchHistory(player.id).collectAsState()

                PlayerProfileDialog(
                    player = player,
                    team = teamObj,
                    battingStats = battingStats,
                    bowlingStats = bowlingStats,
                    timeline = historyTimeline,
                    playerViewModel = playerViewModel,
                    onDismiss = { selectedPlayerForProfile = null }
                )
            }
        }
    }
}

// Helper to check batsman icon role
private fun rValueIcon(roleName: String): Boolean {
    return roleName.contains("batsman") || roleName.contains("rounder") || roleName.contains("keeper")
}

@Composable
fun PlayerProfileDialog(
    player: Player,
    team: Team?,
    battingStats: BattingStats,
    bowlingStats: BowlingStats,
    timeline: List<PlayerMatchHistoryEntry>,
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    var subTab by remember { mutableIntStateOf(1) } // 1: Stats, 2: History Timeline

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header colored block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkForestBg)
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = player.name.uppercase(Locale.US),
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SJLGold)
                            }
                        }
                        Text(
                            text = "${player.role.uppercase(Locale.US)} | JERSEY #${player.jerseyNumber} | ${team?.name ?: "SJL"}",
                            color = SJLGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                // Sub tab selector
                if (player.role != "Coach" && player.role != "Support Staff") {
                    TabRow(
                        selectedTabIndex = subTab - 1,
                        containerColor = LightEmeraldBg,
                        contentColor = ForestGreen
                    ) {
                        Tab(selected = subTab == 1, onClick = { subTab = 1 }) {
                            Text("CAREER STATS", color = if (subTab == 1) ForestGreen else Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp), fontSize = 12.sp)
                        }
                        Tab(selected = subTab == 2, onClick = { subTab = 2 }) {
                            Text("TIMELINE (${timeline.size})", color = if (subTab == 2) ForestGreen else Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(10.dp), fontSize = 12.sp)
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    if (player.role == "Coach" || player.role == "Support Staff") {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = LightEmeraldBg),
                                    border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "💼 OFFICIAL TEAM MANAGMENT",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            color = ForestGreen
                                        )

                                        Text(
                                            text = if (player.role == "Coach") {
                                                "Head Coach & Tactical Director responsible for overall training, matchday selections, strategy mapping, and tactical coordination."
                                            } else {
                                                "Essential support staff member responsible for Physiotherapy, Player conditioning, Data analysis, and Team logistics."
                                            },
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            color = Color.DarkGray
                                        )

                                        Divider(color = Color.LightGray.copy(alpha = 0.5f))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceAround
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("DESIGNATION", fontSize = 10.sp, color = Color.Gray)
                                                Text(player.role.uppercase(Locale.US), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ForestGreen)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("MEMBER ID", fontSize = 10.sp, color = Color.Gray)
                                                Text("#SJL-0${player.id}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (subTab == 1) {
                        // Career Tab
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Batting metrics
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BatIcon(modifier = Modifier.size(16.dp), color = Color(0xFFA16207))
                                    Text("BATTING STATISTICS", fontWeight = FontWeight.Black, fontSize = 12.sp, color = ForestGreen)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = LightEmeraldBg),
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column { Text("Matches:", fontSize = 11.sp, color = Color.Gray); Text(battingStats.matches.toString(), fontWeight = FontWeight.Bold, color = Color.Black) }
                                            Column { Text("Innings:", fontSize = 11.sp, color = Color.Gray); Text(battingStats.innings.toString(), fontWeight = FontWeight.Bold, color = Color.Black) }
                                            Column { Text("Total Runs:", fontSize = 11.sp, color = Color.Gray); Text(battingStats.runs.toString(), fontWeight = FontWeight.Bold, color = ForestGreen) }
                                            Column { Text("High Score:", fontSize = 11.sp, color = Color.Gray); Text(battingStats.highestScore.toString(), fontWeight = FontWeight.Bold, color = Color.Black) }
                                        }
                                        Divider(color = Color.LightGray.copy(alpha = 0.3f))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column { Text("Average:", fontSize = 11.sp, color = Color.Gray); Text(String.format(Locale.US, "%.2f", battingStats.average), fontWeight = FontWeight.Bold, color = Color.Black) }
                                            Column { Text("Strike Rate:", fontSize = 11.sp, color = Color.Gray); Text(String.format(Locale.US, "%.1f", battingStats.strikeRate), fontWeight = FontWeight.Bold, color = Color.Black) }
                                            Column { Text("Fours (4s):", fontSize = 11.sp, color = Color.Gray); Text(battingStats.fours.toString(), fontWeight = FontWeight.Bold, color = Color.Black) }
                                            Column { Text("Sixes (6s):", fontSize = 11.sp, color = Color.Gray); Text(battingStats.sixes.toString(), fontWeight = FontWeight.Bold, color = Color.Black) }
                                        }
                                        Divider(color = Color.LightGray.copy(alpha = 0.3f))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column { Text("Hundreds (100):", fontSize = 11.sp, color = Color.Gray); Text(battingStats.hundreds.toString(), fontWeight = FontWeight.Bold, color = Color.Black) }
                                            Column { Text("Fifties (50):", fontSize = 11.sp, color = Color.Gray); Text(battingStats.fifties.toString(), fontWeight = FontWeight.Bold, color = Color.Black) }
                                            Column { Text("Ducks (0):", fontSize = 11.sp, color = Color.Gray); Text(battingStats.ducks.toString(), fontWeight = FontWeight.Bold, color = Color.Black) }
                                            Column { Text("Balls Faced:", fontSize = 11.sp, color = Color.Gray); Text(battingStats.ballsFaced.toString(), fontWeight = FontWeight.Bold, color = Color.Black) }
                                        }
                                    }
                                }
                            }

                            // Bowling metrics
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BallIcon(modifier = Modifier.size(12.dp))
                                    Text("BOWLING STATISTICS", fontWeight = FontWeight.Black, fontSize = 12.sp, color = ForestGreen)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = LightEmeraldBg),
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column { Text("Overs bowled:", fontSize = 11.sp, color = Color.Gray); Text(String.format(Locale.US, "%.1f", bowlingStats.overs), fontWeight = FontWeight.Bold, color = Color.Black) }
                                            Column { Text("Wickets:", fontSize = 11.sp, color = Color.Gray); Text(bowlingStats.wickets.toString(), fontWeight = FontWeight.Bold, color = Color.Red) }
                                            Column { Text("Runs Conceded:", fontSize = 11.sp, color = Color.Gray); Text(bowlingStats.runsConceded.toString(), fontWeight = FontWeight.Bold, color = Color.Black) }
                                            Column { Text("Best Figures:", fontSize = 11.sp, color = Color.Gray); Text(bowlingStats.bestFigures, fontWeight = FontWeight.Bold, color = Color.Black) }
                                        }
                                        Divider(color = Color.LightGray.copy(alpha = 0.3f))
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column { Text("Bowling Avg:", fontSize = 11.sp, color = Color.Gray); Text(String.format(Locale.US, "%.2f", bowlingStats.average), fontWeight = FontWeight.Bold, color = Color.Black) }
                                            Column { Text("Economy rate:", fontSize = 11.sp, color = Color.Gray); Text(String.format(Locale.US, "%.2f", bowlingStats.economy), fontWeight = FontWeight.Bold, color = Color.Black) }
                                            Column { Text("4-Wkt Hauls:", fontSize = 11.sp, color = Color.Gray); Text(bowlingStats.fourWickets.toString(), fontWeight = FontWeight.Bold, color = Color.Black) }
                                            Column { Text("5-Wkt Hauls:", fontSize = 11.sp, color = Color.Gray); Text(bowlingStats.fiveWickets.toString(), fontWeight = FontWeight.Bold, color = Color.Black) }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (subTab == 2) {
                        // History Timeline Tab
                        if (timeline.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No timeline logs yet.", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(timeline) { entry ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Vertical green line bar
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(SJLGold)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(50.dp)
                                                    .background(ForestGreen)
                                            )
                                        }

                                        // Match details
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = entry.matchDescription.uppercase(Locale.US),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.sp,
                                                color = DarkGrey
                                            )

                                            if (entry.battingRuns != null) {
                                                Text(
                                                    text = "🏏 Batting: ${entry.battingRuns} Runs (${entry.battingBalls} balls)",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = ForestGreen
                                                )
                                            }

                                            if (entry.bowlingRuns != null || entry.bowlingWickets != null) {
                                                val runs = entry.bowlingRuns ?: 0
                                                val wkts = entry.bowlingWickets ?: 0
                                                Text(
                                                    text = "⚾ Bowling: $runs runs - $wkts wkts",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF1976D2)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Delete member button from the squad
                Button(
                    onClick = {
                        playerViewModel.removePlayer(player)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("❌ RELEASE FROM SQUAD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}
