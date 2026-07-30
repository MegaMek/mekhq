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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.options.OptionsConstants;
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
 * <p>Every Manei Domini receives implants; how many and how advanced depends on their rank
 * (<i>Jihad Hot Spots: 3072</i>, pp. 121, 123-124, Rules Annex: Manei Domini Classes / Manei Domini
 * Nomenclature). This applies that availability chart to a freshly generated command.</p>
 *
 * <p>Only the implants MegaMek actually models are issued. The source lists several augmentations the
 * game has no equivalent for - cosmetic enhancements, a secondary power supply, the separate recorder,
 * receiver and transmitter units - and issuing those would mean recording implants that do nothing in
 * play, so they are skipped and the count reflects what a warrior actually fields. Where the source
 * splits an implant more finely than the game does, the game's coarser option stands in: one
 * multi-modal sensory implant covers the source's separate eyes, ears and speech.</p>
 */
public final class ManeiDominiAugmentor {

    private static final MMLogger LOGGER = MMLogger.create(ManeiDominiAugmentor.class);

    /** The RAT Generator faction key for the Word of Blake Shadow Divisions. */
    public static final String SHADOW_DIVISION_FACTION_KEY = "WOB.SD";

    /**
     * Implant allowance for one Manei Domini rank, from the availability chart.
     *
     * @param minimumImplants the fewest implants a warrior of this rank carries
     * @param maximumImplants the most implants a warrior of this rank carries
     * @param maximumLevel    the highest implant level this rank may be issued
     */
    private record ImplantAllowance(int minimumImplants, int maximumImplants, int maximumLevel) {}

    private static final Map<ManeiDominiRank, ImplantAllowance> ALLOWANCE_BY_RANK =
          new EnumMap<>(Map.of(
                ManeiDominiRank.ALPHA, new ImplantAllowance(2, 3, 2),
                ManeiDominiRank.BETA, new ImplantAllowance(3, 4, 2),
                ManeiDominiRank.OMEGA, new ImplantAllowance(3, 4, 3),
                ManeiDominiRank.TAU, new ImplantAllowance(3, 5, 4),
                ManeiDominiRank.DELTA, new ImplantAllowance(4, 7, 4),
                ManeiDominiRank.SIGMA, new ImplantAllowance(4, 8, 4),
                ManeiDominiRank.OMICRON, new ImplantAllowance(6, 10, 5)));

    /**
     * Who an implant actually does something for, according to the effect MegaMek gives it.
     *
     * <p>Most of the catalogue is explicitly conventional-infantry-only: the effusers, the sensory and
     * optical implants, the enhanced prosthetics and prosthetic leg MASC all say so in their own
     * descriptions, and dermal armour and the triple-strength myomer implant are read only by the
     * infantry and BattleArmor calculators. The neural interfaces are the reverse, being what lets a
     * warrior drive the unit they are sitting in.</p>
     */
    private enum ImplantAudience {
        /** Does something only for a warrior fighting on foot. */
        ON_FOOT,
        /** Does something only for a warrior piloting a unit. */
        PILOTING,
        /** Useful whoever carries it. */
        ANYONE;

        boolean servesA(boolean warriorFightsOnFoot) {
            return (this == ANYONE) || (warriorFightsOnFoot ? (this == ON_FOOT) : (this == PILOTING));
        }
    }

    /**
     * One issuable implant: the game options it may be satisfied by, the level it sits at, and who it
     * benefits.
     *
     * <p>Most entries name a single option. The source's "Cybernetic Eye Implants" is one entry that
     * MegaMek splits into three optical implants, so that entry carries all three and one is rolled -
     * a Level III fields a mix of optics rather than every warrior carrying identical eyes.</p>
     *
     * @param level         the implant level, which the warrior's rank caps
     * @param audience      who this implant actually does something for
     * @param optionChoices the game options this entry may be satisfied by; one is chosen at random
     */
    private record ImplantEntry(int level, ImplantAudience audience, List<String> optionChoices) {

        private ImplantEntry(int level, ImplantAudience audience, String singleOption) {
            this(level, audience, List.of(singleOption));
        }
    }

    /**
     * The issuable catalogue, in source order. Level 0 contributes nothing: both of its entries
     * (cosmetic enhancements, and type 4 and 5 prosthetic limbs) are among those MegaMek does not
     * model.
     */
    private static final List<ImplantEntry> CATALOGUE = List.of(
          new ImplantEntry(1, ImplantAudience.ON_FOOT, OptionsConstants.MD_PL_ENHANCED),
          new ImplantEntry(2, ImplantAudience.ANYONE, OptionsConstants.MD_PAIN_SHUNT),
          new ImplantEntry(2, ImplantAudience.ON_FOOT, OptionsConstants.MD_CYBER_IMP_AUDIO),
          new ImplantEntry(2, ImplantAudience.ON_FOOT, List.of(OptionsConstants.MD_CYBER_IMP_VISUAL,
                OptionsConstants.MD_CYBER_IMP_LASER,
                OptionsConstants.MD_CYBER_IMP_TELE)),
          new ImplantEntry(2, ImplantAudience.ANYONE, OptionsConstants.MD_COMM_IMPLANT),
          new ImplantEntry(3, ImplantAudience.ON_FOOT, OptionsConstants.MD_PL_I_ENHANCED),
          new ImplantEntry(3, ImplantAudience.ON_FOOT, OptionsConstants.MD_PL_MASC),
          new ImplantEntry(3, ImplantAudience.ON_FOOT, OptionsConstants.MD_GAS_EFFUSER_PHEROMONE),
          new ImplantEntry(3, ImplantAudience.PILOTING, OptionsConstants.MD_VDNI),
          new ImplantEntry(3, ImplantAudience.ANYONE, OptionsConstants.MD_BOOST_COMM_IMPLANT),
          new ImplantEntry(3, ImplantAudience.ANYONE, OptionsConstants.MD_MM_IMPLANTS),
          new ImplantEntry(4, ImplantAudience.ON_FOOT, OptionsConstants.MD_GAS_EFFUSER_TOXIN),
          new ImplantEntry(4, ImplantAudience.ON_FOOT, OptionsConstants.MD_DERMAL_ARMOR),
          new ImplantEntry(4, ImplantAudience.ON_FOOT, OptionsConstants.MD_TSM_IMPLANT),
          new ImplantEntry(5, ImplantAudience.ANYONE, OptionsConstants.MD_ENH_MM_IMPLANTS),
          new ImplantEntry(5, ImplantAudience.PILOTING, OptionsConstants.MD_BVDNI));

    /**
     * Implants that supersede a lesser version of themselves. Holding both is meaningless, so taking
     * the improved one rules the basic one out and vice versa.
     */
    private static final Map<String, String> SUPERSEDED_BY = Map.of(
          OptionsConstants.MD_PL_ENHANCED, OptionsConstants.MD_PL_I_ENHANCED,
          OptionsConstants.MD_COMM_IMPLANT, OptionsConstants.MD_BOOST_COMM_IMPLANT,
          OptionsConstants.MD_MM_IMPLANTS, OptionsConstants.MD_ENH_MM_IMPLANTS,
          OptionsConstants.MD_VDNI, OptionsConstants.MD_BVDNI);

    /** Multi-modal sensory implants that a non-infantry warrior cannot use without a neural interface. */
    private static final List<String> REQUIRE_NEURAL_INTERFACE = List.of(
          OptionsConstants.MD_MM_IMPLANTS, OptionsConstants.MD_ENH_MM_IMPLANTS);

    /** The neural interfaces that satisfy that requirement. */
    private static final List<String> NEURAL_INTERFACES = List.of(
          OptionsConstants.MD_VDNI, OptionsConstants.MD_BVDNI);

    private ManeiDominiAugmentor() {
    }

    /**
     * Fits every generated warrior with Manei Domini rank, class and implants, where the command being
     * generated is a Shadow Division and the campaign has implants switched on.
     *
     * @param campaign         the campaign the command is being generated into
     * @param generationFaction the RAT Generator faction key the command was generated from, or
     *                          {@code null} if none was recorded
     * @param generatedPersons  every person this generation created
     */
    public static void augment(Campaign campaign, @Nullable String generationFaction,
          List<Person> generatedPersons) {
        if (!isShadowDivision(generationFaction)) {
            LOGGER.debug("[ManeiDomini] skipped - faction '{}' is not the Shadow Divisions ({})",
                  generationFaction, SHADOW_DIVISION_FACTION_KEY);
            return;
        }
        if (!campaign.getCampaignOptions().get(CampaignOption.USE_IMPLANTS)) {
            LOGGER.debug("[ManeiDomini] skipped - the campaign has implants switched off");
            return;
        }
        // The campaign option above is MekHQ's mirror of MegaMek's Manei Domini rule, and the two are
        // only synced when the options dialog is used. The game option is what actually decides
        // whether implants survive into a battle: with it off, ChatLounge clears the whole implant
        // group as the unit enters the lobby and MULParser refuses to restore it. Issuing implants
        // then would leave the roster claiming augmentations that silently vanish in play.
        if (!campaign.getGameOptions().booleanOption(OptionsConstants.RPG_MANEI_DOMINI)) {
            LOGGER.debug("[ManeiDomini] skipped - MegaMek's Manei Domini rule is switched off, so any"
                        + " implants issued would be stripped when the unit reaches the lobby");
            return;
        }

        int augmented = 0;
        int implantsIssued = 0;
        for (Person person : generatedPersons) {
            if (person == null) {
                continue;
            }
            ManeiDominiRank maneiDominiRank = rankFor(person);
            person.setManeiDominiRank(maneiDominiRank);
            person.setManeiDominiClass(classFor(person));
            implantsIssued += issueImplants(person, maneiDominiRank);
            augmented++;
        }
        LOGGER.info("[ManeiDomini] augmented {} of {} generated person(s) with {} implant(s) in total",
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
     * Issues one warrior's implants, and returns how many they received.
     *
     * <p>The explosive charge every Manei Domini implant carries is fitted separately and does not
     * count against the allowance: the source describes it as a property of their augmentation rather
     * than an implant they were issued in its place.</p>
     */
    private static int issueImplants(Person person, ManeiDominiRank maneiDominiRank) {
        PersonnelOptions options = person.getOptions();
        options.acquireAbility(PersonnelOptions.MD_ADVANTAGES, OptionsConstants.MD_SUICIDE_IMPLANTS, true);

        boolean warriorFightsOnFoot = fightsOnFoot(person);
        List<String> issued = selectImplants(maneiDominiRank, warriorFightsOnFoot);
        for (String option : issued) {
            options.acquireAbility(PersonnelOptions.MD_ADVANTAGES, option, true);
        }
        LOGGER.debug("[ManeiDomini] {}: rank {}, fights {} -> {} implant(s) {}",
              person.getFullName(), maneiDominiRank,
              warriorFightsOnFoot ? "on foot" : "from a cockpit", issued.size(), issued);
        return issued.size();
    }

    /**
     * Chooses the implants a warrior of the given rank receives, without fitting them to anyone.
     *
     * <p>Separate from {@link #issueImplants} so the availability rules can be exercised on their own:
     * the counts, the level ceiling, the superseded pairs and the neural interface requirement are all
     * decided here.</p>
     *
     * @param maneiDominiRank the rank whose allowance governs the selection
     *
     * @return the game options to fit, excluding the explosive charge every Manei Domini receives
     */
    static List<String> selectImplants(ManeiDominiRank maneiDominiRank, boolean warriorFightsOnFoot) {
        ImplantAllowance allowance = ALLOWANCE_BY_RANK.get(maneiDominiRank);
        if (allowance == null) {
            LOGGER.warn("[ManeiDomini] no implant allowance for rank {}; issuing none", maneiDominiRank);
            return List.of();
        }

        List<ImplantEntry> withinLevel = CATALOGUE.stream()
                                               .filter(entry -> entry.level() <= allowance.maximumLevel())
                                               .toList();
        List<ImplantEntry> useful = new ArrayList<>(withinLevel.stream()
              .filter(entry -> entry.audience().servesA(warriorFightsOnFoot))
              .toList());
        // Kept only to make up the numbers. A MekWarrior has just two implants that do anything for
        // them at the level 2 ceiling, so drawing strictly from the useful ones would leave the junior
        // ranks short of the minimum the chart states they carry.
        List<ImplantEntry> remainder = new ArrayList<>(withinLevel.stream()
              .filter(entry -> !entry.audience().servesA(warriorFightsOnFoot))
              .toList());

        int target = randomBetween(allowance.minimumImplants(), allowance.maximumImplants());
        List<String> issued = new ArrayList<>();
        // Guarantee the first one is useful, so nobody comes out carrying nothing but implants that
        // do nothing for the way they fight.
        drawInto(issued, useful, 1);
        if (issued.isEmpty()) {
            LOGGER.warn("[ManeiDomini] rank {} has no implant useful to a warrior who fights {}",
                  maneiDominiRank, warriorFightsOnFoot ? "on foot" : "from a cockpit");
        }
        drawInto(issued, useful, target);
        drawInto(issued, remainder, target);

        ensureNeuralInterface(issued, allowance, warriorFightsOnFoot);
        return issued;
    }

    /**
     * Draws at random from {@code available} until the issued list reaches {@code target}, skipping
     * anything ruled out by an implant already issued. Drawn entries are removed, so a later call
     * cannot re-offer them.
     */
    private static void drawInto(List<String> issued, List<ImplantEntry> available, int target) {
        while ((issued.size() < target) && !available.isEmpty()) {
            ImplantEntry entry = available.remove((int) (Math.random() * available.size()));
            String option = entry.optionChoices()
                                  .get((int) (Math.random() * entry.optionChoices().size()));
            if (isRuledOutBySupersession(option, issued)) {
                continue;
            }
            issued.add(option);
        }
    }

    /**
     * Whether this warrior fights with their own body rather than from inside a unit, which decides
     * which implants do anything for them. BattleArmor counts: the infantry calculators read dermal
     * armour and the myomer implants for them the same way they do for a foot platoon.
     *
     * @param person the warrior being augmented
     *
     * @return {@code true} if their implants act on them directly rather than through a unit
     */
    static boolean fightsOnFoot(Person person) {
        PersonnelRole role = person.getPrimaryRole();
        return role.isSoldier() || role.isBattleArmour();
    }

    /**
     * @param option             a game option from the catalogue
     * @param warriorFightsOnFoot whether the warrior fights with their own body rather than a unit
     *
     * @return {@code true} if this implant does something for such a warrior
     */
    static boolean servesWarrior(String option, boolean warriorFightsOnFoot) {
        return CATALOGUE.stream()
                     .filter(entry -> entry.optionChoices().contains(option))
                     .anyMatch(entry -> entry.audience().servesA(warriorFightsOnFoot));
    }

    /**
     * @return the implant level of the given game option, or 0 if it is not part of the catalogue
     */
    static int levelOf(String option) {
        return CATALOGUE.stream()
                     .filter(entry -> entry.optionChoices().contains(option))
                     .mapToInt(ImplantEntry::level)
                     .findFirst()
                     .orElse(0);
    }

    /**
     * @return the fewest and most implants the given rank may carry, or {@code null} if unknown
     */
    static @Nullable int[] allowanceFor(ManeiDominiRank maneiDominiRank) {
        ImplantAllowance allowance = ALLOWANCE_BY_RANK.get(maneiDominiRank);
        return (allowance == null) ? null
              : new int[] { allowance.minimumImplants(), allowance.maximumImplants(),
                            allowance.maximumLevel() };
    }

    /**
     * @return {@code true} if this option is the lesser or greater half of a pair already issued
     */
    private static boolean isRuledOutBySupersession(String option, List<String> issued) {
        String supersedes = SUPERSEDED_BY.get(option);
        if ((supersedes != null) && issued.contains(supersedes)) {
            return true;
        }
        return SUPERSEDED_BY.entrySet()
                     .stream()
                     .anyMatch(pair -> pair.getValue().equals(option) && issued.contains(pair.getKey()));
    }

    /**
     * Multi-modal sensory implants only sync with a vehicle's sensors through a neural interface, so a
     * warrior issued one without the other would carry an implant that does nothing.
     *
     * <p>The interface takes the place of another implant rather than being added on top: the rank's
     * maximum is what the source allows the warrior to carry, and going past it to satisfy a
     * prerequisite would be inventing an allowance the chart does not grant. Where there is nothing
     * else to give up, the multi-modal implant itself is what goes - better a working interface than a
     * sensory implant with nothing to plug into.</p>
     */
    private static void ensureNeuralInterface(List<String> issued, ImplantAllowance allowance,
          boolean warriorFightsOnFoot) {
        // Only non-infantry need the interface: a warrior on foot carries the sensors on their own
        // body, so a multi-modal implant works for them with nothing to sync it to. Fitting one anyway
        // would spend a slot of their allowance on an implant that does nothing for them.
        if (warriorFightsOnFoot) {
            return;
        }
        boolean needsInterface = issued.stream().anyMatch(REQUIRE_NEURAL_INTERFACE::contains);
        boolean hasInterface = issued.stream().anyMatch(NEURAL_INTERFACES::contains);
        if (!needsInterface || hasInterface) {
            return;
        }
        // Buffered is level 5; fall back to the plain interface when the rank cannot reach it.
        String neuralInterface = (allowance.maximumLevel() >= 5)
              ? OptionsConstants.MD_BVDNI
              : OptionsConstants.MD_VDNI;

        String surrendered = issued.stream()
                                   .filter(option -> !REQUIRE_NEURAL_INTERFACE.contains(option))
                                   .findFirst()
                                   .orElseGet(() -> issued.getFirst());
        issued.remove(surrendered);
        issued.add(neuralInterface);
    }

    /**
     * Derives a warrior's Manei Domini rank from the rank they already hold, so that a commander is
     * never out-ranked in augmentation by the warriors under them - which rolling independently of
     * their standing would allow.
     */
    private static ManeiDominiRank rankFor(Person person) {
        int rankIndex = Math.max(0, person.getRankNumeric());
        int rankCount = Math.max(1, person.getRankSystem().getRanks().size() - 1);
        ManeiDominiRank[] byStanding = { ManeiDominiRank.ALPHA, ManeiDominiRank.BETA,
                                         ManeiDominiRank.OMEGA, ManeiDominiRank.TAU,
                                         ManeiDominiRank.DELTA, ManeiDominiRank.SIGMA,
                                         ManeiDominiRank.OMICRON };
        int band = (rankIndex * byStanding.length) / rankCount;
        return byStanding[Math.min(band, byStanding.length - 1)];
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

    /**
     * @return a value between {@code minimum} and {@code maximum}, both inclusive
     */
    private static int randomBetween(int minimum, int maximum) {
        return minimum + (int) (Math.random() * ((maximum - minimum) + 1));
    }
}
