package com.tetris.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DAO para a tabela SoloScores.
 */
public class SoloScoreDAO {

    private Connection connection;

    public SoloScoreDAO() {
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    public void addScore(int userID, int score, int level, int linesCleared, int tetrisCount) {
        if (score <= 0) {
            return;
        }

        String sql = "INSERT INTO SoloScores (UserID, Score, Level, LinesCleared, TetrisCount) " +
                     "VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userID);
            pstmt.setInt(2, score);
            pstmt.setInt(3, level);
            pstmt.setInt(4, linesCleared);
            pstmt.setInt(5, tetrisCount);
            pstmt.executeUpdate();
            
            System.out.println("SoloScoreDAO: Pontuação 1P de " + score + " salva para UserID " + userID);

        } catch (SQLException e) {
            System.err.println("Erro ao salvar pontuação 1P no banco: " + e.getMessage());
        }
    }

    public List<SoloScoreEntry> getTopSoloScores(int limit) {
        List<SoloScoreEntry> topScores = new ArrayList<>();
        
        String sql = "SELECT TOP (?) " +
                     "    p.Username, s.Score, s.Level, s.LinesCleared, s.TetrisCount, s.DateAchieved " +
                     "FROM SoloScores s " +
                     "JOIN PlayerProfiles p ON s.UserID = p.UserID " +
                     "ORDER BY s.Score DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String username = rs.getString("Username");
                    int score = rs.getInt("Score");
                    int level = rs.getInt("Level");
                    int linesCleared = rs.getInt("LinesCleared");
                    int tetrisCount = rs.getInt("TetrisCount");
                    Date date = rs.getTimestamp("DateAchieved");
                    
                    topScores.add(new SoloScoreEntry(username, score, level, 
                                                     linesCleared, tetrisCount, date));
                }
            }

        } catch (SQLException e) {
            System.err.println("Erro ao ler ranking 1P do banco: " + e.getMessage());
        }
        
        return topScores;
    }
}