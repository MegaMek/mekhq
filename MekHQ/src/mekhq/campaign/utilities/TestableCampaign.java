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

import java.io.IOException;

import megamek.common.EquipmentType;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.universe.Systems;

/**
 * The {@code TestableCampaign} class provides a static utility method for preparing the minimal required subsystems to
 * create and test a {@link Campaign} object.
 *
 * @author Illiani
 * @since 0.50.07
 */
public class TestableCampaign {
    private final static MMLogger LOGGER = MMLogger.create(TestableCampaign.class);

    /**
     * Initializes the minimal set of subsystems required to create a {@link Campaign} object for use in unit testing.
     *
     * <p>This includes loading system data, initializing equipment types, and setting up rank systems. If {@code
     * shouldInitializeSkillTypes} is {@code true}, skill types are also initialized, which is necessary for random
     * personnel generation when using the {@link Campaign#newPerson(PersonnelRole)} family of methods.</p>
     *
     * <p>If personnel objects are being created via {@link Person#Person(Campaign)}, skill type initialization may
     * not be required, and this parameter can be set to {@code false}.</p>
     *
     * <p><b>Usage:</b> this method should be used judiciously and only once per battery of tests. Generally
     * speaking, creating {@link Campaign} objects is expensive and not suitable for every test battery. Instead,
     * consider breaking the dependency on {@link Campaign} in the method you're trying to test. This can be done by
     * only passing in the values essential to the method (or class's) function, rather than the entire {@link Campaign}
     * object. If this cannot be done, then create a single {@link Campaign} object for the whole test class (via this
     * method) and then modify that single object to meet your needs. This will help ensure test time doesn't balloon
     * unnecessarily.</p>
     *
     * @param shouldInitializeSkillTypes {@code true} to initialize additional resources required for personnel
     *                                   generation; {@code false} otherwise. This can generally be left {@code false}
     *                                   unless using the {@link Campaign#newPerson(PersonnelRole)} methods.
     *
     * @return a new {@link Campaign} instance with the required subsystems initialized for testing
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static Campaign initializeCampaignForTesting(boolean shouldInitializeSkillTypes) {
        try {
            Systems.setInstance(Systems.loadDefault());
        } catch (IOException e) {
            LOGGER.error("Failed to load default systems", e);
        }

        EquipmentType.initializeTypes();

        Ranks.initializeRankSystems();

        if (shouldInitializeSkillTypes) {
            SkillType.initializeTypes();
        }

        return new Campaign();
    }
}
