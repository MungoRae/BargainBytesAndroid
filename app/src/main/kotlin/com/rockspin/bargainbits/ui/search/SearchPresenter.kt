package com.rockspin.bargainbits.ui.search

import com.rockspin.bargainbits.data.models.GameSearchResult
import com.rockspin.bargainbits.data.rest_client.GameApiService
import com.rockspin.bargainbits.ui.mvp.BaseMvpPresenter
import com.rockspin.bargainbits.ui.mvp.BaseMvpView
import com.rockspin.bargainbits.util.format.PriceFormatter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by valentin.hinov on 22/04/2017.
 */
class SearchPresenter @Inject constructor(val apiService: GameApiService, val formatter: PriceFormatter) : BaseMvpPresenter<SearchPresenter.SearchView>() {

    interface SearchView : BaseMvpView {
        fun showLoading(show: Boolean)
        fun updateResults(viewModels: List<ResultViewModel>)
        fun showFetchError()
        fun showNoResults()
        fun goBack()
        fun showSearchDetail(gameId: String, gameName: String)

        val onBackClick: Observable<*>
        val onResultClick: Observable<Int>
    }

    private var latestResults = emptyList<GameSearchResult>()

    override fun onViewCreated(view: SearchView) {
        super.onViewCreated(view)

        addLifetimeDisposable(view.onBackClick
            .subscribe {
                this.view?.goBack()
            })

        addLifetimeDisposable(view.onResultClick
            .subscribe {
                val selectedResult = latestResults[it]
                this.view?.showSearchDetail(selectedResult.gameID, selectedResult.name)
            })
    }

    var searchQuery: String = ""
        set(value) {
            addLifetimeDisposable(apiService.searchGames(value)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view?.showLoading(true) }
                .doFinally { view?.showLoading(false) }
                .doOnSuccess { latestResults = it }
                .map { it.map { ResultViewModel(it.name, it.thumbnailUrl, formatter.formatPrice(it.cheapestPrice)) } }
                .subscribe( { viewModels ->
                    if (viewModels.isEmpty()) {
                        view?.showNoResults()
                    } else {
                        view?.updateResults(viewModels)
                    }
                }, { throwable ->
                    Timber.e("Error fetching list of game search results", throwable)
                    view?.showFetchError()
                }))
        }
}