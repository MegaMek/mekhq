/*
 * Copyright (C) 2019-2026 The MegaMek Team. All Rights Reserved.
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

import static java.awt.Color.BLACK;
import static java.awt.Color.BLUE;
import static java.awt.Font.BOLD;
import static megamek.utilities.ImageUtilities.addTintToBufferedImage;
import static mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment.Allied;
import static mekhq.campaign.stratCon.StratConScenario.ScenarioState.PRIMARY_FORCES_COMMITTED;
import static mekhq.campaign.stratCon.StratConScenario.ScenarioState.UNRESOLVED;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import megamek.common.util.ImageUtil;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.stratCon.StratConBiomeManifest;
import mekhq.campaign.stratCon.StratConBiomeManifest.ImageType;
import mekhq.campaign.stratCon.StratConCampaignState;
import mekhq.campaign.stratCon.StratConCoords;
import mekhq.campaign.stratCon.StratConFacility;
import mekhq.campaign.stratCon.StratConFacilityFactory;
import mekhq.campaign.stratCon.StratConRulesManager;
import mekhq.campaign.stratCon.StratConScenario;
import mekhq.campaign.stratCon.StratConScenario.ScenarioState;
import mekhq.campaign.stratCon.StratConTrackState;
import mekhq.gui.stratCon.StratConScenarioWizard;
import mekhq.gui.stratCon.TrackForceAssignmentUI;
import mekhq.utilities.ReportingUtilities;

/**
 * This panel handles AtB-StratCon GUI interactions with a specific scenario track.
 *
 * @author NickAragua
 */
public class StratConPanel extends JPanel implements ActionListener {
    private static final MMLogger logger = MMLogger.create(StratConPanel.class);

    public static final int HEX_X_RADIUS = 42;
    public static final int HEX_Y_RADIUS = 36;

    private static final String RIGHT_CLICK_COMMAND_MANAGE_FORCES = "ManageForces";
    private static final String RIGHT_CLICK_COMMAND_MANAGE_SCENARIO = "ManageScenario";
    private static final String RIGHT_CLICK_COMMAND_REVEAL_TRACK = "RevealTrack";
    private static final String RIGHT_CLICK_COMMAND_STICKY_FORCE = "StickyForce";
    private static final String RIGHT_CLICK_COMMAND_STICKY_FORCE_ID = "StickyForceID";
    private static final String RIGHT_CLICK_COMMAND_REMOVE_FACILITY = "RemoveFacility";
    private static final String RIGHT_CLICK_COMMAND_CAPTURE_FACILITY = "CaptureFacility";
    private static final String RIGHT_CLICK_COMMAND_ADD_FACILITY = "AddFacility";
    private static final String RIGHT_CLICK_COMMAND_REMOVE_SCENARIO = "RemoveScenario";
    private static final String RIGHT_CLICK_COMMAND_RESET_DEPLOYMENT = "ResetDeployment";

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
         * Pretend we're drawing a hex, but don't actually do it, useful for figuring out which hex a mouse click landed
         * in, etc.
         */
        DryRun
    }

    private StratConTrackState currentTrack;
    private StratConCampaignState campaignState;
    private final Campaign campaign;

    private final BoardState boardState = new BoardState();

    private Point clickedPoint;
    private JPopupMenu rightClickMenu;
    private JMenuItem menuItemGMReveal;

    // data structure holding how many unit/scenario/base icons have been drawn in
    // the hex
    // used to control how low the text description goes.
    private final Map<StratConCoords, Integer> numIconsInHex = new HashMap<>();

    private final StratConScenarioWizard scenarioWizard;
    private final TrackForceAssignmentUI assignmentUI;

    private final JLabel infoArea;

    private final Map<String, BufferedImage> imageCache = new HashMap<>();

    private boolean commitForces = false;

    public StratConScenarioWizard getStratConScenarioWizard() {
        return scenarioWizard;
    }

    public TrackForceAssignmentUI getAssignmentUI() {
        return assignmentUI;
    }

    /**
     * Constructs a StratConPanel instance, given a parent campaign GUI and a pointer to an info area.
     */
    public StratConPanel(CampaignGUI gui, JLabel infoArea) {
        campaign = gui.getCampaign();

        scenarioWizard = new StratConScenarioWizard(campaign, this);
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
    public void selectTrack(StratConCampaignState campaignState, StratConTrackState track) {
        this.campaignState = campaignState;
        currentTrack = track;

        // clear hex selection
        boardState.selectedX = null;
        boardState.selectedY = null;
        infoArea.setText(buildSelectedHexInfo(campaign));

        repaint();
    }

    /**
     * Constructs the right-click context menu, optionally for a scenario
     */
    private void buildRightClickMenu(StratConCoords coords) {
        rightClickMenu = new JPopupMenu();

        StratConScenario scenario = getSelectedScenario();

        // display "Manage Force Assignment" if there is not a force already on the hex
        // except if there is already a non-cloaked scenario here.
        if (StratConRulesManager.canManuallyDeployAnyForce(coords, currentTrack, campaignState.getContract())) {
            JMenuItem menuItemManageForceAssignments = new JMenuItem();
            menuItemManageForceAssignments.setText("Manage Deployment");
            menuItemManageForceAssignments.setActionCommand(RIGHT_CLICK_COMMAND_MANAGE_FORCES);
            menuItemManageForceAssignments.addActionListener(this);
            rightClickMenu.add(menuItemManageForceAssignments);
        }

        // display "Manage Scenario" if there is already a visible scenario on the hex
        if (scenario != null) {
            AtBDynamicScenario backingScenario = scenario.getBackingScenario();

            if (backingScenario != null && !backingScenario.isCloaked()) {
                JMenuItem menuItemManageScenario = new JMenuItem();

                if (scenario.getCurrentState().equals(UNRESOLVED)) {
                    menuItemManageScenario.setText("Manage Deployment");
                    menuItemManageScenario.setActionCommand(RIGHT_CLICK_COMMAND_MANAGE_FORCES);
                } else {
                    menuItemManageScenario.setText("Manage Reinforcements");
                    menuItemManageScenario.setActionCommand(RIGHT_CLICK_COMMAND_MANAGE_SCENARIO);
                }

                menuItemManageScenario.addActionListener(this);
                rightClickMenu.add(menuItemManageScenario);
            }
        }

        if ((currentTrack != null) && currentTrack.getAssignedCoordForces().containsKey(coords)) {
            for (int forceID : currentTrack.getAssignedCoordForces().get(coords)) {
                String forceName = campaign.getFormation(forceID).getName();

                JCheckBoxMenuItem stickyForceItem = new JCheckBoxMenuItem();
                stickyForceItem.setText(String.format("%s - remain deployed", forceName));
                stickyForceItem.setActionCommand(RIGHT_CLICK_COMMAND_STICKY_FORCE);
                stickyForceItem.putClientProperty(RIGHT_CLICK_COMMAND_STICKY_FORCE_ID, forceID);
                stickyForceItem.addActionListener(this);
                stickyForceItem.setSelected(currentTrack.getStickyForces().contains(forceID));
                rightClickMenu.add(stickyForceItem);
            }
        }

        if ((currentTrack != null) && campaign.isGM()) {
            rightClickMenu.addSeparator();

            menuItemGMReveal = new JMenuItem();
            menuItemGMReveal.setText(currentTrack.isGmRevealed() ? "Hide Sector (GM)" : "Reveal Sector (GM)");
            menuItemGMReveal.setActionCommand(RIGHT_CLICK_COMMAND_REVEAL_TRACK);
            menuItemGMReveal.addActionListener(this);
            rightClickMenu.add(menuItemGMReveal);

            if (currentTrack.getFacility(coords) != null) {
                JMenuItem menuItemRemoveFacility = new JMenuItem();
                menuItemRemoveFacility.setText("Remove Facility (GM)");
                menuItemRemoveFacility.setActionCommand(RIGHT_CLICK_COMMAND_REMOVE_FACILITY);
                menuItemRemoveFacility.addActionListener(this);
                rightClickMenu.add(menuItemRemoveFacility);

                JMenuItem menuItemSwitchOwner = new JMenuItem();
                menuItemSwitchOwner.setText("Switch Owner (GM)");
                menuItemSwitchOwner.setActionCommand(RIGHT_CLICK_COMMAND_CAPTURE_FACILITY);
                menuItemSwitchOwner.addActionListener(this);
                rightClickMenu.add(menuItemSwitchOwner);
            } else {
                JMenu menuItemAddFacility = new JMenu();
                menuItemAddFacility.setText("Add Facility (GM)");

                JMenu menuItemAddAlliedFacility = new JMenu();
                menuItemAddAlliedFacility.setText("Allied");
                menuItemAddFacility.add(menuItemAddAlliedFacility);

                for (StratConFacility facility : StratConFacilityFactory.getAlliedFacilities()) {
                    JMenuItem facilityItem = new JMenuItem();
                    facilityItem.setText(facility.getDisplayableName());
                    facilityItem.setActionCommand(RIGHT_CLICK_COMMAND_ADD_FACILITY);
                    facilityItem.putClientProperty(RIGHT_CLICK_COMMAND_ADD_FACILITY, facility);
                    facilityItem.addActionListener(this);
                    menuItemAddAlliedFacility.add(facilityItem);
                }

                JMenu menuItemAddHostileFacility = new JMenu();
                menuItemAddHostileFacility.setText("Hostile");
                menuItemAddFacility.add(menuItemAddHostileFacility);

                for (StratConFacility facility : StratConFacilityFactory.getHostileFacilities()) {
                    JMenuItem facilityItem = new JMenuItem();
                    facilityItem.setText(facility.getDisplayableName());
                    facilityItem.setActionCommand(RIGHT_CLICK_COMMAND_ADD_FACILITY);
                    facilityItem.putClientProperty(RIGHT_CLICK_COMMAND_ADD_FACILITY, facility);
                    facilityItem.addActionListener(this);
                    menuItemAddHostileFacility.add(facilityItem);
                }

                rightClickMenu.add(menuItemAddFacility);
            }

            if (scenario != null) {
                JMenuItem removeScenarioItem = new JMenuItem();
                removeScenarioItem.setText("Remove Scenario (GM)");
                removeScenarioItem.setActionCommand(RIGHT_CLICK_COMMAND_REMOVE_SCENARIO);
                removeScenarioItem.addActionListener(this);
                rightClickMenu.add(removeScenarioItem);

                JMenuItem resetDeploymentItem = new JMenuItem();
                resetDeploymentItem.setText("Reset Deployment (GM)");
                resetDeploymentItem.setActionCommand(RIGHT_CLICK_COMMAND_RESET_DEPLOYMENT);
                resetDeploymentItem.addActionListener(this);
                rightClickMenu.add(resetDeploymentItem);
            }
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
        drawFacilities(g2D);
        g2D.setTransform(originTransform);
        g2D.translate(HEX_X_RADIUS, HEX_Y_RADIUS);
        drawScenarios(g2D);
        g2D.setTransform(originTransform);
        g2D.translate(HEX_X_RADIUS, HEX_Y_RADIUS);
        drawForces(g2D);

        g2D.setTransform(initialTransform);
        // Enable this code to get a little blue dot wherever you click on the StratCon map. This is useful to
        // confirm whether mouse-clicks are being recognized.
        //        if (clickedPoint != null) {
        //            g2D.setColor(BLUE);
        //            g2D.drawRect((int) clickedPoint.getX(), (int) clickedPoint.getY(), 2, 2);
        //        }
    }

    /**
     * Worker function that generates a hex polygon
     */
    private Polygon generateGraphHex() {
        Polygon graphHex = new Polygon();
        int xRadius = HEX_X_RADIUS;
        int yRadius = HEX_Y_RADIUS;

        graphHex.addPoint(-xRadius / 2, -yRadius);
        graphHex.addPoint(-xRadius, 0);
        graphHex.addPoint(-xRadius / 2, yRadius);
        graphHex.addPoint(xRadius / 2, yRadius);
        graphHex.addPoint(xRadius, 0);
        graphHex.addPoint(xRadius / 2, -yRadius);

        return graphHex;
    }

    /**
     * This method contains a dirty secret hack, described on line 253-258 The point of it is to draw all the hexes for
     * the board. If it's a "dry run", we don't actually draw the hexes, we just pretend to until we "draw" one that
     * encompasses the clicked point.
     *
     * @param g2D         - graphics object on which to draw
     * @param drawHexType - whether to draw the hex backgrounds, hex outlines or a dry run for click detection
     */
    private boolean drawHexes(Graphics2D g2D, DrawHexType drawHexType) {
        Polygon graphHex = generateGraphHex();
        graphHex.translate(HEX_X_RADIUS,
              HEX_Y_RADIUS); // I don't remember why, but omitting this causes facilities etc. to appear
        // displaced
        boolean pointFound = false;

        Point translatedClickedPoint = null;

        // this was derived somewhat experimentally
        // the clicked point always seems a little off, so we
        // a) apply the current transform to it, prior to drawing all the hexes
        // b) subtract an additional Y_RADIUS x 2 (Y_DIAMETER)
        // this gets us the point within the clicked hex
        // it's probably finicky, so any major changes to the rendering mechanism will
        // likely break click detection
        if (clickedPoint != null) {
            translatedClickedPoint = (Point) clickedPoint.clone();

            // since we have the possibility of scrolling, we need to convert the on-screen
            // clicked coordinates
            // to on-board coordinates. Thankfully, SwingUtilities provides the main
            // computational ability for that
            translatedClickedPoint = SwingUtilities.convertPoint(this, translatedClickedPoint, this.getParent());
            translatedClickedPoint.translate((int) getVisibleRect().getX(), (int) getVisibleRect().getY());
            translatedClickedPoint.translate(0, -HEX_Y_RADIUS);

            // useful for graphics coords debugging
            // g2D.setColor(Color.ORANGE);
            // g2D.drawString(translatedClickedPoint.getX() + ", " +
            // translatedClickedPoint.getY(), (int) clickedPoint.getX(), (int)
            // clickedPoint.getY());
        }

        Font pushFont = g2D.getFont();
        Font newFont = pushFont.deriveFont(BOLD, pushFont.getSize());
        g2D.setFont(newFont);

        boolean trackRevealed = currentTrack.hasActiveTrackReveal();

        for (int x = 0; x < currentTrack.getWidth(); x++) {
            for (int y = 0; y < currentTrack.getHeight(); y++) {
                StratConCoords currentCoords = new StratConCoords(x, y);

                if (drawHexType == DrawHexType.Outline) {
                    g2D.setColor(BLACK);

                    // for legacy campaigns with no terrain data or if there's an un/poorly-defined
                    // terrain type
                    // we'll retain drawing a hex outline
                    BufferedImage biomeImage = getImage(currentTrack.getTerrainTile(currentCoords),
                          ImageType.TerrainTile);

                    if (biomeImage == null) {
                        g2D.drawPolygon(graphHex);
                    }
                } else if (drawHexType == DrawHexType.Hex) {
                    // note: this polygon fill is necessary for click detection, so it must be left
                    // here
                    g2D.setColor(Color.DARK_GRAY);
                    g2D.fillPolygon(graphHex);

                    // draw a hex image if we've got one
                    BufferedImage biomeImage = getImage(currentTrack.getTerrainTile(currentCoords),
                          ImageType.TerrainTile);

                    if (biomeImage != null) {
                        // left-most and topmost point; experimentally adjusted to avoid empty space in
                        // the top left
                        g2D.drawImage(biomeImage, null, graphHex.xpoints[1], graphHex.ypoints[0]);
                    }

                    // draw fog of war if applicable
                    if (!trackRevealed && !currentTrack.coordsRevealed(x, y)) {
                        BufferedImage fogOfWarLayerImage = getImage(StratConBiomeManifest.FOG_OF_WAR,
                              ImageType.TerrainTile);
                        if (fogOfWarLayerImage != null) {
                            fogOfWarLayerImage = addTintToBufferedImage(fogOfWarLayerImage, BLUE);
                            g2D.drawImage(fogOfWarLayerImage, null, graphHex.xpoints[1], graphHex.ypoints[0]);
                        }

                        // needs a little more contrast between revealed and un-revealed hexes
                        var push = g2D.getComposite();
                        g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                        g2D.fillPolygon(graphHex);
                        g2D.setComposite(push);
                    }

                    // useful for graphics coords debugging
                    // g2D.setColor(Color.pink);
                    // g2D.drawString(graphHex.getBounds().getX() + ", " +
                    // graphHex.getBounds().getY(), (int) graphHex.getBounds().getX(), (int)
                    // graphHex.getBounds().getY());
                    // g2D.setColor(Color.DARK_GRAY);

                    // draw selected hex and also detect the clicked hex
                    if ((translatedClickedPoint != null) && graphHex.contains(translatedClickedPoint)) {
                        BufferedImage selectedHexImage = getImage(StratConBiomeManifest.HEX_SELECTED,
                              ImageType.TerrainTile);
                        if (selectedHexImage != null) {
                            g2D.drawImage(selectedHexImage, null, graphHex.xpoints[1], graphHex.ypoints[0]);
                        } else {
                            g2D.setColor(Color.WHITE);
                            BasicStroke s = new BasicStroke((float) 8.0);
                            Stroke push = g2D.getStroke();
                            g2D.setStroke(s);
                            g2D.drawPolygon(graphHex);
                            g2D.setStroke(push);
                        }

                        boardState.selectedX = x;
                        boardState.selectedY = y;
                        pointFound = true;
                    }
                } else if (drawHexType == DrawHexType.DryRun) {
                    if ((translatedClickedPoint != null) && graphHex.contains(translatedClickedPoint)) {
                        boardState.selectedX = x;
                        boardState.selectedY = y;
                        pointFound = true;
                    }
                }

                // here we draw the coordinate labels
                if (drawHexType == DrawHexType.Hex) {
                    g2D.setColor(MekHQ.getMHQOptions().getStratConHexCoordForeground());
                    g2D.drawString(currentCoords.toBTString(),
                          graphHex.xpoints[0] + (HEX_X_RADIUS / 5),
                          graphHex.ypoints[0] + ((int) (g2D.getFontMetrics().getHeight() / 1.25)));
                }

                int[] downwardVector = getDownwardYVector();
                graphHex.translate(downwardVector[0], downwardVector[1]);
            }

            int[] translationVector = getRightAndUpVector(x % 2 == 0);
            graphHex.translate(translationVector[0], translationVector[1]);
        }

        g2D.setFont(pushFont);

        return pointFound;
    }

    private BufferedImage getFacilityImage(StratConFacility facility) {
        String imageKeyPrefix = facility.getOwner() == Allied ?
                                      StratConBiomeManifest.FACILITY_ALLIED :
                                      StratConBiomeManifest.FACILITY_HOSTILE;
        String imageKey = imageKeyPrefix + facility.getFacilityType().name();

        return getImage(imageKey, ImageType.Facility);
    }

    /**
     * Retrieves a buffered image from a file given a key into the config file (StratConBiomeManifest.xml)
     */
    private BufferedImage getImage(String imageKey, ImageType imageType) {
        if (imageCache.containsKey(imageKey)) {
            return imageCache.get(imageKey);
        }

        String imageName = switch (imageType) {
            case TerrainTile -> StratConBiomeManifest.getInstance().getBiomeImage(imageKey);
            case Facility -> StratConBiomeManifest.getInstance().getFacilityImage(imageKey);
        };

        if (imageName == null) {
            return null;
        }

        File biomeImageFile = new File(imageName);
        BufferedImage image;

        try {
            image = ImageIO.read(biomeImageFile);
        } catch (Exception e) {
            logger.error("Unable to load image: {} with ID '{}'", imageName, imageKey);
            return null;
        }

        BufferedImage scaledImage = ImageUtil.getScaledImage(image, HEX_X_RADIUS * 2, HEX_Y_RADIUS * 2);

        imageCache.put(imageKey, scaledImage);
        return scaledImage;
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

        Polygon graphHex = generateGraphHex();

        boolean trackRevealed = currentTrack.hasActiveTrackReveal();

        for (int x = 0; x < currentTrack.getWidth(); x++) {
            for (int y = 0; y < currentTrack.getHeight(); y++) {
                StratConCoords currentCoords = new StratConCoords(x, y);
                StratConScenario scenario = currentTrack.getScenario(currentCoords);

                // if there's a scenario here that has a deployment/battle date
                // or if there's a scenario here and the hex has been revealed
                // or if there's a scenario here, and we've gm-revealed everything
                if ((scenario != null) &&
                          ((scenario.getDeploymentDate() != null) ||
                                 (scenario.isStrategicObjective() &&
                                        currentTrack.getRevealedCoords().contains(currentCoords)) ||
                                 currentTrack.isGmRevealed() ||
                                 trackRevealed)) {
                    g2D.setColor(MekHQ.getMHQOptions().getFontColorNegative());

                    BufferedImage scenarioImage = getImage(StratConBiomeManifest.FORCE_HOSTILE, ImageType.TerrainTile);
                    if (scenarioImage != null) {
                        g2D.drawImage(scenarioImage, null, graphHex.xpoints[1], graphHex.ypoints[0]);
                    } else {
                        g2D.drawPolygon(scenarioMarker);
                        g2D.drawPolygon(scenarioMarker2);
                    }

                    if (currentTrack.getFacility(currentCoords) == null) {
                        drawTextEffect(g2D, scenarioMarker, scenario.getName(), currentCoords);
                    } else if (currentTrack.getFacility(currentCoords).getOwner() == Allied) {
                        drawTextEffect(g2D, scenarioMarker, "Under Attack!", currentCoords);
                    }
                }

                int[] downwardVector = getDownwardYVector();
                scenarioMarker.translate(downwardVector[0], downwardVector[1]);
                scenarioMarker2.translate(downwardVector[0], downwardVector[1]);
                graphHex.translate(downwardVector[0], downwardVector[1]);
            }

            int[] translationVector = getRightAndUpVector(x % 2 == 0);
            scenarioMarker.translate(translationVector[0], translationVector[1]);
            scenarioMarker2.translate(translationVector[0], translationVector[1]);
            graphHex.translate(translationVector[0], translationVector[1]);
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

        Polygon graphHex = generateGraphHex();

        boolean trackRevealed = currentTrack.hasActiveTrackReveal();

        for (int x = 0; x < currentTrack.getWidth(); x++) {
            for (int y = 0; y < currentTrack.getHeight(); y++) {
                StratConCoords currentCoords = new StratConCoords(x, y);
                StratConFacility facility = currentTrack.getFacility(currentCoords);

                if ((facility != null) && (facility.isVisible() || trackRevealed || currentTrack.isGmRevealed())) {
                    g2D.setColor(facility.getOwner() == Allied ? Color.CYAN : Color.RED);

                    BufferedImage facilityImage = getFacilityImage(facility);

                    // draw the image if we can find one.
                    // Note: we track our current position using the facility marker, so it cannot
                    // be removed entirely
                    if (facilityImage != null) {
                        g2D.drawImage(facilityImage, null, graphHex.xpoints[1], graphHex.ypoints[0]);
                    } else {
                        g2D.drawPolygon(facilityMarker);
                    }

                    drawTextEffect(g2D, facilityMarker, facility.getFormattedDisplayableName(), currentCoords);
                }

                int[] downwardVector = getDownwardYVector();
                facilityMarker.translate(downwardVector[0], downwardVector[1]);
                graphHex.translate(downwardVector[0], downwardVector[1]);
            }

            int[] translationVector = getRightAndUpVector(x % 2 == 0);
            facilityMarker.translate(translationVector[0], translationVector[1]);
            graphHex.translate(translationVector[0], translationVector[1]);
        }
    }

    /**
     * Worker function to render formation icons to the given surface.
     */
    private void drawForces(Graphics2D g2D) {
        int xRadius = HEX_X_RADIUS / 3;
        int yRadius = HEX_Y_RADIUS / 3;

        Shape forceMarker = new Ellipse2D.Double(-xRadius, -yRadius, xRadius * 2.0, yRadius * 2.0);

        Polygon graphHex = generateGraphHex();

        for (int x = 0; x < currentTrack.getWidth(); x++) {
            for (int y = 0; y < currentTrack.getHeight(); y++) {
                StratConCoords currentCoords = new StratConCoords(x, y);

                if (currentTrack.getAssignedCoordForces().containsKey(currentCoords)) {
                    for (int forceID : currentTrack.getAssignedCoordForces().get(currentCoords)) {
                        String forceName;
                        try {
                            Formation formation = campaign.getFormation(forceID);
                            forceName = formation.getName();
                        } catch (Exception e) {
                            // If we can't successfully fetch the Force, there is no point trying
                            // to draw it on the map.
                            logger.error("Failed to fetch force from ID {}", forceID);
                            continue;
                        }

                        g2D.setColor(Color.GREEN);

                        BufferedImage forceImage = getImage(StratConBiomeManifest.FORCE_FRIENDLY,
                              ImageType.TerrainTile);
                        if (forceImage != null) {
                            g2D.drawImage(forceImage, null, graphHex.xpoints[1], graphHex.ypoints[0]);
                        } else {
                            g2D.draw(forceMarker);
                        }

                        Font currentFont = g2D.getFont();
                        Font newFont = currentFont.deriveFont(Collections.singletonMap(TextAttribute.WEIGHT,
                              TextAttribute.WEIGHT_BOLD));
                        g2D.setFont(newFont);

                        drawTextEffect(g2D, forceMarker, forceName, currentCoords);

                        g2D.setFont(currentFont);
                    }
                }

                int[] downwardVector = getDownwardYVector();
                AffineTransform ellipseTransform = new AffineTransform();
                ellipseTransform.translate(downwardVector[0], downwardVector[1]);
                graphHex.translate(downwardVector[0], downwardVector[1]);
                forceMarker = ellipseTransform.createTransformedShape(forceMarker);
            }

            int[] translationVector = getRightAndUpVector(x % 2 == 0);

            AffineTransform ellipseTransform = new AffineTransform();
            ellipseTransform.translate(translationVector[0], translationVector[1]);
            graphHex.translate(translationVector[0], translationVector[1]);
            forceMarker = ellipseTransform.createTransformedShape(forceMarker);
        }
    }

    /**
     * Draws some text and line to it from a given polygon. Smart enough not to layer multiple strings on top of each
     * other if they're all drawn in the same hex.
     */
    private void drawTextEffect(Graphics2D g2D, Shape marker, String text, StratConCoords coords) {
        int verticalOffsetIndex = numIconsInHex.getOrDefault(coords, 0);

        double startX = marker.getBounds().getMaxX();
        double startY = marker.getBounds().getMinY();
        double midPointX = startX + HEX_X_RADIUS / 4.0;
        double midPointY = startY - HEX_Y_RADIUS / 4.0 + g2D.getFontMetrics().getHeight() * verticalOffsetIndex;
        double endPointX = midPointX + HEX_X_RADIUS / 2.0;

        g2D.drawLine((int) startX, (int) startY, (int) midPointX, (int) midPointY);
        g2D.drawLine((int) midPointX, (int) midPointY, (int) endPointX, (int) midPointY);

        // Save the original font
        Font originalFont = g2D.getFont();
        Font boldFont = originalFont.deriveFont(BOLD);

        // Set the bold font temporarily for text measurement
        g2D.setFont(boldFont);
        FontMetrics boldFontMetrics = g2D.getFontMetrics();

        // Update the rectangle width based on bold text width
        int rectangleYStart = (int) midPointY - boldFontMetrics.getHeight();
        int rectangleWidth = boldFontMetrics.stringWidth(text.toUpperCase()) + 4;

        // Draw black rectangle
        Color push = g2D.getColor();
        g2D.setColor(BLACK);
        g2D.fillRect((int) endPointX, rectangleYStart, rectangleWidth, boldFontMetrics.getHeight());
        g2D.setColor(push);
        g2D.drawRect((int) endPointX, rectangleYStart, rectangleWidth, boldFontMetrics.getHeight());

        // Draw the string with the bold font
        g2D.drawString(text.toUpperCase(), (int) endPointX + 2, (int) midPointY - 2);

        // Restore the original font
        g2D.setFont(originalFont);

        // Register that we drew text off of this hex
        numIconsInHex.put(coords, ++verticalOffsetIndex);
    }

    /**
     * Returns the translation that we need to make to render the "next downward" hex.
     *
     * @return Two-dimensional array with the first element being the x vector and the second being the y vector
     */
    private int[] getDownwardYVector() {
        return new int[] { 0, HEX_Y_RADIUS * 2 };
    }

    /**
     * Returns the translation that we need to make to move from the bottom of a column to the top of the next column to
     * the right.
     *
     * @param evenColumn Whether the column we're currently in is odd or even
     *
     * @return Two-dimensional array with the first element being the x vector and the second being the y vector
     */
    private int[] getRightAndUpVector(boolean evenColumn) {
        int yRadius = HEX_Y_RADIUS;

        int yTranslation = currentTrack.getHeight() * yRadius * 2;
        if (evenColumn) {
            yTranslation += yRadius;
        } else {
            yTranslation -= yRadius;
        }

        return new int[] { (int) Math.floor(HEX_X_RADIUS * 1.5), -yTranslation };
    }

    /**
     * Go to the origin of the hex board and reset the scaling.
     */
    private void performInitialTransform(Graphics2D g2D) {
        g2D.translate(0, HEX_Y_RADIUS);
        float scale = 1f;
        g2D.scale(scale, scale);
    }

    /**
     * Worker function that takes the current clicked point and a graphics 2D object and detects which hex was clicked
     * by doing a dry run hex render.
     * <p>
     * Dependent upon clickedPoint being set and having an active graphics object for this class.
     * <p>
     * Side effects: the dry run sets the boardState clicked hex coordinates.
     *
     * @return Whether the clicked point was found on the hex board
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
                infoArea.setText(buildSelectedHexInfo(campaign));
            }

            repaint();
            // right button generally pops up a context menu
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            clickedPoint = e.getPoint();
            detectClickedHex();

            StratConCoords selectedCoords = boardState.getSelectedCoords();
            if (selectedCoords == null) {
                return;
            }

            repaint();
            buildRightClickMenu(selectedCoords);
            rightClickMenu.show(this, e.getX(), e.getY());
        }
    }

    public StratConScenario getSelectedScenario() {
        return currentTrack.getScenario(boardState.getSelectedCoords());
    }

    public StratConTrackState getCurrentTrack() {
        return currentTrack;
    }

    public void setCurrentTrack(StratConTrackState track) {
        currentTrack = track;
    }

    public StratConCoords getSelectedCoords() {
        return boardState.getSelectedCoords();
    }

    public void setSelectedCoords(StratConCoords coords) {
        boardState.setSelectedCoords(coords);
    }

    /**
     * Worker function that outputs html representing the status of a selected hex, containing info such as whether it's
     * been revealed, assigned forces, scenarios, facilities, etc.
     */
    private String buildSelectedHexInfo(Campaign campaign) {
        StringBuilder infoBuilder = new StringBuilder();
        infoBuilder.append("<html><br/>");

        infoBuilder.append("<b>Average Temperature:</b> ");
        infoBuilder.append(currentTrack.getTemperature());
        infoBuilder.append("&deg;C<br/>");
        infoBuilder.append("<b>Terrain Type:</b> ");
        infoBuilder.append(currentTrack.getTerrainTile(boardState.getSelectedCoords()));
        infoBuilder.append("<br/>");

        boolean coordsRevealed = currentTrack.hasActiveTrackReveal() ||
                                       currentTrack.getRevealedCoords().contains(boardState.getSelectedCoords());
        if (coordsRevealed) {
            infoBuilder.append("<span color='")
                  .append(ReportingUtilities.getPositiveColor())
                  .append("'><i>Recon Complete</i></span><br/>");
        }

        if (currentTrack.getAssignedCoordForces().containsKey(boardState.getSelectedCoords())) {
            for (int forceID : currentTrack.getAssignedCoordForces().get(boardState.getSelectedCoords())) {
                Formation formation = this.campaign.getFormation(forceID);
                infoBuilder.append(formation.getName()).append(" assigned");

                if (currentTrack.getStickyForces().contains(forceID)) {
                    infoBuilder.append("<i> - remain deployed</i>");
                }

                infoBuilder.append("<br/>")
                      .append("<i>Returns on ")
                      .append(currentTrack.getAssignedForceReturnDates().get(forceID))
                      .append("</i><br/>");
            }
        }

        if (coordsRevealed || currentTrack.isGmRevealed()) {
            StratConFacility facility = currentTrack.getFacility(boardState.getSelectedCoords());

            if ((facility != null) && (facility.getFacilityType() != null)) {
                if (facility.isStrategicObjective()) {
                    infoBuilder.append(String.format("<br/><span color='%s'>Contract objective located</span>",
                          facility.getOwner() == Allied ?
                                ReportingUtilities.getPositiveColor() :
                                ReportingUtilities.getNegativeColor()));
                }
                infoBuilder.append("<span color='")
                      .append(facility.getOwner() == Allied ?
                                    ReportingUtilities.getPositiveColor() :
                                    ReportingUtilities.getNegativeColor())
                      .append("'>")
                      .append("<br/>")
                      .append(facility.getFormattedDisplayableName());

                if (facility.getUserDescription() != null) {
                    infoBuilder.append("<br/>").append(facility.getUserDescription());
                }

                infoBuilder.append("<span>");
            }

        } else {
            infoBuilder.append("<span color='")
                  .append(MekHQ.getMHQOptions().getFontColorNegative())
                  .append("'><i>Recon Incomplete</i></span>");
        }
        infoBuilder.append("<br/>");

        StratConScenario selectedScenario = getSelectedScenario();
        if (selectedScenario != null) {
            AtBDynamicScenario backingScenario = selectedScenario.getBackingScenario();

            if (coordsRevealed || !backingScenario.isCloaked() || currentTrack.isGmRevealed()) {
                infoBuilder.append(selectedScenario.getInfo(campaign));
            }
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

        public StratConCoords getSelectedCoords() {
            if ((selectedX == null) || (selectedY == null)) {
                return null;
            } else {
                return new StratConCoords(selectedX, selectedY);
            }
        }

        public void setSelectedCoords(StratConCoords coords) {
            selectedX = coords.getX();
            selectedY = coords.getY();
        }
    }

    /**
     * Handles action events triggered by various StratCon-related commands from the right-click context menu. This
     * method processes user interactions to update the game state, scenarios, facilities, and UI elements based on the
     * selected command and inputs from the context menu.
     *
     * <p>The supported commands and their effects are as follows:</p>
     * <ul>
     *   <li><b>{@code RIGHT_CLICK_COMMAND_MANAGE_FORCES}:</b> Displays the force management UI for the selected coordinates.
     *       <ul>
     *           <li>If no scenario exists at the selected coordinates, the force management UI is directly displayed.</li>
     *           <li>If a scenario exists, it only displays the UI if the scenario is unresolved.</li>
     *       </ul>
     *   </li>
     *   <li><b>{@code RIGHT_CLICK_COMMAND_MANAGE_SCENARIO}:</b> Displays the scenario wizard with the current scenario at the
     *       selected coordinates if the scenario's state is {@code PRIMARY_FORCES_COMMITTED}.</li>
     *   <li><b>{@code RIGHT_CLICK_COMMAND_REVEAL_TRACK}:</b> Toggles the "GM revealed" state for the current track and updates
     *       the menu text to reflect the state ("Hide Track" or "Reveal Track").</li>
     *   <li><b>{@code RIGHT_CLICK_COMMAND_STICKY_FORCE}:</b> Toggles the sticky force assignment for a given force ID at the
     *       selected track. When toggled:</li>
     *           <li>-- If selected, the force is added to the track as sticky.</li>
     *           <li>-- If deselected, the force is removed from the track's sticky forces.</li>
     *   <li><b>{@code RIGHT_CLICK_COMMAND_REMOVE_FACILITY}:</b> Deletes the facility present at the selected coordinates.</li>
     *   <li><b>{@code RIGHT_CLICK_COMMAND_CAPTURE_FACILITY}:</b> Changes the ownership of the facility at the selected coordinates
     *       to a different faction or player, as per the rules defined in {@link StratConRulesManager}.</li>
     *   <li><b>{@code RIGHT_CLICK_COMMAND_ADD_FACILITY}:</b> Adds a new facility to the selected coordinates. The facility's
     *       properties (visibility, type, etc.) are copied from the provided source facility.</li>
     *   <li><b>{@code RIGHT_CLICK_COMMAND_REMOVE_SCENARIO}:</b> Deletes the currently selected scenario from the campaign.</li>
     * </ul>
     *
     * @param evt the {@link ActionEvent} representing the user's action. Contains information about the triggering
     *            source and command (e.g., which menu item was selected).
     *
     *            <p><b>Behavior:</b></p>
     *            <ul>
     *              <li>The method retrieves the {@link StratConCoords} currently selected by the user, and performs actions based on the
     *                  provided command string in the event.</li>
     *              <li>The scenarios, forces, and facilities of the {@link #currentTrack} are modified based on the command type, and
     *                  updates are visually reflected in the UI.</li>
     *              <li>If a UI-related command is processed (e.g., displaying the scenario wizard or force assignment UI), the appropriate
     *                  UI components are updated and made visible to the user.</li>
     *            </ul>
     *
     *            <p><b>General Information:</b> If no valid {@link StratConCoords} are selected at the time of the event,
     *            the method will terminate with no further action. Certain commands (e.g., {@code RIGHT_CLICK_COMMAND_REVEAL_TRACK},
     *            {@code RIGHT_CLICK_COMMAND_ADD_FACILITY}) require valid coordinates or source properties to execute successfully.</p>
     *
     *            <p>If no specific actions from the above list are matched (no corresponding `case`), the method performs no effect.</p>
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
        StratConCoords selectedCoords = boardState.getSelectedCoords();
        if (selectedCoords == null) {
            return;
        }

        boolean isPrimaryForce = false;
        StratConScenario selectedScenario = currentTrack.getScenario(selectedCoords);
        switch (evt.getActionCommand()) {
            case RIGHT_CLICK_COMMAND_MANAGE_FORCES:
                if (selectedScenario == null) {
                    assignmentUI.display(campaign, campaignState, selectedCoords, false);
                    assignmentUI.setVisible(true);
                    isPrimaryForce = true;
                }

                if (selectedScenario != null) {
                    ScenarioState currentState = selectedScenario.getCurrentState();

                    if (currentState.equals(UNRESOLVED)) {
                        AtBDynamicScenario backingScenario = selectedScenario.getBackingScenario();
                        boolean restrictToSingleForce = backingScenario != null &&
                                                              backingScenario.getStratConScenarioType()
                                                                    .isOfficialChallenge();
                        assignmentUI.display(campaign, campaignState, selectedCoords, restrictToSingleForce);
                        assignmentUI.setVisible(true);
                        isPrimaryForce = true;
                    }
                }

                // Let's reload the scenario in case it updated
                selectedScenario = currentTrack.getScenario(selectedCoords);

                if (selectedScenario != null && selectedScenario.getCurrentState() == PRIMARY_FORCES_COMMITTED) {
                    scenarioWizard.setCurrentScenario(currentTrack.getScenario(selectedCoords),
                          currentTrack,
                          campaignState,
                          isPrimaryForce);

                    scenarioWizard.toFront();
                    scenarioWizard.setVisible(true);
                }

                setCommitForces(false);
                break;
            case RIGHT_CLICK_COMMAND_MANAGE_SCENARIO:
                // It's possible a scenario may have been placed when deploying the force, so we
                // need to recheck
                selectedScenario = currentTrack.getScenario(selectedCoords);
                if (selectedScenario != null && selectedScenario.getCurrentState() == PRIMARY_FORCES_COMMITTED) {
                    scenarioWizard.setCurrentScenario(currentTrack.getScenario(selectedCoords),
                          currentTrack,
                          campaignState,
                          false);

                    scenarioWizard.toFront();
                    scenarioWizard.setVisible(true);
                }
                break;
            case RIGHT_CLICK_COMMAND_REVEAL_TRACK:
                currentTrack.setGmRevealed(!currentTrack.isGmRevealed());
                menuItemGMReveal.setText(currentTrack.isGmRevealed() ? "Hide Track" : "Reveal Track");
                break;
            case RIGHT_CLICK_COMMAND_STICKY_FORCE:
                JCheckBoxMenuItem source = (JCheckBoxMenuItem) evt.getSource();
                int forceID = (int) source.getClientProperty(RIGHT_CLICK_COMMAND_STICKY_FORCE_ID);

                if (source.isSelected()) {
                    currentTrack.addStickyForce(forceID);
                } else {
                    currentTrack.removeStickyForce(forceID);
                }

                break;
            case RIGHT_CLICK_COMMAND_REMOVE_FACILITY:
                currentTrack.removeFacility(selectedCoords);
                break;
            case RIGHT_CLICK_COMMAND_CAPTURE_FACILITY:
                StratConRulesManager.switchFacilityOwner(currentTrack.getFacility(selectedCoords));
                break;
            case RIGHT_CLICK_COMMAND_ADD_FACILITY:
                JMenuItem eventSource = (JMenuItem) evt.getSource();
                StratConFacility facility = (StratConFacility) eventSource.getClientProperty(
                      RIGHT_CLICK_COMMAND_ADD_FACILITY);
                StratConFacility newFacility = facility.clone();
                newFacility.setVisible(currentTrack.getRevealedCoords().contains(selectedCoords));
                currentTrack.addFacility(selectedCoords, newFacility);
                break;
            case RIGHT_CLICK_COMMAND_REMOVE_SCENARIO:
                StratConScenario scenario = getSelectedScenario();

                if (scenario != null) {
                    campaign.removeScenario(scenario.getBackingScenario());
                }
                break;
            case RIGHT_CLICK_COMMAND_RESET_DEPLOYMENT:
                StratConScenario scenarioToReset = getSelectedScenario();

                if (scenarioToReset != null) {
                    scenarioToReset.resetScenario(campaign);
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

    public boolean isCommitForces() {
        return commitForces;
    }

    public void setCommitForces(boolean commitForces) {
        this.commitForces = commitForces;
    }
}
