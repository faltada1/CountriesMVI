package com.jurcikova.ivet.countries.mvi.ui.countryList.all

import com.jurcikova.ivet.countries.mvi.business.entity.Country
import com.jurcikova.ivet.countries.mvi.mvibase.MviResult

sealed class CountryListResult : MviResult {
    sealed class LoadCountriesResult : CountryListResult() {
        data class Success(val countries: List<Country>) : LoadCountriesResult()
        data class Failure(val error: Throwable) : LoadCountriesResult()
        data class InProgress(val isRefreshing: Boolean) : LoadCountriesResult()
    }
}