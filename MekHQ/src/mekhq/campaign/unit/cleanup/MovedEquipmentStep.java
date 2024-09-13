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
