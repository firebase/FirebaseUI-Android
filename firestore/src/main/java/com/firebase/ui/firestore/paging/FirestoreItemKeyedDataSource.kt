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
        val lastSeenDocument = params.requestedInitialKey
        val requestedLoadSize = params.requestedLoadSize.toLong()

        if (lastSeenDocument == null) {  // Initial data load could be asynchronous.
            loadFromBeginning(requestedLoadSize, callback)
        } else {    // Stale data refresh. Should be synchronous to prevent RecyclerView items disappearing and loss of current position.
            loadAround(lastSeenDocument, requestedLoadSize, callback).await()
        }
    }

    /**
     * Tries to load half of [loadSize] documents before [document] and half of [loadSize] documents starting at provided [document].
     *
     * @param document The document to load around.
     * @param loadSize Requested number of items to load.
     *
     * @return A Task that will be resolved with the results of the Query.
     */
    private fun loadAround(
            document: DocumentSnapshot,
            loadSize: Long,
            callback: LoadInitialCallback<DocumentSnapshot>
    ) = tryGetDocumentPrior(document, loadSize / 2).continueWithTask { previousTask ->
        loadStartAt(previousTask.result!!, loadSize, callback)
    }

    /**
     * Returns document before [document] maximum by [preloadSize] or [document] if there is no documents before.
     *
     * @param document The document to load before.
     * @param preloadSize Requested number of items to preload.
     *
     * @return A Task that will be resolved with the results of the Query.
     */
    private fun tryGetDocumentPrior(
            document: DocumentSnapshot,
            preloadSize: Long
    ): Task<DocumentSnapshot> = getItemsBefore(document, preloadSize).continueWith { task ->
        if (task.isSuccessful) {
            task.result!!.documents.lastOrNull() ?: document
        } else {
            document
        }
    }

    private fun getItemsBefore(
            document: DocumentSnapshot,
            loadSize: Long
    ) = queryEndAt(document, loadSize).get(CACHE)

    private fun loadStartAt(
            document: DocumentSnapshot,
            loadSize: Long,
            callback: LoadInitialCallback<DocumentSnapshot>
    ) = PageLoader(callback).also { pageLoader ->
        addPageLoader(queryStartAt(document, loadSize), pageLoader)
    }.task

    private fun loadFromBeginning(
            loadSize: Long,
            callback: LoadInitialCallback<DocumentSnapshot>
    ) {
        addPageLoader(
                queryStartFromBeginning(loadSize),
                PageLoader(callback)
        )
    }

    override fun loadAfter(
            params: LoadParams<DocumentSnapshot>,
            callback: LoadCallback<DocumentSnapshot>
    ) {
        addPageLoader(
                queryStartAfter(params.key, params.requestedLoadSize.toLong()),
                PageLoader(callback)
        )
    }

    override fun loadBefore(
            params: LoadParams<DocumentSnapshot>,
            callback: LoadCallback<DocumentSnapshot>
    ) {
        addPageLoader(
                queryEndBefore(params.key, params.requestedLoadSize.toLong()),
                PageLoader(callback.reversedResults())
        )
    }

    override fun getKey(item: DocumentSnapshot) = item

    override fun invalidate() {
        super.invalidate()
        removeAllPageLoaders()
    }

    private fun removeAllPageLoaders() {
        registrations.forEach { it.remove() }
        registrations.clear()
    }

    private fun addPageLoader(
            query: Query,
            pageLoader: PageLoader
    ) {
        registrations.add(query.addSnapshotListener(pageLoader))
    }

    private fun queryStartFromBeginning(loadSize: Long) = directQuery.limit(loadSize)

    private fun queryStartAt(document: DocumentSnapshot, loadSize: Long) = directQuery.startAt(document).limit(loadSize)

    private fun queryStartAfter(document: DocumentSnapshot, loadSize: Long) = directQuery.startAfter(document).limit(loadSize)

    private fun queryEndAt(document: DocumentSnapshot, loadSize: Long) = reverseQuery.startAt(document).limit(loadSize)

    private fun queryEndBefore(document: DocumentSnapshot, loadSize: Long) = reverseQuery.startAfter(document).limit(loadSize)

    /**
     * Implementation of [EventListener].
     *
     * Listen for loading of the requested page.
     *
     * The first received result is treated as page data. The subsequent results treated as data change and invoke
     * data source invalidation.
     *
     * @property callback The callback to pass data to [PagedList].
     */
    private inner class PageLoader(
            private val callback: LoadCallback<DocumentSnapshot>
    ) : EventListener<QuerySnapshot> {
        private val isWaitingForPage = AtomicBoolean(true)
        private val taskCompletionSource = TaskCompletionSource<MutableList<DocumentSnapshot>>()

        /**
         * A [Task] that will be resolved when this page loader loads page or fails.
         * Used to load initial data synchronously on stale data refresh.
         */
        val task get() = taskCompletionSource.task

        override fun onEvent(snapshot: QuerySnapshot?, exception: FirebaseFirestoreException?) {
            if (isWaitingForPage()) {

                snapshot?.documents?.also { result ->
                    submit(result)
                } ?: submit(exception!!)

            } else {
                invalidate()
            }
        }

        private fun submit(result: MutableList<DocumentSnapshot>) {
            callback.onResult(result)
            taskCompletionSource.setResult(result)
        }

        private fun submit(exception: FirebaseFirestoreException) {
            notifyError(exception)
            taskCompletionSource.setException(exception)
        }

        private inline fun isWaitingForPage() = isWaitingForPage.compareAndSet(true, false)
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

    /**
     * Wraps [LoadCallback] with new one that reverses the results list.
     */
    private fun LoadCallback<DocumentSnapshot>.reversedResults() = object : LoadCallback<DocumentSnapshot>() {
        override fun onResult(data: MutableList<DocumentSnapshot>) {
            this@reversedResults.onResult(data.reversed())
        }
    }

    /**
     * Waits for [Task] to resolve ignoring whether it succeeded or failed.
     * Used to load initial page synchronously on stale data refresh.
     */
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