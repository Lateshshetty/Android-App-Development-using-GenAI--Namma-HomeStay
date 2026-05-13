package com.nammahomestay.ui.host

import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.nammahomestay.ui.common.BaseShellFragment
import com.nammahomestay.viewmodel.HostViewModel
import kotlinx.coroutines.launch

class AnalyticsFragment : BaseShellFragment() {
    private val vm: HostViewModel by viewModels()
    override val screenTitle = "Stats"
    override fun build() {
        val chart = BarChart(requireContext())
        form.addView(chart, android.widget.LinearLayout.LayoutParams(-1, 480))
        viewLifecycleOwner.lifecycleScope.launch {
            vm.inquiriesList.collect { rows ->
                form.removeAllViews()
                text("Total Inquiries: ${rows.size}")
                text("This Week: ${rows.size}")
                text("Menu Today: ready")
                form.addView(chart, android.widget.LinearLayout.LayoutParams(-1, 480))
                chart.data = BarData(BarDataSet((0..6).map { BarEntry(it.toFloat(), rows.size.toFloat()) }, "Inquiries last 7 days"))
                chart.invalidate()
            }
        }
    }
}
