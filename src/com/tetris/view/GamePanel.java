package com.tetris.view;

import com.tetris.controller.GameController;
import com.tetris.model.Theme;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

/**
 * Painel principal que contém e organiza os componentes visuais do jogo
 * (InfoPanel, BoardPanel, GarbageBarPanel) para 1 e 2 jogadores.
 * Utiliza um BoxLayout horizontal.
 */
public class GamePanel extends JPanel {

    // --- Componentes P1 ---
    private BoardPanel boardPanel1;
    private InfoPanel infoPanel1;
    private GarbageBarPanel garbageBar1; 
    
    // --- Componentes P2 ---
    private BoardPanel boardPanel2;
    private InfoPanel infoPanel2;
    private GarbageBarPanel garbageBar2; 

    public GamePanel() {
        initComponents();
    }

    /**
     * Inicializa e dispõe os componentes visuais dentro deste painel.
     */
    private void initComponents() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

        // Cria os componentes para o Jogador 1
        infoPanel1 = new InfoPanel();
        boardPanel1 = new BoardPanel();
        garbageBar1 = new GarbageBarPanel(true); // 'true' = barra à direita
        
        // Cria os componentes para o Jogador 2
        infoPanel2 = new InfoPanel();
        boardPanel2 = new BoardPanel();
        garbageBar2 = new GarbageBarPanel(false); // 'false' = barra à esquerda

        // Adiciona os painéis na ordem visual (Esquerda para Direita)
        add(infoPanel1);   
        add(boardPanel1);  
        add(garbageBar1);  
        
        add(garbageBar2);  
        add(boardPanel2);  
        add(infoPanel2);   

        // Define a cor de fundo inicial
        updateTheme(Theme.AVAILABLE_THEMES[0]);

        // Esconde os componentes de 2P por padrão.
        // Isso é crucial para que o 'pack()' inicial no GameFrame
        // calcule o tamanho correto para o menu (visual de 1P).
        garbageBar1.setVisible(false);
        garbageBar2.setVisible(false);
        boardPanel2.setVisible(false);
        infoPanel2.setVisible(false);
    }
    
    /**
     * Alterna a visibilidade dos componentes do Jogador 2 com base no modo de jogo.
     * Isso aciona o 'revalidate', fazendo com que o painel recalcule seu tamanho preferido.
     * @param mode O modo de jogo (ONE_PLAYER ou TWO_PLAYER).
     */
    public void setMode(GameController.GameMode mode) {
        boolean isTwoPlayer = (mode == GameController.GameMode.TWO_PLAYER);
        
        // Mostra/Esconde o placar de "Vitórias" em ambos os painéis de info
        infoPanel1.setShowVictories(isTwoPlayer);
        infoPanel2.setShowVictories(isTwoPlayer);
        
        // A barra de lixo do P1 só é visível no modo 2P
        garbageBar1.setVisible(isTwoPlayer); 
        
        // Componentes P2
        garbageBar2.setVisible(isTwoPlayer);
        boardPanel2.setVisible(isTwoPlayer);
        infoPanel2.setVisible(isTwoPlayer);
        
        revalidate(); // Avisa o layout manager para recalcular o tamanho
    }

    /**
     * Propaga a mudança de tema para todos os componentes filhos.
     */
    public void updateTheme(Theme theme) {
        setBackground(theme.uiBackground());
        
        infoPanel1.updateTheme(theme);
        boardPanel1.updateTheme(theme);
        garbageBar1.updateTheme(theme); 
        
        infoPanel2.updateTheme(theme);
        boardPanel2.updateTheme(theme);
        garbageBar2.updateTheme(theme); 
    }


    // --- Getters para o Controller ---

    public BoardPanel getBoardPanel1() {
        return boardPanel1;
    }
    public InfoPanel getInfoPanel1() {
        return infoPanel1;
    }
    public GarbageBarPanel getGarbageBar1() { 
        return garbageBar1;
    }

    public BoardPanel getBoardPanel2() {
        return boardPanel2;
    }
    public InfoPanel getInfoPanel2() {
        return infoPanel2;
    }
    public GarbageBarPanel getGarbageBar2() { 
        return garbageBar2;
    }
}