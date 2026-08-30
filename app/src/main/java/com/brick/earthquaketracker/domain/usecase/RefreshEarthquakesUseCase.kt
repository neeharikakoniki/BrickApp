package com.brick.earthquaketracker.domain.usecase

import com.brick.earthquaketracker.core.common.AppResult
import com.brick.earthquaketracker.domain.repository.EarthquakeRepository
import javax.inject.Inject

class RefreshEarthquakesUseCase @Inject constructor(
    private val repository: EarthquakeRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> = repository.refresh()
}
