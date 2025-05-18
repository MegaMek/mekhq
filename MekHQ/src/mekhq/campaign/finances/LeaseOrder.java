/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package mekhq.campaign.finances;

import java.io.PrintWriter;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LeaseOrder extends mekhq.campaign.unit.UnitOrder {
    private static final MMLogger LOGGER = MMLogger.create(LeaseOrder.class);

    /*
     * LeaseOrders is in the shopping list, it doesn't have a unit yet to attach to.
     */
    public LeaseOrder(megamek.common.Entity entity, mekhq.campaign.Campaign campaign) {
        super(entity, campaign);
        this.entity = entity;
    }

    // For the XML reader.
    private LeaseOrder() {
    }

    @Override
    public void decrementDaysToWait() {
        super.decrementDaysToWait();
    }

    @Override
    public int getDaysToWait() {
        return super.getDaysToWait();
    }

    @Override
    public String find(int transitDays) {
        super.getCampaign().getQuartermaster().leaseUnit((megamek.common.Entity) getNewEquipment(), transitDays);
        return "<font color='" +
                     mekhq.MekHQ.getMHQOptions().getFontColorPositiveHexColor() +
                     "'><b> unit found for leasing</b>.</font> It will be delivered in " +
                     transitDays +
                     " days.";
    }

    /**
     * Displays the correct name for the Procurement List, so they're not confused with standard orders.
     *
     * @return {String} Lease for UnitName
     */
    @Override
    public String getAcquisitionName() {
        return "Lease for " + getName();
    }

    /**
     * Leases don't actually COST anything when a unit is obtained, so this returns zero.
     */
    @Override
    public Money getTotalBuyCost() {
        return Money.zero();
    }

    /**
     * Leases don't actually COST anything when a unit is obtained, so this returns zero.
     */
    @Override
    public Money getBuyCost() {
        return Money.zero();
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "leaseOrder");
        pw.println(MHQXMLUtility.writeEntityToXmlString(getEntity(), indent, getCampaign().getEntities()));
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "quantity", quantity);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "daysToWait", daysToWait);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "leaseOrder");
    }

    public static LeaseOrder generateInstanceFromXML(Node wn, Campaign c) {
        LeaseOrder retVal = new LeaseOrder();
        retVal.setCampaign(c);

        NodeList nl = wn.getChildNodes();

        try {
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("quantity")) {
                    retVal.quantity = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("daysToWait")) {
                    retVal.daysToWait = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("entity")) {
                    retVal.entity = MHQXMLUtility.parseSingleEntityMul((Element) wn2, c);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("", ex);
        }

        retVal.initializeParts(false);

        return retVal;
    }
}

