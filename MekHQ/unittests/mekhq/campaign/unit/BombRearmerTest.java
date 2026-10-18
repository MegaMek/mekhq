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
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.unit;

import static mekhq.campaign.enums.DailyReportType.TECHNICAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.BombLoadout;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.enums.BombType.BombTypeEnum;
import megamek.common.units.IBomber;
import mekhq.campaign.Campaign;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * An aircraft whose player ticked "Rearm with these bombs after each scenario" is topped back up to that loadout from
 * the warehouse after a scenario (issue #6200). A Shilone SL-17 carries the bombs on its external hardpoints here.
 */
class BombRearmerTest {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.BombRearmer";
    private static final int PREFERRED_HIGH_EXPLOSIVE_BOMBS = 4;
    private static final int BOMBS_LEFT_AFTER_THE_SCENARIO = 1;

    private final PartsScenario scenario = PartsScenario.create();
    private final Campaign campaign = scenario.getCampaign();
    private Unit shilone;
    private IBomber bomber;
    private AmmoType highExplosiveBomb;

    @BeforeEach
    void setUp() {
        shilone = scenario.withUnit(UnitFixture.SHILONE_SL_17);
        bomber = (IBomber) shilone.getEntity();
        highExplosiveBomb = (AmmoType) EquipmentType.get(BombTypeEnum.HE.getInternalName());
        bomber.setExtBombChoices(highExplosiveBombs(BOMBS_LEFT_AFTER_THE_SCENARIO));
    }

    private static BombLoadout highExplosiveBombs(int count) {
        BombLoadout bombs = new BombLoadout();
        bombs.addBombs(BombTypeEnum.HE, count);
        return bombs;
    }

    private void preferHighExplosiveBombs(int count) {
        shilone.setPreferredBombLoadout(new PreferredBombLoadout(new BombLoadout(), highExplosiveBombs(count)));
    }

    private void stockHighExplosiveBombs(int count) {
        campaign.getQuartermaster().addAmmo(highExplosiveBomb, count);
    }

    private int highExplosiveBombsCarried() {
        return bomber.getExtBombChoices().getCount(BombTypeEnum.HE);
    }

    private boolean isReported(String report) {
        return campaign.getDailyReportLog().getLines(TECHNICAL).contains(report);
    }

    @Test
    void theBombsUsedAreReplacedFromTheWarehouse() {
        preferHighExplosiveBombs(PREFERRED_HIGH_EXPLOSIVE_BOMBS);
        stockHighExplosiveBombs(10);

        BombRearmer.rearmAfterScenario(campaign, shilone);

        assertEquals(PREFERRED_HIGH_EXPLOSIVE_BOMBS, highExplosiveBombsCarried());
        assertEquals(7, campaign.getQuartermaster().getAmmoAvailable(highExplosiveBomb));
        assertTrue(isReported(getFormattedTextAt(RESOURCE_BUNDLE, "BombRearmer.rearmed",
              shilone.getHyperlinkedName(), 3)));
    }

    @Test
    void theReportNamesTheBombsTheWarehouseIsShortOf() {
        preferHighExplosiveBombs(PREFERRED_HIGH_EXPLOSIVE_BOMBS);
        stockHighExplosiveBombs(1);

        BombRearmer.rearmAfterScenario(campaign, shilone);

        assertEquals(2, highExplosiveBombsCarried());
        assertEquals(0, campaign.getQuartermaster().getAmmoAvailable(highExplosiveBomb));
        String shortage = getFormattedTextAt(RESOURCE_BUNDLE, "BombRearmer.bombCount", 2,
              BombTypeEnum.HE.getDisplayName());
        assertTrue(isReported(getFormattedTextAt(RESOURCE_BUNDLE, "BombRearmer.partlyRearmed", getWarningColor(),
              shilone.getHyperlinkedName(), 1, shortage)));
    }

    @Test
    void anAircraftWithoutAPreferredLoadoutIsLeftAlone() {
        stockHighExplosiveBombs(10);

        BombRearmer.rearmAfterScenario(campaign, shilone);

        assertEquals(BOMBS_LEFT_AFTER_THE_SCENARIO, highExplosiveBombsCarried());
        assertEquals(10, campaign.getQuartermaster().getAmmoAvailable(highExplosiveBomb));
    }

    @Test
    void aLoadoutTheAircraftCanNoLongerCarryIsDropped() {
        preferHighExplosiveBombs(bomber.getMaxExtBombPoints() + 1);
        stockHighExplosiveBombs(10);

        BombRearmer.rearmAfterScenario(campaign, shilone);

        assertNull(shilone.getPreferredBombLoadout());
        assertEquals(BOMBS_LEFT_AFTER_THE_SCENARIO, highExplosiveBombsCarried());
        assertTrue(isReported(getFormattedTextAt(RESOURCE_BUNDLE, "BombRearmer.noLongerFits", getWarningColor(),
              shilone.getHyperlinkedName())));
    }

    @Test
    void thePreferredLoadoutIsSavedAndLoaded() throws Exception {
        BombLoadout internalBombs = new BombLoadout();
        internalBombs.addBombs(BombTypeEnum.CLUSTER, 1);
        PreferredBombLoadout preferredLoadout = new PreferredBombLoadout(internalBombs, highExplosiveBombs(3));

        StringWriter xml = new StringWriter();
        preferredLoadout.writeToXML(new PrintWriter(xml), 0);
        Node node = DocumentBuilderFactory.newInstance()
                          .newDocumentBuilder()
                          .parse(new ByteArrayInputStream(xml.toString().getBytes(StandardCharsets.UTF_8)))
                          .getDocumentElement();
        PreferredBombLoadout loadedLoadout = PreferredBombLoadout.generateInstanceFromXML(node);

        assertTrue(PreferredBombLoadout.isXMLTag(node));
        assertEquals(1, loadedLoadout.internalBombs().getCount(BombTypeEnum.CLUSTER));
        assertEquals(3, loadedLoadout.externalBombs().getCount(BombTypeEnum.HE));
        assertEquals(0, loadedLoadout.externalBombs().getCount(BombTypeEnum.CLUSTER));
    }
}
