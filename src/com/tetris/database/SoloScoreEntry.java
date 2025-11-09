package com.tetris.database;

import java.util.Date;

/**
 * Representa uma única entrada no ranking 1P (lida do banco).
 * ATUALIZADO: Agora inclui mais detalhes da partida.
 */
public record SoloScoreEntry(
    String username,
    int score,
    int level,
    int linesCleared,
    int tetrisCount,
    Date date
) {}