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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;

import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.BLKFile;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A custom refit may take a name that is already used when the unit saved under it is the same design, so a second
 * platoon converted into the same custom infantry does not need a new name each time (MekHQ issue 1003). These tests
 * check what counts as the same design.
 */
class RefitDesignMatchTest {
    private static final int LATER_YEAR = 3067;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    /** Gives a unit a new name, file identity and year, as a later refit to the same design would have. */
    private static Entity renamed(UnitFixture fixture) {
        Entity entity = fixture.loadEntity();
        entity.setChassis("Hectan Shock Troopers");
        entity.setModel("");
        entity.regenerateUnitFileUUID();
        entity.setYear(LATER_YEAR);
        return entity;
    }

    @Test
    void samePlatoonUnderAnotherNameIsTheSameDesign() {
        assertTrue(RefitDesignMatch.isSameDesign(renamed(UnitFixture.FOOT_PLATOON_LASER),
              UnitFixture.FOOT_PLATOON_LASER.loadEntity()));
    }

    @Test
    void platoonWithOtherWeaponsIsADifferentDesign() {
        assertFalse(RefitDesignMatch.isSameDesign(renamed(UnitFixture.FOOT_PLATOON_LASER),
              UnitFixture.FOOT_PLATOON_MG.loadEntity()));
    }

    @Test
    void platoonConvertedToJumpInfantryIsADifferentDesign() {
        Entity jumpPlatoon = renamed(UnitFixture.FOOT_PLATOON_LASER);
        jumpPlatoon.setMovementMode(EntityMovementMode.INF_JUMP);

        assertFalse(RefitDesignMatch.isSameDesign(jumpPlatoon, UnitFixture.FOOT_PLATOON_LASER.loadEntity()));
    }

    /** The real case: the first conversion saved a unit file, and the second conversion is compared against it. */
    @Test
    void designMatchesItsOwnSavedUnitFile(@TempDir Path folder) throws Exception {
        Entity firstConversion = renamed(UnitFixture.FOOT_PLATOON_LASER);
        File unitFile = folder.resolve("Hectan Shock Troopers.blk").toFile();
        BLKFile.encode(unitFile.getPath(), firstConversion);
        Entity saved = new MekFileParser(unitFile).getEntity();

        Entity secondConversion = renamed(UnitFixture.FOOT_PLATOON_LASER);
        secondConversion.setYear(LATER_YEAR + 1);

        assertTrue(RefitDesignMatch.isSameDesign(secondConversion, saved));
    }

    @Test
    void sameMekUnderAnotherNameIsTheSameDesign() {
        Entity locust = UnitFixture.LOCUST_LCT_1V.loadEntity();
        locust.setModel("LCT-1V2");
        locust.regenerateUnitFileUUID();

        assertTrue(RefitDesignMatch.isSameDesign(locust, UnitFixture.LOCUST_LCT_1V.loadEntity()));
    }

    @Test
    void otherMekVariantIsADifferentDesign() {
        assertFalse(RefitDesignMatch.isSameDesign(UnitFixture.LOCUST_LCT_1E.loadEntity(),
              UnitFixture.LOCUST_LCT_1V.loadEntity()));
    }

    /**
     * A refit that takes the saved custom design's name lists it among the campaign's customs, so the campaign file
     * keeps the design as it does for a newly saved one.
     */
    @Test
    void refitUsingASavedCustomDesignListsItAmongTheCustoms() {
        PartsScenario scenario = PartsScenario.create();
        Campaign campaign = scenario.getCampaign();
        Unit platoon = scenario.withUnit(UnitFixture.FOOT_PLATOON_MG);
        Refit refit = new Refit(platoon, renamed(UnitFixture.FOOT_PLATOON_LASER), true, false, false);
        MekSummary savedCustomDesign = mock(MekSummary.class);
        when(savedCustomDesign.isCanon()).thenReturn(false);

        refit.useSavedDesign(savedCustomDesign);

        Unit refittedPlatoon = mock(Unit.class);
        when(refittedPlatoon.getEntity()).thenReturn(refit.getNewEntity());
        assertTrue(campaign.isCustom(refittedPlatoon));
    }

    @Test
    void refitUsingACanonDesignDoesNotListItAmongTheCustoms() {
        PartsScenario scenario = PartsScenario.create();
        Campaign campaign = scenario.getCampaign();
        Unit platoon = scenario.withUnit(UnitFixture.FOOT_PLATOON_MG);
        Refit refit = new Refit(platoon, UnitFixture.FOOT_PLATOON_LASER.loadEntity(), true, false, false);
        MekSummary canonDesign = mock(MekSummary.class);
        when(canonDesign.isCanon()).thenReturn(true);

        refit.useSavedDesign(canonDesign);

        Unit refittedPlatoon = mock(Unit.class);
        when(refittedPlatoon.getEntity()).thenReturn(refit.getNewEntity());
        assertFalse(campaign.isCustom(refittedPlatoon));
    }

    @Test
    void differentKindsOfUnitAreDifferentDesigns() {
        assertFalse(RefitDesignMatch.isSameDesign(UnitFixture.LOCUST_LCT_1V.loadEntity(),
              UnitFixture.FOOT_PLATOON_LASER.loadEntity()));
    }
}
