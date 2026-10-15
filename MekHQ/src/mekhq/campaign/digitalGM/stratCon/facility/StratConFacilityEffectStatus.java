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
package mekhq.campaign.digitalGM.stratCon.facility;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityCondition;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityIntel;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.atb.AtBScenarioModifier;

/**
 * Explains what facilities are doing right now, and why an effect the player expects is not happening: support already
 * lent this week, held back by damage or the Clans' bidding rules, a garrison that is gone, or a number weakened by the
 * facility's condition. It also adds up what a sector's facilities do to it as a whole, including the share from enemy
 * facilities the player has not found, without saying where those are.
 *
 * <p>Every line is ready to show; none carries line breaks of its own.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConFacilityEffectStatus {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConFacilityOperations";
    private static final String STRATCON_BUNDLE = "mekhq.resources.AtBStratCon";
    private static final String MODIFIER_FILE_SUFFIX = ".json";
    private static final String LIST_SEPARATOR = ", ";

    private StratConFacilityEffectStatus() {}

    /**
     * The campaign's rules for how often facilities lend support to fights elsewhere in their sector, and whether the
     * Clans' bidding rules keep either side's support out.
     *
     * @param alliedDieSize      the 1-in-n chance a fight draws on a facility your side holds; 0 never, 1 always
     * @param enemyDieSize       the same for enemy facilities
     * @param isAlliedRestricted {@code true} if support that adds forces to your side is barred
     * @param isEnemyRestricted  {@code true} if support that adds forces to the enemy is barred
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record SupportRules(int alliedDieSize, int enemyDieSize, boolean isAlliedRestricted,
          boolean isEnemyRestricted) {
        /**
         * @param campaign the current campaign
         * @param contract the contract whose map holds the facilities
         *
         * @return the rules as they apply to the contract
         *
         * @author Illiani
         * @since 0.51.01
         */
        public static SupportRules from(Campaign campaign, AbstractContract contract) {
            CampaignOptions options = campaign.getCampaignOptions();
            boolean isBiddingEnforced = options.get(CampaignOption.CLANS_OBEY_BIDDING_RULES)
                                              && contract.isBatchallAccepted();
            return new SupportRules(options.get(CampaignOption.ALLIED_FACILITY_MODIFIER_DIE_SIZE),
                  options.get(CampaignOption.ENEMY_FACILITY_MODIFIER_DIE_SIZE),
                  isBiddingEnforced && contract.getEmployerFaction().isClan(),
                  isBiddingEnforced && contract.getEnemyFaction().isClan());
        }

        int getDieSize(boolean isAllied) {
            return isAllied ? alliedDieSize : enemyDieSize;
        }

        boolean isRestricted(boolean isAllied) {
            return isAllied ? isAlliedRestricted : isEnemyRestricted;
        }
    }

    /**
     * The shared scenario modifiers a facility can lend to a fight right now: none while it is damaged or crippled, and
     * none the Clans' bidding rules keep out. A facility with nothing here is never drawn on for support, so it cannot
     * use up a fight's draw.
     *
     * @param facility                the facility
     * @param restrictAlliedModifiers {@code true} if support that adds forces to your side is barred
     * @param restrictEnemyModifiers  {@code true} if support that adds forces to the enemy is barred
     *
     * @return the modifier IDs it can lend, in order
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<String> getLendableModifiers(StratConFacility facility, boolean restrictAlliedModifiers,
          boolean restrictEnemyModifiers) {
        List<String> sharedModifiers = facility.getSharedModifiers();
        if (!restrictAlliedModifiers && !restrictEnemyModifiers) {
            return sharedModifiers;
        }

        List<String> lendable = new ArrayList<>();
        for (String modifierId : sharedModifiers) {
            AtBScenarioModifier modifier = AtBScenarioModifier.getScenarioModifier(modifierId);
            if (!StratConRulesManager.scenarioModifierShouldBeBlocked(restrictAlliedModifiers,
                  restrictEnemyModifiers,
                  modifier)) {
                lendable.add(modifierId);
            }
        }
        return lendable;
    }

    /**
     * @param facility the facility
     * @param rules    the campaign's support rules
     *
     * @return {@code true} if a fight elsewhere in the sector could draw on the facility this week
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isReadyToLend(StratConFacility facility, SupportRules rules) {
        return facility.isAvailable()
                     && !getLendableModifiers(facility, rules.isAlliedRestricted(), rules.isEnemyRestricted()).isEmpty();
    }

    /**
     * What a facility is doing right now, effect by effect, and why any part of it is not working. An enemy facility
     * the player has not found, or has found but not scouted, gets nothing: its condition is not known, so neither is
     * what it is doing.
     *
     * @param track  the sector
     * @param coords the facility's hex
     * @param rules  the campaign's support rules
     *
     * @return the lines, most important first; empty if there is nothing the player may be told
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<String> getFacilityEffectLines(StratConTrackState track, StratConCoords coords,
          SupportRules rules) {
        List<String> lines = new ArrayList<>();
        StratConFacility facility = StratConFacilityOperations.getKnownFacility(track, coords);
        if ((facility == null) || facility.isDefinitionMissing()) {
            return lines;
        }

        boolean isAllied = facility.isOwnerAlliedToPlayer();
        FacilityIntel intel = facility.getIntel();
        if (!isAllied && !intel.isAtLeast(FacilityIntel.SCOUTED)) {
            return lines;
        }

        // The garrison is only known with detailed intel, and the defenders it brings depend on it.
        if (isAllied || intel.isAtLeast(FacilityIntel.DETAILED)) {
            addDefenderLine(facility, lines);
        }
        addSupportLines(track, facility, rules, lines);
        addNumericLines(facility, lines);
        return lines;
    }

    private static void addDefenderLine(StratConFacility facility, List<String> lines) {
        List<String> localModifiers = facility.getLocalModifiers();
        if (!localModifiers.isEmpty()) {
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "effect.defenders", describeModifiers(localModifiers)));
        } else if (facility.getGarrison() <= 0) {
            lines.add(getTextAt(RESOURCE_BUNDLE, "effect.defenders.noGarrison"));
        } else {
            lines.add(getTextAt(RESOURCE_BUNDLE, "effect.defenders.none"));
        }
    }

    private static void addSupportLines(StratConTrackState track, StratConFacility facility, SupportRules rules,
          List<String> lines) {
        List<String> profileModifiers = facility.getProfile().getSharedModifierIds();
        if (profileModifiers.isEmpty()) {
            return;
        }

        boolean isAllied = facility.isOwnerAlliedToPlayer();
        String support = describeModifiers(profileModifiers);
        String sideKey = isAllied ? "allied" : "enemy";

        if (facility.getCondition() != FacilityCondition.INTACT) {
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "effect.support.condition." + sideKey,
                  support,
                  getConditionName(facility)));
        } else if (getLendableModifiers(facility, rules.isAlliedRestricted(), rules.isEnemyRestricted()).isEmpty()) {
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "effect.support.batchall." + sideKey, support));
        } else if (rules.getDieSize(isAllied) <= 0) {
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "effect.support.disabled", support));
        } else if (!facility.isAvailable()) {
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "effect.support.spent." + sideKey, support));
        } else if (isAllied) {
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "effect.support.ready.allied",
                  support,
                  rules.getDieSize(true),
                  countReadyLenders(track, true, rules)));
        } else {
            // How many other enemy facilities are ready would give away ones the player hasn't found.
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE, "effect.support.ready.enemy", support));
        }
    }

    private static void addNumericLines(StratConFacility facility, List<String> lines) {
        StratConFacilityProfile profile = facility.getProfile();
        String condition = getConditionName(facility);

        addNumericLine("effect.scanRange", profile.getScanRangeIncrease(), facility.getScanRangeIncrease(),
              condition, lines);
        addNumericLine("effect.scenarioOdds", profile.getScenarioOddsModifier(), facility.getScenarioOddsModifier(),
              condition, lines);
        addNumericLine("effect.supportPoints", profile.getMonthlySupportPoints(), facility.getMonthlySupportPoints(),
              condition, lines);

        if (profile.isRevealingTrack()) {
            lines.add(facility.isRevealingTrack() ?
                            getTextAt(RESOURCE_BUNDLE, "effect.revealTrack") :
                            getFormattedTextAt(RESOURCE_BUNDLE, "effect.revealTrack.off", condition));
        }
        if (profile.isPreventingAerospace()) {
            lines.add(facility.isPreventingAerospace() ?
                            getTextAt(RESOURCE_BUNDLE, "effect.preventAerospace") :
                            getFormattedTextAt(RESOURCE_BUNDLE, "effect.preventAerospace.off", condition));
        }
    }

    private static void addNumericLine(String key, int fullValue, int currentValue, String condition,
          List<String> lines) {
        if ((fullValue == 0) && (currentValue == 0)) {
            return;
        }

        String line = getFormattedTextAt(RESOURCE_BUNDLE, key, formatSigned(currentValue));
        if (currentValue != fullValue) {
            line = getFormattedTextAt(RESOURCE_BUNDLE, "effect.weakened", line, formatSigned(fullValue), condition);
        }
        lines.add(line);
    }

    /**
     * What a sector's facilities add up to: the scan range and chance of fights they give it, whether they keep air
     * and space fights out or reveal it, and how much of your side's support is ready this week. Enemy facilities the
     * player hasn't found still count, as one share, so the player can tell something is at work without learning
     * where.
     *
     * @param track the sector
     * @param rules the campaign's support rules
     *
     * @return the lines; empty if the sector's facilities do nothing worth reporting
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<String> getSectorEffectLines(StratConTrackState track, SupportRules rules) {
        List<String> lines = new ArrayList<>();

        int scanRange = track.getScanRangeIncrease();
        List<String> scanSources = new ArrayList<>();
        int facilityScanRange = addSources(track, scanSources, StratConFacility::getScanRangeIncrease);
        if ((scanRange != 0) || !scanSources.isEmpty()) {
            addOtherSource(scanSources, scanRange - facilityScanRange);
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "sector.scanRange",
                  formatSigned(scanRange),
                  String.join(LIST_SEPARATOR, scanSources)));
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "sector.scanRadius",
                  Math.max(0, scanRange),
                  Math.max(0, scanRange + 1)));
        }

        int scenarioOdds = track.getScenarioOddsAdjustment();
        List<String> oddsSources = new ArrayList<>();
        int facilityOdds = addSources(track, oddsSources, StratConFacility::getScenarioOddsModifier);
        if ((scenarioOdds != 0) || !oddsSources.isEmpty()) {
            addOtherSource(oddsSources, scenarioOdds - facilityOdds);
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "sector.scenarioOdds",
                  formatSigned(scenarioOdds),
                  String.join(LIST_SEPARATOR, oddsSources)));
        }

        if (track.isAerospacePrevented()) {
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "sector.preventAerospace",
                  describeSources(track, StratConFacility::isPreventingAerospace)));
        }

        if (track.hasActiveTrackReveal()) {
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "sector.revealTrack",
                  describeSources(track, StratConFacility::isRevealingTrack)));
        }

        addSectorSupportLine(track, rules, lines);
        return lines;
    }

    private static void addSectorSupportLine(StratConTrackState track, SupportRules rules, List<String> lines) {
        int lenderCount = 0;
        for (StratConFacility facility : track.getFacilities().values()) {
            if (facility.isOwnerAlliedToPlayer() && !facility.getProfile().getSharedModifierIds().isEmpty()) {
                lenderCount++;
            }
        }
        if (lenderCount == 0) {
            return;
        }

        if (rules.alliedDieSize() <= 0) {
            lines.add(getTextAt(RESOURCE_BUNDLE, "sector.support.disabled"));
            return;
        }
        lines.add(getFormattedTextAt(RESOURCE_BUNDLE,
              "sector.support",
              countReadyLenders(track, true, rules),
              lenderCount,
              rules.alliedDieSize()));
    }

    /**
     * What facilities lent to a fight when it was set up. Fights set up before this was recorded get nothing.
     *
     * @param track    the fight's sector
     * @param scenario the fight
     *
     * @return the lines: your side's support (or that there was none), then the enemy's, if any
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<String> getScenarioSupportLines(StratConTrackState track, StratConScenario scenario) {
        List<String> lines = new ArrayList<>();
        if (!scenario.isFacilitySupportRecorded()) {
            return lines;
        }

        List<String> alliedSupport = scenario.getAlliedFacilitySupport();
        if (!alliedSupport.isEmpty()) {
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "scenario.support.allied",
                  String.join(LIST_SEPARATOR, alliedSupport)));
        } else {
            lines.add(getTextAt(RESOURCE_BUNDLE, "scenario.support.none"));
        }

        List<String> enemySources = new ArrayList<>();
        int unknownCount = 0;
        for (StratConCoords coords : scenario.getHostileFacilitySupport()) {
            StratConFacility facility = StratConFacilityOperations.getKnownFacility(track, coords);
            if (facility == null) {
                unknownCount++;
            } else {
                enemySources.add(facility.getDisplayableName());
            }
        }
        if (unknownCount > 0) {
            enemySources.add(getFormattedTextAt(RESOURCE_BUNDLE, "scenario.support.enemy.unknown", unknownCount));
        }
        if (!enemySources.isEmpty()) {
            lines.add(getFormattedTextAt(RESOURCE_BUNDLE,
                  "scenario.support.enemy",
                  String.join(LIST_SEPARATOR, enemySources)));
        }
        return lines;
    }

    private static int countReadyLenders(StratConTrackState track, boolean isAllied, SupportRules rules) {
        int count = 0;
        for (StratConFacility facility : track.getFacilities().values()) {
            if ((facility.isOwnerAlliedToPlayer() == isAllied) && isReadyToLend(facility, rules)) {
                count++;
            }
        }
        return count;
    }

    private interface FacilityValue {
        int get(StratConFacility facility);
    }

    private interface FacilityFlag {
        boolean test(StratConFacility facility);
    }

    /**
     * Names every facility giving the sector a non-zero value, with that value; the share from facilities the player
     * hasn't found is added up into one entry.
     *
     * @return the total from every facility, found or not
     */
    private static int addSources(StratConTrackState track, List<String> sources, FacilityValue value) {
        int total = 0;
        int unknownTotal = 0;
        for (Map.Entry<StratConCoords, StratConFacility> entry : track.getFacilities().entrySet()) {
            StratConFacility facility = entry.getValue();
            int facilityValue = value.get(facility);
            if (facilityValue == 0) {
                continue;
            }

            total += facilityValue;
            if (StratConFacilityOperations.isKnownToPlayer(track, facility)) {
                sources.add(getFormattedTextAt(RESOURCE_BUNDLE,
                      "sector.source",
                      facility.getDisplayableName(),
                      formatSigned(facilityValue)));
            } else {
                unknownTotal += facilityValue;
            }
        }

        if (unknownTotal != 0) {
            sources.add(getFormattedTextAt(RESOURCE_BUNDLE, "sector.source.unknown", formatSigned(unknownTotal)));
        }
        return total;
    }

    private static void addOtherSource(List<String> sources, int otherValue) {
        if (otherValue != 0) {
            sources.add(getFormattedTextAt(RESOURCE_BUNDLE, "sector.source.other", formatSigned(otherValue)));
        }
    }

    private static String describeSources(StratConTrackState track, FacilityFlag flag) {
        List<String> sources = new ArrayList<>();
        boolean hasUnknownSource = false;
        for (StratConFacility facility : track.getFacilities().values()) {
            if (!flag.test(facility)) {
                continue;
            }
            if (StratConFacilityOperations.isKnownToPlayer(track, facility)) {
                sources.add(facility.getDisplayableName());
            } else {
                hasUnknownSource = true;
            }
        }
        if (hasUnknownSource) {
            sources.add(getTextAt(RESOURCE_BUNDLE, "sector.source.unknownFacility"));
        }
        return String.join(LIST_SEPARATOR, sources);
    }

    /**
     * @param modifierIds scenario modifier IDs, such as {@code AlliedAirSupport.json}
     *
     * @return the modifiers as readable names, such as {@code Allied Air Support}, without repeats
     */
    static String describeModifiers(List<String> modifierIds) {
        List<String> names = new ArrayList<>();
        for (String modifierId : modifierIds) {
            String name = describeModifier(modifierId);
            if (!names.contains(name)) {
                names.add(name);
            }
        }
        return String.join(LIST_SEPARATOR, names);
    }

    /**
     * @param modifierId a scenario modifier ID, such as {@code AlliedAirSupport.json}
     *
     * @return the ID as words, such as {@code Allied Air Support}
     */
    static String describeModifier(String modifierId) {
        String name = modifierId.endsWith(MODIFIER_FILE_SUFFIX) ?
                            modifierId.substring(0, modifierId.length() - MODIFIER_FILE_SUFFIX.length()) :
                            modifierId;
        StringBuilder words = new StringBuilder();
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            boolean isWordStart = (index > 0)
                                        && Character.isUpperCase(character)
                                        && Character.isLowerCase(name.charAt(index - 1));
            if (isWordStart) {
                words.append(' ');
            }
            words.append(character);
        }
        return words.toString();
    }

    private static String getConditionName(StratConFacility facility) {
        return getTextAt(STRATCON_BUNDLE, "stratConTab.facilityCondition." + facility.getCondition().name())
                     .toLowerCase();
    }

    private static String formatSigned(int value) {
        return String.format("%+d", value);
    }
}
