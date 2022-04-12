/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.enums.Gender;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.util.weightedMaps.WeightedDoubleMap;
import mekhq.MHQConstants;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.*;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractDeath {
    //region Variable Declarations
    private final RandomDeathMethod method;
    private final Map<AgeGroup, Boolean> enabledAgeGroups;
    private final boolean enableRandomDeathSuicideCause;
    private final Map<Gender, Map<TenYearAgeRange, WeightedDoubleMap<PersonnelStatus>>> causes;
    //endregion Variable Declarations

    //region Constructors
    protected AbstractDeath(final RandomDeathMethod method, final CampaignOptions options) {
        this.method = method;
        this.enabledAgeGroups = options.getEnabledRandomDeathAgeGroups();
        this.enableRandomDeathSuicideCause = options.isUseRandomDeathSuicideCause();
        this.causes = new HashMap<>();
        initializeCauses();
    }
    //endregion Constructors

    //region Getters
    public RandomDeathMethod getMethod() {
        return method;
    }

    public Map<AgeGroup, Boolean> getEnabledAgeGroups() {
        return enabledAgeGroups;
    }

    public boolean isEnableRandomDeathSuicideCause() {
        return enableRandomDeathSuicideCause;
    }

    public Map<Gender, Map<TenYearAgeRange, WeightedDoubleMap<PersonnelStatus>>> getCauses() {
        return causes;
    }
    //endregion Getters

    //region New Day
    /**
     * Processes new day random death for an individual.
     * @param campaign the campaign to process
     * @param today the current day
     * @param person the person to process
     */
    public boolean processNewDay(final Campaign campaign, final LocalDate today, final Person person) {
        return false;
        /*
        if (canDie(person, true) != null) {
            return false;
        }

        if (!getEnabledAgeGroups().get(ageGroup)) {
            return false;
        }

        if (!getRandomDeath().getMethod().isNone()) {
            final int age = p.getAge(getLocalDate());
            final AgeGroup ageGroup = AgeGroup.determineAgeGroup(age);
            if (getRandomDeath().randomDeath(ageGroup, age, p.getGender())) {
                p.changeStatus(campaign, getLocalDate(), getRandomDeath().getCause(getLocalDate(), p, ageGroup, age));
                continue;
            }
        }

        final boolean sameSex = person.getGenealogy().getSpouse().getGender() == person.getGender();
        if (randomlyDies()) {
            divorce(campaign, today, person, SplittingSurnameStyle.WEIGHTED);
        }
         */
    }

    //region Random Death
    /**
     * @param age the person's age
     * @param gender the person's gender
     * @return true if the person is selected to randomly die, otherwise false
     */
    public abstract boolean randomlyDies(final int age, final Gender gender);
    //endregion Random Death
    //endregion New Day

    //region Cause
    /**
     * @param person the person who has died
     * @param ageGroup the person's age group
     * @param age the person's age
     * @return the cause of the Person's random death
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

        final Map<TenYearAgeRange, WeightedDoubleMap<PersonnelStatus>> genderedCauses = getCauses().get(person.getGender());
        if (genderedCauses == null) {
            return getDefaultCause(ageGroup);
        }

        final WeightedDoubleMap<PersonnelStatus> ageRangeCauses = genderedCauses.get(TenYearAgeRange.determineAgeRange(age));
        if (ageRangeCauses == null) {
            return getDefaultCause(ageGroup);
        }

        final PersonnelStatus cause = ageRangeCauses.randomItem();
        return (cause == null) ? getDefaultCause(ageGroup) : cause;
    }

    /**
     * @param ageGroup the age group to get the default random death cause for
     * @return the default cause, which is old age for elders and natural causes for everyone else
     */
    private PersonnelStatus getDefaultCause(final AgeGroup ageGroup) {
        return ageGroup.isElder() ? PersonnelStatus.OLD_AGE : PersonnelStatus.NATURAL_CAUSES;
    }

    /**
     * @param person the person from whom may have died of injuries (which may be diseases, once
     *               that is implemented)
     * @return the personnel status applicable to the form of injury that caused the death, or
     * ACTIVE if it wasn't determined that injuries caused the death
     */
    private PersonnelStatus determineIfInjuriesCausedTheDeath(final Person person) {
        // We care about injuries that are major or deadly. We do not want any chronic conditions
        // nor scratches
        return person.getInjuries().stream().anyMatch(injury -> injury.getLevel().isMajorOrDeadly())
                ? PersonnelStatus.WOUNDS : PersonnelStatus.ACTIVE;
    }
    //endregion Cause

    //region File I/O
    public void initializeCauses() {
        getCauses().clear();

        final File file = new MegaMekFile(MHQConstants.RANDOM_DEATH_CAUSES_FILE_PATH).getFile();
        if (!file.exists()) {
            return;
        }

        final Element element;

        // Open up the file
        try (InputStream is = new FileInputStream(file)) {
            element = MekHqXmlUtil.newSafeDocumentBuilder().parse(is).getDocumentElement();
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to open file", ex);
            return;
        }
        element.normalize();
        final Version version = new Version(element.getAttribute("version"));
        final NodeList nl = element.getChildNodes();

        LogManager.getLogger().info("Parsing Random Death Causes from " + version + "-origin xml");
        for (int i = 0; i < nl.getLength(); i++) {
            final Node wn = nl.item(i);
            if (!wn.hasChildNodes()) {
                continue;
            }
            try {
                final Map<TenYearAgeRange, WeightedDoubleMap<PersonnelStatus>> genderedCauses = new HashMap<>();
                getCauses().put(Gender.valueOf(wn.getNodeName()), genderedCauses);
                final NodeList nl2 = wn.getChildNodes();
                for (int j = 0; j < nl.getLength(); j++) {
                    final Node wn2 = nl2.item(j);
                    if (!wn2.hasChildNodes()) {
                        continue;
                    }

                    try {
                        final WeightedDoubleMap<PersonnelStatus> ageRangeCauses = new WeightedDoubleMap<>();
                        genderedCauses.put(TenYearAgeRange.valueOf(wn2.getNodeName()), ageRangeCauses);
                        final NodeList nl3 = wn2.getChildNodes();
                        for (int k = 0; k < nl3.getLength(); k++) {
                            final Node wn3 = nl3.item(k);
                            try {
                                final PersonnelStatus status = PersonnelStatus.valueOf(wn3.getNodeName());
                                if (status.isSuicide() && !isEnableRandomDeathSuicideCause()) {
                                    continue;
                                }
                                ageRangeCauses.add(Double.parseDouble(wn3.getTextContent().trim()), status);
                            } catch (Exception ignored) {

                            }
                        }
                    } catch (Exception ignored) {

                    }
                }
            } catch (Exception ignored) {

            }
        }
    }
    //endregion File I/O
}
