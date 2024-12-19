/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

package mekhq.campaign.parts;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Node;

import megamek.common.ITechnology;
import megamek.common.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.work.IAcquisitionWork;

public class RefitKit extends Part implements IAcquisitionWork {

    private List<Part> partList;
    private int tons;
    private Money stickerPrice;

    public RefitKit() {
        super(0, false, null);
        partList = new ArrayList<Part>();
        tons = 0;
        stickerPrice = Money.of(0);
    }
    
    public RefitKit(String oldName, String newName, Campaign campaign) {
        super(0, false, campaign);

        partList = new ArrayList<Part>();
        tons = 0;
        stickerPrice = Money.of(0);

        name = String.format("Refit Kit (%s -> %s)", oldName, newName);
    }
    
    public void setName(String newName) {
        name = newName;
    }


    public void addPart(Part part) {
        partList.add(part);
        tons += part.getTonnage();
        stickerPrice = stickerPrice.plus(part.getStickerPrice().multipliedBy(1.10));
    }
    
    public List<Part> getPartList() {
        return partList;
    }


    @Override
    public Money getStickerPrice() {
        return stickerPrice;
    }

    @Override
    public Money getActualValue() {
        return adjustCostsForCampaignOptions(getStickerPrice());
    }

    @Override
    public boolean isSamePartType(Part other) {
        if (!(other instanceof RefitKit)) {
            return false;
        }
        if (!getName().equals(((RefitKit) other).getName())) {
            return false;
        }
        Iterator<Part> thisIter = partList.iterator();
        Iterator<Part> otherIter = ((RefitKit) other).getPartList().iterator();
        while (thisIter.hasNext() && otherIter.hasNext()) {
            Part thisPart = thisIter.next();
            Part otherPart = otherIter.next();
            if (!thisPart.isSamePartType(otherPart)) {
                return false;
            }
        }
        if (thisIter.hasNext() || otherIter.hasNext()) {
            return false;
        }
        return true;
    }

    @Override
    public Part clone() {
        RefitKit newKit = new RefitKit();
        newKit.setName(name);
        for (Part part : partList) {
            newKit.addPart(part.clone());
        }
        return newKit;
    }

    public String getDetails() {
        return String.format("%s Parts", partList.size());
    }


    @Override
    public void writeToXML(PrintWriter pw, int indent) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'writeToXML'");
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'loadFieldsFromXmlNode'");
    }

    @Override
    public IAcquisitionWork getAcquisitionWork() {
        return this;
    }

    /**
     * We are the new equipment
     */
    @Override
    public Part getNewEquipment() {
        return this;
    }



    @Override
    public TargetRoll getAllAcquisitionMods() {
        return new TargetRoll();
    }

    
    @Override
    public double getTonnage() {
        return tons;
    }
    
    @Override
    public Money getBuyCost() {
        return getActualValue();
    }


    // region Stubs

    /**
     * This item can't be repaired
     */
    @Override
    public int getBaseTime() {
        return 0;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        return;
    }

    @Override
    public void updateConditionFromPart() {
        return;
    }

    @Override
    public void remove(boolean salvage) {
        return;
    }

    @Override
    public MissingPart getMissingPart() {
        return null;
    }

    @Override
    public int getLocation() {
        return -1;
    }

    @Override
    public String checkFixable() {
        return "";
    }


    @Override
    public boolean needsFixing() {
        return false;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public String getLocationName() {
        return "";
    }

    @Override
    public ITechnology getTechAdvancement() {
        return TA_GENERIC;
    }

    @Override
    public String getAcquisitionName() {
        return getName();
    }

    @Override
    public String getAcquisitionDisplayName() {
        return getName();

    }

    @Override
    public String getAcquisitionDesc() {
        return getDetails();
    }

    @Override
    public String getAcquisitionExtraDesc() {
        return getDetails();
    }

    @Override
    public String getAcquisitionBonus() {
        return "";
    }

    @Override
    public Part getAcquisitionPart() {
        return this;
    }

    @Override
    public String find(int transitDays) {
        return "";
    }

    @Override
    public String failToFind() {
        return "";
    }

    @Override
    public boolean isIntroducedBy(int year, boolean clan, int techFaction) {
        return true;
    }

    @Override
    public boolean isExtinctIn(int year, boolean clan, int techFaction) {
        return true;
    }

}
