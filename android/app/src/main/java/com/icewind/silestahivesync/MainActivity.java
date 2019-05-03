package com.icewind.silestahivesync;

import android.app.AlertDialog;
import android.os.Handler;
import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
//import com.samsung.android.app.foodnote.data.DailyIntakeCalories;
import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthConstants.Nutrition;
import com.samsung.android.sdk.healthdata.HealthDataObserver;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionKey;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionResult;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionType;
import com.samsung.android.sdk.healthdata.HealthResultHolder;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "SilestaHiveSync";
    public static final long ONE_DAY = 24 * 60 * 60 * 1000;

    private HealthDataStore mStore;
    private FoodDataHelper mDataHelper;
    private boolean mIsStoreConnected;
    private Handler mResultProcessingHandler = new Handler();
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private long mDayStartTime;



    private final HealthDataObserver mObserver = new HealthDataObserver(null) {
        @Override
        public void onChange(String dataTypeName) {
            Log.d(TAG, "onChange");
            if (Nutrition.HEALTH_DATA_TYPE.equals(dataTypeName)) {
                refreshDailyCalories();
            }
        }
    };

    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {
        @Override
        public void onConnected() {
            Log.d(TAG, "onConnected");
            mIsStoreConnected = true;
            if (isPermissionAcquired()) {
                refreshDailyCalories();
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
        pmsKeySet.add(new PermissionKey(HealthConstants.HeartRate.HEALTH_DATA_TYPE, PermissionType.READ));
        pmsKeySet.add(new PermissionKey(HealthConstants.Exercise.HEALTH_DATA_TYPE, PermissionType.READ));
        pmsKeySet.add(new PermissionKey(HealthConstants.AmbientTemperature.HEALTH_DATA_TYPE, PermissionType.READ));


        //pmsKeySet.add(new PermissionKey(Nutrition.HEALTH_DATA_TYPE, PermissionType.WRITE));
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
            refreshDailyCalories();
            // Register an observer to listen changes of the calories
            HealthDataObserver.addObserver(mStore, Nutrition.HEALTH_DATA_TYPE, mObserver);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create a HealthDataStore instance and set its listener
        mStore = new HealthDataStore(this, mConnectionListener);
        // Request the connection to the health data store
        mStore.connectService();

        mDataHelper = new FoodDataHelper(mStore, mResultProcessingHandler);
        // Get the current time and show it
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        mDayStartTime = calendar.getTimeInMillis();
        refreshDailyCalories();
    }

    private void refreshDailyCalories() {
        if (!mIsStoreConnected) {
            return;
        }

        mCompositeDisposable.add(mDataHelper.readDailyIntakeCalories(mDayStartTime)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MainActivity.this::showTotalCalories,
                        MainActivity.this::showTotalCaloriesFailed));
    }

    @UiThread
    private void showTotalCalories(DailyIntakeCalories calories) {
        float total = calories.getBreakfast() + calories.getLunch() + calories.getDinner()
                + calories.getMorningSnack() + calories.getAfternoonSnack() + calories.getEveningSnack();
        showTotalCalories(String.valueOf(calories.getBreakfast()),
                String.valueOf(calories.getLunch()),
                String.valueOf(calories.getDinner()),
                String.valueOf(calories.getMorningSnack()),
                String.valueOf(calories.getAfternoonSnack()),
                String.valueOf(calories.getEveningSnack()),
                String.valueOf(total));
    }

    @UiThread
    private void showTotalCaloriesFailed(Throwable throwable) {
//        showTotalCalories("", "", "", "", "", "", "");
        Toast.makeText(this, "Failed to read calories : " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
    }

    @UiThread
    private void showTotalCalories(String breakfast, String lunch, String dinner, String morningSnack,
                                   String afternoonSnack, String eveningSnack, String total) {

/*        Objects.requireNonNull(mBinding.breakfast).calorieView.setText(breakfast);
        Objects.requireNonNull(mBinding.lunch).calorieView.setText(lunch);
        Objects.requireNonNull(mBinding.dinner).calorieView.setText(dinner);
        Objects.requireNonNull(mBinding.morningSnack).calorieView.setText(morningSnack);
        Objects.requireNonNull(mBinding.afternoonSnack).calorieView.setText(afternoonSnack);
        Objects.requireNonNull(mBinding.eveningSnack).calorieView.setText(eveningSnack);
        mBinding.totalCalorie.setText(total);*/
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        HealthDataObserver.removeObserver(mStore, mObserver);

        mCompositeDisposable.clear();
        mStore.disconnectService();
    }

}
