package com.tetris.database;

import java.util.Date;

/**
 * Representa uma única entrada no ranking 1P (lida do banco).
 * Contém o nome do jogador e os detalhes da pontuação.
 */
public record SoloScoreEntry(String username, int score, Date date) {}