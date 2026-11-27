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
package mekhq.campaign.personnel.familiarity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Coverage for how {@link Familiarity#assignFamiliarity(Campaign, Unit, int, int, FamiliarityGainType)} awards a unit's
 * tech: the crew's gain is divided by the number of units the tech is assigned to, rather than being withheld entirely
 * from techs assigned to more than one unit.
 */
class FamiliarityTechShareTest {
    private static final String CHASSIS = "Atlas";
    private static final int FAMILIARITY_SPEED = 12;

    private Campaign campaign;
    private Person pilot;
    private Person tech;
    private int familiarityCap;

    @BeforeEach
    void beforeEach() {
        campaign = MHQTestUtilities.getTestCampaign();
        campaign.getCampaignOptions().set(CampaignOption.CHASSIS_FAMILIARITY_MODE, Familiarity.NORMAL);
        familiarityCap = Familiarity.NORMAL.getFamiliarityCap();

        pilot = new Person(campaign);
        tech = new Person(campaign);
    }

    private Unit mockUnit(Person... crew) {
        Entity entity = mock(Entity.class);
        when(entity.getChassis()).thenReturn(CHASSIS);
        when(entity.isChassisFamiliarityEligible()).thenReturn(true);

        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        when(unit.getCrew()).thenReturn(List.of(crew));
        when(unit.getTech()).thenReturn(tech);
        tech.addTechUnit(unit);

        return unit;
    }

    private void assignSingle(Unit unit, int speed) {
        Familiarity.assignFamiliarity(campaign, unit, familiarityCap, speed, FamiliarityGainType.SINGLE);
    }

    @Test
    void testSingleUnitTechGainsTheFullAmount() {
        Unit unit = mockUnit(pilot);

        assignSingle(unit, FAMILIARITY_SPEED);

        assertEquals(FAMILIARITY_SPEED, pilot.getChassisFamiliarity(CHASSIS));
        assertEquals(FAMILIARITY_SPEED, tech.getChassisFamiliarity(CHASSIS));
    }

    /** Regression: a tech assigned to more than one unit used to gain no familiarity at all. */
    @Test
    void testMultiUnitTechGainsTheirShare() {
        Unit unit = mockUnit(pilot);
        mockUnit();
        mockUnit();

        assignSingle(unit, FAMILIARITY_SPEED);

        assertEquals(FAMILIARITY_SPEED, pilot.getChassisFamiliarity(CHASSIS), "the crew's gain is not divided");
        assertEquals(FAMILIARITY_SPEED / 3, tech.getChassisFamiliarity(CHASSIS),
              "a tech assigned to three units earns a third of the gain");
    }

    @Test
    void testFractionalShareIsRounded() {
        Unit unit = mockUnit(pilot);
        mockUnit();
        mockUnit();
        mockUnit();

        // 5 / 4 = 1.25, which rounds to 1
        assignSingle(unit, 5);

        assertEquals(1, tech.getChassisFamiliarity(CHASSIS));
    }

    @Test
    void testShareCombinesWithTraitMultiplier() {
        tech.getOptions().getOption(PersonnelOptions.FAMILIARITY_IRON_BOND).setValue(true);
        Unit unit = mockUnit(pilot);
        mockUnit();
        mockUnit();

        // 12 * 1.5 / 3 = 6
        assignSingle(unit, FAMILIARITY_SPEED);

        assertEquals(6, tech.getChassisFamiliarity(CHASSIS));
    }
}
