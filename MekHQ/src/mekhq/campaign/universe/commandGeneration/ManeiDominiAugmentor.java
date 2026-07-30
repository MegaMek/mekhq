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
     * One issuable implant: the game options it may be satisfied by, and the level it sits at.
     *
     * <p>Most entries name a single option. The source's "Cybernetic Eye Implants" is one entry that
     * MegaMek splits into three optical implants, so that entry carries all three and one is rolled -
     * a Level III fields a mix of optics rather than every warrior carrying identical eyes.</p>
     *
     * @param level        the implant level, which the warrior's rank caps
     * @param optionChoices the game options this entry may be satisfied by; one is chosen at random
     */
    private record ImplantEntry(int level, List<String> optionChoices) {

        private ImplantEntry(int level, String singleOption) {
            this(level, List.of(singleOption));
        }
    }

    /**
     * The issuable catalogue, in source order. Level 0 contributes nothing: both of its entries
     * (cosmetic enhancements, and type 4 and 5 prosthetic limbs) are among those MegaMek does not
     * model.
     */
    private static final List<ImplantEntry> CATALOGUE = List.of(
          new ImplantEntry(1, OptionsConstants.MD_PL_ENHANCED),
          new ImplantEntry(2, OptionsConstants.MD_PAIN_SHUNT),
          new ImplantEntry(2, OptionsConstants.MD_CYBER_IMP_AUDIO),
          new ImplantEntry(2, List.of(OptionsConstants.MD_CYBER_IMP_VISUAL,
                OptionsConstants.MD_CYBER_IMP_LASER,
                OptionsConstants.MD_CYBER_IMP_TELE)),
          new ImplantEntry(2, OptionsConstants.MD_COMM_IMPLANT),
          new ImplantEntry(3, OptionsConstants.MD_PL_I_ENHANCED),
          new ImplantEntry(3, OptionsConstants.MD_PL_MASC),
          new ImplantEntry(3, OptionsConstants.MD_GAS_EFFUSER_PHEROMONE),
          new ImplantEntry(3, OptionsConstants.MD_VDNI),
          new ImplantEntry(3, OptionsConstants.MD_BOOST_COMM_IMPLANT),
          new ImplantEntry(3, OptionsConstants.MD_MM_IMPLANTS),
          new ImplantEntry(4, OptionsConstants.MD_GAS_EFFUSER_TOXIN),
          new ImplantEntry(4, OptionsConstants.MD_DERMAL_ARMOR),
          new ImplantEntry(4, OptionsConstants.MD_TSM_IMPLANT),
          new ImplantEntry(5, OptionsConstants.MD_ENH_MM_IMPLANTS),
          new ImplantEntry(5, OptionsConstants.MD_BVDNI));

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

        List<String> issued = selectImplants(maneiDominiRank);
        for (String option : issued) {
            options.acquireAbility(PersonnelOptions.MD_ADVANTAGES, option, true);
        }
        LOGGER.debug("[ManeiDomini] {}: rank {} -> {} implant(s) {}",
              person.getFullName(), maneiDominiRank, issued.size(), issued);
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
    static List<String> selectImplants(ManeiDominiRank maneiDominiRank) {
        ImplantAllowance allowance = ALLOWANCE_BY_RANK.get(maneiDominiRank);
        if (allowance == null) {
            LOGGER.warn("[ManeiDomini] no implant allowance for rank {}; issuing none", maneiDominiRank);
            return List.of();
        }

        List<ImplantEntry> eligible = new ArrayList<>(CATALOGUE.stream()
              .filter(entry -> entry.level() <= allowance.maximumLevel())
              .toList());
        int target = randomBetween(allowance.minimumImplants(), allowance.maximumImplants());

        List<String> issued = new ArrayList<>();
        while ((issued.size() < target) && !eligible.isEmpty()) {
            ImplantEntry entry = eligible.remove((int) (Math.random() * eligible.size()));
            String option = entry.optionChoices()
                                  .get((int) (Math.random() * entry.optionChoices().size()));
            if (isRuledOutBySupersession(option, issued)) {
                continue;
            }
            issued.add(option);
        }
        ensureNeuralInterface(issued, allowance);
        return issued;
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
    private static void ensureNeuralInterface(List<String> issued, ImplantAllowance allowance) {
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
