package com.tetris.database;

/**
 * Representa uma entrada no ranking 2P (lida do banco).
 * Contém o nome do jogador e seu número de vitórias.
 */
public record PlayerWinsEntry(String username, int wins) {}