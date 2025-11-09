package com.tetris.view;

import com.tetris.controller.GameController; 
import com.tetris.controller.GameController.GameScreen; 
import com.tetris.model.Board;
import com.tetris.model.Theme; 
import com.tetris.database.SoloScoreEntry;
import com.tetris.database.PlayerWinsEntry; 
import com.tetris.database.PlayerProfile;

import java.util.List; 
import java.text.SimpleDateFormat; 
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D; 
import java.awt.RenderingHints; 

/**
 * Painel transparente (JLayeredPane) que desenha todas as telas de "overlay"
 * (menus, pausa, game over, rankings) por cima do GamePanel.
 * Esta classe contém a maior parte da lógica de renderização da UI.
 */
public class OverlayPanel extends JPanel {

    // Referências de estado (Model e Controller)
    private Board board1;
    private Board board2;
    private GameController.GameMode currentGameMode;
    private Theme currentTheme; 
    
    // Estado da UI
    private GameScreen currentScreen;
    private int mainMenuSelection;
    private int modeSelectSelection;
    private int gameOverSelection; 
    private int pauseMenuSelection; 
    private int rankingModeSelection; 
    private int profileListSelection; 
    private String playerNameInput = "";
    private String profileErrorMessage = null; // Mensagem de erro para telas de perfil

    // Referências de dados (para rankings e perfis)
    private List<SoloScoreEntry> topSoloScores;
    private List<PlayerWinsEntry> top2PWins; 
    private List<PlayerProfile> allProfiles; 
    private PlayerProfile currentUser = null;  // P1
    private PlayerProfile currentUser2 = null; // P2
    
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");


    public OverlayPanel() {
        setOpaque(false); // Essencial para ser um overlay transparente
    }
    
    /**
     * Atualiza o tema visual a ser usado.
     */
    public void updateTheme(Theme theme) {
        this.currentTheme = theme;
    }

    /**
     * Método principal de "injeção de estado". O GameController chama este método
     * a cada tick para fornecer ao OverlayPanel todos os dados necessários
     * para desenhar a tela correta.
     */
    public void updateMenuState(Board board1, Board board2, GameController.GameMode mode, 
                                GameScreen screen, int mainSelection, int modeSelectSelection,
                                int gameOverSelection, int pauseSelection, int rankingModeSelection,
                                int profileListSelection, 
                                List<SoloScoreEntry> topSoloScores, List<PlayerWinsEntry> top2PWins,
                                List<PlayerProfile> allProfiles, 
                                String playerNameInput, String profileErrorMessage,
                                PlayerProfile currentUser, PlayerProfile currentUser2) { 
        this.board1 = board1;
        this.board2 = board2;
        this.currentGameMode = mode;
        this.currentScreen = screen;
        this.mainMenuSelection = mainSelection;
        this.modeSelectSelection = modeSelectSelection;
        this.gameOverSelection = gameOverSelection;
        this.pauseMenuSelection = pauseSelection; 
        this.rankingModeSelection = rankingModeSelection; 
        this.profileListSelection = profileListSelection; 
        this.topSoloScores = topSoloScores;     
        this.top2PWins = top2PWins;           
        this.allProfiles = allProfiles;           
        this.playerNameInput = playerNameInput; 
        this.profileErrorMessage = profileErrorMessage; 
        this.currentUser = currentUser;       
        this.currentUser2 = currentUser2;     
    }
    
    /**
     * (Método legado, pode ser obsoleto) Atualiza apenas os tabuleiros.
     * 'updateMenuState' é geralmente preferido.
     */
    public void updateBoards(Board board1, Board board2) {
         this.board1 = board1;
         this.board2 = board2;
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (board1 == null || board2 == null || currentTheme == null) {
            return; // Não desenha se o estado não foi inicializado
        }
        
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        boolean isGameActive = board1.isStarted() || board2.isStarted();
        boolean isGameOver = board1.isGameOver() || (currentGameMode == GameController.GameMode.TWO_PLAYER && board2.isGameOver());
        boolean isPaused = (currentScreen == GameScreen.PAUSED_MAIN ||
                            currentScreen == GameScreen.PAUSED_CONTROLS ||
                            currentScreen == GameScreen.PAUSED_RULES);

        // Roteador de renderização principal
        if (isGameActive) {
            // Jogo está rodando
            if (isGameOver) {
                // Desenha overlay de Game Over
                g.setColor(new Color(0, 0, 0, 200)); // Fundo escuro semi-transparente
                g.fillRect(0, 0, getWidth(), getHeight());
                drawGameOver(g2d); 
            
            } else if (isPaused) {
                // Desenha overlay de Pausa
                g.setColor(new Color(0, 0, 0, 180)); 
                g.fillRect(0, 0, getWidth(), getHeight());
                
                if (currentScreen == GameScreen.PAUSED_MAIN) {
                    drawPausedScreen(g2d);
                } else if (currentScreen == GameScreen.PAUSED_CONTROLS) {
                    drawPausedControlsScreen(g2d);
                } else if (currentScreen == GameScreen.PAUSED_RULES) {
                    drawPausedRulesScreen(g2d);
                }
            }
            // Se o jogo está ativo e não pausado/game over, não desenha nada (painel transparente).
            
        } else {
            // Jogo não está ativo (estamos nos menus principais)
            g.setColor(currentTheme.uiBackground()); 
            g.fillRect(0, 0, getWidth(), getHeight());
            
            g.setColor(new Color(0, 0, 0, 200)); // Overlay escuro para destacar o menu
            g.fillRect(0, 0, getWidth(), getHeight());

            if (currentScreen == null) return; 

            // Roteia para a tela de menu específica
            switch (currentScreen) {
                case MAIN_MENU:
                    drawStartScreen(g2d);
                    break;
                case MODE_SELECT:
                    drawModeSelectScreen(g2d);
                    break;
                case PROFILE_SELECTION: 
                    drawProfileSelectionScreen(g2d, 1); // P1
                    break;
                case PROFILE_SELECTION_P2: 
                    drawProfileSelectionScreen(g2d, 2); // P2
                    break;
                case PROFILE_CREATE: 
                    drawProfileCreateScreen(g2d);
                    break;
                case RANKING_MODE_SELECT: 
                    drawRankingModeSelectScreen(g2d);
                    break;
                case RANKING_SCREEN:
                    drawRankingScreen(g2d);
                    break;
                case RANKING_SCREEN_2P: 
                    drawRanking2PScreen(g2d);
                    break;
                case RULES_SCREEN:
                    drawRulesScreen(g2d);
                    break;
                case CONTROLS_SCREEN:
                    drawControlsScreen(g2d);
                    break;
                default:
                    drawStartScreen(g2d); // Tela padrão
                    break;
            }
        }
    }

    // --- ============ MÉTODOS DE DESENHO DE MENU ============ ---
    
    /**
     * Helper para desenhar o "card" de fundo dos menus.
     */
    private void drawMenuCard(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(new Color(20, 20, 30, 220)); // Fundo do card (escuro, semi-transparente)
        g.fillRoundRect(x, y, width, height, 25, 25);
        g.setColor(currentTheme.grid().brighter()); // Borda do card
        g.drawRoundRect(x, y, width, height, 25, 25);
    }
    
    /**
     * Helper para desenhar o título principal dos menus (ex: "TETRIS", "RANKING").
     */
    private void drawMenuTitle(Graphics2D g, String title) {
        g.setFont(new Font("Consolas", Font.BOLD, 72));
        g.setColor(Color.WHITE);
        drawCenteredString(g, title, getWidth() / 2, 120);
        
        g.setColor(Color.CYAN); // Linha sublinhada
        g.fillRect(getWidth() / 2 - 100, 140, 200, 4);
    }
    
    /**
     * Helper para desenhar o texto de dica no rodapé das telas de menu.
     */
    private void drawFooterHint(Graphics2D g, String text) {
        g.setFont(new Font("Consolas", Font.PLAIN, 16));
        g.setColor(Color.GRAY);
        drawCenteredString(g, text, getWidth() / 2, getHeight() - 60);
    }
    
    /**
     * Helper para desenhar a mensagem de erro (se existir) na base de um card de menu.
     */
    private void drawErrorMessage(Graphics2D g, int card_y, int card_height) {
        if (profileErrorMessage != null && !profileErrorMessage.isEmpty()) {
            g.setColor(Color.RED);
            g.setFont(new Font("Consolas", Font.BOLD, 18));
            // Posiciona a mensagem perto da base do card
            drawCenteredString(g, profileErrorMessage, getWidth() / 2, card_y + card_height - 35);
        }
    }


    /**
     * Desenha a tela do Menu Principal (Jogar, Ranking, etc.).
     */
    private void drawStartScreen(Graphics2D g) {
        drawMenuTitle(g, "T E T R I S");
        
        int cardWidth = 350;
        int cardHeight = 280;
        int x = getWidth() / 2 - cardWidth / 2;
        int y = getHeight() / 2 - cardHeight / 2;
        
        drawMenuCard(g, x, y, cardWidth, cardHeight);
        
        g.setFont(new Font("Consolas", Font.PLAIN, 28));
        int y_menu = y + 60;
        
        String[] options = {"Jogar", "Ranking", "Regras", "Controles", "Sair"}; 
        boolean cursorVisible = (System.currentTimeMillis() / 400) % 2 == 0;
        String selector = cursorVisible ? ">" : " ";
        
        // Desenha as opções do menu, destacando a selecionada
        for (int i = 0; i < options.length; i++) {
            if (i == mainMenuSelection) {
                g.setColor(Color.YELLOW);
                drawCenteredString(g, selector + " " + options[i], getWidth() / 2, y_menu);
            } else {
                g.setColor(Color.WHITE);
                drawCenteredString(g, options[i], getWidth() / 2, y_menu);
            }
            y_menu += 45;
        }
        
        drawFooterHint(g, "(Use ↑↓ para selecionar, ENTER para confirmar)");
    }
    
    /**
     * Desenha a tela de seleção de perfil (para P1 ou P2).
     */
    private void drawProfileSelectionScreen(Graphics2D g, int playerNum) {
        int cardWidth = 500; 
        int cardHeight = 450; 
        int x = getWidth() / 2 - cardWidth / 2;
        int y = getHeight() / 2 - cardHeight / 2;
        
        drawMenuCard(g, x, y, cardWidth, cardHeight);
        
        g.setColor(Color.CYAN);
        g.setFont(new Font("Consolas", Font.BOLD, 36));
        
        String title = (playerNum == 1) ? "JOGADOR 1" : "JOGADOR 2";
        drawCenteredString(g, title, getWidth() / 2, y + 70); 

        // Se for P2, mostra quem está logado como P1
        if (playerNum == 2 && currentUser != null) {
            g.setFont(new Font("Consolas", Font.PLAIN, 14));
            g.setColor(Color.LIGHT_GRAY);
            drawCenteredString(g, "P1: " + currentUser.getUsername(), getWidth() / 2, y + 95);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 18));
        drawCenteredString(g, "Selecione um perfil ou crie um novo:", getWidth() / 2, y + 140); 
        
        g.setFont(new Font("Consolas", Font.PLAIN, 24));
        int y_list = y + 190;
        int list_x = getWidth() / 2;
        
        boolean cursorVisible = (System.currentTimeMillis() / 400) % 2 == 0;
        String selector = cursorVisible ? ">" : " ";

        // Desenha a lista de perfis carregados
        if (allProfiles != null) {
            for (int i = 0; i < allProfiles.size(); i++) {
                PlayerProfile profile = allProfiles.get(i);
                String name = profile.getUsername();
                
                // Se for P2, "desabilita" (em cinza) o perfil já selecionado pelo P1
                if (playerNum == 2 && currentUser != null && currentUser.getUserID() == profile.getUserID()) {
                    g.setColor(Color.GRAY); 
                    drawCenteredString(g, name, list_x, y_list);
                } else {
                    // Destaca o perfil selecionado
                    if (i == profileListSelection) {
                        g.setColor(Color.YELLOW);
                        drawCenteredString(g, selector + " " + name, list_x, y_list);
                    } else {
                        g.setColor(Color.WHITE);
                        drawCenteredString(g, name, list_x, y_list);
                    }
                }
                y_list += 35;
            }
        }

        // Desenha a opção [CRIAR NOVO USUÁRIO]
        int createOptionIndex = (allProfiles != null) ? allProfiles.size() : 0;
        if (profileListSelection == createOptionIndex) {
            g.setColor(Color.GREEN); 
            drawCenteredString(g, selector + " [CRIAR NOVO USUÁRIO]", list_x, y_list);
        } else {
            g.setColor(Color.WHITE);
            drawCenteredString(g, "[CRIAR NOVO USUÁRIO]", list_x, y_list);
        }
        
        drawErrorMessage(g, y, cardHeight);
        drawFooterHint(g, "(Use ↑↓ para selecionar, ENTER para confirmar, ESC para voltar)");
    }

    /**
     * Desenha a tela de criação de novo perfil (entrada de texto).
     */
    private void drawProfileCreateScreen(Graphics2D g) {
        int cardWidth = 500; 
        int cardHeight = 300;
        int x = getWidth() / 2 - cardWidth / 2;
        int y = getHeight() / 2 - cardHeight / 2;
        
        drawMenuCard(g, x, y, cardWidth, cardHeight);
        
        g.setColor(Color.CYAN);
        g.setFont(new Font("Consolas", Font.BOLD, 36));
        
        String title = (currentUser == null) ? "CRIAR PERFIL (P1)" : "CRIAR PERFIL (P2)";
        drawCenteredString(g, title, getWidth() / 2, y + 70); 

        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 18));
        drawCenteredString(g, "Digite um novo nome de usuário:", getWidth() / 2, y + 130); 
        
        g.setFont(new Font("Consolas", Font.BOLD, 28));
        g.setColor(Color.WHITE);
        
        // Simula um cursor piscando
        boolean cursorVisible = (System.currentTimeMillis() / 400) % 2 == 0;
        String nameStr = playerNameInput + (cursorVisible ? "_" : "");
        
        drawCenteredString(g, nameStr, getWidth() / 2, y + 210);
        
        drawErrorMessage(g, y, cardHeight);
        drawFooterHint(g, "(A-Z, 0-9) | ENTER para Confirmar | ESC para Voltar à Lista");
    }


    /**
     * Desenha a tela de seleção de modo (1P ou 2P).
     */
    private void drawModeSelectScreen(Graphics2D g) {
        drawMenuTitle(g, "T E T R I S");

        int cardWidth = 350;
        int cardHeight = 150;
        int x = getWidth() / 2 - cardWidth / 2;
        int y = getHeight() / 2 - cardHeight / 2;
        
        drawMenuCard(g, x, y, cardWidth, cardHeight);
        
        g.setFont(new Font("Consolas", Font.PLAIN, 28));
        int y_menu = y + 50;

        String[] options = {"1 Jogador", "2 Jogadores"};
        boolean cursorVisible = (System.currentTimeMillis() / 400) % 2 == 0;
        String selector = cursorVisible ? ">" : " ";
        
        for (int i = 0; i < options.length; i++) {
            if (i == modeSelectSelection) {
                g.setColor(Color.YELLOW);
                drawCenteredString(g, selector + " " + options[i], getWidth() / 2, y_menu);
            } else {
                g.setColor(Color.WHITE);
                drawCenteredString(g, options[i], getWidth() / 2, y_menu);
            }
            y_menu += 45;
        }
        
        drawFooterHint(g, "(Pressione ESC para Voltar ao Menu)");
    }

    /**
     * Desenha a tela de seleção de modo de ranking (1P ou 2P).
     */
    private void drawRankingModeSelectScreen(Graphics2D g) {
        drawMenuTitle(g, "RANKING");

        int cardWidth = 350;
        int cardHeight = 150;
        int x = getWidth() / 2 - cardWidth / 2;
        int y = getHeight() / 2 - cardHeight / 2;
        
        drawMenuCard(g, x, y, cardWidth, cardHeight);
        
        g.setFont(new Font("Consolas", Font.PLAIN, 28));
        int y_menu = y + 50;

        String[] options = {"Ranking 1P (Pontuação)", "Ranking 2P (Vitórias)"};
        boolean cursorVisible = (System.currentTimeMillis() / 400) % 2 == 0;
        String selector = cursorVisible ? ">" : " ";
        
        for (int i = 0; i < options.length; i++) {
            if (i == rankingModeSelection) {
                g.setColor(Color.YELLOW);
                drawCenteredString(g, selector + " " + options[i], getWidth() / 2, y_menu);
            } else {
                g.setColor(Color.WHITE);
                drawCenteredString(g, options[i], getWidth() / 2, y_menu);
            }
            y_menu += 45;
        }
        
        drawFooterHint(g, "(Pressione ESC para Voltar ao Menu)");
    }


    /**
     * Desenha a tela de Ranking 1P (High Scores).
     */
    private void drawRankingScreen(Graphics2D g) {
        int cardWidth = 650; 
        int cardHeight = 550; 
        int x = getWidth() / 2 - cardWidth / 2;
        int y = 100;
        
        drawMenuCard(g, x, y, cardWidth, cardHeight);
        
        g.setColor(Color.CYAN);
        g.setFont(new Font("Consolas", Font.BOLD, 32));
        drawCenteredString(g, "RANKING 1P (MELHOR SCORE)", getWidth() / 2, y + 50);

        // Cabeçalho da tabela
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Consolas", Font.BOLD, 16));
        int y_list = y + 100;
        int x_padding = 40;
        
        g.drawString("POS", x + x_padding, y_list);
        g.drawString("NOME", x + x_padding + 50, y_list);
        g.drawString("PONTUAÇÃO", x + x_padding + 210, y_list);
        g.drawString("NÍVEL", x + x_padding + 330, y_list); 
        g.drawString("LINHAS", x + x_padding + 400, y_list); 
        g.drawString("DATA", x + x_padding + 510, y_list); 
        
        g.setColor(Color.GRAY);
        g.drawLine(x + 20, y_list + 10, x + cardWidth - 20, y_list + 10);
        
        y_list += 35; 
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 16));

        // Desenha as entradas do ranking
        if (topSoloScores == null || topSoloScores.isEmpty()) {
            drawCenteredString(g, "Nenhuma pontuação registrada.", getWidth() / 2, y + 250);
        
        } else {
            int pos = 1;
            for (SoloScoreEntry entry : topSoloScores) {
                String posStr = String.format("%2d.", pos);
                String scoreStr = String.format("%,d", entry.score()); 
                String levelStr = String.format("%02d", entry.level());     
                String linesStr = String.format("%03d", entry.linesCleared()); 
                String dateStr = dateFormat.format(entry.date());

                // Colore as 3 primeiras posições
                if (pos == 1) g.setColor(Color.YELLOW);
                else if (pos == 2) g.setColor(Color.LIGHT_GRAY);
                else if (pos == 3) g.setColor(new Color(205, 127, 50)); // Bronze
                else g.setColor(Color.WHITE);
                
                g.drawString(posStr, x + x_padding, y_list);
                g.drawString(entry.username(), x + x_padding + 50, y_list);
                g.drawString(scoreStr, x + x_padding + 210, y_list);
                g.drawString(levelStr, x + x_padding + 330, y_list); 
                g.drawString(linesStr, x + x_padding + 400, y_list); 
                g.drawString(dateStr, x + x_padding + 510, y_list); 
                
                y_list += 28; 
                pos++;
            }
        }

        drawFooterHint(g, "(Pressione ENTER ou ESC para Voltar)");
    }
    
    /**
     * Desenha a tela de Ranking 2P (Vitórias).
     */
    private void drawRanking2PScreen(Graphics2D g) {
        int cardWidth = 450; 
        int cardHeight = 550; 
        int x = getWidth() / 2 - cardWidth / 2;
        int y = 100;
        
        drawMenuCard(g, x, y, cardWidth, cardHeight);
        
        g.setColor(Color.CYAN);
        g.setFont(new Font("Consolas", Font.BOLD, 32));
        drawCenteredString(g, "RANKING 2P (VITÓRIAS)", getWidth() / 2, y + 50);

        // Cabeçalho da tabela
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Consolas", Font.BOLD, 16));
        int y_list = y + 100;
        int x_padding = 40;
        g.drawString("POS", x + x_padding, y_list);
        g.drawString("NOME", x + x_padding + 70, y_list);
        g.drawString("VITÓRIAS", x + x_padding + 280, y_list);
        
        g.setColor(Color.GRAY);
        g.drawLine(x + 20, y_list + 10, x + cardWidth - 20, y_list + 10);
        
        y_list += 35; 
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 16));

        // Desenha as entradas do ranking
        if (top2PWins == null || top2PWins.isEmpty()) {
            drawCenteredString(g, "Nenhuma vitória registrada.", getWidth() / 2, y + 250);
        
        } else {
            int pos = 1;
            for (PlayerWinsEntry entry : top2PWins) {
                String posStr = String.format("%2d.", pos);
                String winsStr = String.format("%,d", entry.wins()); 

                if (pos == 1) g.setColor(Color.YELLOW);
                else if (pos == 2) g.setColor(Color.LIGHT_GRAY);
                else if (pos == 3) g.setColor(new Color(205, 127, 50)); // Bronze
                else g.setColor(Color.WHITE);
                
                g.drawString(posStr, x + x_padding, y_list);
                g.drawString(entry.username(), x + x_padding + 70, y_list);
                g.drawString(winsStr, x + x_padding + 280, y_list);
                
                y_list += 28; 
                pos++;
            }
        }

        drawFooterHint(g, "(Pressione ENTER ou ESC para Voltar)");
    }
    
    /**
     * Desenha a tela de Regras (visível no menu principal ou pausa).
     */
    private void drawRulesScreen(Graphics2D g) {
        int cardWidth = 700; 
        int cardHeight = 500; 
        int x = getWidth() / 2 - cardWidth / 2;
        int y = 100;
        
        drawMenuCard(g, x, y, cardWidth, cardHeight);
        
        g.setColor(Color.CYAN);
        g.setFont(new Font("Consolas", Font.BOLD, 32));
        drawCenteredString(g, "REGRAS E PONTUAÇÃO", getWidth() / 2, y + 50);

        int y_col = y + 120;
        int x_col1 = x + 50;
        int x_col2 = x + 370;

        // Coluna 1: Pontuação e Nível
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Consolas", Font.BOLD, 22));
        g.drawString("PONTUAÇÃO", x_col1, y_col);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 18));
        y_col += 40; 
        g.drawString("Pontos = Valor Base * Nível", x_col1, y_col);
        g.setColor(Color.CYAN);
        y_col += 40; g.drawString("1 Linha   :  40 pts", x_col1, y_col);
        y_col += 30; g.drawString("2 Linhas  : 100 pts", x_col1, y_col);
        y_col += 30; g.drawString("3 Linhas  : 300 pts", x_col1, y_col);
        g.setColor(Color.ORANGE);
        y_col += 30; g.drawString("TETRIS (4): 1200 pts", x_col1, y_col);
        y_col += 50; 
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Consolas", Font.BOLD, 22));
        g.drawString("NÍVEL", x_col1, y_col);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 16));
        y_col += 30;
        String textNivel = "Você avança de nível a cada\n" +
                           "10 linhas limpas. Um nível\n" +
                           "mais alto aumenta a velocidade\n" +
                           "do jogo e seus pontos.";
        y_col = drawMultiLineString(g, textNivel, x_col1, y_col);

        // Coluna 2: Lixo (Modo 2P)
        y_col = y + 120; 
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Consolas", Font.BOLD, 22));
        g.drawString("LIXO (Modo 2P)", x_col2, y_col);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 16));
        y_col += 40;
        String textLixo = "No modo 2P, limpar linhas envia\n" +
                          "'Lixo' (linhas cinzas) para o \n" +
                          "oponente. O lixo aparece na \n" +
                          "base do tabuleiro, empurrando \n" +
                          "as peças dele para cima.";
        y_col = drawMultiLineString(g, textLixo, x_col2, y_col);
        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font("Consolas", Font.PLAIN, 18));
        y_col += 30; g.drawString("2 Linhas  -> Envia 1 Linha", x_col2, y_col);
        y_col += 30; g.drawString("3 Linhas  -> Envia 2 Linhas", x_col2, y_col);
        y_col += 30; g.drawString("TETRIS    -> Envia 4 Linhas", x_col2, y_col);

        drawFooterHint(g, "(Pressione ENTER ou ESC para Voltar)");
    }

    /**
     * Desenha a tela de Controles (visível no menu principal).
     */
    private void drawControlsScreen(Graphics2D g) {
        int cardWidth = 700;
        int cardHeight = 450;
        int x = getWidth() / 2 - cardWidth / 2;
        int y = 100;
        
        drawMenuCard(g, x, y, cardWidth, cardHeight);
        
        g.setColor(Color.CYAN);
        g.setFont(new Font("Consolas", Font.BOLD, 32));
        drawCenteredString(g, "CONTROLES", getWidth() / 2, y + 50);

        int y_start = y + 120;
        int x_p1 = x + 50;
        int x_p2 = x + 370;
        int y_col;

        // Coluna 1: Controles 1P
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Consolas", Font.BOLD, 18));
        y_col = y_start;
        g.drawString("MODO 1 JOGADOR", x_p1, y_col);
        drawControls1P(g, x_p1, y_col + 30);
        
        // Coluna 2: Controles 2P
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Consolas", Font.BOLD, 18));
        y_col = y_start;
        g.drawString("MODO 2 JOGADORES", x_p2, y_col);
        drawControls2P(g, x_p2, y_col + 30);
        
        // Controles Globais
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Consolas", Font.BOLD, 18));
        y_col = y_start + 230; 
        g.drawString("CONTROLES GLOBAIS", x_p1, y_col);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 14));
        y_col += 30; g.drawString("P   Pausar Jogo", x_p1, y_col);
        y_col += 20; g.drawString("T   Mudar Tema Visual", x_p1, y_col); 
        y_col += 20; g.drawString("G   Ativar/Desativar Prévia", x_p1, y_col);
        
        drawFooterHint(g, "(Pressione ENTER ou ESC para Voltar)");
    }

    /**
     * Desenha a tela de Fim de Jogo.
     */
    private void drawGameOver(Graphics2D g) {
        int cardWidth = 450;
        int cardHeight = 300;
        int x = getWidth() / 2 - cardWidth / 2;
        int y = getHeight() / 2 - cardHeight / 2;
        
        drawMenuCard(g, x, y, cardWidth, cardHeight);
        
        int y_center = y + 70;
        int p1_x_center = getWidth() / 2 - 110;
        int p2_x_center = getWidth() / 2 + 110;
        
        if (currentGameMode == GameController.GameMode.ONE_PLAYER) {
            g.setFont(new Font("Consolas", Font.BOLD, 36));
            g.setColor(Color.RED);
            drawCenteredString(g, "GAME OVER", getWidth() / 2, y_center); 
        } else {
            // Modo 2P: Determina Vencedor/Perdedor
            String p1_name = (currentUser != null) ? currentUser.getUsername() : "P1";
            String p2_name = (currentUser2 != null) ? currentUser2.getUsername() : "P2";

            if (board1.isGameOver() && board2.isGameOver()) {
                g.setFont(new Font("Consolas", Font.BOLD, 36));
                g.setColor(Color.WHITE);
                drawCenteredString(g, "EMPATE", getWidth() / 2, y_center);
            } else if (board1.isGameOver()) {
                g.setFont(new Font("Consolas", Font.BOLD, 28));
                g.setColor(Color.RED);
                drawCenteredString(g, p1_name + " PERDEU", p1_x_center, y_center);
                g.setFont(new Font("Consolas", Font.BOLD, 28));
                g.setColor(Color.GREEN);
                drawCenteredString(g, p2_name + " VENCEU!", p2_x_center, y_center);
            } else if (board2.isGameOver()) {
                g.setFont(new Font("Consolas", Font.BOLD, 28));
                g.setColor(Color.GREEN);
                drawCenteredString(g, p1_name + " VENCEU!", p1_x_center, y_center);
                g.setFont(new Font("Consolas", Font.BOLD, 28));
                g.setColor(Color.RED);
                drawCenteredString(g, p2_name + " PERDEU", p2_x_center, y_center);
            }
        }
        
        // Opções (Reiniciar / Menu)
        g.setFont(new Font("Consolas", Font.PLAIN, 24));
        int y_menu = y_center + 80;
        
        String[] options = {"Reiniciar", "Voltar ao Menu"};
        
        for (int i = 0; i < options.length; i++) {
            if (i == gameOverSelection) {
                g.setColor(Color.YELLOW);
                drawCenteredString(g, "> " + options[i], getWidth() / 2, y_menu);
            } else {
                g.setColor(Color.WHITE);
                drawCenteredString(g, options[i], getWidth() / 2, y_menu);
            }
            y_menu += 40;
        }
    }
    
    /**
     * Desenha o menu de Pausa principal.
     */
    private void drawPausedScreen(Graphics2D g) {
        int cardWidth = 450;
        int cardHeight = 280;
        int x = getWidth() / 2 - cardWidth / 2;
        int y = getHeight() / 2 - cardHeight / 2;
        
        drawMenuCard(g, x, y, cardWidth, cardHeight);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.BOLD, 36));
        drawCenteredString(g, "PAUSADO", getWidth() / 2, y + 60);

        g.setFont(new Font("Consolas", Font.PLAIN, 24));
        int y_menu = y + 110;
        
        String[] options = {"Voltar ao Jogo", "Controles", "Regras", "Sair para o Menu"};
        
        for (int i = 0; i < options.length; i++) {
            if (i == pauseMenuSelection) {
                g.setColor(Color.YELLOW);
                drawCenteredString(g, "> " + options[i], getWidth() / 2, y_menu);
            } else {
                g.setColor(Color.WHITE);
                drawCenteredString(g, options[i], getWidth() / 2, y_menu);
            }
            y_menu += 40;
        }
    }
    
    /**
     * Desenha a tela de Regras (versão da Pausa, pode ser diferente da do menu principal).
     */
    private void drawPausedRulesScreen(Graphics2D g) {
        int cardWidth = 400; 
        int cardHeight = 500; 
        int x = getWidth() / 2 - cardWidth / 2;
        int y = getHeight() / 2 - cardHeight / 2;
        
        drawMenuCard(g, x, y, cardWidth, cardHeight);
        
        g.setColor(Color.CYAN);
        g.setFont(new Font("Consolas", Font.BOLD, 32));
        drawCenteredString(g, "REGRAS", getWidth() / 2, y + 50);

        int y_col = y + 100;
        int x_col = x + 50;

        // Pontuação
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Consolas", Font.BOLD, 22));
        g.drawString("PONTUAÇÃO", x_col, y_col);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 18));
        y_col += 40; 
        g.drawString("Pontos = Valor Base * Nível", x_col, y_col);
        g.setColor(Color.CYAN);
        y_col += 40; g.drawString("1 Linha   :  40 pts", x_col, y_col);
        y_col += 30; g.drawString("2 Linhas  : 100 pts", x_col, y_col);
        y_col += 30; g.drawString("3 Linhas  : 300 pts", x_col, y_col);
        g.setColor(Color.ORANGE);
        y_col += 30; g.drawString("TETRIS (4): 1200 pts", x_col, y_col);
        
        // Nível
        y_col += 40; 
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Consolas", Font.BOLD, 22));
        g.drawString("NÍVEL", x_col, y_col);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 16));
        y_col += 30;
        String textNivel = "A cada 10 linhas limpas,\n" +
                           "você avança de nível.";
        y_col = drawMultiLineString(g, textNivel, x_col, y_col);

        // Lixo (só mostra se estiver em modo 2P)
        if (currentGameMode == GameController.GameMode.TWO_PLAYER) {
            y_col += 40;
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Consolas", Font.BOLD, 22));
            g.drawString("LIXO (Modo 2P)", x_col, y_col);
            g.setColor(Color.LIGHT_GRAY);
            g.setFont(new Font("Consolas", Font.PLAIN, 18));
            y_col += 30; g.drawString("2 Linhas  -> Envia 1 Linha", x_col, y_col);
            y_col += 30; g.drawString("3 Linhas  -> Envia 2 Linhas", x_col, y_col);
            y_col += 30; g.drawString("TETRIS    -> Envia 4 Linhas", x_col, y_col);
        }

        drawFooterHint(g, "(Pressione ENTER ou ESC para Voltar)");
    }
    
    /**
     * Desenha a tela de Controles (versão da Pausa, específica para o modo atual).
     */
    private void drawPausedControlsScreen(Graphics2D g) {
        int cardWidth = 400; 
        int cardHeight = 350;
        int x = getWidth() / 2 - cardWidth / 2;
        int y = getHeight() / 2 - cardHeight / 2;
        
        drawMenuCard(g, x, y, cardWidth, cardHeight);
        
        g.setColor(Color.CYAN);
        g.setFont(new Font("Consolas", Font.BOLD, 32));
        drawCenteredString(g, "CONTROLES", getWidth() / 2, y + 50);

        int y_start = y + 100;
        int x_col = getWidth() / 2 - 150; 

        // Mostra apenas os controles do modo de jogo atual
        if (currentGameMode == GameController.GameMode.ONE_PLAYER) {
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Consolas", Font.BOLD, 18));
            g.drawString("MODO 1 JOGADOR", x_col, y_start);
            drawControls1P(g, x_col, y_start + 30);
        } else {
            x_col = getWidth() / 2 - 160;
            drawControls2P(g, x_col, y_start);
        }
        
        drawFooterHint(g, "(Pressione ENTER ou ESC para Voltar)");
    }
    
    // --- ============ Helpers de Desenho de Texto e Controles ============ ---
    
    /**
     * Helper para desenhar a lista de controles do 1P.
     */
    private int drawControls1P(Graphics g, int x, int y) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 14));
        g.drawString("←   Mover Esquerda", x, y); y += 20;
        g.drawString("→   Mover Direita", x, y); y += 20;
        g.drawString("↑   Girar (Horário)", x, y); y += 20;
        g.drawString("Z   Girar (Anti-horário)", x, y); y += 20;
        g.drawString("↓   Acelerar Queda", x, y); y += 20;
        g.drawString("Espaço   Cair Imediatamente", x, y);
        return y;
    }
    
    /**
     * Helper para desenhar a lista de controles do 2P.
     */
    private int drawControls2P(Graphics g, int x, int y) {
        g.setColor(Color.CYAN);
        g.setFont(new Font("Consolas", Font.BOLD, 16));
        g.drawString("JOGADOR 1 (Esquerda)", x, y);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 14));
        y += 25; g.drawString("A / D   Esquerda / Direita", x, y);
        y += 20; g.drawString("W / Q   Girar Hor / Anti-hor", x, y);
        y += 20; g.drawString("S       Acelerar Queda", x, y);
        y += 20; g.drawString("Espaço  Cair Imediatamente", x, y);

        y += 35;
        g.setColor(Color.CYAN);
        g.setFont(new Font("Consolas", Font.BOLD, 16));
        g.drawString("JOGADOR 2 (Direita)", x, y);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Consolas", Font.PLAIN, 14));
        y += 25; g.drawString("← / →   Esquerda / Direita", x, y);
        y += 20; g.drawString("↑ / M   Girar Hor / Anti-hor", x, y);
        y += 20; g.drawString("↓       Acelerar Queda", x, y);
        y += 20; g.drawString("N       Cair Imediatamente", x, y);
        return y;
    }
    
    /**
     * Helper para desenhar texto centralizado horizontalmente.
     */
    private void drawCenteredString(Graphics g, String text, int x_center, int y) {
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        int x = x_center - (metrics.stringWidth(text) / 2);
        g.drawString(text, x, y);
    }
    
    /**
     * Helper para desenhar blocos de texto com quebra de linha (definida por '\n').
     * @return A próxima posição Y (abaixo do texto).
     */
    private int drawMultiLineString(Graphics g, String text, int x, int y) {
        FontMetrics metrics = g.getFontMetrics(g.getFont());
        int lineHeight = metrics.getHeight();
        for (String line : text.split("\n")) {
            g.drawString(line, x, y);
            y += lineHeight; // Move para a próxima linha
        }
        return y;
    }
}
