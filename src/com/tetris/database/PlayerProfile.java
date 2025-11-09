package com.tetris.database;

/**
 * Representa o Perfil de um Jogador (mapeamento da tabela PlayerProfiles).
 * Usamos uma classe (e não um record) pois as estatísticas são mutáveis
 * e podem ser atualizadas durante a sessão do jogo (embora esta classe
 * sirva primariamente como um DTO - Data Transfer Object).
 */
public class PlayerProfile {

    private int userID;
    private String username;
    private int gamesPlayed1P;
    private int highScore1P;
    private long totalScore_1P;
    private int gamesPlayed2P;
    private int wins2P;
    private int losses_2P;
    
    /**
     * Construtor completo para criar uma instância do perfil do jogador.
     */
    public PlayerProfile(int userID, String username, int gamesPlayed1P, int highScore1P, long totalScore_1P, int gamesPlayed2P, int wins2P, int losses_2P) {
        this.userID = userID;
        this.username = username;
        this.gamesPlayed1P = gamesPlayed1P;
        this.highScore1P = highScore1P;
        this.totalScore_1P = totalScore_1P;
        this.gamesPlayed2P = gamesPlayed2P;
        this.wins2P = wins2P;
        this.losses_2P = losses_2P;
    }

    // --- Getters (usados pelo GameController e DAOs) ---
    
    public int getUserID() { return userID; }
    public String getUsername() { return username; }
    public int getHighScore1P() { return highScore1P; }
    public int getWins2P() { return wins2P; }
    
    // Outros getters (getTotalScore1P, getLosses2P, etc.) podem ser
    // adicionados se a UI precisar exibir estatísticas mais detalhadas.
}