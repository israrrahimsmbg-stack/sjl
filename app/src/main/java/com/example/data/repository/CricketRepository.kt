package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.db.AppDatabase
import com.example.data.db.BallDao
import com.example.data.db.InningsDao
import com.example.data.db.MatchDao
import com.example.data.db.PlayerDao
import com.example.data.db.TeamDao
import com.example.data.model.Ball
import com.example.data.model.Innings
import com.example.data.model.Match
import com.example.data.model.Player
import com.example.data.model.Team
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CricketRepository(
    private val database: AppDatabase,
    private val teamDao: TeamDao,
    private val playerDao: PlayerDao,
    private val matchDao: MatchDao,
    private val inningsDao: InningsDao,
    private val ballDao: BallDao
) {
    val allTeamsFlow: Flow<List<Team>> = teamDao.getAllTeamsFlow()
    val allPlayersFlow: Flow<List<Player>> = playerDao.getAllPlayersFlow()
    val allMatchesFlow: Flow<List<Match>> = matchDao.getAllMatchesFlow()

    suspend fun getTeamById(id: Int): Team? = teamDao.getTeamById(id)

    fun getPlayersByTeamFlow(teamId: Int): Flow<List<Player>> = playerDao.getPlayersByTeamFlow(teamId)
    suspend fun getPlayersByTeam(teamId: Int): List<Player> = playerDao.getPlayersByTeam(teamId)

    fun getMatchFlowById(matchId: Int): Flow<Match?> = matchDao.getMatchFlowById(matchId)
    suspend fun getMatchById(matchId: Int): Match? = matchDao.getMatchById(matchId)

    fun getInningsForMatchFlow(matchId: Int): Flow<List<Innings>> = inningsDao.getInningsForMatchFlow(matchId)
    suspend fun getInningsForMatch(matchId: Int): List<Innings> = inningsDao.getInningsForMatch(matchId)
    suspend fun getInningsByNumber(matchId: Int, inningsNum: Int): Innings? = inningsDao.getInningsByNumber(matchId, inningsNum)

    fun getBallsForInningsFlow(inningsId: Int): Flow<List<Ball>> = ballDao.getBallsForInningsFlow(inningsId)
    suspend fun getBallsForInnings(inningsId: Int): List<Ball> = ballDao.getBallsForInnings(inningsId)

    // Team Operations
    suspend fun insertTeam(team: Team): Long = teamDao.insertTeam(team)
    suspend fun updateTeam(team: Team) = teamDao.updateTeam(team)
    suspend fun deleteTeam(team: Team) = teamDao.deleteTeam(team)

    // Player Operations
    suspend fun insertPlayer(player: Player): Long = playerDao.insertPlayer(player)
    suspend fun updatePlayer(player: Player) = playerDao.updatePlayer(player)
    suspend fun deletePlayer(player: Player) = playerDao.deletePlayer(player)

    // Match Operations
    suspend fun insertMatch(match: Match): Long = matchDao.insertMatch(match)
    suspend fun updateMatch(match: Match) = matchDao.updateMatch(match)
    suspend fun deleteMatch(match: Match) = matchDao.deleteMatch(match)

    // Innings Operations
    suspend fun insertInnings(innings: Innings): Long = inningsDao.insertInnings(innings)
    suspend fun updateInnings(innings: Innings) = inningsDao.updateInnings(innings)

    // Ball Operations
    suspend fun insertBall(ball: Ball): Long = ballDao.insertBall(ball)
    suspend fun deleteBall(ball: Ball) = ballDao.deleteBall(ball)
    suspend fun deleteLastBallOfInnings(inningsId: Int) = ballDao.deleteLastBallOfInnings(inningsId)

    // Prepopulate database with 8 actual mountain teams, rosters and 28 round-robin matches of the SJL
    suspend fun prepopulateIfEmpty() {
        val existingTeams = teamDao.getAllTeams()
        val existingPlayers = playerDao.getAllPlayers()
        
        // Detect if we have old, placeholder, or coaching players in the current state to force a re-seed
        val hasObsolete = existingPlayers.any { 
            it.name == "Gary Kirsten" || it.name == "Dav Whatmore" || it.name == "Misbah-ul-Haq" || it.name == "Ricky Ponting" || it.name == "Andy Flower" || it.name == "Stephen Fleming" || it.name == "Brendon McCullum" || it.name == "Mahela Jayawardene"
        }

        if (existingTeams.isNotEmpty() && existingPlayers.isNotEmpty() && !hasObsolete) return

        database.withTransaction {
            // Clean any old or obsolete team, player and match records before seeding to start clean
            if (existingTeams.isNotEmpty() || existingPlayers.isNotEmpty() || hasObsolete) {
                playerDao.getAllPlayers().forEach { playerDao.deletePlayer(it) }
                teamDao.getAllTeams().forEach { teamDao.deleteTeam(it) }
                matchDao.getAllMatches().forEach { matchDao.deleteMatch(it) }
            }

            // 1. Data classes for neat prepopulation representing the physical images of SJL Season 2
            class PlayerSeed(
                val name: String,
                val jerseyNumber: Int,
                val isCaptain: Boolean,
                val isWicketKeeper: Boolean,
                val specialRole: String
            )

            class TeamSeed(
                val name: String,
                val shortName: String,
                val colorHex: String,
                val homeGround: String,
                val players: List<PlayerSeed>
            )

            val defaultTeamsData = listOf(
                TeamSeed(
                    name = "Al Farooq Warriors",
                    shortName = "AFW",
                    colorHex = "#D97706", // Premium Amber Golden Orange
                    homeGround = "Samarbagh Arena",
                    players = listOf(
                        PlayerSeed("Talha hayat", 67, isCaptain = true, isWicketKeeper = true, specialRole = "WK-Batter"),
                        PlayerSeed("Haroon", 88, isCaptain = false, isWicketKeeper = false, specialRole = "Bowler"),
                        PlayerSeed("Hasnain", 87, isCaptain = false, isWicketKeeper = false, specialRole = "Bowler"),
                        PlayerSeed("Murtaza Lefty", 39, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Awais Zaada", 18, isCaptain = false, isWicketKeeper = false, specialRole = "Left Arm Bowler"),
                        PlayerSeed("Murtaza", 10, isCaptain = false, isWicketKeeper = false, specialRole = "Left Arm Bowler"),
                        PlayerSeed("Zaid Khan", 63, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Awais Rahat", 68, isCaptain = false, isWicketKeeper = false, specialRole = "All-Rounder"),
                        PlayerSeed("Saad", 16, isCaptain = false, isWicketKeeper = true, specialRole = "WK-Batter"),
                        PlayerSeed("Rehan Shakir", 11, isCaptain = false, isWicketKeeper = false, specialRole = "All-Rounder"),
                        PlayerSeed("Imad Khan", 67, isCaptain = false, isWicketKeeper = false, specialRole = "All-Rounder"),
                        PlayerSeed("M.Farman", 322, isCaptain = false, isWicketKeeper = false, specialRole = "Fast Bowler"),
                        PlayerSeed("Anwar Ali", 48, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Mawiya", 71, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Sahil", 8, isCaptain = false, isWicketKeeper = false, specialRole = "Fast Bowler")
                    )
                ),
                TeamSeed(
                    name = "Samarbagh SC Junior",
                    shortName = "SSCJ",
                    colorHex = "#10B981", // Emerald Green
                    homeGround = "Samarbagh Sports Club",
                    players = listOf(
                        PlayerSeed("Abdullah Shah", 8, isCaptain = true, isWicketKeeper = false, specialRole = "All-Rounder"),
                        PlayerSeed("Ab waleed", 17, isCaptain = false, isWicketKeeper = true, specialRole = "WK-Batter"),
                        PlayerSeed("Hasenin Hasso", 3, isCaptain = false, isWicketKeeper = false, specialRole = "All-Rounder"),
                        PlayerSeed("Fazal", 4, isCaptain = false, isWicketKeeper = false, specialRole = "Fast Bowler"),
                        PlayerSeed("Ashraf Khan", 77, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Rizwan", 33, isCaptain = false, isWicketKeeper = false, specialRole = "All-Rounder"),
                        PlayerSeed("Omais", 18, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Batiullah", 30, isCaptain = false, isWicketKeeper = false, specialRole = "Spinner"),
                        PlayerSeed("Sana Sany", 9, isCaptain = false, isWicketKeeper = false, specialRole = "Spinner"),
                        PlayerSeed("Zarar Khan", 5, isCaptain = false, isWicketKeeper = false, specialRole = "Fast Bowler"),
                        PlayerSeed("Huriara", 16, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Umair Khan", 29, isCaptain = false, isWicketKeeper = true, specialRole = "WK-Batter"),
                        PlayerSeed("Nadeem", 17, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Abbas Khan", 56, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Bakar King", 150, isCaptain = false, isWicketKeeper = false, specialRole = "Batter")
                    )
                ),
                TeamSeed(
                    name = "Shalimar Zalmi",
                    shortName = "SZA",
                    colorHex = "#EAB308", // Golden Yellow
                    homeGround = "Shalimar Venue",
                    players = listOf(
                        PlayerSeed("Khalid Zamon", 56, isCaptain = true, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Qudrat Bacha", 13, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Mirwais Ali", 13, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Talha Sudais", 145, isCaptain = false, isWicketKeeper = false, specialRole = "Fast Bowler"),
                        PlayerSeed("Awais Khan", 87, isCaptain = false, isWicketKeeper = false, specialRole = "Fast Bowler"),
                        PlayerSeed("Huzaifa Khan", 5, isCaptain = false, isWicketKeeper = false, specialRole = "Fast Bowler"),
                        PlayerSeed("Waleed Rahul", 1, isCaptain = false, isWicketKeeper = true, specialRole = "Wicket-Keeper"),
                        PlayerSeed("Fawad", 100, isCaptain = false, isWicketKeeper = false, specialRole = "All-Rounder"),
                        PlayerSeed("Murad Anwar", 39, isCaptain = false, isWicketKeeper = false, specialRole = "Spinner"),
                        PlayerSeed("Hamza Zahoor", 16, isCaptain = false, isWicketKeeper = true, specialRole = "Wicket-Keeper"),
                        PlayerSeed("M-Talha", 77, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Zaka Ullah", 21, isCaptain = false, isWicketKeeper = false, specialRole = "All-Rounder"),
                        PlayerSeed("M.Talha", 7, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Nihad Khan", 8, isCaptain = false, isWicketKeeper = false, specialRole = "Fast Bowler"),
                        PlayerSeed("Sahil Saeed", 32, isCaptain = false, isWicketKeeper = false, specialRole = "Fast Bowler"),
                        PlayerSeed("Momand", 5, isCaptain = false, isWicketKeeper = false, specialRole = "All-Rounder")
                    )
                ),
                TeamSeed(
                    name = "Shahid Cricket Academy",
                    shortName = "SCA",
                    colorHex = "#EF4444", // Royal Red
                    homeGround = "Shahid Village Oval",
                    players = listOf(
                        PlayerSeed("Junaid", 8, isCaptain = true, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Abbas", 56, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Shahid", 333, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Mashal", 13, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Umar", 47, isCaptain = false, isWicketKeeper = false, specialRole = "Bowler"),
                        PlayerSeed("Shahzad", 12, isCaptain = false, isWicketKeeper = false, specialRole = "Bowler"),
                        PlayerSeed("Bilal Shah", 15, isCaptain = false, isWicketKeeper = false, specialRole = "Bowler"),
                        PlayerSeed("Maaz Khan", 14, isCaptain = false, isWicketKeeper = false, specialRole = "Leg Spinner"),
                        PlayerSeed("M Aziz", 57, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Ihtisham", 68, isCaptain = false, isWicketKeeper = true, specialRole = "WK-Batter"),
                        PlayerSeed("Ahmad Hussain", 55, isCaptain = false, isWicketKeeper = false, specialRole = "All-Rounder"),
                        PlayerSeed("Amjad Hashir", 10, isCaptain = false, isWicketKeeper = false, specialRole = "Bowler"),
                        PlayerSeed("M Uzair", 2, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Shoaib", 61, isCaptain = false, isWicketKeeper = false, specialRole = "Leg Spinner"),
                        PlayerSeed("Anas Babar", 56, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Sayyab jani", 8, isCaptain = false, isWicketKeeper = false, specialRole = "All-Rounder")
                    )
                ),
                TeamSeed(
                    name = "FS Sports Samarbagh",
                    shortName = "FSS",
                    colorHex = "#3B82F6", // Athletic Blue
                    homeGround = "FS Sports Ground",
                    players = listOf(
                        PlayerSeed("Wesim", 50, isCaptain = true, isWicketKeeper = false, specialRole = "All-Rounder"),
                        PlayerSeed("Asad", 8, isCaptain = false, isWicketKeeper = false, specialRole = "Bowler"),
                        PlayerSeed("Rustam", 88, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("M Salman", 57, isCaptain = false, isWicketKeeper = false, specialRole = "Fast Bowler"),
                        PlayerSeed("Zarar", 143, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Faisal Khan", 87, isCaptain = false, isWicketKeeper = true, specialRole = "Wicket-Keeper"),
                        PlayerSeed("Bilal Ahmad", 13, isCaptain = false, isWicketKeeper = false, specialRole = "Spinner"),
                        PlayerSeed("Abdullah maruf", 22, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("M - Riaz", 32, isCaptain = false, isWicketKeeper = false, specialRole = "Fast Bowler"),
                        PlayerSeed("M - Imran", 74, isCaptain = false, isWicketKeeper = false, specialRole = "Left-Arm Spinner"),
                        PlayerSeed("Aizaz khan", 8, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Nawia", 71, isCaptain = false, isWicketKeeper = false, specialRole = "Fast Bowler"),
                        PlayerSeed("M Shoaib", 58, isCaptain = false, isWicketKeeper = false, specialRole = "Fast Bowler"),
                        PlayerSeed("Huzaifa sherwani", 22, isCaptain = false, isWicketKeeper = false, specialRole = "Fast Bowler")
                    )
                ),
                TeamSeed(
                    name = "Malik Intl Academy",
                    shortName = "MICA",
                    colorHex = "#1E3A8A", // Deep Navy Blue
                    homeGround = "Malik Academy Field",
                    players = listOf(
                        PlayerSeed("Ali Imtaiz", 10, isCaptain = true, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("M Talha", 12, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("M Faheem", 88, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Huzaifa", 7, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Nizamuddin", 33, isCaptain = false, isWicketKeeper = true, specialRole = "Wicket-Keeper"),
                        PlayerSeed("M Irfan", 71, isCaptain = false, isWicketKeeper = false, specialRole = "Bowler"),
                        PlayerSeed("M Amir", 17, isCaptain = false, isWicketKeeper = false, specialRole = "Bowler"),
                        PlayerSeed("Ubaid Ullah", 13, isCaptain = false, isWicketKeeper = false, specialRole = "Bowler"),
                        PlayerSeed("Saad Afridi", 3, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Muntazir", 63, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Anas Afridi", 18, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Zain Ullah", 44, isCaptain = false, isWicketKeeper = false, specialRole = "Bowler"),
                        PlayerSeed("M Nasir", 9, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("M Arif", 8, isCaptain = false, isWicketKeeper = false, specialRole = "Bowler"),
                        PlayerSeed("M Zaid", 7, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("M Umar", 45, isCaptain = false, isWicketKeeper = false, specialRole = "All-Rounder")
                    )
                ),
                TeamSeed(
                    name = "Samarbagh Badshah",
                    shortName = "SBD",
                    colorHex = "#8B5CF6", // Royal Violet
                    homeGround = "Badshah Cricket Arena",
                    players = listOf(
                        PlayerSeed("Jalal Sultani", 7, isCaptain = true, isWicketKeeper = false, specialRole = "All-Rounder"),
                        PlayerSeed("Malak Safi", 17, isCaptain = false, isWicketKeeper = true, specialRole = "Wicket-Keeper"),
                        PlayerSeed("Zia Ullah", 18, isCaptain = false, isWicketKeeper = false, specialRole = "Fast Bowler"),
                        PlayerSeed("Umair Shah", 22, isCaptain = false, isWicketKeeper = false, specialRole = "Fast Bowler"),
                        PlayerSeed("Imad khan", 9, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Aziz Ahmad", 21, isCaptain = false, isWicketKeeper = false, specialRole = "All-Rounder"),
                        PlayerSeed("Abbas Saced", 18, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Yaseen Khan", 7, isCaptain = false, isWicketKeeper = false, specialRole = "All-Rounder"),
                        PlayerSeed("Abdullah Sohil", 12, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Rehan", 7, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Mursalin Khan", 10, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Bilwal", 51, isCaptain = false, isWicketKeeper = false, specialRole = "Leg Spinner"),
                        PlayerSeed("Uzair", 46, isCaptain = false, isWicketKeeper = false, specialRole = "Left Spinner"),
                        PlayerSeed("Ahmad", 57, isCaptain = false, isWicketKeeper = false, specialRole = "All-Rounder"),
                        PlayerSeed("Bilal", 10, isCaptain = false, isWicketKeeper = false, specialRole = "Fast Bowler")
                    )
                ),
                TeamSeed(
                    name = "Malak Kingsman",
                    shortName = "MKM",
                    colorHex = "#059669", // Emerald Forest Green
                    homeGround = "Malak Sports Ground",
                    players = listOf(
                        PlayerSeed("Rizwan King", 18, isCaptain = true, isWicketKeeper = false, specialRole = "All-Rounder"),
                        PlayerSeed("Ihsan Salia", 21, isCaptain = false, isWicketKeeper = true, specialRole = "All-Rounder"),
                        PlayerSeed("Malak Hanzala", 10, isCaptain = false, isWicketKeeper = false, specialRole = "Bowler"),
                        PlayerSeed("Muhammad", 16, isCaptain = false, isWicketKeeper = false, specialRole = "All-Rounder"),
                        PlayerSeed("Homdullah", 8, isCaptain = false, isWicketKeeper = false, specialRole = "All-Rounder"),
                        PlayerSeed("M. Ali", 18, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Usama", 45, isCaptain = false, isWicketKeeper = false, specialRole = "Bowler"),
                        PlayerSeed("Sudais Khan", 57, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Imdadullah", 15, isCaptain = false, isWicketKeeper = false, specialRole = "All-Rounder"),
                        PlayerSeed("Adnan Qazi", 56, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Wajid", 71, isCaptain = false, isWicketKeeper = false, specialRole = "Bowler"),
                        PlayerSeed("Farhan", 56, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Hayyan Babar", 55, isCaptain = false, isWicketKeeper = false, specialRole = "Batter"),
                        PlayerSeed("Rahat Ullah", 87, isCaptain = false, isWicketKeeper = false, specialRole = "Bowler"),
                        PlayerSeed("Izaz Khan", 18, isCaptain = false, isWicketKeeper = false, specialRole = "All-Rounder")
                    )
                )
            )

            val insertedTeamIds = mutableListOf<Int>()
            for (seed in defaultTeamsData) {
                val team = Team(
                    name = seed.name,
                    shortName = seed.shortName,
                    colorHex = seed.colorHex,
                    homeGround = seed.homeGround
                )
                val teamId = teamDao.insertTeam(team).toInt()
                insertedTeamIds.add(teamId)

                for (pSeed in seed.players) {
                    playerDao.insertPlayer(
                        Player(
                            teamId = teamId,
                            name = pSeed.name,
                            jerseyNumber = pSeed.jerseyNumber,
                            isCaptain = pSeed.isCaptain,
                            isWicketKeeper = pSeed.isWicketKeeper,
                            specialRole = pSeed.specialRole
                        )
                    )
                }
            }

        // 3. Generate 28 Round-Robin fixtures scheduled starting from May 21, 2026
        val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val calendar = Calendar.getInstance()
        calendar.set(2026, Calendar.MAY, 21)

        var matchIndex = 1
        for (i in 0 until insertedTeamIds.size) {
            for (j in i + 1 until insertedTeamIds.size) {
                val team1Id = insertedTeamIds[i]
                val team2Id = insertedTeamIds[j]
                val matchDate = simpleDateFormat.format(calendar.time)
                calendar.add(Calendar.DAY_OF_YEAR, 1) // Next match next day

                val venue = when (matchIndex % 4) {
                    0 -> defaultTeamsData[i].homeGround
                    1 -> defaultTeamsData[j].homeGround
                    2 -> "Timergara Sports Stadium"
                    else -> "Samarbagh Cricket Ground"
                }

                matchDao.insertMatch(
                    Match(
                        team1Id = team1Id,
                        team2Id = team2Id,
                        totalOvers = 15, // 15 overs default
                        venue = venue,
                        date = matchDate,
                        tossWinnerId = team1Id,
                        tossChoice = "bat",
                        status = "upcoming",
                        winnerId = null,
                        resultDescription = "Match scheduled"
                    )
                )
                matchIndex++
            }
        }
        }
    }
}
