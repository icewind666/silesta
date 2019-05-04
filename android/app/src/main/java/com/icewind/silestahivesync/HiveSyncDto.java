package com.icewind.silestahivesync;

import java.util.List;

import lombok.Data;

@Data
public class HiveSyncDto {
    StepsInfo steps;
    List<MealDetails> meals;
    long dayStart;
    boolean status;

}
