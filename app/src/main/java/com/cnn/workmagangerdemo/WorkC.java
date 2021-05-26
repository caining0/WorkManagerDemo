package com.cnn.workmagangerdemo;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Created by caining on 2021/5/24 14:28
 * E-Mail Address：cainingning@360.cn
 */
public class WorkC extends Worker {
    public WorkC(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String value = getInputData().getString("key");
        long millis = System.currentTimeMillis();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignore) {
            return Result.failure();
        }
        long end = System.currentTimeMillis();
        Log.i("Work--------> ", "\nC开始时间:" + millis + "----执行时间:" + String.valueOf(end - millis)+ "----结束时间:" +end);
        return Result.success();
    }
}
