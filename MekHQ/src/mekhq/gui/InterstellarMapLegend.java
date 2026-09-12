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

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import megamek.client.ui.util.UIUtil;
import megamek.codeUtilities.ObjectUtility;
import mekhq.gui.baseComponents.ImmersiveScrollBarStyle;
import mekhq.utilities.MHQInternationalization;

final class InterstellarMapLegend {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CampaignGUI";
    private static final int CONTENT_WIDTH = 560;
    private static final int SWATCH_WIDTH = 64;
    private static final int SWATCH_HEIGHT = 38;
    private static final int MAX_VIEWPORT_HEIGHT = 620;
    private static final int SCROLLBAR_WIDTH = 10;
    private static final Color BACKGROUND = new Color(5, 13, 23);
    private static final Color MAP_BACKGROUND = new Color(5, 12, 21);
    private static final Color MUTED_TEXT = new Color(158, 179, 187);
    private static final Color DIVIDER = new Color(65, 210, 224, 45);
    private static final Color BORDER = new Color(65, 210, 224, 105);
    private static final Color TEXT = new Color(198, 214, 220);
    private static final Color BUTTON_ICON = new Color(125, 230, 238);
    private static final Color SCROLLBAR_THUMB = new Color(54, 101, 113);

    enum Symbol {
        FACTION_OWNERSHIP,
        LAYER_TECHNOLOGY,
        LAYER_INDUSTRY,
        LAYER_RAW_MATERIALS,
        LAYER_OUTPUT,
        LAYER_AGRICULTURE,
        LAYER_POPULATION,
        LAYER_HPG,
        HPG_STATIONS,
        LAYER_RECHARGE_STATIONS,
        LAYER_ACADEMIES,
        LAYER_HIRING_HALLS,
        LAYER_DISEASE_OUTBREAKS,
        FACTION_EMBLEM,
        SELECTED_SYSTEM,
        HOVERED_SYSTEM,
        CURRENT_FLEET,
        PLAYER_BASE,
        PLANNED_ROUTE,
        ACTIVE_ROUTE,
        WAYPOINT_BADGE,
        REACHABILITY,
        REACHABILITY_CAUTION,
        REACHABILITY_BLOCKED,
        ROUTE_CAUTION,
        ROUTE_BLOCKED,
        MEASUREMENT,
        CONTRACT_SEARCH_RADIUS,
        PLANETARY_ACQUISITION_RADIUS,
        JUMP_RADIUS,
        HPG_RANGE,
        CAPITAL_HIERARCHY,
        OPERATION,
        RESTRICTED_SYSTEM,
        GM_EDITED_SYSTEM,
        HPG_NETWORK,
        ADMINISTRATIVE_BOUNDARIES,
        SOVEREIGN_TERRITORY,
        DISPUTED_TERRITORY,
        UNCLAIMED_POCKET,
        ENCLAVE
    }

    @FunctionalInterface
    interface SymbolPainter {
        void paint(Graphics2D graphics, Symbol symbol);
    }

    private record Entry(Symbol symbol, String title, String meaning) {
    }

    private record Section(String heading, List<Entry> entries) {
    }

    private static final List<Section> SECTIONS = List.of(
          section("map.legend.section.navigation.text", List.of(
                entry(Symbol.SELECTED_SYSTEM, "map.legend.selectedSystem"),
                entry(Symbol.HOVERED_SYSTEM, "map.legend.hoveredSystem"),
                entry(Symbol.CURRENT_FLEET, "map.legend.currentFleet"),
                entry(Symbol.PLAYER_BASE, "map.legend.playerBase"),
                entry(Symbol.MEASUREMENT, "map.legend.measurement"))),
          section("map.legend.section.routes.text", List.of(
                entry(Symbol.PLANNED_ROUTE, "map.legend.plannedRoute"),
                entry(Symbol.ACTIVE_ROUTE, "map.legend.activeRoute"),
                entry(Symbol.WAYPOINT_BADGE, "map.legend.waypointBadge"),
                entry(Symbol.REACHABILITY, "map.legend.reachability"),
                entry(Symbol.REACHABILITY_CAUTION, "map.legend.reachabilityCaution"),
                entry(Symbol.REACHABILITY_BLOCKED, "map.legend.reachabilityBlocked"),
                entry(Symbol.ROUTE_CAUTION, "map.legend.routeCaution"),
                entry(Symbol.ROUTE_BLOCKED, "map.legend.routeBlocked"))),
          section("map.legend.section.layers.text", List.of(
                entry(Symbol.FACTION_OWNERSHIP, "map.legend.factionOwnership"),
                entry(Symbol.LAYER_TECHNOLOGY, "map.legend.technology"),
                entry(Symbol.LAYER_INDUSTRY, "map.legend.industry"),
                entry(Symbol.LAYER_RAW_MATERIALS, "map.legend.rawMaterials"),
                entry(Symbol.LAYER_OUTPUT, "map.legend.output"),
                entry(Symbol.LAYER_AGRICULTURE, "map.legend.agriculture"),
                entry(Symbol.LAYER_POPULATION, "map.legend.population"),
                entry(Symbol.LAYER_HPG, "map.legend.hpg"),
                entry(Symbol.LAYER_RECHARGE_STATIONS, "map.legend.rechargeStations"),
                entry(Symbol.LAYER_ACADEMIES, "map.legend.academies"),
                entry(Symbol.LAYER_HIRING_HALLS, "map.legend.hiringHalls"),
                entry(Symbol.LAYER_DISEASE_OUTBREAKS, "map.legend.diseaseOutbreaks"))),
          section("map.legend.section.overlays.text", List.of(
                entry(Symbol.HPG_STATIONS, "map.legend.hpgStations"),
                entry(Symbol.FACTION_EMBLEM, "map.legend.factionEmblem"),
                entry(Symbol.HPG_NETWORK, "map.legend.hpgNetwork"),
                entry(Symbol.ADMINISTRATIVE_BOUNDARIES, "map.legend.administrativeBoundaries"),
                entry(Symbol.SOVEREIGN_TERRITORY, "map.legend.sovereignTerritory"),
                entry(Symbol.DISPUTED_TERRITORY, "map.legend.disputedTerritory"),
                entry(Symbol.UNCLAIMED_POCKET, "map.legend.unclaimedPocket"),
                entry(Symbol.ENCLAVE, "map.legend.enclave"))),
          section("map.legend.section.rangeRings.text", List.of(
                entry(Symbol.CONTRACT_SEARCH_RADIUS, "map.legend.contractSearchRadius"),
                entry(Symbol.PLANETARY_ACQUISITION_RADIUS, "map.legend.planetaryAcquisitionRadius"),
                entry(Symbol.JUMP_RADIUS, "map.legend.jumpRadius"),
                entry(Symbol.HPG_RANGE, "map.legend.hpgRange"))),
          section("map.legend.section.systemStatus.text", List.of(
                entry(Symbol.CAPITAL_HIERARCHY, "map.legend.capitalHierarchy"),
                entry(Symbol.OPERATION, "map.legend.operation"),
                entry(Symbol.RESTRICTED_SYSTEM, "map.legend.restrictedSystem"),
                entry(Symbol.GM_EDITED_SYSTEM, "map.legend.gmEditedSystem"))));

    private InterstellarMapLegend() {
    }

    static JTabbedPane createTabbedPane(SymbolPainter symbolPainter) {
        int contentWidth = UIUtil.scaleForGUI(CONTENT_WIDTH);
        List<JPanel> sectionPanels = new ArrayList<>(SECTIONS.size());
        int maximumSectionHeight = 1;
        for (Section section : SECTIONS) {
            JPanel sectionPanel = createSection(section, contentWidth, symbolPainter);
            sectionPanels.add(sectionPanel);
            maximumSectionHeight = Math.max(maximumSectionHeight, sectionPanel.getPreferredSize().height);
        }
        int viewportHeight = Math.min(maximumSectionHeight, UIUtil.scaleForGUI(MAX_VIEWPORT_HEIGHT));

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setOpaque(true);
        tabbedPane.setBackground(BACKGROUND);
        tabbedPane.setForeground(TEXT);
        tabbedPane.setFocusable(true);
        tabbedPane.getAccessibleContext().setAccessibleName(MHQInternationalization.getTextAt(RESOURCE_BUNDLE, "map.legend.accessibleName"));
        tabbedPane.getAccessibleContext().setAccessibleDescription(MHQInternationalization.getTextAt(RESOURCE_BUNDLE, "map.legend.accessibleDescription"));
        for (int sectionIndex = 0; sectionIndex < sectionPanels.size(); sectionIndex++) {
            Section section = SECTIONS.get(sectionIndex);
            JScrollPane scrollPane = createSectionScrollPane(sectionPanels.get(sectionIndex), contentWidth,
                  viewportHeight);
            tabbedPane.addTab(section.heading(), scrollPane);
            tabbedPane.setBackgroundAt(sectionIndex, BACKGROUND);
            tabbedPane.setForegroundAt(sectionIndex, TEXT);
        }
        return tabbedPane;
    }

    private static JScrollPane createSectionScrollPane(JPanel section, int viewportWidth, int viewportHeight) {
        JScrollPane scrollPane = new JScrollPane(section, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(true);
        scrollPane.setBackground(BACKGROUND);
        scrollPane.setFocusable(true);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(BACKGROUND);
        Dimension viewportSize = new Dimension(viewportWidth, viewportHeight);
        scrollPane.getViewport().setPreferredSize(viewportSize);
        scrollPane.setPreferredSize(viewportSize);
        ImmersiveScrollBarStyle.apply(scrollPane.getVerticalScrollBar(), BACKGROUND, DIVIDER,
              SCROLLBAR_THUMB, BUTTON_ICON, UIUtil.scaleForGUI(SCROLLBAR_WIDTH));
        String sectionName = section.getAccessibleContext().getAccessibleName();
        scrollPane.getAccessibleContext().setAccessibleName(sectionName);
        scrollPane.getAccessibleContext().setAccessibleDescription(
              MHQInternationalization.getFormattedTextAt(RESOURCE_BUNDLE, "map.legend.section.accessibleDescription", sectionName));
        return scrollPane;
    }

    private static JPanel createSection(Section section, int sectionWidth, SymbolPainter symbolPainter) {
        int horizontalPadding = UIUtil.scaleForGUI(8);
        int rowWidth = sectionWidth - (horizontalPadding * 2);
        JPanel sectionPanel = new JPanel();
        sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.Y_AXIS));
        sectionPanel.setOpaque(true);
        sectionPanel.setBackground(BACKGROUND);
        sectionPanel.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(8), horizontalPadding,
              UIUtil.scaleForGUI(8), horizontalPadding));
        sectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sectionPanel.getAccessibleContext().setAccessibleName(section.heading());
        sectionPanel.putClientProperty("mapLegendSection", section.heading());
        for (Entry entry : section.entries()) {
            sectionPanel.add(createRow(entry, rowWidth, symbolPainter));
        }

        Dimension preferredSize = sectionPanel.getPreferredSize();
        sectionPanel.setPreferredSize(new Dimension(sectionWidth, preferredSize.height));
        sectionPanel.setMaximumSize(new Dimension(sectionWidth, preferredSize.height));
        return sectionPanel;
    }

    private static JPanel createRow(Entry entry, int rowWidth, SymbolPainter symbolPainter) {
        int verticalPadding = UIUtil.scaleForGUI(6);
        int swatchGap = UIUtil.scaleForGUI(10);
        int swatchWidth = UIUtil.scaleForGUI(SWATCH_WIDTH);
        int textWidth = rowWidth - swatchWidth - swatchGap;
        MapLegendSwatch swatch = new MapLegendSwatch(entry, symbolPainter);

        JLabel title = new JLabel(entry.title());
        title.setForeground(TEXT);
        Font baseFont = ObjectUtility.nonNull(UIManager.getFont("Label.font"), title.getFont());
        title.setFont(baseFont.deriveFont(Font.BOLD, baseFont.getSize2D()));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea meaning = new JTextArea(entry.meaning());
        meaning.setEditable(false);
        meaning.setFocusable(false);
        meaning.setOpaque(false);
        meaning.setForeground(MUTED_TEXT);
        meaning.setFont(baseFont.deriveFont(Math.max(10.0f, baseFont.getSize2D() - 1.0f)));
        meaning.setLineWrap(true);
        meaning.setWrapStyleWord(true);
        meaning.setBorder(null);
        meaning.setAlignmentX(Component.LEFT_ALIGNMENT);
        meaning.setSize(textWidth, Short.MAX_VALUE);
        int meaningHeight = meaning.getPreferredSize().height;
        Dimension meaningSize = new Dimension(textWidth, meaningHeight);
        meaning.setPreferredSize(meaningSize);
        meaning.setMinimumSize(meaningSize);
        meaning.setMaximumSize(meaningSize);
        meaning.putClientProperty("mapLegendTitle", entry.title());

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(title);
        textPanel.add(Box.createRigidArea(new Dimension(0, UIUtil.scaleForGUI(2))));
        textPanel.add(meaning);
        int textHeight = title.getPreferredSize().height + UIUtil.scaleForGUI(2) + meaningHeight;
        Dimension textSize = new Dimension(textWidth, textHeight);
        textPanel.setPreferredSize(textSize);
        textPanel.setMinimumSize(textSize);
        textPanel.setMaximumSize(textSize);
        textPanel.putClientProperty("mapLegendTextCell", Boolean.TRUE);
        textPanel.putClientProperty("mapLegendTitle", entry.title());

        JPanel swatchCell = new JPanel(new GridBagLayout());
        swatchCell.setOpaque(false);
        swatchCell.setPreferredSize(swatch.getPreferredSize());
        swatchCell.setMinimumSize(swatch.getPreferredSize());
        swatchCell.add(swatch);
        swatchCell.putClientProperty("mapLegendSwatchCell", Boolean.TRUE);
        swatchCell.putClientProperty("mapLegendTitle", entry.title());

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints swatchConstraints = new GridBagConstraints();
        swatchConstraints.gridx = 0;
        swatchConstraints.gridy = 0;
        swatchConstraints.weighty = 1.0;
        swatchConstraints.fill = GridBagConstraints.VERTICAL;
        swatchConstraints.anchor = GridBagConstraints.CENTER;
        swatchConstraints.insets = new Insets(0, 0, 0, swatchGap);
        row.add(swatchCell, swatchConstraints);

        GridBagConstraints textConstraints = new GridBagConstraints();
        textConstraints.gridx = 1;
        textConstraints.gridy = 0;
        textConstraints.weightx = 1.0;
        textConstraints.weighty = 1.0;
        textConstraints.fill = GridBagConstraints.HORIZONTAL;
        textConstraints.anchor = GridBagConstraints.CENTER;
        row.add(textPanel, textConstraints);
        row.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, UIUtil.scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createEmptyBorder(verticalPadding, 0, verticalPadding, 0)));
        int rowHeight = Math.max(swatch.getPreferredSize().height, textHeight) + (verticalPadding * 2) + 1;
        Dimension rowSize = new Dimension(rowWidth, rowHeight);
        row.setPreferredSize(rowSize);
        row.setMinimumSize(rowSize);
        row.setMaximumSize(rowSize);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.putClientProperty("mapLegendRow", Boolean.TRUE);
        row.putClientProperty("mapLegendTitle", entry.title());
        row.getAccessibleContext().setAccessibleName(entry.title());
        row.getAccessibleContext().setAccessibleDescription(entry.meaning());
        return row;
    }

    private static Entry entry(Symbol symbol, String resourceKey) {
        return new Entry(symbol, MHQInternationalization.getTextAt(RESOURCE_BUNDLE, resourceKey + ".title"), MHQInternationalization.getTextAt(RESOURCE_BUNDLE, resourceKey + ".description"));
    }

    private static Section section(String resourceKey, List<Entry> entries) {
        return new Section(MHQInternationalization.getTextAt(RESOURCE_BUNDLE, resourceKey), entries);
    }

    private static final class MapLegendSwatch extends JComponent {
        private final Symbol symbol;
        private final SymbolPainter symbolPainter;

        private MapLegendSwatch(Entry entry, SymbolPainter symbolPainter) {
            symbol = entry.symbol();
            this.symbolPainter = symbolPainter;
            Dimension swatchSize = UIUtil.scaleForGUI(SWATCH_WIDTH, SWATCH_HEIGHT);
            setPreferredSize(swatchSize);
            setMinimumSize(swatchSize);
            setMaximumSize(swatchSize);
            setFocusable(false);
            putClientProperty("mapLegendSwatch", Boolean.TRUE);
            putClientProperty("mapLegendTitle", entry.title());
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D swatchGraphics = (Graphics2D) graphics.create();
            try {
                swatchGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                      RenderingHints.VALUE_ANTIALIAS_ON);
                swatchGraphics.scale(getWidth() / (double) SWATCH_WIDTH, getHeight() / (double) SWATCH_HEIGHT);
                swatchGraphics.setPaint(new GradientPaint(0, 0, MAP_BACKGROUND,
                      0, SWATCH_HEIGHT, MAP_BACKGROUND));
                swatchGraphics.fillRect(0, 0, SWATCH_WIDTH, SWATCH_HEIGHT);
                symbolPainter.paint(swatchGraphics, symbol);
                swatchGraphics.setPaint(BORDER);
                float borderWidth = 0.8f;
                double borderInset = borderWidth / 2.0;
                swatchGraphics.setStroke(new BasicStroke(borderWidth));
                swatchGraphics.draw(new Rectangle2D.Double(borderInset, borderInset,
                      SWATCH_WIDTH - borderWidth, SWATCH_HEIGHT - borderWidth));
            } finally {
                swatchGraphics.dispose();
            }
        }
    }
}
