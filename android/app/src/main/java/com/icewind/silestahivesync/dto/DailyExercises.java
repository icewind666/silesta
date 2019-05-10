package com.icewind.silestahivesync.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class DailyExercises extends BaseApiDto {
    List<ExerciseDto> exerciseDtos;

    public DailyExercises() {

    }

    public DailyExercises(List<ExerciseDto> dtos) {
        exerciseDtos = dtos;
    }
}
