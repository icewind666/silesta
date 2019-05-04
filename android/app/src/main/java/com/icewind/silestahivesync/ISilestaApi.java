package com.icewind.silestahivesync;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ISilestaApi {
    @POST("/hiveapp")
    Call<HiveSyncDto> sendDeviceData(@Body HiveSyncDto data);
}
