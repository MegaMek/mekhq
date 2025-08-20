/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import megamek.Version;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.ProcurementEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.UnitOrder;
import mekhq.campaign.work.IAcquisitionWork;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A list of IAcquisitionWork
 * <p>
 * When a new acquisition is requested (via the parts store or the acquisition tab), we iterate through this list and
 * look for the MissingPart.getNewPart that matches the desired part. Here are the possible outcomes:
 * <p>
 * - We find it, but we cannot check today, so we add the quantity requested - We don't find it, we immediately check
 * and add to the list if we fail
 * <p>
 * On Campaign.newDay, we also cycle through the list and check any items that have no more days to wait for the next
 * check.
 * <p>
 * Checking procedure Using a while loop, we keep checking using an acquisition roll until we fail or we hit zero
 * quantity. If we hit zero quantity, then we can remove the item. If we fail, then we reset the dayCounter to the max.
 * <p>
 * We also now only use one person to make all checks. We allow the user to set the skill and other options for who
 * makes the check in the campaign options.,
 * <p>
 * Do we use a separate shopping list for new units?
 */
public class ShoppingList {
    private static final MMLogger logger = MMLogger.create(ShoppingList.class);

    // region Variable Declarations
    private List<IAcquisitionWork> shoppingList;
    // endregion Variable Declarations

    // region Constructors
    public ShoppingList() {
        setShoppingList(new ArrayList<>());
    }

    public ShoppingList(List<IAcquisitionWork> shoppingList) {
        setShoppingList(shoppingList);
    }
    // endregion Constructors

    // region Getters/Setters
    public List<IAcquisitionWork> getShoppingList() {
        return shoppingList;
    }

    public void setShoppingList(List<IAcquisitionWork> shoppingList) {
        this.shoppingList = shoppingList;
    }
    // endregion Getters/Setters

    public @Nullable IAcquisitionWork getShoppingItem(final Object newEquipment) {
        return getShoppingList().stream()
                     .filter(shoppingItem -> isSameEquipment(shoppingItem.getNewEquipment(), newEquipment))
                     .findFirst()
                     .orElse(null);
    }

    public void removeItem(Object equipment) {
        int idx = -1;
        int row = 0;
        for (IAcquisitionWork shoppingItem : getShoppingList()) {
            if (isSameEquipment(shoppingItem.getNewEquipment(), equipment)) {
                idx = row;
                break;
            }
            row++;
        }
        if (idx > -1) {
            getShoppingList().remove(idx);
        }
    }

    public void addShoppingItemWithoutChecking(IAcquisitionWork newWork) {
        getShoppingList().add(newWork);
    }

    public void addShoppingItem(IAcquisitionWork newWork, int quantity, Campaign campaign) {
        // check to see if this is already on the shopping list. If so, then add
        // quantity to the list
        // and return
        for (IAcquisitionWork shoppingItem : getShoppingList()) {
            if (isSameEquipment(shoppingItem.getNewEquipment(), newWork.getNewEquipment())) {
                campaign.addReport(newWork.getShoppingListReport(quantity));
                while (quantity > 0) {
                    shoppingItem.incrementQuantity();
                    quantity--;
                }
                MekHQ.triggerEvent(new ProcurementEvent(newWork));
                return;
            }
        }

        // if not on the shopping list then try to acquire it with a temporary short
        // shopping list.
        // If we fail, then add it to the shopping list
        int origQuantity = quantity;
        while (quantity > 1) {
            newWork.incrementQuantity();
            quantity--;
        }

        if (newWork.getQuantity() > 0) {
            // if using planetary acquisition check with low verbosity, check to see if
            // nothing was found
            // because it is not reported elsewhere
            if ((newWork.getQuantity() == origQuantity) &&
                      campaign.getCampaignOptions().isUsePlanetaryAcquisition() &&
                      !campaign.getCampaignOptions().isPlanetAcquisitionVerbose()) {
                campaign.addReport("<font color='" +
                                         ReportingUtilities.getNegativeColor() +
                                         "'><b>You failed to find " +
                                         newWork.getAcquisitionName() +
                                         " within " +
                                         campaign.getCampaignOptions().getMaxJumpsPlanetaryAcquisition() +
                                         " jumps</b></font>");
            }

            campaign.addReport(newWork.getShoppingListReport(newWork.getQuantity()));

            getShoppingList().add(newWork);
            MekHQ.triggerEvent(new ProcurementEvent(newWork));
        }
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        if (getShoppingList().isEmpty()) {
            return;
        }

        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "shoppingList");
        for (IAcquisitionWork shoppingItem : getShoppingList()) {
            // don't write refits to the shopping list - we will add them manually
            // when we parse units and find refit kits that have not been found
            if ((shoppingItem instanceof Part) && !(shoppingItem instanceof Refit)) {
                ((Part) shoppingItem).writeToXML(pw, indent);
            } else if (shoppingItem instanceof UnitOrder) {
                ((UnitOrder) shoppingItem).writeToXML(pw, indent);
            }
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "shoppingList");
    }

    public static ShoppingList generateInstanceFromXML(Node wn, Campaign c, Version version) {
        ShoppingList retVal = new ShoppingList();

        NodeList nl = wn.getChildNodes();

        try {
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("part")) {
                    Part p = Part.generateInstanceFromXML(wn2, version);
                    p.setCampaign(c);
                    if (p instanceof IAcquisitionWork) {
                        retVal.getShoppingList().add((IAcquisitionWork) p);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("unitOrder")) {
                    UnitOrder u = UnitOrder.generateInstanceFromXML(wn2, c);
                    u.setCampaign(c);
                    if (u.getEntity() != null) {
                        retVal.getShoppingList().add(u);
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("", ex);
        }

        return retVal;
    }

    public void restore() {
        List<IAcquisitionWork> newShoppingList = new ArrayList<>();

        for (IAcquisitionWork shoppingItem : getShoppingList()) {
            if (shoppingItem instanceof MissingEquipmentPart) {
                ((MissingEquipmentPart) shoppingItem).restore();
                if (((MissingEquipmentPart) shoppingItem).getType() != null) {
                    newShoppingList.add(shoppingItem);
                }
            } else {
                newShoppingList.add(shoppingItem);
            }
        }
        setShoppingList(newShoppingList);
    }

    public void removeZeroQuantityFromList() {
        List<IAcquisitionWork> newShoppingList = new ArrayList<>();
        for (IAcquisitionWork shoppingItem : getShoppingList()) {
            if (shoppingItem.getQuantity() > 0) {
                newShoppingList.add(shoppingItem);
            }
        }
        setShoppingList(newShoppingList);
    }

    public Money getTotalBuyCost() {
        Money totalBuyCost = Money.zero();
        for (IAcquisitionWork shoppingItem : getShoppingList()) {
            totalBuyCost = totalBuyCost.plus(shoppingItem.getTotalBuyCost());
        }
        return totalBuyCost;
    }

    public List<IAcquisitionWork> getPartList() {
        List<IAcquisitionWork> partList = new ArrayList<>();
        for (IAcquisitionWork shoppingItem : getShoppingList()) {
            if (shoppingItem instanceof Part) {
                partList.add(shoppingItem);
            }
        }
        return partList;
    }

    public List<IAcquisitionWork> getUnitList() {
        List<IAcquisitionWork> unitList = new ArrayList<>();
        for (IAcquisitionWork shoppingItem : getShoppingList()) {
            if (shoppingItem instanceof UnitOrder) {
                unitList.add(shoppingItem);
            }
        }
        return unitList;
    }

    private boolean isSameEquipment(Object equipment, Object newEquipment) {
        if ((newEquipment instanceof Part) && (equipment instanceof Part)) {
            return ((Part) equipment).isSamePartType((Part) newEquipment);
        } else if ((newEquipment instanceof Entity) && (equipment instanceof Entity)) {
            Entity entityA = (Entity) newEquipment;
            Entity entityB = (Entity) equipment;
            return entityA.getChassis().equals(entityB.getChassis()) && entityA.getModel().equals(entityB.getModel());
        }
        return false;
    }
}
