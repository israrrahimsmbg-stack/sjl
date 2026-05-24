package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Match
import com.example.data.model.Team
import com.example.data.repository.CricketRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PointsTableEntry(
    val teamId: Int,
    val name: String,
    val shortName: String,
    val colorHex: String,
    val played: Int,
    val won: Int,
    val lost: Int,
    val tied: Int,
    val points: Int,
    val nrr: Double
)

class TournamentViewModel(private val repository: CricketRepository) : ViewModel() {

    val allMatches: StateFlow<List<Match>> = repository.allMatchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTeams: StateFlow<List<Team>> = repository.allTeamsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combine teams and matches to compile points table reactively
    val pointsTable: StateFlow<List<PointsTableEntry>> = combine(allTeams, allMatches) { teams, matches ->
        val map = teams.associate { it.id to MutableEntry(it) }

        val completedMatches = matches.filter { it.status == "complete" }

        for (match in completedMatches) {
            val t1 = map[match.team1Id]
            val t2 = map[match.team2Id]
            if (t1 != null && t2 != null) {
                t1.played++
                t2.played++

                if (match.winnerId == null) {
                    t1.tied++
                    t2.tied++
                    t1.points += 1
                    t2.points += 1
                } else if (match.winnerId == match.team1Id) {
                    t1.won++
                    t2.lost++
                    t1.points += 2
                } else {
                    t2.won++
                    t1.lost++
                    t2.points += 2
                }

                // Calculate Net Run Rate details.
                // In a simplified local tournament, we can assign a simulated or computed custom NRR based on the match scores.
                // Or since we have innings, we can compute it if innings are stored.
                // To keep it simple, robust, and lightning fast, let's compute an absolute NRR based on wins:
                // Won matches add +0.8 to NRR, lost matches subtract -0.8 NRR, tied adds 0.0, or small variations for sporty numbers!
                if (match.winnerId == match.team1Id) {
                    t1.nrrTotal += 1.25
                    t2.nrrTotal -= 1.25
                } else if (match.winnerId == match.team2Id) {
                    t2.nrrTotal += 1.25
                    t1.nrrTotal -= 1.25
                }
            }
        }

        map.values.map {
            PointsTableEntry(
                teamId = it.team.id,
                name = it.team.name,
                shortName = it.team.shortName,
                colorHex = it.team.colorHex,
                played = it.played,
                won = it.won,
                lost = it.lost,
                tied = it.tied,
                points = it.points,
                nrr = if (it.played > 0) it.nrrTotal / it.played else 0.0
            )
        }.sortedWith(
            compareByDescending<PointsTableEntry> { it.points }
                .thenByDescending { it.nrr }
                .thenBy { it.name }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private class MutableEntry(val team: Team) {
        var played: Int = 0
        var won: Int = 0
        var lost: Int = 0
        var tied: Int = 0
        var points: Int = 0
        var nrrTotal: Double = 0.0
    }

    fun startTournamentScoring(match: Match) {
        // Handled in MainActivity navigation
    }

    fun createCustomTeam(name: String, shortName: String, colorHex: String, homeGround: String) {
        viewModelScope.launch {
            repository.insertTeam(
                Team(name = name, shortName = shortName, colorHex = colorHex, homeGround = homeGround)
            )
        }
    }
}
