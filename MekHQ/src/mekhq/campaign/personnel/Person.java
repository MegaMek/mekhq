/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (c) 2020-2023 - The MegaMek Team. All Rights Reserved.
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

import megamek.Version;
import megamek.client.generator.RandomNameGenerator;
import megamek.codeUtilities.StringUtility;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Portrait;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import mekhq.MekHQ;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.utilities.MHQXMLUtility;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.ExtraData;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.event.PersonStatusChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.io.CampaignXmlParser;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.LogEntryFactory;
import mekhq.campaign.log.ServiceLogger;
import mekhq.campaign.mod.am.InjuryUtil;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.enums.*;
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
import mekhq.io.migration.FactionMigrator;
import mekhq.io.migration.PersonMigrator;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 * @author Justin "Windchild" Bowen
 */
public class Person {
    //region Variable Declarations
    private static final Map<Integer, Money> MECHWARRIOR_AERO_RANSOM_VALUES;
    private static final Map<Integer, Money> OTHER_RANSOM_VALUES;

    private PersonAwardController awardController;

    //region Family Variables
    // Lineage
    private final Genealogy genealogy;

    //region Procreation
    private LocalDate dueDate;
    private LocalDate expectedDueDate;
    //endregion Procreation
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
    private Portrait portrait;

    private PersonnelRole primaryRole;
    private PersonnelRole secondaryRole;

    private ROMDesignation primaryDesignator;
    private ROMDesignation secondaryDesignator;

    private String biography;
    private LocalDate birthday;
    private LocalDate recruitment;
    private LocalDate lastRankChangeDate;
    private LocalDate retirement;
    private LocalDate dateOfDeath;
    private List<LogEntry> personnelLog;
    private List<LogEntry> scenarioLog;

    private Skills skills;
    private PersonnelOptions options;
    private int toughness;

    private PersonnelStatus status;
    private int xp;
    private int totalXPEarnings;
    private int acquisitions;
    private Money salary;
    private Money totalEarnings;
    private int hits;
    private PrisonerStatus prisonerStatus;

    // Supports edge usage by a ship's engineer composite crewman
    private int edgeUsedThisRound;
    // To track how many edge points support personnel have left until next refresh
    private int currentEdge;

    // phenotype and background
    private Phenotype phenotype;
    private String bloodname;
    private Faction originFaction;
    private Planet originPlanet;

    // assignments
    private Unit unit;
    private UUID doctorId;
    private List<Unit> techUnits;

    // days of rest
    private int idleMonths;
    private int daysToWaitForHealing;

    // Our rank
    private RankSystem rankSystem;
    private int rank;
    private int rankLevel;

    private ManeiDominiClass maneiDominiClass;
    private ManeiDominiRank maneiDominiRank;

    // stuff to track for support teams
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
    private int originalUnitWeight; // uses EntityWeightClass with 0 (Extra-Light) for no original unit
    public static final int TECH_IS1 = 0;
    public static final int TECH_IS2 = 1;
    public static final int TECH_CLAN = 2;
    private int originalUnitTech;
    private UUID originalUnitId;
    //endregion Against the Bot

    //region Flags
    private boolean clanPersonnel;
    private boolean commander;
    private boolean divorceable;
    private boolean founder; // +1 share if using shares system
    private boolean immortal;
    // this is a flag used in determine whether a person is a potential marriage candidate provided
    // that they are not married, are old enough, etc.
    private boolean marriageable;
    // this is a flag used in random procreation to determine whether to attempt to procreate
    private boolean tryingToConceive;
    //endregion Flags

    // Generic extra data, for use with plugins and mods
    private ExtraData extraData;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
            MekHQ.getMHQOptions().getLocale());

    // initializes the AtB ransom values
    static {
        MECHWARRIOR_AERO_RANSOM_VALUES = new HashMap<>();
        MECHWARRIOR_AERO_RANSOM_VALUES.put(SkillType.EXP_NONE, Money.of(0)); // no official AtB rules for really inexperienced scrubs, but...
        MECHWARRIOR_AERO_RANSOM_VALUES.put(SkillType.EXP_ULTRA_GREEN, Money.of(5000)); // no official AtB rules for really inexperienced scrubs, but...
        MECHWARRIOR_AERO_RANSOM_VALUES.put(SkillType.EXP_GREEN, Money.of(10000));
        MECHWARRIOR_AERO_RANSOM_VALUES.put(SkillType.EXP_REGULAR, Money.of(25000));
        MECHWARRIOR_AERO_RANSOM_VALUES.put(SkillType.EXP_VETERAN, Money.of(75000));
        MECHWARRIOR_AERO_RANSOM_VALUES.put(SkillType.EXP_ELITE, Money.of(150000));

        OTHER_RANSOM_VALUES = new HashMap<>();
        OTHER_RANSOM_VALUES.put(SkillType.EXP_NONE, Money.of(0));
        OTHER_RANSOM_VALUES.put(SkillType.EXP_ULTRA_GREEN, Money.of(2500));
        OTHER_RANSOM_VALUES.put(SkillType.EXP_GREEN, Money.of(5000));
        OTHER_RANSOM_VALUES.put(SkillType.EXP_REGULAR, Money.of(10000));
        OTHER_RANSOM_VALUES.put(SkillType.EXP_VETERAN, Money.of(25000));
        OTHER_RANSOM_VALUES.put(SkillType.EXP_ELITE, Money.of(50000));
    }
    //endregion Variable Declarations

    //region Constructors
    protected Person(final UUID id) {
        this.id = id;
        this.genealogy = new Genealogy(this);
    }

    public Person(final Campaign campaign) {
        this(RandomNameGenerator.UNNAMED, RandomNameGenerator.UNNAMED_SURNAME, campaign);
    }

    public Person(final Campaign campaign, final String factionCode) {
        this(RandomNameGenerator.UNNAMED, RandomNameGenerator.UNNAMED_SURNAME, campaign, factionCode);
    }

    public Person(final String givenName, final String surname, final Campaign campaign) {
        this(givenName, surname, campaign, campaign.getFactionCode());
    }

    public Person(final String givenName, final String surname, final @Nullable Campaign campaign,
                  final String factionCode) {
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
        // We assign the variables in XML file order
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
        setBirthday(LocalDate.now());

        originFaction = Factions.getInstance().getFaction(factionCode);
        originPlanet = null;
        phenotype = Phenotype.NONE;
        bloodname = "";
        biography = "";
        this.genealogy = new Genealogy(this);
        dueDate = null;
        expectedDueDate = null;
        setPortrait(new Portrait());
        setXPDirect(0);
        setTotalXPEarnings(0);
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
        dateOfDeath = null;
        recruitment = null;
        lastRankChangeDate = null;
        retirement = null;
        skills = new Skills();
        options = new PersonnelOptions();
        currentEdge = 0;
        techUnits = new ArrayList<>();
        personnelLog = new ArrayList<>();
        scenarioLog = new ArrayList<>();
        awardController = new PersonAwardController(this);
        injuries = new ArrayList<>();
        originalUnitWeight = EntityWeightClass.WEIGHT_ULTRA_LIGHT;
        originalUnitTech = TECH_IS1;
        originalUnitId = null;
        acquisitions = 0;

        //region Flags
        setClanPersonnel(originFaction.isClan());
        setCommander(false);
        setDivorceable(true);
        setFounder(false);
        setImmortal(false);
        setMarriageable(true);
        setTryingToConceive(true);
        //endregion Flags

        extraData = new ExtraData();

        // Initialize Data based on these settings
        setFullName();
    }
    //endregion Constructors

    public Phenotype getPhenotype() {
        return phenotype;
    }

    public void setPhenotype(final Phenotype phenotype) {
        this.phenotype = phenotype;
    }

    public String getBloodname() {
        return bloodname;
    }

    public void setBloodname(final String bloodname) {
        this.bloodname = bloodname;
        setFullName();
    }

    public Faction getOriginFaction() {
        return originFaction;
    }

    public void setOriginFaction(final Faction originFaction) {
        this.originFaction = originFaction;
    }

    public Planet getOriginPlanet() {
        return originPlanet;
    }

    public void setOriginPlanet(final Planet originPlanet) {
        this.originPlanet = originPlanet;
    }

    public PrisonerStatus getPrisonerStatus() {
        return prisonerStatus;
    }

    /**
     * This requires expanded checks because a number of functionalities are strictly dependent on
     * the current person's prisoner status.
     * @param campaign the campaign the person is a part of
     * @param prisonerStatus The new prisoner status for the person in question
     * @param log whether to log the change or not
     */
    public void setPrisonerStatus(final Campaign campaign, final PrisonerStatus prisonerStatus,
                                  final boolean log) {
        // This must be processed completely, as the unchanged prisoner status of Free to Free is
        // used during recruitment

        final boolean freed = !getPrisonerStatus().isFree();
        final boolean isPrisoner = prisonerStatus.isCurrentPrisoner();
        setPrisonerStatusDirect(prisonerStatus);

        // Now, we need to fix values and ranks based on the Person's status
        switch (prisonerStatus) {
            case PRISONER:
            case PRISONER_DEFECTOR:
            case BONDSMAN:
                setRecruitment(null);
                setLastRankChangeDate(null);
                if (log) {
                    if (isPrisoner) {
                        ServiceLogger.madePrisoner(this, campaign.getLocalDate(),
                                campaign.getName(), "");
                    } else {
                        ServiceLogger.madeBondsman(this, campaign.getLocalDate(),
                                campaign.getName(), "");
                    }
                }
                break;
            case FREE:
                if (!getPrimaryRole().isDependent()) {
                    if (campaign.getCampaignOptions().isUseTimeInService()) {
                        setRecruitment(campaign.getLocalDate());
                    }
                    if (campaign.getCampaignOptions().isUseTimeInRank()) {
                        setLastRankChangeDate(campaign.getLocalDate());
                    }
                }

                if (log) {
                    if (freed) {
                        ServiceLogger.freed(this, campaign.getLocalDate(),
                                campaign.getName(), "");
                    } else {
                        ServiceLogger.joined(this, campaign.getLocalDate(),
                                campaign.getName(), "");
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

    /**
     * This is public for unit testing reasons
     * @param prisonerStatus the person's new prisoner status
     */
    public void setPrisonerStatusDirect(final PrisonerStatus prisonerStatus) {
        this.prisonerStatus = prisonerStatus;
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
                + (getCallsign().isBlank() ? "" : (" \"" + getCallsign() + '"'))
                + (lastName.isBlank() ? "" : ' ' + lastName));
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
        return (getPreNominal().isBlank() ? "" : (getPreNominal() + ' ')) + getGivenName();
    }

    /**
     * Return a full last name which may be a bloodname or a surname with or without a post-nominal.
     * A bloodname will overrule a surname but we do not disallow surnames for clan personnel, if the
     * player wants to input them
     * @return a String of the person's last name
     */
    public String getLastName() {
        String lastName = !StringUtility.isNullOrBlank(getBloodname()) ? getBloodname()
                : !StringUtility.isNullOrBlank(getSurname()) ? getSurname()
                : "";
        if (!StringUtility.isNullOrBlank(getPostNominal())) {
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
        // Then, it depends on whether the person is Clan or not.
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

        if (isClanPersonnel()) {
            if (name.length > 1) {
                int i;
                for (i = 1; i < name.length - 1; i++) {
                    givenName.append(' ').append(name[i]);
                }

                if (!(!StringUtility.isNullOrBlank(getBloodname()) && getBloodname().equals(name[i]))) {
                    givenName.append(' ').append(name[i]);
                }
            }
        } else {
            if (name.length == 2) {
                setSurnameDirect(name[1]);
            } else if (name.length == 3) {
                setSurnameDirect(name[1] + ' ' + name[2]);
            } else if (name.length > 3) {
                int i;
                for (i = 1; i < name.length - 2; i++) {
                    givenName.append(' ').append(name[i]);
                }
                setSurnameDirect(name[i] + ' ' + name[i + 1]);
            }
        }

        if ((getSurname() == null) || getSurname().equals(RandomNameGenerator.UNNAMED_SURNAME)) {
            setSurnameDirect("");
        }

        setGivenNameDirect(givenName.toString());
        setFullName();
    }
    //endregion Names

    public Portrait getPortrait() {
        return portrait;
    }

    public void setPortrait(final Portrait portrait) {
        this.portrait = Objects.requireNonNull(portrait, "Illegal assignment: cannot have a null Portrait");
    }

    //region Personnel Roles
    public PersonnelRole getPrimaryRole() {
        return primaryRole;
    }

    public void setPrimaryRole(final Campaign campaign, final PersonnelRole primaryRole) {
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
            setRecruitment(campaign.getLocalDate());
            setLastRankChangeDate(campaign.getLocalDate());
        }

        // Finally, we can set the primary role
        setPrimaryRoleDirect(primaryRole);

        // and trigger the update event
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    public void setPrimaryRoleDirect(final PersonnelRole primaryRole) {
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

    public void setSecondaryRoleDirect(final PersonnelRole secondaryRole) {
        this.secondaryRole = secondaryRole;
    }

    /**
     * This is used to determine if a person has a specific role as either their primary OR their
     * secondary role
     * @param role the role to determine
     * @return true if the person has the specific role either as their primary or secondary role
     */
    public boolean hasRole(final PersonnelRole role) {
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
    public boolean hasSupportRole(final boolean excludeUnmarketable) {
        return getPrimaryRole().isSupport(excludeUnmarketable) || getSecondaryRole().isSupport(excludeUnmarketable);
    }

    public String getRoleDesc() {
        String role = getPrimaryRoleDesc();
        if (!getSecondaryRole().isNone()) {
            role += '/' + getSecondaryRoleDesc();
        }
        return role;
    }

    public String getPrimaryRoleDesc() {
        String bgPrefix = "";
        if (isClanPersonnel()) {
            bgPrefix = getPhenotype().getShortName() + ' ';
        }
        return bgPrefix + getPrimaryRole().getName(isClanPersonnel());
    }

    public String getSecondaryRoleDesc() {
        return getSecondaryRole().getName(isClanPersonnel());
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
                return Stream.of(SkillType.S_GUN_MECH, SkillType.S_PILOT_MECH, SkillType.S_GUN_AERO, SkillType.S_PILOT_AERO)
                        .allMatch(this::hasSkill);
            case GROUND_VEHICLE_DRIVER:
                return hasSkill(SkillType.S_PILOT_GVEE);
            case NAVAL_VEHICLE_DRIVER:
                return hasSkill(SkillType.S_PILOT_NVEE);
            case VTOL_PILOT:
                return hasSkill(SkillType.S_PILOT_VTOL);
            case VEHICLE_GUNNER:
                return hasSkill(SkillType.S_GUN_VEE);
            case VEHICLE_CREW:
                return hasSkill(SkillType.S_TECH_MECHANIC)
                        && (getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN);
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
                return hasSkill(SkillType.S_TECH_MECH)
                        && (getSkill(SkillType.S_TECH_MECH).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN);
            case MECHANIC:
                return hasSkill(SkillType.S_TECH_MECHANIC)
                        && (getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN);
            case AERO_TECH:
                return hasSkill(SkillType.S_TECH_AERO)
                        && (getSkill(SkillType.S_TECH_AERO).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN);
            case BA_TECH:
                return hasSkill(SkillType.S_TECH_BA)
                        && (getSkill(SkillType.S_TECH_BA).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN);
            case ASTECH:
                return hasSkill(SkillType.S_ASTECH);
            case DOCTOR:
                return hasSkill(SkillType.S_DOCTOR)
                        && (getSkill(SkillType.S_DOCTOR).getExperienceLevel() > SkillType.EXP_ULTRA_GREEN);
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
     * @param today the current date
     * @param status the person's new PersonnelStatus
     */
    public void changeStatus(final Campaign campaign, final LocalDate today,
                             final PersonnelStatus status) {
        if (status == getStatus()) { // no change means we don't need to process anything
            return;
        } else if (getStatus().isDead() && !status.isDead()) {
            // remove date of death for resurrection
            setDateOfDeath(null);
            campaign.addReport(String.format(resources.getString("resurrected.report"),
                    getHyperlinkedFullTitle()));
            ServiceLogger.resurrected(this, today);
        }

        switch (status) {
            case ACTIVE:
                if (getStatus().isMIA()) {
                    campaign.addReport(String.format(resources.getString("recoveredMIA.report"),
                            getHyperlinkedFullTitle()));
                    ServiceLogger.recoveredMia(this, today);
                } else if (getStatus().isPoW()) {
                    campaign.addReport(String.format(resources.getString("recoveredPoW.report"),
                            getHyperlinkedFullTitle()));
                    ServiceLogger.recoveredPoW(this, campaign.getLocalDate());
                } else if (getStatus().isOnLeave()) {
                    campaign.addReport(String.format(resources.getString("returnedFromLeave.report"),
                            getHyperlinkedFullTitle()));
                    ServiceLogger.returnedFromLeave(this, campaign.getLocalDate());
                } else if (getStatus().isAWOL()) {
                    campaign.addReport(String.format(resources.getString("returnedFromAWOL.report"),
                            getHyperlinkedFullTitle()));
                    ServiceLogger.returnedFromAWOL(this, campaign.getLocalDate());
                } else {
                    campaign.addReport(String.format(resources.getString("rehired.report"),
                            getHyperlinkedFullTitle()));
                    ServiceLogger.rehired(this, today);
                }
                setRetirement(null);
                break;
            case RETIRED:
                campaign.addReport(String.format(status.getReportText(), getHyperlinkedFullTitle()));
                ServiceLogger.retired(this, today);
                if (campaign.getCampaignOptions().isUseRetirementDateTracking()) {
                    setRetirement(today);
                }
                break;
            case PREGNANCY_COMPLICATIONS:
                campaign.getProcreation().processPregnancyComplications(campaign, campaign.getLocalDate(), this);
                // purposeful fall through
            default:
                campaign.addReport(String.format(status.getReportText(), getHyperlinkedFullTitle()));
                ServiceLogger.changedStatus(this, campaign.getLocalDate(), status);
                break;
        }

        setStatus(status);

        if (status.isDead()) {
            setDateOfDeath(today);
            // Don't forget to tell the spouse
            if (getGenealogy().hasSpouse() && !getGenealogy().getSpouse().getStatus().isDeadOrMIA()) {
                campaign.getDivorce().widowed(campaign, campaign.getLocalDate(), getGenealogy().getSpouse());
            }
        }

        if (status.isActive()) {
            // Check Pregnancy
            if (isPregnant() && getDueDate().isBefore(today)) {
                campaign.getProcreation().birth(campaign, getDueDate(), this);
            }
        } else {
            setDoctorId(null, campaign.getCampaignOptions().getNaturalHealingWaitingPeriod());

            // If we're assigned to a unit, remove us from it
            if (getUnit() != null) {
                getUnit().remove(this, true);
            }

            // Clear Tech Setup
            removeAllTechJobs(campaign);
        }

        MekHQ.triggerEvent(new PersonStatusChangedEvent(this));
    }

    /**
     * This is used to directly set the Person's PersonnelStatus without any processing
     * @param status the person's new status
     */
    public void setStatus(final PersonnelStatus status) {
        this.status = status;
    }

    public int getIdleMonths() {
        return idleMonths;
    }

    public void setIdleMonths(final int idleMonths) {
        this.idleMonths = idleMonths;
    }

    public int getDaysToWaitForHealing() {
        return daysToWaitForHealing;
    }

    public void setDaysToWaitForHealing(final int daysToWaitForHealing) {
        this.daysToWaitForHealing = daysToWaitForHealing;
    }

    public void setGender(final Gender gender) {
        this.gender = gender;
    }

    public Gender getGender() {
        return gender;
    }

    public void setBirthday(final LocalDate birthday) {
        this.birthday = birthday;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public @Nullable LocalDate getDateOfDeath() {
        return dateOfDeath;
    }

    public void setDateOfDeath(final @Nullable LocalDate dateOfDeath) {
        this.dateOfDeath = dateOfDeath;
    }

    public int getAge(LocalDate today) {
        // Get age based on year
        if (getDateOfDeath() != null) {
            //use date of death instead of birthday
            today = getDateOfDeath();
        }

        return Math.toIntExact(ChronoUnit.YEARS.between(getBirthday(), today));
    }

    public @Nullable LocalDate getRecruitment() {
        return recruitment;
    }

    public void setRecruitment(final @Nullable LocalDate recruitment) {
        this.recruitment = recruitment;
    }

    public String getTimeInService(final Campaign campaign) {
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

    public @Nullable LocalDate getLastRankChangeDate() {
        return lastRankChangeDate;
    }

    public void setLastRankChangeDate(final @Nullable LocalDate lastRankChangeDate) {
        this.lastRankChangeDate = lastRankChangeDate;
    }

    public String getTimeInRank(final Campaign campaign) {
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

    public @Nullable LocalDate getRetirement() {
        return retirement;
    }

    public void setRetirement(final @Nullable LocalDate retirement) {
        this.retirement = retirement;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public boolean isChild(final LocalDate today) {
        return getAge(today) < 18;
    }

    public Genealogy getGenealogy() {
        return genealogy;
    }

    //region Pregnancy
    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(final LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getExpectedDueDate() {
        return expectedDueDate;
    }

    public void setExpectedDueDate(final LocalDate expectedDueDate) {
        this.expectedDueDate = expectedDueDate;
    }

    public String getDueDateAsString(final Campaign campaign) {
        final LocalDate date = campaign.getCampaignOptions().isDisplayTrueDueDate()
                ? getDueDate() : getExpectedDueDate();
        return (date == null) ? "" : MekHQ.getMHQOptions().getDisplayFormattedDate(date);
    }

    public boolean isPregnant() {
        return dueDate != null;
    }
    //endregion Pregnancy

    //region Experience
    public int getXP() {
        return xp;
    }

    public void awardXP(final Campaign campaign, final int xp) {
        this.xp += xp;
        if (campaign.getCampaignOptions().isTrackTotalXPEarnings()) {
            changeTotalXPEarnings(xp);
        }
    }

    public void spendXP(final int xp) {
        this.xp -= xp;
    }

    public void setXP(final Campaign campaign, final int xp) {
        if (campaign.getCampaignOptions().isTrackTotalXPEarnings()) {
            changeTotalXPEarnings(xp - getXP());
        }
        setXPDirect(xp);
    }

    private void setXPDirect(final int xp) {
        this.xp = xp;
    }

    public int getTotalXPEarnings() {
        return totalXPEarnings;
    }

    public void changeTotalXPEarnings(final int xp) {
        setTotalXPEarnings(getTotalXPEarnings() + xp);
    }

    public void setTotalXPEarnings(final int totalXPEarnings) {
        this.totalXPEarnings = totalXPEarnings;
    }
    //endregion Experience

    public int getAcquisitions() {
        return acquisitions;
    }

    public void setAcquisition(final int acquisitions) {
        this.acquisitions = acquisitions;
    }

    public void incrementAcquisition() {
        acquisitions++;
    }

    public void setDoctorId(final @Nullable UUID doctorId, final int daysToWaitForHealing) {
        this.doctorId = doctorId;
        this.daysToWaitForHealing = daysToWaitForHealing;
    }

    public boolean checkNaturalHealing(final int daysToWait) {
        if (needsFixing() && (getDaysToWaitForHealing() <= 0) && (getDoctorId() == null)) {
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
        return (getUnit() != null) && (getUnit().getScenarioId() != -1);
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(final String biography) {
        this.biography = biography;
    }

    //region Flags
    public boolean isClanPersonnel() {
        return clanPersonnel;
    }

    public void setClanPersonnel(final boolean clanPersonnel) {
        this.clanPersonnel = clanPersonnel;
    }

    public boolean isCommander() {
        return commander;
    }

    public void setCommander(final boolean commander) {
        this.commander = commander;
    }

    public boolean isDivorceable() {
        return divorceable;
    }

    public void setDivorceable(final boolean divorceable) {
        this.divorceable = divorceable;
    }

    public boolean isFounder() {
        return founder;
    }

    public void setFounder(final boolean founder) {
        this.founder = founder;
    }

    public boolean isImmortal() {
        return immortal;
    }

    public void setImmortal(final boolean immortal) {
        this.immortal = immortal;
    }

    public boolean isMarriageable() {
        return marriageable;
    }

    public void setMarriageable(final boolean marriageable) {
        this.marriageable = marriageable;
    }

    public boolean isTryingToConceive() {
        return tryingToConceive;
    }

    public void setTryingToConceive(final boolean tryingToConceive) {
        this.tryingToConceive = tryingToConceive;
    }
    //endregion Flags

    public ExtraData getExtraData() {
        return extraData;
    }

    //region File I/O
    public void writeToXML(final PrintWriter pw, int indent, final Campaign campaign) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "person", "id", id, "type", getClass());
        try {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "id", id.toString());

            //region Name
            if (!StringUtility.isNullOrBlank(getPreNominal())) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "preNominal", getPreNominal());
            }
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "givenName", getGivenName());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "surname", getSurname());
            if (!StringUtility.isNullOrBlank(getPostNominal())) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "postNominal", getPostNominal());
            }

            if (getMaidenName() != null) { // this is only a != null comparison because empty is a use case for divorce
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "maidenName", getMaidenName());
            }

            if (!StringUtility.isNullOrBlank(getCallsign())) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "callsign", getCallsign());
            }
            //endregion Name

            // Always save the primary role
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "primaryRole", getPrimaryRole().name());
            if (!getSecondaryRole().isNone()) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "secondaryRole", getSecondaryRole().name());
            }

            if (primaryDesignator != ROMDesignation.NONE) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "primaryDesignator", primaryDesignator.name());
            }

            if (secondaryDesignator != ROMDesignation.NONE) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "secondaryDesignator", secondaryDesignator.name());
            }

            // Always save the person's origin faction
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "faction", originFaction.getShortName());
            if (originPlanet != null) {
                MHQXMLUtility.writeSimpleXMLAttributedTag(pw, indent, "planetId", "systemId",
                        originPlanet.getParentSystem().getId(), originPlanet.getId());
            }

            if (!getPhenotype().isNone()) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "phenotype", getPhenotype().name());
            }

            if (!StringUtility.isNullOrBlank(bloodname)) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "bloodname", bloodname);
            }

            if (!StringUtility.isNullOrBlank(biography)) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "biography", biography);
            }

            if (idleMonths > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "idleMonths", idleMonths);
            }

            if (!genealogy.isEmpty()) {
                genealogy.writeToXML(pw, indent);
            }
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "dueDate", getDueDate());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "expectedDueDate", getExpectedDueDate());
            getPortrait().writeToXML(pw, indent);
            if (getXP() != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "xp", getXP());
            }

            if (getTotalXPEarnings() != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "totalXPEarnings", getTotalXPEarnings());
            }

            if (daysToWaitForHealing != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "daysToWaitForHealing", daysToWaitForHealing);
            }
            // Always save the person's gender, as it would otherwise get confusing fast
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "gender", getGender().name());
            if (!getRankSystem().equals(campaign.getRankSystem())) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "rankSystem", getRankSystem().getCode());
            }
            // Always save a person's rank
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "rank", getRankNumeric());
            if (getRankLevel() != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "rankLevel", getRankLevel());
            }

            if (!getManeiDominiClass().isNone()) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "maneiDominiClass", getManeiDominiClass().name());
            }

            if (!getManeiDominiRank().isNone()) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "maneiDominiRank", getManeiDominiRank().name());
            }

            if (nTasks > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "nTasks", nTasks);
            }
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "doctorId", doctorId);
            if (getUnit() != null) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unitId", getUnit().getId());
            }

            if (!salary.equals(Money.of(-1))) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "salary", salary);
            }

            if (!totalEarnings.equals(Money.of(0))) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "totalEarnings", totalEarnings);
            }
            // Always save a person's status, to make it easy to parse the personnel saved data
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "status", status.name());
            if (prisonerStatus != PrisonerStatus.FREE) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "prisonerStatus", prisonerStatus.name());
            }

            if (hits > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "hits", hits);
            }

            if (toughness != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "toughness", toughness);
            }

            if (minutesLeft > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "minutesLeft", minutesLeft);
            }

            if (overtimeLeft > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "overtimeLeft", overtimeLeft);
            }
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "birthday", getBirthday());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "deathday", getDateOfDeath());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "recruitment", getRecruitment());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "lastRankChangeDate", getLastRankChangeDate());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "retirement", getRetirement());
            for (Skill skill : skills.getSkills()) {
                skill.writeToXML(pw, indent);
            }

            if (countOptions(PersonnelOptions.LVL3_ADVANTAGES) > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "advantages",
                        getOptionList("::", PersonnelOptions.LVL3_ADVANTAGES));
            }

            if (countOptions(PersonnelOptions.EDGE_ADVANTAGES) > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "edge",
                        getOptionList("::", PersonnelOptions.EDGE_ADVANTAGES));
                // For support personnel, write an available edge value
                if (hasSupportRole(true) || isEngineer()) {
                    MHQXMLUtility.writeSimpleXMLTag(pw, indent, "edgeAvailable", getCurrentEdge());
                }
            }

            if (countOptions(PersonnelOptions.MD_ADVANTAGES) > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "implants",
                        getOptionList("::", PersonnelOptions.MD_ADVANTAGES));
            }

            if (!techUnits.isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "techUnitIds");
                for (Unit unit : techUnits) {
                    MHQXMLUtility.writeSimpleXMLTag(pw, indent, "id", unit.getId());
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "techUnitIds");
            }

            if (!personnelLog.isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "personnelLog");
                for (LogEntry entry : personnelLog) {
                    entry.writeToXML(pw, indent);
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "personnelLog");
            }

            if (!scenarioLog.isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "scenarioLog");
                for (LogEntry entry : scenarioLog) {
                    entry.writeToXML(pw, indent);
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "scenarioLog");
            }

            if (!getAwardController().getAwards().isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "awards");
                for (Award award : getAwardController().getAwards()) {
                    award.writeToXML(pw, indent);
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "awards");
            }

            if (!injuries.isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "injuries");
                for (Injury injury : injuries) {
                    injury.writeToXml(pw, indent);
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "injuries");
            }

            if (originalUnitWeight != EntityWeightClass.WEIGHT_ULTRA_LIGHT) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "originalUnitWeight", originalUnitWeight);
            }

            if (originalUnitTech != TECH_IS1) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "originalUnitTech", originalUnitTech);
            }
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "originalUnitId", originalUnitId);
            if (acquisitions != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "acquisitions", acquisitions);
            }

            //region Flags
            // Always save whether they are clan personnel or not
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "clanPersonnel", isClanPersonnel());
            if (isCommander()) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "commander", true);
            }

            if (!isDivorceable()) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "divorceable", false);
            }

            if (isFounder()) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "founder", true);
            }

            if (isImmortal()) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "immortal", true);
            }

            if (!isMarriageable()) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "marriageable", false);
            }

            if (!isTryingToConceive()) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "tryingToConceive", false);
            }
            //endregion Flags

            if (!extraData.isEmpty()) {
                extraData.writeToXml(pw);
            }
        } catch (Exception ex) {
            LogManager.getLogger().error("Failed to write " + getFullName() + " to the XML File", ex);
            throw ex; // we want to rethrow to ensure that the save fails
        }

        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "person");
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

                if (wn2.getNodeName().equalsIgnoreCase("preNominal")) {
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
                } else if (wn2.getNodeName().equalsIgnoreCase("faction")) {
                    if (version.isLowerThan("0.49.7")) {
                        retVal.setOriginFaction(Factions.getInstance().getFaction(
                                FactionMigrator.migrateCodePINDRemoval(wn2.getTextContent().trim())));
                    } else {
                        retVal.setOriginFaction(Factions.getInstance().getFaction(wn2.getTextContent().trim()));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("planetId")) {
                    String systemId = "", planetId = "";
                    try {
                        systemId = wn2.getAttributes().getNamedItem("systemId").getTextContent().trim();
                        planetId = wn2.getTextContent().trim();
                        PlanetarySystem ps = c.getSystemById(systemId);
                        Planet p = null;
                        if (ps == null) {
                            ps = c.getSystemByName(systemId);
                        }
                        if (ps != null) {
                            p = ps.getPlanetById(planetId);
                        }
                        retVal.originPlanet = p;
                    } catch (NullPointerException e) {
                        LogManager.getLogger().error("Error loading originPlanet for " + systemId + ", " + planetId, e);
                    }
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
                } else if (wn2.getNodeName().equalsIgnoreCase("genealogy")) {
                    retVal.getGenealogy().fillFromXML(wn2.getChildNodes());
                } else if (wn2.getNodeName().equalsIgnoreCase("dueDate")) {
                    retVal.dueDate = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("expectedDueDate")) {
                    retVal.expectedDueDate = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase(Portrait.XML_TAG)) {
                    retVal.setPortrait(Portrait.parseFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase("xp")) {
                    retVal.setXPDirect(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("totalXPEarnings")) {
                    retVal.setTotalXPEarnings(Integer.parseInt(wn2.getTextContent().trim()));
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
                } else if (wn2.getNodeName().equalsIgnoreCase("salary")) {
                    retVal.salary = Money.fromXmlString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("totalEarnings")) {
                    retVal.totalEarnings = Money.fromXmlString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("minutesLeft")) {
                    retVal.minutesLeft = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("overtimeLeft")) {
                    retVal.overtimeLeft = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("birthday")) {
                    retVal.birthday = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("deathday")) {
                    retVal.dateOfDeath = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("recruitment")) {
                    retVal.recruitment = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("lastRankChangeDate")) {
                    retVal.lastRankChangeDate = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("retirement")) {
                    retVal.setRetirement(MHQXMLUtility.parseDate(wn2.getTextContent().trim()));
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
                            LogManager.getLogger().error("Unknown node type not loaded in techUnitIds nodes: " + wn3.getNodeName());
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
                            LogManager.getLogger().error("Unknown node type not loaded in personnel log nodes: " + wn3.getNodeName());
                            continue;
                        }

                        final LogEntry logEntry = LogEntryFactory.getInstance().generateInstanceFromXML(wn3);
                        if (logEntry != null) {
                            retVal.addLogEntry(logEntry);
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("missionLog") // Legacy - 0.49.11 removal
                        || wn2.getNodeName().equalsIgnoreCase("scenarioLog")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("logEntry")) {
                            LogManager.getLogger().error("Unknown node type not loaded in scenario log nodes: " + wn3.getNodeName());
                            continue;
                        }

                        final LogEntry logEntry = LogEntryFactory.getInstance().generateInstanceFromXML(wn3);
                        if (logEntry != null) {
                            retVal.addScenarioLogEntry(logEntry);
                        }
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
                            LogManager.getLogger().error("Unknown node type not loaded in personnel log nodes: " + wn3.getNodeName());
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
                            LogManager.getLogger().error("Unknown node type not loaded in injury nodes: " + wn3.getNodeName());
                            continue;
                        }
                        retVal.injuries.add(Injury.generateInstanceFromXML(wn3));
                    }
                    LocalDate now = c.getLocalDate();
                    retVal.injuries.stream().filter(inj -> (null == inj.getStart()))
                        .forEach(inj -> inj.setStart(now.minusDays(inj.getOriginalTime() - inj.getTime())));
                } else if (wn2.getNodeName().equalsIgnoreCase("originalUnitWeight")) {
                    retVal.originalUnitWeight = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("originalUnitTech")) {
                    retVal.originalUnitTech = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("originalUnitId")) {
                    retVal.originalUnitId = UUID.fromString(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("clanPersonnel")
                        || wn2.getNodeName().equalsIgnoreCase("clan")) { // Legacy - 0.49.9 removal
                    retVal.setClanPersonnel(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("commander")) {
                    retVal.setCommander(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("divorceable")) {
                    retVal.setDivorceable(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("founder")) {
                    retVal.setFounder(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("immortal")) {
                    retVal.setImmortal(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("marriageable")) {
                    retVal.setMarriageable(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("tryingToConceive")) {
                    retVal.setTryingToConceive(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("extraData")) {
                    retVal.extraData = ExtraData.createFromXml(wn2);
                } else if (wn2.getNodeName().equalsIgnoreCase("tryingToMarry")) { // Legacy - 0.49.4 removal
                    retVal.setMarriageable(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("honorific")) { // Legacy, removed in 0.49.3
                    retVal.setPostNominalDirect(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("portraitCategory")) { // Legacy - 0.49.3 removal
                    retVal.getPortrait().setCategory(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("portraitFile")) { // Legacy - 0.49.3 removal
                    retVal.getPortrait().setFilename(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("dependent")) { // Legacy, 0.49.1 removal
                    // Legacy setup was as Astechs, but people (including me) often forgot to remove
                    // the flag so... just ignoring if they aren't astechs
                    if (retVal.getPrimaryRole().isAstech()) {
                        retVal.setPrimaryRoleDirect(PersonnelRole.DEPENDENT);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("willingToDefect")) { // Legacy
                    if (Boolean.parseBoolean(wn2.getTextContent().trim())) {
                        retVal.prisonerStatus = PrisonerStatus.PRISONER_DEFECTOR;
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("ancestors")) { // legacy - 0.47.6 removal
                    CampaignXmlParser.addToAncestryMigrationMap(UUID.fromString(wn2.getTextContent().trim()), retVal);
                } else if (wn2.getNodeName().equalsIgnoreCase("spouse")) { // legacy - 0.47.6 removal
                    retVal.getGenealogy().setSpouse(new PersonIdReference(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("formerSpouses")) { // legacy - 0.47.6 removal
                    retVal.getGenealogy().loadFormerSpouses(wn2.getChildNodes());
                } else if (wn2.getNodeName().equalsIgnoreCase("name")) { // legacy - 0.47.5 removal
                    retVal.migrateName(wn2.getTextContent());
                }
            }

            retVal.setFullName(); // this sets the name based on the loaded values

            if (version.isLowerThan("0.47.5") && (retVal.getExpectedDueDate() == null)
                    && (retVal.getDueDate() != null)) {
                retVal.setExpectedDueDate(retVal.getDueDate());
            }

            if ((advantages != null) && !advantages.isBlank()) {
                StringTokenizer st = new StringTokenizer(advantages, "::");
                while (st.hasMoreTokens()) {
                    String adv = st.nextToken();
                    String advName = Crew.parseAdvantageName(adv);
                    Object value = Crew.parseAdvantageValue(adv);

                    try {
                        retVal.getOptions().getOption(advName).setValue(value);
                    } catch (Exception e) {
                        LogManager.getLogger().error("Error restoring advantage: " + adv);
                    }
                }
            }

            if ((edge != null) && !edge.isBlank()) {
                StringTokenizer st = new StringTokenizer(edge, "::");
                while (st.hasMoreTokens()) {
                    String adv = st.nextToken();
                    String advName = Crew.parseAdvantageName(adv);
                    Object value = Crew.parseAdvantageValue(adv);

                    try {
                        retVal.getOptions().getOption(advName).setValue(value);
                    } catch (Exception e) {
                        LogManager.getLogger().error("Error restoring edge: " + adv);
                    }
                }
            }

            if ((implants != null) && !implants.isBlank()) {
                StringTokenizer st = new StringTokenizer(implants, "::");
                while (st.hasMoreTokens()) {
                    String adv = st.nextToken();
                    String advName = Crew.parseAdvantageName(adv);
                    Object value = Crew.parseAdvantageValue(adv);

                    try {
                        retVal.getOptions().getOption(advName).setValue(value);
                    } catch (Exception e) {
                        LogManager.getLogger().error("Error restoring implants: " + adv);
                    }
                }
            }

            // Fixing Prisoner Ranks - 0.47.X Fix
            if (retVal.getRankNumeric() < 0) {
                retVal.setRank(0);
            }
        } catch (Exception e) {
            LogManager.getLogger().error("Failed to read person " + retVal.getFullName() + " from file", e);
            retVal = null;
        }

        return retVal;
    }
    //endregion File I/O

    public void setSalary(final Money salary) {
        this.salary = salary;
    }

    public Money getSalary(final Campaign campaign) {
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

        // SpecInf is a special case, this needs to be applied first to bring base salary up to RAW.
        if (getPrimaryRole().isSoldierOrBattleArmour()) {
            if ((getUnit() != null) && getUnit().isConventionalInfantry()
                    && ((Infantry) getUnit().getEntity()).hasSpecialization()) {
                primaryBase = primaryBase.multipliedBy(campaign.getCampaignOptions().getSalarySpecialistInfantryMultiplier());
            }
        }

        // Experience multiplier
        primaryBase = primaryBase.multipliedBy(campaign.getCampaignOptions().getSalaryXPMultipliers().get(getSkillLevel(campaign, false)));

        // Specialization multiplier
        if (getPrimaryRole().isSoldierOrBattleArmour()) {
            if (hasSkill(SkillType.S_ANTI_MECH)) {
                primaryBase = primaryBase.multipliedBy(campaign.getCampaignOptions().getSalaryAntiMekMultiplier());
            }
        }

        // CamOps doesn't cover secondary roles, so we just half the base salary of the secondary role.
        Money secondaryBase = campaign.getCampaignOptions().getRoleBaseSalaries()[getSecondaryRole().ordinal()].dividedBy(2);

        // SpecInf is a special case, this needs to be applied first to bring base salary up to RAW.
        if (getSecondaryRole().isSoldierOrBattleArmour()) {
            if (hasSkill(SkillType.S_ANTI_MECH)) {
                secondaryBase = secondaryBase.multipliedBy(campaign.getCampaignOptions().getSalaryAntiMekMultiplier());
            }
        }

        // Experience modifier
        secondaryBase = secondaryBase.multipliedBy(campaign.getCampaignOptions().getSalaryXPMultipliers().get(getSkillLevel(campaign, true)));

        // Specialization
        if (getSecondaryRole().isSoldierOrBattleArmour()) {
            if (hasSkill(SkillType.S_ANTI_MECH)) {
                secondaryBase = secondaryBase.multipliedBy(campaign.getCampaignOptions().getSalaryAntiMekMultiplier());
            }
        }

        // TODO: distinguish DropShip, JumpShip, and WarShip crew
        // TODO: Add era mod to salary calc..
        return primaryBase.plus(secondaryBase);
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
    public void payPerson(final Money money) {
        totalEarnings = getTotalEarnings().plus(money);
    }

    /**
     * This is used to pay a person their salary
     * @param campaign the campaign the person is a part of
     */
    public void payPersonSalary(final Campaign campaign) {
        if (getStatus().isActive()) {
            payPerson(getSalary(campaign));
        }
    }

    /**
     * This is used to pay a person their share value based on the value of a single share
     * @param campaign the campaign the person is a part of
     * @param money the value of a single share
     * @param sharesForAll whether or not all personnel have shares
     */
    public void payPersonShares(final Campaign campaign, final Money money,
                                final boolean sharesForAll) {
        final int shares = getNumShares(campaign, sharesForAll);
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

        if (campaign.getCampaignOptions().isUseTimeInRank()) {
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
            rankName += ROMDesignation.getComStarBranchDesignation(this);
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
     * @return true if <code>other</code> has a lower rank, or if <code>other</code> is null.
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

    @Override
    public String toString() {
        return getFullName();
    }

    /**
     * Two people are determined to be equal if they have the same id
     * @param object The object to check if it is equal to the person or not
     * @return True if they have the same id, otherwise false
     */
    @Override
    public boolean equals(final @Nullable Object object) {
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

    public SkillLevel getSkillLevel(final Campaign campaign, final boolean secondary) {
        return Skills.SKILL_LEVELS[getExperienceLevel(campaign, secondary) + 1];
    }

    public int getExperienceLevel(final Campaign campaign, final boolean secondary) {
        final PersonnelRole role = secondary ? getSecondaryRole() : getPrimaryRole();
        switch (role) {
            case MECHWARRIOR:
                if (hasSkill(SkillType.S_GUN_MECH) && hasSkill(SkillType.S_PILOT_MECH)) {
                    /*
                     * Attempt to use higher precision averaging, but if it doesn't provide a clear result
                     * due to non-standard experience thresholds then fall back on lower precision averaging
                     * See Bug #140
                     */
                    if (campaign.getCampaignOptions().isAlternativeQualityAveraging()) {
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
                    return SkillType.EXP_NONE;
                }
            case LAM_PILOT:
                if (Stream.of(SkillType.S_GUN_MECH, SkillType.S_PILOT_MECH,
                        SkillType.S_GUN_AERO, SkillType.S_PILOT_AERO).allMatch(this::hasSkill)) {
                    /*
                     * Attempt to use higher precision averaging, but if it doesn't provide a clear result
                     * due to non-standard experience thresholds then fall back on lower precision averaging
                     * See Bug #140
                     */
                    if (campaign.getCampaignOptions().isAlternativeQualityAveraging()) {
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
                    return SkillType.EXP_NONE;
                }
            case GROUND_VEHICLE_DRIVER:
                return hasSkill(SkillType.S_PILOT_GVEE) ? getSkill(SkillType.S_PILOT_GVEE).getExperienceLevel() : SkillType.EXP_NONE;
            case NAVAL_VEHICLE_DRIVER:
                return hasSkill(SkillType.S_PILOT_NVEE) ? getSkill(SkillType.S_PILOT_NVEE).getExperienceLevel() : SkillType.EXP_NONE;
            case VTOL_PILOT:
                return hasSkill(SkillType.S_PILOT_VTOL) ? getSkill(SkillType.S_PILOT_VTOL).getExperienceLevel() : SkillType.EXP_NONE;
            case VEHICLE_GUNNER:
                return hasSkill(SkillType.S_GUN_VEE) ? getSkill(SkillType.S_GUN_VEE).getExperienceLevel() : SkillType.EXP_NONE;
            case VEHICLE_CREW:
                return hasSkill(SkillType.S_TECH_MECHANIC) ? getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel() : SkillType.EXP_NONE;
            case AEROSPACE_PILOT:
                if (hasSkill(SkillType.S_GUN_AERO) && hasSkill(SkillType.S_PILOT_AERO)) {
                    if (campaign.getCampaignOptions().isAlternativeQualityAveraging()) {
                        int rawScore = (int) Math.floor(
                            (getSkill(SkillType.S_GUN_AERO).getLevel() + getSkill(SkillType.S_PILOT_AERO).getLevel()) / 2.0
                        );
                        if (getSkill(SkillType.S_GUN_AERO).getType().getExperienceLevel(rawScore) ==
                            getSkill(SkillType.S_PILOT_AERO).getType().getExperienceLevel(rawScore)) {
                            return getSkill(SkillType.S_GUN_AERO).getType().getExperienceLevel(rawScore);
                        }
                    }

                    return (int) Math.floor((getSkill(SkillType.S_GUN_AERO).getExperienceLevel()
                            + getSkill(SkillType.S_PILOT_AERO).getExperienceLevel()) / 2.0);
                } else {
                    return SkillType.EXP_NONE;
                }
            case CONVENTIONAL_AIRCRAFT_PILOT:
                if (hasSkill(SkillType.S_GUN_JET) && hasSkill(SkillType.S_PILOT_JET)) {
                    if (campaign.getCampaignOptions().isAlternativeQualityAveraging()) {
                        int rawScore = (int) Math.floor(
                            (getSkill(SkillType.S_GUN_JET).getLevel() + getSkill(SkillType.S_PILOT_JET).getLevel()) / 2.0
                        );
                        if (getSkill(SkillType.S_GUN_JET).getType().getExperienceLevel(rawScore) ==
                            getSkill(SkillType.S_PILOT_JET).getType().getExperienceLevel(rawScore)) {
                            return getSkill(SkillType.S_GUN_JET).getType().getExperienceLevel(rawScore);
                        }
                    }

                    return (int) Math.floor((getSkill(SkillType.S_GUN_JET).getExperienceLevel()
                            + getSkill(SkillType.S_PILOT_JET).getExperienceLevel()) / 2.0);
                } else {
                    return SkillType.EXP_NONE;
                }
            case PROTOMECH_PILOT:
                return hasSkill(SkillType.S_GUN_PROTO) ? getSkill(SkillType.S_GUN_PROTO).getExperienceLevel() : SkillType.EXP_NONE;
            case BATTLE_ARMOUR:
                if (hasSkill(SkillType.S_GUN_BA) && hasSkill(SkillType.S_ANTI_MECH)) {
                    if (campaign.getCampaignOptions().isAlternativeQualityAveraging()) {
                        int rawScore = (int) Math.floor(
                            (getSkill(SkillType.S_GUN_BA).getLevel() + getSkill(SkillType.S_ANTI_MECH).getLevel()) / 2.0
                        );
                        if (getSkill(SkillType.S_GUN_BA).getType().getExperienceLevel(rawScore) ==
                            getSkill(SkillType.S_ANTI_MECH).getType().getExperienceLevel(rawScore)) {
                            return getSkill(SkillType.S_GUN_BA).getType().getExperienceLevel(rawScore);
                        }
                    }

                    return (int) Math.floor((getSkill(SkillType.S_GUN_BA).getExperienceLevel()
                            + getSkill(SkillType.S_ANTI_MECH).getExperienceLevel()) / 2.0);
                } else {
                    return SkillType.EXP_NONE;
                }
            case SOLDIER:
                return hasSkill(SkillType.S_SMALL_ARMS) ? getSkill(SkillType.S_SMALL_ARMS).getExperienceLevel() : SkillType.EXP_NONE;
            case VESSEL_PILOT:
                return hasSkill(SkillType.S_PILOT_SPACE) ? getSkill(SkillType.S_PILOT_SPACE).getExperienceLevel() : SkillType.EXP_NONE;
            case VESSEL_GUNNER:
                return hasSkill(SkillType.S_GUN_SPACE) ? getSkill(SkillType.S_GUN_SPACE).getExperienceLevel() : SkillType.EXP_NONE;
            case VESSEL_CREW:
                return hasSkill(SkillType.S_TECH_VESSEL) ? getSkill(SkillType.S_TECH_VESSEL).getExperienceLevel() : SkillType.EXP_NONE;
            case VESSEL_NAVIGATOR:
                return hasSkill(SkillType.S_NAV) ? getSkill(SkillType.S_NAV).getExperienceLevel() : SkillType.EXP_NONE;
            case MECH_TECH:
                return hasSkill(SkillType.S_TECH_MECH) ? getSkill(SkillType.S_TECH_MECH).getExperienceLevel() : SkillType.EXP_NONE;
            case MECHANIC:
                return hasSkill(SkillType.S_TECH_MECHANIC) ? getSkill(SkillType.S_TECH_MECHANIC).getExperienceLevel() : SkillType.EXP_NONE;
            case AERO_TECH:
                return hasSkill(SkillType.S_TECH_AERO) ? getSkill(SkillType.S_TECH_AERO).getExperienceLevel() : SkillType.EXP_NONE;
            case BA_TECH:
                return hasSkill(SkillType.S_TECH_BA) ? getSkill(SkillType.S_TECH_BA).getExperienceLevel() : SkillType.EXP_NONE;
            case ASTECH:
                return hasSkill(SkillType.S_ASTECH) ? getSkill(SkillType.S_ASTECH).getExperienceLevel() : SkillType.EXP_NONE;
            case DOCTOR:
                return hasSkill(SkillType.S_DOCTOR) ? getSkill(SkillType.S_DOCTOR).getExperienceLevel() : SkillType.EXP_NONE;
            case MEDIC:
                return hasSkill(SkillType.S_MEDTECH) ? getSkill(SkillType.S_MEDTECH).getExperienceLevel() : SkillType.EXP_NONE;
            case ADMINISTRATOR_COMMAND:
            case ADMINISTRATOR_LOGISTICS:
            case ADMINISTRATOR_TRANSPORT:
            case ADMINISTRATOR_HR:
                return hasSkill(SkillType.S_ADMIN) ? getSkill(SkillType.S_ADMIN).getExperienceLevel() : SkillType.EXP_NONE;
            case DEPENDENT:
            case NONE:
            default:
                return SkillType.EXP_NONE;
        }
    }

    /**
     * @param campaign the campaign the person is a part of
     * @return a full description in HTML format that will be used for the graphical display in the
     * personnel table among other places
     */
    public String getFullDesc(final Campaign campaign) {
        return "<b>" + getFullTitle() + "</b><br/>" + getSkillLevel(campaign, false) + ' ' + getRoleDesc();
    }

    public String getHTMLTitle() {
        return String.format("<html><div id=\"%s\" style=\"white-space: nowrap;\">%s</div></html>",
                getId(), getFullTitle());
    }

    public String getFullTitle() {
        String rank = getRankName();

        if (!rank.isBlank()) {
            rank = rank + ' ';
        }

        return rank + getFullName();
    }

    public String makeHTMLRank() {
        return String.format("<html><div id=\"%s\">%s</div></html>", getId(), getRankName().trim());
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
    public void setPrimaryDesignator(final ROMDesignation primaryDesignator) {
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
    public void setSecondaryDesignator(final ROMDesignation secondaryDesignator) {
        this.secondaryDesignator = secondaryDesignator;
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    public int getHealingDifficulty(final Campaign campaign) {
        return campaign.getCampaignOptions().isTougherHealing() ? Math.max(0, getHits() - 2) : 0;
    }

    public TargetRoll getHealingMods(final Campaign campaign) {
        return new TargetRoll(getHealingDifficulty(campaign), "difficulty");
    }

    public String fail() {
        return " <font color='red'><b>Failed to heal.</b></font>";
    }

    //region skill
    public boolean hasSkill(final @Nullable String skillName) {
        return skills.hasSkill(skillName);
    }

    public Skills getSkills() {
        return skills;
    }

    public @Nullable Skill getSkill(final @Nullable String skillName) {
        return skills.getSkill(skillName);
    }

    public int getSkillLevel(final String skillName) {
        final Skill skill = getSkill(skillName);
        return (skill == null) ? 0 : skill.getExperienceLevel();
    }

    public void addSkill(final String skillName, final Skill skill) {
        skills.addSkill(skillName, skill);
    }

    public void addSkill(final String skillName, final int level, final int bonus) {
        skills.addSkill(skillName, new Skill(skillName, level, bonus));
    }

    public void removeSkill(final String skillName) {
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
    public void limitSkills(final int maxLevel) {
        for (final Skill skill : skills.getSkills()) {
            if (skill.getLevel() > maxLevel) {
                skill.setLevel(maxLevel);
            }
        }
    }

    public void improveSkill(final String skillName) {
        if (hasSkill(skillName)) {
            getSkill(skillName).improve();
        } else {
            addSkill(skillName, 0, 0);
        }
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    public int getCostToImprove(final String skillName) {
        return hasSkill(skillName) ? getSkill(skillName).getCostToImprove() : -1;
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

    public void setHits(final int hits) {
        this.hits = hits;
    }

    /**
      * @return <code>true</code> if the location (or any of its parent locations) has an injury
      * which implies that the location (most likely a limb) is severed. By checking parents we
     * can tell that they should be missing from the parent being severed, like a hand is missing if
     * the corresponding arms is.
      */
    public boolean isLocationMissing(final @Nullable BodyLocation location) {
        return (location != null)
                && (getInjuriesByLocation(location).stream()
                        .anyMatch(injury -> injury.getType().impliesMissingLocation(location))
                || isLocationMissing(location.Parent()));
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
     * @return the options of the given category that this pilot has
     */
    public Enumeration<IOption> getOptions(final String groupKey) {
        return options.getOptions(groupKey);
    }

    public int countOptions(final String groupKey) {
        int count = 0;

        for (final Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            final IOptionGroup group = i.nextElement();

            if (!group.getKey().equalsIgnoreCase(groupKey)) {
                continue;
            }

            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                final IOption option = j.nextElement();

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
    public String getOptionList(@Nullable String sep, final String groupKey) {
        final StringBuilder adv = new StringBuilder();

        if (sep == null) {
            sep = "";
        }

        for (final Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            final IOptionGroup group = i.nextElement();
            if (!group.getKey().equalsIgnoreCase(groupKey)) {
                continue;
            }

            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                final IOption option = j.nextElement();

                if (option.booleanValue()) {
                    if (adv.length() > 0) {
                        adv.append(sep);
                    }

                    adv.append(option.getName());
                    if (IntStream.of(IOption.STRING, IOption.CHOICE, IOption.INTEGER)
                            .anyMatch(k -> (option.getType() == k))) {
                        adv.append(' ').append(option.stringValue());
                    }
                }
            }
        }

        return adv.toString();
    }

    /**
     * @return an html-coded list that says what abilities are enabled for this pilot
     */
    public @Nullable String getAbilityListAsString(final String type) {
        final StringBuilder abilityString = new StringBuilder();
        for (Enumeration<IOption> i = getOptions(type); i.hasMoreElements(); ) {
            final IOption ability = i.nextElement();
            if (ability.booleanValue()) {
                abilityString.append(Utilities.getOptionDisplayName(ability)).append("<br>");
            }
        }

        return (abilityString.length() == 0) ? null : "<html>" + abilityString + "</html>";
    }
    //endregion Personnel Options

    //region edge
    public int getEdge() {
        return getOptions().intOption(OptionsConstants.EDGE);
    }

    public void setEdge(final int edge) {
        for (Enumeration<IOption> i = getOptions(PersonnelOptions.EDGE_ADVANTAGES); i.hasMoreElements(); ) {
            IOption ability = i.nextElement();
            if (OptionsConstants.EDGE.equals(ability.getName())) {
                ability.setValue(edge);
            }
        }
    }

    public void changeEdge(final int amount) {
        setEdge(Math.max(getEdge() + amount, 0));
    }

    /**
     * Resets support personnel edge points to the purchased level. Used for weekly refresh.
     */
    public void resetCurrentEdge() {
        setCurrentEdge(getEdge());
    }

    /**
     * Sets support personnel edge points to the value 'currentEdge'. Used for weekly refresh.
     * @param currentEdge - integer used to track this person's edge points available for the current week
     */
    public void setCurrentEdge(final int currentEdge) {
        this.currentEdge = currentEdge;
    }

    public void changeCurrentEdge(final int amount) {
        currentEdge = Math.max(currentEdge + amount, 0);
    }

    /**
     * @return this person's currently available edge points. Used for weekly refresh.
     */
    public int getCurrentEdge() {
        return currentEdge;
    }

    public void setEdgeUsed(final int edgeUsedThisRound) {
        this.edgeUsedThisRound = edgeUsedThisRound;
    }

    public int getEdgeUsed() {
        return edgeUsedThisRound;
    }

    /**
     * This will set a specific edge trigger, regardless of the current status
     */
    public void setEdgeTrigger(final String name, final boolean status) {
        for (Enumeration<IOption> i = getOptions(PersonnelOptions.EDGE_ADVANTAGES); i.hasMoreElements(); ) {
            final IOption ability = i.nextElement();
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
    public void changeEdgeTrigger(final String name) {
        for (Enumeration<IOption> i = getOptions(PersonnelOptions.EDGE_ADVANTAGES); i.hasMoreElements(); ) {
            final IOption ability = i.nextElement();
            if (ability.getName().equals(name)) {
                ability.setValue(!ability.booleanValue());
            }
        }
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    /**
     * @return an html-coded tooltip that says what edge will be used
     */
    public String getEdgeTooltip() {
        final StringBuilder stringBuilder = new StringBuilder();
        for (Enumeration<IOption> i = getOptions(PersonnelOptions.EDGE_ADVANTAGES); i.hasMoreElements(); ) {
            final IOption ability = i.nextElement();
            // yuck, it would be nice to have a more fool-proof way of identifying edge triggers
            if (ability.getName().contains("edge_when") && ability.booleanValue()) {
                stringBuilder.append(ability.getDescription()).append("<br>");
            }
        }

        return stringBuilder.toString().isBlank() ? "No triggers set" : "<html>" + stringBuilder + "</html>";
    }
    //endregion edge

    public boolean canDrive(final Entity entity) {
        if (entity instanceof LandAirMech) {
            return hasSkill(SkillType.S_PILOT_MECH) && hasSkill(SkillType.S_PILOT_AERO);
        } else if (entity instanceof Mech) {
            return hasSkill(SkillType.S_PILOT_MECH);
        } else if (entity instanceof VTOL) {
            return hasSkill(SkillType.S_PILOT_VTOL);
        } else if (entity instanceof Tank) {
            return hasSkill(entity.getMovementMode().isMarine() ? SkillType.S_PILOT_NVEE : SkillType.S_PILOT_GVEE);
        } else if (entity instanceof ConvFighter) {
            return hasSkill(SkillType.S_PILOT_JET) || hasSkill(SkillType.S_PILOT_AERO);
        } else if ((entity instanceof SmallCraft) || (entity instanceof Jumpship)) {
            return hasSkill(SkillType.S_PILOT_SPACE);
        } else if (entity instanceof Aero) {
            return hasSkill(SkillType.S_PILOT_AERO);
        } else if (entity instanceof BattleArmor) {
            return hasSkill(SkillType.S_GUN_BA);
        } else if (entity instanceof Infantry) {
            return hasSkill(SkillType.S_SMALL_ARMS);
        } else if (entity instanceof Protomech) {
            return hasSkill(SkillType.S_GUN_PROTO);
        } else {
            return false;
        }
    }

    public boolean canGun(final Entity entity) {
        if (entity instanceof LandAirMech) {
            return hasSkill(SkillType.S_GUN_MECH) && hasSkill(SkillType.S_GUN_AERO);
        } else if (entity instanceof Mech) {
            return hasSkill(SkillType.S_GUN_MECH);
        } else if (entity instanceof Tank) {
            return hasSkill(SkillType.S_GUN_VEE);
        } else if (entity instanceof ConvFighter) {
            return hasSkill(SkillType.S_GUN_JET) || hasSkill(SkillType.S_GUN_AERO);
        } else if ((entity instanceof SmallCraft) || (entity instanceof Jumpship)) {
            return hasSkill(SkillType.S_GUN_SPACE);
        } else if (entity instanceof Aero) {
            return hasSkill(SkillType.S_GUN_AERO);
        } else if (entity instanceof BattleArmor) {
            return hasSkill(SkillType.S_GUN_BA);
        } else if (entity instanceof Infantry) {
            return hasSkill(SkillType.S_SMALL_ARMS);
        } else if (entity instanceof Protomech) {
            return hasSkill(SkillType.S_GUN_PROTO);
        } else {
            return false;
        }
    }

    public boolean canTech(final Entity entity) {
        if ((entity instanceof Mech) || (entity instanceof Protomech)) {
            return hasSkill(SkillType.S_TECH_MECH);
        } else if (entity instanceof Aero) {
            return hasSkill(SkillType.S_TECH_AERO);
        } else if (entity instanceof BattleArmor) {
            return hasSkill(SkillType.S_TECH_BA);
        } else if (entity instanceof Tank) {
            return hasSkill(SkillType.S_TECH_MECHANIC);
        } else {
            return false;
        }
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
        return getTechUnits().stream().mapToInt(Unit::getMaintenanceTime).sum();
    }

    public boolean isMothballing() {
        return isTech() && techUnits.stream().anyMatch(Unit::isMothballing);
    }

    public @Nullable Unit getUnit() {
        return unit;
    }

    public void setUnit(final @Nullable Unit unit) {
        this.unit = unit;
    }

    public void removeTechUnit(final Unit unit) {
        techUnits.remove(unit);
    }

    public void addTechUnit(final Unit unit) {
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

    public void setMinutesLeft(final int minutesLeft) {
        this.minutesLeft = minutesLeft;
        if (engineer && (getUnit() != null)) {
            // set minutes for all crewmembers
            for (Person p : getUnit().getActiveCrew()) {
                p.setMinutesLeft(minutesLeft);
            }
        }
    }

    public int getOvertimeLeft() {
        return overtimeLeft;
    }

    public void setOvertimeLeft(final int overtimeLeft) {
        this.overtimeLeft = overtimeLeft;
        if (engineer && (getUnit() != null)) {
            // set minutes for all crewmembers
            for (Person p : getUnit().getActiveCrew()) {
                p.setMinutesLeft(overtimeLeft);
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
        int lvl = SkillType.EXP_NONE;
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

    public boolean isTaskOvertime(final IPartWork partWork) {
        return (partWork.getTimeLeft() > getMinutesLeft()) && (getOvertimeLeft() > 0);
    }

    public Skill getSkillForWorkingOn(final IPartWork part) {
        final Unit unit = part.getUnit();
        Skill skill = getSkillForWorkingOn(unit);
        if (skill != null) {
            return skill;
        }
        // check spare parts
        // return the best one
        if (part.isRightTechType(SkillType.S_TECH_MECH) && hasSkill(SkillType.S_TECH_MECH)) {
            skill = getSkill(SkillType.S_TECH_MECH);
        }

        if (part.isRightTechType(SkillType.S_TECH_BA) && hasSkill(SkillType.S_TECH_BA)) {
            if ((skill == null) || (skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_BA).getFinalSkillValue())) {
                skill = getSkill(SkillType.S_TECH_BA);
            }
        }

        if (part.isRightTechType(SkillType.S_TECH_AERO) && hasSkill(SkillType.S_TECH_AERO)) {
            if ((skill == null) || (skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_AERO).getFinalSkillValue())) {
                skill = getSkill(SkillType.S_TECH_AERO);
            }
        }

        if (part.isRightTechType(SkillType.S_TECH_MECHANIC) && hasSkill(SkillType.S_TECH_MECHANIC)) {
            if ((skill == null) || (skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_MECHANIC).getFinalSkillValue())) {
                skill = getSkill(SkillType.S_TECH_MECHANIC);
            }
        }

        if (part.isRightTechType(SkillType.S_TECH_VESSEL) && hasSkill(SkillType.S_TECH_VESSEL)) {
            if ((skill == null) || (skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_VESSEL).getFinalSkillValue())) {
                skill = getSkill(SkillType.S_TECH_VESSEL);
            }
        }

        if (skill != null) {
            return skill;
        }
        // if we are still here then we didn't have the right tech skill, so return the highest
        // of any tech skills that we do have
        if (hasSkill(SkillType.S_TECH_MECH)) {
            skill = getSkill(SkillType.S_TECH_MECH);
        }

        if (hasSkill(SkillType.S_TECH_BA)) {
            if ((skill == null) || (skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_BA).getFinalSkillValue())) {
                skill = getSkill(SkillType.S_TECH_BA);
            }
        }

        if (hasSkill(SkillType.S_TECH_MECHANIC)) {
            if ((skill == null) || (skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_MECHANIC).getFinalSkillValue())) {
                skill = getSkill(SkillType.S_TECH_MECHANIC);
            }
        }

        if (hasSkill(SkillType.S_TECH_AERO)) {
            if ((skill == null) || (skill.getFinalSkillValue() > getSkill(SkillType.S_TECH_AERO).getFinalSkillValue())) {
                skill = getSkill(SkillType.S_TECH_AERO);
            }
        }

        return skill;
    }

    public @Nullable Skill getSkillForWorkingOn(final @Nullable Unit unit) {
        if (unit == null) {
            return null;
        } else if (((unit.getEntity() instanceof Mech) || (unit.getEntity() instanceof Protomech))
                && hasSkill(SkillType.S_TECH_MECH)) {
            return getSkill(SkillType.S_TECH_MECH);
        } else if ((unit.getEntity() instanceof BattleArmor) && hasSkill(SkillType.S_TECH_BA)) {
            return getSkill(SkillType.S_TECH_BA);
        } else if ((unit.getEntity() instanceof Tank) && hasSkill(SkillType.S_TECH_MECHANIC)) {
            return getSkill(SkillType.S_TECH_MECHANIC);
        } else if (((unit.getEntity() instanceof Dropship) || (unit.getEntity() instanceof Jumpship))
                && hasSkill(SkillType.S_TECH_VESSEL)) {
            return getSkill(SkillType.S_TECH_VESSEL);
        } else if ((unit.getEntity() instanceof Aero) && !(unit.getEntity() instanceof Dropship)
                && !(unit.getEntity() instanceof Jumpship) && hasSkill(SkillType.S_TECH_AERO)) {
            return getSkill(SkillType.S_TECH_AERO);
        } else {
            return null;
        }
    }

    public @Nullable Skill getSkillForWorkingOn(final @Nullable String skillName) {
        if (CampaignOptions.S_TECH.equals(skillName)) {
            return getBestTechSkill();
        } else if (hasSkill(skillName)) {
            return getSkill(skillName);
        } else {
            return null;
        }
    }

    public int getBestTechLevel() {
        int level = SkillType.EXP_NONE;
        final Skill mechSkill = getSkill(SkillType.S_TECH_MECH);
        final Skill mechanicSkill = getSkill(SkillType.S_TECH_MECHANIC);
        final Skill baSkill = getSkill(SkillType.S_TECH_BA);
        final Skill aeroSkill = getSkill(SkillType.S_TECH_AERO);
        if ((mechSkill != null) && (mechSkill.getLevel() > level)) {
            level = mechSkill.getLevel();
        }

        if ((mechanicSkill != null) && (mechanicSkill.getLevel() > level)) {
            level = mechanicSkill.getLevel();
        }

        if ((baSkill != null) && (baSkill.getLevel() > level)) {
            level = baSkill.getLevel();
        }

        if ((aeroSkill != null) && (aeroSkill.getLevel() > level)) {
            level = aeroSkill.getLevel();
        }

        return level;
    }

    public boolean isRightTechTypeFor(final IPartWork part) {
        Unit unit = part.getUnit();
        if (unit == null) {
            return (hasSkill(SkillType.S_TECH_MECH) && part.isRightTechType(SkillType.S_TECH_MECH))
                    || (hasSkill(SkillType.S_TECH_AERO) && part.isRightTechType(SkillType.S_TECH_AERO))
                    || (hasSkill(SkillType.S_TECH_MECHANIC) && part.isRightTechType(SkillType.S_TECH_MECHANIC))
                    || (hasSkill(SkillType.S_TECH_BA) && part.isRightTechType(SkillType.S_TECH_BA))
                    || (hasSkill(SkillType.S_TECH_VESSEL) && part.isRightTechType(SkillType.S_TECH_VESSEL));
        } else if ((unit.getEntity() instanceof Mech) || (unit.getEntity() instanceof Protomech)) {
            return hasSkill(SkillType.S_TECH_MECH);
        } else if (unit.getEntity() instanceof BattleArmor) {
            return hasSkill(SkillType.S_TECH_BA);
        } else if ((unit.getEntity() instanceof Tank) || (unit.getEntity() instanceof Infantry)) {
            return hasSkill(SkillType.S_TECH_MECHANIC);
        } else if ((unit.getEntity() instanceof Dropship) || (unit.getEntity() instanceof Jumpship)) {
            return hasSkill(SkillType.S_TECH_VESSEL);
        } else if (unit.getEntity() instanceof Aero) {
            return hasSkill(SkillType.S_TECH_AERO);
        } else {
            return false;
        }
    }

    public @Nullable UUID getDoctorId() {
        return doctorId;
    }

    public int getToughness() {
        return toughness;
    }

    public void setToughness(final int toughness) {
        this.toughness = toughness;
    }

    public void resetSkillTypes() {
        skills.getSkills().forEach(Skill::updateType);
    }

    public int getNTasks() {
        return nTasks;
    }

    public void setNTasks(final int nTasks) {
        this.nTasks = nTasks;
    }

    public List<LogEntry> getPersonnelLog() {
        personnelLog.sort(Comparator.comparing(LogEntry::getDate));
        return personnelLog;
    }

    public List<LogEntry> getScenarioLog() {
        scenarioLog.sort(Comparator.comparing(LogEntry::getDate));
        return scenarioLog;
    }

    public void addLogEntry(final LogEntry entry) {
        personnelLog.add(entry);
    }

    public void addScenarioLogEntry(final LogEntry entry) {
        scenarioLog.add(entry);
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

    public void removeInjury(final Injury injury) {
        injuries.remove(injury);
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    public void diagnose(final Campaign campaign, final int hits) {
        InjuryUtil.resolveAfterCombat(campaign, this, hits);
        InjuryUtil.resolveCombatDamage(campaign, this, hits);
        setHits(0);
    }

    public int getAbilityTimeModifier(final Campaign campaign) {
        int modifier = 100;
        if (campaign.getCampaignOptions().isUseToughness()) {
            if (getToughness() == 1) {
                modifier -= 10;
            }
            if (getToughness() > 1) {
                modifier -= 15;
            }
        } // TODO: Fully implement this for advanced healing

        if (getOptions().booleanOption(OptionsConstants.MISC_PAIN_RESISTANCE)) {
            modifier -= 15;
        } else if (getOptions().booleanOption(OptionsConstants.MISC_IRON_MAN)) {
            modifier -= 10;
        }

        return modifier;
    }

    public boolean hasInjury(final BodyLocation location) {
        return getInjuryByLocation(location) != null;
    }

    public boolean needsAMFixing() {
        return !injuries.isEmpty()
                && injuries.stream().anyMatch(injury -> (injury.getTime() > 0) || !injury.isPermanent());
    }

    public int getPilotingInjuryMod() {
        return Modifier.calcTotalModifier(injuries.stream().flatMap(injury -> injury.getModifiers().stream()),
                ModifierValue.PILOTING);
    }

    public int getGunneryInjuryMod() {
        return Modifier.calcTotalModifier(injuries.stream().flatMap(injury -> injury.getModifiers().stream()),
                ModifierValue.GUNNERY);
    }

    public boolean hasInjuries(final boolean permanentCheck) {
        return !injuries.isEmpty() && (!permanentCheck
                || injuries.stream().anyMatch(injury -> !injury.isPermanent() || (injury.getTime() > 0)));
    }

    public boolean hasOnlyHealedPermanentInjuries() {
        return !injuries.isEmpty()
                && injuries.stream().noneMatch(injury -> !injury.isPermanent() || (injury.getTime() > 0));
    }

    public List<Injury> getInjuriesByLocation(final BodyLocation location) {
        return injuries.stream().filter(injury -> (injury.getLocation() == location)).collect(Collectors.toList());
    }

    // Returns only the first injury in a location
    public @Nullable Injury getInjuryByLocation(final BodyLocation location) {
        return injuries.stream().filter(injury -> (injury.getLocation() == location)).findFirst().orElse(null);
    }

    public void addInjury(final Injury injury) {
        injuries.add(Objects.requireNonNull(injury));
        if (getUnit() != null) {
            getUnit().resetPilotAndEntity();
        }
    }
    //endregion injuries

    /* For use by Against the Bot Employee Turnover rolls */
    public int getOriginalUnitWeight() {
        return originalUnitWeight;
    }

    public void setOriginalUnitWeight(final int originalUnitWeight) {
        this.originalUnitWeight = originalUnitWeight;
    }

    public int getOriginalUnitTech() {
        return originalUnitTech;
    }

    public void setOriginalUnitTech(final int originalUnitTech) {
        this.originalUnitTech = originalUnitTech;
    }

    public UUID getOriginalUnitId() {
        return originalUnitId;
    }

    public void setOriginalUnitId(final UUID originalUnitId) {
        this.originalUnitId = originalUnitId;
    }

    public void setOriginalUnit(final Unit unit) {
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
     * @param campaign the campaign the person is a part of
     * @param sharesForAll true if all combat and support personnel have shares, otherwise false if
     *                     just MechWarriors have shares
     * @return the number of shares the person has
     */
    public int getNumShares(final Campaign campaign, final boolean sharesForAll) {
        if (!getStatus().isActive() || !getPrisonerStatus().isFree()
                || (!sharesForAll && !hasRole(PersonnelRole.MECHWARRIOR))) {
            return 0;
        }
        int shares = 1;
        if (isFounder()) {
            shares++;
        }
        shares += Math.max(-1, getExperienceLevel(campaign, false) - 2);

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

        if (getOriginalUnitWeight() >= 3) {
            shares++;
        }
        shares += getOriginalUnitTech();

        return shares;
    }

    public boolean isEngineer() {
        return engineer;
    }

    public void setEngineer(final boolean engineer) {
        this.engineer = engineer;
    }

    /**
     * @param campaign the campaign to get the ransom value based on
     * @return the ransom value of this individual
     * Useful for prisoner who you want to ransom or hand off to your employer in an AtB context
     */
    public Money getRansomValue(final Campaign campaign) {
        // MechWarriors and aero pilots are worth more than the other types of scrubs
        return (getPrimaryRole().isMechWarriorGrouping() || getPrimaryRole().isAerospacePilot()
                ? MECHWARRIOR_AERO_RANSOM_VALUES : OTHER_RANSOM_VALUES)
                .get(getExperienceLevel(campaign, false));
    }

    public static class PersonUnitRef extends Unit {
        private PersonUnitRef(final UUID id) {
            setId(id);
        }
    }

    public void fixReferences(final Campaign campaign) {
        if (unit instanceof PersonUnitRef) {
            final UUID id = unit.getId();
            unit = campaign.getUnit(id);
            if (unit == null) {
                LogManager.getLogger().error(String.format("Person %s ('%s') references missing unit %s",
                        getId(), getFullName(), id));
            }
        }

        for (int ii = techUnits.size() - 1; ii >= 0; --ii) {
            final Unit techUnit = techUnits.get(ii);
            if (techUnit instanceof PersonUnitRef) {
                final Unit realUnit = campaign.getUnit(techUnit.getId());
                if (realUnit != null) {
                    techUnits.set(ii, realUnit);
                } else {
                    LogManager.getLogger().error(String.format("Person %s ('%s') techs missing unit %s",
                            getId(), getFullName(), techUnit.getId()));
                    techUnits.remove(ii);
                }
            }
        }
    }
}
