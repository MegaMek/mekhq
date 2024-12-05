/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.model;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.AwardBonus;
import mekhq.gui.BasicInfo;
import mekhq.gui.utilities.MekHqTableCellRenderer;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AutoAwardsTableModel extends AbstractTableModel {
    private static final MMLogger logger = MMLogger.create(AutoAwardsTableModel.class);

    public static final int COL_PERSON = 0;
    public static final int COL_NAME = 1;
    public static final int COL_SET = 2;
    public static final int COL_AWARD = 3;
    public static final int COL_DESCRIPTION = 4;
    public static final int N_COL = 5;

    private static final String[] colNames = {
            "Person", "Name", "Set", "Award", "Description"
    };

    private final Campaign campaign;
    private Map<Integer, List<Object>> data;

    public AutoAwardsTableModel(Campaign c) {
        this.campaign = c;
        data = new HashMap<>();
    }

    public void setData(Map<Integer, List<Object>> map) {
        if (map.isEmpty()) {
            logger.error("AutoAwardsDialog failed to pass 'data' into AutoAwardsTableModel");
        } else {
            logger.debug("AutoAwardsDialog passed 'data' into AutoAwardsTableModel: {}", map);
        }

        data = map;
        logger.debug("Translated data: {}", data);
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
        return colNames[column];
    }

    public int getColumnWidth(int column) {
        return switch (column) {
            case COL_PERSON, COL_NAME -> 75;
            case COL_SET -> 40;
            case COL_DESCRIPTION -> 400;
            default -> 30;
        };
    }

    public int getAlignment(int column) {
        return switch (column) {
            case COL_PERSON, COL_DESCRIPTION -> SwingConstants.LEFT;
            default -> SwingConstants.CENTER;
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == COL_AWARD;
    }

    @Override
    public Class<?> getColumnClass(int col) {
        Class<?> retVal = Object.class;
        try {
            Object value = getValueAt(0, col);
            if (value != null) {
                retVal = value.getClass();
            }
        } catch (NullPointerException e) {
            logger.error("autoAwards 'getColumnClass()' failed to parse {}",
                    getValueAt(0, col));
        }
        return retVal;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (data.isEmpty() || !data.containsKey(rowIndex)) {
            logger.error("'data' is empty or does not contain key for index: {}", rowIndex);
            return "";
        }

        List<Object> rowData = data.get(rowIndex);

        UUID personUUID = (UUID) rowData.get(0);
        Person person = campaign.getPerson(personUUID);
        Award award = (Award) rowData.get(1);

        return switch (columnIndex) {
            case COL_PERSON -> person.makeHTMLRank();
            case COL_NAME -> award.getName();
            case COL_SET -> award.getSet();
            case COL_AWARD -> rowData.get(2);
            case COL_DESCRIPTION -> {
                String awards = getDescriptionString(award);

                yield award.getDescription() + awards;
            }
            default -> "?";
        };
    }

    /**
     * Retrieves a description for the given award based on the campaign's award bonus style.
     *
     * @param award The {@link Award} object for which the description string is generated.
     * @return A {@link String} containing the awards based on the style, including XP and Edge rewards if applicable.
     */
    private String getDescriptionString(Award award) {
        AwardBonus style = campaign.getCampaignOptions().getAwardBonusStyle();
        int xpAward = award.getXPReward();
        int edgeAward = award.getEdgeReward();

        String awards = "";
        if (style.isBoth() || style.isXP()) {
            awards += (xpAward > 0) ? " (" + xpAward + "XP)" : "";
        }
        if (style.isBoth() || style.isEdge()) {
            awards += (edgeAward > 0) ? " (" + edgeAward + " Edge)" : "";
        }
        return awards;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int column) {
        if (column == COL_AWARD) {
            data.get(rowIndex).set(2, value);
        }

        fireTableDataChanged();
    }

    public Person getPerson(int rowIndex) {
        return campaign.getPerson((UUID) data.get(rowIndex).get(0));
    }

    public String getAwardName(int rowIndex) {
        return ((Award) data.get(rowIndex).get(1)).getName();
    }

    public String getAwardSet(int rowIndex) {
        return ((Award) data.get(rowIndex).get(1)).getSet();
    }

    public String getAwardDescription(int rowIndex) {
        return ((Award) data.get(rowIndex).get(1)).getDescription();
    }

    public TableCellRenderer getRenderer(int col) {
        if (col == COL_PERSON) {
            return new VisualRenderer();
        } else {
            return new TextRenderer();
        }
    }

    public class TextRenderer extends MekHqTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int rowIndex, int columnIndex) {
            super.getTableCellRendererComponent(table, value, isSelected,
                    hasFocus, rowIndex, columnIndex);
            int actualColumn = table.convertColumnIndexToModel(columnIndex);

            setHorizontalAlignment(getAlignment(actualColumn));

            return this;
        }
    }

    public class VisualRenderer extends BasicInfo implements TableCellRenderer {
        public VisualRenderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int rowIndex, int columnIndex) {
            int actualColumn = table.convertColumnIndexToModel(columnIndex);
            int actualRow = table.convertRowIndexToModel(rowIndex);

            switch (actualColumn) {
                case COL_PERSON:
                    setText(getPerson(actualRow).getFullDesc(campaign));
                    setImage(getPerson(actualRow).getPortrait().getImage(50));
                    break;
                case COL_NAME:
                    setText(getAwardName(actualRow));
                    break;
                case COL_SET:
                    setText(getAwardSet(actualRow));
                    break;
                case COL_DESCRIPTION:
                    setText(getAwardDescription(actualRow));
                    break;
                default:
            }

            MekHqTableCellRenderer.setupTableColors(this, table, isSelected, hasFocus, rowIndex);

            return this;
        }
    }
}
