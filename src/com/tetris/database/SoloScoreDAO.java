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
 * Lida com salvar pontuações 1P e ler o ranking 1P.
 */
public class SoloScoreDAO {

    private Connection connection;

    public SoloScoreDAO() {
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    /**
     * Adiciona uma nova pontuação 1P (SoloScore) associada a um UserID.
     * @param userID O ID do usuário que fez a pontuação.
     * @param score A pontuação.
     */
    public void addScore(int userID, int score) {
        if (score <= 0) {
            return; // Não salva pontuação zero
        }

        String sql = "INSERT INTO SoloScores (UserID, Score) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userID);
            pstmt.setInt(2, score);
            pstmt.executeUpdate();
            
            System.out.println("SoloScoreDAO: Pontuação 1P de " + score + " salva para UserID " + userID);

        } catch (SQLException e) {
            System.err.println("Erro ao salvar pontuação 1P no banco: " + e.getMessage());
        }
    }

    /**
     * Retorna as X melhores pontuações 1P do banco (com nomes).
     * @param limit O número de pontuações a retornar (ex: 10).
     * @return Uma lista de SoloScoreEntry.
     */
    public List<SoloScoreEntry> getTopSoloScores(int limit) {
        List<SoloScoreEntry> topScores = new ArrayList<>();
        
        // SQL Robusto: Faz um JOIN entre SoloScores e PlayerProfiles para pegar o nome
        String sql = "SELECT TOP (?) p.Username, s.Score, s.DateAchieved " +
                     "FROM SoloScores s " +
                     "JOIN PlayerProfiles p ON s.UserID = p.UserID " +
                     "ORDER BY s.Score DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit); // TOP (?)
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String username = rs.getString("Username");
                    int score = rs.getInt("Score");
                    Date date = rs.getTimestamp("DateAchieved"); 
                    
                    topScores.add(new SoloScoreEntry(username, score, date));
                }
            }

        } catch (SQLException e) {
            System.err.println("Erro ao ler ranking 1P do banco: " + e.getMessage());
        }
        
        return topScores;
    }
}