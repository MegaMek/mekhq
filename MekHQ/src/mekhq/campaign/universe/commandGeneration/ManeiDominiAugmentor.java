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
package mekhq.campaign.universe.commandGeneration;

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.enums.AugmentedUnitType;
import megamek.common.enums.ManeiDominiAugmentationRank;
import megamek.common.enums.ManeiDominiImplants;
import megamek.common.enums.NeuralInterfaceMode;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.ManeiDominiClass;
import mekhq.campaign.personnel.enums.ManeiDominiRank;
import mekhq.campaign.personnel.enums.PersonnelRole;

/**
 * Fits generated Word of Blake Shadow Division warriors with Manei Domini rank, class and cybernetics.
 *
 * <p>The availability rules themselves live in {@link ManeiDominiImplants}, so MegaMek owns them and
 * anything generating a Manei Domini can apply the same chart. What stays here is what only a campaign
 * has: the campaign's own switches, a {@link Person} to fit implants to, and the campaign rank a
 * warrior's Manei Domini standing is derived from.</p>
 *
 * @see ManeiDominiImplants
 */
public final class ManeiDominiAugmentor {

    private static final MMLogger LOGGER = MMLogger.create(ManeiDominiAugmentor.class);

    /** The RAT Generator faction key for the Word of Blake Shadow Divisions. */
    public static final String SHADOW_DIVISION_FACTION_KEY = "WOB.SD";

    private ManeiDominiAugmentor() {
    }

    /**
     * Fits every generated warrior with Manei Domini rank, class and implants, where the command being
     * generated is a Shadow Division and the campaign has implants switched on.
     *
     * @param campaign          the campaign the command is being generated into
     * @param generationFaction the RAT Generator faction key the command was generated from, or
     *                          {@code null} if none was recorded
     * @param generatedPersons  every person this generation created
     */
    public static void augment(Campaign campaign, @Nullable String generationFaction,
          List<Person> generatedPersons) {
        boolean campaignAllowsImplants = campaign.getCampaignOptions().get(CampaignOption.USE_IMPLANTS);
        boolean gameAllowsImplants = NeuralInterfaceMode.from(campaign.getGameOptions()).allowsImplants();
        // One line that answers "did this even get a chance to run", so a playtest never has to infer
        // it from the absence of later lines. Logged before any gate so it appears whatever happens.
        LOGGER.info("[ManeiDomini] ENTER: generationFaction='{}' (Shadow Divisions key '{}'),"
                    + " campaign Use Implants={}, MegaMek Pilot Implants option allows implants={},"
                    + " generatedPersons={}",
              generationFaction, SHADOW_DIVISION_FACTION_KEY, campaignAllowsImplants,
              gameAllowsImplants, generatedPersons.size());

        if (!isShadowDivision(generationFaction)) {
            LOGGER.info("[ManeiDomini] SKIPPED - the command was generated for faction '{}', not the"
                        + " Word of Blake Shadow Divisions ('{}'). Only a Shadow Division is Manei"
                        + " Domini. Pick Word of Blake in MekHQ's Command Generator, then Shadow"
                        + " Divisions in the sub-faction picker beneath it. Note this runs only in"
                        + " MekHQ's Command Generator - MegaMek's own Force Generator builds crews"
                        + " directly and has no MekHQ personnel to augment.",
                  generationFaction, SHADOW_DIVISION_FACTION_KEY);
            return;
        }
        if (!campaignAllowsImplants) {
            LOGGER.info("[ManeiDomini] SKIPPED - a Shadow Division was generated but the campaign has"
                        + " implants switched off. Turn on Campaign Options > Personnel > General >"
                        + " Use Implants and generate again; this cannot be applied retrospectively.");
            return;
        }
        // The campaign option above is MekHQ's mirror of MegaMek's pilot implants option, and the two
        // are only synced when the options dialog is used. The game option is what actually decides
        // whether implants survive into a battle: with it Off, ChatLounge clears the whole implant
        // group as the unit enters the lobby and MULParser refuses to restore it. Issuing implants
        // then would leave the roster claiming augmentations that silently vanish in play.
        if (!gameAllowsImplants) {
            LOGGER.info("[ManeiDomini] SKIPPED - a Shadow Division was generated with campaign implants"
                        + " on, but MegaMek's Pilot Implants option is Off in this campaign's game options,"
                        + " so any implants issued would be stripped the moment a unit reached the"
                        + " lobby. Re-open Campaign Options and accept it to push the rule across.");
            return;
        }

        int augmented = 0;
        int implantsIssued = 0;
        // Logging one line per warrior is deliberate: a generated command is a bounded list of a few
        // dozen people, and the whole point of this trail is to answer "why did this one get that"
        // without a debugger.
        for (Person person : generatedPersons) {
            if (person == null) {
                LOGGER.warn("[ManeiDomini] a null person was in the generated list; skipped");
                continue;
            }
            ManeiDominiAugmentationRank augmentationRank = rankFor(person);
            person.setManeiDominiRank(toCampaignRank(augmentationRank));
            person.setManeiDominiClass(classFor(person));
            implantsIssued += issueImplants(person, augmentationRank, generationFaction);
            // Read the values back rather than trusting the setters, and show the rank name the
            // roster will actually display. If the rank system is not flagged for Manei Domini the
            // name comes out plain, which is the difference between "not assigned" and "assigned but
            // not shown" - the two failures look identical in the UI.
            LOGGER.info("[ManeiDomini]   '{}' -> class={} rank={} | rankSystem={} usesManeiDomini={}"
                        + " | displayed as '{}'",
                  person.getFullName(), person.getManeiDominiClass(), person.getManeiDominiRank(),
                  person.getRankSystem() == null ? "null" : person.getRankSystem().getCode(),
                  person.getRankSystem() != null && person.getRankSystem().isUseManeiDomini(),
                  person.getRankName());
            augmented++;
        }
        LOGGER.info("[ManeiDomini] DONE: augmented {} of {} generated person(s), {} implant(s) issued"
                    + " in total (plus one explosive charge each)",
              augmented, generatedPersons.size(), implantsIssued);
    }

    /**
     * @param generationFaction the RAT Generator faction key the command was generated from
     *
     * @return {@code true} if this command is a Word of Blake Shadow Division
     */
    public static boolean isShadowDivision(@Nullable String generationFaction) {
        return SHADOW_DIVISION_FACTION_KEY.equalsIgnoreCase(generationFaction);
    }

    /**
     * Bridges MegaMek's augmentation rank to the campaign's own, which is what a {@link Person} carries
     * and what campaign files record. The two sets of constants are deliberately named alike.
     *
     * @param augmentationRank the rank the shared chart is indexed by
     *
     * @return the campaign rank of the same name, or {@link ManeiDominiRank#NONE} if the campaign does
     *       not know it
     */
    static ManeiDominiRank toCampaignRank(ManeiDominiAugmentationRank augmentationRank) {
        for (ManeiDominiRank campaignRank : ManeiDominiRank.values()) {
            if (campaignRank.name().equalsIgnoreCase(augmentationRank.name())) {
                return campaignRank;
            }
        }
        LOGGER.warn("[ManeiDomini] no campaign rank matches '{}'; leaving the warrior unranked",
              augmentationRank);
        return ManeiDominiRank.NONE;
    }

    /**
     * Issues one warrior's implants, and returns how many they received.
     *
     * <p>The explosive charge every Manei Domini implant carries is fitted separately and does not
     * count against the allowance.</p>
     */
    private static int issueImplants(Person person, ManeiDominiAugmentationRank augmentationRank,
          String factionCode) {
        AugmentedUnitType unitType = unitTypeFor(person);
        // Selected and fitted by the shared code, so a warrior raised in MegaMek's generator is
        // augmented exactly as one raised here. A campaign's PersonnelOptions is a PilotOptions, which
        // is what lets the one method serve both.
        List<String> issued = ManeiDominiImplants.fitTo(person.getOptions(), augmentationRank,
              unitType, factionCode);

        // Read every implant back off the person. Setting an option the group does not carry does
        // nothing, so a typo or a renamed constant would otherwise leave the log claiming implants the
        // warrior does not actually carry.
        List<String> confirmed = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        for (String option : issued) {
            if (person.getOptions().booleanOption(option)) {
                confirmed.add(option);
            } else {
                failed.add(option);
            }
        }
        boolean chargeFitted =
              person.getOptions().booleanOption(ManeiDominiImplants.getExplosiveCharge());
        LOGGER.info("[ManeiDomini]   '{}': role={} unitType={} rank={} -> chose {} implant(s) {}",
              person.getFullName(), person.getPrimaryRole(),
              unitType, augmentationRank,
              issued.size(), issued);
        LOGGER.info("[ManeiDomini]     confirmed on the person: {} of {} {}; explosive charge fitted={}",
              confirmed.size(), issued.size(), confirmed, chargeFitted);
        if (!failed.isEmpty()) {
            LOGGER.error("[ManeiDomini]     these implants did NOT take on '{}': {} - the option name"
                        + " is probably not in the {} group",
                  person.getFullName(), failed, PersonnelOptions.MD_ADVANTAGES);
        }
        return issued.size();
    }

    /**
     * The unit type the construction rules read this warrior as, which decides both what they may
     * carry and what does anything for them. Battle armour is its own category rather than infantry:
     * a battle armour trooper may carry a neural interface where a foot trooper may not.
     *
     * @param person the warrior being augmented
     *
     * @return the unit type their augmentations are read against
     */
    static AugmentedUnitType unitTypeFor(Person person) {
        PersonnelRole role = person.getPrimaryRole();
        if (role.isBattleArmour()) {
            return AugmentedUnitType.BATTLE_ARMOR;
        }
        if (role.isSoldier()) {
            return AugmentedUnitType.CONVENTIONAL_INFANTRY;
        }
        if (role.isMekWarrior()) {
            return AugmentedUnitType.BATTLE_MEK;
        }
        if (role.isAerospacePilot()) {
            return AugmentedUnitType.AEROSPACE_FIGHTER;
        }
        if (role.isConventionalAircraftPilot()) {
            return AugmentedUnitType.CONVENTIONAL_FIGHTER;
        }
        if (role.isVehicleCrewMember() || role.isGroundVehicleCrew() || role.isVTOLCrew()) {
            return AugmentedUnitType.COMBAT_VEHICLE;
        }
        return AugmentedUnitType.OTHER;
    }

    /**
     * Derives a warrior's Manei Domini rank from the rank they already hold, so a commander is never
     * out-augmented by the warriors under them - which rolling independently of their standing would
     * allow.
     */
    private static ManeiDominiAugmentationRank rankFor(Person person) {
        int rankIndex = person.getRankNumeric();
        int highestRankIndex = person.getRankSystem().getRanks().size() - 1;
        // Shared with MegaMek's generator so a Shadow Division raised in either comes out the same.
        ManeiDominiAugmentationRank derived =
              ManeiDominiAugmentationRank.forRankIndex(rankIndex, highestRankIndex);
        LOGGER.debug("[ManeiDomini]     rank derivation for '{}': militaryRank='{}' index={} of {} -> {}",
              person.getFullName(), person.getRankName(), rankIndex, highestRankIndex, derived);
        return derived;
    }

    /**
     * Assigns the Manei Domini class that matches what the warrior does, the source tying availability
     * to job as well as rank. The class is half of a Manei Domini's name - a rank system flagged for
     * them renders "Ghost Adept Omicron" - so leaving it unset would read wrongly on the roster.
     */
    private static ManeiDominiClass classFor(Person person) {
        PersonnelRole role = person.getPrimaryRole();
        if (role.isMekWarrior()) {
            return ManeiDominiClass.WRAITH;
        }
        if (role.isAerospacePilot() || role.isConventionalAircraftPilot()) {
            return ManeiDominiClass.BANSHEE;
        }
        if (role.isVehicleCrewMember() || role.isGroundVehicleCrew() || role.isVTOLCrew()) {
            return ManeiDominiClass.ZOMBIE;
        }
        if (role.isBattleArmour() || role.isSoldier()) {
            return ManeiDominiClass.GHOST;
        }
        return ManeiDominiClass.SPECTER;
    }
}
