package com.icewind.silestahivesync;

import com.icewind.silestahivesync.dto.BaseApiDto;
import com.icewind.silestahivesync.dto.SleepStageDto;
import com.samsung.android.sdk.healthdata.HealthConstants;

public interface OnSamsungHealthResult {
    void handle(BaseApiDto dtoResult);
//    void handleSleep(SleepStageDto[] stages);
//    void handleExercise();
}
