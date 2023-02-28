/*
Copyright 2020 RethinkDNS and its authors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.celzero.bravedns.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import by.kirich1409.viewbindingdelegate.viewBinding
import com.celzero.bravedns.R
import com.celzero.bravedns.databinding.ActivityNetworkLogsBinding
import com.celzero.bravedns.service.BraveVPNService
import com.celzero.bravedns.service.PersistentState
import com.celzero.bravedns.service.VpnController
import com.celzero.bravedns.util.Constants
import com.celzero.bravedns.util.Themes.Companion.getCurrentTheme
import com.google.android.material.tabs.TabLayoutMediator
import org.koin.android.ext.android.inject

class NetworkLogsActivity : AppCompatActivity(R.layout.activity_network_logs) {
    private val b by viewBinding(ActivityNetworkLogsBinding::bind)
    private var fragmentIndex = 0
    private var searchParam = ""
    private val persistentState by inject<PersistentState>()

    enum class Tabs(val screen: Int) {
        NETWORK_LOGS(0),
        DNS_LOGS(1);

        companion object {
            fun getCount(): Int {
                return values().count()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getCurrentTheme(isDarkThemeOn(), persistentState.theme))
        super.onCreate(savedInstanceState)
        fragmentIndex = intent.getIntExtra(Constants.VIEW_PAGER_SCREEN_TO_LOAD, 0)
        searchParam = intent.getStringExtra(Constants.SEARCH_QUERY) ?: ""
        init()
    }

    private fun Context.isDarkThemeOn(): Boolean {
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
    }

    private fun init() {

        b.logsActViewpager.adapter =
            object : FragmentStateAdapter(this) {
                override fun createFragment(position: Int): Fragment {
                    return when (position) {
                        Tabs.NETWORK_LOGS.screen ->
                            ConnectionTrackerFragment.newInstance(searchParam)
                        Tabs.DNS_LOGS.screen -> DnsLogFragment.newInstance(searchParam)
                        else -> FirewallSettingsFragment.newInstance()
                    }
                }

                override fun getItemCount(): Int {
                    return Tabs.getCount()
                }
            }

        TabLayoutMediator(b.logsActTabLayout, b.logsActViewpager) { tab, position
                -> // Styling each tab here
                tab.text =
                    when (position) {
                        Tabs.NETWORK_LOGS.screen ->
                            getString(R.string.firewall_act_network_monitor_tab)
                        Tabs.DNS_LOGS.screen -> getString(R.string.dns_mode_info_title)
                        else -> getString(R.string.firewall_act_network_monitor_tab)
                    }
            }
            .attach()

        b.logsActViewpager.setCurrentItem(fragmentIndex, false)

        observeAppState()
    }

    private fun observeAppState() {
        VpnController.connectionStatus.observe(this) {
            if (it == BraveVPNService.State.PAUSED) {
                startActivity(Intent().setClass(this, PauseActivity::class.java))
                finish()
            }
        }
    }
}
