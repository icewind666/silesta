package com.icewind.silestahivesync;


import android.os.Handler;
import android.util.Log;

import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthData;
import com.samsung.android.sdk.healthdata.HealthDataResolver;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthResultHolder;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;

import io.reactivex.Single;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class FoodDataHelper {
    private static final long ONE_DAY = 24 * 60 * 60 * 1000;
    private final HealthDataStore mStore;
    private final Handler mResultProcessingHandler;


    Single<List<MealDetails>> readDailyIntakeDetails(long startTime) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, mResultProcessingHandler);
        HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder().setDataType(HealthConstants.Nutrition.HEALTH_DATA_TYPE)
                .setProperties(new String[] {
                        HealthConstants.Nutrition.UUID,
                        HealthConstants.Nutrition.TITLE,
                        HealthConstants.Nutrition.CALORIE,
                        HealthConstants.Nutrition.PACKAGE_NAME,
                        HealthConstants.Nutrition.PROTEIN,
                        HealthConstants.Nutrition.TOTAL_FAT,
                        HealthConstants.Nutrition.CARBOHYDRATE,
                        HealthConstants.Nutrition.CALCIUM,
                        HealthConstants.Nutrition.CHOLESTEROL,
                        HealthConstants.Nutrition.DIETARY_FIBER,
                        HealthConstants.Nutrition.IRON,
                        HealthConstants.Nutrition.MONOSATURATED_FAT,
                        HealthConstants.Nutrition.POLYSATURATED_FAT,
                        HealthConstants.Nutrition.POTASSIUM,
                        HealthConstants.Nutrition.SATURATED_FAT,
                        HealthConstants.Nutrition.SODIUM,
                        HealthConstants.Nutrition.SUGAR,
                        HealthConstants.Nutrition.TRANS_FAT,
                        HealthConstants.Nutrition.VITAMIN_A,
                        HealthConstants.Nutrition.VITAMIN_C
                    }
                )
                .setLocalTimeRange(HealthConstants.Nutrition.START_TIME, HealthConstants.Nutrition.TIME_OFFSET, startTime, startTime + ONE_DAY)
                .build();

        return operate(() -> resolver.read(request))
                .doAfterSuccess(HealthDataResolver.ReadResult::close)
                .flattenAsObservable(result -> result)
                .map(data -> new MealDetails(
                        data.getFloat(HealthConstants.Nutrition.CALORIE),
                        data.getFloat(HealthConstants.Nutrition.PROTEIN),
                        data.getFloat(HealthConstants.Nutrition.VITAMIN_A),
                        data.getFloat(HealthConstants.Nutrition.VITAMIN_C),
                        data.getFloat(HealthConstants.Nutrition.TOTAL_FAT),
                        data.getFloat(HealthConstants.Nutrition.CARBOHYDRATE),
                        data.getFloat(HealthConstants.Nutrition.POTASSIUM),
                        data.getFloat(HealthConstants.Nutrition.TOTAL_FAT),
                        data.getFloat(HealthConstants.Nutrition.CALCIUM),
                        data.getFloat(HealthConstants.Nutrition.CHOLESTEROL),
                        data.getFloat(HealthConstants.Nutrition.DIETARY_FIBER),
                        data.getFloat(HealthConstants.Nutrition.IRON),
                        data.getFloat(HealthConstants.Nutrition.MONOSATURATED_FAT),
                        data.getFloat(HealthConstants.Nutrition.POLYSATURATED_FAT),
                        data.getFloat(HealthConstants.Nutrition.SATURATED_FAT),
                        data.getFloat(HealthConstants.Nutrition.SODIUM),
                        data.getFloat(HealthConstants.Nutrition.SUGAR),
                        data.getFloat(HealthConstants.Nutrition.TRANS_FAT),
                        data.getString(HealthConstants.Nutrition.TITLE)
                ))
                .toList();
    }


    // Read the today's step count on demand
    protected void readStepCount(long startTime, OnStepResultHandler handler) {
        // Read the today's step count on demand
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);
        HealthDataResolver.Filter filter = HealthDataResolver.Filter.and(
                HealthDataResolver.Filter.eq(HealthConstants.StepCount.SAMPLE_POSITION_TYPE,
                        HealthConstants.StepCount.SAMPLE_POSITION_TYPE_WRIST));

        HealthResultHolder.ResultListener<HealthDataResolver.ReadResult> mListener = result -> {
            int count = 0;
            float speed = 0;
            float totalDistance = 0;
            int recordCount = result.getCount();
            try {
                for (HealthData data : result) {
                    Log.d("TAG", data.getContentValues().toString());
                    count += data.getInt(HealthConstants.StepCount.COUNT);
                    totalDistance += data.getFloat(HealthConstants.StepCount.DISTANCE);
                    speed += data.getFloat(HealthConstants.StepCount.SPEED);
                }

                handler.handle(count, totalDistance, speed/recordCount);
            } finally {
                result.close();
            }
        };
        // Set time range from start time of today to the current time

        long endTime = startTime + ONE_DAY;

        HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.StepCount.HEALTH_DATA_TYPE)
                .setProperties(new String[] {HealthConstants.StepCount.COUNT,
                        HealthConstants.StepCount.DISTANCE,
                        HealthConstants.StepCount.UUID,
                        HealthConstants.StepCount.PACKAGE_NAME,
                        HealthConstants.StepCount.CUSTOM,
                        HealthConstants.StepCount.SAMPLE_POSITION_TYPE,
                        HealthConstants.StepCount.SPEED
                })
                .setFilter(filter)
                .setLocalTimeRange(HealthConstants.StepCount.START_TIME, HealthConstants.StepCount.TIME_OFFSET,
                        startTime, endTime)
                .build();

        try {
            resolver.read(request).setResultListener(mListener);
        } catch (Exception e) {
            Log.e(MainActivity.TAG, "Getting step count fails.", e);
        }
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