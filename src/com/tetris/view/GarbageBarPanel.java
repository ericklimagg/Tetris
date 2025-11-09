package com.tetris.view;

import com.tetris.model.Board;
import com.tetris.model.Theme;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

/**
 * Um painel vertical estreito que exibe uma barra indicando
 * o "lixo" (garbage) pendente a ser recebido pelo jogador.
 */
public class GarbageBarPanel extends JPanel {

    private static final int BAR_WIDTH = 20;

    private Board board;
    private Theme currentTheme;
    private boolean isPlayerOne; // Usado para lógica futura (ex: posição)

    public GarbageBarPanel(boolean isPlayerOne) {
        this.isPlayerOne = isPlayerOne;
        this.currentTheme = Theme.AVAILABLE_THEMES[0];
        
        // Calcula a altura com base na altura do BoardPanel
        int height = new BoardPanel().getPreferredSize().height;
        setPreferredSize(new Dimension(BAR_WIDTH, height));
        
        setBackground(currentTheme.uiBackground());
    }

    /**
     * Atualiza a referência ao tabuleiro (Board) para obter dados de lixo.
     */
    public void updateBoard(Board board) {
        this.board = board;
    }
    
    /**
     * Atualiza o tema visual do painel.
     */
    public void updateTheme(Theme theme) {
        this.currentTheme = theme;
        setBackground(theme.uiBackground());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (board == null) {
            return;
        }
        
        int incomingGarbage = board.getIncomingGarbage();
        if (incomingGarbage == 0) {
            return; // Não desenha nada se não houver lixo
        }

        // A cor da barra fica mais vermelha quanto mais lixo houver
        int red = Math.min(255, 100 + incomingGarbage * 15);
        g.setColor(new Color(red, 50, 50));

        // Calcula a altura da barra (proporcional à altura do tabuleiro)
        int squareSize = getPreferredSize().height / Board.BOARD_HEIGHT;
        int barHeight = Math.min(getPreferredSize().height, incomingGarbage * squareSize);
        
        // Desenha a barra preenchendo de baixo para cima
        int y = getHeight() - barHeight;
        
        // (A lógica de isPlayerOne não está sendo usada para diferenciar o desenho,
        // mas está mantida caso o layout mude no futuro)
        g.fillRect(0, y, BAR_WIDTH, barHeight);
        
        // Desenha um contorno branco para dar definição
        g.setColor(Color.WHITE);
        g.drawRect(0, y, BAR_WIDTH - 1, barHeight - 1);
    }
}