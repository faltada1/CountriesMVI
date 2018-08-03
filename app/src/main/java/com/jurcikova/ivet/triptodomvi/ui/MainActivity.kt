package com.jurcikova.ivet.triptodomvi.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import androidx.navigation.Navigation
import androidx.navigation.ui.NavigationUI
import com.jurcikova.ivet.triptodomvi.R
import com.jurcikova.ivet.triptodomvi.common.BindActivity
import com.jurcikova.ivet.triptodomvi.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by BindActivity(R.layout.activity_main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.toolbar.let {
            it.title = title
            it.inflateMenu(R.menu.menu_search)
            it.setOnMenuItemClickListener {item ->
                NavigationUI.onNavDestinationSelected(item, Navigation.findNavController(this, R.id.my_nav_host_fragment))
            }
        }
    }
}