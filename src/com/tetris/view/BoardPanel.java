package com.tetris.view;

import com.tetris.model.Board;
import com.tetris.model.Piece;
import com.tetris.model.Shape;
import com.tetris.model.Theme;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

/**
 * Painel responsável por desenhar um único tabuleiro de jogo,
 * incluindo a grade, as peças fixas, a peça atual e a peça "fantasma" (ghost).
 */
public class BoardPanel extends JPanel {

    private Board board;
    private Theme currentTheme;
    
    // Define um tamanho fixo para os quadrados (pixels)
    private static final int SQUARE_SIZE = 40;

    public BoardPanel() {
        this.currentTheme = Theme.AVAILABLE_THEMES[0];
        // Define o tamanho preferido com base no tamanho do quadrado e nas dimensões do tabuleiro
        setPreferredSize(new Dimension(SQUARE_SIZE * Board.BOARD_WIDTH, SQUARE_SIZE * Board.BOARD_HEIGHT));
    }

    /**
     * Recebe a referência mais recente do estado do tabuleiro (Model).
     */
    public void updateBoard(Board board) {
        this.board = board;
    }

    /**
     * Atualiza o tema visual a ser usado para desenhar.
     */
    public void updateTheme(Theme theme) {
        this.currentTheme = theme;
    }

    /**
     * Método principal de renderização do Swing.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Se o jogo ainda não começou (ex: estamos no menu), não desenha
        // o fundo do tabuleiro. Isso permite que o OverlayPanel controle o fundo.
        if (board == null || !board.isStarted() || currentTheme == null) {
            return;
        }
        
        // Desenha os componentes do jogo ativo
        drawBoardBackground(g);
        drawGrid(g);
        drawPlacedPieces(g);
        drawGhostPiece(g); 
        drawCurrentPiece(g);
        drawLinedClearAnimation(g);
    }

    /**
     * Desenha o fundo sólido do tabuleiro.
     */
    private void drawBoardBackground(Graphics g) {
        g.setColor(currentTheme.boardBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /**
     * Desenha as linhas da grade.
     */
    private void drawGrid(Graphics g) {
        g.setColor(currentTheme.grid());
        int squareSize = getSquareSize();
        // Linhas verticais
        for (int i = 0; i <= Board.BOARD_WIDTH; i++) {
            g.drawLine(i * squareSize, 0, i * squareSize, getHeight());
        }
        // Linhas horizontais
        for (int i = 0; i <= Board.BOARD_HEIGHT; i++) {
            g.drawLine(0, i * squareSize, getWidth(), i * squareSize);
        }
    }

    /**
     * Desenha todas as peças que já foram fixadas na grade.
     */
    private void drawPlacedPieces(Graphics g) {
        int squareSize = getSquareSize();
        for (int i = 0; i < Board.BOARD_HEIGHT; i++) {
            
            // Pula o desenho de linhas que estão na animação de "flash"
            if (board.isAnimatingLineClear() && board.getLinesBeingCleared().contains(i)) {
                continue;
            }

            for (int j = 0; j < Board.BOARD_WIDTH; j++) {
                Shape.Tetrominoe shape = board.shapeAt(j, i);
                if (shape != Shape.Tetrominoe.NoShape) {
                    // Converte a coordenada (i) do modelo (de baixo para cima) para
                    // a coordenada (y) da view (de cima para baixo).
                    drawSquare(g, j * squareSize, (Board.BOARD_HEIGHT - 1 - i) * squareSize, shape, false);
                }
            }
        }
    }

    /**
     * Desenha a peça atualmente controlada pelo jogador.
     */
    private void drawCurrentPiece(Graphics g) {
        if (board.isAnimatingLineClear()) {
            return; // Não desenha a peça se estivermos na animação de limpeza
        }
        
        Piece currentPiece = board.getCurrentPiece();
        if (board.isStarted() && currentPiece.getShape() != Shape.Tetrominoe.NoShape) {
            int squareSize = getSquareSize();
            for (int i = 0; i < 4; i++) {
                int x = currentPiece.getX() + currentPiece.x(i);
                int y = currentPiece.getY() - currentPiece.y(i);
                
                // Desenha apenas blocos que estão dentro da altura visível
                if (y < Board.BOARD_HEIGHT) { 
                    drawSquare(g, x * squareSize, (Board.BOARD_HEIGHT - 1 - y) * squareSize, currentPiece.getShape(), false);
                }
            }
        }
    }

    /**
     * Desenha a prévia (fantasma) de onde a peça cairá.
     */
    private void drawGhostPiece(Graphics g) {
        if (!board.isGhostPieceEnabled() || !board.isStarted() || board.isAnimatingLineClear()) {
            return;
        }
        
        Piece currentPiece = board.getCurrentPiece();
        if (currentPiece.getShape() == Shape.Tetrominoe.NoShape) {
            return;
        }

        int ghostY = board.getGhostPieceY(); // Posição Y final da peça
        int squareSize = getSquareSize();

        for (int i = 0; i < 4; i++) {
            int x = currentPiece.getX() + currentPiece.x(i);
            int y = ghostY - currentPiece.y(i);
             if (y < Board.BOARD_HEIGHT) {
                drawSquare(g, x * squareSize, (Board.BOARD_HEIGHT - 1 - y) * squareSize, currentPiece.getShape(), true);
            }
        }
    }

    /**
     * Desenha a animação de "flash" branco ao limpar linhas.
     */
    private void drawLinedClearAnimation(Graphics g) {
        if (!board.isAnimatingLineClear()) {
            return;
        }

        int timer = board.getLineClearTimer();
        
        // Alterna a cor do flash (branco / cor de fundo)
        Color flashColor = (timer % 2 == 0) ? Color.WHITE : currentTheme.boardBackground();
        g.setColor(flashColor);

        int squareSize = getSquareSize();
        for (int row : board.getLinesBeingCleared()) {
            int y = (Board.BOARD_HEIGHT - 1 - row) * squareSize;
            g.fillRect(0, y, getWidth(), squareSize); // Desenha um retângulo sobre a linha
        }
    }


    /**
     * Helper centralizado para desenhar um único quadrado de peça ou fantasma.
     * @param g Graphics context
     * @param x Coordenada X (em pixels) do canto superior esquerdo
     * @param y Coordenada Y (em pixels) do canto superior esquerdo
     * @param shape O tipo de peça (para definir a cor)
     * @param isGhost Se true, desenha apenas um contorno (peça fantasma)
     */
    private void drawSquare(Graphics g, int x, int y, Shape.Tetrominoe shape, boolean isGhost) {
        int size = getSquareSize();
        Color[] colors = currentTheme.pieceColors();
        Color color = colors[shape.ordinal()];

        if (isGhost) {
            // Desenha apenas o contorno para a peça fantasma
            g.setColor(color.darker()); 
            g.drawRect(x + 1, y + 1, size - 2, size - 2); 
        } else {
            // Desenha o bloco sólido
            g.setColor(color);
            g.fillRect(x + 1, y + 1, size - 2, size - 2);

            // Adiciona destaques 3D (claro em cima/esquerda, escuro em baixo/direita)
            g.setColor(color.brighter());
            g.drawLine(x, y + size - 1, x, y);
            g.drawLine(x, y, x + size - 1, y);

            g.setColor(color.darker());
            g.drawLine(x + 1, y + size - 1, x + size - 1, y + size - 1);
            g.drawLine(x + size - 1, y + size - 1, x + size - 1, y + 1);
        }
    }
    
    /**
     * Retorna o tamanho fixo de um quadrado do tabuleiro.
     */
    private int getSquareSize() {
        return SQUARE_SIZE;
    }
}