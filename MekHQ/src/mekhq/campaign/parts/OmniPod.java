/*
 * Copyright (C) 2017 MegaMek team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.parts;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.EquipmentType;
import megamek.common.TechConstants;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;

/**
 * An empty omnipod, which can be purchased or created when equipment is removed from a pod.
 * When fixed, the omnipod is removed from the warehouse and one replacement part is podded.
 * 
 * @author Neoancient
 *
 */
public class OmniPod extends Part {

    private static final long serialVersionUID = -8236359530423260992L;
    
    // Pods are specific to the type of equipment they contain.
    private MissingPart partType;

    public OmniPod(Part partType, Campaign c) {
        super(0, false, c);
        if (partType instanceof MissingPart) {
            this.partType = (MissingPart)partType;
        } else {
            this.partType = partType.getMissingPart();
        }
        partType.setOmniPodded(false);
        name = "OmniPod";
    }

    @Override
    public String getDetails() {
        return partType.getName();
    }
    
    @Override
    public int getBaseTime() {
        return partType.getBaseTime();
    }

    @Override
    public void updateConditionFromPart() {
        // do nothing
    }

    //This can only be found in the warehouse
    @Override
    public int getLocation() {
        return -1;
    }

    @Override
    public String checkFixable() {
        return null;
    }

    //Podding equipment is a Class D (Maintenance) refit, which carries a +2 modifier.
    @Override
    public int getDifficulty() {
        return partType.getDifficulty() + 2;
    }

    public Part getNewPart() {
        Part part = partType.getNewPart();
        part.setOmniPodded(true);
        return part;
    }

    //Weight is negligible
    @Override
    public double getTonnage() {
        return 0;
    }

    //Using tech rating for Omni construction option from IOps.
    @Override
    public int getTechRating() {
        return EquipmentType.RATING_E;
    }

    @Override
    public int getAvailability(int era) {
        if (era == EquipmentType.ERA_SL
                || (era == EquipmentType.ERA_SW && partType.getTechBase() == T_IS)) {
            return EquipmentType.RATING_X;
        } else if (era == EquipmentType.ERA_DA) {
            return Math.max(partType.getAvailability(era), EquipmentType.RATING_D);
        } else {
            return Math.max(partType.getAvailability(era), EquipmentType.RATING_E);
        }
    }

    @Override
    public int getIntroDate() {
        if (partType.getTechBase() == T_IS) {
            return Math.max(3052, partType.getIntroDate());
        } else {
            return Math.max(2850, partType.getIntroDate());
        }
    }

    @Override
    public int getExtinctDate() {
        return partType.getExtinctDate();
    }

    @Override
    public int getReIntroDate() {
        return partType.getReIntroDate();
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("partType")) {
                partType = (MissingPart)Part.generateInstanceFromXML(wn2, new Version(null));
            }
        }
    }

    @Override
    public String getLocationName() {
        return null;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        //do nothing
    }

    @Override
    public void remove(boolean salvage) {
        //do nothing
    }

    @Override
    public MissingPart getMissingPart() {
        return null;
    }

    @Override
    public boolean needsFixing() {
        return false;
    }

    @Override
    public long getStickerPrice() {
        return (long)Math.ceil(partType.getNewPart().getStickerPrice() / 5.0);
    }

    @Override
    public int getTechLevel() {
        if (partType.isClanTechBase()) {
            return TechConstants.T_CLAN_TW;
        }
        return TechConstants.T_IS_TW_ALL;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof OmniPod
                && (partType.isSamePartType(((OmniPod)part).partType));
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "<partType>");
        partType.writeToXml(pw1, indent + 1);
        pw1.println(MekHqXmlUtil.indentStr(indent + 1) + "</partType>");
    }

    @Override
    public Part clone() {
        return new OmniPod(partType, campaign);
    }

}
