package com.rockspin.bargainbits.watch_list.job;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.annotation.NonNull;
import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.rockspin.apputils.di.annotations.ApplicationScope;
import com.rockspin.bargainbits.data.models.GameInfo;
import com.rockspin.bargainbits.data.models.currency.CurrencyHelper;
import com.rockspin.bargainbits.data.repository.CurrencyRepository;
import com.rockspin.bargainbits.data.repository.WatchListRepository;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.inject.Inject;
import rx.Observable;
import timber.log.Timber;

public class WatchListCheckJob extends Job {

    public static final String TAG = "WatchListCheckTag";

    private static final int NOTIFICATION_ID = 1;

    @Inject @ApplicationScope Context context;
    @Inject WatchListRepository watchList;
    @Inject CurrencyRepository currencyRepository;

    @Inject public WatchListCheckJob() { /* not used */ }

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        Timber.d("WatchListCheckJob onRunJob");

        if (watchList.isEmpty()) {
            schedule();
            return Result.SUCCESS;
        }

        final Observable<CurrencyHelper> currencyObservable = currencyRepository.onCurrentCurrencyChanged()
                .take(1);
        final Observable<List<GameInfo>> gamesOnSale = watchList.getGamesOnSale()
                .toObservable()
                .take(1);

        try {
            final GameTitlePrice gameTitlePrice = currencyObservable.zipWith(gamesOnSale, (currencyHelper, gameInfoList) -> new GameTitlePrice(context, gameInfoList, currencyHelper)).toBlocking().single();
            Notification notification = null;
            if (gameTitlePrice.gamesOnSaleCount == 1) {
                notification = WatchListNotification.setUpNotification(context, gameTitlePrice.notificationTextList.get(0));
            } else if (gameTitlePrice.gamesOnSaleCount > 1) {
                notification = WatchListNotification.setUpNotification(context, gameTitlePrice.notificationTextList, gameTitlePrice.gamesOnSaleCount);
            }

            if (notification == null) {
                schedule();
                return Result.SUCCESS;
            }

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, notification);

            schedule();
            return Result.SUCCESS;

        } catch (NoSuchElementException e) {
            Timber.e(e, "Error while running WatchListCheckJob");
            return Result.RESCHEDULE;
        }
    }

    @Override
    protected void onReschedule(int newJobId) {
        Timber.d("WatchListCheckJob is rescheduled");
    }

    public static void schedule() {
        final JobManager jobManager = JobManager.instance();
        final Set<JobRequest> jobRequests = jobManager.getAllJobRequestsForTag(WatchListCheckJob.TAG);
        final boolean anyPendingJobRequests = !jobRequests.isEmpty();

        if (anyPendingJobRequests) {
            Timber.d("Calling schedule when there are already jobs pending.");
            return;
        }

        final JobScheduleHelper.ExecutionWindow executionWindow = JobScheduleHelper.getBestExecutionWindowForDate(new Date());

        new JobRequest.Builder(WatchListCheckJob.TAG)
                .setExecutionWindow(executionWindow.startMS, executionWindow.endMS)
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setBackoffCriteria(60_000L, JobRequest.BackoffPolicy.EXPONENTIAL)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
                .setRequiresCharging(false)
                .setRequirementsEnforced(true)
                .build()
                .schedule();

        Timber.d("Job Scheduled");
    }
}
