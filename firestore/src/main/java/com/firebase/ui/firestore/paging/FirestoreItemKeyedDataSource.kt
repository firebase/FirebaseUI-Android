package com.firebase.ui.firestore.paging


import android.arch.paging.DataSource
import android.arch.paging.ItemKeyedDataSource
import android.arch.paging.PagedList
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.*
import com.google.firebase.firestore.Source.CACHE
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Implementation of [ItemKeyedDataSource] for Firebase Firestore.
 *
 * [errorCallback] is optional and if not provided the [defaultErrorCallback] will be used.
 *
 * Note: If user provide [errorCallback] he should control data source invalidation manually.
 *
 * @property directQuery The direct query.
 * @property reverseQuery The reverse query.
 * @property errorCallback The error callback.
 */
@Suppress("NOTHING_TO_INLINE")
class FirestoreItemKeyedDataSource(
        private val directQuery: Query,
        private val reverseQuery: Query,
        private val errorCallback: ErrorCallback?
) : ItemKeyedDataSource<DocumentSnapshot, DocumentSnapshot>() {

    private val registrations = mutableListOf<ListenerRegistration>()

    /**
     * Factory for [FirestoreItemKeyedDataSource].
     *
     * @property queryFactory The query factory.
     * @property errorCallback The error callback.
     */
    class Factory @JvmOverloads constructor(
            private val queryFactory: QueryFactory,
            private val errorCallback: ErrorCallback? = null
    ) : DataSource.Factory<DocumentSnapshot, DocumentSnapshot>() {

        /**
         * Creates and returns [FirestoreItemKeyedDataSource].
         *
         * @return The created data source.
         */
        override fun create(): DataSource<DocumentSnapshot, DocumentSnapshot> =
                FirestoreItemKeyedDataSource(queryFactory.create(), queryFactory.createReverse(), errorCallback)
    }

    override fun loadInitial(
            params: LoadInitialParams<DocumentSnapshot>,
            callback: LoadInitialCallback<DocumentSnapshot>
    ) {
        val key = params.requestedInitialKey
        val loadSize = params.requestedLoadSize.toLong()

        if (key == null) {  // Initial data load could be asynchronous.
            loadFromBeginning(loadSize, callback)
        } else {    // Stale data refresh. Should be synchronous to prevent RecyclerView items disappearing and loss of current position.
            loadAround(key, loadSize, callback).await()
        }
    }

    /**
     * Tries to load some items before and after provided [key].
     *
     * @param key The key to load around.
     * @param loadSize Requested number of items to load.
     *
     * @return A Task that will be resolved with the results of the Query.
     */
    private fun loadAround(
            key: DocumentSnapshot,
            loadSize: Long,
            callback: LoadInitialCallback<DocumentSnapshot>
    ) = tryGetPriorKey(key, loadSize / 2).continueWithTask { previousTask ->
        loadStartAt(previousTask.result!!, loadSize, callback)
    }

    /**
     * Tries to get key before provided [key] to start load page from it.
     *
     * @param key The key to load before.
     * @param preloadSize Requested number of items to preload.
     *
     * @return A Task that will be resolved with the results of the Query.
     */
    private fun tryGetPriorKey(
            key: DocumentSnapshot,
            preloadSize: Long
    ): Task<DocumentSnapshot> = getItemsBeforeKey(key, preloadSize).continueWith { task ->
        if (task.isSuccessful) {
            val documents = task.result!!.documents
            documents.getOrElse(documents.lastIndex) { key }
        } else {
            key
        }
    }

    private fun getItemsBeforeKey(
            key: DocumentSnapshot,
            preloadSize: Long
    ) = endAtQuery(key, preloadSize).get(CACHE)

    private fun loadStartAt(
            key: DocumentSnapshot,
            loadSize: Long,
            callback: LoadInitialCallback<DocumentSnapshot>
    ): Task<Unit> = resultsListener(callback).also { listener ->
        register(startAtQuery(key, loadSize), listener)
    }.firstEventFiredTask

    private fun loadFromBeginning(
            loadSize: Long,
            callback: LoadInitialCallback<DocumentSnapshot>
    ) {
        register(startFromBeginningQuery(loadSize), resultsListener(callback))
    }

    override fun loadAfter(
            params: LoadParams<DocumentSnapshot>,
            callback: LoadCallback<DocumentSnapshot>
    ) {
        register(startAfterQuery(params), resultsListener(callback))
    }

    override fun loadBefore(
            params: LoadParams<DocumentSnapshot>,
            callback: LoadCallback<DocumentSnapshot>
    ) {
        register(startBeforeQuery(params), reversedResultsListener(callback))
    }

    override fun getKey(item: DocumentSnapshot) = item

    override fun invalidate() {
        super.invalidate()
        registrations.clear()
    }

    private fun register(
            query: Query,
            resultListener: ResultsListener
    ) {
        registrations.add(query.addSnapshotListener(resultListener))
    }

    private fun startFromBeginningQuery(loadSize: Long) = directQuery.limit(loadSize)

    private fun startAtQuery(key: DocumentSnapshot, loadSize: Long) = directQuery.startAt(key).limit(loadSize)

    private fun startAfterQuery(params: LoadParams<DocumentSnapshot>) =
            directQuery.startAfter(params.key).limit(params.requestedLoadSize.toLong())

    private fun startBeforeQuery(params: LoadParams<DocumentSnapshot>) =
            reverseQuery.startAfter(params.key).limit(params.requestedLoadSize.toLong())

    private fun endAtQuery(key: DocumentSnapshot, loadSize: Long) = reverseQuery.startAt(key).limit(loadSize)

    private fun resultsListener(callback: LoadCallback<DocumentSnapshot>) = ResultsListener(callback)
    private fun reversedResultsListener(callback: LoadCallback<DocumentSnapshot>) = ResultsListener(callback, true)

    /**
     * Implementation of [EventListener].
     *
     * Listen for [DocumentSnapshot] for the requested page to load.
     * The first received result is treated as page data. The subsequent results treated as data change and invoke
     * data source invalidation.
     *
     * @property callback The callback to pass data to [PagedList].
     * @property isReversedResults The flag that tells that this listener will receive reversed result.
     */
    private inner class ResultsListener(
            private val callback: LoadCallback<DocumentSnapshot>,
            private val isReversedResults: Boolean = false
    ) : EventListener<QuerySnapshot> {

        private val isFirstEvent = AtomicBoolean(true)
        private val taskCompletionSource = TaskCompletionSource<Unit>()

        /**
         * A [Task] that will be resolved when this listener get first result or error.
         * Used to load initial data synchronously on stale data refresh.
         */
        val firstEventFiredTask get() = taskCompletionSource.task

        override fun onEvent(snapshot: QuerySnapshot?, exception: FirebaseFirestoreException?) {
            if (isFirstEvent()) {

                snapshot?.documents?.also { documents ->
                    callback.onResult(if (isReversedResults) documents.reversed() else documents)
                } ?: notifyError(exception!!)

                taskCompletionSource.setResult(Unit)
            } else {
                invalidate()
            }
        }

        private inline fun isFirstEvent() = isFirstEvent.compareAndSet(true, false)
    }

    private fun notifyError(exception: FirebaseFirestoreException) {
        getErrorCallback().onError(exception)
    }

    private inline fun getErrorCallback() = errorCallback ?: defaultErrorCallback

    /**
     * Callback to listen to data loading errors.
     */
    interface ErrorCallback {
        fun onError(exception: FirebaseFirestoreException)
    }

    private inline fun <TResult> Task<TResult>.await() {
        try {
            Tasks.await(this)
        } catch (e: Throwable) {  }
    }

    /**
     * Used if user do not provide [errorCallback].
     * Simply invalidates this data source if loading error occurred.
     */
    private val defaultErrorCallback = object : ErrorCallback {
        override fun onError(exception: FirebaseFirestoreException) {
            invalidate()
        }
    }
}