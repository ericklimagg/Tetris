package com.tetris.view;

import com.tetris.controller.GameController; 
import com.tetris.model.Theme; 

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import java.awt.Dimension;
import java.awt.Color; 

/**
 * A janela principal do jogo (o JFrame).
 * Esta classe é o container de nível superior para todos os elementos visuais
 * e usa um JLayeredPane para sobrepor o OverlayPanel (menus) sobre o GamePanel (jogo).
 */
public class GameFrame extends JFrame {

    private GamePanel gamePanel;
    private OverlayPanel overlayPanel;
    private JLayeredPane layeredPane;

    public GameFrame() {
        initComponents();
    }

    /**
     * Inicializa e configura os componentes visuais da janela.
     */
    private void initComponents() {
        layeredPane = new JLayeredPane();
        
        gamePanel = new GamePanel();
        overlayPanel = new OverlayPanel();

        // --- Inicialização de Layout ---
        // Força o GamePanel a calcular seu tamanho MÁXIMO (modo 2P) primeiro.
        // Isso garante que a janela 'pack()' para o maior tamanho possível inicialmente,
        // evitando redimensionamentos inesperados ao alternar para o modo 2P.
        gamePanel.setMode(GameController.GameMode.TWO_PLAYER);
        Dimension size = gamePanel.getPreferredSize();
        
        // Retorna visualmente ao modo 1P para exibir o menu corretamente.
        gamePanel.setMode(GameController.GameMode.ONE_PLAYER); 
        
        layeredPane.setPreferredSize(size); // Define o tamanho do painel em camadas
        
        // Garante que o layeredPane tenha uma cor de fundo, evitando
        // "margens brancas" durante o redimensionamento.
        layeredPane.setOpaque(true);
        layeredPane.setBackground(Theme.AVAILABLE_THEMES[0].uiBackground()); 
        
        layeredPane.setLayout(null); // Usamos layout absoluto (bounds)
        
        // Define o tamanho exato dos painéis filhos
        gamePanel.setBounds(0, 0, size.width, size.height);
        overlayPanel.setBounds(0, 0, size.width, size.height);
        overlayPanel.setOpaque(false); // O painel de overlay deve ser transparente

        // Adiciona painéis ao JLayeredPane em suas respectivas camadas
        layeredPane.add(gamePanel, JLayeredPane.DEFAULT_LAYER); // Camada inferior (jogo)
        layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER); // Camada superior (menus)
        
        add(layeredPane); // Adiciona o painel principal ao JFrame

        // Configurações padrão do JFrame
        setTitle("Tetris");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        pack(); // Compacta a janela ao tamanho do layeredPane
        setLocationRelativeTo(null); // Centraliza na tela
    }
    
    /**
     * Redimensiona a janela (JFrame) para se ajustar ao conteúdo do GamePanel
     * (seja 1P ou 2P) e a re-centraliza na tela.
     * Chamado pelo GameController ao iniciar/mudar modos.
     */
    public void packAndCenter() {
        // Obtém o novo tamanho preferido do GamePanel (que pode ter mudado)
        Dimension size = gamePanel.getPreferredSize();
        layeredPane.setPreferredSize(size);
        
        // Atualiza os limites de ambos os painéis filhos para o novo tamanho
        gamePanel.setBounds(0, 0, size.width, size.height);
        overlayPanel.setBounds(0, 0, size.width, size.height);
        
        pack(); // Re-calcula o tamanho do JFrame
        setLocationRelativeTo(null); // Re-centraliza a janela
    }

    // --- Métodos de acesso para o Controller ---

    public GamePanel getGamePanel() {
        return gamePanel;
    }

    public OverlayPanel getOverlayPanel() {
        return overlayPanel;
    }
}