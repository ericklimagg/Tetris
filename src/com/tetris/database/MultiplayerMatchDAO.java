package com.tetris.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para a tabela MultiplayerMatches.
 */
public class MultiplayerMatchDAO {

    private Connection connection;

    public MultiplayerMatchDAO() {
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    public void recordMatch(int winnerID, int loserID, int winnerScore, int loserScore) {
        String sql = "INSERT INTO MultiplayerMatches (WinnerID, LoserID, WinnerScore, LoserScore) " +
                     "VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, winnerID);
            pstmt.setInt(2, loserID);
            pstmt.setInt(3, winnerScore);
            pstmt.setInt(4, loserScore);
            pstmt.executeUpdate();
            
            System.out.println("MultiplayerMatchDAO: Partida registrada - Vencedor: " + winnerID);

        } catch (SQLException e) {
            System.err.println("Erro ao registrar partida 2P: " + e.getMessage());
        }
    }

    public List<RankingEntry2P> getTopPlayers(int limit) {
        List<RankingEntry2P> ranking = new ArrayList<>();
        
        String sql = "SELECT TOP (?) " +
                     "    UserID, Username, Wins_2P, Losses_2P, GamesPlayed_2P, " +
                     "    CASE " +
                     "        WHEN GamesPlayed_2P > 0 " +
                     "        THEN CAST(Wins_2P AS FLOAT) / GamesPlayed_2P * 100 " +
                     "        ELSE 0 " +
                     "    END AS WinRate " +
                     "FROM PlayerProfiles " +
                     "WHERE GamesPlayed_2P > 0 " +
                     "ORDER BY Wins_2P DESC, WinRate DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String username = rs.getString("Username");
                    int wins = rs.getInt("Wins_2P");
                    int losses = rs.getInt("Losses_2P");
                    int gamesPlayed = rs.getInt("GamesPlayed_2P");
                    double winRate = rs.getDouble("WinRate");
                    
                    ranking.add(new RankingEntry2P(username, wins, losses, gamesPlayed, winRate));
                }
            }

        } catch (SQLException e) {
            System.err.println("Erro ao ler ranking 2P: " + e.getMessage());
        }
        
        return ranking;
    }
}