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

import megamek.common.EquipmentType;
import megamek.common.Mounted;
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
            final String eName = isMissing ? "<Incorrect>"
                    : ((equipNum >= 0) ? unit.getEntity().getEquipment(equipNum).getName() : "<None>");

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
