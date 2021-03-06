package com.jurcikova.ivet.countries.mvi.business.interactor

import com.jurcikova.ivet.countries.mvi.business.repository.CountryRepository
import com.jurcikova.ivet.countries.mvi.common.pairWithDelay
import com.jurcikova.ivet.countries.mvi.mvibase.MviInteractor
import com.jurcikova.ivet.countries.mvi.ui.countryDetail.CountryDetailAction
import com.jurcikova.ivet.countries.mvi.ui.countryDetail.CountryDetailResult
import com.strv.ktools.logD
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class CountryDetailInteractor(val countryRepository: CountryRepository) : MviInteractor<CountryDetailAction, CountryDetailResult> {

    override val actionProcessor =
            ObservableTransformer<CountryDetailAction, CountryDetailResult> { actions ->
                actions.publish { selector ->
                    Observable.merge(
                            selector.ofType(CountryDetailAction.LoadCountryDetailAction::class.java).compose(loadCountryDetail)
                                    .doOnNext { result ->
                                        logD("result: $result")
                                    },
                            selector.ofType(CountryDetailAction.AddToFavoriteAction::class.java).compose(addToFavorite)
                                    .doOnNext { result ->
                                        logD("result: $result")
                                    },
                            selector.ofType(CountryDetailAction.RemoveFromFavoriteAction::class.java).compose(removeFromFavorite)
                                    .doOnNext { result ->
                                        logD("result: $result")
                                    }
                    )
                }
            }

    private val loadCountryDetail =
            ObservableTransformer<CountryDetailAction.LoadCountryDetailAction, CountryDetailResult> { actions ->
                actions.flatMap { action ->
                    countryRepository.getCountry(action.countryName)
                            .toObservable()
                            // Wrap returned data into an immutable object
                            .map { country -> CountryDetailResult.LoadCountryDetailResult.Success(country) }
                            .cast(CountryDetailResult.LoadCountryDetailResult::class.java)
                            // Wrap any error into an immutable object and pass it down the stream
                            // without crashing.
                            // Because errors are data and hence, should just be part of the stream.
                            .onErrorReturn { CountryDetailResult.LoadCountryDetailResult.Failure(it) }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            // Emit an InProgress event to notify the subscribers (e.g. the UI) we are
                            // doing work and waiting on a response.
                            // We emit it after observing on the UI thread to allow the event to be emitted
                            // on the current frame and avoid jank.
                            .startWith(CountryDetailResult.LoadCountryDetailResult.InProgress)
                }
            }

    private val addToFavorite =
            ObservableTransformer<CountryDetailAction.AddToFavoriteAction, CountryDetailResult> { actions ->
                actions.flatMap { action ->
                    Completable.fromAction {
                        countryRepository.addToFavorite(action.countryName)
                    }
                            .andThen(
                                    // Emit two events to allow the UI notification to be hidden after
                                    // some delay
                                    pairWithDelay(
                                            CountryDetailResult.AddToFavoriteResult.Success,
                                            CountryDetailResult.AddToFavoriteResult.Reset)
                            )
                            .cast(CountryDetailResult::class.java)
                            .onErrorReturn { CountryDetailResult.AddToFavoriteResult.Failure(it) }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .startWith(CountryDetailResult.AddToFavoriteResult.InProgress)
                }
            }

    private val removeFromFavorite =
            ObservableTransformer<CountryDetailAction.RemoveFromFavoriteAction, CountryDetailResult> { actions ->
                actions.flatMap { action ->
                    Completable.fromAction {
                        countryRepository.removeFromFavorite(action.countryName)
                    }
                            .andThen(
                                    // Emit two events to allow the UI notification to be hidden after
                                    // some delay
                                    pairWithDelay(
                                            CountryDetailResult.RemoveFromFavoriteResult.Success,
                                            CountryDetailResult.RemoveFromFavoriteResult.Reset)
                            )
                            .cast(CountryDetailResult::class.java)
                            .onErrorReturn { CountryDetailResult.RemoveFromFavoriteResult.Failure(it) }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .startWith(CountryDetailResult.RemoveFromFavoriteResult.InProgress)
                }
            }
}