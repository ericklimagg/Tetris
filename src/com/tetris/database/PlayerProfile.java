package com.tetris.database;

/**
 * Representa o Perfil de um Jogador (a tabela PlayerProfiles).
 * Usamos uma classe (e não um record) pois as estatísticas
 * são mutáveis e atualizadas durante o jogo.
 */
public class PlayerProfile {

    private int userID;
    private String username;
    private int gamesPlayed1P;
    private int highScore1P;
    private int gamesPlayed2P;
    private int wins2P;
    
    // Construtor usado pelo DAO ao ler do banco
    public PlayerProfile(int userID, String username, int gamesPlayed1P, int highScore1P, int gamesPlayed2P, int wins2P) {
        this.userID = userID;
        this.username = username;
        this.gamesPlayed1P = gamesPlayed1P;
        this.highScore1P = highScore1P;
        this.gamesPlayed2P = gamesPlayed2P;
        this.wins2P = wins2P;
    }

    // Getters para o GameController usar
    public int getUserID() { return userID; }
    public String getUsername() { return username; }
    public int getHighScore1P() { return highScore1P; }
    public int getWins2P() { return wins2P; }
    
    // (Poderíamos adicionar mais getters para a tela de estatísticas)
}