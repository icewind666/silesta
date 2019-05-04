package com.icewind.silestahivesync;

import lombok.Data;

@Data
public class StepsInfo {
    long startTime;
    long endTime;
    long count;
    float distance;
    float speed;

    StepsInfo(long count, float distance, float speed, long start, long end) {
        this.count = count;
        this.distance = distance;
        this.speed = speed;
        this.startTime = start;
        this.endTime = end;
    }


}
