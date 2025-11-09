package com.tetris.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Gerencia a conexão com o banco de dados SQL Server usando o padrão Singleton.
 * Esta classe é responsável por carregar as credenciais de 'config.properties'
 * e fornecer uma instância de conexão única e centralizada para os DAOs.
 */
public class DatabaseManager {

    // --- Instância Singleton ---
    private static DatabaseManager instance;

    private Connection connection;

    // --- Configurações Carregadas ---
    // A String de conexão é final e inicializada no bloco estático.
    private static final String CONNECTION_STRING;

    /**
     * Bloco estático para carregar as configurações do 'config.properties'
     * ANTES que qualquer instância do DatabaseManager seja criada.
     * Isso garante que a conexão possa ser estabelecida no construtor.
     */
    static {
        Properties props = new Properties();
        String host, port, name, user, password;

        try (FileInputStream fis = new FileInputStream("config.properties")) {
            props.load(fis);
            
            host = props.getProperty("db.host");
            port = props.getProperty("db.port");
            name = props.getProperty("db.name");
            user = props.getProperty("db.user");
            password = props.getProperty("db.password");

            if (user == null || password == null) {
                throw new RuntimeException("Erro: 'db.user' ou 'db.password' não encontrado em config.properties");
            }

        } catch (IOException e) {
            System.err.println("--- ERRO CRÍTICO ---");
            System.err.println("Não foi possível carregar o arquivo 'config.properties'!");
            System.err.println("Verifique se o arquivo está na raiz do projeto.");
            e.printStackTrace();
            // Lança um erro para impedir a aplicação de continuar sem o banco
            throw new RuntimeException("Falha ao carregar config.properties", e);
        }

        // Constrói a String de conexão final
        CONNECTION_STRING = String.format(
            "jdbc:sqlserver://%s:%s;databaseName=%s;user=%s;password=%s;encrypt=true;trustServerCertificate=true;",
            host, port, name, user, password
        );
    }

    /**
     * Construtor privado (parte do padrão Singleton).
     * Tenta carregar o driver JDBC e conectar-se ao banco de dados.
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
            System.err.println("Verifique suas credenciais (config.properties), firewall e se o SQL Server (Docker) está rodando.");
            e.printStackTrace();
        }
    }

    /**
     * Ponto de acesso global para a instância do Singleton.
     * É 'synchronized' para garantir a segurança em ambientes com múltiplas threads
     * durante a primeira inicialização.
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
     * Método utilitário para fechar recursos do JDBC (Statement e ResultSet) de forma segura.
     */
    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        try { if (rs != null) rs.close(); } catch (SQLException e) { /* ignora */ }
        try { if (stmt != null) stmt.close(); } catch (SQLException e) { /* ignora */ }
        // A conexão (conn) não é fechada aqui, pois é gerenciada pelo Singleton.
    }
    
    /**
     * Sobrecarga do método close para quando não há um Connection a ser fechado.
     */
    public static void close(Statement stmt, ResultSet rs) {
        close(null, stmt, rs);
    }
}