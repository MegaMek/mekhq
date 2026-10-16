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

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.DropshipDockingCollar;
import mekhq.campaign.parts.GravDeck;
import mekhq.campaign.parts.JumpshipDockingCollar;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.kfs.KFChargingSystem;
import mekhq.campaign.parts.kfs.KFDriveCoil;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Element;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Damage to a ship's drive, grav decks and docking collars is still there after a save and load. Before, the save
 * left it out, so loading the campaign repaired it for free (MegaMek issue #9226).
 */
class LargeCraftDamageSaveLoadTest {
    private PartsScenario scenario;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
    }

    /** Saves the unit and loads it back the way a campaign load does: parts rebuilt and read from the unit. */
    private Unit saveAndLoad(Unit unit) throws Exception {
        StringWriter unitXml = new StringWriter();
        PrintWriter printWriter = new PrintWriter(unitXml);
        unit.writeToXML(printWriter, 0);
        printWriter.flush();
        Element unitElement = MHQXMLUtility.newSafeDocumentBuilder()
                                    .parse(new ByteArrayInputStream(
                                          unitXml.toString().getBytes(StandardCharsets.UTF_8)))
                                    .getDocumentElement();
        Unit loadedUnit = Unit.generateInstanceFromXML(unitElement, new Version(), campaign);
        loadedUnit.setCampaign(campaign);
        loadedUnit.initializeParts(false);
        loadedUnit.runDiagnostic(false);
        return loadedUnit;
    }

    private static void damage(Unit unit, Class<? extends Part> partType) {
        Part part = PartsScenario.unitParts(unit, partType).getFirst();
        part.setHits(1);
        part.updateConditionFromPart();
        assertTrue(part.needsFixing(), partType.getSimpleName() + " starts damaged");
    }

    private static boolean isDamaged(Unit unit, Class<? extends Part> partType) {
        return PartsScenario.unitParts(unit, partType).getFirst().needsFixing();
    }

    @ParameterizedTest
    @ValueSource(classes = { KFDriveCoil.class, KFChargingSystem.class, GravDeck.class,
                             JumpshipDockingCollar.class })
    void jumpshipDamageSurvivesASaveAndLoad(Class<? extends Part> partType) throws Exception {
        Unit invader = scenario.withUnit(UnitFixture.INVADER_JUMPSHIP);
        damage(invader, partType);

        Unit loaded = saveAndLoad(invader);

        assertTrue(isDamaged(loaded, partType), partType.getSimpleName() + " is still damaged after loading");
    }

    @ParameterizedTest
    @ValueSource(classes = { DropshipDockingCollar.class })
    void dropshipCollarDamageSurvivesASaveAndLoad(Class<? extends Part> partType) throws Exception {
        Unit leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        damage(leopard, partType);

        Unit loaded = saveAndLoad(leopard);

        assertTrue(isDamaged(loaded, partType), "The docking collar is still damaged after loading");
    }
}
