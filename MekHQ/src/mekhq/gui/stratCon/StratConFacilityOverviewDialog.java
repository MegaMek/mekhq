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
package mekhq.gui.stratCon;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityIntel;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityAdvisor;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityAdvisor.UpcomingEvent;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilitySupply;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.gui.StratConTab;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;

/**
 * Lists every facility the player knows of on a contract's StratCon map, with who holds it, its state and what is
 * happening there, and, below, what is about to happen across the map, soonest first. Clicking a row shows that hex
 * on the map; double-clicking it opens the facility's details.
 *
 * <p>The dialog does not block the map, and it refreshes itself whenever it regains focus.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConFacilityOverviewDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConFacilityOperations";
    private static final String STRATCON_BUNDLE = "mekhq.resources.AtBStratCon";

    private final Campaign campaign;
    private final AbstractContract contract;
    private final StratConTab stratConTab;
    private final DefaultTableModel facilityModel = createModel("overview.column.sector",
          "overview.column.facility",
          "overview.column.heldBy",
          "overview.column.tier",
          "overview.column.condition",
          "overview.column.garrison",
          "overview.column.supply",
          "overview.column.traits",
          "overview.column.activity");
    private final DefaultTableModel eventModel = createModel("overview.column.date",
          "overview.column.sector",
          "overview.column.event");
    private final List<Location> facilityLocations = new ArrayList<>();
    private final List<Location> eventLocations = new ArrayList<>();

    private record Location(StratConTrackState track, StratConCoords coords) {
    }

    /**
     * Opens the overview beside the map.
     *
     * @param owner       the window to centre on
     * @param campaign    the current campaign
     * @param contract    the contract whose map to list
     * @param stratConTab the tab whose map to move when a row is chosen
     *
     * @author Illiani
     * @since 0.51.01
     */
    public StratConFacilityOverviewDialog(@Nullable Frame owner, Campaign campaign, AbstractContract contract,
          StratConTab stratConTab) {
        super(owner, getTextAt(RESOURCE_BUNDLE, "overview.title"), false);
        this.campaign = campaign;
        this.contract = contract;
        this.stratConTab = stratConTab;

        JTable facilityTable = createTable(facilityModel, facilityLocations, true);
        JTable eventTable = createTable(eventModel, eventLocations, false);

        JScrollPane facilityPane = new JScrollPane(facilityTable);
        facilityPane.setBorder(BorderFactory.createTitledBorder(getTextAt(RESOURCE_BUNDLE, "overview.facilities")));
        JScrollPane eventPane = new JScrollPane(eventTable);
        eventPane.setBorder(BorderFactory.createTitledBorder(getTextAt(RESOURCE_BUNDLE, "overview.upcoming")));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, facilityPane, eventPane);
        splitPane.setResizeWeight(0.6);

        RoundedJButton closeButton = new RoundedJButton(getTextAt(RESOURCE_BUNDLE, "dialog.close"));
        closeButton.addActionListener(evt -> dispose());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);

        setLayout(new BorderLayout());
        add(splitPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent event) {
                reload();
            }
        });

        reload();
        setPreferredSize(new Dimension(UIUtil.scaleForGUI(900), UIUtil.scaleForGUI(600)));
        pack();
        setLocationRelativeTo(owner);
        setVisible(true);
    }

    private static DefaultTableModel createModel(String... columnKeys) {
        Object[] columns = new Object[columnKeys.length];
        for (int index = 0; index < columnKeys.length; index++) {
            columns[index] = getTextAt(RESOURCE_BUNDLE, columnKeys[index]);
        }
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JTable createTable(DefaultTableModel model, List<Location> locations, boolean opensDetails) {
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                int row = table.rowAtPoint(event.getPoint());
                if ((row < 0) || (row >= locations.size())) {
                    return;
                }
                Location location = locations.get(row);
                if (!stratConTab.focusOnHex(contract, location.track(), location.coords())) {
                    return;
                }
                if (opensDetails && (event.getClickCount() == 2)) {
                    stratConTab.getStratconPanel().openFacilityDialog(location.coords());
                }
            }
        });
        return table;
    }

    private void reload() {
        StratConCampaignState campaignState = contract.getStratConCampaignState();
        facilityModel.setRowCount(0);
        eventModel.setRowCount(0);
        facilityLocations.clear();
        eventLocations.clear();
        if (campaignState == null) {
            return;
        }

        boolean isSupplyLinesActive = StratConFacilitySupply.isSupplyLinesActive(campaign);
        for (StratConTrackState track : campaignState.getTracks()) {
            for (Map.Entry<StratConCoords, StratConFacility> entry : track.getFacilities().entrySet()) {
                StratConFacility facility = entry.getValue();
                if (!isKnown(track, entry.getKey(), facility)) {
                    continue;
                }
                facilityModel.addRow(createFacilityRow(track, entry.getKey(), facility, isSupplyLinesActive));
                facilityLocations.add(new Location(track, entry.getKey()));
            }
        }

        for (UpcomingEvent event : StratConFacilityAdvisor.getUpcomingEvents(campaign, campaignState)) {
            eventModel.addRow(new Object[] { event.date(), event.track().getDisplayableName(),
                                             event.description() });
            eventLocations.add(new Location(event.track(), event.coords()));
        }
    }

    /**
     * @return {@code true} if the player can see the facility on the map: their side holds it, or they have located
     *       it on a hex they have scouted
     */
    private static boolean isKnown(StratConTrackState track, StratConCoords coords, StratConFacility facility) {
        if (facility.isOwnerAlliedToPlayer() || track.isGmRevealed()) {
            return true;
        }
        boolean isRevealed = track.hasActiveTrackReveal() || track.getRevealedCoords().contains(coords);
        return isRevealed && facility.isVisible();
    }

    private Object[] createFacilityRow(StratConTrackState track, StratConCoords coords, StratConFacility facility,
          boolean isSupplyLinesActive) {
        FacilityIntel intel = facility.getIntel();
        String unknown = getTextAt(RESOURCE_BUNDLE, "overview.unknown");
        String tier = intel.isAtLeast(FacilityIntel.SCOUTED) ?
                            getTextAt(STRATCON_BUNDLE, "stratConTab.facilityTier." + facility.getTier().name()) :
                            unknown;
        String condition = intel.isAtLeast(FacilityIntel.SCOUTED) ?
                                 getTextAt(STRATCON_BUNDLE,
                                       "stratConTab.facilityCondition." + facility.getCondition().name()) :
                                 unknown;
        String garrison = intel.isAtLeast(FacilityIntel.DETAILED) ?
                                facility.getGarrison() + " / " + facility.getGarrisonMaximum() :
                                unknown;
        String supply = "";
        if (isSupplyLinesActive) {
            supply = getTextAt(RESOURCE_BUNDLE,
                  StratConFacilitySupply.isCutOff(track, coords) ? "overview.supply.cut" : "overview.supply.ok");
        }

        return new Object[] { track.getDisplayableName(), facility.getDisplayableName(),
                              getTextAt(RESOURCE_BUNDLE, "dialog.owner." + StratConFacilityDialog.getOwnerKey(facility.getOwner())), tier,
                              condition, garrison, supply,
                              StratConFacilityAdvisor.areTraitsKnown(facility) ?
                                    StratConFacilityAdvisor.getTraitSummary(facility) :
                                    getTextAt(RESOURCE_BUNDLE, "overview.unknown"),
                              StratConFacilityAdvisor.getActivitySummary(track, coords) };
    }
}
