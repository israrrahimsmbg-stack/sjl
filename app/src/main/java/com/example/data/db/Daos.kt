package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Ball
import com.example.data.model.Innings
import com.example.data.model.Match
import com.example.data.model.Player
import com.example.data.model.Team
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    @Query("SELECT * FROM teams ORDER BY name ASC")
    fun getAllTeamsFlow(): Flow<List<Team>>

    @Query("SELECT * FROM teams ORDER BY name ASC")
    suspend fun getAllTeams(): List<Team>

    @Query("SELECT * FROM teams WHERE id = :id")
    suspend fun getTeamById(id: Int): Team?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: Team): Long

    @Update
    suspend fun updateTeam(team: Team)

    @Delete
    suspend fun deleteTeam(team: Team)
}

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY name ASC")
    fun getAllPlayersFlow(): Flow<List<Player>>

    @Query("SELECT * FROM players ORDER BY name ASC")
    suspend fun getAllPlayers(): List<Player>

    @Query("SELECT * FROM players WHERE teamId = :teamId ORDER BY name ASC")
    fun getPlayersByTeamFlow(teamId: Int): Flow<List<Player>>

    @Query("SELECT * FROM players WHERE teamId = :teamId ORDER BY name ASC")
    suspend fun getPlayersByTeam(teamId: Int): List<Player>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerById(id: Int): Player?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: Player): Long

    @Update
    suspend fun updatePlayer(player: Player)

    @Delete
    suspend fun deletePlayer(player: Player)
}

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY id DESC")
    fun getAllMatchesFlow(): Flow<List<Match>>

    @Query("SELECT * FROM matches ORDER BY id DESC")
    suspend fun getAllMatches(): List<Match>

    @Query("SELECT * FROM matches WHERE id = :id")
    fun getMatchFlowById(id: Int): Flow<Match?>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: Int): Match?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: Match): Long

    @Update
    suspend fun updateMatch(match: Match)

    @Delete
    suspend fun deleteMatch(match: Match)
}

@Dao
interface InningsDao {
    @Query("SELECT * FROM innings WHERE matchId = :matchId ORDER BY inningsNumber ASC")
    fun getInningsForMatchFlow(matchId: Int): Flow<List<Innings>>

    @Query("SELECT * FROM innings WHERE matchId = :matchId ORDER BY inningsNumber ASC")
    suspend fun getInningsForMatch(matchId: Int): List<Innings>

    @Query("SELECT * FROM innings WHERE matchId = :matchId AND inningsNumber = :inningsNum")
    suspend fun getInningsByNumber(matchId: Int, inningsNum: Int): Innings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInnings(innings: Innings): Long

    @Update
    suspend fun updateInnings(innings: Innings)
}

@Dao
interface BallDao {
    @Query("SELECT * FROM balls WHERE inningsId = :inningsId ORDER BY id ASC")
    fun getBallsForInningsFlow(inningsId: Int): Flow<List<Ball>>

    @Query("SELECT * FROM balls WHERE inningsId = :inningsId ORDER BY id ASC")
    suspend fun getBallsForInnings(inningsId: Int): List<Ball>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBall(ball: Ball): Long

    @Delete
    suspend fun deleteBall(ball: Ball)

    @Query("DELETE FROM balls WHERE inningsId = :inningsId AND id = (SELECT MAX(id) FROM balls WHERE inningsId = :inningsId)")
    suspend fun deleteLastBallOfInnings(inningsId: Int)
}
