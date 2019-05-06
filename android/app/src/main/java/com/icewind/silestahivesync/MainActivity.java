package com.icewind.silestahivesync;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.icewind.silestahivesync.dto.MealDetails;
import com.icewind.silestahivesync.dto.NutritionDto;
import com.icewind.silestahivesync.dto.StepsDto;
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthConstants.Nutrition;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionKey;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionResult;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity {
    public static final String TAG = "SilestaHiveSync";
    public static final long ONE_DAY = 24 * 60 * 60 * 1000;

    private HealthDataStore mStore;
    private DeviceDataHelper mDataHelper;
    private boolean mIsStoreConnected;
    private Handler mResultProcessingHandler = new Handler();
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private long mDayStartTime;

    private List<MealDetails> meals = new ArrayList<>();

    @BindView(R.id.statusText)
    TextView mStatusTextArea;

    @BindView(R.id.sendBtn)
    Button mSendBtn;

//    private final HealthDataObserver mObserver = new HealthDataObserver(null) {
//        @Override
//        public void onChange(String dataTypeName) {
//            Log.d(TAG, "onChange");
//            if (Nutrition.HEALTH_DATA_TYPE.equals(dataTypeName)) {
//                getDailyHealthInformation();
//            }
//        }
//    };

    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {
        @Override
        public void onConnected() {
            Log.d(TAG, "onConnected");
            mIsStoreConnected = true;
            if (isPermissionAcquired()) {
                gatherDataFromDevice();
            } else {
                requestPermission();
            }
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            Log.d(TAG, "onConnectionFailed");
            showConnectionFailureDialog(error);
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "onDisconnected");
            mIsStoreConnected = false;
        }
    };

    private void showPermissionAlarmDialog() {
        if (isFinishing()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.notice)
                .setMessage(R.string.msg_perm_acquired)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    private void showConnectionFailureDialog(final HealthConnectionErrorResult error) {
        if (isFinishing()) {
            return;
        }
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        if (error.hasResolution()) {
            switch (error.getErrorCode()) {
                case HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED:
                    alert.setMessage(R.string.msg_req_install);
                    break;
                case HealthConnectionErrorResult.OLD_VERSION_PLATFORM:
                    alert.setMessage(R.string.msg_req_upgrade);
                    break;
                case HealthConnectionErrorResult.PLATFORM_DISABLED:
                    alert.setMessage(R.string.msg_req_enable);
                    break;
                case HealthConnectionErrorResult.USER_AGREEMENT_NEEDED:
                    alert.setMessage(R.string.msg_req_agree);
                    break;
                default:
                    alert.setMessage(R.string.msg_req_available);
                    break;
            }
        } else {
            alert.setMessage(R.string.msg_conn_not_available);
        }
        alert.setPositiveButton(R.string.ok, (dialog, id) -> {
            if (error.hasResolution()) {
                error.resolve(MainActivity.this);
            }
        });
        if (error.hasResolution()) {
            alert.setNegativeButton(R.string.cancel, null);
        }
        alert.show();
    }

    private boolean isPermissionAcquired() {
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        try {
            // Check whether the permissions that this application needs are acquired
            Map<PermissionKey, Boolean> resultMap = pmsManager.isPermissionAcquired(generatePermissionKeySet());
            return !resultMap.values().contains(Boolean.FALSE);
        } catch (Exception e) {
            Log.e(TAG, "Permission request fails.", e);
        }
        return false;
    }

    private void requestPermission() {
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        try {
            // Show user permission UI for allowing user to change options
            pmsManager.requestPermissions(generatePermissionKeySet(), MainActivity.this)
                    .setResultListener(this::handlePermissionResult);
        } catch (Exception e) {
            Toast.makeText(this, "Permission setting fails : " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Permission setting fails.", e);
        }
    }

    private Set<PermissionKey> generatePermissionKeySet() {
        Set<PermissionKey> pmsKeySet = new HashSet<>();

        // Add the read and write permissions to Permission KeySet
        pmsKeySet.add(new PermissionKey(Nutrition.HEALTH_DATA_TYPE, PermissionType.READ));
        pmsKeySet.add(new PermissionKey(HealthConstants.StepCount.HEALTH_DATA_TYPE, PermissionType.READ));
        pmsKeySet.add(new PermissionKey("com.samsung.shealth.step_daily_trend", PermissionType.READ));
        pmsKeySet.add(new PermissionKey(HealthConstants.HeartRate.HEALTH_DATA_TYPE, PermissionType.READ));
        pmsKeySet.add(new PermissionKey(HealthConstants.Exercise.HEALTH_DATA_TYPE, PermissionType.READ));
        pmsKeySet.add(new PermissionKey(HealthConstants.CaffeineIntake.HEALTH_DATA_TYPE, PermissionType.READ));
        pmsKeySet.add(new PermissionKey(HealthConstants.WaterIntake.HEALTH_DATA_TYPE, PermissionType.READ));
        pmsKeySet.add(new PermissionKey(HealthConstants.AmbientTemperature.HEALTH_DATA_TYPE, PermissionType.READ));
        return pmsKeySet;
    }

    private void handlePermissionResult(PermissionResult result) {
        Map<PermissionKey, Boolean> resultMap = result.getResultMap();
        if (resultMap.values().contains(Boolean.FALSE)) {
            showPermissionAlarmDialog();
        } else {
            gatherDataFromDevice();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Create a HealthDataStore instance and set its listener
        mStore = new HealthDataStore(this, mConnectionListener);
        mStore.connectService();
        mDataHelper = new DeviceDataHelper(mStore, mResultProcessingHandler);

        // Get the current time and show it
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        mDayStartTime = calendar.getTimeInMillis();

        mSendBtn.setOnClickListener(view -> {
            //sendNutrition();
            //sendSteps();
            //            sendWater();
            //            sendExercises();
            //            sendCoffee();
            //            sendSleep();
        });
    }

    private void sendNutrition(List<MealDetails> d) {
        NutritionDto dto = new NutritionDto();
        //dto.setSteps(steps);
        dto.setMeals(meals);
        dto.setDayStart(mDayStartTime);
        dto.status = false;
        sendNutritionDataToHive(dto);
    }

    private void gatherDataFromDevice() {
        if (!mIsStoreConnected) {
            mStatusTextArea.append("\nCant connect to health data store\n");
            return;
        }
        mCompositeDisposable.add(mDataHelper.readDailyIntakeDetails(mDayStartTime)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MainActivity.this::sendNutrition,
                        MainActivity.this::showTotalCaloriesFailed)
        );

        mDataHelper.readStepCount(mDayStartTime,
                (steps, distance, speed) -> MainActivity.this.sendSteps(new StepsDto(steps,
                        distance,speed, mDayStartTime, mDayStartTime+ONE_DAY)));

        mDataHelper.readSleepStages(mDayStartTime,
                (steps, distance, speed) -> MainActivity.this.sendSteps(new StepsDto(steps,
                        distance,speed, mDayStartTime, mDayStartTime+ONE_DAY)));

    }


    private void showTotalCaloriesFailed(Throwable throwable) {
        Toast.makeText(this, "Failed to read calories : " + throwable.getMessage(),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //HealthDataObserver.removeObserver(mStore, mObserver);
        mCompositeDisposable.clear();
        mStore.disconnectService();
    }

    private void sendSteps(StepsDto dto) {

    }

    private void sendNutritionDataToHive(NutritionDto dto) {
        NetworkService.getInstance()
                .getSilestaApi()
                .sendNutrition(dto)
                .enqueue(new Callback<NutritionDto>() {
                    @Override
                    public void onResponse(@NonNull Call<NutritionDto> call,
                                           @NonNull Response<NutritionDto> response) {
                        mStatusTextArea.append("\nData sent. Response received\n");
                    }

                    @Override
                    public void onFailure(@NonNull Call<NutritionDto> call,
                                          @NonNull Throwable t) {
                        mStatusTextArea.append("\nResponse received: error\n");
                        mStatusTextArea.append("\n");
                        mStatusTextArea.append(t.toString());
                        mStatusTextArea.append("\n");
                        Log.d(TAG, "Response received : FAIL");
                    }
                });
    }

}
