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

import static mekhq.campaign.mission.enums.CombatRole.CADRE;
import static mekhq.campaign.mission.enums.CombatRole.FRONTLINE;
import static mekhq.campaign.mission.enums.CombatRole.MANEUVER;
import static mekhq.campaign.mission.enums.CombatRole.PATROL;

import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingConstants;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.gui.model.DataTableModel;

class RequiredLancesTableModel extends DataTableModel<AtBContract> {
    public static final int COL_CONTRACT = 0;
    public static final int COL_TOTAL = 1;
    public static final int COL_FIGHT = 2;
    public static final int COL_DEFEND = 3;
    public static final int COL_SCOUT = 4;
    public static final int COL_TRAINING = 5;
    public static final int COL_NUM = 6;

    private final Campaign campaign;

    public RequiredLancesTableModel(final Campaign campaign) {
        this.campaign = campaign;
        data = new ArrayList<>();
        columnNames = new String[] { "Contract", "Total", MANEUVER.toString(), FRONTLINE.toString(), PATROL.toString(),
                                     CADRE.toString() };
    }

    static int getAssignedCombatElementCount(Campaign campaign, AtBContract contract) {
        int assignedCombatElements = 0;
        for (CombatTeam combatTeam : campaign.getCombatTeamsAsList()) {
            if (!contract.equals(combatTeam.getContract(campaign))) {
                continue;
            }

            CombatRole role = combatTeam.getRole();
            boolean isRoleSuitable = (contract.getContractType().isCadreDuty() && role.isCadre()) || role.isCombatRole();
            if (isRoleSuitable && combatTeam.isEligible(campaign)) {
                assignedCombatElements += combatTeam.getSize(campaign);
            }
        }
        return assignedCombatElements;
    }

    List<String> getDeploymentShortfallSummaries() {
        List<String> shortfalls = new ArrayList<>();
        for (AtBContract contract : data) {
            List<String> contractShortfalls = new ArrayList<>();

            int assignedCombatElements = getAssignedCombatElementCount(campaign, contract);
            int requiredCombatElements = contract.getRequiredCombatElements();
            if (assignedCombatElements < requiredCombatElements) {
                contractShortfalls.add(columnNames[COL_TOTAL] + ' ' + assignedCombatElements + '/' +
                                            requiredCombatElements);
            }

            CombatRole requiredRole = contract.getContractType().getRequiredCombatRole();
            int requiredRoleColumn = getColumnForRole(requiredRole);
            if (requiredRoleColumn >= 0) {
                int assignedRoleElements = getAssignedCombatRoleCount(contract, requiredRole);
                int requiredRoleElements = Math.max(requiredCombatElements / 2, 1);
                if (assignedRoleElements < requiredRoleElements) {
                    contractShortfalls.add(columnNames[requiredRoleColumn] + ' ' + assignedRoleElements + '/' +
                                                requiredRoleElements);
                }
            }

            if (!contractShortfalls.isEmpty()) {
                shortfalls.add(contract.getName() + ": " + String.join(", ", contractShortfalls));
            }
        }
        return shortfalls;
    }

    private int getAssignedCombatRoleCount(AtBContract contract, CombatRole requiredRole) {
        int assignedCombatElements = 0;
        for (CombatTeam combatTeam : campaign.getCombatTeamsAsList()) {
            if (contract.equals(combatTeam.getContract(campaign)) &&
                      (combatTeam.getRole() == requiredRole) &&
                      combatTeam.isEligible(campaign)) {
                assignedCombatElements += combatTeam.getSize(campaign);
            }
        }
        return assignedCombatElements;
    }

    private int getColumnForRole(CombatRole role) {
        if (role == null) {
            return -1;
        }

        return switch (role) {
            case MANEUVER -> COL_FIGHT;
            case FRONTLINE -> COL_DEFEND;
            case PATROL -> COL_SCOUT;
            case CADRE -> COL_TRAINING;
            default -> -1;
        };
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
        if (col == COL_CONTRACT) {
            return 100;
        } else {
            return 20;
        }
    }

    public int getAlignment(int col) {
        if (col == COL_CONTRACT) {
            return SwingConstants.LEFT;
        } else {
            return SwingConstants.CENTER;
        }
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    public AtBContract getRow(int row) {
        return data.get(row);
    }

    @Override
    public Object getValueAt(int row, int column) {
        if (row >= getRowCount()) {
            return "";
        }

        if (COL_CONTRACT == column) {
            return data.get(row).getName();
        }

        AtBContract contract = getRow(row);

        if (column == COL_TOTAL) {
            int t = getAssignedCombatElementCount(campaign, contract);
            if (t < contract.getRequiredCombatElements()) {
                return t + "/" + contract.getRequiredCombatElements();
            }
            return Integer.toString(contract.getRequiredCombatElements());
        }

        CombatRole requiredRole = contract.getContractType().getRequiredCombatRole();
        int requiredRoleColumn = getColumnForRole(requiredRole);

        if (column == requiredRoleColumn) {
            int t = getAssignedCombatRoleCount(contract, requiredRole);
            int required = Math.max(contract.getRequiredCombatElements() / 2, 1);
            if (t < required) {
                return t + "/" + required;
            }
            return Integer.toString(required);
        }

        return "";
    }
}
