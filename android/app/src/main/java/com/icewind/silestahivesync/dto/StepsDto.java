package com.icewind.silestahivesync.dto;

import lombok.Data;

@Data
public class StepsDto extends BaseApiDto {
    long startTime;
    long endTime;
    long count;
    float distance;
    float speed;

    public StepsDto(long count, float distance, float speed, long start, long end) {
        this.count = count;
        this.distance = distance;
        this.speed = speed;
        this.startTime = start;
        this.endTime = end;
    }

}
