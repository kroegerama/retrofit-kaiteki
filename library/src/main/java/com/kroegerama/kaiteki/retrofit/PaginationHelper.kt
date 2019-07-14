package com.kroegerama.kaiteki.retrofit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PageKeyedDataSource
import androidx.paging.PagedList
import kotlinx.coroutines.*
import retrofit2.Response
import java.io.Closeable

private typealias ApiListFun<T> = suspend (page: Int) -> Response<List<T>>
private typealias RetryFun = (() -> Unit)?

interface PageProvider {
    val firstPage: Int
    fun getNextPage(currentPage: Int): Int?
    fun getPreviousPage(currentPage: Int): Int?
}

object DefaultPageProvider : PageProvider {
    override val firstPage = 0
    override fun getNextPage(currentPage: Int) = currentPage + 1
    override fun getPreviousPage(currentPage: Int): Int? = null
}

val DefaultPageConfig by lazy { PagedList.Config.Builder().setPageSize(10).setPrefetchDistance(20).build() }

class PagedListing<T>(
        val pagedList: LiveData<PagedList<T>>,
        val initialState: LiveData<ListingState>,
        val loadState: LiveData<ListingState>,
        val initialRetry: LiveData<RetryFun>,
        val loadRetry: LiveData<RetryFun>,
        private val refreshFun: () -> Unit,
        private val cancelFun: () -> Unit
) : Closeable {
    override fun close() = cancel()

    fun refresh() = refreshFun.invoke()
    fun cancel() = cancelFun.invoke()

    fun retryInitial() {
        initialRetry.value?.invoke()
    }

    fun retryLoad() {
        loadRetry.value?.invoke()
    }
}

fun <T> CoroutineScope.retrofitPagedListing(
        config: PagedList.Config = DefaultPageConfig,
        pageProvider: PageProvider = DefaultPageProvider,
        apiFun: ApiListFun<T>): PagedListing<T> {
    val parentJob = SupervisorJob()
    val factory = RetrofitDataSourceFactory(this, parentJob, apiFun, pageProvider)
    val livePagedList = LivePagedListBuilder(factory, config)
            .setFetchExecutor { CoroutineScope(Dispatchers.IO).launch { it.run() } }
            .build()

    return PagedListing(
            pagedList = livePagedList,
            initialState = Transformations.switchMap(factory.source) { it.initialState },
            loadState = Transformations.switchMap(factory.source) { it.loadState },
            initialRetry = Transformations.switchMap(factory.source) { it.initialRetry },
            loadRetry = Transformations.switchMap(factory.source) { it.loadRetry },
            refreshFun = { factory.source.value?.invalidate() },
            cancelFun = { parentJob.cancel() }
    )
}

class RetrofitDataSourceFactory<T>(
        private val scope: CoroutineScope,
        private val parentJob: Job,
        private val apiFun: ApiListFun<T>,
        private val pageProvider: PageProvider
) : DataSource.Factory<Int, T>() {

    val source = MutableLiveData<RetrofitDataSource<T>>()

    override fun create(): RetrofitDataSource<T> {
        val source = RetrofitDataSource(scope, parentJob, apiFun, pageProvider)
        this.source.postValue(source)
        return source
    }
}

class RetrofitDataSource<T>(
        private val scope: CoroutineScope,
        private val parentJob: Job,
        private val apiFun: ApiListFun<T>,
        private val pageProvider: PageProvider
) : PageKeyedDataSource<Int, T>() {

    val initialState = MutableLiveData<ListingState>()
    val loadState = MutableLiveData<ListingState>()

    val initialRetry = MutableLiveData<RetryFun>()
    val loadRetry = MutableLiveData<RetryFun>()

    override fun loadInitial(params: LoadInitialParams<Int>, callback: LoadInitialCallback<Int, T>) {
        val page = pageProvider.firstPage
        makeLoadRequest(true, page) { data ->
            callback.onResult(data, pageProvider.getPreviousPage(page), pageProvider.getNextPage(page))
        }
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, T>) {
        val page = params.key
        makeLoadRequest(false, page) { data ->
            callback.onResult(data, pageProvider.getNextPage(page))
        }
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, T>) {
        val page = params.key
        makeLoadRequest(false, page) { data ->
            callback.onResult(data, pageProvider.getPreviousPage(page))
        }
    }

    private fun makeLoadRequest(
            isInitial: Boolean,
            currentPage: Int,
            callback: (List<T>) -> Any
    ): Job = scope.launch(parentJob) {
        updateState(isInitial, ListingState.RUNNING)
        val result = retrofitCall { apiFun(currentPage) }

        if (result is Result.Success) {
            val items = result.data.orEmpty()
            callback(items)
            updateRetry(isInitial, null)
            updateState(isInitial, ListingState.FINISHED)
        } else {
            updateRetry(isInitial) {
                makeLoadRequest(isInitial, currentPage, callback)
            }
            updateState(isInitial, ListingState.IDLE)
        }
    }

    private suspend fun updateRetry(isInitial: Boolean, retry: RetryFun) = withContext(Dispatchers.Main) {
        if (isInitial) {
            initialRetry.value = retry
        } else {
            loadRetry.value = retry
        }
    }

    private suspend fun updateState(isInitial: Boolean, state: ListingState) = withContext(Dispatchers.Main) {
        if (isInitial) {
            initialState.value = state
        }
        loadState.value = state
    }
}