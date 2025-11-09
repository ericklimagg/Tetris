package com.tetris.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList; 
import java.util.List;      

/**
 * DAO para a tabela PlayerProfiles.
 * Lida com login, criação e atualização de estatísticas do perfil.
 *
 * ATUALIZADO: Adicionado getAllPlayerProfiles()
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
        String sql = "UPDATE PlayerProfiles SET LastLogin = GETDATE() " +
                     "OUTPUT inserted.UserID, inserted.Username, inserted.GamesPlayed_1P, " +
                     "inserted.HighScore_1P, inserted.TotalScore_1P, " + 
                     "inserted.GamesPlayed_2P, inserted.Wins_2P, inserted.Losses_2P " + 
                     "WHERE Username = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToPlayerProfile(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar/atualizar usuário: " + e.getMessage());
        }
        return null; 
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

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int newUserID = rs.getInt(1);
                    System.out.println("PlayerProfileDAO: Perfil '" + username + "' criado com ID " + newUserID);
                    return new PlayerProfile(newUserID, username, 0, 0, 0L, 0, 0, 0);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao criar usuário (talvez já exista?): " + e.getMessage());
        }
        return null; 
    }

    /**
     * Método principal: Tenta encontrar um usuário. Se não existir, cria um.
     * @param username O nome do jogador.
     * @return O objeto PlayerProfile (existente ou recém-criado).
     */
    public PlayerProfile findOrCreatePlayer(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null; 
        }
        
        PlayerProfile user = findUserByUsername(username);
        
        if (user != null) {
            return user;
        } else {
            return createPlayer(username);
        }
    }

    /**
     * Atualiza as estatísticas de 1P de um jogador após uma partida.
     */
    public void updateStats1P(int userID, int finalScore) {
        String sql = "UPDATE PlayerProfiles " +
                     "SET GamesPlayed_1P = GamesPlayed_1P + 1, " +
                     "    TotalScore_1P = TotalScore_1P + ?, " + 
                     "    HighScore_1P = CASE WHEN ? > HighScore_1P THEN ? ELSE HighScore_1P END " +
                     "WHERE UserID = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, finalScore); 
            pstmt.setInt(2, finalScore); 
            pstmt.setInt(3, finalScore); 
            pstmt.setInt(4, userID);     
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar estatísticas 1P: " + e.getMessage());
        }
    }
    
    /**
     * Atualiza as estatísticas de 2P para vencedor e perdedor.
     */
    public void updateStats2P(int winnerUserID, int loserUserID) {
        String sqlWinner = "UPDATE PlayerProfiles " +
                           "SET GamesPlayed_2P = GamesPlayed_2P + 1, " +
                           "    Wins_2P = Wins_2P + 1 " +
                           "WHERE UserID = ?";
        
        String sqlLoser = "UPDATE PlayerProfiles " +
                          "SET GamesPlayed_2P = GamesPlayed_2P + 1, " +
                          "    Losses_2P = Losses_2P + 1 " +
                          "WHERE UserID = ?";
        
        try (PreparedStatement pstmtWinner = connection.prepareStatement(sqlWinner);
             PreparedStatement pstmtLoser = connection.prepareStatement(sqlLoser)) {
            
            pstmtWinner.setInt(1, winnerUserID);
            pstmtWinner.executeUpdate();
            
            pstmtLoser.setInt(1, loserUserID);
            pstmtLoser.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Erro ao atualizar estatísticas 2P: " + e.getMessage());
        }
    }

    /**
     * Retorna os X melhores jogadores 2P (por vitórias).
     * @param limit O número de jogadores a retornar.
     * @return Uma lista de PlayerWinsEntry.
     */
    public List<PlayerWinsEntry> getTopPlayerWins(int limit) {
        List<PlayerWinsEntry> topPlayers = new ArrayList<>();
        
        String sql = "SELECT TOP (?) Username, Wins_2P " +
                     "FROM PlayerProfiles " +
                     "WHERE Wins_2P > 0 " +
                     "ORDER BY Wins_2P DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    topPlayers.add(new PlayerWinsEntry(
                        rs.getString("Username"),
                        rs.getInt("Wins_2P")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao ler ranking 2P do banco: " + e.getMessage());
        }
        return topPlayers;
    }

    // --- NOVO MÉTODO ---
    /**
     * Retorna uma lista de todos os perfis de jogadores existentes.
     * @return Lista de PlayerProfile.
     */
    public List<PlayerProfile> getAllPlayerProfiles() {
        List<PlayerProfile> allProfiles = new ArrayList<>();
        // Usamos a mesma seleção de colunas do findUserByUsername para garantir
        // que o mapRowToPlayerProfile funcione.
        String sql = "SELECT UserID, Username, GamesPlayed_1P, HighScore_1P, " +
                     "TotalScore_1P, GamesPlayed_2P, Wins_2P, Losses_2P " +
                     "FROM PlayerProfiles ORDER BY Username";

        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                allProfiles.add(mapRowToPlayerProfile(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar todos os perfis: " + e.getMessage());
        }
        return allProfiles;
    }
    // --- FIM DO NOVO MÉTODO ---

    /**
     * Helper para mapear um ResultSet para um objeto PlayerProfile.
     */
    private PlayerProfile mapRowToPlayerProfile(ResultSet rs) throws SQLException {
        return new PlayerProfile(
            rs.getInt("UserID"),
            rs.getString("Username"),
            rs.getInt("GamesPlayed_1P"),
            rs.getInt("HighScore_1P"),
            rs.getLong("TotalScore_1P"), 
            rs.getInt("GamesPlayed_2P"),
            rs.getInt("Wins_2P"),
            rs.getInt("Losses_2P")      
        );
    }
}