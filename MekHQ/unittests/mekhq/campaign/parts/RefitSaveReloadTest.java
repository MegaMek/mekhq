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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import megamek.Version;
import megamek.common.icons.Camouflage;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import testUtilities.parts.PartsCensus;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A refit survives a save and reload with everything it set aside, and a refit or refurbishment leaves the unit's
 * camouflage alone (issue #10200).
 */
class RefitSaveReloadTest {
    private static final String CAMOUFLAGE_CATEGORY = "Standard Camouflage";
    private static final String CAMOUFLAGE_FILE = "Northwind Highlanders.jpg";

    private PartsScenario scenario;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
    }

    @Test
    void aNewAmmoBinSurvivesAReload() throws Exception {
        // The WVR-6R adds an AC/5 and its ammo bin to the WVR-6M
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6M);
        Refit refit = new Refit(wolverine, UnitFixture.WOLVERINE_WVR_6R.loadEntity(), false, false, false);
        refit.begin();
        refit.find(0, 1.0);
        boolean hasNewAmmoBin = refit.getNewUnitParts().stream().anyMatch(part -> part instanceof AmmoBin);
        assertTrue(hasNewAmmoBin, "The refit set aside a new ammo bin");

        Refit reloaded = writeAndReadBack(refit, wolverine);

        assertEquals(PartsCensus.ofParts(refit.getNewUnitParts()), PartsCensus.ofParts(reloaded.getNewUnitParts()));
    }

    @Test
    void theHeatSinksFreedFromTheEngineSurviveAReload() throws Exception {
        // The LCT-5V keeps the 160 engine but swaps its six built-in single heat sinks for doubles
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        Refit refit = new Refit(locust, UnitFixture.LOCUST_LCT_5V.loadEntity(), false, false, false);
        refit.begin();
        assertFalse(refit.getOldIntegratedHeatSinks().isEmpty(), "The refit frees heat sinks from the engine");

        Refit reloaded = writeAndReadBack(refit, locust);

        assertEquals(PartsCensus.ofParts(refit.getOldIntegratedHeatSinks()),
              PartsCensus.ofParts(reloaded.getOldIntegratedHeatSinks()));
    }

    @Test
    void aCompletedRefitKeepsTheUnitsCamouflage() throws Exception {
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        wolverine.getEntity().setCamouflage(new Camouflage(CAMOUFLAGE_CATEGORY, CAMOUFLAGE_FILE));
        Refit refit = new Refit(wolverine, UnitFixture.WOLVERINE_WVR_6M.loadEntity(), false, false, false);
        refit.begin();
        refit.find(0, 1.0);
        assertTrue(refit.acquireParts());

        refit.succeed();

        assertEquals("WVR-6M", wolverine.getEntity().getModel());
        assertCamouflageKept(wolverine);
    }

    @Test
    void aCancelledRefurbishmentKeepsTheUnitsCamouflage() throws Exception {
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        wolverine.getEntity().setCamouflage(new Camouflage(CAMOUFLAGE_CATEGORY, CAMOUFLAGE_FILE));

        Refit refurbishment = new Refit(wolverine, wolverine.getEntity(), false, true, false);

        assertCamouflageKept(wolverine);
        refurbishment.cancel();
        assertCamouflageKept(wolverine);
    }

    private static void assertCamouflageKept(Unit unit) {
        Camouflage camouflage = unit.getEntity().getCamouflage();
        assertEquals(CAMOUFLAGE_CATEGORY, camouflage.getCategory());
        assertEquals(CAMOUFLAGE_FILE, camouflage.getFilename());
    }

    /**
     * Writes the refit out and reads it back the way a campaign save does, then resolves its part references against
     * the warehouse. The target design itself is skipped on reading because it needs the unit data cache.
     */
    private Refit writeAndReadBack(Refit refit, Unit unit) throws Exception {
        StringWriter xmlText = new StringWriter();
        try (PrintWriter xmlWriter = new PrintWriter(xmlText)) {
            refit.writeToXML(xmlWriter, 0);
        }
        Document document = MHQXMLUtility.newSafeDocumentBuilder()
                                  .parse(new ByteArrayInputStream(xmlText.toString()
                                                                        .getBytes(StandardCharsets.UTF_8)));
        Refit reloaded = Refit.generateInstanceFromXML(document.getDocumentElement(), new Version(), campaign, unit,
              true);
        assertNotNull(reloaded);
        reloaded.fixReferences(campaign);
        return reloaded;
    }
}
