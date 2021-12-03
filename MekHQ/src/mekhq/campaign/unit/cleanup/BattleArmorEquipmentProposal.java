/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit.cleanup;

import megamek.common.BattleArmor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.BattleArmorEquipmentPart;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.unit.Unit;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BattleArmorEquipmentProposal extends EquipmentProposal {
    //region Variable Declarations
    private final int squadSize;
    //endregion Variable Declarations

    //region Constructors
    public BattleArmorEquipmentProposal(final Unit unit) {
        super(unit);

        squadSize = ((BattleArmor) unit.getEntity()).getSquadSize();
    }
    //endregion Constructors

    @Override
    public void proposeMapping(final Part part, final int equipmentNum) {
        // We do not clear the equipment because multiple battle armor parts may use the same
        // equipment.
        mapped.put(part, equipmentNum);
    }

    @Override
    public void apply() {
        super.apply();

        // Clear used equipment from our list
        for (final int equipmentNum : mapped.values()) {
            equipment.remove(equipmentNum);
        }

        // Assign troopers per mount
        final Map<Integer, List<BattleArmorEquipmentPart>> baPartMap = mapped.keySet().stream()
                .filter(part -> part instanceof BattleArmorEquipmentPart)
                .map(part -> (BattleArmorEquipmentPart) part)
                .collect(Collectors.groupingBy(EquipmentPart::getEquipmentNum));

        for (final List<BattleArmorEquipmentPart> parts : baPartMap.values()) {
            // Try to find one for each trooper; if the Entity has multiple pieces of equipment of
            // this type this will make sure we're only setting one group to this equipment number.
            final Part[] perTrooper = new Part[squadSize];
            for (final EquipmentPart part : parts) {
                final int trooper = ((BattleArmorEquipmentPart) part).getTrooper();
                if (trooper > 0) {
                    perTrooper[trooper - 1] = part;
                }
            }

            // Assign a part to any empty position and set the trooper field
            for (int t = 0; t < perTrooper.length; t++) {
                if (perTrooper[t] == null) {
                    for (final Part part : parts) {
                        if (((BattleArmorEquipmentPart) part).getTrooper() < 1) {
                            ((BattleArmorEquipmentPart) part).setTrooper(t + 1);
                            break;
                        }
                    }
                }
            }
        }
    }
}
