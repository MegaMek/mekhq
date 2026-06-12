/*
 * Copyright (C) 2017-2026 The MegaMek Team. All Rights Reserved.
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
 * NOTICE: The MegaMek Organization is a non-profit group of volunteers
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

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import megamek.client.ui.util.UIUtil;
import megamek.common.ui.FastJScrollPane;
import megamek.common.units.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.base.AbstractBase;
import mekhq.campaign.location.IPlace;
import mekhq.campaign.location.LocationNode;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.enums.MHQTabType;
import mekhq.gui.model.LocationDisplay;
import mekhq.gui.view.LocationPlacePanel;

/**
 * Displays all active {@link IPlace} instances in a split-pane layout. A view dropdown above the table switches between
 * General (system/planet), Units, Parts, and People column sets while always keeping Name and Type visible. Selecting a
 * row populates the detail panel on the right.
 *
 * <p>Follows MVC: {@link LocationsTabModel} owns the table data and current view mode,
 * {@link LocationsTabView} owns the widgets, and {@link LocationsTabController} drives both data refresh and user
 * interaction.</p>
 */
public class LocationsTab extends CampaignGuiTab {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.GUI";

    private enum ViewMode {
        GENERAL("LocationsTab.view.general"),
        UNITS("LocationsTab.view.units"),
        PARTS("LocationsTab.view.parts"),
        PEOPLE("LocationsTab.view.people");

        private final String resourceKey;

        ViewMode(String resourceKey) {
            this.resourceKey = resourceKey;
        }

        @Override
        public String toString() {
            return getTextAt(RESOURCE_BUNDLE, resourceKey);
        }
    }

    private LocationsTabController controller;

    public LocationsTab(CampaignGUI gui, String tabName) {
        super(gui, tabName);
    }

    @Override
    public void initTab() {
        LocationsTabModel model = new LocationsTabModel();
        LocationsTabView view = new LocationsTabView(model);
        controller = new LocationsTabController(model, view);
        setLayout(new BorderLayout());
        add(view, BorderLayout.CENTER);
    }

    @Override
    public MHQTabType tabType() {
        return MHQTabType.NAVIGATION;
    }

    @Override
    public void refreshAll() {
        controller.refresh(getCampaign());
    }

    // =========================================================================
    // Model
    // =========================================================================

    private static final class LocationsTabModel extends AbstractTableModel {

        private static final int COL_NAME = 0;
        private static final int COL_TYPE = 1;
        private static final int COL_MODE_A = 2;  // System (General) or At Location (others)
        private static final int COL_MODE_B = 3;  // Planet (General) or In Transit (others)
        private static final int COL_EXTRA_START = 4;

        // Unit type breakdown columns (displayed in UNITS mode, col >= COL_EXTRA_START)
        private static final int[] UNIT_TYPES = {
              UnitType.MEK,
              UnitType.TANK,
              UnitType.VTOL,
              UnitType.INFANTRY,
              UnitType.BATTLE_ARMOR,
              UnitType.PROTOMEK,
              UnitType.CONV_FIGHTER,
              UnitType.AEROSPACE_FIGHTER,
              UnitType.SMALL_CRAFT,
              UnitType.DROPSHIP,
              UnitType.JUMPSHIP,
              UnitType.WARSHIP,
              UnitType.NAVAL,
              };

        // Personnel role breakdown columns (displayed in PEOPLE mode)
        private static final int PEOPLE_COMBAT_COL = COL_EXTRA_START;
        private static final int PEOPLE_SUPPORT_COL = COL_EXTRA_START + 1;
        private static final int PEOPLE_CIVIL_COL = COL_EXTRA_START + 2;

        private List<IPlace> places = List.of();
        private Campaign campaign;
        private ViewMode viewMode = ViewMode.GENERAL;

        void setData(List<IPlace> places, Campaign campaign) {
            this.places = List.copyOf(places);
            this.campaign = campaign;
            fireTableDataChanged();
        }

        void setViewMode(ViewMode mode) {
            this.viewMode = mode;
            fireTableStructureChanged();
        }

        Campaign getCampaign() {
            return campaign;
        }

        IPlace getPlace(int row) {
            return places.get(row);
        }

        @Override
        public int getRowCount() {
            return places.size();
        }

        @Override
        public int getColumnCount() {
            return switch (viewMode) {
                case UNITS -> COL_EXTRA_START + UNIT_TYPES.length;
                case PEOPLE -> COL_EXTRA_START + 3;
                default -> COL_EXTRA_START;
            };
        }

        @Override
        public String getColumnName(int col) {
            if (col >= COL_EXTRA_START) {
                int extra = col - COL_EXTRA_START;
                return switch (viewMode) {
                    case UNITS -> UnitType.getTypeDisplayableName(UNIT_TYPES[extra]);
                    case PEOPLE -> switch (extra) {
                        case 0 -> getTextAt(RESOURCE_BUNDLE, "LocationsTab.column.combat");
                        case 1 -> getTextAt(RESOURCE_BUNDLE, "LocationsTab.column.support");
                        case 2 -> getTextAt(RESOURCE_BUNDLE, "LocationsTab.column.civilian");
                        default -> "";
                    };
                    default -> "";
                };
            }
            return switch (col) {
                case COL_NAME -> getTextAt(RESOURCE_BUNDLE, "LocationsTab.column.name");
                case COL_TYPE -> getTextAt(RESOURCE_BUNDLE, "LocationsTab.column.type");
                case COL_MODE_A -> switch (viewMode) {
                    case GENERAL -> getTextAt(RESOURCE_BUNDLE, "LocationsTab.column.system");
                    default -> getTextAt(RESOURCE_BUNDLE, "LocationsTab.column.atLocation");
                };
                case COL_MODE_B -> switch (viewMode) {
                    case GENERAL -> getTextAt(RESOURCE_BUNDLE, "LocationsTab.column.planet");
                    default -> getTextAt(RESOURCE_BUNDLE, "LocationsTab.column.inTransit");
                };
                default -> "";
            };
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (campaign == null || row >= places.size()) {
                return "";
            }
            IPlace place = places.get(row);
            LocalDate today = campaign.getLocalDate();
            if (col >= COL_EXTRA_START) {
                int extra = col - COL_EXTRA_START;
                return switch (viewMode) {
                    case UNITS -> countUnitsAtByType(place, UNIT_TYPES[extra]);
                    case PEOPLE -> switch (extra) {
                        case 0 -> countPeopleAtCombat(place);
                        case 1 -> countPeopleAtSupport(place);
                        case 2 -> countPeopleAtCivilian(place);
                        default -> "";
                    };
                    default -> "";
                };
            }
            return switch (col) {
                case COL_NAME -> LocationDisplay.getLocationName(place, campaign, today);
                case COL_TYPE -> resolveType(place);
                case COL_MODE_A -> switch (viewMode) {
                    case GENERAL -> LocationDisplay.getLocationSystem(place, today, campaign);
                    case UNITS -> countUnitsAt(place);
                    case PARTS -> countPartsAt(place);
                    case PEOPLE -> countPeopleAt(place);
                };
                case COL_MODE_B -> switch (viewMode) {
                    case GENERAL -> LocationDisplay.getLocationPlanet(place, today, campaign);
                    case UNITS -> countUnitsInTransit(place);
                    case PARTS -> countPartsInTransit(place);
                    case PEOPLE -> countPeopleInTransit(place);
                };
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        private static String resolveType(IPlace place) {
            if (place instanceof Campaign) {
                return getTextAt(RESOURCE_BUNDLE, "LocationPlacePanel.type.mainForce");
            }
            if (place instanceof AbstractBase base) {
                String displayType = base.getDisplayType();
                return (displayType != null && !displayType.isBlank())
                             ? displayType
                             : getTextAt(RESOURCE_BUNDLE, "LocationPlacePanel.type.base");
            }
            return getTextAt(RESOURCE_BUNDLE, "LocationPlacePanel.type.location");
        }

        private static int countUnitsAt(IPlace place) {
            return place.getHangar() != null ? place.getHangar().getUnits().size() : 0;
        }

        private static int countUnitsInTransit(IPlace place) {
            return countInTransit(place, node -> node.fetchUnitsAtLocation().size());
        }

        private static int countPartsAt(IPlace place) {
            if (place.getWarehouse() == null) {
                return 0;
            }
            int count = 0;
            for (Part part : place.getWarehouse().getParts()) {
                if (part.isSpare() && part.isPresent()) {
                    count += part.getTotalQuantity();
                }
            }
            return count;
        }

        private static int countPartsInTransit(IPlace place) {
            if (place.getWarehouse() == null) {
                return 0;
            }
            int count = 0;
            for (Part part : place.getWarehouse().getParts()) {
                if (part.isSpare() && !part.isPresent()) {
                    count += part.getTotalQuantity();
                }
            }
            return count;
        }

        private static int countPeopleAt(IPlace place) {
            return place.getPersonnel() != null ? place.getPersonnel().values().size() : 0;
        }

        private static int countPeopleInTransit(IPlace place) {
            return countInTransit(place, node -> node.fetchPersonnelAtLocation().size());
        }

        private static int countUnitsAtByType(IPlace place, int unitType) {
            if (place.getHangar() == null) {
                return 0;
            }
            int count = 0;
            for (Unit unit : place.getHangar().getUnits()) {
                if (unit.getEntity() != null && unit.getEntity().getUnitType() == unitType) {
                    count++;
                }
            }
            return count;
        }

        private static int countPeopleAtCombat(IPlace place) {
            if (place.getPersonnel() == null) {
                return 0;
            }
            int count = 0;
            for (Person person : place.getPersonnel().values()) {
                if (person.isCombat()) {
                    count++;
                }
            }
            return count;
        }

        private static int countPeopleAtSupport(IPlace place) {
            if (place.getPersonnel() == null) {
                return 0;
            }
            int count = 0;
            for (Person person : place.getPersonnel().values()) {
                if (person.isSupport() && !person.isCivilian()) {
                    count++;
                }
            }
            return count;
        }

        private static int countPeopleAtCivilian(IPlace place) {
            if (place.getPersonnel() == null) {
                return 0;
            }
            int count = 0;
            for (Person person : place.getPersonnel().values()) {
                if (person.isCivilian()) {
                    count++;
                }
            }
            return count;
        }

        @FunctionalInterface
        private interface TransitCounter {
            int count(CurrentLocation node);
        }

        private static int countInTransit(IPlace place, TransitCounter counter) {
            if (!place.hasLocationNode()) {
                return 0;
            }
            int total = 0;
            for (LocationNode child : place.getLocationNode().getChildren()) {
                if (child.getLocatable() instanceof CurrentLocation travel && !travel.isOnPlanet()) {
                    total += counter.count(travel);
                }
            }
            return total;
        }
    }

    // =========================================================================
    // View
    // =========================================================================

    private static final class LocationsTabView extends JPanel {

        private static final int PLACE_DETAIL_WIDTH = UIUtil.scaleForGUI(400);

        private final JTable table;
        private final JComboBox<ViewMode> viewDropdown;
        private final FastJScrollPane detailScrollPane;

        LocationsTabView(LocationsTabModel model) {
            setLayout(new BorderLayout());

            viewDropdown = new JComboBox<>(ViewMode.values());

            JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
            toolbar.add(new JLabel(getTextAt(RESOURCE_BUNDLE, "LocationsTab.toolbar.view")));
            toolbar.add(viewDropdown);

            table = new JTable(model);
            JScrollPane tableScrollPane = new FastJScrollPane(table);

            detailScrollPane = new FastJScrollPane();
            detailScrollPane.setBorder(RoundedLineBorder.createRoundedLineBorder());
            detailScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            detailScrollPane.setMinimumSize(new Dimension((int) (PLACE_DETAIL_WIDTH * 0.9), 600));
            detailScrollPane.setPreferredSize(new Dimension(PLACE_DETAIL_WIDTH, 600));
            detailScrollPane.setViewportView(null);

            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                  tableScrollPane, detailScrollPane);
            splitPane.setOneTouchExpandable(true);
            splitPane.setResizeWeight(1.0);

            add(toolbar, BorderLayout.NORTH);
            add(splitPane, BorderLayout.CENTER);
        }

        JTable getTable() {
            return table;
        }

        JComboBox<ViewMode> getViewDropdown() {
            return viewDropdown;
        }

        FastJScrollPane getDetailScrollPane() {
            return detailScrollPane;
        }
    }

    // =========================================================================
    // Controller
    // =========================================================================

    private static final class LocationsTabController {

        private final LocationsTabModel model;
        private final LocationsTabView view;

        LocationsTabController(LocationsTabModel model, LocationsTabView view) {
            this.model = model;
            this.view = view;

            view.getTable().getSelectionModel().addListSelectionListener(ev -> onSelectionChanged());
            view.getViewDropdown().addActionListener(
                  e -> model.setViewMode((ViewMode) view.getViewDropdown().getSelectedItem()));
        }

        void refresh(Campaign campaign) {
            List<IPlace> places = new ArrayList<>();
            places.add(campaign);
            places.addAll(campaign.getPlayerBases());
            model.setData(places, campaign);
        }

        private void onSelectionChanged() {
            Campaign campaign = model.getCampaign();
            if (campaign == null) {
                view.getDetailScrollPane().setViewportView(null);
                return;
            }
            int row = view.getTable().getSelectedRow();
            if (row < 0) {
                view.getDetailScrollPane().setViewportView(null);
                return;
            }
            IPlace place = model.getPlace(view.getTable().convertRowIndexToModel(row));
            view.getDetailScrollPane().setViewportView(new LocationPlacePanel(place, campaign));
            SwingUtilities.invokeLater(
                  () -> view.getDetailScrollPane().getVerticalScrollBar().setValue(0));
        }
    }
}
