/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.death;

import static megamek.common.eras.EraFlag.*;
import static mekhq.campaign.personnel.enums.TenYearAgeRange.determineAgeRange;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import megamek.Version;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.Gender;
import megamek.common.eras.EraFlag;
import megamek.common.util.weightedMaps.WeightedDoubleMap;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.AgeGroup;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.TenYearAgeRange;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.enums.HPGRating;
import mekhq.campaign.universe.eras.Era;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Handles logic for simulating random deaths in a campaign.
 *
 * <p>The {@code RandomDeath} class is responsible for determining whether a person dies randomly
 * based on various factors such as age, gender, campaign settings, and defined death causes. It provides functionality
 * to configure and process random deaths, manage XML-based cause sources, and track different categories of death
 * causes.</p>
 *
 * <p><b>Core Features:</b></p>
 * <ul>
 *     <li>Supports enabling/disabling random death categories by age group.</li>
 *     <li>Allows separate configuration for suicide-related deaths.</li>
 *     <li>Adjusts random death chances based on age, gender, and campaign-wide configurations.</li>
 *     <li>Parses random death causes from XML files, organized by gender and age range.</li>
 *     <li>Provides detailed reasons and causes of death using weighted probability.</li>
 * </ul>
 */
public class RandomDeath {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.RandomDeath";
    private static final MMLogger LOGGER = MMLogger.create(RandomDeath.class);

    private final Campaign campaign;
    private final CampaignOptions campaignOptions;
    private final Map<AgeGroup, Boolean> enabledAgeGroups;
    private final boolean enableRandomDeathSuicideCause;
    private Map<Gender, Map<TenYearAgeRange, WeightedDoubleMap<PersonnelStatus>>> causes;
    private final double randomDeathMultiplier;

    // Base Chances
    private final List<RandomDeathChance> deathChances = List.of(
          new RandomDeathChance(9, 4, 2),
          new RandomDeathChance(19, 9, 4),
          new RandomDeathChance(29, 17, 8),
          new RandomDeathChance(39, 20, 10),
          new RandomDeathChance(49, 30, 18),
          new RandomDeathChance(59, 70, 40),
          new RandomDeathChance(69, 149, 90),
          new RandomDeathChance(79, 385, 233),
          new RandomDeathChance(89, 1000, 714),
          new RandomDeathChance(99, 2500, 2000),
          new RandomDeathChance(Integer.MAX_VALUE, 5000, 3333)
    );

    // Multipliers
    private final double ERA_MULTIPLIER_AGE_OF_WAR = 1.2;
    private final double ERA_MULTIPLIER_STAR_LEAGUE = 0.9;
    private final double ERA_MULTIPLIER_SUCCESSION_WARS = 1.05;
    private final double ERA_MULTIPLIER_CLAN_INVASION = 0.95;
    private final double ERA_MULTIPLIER_CIVIL_WAR = 0.93;
    private final double ERA_MULTIPLIER_JIHAD = 1.0;
    private final double ERA_MULTIPLIER_REPUBLIC = 0.92;
    private final double ERA_MULTIPLIER_DARK_AGE = 0.95;
    private final double ERA_MULTIPLIER_ILCLAN = 0.85;

    private final double FACTION_MULTIPLIER_CLANS = 0.85;
    private final double FACTION_MULTIPLIER_IS_MAJOR = 0.9;
    private final double FACTION_MULTIPLIER_IS_MINOR = 0.95;
    private final double FACTION_MULTIPLIER_PERIPHERY = 1.00;
    private final double FACTION_MULTIPLIER_PERIPHERY_DEEP = 1.30;
    private final double FACTION_MULTIPLIER_PIRATE = 1.35;
    private final double FACTION_MULTIPLIER_MERCENARY = 1.00;
    private final double FACTION_MULTIPLIER_CANOPUS = 0.85;

    private final double MEDICAL_MULTIPLIER_INJURY_TRANSIENT = 0.1; // per injury
    private final double MEDICAL_MULTIPLIER_INJURY_PERMANENT = 0.25; // once no matter how many
    private final double MEDICAL_MULTIPLIER_HPG_ACCESS = -0.05;

    // Die Size
    private final int DIE_SIZE = 1000000;

    /**
     * Constructs a {@code RandomDeath} object using campaign-specific options.
     *
     * <p>Initializes configurable options such as enabling specific age groups for random deaths,
     * enabling or disabling suicide causes, and retrieving the base random death chances. The death causes map is also
     * initialized by reading relevant files.</p>
     *
     * @param campaign The current campaign.
     */
    public RandomDeath(final Campaign campaign) {
        this.campaign = campaign;
        this.campaignOptions = campaign.getCampaignOptions();

        enabledAgeGroups = campaignOptions.getEnabledRandomDeathAgeGroups();
        enableRandomDeathSuicideCause = campaignOptions.isUseRandomDeathSuicideCause();
        randomDeathMultiplier = campaignOptions.getRandomDeathMultiplier();

        initializeCauses();
    }

    /**
     * Clears and reloads the random death causes from default and user-defined XML files.
     *
     * <p>Both the default XML file and the user-defined XML file are read and processed to populate
     * the {@code causes} map.</p>
     */
    public void initializeCauses() {
        causes = new HashMap<>();
        initializeCausesFromFile(new File(MHQConstants.RANDOM_DEATH_CAUSES_FILE_PATH));
        initializeCausesFromFile(new File(MHQConstants.USER_RANDOM_DEATH_CAUSES_FILE_PATH));
    }

    /**
     * Initializes the random death causes by reading them from an XML file.
     *
     * <p>The XML file contains structured data about causes of random deaths, organized by
     * gender, age range, and personnel statuses. The method parses the file and populates the {@code causes} map.</p>
     *
     * @param file The XML file containing the cause definitions.
     */
    private void initializeCausesFromFile(final File file) {
        if (!file.exists()) {
            LOGGER.warn("File does not exist: {}", file.getPath());
            return;
        }

        final Element rootElement = parseXmlFile(file);
        if (rootElement == null) {
            return;
        }

        final Version version = new Version(rootElement.getAttribute("version"));
        LOGGER.info("Parsing Random Death Causes from {}-origin XML", version);

        final NodeList genderNodes = rootElement.getChildNodes();
        for (int i = 0; i < genderNodes.getLength(); i++) {
            final Node genderNode = genderNodes.item(i);
            if (isInvalidNode(genderNode)) {
                continue;
            }

            try {
                parseGenderNode(genderNode);
            } catch (Exception e) {
                LOGGER.error("Error parsing gender node: {} - {}", genderNode.getNodeName(), e);
            }
        }
    }

    /**
     * Parses the specified XML file into a DOM {@link Element}.
     *
     * @param file The input file.
     *
     * @return The root {@link Element} of the parsed XML document, or {@code null} if an error occurred.
     */
    private Element parseXmlFile(final File file) {
        try (InputStream is = new FileInputStream(file)) {
            final Element element = MHQXMLUtility.newSafeDocumentBuilder()
                                          .parse(is)
                                          .getDocumentElement();
            element.normalize();
            return element;
        } catch (Exception ex) {
            LOGGER.error("Failed to parse XML file: {} - {}", file.getPath(), ex);
            return null;
        }
    }

    /**
     * Processes a top-level gender node from the XML and parses its child nodes.
     *
     * @param genderNode The node representing a gender and its associated death causes.
     */
    private void parseGenderNode(final Node genderNode) {
        final Gender gender = Gender.valueOf(genderNode.getNodeName());
        causes.putIfAbsent(gender, new HashMap<>());

        final NodeList ageRangeNodes = genderNode.getChildNodes();
        for (int i = 0; i < ageRangeNodes.getLength(); i++) {
            final Node ageRangeNode = ageRangeNodes.item(i);
            if (isInvalidNode(ageRangeNode)) {
                continue;
            }

            try {
                parseAgeRangeNode(gender, ageRangeNode);
            } catch (Exception e) {
                LOGGER.error(e, "Error parsing age range node for gender: {}", gender);
            }
        }
    }

    /**
     * Processes an age range node and populates its associated causes.
     *
     * @param gender       The gender associated with the age range node.
     * @param ageRangeNode The node representing an age range and its associated causes.
     */
    private void parseAgeRangeNode(final Gender gender, final Node ageRangeNode) {
        final TenYearAgeRange ageRange = TenYearAgeRange.valueOf(ageRangeNode.getNodeName());
        final WeightedDoubleMap<PersonnelStatus> ageRangeCauses = new WeightedDoubleMap<>();
        causes.get(gender).put(ageRange, ageRangeCauses);

        final NodeList statusNodes = ageRangeNode.getChildNodes();
        for (int i = 0; i < statusNodes.getLength(); i++) {
            final Node statusNode = statusNodes.item(i);
            if (!isElementNode(statusNode)) {
                continue;
            }

            try {
                parseStatusNode(ageRangeCauses, statusNode);
            } catch (Exception e) {
                LOGGER.error(e, "Error parsing status node: {}", statusNode.getNodeName());
            }
        }
    }

    /**
     * Processes a status node and updates the details in the age range's causes map.
     *
     * <p>This method handles parsing of the text content (probability weight) and
     * links it to the specified {@code PersonnelStatus}. Factors such as whether suicide causes are enabled are also
     * considered.</p>
     *
     * @param ageRangeCauses The map of causes for a particular age range.
     * @param statusNode     The node representing a specific personnel status.
     */
    private void parseStatusNode(final WeightedDoubleMap<PersonnelStatus> ageRangeCauses, final Node statusNode) {
        final PersonnelStatus status = PersonnelStatus.valueOf(statusNode.getNodeName());
        if (status.isSuicide() && !enableRandomDeathSuicideCause) {
            return;
        }

        try {
            final double weight = Double.parseDouble(statusNode.getTextContent().trim());
            ageRangeCauses.add(weight, status);
        } catch (NumberFormatException e) {
            LOGGER.info("Unable to parse status node {}: {}", statusNode.getNodeName(), e.getMessage());
        }
    }

    /**
     * Determines if an XML node is invalid for processing.
     *
     * @param node The node to validate.
     *
     * @return {@code true} if the node is invalid (e.g., null or without child nodes), {@code false} otherwise.
     */
    private boolean isInvalidNode(final Node node) {
        return node == null || !node.hasChildNodes();
    }

    /**
     * Checks if a node is an XML element node.
     *
     * @param node The node to check.
     *
     * @return {@code true} if the node is an element node; {@code false} otherwise.
     */
    private boolean isElementNode(final Node node) {
        return node != null && node.getNodeType() == Node.ELEMENT_NODE;
    }

    /**
     * Determines if a person randomly dies based on various multipliers and random chance.
     *
     * <p>This method calculates the probability of a person dying based on era, faction, health,
     * and other modifiers, then performs a random roll to decide if the person dies.</p>
     *
     * @param person the person to evaluate for random death.
     *
     * @return {@code true} if the person randomly dies, {@code false} otherwise.
     */
    public boolean randomlyDies(Person person) {
        if (canDie(person, true) != null) {
            return false;
        }

        Era era = campaign.getEra();
        Faction faction = campaign.getFaction();

        // Determine base chance
        double randomDeathChance = getBaseDeathChance(person);

        // If randomDeathChance is 0, we're never going to have a result other than zero, so just
        // early exit.
        if (randomDeathChance == 0) {
            return false;
        }

        // Apply Era Multiplier
        randomDeathChance = randomDeathChance * getEraMultiplier(era);

        // Apply Faction Multiplier
        randomDeathChance = randomDeathChance * getFactionMultiplier(faction);

        // Apply Health Multiplier
        randomDeathChance = randomDeathChance * getHealthModifier(person);

        // Apply Campaign Options Multiplier
        randomDeathChance = randomDeathChance * randomDeathMultiplier;

        // Round to the nearest int. We need an int for the final roll.
        int actualDeathChance = (int) Math.round(randomDeathChance);

        if (actualDeathChance == 0) {
            return false;
        }

        int roll = randomInt(DIE_SIZE);

        return roll < actualDeathChance;
    }

    /**
     * Retrieves the base death chance for a person based on their age and gender.
     *
     * <p>This method iterates over the list of predefined {@link RandomDeathChance} configurations
     * and finds the matching rule based on the age of the person. Gender-based multipliers are applied
     * accordingly.</p>
     *
     * @param person the person whose death chance is being calculated.
     *
     * @return the base death chance as a double, based on the matching {@link RandomDeathChance}.
     */
    double getBaseDeathChance(Person person) {
        int age = person.getAge(campaign.getLocalDate());

        for (RandomDeathChance deathChance : deathChances) {
            // Check if the age falls within the range of this death chance
            if (age <= deathChance.maximumAge) {
                if (person.getGender().isFemale()) {
                    return deathChance.female; // Use female death chance
                } else {
                    return deathChance.male; // Use male death chance
                }
            }
        }

        // If no matching entry is found for the provided age, default to 0
        return 0.0;
    }

    /**
     * Retrieves the era-based multiplier for determining the death chance.
     *
     * <p>The multiplier is obtained based on the characteristics of the current era (via
     * {@link EraFlag}).</p>
     *
     * @param era the current era being analyzed.
     *
     * @return the death chance multiplier specific to the provided era.
     */
    double getEraMultiplier(Era era) {
        Set<EraFlag> flags = era.getFlags();

        if (flags.contains(PRE_SPACEFLIGHT) || flags.contains(EARLY_SPACEFLIGHT)
                  || flags.contains(AGE_OF_WAR)) {
            return ERA_MULTIPLIER_AGE_OF_WAR;
        }

        if (flags.contains(STAR_LEAGUE)) {
            return ERA_MULTIPLIER_STAR_LEAGUE;
        }

        if (flags.contains(EARLY_SUCCESSION_WARS) || flags.contains(LATE_SUCCESSION_WARS_LOSTECH)
                  || flags.contains(LATE_SUCCESSION_WARS_RENAISSANCE)) {
            return ERA_MULTIPLIER_SUCCESSION_WARS;
        }

        if (flags.contains(CLAN_INVASION)) {
            return ERA_MULTIPLIER_CLAN_INVASION;
        }

        if (flags.contains(CIVIL_WAR)) {
            return ERA_MULTIPLIER_CIVIL_WAR;
        }

        if (flags.contains(JIHAD)) {
            return ERA_MULTIPLIER_JIHAD;
        }

        if (flags.contains(EARLY_REPUBLIC) || flags.contains(LATE_REPUBLIC)) {
            return ERA_MULTIPLIER_REPUBLIC;
        }

        if (flags.contains(DARK_AGES)) {
            return ERA_MULTIPLIER_DARK_AGE;
        }

        // this is the current era, so if we've not hit any of the others, that means we're in ilClan
        return ERA_MULTIPLIER_ILCLAN;
    }

    /**
     * Retrieves the faction-based multiplier for determining the death chance.
     *
     * <p>The multiplier is determined based on the type of faction the campaign belongs to,
     * such as Clan, Periphery, Major Power, or Mercenary. Each faction type has a predefined multiplier applied to the
     * death chance.</p>
     *
     * @param faction the faction to calculate the multiplier for.
     *
     * @return the death chance multiplier specific to the provided faction.
     */
    double getFactionMultiplier(Faction faction) {
        // We have to use String Comparison here due to how
        Faction canopus = Factions.getInstance().getFaction("MOC");

        if (Objects.equals(faction, canopus)) {
            return FACTION_MULTIPLIER_CANOPUS;
        }

        if (faction.isClan()) {
            return FACTION_MULTIPLIER_CLANS;
        }

        if (faction.isDeepPeriphery()) {
            return FACTION_MULTIPLIER_PERIPHERY_DEEP;
        }

        if (faction.isPeriphery()) {
            return FACTION_MULTIPLIER_PERIPHERY;
        }

        if (faction.isMinorPower()) {
            return FACTION_MULTIPLIER_IS_MINOR;
        }

        if (faction.isMajorPower()) {
            return FACTION_MULTIPLIER_IS_MAJOR;
        }

        if (faction.isPirate()) {
            return FACTION_MULTIPLIER_PIRATE;
        }

        // We also use the Mercenary modifier as a fallback
        return FACTION_MULTIPLIER_MERCENARY;
    }

    /**
     * Calculates the health-based multiplier for determining the death chance.
     *
     * <p>The health multiplier accounts for HPG access, injuries, and other health modifiers.
     * When cumulative injuries (transient or permanent) are present, and depending on the use of advanced medical care,
     * additional multipliers are applied to represent the overall health.</p>
     *
     * @param person the person to evaluate for health-related modifiers.
     *
     * @return the health multiplier as a double.
     */
    double getHealthModifier(Person person) {
        double healthMultiplier = 1;

        // Apply HPG access modifier if applicable
        healthMultiplier += getHpgAccessMultiplier();

        // Apply injury-related modifiers
        if (person.needsFixing()) {
            healthMultiplier += getInjuryModifier(person);
        }

        return healthMultiplier;
    }

    /**
     * Calculates the multiplier based on HPG access if applicable.
     *
     * @return the HPG access multiplier, or 0 if no modifier is required.
     */
    private double getHpgAccessMultiplier() {
        HPGRating hpgRating = campaign.getLocation().getPlanet().getHPG(campaign.getLocalDate());
        if (hpgRating != null && hpgRating.compareTo(HPGRating.B) >= 0) {
            return MEDICAL_MULTIPLIER_HPG_ACCESS;
        }
        return 0;
    }

    /**
     * Calculates the modifier based on the injuries of the person.
     *
     * <p>If advanced medical care is enabled in the campaign options, individual injuries are
     * evaluated for either transient or permanent injuries. Otherwise, a simpler calculation is applied based on the
     * total number of injuries.</p>
     *
     * @param person the person whose injuries are evaluated.
     *
     * @return the injury-related health multiplier.
     */
    private double getInjuryModifier(Person person) {
        if (!campaignOptions.isUseAdvancedMedical()) {
            // Simplified injury multiplier without advanced medical care
            return 1 + (MEDICAL_MULTIPLIER_INJURY_TRANSIENT * person.getHits());
        }

        // Advanced medical care: calculate based on individual injuries
        return calculateAdvancedInjuryModifier(person.getInjuries());
    }

    /**
     * Calculates the injury modifier when advanced medical care is used.
     *
     * <p>Counts both transient and permanent injuries, and applies appropriate modifiers.</p>
     *
     * @param injuries the list of injuries to evaluate.
     *
     * @return the calculated health multiplier for advanced injuries.
     */
    private double calculateAdvancedInjuryModifier(List<Injury> injuries) {
        boolean hasPermanentInjuries = false;
        double injuryMultiplier = 0;

        for (Injury injury : injuries) {
            if (injury.isPermanent()) {
                hasPermanentInjuries = true;
            } else {
                injuryMultiplier += MEDICAL_MULTIPLIER_INJURY_TRANSIENT;
            }
        }

        // Apply permanent injury penalty if applicable
        if (hasPermanentInjuries) {
            injuryMultiplier += MEDICAL_MULTIPLIER_INJURY_PERMANENT;
        }

        return injuryMultiplier;
    }

    /**
     * Checks whether a person is exempt from random death and provides a reason if applicable.
     *
     * <p>The following conditions are evaluated:</p>
     * <ul>
     *     <li>If the person is dead: Returns a reason indicating they are already dead.</li>
     *     <li>If random death is enabled and the person is immortal: Returns the immortality reason.</li>
     *     <li>If the person's age group is disabled for random deaths: Returns the reason for the
     *         age group being excluded.</li>
     * </ul>
     *
     * @param person      The individual to evaluate.
     * @param randomDeath Whether random deaths are enabled in the campaign.
     *
     * @return A string describing why the individual cannot die, or {@code null} if no restrictions apply.
     */
    public @Nullable String canDie(final Person person, final boolean randomDeath) {
        LocalDate today = campaign.getLocalDate();
        int age = person.getAge(today);
        AgeGroup ageGroup = AgeGroup.determineAgeGroup(age);

        if (person.getStatus().isDead()) {
            return getCannotDieMessage("cannotDie.Dead.text");
        }

        if (randomDeath) {
            if (person.isImmortal()) {
                return getCannotDieMessage("cannotDie.Immortal.text");
            }

            if (!enabledAgeGroups.get(ageGroup)) {
                return getCannotDieMessage("cannotDie.AgeGroupDisabled.text");
            }
        }

        return null;
    }

    /**
     * Retrieves a localized message for why a person cannot die.
     *
     * @param messageKey The key for the message in the resource bundle.
     *
     * @return The localized reason message.
     */
    private String getCannotDieMessage(final String messageKey) {
        return getFormattedTextAt(RESOURCE_BUNDLE, messageKey);
    }

    /**
     * Processes random death checks for the given individual in a weekly tick.
     *
     * <p>If the person dies, this method updates the campaign and individual status accordingly,
     * and generates a detailed death report. Random death reasons and causes are evaluated as per the configuration and
     * individual factors.</p>
     *
     * @param campaign The active campaign to update.
     * @param today    The current date.
     * @param person   The person being evaluated.
     *
     * @return {@code true} if the person dies during this week; otherwise, {@code false}.
     */
    public boolean processNewWeek(final Campaign campaign, final LocalDate today,
          final Person person) {
        final int age = person.getAge(today);
        final AgeGroup ageGroup = AgeGroup.determineAgeGroup(age);

        if (canDie(person, true) != null) {
            return false;
        }

        if (randomlyDies(person)) {
            // We double-report here, to make sure the user definitely notices that a random death has occurred.
            // Prior to this change, it was exceptionally easy to miss these events.
            String color = ReportingUtilities.getNegativeColor();
            String formatOpener = ReportingUtilities.spanOpeningWithCustomColor(color);
            campaign.addReport(String.format("%s has %s<b>died</b>%s.",
                  person.getHyperlinkedFullTitle(), formatOpener, CLOSING_SPAN_TAG));

            person.changeStatus(campaign, today, getCause(person, ageGroup, age));

            return true;
        } else {
            return false;
        }
    }

    /**
     * Determines the reason or cause of death for a person.
     *
     * <p>Factors including age, gender, injuries, and other conditions like pregnancy
     * are considered in determining the death cause. If no specific cause is found, a default cause is selected.</p>
     *
     * @param person   The person who has died.
     * @param ageGroup The age group of the person.
     * @param age      The person's age.
     *
     * @return The {@code PersonnelStatus} representing the cause of death.
     */
    public PersonnelStatus getCause(final Person person, final AgeGroup ageGroup, final int age) {
        if (person.getStatus().isMIA()) {
            return PersonnelStatus.KIA;
        } else if (person.hasInjuries(false)) {
            final PersonnelStatus status = determineIfInjuriesCausedTheDeath(person);
            if (!status.isActive()) {
                return status;
            }
        }

        if (person.isPregnant()) {
            return PersonnelStatus.PREGNANCY_COMPLICATIONS;
        }

        final Map<TenYearAgeRange, WeightedDoubleMap<PersonnelStatus>> genderedCauses = causes
                                                                                              .get(person.getGender());
        if (genderedCauses == null) {
            return getDefaultCause(ageGroup);
        }

        final WeightedDoubleMap<PersonnelStatus> ageRangeCauses = genderedCauses.get(determineAgeRange(age));
        if (ageRangeCauses == null) {
            return getDefaultCause(ageGroup);
        }

        final PersonnelStatus cause = ageRangeCauses.randomItem();
        return (cause == null) ? getDefaultCause(ageGroup) : cause;
    }

    /**
     * Determines whether a person's death was caused by major or deadly injuries.
     *
     * <p>This method evaluates the person's injuries and checks if any of them are classified
     * as "major or deadly." Only significant injuries are considered for this determination, while minor or chronic
     * conditions are ignored.</p>
     *
     * @param person The person whose injuries are being evaluated.
     *
     * @return {@link PersonnelStatus#WOUNDS} if major or deadly injuries caused the death; otherwise,
     *       {@link PersonnelStatus#ACTIVE} if no significant injuries are found.
     */
    private PersonnelStatus determineIfInjuriesCausedTheDeath(final Person person) {
        // We care about injuries that are major or deadly. We do not want any chronic conditions nor scratches
        return person.getInjuries().stream().anyMatch(injury -> injury.getLevel().isMajorOrDeadly())
                     ? PersonnelStatus.WOUNDS
                     : PersonnelStatus.ACTIVE;
    }

    /**
     * Determines the default cause of death based on the age group of the person.
     *
     * <p>The method assigns a default cause of death based on whether the person is considered
     * elderly. Elderly persons are assigned {@link PersonnelStatus#OLD_AGE} as the cause, while younger individuals are
     * assigned {@link PersonnelStatus#NATURAL_CAUSES}.</p>
     *
     * @param ageGroup The age group of the person.
     *
     * @return {@link PersonnelStatus#OLD_AGE} if the person is in the elder age group; otherwise,
     *       {@link PersonnelStatus#NATURAL_CAUSES}.
     */
    private PersonnelStatus getDefaultCause(final AgeGroup ageGroup) {
        return ageGroup.isElder() ? PersonnelStatus.OLD_AGE : PersonnelStatus.NATURAL_CAUSES;
    }

    /**
     * Generates a random integer up to the given bound (exclusive)
     *
     * <p>We use this custom method to make it easier to test the random components of the
     * `randomlyDies` method.</p>
     *
     * @param bound The upper bound for the random number.
     *
     * @return A random integer between 0 (inclusive) and {@code bound} (exclusive).
     */
    protected int randomInt(int bound) {
        return Compute.randomInt(bound);
    }

    /**
     * A record representing the random death chance information based on gender and maximum age.
     *
     * <p>This record stores the following information:</p>
     * <ul>
     *     <li>{@code maximumAge}: The maximum age to which the death chance applies.</li>
     *     <li>{@code male}: The death chance multiplier for male individuals.</li>
     *     <li>{@code female}: The death chance multiplier for female individuals.</li>
     * </ul>
     */
    public record RandomDeathChance(int maximumAge, double male, double female) {
        /**
         * Constructs a new {@code RandomDeathChance} record, which ensures the values are valid.
         *
         * @param maximumAge The maximum age limit for the death chance.
         * @param male       The death chance multiplier for males.
         * @param female     The death chance multiplier for females.
         *
         * @throws IllegalArgumentException if any values are invalid: - {@code maximumAge} must be greater than 0. -
         *                                  {@code male} and {@code female} must be non-negative.
         */
        public RandomDeathChance {
            if (maximumAge < 0) {
                throw new IllegalArgumentException("maximumAge must be 0 or greater: " + maximumAge);
            }
            if (male < 0 || female < 0) {
                throw new IllegalArgumentException("male and female multipliers must be non-negative: male="
                                                         + male + ", female=" + female);
            }
        }
    }
}
