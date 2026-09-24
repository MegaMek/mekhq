/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.universe.commandGeneration.ratgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.client.ratgenerator.ForceDescriptor;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

/**
 * Covers what the snapshot copies from the Force Generator panel's descriptor on Accept.
 */
class ForceDescriptorSnapshotTest {

    @Test
    void populateFromForceDescriptorCopiesTheTransportPercentages() {
        ForceDescriptor fd = new ForceDescriptor();
        fd.setDropshipPct(1.0);
        fd.setJumpshipPct(0.5);
        fd.setCargoPct(0.25);
        ForceDescriptorSnapshot snapshot = new ForceDescriptorSnapshot();

        snapshot.populateFromForceDescriptor(fd);

        assertEquals(1.0, snapshot.getDropshipPct());
        assertEquals(0.5, snapshot.getJumpshipPct(), "the JumpShip share the panel asks for reaches the build");
        assertEquals(0.25, snapshot.getCargoPct());
    }

    /**
     * A preset holding a setting that is not a number loses that one setting, not the preset.
     */
    @Test
    void anUnreadableNumberKeepsItsDefaultAndTheRestStillLoads() throws Exception {
        String xml = "<forceDescriptorSnapshot>"
              + "<faction>FS</faction>"
              + "<year>not-a-year</year>"
              + "<echelon>seven</echelon>"
              + "<dropshipPct>lots</dropshipPct>"
              + "<rating>A</rating>"
              + "</forceDescriptorSnapshot>";
        Element element = DocumentBuilderFactory.newInstance()
              .newDocumentBuilder()
              .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
              .getDocumentElement();
        ForceDescriptorSnapshot defaults = new ForceDescriptorSnapshot();

        ForceDescriptorSnapshot snapshot = ForceDescriptorSnapshot.parseFromXML(element);

        assertEquals(defaults.getYear(), snapshot.getYear(), "an unreadable year keeps the default");
        assertNull(snapshot.getEchelon(), "an unreadable echelon stays unset");
        assertEquals(defaults.getDropshipPct(), snapshot.getDropshipPct());
        assertEquals("FS", snapshot.getFaction(), "the settings around the bad ones still load");
        assertEquals("A", snapshot.getRating());
    }
}
