/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

import org.jetbrains.skia.Canvas;
import org.jetbrains.skia.FilterMode;
import org.jetbrains.skia.FilterTileMode;
import org.jetbrains.skia.Paint;
import org.jetbrains.skia.Picture;
import org.jetbrains.skia.PictureRecorder;
import org.jetbrains.skia.Rect;
import org.jetbrains.skia.RuntimeEffect;
import org.jetbrains.skia.RuntimeShaderBuilder;
import org.jetbrains.skia.Shader;

final class SkikoMotionLayer implements AutoCloseable {
    private static final double MAX_PIXELS = 2048.0 * 2048;
    private static final Method PICTURE_SHADER;
    private static final Method RUNTIME_SHADER;

    static {
        try {
            PICTURE_SHADER = Picture.class.getMethod("makeShader-juH9G2M", FilterTileMode.class,
                  FilterTileMode.class, FilterMode.class, float[].class, Rect.class);
            RUNTIME_SHADER = RuntimeShaderBuilder.class.getMethod("makeShader-mTDiogE", float[].class);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private Picture picture;
    private Shader shader;
    private RuntimeEffect effect;
    private ExperimentalMapView.ViewState view;
    private int width;
    private int height;
    private int margin;
    private float contentScale;
    private float rasterScale;
    private int builds;
    private int hits;

    int builds() {
        return builds;
    }

    int hits() {
        return hits;
    }

    boolean active() {
        return picture != null;
    }

    String info() {
        return "builds=" + builds + ",hits=" + hits + ",active=" + active()
              + ",rasterScale=" + rasterScale + ",size=" + width + "x" + height;
    }

    void draw(Canvas canvas, ExperimentalMapView.ViewState requested, int requestedWidth, int requestedHeight,
          float requestedContentScale, int requestedMargin, int background, BiConsumer<Canvas, Integer> renderer) {
        float ratio = view == null ? 1 : (float) (requested.scale() / view.scale());
        float originX = view == null ? 0 : requestedWidth / 2f * (1 - ratio)
              + (float) ((view.centerX() - requested.centerX()) * requested.scale()) - margin * ratio;
        float originY = view == null ? 0 : requestedHeight / 2f * (1 - ratio)
              + (float) ((requested.centerY() - view.centerY()) * requested.scale()) - margin * ratio;
        if (picture == null || width != requestedWidth || height != requestedHeight
              || contentScale != requestedContentScale || margin != requestedMargin
              || ratio < 0.75f || ratio > 1.5f || originX > -2 || originY > -2
              || originX + (width + 2f * margin) * ratio < requestedWidth + 2
              || originY + (height + 2f * margin) * ratio < requestedHeight + 2) {
            close();
            width = requestedWidth;
            height = requestedHeight;
            margin = requestedMargin;
            contentScale = requestedContentScale;
            rasterScale = (float) Math.min(contentScale,
                  Math.sqrt(MAX_PIXELS / ((width + 2.0 * margin) * (height + 2.0 * margin))));
            try (PictureRecorder recorder = new PictureRecorder()) {
                Canvas recording = recorder.beginRecording(Rect.makeXYWH(0, 0,
                      (width + 2f * margin) * rasterScale, (height + 2f * margin) * rasterScale), null);
                recording.clear(background);
                recording.scale(rasterScale, rasterScale);
                recording.translate(margin, margin);
                renderer.accept(recording, margin);
                picture = recorder.finishRecordingAsPicture();
                shader = (Shader) PICTURE_SHADER.invoke(picture, FilterTileMode.CLAMP, FilterTileMode.CLAMP,
                      FilterMode.LINEAR, null, null);
                effect = RuntimeEffect.Companion.makeForShader("""
                      uniform shader tile;
                      uniform float2 origin;
                      uniform float sampleScale;
                      half4 main(float2 position) {
                          return tile.eval((position - origin) * sampleScale);
                      }
                      """);
            } catch (ReflectiveOperationException | RuntimeException exception) {
                close();
                throw new IllegalStateException("Unable to record native motion layer", exception);
            }
            view = requested;
            builds++;
            ratio = 1;
            originX = -margin;
            originY = -margin;
        } else {
            hits++;
        }
        try (RuntimeShaderBuilder builder = new RuntimeShaderBuilder(effect); Paint paint = new Paint()) {
            builder.child("tile", shader);
            builder.uniform("origin", originX * contentScale, originY * contentScale);
            builder.uniform("sampleScale", rasterScale / (ratio * contentScale));
            try (Shader transformed = (Shader) RUNTIME_SHADER.invoke(builder, (Object) null)) {
                paint.setShader(transformed);
                canvas.save();
                try {
                    canvas.scale(1 / contentScale, 1 / contentScale);
                    canvas.drawRect(Rect.makeXYWH(0, 0, width * contentScale, height * contentScale), paint);
                } finally {
                    canvas.restore();
                }
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to sample native motion layer", exception);
        }
    }

    @Override
    public void close() {
        if (effect != null) {
            effect.close();
            effect = null;
        }
        if (shader != null) {
            shader.close();
            shader = null;
        }
        if (picture != null) {
            picture.close();
            picture = null;
        }
        view = null;
    }
}