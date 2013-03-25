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
    
    public IAcquisitionWork getShoppingItem(Part newPart) {
        for(IAcquisitionWork shoppingItem : shoppingList) {
            if(shoppingItem.getNewPart().isSamePartType(newPart)) {
                return shoppingItem;
            }
        }
        return null;
    }
    
    public void removeItem(Part newPart) {
        int idx = -1;
        int row = 0;
        for(IAcquisitionWork shoppingItem : shoppingList) {
            if(shoppingItem.getNewPart().isSamePartType(newPart)) {
                idx = row;
                break;
            }
            row++;
        }
        if(idx > -1) {
            shoppingList.remove(idx);
        }
    }
    
    public void addShoppingItem(Part newPart, int quantity) {
        Person person = newPart.getCampaign().getLogisticsPerson();
        if(null == person && !newPart.getCampaign().getCampaignOptions().getAcquisitionSkill().equals(CampaignOptions.S_AUTO)) {
            newPart.getCampaign().addReport("Your force has no one capable of acquiring parts.");
            return;
        }
        for(IAcquisitionWork shoppingItem : shoppingList) {
            if(shoppingItem.getNewPart().isSamePartType(newPart)) {
                newPart.getCampaign().addReport(newPart.getShoppingListReport(quantity));
                while(quantity > 0) {
                    shoppingItem.incrementQuantity();
                    quantity--;
                }
                return;
            }
        }
        //if we are still here then this is new so add it and check       
        //The order this is done in below matters because some parts (AmmoBins) can 
        //be both acquisition items and have a valid missing part. So MissingParts should
        //be checked for first. 
        IAcquisitionWork shoppingItem = (MissingPart)newPart.getMissingPart();
        if(null == shoppingItem && newPart instanceof IAcquisitionWork) {
            shoppingItem = (IAcquisitionWork)newPart;
        }         
        if (null == shoppingItem) {
            //somethings wrong
            MekHQ.logMessage("found a null shopping item when trying to add " + newPart.getName());
            return;
        }
        while(quantity > 0 && newPart.getCampaign().acquirePart(shoppingItem, person)) {
            quantity--;
        }
        if(quantity > 0) {
            newPart.getCampaign().addReport(newPart.getShoppingListReport(quantity));
            while(quantity > 1) {
                shoppingItem.incrementQuantity();
                quantity--;
            }
            shoppingList.add(shoppingItem);
        }
    }
    
    public void newDay(Campaign campaign) {
        Person person = campaign.getLogisticsPerson();
        if(null == person && !campaign.getCampaignOptions().getAcquisitionSkill().equals(CampaignOptions.S_AUTO)) {
            campaign.addReport("Your force has no one capable of acquiring parts.");
            return;
        }
        ArrayList<IAcquisitionWork> newShoppingList = new ArrayList<IAcquisitionWork>();
        for(IAcquisitionWork shoppingItem : shoppingList) {
            shoppingItem.decrementDaysToWait();
            if(shoppingItem.getDaysToWait() <= 0) {
                while(shoppingItem.getQuantity() > 0 && campaign.acquirePart(shoppingItem, person)) {
                    shoppingItem.decrementQuantity();
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
    
    public ArrayList<IAcquisitionWork> getList() {
        return shoppingList;
    }
    
}