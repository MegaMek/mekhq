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
import static java.lang.Math.max;
import static megamek.utilities.ImageUtilities.addTintToBufferedImage;
import static mekhq.campaign.digitalGM.stratCon.StratConScenario.ScenarioState.PRIMARY_FORCES_COMMITTED;
import static mekhq.campaign.digitalGM.stratCon.StratConScenario.ScenarioState.UNRESOLVED;
import static mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment.Allied;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getAmazingColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.*;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.util.ImageUtil;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer.ResizeImpact;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConScenario.ScenarioState;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest;
import mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest.ImageType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityFactory;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConHexGeometry;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.gui.dialog.StratConTerrainPaintDialog;
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

    /** Horizontal pixel spacing between hex columns, matching the map layout, used to place road lines. */
    private static final int ROAD_STEP_X = (int) Math.floor(HEX_X_RADIUS * 1.5);
    private static final Color ROAD_COLOR = new Color(110, 75, 45, 205);
    private static final float ROAD_STROKE_WIDTH = 3.5f;

    /**
     * Cartographic casing: a dark outline stroked under the road fill, so the road stays visible on terrain close to
     * its own color (dusty badlands especially). The casing carries the contrast on light hexes, the fill on dark
     * ones.
     */
    private static final Color ROAD_CASING_COLOR = new Color(40, 26, 14, 230);
    private static final float ROAD_CASING_STROKE_WIDTH = ROAD_STROKE_WIDTH + 2.5f;

    /** Zoom bounds and the multiplicative step applied per mouse-wheel notch. */
    private static final double MIN_SCALE = 0.5;
    private static final double MAX_SCALE = 2.0;
    private static final double ZOOM_STEP = 1.1;

    /**
     * How far (in screen pixels) a left-mouse press may move before it is treated as a pan rather than a hex-selection
     * click.
     */
    private static final int DRAG_THRESHOLD = 5;

    /**
     * Opacity of an unscouted hex's terrain (and city sprite) under the alternate fog-of-war display: dim enough to be
     * unmistakably unscouted, but with the ground still just readable.
     */
    private static final float ALTERNATE_FOG_ALPHA = 0.25f;

    private static final String RIGHT_CLICK_COMMAND_MANAGE_FORCES = "ManageForces";
    private static final String RIGHT_CLICK_COMMAND_MANAGE_SCENARIO = "ManageScenario";
    private static final String RIGHT_CLICK_COMMAND_STICKY_FORCE = "StickyForce";
    private static final String RIGHT_CLICK_COMMAND_STICKY_FORCE_ID = "StickyForceID";
    private static final String RIGHT_CLICK_COMMAND_REMOVE_FACILITY = "RemoveFacility";
    private static final String RIGHT_CLICK_COMMAND_CAPTURE_FACILITY = "CaptureFacility";
    private static final String RIGHT_CLICK_COMMAND_ADD_FACILITY = "AddFacility";
    private static final String RIGHT_CLICK_COMMAND_REMOVE_SCENARIO = "RemoveScenario";
    private static final String RIGHT_CLICK_COMMAND_RESET_DEPLOYMENT = "ResetDeployment";
    private static final String RIGHT_CLICK_COMMAND_ADD_CITY = "AddCity";

    private static final String RESOURCE_BUNDLE = "mekhq.resources.AtBStratCon";

    /** Upper bound offered when resizing a sector by hand, to keep a stray keystroke from generating a huge map. */
    private static final int MAX_SECTOR_SIZE = 60;
    private static final String RIGHT_CLICK_COMMAND_REMOVE_CITY = "RemoveCity";

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

    /** Current zoom factor applied to the hex map. 1.0 == no zoom. */
    private double scale = 1.0;

    // Left-mouse-drag panning state, tracked in screen coordinates so it is stable as the view scrolls beneath us.
    private Point panDragStartScreen;
    private Point panDragLastScreen;
    private boolean panning;

    // GM terrain painting. While paintTerrain is set the panel is in paint mode: left-click and drag lay terrain down
    // instead of selecting and panning. The expensive consequences of an edit (re-laying roads, moving anyone who ended
    // up in the water) are deferred to the end of a stroke rather than run per hex.
    private String paintTerrain;
    private StratConTerrainPaintDialog terrainPaintDialog;
    private int paintBrushRadius;
    private boolean paintStrokeChangedTerrain;

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
        // The selected-hex info is drawn as a HUD over the (dark) map, so its default text reads white; the HTML's own
        // colored spans (recon/objective cues) still show through.
        this.infoArea.setForeground(Color.WHITE);
        this.infoArea.setOpaque(false);

        assignmentUI = new TrackForceAssignmentUI(this);
        assignmentUI.setVisible(false);

        MapInputHandler inputHandler = new MapInputHandler();
        addMouseListener(inputHandler);
        addMouseMotionListener(inputHandler);
        addMouseWheelListener(inputHandler);
    }

    /**
     * Handles map navigation input: left-mouse-drag to pan (bounded by the map edges via the enclosing viewport) and
     * mouse-wheel to zoom in/out centered on the cursor, matching the interstellar map's controls. A left-mouse press
     * that does not move beyond {@link #DRAG_THRESHOLD} is treated as a hex-selection click instead of a pan.
     */
    private class MapInputHandler extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                // In paint mode the left button is the brush, so it neither selects nor pans.
                if (isPaintingTerrain()) {
                    paintAt(e.getPoint());
                    return;
                }

                panDragStartScreen = e.getLocationOnScreen();
                panDragLastScreen = e.getLocationOnScreen();
                panning = false;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (!SwingUtilities.isLeftMouseButton(e)) {
                return;
            }

            if (isPaintingTerrain()) {
                paintAt(e.getPoint());
                return;
            }

            if (panDragStartScreen == null) {
                return;
            }

            Point current = e.getLocationOnScreen();

            // Don't move the map until the cursor travels past the click threshold; below it the gesture is still a
            // hex-selection click, not a pan.
            if (!panning) {
                if ((Math.abs(current.x - panDragStartScreen.x) <= DRAG_THRESHOLD) &&
                          (Math.abs(current.y - panDragStartScreen.y) <= DRAG_THRESHOLD)) {
                    return;
                }

                // Transition into panning; re-anchor here so the first pan step doesn't jump by the threshold distance.
                panning = true;
                panDragLastScreen = current;
                return;
            }

            panBy(current.x - panDragLastScreen.x, current.y - panDragLastScreen.y);
            panDragLastScreen = current;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (isPaintingTerrain() && SwingUtilities.isLeftMouseButton(e)) {
                // The stroke is over, so pay for it once rather than once per hex.
                finishPaintStroke();
                return;
            }

            mouseReleasedHandler(e);
            panDragStartScreen = null;
            panDragLastScreen = null;
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            zoomAt(e);
            e.consume();
        }
    }

    /**
     * Handler for when a specific track is selected - switches rendering to that track.
     */
    public void selectTrack(StratConCampaignState campaignState, StratConTrackState track) {
        // The palette paints wherever you drag, so it must not outlive the sector it was opened for.
        closeTerrainPaintDialog();

        this.campaignState = campaignState;
        currentTrack = track;

        // clear hex selection
        boardState.selectedX = null;
        boardState.selectedY = null;
        infoArea.setText(buildSelectedHexInfo(campaign));

        repaint();
    }

    /**
     * Scouts (permanently reveals) every hex in the currently selected sector, then repaints. Unlike the GM sector
     * reveal, this marks the hexes as revealed, so their contents stay visible afterward.
     */
    public void scoutCurrentSector() {
        if (currentTrack == null) {
            return;
        }

        for (int x = 0; x < currentTrack.getWidth(); x++) {
            for (int y = 0; y < currentTrack.getHeight(); y++) {
                currentTrack.getRevealedCoords().add(new StratConCoords(x, y));
            }
        }

        infoArea.setText(buildSelectedHexInfo(campaign));
        repaint();
    }

    /**
     * GM tool: clears the current sector and regenerates its terrain (and cities/roads), then repaints. Scenarios,
     * facilities, and assigned forces are left in place.
     */
    public void regenerateCurrentSector() {
        if ((currentTrack == null) || (campaignState == null)) {
            return;
        }

        StratConContractInitializer.regenerateTrack(currentTrack, campaignState.getContract(), campaign);
        infoArea.setText(buildSelectedHexInfo(campaign));
        repaint();
    }

    /**
     * GM tool: opens the terrain palette, putting the map into paint mode until the palette is closed.
     */
    public void openTerrainPaintDialog() {
        if ((currentTrack == null) || (campaignState == null)) {
            return;
        }

        // One palette at a time: a second would fight the first over the selected terrain and brush.
        if (terrainPaintDialog != null) {
            terrainPaintDialog.toFront();
            return;
        }

        terrainPaintDialog = new StratConTerrainPaintDialog(this);
        terrainPaintDialog.setVisible(true);
    }

    /**
     * Closes the terrain palette and leaves paint mode, if it is open. Called when the sector changes underneath it, so
     * a brush aimed at one sector cannot be dragged across another.
     */
    private void closeTerrainPaintDialog() {
        if (terrainPaintDialog != null) {
            terrainPaintDialog.dispose();
        }
    }

    /**
     * GM tool: prompts for new sector dimensions and applies them, growing or shrinking at the right and bottom edges.
     * Warns first when the new size would displace anything.
     */
    public void resizeSector() {
        if ((currentTrack == null) || (campaignState == null)) {
            return;
        }

        // Floor matches what generation guarantees: several placers assume a sector big enough to hold a feature, and
        // a hand-shrunk 1x1 sector would crash the next regeneration.
        int minimum = StratConContractInitializer.MIN_SECTOR_DIMENSION;
        JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(max(currentTrack.getWidth(), minimum),
              minimum, MAX_SECTOR_SIZE, 1));
        JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(max(currentTrack.getHeight(), minimum),
              minimum, MAX_SECTOR_SIZE, 1));

        JPanel prompt = new JPanel(new GridLayout(0, 2, UIUtil.scaleForGUI(5), UIUtil.scaleForGUI(5)));
        prompt.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "resizeSector.width.label")));
        prompt.add(widthSpinner);
        prompt.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "resizeSector.height.label")));
        prompt.add(heightSpinner);

        if (JOptionPane.showConfirmDialog(this,
              prompt,
              getTextAt(RESOURCE_BUNDLE, "resizeSector.title"),
              JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }

        int newWidth = (int) widthSpinner.getValue();
        int newHeight = (int) heightSpinner.getValue();

        ResizeImpact impact = StratConContractInitializer.previewResize(currentTrack, newWidth, newHeight);

        // A size with nowhere to put the displaced occupants is refused outright rather than quietly destroying them.
        if (!impact.fits()) {
            JOptionPane.showMessageDialog(this,
                  getFormattedTextAt(RESOURCE_BUNDLE,
                        "resizeSector.tooSmall",
                        impact.displacedOccupants(),
                        impact.freeHexes()),
                  getTextAt(RESOURCE_BUNDLE, "resizeSector.title"),
                  JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Shrinking can displace bases, scenarios, objectives, and deployed forces. None of that is silent: say exactly
        // what will move and let the GM back out.
        if (!impact.isEmpty()) {
            String warning = getFormattedTextAt(RESOURCE_BUNDLE,
                  "resizeSector.warning",
                  impact.facilities(),
                  impact.scenarios(),
                  impact.objectives(),
                  impact.forces());

            if (JOptionPane.showConfirmDialog(this,
                  warning,
                  getTextAt(RESOURCE_BUNDLE, "resizeSector.title"),
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                return;
            }
        }

        StratConContractInitializer.resizeTrack(currentTrack,
              newWidth,
              newHeight,
              campaignState.getContract(),
              campaign);

        boardState.setSelectedCoords(null);
        infoArea.setText(buildSelectedHexInfo(campaign));
        revalidate();
        repaint();
    }

    /** Sets the terrain the brush lays down. Called by the terrain palette. */
    public void setPaintTerrain(String terrain) {
        this.paintTerrain = terrain;
    }

    /** Sets the brush radius in hexes: 0 paints a single hex, 1 paints it and its neighbors, and so on. */
    public void setPaintBrushRadius(int radius) {
        this.paintBrushRadius = radius;
    }

    /**
     * Named for the terrain brush rather than {@code isPainting}, which would sit confusingly beside the
     * package-private {@code JComponent.isPainting} this panel inherits but cannot override - a reader inside the
     * painting code would have no way to tell the two apart.
     *
     * @return {@code true} while the terrain palette is open and clicks paint rather than select.
     */
    public boolean isPaintingTerrain() {
        return paintTerrain != null;
    }

    /** Leaves paint mode, restoring normal selection and panning. Called when the terrain palette closes. */
    public void exitPaintMode() {
        paintTerrain = null;
        terrainPaintDialog = null;
        repaint();
    }

    /** @return the map sprite for a terrain type, for the terrain palette to render alongside the name. */
    public BufferedImage getTerrainImage(String terrainType) {
        return getImage(terrainType, ImageType.TerrainTile);
    }

    /**
     * Lays the current brush down on the hex under the given point. Only the terrain itself changes here; putting the
     * sector back in order is deferred to {@link #finishPaintStroke()} so a drag does not rebuild the road network once
     * per hex.
     */
    private void paintAt(Point point) {
        // detectClickedHex draws a dry run straight through drawHexes, which dereferences the track without a guard of
        // its own - unlike paintComponent, which checks before it ever gets there.
        if (currentTrack == null) {
            return;
        }

        clickedPoint = point;
        if (!detectClickedHex()) {
            return;
        }

        StratConCoords center = boardState.getSelectedCoords();
        if (center == null) {
            return;
        }

        // Grown through the map's own adjacency; see StratConHexGeometry.withinRadius for why a plain coordinate
        // distance gives a lopsided brush here.
        for (StratConCoords coords : StratConHexGeometry.withinRadius(currentTrack, center, paintBrushRadius)) {
            if (!paintTerrain.equals(currentTrack.getTerrainTile(coords))) {
                currentTrack.setTerrainTile(coords, paintTerrain);
                paintStrokeChangedTerrain = true;
            }
        }

        repaint();
    }

    /**
     * Ends a paint stroke, re-settling the sector around the new terrain: flooded cities go, anyone left in the water
     * moves ashore, and the road network is re-laid.
     */
    private void finishPaintStroke() {
        if (!paintStrokeChangedTerrain) {
            return;
        }
        paintStrokeChangedTerrain = false;

        StratConContractInitializer.applyTerrainChange(currentTrack, campaignState.getContract(), campaign);
        infoArea.setText(buildSelectedHexInfo(campaign));
        repaint();
    }

    /**
     * Rebuilds the current sector's road network after a GM edit to its cities or facilities, so the roads keep serving
     * the settlements that are actually there. Facilities are folded in only when the planet's owner holds them, and a
     * road-less sector generator ignores the request entirely.
     */
    private void recalculateRoads() {
        if ((currentTrack == null) || (campaignState == null)) {
            return;
        }

        StratConContractInitializer.connectFacilitiesToRoads(currentTrack, campaignState.getContract(), campaign);
    }

    /**
     * GM tool: un-reveals every hex in the current sector so scouting can be re-tested, then repaints. Open water is
     * re-revealed, since it never holds fog of war.
     */
    public void resetSectorFog() {
        if (currentTrack == null) {
            return;
        }

        currentTrack.getRevealedCoords().clear();
        for (int x = 0; x < currentTrack.getWidth(); x++) {
            for (int y = 0; y < currentTrack.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                if (StratConBiomeManifest.isOceanTerrain(currentTrack.getTerrainTile(coords))) {
                    currentTrack.getRevealedCoords().add(coords);
                }
            }
        }

        infoArea.setText(buildSelectedHexInfo(campaign));
        repaint();
    }

    /**
     * Toggles the current sector's "GM revealed" state, which shows or hides otherwise-hidden objects (cloaked
     * scenarios, invisible facilities, and unscouted hexes). Formerly the "Reveal/Hide Sector" right-click item.
     */
    public void toggleHiddenObjects() {
        if (currentTrack == null) {
            return;
        }

        currentTrack.setGmRevealed(!currentTrack.isGmRevealed());
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
        if (StratConRulesManager.canManuallyDeployAnyForce(coords, currentTrack)) {
            JMenuItem menuItemManageForceAssignments = new JMenuItem();
            menuItemManageForceAssignments.setText(getTextAt(RESOURCE_BUNDLE,
                  "stratConTab.contextMenu.manageDeployment"));
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
                    menuItemManageScenario.setText(getTextAt(RESOURCE_BUNDLE,
                          "stratConTab.contextMenu.manageDeployment"));
                    menuItemManageScenario.setActionCommand(RIGHT_CLICK_COMMAND_MANAGE_FORCES);
                } else {
                    menuItemManageScenario.setText(getTextAt(RESOURCE_BUNDLE,
                          "stratConTab.contextMenu.manageReinforcements"));
                    menuItemManageScenario.setActionCommand(RIGHT_CLICK_COMMAND_MANAGE_SCENARIO);
                }

                menuItemManageScenario.addActionListener(this);
                rightClickMenu.add(menuItemManageScenario);
            }
        }

        if ((currentTrack != null) && currentTrack.getAssignedCoordForces().containsKey(coords)) {
            for (int forceID : currentTrack.getAssignedCoordForces().get(coords)) {
                String forceName = campaign.getPlayerForce().getFormation(forceID).getName();

                JCheckBoxMenuItem stickyForceItem = new JCheckBoxMenuItem();
                stickyForceItem.setText(getFormattedTextAt(RESOURCE_BUNDLE,
                      "stratConTab.contextMenu.remainDeployed",
                      forceName));
                stickyForceItem.setActionCommand(RIGHT_CLICK_COMMAND_STICKY_FORCE);
                stickyForceItem.putClientProperty(RIGHT_CLICK_COMMAND_STICKY_FORCE_ID, forceID);
                stickyForceItem.addActionListener(this);
                stickyForceItem.setSelected(currentTrack.getStickyForces().contains(forceID));
                rightClickMenu.add(stickyForceItem);
            }
        }

        if ((currentTrack != null) && campaign.isGM()) {
            rightClickMenu.addSeparator();

            // "Reveal/Hide Sector" moved to the "Toggle Hidden Objects" button on the StratCon tab's GM button bar.

            if (currentTrack.getFacility(coords) != null) {
                JMenuItem menuItemRemoveFacility = new JMenuItem();
                menuItemRemoveFacility.setText(getTextAt(RESOURCE_BUNDLE, "stratConTab.contextMenu.removeFacility"));
                menuItemRemoveFacility.setActionCommand(RIGHT_CLICK_COMMAND_REMOVE_FACILITY);
                menuItemRemoveFacility.addActionListener(this);
                rightClickMenu.add(menuItemRemoveFacility);

                JMenuItem menuItemSwitchOwner = new JMenuItem();
                menuItemSwitchOwner.setText(getTextAt(RESOURCE_BUNDLE, "stratConTab.contextMenu.switchOwner"));
                menuItemSwitchOwner.setActionCommand(RIGHT_CLICK_COMMAND_CAPTURE_FACILITY);
                menuItemSwitchOwner.addActionListener(this);
                rightClickMenu.add(menuItemSwitchOwner);
            } else {
                JMenu menuItemAddFacility = new JMenu();
                menuItemAddFacility.setText(getTextAt(RESOURCE_BUNDLE, "stratConTab.contextMenu.addFacility"));

                JMenu menuItemAddAlliedFacility = new JMenu();
                menuItemAddAlliedFacility.setText(getTextAt(RESOURCE_BUNDLE, "stratConTab.contextMenu.allied"));
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
                menuItemAddHostileFacility.setText(getTextAt(RESOURCE_BUNDLE, "stratConTab.contextMenu.hostile"));
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

            // City overlay editing: remove an existing city, or add one to any dry hex. Either change recomputes roads.
            if (currentTrack.isCity(coords)) {
                JMenuItem menuItemRemoveCity = new JMenuItem();
                menuItemRemoveCity.setText(getTextAt(RESOURCE_BUNDLE, "stratConTab.contextMenu.removeCity"));
                menuItemRemoveCity.setActionCommand(RIGHT_CLICK_COMMAND_REMOVE_CITY);
                menuItemRemoveCity.addActionListener(this);
                rightClickMenu.add(menuItemRemoveCity);
            } else if (!StratConBiomeManifest.isOceanTerrain(currentTrack.getTerrainTile(coords))) {
                JMenuItem menuItemAddCity = new JMenuItem();
                menuItemAddCity.setText(getTextAt(RESOURCE_BUNDLE, "stratConTab.contextMenu.addCity"));
                menuItemAddCity.setActionCommand(RIGHT_CLICK_COMMAND_ADD_CITY);
                menuItemAddCity.addActionListener(this);
                rightClickMenu.add(menuItemAddCity);
            }

            if (scenario != null) {
                JMenuItem removeScenarioItem = new JMenuItem();
                removeScenarioItem.setText(getTextAt(RESOURCE_BUNDLE, "stratConTab.contextMenu.removeScenario"));
                removeScenarioItem.setActionCommand(RIGHT_CLICK_COMMAND_REMOVE_SCENARIO);
                removeScenarioItem.addActionListener(this);
                rightClickMenu.add(removeScenarioItem);

                JMenuItem resetDeploymentItem = new JMenuItem();
                resetDeploymentItem.setText(getTextAt(RESOURCE_BUNDLE, "stratConTab.contextMenu.resetDeployment"));
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
        drawRoads(g2D);
        g2D.setTransform(originTransform);
        g2D.translate(HEX_X_RADIUS, HEX_Y_RADIUS);
        drawCities(g2D);
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

        // Drawn last, pinned to the corners of the visible viewport, so they stay put as the map scrolls.
        drawSectorEnvironment(g2D);
        drawSelectedHexInfo(g2D);
    }

    /**
     * Draws the selected-hex info (temperature, terrain, recon status, scenario details) as a translucent box pinned to
     * the bottom-right of the visible map area. Skipped when no hex is selected. The content wraps to a bounded width
     * (see {@link #buildSelectedHexInfo}) so long terrain/scenario names do not overflow.
     */
    private void drawSelectedHexInfo(Graphics2D g2D) {
        if ((currentTrack == null) || (boardState.getSelectedCoords() == null)) {
            return;
        }

        Dimension content = infoArea.getPreferredSize();
        if ((content.width <= 0) || (content.height <= 0)) {
            return;
        }
        infoArea.setSize(content);

        int pad = UIUtil.scaleForGUI(8);
        int margin = UIUtil.scaleForGUI(8);
        int arc = UIUtil.scaleForGUI(10);
        int boxWidth = content.width + (pad * 2);
        int boxHeight = content.height + (pad * 2);

        Rectangle visible = getVisibleRect();
        int boxX = (visible.x + visible.width) - boxWidth - margin;
        int boxY = (visible.y + visible.height) - boxHeight - margin;

        g2D.setColor(new Color(0, 0, 0, 190));
        g2D.fillRoundRect(boxX, boxY, boxWidth, boxHeight, arc, arc);
        g2D.setColor(new Color(255, 255, 255, 60));
        g2D.drawRoundRect(boxX, boxY, boxWidth, boxHeight, arc, arc);

        AffineTransform saved = g2D.getTransform();
        g2D.translate(boxX + pad, boxY + pad);
        infoArea.paint(g2D);
        g2D.setTransform(saved);
    }

    /**
     * Draws the sector environment HUD (latitude, average temperature, and the generation profiles) as a translucent
     * box pinned to the top-right of the visible map area. Skipped for legacy sectors that carry no profile data.
     */
    private void drawSectorEnvironment(Graphics2D g2D) {
        if ((currentTrack == null) || (currentTrack.getLatitudeBand() == null)) {
            return;
        }

        String[] lines = {
              getTextAt(RESOURCE_BUNDLE, "stratConTab.environment.title"),
              getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.environment.latitude",
                    prettifyProfile(currentTrack.getLatitudeBand())),
              getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.environment.temperature",
                    currentTrack.getTemperature()),
              getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.environment.hydrology",
                    prettifyProfile(currentTrack.getHydrologyProfile())),
              getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.environment.terrain",
                    prettifyProfile(currentTrack.getOrogenyProfile())),
              getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.environment.settlement",
                    currentTrack.getCities().size(), prettifyProfile(currentTrack.getUrbanProfile()))
        };

        Font bodyFont = getFont().deriveFont(Font.PLAIN, UIUtil.scaleForGUI(12));
        Font titleFont = bodyFont.deriveFont(Font.BOLD);
        FontMetrics bodyMetrics = g2D.getFontMetrics(bodyFont);
        FontMetrics titleMetrics = g2D.getFontMetrics(titleFont);
        int lineHeight = bodyMetrics.getHeight();

        int textWidth = titleMetrics.stringWidth(lines[0]);
        for (int i = 1; i < lines.length; i++) {
            textWidth = Math.max(textWidth, bodyMetrics.stringWidth(lines[i]));
        }

        int pad = UIUtil.scaleForGUI(8);
        int margin = UIUtil.scaleForGUI(8);
        int arc = UIUtil.scaleForGUI(10);
        int boxWidth = textWidth + (pad * 2);
        int boxHeight = (lineHeight * lines.length) + (pad * 2);

        Rectangle visible = getVisibleRect();
        int boxX = (visible.x + visible.width) - boxWidth - margin;
        int boxY = visible.y + margin;

        g2D.setColor(new Color(0, 0, 0, 190));
        g2D.fillRoundRect(boxX, boxY, boxWidth, boxHeight, arc, arc);
        g2D.setColor(new Color(255, 255, 255, 60));
        g2D.drawRoundRect(boxX, boxY, boxWidth, boxHeight, arc, arc);

        int textX = boxX + pad;
        int textY = boxY + pad + bodyMetrics.getAscent();
        for (int i = 0; i < lines.length; i++) {
            if (i == 0) {
                g2D.setFont(titleFont);
                g2D.setColor(new Color(0xE8, 0xC4, 0x0A));
            } else {
                g2D.setFont(bodyFont);
                g2D.setColor(Color.WHITE);
            }
            g2D.drawString(lines[i], textX, textY);
            textY += lineHeight;
        }
    }

    /**
     * Turns a stored profile/band enum name ({@code COASTAL_PORTS}) into a readable label ({@code Coastal Ports}), or
     * an em dash when the value is absent.
     */
    private static String prettifyProfile(String enumName) {
        if ((enumName == null) || enumName.isBlank()) {
            return "—";
        }

        StringBuilder pretty = new StringBuilder();
        for (String word : enumName.toLowerCase().split("_")) {
            if (!word.isEmpty()) {
                pretty.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
            }
        }
        return pretty.toString().trim();
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

            // the hexes are drawn under a zoom scaling, but the polygons we test against are in unscaled model space,
            // so divide the click point back down by the current scale to line the two up
            translatedClickedPoint.setLocation(translatedClickedPoint.getX() / scale,
                  translatedClickedPoint.getY() / scale);

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
        // Read once per pass: this is a java.util.prefs lookup, and per-hex would be a thousand of them per repaint.
        boolean alternateFogOfWar = MekHQ.getMHQOptions().getUseAlternateStratConFogOfWarDisplay();

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

                    boolean unscouted = !trackRevealed && !currentTrack.coordsRevealed(x, y);

                    if (biomeImage != null) {
                        if (unscouted && alternateFogOfWar) {
                            // Alternate fog: the terrain itself at quarter strength over the dark base fill, so the
                            // ground is just legible while the hex still clearly reads as unscouted.
                            var push = g2D.getComposite();
                            g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ALTERNATE_FOG_ALPHA));
                            g2D.drawImage(biomeImage, null, graphHex.xpoints[1], graphHex.ypoints[0]);
                            g2D.setComposite(push);
                        } else {
                            // left-most and topmost point; experimentally adjusted to avoid empty space in
                            // the top left
                            g2D.drawImage(biomeImage, null, graphHex.xpoints[1], graphHex.ypoints[0]);
                        }
                    }

                    // Classic fog of war: the blue-tinted fog layer plus a contrast fill. Roads and cities are drawn
                    // in later passes (over the fog image), and the unscouted contrast tint is reapplied there so
                    // those hexes still read as unscouted.
                    if (unscouted && !alternateFogOfWar) {
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
     * Renders the road network as semi-transparent lines between hex centers, plus a stub off the map for each network
     * that branches to the sector edge. Drawn before cities, so a city's sprite sits on top and the road reads as
     * leading into it.
     *
     * <p>The whole network is collected into one path and stroked twice: a wide dark casing, then the brown fill on
     * top. Stroking a single path composites each pass as one shape, so overlapping segments at junctions blend cleanly
     * instead of stacking their semi-transparent strokes, and no segment's casing can cut across another's fill.</p>
     *
     * <p>Under the alternate fog-of-war display, the portion of the network crossing unscouted hexes is drawn at the
     * same reduced opacity as the terrain beneath it. This is done with complementary clip regions rather than by
     * splitting segments: the full-strength pass is clipped away from the unscouted hexes and the faded pass is clipped
     * to them, so a segment dims exactly at the hex border with no gap or double-draw. Under the classic display, roads
     * keep their long-standing behavior of drawing at full strength over the fog layer.</p>
     */
    private void drawRoads(Graphics2D g2D) {
        var roads = currentTrack.getRoads();
        if (roads.isEmpty()) {
            return;
        }

        Path2D.Double network = new Path2D.Double();

        // Add each undirected road segment once, between adjacent road hexes.
        for (StratConCoords road : roads) {
            Point from = hexCenter(road.getX(), road.getY());
            for (StratConCoords neighbor : StratConHexGeometry.neighbors(currentTrack, road)) {
                if (roads.contains(neighbor) && isAfter(neighbor, road)) {
                    Point to = hexCenter(neighbor.getX(), neighbor.getY());
                    network.moveTo(from.x, from.y);
                    network.lineTo(to.x, to.y);
                }
            }
        }

        // Add a stub off the map for each off-map branch.
        for (StratConCoords exit : currentTrack.getRoadExits()) {
            Point from = hexCenter(exit.getX(), exit.getY());
            Point off = offMapPoint(exit);
            network.moveTo(from.x, from.y);
            network.lineTo(off.x, off.y);
        }

        Stroke pushStroke = g2D.getStroke();
        Color pushColor = g2D.getColor();

        Area unscouted = unscoutedRoadArea(roads);
        if (unscouted == null) {
            strokeRoadNetwork(g2D, network);
        } else {
            Shape pushClip = g2D.getClip();
            Composite pushComposite = g2D.getComposite();

            // The scouted portion at full strength: the current clip (or the network's own bounds, grown so no
            // stroke edge is clipped, if there is none) minus the unscouted hexes.
            Rectangle roomForStrokes = network.getBounds();
            roomForStrokes.grow((int) ROAD_CASING_STROKE_WIDTH, (int) ROAD_CASING_STROKE_WIDTH);
            Area scoutedClip = new Area(pushClip != null ? pushClip : roomForStrokes);
            scoutedClip.subtract(unscouted);
            g2D.setClip(scoutedClip);
            strokeRoadNetwork(g2D, network);

            // The unscouted portion, faded to match the terrain it crosses.
            g2D.setClip(pushClip);
            g2D.clip(unscouted);
            g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ALTERNATE_FOG_ALPHA));
            strokeRoadNetwork(g2D, network);

            g2D.setComposite(pushComposite);
            g2D.setClip(pushClip);
        }

        g2D.setStroke(pushStroke);
        g2D.setColor(pushColor);
    }

    /** Strokes the road network once: the dark casing, then the brown fill on top of it. */
    private void strokeRoadNetwork(Graphics2D g2D, Path2D.Double network) {
        g2D.setStroke(new BasicStroke(ROAD_CASING_STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2D.setColor(ROAD_CASING_COLOR);
        g2D.draw(network);

        g2D.setStroke(new BasicStroke(ROAD_STROKE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2D.setColor(ROAD_COLOR);
        g2D.draw(network);
    }

    /**
     * @return the union of every unscouted <em>road</em> hex's polygon (in road-drawing space), for fading the roads
     *       that cross them — or {@code null} when nothing needs fading. Only road hexes are collected, not the whole
     *       grid: a segment between two adjacent hex centers, stroke width included, lies entirely within those two
     *       hexes, so other hexes can never clip any road ink. Off-map road stubs get a phantom hex beyond the edge
     *       that follows their border hex's scouted state, so a stub fades as a whole with the hex it exits from.
     */
    private @Nullable Area unscoutedRoadArea(Set<StratConCoords> roads) {
        Area unscouted = new Area();
        for (StratConCoords road : roads) {
            if (!currentTrack.coordsRevealed(road.getX(), road.getY())) {
                unscouted.add(hexArea(hexCenter(road.getX(), road.getY())));
            }
        }

        for (StratConCoords exit : currentTrack.getRoadExits()) {
            if (!currentTrack.coordsRevealed(exit.getX(), exit.getY())) {
                unscouted.add(hexArea(offMapPoint(exit)));
            }
        }

        return unscouted.isEmpty() ? null : unscouted;
    }

    /** @return the hex polygon centered on the given point, as an {@link Area} for clip arithmetic */
    private Area hexArea(Point center) {
        Polygon hex = generateGraphHex();
        hex.translate(center.x, center.y);
        return new Area(hex);
    }

    /**
     * Renders the city overlay: the generic urban sprite on each city hex, drawn over terrain, fog, and roads (you
     * cannot hide a city). An unscouted city hex still reads as unscouted: under the classic fog display the contrast
     * tint is reapplied on top of the sprite, and under the alternate display the sprite is drawn at the same quarter
     * strength as its terrain.
     */
    private void drawCities(Graphics2D g2D) {
        Polygon graphHex = generateGraphHex();
        boolean trackRevealed = currentTrack.hasActiveTrackReveal();
        // Read once per pass: this is a java.util.prefs lookup, and per-hex would be a thousand of them per repaint.
        boolean alternateFogOfWar = MekHQ.getMHQOptions().getUseAlternateStratConFogOfWarDisplay();

        for (int x = 0; x < currentTrack.getWidth(); x++) {
            for (int y = 0; y < currentTrack.getHeight(); y++) {
                StratConCoords currentCoords = new StratConCoords(x, y);

                if (currentTrack.isCity(currentCoords)) {
                    boolean unscouted = !trackRevealed && !currentTrack.coordsRevealed(x, y);

                    BufferedImage cityImage = getImage(StratConBiomeManifest.CITY, ImageType.TerrainTile);
                    if (cityImage != null) {
                        if (unscouted && alternateFogOfWar) {
                            // Alternate fog: the city sprite at the same quarter strength as its unscouted terrain.
                            var push = g2D.getComposite();
                            g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ALTERNATE_FOG_ALPHA));
                            g2D.drawImage(cityImage, null, graphHex.xpoints[1], graphHex.ypoints[0]);
                            g2D.setComposite(push);
                        } else {
                            g2D.drawImage(cityImage, null, graphHex.xpoints[1], graphHex.ypoints[0]);
                        }
                    }

                    if (unscouted && !alternateFogOfWar) {
                        Color pushColor = g2D.getColor();
                        var pushComposite = g2D.getComposite();
                        g2D.setColor(Color.DARK_GRAY);
                        g2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                        g2D.fillPolygon(graphHex);
                        g2D.setColor(pushColor);
                        g2D.setComposite(pushComposite);
                    }
                }

                int[] downwardVector = getDownwardYVector();
                graphHex.translate(downwardVector[0], downwardVector[1]);
            }

            int[] translationVector = getRightAndUpVector(x % 2 == 0);
            graphHex.translate(translationVector[0], translationVector[1]);
        }
    }

    /**
     * @return the pixel center of hex {@code (x, y)} in the same translated space as the facility/scenario passes
     */
    private Point hexCenter(int x, int y) {
        int centerX = x * ROAD_STEP_X;
        int centerY = (y * HEX_Y_RADIUS * 2) + ((x % 2 != 0) ? -HEX_Y_RADIUS : 0);
        return new Point(centerX, centerY);
    }

    /**
     * @return a point one hex beyond the sector edge from the given border hex, for drawing an off-map road stub
     */
    private Point offMapPoint(StratConCoords exit) {
        int x = exit.getX();
        int y = exit.getY();
        if (x == 0) {
            x = -1;
        } else if (x == (currentTrack.getWidth() - 1)) {
            x = currentTrack.getWidth();
        } else if (y == 0) {
            y = -1;
        } else if (y == (currentTrack.getHeight() - 1)) {
            y = currentTrack.getHeight();
        }
        return hexCenter(x, y);
    }

    /**
     * @return {@code true} if {@code a} sorts after {@code b}, used to draw each undirected road segment only once
     */
    private static boolean isAfter(StratConCoords a, StratConCoords b) {
        return (a.getX() > b.getX()) || ((a.getX() == b.getX()) && (a.getY() > b.getY()));
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
                            Formation formation = campaign.getPlayerForce().getFormation(forceID);
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
        g2D.scale(scale, scale);
    }

    /**
     * @return the {@link JViewport} this panel is scrolled within, or {@code null} if it is not inside one.
     */
    private JViewport getViewport() {
        Container parent = getParent();
        return (parent instanceof JViewport) ? (JViewport) parent : null;
    }

    /**
     * Clamps a proposed viewport position so the view can never scroll past the edges of the map - this is what keeps
     * the player from ever losing sight of the hex board.
     *
     * @param proposed the desired top-left view position
     * @param viewport the viewport the map is displayed in
     *
     * @return a position guaranteed to keep the map filling (or bounded by) the viewport
     */
    private Point clampViewPosition(Point proposed, JViewport viewport) {
        Dimension viewSize = getPreferredSize();
        Dimension extent = viewport.getExtentSize();

        int maxX = Math.max(0, viewSize.width - extent.width);
        int maxY = Math.max(0, viewSize.height - extent.height);

        int x = Math.clamp(proposed.x, 0, maxX);
        int y = Math.clamp(proposed.y, 0, maxY);

        return new Point(x, y);
    }

    /**
     * Pans the map by the given screen-pixel delta, clamped to the map edges. Dragging the mouse right/down moves the
     * content the same way, which corresponds to decreasing the view position.
     */
    private void panBy(int dxScreen, int dyScreen) {
        JViewport viewport = getViewport();
        if (viewport == null) {
            return;
        }

        Point viewPos = viewport.getViewPosition();
        Point newPos = new Point(viewPos.x - dxScreen, viewPos.y - dyScreen);
        viewport.setViewPosition(clampViewPosition(newPos, viewport));
    }

    /**
     * Zooms the map in or out one step in response to a mouse-wheel event, keeping the point under the cursor anchored
     * in place. Rescales the panel (so the scrollbars track the new size) and re-clamps the view to the map edges.
     */
    private void zoomAt(MouseWheelEvent e) {
        double oldScale = scale;

        // use the precise (fractional) rotation so trackpads and high-resolution wheels zoom smoothly instead of in
        // fixed notches; a raw mouse wheel still reports +/-1 per notch
        double newScale = oldScale * Math.pow(ZOOM_STEP, -e.getPreciseWheelRotation());
        newScale = Math.clamp(newScale, MIN_SCALE, MAX_SCALE);

        if (newScale == oldScale) {
            return;
        }

        JViewport viewport = getViewport();
        Point cursor = e.getPoint();

        scale = newScale;

        if (viewport != null) {
            Point viewPos = viewport.getViewPosition();
            int cursorInViewX = cursor.x - viewPos.x;
            int cursorInViewY = cursor.y - viewPos.y;

            double ratio = newScale / oldScale;
            int newContentX = (int) Math.round(cursor.x * ratio);
            int newContentY = (int) Math.round(cursor.y * ratio);

            Point target = new Point(newContentX - cursorInViewX, newContentY - cursorInViewY);

            // resize the view synchronously so the viewport clamps against the new dimensions within this same event,
            // then reposition in one shot. Deferring the reposition (e.g. via invokeLater) paints one frame at the new
            // scale but the old position first, which is what read as the view "re-centering" on every zoom.
            setSize(getPreferredSize());
            viewport.setViewPosition(clampViewPosition(target, viewport));
        }

        revalidate();
        repaint();
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

        // left button generally selects a hex...
        if (e.getButton() == MouseEvent.BUTTON1) {
            // ...unless the player was dragging to pan the map, in which case suppress the selection
            if (panning) {
                return;
            }

            clickedPoint = e.getPoint();
            boolean pointFoundOnBoard = detectClickedHex();

            if (pointFoundOnBoard) {
                infoArea.setText(buildSelectedHexInfo(campaign));
            }

            repaint();
            // right button pops up a context menu
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
    /**
     * A rough local temperature for a hex: the sector's average temperature shifted by the hex's terrain climate (see
     * {@link StratConBiomeManifest#terrainTemperatureOffset}). This is the same value a scenario spawned here uses for
     * its board temperature.
     *
     * @param coords the hex to evaluate
     *
     * @return the estimated local temperature in Celsius
     */
    private int selectedHexTemperature(StratConCoords coords) {
        return currentTrack.getTemperature() +
                     StratConBiomeManifest.terrainTemperatureOffset(currentTrack.getTerrainTile(coords));
    }

    private String buildSelectedHexInfo(Campaign campaign) {
        StringBuilder infoBuilder = new StringBuilder();
        // Bound the width so long content (e.g. long terrain/scenario names) wraps instead of overflowing the HUD.
        infoBuilder.append("<html><body style='width: ").append(UIUtil.scaleForGUI(300)).append("px'>");

        infoBuilder.append(getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.environment.temperature",
              selectedHexTemperature(boardState.getSelectedCoords())));
        infoBuilder.append("<br/>");
        infoBuilder.append(getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.environment.terrain",
              currentTrack.getTerrainTile(boardState.getSelectedCoords())));
        infoBuilder.append("<br/>");

        if (currentTrack.isCity(boardState.getSelectedCoords())) {
            infoBuilder.append(getTextAt(RESOURCE_BUNDLE, "stratConTab.hexInfo.city"));
        }

        boolean coordsRevealed = currentTrack.hasActiveTrackReveal() ||
                                       currentTrack.getRevealedCoords().contains(boardState.getSelectedCoords());
        if (coordsRevealed) {
            infoBuilder.append(getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.hexInfo.reconComplete",
                  spanOpeningWithCustomColor(getPositiveColor()), CLOSING_SPAN_TAG));
        }

        if (currentTrack.getAssignedCoordForces().containsKey(boardState.getSelectedCoords())) {
            for (int forceID : currentTrack.getAssignedCoordForces().get(boardState.getSelectedCoords())) {
                Formation formation = this.campaign.getPlayerForce().getFormation(forceID);
                infoBuilder.append(getFormattedTextAt(RESOURCE_BUNDLE,
                      "stratConTab.hexInfo.assignment",
                      formation.getName()));

                if (currentTrack.getStickyForces().contains(forceID)) {
                    infoBuilder.append(" ").append(getTextAt(RESOURCE_BUNDLE, "stratConTab.hexInfo.sticky"));
                }

                infoBuilder.append("<br/>")
                      .append(getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.hexInfo.return",
                            currentTrack.getAssignedForceReturnDates().get(forceID)));
            }
        }

        if (coordsRevealed || currentTrack.isGmRevealed()) {
            StratConFacility facility = currentTrack.getFacility(boardState.getSelectedCoords());

            if ((facility != null) && (facility.getFacilityType() != null)) {
                if (facility.isStrategicObjective()) {
                    infoBuilder.append(getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.hexInfo.strategicObjective",
                          spanOpeningWithCustomColor(getAmazingColor()), CLOSING_SPAN_TAG));
                }
                infoBuilder.append("<span color='")
                      .append(facility.getOwner() == Allied ?
                                    getPositiveColor() :
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
            infoBuilder.append(getFormattedTextAt(RESOURCE_BUNDLE, "stratConTab.hexInfo.reconIncomplete",
                  spanOpeningWithCustomColor(getWarningColor()), CLOSING_SPAN_TAG));
        }
        infoBuilder.append("<br/>");

        StratConScenario selectedScenario = getSelectedScenario();
        if (selectedScenario != null) {
            AtBDynamicScenario backingScenario = selectedScenario.getBackingScenario();

            if (coordsRevealed || !backingScenario.isCloaked() || currentTrack.isGmRevealed()) {
                infoBuilder.append(selectedScenario.getInfo(campaign));
            }
        }

        infoBuilder.append("</body></html>");

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

        /**
         * Sets the selected hex, or clears the selection when given {@code null} (mirroring
         * {@link #getSelectedCoords}).
         */
        public void setSelectedCoords(@Nullable StratConCoords coords) {
            selectedX = (coords == null) ? null : coords.getX();
            selectedY = (coords == null) ? null : coords.getY();
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
                    assignmentUI.display(campaign, campaignState, selectedCoords, false, false);
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
                        assignmentUI.display(campaign, campaignState, selectedCoords, restrictToSingleForce, true);
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
                recalculateRoads();
                break;
            case RIGHT_CLICK_COMMAND_CAPTURE_FACILITY:
                StratConRulesManager.switchFacilityOwner(currentTrack.getFacility(selectedCoords));
                // Deliberately does NOT recalculate roads. A road is built ground: taking the base at the end of it
                // neither lays new road nor tears up the old. Recalculating would also rebuild the whole network from
                // scratch, so a single capture could redraw roads across the sector. Capturing the same facility by
                // winning a scenario leaves the network alone for the same reason.
                break;
            case RIGHT_CLICK_COMMAND_ADD_FACILITY:
                JMenuItem eventSource = (JMenuItem) evt.getSource();
                StratConFacility facility = (StratConFacility) eventSource.getClientProperty(
                      RIGHT_CLICK_COMMAND_ADD_FACILITY);
                StratConFacility newFacility = facility.clone();
                newFacility.setVisible(currentTrack.getRevealedCoords().contains(selectedCoords));
                currentTrack.addFacility(selectedCoords, newFacility);
                recalculateRoads();
                break;
            case RIGHT_CLICK_COMMAND_ADD_CITY:
                currentTrack.addCity(selectedCoords);
                recalculateRoads();
                break;
            case RIGHT_CLICK_COMMAND_REMOVE_CITY:
                currentTrack.getCities().remove(selectedCoords);
                recalculateRoads();
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
            int xDimension = (int) Math.floor(HEX_X_RADIUS * 1.75 * currentTrack.getWidth() * scale);
            int yDimension = (int) Math.floor(HEX_Y_RADIUS * 2.1 * currentTrack.getHeight() * scale);

            return new Dimension(xDimension, yDimension);
        } else {
            return super.getPreferredSize();
        }
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public boolean isCommitForces() {
        return commitForces;
    }

    public void setCommitForces(boolean commitForces) {
        this.commitForces = commitForces;
    }
}
