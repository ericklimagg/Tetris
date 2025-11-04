package com.tetris.audio;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.net.URL;

/**
 * Gerencia a reprodução de áudio no jogo.
 * Carrega e toca clipes de áudio, como a música de fundo.
 */
public class AudioManager {

    private Clip musicClip;

    public AudioManager(String path) {
        System.out.println("AudioManager: Tentando carregar áudio. Caminho original recebido: " + path);
        
        try {
            // --- INÍCIO DA CORREÇÃO ---
            // O código que chama este construtor está passando um caminho "sujo" (ex: "src/com/...")
            // Vamos limpar o caminho para que ele seja relativo à raiz do classpath (a pasta 'bin').

            // 1. Remove o prefixo "src/" se ele existir
            if (path.startsWith("src/")) {
                path = path.substring(4); // Remove os primeiros 4 caracteres ("src/")
                System.out.println("AudioManager: Path corrigido (removido 'src/'): " + path);
            }

            // 2. Garante que o caminho começa com "/" para ser absoluto do classpath
            if (!path.startsWith("/")) {
                path = "/" + path;
                System.out.println("AudioManager: Path corrigido (adicionado '/'): " + path);
            }
            // Agora, o 'path' será algo como "/com/tetris/audio/background-music.wav"
            // --- FIM DA CORREÇÃO ---


            URL url = AudioManager.class.getResource(path);
            
            if (url == null) {
                System.err.println("************************************************************");
                System.err.println("ERRO CRÍTICO: Áudio não encontrado COM O CAMINHO CORRIGIDO!");
                System.err.println("Caminho procurado: " + path);
                System.err.println("Verifique se o arquivo existe em: bin" + path);
                System.err.println("************************************************************");
                return;
            }
            
            System.out.println("AudioManager: Ficheiro de áudio encontrado em: " + url.getPath());

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
            System.out.println("AudioManager: AudioInputStream criado com sucesso.");
            
            musicClip = AudioSystem.getClip();
            System.out.println("AudioManager: Clip obtido com sucesso.");
            
            musicClip.open(audioStream);
            System.out.println("AudioManager: Clip aberto com sucesso. Pronto para tocar.");

        } catch (UnsupportedAudioFileException e) {
            System.err.println("************************************************************");
            System.err.println("ERRO CRÍTICO: Formato de áudio não suportado!");
            System.err.println("O ficheiro " + path + " pode estar corrompido ou num formato inválido (ex: MP3).");
            System.err.println("Por favor, tente converter o ficheiro para WAV (PCM Signed, 16 bit) novamente.");
            System.err.println("************************************************************");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("************************************************************");
            System.err.println("ERRO CRÍTICO: Erro de I/O ao ler o ficheiro de áudio.");
            System.err.println("************************************************************");
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            System.err.println("************************************************************");
            System.err.println("ERRO CRÍTICO: Linha de áudio não disponível.");
            System.err.println("Pode haver um problema com o sistema de som do seu computador.");
            System.err.println("************************************************************");
            e.printStackTrace();
        }
    }

    public void playMusic() {
        if (musicClip != null) {
            System.out.println("AudioManager: A tocar música...");
            musicClip.setFramePosition(0); // Reinicia a música do início
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
        } else {
            System.err.println("AudioManager: Não é possível tocar música porque o clip é nulo.");
        }
    }

    public void stopMusic() {
        if (musicClip != null && musicClip.isRunning()) {
            System.out.println("AudioManager: A parar a música...");
            musicClip.stop();
        }
    }
}
