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
package mekhq.campaign.mission.contract.contractGeneration;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;

/**
 * Composes a contract's operation codename body from the two word pools held by {@link RandomOperationNameGenerator}:
 * one weighted draw from the shared descriptor pool combined with one weighted draw from the noun pool themed to the
 * contract's {@link ContractObjectiveType} (for example, {@code RED} + {@code EAGLE} yields "RED EAGLE"). The caller
 * prepends the literal "Operation".
 *
 * <p>Structured after {@link mekhq.campaign.personnel.backgrounds.BackgroundsController}: a stateless composer over a
 * separate data-holding singleton, falling back to a resource-bundle value if a pool cannot be read.</p>
 */
public class OperationNameController {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.RandomOperationNameGenerator";

    /**
     * Generates an operation codename body for a contract of the given objective. The two words come from disjoint
     * pools (descriptor and noun), so they never collide and no de-duplication is needed.
     *
     * @param objectiveType the contract's objective, selecting the noun pool that carries the objective flavor
     *
     * @return a two-word codename body such as "RED EAGLE", or the fallback value if the word pools are unavailable
     */
    public static String generateOperationName(ContractObjectiveType objectiveType) {
        RandomOperationNameGenerator generator = RandomOperationNameGenerator.getInstance();

        String descriptor = generator.generateDescriptor();
        String noun = generator.generateNoun(objectiveType);

        if ((descriptor == null) || descriptor.isBlank() || (noun == null) || noun.isBlank()) {
            return getTextAt(RESOURCE_BUNDLE, "fallbackValue");
        }

        return descriptor + ' ' + noun;
    }
}
