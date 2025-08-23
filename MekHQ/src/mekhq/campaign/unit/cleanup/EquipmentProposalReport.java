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

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;

public final class EquipmentProposalReport {
    // region Constructors
    private EquipmentProposalReport() {

    }
    // endregion Constructors

    /**
     * Creates a message detailing the results of the unscrambling.
     *
     * @param proposal The unscrambling proposal.
     *
     * @return A String describing the result of the unscrambling operation.
     */
    public static String createReport(final EquipmentProposal proposal) {
        final StringBuilder builder = new StringBuilder();

        final Unit unit = proposal.getUnit();
        if (!proposal.isReduced()) {
            builder.append(String.format("Could not unscramble equipment for %s (%s)\n\n",
                  unit.getName(), unit.getId()));
            for (final Part part : proposal.getParts()) {
                if (proposal.hasProposal(part)) {
                    continue;
                }

                builder.append(" - ").append(part.getPartName()).append(" equipmentNum: ")
                      .append(proposal.getOriginalMapping(part)).append("\n");
            }
        } else {
            builder.append(String.format("Unscrambled equipment for %s (%s)\n\n",
                  unit.getName(), unit.getId()));
        }

        builder.append("\nEquipment Parts:\n");
        for (final Part part : proposal.getParts()) {
            final int equipNum;
            if (part instanceof EquipmentPart) {
                equipNum = ((EquipmentPart) part).getEquipmentNum();
            } else if (part instanceof MissingEquipmentPart) {
                equipNum = ((MissingEquipmentPart) part).getEquipmentNum();
            } else {
                continue;
            }

            final boolean isMissing = !proposal.hasProposal(part);
            final String eName = isMissing ?
                                       "<Incorrect>"
                                       :
                                       ((equipNum >= 0) ? unit.getEntity().getEquipment(equipNum).getName() : "<None>");

            builder.append(
                  String.format(" %d: %s %s %s %s\n", (!isMissing ? equipNum : proposal.getOriginalMapping(part)),
                        part.getName(), part.getLocationName(), eName, isMissing ? " (Missing)" : ""));
        }

        builder.append("\nEquipment:\n");
        for (final Mounted<?> m : unit.getEntity().getEquipment()) {
            final int equipNum = unit.getEntity().getEquipmentNum(m);
            final EquipmentType mType = m.getType();
            final boolean isAvailable = proposal.getEquipment(equipNum) != null;
            builder.append(String.format(" %d: %s %s%s\n", equipNum, m.getName(), mType.getName(),
                  isAvailable ? " (Available)" : ""));
        }

        return builder.toString();
    }
}
