package com.silesta.interfaces;

/**
 * Общий интерфейс для всех модулей которые парсят данные
 * из файлов типа html и pdf (то есть из физических ресурсов)
 *
 * Не используется для приема данных.
 *
 */
public interface IExtractor {
    void extract();
    void init(ExtractorConfiguration config);
}
