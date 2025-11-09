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
 *
 * ATUALIZADO: getTopSoloScores agora seleciona e retorna os campos extras.
 */
public class SoloScoreDAO {

    private Connection connection;

    public SoloScoreDAO() {
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    /**
     * Adiciona uma nova pontuação 1P (SoloScore) associada a um UserID.
     * ATUALIZADO: Salva também Nível, Linhas e Tetris Count.
     * @param userID O ID do usuário.
     * @param score A pontuação.
     * @param level O nível alcançado.
     * @param lines O total de linhas limpas.
     * @param tetrisCount O total de "Tetris" (4 linhas).
     */
    public void addScore(int userID, int score, int level, int lines, int tetrisCount) {
        if (score <= 0) {
            return; // Não salva pontuação zero
        }

        // SQL ATUALIZADO: Insere os novos campos
        String sql = "INSERT INTO SoloScores (UserID, Score, Level, LinesCleared, TetrisCount) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, userID);
            pstmt.setInt(2, score);
            pstmt.setInt(3, level);        // <-- ADICIONADO
            pstmt.setInt(4, lines);        // <-- ADICIONADO
            pstmt.setInt(5, tetrisCount);  // <-- ADICIONADO
            pstmt.executeUpdate();
            
            System.out.println("SoloScoreDAO: Pontuação 1P de " + score + " salva para UserID " + userID);

        } catch (SQLException e) {
            System.err.println("Erro ao salvar pontuação 1P no banco: " + e.getMessage());
        }
    }

    /**
     * Retorna as X melhores pontuações 1P do banco (com nomes).
     * ATUALIZADO: Retorna apenas a MAIOR pontuação de cada usuário.
     * @param limit O número de pontuações a retornar (ex: 10).
     * @return Uma lista de SoloScoreEntry.
     */
    public List<SoloScoreEntry> getTopSoloScores(int limit) {
        List<SoloScoreEntry> topScores = new ArrayList<>();
        
        // --- CONSULTA ATUALIZADA ---
        // Pega os novos campos (Level, LinesCleared, TetrisCount)
        String sql = "WITH RankedScores AS (" +
                     "    SELECT UserID, Score, Level, LinesCleared, TetrisCount, DateAchieved, " + // <-- CAMPOS ADICIONADOS
                     "    ROW_NUMBER() OVER(PARTITION BY UserID ORDER BY Score DESC) as rn " +
                     "    FROM SoloScores" +
                     ") " +
                     "SELECT TOP (?) p.Username, rs.Score, rs.Level, rs.LinesCleared, rs.TetrisCount, rs.DateAchieved " + // <-- CAMPOS ADICIONADOS
                     "FROM RankedScores rs " +
                     "JOIN PlayerProfiles p ON rs.UserID = p.UserID " +
                     "WHERE rs.rn = 1 AND rs.Score > 0 " +
                     "ORDER BY rs.Score DESC";
        // --- FIM DA ATUALIZAÇÃO ---

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit); // TOP (?)
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String username = rs.getString("Username");
                    int score = rs.getInt("Score");
                    int level = rs.getInt("Level");               // <-- ADICIONADO
                    int lines = rs.getInt("LinesCleared");      // <-- ADICIONADO
                    int tetris = rs.getInt("TetrisCount");      // <-- ADICIONADO
                    Date date = rs.getTimestamp("DateAchieved"); 
                    
                    // CORREÇÃO (Erro 3): Chama o construtor de 6 argumentos
                    topScores.add(new SoloScoreEntry(username, score, level, lines, tetris, date));
                }
            }

        } catch (SQLException e) {
            System.err.println("Erro ao ler ranking 1P do banco: " + e.getMessage());
        }
        
        return topScores;
    }
}