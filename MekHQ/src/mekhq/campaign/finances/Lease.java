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
 */
package mekhq.campaign.finances;

import java.io.PrintWriter;
import java.time.LocalDate;

import megamek.common.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitOrder;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Lease {
    private static final MMLogger logger = MMLogger.create(UnitOrder.class);

    private Money leaseCost;
    private LocalDate acquisitionDate;

    /**
     * Leases are nominally attached to units while they are in the hanger.
     */
    public Lease(LocalDate currentDay, Unit unit) {
        acquisitionDate = currentDay;
        // Campaign Operations 4th., p43
        leaseCost = unit.getSellValue().multipliedBy(0.005);
    }

    /**
     * This constructor is only used for reloading leases from the XML.
     */
    private Lease() {
    }

    /**
     * Gets the lease cost, for the accountant. Lease cost is prorated for the first month, so we need to check if
     * yesterday was the first month. Should only be called on the 1st. It's also possible we get a lease object before
     * the lease actually starts, if it takes a while to deliver the unit. Cost will be zero in this case.
     *
     * @param time The current campaign LocalDate
     */
    public Money getLeaseCostNow(LocalDate time) {
        if (getLeaseStart().isBefore(time)) {
            if (isLeaseFirstMonth(time.minusDays(1))) {
                return getFirstLeaseCost(time);
            }
            return leaseCost;
        }
        return Money.zero();
    }

    /**
     * This is the total monthly cost for the entire lease.
     */
    public Money getLeaseCost() {
        return leaseCost;
    }

    /**
     * Leases can start at any time of the month, but they are only processed on the 1st by the accountant.
     */
    public LocalDate getLeaseStart() {
        return acquisitionDate;
    }

    /**
     * Utility function. Is this the first month of the lease?
     *
     * @param today The LocalDate to check with. No corrections done.
     */
    public boolean isLeaseFirstMonth(LocalDate today) {
        return (today.getYear() == acquisitionDate.getYear() && today.getMonth() == acquisitionDate.getMonth());
    }

    /**
     * Gets the final cost of the lease remaining in the last month for use when ending a lease. If you call this in the
     * same month you acquired the unit, only the days between lease start and now are counted. Can be called on any day
     * of the month
     *
     * @return Money Prorated last payment of lease
     */
    public Money getFinalLeaseCost(LocalDate today) {
        int startDay = 0;
        int currentDay = today.getDayOfMonth();
        if (isLeaseFirstMonth(today)) {
            startDay = acquisitionDate.getDayOfMonth();
        }
        float fractionOfMonth = (float) (currentDay - startDay) / (float) today.lengthOfMonth();
        return leaseCost.multipliedBy(fractionOfMonth);
    }

    /**
     * Gets the cost of the lease, prorated in the first month. Assumes that it's only called on the first day of the
     * month, so we need to find yesterday for the last.
     *
     * @return Money Prorated first payment of lease
     */
    public Money getFirstLeaseCost(LocalDate today) {
        int startDay = acquisitionDate.getDayOfMonth();
        int yesterday = today.minusDays(1).getDayOfMonth();
        float fractionOfMonth = (float) (yesterday - startDay) + 1 / (float) yesterday;
        return leaseCost.multipliedBy(fractionOfMonth);
    }

    /**
     * This function checks to see if the Entity is of a leasable type. This is currently hardcoded to restrict it to
     * Dropships and Jumpships only.
     *
     * @return True if unit is leasable
     */
    public static boolean isLeasable(Entity check) {
        return check instanceof megamek.common.Dropship || check instanceof megamek.common.Jumpship;
    }

    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "lease");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "leaseCost", leaseCost);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "acquisitionDate", acquisitionDate);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "lease");
    }

    public static Lease generateInstanceFromXML(Node wn, Unit u) {
        Lease retVal = new Lease();
        NodeList nl = wn.getChildNodes();

        try {
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("leaseCost")) {
                    retVal.leaseCost = Money.fromXmlString(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("acquisitionDate")) {
                    retVal.acquisitionDate = LocalDate.parse(wn2.getTextContent());
                }
            }
            return retVal;
        } catch (Exception ex) {
            logger.error("Could not parse lease for unit {}", u.getId(), ex);
        }
        return null;
    }
}

