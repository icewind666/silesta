package com.silesta.runtime;

import com.silesta.util.Command;
import java.util.Properties;

/**
 * Runs given command
 * Created by icewind on 15.04.17.
 */
public class CommandRunner {

    private String cmd;
    private Properties properties;

    public CommandRunner(String cmd, Properties props) {
        this.properties = props;
        this.cmd = cmd;
    }

    public void go() {
        System.out.println(cmd);

        switch(cmd) {
            case Command.BRIEF:
                //BRIEF command
                break;
            case Command.STATS:
                //STATS command
                break;
        }

    }
}
