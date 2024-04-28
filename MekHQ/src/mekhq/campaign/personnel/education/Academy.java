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

import java.util.List;
import java.util.ResourceBundle;

/**
 * This class represents an academy.
 * The following fields are serialized to XML: name, description, locationSystems, constructionYear, destructionYear,
 * tuition, durationDays, tierMin, tierMax, skills, and skillLevel.
 */
@XmlRootElement(name = "academy")
@XmlAccessorType(value = XmlAccessType.FIELD)
public class Academy {
    @XmlElement(name = "name")
    private String name;

    @XmlElement(name = "isMilitary")
    private Boolean isMilitary;

    @XmlElement(name = "isLocal")
    private Boolean isLocal = false;

    @XmlElement(name = "description")
    private String description;

    @XmlElement(name = "locationSystem")
    private List<String> locationSystems;

    @XmlElement(name = "constructionYear")
    private Integer constructionYear;

    @XmlElement(name = "destructionYear")
    private Integer destructionYear = 9999;

    @XmlElement(name = "tuition")
    private Integer tuition;

    @XmlElement(name = "durationDays")
    private Integer durationDays;

    @XmlElement(name = "facultySkill")
    private Integer facultySkill;

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

    @XmlElement(name = "skillLevel")
    private Integer skillLevel;

    private String set;

    /**
     * This class provides a no-arg constructor, which is needed for unmarshalling of XML.
     */
    // no-arg constructor needed for unmarshalling of xml
    public Academy() {
    }


    /**
     * Creates an instance of the Academy class.
     *
     * @param set                  the set the academy belongs to
     * @param name                 the name of the academy
     * @param isMilitary           a boolean value indicating whether the academy is military or not
     * @param isLocal a boolean value indicating whether the academy is local or not
     * @param description          a description of the academy
     * @param locationSystems      a list of location systems where the academy is located
     * @param constructionYear     the year the academy was constructed
     * @param destructionYear      the year the academy was destroyed (optional, default value is 9999)
     * @param tuition              the tuition cost of the academy
     * @param durationDays         the duration of education in days
     * @param facultySkill         the skill level of the academy faculty
     * @param educationLevelMin    the minimum education level required for admission to the academy
     * @param educationLevelMax    the maximum education level provided by the academy
     * @param ageMin               the minimum age required for admission to the academy (optional, default value is 0)
     * @param ageMax               the maximum age required for admission to the academy (optional, default value is 9999)
     * @param qualifications       a list of qualifications provided by the academy
     * @param curriculums          a list of curriculums offered by the academy (groups of skills to be improved)
     * @param qualificationStartYears a list of start years for each curriculum offered by the academy
     * @param skillLevel           the base skill level provided for completion of a curriculum
     */
    public Academy(String set, String name, Boolean isMilitary, Boolean isLocal, String description, List<String> locationSystems,
                   Integer constructionYear, Integer destructionYear, Integer tuition, Integer durationDays, Integer facultySkill,
                   Integer educationLevelMin, Integer educationLevelMax, Integer ageMin, Integer ageMax, List<String> qualifications,
                   List<String> curriculums, List<Integer> qualificationStartYears, Integer skillLevel) {
        this.set = set;
        this.name = name;
        this.isMilitary = isMilitary;
        this.isLocal = isLocal;
        this.description = description;
        this.locationSystems = locationSystems;
        this.constructionYear = constructionYear;
        this.destructionYear = destructionYear;
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
        this.skillLevel = skillLevel;
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
     * Returns the academy's location system or faction.
     *
     * @return the academy's location system or faction as a string
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
     * Retrieves course duration in days.
     *
     * @return The course duration in days as an Integer.
     */
    public Integer getDurationDays() {
        return durationDays;
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
    public Integer getAcademicSkillLevel() {
        return skillLevel;
    }

    /**
     * Returns an HTML formatted string to be used as a tooltip.
     *
     * @param campaign    the current campaign
     * @param person      the person we're generating the tooltip for
     * @param courseIndex
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

        int skillLevelAdjusted = skillLevel + educationLevel;

        tooltip.append("<b>").append(resources.getString("curriculum.text")).append("</b><br>");

        // here we display the skills
        String[] skills = curriculums.get(courseIndex).replaceAll(", ", ",").split(",");
        for (String skill : skills) {
            tooltip.append(skill).append(" (");

            String skillParsed = skillParser(skill);

            if (person.hasSkill(skillParsed)) {
                if (person.getSkillLevel(skillParsed) >= skillLevelAdjusted) {
                    tooltip.append("nothingToLearn.text").append(")<br>");
                } else {
                    tooltip.append('+').append(skillLevelAdjusted - person.getSkillLevel(skillParsed)).append(")<br>");
                }
            } else {
                tooltip.append('+').append(skillLevelAdjusted - person.getSkillLevel(skillParsed)).append(")<br>");
            }
        }

        // with the skill content resolved, we can move onto the rest of the tooltip
        tooltip.append("<br><b>").append(resources.getString("tuition.text")).append("</b> ")
                .append(tuition).append (" CSB").append("<br>");
        tooltip.append("<b>").append(resources.getString("duration.text")).append("</b> ")
                .append(durationDays / 7).append (" weeks").append("<br>");

        // we need to do a little extra work to get distance, to cover academies with multiple campuses
        int distance = 2;
        String destination = campaign.getCurrentSystem().getName(campaign.getLocalDate());

        if (!isLocal) {
            for (String system : locationSystems) {
                int distanceNew = EducationController.simplifiedTravelTime(campaign, null, system);

                if (distanceNew > distance) {
                    distance = distanceNew;
                    destination = system;
                }
            }
        }

        tooltip.append("<b>").append(resources.getString("distance.text")).append("</b> ")
                .append(distance / 7).append (" weeks (").append(destination).append(")<br>");
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
    private static String skillParser(String skill) {
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