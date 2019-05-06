package com.icewind.silestahivesync.dto;

import java.util.List;

import lombok.Data;

/**
 * Represents information about sleep.
 * Currently tracked by Galaxy Watch.
 * Contains several stages of sleep.
 * Each stage contains start and end.
 */
@Data
public class SleepDto extends BaseApiDto {
    List<SleepStageDto> stages;
}
