package com.tetris.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DAO para a tabela PlayerProfiles.
 * Lida com login, criação e atualização de estatísticas do perfil.
 */
public class PlayerProfileDAO {

    private Connection connection;

    public PlayerProfileDAO() {
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    /**
     * Tenta encontrar um perfil pelo nome de usuário.
     * Atualiza o 'LastLogin' se encontrar.
     * @param username O nome a procurar.
     * @return Um objeto PlayerProfile se encontrado, ou null se não.
     */
    public PlayerProfile findUserByUsername(String username) {
        // SQL para buscar o usuário E atualizar o LastLogin ao mesmo tempo
        String sql = "UPDATE PlayerProfiles SET LastLogin = GETDATE() " +
                     "OUTPUT inserted.UserID, inserted.Username, inserted.GamesPlayed_1P, " +
                     "inserted.HighScore_1P, inserted.GamesPlayed_2P, inserted.Wins_2P " +
                     "WHERE Username = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Usuário encontrado, login atualizado
                    return mapRowToPlayerProfile(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar/atualizar usuário: " + e.getMessage());
        }
        return null; // Não encontrado
    }

    /**
     * Tenta criar um novo perfil.
     * @param username O nome do novo usuário.
     * @return O objeto PlayerProfile do usuário recém-criado.
     */
    private PlayerProfile createPlayer(String username) {
        String sql = "INSERT INTO PlayerProfiles (Username) VALUES (?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();

            // Pega o UserID gerado pelo banco
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int newUserID = rs.getInt(1);
                    System.out.println("PlayerProfileDAO: Perfil '" + username + "' criado com ID " + newUserID);
                    // Retorna o perfil recém-criado (com estatísticas zeradas)
                    return new PlayerProfile(newUserID, username, 0, 0, 0, 0);
                }
            }
        } catch (SQLException e) {
            // Pode falhar se o 'Username' já existir (Unique constraint)
            System.err.println("Erro ao criar usuário (talvez já exista?): " + e.getMessage());
        }
        return null; // Falha na criação
    }

    /**
     * Método principal: Tenta encontrar um usuário. Se não existir, cria um.
     * @param username O nome do jogador.
     * @return O objeto PlayerProfile (existente ou recém-criado).
     */
    public PlayerProfile findOrCreatePlayer(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null; // Não permite nomes vazios
        }
        
        PlayerProfile user = findUserByUsername(username);
        
        if (user != null) {
            // Usuário já existe e LastLogin foi atualizado
            return user;
        } else {
            // Usuário não existe, vamos criar
            return createPlayer(username);
        }
    }

    /**
     * Atualiza as estatísticas de 1P de um jogador após uma partida.
     */
    public void updateStats1P(int userID, int finalScore) {
        // SQL robusto: Incrementa GamesPlayed e atualiza HighScore APENAS SE o novo score for maior
        String sql = "UPDATE PlayerProfiles " +
                     "SET GamesPlayed_1P = GamesPlayed_1P + 1, " +
                     "    HighScore_1P = CASE WHEN ? > HighScore_1P THEN ? ELSE HighScore_1P END " +
                     "WHERE UserID = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, finalScore);
            pstmt.setInt(2, finalScore);
            pstmt.setInt(3, userID);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar estatísticas 1P: " + e.getMessage());
        }
    }
    
    /**
     * Atualiza as estatísticas de 2P para vencedor e perdedor.
     */
    public void updateStats2P(int winnerUserID, int loserUserID) {
        // SQL para o Vencedor
        String sqlWinner = "UPDATE PlayerProfiles " +
                           "SET GamesPlayed_2P = GamesPlayed_2P + 1, " +
                           "    Wins_2P = Wins_2P + 1 " +
                           "WHERE UserID = ?";
        
        // SQL para o Perdedor
        String sqlLoser = "UPDATE PlayerProfiles " +
                          "SET GamesPlayed_2P = GamesPlayed_2P + 1 " +
                          "WHERE UserID = ?";
        
        try (PreparedStatement pstmtWinner = connection.prepareStatement(sqlWinner);
             PreparedStatement pstmtLoser = connection.prepareStatement(sqlLoser)) {
            
            // Atualiza Vencedor
            pstmtWinner.setInt(1, winnerUserID);
            pstmtWinner.executeUpdate();
            
            // Atualiza Perdedor
            pstmtLoser.setInt(1, loserUserID);
            pstmtLoser.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar estatísticas 2P: " + e.getMessage());
        }
    }

    /**
     * Helper para mapear um ResultSet para um objeto PlayerProfile.
     */
    private PlayerProfile mapRowToPlayerProfile(ResultSet rs) throws SQLException {
        return new PlayerProfile(
            rs.getInt("UserID"),
            rs.getString("Username"),
            rs.getInt("GamesPlayed_1P"),
            rs.getInt("HighScore_1P"),
            rs.getInt("GamesPlayed_2P"),
            rs.getInt("Wins_2P")
        );
    }
}