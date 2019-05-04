package com.icewind.silestahivesync;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthConstants.Nutrition;
import com.samsung.android.sdk.healthdata.HealthDataObserver;
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
    private FoodDataHelper mDataHelper;
    private boolean mIsStoreConnected;
    private Handler mResultProcessingHandler = new Handler();
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private long mDayStartTime;

    private List<MealDetails> meals = new ArrayList<>();
    private StepsInfo steps;

    @BindView(R.id.statusText)
    TextView mStatusTextArea;

    @BindView(R.id.sendBtn)
    Button mSendBtn;

    private final HealthDataObserver mObserver = new HealthDataObserver(null) {
        @Override
        public void onChange(String dataTypeName) {
            Log.d(TAG, "onChange");
            if (Nutrition.HEALTH_DATA_TYPE.equals(dataTypeName)) {
                getDailyHealthInformation();
            }
        }
    };

    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {
        @Override
        public void onConnected() {
            Log.d(TAG, "onConnected");
            mIsStoreConnected = true;
            if (isPermissionAcquired()) {
                getDailyHealthInformation();
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
        pmsKeySet.add(new PermissionKey(HealthConstants.WaterIntake.HEALTH_DATA_TYPE, PermissionType.READ));
        pmsKeySet.add(new PermissionKey(HealthConstants.AmbientTemperature.HEALTH_DATA_TYPE, PermissionType.READ));
        return pmsKeySet;
    }

    private void handlePermissionResult(PermissionResult result) {
        Map<PermissionKey, Boolean> resultMap = result.getResultMap();
        // Show a permission alarm and initializes the calories if
        // permissions are not acquired
        if (resultMap.values().contains(Boolean.FALSE)) {
            showPermissionAlarmDialog();
        } else {
            // Get the calories of Indexed time and display it
            getDailyHealthInformation();
            // Register an observer to listen changes of the calories
//            HealthDataObserver.addObserver(mStore, Nutrition.HEALTH_DATA_TYPE, mObserver);
//            HealthDataObserver.addObserver(mStore, HealthConstants.StepCount.HEALTH_DATA_TYPE, mObserver);
//            HealthDataObserver.addObserver(mStore, HealthConstants.Exercise.HEALTH_DATA_TYPE, mObserver);
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
        mDataHelper = new FoodDataHelper(mStore, mResultProcessingHandler);

        // Get the current time and show it
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Moscow"));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        mDayStartTime = calendar.getTimeInMillis();

        getDailyHealthInformation();

        mSendBtn.setOnClickListener(view -> {
            HiveSyncDto dto = new HiveSyncDto();
            dto.setSteps(steps);
            dto.setMeals(meals);
            dto.setDayStart(mDayStartTime);
            dto.status = false;

            sendDataToHive(dto);
        });
    }

    private void getDailyHealthInformation() {
        if (!mIsStoreConnected) {
            mStatusTextArea.append("\nCant connect to health data store\n");
            return;
        }
        mCompositeDisposable.add(mDataHelper.readDailyIntakeDetails(mDayStartTime)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MainActivity.this::saveMealDetails,
                        MainActivity.this::showTotalCaloriesFailed)
        );

        mDataHelper.readStepCount(mDayStartTime,
                (steps, distance, speed) -> MainActivity.this.saveSteps(new StepsInfo(steps,
                        distance,speed, mDayStartTime, mDayStartTime+ONE_DAY)));
    }

    private void saveMealDetails(List<MealDetails> d) {
        meals.addAll(d);
        mStatusTextArea.append("Nutrition data received\n");
    }

    private void saveSteps(StepsInfo info) {
        Log.d(TAG, info.toString());
        steps = info;
        mStatusTextArea.append("Steps data received\n");
    }

    private void showTotalCaloriesFailed(Throwable throwable) {
        Toast.makeText(this, "Failed to read calories : " + throwable.getMessage(),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        HealthDataObserver.removeObserver(mStore, mObserver);
        mCompositeDisposable.clear();
        mStore.disconnectService();
    }


    private void sendDataToHive(HiveSyncDto dto) {
        NetworkService.getInstance()
                .getSilestaApi()
                .sendDeviceData(dto)
                .enqueue(new Callback<HiveSyncDto>() {
                    @Override
                    public void onResponse(@NonNull Call<HiveSyncDto> call,
                                           @NonNull Response<HiveSyncDto> response) {
                        mStatusTextArea.append("\nData sent. Response received\n");
                    }

                    @Override
                    public void onFailure(@NonNull Call<HiveSyncDto> call,
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
