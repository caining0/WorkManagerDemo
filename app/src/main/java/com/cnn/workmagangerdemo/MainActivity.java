package com.cnn.workmagangerdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.work.ArrayCreatingInputMerger;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    @SuppressLint("EnqueueWork")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Data myData = new Data.Builder()
                .putString("input", "123")
                .build();

        OneTimeWorkRequest requestWorkA = new OneTimeWorkRequest.Builder(WorkA.class).setInputData(myData).build();
        OneTimeWorkRequest requestWorkB = new OneTimeWorkRequest.Builder(WorkB.class).build();
        OneTimeWorkRequest requestWorkC = new OneTimeWorkRequest.Builder(WorkC.class).build();
        OneTimeWorkRequest requestWorkD = new OneTimeWorkRequest.Builder(WorkD.class).build();
        OneTimeWorkRequest requestWorkE = new OneTimeWorkRequest.Builder(WorkE.class).build();

        actionOne(requestWorkA, requestWorkB, requestWorkC, requestWorkD);
//        actionTwo(requestWorkA, requestWorkB, requestWorkC, requestWorkD, requestWorkE);
//        actionThree(requestWorkA, requestWorkB, requestWorkC, requestWorkD, requestWorkE);
    }

    /**
     * A       C
     * |       |
     * B       D
     * |       |
     * +-------+
     * |
     * E
     */
    @SuppressLint("EnqueueWork")
    private void actionThree(OneTimeWorkRequest requestWorkA, OneTimeWorkRequest requestWorkB, OneTimeWorkRequest requestWorkC, OneTimeWorkRequest requestWorkD, OneTimeWorkRequest requestWorkE) {
        WorkManager workManager = WorkManager.getInstance(MainActivity.this);
        WorkContinuation left = workManager.beginWith(requestWorkA).then(requestWorkB);
        WorkContinuation right = workManager.beginWith(requestWorkC).then(requestWorkD);
        WorkContinuation finalWork = WorkContinuation.combine(Arrays.asList(left, right)).then(requestWorkE);
        finalWork.enqueue();
    }

    /**
     * A       B   D
     * |       |   |
     * +-------+   |
     * |       |
     * C       |
     * |       |
     * +-------+
     * |
     * E
     */

    @SuppressLint("EnqueueWork")
    private void actionTwo(OneTimeWorkRequest requestWorkA, OneTimeWorkRequest requestWorkB, OneTimeWorkRequest requestWorkC, OneTimeWorkRequest requestWorkD, OneTimeWorkRequest requestWorkE) {
        WorkManager workManager = WorkManager.getInstance(MainActivity.this);
        WorkContinuation firstABC = workManager.beginWith(Arrays.asList(requestWorkA, requestWorkB)).then(requestWorkC);
        WorkContinuation dWork = workManager.beginWith(requestWorkD);
        WorkContinuation.combine(Arrays.asList(firstABC, dWork))
                .then(requestWorkE)
                .enqueue();
    }

    /**
     * /**
     * A       B       C
     * |       |       |
     * +-------+-------+
     * |
     * D
     */

    private void actionOne(OneTimeWorkRequest requestWorkA, OneTimeWorkRequest requestWorkB, OneTimeWorkRequest requestWorkC, OneTimeWorkRequest requestWorkD) {
        WorkManager manager = WorkManager.getInstance(MainActivity.this);
        manager.beginWith(Arrays.asList(requestWorkA, requestWorkB, requestWorkC))
                .then(requestWorkD)
                .enqueue();


        //step3
        manager.getWorkInfoByIdLiveData(requestWorkA.getId()).observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo != null && workInfo.getState().isFinished()) {
                    try {
                        Log.i("Work--------> ","监听到workA 回传了output"+ workInfo.getOutputData().getString("output"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

            }
        });
    }


}