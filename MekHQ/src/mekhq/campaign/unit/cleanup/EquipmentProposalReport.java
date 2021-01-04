package mekhq.campaign.unit.cleanup;

import megamek.common.EquipmentType;
import megamek.common.Mounted;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;

public class EquipmentProposalReport {
    /**
     * Creates a message detailing the results of the unscrambling.
     * @param proposal The unscrambling proposal.
     * @return A String describing the result of the unscrambling operation.
     */
    public static String createReport(Unit unit, EquipmentProposal proposal) {
        StringBuilder builder = new StringBuilder();

        if (!proposal.isReduced()) {
            builder.append(String.format("Could not unscramble equipment for %s (%s)\r\n\r\n",
                    unit.getName(), unit.getId()));
            for (Part part : proposal.getParts()) {
                if (proposal.hasProposal(part)) {
                    continue;
                }

                builder.append(" - ").append(part.getPartName()).append(" equipmentNum: ");
                if (part instanceof EquipmentPart) {
                    builder.append(((EquipmentPart) part).getEquipmentNum()).append("\r\n");
                }
                else if (part instanceof MissingEquipmentPart) {
                    builder.append(((MissingEquipmentPart) part).getEquipmentNum()).append("\r\n");
                }
            }
        } else {
            builder.append(String.format("Unscrambled equipment for %s (%s)\r\n\r\n",
                    unit.getName(), unit.getId()));
        }

        builder.append("\r\nEquipment Parts:\r\n");
        for (Part p : unit.getParts()) {
            if (!(p instanceof EquipmentPart) &&
                    (!(p instanceof MissingEquipmentPart))) {
                continue;
            }
            int equipNum;
            if (p instanceof EquipmentPart) {
                EquipmentPart ePart = (EquipmentPart) p;
                equipNum = ePart.getEquipmentNum();
            } else {
                MissingEquipmentPart mePart = (MissingEquipmentPart) p;
                equipNum = mePart.getEquipmentNum();
            }
            boolean isMissing = !proposal.hasProposal(p);
            String eName = equipNum >= 0 ? unit.getEntity().getEquipment(equipNum).getName() : "<None>";
            if (isMissing) {
                eName = "<Incorrect>";
            }
            builder.append(String.format(" %d: %s %s %s %s\r\n", equipNum, p.getName(), p.getLocationName(), eName, isMissing ? " (Missing)" : ""));
        }

        builder.append("\r\nEquipment:\r\n");
        for (Mounted m : unit.getEntity().getEquipment()) {
            int equipNum = unit.getEntity().getEquipmentNum(m);
            EquipmentType mType = m.getType();
            boolean isAvailable = proposal.getEquipment(equipNum) != null;
            builder.append(String.format(" %d: %s %s%s\r\n", equipNum, m.getName(), mType.getName(), isAvailable ? " (Available)" : ""));
        }

        return builder.toString();
    }
}
