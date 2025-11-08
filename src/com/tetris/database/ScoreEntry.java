package com.tetris.database;

import java.util.Date;

/**
 * Representa uma única entrada no ranking (lida do banco).
 * Contém o nome do usuário (da tabela Users) e a pontuação (da tabela Scores).
 */
public record ScoreEntry(String username, int score, Date date) {}