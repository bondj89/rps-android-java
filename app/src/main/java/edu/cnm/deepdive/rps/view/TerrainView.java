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
package edu.cnm.deepdive.rps.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import edu.cnm.deepdive.rps.model.Arena;

/**
 * {@link View} subclass that renders the cells on the terrain of an {@link Arena}. Note that this
 * is not a {@link android.view.SurfaceView}; updates are being driven entirely by updates to the
 * underlying LiveData; if these occur too frequently, it would probably make sense to implement a
 * {@link android.view.SurfaceView} subclass instead.
 *
 * @author Nicholas Bennett
 */
public class TerrainView extends View {

  private static final float MAX_HUE = 360;
  private static final float SATURATION = 1;
  private static final float BRIGHTNESS = 0.85f;

  private Canvas canvas;
  private Bitmap bitmap;
  private Arena arena;
  private byte[][] terrain;
  private int[] breedColors;
  private Paint paint;
  private boolean measured;
  private Rect source = new Rect();
  private Rect dest = new Rect();
  private final Object lock = new Object();
  private Updater updater;

  {
    setWillNotDraw(false);
    paint = new Paint();
    paint.setStyle(Paint.Style.FILL);
  }

  /**
   * Initializes by chaining to {@link View#View(Context)}.
   *
   * @param context
   */
  public TerrainView(Context context) {
    super(context);
  }

  /**
   * Initializes by chaining to {@link View#View(Context, AttributeSet)}.
   */
  public TerrainView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  /**
   * Initializes by chaining to {@link View#View(Context, AttributeSet, int)}.
   */
  public TerrainView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  /**
   * Initializes by chaining to {@link View#View(Context, AttributeSet, int, int)}.
   */
  public TerrainView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
  }

  /**
   * Returns dimensions based on the larger of this view's suggested height and width, so that the
   * content is square. For this to be the appropriate choice, this view should be contained within
   * a {@link android.widget.ScrollView}, with its width set to {@code match_parent} and its height
   * set to {@code wrap_content}; or within a {@link android.widget.HorizontalScrollView}, with its
   * width set to {@code wrap_content} and its height set to {@code match_parent}.
   *
   * @param widthMeasureSpec  specification control value.
   * @param heightMeasureSpec specification control value.
   */
  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    measured = false;
    int width = getSuggestedMinimumWidth();
    int height = getSuggestedMinimumHeight();
    width = resolveSizeAndState(getPaddingLeft() + getPaddingRight() + width, widthMeasureSpec, 0);
    height = resolveSizeAndState(getPaddingTop() + getPaddingBottom() + height, heightMeasureSpec,
        0);
    int size = Math.max(width, height);
    setMeasuredDimension(size, size);
  }

  /**
   * Performs layout on all child elements (none), and creates a {@link Bitmap} to fit the specified
   * dimensions.
   */
  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    measured = true;
    dest.set(left, top, right, bottom);
    updateBitmap();
  }

  /**
   * Renders the contents of the {@link Arena Arena's} terrain.
   *
   * @param canvas rendering target.
   */
  @Override
  protected void onDraw(Canvas canvas) {
    if (bitmap != null) {
      dest.set(0, 0, getWidth(), getHeight());
      canvas.drawBitmap(bitmap, source, dest, null);
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (updater != null) {
      updater.setRunning(false);
    }
    updater = new Updater();
    updater.start();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (updater != null) {
      updater.setRunning(false);
      updater = null;
    }
  }

  /**
   * Specifices the {@link Arena} instance to be rendered by this view. In general, this will most
   * simply be invoked via data binding in the layout XML.
   *
   * @param arena instance to render.
   */
  public void setArena(Arena arena) {
    this.arena = arena;
    if (arena != null) {
      int numBreeds = arena.getNumBreeds();
      int size = arena.getArenaSize();
      terrain = new byte[size][size];
      bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
      source.set(0, 0, size, size);
      float[] hsv = {0, SATURATION, BRIGHTNESS};
      float hueInterval = MAX_HUE / numBreeds;
      breedColors = new int[numBreeds];
      for (int i = 0; i < numBreeds; i++) {
        breedColors[i] = Color.HSVToColor(hsv);
        hsv[0] += hueInterval;
      }
    }
  }

  /**
   * Updates the current generation count, triggering a display refresh. Without invoking this
   * method, the cell terrain rendering will not be updated; however, if data binding is used in the
   * layout XML, this can happen automatically.
   *
   * @param generation number of generations (iterations) completed in the {@link Arena}
   *                   simulation.
   */
  public void setGeneration(long generation) {
    if (updater != null) {
      updater.setGeneration(generation);
    }
  }

  private void updateBitmap() {
    if (measured && terrain != null) {
      if (bitmap != null) {
        arena.copyTerrain(terrain);
        for (int row = 0; row < terrain.length; row++) {
          for (int col = 0; col < terrain[row].length; col++) {
            bitmap.setPixel(col, row, breedColors[terrain[row][col]]);
          }
        }
      }
    }
  }

  private class Updater extends Thread {

    private transient boolean running = true;
    private transient long generation = 0;

    @Override
    public void run() {
      long generation = -1;
      while (running) {
        if (this.generation == 0 || this.generation > generation) {
          generation = this.generation;
          updateBitmap();
          if (generation == 0) {
            postInvalidate();
          }
        }
        try {
          sleep(1);
        } catch (InterruptedException expected) {
          // Ignore innocuous exception.
        }
      }
    }

    public void setRunning(boolean running) {
      this.running = running;
    }

    public void setGeneration(long generation) {
      this.generation = generation;
    }

  }

}
