/*
 * Copyright (c) 2009 - Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.client.generator.RandomNameGenerator;
import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.ConvFighter;
import megamek.common.Crew;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.LandAirMech;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.SmallCraft;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import megamek.common.VTOL;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.icons.AbstractIcon;
import megamek.common.icons.Portrait;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.PilotOptions;
import megamek.common.util.EncodeControl;
import megamek.common.util.StringUtil;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;
import mekhq.Version;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.ExtraData;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.io.CampaignXmlParser;
import mekhq.campaign.io.Migration.PersonMigrator;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.LogEntryFactory;
import mekhq.campaign.log.MedicalLogger;
import mekhq.campaign.log.PersonalLogger;
import mekhq.campaign.log.ServiceLogger;
import mekhq.campaign.mod.am.InjuryUtil;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.enums.BodyLocation;
import mekhq.campaign.personnel.enums.Divorce;
import mekhq.campaign.personnel.enums.FamilialRelationshipType;
import mekhq.campaign.personnel.enums.GenderDescriptors;
import mekhq.campaign.personnel.enums.ManeiDominiClass;
import mekhq.campaign.personnel.enums.ManeiDominiRank;
import mekhq.campaign.personnel.enums.Marriage;
import mekhq.campaign.personnel.enums.ModifierValue;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.enums.PrisonerStatus;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.personnel.enums.ROMDesignation;
import mekhq.campaign.personnel.familyTree.Genealogy;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.work.IPartWork;
import mekhq.io.idReferenceClasses.PersonIdReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Person implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = -847642980395311152L;

    private static final Map<Integer, Money> MECHWARRIOR_AERO_RANSOM_VALUES;
    private static final Map<Integer, Money> OTHER_RANSOM_VALUES;

    private PersonAwardController awardController;

    //region Family Variables
    // Lineage
    private Genealogy genealogy;

    //region Procreation
    // this is a flag used in random procreation to determine whether or not to attempt to procreate
    private boolean tryingToConceive;
    private LocalDate dueDate;
    private LocalDate expectedDueDate;

    private static final int PREGNANCY_STANDARD_DURATION = 268; //standard duration of a pregnancy in days

    // This creates a random range of approximately six weeks with which to modify the standard pregnancy duration
    // To create randomized pregnancy duration
    private static final IntSupplier PREGNANCY_MODIFY_DURATION = () -> {
        double gaussian = Math.sqrt(-2 * Math.log(Math.nextUp(Math.random())))
            * Math.cos(2.0 * Math.PI * Math.random());
        // To not get weird results, we limit the values to +/- 4.0 (almost 6 weeks)
        return (int) Math.round(Math.max(-4.0, Math.min(4.0, gaussian)) * 10);
    };

    private static final IntSupplier PREGNANCY_SIZE = () -> {
        int children = 1;
        // Hellin's law says it's 1:89 chance, to not make it appear too seldom, we use 1:50
        while (Compute.randomInt(50) == 0) {
            ++ children;
        }
        return Math.min(children, 10); // Limit to decuplets, for the sake of sanity
    };

    private static final String[] PREGNANCY_MULTIPLE_NAMES = {null, null,
        "twins", "triplets", "quadruplets", "quintuplets",
        "sextuplets", "septuplets", "octuplets", "nonuplets", "decuplets"
    };

    public static final ExtraData.IntKey PREGNANCY_CHILDREN_DATA = new ExtraData.IntKey("procreation:children");
    public static final ExtraData.StringKey PREGNANCY_FATHER_DATA = new ExtraData.StringKey("procreation:father");
    //endregion Procreation

    //region Marriage
    // this is a flag used in determine whether or not a person is a potential marriage candidate
    // provided that they are not married, are old enough, etc.
    private boolean tryingToMarry;
    //endregion Marriage
    //endregion Family Variables

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

    private Gender gender;
    private AbstractIcon portrait;

    private PersonnelRole primaryRole;
    private PersonnelRole secondaryRole;

    private ROMDesignation primaryDesignator;
    private ROMDesignation secondaryDesignator;

    private String biography;
    private LocalDate birthday;
    private LocalDate dateOfDeath;
    private LocalDate recruitment;
    private LocalDate lastRankChangeDate;
    private LocalDate retirement;
    private List<LogEntry> personnelLog;
    private List<LogEntry> missionLog;

    private Skills skills;
    private PersonnelOptions options;
    private int toughness;

    private PersonnelStatus status;
    private int xp;
    private int acquisitions;
    private Money salary;
    private Money totalEarnings;
    private int hits;
    private PrisonerStatus prisonerStatus;

    private boolean commander;

    // Supports edge usage by a ship's engineer composite crewman
    private int edgeUsedThisRound;
    // To track how many edge points support personnel have left until next refresh
    private int currentEdge;

    //phenotype and background
    private Phenotype phenotype;
    private boolean clan;
    private String bloodname;
    private Faction originFaction;
    private Planet originPlanet;

    //assignments
    private Unit unit;
    private UUID doctorId;
    private List<Unit> techUnits;

    //days of rest
    private int idleMonths;
    private int daysToWaitForHealing;

    // Our rank
    private RankSystem rankSystem;
    private int rank;
    private int rankLevel;

    private ManeiDominiClass maneiDominiClass;
    private ManeiDominiRank maneiDominiRank;

    //stuff to track for support teams
    private int minutesLeft;
    private int overtimeLeft;
    private int nTasks;
    private boolean engineer;
    public static final int PRIMARY_ROLE_SUPPORT_TIME = 480;
    public static final int PRIMARY_ROLE_OVERTIME_SUPPORT_TIME = 240;
    public static final int SECONDARY_ROLE_SUPPORT_TIME = 240;
    public static final int SECONDARY_ROLE_OVERTIME_SUPPORT_TIME = 120;

    //region Advanced Medical
    private List<Injury> injuries;
    //endregion Advanced Medical

    //region Against the Bot
    private boolean founder; // +1 share if using shares system
    private int originalUnitWeight; // uses EntityWeightClass with 0 (Extra-Light) for no original unit
    public static final int TECH_IS1 = 0;
    public static final int TECH_IS2 = 1;
    public static final int TECH_CLAN = 2;
    private int originalUnitTech;
    private UUID originalUnitId;
    //endregion Against the Bot

    // Generic extra data, for use with plugins and mods
    private ExtraData extraData;

    //lets just go ahead and pass in the campaign - to hell with OOP
    @Deprecated // May 1st, 2020 - As part of moving Person to be a fully OOP class
    private Campaign campaign;

    // For upgrading personnel entries to missing log entries
    private static String missionParticipatedString;
    private static String getMissionParticipatedString() {
        if (missionParticipatedString == null) {
            ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.LogEntries", new EncodeControl());
            missionParticipatedString = resourceMap.getString("participatedInMission.text");
            missionParticipatedString = missionParticipatedString.substring(0, missionParticipatedString.indexOf(" "));
        }

        return missionParticipatedString;
    }

    // initializes the AtB ransom values
    static {
        MECHWARRIOR_AERO_RANSOM_VALUES = new HashMap<>();
        MECHWARRIOR_AERO_RANSOM_VALUES.put(SkillType.EXP_ULTRA_GREEN, Money.of(5000)); // no official AtB rules for really inexperienced scrubs, but...
        MECHWARRIOR_AERO_RANSOM_VALUES.put(SkillType.EXP_GREEN, Money.of(10000));
        MECHWARRIOR_AERO_RANSOM_VALUES.put(SkillType.EXP_REGULAR, Money.of(25000));
        MECHWARRIOR_AERO_RANSOM_VALUES.put(SkillType.EXP_VETERAN, Money.of(75000));
        MECHWARRIOR_AERO_RANSOM_VALUES.put(SkillType.EXP_ELITE, Money.of(150000));

        OTHER_RANSOM_VALUES = new HashMap<>();
        OTHER_RANSOM_VALUES.put(SkillType.EXP_ULTRA_GREEN, Money.of(2500));
        OTHER_RANSOM_VALUES.put(SkillType.EXP_GREEN, Money.of(5000));
        OTHER_RANSOM_VALUES.put(SkillType.EXP_REGULAR, Money.of(10000));
        OTHER_RANSOM_VALUES.put(SkillType.EXP_VETERAN, Money.of(25000));
        OTHER_RANSOM_VALUES.put(SkillType.EXP_ELITE, Money.of(50000));
    }

    //endregion Variable Declarations

    //region Constructors
    //default constructor
    protected Person(UUID id) {
        this.id = id;
    }

    public Person(Campaign campaign) {
        this(RandomNameGenerator.UNNAMED, RandomNameGenerator.UNNAMED_SURNAME, campaign);
    }

    public Person(Campaign campaign, String factionCode) {
        this(RandomNameGenerator.UNNAMED, RandomNameGenerator.UNNAMED_SURNAME, campaign, factionCode);
    }

    public Person(String givenName, String surname, Campaign campaign) {
        this(givenName, surname, campaign, campaign.getFactionCode());
    }

    public Person(String givenName, String surname, @Nullable Campaign campaign, String factionCode) {
        this("", givenName, surname, "", campaign, factionCode);
    }

    /**
     * Primary Person constructor, variables are initialized in the exact same order as they are
     * saved to the XML file
     * @param preNominal    the person's pre-nominal
     * @param givenName     the person's given name
     * @param surname       the person's surname
     * @param postNominal   the person's post-nominal
     * @param campaign      the campaign this person is a part of, or null (unit testing only)
     * @param factionCode   the faction this person was borne into
     */
    public Person(final String preNominal, final String givenName, final String surname,
                  final String postNominal, final @Nullable Campaign campaign,
                  final String factionCode) {
        // First, we assign campaign
        this.campaign = campaign;

        // Then, we assign the variables in XML file order
        id = UUID.randomUUID();

        //region Name
        setPreNominalDirect(preNominal);
        setGivenNameDirect(givenName);
        setSurnameDirect(surname);
        setPostNominalDirect(postNominal);
        setMaidenName(null); // this is set to null to handle divorce cases
        setCallsignDirect("");
        //endregion Name

        primaryRole = PersonnelRole.NONE;
        secondaryRole = PersonnelRole.NONE;
        primaryDesignator = ROMDesignation.NONE;
        secondaryDesignator = ROMDesignation.NONE;
        commander = false;
        originFaction = Factions.getInstance().getFaction(factionCode);
        originPlanet = null;
        clan = originFaction.isClan();
        phenotype = Phenotype.NONE;
        bloodname = "";
        biography = "";
        setGenealogy(new Genealogy(this));
        tryingToMarry = true;
        tryingToConceive = true;
        dueDate = null;
        expectedDueDate = null;
        setPortrait(new Portrait());
        xp = 0;
        daysToWaitForHealing = 0;
        setGender(Gender.MALE);
        setRankSystemDirect((campaign == null) ? null : campaign.getRankSystem());
        setRank(0);
        setRankLevel(0);
        setManeiDominiClassDirect(ManeiDominiClass.NONE);
        setManeiDominiRankDirect(ManeiDominiRank.NONE);
        nTasks = 0;
        doctorId = null;
        salary = Money.of(-1);
        totalEarnings = Money.of(0);
        status = PersonnelStatus.ACTIVE;
        prisonerStatus = PrisonerStatus.FREE;
        hits = 0;
        toughness = 0;
        resetMinutesLeft(); // this assigns minutesLeft and overtimeLeft
        birthday = null;
        dateOfDeath = null;
        recruitment = null;
        lastRankChangeDate = null;
        retirement = null;
        skills = new Skills();
        options = new PersonnelOptions();
        currentEdge = 0;
        techUnits = new ArrayList<>();
        personnelLog = new ArrayList<>();
        missionLog = new ArrayList<>();
        awardController = new PersonAwardController(this);
        injuries = new ArrayList<>();
        founder = false;
        originalUnitWeight = EntityWeightClass.WEIGHT_ULTRA_LIGHT;
        originalUnitTech = TECH_IS1;
        originalUnitId = null;
        acquisitions = 0;
        extraData = new ExtraData();

        // Initialize Data based on these settings
        setFullName();
    }
    //endregion Constructors

    @Deprecated // May 1st, 2020 - as part of turning Person into a fully OOP class
    public Campaign getCampaign() {
        return campaign;
    }

    public Phenotype getPhenotype() {
        return phenotype;
    }

    public void setPhenotype(Phenotype phenotype) {
        this.phenotype = phenotype;
    }

    public boolean isClanner() {
        return clan;
    }

    public void setClanner(boolean b) {
        clan = b;
    }

    public String getBloodname() {
        return bloodname;
    }

    public void setBloodname(String bloodname) {
        this.bloodname = bloodname;
        setFullName();
    }

    public Faction getOriginFaction() {
        return originFaction;
    }

    public void setOriginFaction(Faction f) {
        originFaction = f;
    }

    public Planet getOriginPlanet() {
        return originPlanet;
    }

    public void setOriginPlanet(Planet p) {
        originPlanet = p;
    }

    public boolean isCommander() {
        return commander;
    }

    public void setCommander(boolean tf) {
        commander = tf;
    }

    public PrisonerStatus getPrisonerStatus() {
        return prisonerStatus;
    }

    public void setPrisonerStatus(PrisonerStatus prisonerStatus) {
        setPrisonerStatus(prisonerStatus, true);
    }

    /**
     * This requires expanded checks because a number of functionalities are strictly dependant on
     * the current person's prisoner status.
     * @param prisonerStatus The new prisoner status for the person in question
     * @param log whether to log the change or not
     */
    public void setPrisonerStatus(PrisonerStatus prisonerStatus, boolean log) {
        // This must be processed completely, as the unchanged prisoner status of Free to Free is
        // used during recruitment

        final boolean freed = !getPrisonerStatus().isFree();
        final boolean isPrisoner = prisonerStatus.isPrisoner();
        this.prisonerStatus = prisonerStatus;

        // Now, we need to fix values and ranks based on the Person's status
        switch (prisonerStatus) {
            case PRISONER:
            case PRISONER_DEFECTOR:
            case BONDSMAN:
                setRecruitment(null);
                setLastRankChangeDate(null);
                if (log) {
                    if (isPrisoner) {
                        ServiceLogger.madePrisoner(this, getCampaign().getLocalDate(),
                                getCampaign().getName(), "");
                    } else {
                        ServiceLogger.madeBondsman(this, getCampaign().getLocalDate(),
                                getCampaign().getName(), "");
                    }
                }
                break;
            case FREE:
                if (!getPrimaryRole().isDependent()) {
                    if (getCampaign().getCampaignOptions().getUseTimeInService()) {
                        setRecruitment(getCampaign().getLocalDate());
                    }
                    if (getCampaign().getCampaignOptions().getUseTimeInRank()) {
                        setLastRankChangeDate(getCampaign().getLocalDate());
                    }
                }

                if (log) {
                    if (freed) {
                        ServiceLogger.freed(this, getCampaign().getLocalDate(),
                                getCampaign().getName(), "");
                    } else {
                        ServiceLogger.joined(this, getCampaign().getLocalDate(),
                                getCampaign().getName(), "");
                    }
                }
                break;
        }

        if (!prisonerStatus.isFree()) {
            if (getUnit() != null) {
                getUnit().remove(this, true);
            }
        }

        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    //region Text Getters
    public String pregnancyStatus() {
        return isPregnant() ? " (Pregnant)" : "";
    }
    //endregion Text Getters

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
        return String.format("<a href='PERSON:%s'>%s</a>", getId(), getFullName());
    }

    /**
     * This is used to create the full name of the person, based on their first and last names
     */
    public void setFullName() {
        final String lastName = getLastName();
        setFullNameDirect(getFirstName()
                + (getCallsign().isBlank() ? "" : (" \"" + getCallsign() + "\""))
                + (lastName.isBlank() ? "" : " " + lastName));
    }

    /**
     * @param fullName this sets the full name to be equal to the input string. This can ONLY be
     *                 called by {@link Person#setFullName()} or its overrides.
     */
    protected void setFullNameDirect(final String fullName) {
        this.fullName = fullName;
    }

    /**
     * @return a String containing the person's first name including their pre-nominal
     */
    public String getFirstName() {
        return (getPreNominal().isBlank() ? "" : (getPreNominal() + " ")) + getGivenName();
    }

    /**
     * Return a full last name which may be a bloodname or a surname with or without a post-nominal.
     * A bloodname will overrule a surname but we do not disallow surnames for clanners, if the
     * player wants to input them
     * @return a String of the person's last name
     */
    public String getLastName() {
        String lastName = !StringUtil.isNullOrEmpty(getBloodname()) ? getBloodname()
                : !StringUtil.isNullOrEmpty(getSurname()) ? getSurname()
                : "";
        if (!StringUtil.isNullOrEmpty(getPostNominal())) {
            lastName += (lastName.isBlank() ? "" : " ") + getPostNominal();
        }
        return lastName;
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
    public void setPreNominal(final String preNominal) {
        setPreNominalDirect(preNominal);
        setFullName();
    }

    protected void setPreNominalDirect(final String preNominal) {
        this.preNominal = preNominal;
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
    public void setGivenName(final String givenName) {
        setGivenNameDirect(givenName);
        setFullName();
    }

    protected void setGivenNameDirect(final String givenName) {
        this.givenName = givenName;
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
    public void setSurname(final String surname) {
        setSurnameDirect(surname);
        setFullName();
    }

    protected void setSurnameDirect(final String surname) {
        this.surname = surname;
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
    public void setPostNominal(final String postNominal) {
        setPostNominalDirect(postNominal);
        setFullName();
    }

    protected void setPostNominalDirect(final String postNominal) {
        this.postNominal = postNominal;
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
    public void setMaidenName(final @Nullable String maidenName) {
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
    public void setCallsign(final String callsign) {
        setCallsignDirect(callsign);
        setFullName();
    }

    protected void setCallsignDirect(final String callsign) {
        this.callsign = callsign;
    }

    /**
     * This method is used to migrate names from being a joined name to split between given name and
     * surname, as part of the Personnel changes in MekHQ 0.47.4, and is used to migrate from
     * MM-style names to MHQ-style names
     * @param text text containing the name to be migrated
     */
    public void migrateName(final String text) {
        // How this works:
        // Takes the input name, and splits it into individual parts.
        // Then, it depends on whether the person is a Clanner or not.
        // For Clan names:
        // Takes the input name, and assumes that person does not have a surname
        // Bloodnames are assumed to have been assigned by MekHQ
        // For Inner Sphere names:
        // Depending on the length of the resulting array, the name is processed differently
        // Array of length 1: the name is assumed to not have a surname, just a given name
        // Array of length 2: the name is assumed to be a given name and a surname
        // Array of length 3: the name is assumed to be a given name and two surnames
        // Array of length 4+: the name is assumed to be as many given names as possible and two surnames
        //
        // Then, the full name is set
        final String[] name = text.trim().split("\\s+");
        final StringBuilder givenName = new StringBuilder(name[0]);

        if (isClanner()) {
            if (name.length > 1) {
                int i;
                for (i = 1; i < name.length - 1; i++) {
                    givenName.append(" ").append(name[i]);
                }

                if (!(!StringUtil.isNullOrEmpty(getBloodname()) && getBloodname().equals(name[i]))) {
                    givenName.append(" ").append(name[i]);
                }
            }
        } else {
            if (name.length == 2) {
                setSurnameDirect(name[1]);
            } else if (name.length == 3) {
                setSurnameDirect(name[1] + " " + name[2]);
            } else if (name.length > 3) {
                int i;
                for (i = 1; i < name.length - 2; i++) {
                    givenName.append(" ").append(name[i]);
                }
                setSurnameDirect(name[i] + " " + name[i + 1]);
            }
        }

        if ((getSurname() == null) || getSurname().equals(RandomNameGenerator.UNNAMED_SURNAME)) {
            setSurnameDirect("");
        }

        setGivenNameDirect(givenName.toString());
        setFullName();
    }
    //endregion Names

    //region Portrait
    public AbstractIcon getPortrait() {
        return portrait;
    }

    public void setPortrait(AbstractIcon portrait) {
        assert (portrait != null) : "Illegal assignment: cannot have a null AbstractIcon for a Portrait";
        this.portrait = Objects.requireNonNull(portrait);
    }

    public String getPortraitCategory() {
        return getPortrait().getCategory();
    }

    public String getPortraitFileName() {
        return getPortrait().getFilename();
    }

    //region Personnel Roles
    public PersonnelRole getPrimaryRole() {
        return primaryRole;
    }

    public void setPrimaryRole(final PersonnelRole primaryRole) {
        // don't need to do any processing for no changes
        if (primaryRole == getPrimaryRole()) {
            return;
        }

        // We need to make some secondary role assignments to None here for better UX in
        // assigning roles, following these rules:
        // 1) Cannot have the same primary and secondary roles
        // 2) Must have a None secondary role if you are a Dependent
        // 3) Cannot be a primary tech and a secondary Astech
        // 4) Cannot be a primary Astech and a secondary tech
        // 5) Cannot be primary medical staff and a secondary Medic
        // 6) Cannot be a primary Medic and secondary medical staff
        if ((primaryRole == getSecondaryRole())
                || primaryRole.isDependent()
                || (primaryRole.isTech() && getSecondaryRole().isAstech())
                || (primaryRole.isAstech() && getSecondaryRole().isTechSecondary())
                || (primaryRole.isMedicalStaff() && getSecondaryRole().isMedic())
                || (primaryRole.isMedic() && getSecondaryRole().isMedicalStaff())) {
            setSecondaryRoleDirect(PersonnelRole.NONE);
        }

        // Now, we can perform the time in service and last rank change tracking change for dependents
        if (primaryRole.isDependent()) {
            setRecruitment(null);
            setLastRankChangeDate(null);
        } else if (getPrimaryRole().isDependent()) {
            setRecruitment(getCampaign().getLocalDate());
            setLastRankChangeDate(getCampaign().getLocalDate());
        }

        // Finally, we can set the primary role
        setPrimaryRoleDirect(primaryRole);

        // and trigger the update event
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    public void setPrimaryRoleDirect(PersonnelRole primaryRole) {
        this.primaryRole = primaryRole;
    }

    public PersonnelRole getSecondaryRole() {
        return secondaryRole;
    }

    public void setSecondaryRole(final PersonnelRole secondaryRole) {
        if (secondaryRole == getSecondaryRole()) {
            return;
        }

        setSecondaryRoleDirect(secondaryRole);
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    public void setSecondaryRoleDirect(PersonnelRole secondaryRole) {
        this.secondaryRole = secondaryRole;
    }

    /**
     * This is used to determine if a person has a specific role as either their primary OR their
     * secondary role
     * @param role the role to determine
     * @return true if the person has the specific role either as their primary or secondary role
     */
    public boolean hasRole(PersonnelRole role) {
        return (getPrimaryRole() == role) || (getSecondaryRole() == role);
    }

    /**
     * @return true if the person has a primary or secondary combat role
     */
    public boolean hasCombatRole() {
        return getPrimaryRole().isCombat() || getSecondaryRole().isCombat();
    }

    /**
     * @param excludeUnmarketable whether to exclude the unmarketable roles from the comparison
     * @return true if the person has a primary or secondary support role
     */
    public boolean hasSupportRole(boolean excludeUnmarketable) {
        return getPrimaryRole().isSupport(excludeUnmarketable) || getSecondaryRole().isSupport(excludeUnmarketable);
    }

    public String getRoleDesc() {
        String role = getPrimaryRoleDesc();
        if (!getSecondaryRole().isNone()) {
            role += "/" + getSecondaryRoleDesc();
        }
        return role;
    }

    public String getPrimaryRoleDesc() {
        String bgPrefix = "";
        if (isClanner()) {
            bgPrefix = getPhenotype().getShortName() + " ";
        }
        return bgPrefix + getPrimaryRole().getName(isClanner());
    }

    public String getSecondaryRoleDesc() {
        return getSecondaryRole().getName(isClanner());
    }

    public boolean canPerformRole(final PersonnelRole role, final boolean primary) {
        if (primary) {
            // Primary Role:
            // We only do a few here, as it is better on the UX-side to correct the issues when
            // assigning the primary role
            // 1) Can always be Dependent
            // 2) Cannot be None
            if (role.isDependent()) {
                return true;
            } else if (role.isNone()) {
                return false;
            }
        } else {
            // Secondary Role:
            // 1) Can always be None
            // 2) Cannot be Dependent
            // 3) Can only be None if the primary role is a Dependent
            // 4) Cannot be equal to the primary role
            // 5) Cannot be a tech role if the primary role is an Astech
            // 6) Cannot be Astech if the primary role is a tech role
            // 7) Cannot be a medical staff role if the primary role is a Medic
            // 8) Cannot be Medic if the primary role is one of the medical staff roles
            if (role.isNone()) {
                return true;
            } else if (role.isDependent()
                    || getPrimaryRole().isDependent()
                    || (getPrimaryRole() == role)
                    || (role.isTechSecondary() && getPrimaryRole().isAstech())
                    || (role.isAstech() && getPrimaryRole().isTech())
                    || (role.isMedicalStaff() && getPrimaryRole().isMedic())
                    || (role.isMedic() && getPrimaryRole().isMedicalStaff())) {
                return false;
            }
        }

        switch (role) {
            case MECHWARRIOR:
                return hasSkill(SkillType.S_GUN_MECH) && hasSkill(SkillType.S_PILOT_MECH);
            case LAM_PILOT:
                return hasSkill(SkillType.S_GUN_MECH) && hasSkill(SkillType.S_PILOT_MECH)
                        && hasSkill(SkillType.S_GUN_AERO) && hasSkill(SkillType.S_PILOT_AERO);
            case GROUND_VEHICLE_DRIVER:
                return hasSkill(SkillType.S_PILOT_GVEE);
            case NAVAL_VEHICLE_DRIVER:
                return hasSkill(SkillType.S_PILOT_NVEE);
            case VTOL_PILOT:
                return hasSkill(SkillType.S_PILOT_VTOL);
            case VEHICLE_GUNNER:
                return hasSkill(SkillType.S_GUN_VEE);
            case VEHICLE_CREW:
                return hasSkill(SkillType.S_TECH_MECHANIC) && getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
            case AEROSPACE_PILOT:
                return hasSkill(SkillType.S_GUN_AERO) && hasSkill(SkillType.S_PILOT_AERO);
            case CONVENTIONAL_AIRCRAFT_PILOT:
                return hasSkill(SkillType.S_GUN_JET) && hasSkill(SkillType.S_PILOT_JET);
            case PROTOMECH_PILOT:
                return hasSkill(SkillType.S_GUN_PROTO);
            case BATTLE_ARMOUR:
                return hasSkill(SkillType.S_GUN_BA);
            case SOLDIER:
                return hasSkill(SkillType.S_SMALL_ARMS);
            case VESSEL_PILOT:
                return hasSkill(SkillType.S_PILOT_SPACE);
            case VESSEL_CREW:
                return hasSkill(SkillType.S_TECH_VESSEL);
            case VESSEL_GUNNER:
                return hasSkill(SkillType.S_GUN_SPACE);
            case VESSEL_NAVIGATOR:
                return hasSkill(SkillType.S_NAV);
            case MECH_TECH:
                return hasSkill(SkillType.S_TECH_MECH) && getSkill(SkillType.S_TECH_MECH).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
            case MECHANIC:
                return hasSkill(SkillType.S_TECH_MECHANIC) && getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
            case AERO_TECH:
                return hasSkill(SkillType.S_TECH_AERO) && getSkill(SkillType.S_TECH_AERO).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
            case BA_TECH:
                return hasSkill(SkillType.S_TECH_BA) && getSkill(SkillType.S_TECH_BA).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
            case ASTECH:
                return hasSkill(SkillType.S_ASTECH);
            case DOCTOR:
                return hasSkill(SkillType.S_DOCTOR) && getSkill(SkillType.S_DOCTOR).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
            case MEDIC:
                return hasSkill(SkillType.S_MEDTECH);
            case ADMINISTRATOR_COMMAND:
            case ADMINISTRATOR_LOGISTICS:
            case ADMINISTRATOR_TRANSPORT:
            case ADMINISTRATOR_HR:
                return hasSkill(SkillType.S_ADMIN);
            case DEPENDENT:
            case NONE:
                return true;
            default:
                return false;
        }
    }
    //endregion Personnel Roles

    public PersonnelStatus getStatus() {
        return status;
    }

    /**
     * This is used to change the person's PersonnelStatus
     * @param campaign the campaign the person is part of
     * @param status the person's new PersonnelStatus
     */
    public void changeStatus(Campaign campaign, PersonnelStatus status) {
        if (status == getStatus()) { // no change means we don't need to process anything
            return;
        } else if (getStatus().isKIA()) {
            // remove date of death for resurrection
            setDateOfDeath(null);
        }

        switch (status) {
            case ACTIVE:
                if (getStatus().isMIA()) {
                    ServiceLogger.recoveredMia(this, campaign.getLocalDate());
                } else if (getStatus().isDead()) {
                    ServiceLogger.resurrected(this, campaign.getLocalDate());
                } else {
                    ServiceLogger.rehired(this, campaign.getLocalDate());
                }
                setRetirement(null);
                break;
            case RETIRED:
                ServiceLogger.retired(this, campaign.getLocalDate());
                if (campaign.getCampaignOptions().useRetirementDateTracking()) {
                    setRetirement(campaign.getLocalDate());
                }
                break;
            case MIA:
                ServiceLogger.mia(this, campaign.getLocalDate());
                break;
            case KIA:
                ServiceLogger.kia(this, campaign.getLocalDate());
                break;
            case NATURAL_CAUSES:
                MedicalLogger.diedOfNaturalCauses(this, campaign.getLocalDate());
                ServiceLogger.passedAway(this, campaign.getLocalDate(), status.toString());
                break;
            case WOUNDS:
                MedicalLogger.diedFromWounds(this, campaign.getLocalDate());
                ServiceLogger.passedAway(this, campaign.getLocalDate(), status.toString());
                break;
            case DISEASE:
                MedicalLogger.diedFromDisease(this, campaign.getLocalDate());
                ServiceLogger.passedAway(this, campaign.getLocalDate(), status.toString());
                break;
            case OLD_AGE:
                MedicalLogger.diedOfOldAge(this, campaign.getLocalDate());
                ServiceLogger.passedAway(this, campaign.getLocalDate(), status.toString());
                break;
            case PREGNANCY_COMPLICATIONS:
                // The child might be able to be born, albeit into a world without their mother.
                // This can be manually set by males and for those who are not pregnant. This is
                // purposeful, to allow for player customization, and thus we first check if someone
                // is pregnant before having the birth
                if (isPregnant()) {
                    int pregnancyWeek = getPregnancyWeek(campaign.getLocalDate());
                    double babyBornChance;
                    if (pregnancyWeek > 35) {
                        babyBornChance = 0.99;
                    } else if (pregnancyWeek > 29) {
                        babyBornChance = 0.95;
                    } else if (pregnancyWeek > 25) {
                        babyBornChance = 0.9;
                    } else if (pregnancyWeek == 25) {
                        babyBornChance = 0.8;
                    } else if (pregnancyWeek == 24) {
                        babyBornChance = 0.5;
                    } else if (pregnancyWeek == 23) {
                        babyBornChance = 0.25;
                    } else {
                        babyBornChance = 0;
                    }

                    if (Compute.randomFloat() < babyBornChance) {
                        birth(campaign);
                    }
                }
                MedicalLogger.diedFromPregnancyComplications(this, campaign.getLocalDate());
                ServiceLogger.passedAway(this, campaign.getLocalDate(), status.toString());
                break;
        }

        setStatus(status);

        if (status.isDead()) {
            setDateOfDeath(campaign.getLocalDate());
            // Don't forget to tell the spouse
            if (getGenealogy().hasSpouse() && !getGenealogy().getSpouse().getStatus().isDeadOrMIA()) {
                Divorce divorceType = campaign.getCampaignOptions().getKeepMarriedNameUponSpouseDeath()
                        ? Divorce.ORIGIN_CHANGE_SURNAME : Divorce.SPOUSE_CHANGE_SURNAME;
                divorceType.divorce(this, campaign);
            }
        }

        if (!status.isActive()) {
            setDoctorId(null, campaign.getCampaignOptions().getNaturalHealingWaitingPeriod());

            // If we're assigned to a unit, remove us from it
            if (getUnit() != null) {
                getUnit().remove(this, true);
            }

            // Clear Tech Setup
            removeAllTechJobs(campaign);
        }

        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    /**
     * This is used to directly set the Person's PersonnelStatus without any processing
     * @param status the person's new status
     */
    public void setStatus(PersonnelStatus status) {
        this.status = status;
    }

    public int getIdleMonths() {
        return idleMonths;
    }

    public void setIdleMonths(int m) {
        this.idleMonths = m;
    }

    public int getDaysToWaitForHealing() {
        return daysToWaitForHealing;
    }

    public void setDaysToWaitForHealing(int d) {
        this.daysToWaitForHealing = d;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Gender getGender() {
        return gender;
    }

    public void setBirthday(LocalDate date) {
        this.birthday = date;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public LocalDate getDateOfDeath() {
        return dateOfDeath;
    }

    public String getDeathDateAsString() {
        if (getDateOfDeath() == null) {
            return "";
        } else {
            return MekHQ.getMekHQOptions().getDisplayFormattedDate(getDateOfDeath());
        }
    }

    public void setDateOfDeath(LocalDate date) {
        this.dateOfDeath = date;
    }

    public int getAge(LocalDate today) {
        // Get age based on year
        if (getDateOfDeath() != null) {
            //use date of death instead of birthday
            today = getDateOfDeath();
        }

        return Math.toIntExact(ChronoUnit.YEARS.between(getBirthday(), today));
    }

    public void setRecruitment(LocalDate date) {
        this.recruitment = date;
    }

    public LocalDate getRecruitment() {
        return recruitment;
    }

    public String getRecruitmentAsString() {
        if (getRecruitment() == null) {
            return "";
        } else {
            return MekHQ.getMekHQOptions().getDisplayFormattedDate(getRecruitment());
        }
    }

    public String getTimeInService(Campaign campaign) {
        // Get time in service based on year
        if (getRecruitment() == null) {
            //use "" they haven't been recruited or are dependents
            return "";
        }

        LocalDate today = campaign.getLocalDate();

        // If the person is dead, we only care about how long they spent in service to the company
        if (getDateOfDeath() != null) {
            //use date of death instead of the current day
            today = getDateOfDeath();
        }

        return campaign.getCampaignOptions().getTimeInServiceDisplayFormat()
                .getDisplayFormattedOutput(getRecruitment(), today);
    }

    public void setLastRankChangeDate(LocalDate date) {
        this.lastRankChangeDate = date;
    }

    public LocalDate getLastRankChangeDate() {
        return lastRankChangeDate;
    }

    public String getLastRankChangeDateAsString() {
        if (getLastRankChangeDate() == null) {
            return "";
        } else {
            return MekHQ.getMekHQOptions().getDisplayFormattedDate(getLastRankChangeDate());
        }
    }

    public String getTimeInRank(Campaign campaign) {
        if (getLastRankChangeDate() == null) {
            return "";
        }

        LocalDate today = campaign.getLocalDate();

        // If the person is dead, we only care about how long it was from their last promotion till they died
        if (getDateOfDeath() != null) {
            //use date of death instead of the current day
            today = getDateOfDeath();
        }

        return campaign.getCampaignOptions().getTimeInRankDisplayFormat()
                .getDisplayFormattedOutput(getLastRankChangeDate(), today);
    }

    public void setRetirement(LocalDate date) {
        this.retirement = date;
    }

    public LocalDate getRetirement() {
        return retirement;
    }

    public String getRetirementAsString() {
        if (getRetirement() == null) {
            return "";
        } else {
            return MekHQ.getMekHQOptions().getDisplayFormattedDate(getRetirement());
        }
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public boolean isChild() {
        return (getAge(getCampaign().getLocalDate()) <= 13);
    }

    public Genealogy getGenealogy() {
        return genealogy;
    }

    public void setGenealogy(Genealogy genealogy) {
        this.genealogy = genealogy;
    }

    //region Pregnancy
    public boolean isTryingToConceive() {
        return tryingToConceive;
    }

    public void setTryingToConceive(boolean tryingToConceive) {
        this.tryingToConceive = tryingToConceive;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getExpectedDueDate() {
        return expectedDueDate;
    }

    public void setExpectedDueDate(LocalDate expectedDueDate) {
        this.expectedDueDate = expectedDueDate;
    }

    public int getPregnancyWeek(LocalDate today) {
        return Math.toIntExact(ChronoUnit.WEEKS.between(getExpectedDueDate()
                .minus(PREGNANCY_STANDARD_DURATION, ChronoUnit.DAYS)
                .plus(1, ChronoUnit.DAYS), today));
    }

    public boolean isPregnant() {
        return dueDate != null;
    }

    /**
     * This is used to determine if a person can procreate
     * @param campaign the campaign the person was in
     * @return true if they can, otherwise false
     */
    public boolean canProcreate(Campaign campaign) {
        return getGender().isFemale() && isTryingToConceive() && !isPregnant() && !isDeployed()
                && !isChild() && (getAge(campaign.getLocalDate()) < 51);
    }

    public void procreate(Campaign campaign) {
        if (canProcreate(campaign)) {
            boolean conceived = false;
            if (getGenealogy().hasSpouse()) {
                Person spouse = getGenealogy().getSpouse();
                if (!spouse.isDeployed() && !spouse.getStatus().isDeadOrMIA() && !spouse.isChild()
                        && !(spouse.getGender() == getGender())) {
                    // setting is the decimal chance that this procreation attempt will create a child, base is 0.05%
                    conceived = (Compute.randomFloat() < (campaign.getCampaignOptions().getChanceProcreation()));
                }
            } else if (campaign.getCampaignOptions().useProcreationNoRelationship()) {
                // setting is the decimal chance that this procreation attempt will create a child, base is 0.005%
                conceived = (Compute.randomFloat() < (campaign.getCampaignOptions().getChanceProcreationNoRelationship()));
            }

            if (conceived) {
                addPregnancy(campaign);
            }
        }
    }

    public void addPregnancy(Campaign campaign) {
        LocalDate dueDate = campaign.getLocalDate();
        dueDate = dueDate.plus(PREGNANCY_STANDARD_DURATION, ChronoUnit.DAYS);
        setExpectedDueDate(dueDate);
        dueDate = dueDate.plus(PREGNANCY_MODIFY_DURATION.getAsInt(), ChronoUnit.DAYS);
        setDueDate(dueDate);

        int size = PREGNANCY_SIZE.getAsInt();
        extraData.set(PREGNANCY_CHILDREN_DATA, size);
        extraData.set(PREGNANCY_FATHER_DATA, (getGenealogy().hasSpouse())
                ? getGenealogy().getSpouse().getId().toString() : null);

        String sizeString = (size < PREGNANCY_MULTIPLE_NAMES.length) ? PREGNANCY_MULTIPLE_NAMES[size] : null;

        campaign.addReport(getHyperlinkedName() + " has conceived" + (sizeString == null ? "" : (" " + sizeString)));
        if (campaign.getCampaignOptions().logConception()) {
            MedicalLogger.hasConceived(this, campaign.getLocalDate(), sizeString);
            if (getGenealogy().hasSpouse()) {
                PersonalLogger.spouseConceived(getGenealogy().getSpouse(),
                        getFullName(), getCampaign().getLocalDate(), sizeString);
            }
        }
    }

    /**
     * Removes a pregnancy and clears all related data from the current person
     */
    public void removePregnancy() {
        setDueDate(null);
        setExpectedDueDate(null);
        extraData.set(PREGNANCY_CHILDREN_DATA, null);
        extraData.set(PREGNANCY_FATHER_DATA, null);
    }

    /**
     * This method is how a person gives birth to a number of babies and have them added to the campaign
     * @param campaign the campaign to add the baby in question to
     */
    public void birth(Campaign campaign) {
        // Determine the number of children
        int size = extraData.get(PREGNANCY_CHILDREN_DATA, 1);

        // Determine father information
        Person father = (getExtraData().get(PREGNANCY_FATHER_DATA) != null)
                ? campaign.getPerson(UUID.fromString(getExtraData().get(PREGNANCY_FATHER_DATA))) : null;
        father = campaign.getCampaignOptions().determineFatherAtBirth()
                ? Utilities.nonNull(getGenealogy().getSpouse(), father) : father;

        // Determine Prisoner Status
        final PrisonerStatus prisonerStatus = campaign.getCampaignOptions().getPrisonerBabyStatus()
                ? getPrisonerStatus() : PrisonerStatus.FREE;

        // Output a specific report to the campaign if they are giving birth to multiple children
        if (PREGNANCY_MULTIPLE_NAMES[size] != null) {
            campaign.addReport(String.format("%s has given birth to %s!", getHyperlinkedName(),
                    PREGNANCY_MULTIPLE_NAMES[size]));
        }

        // Create Babies
        for (int i = 0; i < size; i++) {
            // Create the specific baby
            Person baby = campaign.newDependent(true);
            String surname = campaign.getCampaignOptions().getBabySurnameStyle()
                    .generateBabySurname(this, father, baby.getGender());
            baby.setSurname(surname);
            baby.setBirthday(campaign.getLocalDate());

            // Recruit the baby
            campaign.recruitPerson(baby, prisonerStatus, true, true);

            // Create genealogy information
            baby.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, this);
            getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, baby);
            if (father != null) {
                baby.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, father);
                father.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, baby);
            }

            // Create reports and log the birth
            campaign.addReport(String.format("%s has given birth to %s, a baby %s!", getHyperlinkedName(),
                    baby.getHyperlinkedName(), GenderDescriptors.BOY_GIRL.getDescriptor(baby.getGender())));
            if (campaign.getCampaignOptions().logConception()) {
                MedicalLogger.deliveredBaby(this, baby, campaign.getLocalDate());
                if (father != null) {
                    PersonalLogger.ourChildBorn(father, baby, getFullName(), campaign.getLocalDate());
                }
            }
        }

        // Cleanup Data
        removePregnancy();
    }
    //endregion Pregnancy

    //region Marriage
    public boolean isTryingToMarry() {
        return tryingToMarry;
    }

    public void setTryingToMarry(boolean tryingToMarry) {
        this.tryingToMarry = tryingToMarry;
    }

    /**
     * Determines if another person is a safe spouse for the current person
     * @param person the person to determine if they are a safe spouse
     * @param campaign the campaign to use to determine if they are a safe spouse
     */
    public boolean safeSpouse(Person person, Campaign campaign) {
        // Huge convoluted return statement, with the following restrictions
        // can't marry yourself
        // can't marry someone who is already married
        // can't marry someone who doesn't want to be married
        // can't marry a prisoner, unless you are also a prisoner (this is purposely left open for prisoners to marry who they want)
        // can't marry a person who is dead or MIA
        // can't marry inactive personnel (this is to show how they aren't part of the force anymore)
        // TODO : can't marry anyone who is not located at the same planet as the person - GitHub #1672: Implement current planet tracking for personnel
        // can't marry a close relative
        return (
                !this.equals(person)
                && !person.getGenealogy().hasSpouse()
                && person.isTryingToMarry()
                && person.oldEnoughToMarry(campaign)
                && (!person.getPrisonerStatus().isPrisoner() || getPrisonerStatus().isPrisoner())
                && !person.getStatus().isDeadOrMIA()
                && person.getStatus().isActive()
                && !getGenealogy().checkMutualAncestors(person, getCampaign())
        );
    }

    public boolean oldEnoughToMarry(Campaign campaign) {
        return (getAge(campaign.getLocalDate()) >= campaign.getCampaignOptions().getMinimumMarriageAge());
    }

    public void randomMarriage(Campaign campaign) {
        // Don't attempt to generate is someone isn't trying to marry, has a spouse,
        // isn't old enough to marry, or is actively deployed
        if (!isTryingToMarry() || getGenealogy().hasSpouse() || !oldEnoughToMarry(campaign) || isDeployed()) {
            return;
        }

        // setting is the fractional chance that this attempt at finding a marriage will result in one
        if (Compute.randomFloat() < (campaign.getCampaignOptions().getChanceRandomMarriages())) {
            addRandomSpouse(false, campaign);
        } else if (campaign.getCampaignOptions().useRandomSameSexMarriages()) {
            if (Compute.randomFloat() < (campaign.getCampaignOptions().getChanceRandomSameSexMarriages())) {
                addRandomSpouse(true, campaign);
            }
        }
    }

    public void addRandomSpouse(boolean sameSex, Campaign campaign) {
        List<Person> potentials = new ArrayList<>();
        Gender gender = sameSex ? getGender() : (getGender().isMale() ? Gender.FEMALE : Gender.MALE);
        for (Person p : campaign.getActivePersonnel()) {
            if (isPotentialRandomSpouse(p, gender, campaign)) {
                potentials.add(p);
            }
        }

        int n = potentials.size();
        if (n > 0) {
            Marriage.WEIGHTED.marry(campaign, this, potentials.get(Compute.randomInt(n)));
        }
    }

    public boolean isPotentialRandomSpouse(Person p, Gender gender, Campaign campaign) {
        if ((p.getGender() != gender) || !safeSpouse(p, campaign)
                || !(getPrisonerStatus().isFree()
                || (getPrisonerStatus().isPrisoner() && p.getPrisonerStatus().isPrisoner()))) {
            return false;
        }

        int ageDifference = Math.abs(p.getAge(campaign.getLocalDate()) - getAge(campaign.getLocalDate()));

        return (ageDifference <= campaign.getCampaignOptions().getMarriageAgeRange());
    }
    //endregion Marriage

    //region Experience
    public int getXP() {
        return xp;
    }

    public void setXP(int xp) {
        this.xp = xp;
    }

    public void awardXP(int xp) {
        this.xp += xp;
    }
    //endregion Experience

    public int getAcquisitions() {
        return acquisitions;
    }

    public void setAcquisition(int a) {
        acquisitions = a;
    }

    public void incrementAcquisition() {
        acquisitions++;
    }

    public void setDoctorId(UUID t, int daysToWait) {
        this.doctorId = t;
        this.daysToWaitForHealing = daysToWait;
    }

    public boolean checkNaturalHealing(int daysToWait) {
        if (needsFixing() && daysToWaitForHealing <= 0 && doctorId == null) {
            heal();
            daysToWaitForHealing = daysToWait;
            return true;
        }
        return false;
    }

    public void decrementDaysToWaitForHealing() {
        if (daysToWaitForHealing > 0) {
            daysToWaitForHealing--;
        }
    }

    public boolean isDeployed() {
        if (null != getUnit()) {
            return (getUnit().getScenarioId() != -1);
        }
        return false;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String s) {
        this.biography = s;
    }

    public ExtraData getExtraData() {
        return extraData;
    }

    //region File I/O
    public void writeToXML(final Campaign campaign, final PrintWriter pw1, int indent) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<person id=\"" + id
                + "\" type=\"" + this.getClass().getName() + "\">");
        try {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "id", id.toString());

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

            // Always save the primary role
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "primaryRole", getPrimaryRole().name());
            if (!getSecondaryRole().isNone()) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "secondaryRole", getSecondaryRole().name());
            }
            if (primaryDesignator != ROMDesignation.NONE) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "primaryDesignator", primaryDesignator.name());
            }
            if (secondaryDesignator != ROMDesignation.NONE) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "secondaryDesignator", secondaryDesignator.name());
            }
            if (commander) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "commander", true);
            }
            // Always save the person's origin faction
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "faction", originFaction.getShortName());
            if (originPlanet != null) {
                pw1.println(MekHqXmlUtil.indentStr(indent + 1)
                        + "<planetId systemId=\""
                        + originPlanet.getParentSystem().getId()
                        + "\">"
                        + originPlanet.getId()
                        + "</planetId>");
            }
            // Always save whether or not someone is a clanner
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "clan", clan);
            if (phenotype != Phenotype.NONE) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "phenotype", phenotype.name());
            }
            if (!StringUtil.isNullOrEmpty(bloodname)) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "bloodname", bloodname);
            }
            if (!StringUtil.isNullOrEmpty(biography)) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "biography", biography);
            }
            if (idleMonths > 0) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "idleMonths", idleMonths);
            }
            if (!genealogy.isEmpty()) {
                genealogy.writeToXML(pw1, indent + 1);
            }
            if (!isTryingToMarry()) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "tryingToMarry", false);
            }
            if (!isTryingToConceive()) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "tryingToConceive", false);
            }
            if (dueDate != null) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "dueDate",
                        MekHqXmlUtil.saveFormattedDate(dueDate));
            }
            if (expectedDueDate != null) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "expectedDueDate",
                        MekHqXmlUtil.saveFormattedDate(expectedDueDate));
            }
            getPortrait().writeToXML(pw1, indent + 1);
            // Always save the current XP
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "xp", xp);
            if (daysToWaitForHealing != 0) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "daysToWaitForHealing", daysToWaitForHealing);
            }
            // Always save the person's gender, as it would otherwise get confusing fast
            MekHqXmlUtil.writeSimpleXMLTag(pw1, indent + 1, "gender", getGender().name());
            if (!getRankSystem().equals(campaign.getRankSystem())) {
                MekHqXmlUtil.writeSimpleXMLTag(pw1, indent + 1, "rankSystem", getRankSystem().getCode());
            }
            // Always save a person's rank
            MekHqXmlUtil.writeSimpleXMLTag(pw1, indent + 1, "rank", getRankNumeric());
            if (getRankLevel() != 0) {
                MekHqXmlUtil.writeSimpleXMLTag(pw1, indent + 1, "rankLevel", getRankLevel());
            }
            if (!getManeiDominiClass().isNone()) {
                MekHqXmlUtil.writeSimpleXMLTag(pw1, indent + 1, "maneiDominiClass", getManeiDominiClass().name());
            }
            if (!getManeiDominiRank().isNone()) {
                MekHqXmlUtil.writeSimpleXMLTag(pw1, indent + 1, "maneiDominiRank", getManeiDominiRank().name());
            }
            if (nTasks > 0) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "nTasks", nTasks);
            }
            if (doctorId != null) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "doctorId", doctorId.toString());
            }
            if (getUnit() != null) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "unitId", getUnit().getId());
            }
            if (!salary.equals(Money.of(-1))) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "salary", salary.toXmlString());
            }
            if (!totalEarnings.equals(Money.of(0))) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "totalEarnings", totalEarnings.toXmlString());
            }
            // Always save a person's status, to make it easy to parse the personnel saved data
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "status", status.name());
            if (prisonerStatus != PrisonerStatus.FREE) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "prisonerStatus", prisonerStatus.name());
            }
            if (hits > 0) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "hits", hits);
            }
            if (toughness != 0) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "toughness", toughness);
            }
            if (minutesLeft > 0) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "minutesLeft", minutesLeft);
            }
            if (overtimeLeft > 0) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "overtimeLeft", overtimeLeft);
            }
            if (birthday != null) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "birthday",
                        MekHqXmlUtil.saveFormattedDate(birthday));
            }
            if (dateOfDeath != null) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "deathday",
                        MekHqXmlUtil.saveFormattedDate(dateOfDeath));
            }
            if (recruitment != null) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "recruitment",
                        MekHqXmlUtil.saveFormattedDate(recruitment));
            }
            if (lastRankChangeDate != null) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "lastRankChangeDate",
                        MekHqXmlUtil.saveFormattedDate(lastRankChangeDate));
            }
            if (getRetirement() != null) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "retirement",
                        MekHqXmlUtil.saveFormattedDate(getRetirement()));
            }
            for (Skill skill : skills.getSkills()) {
                skill.writeToXml(pw1, indent + 1);
            }
            if (countOptions(PilotOptions.LVL3_ADVANTAGES) > 0) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "advantages",
                        getOptionList("::", PilotOptions.LVL3_ADVANTAGES));
            }
            if (countOptions(PilotOptions.EDGE_ADVANTAGES) > 0) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "edge",
                        getOptionList("::", PilotOptions.EDGE_ADVANTAGES));
                // For support personnel, write an available edge value
                if (hasSupportRole(true) || isEngineer()) {
                    MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "edgeAvailable", getCurrentEdge());
                }
            }
            if (countOptions(PilotOptions.MD_ADVANTAGES) > 0) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "implants",
                        getOptionList("::", PilotOptions.MD_ADVANTAGES));
            }
            if (!techUnits.isEmpty()) {
                MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent + 1, "techUnitIds");
                for (Unit unit : techUnits) {
                    MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 2, "id", unit.getId());
                }
                MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent + 1, "techUnitIds");
            }
            if (!personnelLog.isEmpty()) {
                MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent + 1, "personnelLog");
                for (LogEntry entry : personnelLog) {
                    entry.writeToXml(pw1, indent + 2);
                }
                MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent + 1, "personnelLog");
            }
            if (!missionLog.isEmpty()) {
                MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent + 1, "missionLog");
                for (LogEntry entry : missionLog) {
                    entry.writeToXml(pw1, indent + 2);
                }
                MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent + 1, "missionLog");
            }
            if (!getAwardController().getAwards().isEmpty()) {
                MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent + 1, "awards");
                for (Award award : getAwardController().getAwards()) {
                    award.writeToXml(pw1, indent + 2);
                }
                MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent + 1, "awards");
            }
            if (injuries.size() > 0) {
                MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent + 1, "injuries");
                for (Injury injury : injuries) {
                    injury.writeToXml(pw1, indent + 2);
                }
                MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent + 1, "injuries");
            }
            if (founder) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "founder", true);
            }
            if (originalUnitWeight != EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "originalUnitWeight", originalUnitWeight);
            }
            if (originalUnitTech != TECH_IS1) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "originalUnitTech", originalUnitTech);
            }
            if (originalUnitId != null) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "originalUnitId", originalUnitId.toString());
            }
            if (acquisitions != 0) {
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "acquisitions", acquisitions);
            }
            if (!extraData.isEmpty()) {
                extraData.writeToXml(pw1);
            }
        } catch (Exception e) {
            MekHQ.getLogger().error("Failed to write " + getFullName() + " to the XML File", e);
            throw e; // we want to rethrow to ensure that that the save fails
        }
        pw1.println(MekHqXmlUtil.indentStr(indent) + "</person>");
    }

    public static Person generateInstanceFromXML(Node wn, Campaign c, Version version) {
        Person retVal = new Person(c);

        try {
            // Okay, now load Person-specific fields!
            NodeList nl = wn.getChildNodes();

            String advantages = null;
            String edge = null;
            String implants = null;

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("name")) { // legacy - 0.47.5 removal
                    retVal.migrateName(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("preNominal")) {
                    retVal.setPreNominalDirect(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("givenName")) {
                    retVal.setGivenNameDirect(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("surname")) {
                    retVal.setSurnameDirect(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("postNominal")) {
                    retVal.setPostNominalDirect(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("maidenName")) {
                    retVal.setMaidenName(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("callsign")) {
                    retVal.setCallsignDirect(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("commander")) {
                    retVal.commander = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("dependent")) { // Legacy, 0.49.1 removal
                    // Legacy setup was as Astechs, but people (including me) often forgot to remove
                    // the flag so... just ignoring if they aren't astechs
                    if (retVal.getPrimaryRole().isAstech()) {
                        retVal.setPrimaryRoleDirect(PersonnelRole.DEPENDENT);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("faction")) {
                    retVal.originFaction = Factions.getInstance().getFaction(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("planetId")) {
                    String systemId = wn2.getAttributes().getNamedItem("systemId").getTextContent().trim();
                    String planetId = wn2.getTextContent().trim();
                    retVal.originPlanet = c.getSystemById(systemId).getPlanetById(planetId);
                } else if (wn2.getNodeName().equalsIgnoreCase("clan")) {
                    retVal.clan = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("phenotype")) {
                    retVal.phenotype = Phenotype.parseFromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("bloodname")) {
                    retVal.bloodname = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("biography")) {
                    retVal.biography = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("primaryRole")) {
                    final PersonnelRole primaryRole = PersonnelRole.parseFromString(wn2.getTextContent().trim());
                    if (version.isLowerThan("0.49.1") && primaryRole.isNone()) {
                        retVal.setPrimaryRoleDirect(PersonnelRole.DEPENDENT);
                    } else {
                        retVal.setPrimaryRoleDirect(primaryRole);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("secondaryRole")) {
                    retVal.setSecondaryRoleDirect(PersonnelRole.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("acquisitions")) {
                    retVal.acquisitions = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("primaryDesignator")) {
                    retVal.primaryDesignator = ROMDesignation.parseFromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("secondaryDesignator")) {
                    retVal.secondaryDesignator = ROMDesignation.parseFromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("daysToWaitForHealing")) {
                    retVal.daysToWaitForHealing = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("idleMonths")) {
                    retVal.idleMonths = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("id")) {
                    retVal.id = UUID.fromString(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("ancestors")) { // legacy - 0.47.6 removal
                    CampaignXmlParser.addToAncestryMigrationMap(UUID.fromString(wn2.getTextContent().trim()), retVal);
                } else if (wn2.getNodeName().equalsIgnoreCase("spouse")) { // legacy - 0.47.6 removal
                    retVal.getGenealogy().setSpouse(new PersonIdReference(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("formerSpouses")) { // legacy - 0.47.6 removal
                    retVal.getGenealogy().loadFormerSpouses(wn2.getChildNodes());
                } else if (wn2.getNodeName().equalsIgnoreCase("genealogy")) {
                    retVal.getGenealogy().fillFromXML(wn2.getChildNodes());
                } else if (wn2.getNodeName().equalsIgnoreCase("tryingToMarry")) {
                    retVal.tryingToMarry = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("tryingToConceive")) {
                    retVal.tryingToConceive = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("dueDate")) {
                    retVal.dueDate = MekHqXmlUtil.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("expectedDueDate")) {
                    retVal.expectedDueDate = MekHqXmlUtil.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase(Portrait.XML_TAG)) {
                    retVal.setPortrait(Portrait.parseFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase("portraitCategory")) { // Legacy - 0.49.3 removal
                    retVal.getPortrait().setCategory(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("portraitFile")) { // Legacy - 0.49.3 removal
                    retVal.getPortrait().setFilename(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("xp")) {
                    retVal.xp = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("nTasks")) {
                    retVal.nTasks = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("hits")) {
                    retVal.hits = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("gender")) {
                    retVal.setGender(Gender.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("rankSystem")) {
                    final RankSystem rankSystem;

                    if (version.isLowerThan("0.49.0")) {
                        final int rankSystemNumeric = Integer.parseInt(wn2.getTextContent().trim());
                        rankSystem = (rankSystemNumeric >= 0) ? Ranks.getRankSystemFromCode(PersonMigrator
                                .migrateRankSystemCode(rankSystemNumeric)) : retVal.getRankSystem();
                    } else {
                        rankSystem = Ranks.getRankSystemFromCode(wn2.getTextContent().trim());
                    }

                    if (rankSystem != null) {
                        retVal.setRankSystemDirect(rankSystem);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("rank")) {
                    retVal.setRank(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("rankLevel")) {
                    retVal.setRankLevel(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("maneiDominiClass")) {
                    retVal.setManeiDominiClassDirect(ManeiDominiClass.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("maneiDominiRank")) {
                    retVal.setManeiDominiRankDirect(ManeiDominiRank.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("doctorId")) {
                    if (!wn2.getTextContent().equals("null")) {
                        retVal.doctorId = UUID.fromString(wn2.getTextContent());
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("unitId")) {
                    if (!wn2.getTextContent().equals("null")) {
                        retVal.unit = new PersonUnitRef(UUID.fromString(wn2.getTextContent()));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("status")) {
                    retVal.setStatus(PersonnelStatus.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("prisonerStatus")) {
                    retVal.prisonerStatus = PrisonerStatus.parseFromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("willingToDefect")) { // Legacy
                    if (Boolean.parseBoolean(wn2.getTextContent().trim())) {
                        retVal.prisonerStatus = PrisonerStatus.PRISONER_DEFECTOR;
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("salary")) {
                    retVal.salary = Money.fromXmlString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("totalEarnings")) {
                    retVal.totalEarnings = Money.fromXmlString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("minutesLeft")) {
                    retVal.minutesLeft = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("overtimeLeft")) {
                    retVal.overtimeLeft = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("birthday")) {
                    retVal.birthday = MekHqXmlUtil.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("deathday")) {
                    retVal.dateOfDeath = MekHqXmlUtil.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("recruitment")) {
                    retVal.recruitment = MekHqXmlUtil.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("lastRankChangeDate")) {
                    retVal.lastRankChangeDate = MekHqXmlUtil.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("retirement")) {
                    retVal.setRetirement(MekHqXmlUtil.parseDate(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("advantages")) {
                    advantages = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("edge")) {
                    edge = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("edgeAvailable")) {
                    retVal.currentEdge = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("implants")) {
                    implants = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("toughness")) {
                    retVal.toughness = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("pilotHits")) {
                    retVal.hits = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("skill")) {
                    Skill s = Skill.generateInstanceFromXML(wn2);
                    if ((s != null) && (s.getType() != null)) {
                        retVal.skills.addSkill(s.getType().getName(), s);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("techUnitIds")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("id")) {
                            MekHQ.getLogger().error("Unknown node type not loaded in techUnitIds nodes: " + wn3.getNodeName());
                            continue;
                        }
                        retVal.addTechUnit(new PersonUnitRef(UUID.fromString(wn3.getTextContent())));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("personnelLog")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("logEntry")) {
                            MekHQ.getLogger().error("Unknown node type not loaded in personnel log nodes: " + wn3.getNodeName());
                            continue;
                        }

                        LogEntry entry = LogEntryFactory.getInstance().generateInstanceFromXML(wn3);

                        // If the version of this campaign is earlier than 0.45.4,
                        // we didn't have the mission log separated from the personnel log,
                        // so we need to separate the log entries manually
                        if (version.isLowerThan("0.45.4")) {
                            if (entry.getDesc().startsWith(getMissionParticipatedString())) {
                                retVal.addMissionLogEntry(entry);
                            } else {
                                retVal.addLogEntry(entry);
                            }
                        } else {
                            retVal.addLogEntry(entry);
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("missionLog")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("logEntry")) {
                            MekHQ.getLogger().error("Unknown node type not loaded in mission log nodes: " + wn3.getNodeName());
                            continue;
                        }
                        retVal.addMissionLogEntry(LogEntryFactory.getInstance().generateInstanceFromXML(wn3));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("awards")) {
                    final boolean defaultSetMigrationRequired = version.isLowerThan("0.47.15");
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {

                        Node wn3 = nl2.item(y);

                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("award")) {
                            MekHQ.getLogger().error("Unknown node type not loaded in personnel log nodes: " + wn3.getNodeName());
                            continue;
                        }

                        retVal.getAwardController().addAwardFromXml(AwardsFactory.getInstance()
                                .generateNewFromXML(wn3, defaultSetMigrationRequired));
                    }

                } else if (wn2.getNodeName().equalsIgnoreCase("injuries")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("injury")) {
                            MekHQ.getLogger().error("Unknown node type not loaded in injury nodes: " + wn3.getNodeName());
                            continue;
                        }
                        retVal.injuries.add(Injury.generateInstanceFromXML(wn3));
                    }
                    LocalDate now = c.getLocalDate();
                    retVal.injuries.stream().filter(inj -> (null == inj.getStart()))
                        .forEach(inj -> inj.setStart(now.minusDays(inj.getOriginalTime() - inj.getTime())));
                } else if (wn2.getNodeName().equalsIgnoreCase("founder")) {
                    retVal.founder = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("originalUnitWeight")) {
                    retVal.originalUnitWeight = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("originalUnitTech")) {
                    retVal.originalUnitTech = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("originalUnitId")) {
                    retVal.originalUnitId = UUID.fromString(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("extraData")) {
                    retVal.extraData = ExtraData.createFromXml(wn2);
                } else if (wn2.getNodeName().equalsIgnoreCase("honorific")) { //Legacy, removed in 0.49.3
                    retVal.setPostNominalDirect(wn2.getTextContent());
                }
            }

            retVal.setFullName(); // this sets the name based on the loaded values

            if (version.isLowerThan("0.47.5") && (retVal.getExpectedDueDate() == null)
                    && (retVal.getDueDate() != null)) {
                retVal.setExpectedDueDate(retVal.getDueDate());
            }

            if ((null != advantages) && (advantages.trim().length() > 0)) {
                StringTokenizer st = new StringTokenizer(advantages, "::");
                while (st.hasMoreTokens()) {
                    String adv = st.nextToken();
                    String advName = Crew.parseAdvantageName(adv);
                    Object value = Crew.parseAdvantageValue(adv);

                    try {
                        retVal.getOptions().getOption(advName).setValue(value);
                    } catch (Exception e) {
                        MekHQ.getLogger().error("Error restoring advantage: " + adv);
                    }
                }
            }
            if ((null != edge) && (edge.trim().length() > 0)) {
                StringTokenizer st = new StringTokenizer(edge, "::");
                while (st.hasMoreTokens()) {
                    String adv = st.nextToken();
                    String advName = Crew.parseAdvantageName(adv);
                    Object value = Crew.parseAdvantageValue(adv);

                    try {
                        retVal.getOptions().getOption(advName).setValue(value);
                    } catch (Exception e) {
                        MekHQ.getLogger().error("Error restoring edge: " + adv);
                    }
                }
            }
            if ((null != implants) && (implants.trim().length() > 0)) {
                StringTokenizer st = new StringTokenizer(implants, "::");
                while (st.hasMoreTokens()) {
                    String adv = st.nextToken();
                    String advName = Crew.parseAdvantageName(adv);
                    Object value = Crew.parseAdvantageValue(adv);

                    try {
                        retVal.getOptions().getOption(advName).setValue(value);
                    } catch (Exception e) {
                        MekHQ.getLogger().error("Error restoring implants: " + adv);
                    }
                }
            }

            // Ensure the Genealogy Origin is set to this
            retVal.getGenealogy().setOrigin(retVal);

            // Fixing Prisoner Ranks - 0.47.X Fix
            if (retVal.getRankNumeric() < 0) {
                retVal.setRank(0);
            }
        } catch (Exception e) {
            MekHQ.getLogger().error("Failed to read person " + retVal.getFullName() + " from file", e);
            retVal = null;
        }

        return retVal;
    }
    //endregion File I/O

    public void setSalary(Money s) {
        salary = s;
    }

    public Money getSalary() {
        if (!getPrisonerStatus().isFree()) {
            return Money.zero();
        }

        if (salary.isPositiveOrZero()) {
            return salary;
        }

        // If the salary is negative, then use the standard amounts
        // TODO : Figure out a way to allow negative salaries... could be used to simulate a Holovid
        // TODO : star paying to be part of the company, for example
        Money primaryBase = campaign.getCampaignOptions().getRoleBaseSalaries()[getPrimaryRole().ordinal()];
        primaryBase = primaryBase.multipliedBy(campaign.getCampaignOptions().getSalaryXPMultiplier(getExperienceLevel(false)));
        if (getPrimaryRole().isSoldierOrBattleArmour() && hasSkill(SkillType.S_ANTI_MECH)) {
            primaryBase = primaryBase.multipliedBy(campaign.getCampaignOptions().getSalaryAntiMekMultiplier());
        }

        Money secondaryBase = campaign.getCampaignOptions().getRoleBaseSalaries()[getSecondaryRole().ordinal()].dividedBy(2);
        secondaryBase = secondaryBase.multipliedBy(campaign.getCampaignOptions().getSalaryXPMultiplier(getExperienceLevel(true)));
        if (getPrimaryRole().isSoldierOrBattleArmour() && hasSkill(SkillType.S_ANTI_MECH)) {
            secondaryBase = secondaryBase.multipliedBy(campaign.getCampaignOptions().getSalaryAntiMekMultiplier());
        }

        //TODO: distinguish DropShip, JumpShip, and WarShip crew
        //TODO: Add era mod to salary calc..
        return primaryBase.plus(secondaryBase)
                .multipliedBy(getRank().isOfficer()
                        ? campaign.getCampaignOptions().getSalaryCommissionMultiplier()
                        : campaign.getCampaignOptions().getSalaryEnlistedMultiplier())
                .multipliedBy(getRank().getPayMultiplier());
    }

    /**
     * @return the person's total earnings
     */
    public Money getTotalEarnings() {
        return totalEarnings;
    }

    /**
     * This is used to pay a person
     * @param money the amount of money to add to their total earnings
     */
    public void payPerson(Money money) {
        totalEarnings = getTotalEarnings().plus(money);
    }

    /**
     * This is used to pay a person their salary
     */
    public void payPersonSalary() {
        if (getStatus().isActive()) {
            payPerson(getSalary());
        }
    }

    /**
     * This is used to pay a person their share value based on the value of a single share
     * @param money the value of a single share
     * @param sharesForAll whether or not all personnel have shares
     */
    public void payPersonShares(Money money, boolean sharesForAll) {
        int shares = getNumShares(sharesForAll);
        if (shares > 0) {
            payPerson(money.multipliedBy(shares));
        }
    }

    //region Ranks
    public RankSystem getRankSystem() {
        return rankSystem;
    }

    public void setRankSystem(final RankValidator rankValidator, final RankSystem rankSystem) {
        setRankSystemDirect(rankSystem);
        rankValidator.checkPersonRank(this);
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    private void setRankSystemDirect(final RankSystem rankSystem) {
        this.rankSystem = rankSystem;
    }

    public Rank getRank() {
        return getRankSystem().getRank(getRankNumeric());
    }

    public int getRankNumeric() {
        return rank;
    }

    public void setRank(final int rank) {
        this.rank = rank;
    }

    public int getRankLevel() {
        return rankLevel;
    }

    public void setRankLevel(final int rankLevel) {
        this.rankLevel = rankLevel;
    }

    public void changeRank(final Campaign campaign, final int rankNumeric, final int rankLevel,
                           final boolean report) {
        final int oldRankNumeric = getRankNumeric();
        final int oldRankLevel = getRankLevel();
        setRank(rankNumeric);
        setRankLevel(rankLevel);

        if (campaign.getCampaignOptions().getUseTimeInRank()) {
            if (getPrisonerStatus().isFree() && !getPrimaryRole().isDependent()) {
                setLastRankChangeDate(campaign.getLocalDate());
            } else {
                setLastRankChangeDate(null);
            }
        }

        campaign.personUpdated(this);

        if (report) {
            if ((rankNumeric > oldRankNumeric)
                    || ((rankNumeric == oldRankNumeric) && (rankLevel > oldRankLevel))) {
                ServiceLogger.promotedTo(this, campaign.getLocalDate());
            } else if ((rankNumeric < oldRankNumeric) || (rankLevel < oldRankLevel)) {
                ServiceLogger.demotedTo(this, campaign.getLocalDate());
            }
        }
    }

    public String getRankName() {
        final Profession profession = Profession.getProfessionFromPersonnelRole(getPrimaryRole());
        String rankName = getRank().getName(profession.getProfession(getRankSystem(), getRank()));

        // Manei Domini Additions
        if (getRankSystem().isUseManeiDomini()) {
            if (!getManeiDominiClass().isNone()) {
                rankName = getManeiDominiClass() + " " + rankName;
            }

            if (!getManeiDominiRank().isNone()) {
                rankName += " " + getManeiDominiRank();
            }
        }

        if (getRankSystem().isUseROMDesignation()) {
            rankName += ROMDesignation.getComStarBranchDesignation(this, campaign);
        }

        // Rank Level Modifications
        if (getRankLevel() > 0) {
            rankName += Utilities.getRomanNumeralsFromArabicNumber(rankLevel, true);
        }

        // Prisoner Status Modifications
        rankName = rankName.equalsIgnoreCase("None")
                ? getPrisonerStatus().getTitleExtension()
                : getPrisonerStatus().getTitleExtension() + rankName;

        // We have our name, return it
        return rankName.trim();
    }

    public ManeiDominiClass getManeiDominiClass() {
        return maneiDominiClass;
    }

    public void setManeiDominiClass(final ManeiDominiClass maneiDominiClass) {
        setManeiDominiClassDirect(maneiDominiClass);
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    private void setManeiDominiClassDirect(final ManeiDominiClass maneiDominiClass) {
        this.maneiDominiClass = maneiDominiClass;
    }

    public ManeiDominiRank getManeiDominiRank() {
        return maneiDominiRank;
    }

    public void setManeiDominiRank(final ManeiDominiRank maneiDominiRank) {
        setManeiDominiRankDirect(maneiDominiRank);
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    private void setManeiDominiRankDirect(final ManeiDominiRank maneiDominiRank) {
        this.maneiDominiRank = maneiDominiRank;
    }

    /**
     * Determines whether this person outranks another, taking into account the seniority rank for
     * ComStar and WoB ranks.
     *
     * @param other The <code>Person</code> to compare ranks with
     * @return      true if <code>other</code> has a lower rank, or if <code>other</code> is null.
     */
    public boolean outRanks(final @Nullable Person other) {
        if (other == null) {
            return true;
        } else if (getRankNumeric() == other.getRankNumeric()) {
            return getRankLevel() > other.getRankLevel();
        } else {
            return getRankNumeric() > other.getRankNumeric();
        }
    }
    //endregion Ranks

    public String getSkillSummary() {
        return SkillType.getExperienceLevelName(getExperienceLevel(false));
    }

    @Override
    public String toString() {
        return getFullName();
    }

    /**
     * Two people are determined to be equal if they have the same id
     * @param object the object to check if it is equal to the person or not
     * @return true if they have the same id, otherwise false
     */
    @Override
    public boolean equals(@Nullable Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof Person)) {
            return false;
        } else {
            return getId().equals(((Person) object).getId());
        }
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    public int getExperienceLevel(boolean secondary) {
        PersonnelRole role = secondary ? getSecondaryRole() : getPrimaryRole();
        switch (role) {
            case MECHWARRIOR:
                if (hasSkill(SkillType.S_GUN_MECH) && hasSkill(SkillType.S_PILOT_MECH)) {
                    /*
                     * Attempt to use higher precision averaging, but if it doesn't provide a clear result
                     * due to non-standard experience thresholds then fall back on lower precision averaging
                     * See Bug #140
                     */
                    if (campaign.getCampaignOptions().useAlternativeQualityAveraging()) {
                        int rawScore = (int) Math.floor(
                            (getSkill(SkillType.S_GUN_MECH).getLevel() + getSkill(SkillType.S_PILOT_MECH).getLevel()) / 2.0
                        );
                        if (getSkill(SkillType.S_GUN_MECH).getType().getExperienceLevel(rawScore) ==
                            getSkill(SkillType.S_PILOT_MECH).getType().getExperienceLevel(rawScore)) {
                            return getSkill(SkillType.S_GUN_MECH).getType().getExperienceLevel(rawScore);
                        }
                    }

                    return (int) Math.floor((getSkill(SkillType.S_GUN_MECH).getExperienceLevel()
                                             + getSkill(SkillType.S_PILOT_MECH).getExperienceLevel()) / 2.0);
                } else {
                    return -1;
                }
            case LAM_PILOT:
                if (Stream.of(SkillType.S_GUN_MECH, SkillType.S_PILOT_MECH,
                        SkillType.S_GUN_AERO, SkillType.S_PILOT_AERO).allMatch(this::hasSkill)) {
                    /*
                     * Attempt to use higher precision averaging, but if it doesn't provide a clear result
                     * due to non-standard experience thresholds then fall back on lower precision averaging
                     * See Bug #140
                     */
                    if (campaign.getCampaignOptions().useAlternativeQualityAveraging()) {
                        int rawScore = (int) Math.floor((Stream.of(SkillType.S_GUN_MECH, SkillType.S_PILOT_MECH,
                                SkillType.S_GUN_AERO, SkillType.S_PILOT_AERO).mapToInt(s -> getSkill(s).getLevel()).sum())
                                / 4.0);

                        final int mechGunneryExperienceLevel = SkillType.lookupHash.get(SkillType.S_GUN_MECH).getExperienceLevel(rawScore);
                        if ((mechGunneryExperienceLevel == SkillType.lookupHash.get(SkillType.S_PILOT_MECH).getExperienceLevel(rawScore)
                                && (mechGunneryExperienceLevel == SkillType.lookupHash.get(SkillType.S_GUN_AERO).getExperienceLevel(rawScore))
                                && (mechGunneryExperienceLevel == SkillType.lookupHash.get(SkillType.S_PILOT_AERO).getExperienceLevel(rawScore)))) {
                            return getSkill(SkillType.S_GUN_MECH).getType().getExperienceLevel(rawScore);
                        }
                    }

                    return (int) Math.floor(
                            (getSkill(SkillType.S_GUN_MECH).getExperienceLevel() + getSkill(SkillType.S_PILOT_MECH).getExperienceLevel()
                            + getSkill(SkillType.S_GUN_AERO).getExperienceLevel() + getSkill(SkillType.S_PILOT_AERO).getExperienceLevel())
                            / 4.0);
                } else {
                    return -1;
                }
            case GROUND_VEHICLE_DRIVER:
                return hasSkill(SkillType.S_PILOT_GVEE) ? getSkill(SkillType.S_PILOT_GVEE).getExperienceLevel() : -1;
            case NAVAL_VEHICLE_DRIVER:
                return hasSkill(SkillType.S_PILOT_NVEE) ? getSkill(SkillType.S_PILOT_NVEE).getExperienceLevel() : -1;
            case VTOL_PILOT:
                return hasSkill(SkillType.S_PILOT_VTOL) ? getSkill(SkillType.S_PILOT_VTOL).getExperienceLevel() : -1;
            case VEHICLE_GUNNER:
                return hasSkill(SkillType.S_GUN_VEE) ? getSkill(SkillType.S_GUN_VEE).getExperienceLevel() : -1;
            case VEHICLE_CREW:
                return hasSkill(SkillType.S_TECH_MECHANIC) ? getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel() : -1;
            case AEROSPACE_PILOT:
                if (hasSkill(SkillType.S_GUN_AERO) && hasSkill(SkillType.S_PILOT_AERO)) {
                    if (campaign.getCampaignOptions().useAlternativeQualityAveraging()) {
                        int rawScore = (int) Math.floor(
                            (getSkill(SkillType.S_GUN_AERO).getLevel() + getSkill(SkillType.S_PILOT_AERO)
                                    .getLevel()) / 2.0
                        );
                        if (getSkill(SkillType.S_GUN_AERO).getType().getExperienceLevel(rawScore) ==
                            getSkill(SkillType.S_PILOT_AERO).getType().getExperienceLevel(rawScore)) {
                            return getSkill(SkillType.S_GUN_AERO).getType().getExperienceLevel(rawScore);
                        }
                    }

                    return (int) Math.floor((getSkill(SkillType.S_GUN_AERO).getExperienceLevel()
                                                 + getSkill(SkillType.S_PILOT_AERO).getExperienceLevel()) / 2.0);
                } else {
                    return -1;
                }
            case CONVENTIONAL_AIRCRAFT_PILOT:
                if (hasSkill(SkillType.S_GUN_JET) && hasSkill(SkillType.S_PILOT_JET)) {
                    if (campaign.getCampaignOptions().useAlternativeQualityAveraging()) {
                        int rawScore = (int) Math.floor(
                            (getSkill(SkillType.S_GUN_JET).getLevel() + getSkill(SkillType.S_PILOT_JET)
                                    .getLevel()) / 2.0
                        );
                        if (getSkill(SkillType.S_GUN_JET).getType().getExperienceLevel(rawScore) ==
                            getSkill(SkillType.S_PILOT_JET).getType().getExperienceLevel(rawScore)) {
                            return getSkill(SkillType.S_GUN_JET).getType().getExperienceLevel(rawScore);
                        }
                    }

                    return (int) Math.floor((getSkill(SkillType.S_GUN_JET).getExperienceLevel()
                                             + getSkill(SkillType.S_PILOT_JET).getExperienceLevel()) / 2.0);
                } else {
                    return -1;
                }
            case PROTOMECH_PILOT:
                return hasSkill(SkillType.S_GUN_PROTO) ? getSkill(SkillType.S_GUN_PROTO).getExperienceLevel() : -1;
            case BATTLE_ARMOUR:
                if (hasSkill(SkillType.S_GUN_BA) && hasSkill(SkillType.S_ANTI_MECH)) {
                    if (campaign.getCampaignOptions().useAlternativeQualityAveraging()) {
                        int rawScore = (int) Math.floor(
                            (getSkill(SkillType.S_GUN_BA).getLevel() + getSkill(SkillType.S_ANTI_MECH)
                                    .getLevel()) / 2.0
                        );
                        if (getSkill(SkillType.S_GUN_BA).getType().getExperienceLevel(rawScore) ==
                            getSkill(SkillType.S_ANTI_MECH).getType().getExperienceLevel(rawScore)) {
                            return getSkill(SkillType.S_GUN_BA).getType().getExperienceLevel(rawScore);
                        }
                    }

                    return (int) Math.floor((getSkill(SkillType.S_GUN_BA).getExperienceLevel()
                                             + getSkill(SkillType.S_ANTI_MECH).getExperienceLevel()) / 2.0);
                } else {
                    return -1;
                }
            case SOLDIER:
                return hasSkill(SkillType.S_SMALL_ARMS) ? getSkill(SkillType.S_SMALL_ARMS).getExperienceLevel() : -1;
            case VESSEL_PILOT:
                return hasSkill(SkillType.S_PILOT_SPACE) ? getSkill(SkillType.S_PILOT_SPACE).getExperienceLevel() : -1;
            case VESSEL_GUNNER:
                return hasSkill(SkillType.S_GUN_SPACE) ? getSkill(SkillType.S_GUN_SPACE).getExperienceLevel() : -1;
            case VESSEL_CREW:
                return hasSkill(SkillType.S_TECH_VESSEL) ? getSkill(SkillType.S_TECH_VESSEL).getExperienceLevel() : -1;
            case VESSEL_NAVIGATOR:
                return hasSkill(SkillType.S_NAV) ? getSkill(SkillType.S_NAV).getExperienceLevel() : -1;
            case MECH_TECH:
                return hasSkill(SkillType.S_TECH_MECH) ? getSkill(SkillType.S_TECH_MECH).getExperienceLevel() : -1;
            case MECHANIC:
                return hasSkill(SkillType.S_TECH_MECHANIC) ? getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel() : -1;
            case AERO_TECH:
                return hasSkill(SkillType.S_TECH_AERO) ? getSkill(SkillType.S_TECH_AERO).getExperienceLevel() : -1;
            case BA_TECH:
                return hasSkill(SkillType.S_TECH_BA) ? getSkill(SkillType.S_TECH_BA).getExperienceLevel() : -1;
            case ASTECH:
                return hasSkill(SkillType.S_ASTECH) ? getSkill(SkillType.S_ASTECH).getExperienceLevel() : -1;
            case DOCTOR:
                return hasSkill(SkillType.S_DOCTOR) ? getSkill(SkillType.S_DOCTOR).getExperienceLevel() : -1;
            case MEDIC:
                return hasSkill(SkillType.S_MEDTECH) ? getSkill(SkillType.S_MEDTECH).getExperienceLevel() : -1;
            case ADMINISTRATOR_COMMAND:
            case ADMINISTRATOR_LOGISTICS:
            case ADMINISTRATOR_TRANSPORT:
            case ADMINISTRATOR_HR:
                return hasSkill(SkillType.S_ADMIN) ? getSkill(SkillType.S_ADMIN).getExperienceLevel() : -1;
            case DEPENDENT:
            case NONE:
            default:
                return -1;
        }
    }

    /**
     * returns a full description in HTML format that will be used for the graphical display in the
     * personnel table among other places
     * @return String
     */
    public String getFullDesc() {
        return "<b>" + getFullTitle() + "</b><br/>" + getSkillSummary() + " " + getRoleDesc();
    }

    public String getFullTitle() {
        String rank = getRankName();

        if (!rank.isBlank()) {
            rank = rank + " ";
        }

        return rank + getFullName();
    }

    public String makeHTMLRank() {
        return String.format("<html>%s</html>", makeHTMLRankDiv());
    }

    public String makeHTMLRankDiv() {
        return String.format("<div id=\"%s\">%s</div>", getId(), getRankName().trim());
    }

    public String getHyperlinkedFullTitle() {
        return String.format("<a href='PERSON:%s'>%s</a>", getId(), getFullTitle());
    }

    /**
     * @return the primaryDesignator
     */
    public ROMDesignation getPrimaryDesignator() {
        return primaryDesignator;
    }

    /**
     * @param primaryDesignator the primaryDesignator to set
     */
    public void setPrimaryDesignator(ROMDesignation primaryDesignator) {
        this.primaryDesignator = primaryDesignator;
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    /**
     * @return the secondaryDesignator
     */
    public ROMDesignation getSecondaryDesignator() {
        return secondaryDesignator;
    }

    /**
     * @param secondaryDesignator the secondaryDesignator to set
     */
    public void setSecondaryDesignator(ROMDesignation secondaryDesignator) {
        this.secondaryDesignator = secondaryDesignator;
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    public int getHealingDifficulty() {
        if (campaign.getCampaignOptions().useTougherHealing()) {
            return Math.max(0, getHits() - 2);
        }
        return 0;
    }

    public TargetRoll getHealingMods() {
        return new TargetRoll(getHealingDifficulty(), "difficulty");
    }

    public String fail() {
        return " <font color='red'><b>Failed to heal.</b></font>";
    }

    //region skill
    public boolean hasSkill(String skillName) {
        return skills.hasSkill(skillName);
    }

    public Skills getSkills() {
        return skills;
    }

    @Nullable
    public Skill getSkill(String skillName) {
        return skills.getSkill(skillName);
    }

    public void addSkill(String skillName, Skill skill) {
        skills.addSkill(skillName, skill);
    }

    public void addSkill(String skillName, int level, int bonus) {
        skills.addSkill(skillName, new Skill(skillName, level, bonus));
    }

    public void removeSkill(String skillName) {
        skills.removeSkill(skillName);
    }

    public int getSkillNumber() {
        return skills.size();
    }

    /**
     * Remove all skills
     */
    public void removeAllSkills() {
        skills.clear();
    }

    /**
     * Limit skills to the maximum of the given level
     */
    public void limitSkills(int maxLvl) {
        for (Skill skill : skills.getSkills()) {
            if (skill.getLevel() > maxLvl) {
                skill.setLevel(maxLvl);
            }
        }
    }

    public void improveSkill(String skillName) {
        if (hasSkill(skillName)) {
            getSkill(skillName).improve();
        } else {
            addSkill(skillName, 0, 0);
        }
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    public int getCostToImprove(String skillName) {
        if (hasSkill(skillName)) {
            return getSkill(skillName).getCostToImprove();
        } else {
            return -1;
        }
    }
    //endregion skill

    //region Awards
    public PersonAwardController getAwardController() {
        return awardController;
    }
    //endregion Awards

    public int getHits() {
        return hits;
    }

    public void setHits(int h) {
        this.hits = h;
    }

    /**
      * @return <tt>true</tt> if the location (or any of its parent locations) has an injury
      * which implies that the location (most likely a limb) is severed.
      */
    public boolean isLocationMissing(BodyLocation loc) {
        if (null == loc) {
            return false;
        }
        for (Injury i : getInjuriesByLocation(loc)) {
            if (i.getType().impliesMissingLocation(loc)) {
                return true;
            }
        }
        // Check parent locations as well (a hand can be missing if the corresponding arm is)
        return isLocationMissing(loc.Parent());
    }

    public void heal() {
        hits = Math.max(hits - 1, 0);
        if (!needsFixing()) {
            doctorId = null;
        }
    }

    public boolean needsFixing() {
        return ((hits > 0) || needsAMFixing()) && getStatus().isActive();
    }

    public String succeed() {
        heal();
        return " <font color='green'><b>Successfully healed one hit.</b></font>";
    }

    //region Personnel Options
    public PersonnelOptions getOptions() {
        return options;
    }

    /**
     * Returns the options of the given category that this pilot has
     */
    public Enumeration<IOption> getOptions(String grpKey) {
        return options.getOptions(grpKey);
    }

    public int countOptions(String grpKey) {
        int count = 0;

        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(grpKey)) {
                continue;
            }

            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();

                if (option.booleanValue()) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Returns a string of all the option "codes" for this pilot, for a given group, using sep as the separator
     */
    public String getOptionList(String sep, String grpKey) {
        StringBuilder adv = new StringBuilder();

        if (null == sep) {
            sep = "";
        }

        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            if (!group.getKey().equalsIgnoreCase(grpKey)) {
                continue;
            }
            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();

                if (option.booleanValue()) {
                    if (adv.length() > 0) {
                        adv.append(sep);
                    }

                    adv.append(option.getName());
                    if ((option.getType() == IOption.STRING) || (option.getType() == IOption.CHOICE) || (option.getType() == IOption.INTEGER)) {
                        adv.append(" ").append(option.stringValue());
                    }
                }
            }
        }

        return adv.toString();
    }

    /**
     * @return an html-coded list that says what abilities are enabled for this pilot
     */
    public String getAbilityListAsString(String type) {
        StringBuilder abilityString = new StringBuilder();
        for (Enumeration<IOption> i = getOptions(type); i.hasMoreElements(); ) {
            IOption ability = i.nextElement();
            if (ability.booleanValue()) {
                abilityString.append(Utilities.getOptionDisplayName(ability)).append("<br>");
            }
        }
        if (abilityString.length() == 0) {
            return null;
        }
        return "<html>" + abilityString + "</html>";
    }
    //endregion Personnel Options

    //region edge
    public int getEdge() {
        return getOptions().intOption("edge");
    }

    public void setEdge(int e) {
        for (Enumeration<IOption> i = getOptions(PilotOptions.EDGE_ADVANTAGES); i.hasMoreElements(); ) {
            IOption ability = i.nextElement();
            if (ability.getName().equals("edge")) {
                ability.setValue(e);
            }
        }
    }

    public void changeEdge(int amount) {
        setEdge(Math.max(getEdge() + amount, 0));
    }

    /**
     * Resets support personnel edge points to the purchased level. Used for weekly refresh.
     */
    public void resetCurrentEdge() {
        setCurrentEdge(getEdge());
    }

    /**
     * Sets support personnel edge points to the value 'e'. Used for weekly refresh.
     * @param e - integer used to track this person's edge points available for the current week
     */
    public void setCurrentEdge(int e) {
        currentEdge = e;
    }

    public void changeCurrentEdge(int amount) {
        currentEdge = Math.max(currentEdge + amount, 0);
    }

    /**
     *  Returns this person's currently available edge points. Used for weekly refresh.
     */
    public int getCurrentEdge() {
        return currentEdge;
    }

    public void setEdgeUsed(int e) {
        edgeUsedThisRound = e;
    }

    public int getEdgeUsed() {
        return edgeUsedThisRound;
    }

    /**
     * This will set a specific edge trigger, regardless of the current status
     */
    public void setEdgeTrigger(String name, boolean status) {
        for (Enumeration<IOption> i = getOptions(PilotOptions.EDGE_ADVANTAGES); i.hasMoreElements(); ) {
            IOption ability = i.nextElement();
            if (ability.getName().equals(name)) {
                ability.setValue(status);
            }
        }
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    /**
     * This will flip the boolean status of the current edge trigger
     *
     * @param name of the trigger condition
     */
    public void changeEdgeTrigger(String name) {
        for (Enumeration<IOption> i = getOptions(PilotOptions.EDGE_ADVANTAGES); i.hasMoreElements(); ) {
            IOption ability = i.nextElement();
            if (ability.getName().equals(name)) {
                ability.setValue(!ability.booleanValue());
            }
        }
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    /**
     *
     * @return an html-coded tooltip that says what edge will be used
     */
    public String getEdgeTooltip() {
        StringBuilder edgett = new StringBuilder();
        for (Enumeration<IOption> i = getOptions(PilotOptions.EDGE_ADVANTAGES); i.hasMoreElements(); ) {
            IOption ability = i.nextElement();
            //yuck, it would be nice to have a more fool-proof way of identifying edge triggers
            if (ability.getName().contains("edge_when") && ability.booleanValue()) {
                edgett.append(ability.getDescription()).append("<br>");
            }
        }
        if (edgett.toString().equals("")) {
            return "No triggers set";
        }
        return "<html>" + edgett + "</html>";
    }
    //endregion edge

    public boolean canDrive(Entity ent) {
        if (ent instanceof LandAirMech) {
            return hasSkill(SkillType.S_PILOT_MECH) && hasSkill(SkillType.S_PILOT_AERO);
        } else if (ent instanceof Mech) {
            return hasSkill(SkillType.S_PILOT_MECH);
        } else if (ent instanceof VTOL) {
            return hasSkill(SkillType.S_PILOT_VTOL);
        } else if (ent instanceof Tank) {
            if (ent.getMovementMode() == EntityMovementMode.NAVAL
                || ent.getMovementMode() == EntityMovementMode.HYDROFOIL
                || ent.getMovementMode() == EntityMovementMode.SUBMARINE) {
                return hasSkill(SkillType.S_PILOT_NVEE);
            } else {
                return hasSkill(SkillType.S_PILOT_GVEE);
            }
        } else if (ent instanceof ConvFighter) {
            return hasSkill(SkillType.S_PILOT_JET) || hasSkill(SkillType.S_PILOT_AERO);
        } else if (ent instanceof SmallCraft || ent instanceof Jumpship) {
            return hasSkill(SkillType.S_PILOT_SPACE);
        } else if (ent instanceof Aero) {
            return hasSkill(SkillType.S_PILOT_AERO);
        } else if (ent instanceof BattleArmor) {
            return hasSkill(SkillType.S_GUN_BA);
        } else if (ent instanceof Infantry) {
            return hasSkill(SkillType.S_SMALL_ARMS);
        } else if (ent instanceof Protomech) {
            return hasSkill(SkillType.S_GUN_PROTO);
        }
        return false;
    }

    public boolean canGun(Entity ent) {
        if (ent instanceof LandAirMech) {
            return hasSkill(SkillType.S_GUN_MECH) && hasSkill(SkillType.S_GUN_AERO);
        } else if (ent instanceof Mech) {
            return hasSkill(SkillType.S_GUN_MECH);
        } else if (ent instanceof Tank) {
            return hasSkill(SkillType.S_GUN_VEE);
        } else if (ent instanceof ConvFighter) {
            return hasSkill(SkillType.S_GUN_JET) || hasSkill(SkillType.S_GUN_AERO);
        } else if (ent instanceof SmallCraft || ent instanceof Jumpship) {
            return hasSkill(SkillType.S_GUN_SPACE);
        } else if (ent instanceof Aero) {
            return hasSkill(SkillType.S_GUN_AERO);
        } else if (ent instanceof BattleArmor) {
            return hasSkill(SkillType.S_GUN_BA);
        } else if (ent instanceof Infantry) {
            return hasSkill(SkillType.S_SMALL_ARMS);
        } else if (ent instanceof Protomech) {
            return hasSkill(SkillType.S_GUN_PROTO);
        }
        return false;
    }

    public boolean canTech(Entity ent) {
        if (ent instanceof Mech || ent instanceof Protomech) {
            return hasSkill(SkillType.S_TECH_MECH);
        } else if (ent instanceof Aero) {
            return hasSkill(SkillType.S_TECH_AERO);
        } else if (ent instanceof BattleArmor) {
            return hasSkill(SkillType.S_TECH_BA);
        } else if (ent instanceof Tank) {
            return hasSkill(SkillType.S_TECH_MECHANIC);
        }
        return false;
    }

    /**
     * @return the person's current daily available tech time. This does NOT account for any expended
     * time.
     */
    public int getDailyAvailableTechTime() {
        return (getPrimaryRole().isTech() ? PRIMARY_ROLE_SUPPORT_TIME : SECONDARY_ROLE_SUPPORT_TIME)
                - getMaintenanceTimeUsing();
    }

    public int getMaintenanceTimeUsing() {
        int time = 0;
        for (Unit u : getTechUnits()) {
            time += u.getMaintenanceTime();
        }
        return time;
    }

    public boolean isMothballing() {
        return isTech() && techUnits.stream().anyMatch(Unit::isMothballing);
    }

    public @Nullable Unit getUnit() {
        return unit;
    }

    public void setUnit(@Nullable Unit unit) {
        this.unit = unit;
    }

    public void removeTechUnit(Unit unit) {
        techUnits.remove(unit);
    }

    public void addTechUnit(Unit unit) {
        Objects.requireNonNull(unit);

        if (!techUnits.contains(unit)) {
            techUnits.add(unit);
        }
    }

    public void clearTechUnits() {
        techUnits.clear();
    }

    public List<Unit> getTechUnits() {
        return Collections.unmodifiableList(techUnits);
    }

    public void removeAllTechJobs(final Campaign campaign) {
        campaign.getHangar().forEachUnit(u -> {
            if (equals(u.getTech())) {
                u.remove(this, true);
            }

            if ((u.getRefit() != null) && equals(u.getRefit().getTech())) {
                u.getRefit().setTech(null);
            }
        });

        for (final Part part : campaign.getWarehouse().getParts()) {
            if (equals(part.getTech())) {
                part.cancelAssignment();
            }
        }

        for (final Force force : campaign.getAllForces()) {
            if (getId().equals(force.getTechID())) {
                force.setTechID(null);
            }
        }
    }

    public int getMinutesLeft() {
        return minutesLeft;
    }

    public void setMinutesLeft(int m) {
        this.minutesLeft = m;
        if (engineer && (null != getUnit())) {
            //set minutes for all crewmembers
            for (Person p : getUnit().getActiveCrew()) {
                p.setMinutesLeft(m);
            }
        }
    }

    public int getOvertimeLeft() {
        return overtimeLeft;
    }

    public void setOvertimeLeft(int m) {
        this.overtimeLeft = m;
        if (engineer && (null != getUnit())) {
            //set minutes for all crewmembers
            for (Person p : getUnit().getActiveCrew()) {
                p.setMinutesLeft(m);
            }
        }
    }

    public void resetMinutesLeft() {
        if (getPrimaryRole().isTech() || getPrimaryRole().isDoctor()) {
            this.minutesLeft = PRIMARY_ROLE_SUPPORT_TIME;
            this.overtimeLeft = PRIMARY_ROLE_OVERTIME_SUPPORT_TIME;
        } else if (getSecondaryRole().isTechSecondary() || getSecondaryRole().isDoctor()) {
            this.minutesLeft = SECONDARY_ROLE_SUPPORT_TIME;
            this.overtimeLeft = SECONDARY_ROLE_OVERTIME_SUPPORT_TIME;
        }
    }

    public Skill getBestTechSkill() {
        Skill skill = null;
        int lvl = -1;
        if (hasSkill(SkillType.S_TECH_MECH) && getSkill(SkillType.S_TECH_MECH).getExperienceLevel() > lvl) {
            skill = getSkill(SkillType.S_TECH_MECH);
            lvl = getSkill(SkillType.S_TECH_MECH).getExperienceLevel();
        }
        if (hasSkill(SkillType.S_TECH_AERO) && getSkill(SkillType.S_TECH_AERO).getExperienceLevel() > lvl) {
            skill = getSkill(SkillType.S_TECH_AERO);
            lvl = getSkill(SkillType.S_TECH_AERO).getExperienceLevel();
        }
        if (hasSkill(SkillType.S_TECH_MECHANIC) && getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel() > lvl) {
            skill = getSkill(SkillType.S_TECH_MECHANIC);
            lvl = getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel();
        }
        if (hasSkill(SkillType.S_TECH_BA) && getSkill(SkillType.S_TECH_BA).getExperienceLevel() > lvl) {
            skill = getSkill(SkillType.S_TECH_BA);
        }
        return skill;
    }

    public boolean isTech() {
        //type must be correct and you must be more than ultra-green in the skill
        boolean isMechTech = hasSkill(SkillType.S_TECH_MECH) && getSkill(SkillType.S_TECH_MECH).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
        boolean isAeroTech = hasSkill(SkillType.S_TECH_AERO) && getSkill(SkillType.S_TECH_AERO).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
        boolean isMechanic = hasSkill(SkillType.S_TECH_MECHANIC) && getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
        boolean isBATech = hasSkill(SkillType.S_TECH_BA) && getSkill(SkillType.S_TECH_BA).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN;
        // At some point we may want to re-write things to include this
        /*boolean isEngineer = hasSkill(SkillType.S_TECH_VESSEL) && getSkill(SkillType.S_TECH_VESSEL).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN
                && campaign.getUnit(getUnitId()).getEngineer() != null
                && campaign.getUnit(getUnitId()).getEngineer().equals(this);*/
        return (getPrimaryRole().isTech() || getSecondaryRole().isTechSecondary()) && (isMechTech || isAeroTech || isMechanic || isBATech);
    }

    public boolean isAdministrator() {
        return (getPrimaryRole().isAdministrator() || getSecondaryRole().isAdministrator());
    }

    public boolean isDoctor() {
        return hasSkill(SkillType.S_DOCTOR) && (getPrimaryRole().isDoctor() || getSecondaryRole().isDoctor());
    }

    public boolean isTaskOvertime(IPartWork partWork) {
        return partWork.getTimeLeft() > getMinutesLeft()
               && (partWork.getTimeLeft() - getMinutesLeft()) <= getOvertimeLeft();
    }

    public Skill getSkillForWorkingOn(IPartWork part) {
        Unit unit = part.getUnit();
        Skill skill = getSkillForWorkingOn(unit);
        if (null != skill) {
            return skill;
        }
        //check spare parts
        //return the best one
        if (part.isRightTechType(SkillType.S_TECH_MECH) && hasSkill(SkillType.S_TECH_MECH)) {
            skill = getSkill(SkillType.S_TECH_MECH);
        }
        if (part.isRightTechType(SkillType.S_TECH_BA) && hasSkill(SkillType.S_TECH_BA)) {
            if (null == skill || skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_BA).getFinalSkillValue()) {
                skill = getSkill(SkillType.S_TECH_BA);
            }
        }
        if (part.isRightTechType(SkillType.S_TECH_AERO) && hasSkill(SkillType.S_TECH_AERO)) {
            if (null == skill || skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_AERO).getFinalSkillValue()) {
                skill = getSkill(SkillType.S_TECH_AERO);
            }
        }
        if (part.isRightTechType(SkillType.S_TECH_MECHANIC) && hasSkill(SkillType.S_TECH_MECHANIC)) {
            if (null == skill || skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_MECHANIC).getFinalSkillValue()) {
                skill = getSkill(SkillType.S_TECH_MECHANIC);
            }
        }
        if (part.isRightTechType(SkillType.S_TECH_VESSEL) && hasSkill(SkillType.S_TECH_VESSEL)) {
            if (null == skill || skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_VESSEL).getFinalSkillValue()) {
                skill = getSkill(SkillType.S_TECH_VESSEL);
            }
        }
        if (null != skill) {
            return skill;
        }
        //if we are still here then we didn't have the right tech skill, so return the highest
        //of any tech skills that we do have
        if (hasSkill(SkillType.S_TECH_MECH)) {
            skill = getSkill(SkillType.S_TECH_MECH);
        }
        if (hasSkill(SkillType.S_TECH_BA)) {
            if (null == skill || skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_BA).getFinalSkillValue()) {
                skill = getSkill(SkillType.S_TECH_BA);
            }
        }
        if (hasSkill(SkillType.S_TECH_MECHANIC)) {
            if (null == skill || skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_MECHANIC).getFinalSkillValue()) {
                skill = getSkill(SkillType.S_TECH_MECHANIC);
            }
        }
        if (hasSkill(SkillType.S_TECH_AERO)) {
            if (null == skill || skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_AERO).getFinalSkillValue()) {
                skill = getSkill(SkillType.S_TECH_AERO);
            }
        }
        return skill;
    }

    public Skill getSkillForWorkingOn(Unit unit) {
        if (unit == null) {
            return null;
        }
        if ((unit.getEntity() instanceof Mech || unit.getEntity() instanceof Protomech)
            && hasSkill(SkillType.S_TECH_MECH)) {
            return getSkill(SkillType.S_TECH_MECH);
        }
        if (unit.getEntity() instanceof BattleArmor && hasSkill(SkillType.S_TECH_BA)) {
            return getSkill(SkillType.S_TECH_BA);
        }
        if (unit.getEntity() instanceof Tank && hasSkill(SkillType.S_TECH_MECHANIC)) {
            return getSkill(SkillType.S_TECH_MECHANIC);
        }
        if ((unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)
            && hasSkill(SkillType.S_TECH_VESSEL)) {
            return getSkill(SkillType.S_TECH_VESSEL);
        }
        if (unit.getEntity() instanceof Aero
            && !(unit.getEntity() instanceof Dropship)
            && !(unit.getEntity() instanceof Jumpship)
            && hasSkill(SkillType.S_TECH_AERO)) {
            return getSkill(SkillType.S_TECH_AERO);
        }
        return null;
    }

    public Skill getSkillForWorkingOn(String skillName) {
        if (skillName.equals(CampaignOptions.S_TECH)) {
            return getBestTechSkill();
        }
        if (hasSkill(skillName)) {
            return getSkill(skillName);
        }
        return null;
    }

    public int getBestTechLevel() {
        int lvl = -1;
        Skill mechSkill = getSkill(SkillType.S_TECH_MECH);
        Skill mechanicSkill = getSkill(SkillType.S_TECH_MECHANIC);
        Skill baSkill = getSkill(SkillType.S_TECH_BA);
        Skill aeroSkill = getSkill(SkillType.S_TECH_AERO);
        if (null != mechSkill && mechSkill.getLevel() > lvl) {
            lvl = mechSkill.getLevel();
        }
        if (null != mechanicSkill && mechanicSkill.getLevel() > lvl) {
            lvl = mechanicSkill.getLevel();
        }
        if (null != baSkill && baSkill.getLevel() > lvl) {
            lvl = baSkill.getLevel();
        }
        if (null != aeroSkill && aeroSkill.getLevel() > lvl) {
            lvl = aeroSkill.getLevel();
        }
        return lvl;
    }

    public boolean isRightTechTypeFor(IPartWork part) {
        Unit unit = part.getUnit();
        if (null == unit) {
            return (hasSkill(SkillType.S_TECH_MECH) && part.isRightTechType(SkillType.S_TECH_MECH))
                    || (hasSkill(SkillType.S_TECH_AERO) && part.isRightTechType(SkillType.S_TECH_AERO))
                    || (hasSkill(SkillType.S_TECH_MECHANIC) && part.isRightTechType(SkillType.S_TECH_MECHANIC))
                    || (hasSkill(SkillType.S_TECH_BA) && part.isRightTechType(SkillType.S_TECH_BA))
                    || (hasSkill(SkillType.S_TECH_VESSEL) && part.isRightTechType(SkillType.S_TECH_VESSEL));
        }
        if (unit.getEntity() instanceof Mech || unit.getEntity() instanceof Protomech) {
            return hasSkill(SkillType.S_TECH_MECH);
        }
        if (unit.getEntity() instanceof BattleArmor) {
            return hasSkill(SkillType.S_TECH_BA);
        }
        if (unit.getEntity() instanceof Tank || unit.getEntity() instanceof Infantry) {
            return hasSkill(SkillType.S_TECH_MECHANIC);
        }
        if (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship) {
            return hasSkill(SkillType.S_TECH_VESSEL);
        }
        if (unit.getEntity() instanceof Aero) {
            return hasSkill(SkillType.S_TECH_AERO);
        }
        return false;
    }

    public UUID getDoctorId() {
        return doctorId;
    }

    public int getToughness() {
        return toughness;
    }

    public void setToughness(int t) {
        toughness = t;
    }

    public void resetSkillTypes() {
        for (Skill s : skills.getSkills()) {
            s.updateType();
        }
    }

    public int getNTasks() {
        return nTasks;
    }

    public void setNTasks(int n) {
        nTasks = n;
    }

    public List<LogEntry> getPersonnelLog() {
        personnelLog.sort(Comparator.comparing(LogEntry::getDate));
        return personnelLog;
    }

    public List<LogEntry> getMissionLog() {
        missionLog.sort(Comparator.comparing(LogEntry::getDate));
        return missionLog;
    }

    public void addLogEntry(LogEntry entry) {
        personnelLog.add(entry);
    }

    public void addMissionLogEntry(LogEntry entry) {
        missionLog.add(entry);
    }

    //region injuries
    /**
     * All methods below are for the Advanced Medical option
     */

    public List<Injury> getInjuries() {
        return new ArrayList<>(injuries);
    }

    public void clearInjuries() {
        injuries.clear();

        // Clear the doctor if there is one
        doctorId = null;
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    public void removeInjury(Injury i) {
        injuries.remove(i);
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    public void diagnose(int hits) {
        InjuryUtil.resolveAfterCombat(campaign, this, hits);
        InjuryUtil.resolveCombatDamage(campaign, this, hits);
        setHits(0);
    }

    public int getAbilityTimeModifier() {
        int modifier = 100;
        if (campaign.getCampaignOptions().useToughness()) {
            if (getToughness() == 1) {
                modifier -= 10;
            }
            if (getToughness() > 1) {
                modifier -= 15;
            }
        } // TODO: Fully implement this for advanced healing
        if (getOptions().booleanOption("pain_resistance")) {
            modifier -= 15;
        } else if (getOptions().booleanOption("iron_man")) {
            modifier -= 10;
        }

        return modifier;
    }

    public boolean hasInjury(BodyLocation loc) {
        return (null != getInjuryByLocation(loc));
    }

    public boolean needsAMFixing() {
        boolean retVal = false;
        if (injuries.size() > 0) {
            for (Injury i : injuries) {
                if (i.getTime() > 0 || !(i.isPermanent())) {
                    retVal = true;
                    break;
                }
            }
        }
        return retVal;
    }

    public int getPilotingInjuryMod() {
        return Modifier.calcTotalModifier(injuries.stream().flatMap(i -> i.getModifiers().stream()), ModifierValue.PILOTING);
    }

    public int getGunneryInjuryMod() {
        return Modifier.calcTotalModifier(injuries.stream().flatMap(i -> i.getModifiers().stream()), ModifierValue.GUNNERY);
    }

    public boolean hasInjuries(boolean permCheck) {
        boolean tf = false;
        if (injuries.size() > 0) {
            if (permCheck) {
                for (Injury injury : injuries) {
                    if (!injury.isPermanent() || injury.getTime() > 0) {
                        tf = true;
                        break;
                    }
                }
            } else {
                tf = true;
            }
        }
        return tf;
    }

    public boolean hasOnlyHealedPermanentInjuries() {
        if (injuries.size() == 0) {
            return false;
        }
        for (Injury injury : injuries) {
            if (!injury.isPermanent() || injury.getTime() > 0) {
                return false;
            }
        }
        return true;
    }

    public List<Injury> getInjuriesByLocation(BodyLocation loc) {
        return injuries.stream()
            .filter((i) -> (i.getLocation() == loc)).collect(Collectors.toList());
    }

    // Returns only the first injury in a location
    public Injury getInjuryByLocation(BodyLocation loc) {
        return injuries.stream()
            .filter((i) -> (i.getLocation() == loc)).findFirst().orElse(null);
    }

    public void addInjury(Injury i) {
        injuries.add(Objects.requireNonNull(i));
        if (null != getUnit()) {
            getUnit().resetPilotAndEntity();
        }
    }
    //endregion injuries

    /* For use by Against the Bot retirement/defection rolls */
    public boolean isFounder() {
        return founder;
    }

    public void setFounder(boolean founder) {
        this.founder = founder;
    }

    public int getOriginalUnitWeight() {
        return originalUnitWeight;
    }

    public void setOriginalUnitWeight(int weight) {
        originalUnitWeight = weight;
    }

    public int getOriginalUnitTech() {
        return originalUnitTech;
    }

    public void setOriginalUnitTech(int tech) {
        originalUnitTech = tech;
    }

    public UUID getOriginalUnitId() {
        return originalUnitId;
    }

    public void setOriginalUnitId(UUID id) {
        originalUnitId = id;
    }

    public void setOriginalUnit(Unit unit) {
        originalUnitId = unit.getId();
        if (unit.getEntity().isClan()) {
            originalUnitTech = TECH_CLAN;
        } else if (unit.getEntity().getTechLevel() > TechConstants.T_INTRO_BOXSET) {
            originalUnitTech = TECH_IS2;
        } else {
            originalUnitTech = TECH_IS1;
        }
        originalUnitWeight = unit.getEntity().getWeightClass();
    }

    /**
     * This is used to get the number of shares the person has
     * @param sharesForAll true if all combat and support personnel have shares, otherwise false if
     *                     just MechWarriors have shares
     * @return the number of shares the person has
     */
    public int getNumShares(boolean sharesForAll) {
        if (!getStatus().isActive() || !getPrisonerStatus().isFree()
                || (!sharesForAll && !hasRole(PersonnelRole.MECHWARRIOR))) {
            return 0;
        }
        int shares = 1;
        if (isFounder()) {
            shares++;
        }
        shares += Math.max(-1, getExperienceLevel(false) - 2);

        if (getRank().isOfficer()) {
            final Profession profession = Profession.getProfessionFromPersonnelRole(getPrimaryRole());
            int rankOrder = getRankSystem().getOfficerCut();
            while ((rankOrder <= getRankNumeric()) && (rankOrder < Rank.RC_NUM)) {
                Rank rank = getRankSystem().getRanks().get(rankOrder);
                if (!rank.isEmpty(profession)) {
                    shares++;
                }
                rankOrder++;
            }
        }
        if (getOriginalUnitWeight() >= 1) {
            shares++;
        }
        if (getOriginalUnitWeight()  >= 3) {
            shares++;
        }
        shares += getOriginalUnitTech();

        return shares;
    }

    public boolean isEngineer() {
        return engineer;
    }

    public void setEngineer(boolean b) {
        engineer = b;
    }

    /**
     *
     * @return the ransom value of this individual
     * Useful for prisoner who you want to ransom or hand off to your employer in an AtB context
     */
    public Money getRansomValue() {
        // MechWarriors and aero pilots are worth more than the other types of scrubs
        return (getPrimaryRole().isMechWarriorGrouping() || getPrimaryRole().isAerospacePilot()
                ? MECHWARRIOR_AERO_RANSOM_VALUES : OTHER_RANSOM_VALUES)
                .get(getExperienceLevel(false));
    }

    public static class PersonUnitRef extends Unit {
        private PersonUnitRef(UUID id) {
            setId(id);
        }
    }

    public void fixReferences(Campaign campaign) {
        if (unit instanceof PersonUnitRef) {
            UUID id = unit.getId();
            unit = campaign.getUnit(id);
            if (unit == null) {
                MekHQ.getLogger().error(
                    String.format("Person %s ('%s') references missing unit %s",
                        getId(), getFullName(), id));
            }
        }
        for (int ii = techUnits.size() - 1; ii >= 0; --ii) {
            Unit techUnit = techUnits.get(ii);
            if (techUnit instanceof PersonUnitRef) {
                Unit realUnit = campaign.getUnit(techUnit.getId());
                if (realUnit != null) {
                    techUnits.set(ii, realUnit);
                } else {
                    MekHQ.getLogger().error(
                        String.format("Person %s ('%s') techs missing unit %s",
                            getId(), getFullName(), techUnit.getId()));
                    techUnits.remove(ii);
                }
            }
        }
    }
}
