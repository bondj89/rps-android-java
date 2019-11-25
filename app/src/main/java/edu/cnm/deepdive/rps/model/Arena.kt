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
package edu.cnm.deepdive.rps.model;

import java.util.Arrays;
import java.util.Random;

/**
 * Models a non-transitive competitive ecosystem. With 3 breeds, the resulting ecosystem resembles
 * an evolutionary game of Rock-Paper-Scissors. While it can be a practical challenge to enumerate
 * the relationship of a non-transitive system with many more breeds than 3, it is in fact very
 * straightforward to simulate such a system&mdash;at least in an idealized form: dominance
 * relationships between the breeds can be envisioned by arranging them in regular intervals around
 * a circle; any given breed dominates those breeds that can be reached via a shorter path in the
 * counter-clockwise direction than in the clockwise direction.
 * <p>This ecosystem model is a stochastic CA, where all cells survive unchanged to the next
 * generation&mdash;with the possible exception of the cell chosen at random to be the "challenger",
 * and that cell's randomly selected neighbor (the "defender"). One or both (in the event of a tie)
 * of these 2 cells survive to the next generation, while the loser (if there is not a tie) is
 * replaced by a copy of the winner.</p>
 * <p>The size of the terrain where the ecosystem resides is square, and the topology is toroidal:
 * cells on the left and right edges of any given row are considered to be neighbors, as are cells
 * at the top and bottom of any given column.</p>
 *
 * @author Nicholas Bennett
 */
public class Arena {

  private final byte numBreeds;
  private final int arenaSize;
  private final Random rng;
  private final byte[][] terrain;
  private final int[] populations;
  private long generation;
  private byte survivingBreeds;

  private Arena(byte numBreeds, int arenaSize, Random rng) {
    this.numBreeds = numBreeds;
    this.arenaSize = arenaSize;
    this.rng = rng;
    terrain = new byte[arenaSize][arenaSize];
    populations = new int[numBreeds];
  }

  /**
   * Initializes the terrain by placing a member of a randomly selected breed in each cell. The
   * breed population sizes are not guaranteed to be exactly equal, though they are asymptotically
   * equal as the terrain size increases. On the other hand, the initial arrangement is guaranteed
   * not to be degenerate&mdash;that is, even on the smallest allowed terrain (2 X 2), there will be
   * members of at least 3 breeds placed on the terrain initially.
   */
  public void init() {
    do {
      survivingBreeds = 0;
      Arrays.fill(populations, 0);
      for (int row = 0; row < arenaSize; row++) {
        for (int col = 0; col < arenaSize; col++) {
          byte breed = (byte) rng.nextInt(numBreeds);
          terrain[row][col] = breed;
          if (0 == populations[breed]++) {
            survivingBreeds++;
          }
        }
      }
    }
    while (survivingBreeds < 3);
    generation = 0;
  }

  /**
   * Advances the ecosystem simulation by a single generation (iteration), as long as it not already
   * in the absorbed state. Each generation consists of just 1 competition, between a randomly
   * selected adjacent pair.
   */
  public void advance() {
    if (survivingBreeds > 1) {
      int challengerRow = rng.nextInt(arenaSize);
      int challengerCol = rng.nextInt(arenaSize);
      byte challenger = terrain[challengerRow][challengerCol];
      Direction direction = Direction.random(rng);
      int defenderRow = wrap(challengerRow + direction.getRowOffset());
      int defenderCol = wrap(challengerCol + direction.getColumnOffset());
      byte defender = terrain[defenderRow][defenderCol];
      int comparison = compare(challenger, defender);
      int loserRow;
      int loserCol;
      byte winner;
      byte loser;
      if (comparison > 0) {
        winner = challenger;
        loser = defender;
        loserRow = defenderRow;
        loserCol = defenderCol;
      } else if (comparison < 0) {
        winner = defender;
        loser = challenger;
        loserRow = challengerRow;
        loserCol = challengerCol;
      } else {
        return;
      }
      terrain[loserRow][loserCol] = winner;
      adjustPopulations(winner, loser);
      generation++;
    }
  }

  /**
   * Copies the terrain contents to a 2-dimensional destination array. The destination array must be
   * fully allocated, and must have at least as many rows and columns as the terrain, or an
   * exception will be thrown.
   *
   * @param dest square array of cells to receive a copy of the terrain.
   */
  public void copyTerrain(byte[][] dest) {
    for (int row = 0; row < arenaSize; row++) {
      System.arraycopy(terrain[row], 0, dest[row], 0, terrain[row].length);
    }
  }

  /**
   * Returns the number of breeds specified when this {@code Arena} was created. The value returned
   * is not necessarily the same as that returned by {@link #getSurvivingBreeds()}, which counts
   * only non-extinct breeds.
   */
  public byte getNumBreeds() {
    return numBreeds;
  }

  /**
   * Returns the height and width of the (square) terrain.
   */
  public int getArenaSize() {
    return arenaSize;
  }

  /**
   * Returns the current count of non-extinct breeds in the ecosystem of this {@code Arena}.
   */
  public byte getSurvivingBreeds() {
    return survivingBreeds;
  }

  /**
   * Returns the current generation (iteration) of the ecosystem of this {@code Arena}.
   */
  public long getGeneration() {
    return generation;
  }

  /**
   * Returns a {@code boolean} flag indicating whether the ecosystem of this {@code Arena} is in the
   * <em>absorbed</em> state, in which all breeds but 1 are extinct.
   */
  public boolean isAbsorbed() {
    return survivingBreeds == 1;
  }

  private void adjustPopulations(byte winner, byte loser) {
    populations[winner]++;
    if (--populations[loser] == 0) {
      survivingBreeds--;
    }
  }

  private int wrap(int value) {
    value %= arenaSize;
    return (value >= 0) ? value : value + arenaSize;
  }

  private int compare(byte cell1, byte cell2) {
    int comparison;
    if (cell1 == cell2) {
      comparison = 0;
    } else {
      int distanceClockwise = (cell2 - cell1 + numBreeds) % numBreeds;
      comparison = 2 * distanceClockwise - numBreeds;
    }
    return comparison;
  }

  /**
   * Implements the <em>builder pattern</em> to construct instances of {@link Arena}. The
   * <em>fluent</em> programming style may be used: each non-terminal method returns the current
   * {@code Builder} instance, so multiple invocations may be chained.
   */
  public static class Builder {

    /**
     * Number of breeds in {@link Arena}, if not set via {@link #setNumBreeds(byte)}.
     */
    public static final byte DEFAULT_NUM_BREEDS = 3;
    /**
     * Size (height &amp; width) of {@link Arena}, if not set via {@link #setArenaSize(int)}.
     */
    public static final int DEFAULT_ARENA_SIZE = 50;
    private static final String ILLEGAL_NUM_BREEDS = "Number of breeds must be > 2";
    private static final String ILLEGAL_ARENA_SIZE = "Arena size must be > 1";

    private byte numBreeds = DEFAULT_NUM_BREEDS;
    private int arenaSize = DEFAULT_ARENA_SIZE;
    private Random rng;

    /**
     * Sets the number of breeds placed in the {@link Arena} returned by {@link #build()}.
     *
     * @param numBreeds number of breeds
     * @return this {@code Builder} instance.
     * @throws IllegalArgumentException if {@code numBreeds < 3}.
     */
    public Builder setNumBreeds(byte numBreeds) throws IllegalArgumentException {
      if (numBreeds < 3) {
        throw new IllegalArgumentException(ILLEGAL_NUM_BREEDS);
      }
      this.numBreeds = numBreeds;
      return this;
    }

    /**
     * Sets the size (height &amp; width) of toroidal terrain hosting the {@link Arena} ecosystem.
     *
     * @param arenaSize size (height &amp; width) of ecosystem terrain.
     * @return this {@code Builder} instance.
     * @throws IllegalArgumentException if {@code arenaSize < 2}.
     */
    public Builder setArenaSize(int arenaSize) throws IllegalArgumentException {
      if (arenaSize < 2) {
        throw new IllegalArgumentException(ILLEGAL_ARENA_SIZE);
      }
      this.arenaSize = arenaSize;
      return this;
    }

    /**
     * Sets the source of randomness used by the {@link Arena} when initially populating its terrain
     * cells, and in advancing the stochastic GA from one generation to the next.
     *
     * @param rng source of randomness.
     * @return this {@code Builder} instance.
     */
    public Builder setRng(Random rng) {
      this.rng = rng;
      return this;
    }

    /**
     * Constructs and returns an {@link Arena} instance, using the property values set prior to
     * invoking this method (or the default values of properties not explicitly set).
     *
     * @return a new instance of {@link Arena}.
     */
    public Arena build() {
      return new Arena(numBreeds, arenaSize, (rng != null) ? rng : new Random());
    }

  }

  private enum Direction {

    NORTH(-1, 0),
    EAST(0, 1),
    SOUTH(1, 0),
    WEST(0, -1);

    private final int rowOffset;
    private final int columnOffset;

    Direction(int rowOffset, int columnOffset) {
      this.rowOffset = rowOffset;
      this.columnOffset = columnOffset;
    }

    private static Direction random(Random rng) {
      Direction[] allDirections = Direction.values();
      return allDirections[rng.nextInt(allDirections.length)];
    }

    public int getRowOffset() {
      return rowOffset;
    }

    public int getColumnOffset() {
      return columnOffset;
    }

  }

}
