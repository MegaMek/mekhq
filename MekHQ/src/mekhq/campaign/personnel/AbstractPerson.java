/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved
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
package mekhq.campaign.personnel;

import megamek.common.Crew;
import megamek.common.util.StringUtil;
import mekhq.MekHqXmlSerializable;
import mekhq.Utilities;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

public abstract class AbstractPerson implements Serializable, MekHqXmlSerializable {
    //region Variable Declarations
    private static final long serialVersionUID = 2190101430016271321L;

    protected UUID id;

    //region Name
    protected transient String fullName; // this is a runtime variable, and shouldn't be saved
    protected String preNominal;
    protected String givenName;
    protected String surname;
    protected String postNominal;
    protected String maidenName;
    protected String callsign;
    //endregion Name

    //region Personal Information
    protected int gender;
    protected PersonnelStatus status;
    protected LocalDate birthday;
    protected LocalDate dateOfDeath;
    protected String biography;
    private String originFactionCode;
    protected transient Faction originFaction;
    protected Planet originPlanet;
    //endregion Personal Information

    //region Portraits
    protected String portraitCategory;
    protected String portraitFile;
    // runtime override portraits (not saved)
    private transient String portraitCategoryOverride = null; // Potential Values are Crew.ROOT_PORTRAIT or null
    private transient String portraitFileOverride = null; // Potential Values are Crew.PORTRAIT_NONE or null
    //endregion Portraits
    //endregion Variable Declarations

    //region Constructors
    /**
     * Primary AbstractPerson constructor, variables are initialized in the exact same order as they are
     * saved to the XML file
     * @param preNominal        the person's pre-nominal
     * @param givenName         the person's given name
     * @param surname           the person's surname
     * @param postNominal       the person's post-nominal
     * @param originFactionCode the faction this person was borne into
     */
    public AbstractPerson(String preNominal, String givenName, String surname, String postNominal,
                          String originFactionCode) {
        id = null;

        //region Name
        this.preNominal = preNominal;
        this.givenName = givenName;
        this.surname = surname;
        this.postNominal = postNominal;
        maidenName = null; // this is set to null to handle divorce cases
        callsign = "";
        //endregion Name

        //region Personal Information
        gender = Crew.G_MALE;
        status = PersonnelStatus.ACTIVE;
        birthday = null;
        dateOfDeath = null;
        biography = "";
        this.originFactionCode = originFactionCode;
        originFaction = Faction.getFaction(originFactionCode);
        originPlanet = null;
        //region Personal Information

        //region Portraits
        portraitCategory = Crew.ROOT_PORTRAIT;
        portraitFile = Crew.PORTRAIT_NONE;
        //endregion Portraits

        // Initialize Data based on these settings
        setFullName();
    }
    //endregion Constructors

    //region Getters/Setters
    /**
     * @return the Person's Unique Id
     */
    public UUID getId() {
        return id;
    }

    /**
     * @param id the Unique Id to set the person's id to
     */
    public void setId(UUID id) {
        this.id = id;
    }

    //region Name
    public String getFullName() {
        return fullName;
    }

    public String getHyperlinkedName() {
        return String.format("<a href='PERSON:%s'>%s</a>", getId().toString(), getFullName());
    }

    public void setFullName() {
        String fullName = "";
        if (!StringUtil.isNullOrEmpty(getPreNominal())) {
            fullName = getPreNominal() + " ";
        }

        fullName += getGivenName();

        if (!StringUtil.isNullOrEmpty(getSurname())) {
            fullName += " " + getSurname();
        }

        if (!StringUtil.isNullOrEmpty(getPostNominal())) {
            fullName += " " + getPostNominal();
        }

        this.fullName = fullName;
    }

    public String getPreNominal() {
        return preNominal;
    }

    public void setPreNominal(String preNominal) {
        this.preNominal = preNominal;
        setFullName();
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
        setFullName();
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
        setFullName();
    }

    public String getPostNominal() {
        return postNominal;
    }

    public void setPostNominal(String postNominal) {
        this.postNominal = postNominal;
        setFullName();
    }

    public String getMaidenName() {
        return maidenName;
    }

    public void setMaidenName(String maidenName) {
        this.maidenName = maidenName;
    }

    public String getCallsign() {
        return callsign;
    }

    public void setCallsign(String callsign) {
        this.callsign = callsign;
    }
    //endregion Name

    //region Personal Information
    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public PersonnelStatus getStatus() {
        return status;
    }

    public void setStatus(PersonnelStatus status) {
        this.status = status;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public LocalDate getDateOfDeath() {
        return dateOfDeath;
    }

    public void setDateOfDeath(LocalDate dateOfDeath) {
        this.dateOfDeath = dateOfDeath;
    }

    /**
     * @param today the current date
     * @return the age of the person, based on either today or, if they are dead, their date of death
     */
    public int getAge(LocalDate today) {
        return Period.between(getBirthday(), Utilities.nonNull(getDateOfDeath(), today)).getYears();
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getOriginFactionCode() {
        return originFactionCode;
    }

    public Faction getOriginFaction() {
        return originFaction;
    }

    public void setOriginFaction(Faction originFaction) {
        this.originFaction = originFaction;
        this.originFactionCode = this.originFaction.getShortName();
    }

    public Planet getOriginPlanet() {
        return originPlanet;
    }

    public void setOriginPlanet(Planet originPlanet) {
        this.originPlanet = originPlanet;
    }
    //endregion Personal Information

    //region Portraits
    public String getPortraitCategory() {
        return Utilities.nonNull(portraitCategoryOverride, portraitCategory);
    }

    public void setPortraitCategory(String s) {
        this.portraitCategory = s;
    }

    public void setPortraitCategoryOverride(String s) {
        this.portraitCategoryOverride = s;
    }

    public String getPortraitFileName() {
        return Utilities.nonNull(portraitFileOverride, portraitFile);
    }

    public void setPortraitFileName(String s) {
        this.portraitFile = s;
    }

    public void setPortraitFileNameOverride(String s) {
        this.portraitFileOverride = s;
    }
    //endregion Portraits
    //endregion Getters/Setters

    //region Boolean Information Methods
    public boolean isFemale() {
        return gender == Crew.G_FEMALE;
    }

    public boolean isMale() {
        return gender == Crew.G_MALE;
    }

    public boolean isChild(LocalDate today) {
        return (getAge(today) <= 13);
    }
    //endregion Boolean Information Methods
}
