package com.icewind.silestahivesync;


import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthData;
import com.samsung.android.sdk.healthdata.HealthDataResolver;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthDeviceManager;
import com.samsung.android.sdk.healthdata.HealthResultHolder;

import java.util.TimeZone;
import java.util.concurrent.Callable;

import io.reactivex.Single;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FoodDataHelper {

    private static final String TAG = MainActivity.TAG;

    private static final long ONE_DAY = 24 * 60 * 60 * 1000;
    private static final String ALIAS_SUM_OF_CALORIE = "alias_sum_of_calorie";
    private static final String ALIAS_GROUP_OF_MEAL_TYPE = "alias_group_of_meal_type";

    private final HealthDataStore mStore;
    private final Handler mResultProcessingHandler;

    // Return the result of inserting successfully
//    public Single<Boolean> insertNutrition(@NonNull String foodName, float intakeCount, int mealType, long intakeTime) {
//
//        Log.i(TAG, "insert nutrition async, title: " + foodName);
//
//        HealthDataResolver resolver = new HealthDataResolver(mStore, mResultProcessingHandler);
//        HealthDataResolver.InsertRequest request = new HealthDataResolver.InsertRequest.Builder().setDataType(HealthConstants.Nutrition.HEALTH_DATA_TYPE).build();
//        HealthData nutritionData = getNutritionData(foodName, intakeCount, mealType, intakeTime);
//        request.addHealthData(nutritionData);
//
//        return operate(() -> resolver.insert(request))
//                .map(HealthResultHolder.BaseResult::getStatus)
//                .map(status -> status == HealthResultHolder.BaseResult.STATUS_SUCCESSFUL)
//                .doOnError(throwable -> Log.e(TAG, "Failed to insert nutrition", throwable))
//                .onErrorReturnItem(false);
//    }
//
//    public Single<Boolean> deleteNutrition(@NonNull String uuid) {
//
//        HealthDataResolver resolver = new HealthDataResolver(mStore, mResultProcessingHandler);
//        HealthDataResolver.Filter filter = HealthDataResolver.Filter.eq(HealthConstants.Nutrition.UUID, uuid);
//        HealthDataResolver.DeleteRequest deleteRequest = new HealthDataResolver.DeleteRequest.Builder()
//                .setDataType(HealthConstants.Nutrition.HEALTH_DATA_TYPE)
//                .setFilter(filter)
//                .build();
//
//        return operate(() -> resolver.delete(deleteRequest))
//                .map(baseResult -> baseResult.getStatus() == HealthResultHolder.BaseResult.STATUS_SUCCESSFUL && baseResult.getCount() >= 1)
//                .doOnError(throwable -> Log.e(TAG, "Failed to delete nutrition", throwable))
//                .onErrorReturnItem(false);
//    }

    public Single<DailyIntakeCalories> readDailyIntakeCalories(long startTime) {

        HealthDataResolver resolver = new HealthDataResolver(mStore, mResultProcessingHandler);

        HealthDataResolver.AggregateRequest request = new HealthDataResolver.AggregateRequest.Builder()
                .setDataType(HealthConstants.Nutrition.HEALTH_DATA_TYPE)
                .addFunction(HealthDataResolver.AggregateRequest.AggregateFunction.SUM, HealthConstants.Nutrition.CALORIE, ALIAS_SUM_OF_CALORIE)
                .setLocalTimeRange(HealthConstants.Nutrition.START_TIME, HealthConstants.Nutrition.TIME_OFFSET, startTime, startTime + ONE_DAY)
                .addGroup(HealthConstants.Nutrition.MEAL_TYPE, ALIAS_GROUP_OF_MEAL_TYPE)
                .build();
        return operate(() -> resolver.aggregate(request))
                .doAfterSuccess(HealthDataResolver.AggregateResult::close)
                .flattenAsObservable(result -> result)
                .toMap(data -> data.getInt(ALIAS_GROUP_OF_MEAL_TYPE),
                        data -> data.getFloat(ALIAS_SUM_OF_CALORIE))
                .map(DailyIntakeCalories::fromMap);

    }


    public Single<DailyIntakeDetails> readDailyIntakeDetails(long startTime, int mealType) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, mResultProcessingHandler);

        HealthDataResolver.Filter filter = HealthDataResolver.Filter.eq(HealthConstants.Nutrition.MEAL_TYPE, mealType);
        // Read the foodIntake data of specified day and meal type(startTime to end)
        HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder().setDataType(HealthConstants.Nutrition.HEALTH_DATA_TYPE)
                .setProperties(new String[]{
                        HealthConstants.Nutrition.UUID, HealthConstants.Nutrition.TITLE, HealthConstants.Nutrition.CALORIE, HealthConstants.Nutrition.PACKAGE_NAME})
                .setLocalTimeRange(HealthConstants.Nutrition.START_TIME, HealthConstants.Nutrition.TIME_OFFSET, startTime, startTime + ONE_DAY)
                .setFilter(filter)
                .build();

        return operate(() -> resolver.read(request))
                .doAfterSuccess(HealthDataResolver.ReadResult::close)
                .flattenAsObservable(result -> result)
                .reduce(new DailyIntakeDetails(), (dailyIntakeDetails, data) -> {
                    String uuid = data.getString(HealthConstants.Nutrition.UUID);
                    float calories = data.getFloat(HealthConstants.Nutrition.CALORIE);
                    String title = data.getString(HealthConstants.Nutrition.TITLE);
                    String packageName = data.getString(HealthConstants.Nutrition.PACKAGE_NAME);

                    dailyIntakeDetails.addCalories(calories);
                    dailyIntakeDetails.getUuidList().add(uuid);
                    dailyIntakeDetails.getFoodNameList().add(title + " : (" + calories + " Cals"
                            + (BuildConfig.APPLICATION_ID.equals(packageName) ? ")" : ", Not Deletable)"));

                    return dailyIntakeDetails;
                });

    }

    private static <T extends HealthResultHolder.BaseResult> Single<T> operate(Callable<HealthResultHolder<T>> operation) {
        return Single.create(emitter -> {
            try {
                operation.call().setResultListener(emitter::onSuccess);
            } catch (Exception e) {
                emitter.onError(e);
            }
        });
    }
}