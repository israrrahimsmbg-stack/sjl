package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.db.AppDatabase
import com.example.data.repository.CricketRepository
import com.example.ui.home.HomeScreen
import com.example.ui.players.PlayersScreen
import com.example.ui.scorecard.ScorecardScreen
import com.example.ui.scoring.ScoringScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.tournament.TournamentScreen
import com.example.viewmodel.PlayerViewModel
import com.example.viewmodel.ScoringViewModel
import com.example.viewmodel.TournamentViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Local Room persistence instantiation on launch
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = CricketRepository(
            database,
            database.teamDao(),
            database.playerDao(),
            database.matchDao(),
            database.inningsDao(),
            database.ballDao()
        )

        val scoringViewModel = ScoringViewModel(repository)
        val tournamentViewModel = TournamentViewModel(repository)
        val playerViewModel = PlayerViewModel(repository)

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFF061C0F), // Sleek Dark Pakistani Emerald
                            tonalElevation = 8.dp
                        ) {
                            val items = listOf(
                                NavigationItem("home", "HOME", Icons.Default.Home),
                                NavigationItem("score", "SCORE", Icons.Default.Edit),
                                NavigationItem("scorecard", "SCORECARDS", Icons.Default.List),
                                NavigationItem("tournament", "STANDINGS", Icons.Default.DateRange),
                                NavigationItem("players", "PLAYERS", Icons.Default.Person)
                            )

                            items.forEach { item ->
                                val isSelected = currentRoute?.startsWith(item.route) == true

                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF061C0F),
                                        selectedTextColor = Color(0xFFF6C344),
                                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                                        unselectedTextColor = Color.White.copy(alpha = 0.5f),
                                        indicatorColor = Color(0xFFF6C344)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(
                                scoringViewModel = scoringViewModel,
                                tournamentViewModel = tournamentViewModel,
                                playerViewModel = playerViewModel,
                                onNavigateToScoring = { navController.navigate("score") },
                                onNavigateToScorecard = { matchId ->
                                    navController.navigate("scorecard?matchId=$matchId")
                                }
                            )
                        }

                        composable("score") {
                            ScoringScreen(
                                viewModel = scoringViewModel,
                                onNavigateHome = { navController.navigate("home") },
                                onNavigateToScorecard = { matchId ->
                                    navController.navigate("scorecard?matchId=$matchId")
                                }
                            )
                        }

                        composable(
                            route = "scorecard?matchId={matchId}",
                            arguments = listOf(navArgument("matchId") {
                                type = NavType.IntType
                                defaultValue = -1
                            })
                        ) { backStackEntry ->
                            val matchIdArg = backStackEntry.arguments?.getInt("matchId") ?: -1
                            val activeMatchId = if (matchIdArg == -1) null else matchIdArg

                            ScorecardScreen(
                                scoringViewModel = scoringViewModel,
                                tournamentViewModel = tournamentViewModel,
                                playerViewModel = playerViewModel,
                                selectedMatchId = activeMatchId
                            )
                        }

                        composable("tournament") {
                            TournamentScreen(
                                tournamentViewModel = tournamentViewModel,
                                scoringViewModel = scoringViewModel,
                                playerViewModel = playerViewModel,
                                onNavigateToScoring = { navController.navigate("score") },
                                onNavigateToScorecard = { matchId ->
                                    navController.navigate("scorecard?matchId=$matchId")
                                }
                            )
                        }

                        composable("players") {
                            PlayersScreen(
                                playerViewModel = playerViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

data class NavigationItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
