package com.icewind.silestahivesync;

import com.icewind.silestahivesync.dto.CoffeeDto;
import com.icewind.silestahivesync.dto.ExerciseDto;
import com.icewind.silestahivesync.dto.NutritionDto;
import com.icewind.silestahivesync.dto.PulseDto;
import com.icewind.silestahivesync.dto.StepsDto;
import com.icewind.silestahivesync.dto.WaterDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ISilestaApi {
    @POST("/hiveapp/nutrition")
    Call<NutritionDto> sendNutrition(@Body NutritionDto data);

    @POST("/hiveapp/steps")
    Call<StepsDto> sendSteps(@Body StepsDto data);

    @POST("/hiveapp/exercises")
    Call<ExerciseDto> sendExercises(@Body ExerciseDto data);

    @POST("/hiveapp/pulse")
    Call<PulseDto> sendPulse(@Body PulseDto data);

    @POST("/hiveapp/water")
    Call<WaterDto> sendWater(@Body WaterDto data);

    @POST("/hiveapp/coffee")
    Call<CoffeeDto> sendCoffee(@Body CoffeeDto data);
}
