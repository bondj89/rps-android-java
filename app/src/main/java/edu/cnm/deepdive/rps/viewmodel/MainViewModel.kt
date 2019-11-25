
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
package edu.cnm.deepdive.rps.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.cnm.deepdive.rps.model.Arena
import edu.cnm.deepdive.rps.viewmodel.MainViewModel

/**
 * Manages interaction with (and simulation execution of) an instance of [Arena], and exposes
 * its key properties as LiveData.
 *
 * @author Nicholas Bennett
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val arena = MutableLiveData<Arena?>(null)
    /**
     * Returns LiveData containing the current `long` generation count of the [Arena]
     * instance.
     */
    val generation = MutableLiveData<Long>()
    private val running = MutableLiveData<Boolean>()
    private var runner: Runner? = null
    /**
     * Returns LiveData containing the current [Arena].
     */
    fun getArena(): LiveData<Arena?> {
        return arena
    }

    /**
     * Returns LiveData containing a `boolean` flag indicating whether the simulation model is
     * currently running.
     */
    val isRunning: LiveData<Boolean>
        get() = running


    /**
     * Starts or resumes execution of the simulation of the [Arena] instance.
     */
    fun start() {
        stopRunner()
        running.value = true
        runner = Runner()
        runner!!.start()
    }

    /**
     * Pauses execution of the simulation of the [Arena] instance.
     */
    fun stop() {
        stopRunner()
        running.value = false
    }

    /**
     * Resets the current [Arena] (if any). If an `Arena` has not yet been created,
     * creates one with the [.DEFAULT_NUM_BREEDS] breeds and [.DEFAULT_ARENA_SIZE] height
     * and width.
     */
    fun reset() {
        val arena = arena.value
        if (arena == null) {
            reset(DEFAULT_NUM_BREEDS, DEFAULT_ARENA_SIZE)
        } else {
            arena.init()
            generation.value = arena.generation
            running.setValue(false)
        }
    }

    /**
     * Creates a new [Arena] with the specified number of breeds and size.
     *
     * @param numBreeds number of breeds placed initially on the terrain of the new [Arena].
     * @param arenaSize height and width of the terrain of the new [Arena].
     */
    fun reset(numBreeds: Byte, arenaSize: Int) {
        val arena = Arena.Builder()
                .setNumBreeds(numBreeds)
                .setArenaSize(arenaSize)
                .build()
        arena.init()
        this.arena.value = arena
        generation.value = arena.generation
        running.value = false
    }

    private fun stopRunner() {
        if (runner != null) {
            runner!!.setRunning(false)
            runner = null
        }
    }

    private inner class Runner : Thread() {
        private var running = true
        override fun run() {
            while (running) {
                val arena = arena.value
                try {
                    if (arena != null) {
                        for (i in 0 until ITERATIONS_PER_SLEEP) {
                            arena.advance()
                        }
                        running = running and !arena.isAbsorbed
                    }
                    generation.postValue(arena!!.generation)
                    sleep(SLEEP_INTERVAL.toLong())
                } catch (expected: InterruptedException) { // Ignore innocuous exception.
                }
            }
            this@MainViewModel.running.postValue(false)
        }

        fun setRunning(running: Boolean) {
            this.running = running
        }
    }

    companion object {
        /** Default number of breeds populating the [Arena] instance managed by this ViewModel.  */
        const val DEFAULT_NUM_BREEDS: Byte = 5
        /** Default size of the terrain used in the [Arena] instance managed by this ViewModel.  */
        const val DEFAULT_ARENA_SIZE = 50
        private const val ITERATIONS_PER_SLEEP = DEFAULT_ARENA_SIZE * DEFAULT_ARENA_SIZE / 20
        private const val SLEEP_INTERVAL = 1
    }

    /**
     * Initializes ViewModel and creates an instance of [Arena] with default constructor
     * paramater values.
     *
     * @param application
     */
    init {
        reset()
    }
}