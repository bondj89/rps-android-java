/*
 *  Copyright 2019 Nicholas Bennett & Deep Dive Coding/CNM Ingenuity
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.cnm.deepdive.rps.controller

import android.os.Bundle
import android.os.PersistableBundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import edu.cnm.deepdive.rps.R
import edu.cnm.deepdive.rps.databinding.ActivityMainBinding
import edu.cnm.deepdive.rps.viewmodel.MainViewModel

/**
 * Controller class supporting user interaction with an instance of [ ]. Note that in this implementation, this class does not itself
 * interact directly with [edu.cnm.deepdive.rps.model.Arena], nor [ ] (which displays the arena terrain). Instead, this class
 * interacts with [MainViewModel] to control simulation execution, and display updates are
 * managed through LiveData data binding between [MainViewModel] and [ ].
 *
 * @author Nicholas Bennett
 */
class MainActivity : AppCompatActivity() {

    private var viewModel: MainViewModel? = null
    private var running: Boolean = false

    /**
     * Sets up a connection to an instance of [MainViewModel], in order to observe and modify
     * the execution state of the simulation, and to enable LiveData data binding between [ ] and the views in the layout (in particular, [ ]).
     *
     * @param savedInstanceState state saved on a previous invocation of [ ][.onSaveInstanceState].
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java!!)
        binding.viewModel = viewModel
        viewModel?.isRunning?.observe(this, Observer { running: Boolean ->
            if (running != this@MainActivity.running) {
                this@MainActivity.running = running
                invalidateOptionsMenu()
            }
        })
    }

    /**
     * Inflates the options menu containing simulation execution controls.
     *
     * @param menu [Menu] to which inflated items are added.
     * @return flag indicating that items have been added to `menu`.
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.actions, menu)
        return true
    }

    /**
     * Updates the options menu, based on the current execution state of the [ ] simulation, as reflected in [ ][MainViewModel.isRunning].
     *
     * @param menu [Menu] containing options menu items.
     * @return flag indicating that the menu has been modified (always `true`, in this case).
     */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.run).isVisible = !running
        menu.findItem(R.id.pause).isVisible = running
        menu.findItem(R.id.reset).isVisible = !running
        return true
    }

    /**
     * Handles user selection of an options menu item by invoking [MainViewModel] methods to
     * start (resume), stop, or reset the [edu.cnm.deepdive.rps.model.Arena] simulation.
     *
     * @param item selected item in options menu.
     * @return flag indicating whether user selection was handled by this method (or the superclass's
     * implementation).
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var handled = when (item.itemId) {
            R.id.run ->  {
                viewModel?.start()
                true
            }
            R.id.pause -> {
                viewModel?.stop()
                true
            }
            R.id.reset -> {
                viewModel?.reset()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
        return handled
    }

}