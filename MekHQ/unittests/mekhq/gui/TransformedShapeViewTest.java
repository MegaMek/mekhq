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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class TransformedShapeViewTest {
    private static final Area SHAPE = createShape();

    private static Area createShape() {
        Area area = new Area(new Rectangle2D.Double(-3.0, 1.0, 8.0, 5.0));
        area.add(new Area(new Ellipse2D.Double(2.0, -4.0, 6.0, 6.0)));
        return area;
    }

    private static AffineTransform mapToScreen() {
        AffineTransform transform = new AffineTransform();
        transform.translate(140.0, 90.0);
        transform.scale(3.5, -3.5);
        transform.translate(-1.25, 2.5);
        return transform;
    }

    @Test
    void viewMatchesACopiedTransformedShape() {
        AffineTransform transform = mapToScreen();
        Shape copy = transform.createTransformedShape(SHAPE);
        Shape view = TransformedShapeView.of(SHAPE, transform);

        assertInstanceOf(TransformedShapeView.class, view);
        assertEquals(segments(copy, null), segments(view, null));
        AffineTransform device = AffineTransform.getScaleInstance(1.75, 1.75);
        device.translate(-12.0, 7.0);
        assertEquals(segments(copy, device), segments(view, device));
        Rectangle2D copyBounds = copy.getBounds2D();
        Rectangle2D viewBounds = view.getBounds2D();
        assertEquals(copyBounds.getMinX(), viewBounds.getMinX(), 1e-9);
        assertEquals(copyBounds.getMinY(), viewBounds.getMinY(), 1e-9);
        assertEquals(copyBounds.getMaxX(), viewBounds.getMaxX(), 1e-9);
        assertEquals(copyBounds.getMaxY(), viewBounds.getMaxY(), 1e-9);
        for (double[] point : new double[][] { { 140.0, 90.0 }, { 150.0, 70.0 }, { 20.0, 20.0 } }) {
            assertEquals(copy.contains(point[0], point[1]), view.contains(point[0], point[1]));
        }
        assertTrue(view.intersects(130.0, 60.0, 20.0, 20.0));
        assertFalse(view.intersects(0.0, 0.0, 5.0, 5.0));
    }

    @Test
    void viewIsUnaffectedByLaterChangesToTheTransform() {
        AffineTransform transform = mapToScreen();
        Shape view = TransformedShapeView.of(SHAPE, transform);
        List<String> before = segments(view, null);
        transform.translate(50.0, 50.0);

        assertEquals(before, segments(view, null));
    }

    @Test
    void rotationFallsBackToACopy() {
        AffineTransform rotation = AffineTransform.getRotateInstance(0.3);
        Shape shape = TransformedShapeView.of(SHAPE, rotation);

        assertFalse(shape instanceof TransformedShapeView);
        assertArrayEquals(segments(rotation.createTransformedShape(SHAPE), null).toArray(),
              segments(shape, null).toArray());
    }

    @Test
    void rectangleQueriesRejectNonPositiveDimensions() {
        Area square = new Area(new Rectangle2D.Double(0.0, 0.0, 10.0, 10.0));
        Shape view = TransformedShapeView.of(square, AffineTransform.getScaleInstance(1.0, -1.0));
        Shape copy = AffineTransform.getScaleInstance(1.0, -1.0).createTransformedShape(square);

        for (double[] query : new double[][] { { 8.0, -3.0, -6.0, 2.0 }, { 2.0, -9.0, 2.0, -6.0 },
              { 2.0, -8.0, 0.0, 3.0 }, { 2.0, -8.0, Double.NaN, 3.0 } }) {
            assertEquals(copy.contains(query[0], query[1], query[2], query[3]),
                  view.contains(query[0], query[1], query[2], query[3]));
            assertEquals(copy.intersects(query[0], query[1], query[2], query[3]),
                  view.intersects(query[0], query[1], query[2], query[3]));
            assertFalse(view.contains(query[0], query[1], query[2], query[3]));
            assertFalse(view.intersects(query[0], query[1], query[2], query[3]));
        }
        assertTrue(view.contains(2.0, -8.0, 6.0, 6.0));
        assertTrue(view.intersects(8.0, -3.0, 6.0, 2.0));
    }

    private static List<String> segments(Shape shape, AffineTransform transform) {
        List<String> segments = new ArrayList<>();
        double[] coordinates = new double[6];
        for (PathIterator iterator = shape.getPathIterator(transform); !iterator.isDone(); iterator.next()) {
            int type = iterator.currentSegment(coordinates);
            StringBuilder segment = new StringBuilder().append(type);
            for (double coordinate : coordinates) {
                segment.append(String.format(java.util.Locale.ROOT, " %.6f", coordinate));
            }
            segments.add(segment.toString());
        }
        return segments;
    }
}
