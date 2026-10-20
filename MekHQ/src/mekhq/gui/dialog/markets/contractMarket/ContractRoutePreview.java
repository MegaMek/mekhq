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
package mekhq.gui.dialog.markets.contractMarket;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT_BRIGHT;
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;
import static mekhq.gui.baseComponents.hud.HudStyle.translucent;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;

import jakarta.annotation.Nullable;
import mekhq.campaign.JumpPath;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;

/**
 * A small, fixed snapshot of a contract's jump path, drawn in the interstellar map's style: the systems around the
 * route as faction-coloured dots on the map's dark field and grid, the route in the map's planned-route cyan, the
 * force's current system marked in the map's current-location amber, and the target ringed and named.
 *
 * <p>It is deliberately not the interstellar map itself, which is a large live component with its own caches and
 * controls. This preview only reads the route the market has already plotted, so building it costs nothing extra.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class ContractRoutePreview extends JComponent {
    // The interstellar map's own colours, so the preview reads as a piece of it.
    private static final Color MAP_BACKGROUND = new Color(5, 12, 21);
    private static final Color MAP_GRID_MINOR = new Color(35, 66, 82, 45);
    private static final Color MAP_GRID_MAJOR = new Color(50, 91, 108, 75);
    private static final Color ROUTE_COLOR = new Color(65, 210, 224);
    private static final Color CURRENT_LOCATION_COLOR = new Color(255, 190, 82);
    private static final Color UNOWNED_SYSTEM_COLOR = new Color(132, 153, 161);

    /** The smallest span shown, in light years, so a one-jump route is not blown up to fill the frame. */
    private static final double MINIMUM_SPAN = 90.0;
    /** Space kept around the route, as a share of the route's own span. */
    private static final double MARGIN = 0.18;
    private static final double MINOR_GRID_STEP = 10.0;
    private static final double MAJOR_GRID_STEP = 50.0;
    /** The closest grid lines are drawn, in unscaled pixels. */
    private static final int MINIMUM_GRID_SPACING = 6;
    private static final int PREFERRED_WIDTH = 320;
    private static final int PREFERRED_HEIGHT = 170;

    /** A system to draw, with its map position and dot colour worked out once. */
    private record MapPoint(PlanetarySystem system, double x, double y, Color color) {}

    private final LocalDate date;
    private final List<MapPoint> route = new ArrayList<>();
    private final List<MapPoint> surroundings = new ArrayList<>();
    private final String emptyMessage;
    private double minimumX;
    private double minimumY;
    private double spanX;
    private double spanY;

    /**
     * @param path         the route to draw, or {@code null} when there is none
     * @param date         the campaign date, for system names and ownership
     * @param emptyMessage shown in place of the map when there is no route to draw
     */
    ContractRoutePreview(@Nullable JumpPath path, LocalDate date, String emptyMessage) {
        this.date = date;
        this.emptyMessage = emptyMessage;
        setPreferredSize(new Dimension(scaleForGUI(PREFERRED_WIDTH), scaleForGUI(PREFERRED_HEIGHT)));
        setMinimumSize(new Dimension(scaleForGUI(160), scaleForGUI(PREFERRED_HEIGHT)));
        setOpaque(true);

        if (path == null || path.isEmpty()) {
            return;
        }
        for (PlanetarySystem system : path.getSystems()) {
            route.add(point(system));
        }
        frameRoute();
        collectSurroundings();
    }

    /** @return {@code true} if there is a route to draw */
    boolean hasRoute() {
        return !route.isEmpty();
    }

    private MapPoint point(PlanetarySystem system) {
        return new MapPoint(system, system.getX(), system.getY(), ownerColor(system));
    }

    /**
     * The owning faction's map colour, or a neutral grey for unclaimed or contested-by-none space. A system held by
     * several factions takes the first one's colour, as the map's faction dots do.
     */
    private Color ownerColor(PlanetarySystem system) {
        Set<Faction> owners = system.getFactionSet(date);
        if (owners == null || owners.isEmpty()) {
            return UNOWNED_SYSTEM_COLOR;
        }
        Color color = owners.iterator().next().getColor();
        return color == null ? UNOWNED_SYSTEM_COLOR : color;
    }

    /** Fits the view to the route, with a margin, and widened to the frame's shape so the map is not stretched. */
    private void frameRoute() {
        double lowX = Double.MAX_VALUE;
        double lowY = Double.MAX_VALUE;
        double highX = -Double.MAX_VALUE;
        double highY = -Double.MAX_VALUE;
        for (MapPoint point : route) {
            lowX = Math.min(lowX, point.x());
            lowY = Math.min(lowY, point.y());
            highX = Math.max(highX, point.x());
            highY = Math.max(highY, point.y());
        }
        double centerX = (lowX + highX) / 2.0;
        double centerY = (lowY + highY) / 2.0;
        double width = Math.max(MINIMUM_SPAN, (highX - lowX) * (1 + 2 * MARGIN));
        double height = Math.max(MINIMUM_SPAN, (highY - lowY) * (1 + 2 * MARGIN));

        // Match the frame's aspect ratio, so one light year is the same length across and down.
        double aspect = (double) PREFERRED_WIDTH / PREFERRED_HEIGHT;
        if (width / height < aspect) {
            width = height * aspect;
        } else {
            height = width / aspect;
        }
        spanX = width;
        spanY = height;
        minimumX = centerX - width / 2.0;
        minimumY = centerY - height / 2.0;
    }

    /** Gathers every other system inside the framed area, to draw as the map's background stars. */
    private void collectSurroundings() {
        Set<PlanetarySystem> onRoute = new HashSet<>();
        for (MapPoint point : route) {
            onRoute.add(point.system());
        }
        for (PlanetarySystem system : Systems.getInstance().getSystems().values()) {
            Double x = system.getX();
            Double y = system.getY();
            if (x == null || y == null || x < minimumX || x > minimumX + spanX || y < minimumY
                      || y > minimumY + spanY) {
                continue;
            }
            if (!onRoute.contains(system)) {
                surroundings.add(point(system));
            }
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        try {
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            canvas.setColor(MAP_BACKGROUND);
            canvas.fillRect(0, 0, getWidth(), getHeight());

            if (route.isEmpty()) {
                paintEmpty(canvas);
            } else {
                paintGrid(canvas);
                paintSurroundings(canvas);
                paintRoute(canvas);
                paintLabels(canvas);
            }

            canvas.setColor(BORDER);
            canvas.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        } finally {
            canvas.dispose();
        }
    }

    // region Painting

    private double scale() {
        // The frame keeps the route's aspect ratio, so the smaller fit keeps everything inside.
        return Math.min(getWidth() / spanX, getHeight() / spanY);
    }

    private Point2D.Double toScreen(double x, double y) {
        double scale = scale();
        double offsetX = (getWidth() - spanX * scale) / 2.0;
        double offsetY = (getHeight() - spanY * scale) / 2.0;
        // The map's y axis points up, as on the interstellar map.
        return new Point2D.Double(offsetX + (x - minimumX) * scale, getHeight() - offsetY - (y - minimumY) * scale);
    }

    private void paintGrid(Graphics2D canvas) {
        paintGridLines(canvas, MINOR_GRID_STEP, MAP_GRID_MINOR);
        paintGridLines(canvas, MAJOR_GRID_STEP, MAP_GRID_MAJOR);
    }

    private void paintGridLines(Graphics2D canvas, double step, Color color) {
        // On a long route the lines would crowd into a haze, so a grid too fine to read is left out.
        if (step * scale() < scaleForGUI(MINIMUM_GRID_SPACING)) {
            return;
        }
        canvas.setColor(color);
        canvas.setStroke(new BasicStroke(scaleForGUI(1)));
        for (double x = Math.ceil(minimumX / step) * step; x <= minimumX + spanX; x += step) {
            int screenX = (int) Math.round(toScreen(x, minimumY).x);
            canvas.drawLine(screenX, 0, screenX, getHeight());
        }
        for (double y = Math.ceil(minimumY / step) * step; y <= minimumY + spanY; y += step) {
            int screenY = (int) Math.round(toScreen(minimumX, y).y);
            canvas.drawLine(0, screenY, getWidth(), screenY);
        }
    }

    private void paintSurroundings(Graphics2D canvas) {
        double radius = scaleForGUI(2) / 2.0 + 0.5;
        for (MapPoint point : surroundings) {
            Point2D.Double screen = toScreen(point.x(), point.y());
            canvas.setColor(translucent(point.color(), 150));
            canvas.fill(new Ellipse2D.Double(screen.x - radius, screen.y - radius, radius * 2, radius * 2));
        }
    }

    private void paintRoute(Graphics2D canvas) {
        Path2D.Double line = new Path2D.Double();
        for (int index = 0; index < route.size(); index++) {
            Point2D.Double screen = toScreen(route.get(index).x(), route.get(index).y());
            if (index == 0) {
                line.moveTo(screen.x, screen.y);
            } else {
                line.lineTo(screen.x, screen.y);
            }
        }
        // A soft glow under a crisp line, as the map draws a planned route.
        canvas.setColor(translucent(ROUTE_COLOR, 60));
        canvas.setStroke(new BasicStroke(scaleForGUI(5), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        canvas.draw(line);
        canvas.setColor(ROUTE_COLOR);
        canvas.setStroke(new BasicStroke(scaleForGUI(2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        canvas.draw(line);

        // Each jump point, in its owner's colour, ringed in the route colour.
        double radius = scaleForGUI(3);
        for (int index = 1; index < route.size() - 1; index++) {
            MapPoint point = route.get(index);
            Point2D.Double screen = toScreen(point.x(), point.y());
            Ellipse2D.Double dot = new Ellipse2D.Double(screen.x - radius, screen.y - radius, radius * 2, radius * 2);
            canvas.setColor(point.color());
            canvas.fill(dot);
            canvas.setColor(ROUTE_COLOR);
            canvas.setStroke(new BasicStroke(scaleForGUI(1)));
            canvas.draw(dot);
        }

        MapPoint origin = route.getFirst();
        MapPoint target = route.getLast();
        paintMarker(canvas, origin, CURRENT_LOCATION_COLOR, scaleForGUI(5));
        if (target != origin) {
            paintMarker(canvas, target, ACCENT_BRIGHT, scaleForGUI(6));
        }
    }

    private void paintMarker(Graphics2D canvas, MapPoint point, Color ringColor, double radius) {
        Point2D.Double screen = toScreen(point.x(), point.y());
        double core = radius / 2.0;
        canvas.setColor(point.color());
        canvas.fill(new Ellipse2D.Double(screen.x - core, screen.y - core, core * 2, core * 2));
        canvas.setColor(ringColor);
        canvas.setStroke(new BasicStroke(scaleForGUI(2)));
        canvas.draw(new Ellipse2D.Double(screen.x - radius, screen.y - radius, radius * 2, radius * 2));
    }

    /** Names the starting and target systems, kept inside the frame. */
    private void paintLabels(Graphics2D canvas) {
        canvas.setFont(hudFont(Font.BOLD, 0.74f, 0.08f));
        MapPoint origin = route.getFirst();
        MapPoint target = route.getLast();
        paintLabel(canvas, origin, CURRENT_LOCATION_COLOR);
        if (target != origin) {
            paintLabel(canvas, target, TEXT);
        }
    }

    private void paintLabel(Graphics2D canvas, MapPoint point, Color color) {
        String name = point.system().getName(date);
        if (name == null) {
            return;
        }
        FontMetrics metrics = canvas.getFontMetrics();
        Point2D.Double screen = toScreen(point.x(), point.y());
        int pad = scaleForGUI(4);
        int x = (int) Math.round(screen.x) + scaleForGUI(9);
        int y = (int) Math.round(screen.y) + metrics.getAscent() / 2 - scaleForGUI(1);
        // Flip to the left of the marker when the name would run off the right edge.
        if (x + metrics.stringWidth(name) > getWidth() - pad) {
            x = (int) Math.round(screen.x) - scaleForGUI(9) - metrics.stringWidth(name);
        }
        x = Math.clamp(x, pad, Math.max(pad, getWidth() - pad - metrics.stringWidth(name)));
        y = Math.clamp(y, metrics.getAscent() + pad, Math.max(metrics.getAscent() + pad, getHeight() - pad));

        // A dark halo keeps the name readable over stars and grid lines.
        canvas.setColor(translucent(MAP_BACKGROUND, 200));
        int halo = scaleForGUI(2);
        canvas.fillRect(x - halo, y - metrics.getAscent(), metrics.stringWidth(name) + 2 * halo, metrics.getHeight());
        canvas.setColor(color);
        canvas.drawString(name, x, y);
    }

    private void paintEmpty(Graphics2D canvas) {
        paintGridLinesWithoutRoute(canvas);
        canvas.setFont(hudFont(Font.PLAIN, 0.86f, 0.0f));
        FontMetrics metrics = canvas.getFontMetrics();
        canvas.setColor(TEXT_FAINT);
        int x = Math.max(scaleForGUI(8), (getWidth() - metrics.stringWidth(emptyMessage)) / 2);
        canvas.drawString(emptyMessage, x, (getHeight() + metrics.getAscent()) / 2);
    }

    /** Without a route there is nothing to frame, so the grid is drawn at a fixed pixel spacing instead. */
    private void paintGridLinesWithoutRoute(Graphics2D canvas) {
        int step = scaleForGUI(18);
        canvas.setColor(MAP_GRID_MINOR);
        for (int x = step; x < getWidth(); x += step) {
            canvas.drawLine(x, 0, x, getHeight());
        }
        for (int y = step; y < getHeight(); y += step) {
            canvas.drawLine(0, y, getWidth(), y);
        }
    }

    // endregion Painting
}
