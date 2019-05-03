package com.icewind.silestahivesync;

import com.samsung.android.sdk.healthdata.HealthConstants;

import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class DailyIntakeCalories {
    final float breakfast;
    final float lunch;
    final float dinner;
    final float morningSnack;
    final float afternoonSnack;
    final float eveningSnack;

    public static DailyIntakeCalories fromMap(Map<Integer, Float> calorieMap) {
        return new DailyIntakeCalories(calorieMap.getOrDefault(HealthConstants.Nutrition.MEAL_TYPE_BREAKFAST, 0.f),
                calorieMap.getOrDefault(HealthConstants.Nutrition.MEAL_TYPE_LUNCH, 0.f),
                calorieMap.getOrDefault(HealthConstants.Nutrition.MEAL_TYPE_DINNER, 0.f),
                calorieMap.getOrDefault(HealthConstants.Nutrition.MEAL_TYPE_MORNING_SNACK, 0.f),
                calorieMap.getOrDefault(HealthConstants.Nutrition.MEAL_TYPE_AFTERNOON_SNACK, 0.f),
                calorieMap.getOrDefault(HealthConstants.Nutrition.MEAL_TYPE_EVENING_SNACK, 0.f));
    }

}
