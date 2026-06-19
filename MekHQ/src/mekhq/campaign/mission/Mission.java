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
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import org.w3c.dom.Node;

/**
 * Missions are primarily holder objects for a set of scenarios.
 * <p>
 * The really cool stuff will happen when we subclass this into Contract
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Mission extends AbstractMission {
    private static final MMLogger LOGGER = MMLogger.create(Mission.class);

    // region Constructors
    public Mission() {
        this(null);
    }

    public Mission(final @Nullable String name) {
        setName(name);
        setSystemId("Unknown System");
    }
    // endregion Constructors

    @Override
    protected int writeToXMLBegin(Campaign campaign, final PrintWriter printWriter, int indent) {
        // All Mission-level fields are owned by AbstractMission.
        return super.writeToXMLBegin(campaign, printWriter, indent);
    }

    @Override
    public void loadFieldsFromXmlNode(Campaign campaign, Version version, Node node) throws ParseException {
        // All Mission-level fields are parsed by AbstractMission.
        super.loadFieldsFromXmlNode(campaign, version, node);
    }

    /**
     * @deprecated Call {@link AbstractMission#generateInstanceFromXML} directly. This delegate exists only so that
     *       existing call sites in the campaign loader do not need to be updated immediately.
     */
    @Deprecated
    public static Mission generateInstanceFromXML(Node node, Campaign campaign, Version version) {
        return (Mission) AbstractMission.generateInstanceFromXML(node, campaign, version);
    }

    @Override
    public String toString() {
        return getStatus().isCompleted() ? getName() + " (Complete)" : getName();
    }
}
