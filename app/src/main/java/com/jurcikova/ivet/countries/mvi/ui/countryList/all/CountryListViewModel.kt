package com.jurcikova.ivet.countries.mvi.ui.countryList.all

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.LiveDataReactiveStreams
import android.arch.lifecycle.ViewModel
import com.jurcikova.ivet.countries.mvi.business.interactor.CountryListInteractor
import com.jurcikova.ivet.countries.mvi.common.notOfType
import com.jurcikova.ivet.countries.mvi.mvibase.MviViewModel
import com.jurcikova.ivet.countries.mvi.ui.countryList.all.CountryListAction.LoadCountriesAction
import com.jurcikova.ivet.countries.mvi.ui.countryList.all.CountryListIntent.InitialIntent
import com.jurcikova.ivet.countries.mvi.ui.countryList.all.CountryListIntent.SwipeToRefresh
import com.jurcikova.ivet.countries.mvi.ui.countryList.all.CountryListResult.LoadCountriesResult
import com.strv.ktools.inject
import com.strv.ktools.logD
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject

class CountryListViewModel : ViewModel(), MviViewModel<CountryListIntent, CountryListViewState> {

    private val countryListInteractor by inject<CountryListInteractor>()

    /**
     * The Reducer is where [MviViewState], that the [MviView] will use to
     * render itself, are created.
     * It takes the last cached [MviViewState], the latest [MviResult] and
     * creates a new [MviViewState] by only updating the related fields.
     * This is basically like a big switch statement of all possible types for the [MviResult]
     */
    private val reducer = BiFunction { previousState: CountryListViewState, result: CountryListResult ->
        when (result) {
            is LoadCountriesResult -> when (result) {
                is LoadCountriesResult.Success -> {
                    previousState.copy(
                            isLoading = false,
                            isRefreshing = false,
                            countries = result.countries

                    )
                }
                is LoadCountriesResult.Failure -> previousState.copy(isLoading = false, isRefreshing = false, error = result.error)
                is LoadCountriesResult.InProgress -> {
                    if (result.isRefreshing) {
                        previousState.copy(isLoading = false, isRefreshing = true)
                    } else previousState.copy(isLoading = true, isRefreshing = false)
                }
            }
        }
    }

    /**
     * Proxy subject used to keep the stream alive even after the UI gets recycled.
     * This is basically used to keep ongoing events and the last cached State alive
     * while the UI disconnects and reconnects on config changes.
     */
    private val intentsSubject: PublishSubject<CountryListIntent> = PublishSubject.create()
    /**
     * Compose all components to create the stream logic
     */
    private val statesObservable: Observable<CountryListViewState> = intentsSubject
            .compose(intentFilter)
            .map(this::actionFromIntent)
            .doOnNext { action ->
                logD("action: $action")
            }
            .compose(countryListInteractor.actionProcessor)
            // Cache each state and pass it to the reducer to create a new state from
            // the previous cached one and the latest Result emitted from the action processor.
            // The Scan operator is used here for the caching.
            .scan(CountryListViewState.idle(), reducer)
            // When a reducer just emits previousState, there's no reason to call render. In fact,
            // redrawing the UI in cases like this can cause jank (e.g. messing up snackbar animations
            // by showing the same snackbar twice in rapid succession).
            .distinctUntilChanged()
            // Emit the last one event of the stream on subscription
            // Useful when a View rebinds to the ViewModel after rotation.
            .replay(1)
            // Create the stream on creation without waiting for anyone to subscribe
            // This allows the stream to stay alive even when the UI disconnects and
            // match the stream's lifecycle to the ViewModel's one.
            .autoConnect(0)

    /**
     * take only the first ever InitialIntent and all intents of other types
     * to avoid reloading data on config changes
     */
    private val intentFilter: ObservableTransformer<CountryListIntent, CountryListIntent>
        get() = ObservableTransformer { intents ->
            intents.publish { selected ->
                Observable.merge(
                        selected.ofType(InitialIntent::class.java).take(1),
                        selected.notOfType(InitialIntent::class.java)
                )
            }
        }

    override fun states(): LiveData<CountryListViewState> =
            LiveDataReactiveStreams.fromPublisher(statesObservable.toFlowable(BackpressureStrategy.BUFFER))

    override fun processIntents(intents: Observable<CountryListIntent>) {
        intents
                .doOnNext { intent ->
                    logD("intent: $intent")
                }
                .subscribe(intentsSubject)
    }

    /**
     * Translate an [MviIntent] to an [MviAction].
     * Used to decouple the UI and the business logic to allow easy testings and reusability.
     */
    private fun actionFromIntent(intent: CountryListIntent): CountryListAction {
        return when (intent) {
            is InitialIntent -> LoadCountriesAction(false)
            is SwipeToRefresh -> LoadCountriesAction(true)
        }
    }
}