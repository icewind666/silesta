package com.silesta;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.silesta.runtime.CommandRunner;
import com.silesta.util.Command;
import com.silesta.voice.SilestaVoice;
import marytts.exceptions.MaryConfigurationException;
import marytts.exceptions.SynthesisException;
import sun.util.logging.PlatformLogger;

public class Main {
    private static Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        log.info("S.I.L.E.S.T.A project is starting up, ...");
        SilestaVoice voice = new SilestaVoice();

        if (args.length >= 1) {
            //user specified a config file
            Properties p = new Properties();

            try {
                p.load(new FileInputStream(args[0]));
                String commandToRun = args[1];
                CommandRunner runner = new CommandRunner(commandToRun, p);
                runner.go();

            } catch (IOException e) {
                e.printStackTrace();
                log.severe("Error: I cant load properties file");
                log.log(Level.SEVERE, "Exception: ", e);
            }

        }
    }
}
