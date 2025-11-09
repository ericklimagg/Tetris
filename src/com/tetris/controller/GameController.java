package com.tetris.controller;

import com.tetris.model.Board;
import com.tetris.model.Theme;
import com.tetris.view.GameFrame;
import com.tetris.audio.AudioManager;
import com.tetris.database.PlayerProfileDAO;
import com.tetris.database.SoloScoreDAO;
import com.tetris.database.MultiplayerMatchDAO;
import com.tetris.database.PlayerProfile;
import com.tetris.database.SoloScoreEntry;
import com.tetris.database.RankingEntry2P;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List; 

/**
 * O Controller no padrão MVC.
 * ATUALIZADO: Sistema completo de perfis e ranking (1P e 2P).
 */
public class GameController extends KeyAdapter implements ActionListener {

    public enum GameMode {
        ONE_PLAYER,
        TWO_PLAYER
    }
    
    public enum GameScreen {
        MAIN_MENU,
        MODE_SELECT,
        RANKING_SCREEN_1P,
        RANKING_SCREEN_2P,
        RULES_SCREEN,
        CONTROLS_SCREEN,
        PAUSED_MAIN,
        PAUSED_CONTROLS,
        PAUSED_RULES,
        PROFILE_SELECTION,
        PLAYER2_PROFILE_SELECTION
    }

    private static final int INITIAL_DELAY = 400;
    private static final int GAME_LOOP_DELAY = 33; 

    private final GameFrame gameFrame;
    private final Board board1; 
    private final Board board2; 
    private final Timer timer;
    private final AudioManager backgroundMusic;

    private final PlayerProfileDAO profileDAO;
    private final SoloScoreDAO soloScoreDAO;
    private final MultiplayerMatchDAO multiplayerDAO;
    
    private PlayerProfile currentUser = null;
    private PlayerProfile player2User = null;
    
    private List<SoloScoreEntry> topSoloScores;
    private List<RankingEntry2P> topMultiplayerRanking;
    
    private String playerNameInput = "";
    private String player2NameInput = "";

    private int currentThemeIndex = 0;
    private GameMode currentGameMode = GameMode.ONE_PLAYER;
    
    private GameScreen currentScreen = GameScreen.MAIN_MENU;
    private int mainMenuSelection = 0;
    private final int MAIN_MENU_OPTIONS = 6;
    private int modeSelectSelection = 0;
    private final int MODE_SELECT_OPTIONS = 2; 
    
    private int gameOverSelection = 0; 
    private final int GAMEOVER_MENU_OPTIONS = 2;
    
    private int pauseMenuSelection = 0; 
    private final int PAUSE_MENU_OPTIONS = 4;
    
    private long lastPieceMoveTime1;
    private long lastPieceMoveTime2;

    public GameController(GameFrame gameFrame, Board board1, Board board2) {
        this.gameFrame = gameFrame;
        this.board1 = board1;
        this.board2 = board2;
        
        this.timer = new Timer(GAME_LOOP_DELAY, this);
        
        this.gameFrame.getGamePanel().addKeyListener(this);
        this.gameFrame.getGamePanel().setFocusable(true);

        System.out.println("GameController: Tentando inicializar o AudioManager...");
        this.backgroundMusic = new AudioManager("/com/tetris/audio/background-music.wav");
        
        this.profileDAO = new PlayerProfileDAO();
        this.soloScoreDAO = new SoloScoreDAO();
        this.multiplayerDAO = new MultiplayerMatchDAO();
    }

    public void start() {
        long startTime = System.currentTimeMillis();
        lastPieceMoveTime1 = startTime; 
        lastPieceMoveTime2 = startTime; 
        timer.start();
        updateView(); 
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        
        boolean isGameRunning = board1.isStarted() &&
                                currentScreen != GameScreen.PAUSED_MAIN &&
                                currentScreen != GameScreen.PAUSED_CONTROLS &&
                                currentScreen != GameScreen.PAUSED_RULES &&
                                currentScreen != GameScreen.PROFILE_SELECTION &&
                                currentScreen != GameScreen.PLAYER2_PROFILE_SELECTION;
                                
        if (isGameRunning) {
            
            long currentTime = System.currentTimeMillis();
            long delay1 = getDelayForLevel(board1);
            handlePlayerLogic(board1, currentTime, lastPieceMoveTime1, delay1);
            if (currentTime - lastPieceMoveTime1 > delay1) {
                lastPieceMoveTime1 = currentTime;
            }
            if (currentGameMode == GameMode.TWO_PLAYER) {
                long delay2 = getDelayForLevel(board2);
                handlePlayerLogic(board2, currentTime, lastPieceMoveTime2, delay2);
                if (currentTime - lastPieceMoveTime2 > delay2) {
                    lastPieceMoveTime2 = currentTime;
                }
            }
            if (currentGameMode == GameMode.TWO_PLAYER) {
                int p1_garbage = board1.getOutgoingGarbage();
                int p2_garbage = board2.getOutgoingGarbage();
                if (p1_garbage > 0 || p2_garbage > 0) {
                    if (p1_garbage > p2_garbage) {
                        board2.addIncomingGarbage(p1_garbage - p2_garbage);
                    } else if (p2_garbage > p1_garbage) {
                        board1.addIncomingGarbage(p2_garbage - p1_garbage);
                    }
                    board1.clearOutgoingGarbage();
                    board2.clearOutgoingGarbage();
                }
            }
            
            boolean p1_over = board1.isGameOver();
            boolean p2_over = (currentGameMode == GameMode.TWO_PLAYER) && board2.isGameOver();
            boolean round_is_finished = (currentGameMode == GameMode.ONE_PLAYER) ? p1_over : (p1_over || p2_over);

            if (round_is_finished) {
                
                if (timer.isRunning()) { 
                    timer.stop();
                    
                    if (backgroundMusic != null) {
                        backgroundMusic.stopMusic();
                    }
                    
                    if (currentGameMode == GameMode.ONE_PLAYER && p1_over && currentUser != null) {
                        if (board1.getScore() > 0) {
                            soloScoreDAO.addScore(
                                currentUser.getUserID(), 
                                board1.getScore(),
                                board1.getLevel(),
                                board1.getLinesCleared(),
                                board1.getTetrisCount()
                            );
                        }
                        profileDAO.updateStats1P(currentUser.getUserID(), board1.getScore());
                        
                    } else if (currentGameMode == GameMode.TWO_PLAYER && currentUser != null && player2User != null) {
                        int winnerID = -1;
                        int loserID = -1;
                        int winnerScore = 0;
                        int loserScore = 0;
                        
                        if (p1_over && !p2_over) {
                            board2.addWin();
                            winnerID = player2User.getUserID();
                            loserID = currentUser.getUserID();
                            winnerScore = board2.getScore();
                            loserScore = board1.getScore();
                        } else if (p2_over && !p1_over) {
                            board1.addWin();
                            winnerID = currentUser.getUserID();
                            loserID = player2User.getUserID();
                            winnerScore = board1.getScore();
                            loserScore = board2.getScore();
                        }
                        
                        if (winnerID != -1 && loserID != -1) {
                            profileDAO.updateStats2P(winnerID, loserID);
                            multiplayerDAO.recordMatch(winnerID, loserID, winnerScore, loserScore);
                        }
                    }
                    
                    gameOverSelection = 0;
                }
            }
        }
        
        updateView();
    }

    private void handlePlayerLogic(Board board, long currentTime, long lastMoveTime, long delay) {
        if (board.isGameOver()) return; 
        if (board.isAnimatingLineClear()) {
            board.decrementLineClearTimer();
            if (board.getLineClearTimer() <= 0) {
                board.finishLineClear(); 
                if (!board.isGameOver()) board.newPiece();
            }
            return; 
        }
        if (board.isStarted()) {
            if (currentTime - lastMoveTime > delay) board.movePieceDown();
        }
    }

    private void updateView() {
        gameFrame.getGamePanel().getBoardPanel1().updateBoard(board1);
        gameFrame.getGamePanel().getInfoPanel1().updateInfo(board1);
        gameFrame.getGamePanel().getGarbageBar1().updateBoard(board1);
        
        gameFrame.getGamePanel().getBoardPanel2().updateBoard(board2);
        gameFrame.getGamePanel().getInfoPanel2().updateInfo(board2);
        gameFrame.getGamePanel().getGarbageBar2().updateBoard(board2);
        
        Theme currentTheme = Theme.AVAILABLE_THEMES[currentThemeIndex];
        
        gameFrame.getOverlayPanel().updateTheme(currentTheme);

        gameFrame.getOverlayPanel().updateMenuState(
            board1, 
            board2, 
            currentGameMode, 
            currentScreen, 
            mainMenuSelection, 
            modeSelectSelection,
            gameOverSelection,
            pauseMenuSelection,
            topSoloScores,     
            topMultiplayerRanking,
            playerNameInput,   
            player2NameInput,
            currentUser,
            player2User
        );

        gameFrame.getGamePanel().updateTheme(currentTheme);
        gameFrame.repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        
        if (currentScreen == GameScreen.PROFILE_SELECTION) {
            handleProfileSelectionKeys(e);
            updateView();
            return; 
        }
        
        if (currentScreen == GameScreen.PLAYER2_PROFILE_SELECTION) {
            handlePlayer2ProfileSelectionKeys(e);
            updateView();
            return;
        }

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
    
    private void handleProfileSelectionKeys(KeyEvent e) {
        int keycode = e.getKeyCode();

        if (keycode == KeyEvent.VK_ENTER) {
            String cleanUsername = playerNameInput.trim();
            if (!cleanUsername.isEmpty()) {
                this.currentUser = profileDAO.findOrCreatePlayer(cleanUsername);
                
                if (this.currentUser != null) {
                    System.out.println("GameController: P1 logado como " + this.currentUser.getUsername());
                    currentScreen = GameScreen.MODE_SELECT;
                } else {
                    System.err.println("GameController: Erro ao logar P1.");
                }
            }
        } else if (keycode == KeyEvent.VK_BACK_SPACE) {
            if (!playerNameInput.isEmpty()) {
                playerNameInput = playerNameInput.substring(0, playerNameInput.length() - 1);
            }
        } else if (keycode == KeyEvent.VK_ESCAPE) {
            currentScreen = GameScreen.MAIN_MENU;
        
        } else {
            char c = e.getKeyChar();
            if ((Character.isLetterOrDigit(c)) && playerNameInput.length() < 15) {
                playerNameInput += Character.toUpperCase(c);
            }
        }
    }
    
    private void handlePlayer2ProfileSelectionKeys(KeyEvent e) {
        int keycode = e.getKeyCode();

        if (keycode == KeyEvent.VK_ENTER) {
            String cleanUsername = player2NameInput.trim();
            if (!cleanUsername.isEmpty()) {
                if (currentUser != null && cleanUsername.equalsIgnoreCase(currentUser.getUsername())) {
                    System.out.println("GameController: P2 não pode ter o mesmo nome que P1!");
                    player2NameInput = "";
                    return;
                }
                
                this.player2User = profileDAO.findOrCreatePlayer(cleanUsername);
                
                if (this.player2User != null) {
                    System.out.println("GameController: P2 logado como " + this.player2User.getUsername());
                    startGame(GameMode.TWO_PLAYER);
                } else {
                    System.err.println("GameController: Erro ao logar P2.");
                }
            }
        } else if (keycode == KeyEvent.VK_BACK_SPACE) {
            if (!player2NameInput.isEmpty()) {
                player2NameInput = player2NameInput.substring(0, player2NameInput.length() - 1);
            }
        } else if (keycode == KeyEvent.VK_ESCAPE) {
            currentScreen = GameScreen.MODE_SELECT;
            player2NameInput = "";
        
        } else {
            char c = e.getKeyChar();
            if ((Character.isLetterOrDigit(c)) && player2NameInput.length() < 15) {
                player2NameInput += Character.toUpperCase(c);
            }
        }
    }

    private void startGame(GameMode mode) {
        currentGameMode = mode;
        
        gameFrame.getGamePanel().setMode(currentGameMode); 
        gameFrame.packAndCenter();
        
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
        long startTime = System.currentTimeMillis();
        lastPieceMoveTime1 = startTime; 
        lastPieceMoveTime2 = startTime;
        
        currentScreen = null; 
    }

    private void handleGameOverKeys(int keycode) {
        if (keycode == KeyEvent.VK_UP || keycode == KeyEvent.VK_W) {
            gameOverSelection = (gameOverSelection - 1 + GAMEOVER_MENU_OPTIONS) % GAMEOVER_MENU_OPTIONS;
        }
        if (keycode == KeyEvent.VK_DOWN || keycode == KeyEvent.VK_S) {
            gameOverSelection = (gameOverSelection + 1) % GAMEOVER_MENU_OPTIONS;
        }
        
        if (keycode == KeyEvent.VK_ENTER) {
            if (gameOverSelection == 0) { 
                startGame(currentGameMode); 
            } else { 
                goToMenu();
            }
        }
    }

    private void handlePausedKeys(int keycode) {
        
        if (currentScreen == null) return; 

        switch (currentScreen) {
            case PAUSED_MAIN: 
                if (keycode == KeyEvent.VK_P) { 
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
    
    private void unpauseGame() {
        currentScreen = null; 
        if (backgroundMusic != null) {
            backgroundMusic.playMusic();
        }
        long currentTime = System.currentTimeMillis();
        lastPieceMoveTime1 = currentTime;
        lastPieceMoveTime2 = currentTime;
    }
    
    private void goToMenu() {
        if (backgroundMusic != null) {
            backgroundMusic.stopMusic();
        }
        
        board1.resetForMenu();
        board2.resetForMenu();
        
        gameFrame.getGamePanel().setMode(GameController.GameMode.TWO_PLAYER);
        gameFrame.packAndCenter(); 
        gameFrame.getGamePanel().setMode(GameController.GameMode.ONE_PLAYER); 

        currentScreen = GameScreen.MAIN_MENU;
        mainMenuSelection = 0;
        topSoloScores = null;
        topMultiplayerRanking = null;
        
        currentUser = null;
        player2User = null;
        playerNameInput = "";
        player2NameInput = "";
    }

    private void handleGameKeys(int keycode) {
        
        if (keycode == KeyEvent.VK_T) {
            currentThemeIndex = (currentThemeIndex + 1) % Theme.AVAILABLE_THEMES.length;
            return;
        }
        if (keycode == KeyEvent.VK_G) {
            board1.toggleGhostPiece();
            if (currentGameMode == GameMode.TWO_PLAYER) board2.toggleGhostPiece();
            return;
        }
        if (keycode == KeyEvent.VK_P) {
             currentScreen = GameScreen.PAUSED_MAIN;
             pauseMenuSelection = 0; 
             if (backgroundMusic != null) {
                 backgroundMusic.stopMusic();
             }
             return;
        }
        
        boolean p1_canPlay = !board1.isAnimatingLineClear();
        boolean p2_canPlay = (currentGameMode == GameMode.TWO_PLAYER) && !board2.isAnimatingLineClear();
        if (currentGameMode == GameMode.ONE_PLAYER && p1_canPlay) {
            switch (keycode) {
                case KeyEvent.VK_LEFT: board1.moveLeft(); break;
                case KeyEvent.VK_RIGHT: board1.moveRight(); break;
                case KeyEvent.VK_DOWN:
                    board1.movePieceDown();
                    lastPieceMoveTime1 = System.currentTimeMillis();
                    break;
                case KeyEvent.VK_UP: board1.rotateRight(); break;
                case KeyEvent.VK_Z: board1.rotateLeft(); break;
                case KeyEvent.VK_SPACE:
                    board1.dropDown();
                    lastPieceMoveTime1 = System.currentTimeMillis();
                    break;
            }
        }
        if (currentGameMode == GameMode.TWO_PLAYER) {
            switch (keycode) {
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
                        case 0:
                            playerNameInput = ""; 
                            currentScreen = GameScreen.PROFILE_SELECTION; 
                            break; 
                        case 1:
                            this.topSoloScores = soloScoreDAO.getTopSoloScores(10); 
                            currentScreen = GameScreen.RANKING_SCREEN_1P; 
                            break; 
                        case 2:
                            this.topMultiplayerRanking = multiplayerDAO.getTopPlayers(10);
                            currentScreen = GameScreen.RANKING_SCREEN_2P;
                            break;
                        case 3: currentScreen = GameScreen.RULES_SCREEN; break; 
                        case 4: currentScreen = GameScreen.CONTROLS_SCREEN; break; 
                        case 5: System.exit(0); break; 
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
                        startGame(GameMode.ONE_PLAYER);
                    } else {
                        player2NameInput = "";
                        currentScreen = GameScreen.PLAYER2_PROFILE_SELECTION;
                    }
                }
                if (keycode == KeyEvent.VK_ESCAPE || keycode == KeyEvent.VK_BACK_SPACE) {
                    currentScreen = GameScreen.MAIN_MENU;
                    currentUser = null;
                }
                break;
                
            case RANKING_SCREEN_1P:
            case RANKING_SCREEN_2P:
            case RULES_SCREEN:
            case CONTROLS_SCREEN:
                if (keycode == KeyEvent.VK_ENTER || keycode == KeyEvent.VK_ESCAPE || keycode == KeyEvent.VK_BACK_SPACE) {
                    currentScreen = GameScreen.MAIN_MENU;
                }
                break;
            
            default:
                currentScreen = GameScreen.MAIN_MENU;
                break;
        }
    }

    private int getDelayForLevel(Board board) {
        return Math.max(100, INITIAL_DELAY - (board.getLevel() - 1) * 30);
    }
}