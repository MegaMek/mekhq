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
package mekhq.campaign.personnel;

import static mekhq.campaign.personnel.familiarity.Familiarity.FAMILIARITY_THREE_HUNDRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static testUtilities.MHQTestUtilities.mockCampaign;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.familiarity.Familiarity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Coverage for {@link Person#addChassisFamiliarity(String, int, int)}'s handling of the caller-supplied cap.
 *
 * <p>Different callers pass different caps for the same character in the same session: a scenario grants up to
 * {@link Familiarity#getFamiliarityCap()}, while training caps a trainee at {@link Familiarity#getTrainingCap()} and an
 * educator at half that. A cap must therefore limit only what a gain can reach - never pull down progression already
 * earned above it.</p>
 */
class PersonChassisFamiliarityTest {
    private static final String CHASSIS = "Atlas";

    private Person person;

    @BeforeEach
    void setUp() {
        Campaign campaign = mockCampaign();
        person = new Person("Given", "Sur", campaign, "MERC");
    }

    @Test
    void testAddBelowCapAccumulates() {
        person.setChassisFamiliarity(CHASSIS, 40);

        person.addChassisFamiliarity(CHASSIS, 10, 100);

        assertEquals(50, person.getChassisFamiliarity(CHASSIS));
    }

    @Test
    void testAddIsLimitedToCap() {
        person.setChassisFamiliarity(CHASSIS, 95);

        person.addChassisFamiliarity(CHASSIS, 10, 100);

        assertEquals(100, person.getChassisFamiliarity(CHASSIS), "a gain may not overshoot the cap it is given");
    }

    /**
     * Regression: a veteran at 180 receiving a training gain capped at the 100 trainee cap kept the gain's cap as its
     * new total, wiping 80 points of persisted progression.
     */
    @Test
    void testAddAboveTraineeCapPreservesExistingValue() {
        person.setChassisFamiliarity(CHASSIS, 180);

        person.addChassisFamiliarity(CHASSIS, 2, Familiarity.NORMAL.getTrainingCap());

        assertEquals(180, person.getChassisFamiliarity(CHASSIS),
              "a cap-limited gain must not reduce familiarity already above that cap");
    }

    /**
     * Regression: the educator's half-of-training cap (50) reduced a commander who had just been granted familiarity up
     * to the full mode cap during the same training session.
     */
    @Test
    void testAddAboveEducatorCapPreservesExistingValue() {
        person.setChassisFamiliarity(CHASSIS, 250);

        person.addChassisFamiliarity(CHASSIS, 1, Familiarity.NORMAL.getTrainingCap() / 2);

        assertEquals(250, person.getChassisFamiliarity(CHASSIS),
              "the educator's halved training cap must not overwrite earned progression");
    }

    /**
     * The full session order the reviewed defect described: a scenario/commander grant up to the mode cap, followed by
     * the same character being processed as an educator under the halved training cap.
     */
    @Test
    void testFullCapGrantSurvivesLaterEducatorGrant() {
        Familiarity mode = Familiarity.HARD;
        person.addChassisFamiliarity(CHASSIS, 500, mode.getFamiliarityCap());
        assertEquals(FAMILIARITY_THREE_HUNDRED, person.getChassisFamiliarity(CHASSIS));

        person.addChassisFamiliarity(CHASSIS, 3, mode.getTrainingCap() / 2);

        assertEquals(FAMILIARITY_THREE_HUNDRED, person.getChassisFamiliarity(CHASSIS),
              "the full-cap grant must survive educator processing in the same session");
    }

    @Test
    void testNegativeAmountAppliesAboveCap() {
        person.setChassisFamiliarity(CHASSIS, 250);

        person.addChassisFamiliarity(CHASSIS, -10, 50);

        assertEquals(240, person.getChassisFamiliarity(CHASSIS),
              "a loss applies in full and is not collapsed onto the cap");
    }

    @Test
    void testNegativeAmountFloorsAtZeroAndRemovesEntry() {
        person.setChassisFamiliarity(CHASSIS, 5);

        person.addChassisFamiliarity(CHASSIS, -20, 100);

        assertEquals(0, person.getChassisFamiliarity(CHASSIS));
        assertFalse(person.getChassisFamiliarity().containsKey(CHASSIS), "a zeroed chassis is not retained");
    }

    @Test
    void testNoOpAmountAndBlankChassisAreIgnored() {
        person.setChassisFamiliarity(CHASSIS, 60);

        person.addChassisFamiliarity(CHASSIS, 0, 100);
        person.addChassisFamiliarity("", 10, 100);
        person.addChassisFamiliarity(null, 10, 100);

        assertEquals(60, person.getChassisFamiliarity(CHASSIS));
        assertEquals(1, person.getChassisFamiliarity().size());
    }
}
