package com.tetris.controller;

import com.tetris.model.Board;
import com.tetris.model.Theme;
import com.tetris.view.GameFrame;
import com.tetris.audio.AudioManager;
import com.tetris.database.PlayerProfileDAO;
import com.tetris.database.SoloScoreDAO;
import com.tetris.database.PlayerProfile;
import com.tetris.database.SoloScoreEntry;
import com.tetris.database.PlayerWinsEntry;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List; 

/**
 * O Controller (no padrão MVC) principal do jogo.
 * Gerencia o loop de jogo (Timer), a entrada do usuário (KeyAdapter)
 * e coordena a lógica de negócio (Model) com a exibição (View).
 */
public class GameController extends KeyAdapter implements ActionListener {

    // Define os modos de jogo disponíveis
    public enum GameMode {
        ONE_PLAYER,
        TWO_PLAYER
    }
    
    // Define todas as telas possíveis que o OverlayPanel pode renderizar
    public enum GameScreen {
        MAIN_MENU,
        MODE_SELECT,
        RANKING_MODE_SELECT, 
        RANKING_SCREEN, 
        RANKING_SCREEN_2P,
        RULES_SCREEN,
        CONTROLS_SCREEN,
        PAUSED_MAIN,
        PAUSED_CONTROLS,
        PAUSED_RULES,
        PROFILE_SELECTION,    
        PROFILE_SELECTION_P2, 
        PROFILE_CREATE        
    }

    // Constantes do loop de jogo
    private static final int INITIAL_DELAY = 400; // Delay inicial de queda (Nível 1)
    private static final int GAME_LOOP_DELAY = 33; // ~30 FPS para animações e lógica

    // Referências MVC
    private final GameFrame gameFrame;
    private final Board board1; 
    private final Board board2; 
    
    // Componentes de Jogo
    private final Timer timer;
    private final AudioManager backgroundMusic;

    // Acesso ao Banco de Dados (DAO)
    private final PlayerProfileDAO profileDAO;
    private final SoloScoreDAO soloScoreDAO;
    
    // Estado de Sessão e Perfis
    private PlayerProfile currentUser = null;   // Perfil do Jogador 1
    private PlayerProfile currentUser2 = null;  // Perfil do Jogador 2
    private List<SoloScoreEntry> topSoloScores; 
    private List<PlayerWinsEntry> top2PWins;    
    private List<PlayerProfile> allProfiles; 
    private String playerNameInput = ""; 
    private String profileErrorMessage = null; // Feedback para falhas de login/criação
    
    // Estado de UI
    private int currentThemeIndex = 0;
    private GameMode currentGameMode = GameMode.ONE_PLAYER;
    private GameScreen currentScreen = GameScreen.MAIN_MENU;
    
    // Seleções de Menu
    private int mainMenuSelection = 0;
    private final int MAIN_MENU_OPTIONS = 5; 
    private int modeSelectSelection = 0;
    private final int MODE_SELECT_OPTIONS = 2; 
    private int rankingModeSelection = 0; 
    private final int RANKING_MODE_OPTIONS = 2; 
    private int profileListSelection = 0; 
    private int gameOverSelection = 0; 
    private final int GAMEOVER_MENU_OPTIONS = 2;
    private int pauseMenuSelection = 0; 
    private final int PAUSE_MENU_OPTIONS = 4;
    
    // Timers de lógica de queda
    private long lastPieceMoveTime1;
    private long lastPieceMoveTime2;


    public GameController(GameFrame gameFrame, Board board1, Board board2) {
        this.gameFrame = gameFrame;
        this.board1 = board1;
        this.board2 = board2;
        
        this.timer = new Timer(GAME_LOOP_DELAY, this);
        
        // Adiciona o listener de teclas ao painel do jogo e requisita foco
        this.gameFrame.getGamePanel().addKeyListener(this);
        this.gameFrame.getGamePanel().setFocusable(true);

        System.out.println("GameController: Tentando inicializar o AudioManager...");
        this.backgroundMusic = new AudioManager("/com/tetris/audio/background-music.wav");
        
        // Instancia os DAOs para interação com o banco
        this.profileDAO = new PlayerProfileDAO();
        this.soloScoreDAO = new SoloScoreDAO();
    }

    /**
     * Inicia o timer principal do jogo e atualiza a view pela primeira vez.
     */
    public void start() {
        long startTime = System.currentTimeMillis();
        lastPieceMoveTime1 = startTime; 
        lastPieceMoveTime2 = startTime; 
        timer.start();
        updateView(); 
    }

    /**
     * O loop principal do jogo, chamado pelo Timer a cada GAME_LOOP_DELAY ms.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        
        // Verifica se o jogo está ativamente em andamento
        boolean isGameRunning = board1.isStarted() &&
                                currentScreen != GameScreen.PAUSED_MAIN &&
                                currentScreen != GameScreen.PAUSED_CONTROLS &&
                                currentScreen != GameScreen.PAUSED_RULES &&
                                currentScreen != GameScreen.PROFILE_SELECTION &&
                                currentScreen != GameScreen.PROFILE_SELECTION_P2 &&
                                currentScreen != GameScreen.PROFILE_CREATE; 
                                
        if (isGameRunning) {
            
            // --- Lógica de Jogo Principal ---
            long currentTime = System.currentTimeMillis();
            
            // Lógica P1
            long delay1 = getDelayForLevel(board1);
            handlePlayerLogic(board1, currentTime, lastPieceMoveTime1, delay1);
            if (currentTime - lastPieceMoveTime1 > delay1) {
                lastPieceMoveTime1 = currentTime;
            }
            
            // Lógica P2 (se aplicável)
            if (currentGameMode == GameMode.TWO_PLAYER) {
                long delay2 = getDelayForLevel(board2);
                handlePlayerLogic(board2, currentTime, lastPieceMoveTime2, delay2);
                if (currentTime - lastPieceMoveTime2 > delay2) {
                    lastPieceMoveTime2 = currentTime;
                }
            }
            
            // Lógica de "Garbage" (Lixo) para 2P
            if (currentGameMode == GameMode.TWO_PLAYER) {
                int p1_garbage = board1.getOutgoingGarbage();
                int p2_garbage = board2.getOutgoingGarbage();
                if (p1_garbage > 0 || p2_garbage > 0) {
                    // Cancela lixo (ataques se anulam)
                    if (p1_garbage > p2_garbage) {
                        board2.addIncomingGarbage(p1_garbage - p2_garbage);
                    } else if (p2_garbage > p1_garbage) { 
                        board1.addIncomingGarbage(p2_garbage - p1_garbage);
                    }
                    board1.clearOutgoingGarbage();
                    board2.clearOutgoingGarbage();
                }
            }
            
            // --- Detecção de Fim de Jogo ---
            boolean p1_over = board1.isGameOver();
            boolean p2_over = (currentGameMode == GameMode.TWO_PLAYER) && board2.isGameOver();
            boolean round_is_finished = (currentGameMode == GameMode.ONE_PLAYER) ? p1_over : (p1_over || p2_over);

            // Processa o fim de jogo (salva scores, etc.)
            if (round_is_finished) {
                
                if (timer.isRunning()) { 
                    timer.stop();
                    
                    if (backgroundMusic != null) {
                        backgroundMusic.stopMusic();
                    }
                    
                    // Salva estatísticas 1P
                    if (currentGameMode == GameMode.ONE_PLAYER && p1_over) {
                        if (currentUser != null && board1.getScore() > 0) {
                            // Salva o score individual da partida
                            soloScoreDAO.addScore(
                                currentUser.getUserID(), 
                                board1.getScore(),
                                board1.getLevel(),
                                board1.getLinesCleared(),
                                board1.getTetrisCount()
                            );
                            // Atualiza o perfil (jogos jogados, high score)
                            profileDAO.updateStats1P(currentUser.getUserID(), board1.getScore());

                            // Se um novo high score foi salvo, atualiza o objeto 'currentUser'
                            if (board1.getScore() > currentUser.getHighScore1P()) {
                                fetchAllProfiles(); // Recarrega dados dos perfis
                                currentUser = profileDAO.findUserByUsername(currentUser.getUsername());
                            }
                        }
                    
                    // Salva estatísticas 2P
                    } else if (currentGameMode == GameMode.TWO_PLAYER) {
                         if (currentUser != null && currentUser2 != null) {
                            PlayerProfile winner = null;
                            PlayerProfile loser = null;
                            
                            if (p1_over && !p2_over) { // P2 Venceu
                                board2.addWin();
                                winner = currentUser2;
                                loser = currentUser;
                            } else if (p2_over && !p1_over) { // P1 Venceu
                                board1.addWin();
                                winner = currentUser;
                                loser = currentUser2;
                            }
                            
                            // Atualiza o perfil de ambos (vitória/derrota)
                            if (winner != null && loser != null) {
                                profileDAO.updateStats2P(winner.getUserID(), loser.getUserID());
                            }
                         }
                    }
                    
                    gameOverSelection = 0; // Reseta a seleção do menu "Game Over"
                }
            }
            
        }
        
        // Atualiza a tela independentemente do estado (jogo rodando ou menu)
        updateView();
    }

    /**
     * Gerencia a lógica de queda de peça e animações de limpeza de linha.
     */
    private void handlePlayerLogic(Board board, long currentTime, long lastMoveTime, long delay) {
        if (board.isGameOver()) return; 
        
        // Se estiver animando a limpeza de linha, apenas conta o tempo
        if (board.isAnimatingLineClear()) {
            board.decrementLineClearTimer();
            if (board.getLineClearTimer() <= 0) {
                board.finishLineClear(); // Executa a lógica de limpeza
                if (!board.isGameOver()) board.newPiece(); // Gera nova peça
            }
            return; // Pula a lógica de movimento
        }
        
        // Se o jogo estiver rodando, processa a queda da peça
        if (board.isStarted()) {
            if (currentTime - lastMoveTime > delay) board.movePieceDown();
        }
    }


    /**
     * Envia todos os dados de estado atualizados para a View (GameFrame e seus painéis filhos).
     */
    private void updateView() {
        // Atualiza os componentes do Jogo (Tabuleiro, Infos)
        gameFrame.getGamePanel().getBoardPanel1().updateBoard(board1);
        gameFrame.getGamePanel().getInfoPanel1().updateInfo(board1);
        gameFrame.getGamePanel().getGarbageBar1().updateBoard(board1);
        
        gameFrame.getGamePanel().getBoardPanel2().updateBoard(board2);
        gameFrame.getGamePanel().getInfoPanel2().updateInfo(board2);
        gameFrame.getGamePanel().getGarbageBar2().updateBoard(board2);
        
        Theme currentTheme = Theme.AVAILABLE_THEMES[currentThemeIndex];
        
        // Atualiza o tema de todos os componentes
        gameFrame.getOverlayPanel().updateTheme(currentTheme);
        gameFrame.getGamePanel().updateTheme(currentTheme);
        
        // Define o High Score a ser exibido nos InfoPanels
        int hScore1 = (currentUser != null) ? currentUser.getHighScore1P() : 0;
        gameFrame.getGamePanel().getInfoPanel1().setHighScore(hScore1);

        if (currentGameMode == GameMode.TWO_PLAYER && currentUser2 != null) {
            int hScore2 = currentUser2.getHighScore1P();
            gameFrame.getGamePanel().getInfoPanel2().setHighScore(hScore2);
        } else {
            gameFrame.getGamePanel().getInfoPanel2().setHighScore(0); 
        }

        // Envia todos os dados de estado para o Overlay (menus, ranking, etc.)
        gameFrame.getOverlayPanel().updateMenuState(
            board1, 
            board2, 
            currentGameMode, 
            currentScreen, 
            mainMenuSelection, 
            modeSelectSelection,
            gameOverSelection,
            pauseMenuSelection,
            rankingModeSelection, 
            profileListSelection, 
            topSoloScores,     
            top2PWins,         
            allProfiles,       
            playerNameInput,   
            profileErrorMessage,
            currentUser,
            currentUser2       
        );

        gameFrame.repaint(); // Redesenha a janela
    }

    /**
     * Roteador principal de entrada do teclado.
     */
    @Override
    public void keyPressed(KeyEvent e) {
        
        // Limpa a mensagem de erro em telas de perfil ao digitar
        if (currentScreen == GameScreen.PROFILE_SELECTION ||
            currentScreen == GameScreen.PROFILE_SELECTION_P2 ||
            currentScreen == GameScreen.PROFILE_CREATE) {
            profileErrorMessage = null;
        }

        // Roteia para handlers de telas específicas (que precisam de inputs complexos)
        if (currentScreen == GameScreen.PROFILE_SELECTION) {
            handleProfileListKeys(e, 1); 
            updateView();
            return; 
        }
        if (currentScreen == GameScreen.PROFILE_SELECTION_P2) {
            handleProfileListKeys(e, 2); 
            updateView();
            return;
        }
        if (currentScreen == GameScreen.PROFILE_CREATE) {
            handleProfileCreateKeys(e);
            updateView();
            return;
        }
        if (currentScreen == GameScreen.RANKING_MODE_SELECT) {
            handleRankingModeSelectKeys(e);
            updateView();
            return;
        }

        // Roteia para handlers de estado (Jogo Ativo vs. Menu Principal)
        int keycode = e.getKeyCode(); 
        boolean isGameActive = board1.isStarted() || board2.isStarted();
        
        if (isGameActive) {
            boolean isGameOver = board1.isGameOver() || (currentGameMode == GameMode.TWO_PLAYER && board2.isGameOver());
            
            if (isGameOver) {
                handleGameOverKeys(keycode);
            } else if (currentScreen == GameScreen.PAUSED_MAIN ||
                       currentScreen == GameScreen.PAUSED_CONTROLS ||
                       currentScreen == GameScreen.PAUSED_RULES) {
                handlePausedKeys(keycode);
            } else {
                handleGameKeys(keycode);
            }
        } else {
            handleMenuKeys(keycode); 
        }
        
        updateView();
    }
    
    /**
     * Gerencia a navegação na lista de perfis (Seleção de P1 e P2).
     */
    private void handleProfileListKeys(KeyEvent e, int playerNum) {
        int keycode = e.getKeyCode();
        
        int numOptions = (allProfiles != null ? allProfiles.size() : 0) + 1; // +1 para a opção "Criar Novo"

        if (keycode == KeyEvent.VK_UP || keycode == KeyEvent.VK_W) {
            profileListSelection = (profileListSelection - 1 + numOptions) % numOptions;
        }
        if (keycode == KeyEvent.VK_DOWN || keycode == KeyEvent.VK_S) {
            profileListSelection = (profileListSelection + 1) % numOptions;
        }
        
        // Voltar para a seleção de modo
        if (keycode == KeyEvent.VK_ESCAPE || keycode == KeyEvent.VK_BACK_SPACE) {
            currentScreen = GameScreen.MODE_SELECT;
            allProfiles = null; 
            currentUser = null;
            currentUser2 = null;
            return;
        }

        if (keycode == KeyEvent.VK_ENTER) {
            // Caso: selecionou [CRIAR NOVO USUÁRIO]
            if (profileListSelection == numOptions - 1) { 
                playerNameInput = "";
                currentScreen = GameScreen.PROFILE_CREATE; 
            
            // Caso: selecionou um perfil existente
            } else if (allProfiles != null && profileListSelection < allProfiles.size()) {
                PlayerProfile selectedProfile = allProfiles.get(profileListSelection);
                
                // Validação: P2 não pode ser o mesmo que P1
                if (playerNum == 2 && currentUser != null && selectedProfile.getUserID() == currentUser.getUserID()) {
                    System.err.println("GameController: P2 não pode ser o mesmo que P1.");
                    profileErrorMessage = "P2 NÃO PODE SER IGUAL AO P1!";
                    return; 
                }
                
                if (playerNum == 1) {
                    currentUser = profileDAO.findUserByUsername(selectedProfile.getUsername()); 
                    System.out.println("GameController: P1 logado como " + currentUser.getUsername());
                    
                    if (currentGameMode == GameMode.ONE_PLAYER) {
                        startGame(); // Jogo 1P começa direto
                    } else {
                        profileListSelection = 0; // Prepara para seleção P2
                        currentScreen = GameScreen.PROFILE_SELECTION_P2;
                    }
                } else { // playerNum == 2
                    currentUser2 = profileDAO.findUserByUsername(selectedProfile.getUsername()); 
                    System.out.println("GameController: P2 logado como " + currentUser2.getUsername());
                    startGame(); // Jogo 2P começa após P2
                }
            }
        }
    }

    /**
     * Gerencia a entrada de texto na tela de criação de perfil.
     */
    private void handleProfileCreateKeys(KeyEvent e) {
        int keycode = e.getKeyCode();

        if (keycode == KeyEvent.VK_ENTER) {
            String cleanUsername = playerNameInput.trim();
            if (cleanUsername.isEmpty()) return;

            // Validação: P2 não pode ser igual a P1
            if (currentGameMode == GameMode.TWO_PLAYER && 
                currentUser != null && 
                cleanUsername.equalsIgnoreCase(currentUser.getUsername())) {
                
                System.err.println("GameController: P2 não pode ser o mesmo que P1 (Criação).");
                profileErrorMessage = "P2 NÃO PODE SER IGUAL AO P1!";
                return;
            }
            
            // Validação: Nome de usuário já existe
            PlayerProfile existingUser = profileDAO.findUserByUsername(cleanUsername);
            
            if (existingUser != null) {
                System.err.println("GameController: Nome de usuário já existe.");
                profileErrorMessage = "NOME DE USUÁRIO JÁ EXISTE!";
                return; 
            }

            // Cria o jogador
            PlayerProfile profile = profileDAO.findOrCreatePlayer(cleanUsername);
            if (profile == null) {
                System.err.println("GameController: Erro ao criar perfil (nome inválido?).");
                profileErrorMessage = "NOME INVÁLIDO!";
                return;
            }
            
            // Define o perfil criado como P1 ou P2
            if (currentUser == null) {
                currentUser = profile;
                System.out.println("GameController: P1 criado/logado como " + currentUser.getUsername());
                
                if (currentGameMode == GameMode.ONE_PLAYER) {
                    startGame(); 
                } else {
                    fetchAllProfiles(); // Atualiza a lista para o P2 ver
                    profileListSelection = 0; 
                    currentScreen = GameScreen.PROFILE_SELECTION_P2; 
                }
            } else { 
                currentUser2 = profile;
                System.out.println("GameController: P2 criado/logado como " + currentUser2.getUsername());
                startGame(); 
            }

        } else if (keycode == KeyEvent.VK_BACK_SPACE) {
            // Apaga o último caractere
            if (!playerNameInput.isEmpty()) {
                playerNameInput = playerNameInput.substring(0, playerNameInput.length() - 1);
            }
        } else if (keycode == KeyEvent.VK_ESCAPE) {
            // Volta para a tela de seleção anterior
            if (currentUser == null) { 
                currentScreen = GameScreen.PROFILE_SELECTION;
            } else { 
                currentScreen = GameScreen.PROFILE_SELECTION_P2;
            }
            playerNameInput = "";
        
        } else {
            // Adiciona o caractere digitado (se válido)
            char c = e.getKeyChar();
            if ((Character.isLetterOrDigit(c)) && playerNameInput.length() < 15) {
                playerNameInput += Character.toUpperCase(c);
            }
        }
    }
    
    /**
     * Gerencia a seleção de modo de ranking (1P ou 2P).
     */
    private void handleRankingModeSelectKeys(KeyEvent e) {
        int keycode = e.getKeyCode();

        if (keycode == KeyEvent.VK_UP || keycode == KeyEvent.VK_W) {
            rankingModeSelection = (rankingModeSelection - 1 + RANKING_MODE_OPTIONS) % RANKING_MODE_OPTIONS;
        }
        if (keycode == KeyEvent.VK_DOWN || keycode == KeyEvent.VK_S) {
            rankingModeSelection = (rankingModeSelection + 1) % RANKING_MODE_OPTIONS;
        }
        if (keycode == KeyEvent.VK_ESCAPE || keycode == KeyEvent.VK_BACK_SPACE) {
            currentScreen = GameScreen.MAIN_MENU;
        }
        
        if (keycode == KeyEvent.VK_ENTER) {
            switch (rankingModeSelection) {
                case 0: // Ranking 1P
                    this.topSoloScores = soloScoreDAO.getTopSoloScores(10); 
                    currentScreen = GameScreen.RANKING_SCREEN; 
                    break;
                case 1: // Ranking 2P
                    this.top2PWins = profileDAO.getTopPlayerWins(10);
                    currentScreen = GameScreen.RANKING_SCREEN_2P;
                    break;
            }
        }
    }

    /**
     * Prepara e inicia o estado de jogo (1P ou 2P).
     */
    private void startGame() {
        gameFrame.getGamePanel().setMode(currentGameMode); 
        gameFrame.packAndCenter(); // Ajusta o tamanho da janela para 1P ou 2P
        
        // Define os nomes dos jogadores nos InfoPanels
        if (currentUser != null) {
            gameFrame.getGamePanel().getInfoPanel1().setPlayerName(currentUser.getUsername());
        }
        if (currentGameMode == GameMode.TWO_PLAYER && currentUser2 != null) {
            gameFrame.getGamePanel().getInfoPanel2().setPlayerName(currentUser2.getUsername());
        }
        
        board1.start();
        if (currentGameMode == GameMode.TWO_PLAYER) {
            board2.start();
        }

        if (backgroundMusic != null) {
            backgroundMusic.playMusic();
        }

        if (!timer.isRunning()) {
            timer.start();
        }
        // Reseta os timers de queda de peça
        long startTime = System.currentTimeMillis();
        lastPieceMoveTime1 = startTime; 
        lastPieceMoveTime2 = startTime;
        
        currentScreen = null; // Remove qualquer overlay de menu
    }

    /**
     * Gerencia a navegação no menu de "Game Over".
     */
    private void handleGameOverKeys(int keycode) {
        if (keycode == KeyEvent.VK_UP || keycode == KeyEvent.VK_W) {
            gameOverSelection = (gameOverSelection - 1 + GAMEOVER_MENU_OPTIONS) % GAMEOVER_MENU_OPTIONS;
        }
        if (keycode == KeyEvent.VK_DOWN || keycode == KeyEvent.VK_S) {
            gameOverSelection = (gameOverSelection + 1) % GAMEOVER_MENU_OPTIONS;
        }
        
        if (keycode == KeyEvent.VK_ENTER) {
            if (gameOverSelection == 0) { // Reiniciar
                startGame(); 
            } else { // Voltar ao Menu
                goToMenu();
            }
        }
    }

    /**
     * Gerencia a navegação nos menus de Pausa (Principal, Controles, Regras).
     */
    private void handlePausedKeys(int keycode) {
        
        if (currentScreen == null) return; 

        switch (currentScreen) {
            // Navegação no menu de pausa principal
            case PAUSED_MAIN: 
                if (keycode == KeyEvent.VK_P) { // Tecla de atalho para despausar
                    unpauseGame();
                    return;
                }
                if (keycode == KeyEvent.VK_UP || keycode == KeyEvent.VK_W) {
                    pauseMenuSelection = (pauseMenuSelection - 1 + PAUSE_MENU_OPTIONS) % PAUSE_MENU_OPTIONS;
                }
                if (keycode == KeyEvent.VK_DOWN || keycode == KeyEvent.VK_S) {
                    pauseMenuSelection = (pauseMenuSelection + 1) % PAUSE_MENU_OPTIONS;
                }
                if (keycode == KeyEvent.VK_ENTER) {
                    switch (pauseMenuSelection) {
                        case 0: unpauseGame(); break; 
                        case 1: currentScreen = GameScreen.PAUSED_CONTROLS; break; 
                        case 2: currentScreen = GameScreen.PAUSED_RULES; break; 
                        case 3: goToMenu(); break; 
                    }
                }
                break;
                
            // Voltar das telas de Controles ou Regras
            case PAUSED_CONTROLS:
            case PAUSED_RULES:
                if (keycode == KeyEvent.VK_ENTER || keycode == KeyEvent.VK_ESCAPE || keycode == KeyEvent.VK_BACK_SPACE) {
                    currentScreen = GameScreen.PAUSED_MAIN;
                }
                break;
            default:
                currentScreen = GameScreen.PAUSED_MAIN;
                break;
        }
    }
    
    /**
     * Sai do estado de pausa e retoma o jogo.
     */
    private void unpauseGame() {
        currentScreen = null; // Remove o overlay de pausa
        if (backgroundMusic != null) {
            backgroundMusic.playMusic();
        }
        // Reseta os timers de queda para evitar queda imediata
        long currentTime = System.currentTimeMillis();
        lastPieceMoveTime1 = currentTime;
        lastPieceMoveTime2 = currentTime;
    }
    
    /**
     * Busca a lista completa de perfis do banco de dados.
     */
    private void fetchAllProfiles() {
        this.allProfiles = profileDAO.getAllPlayerProfiles();
    }

    /**
     * Para a música, reseta os tabuleiros e retorna ao menu principal.
     */
    private void goToMenu() {
        if (backgroundMusic != null) {
            backgroundMusic.stopMusic();
        }
        
        board1.resetForMenu();
        board2.resetForMenu();
        
        // Limpa os nomes dos jogadores da UI
        gameFrame.getGamePanel().getInfoPanel1().setPlayerName(null);
        gameFrame.getGamePanel().getInfoPanel2().setPlayerName(null);
        
        // Truque de layout: força a janela a recalcular o tamanho máximo (2P)
        // e depois define o modo visual para 1P (padrão do menu).
        gameFrame.getGamePanel().setMode(GameController.GameMode.TWO_PLAYER);
        gameFrame.packAndCenter(); 
        gameFrame.getGamePanel().setMode(GameController.GameMode.ONE_PLAYER); 

        // Reseta todo o estado da sessão
        currentScreen = GameScreen.MAIN_MENU;
        mainMenuSelection = 0;
        topSoloScores = null; 
        top2PWins = null; 
        allProfiles = null; 
        
        currentUser = null;
        currentUser2 = null; 
        playerNameInput = "";
        profileErrorMessage = null; 
    }

    /**
     * Gerencia as teclas de ação durante o jogo (movimento, rotação, etc.).
     */
    private void handleGameKeys(int keycode) {
        
        // --- Controles Globais ---
        if (keycode == KeyEvent.VK_T) { // Mudar Tema
            currentThemeIndex = (currentThemeIndex + 1) % Theme.AVAILABLE_THEMES.length;
            return;
        }
        if (keycode == KeyEvent.VK_G) { // Alternar Peça Fantasma (Ghost Piece)
            board1.toggleGhostPiece();
            if (currentGameMode == GameMode.TWO_PLAYER) board2.toggleGhostPiece();
            return;
        }
        if (keycode == KeyEvent.VK_P) { // Pausar
             currentScreen = GameScreen.PAUSED_MAIN;
             pauseMenuSelection = 0; 
             if (backgroundMusic != null) {
                 backgroundMusic.stopMusic();
             }
             return;
        }
        
        // Verifica se o jogador pode se mover (não está em animação de linha)
        boolean p1_canPlay = !board1.isAnimatingLineClear();
        boolean p2_canPlay = (currentGameMode == GameMode.TWO_PLAYER) && !board2.isAnimatingLineClear();
        
        // --- Controles 1P ---
        if (currentGameMode == GameMode.ONE_PLAYER && p1_canPlay) {
            switch (keycode) {
                case KeyEvent.VK_LEFT: board1.moveLeft(); break;
                case KeyEvent.VK_RIGHT: board1.moveRight(); break;
                case KeyEvent.VK_DOWN:
                    board1.movePieceDown();
                    lastPieceMoveTime1 = System.currentTimeMillis(); // Reseta o timer de queda
                    break;
                case KeyEvent.VK_UP: board1.rotateRight(); break;
                case KeyEvent.VK_Z: board1.rotateLeft(); break;
                case KeyEvent.VK_SPACE:
                    board1.dropDown();
                    lastPieceMoveTime1 = System.currentTimeMillis(); // Reseta o timer de queda
                    break;
            }
        }
        
        // --- Controles 2P ---
        if (currentGameMode == GameMode.TWO_PLAYER) {
            switch (keycode) {
                // P1 (WASD)
                case KeyEvent.VK_A: if (p1_canPlay) board1.moveLeft(); break;
                case KeyEvent.VK_D: if (p1_canPlay) board1.moveRight(); break;
                case KeyEvent.VK_S: 
                    if (p1_canPlay) {
                        board1.movePieceDown();
                        lastPieceMoveTime1 = System.currentTimeMillis();
                    }
                    break;
                case KeyEvent.VK_W: if (p1_canPlay) board1.rotateRight(); break;
                case KeyEvent.VK_Q: if (p1_canPlay) board1.rotateLeft(); break;
                case KeyEvent.VK_SPACE: 
                    if (p1_canPlay) {
                        board1.dropDown();
                        lastPieceMoveTime1 = System.currentTimeMillis();
                    }
                    break;
                // P2 (Setas)
                case KeyEvent.VK_LEFT: if (p2_canPlay) board2.moveLeft(); break;
                case KeyEvent.VK_RIGHT: if (p2_canPlay) board2.moveRight(); break;
                case KeyEvent.VK_DOWN: 
                    if (p2_canPlay) {
                        board2.movePieceDown();
                        lastPieceMoveTime2 = System.currentTimeMillis();
                    }
                    break;
                case KeyEvent.VK_UP: if (p2_canPlay) board2.rotateRight(); break;
                case KeyEvent.VK_M: if (p2_canPlay) board2.rotateLeft(); break;
                case KeyEvent.VK_N: 
                    if (p2_canPlay) {
                        board2.dropDown();
                        lastPieceMoveTime2 = System.currentTimeMillis();
                    }
                    break;
            }
        }
    }

    /**
     * Gerencia a navegação nos menus principais (não-jogo).
     */
    private void handleMenuKeys(int keycode) {
        
        if (currentScreen == null) return; 

        switch (currentScreen) {
            
            case MAIN_MENU:
                if (keycode == KeyEvent.VK_UP || keycode == KeyEvent.VK_W) {
                    mainMenuSelection = (mainMenuSelection - 1 + MAIN_MENU_OPTIONS) % MAIN_MENU_OPTIONS;
                }
                if (keycode == KeyEvent.VK_DOWN || keycode == KeyEvent.VK_S) {
                    mainMenuSelection = (mainMenuSelection + 1) % MAIN_MENU_OPTIONS;
                }
                if (keycode == KeyEvent.VK_ENTER) {
                    switch (mainMenuSelection) {
                        case 0: // Jogar
                            currentScreen = GameScreen.MODE_SELECT; 
                            modeSelectSelection = 0;
                            break; 
                        case 1: // Ranking
                            currentScreen = GameScreen.RANKING_MODE_SELECT; 
                            rankingModeSelection = 0;
                            break; 
                        case 2: currentScreen = GameScreen.RULES_SCREEN; break; 
                        case 3: currentScreen = GameScreen.CONTROLS_SCREEN; break; 
                        case 4: System.exit(0); break; // Sair
                    }
                }
                break;
                
            case MODE_SELECT:
                if (keycode == KeyEvent.VK_UP || keycode == KeyEvent.VK_W) {
                    modeSelectSelection = (modeSelectSelection - 1 + MODE_SELECT_OPTIONS) % MODE_SELECT_OPTIONS;
                }
                if (keycode == KeyEvent.VK_DOWN || keycode == KeyEvent.VK_S) {
                    modeSelectSelection = (modeSelectSelection + 1) % MODE_SELECT_OPTIONS;
                }
                if (keycode == KeyEvent.VK_ENTER) {
                    if (modeSelectSelection == 0) {
                        currentGameMode = GameMode.ONE_PLAYER;
                    } else {
                        currentGameMode = GameMode.TWO_PLAYER;
                    }
                    fetchAllProfiles(); // Busca perfis para a próxima tela
                    profileListSelection = 0; 
                    currentScreen = GameScreen.PROFILE_SELECTION; 
                }
                if (keycode == KeyEvent.VK_ESCAPE || keycode == KeyEvent.VK_BACK_SPACE) {
                    currentScreen = GameScreen.MAIN_MENU;
                }
                break;
                
            // Telas "informativas" que só precisam voltar
            case RANKING_SCREEN:
            case RANKING_SCREEN_2P: 
            case RULES_SCREEN:
            case CONTROLS_SCREEN:
                if (keycode == KeyEvent.VK_ENTER || keycode == KeyEvent.VK_ESCAPE || keycode == KeyEvent.VK_BACK_SPACE) {
                    // Se estivermos em uma tela de ranking, volta para a seleção de modo de ranking
                    if (currentScreen == GameScreen.RANKING_SCREEN || currentScreen == GameScreen.RANKING_SCREEN_2P) {
                        currentScreen = GameScreen.RANKING_MODE_SELECT;
                        topSoloScores = null; // Limpa os dados carregados
                        top2PWins = null;     
                    } else {
                        currentScreen = GameScreen.MAIN_MENU;
                    }
                }
                break;
            
            default:
                currentScreen = GameScreen.MAIN_MENU;
                break;
        }
    }

    /**
     * Calcula o delay de queda da peça com base no nível atual.
     * O jogo fica mais rápido (delay menor) a cada nível.
     */
    private int getDelayForLevel(Board board) {
        // Garante que o delay nunca seja menor que 100ms
        return Math.max(100, INITIAL_DELAY - (board.getLevel() - 1) * 30);
    }
}