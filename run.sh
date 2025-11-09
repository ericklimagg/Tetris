#!/bin/bash

# --- NOVO: Define o Classpath ---
# Define o separador de classpath (é ":" no Linux/macOS)
CP_SEP=":"
# ⚠️ ATENÇÃO: Confirme que este é o nome do JAR na sua pasta 'lib'
DRIVER_JAR="lib/mssql-jdbc-13.2.1.jre11.jar"
# Define o classpath completo
CLASSPATH="bin${CP_SEP}${DRIVER_JAR}"

# 1. Limpa builds antigos
echo "Limpando builds antigos (pasta bin/ e arquivos .class)..."
rm -rf bin
find src -name "*.class" -delete

# 2. Cria a pasta de 'build' (saída)
echo "Criando pasta bin..."
mkdir bin

# 3. Compila TODOS os arquivos .java
echo "Compilando código-fonte (.java)..."
# --- ATUALIZADO: Adiciona o driver ao classpath de compilação ---
javac -d bin -cp "src${CP_SEP}${DRIVER_JAR}" $(find src -name "*.java")

# 4. Verifica se a compilação falhou
if [ $? -ne 0 ]; then
    echo "----------------------------------------"
    echo "ERRO: Falha na compilação."
    echo "Corrija os erros de código e tente novamente."
    echo "----------------------------------------"
    exit 1
fi

# 5. Copia TODOS os recursos (áudio, txt, etc.) da 'src' para a 'bin'
echo "Copiando todos os recursos (áudio, txt, etc.) para a pasta 'bin'..."
rsync -av --prune-empty-dirs --exclude="*.java" src/ bin/
# Copia também o highscore.txt da raiz
cp highscore.txt bin/

# 6. Executa o programa!
echo "----------------------------------------"
echo "Build concluído. Iniciando o Jogo..."
echo "----------------------------------------"
# --- ATUALIZADO: Usa a variável CLASSPATH definida no início ---
java -cp "${CLASSPATH}" com.tetris.Main
