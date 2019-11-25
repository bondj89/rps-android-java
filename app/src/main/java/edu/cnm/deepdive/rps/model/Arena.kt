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
package edu.cnm.deepdive.rps.model

import java.util.*

/**
 * Models a non-transitive competitive ecosystem. With 3 breeds, the resulting ecosystem resembles
 * an evolutionary game of Rock-Paper-Scissors. While it can be a practical challenge to enumerate
 * the relationship of a non-transitive system with many more breeds than 3, it is in fact very
 * straightforward to simulate such a systemat least in an idealized form: dominance
 * relationships between the breeds can be envisioned by arranging them in regular intervals around
 * a circle; any given breed dominates those breeds that can be reached via a shorter path in the
 * counter-clockwise direction than in the clockwise direction.
 *
 * This ecosystem model is a stochastic CA, where all cells survive unchanged to the next
 * generationwith the possible exception of the cell chosen at random to be the "challenger",
 * and that cell's randomly selected neighbor (the "defender"). One or both (in the event of a tie)
 * of these 2 cells survive to the next generation, while the loser (if there is not a tie) is
 * replaced by a copy of the winner.
 *
 * The size of the terrain where the ecosystem resides is square, and the topology is toroidal:
 * cells on the left and right edges of any given row are considered to be neighbors, as are cells
 * at the top and bottom of any given column.
 *
 * @author Nicholas Bennett
 */
class Arena private constructor(
        /**
         * Returns the number of breeds specified when this `Arena` was created. The value returned
         * is not necessarily the same as that returned by [.getSurvivingBreeds], which counts
         * only non-extinct breeds.
         */
        val numBreeds: Byte,
        /**
         * Returns the height and width of the (square) terrain.
         */
        val arenaSize: Int, private val rng: Random) {
    private val terrain: Array<ByteArray>
    private val populations: IntArray
    /**
     * Returns the current generation (iteration) of the ecosystem of this `Arena`.
     */
    var generation: Long = 0
        private set
    /**
     * Returns the current count of non-extinct breeds in the ecosystem of this `Arena`.
     */
    var survivingBreeds: Byte = 0
        private set

    /**
     * Initializes the terrain by placing a member of a randomly selected breed in each cell. The
     * breed population sizes are not guaranteed to be exactly equal, though they are asymptotically
     * equal as the terrain size increases. On the other hand, the initial arrangement is guaranteed
     * not to be degeneratethat is, even on the smallest allowed terrain (2 X 2), there will be
     * members of at least 3 breeds placed on the terrain initially.
     */
    fun init() {
        do {
            survivingBreeds = 0
            Arrays.fill(populations, 0)
            for (row in 0 until arenaSize) {
                for (col in 0 until arenaSize) {
                    val breed = rng.nextInt(numBreeds.toInt()).toByte()
                    terrain[row][col] = breed
                    if (0 == populations[breed.toInt()]++) {
                        survivingBreeds++
                    }
                }
            }
        } while (survivingBreeds < 3)
        generation = 0
    }

    /**
     * Advances the ecosystem simulation by a single generation (iteration), as long as it not already
     * in the absorbed state. Each generation consists of just 1 competition, between a randomly
     * selected adjacent pair.
     */
    fun advance() {
        if (survivingBreeds > 1) {
            val challengerRow = rng.nextInt(arenaSize)
            val challengerCol = rng.nextInt(arenaSize)
            val challenger = terrain[challengerRow][challengerCol]
            val direction = Direction.random(rng)
            val defenderRow = wrap(challengerRow + direction.rowOffset)
            val defenderCol = wrap(challengerCol + direction.columnOffset)
            val defender = terrain[defenderRow][defenderCol]
            val comparison = compare(challenger, defender)
            val loserRow: Int
            val loserCol: Int
            val winner: Byte
            val loser: Byte
            if (comparison > 0) {
                winner = challenger
                loser = defender
                loserRow = defenderRow
                loserCol = defenderCol
            } else if (comparison < 0) {
                winner = defender
                loser = challenger
                loserRow = challengerRow
                loserCol = challengerCol
            } else {
                return
            }
            terrain[loserRow][loserCol] = winner
            adjustPopulations(winner, loser)
            generation++
        }
    }

    /**
     * Copies the terrain contents to a 2-dimensional destination array. The destination array must be
     * fully allocated, and must have at least as many rows and columns as the terrain, or an
     * exception will be thrown.
     *
     * @param dest square array of cells to receive a copy of the terrain.
     */
    fun copyTerrain(dest: Array<ByteArray?>) {
        for (row in 0 until arenaSize) {
            System.arraycopy(terrain[row], 0, dest[row], 0, terrain[row].size)
        }
    }

    /**
     * Returns a `boolean` flag indicating whether the ecosystem of this `Arena` is in the
     * *absorbed* state, in which all breeds but 1 are extinct.
     */
    val isAbsorbed: Boolean
        get() = survivingBreeds.toInt() == 1

    private fun adjustPopulations(winner: Byte, loser: Byte) {
        populations[winner.toInt()]++
        if (--populations[loser.toInt()] == 0) {
            survivingBreeds--
        }
    }

    private fun wrap(value: Int): Int {
        var value = value
        value %= arenaSize
        return if (value >= 0) value else value + arenaSize
    }

    private fun compare(cell1: Byte, cell2: Byte): Int {
        val comparison: Int
        comparison = if (cell1 == cell2) {
            0
        } else {
            val distanceClockwise = (cell2 - cell1 + numBreeds) % numBreeds
            2 * distanceClockwise - numBreeds
        }
        return comparison
    }

    /**
     * Implements the *builder pattern* to construct instances of [Arena]. The
     * *fluent* programming style may be used: each non-terminal method returns the current
     * `Builder` instance, so multiple invocations may be chained.
     */
    class Builder {
        private var numBreeds = DEFAULT_NUM_BREEDS
        private var arenaSize = DEFAULT_ARENA_SIZE
        private var rng: Random? = null
        /**
         * Sets the number of breeds placed in the [Arena] returned by [.build].
         *
         * @param numBreeds number of breeds
         * @return this `Builder` instance.
         * @throws IllegalArgumentException if `numBreeds < 3`.
         */
        @Throws(IllegalArgumentException::class)
        fun setNumBreeds(numBreeds: Byte): Builder {
            require(numBreeds >= 3) { ILLEGAL_NUM_BREEDS }
            this.numBreeds = numBreeds
            return this
        }

        /**
         * Sets the size (height &amp; width) of toroidal terrain hosting the [Arena] ecosystem.
         *
         * @param arenaSize size (height &amp; width) of ecosystem terrain.
         * @return this `Builder` instance.
         * @throws IllegalArgumentException if `arenaSize < 2`.
         */
        @Throws(IllegalArgumentException::class)
        fun setArenaSize(arenaSize: Int): Builder {
            require(arenaSize >= 2) { ILLEGAL_ARENA_SIZE }
            this.arenaSize = arenaSize
            return this
        }

        /**
         * Sets the source of randomness used by the [Arena] when initially populating its terrain
         * cells, and in advancing the stochastic GA from one generation to the next.
         *
         * @param rng source of randomness.
         * @return this `Builder` instance.
         */
        fun setRng(rng: Random?): Builder {
            this.rng = rng
            return this
        }

        /**
         * Constructs and returns an [Arena] instance, using the property values set prior to
         * invoking this method (or the default values of properties not explicitly set).
         *
         * @return a new instance of [Arena].
         */
        fun build(): Arena {
            return Arena(numBreeds, arenaSize, (if (rng != null) rng!! else Random()))
        }

        companion object {
            /**
             * Number of breeds in [Arena], if not set via [.setNumBreeds].
             */
            const val DEFAULT_NUM_BREEDS: Byte = 3
            /**
             * Size (height &amp; width) of [Arena], if not set via [.setArenaSize].
             */
            const val DEFAULT_ARENA_SIZE = 50
            private const val ILLEGAL_NUM_BREEDS = "Number of breeds must be > 2"
            private const val ILLEGAL_ARENA_SIZE = "Arena size must be > 1"
        }
    }

    private enum class Direction(val rowOffset: Int, val columnOffset: Int) {
        NORTH(-1, 0), EAST(0, 1), SOUTH(1, 0), WEST(0, -1);

        companion object {
            fun random(rng: Random): Direction {
                val allDirections = values()
                return allDirections[rng.nextInt(allDirections.size)]
            }
        }

    }

    init {
        terrain = Array(arenaSize) { ByteArray(arenaSize) }
        populations = IntArray(numBreeds.toInt())
    }
}