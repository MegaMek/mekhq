/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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

public class RandomDeath {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.RandomDeath";
    private static final MMLogger logger = MMLogger.create(RandomDeath.class);

    private final Map<AgeGroup, Boolean> enabledAgeGroups;
    private final boolean enableRandomDeathSuicideCause;
    private final Map<Gender, Map<TenYearAgeRange, WeightedDoubleMap<PersonnelStatus>>> causes;
    private final int baseRandomDeathChance;

    public RandomDeath(final CampaignOptions campaignOptions) {
        enabledAgeGroups = campaignOptions.getEnabledRandomDeathAgeGroups();
        enableRandomDeathSuicideCause = campaignOptions.isUseRandomDeathSuicideCause();
        baseRandomDeathChance = campaignOptions.getRandomDeathChance();
        causes = new HashMap<>();

        initializeCauses();
    }

    public void initializeCauses() {
        causes.clear();
        initializeCausesFromFile(new File(MHQConstants.RANDOM_DEATH_CAUSES_FILE_PATH));
        initializeCausesFromFile(new File(MHQConstants.USER_RANDOM_DEATH_CAUSES_FILE_PATH));
    }

    /**
     * Initializes the random death causes by reading them from an XML file.
     *
     * <p>The XML file contains structured information about different causes of random death,
     * organized by gender, age range, and personnel status. The method parses this file, processes
     * the data, and populates the `causes` map accordingly.</p>
     *
     * @param file The XML file containing the cause definitions.
     */
    private void initializeCausesFromFile(final File file) {
        if (!file.exists()) {
            logger.warn("File does not exist: " + file.getPath());
            return;
        }

        final Element rootElement = parseXmlFile(file);
        if (rootElement == null) {
            return;
        }

        final Version version = new Version(rootElement.getAttribute("version"));
        logger.info("Parsing Random Death Causes from " + version + "-origin XML");

        final NodeList genderNodes = rootElement.getChildNodes();
        for (int i = 0; i < genderNodes.getLength(); i++) {
            final Node genderNode = genderNodes.item(i);
            if (isInvalidNode(genderNode)) {
                continue;
            }

            try {
                parseGenderNode(genderNode);
            } catch (Exception e) {
                logger.error("Error parsing gender node: " + genderNode.getNodeName(), e);
            }
        }
    }

    /**
     * Parses the XML file into a DOM {@link Element}.
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
            logger.error("Failed to parse XML file: " + file.getPath(), ex);
            return null;
        }
    }

    /**
     * Processes a top-level gender node and parses its child nodes.
     *
     * @param genderNode The node corresponding to a gender.
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
     * Processes an age range node and populates its causes.
     *
     * @param gender The gender associated with the node.
     * @param ageRangeNode The node corresponding to an age range.
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
     * Processes a status node and updates the age range causes map.
     *
     * @param ageRangeCauses The map of causes for a particular age range.
     * @param statusNode The node corresponding to a personnel status.
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
     * Validates an XML node to ensure it is meaningful (e.g., has child nodes).
     *
     * @param node The node to validate.
     * @return {@code true} if the node is valid; {@code false} otherwise.
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
     * Determines if a person randomly dies based on the campaign, age, and gender.
     *
     * <p>The probability of death increases as a person's age exceeds a specific
     * threshold, with the chance of death growing exponentially for extra years lived.</p>
     *
     * @param age The individual's age.
     * @param gender The individual's gender. Currently unused but supports future extensibility.
     * @return {@code true} if the person randomly dies; {@code false} otherwise.
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
     * Determines if a person cannot die and returns a reason, if any.
     *
     * <p>Checks various conditions such as if the person is already dead, whether
     * random death is enabled, if the person is immortal, or if the death for the
     * provided age group is disabled. Returns {@code null} if none of these
     * conditions prevent the person from dying.</p>
     *
     * @param person       The person to check.
     * @param ageGroup     The age group of the person.
     * @param randomDeath  Whether random death is enabled.
     * @return A reason the person cannot die, or {@code null} if they can die.
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

    private PersonnelStatus determineIfInjuriesCausedTheDeath(final Person person) {
        // We care about injuries that are major or deadly. We do not want any chronic conditions nor scratches
        return person.getInjuries().stream().anyMatch(injury -> injury.getLevel().isMajorOrDeadly())
            ? PersonnelStatus.WOUNDS
            : PersonnelStatus.ACTIVE;
    }

    private PersonnelStatus getDefaultCause(final AgeGroup ageGroup) {
        return ageGroup.isElder() ? PersonnelStatus.OLD_AGE : PersonnelStatus.NATURAL_CAUSES;
    }
}
