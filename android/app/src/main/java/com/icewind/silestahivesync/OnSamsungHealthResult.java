package com.icewind.silestahivesync;

import com.icewind.silestahivesync.dto.SleepStageDto;
import com.samsung.android.sdk.healthdata.HealthConstants;

public interface OnSamsungHealthResult {
    void handle(long steps, float distance, float speed);
    void handleSleep(SleepStageDto[] stages);
}
