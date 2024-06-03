/*
 * Copyright (c) 2018-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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
package mekhq.campaign.personnel.education;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.RandomFactionGenerator;

import java.util.*;

/**
 * The Academy class represents an academy with various properties and methods.
 */
@XmlRootElement(name = "academy")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class Academy implements Comparable<Academy> {
    @XmlElement(name = "name")
    private String name = "Error: Name Missing";

    // 0: none
    // 1: high school
    // 2: college
    // 3: university
    // 4: military academy
    // 5: basic training
    // 6: nco academy
    // 7: warrant officer academy
    // 8: officer academy
    @XmlElement(name = "type")
    private int type = 0;

    @XmlElement(name = "isMilitary")
    private Boolean isMilitary = false;

    @XmlElement(name = "isReeducationCamp")
    private Boolean isReeducationCamp = false;

    @XmlElement(name = "promotion")
    private String promotion = "None";

    @XmlElement(name = "isPrepSchool")
    private Boolean isPrepSchool = false;

    @XmlElement(name = "isClan")
    private Boolean isClan = false;

    @XmlElement(name = "isTrueborn")
    private Boolean isTrueborn = false;

    @XmlElement(name = "description")
    private String description = "Error: no description";

    @XmlElement(name = "factionDiscount")
    private Integer factionDiscount = 10;

    @XmlElement(name = "isFactionRestricted")
    private Boolean isFactionRestricted = false;

    @XmlElement(name = "faction")
    private String faction = "CS";

    @XmlElement(name = "isLocal")
    private Boolean isLocal = false;

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
    private Integer durationDays = 10;

    @XmlElement(name = "facultySkill")
    private Integer facultySkill = 7;

    // 0 = early childhood <= 10
    // 1 = (11-16) high school
    // 2 = college / university
    // 3 = post-grad
    // 4 = doctorate
    @XmlElement(name = "educationLevelMin")
    private Integer educationLevelMin = 0;

    @XmlElement(name = "educationLevelMax")
    private Integer educationLevelMax = 1;

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
     * This class provides a no-arg constructor, which is needed for unmarshalling of XML.
     */
    public Academy() {
    }

    /**
     * Constructs a new Academy object.
     *
     * @param set                     the set name of the academy
     * @param name                    the name of the academy
     * @param type                    the type of academy (used by autoAwards)
     * @param isMilitary              indicates if the academy is a military academy (true) or not (false)
     * @param isReeducationCamp       indicates if the academy is a reeducation camp (true) or not (false)
     * @param promotion               indicates the promotion rank earned for completing an academic course
     * @param isClan                  indicates if the academy is a Clan Sibko or Crèche (true) or not (false)
     * @param isTrueborn              indicates if the Sibko is intended for Trueborn (true) or not (false)
     * @param isPrepSchool            indicates if the academy is focused on children (true) or not (false)
     * @param description             the description of the academy
     * @param factionDiscount         the discount offered by the academy to faction members
     * @param isFactionRestricted     indicates if the academy is restricted to faction members (true) or not (false)
     * @param faction                 the faction code the academy is restricted to
     * @param isLocal                 indicates if the academy is local (true) or not (false) (overrides locationSystems)
     * @param locationSystems         the list of location systems where the academy is present
     * @param constructionYear        the year when the academy was constructed
     * @param destructionYear         the year when the academy was destroyed
     * @param tuition                 the tuition fee for attending the academy
     * @param durationDays            the duration of the academy in days
     * @param facultySkill            the skill level of the academy's faculty
     * @param educationLevelMin       the minimum education level required to attend the academy
     * @param educationLevelMax       the maximum education level provided by the academy
     * @param ageMin                  the minimum age requirement to attend the academy
     * @param ageMax                  the maximum age accepted by the academy
     * @param qualifications          the list of qualifications provided by the academy
     * @param curriculums             the list of curriculums offered by the academy
     * @param qualificationStartYears the list of years when each qualification becomes available
     * @param baseAcademicSkillLevel  the base skill level provided by the academy
     * @param id                      the id number of the academy, used for sorting academies in mhq
     */
    public Academy(String set, String name, int type, Boolean isMilitary, Boolean isReeducationCamp, String promotion,
                   Boolean isClan, Boolean isTrueborn, Boolean isPrepSchool, String description, Integer factionDiscount,
                   Boolean isFactionRestricted, String faction, List<String> locationSystems, Boolean isLocal,
                   Integer constructionYear, Integer destructionYear, Integer closureYear, Integer tuition,
                   Integer durationDays, Integer facultySkill, Integer educationLevelMin, Integer educationLevelMax,
                   Integer ageMin, Integer ageMax, List<String> qualifications, List<String> curriculums,
                   List<Integer> qualificationStartYears, Integer baseAcademicSkillLevel, Integer id) {
        this.set = set;
        this.name = name;
        this.type = type;
        this.isMilitary = isMilitary;
        this.isReeducationCamp = isReeducationCamp;
        this.promotion = promotion;
        this.isClan = isClan;
        this.isTrueborn = isTrueborn;
        this.isPrepSchool = isPrepSchool;
        this.description = description;
        this.factionDiscount = factionDiscount;
        this.isFactionRestricted = isFactionRestricted;
        this.faction = faction;
        this.isLocal = isLocal;
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
    public int getType() {
        return type;
    }

    /**
     * Sets the type of academy.
     *
     * @param type the type to be set.
     */
    public void setType(final int type) {
        this.type = type;
    }

    /**
     * Checks if the academy is a military academy.
     *
     * @return {@code true} if the academy is a military academy, {@code false} otherwise.
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
     * Sets the value of the isReeducationCamp property.
     *
     * @param isReeducationCamp the new value for the isReeducationCamp property
     */
    public void setIsReeducationCamp(final boolean isReeducationCamp) {
        this.isReeducationCamp = isReeducationCamp;
    }

    /**
     * Retrieves the promotion awarded for completing an academic course.
     *
     * @return the promotion awarded for completing an academic course
     */
    public String getPromotion() {
        return promotion;
    }

    /**
     * Sets the promotion for the academy.
     *
     * @param promotion the promotion to be set
     */
    public void setPromotion(final String promotion) {
        this.promotion = promotion;
    }

    /**
     * Checks if the academy is a Clan Sibko or Crèche.
     *
     * @return {@code true} if the academy is a Clan Sibko or Crèche, {@code false} otherwise.
     */
    public Boolean isClan() {
        return isClan;
    }

    /**
     * Sets the value indicating whether the academy is a Clan Sibko or Crèche.
     *
     * @param isClan true if the academy is a Clan Sibko or Crèche, false otherwise.
     */
    public void setIsClan(final boolean isClan) {
        this.isClan = isClan;
    }

    /**
     * Checks if the academy is a Truenorn Sibko or Crèche.
     *
     * @return {@code true} if the academy is a Trueborn Sibko or Crèche, {@code false} otherwise.
     */
    public Boolean isTrueborn() {
        return isTrueborn;
    }

    /**
     * Sets the value indicating whether the academy is a Trueborn Sibko or Crèche.
     *
     * @param isTrueborn true if the academy is a Trueborn Sibko or Crèche, false otherwise.
     */
    public void setIsTrueborn(final boolean isTrueborn) {
        this.isTrueborn = isTrueborn;
    }

    /**
     * Checks if the academy is a Prep School.
     *
     * @return {@code true} if the academy is a Prep School, {@code false} otherwise.
     */
    public Boolean isPrepSchool() {
        return isPrepSchool;
    }

    /**
     * Sets the value indicating whether the academy is a prep school.
     *
     * @param isPrepSchool true if the academy is a prep school, false otherwise.
     */
    public void setIsPrepSchool(final boolean isPrepSchool) {
        this.isPrepSchool = isPrepSchool;
    }

    /**
     * Checks if the academy is a local academy.
     *
     * @return {@code true} if the academy is a local academy, {@code false} otherwise.
     */
    public Boolean isLocal() {
        return isLocal;
    }

    /**
     * Sets the value indicating whether the academy is local.
     *
     * @param isLocal true if the academy is local, false otherwise.
     */
    public void setIsLocal(final boolean isLocal) {
        this.isLocal = isLocal;
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
     * Sets the construction year of the academy.
     *
     * @param constructionYear the construction year to be set
     */
    public void setConstructionYear(final Integer constructionYear) {
        this.constructionYear = constructionYear;
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
     * Sets the destruction year of the academy.
     *
     * @param destructionYear the destruction year to be set
     */
    public void setDestructionYear(final Integer destructionYear) {
        this.destructionYear = destructionYear;
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
     * Sets the closure year of the academy.
     *
     * @param closureYear the closure year to be set
     */
    public void setClosureYear(final Integer closureYear) {
        this.closureYear = closureYear;
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
     * Sets the minimum age.
     *
     * @param ageMin the minimum age to set
     */
    public void setAgeMin(final Integer ageMin) {
        this.ageMin = ageMin;
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
     * Sets the maximum age.
     *
     * @param ageMax the maximum age to be set
     */
    public void setAgeMax(final Integer ageMax) {
        this.ageMax = ageMax;
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
     * Sets the course duration in days.
     *
     * @param durationDays the duration in days to set
     */
    public void setDurationDays(final Integer durationDays) {
        this.durationDays = durationDays;
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
     * Checks if the academy is faction restricted.
     *
     * @return true if the academy is faction restricted, otherwise false.
     */
    public Boolean isFactionRestricted() {
        return isFactionRestricted;
    }

    /**
     * Sets whether the academy is faction restricted.
     *
     * @param isFactionRestricted true if the academy is faction restricted, false otherwise
     */
    public void setIsFactionRestricted(final Boolean isFactionRestricted) {
        this.isFactionRestricted = isFactionRestricted;
    }

    /**
     * Retrieves the faction associated with an academy.
     *
     * @return The faction of the academy.
     */
    public String getFaction() {
        return faction;
    }

    /**
     * Sets the faction of an academy.
     *
     * @param faction the faction to be set
     */
    public void setFaction(final String faction) {
        this.faction = faction;
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
     * Sets the skill level of the faculty.
     *
     * @param facultySkill the skill level of the faculty to be set
     */
    public void setFacultySkill(final Integer facultySkill) {
        this.facultySkill = facultySkill;
    }

    /**
     * Returns the minimum academic tier value.
     *
     * @return The minimum academic tier value as an Integer.
     */
    public Integer getEducationLevelMin() {
        return educationLevelMin;
    }

    /**
     * Sets the minimum education level required for admission.
     *
     * @param educationLevelMin the minimum education level required, as an Integer
     */
    public void setEducationLevelMin(final Integer educationLevelMin) {
        this.educationLevelMin = educationLevelMin;
    }

    /**
     * Retrieves the maximum academic tier value.
     *
     * @return The maximum academic tier value as an Integer
     */
    public Integer getEducationLevelMax() {
        return educationLevelMax;
    }

    /**
     * Sets the maximum education level provided by the academy.
     *
     * @param educationLevelMax the maximum education level to be set
     */
    public void setEducationLevelMax(final Integer educationLevelMax) {
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
     * Sets the qualifications for the academy.
     *
     * @param qualifications a list of strings representing the qualifications to set
     */
    public void setQualifications(final List<String> qualifications) {
        this.qualifications = qualifications;
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
     * Sets the curriculums for the academy.
     *
     * @param curriculums The list of curriculums to be set.
     */
    public void setCurriculums(final List<String> curriculums) {
        this.curriculums = curriculums;
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
     * Sets the qualification start years.
     *
     * @param qualificationStartYears the list of qualification start years to be set
     */
    public void setQualificationStartYears(List<Integer> qualificationStartYears) {
        this.qualificationStartYears = qualificationStartYears;
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
     * Sets the base academic skill level.
     *
     * @param baseAcademicSkillLevel the new base academic skill level
     */
    public void setBaseAcademicSkillLevel(final Integer baseAcademicSkillLevel) {
        this.baseAcademicSkillLevel = baseAcademicSkillLevel;
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
     * Compares an academy with this one by priority: xp, edge and name. Used for sorting.
     * @param other academy to be compared
     * @return int used for sorting
     */
    @Override
    public int compareTo(Academy other) {
        return Integer.compare(this.id, other.id);
    }

    /**
     * Retrieves the adjusted value of the academy's tuition based on the specified tier minimum and education level.
     *
     * @param person        the person for whom the tuition is being calculated
     * @return the adjusted tuition value as an Integer
     */
    public Integer getTuitionAdjusted(Person person) {
        if (getEducationLevel(person) < 2) {
            return tuition;
        } else {
            return tuition * getEducationLevel(person);
        }
    }

    /**
     * Calculates the adjusted faction discount for a given person in a campaign.
     *
     * @param campaign the campaign the person belongs to
     * @param person the person receiving the discount
     * @return the faction discount as a double value, between 0.00 and 1.00
     */
    public Double getFactionDiscountAdjusted(Campaign campaign, Person person) {
        List<String> locations;

        if (isLocal) {
            locations = Collections.singletonList(campaign.getCurrentSystem().getId());
        } else {
            locations = locationSystems;
        }

        if (locations.stream()
                .flatMap(campus -> campaign.getSystemById(campus)
                        .getFactions(campaign.getLocalDate())
                        .stream())
                .anyMatch(faction -> faction.equalsIgnoreCase(person.getOriginFaction().getShortName()))) {
            return (double) (factionDiscount / 100);
        }

        return 1.00;
    }

    /**
     * Retrieves the first Faction not in conflict with person's Faction or the campaign Faction.
     *
     * @param campaign The campaign being played.
     * @param person The person for whom to filter the faction.
     * @param system The system in which the local campus is located.
     * @return The filtered faction for the local campus, or null if no faction is found.
     */
    public String getFilteredFaction(Campaign campaign, Person person, String system) {
        List<String> systemOwners = campaign.getSystemById(system).getFactions(campaign.getLocalDate());

        for (String faction : systemOwners) {
            if ((isFactionRestricted) && (Objects.equals(campaign.getFaction().getShortName(), faction))) {
                return Factions.getInstance().getFaction(faction).getShortName();
            } else if ((isFactionRestricted) && (Objects.equals(person.getOriginFaction().getShortName(), faction))) {
                return Factions.getInstance().getFaction(faction).getShortName();
            } else if (!RandomFactionGenerator.getInstance().getFactionHints()
                    .isAtWarWith(person.getOriginFaction(), Factions.getInstance().getFaction(faction),
                            campaign.getLocalDate())) {
                return Factions.getInstance().getFaction(faction).getShortName();
            } else if (!RandomFactionGenerator.getInstance().getFactionHints()
                    .isAtWarWith(campaign.getFaction(), Factions.getInstance().getFaction(faction),
                            campaign.getLocalDate())) {
                return Factions.getInstance().getFaction(faction).getShortName();
            }
        }
        return null;
    }

    /**
     * Checks if a person is qualified to enroll based on their highest education level.
     *
     * @param person The person to check qualification for.
     * @return True, if the person's highest education level is greater than or equal to the minimum education level required, false otherwise.
     */
    public boolean isQualified(Person person) {
        return person.getEduHighestEducation() >= educationLevelMin;
    }

    /**
     * Calculates the education level of a qualification based on the applicant's highest prior education level and
     * the range of education levels offered by the academy.
     *
     * @param person  The person whose education level needs to be determined.
     * @return The education level of the qualification.
     */
    public int getEducationLevel(Person person) {
        int educationLevel;

        if ((person.getEduHighestEducation() + educationLevelMin) >= educationLevelMax) {
            educationLevel = educationLevelMax;
        } else if ((person.getEduHighestEducation() + educationLevelMin) == 0) {
            educationLevel = person.getEduHighestEducation() + educationLevelMin + 1;
        } else {
            educationLevel = person.getEduHighestEducation() + educationLevelMin;
        }

        if (educationLevel > 10) {
            educationLevel = 10;
        } else if (educationLevel < 0) {
            educationLevel = 0;
        }

        return educationLevel;
    }

    /**
     * Checks if there is a conflict between the factions related to the academy and person or campaign.
     *
     * @param campaign The campaign to check faction conflict with.
     * @param person The person to check the faction conflict with.
     * @return true if there is a faction conflict, false otherwise.
     */
    public Boolean isFactionConflict(Campaign campaign, Person person) {
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
     * Returns an HTML formatted string to be used as a tooltip.
     *
     * @param campaign      the current campaign
     * @param person        the person we're generating the tooltip for
     * @param courseIndex   the index of the course in the curriculum
     * @return the tooltip string
     */
    public String getTooltip(Campaign campaign, Person person, int courseIndex, PlanetarySystem destination) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education",
                MekHQ.getMHQOptions().getLocale());

        StringBuilder tooltip = new StringBuilder().append("<html><body style='width: 200px'>");
        tooltip.append("<i>").append(description).append("</i><br><br>");
        tooltip.append("<b>").append(resources.getString("curriculum.text")).append("</b><br>");

        int educationLevel = getEducationLevel(person) + baseAcademicSkillLevel;

        if (educationLevel > 10) {
            educationLevel = 10;
        } else if (educationLevel < 0) {
            educationLevel = 0;
        }

        // here we display the skills
        String[] skills = curriculums.get(courseIndex).split(",");

        skills = Arrays.stream(skills)
                .map(String::trim)
                .toArray(String[]::new);

        for (String skill : skills) {
            tooltip.append(skill).append(" (");

            if (skill.equalsIgnoreCase("xp bonus") || (skill.equalsIgnoreCase("bonus xp"))) {
                tooltip.append(educationLevel).append(resources.getString("xpBonus.text")).append(")<br>");
            } else {
                String skillParsed = skillParser(skill);

                if ((person.hasSkill(skillParsed)) && (person.getSkill(skillParsed).getExperienceLevel() >= educationLevel)) {
                    tooltip.append(resources.getString("nothingToLearn.text")).append(")<br>");
                } else {
                    tooltip.append(SkillType.getExperienceLevelName(educationLevel)).append(")<br>");
                }
            }
        }

        tooltip.append("<br>");

        // with the skill content resolved, we can move onto the rest of the tooltip
        if (tuition * getFactionDiscountAdjusted(campaign, person) > 0) {
            tooltip.append("<b>").append(resources.getString("tuition.text")).append("</b> ")
                    .append(tuition * getFactionDiscountAdjusted(campaign, person)).append(" CSB").append("<br>");
        }
        if (isPrepSchool) {
            tooltip.append("<b>").append(resources.getString("duration.text"))
                    .append("</b> ").append(' ').append(String.format(resources.getString("durationAge.text"), ageMax)).append("<br>");
        } else {
            tooltip.append("<b>").append(resources.getString("duration.text")).append("</b> ");
            if ((durationDays / 7) < 1) {
                tooltip.append(durationDays).append(' ').append(resources.getString("durationDays.text")).append("<br>");
            } else {
                tooltip.append(durationDays / 7).append(' ').append(resources.getString("durationWeeks.text")).append("<br>");
            }
        }

        // we need to do a little extra work to get travel time, to cover academies with multiple campuses or Clan nonsense
        int distance = 2;

        if ((isClan) && (!isLocal)) {
            if (!isTrueborn) {
                try {
                    distance = campaign.getSimplifiedTravelTime(campaign.getFaction().getStartingPlanet(campaign, campaign.getLocalDate()));
                } catch (Exception e) {
                    distance = campaign.getSimplifiedTravelTime(campaign.getSystemById("Strana Mechty"));
                }
            }
        } else {
            distance = campaign.getSimplifiedTravelTime(destination);
        }

        tooltip.append("<b>").append(resources.getString("distance.text")).append("</b> ");

        if ((distance / 7) < 1) {
            tooltip.append(distance).append(' ').append(resources.getString("durationDays.text"));
        } else {
            tooltip.append(distance / 7).append(' ').append(resources.getString("durationWeeks.text"));
        }

        if ((isClan) && (!isLocal)) {
            destination = campaign.getFaction().getStartingPlanet(campaign, campaign.getLocalDate());

            try {
                tooltip.append(" (").append(destination.getName(campaign.getLocalDate())).append(")<br>");
            } catch (Exception e) {
                tooltip.append("Strana Mechty").append(")<br>");
            }
        } else {
            tooltip.append(" (").append(destination.getName(campaign.getLocalDate())).append(")<br>");
        }

        // with travel time out the way; all that's left is to add the last couple of entries
        if ((isMilitary) && (!Objects.equals(promotion, "None"))) {
            tooltip.append("<b>").append(resources.getString("promotion.text")).append("</b> ")
                    .append(promotion).append("<br>");
        }

        if ((isReeducationCamp) && (campaign.getCampaignOptions().isUseReeducationCamps())) {
            tooltip.append("<b>").append(resources.getString("reeducation.text")).append("</b> ");

            if (!Objects.equals(person.getOriginFaction().getShortName(), campaign.getFaction().getShortName())) {
                tooltip.append(campaign.getFaction().getFullName(campaign.getGameYear())).append("<br>");
            } else {
                tooltip.append(resources.getString("reeducationNoChange.text")).append("<br>");
            }

            tooltip.append("<br>");
        }

        tooltip.append("<b>").append(resources.getString("facultySkill.text")).append("</b> ")
                .append(facultySkill).append ('+').append("<br>");
        tooltip.append("<b>").append(resources.getString("educationLevel.text")).append("</b> ")
                .append(getEducationLevel(person)).append("<br>");

        return tooltip.append("</html>").toString();
    }

    /**
     * Parses a given skill string and returns the corresponding skill type.
     *
     * @param skill the skill string to parse
     * @return the corresponding skill code as a string
     * @throws IllegalStateException if the skill string is unexpected or invalid
     */
    static String skillParser(String skill) {
        switch (skill.toLowerCase().trim()) {
            case "piloting/mech":
                return SkillType.S_PILOT_MECH;
            case "gunnery/mech":
                return SkillType.S_GUN_MECH;
            case "piloting/aerospace":
                return SkillType.S_PILOT_AERO;
            case "gunnery/aerospace":
                return SkillType.S_GUN_AERO;
            case "piloting/ground vehicle":
                return SkillType.S_PILOT_GVEE;
            case "piloting/vtol":
                return SkillType.S_PILOT_VTOL;
            case "piloting/naval":
                return SkillType.S_PILOT_NVEE;
            case "gunnery/vehicle":
                return SkillType.S_GUN_VEE;
            case "piloting/aircraft":
                return SkillType.S_PILOT_JET;
            case "gunnery/aircraft":
                return SkillType.S_GUN_JET;
            case "piloting/spacecraft":
                return SkillType.S_PILOT_SPACE;
            case "gunnery/spacecraft":
                return SkillType.S_GUN_SPACE;
            case "artillery":
                return SkillType.S_ARTILLERY;
            case "gunnery/battlesuit":
                return SkillType.S_GUN_BA;
            case "gunnery/protomech":
                return SkillType.S_GUN_PROTO;
            case "small arms":
                return SkillType.S_SMALL_ARMS;
            case "anti-mech":
                return SkillType.S_ANTI_MECH;
            case "tech/mech":
                return SkillType.S_TECH_MECH;
            case "tech/mechanic":
                return SkillType.S_TECH_MECHANIC;
            case "tech/aero":
                return SkillType.S_TECH_AERO;
            case "tech/ba":
                return SkillType.S_TECH_BA;
            case "tech/vessel":
                return SkillType.S_TECH_VESSEL;
            case "astech":
                return SkillType.S_ASTECH;
            case "doctor":
                return SkillType.S_DOCTOR;
            case "medtech":
                return SkillType.S_MEDTECH;
            case "hyperspace navigation":
                return SkillType.S_NAV;
            case "administration":
                return SkillType.S_ADMIN;
            case "tactics":
                return SkillType.S_TACTICS;
            case "strategy":
                return SkillType.S_STRATEGY;
            case "negotiation":
                return SkillType.S_NEG;
            case "leadership":
                return SkillType.S_LEADER;
            case "scrounge":
                return SkillType.S_SCROUNGE;
            default:
                throw new IllegalStateException("Unexpected skill in skillParser(): " + skill);
        }
    }
}
