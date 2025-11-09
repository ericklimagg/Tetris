package com.tetris.database;

import java.util.Date;

/**
 * Representa uma entrada no histórico de partidas de um jogador.
 */
public record MatchHistoryEntry(
    boolean isWin,
    String opponentName,
    int playerScore,
    int opponentScore,
    Date datePlayed
) {}