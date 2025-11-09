package com.tetris.model;

import java.awt.Color;

/**
 * Representa um tema visual para o jogo, contendo todas as cores necessárias.
 * Utiliza um 'record' para uma definição concisa e imutável de um tema.
 */
public record Theme(
    String name,
    Color uiBackground,
    Color boardBackground,
    Color grid,
    Color[] pieceColors // Array com 9 cores: NoShape, 7 peças, e GarbageShape.
) {
    // --- Temas Pré-definidos ---

    /**
     * O tema escuro padrão.
     */
    public static final Theme CLASSIC_DARK = new Theme(
        "Clássico Escuro",
        new Color(40, 40, 55),
        new Color(20, 20, 30),
        new Color(50, 50, 70),
        new Color[] {
            new Color(0, 0, 0),       // NoShape
            new Color(204, 102, 102), // ZShape
            new Color(102, 204, 102), // SShape
            new Color(102, 102, 204), // LineShape
            new Color(204, 204, 102), // TShape
            new Color(204, 102, 204), // SquareShape
            new Color(102, 204, 204), // LShape
            new Color(218, 170, 0),    // MirroredLShape
            new Color(80, 80, 80)     // GarbageShape
        }
    );

    /**
     * Um tema claro, com cores vibrantes.
     */
    public static final Theme LIGHT = new Theme(
        "Claro",
        new Color(220, 220, 230),
        new Color(240, 240, 255),
        new Color(200, 200, 210),
        new Color[] {
            new Color(0, 0, 0),       // NoShape
            new Color(255, 80, 80),    // ZShape
            new Color(80, 255, 80),    // SShape
            new Color(80, 80, 255),    // LineShape
            new Color(255, 255, 80),   // TShape
            new Color(255, 80, 255),   // SquareShape
            new Color(80, 255, 255),   // LShape
            new Color(255, 170, 0),    // MirroredLShape
            new Color(130, 130, 130)  // GarbageShape
        }
    );
    
    /**
     * Um tema retro que imita as cores clássicas do Game Boy.
     */
    public static final Theme RETRO_GB = new Theme(
        "Retro GB",
        new Color(155, 188, 15), // Fundo UI 
        new Color(195, 228, 55), // Fundo do tabuleiro
        new Color(135, 168, 15), // Grelha
         new Color[] {
            new Color(15, 56, 15), // Cor única para todas as peças
            new Color(15, 56, 15),
            new Color(15, 56, 15),
            new Color(15, 56, 15),
            new Color(15, 56, 15),
            new Color(15, 56, 15),
            new Color(15, 56, 15),
            new Color(15, 56, 15),
            new Color(15, 56, 15) // GarbageShape
        }
    );

    /**
     * Array que expõe todos os temas disponíveis para o Controller/View.
     */
    public static final Theme[] AVAILABLE_THEMES = { CLASSIC_DARK, LIGHT, RETRO_GB };
}