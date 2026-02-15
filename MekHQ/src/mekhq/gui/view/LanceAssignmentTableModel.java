/*
 * Copyright (c) 2014 Carl Spain. All rights reserved.
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.view;

import java.util.ArrayList;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.gui.model.DataTableModel;

class LanceAssignmentTableModel extends DataTableModel<CombatTeam> {
    public static final int COL_FORCE = 0;
    public static final int COL_WEIGHT_CLASS = 1;
    public static final int COL_CONTRACT = 2;
    public static final int COL_ROLE = 3;
    public static final int COL_NUM = 4;

    private final Campaign campaign;

    public LanceAssignmentTableModel(Campaign campaign) {
        this.campaign = campaign;
        data = new ArrayList<>();
        columnNames = new String[] { "Force", "Weight Class", "Mission", "Role" };
    }

    @Override
    public int getColumnCount() {
        return COL_NUM;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnWidth(int col) {
        return switch (col) {
            case COL_FORCE, COL_CONTRACT -> 100;
            case COL_WEIGHT_CLASS -> 5;
            default -> 50;
        };
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return switch (c) {
            case COL_FORCE -> Formation.class;
            case COL_CONTRACT -> AtBContract.class;
            case COL_ROLE -> CombatRole.class;
            default -> String.class;
        };
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return col > COL_WEIGHT_CLASS;
    }

    public CombatTeam getRow(int row) {
        return data.get(row);
    }

    @Override
    public Object getValueAt(int row, int column) {
        final String[] WEIGHT_CODES = { "Ultra-Light", "Light", "Medium", "Heavy", "Assault", "Super Heavy" };

        if (row >= getRowCount()) {
            return "";
        }
        return switch (column) {
            case COL_FORCE -> campaign.getFormation(data.get(row).getFormationId());
            case COL_WEIGHT_CLASS -> WEIGHT_CODES[data.get(row).getWeightClass(campaign)];
            case COL_CONTRACT -> campaign.getMission(data.get(row).getMissionId());
            case COL_ROLE -> data.get(row).getRole();
            default -> "?";
        };
    }

    @Override
    public void setValueAt(Object value, int row, int col) {
        if (col == COL_CONTRACT) {
            data.get(row).setContract((AtBContract) value);
        } else if (col == COL_ROLE) {
            if (value instanceof CombatRole) {
                data.get(row).setRole((CombatRole) value);
                Formation chosenFormation = (Formation) getValueAt(row, COL_FORCE);
                chosenFormation.setCombatRoleInMemory((CombatRole) value);
                for (Formation formation : chosenFormation.getSubFormations()) {
                    formation.setCombatRoleInMemory((CombatRole) value);
                }
            }
        }
        fireTableDataChanged();
    }
}
