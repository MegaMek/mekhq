/*
 * Copyright (c) 2022-2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.death;

import megamek.Version;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.util.weightedMaps.WeightedDoubleMap;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.AgeGroup;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.TenYearAgeRange;
import mekhq.utilities.MHQInternationalization;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.round;
import static megamek.common.Compute.randomInt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;

/**
 * Handles logic for simulating random deaths in a campaign.
 *
 * <p>The {@code RandomDeath} class is responsible for determining whether a person dies randomly
 * based on various factors such as age, gender, campaign settings, and defined death causes.
 * It provides functionality to configure and process random deaths, manage XML-based cause sources,
 * and track different categories of death causes.</p>
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
    private static final MMLogger logger = MMLogger.create(RandomDeath.class);

    private final Map<AgeGroup, Boolean> enabledAgeGroups;
    private final boolean enableRandomDeathSuicideCause;
    private final Map<Gender, Map<TenYearAgeRange, WeightedDoubleMap<PersonnelStatus>>> causes;
    private final int baseRandomDeathChance;

    /**
     * Constructs a {@code RandomDeath} object using campaign-specific options.
     *
     * <p>Initializes configurable options such as enabling specific age groups for random deaths,
     * enabling or disabling suicide causes, and retrieving the base random death chances.
     * The death causes map is also initialized by reading relevant files.</p>
     *
     * @param campaignOptions The campaign options containing random death configurations.
     */
    public RandomDeath(final CampaignOptions campaignOptions) {
        enabledAgeGroups = campaignOptions.getEnabledRandomDeathAgeGroups();
        enableRandomDeathSuicideCause = campaignOptions.isUseRandomDeathSuicideCause();
        baseRandomDeathChance = campaignOptions.getRandomDeathChance();
        causes = new HashMap<>();

        initializeCauses();
    }

    /**
     * Clears and reloads the random death causes from default and user-defined XML files.
     *
     * <p>Both the default XML file and the user-defined XML file are read and processed to populate
     * the {@code causes} map.</p>
     */
    public void initializeCauses() {
        causes.clear();
        initializeCausesFromFile(new File(MHQConstants.RANDOM_DEATH_CAUSES_FILE_PATH));
        initializeCausesFromFile(new File(MHQConstants.USER_RANDOM_DEATH_CAUSES_FILE_PATH));
    }

    /**
     * Initializes the random death causes by reading them from an XML file.
     *
     * <p>The XML file contains structured data about causes of random deaths, organized by
     * gender, age range, and personnel statuses. The method parses the file and populates
     * the {@code causes} map.</p>
     *
     * @param file The XML file containing the cause definitions.
     */
    private void initializeCausesFromFile(final File file) {
        if (!file.exists()) {
            logger.warn("File does not exist: {}", file.getPath());
            return;
        }

        final Element rootElement = parseXmlFile(file);
        if (rootElement == null) {
            return;
        }

        final Version version = new Version(rootElement.getAttribute("version"));
        logger.info("Parsing Random Death Causes from {}-origin XML", version);

        final NodeList genderNodes = rootElement.getChildNodes();
        for (int i = 0; i < genderNodes.getLength(); i++) {
            final Node genderNode = genderNodes.item(i);
            if (isInvalidNode(genderNode)) {
                continue;
            }

            try {
                parseGenderNode(genderNode);
            } catch (Exception e) {
                logger.error("Error parsing gender node: {} - {}", genderNode.getNodeName(), e);
            }
        }
    }

    /**
     * Parses the specified XML file into a DOM {@link Element}.
     *
     * @param file The input file.
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
            logger.error("Failed to parse XML file: {} - {}", file.getPath(), ex);
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
                logger.error("Error parsing age range node for gender: " + gender, e);
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
                logger.error("Error parsing status node: " + statusNode.getNodeName(), e);
            }
        }
    }

    /**
     * Processes a status node and updates the details in the age range's causes map.
     *
     * <p>This method handles parsing of the text content (probability weight) and
     * links it to the specified {@code PersonnelStatus}. Factors such as whether
     * suicide causes are enabled are also considered.</p>
     *
     * @param ageRangeCauses The map of causes for a particular age range.
     * @param statusNode     The node representing a specific personnel status.
     */
    private void parseStatusNode(final WeightedDoubleMap<PersonnelStatus> ageRangeCauses, final Node statusNode) {
        final PersonnelStatus status = PersonnelStatus.valueOf(statusNode.getNodeName());
        if (status.isSuicide() && !enableRandomDeathSuicideCause) {
            return;
        }

        final double weight = Double.parseDouble(statusNode.getTextContent().trim());
        ageRangeCauses.add(weight, status);
    }

    /**
     * Determines if an XML node is invalid for processing.
     *
     * @param node The node to validate.
     * @return {@code true} if the node is invalid (e.g., null or without child nodes),
     *         {@code false} otherwise.
     */
    private boolean isInvalidNode(final Node node) {
        return node == null || !node.hasChildNodes();
    }

    /**
     * Checks if a node is an XML element node.
     *
     * @param node The node to check.
     * @return {@code true} if the node is an element node; {@code false} otherwise.
     */
    private boolean isElementNode(final Node node) {
        return node != null && node.getNodeType() == Node.ELEMENT_NODE;
    }

    /**
     * Determines whether an individual dies randomly based on age, gender, and campaign configuration.
     *
     * <p>The chance of random death is influenced by:</p>
     * <ul>
     *     <li>Age: The risk increases exponentially after a certain threshold.</li>
     *     <li>Gender: Gender-based multipliers affect the base death chance.</li>
     *     <li>Campaign settings: The base random death chance is configured globally.</li>
     * </ul>
     *
     * @param age    The age of the individual.
     * @param gender The gender of the individual.
     * @return {@code true} if the individual dies randomly; {@code false} otherwise.
     */
    public boolean randomlyDies(final int age, final Gender gender) {
        final int AGE_THRESHOLD = 90;
        final double REDUCTION_MULTIPLIER = 0.90;
        final double FEMALE_MULTIPLIER = 1.1;

        int baseDieSize = baseRandomDeathChance;

        // If Random Death disabled?
        if (baseDieSize == 0) {
            return false;
        }

        // Modifier for gender
        if (gender == Gender.FEMALE) {
            baseDieSize = (int) round(baseDieSize * FEMALE_MULTIPLIER);
        }

        // Calculate adjusted die size if the age exceeds the threshold
        int adjustedDieSize = (age > AGE_THRESHOLD)
            ? (int) round(baseDieSize * Math.pow(REDUCTION_MULTIPLIER, (age - AGE_THRESHOLD)))
            : baseDieSize;

        // Return random death outcome
        return randomInt(adjustedDieSize) == 0;
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
     * @param ageGroup    The person's age group.
     * @param randomDeath Whether random deaths are enabled in the campaign.
     * @return A string describing why the individual cannot die, or {@code null} if no restrictions apply.
     */
    public @Nullable String canDie(final Person person, final AgeGroup ageGroup, final boolean randomDeath) {
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
     * @return The localized reason message.
     */
    private String getCannotDieMessage(final String messageKey) {
        return MHQInternationalization.getFormattedTextAt(RESOURCE_BUNDLE, messageKey);
    }

    /**
     * Processes random death checks for the given individual in a weekly tick.
     *
     * <p>If the person dies, this method updates the campaign and individual status accordingly,
     * and generates a detailed death report. Random death reasons and causes are evaluated as per
     * the configuration and individual factors.</p>
     *
     * @param campaign The active campaign to update.
     * @param today    The current date.
     * @param person   The person being evaluated.
     * @return {@code true} if the person dies during this week; otherwise, {@code false}.
     */
    public boolean processNewWeek(final Campaign campaign, final LocalDate today,
                                  final Person person) {
        final int age = person.getAge(today);
        final AgeGroup ageGroup = AgeGroup.determineAgeGroup(age);

        if (canDie(person, ageGroup, true) != null) {
            return false;
        }

        if (randomlyDies(age, person.getGender())) {
            // We double-report here, to make sure the user definitely notices that a random death has occurred.
            // Prior to this change, it was exceptionally easy to miss these events.
            String color = MekHQ.getMHQOptions().getFontColorNegativeHexColor();
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
     * are considered in determining the death cause. If no specific cause is found,
     * a default cause is selected.</p>
     *
     * @param person   The person who has died.
     * @param ageGroup The age group of the person.
     * @param age      The person's age.
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

        final WeightedDoubleMap<PersonnelStatus> ageRangeCauses = genderedCauses
            .get(TenYearAgeRange.determineAgeRange(age));
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
     * as "major or deadly." Only significant injuries are considered for this determination,
     * while minor or chronic conditions are ignored.</p>
     *
     * @param person The person whose injuries are being evaluated.
     * @return {@link PersonnelStatus#WOUNDS} if major or deadly injuries caused the death;
     *         otherwise, {@link PersonnelStatus#ACTIVE} if no significant injuries are found.
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
     * elderly. Elderly persons are assigned {@link PersonnelStatus#OLD_AGE} as the cause,
     * while younger individuals are assigned {@link PersonnelStatus#NATURAL_CAUSES}.</p>
     *
     * @param ageGroup The age group of the person.
     * @return {@link PersonnelStatus#OLD_AGE} if the person is in the elder age group;
     *         otherwise, {@link PersonnelStatus#NATURAL_CAUSES}.
     */
    private PersonnelStatus getDefaultCause(final AgeGroup ageGroup) {
        return ageGroup.isElder() ? PersonnelStatus.OLD_AGE : PersonnelStatus.NATURAL_CAUSES;
    }
}
