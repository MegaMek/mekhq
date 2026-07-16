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
package mekhq.campaign.digitalGM.stratCon;

import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.DigitalGMRegistry;
import mekhq.campaign.digitalGM.IDigitalGM;
import mekhq.campaign.digitalGM.IForceDeploymentStrategy;
import mekhq.campaign.digitalGM.IMapGenerationStrategy;
import mekhq.campaign.digitalGM.IOpForDeploymentStrategy;
import mekhq.campaign.digitalGM.IOpForGenerationStrategy;

/**
 * Entry point for non-GM StratCon code (mainly the deployment UI) to reach the active digital GM's strategies for a
 * campaign, rather than calling the static rules directly. This is what makes the strategy seams live: routing a call
 * through here means a GM that overrides the strategy actually changes behaviour at the call site.
 *
 * <p>When no digital GM governs the campaign &mdash; or the active GM is not StratCon-based &mdash; the lookup falls
 * back to the default StratCon strategy, which delegates to {@link StratConRulesManager}. That keeps behaviour
 * identical to the pre-registry code path.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConGMs {
    /**
     * The GM consulted when no digital GM governs the campaign (or the active one is not StratCon-based): the default,
     * map-based StratCon system. Its strategies <i>are</i> the StratCon defaults, defined once by the concrete system
     * ({@link StratConDigitalGM} / {@link AbstractStratConGM}) rather than duplicated here.
     */
    private static final AbstractStratConGM DEFAULT_GM = new StratConDigitalGM();

    private StratConGMs() {
    }

    /**
     * Resolves the force-deployment strategy of the digital GM governing the given campaign.
     *
     * @param campaignOptions the campaign options used to consult the right GM
     *
     * @return the active StratCon GM's {@link IForceDeploymentStrategy}, or the default StratCon strategy when no
     *       StratCon GM is active
     */
    public static IForceDeploymentStrategy forceDeployment(CampaignOptions campaignOptions) {
        return resolveGM(campaignOptions).getForceDeploymentStrategy();
    }

    /**
     * Resolves the OpFor-generation strategy of the digital GM governing the given campaign.
     *
     * @param campaignOptions the campaign options used to consult the right GM
     *
     * @return the active StratCon GM's {@link IOpForGenerationStrategy}, or the default StratCon strategy when no
     *       StratCon GM is active
     */
    public static IOpForGenerationStrategy opForGeneration(CampaignOptions campaignOptions) {
        return resolveGM(campaignOptions).getOpForGenerationStrategy();
    }

    /**
     * Resolves the OpFor-deployment strategy of the digital GM governing the given campaign.
     *
     * @param campaignOptions the campaign options used to consult the right GM
     *
     * @return the active StratCon GM's {@link IOpForDeploymentStrategy}, or the default StratCon strategy when no
     *       StratCon GM is active
     */
    public static IOpForDeploymentStrategy opForDeployment(CampaignOptions campaignOptions) {
        return resolveGM(campaignOptions).getOpForDeploymentStrategy();
    }

    /**
     * Resolves the map-generation (terrain) strategy of the digital GM governing the given campaign.
     *
     * @param campaignOptions the campaign options used to consult the right GM
     *
     * @return the active StratCon GM's {@link IMapGenerationStrategy}, or the default StratCon strategy when no
     *       StratCon GM is active
     */
    public static IMapGenerationStrategy mapGeneration(CampaignOptions campaignOptions) {
        return resolveGM(campaignOptions).getMapGenerationStrategy();
    }

    /**
     * Resolves the StratCon GM that governs the given campaign, falling back to the
     * {@link #DEFAULT_GM default StratCon GM} when no GM is active or the active one is not StratCon-based. Every
     * strategy lookup routes through here so the fallback strategies come from the concrete system definition instead
     * of being re-declared in this facade.
     *
     * @param campaignOptions the campaign options used to consult the right GM
     *
     * @return the governing StratCon GM, or the default StratCon GM
     */
    private static AbstractStratConGM resolveGM(CampaignOptions campaignOptions) {
        IDigitalGM activeGM = DigitalGMRegistry.getActiveGM(campaignOptions).orElse(null);

        if (activeGM instanceof AbstractStratConGM stratConGM) {
            return stratConGM;
        }

        return DEFAULT_GM;
    }
}
