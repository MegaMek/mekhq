/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.utilities;

import static mekhq.campaign.utilities.TestableCampaign.initializeCampaignForTesting;
import static org.junit.jupiter.api.Assertions.fail;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.enums.PersonnelRole;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * A test class for testing the functionality and initialization of {@link Campaign} objects.
 *
 * <p>The test methods ensure that the critical functions of {@link Campaign} operate as expected, including
 * initialization and personnel generation. This class uses the
 * {@link TestableCampaign#initializeCampaignForTesting(boolean)} utility to set up the environment before tests are
 * executed.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
class TestableCampaignTest {
    static Campaign campaign;

    /**
     * Sets up resources required for all test methods within this test class by initializing a {@link Campaign}
     * object.
     *
     * <p>This method also validates that the {@link TestableCampaign#initializeCampaignForTesting(boolean)} utility
     * function successfully initializes and returns a non-null {@link Campaign} instance. If the function returns null
     * or throws an exception, the test will fail.</p>
     *
     * <p>This test is the first warning sign that we've changed something in {@link Campaign} initialization that
     * will result in test failure. Generally, if we're seeing this test fail, it means a new step needs to be added to
     * {@link TestableCampaign#initializeCampaignForTesting(boolean)}.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    @BeforeAll
    static void beforeAll() {
        try {
            campaign = initializeCampaignForTesting(true);
        } catch (Exception e) {
            fail("Failed to create new campaign: " + e.getMessage());
        }
    }

    /**
     * Tests that a new person can be generated for a campaign without throwing any exceptions.
     *
     * <p>This method verifies the functionality of the {@link Campaign#newPerson(PersonnelRole)} method to ensure
     * that a new person can be successfully created. If the method encounters an exception or fails during execution,
     * the test will fail and provide the exception message.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    @Test
    void test_canGeneratePerson() {
        try {
            campaign.newPerson(PersonnelRole.MEKWARRIOR);
        } catch (Exception e) {
            fail("Failed to create new person: " + e.getMessage());
        }
    }
}
