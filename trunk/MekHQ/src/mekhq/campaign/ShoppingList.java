/*
 * ShoppingList.java
 * 
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

package mekhq.campaign;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.UUID;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Entity;
import mekhq.MekHQ;
import mekhq.Version;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.work.IAcquisitionWork;

/**
 *  An arraylist of IAcquisitionWork
 *  
 *  When a new acquisition is requested (via the parts store or the acquisition tab), we 
 *  iterate through this list and look for the MissingPart.getNewPart that matches
 *  the desired part. Here are the possible outcomes:
 *  
 *  - We find it, but we cannot check today, so we add the quantity requested
 *  - We don't find it, we immediately check and add to the list if we fail
 *  
 *  On Campaign.newDay, we also cycle through the list and check any items that have no
 *  more days to wait for the next check. 
 *  
 *  Checking procedure
 *  Using a while loop, we keep checking using an acquisition roll until we fail or we hit 
 *  zero quantity. If we hit zero quantity, then we can remove the item. If we fail, then 
 *  we reset the dayCounter to the max.
 *  
 *  We also now only use one person to make all checks. We allow the user to set the skill
 *  and other options for who makes the check in the campaign options.,
 *   
 *   Do we use a separate shopping list for new units?
 */
public class ShoppingList implements MekHqXmlSerializable {

    private ArrayList<IAcquisitionWork> shoppingList;
    
    public ShoppingList() {
        shoppingList = new ArrayList<IAcquisitionWork>();
    }
    
    public IAcquisitionWork getShoppingItem(Object newEquipment) {
        for(IAcquisitionWork shoppingItem : shoppingList) {
            if(isSameEquipment(shoppingItem.getNewEquipment(), newEquipment)) {
                return shoppingItem;
            }
        }
        return null;
    }
    
    public void removeItem(Object equipment) {
        int idx = -1;
        int row = 0;
        for(IAcquisitionWork shoppingItem : shoppingList) {           
            if(isSameEquipment(shoppingItem.getNewEquipment(), equipment)) {
                idx = row;
                break;
            }
            row++;
        }
        if(idx > -1) {
            shoppingList.remove(idx);
        }
    }
    
    public void addShoppingItem(IAcquisitionWork newWork, int quantity, Campaign campaign) {
        Person person = campaign.getLogisticsPerson();
        if(null == person && !campaign.getCampaignOptions().getAcquisitionSkill().equals(CampaignOptions.S_AUTO)) {
            campaign.addReport("Your force has no one capable of acquiring equipment.");
            return;
        }
        for(IAcquisitionWork shoppingItem : shoppingList) {
            if(isSameEquipment(shoppingItem.getNewEquipment(), newWork.getNewEquipment())) {
                campaign.addReport(newWork.getShoppingListReport(quantity));
                while(quantity > 0) {
                    shoppingItem.incrementQuantity();
                    quantity--;
                }
                return;
            }
        }
        boolean canAfford = true;
        if(campaign.getFunds() < newWork.getBuyCost()) {
             campaign.addReport("<font color='red'><b>You cannot afford to purchase " + newWork.getAcquisitionName() + "</b></font>");
             canAfford = false;
        }
        while(canAfford && quantity > 0 && campaign.acquireEquipment(newWork, person)) {
            quantity--;
            if(quantity > 0 && campaign.getFunds() < newWork.getBuyCost()) {
                canAfford = false;
                campaign.addReport("<font color='red'><b>You cannot afford to purchase " + newWork.getAcquisitionName() + "</b></font>");
            } 
        }   
        if(quantity > 0) {
            campaign.addReport(newWork.getShoppingListReport(quantity));
            while(quantity > 1) {
                newWork.incrementQuantity();
                quantity--;
            }
            shoppingList.add(newWork);
        }
    }
    
    public void newDay(Campaign campaign) {
        Person person = campaign.getLogisticsPerson();
        if(null == person && !campaign.getCampaignOptions().getAcquisitionSkill().equals(CampaignOptions.S_AUTO)) {
            campaign.addReport("Your force has no one capable of acquiring equipment.");
            return;
        }
        ArrayList<IAcquisitionWork> newShoppingList = new ArrayList<IAcquisitionWork>();
        for(IAcquisitionWork shoppingItem : shoppingList) {
            shoppingItem.decrementDaysToWait();
            if(shoppingItem.getDaysToWait() <= 0) {
                boolean canAfford = true;
                if(campaign.getFunds() < shoppingItem.getBuyCost()) {
                     campaign.addReport("<font color='red'><b>You cannot afford to purchase " + shoppingItem.getAcquisitionName() + "</b></font>");
                     canAfford = false;
                }
                while(canAfford && shoppingItem.getQuantity() > 0 && campaign.acquireEquipment(shoppingItem, person)) {
                    shoppingItem.decrementQuantity();
                    if(shoppingItem.getQuantity() > 0 && campaign.getFunds() < shoppingItem.getBuyCost()) {
                        canAfford = false;
                        campaign.addReport("<font color='red'><b>You cannot afford to purchase " + shoppingItem.getAcquisitionName() + "</b></font>");
                    } 
                }
            }
            if(shoppingItem.getQuantity() > 0 || shoppingItem.getDaysToWait() > 0) {  
                newShoppingList.add(shoppingItem);
            }
        }
        shoppingList = newShoppingList;
        
    } 
    
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<shoppingList>");
        for(IAcquisitionWork shoppingItem : shoppingList) {
            if(shoppingItem instanceof Part) {
                ((Part)shoppingItem).writeToXml(pw1, indent+1);
            } 
            else if(shoppingItem instanceof UnitOrder) {
                ((UnitOrder)shoppingItem).writeToXml(pw1, indent+1);
            } 
        }
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</shoppingList>");        
    }
    
    public static ShoppingList generateInstanceFromXML(Node wn, Campaign c, Version version) {
        ShoppingList retVal = new ShoppingList();
        
        NodeList nl = wn.getChildNodes();
        
        try {
            for (int x=0; x<nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("part")) {
                    Part p = Part.generateInstanceFromXML(wn2, version);
                    p.setCampaign(c);
                    if(p instanceof IAcquisitionWork) {
                        retVal.shoppingList.add((IAcquisitionWork)p);
                    }
                } 
                else if (wn2.getNodeName().equalsIgnoreCase("unitOrder")) {
                    UnitOrder u = UnitOrder.generateInstanceFromXML(wn2, version);
                    u.campaign = c;
                    if(null != u.getEntity()) {
                        retVal.shoppingList.add(u);
                    }
                } 
            }
        } catch (Exception ex) {
            // Doh!
            MekHQ.logError(ex);
        }
        
        return retVal;
    }
    
    public void restore() {
        for(IAcquisitionWork shoppingItem : shoppingList) {
            if(shoppingItem instanceof MissingEquipmentPart) {
                ((MissingEquipmentPart)shoppingItem).restore();
            }
        }
    }
    
    public ArrayList<IAcquisitionWork> getPartList() {
        ArrayList<IAcquisitionWork> partList = new ArrayList<IAcquisitionWork>();
        for(IAcquisitionWork shoppingItem : shoppingList) {
            if(shoppingItem instanceof Part) {
                partList.add(shoppingItem);
            }
        }
        return partList;
    }
    
    public ArrayList<IAcquisitionWork> getUnitList() {
        ArrayList<IAcquisitionWork> unitList = new ArrayList<IAcquisitionWork>();
        for(IAcquisitionWork shoppingItem : shoppingList) {
            if(shoppingItem instanceof UnitOrder) {
                unitList.add(shoppingItem);
            }
        }
        return unitList;
    }
    
    private boolean isSameEquipment(Object equipment, Object newEquipment) {
        if(newEquipment instanceof Part && equipment instanceof Part) {
            if(((Part)equipment).isSamePartType((Part)newEquipment)) {
                return true;
            }
        }
        if(newEquipment instanceof Entity && equipment instanceof Entity) {
            Entity entityA = (Entity)newEquipment;
            Entity entityB = (Entity)equipment;
            if(entityA.getChassis().equals(entityB.getChassis())
                    && entityA.getModel().equals(entityB.getModel())) {
                return true;
            }
        }
        return false;
    }
    
}