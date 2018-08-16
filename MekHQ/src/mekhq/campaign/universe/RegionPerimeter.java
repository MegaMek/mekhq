/*
 * Copyright (c) 2018 - The MegaMek Team
 * 
 * This file is part of MekHQ.
 * 
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.universe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Given a list of planets, uses a convex hull algorithm to select the planets that form a polygon that
 * completely enclose the space.
 * 
 * @author Neoancient
 *
 */
public class RegionPerimeter {
    
    // Margin for coordinates to be considered equal
    static final double EPSILON = 0.001;
    
    private List<Point> border;
    private double boundsX1, boundsY1, boundsX2, boundsY2;
    
    /**
     * Calculates the border polygon for the list of Planets provided.
     * 
     * @param planets A list of planets that define the region.
     */
    public RegionPerimeter(Collection<Planet> planets) {
        if (!planets.isEmpty()) {
            List<Point> points = new ArrayList<>();
            boundsX1 = Double.MAX_VALUE;
            boundsY1 = Double.MAX_VALUE;
            boundsX2 = Double.MIN_VALUE;
            boundsY2 = Double.MIN_VALUE;
            for (Planet planet : planets) {
                Point p = new Point(planet.getX(), planet.getY());
                boundsX1 = Math.min(boundsX1, p.getX());
                boundsX2 = Math.max(boundsX2, p.getX());
                boundsY1 = Math.min(boundsY1, p.getY());
                boundsY2 = Math.max(boundsY2, p.getY());
                points.add(p);
            }
            border = performGrahamScan(points);
        } else {
            border = new ArrayList<>();
        }
    }
    
    /**
     * @return A list of points that mark the vertices of a convex polygon that contains the entire region
     */
    public List<Point> getVertices() {
        return Collections.unmodifiableList(border);
    }
    
    /**
     * Tests whether a given point is inside the border region using a ray casting algorithm.
     * 
     * @param p The Point to test
     * @return  Whether the point is contained within the convex polygon describing the border.
     */
    public boolean isInsideRegion(Point p) {
        return isInsideRegion(p.getX(), p.getY(), border);
    }
    
    /**
     * Tests whether a given point is inside a convex polygon using a ray casting algorithm.
     * 
     * @param x      The x coordinate of the point to test
     * @param y      The y coordinate of the point to test
     * @param region An ordered list of vertices of a convex polygon
     * @return       Whether the point is contained within the polygon.
     */
    public static boolean isInsideRegion(double x, double y, List<Point> region) {
        // Track how many sides are intersected by a ray along the X axis originating at the point
        // being tested.
        int intersections = 0;
        for (int i = 0; i < region.size(); i++) {
            Point p1 = region.get(i);
            Point p2 = region.get((i + 1) % region.size());
            // If both X coordinates are to the left of the point there will be no intersection.
            if ((x > p1.getX()) && (x > p2.getX())) {
                continue;
            }
            // Simplify calculations by always having p1 have the lower Y value.
            if (p1.getY() > p2.getY()) {
                p1 = p2;
                p2 = region.get(i);
            }
            // Special case; if the point has the same Y coordinate as one of the vertices we fudge it a
            // bit to keep from counting it twice.
            if ((y == p1.getY()) || (y == p2.getY())) {
                y += EPSILON;
            }
            // The ray can only intersect the segment if the Y coordinate lies between the two end points.
            if ((y < p1.getY()) || (y > p2.getY())) {
                continue;
            }
            // If the point being tested is to the left of both end points of the edge, the ray will intersect.
            // If it is to the left of one of them it lies to the left if the slope of p1->p is greater
            // than the slope of p1->p2.
            if ((x < p1.getX()) && (x < p2.getX())) {
                intersections++;
            } else if ((y - p1.getY()) / (x - p1.getX())
                    > (p2.getY() - p1.getY()) / (p2.getX() - p1.getX())) {
                intersections++;
            }
            // Since we are only dealing with convex polygons, two intersections means that the point
            // lies outside the polygon to the left and we can short-circuit.
            if (intersections == 2) {
                return false;
            }
        }
        return intersections == 1;
    }
    
    /**
     * Test whether the coordinates are in the region's bounding rectangle. Used for filtering likely values.
     * @param x
     * @param y
     * @return  True if the point is contained within the bounding rectangle.
     */
    public boolean isInsideBoundingBox(double x, double y) {
        return (x > boundsX1) && (x < boundsX2)
                && (y > boundsY1) && (y < boundsY2);
    }
    
    /**
     * Test whether the point in the region's bounding rectangle. Used for filtering likely values.
     * @param p
     * @return  True if the point is contained within the bounding rectangle.
     */
    public boolean isInsideBoundingBox(Point p) {
        return isInsideBoundingBox(p.getX(), p.getY());
    }
    
    /**
     * Test whether the planet in the region's bounding rectangle. Used for filtering likely values.
     * @param p
     * @return  True if the point is contained within the bounding rectangle.
     */
    public boolean isInsideBoundingBox(Planet p) {
        return isInsideBoundingBox(p.getX(), p.getY());
    }
    
    /**
     * Calculates a convex polygon that surrounds a set of points with a minimum border width.
     * 
     * @param region  The region to surround
     * @param padding The size of the border to add around the inner region
     * @return        A list of vertices of a convex polygon
     */
    public static List<Point> getPaddedRegion(List<Point> region, double padding) {
        if (padding > 0) {
            List<Point> retVal = new ArrayList<>();
            for (Point p : region) {
                retVal.add(new Point(p.getX() - padding, p.getY() - padding));
                retVal.add(new Point(p.getX() + padding, p.getY() - padding));
                retVal.add(new Point(p.getX() - padding, p.getY() + padding));
                retVal.add(new Point(p.getX() + padding, p.getY() + padding));
            }
            return performGrahamScan(retVal);
        } else {
            return region;
        }
    }
    
    /**
     * Calculates the intersection between this region and another with the possibility of setting the
     * width of a border around each.
     * 
     * @param other    The other intersecting region
     * @param padding  If > 0, adds extra space of the given width around each region before
     *                 calculating the intersection.
     * @return         A list of the vertices of the polygon around the intersection.
     */
    public List<Point> intersection(RegionPerimeter other, double padding) {
        return intersection(getPaddedRegion(border, padding),
                getPaddedRegion(other.border, padding));
    }

    /**
     * Finds the intersection of two polygons using Sutherland-Hodgman polygon clipping.
     * 
     * @param subject A collection of points defining the first polygon.
     * @param clipper A collection of points defining the second polygon. This one must be convex.
     * @return The set of vertices defining the intersection between the two polygons.
     */
    public static List<Point> intersection(List<Point> subject, List<Point> clipper) {
        /* Each edge of the clipping polygon is extended into a line, and each vertex on the subject polygon
         * that lies on the side of the line that is inside the clipping polygon is added to the output list.
         * For each edge of the subject that has one edge inside and one outside the point where the two
         * edges intersect is added to the output list. Each iteration around the edges of the clipping polygon
         * starts with the results of the previous iteration as the new subject. */
        int clipperSize = clipper.size();
        List<Point> output = new ArrayList<>(subject);
        for (int i = 0; i < clipperSize; i++) {
            int outputSize = output.size();
            
            List<Point> input = output;
            output = new ArrayList<>(outputSize);
            
            Point a = clipper.get((i + clipperSize - 1) % clipperSize);
            Point b = clipper.get(i);
            
            for (int j = 0; j < outputSize; j++) {
                 Point p = input.get((j + outputSize - 1) % outputSize);
                 Point q = input.get(j);
                 
                 if (vectorCrossProduct(a, b, q) > 0) {
                     if (vectorCrossProduct(a, b, p) <= 0) {
                         output.add(lineIntersection(a, b, p, q));
                     }
                     output.add(q);
                 } else if (vectorCrossProduct(a, b, p) > 0) {
                     output.add(lineIntersection(a, b, p, q));
                 }
            }
            
        }
        
        return output;
    }

    /**
     * Find the point where two lines intersect in a plane. Does not test for parallel or for distinct
     * points defining a line.
     * 
     * @param a A point on the first line
     * @param b Another point on the first line
     * @param p A point on the second line
     * @param q Another point on the second line
     * @return  The intersection
     */
    private static Point lineIntersection(Point a, Point b, Point p, Point q) {
        double a1 = b.getY() - a.getY();
        double b1 = a.getX() - b.getX();
        double c1 = a1 * a.getX() + b1 * a.getY();
 
        double a2 = q.getY() - p.getY();
        double b2 = p.getX() - q.getX();
        double c2 = a2 * p.getX() + b2 * p.getY();
 
        double determinant = a1 * b2 - a2 * b1;
        double x = (b2 * c1 - b1 * c2) / determinant;
        double y = (a1 * c2 - a2 * c1) / determinant;
 
        return new Point(x, y);
    }

    /**
     * Method to compute the convex hull of a list of points. Starts by determining the point
     * with the lowest y coordinate, selecting the lowest x coordinate if there is more than one that
     * shares the lowest y. The remaining points are sorted according to the angle made by the X axis
     * and a line from the reference point. Points are then added to a stack in the sorted order. If
     * adding a point would make a concavity in the polygon, previous points are popped off the
     * stack until adding the current point makes a convex angle.
     * 
     * @param points  A list of points in the region
     * @return        A list of points whose coordinates define a convex polygon surrounding
     *                all the points in the list.
     */
    static List<Point> performGrahamScan(List<Point> points) {
        Optional<Point> start = points.stream().min(leastYSorter);
        if (!start.isPresent()) {
            return Collections.emptyList();
        }
        final Point origin = start.get();
        Comparator<Point> pointSorter = new GrahamScanPointSorter(origin);
        List<Point> sortedPoints = points.stream()
                .filter(p -> !p.equals(origin))
                .sorted(pointSorter)
                .collect(Collectors.toList());
        // Check for a special case: if there are more than two points that have the same least Y,
        // remove all but the right-most to prevent popping too many values off the stack in the next
        // step.
        while ((sortedPoints.size() > 1) && (sortedPoints.get(0).getY() == sortedPoints.get(1).getY())) {
            if (sortedPoints.get(0).getX() > sortedPoints.get(1).getX()) {
                sortedPoints.remove(1);
            } else {
                sortedPoints.remove(0);
            }
        }
        
        LinkedList<Point> stack = new LinkedList<>();
        stack.add(origin);
        if (sortedPoints.size() > 0) {
            stack.add(sortedPoints.get(0));
        }
        if (sortedPoints.size() > 1) {
            stack.add(sortedPoints.get(1));
        }
        for (int i = 2; i < sortedPoints.size(); i++) {
            while (vectorCrossProduct(stack.get(stack.size() - 2),
                    stack.getLast(), sortedPoints.get(i)) <= 0) {
                stack.removeLast();
            }
            stack.add(sortedPoints.get(i));
        }
        return stack;
    }

    /**
     * Computes the cross product of two vectors from p1 -> p2 and p1 -> p3. This can
     * be used to determine if a path from the origin to p1 to p2 is clockwise ( < 0 ), anticlockwise
     * ( > 0 ) or a straight line ( 0 ).
     * 
     * @param p1 First point in sequence
     * @param p2 Second point in sequence
     * @param p3 Third point in sequence
     * @return   The cross product of the vectors p1->p2 and p1->p3
     */
    static double vectorCrossProduct(Point p1, Point p2, Point p3) {
        return (p2.getX() - p1.getX()) * (p3.getY() - p1.getY())
                - (p2.getY() - p1.getY()) * (p3.getX() - p1.getX());
    }

    /**
     * Sorts points from lowest Y coordinate to highest. If Y coordinates are equal, sorts
     * from lowest to highest X.
     */
    final static Comparator<Point> leastYSorter = (p1, p2) -> {
        int retVal = Double.compare(p1.getY(), p2.getY());
        if (retVal == 0) {
            return Double.compare(p1.getX(), p2.getX());
        }
        return retVal;
    };

    /**
     * Used to sort planets according to their angle from a third planet
     */
    static class GrahamScanPointSorter implements Comparator<Point> {
        private final Point origin;
        
        GrahamScanPointSorter(Point origin) {
            this.origin = origin;
        }

        @Override
        public int compare(Point p1, Point p2) {
            double cp = vectorCrossProduct(origin, p1, p2);
            if (cp > 0) {
                return -1;
            } else if (cp < 0) {
                return 1;
            } else {
                return 0;
            }
        }
    }
    
    /**
     * Utility class to track x and y values of a planar coordinate.
     *
     */
    public static class Point {
        private final double x;
        private final double y;
        
        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        public double getX() {
            return x;
        }
        
        public double getY() {
            return y;
        }
        
        @Override
        public String toString() {
            return String.format("(%3.2f,%3.2f)", x, y);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            long temp;
            temp = Double.doubleToLongBits(x);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(y);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Point other = (Point) obj;
            return (Math.abs(x - other.x) < EPSILON) && (Math.abs(y - other.y) < EPSILON); 
        }
    }
    
}
