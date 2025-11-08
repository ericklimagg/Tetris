package com.tetris.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Data Access Object (DAO) para a tabela Users.
 * Lida com a criação e busca de jogadores.
 */
public class UserDAO {

    private Connection connection;

    public UserDAO() {
        this.connection = DatabaseManager.getInstance().getConnection();
    }

    /**
     * Tenta encontrar um usuário pelo nome.
     * @param username O nome a procurar.
     * @return Um objeto User se encontrado, ou null se não.
     */
    public User findUserByUsername(String username) {
        String sql = "SELECT UserID, Username FROM Users WHERE Username = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Usuário encontrado
                    return new User(rs.getInt("UserID"), rs.getString("Username"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao buscar usuário: " + e.getMessage());
        }
        // Usuário não encontrado
        return null;
    }

    /**
     * Tenta criar um novo usuário.
     * @param username O nome do novo usuário.
     * @return O objeto User do usuário recém-criado.
     */
    private User createUser(String username) {
        String sql = "INSERT INTO Users (Username) VALUES (?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();

            // Pega o UserID gerado pelo banco
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int newUserID = rs.getInt(1);
                    System.out.println("UserDAO: Usuário '" + username + "' criado com ID " + newUserID);
                    return new User(newUserID, username);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erro ao criar usuário: " + e.getMessage());
        }
        return null; // Falha na criação
    }

    /**
     * O método principal: Tenta encontrar um usuário. Se não existir, cria um.
     * @param username O nome do jogador.
     * @return O objeto User (existente ou recém-criado).
     */
    public User findOrCreateUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null; // Não permite nomes vazios
        }
        
        User user = findUserByUsername(username);
        
        if (user != null) {
            // Usuário já existe
            return user;
        } else {
            // Usuário não existe, vamos criar
            return createUser(username);
        }
    }
}