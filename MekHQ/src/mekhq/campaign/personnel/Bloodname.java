/*
 * Copyright (c) 2014 - Carl Spain. All Rights Reserved.
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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

import static megamek.common.Compute.d6;
import static mekhq.campaign.personnel.skills.SkillCheckUtility.getTotalAttributeModifier;
import static mekhq.campaign.personnel.skills.SkillType.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;

import megamek.common.Compute;
import megamek.common.TargetRoll;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.skills.Attributes;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Neoancient
 */
public class Bloodname {
    private static final MMLogger logger = MMLogger.create(Bloodname.class);

    // region Variable Declarations
    private static List<Bloodname> bloodnames;

    private String name;
    private String founder;
    private Clan origClan;
    private boolean exclusive;
    private boolean limited;
    private int inactive;
    private int abjured;
    private int reactivated;
    private int startDate;
    private Phenotype phenotype;
    private List<Clan> postReavingClans;
    private List<NameAcquired> acquiringClans;
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

    public boolean isLimited() {
        return limited;
    }

    public boolean isInactive(int year) {
        return (year < startDate) || ((inactive > 0) && (inactive < year)
                && !((reactivated > 0) && (reactivated <= year)));
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
     * @param warriorType A Phenotype constant
     * @param year        The current year of the campaign setting
     * @return An adjustment to the frequency of this name for the phenotype.
     *
     *         A warrior is three times as likely to have a Bloodname associated
     *         with the
     *         same phenotype as a general name (which is split among the three
     *         types).
     *         Elemental names are treated as general prior to 2870. The names that
     *         later
     *         became associated with ProtoMek pilots (identified in WoR) are
     *         assumed
     *         to have been poor performers and have a lower frequency even before
     *         the
     *         invention of the PM, though have a higher frequency for PM pilots
     *         than other
     *         aerospace names.
     */
    private int phenotypeMultiplier(Phenotype warriorType, int year) {
        switch (getPhenotype()) {
            case MEKWARRIOR:
                return warriorType.isMekWarrior() ? 3 : 0;
            case AEROSPACE:
                return (warriorType.isAerospace() || warriorType.isProtoMek()) ? 3 : 0;
            case ELEMENTAL:
                if (year < 2870) {
                    return 1;
                }
                return warriorType.isElemental() ? 3 : 0;
            case PROTOMEK:
                switch (warriorType) {
                    case PROTOMEK:
                        return 9;
                    case AEROSPACE:
                        return 1;
                    default:
                        return 0;
                }
            case NAVAL:
                return warriorType.isNaval() ? 3 : 0;
            case VEHICLE:
            case GENERAL:
            default:
                return 1;
        }
    }

    public static Bloodname loadFromXml(Node node) {
        Bloodname retVal = new Bloodname();
        NodeList nl = node.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node wn = nl.item(i);

            try {
                if (wn.getNodeName().equalsIgnoreCase("name")) {
                    retVal.name = wn.getTextContent().trim();
                } else if (wn.getNodeName().equalsIgnoreCase("founder")) {
                    retVal.founder = wn.getTextContent().trim();
                } else if (wn.getNodeName().equalsIgnoreCase("clan")) {
                    retVal.origClan = Clan.getClan(wn.getTextContent().trim());
                } else if (wn.getNodeName().equalsIgnoreCase("exclusive")) {
                    retVal.exclusive = true;
                } else if (wn.getNodeName().equalsIgnoreCase("reaved")) {
                    retVal.inactive = Integer.parseInt(wn.getTextContent().trim());
                } else if (wn.getNodeName().equalsIgnoreCase("dormant")) {
                    retVal.inactive = Integer.parseInt(wn.getTextContent().trim()) + 10;
                } else if (wn.getNodeName().equalsIgnoreCase("abjured")) {
                    retVal.abjured = Integer.parseInt(wn.getTextContent().trim());
                } else if (wn.getNodeName().equalsIgnoreCase("reactivated")) {
                    retVal.reactivated = Integer.parseInt(wn.getTextContent().trim() + 20);
                } else if (wn.getNodeName().equalsIgnoreCase("phenotype")) {
                    retVal.phenotype = Phenotype.fromString(wn.getTextContent().trim());
                } else if (wn.getNodeName().equalsIgnoreCase("postReaving")) {
                    String[] clans = wn.getTextContent().trim().split(",");
                    for (String c : clans) {
                        retVal.postReavingClans.add(Clan.getClan(c));
                    }
                } else if (wn.getNodeName().equalsIgnoreCase("acquired")) {
                    retVal.acquiringClans.add(new NameAcquired(
                            Integer.parseInt(wn.getAttributes().getNamedItem("date").getTextContent()) + 10,
                            wn.getTextContent().trim()));
                } else if (wn.getNodeName().equalsIgnoreCase("shared")) {
                    retVal.acquiringClans.add(new NameAcquired(
                            Integer.parseInt(wn.getAttributes().getNamedItem("date").getTextContent()),
                            wn.getTextContent().trim()));
                } else if (wn.getNodeName().equalsIgnoreCase("absorbed")) {
                    retVal.absorbed = new NameAcquired(
                            Integer.parseInt(wn.getAttributes().getNamedItem("date").getTextContent()),
                            wn.getTextContent().trim());
                } else if (wn.getNodeName().equalsIgnoreCase("created")) {
                    retVal.startDate = Integer.parseInt(wn.getTextContent().trim()) + 20;
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }

        return retVal;
    }

    /**
     * Determines a likely Bloodname based on Clan, phenotype, and year.
     *
     * @param factionCode The faction code for the Clan; must exist in
     *                    data/names/bloodnames/clans.xml
     * @param phenotype   The person's Phenotype
     * @param year        The current campaign year
     * @return An object representing the chosen Bloodname
     *
     *         Though based as much as possible on official sources, the method
     *         employed here involves a
     *         considerable amount of speculation.
     */
    public static @Nullable Bloodname randomBloodname(String factionCode, Phenotype phenotype, int year) {
        return randomBloodname(Clan.getClan(factionCode), phenotype, year);
    }

    /**
     * Determines a likely Bloodname based on Clan, phenotype, and year.
     *
     * @param faction   The Clan faction; must exist in
     *                  data/names/bloodnames/clans.xml
     * @param phenotype The person's Phenotype
     * @param year      The current campaign year
     * @return An object representing the chosen Bloodname
     *
     *         Though based as much as possible on official sources, the method
     *         employed here involves a
     *         considerable amount of speculation.
     */
    public static @Nullable Bloodname randomBloodname(Clan faction, Phenotype phenotype, int year) {
        if (faction == null) {
            logger.error("Random Bloodname attempted for a clan that does not exist."
                    + System.lineSeparator()
                    + "Please ensure that your clan exists in both the clans.xml and bloodnames.xml files as appropriate.");
            return null;
        } else if (phenotype == null) {
            logger.error(
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
                    (name.isAbjured(year) && !name.getOrigClan().equals(faction.getGenerationCode()))
                    || (0 == name.phenotypeMultiplier(phenotype, year))) {
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
                for (Bloodname.NameAcquired a : name.getAcquiringClans()) {
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
                        (null != name.getAbsorbed() && faction.getGenerationCode().equals(name.getAbsorbed().clan) &&
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
                         * more reavings and have a lower Bloodcount.
                         */
                        weight.mul(weight);
                    }
                } else {
                    /*
                     * Most non-exclusives have an unknown distribution and are estimated.
                     * When the actual Clans sharing the Bloodname are known, it is divided
                     * among those Clans.
                     */
                    for (Bloodname.NameAcquired a : name.getAcquiringClans()) {
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
                     * are more numerous (higher bloodcount).
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
            for (int i = 0; i < weights.get(b).value(); i++) {
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
     * Represents the decreasing frequency of non-exclusive names within the
     * original Clan
     * due to dispersal throughout the Clans and reavings.
     *
     * @param year The current year of the campaign
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

    public static void loadBloodnameData() {
        Clan.loadClanData();
        bloodnames = new ArrayList<>();

        File f = new File("data/names/bloodnames/bloodnames.xml"); // TODO : Remove inline file path
        FileInputStream fis;
        try {
            fis = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            logger.error("Cannot find file bloodnames.xml");
            return;
        }

        Document doc;

        try {
            DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();
            doc = db.parse(fis);
            fis.close();
        } catch (Exception ex) {
            logger.error("Could not parse bloodnames.xml", ex);
            return;
        }

        Element bloodnameElement = doc.getDocumentElement();
        NodeList nl = bloodnameElement.getChildNodes();
        bloodnameElement.normalize();

        for (int i = 0; i < nl.getLength(); i++) {
            Node wn = nl.item(i);
            if (wn.getNodeType() == Node.ELEMENT_NODE) {
                if (wn.getNodeName().equalsIgnoreCase("bloodname")) {
                    bloodnames.add(Bloodname.loadFromXml(wn));
                }
            }
        }
        logger.info("Loaded " + bloodnames.size() + " Bloodname records.");
    }

    /**
     * If the person does not already have a bloodname, assigns a chance of having one based on skill and rank. If the
     * roll indicates there should be a bloodname, one is assigned as appropriate to the person's phenotype and the
     * player's faction.
     *
     * @param person     The Bloodname candidate
     * @param ignoreDice If true, skips the random roll and assigns a Bloodname automatically
     */
    public static void checkBloodnameAdd(Person person, boolean ignoreDice, boolean isUseUnitRating,
          int atbUnitRatingModifier, int currentYear, int officerCutOff, Faction campaignFaction) {
        // if person is non-clan or does not have a phenotype
        if (!person.isClanPersonnel() || person.getPhenotype().isNone()) {
            return;
        }

        // Person already has a bloodname, we open up the dialog to ask if they want to
        // keep the current bloodname or assign a new one
        if (!person.getBloodname().isEmpty()) {
            int result = JOptionPane.showConfirmDialog(null,
                  person.getFullTitle() +
                        " already has the bloodname " +
                        person.getBloodname() +
                        "\nDo you wish to remove that bloodname and generate a new one?",
                  "Already Has Bloodname",
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.QUESTION_MESSAGE);
            if (result == JOptionPane.NO_OPTION) {
                return;
            } else {
                ignoreDice = true;
            }
        }

        // Go ahead and generate a new bloodname
        TargetRoll targetRoll = new TargetRoll(6, "Base Target Number");
        PersonnelOptions options = person.getOptions();
        Attributes attributes = person.getATOWAttributes();
        if (!ignoreDice) {
            switch (person.getPhenotype()) {
                case MEKWARRIOR -> {
                    if (person.hasSkill(S_GUN_MEK) && person.hasSkill(S_PILOT_MEK)) {
                        targetRoll.addModifier(person.getSkill(S_GUN_MEK).getFinalSkillValue(options), S_GUN_MEK);
                        targetRoll.addModifier(getTotalAttributeModifier(attributes, getType(S_GUN_MEK)),
                              S_GUN_MEK + ":Attributes");

                        targetRoll.addModifier(person.getSkill(S_PILOT_MEK).getFinalSkillValue(options), S_PILOT_MEK);
                        targetRoll.addModifier(getTotalAttributeModifier(attributes, getType(S_PILOT_MEK)),
                              S_PILOT_MEK + ":Attributes");
                    } else {
                        logger.info("Lacks necessary skills for a Mekwarrior");
                        return; // automatic fail
                    }
                }
                case AEROSPACE -> {
                    if (person.hasSkill(S_GUN_AERO) && person.hasSkill(S_PILOT_MEK)) {
                        targetRoll.addModifier(person.getSkill(S_GUN_AERO).getFinalSkillValue(options), S_GUN_AERO);
                        targetRoll.addModifier(getTotalAttributeModifier(attributes, getType(S_GUN_AERO)),
                              S_GUN_AERO + ":Attributes");

                        targetRoll.addModifier(person.getSkill(S_PILOT_AERO).getFinalSkillValue(options), S_PILOT_AERO);
                        targetRoll.addModifier(getTotalAttributeModifier(attributes, getType(S_PILOT_AERO)),
                              S_PILOT_AERO + ":Attributes");
                    } else {
                        logger.info("Lacks necessary skills for an Aerospace Pilot");
                        return; // automatic fail
                    }
                }
                case ELEMENTAL -> {
                    if (person.hasSkill(S_GUN_BA) && person.hasSkill(S_GUN_BA)) {
                        targetRoll.addModifier(person.getSkill(S_GUN_BA).getFinalSkillValue(options), S_GUN_BA);
                        targetRoll.addModifier(getTotalAttributeModifier(attributes, getType(S_GUN_BA)),
                              S_GUN_BA + ":Attributes");

                        targetRoll.addModifier(person.getSkill(S_ANTI_MEK).getFinalSkillValue(options), S_ANTI_MEK);
                        targetRoll.addModifier(getTotalAttributeModifier(attributes, getType(S_ANTI_MEK)),
                              S_ANTI_MEK + ":Attributes");
                    } else {
                        logger.info("Lacks necessary skills for a Battle Armor pilot");
                        return; // automatic fail
                    }
                }
                case VEHICLE -> {
                    switch (person.getPrimaryRole()) {
                        case GROUND_VEHICLE_DRIVER -> {
                            if (person.hasSkill(S_PILOT_GVEE)) {
                                targetRoll.addModifier(person.getSkill(S_PILOT_GVEE).getFinalSkillValue(options),
                                      S_PILOT_GVEE);
                                targetRoll.addModifier(getTotalAttributeModifier(attributes, getType(S_PILOT_GVEE)),
                                      S_PILOT_GVEE + ":Attributes");
                            } else {
                                logger.info("Lacks necessary skills for a vehicle driver");
                                return; // automatic fail
                            }
                        }
                        case NAVAL_VEHICLE_DRIVER -> {
                            if (person.hasSkill(S_PILOT_NVEE)) {
                                targetRoll.addModifier(person.getSkill(S_PILOT_NVEE).getFinalSkillValue(options),
                                      S_PILOT_NVEE);
                                targetRoll.addModifier(getTotalAttributeModifier(attributes, getType(S_PILOT_NVEE)),
                                      S_PILOT_NVEE + ":Attributes");
                            } else {
                                logger.info("Lacks necessary skills for a Naval Driver");
                                return; // automatic fail
                            }
                        }
                        case VTOL_PILOT -> {
                            if (person.hasSkill(S_PILOT_VTOL)) {
                                targetRoll.addModifier(person.getSkill(S_PILOT_VTOL).getFinalSkillValue(options),
                                      S_PILOT_VTOL);
                                targetRoll.addModifier(getTotalAttributeModifier(attributes, getType(S_PILOT_VTOL)),
                                      S_PILOT_VTOL + ":Attributes");
                            } else {
                                logger.info("Lacks necessary skills for a VTOL pilot");
                                return; // automatic fail
                            }
                        }
                        case VEHICLE_GUNNER -> {
                            if (person.hasSkill(S_GUN_VEE)) {
                                targetRoll.addModifier(person.getSkill(S_GUN_VEE).getFinalSkillValue(options),
                                      S_GUN_VEE);
                                targetRoll.addModifier(getTotalAttributeModifier(attributes, getType(S_GUN_VEE)),
                                      S_GUN_VEE + ":Attributes");
                            } else {
                                logger.info("Lacks necessary skills for a Vehicle Gunner");
                                return; // automatic fail
                            }
                        }
                        default -> {
                            return;
                        }
                    }
                }
                case PROTOMEK -> {
                    if (person.hasSkill(S_GUN_PROTO)) {
                        targetRoll.addModifier(person.getSkill(S_GUN_PROTO).getFinalSkillValue(options) * 2,
                              S_GUN_PROTO);
                        targetRoll.addModifier(getTotalAttributeModifier(attributes, getType(S_GUN_PROTO)) * 2,
                              S_GUN_PROTO + ":Attributes");
                    } else {
                        logger.info("Lacks necessary skills for a ProtoMek Warrior");
                        return; // automatic fail
                    }
                }
                case NAVAL -> {
                    switch (person.getPrimaryRole()) {
                        case VESSEL_PILOT -> {
                            if (person.hasSkill(S_PILOT_SPACE)) {
                                targetRoll.addModifier(person.getSkill(S_PILOT_SPACE).getFinalSkillValue(options) * 2,
                                      S_PILOT_SPACE);
                                targetRoll.addModifier(getTotalAttributeModifier(attributes, getType(S_PILOT_SPACE)) *
                                                             2, S_PILOT_SPACE + ":Attributes");
                            } else {
                                logger.info("Lacks necessary skills for a Vessel Pilot");
                                return; // automatic fail
                            }
                        }
                        case VESSEL_GUNNER -> {
                            if (person.hasSkill(S_GUN_SPACE)) {
                                targetRoll.addModifier(person.getSkill(S_GUN_SPACE).getFinalSkillValue(options) * 2,
                                      S_PILOT_SPACE);
                                targetRoll.addModifier(getTotalAttributeModifier(attributes, getType(S_GUN_SPACE)) * 2,
                                      S_GUN_SPACE + ":Attributes");
                            } else {
                                logger.info("Lacks necessary skills for a Vessel Gunner");
                                return; // automatic fail
                            }
                        }
                        case VESSEL_CREW -> {
                            if (person.hasSkill(S_TECH_VESSEL)) {
                                targetRoll.addModifier(person.getSkill(S_TECH_VESSEL).getFinalSkillValue(options) * 2,
                                      S_TECH_VESSEL);
                                targetRoll.addModifier(getTotalAttributeModifier(attributes, getType(S_TECH_VESSEL)) *
                                                             2, S_TECH_VESSEL + ":Attributes");
                            } else {
                                logger.info("Lacks necessary skills for a Vessel Tech");
                                return; // automatic fail
                            }
                        }
                        default -> {
                            return;
                        }
                    }
                }
                default -> {
                    return;
                }
            }

            // Higher-rated units are more likely to have Bloodnamed
            if (isUseUnitRating) {
                targetRoll.addModifier(IUnitRating.DRAGOON_C - atbUnitRatingModifier, "Unit Rating");
            }

            // Reavings diminish the number of available Bloodrights in later eras
            int eraModifier = 0;
            if (currentYear <= 2950) {
                eraModifier--;
            }

            if (currentYear > 3055) {
                eraModifier++;
            }

            if (currentYear > 3065) {
                eraModifier++;
            }

            if (currentYear > 3080) {
                eraModifier++;
            }

            if (eraModifier > 0) {
                targetRoll.addModifier(eraModifier, "Era");
            }

            // Officers have better chance; no penalty for non-officer
            targetRoll.addModifier(Math.min(0, officerCutOff - person.getRankNumeric()), "Rank");
        }

        if (ignoreDice || (d6(2) >= targetRoll.getValue())) {
            final Phenotype phenotype = person.getPhenotype().isNone() ? Phenotype.GENERAL : person.getPhenotype();

            final Bloodname bloodname = Bloodname.randomBloodname((campaignFaction.isClan() ?
                                                                         campaignFaction :
                                                                         person.getOriginFaction()).getShortName(),
                  phenotype,
                  currentYear);
            if (bloodname != null) {
                person.setBloodname(bloodname.getName());
                MekHQ.triggerEvent(new PersonChangedEvent(person));
            }
        }
    }

    private static class NameAcquired {
        public int year;
        public String clan;

        public NameAcquired(int y, String c) {
            year = y;
            clan = c;
        }
    }

    static class Fraction {
        private int numerator;
        private int denominator;

        public Fraction() {
            numerator = 0;
            denominator = 1;
        }

        public Fraction(int n, int d) {
            if (d == 0) {
                throw new IllegalArgumentException("Denominator is zero.");
            }
            if (d < 0) {
                n = -n;
                d = -d;
            }
            numerator = n;
            denominator = d;
        }

        public Fraction(int i) {
            numerator = i;
            denominator = 1;
        }

        public Fraction(Fraction f) {
            numerator = f.numerator;
            denominator = f.denominator;
        }

        @Override
        public String toString() {
            return numerator + "/" + denominator;
        }

        @Override
        public boolean equals(final @Nullable Object object) {
            if (this == object) {
                return true;
            } else if (!(object instanceof Fraction)) {
                return false;
            } else {
                return value() == ((Fraction) object).value();
            }
        }

        @Override
        public int hashCode() {
            return Double.valueOf(value()).hashCode();
        }

        @Override
        public Object clone() {
            return new Fraction(this);
        }

        public double value() {
            return (double) numerator / (double) denominator;
        }

        public void reduce() {
            if (denominator > 1) {
                for (int i = denominator - 1; i > 1; i--) {
                    if (numerator % i == 0 && denominator % i == 0) {
                        numerator /= i;
                        denominator /= i;
                        i = denominator - 1;
                    }
                }
            }
        }

        public int getNumerator() {
            return numerator;
        }

        public int getDenominator() {
            return denominator;
        }

        public void add(Fraction f) {
            numerator = numerator * f.denominator + f.numerator * denominator;
            denominator = denominator * f.denominator;
            reduce();
        }

        public void add(int i) {
            numerator += i * denominator;
            reduce();
        }

        public void sub(Fraction f) {
            numerator = numerator * f.denominator - f.numerator * denominator;
            denominator = denominator * f.denominator;
            reduce();
        }

        public void sub(int i) {
            numerator -= i * denominator;
            reduce();
        }

        public void mul(Fraction f) {
            numerator *= f.numerator;
            denominator *= f.denominator;
            reduce();
        }

        public void mul(int i) {
            numerator *= i;
            reduce();
        }

        public void div(Fraction f) {
            numerator *= f.denominator;
            denominator *= f.numerator;
            reduce();
        }

        public void div(int i) {
            denominator *= i;
        }

        public static int lcd(Collection<Fraction> list) {
            Set<Integer> denominators = new HashSet<>();
            for (Fraction f : list) {
                denominators.add(f.denominator);
            }
            boolean done = false;
            int retVal = 1;
            while (!done) {
                done = true;
                for (Integer d : denominators) {
                    if (d / retVal > 1 || retVal % d != 0) {
                        retVal++;
                        done = false;
                        break;
                    }
                }
            }
            return retVal;
        }
    }
}
