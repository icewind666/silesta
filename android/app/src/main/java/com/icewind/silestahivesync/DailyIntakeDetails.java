package com.icewind.silestahivesync;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class DailyIntakeDetails {
    private float calories = 0f;

    private final List<String> uuidList = new ArrayList<>();
    private final List<String> foodNameList = new ArrayList<>();


    public void addCalories(float calories) {
        this.calories += calories;
    }
}
