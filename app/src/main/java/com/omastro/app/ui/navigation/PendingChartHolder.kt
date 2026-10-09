package com.omastro.app.ui.navigation

import com.omastro.app.data.remote.SavedChart

/**
 * Temporary holder for a SavedChart selected in SavedChartsScreen,
 * to be loaded by KundaliScreen. Cleared after consumption.
 */
object PendingChartHolder {
    var pendingChart: SavedChart? = null
        private set

    fun set(chart: SavedChart) {
        pendingChart = chart
    }

    fun consume(): SavedChart? {
        val chart = pendingChart
        pendingChart = null
        return chart
    }
}
