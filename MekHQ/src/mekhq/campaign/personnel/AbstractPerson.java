/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.enums.Gender;
import megamek.common.util.StringUtil;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.familyTree.Genealogy;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public abstract class AbstractPerson implements Serializable, MekHqXmlSerializable {
    //region Variable Declarations
    private static final long serialVersionUID = 2190101430016271321L;

    private UUID id;

    //region Name
    private transient String fullName; // this is a runtime variable, and shouldn't be saved
    private String preNominal;
    private String givenName;
    private String surname;
    private String postNominal;
    private String maidenName;
    private String callsign;
    //endregion Name

    //region Personal Information
    private Gender gender;
    private PersonnelStatus status;
    private LocalDate birthday;
    private LocalDate dateOfDeath;
    private Genealogy genealogy;
    private String biography;
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
     */
    public AbstractPerson(String preNominal, String givenName, String surname, String postNominal) {
        id = UUID.randomUUID();

        //region Name
        this.preNominal = preNominal;
        this.givenName = givenName;
        this.surname = surname;
        this.postNominal = postNominal;
        maidenName = null; // this is set to null to handle divorce cases
        callsign = "";
        //endregion Name

        //region Personal Information
        gender = Gender.MALE;
        status = PersonnelStatus.ACTIVE;
        birthday = null;
        dateOfDeath = null;
        genealogy = new Genealogy(getId());
        biography = "";
        //endregion Personal Information

        //region Portraits
        portraitCategory = Crew.ROOT_PORTRAIT;
        portraitFileName = Crew.PORTRAIT_NONE;
        //endregion Portraits
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
     * @return a String containing the person's first name including their pre-nominal
     */
    public String getFirstName() {
        return getPreNominal() + " " + getGivenName();
    }

    /**
     * return a full last name which may be a bloodname or a surname with or without honorifics.
     * A bloodname will overrule a surname but we do not disallow surnames for clanners, if the
     * player wants to input them
     * @return a String of the person's last name
     */
    public String getLastName() {
        String lastName = "";

        if (!StringUtil.isNullOrEmpty(getSurname())) {
            lastName += getSurname();
        }

        if (!StringUtil.isNullOrEmpty(getPostNominal())) {
            lastName += " " + getPostNominal();
        }
        return lastName;
    }

    /**
     * This is used to create the full name of the person, based on their pre-nomi
     */
    public void setFullName() {
        String lastName = getLastName();

        if (!StringUtil.isNullOrEmpty(lastName)) {
            setFullNameDirect(getFirstName() + " " + lastName);
        } else {
            setFullNameDirect(getFirstName());
        }
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
    public Gender getGender() {
        return gender;
    }

    /**
     * @param gender the person's new gender
     */
    public void setGender(Gender gender) {
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
        // Get age based on year
        if (getDateOfDeath() != null) {
            //use date of death instead of birthday
            today = getDateOfDeath();
        }

        return Math.toIntExact(ChronoUnit.YEARS.between(getBirthday(), today));
    }

    public Genealogy getGenealogy() {
        return genealogy;
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
    protected String getPortraitCategoryDirect() {
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
    protected String getPortraitFileNameDirect() {
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
     * @param today the current date
     * @return true if the person is a child (age is less than adulthood), otherwise false
     */
    public boolean isChild(LocalDate today) {
        // TODO : Windchild - make this based on age of adulthood option
        return (getAge(today) < 14);
    }
    //endregion Boolean Information Methods

    //region Read/Write from XML
    /**
     * This is to be called by the individual methods that implement AbstractPerson, as it does not
     * create a start or end tag.
     * @param pw1       The PrintWriter to print to
     * @param indent    The indent level to use
     */
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "id", getId().toString());

        //region Name
        if (!StringUtil.isNullOrEmpty(getPreNominal())) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "preNominal", getPreNominal());
        }
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "givenName", getGivenName());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "surname", getSurname());
        if (!StringUtil.isNullOrEmpty(getPostNominal())) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "postNominal", getPostNominal());
        }

        if (getMaidenName() != null) { // this is only a != null comparison because empty is a use case for divorce
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "maidenName", getMaidenName());
        }

        if (!StringUtil.isNullOrEmpty(getCallsign())) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "callsign", getCallsign());
        }
        //endregion Name

        //region Personal Information
        // Always save the person's gender, as it would otherwise get confusing fast
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "gender", getGender().name());
        // Always save a person's status, to make it easy to parse the personnel saved data
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "status", getStatus().name());
        if (getBirthday() != null) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "birthday",
                    MekHqXmlUtil.saveFormattedDate(getBirthday()));
        }

        if (getDateOfDeath() != null) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "deathday",
                    MekHqXmlUtil.saveFormattedDate(getDateOfDeath()));
        }

        if (!getGenealogy().isEmpty()) {
            getGenealogy().writeToXml(pw1, indent);
        }

        if (!StringUtil.isNullOrEmpty(getBiography())) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "biography", getBiography());
        }
        //endregion Personal Information

        //region Portraits
        if (!getPortraitCategoryDirect().equals(Crew.ROOT_PORTRAIT)) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "portraitCategory", getPortraitCategoryDirect());
        }

        if (!getPortraitFileNameDirect().equals(Crew.PORTRAIT_NONE)) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "portraitFile", getPortraitFileNameDirect());
        }
        //endregion Portraits
    }

    /**
     * This is used to parse the AbstractPerson fields
     * @param wn        The node to parse from
     * @param retVal    The initialized class that inherits AbstractPerson
     * @return an AbstractPerson that has all AbstractPerson fields loaded
     */
    public static AbstractPerson generateInstanceFromXML(Node wn, AbstractPerson retVal) {
        try {
            // Okay, now load AbstractPerson-specific fields!
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("id")) {
                    try {
                        retVal.id = UUID.fromString(wn2.getTextContent().trim());
                    } catch (Exception ignored) {

                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("preNominal")) {
                    retVal.preNominal = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("givenName")) {
                    retVal.givenName = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("surname")) {
                    retVal.surname = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("postNominal")) {
                    retVal.postNominal = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("maidenName")) {
                    retVal.maidenName = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("callsign")) {
                    retVal.callsign = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("gender")) {
                    retVal.gender = Gender.parseFromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("status")) {
                    retVal.status = PersonnelStatus.parseFromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("birthday")) {
                    retVal.birthday = MekHqXmlUtil.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("deathday")) {
                    retVal.dateOfDeath = MekHqXmlUtil.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("genealogy")) {
                    retVal.genealogy = Genealogy.generateInstanceFromXML(wn2.getChildNodes());
                } else if (wn2.getNodeName().equalsIgnoreCase("biography")) {
                    retVal.biography = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("portraitCategory")) {
                    retVal.setPortraitCategory(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("portraitFile")) {
                    retVal.setPortraitFileName(wn2.getTextContent().trim());
                }
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(AbstractPerson.class, "generateInstanceFromXML",
                    "Failed to load AbstractPerson, because of " + e.getMessage(), e);
            retVal = null;
        }

        return retVal;
    }
    //endregion Read/Write from XML
}
