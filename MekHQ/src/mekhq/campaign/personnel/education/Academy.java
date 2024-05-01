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

import java.util.List;
import java.util.ResourceBundle;

/**
 * This class represents an academy.
 * The following fields are serialized to XML: name, description, locationSystems, constructionYear, destructionYear,
 * tuition, durationDays, tierMin, tierMax, skills, and baseSkillLevel.
 */
@XmlRootElement(name = "academy")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class Academy {
    @XmlElement(name = "name")
    private String name;

    @XmlElement(name = "isMilitary")
    private Boolean isMilitary = false;

    @XmlElement(name = "isPrepSchool")
    private Boolean isPrepSchool = false;

    @XmlElement(name = "isClan")
    private Boolean isClan = false;

    @XmlElement(name = "description")
    private String description;

    @XmlElement(name = "factionDiscount")
    private Integer factionDiscount = 0;

    @XmlElement(name = "isFactionRestricted")
    private Boolean isFactionRestricted = false;

    @XmlElement(name = "isLocal")
    private Boolean isLocal = false;

    @XmlElement(name = "locationSystem")
    private List<String> locationSystems = null;

    @XmlElement(name = "constructionYear")
    private Integer constructionYear = 2300;

    @XmlElement(name = "destructionYear")
    private Integer destructionYear = 9999;

    @XmlElement(name = "closureYear")
    private Integer closureYear = 9999;

    @XmlElement(name = "tuition")
    private Integer tuition;

    @XmlElement(name = "durationDays")
    // this number is chosen so that PrepSchools still experience dropouts
    private Integer durationDays = 11;

    @XmlElement(name = "facultySkill")
    private Integer facultySkill = 12;

    // 0 = early childhood <= 10
    // 1 = 11-16
    // 2 = high school
    // 3 = college / university
    // 4 = post-grad
    // 5 = doctorate
    @XmlElement(name = "educationLevelMin")
    private Integer educationLevelMin;

    @XmlElement(name = "educationLevelMax")
    private Integer educationLevelMax;

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

    @XmlElement(name = "baseSkillLevel")
    private Integer baseSkillLevel;

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
     * @param isMilitary              indicates if the academy is a military academy (true) or not (false)
     * @param isClan                  indicates if the academy is Clan-based (true) or not (false)
     * @param isPrepSchool            indicates if the academy is focused on children (true) or not (false)
     * @param description             the description of the academy
     * @param factionDiscount         the discount offered by the academy to faction members
     * @param isFactionRestricted     indicates if the academy is restricted to faction members (true) or not (false)
     * @param isLocal                 indicates if the academy is local (true) or not (false) (overrides locationSystems)
     * @param locationSystems         the list of location systems where the academy is present
     * @param constructionYear        the year when the academy was constructed
     * @param destructionYear         the year when the academy was destroyed (null if not destroyed)
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
     * @param baseSkillLevel              the base skill level provided by the academy
     */
    public Academy(String set, String name, Boolean isMilitary, Boolean isClan, Boolean isPrepSchool,
                   String description, Integer factionDiscount, Boolean isFactionRestricted,
                   List<String> locationSystems, Boolean isLocal, Integer constructionYear,
                   Integer destructionYear, Integer closureYear, Integer tuition, Integer durationDays,
                   Integer facultySkill, Integer educationLevelMin, Integer educationLevelMax, Integer ageMin,
                   Integer ageMax, List<String> qualifications, List<String> curriculums, List<Integer> qualificationStartYears,
                   Integer baseSkillLevel) {
        this.set = set;
        this.name = name;
        this.isMilitary = isMilitary;
        this.isClan = isClan;
        this.isPrepSchool = isPrepSchool;
        this.description = description;
        this.factionDiscount = factionDiscount;
        this.isFactionRestricted = isFactionRestricted;
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
        this.baseSkillLevel = baseSkillLevel;
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
     * Checks if the academy is a military academy.
     *
     * @return {@code true} if the academy is a military academy, {@code false} otherwise.
     */
    public Boolean isMilitary() {
        return isMilitary;
    }

    /**
     * Checks if the academy is a Clan academy.
     *
     * @return {@code true} if the academy is a Clan academy, {@code false} otherwise.
     */
    public Boolean isClan() {
        return isClan;
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
     * Checks if the academy is a local academy.
     *
     * @return {@code true} if the academy is a local academy, {@code false} otherwise.
     */
    public Boolean isLocal() {
        return isLocal;
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
     * Retrieves the list of location systems where the academy is present.
     *
     * @return The list of location systems as a List of String.
     */
    public List<String> getLocationSystems() {
        return locationSystems;
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
     * Sets the closure year for the specific academy.
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
     * Retrieves the adjusted value of the academy's tuition based on the specified tier minimum and education level.
     *
     * @param tierMin        the minimum education level tier for calculating the adjustment
     * @param educationLevel the education level for which the tuition adjustment is calculated
     * @return the adjusted tuition value as an Integer
     */
    public Integer getTuitionAdjusted(Integer tierMin, Integer educationLevel) {
        return getTuition() * ((educationLevel - tierMin) / 2);
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
    public Integer getFactionDiscountRaw() {
        return factionDiscount;
    }


    /**
     * Calculates the adjusted faction discount for a given person in a campaign.
     *
     * @param campaign the campaign the person belongs to
     * @param person the person receiving the discount
     * @return the faction discount as a double value, between 0.00 and 1.00
     */
    public Double getFactionDiscountAdjusted(Campaign campaign, Person person) {
        if (locationSystems.stream()
                .flatMap(campus -> campaign.getSystemByName(campus)
                        .getFactions(campaign.getLocalDate())
                        .stream())
                .anyMatch(faction -> faction.equalsIgnoreCase(person.getOriginFaction().getShortName()))) {
            return (double) (factionDiscount / 100);
        }

        return 1.00;
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
    public Integer getEducationLevelMin() {
        return educationLevelMin;
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
    public Integer getAcademicBaseSkillLevel() {
        return baseSkillLevel;
    }

    /**
     * Retrieves the filtered faction for a given local campus in a campaign.
     *
     * @param campaign The campaign being played.
     * @param person The person for whom to filter the faction.
     * @param system The system in which the local campus is located.
     * @return The filtered faction for the local campus, or null if no faction is found.
     */
    public String getCampusFilteredFaction(Campaign campaign, Person person, String system) {
        List<String> systemOwners = campaign.getSystemByName(system).getFactions(campaign.getLocalDate());

        for (String faction : systemOwners) {
            if ((isFactionRestricted) && (!faction.equals(person.getOriginFaction().getShortName()))) {
                if (RandomFactionGenerator.getInstance().getFactionHints().isAtWarWith(person.getOriginFaction(),
                        Factions.getInstance().getFaction(faction), campaign.getLocalDate())) {
                    if (RandomFactionGenerator.getInstance().getFactionHints().isAtWarWith(campaign.getFaction(),
                            Factions.getInstance().getFaction(faction), campaign.getLocalDate())) {
                        return faction;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Retrieves a list of campuses filtered by faction restrictions.
     *
     * @param campaign the campaign object containing system and faction information
     * @param person the person object representing the player
     * @return a list of campuses filtered by faction, or null if no campuses are available
     */
    public List<String> getCampusesFilteredByFaction(Campaign campaign, Person person) {
        List<String> campuses = locationSystems;

        for (String campus : campuses) {
            List<String> systemOwners = campaign.getSystemByName(campus).getFactions(campaign.getLocalDate());

            for (String faction : systemOwners) {
                if (RandomFactionGenerator.getInstance().getFactionHints().isAtWarWith(person.getOriginFaction(),
                        Factions.getInstance().getFaction(faction), campaign.getLocalDate())) {
                    campuses.remove(campus);
                } else if (RandomFactionGenerator.getInstance().getFactionHints().isAtWarWith(campaign.getFaction(),
                        Factions.getInstance().getFaction(faction), campaign.getLocalDate())) {
                    campuses.remove(campus);
                } else if ((isFactionRestricted) && (!faction.equals(person.getOriginFaction().getShortName()))) {
                    campuses.remove(campus);
                }
            }
        }

        if (!campuses.isEmpty()) {
            return campuses;
        } else {
            return null;
        }
    }


    /**
     * Checks if there is a conflict between the factions related to the academy and person or campaign.
     *
     * @param campaign The current campaign.
     * @param person The person to check the faction conflict with.
     * @return true if there is a faction conflict, false otherwise.
     */
    public Boolean isFactionConflict(Campaign campaign, Person person) {
        String faction = person.getEduAcademyFaction();

        // if academy faction at war with person's faction?
        if (RandomFactionGenerator.getInstance().getFactionHints().isAtWarWith(person.getOriginFaction(),
                Factions.getInstance().getFaction(faction), campaign.getLocalDate())) {
            return true;
        } else {
            // else, is academy faction at war with campaign's faction?
            return RandomFactionGenerator.getInstance().getFactionHints().isAtWarWith(campaign.getFaction(),
                    Factions.getInstance().getFaction(faction), campaign.getLocalDate());
        }
    }

    /**
     * Returns the nearest campus to a given campaign.
     *
     * @param campaign the campaign for which to find the nearest campus
     * @param campuses a list of campuses to consider
     * @return the nearest campus to the campaign
     */
    public String getNearestCampus(Campaign campaign, List<String> campuses) {
        int distance = 0;
        String nearestCampus = "";

        for (String campus : campuses) {
            int travelTime = Campaign.getSimplifiedTravelTime(campaign, campaign.getSystemByName(campus));

            if (travelTime > distance) {
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
    public String getTooltip(Campaign campaign, Person person, int courseIndex) {
        ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Education",
                MekHQ.getMHQOptions().getLocale());

        StringBuilder tooltip = new StringBuilder().append("<html>");
        tooltip.append("<i>").append(description).append("</i><br><br>");

        // this is where we check Person is eligible for the education and prep the skill display
        int educationLevel = 0;
        if (person.getEduHighestEducation() < educationLevelMin) {
            // If the person is ineligible to attend the academy, we tell them here
            tooltip.append("<b>").append(resources.getString("ineligible.text")
                    .replaceAll("0", String.valueOf(educationLevelMin))).append(" </b></html");
            return tooltip.toString();
        } else if (person.getEduHighestEducation() >= educationLevelMax) {
            educationLevel = educationLevelMax - educationLevelMin;
        } else {
            educationLevel += person.getEduHighestEducation() - educationLevelMin;
        }

        int baseSkillLevelAdjusted = baseSkillLevel + educationLevel;

        tooltip.append("<b>").append(resources.getString("curriculum.text")).append("</b><br>");

        // here we display the skills
        String[] skills = curriculums.get(courseIndex).replaceAll(", ", ",").split(",");
        for (String skill : skills) {
            tooltip.append(skill).append(" (");

            String skillParsed = skillParser(skill);

            if (person.hasSkill(skillParsed)) {
                if (person.getSkillLevel(skillParsed) >= baseSkillLevelAdjusted) {
                    tooltip.append("nothingToLearn.text").append(")<br>");
                } else {
                    tooltip.append('+').append(baseSkillLevelAdjusted - person.getSkillLevel(skillParsed)).append(")<br>");
                }
            } else {
                tooltip.append('+').append(baseSkillLevelAdjusted - person.getSkillLevel(skillParsed)).append(")<br>");
            }
        }

        // with the skill content resolved, we can move onto the rest of the tooltip
        tooltip.append("<br><b>").append(resources.getString("tuition.text")).append("</b> ")
                .append(tuition * getFactionDiscountAdjusted(campaign, person)).append (" CSB").append("<br>");
        tooltip.append("<b>").append(resources.getString("duration.text")).append("</b> ")
                .append(durationDays / 7).append (" weeks").append("<br>");

        // we need to do a little extra work to get distance, to cover academies with multiple campuses
        int distance = 2;
        PlanetarySystem destination = campaign.getCurrentSystem();

        if (!isLocal) {
            for (String system : locationSystems) {
                int distanceNew = Campaign.getSimplifiedTravelTime(campaign, campaign.getSystemByName(system));

                if (distanceNew > distance) {
                    distance = distanceNew;
                    destination = campaign.getSystemByName(system);
                }
            }
        }

        tooltip.append("<b>").append(resources.getString("distance.text")).append("</b> ")
                .append(distance / 7).append (" weeks (").append(destination.getName(campaign.getLocalDate())).append(")<br>");
        tooltip.append("<b>").append(resources.getString("facultySkill.text")).append("</b> ")
                .append(facultySkill).append ('+').append("<br>");
        tooltip.append("<b>").append(resources.getString("educationLevel.text")).append("</b> ")
                .append(educationLevel).append ('+').append("<br>");

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
        switch (skill.toLowerCase().replaceAll("\\s", "")) {
            case "piloting/mech":
                return SkillType.S_PILOT_MECH;
            case "gunnery/mech":
                return SkillType.S_GUN_MECH;
            case "piloting/aerospace":
                return SkillType.S_PILOT_AERO;
            case "gunnery/aerospace":
                return SkillType.S_GUN_AERO;
            case "piloting/groundvehicle":
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
            case "smallarms":
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
            case "hyperspacenavigation":
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