@echo off
echo ----------------------------------------
echo Script de Build e Execucao Tetris (Windows)
echo ----------------------------------------

:: 1. Define as variaveis de ambiente (use ; como separador)
set CP_SEP=;
set DRIVER_JAR=lib\mssql-jdbc-13.2.1.jre11.jar

:: 2. Limpa builds antigos
echo Limpando builds antigos (pasta bin/)...
if exist bin ( rmdir /s /q bin )

:: 3. Cria a pasta bin
echo Criando pasta bin...
mkdir bin

:: 4. Encontra e compila todos os arquivos .java
echo Compilando codigo-fonte (.java)...
dir /s /B src\*.java > files.txt
javac -d bin -cp "src%CP_SEP%%DRIVER_JAR%" @files.txt
del files.txt

:: 5. Verifica se a compilacao falhou
if %ERRORLEVEL% NEQ 0 (
    echo ----------------------------------------
    echo ERRO: Falha na compilacao.
    echo Corrija os erros de codigo e tente novamente.
    echo ----------------------------------------
    pause
    exit /b 1
)

:: 6. Copia os recursos (audio, etc.)
echo Copiando todos os recursos (audio, txt, etc.) para a pasta 'bin'...
xcopy src bin /s /e /i /y /exclude:*.java > nul

:: 7. Executa o programa!
echo ----------------------------------------
echo Build concluido. Iniciando o Jogo...
echo ----------------------------------------
set CLASSPATH=bin%CP_SEP%%DRIVER_JAR%
java -cp "%CLASSPATH%" com.tetris.Main