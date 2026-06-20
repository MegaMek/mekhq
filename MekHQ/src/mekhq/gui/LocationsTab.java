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

import static mekhq.utilities.MHQInternationalization.getText;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import megamek.client.ui.util.UIUtil;
import megamek.codeUtilities.MathUtility;
import megamek.common.event.Subscribe;
import megamek.common.ui.FastJScrollPane;
import megamek.common.units.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.base.AbstractBase;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.events.LocationEvent;
import mekhq.campaign.location.ILocation;
import mekhq.campaign.location.IPlace;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;
import mekhq.gui.dialog.BaseSettingsDialog;
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
// FIXME: this class should not inherit from CampaignGuiTab because it is managed by NavigationTab now
public class LocationsTab extends CampaignGuiTab {

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
            return getText(resourceKey);
        }
    }

    private LocationsTabController controller;
    private final ActionScheduler locationListScheduler = new ActionScheduler(this::refreshAll);

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
        return null;
    }

    @Override
    public void refreshAll() {
        controller.refresh(getCampaign());
    }

    @Subscribe
    public void handle(LocationEvent ev) {
        locationListScheduler.schedule();
    }

    /** Model: owns the place list and current view mode; notifies the table of structural and data changes. */
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
              UnitType.SPACE_STATION,
              UnitType.ADVANCED_BUILDING
              };

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
            return viewMode == ViewMode.UNITS ? COL_EXTRA_START + UNIT_TYPES.length : COL_EXTRA_START;
        }

        @Override
        public String getColumnName(int col) {
            if (col >= COL_EXTRA_START && viewMode == ViewMode.UNITS) {
                return UnitType.getTypeDisplayableName(UNIT_TYPES[col - COL_EXTRA_START]);
            }
            return switch (col) {
                case COL_NAME -> getText("LocationsTab.column.name");
                case COL_TYPE -> getText("LocationsTab.column.type");
                case COL_MODE_A -> viewMode == ViewMode.GENERAL
                                         ? getText("LocationsTab.column.system")
                                         : getText("LocationsTab.column.atLocation");
                case COL_MODE_B -> viewMode == ViewMode.GENERAL
                                         ? getText("LocationsTab.column.planet")
                                         : getText("LocationsTab.column.inTransit");
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
            if (col >= COL_EXTRA_START && viewMode == ViewMode.UNITS) {
                return countUnitsAtByType(place, UNIT_TYPES[col - COL_EXTRA_START]);
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
                return getText("LocationPlacePanel.type.mainForce");
            }
            if (place instanceof AbstractBase base) {
                String displayType = base.getDisplayType();
                return (displayType != null && !displayType.isBlank())
                             ? displayType
                             : getText("LocationPlacePanel.type.base");
            }
            return getText("LocationPlacePanel.type.location");
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

        @FunctionalInterface
        private interface TransitCounter {
            int count(CurrentLocation node);
        }

        private static int countInTransit(IPlace place, TransitCounter counter) {
            if (!place.hasLocationNode()) {
                return 0;
            }
            int total = 0;
            for (ILocation child : place.getChildLocations()) {
                if (child instanceof CurrentLocation travel && !travel.isOnPlanet()) {
                    total += counter.count(travel);
                }
            }
            return total;
        }
    }

    /** View: owns all widgets: the view-mode dropdown toolbar, the table, and the detail scroll pane. */
    private static final class LocationsTabView extends JPanel {

        private static final int PLACE_DETAIL_WIDTH = UIUtil.scaleForGUI(400);

        private final JTable table;
        private final JComboBox<ViewMode> viewDropdown;
        private final FastJScrollPane detailScrollPane;

        LocationsTabView(LocationsTabModel model) {
            setLayout(new BorderLayout());

            viewDropdown = new JComboBox<>(ViewMode.values());

            JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
            toolbar.add(new JLabel(getText("LocationsTab.toolbar.view")));
            toolbar.add(viewDropdown);

            table = new JTable(model);
            JScrollPane tableScrollPane = new FastJScrollPane(table);

            detailScrollPane = new FastJScrollPane();
            detailScrollPane.setBorder(RoundedLineBorder.createRoundedLineBorder());
            detailScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            detailScrollPane.setMinimumSize(new Dimension(MathUtility.roundAwayFromZero(PLACE_DETAIL_WIDTH * 0.9),
                  600));
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

    /** Controller: wires selection, dropdown, and right-click events; drives model/view updates and campaign refresh. */
    private static final class LocationsTabController {

        private final LocationsTabModel model;
        private final LocationsTabView view;

        LocationsTabController(LocationsTabModel model, LocationsTabView view) {
            this.model = model;
            this.view = view;

            view.getTable().getSelectionModel().addListSelectionListener(ev -> onSelectionChanged());
            view.getViewDropdown().addActionListener(
                  e -> model.setViewMode((ViewMode) view.getViewDropdown().getSelectedItem()));
            view.getTable().addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        showContextMenu(e);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        showContextMenu(e);
                    }
                }

                private void showContextMenu(MouseEvent e) {
                    int row = view.getTable().rowAtPoint(e.getPoint());
                    if (row < 0) {
                        return;
                    }
                    view.getTable().setRowSelectionInterval(row, row);
                    IPlace place = model.getPlace(view.getTable().convertRowIndexToModel(row));
                    if (!(place instanceof PlayerBase base)) {
                        return;
                    }
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem configItem = new JMenuItem(
                          getText("LocationsTab.menu.configureBase"));
                    configItem.addActionListener(ev -> openBaseConfig(base));
                    menu.add(configItem);
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }

                private void openBaseConfig(PlayerBase base) {
                    Campaign campaign = model.getCampaign();
                    if (campaign == null) {
                        return;
                    }
                    JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(view.getTable());
                    new BaseSettingsDialog(frame, campaign, base).setVisible(true);
                }
            });
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
