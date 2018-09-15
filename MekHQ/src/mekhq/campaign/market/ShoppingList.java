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

package mekhq.campaign.market;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Entity;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.ProcurementEvent;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.UnitOrder;
import mekhq.campaign.work.IAcquisitionWork;

/**
 *  A list of IAcquisitionWork
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

    private List<IAcquisitionWork> shoppingList;

    public ShoppingList() {
        shoppingList = new ArrayList<IAcquisitionWork>();
    }
    
    public ShoppingList(List<IAcquisitionWork> items) {
        shoppingList = items;
    }
    
    public ShoppingList(IAcquisitionWork item) {
        shoppingList = new ArrayList<IAcquisitionWork>();   
        shoppingList.add(item);
    }

    public List<IAcquisitionWork> getAllShoppingItems() {
    	return shoppingList;
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

    public void addShoppingItemWithoutChecking(IAcquisitionWork newWork) {
        shoppingList.add(newWork);
    }

    public void addShoppingItem(IAcquisitionWork newWork, int quantity, Campaign campaign) {
        //ammo bins need a little extra work here
        if(newWork instanceof AmmoBin) {
            newWork = ((AmmoBin) newWork).getAcquisitionWork();
        }
        
        //check to see if this is already on the shopping list. If so, then add quantity to the list
        //and return
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
        
        //if not on the shopping list then try to acquire it with a temporary short shopping list. 
        //If we fail, then add it to the shopping list
        int origQuantity = quantity;
        while(quantity > 1) {
            newWork.incrementQuantity();
            quantity--;
        }
        ShoppingList shortList = new ShoppingList(newWork);
        shortList = campaign.goShopping(shortList);
        
        if(newWork.getQuantity() > 0) {
            //if using planetary acquisition check with low verbosity, check to see if nothing was found 
            //because it is not reported elsewhere
            if(newWork.getQuantity() == origQuantity && 
                    campaign.getCampaignOptions().usesPlanetaryAcquisition() &&
                    !campaign.getCampaignOptions().usePlanetAcquisitionVerboseReporting()) {
                campaign.addReport("<font color='red'><b>You failed to find " + newWork.getAcquisitionName() + " within " + campaign.getCampaignOptions().getMaxJumpsPlanetaryAcquisition() + " jumps</b></font>");
            }

            campaign.addReport(newWork.getShoppingListReport(newWork.getQuantity()));
            
            shoppingList.add(newWork);
            MekHQ.triggerEvent(new ProcurementEvent(newWork));
        }
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<shoppingList>");
        for(IAcquisitionWork shoppingItem : shoppingList) {
            //don't write refits to the shopping list - we will add them manually
            //when we parse units and find refit kits that have not been found
            if(shoppingItem instanceof Part && !(shoppingItem instanceof Refit)) {
                ((Part)shoppingItem).writeToXml(pw1, indent+1);
            }
            else if(shoppingItem instanceof UnitOrder) {
                ((UnitOrder)shoppingItem).writeToXml(pw1, indent+1);
            }
        }
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</shoppingList>");
    }

    public static ShoppingList generateInstanceFromXML(Node wn, Campaign c, Version version) {
        final String METHOD_NAME = "generateInstanceFromXML(Node,Campaign,Version)"; //$NON-NLS-1$

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
                    UnitOrder u = UnitOrder.generateInstanceFromXML(wn2, c, version);
                    u.campaign = c;
                    if(null != u.getEntity()) {
                        retVal.shoppingList.add(u);
                    }
                }
            }
        } catch (Exception ex) {
            // Doh!
            MekHQ.getLogger().error(ShoppingList.class, METHOD_NAME, ex);
        }

        return retVal;
    }

    public void restore() {
    	ArrayList<IAcquisitionWork> newShoppingList = new ArrayList<IAcquisitionWork>();;
    	

        for(IAcquisitionWork shoppingItem : shoppingList) {
            if(shoppingItem instanceof MissingEquipmentPart) {
                ((MissingEquipmentPart)shoppingItem).restore();
                if(null != ((MissingEquipmentPart)shoppingItem).getType()) {
                	newShoppingList.add(shoppingItem);
                }
            } else {
            	newShoppingList.add(shoppingItem);
            }
        }
        shoppingList = newShoppingList;
    }

    public void removeZeroQuantityFromList() {
    	ArrayList<IAcquisitionWork> newShoppingList = new ArrayList<IAcquisitionWork>();
        for(IAcquisitionWork shoppingItem : shoppingList) {
            if(shoppingItem.getQuantity() > 0) {
                newShoppingList.add(shoppingItem);
            }
        }
        shoppingList = newShoppingList;
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