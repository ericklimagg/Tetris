package com.tetris;

import com.tetris.controller.GameController;
import com.tetris.model.Board;
import com.tetris.view.GameFrame;
import javax.swing.SwingUtilities;
import com.tetris.database.DatabaseManager;

/**
 * Ponto de entrada principal da aplicação.
 * Responsável por instanciar e conectar o Model, a View e o Controller.
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            
            // Inicializa a conexão com o banco de dados antes de criar a UI.
            // Isso garante que a aplicação falhe rapidamente se o banco
            // estiver inacessível, em vez de falhar após a janela abrir.
            System.out.println("Main: Inicializando DatabaseManager...");
            DatabaseManager.getInstance();
            System.out.println("Main: DatabaseManager inicializado.");

            // Cria os Models (um para cada jogador)
            Board board1 = new Board();
            Board board2 = new Board();

            // Cria a View principal
            GameFrame gameFrame = new GameFrame();

            // Cria o Controller e conecta os Models e a View
            GameController gameController = new GameController(gameFrame, board1, board2);

            // Inicia o jogo e exibe a janela
            gameController.start();
            gameFrame.setVisible(true);
        });
    }
}