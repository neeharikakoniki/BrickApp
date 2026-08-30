package com.brick.earthquaketracker.ui.filter

import com.brick.earthquaketracker.domain.model.EarthquakeFilter
import com.brick.earthquaketracker.domain.model.SortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared filter/sort state across list and map tabs.
 * @Singleton so both ViewModels observe the same instance.
 */
@Singleton
class FilterStateHolder @Inject constructor() {

    private val _filter = MutableStateFlow(EarthquakeFilter.Default)
    val filter: StateFlow<EarthquakeFilter> = _filter.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.MOST_RECENT)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    fun updateFilter(filter: EarthquakeFilter) {
        _filter.value = filter
    }

    fun updateSortOrder(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }
}
