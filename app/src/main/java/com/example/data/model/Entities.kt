package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Team Entity
@Entity(tableName = "teams")
data class Team(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val shortName: String, // 3 letters e.g. SJL
    val colorHex: String,
    val homeGround: String
)

// Player Entity
@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val teamId: Int,
    val name: String,
    val jerseyNumber: Int,
    val isCaptain: Boolean,
    val isWicketKeeper: Boolean,
    val specialRole: String = "Player" // "Player", "Coach", "Support Staff", "Concussion Sub", "Impact Player"
)

// Match Entity
@Entity(tableName = "matches")
data class Match(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val team1Id: Int,
    val team2Id: Int,
    val totalOvers: Int,
    val venue: String,
    val date: String, // DD/MM/YYYY
    val tossWinnerId: Int,
    val tossChoice: String, // "bat" or "bowl"
    val status: String, // "upcoming", "live", "complete"
    val winnerId: Int?,
    val resultDescription: String
)

// Innings Entity
@Entity(tableName = "innings")
data class Innings(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val matchId: Int,
    val battingTeamId: Int,
    val bowlingTeamId: Int,
    val inningsNumber: Int, // 1 or 2
    val totalRuns: Int = 0,
    val wickets: Int = 0,
    val oversBowled: Float = 0.0f,
    val extras: Int = 0,
    val wides: Int = 0,
    val noBalls: Int = 0,
    val byes: Int = 0,
    val legByes: Int = 0,
    val isComplete: Boolean = false
)

// Ball (ball-by-ball record)
@Entity(tableName = "balls")
data class Ball(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val inningsId: Int,
    val overNumber: Int,    // 0-indexed over number (0 = 1st over, 1 = 2nd over, etc.)
    val ballNumber: Int,    // 1 to 6 legal balls
    val batsmanId: Int,
    val bowlerId: Int,
    val runs: Int,          // runs off bat
    val extras: Int,        // extra runs
    val extraType: String,  // "", "wide", "noball", "bye", "legbye"
    val isWicket: Boolean,
    val wicketType: String, // "bowled", "caught", "lbw", "runout", "stumped", "hitwicket", "caughtbowled"
    val fielderName: String,
    val isLegalDelivery: Boolean
)
