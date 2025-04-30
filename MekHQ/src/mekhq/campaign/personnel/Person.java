/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.personnel;

import static java.lang.Math.abs;
import static java.lang.Math.floor;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static megamek.codeUtilities.MathUtility.clamp;
import static megamek.common.Compute.randomInt;
import static megamek.common.enums.SkillLevel.REGULAR;
import static mekhq.campaign.log.LogEntryType.ASSIGNMENT;
import static mekhq.campaign.log.LogEntryType.MEDICAL;
import static mekhq.campaign.log.LogEntryType.PERFORMANCE;
import static mekhq.campaign.log.LogEntryType.SERVICE;
import static mekhq.campaign.personnel.PersonnelOptions.*;
import static mekhq.campaign.personnel.enums.BloodGroup.getRandomBloodGroup;
import static mekhq.campaign.personnel.skills.Aging.getReputationAgeModifier;
import static mekhq.campaign.personnel.skills.Attributes.DEFAULT_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MAXIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MINIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.SkillCheckUtility.determineTargetNumber;
import static mekhq.campaign.personnel.skills.SkillType.S_ADMIN;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_AERO;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_BA;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MECHANIC;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MEK;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_VESSEL;
import static mekhq.campaign.personnel.skills.SkillType.getType;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import megamek.Version;
import megamek.client.generator.RandomNameGenerator;
import megamek.codeUtilities.MathUtility;
import megamek.codeUtilities.StringUtility;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Portrait;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.ExtraData;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.event.PersonStatusChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.LogEntryFactory;
import mekhq.campaign.log.LogEntryType;
import mekhq.campaign.log.PersonalLogger;
import mekhq.campaign.log.ServiceLogger;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.personnel.enums.BloodGroup;
import mekhq.campaign.personnel.enums.ManeiDominiClass;
import mekhq.campaign.personnel.enums.ManeiDominiRank;
import mekhq.campaign.personnel.enums.ModifierValue;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.personnel.enums.ROMDesignation;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import mekhq.campaign.personnel.enums.education.EducationStage;
import mekhq.campaign.personnel.familyTree.Genealogy;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryUtil;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.Attributes;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.Skills;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.randomEvents.personalities.enums.Aggression;
import mekhq.campaign.randomEvents.personalities.enums.Ambition;
import mekhq.campaign.randomEvents.personalities.enums.Greed;
import mekhq.campaign.randomEvents.personalities.enums.PersonalityQuirk;
import mekhq.campaign.randomEvents.personalities.enums.Reasoning;
import mekhq.campaign.randomEvents.personalities.enums.Social;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.work.IPartWork;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 * @author Justin "Windchild" Bowen
 */
public class Person {
    // region Variable Declarations
    public static final Map<Integer, Money> MEKWARRIOR_AERO_RANSOM_VALUES;
    public static final Map<Integer, Money> OTHER_RANSOM_VALUES;

    // Traits
    public static final int TRAIT_MODIFICATION_COST = 100;

    public static final String CONNECTIONS_LABEL = "CONNECTIONS";
    public static final int MINIMUM_CONNECTIONS = 0;
    public static final int MAXIMUM_CONNECTIONS = 10;

    public static final String REPUTATION_LABEL = "REPUTATION";
    public static final int MINIMUM_REPUTATION = -5;
    public static final int MAXIMUM_REPUTATION = 5;

    public static final String WEALTH_LABEL = "WEALTH";
    public static final int MINIMUM_WEALTH = -1;
    public static final int MAXIMUM_WEALTH = 10;

    public static final String UNLUCKY_LABEL = "UNLUCKY";
    public static final int MINIMUM_UNLUCKY = 0;
    public static final int MAXIMUM_UNLUCKY = 5;


    private PersonAwardController awardController;

    // region Family Variables
    // Lineage
    private final Genealogy genealogy;

    // region Procreation
    private LocalDate dueDate;
    private LocalDate expectedDueDate;
    // endregion Procreation
    // endregion Family Variables

    private UUID id;

    // region Name
    private transient String fullName; // this is a runtime variable, and shouldn't be saved
    private String preNominal;
    private String givenName;
    private String surname;
    private String postNominal;
    private String maidenName;
    private String callsign;
    // endregion Name

    private Gender gender;
    private BloodGroup bloodGroup;

    private Portrait portrait;

    private PersonnelRole primaryRole;
    private PersonnelRole secondaryRole;

    private ROMDesignation primaryDesignator;
    private ROMDesignation secondaryDesignator;

    private String biography;
    private LocalDate birthday;
    private LocalDate joinedCampaign;
    private LocalDate recruitment;
    private LocalDate lastRankChangeDate;
    private LocalDate dateOfDeath;
    private List<LogEntry> personnelLog;
    private List<LogEntry> medicalLog;
    private List<LogEntry> scenarioLog;
    private List<LogEntry> assignmentLog;
    private List<LogEntry> performanceLog;

    // this is used by autoAwards to abstract the support person of the year award
    private int autoAwardSupportPoints;

    private LocalDate retirement;
    private int loyalty;
    private int fatigue;
    private Boolean isRecoveringFromFatigue;

    private Skills skills;
    private PersonnelOptions options;
    private int toughness;
    private int connections;
    private int wealth;
    private boolean hasPerformedExtremeExpenditure;
    private int reputation;
    private int unlucky;
    private Attributes atowAttributes;

    private PersonnelStatus status;
    private int xp;
    private int totalXPEarnings;
    private int acquisitions;
    private Money salary;
    private Money totalEarnings;
    private int hits;
    private int hitsPrior;
    private PrisonerStatus prisonerStatus;

    // Supports edge usage by a ship's engineer composite crewman
    private int edgeUsedThisRound;
    // To track how many edge points personnel have left until next refresh
    private int currentEdge;

    // phenotype and background
    private Phenotype phenotype;
    private String bloodname;
    private Faction originFaction;
    private Planet originPlanet;
    private LocalDate becomingBondsmanEndDate;

    // assignments
    private Unit unit;
    private UUID doctorId;
    private List<Unit> techUnits;

    private int vocationalXPTimer;

    // days of rest
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

    // region Advanced Medical
    private List<Injury> injuries;
    // endregion Advanced Medical

    // region Against the Bot
    private int originalUnitWeight; // uses EntityWeightClass with 0 (Extra-Light) for no original unit
    public static final int TECH_IS1 = 0;
    public static final int TECH_IS2 = 1;
    public static final int TECH_CLAN = 2;
    private int originalUnitTech;
    private UUID originalUnitId;
    // endregion Against the Bot

    // region Education
    private EducationLevel eduHighestEducation;
    private String eduAcademyName;
    private String eduAcademySet;
    private String eduAcademyNameInSet;
    private String eduAcademyFaction;
    private String eduAcademySystem;
    private int eduCourseIndex;
    private EducationStage eduEducationStage;
    private int eduJourneyTime;
    private int eduEducationTime;
    private int eduDaysOfTravel;
    private List<UUID> eduTagAlongs;
    private List<String> eduFailedApplications;
    // endregion Education

    // region Personality
    private Aggression aggression;
    private int aggressionDescriptionIndex;
    private Ambition ambition;
    private int ambitionDescriptionIndex;
    private Greed greed;
    private int greedDescriptionIndex;
    private Social social;
    private int socialDescriptionIndex;
    private PersonalityQuirk personalityQuirk;
    private int personalityQuirkDescriptionIndex;
    private Reasoning reasoning;
    private int reasoningDescriptionIndex;
    private String personalityDescription;
    // endregion Personality

    // region Flags
    private boolean clanPersonnel;
    private boolean commander;
    private boolean divorceable;
    private boolean founder; // +1 share if using shares system
    private boolean immortal;
    // this is a flag used in determine whether a person is a potential marriage
    // candidate provided
    // that they are not married, are old enough, etc.
    private boolean marriageable;
    // this is a flag used in random procreation to determine whether to attempt to
    // procreate
    private boolean tryingToConceive;
    // endregion Flags

    // Generic extra data, for use with plugins and mods
    private ExtraData extraData;

    private final static ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
          MekHQ.getMHQOptions().getLocale());
    private static final MMLogger logger = MMLogger.create(Person.class);

    // initializes the AtB ransom values
    static {
        MEKWARRIOR_AERO_RANSOM_VALUES = new HashMap<>();

        // no official AtB rules for really inexperienced scrubs, but...
        MEKWARRIOR_AERO_RANSOM_VALUES.put(SkillType.EXP_NONE, Money.of(2500));

        // no official AtB rules for really inexperienced scrubs, but...
        MEKWARRIOR_AERO_RANSOM_VALUES.put(SkillType.EXP_ULTRA_GREEN, Money.of(5000));

        MEKWARRIOR_AERO_RANSOM_VALUES.put(SkillType.EXP_GREEN, Money.of(10000));
        MEKWARRIOR_AERO_RANSOM_VALUES.put(SkillType.EXP_REGULAR, Money.of(25000));
        MEKWARRIOR_AERO_RANSOM_VALUES.put(SkillType.EXP_VETERAN, Money.of(50000));
        MEKWARRIOR_AERO_RANSOM_VALUES.put(SkillType.EXP_ELITE, Money.of(100000));

        OTHER_RANSOM_VALUES = new HashMap<>();
        OTHER_RANSOM_VALUES.put(SkillType.EXP_NONE, Money.of(1250));
        OTHER_RANSOM_VALUES.put(SkillType.EXP_ULTRA_GREEN, Money.of(2500));
        OTHER_RANSOM_VALUES.put(SkillType.EXP_GREEN, Money.of(5000));
        OTHER_RANSOM_VALUES.put(SkillType.EXP_REGULAR, Money.of(10000));
        OTHER_RANSOM_VALUES.put(SkillType.EXP_VETERAN, Money.of(25000));
        OTHER_RANSOM_VALUES.put(SkillType.EXP_ELITE, Money.of(50000));
    }
    // endregion Variable Declarations

    // region Constructors
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
        this(givenName, surname, campaign, campaign.getFaction().getShortName());
    }

    public Person(final String givenName, final String surname, final @Nullable Campaign campaign,
          final String factionCode) {
        this("", givenName, surname, "", campaign, factionCode);
    }

    /**
     * Primary Person constructor, variables are initialized in the exact same order as they are saved to the XML file
     *
     * @param preNominal  the person's pre-nominal
     * @param givenName   the person's given name
     * @param surname     the person's surname
     * @param postNominal the person's post-nominal
     * @param campaign    the campaign this person is a part of, or null (unit testing only)
     * @param factionCode the faction this person was borne into
     */
    public Person(final String preNominal, final String givenName, final String surname, final String postNominal,
          final @Nullable Campaign campaign, final String factionCode) {
        // We assign the variables in XML file order
        id = UUID.randomUUID();

        // region Name
        setPreNominalDirect(preNominal);
        setGivenNameDirect(givenName);
        setSurnameDirect(surname);
        setPostNominalDirect(postNominal);
        setMaidenName(null); // this is set to null to handle divorce cases
        setCallsignDirect("");
        // endregion Name

        primaryRole = PersonnelRole.NONE;
        secondaryRole = PersonnelRole.NONE;
        primaryDesignator = ROMDesignation.NONE;
        secondaryDesignator = ROMDesignation.NONE;
        setDateOfBirth(LocalDate.now());

        originFaction = Factions.getInstance().getFaction(factionCode);
        originPlanet = null;
        becomingBondsmanEndDate = null;
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
        hitsPrior = 0;
        toughness = 0;
        connections = 0;
        wealth = 0;
        hasPerformedExtremeExpenditure = false;
        reputation = 0;
        unlucky = 0;
        atowAttributes = new Attributes();
        dateOfDeath = null;
        recruitment = null;
        joinedCampaign = null;
        lastRankChangeDate = null;
        autoAwardSupportPoints = 0;
        retirement = null;
        loyalty = 9;
        fatigue = 0;
        isRecoveringFromFatigue = false;
        skills = new Skills();
        options = new PersonnelOptions();
        currentEdge = 0;
        techUnits = new ArrayList<>();
        personnelLog = new ArrayList<>();
        medicalLog = new ArrayList<>();
        scenarioLog = new ArrayList<>();
        assignmentLog = new ArrayList<>();
        performanceLog = new ArrayList<>();
        awardController = new PersonAwardController(this);
        injuries = new ArrayList<>();
        originalUnitWeight = EntityWeightClass.WEIGHT_ULTRA_LIGHT;
        originalUnitTech = TECH_IS1;
        originalUnitId = null;
        acquisitions = 0;
        eduHighestEducation = EducationLevel.EARLY_CHILDHOOD;
        eduAcademyName = null;
        eduAcademySystem = null;
        eduCourseIndex = 0;
        eduEducationStage = EducationStage.NONE;
        eduJourneyTime = 0;
        eduEducationTime = 0;
        eduDaysOfTravel = 0;
        eduTagAlongs = new ArrayList<>();
        eduFailedApplications = new ArrayList<>();
        eduAcademySet = null;
        eduAcademyNameInSet = null;
        eduAcademyFaction = null;
        aggression = Aggression.NONE;
        aggressionDescriptionIndex = randomInt(Aggression.MAXIMUM_VARIATIONS);
        ambition = Ambition.NONE;
        ambitionDescriptionIndex = randomInt(Ambition.MAXIMUM_VARIATIONS);
        greed = Greed.NONE;
        greedDescriptionIndex = randomInt(Greed.MAXIMUM_VARIATIONS);
        social = Social.NONE;
        socialDescriptionIndex = randomInt(Social.MAXIMUM_VARIATIONS);
        personalityQuirk = PersonalityQuirk.NONE;
        personalityQuirkDescriptionIndex = randomInt(PersonalityQuirk.MAXIMUM_VARIATIONS);
        reasoning = Reasoning.AVERAGE;
        reasoningDescriptionIndex = randomInt(Reasoning.MAXIMUM_VARIATIONS);
        personalityDescription = "";

        // This assigns minutesLeft and overtimeLeft. Must be after skills to avoid an NPE.
        if (campaign != null) {
            // The reason for this paranoid checking is to allow us to Unit Test with real Person objects without
            // needing
            // to initialize CampaignOptions
            CampaignOptions campaignOptions = campaign.getCampaignOptions();

            if (campaignOptions != null) {
                resetMinutesLeft(campaignOptions.isTechsUseAdministration());
            }
        }

        // region Flags
        setClanPersonnel(originFaction.isClan());
        setCommander(false);
        setDivorceable(true);
        setFounder(false);
        setImmortal(false);
        setMarriageable(true);
        setTryingToConceive(true);
        // endregion Flags

        extraData = new ExtraData();

        // Initialize Data based on these settings
        setFullName();
    }
    // endregion Constructors

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

    public LocalDate getBecomingBondsmanEndDate() {
        return becomingBondsmanEndDate;
    }

    public void setBecomingBondsmanEndDate(final LocalDate becomingBondsmanEndDate) {
        this.becomingBondsmanEndDate = becomingBondsmanEndDate;
    }

    public PrisonerStatus getPrisonerStatus() {
        return prisonerStatus;
    }

    /**
     * This requires expanded checks because a number of functionalities are strictly dependent on the current person's
     * prisoner status.
     *
     * @param campaign       the campaign the person is a part of
     * @param prisonerStatus The new prisoner status for the person in question
     * @param log            whether to log the change or not
     */
    public void setPrisonerStatus(final Campaign campaign, final PrisonerStatus prisonerStatus, final boolean log) {
        // This must be processed completely, as the unchanged prisoner status of Free
        // to Free is
        // used during recruitment

        final boolean freed = !getPrisonerStatus().isFree();
        final boolean isPrisoner = prisonerStatus.isCurrentPrisoner();
        setPrisonerStatusDirect(prisonerStatus);

        // Now, we need to fix values and ranks based on the Person's status
        switch (prisonerStatus) {
            case PRISONER:
            case PRISONER_DEFECTOR:
            case BECOMING_BONDSMAN:
                setRecruitment(null);
                setLastRankChangeDate(null);
                if (log) {
                    if (isPrisoner) {
                        ServiceLogger.madePrisoner(this, campaign.getLocalDate(), campaign.getName(), "");
                    } else {
                        ServiceLogger.madeBondsman(this, campaign.getLocalDate(), campaign.getName(), "");
                    }
                }
                break;
            case BONDSMAN:
                LocalDate today = campaign.getLocalDate();
                setRecruitment(today);
                setLastRankChangeDate(today);
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
                        ServiceLogger.freed(this, campaign.getLocalDate(), campaign.getName(), "");
                    } else {
                        ServiceLogger.joined(this, campaign.getLocalDate(), campaign.getName(), "");
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
     *
     * @param prisonerStatus the person's new prisoner status
     */
    public void setPrisonerStatusDirect(final PrisonerStatus prisonerStatus) {
        this.prisonerStatus = prisonerStatus;
    }

    // region Text Getters
    public String pregnancyStatus() {
        return isPregnant() ? " (Pregnant)" : "";
    }
    // endregion Text Getters

    // region Name

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
        setFullNameDirect(getFirstName() +
                                (getCallsign().isBlank() ? "" : (" \"" + getCallsign() + '"')) +
                                (lastName.isBlank() ? "" : ' ' + lastName));
    }

    /**
     * @param fullName this sets the full name to be equal to the input string. This can ONLY be called by
     *                 {@link Person#setFullName()} or its overrides.
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
     * Return a full last name which may be a bloodname or a surname with or without a post-nominal. A bloodname will
     * overrule a surname but we do not disallow surnames for clan personnel, if the player wants to input them
     *
     * @return a String of the person's last name
     */
    public String getLastName() {
        String lastName = !StringUtility.isNullOrBlank(getBloodname()) ?
                                getBloodname() :
                                !StringUtility.isNullOrBlank(getSurname()) ? getSurname() : "";
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
     * This method is used to migrate names from being a joined name to split between given name and surname, as part of
     * the Personnel changes in MekHQ 0.47.4, and is used to migrate from MM-style names to MHQ-style names
     *
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
        // Depending on the length of the resulting array, the name is processed
        // differently
        // Array of length 1: the name is assumed to not have a surname, just a given
        // name
        // Array of length 2: the name is assumed to be a given name and a surname
        // Array of length 3: the name is assumed to be a given name and two surnames
        // Array of length 4+: the name is assumed to be as many given names as possible
        // and two surnames
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
    // endregion Names

    public Portrait getPortrait() {
        return portrait;
    }

    public void setPortrait(final Portrait portrait) {
        this.portrait = Objects.requireNonNull(portrait, "Illegal assignment: cannot have a null Portrait");
    }

    // region Personnel Roles
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
        if ((primaryRole == getSecondaryRole()) ||
                  primaryRole.isDependent() ||
                  (primaryRole.isTech() && getSecondaryRole().isAstech()) ||
                  (primaryRole.isAstech() && getSecondaryRole().isTechSecondary()) ||
                  (primaryRole.isMedicalStaff() && getSecondaryRole().isMedic()) ||
                  (primaryRole.isMedic() && getSecondaryRole().isMedicalStaff())) {
            setSecondaryRoleDirect(PersonnelRole.NONE);
        }

        // Now, we can perform the time in service and last rank change tracking change
        // for dependents
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
     * This is used to determine if a person has a specific role as either their primary OR their secondary role
     *
     * @param role the role to determine
     *
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
     *
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
        return bgPrefix + getPrimaryRole().getLabel(isClanPersonnel());
    }

    public String getSecondaryRoleDesc() {
        return getSecondaryRole().getLabel(isClanPersonnel());
    }

    public boolean canPerformRole(LocalDate today, final PersonnelRole role, final boolean primary) {
        if (primary) {
            // Primary Role:
            // We only do a few here, as it is better on the UX-side to correct the issues when assigning the primary
            // role
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
            } else if (role.isDependent() ||
                             getPrimaryRole().isDependent() ||
                             (getPrimaryRole() == role) ||
                             (role.isTechSecondary() && getPrimaryRole().isAstech()) ||
                             (role.isAstech() && getPrimaryRole().isTech()) ||
                             (role.isMedicalStaff() && getPrimaryRole().isMedic()) ||
                             (role.isMedic() && getPrimaryRole().isMedicalStaff())) {
                return false;
            }
        }

        if (isChild(today)) {
            return false;
        }

        return switch (role) {
            case MEKWARRIOR -> hasSkill(SkillType.S_GUN_MEK) && hasSkill(SkillType.S_PILOT_MEK);
            case LAM_PILOT ->
                  Stream.of(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK, SkillType.S_GUN_AERO, SkillType.S_PILOT_AERO)
                        .allMatch(this::hasSkill);
            case GROUND_VEHICLE_DRIVER -> hasSkill(SkillType.S_PILOT_GVEE);
            case NAVAL_VEHICLE_DRIVER -> hasSkill(SkillType.S_PILOT_NVEE);
            case VTOL_PILOT -> hasSkill(SkillType.S_PILOT_VTOL);
            case VEHICLE_GUNNER -> hasSkill(SkillType.S_GUN_VEE);
            case MECHANIC -> hasSkill(S_TECH_MECHANIC);
            case VEHICLE_CREW -> Stream.of(S_TECH_MEK, S_TECH_AERO, S_TECH_MECHANIC, S_TECH_BA,
                  SkillType.S_DOCTOR,
                  SkillType.S_MEDTECH,
                  SkillType.S_ASTECH).anyMatch(this::hasSkill);
            case AEROSPACE_PILOT -> hasSkill(SkillType.S_GUN_AERO) && hasSkill(SkillType.S_PILOT_AERO);
            case CONVENTIONAL_AIRCRAFT_PILOT -> hasSkill(SkillType.S_GUN_JET) && hasSkill(SkillType.S_PILOT_JET);
            case PROTOMEK_PILOT -> hasSkill(SkillType.S_GUN_PROTO);
            case BATTLE_ARMOUR -> hasSkill(SkillType.S_GUN_BA);
            case SOLDIER -> hasSkill(SkillType.S_SMALL_ARMS);
            case VESSEL_PILOT -> hasSkill(SkillType.S_PILOT_SPACE);
            case VESSEL_CREW -> hasSkill(S_TECH_VESSEL);
            case VESSEL_GUNNER -> hasSkill(SkillType.S_GUN_SPACE);
            case VESSEL_NAVIGATOR -> hasSkill(SkillType.S_NAV);
            case MEK_TECH -> hasSkill(S_TECH_MEK);
            case AERO_TEK -> hasSkill(S_TECH_AERO);
            case BA_TECH -> hasSkill(S_TECH_BA);
            case ASTECH -> hasSkill(SkillType.S_ASTECH);
            case DOCTOR -> hasSkill(SkillType.S_DOCTOR);
            case MEDIC -> hasSkill(SkillType.S_MEDTECH);
            case ADMINISTRATOR_COMMAND, ADMINISTRATOR_LOGISTICS, ADMINISTRATOR_TRANSPORT, ADMINISTRATOR_HR ->
                  hasSkill(S_ADMIN);
            case DEPENDENT, NONE -> true;
        };
    }
    // endregion Personnel Roles

    public PersonnelStatus getStatus() {
        return status;
    }

    /**
     * This is used to change the person's PersonnelStatus
     *
     * @param campaign the campaign the person is part of
     * @param today    the current date
     * @param status   the person's new PersonnelStatus
     */
    public void changeStatus(final Campaign campaign, final LocalDate today, final PersonnelStatus status) {
        if (status == getStatus()) { // no change means we don't need to process anything
            return;
        } else if (getStatus().isDead() && !status.isDead()) {
            // remove date of death for resurrection
            setDateOfDeath(null);
            campaign.addReport(String.format(resources.getString("resurrected.report"), getHyperlinkedFullTitle()));
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
                } else if (getStatus().isOnLeave() || getStatus().isOnMaternityLeave()) {
                    campaign.addReport(String.format(resources.getString("returnedFromLeave.report"),
                          getHyperlinkedFullTitle()));
                    ServiceLogger.returnedFromLeave(this, campaign.getLocalDate());
                } else if (getStatus().isStudent()) {
                    campaign.addReport(String.format(resources.getString("returnedFromEducation.report"),
                          getHyperlinkedFullTitle()));
                    ServiceLogger.returnedFromEducation(this, campaign.getLocalDate());
                } else if (getStatus().isMissing()) {
                    campaign.addReport(String.format(resources.getString("returnedFromMissing.report"),
                          getHyperlinkedFullTitle()));
                    ServiceLogger.returnedFromMissing(this, campaign.getLocalDate());
                } else if (getStatus().isAwol()) {
                    campaign.addReport(String.format(resources.getString("returnedFromAWOL.report"),
                          getHyperlinkedFullTitle()));
                    ServiceLogger.returnedFromAWOL(this, campaign.getLocalDate());
                } else {
                    campaign.addReport(String.format(resources.getString("rehired.report"), getHyperlinkedFullTitle()));
                    ServiceLogger.rehired(this, today);
                }
                setRetirement(null);
                break;
            case RETIRED:
                campaign.addReport(String.format(status.getReportText(), getHyperlinkedFullTitle()));
                ServiceLogger.retired(this, today);

                setRetirement(today);

                break;
            case RESIGNED:
                campaign.addReport(String.format(status.getReportText(), getHyperlinkedFullTitle()));
                ServiceLogger.resigned(this, today);

                setRetirement(today);

                break;
            case DESERTED:
                campaign.addReport(String.format(status.getReportText(), getHyperlinkedFullTitle()));
                ServiceLogger.deserted(this, today);

                setRetirement(today);

                break;
            case DEFECTED:
                campaign.addReport(String.format(status.getReportText(), getHyperlinkedFullTitle()));
                ServiceLogger.defected(this, today);

                setRetirement(today);

                break;
            case SACKED:
                campaign.addReport(String.format(status.getReportText(), getHyperlinkedFullTitle()));
                ServiceLogger.sacked(this, today);

                setRetirement(today);

                break;
            case LEFT:
                campaign.addReport(String.format(status.getReportText(), getHyperlinkedFullTitle()));
                ServiceLogger.left(this, today);

                setRetirement(today);

                break;
            case STUDENT:
                // log entries and reports are handled by the education package
                // (mekhq/campaign/personnel/education)
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

            if ((genealogy.hasSpouse()) && (!genealogy.getSpouse().getStatus().isDead())) {
                campaign.getDivorce().widowed(campaign, campaign.getLocalDate(), this);
            }

            // log death across genealogy
            if (genealogy.hasChildren()) {
                for (Person child : genealogy.getChildren()) {
                    if (!child.getStatus().isDead()) {
                        if (!child.getGenealogy().hasLivingParents()) {
                            ServiceLogger.orphaned(child, campaign.getLocalDate());
                        } else if (child.getGenealogy().hasLivingParents()) {
                            PersonalLogger.RelativeHasDied(child,
                                  this,
                                  resources.getString("relationParent.text"),
                                  campaign.getLocalDate());
                        }
                    }
                }
            }

            if (genealogy.hasLivingParents()) {
                for (Person parent : genealogy.getParents()) {
                    if (!parent.getStatus().isDead()) {
                        PersonalLogger.RelativeHasDied(parent,
                              this,
                              resources.getString("relationChild.text"),
                              campaign.getLocalDate());
                    }
                }
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

        // release the commander flag.
        if ((isCommander()) && (status.isDepartedUnit())) {
            if ((!status.isResigned()) && (!status.isRetired())) {
                leadershipMassChangeLoyalty(campaign);
            }

            setCommander(false);
        }

        // clean up the save entry
        this.setEduAcademyName(null);
        this.setEduAcademyFaction(null);
        this.setEduAcademySet(null);
        this.setEduAcademyNameInSet(null);
        this.setEduAcademySystem(null);
        this.setEduCourseIndex(0);
        this.setEduEducationStage(EducationStage.NONE);
        this.setEduEducationTime(0);
        this.setEduJourneyTime(0);
        this.setEduDaysOfTravel(0);

        for (UUID tagAlongId : eduTagAlongs) {
            Person tagAlong = campaign.getPerson(tagAlongId);
            tagAlong.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ACTIVE);
        }
        this.setEduTagAlongs(new ArrayList<>());

        MekHQ.triggerEvent(new PersonStatusChangedEvent(this));
    }

    /**
     * If the current character is the campaign commander, adjust loyalty across the entire unit.
     *
     * @param campaign The current campaign
     */
    private void leadershipMassChangeLoyalty(Campaign campaign) {
        for (Person person : campaign.getPersonnel()) {
            if (person.getStatus().isDepartedUnit()) {
                continue;
            }

            if (person.getPrisonerStatus().isCurrentPrisoner()) {
                continue;
            }

            person.performRandomizedLoyaltyChange(campaign, false, false);
        }

        if (campaign.getCampaignOptions().isUseLoyaltyModifiers()) {
            campaign.addReport(String.format(resources.getString("loyaltyChangeGroup.text"),
                  "<span color=" + MekHQ.getMHQOptions().getFontColorWarningHexColor() + "'>",
                  ReportingUtilities.CLOSING_SPAN_TAG));
        }
    }

    /**
     * Performs a randomized loyalty change for an individual
     *
     * @param campaign  The current campaign
     * @param isMajor   Flag to indicate if the loyalty change is major.
     * @param isVerbose Flag to indicate if the change should be individually posted to the campaign report.
     */
    public void performRandomizedLoyaltyChange(Campaign campaign, boolean isMajor, boolean isVerbose) {
        int originalLoyalty = loyalty;

        Consumer<Integer> applyLoyaltyChange = (roll) -> {
            switch (roll) {
                case 1, 2, 3 -> changeLoyalty(-3);
                case 4 -> changeLoyalty(-2);
                case 5, 6 -> changeLoyalty(-1);
                case 15, 16 -> changeLoyalty(1);
                case 17 -> changeLoyalty(2);
                case 18 -> changeLoyalty(3);
                default -> {
                }
            }
        };

        int roll = Compute.d6(3);
        int secondRoll = Compute.d6(3);

        // if this is a major change, we use whichever result is furthest from the
        // midpoint (9)
        if (isMajor) {
            roll = abs(roll - 9) > abs(secondRoll - 9) ? roll : secondRoll;
        }

        applyLoyaltyChange.accept(roll);

        if (isVerbose && originalLoyalty != loyalty) {
            reportLoyaltyChange(campaign, originalLoyalty);
        }
    }

    /**
     * Performs a loyalty change where the results will always be neutral or positive, or neutral or negative.
     *
     * @param campaign   the current campaign
     * @param isPositive a boolean indicating whether the loyalty change should be positive or negative
     * @param isMajor    a boolean indicating whether a major loyalty change should be performed in addition to the
     *                   initial change
     * @param isVerbose  a boolean indicating whether the method should generate a report if the loyalty has changed
     */
    public void performForcedDirectionLoyaltyChange(Campaign campaign, boolean isPositive, boolean isMajor,
          boolean isVerbose) {
        int originalLoyalty = loyalty;

        Consumer<Integer> applyLoyaltyChange = (roll) -> {
            int changeValue = switch (roll) {
                case 1, 2, 3, 18 -> 3;
                case 4, 17 -> 2;
                case 5, 6, 15, 16 -> 1;
                default -> 0;
            };

            if (changeValue > 0) {
                changeLoyalty(isPositive ? changeValue : -changeValue);
            }
        };

        applyLoyaltyChange.accept(Compute.d6(3));

        if (isMajor) {
            applyLoyaltyChange.accept(Compute.d6(3));
        }

        if ((isVerbose) && (originalLoyalty != loyalty)) {
            reportLoyaltyChange(campaign, originalLoyalty);
        }
    }

    /**
     * Reports the change in loyalty.
     *
     * @param campaign        The campaign for which the loyalty change is being reported.
     * @param originalLoyalty The original loyalty value before the change.
     */
    private void reportLoyaltyChange(Campaign campaign, int originalLoyalty) {
        if (!campaign.getCampaignOptions().isUseLoyaltyModifiers()) {
            return;
        }

        StringBuilder changeString = new StringBuilder();
        String color;

        // choose the color and string based on the loyalty comparison.
        if (originalLoyalty > loyalty) {
            color = MekHQ.getMHQOptions().getFontColorNegativeHexColor();
            changeString.append(resources.getString("loyaltyChangeNegative.text"));
        } else {
            color = MekHQ.getMHQOptions().getFontColorPositiveHexColor();
            changeString.append(resources.getString("loyaltyChangePositive.text"));
        }

        String report = String.format(resources.getString("loyaltyChangeReport.text"),
              getHyperlinkedFullTitle(),
              "<span color=" + color + "'>",
              changeString,
              ReportingUtilities.CLOSING_SPAN_TAG);

        campaign.addReport(report);
    }

    /**
     * This is used to directly set the Person's PersonnelStatus without any processing
     *
     * @param status the person's new status
     */
    public void setStatus(final PersonnelStatus status) {
        this.status = status;
    }

    public int getVocationalXPTimer() {
        return vocationalXPTimer;
    }

    public void setVocationalXPTimer(final int vocationalXPTimer) {
        this.vocationalXPTimer = vocationalXPTimer;
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

    public void setBloodGroup(final BloodGroup bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    /**
     * Retrieves the blood group of the person. If the blood group has not been set, it generates a random blood group
     * using {@link BloodGroup#getRandomBloodGroup()}.
     *
     * @return The {@link BloodGroup} of the entity. If no blood group is previously assigned, a random one is generated
     *       and returned.
     */
    public BloodGroup getBloodGroup() {
        if (bloodGroup == null) {
            bloodGroup = getRandomBloodGroup();
        }

        return bloodGroup;
    }

    /**
     * Sets the date of birth (the date they are born) for the person.
     *
     * @param birthday the person's new date of birth
     */
    public void setDateOfBirth(final LocalDate birthday) {
        this.birthday = birthday;
    }

    /**
     * Returns the date a person was born.
     *
     * @return a LocalDate representing the person's date of birth
     */
    public LocalDate getDateOfBirth() {
        return birthday;
    }

    /**
     * Retrieves the birthday for a person, with the year set to the same as the provided year.
     *
     * @param currentYear the current in-game year
     *
     * @return the birthday with the year updated to match the provided year
     */
    public LocalDate getBirthday(int currentYear) {
        return birthday.withYear(currentYear);
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
            // use date of death instead of birthday
            today = getDateOfDeath();
        }

        return Math.toIntExact(ChronoUnit.YEARS.between(getDateOfBirth(), today));
    }

    public @Nullable LocalDate getJoinedCampaign() {
        return joinedCampaign;
    }

    public void setJoinedCampaign(final @Nullable LocalDate joinedCampaign) {
        this.joinedCampaign = joinedCampaign;
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
            // use "" they haven't been recruited or are dependents
            return "";
        }

        LocalDate today = campaign.getLocalDate();

        // If the person is dead, we only care about how long they spent in service to
        // the company
        if (getDateOfDeath() != null) {
            // use date of death instead of the current day
            today = getDateOfDeath();
        }

        return campaign.getCampaignOptions()
                     .getTimeInServiceDisplayFormat()
                     .getDisplayFormattedOutput(getRecruitment(), today);
    }

    /**
     * @param campaign the current Campaign
     *
     * @return how many years a character has spent employed in the campaign, factoring in date of death and retirement
     */
    public long getYearsInService(final Campaign campaign) {
        // Get time in service based on year
        if (getRecruitment() == null) {
            return 0;
        }

        LocalDate today = campaign.getLocalDate();

        // If the person is dead or has left the unit, we only care about how long they
        // spent in service to the company
        if (getRetirement() != null) {
            today = getRetirement();
        } else if (getDateOfDeath() != null) {
            today = getDateOfDeath();
        }

        return ChronoUnit.YEARS.between(getRecruitment(), today);
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

        // If the person is dead, we only care about how long it was from their last
        // promotion till they died
        if (getDateOfDeath() != null) {
            // use date of death instead of the current day
            today = getDateOfDeath();
        }

        return campaign.getCampaignOptions()
                     .getTimeInRankDisplayFormat()
                     .getDisplayFormattedOutput(getLastRankChangeDate(), today);
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    /**
     * Checks if the person is considered a child based on their age and today's date.
     *
     * <p>This method uses the default context where the person is not being checked
     * for procreation-specific thresholds.</p>
     *
     * @param today the current date to calculate the age against
     *
     * @return {@code true} if the person's age is less than 16; {@code false} otherwise
     */
    public boolean isChild(final LocalDate today) {
        return isChild(today, false);
    }

    /**
     * Checks if the person is considered a child based on their age, today's date, and procreation status.
     *
     * @param today the current date to calculate the age against
     * @param use18 if {@code true}, the threshold considers a person a child if their age is less than 18; otherwise,
     *              the default age threshold of 16 applies
     *
     * @return {@code true} if the person's age is less than the specified threshold (procreation or default),
     *       {@code false} otherwise
     */
    public boolean isChild(final LocalDate today, boolean use18) {
        int age = getAge(today);
        return age < (use18 ? 18 : 16);
    }

    public Genealogy getGenealogy() {
        return genealogy;
    }

    // region autoAwards
    public int getAutoAwardSupportPoints() {
        return autoAwardSupportPoints;
    }

    public void setAutoAwardSupportPoints(final int autoAwardSupportPoints) {
        this.autoAwardSupportPoints = autoAwardSupportPoints;
    }

    public void changeAutoAwardSupportPoints(int change) {
        autoAwardSupportPoints += change;
    }
    // endregion autoAwards

    // region Turnover and Retention
    public @Nullable LocalDate getRetirement() {
        return retirement;
    }

    public void setRetirement(final @Nullable LocalDate retirement) {
        this.retirement = retirement;
    }

    public int getLoyalty() {
        return loyalty;
    }

    public void setLoyalty(int loyalty) {
        this.loyalty = loyalty;
    }

    /**
     * Changes the loyalty value for the current person by the specified amount. Positive values increase loyalty, while
     * negative values decrease loyalty.
     *
     * @param change The amount to change the loyalty value by.
     */
    public void changeLoyalty(int change) {
        this.loyalty += change;
    }

    /**
     * @param loyaltyModifier the loyalty modifier
     *
     * @return the name corresponding to an individual's loyalty modifier.
     *
     * @throws IllegalStateException if an unexpected value is passed for loyaltyModifier
     */
    public static String getLoyaltyName(int loyaltyModifier) {
        return switch (loyaltyModifier) {
            case -3 -> "Devoted";
            case -2 -> "Loyal";
            case -1 -> "Reliable";
            case 0 -> "Neutral";
            case 1 -> "Unreliable";
            case 2 -> "Disloyal";
            case 3 -> "Treacherous";
            default -> throw new IllegalStateException(
                  "Unexpected value in mekhq/campaign/personnel/Person.java/getLoyaltyName: " + loyaltyModifier);
        };
    }

    public int getFatigue() {
        return fatigue;
    }

    public void setFatigue(final int fatigue) {
        this.fatigue = fatigue;
    }

    /**
     * Adjusts the current fatigue level by the specified amount, applying an SPA fatigue multiplier where applicable.
     *
     * <p>This method modifies the fatigue level based on the given {@code delta} value. Positive values, which
     * indicate an increase in fatigue, are scaled by the result of {@link #getFatigueMultiplier()} and rounded down
     * using {@link Math#floor(double)} to ensure consistent results. Negative values, which indicate a reduction in
     * fatigue, are applied directly without modification.</p>
     *
     * @param delta The amount to adjust the fatigue by. Positive values represent fatigue gain and are scaled by the
     *              fatigue multiplier, while negative values represent fatigue reduction and are applied as-is.
     */
    public void changeFatigue(int delta) {
        if (delta > 0) {
            // Only fatigue gain is modified by SPAs, not reduction.
            delta = (int) floor(delta * getFatigueMultiplier());
        }

        this.fatigue = this.fatigue + delta;
    }

    public boolean getIsRecoveringFromFatigue() {
        return isRecoveringFromFatigue;
    }

    public void setIsRecoveringFromFatigue(final boolean isRecoveringFromFatigue) {
        this.isRecoveringFromFatigue = isRecoveringFromFatigue;
    }

    /**
     * Calculates the fatigue multiplier for a character based on their traits and fitness-related options.
     *
     * <p>The calculation is influenced by the following conditions:</p>
     * <ul>
     *     <li><b>{@code FLAW_GLASS_JAW}</b>: If set, increases the multiplier by 1.</li>
     *     <li><b>{@code ATOW_TOUGHNESS}</b>: If set, decreases the multiplier by 1.</li>
     *     <li>Both {@code FLAW_GLASS_JAW} and {@code ATOW_TOUGHNESS} cannot modify the multiplier if both are
     *     present, as they cancel each other out.</li>
     *     <li><b>{@code ATOW_FIT}</b>: If set, decreases the multiplier by 1.</li>
     *     <li><b>{@code FLAW_UNFIT}</b>: If set, increases the multiplier by 1.</li>
     *     <li>Both {@code ATOW_FIT} and {@code FLAW_UNFIT}, when present simultaneously, cancel each other out and
     *     do not affect the multiplier.</li>
     * </ul>
     *
     * <p>After calculating the initial multiplier, the following adjustments are applied:</p>
     * <ul>
     *     <li>If the resulting multiplier equals {@code 0}, it is set to {@code 0.5} to avoid zeroing Fatigue.</li>
     *     <li>If the resulting multiplier is less than {@code 0}, it is set to a minimum value of {@code 0.25}.</li>
     * </ul>
     *
     * @return the calculated fatigue multiplier, adjusted based on the character's traits and options
     *
     * @author Illiani
     * @since 0.50.05
     */
    private double getFatigueMultiplier() {
        double fatigueMultiplier = 1;

        // Glass Jaw and Toughness
        boolean hasGlassJaw = options.booleanOption(FLAW_GLASS_JAW);
        boolean hasToughness = options.booleanOption(ATOW_TOUGHNESS);
        boolean modifyForGlassJawToughness = !(hasGlassJaw && hasToughness);

        if (modifyForGlassJawToughness) {
            fatigueMultiplier += (hasGlassJaw ? 1 : 0);
            fatigueMultiplier -= (hasToughness ? 1 : 0);
        }

        // Fit and Unfit
        boolean hasFit = options.booleanOption(ATOW_FIT);
        boolean hasUnfit = options.booleanOption(FLAW_UNFIT);
        boolean modifyForFitness = !(hasFit && hasUnfit);

        if (modifyForFitness) {
            fatigueMultiplier += (hasUnfit ? 1 : 0);
            fatigueMultiplier -= (hasFit ? 1 : 0);
        }

        // Conclusion
        if (fatigueMultiplier == 0) {
            fatigueMultiplier = 0.5;
        } else if (fatigueMultiplier < 0) {
            fatigueMultiplier = 0.25;
        }

        return fatigueMultiplier;
    }
    // region Turnover and Retention

    // region Pregnancy
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
        final LocalDate date = campaign.getCampaignOptions().isDisplayTrueDueDate() ?
                                     getDueDate() :
                                     getExpectedDueDate();
        return (date == null) ? "" : MekHQ.getMHQOptions().getDisplayFormattedDate(date);
    }

    public boolean isPregnant() {
        return dueDate != null;
    }
    // endregion Pregnancy

    // region Experience

    /**
     * @return the current experience points (XP) of the character.
     */
    public int getXP() {
        return xp;
    }

    /**
     * Awards experience points (XP) to the character and optionally tracks the total XP earnings if enabled.
     *
     * <p>This method increments the current XP by the specified amount and, if the campaign
     * option for tracking total XP earnings is enabled, updates the total XP earnings as well.</p>
     *
     * @param campaign the {@link Campaign} instance providing the campaign options
     * @param xp       the amount of XP to be awarded
     */
    public void awardXP(final Campaign campaign, final int xp) {
        this.xp += xp;
        if (campaign.getCampaignOptions().isTrackTotalXPEarnings()) {
            changeTotalXPEarnings(xp);
        }
    }

    /**
     * Spends (deducts) experience points (XP) from the character's current XP total.
     *
     * <p>This method decrements the current XP by the specified amount.</p>
     *
     * @param xp the amount of XP to deduct
     */
    public void spendXP(final int xp) {
        this.xp -= xp;
    }

    /**
     * Sets the current experience points (XP) for the character and optionally tracks the adjustment in total XP
     * earnings if enabled.
     *
     * <p>This method updates the current XP to the specified value. If the campaign option for tracking total XP
     * earnings is enabled, it also calculates and updates the total XP earnings based on the difference between the new
     * and current XP values.</p>
     *
     * @param campaign the {@link Campaign} instance providing the campaign options
     * @param xp       the new XP value to set
     */
    public void setXP(final Campaign campaign, final int xp) {
        if (campaign.getCampaignOptions().isTrackTotalXPEarnings()) {
            changeTotalXPEarnings(xp - getXP());
        }
        setXPDirect(xp);
    }

    /**
     * Directly sets the experience points (XP) for the entity without adjusting total XP earnings tracking.
     *
     * <p>This method updates the XP value directly, bypassing any optional campaign-related tracking logic.</p>
     *
     * <p><b>Usage:</b> Generally this should only be used in special circumstances, as it bypasses the tracking of
     * experience point gains. For most use cases {@code #awardXP()} or {@code #setXP()} are preferred.</p>
     *
     * @param xp the new XP value to set
     */
    public void setXPDirect(final int xp) {
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
    // endregion Experience

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

    public EducationLevel getEduHighestEducation() {
        return eduHighestEducation;
    }

    public void setEduHighestEducation(final EducationLevel eduHighestEducation) {
        this.eduHighestEducation = eduHighestEducation;
    }

    public int getEduJourneyTime() {
        return eduJourneyTime;
    }

    public void setEduJourneyTime(final int eduJourneyTime) {
        this.eduJourneyTime = eduJourneyTime;
    }

    public int getEduDaysOfTravel() {
        return eduDaysOfTravel;
    }

    public void setEduDaysOfTravel(final int eduDaysOfTravel) {
        this.eduDaysOfTravel = eduDaysOfTravel;
    }

    public List<UUID> getEduTagAlongs() {
        return eduTagAlongs;
    }

    public void setEduTagAlongs(final List<UUID> eduTagAlongs) {
        this.eduTagAlongs = eduTagAlongs;
    }

    public void addEduTagAlong(final UUID tagAlong) {
        this.eduTagAlongs.add(tagAlong);
    }

    public List<String> getEduFailedApplications() {
        return eduFailedApplications;
    }

    public void addEduFailedApplications(final String failedApplication) {
        eduFailedApplications.add(failedApplication);
    }

    /**
     * Increments the number educational travel days by 1.
     */
    public void incrementEduDaysOfTravel() {
        this.eduDaysOfTravel++;
    }

    public int getEduEducationTime() {
        return eduEducationTime;
    }

    public void setEduEducationTime(final int eduEducationTime) {
        this.eduEducationTime = eduEducationTime;
    }

    public String getEduAcademySystem() {
        return eduAcademySystem;
    }

    public void setEduAcademySystem(final String eduAcademySystem) {
        this.eduAcademySystem = eduAcademySystem;
    }

    public String getEduAcademyNameInSet() {
        return eduAcademyNameInSet;
    }

    public void setEduAcademyNameInSet(final String eduAcademyNameInSet) {
        this.eduAcademyNameInSet = eduAcademyNameInSet;
    }

    public String getEduAcademyFaction() {
        return eduAcademyFaction;
    }

    public void setEduAcademyFaction(final String eduAcademyFaction) {
        this.eduAcademyFaction = eduAcademyFaction;
    }

    public Integer getEduCourseIndex() {
        return eduCourseIndex;
    }

    public void setEduCourseIndex(final Integer eduCourseIndex) {
        this.eduCourseIndex = eduCourseIndex;
    }

    public EducationStage getEduEducationStage() {
        return eduEducationStage;
    }

    public void setEduEducationStage(final EducationStage eduEducationStage) {
        this.eduEducationStage = eduEducationStage;
    }

    public String getEduAcademyName() {
        return eduAcademyName;
    }

    public void setEduAcademyName(final String eduAcademyName) {
        this.eduAcademyName = eduAcademyName;
    }

    public void setEduAcademySet(final String eduAcademySet) {
        this.eduAcademySet = eduAcademySet;
    }

    public String getEduAcademySet() {
        return eduAcademySet;
    }

    public Aggression getAggression() {
        return aggression;
    }

    public void setAggression(final Aggression aggression) {
        this.aggression = aggression;
    }

    public int getAggressionDescriptionIndex() {
        return aggressionDescriptionIndex;
    }

    /**
     * Sets the index value for the {@link Aggression} description.
     *
     * @param aggressionDescriptionIndex The index value to set for the aggression description. It will be clamped to
     *                                   ensure it remains within the valid range.
     */
    public void setAggressionDescriptionIndex(final int aggressionDescriptionIndex) {
        this.aggressionDescriptionIndex = clamp(aggressionDescriptionIndex, 0, Aggression.MAXIMUM_VARIATIONS - 1);
    }

    public Ambition getAmbition() {
        return ambition;
    }

    public void setAmbition(final Ambition ambition) {
        this.ambition = ambition;
    }

    public int getAmbitionDescriptionIndex() {
        return ambitionDescriptionIndex;
    }

    /**
     * Sets the index value for the {@link Ambition} description.
     *
     * @param ambitionDescriptionIndex The index value to set for the Ambition description. It will be clamped to ensure
     *                                 it remains within the valid range.
     */
    public void setAmbitionDescriptionIndex(final int ambitionDescriptionIndex) {
        this.ambitionDescriptionIndex = clamp(ambitionDescriptionIndex, 0, Ambition.MAXIMUM_VARIATIONS - 1);
    }

    public Greed getGreed() {
        return greed;
    }

    public void setGreed(final Greed greed) {
        this.greed = greed;
    }

    public int getGreedDescriptionIndex() {
        return greedDescriptionIndex;
    }

    /**
     * Sets the index value for the {@link Greed} description.
     *
     * @param greedDescriptionIndex The index value to set for the Greed description. It will be clamped to ensure it
     *                              remains within the valid range.
     */
    public void setGreedDescriptionIndex(final int greedDescriptionIndex) {
        this.greedDescriptionIndex = clamp(greedDescriptionIndex, 0, Greed.MAXIMUM_VARIATIONS - 1);
    }

    public Social getSocial() {
        return social;
    }

    public void setSocial(final Social social) {
        this.social = social;
    }

    public int getSocialDescriptionIndex() {
        return socialDescriptionIndex;
    }

    /**
     * Sets the index value for the {@link Social} description.
     *
     * @param socialDescriptionIndex The index value to set for the Social description. It will be clamped to ensure it
     *                               remains within the valid range.
     */
    public void setSocialDescriptionIndex(final int socialDescriptionIndex) {
        this.socialDescriptionIndex = clamp(socialDescriptionIndex, 0, Social.MAXIMUM_VARIATIONS - 1);
    }

    public PersonalityQuirk getPersonalityQuirk() {
        return personalityQuirk;
    }

    public void setPersonalityQuirk(final PersonalityQuirk personalityQuirk) {
        this.personalityQuirk = personalityQuirk;
    }

    public int getPersonalityQuirkDescriptionIndex() {
        return personalityQuirkDescriptionIndex;
    }

    /**
     * Sets the index value for the {@link PersonalityQuirk} description.
     *
     * @param personalityQuirkDescriptionIndex The index value to set for the quirk description. It will be clamped to
     *                                         ensure it remains within the valid range.
     */
    public void setPersonalityQuirkDescriptionIndex(final int personalityQuirkDescriptionIndex) {
        this.personalityQuirkDescriptionIndex = clamp(personalityQuirkDescriptionIndex,
              0,
              PersonalityQuirk.MAXIMUM_VARIATIONS - 1);
    }

    /**
     * @deprecated replaced by {@link #getReasoning()}
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public Reasoning getIntelligence() {
        return reasoning;
    }

    public Reasoning getReasoning() {
        return reasoning;
    }

    /**
     * @deprecated replaced by {@link #setReasoning(Reasoning)}
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public void setIntelligence(final Reasoning reasoning) {
        this.reasoning = reasoning;
    }

    public void setReasoning(final Reasoning reasoning) {
        this.reasoning = reasoning;
    }

    /**
     * @deprecated replaced by {@link #getReasoningDescriptionIndex()} ()}
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public int getIntelligenceDescriptionIndex() {
        return reasoningDescriptionIndex;
    }

    public int getReasoningDescriptionIndex() {
        return reasoningDescriptionIndex;
    }

    /**
     * @deprecated replaced by {@link #setReasoningDescriptionIndex(int)}
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public void setIntelligenceDescriptionIndex(final int intelligenceDescriptionIndex) {
        setReasoningDescriptionIndex(intelligenceDescriptionIndex);
    }

    /**
     * Sets the index value for the {@link Reasoning} description.
     *
     * @param reasoningDescriptionIndex The index value to set for the Reasoning description. It will be clamped to
     *                                  ensure it remains within the valid range.
     */
    public void setReasoningDescriptionIndex(final int reasoningDescriptionIndex) {
        this.reasoningDescriptionIndex = clamp(reasoningDescriptionIndex, 0, Reasoning.MAXIMUM_VARIATIONS - 1);
    }

    public String getPersonalityDescription() {
        return personalityDescription;
    }

    public void setPersonalityDescription(final String personalityDescription) {
        this.personalityDescription = personalityDescription;
    }

    // region Flags
    public boolean isClanPersonnel() {
        return clanPersonnel;
    }

    public void setClanPersonnel(final boolean clanPersonnel) {
        this.clanPersonnel = clanPersonnel;
    }

    /**
     * @return true if the person is the campaign commander, false otherwise.
     */
    public boolean isCommander() {
        return commander;
    }

    /**
     * Flags the person as the campaign commander.
     */
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
    // endregion Flags

    public ExtraData getExtraData() {
        return extraData;
    }

    // region File I/O
    public void writeToXML(final PrintWriter pw, int indent, final Campaign campaign) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "person", "id", id, "type", getClass());
        try {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "id", id.toString());

            // region Name
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
            // endregion Name

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
                MHQXMLUtility.writeSimpleXMLAttributedTag(pw,
                      indent,
                      "planetId",
                      "systemId",
                      originPlanet.getParentSystem().getId(),
                      originPlanet.getId());
            }

            if (becomingBondsmanEndDate != null) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "becomingBondsmanEndDate", becomingBondsmanEndDate);
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

            if (vocationalXPTimer > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "vocationalXPTimer", vocationalXPTimer);
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
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "bloodGroup", getBloodGroup().name());
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
            // Always save a person's status, to make it easy to parse the personnel saved
            // data
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "status", status.name());
            if (prisonerStatus != PrisonerStatus.FREE) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "prisonerStatus", prisonerStatus.name());
            }

            if (hits > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "hits", hits);
            }

            if (hitsPrior > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "hitsPrior", hitsPrior);
            }

            if (toughness != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "toughness", toughness);
            }

            if (connections != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "connections", connections);
            }

            if (wealth != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "wealth", wealth);
            }

            if (hasPerformedExtremeExpenditure) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "hasPerformedExtremeExpenditure", true);
            }

            if (reputation != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "reputation", reputation);
            }

            if (unlucky != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unlucky", unlucky);
            }

            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "atowAttributes");
            atowAttributes.writeAttributesToXML(pw, indent);
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "atowAttributes");

            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "minutesLeft", minutesLeft);

            if (overtimeLeft > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "overtimeLeft", overtimeLeft);
            }
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "birthday", getDateOfBirth());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "deathday", getDateOfDeath());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "recruitment", getRecruitment());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "joinedCampaign", getJoinedCampaign());

            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "lastRankChangeDate", getLastRankChangeDate());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "autoAwardSupportPoints", getAutoAwardSupportPoints());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "retirement", getRetirement());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "loyalty", getLoyalty());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "fatigue", getFatigue());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "isRecoveringFromFatigue", getIsRecoveringFromFatigue());
            for (Skill skill : skills.getSkills()) {
                skill.writeToXML(pw, indent);
            }

            if (countOptions(PersonnelOptions.LVL3_ADVANTAGES) > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw,
                      indent,
                      "advantages",
                      getOptionList("::", PersonnelOptions.LVL3_ADVANTAGES));
            }

            if (countOptions(PersonnelOptions.EDGE_ADVANTAGES) > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw,
                      indent,
                      "edge",
                      getOptionList("::", PersonnelOptions.EDGE_ADVANTAGES));
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "edgeAvailable", getCurrentEdge());
            }

            if (countOptions(PersonnelOptions.MD_ADVANTAGES) > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw,
                      indent,
                      "implants",
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

            if (!medicalLog.isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "medicalLog");
                for (LogEntry entry : medicalLog) {
                    entry.writeToXML(pw, indent);
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "medicalLog");
            }

            if (!scenarioLog.isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "scenarioLog");
                for (LogEntry entry : scenarioLog) {
                    entry.writeToXML(pw, indent);
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "scenarioLog");
            }

            if (!assignmentLog.isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "assignmentLog");
                for (LogEntry entry : assignmentLog) {
                    entry.writeToXML(pw, indent);
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "assignmentLog");
            }

            if (!performanceLog.isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "performanceLog");
                for (LogEntry entry : performanceLog) {
                    entry.writeToXML(pw, indent);
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "performanceLog");
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

            if (eduHighestEducation != EducationLevel.EARLY_CHILDHOOD) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "eduHighestEducation", eduHighestEducation.name());
            }

            if (eduJourneyTime != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "eduJourneyTime", eduJourneyTime);
            }

            if (eduDaysOfTravel != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "eduDaysOfTravel", eduDaysOfTravel);
            }

            if (!eduTagAlongs.isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "eduTagAlongs");

                for (UUID tagAlong : eduTagAlongs) {
                    MHQXMLUtility.writeSimpleXMLTag(pw, indent, "tagAlong", tagAlong.toString());
                }

                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "eduTagAlongs");
            }

            if (!eduTagAlongs.isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "eduFailedApplications");

                for (String failedApplication : eduFailedApplications) {
                    MHQXMLUtility.writeSimpleXMLTag(pw, indent, "eduFailedApplication", failedApplication);
                }

                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "eduFailedApplications");
            }

            if (eduAcademySystem != null) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "eduAcademySystem", eduAcademySystem);
            }

            if (eduAcademyNameInSet != null) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "eduAcademyNameInSet", eduAcademyNameInSet);
            }

            if (eduAcademyFaction != null) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "eduAcademyFaction", eduAcademyFaction);
            }

            if (eduAcademySet != null) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "eduAcademySet", eduAcademySet);
            }

            if (eduAcademyName != null) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "eduAcademyName", eduAcademyName);
            }

            if (eduCourseIndex != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "eduCourseIndex", eduCourseIndex);
            }

            if (eduEducationStage != EducationStage.NONE) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "eduEducationStage", eduEducationStage.toString());
            }

            if (eduEducationTime != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "eduEducationTime", eduEducationTime);
            }

            if (aggression != Aggression.NONE) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "aggression", aggression.name());
            }

            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "aggressionDescriptionIndex", aggressionDescriptionIndex);

            if (ambition != Ambition.NONE) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "ambition", ambition.name());
            }

            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "ambitionDescriptionIndex", ambitionDescriptionIndex);

            if (greed != Greed.NONE) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "greed", greed.name());
            }

            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "greedDescriptionIndex", greedDescriptionIndex);

            if (social != Social.NONE) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "social", social.name());
            }

            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "socialDescriptionIndex", socialDescriptionIndex);

            if (personalityQuirk != PersonalityQuirk.NONE) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personalityQuirk", personalityQuirk.ordinal());
            }

            MHQXMLUtility.writeSimpleXMLTag(pw,
                  indent,
                  "personalityQuirkDescriptionIndex",
                  personalityQuirkDescriptionIndex);

            if (reasoning != Reasoning.AVERAGE) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "reasoning", reasoning.ordinal());
            }

            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "reasoningDescriptionIndex", reasoningDescriptionIndex);

            if (!StringUtility.isNullOrBlank(personalityDescription)) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personalityDescription", personalityDescription);
            }

            // region Flags
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
            // endregion Flags

            if (!extraData.isEmpty()) {
                extraData.writeToXml(pw);
            }
        } catch (Exception ex) {
            logger.error(ex, "Failed to write {} to the XML File", getFullName());
            throw ex; // we want to rethrow to ensure that the save fails
        }

        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "person");
    }

    public static Person generateInstanceFromXML(Node wn, Campaign campaign, Version version) {
        Person person = new Person(campaign);

        try {
            // Okay, now load Person-specific fields!
            NodeList nl = wn.getChildNodes();

            String advantages = null;
            String edge = null;
            String implants = null;

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                String nodeName = wn2.getNodeName();

                if (nodeName.equalsIgnoreCase("preNominal")) {
                    person.setPreNominalDirect(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("givenName")) {
                    person.setGivenNameDirect(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("surname")) {
                    person.setSurnameDirect(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("postNominal")) {
                    person.setPostNominalDirect(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("maidenName")) {
                    person.setMaidenName(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("callsign")) {
                    person.setCallsignDirect(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("faction")) {
                    person.setOriginFaction(Factions.getInstance().getFaction(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("planetId")) {
                    String systemId = "", planetId = "";
                    try {
                        systemId = wn2.getAttributes().getNamedItem("systemId").getTextContent().trim();
                        planetId = wn2.getTextContent().trim();
                        PlanetarySystem ps = campaign.getSystemById(systemId);
                        Planet p = null;
                        if (ps == null) {
                            ps = campaign.getSystemByName(systemId);
                        }
                        if (ps != null) {
                            p = ps.getPlanetById(planetId);
                        }
                        person.originPlanet = p;
                    } catch (NullPointerException e) {
                        logger.error("Error loading originPlanet for {}, {}", systemId, planetId, e);
                    }
                } else if (nodeName.equalsIgnoreCase("becomingBondsmanEndDate")) {
                    person.becomingBondsmanEndDate = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("phenotype")) {
                    person.phenotype = Phenotype.fromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("bloodname")) {
                    person.bloodname = wn2.getTextContent();
                } else if (nodeName.equalsIgnoreCase("biography")) {
                    person.biography = wn2.getTextContent();
                } else if (nodeName.equalsIgnoreCase("primaryRole")) {
                    final PersonnelRole primaryRole = PersonnelRole.fromString(wn2.getTextContent().trim());
                    person.setPrimaryRoleDirect(primaryRole);
                } else if (nodeName.equalsIgnoreCase("secondaryRole")) {
                    person.setSecondaryRoleDirect(PersonnelRole.fromString(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("acquisitions")) {
                    person.acquisitions = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("primaryDesignator")) {
                    person.primaryDesignator = ROMDesignation.parseFromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("secondaryDesignator")) {
                    person.secondaryDesignator = ROMDesignation.parseFromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("daysToWaitForHealing")) {
                    person.daysToWaitForHealing = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("vocationalXPTimer")
                                 // <50.03 compatibility handler
                                 || nodeName.equalsIgnoreCase("idleMonths")) {
                    person.vocationalXPTimer = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("id")) {
                    person.id = UUID.fromString(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("genealogy")) {
                    person.getGenealogy().fillFromXML(wn2.getChildNodes());
                } else if (nodeName.equalsIgnoreCase("dueDate")) {
                    person.dueDate = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("expectedDueDate")) {
                    person.expectedDueDate = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase(Portrait.XML_TAG)) {
                    person.setPortrait(Portrait.parseFromXML(wn2));
                } else if (nodeName.equalsIgnoreCase("xp")) {
                    person.setXPDirect(MathUtility.parseInt(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("totalXPEarnings")) {
                    person.setTotalXPEarnings(MathUtility.parseInt(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("nTasks")) {
                    person.nTasks = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("hits")) {
                    person.hits = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("hitsPrior")) {
                    person.hitsPrior = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("gender")) {
                    person.setGender(Gender.parseFromString(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("bloodGroup")) {
                    person.setBloodGroup(BloodGroup.fromString(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("rankSystem")) {
                    final RankSystem rankSystem = Ranks.getRankSystemFromCode(wn2.getTextContent().trim());

                    if (rankSystem != null) {
                        person.setRankSystemDirect(rankSystem);
                    }
                } else if (nodeName.equalsIgnoreCase("rank")) {
                    person.setRank(MathUtility.parseInt(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("rankLevel")) {
                    person.setRankLevel(MathUtility.parseInt(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("maneiDominiClass")) {
                    person.setManeiDominiClassDirect(ManeiDominiClass.parseFromString(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("maneiDominiRank")) {
                    person.setManeiDominiRankDirect(ManeiDominiRank.parseFromString(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("doctorId")) {
                    if (!wn2.getTextContent().equals("null")) {
                        person.doctorId = UUID.fromString(wn2.getTextContent());
                    }
                } else if (nodeName.equalsIgnoreCase("unitId")) {
                    if (!wn2.getTextContent().equals("null")) {
                        person.unit = new PersonUnitRef(UUID.fromString(wn2.getTextContent()));
                    }
                } else if (nodeName.equalsIgnoreCase("status")) {
                    person.setStatus(PersonnelStatus.fromString(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("prisonerStatus")) {
                    person.prisonerStatus = PrisonerStatus.parseFromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("salary")) {
                    person.salary = Money.fromXmlString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("totalEarnings")) {
                    person.totalEarnings = Money.fromXmlString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("minutesLeft")) {
                    person.minutesLeft = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("overtimeLeft")) {
                    person.overtimeLeft = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("birthday")) {
                    person.birthday = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("deathday")) {
                    person.dateOfDeath = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("recruitment")) {
                    person.recruitment = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("joinedCampaign")) {
                    person.joinedCampaign = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("lastRankChangeDate")) {
                    person.lastRankChangeDate = MHQXMLUtility.parseDate(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("autoAwardSupportPoints")) {
                    person.setAutoAwardSupportPoints(MathUtility.parseInt(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("retirement")) {
                    person.setRetirement(MHQXMLUtility.parseDate(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("loyalty")) {
                    person.loyalty = MathUtility.parseInt(wn2.getTextContent(), 9);
                } else if (nodeName.equalsIgnoreCase("fatigue")) {
                    person.fatigue = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("isRecoveringFromFatigue")) {
                    person.isRecoveringFromFatigue = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("advantages")) {
                    advantages = wn2.getTextContent();
                } else if (nodeName.equalsIgnoreCase("edge")) {
                    edge = wn2.getTextContent();
                } else if (nodeName.equalsIgnoreCase("edgeAvailable")) {
                    person.currentEdge = MathUtility.parseInt(wn2.getTextContent(), 0);
                } else if (nodeName.equalsIgnoreCase("implants")) {
                    implants = wn2.getTextContent();
                } else if (nodeName.equalsIgnoreCase("toughness")) {
                    person.toughness = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("connections")) {
                    person.connections = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("wealth")) {
                    person.wealth = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("hasPerformedExtremeExpenditure")) {
                    person.hasPerformedExtremeExpenditure = Boolean.parseBoolean(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("reputation")) {
                    person.reputation = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("unlucky")) {
                    person.unlucky = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("atowAttributes")) {
                    person.atowAttributes = new Attributes().generateAttributesFromXML(wn2);
                } else if (nodeName.equalsIgnoreCase("pilotHits")) {
                    person.hits = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("skill")) {
                    Skill s = Skill.generateInstanceFromXML(wn2);
                    if ((s != null) && (s.getType() != null)) {
                        person.skills.addSkill(s.getType().getName(), s);
                    }
                } else if (nodeName.equalsIgnoreCase("techUnitIds")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("id")) {
                            logger.error("(techUnitIds) Unknown node type not loaded in techUnitIds nodes: {}",
                                  wn3.getNodeName());
                            continue;
                        }
                        person.addTechUnit(new PersonUnitRef(UUID.fromString(wn3.getTextContent())));
                    }
                } else if (nodeName.equalsIgnoreCase("personnelLog")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("logEntry")) {
                            logger.error("(personnelLog) Unknown node type not loaded in personnel logEntry nodes: {}",
                                  wn3.getNodeName());
                            continue;
                        }

                        final LogEntry logEntry = LogEntryFactory.getInstance().generateInstanceFromXML(wn3);
                        if (logEntry != null) {
                            // <50.05 compatibility handler
                            LogEntryType logEntryType = logEntry.getType();
                            String logEntryDescription = logEntry.getDesc();
                            if (logEntryType == MEDICAL) {
                                person.addMedicalLogEntry(logEntry);
                            } else if (logEntryType == SERVICE) {
                                // < 50.05 compatibility handler
                                List<String> assignmentTargetStrings = List.of("Assigned to",
                                      "Reassigned from",
                                      "Removed from",
                                      "Added to");

                                for (String targetString : assignmentTargetStrings) {
                                    if (logEntryDescription.startsWith(targetString)) {
                                        logEntry.setType(ASSIGNMENT);
                                        person.addAssignmentLogEntry(logEntry);
                                        break;
                                    }
                                }
                            } else {
                                // < 50.05 compatibility handler
                                List<String> performanceTargetStrings = List.of("Changed edge to",
                                      "Gained",
                                      "Improved",
                                      "injuries, gaining",
                                      "XP from successful medical work");

                                boolean foundPerformanceTarget = false;
                                for (String targetString : performanceTargetStrings) {
                                    if (logEntryDescription.startsWith(targetString)) {
                                        foundPerformanceTarget = true;
                                        break;
                                    }
                                }

                                if (foundPerformanceTarget) {
                                    logEntry.setType(PERFORMANCE);
                                    person.addPerformanceLogEntry(logEntry);
                                } else {
                                    person.addPersonalLogEntry(logEntry);
                                }
                            }
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("medicalLog")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("logEntry")) {
                            logger.error("(medicalLog) Unknown node type not loaded in personnel logEntry nodes: {}",
                                  wn3.getNodeName());
                            continue;
                        }

                        final LogEntry logEntry = LogEntryFactory.getInstance().generateInstanceFromXML(wn3);
                        if (logEntry != null) {
                            person.addMedicalLogEntry(logEntry);
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("scenarioLog")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("logEntry")) {
                            logger.error("Unknown node type not loaded in scenario logEntry nodes: {}",
                                  wn3.getNodeName());
                            continue;
                        }

                        final LogEntry logEntry = LogEntryFactory.getInstance().generateInstanceFromXML(wn3);
                        if (logEntry != null) {
                            person.addScenarioLogEntry(logEntry);
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("assignmentLog")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("logEntry")) {
                            logger.error("(assignmentLog) Unknown node type not loaded in scenario logEntry nodes: {}",
                                  wn3.getNodeName());
                            continue;
                        }

                        final LogEntry logEntry = LogEntryFactory.getInstance().generateInstanceFromXML(wn3);
                        if (logEntry != null) {
                            person.addAssignmentLogEntry(logEntry);
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("performanceLog")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("logEntry")) {
                            logger.error("(performanceLog) Unknown node type not loaded in scenario logEntry nodes: {}",
                                  wn3.getNodeName());
                            continue;
                        }

                        final LogEntry logEntry = LogEntryFactory.getInstance().generateInstanceFromXML(wn3);
                        if (logEntry != null) {
                            person.addPerformanceLogEntry(logEntry);
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("awards")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("award")) {
                            logger.error("Unknown node type not loaded in personnel award log nodes: {}",
                                  wn3.getNodeName());
                            continue;
                        }

                        person.getAwardController()
                              .addAwardFromXml(AwardsFactory.getInstance().generateNewFromXML(wn3));
                    }
                } else if (nodeName.equalsIgnoreCase("injuries")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("injury")) {
                            logger.error("Unknown node type not loaded in injury nodes: {}", wn3.getNodeName());
                            continue;
                        }
                        person.injuries.add(Injury.generateInstanceFromXML(wn3));
                    }
                    LocalDate now = campaign.getLocalDate();
                    person.injuries.stream()
                          .filter(inj -> (null == inj.getStart()))
                          .forEach(inj -> inj.setStart(now.minusDays(inj.getOriginalTime() - inj.getTime())));
                } else if (nodeName.equalsIgnoreCase("originalUnitWeight")) {
                    person.originalUnitWeight = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("originalUnitTech")) {
                    person.originalUnitTech = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("originalUnitId")) {
                    person.originalUnitId = UUID.fromString(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("eduHighestEducation")) {
                    person.eduHighestEducation = EducationLevel.fromString(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("eduJourneyTime")) {
                    person.eduJourneyTime = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("eduDaysOfTravel")) {
                    person.eduDaysOfTravel = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("eduTagAlongs")) {
                    if (nodeName.equalsIgnoreCase("eduTagAlongs")) {
                        NodeList uuidNodes = wn2.getChildNodes();

                        for (int j = 0; j < uuidNodes.getLength(); j++) {
                            Node uuidNode = uuidNodes.item(j);

                            if (uuidNode.getNodeName().equalsIgnoreCase("tagAlong")) {
                                String uuidString = uuidNode.getTextContent();

                                UUID uuid = UUID.fromString(uuidString);

                                person.eduTagAlongs.add(uuid);
                            }
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("eduFailedApplications")) {
                    if (nodeName.equalsIgnoreCase("eduFailedApplications")) {
                        NodeList nodes = wn2.getChildNodes();

                        for (int j = 0; j < nodes.getLength(); j++) {
                            Node node = nodes.item(j);

                            if (node.getNodeName().equalsIgnoreCase("eduFailedApplication")) {
                                person.eduFailedApplications.add(node.getTextContent());
                            }
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("eduAcademySystem")) {
                    person.eduAcademySystem = String.valueOf(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("eduAcademyName")) {
                    person.eduAcademyName = String.valueOf(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("eduAcademySet")) {
                    person.eduAcademySet = String.valueOf(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("eduAcademyNameInSet")) {
                    person.eduAcademyNameInSet = String.valueOf(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("eduAcademyFaction")) {
                    person.eduAcademyFaction = String.valueOf(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("eduCourseIndex")) {
                    person.eduCourseIndex = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("eduEducationStage")) {
                    person.eduEducationStage = EducationStage.parseFromString(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("eduEducationTime")) {
                    person.eduEducationTime = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("aggression")) {
                    person.aggression = Aggression.fromString(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("aggressionDescriptionIndex")) {
                    person.aggressionDescriptionIndex = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("ambition")) {
                    person.ambition = Ambition.fromString(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("ambitionDescriptionIndex")) {
                    person.ambitionDescriptionIndex = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("greed")) {
                    person.greed = Greed.fromString(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("greedDescriptionIndex")) {
                    person.greedDescriptionIndex = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("social")) {
                    person.social = Social.fromString(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("socialDescriptionIndex")) {
                    person.socialDescriptionIndex = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("personalityQuirk")) {
                    person.personalityQuirk = PersonalityQuirk.fromString(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("personalityQuirkDescriptionIndex")) {
                    person.personalityQuirkDescriptionIndex = MathUtility.parseInt(wn2.getTextContent());
                    // 'intelligence' is a <50.05 compatibility handler
                } else if ((nodeName.equalsIgnoreCase("reasoning")) || (nodeName.equalsIgnoreCase("intelligence"))) {
                    person.reasoning = Reasoning.fromString(wn2.getTextContent());
                    // 'intelligenceDescriptionIndex' is a <50.05 compatibility handler
                } else if ((nodeName.equalsIgnoreCase("reasoningDescriptionIndex")) ||
                                 (nodeName.equalsIgnoreCase("intelligenceDescriptionIndex"))) {
                    person.reasoningDescriptionIndex = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("personalityDescription")) {
                    person.personalityDescription = wn2.getTextContent();
                } else if (nodeName.equalsIgnoreCase("clanPersonnel")) {
                    person.setClanPersonnel(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("commander")) {
                    person.setCommander(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("divorceable")) {
                    person.setDivorceable(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("founder")) {
                    person.setFounder(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("immortal")) {
                    person.setImmortal(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("marriageable")) {
                    person.setMarriageable(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("tryingToConceive")) {
                    person.setTryingToConceive(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("extraData")) {
                    person.extraData = ExtraData.createFromXml(wn2);
                }
            }

            person.setFullName(); // this sets the name based on the loaded values

            if ((advantages != null) && !advantages.isBlank()) {
                StringTokenizer st = new StringTokenizer(advantages, "::");
                while (st.hasMoreTokens()) {
                    String adv = st.nextToken();
                    String advName = Crew.parseAdvantageName(adv);
                    Object value = Crew.parseAdvantageValue(adv);

                    try {
                        person.getOptions().getOption(advName).setValue(value);
                    } catch (Exception e) {
                        logger.warn("Error restoring advantage: {}", adv);
                    }
                }
            }

            if ((edge != null) && !edge.isBlank()) {
                List<String> edgeOptionList = getEdgeTriggersList();
                // this prevents an error caused by the Option Group name being included in the
                // list of options for that group
                edgeOptionList.remove(0);

                updateOptions(edge, person, edgeOptionList);
                removeUnusedEdgeTriggers(person, edgeOptionList);
            }

            if ((implants != null) && !implants.isBlank()) {
                StringTokenizer st = new StringTokenizer(implants, "::");
                while (st.hasMoreTokens()) {
                    String adv = st.nextToken();
                    String advName = Crew.parseAdvantageName(adv);
                    Object value = Crew.parseAdvantageValue(adv);

                    try {
                        person.getOptions().getOption(advName).setValue(value);
                    } catch (Exception e) {
                        logger.error("Error restoring implants: {}", adv);
                    }
                }
            }

            // Fixing Prisoner Ranks - 0.47.X Fix
            if (person.getRankNumeric() < 0) {
                person.setRank(0);
            }

            // Fixing recruitment dates
            // I don't know when this metric was added, so we check all versions
            if (person.getRecruitment() == null) {
                person.setRecruitment(campaign.getLocalDate());
            }

            // This resolves a bug squashed in 2025 (50.03) but lurked in our codebase
            // potentially as far back as 2014. The next two handlers should never be removed.
            if (!person.canPerformRole(campaign.getLocalDate(), person.getPrimaryRole(), true)) {
                person.setPrimaryRole(campaign, PersonnelRole.NONE);

                campaign.addReport(String.format(resources.getString("ineligibleForPrimaryRole"),
                      spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                      CLOSING_SPAN_TAG,
                      person.getHyperlinkedFullTitle()));
            }

            if (!person.canPerformRole(campaign.getLocalDate(), person.getSecondaryRole(), false)) {
                person.setSecondaryRole(PersonnelRole.NONE);

                campaign.addReport(String.format(resources.getString("ineligibleForSecondaryRole"),
                      spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorWarningHexColor()),
                      CLOSING_SPAN_TAG,
                      person.getHyperlinkedFullTitle()));
            }
        } catch (Exception e) {
            logger.error(e, "Failed to read person {} from file", person.getFullName());
            person = null;
        }

        return person;
    }
    // endregion File I/O

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
        Money primaryBase = campaign.getCampaignOptions().getRoleBaseSalaries()[getPrimaryRole().ordinal()];

        // SpecInf is a special case, this needs to be applied first to bring base
        // salary up to RAW.
        if (getPrimaryRole().isSoldierOrBattleArmour()) {
            if ((getUnit() != null) &&
                      getUnit().isConventionalInfantry() &&
                      ((Infantry) getUnit().getEntity()).hasSpecialization()) {
                primaryBase = primaryBase.multipliedBy(campaign.getCampaignOptions()
                                                             .getSalarySpecialistInfantryMultiplier());
            }
        }

        // Experience multiplier
        primaryBase = primaryBase.multipliedBy(campaign.getCampaignOptions()
                                                     .getSalaryXPMultipliers()
                                                     .get(getSkillLevel(campaign, false)));

        // Specialization multiplier
        if (getPrimaryRole().isSoldierOrBattleArmour()) {
            if (hasSkill(SkillType.S_ANTI_MEK)) {
                primaryBase = primaryBase.multipliedBy(campaign.getCampaignOptions().getSalaryAntiMekMultiplier());
            }
        }

        // CamOps doesn't cover secondary roles, so we just half the base salary of the
        // secondary role.
        Money secondaryBase = Money.zero();

        if (!campaign.getCampaignOptions().isDisableSecondaryRoleSalary()) {
            secondaryBase = campaign.getCampaignOptions().getRoleBaseSalaries()[getSecondaryRole().ordinal()].dividedBy(
                  2);

            // SpecInf is a special case, this needs to be applied first to bring base
            // salary up to RAW.
            if (getSecondaryRole().isSoldierOrBattleArmour()) {
                if (hasSkill(SkillType.S_ANTI_MEK)) {
                    secondaryBase = secondaryBase.multipliedBy(campaign.getCampaignOptions()
                                                                     .getSalaryAntiMekMultiplier());
                }
            }

            // Experience modifier
            secondaryBase = secondaryBase.multipliedBy(campaign.getCampaignOptions()
                                                             .getSalaryXPMultipliers()
                                                             .get(getSkillLevel(campaign, true)));

            // Specialization
            if (getSecondaryRole().isSoldierOrBattleArmour()) {
                if (hasSkill(SkillType.S_ANTI_MEK)) {
                    secondaryBase = secondaryBase.multipliedBy(campaign.getCampaignOptions()
                                                                     .getSalaryAntiMekMultiplier());
                }
            }
        }

        // TODO: distinguish DropShip, JumpShip, and WarShip crew
        // TODO: Add era mod to salary calc..
        if (getRank().getPayMultiplier() > 0) {
            return primaryBase.plus(secondaryBase).multipliedBy(getRank().getPayMultiplier());
        } else {
            return primaryBase.plus(secondaryBase);
        }
    }

    /**
     * Retrieves a list of edge triggers from PilotOptions.
     *
     * @return a List of edge triggers. If no edge triggers are found, an empty List is returned.
     */
    private static List<String> getEdgeTriggersList() {
        Enumeration<IOptionGroup> groups = new PilotOptions().getGroups();

        while (groups.hasMoreElements()) {
            IOptionGroup group = groups.nextElement();

            if (group.getKey().equals(PilotOptions.EDGE_ADVANTAGES)) {
                return Collections.list(group.getOptionNames());
            }
        }

        return new ArrayList<>();
    }

    /**
     * Updates the status of Edge Triggers based on those stored in edgeTriggers
     *
     * @param edgeTriggers   the string containing edge triggers delimited by "::"
     * @param retVal         the person to update
     * @param edgeOptionList the list of edge triggers to remove
     */
    private static void updateOptions(String edgeTriggers, Person retVal, List<String> edgeOptionList) {
        StringTokenizer st = new StringTokenizer(edgeTriggers, "::");

        while (st.hasMoreTokens()) {
            String trigger = st.nextToken();
            String triggerName = Crew.parseAdvantageName(trigger);
            Object value = Crew.parseAdvantageValue(trigger);

            try {
                retVal.getOptions().getOption(triggerName).setValue(value);
                edgeOptionList.remove(triggerName);
            } catch (Exception e) {
                logger.error("Error restoring edge trigger: {}", trigger);
            }
        }
    }

    /**
     * Explicitly disables unused Edge triggers
     *
     * @param retVal         the person for whom the triggers are disabled
     * @param edgeOptionList the list of edge triggers to be processed
     */
    private static void removeUnusedEdgeTriggers(Person retVal, List<String> edgeOptionList) {
        for (String edgeTrigger : edgeOptionList) {
            String advName = Crew.parseAdvantageName(edgeTrigger);

            try {
                retVal.getOptions().getOption(advName).setValue(false);
            } catch (Exception e) {
                logger.error("Error disabling edge trigger: {}", edgeTrigger);
            }
        }
    }

    /**
     * @return the person's total earnings
     */
    public Money getTotalEarnings() {
        return totalEarnings;
    }

    /**
     * This is used to pay a person. Preventing negative payments is intentional to ensure we don't accidentally change
     * someone when trying to give them money. To charge a person, implement a new method. (And then add a @see here)
     *
     * @param money the amount of money to add to their total earnings
     */
    public void payPerson(final Money money) {
        if (money.isPositiveOrZero()) {
            totalEarnings = getTotalEarnings().plus((money));
        }
    }

    /**
     * This is used to pay a person their share value based on the value of a single share
     *
     * @param campaign     the campaign the person is a part of
     * @param money        the value of a single share
     * @param sharesForAll whether or not all personnel have shares
     */
    public void payPersonShares(final Campaign campaign, final Money money, final boolean sharesForAll) {
        final int shares = getNumShares(campaign, sharesForAll);
        if (shares > 0) {
            payPerson(money.multipliedBy(shares));
        }
    }

    // region Ranks
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

    public void changeRank(final Campaign campaign, final int rankNumeric, final int rankLevel, final boolean report) {
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
            if ((rankNumeric > oldRankNumeric) || ((rankNumeric == oldRankNumeric) && (rankLevel > oldRankLevel))) {
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
        rankName = rankName.equalsIgnoreCase("None") ?
                         getPrisonerStatus().getTitleExtension() :
                         getPrisonerStatus().getTitleExtension() + ' ' + rankName;

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
     * Determines whether this person outranks another, taking into account the seniority rank for ComStar and WoB
     * ranks.
     *
     * @param other The <code>Person</code> to compare ranks with
     *
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

    /**
     * Checks if the current person outranks another person using a skill tiebreaker. If the other person is null, it is
     * considered that the current person outranks them. If both persons have the same rank numeric value, the rank
     * level is compared. If both persons have the same rank numeric value and rank level, the experience levels are
     * compared.
     *
     * @param campaign    the campaign used to calculate the experience levels
     * @param otherPerson the other person to compare ranks with
     *
     * @return true if the current person outranks the other person, false otherwise
     */
    public boolean outRanksUsingSkillTiebreaker(Campaign campaign, @Nullable Person otherPerson) {
        if (otherPerson == null) {
            return true;
        } else if (getRankNumeric() == otherPerson.getRankNumeric()) {
            if (getRankLevel() > otherPerson.getRankLevel()) {
                return true;
            } else if (getRankLevel() < otherPerson.getRankLevel()) {
                return false;
            } else {
                if (getExperienceLevel(campaign, false) == otherPerson.getExperienceLevel(campaign, false)) {
                    return getExperienceLevel(campaign, true) > otherPerson.getExperienceLevel(campaign, true);
                } else {
                    return getExperienceLevel(campaign, false) > otherPerson.getExperienceLevel(campaign, false);
                }
            }
        } else {
            return getRankNumeric() > otherPerson.getRankNumeric();
        }
    }
    // endregion Ranks

    @Override
    public String toString() {
        return getFullName();
    }

    /**
     * Two people are determined to be equal if they have the same id
     *
     * @param object The object to check if it is equal to the person or not
     *
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
        final CampaignOptions campaignOptions = campaign.getCampaignOptions();
        final boolean isAlternativeQualityAveraging = campaignOptions.isAlternativeQualityAveraging();

        switch (role) {
            case MEKWARRIOR:
                if (hasSkill(SkillType.S_GUN_MEK) && hasSkill(SkillType.S_PILOT_MEK)) {
                    /*
                     * Attempt to use higher precision averaging, but if it doesn't provide a clear result  due to
                     * non-standard experience thresholds then fall back on lower precision averaging See Bug #140
                     */
                    if (isAlternativeQualityAveraging) {
                        int rawScore = (int) floor((getSkill(SkillType.S_GUN_MEK).getLevel() +
                                                          getSkill(SkillType.S_PILOT_MEK).getLevel()) / 2.0);
                        if (getSkill(SkillType.S_GUN_MEK).getType().getExperienceLevel(rawScore) ==
                                  getSkill(SkillType.S_PILOT_MEK).getType().getExperienceLevel(rawScore)) {
                            return getSkill(SkillType.S_GUN_MEK).getType().getExperienceLevel(rawScore);
                        }
                    }

                    return (int) floor((getSkill(SkillType.S_GUN_MEK).getExperienceLevel() +
                                              getSkill(SkillType.S_PILOT_MEK).getExperienceLevel()) / 2.0);
                } else {
                    return SkillType.EXP_NONE;
                }
            case LAM_PILOT:
                if (Stream.of(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK, SkillType.S_GUN_AERO, SkillType.S_PILOT_AERO)
                          .allMatch(this::hasSkill)) {
                    /*
                     * Attempt to use higher precision averaging, but if it doesn't provide a clear result due to
                     * non-standard experience thresholds then fall back on lower precision averaging See Bug #140
                     */
                    if (isAlternativeQualityAveraging) {
                        int rawScore = (int) floor((Stream.of(SkillType.S_GUN_MEK,
                              SkillType.S_PILOT_MEK,
                              SkillType.S_GUN_AERO,
                              SkillType.S_PILOT_AERO).mapToInt(s -> getSkill(s).getLevel()).sum()) / 4.0);

                        final int mekGunneryExperienceLevel = SkillType.lookupHash.get(SkillType.S_GUN_MEK)
                                                                    .getExperienceLevel(rawScore);
                        if ((mekGunneryExperienceLevel ==
                                   SkillType.lookupHash.get(SkillType.S_PILOT_MEK).getExperienceLevel(rawScore) &&
                                   (mekGunneryExperienceLevel ==
                                          SkillType.lookupHash.get(SkillType.S_GUN_AERO)
                                                .getExperienceLevel(rawScore)) &&
                                   (mekGunneryExperienceLevel ==
                                          SkillType.lookupHash.get(SkillType.S_PILOT_AERO)
                                                .getExperienceLevel(rawScore)))) {
                            return getSkill(SkillType.S_GUN_MEK).getType().getExperienceLevel(rawScore);
                        }
                    }

                    return (int) floor((getSkill(SkillType.S_GUN_MEK).getExperienceLevel() +
                                              getSkill(SkillType.S_PILOT_MEK).getExperienceLevel() +
                                              getSkill(SkillType.S_GUN_AERO).getExperienceLevel() +
                                              getSkill(SkillType.S_PILOT_AERO).getExperienceLevel()) / 4.0);
                } else {
                    return SkillType.EXP_NONE;
                }
            case GROUND_VEHICLE_DRIVER:
                return hasSkill(SkillType.S_PILOT_GVEE) ?
                             getSkill(SkillType.S_PILOT_GVEE).getExperienceLevel() :
                             SkillType.EXP_NONE;
            case NAVAL_VEHICLE_DRIVER:
                return hasSkill(SkillType.S_PILOT_NVEE) ?
                             getSkill(SkillType.S_PILOT_NVEE).getExperienceLevel() :
                             SkillType.EXP_NONE;
            case VTOL_PILOT:
                return hasSkill(SkillType.S_PILOT_VTOL) ?
                             getSkill(SkillType.S_PILOT_VTOL).getExperienceLevel() :
                             SkillType.EXP_NONE;
            case VEHICLE_GUNNER:
                if (!campaignOptions.isUseArtillery()) {
                    return hasSkill(SkillType.S_GUN_VEE) ?
                                 getSkill(SkillType.S_GUN_VEE).getExperienceLevel() :
                                 SkillType.EXP_NONE;
                } else {
                    if ((hasSkill(SkillType.S_GUN_VEE)) && (hasSkill(SkillType.S_ARTILLERY))) {
                        return Math.max((getSkill(SkillType.S_GUN_VEE).getExperienceLevel()),
                              (getSkill(SkillType.S_ARTILLERY).getExperienceLevel()));
                    } else if (hasSkill(SkillType.S_GUN_VEE)) {
                        return getSkill(SkillType.S_GUN_VEE).getExperienceLevel();
                    } else if (hasSkill(SkillType.S_ARTILLERY)) {
                        return getSkill(SkillType.S_ARTILLERY).getExperienceLevel();
                    } else {
                        return SkillType.EXP_NONE;
                    }
                }
            case VEHICLE_CREW, MECHANIC:
                return hasSkill(S_TECH_MECHANIC) ? getSkill(S_TECH_MECHANIC).getExperienceLevel() :
                             SkillType.EXP_NONE;
            case AEROSPACE_PILOT:
                if (hasSkill(SkillType.S_GUN_AERO) && hasSkill(SkillType.S_PILOT_AERO)) {
                    if (isAlternativeQualityAveraging) {
                        int rawScore = (int) floor((getSkill(SkillType.S_GUN_AERO).getLevel() +
                                                          getSkill(SkillType.S_PILOT_AERO).getLevel()) / 2.0);
                        if (getSkill(SkillType.S_GUN_AERO).getType().getExperienceLevel(rawScore) ==
                                  getSkill(SkillType.S_PILOT_AERO).getType().getExperienceLevel(rawScore)) {
                            return getSkill(SkillType.S_GUN_AERO).getType().getExperienceLevel(rawScore);
                        }
                    }

                    return (int) floor((getSkill(SkillType.S_GUN_AERO).getExperienceLevel() +
                                              getSkill(SkillType.S_PILOT_AERO).getExperienceLevel()) / 2.0);
                } else {
                    return SkillType.EXP_NONE;
                }
            case CONVENTIONAL_AIRCRAFT_PILOT:
                if (hasSkill(SkillType.S_GUN_JET) && hasSkill(SkillType.S_PILOT_JET)) {
                    if (isAlternativeQualityAveraging) {
                        int rawScore = (int) floor((getSkill(SkillType.S_GUN_JET).getLevel() +
                                                          getSkill(SkillType.S_PILOT_JET).getLevel()) / 2.0);
                        if (getSkill(SkillType.S_GUN_JET).getType().getExperienceLevel(rawScore) ==
                                  getSkill(SkillType.S_PILOT_JET).getType().getExperienceLevel(rawScore)) {
                            return getSkill(SkillType.S_GUN_JET).getType().getExperienceLevel(rawScore);
                        }
                    }

                    return (int) floor((getSkill(SkillType.S_GUN_JET).getExperienceLevel() +
                                              getSkill(SkillType.S_PILOT_JET).getExperienceLevel()) / 2.0);
                } else {
                    return SkillType.EXP_NONE;
                }
            case PROTOMEK_PILOT:
                return hasSkill(SkillType.S_GUN_PROTO) ?
                             getSkill(SkillType.S_GUN_PROTO).getExperienceLevel() :
                             SkillType.EXP_NONE;
            case BATTLE_ARMOUR:
                if (hasSkill(SkillType.S_GUN_BA) && hasSkill(SkillType.S_ANTI_MEK)) {
                    if (isAlternativeQualityAveraging) {
                        int rawScore = (int) floor((getSkill(SkillType.S_GUN_BA).getLevel() +
                                                          getSkill(SkillType.S_ANTI_MEK).getLevel()) / 2.0);
                        if (getSkill(SkillType.S_GUN_BA).getType().getExperienceLevel(rawScore) ==
                                  getSkill(SkillType.S_ANTI_MEK).getType().getExperienceLevel(rawScore)) {
                            return getSkill(SkillType.S_GUN_BA).getType().getExperienceLevel(rawScore);
                        }
                    }

                    return (int) floor((getSkill(SkillType.S_GUN_BA).getExperienceLevel() +
                                              getSkill(SkillType.S_ANTI_MEK).getExperienceLevel()) / 2.0);
                } else {
                    return SkillType.EXP_NONE;
                }
            case SOLDIER:
                return hasSkill(SkillType.S_SMALL_ARMS) ?
                             getSkill(SkillType.S_SMALL_ARMS).getExperienceLevel() :
                             SkillType.EXP_NONE;
            case VESSEL_PILOT:
                return hasSkill(SkillType.S_PILOT_SPACE) ?
                             getSkill(SkillType.S_PILOT_SPACE).getExperienceLevel() :
                             SkillType.EXP_NONE;
            case VESSEL_GUNNER:
                return hasSkill(SkillType.S_GUN_SPACE) ?
                             getSkill(SkillType.S_GUN_SPACE).getExperienceLevel() :
                             SkillType.EXP_NONE;
            case VESSEL_CREW:
                return hasSkill(S_TECH_VESSEL) ? getSkill(S_TECH_VESSEL).getExperienceLevel() :
                             SkillType.EXP_NONE;
            case VESSEL_NAVIGATOR:
                return hasSkill(SkillType.S_NAV) ? getSkill(SkillType.S_NAV).getExperienceLevel() : SkillType.EXP_NONE;
            case MEK_TECH:
                return hasSkill(S_TECH_MEK) ? getSkill(S_TECH_MEK).getExperienceLevel() :
                             SkillType.EXP_NONE;
            case AERO_TEK:
                return hasSkill(S_TECH_AERO) ? getSkill(S_TECH_AERO).getExperienceLevel() :
                             SkillType.EXP_NONE;
            case BA_TECH:
                return hasSkill(S_TECH_BA) ? getSkill(S_TECH_BA).getExperienceLevel() :
                             SkillType.EXP_NONE;
            case ASTECH:
                return hasSkill(SkillType.S_ASTECH) ?
                             getSkill(SkillType.S_ASTECH).getExperienceLevel() :
                             SkillType.EXP_NONE;
            case DOCTOR:
                return hasSkill(SkillType.S_DOCTOR) ?
                             getSkill(SkillType.S_DOCTOR).getExperienceLevel() :
                             SkillType.EXP_NONE;
            case MEDIC:
                return hasSkill(SkillType.S_MEDTECH) ?
                             getSkill(SkillType.S_MEDTECH).getExperienceLevel() :
                             SkillType.EXP_NONE;
            case ADMINISTRATOR_COMMAND:
            case ADMINISTRATOR_LOGISTICS:
            case ADMINISTRATOR_TRANSPORT:
            case ADMINISTRATOR_HR:
                int adminLevel = getSkillLevelOrNegative(S_ADMIN);
                int negotiationLevel = getSkillLevelOrNegative(SkillType.S_NEG);
                int scroungeLevel = getSkillLevelOrNegative(SkillType.S_SCROUNGE);

                int levelSum;
                boolean includeNegotiation = campaign.getCampaignOptions().isAdminExperienceLevelIncludeNegotiation();
                boolean includeScrounge = campaign.getCampaignOptions().isAdminExperienceLevelIncludeScrounge();
                int divisor;

                if (includeNegotiation && includeScrounge) {
                    levelSum = adminLevel + negotiationLevel + scroungeLevel;
                    divisor = 3;
                } else if (includeNegotiation) {
                    levelSum = adminLevel + negotiationLevel;
                    divisor = 2;
                } else if (includeScrounge) {
                    levelSum = adminLevel + scroungeLevel;
                    divisor = 2;
                } else {
                    levelSum = adminLevel;
                    divisor = 1;
                }

                if (levelSum == -divisor) {
                    return SkillType.EXP_NONE;
                } else {
                    return Math.max(0, levelSum / divisor);
                }
            case DEPENDENT:
            case NONE:
            default:
                return SkillType.EXP_NONE;
        }
    }

    /**
     * Retrieves the skills associated with the character's profession. The skills returned depend on whether the
     * personnel's primary or secondary role is being queried and may also vary based on the campaign's configuration
     * settings, such as whether artillery skills are enabled.
     *
     * <p>This method identifies the {@link PersonnelRole} associated with the personnel and returns
     * a list of corresponding skills. The resulting skills depend on the profession and specific conditions, such as
     * whether artillery is enabled in the campaign options.</p>
     *
     * <p>Examples of skill mappings include:
     * <ul>
     *     <li><strong>MEKWARRIOR:</strong> Includes gun and piloting skills for meks, with optional
     *     artillery skills if enabled in the campaign.</li>
     *     <li><strong>LAM_PILOT:</strong> Covers skills for both meks and aerospace combat.</li>
     *     <li><strong>GROUND_VEHICLE_DRIVER:</strong> Includes piloting skills for ground vehicles.</li>
     *     <li><strong>VEHICLE_GUNNER:</strong> Includes vehicle gunnery skills, with optional artillery skills
     *         if enabled.</li>
     *     <li><strong>AEROSPACE_PILOT:</strong> Covers skills for aerospace gunnery and piloting.</li>
     *     <li><strong>ADMINISTRATORS:</strong> Includes administrative, negotiation, and scrounging skills.</li>
     *     <li><strong>DEPENDENT or NONE:</strong> Returns no specific skills.</li>
     * </ul>
     *
     * @param campaign  the current {@link Campaign}
     * @param secondary a boolean indicating whether to retrieve skills for the secondary ({@code true}) or primary
     *                  ({@code false}) profession of the character
     *
     * @return a {@link List} of skill identifiers ({@link String}) associated with the personnel's role, possibly
     *       modified by campaign settings
     */
    public List<String> getProfessionSkills(final Campaign campaign, final boolean secondary) {
        final PersonnelRole profession = secondary ? getSecondaryRole() : getPrimaryRole();
        final boolean isUseArtillery = campaign.getCampaignOptions().isUseArtillery();

        return switch (profession) {
            case MEKWARRIOR -> {
                if (isUseArtillery) {
                    yield List.of(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK, SkillType.S_ARTILLERY);
                } else {
                    yield List.of(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK);
                }
            }
            case LAM_PILOT ->
                  List.of(SkillType.S_GUN_MEK, SkillType.S_PILOT_MEK, SkillType.S_GUN_AERO, SkillType.S_PILOT_AERO);
            case GROUND_VEHICLE_DRIVER -> List.of(SkillType.S_PILOT_GVEE);
            case NAVAL_VEHICLE_DRIVER -> List.of(SkillType.S_PILOT_NVEE);
            case VTOL_PILOT -> List.of(SkillType.S_PILOT_VTOL);
            case VEHICLE_GUNNER -> {
                if (isUseArtillery) {
                    yield List.of(SkillType.S_GUN_VEE, SkillType.S_ARTILLERY);
                } else {
                    yield List.of(SkillType.S_GUN_VEE);
                }
            }
            case VEHICLE_CREW, MECHANIC -> List.of(S_TECH_MECHANIC);
            case AEROSPACE_PILOT -> List.of(SkillType.S_GUN_AERO, SkillType.S_PILOT_AERO);
            case CONVENTIONAL_AIRCRAFT_PILOT -> List.of(SkillType.S_GUN_JET, SkillType.S_PILOT_JET);
            case PROTOMEK_PILOT -> List.of(SkillType.S_GUN_PROTO, SkillType.S_GUN_PROTO);
            case BATTLE_ARMOUR -> List.of(SkillType.S_GUN_BA, SkillType.S_ANTI_MEK);
            case SOLDIER -> List.of(SkillType.S_SMALL_ARMS);
            case VESSEL_PILOT -> List.of(SkillType.S_PILOT_SPACE);
            case VESSEL_GUNNER -> List.of(SkillType.S_GUN_SPACE);
            case VESSEL_CREW -> List.of(S_TECH_VESSEL);
            case VESSEL_NAVIGATOR -> List.of(SkillType.S_NAV);
            case MEK_TECH -> List.of(S_TECH_MEK);
            case AERO_TEK -> List.of(S_TECH_AERO);
            case BA_TECH -> List.of(S_TECH_BA);
            case ASTECH -> List.of(SkillType.S_ASTECH);
            case DOCTOR -> List.of(SkillType.S_DOCTOR);
            case MEDIC -> List.of(SkillType.S_MEDTECH);
            case ADMINISTRATOR_COMMAND, ADMINISTRATOR_LOGISTICS, ADMINISTRATOR_TRANSPORT, ADMINISTRATOR_HR ->
                  List.of(S_ADMIN, SkillType.S_NEG, SkillType.S_SCROUNGE);
            case DEPENDENT, NONE -> List.of(String.valueOf(SkillType.EXP_NONE));
        };
    }

    /**
     * @param campaign the campaign the person is a part of
     *
     * @return a full description in HTML format that will be used for the graphical display in the personnel table
     *       among other places
     */
    public String getFullDesc(final Campaign campaign) {
        return "<b>" + getFullTitle() + "</b><br/>" + getSkillLevel(campaign, false) + ' ' + getRoleDesc();
    }

    public String getHTMLTitle() {
        return String.format("<html><div id=\"%s\" style=\"white-space: nowrap;\">%s</div></html>",
              getId(),
              getFullTitle());
    }

    /**
     * Constructs and returns the full title by combining the rank and full name. If the rank is not available or an
     * exception occurs while retrieving it, the method will only return the full name.
     *
     * @return the full title as a combination of rank and full name, or just the full name if the rank is unavailable
     */
    public String getFullTitle() {
        String rank = "";

        try {
            rank = getRankName();

            if (!rank.isBlank()) {
                rank = rank + ' ';
            }
        } catch (Exception ignored) {
            // This try-catch exists to allow us to more easily test Person objects. Previously, if
            // a method included 'getFullTitle' it would break if the Person object hadn't been
            // assigned a Rank System.
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

    public TargetRollModifier getHealingMods(final Campaign campaign) {
        return new TargetRollModifier(getHealingDifficulty(campaign), "difficulty");
    }

    public String fail() {
        return " <font color='" +
                     MekHQ.getMHQOptions().getFontColorNegativeHexColor() +
                     "'><b>Failed to heal.</b></font>";
    }

    // region skill
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

    /**
     * @param skillName The name of the skill to retrieve the level for.
     *
     * @return the skill level of a person for a given skill, or -1 if the person does not have the skill.
     */
    public int getSkillLevelOrNegative(final String skillName) {
        if (hasSkill(skillName)) {
            return getSkill(skillName).getExperienceLevel();
        } else {
            return -1;
        }
    }

    public void addSkill(final String skillName, final Skill skill) {
        skills.addSkill(skillName, skill);
    }

    public void addSkill(final String skillName, final int level, final int bonus) {
        skills.addSkill(skillName, new Skill(skillName, level, bonus));
    }

    public void addSkill(final String skillName, final int level, final int bonus, final int ageModifier) {
        skills.addSkill(skillName, new Skill(skillName, level, bonus, ageModifier));
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

    /**
     * @deprecated Use {@link #getCostToImprove(String, boolean)} instead.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public int getCostToImprove(final String skillName) {
        return getCostToImprove(skillName, false);
    }

    /**
     * Calculates the cost to improve a specific skill, with an optional reasoning multiplier.
     *
     * <p>If the skill exists, the cost is based on its current level's improvement cost.</p>
     *
     * <p>If the skill does not exist, the method calculates the cost using the default cost for the skill type at
     * level 0.</p>
     *
     * @param skillName    the name of the skill for which to calculate the improvement cost.
     * @param useReasoning a boolean indicating whether to apply {@link Reasoning} cost multipliers.
     *
     * @return the cost to improve the skill, adjusted by the reasoning multiplier if applicable, or the cost for level
     *       0 if the specified skill does not currently exist.
     */
    public int getCostToImprove(final String skillName, final boolean useReasoning) {
        final Skill skill = getSkill(skillName);
        final SkillType skillType = getType(skillName);
        int cost = hasSkill(skillName) ? skill.getCostToImprove() : skillType.getCost(0);

        double multiplier = getReasoningXpCostMultiplier(useReasoning);

        if (options.booleanOption(FLAW_SLOW_LEARNER)) {
            multiplier += 0.2;
        }

        if (options.booleanOption(ATOW_FAST_LEARNER)) {
            multiplier -= 0.2;
        }

        if (skillType.isAffectedByGremlinsOrTechEmpathy()) {
            if (options.booleanOption(FLAW_GREMLINS)) {
                multiplier += 0.1;
            }

            if (options.booleanOption(ATOW_TECH_EMPATHY)) {
                multiplier -= 0.1;
            }
        }

        return (int) round(cost * multiplier);
    }
    // endregion skill

    // region Awards
    public PersonAwardController getAwardController() {
        return awardController;
    }
    // endregion Awards

    public int getHits() {
        return hits;
    }

    public void setHits(final int hits) {
        this.hits = hits;
    }

    /**
     * @return the number of hits sustained prior to the last completed scenario.
     */
    public int getHitsPrior() {
        return hitsPrior;
    }

    /**
     * Sets the number of hits sustained prior to the last completed scenario.
     *
     * @param hitsPrior the new value for {@code hitsPrior}
     */
    public void setHitsPrior(final int hitsPrior) {
        this.hitsPrior = hitsPrior;
    }

    /**
     * @return <code>true</code> if the location (or any of its parent locations)
     *       has an injury which implies that the location (most likely a limb) is severed. By checking parents we can
     *       tell that they should be missing from the parent being severed, like a hand is missing if the corresponding
     *       arms is.
     */
    public boolean isLocationMissing(final @Nullable BodyLocation location) {
        return (location != null) &&
                     (getInjuriesByLocation(location).stream()
                            .anyMatch(injury -> injury.getType().impliesMissingLocation()) ||
                            isLocationMissing(location.Parent()));
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

    /**
     * @deprecated No longer in use
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public String succeed() {
        heal();
        return " <font color='" +
                     MekHQ.getMHQOptions().getFontColorPositiveHexColor() +
                     "'><b>Successfully healed one hit.</b></font>";
    }

    // region Personnel Options
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
                    if (!adv.isEmpty()) {
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

        return (abilityString.isEmpty()) ? null : "<html>" + abilityString + "</html>";
    }
    // endregion Personnel Options

    // region edge

    /**
     * Retrieves the edge value for the current person.
     *
     * <p><b>Usage:</b> This method gets the character's raw Edge score. Generally you likely want to use
     * {@link #getAdjustedEdge()} instead, as that includes adjustments for the character's {@code unlucky} trait.</p>
     *
     * @return The edge value defined in the person's options.
     */
    public int getEdge() {
        return getOptions().intOption(OptionsConstants.EDGE);
    }

    /**
     * Retrieves the adjusted edge value for the current person.
     *
     * <p>The adjusted Edge value is calculated by subtracting the person's level of bad luck (unlucky)
     * from their base Edge value.</p>
     *
     * @return The adjusted edge value after accounting for the person's level of bad luck.
     */
    public int getAdjustedEdge() {
        return getOptions().intOption(OptionsConstants.EDGE) - unlucky;
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
     * Resets edge points to the purchased level. Used for weekly refresh.
     */
    public void resetCurrentEdge() {
        setCurrentEdge(getAdjustedEdge());
    }

    /**
     * Sets edge points to the value 'currentEdge'. Used for weekly refresh.
     *
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
            // yuck, it would be nice to have a more fool-proof way of identifying edge
            // triggers
            if (ability.getName().contains("edge_when") && ability.booleanValue()) {
                stringBuilder.append(ability.getDescription()).append("<br>");
            }
        }

        return stringBuilder.toString().isBlank() ? "No triggers set" : "<html>" + stringBuilder + "</html>";
    }
    // endregion edge

    public boolean canDrive(final Entity entity) {
        if (entity instanceof LandAirMek) {
            return hasSkill(SkillType.S_PILOT_MEK) && hasSkill(SkillType.S_PILOT_AERO);
        } else if (entity instanceof Mek) {
            return hasSkill(SkillType.S_PILOT_MEK);
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
        } else if (entity instanceof ProtoMek) {
            return hasSkill(SkillType.S_GUN_PROTO);
        } else {
            return false;
        }
    }

    public boolean canGun(final Entity entity) {
        if (entity instanceof LandAirMek) {
            return hasSkill(SkillType.S_GUN_MEK) && hasSkill(SkillType.S_GUN_AERO);
        } else if (entity instanceof Mek) {
            return hasSkill(SkillType.S_GUN_MEK);
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
        } else if (entity instanceof ProtoMek) {
            return hasSkill(SkillType.S_GUN_PROTO);
        } else {
            return false;
        }
    }

    public boolean canTech(final Entity entity) {
        if (entity == null) {
            return false;
        }
        if ((entity instanceof Mek) || (entity instanceof ProtoMek)) {
            return hasSkill(S_TECH_MEK);
        } else if (entity instanceof Dropship || entity instanceof Jumpship) {
            return hasSkill(S_TECH_VESSEL);
        } else if (entity instanceof Aero) {
            return hasSkill(S_TECH_AERO);
        } else if (entity instanceof BattleArmor) {
            return hasSkill(S_TECH_BA);
        } else if (entity instanceof Tank) {
            return hasSkill(S_TECH_MECHANIC);
        } else {
            return false;
        }
    }

    /**
     * @deprecated Use {@link #getDailyAvailableTechTime(boolean)}.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public int getDailyAvailableTechTime() {
        return getDailyAvailableTechTime(false);
    }

    /**
     * Calculates and retrieves the current daily available tech time for the person.
     *
     * <p>This calculation does not account for any expended time but incorporates potential administrative
     * adjustments if specified.</p>
     *
     * <p>The calculation follows these rules:</p>
     * <ul>
     *   <li>If the person's primary role is a technician, the base support time is determined from the primary
     *   role.</li>
     *   <li>Otherwise, the base support time is taken from the secondary role.</li>
     * </ul>
     *
     * <p>If administrative adjustments are enabled (via the {@code isTechsUseAdministration} parameter),
     * the support time is multiplied by an administrative adjustment multiplier.</p>
     *
     * @param isTechsUseAdministration A boolean flag indicating whether administrative adjustments should be applied in
     *                                 the calculation.
     *
     * @return The adjusted daily available tech time for the person, after factoring in the appropriate role support
     *       time, applying the administrative multiplier (if enabled), and deducting maintenance time.
     */
    public int getDailyAvailableTechTime(final boolean isTechsUseAdministration) {
        int baseTime = (getPrimaryRole().isTech() ? PRIMARY_ROLE_SUPPORT_TIME : SECONDARY_ROLE_SUPPORT_TIME);

        return (int) round(baseTime * calculateTechTimeMultiplier(isTechsUseAdministration));
    }

    public int getMaintenanceTimeUsing() {
        return getTechUnits().stream()
                     .filter(unit -> !(unit.isRefitting() && unit.getRefit().getTech() == this))
                     .mapToInt(Unit::getMaintenanceTime)
                     .sum();
    }

    public boolean isMothballing() {
        return isTech() && techUnits.stream().anyMatch(Unit::isMothballing);
    }

    /**
     * Determines whether this {@code Person} is considered "busy" based on their current status, unit assignment, and
     * associated tasks.
     *
     * <p>This method checks:</p>
     * <ol>
     *     <li>If the personnel is active (i.e., has an active {@link PersonnelStatus}).</li>
     *     <li>Special cases for units that are self-crewed, including activities such as
     *         mothballing, refitting, or undergoing repairs, during which crew members are
     *         considered busy.</li>
     *     <li>If the personnel is a technician, by reviewing their current tech assignments,
     *         such as units being mothballed, refitted, or repaired.</li>
     *     <li>If the personnel has a unit assignment and whether that unit is currently deployed.</li>
     * </ol>
     *
     * @return {@code true} if the person is deemed busy due to one of the above conditions; {@code false} otherwise.
     */
    public boolean isBusy() {
        // Personnel status
        if (!status.isActive()) {
            return false;
        }

        final boolean hasUnitAssignment = unit != null;
        final Entity entity = hasUnitAssignment ? unit.getEntity() : null;
        final boolean isSpecialCase = entity != null && unit.isSelfCrewed();

        // Special case handlers (self crewed units have their tech teams formed as a composite of their crew, so all
        // crew are considered to be busy during these states)
        if (isSpecialCase) {
            if (unit.isMothballing()) {
                return true;
            }

            if (unit.isRefitting()) {
                return true;
            }

            if (unit.isUnderRepair()) {
                return true;
            }
        }

        // Tech assignments
        if (isTech()) {
            for (Unit unit : techUnits) {
                Refit refit = unit.getRefit();
                boolean isActiveTech = refit != null && Objects.equals(refit.getTech(), this);

                if (unit.isMothballing() && isActiveTech) {
                    return true;
                }

                if (unit.isRefitting() && isActiveTech) {
                    return true;
                }

                if (unit.isUnderRepair()) {
                    for (Part part : unit.getParts()) {
                        if (Objects.equals(part.getTech(), this)) {
                            return true;
                        }
                    }
                }
            }
        }

        // Unit assignments
        if (hasUnitAssignment) {
            return unit.isDeployed();
        }

        return false;
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
                part.cancelAssignment(true);
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
            // set minutes for all crew members, except the engineer to not cause infinite recursion.
            getUnit().getActiveCrew().stream().filter(this::isNotSelf).forEach(p -> p.setMinutesLeft(minutesLeft));
        }
    }

    /**
     * Checks if the other person is not the same person as this person, easy right?
     *
     * @param p Person to check against
     *
     * @return true if the person is not the same person as this person
     */
    private boolean isNotSelf(Person p) {
        return !this.equals(p);
    }

    public int getOvertimeLeft() {
        return overtimeLeft;
    }

    public void setOvertimeLeft(final int overtimeLeft) {
        this.overtimeLeft = overtimeLeft;
        if (engineer && (getUnit() != null)) {
            getUnit().getActiveCrew().stream().filter(this::isNotSelf).forEach(p -> p.setOvertimeLeft(overtimeLeft));
        }
    }

    /**
     * @deprecated Use {@link #resetMinutesLeft(boolean)}.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public void resetMinutesLeft() {
        resetMinutesLeft(false);
    }

    /**
     * Resets the number of minutes and overtime minutes a person has left for tasks, based on their primary or
     * secondary role. Administrative adjustments may be applied for technicians if specified.
     *
     * <p>This method calculates and assigns task and overtime time values depending on whether
     * the person is identified as a technician or doctor, and whether their role is primary or secondary. If
     * administrative adjustments are enabled (via the {@code isTechsUseAdministration} parameter), a multiplier is
     * applied to calculate the adjusted task time for technicians.</p>
     *
     * <ul>
     *   <li>If the primary role is a doctor, the base support time values for the primary role
     *       are assigned without any adjustments.</li>
     *   <li>If the secondary role is a doctor, the base support time values for the secondary role
     *       are assigned without any adjustments.</li>
     *   <li>If the primary role is a technician and administrative adjustments are enabled, the primary
     *       role's support time is multiplied by the administrative adjustment multiplier and assigned.</li>
     *   <li>If the secondary role is a technician (secondary-specific), and administrative adjustments
     *       are enabled, the secondary role's support time is multiplied by the adjustment multiplier and assigned.</li>
     *   <li>If administrative adjustments are not enabled for technicians, base (non-adjusted) time values
     *       are used for both primary and secondary roles.</li>
     * </ul>
     *
     * <p>If the person has both primary and secondary roles applicable (e.g., a doctor as the primary
     * and a technician as the secondary), the logic prioritizes the roles as listed above, with primary roles
     * taking precedence.</p>
     *
     * @param isTechsUseAdministration Indicates whether administrative adjustments should be applied to the time
     *                                 calculations for technicians.
     */
    public void resetMinutesLeft(boolean isTechsUseAdministration) {
        // Doctors and Technicians without adjustments
        if (primaryRole.isDoctor() || (primaryRole.isTech() && !isTechsUseAdministration)) {
            this.minutesLeft = PRIMARY_ROLE_SUPPORT_TIME;
            this.overtimeLeft = PRIMARY_ROLE_OVERTIME_SUPPORT_TIME;
        } else if (secondaryRole.isDoctor() || (secondaryRole.isTech() && !isTechsUseAdministration)) {
            this.minutesLeft = SECONDARY_ROLE_SUPPORT_TIME;
            this.overtimeLeft = SECONDARY_ROLE_OVERTIME_SUPPORT_TIME;
        }

        // Technicians with adjustments
        if (primaryRole.isTech()) {
            double techTimeMultiplier = calculateTechTimeMultiplier(isTechsUseAdministration);

            this.minutesLeft = (int) round(PRIMARY_ROLE_SUPPORT_TIME * techTimeMultiplier);
            this.overtimeLeft = (int) round(PRIMARY_ROLE_OVERTIME_SUPPORT_TIME * techTimeMultiplier);
        } else if (secondaryRole.isTechSecondary()) {
            double techTimeMultiplier = calculateTechTimeMultiplier(isTechsUseAdministration);

            this.minutesLeft = (int) round(SECONDARY_ROLE_SUPPORT_TIME * techTimeMultiplier);
            this.overtimeLeft = (int) round(SECONDARY_ROLE_OVERTIME_SUPPORT_TIME * techTimeMultiplier);
        }
    }

    public Skill getBestTechSkill() {
        Skill skill = null;
        int lvl = SkillType.EXP_NONE;
        if (hasSkill(S_TECH_MEK) && getSkill(S_TECH_MEK).getExperienceLevel() > lvl) {
            skill = getSkill(S_TECH_MEK);
            lvl = getSkill(S_TECH_MEK).getExperienceLevel();
        }
        if (hasSkill(S_TECH_AERO) && getSkill(S_TECH_AERO).getExperienceLevel() > lvl) {
            skill = getSkill(S_TECH_AERO);
            lvl = getSkill(S_TECH_AERO).getExperienceLevel();
        }
        if (hasSkill(S_TECH_MECHANIC) && getSkill(S_TECH_MECHANIC).getExperienceLevel() > lvl) {
            skill = getSkill(S_TECH_MECHANIC);
            lvl = getSkill(S_TECH_MECHANIC).getExperienceLevel();
        }
        if (hasSkill(S_TECH_BA) && getSkill(S_TECH_BA).getExperienceLevel() > lvl) {
            skill = getSkill(S_TECH_BA);
        }
        return skill;
    }

    public boolean isTech() {
        return isTechMek() || isTechAero() || isTechMechanic() || isTechBA();
    }

    /**
     * Checks if the person is a tech, includes mektek, mechanic, aerotek, BAtek and the non-cannon "large vessel tek"
     *
     * @return true if the person is a tech
     */
    public boolean isTechExpanded() {
        return isTechMek() || isTechAero() || isTechMechanic() || isTechBA() || isTechLargeVessel();
    }

    public boolean isTechLargeVessel() {
        boolean hasSkill = hasSkill(S_TECH_VESSEL);
        return hasSkill && (getPrimaryRole().isVesselCrew() || getSecondaryRole().isVesselCrew());
    }

    public boolean isTechMek() {
        boolean hasSkill = hasSkill(S_TECH_MEK);
        return hasSkill && (getPrimaryRole().isMekTech() || getSecondaryRole().isMekTech());
    }

    public boolean isTechAero() {
        boolean hasSkill = hasSkill(S_TECH_AERO);
        return hasSkill && (getPrimaryRole().isAeroTek() || getSecondaryRole().isAeroTek());
    }

    public boolean isTechMechanic() {
        boolean hasSkill = hasSkill(S_TECH_MECHANIC);
        return hasSkill && (getPrimaryRole().isMechanic() || getSecondaryRole().isMechanic());
    }

    public boolean isTechBA() {
        boolean hasSkill = hasSkill(S_TECH_BA);
        return hasSkill && (getPrimaryRole().isBATech() || getSecondaryRole().isBATech());
    }

    /**
     * Calculates the tech availability time multiplier for tasks based on the technician's experience level and
     * administration skill.
     *
     * <p>The method considers whether administration skills should be applied to improve efficiency. If
     * administration is enabled, the multiplier is adjusted based on the technician's baseline experience level and
     * their administration skill level.</p>
     *
     * @param isTechsUseAdministration {@code true} if administration skills are considered for task calculation;
     *                                 {@code false} otherwise.
     *
     * @return the calculated time multiplier, where:
     *       <ul>
     *         <li>0.0 indicates the person is not a technician.</li>
     *         <li>1.0 indicates no adjustment is applied.</li>
     *         <li>Values greater or less than 1.0 adjust task times accordingly.</li>
     *       </ul>
     */
    public double calculateTechTimeMultiplier(boolean isTechsUseAdministration) {
        final double TECH_ADMINISTRATION_MULTIPLIER = 0.05;
        final int REGULAR_EXPERIENCE_LEVEL = REGULAR.getExperienceLevel();

        if (!isTechExpanded()) {
            return 0;
        }

        if (!isTechsUseAdministration) {
            return 1.0;
        }

        double administrationMultiplier = 1.0 - (TECH_ADMINISTRATION_MULTIPLIER * REGULAR_EXPERIENCE_LEVEL);

        Skill administration = skills.getSkill(S_ADMIN);
        int experienceLevel = SkillLevel.NONE.getExperienceLevel();

        if (administration != null) {
            experienceLevel = administration.getExperienceLevel();
        }

        administrationMultiplier += experienceLevel * TECH_ADMINISTRATION_MULTIPLIER;

        return administrationMultiplier;
    }

    public boolean isAdministrator() {
        return (getPrimaryRole().isAdministrator() || getSecondaryRole().isAdministrator());
    }

    public boolean isDoctor() {
        return hasSkill(SkillType.S_DOCTOR) && (getPrimaryRole().isDoctor() || getSecondaryRole().isDoctor());
    }

    /**
     * Calculates the medical capacity of a doctor based on their administrative skills, and the base number of hospital
     * beds they are responsible for. If the entity represented is not a doctor, the capacity is returned as 0.
     *
     * @param doctorsUseAdministration A flag indicating whether the doctor's administrative skills should be considered
     *                                 in the calculation. If {@code true}, administrative skills are included in the
     *                                 performance multiplier adjustment. If {@code false}, {@code baseBedCount} is
     *                                 returned, instead.
     * @param baseBedCount             The base number of hospital beds assigned to the doctor. This value is adjusted
     *                                 by the calculated multiplier to determine the doctor's effective capacity.
     *
     * @return The calculated medical capacity of the doctor, as an {@link Integer} representing their ability to
     *       effectively manage hospital beds. If the entity is not a doctor, returns {@code 0}.
     */
    public int getDoctorMedicalCapacity(final boolean doctorsUseAdministration, final int baseBedCount) {
        final double DOCTOR_ADMINISTRATION_MULTIPLIER = 0.2;
        final int REGULAR_EXPERIENCE_LEVEL = REGULAR.getExperienceLevel();

        if (!isDoctor()) {
            return 0;
        }

        if (!doctorsUseAdministration) {
            return baseBedCount;
        }

        double administrationMultiplier = 1.0 - (DOCTOR_ADMINISTRATION_MULTIPLIER * REGULAR_EXPERIENCE_LEVEL);

        Skill administration = skills.getSkill(S_ADMIN);
        int experienceLevel = SkillLevel.NONE.getExperienceLevel();

        if (administration != null) {
            experienceLevel = administration.getExperienceLevel();
        }

        administrationMultiplier += experienceLevel * DOCTOR_ADMINISTRATION_MULTIPLIER;

        return (int) round(baseBedCount * administrationMultiplier);
    }

    public boolean isSupport() {
        return !isCombat();
    }

    public boolean isCombat() {
        return getPrimaryRole().isCombat() || getSecondaryRole().isCombat();
    }

    public boolean isDependent() {
        return (getPrimaryRole().isDependent() || getSecondaryRole().isDependent());
    }

    public boolean isTaskOvertime(final IPartWork partWork) {
        return (partWork.getTimeLeft() > getMinutesLeft()) && (getOvertimeLeft() > 0);
    }

    /**
     * Selects the most suitable {@link Skill} for working on the specified part.
     *
     * <p>This method first attempts to find the best applicable skill that directly matches the technical type
     * required
     * by the part, choosing the one with the lowest target number for the task. If no matching skill is found, it falls
     * back to select the best available general technical skill, again prioritizing the lowest target number among
     * possessed skills.</p>
     *
     * @param part the {@link IPartWork} that work is to be performed on
     *
     * @return the most appropriate {@link Skill} to use for working on the given part, or {@code null} if none is found
     */
    public Skill getSkillForWorkingOn(final IPartWork part) {
        final Unit unit = part.getUnit();
        Skill skill = getSkillForWorkingOn(unit);
        if (skill != null) {
            return skill;
        }

        String[] skillTypeNames = { S_TECH_MEK, S_TECH_BA, S_TECH_AERO, S_TECH_MECHANIC, S_TECH_VESSEL };

        Skill bestSkill = null;
        int bestValue = Integer.MAX_VALUE;

        // First: among tech types specifically matching part requirement
        for (String typeName : skillTypeNames) {
            if (part.isRightTechType(typeName) && hasSkill(typeName)) {
                Skill candidate = getSkill(typeName);
                int value = determineTargetNumber(this, candidate.getType(), 0).getValue();
                if (bestSkill == null || value < bestValue) {
                    bestSkill = candidate;
                    bestValue = value;
                }
            }
        }
        if (bestSkill != null) {
            return bestSkill;
        }

        // Second: among any tech skills possessed
        for (String typeName : skillTypeNames) {
            if (hasSkill(typeName)) {
                Skill candidate = getSkill(typeName);
                int value = determineTargetNumber(this, candidate.getType(), 0).getValue();
                if (bestSkill == null || value < bestValue) {
                    bestSkill = candidate;
                    bestValue = value;
                }
            }
        }

        return bestSkill;
    }

    public @Nullable Skill getSkillForWorkingOn(final @Nullable Unit unit) {
        if (unit == null) {
            return null;
        } else if (((unit.getEntity() instanceof Mek) || (unit.getEntity() instanceof ProtoMek)) &&
                                                   hasSkill(S_TECH_MEK)) {
            return getSkill(S_TECH_MEK);
        } else if ((unit.getEntity() instanceof BattleArmor) && hasSkill(S_TECH_BA)) {
            return getSkill(S_TECH_BA);
        } else if ((unit.getEntity() instanceof Tank) && hasSkill(S_TECH_MECHANIC)) {
            return getSkill(S_TECH_MECHANIC);
        } else if (((unit.getEntity() instanceof Dropship) || (unit.getEntity() instanceof Jumpship)) &&
                         hasSkill(S_TECH_VESSEL)) {
            return getSkill(S_TECH_VESSEL);
        } else if ((unit.getEntity() instanceof Aero) &&
                         !(unit.getEntity() instanceof Dropship) &&
                         !(unit.getEntity() instanceof Jumpship) && hasSkill(S_TECH_AERO)) {
            return getSkill(S_TECH_AERO);
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
        final Skill mekSkill = getSkill(S_TECH_MEK);
        final Skill mechanicSkill = getSkill(S_TECH_MECHANIC);
        final Skill baSkill = getSkill(S_TECH_BA);
        final Skill aeroSkill = getSkill(S_TECH_AERO);
        if ((mekSkill != null) && (mekSkill.getLevel() > level)) {
            level = mekSkill.getLevel();
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
            return (hasSkill(S_TECH_MEK) && part.isRightTechType(S_TECH_MEK)) ||
                         (hasSkill(S_TECH_AERO) && part.isRightTechType(S_TECH_AERO)) ||
                         (hasSkill(S_TECH_MECHANIC) && part.isRightTechType(S_TECH_MECHANIC)) ||
                         (hasSkill(S_TECH_BA) && part.isRightTechType(S_TECH_BA)) ||
                         (hasSkill(S_TECH_VESSEL) && part.isRightTechType(S_TECH_VESSEL));
        } else if ((unit.getEntity() instanceof Mek) || (unit.getEntity() instanceof ProtoMek)) {
            return hasSkill(S_TECH_MEK);
        } else if (unit.getEntity() instanceof BattleArmor) {
            return hasSkill(S_TECH_BA);
        } else if ((unit.getEntity() instanceof Tank) || (unit.getEntity() instanceof Infantry)) {
            return hasSkill(S_TECH_MECHANIC);
        } else if ((unit.getEntity() instanceof Dropship) || (unit.getEntity() instanceof Jumpship)) {
            return hasSkill(S_TECH_VESSEL);
        } else if (unit.getEntity() instanceof Aero) {
            return hasSkill(S_TECH_AERO);
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

    public int getConnections() {
        return connections;
    }

    public void setConnections(final int connections) {
        this.connections = clamp(connections, MINIMUM_CONNECTIONS, MAXIMUM_CONNECTIONS);
    }

    /**
     * Adjusts the person's Connections score by the specified amount.
     *
     * <p>The change in connections can be positive or negative, depending on the provided delta value.</p>
     *
     * @param delta The amount by which to adjust the number of connections. A positive value increases the connections,
     *              while a negative value decreases them.
     */
    public void changeConnections(final int delta) {
        int newValue = connections + delta;
        connections = clamp(newValue, MINIMUM_CONNECTIONS, MAXIMUM_CONNECTIONS);
    }

    public int getWealth() {
        return wealth;
    }

    public void setWealth(final int wealth) {
        this.wealth = clamp(wealth, MINIMUM_REPUTATION, MAXIMUM_REPUTATION);
    }

    /**
     * Adjusts the person's wealth by the specified amount.
     *
     * <p>The change in wealth can be positive or negative, depending on the provided delta value.</p>
     *
     * @param delta The amount by which to adjust the wealth. A positive value increases the wealth, while a negative
     *              value decreases it.
     */
    public void changeWealth(final int delta) {
        int newValue = wealth + delta;
        wealth = clamp(newValue, MINIMUM_WEALTH, MAXIMUM_WEALTH);
    }

    public boolean isHasPerformedExtremeExpenditure() {
        return hasPerformedExtremeExpenditure;
    }

    public void setHasPerformedExtremeExpenditure(final boolean hasPerformedExtremeExpenditure) {
        this.hasPerformedExtremeExpenditure = hasPerformedExtremeExpenditure;
    }

    /**
     * Retrieves the raw reputation value of the character.
     *
     * <p>This method returns the unadjusted reputation value associated with the character.</p>
     *
     * <p><b>Usage:</b> If aging effects are enabled, you likely want to use
     * {@link #getAdjustedReputation(boolean, boolean, LocalDate, int)}  instead.</p>
     *
     * @return The raw reputation value.
     */
    public int getReputation() {
        return reputation;
    }

    /**
     * Calculates the adjusted reputation value for the character based on aging effects, the current campaign type,
     * date, and rank.
     *
     * <p>This method computes the character's reputation by applying age-based modifiers, which depend on factors such
     * as whether aging effects are enabled, whether the campaign is clan-specific, the character's bloodname status,
     * and their rank in the clan hierarchy. If aging effects are disabled, the reputation remains unchanged.</p>
     *
     * <p><b>Usage:</b> If aging effects are disabled, the result will be equivalent to the base reputation value
     * provided by {@link #getReputation()}.</p>
     *
     * @param isUseAgingEffects Indicates whether aging effects should be applied to the reputation calculation.
     * @param isClanCampaign    Indicates whether the current campaign is specific to a clan.
     * @param today             The current date used to calculate the character's age.
     * @param rankIndex         The rank index of the character, which can adjust the reputation modifier in clan-based
     *                          campaigns.
     *
     * @return The adjusted reputation value, accounting for factors like age, clan campaign status, bloodname
     *       possession, and rank. If aging effects are disabled, the base reputation value is returned.
     */
    public int getAdjustedReputation(boolean isUseAgingEffects, boolean isClanCampaign, LocalDate today,
          int rankIndex) {
        return reputation +
                     (isUseAgingEffects ?
                            getReputationAgeModifier(getAge(today), isClanCampaign, !bloodname.isBlank(), rankIndex) :
                            0);
    }

    public void setReputation(final int reputation) {
        this.reputation = clamp(reputation, MINIMUM_REPUTATION, MAXIMUM_REPUTATION);
    }

    /**
     * Adjusts the person's reputation by the specified amount.
     *
     * <p>The change in reputation can be positive or negative, depending on the provided delta value.</p>
     *
     * @param delta The amount by which to adjust the reputation. A positive value increases the reputation, while a
     *              negative value decreases it.
     */
    public void changeReputation(final int delta) {
        int newValue = reputation + delta;
        reputation = clamp(newValue, MINIMUM_REPUTATION, MAXIMUM_REPUTATION);
    }

    public int getUnlucky() {
        return unlucky;
    }

    public void setUnlucky(final int unlucky) {
        this.unlucky = clamp(unlucky, MINIMUM_UNLUCKY, MAXIMUM_UNLUCKY);
    }

    public void changeUnlucky(final int delta) {
        int newValue = unlucky + delta;
        unlucky = clamp(newValue, MINIMUM_UNLUCKY, MAXIMUM_UNLUCKY);
    }

    /**
     * Retrieves the character's {@link Attributes} object containing the character's attribute scores.
     *
     * <p><b>Usage:</b> In most cases you'll want to use {@link #getAttributeScore(SkillAttribute)} instead, as that
     * will allow you to jump straight to the exact score you need.</p>
     *
     * @return the character's {@link Attributes} object.
     *
     * @since 0.50.5
     */
    public Attributes getATOWAttributes() {
        return atowAttributes;
    }

    /**
     * Updates the score for a specific skill attribute.
     *
     * <p>This method sets the provided score for the given {@link SkillAttribute}. If the attribute is
     * <code>null</code> or represents "NONE", the method logs a warning and exits without making any changes.</p>
     *
     * <p>The actual attribute score update is delegated to the underlying attribute handler.</p>
     *
     * @param attribute The {@link SkillAttribute} to be updated. Must not be <code>null</code> or "NONE".
     * @param newScore  The new score to assign to the specified skill attribute.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public void setAttributeScore(final SkillAttribute attribute, final int newScore) {
        if (attribute == null || attribute == SkillAttribute.NONE) {
            logger.warn("(setAttributeScore) SkillAttribute is null or NONE.");
            return;
        }

        atowAttributes.setAttributeScore(phenotype, options, attribute, newScore);
    }

    /**
     * Retrieves the score of a specified attribute.
     *
     * @param attribute the {@link SkillAttribute} to retrieve the score for.
     *
     * @return the score of the specified attribute, or {@link Attributes#DEFAULT_ATTRIBUTE_SCORE} if the attribute is
     *       {@code NONE} or {@code null}.
     *
     * @since 0.50.5
     */
    public int getAttributeScore(final SkillAttribute attribute) {
        if (attribute == null || attribute.isNone()) {
            logger.error("(getAttributeScore) SkillAttribute is null or NONE.");
            return DEFAULT_ATTRIBUTE_SCORE;
        }

        boolean hasFreakishStrength = options.booleanOption(MUTATION_FREAKISH_STRENGTH);
        boolean hasExoticAppearance = options.booleanOption(MUTATION_EXOTIC_APPEARANCE);
        boolean hasFacialHair = options.booleanOption(MUTATION_FACIAL_HAIR);
        boolean hasSeriousDisfigurement = options.booleanOption(MUTATION_SERIOUS_DISFIGUREMENT);
        boolean isCatGirl = options.booleanOption(MUTATION_CAT_GIRL);
        boolean isCatGirlUnofficial = options.booleanOption(MUTATION_CAT_GIRL_UNOFFICIAL);

        return switch (attribute) {
            case NONE -> 0;
            case STRENGTH -> {
                int attributeScore = atowAttributes.getAttributeScore(attribute);
                if (hasFreakishStrength) {
                    attributeScore += 2;
                }
                yield min(attributeScore, MAXIMUM_ATTRIBUTE_SCORE);
            }
            case BODY, REFLEXES, DEXTERITY, INTELLIGENCE, WILLPOWER -> atowAttributes.getAttributeScore(attribute);
            case CHARISMA -> {
                int attributeScore = atowAttributes.getAttributeScore(attribute);
                if (hasExoticAppearance) {
                    attributeScore++;
                }
                if (hasFacialHair) {
                    attributeScore--;
                }
                if (hasSeriousDisfigurement) {
                    attributeScore -= 3;
                }
                if (isCatGirl) {
                    attributeScore -= 3;
                }
                if (isCatGirlUnofficial) {
                    attributeScore++;
                }
                yield clamp(attributeScore, MINIMUM_ATTRIBUTE_SCORE, MAXIMUM_ATTRIBUTE_SCORE);
            }
        };
    }

    /**
     * Retrieves the maximum allowed value (cap) for the specified {@link SkillAttribute}.
     *
     * <p>If the attribute is {@code null} or marked as {@link SkillAttribute#NONE}, a default maximum attribute score
     * is returned, and a warning is logged.</p>
     *
     * <p>For valid attributes, this method delegates to
     * {@link Attributes#getAttributeCap(Phenotype, PersonnelOptions, SkillAttribute)}.</p>
     *
     * @param attribute The {@link SkillAttribute} for which the maximum value is being retrieved. Must not be
     *                  {@code null} or {@link SkillAttribute#NONE}.
     *
     * @return The maximum allowed value (cap) for the given attribute. Returns the default maximum value if the input
     *       attribute is invalid.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public int getAttributeCap(final SkillAttribute attribute) {
        if (attribute == null || attribute.isNone()) {
            logger.warn("(getAttributeCap) SkillAttribute is null or NONE.");
            return MAXIMUM_ATTRIBUTE_SCORE;
        }

        return atowAttributes.getAttributeCap(phenotype, options, attribute);
    }

    /**
     * Sets the character's {@link Attributes} object which contains their ATOW Attribute scores.
     *
     * <p><b>Usage:</b> This completely wipes the character's attribute scores and is likely not the method you're
     * looking for. Consider{@link #changeAttributeScore(SkillAttribute, int)} if you just want to increment or
     * decrement a specific attribute by a certain value.</p>
     *
     * @param atowAttributes the {@link Attributes} object to set.
     *
     * @since 0.50.5
     */
    public void setATOWAttributes(final Attributes atowAttributes) {
        this.atowAttributes = atowAttributes;
    }

    /**
     * Modifies the score of a specified skill attribute by a given delta value.
     *
     * <p>This method adjusts the current score of the provided {@link SkillAttribute} by adding the specified delta
     * to it. If the attribute is {@code null} or {@link SkillAttribute#NONE}, a warning is logged, and the method exits
     * without making any changes.</p>
     *
     * <p>The new score is computed as the sum of the current score and the delta, and it is passed
     * to {@link Attributes#setAttributeScore(Phenotype, PersonnelOptions, SkillAttribute, int)} to ensure is compiles
     * with the character's minimum and maximum attribute score values.</p>
     *
     * @param attribute The {@link SkillAttribute} whose score is to be modified. Must not be <code>null</code>.
     * @param delta     The value to add to the current score of the specified skill attribute.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public void changeAttributeScore(final SkillAttribute attribute, final int delta) {
        if (attribute == null || attribute.isNone()) {
            logger.warn("(changeAttributeScore) SkillAttribute is null or NONE.");
            return;
        }

        int current = atowAttributes.getAttributeScore(attribute);
        int newScore = current + delta;

        setAttributeScore(attribute, newScore);
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

    /**
     * @deprecated use {@link #getPersonalLog()} instead.
     */
    @Deprecated(forRemoval = true, since = "0.50.5")
    public List<LogEntry> getPersonnelLog() {
        return getPersonalLog();
    }

    public List<LogEntry> getPersonalLog() {
        personnelLog.sort(Comparator.comparing(LogEntry::getDate));
        return personnelLog;
    }

    public List<LogEntry> getMedicalLog() {
        medicalLog.sort(Comparator.comparing(LogEntry::getDate));
        return medicalLog;
    }

    public List<LogEntry> getScenarioLog() {
        scenarioLog.sort(Comparator.comparing(LogEntry::getDate));
        return scenarioLog;
    }

    public List<LogEntry> getAssignmentLog() {
        assignmentLog.sort(Comparator.comparing(LogEntry::getDate));
        return assignmentLog;
    }

    public List<LogEntry> getPerformanceLog() {
        performanceLog.sort(Comparator.comparing(LogEntry::getDate));
        return performanceLog;
    }

    /**
     * @deprecated use {@link #addPersonalLogEntry(LogEntry)} instead.
     */
    @Deprecated(forRemoval = true, since = "0.50.5")
    public void addLogEntry(final LogEntry entry) {
        addPersonalLogEntry(entry);
    }

    public void addPersonalLogEntry(final LogEntry entry) {
        personnelLog.add(entry);
    }

    public void addMedicalLogEntry(final LogEntry entry) {
        medicalLog.add(entry);
    }

    public void addScenarioLogEntry(final LogEntry entry) {
        scenarioLog.add(entry);
    }

    public void addAssignmentLogEntry(final LogEntry entry) {
        assignmentLog.add(entry);
    }

    public void addPerformanceLogEntry(final LogEntry entry) {
        performanceLog.add(entry);
    }

    // region injuries

    /**
     * All methods below are for the Advanced Medical option
     */

    public List<Injury> getInjuries() {
        return new ArrayList<>(injuries);
    }

    public List<Injury> getPermanentInjuries() {
        return injuries.stream().filter(Injury::isPermanent).collect(Collectors.toList());
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
        return !injuries.isEmpty() &&
                     injuries.stream().anyMatch(injury -> (injury.getTime() > 0) || !injury.isPermanent());
    }

    /**
     * Calculates the total injury modifier for the pilot, based on the character's injuries and ambidextrous trait (if
     * present). This modifier can apply to either piloting or gunnery checks depending on the input parameter.
     *
     * <p>This method examines all injuries and their associated modifiers, distinguishing between left-side and
     * right-side injuries if the character is ambidextrous, and the injury implies a missing body location. If the
     * character is not ambidextrous, all modifiers are considered uniformly.</p>
     *
     * <p>The method performs the following steps:</p>
     * <ul>
     *    <li>If the character is ambidextrous and the injury implies a missing location:
     *        <ul>
     *            <li>Classifies injuries into left-side or right-side based on their body location.</li>
     *            <li>Adds associated modifiers to separate lists for left-side and right-side injuries.</li>
     *            <li>If injuries are only present on one side, the modifiers for the opposite side are removed.</li>
     *        </ul>
     *    </li>
     *    <li>If the character is not ambidextrous or the injury does not imply a missing body location all modifiers
     *    from all injuries are included without distinguishing between left and right sides.</li>
     * </ul>
     *
     * <p>After processing the injuries, the method calculates the total injury modifier by summing up the relevant
     * modifier values, taking into account whether the modifier applies to piloting or gunnery checks.</p>
     *
     * @param isPiloting A boolean value indicating whether the modifier calculation is for piloting checks
     *                   ({@code true}) or gunnery checks ({@code false}).
     *
     * @return The total injury modifier calculated from the character's injuries, specific to piloting or gunnery.
     *
     * @author Illiani
     * @since 0.50.05
     */
    public int getInjuryModifiers(boolean isPiloting) {
        boolean isAmbidextrous = options.booleanOption(ATOW_AMBIDEXTROUS);

        List<Modifier> leftSideModifiers = new ArrayList<>();
        List<Modifier> rightSideModifiers = new ArrayList<>();

        List<Modifier> allModifiers = new ArrayList<>();
        for (Injury injury : injuries) {
            boolean isLeftSide = false;
            boolean isRightSide = false;
            if (isAmbidextrous && injury.getType().impliesMissingLocation()) {
                BodyLocation location = injury.getLocation();
                if (location.isLimb()) {
                    if (location == BodyLocation.LEFT_ARM || location == BodyLocation.LEFT_HAND) {
                        isLeftSide = true;
                    } else if (location == BodyLocation.RIGHT_ARM || location == BodyLocation.RIGHT_HAND) {
                        isRightSide = true;
                    }
                }
            }

            for (Modifier modifier : injury.getModifiers()) {
                if (isAmbidextrous) {
                    if (isLeftSide) {
                        leftSideModifiers.add(modifier);
                    }

                    if (isRightSide) {
                        rightSideModifiers.add(modifier);
                    }
                }

                allModifiers.add(modifier);
            }
        }

        if (isAmbidextrous) {
            if (leftSideModifiers.isEmpty() && !rightSideModifiers.isEmpty()) {
                allModifiers.removeAll(rightSideModifiers);
            }

            if (rightSideModifiers.isEmpty() && !leftSideModifiers.isEmpty()) {
                allModifiers.removeAll(leftSideModifiers);
            }
        }

        return Modifier.calcTotalModifier(allModifiers.stream(),
              isPiloting ? ModifierValue.PILOTING : ModifierValue.GUNNERY);
    }

    /**
     * @deprecated use {@link #getInjuryModifiers(boolean)} instead.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public int getPilotingInjuryMod() {
        return getInjuryModifiers(true);
    }

    /**
     * @deprecated use {@link #getInjuryModifiers(boolean)} instead.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public int getGunneryInjuryMod() {
        return getInjuryModifiers(false);
    }

    public boolean hasInjuries(final boolean permanentCheck) {
        return !injuries.isEmpty() &&
                     (!permanentCheck ||
                            injuries.stream().anyMatch(injury -> !injury.isPermanent() || (injury.getTime() > 0)));
    }

    public boolean hasOnlyHealedPermanentInjuries() {
        return !injuries.isEmpty() &&
                     injuries.stream().noneMatch(injury -> !injury.isPermanent() || (injury.getTime() > 0));
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
    // endregion injuries

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
        if (unit == null) {
            originalUnitId = null;
            originalUnitTech = 0;
            originalUnitWeight = 0;

            return;
        }

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
     *
     * @param campaign     the campaign the person is a part of
     * @param sharesForAll true if all combat and support personnel have shares, otherwise false if just MekWarriors
     *                     have shares
     *
     * @return the number of shares the person has
     */
    public int getNumShares(final Campaign campaign, final boolean sharesForAll) {
        if (!getStatus().isActive() ||
                  !getPrisonerStatus().isFree() ||
                  (!sharesForAll && !hasRole(PersonnelRole.MEKWARRIOR))) {
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
     *
     * @return the ransom value of this individual Useful for prisoner who you want to ransom or hand off to your
     *       employer in an AtB context
     */
    public Money getRansomValue(final Campaign campaign) {
        // MekWarriors and aero pilots are worth more than the other types of scrubs
        return (getPrimaryRole().isMekWarriorGrouping() || getPrimaryRole().isAerospacePilot() ?
                      MEKWARRIOR_AERO_RANSOM_VALUES :
                      OTHER_RANSOM_VALUES).get(getExperienceLevel(campaign, false));
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
                logger.error(String.format("Person %s ('%s') references missing unit %s", getId(), getFullName(), id));
            }
        }

        for (int ii = techUnits.size() - 1; ii >= 0; --ii) {
            final Unit techUnit = techUnits.get(ii);
            if (techUnit instanceof PersonUnitRef) {
                final Unit realUnit = campaign.getUnit(techUnit.getId());
                if (realUnit != null) {
                    techUnits.set(ii, realUnit);
                } else {
                    logger.error("Person {} ('{}') techs missing unit {}", getId(), getFullName(), techUnit.getId());
                    techUnits.remove(ii);
                }
            }
        }
    }

    /**
     * Generates the loyalty modifier for a given loyalty score.
     *
     * @param loyalty the person's loyalty score
     */
    public int getLoyaltyModifier(int loyalty) {
        if (loyalty < 1) {
            loyalty = 1;
        }

        return switch (loyalty) {
            case 1, 2, 3 -> 3;
            case 4 -> 2;
            case 5, 6 -> 1;
            case 7, 8, 9, 10, 11, 12, 13, 14 -> 0;
            case 15, 16 -> -1;
            case 17 -> -2;
            default -> -3;
        };
    }

    /**
     * @deprecated replaced by {@link #getIntelligenceXpCostMultiplier(boolean)} which was then replaced by
     *       {@link #getReasoningXpCostMultiplier(boolean)}.
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public double getIntelligenceXpCostMultiplier(CampaignOptions campaignOptions) {
        return getReasoningXpCostMultiplier(campaignOptions.isUseReasoningXpMultiplier());
    }

    /**
     * @deprecated replaced by {@link #getReasoningXpCostMultiplier(boolean)}
     */
    @Deprecated(since = "0.50.05", forRemoval = true)
    public double getIntelligenceXpCostMultiplier(final boolean useReasoningXpCostMultiplier) {
        return getReasoningXpCostMultiplier(useReasoningXpCostMultiplier);
    }

    /**
     * Calculates the experience cost multiplier based on reasoning.
     *
     * <p>If reasoning adjustment is not enabled, the multiplier is 1 (no effect).</p>
     *
     * <p>Otherwise, the multiplier is determined by the reasoning score, where each point adjusts the cost by 2.5%.
     * A neutral reasoning score (resulting in a modifier of 0) will also return a multiplier of 1.</p>
     *
     * @param useReasoningXpCostMultiplier a {@link Boolean} indicating whether to apply the reasoning-based adjustment
     *                                     to the experience cost.
     *
     * @return the experience cost multiplier: - `1` if reasoning adjustment is disabled or {@link Reasoning} is
     *       neutral. - A value adjusted by the formula `1 - (score * 0.025)` otherwise.
     */
    public double getReasoningXpCostMultiplier(final boolean useReasoningXpCostMultiplier) {
        Reasoning reasoning = getReasoning();

        if (!useReasoningXpCostMultiplier || reasoning.isAverageType()) {
            return 1;
        }

        double reasoningMultiplier = 0.025; // each rank in Reasoning should adjust costs by 2.5%

        int score = reasoning.getReasoningScore();
        double modifier = score * reasoningMultiplier;

        if (modifier == 0) { // neutral reasoning
            return 1;
        } else {
            return 1 - modifier;
        }
    }
}
