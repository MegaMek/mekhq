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
import megamek.common.annotations.Nullable;
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

    private UUID id;

    //region Name
    protected transient String fullName; // this is a runtime variable, and shouldn't be saved
    private String preNominal;
    private String givenName;
    private String surname;
    private String postNominal;
    private String maidenName;
    private String callsign;
    //endregion Name

    //region Personal Information
    private int gender;
    private PersonnelStatus status;
    private LocalDate birthday;
    private LocalDate dateOfDeath;
    private String biography;
    private String originFactionCode;
    private transient Faction originFaction;
    private Planet originPlanet;
    //endregion Personal Information

    //region Portraits
    private String portraitCategory;
    private String portraitFileName;
    // runtime override portraits (not saved)
    private transient String portraitCategoryOverride = null; // Potential Values are Crew.ROOT_PORTRAIT or null
    private transient String portraitFileNameOverride = null; // Potential Values are Crew.PORTRAIT_NONE or null
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
        portraitFileName = Crew.PORTRAIT_NONE;
        //endregion Portraits

        // Initialize Data based on these settings
        setFullName();
    }
    //endregion Constructors

    //region Getters/Setters
    /**
     * @return the person's id
     */
    public UUID getId() {
        return id;
    }

    /**
     * @param id the person's new id
     */
    public void setId(UUID id) {
        this.id = id;
    }

    /**
     * This creates a new random id for the person
     */
    public void setRandomId() {
        this.id = UUID.randomUUID();
    }

    //region Name
    /**
     * @return the person's full name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * @return a hyperlinked string for the person's name
     */
    public String getHyperlinkedName() {
        return String.format("<a href='PERSON:%s'>%s</a>", getId().toString(), getFullName());
    }

    /**
     * This is used to create the full name of the person, based on their pre-nominal, given name,
     *
     */
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

        setFullNameDirect(fullName);
    }

    /**
     * @param fullName this sets the full name to be equal to the input string. This can ONLY be
     *                 called by {@link AbstractPerson#setFullName()} or its overrides.
     */
    protected void setFullNameDirect(String fullName) {
        this.fullName = fullName;
    }

    /**
     * @return the person's pre-nominal
     */
    public String getPreNominal() {
        return preNominal;
    }

    /**
     * @param preNominal the person's new pre-nominal
     */
    public void setPreNominal(String preNominal) {
        this.preNominal = preNominal;
        setFullName();
    }

    /**
     * @return the person's given name
     */
    public String getGivenName() {
        return givenName;
    }

    /**
     * @param givenName the person's new given name
     */
    public void setGivenName(String givenName) {
        this.givenName = givenName;
        setFullName();
    }

    /**
     * @return the person's surname
     */
    public String getSurname() {
        return surname;
    }

    /**
     * @param surname the person's new surname
     */
    public void setSurname(String surname) {
        this.surname = surname;
        setFullName();
    }

    /**
     * @return the person's post-nominal
     */
    public String getPostNominal() {
        return postNominal;
    }

    /**
     * @param postNominal the person's new post-nominal
     */
    public void setPostNominal(String postNominal) {
        this.postNominal = postNominal;
        setFullName();
    }

    /**
     * @return the person's maiden name
     */
    public @Nullable String getMaidenName() {
        return maidenName;
    }

    /**
     * @param maidenName the person's new maiden name
     */
    public void setMaidenName(@Nullable String maidenName) {
        this.maidenName = maidenName;
    }

    /**
     * @return the person's callsign
     */
    public String getCallsign() {
        return callsign;
    }

    /**
     * @param callsign the person's new callsign
     */
    public void setCallsign(String callsign) {
        this.callsign = callsign;
    }
    //endregion Name

    //region Personal Information
    /**
     * @return the person's gender
     */
    public int getGender() {
        return gender;
    }

    /**
     * @param gender the person's new gender
     */
    public void setGender(int gender) {
        this.gender = gender;
    }

    /**
     * @return the person's status
     */
    public PersonnelStatus getStatus() {
        return status;
    }

    /**
     * @param status the person's new status
     */
    public void setStatus(PersonnelStatus status) {
        this.status = status;
    }

    /**
     * @return the person's birthday
     */
    public LocalDate getBirthday() {
        return birthday;
    }

    /**
     * @param birthday the person's new birthday
     */
    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    /**
     * @return the date of the person's death
     */
    public @Nullable LocalDate getDateOfDeath() {
        return dateOfDeath;
    }

    /**
     * @param dateOfDeath the date the person died
     */
    public void setDateOfDeath(@Nullable LocalDate dateOfDeath) {
        this.dateOfDeath = dateOfDeath;
    }

    /**
     * @param today the current date
     * @return the age of the person, based on either today or, if they are dead, their date of death
     */
    public int getAge(LocalDate today) {
        return Period.between(getBirthday(), Utilities.nonNull(getDateOfDeath(), today)).getYears();
    }

    /**
     * @return the person's biography
     */
    public String getBiography() {
        return biography;
    }

    /**
     * @param biography the person's new biography
     */
    public void setBiography(String biography) {
        this.biography = biography;
    }

    /**
     * @return the person's origin faction's code
     */
    public String getOriginFactionCode() {
        return originFactionCode;
    }

    /**
     * This sets the person's origin faction and origin faction code based on the input origin faction code
     * @param originFactionCode the person's new origin faction's code
     */
    public void setOriginFactionCode(String originFactionCode) {
        this.originFactionCode = originFactionCode;
        this.originFaction = Faction.getFaction(originFactionCode);
    }

    /**
     * @return the person's origin faction
     */
    public Faction getOriginFaction() {
        return originFaction;
    }

    /**
     * This sets the person's origin faction and origin faction code based on the input origin faction
     * @param originFaction the person's new origin faction
     */
    public void setOriginFaction(Faction originFaction) {
        this.originFaction = originFaction;
        this.originFactionCode = this.originFaction.getShortName();
    }

    /**
     * @return the person's origin planet
     */
    public Planet getOriginPlanet() {
        return originPlanet;
    }

    /**
     * @param originPlanet the person's new origin planet
     */
    public void setOriginPlanet(Planet originPlanet) {
        this.originPlanet = originPlanet;
    }
    //endregion Personal Information

    //region Portraits

    /**
     * @return the person's current portrait category, which is either the override if the portrait
     * category was not found during load or their current category
     */
    public String getPortraitCategory() {
        return Utilities.nonNull(getPortraitCategoryOverrideDirect(), getPortraitCategoryDirect());
    }

    /**
     * @return the person's current portrait category.This can ONLY be called by
     * {@link AbstractPerson#getPortraitCategory()} or its overrides.
     */
    private String getPortraitCategoryDirect() {
        return portraitCategory;
    }

    /**
     * @param portraitCategory the person's portrait category
     */
    public void setPortraitCategory(String portraitCategory) {
        this.portraitCategory = portraitCategory;
    }

    /**
     * @return the current portrait category override.This can ONLY be called by
     * {@link AbstractPerson#getPortraitCategory()} or its overrides.
     */
    private @Nullable String getPortraitCategoryOverrideDirect() {
        return portraitCategoryOverride;
    }

    /**
     * This sets the person's Portrait Category Override to be equal to Crew.ROOT_PORTRAIT, which
     * means that the portrait was not found
     */
    public void setPortraitCategoryOverride() {
        this.portraitCategoryOverride = Crew.ROOT_PORTRAIT;
    }

    /**
     * @return the person's current portrait file name
     */
    public String getPortraitFileName() {
        return Utilities.nonNull(getPortraitFileNameOverrideDirect(), getPortraitFileNameDirect());
    }

    /**
     * @return the person's current portrait file name.This can ONLY be called by
     * {@link AbstractPerson#getPortraitFileName()} or its overrides.
     */
    private String getPortraitFileNameDirect() {
        return portraitFileName;
    }

    /**
     * @param portraitFileName the person's portrait file name
     */
    public void setPortraitFileName(String portraitFileName) {
        this.portraitFileName = portraitFileName;
    }

    /**
     * @return the person's current portrait file name override.This can ONLY be called by
     * {@link AbstractPerson#getPortraitFileName()} or its overrides.
     */
    private @Nullable String getPortraitFileNameOverrideDirect() {
        return portraitFileNameOverride;
    }

    /**
     * This sets the person's Portrait File Name to be equal to Crew.PORTRAIT_NONE, which
     * means that the portrait was not found
     */
    public void setPortraitFileNameOverride() {
        this.portraitFileNameOverride = Crew.PORTRAIT_NONE;
    }
    //endregion Portraits
    //endregion Getters/Setters

    //region Boolean Information Methods

    /**
     * @return true if the person is female, otherwise false
     */
    @Deprecated // this should be part of a gender enum
    public boolean isFemale() {
        return gender == Crew.G_FEMALE;
    }

    /**
     * @return true if the person is male, otherwise false
     */
    @Deprecated // this should be part of a gender enum
    public boolean isMale() {
        return gender == Crew.G_MALE;
    }

    /**
     * @param today the current date
     * @return true if the person is a child (age is less than adulthood), otherwise false
     */
    public boolean isChild(LocalDate today) {
        // TODO : make this based on age of adulthood option
        return (getAge(today) < 14);
    }
    //endregion Boolean Information Methods

    //region Read/Write from XML
    private static void fixBadDataOnLoad(AbstractPerson person) {
/*
        if (person.getBirthday() == null) {
            person.setBirthday(Campaign.getDefaults().getDate());
        }
 */
    }
    //endregion Read/Write from XML
}
