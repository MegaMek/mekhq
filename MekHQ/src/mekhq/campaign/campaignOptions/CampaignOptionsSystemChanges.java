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
package mekhq.campaign.campaignOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Works out which of the campaign's major systems switch on or off when one set of campaign options replaces another,
 * and which of those switches open a follow-up dialog afterward.
 *
 * <p>The systems are the ones {@link CampaignOptionsFreebieTracker} records, because those are the switches that
 * {@code CampaignOptionsPane.triggerUpgradeFreebies} reacts to. The follow-up rules here mirror that method, so the
 * campaign upgrade console can warn the player about each dialog before it appears.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class CampaignOptionsSystemChanges {
    /**
     * When a system's change opens a follow-up dialog.
     */
    public enum FollowUp {
        /** The change never opens a dialog. */
        NEVER,
        /** Only switching the system on opens a dialog. */
        WHEN_TURNED_ON,
        /** Switching the system on or off opens a dialog. */
        ALWAYS
    }

    /**
     * A campaign system tracked by {@link CampaignOptionsFreebieTracker}.
     */
    public enum TrackedSystem {
        FACTION_STANDING(CampaignOptionsFreebieTracker::trackFactionStanding, FollowUp.ALWAYS),
        VETERANCY_AWARDS(CampaignOptionsFreebieTracker::awardVeterancySPAs, FollowUp.WHEN_TURNED_ON),
        SUPPORT_TEAMS(CampaignOptionsFreebieTracker::useSupportTeams, FollowUp.WHEN_TURNED_ON),
        PRISONERS(CampaignOptionsFreebieTracker::trackPrisoners, FollowUp.WHEN_TURNED_ON),
        MASH_THEATRES(CampaignOptionsFreebieTracker::useMASHTheatres, FollowUp.WHEN_TURNED_ON),
        FATIGUE(CampaignOptionsFreebieTracker::useFatigue, FollowUp.WHEN_TURNED_ON),
        SALVAGE_OPERATIONS(CampaignOptionsFreebieTracker::useAdvancedSalvage, FollowUp.WHEN_TURNED_ON),
        STRATCON(CampaignOptionsFreebieTracker::useStratCon, FollowUp.WHEN_TURNED_ON),
        STRATCON_MAPLESS(CampaignOptionsFreebieTracker::useMapless, FollowUp.WHEN_TURNED_ON),
        ADVANCED_SCOUTING(options -> options.useAdvancedScouting() && options.useStratCon(),
              FollowUp.WHEN_TURNED_ON),
        ALTERNATIVE_ADVANCED_MEDICAL(CampaignOptionsFreebieTracker::useAltAdvancedMedical, FollowUp.WHEN_TURNED_ON),
        DISEASES(options -> options.useAltAdvancedMedical() && options.useDiseases(), FollowUp.NEVER),
        LEGACY_CONTRACT_PAY(CampaignOptionsFreebieTracker::useLegacyContractOptions, FollowUp.NEVER),
        NORMALIZED_CONTRACT_PAY(options -> options.useNormalizedContractPayModel()
                                                 && options.useLegacyContractOptions(), FollowUp.WHEN_TURNED_ON),
        DIMINISHING_CONTRACT_PAY(options -> options.useDiminishingContractPay()
                                                  && options.useLegacyContractOptions(), FollowUp.WHEN_TURNED_ON),
        CHAOS_REPUTATION(CampaignOptionsFreebieTracker::useChaosReputation, FollowUp.WHEN_TURNED_ON),
        MEKWARRIOR_KITS(CampaignOptionsFreebieTracker::requireMekWarriorKitToDeploy, FollowUp.WHEN_TURNED_ON),
        AEROSPACE_KITS(CampaignOptionsFreebieTracker::requireAerospaceKitToDeploy, FollowUp.WHEN_TURNED_ON),
        SPECIALIST_TECH_SKILLS(CampaignOptionsFreebieTracker::specialistTechSkillsEnabled, FollowUp.WHEN_TURNED_ON),
        TECH_TOOL_KITS(CampaignOptionsFreebieTracker::techsNeedToolKit, FollowUp.WHEN_TURNED_ON);

        private final Predicate<CampaignOptionsFreebieTracker> isOn;
        private final FollowUp followUp;

        TrackedSystem(Predicate<CampaignOptionsFreebieTracker> isOn, FollowUp followUp) {
            this.isOn = isOn;
            this.followUp = followUp;
        }

        /**
         * @param options the options to read
         *
         * @return {@code true} if the system is switched on in those options
         */
        public boolean isOn(CampaignOptionsFreebieTracker options) {
            return isOn.test(options);
        }

        /**
         * @return when a change to this system opens a follow-up dialog
         */
        public FollowUp getFollowUp() {
            return followUp;
        }
    }

    /**
     * One system that changes.
     *
     * @param system      the system
     * @param isNowOn     {@code true} if the system switches on; {@code false} if it switches off
     * @param hasFollowUp {@code true} if the change may open a follow-up dialog once the new options are applied
     */
    public record SystemChange(TrackedSystem system, boolean isNowOn, boolean hasFollowUp) {}

    private CampaignOptionsSystemChanges() {}

    /**
     * Lists the systems that switch on or off when {@code newOptions} replace {@code oldOptions}, in the order of
     * {@link TrackedSystem}.
     *
     * @param oldOptions the options in use now
     * @param newOptions the options that will replace them
     *
     * @return the systems that change; empty if none do
     */
    public static List<SystemChange> between(CampaignOptionsFreebieTracker oldOptions,
          CampaignOptionsFreebieTracker newOptions) {
        List<SystemChange> changes = new ArrayList<>();
        for (TrackedSystem system : TrackedSystem.values()) {
            boolean wasOn = system.isOn(oldOptions);
            boolean isNowOn = system.isOn(newOptions);
            if (wasOn == isNowOn) {
                continue;
            }
            boolean hasFollowUp = switch (system.getFollowUp()) {
                case NEVER -> false;
                case WHEN_TURNED_ON -> isNowOn;
                case ALWAYS -> true;
            };
            changes.add(new SystemChange(system, isNowOn, hasFollowUp));
        }
        return changes;
    }
}
