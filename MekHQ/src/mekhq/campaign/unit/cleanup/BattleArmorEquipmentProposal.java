/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit.cleanup;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import megamek.common.battleArmor.BattleArmor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.BattleArmorEquipmentPart;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.unit.Unit;

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
                                                                             .collect(Collectors.groupingBy(
                                                                                   EquipmentPart::getEquipmentNum));

        for (final List<BattleArmorEquipmentPart> parts : baPartMap.values()) {
            // Try to find one for each trooper; if the Entity has multiple pieces of equipment of
            // this type this will make sure we're only setting one group to this equipment number.
            final Part[] perTrooper = new Part[squadSize];
            for (final BattleArmorEquipmentPart part : parts) {
                final int trooper = part.getTrooper();
                if (trooper > 0) {
                    perTrooper[trooper - 1] = part;
                }
            }

            // Assign a part to any empty position and set the trooper field
            for (int t = 0; t < perTrooper.length; t++) {
                if (perTrooper[t] == null) {
                    for (final BattleArmorEquipmentPart part : parts) {
                        if (part.getTrooper() < 1) {
                            part.setTrooper(t + 1);
                            break;
                        }
                    }
                }
            }
        }
    }
}
