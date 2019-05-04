package com.icewind.silestahivesync;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@ToString
class MealDetails {
    private float calories = 0f;
    private float protein = 0f;
    private float vitaminC = 0f;
    private float vitaminA = 0f;
    private float fat = 0f;
    private float carbohydate = 0f;
    private float potassium = 0f;
    private float totalFat = 0f;
    private float calcium = 0f;
    private float cholesterol = 0f;
    private float fiber = 0f;
    private float iron = 0f;
    private float monosaturatedFat = 0f;
    private float polysaturatedFat = 0f;
    private float saturatedFat = 0f;
    private float sodium = 0f;
    private float sugar = 0f;
    private float transFat = 0f;

    private String mealType;

    MealDetails(float calories, float protein, float vitaminC, float vitaminA, float fat,
                float carbohydate, float potassium, float totalFat, float calcium, float cholesterol,
                float fiber, float iron, float monosaturatedFat, float polysaturatedFat, float saturatedFat,
                float sodium, float sugar, float transFat, String mealType) {
        this.calcium = calcium;
        this.calories = calories;
        this.protein = protein;
        this.vitaminC = vitaminC;
        this.vitaminA = vitaminA;
        this.fat = fat;
        this.carbohydate = carbohydate;
        this.potassium = potassium;
        this.totalFat = totalFat;
        this.cholesterol = cholesterol;
        this.fiber = fiber;
        this.iron = iron;
        this.monosaturatedFat = monosaturatedFat;
        this.polysaturatedFat = polysaturatedFat;
        this.saturatedFat = saturatedFat;
        this.sodium = sodium;
        this.sugar = sugar;
        this.transFat = transFat;
        this.mealType = mealType;
    }
}
