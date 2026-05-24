package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Ball
import com.example.data.model.Player
import com.example.data.model.Team
import com.example.data.repository.CricketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BattingStats(
    val matches: Int = 0,
    val innings: Int = 0,
    val runs: Int = 0,
    val highestScore: Int = 0,
    val average: Double = 0.0,
    val strikeRate: Double = 0.0,
    val fifties: Int = 0,
    val hundreds: Int = 0,
    val ducks: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val ballsFaced: Int = 0,
    val dismissals: Int = 0
)

data class BowlingStats(
    val matches: Int = 0,
    val overs: Double = 0.0,
    val wickets: Int = 0,
    val runsConceded: Int = 0,
    val average: Double = 0.0,
    val economy: Double = 0.0,
    val fourWickets: Int = 0,
    val fiveWickets: Int = 0,
    val bestFigures: String = "0/0"
)

data class PlayerMatchHistoryEntry(
    val matchDescription: String,
    val date: String,
    val battingRuns: Int?,
    val battingBalls: Int?,
    val bowlingWickets: Int?,
    val bowlingRuns: Int?
)

data class PlayerLeaderboardEntry(
    val playerId: Int,
    val name: String,
    val teamName: String,
    val teamColor: String,
    val value: Int,
    val matches: Int
)

class PlayerViewModel(private val repository: CricketRepository) : ViewModel() {

    val allTeams: StateFlow<List<Team>> = repository.allTeamsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlayers: StateFlow<List<Player>> = repository.allPlayersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Exposed lists of player stats by compiling all balls reactively
    private val _allBalls = MutableStateFlow<List<Ball>>(emptyList())

    init {
        // Collect balls flow and pipe into stats state
        viewModelScope.launch {
            repository.allMatchesFlow.collect { matches ->
                val allBallsList = mutableListOf<Ball>()
                for (match in matches) {
                    val inningsList = repository.getInningsForMatch(match.id)
                    for (innings in inningsList) {
                        allBallsList.addAll(repository.getBallsForInnings(innings.id))
                    }
                }
                _allBalls.value = allBallsList
            }
        }
    }

    // Reactive computation of all player batting and bowling stats
    val playerStatsState = combine(allPlayers, allTeams, _allBalls) { players, teams, balls ->
        val playerBattingMap = mutableMapOf<Int, BattingStats>()
        val playerBowlingMap = mutableMapOf<Int, BowlingStats>()

        val ballsByBatsman = balls.groupBy { it.batsmanId }
        val ballsByBowler = balls.groupBy { it.bowlerId }

        for (player in players) {
            val batsmanBalls = ballsByBatsman[player.id] ?: emptyList()
            val bowlerBalls = ballsByBowler[player.id] ?: emptyList()

            // Calculate Batting Stats
            if (batsmanBalls.isNotEmpty()) {
                val inningsGroup = batsmanBalls.groupBy { it.inningsId }
                var runsTotal = 0
                var foursTotal = 0
                var sixesTotal = 0
                var ballsFacedTotal = 0
                var hsMax = 0
                var fiftiesCount = 0
                var hundredsCount = 0
                var ducksCount = 0
                var dismissalsCount = 0

                for ((inningsId, ballList) in inningsGroup) {
                    var runsInInnings = 0
                    var facedInInnings = 0
                    var isDismissed = false

                    for (ball in ballList) {
                        runsInInnings += ball.runs
                        if (ball.extraType != "wide") {
                            facedInInnings++
                        }
                    }

                    // Check if batsman was dismissed in this innings.
                    // (A simple check for wicket where batsmanId == player.id is perfect)
                    val wasOut = ballList.any { it.isWicket }
                    if (wasOut) {
                        dismissalsCount++
                        isDismissed = true
                    }

                    runsTotal += runsInInnings
                    foursTotal += ballList.count { it.runs == 4 }
                    sixesTotal += ballList.count { it.runs == 6 }
                    ballsFacedTotal += facedInInnings

                    if (runsInInnings > hsMax) hsMax = runsInInnings
                    if (runsInInnings >= 100) hundredsCount++
                    else if (runsInInnings >= 50) fiftiesCount++

                    if (runsInInnings == 0 && isDismissed) ducksCount++
                }

                val avg = if (dismissalsCount > 0) runsTotal.toDouble() / dismissalsCount else runsTotal.toDouble()
                val sr = if (ballsFacedTotal > 0) (runsTotal.toDouble() / ballsFacedTotal) * 100.0 else 0.0

                playerBattingMap[player.id] = BattingStats(
                    matches = inningsGroup.size,
                    innings = inningsGroup.size,
                    runs = runsTotal,
                    highestScore = hsMax,
                    average = avg,
                    strikeRate = sr,
                    fifties = fiftiesCount,
                    hundreds = hundredsCount,
                    ducks = ducksCount,
                    fours = foursTotal,
                    sixes = sixesTotal,
                    ballsFaced = ballsFacedTotal,
                    dismissals = dismissalsCount
                )
            } else {
                playerBattingMap[player.id] = BattingStats()
            }

            // Calculate Bowling Stats
            if (bowlerBalls.isNotEmpty()) {
                val inningsGroup = bowlerBalls.groupBy { it.inningsId }
                var wicketsTotal = 0
                var runsConcededTotal = 0
                var legalBallsTotal = 0
                var bestWkts = 0
                var bestRuns = 999
                var fourWktsCount = 0
                var fiveWktsCount = 0

                for ((_, ballList) in inningsGroup) {
                    var wktsInInnings = 0
                    var runsConcededInInnings = 0
                    var legalInInnings = 0

                    for (ball in ballList) {
                        if (ball.isWicket && ball.wicketType != "runout") {
                            wktsInInnings++
                        }
                        // Bowler concedes bat runs + wides + noballs
                        if (ball.extraType == "wide" || ball.extraType == "noball") {
                            runsConcededInInnings += (ball.runs + ball.extras)
                        } else {
                            runsConcededInInnings += ball.runs
                        }

                        if (ball.isLegalDelivery) {
                            legalInInnings++
                        }
                    }

                    wicketsTotal += wktsInInnings
                    runsConcededTotal += runsConcededInInnings
                    legalBallsTotal += legalInInnings

                    if (wktsInInnings >= 5) fiveWktsCount++
                    else if (wktsInInnings >= 4) fourWktsCount++

                    if (wktsInInnings > bestWkts || (wktsInInnings == bestWkts && runsConcededInInnings < bestRuns)) {
                        bestWkts = wktsInInnings
                        bestRuns = runsConcededInInnings
                    }
                }

                val oversCount = (legalBallsTotal / 6) + (legalBallsTotal % 6) / 10.0
                val avg = if (wicketsTotal > 0) runsConcededTotal.toDouble() / wicketsTotal else runsConcededTotal.toDouble()
                val eco = if (legalBallsTotal > 0) (runsConcededTotal.toDouble() / legalBallsTotal) * 6 else 0.0

                playerBowlingMap[player.id] = BowlingStats(
                    matches = inningsGroup.size,
                    overs = oversCount,
                    wickets = wicketsTotal,
                    runsConceded = runsConcededTotal,
                    average = avg,
                    economy = eco,
                    fourWickets = fourWktsCount,
                    fiveWickets = fiveWktsCount,
                    bestFigures = if (bestWkts > 0) "$bestWkts/$bestRuns" else "0/0"
                )
            } else {
                playerBowlingMap[player.id] = BowlingStats()
            }
        }

        PlayerStatsPackage(playerBattingMap, playerBowlingMap)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerStatsPackage())

    // Derived top 10 lists
    val top10Runs: StateFlow<List<PlayerLeaderboardEntry>> = combine(allPlayers, allTeams, playerStatsState) { players, teams, stats ->
        players.map { player ->
            val team = teams.find { it.id == player.teamId }
            val pStats = stats.batting[player.id] ?: BattingStats()
            PlayerLeaderboardEntry(
                playerId = player.id,
                name = player.name,
                teamName = team?.shortName ?: "SJL",
                teamColor = team?.colorHex ?: "#1a5c2e",
                value = pStats.runs,
                matches = pStats.matches
            )
        }.filter { it.value > 0 }
         .sortedByDescending { it.value }
         .take(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val top10Wickets: StateFlow<List<PlayerLeaderboardEntry>> = combine(allPlayers, allTeams, playerStatsState) { players, teams, stats ->
        players.map { player ->
            val team = teams.find { it.id == player.teamId }
            val pStats = stats.bowling[player.id] ?: BowlingStats()
            PlayerLeaderboardEntry(
                playerId = player.id,
                name = player.name,
                teamName = team?.shortName ?: "SJL",
                teamColor = team?.colorHex ?: "#1a5c2e",
                value = pStats.wickets,
                matches = pStats.matches
            )
        }.filter { it.value > 0 }
         .sortedByDescending { it.value }
         .take(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Player CRUD actions
    fun getPlayerBattingStats(playerId: Int): StateFlow<BattingStats> {
        val flow = MutableStateFlow(BattingStats())
        viewModelScope.launch {
            playerStatsState.collect {
                flow.value = it.batting[playerId] ?: BattingStats()
            }
        }
        return flow
    }

    fun getPlayerBowlingStats(playerId: Int): StateFlow<BowlingStats> {
        val flow = MutableStateFlow(BowlingStats())
        viewModelScope.launch {
            playerStatsState.collect {
                flow.value = it.bowling[playerId] ?: BowlingStats()
            }
        }
        return flow
    }

    fun addPlayerToTeam(teamId: Int, name: String, jerseyNumber: Int, isCaptain: Boolean, isWicketKeeper: Boolean, specialRole: String) {
        viewModelScope.launch {
            repository.insertPlayer(
                Player(
                    teamId = teamId,
                    name = name,
                    jerseyNumber = jerseyNumber,
                    isCaptain = isCaptain,
                    isWicketKeeper = isWicketKeeper,
                    specialRole = specialRole
                )
            )
        }
    }

    fun updatePlayer(playerId: Int, teamId: Int, name: String, jerseyNumber: Int, isCaptain: Boolean, isWicketKeeper: Boolean, specialRole: String) {
        viewModelScope.launch {
            val player = repository.allPlayersFlow.first().find { it.id == playerId }
            if (player != null) {
                repository.updatePlayer(
                    player.copy(
                        teamId = teamId,
                        name = name,
                        jerseyNumber = jerseyNumber,
                        isCaptain = isCaptain,
                        isWicketKeeper = isWicketKeeper,
                        specialRole = specialRole
                    )
                )
            }
        }
    }

    fun removePlayer(player: Player) {
        viewModelScope.launch {
            repository.deletePlayer(player)
        }
    }

    fun modifyTeam(teamId: Int, name: String, shortName: String, colorHex: String, homeGround: String) {
        viewModelScope.launch {
            val team = repository.allTeamsFlow.first().find { it.id == teamId }
            if (team != null) {
                repository.updateTeam(
                    team.copy(
                        name = name,
                        shortName = shortName,
                        colorHex = colorHex,
                        homeGround = homeGround
                    )
                )
            }
        }
    }

    // Single Player Profile History Builder
    fun getPlayerMatchHistory(playerId: Int): StateFlow<List<PlayerMatchHistoryEntry>> {
        val flow = MutableStateFlow<List<PlayerMatchHistoryEntry>>(emptyList())
        viewModelScope.launch {
            val matchesList = repository.allMatchesFlow.first()
            val history = mutableListOf<PlayerMatchHistoryEntry>()

            for (match in matchesList) {
                val team1 = repository.getTeamById(match.team1Id)
                val team2 = repository.getTeamById(match.team2Id)
                val matchDesc = "${team1?.shortName ?: "T1"} vs ${team2?.shortName ?: "T2"}"

                val innings = repository.getInningsForMatch(match.id)
                var battingRuns: Int? = null
                var battingBalls: Int? = null
                var bowlingWickets: Int? = null
                var bowlingRuns: Int? = null

                for (inn in innings) {
                    val ballsInInn = repository.getBallsForInnings(inn.id)

                    // Batting historical
                    val batBalls = ballsInInn.filter { it.batsmanId == playerId }
                    if (batBalls.isNotEmpty()) {
                        battingRuns = batBalls.sumOf { it.runs }
                        battingBalls = batBalls.count { it.extraType != "wide" }
                    }

                    // Bowling historical
                    val bowlBalls = ballsInInn.filter { it.bowlerId == playerId }
                    if (bowlBalls.isNotEmpty()) {
                        bowlingWickets = bowlBalls.count { it.isWicket && it.wicketType != "runout" }
                        bowlingRuns = bowlBalls.sumOf {
                            if (it.extraType == "wide" || it.extraType == "noball") it.runs + it.extras else it.runs
                        }
                    }
                }

                if (battingRuns != null || bowlingWickets != null) {
                    history.add(
                        PlayerMatchHistoryEntry(
                            matchDescription = matchDesc,
                            date = match.date,
                            battingRuns = battingRuns,
                            battingBalls = battingBalls,
                            bowlingWickets = bowlingWickets,
                            bowlingRuns = bowlingRuns
                        )
                    )
                }
            }
            flow.value = history
        }
        return flow
    }
}

data class PlayerStatsPackage(
    val batting: Map<Int, BattingStats> = emptyMap(),
    val bowling: Map<Int, BowlingStats> = emptyMap()
)
