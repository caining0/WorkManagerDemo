package com.cnn.workmagangerdemo;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Created by caining on 2021/5/24 14:28
 * E-Mail Address：cainingning@360.cn
 */
public class WorkA extends Worker {
    public WorkA(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        long millis = System.currentTimeMillis();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignore) {
            return Result.failure();
        }
        long end = System.currentTimeMillis();
        Log.i("Work--------> ", "\nA开始时间:" + millis + "----执行时间:" + String.valueOf(end - millis)+ "----结束时间:" +end);

        Data output = new Data.Builder()
                .putString("output", "456")
                .build();
        Log.i("Work--------> ", "workA 获取到 input>"+getInputData().getString("input")+";并回传output>456");
        return Result.success(output);
    }
}
