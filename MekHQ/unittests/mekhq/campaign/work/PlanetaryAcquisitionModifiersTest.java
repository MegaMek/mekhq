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
package mekhq.campaign.work;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.TargetRollModifier;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Under Planetary Acquisition the daily search adds the planet's tech, industry and output modifiers, but the
 * Procurement tab and the other screens showed the target number without them. A part shown at 3+ was rolled against a
 * much higher number, and failed day after day with no reason given (issue #5691).
 */
class PlanetaryAcquisitionModifiersTest {
    private static final int PLANET_TECH_PENALTY = 3;

    private Campaign campaign;
    private MissingPart missingLaser;

    @BeforeEach
    void setUp() {
        PartsScenario scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        CampaignOptions options = campaign.getCampaignOptions();
        options.set(CampaignOption.USE_PLANETARY_ACQUISITION, true);
        for (PlanetarySophistication sophistication : PlanetarySophistication.values()) {
            options.setPlanetTechAcquisitionBonus(PLANET_TECH_PENALTY, sophistication);
        }
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        PartsScenario.unitParts(locust, EquipmentPart.class).getFirst().remove(false);
        missingLaser = PartsScenario.unitParts(locust, MissingPart.class).getFirst();
    }

    private int shownModifiers() {
        return PlanetaryAcquisitionModifiers.addForCurrentSystem(campaign, missingLaser,
              campaign.getAcquisitionModifiers(missingLaser)).getValue();
    }

    private int modifiersWithoutThePlanet() {
        return campaign.getAcquisitionModifiers(missingLaser).getValue();
    }

    @Test
    void theShownModifiersIncludeTheCurrentPlanet() {
        PlanetarySystem currentSystem = campaign.getCurrentSystem();
        assertNotNull(currentSystem, "The test campaign starts in a star system");
        List<TargetRollModifier> planetModifiers = PlanetaryAcquisitionModifiers.forSystem(campaign, missingLaser,
              currentSystem);
        int planetTotal = planetModifiers.stream().mapToInt(TargetRollModifier::value).sum();
        assertTrue(planetTotal >= PLANET_TECH_PENALTY, "Every tech level carries a penalty in this test");

        assertEquals(modifiersWithoutThePlanet() + planetTotal, shownModifiers(),
              "The shown modifiers include the ones the daily search adds here");
    }

    @Test
    void theShownModifiersIgnoreThePlanetWithoutPlanetaryAcquisition() {
        campaign.getCampaignOptions().set(CampaignOption.USE_PLANETARY_ACQUISITION, false);

        assertEquals(modifiersWithoutThePlanet(), shownModifiers());
    }

    @Test
    void aPurchaseNotTiedToASystemHasNoPlanetModifiers() {
        assertTrue(PlanetaryAcquisitionModifiers.forSystem(campaign, missingLaser, null).isEmpty());
    }
}
