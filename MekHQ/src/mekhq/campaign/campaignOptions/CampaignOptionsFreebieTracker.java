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
package mekhq.campaign.campaignOptions;

/**
 * Immutable snapshot of a subset of {@link CampaignOptions} that are considered "critical" for gameplay balance and
 * player entitlements.
 *
 * <p>This record exists to support change detection: store an instance representing the option state at a known
 * point in time (for example, campaign creation or last acknowledged configuration), then compare it to a newly
 * constructed instance to determine whether the player has enabled/disabled options that may warrant:</p>
 *
 * <ul>
 *   <li>offering free units (or similar compensation),</li>
 *   <li>triggering migration/adjustment handlers, or</li>
 *   <li>showing warnings/confirmation prompts before applying the changes.</li>
 * </ul>
 *
 * <p>The values captured here are intentionally reduced to simple booleans so they can be compared cheaply and
 * reliably via the record's generated {@link #equals(Object)} and {@link #hashCode()} implementations.</p>
 *
 * @param awardVeterancySPAs            whether the campaign awards veterancy SPAs
 * @param trackFactionStanding          whether faction standing is tracked
 * @param trackPrisoners                whether prisoners are tracked (derived from prisoner capture style)
 * @param useMASHTheatres               whether MASH theatres are enabled
 * @param useFatigue                    whether fatigue rules are enabled
 * @param useAdvancedSalvage            whether advanced salvage rules are enabled
 * @param useStratCon                   whether StratCon is enabled
 * @param useMapless                    whether StratCon mapless mode is enabled
 * @param useAdvancedScouting           whether advanced scouting is enabled (and applicable)
 * @param useAltAdvancedMedical         whether alternative advanced medical rules are enabled
 * @param useDiseases                   whether random diseases are enabled (and applicable)
 * @param useNormalizedContractPayModel whether contract pay is using the alternate method
 * @param useDiminishingContractPay     whether diminishing returns are applied to contract pay
 *
 * @author Illiani
 * @since 0.50.11
 */
public record CampaignOptionsFreebieTracker(boolean awardVeterancySPAs, boolean trackFactionStanding,
      boolean trackPrisoners, boolean useMASHTheatres, boolean useFatigue, boolean useAdvancedSalvage,
      boolean useStratCon, boolean useMapless, boolean useAdvancedScouting, boolean useAltAdvancedMedical,
      boolean useDiseases, boolean useNormalizedContractPayModel, boolean useDiminishingContractPay) {
    /**
     * Creates a tracker snapshot from the provided {@link CampaignOptions}.
     *
     * <p>Some fields are derived to reflect the effective ruleset:</p>
     * <ul>
     *   <li>{@code trackPrisoners} is derived from the prisoner capture style,</li>
     *   <li>{@code useAdvancedScouting} only applies when StratCon is enabled,</li>
     *   <li>{@code useDiseases} only applies when alternative advanced medical is enabled.</li>
     *   <li>{@code useDiminishingContractPay} only applies when certain contract payment models are also selected.</li>
     * </ul>
     *
     * @param options source options to snapshot
     *
     * @author Illiani
     * @since 0.50.11
     */
    public CampaignOptionsFreebieTracker(CampaignOptions options) {
        this(
              options.isAwardVeterancySPAs(),
              options.isTrackFactionStanding(),
              !options.getPrisonerCaptureStyle().isNone(),
              options.isUseMASHTheatres(),
              options.isUseFatigue(),
              options.isUseCamOpsSalvage(),
              options.isUseStratCon(),
              options.isUseStratConMaplessMode(),
              options.isUseAdvancedScouting() && options.isUseStratCon(),
              options.isUseAlternativeAdvancedMedical(),
              options.isUseAlternativeAdvancedMedical() && options.isUseRandomDiseases(),
              options.isUseAlternatePaymentMode() && isDiminishingContractPayRelevant(options),
              options.isUseDiminishingContractPay() && isDiminishingContractPayRelevant(options)
        );
    }

    private static boolean isDiminishingContractPayRelevant(CampaignOptions options) {
        return options.isUsePeacetimeCost() || options.isEquipmentContractBase();
    }
}
