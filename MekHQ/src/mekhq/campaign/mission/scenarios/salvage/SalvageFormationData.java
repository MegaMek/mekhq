/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.scenarios.salvage;

import static java.lang.Math.max;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.Nullable;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * A summary of what a formation can bring to a salvage operation.
 *
 * @param formation            the formation
 * @param formationType        the formation's type
 * @param tech                 the formation's TO&amp;E tech, if it's a Salvage formation and the tech isn't an engineer;
 *                             otherwise {@code null}
 * @param maximumCargoCapacity the largest cargo capacity of any of its units that can salvage, in tons
 * @param maximumTowCapacity   the largest tow capacity of any of its units that can salvage, in tons; in space, the
 *                             heaviest such unit's weight
 * @param salvageCapableUnits  how many of its units can take part in the salvage operation
 * @param hasTug               {@code true} if one of those units has a working naval tug adaptor (space only)
 * @param isSpaceScenario      {@code true} if the salvage operation takes place in space
 */
public record SalvageFormationData(Formation formation, FormationType formationType, @Nullable Person tech,
      double maximumCargoCapacity,
      double maximumTowCapacity, int salvageCapableUnits, boolean hasTug, boolean isSpaceScenario) {
    /**
     * Summarizes a formation's salvage capabilities.
     *
     * @param campaign        the current campaign
     * @param formation       the formation
     * @param isSpaceScenario {@code true} if the salvage operation takes place in space
     *
     * @return the formation's salvage capabilities
     */
    public static SalvageFormationData buildData(Campaign campaign, Formation formation, boolean isSpaceScenario) {
        FormationType formationType = formation.getFormationType();
        UUID techId = formation.getTechID();
        Person tech;
        if ((techId == null) || !formationType.isSalvage()) {
            tech = null;
        } else {
            tech = campaign.getPlayerForce().getHumanResources().getPerson(techId);
        }
        if ((tech != null) && tech.isEngineer()) { // Engineers cannot salvage
            tech = null;
        }

        double maximumCargoCapacity = 0.0;
        double maximumTowCapacity = 0.0;
        int salvageCapableUnits = 0;
        boolean hasTug = false;

        for (Unit unit : getAvailableSalvageUnits(campaign, formation, isSpaceScenario)) {
            Entity entity = unit.getEntity();
            salvageCapableUnits++;

            double cargoCapacity = unit.getCargoCapacityForSalvage();
            maximumCargoCapacity = max(cargoCapacity, maximumCargoCapacity);

            if (isSpaceScenario) {
                maximumTowCapacity = max(entity.getWeight(), maximumTowCapacity);
                hasTug |= CamOpsSalvageUtilities.hasNavalTug(entity);
            } else {
                maximumTowCapacity = max(CamOpsSalvageUtilities.getTowCapacity(unit), maximumTowCapacity);
            }
        }

        return new SalvageFormationData(formation,
              formationType,
              tech,
              maximumCargoCapacity,
              maximumTowCapacity,
              salvageCapableUnits,
              hasTug,
              isSpaceScenario);
    }

    /**
     * Collects the units in a formation that are able to take part in salvage operations.
     *
     * @param campaign        the current campaign
     * @param formation       the formation to check
     * @param isSpaceScenario {@code true} if the salvage operation takes place in space
     *
     * @return the units available for salvage operations
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static List<Unit> getAvailableSalvageUnits(Campaign campaign, Formation formation,
          boolean isSpaceScenario) {
        AbstractSalvage salvageRules = campaign.getCampaignOptions().get(CampaignOption.SALVAGE_SYSTEM).getSalvage();
        List<Unit> availableUnits = new ArrayList<>();
        for (Unit unit : formation.getAllUnitsAsUnits(campaign.getPlayerForce().getHangar(), false)) {
            if (salvageRules.isAvailableForSalvage(unit, isSpaceScenario)) {
                availableUnits.add(unit);
            }
        }
        return availableUnits;
    }
}
