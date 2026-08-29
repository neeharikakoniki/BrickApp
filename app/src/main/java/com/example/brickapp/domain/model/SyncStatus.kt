package com.example.brickapp.domain.model

import java.time.Instant

data class SyncStatus(
    val lastSyncAt: Instant?,
    val inFlight: Boolean,
)
