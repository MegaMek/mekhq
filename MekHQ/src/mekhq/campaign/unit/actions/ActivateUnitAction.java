/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.unit.actions;

import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * Activates a unit.
 */
public record ActivateUnitAction(Person tech, boolean isGM) implements IUnitAction {
    private static final MMLogger LOGGER = MMLogger.create(ActivateUnitAction.class);

    /**
     * Initializes a new instance of the ActivateUnitAction class.
     *
     * @param tech The technician performing the work, or null if no one is needed to perform the work (self crewed or
     *             GM).
     * @param isGM A boolean value indicating whether GM mode should be used to complete the action.
     */
    public ActivateUnitAction(@Nullable Person tech, boolean isGM) {
        this.tech = tech;
        this.isGM = isGM;
    }

    @Override
    public void execute(Campaign campaign, Unit unit) {
        if (isGM) {
            unit.startActivating(null, true);
        } else {
            if (!unit.isConventionalInfantry() && (null == tech)) {
                return;
            }

            Entity entity = unit.getEntity();

            if (entity == null) {
                LOGGER.error("Unit has no entity: {}", unit.getName());
                return;
            }

            if (tech != null && entity.isLargeCraft() && !unit.getCrew().contains(tech)) {
                if (!tech.isTechLargeVessel()) {
                    LOGGER.error("{} is not a vessel tech", tech.getFullTitle());
                    return;
                }

                if (!unit.canTakeMoreGenericCrew()) {
                    LOGGER.warn("Unit has too many vessel crew members: {}", unit.getName());
                    return;
                }

                unit.addGenericCrew(tech, true);
            }

            unit.startActivating(tech);
        }
    }
}
