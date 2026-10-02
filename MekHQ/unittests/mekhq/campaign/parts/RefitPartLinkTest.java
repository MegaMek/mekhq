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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

import megamek.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A refit's lists of parts survive a load: by identity in a new save, and in an older save even when the parts are
 * kept at a base whose part numbers repeat the main force's (issue #10343).
 *
 * <p>The refitting unit is in the main hangar while its parts are at a base, as a real campaign save showed.</p>
 */
class RefitPartLinkTest {
    private static final int ON_UNIT_NUMBER = 1;
    private static final int KIT_NUMBER = 2;
    private static final int ARMOR_NUMBER = 3;

    private Campaign campaign;
    private Unit wolverine;
    private LocalWarehouse mainWarehouse;
    private LocalWarehouse baseWarehouse;
    private Part laserTemplate;
    private Part armorTemplate;

    @BeforeEach
    void setUp() {
        PartsScenario scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        laserTemplate = PartsScenario.unitParts(wolverine, EquipmentPart.class).getFirst();
        armorTemplate = PartsScenario.unitParts(wolverine, Armor.class).getFirst();
        mainWarehouse = campaign.getPlayerForce().getWarehouse();
        for (Part part : List.copyOf(mainWarehouse.getParts())) {
            mainWarehouse.removePart(part);
        }
        PlayerBase base = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        campaign.getCampaignLocationManager().addPlayerBase(base);
        baseWarehouse = base.getBaseWarehouse();

        // Spares in the main warehouse that carry the numbers the base's parts use, and are nothing to the refit
        mainWarehouse.addPart(olderSavePart(laserTemplate, ON_UNIT_NUMBER, ""));
        mainWarehouse.addPart(olderSavePart(laserTemplate, KIT_NUMBER, ""));
        mainWarehouse.addPart(olderSavePart(armorTemplate, ARMOR_NUMBER, ""));
    }

    private Part olderSavePart(Part template, int number, String extraTags) {
        StringWriter partXml = new StringWriter();
        PrintWriter printWriter = new PrintWriter(partXml);
        template.writeToXML(printWriter, 0);
        printWriter.flush();
        String olderSave = partXml.toString()
                                 .replaceAll("<(uniqueId|unitId|refitId)>[^<]*</\\1>\\s*", "")
                                 .replaceFirst("id=\"-?\\d+\"", "id=\"" + number + "\"")
                                 .replaceFirst("<id>-?\\d+</id>", "<id>" + number + "</id>")
                                 .replace("</part>", extraTags + "</part>");
        Part part = Part.generateInstanceFromXML(parse(olderSave), new Version());
        part.setCampaign(campaign);
        return part;
    }

    private Refit loadedRefit(String refitBody) {
        Refit refit = Refit.generateInstanceFromXML(parse("<refit>" + refitBody + "</refit>"), new Version(), campaign,
              wolverine, true);
        assertNotNull(refit);
        refit.fixReferences(campaign);
        return refit;
    }

    private static Element parse(String xml) {
        try {
            return MHQXMLUtility.newSafeDocumentBuilder()
                         .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                         .getDocumentElement();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void anOlderSaveRefitFindsItsPartsAtTheBaseNotTheMainForcesSameNumbers() {
        String onUnit = "<unitId>" + wolverine.getId() + "</unitId>";
        String reserved = "<refitId>" + wolverine.getId() + "</refitId>";
        Part partOnUnit = olderSavePart(laserTemplate, ON_UNIT_NUMBER, onUnit);
        Part kitPart = olderSavePart(laserTemplate, KIT_NUMBER, reserved);
        Part armorSupplies = olderSavePart(armorTemplate, ARMOR_NUMBER, reserved);
        baseWarehouse.addPart(partOnUnit);
        baseWarehouse.addPart(kitPart);
        baseWarehouse.addPart(armorSupplies);

        Refit refit = loadedRefit("<oldUnitParts><pid>" + ON_UNIT_NUMBER + "</pid></oldUnitParts>"
                                        + "<newUnitParts><pid>" + ON_UNIT_NUMBER + "</pid><pid>" + KIT_NUMBER
                                        + "</pid></newUnitParts>"
                                        + "<newArmorSuppliesId>" + ARMOR_NUMBER + "</newArmorSuppliesId>");

        assertEquals(List.of(partOnUnit), refit.getOldUnitParts());
        assertEquals(List.of(partOnUnit, kitPart), refit.getNewUnitParts());
        assertSame(armorSupplies, refit.getNewArmorSupplies());
    }

    @Test
    void anOlderSaveRefitDropsANumberThatNamesNoPartOfItsOwn() {
        Refit refit = loadedRefit("<oldUnitParts><pid>" + ON_UNIT_NUMBER + "</pid></oldUnitParts>");

        assertEquals(List.of(), refit.getOldUnitParts(), "The main force's spare with that number is not taken");
    }

    @Test
    void aRefitSavedByIdentityFindsItsPartsInAnyWarehouse() {
        Part kitPart = olderSavePart(laserTemplate, KIT_NUMBER, "<refitId>" + wolverine.getId() + "</refitId>");
        baseWarehouse.addPart(kitPart);

        Refit refit = loadedRefit("<newUnitParts><partUniqueId>" + kitPart.getUniqueId()
                                        + "</partUniqueId></newUnitParts>");

        assertEquals(List.of(kitPart), refit.getNewUnitParts());
    }
}
