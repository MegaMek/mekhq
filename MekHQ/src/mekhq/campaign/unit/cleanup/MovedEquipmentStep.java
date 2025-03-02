/*
 * Copyright (C) 2021-2025 The MegaMek Team
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
 */
package mekhq.campaign.unit.cleanup;

import java.util.Map.Entry;

import megamek.common.Mounted;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;

public class MovedEquipmentStep extends UnscrambleStep {
    @Override
    public void visit(final EquipmentProposal proposal, final EquipmentPart part) {
        for (final Entry<Integer, Mounted> equipment : proposal.getEquipment()) {
            final Mounted<?> m = equipment.getValue();
            if (m.isDestroyed()) {
                continue;
            }

            if (m.getType().equals(part.getType())) {
                proposal.proposeMapping(part, equipment.getKey());
                return;
            }
        }
    }

    @Override
    public void visit(final EquipmentProposal proposal, final MissingEquipmentPart part) {
        for (final Entry<Integer, Mounted> equipment : proposal.getEquipment()) {
            final Mounted<?> m = equipment.getValue();
            if (m.isDestroyed()) {
                continue;
            }

            if (m.getType().equals(part.getType())) {
                proposal.proposeMapping(part, equipment.getKey());
                return;
            }
        }
    }
}
