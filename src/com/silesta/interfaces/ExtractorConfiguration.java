package com.silesta.interfaces;

import org.jooq.DSLContext;

import java.util.HashMap;

public abstract class ExtractorConfiguration {
    protected ExtractorConfiguration(HashMap<String, String> properties) {
        this.properties = properties;
    }

    public DSLContext getCTX() {
        return CTX;
    }

    public void setCTX(DSLContext CTX) {
        this.CTX = CTX;
    }

    public String getProperty(String name) {
        if (properties.containsKey(name)) {
            return properties.get(name);
        }
        return null;
    }

    /**
     * jooq db context
     */
    private DSLContext CTX;

    private HashMap<String, String> properties;

}
