

# Jetpack之WorkManager 的链式调度

@author 竹叶青

## 简介

- WorkManager 主要的能力: 可供轻松调度那些即使在**退出应用**或**重启设备**后仍应运行的[**可延期** - **异步**任务]
- 本篇主要介绍**异步**的链式调度，至于**可延期**定时任务的功能，我们后面再做分享

- WorkManager 根据以下条件使用底层作业来调度服务:

  如果设备在 API 级别 `23` 或更高级别上运行，系统会使用 `JobScheduler`。在 API 级别 `14-22` 上，系统会使用 `GcmNetworkManager`（如果可用），否则会使用自定义 `AlarmManager` 和 `BroadcastReciever` 实现作为备用

<img src="https://developer.android.com/images/topic/libraries/architecture/workmanager/overview-criteria.png?hl=zh-cn" alt="a" style="zoom: 50%;" />

## 使用入门

- gradle 引入

```groovy
dependencies {
  def work_version = "2.5.0"
    // (Java only)
    implementation "androidx.work:work-runtime:$work_version"
    // Kotlin + coroutines
    implementation "androidx.work:work-runtime-ktx:$work_version"
    // optional - RxJava2 support
    implementation "androidx.work:work-rxjava2:$work_version"
    // optional - GCMNetworkManager support
    implementation "androidx.work:work-gcm:$work_version"
    // optional - Test helpers
    androidTestImplementation "androidx.work:work-testing:$work_version"
    // optional - Multiprocess support
    implementation "androidx.work:work-multiprocess:$work_version"
  }
```

- 如需创建工作链，可以使用 [`WorkManager.beginWith(OneTimeWorkRequest)`](https://developer.android.com/reference/androidx/work/WorkManager?hl=zh-cn#beginWith(androidx.work.OneTimeWorkRequest)) 或 [`WorkManager.beginWith(List)`](https://developer.android.com/reference/androidx/work/WorkManager?hl=zh-cn#beginWith(java.util.List))，这会返回 [`WorkContinuation`](https://developer.android.com/reference/androidx/work/WorkContinuation?hl=zh-cn) 实例

- 然后，可以使用 `WorkContinuation` 通过 [`then(OneTimeWorkRequest)`](https://developer.android.com/reference/androidx/work/WorkContinuation?hl=zh-cn#then(androidx.work.OneTimeWorkRequest)) 或 [`then(List)`](https://developer.android.com/reference/androidx/work/WorkContinuation?hl=zh-cn#then(java.util.List)) 添加 `OneTimeWorkRequest` 依赖实例

- 每次调用 `WorkContinuation.then(...)` 都会返回一个新的 `WorkContinuation` 实例。如果添加了 `OneTimeWorkRequest` 实例的 `List`，这些请求会并行运行

- 最后，可以使用 [`WorkContinuation.enqueue()`](https://developer.android.com/reference/androidx/work/WorkContinuation?hl=zh-cn#enqueue()) 方法对 `WorkContinuation` 工作链执行 `enqueue()` 操作

- [`WorkManager.enqueueUniqueWork()`](https://developer.android.com/reference/androidx/work/WorkManager?hl=zh-cn#enqueueUniqueWork(java.lang.String, androidx.work.ExistingWorkPolicy, androidx.work.OneTimeWorkRequest))（用于一次性工作）

- [`WorkManager.enqueueUniquePeriodicWork()`](https://developer.android.com/reference/androidx/work/WorkManager?hl=zh-cn#enqueueUniquePeriodicWork(java.lang.String, androidx.work.ExistingPeriodicWorkPolicy, androidx.work.PeriodicWorkRequest))（用于定期工作）

  


## 链式调度示例

- 首先，Work 关键代码如下：

```java
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Created by caining on 2021/5/24 14:28
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
        return Result.success();
    }
}

```

- Work 工作的状态&&dowork return

  <img src="https://i0.hdslb.com/bfs/album/889066a766e74cd638159dbfe608c2ce7fabd3a0.png" alt="image-20210525102439894" style="zoom:50%;" />

  - `Result.success()`：工作成功完成。
  - `Result.failure()`：工作失败。
  - `Result.retry()`：工作失败，应根据其[重试政策](https://developer.android.com/topic/libraries/architecture/workmanager/how-to/define-work?hl=zh-cn#retries_backoff)在其他时间尝试。
  - 可以用[`WorkInfo.State.isFinished()`](https://developer.android.com/reference/androidx/work/WorkInfo.State?hl=zh-cn#isFinished()) 判断完成状态

- 创建OneTimeWorkRequest

```java
OneTimeWorkRequest requestWorkA = new OneTimeWorkRequest.Builder(WorkA.class).build();
OneTimeWorkRequest requestWorkB = new OneTimeWorkRequest.Builder(WorkB.class).build();
OneTimeWorkRequest requestWorkC = new OneTimeWorkRequest.Builder(WorkC.class).build();
OneTimeWorkRequest requestWorkD = new OneTimeWorkRequest.Builder(WorkD.class).build();
OneTimeWorkRequest requestWorkE = new OneTimeWorkRequest.Builder(WorkE.class).build();
```

- 链式调度 example1：

```java
  /**
         A       B       C
         |       |       | 
         +-------+-------+
                 |
                 D
     */
WorkManager.getInstance(MainActivity.this)
        .beginWith(Arrays.asList(requestWorkA, requestWorkB, requestWorkC))
        .then(requestWorkD)
        .enqueue();

/**
	example1：执行结果如下
  I/Work-------->: A开始时间:1621843840447----执行时间:1000----结束时间:1621843841447
  I/Work-------->: B开始时间:1621843840448----执行时间:2001----结束时间:1621843842449
  I/Work-------->: C开始时间:1621843840456----执行时间:3000----结束时间:1621843843456
  I/Work-------->: D开始时间:1621843843539----执行时间:1000----结束时间:1621843844539
*/
```

从时间结果可以看出 执行顺序是 abc 同时开始，执行结束后执行D 任务，流程图如下

<img src="https://i0.hdslb.com/bfs/album/88f59c7071349340196608baabe3b79d44a103a5.png" alt="image-20210524161909979" style="zoom:50%;" />

- 链式调度 example2，利用WorkContinuation.combine 合并任务，还可以这样：

```java
    /**
         A       B   D
         |       |   |
         +-------+   |
             |       |
             C       |
             |       |
             +-------+
                 |
                 E
      `WorkContinuation.combine` 合并混合两个任务
     */

WorkManager workManager = WorkManager.getInstance(MainActivity.this);
WorkContinuation firstABC = workManager.beginWith(Arrays.asList(requestWorkA,requestWorkB)).then(requestWorkC);
WorkContinuation dWork = workManager.beginWith(requestWorkD);
WorkContinuation.combine(Arrays.asList(firstABC, dWork)).then(requestWorkE).enqueue();
```

```bash
I/Work-------->: A开始时间:1621848348002----执行时间:1000----结束时间:1621848349002
I/Work-------->: D开始时间:1621848348003----执行时间:1000----结束时间:1621848349003
I/Work-------->: B开始时间:1621848348003----执行时间:2000----结束时间:1621848350003
I/Work-------->: C开始时间:1621848350132----执行时间:3000----结束时间:1621848353132
I/Work-------->: E开始时间:1621848353314----执行时间:1000----结束时间:1621848354314
```

<img src="https://i0.hdslb.com/bfs/album/45b46f820f378725b79a4b52414e2fb6594317f3.png" alt="image-20210524172851974" style="zoom:50%;" />

- 链式调度 example3：

```java
/**
         A       C
         |       |
         B       D
         |       |
         +-------+
             |
             E
     */
WorkManager workManager = WorkManager.getInstance(MainActivity.this);
WorkContinuation left = workManager.beginWith(requestWorkA).then(requestWorkB);
WorkContinuation right = workManager.beginWith(requestWorkC).then(requestWorkD);
WorkContinuation finalWork = WorkContinuation.combine(Arrays.asList(left, right)).then(requestWorkE);
finalWork.enqueue();
/**
    I/Work-------->: A开始时间:1621934646458----执行时间:1000----结束时间:1621934647458
    I/Work-------->: C开始时间:1621934646459----执行时间:3000----结束时间:1621934649459
    I/Work-------->: B开始时间:1621934647541----执行时间:2001----结束时间:1621934649542
    I/Work-------->: D开始时间:1621934649539----执行时间:1000----结束时间:1621934650539
    I/Work-------->: E开始时间:1621934650703----执行时间:1001----结束时间:1621934651704
*/
```

<img src="https://i0.hdslb.com/bfs/album/c455e23679e7e46c3e21d5d5aa191a1ed072ac90.png" alt="image-20210525171116083" style="zoom:50%;" />

## 数据传递

- 数据传递流程如下

<img src="https://i0.hdslb.com/bfs/album/0aa30232d1eaa3beaba96c733b0234c5588ecf37.png" alt="image-20210526104616469" style="zoom:50%;" />

```java
//step 1 
Data myData = new Data.Builder()
                .putString("input", "123")
                .build();
```

```java
//step 2
new OneTimeWorkRequest.Builder(AWorker.class).setInputData(myData).build();
```

```java
 //step 3 >AWorker
@NonNull
    @Override
    public Result doWork() {
        String input = getInputData().getString("input");
    		Data output = new Data.Builder()
                .putString("output", "456")
                .build();
        return Result.success(output);
    }
```

```java
//step4
manager.getWorkInfoByIdLiveData(requestWorkA.getId()).observe(activity, new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo != null && workInfo.getState().isFinished()) {
                    try {
                        output = workInfo.getOutputData().getString("output");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    MLogger.i("outPut--->" + output);
                }

            }
        });
```

## input合并器

WorkManager 提供两种不同类型的 `InputMerger`：

- [`OverwritingInputMerger`](https://developer.android.com/reference/androidx/work/OverwritingInputMerger?hl=zh-cn) 会尝试将所有输入中的所有键添加到输出中。如果发生冲突，它会覆盖先前设置的键。
- [`ArrayCreatingInputMerger`](https://developer.android.com/reference/androidx/work/ArrayCreatingInputMerger?hl=zh-cn) 会尝试合并输入，并在必要时创建数组。

## 引用

- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager/)
- [Reference guide](https://developer.android.com/reference/androidx/work/package-summary)