package com.silesta.extractors;

import com.silesta.interfaces.ExtractorConfiguration;

import java.util.HashMap;

/**
 * Конфигурация для парсера сберовских выписок (HTML из онлайн банка)
 */
public class SberbankExtractorConfig extends ExtractorConfiguration {
    void SberbankExtractorConfig() {

    }

    public SberbankExtractorConfig(HashMap<String, String> properties) {
        super(properties);
    }
}
