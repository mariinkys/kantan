package dev.mariinkys.kantan

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import dev.mariinkys.kantan.data.worker.DictionaryImportWorker
import javax.inject.Inject

@HiltAndroidApp
class KantanApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    @Inject
    lateinit var workManager: WorkManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleDictionaryImport()
    }

    /**
     * Enqueues the dictionary import exactly once. WorkManager persists the
     * "DICT_IMPORT" job across reboots — if the worker already succeeded it
     * will not run again. If it failed/retried last time it will pick up where
     * it left off.
     */
    private fun scheduleDictionaryImport() {
        val request = OneTimeWorkRequestBuilder<DictionaryImportWorker>().build()
        workManager.enqueueUniqueWork(
            "DICT_IMPORT",
            ExistingWorkPolicy.KEEP,   // don't restart if already done/running
            request
        )
    }
}