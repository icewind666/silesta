package com.icewind.silestahivesync;


import android.os.Handler;
import android.util.Log;

import com.icewind.silestahivesync.dto.DailyExercises;
import com.icewind.silestahivesync.dto.ExerciseDto;
import com.icewind.silestahivesync.dto.MealDetails;
import com.icewind.silestahivesync.dto.SleepDto;
import com.icewind.silestahivesync.dto.SleepStageDto;
import com.icewind.silestahivesync.dto.StepsDto;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthData;
import com.samsung.android.sdk.healthdata.HealthDataResolver;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthResultHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Single;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class DeviceDataHelper {
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
    public void readStepCount(long startTime, OnSamsungHealthResult handler) {
        // Read the today's step count on demand
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);
        HealthDataResolver.Filter filter = HealthDataResolver.Filter.and(
                HealthDataResolver.Filter.eq(HealthConstants.StepCount.SAMPLE_POSITION_TYPE,
                        HealthConstants.StepCount.SAMPLE_POSITION_TYPE_WRIST));
        // Set time range from start time of today to the current time
        long endTime = startTime + ONE_DAY;

        HealthResultHolder.ResultListener<HealthDataResolver.ReadResult> mListener = result -> {
            int count = 0;
            float speed = 0;
            float totalDistance = 0;
            int recordCount = result.getCount();

            try {
                // result here is in form of atomic measurements
                // collecting total result
                for (HealthData data : result) {
                    count += data.getInt(HealthConstants.StepCount.COUNT);
                    totalDistance += data.getFloat(HealthConstants.StepCount.DISTANCE);
                    speed += data.getFloat(HealthConstants.StepCount.SPEED);
                }

                // start and end should set the handler
                StepsDto steps = new StepsDto(count, totalDistance, speed/recordCount,
                        startTime, endTime);
                // passing result to handler
                handler.handle(steps);
            } finally {
                result.close();
            }
        };

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

    // Read the today's step count on demand
    void readSleepStages(long startTime, OnSamsungHealthResult handler) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);
        // Set time range from start time of today to the current time
        long endTime = startTime + ONE_DAY;

        HealthResultHolder.ResultListener<HealthDataResolver.ReadResult> mListener = result -> {
            List<SleepStageDto> stages = new ArrayList<>();
            try {
                // collecting sleep stages
                for (HealthData data : result) {
                    SleepStageDto dto = new SleepStageDto();
                    dto.setStage(data.getString(HealthConstants.SleepStage.STAGE));
                    dto.setStageStart(data.getLong(HealthConstants.SleepStage.START_TIME));
                    dto.setStageStart(data.getLong(HealthConstants.SleepStage.END_TIME));
                    stages.add(dto);
                }

                SleepDto resultDto = new SleepDto();
                resultDto.setStages(stages);
                resultDto.setDayStart(startTime);

                // triggering result handle
                handler.handle(resultDto);
            } finally {
                result.close();
            }
        };

        HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.SleepStage.HEALTH_DATA_TYPE)
                .setProperties(new String[] {HealthConstants.SleepStage.STAGE,
                        HealthConstants.SleepStage.CUSTOM,
                        HealthConstants.SleepStage.SLEEP_ID,
                        HealthConstants.SleepStage.PACKAGE_NAME,
                        HealthConstants.SleepStage.START_TIME
                })
                .setLocalTimeRange(HealthConstants.SleepStage.START_TIME, HealthConstants.SleepStage.TIME_OFFSET,
                        startTime, endTime)
                .build();

        try {
            resolver.read(request).setResultListener(mListener);
        } catch (Exception e) {
            Log.e(MainActivity.TAG, "Getting sleep count fails.", e);
        }
    }


    /**
     * Reads all exercises from given time and + one day
     * @param startTime
     * @param handler
     */
    void readExercises(long startTime, OnSamsungHealthResult handler) {
        HealthDataResolver resolver = new HealthDataResolver(mStore, null);
        long endTime = startTime + ONE_DAY;

        HealthResultHolder.ResultListener<HealthDataResolver.ReadResult> mListener = result -> {
            List<ExerciseDto> stages = new ArrayList<>();
            try {
                for (HealthData data : result) {
                    ExerciseDto dto = new ExerciseDto();
                    dto.setCalorie(data.getFloat(HealthConstants.Exercise.CALORIE));
                    dto.setCount(data.getInt(HealthConstants.Exercise.COUNT));
                    dto.setDistance(data.getFloat(HealthConstants.Exercise.DISTANCE));
                    dto.setDuration(data.getLong(HealthConstants.Exercise.DURATION));
                    dto.setStartTime(data.getLong(HealthConstants.Exercise.START_TIME));
                    dto.setEndTime(data.getLong(HealthConstants.Exercise.END_TIME));

                    switch (data.getInt(HealthConstants.Exercise.EXERCISE_TYPE)) {
                        case 1001:
                            dto.setType("walking");
                            break;
                        case 1002:
                            dto.setType("running");
                            break;
                        case 10005:
                            dto.setType("pull-ups");
                            break;
                        case 10004:
                            dto.setType("push-ups");
                            break;
                        case 10006:
                            dto.setType("sit-ups");
                            break;
                        default:
                            dto.setType("default");
                    }
                    stages.add(dto);
                }
                DailyExercises resultDto = new DailyExercises(stages);
                resultDto.setDayStart(startTime);
                handler.handle(resultDto);
            } finally {
                result.close();
            }
        };

        HealthDataResolver.ReadRequest request = new HealthDataResolver.ReadRequest.Builder()
                .setDataType(HealthConstants.Exercise.HEALTH_DATA_TYPE)
                .setProperties(new String[] {HealthConstants.Exercise.CALORIE,
                        HealthConstants.Exercise.COUNT,
                        HealthConstants.Exercise.DISTANCE,
                        HealthConstants.Exercise.EXERCISE_TYPE,
                        HealthConstants.Exercise.DURATION,
                        HealthConstants.Exercise.START_TIME,
                        HealthConstants.Exercise.END_TIME,
                })
                .setLocalTimeRange(HealthConstants.Exercise.START_TIME, HealthConstants.Exercise.TIME_OFFSET,
                        startTime, endTime)
                .build();

        try {
            resolver.read(request).setResultListener(mListener);
        } catch (Exception e) {
            Log.e(MainActivity.TAG, "Getting exercises fails.", e);
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