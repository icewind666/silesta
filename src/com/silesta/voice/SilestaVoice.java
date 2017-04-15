package com.silesta.voice;

import com.silesta.Main;
import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import marytts.util.data.audio.AudioPlayer;

import javax.sound.sampled.AudioInputStream;
import java.util.Set;
import java.util.logging.Logger;

public class SilestaVoice {
    private static Logger log = Logger.getLogger(Main.class.getName());
    MaryInterface voiceInterface;

    public SilestaVoice() {
        try {
            voiceInterface = new LocalMaryInterface();
            Set<String> voices = voiceInterface.getAvailableVoices();
            voiceInterface.setVoice(voices.iterator().next());
        } catch (MaryConfigurationException e) {
            e.printStackTrace();
        }

    }

    public void say(String what) {
        AudioInputStream audio = null;
        try {
            audio = voiceInterface.generateAudio("SILESTA is starting up, master");
            AudioPlayer player = new AudioPlayer(audio);
            player.start();
            player.join();
        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}