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

import megamek.common.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.UnitOrder;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.time.LocalDate;

public class Lease {
    private static final MMLogger LOGGER = MMLogger.create(UnitOrder.class);

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
        if (time.getDayOfMonth() == 1) {
            if (getLeaseStart().isBefore(time)) {
                if (isLeaseFirstMonth(time.minusDays(1))) {
                    return getFirstLeaseCost(time);
                }
                return leaseCost;
            }
            return Money.zero();
        } else {
            LOGGER.error("getLeaseCostNow(time) cannot be called on other days of the month then the 1st.");
            return null;
        }
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
    private boolean isLeaseFirstMonth(LocalDate today) {
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
        int startDay = 1;
        int currentDay = today.getDayOfMonth();
        if (isLeaseFirstMonth(today)) {
            startDay = acquisitionDate.getDayOfMonth();
        }
        int daysElapsed = currentDay - startDay + 1;
        float fractionOfMonth = (float) daysElapsed / today.lengthOfMonth();
        return leaseCost.multipliedBy(fractionOfMonth);
    }

    /**
     * Gets the cost of the lease, prorated in the first month. Assumes that it's only called on the first day of the
     * month, so we need to find yesterday for the last.
     *
     * @return Money Prorated first payment of lease
     */
    public Money getFirstLeaseCost(LocalDate today) {
        if (today.getDayOfMonth() == 1) {
            int startDay = acquisitionDate.getDayOfMonth();
            LocalDate yesterday = today.minusDays(1);
            int daysInMonth = yesterday.lengthOfMonth();
            int daysElapsed = yesterday.getDayOfMonth() - startDay + 1;
            float fractionOfMonth = (float) daysElapsed / daysInMonth;
            return leaseCost.multipliedBy(fractionOfMonth);
        } else {
            LOGGER.error("getFirstLeaseCost(today) cannot be called on other days of the month than the 1st.");
            return null;
        }
    }

    /**
     * This function checks to see if the Entity is of a leasable type. This is currently hardcoded to restrict it to
     * Dropships and Jumpships only.
     *
     * @return True if unit is leasable
     */
    public static boolean isLeasable(Entity check) {
        return check.isDropShip() || check.isJumpShip();
    }

    public void writeToXML(final PrintWriter writer, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "lease");
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "leaseCost", leaseCost);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "acquisitionDate", acquisitionDate);
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "lease");
    }

    public static Lease generateInstanceFromXML(Node writerNode, Unit parseUnit) {
        Lease savedLease = new Lease();
        NodeList childNodeList = writerNode.getChildNodes();

        try {
            for (int x = 0; x < childNodeList.getLength(); x++) {
                Node childNode = childNodeList.item(x);
                String nodeName = childNode.getNodeName();
                String nodeText = childNode.getTextContent();

                if (nodeName.equalsIgnoreCase("leaseCost")) {
                    savedLease.leaseCost = Money.fromXmlString(nodeText);
                } else if (nodeName.equalsIgnoreCase("acquisitionDate")) {
                    savedLease.acquisitionDate = LocalDate.parse(nodeText);
                }
            }
            return savedLease;
        } catch (Exception ex) {
            LOGGER.error("Could not parse lease for unit {}", parseUnit.getId(), ex);
        }
        return null;
    }
}

