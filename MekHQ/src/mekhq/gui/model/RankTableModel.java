/*
 * Copyright (c) 2014-2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.model;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import megamek.common.annotations.Nullable;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.gui.utilities.MekHqTableCellRenderer;

public class RankTableModel extends DefaultTableModel {
    //region Variable Declarations
    private static final long serialVersionUID = 534443424190075264L;

    private RankSystem rankSystem;

    public final static int COL_NAME_RATE = 0;
    public final static int COL_NAME_MW = 1;
    public final static int COL_NAME_ASF = 2;
    public final static int COL_NAME_VEE = 3;
    public final static int COL_NAME_NAVAL = 4;
    public final static int COL_NAME_INF = 5;
    public final static int COL_NAME_TECH = 6;
    public final static int COL_NAME_MEDICAL = 7;
    public final static int COL_NAME_ADMIN = 8;
    public final static int COL_NAME_CIVILIAN = 9;
    public final static int COL_OFFICER = 10;
    public final static int COL_PAYMULT = 11;
    public final static int COL_NUM = 12;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    public RankTableModel(final RankSystem rankSystem) {
        setRankSystem(rankSystem);
    }
    //endregion Constructors

    //region Getters/Setters
    public RankSystem getRankSystem() {
        return rankSystem;
    }

    /**
     * @param rankSystem The system to set the model for. Null values are properly handled but are
     *                   considered to be an unexpected error condition and thus do not change the
     *                   underlying model.
     */
    public void setRankSystem(final @Nullable RankSystem rankSystem) {
        if (rankSystem == null) {
            MekHQ.getLogger().error("Attempted to set based on a null rank system, returning without setting any data");
            return;
        }

        setRankSystemDirect(rankSystem);

        final List<Rank> ranks = rankSystem.getRanks();
        final Object[][] array = new Object[ranks.size()][RankTableModel.COL_NUM];
        for (int i = 0; i < ranks.size(); i++) {
            final Rank rank = ranks.get(i);
            final String rating;
            if (i > Rank.RWO_MAX) {
                rating = "O" + (i - Rank.RWO_MAX);
            } else if (i > Rank.RE_MAX) {
                rating = "WO" + (i - Rank.RE_MAX);
            } else {
                rating = "E" + i;
            }
            array[i][RankTableModel.COL_NAME_RATE] = rating;
            array[i][RankTableModel.COL_NAME_MW] = rank.getNameWithLevels(Profession.MECHWARRIOR);
            array[i][RankTableModel.COL_NAME_ASF] = rank.getNameWithLevels(Profession.AEROSPACE);
            array[i][RankTableModel.COL_NAME_VEE] = rank.getNameWithLevels(Profession.VEHICLE);
            array[i][RankTableModel.COL_NAME_NAVAL] = rank.getNameWithLevels(Profession.NAVAL);
            array[i][RankTableModel.COL_NAME_INF] = rank.getNameWithLevels(Profession.INFANTRY);
            array[i][RankTableModel.COL_NAME_TECH] = rank.getNameWithLevels(Profession.TECH);
            array[i][RankTableModel.COL_NAME_MEDICAL] = rank.getNameWithLevels(Profession.MEDICAL);
            array[i][RankTableModel.COL_NAME_ADMIN] = rank.getNameWithLevels(Profession.ADMIN);
            array[i][RankTableModel.COL_NAME_CIVILIAN] = rank.getNameWithLevels(Profession.CIVILIAN);
            array[i][RankTableModel.COL_OFFICER] = rank.isOfficer();
            array[i][RankTableModel.COL_PAYMULT] = rank.getPayMultiplier();
        }

        setDataVector(array, resources.getString("RankTableModel.columnNames").split(","));
    }

    public void setRankSystemDirect(final RankSystem rankSystem) {
        this.rankSystem = rankSystem;
    }
    //endregion Getters/Setters

    @Override
    public boolean isCellEditable(final int row, final int column) {
        return !getRankSystem().getType().isDefault() && (column != COL_NAME_RATE) && (column != COL_OFFICER);
    }

    @Override
    public Class<?> getColumnClass(final int column) {
        switch (column) {
            case COL_NAME_RATE:
            case COL_NAME_MW:
            case COL_NAME_ASF:
            case COL_NAME_VEE:
            case COL_NAME_NAVAL:
            case COL_NAME_INF:
            case COL_NAME_TECH:
            case COL_NAME_MEDICAL:
            case COL_NAME_ADMIN:
            case COL_NAME_CIVILIAN:
                return String.class;
            case COL_OFFICER:
                return Boolean.class;
            case COL_PAYMULT:
                return Double.class;
            default:
                return getValueAt(0, column).getClass();
        }
    }

    public int getColumnWidth(final int column) {
        switch (column) {
            case COL_NAME_RATE:
                return 100;
            case COL_OFFICER:
                return 250;
            default:
                return 500;
        }
    }

    public int getAlignment(final int column) {
        switch (column) {
            case COL_NAME_RATE:
            case COL_NAME_MW:
            case COL_NAME_ASF:
            case COL_NAME_VEE:
            case COL_NAME_NAVAL:
            case COL_NAME_INF:
            case COL_NAME_TECH:
            case COL_NAME_MEDICAL:
            case COL_NAME_ADMIN:
            case COL_NAME_CIVILIAN:
                return SwingConstants.LEFT;
            default:
                return SwingConstants.CENTER;
        }
    }

    public String getToolTip(final int column) {
        switch (column) {
            case COL_NAME_RATE:
                return resources.getString("RankTableModel.COL_NAME_RATE.toolTipText");
            case COL_NAME_MW:
                return Profession.MECHWARRIOR.getToolTipText();
            case COL_NAME_ASF:
                return Profession.AEROSPACE.getToolTipText();
            case COL_NAME_VEE:
                return Profession.VEHICLE.getToolTipText();
            case COL_NAME_NAVAL:
                return Profession.NAVAL.getToolTipText();
            case COL_NAME_INF:
                return Profession.INFANTRY.getToolTipText();
            case COL_NAME_TECH:
                return Profession.TECH.getToolTipText();
            case COL_NAME_MEDICAL:
                return Profession.MEDICAL.getToolTipText();
            case COL_NAME_ADMIN:
                return Profession.ADMIN.getToolTipText();
            case COL_NAME_CIVILIAN:
                return Profession.CIVILIAN.getToolTipText();
            case COL_OFFICER:
                return resources.getString("RankTableModel.COL_OFFICER.toolTipText");
            case COL_PAYMULT:
                return resources.getString("RankTableModel.COL_PAYMULT.toolTipText");
            default:
                MekHQ.getLogger().error("Unknown column in RankTableModel of " + column);
                return resources.getString("RankTableModel.defaultToolTip.toolTipText");
        }
    }

    public List<Rank> getRanks() {
        try {
            final List<Rank> ranks = new ArrayList<>();

            // Java annoyingly doesn't have typed vectors in the DefaultTableModel, but we can just
            // suppress the warnings this causes
            @SuppressWarnings(value = "rawtypes") final Vector<Vector> vectors = getDataVector();
            for (@SuppressWarnings(value = "rawtypes") Vector row : vectors) {
                final String[] names = {
                        (String) row.get(RankTableModel.COL_NAME_MW), (String) row.get(RankTableModel.COL_NAME_ASF),
                        (String) row.get(RankTableModel.COL_NAME_VEE), (String) row.get(RankTableModel.COL_NAME_NAVAL),
                        (String) row.get(RankTableModel.COL_NAME_INF), (String) row.get(RankTableModel.COL_NAME_TECH),
                        (String) row.get(RankTableModel.COL_NAME_MEDICAL), (String) row.get(RankTableModel.COL_NAME_ADMIN),
                        (String) row.get(RankTableModel.COL_NAME_CIVILIAN)
                };
                final boolean officer = (boolean) row.get(RankTableModel.COL_OFFICER);
                final double paymentMultiplier = (double) row.get(RankTableModel.COL_PAYMULT);
                ranks.add(new Rank(names, officer, paymentMultiplier));
            }
            return ranks;
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
            return new ArrayList<>();
        }
    }

    public TableCellRenderer getRenderer() {
        return new RankTableModel.Renderer();
    }

    public class Renderer extends MekHqTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value,
                                                       final boolean isSelected, final boolean hasFocus,
                                                       final int row, final int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            final int actualCol = table.convertColumnIndexToModel(column);
            setToolTipText(getToolTip(actualCol));
            setOpaque(true);
            setHorizontalAlignment(getAlignment(actualCol));
            return this;
        }
    }
}
