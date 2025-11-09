package com.tetris.view;

import com.tetris.model.Board;
import com.tetris.model.Piece;
import com.tetris.model.Shape;
import com.tetris.model.Theme;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.FontMetrics; 

/**
 * Painel lateral que exibe as informações de um jogador:
 * pontuação, nível, linhas, prévia da próxima peça e,
 * opcionalmente, o placar de vitórias (no modo 2P).
 */
public class InfoPanel extends JPanel {

    private static final int PANEL_WIDTH = 250;
    private static final int SQUARE_PREVIEW_SIZE = 20;

    private Board board; // Referência ao model do tabuleiro
    private Theme currentTheme;
    
    private boolean showVictories = false; // Controla a exibição do placar de vitórias
    private String playerName = null; 
    private int currentHighScore = 0; // Armazena o high score vindo do perfil

    public InfoPanel() {
        this.currentTheme = Theme.AVAILABLE_THEMES[0];
        setPreferredSize(new Dimension(PANEL_WIDTH, 1)); // Largura fixa, altura flexível
        setBackground(currentTheme.uiBackground());
    }

    /**
     * Atualiza a referência ao tabuleiro (Board) para obter dados de jogo.
     */
    public void updateInfo(Board board) {
        this.board = board;
    }
    
    /**
     * Atualiza o tema visual do painel.
     */
    public void updateTheme(Theme theme) {
        this.currentTheme = theme;
        setBackground(theme.uiBackground());
    }
    
    /**
     * Controla se o bloco "VITÓRIAS" deve ser exibido.
     * @param show true para exibir (modo 2P), false para ocultar (modo 1P).
     */
    public void setShowVictories(boolean show) {
        this.showVictories = show;
    }

    /**
     * Define o nome do jogador a ser exibido no topo do painel.
     */
    public void setPlayerName(String name) {
        this.playerName = name;
    }

    /**
     * Define o high score a ser exibido, recebido do GameController.
     * (O high score vem do perfil do jogador, não mais do Board).
     * @param score O high score do jogador.
     */
    public void setHighScore(int score) {
        this.currentHighScore = score;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Não desenha nada se o jogo ainda não começou
        if (board == null || !board.isStarted()) {
            return;
        }

        drawGameInfo((Graphics2D) g);
    }
    
    /**
     * Desenha todos os blocos de informação (Nome, Score, Nível, etc.).
     * @param g2d O contexto gráfico 2D.
     */
    private void drawGameInfo(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Define a cor do texto com base no brilho do fundo
        Color textColor = (currentTheme.uiBackground().getRed() < 128) ? Color.WHITE : Color.BLACK;
        
        int padding = 20;
        int blockWidth = PANEL_WIDTH - (2 * padding);
        int blockHeight = 60;
        int spacing = 15;
        
        int currentY = 40; // Posição Y inicial para desenhar

        // Desenha o nome do jogador (se definido)
        if (playerName != null && !playerName.isEmpty()) {
            g2d.setColor(Color.CYAN); 
            g2d.setFont(new Font("Consolas", Font.BOLD, 22));
            
            FontMetrics fm = g2d.getFontMetrics();
            int nameWidth = fm.stringWidth(playerName);
            g2d.drawString(playerName, (PANEL_WIDTH - nameWidth) / 2, currentY);
            
            currentY += 30; 
        }

        // Desenha o High Score (do perfil)
        currentY = drawInfoBlock(g2d, "HIGH SCORE", String.format("%06d", this.currentHighScore), padding, currentY, blockWidth, blockHeight, textColor);
        currentY += spacing;
        
        // Desenha Vitórias (apenas no modo 2P)
        if (this.showVictories) {
            currentY = drawInfoBlock(g2d, "VITÓRIAS", String.format("%03d", board.getWins()), padding, currentY, blockWidth, blockHeight, textColor);
            currentY += spacing;
        }
        
        // Desenha Pontuação
        currentY = drawInfoBlock(g2d, "PONTUAÇÃO", String.format("%06d", board.getScore()), padding, currentY, blockWidth, blockHeight, textColor);
        currentY += spacing;
        
        // Desenha blocos divididos (Nível e Linhas)
        int halfWidth = (blockWidth - spacing) / 2;
        drawInfoBlock(g2d, "NÍVEL", String.format("%02d", board.getLevel()), padding, currentY, halfWidth, blockHeight, textColor);
        drawInfoBlock(g2d, "LINHAS", String.format("%03d", board.getLinesCleared()), padding + halfWidth + spacing, currentY, halfWidth, blockHeight, textColor);
        currentY += blockHeight + spacing;

        // Desenha blocos divididos (Tetris e Peças)
        drawInfoBlock(g2d, "TETRIS", String.format("%03d", board.getTetrisCount()), padding, currentY, halfWidth, blockHeight, textColor);
        drawInfoBlock(g2d, "PEÇAS", String.format("%04d", board.getTotalPieces()), padding + halfWidth + spacing, currentY, halfWidth, blockHeight, textColor);
        currentY += blockHeight + spacing;

        // Desenha o painel de "Próxima Peça"
        currentY = drawNextPiecePanel(g2d, "PRÓXIMA PEÇA", padding, currentY, blockWidth, 110, textColor);
        
        // Desenha a dica de pausa no rodapé
        drawControlHintBlock(g2d, "PAUSA (P)", padding, getHeight() - 85, blockWidth, 60, textColor);
    }
    
    /**
     * Helper para desenhar um bloco de informação padrão com título e valor.
     */
    private int drawInfoBlock(Graphics2D g, String title, String value, int x, int y, int width, int height, Color textColor) {
        Color blockColor = currentTheme.uiBackground().darker();
        Color borderColor = currentTheme.uiBackground().brighter();

        // Fundo
        g.setColor(blockColor);
        g.fillRoundRect(x, y, width, height, 15, 15);
        
        // Borda
        g.setColor(borderColor);
        g.drawRoundRect(x, y, width, height, 15, 15);
        
        // Texto (Título)
        g.setColor(textColor);
        g.setFont(new Font("Consolas", Font.PLAIN, 14));
        g.drawString(title, x + 15, y + 22);
        
        // Texto (Valor)
        g.setFont(new Font("Consolas", Font.BOLD, 22));
        g.drawString(value, x + 15, y + 48);

        return y + height;
    }

    /**
     * Helper para desenhar o bloco que contém a prévia da próxima peça.
     */
    private int drawNextPiecePanel(Graphics2D g, String title, int x, int y, int width, int height, Color textColor) {
        Color blockColor = currentTheme.uiBackground().darker();
        Color borderColor = currentTheme.uiBackground().brighter();

        // Fundo e Borda
        g.setColor(blockColor);
        g.fillRoundRect(x, y, width, height, 15, 15);
        g.setColor(borderColor);
        g.drawRoundRect(x, y, width, height, 15, 15);

        // Título
        g.setColor(textColor);
        g.setFont(new Font("Consolas", Font.PLAIN, 14));
        g.drawString(title, x + 15, y + 22);
        
        // Desenha a peça
        Piece nextPiece = board.getNextPiece();
        if (nextPiece != null) {
            int previewX = x + (width / 2) - (2 * SQUARE_PREVIEW_SIZE);
            int previewY = y + 45; 
            for (int i = 0; i < 4; i++) {
                int px = previewX + (nextPiece.x(i) + 1) * SQUARE_PREVIEW_SIZE;
                int py = previewY + (1 - nextPiece.y(i)) * SQUARE_PREVIEW_SIZE;
                drawSquare(g, px, py, nextPiece.getShape(), SQUARE_PREVIEW_SIZE);
            }
        }
        return y + height;
    }

    /**
     * Helper para desenhar o bloco de dica de controle no rodapé.
     */
    private void drawControlHintBlock(Graphics2D g, String text, int x, int y, int width, int height, Color textColor) {
        Color blockColor = currentTheme.uiBackground().darker();
        Color borderColor = currentTheme.uiBackground().brighter();

        g.setColor(blockColor);
        g.fillRoundRect(x, y, width, height, 15, 15);
        g.setColor(borderColor);
        g.drawRoundRect(x, y, width, height, 15, 15);

        g.setColor(textColor);
        g.setFont(new Font("Consolas", Font.BOLD, 18));
        
        // Centraliza o texto
        int stringWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, x + (width - stringWidth) / 2, y + (height / 2) + 7);
    }

    /**
     * Helper para desenhar um único quadrado de peça (usado na prévia).
     */
    private void drawSquare(Graphics g, int x, int y, Shape.Tetrominoe shape, int size) {
        Color[] colors = currentTheme.pieceColors();
        Color color = colors[shape.ordinal()];

        g.setColor(color);
        g.fillRect(x + 1, y + 1, size - 2, size - 2);

        // Destaques 3D
        g.setColor(color.brighter());
        g.drawLine(x, y + size - 1, x, y);
        g.drawLine(x, y, x + size - 1, y);

        g.setColor(color.darker());
        g.drawLine(x + 1, y + size - 1, x + size - 1, y + size - 1);
        g.drawLine(x + size - 1, y + size - 1, x + size - 1, y + 1);
    }
}