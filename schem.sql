/* ====================================================================
   TETRIS - SISTEMA ROBUSTO DE PERFIS E RANKING (1P e 2P)
   ==================================================================== */

/* Instrução: Crie um banco de dados (ex: 'USE TetrisDB;')
   e então execute este script para criar todas as tabelas.
*/
USE TetrisDB;
GO

/* 1. APAGA as tabelas antigas (se existirem) */
DROP TABLE IF EXISTS SoloScores;
DROP TABLE IF EXISTS MultiplayerMatches;
DROP TABLE IF EXISTS PlayerProfiles;
DROP TABLE IF EXISTS Scores;
DROP TABLE IF EXISTS Users;
DROP TABLE IF EXISTS Ranking;
GO

/* 2. CRIA a tabela de Perfis de Jogador (MELHORADA) */
CREATE TABLE PlayerProfiles (
    UserID INT IDENTITY(1,1) PRIMARY KEY,
    Username NVARCHAR(50) NOT NULL UNIQUE,
    DateCreated DATETIME DEFAULT GETDATE(),
    LastLogin DATETIME DEFAULT GETDATE(),
    
    -- Estatísticas 1P
    GamesPlayed_1P INT NOT NULL DEFAULT 0,
    HighScore_1P INT NOT NULL DEFAULT 0,
    TotalScore_1P BIGINT NOT NULL DEFAULT 0,
    
    -- Estatísticas 2P
    GamesPlayed_2P INT NOT NULL DEFAULT 0,
    Wins_2P INT NOT NULL DEFAULT 0,
    Losses_2P INT NOT NULL DEFAULT 0,
    
    INDEX IX_Username (Username),
    INDEX IX_HighScore_1P (HighScore_1P DESC),
    INDEX IX_Wins_2P (Wins_2P DESC)
);
GO

/* 3. CRIA a tabela de Scores Solo (Ranking 1P) */
CREATE TABLE SoloScores (
    ScoreID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL,
    Score INT NOT NULL,
    Level INT NOT NULL DEFAULT 1,
    LinesCleared INT NOT NULL DEFAULT 0,
    TetrisCount INT NOT NULL DEFAULT 0,
    DateAchieved DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT FK_SoloScores_PlayerProfiles FOREIGN KEY (UserID)
        REFERENCES PlayerProfiles(UserID)
        ON DELETE CASCADE,
    
    INDEX IX_Score (Score DESC),
    INDEX IX_UserID_Date (UserID, DateAchieved DESC)
);
GO

/* 4. CRIA a tabela de Partidas Multiplayer (Ranking 2P) */
CREATE TABLE MultiplayerMatches (
    MatchID INT IDENTITY(1,1) PRIMARY KEY,
    WinnerID INT NOT NULL,
    LoserID INT NOT NULL,
    WinnerScore INT NOT NULL,
    LoserScore INT NOT NULL,
    DatePlayed DATETIME DEFAULT GETDATE(),
    
    CONSTRAINT FK_Multiplayer_Winner FOREIGN KEY (WinnerID)
        REFERENCES PlayerProfiles(UserID)
        ON DELETE NO ACTION,
    
    CONSTRAINT FK_Multiplayer_Loser FOREIGN KEY (LoserID)
        REFERENCES PlayerProfiles(UserID)
        ON DELETE NO ACTION,
    
    INDEX IX_WinnerID (WinnerID),
    INDEX IX_LoserID (LoserID),
    INDEX IX_DatePlayed (DatePlayed DESC)
);
GO

PRINT 'Banco de dados configurado com sucesso!';
GO