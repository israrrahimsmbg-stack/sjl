package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Ball
import com.example.data.model.Innings
import com.example.data.model.Match
import com.example.data.model.Player
import com.example.data.model.Team
import com.example.data.repository.CricketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Calendar

class ScoringViewModel(val repository: CricketRepository) : ViewModel() {

    // --- SETUP STATE ---
    val allTeams = repository.allTeamsFlow
    private val _team1Squad = MutableStateFlow<List<Player>>(emptyList())
    val team1Squad = _team1Squad.asStateFlow()

    private val _team2Squad = MutableStateFlow<List<Player>>(emptyList())
    val team2Squad = _team2Squad.asStateFlow()

    // --- LIVE MATCH STATE ---
    private val _liveMatch = MutableStateFlow<Match?>(null)
    val liveMatch: StateFlow<Match?> = _liveMatch.asStateFlow()

    private val _activeInnings = MutableStateFlow<Innings?>(null)
    val activeInnings: StateFlow<Innings?> = _activeInnings.asStateFlow()

    private val _firstInnings = MutableStateFlow<Innings?>(null)
    val firstInnings: StateFlow<Innings?> = _firstInnings.asStateFlow()

    private val _balls = MutableStateFlow<List<Ball>>(emptyList())
    val balls: StateFlow<List<Ball>> = _balls.asStateFlow()

    // --- LINEUP SELECTIONS ---
    private val _selectedTeam1XIIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedTeam1XIIds = _selectedTeam1XIIds.asStateFlow()

    private val _selectedTeam2XIIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedTeam2XIIds = _selectedTeam2XIIds.asStateFlow()

    // --- CREASE STATE ---
    private val _strikerId = MutableStateFlow<Int?>(null)
    val strikerId = _strikerId.asStateFlow()

    private val _nonStrikerId = MutableStateFlow<Int?>(null)
    val nonStrikerId = _nonStrikerId.asStateFlow()

    private val _currentBowlerId = MutableStateFlow<Int?>(null)
    val currentBowlerId = _currentBowlerId.asStateFlow()

    // Dialog & UI flows
    private val _showWicketDialog = MutableStateFlow(false)
    val showWicketDialog = _showWicketDialog.asStateFlow()

    private val _showBowlerSelectDialog = MutableStateFlow(false)
    val showBowlerSelectDialog = _showBowlerSelectDialog.asStateFlow()

    // Triggered at end of live match
    private val _matchWinner = MutableStateFlow<Team?>(null)
    val matchWinner = _matchWinner.asStateFlow()

    init {
        // Load any active "live" match from the DB immediately to restore scorer session
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
            restoreActiveMatch()
        }
    }

    private suspend fun restoreActiveMatch() {
        val matches = repository.allMatchesFlow.first()
        val active = matches.find { it.status == "live" }
        if (active != null) {
            _liveMatch.value = active
            _team1Squad.value = repository.getPlayersByTeam(active.team1Id)
            _team2Squad.value = repository.getPlayersByTeam(active.team2Id)

            val inningsList = repository.getInningsForMatch(active.id)
            if (inningsList.isNotEmpty()) {
                val currentInnings = inningsList.last()
                _activeInnings.value = currentInnings
                if (inningsList.size > 1) {
                    _firstInnings.value = inningsList.first()
                }
                loadBallsAndPlayers(currentInnings)
            }
        }
    }

    private var team1Job: kotlinx.coroutines.Job? = null
    private var team2Job: kotlinx.coroutines.Job? = null

    fun onSquadSelected(team1Id: Int, team2Id: Int) {
        team1Job?.cancel()
        if (team1Id != 0) {
            team1Job = viewModelScope.launch {
                repository.getPlayersByTeamFlow(team1Id).collect { players ->
                    _team1Squad.value = players
                    val currentSel = _selectedTeam1XIIds.value
                    if (currentSel.isEmpty() || currentSel.all { id -> players.none { it.id == id } }) {
                        _selectedTeam1XIIds.value = players.take(11).map { it.id }.toSet()
                    }
                }
            }
        } else {
            _team1Squad.value = emptyList()
            _selectedTeam1XIIds.value = emptySet()
        }

        team2Job?.cancel()
        if (team2Id != 0) {
            team2Job = viewModelScope.launch {
                repository.getPlayersByTeamFlow(team2Id).collect { players ->
                    _team2Squad.value = players
                    val currentSel = _selectedTeam2XIIds.value
                    if (currentSel.isEmpty() || currentSel.all { id -> players.none { it.id == id } }) {
                        _selectedTeam2XIIds.value = players.take(11).map { it.id }.toSet()
                    }
                }
            }
        } else {
            _team2Squad.value = emptyList()
            _selectedTeam2XIIds.value = emptySet()
        }
    }

    fun togglePlayerSelection(playerId: Int, teamNum: Int) {
        if (teamNum == 1) {
            val current = _selectedTeam1XIIds.value.toMutableSet()
            if (current.contains(playerId)) {
                if (current.size > 2) current.remove(playerId)
            } else {
                if (current.size < 11) current.add(playerId)
            }
            _selectedTeam1XIIds.value = current
        } else {
            val current = _selectedTeam2XIIds.value.toMutableSet()
            if (current.contains(playerId)) {
                if (current.size > 2) current.remove(playerId)
            } else {
                if (current.size < 11) current.add(playerId)
            }
            _selectedTeam2XIIds.value = current
        }
    }

    // Initialize/Start a Match Scoring Sequence
    fun startMatch(
        team1Id: Int,
        team2Id: Int,
        overs: Int,
        venue: String,
        tossWinnerId: Int,
        tossChoice: String,
        openingStrikerId: Int,
        openingNonStrikerId: Int,
        openingBowlerId: Int
    ) {
        viewModelScope.launch {
            // Find an upcoming match of these two teams or create one
            val matches = repository.allMatchesFlow.first()
            var existingMatch = matches.find {
                it.status == "upcoming" &&
                ((it.team1Id == team1Id && it.team2Id == team2Id) || (it.team1Id == team2Id && it.team2Id == team1Id))
            }

            val today = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(System.currentTimeMillis())

            val matchToScore = if (existingMatch != null) {
                existingMatch.copy(
                    totalOvers = overs,
                    venue = venue,
                    tossWinnerId = tossWinnerId,
                    tossChoice = tossChoice,
                    status = "live",
                    date = today
                )
            } else {
                Match(
                    team1Id = team1Id,
                    team2Id = team2Id,
                    totalOvers = overs,
                    venue = venue,
                    date = today,
                    tossWinnerId = tossWinnerId,
                    tossChoice = tossChoice,
                    status = "live",
                    winnerId = null,
                    resultDescription = "Match Live"
                )
            }

            val matchId = if (matchToScore.id == 0) {
                repository.insertMatch(matchToScore).toInt()
            } else {
                repository.updateMatch(matchToScore)
                matchToScore.id
            }

            val finalMatch = repository.getMatchById(matchId)!!
            _liveMatch.value = finalMatch

            // Setup first Innings
            // If tossChoice is bat: tossWinner bats first, other bowls first
            // If tossChoice is bowl: tossWinner bowls first, other bats first
            val firstBattingTeamId = if (tossChoice == "bat") tossWinnerId else {
                if (tossWinnerId == team1Id) team2Id else team1Id
            }
            val firstBowlingTeamId = if (firstBattingTeamId == team1Id) team2Id else team1Id

            val innings = Innings(
                matchId = matchId,
                battingTeamId = firstBattingTeamId,
                bowlingTeamId = firstBowlingTeamId,
                inningsNumber = 1
            )
            val inningsId = repository.insertInnings(innings).toInt()
            val activeInningsObj = innings.copy(id = inningsId)

            _activeInnings.value = activeInningsObj
            _firstInnings.value = null
            _strikerId.value = openingStrikerId
            _nonStrikerId.value = openingNonStrikerId
            _currentBowlerId.value = openingBowlerId
            _balls.value = emptyList()

            _team1Squad.value = repository.getPlayersByTeam(team1Id)
            _team2Squad.value = repository.getPlayersByTeam(team2Id)
        }
    }

    private suspend fun loadBallsAndPlayers(innings: Innings) {
        val ballsList = repository.getBallsForInnings(innings.id)
        _balls.value = ballsList

        if (ballsList.isNotEmpty()) {
            val lastBall = ballsList.last()
            // Auto deduce who is striker/nonstriker based on last legal delivery and swaps
            // Let's use basic logic:
            _currentBowlerId.value = lastBall.bowlerId

            // To avoid complexity, we can reconstruct the running strikers by stepping through the entire balls list
            var currentStriker = strikerId.value ?: lastBall.batsmanId // fallback
            var currentNonStriker = nonStrikerId.value ?: 0 // fallback
            // We reconstruct at the end of load
        }
    }

    fun swapBatsmen() {
        val s = _strikerId.value
        val n = _nonStrikerId.value
        _strikerId.value = n
        _nonStrikerId.value = s
    }

    fun selectBowler(bowlerId: Int) {
        _currentBowlerId.value = bowlerId
        _showBowlerSelectDialog.value = false
    }

    fun closeWicketDialog() {
        _showWicketDialog.value = false
    }

    // RECORD A BALL
    fun recordBall(runsOffBat: Int, extraType: String) {
        val match = _liveMatch.value ?: return
        val innings = _activeInnings.value ?: return
        val striker = _strikerId.value ?: return
        val bowler = _currentBowlerId.value ?: return

        viewModelScope.launch {
            val ballsForInnings = repository.getBallsForInnings(innings.id)
            val legalBallsCountInOver = ballsForInnings.filter { it.isLegalDelivery }.size
            val currentOverNumber = legalBallsCountInOver / 6
            val currentBallNumber = (legalBallsCountInOver % 6) + 1

            val isLegal = (extraType != "wide" && extraType != "noball")
            val nextLegalBallNumber = if (isLegal) currentBallNumber else 0

            var totalRunsThisBall = runsOffBat
            var extrasThisBall = 0

            when (extraType) {
                "wide" -> {
                    extrasThisBall = runsOffBat + 1 // e.g. wide + extra byes off wide
                    totalRunsThisBall = 0 // not batsman's runs
                }
                "noball" -> {
                    extrasThisBall = 1
                    totalRunsThisBall = runsOffBat // batsman gets run off bat on noball
                }
                "bye", "legbye" -> {
                    extrasThisBall = runsOffBat
                    totalRunsThisBall = 0 // credited to extras
                }
            }

            val ball = Ball(
                inningsId = innings.id,
                overNumber = currentOverNumber,
                ballNumber = nextLegalBallNumber,
                batsmanId = striker,
                bowlerId = bowler,
                runs = totalRunsThisBall,
                extras = extrasThisBall,
                extraType = extraType,
                isWicket = false,
                wicketType = "",
                fielderName = "",
                isLegalDelivery = isLegal
            )

            repository.insertBall(ball)
            val updatedBalls = repository.getBallsForInnings(innings.id)
            _balls.value = updatedBalls

            // Update Innings stats
            val runsAdded = totalRunsThisBall + extrasThisBall
            val legalBallsNow = updatedBalls.filter { it.isLegalDelivery }.size

            val updatedInnings = innings.copy(
                totalRuns = innings.totalRuns + runsAdded,
                extras = innings.extras + extrasThisBall,
                wides = innings.wides + (if (extraType == "wide") runsAdded else 0),
                noBalls = innings.noBalls + (if (extraType == "noball") 1 else 0),
                byes = innings.byes + (if (extraType == "bye") runsAdded else 0),
                legByes = innings.legByes + (if (extraType == "legbye") runsAdded else 0),
                oversBowled = (legalBallsNow / 6) + (legalBallsNow % 6) / 10f
            )

            repository.updateInnings(updatedInnings)
            _activeInnings.value = updatedInnings

            // Swap batsmen if runs off bat is odd, or if extras (byes) are odd
            val scoreRotation = if (extraType == "bye" || extraType == "legbye") extrasThisBall else runsOffBat
            if (scoreRotation % 2 != 0) {
                swapBatsmen()
            }

            // check end of over or end of match
            checkInningsMilestones(updatedInnings, legalBallsNow, match)
        }
    }

    // RECORD A WICKET (triggered via bottom sheet)
    fun recordWicket(wicketType: String, fielderName: String, nextBatsmanId: Int) {
        val match = _liveMatch.value ?: return
        val innings = _activeInnings.value ?: return
        val striker = _strikerId.value ?: return
        val nonStriker = _nonStrikerId.value ?: return
        val bowler = _currentBowlerId.value ?: return

        viewModelScope.launch {
            val ballsForInnings = repository.getBallsForInnings(innings.id)
            val legalBallsCountInOver = ballsForInnings.filter { it.isLegalDelivery }.size
            val currentOverNumber = legalBallsCountInOver / 6
            val currentBallNumber = (legalBallsCountInOver % 6) + 1

            // Wicket ball is a legal delivery (unless specified, typically yes in our simple app)
            val ball = Ball(
                inningsId = innings.id,
                overNumber = currentOverNumber,
                ballNumber = currentBallNumber,
                batsmanId = striker,
                bowlerId = bowler,
                runs = 0,
                extras = 0,
                extraType = "",
                isWicket = true,
                wicketType = wicketType,
                fielderName = fielderName,
                isLegalDelivery = true
            )

            repository.insertBall(ball)
            val updatedBalls = repository.getBallsForInnings(innings.id)
            _balls.value = updatedBalls

            val legalBallsNow = updatedBalls.filter { it.isLegalDelivery }.size
            val updatedInnings = innings.copy(
                wickets = innings.wickets + 1,
                oversBowled = (legalBallsNow / 6) + (legalBallsNow % 6) / 10f
            )

            repository.updateInnings(updatedInnings)
            _activeInnings.value = updatedInnings
            _showWicketDialog.value = false

            // Set new batsman
            if (updatedInnings.wickets < 10) {
                _strikerId.value = nextBatsmanId
            }

            // check end of over or end of match
            checkInningsMilestones(updatedInnings, legalBallsNow, match)
        }
    }

    private fun checkInningsMilestones(innings: Innings, legalBallsNow: Int, match: Match) {
        val maxBalls = match.totalOvers * 6
        val isOversComplete = legalBallsNow >= maxBalls
        val isAllOut = innings.wickets >= 10

        // In 2nd innings, we also check if target is chased
        var isChased = false
        if (innings.inningsNumber == 2) {
            val target = (_firstInnings.value?.totalRuns ?: 0) + 1
            if (innings.totalRuns >= target) {
                isChased = true
            }
        }

        if (isAllOut || isOversComplete || isChased) {
            // End of innings!
            endInnings(innings, match)
        } else {
            // Check if end of over (6 legal deliveries)
            if (legalBallsNow > 0 && legalBallsNow % 6 == 0) {
                // End of over! Prompt select bowler and swap ends
                swapBatsmen()
                _showBowlerSelectDialog.value = true
            }
        }
    }

    private fun endInnings(innings: Innings, match: Match) {
        viewModelScope.launch {
            val completedInnings = innings.copy(isComplete = true)
            repository.updateInnings(completedInnings)

            if (innings.inningsNumber == 1) {
                // Transit to 2nd Innings
                _firstInnings.value = completedInnings

                val secondInnings = Innings(
                    matchId = match.id,
                    battingTeamId = innings.bowlingTeamId,
                    bowlingTeamId = innings.battingTeamId,
                    inningsNumber = 2
                )
                val secondInningsId = repository.insertInnings(secondInnings).toInt()
                val secondInningsObj = secondInnings.copy(id = secondInningsId)

                // Swap striker & nonstriker and bowler to let user choose for second innings
                _activeInnings.value = secondInningsObj
                _balls.value = emptyList()

                // Default opening selections from opposing squads
                val battingSquad = if (secondInningsObj.battingTeamId == match.team1Id) _team1Squad.value else _team2Squad.value
                val bowlingSquad = if (secondInningsObj.bowlingTeamId == match.team1Id) _team1Squad.value else _team2Squad.value

                _strikerId.value = battingSquad.getOrNull(0)?.id ?: 0
                _nonStrikerId.value = battingSquad.getOrNull(1)?.id ?: 0
                _currentBowlerId.value = bowlingSquad.getOrNull(0)?.id ?: 0

            } else {
                // 2nd innings complete! End match
                val target = (_firstInnings.value?.totalRuns ?: 0) + 1
                val firstInningsRuns = _firstInnings.value?.totalRuns ?: 0
                val secondInningsRuns = innings.totalRuns

                val winnerId: Int?
                val desc: String

                val team1Short = if (match.team1Id == match.team1Id) {
                    _team1Squad.value.getOrNull(0)?.let { "Team 1" } ?: "Batting"
                } else "Bowling"

                val team1 = repository.getTeamById(match.team1Id)
                val team2 = repository.getTeamById(match.team2Id)

                val t1Name = team1?.name ?: "Team 1"
                val t2Name = team2?.name ?: "Team 2"

                val battingTeam = repository.getTeamById(innings.battingTeamId)
                val bowlingTeam = repository.getTeamById(innings.bowlingTeamId)

                val bName = battingTeam?.name ?: "Batting Team"
                val bowName = bowlingTeam?.name ?: "Bowling Team"

                if (secondInningsRuns >= target) {
                    winnerId = innings.battingTeamId
                    val wicketsLeft = 10 - innings.wickets
                    desc = "$bName won by $wicketsLeft wickets!"
                } else if (secondInningsRuns < firstInningsRuns) {
                    winnerId = innings.bowlingTeamId
                    val margin = firstInningsRuns - secondInningsRuns
                    desc = "$bowName won by $margin runs!"
                } else {
                    winnerId = null // Tie
                    desc = "Match Tied!"
                }

                val completedMatch = match.copy(
                    status = "complete",
                    winnerId = winnerId,
                    resultDescription = desc
                )
                repository.updateMatch(completedMatch)
                _liveMatch.value = completedMatch
                _activeInnings.value = completedInnings

                if (winnerId != null) {
                    _matchWinner.value = repository.getTeamById(winnerId)
                }
            }
        }
    }

    // TRIGGER WICKET BOTTOMSHEET
    fun openWicketDialog() {
        _showWicketDialog.value = true
    }

    // TRIGGER BOWLER SELECT DIALOG
    fun openBowlerSelectDialog() {
        _showBowlerSelectDialog.value = true
    }

    fun closeBowlerSelectDialog() {
        _showBowlerSelectDialog.value = false
    }

    // UNDO ACTION (Rollback last ball from db)
    fun undoLastBall() {
        val innings = _activeInnings.value ?: return
        viewModelScope.launch {
            val ballsForInnings = repository.getBallsForInnings(innings.id)
            if (ballsForInnings.isEmpty()) return@launch

            val lastBall = ballsForInnings.last()

            // Delete the last ball
            repository.deleteLastBallOfInnings(innings.id)

            // Re-aggregate innings score
            val remainingBalls = repository.getBallsForInnings(innings.id)
            _balls.value = remainingBalls

            var totalRuns = 0
            var wickets = 0
            var extras = 0
            var wides = 0
            var noBalls = 0
            var byes = 0
            var legByes = 0

            for (ball in remainingBalls) {
                val r = ball.runs
                val e = ball.extras
                totalRuns += (r + e)
                extras += e
                if (ball.isWicket) wickets++
                when (ball.extraType) {
                    "wide" -> wides += e
                    "noball" -> {
                        noBalls++
                        totalRuns += r // adds bat runs of noball too is aggregated as r + e already
                    }
                    "bye" -> byes += e
                    "legbye" -> legByes += e
                }
            }

            // Adjust noballs summation if duplication
            val finalOversCount = remainingBalls.filter { it.isLegalDelivery }.size
            val finalInnings = innings.copy(
                totalRuns = totalRuns,
                wickets = wickets,
                extras = extras,
                wides = wides,
                noBalls = noBalls,
                byes = byes,
                legByes = legByes,
                oversBowled = (finalOversCount / 6) + (finalOversCount % 6) / 10f,
                isComplete = false
            )

            repository.updateInnings(finalInnings)
            _activeInnings.value = finalInnings

            // Re-establish striker/nonstriker state
            _strikerId.value = lastBall.batsmanId // restore striker who played the ball
            _currentBowlerId.value = lastBall.bowlerId
            _showWicketDialog.value = false
        }
    }

    fun completeMatchExplicitly() {
        val match = _liveMatch.value ?: return
        val innings1 = _firstInnings.value ?: _activeInnings.value ?: return
        viewModelScope.launch {
            val allInnings = repository.getInningsForMatch(match.id)
            val updatedInningsList = allInnings.map { it.copy(isComplete = true) }
            updatedInningsList.forEach { repository.updateInnings(it) }

            val i1 = updatedInningsList.getOrNull(0)
            val i2 = updatedInningsList.getOrNull(1)

            val winnerId: Int?
            val desc: String

            val team1 = repository.getTeamById(match.team1Id)
            val team2 = repository.getTeamById(match.team2Id)

            val t1Name = team1?.name ?: "Team 1"
            val t2Name = team2?.name ?: "Team 2"

            if (i2 != null) {
                val t1Runs = i1?.totalRuns ?: 0
                val t2Runs = i2.totalRuns
                if (t2Runs > t1Runs) {
                    winnerId = i2.battingTeamId
                    val wkts = 10 - i2.wickets
                    desc = "${if (winnerId == match.team1Id) t1Name else t2Name} won by $wkts wickets!"
                } else if (t2Runs < t1Runs) {
                    winnerId = i1!!.battingTeamId
                    val margin = t1Runs - t2Runs
                    desc = "${if (winnerId == match.team1Id) t1Name else t2Name} won by $margin runs!"
                } else {
                    winnerId = null
                    desc = "Match Tied!"
                }
            } else {
                winnerId = i1?.battingTeamId ?: match.team1Id
                desc = "Tournament Match completed early!"
            }

            val completedMatch = match.copy(
                status = "complete",
                winnerId = winnerId,
                resultDescription = desc
            )
            repository.updateMatch(completedMatch)
            _liveMatch.value = completedMatch
        }
    }

    fun clearLiveMatch() {
        _liveMatch.value = null
        _activeInnings.value = null
        _firstInnings.value = null
        _balls.value = emptyList()
        _strikerId.value = null
        _nonStrikerId.value = null
        _currentBowlerId.value = null
    }
}
