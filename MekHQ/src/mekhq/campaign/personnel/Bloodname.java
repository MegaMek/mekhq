/*
 * Copyright (c) 2014 - Carl Spain. All Rights Reserved.
 * Copyright (C) 2014-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.universe.Bloodname2;
import megamek.common.universe.BloodnameHouse;
import megamek.common.universe.BloodnameTransfer;
import megamek.common.universe.Bloodnames2;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.enums.Phenotype;

/**
 * @author Neoancient
 */
public class Bloodname {
    private static final MMLogger LOGGER = MMLogger.create(Bloodname.class);

    /** The Clans were founded in 2807; a Bloodname with no date of its own dates from then. */
    private static final int DEFAULT_START_DATE = 2807;

    /**
     * How long a change takes to show up in a breeding programme. A legacy that falls dormant keeps
     * producing warriors for about this long, and a Clan that takes a name on does not grant it
     * immediately.
     */
    private static final int BREEDING_LAG_YEARS = 10;

    /** As above, for a legacy created or brought back, which takes a full generation to bear warriors. */
    private static final int GENERATION_LAG_YEARS = 20;

    // region Variable Declarations
    private static List<Bloodname> bloodnames;

    private String name;
    private String founder;
    private Clan origClan;
    private boolean exclusive;

    /** Year exclusivity ended, or {@code null} while it still holds. */
    private Integer exclusiveUntil;

    private boolean limited;

    /** Year the name became Limited, or {@code null} when the data does not say. */
    private Integer limitedSince;
    private int inactive;
    private int abjured;
    private int reactivated;
    private int startDate;
    private Phenotype phenotype;
    private final List<Clan> postReavingClans;
    private final List<NameAcquired> acquiringClans;
    private NameAcquired absorbed;
    // endregion Variable Declarations

    public Bloodname() {
        name = "";
        founder = "";
        exclusive = false;
        limited = false;
        inactive = 0;
        abjured = 0;
        reactivated = 0;
        startDate = 2807;
        phenotype = Phenotype.GENERAL;
        postReavingClans = new ArrayList<>();
        acquiringClans = new ArrayList<>();
        absorbed = null;
    }

    public String getName() {
        return name;
    }

    public String getFounder() {
        return founder;
    }

    public Clan getOriginClan() {
        return origClan;
    }

    public String getOrigClan() {
        return origClan.getCode();
    }

    public boolean isExclusive() {
        return exclusive;
    }

    /**
     * Whether the name is still exclusive to its origin Clan in the given year.
     *
     * <p>Exclusivity is not permanent - the Council of Six Clans ended it for several names in 3084,
     * and a Trial of Possession can end it at any time.</p>
     *
     * @param year the year to judge it in
     *
     * @return {@code true} if exclusivity is recorded and has not yet ended
     */
    public boolean isExclusive(int year) {
        return exclusive && ((exclusiveUntil == null) || (year < exclusiveUntil));
    }

    public boolean isLimited() {
        return limited;
    }

    /**
     * Whether the name counts as Limited in the given year.
     *
     * <p>A name becomes Limited as its Bloodcount falls; it is not so from its founding. Judging it
     * without a year put a thirty-second-century state of affairs in front of campaigns centuries
     * earlier. A name with no recorded year is treated as having always been Limited.</p>
     *
     * @param year the year to judge it in
     *
     * @return {@code true} if the name is Limited and the campaign has reached the year it became so
     */
    public boolean isLimited(int year) {
        return limited && ((limitedSince == null) || (year >= limitedSince));
    }

    /**
     * @param warriorType A Phenotype constant
     * @param year        The current year of the campaign setting
     *
     * @return An adjustment to the frequency of this name for the phenotype.
     *       <p>
     *       A warrior is three times as likely to have a Bloodname associated with the same phenotype as a general name
     *       (which is split among the three types). Elemental names are treated as general prior to 2870. The names
     *       that later became associated with ProtoMek pilots (identified in WoR) are assumed to have been poor
     *       performers and have a lower frequency even before the invention of the PM, though have a higher frequency
     *       for PM pilots than other aerospace names.
     */
    private int phenotypeMultiplier(Phenotype warriorType, int year) {
        return switch (getPhenotype()) {
            case MEKWARRIOR -> warriorType.isMekWarrior() ? 3 : 0;
            case AEROSPACE -> (warriorType.isAerospace() || warriorType.isProtoMek()) ? 3 : 0;
            case ELEMENTAL -> {
                if (year < 2870) {
                    yield 1;
                }
                yield warriorType.isElemental() ? 3 : 0;
            }
            case PROTOMEK -> switch (warriorType) {
                case PROTOMEK -> 9;
                case AEROSPACE -> 1;
                default -> 0;
            };
            case NAVAL -> warriorType.isNaval() ? 3 : 0;
            default -> 1;
        };
    }

    public boolean isInactive(int year) {
        return (year < startDate) ||
                     ((inactive > 0) && (inactive < year) && !((reactivated > 0) && (reactivated <= year)));
    }

    public boolean isAbjured(int year) {
        return ((abjured > 0) && (abjured < year));
    }

    public Phenotype getPhenotype() {
        return phenotype;
    }

    public List<Clan> getPostReavingClans() {
        return postReavingClans;
    }

    public List<NameAcquired> getAcquiringClans() {
        return acquiringClans;
    }

    public NameAcquired getAbsorbed() {
        return absorbed;
    }

    /**
     * Determines a likely Bloodname based on Clan, phenotype, and year.
     *
     * @param factionCode The faction code for the Clan; must exist in data/names/bloodnames/clans.xml
     * @param phenotype   The person's Phenotype
     * @param year        The current campaign year
     *
     * @return An object representing the chosen Bloodname
     *       <p>
     *       Though based as much as possible on official sources, the method employed here involves a considerable
     *       amount of speculation.
     */
    /**
     * Finds a Bloodname by name.
     *
     * <p>{@link Person} stores only the name it was given, so anything that
     * wants the house behind it - its founder, the Clan it originates with, whether it is exclusive -
     * has to look the record back up.</p>
     *
     * @param name the Bloodname to find, matched without regard to case
     *
     * @return the matching Bloodname, or {@code null} if the name is blank, the data has not loaded,
     *       or no such Bloodname exists
     */
    public static @Nullable Bloodname getBloodname(final @Nullable String name) {
        if ((bloodnames == null) || (name == null) || name.isBlank()) {
            return null;
        }
        for (Bloodname bloodname : bloodnames) {
            if (name.equalsIgnoreCase(bloodname.getName())) {
                return bloodname;
            }
        }
        return null;
    }

    public static @Nullable Bloodname randomBloodname(String factionCode, Phenotype phenotype, int year) {
        return randomBloodname(Clan.getClan(factionCode), phenotype, year);
    }

    /**
     * Determines a likely Bloodname based on Clan, phenotype, and year.
     *
     * @param faction   The Clan faction; must exist in data/names/bloodnames/clans.xml
     * @param phenotype The person's Phenotype
     * @param year      The current campaign year
     *
     * @return An object representing the chosen Bloodname
     *       <p>
     *       Though based as much as possible on official sources, the method employed here involves a considerable
     *       amount of speculation.
     */
    public static @Nullable Bloodname randomBloodname(Clan faction, Phenotype phenotype, int year) {
        if (faction == null) {
            LOGGER.error(
                  "Random Bloodname attempted for a clan that does not exist.{}Please ensure that your clan exists " +
                        "in both clans.xml and data/universe/bloodnames as appropriate. This can be ignored for the " +
                        "Bandit Caste",
                  System.lineSeparator());
            return null;
        } else if (phenotype == null) {
            LOGGER.error(
                  "Random Bloodname attempted for an unknown phenotype. Please open a bug report so this issue may be fixed.");
            return null;
        }

        // This is required because there are currently no bloodnames specifically for
        // vehicle phenotypes
        if (phenotype.isVehicle()) {
            phenotype = Phenotype.GENERAL;
        }

        if (Compute.randomInt(20) == 0) {
            /* 1 in 20 chance that warrior was taken as isorla from another Clan */
            return randomBloodname(faction.getRivalClan(year), phenotype, year);
        }

        if (Compute.randomInt(20) == 0) {
            /*
             * Bloodnames that are predominantly used for a particular phenotype are not
             * exclusively used for that phenotype. A 5% chance of ignoring phenotype will
             * result in a very small chance (around 1%) of a Bloodname usually associated
             * with a different phenotype.
             */
            phenotype = Phenotype.GENERAL;
        }

        /*
         * The relative probability of the various Bloodnames that are original to this
         * Clan
         */
        Map<Bloodname, Fraction> weights = new HashMap<>();
        /* A list of non-exclusive Bloodnames from other Clans */
        List<Bloodname> nonExclusives = new ArrayList<>();
        /*
         * The relative probability that a warrior in this Clan will have a
         * non-exclusive
         * Bloodname that originally belonged to another Clan; the smaller the number
         * of exclusive Bloodnames of this Clan, the larger this chance.
         */
        double nonExclusivesWeight = 0.0;

        for (Bloodname name : bloodnames) {
            /*
             * Bloodnames exclusive to Clans that have been abjured (NC, WIE) continue
             * to be used by those Clans but not by others.
             */
            if (name.isInactive(year) ||
                      (name.isAbjured(year) && !name.getOrigClan().equals(faction.getGenerationCode())) ||
                      (0 == name.phenotypeMultiplier(phenotype, year))) {
                continue;
            }

            Fraction weight = null;

            /*
             * Effects of the Wars of Reaving would take a generation to show up
             * in the breeding programs, so the tables given in the WoR source book
             * are in effect from about 3100 on.
             */
            if (year < 3100) {
                int numClans = 1;
                for (NameAcquired a : name.getAcquiringClans()) {
                    if (a.year < year) {
                        numClans++;
                    }
                }
                /*
                 * Non-exclusive names have a weight of 1 (equal to exclusives) up to 2900,
                 * then decline 10% per 50 years to a minimum of 0.6 in 3050+. In the few
                 * cases where the other Clans using the name are known, the weight is
                 * 1/(number of Clans) instead.
                 */
                if (name.getOrigClan().equals(faction.getGenerationCode()) ||
                          (null != name.getAbsorbed() &&
                                 faction.getGenerationCode().equals(name.getAbsorbed().clan) &&
                                 name.getAbsorbed().year > year)) {
                    if (name.isExclusive() || numClans > 1) {
                        weight = new Fraction(1, numClans);
                    } else {
                        weight = eraFraction(year);
                        nonExclusivesWeight += 1 - weight.value();
                        /*
                         * The fraction is squared to represent the combined effect
                         * of increasing distribution among the Clans and the likelihood
                         * that non-exclusive names would suffer
                         * more reavings and have a lower Blood count.
                         */
                        weight.mul(weight);
                    }
                } else {
                    /*
                     * Most non-exclusives have an unknown distribution and are estimated.
                     * When the actual Clans sharing the Bloodname are known, it is divided
                     * among those Clans.
                     */
                    for (NameAcquired a : name.getAcquiringClans()) {
                        if (faction.getGenerationCode().equals(a.clan)) {
                            weight = new Fraction(1, numClans);
                            break;
                        }
                    }
                    if (null == weight && !name.isExclusive()) {
                        for (int i = 0; i < name.phenotypeMultiplier(phenotype, year); i++) {
                            nonExclusives.add(name);
                        }
                    }
                }
            } else {
                if (name.getPostReavingClans().contains(faction)) {
                    weight = new Fraction(name.phenotypeMultiplier(phenotype, year), name.getPostReavingClans().size());
                    /*
                     * Assume that Bloodnames that were exclusive before the Wars of Reaving
                     * are more numerous (higher blood count).
                     */
                    if (!name.isLimited()) {
                        if (name.isExclusive()) {
                            weight.mul(4);
                        } else {
                            weight.mul(2);
                        }
                    }
                } else if (name.getPostReavingClans().isEmpty()) {
                    for (int i = 0; i < name.phenotypeMultiplier(phenotype, year); i++) {
                        nonExclusives.add(name);
                    }
                }
            }
            if (null != weight) {
                weight.mul(name.phenotypeMultiplier(phenotype, year));
                weights.put(name, weight);
            }
        }

        int lcd = Fraction.lcd(weights.values());
        for (Fraction f : weights.values()) {
            f.mul(lcd);
        }
        List<Bloodname> nameList = new ArrayList<>();
        for (Bloodname b : weights.keySet()) {
            // After scaling by the LCD above, each weight is a whole number, so value() is exactly integral.
            int count = (int) weights.get(b).value();
            for (int i = 0; i < count; i++) {
                nameList.add(b);
            }
        }
        nonExclusivesWeight *= lcd;
        if (year >= 3100) {
            nonExclusivesWeight = nameList.size() / 10.0;
        }
        int roll = Compute.randomInt(nameList.size() + (int) Math.round(nonExclusivesWeight + 0.5));
        if (roll > nameList.size() - 1) {
            return nonExclusives.isEmpty() ? null : nonExclusives.get(Compute.randomInt(nonExclusives.size()));
        } else {
            return nameList.get(roll);
        }
    }

    /**
     * Represents the decreasing frequency of non-exclusive names within the original Clan due to dispersal throughout
     * the Clans and reavings.
     *
     * @param year The current year of the campaign
     *
     * @return A fraction that decreases by 10%/year
     */
    private static Fraction eraFraction(int year) {
        if (year < 2900) {
            return new Fraction(1);
        } else if (year < 2950) {
            return new Fraction(9, 10);
        } else if (year < 3000) {
            return new Fraction(4, 5);
        } else if (year < 3050) {
            return new Fraction(7, 10);
        } else {
            return new Fraction(3, 5);
        }
    }

    /**
     * Loads the Bloodname data from {@code data/universe/bloodnames}, one folder per Clan and one file
     * per Bloodname.
     *
     * <p>This replaced {@code bloodnames.xml}. The data is the same, but held per Bloodname rather
     * than in one file, and it no longer loses the Clans and years attached to shared, acquired and
     * abjured records.</p>
     */
    public static void loadBloodnameData() {
        Clan.loadClanData();
        bloodnames = new ArrayList<>();

        Bloodnames2 source = Bloodnames2.getInstance();
        if (source.isEmpty()) {
            LOGGER.error("No Bloodname data was loaded. Check that data/universe/bloodnames is present.");
            return;
        }
        for (Bloodname2 record : source.getAllBloodnames()) {
            for (BloodnameHouse house : record.getHouses()) {
                bloodnames.add(fromHouse(record, house));
            }
        }
        LOGGER.info("Loaded {} Bloodname houses", bloodnames.size());
    }

    /**
     * Builds one Bloodname from a House record.
     *
     * <p>Several fields are shifted forward in time on the way in, which the previous loader also did:
     * a legacy takes a generation to show up in a breeding programme, so a name that fell dormant is
     * treated as available for another decade, and one created or taken on is treated as unavailable
     * for a further ten or twenty years.</p>
     *
     * @param record the Bloodname the House belongs to
     * @param house  the House to convert
     *
     * @return the Bloodname as the selection code expects it
     */
    private static Bloodname fromHouse(Bloodname2 record, BloodnameHouse house) {
        Bloodname bloodname = new Bloodname();
        bloodname.name = record.getName();
        bloodname.founder = house.getFounder();
        bloodname.origClan = Clan.getClan(record.getClan());
        bloodname.exclusive = house.isExclusive();
        bloodname.exclusiveUntil = house.getEffectiveExclusiveUntil();
        bloodname.limited = house.isLimited();
        bloodname.limitedSince = house.getLimitedSince();

        if (house.getPhenotype() != null) {
            bloodname.phenotype = Phenotype.fromString(house.getPhenotype());
        }
        if (house.getReaved() != null) {
            bloodname.inactive = house.getReaved();
        }
        if (house.getDormant() != null) {
            bloodname.inactive = house.getDormant() + BREEDING_LAG_YEARS;
        }
        if (house.getAbjured() != null) {
            bloodname.abjured = house.getAbjured();
        }
        if (house.getReactivated() != null) {
            bloodname.reactivated = house.getReactivated() + GENERATION_LAG_YEARS;
        }
        if (house.getCreated() != null) {
            bloodname.startDate = house.getCreated() + GENERATION_LAG_YEARS;
        }

        for (String clanCode : house.getPostReaving()) {
            Clan clan = Clan.getClan(clanCode);
            if (clan != null) {
                bloodname.postReavingClans.add(clan);
            }
        }
        // A Clan that took the name on only starts granting it a decade later; one it is shared with
        // grants it from the same year.
        for (BloodnameTransfer transfer : house.getAcquired()) {
            bloodname.acquiringClans.add(new NameAcquired(
                  yearOf(transfer) + BREEDING_LAG_YEARS, transfer.getClan()));
        }
        for (BloodnameTransfer transfer : house.getShared()) {
            bloodname.acquiringClans.add(new NameAcquired(yearOf(transfer), transfer.getClan()));
        }
        if (house.getAbsorbed() != null) {
            bloodname.absorbed = new NameAcquired(yearOf(house.getAbsorbed()),
                  house.getAbsorbed().getClan());
        }
        return bloodname;
    }

    /**
     * @return the year of a transfer, or the founding of the Clans when the record does not give one
     */
    private static int yearOf(BloodnameTransfer transfer) {
        return (transfer.getDate() == null) ? DEFAULT_START_DATE : transfer.getDate();
    }

}
