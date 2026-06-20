/*
 * Copyright (c) 2011 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission;

import java.io.PrintWriter;
import java.text.ParseException;

import megamek.Version;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.enums.ContractCommandRights;
import org.w3c.dom.Node;

/**
 * Contracts - we need to track static amounts here because changes in the underlying campaign don't change the figures
 * once the ink is dry
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Contract extends AbstractMissionTransition {
    private static final MMLogger logger = MMLogger.create(Contract.class);

    public Contract() {
        this(null, null);
    }

    public Contract(String name, String employer) {
        super();
        setName(name);
        setEmployerName(employer);

        setLengthInMonths(12);
        setPaymentMultiplier(2.0);
        setCommandRights(ContractCommandRights.HOUSE);
        setStraightSupport(50);
        setBattleLossCompensation(50);
        setSalvagePercent(50);
        setTransportCompensation(50);
        setAdvancePercent(25);
    }

    @Override
    public void setSystemId(String n) {
        super.setSystemId(n);
        setCachedJumpPath(null);
    }

    @Override
    protected int writeToXMLBegin(Campaign campaign, final PrintWriter printWriter, int indent) {
        // All Contract-level fields are owned by AbstractMission.
        return super.writeToXMLBegin(campaign, printWriter, indent);
    }

    @Override
    public void loadFieldsFromXmlNode(Campaign campaign, Version version, Node node) throws ParseException {
        // All Contract-level fields are parsed by AbstractMission.
        super.loadFieldsFromXmlNode(campaign, version, node);

        // Version compatibility fix: Prior to 0.50.12, transportAmount stored the player's out-of-pocket
        // transport cost. Now it stores the employer's transport reimbursement. Recalculate for old saves.
        if (version.isLowerThan(new Version("0.50.12"))) {
            if ((null != getSystem()) && campaign.getCampaignOptions().isPayForTransport()) {
                setTransportAmount(getEmployerTransportReimbursement(campaign));
            } else {
                setTransportAmount(Money.zero());
            }
        }
    }
}
