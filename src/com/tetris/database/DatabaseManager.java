package com.tetris.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gerencia a conexão com o banco de dados SQL Server usando o padrão Singleton.
 * Isso garante que o jogo inteiro use apenas UMA conexão.
 */
public class DatabaseManager {

    // --- Instância Singleton ---
    private static DatabaseManager instance;

    private Connection connection;

    // --- Suas Configurações (Prontas!) ---
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "1433"; // Porta padrão do SQL Server
    private static final String DB_NAME = "TetrisDB";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "Papagaio@83";
    
    // String de conexão final
    private static final String CONNECTION_STRING = 
        String.format("jdbc:sqlserver://%s:%s;databaseName=%s;user=%s;password=%s;encrypt=true;trustServerCertificate=true;",
        DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD);

    /**
     * Construtor privado (parte do padrão Singleton).
     * Tenta carregar o driver e conectar ao banco.
     */
    private DatabaseManager() {
        try {
            // 1. Carrega a classe do driver JDBC
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            
            System.out.println("DatabaseManager: Conectando ao SQL Server...");
            
            // 2. Tenta estabelecer a conexão
            this.connection = DriverManager.getConnection(CONNECTION_STRING);
            
            System.out.println("DatabaseManager: Conexão estabelecida com sucesso!");

        } catch (ClassNotFoundException e) {
            System.err.println("--- ERRO CRÍTICO ---");
            System.err.println("Driver JDBC do SQL Server não encontrado!");
            System.err.println("Verifique se o JAR está na pasta 'lib' e se o 'run.sh' está correto.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("--- ERRO CRÍTICO ---");
            System.err.println("Falha ao conectar ao banco de dados!");
            System.err.println("Verifique suas credenciais, firewall e se o SQL Server (Docker) está rodando.");
            e.printStackTrace();
        }
    }

    /**
     * Ponto de acesso global para a instância do Singleton.
     * É 'synchronized' para evitar problemas com múltiplas threads.
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Obtém a conexão ativa com o banco.
     */
    public Connection getConnection() {
        try {
            // Verifica se a conexão foi perdida (ex: timeout) e reconecta se necessário
            if (this.connection == null || this.connection.isClosed()) {
                System.out.println("DatabaseManager: Conexão perdida. Reconectando...");
                this.connection = DriverManager.getConnection(CONNECTION_STRING);
            }
        } catch (SQLException e) {
            System.err.println("Erro ao verificar/recriar conexão: " + e.getMessage());
        }
        return this.connection;
    }

    /**
     * Helper para fechar recursos do JDBC de forma segura.
     */
    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignora */ }
        try { if (stmt != null) stmt.close(); } catch (SQLException e) { /* ignora */ }
        // Não fechamos a 'conn' aqui
    }
    
    public static void close(Statement stmt, ResultSet rs) {
        close(null, stmt, rs);
    }
}