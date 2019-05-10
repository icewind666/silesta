package com.icewind.silestahivesync.dto;

import lombok.Data;

@Data
public class ExerciseDto extends BaseApiDto {
    double calorie;
    double count;
    String type;
    double distance;
    long duration;
    long startTime;
    long endTime;
}
