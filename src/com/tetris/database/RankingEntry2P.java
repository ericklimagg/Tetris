package com.tetris.database;

/**
 * Representa uma entrada no ranking 2P.
 */
public record RankingEntry2P(
    String username,
    int wins,
    int losses,
    int gamesPlayed,
    double winRate
) {}