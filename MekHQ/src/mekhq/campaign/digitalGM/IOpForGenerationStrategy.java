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
package mekhq.campaign.digitalGM;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.AbstractStratConGM;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;

/**
 * Strategy for generating the opposing force (OpFor) of a scenario &mdash; <i>what you fight</i>, as opposed to
 * {@link IScenarioGenerationStrategy}, which decides <i>when and which</i> scenarios appear. Splitting this out is what
 * lets a GM keep the standard mission cadence while changing enemy composition (scripted, fixed, differently scaled,
 * and so on).
 *
 * <p>The default StratCon implementation delegates to
 * {@link mekhq.campaign.mission.AtBDynamicScenarioFactory#finalizeScenario AtBDynamicScenarioFactory.finalizeScenario}
 * &mdash; the existing dynamic/random AtB generation &mdash; so the rules themselves are unchanged. The accessor lives
 * on {@link AbstractStratConGM AbstractStratConGM}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public interface IOpForGenerationStrategy {

    /**
     * Generates and finalises the OpFor for a scenario's backing dynamic scenario.
     *
     * @param backingScenario the dynamic scenario to populate with opposing forces
     * @param contract        the contract the scenario belongs to
     * @param campaign        the active campaign
     */
    void generateOpFor(AtBDynamicScenario backingScenario, AtBContract contract, Campaign campaign);
}
