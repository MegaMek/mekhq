/*
* MegaMek - Copyright (C) 2020 - The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package mekhq.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconCoords;
import mekhq.campaign.stratcon.StratconFacility;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconTrackState;
import mekhq.gui.stratcon.StratconScenarioWizard;
import mekhq.gui.stratcon.TrackForceAssignmentUI;

/**
 * This panel handles AtB-Stratcon GUI interactions with a specific scenario track.
 * @author NickAragua
 */
public class StratconPanel extends JPanel implements ActionListener {
    private static final long serialVersionUID = 7405934788894417292L;
    
    public static final int HEX_X_RADIUS = 42;
    public static final int HEX_Y_RADIUS = 37;
    
    private static final String RCLICK_COMMAND_MANAGE_FORCES = "ManageForces";
    private static final String RCLICK_COMMAND_MANAGE_SCENARIO = "ManageScenario";
    private static final String RCLICK_COMMAND_REVEAL_TRACK = "RevealTrack";
    private static final String RCLICK_COMMAND_STICKY_FORCE = "StickyForce";
    private static final String RCLICK_COMMAND_STICKY_FORCE_ID = "StickyForceID";

    /**
     * What to do when drawing a hex
     */
    private enum DrawHexType {
        /**
         * The interior of a hex
         */
        Hex,
        
        /**
         * The outline of a hex
         */
        Outline,
        
        /**
         * Pretend we're drawing a hex, but don't actually do it, useful for 
         * figuring out which hex a mouse click landed in, etc.
         */
        DryRun
    }

    private float scale = 1f;

    private StratconTrackState currentTrack;
    private StratconCampaignState campaignState;
    private Campaign campaign;

    private BoardState boardState = new BoardState();

    private Point clickedPoint;
    private JPopupMenu rightClickMenu;
    private JMenuItem menuItemManageForceAssignments;
    private JMenuItem menuItemManageScenario;
    private JMenuItem menuItemGMReveal;
    
    // data structure holding how many unit/scenario/base icons have been drawn in the hex
    // used to control how low the text description goes.
    private Map<StratconCoords, Integer> numIconsInHex = new HashMap<>();
    
    private StratconScenarioWizard scenarioWizard;
    private TrackForceAssignmentUI assignmentUI;
    
    private JLabel infoArea;

    /**
     * Constructs a StratconPanel instance, given a parent campaign GUI and a pointer to an info area.
     */
    public StratconPanel(CampaignGUI gui, JLabel infoArea) {
        campaign = gui.getCampaign();
        
        scenarioWizard = new StratconScenarioWizard(campaign);
        this.infoArea = infoArea;
        
        assignmentUI = new TrackForceAssignmentUI(this);
        assignmentUI.setVisible(false);
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                mouseReleasedHandler(e);
            }
        });
    }

    /**
     * Handler for when a specific track is selected - switches rendering to that track.
     */
    public void selectTrack(StratconCampaignState campaignState, StratconTrackState track) {
        this.campaignState = campaignState;
        currentTrack = track;
        repaint();
    }
    
    /**
     * Constructs the right-click context menu, optionally for a scenario
     */
    private void buildRightClickMenu(StratconCoords coords) {
        rightClickMenu = new JPopupMenu();
        
        StratconScenario scenario = getSelectedScenario();
        
        if (scenario == null) {
            menuItemManageForceAssignments = new JMenuItem();
            menuItemManageForceAssignments.setText("Manage Force Assignments");
            menuItemManageForceAssignments.setActionCommand(RCLICK_COMMAND_MANAGE_FORCES);
            menuItemManageForceAssignments.addActionListener(this);
            rightClickMenu.add(menuItemManageForceAssignments);
        }
        
        if (scenario != null) {
            menuItemManageScenario = new JMenuItem();
            menuItemManageScenario.setText("Manage Scenario");
            menuItemManageScenario.setActionCommand(RCLICK_COMMAND_MANAGE_SCENARIO);
            menuItemManageScenario.addActionListener(this);
            rightClickMenu.add(menuItemManageScenario);
        }
        
        if ((currentTrack != null) && currentTrack.getAssignedCoordForces().containsKey(coords)) {
            for (int forceID : currentTrack.getAssignedCoordForces().get(coords)) {
                String forceName = campaign.getForce(forceID).getName();
                
                JCheckBoxMenuItem stickyForceItem = new JCheckBoxMenuItem();
                stickyForceItem.setText(String.format("%s - remain deployed", forceName));
                stickyForceItem.setActionCommand(RCLICK_COMMAND_STICKY_FORCE);
                stickyForceItem.putClientProperty(RCLICK_COMMAND_STICKY_FORCE_ID, forceID);
                stickyForceItem.addActionListener(this);
                stickyForceItem.setSelected(currentTrack.getStickyForces().contains(forceID));
                rightClickMenu.add(stickyForceItem);
            }
        }
            
        if ((currentTrack != null) && campaign.isGM()) {
            rightClickMenu.addSeparator();
            
            menuItemGMReveal = new JMenuItem();
            menuItemGMReveal.setText(currentTrack.isGmRevealed() ? "Hide Track" : "Reveal Track");
            menuItemGMReveal.setActionCommand(RCLICK_COMMAND_REVEAL_TRACK);
            menuItemGMReveal.addActionListener(this);
            rightClickMenu.add(menuItemGMReveal);
        }
    }

    /**
     * Renders the panel, hexes, forces, facilities and all that.
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if ((campaignState == null) || (currentTrack == null)) {
            return;
        }
        
        numIconsInHex.clear();

        Graphics2D g2D = (Graphics2D) g;
        AffineTransform initialTransform = g2D.getTransform();
        performInitialTransform(g2D);
        AffineTransform originTransform = g2D.getTransform();

        drawHexes(g2D, DrawHexType.Hex);
        g2D.setTransform(originTransform);
        drawHexes(g2D, DrawHexType.Outline);
        g2D.setTransform(originTransform);
        g2D.translate(HEX_X_RADIUS, HEX_Y_RADIUS);
        drawScenarios(g2D);
        g2D.setTransform(originTransform);
        g2D.translate(HEX_X_RADIUS, HEX_Y_RADIUS);
        drawFacilities(g2D);
        g2D.setTransform(originTransform);
        g2D.translate(HEX_X_RADIUS, HEX_Y_RADIUS);
        drawForces(g2D);

        g2D.setTransform(initialTransform);
        if (clickedPoint != null) {
            g2D.setColor(Color.BLUE);
            g2D.drawRect((int) clickedPoint.getX(), (int) clickedPoint.getY(), 2, 2);
        }
    }

    /**
     * This method contains a dirty secret hack, described on line 253-258
     * The point of it is to draw all the hexes for the board. 
     * If it's a "dry run", we don't actually draw the hexes, we just pretend to
     * until we "draw" one that encompasses the clicked point.
     * @param g2D - graphics object on which to draw
     * @param drawHexType - whether to draw the hex backgrounds, hex outlines or a dry run for click detection
     */
    private boolean drawHexes(Graphics2D g2D, DrawHexType drawHexType) {
        Polygon graphHex = new Polygon();
        int xRadius = HEX_X_RADIUS;
        int yRadius = HEX_Y_RADIUS;
        boolean pointFound = false;

        graphHex.addPoint(-xRadius / 2, -yRadius);
        graphHex.addPoint(-xRadius, 0);
        graphHex.addPoint(-xRadius / 2, yRadius);
        graphHex.addPoint(xRadius / 2, yRadius);
        graphHex.addPoint(xRadius, 0);
        graphHex.addPoint(xRadius / 2, -yRadius);

        graphHex.translate(xRadius, yRadius);

        Point translatedClickedPoint = null;

        // this was derived somewhat experimentally
        // the clicked point always seems a little off, so we 
        // a) apply the current transform to it, prior to drawing all the hexes
        // b) subtract an additional Y_RADIUS x 2 (Y_DIAMETER)
        // this gets us the point within the clicked hex
        // it's probably finicky, so any major changes to the rendering mechanism will likely break the detection
        if (clickedPoint != null) {
            translatedClickedPoint = (Point) clickedPoint.clone();
            
            // since we have the possibility of scrolling, we need to convert the on-screen clicked coordinates
            // to on-board coordinates. Thankfully, SwingUtilities provides the main computational ability for that
            Point actualPanelPoint = SwingUtilities.convertPoint(this, translatedClickedPoint, this.getParent());
            translatedClickedPoint.translate((int) -(actualPanelPoint.getX() - translatedClickedPoint.getX()), 
                                                (int) -(actualPanelPoint.getY() - translatedClickedPoint.getY()));
            
            // now we translate to the starting point of where we're drawing and then go down a hex
            translatedClickedPoint.translate((int) g2D.getTransform().getTranslateX(), (int) g2D.getTransform().getTranslateY());
            translatedClickedPoint.translate(0, HEX_Y_RADIUS * -2);
        }

        for (int x = 0; x < currentTrack.getWidth(); x++) {            
            for (int y = 0; y < currentTrack.getHeight(); y++) {
                if (drawHexType == DrawHexType.Outline) {
                    g2D.setColor(new Color(0, 0, 0));
                    g2D.drawPolygon(graphHex);                    
                } else if (drawHexType == DrawHexType.Hex) {
                    
                    if (currentTrack.coordsRevealed(x, y) || currentTrack.isGmRevealed()) {
                        g2D.setColor(Color.LIGHT_GRAY);
                    } else {
                        g2D.setColor(Color.DARK_GRAY);
                    }
                    g2D.fillPolygon(graphHex);
                    
                    if ((translatedClickedPoint != null) && graphHex.contains(translatedClickedPoint)) {
                        g2D.setColor(Color.WHITE);
                        BasicStroke s = new BasicStroke((float) 8.0);
                        Stroke push = g2D.getStroke();
                        g2D.setStroke(s);
                        g2D.drawPolygon(graphHex);
                        
                        boardState.selectedX = x;
                        boardState.selectedY = y;
                        pointFound = true;
                        g2D.setStroke(push);
                    }
                } else if (drawHexType == DrawHexType.DryRun) {
                    if ((translatedClickedPoint != null) && graphHex.contains(translatedClickedPoint)) {
                        boardState.selectedX = x;
                        boardState.selectedY = y;
                        pointFound = true;
                    }
                }

                if (drawHexType == DrawHexType.Hex) {
                    g2D.setColor(Color.GREEN);
                    g2D.drawString(x + "," + y, graphHex.xpoints[0] + (xRadius / 4), graphHex.ypoints[0] + yRadius);
                }

                int[] downwardVector = getDownwardYVector();
                graphHex.translate(downwardVector[0], downwardVector[1]);
            }

            int[] translationVector = getRightAndUpVector(x % 2 == 0);
            graphHex.translate(translationVector[0], translationVector[1]);
        }

        return pointFound;
    }

    /**
     * Worker function to render icons representing scenarios to the given surface.
     */
    private void drawScenarios(Graphics2D g2D) {
        Polygon scenarioMarker = new Polygon();
        Polygon scenarioMarker2 = new Polygon();
        int xRadius = HEX_X_RADIUS / 3;
        int yRadius = HEX_Y_RADIUS / 3;
        int smallXRadius = xRadius / 2;
        int smallYRadius = xRadius / 2;

        scenarioMarker.addPoint(-xRadius, -yRadius);
        scenarioMarker.addPoint(-xRadius, yRadius);
        scenarioMarker.addPoint(xRadius, yRadius);
        scenarioMarker.addPoint(xRadius, -yRadius);
        
        scenarioMarker2.addPoint(-smallXRadius, -smallYRadius);
        scenarioMarker2.addPoint(-smallXRadius, smallYRadius);
        scenarioMarker2.addPoint(smallXRadius, smallYRadius);
        scenarioMarker2.addPoint(smallXRadius, -smallYRadius);

        for (int x = 0; x < currentTrack.getWidth(); x++) {            
            for (int y = 0; y < currentTrack.getHeight(); y++) {
                StratconCoords currentCoords = new StratconCoords(x, y);
                StratconScenario scenario = currentTrack.getScenario(currentCoords);
                
                // if there's a scenario here that has a deployment/battle date
                // or if there's a scenario here and we've gm-revealed everything
                if ((scenario != null) &&
                        ((scenario.getDeploymentDate() != null) || currentTrack.isGmRevealed())) {
                    g2D.setColor(Color.RED);
                    g2D.drawPolygon(scenarioMarker);
                    g2D.drawPolygon(scenarioMarker2);
                    if (currentTrack.getFacility(currentCoords) == null) {
                        drawTextEffect(g2D, scenarioMarker, "Hostile Force Detected", currentCoords);
                    } else if (currentTrack.getFacility(currentCoords).getOwner() == ForceAlignment.Allied) {
                        drawTextEffect(g2D, scenarioMarker, "Under Attack!", currentCoords);
                    }
                }

                int[] downwardVector = getDownwardYVector();
                scenarioMarker.translate(downwardVector[0], downwardVector[1]);
                scenarioMarker2.translate(downwardVector[0], downwardVector[1]);
            }

            int[] translationVector = getRightAndUpVector(x % 2 == 0);
            scenarioMarker.translate(translationVector[0], translationVector[1]);
            scenarioMarker2.translate(translationVector[0], translationVector[1]);
        }
    }
    
    /**
     * Worker function to render facility icons to the given surface.
     */
    private void drawFacilities(Graphics2D g2D) {
        Polygon facilityMarker = new Polygon();
        int xRadius = HEX_X_RADIUS / 3;
        int yRadius = HEX_Y_RADIUS / 3;

        facilityMarker.addPoint(-xRadius, -yRadius);
        facilityMarker.addPoint(-xRadius, yRadius);
        facilityMarker.addPoint(xRadius, yRadius);
        facilityMarker.addPoint(xRadius, -yRadius);

        for (int x = 0; x < currentTrack.getWidth(); x++) {            
            for (int y = 0; y < currentTrack.getHeight(); y++) {
                StratconCoords currentCoords = new StratconCoords(x, y);
                StratconFacility facility = currentTrack.getFacility(currentCoords);
                
                if ((facility != null) && (facility.isVisible() || currentTrack.isGmRevealed())) {
                    g2D.setColor(facility.getOwner() == ForceAlignment.Allied ? Color.GREEN : Color.RED);
                    g2D.drawPolygon(facilityMarker);
                    drawTextEffect(g2D, facilityMarker, facility.getFormattedDisplayableName(), currentCoords);
                }

                int[] downwardVector = getDownwardYVector();
                facilityMarker.translate(downwardVector[0], downwardVector[1]);
            }

            int[] translationVector = getRightAndUpVector(x % 2 == 0);
            facilityMarker.translate(translationVector[0], translationVector[1]);
        }
    }
    
    /**
     * Worker function to render force icons to the given surface.
     */
    private void drawForces(Graphics2D g2D) {
        int xRadius = HEX_X_RADIUS / 3;
        int yRadius = HEX_Y_RADIUS / 3;

        Shape forceMarker = new Ellipse2D.Double(-xRadius, -yRadius, 
                xRadius * 2.0, yRadius * 2.0);
        
        for (int x = 0; x < currentTrack.getWidth(); x++) {            
            for (int y = 0; y < currentTrack.getHeight(); y++) {
                StratconCoords currentCoords = new StratconCoords(x, y);
                
                if (currentTrack.getAssignedCoordForces().containsKey(currentCoords)) {
                    for (int forceID : currentTrack.getAssignedCoordForces().get(currentCoords)) {                   
                        g2D.setColor(Color.CYAN);
                        g2D.draw(forceMarker);
                        
                        Font currentFont = g2D.getFont();
                        Font newFont = currentFont.deriveFont(Collections.singletonMap(
                                TextAttribute.WEIGHT, TextAttribute.WEIGHT_BOLD));
                        g2D.setFont(newFont);
                        
                        drawTextEffect(g2D, forceMarker, campaign.getForce(forceID).getName(), currentCoords);
                        
                        g2D.setFont(currentFont);
                    }
                }

                int[] downwardVector = getDownwardYVector();
                AffineTransform ellipseTransform = new AffineTransform();
                ellipseTransform.translate(downwardVector[0], downwardVector[1]);
                forceMarker = ellipseTransform.createTransformedShape(forceMarker);
            }

            int[] translationVector = getRightAndUpVector(x % 2 == 0);
            
            AffineTransform ellipseTransform = new AffineTransform();
            ellipseTransform.translate(translationVector[0], translationVector[1]);
            forceMarker = ellipseTransform.createTransformedShape(forceMarker);
        }
    }
    
    /**
     * Draws some text and line to it from a given polygon. 
     * Smart enough not to layer multiple strings on top of each other if they're all drawn in the same hex.
     */
    private void drawTextEffect(Graphics2D g2D, Shape marker, String text, StratconCoords coords) {
        int verticalOffsetIndex = numIconsInHex.containsKey(coords) ? numIconsInHex.get(coords) : 0;
        
        double startX = marker.getBounds().getMaxX();
        double startY = marker.getBounds().getMinY();
        double midPointX = startX + HEX_X_RADIUS / 4;
        double midPointY = startY - HEX_Y_RADIUS / 4 + g2D.getFontMetrics().getHeight() * verticalOffsetIndex;
        double endPointX = midPointX + HEX_X_RADIUS / 2;
        
        g2D.drawLine((int) startX, (int) startY, (int) midPointX, (int) midPointY);
        g2D.drawLine((int) midPointX, (int) midPointY, (int) endPointX, (int) midPointY);
        g2D.drawString(text, (int) endPointX, (int) midPointY);
        
        // register that we drew text off of this hex
        numIconsInHex.put(coords, ++verticalOffsetIndex);
    }

    /**
     * Returns the translation that we need to make to render the "next downward" hex.
     * @return Two dimensional array with the first element being the x vector and the second being the y vector
     */
    private int[] getDownwardYVector() {
        return new int[] { 0, HEX_Y_RADIUS * 2 };
    }

    /**
     * Returns the translation that we need to make to move from the bottom of a column to the top of the next
     * column to the right.
     * @param evenColumn Whether the column we're currently in is odd or even
     * @return Two dimensional array with the first element being the x vector and the second being the y vector
     */
    private int[] getRightAndUpVector(boolean evenColumn) {
        int yRadius = (int) (HEX_Y_RADIUS);
        int xRadius = (int) (HEX_X_RADIUS);

        int yTranslation = currentTrack.getHeight() * yRadius * 2;
        if (evenColumn) {
            yTranslation += yRadius;
        } else {
            yTranslation -= yRadius;
        }

        return new int[] { (int) Math.floor(xRadius * 1.5), -yTranslation};
    }

    /**
     * Go to the origin of the hex board and reset the scaling.
     */
    private void performInitialTransform(Graphics2D g2D) {
        g2D.translate(0, 0 + HEX_Y_RADIUS);
        g2D.scale(scale, scale);
    }

    /** 
     * Worker function that takes the current clicked point and a graphics 2D object
     * and detects which hex was clicked by doing a dry run hex render.
     * 
     * Dependent upon clickedPoint being set and having an active graphics object for this class.
     * 
     * Side effects: the dry run sets the boardState clicked hex coordinates.
     * @return Whether or not the clicked point was found on the hex board
     */
    private boolean detectClickedHex() {
        Graphics2D g2D = (Graphics2D) getGraphics();
        AffineTransform transform = g2D.getTransform();
        performInitialTransform(g2D);
        boolean pointFoundOnBoard = drawHexes(g2D, DrawHexType.DryRun);
        g2D.setTransform(transform);

        return pointFoundOnBoard;
    }

    /**
     * Event handler for when a mouse button is released.
     */
    public void mouseReleasedHandler(MouseEvent e) {
        if (e.getSource() != this) {
            return;
        }

        // left button generally selects a hex
        if (e.getButton() == MouseEvent.BUTTON1) {        
            clickedPoint = e.getPoint();
            boolean pointFoundOnBoard = detectClickedHex();
            
            if (pointFoundOnBoard) {
                infoArea.setText(buildSelectedHexInfo());
            }

            repaint();
        // right button generally pops up a context menu
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            clickedPoint = e.getPoint();
            detectClickedHex();
            
            StratconCoords selectedCoords = boardState.getSelectedCoords();
            if (selectedCoords == null) {
                return;
            }
            
            repaint();
            buildRightClickMenu(selectedCoords);
            rightClickMenu.show(this, e.getX(), e.getY());
        }
    }
    
    public StratconScenario getSelectedScenario() {
        return currentTrack.getScenario(boardState.getSelectedCoords());
    }
    
    public StratconTrackState getCurrentTrack() {
        return currentTrack;
    }
    
    public StratconCoords getSelectedCoords() {
        return boardState.getSelectedCoords();
    }
    
    /**
     * Worker function that outputs html representing the status of a selected hex,
     * containing info such as whether it's been revealed, assigned forces, scenarios, facilities, etc.
     */
    private String buildSelectedHexInfo() {
        StringBuilder infoBuilder = new StringBuilder();
        infoBuilder.append("<html><br/>");
        
        boolean coordsRevealed = currentTrack.getRevealedCoords().contains(boardState.getSelectedCoords());
        if (coordsRevealed) {
            infoBuilder.append("<span color='green'>Recon complete</span><br/>");
        }
        
        if (currentTrack.getAssignedCoordForces().containsKey(boardState.getSelectedCoords())) {
            for (int forceID : currentTrack.getAssignedCoordForces().get(boardState.getSelectedCoords())) {
                Force force = campaign.getForce(forceID);
                infoBuilder.append(force.getName()).append(" assigned");
                
                if (currentTrack.getStickyForces().contains(forceID)) {
                    infoBuilder.append(" - remain deployed");
                }
                
                infoBuilder.append("<br/>")
                    .append("Returns on ")
                    .append(currentTrack.getAssignedForceReturnDates().get(forceID))
                    .append("<br/>");
            }
        }
        
        if (coordsRevealed || currentTrack.isGmRevealed()) {
            StratconFacility facility = currentTrack.getFacility(boardState.getSelectedCoords());
            
            if ((facility != null) && (facility.getFacilityType() != null)) {
                if (facility.isStrategicObjective()) {
                    infoBuilder.append(String.format("<br/><span color='%s'>Contract objective located</span>", 
                            facility.getOwner() == ForceAlignment.Allied ? "green" : "red"));
                }
                infoBuilder.append((facility.getOwner() == ForceAlignment.Allied) ? "<span color='green'>" : "<span color='red'>")
                    .append("<br/>")
                    .append(facility.getFormattedDisplayableName())
                    .append("<span>");
            }
            
        } else {
            infoBuilder.append("<span color='red'>Recon incomplete</span>");
        }
        infoBuilder.append("<br/>");
        
        
        StratconScenario selectedScenario = getSelectedScenario();
        if ((selectedScenario != null) &&
                ((selectedScenario.getDeploymentDate() != null) || currentTrack.isGmRevealed())) {
            infoBuilder.append(selectedScenario.getInfo());
        }
        
        infoBuilder.append("</html>");
        
        return infoBuilder.toString();
    }

    /**
     * Data structure containing current state of the board.
     */
    private static class BoardState {
        public Integer selectedX;
        public Integer selectedY;
        
        public StratconCoords getSelectedCoords() {
            if ((selectedX == null) || (selectedY == null)) {
                return null;
            } else {
                return new StratconCoords(selectedX, selectedY);
            }
        }
    }

    /**
     * Event handler for various button and menu item presses.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        StratconCoords selectedCoords = boardState.getSelectedCoords();
        if (selectedCoords == null) {
            return;
        }
        
        switch (e.getActionCommand()) {
            case RCLICK_COMMAND_MANAGE_FORCES:
                assignmentUI.display(campaign, campaignState, selectedCoords);
                assignmentUI.setVisible(true);
                break;
            case RCLICK_COMMAND_MANAGE_SCENARIO:
                scenarioWizard.setCurrentScenario(currentTrack.getScenario(selectedCoords),
                        currentTrack, campaignState);
                scenarioWizard.toFront();
                scenarioWizard.setVisible(true);
                break;
            case RCLICK_COMMAND_REVEAL_TRACK:
                currentTrack.setGmRevealed(!currentTrack.isGmRevealed());
                menuItemGMReveal.setText(currentTrack.isGmRevealed() ? "Hide Track" : "Reveal Track");
                break;
            case RCLICK_COMMAND_STICKY_FORCE:
                JCheckBoxMenuItem source = (JCheckBoxMenuItem) e.getSource();
                int forceID = (int) source.getClientProperty(RCLICK_COMMAND_STICKY_FORCE_ID);
                
                if (source.isSelected()) {
                    currentTrack.addStickyForce(forceID);
                } else {
                    currentTrack.removeStickyForce(forceID);
                }
                
                break;
        }
        
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        if (currentTrack != null) {
            int xDimension = (int) Math.floor(HEX_X_RADIUS * 1.75 * currentTrack.getWidth());
            int yDimension = (int) Math.floor(HEX_Y_RADIUS * 2.1 * currentTrack.getHeight());
            
            return new Dimension(xDimension, yDimension);
        } else {
            return super.getPreferredSize();
        }
    }
}
