package com.tetris.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList; 
import java.util.List;      

/**
 * DAO (Data Access Object) para a tabela PlayerProfiles.
 * Lida com login, criação e atualização de estatísticas de perfis de jogador.
 */
public class PlayerProfileDAO {

    private Connection connection;

    public PlayerProfileDAO() {
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    /**
     * Tenta encontrar um perfil pelo nome de usuário.
     * Se encontrar, atualiza o campo 'LastLogin' para a data/hora atual
     * e retorna o perfil completo.
     * * @param username O nome a procurar.
     * @return Um objeto PlayerProfile se encontrado, ou null se não.
     */
    public PlayerProfile findUserByUsername(String username) {
        // A cláusula OUTPUT é usada para retornar os dados atualizados
        // (incluindo o novo LastLogin) em uma única operação.
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
        return null; // Usuário não encontrado
    }

    /**
     * Cria um novo perfil de jogador no banco de dados.
     * @param username O nome do novo usuário.
     * @return O objeto PlayerProfile do usuário recém-criado, ou null se falhar.
     */
    private PlayerProfile createPlayer(String username) {
        String sql = "INSERT INTO PlayerProfiles (Username) VALUES (?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();

            // Obtém o UserID gerado automaticamente pelo banco
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int newUserID = rs.getInt(1);
                    System.out.println("PlayerProfileDAO: Perfil '" + username + "' criado com ID " + newUserID);
                    // Retorna um novo objeto de perfil com estatísticas zeradas
                    return new PlayerProfile(newUserID, username, 0, 0, 0L, 0, 0, 0);
                }
            }
        } catch (SQLException e) {
            // A falha mais comum aqui é uma violação da restrição UNIQUE (nome já existe)
            System.err.println("Erro ao criar usuário (talvez já exista?): " + e.getMessage());
        }
        return null; 
    }

    /**
     * Método principal de login/criação: Tenta encontrar um usuário.
     * Se não existir, cria um novo perfil com esse nome.
     * * @param username O nome do jogador.
     * @return O objeto PlayerProfile (existente ou recém-criado).
     */
    public PlayerProfile findOrCreatePlayer(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null; // Nomes vazios não são permitidos
        }
        
        PlayerProfile user = findUserByUsername(username);
        
        if (user != null) {
            return user; // Retorna o usuário existente (e já logado)
        } else {
            return createPlayer(username); // Tenta criar um novo
        }
    }

    /**
     * Atualiza as estatísticas de 1P de um jogador após o término de uma partida.
     * Incrementa jogos, soma ao total e atualiza o high score se necessário.
     */
    public void updateStats1P(int userID, int finalScore) {
        // Usa CASE para atualizar condicionalmente o HighScore_1P
        String sql = "UPDATE PlayerProfiles " +
                     "SET GamesPlayed_1P = GamesPlayed_1P + 1, " +
                     "    TotalScore_1P = TotalScore_1P + ?, " + 
                     "    HighScore_1P = CASE WHEN ? > HighScore_1P THEN ? ELSE HighScore_1P END " +
                     "WHERE UserID = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, finalScore); // para TotalScore_1P
            pstmt.setInt(2, finalScore); // para a condição CASE
            pstmt.setInt(3, finalScore); // para o valor CASE
            pstmt.setInt(4, userID);     
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erro ao atualizar estatísticas 1P: " + e.getMessage());
        }
    }
    
    /**
     * Atualiza as estatísticas de 2P para o vencedor e o perdedor
     * (incrementa jogos jogados para ambos, e vitória/derrota).
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
        
        // Executa ambas as atualizações (idealmente em uma transação,
        // mas aqui está simplificado)
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
     * Retorna os X melhores jogadores do modo 2P, ordenados por vitórias.
     * @param limit O número de jogadores a retornar (ex: 10).
     * @return Uma lista de PlayerWinsEntry (Nome, Vitórias).
     */
    public List<PlayerWinsEntry> getTopPlayerWins(int limit) {
        List<PlayerWinsEntry> topPlayers = new ArrayList<>();
        
        String sql = "SELECT TOP (?) Username, Wins_2P " +
                     "FROM PlayerProfiles " +
                     "WHERE Wins_2P > 0 " + // Não mostra jogadores com 0 vitórias
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

    /**
     * Retorna uma lista de todos os perfis de jogadores existentes.
     * Usado para popular a tela de seleção de perfil.
     * @return Lista de PlayerProfile.
     */
    public List<PlayerProfile> getAllPlayerProfiles() {
        List<PlayerProfile> allProfiles = new ArrayList<>();
        // A seleção de colunas deve ser idêntica à de 'findUserByUsername'
        // para garantir que 'mapRowToPlayerProfile' funcione corretamente.
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

    /**
     * Método utilitário para converter uma linha de ResultSet em um objeto PlayerProfile.
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