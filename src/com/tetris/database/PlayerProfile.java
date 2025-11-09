package com.tetris.database;

/**
 * Representa o Perfil de um Jogador (a tabela PlayerProfiles).
 * ATUALIZADO: Adiciona TotalScore_1P e Losses_2P.
 */
public class PlayerProfile {

    private int userID;
    private String username;
    private int gamesPlayed1P;
    private int highScore1P;
    private long totalScore1P;
    private int gamesPlayed2P;
    private int wins2P;
    private int losses2P;
    
    public PlayerProfile(int userID, String username, int gamesPlayed1P, int highScore1P, 
                        long totalScore1P, int gamesPlayed2P, int wins2P, int losses2P) {
        this.userID = userID;
        this.username = username;
        this.gamesPlayed1P = gamesPlayed1P;
        this.highScore1P = highScore1P;
        this.totalScore1P = totalScore1P;
        this.gamesPlayed2P = gamesPlayed2P;
        this.wins2P = wins2P;
        this.losses2P = losses2P;
    }

    public int getUserID() { return userID; }
    public String getUsername() { return username; }
    public int getGamesPlayed1P() { return gamesPlayed1P; }
    public int getHighScore1P() { return highScore1P; }
    public long getTotalScore1P() { return totalScore1P; }
    public int getGamesPlayed2P() { return gamesPlayed2P; }
    public int getWins2P() { return wins2P; }
    public int getLosses2P() { return losses2P; }
    
    public double getAverageScore1P() {
        if (gamesPlayed1P == 0) return 0.0;
        return (double) totalScore1P / gamesPlayed1P;
    }
    
    public double getWinRate2P() {
        if (gamesPlayed2P == 0) return 0.0;
        return ((double) wins2P / gamesPlayed2P) * 100.0;
    }
}