package com.tetris.database;

/**
 * Representa o Perfil de um Jogador (a tabela PlayerProfiles).
 * Usamos uma classe (e não um record) pois as estatísticas
 * são mutáveis e atualizadas durante o jogo.
 *
 * ATUALIZADO: Adiciona todos os campos da tabela SQL.
 */
public class PlayerProfile {

    private int userID;
    private String username;
    private int gamesPlayed1P;
    private int highScore1P;
    private long totalScore_1P; // <-- CAMPO ADICIONADO
    private int gamesPlayed2P;
    private int wins2P;
    private int losses_2P;     // <-- CAMPO ADICIONADO
    
    // Construtor ATUALIZADO para 8 argumentos
    public PlayerProfile(int userID, String username, int gamesPlayed1P, int highScore1P, long totalScore_1P, int gamesPlayed2P, int wins2P, int losses_2P) {
        this.userID = userID;
        this.username = username;
        this.gamesPlayed1P = gamesPlayed1P;
        this.highScore1P = highScore1P;
        this.totalScore_1P = totalScore_1P; // <-- ADICIONADO
        this.gamesPlayed2P = gamesPlayed2P;
        this.wins2P = wins2P;
        this.losses_2P = losses_2P;     // <-- ADICIONADO
    }

    // Getters para o GameController usar
    public int getUserID() { return userID; }
    public String getUsername() { return username; }
    public int getHighScore1P() { return highScore1P; }
    public int getWins2P() { return wins2P; }
    
    // (Poderíamos adicionar mais getters para a tela de estatísticas, 
    // como getTotalScore1P() ou getLosses2P() se necessário no futuro)
}