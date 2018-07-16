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
public class RegionBorder {
    
    private List<Point> border;
    private double boundsX1, boundsX2, boundsY1, boundsY2;
    
    /**
     * Calculates the border polygon for the list of Planets provided.
     * 
     * @param planets A list of planets that define the region.
     */
    public RegionBorder(List<Planet> planets) {
        if (!planets.isEmpty()) {
            boundsX1 = boundsX2 = planets.get(0).getX();
            boundsY1 = boundsY2 = planets.get(0).getY();
            List<Point> points = new ArrayList<>();
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
        // Track how many sides are intersected by a ray along the X axis originating at the point
        // being tested.
        int intersections = 0;
        for (int i = 0; i < border.size(); i++) {
            Point p1 = border.get(i);
            Point p2 = border.get((i + 1) % border.size());
            // If both X coordinates are to the left of the point there will be no intersection.
            if ((p.getX() > p1.getX()) && (p.getX() > p2.getX())) {
                continue;
            }
            // Simplify calculations by always having p1 have the lower Y value.
            if (p1.getY() > p2.getY()) {
                p1 = p2;
                p2 = border.get(i);
            }
            // Special case; if the point has the same Y coordinate as one of the vertices we fudge it a
            // bit to keep from counting it twice.
            double y = p.getY();
            if ((y == p1.getY()) || (y == p2.getY())) {
                y += 0.001;
            }
            // The ray can only intersect the segment if the Y coordinate lies between the two end points.
            if ((y < p1.getY()) || (y > p2.getY())) {
                continue;
            }
            // If the point being tested is to the left of both end points of the edge, the ray will intersect.
            // If it is to the left of one of them it lies to the left if the slope of p1->p is greater
            // than the slope of p1->p2.
            if ((p.getX() < p1.getX()) && (p.getX() < p2.getX())) {
                intersections++;
            } else if ((y - p1.getY()) / (p.getX() - p1.getX())
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
    List<Point> performGrahamScan(List<Point> points) {
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
            if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
                return false;
            if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
                return false;
            return true;
        }
    }
    
}
