package com.tetris.model;

/**
 * Define as formas dos Tetrominós e armazena a tabela de coordenadas
 * relativas para cada bloco que compõe uma peça.
 */
public class Shape {

    /**
     * Enumeração de todas as formas de peças possíveis, incluindo
     * estados especiais como 'NoShape' (vazio) e 'GarbageShape' (lixo).
     */
    public enum Tetrominoe {
        NoShape, ZShape, SShape, LineShape, TShape, SquareShape, LShape, MirroredLShape,
        GarbageShape
    }

    // Tabela de coordenadas [Shape][Bloco][x,y]
    private static int[][][] coordsTable;

    static {
        coordsTable = new int[][][] {
            { { 0, 0 },  { 0, 0 },  { 0, 0 },  { 0, 0 } }, // NoShape
            { { 0, -1 }, { 0, 0 },  { -1, 0 }, { -1, 1 } }, // ZShape
            { { 0, -1 }, { 0, 0 },  { 1, 0 },  { 1, 1 } },  // SShape
            { { 0, -1 }, { 0, 0 },  { 0, 1 },  { 0, 2 } },  // LineShape
            { { -1, 0 }, { 0, 0 },  { 1, 0 },  { 0, 1 } },  // TShape
            { { 0, 0 },  { 1, 0 },  { 0, 1 },  { 1, 1 } },  // SquareShape
            { { -1, -1 },{ 0, -1 }, { 0, 0 },  { 0, 1 } },  // LShape
            { { 1, -1 }, { 0, -1 }, { 0, 0 },  { 0, 1 } },  // MirroredLShape
            { { 0, 0 },  { 0, 0 },  { 0, 0 },  { 0, 0 } }  // GarbageShape (coordenadas dummy, não é rotacionável)
        };
    }

    public static int[][][] getCoordsTable() {
        return coordsTable;
    }
}