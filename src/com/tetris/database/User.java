package com.tetris.database;

/**
 * Representa um único usuário (jogador) no banco de dados.
 * Usamos um 'record' para uma classe de dados simples e imutável.
 */
public record User(int userID, String username) {}