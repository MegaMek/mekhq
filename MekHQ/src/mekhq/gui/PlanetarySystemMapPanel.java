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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.text.JTextComponent;

import jakarta.annotation.Nullable;
import megamek.client.ui.util.UIUtil;
import megamek.common.units.Dropship;
import megamek.common.units.Jumpship;
import megamek.common.util.ImageUtil;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.Utilities;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.StarUtil;
import mekhq.utilities.MHQInternationalization;

/**
 * This panel displays a particular star system with suns and planets and information about the player's unit's position
 * if in the system
 *
 * @author Taharqa (Aaron Gullickson)
 */
public class PlanetarySystemMapPanel extends JPanel {
    private static final MMLogger LOGGER = MMLogger.create(PlanetarySystemMapPanel.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CampaignGUI";
    private static final int BACK_BUTTON_SIZE = UIUtil.scaleForGUI(36);
    private static final int BACK_BUTTON_INSET = UIUtil.scaleForGUI(4);
    private final JLayeredPane pane;
    private final JPanel mapPanel;
    private final JButton btnBack;

    private final Campaign campaign;
    private final CampaignGUI hqView;
    private PlanetarySystem system;
    private int selectedPlanet;
    private JumpPath plannedRoute = new JumpPath();

    private static final Color PLANNED_ROUTE_COLOR = new Color(65, 210, 224);

    // various images to paint
    private BufferedImage imgZenithPoint;
    private BufferedImage imgNadirPoint;
    private BufferedImage imgRechargeStation;
    private BufferedImage imgDefaultDropshipFleet;
    private BufferedImage imgDefaultJumpshipFleet;
    private BufferedImage imgDropshipFleet;
    private BufferedImage imgJumpshipFleet;
    private BufferedImage imgSpace;

    private static final int minDiameter = UIUtil.scaleForGUI(16);
    private static final int maxDiameter = UIUtil.scaleForGUI(64);
    private static final int maxStarWidth = UIUtil.scaleForGUI(178);
    private static final int starImgSize = UIUtil.scaleForGUI(356);

    public PlanetarySystemMapPanel(Campaign campaign, CampaignGUI view) {
        this.hqView = view;
        this.campaign = campaign;
        this.system = this.campaign.getCurrentSystem();
        selectedPlanet = system.getPrimaryPlanetPosition();

        try {
            imgSpace = ImageIO.read(new File("data/images/universe/space.jpg")); // TODO : Remove inline file path
        } catch (IOException e1) {
            imgSpace = null;
            LOGGER.error("missing default space image");
        }
        try {
            imgZenithPoint = ImageIO.read(new File("data/images/universe/default_zenithpoint.png")); // TODO : Remove
            // inline file path
        } catch (IOException e) {
            imgZenithPoint = null;
            LOGGER.error("missing default zenith point image");
        }

        try {
            imgNadirPoint = ImageIO.read(new File("data/images/universe/default_nadirpoint.png")); // TODO : Remove
            // inline file path
        } catch (IOException e) {
            imgNadirPoint = null;
            LOGGER.error("missing default nadir point image");
        }

        try {
            imgRechargeStation = ImageIO.read(new File("data/images/universe/default_recharge_station.png")); // TODO :
            // Remove
            // inline
            // file
            // path
        } catch (IOException e) {
            imgRechargeStation = null;
            LOGGER.error("missing default recharge station image");
        }

        try {
            imgDefaultDropshipFleet = ImageIO.read(new File("data/images/universe/default_dropship_fleet.png")); // TODO
            // :
            // Remove
            // inline
            // file
            // path
        } catch (IOException e) {
            imgDefaultDropshipFleet = null;
            LOGGER.error("missing default DropShip fleet image");
        }

        try {
            imgDefaultJumpshipFleet = ImageIO.read(new File("data/images/universe/default_jumpship_fleet.png")); // TODO
            // :
            // Remove
            // inline
            // file
            // path
        } catch (IOException e) {
            imgDefaultJumpshipFleet = null;
            LOGGER.error("missing default JumpShip fleet image");
        }

        pane = new JLayeredPane();

        mapPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // tile the space image
                if (null != imgSpace) {
                    int tileWidth = imgSpace.getWidth();
                    int tileHeight = imgSpace.getHeight();
                    for (int y = 0; y < getHeight(); y += tileHeight) {
                        for (int x = 0; x < getWidth(); x += tileWidth) {
                            g2.drawImage(imgSpace, x, y, this);
                        }
                    }
                }

                // set up some numbers
                int n = system.getPlanets().size();
                // star size is minimum of half the image or 20% of total width
                int starWidth = Math.min(maxStarWidth, (int) Math.round(0.2 * getWidth()));
                int rectWidth = (getWidth() - starWidth) / n;
                int y = getHeight() / 2;
                int x;

                int jumpPointImgWidth = UIUtil.scaleForGUI(84);
                int jumpPointImgHeight = UIUtil.scaleForGUI(72);
                int rechargeImgSize = UIUtil.scaleForGUI(64);
                int shipImgSize = UIUtil.scaleForGUI(24);
                int nadirX = UIUtil.scaleForGUI(12);
                int nadirY = getHeight() - UIUtil.scaleForGUI(60) - jumpPointImgHeight;
                int zenithX = UIUtil.scaleForGUI(12);
                int zenithY = UIUtil.scaleForGUI(60);

                // where is the JumpShip
                int jumpshipX = zenithX + jumpPointImgWidth + UIUtil.scaleForGUI(8);
                int jumpshipY = zenithY + (jumpPointImgHeight / 2) - (shipImgSize / 2);
                if (!PlanetarySystemMapPanel.this.campaign.getPlayerForce().getForceDetachment().getCurrentLocation().isJumpZenith()) {
                    jumpshipX = nadirX + jumpPointImgWidth + UIUtil.scaleForGUI(8);
                    jumpshipY = nadirY + (jumpPointImgHeight / 2) - (shipImgSize / 2);
                }

                // choose the font based on sizes
                    chooseFont(g2, system, PlanetarySystemMapPanel.this.campaign,
                        rectWidth - UIUtil.scaleForGUI(6));

                // place the sun first
                Image starIcon = ImageUtil.loadImageFromFile("data/" + StarUtil.getIconImage(system)); // TODO : Remove
                // inline file
                // path
                g2.drawImage(starIcon, starWidth - starImgSize, y - (starImgSize / 2), starImgSize, starImgSize, null);

                // draw nadir and zenith points
                if (null != imgZenithPoint) {
                    g2.drawImage(imgZenithPoint, zenithX, zenithY, jumpPointImgWidth, jumpPointImgHeight, null);
                }
                if (system.isZenithCharge(PlanetarySystemMapPanel.this.campaign.getLocalDate()) &&
                          (null != imgRechargeStation)) {
                      drawRotatedImage(g2, imgRechargeStation, 90.0, zenithX,
                          zenithY + UIUtil.scaleForGUI(12), rechargeImgSize,
                          rechargeImgSize);
                }
                if (null != imgNadirPoint) {
                    g2.drawImage(imgNadirPoint, nadirX, nadirY, jumpPointImgWidth, jumpPointImgHeight, null);
                }
                if (system.isNadirCharge(PlanetarySystemMapPanel.this.campaign.getLocalDate()) &&
                          (null != imgRechargeStation)) {
                      drawRotatedImage(g2, imgRechargeStation, 90.0, nadirX,
                          nadirY + UIUtil.scaleForGUI(12), rechargeImgSize,
                          rechargeImgSize);
                }

                // get the biggest diameter allowed within this space for a planet
                int biggestDiameterPixels = rectWidth - UIUtil.scaleForGUI(32);
                if (biggestDiameterPixels < minDiameter) {
                    biggestDiameterPixels = minDiameter;
                } else if (biggestDiameterPixels > maxDiameter) {
                    biggestDiameterPixels = maxDiameter;
                }

                // find the biggest diameter among all planets
                double biggestDiameter = 0;
                for (Planet p : system.getPlanets()) {
                    if (p.getDiameter() > biggestDiameter) {
                        biggestDiameter = p.getDiameter();
                    }
                }
                Map<String, Integer> playerBaseCounts = playerBaseCountsByPlanet();

                // where is the force within this system
                AbstractLocation forceLocation = PlanetarySystemMapPanel.this.campaign.getPlayerForce()
                                                       .getForceDetachment()
                                                       .getCurrentLocation();
                boolean isForceInSystem = forceLocation.getCurrentSystem().equals(system);
                JumpPath activeRoute = forceLocation.getJumpPath();
                boolean isTransferring = isForceInSystem && (activeRoute != null)
                                               && activeRoute.isInSystemTransfer() && isInSystemRoute(activeRoute);
                int forcePosition = getPosition(getForceWorld(forceLocation, activeRoute));
                if (isForceInSystem) {
                    updateShipImages();
                }

                // in-system courses are drawn first, so the worlds they connect sit on top of them
                int jumpPointLineX = jumpshipX + shipImgSize;
                int jumpPointLineY = forceLocation.isJumpZenith() ? jumpshipY + shipImgSize : jumpshipY - shipImgSize;
                if (isForceInSystem && isInSystemRoute(plannedRoute)) {
                    Planet destination = plannedRoute.getTargetPlanet();
                    int destinationPosition = getPosition(destination);
                    int destinationX = planetX(destinationPosition, starWidth, rectWidth);
                    int destinationTop = y - (scaledDiameter(destination, biggestDiameterPixels, biggestDiameter) / 2);
                    if (plannedRoute.isInSystemTransfer()) {
                        Planet departure = plannedRoute.getOriginPlanet();
                        QuadCurve2D course = createTransferCourse(
                              planetX(getPosition(departure), starWidth, rectWidth),
                              y - (scaledDiameter(departure, biggestDiameterPixels, biggestDiameter) / 2),
                              destinationX, destinationTop, y);
                        drawCourse(g2, course, PLANNED_ROUTE_COLOR);
                    } else {
                        // not at a world, so the course runs from the jump point side of the system
                        drawCourse(g2, new Line2D.Double(jumpPointLineX, jumpPointLineY, destinationX,
                              destinationTop), PLANNED_ROUTE_COLOR);
                    }
                }
                QuadCurve2D activeTransferCourse = null;
                if (isTransferring) {
                    Planet departure = activeRoute.getOriginPlanet();
                    activeTransferCourse = createTransferCourse(
                          planetX(getPosition(departure), starWidth, rectWidth),
                          y - (scaledDiameter(departure, biggestDiameterPixels, biggestDiameter) / 2),
                          planetX(forcePosition, starWidth, rectWidth),
                          y - (scaledDiameter(activeRoute.getTargetPlanet(), biggestDiameterPixels,
                                biggestDiameter) / 2),
                          y);
                    drawCourse(g2, activeTransferCourse, Color.YELLOW);
                }

                for (int i = 1; i <= n; i++) {
                    x = planetX(i, starWidth, rectWidth);
                    Planet p = system.getPlanet(i);

                    if (null != p) {
                        int diameter = scaledDiameter(p, biggestDiameterPixels, biggestDiameter);
                        int radius = diameter / 2;

                        // check for current location; an in-system transfer is drawn once all worlds are in place
                        if (isForceInSystem && !isTransferring && (i == forcePosition)) {
                            JumpPath jp = mekhq.gui.PlanetarySystemMapPanel.this.campaign.getPlayerForce()
                                                .getForceDetachment()
                                                .getCurrentLocation()
                                                .getJumpPath();
                            int lineX1 = x;
                            int lineY1 = y - radius;
                            int lineX2 = jumpshipX + shipImgSize;
                            int lineY2 = jumpshipY + shipImgSize;
                            if (!mekhq.gui.PlanetarySystemMapPanel.this.campaign.getPlayerForce()
                                       .getForceDetachment()
                                       .getCurrentLocation()
                                       .isJumpZenith()) {
                                lineY2 = jumpshipY - shipImgSize;
                            }
                            if (null != jp) {
                                if (!mekhq.gui.PlanetarySystemMapPanel.this.campaign.getPlayerForce()
                                           .getForceDetachment()
                                           .getCurrentLocation()
                                           .isAtJumpPoint() ||
                                          jp.getLastSystem().equals(system)) {
                                    // the unit has a flight plan in this system so draw the line
                                    // in transit so draw a path
                                    g2.setColor(java.awt.Color.YELLOW);
                                    java.awt.Stroke dashed = new java.awt.BasicStroke(UIUtil.scaleForGUI(3),
                                          java.awt.BasicStroke.CAP_BUTT,
                                          java.awt.BasicStroke.JOIN_BEVEL,
                                          0,
                                          new float[] { UIUtil.scaleForGUI(9) },
                                          0);
                                    g2.setStroke(dashed);
                                    g2.drawLine(lineX1, lineY1, lineX2, lineY2);
                                }
                            }
                            if (mekhq.gui.PlanetarySystemMapPanel.this.campaign.getPlayerForce()
                                      .getForceDetachment()
                                      .getCurrentLocation()
                                      .isAtJumpPoint()) {
                                // draw a ring around jumpship
                                drawRing(g2, jumpshipX + (shipImgSize / 2), jumpshipY + (shipImgSize / 2),
                                      shipImgSize / 2, Color.ORANGE);
                                if (null != imgJumpshipFleet) {
                                    drawRotatedImage(g2, imgJumpshipFleet, 90, jumpshipX, jumpshipY, shipImgSize,
                                          shipImgSize);
                                }
                            } else {
                                if (mekhq.gui.PlanetarySystemMapPanel.this.campaign.getPlayerForce()
                                          .getForceDetachment()
                                          .getCurrentLocation()
                                          .isOnPlanet()) {
                                    drawRing(g2, x, y, radius, java.awt.Color.ORANGE);
                                    if (null != imgDropshipFleet) {
                                        g2.drawImage(imgDropshipFleet,
                                              x - radius - shipImgSize,
                                              y - radius - shipImgSize,
                                              shipImgSize,
                                              shipImgSize,
                                              null);
                                    }
                                    // draw jumpship too
                                    if (null != imgJumpshipFleet) {
                                        drawRotatedImage(g2, imgJumpshipFleet, 90, jumpshipX, jumpshipY, shipImgSize,
                                              shipImgSize);
                                    }
                                } else {
                                    if (null != imgDropshipFleet) {
                                        int lengthX = lineX1 - lineX2;
                                        int lengthY = lineY1 - lineY2;
                                        double rotationRequired = getFlightRotation(lengthX, lengthY,
                                              null != jp && jp.getLastSystem().equals(system),
                                              mekhq.gui.PlanetarySystemMapPanel.this.campaign.getPlayerForce()
                                                    .getForceDetachment()
                                                    .getCurrentLocation()
                                                    .isJumpZenith());
                                        int partialX = lineX2
                                                             +
                                                             (int) Math.round(lengthX *
                                                                                    mekhq.gui.PlanetarySystemMapPanel.this.campaign.getPlayerForce()
                                                                                          .getForceDetachment()
                                                                                          .getCurrentLocation()
                                                                                          .getPercentageTransit());
                                        int partialY = lineY2
                                                             +
                                                             (int) Math.round(lengthY *
                                                                                    mekhq.gui.PlanetarySystemMapPanel.this.campaign.getPlayerForce()
                                                                                          .getForceDetachment()
                                                                                          .getCurrentLocation()
                                                                                          .getPercentageTransit());
                                        drawRing(g2, partialX, partialY, shipImgSize / 2, java.awt.Color.ORANGE);
                                        drawRotatedImage(g2, imgDropshipFleet, rotationRequired,
                                              partialX - (shipImgSize / 2), partialY - (shipImgSize / 2), shipImgSize,
                                              shipImgSize);
                                    }
                                    // draw jumpship too
                                    if (null != imgJumpshipFleet) {
                                        drawRotatedImage(g2, imgJumpshipFleet, 90, jumpshipX, jumpshipY, shipImgSize,
                                              shipImgSize);
                                    }
                                }
                            }
                        }

                        // add ring for selected planet
                        if (selectedPlanet == i) {
                            drawRing(g2, x, y, radius, Color.WHITE);
                        }

                        // draw the planet icon
                        Image planetIcon = ImageUtil.loadImageFromFile("data/" + StarUtil.getIconImage(p)); // TODO :
                        // Remove
                        // inline
                        // file path
                        g2.drawImage(planetIcon, x - radius, y - radius, diameter, diameter, null);
                        int playerBaseCount = playerBaseCounts.getOrDefault(p.getId(), 0);
                        if (playerBaseCount > 0) {
                            double markerRadius = UIUtil.scaleForGUI(7);
                            double selectionRingClearance = (selectedPlanet == i) ? UIUtil.scaleForGUI(6) : 0.0;
                            InterstellarMapPanel.drawPlayerBaseGlyph(g2,
                                new java.awt.geom.Point2D.Double(x - radius - markerRadius - selectionRingClearance,
                                    y + radius + markerRadius + selectionRingClearance),
                                markerRadius, playerBaseCount, 1.0);
                        }
                        final String planetName = p.getPrintableName(PlanetarySystemMapPanel.this.campaign.getLocalDate());

                        // planet name
                        g2.setColor(Color.WHITE);
                        drawCenteredString(g2, planetName, x,
                            y + (biggestDiameterPixels / 2) + UIUtil.scaleForGUI(12)
                                + g.getFontMetrics().getHeight(),
                            rectWidth - UIUtil.scaleForGUI(6));
                    }
                }

                if (activeTransferCourse != null) {
                    double progress = Math.clamp(forceLocation.getPercentageTransit(), 0.0, 1.0);
                    Point2D shipPosition = getPointOnCourse(activeTransferCourse, progress);
                    int shipX = (int) Math.round(shipPosition.getX());
                    int shipY = (int) Math.round(shipPosition.getY());
                    drawRing(g2, shipX, shipY, shipImgSize / 2, Color.ORANGE);
                    if (null != imgDropshipFleet) {
                        drawRotatedImage(g2, imgDropshipFleet, getCourseHeading(activeTransferCourse, progress),
                              shipX - (shipImgSize / 2), shipY - (shipImgSize / 2), shipImgSize, shipImgSize);
                    }
                    // the JumpShips wait at the jump point
                    if (null != imgJumpshipFleet) {
                        drawRotatedImage(g2, imgJumpshipFleet, 90, jumpshipX, jumpshipY, shipImgSize,
                              shipImgSize);
                    }
                }
            }
        };

          btnBack = InterstellarMapPanel.createNavigationUtilityIconButton(0xE5C4, BACK_BUTTON_SIZE,
              MHQInternationalization.getTextAt(RESOURCE_BUNDLE, "map.system.back.toolTipText"),
              MHQInternationalization.getTextAt(RESOURCE_BUNDLE, "map.system.back.accessibleName"),
              MHQInternationalization.getTextAt(RESOURCE_BUNDLE, "map.system.back.accessibleDescription"));
        btnBack.addActionListener(ev -> back());

        // set up key bindings
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "back");
        getActionMap().put("back", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isTextFieldFocused()) {
                    return;
                }
                back();
            }
        });

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "left");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0), "left");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "left");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0), "left");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "left");
        getActionMap().put("left", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isTextFieldFocused()) {
                    return;
                }
                int target_pos = selectedPlanet - 1;
                if (target_pos < 1) {
                    return;
                }
                changeSelectedPlanet(target_pos);
                repaint();
            }
        });

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "right");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, 0), "right");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "right");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0), "right");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "right");
        getActionMap().put("right", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isTextFieldFocused()) {
                    return;
                }
                int target_pos = selectedPlanet + 1;
                if (target_pos > (system.getPlanets().size())) {
                    return;
                }
                changeSelectedPlanet(target_pos);
                repaint();
            }
        });

        mapPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    int target_pos = nearestNeighbour(e.getX(), e.getY());
                    if (target_pos < 1) {
                        return;
                    }
                    changeSelectedPlanet(target_pos);
                    repaint();
                }
            }
        });

        pane.add(mapPanel, Integer.valueOf(1));
        pane.add(btnBack, Integer.valueOf(10));

        add(pane);

        repaint();

    }

    private Map<String, Integer> playerBaseCountsByPlanet() {
        Map<String, Integer> counts = new HashMap<>();
        for (PlayerBase base : campaign.getCampaignLocationManager().getPlayerBases()) {
            if ((base.getPlanetId() != null) && (base.getCurrentLocation() != null)
                  && system.equals(base.getCurrentLocation().getCurrentSystem())) {
                counts.merge(base.getPlanetId(), 1, Integer::sum);
            }
        }
        return counts;
    }

    @Override
    protected void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();
        pane.setBounds(0, 0, width, height);
        mapPanel.setBounds(0, 0, width, height);
        btnBack.setBounds(BACK_BUTTON_INSET, BACK_BUTTON_INSET, BACK_BUTTON_SIZE, BACK_BUTTON_SIZE);
        super.paintComponent(g);
    }

    /**
     * Choose a font size based on how large it needs to be to fit within the space
     *
     * @param g      - a <code>Graphics</code> device
     * @param system - a <code>PlanetarySystem</code>
     * @param c      - a <code>Campaign</code>
     * @param limit  - an integer indicating how large the text can be
     */
    private void chooseFont(Graphics g, PlanetarySystem system, Campaign c, int limit) {
        int fontSize = UIUtil.scaleForGUI(16);
        int minimumFontSize = UIUtil.scaleForGUI(10);
        g.setFont(new Font(MHQConstants.FONT_HELVETICA, Font.PLAIN, fontSize));
        while (areNamesTooBig(g, system, campaign.getLocalDate(), limit) && fontSize >= minimumFontSize) {
            fontSize--;
            g.setFont(new Font(MHQConstants.FONT_HELVETICA, Font.PLAIN, fontSize));
        }
    }

    /**
     * Determine if planet names are too big for the display at current font size
     *
     * @param g      - a <code>Graphics</code> device
     * @param system - a <code>PlanetarySystem</code>
     * @param when   - <code>DateTime</code> object
     * @param limit  - an integer indicating how large the text can be
     *
     */
    private boolean areNamesTooBig(Graphics g, PlanetarySystem system, LocalDate when, int limit) {
        for (Planet p : system.getPlanets()) {
            String name = p.getName(when);
            if (g.getFontMetrics().stringWidth(name) > limit) {
                // try splitting
                if (name.contains(" ")) {
                    for (String line : name.split("\\s+")) {
                        if (g.getFontMetrics().stringWidth(line) > limit) {
                            return true;
                        }
                    }
                } else if (name.contains("-")) {
                    for (String line : name.split("-")) {
                        if (g.getFontMetrics().stringWidth(line) > limit) {
                            return true;
                        }
                    }
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Draw a string that is centered on x and y. It will wrap text by spaces of hyphens if too large.
     *
     * @param g     - a <code>Graphics2D</code> device to draw on
     * @param text  - a <code>String</code> of text to be drawn
     * @param x     - an <code>int</code> for the x-axis position
     * @param y     - an <code>int</code> for the y-axis position
     * @param limit - an <code>int</coce> for how big the text can be
     */
    private void drawCenteredString(Graphics2D g, String text, int x, int y, int limit) {
        FontMetrics metrics = g.getFontMetrics();
        y = y - (metrics.getHeight() / 2);
        if (metrics.stringWidth(text) > limit && (text.contains(" ") || text.contains("-"))) {
            if (text.contains(" ")) {
                // try spaces first
                for (String line : text.split("\\s+")) {
                    g.drawString(line, x - (metrics.stringWidth(line) / 2), y += g.getFontMetrics().getHeight());
                }
            } else {
                // otherwise break on the hyphen
                String[] lines = text.split("-");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    if (i != (lines.length - 1)) {
                        line = line + "-";
                    }
                    g.drawString(line, x - (metrics.stringWidth(line) / 2), y += g.getFontMetrics().getHeight());
                }
            }
        } else {
            g.drawString(text, x - (metrics.stringWidth(text) / 2), y + g.getFontMetrics().getHeight());
        }
    }

    /**
     * Update and repaint the panel for a new planetary system.
     *
     * @param s - a {@link PlanetarySystem} to paint
     */
    public void updatePlanetarySystem(PlanetarySystem s) {
        if (null == s) {
            return;
        }
        this.system = s;
        selectedPlanet = system.getPrimaryPlanetPosition();
        mapPanel.repaint();
    }

    /**
     * Update and repaint the panel for a new {@link Planet}
     *
     * @param p - a {@link Planet} to paint.
     */
    public void updatePlanetarySystem(Planet p) {
        if (null == p) {
            return;
        }
        this.system = p.getParentSystem();
        selectedPlanet = p.getSystemPosition();
        mapPanel.repaint();
    }

    /**
     *
     * @return the currently selected planet
     */
    public int getSelectedPlanetPosition() {
        return selectedPlanet;
    }

    public @Nullable Planet getSelectedPlanet() {
        return system.getPlanet(selectedPlanet);
    }

    /**
     * Change the selected planets and notify listeners so view is changed
     *
     * @param pos - an <code>int</code> giving the position of the newly selected planet
     */
    private void changeSelectedPlanet(int pos) {
        selectedPlanet = pos;
        notifyListeners();
    }

    /**
     * Find any planet in the display that contains the x and y parameters. Used to identify if planets were selected by
     * the cursor.
     *
     * @param x - the x-value of the selection
     * @param y - the y-value of the selection
     *
     * @return - a planet position
     */
    private int nearestNeighbour(double x, double y) {
        int n = system.getPlanets().size();
        int starWidth = Math.min(maxStarWidth, (int) Math.round(0.2 * getWidth()));
        int rectWidth = (getWidth() - starWidth) / n;
        int midpoint = rectWidth / 2;
        int xTarget;
        int yTarget = getHeight() / 2;

        for (int i = 1; i <= n; i++) {
            xTarget = starWidth + rectWidth * (i - 1) + midpoint;
            // must be within total possible radius
            int radius = maxDiameter / 2;
            if (x <= (xTarget + radius) & x >= (xTarget - radius) &
                      y <= (yTarget + radius) & y >= (yTarget - radius)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Draw and image with the given rotation
     *
     * @param g       - the <code>Graphics2D</code> device to draw on
     * @param image   - a <code>BufferedImage</code> to be drawn
     * @param degrees - A <code>double</code> giving the rotation to apply
     * @param x       - the x position for placement
     * @param y       - the y position for placement
     * @param width   - the width to draw the image to
     * @param height  - the height to draw the image to
     */
    private void drawRotatedImage(Graphics2D g, BufferedImage image, double degrees, int x, int y, int width,
          int height) {
        double rotationRequired = Math.toRadians(degrees);
        double locationX = image.getWidth() / 2.0;
        double locationY = image.getHeight() / 2.0;
        AffineTransform tx = AffineTransform.getRotateInstance(rotationRequired, locationX, locationY);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        // Drawing the rotated image at the required drawing locations
        g.drawImage(op.filter(image, null), x, y, width, height, null);
    }

    /**
     * Draw a selection ring of the given color around some point. This will draw over everything in the ring with a
     * black background, so should be placed before anything selected.
     *
     * @param g      - the <code>Graphics2D</code> device to draw on
     * @param x      - an <code>int</code> giving the x-position of the center of the ring
     * @param y      - an <code>int</code> giving the y-position of the center of the ring
     * @param radius - an <code>int</code> giving the radius of the ring
     * @param c      - the <code>Color</code> of the ring
     */
    private void drawRing(Graphics2D g, int x, int y, int radius, Color c) {
        Arc2D.Double arc = new Arc2D.Double();
        g.setPaint(c);
        arc.setArcByCenter(x, y, radius + UIUtil.scaleForGUI(6), 0, 360, Arc2D.OPEN);
        g.fill(arc);
        g.setPaint(Color.BLACK);
        arc.setArcByCenter(x, y, radius + UIUtil.scaleForGUI(4), 0, 360, Arc2D.OPEN);
        g.fill(arc);
        g.setPaint(c);
        arc.setArcByCenter(x, y, radius + UIUtil.scaleForGUI(3), 0, 360, Arc2D.OPEN);
        g.fill(arc);
        g.setPaint(Color.BLACK);
        arc.setArcByCenter(x, y, radius + UIUtil.scaleForGUI(2), 0, 360, Arc2D.OPEN);
        g.fill(arc);
    }

    /**
     * Determine the degree of rotation for a ship in flight to the planet or jump point
     *
     * @param lengthX    - An <code>int</code> giving the length of the x-axis by the triangle formed between the planet
     *                   and the jump point
     * @param lengthY    - An <code>int</code> giving the length of the y-axis by the triangle formed between the planet
     *                   and the jump point
     * @param inbound    - A <code>boolean</code> for whether the flight is inbound.
     * @param zenithJump - A <code>boolean</code> for whether the zenith point is the jump point (false if nadir).
     *
     * @return a <code>double</code> giving the proper rotation.
     */
    private double getFlightRotation(int lengthX, int lengthY, boolean inbound, boolean zenithJump) {
        double rotation = Math.toDegrees(Math.atan(lengthX / ((1.0 * lengthY))));
        // rotation depends on inbound or outbound
        if ((zenithJump && !inbound) || (!zenithJump && inbound)) {
            return 0 - rotation;
        } else {
            return 180 - rotation;
        }
    }

    /**
     * Get the best DropShip among the player's units. For choosing which one to display on screen for transit. This is
     * determined by weight.
     *
     * @return a <code>Unit</code> for the best DropShip.
     */
    private Unit getBestDropship() {
        Unit bestUnit = null;
        double bestWeight = 0.0;
        for (Unit u : campaign.getPlayerForce().getHangar().getUnits()) {
            if ((u.getEntity() instanceof Dropship) && (u.getEntity().getWeight() > bestWeight)) {
                bestUnit = u;
                bestWeight = u.getEntity().getWeight();
            }
        }
        return bestUnit;
    }

    /**
     * Get the best JumpShip among the player's units. For choosing which one to display on screen for transit, this is
     * determined by weight.
     *
     * @return a <code>Unit</code> for the best JumpShip.
     */
    private Unit getBestJumpship() {
        Unit bestUnit = null;
        double bestWeight = 0.0;
        for (Unit u : campaign.getPlayerForce().getHangar().getUnits()) {
            if (u.getEntity() instanceof Jumpship && u.getEntity().getWeight() > bestWeight) {
                bestUnit = u;
                bestWeight = u.getEntity().getWeight();
            }
        }
        return bestUnit;
    }

    /**
     * Update the DropShip and JumpShip fleet images by the player's campaign's best DropShip and JumpShip,
     * respectively. If the campaign has none, then it returns the default.
     */
    public void updateShipImages() {
        // get the best DropShip and JumpShip of the unit for display
        Unit dropship = getBestDropship();
        imgDropshipFleet = imgDefaultDropshipFleet;
        if (null != dropship) {
            imgDropshipFleet = Utilities.toBufferedImage(dropship.getImage(this));
        }

        Unit jumpship = getBestJumpship();
        imgJumpshipFleet = imgDefaultJumpshipFleet;
        if (null != jumpship) {
            imgJumpshipFleet = Utilities.toBufferedImage(jumpship.getImage(this));
        }
    }

    /**
     * Sets the course the player is planning, so an in-system course within this system can be previewed.
     *
     * @param route the planned route, or {@code null} if none is planned
     */
    public void setPlannedRoute(@Nullable JumpPath route) {
        plannedRoute = (route == null) ? new JumpPath() : route;
    }

    /**
     * @return the world the force is at, or headed for on its final in-system leg
     */
    private static @Nullable Planet getForceWorld(AbstractLocation location, @Nullable JumpPath activeRoute) {
        if ((activeRoute != null) && (activeRoute.size() == 1) && (activeRoute.getTargetPlanet() != null)) {
            return activeRoute.getTargetPlanet();
        }
        return location.getPlanet();
    }

    /**
     * @return the position of {@code planet} within its system, or {@code 0} if it has none
     */
    private static int getPosition(@Nullable Planet planet) {
        Integer position = (planet == null) ? null : planet.getSystemPosition();
        return (position == null) ? 0 : position;
    }

    /**
     * @return whether {@code route} is a course between worlds of the displayed system
     */
    private boolean isInSystemRoute(@Nullable JumpPath route) {
        if ((route == null) || (route.size() != 1) || !system.equals(route.getFirstSystem())) {
            return false;
        }
        Planet destination = route.getTargetPlanet();
        return (destination != null) && system.equals(destination.getParentSystem()) && (getPosition(destination) > 0)
                     && ((route.getOriginPlanet() == null) || (getPosition(route.getOriginPlanet()) > 0));
    }

    /**
     * @return the x-axis position of the center of the planet at {@code position}
     */
    private static int planetX(int position, int starWidth, int rectWidth) {
        return starWidth + (rectWidth * (position - 1)) + (rectWidth / 2);
    }

    /**
     * @return the drawn diameter of {@code planet}, in pixels
     */
    private static int scaledDiameter(Planet planet, int biggestDiameterPixels, double biggestDiameter) {
        // diameters need to be scaled relative to the largest planet, but linear
        // scale will make all but gas/ice giants tiny. log scale made sizes too close,
        // but cubic root scale seems to work pretty well.
        int diameter = (int) ((biggestDiameterPixels) * (Math.cbrt(planet.getDiameter()) / Math.cbrt(biggestDiameter)));
        return Math.clamp(diameter, minDiameter, maxDiameter);
    }

    /**
     * Creates the arc an in-system transfer follows, rising above the worlds between its two ends so it doesn't cross
     * them.
     *
     * @param centerY the y-axis position the worlds are drawn along
     */
    private static QuadCurve2D createTransferCourse(int departureX, int departureY, int destinationX,
          int destinationY, int centerY) {
        double lift = Math.min(Math.max(Math.abs(destinationX - departureX) / 2.0, UIUtil.scaleForGUI(48)),
              centerY - UIUtil.scaleForGUI(24));
        double controlY = Math.min(departureY, destinationY) - lift;
        return new QuadCurve2D.Double(departureX, departureY, (departureX + destinationX) / 2.0, controlY,
              destinationX, destinationY);
    }

    /**
     * Draws a course as a dashed line of the given color.
     */
    private static void drawCourse(Graphics2D g, Shape course, Color color) {
        Stroke originalStroke = g.getStroke();
        g.setColor(color);
        g.setStroke(new BasicStroke(UIUtil.scaleForGUI(3), BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
              new float[] { UIUtil.scaleForGUI(9) }, 0));
        g.draw(course);
        g.setStroke(originalStroke);
    }

    /**
     * @return the point {@code progress} of the way along {@code course}
     */
    private static Point2D getPointOnCourse(QuadCurve2D course, double progress) {
        double remaining = 1 - progress;
        double x = (remaining * remaining * course.getX1()) + (2 * remaining * progress * course.getCtrlX())
                         + (progress * progress * course.getX2());
        double y = (remaining * remaining * course.getY1()) + (2 * remaining * progress * course.getCtrlY())
                         + (progress * progress * course.getY2());
        return new Point2D.Double(x, y);
    }

    /**
     * @return the rotation, in degrees clockwise from facing up, of a ship {@code progress} of the way along
     *       {@code course}
     */
    private static double getCourseHeading(QuadCurve2D course, double progress) {
        double dx = (2 * (1 - progress) * (course.getCtrlX() - course.getX1()))
                          + (2 * progress * (course.getX2() - course.getCtrlX()));
        double dy = (2 * (1 - progress) * (course.getCtrlY() - course.getY1()))
                          + (2 * progress * (course.getY2() - course.getCtrlY()));
        return Math.toDegrees(Math.atan2(dx, -dy));
    }

    /**
     * The navigation HUD's search fields share the window with this panel, so letters typed there must not also move
     * the planet selection.
     */
    private static boolean isTextFieldFocused() {
        return KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JTextComponent;
    }

    private final transient List<ActionListener> listeners = new ArrayList<>();

    public void addActionListener(ActionListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public void removeActionListener(ActionListener l) {
        listeners.remove(l);
    }

    private void notifyListeners() {
        ActionEvent ev = new ActionEvent(this, ActionEvent.ACTION_FIRST, "refresh");
        listeners.forEach(l -> l.actionPerformed(ev));
    }

    /**
     * Switch back to the interstellar map
     */
    private void back() {
        hqView.getNavigationTab().getMapTab().switchSystemsMap();
    }
}
