/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.personnel.education;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.education.AcademyType;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import mekhq.campaign.personnel.enums.education.EducationLevel.Adapter;
import mekhq.campaign.universe.*;

import java.util.*;

/**
 * The Academy class represents an academy with various properties and methods.
 */
@XmlRootElement(name = "academy")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class Academy implements Comparable<Academy> {
    @XmlElement(name = "name")
    private String name = "Error: Name Missing";

    @XmlElement(name = "type")
    private String type = "None";

    @XmlElement(name = "isMilitary")
    private Boolean isMilitary = false;

    @XmlElement(name = "isReeducationCamp")
    private Boolean isReeducationCamp = false;

    @XmlElement(name = "isPrepSchool")
    private Boolean isPrepSchool = false;

    @XmlElement(name = "description")
    private String description = "Error: no description";

    @XmlElement(name = "factionDiscount")
    private Integer factionDiscount = 10;

    @XmlElement(name = "isFactionRestricted")
    private Boolean isFactionRestricted = false;

    @XmlElement(name = "isLocal")
    private Boolean isLocal = false;

    @XmlElement(name = "isHomeSchool")
    private Boolean isHomeSchool = false;

    @XmlElement(name = "locationSystem")
    private List<String> locationSystems;

    @XmlElement(name = "constructionYear")
    private Integer constructionYear = 2300;

    @XmlElement(name = "destructionYear")
    private Integer destructionYear = 9999;

    @XmlElement(name = "closureYear")
    private Integer closureYear = 9999;

    @XmlElement(name = "tuition")
    private Integer tuition = 0;

    @XmlElement(name = "durationDays")
    // this number is chosen so that PrepSchools still experience dropouts
    // otherwise 300/year
    private Integer durationDays = 11;

    @XmlElement(name = "facultySkill")
    private Integer facultySkill = 7;

    @XmlJavaTypeAdapter(value = Adapter.class)
    private EducationLevel educationLevelMin = EducationLevel.EARLY_CHILDHOOD;

    @XmlJavaTypeAdapter(value = Adapter.class)
    private EducationLevel educationLevelMax = EducationLevel.HIGH_SCHOOL;

    @XmlElement(name = "ageMin")
    private Integer ageMin = 0;

    @XmlElement(name = "ageMax")
    private Integer ageMax = 9999;

    @XmlElement(name = "qualification")
    private List<String> qualifications;

    @XmlElement(name = "curriculum")
    private List<String> curriculums;

    @XmlElement(name = "qualificationStartYear")
    private List<Integer> qualificationStartYears;

    @XmlElement(name = "baseAcademicSkillLevel")
    private Integer baseAcademicSkillLevel = -1;

    private Integer id;
    private String set;

    /**
     * This class provides a no-arg constructor, which is necessary for the unmarshalling of XML.
     */
    public Academy() {
    }

    /**
     * Constructs a new Academy object.
     *
     * @param set                     the set name of the academy
     * @param name                    the name of the academy
     * @param type                    the type of academy (used by autoAwards)
     * @param isMilitary              indicates if the academy is a military academy
     *                                (true) or not (false)
     * @param isReeducationCamp       indicates if the academy is a reeducation camp
     *                                (true) or not (false)
     * @param isPrepSchool            indicates if the academy is focused on
     *                                children (true) or not (false)
     * @param description             the description of the academy
     * @param factionDiscount         the discount offered by the academy to faction
     *                                members
     * @param isFactionRestricted     indicates if the academy is restricted to
     *                                faction members (true) or not (false)
     * @param isLocal                 indicates if the academy is local (true) or
     *                                not (false) (overrides locationSystems)
     * @param isHomeSchool            indicates if the academy is a home school
     *                                (true) or not (false)
     * @param locationSystems         the list of location systems where the academy
     *                                is present
     * @param constructionYear        the year when the academy was constructed
     * @param destructionYear         the year when the academy was destroyed
     * @param tuition                 the tuition fee for attending the academy
     * @param durationDays            the duration of the academy in days
     * @param facultySkill            the skill level of the academy's faculty
     * @param educationLevelMin       the minimum education level required to attend
     *                                the academy
     * @param educationLevelMax       the maximum education level provided by the
     *                                academy
     * @param ageMin                  the minimum age requirement to attend the
     *                                academy
     * @param ageMax                  the maximum age accepted by the academy
     * @param qualifications          the list of qualifications provided by the
     *                                academy
     * @param curriculums             the list of curriculums offered by the academy
     * @param qualificationStartYears the list of years when each qualification
     *                                becomes available
     * @param baseAcademicSkillLevel  the base skill level provided by the academy
     * @param id                      the id number of the academy, used for sorting
     *                                academies in mhq
     */
    public Academy(String set, String name, String type, Boolean isMilitary, Boolean isReeducationCamp,
            Boolean isPrepSchool, String description, Integer factionDiscount, Boolean isFactionRestricted,
            List<String> locationSystems, Boolean isLocal, Boolean isHomeSchool, Integer constructionYear,
            Integer destructionYear, Integer closureYear, Integer tuition, Integer durationDays,
            Integer facultySkill, EducationLevel educationLevelMin, EducationLevel educationLevelMax,
            Integer ageMin, Integer ageMax, List<String> qualifications, List<String> curriculums,
            List<Integer> qualificationStartYears, Integer baseAcademicSkillLevel, Integer id) {
        this.set = set;
        this.name = name;
        this.type = type;
        this.isMilitary = isMilitary;
        this.isReeducationCamp = isReeducationCamp;
        this.isPrepSchool = isPrepSchool;
        this.description = description;
        this.factionDiscount = factionDiscount;
        this.isFactionRestricted = isFactionRestricted;
        this.isLocal = isLocal;
        this.isHomeSchool = isHomeSchool;
        this.locationSystems = locationSystems;
        this.constructionYear = constructionYear;
        this.destructionYear = destructionYear;
        this.closureYear = closureYear;
        this.tuition = tuition;
        this.durationDays = durationDays;
        this.facultySkill = facultySkill;
        this.educationLevelMin = educationLevelMin;
        this.educationLevelMax = educationLevelMax;
        this.ageMin = ageMin;
        this.ageMax = ageMax;
        this.qualifications = qualifications;
        this.curriculums = curriculums;
        this.qualificationStartYears = qualificationStartYears;
        this.baseAcademicSkillLevel = baseAcademicSkillLevel;
        this.id = id;
    }

    /**
     * Retrieves the value of the "set" property for academies.
     *
     * @return The value of the "set" property.
     */
    public String getSet() {
        return set;
    }

    /**
     * Sets the value of the set variable for academies.
     *
     * @param set the value to be assigned to the set variable
     */
    public void setSet(String set) {
        this.set = set;
    }

    /**
     * Retrieves the name of the academy.
     *
     * @return the name of the academy
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the academy.
     *
     * @param name the new name to be set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Gets the type of academy.
     *
     * @return The type of academy.
     */
    public AcademyType getType() {
        return AcademyType.parseFromString(type);
    }

    /**
     * Sets the type of academy.
     *
     * @param type the type to be set.
     */
    public void setType(final String type) {
        this.type = type;
    }

    /**
     * Checks if the academy is a military academy.
     *
     * @return {@code true} if the academy is a military academy, {@code false}
     *         otherwise.
     */
    public Boolean isMilitary() {
        return isMilitary;
    }

    /**
     * Sets the value indicating whether the academy is a military academy.
     *
     * @param isMilitary true if the academy is military, false otherwise.
     */
    public void setIsMilitary(final boolean isMilitary) {
        this.isMilitary = isMilitary;
    }

    /**
     * Determines if the academy is a reeducation camp.
     *
     * @return true, if the academy is a reeducation camp; otherwise, false.
     */
    public boolean isReeducationCamp() {
        return isReeducationCamp;
    }

    /**
     * Checks if the academy is a Prep School.
     *
     * @return {@code true} if the academy is a Prep School, {@code false}
     *         otherwise.
     */
    public Boolean isPrepSchool() {
        return isPrepSchool;
    }

    /**
     * Checks if the academy is a local academy.
     *
     * @return {@code true} if the academy is a local academy, {@code false}
     *         otherwise.
     */
    public Boolean isLocal() {
        return isLocal;
    }

    /**
     * @return {@code true} if the academy is a home school, {@code false}
     *         otherwise.
     */
    public Boolean isHomeSchool() {
        return isHomeSchool;
    }

    /**
     * Returns the description of the academy.
     *
     * @return The description of the academy.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the academy.
     *
     * @param description the new description for the academy
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Retrieves the list of location systems where the academy is present.
     *
     * @return The list of location systems as a List of String.
     */
    public List<String> getLocationSystems() {
        return locationSystems;
    }

    /**
     * Sets the location systems for the academy.
     *
     * @param locationSystems the list of location systems to be set
     */
    public void setLocationSystems(final List<String> locationSystems) {
        this.locationSystems = locationSystems;
    }

    /**
     * Returns the academy's construction year.
     *
     * @return the academy's construction year as an Integer value.
     */
    public Integer getConstructionYear() {
        return constructionYear;
    }

    /**
     * Retrieves the academy's destruction year.
     *
     * @return The academy's destruction year, represented as an Integer.
     */
    public Integer getDestructionYear() {
        return destructionYear;
    }

    /**
     * Retrieves the closure year of an academy.
     *
     * @return The closure year as an Integer.
     */
    public Integer getClosureYear() {
        return closureYear;
    }

    /**
     * Retrieves the minimum age allowed at the academy.
     *
     * @return the minimum age allowed as an Integer.
     */
    public Integer getAgeMin() {
        return ageMin;
    }

    /**
     * Retrieves the maximum age allowed at the academy.
     *
     * @return the maximum age allowed as an Integer.
     */
    public Integer getAgeMax() {
        return ageMax;
    }

    /**
     * Retrieves the value of the academy's tuition.
     *
     * @return the academy's tuition value as an Integer.
     */
    public Integer getTuition() {
        return tuition;
    }

    /**
     * Sets the tuition value.
     *
     * @param tuition the new tuition value to be set
     */
    public void setTuition(final Integer tuition) {
        this.tuition = tuition;
    }

    /**
     * Retrieves course duration (in days).
     *
     * @return The course duration in days as an Integer.
     */
    public Integer getDurationDays() {
        return durationDays;
    }

    /**
     * Retrieves the academy's faction discount value.
     *
     * @return The academy's discount value for the faction.
     */

    public Integer getFactionDiscount() {
        return factionDiscount;
    }

    /**
     * Sets the faction discount raw value.
     *
     * @param factionDiscount the faction discount to set
     */

    public void setFactionDiscount(final Integer factionDiscount) {
        this.factionDiscount = factionDiscount;
    }

    /**
     * Retrieves faculty skill level.
     *
     * @return The faculty skill level as an Integer.
     */
    public Integer getFacultySkill() {
        return facultySkill;
    }

    /**
     * Returns the minimum academic tier value.
     *
     * @return The minimum academic tier value as an Integer.
     */
    public EducationLevel getEducationLevelMin() {
        return educationLevelMin;
    }

    /**
     * Sets the minimum education level required for admission.
     *
     * @param educationLevelMin the minimum education level required, as an Integer
     */
    public void setEducationLevelMin(final EducationLevel educationLevelMin) {
        this.educationLevelMin = educationLevelMin;
    }

    /**
     * Sets the maximum education level provided by the academy.
     *
     * @param educationLevelMax the maximum education level to be set
     */
    public void setEducationLevelMax(final EducationLevel educationLevelMax) {
        this.educationLevelMax = educationLevelMax;
    }

    /**
     * Returns the list of qualification names.
     *
     * @return the list of qualification names.
     */
    public List<String> getQualifications() {
        return qualifications;
    }

    /**
     * Retrieves the skills improved by this academy.
     *
     * @return The skills improved by this academy as a String.
     */
    public List<String> getCurriculums() {
        return curriculums;
    }

    /**
     * Retrieves the skills improved by this academy.
     *
     * @return The skills improved by this academy as a String.
     */
    public List<Integer> getQualificationStartYears() {
        return qualificationStartYears;
    }

    /**
     * Retrieves the base skill level granted by this academy.
     *
     * @return the base skill level granted by this academy as an Integer
     */
    public Integer getBaseAcademicSkillLevel() {
        return baseAcademicSkillLevel;
    }

    /**
     * Retrieves the ID of the academy.
     *
     * @return the ID of the academy
     */
    public Integer getId() {
        return id;
    }

    /**
     * Sets the id of the academy.
     *
     * @param id The id to be set.
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Compares an academy with this one by priority: xp, edge and name. Used for
     * sorting.
     *
     * @param other academy to be compared
     * @return int used for sorting
     */
    @Override
    public int compareTo(Academy other) {
        return Integer.compare(this.id, other.id);
    }

    /**
     * Retrieves the adjusted value of the academy's tuition based on the specified
     * tier minimum and education level.
     *
     * @param person the person for whom the tuition is being calculated
     * @return the adjusted tuition value as an Integer
     */
    public int getTuitionAdjusted(Person person) {
        double educationLevel = Math.max(1,
                getEducationLevel(person) - (EducationLevel.parseToInt(educationLevelMin) / 4));

        return (int) (tuition * educationLevel);
    }

    /**
     * Calculates the adjusted faction discount for a given person in a campaign.
     *
     * @param campaign the campaign the person belongs to
     * @param person   the person receiving the discount
     * @return the faction discount as a double value, between 0.00 and 1.00
     */
    public Double getFactionDiscountAdjusted(Campaign campaign, Person person) {
        if (isFactionRestricted) {
            return 1.00;
        }

        List<String> campuses = isLocal ? Collections.singletonList(campaign.getCurrentSystem().getId())
                : locationSystems;

        Set<String> relevantFactions = new HashSet<>();
        relevantFactions.add(campaign.getFaction().getShortName());
        relevantFactions.add(person.getOriginFaction().getShortName());

        for (String campus : campuses) {
            List<String> factions = campaign.getSystemById(campus).getFactions(campaign.getLocalDate());

            if (Collections.disjoint(factions, relevantFactions)) {
                return 1.00;
            } else {
                return 1 - ((double) factionDiscount / 100);
            }
        }

        return 1.00;
    }

    /**
     * Retrieves the first Faction not in conflict with person's Faction or the
     * campaign Faction.
     *
     * @param campaign The campaign being played.
     * @param person   The person for whom to filter the faction.
     * @param factions The factions to check eligibility against.
     * @return The filtered faction for the local campus, or null if no faction is
     *         found.
     */
    public String getFilteredFaction(Campaign campaign, Person person, List<String> factions) {
        if (factions.isEmpty()) {
            return null;
        }

        Faction originFaction = person.getOriginFaction();
        Faction campaignFaction = campaign.getFaction();
        FactionHints hints = RandomFactionGenerator.getInstance().getFactionHints();

        for (String shortName : factions) {
            Faction faction = Factions.getInstance().getFaction(shortName);

            if (isFactionRestricted) {
                if (faction.equals(originFaction) || faction.equals(campaignFaction)) {
                    return faction.getShortName();
                }

                return null;
            }

            // For reeducation camps we only care about whether the campaign faction is at war
            if (isReeducationCamp) {
                if (!hints.isAtWarWith(campaignFaction, faction, campaign.getLocalDate())) {
                    return faction.getShortName();
                }
            } else {
                if (!hints.isAtWarWith(originFaction, faction, campaign.getLocalDate())
                    || !hints.isAtWarWith(campaignFaction, faction, campaign.getLocalDate())) {
                    return faction.getShortName();
                }
            }
        }

        return null;
    }

    /**
     * Checks if a person is qualified to enroll based on their highest education
     * level.
     *
     * @param person The person to check qualification for.
     * @return True, if the person's highest education level is greater than or
     *         equal to the minimum education level required, false otherwise.
     */
    public boolean isQualified(Person person) {
        return EducationLevel.parseToInt(person.getEduHighestEducation()) >= EducationLevel
                .parseToInt(educationLevelMin);
    }

    /**
     * Checks if a person has a rejected application for a specific academy.
     *
     * @param person the person for whom to check the rejected applications
     * @return true if the person has a rejected application for the given academy,
     *         false otherwise
     */
    public boolean hasRejectedApplication(Person person) {
        return person.getEduFailedApplications().contains(name + "::" + getEducationLevel(person));
    }

    /**
     * Calculates the education level of a qualification based on the applicant's
     * highest prior education level and
     * the range of education levels offered by the academy.
     *
     * @param person The person whose education level needs to be determined.
     * @return The education level of the qualification.
     */
    public int getEducationLevel(Person person) {
        int currentEducationLevel = EducationLevel.parseToInt(person.getEduHighestEducation());
        int minimumEducationLevel = EducationLevel.parseToInt(educationLevelMin);
        int maximumEducationLevel = EducationLevel.parseToInt(educationLevelMax);

        int educationLevel;

        if ((currentEducationLevel + minimumEducationLevel) >= maximumEducationLevel) {
            educationLevel = maximumEducationLevel;
        } else if ((currentEducationLevel + minimumEducationLevel) == 0) {
            educationLevel = currentEducationLevel + minimumEducationLevel + 1;
        } else {
            educationLevel = currentEducationLevel + minimumEducationLevel;
        }

        // this probably isn't necessary, but a little insurance goes a long way
        if (educationLevel > EducationLevel.values().length - 1) {
            educationLevel = EducationLevel.values().length - 1;
        } else if (educationLevel < 0) {
            educationLevel = 0;
        }

        return educationLevel;
    }

    /**
     * Checks if there is a conflict between the factions related to the academy and
     * person or campaign.
     *
     * @param campaign The campaign to check faction conflict with.
     * @param person   The person to check the faction conflict with.
     * @return true if there is a faction conflict, false otherwise.
     */
    public Boolean isFactionConflict(Campaign campaign, Person person) {
        // Reeducation camps only care if they're at war with the campaign faction
        if (isReeducationCamp) {
            return RandomFactionGenerator.getInstance().getFactionHints().isAtWarWith(campaign.getFaction(),
                Factions.getInstance().getFaction(person.getEduAcademyFaction()), campaign.getLocalDate());
        }

        // is there a conflict between academy faction & person's faction?
        if (RandomFactionGenerator.getInstance().getFactionHints().isAtWarWith(person.getOriginFaction(),
                Factions.getInstance().getFaction(person.getEduAcademyFaction()), campaign.getLocalDate())) {
            return true;
            // is there a conflict between academy faction & campaign faction?
        } else {
            return RandomFactionGenerator.getInstance().getFactionHints().isAtWarWith(campaign.getFaction(),
                    Factions.getInstance().getFaction(person.getEduAcademyFaction()), campaign.getLocalDate());
        }
    }

    /**
     * Returns the nearest campus to a given campaign.
     *
     * @param campaign the campaign for which to find the nearest campus
     * @param campuses a list of campuses to consider
     * @return the nearest campus to the campaign
     */
    public static String getNearestCampus(Campaign campaign, List<String> campuses) {
        int distance = 999999999;
        String nearestCampus = "";

        for (String campus : campuses) {
            int travelTime = campaign.getSimplifiedTravelTime(campaign.getSystemById(campus));

            if (travelTime < distance) {
                distance = travelTime;
                nearestCampus = campus;
            }
        }

        return nearestCampus;
    }

    /**
     * Retrieves the tooltip for an academy, based on the number of persons in
     * 'personnel'
     *
     * @param campaign    The campaign to retrieve the tooltip for.
     * @param personnel   The list of personnel.
     * @param courseIndex The index of the course.
     * @param destination The campus.
     * @return The tooltip as a String.
     */
    public String getTooltip(Campaign campaign, List<Person> personnel, int courseIndex, PlanetarySystem destination) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education",
                MekHQ.getMHQOptions().getLocale());

        StringBuilder tooltip = new StringBuilder().append("<html><body style='width: 200px'>");
        tooltip.append("<i>").append(description).append("</i><br><br>");
        tooltip.append("<b>").append(resources.getString("curriculum.text")).append("</b><br>");

        Person person = personnel.get(0);

        int educationLevel = 0;

        if (personnel.size() == 1) {
            educationLevel = getEducationLevel(person) + baseAcademicSkillLevel;
        }

        // here we display the skills
        String[] skills = curriculums.get(courseIndex).split(",");

        skills = Arrays.stream(skills)
                .map(String::trim)
                .toArray(String[]::new);

        if (personnel.size() == 1) {
            for (String skill : skills) {
                if (skill.equalsIgnoreCase("none")) {
                    tooltip.append(skill).append("<br>");
                    continue;
                } else {
                    tooltip.append(skill).append(" (");
                }

                if (skill.equalsIgnoreCase("xp")) {
                    if (EducationLevel.parseToInt(person.getEduHighestEducation()) >= educationLevel) {
                        tooltip.append(resources.getString("nothingToLearn.text")).append(")<br>");
                    } else {
                        tooltip.append(educationLevel * campaign.getCampaignOptions().getCurriculumXpRate())
                                .append(")<br>");
                    }
                } else {
                    String skillParsed = skillParser(skill);

                    if ((person.hasSkill(skillParsed))
                            && (person.getSkill(skillParsed).getExperienceLevel() >= educationLevel)) {
                        tooltip.append(resources.getString("nothingToLearn.text")).append(")<br>");
                    } else {
                        tooltip.append(SkillType.getExperienceLevelName(educationLevel)).append(")<br>");
                    }
                }
            }
        } else {
            for (String skill : skills) {
                tooltip.append(skill).append("<br>");
            }
        }

        tooltip.append("<br>");

        // with the skill content resolved, we can move onto the rest of the tooltip
        if (!isLocal && !isHomeSchool) {
            int targetNumber = campaign.getCampaignOptions().getEntranceExamBaseTargetNumber() - facultySkill;
            tooltip.append("<b>").append(resources.getString("entranceExam.text"))
                .append("</b> ").append(' ').append(targetNumber).append("+<br>");
        }

        if (personnel.size() == 1) {
            tooltip.append("<b>").append(resources.getString("tuition.text")).append("</b> ")
                    .append(getTuitionAdjusted(person) * getFactionDiscountAdjusted(campaign, person)).append(" CSB")
                    .append("<br>");
        }

        if (isPrepSchool) {
            tooltip.append("<b>").append(resources.getString("duration.text"))
                    .append("</b> ").append(' ').append(String.format(resources.getString("durationAge.text"), ageMax))
                    .append("<br>");
        } else {
            tooltip.append("<b>").append(resources.getString("duration.text")).append("</b> ");

            tooltip.append(durationDays).append(' ').append(resources.getString("durationDays.text")).append("<br>");
        }

        // we need to do a little extra work to get travel time, to cover academies with
        // multiple campuses
        if (!isHomeSchool) {
            int distance = campaign.getSimplifiedTravelTime(destination);

            tooltip.append("<b>").append(resources.getString("distance.text")).append("</b> ");

            tooltip.append(distance).append(' ').append(resources.getString("durationDays.text"));

            tooltip.append(" (").append(destination.getName(campaign.getLocalDate())).append(")<br>");
        }

        // with travel time out the way, all that's left is to add the last couple of
        // entries
        if ((isReeducationCamp) && (campaign.getCampaignOptions().isUseReeducationCamps())) {
            tooltip.append("<b>").append(resources.getString("reeducation.text")).append("</b> ");

            if (personnel.size() == 1) {
                if (!Objects.equals(person.getOriginFaction().getShortName(), campaign.getFaction().getShortName())) {
                    tooltip.append(campaign.getFaction().getFullName(campaign.getGameYear())).append("<br>");
                } else {
                    tooltip.append(resources.getString("reeducationNoChange.text")).append("<br>");
                }
            } else {
                tooltip.append(campaign.getFaction().getFullName(campaign.getGameYear())).append("<br>");
            }

            tooltip.append("<br>");
        }

        tooltip.append("<b>").append(resources.getString("facultySkill.text")).append("</b> ")
                .append(facultySkill).append('+').append("<br>");

        if (personnel.size() == 1) {
            tooltip.append("<b>").append(resources.getString("educationLevel.text")).append("</b> ")
                    .append(EducationLevel.parseFromInt(getEducationLevel(person))).append("<br>");
        }

        return tooltip.append("</html>").toString();
    }

    /**
     * Parses a given skill string and returns the corresponding skill type.
     *
     * @param skill the skill string to parse
     * @return the corresponding skill code as a string
     * @throws IllegalStateException if the skill string is unexpected or invalid
     */
    public static String skillParser(String skill) {
        return switch (skill.toLowerCase().trim()) {
            case "piloting/mek" -> SkillType.S_PILOT_MEK;
            case "gunnery/mek" -> SkillType.S_GUN_MEK;
            case "piloting/aerospace" -> SkillType.S_PILOT_AERO;
            case "gunnery/aerospace" -> SkillType.S_GUN_AERO;
            case "piloting/ground vehicle" -> SkillType.S_PILOT_GVEE;
            case "piloting/vtol" -> SkillType.S_PILOT_VTOL;
            case "piloting/naval" -> SkillType.S_PILOT_NVEE;
            case "gunnery/vehicle" -> SkillType.S_GUN_VEE;
            case "piloting/aircraft" -> SkillType.S_PILOT_JET;
            case "gunnery/aircraft" -> SkillType.S_GUN_JET;
            case "piloting/spacecraft" -> SkillType.S_PILOT_SPACE;
            case "gunnery/spacecraft" -> SkillType.S_GUN_SPACE;
            case "artillery" -> SkillType.S_ARTILLERY;
            case "gunnery/battlearmor" -> SkillType.S_GUN_BA;
            case "gunnery/protomek" -> SkillType.S_GUN_PROTO;
            case "small arms" -> SkillType.S_SMALL_ARMS;
            case "anti-mek" -> SkillType.S_ANTI_MEK;
            case "tech/mek" -> SkillType.S_TECH_MEK;
            case "tech/mechanic" -> SkillType.S_TECH_MECHANIC;
            case "tech/aero" -> SkillType.S_TECH_AERO;
            case "tech/battlearmor" -> SkillType.S_TECH_BA;
            case "tech/vessel" -> SkillType.S_TECH_VESSEL;
            case "astech" -> SkillType.S_ASTECH;
            case "doctor" -> SkillType.S_DOCTOR;
            case "medtech" -> SkillType.S_MEDTECH;
            case "hyperspace navigation" -> SkillType.S_NAV;
            case "administration" -> SkillType.S_ADMIN;
            case "tactics" -> SkillType.S_TACTICS;
            case "strategy" -> SkillType.S_STRATEGY;
            case "negotiation" -> SkillType.S_NEG;
            case "leadership" -> SkillType.S_LEADER;
            case "scrounge" -> SkillType.S_SCROUNGE;

            // <50.01 compatibility handlers
            case "piloting/mech" -> SkillType.S_PILOT_MEK;
            case "gunnery/mech" -> SkillType.S_GUN_MEK;
            case "gunnery/battlesuit" -> SkillType.S_GUN_BA;
            case "gunnery/protomech" -> SkillType.S_GUN_PROTO;
            case "anti-mech" -> SkillType.S_ANTI_MEK;
            case "tech/mech" -> SkillType.S_TECH_MEK;
            case "tech/ba" -> SkillType.S_TECH_BA;

            default -> throw new IllegalStateException("Unexpected skill in skillParser(): " + skill);
        };
    }
}
