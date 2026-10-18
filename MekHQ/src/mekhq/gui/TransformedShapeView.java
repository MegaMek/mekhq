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

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * A shape seen through a scale-and-translate transform. Unlike {@link AffineTransform#createTransformedShape}, it does
 * not copy the path: the transform is applied while iterating, so drawing a large territory outline into many tiles
 * does not allocate a full copy of it per tile.
 */
final class TransformedShapeView implements Shape {
    private static final int UNSUPPORTED_TYPES = AffineTransform.TYPE_GENERAL_ROTATION
          | AffineTransform.TYPE_QUADRANT_ROTATION | AffineTransform.TYPE_GENERAL_TRANSFORM;

    private final Shape shape;
    private final AffineTransform transform;
    private final AffineTransform inverse;

    private TransformedShapeView(Shape shape, AffineTransform transform, AffineTransform inverse) {
        this.shape = shape;
        this.transform = transform;
        this.inverse = inverse;
    }

    /** Returns a view of {@code shape} under {@code transform}, or a copy when the transform rotates or shears. */
    static Shape of(Shape shape, AffineTransform transform) {
        if ((transform.getType() & UNSUPPORTED_TYPES) != 0) {
            return transform.createTransformedShape(shape);
        }
        try {
            return new TransformedShapeView(shape, new AffineTransform(transform), transform.createInverse());
        } catch (NoninvertibleTransformException exception) {
            return transform.createTransformedShape(shape);
        }
    }

    @Override
    public Rectangle getBounds() {
        return getBounds2D().getBounds();
    }

    @Override
    public Rectangle2D getBounds2D() {
        return transformRectangle(transform, shape.getBounds2D());
    }

    @Override
    public boolean contains(double x, double y) {
        Point2D point = inverse.transform(new Point2D.Double(x, y), null);
        return shape.contains(point);
    }

    @Override
    public boolean contains(Point2D point) {
        return contains(point.getX(), point.getY());
    }

    @Override
    public boolean intersects(double x, double y, double width, double height) {
        if (!(width > 0.0) || !(height > 0.0)) {
            return false;
        }
        return shape.intersects(transformRectangle(inverse, new Rectangle2D.Double(x, y, width, height)));
    }

    @Override
    public boolean intersects(Rectangle2D rectangle) {
        return intersects(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
    }

    @Override
    public boolean contains(double x, double y, double width, double height) {
        if (!(width > 0.0) || !(height > 0.0)) {
            return false;
        }
        return shape.contains(transformRectangle(inverse, new Rectangle2D.Double(x, y, width, height)));
    }

    @Override
    public boolean contains(Rectangle2D rectangle) {
        return contains(rectangle.getX(), rectangle.getY(), rectangle.getWidth(), rectangle.getHeight());
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return shape.getPathIterator(combined(at));
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return shape.getPathIterator(combined(at), flatness);
    }

    private AffineTransform combined(AffineTransform at) {
        if ((at == null) || at.isIdentity()) {
            return transform;
        }
        AffineTransform combined = new AffineTransform(at);
        combined.concatenate(transform);
        return combined;
    }

    /** Exact for the axis-preserving transforms this view accepts. */
    private static Rectangle2D transformRectangle(AffineTransform transform, Rectangle2D rectangle) {
        double[] corners = { rectangle.getMinX(), rectangle.getMinY(), rectangle.getMaxX(), rectangle.getMaxY() };
        transform.transform(corners, 0, corners, 0, 2);
        return new Rectangle2D.Double(Math.min(corners[0], corners[2]), Math.min(corners[1], corners[3]),
              Math.abs(corners[2] - corners[0]), Math.abs(corners[3] - corners[1]));
    }
}
