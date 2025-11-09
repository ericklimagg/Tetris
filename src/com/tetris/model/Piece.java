package com.tetris.model;
import java.util.Random;

import com.tetris.model.Shape.Tetrominoe;

/**
 * Representa uma peça (tetrominó) individual.
 * Armazena sua forma, coordenadas relativas dos seus blocos e sua
 * posição (x, y) no tabuleiro. Também contém a lógica de rotação.
 */
public class Piece {

    private Shape.Tetrominoe pieceShape;
    private int[][] coords;
    private int x, y; // Posição (ponto de pivô) da peça no tabuleiro

    // Gerador de números aleatórios compartilhado para todas as peças.
    private static final Random randomGenerator = new Random();

    public Piece() {
        coords = new int[4][2];
        setShape(Shape.Tetrominoe.NoShape);
    }

    /**
     * Define a forma desta peça, atualizando as coordenadas relativas
     * com base na tabela de Shape.
     */
    public void setShape(Shape.Tetrominoe shape) {
        int[][][] coordsTable = Shape.getCoordsTable();

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 2; ++j) {
                coords[i][j] = coordsTable[shape.ordinal()][i][j];
            }
        }
        pieceShape = shape;
    }

    // --- Getters e Setters para posição no tabuleiro ---
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    // --- Getters e Setters para coordenadas relativas dos blocos ---
    private void setCoordX(int index, int val) { coords[index][0] = val; }
    private void setCoordY(int index, int val) { coords[index][1] = val; }
    public int x(int index) { return coords[index][0]; }
    public int y(int index) { return coords[index][1]; }
    public Shape.Tetrominoe getShape() { return pieceShape; }

    /**
     * Define a forma desta peça para um tetrominó aleatório (excluindo NoShape).
     */
    public void setRandomShape() {
        // Gera um índice entre 1 e 7 (inclusive)
        int x = Math.abs(randomGenerator.nextInt()) % 7 + 1; 
        Shape.Tetrominoe[] values = Shape.Tetrominoe.values();
        setShape(values[x]);
    }

    /**
     * Retorna a coordenada X relativa mais baixa (mais à esquerda).
     */
    public int minX() {
        int m = coords[0][0];
        for (int i = 1; i < 4; i++) m = Math.min(m, coords[i][0]);
        return m;
    }

    /**
     * Retorna a coordenada Y relativa mais baixa (mais "baixa" na tela).
     */
    public int minY() {
        int m = coords[0][1];
        for (int i = 1; i < 4; i++) m = Math.min(m, coords[i][1]);
        return m;
    }

    /**
     * Retorna uma nova Peça rotacionada 90 graus à esquerda (anti-horário).
     * A peça Quadrado (SquareShape) não é rotacionada.
     */
    public Piece rotateLeft() {
        if (pieceShape == Shape.Tetrominoe.SquareShape) return this;
        
        Piece result = new Piece();
        result.pieceShape = this.pieceShape;
        
        // Aplica a matriz de rotação anti-horária: (x, y) -> (-y, x)
        for (int i = 0; i < 4; i++) {
            result.setCoordX(i, -y(i));
            result.setCoordY(i, x(i));
        }
        return result;
    }

    /**
     * Retorna uma nova Peça rotacionada 90 graus à direita (horário).
     * A peça Quadrado (SquareShape) não é rotacionada.
     */
    public Piece rotateRight() {
        if (pieceShape == Shape.Tetrominoe.SquareShape) return this;
        
        Piece result = new Piece();
        result.pieceShape = this.pieceShape;
        
        // Aplica a matriz de rotação horária: (x, y) -> (y, -x)
        for (int i = 0; i < 4; i++) {
            result.setCoordX(i, y(i));
            result.setCoordY(i, -x(i));
        }
        return result;
    }
}