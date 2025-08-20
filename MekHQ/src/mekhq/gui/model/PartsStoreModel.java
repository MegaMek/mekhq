/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.model;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Objects;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import megamek.common.annotations.Nullable;
import megamek.common.TargetRoll;

import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInventory;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.gui.CampaignGUI;

/**
 * A table model for displaying parts - similar to the one in CampaignGUI, but not exactly
 */
public class PartsStoreModel extends AbstractTableModel {    
    protected Campaign campaign;
    protected String[] columnNames;
    protected ArrayList<PartProxy> data;
    protected Person logisticsPerson;


    public final static int COL_NAME = 0;
    public final static int COL_DETAIL = 1;
    public final static int COL_TECH_BASE = 2;
    public final static int COL_COST = 3;
    public final static int COL_TON = 4;
    public final static int COL_TARGET = 5;
    public final static int COL_SUPPLY = 6;
    public final static int COL_TRANSIT = 7;
    public final static int COL_QUEUE = 8;
    public final static int N_COL = 9;

    /**
     * Provides a lazy view to a {@link TargetRoll} for use in a UI (e.g. sorting in a table).
     */
    public static class TargetProxy implements Comparable<TargetProxy> {
        private TargetRoll target;
        private String details;
        private String description;

        /**
         * Creates a new proxy object for a {@link TargetRoll}.
         *
         * @param t The {@link TargetRoll} to be proxied. May be null.
         */
        public TargetProxy(@Nullable TargetRoll t) {
            target = t;
        }

        /**
         * Gets the target roll.
         *
         * @return The target roll.
         */
        public TargetRoll getTargetRoll() {
            return target;
        }

        /**
         * Gets a description of the target roll.
         *
         * @return A description of the target roll.
         */
        @Nullable
        public String getDescription() {
            if (null == target) {
                return null;
            }
            if (null == description) {
                description = target.getDesc();
            }
            return description;
        }

        /**
         * Gets a string representation of a {@link TargetRoll}.
         *
         * @return A string representation of a {@link TargetRoll}.
         */
        @Override
        public String toString() {
            if (null == target) {
                return "-";
            }

            if (null == details) {
                details = target.getValueAsString();
                if (target.getValue() != TargetRoll.IMPOSSIBLE &&
                          target.getValue() != TargetRoll.AUTOMATIC_SUCCESS &&
                          target.getValue() != TargetRoll.AUTOMATIC_FAIL) {
                    details += "+";
                }
            }

            return details;
        }

        /**
         * Converts a {@link TargetRoll} into an integer for comparisons.
         *
         * @return An integer representation of the {@link TargetRoll}.
         */
        private int coerceTargetRoll() {
            int r = target.getValue();
            if (r == TargetRoll.IMPOSSIBLE) {
                return Integer.MAX_VALUE;
            } else if (r == TargetRoll.AUTOMATIC_FAIL) {
                return Integer.MAX_VALUE - 1;
            } else if (r == TargetRoll.AUTOMATIC_SUCCESS) {
                return Integer.MIN_VALUE;
            }
            return r;
        }

        /**
         * {@inheritDoc}
         *
         * @param o The {@link TargetProxy} to compare this instance to.
         *
         * @return {@inheritDoc}
         */
        @Override
        public int compareTo(TargetProxy o) {
            return Integer.compare(coerceTargetRoll(), o.coerceTargetRoll());
        }
    }

    /**
     * Provides a container for a value formatted for display and the value itself for sorting.
     */
    public static class FormattedValue<T extends Comparable<T>> implements Comparable<FormattedValue<T>> {
        private T value;
        private String formatted;

        /**
         * Creates a wrapper around a value and a formatted string representing the value.
         */
        public FormattedValue(T v, String f) {
            value = v;
            formatted = f;
        }

        /**
         * Gets the wrapped value.
         *
         * @return The value.
         */
        public T getValue() {
            return value;
        }

        /**
         * Gets the formatted value.
         *
         * @return The formatted value.
         */
        @Override
        public String toString() {
            return formatted;
        }

        /**
         * {@inheritDoc}
         *
         * @return {@inheritDoc}
         */
        @Override
        public int compareTo(FormattedValue<T> o) {
            if (null == o) {
                return -1;
            }
            return getValue().compareTo(o.getValue());
        }
    }

    /**
     * Provides a lazy view to a {@link Part} for use in a UI (e.g. sorting in a table).
     */
    public class PartProxy {
        private Part part;
        private String details;
        private TargetProxy targetProxy;
        private FormattedValue<Money> cost;
        private PartInventory inventories;
        private FormattedValue<Integer> ordered;
        private FormattedValue<Integer> supply;
        private FormattedValue<Integer> transit;

        /**
         * Initializes a new instance of the class to provide a proxy view into a part.
         *
         * @param p The part to proxy. Must not be null.
         */
        public PartProxy(Part p) {
            part = Objects.requireNonNull(p);
        }

        /**
         * Updates the proxied view of the properties which changed outside the proxy.
         */
        public void updateTargetAndInventories() {
            targetProxy = null;
            inventories = null;
            ordered = null;
            supply = null;
            transit = null;
        }

        /**
         * Gets the part being proxied.
         *
         * @return The part being proxied.
         */
        public Part getPart() {
            return part;
        }

        /**
         * Gets the part's name.
         *
         * @return The part's name.
         */
        public String getName() {
            return part.getName();
        }

        /**
         * Gets the part's details.
         *
         * @return The part's detailed.
         */
        public String getDetails() {
            if (null == details) {
                details = part.getDetails();
            }

            return details;
        }

        /**
         * Gets the part's cost, suitable for use in a UI element which requires both a display value and a sortable
         * value.
         *
         * @return The part's cost as a {@link FormattedValue}
         */
        public FormattedValue<Money> getCost() {
            if (null == cost) {
                Money actualValue = part.getActualValue();
                cost = new FormattedValue<>(actualValue, actualValue.toAmountString());
            }
            return cost;
        }

        /**
         * Gets the part's tonnage.
         *
         * @return The part's tonnage.
         */
        public double getTonnage() {
            return Math.round(part.getTonnage() * 100) / 100.0;
        }

        /**
         * Gets the part's tech base.
         *
         * @return The part's tech base.
         */
        public String getTechBase() {
            return part.getTechBaseName();
        }

        /**
         * Gets the part's {@link TargetRoll}.
         *
         * @return A {@link TargetProxy} representing the target roll for the part.
         */
        public TargetProxy getTarget() {
            if (null == targetProxy) {
                IAcquisitionWork shoppingItem = part.getMissingPart();
                if (null == shoppingItem && part instanceof IAcquisitionWork) {
                    shoppingItem = (IAcquisitionWork) part;
                }
                if (null != shoppingItem) {
                    TargetRoll target = campaign.getTargetForAcquisition(shoppingItem, getLogisticsPerson(), true);
                    targetProxy = new TargetProxy(target);
                } else {
                    targetProxy = new TargetProxy(null);
                }
            }

            return targetProxy;
        }

        /**
         * Gets the part's quantity on order, suitable for use in a UI element which requires both a display value
         * and a sortable value.
         *
         * @return The part's quantity on order as a {@link FormattedValue}
         */
        public FormattedValue<Integer> getOrdered() {
            if (null == inventories) {
                inventories = campaign.getPartInventory(part);
            }
            if (null == ordered) {
                ordered = new FormattedValue<>(inventories.getOrdered(), inventories.orderedAsString());
            }
            return ordered;
        }

        /**
         * Gets the part's quantity on hand, suitable for use in a UI element which requires both a display value
         * and a sortable value.
         *
         * @return The part's quantity on hand as a {@link FormattedValue}
         */
        public FormattedValue<Integer> getSupply() {
            if (null == inventories) {
                inventories = campaign.getPartInventory(part);
            }
            if (null == supply) {
                supply = new FormattedValue<>(inventories.getSupply(), inventories.supplyAsString());
            }
            return supply;
        }

        /**
         * Gets the part's quantity in transit, suitable for use in a UI element which requires both a display value
         * and a sortable value.
         *
         * @return The part's quantity in transit as a {@link FormattedValue}
         */
        public FormattedValue<Integer> getTransit() {
            if (null == inventories) {
                inventories = campaign.getPartInventory(part);
            }
            if (null == transit) {
                transit = new FormattedValue<>(inventories.getTransit(), inventories.transitAsString());
            }
            return transit;
        }
    }

    public PartsStoreModel(CampaignGUI gui, ArrayList<Part> inventory) {
        campaign = gui.getCampaign();
        data = new ArrayList<>(inventory.size());
        logisticsPerson = null;

        for (Part part : inventory) {
            data.add(new PartProxy(part));
        }
    }


    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return N_COL;
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case COL_NAME -> "Name";
            case COL_DETAIL -> "Detail";
            case COL_TECH_BASE -> "Tech";
            case COL_COST -> "Cost";
            case COL_TON -> "Ton";
            case COL_TARGET -> "Target";
            case COL_QUEUE -> "# Ordered";
            case COL_SUPPLY -> "# Supply";
            case COL_TRANSIT -> "# Transit";
            default -> "?";
        };
    }

    @Override
    public Object getValueAt(int row, int col) {
        if (data.isEmpty()) {
            return "";
        }
        PartProxy part = data.get(row);
        return switch(col) {
            case COL_NAME -> part.getName();
            case COL_DETAIL -> part.getDetails();
            case COL_TECH_BASE -> part.getTechBase();
            case COL_COST -> part.getCost();
            case COL_TON -> part.getTonnage();
            case COL_TARGET -> part.getTarget();
            case COL_SUPPLY -> part.getSupply();
            case COL_TRANSIT -> part.getTransit();
            case COL_QUEUE -> part.getOrdered();
            default -> "?";
        };
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    public PartProxy getPartProxyAt(int row) {
        return data.get(row);
    }

    public Part getPartAt(int row) {
        return data.get(row).getPart();
    }

    public int getColumnWidth(int c) {
        return switch (c) {
            case COL_NAME, COL_DETAIL -> 100;
            case COL_COST, COL_TARGET -> 40;
            case COL_SUPPLY, COL_TRANSIT, COL_QUEUE -> 30;
            default -> 15;
        };
    }

    public int getAlignment(int col) {
        return switch (col) {
            case COL_COST, COL_TON -> SwingConstants.RIGHT;
            case COL_TARGET -> SwingConstants.CENTER;
            default -> SwingConstants.LEFT;
        };
    }

    public String getTooltip(int row, int col) {
        PartProxy part;
        if (data.isEmpty()) {
            return null;
        } else {
            part = data.get(row);
        }
        if (col == COL_TARGET) {
            return part.getTarget().getDescription();
        }
        return null;
    }

    public Renderer getRenderer() {
        return new Renderer();
    }

    public class Renderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
              boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setOpaque(true);
            int actualCol = table.convertColumnIndexToModel(column);
            int actualRow = table.convertRowIndexToModel(row);
            setHorizontalAlignment(getAlignment(actualCol));
            setToolTipText(getTooltip(actualRow, actualCol));

            return this;
        }

    }
    

    private Person getLogisticsPerson() {
        if (null == logisticsPerson) {
            logisticsPerson = campaign.getLogisticsPerson();
        }
        return logisticsPerson;
    }
}
