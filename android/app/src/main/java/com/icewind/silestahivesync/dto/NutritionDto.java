package com.icewind.silestahivesync.dto;

import java.util.List;
import lombok.Data;

@Data
public class NutritionDto extends BaseApiDto {
    List<MealDetails> meals;
}
