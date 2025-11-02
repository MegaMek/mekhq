/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
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
import static megamek.codeUtilities.StringUtility.isNullOrBlank;
import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static megamek.common.enums.SkillLevel.REGULAR;
import static megamek.common.icons.Portrait.DEFAULT_IMAGE_WIDTH;
import static megamek.common.icons.Portrait.DEFAULT_PORTRAIT_FILENAME;
import static megamek.common.icons.Portrait.NO_PORTRAIT_NAME;
import static mekhq.MHQConstants.BATTLE_OF_TUKAYYID;
import static mekhq.campaign.log.LogEntryType.ASSIGNMENT;
import static mekhq.campaign.log.LogEntryType.MEDICAL;
import static mekhq.campaign.log.LogEntryType.PATIENT;
import static mekhq.campaign.log.LogEntryType.PERFORMANCE;
import static mekhq.campaign.personnel.PersonnelOptions.*;
import static mekhq.campaign.personnel.enums.BloodGroup.getRandomBloodGroup;
import static mekhq.campaign.personnel.medical.BodyLocation.INTERNAL;
import static mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes.CATATONIA;
import static mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes.CHILDLIKE_REGRESSION;
import static mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes.CRIPPLING_FLASHBACKS;
import static mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes.DISCONTINUATION_SYNDROME;
import static mekhq.campaign.personnel.skills.Aging.getReputationAgeModifier;
import static mekhq.campaign.personnel.skills.Attributes.DEFAULT_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MAXIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.Attributes.MINIMUM_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.InfantryGunnerySkills.INFANTRY_GUNNERY_SKILLS;
import static mekhq.campaign.personnel.skills.SkillType.*;
import static mekhq.campaign.randomEvents.personalities.PersonalityController.generateReasoning;
import static mekhq.campaign.randomEvents.personalities.PersonalityController.getTraitIndex;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.swing.ImageIcon;

import megamek.Version;
import megamek.client.generator.RandomNameGenerator;
import megamek.codeUtilities.MathUtility;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.TargetRollModifier;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Portrait;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.*;
import megamek.logging.MMLogger;
import megamek.utilities.ImageUtilities;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.ExtraData;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.events.persons.PersonStatusChangedEvent;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.Force;
import mekhq.campaign.log.LogEntry;
import mekhq.campaign.log.LogEntryFactory;
import mekhq.campaign.log.LogEntryType;
import mekhq.campaign.log.PersonalLogger;
import mekhq.campaign.log.ServiceLogger;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.personnel.enums.*;
import mekhq.campaign.personnel.enums.education.EducationLevel;
import mekhq.campaign.personnel.enums.education.EducationStage;
import mekhq.campaign.personnel.familyTree.Genealogy;
import mekhq.campaign.personnel.generator.DefaultPersonnelGenerator;
import mekhq.campaign.personnel.generator.SingleSpecialAbilityGenerator;
import mekhq.campaign.personnel.medical.BodyLocation;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryUtil;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.AdvancedMedicalAlternate;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.InjuryEffect;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.RankValidator;
import mekhq.campaign.personnel.ranks.Ranks;
import mekhq.campaign.personnel.skills.Attributes;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.Skills;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;
import mekhq.campaign.personnel.skills.enums.SkillSubType;
import mekhq.campaign.randomEvents.personalities.PersonalityController;
import mekhq.campaign.randomEvents.personalities.enums.Aggression;
import mekhq.campaign.randomEvents.personalities.enums.Ambition;
import mekhq.campaign.randomEvents.personalities.enums.Greed;
import mekhq.campaign.randomEvents.personalities.enums.PersonalityQuirk;
import mekhq.campaign.randomEvents.personalities.enums.PersonalityTraitType;
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

    public static final String BLOODMARK_LABEL = "BLOODMARK";
    public static final int MINIMUM_BLOODMARK = 0;
    public static final int MAXIMUM_BLOODMARK = 5;

    public static final String EXTRA_INCOME_LABEL = "EXTRA_INCOME";
    public static final int MINIMUM_EXTRA_INCOME = ExtraIncome.NEGATIVE_TEN.getTraitLevel();
    public static final int MAXIMUM_EXTRA_INCOME = ExtraIncome.POSITIVE_TEN.getTraitLevel();

    public static final int CONNECTIONS_TARGET_NUMBER = 4; // Arbitrary value

    private static final String DELIMITER = "::";


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
    private List<LogEntry> patientLog;
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
    private boolean hasGainedVeterancySPA;
    private int toughness;
    private Attributes atowAttributes;

    // If new Traits are added, make sure to also add them to LifePathDataTraitLookup
    private int connections;
    private int wealth;
    private ExtraIncome extraIncome;
    private boolean hasPerformedExtremeExpenditure;
    private int reputation;
    private int unlucky;
    private int bloodmark;
    private List<LocalDate> bloodhuntSchedule;

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
    private String personalityDescription;
    private String personalityInterviewNotes;
    private Reasoning reasoning;
    // endregion Personality

    // region SPAs
    private String storedGivenName;
    private String storedSurname;
    private int storedLoyalty;
    private Faction storedOriginFaction;
    private Aggression storedAggression;
    private int storedAggressionDescriptionIndex;
    private Ambition storedAmbition;
    private int storedAmbitionDescriptionIndex;
    private Greed storedGreed;
    private int storedGreedDescriptionIndex;
    private Social storedSocial;
    private int storedSocialDescriptionIndex;
    private PersonalityQuirk storedPersonalityQuirk;
    private int storedPersonalityQuirkDescriptionIndex;
    private Reasoning storedReasoning;
    private boolean sufferingFromClinicalParanoia;
    private boolean darkSecretRevealed;
    private LocalDate burnedConnectionsEndDate;
    // endregion SPAs

    // region Flags
    private boolean clanPersonnel;
    private boolean commander;
    private boolean divorceable;
    private boolean founder; // +1 share if using shares system
    private boolean immortal;
    private boolean quickTrainIgnore;
    // this is a flag used in determine whether a person is a potential marriage candidate provided that they are not
    // married, are old enough, etc.
    @Deprecated(since = "0.50.10", forRemoval = true)
    private boolean marriageable;
    private boolean prefersMen;
    private boolean prefersWomen;
    // this is a flag used in random procreation to determine whether to attempt to
    // procreate
    private boolean tryingToConceive;
    private boolean hidePersonality;
    // endregion Flags

    // Generic extra data, for use with plugins and mods
    private ExtraData extraData;

    /** @deprecated Use {@link #RESOURCE_BUNDLE} instead for all new strings */
    @Deprecated(since = "0.50.10", forRemoval = false)
    private final static ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel",
          MekHQ.getMHQOptions().getLocale());
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Personnel";
    private static final MMLogger LOGGER = MMLogger.create(Person.class);

    // initializes the AtB ransom values
    static {
        MEKWARRIOR_AERO_RANSOM_VALUES = new HashMap<>();

        // no official AtB rules for really inexperienced scrubs, but...
        MEKWARRIOR_AERO_RANSOM_VALUES.put(EXP_NONE, Money.of(2500));

        // no official AtB rules for really inexperienced scrubs, but...
        MEKWARRIOR_AERO_RANSOM_VALUES.put(EXP_ULTRA_GREEN, Money.of(5000));

        MEKWARRIOR_AERO_RANSOM_VALUES.put(EXP_GREEN, Money.of(10000));
        MEKWARRIOR_AERO_RANSOM_VALUES.put(EXP_REGULAR, Money.of(25000));
        MEKWARRIOR_AERO_RANSOM_VALUES.put(EXP_VETERAN, Money.of(50000));
        MEKWARRIOR_AERO_RANSOM_VALUES.put(EXP_ELITE, Money.of(100000));
        MEKWARRIOR_AERO_RANSOM_VALUES.put(EXP_HEROIC, Money.of(150000));
        MEKWARRIOR_AERO_RANSOM_VALUES.put(EXP_LEGENDARY, Money.of(200000));

        OTHER_RANSOM_VALUES = new HashMap<>();
        OTHER_RANSOM_VALUES.put(EXP_NONE, Money.of(1250));
        OTHER_RANSOM_VALUES.put(EXP_ULTRA_GREEN, Money.of(2500));
        OTHER_RANSOM_VALUES.put(EXP_GREEN, Money.of(5000));
        OTHER_RANSOM_VALUES.put(EXP_REGULAR, Money.of(10000));
        OTHER_RANSOM_VALUES.put(EXP_VETERAN, Money.of(25000));
        OTHER_RANSOM_VALUES.put(EXP_ELITE, Money.of(50000));
        OTHER_RANSOM_VALUES.put(EXP_HEROIC, Money.of(100000));
        OTHER_RANSOM_VALUES.put(EXP_LEGENDARY, Money.of(150000));
    }
    // endregion Variable Declarations

    // region Constructors
    public Person(final UUID id) {
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
        hasGainedVeterancySPA = false;
        connections = 0;
        wealth = 0;
        extraIncome = ExtraIncome.ZERO;
        hasPerformedExtremeExpenditure = false;
        reputation = 0;
        unlucky = 0;
        bloodmark = 0;
        bloodhuntSchedule = new ArrayList<>();
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
        patientLog = new ArrayList<>();
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
        personalityDescription = "";
        personalityInterviewNotes = "";
        storedLoyalty = 0;
        storedAggression = Aggression.NONE;
        storedAggressionDescriptionIndex = 0;
        storedAmbition = Ambition.NONE;
        storedAmbitionDescriptionIndex = 0;
        storedGreed = Greed.NONE;
        storedGreedDescriptionIndex = 0;
        storedSocial = Social.NONE;
        storedSocialDescriptionIndex = 0;
        storedPersonalityQuirk = PersonalityQuirk.NONE;
        storedPersonalityQuirkDescriptionIndex = 0;
        storedReasoning = Reasoning.AVERAGE;
        sufferingFromClinicalParanoia = false;
        darkSecretRevealed = false;
        burnedConnectionsEndDate = null;

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
        setQuickTrainIgnore(false);
        setPrefersMen(false);
        setPrefersWomen(false);
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

    public @Nullable String getBloodname() {
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

    Faction getStoredOriginFaction() {
        return storedOriginFaction;
    }

    void setStoredOriginFaction(final Faction originFaction) {
        this.storedOriginFaction = originFaction;
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
                    setRecruitment(campaign.getLocalDate());
                    setLastRankChangeDate(campaign.getLocalDate());
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
     * overrule a surname, but we do not disallow surnames for clan personnel, if the player wants to input them
     *
     * @return a String of the person's last name
     */
    public String getLastName() {
        String lastName = !isNullOrBlank(getBloodname()) ?
                                getBloodname() :
                                !isNullOrBlank(getSurname()) ? getSurname() : "";
        if (!isNullOrBlank(getPostNominal())) {
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

    String getStoredGivenName() {
        return storedGivenName;
    }

    void setStoredGivenName(String storedGivenName) {
        this.storedGivenName = storedGivenName;
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

    String getStoredSurname() {
        return storedSurname;
    }

    void setStoredSurname(String storedSurname) {
        this.storedSurname = storedSurname;
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

                if (!(!isNullOrBlank(getBloodname()) && getBloodname().equals(name[i]))) {
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

    /**
     * Retrieves the portrait object associated with this entity.
     *
     * <p>Consider using {@link #getPortraitImageIconWithFallback(boolean)}, instead.</p>
     *
     * @return the {@link Portrait} object representing the visual representation of this entity
     */
    public Portrait getPortrait() {
        return portrait;
    }

    /**
     * Retrieves the portrait image for a given entity. If the provided condition enables the use of an origin faction
     * backup and the portrait image is unavailable or matches default filenames, a fallback image is retrieved based on
     * the origin faction's logo.
     *
     * @param useOriginFactionBackup a boolean flag indicating whether to use the origin faction backup for the portrait
     *                               image if the primary portrait is unavailable or invalid
     *
     * @return the portrait image for the entity; if a fallback is required based on the condition, the fallback image
     *       generated from the origin faction's logo is returned
     *
     * @author Illiani
     * @since 0.50.10
     */
    public ImageIcon getPortraitImageIconWithFallback(boolean useOriginFactionBackup) {
        if (useOriginFactionBackup) {
            if (portrait == null) {
                return getFallbackPortrait();
            }

            String portraitFilename = portrait.getFilename();
            if (portraitFilename.equalsIgnoreCase(DEFAULT_PORTRAIT_FILENAME) ||
                      portraitFilename.equalsIgnoreCase(NO_PORTRAIT_NAME)) {
                return getFallbackPortrait();
            }
        }

        return (portrait == null) ? new ImageIcon() : portrait.getImageIcon();
    }

    /**
     * Retrieves a fallback portrait image when no specific portrait is available.
     *
     * <p>This method generates a fallback image by using the faction logo corresponding to the person's origin
     * faction and birth year. The logo is scaled to the default image width while maintaining aspect ratio.</p>
     *
     * @return A scaled {@link ImageIcon} containing the faction logo as a fallback portrait
     *
     * @author Illiani
     * @since 0.50.10
     */
    private ImageIcon getFallbackPortrait() {
        ImageIcon fallbackImage = Factions.getFactionLogo(birthday.getYear(), originFaction.getShortName());
        return ImageUtilities.scaleImageIcon(fallbackImage, DEFAULT_IMAGE_WIDTH, true);
    }

    public void setPortrait(final Portrait portrait) {
        this.portrait = Objects.requireNonNull(portrait, "Illegal assignment: cannot have a null Portrait");
    }

    // region Personnel Roles
    public PersonnelRole getPrimaryRole() {
        return primaryRole;
    }

    /**
     * Use {@link #setPrimaryRole(LocalDate, PersonnelRole)} instead
     */
    @Deprecated(since = "0.50.07") // we need to remove the uses before removal
    public void setPrimaryRole(final Campaign campaign, final PersonnelRole primaryRole) {
        // don't need to do any processing for no changes
        if (primaryRole == getPrimaryRole()) {
            return;
        }

        // Now, we can perform the time in service and last rank change tracking change for dependents
        if (!primaryRole.isCivilian() && recruitment == null) {
            setRecruitment(campaign.getLocalDate());
            setLastRankChangeDate(campaign.getLocalDate());
        }

        // Finally, we can set the primary role
        setPrimaryRoleDirect(primaryRole);

        // and trigger the update event
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    /**
     * Sets the primary role for this person as of the given date.
     *
     * <p>If the new primary role differs from the current primary role, this method updates internal state as
     * follows:</p>
     * <ul>
     *     <li>If the new role is not civilian and this person does not already have a recruitment date, assigns
     *     the provided date as both recruitment and last-rank-change dates.</li>
     *     <li>Updates the person's primary role to the provided value.</li>
     *     <li>Triggers a {@link PersonChangedEvent} so that relevant systems are notified of the change.</li>
     * </ul>
     *
     * <p><b>Usage tip:</b> If it’s unclear whether this person is eligible for the new role, call
     * {@code canPerformRole(LocalDate, PersonnelRole, boolean)} before this method.</p>
     *
     * @param today       the current in-game date, used for setting recruitment and rank-change dates if required
     * @param primaryRole the new {@link PersonnelRole} to be set as primary for this person
     */
    public void setPrimaryRole(final LocalDate today, final PersonnelRole primaryRole) {
        // don't need to do any processing for no changes
        if (primaryRole == getPrimaryRole()) {
            return;
        }

        // Now, we can perform the time in service and last rank change tracking change for dependents
        if (!primaryRole.isCivilian() && recruitment == null) {
            setRecruitment(today);
            setLastRankChangeDate(today);
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

    /**
     * Returns an HTML-formatted string describing the primary and, if applicable, secondary personnel roles. Civilian
     * roles are displayed in italics. If a secondary role is present and is not {@code NONE}, it is appended to the
     * description, separated by a slash. The description is wrapped in HTML tags.
     *
     * @return an HTML-formatted string describing the personnel roles, with civilian roles shown in italics
     *
     * @author Illiani
     * @since 0.50.06
     */
    public String getFormatedRoleDescriptions(LocalDate today) {
        StringBuilder description = new StringBuilder("<html>");

        if (!isEmployed()) {
            description.append("\u25CF ");
        }

        String primaryDesc = getPrimaryRoleDesc();

        if (primaryRole.isSubType(PersonnelRoleSubType.CIVILIAN)) {
            if (primaryRole.isNone()) {
                // Error state: emphasize the issue
                description.append("<b><i><u>").append(primaryDesc.toUpperCase()).append("</u></i></b>");
            } else if (primaryRole.isDependent()) {
                String label;
                if (status.isStudent()) {
                    label = status.getLabel();
                } else if (isChild(today)) {
                    label = resources.getString("relationChild.text");
                } else {
                    label = primaryDesc;
                }
                description.append("<i>").append(label).append("</i>");
            } else {
                description.append("<i>").append(primaryDesc).append("</i>");
            }
        } else {
            description.append(primaryDesc);
        }

        if (!secondaryRole.isNone()) {
            description.append(" / ");
            String secondaryDesc = getSecondaryRoleDesc();
            if (secondaryRole.isSubType(PersonnelRoleSubType.CIVILIAN)) {
                description.append("<i>").append(secondaryDesc).append("</i>");
            } else {
                description.append(secondaryDesc);
            }
        }

        description.append("</html>");
        return description.toString();
    }

    public String getSecondaryRoleDesc() {
        return getSecondaryRole().getLabel(isClanPersonnel());
    }

    /**
     * Determines if this person can perform the specified {@link PersonnelRole} as either a primary or secondary role
     * on the given date.
     *
     * <p>For primary roles, certain constraints are enforced, such as uniqueness compared to the secondary role and
     * limitations based on the type of role (e.g., tech, medical, administrator).</p>
     *
     * <p>For secondary roles, different restrictions apply, including the ability to always select "None" and
     * disallowing dependent roles.</p>
     *
     * <p>Additionally, the person's age and required skill sets are considered to ensure eligibility for the chosen
     * role.</p>
     *
     * @param today   the {@link LocalDate} representing the current date, used for age-based checks
     * @param role    the {@link PersonnelRole} being considered for assignment
     * @param primary {@code true} to check eligibility as a primary role, {@code false} for secondary
     *
     * @return {@code true} if the person is eligible to perform the given role as specified; {@code false} otherwise
     */
    public boolean canPerformRole(LocalDate today, final PersonnelRole role, final boolean primary) {
        if (primary) {
            // Primary Role:
            // 1) Can always be Dependent
            // 2) Cannot be None
            // 3) Cannot be equal to the secondary role
            // 4) Cannot be a tech role if the secondary role is a tech role (inc. Astech)
            // 5) Cannot be a medical if the secondary role is one of the medical staff roles
            // 6) Cannot be an admin role if the secondary role is one of the administrator roles
            if (role.isDependent()) {
                return true;
            }

            if (role.isNone()) {
                return false;
            }

            if (role == secondaryRole) {
                return false;
            }

            if (role.isTech() && (secondaryRole.isTech() || secondaryRole.isAstech())) {
                return false;
            }

            if (role.isMedicalStaff() && secondaryRole.isMedicalStaff()) {
                return false;
            }

            if (role.isAdministrator() && secondaryRole.isAdministrator()) {
                return false;
            }
        } else {
            // Secondary Role:
            // 1) Can always be None
            // 2) Cannot be Dependent
            // 3) Cannot be equal to the primary role
            // 4) Cannot be a tech role if the primary role is a tech role (inc. Astech)
            // 5) Cannot be a medical role if the primary role is one of the medical staff roles
            // 6) Cannot be an admin role if the primary role is one of the administrator roles
            if (role.isNone()) {
                return true;
            }

            if (role.isDependent()) {
                return false;
            }

            if (role == primaryRole) {
                return false;
            }

            if (role.isTech() && (primaryRole.isTech() || primaryRole.isAstech())) {
                return false;
            }

            if (role.isMedicalStaff() && primaryRole.isMedicalStaff()) {
                return false;
            }

            if (role.isAdministrator() && primaryRole.isAdministrator()) {
                return false;
            }
        }

        if (isChild(today)) {
            return false;
        }

        List<String> skillsForProfession = role.getSkillsForProfession();
        return switch (role) {
            case SOLDIER -> INFANTRY_GUNNERY_SKILLS.stream().anyMatch(this::hasSkill);
            case BATTLE_ARMOUR -> hasSkill(S_GUN_BA);
            case VESSEL_CREW -> hasSkill(S_TECH_VESSEL);
            case MEK_TECH -> hasSkill(S_TECH_MEK);
            case AERO_TEK -> hasSkill(S_TECH_AERO);
            case BA_TECH -> hasSkill(S_TECH_BA);
            case DOCTOR -> hasSkill(S_SURGERY);
            case ADMINISTRATOR_COMMAND, ADMINISTRATOR_LOGISTICS, ADMINISTRATOR_TRANSPORT, ADMINISTRATOR_HR ->
                  hasSkill(S_ADMIN);
            case ADULT_ENTERTAINER -> {
                // A character under the age of 18 should never have access to this profession
                if (isChild(today, true)) {
                    yield false;
                } else {
                    yield hasSkill(S_ART_OTHER) && hasSkill(S_ACTING);
                }
            }
            case LUXURY_COMPANION -> {
                // A character under the age of 18 should never have access to this profession
                if (isChild(today, true)) {
                    yield false;
                } else {
                    yield hasSkill(S_ACTING) && hasSkill(S_PROTOCOLS);
                }
            }
            default -> {
                for (String skillName : skillsForProfession) {
                    if (!hasSkill(skillName)) {
                        yield false;
                    }
                }

                yield true;
            }
        };
    }

    /**
     * Validates and updates the primary and secondary roles of this person for the given campaign.
     *
     * <p>This method checks if the current primary and secondary roles can be performed based on the campaign's
     * local date. If the person is not eligible for their primary role, it will be set to
     * {@link PersonnelRole#DEPENDENT}. If they cannot perform their secondary role, it will be set to
     * {@link PersonnelRole#NONE}.
     *
     * @param campaign the {@link Campaign} context used for validation, particularly the local date
     */
    public void validateRoles(Campaign campaign) {
        if (!primaryRole.isNone()) {
            boolean canPerform = canPerformRole(campaign.getLocalDate(), primaryRole, true);

            if (!canPerform) {
                setPrimaryRole(campaign, PersonnelRole.DEPENDENT);
            }
        }

        if (!secondaryRole.isNone()) {
            boolean canPerform = canPerformRole(campaign.getLocalDate(), secondaryRole, false);

            if (!canPerform) {
                setSecondaryRole(PersonnelRole.NONE);
            }
        }
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
            case ACTIVE -> {
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
            }
            case RETIRED -> {
                campaign.addReport(String.format(status.getReportText(), getHyperlinkedFullTitle()));
                ServiceLogger.retired(this, today);

                setRetirement(today);
            }
            case RESIGNED -> {
                campaign.addReport(String.format(status.getReportText(), getHyperlinkedFullTitle()));
                ServiceLogger.resigned(this, today);

                setRetirement(today);
            }
            case DESERTED -> {
                campaign.addReport(String.format(status.getReportText(), getHyperlinkedFullTitle()));
                ServiceLogger.deserted(this, today);

                setRetirement(today);
            }
            case DEFECTED -> {
                campaign.addReport(String.format(status.getReportText(), getHyperlinkedFullTitle()));
                ServiceLogger.defected(this, today);

                setRetirement(today);
            }
            case CAMP_FOLLOWER, SACKED -> {
                campaign.addReport(String.format(status.getReportText(), getHyperlinkedFullTitle()));
                ServiceLogger.sacked(this, today);

                setRetirement(today);
            }
            case LEFT -> {
                campaign.addReport(String.format(status.getReportText(), getHyperlinkedFullTitle()));
                ServiceLogger.left(this, today);

                setRetirement(today);
            }
            case STUDENT -> {
                // log entries and reports are handled by the education package
                // (mekhq/campaign/personnel/education)
            }
            case PREGNANCY_COMPLICATIONS -> {
                campaign.getProcreation().processPregnancyComplications(campaign, campaign.getLocalDate(), this);
                campaign.addReport(String.format(status.getReportText(), getHyperlinkedFullTitle()));
                ServiceLogger.changedStatus(this, campaign.getLocalDate(), status);
            }
            default -> {
                campaign.addReport(String.format(status.getReportText(), getHyperlinkedFullTitle()));
                ServiceLogger.changedStatus(this, campaign.getLocalDate(), status);
            }
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

        if (status.isActiveFlexible()) {
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

            if (tagAlong != null) {
                tagAlong.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.ACTIVE);
            }
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
                  spanOpeningWithCustomColor(getWarningColor()),
                  CLOSING_SPAN_TAG));
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

        int roll = d6(3);
        int secondRoll = d6(3);

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

        applyLoyaltyChange.accept(d6(3));

        if (isMajor) {
            applyLoyaltyChange.accept(d6(3));
        }

        if ((isVerbose) && (originalLoyalty != loyalty)) {
            reportLoyaltyChange(campaign, originalLoyalty);
        }
    }

    /**
     * Applies a forced loyalty change to all eligible personnel in the campaign.
     *
     * <p>This method iterates through all personnel in the given {@link Campaign} and, for each person who is
     * neither departed from the unit nor currently a prisoner, calls {@link Person#performForcedDirectionLoyaltyChange}
     * with the specified parameters. After all changes, if the campaign is using loyalty modifiers, a report about the
     * group loyalty change is added to the campaign reports.</p>
     *
     * @param campaign   the {@link Campaign} whose personnel will have their loyalty modified
     * @param isPositive {@code true} for a positive loyalty direction change, {@code false} for negative
     * @param isMajor    {@code true} for a major loyalty change, {@code false} for minor
     */
    public static void performMassForcedDirectionLoyaltyChange(Campaign campaign, boolean isPositive,
          boolean isMajor) {
        for (Person person : campaign.getPersonnel()) {
            if (person.getStatus().isDepartedUnit()) {
                continue;
            }

            if (person.getPrisonerStatus().isCurrentPrisoner()) {
                continue;
            }

            person.performForcedDirectionLoyaltyChange(campaign, isPositive, isMajor, false);
        }

        if (campaign.getCampaignOptions().isUseLoyaltyModifiers()) {
            campaign.addReport(String.format(resources.getString("loyaltyChangeGroup.text"),
                  "<span color=" + getWarningColor() + "'>",
                  CLOSING_SPAN_TAG));
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
            color = getNegativeColor();
            changeString.append(resources.getString("loyaltyChangeNegative.text"));
        } else {
            color = getPositiveColor();
            changeString.append(resources.getString("loyaltyChangePositive.text"));
        }

        String report = String.format(resources.getString("loyaltyChangeReport.text"),
              getHyperlinkedFullTitle(),
              "<span color=" + color + "'>",
              changeString,
              CLOSING_SPAN_TAG);

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

    /**
     * Sets the recruitment (join) date for this entity.
     * <p>
     * If the provided date is not {@code null}, the entity is marked as employed.
     * </p>
     *
     * @param recruitment the date the entity was recruited, or {@code null} to unset
     */
    public void setRecruitment(final @Nullable LocalDate recruitment) {
        if (recruitment == null) {
            status = PersonnelStatus.CAMP_FOLLOWER;
        }

        this.recruitment = recruitment;
    }

    public String getTimeInService(final Campaign campaign) {
        // Get time in service based on year
        if (getRecruitment() == null) {
            // use "" they haven't been recruited
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

    /**
     * Use {@link #getBaseLoyalty()} instead.
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public int getLoyalty() {
        return getBaseLoyalty();
    }

    /**
     * This method returns the character's base loyalty score.
     *
     * <p><b>Usage:</b> In most cases you will want to use {@link #getAdjustedLoyalty(Faction)} instead.</p>
     *
     * @return the loyalty value as an {@link Integer}
     */
    public int getBaseLoyalty() {
        return loyalty;
    }

    /**
     * Calculates and returns the adjusted loyalty value for the given campaign faction.
     *
     * @param campaignFaction the campaign {@link Faction} being compared with the origin {@link Faction}
     *
     * @return the loyalty value adjusted based on the provided campaign {@link Faction}
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getAdjustedLoyalty(Faction campaignFaction) {
        final int LOYALTY_PENALTY_FOR_ANARCHIST = -2;

        boolean campaignFactionMatchesOriginFaction = originFaction.equals(campaignFaction);

        int modifier = 0;
        boolean hasHatredForAuthority = options.booleanOption(COMPULSION_ANARCHIST);
        if (hasHatredForAuthority) {
            modifier += commander ? 0 : LOYALTY_PENALTY_FOR_ANARCHIST;
        }

        boolean hasFactionPride = options.booleanOption(COMPULSION_FACTION_PRIDE);
        if (hasFactionPride) {
            modifier += campaignFactionMatchesOriginFaction ? 1 : -2;
        }

        boolean hasFactionLoyalty = options.booleanOption(COMPULSION_FACTION_LOYALTY);
        if (hasFactionLoyalty) {
            modifier += campaignFactionMatchesOriginFaction ? 1 : -4;
        }

        return loyalty + modifier;
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

    int getStoredLoyalty() {
        return storedLoyalty;
    }

    void setStoredLoyalty(int storedLoyalty) {
        this.storedLoyalty = storedLoyalty;
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
     * <p><b>Usage:</b> this method should only be used when the act of spending XP cannot change the characters'
     * experience level (green, veteran, etc.). In <b>all</b> other instances {@link #spendXPOnSkills(Campaign, int)}
     * must be used.</p>
     *
     * @param xp the amount of XP to deduct
     */
    public void spendXP(final int xp) {
        this.xp -= xp;
    }

    /**
     * Processes spending XP on skill upgrades for the character and checks for veterancy SPA (Special Personnel
     * Ability) gain.
     *
     * <p>Deducts the specified XP amount, and if campaign options and the character's veteran status allow, attempts
     * to assign a veterancy special ability. Triggers relevant events and logs a report if a SPA is gained.</p>
     *
     * @param campaign the campaign context for skill and SPA gain rules
     * @param xp       the amount of XP to spend
     *
     * @author Illiani
     * @since 0.50.10
     */
    public void spendXPOnSkills(final Campaign campaign, final int xp) {
        spendXP(xp); // Process spending of XP as normal

        // Check whether we need to process the veterancy SPA gain
        processVeterancyAwards(campaign);
    }

    public void processVeterancyAwards(Campaign campaign) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        boolean isUseAbilities = campaignOptions.isUseAbilities();
        boolean isUseVeterancySPA = campaignOptions.isAwardVeterancySPAs();
        if (hasGainedVeterancySPA || !isUseAbilities || !isUseVeterancySPA) {
            return;
        }

        // Is the character a veteran in their primary profession?
        int experienceLevel = getExperienceLevel(campaign, false);
        if (experienceLevel < EXP_VETERAN) {
            return;
        }

        SingleSpecialAbilityGenerator singleSpecialAbilityGenerator = new SingleSpecialAbilityGenerator();
        String spaGained = singleSpecialAbilityGenerator.rollSPA(campaign, this, true, true, true);
        if (spaGained == null) {
            return;
        } else {
            hasGainedVeterancySPA = true;
        }

        String spaGainedMessage = getVeterancyAwardReport(spaGained);
        campaign.addReport(spaGainedMessage);
        MekHQ.triggerEvent(new PersonChangedEvent(this));
    }

    /**
     * Generates a formatted report string describing the veterancy SPA award for a person.
     *
     * <p>Removes any specialization or extra information in parentheses from the SPA name, applies color formatting
     * to the SPA, and returns a localized report message string suitable for display in the daily report.</p>
     *
     * @param spaGained the name of the SPA gained (may include specialization in parentheses)
     *
     * @return a formatted, localized report string announcing the award
     *
     * @author Illiani
     * @since 0.50.10
     */
    public String getVeterancyAwardReport(String spaGained) {
        String spaGainedClean = spaGained.replaceAll("\\s*\\([^)]*\\)", "");
        String veteranColor = MekHQ.getMHQOptions().getFontColorSkillVeteranHexColor();
        String amazingColor = getWarningColor(); // We use warning as it can be a positive or negative event
        return String.format(resources.getString("Person.veterancySPA.gain"),
              getHyperlinkedFullTitle(), spanOpeningWithCustomColor(veteranColor), CLOSING_SPAN_TAG,
              spanOpeningWithCustomColor(amazingColor), spaGainedClean, CLOSING_SPAN_TAG);
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

    Aggression getStoredAggression() {
        return storedAggression;
    }

    void setStoredAggression(Aggression storedAggression) {
        this.storedAggression = storedAggression;
    }

    int getStoredAggressionDescriptionIndex() {
        return storedAggressionDescriptionIndex;
    }

    void setStoredAggressionDescriptionIndex(int storedAggressionDescriptionIndex) {
        this.storedAggressionDescriptionIndex = storedAggressionDescriptionIndex;
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

    Ambition getStoredAmbition() {
        return storedAmbition;
    }

    void setStoredAmbition(Ambition storedAmbition) {
        this.storedAmbition = storedAmbition;
    }

    int getStoredAmbitionDescriptionIndex() {
        return storedAmbitionDescriptionIndex;
    }

    void setStoredAmbitionDescriptionIndex(int storedAmbitionDescriptionIndex) {
        this.storedAmbitionDescriptionIndex = storedAmbitionDescriptionIndex;
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

    Greed getStoredGreed() {
        return storedGreed;
    }

    void setStoredGreed(Greed storedGreed) {
        this.storedGreed = storedGreed;
    }

    int getStoredGreedDescriptionIndex() {
        return storedGreedDescriptionIndex;
    }

    void setStoredGreedDescriptionIndex(int storedGreedDescriptionIndex) {
        this.storedGreedDescriptionIndex = storedGreedDescriptionIndex;
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

    Social getStoredSocial() {
        return storedSocial;
    }

    void setStoredSocial(Social storedSocial) {
        this.storedSocial = storedSocial;
    }

    int getStoredSocialDescriptionIndex() {
        return storedSocialDescriptionIndex;
    }

    void setStoredSocialDescriptionIndex(int storedSocialDescriptionIndex) {
        this.storedSocialDescriptionIndex = storedSocialDescriptionIndex;
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

    PersonalityQuirk getStoredPersonalityQuirk() {
        return storedPersonalityQuirk;
    }

    void setStoredPersonalityQuirk(PersonalityQuirk storedPersonalityQuirk) {
        this.storedPersonalityQuirk = storedPersonalityQuirk;
    }

    int getStoredPersonalityQuirkDescriptionIndex() {
        return storedPersonalityQuirkDescriptionIndex;
    }

    void setStoredPersonalityQuirkDescriptionIndex(int storedPersonalityQuirkDescriptionIndex) {
        this.storedPersonalityQuirkDescriptionIndex = storedPersonalityQuirkDescriptionIndex;
    }

    public Reasoning getReasoning() {
        return reasoning;
    }

    public void setReasoning(final Reasoning reasoning) {
        this.reasoning = reasoning;
    }

    @Deprecated(since = "0.50.07", forRemoval = true)
    public int getReasoningDescriptionIndex() {
        return 0;
    }

    @Deprecated(since = "0.50.07", forRemoval = true)
    public void setReasoningDescriptionIndex(final int reasoningDescriptionIndex) {
    }

    Reasoning getStoredReasoning() {
        return storedReasoning;
    }

    void setStoredReasoning(Reasoning storedReasoning) {
        this.storedReasoning = storedReasoning;
    }

    @Deprecated(since = "0.50.07", forRemoval = true)
    int getStoredReasoningDescriptionIndex() {
        return 0;
    }

    @Deprecated(since = "0.50.07", forRemoval = true)
    void setStoredReasoningDescriptionIndex(int storedReasoningDescriptionIndex) {
    }

    public String getPersonalityDescription() {
        return personalityDescription;
    }

    public void setPersonalityDescription(final String personalityDescription) {
        this.personalityDescription = personalityDescription;
    }

    public String getPersonalityInterviewNotes() {
        return personalityInterviewNotes;
    }

    public void setPersonalityInterviewNotes(final String personalityInterviewNotes) {
        this.personalityInterviewNotes = personalityInterviewNotes;
    }

    public boolean isSufferingFromClinicalParanoia() {
        return sufferingFromClinicalParanoia;
    }

    public void setSufferingFromClinicalParanoia(final boolean sufferingFromClinicalParanoia) {
        this.sufferingFromClinicalParanoia = sufferingFromClinicalParanoia;
    }

    public boolean isDarkSecretRevealed() {
        return darkSecretRevealed;
    }

    public void setDarkSecretRevealed(final boolean darkSecretRevealed) {
        this.darkSecretRevealed = darkSecretRevealed;
    }

    public @Nullable LocalDate getBurnedConnectionsEndDate() {
        return burnedConnectionsEndDate;
    }

    public void setBurnedConnectionsEndDate(final @Nullable LocalDate burnedConnectionsEndDate) {
        this.burnedConnectionsEndDate = burnedConnectionsEndDate;
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

    public boolean isQuickTrainIgnore() {
        return quickTrainIgnore;
    }

    public void setQuickTrainIgnore(final boolean quickTrainIgnore) {
        this.quickTrainIgnore = quickTrainIgnore;
    }

    public boolean isEmployed() {
        return status != PersonnelStatus.CAMP_FOLLOWER;
    }

    /**
     * Determines whether this person is open to marriage or romantic relationships.
     *
     * <p>A person is considered marriageable if they have romantic interest in at least one gender (men, women, or
     * both). Aromantic/asexual individuals who prefer neither gender are not marriageable.</p>
     *
     * @return {@code true} if the person has romantic interest in men, women, or both; {@code false} if
     *       aromantic/asexual
     */
    public boolean isMarriageable() {
        return isPrefersMen() || isPrefersWomen();
    }

    @Deprecated(since = "0.50.10", forRemoval = true)
    public void setMarriageable(final boolean marriageable) {
        this.marriageable = marriageable;
    }

    public boolean isPrefersMen() {
        return prefersMen;
    }

    public void setPrefersMen(final boolean prefersMen) {
        this.prefersMen = prefersMen;
    }

    public boolean isPrefersWomen() {
        return prefersWomen;
    }

    public void setPrefersWomen(final boolean prefersWomen) {
        this.prefersWomen = prefersWomen;
    }

    public boolean isTryingToConceive() {
        return tryingToConceive;
    }

    public void setTryingToConceive(final boolean tryingToConceive) {
        this.tryingToConceive = tryingToConceive;
    }

    public boolean isHidePersonality() {
        return hidePersonality;
    }

    public void setHidePersonality(final boolean hidePersonality) {
        this.hidePersonality = hidePersonality;
    }
    // endregion Flags

    public ExtraData getExtraData() {
        return extraData;
    }

    // region File I/O
    public void writeToXML(final PrintWriter pw, int indent, final Campaign campaign) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "person", "id", id, "type", getClass());
        indent = writeToXMLHeadless(pw, indent, campaign);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "person");
    }

    public int writeToXMLHeadless(PrintWriter pw, int indent, Campaign campaign) {
        try {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "id", id.toString());

            // region Name
            if (!isNullOrBlank(getPreNominal())) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "preNominal", getPreNominal());
            }
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "givenName", getGivenName());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "surname", getSurname());
            if (!isNullOrBlank(getPostNominal())) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "postNominal", getPostNominal());
            }

            if (getMaidenName() != null) { // this is only a != null comparison because empty is a use case for divorce
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "maidenName", getMaidenName());
            }

            if (!isNullOrBlank(getCallsign())) {
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
                MHQXMLUtility.writeSimpleXMLAttributedTag(pw, indent,
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

            if (!isNullOrBlank(bloodname)) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "bloodname", bloodname);
            }

            if (!isNullOrBlank(biography)) {
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

            if (hasGainedVeterancySPA) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "hasGainedVeterancySPA", hasGainedVeterancySPA);
            }

            if (connections != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "connections", connections);
            }

            if (wealth != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "wealth", wealth);
            }

            if (!ExtraIncome.ZERO.equals(extraIncome)) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "extraIncome", extraIncome.name());
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

            if (bloodmark != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "bloodmark", bloodmark);
            }

            if (!bloodhuntSchedule.isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "bloodhuntSchedule");
                for (LocalDate attemptDate : bloodhuntSchedule) {
                    MHQXMLUtility.writeSimpleXMLTag(pw, indent, "attemptDate", attemptDate);
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "bloodhuntSchedule");
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
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "loyalty", getBaseLoyalty());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "fatigue", getFatigue());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "isRecoveringFromFatigue", getIsRecoveringFromFatigue());
            for (Skill skill : skills.getSkills()) {
                skill.writeToXML(pw, indent);
            }

            if (countOptions(PersonnelOptions.LVL3_ADVANTAGES) > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent,
                      "advantages",
                      getOptionList(DELIMITER, PersonnelOptions.LVL3_ADVANTAGES));
            }

            if (countOptions(PersonnelOptions.EDGE_ADVANTAGES) > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent,
                      "edge",
                      getOptionList(DELIMITER, PersonnelOptions.EDGE_ADVANTAGES));
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "edgeAvailable", getCurrentEdge());
            }

            if (countOptions(PersonnelOptions.MD_ADVANTAGES) > 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent,
                      "implants",
                      getOptionList(DELIMITER, PersonnelOptions.MD_ADVANTAGES));
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

            if (!patientLog.isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "patientLog");
                for (LogEntry entry : patientLog) {
                    entry.writeToXML(pw, indent);
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "patientLog");
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
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personalityQuirk", personalityQuirk.name());
            }

            MHQXMLUtility.writeSimpleXMLTag(pw, indent,
                  "personalityQuirkDescriptionIndex",
                  personalityQuirkDescriptionIndex);

            if (reasoning != Reasoning.AVERAGE) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "reasoning", reasoning.ordinal());
            }

            if (!isNullOrBlank(personalityDescription)) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personalityDescription", personalityDescription);
            }

            if (!isNullOrBlank(personalityInterviewNotes)) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personalityInterviewNotes", personalityInterviewNotes);
            }

            if (!isNullOrBlank(storedGivenName)) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "storedGivenName", storedGivenName);
            }

            if (!isNullOrBlank(storedSurname)) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "storedSurname", storedSurname);
            }

            if (storedLoyalty != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "storedLoyalty", storedLoyalty);
            }

            if (storedOriginFaction != null) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "storedOriginFaction", storedOriginFaction.getShortName());
            }

            if (storedAggression != Aggression.NONE) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "storedAggression", storedAggression.name());
            }

            if (storedAggressionDescriptionIndex != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw,
                      indent,
                      "storedAggressionDescriptionIndex",
                      storedAggressionDescriptionIndex);
            }

            if (storedAmbition != Ambition.NONE) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "storedAmbition", storedAmbition.name());
            }

            if (storedAmbitionDescriptionIndex != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw,
                      indent,
                      "storedAmbitionDescriptionIndex",
                      storedAmbitionDescriptionIndex);
            }

            if (storedGreed != Greed.NONE) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "storedGreed", storedGreed.name());
            }

            if (storedGreedDescriptionIndex != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "storedGreedDescriptionIndex", storedGreedDescriptionIndex);
            }

            if (storedSocial != Social.NONE) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "storedSocial", storedSocial.name());
            }

            if (storedSocialDescriptionIndex != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw,
                      indent,
                      "storedSocialDescriptionIndex",
                      storedSocialDescriptionIndex);
            }

            if (storedPersonalityQuirk != PersonalityQuirk.NONE) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "storedPersonalityQuirk", storedPersonalityQuirk.name());
            }

            if (storedPersonalityQuirkDescriptionIndex != 0) {
                MHQXMLUtility.writeSimpleXMLTag(pw,
                      indent,
                      "storedPersonalityQuirkDescriptionIndex",
                      storedPersonalityQuirkDescriptionIndex);
            }

            if (storedReasoning != Reasoning.AVERAGE) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "storedReasoning", storedReasoning.name());
            }

            if (sufferingFromClinicalParanoia) {
                MHQXMLUtility.writeSimpleXMLTag(pw,
                      indent,
                      "sufferingFromClinicalParanoia",
                      sufferingFromClinicalParanoia);
            }

            if (darkSecretRevealed) {
                MHQXMLUtility.writeSimpleXMLTag(pw,
                      indent,
                      "darkSecretRevealed",
                      darkSecretRevealed);
            }

            if (burnedConnectionsEndDate != null) {
                MHQXMLUtility.writeSimpleXMLTag(pw,
                      indent,
                      "burnedConnectionsEndDate",
                      burnedConnectionsEndDate);
            }

            // region Flags
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "clanPersonnel", isClanPersonnel());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "commander", commander);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "divorceable", divorceable);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "founder", founder);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "immortal", immortal);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "quickTrainIgnore", quickTrainIgnore);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "marriageable", marriageable);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "prefersMen", prefersMen);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "prefersWomen", prefersWomen);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "tryingToConceive", tryingToConceive);
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "hidePersonality", hidePersonality);
            // endregion Flags

            if (!extraData.isEmpty()) {
                extraData.writeToXml(pw);
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "Failed to write {} to the XML File", getFullName());
            throw ex; // we want to rethrow to ensure that the save fails
        }
        return indent;
    }

    public static Person generateInstanceFromXML(Node wn, Campaign campaign, Version version) {
        Person person = new Person(campaign);
        LocalDate today = campaign.getLocalDate();

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
                        LOGGER.error("Error loading originPlanet for {}, {}", systemId, planetId, e);
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
                    person.acquisitions = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("primaryDesignator")) {
                    person.primaryDesignator = ROMDesignation.parseFromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("secondaryDesignator")) {
                    person.secondaryDesignator = ROMDesignation.parseFromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("daysToWaitForHealing")) {
                    person.daysToWaitForHealing = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("vocationalXPTimer")) {
                    person.vocationalXPTimer = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("id")) {
                    person.id = UUID.fromString(wn2.getTextContent().trim());
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
                    person.nTasks = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("hits")) {
                    person.hits = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("hitsPrior")) {
                    person.hitsPrior = MathUtility.parseInt(wn2.getTextContent().trim());
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
                        person.doctorId = UUID.fromString(wn2.getTextContent().trim());
                    }
                } else if (nodeName.equalsIgnoreCase("unitId")) {
                    if (!wn2.getTextContent().equals("null")) {
                        person.unit = new PersonUnitRef(UUID.fromString(wn2.getTextContent().trim()));
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
                    person.minutesLeft = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("overtimeLeft")) {
                    person.overtimeLeft = MathUtility.parseInt(wn2.getTextContent().trim());
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
                    person.fatigue = MathUtility.parseInt(wn2.getTextContent().trim());
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
                    person.toughness = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("hasGainedVeterancySPA")) {
                    person.hasGainedVeterancySPA = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("connections")) {
                    person.connections = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("wealth")) {
                    person.wealth = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("extraIncome")) {
                    person.extraIncome = ExtraIncome.extraIncomeParseFromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("hasPerformedExtremeExpenditure")) {
                    person.hasPerformedExtremeExpenditure = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("reputation")) {
                    person.reputation = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("unlucky")) {
                    person.unlucky = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("bloodmark")) {
                    person.bloodmark = MathUtility.parseInt(wn2.getTextContent());
                } else if (nodeName.equalsIgnoreCase("bloodhuntSchedule")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("attemptDate")) {
                            LOGGER.error("(techUnitIds) Unknown node type not loaded in bloodhuntSchedule nodes: {}",
                                  wn3.getNodeName());
                            continue;
                        }
                        person.addBloodhuntDate(LocalDate.parse(wn3.getTextContent().trim()));
                    }
                } else if (nodeName.equalsIgnoreCase("atowAttributes")) {
                    person.atowAttributes = new Attributes().generateAttributesFromXML(wn2);
                } else if (nodeName.equalsIgnoreCase("pilotHits")) {
                    person.hits = MathUtility.parseInt(wn2.getTextContent().trim());
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
                            LOGGER.error("(techUnitIds) Unknown node type not loaded in techUnitIds nodes: {}",
                                  wn3.getNodeName());
                            continue;
                        }
                        person.addTechUnit(new PersonUnitRef(UUID.fromString(wn3.getTextContent().trim())));
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
                            LOGGER.error("(personnelLog) Unknown node type not loaded in personnel logEntry nodes: {}",
                                  wn3.getNodeName());
                            continue;
                        }

                        final LogEntry logEntry = LogEntryFactory.getInstance().generateInstanceFromXML(wn3);
                        if (logEntry != null) {
                            LogEntryType logEntryType = logEntry.getType();
                            String logEntryDescription = logEntry.getDesc();
                            if (logEntryType == MEDICAL) {
                                person.addMedicalLogEntry(logEntry);
                            } else {
                                Map<String, LogEntryType> logMap = new HashMap<>();
                                logMap.put("Assigned to", ASSIGNMENT); // <50.07 compatibility
                                logMap.put("Reassigned to", ASSIGNMENT); // <50.07 compatibility
                                logMap.put("Removed from", ASSIGNMENT); // <50.07 compatibility
                                logMap.put("Added to", ASSIGNMENT); // <50.07 compatibility
                                logMap.put("Changed edge to", PERFORMANCE); // <50.07 compatibility
                                logMap.put("Gained", PERFORMANCE); // <50.07 compatibility
                                logMap.put("Improved", PERFORMANCE); // <50.07 compatibility
                                logMap.put("injuries, gaining", PERFORMANCE); // <50.07 compatibility
                                logMap.put("XP from successful medical work", PERFORMANCE); // <50.07 compatibility
                                logMap.put("Successfully treated", PATIENT); // <50.07 compatibility

                                boolean logEntryWasReassigned = false;
                                for (Map.Entry<String, LogEntryType> entry : logMap.entrySet()) {
                                    if (logEntryDescription.contains(entry.getKey())) {
                                        LogEntryType newType = entry.getValue();
                                        logEntry.setType(newType);

                                        switch (newType) {
                                            case ASSIGNMENT -> person.addAssignmentLogEntry(logEntry);
                                            case PERFORMANCE -> person.addPerformanceLogEntry(logEntry);
                                            case PATIENT -> person.addPatientLogEntry(logEntry);
                                        }

                                        logEntryWasReassigned = true;
                                        break;
                                    }
                                }

                                if (!logEntryWasReassigned) {
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
                            LOGGER.error("(medicalLog) Unknown node type not loaded in personnel logEntry nodes: {}",
                                  wn3.getNodeName());
                            continue;
                        }

                        final LogEntry logEntry = LogEntryFactory.getInstance().generateInstanceFromXML(wn3);
                        if (logEntry != null) {
                            person.addMedicalLogEntry(logEntry);
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("patientLog")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("logEntry")) {
                            LOGGER.error("(patientLog) Unknown node type not loaded in personnel logEntry nodes: {}",
                                  wn3.getNodeName());
                            continue;
                        }

                        final LogEntry logEntry = LogEntryFactory.getInstance().generateInstanceFromXML(wn3);
                        if (logEntry != null) {
                            person.addPatientLogEntry(logEntry);
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
                            LOGGER.error("Unknown node type not loaded in scenario logEntry nodes: {}",
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
                            LOGGER.error("(assignmentLog) Unknown node type not loaded in scenario logEntry nodes: {}",
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
                            LOGGER.error("(performanceLog) Unknown node type not loaded in scenario logEntry nodes: {}",
                                  wn3.getNodeName());
                            continue;
                        }

                        final LogEntry logEntry = LogEntryFactory.getInstance().generateInstanceFromXML(wn3);
                        if (logEntry != null) {
                            String logEntryDescription = logEntry.getDesc();

                            if (logEntryDescription.contains("Successfully treated")) {
                                logEntry.setType(PATIENT);
                                person.addPatientLogEntry(logEntry);
                            } else {
                                person.addPerformanceLogEntry(logEntry);
                            }
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
                            LOGGER.error("Unknown node type not loaded in personnel award log nodes: {}",
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
                            LOGGER.error("Unknown node type not loaded in injury nodes: {}", wn3.getNodeName());
                            continue;
                        }
                        person.injuries.add(Injury.generateInstanceFromXML(wn3));
                    }
                    person.injuries.stream()
                          .filter(inj -> (null == inj.getStart()))
                          .forEach(inj -> inj.setStart(today.minusDays(inj.getOriginalTime() - inj.getTime())));
                } else if (nodeName.equalsIgnoreCase("originalUnitWeight")) {
                    person.originalUnitWeight = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("originalUnitTech")) {
                    person.originalUnitTech = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("originalUnitId")) {
                    person.originalUnitId = UUID.fromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("eduHighestEducation")) {
                    person.eduHighestEducation = EducationLevel.fromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("eduJourneyTime")) {
                    person.eduJourneyTime = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("eduDaysOfTravel")) {
                    person.eduDaysOfTravel = MathUtility.parseInt(wn2.getTextContent().trim());
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
                                person.eduFailedApplications.add(node.getTextContent().trim());
                            }
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("eduAcademySystem")) {
                    person.eduAcademySystem = String.valueOf(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("eduAcademyName")) {
                    person.eduAcademyName = String.valueOf(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("eduAcademySet")) {
                    person.eduAcademySet = String.valueOf(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("eduAcademyNameInSet")) {
                    String academyNameInSet = wn2.getTextContent().trim();
                    // Compatibility handler
                    if (academyNameInSet != null) {
                        person.eduAcademyNameInSet = switch (academyNameInSet) {
                            case "Boot Camp" -> "Bootcamp"; // <50.10 compatibility handler
                            default -> academyNameInSet;
                        };
                    } else {
                        person.eduAcademyNameInSet = null;
                    }
                } else if (nodeName.equalsIgnoreCase("eduAcademyFaction")) {
                    person.eduAcademyFaction = String.valueOf(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("eduCourseIndex")) {
                    person.eduCourseIndex = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("eduEducationStage")) {
                    person.eduEducationStage = EducationStage.parseFromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("eduEducationTime")) {
                    person.eduEducationTime = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("aggression")) {
                    person.aggression = Aggression.fromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("aggressionDescriptionIndex")) {
                    person.aggressionDescriptionIndex = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("ambition")) {
                    person.ambition = Ambition.fromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("ambitionDescriptionIndex")) {
                    person.ambitionDescriptionIndex = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("greed")) {
                    person.greed = Greed.fromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("greedDescriptionIndex")) {
                    person.greedDescriptionIndex = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("social")) {
                    person.social = Social.fromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("socialDescriptionIndex")) {
                    person.socialDescriptionIndex = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("personalityQuirk")) {
                    person.personalityQuirk = PersonalityQuirk.fromString(wn2.getTextContent().trim());

                    // < 50.07 compatibility handler
                    if (person.personalityQuirk == PersonalityQuirk.BROKEN) {
                        person.personalityQuirk = PersonalityQuirk.HAUNTED;
                    }
                } else if (nodeName.equalsIgnoreCase("personalityQuirkDescriptionIndex")) {
                    person.personalityQuirkDescriptionIndex = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if ((nodeName.equalsIgnoreCase("reasoning"))) {
                    person.reasoning = Reasoning.fromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("personalityDescription")) {
                    person.personalityDescription = wn2.getTextContent();
                } else if (nodeName.equalsIgnoreCase("personalityInterviewNotes")) {
                    person.personalityInterviewNotes = wn2.getTextContent();
                } else if (nodeName.equalsIgnoreCase("storedGivenName")) {
                    person.storedGivenName = wn2.getTextContent();
                } else if (nodeName.equalsIgnoreCase("storedSurname")) {
                    person.storedSurname = wn2.getTextContent();
                } else if (nodeName.equalsIgnoreCase("storedLoyalty")) {
                    person.storedLoyalty = MathUtility.parseInt(wn2.getTextContent().trim(), 9);
                } else if (nodeName.equalsIgnoreCase("storedOriginFaction")) {
                    person.storedOriginFaction = Factions.getInstance().getFaction(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("storedAggression")) {
                    person.storedAggression = Aggression.fromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("storedAggressionDescriptionIndex")) {
                    person.storedAggressionDescriptionIndex = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("storedAmbition")) {
                    person.storedAmbition = Ambition.fromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("storedAmbitionDescriptionIndex")) {
                    person.storedAmbitionDescriptionIndex = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("storedGreed")) {
                    person.storedGreed = Greed.fromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("storedGreedDescriptionIndex")) {
                    person.storedGreedDescriptionIndex = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("storedSocial")) {
                    person.storedSocial = Social.fromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("storedSocialDescriptionIndex")) {
                    person.storedSocialDescriptionIndex = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("storedPersonalityQuirk")) {
                    person.storedPersonalityQuirk = PersonalityQuirk.fromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("storedPersonalityQuirkDescriptionIndex")) {
                    person.storedPersonalityQuirkDescriptionIndex = MathUtility.parseInt(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("storedReasoning")) {
                    person.storedReasoning = Reasoning.fromString(wn2.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("sufferingFromClinicalParanoia")) {
                    person.setSufferingFromClinicalParanoia(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("darkSecretRevealed")) {
                    person.setDarkSecretRevealed(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("burnedConnectionsEndDate")) {
                    person.setBurnedConnectionsEndDate(LocalDate.parse(wn2.getTextContent().trim()));
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
                } else if (nodeName.equalsIgnoreCase("quickTrainIgnore")) {
                    person.setQuickTrainIgnore(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("marriageable")) { // Legacy: <50.10
                    boolean marriageable = Boolean.parseBoolean(wn2.getTextContent().trim());
                    CampaignOptions campaignOptions = campaign.getCampaignOptions();
                    sexualityCompatibilityHandler(marriageable,
                          person,
                          campaignOptions.getNoInterestInRelationshipsDiceSize(),
                          campaignOptions.getInterestedInSameSexDiceSize(),
                          campaignOptions.getInterestedInBothSexesDiceSize());
                } else if (nodeName.equalsIgnoreCase("prefersMen")) {
                    person.setPrefersMen(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("prefersWomen")) {
                    person.setPrefersWomen(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("tryingToConceive")) {
                    person.setTryingToConceive(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("hidePersonality")) {
                    person.setHidePersonality(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (nodeName.equalsIgnoreCase("extraData")) {
                    person.extraData = ExtraData.createFromXml(wn2);
                }
            }

            person.setFullName(); // this sets the name based on the loaded values

            if ((advantages != null) && !advantages.isBlank()) {
                StringTokenizer st = new StringTokenizer(advantages, DELIMITER);
                while (st.hasMoreTokens()) {
                    String adv = st.nextToken();
                    String advName = Crew.parseAdvantageName(adv);
                    Object value = Crew.parseAdvantageValue(adv);

                    try {
                        person.getOptions().getOption(advName).setValue(value);
                    } catch (Exception e) {
                        LOGGER.warn("Error restoring advantage: {}", adv);
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
                StringTokenizer st = new StringTokenizer(implants, DELIMITER);
                while (st.hasMoreTokens()) {
                    String adv = st.nextToken();
                    String advName = Crew.parseAdvantageName(adv);
                    Object value = Crew.parseAdvantageValue(adv);

                    try {
                        person.getOptions().getOption(advName).setValue(value);
                    } catch (Exception e) {
                        LOGGER.error("Error restoring implants: {}", adv);
                    }
                }
            }

            // Fixing Prisoner Ranks - 0.47.X Fix
            if (person.getRankNumeric() < 0) {
                person.setRank(0);
            }

            if (person.getJoinedCampaign() == null) {
                person.setJoinedCampaign(today);
            }

            // <50.10 compatibility handler
            if (updateSkillsForVehicleProfessions(today, person, person.getPrimaryRole(), true) ||
                      updateSkillsForVehicleProfessions(today, person, person.getSecondaryRole(), false)) {
                String report = getFormattedTextAt(RESOURCE_BUNDLE, "vehicleProfessionSkillChange",
                      spanOpeningWithCustomColor(getWarningColor()),
                      CLOSING_SPAN_TAG,
                      person.getHyperlinkedFullTitle());
                campaign.addReport(report);
            }

            // This resolves a bug squashed in 2025 (50.03) but lurked in our codebase
            // potentially as far back as 2014. The next two handlers should never be removed.
            if (!person.canPerformRole(today, person.getSecondaryRole(), false)) {
                person.setSecondaryRole(PersonnelRole.NONE);

                campaign.addReport(String.format(resources.getString("ineligibleForSecondaryRole"),
                      spanOpeningWithCustomColor(getWarningColor()),
                      CLOSING_SPAN_TAG,
                      person.getHyperlinkedFullTitle()));
            }

            if (!person.canPerformRole(today, person.getPrimaryRole(), true)) {
                person.setPrimaryRole(campaign, PersonnelRole.NONE);

                campaign.addReport(String.format(resources.getString("ineligibleForPrimaryRole"),
                      spanOpeningWithCustomColor(getNegativeColor()),
                      CLOSING_SPAN_TAG,
                      person.getHyperlinkedFullTitle()));
            }
        } catch (Exception e) {
            LOGGER.error(e, "Failed to read person {} from file", person.getFullName());
            person = null;
        }

        return person;
    }

    /**
     * Updates skills and role for personnel with deprecated vehicle professions.
     *
     * <p>This method is used during XML loading to migrate legacy vehicle roles to the new vehicle crew system.</p>
     *
     * <p>It...</p>
     * <ul>
     *   <li>Determines the appropriate new vehicle crew role based on the old role</li>
     *   <li>Adds missing driving and gunnery skills at appropriate levels</li>
     *   <li>Updates the person's role (primary or secondary) to the new profession</li>
     * </ul>
     *
     * <p>For {@link PersonnelRole#VEHICLE_GUNNER}, the new role is determined by examining the person's currently
     * assigned entity. If no entity is assigned, defaults to ground vehicle crew.</p>
     *
     * <p>Skills are added at level 3 if the complementary skill is missing, otherwise they are added at the same
     * level as the existing complementary skill.</p>
     *
     * @param today     the current date
     * @param person    the person whose skills and role should be updated
     * @param role      the deprecated role to migrate from
     * @param isPrimary whether this is the person's primary role (true) or secondary role (false)
     *
     * @return {@code true} if skills or profession was updated, {@code false} if no update was needed
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static boolean updateSkillsForVehicleProfessions(LocalDate today, Person person, PersonnelRole role,
          boolean isPrimary) {
        if (role == PersonnelRole.VEHICLE_CREW) { // The old vehicle crew profession is handled differently
            return updateSkillsForVehicleCrewProfession(today, person, role, isPrimary);
        }

        PersonnelRole newProfession = null;
        String drivingSkillType = null;
        String gunnerySkillType = S_GUN_VEE;

        switch (role) {
            case VTOL_PILOT -> {
                newProfession = PersonnelRole.VEHICLE_CREW_VTOL;
                drivingSkillType = S_PILOT_VTOL;
            }
            case NAVAL_VEHICLE_DRIVER -> {
                newProfession = PersonnelRole.VEHICLE_CREW_NAVAL;
                drivingSkillType = S_PILOT_NVEE;
            }
            case GROUND_VEHICLE_DRIVER -> {
                newProfession = PersonnelRole.VEHICLE_CREW_GROUND;
                drivingSkillType = S_PILOT_GVEE;
            }
            case VEHICLE_GUNNER -> {
                // Vehicle gunners need special handling to guesstimate what they should be. We base this on the unit
                // they are currently assigned to.
                Entity assignedEntity = person.getEntity();
                if (assignedEntity != null) {
                    if (assignedEntity instanceof VTOL) {
                        newProfession = PersonnelRole.VEHICLE_CREW_VTOL;
                        drivingSkillType = S_PILOT_VTOL;
                    } else if (assignedEntity.getMovementMode().isMarine()) {
                        newProfession = PersonnelRole.VEHICLE_CREW_NAVAL;
                        drivingSkillType = S_PILOT_NVEE;
                    } else {
                        newProfession = PersonnelRole.VEHICLE_CREW_GROUND;
                        drivingSkillType = S_PILOT_GVEE;
                    }
                }

                // Fallback
                if (newProfession == null) {
                    newProfession = PersonnelRole.VEHICLE_CREW_GROUND;
                    drivingSkillType = S_PILOT_GVEE;
                }
            }
            default -> { // Not a vehicle profession
                return false;
            }
        }

        Skill drivingSkill = person.getSkill(drivingSkillType);
        Skill gunnerySkill = person.getSkill(gunnerySkillType);

        int drivingTargetLevel = gunnerySkill == null ? 3 : gunnerySkill.getLevel();
        int gunneryTargetLevel = drivingSkill == null ? 3 : drivingSkill.getLevel();

        if (gunnerySkill == null) {
            person.addSkill(gunnerySkillType, gunneryTargetLevel, 0);
        }

        if (drivingSkill == null) {
            person.addSkill(drivingSkillType, drivingTargetLevel, 0);
        }

        if (isPrimary) {
            person.setPrimaryRole(today, newProfession);
        } else {
            person.setSecondaryRole(newProfession);
        }

        return true;
    }

    /**
     * Updates skills for personnel with the Vehicle Crew profession by ensuring they have the Mechanic skill.
     *
     * <p>This method is used during XML loading to migrate legacy data. If the person lacks the
     * {@link SkillType#S_TECH_MECHANIC} skill, it will be added at a level equal to their highest existing vehicle
     * crew-related skill (e.g., Tech Vee, Gunnery Vee, Piloting Vee, or Driving). This ensures backwards compatibility
     * when loading older save files.</p>
     *
     * @param today       the current date
     * @param person      the person whose skills should be updated
     * @param currentRole the role to check
     * @param isPrimary   if the role is the characters' primary profession
     *
     * @return {@code true} if the Mechanic skill was added, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static boolean updateSkillsForVehicleCrewProfession(LocalDate today, Person person,
          PersonnelRole currentRole,
          boolean isPrimary) {
        if (currentRole != PersonnelRole.VEHICLE_CREW) {
            return false;
        }

        if (!person.hasSkill(S_TECH_MECHANIC)) {
            person.addSkill(S_TECH_MECHANIC, 3, 0);
            return true;
        }

        if (isPrimary) {
            person.setPrimaryRole(today, PersonnelRole.COMBAT_TECHNICIAN);
        } else {
            person.setSecondaryRole(PersonnelRole.COMBAT_TECHNICIAN);
        }

        return false;
    }

    /**
     * Configures a person's sexual orientation preferences based on their marriageability status and weighted random
     * probabilities.
     *
     * <p>For non-marriageable characters, all romantic preferences are disabled. For marriageable characters,
     * orientation is determined through sequential probability checks using the provided dice sizes, which represent
     * the denominators for calculating the chance of each orientation (e.g., a die size of 100 means a 1% chance).</p>
     *
     * <p>The orientation determination follows this priority order:</p>
     * <ol>
     *     <li>Aromantic/asexual (no interest in relationships)</li>
     *     <li>Homosexual (interested in the same sex)</li>
     *     <li>Bisexual/pansexual (interested in both sexes)</li>
     *     <li>Heterosexual (default if no other orientation is rolled)</li>
     * </ol>
     *
     * <p>Default percentile chances of each sexuality are viewable in
     * {@link DefaultPersonnelGenerator#determineOrientation(Person, int, int, int)}</p>
     *
     * @param marriageable                      {@code true} if the person is eligible for romantic relationships;
     *                                          {@code false} otherwise
     * @param person                            the {@link Person} whose orientation preferences are being configured
     * @param noInterestInRelationshipsDiceSize dice size for aromantic/asexual orientation (checked first)
     * @param interestedInSameSexDiceSize       dice size for homosexual orientation (checked second)
     * @param interestedInBothSexesDiceSize     dice size for bisexual/pansexual orientation (checked third)
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void sexualityCompatibilityHandler(boolean marriageable, Person person,
          int noInterestInRelationshipsDiceSize,
          int interestedInSameSexDiceSize, int interestedInBothSexesDiceSize) {
        if (!marriageable) {
            person.setPrefersMen(false);
            person.setPrefersWomen(false);
        } else {
            DefaultPersonnelGenerator.determineOrientation(person, noInterestInRelationshipsDiceSize,
                  interestedInSameSexDiceSize, interestedInBothSexesDiceSize);
        }
    }
    // endregion File I/O

    public void setSalary(final Money salary) {
        this.salary = salary;
    }

    /**
     * Calculates and returns the salary for this person based on campaign rules and status.
     *
     * <p>The method applies the following logic:</p>
     * <ul>
     *     <li>If the person is not free (e.g., a prisoner), returns a zero salary.</li>
     *     <li>If a positive or zero custom salary has been set, it is used directly.</li>
     *     <li>If the salary is negative, the standard salary is calculated based on campaign options and the
     *     person's roles, skills, and attributes:</li>
     *     <li>Base salaries are taken from the campaign options, according to primary and secondary roles.</li>
     *     <li>If the person is specialized infantry with applicable unit and specialization, a multiplier is
     *     applied to the primary base salary.</li>
     *     <li>An experience-level multiplier is applied to both primary and secondary salaries based on the
     *     person's skills.</li>
     *     <li>Additional multipliers for specializations (e.g., anti-mek skill) may also apply.</li>
     *     <li>Secondary role salaries are halved and only applied if not disabled via campaign options.</li>
     *     <li>The base salaries for primary and secondary roles are summed.</li>
     *     <li>If the person's rank provides a pay multiplier, the calculated total is multiplied accordingly.</li>
     * </ul>
     *
     * <p>The method does not currently account for era modifiers or crew type (e.g., DropShip, JumpShip, WarShip).</p>
     *
     * @param campaign The current {@link Campaign} used to determine relevant options and settings.
     *
     * @return A {@link Money} object representing the person's salary according to current campaign rules and their
     *       status.
     */
    public Money getSalary(final Campaign campaign) {
        if (!getPrisonerStatus().isFree()) {
            return Money.zero();
        }

        if (!isEmployed()) {
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
            if (hasSkill(S_ANTI_MEK)) {
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
                if (hasSkill(S_ANTI_MEK)) {
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
                if (hasSkill(S_ANTI_MEK)) {
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
        StringTokenizer st = new StringTokenizer(edgeTriggers, DELIMITER);

        while (st.hasMoreTokens()) {
            String trigger = st.nextToken();
            String triggerName = Crew.parseAdvantageName(trigger);
            Object value = Crew.parseAdvantageValue(trigger);

            try {
                retVal.getOptions().getOption(triggerName).setValue(value);
                edgeOptionList.remove(triggerName);
            } catch (Exception e) {
                LOGGER.error("Error restoring edge trigger: {}", trigger);
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
                LOGGER.error("Error disabling edge trigger: {}", edgeTrigger);
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
     * @param sharesForAll whether all personnel have shares
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

    /**
     * Retrieves the index of the character's rank
     *
     * @return the numeric value of the rank as an {@link Integer}
     */
    public int getRankNumeric() {
        return rank;
    }

    public void setRank(final int rank) {
        this.rank = rank;
    }

    /**
     * Retrieves the character's rank <b>sublevel</b>. Predominantly used in ComStar rank styles.
     *
     * <p><b>Important:</b> You almost always want to use {@link #getRankNumeric()} instead.</p>
     *
     * @return the rank level as an integer
     */
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

        if (getPrisonerStatus().isFree() && !getPrimaryRole().isDependent()) {
            setLastRankChangeDate(campaign.getLocalDate());
        } else {
            setLastRankChangeDate(null);
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

    /**
     * Determines the experience level of a person in their current profession within the context of a campaign.
     *
     * <p>The calculation varies depending on the person's role and campaign options:</p>
     * <ul>
     *     <li>
     *         <b>Vehicle Gunners:</b> If artillery usage is enabled in the campaign, calculates the maximum
     *         experience level between Gunnery (Vee) and Artillery skills. Otherwise, uses the profession's
     *         associated skills and campaign averaging option.
     *     </li>
     *     <li>
     *         <b>Vehicle Crew:</b> Returns the highest experience level among a specific set of technical and support skills.
     *     </li>
     *     <li>
     *         <b>Administrators:</b> Averages the Administrator skill and (optionally) Negotiation skills,
     *         depending on campaign options. If all selected skills are untrained, returns {@link SkillType#EXP_NONE}.
     *         Otherwise, returns the average, floored at 0.
     *     </li>
     *     <li>
     *         <b>All other roles:</b> Calculates the experience level using their associated skills and campaign averaging option.
     *     </li>
     * </ul>
     *
     * @param campaign  the campaign context, providing options and relevant configuration
     * @param secondary if {@code true}, evaluates the person's secondary role; if {@code false}, evaluates the primary
     *                  role
     *
     * @return the calculated experience level for the relevant role, or {@link SkillType#EXP_NONE} if not qualified
     */
    public int getExperienceLevel(final Campaign campaign, final boolean secondary) {
        final PersonnelRole role = secondary ? getSecondaryRole() : getPrimaryRole();

        final CampaignOptions campaignOptions = campaign.getCampaignOptions();
        final boolean doAdminCountNegotiation = campaignOptions.isAdminExperienceLevelIncludeNegotiation();
        final boolean isUseArtillery = campaignOptions.isUseArtillery();
        final boolean isAlternativeQualityAveraging = campaignOptions.isAlternativeQualityAveraging();
        final boolean isUseAgingEffects = campaignOptions.isUseAgeEffects();
        final boolean isClanCampaign = campaign.isClanCampaign();
        final LocalDate today = campaign.getLocalDate();

        final SkillModifierData skillModifierData = getSkillModifierData(isUseAgingEffects, isClanCampaign, today);

        // Optional skills such as Admin for Techs are not counted towards the character's experience level, except
        // in the special case of Vehicle Gunners. So we only want to fetch the base professions.
        List<String> associatedSkillNames = role.getSkillsForProfession();

        return switch (role) {
            case VEHICLE_CREW_GROUND, VEHICLE_CREW_NAVAL, VEHICLE_CREW_VTOL -> {
                if (!isUseArtillery) {
                    yield calculateExperienceLevelForProfession(associatedSkillNames,
                          isAlternativeQualityAveraging,
                          skillModifierData);
                } else {
                    Skill gunnery = getSkill(S_GUN_VEE);
                    int gunneryExperienceLevel = gunnery == null ?
                                                       EXP_NONE :
                                                       gunnery.getExperienceLevel(skillModifierData);
                    Skill artillery = getSkill(S_ARTILLERY);
                    int artilleryExperienceLevel = artillery == null ?
                                                         EXP_NONE :
                                                         artillery.getExperienceLevel(skillModifierData);

                    if (artilleryExperienceLevel > gunneryExperienceLevel) {
                        associatedSkillNames.remove(S_GUN_VEE);
                        associatedSkillNames.add(S_ARTILLERY);
                    }

                    yield calculateExperienceLevelForProfession(associatedSkillNames,
                          isAlternativeQualityAveraging,
                          skillModifierData);
                }
            }
            case SOLDIER -> {
                int highestExperienceLevel = EXP_NONE;
                for (String relevantSkill : INFANTRY_GUNNERY_SKILLS) {
                    Skill skill = getSkill(relevantSkill);

                    if (skill == null) {
                        continue;
                    }

                    int currentExperienceLevel = skill.getExperienceLevel(skillModifierData);
                    if (currentExperienceLevel > highestExperienceLevel) {
                        highestExperienceLevel = currentExperienceLevel;
                    }
                }

                yield highestExperienceLevel;
            }
            case ADMINISTRATOR_COMMAND, ADMINISTRATOR_LOGISTICS, ADMINISTRATOR_TRANSPORT, ADMINISTRATOR_HR -> {
                int adminLevel = getSkillLevelOrNegative(S_ADMIN, skillModifierData);
                adminLevel = adminLevel == -1 ? 0 : adminLevel;

                int negotiationLevel = getSkillLevelOrNegative(S_NEGOTIATION, skillModifierData);
                negotiationLevel = negotiationLevel == -1 ? 0 : negotiationLevel;

                int levelSum;
                int divisor;

                if (doAdminCountNegotiation) {
                    levelSum = adminLevel + negotiationLevel;
                    divisor = 2;
                } else {
                    levelSum = adminLevel;
                    divisor = 1;
                }

                if (levelSum == -divisor) {
                    yield EXP_NONE;
                } else {
                    yield Math.max(0, levelSum / divisor);
                }
            }
            default -> calculateExperienceLevelForProfession(associatedSkillNames,
                  isAlternativeQualityAveraging,
                  skillModifierData);
        };
    }

    /**
     * Calculates the experience level for a profession based on the specified skill names and quality averaging
     * method.
     *
     * <p>If the provided list of skill names is empty, this method returns {@link SkillType#EXP_REGULAR} by default.
     * If any skill is missing or its type cannot be determined, {@link SkillType#EXP_NONE} is returned.</p>
     *
     * <ul>
     *     <li>
     *         <b>Standard Averaging:</b> If {@code isAlternativeQualityAveraging} is {@code false}, the experience
     *         level is determined by averaging the levels of all provided skills and converting the average to an
     *         experience level using the first skill's type.
     *     </li>
     *     <li>
     *         <b>Alternative Quality Averaging:</b> If {@code isAlternativeQualityAveraging} is {@code true}, the
     *         method checks if all experience levels for the listed skills are equal. If they are, that shared
     *         experience level is returned. Otherwise, standard averaging is used as described above.
     *     </li>
     * </ul>
     *
     * @param skillNames                    list of skill names relevant to the profession
     * @param isAlternativeQualityAveraging if {@code true}, uses the alternative averaging method; if {@code false},
     *                                      uses standard averaging
     *
     * @return the determined experience level, or {@link SkillType#EXP_NONE} if an error occurs or prerequisite skills
     *       are missing
     *
     * @author Illiani
     * @since 0.50.06
     */
    private int calculateExperienceLevelForProfession(List<String> skillNames, boolean isAlternativeQualityAveraging,
          SkillModifierData skillModifierData) {
        if (skillNames.isEmpty()) {
            // If we're not tracking skills for this profession, it always counts as REGULAR
            return EXP_REGULAR;
        }
        int totalSkillLevel = 0;
        boolean areAllEqual = true;
        Integer expectedExperienceLevel = null;

        for (String skillName : skillNames) {
            Skill skill = getSkill(skillName);
            if (skill == null) {
                // If a character is missing a skill, it means they're unqualified for a profession. They will lose
                // that profession the next time the campaign is loaded. We don't remove it here as that would
                // require passing in a bunch of extra information that is largely irrelevant.
                return EXP_NONE;
            }

            SkillType skillType = getType(skillName);
            if (skillType == null) {
                LOGGER.warn("Unable to find skill type for {}. Experience level assessment aborted", skillName);
                return EXP_NONE;
            }

            int individualSkillLevel = skill.getTotalSkillLevel(skillModifierData);
            totalSkillLevel += individualSkillLevel;

            if (isAlternativeQualityAveraging) {
                int expLevel = skill.getExperienceLevel(skillModifierData);
                if (expectedExperienceLevel == null) {
                    expectedExperienceLevel = expLevel;
                } else if (!expectedExperienceLevel.equals(expLevel)) {
                    areAllEqual = false;
                }
            }
        }

        if (isAlternativeQualityAveraging && areAllEqual) {
            return expectedExperienceLevel;
        }

        int averageSkillLevel = (int) floor((double) totalSkillLevel / skillNames.size());

        Skill skill = getSkill(skillNames.get(0));
        if (skill == null) {
            return EXP_NONE;
        }

        return skill.getType().getExperienceLevel(averageSkillLevel);
    }

    /**
     * Retrieves the skills associated with the character's profession. The skills returned depend on whether the
     * personnel's primary or secondary role is being queried and may also vary based on the campaign's configuration
     * settings, such as whether artillery skills are enabled.
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

        final CampaignOptions campaignOptions = campaign.getCampaignOptions();
        final boolean isAdminsHaveNegotiation = campaignOptions.isAdminsHaveNegotiation();
        final boolean isDoctorsUseAdministration = campaignOptions.isDoctorsUseAdministration();
        final boolean isTechsUseAdministration = campaignOptions.isTechsUseAdministration();
        final boolean isUseArtillery = campaignOptions.isUseArtillery();

        return profession.getSkillsForProfession(isAdminsHaveNegotiation,
              isDoctorsUseAdministration,
              isTechsUseAdministration,
              isUseArtillery);
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

    /**
     * Returns the person's title (rank) and surname as a single string.
     *
     * <p>If the person has an assigned rank, the rank (followed by a space) will precede the surname. If no rank is
     * available, only the surname is returned. If an exception occurs while retrieving the rank (for example, if the
     * person has not been assigned a rank system), the method will ignore the exception and return only the
     * surname.</p>
     *
     * <p>This design ensures robust behavior for test cases and scenarios where the person may not have a rank
     * assignment.</p>
     *
     * @return a string containing the person's rank (if any) and surname
     *
     * @author Illiani
     * @since 0.50.06
     */
    public String getTitleAndSurname() {
        String rank = "";

        try {
            rank = getRankName();

            if (!rank.isBlank()) {
                rank = rank + ' ';
            }
        } catch (Exception ignored) {
            // This try-catch exists to allow us to more easily test Person objects. Previously, if
            // a method included 'getTitleAndSurname' it would break if the Person object hadn't been
            // assigned a Rank System.
        }

        return rank + getSurname();
    }

    public String makeHTMLRank() {
        return String.format("<html><div id=\"%s\">%s</div></html>", getId(), getRankName().trim());
    }

    public String getHyperlinkedFullTitle() {
        return String.format("<a href='PERSON:%s'>%s</a>", getId(), getFullTitle());
    }

    public String getFullTitleAndProfessions() {
        return getFullTitle() + " (" + getPrimaryRoleDesc() + " / " + getSecondaryRoleDesc() + ')';
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
        return " <font color='" + getNegativeColor() + "'><b>Failed to heal.</b></font>";
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

    /**
     * Retrieves the experience level for a specified skill by name, with options to account for aging effects and
     * campaign type.
     *
     * <p>This method calculates the experience level for the given skill, applying adjustments based on aging effects,
     * campaign context, and the current date. If the skill is not found, {@code 0} is returned.</p>
     *
     * @param skillName         the name of the skill to retrieve
     * @param isUseAgingEffects {@code true} to include aging effects in reputation adjustment, {@code false} otherwise
     * @param isClanCampaign    {@code true} if the context is a Clan campaign, {@code false} otherwise
     * @param today             the current date used for age-related calculations
     *
     * @return the corresponding experience level for the skill, or {@code 0} if the skill does not exist
     */
    public int getSkillLevel(final String skillName, boolean isUseAgingEffects, boolean isClanCampaign,
          LocalDate today) {
        final Skill skill = getSkill(skillName);
        SkillModifierData skillModifierData = getSkillModifierData(isUseAgingEffects, isClanCampaign, today);

        return (skill == null) ? 0 : skill.getExperienceLevel(skillModifierData);
    }

    /**
     * Returns the experience level for the specified skill, or {@code -1} if the skill is not present.
     *
     * <p>If the entity has the specified skill, this method retrieves the skill and returns its experience level,
     * potentially taking into account any configured options or attribute modifiers. Otherwise, it returns {@code -1}
     * to indicate that the skill is not available.</p>
     *
     * @param skillName the name of the skill to query
     *
     * @return the experience level of the skill, or {@code -1} if the skill is not found
     */
    public int getSkillLevelOrNegative(final String skillName, boolean isUseAgingEffects, boolean isClanCampaign,
          LocalDate today) {
        SkillModifierData skillModifierData = getSkillModifierData(isUseAgingEffects, isClanCampaign, today);
        return getSkillLevelOrNegative(skillName, skillModifierData);
    }

    public int getSkillLevelOrNegative(final String skillName, SkillModifierData skillModifierData) {
        if (hasSkill(skillName)) {
            return getSkill(skillName).getExperienceLevel(skillModifierData);
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

    /**
     * @return the number of skills learned by the character.
     */
    public int getSkillNumber() {
        return skills.size();
    }

    /**
     * Returns a list of skill names that the current object possesses, filtered by the specified skill subtypes.
     *
     * <p>For each skill subtype provided, this method collects all skill names associated
     * with those subtypes, then adds to the result only those skills that the object is known to have (i.e., those for
     * which {@code hasSkill(skillName)} returns true).</p>
     *
     * @param skillSubTypes the list of {@link SkillSubType} to use for filtering skills
     *
     * @return a {@link List} of skill names that are both of the specified subtypes and known to the object
     *
     * @author Illiani
     * @since 0.50.06
     */
    public List<String> getKnownSkillsBySkillSubType(List<SkillSubType> skillSubTypes) {
        List<String> knownSkills = new ArrayList<>();
        for (String skillName : getSkillsBySkillSubType(skillSubTypes)) {
            if (hasSkill(skillName)) {
                knownSkills.add(skillName);
            }
        }

        return knownSkills;
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
        return ((hits > 0) || needsAMFixing()) && getStatus().isActiveFlexible();
    }

    /**
     * @deprecated No longer in use
     */
    @Deprecated(since = "0.50.06", forRemoval = true)
    public String succeed() {
        heal();
        return " <font color='" +
                     getPositiveColor() +
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
     * @return a html-coded list that says what abilities are enabled for this pilot
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
        boolean hasTraumaticPast = options.booleanOption(COMPULSION_TRAUMATIC_PAST);
        int modifier = hasTraumaticPast ? -1 : 0;
        return options.intOption(OptionsConstants.EDGE) - unlucky + modifier;
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
     * @return a html-coded tooltip that says what edge will be used
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

    /**
     * Determines if the user possesses the required skills and role to operate (pilot/drive) the given entity.
     *
     * <p>The method checks the type of the entity and validates whether the corresponding piloting skill and role
     * are assigned. Supported types include Land-Air Mek, Mek, VTOL, tank (including variants for marine and ground
     * modes), conventional fighter, small craft, jumpship, aerospace unit, battle armor, infantry, and ProtoMek.</p>
     *
     * @param entity the entity to check for piloting/driving capability. If {@code null}, returns {@code false}.
     *
     * @return {@code true} if the user is qualified to pilot or drive the specified entity; {@code false} otherwise
     */
    public boolean canDrive(final Entity entity) {
        if (entity == null) {
            return false;
        }

        if (entity instanceof LandAirMek) {
            return hasSkill(S_PILOT_MEK) && hasSkill(S_PILOT_AERO) && isRole(PersonnelRole.LAM_PILOT);
        } else if (entity instanceof Mek) {
            return hasSkill(S_PILOT_MEK) && isRole(PersonnelRole.MEKWARRIOR);
        } else if (entity instanceof VTOL) {
            return hasSkill(S_PILOT_VTOL) && isRole(PersonnelRole.VEHICLE_CREW_VTOL);
        } else if (entity instanceof Tank) {
            if (entity.getMovementMode().isMarine()) {
                return hasSkill(S_PILOT_NVEE) && isRole(PersonnelRole.VEHICLE_CREW_NAVAL);
            } else {
                return hasSkill(S_PILOT_GVEE) && isRole(PersonnelRole.VEHICLE_CREW_GROUND);
            }
        } else if (entity instanceof ConvFighter) {
            return hasSkill(S_PILOT_JET) && isRole(PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT);
        } else if ((entity instanceof SmallCraft) || (entity instanceof Jumpship)) {
            return hasSkill(S_PILOT_SPACE) && isRole(PersonnelRole.VESSEL_PILOT);
        } else if (entity instanceof Aero) {
            return hasSkill(S_PILOT_AERO) && isRole(PersonnelRole.AEROSPACE_PILOT);
        } else if (entity instanceof BattleArmor) {
            return hasSkill(S_GUN_BA) && isRole(PersonnelRole.BATTLE_ARMOUR);
        } else if (entity instanceof Infantry) {
            if (isRole(PersonnelRole.SOLDIER)) {
                for (String skill : INFANTRY_GUNNERY_SKILLS) {
                    if (hasSkill(skill)) {
                        return true;
                    }
                }
            }

            return false;
        } else if (entity instanceof ProtoMek) {
            return hasSkill(S_GUN_PROTO) && isRole(PersonnelRole.PROTOMEK_PILOT);
        } else {
            return false;
        }
    }

    /**
     * Determines if the user has the appropriate skills and role to operate the weapon systems (gun) of the given
     * entity.
     *
     * <p>This method evaluates the entity type and ensures that the required gunnery skill and role are present. It
     * supports a range of unit types such as Land-Air Mek, Mek, tank, conventional fighter, small craft, jumpship,
     * aerospace unit, battle armor, infantry, and ProtoMek.</p>
     *
     * @param entity the entity to check for gunnery capability. If {@code null}, returns {@code false}.
     *
     * @return {@code true} if the user is qualified to operate the weapons of the specified entity; {@code false}
     *       otherwise
     */
    public boolean canGun(final Entity entity) {
        if (entity == null) {
            return false;
        }

        if (entity instanceof LandAirMek) {
            return hasSkill(S_GUN_MEK) && hasSkill(S_GUN_AERO) && isRole(PersonnelRole.LAM_PILOT);
        } else if (entity instanceof Mek) {
            return hasSkill(S_GUN_MEK) && isRole(PersonnelRole.MEKWARRIOR);
        } else if (entity instanceof Tank) {
            if (entity.getMovementMode().isMarine()) {
                return hasSkill(S_GUN_VEE) && isRole(PersonnelRole.VEHICLE_CREW_NAVAL);
            } else if (entity.getMovementMode().isVTOL()) {
                return hasSkill(S_GUN_VEE) && isRole(PersonnelRole.VEHICLE_CREW_VTOL);
            } else {
                return hasSkill(S_GUN_VEE) && isRole(PersonnelRole.VEHICLE_CREW_GROUND);
            }
        } else if (entity instanceof ConvFighter) {
            return hasSkill(S_GUN_JET) && isRole(PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT);
        } else if ((entity instanceof SmallCraft) || (entity instanceof Jumpship)) {
            return hasSkill(S_GUN_SPACE) && isRole(PersonnelRole.VESSEL_GUNNER);
        } else if (entity instanceof Aero) {
            return hasSkill(S_GUN_AERO) && isRole(PersonnelRole.AEROSPACE_PILOT);
        } else if (entity instanceof BattleArmor) {
            return hasSkill(S_GUN_BA) && isRole(PersonnelRole.BATTLE_ARMOUR);
        } else if (entity instanceof Infantry) {
            if (isRole(PersonnelRole.SOLDIER)) {
                for (String skill : INFANTRY_GUNNERY_SKILLS) {
                    if (hasSkill(skill)) {
                        return true;
                    }
                }
            }

            return false;
        } else if (entity instanceof ProtoMek) {
            return hasSkill(S_GUN_PROTO) && isRole(PersonnelRole.PROTOMEK_PILOT);
        } else {
            return false;
        }
    }

    /**
     * Determines if the user holds the necessary technical skills to service or repair the specified entity.
     *
     * <p>The method inspects the entity type and checks for the corresponding technical skills required to perform
     * maintenance or repairs. Supported types include Mek, ProtoMek, dropship, jumpship, aerospace unit, battle armor,
     * and tank.</p>
     *
     * @param entity the entity to assess for technical capability. If {@code null}, returns {@code false}.
     *
     * @return {@code true} if the user is qualified to service or repair the given entity; {@code false} otherwise
     */
    public boolean canTech(final Entity entity) {
        if (entity == null) {
            return false;
        }

        if ((entity instanceof Mek) || (entity instanceof ProtoMek)) {
            return hasSkill(S_TECH_MEK) && isTechMek();
        } else if (entity instanceof Dropship || entity instanceof Jumpship) {
            return hasSkill(S_TECH_VESSEL) && isTechLargeVessel();
        } else if (entity instanceof Aero) {
            return hasSkill(S_TECH_AERO) && isTechAero();
        } else if (entity instanceof BattleArmor) {
            return hasSkill(S_TECH_BA) && isTechBA();
        } else if (entity instanceof Tank) {
            return hasSkill(S_TECH_MECHANIC) && (isTechMechanic() || isVehicleCrew());
        } else {
            return false;
        }
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

    public @Nullable Entity getEntity() {
        if (unit == null) {
            return null;
        }

        return unit.getEntity();
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
        // Doctors
        if (primaryRole.isDoctor()) {
            this.minutesLeft = PRIMARY_ROLE_SUPPORT_TIME;
            this.overtimeLeft = PRIMARY_ROLE_OVERTIME_SUPPORT_TIME;
            return;
        }

        if (secondaryRole.isDoctor()) {
            this.minutesLeft = SECONDARY_ROLE_SUPPORT_TIME;
            this.overtimeLeft = SECONDARY_ROLE_OVERTIME_SUPPORT_TIME;
            return;
        }

        // Technicians
        if (primaryRole.isTech()) {
            double multiplier = calculateTechTimeMultiplier(isTechsUseAdministration);
            this.minutesLeft = (int) Math.round(PRIMARY_ROLE_SUPPORT_TIME * multiplier);
            this.overtimeLeft = (int) Math.round(PRIMARY_ROLE_OVERTIME_SUPPORT_TIME * multiplier);
            return;
        }

        if (secondaryRole.isTechSecondary()) {
            double multiplier = calculateTechTimeMultiplier(isTechsUseAdministration);
            this.minutesLeft = (int) Math.round(SECONDARY_ROLE_SUPPORT_TIME * multiplier);
            this.overtimeLeft = (int) Math.round(SECONDARY_ROLE_OVERTIME_SUPPORT_TIME * multiplier);
        }
    }

    /**
     * Determines and returns the tech skill with the highest experience level possessed by this entity.
     *
     * <p>This method evaluates all available technical skills (such as Mek, Aero, Mechanic, and Battle Armor tech
     * skills) and selects the one with the greatest experience level. If multiple skills are present, the one with the
     * highest experience is returned. If no relevant tech skills are found, returns {@code null}.</p>
     *
     * @return the {@link Skill} object representing the highest-level technical skill, or {@code null} if none are
     *       present
     */
    public @Nullable Skill getBestTechSkill() {
        SkillModifierData skillModifierData = getSkillModifierData();

        Skill skill = null;
        int level = EXP_NONE;

        if (hasSkill(S_TECH_MEK) && getSkill(S_TECH_MEK).getExperienceLevel(skillModifierData) > level) {
            skill = getSkill(S_TECH_MEK);
            level = getSkill(S_TECH_MEK).getExperienceLevel(skillModifierData);
        }
        if (hasSkill(S_TECH_AERO) && getSkill(S_TECH_AERO).getExperienceLevel(skillModifierData) > level) {
            skill = getSkill(S_TECH_AERO);
            level = getSkill(S_TECH_AERO).getExperienceLevel(skillModifierData);
        }
        if (hasSkill(S_TECH_MECHANIC) &&
                  getSkill(S_TECH_MECHANIC).getExperienceLevel(skillModifierData) > level) {
            skill = getSkill(S_TECH_MECHANIC);
            level = getSkill(S_TECH_MECHANIC).getExperienceLevel(skillModifierData);
        }
        if (hasSkill(S_TECH_BA) && getSkill(S_TECH_BA).getExperienceLevel(skillModifierData) > level) {
            skill = getSkill(S_TECH_BA);
        }
        return skill;
    }

    public boolean isTech() {
        return isTechMek() || isTechAero() || isTechMechanic() || isTechBA() || isVehicleCrew();
    }

    /**
     * Checks if the person is a tech, includes mektek, mechanic, aerotek, BAtek and the non-cannon "large vessel tek"
     *
     * @return true if the person is a tech
     */
    public boolean isTechExpanded() {
        return isTechMek() || isTechAero() || isTechMechanic() || isVehicleCrew() || isTechBA() || isTechLargeVessel();
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

    public boolean isVehicleCrew() {
        boolean hasSkill = hasSkill(S_TECH_MECHANIC);
        return hasSkill && (getPrimaryRole().isCombatTechnician() || getSecondaryRole().isCombatTechnician());
    }

    public boolean isAsTech() {
        boolean hasSkill = hasSkill(S_ASTECH);
        return hasSkill && (getPrimaryRole().isAstech() || getSecondaryRole().isAstech());
    }

    /**
     * Checks whether this character satisfies the requirements for a given personnel role.
     *
     * <p>This method verifies that the specified role matches either the character's primary or secondary role, and
     * ensures the character possesses all the required skills for that profession. If any required skill is missing, a
     * warning is logged and the method returns {@code false}.</p>
     *
     * @param role the {@link PersonnelRole} to check against this character
     *
     * @return {@code true} if the character matches the specified role and has all necessary skills; {@code false}
     *       otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean isRole(PersonnelRole role) {
        // Does the character have the appropriate role?
        if (!role.equals(getPrimaryRole()) && !role.equals(getSecondaryRole())) {
            return false;
        }

        // Do they have the skills for that role? This should be assumed, we include the check here as a safety net.
        for (String skillName : role.getSkillsForProfession()) {
            Skill skill = getSkill(skillName);
            if (skill == null) {
                LOGGER.warn("Unable to find skill {} needed for {} profession for {}",
                      skillName,
                      role.getLabel(false),
                      getFullTitle());
                return false;
            }
        }

        // If everything checks out, return true
        return true;
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
            SkillModifierData skillModifierData = getSkillModifierData();
            experienceLevel = administration.getExperienceLevel(skillModifierData);
        }

        administrationMultiplier += experienceLevel * TECH_ADMINISTRATION_MULTIPLIER;

        return administrationMultiplier;
    }

    public boolean isAdministrator() {
        return (getPrimaryRole().isAdministrator() || getSecondaryRole().isAdministrator());
    }

    public boolean isDoctor() {
        return hasSkill(S_SURGERY) && (getPrimaryRole().isDoctor() || getSecondaryRole().isDoctor());
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
            SkillModifierData skillModifierData = getSkillModifierData();
            experienceLevel = administration.getExperienceLevel(skillModifierData);
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

    public boolean isCivilian() {
        return (getPrimaryRole().isCivilian() && getSecondaryRole().isCivilian());
    }

    public boolean isTaskOvertime(final IPartWork partWork) {
        return (partWork.getTimeLeft() > getMinutesLeft()) && (getOvertimeLeft() > 0);
    }

    public @Nullable Skill getSkillForWorkingOn(final IPartWork part) {
        final Unit unit = part.getUnit();
        Skill skill = getSkillForWorkingOn(unit);
        if (skill != null) {
            return skill;
        }

        SkillModifierData skillModifierData = getSkillModifierData();

        // check spare parts
        // return the best one
        if (part.isRightTechType(S_TECH_MEK) && hasSkill(S_TECH_MEK)) {
            skill = getSkill(S_TECH_MEK);
        }

        if (part.isRightTechType(S_TECH_BA) && hasSkill(S_TECH_BA)) {
            if ((skill == null) ||
                      (skill.getFinalSkillValue(skillModifierData) >
                             getSkill(S_TECH_BA).getFinalSkillValue(skillModifierData))) {
                skill = getSkill(S_TECH_BA);
            }
        }

        if (part.isRightTechType(S_TECH_AERO) && hasSkill(S_TECH_AERO)) {
            if ((skill == null) ||
                      (skill.getFinalSkillValue(skillModifierData) >
                             getSkill(S_TECH_AERO).getFinalSkillValue(skillModifierData))) {
                skill = getSkill(S_TECH_AERO);
            }
        }

        if (part.isRightTechType(S_TECH_MECHANIC) && hasSkill(S_TECH_MECHANIC)) {
            if ((skill == null) ||
                      (skill.getFinalSkillValue(skillModifierData) >
                             getSkill(S_TECH_MECHANIC).getFinalSkillValue(skillModifierData))) {
                skill = getSkill(S_TECH_MECHANIC);
            }
        }

        if (part.isRightTechType(S_TECH_VESSEL) && hasSkill(S_TECH_VESSEL)) {
            if ((skill == null) ||
                      (skill.getFinalSkillValue(skillModifierData) >
                             getSkill(S_TECH_VESSEL).getFinalSkillValue(skillModifierData))) {
                skill = getSkill(S_TECH_VESSEL);
            }
        }

        if (skill != null) {
            return skill;
        }
        // if we are still here then we didn't have the right tech skill, so return the
        // highest
        // of any tech skills that we do have
        if (hasSkill(S_TECH_MEK)) {
            skill = getSkill(S_TECH_MEK);
        }

        if (hasSkill(S_TECH_BA)) {
            if ((skill == null) ||
                      (skill.getFinalSkillValue(skillModifierData) >
                             getSkill(S_TECH_BA).getFinalSkillValue(skillModifierData))) {
                skill = getSkill(S_TECH_BA);
            }
        }

        if (hasSkill(S_TECH_MECHANIC)) {
            if ((skill == null) ||
                      (skill.getFinalSkillValue(skillModifierData) >
                             getSkill(S_TECH_MECHANIC).getFinalSkillValue(skillModifierData))) {
                skill = getSkill(S_TECH_MECHANIC);
            }
        }

        if (hasSkill(S_TECH_AERO)) {
            if ((skill == null) ||
                      (skill.getFinalSkillValue(skillModifierData) >
                             getSkill(S_TECH_AERO).getFinalSkillValue(skillModifierData))) {
                skill = getSkill(S_TECH_AERO);
            }
        }

        return skill;
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
                         !(unit.getEntity() instanceof Jumpship) &&
                         hasSkill(S_TECH_AERO)) {
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
        int level = EXP_NONE;
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

    public boolean getHasGainedVeterancySPA() {
        return hasGainedVeterancySPA;
    }

    public void setHasGainedVeterancySPA(final boolean hasGainedVeterancySPA) {
        this.hasGainedVeterancySPA = hasGainedVeterancySPA;
    }

    public int getConnections() {
        return connections;
    }

    /**
     * Calculates and returns the character's adjusted Connections value.
     *
     * <p>If the character has burned their Connections, their Connections value is fixed as 0.</p>
     *
     * <p>If the character is suffering from an episode of Clinical Paranoia, their Connections value is fixed as
     * 0.</p>
     *
     * <p>If the character has the {@link PersonnelOptions#COMPULSION_XENOPHOBIA} SPA their Connections value is
     * decreased by 1.</p>
     *
     * <p>If the character has the {@link PersonnelOptions#ATOW_CITIZENSHIP} SPA their Connections value is
     * increased by 1.</p>
     *
     * <p>If the character has the {@link PersonnelOptions#COMPULSION_MILD_PARANOIA} SPA their Connections value is
     * reduced by 1.</p>
     *
     * <p>The Connections value is clamped within the allowed minimum and maximum range before being returned.</p>
     *
     * @return the character's Connections value, clamped within the minimum and maximum limits
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getAdjustedConnections() {
        if (burnedConnectionsEndDate != null) {
            return 0;
        }

        if (sufferingFromClinicalParanoia) {
            return 0;
        }

        boolean hasXenophobia = options.booleanOption(COMPULSION_XENOPHOBIA);
        int modifiers = (hasXenophobia ? -1 : 0);

        boolean hasCitizenship = options.booleanOption(ATOW_CITIZENSHIP);
        modifiers += (hasCitizenship ? 1 : 0);

        boolean hasMildParanoia = options.booleanOption(COMPULSION_MILD_PARANOIA);
        modifiers += (hasMildParanoia ? -1 : 0);

        modifiers += getDarkSecretModifier(false);

        return clamp(connections + modifiers, MINIMUM_CONNECTIONS, MAXIMUM_CONNECTIONS);
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
        this.wealth = clamp(wealth, MINIMUM_WEALTH, MAXIMUM_WEALTH);
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
     * Returns the current {@link ExtraIncome} value.
     *
     * @return the {@link ExtraIncome} object representing the current extra income setting.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public ExtraIncome getExtraIncome() {
        return extraIncome;
    }

    /**
     * Returns the trait level associated with the current {@link ExtraIncome}.
     *
     * @return the integer trait level for the current extra income value.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public int getExtraIncomeTraitLevel() {
        return extraIncome.getTraitLevel();
    }

    /**
     * Sets the {@link ExtraIncome} value based on a specified trait level.
     *
     * <p>The trait level is clamped to the allowed range before being converted into an {@link ExtraIncome}
     * object.</p>
     *
     * @param traitLevel the integer value representing the trait level to set for extra income.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public void setExtraIncomeFromTraitLevel(final int traitLevel) {
        int newExtraIncomeTraitLevel = clamp(traitLevel, MINIMUM_EXTRA_INCOME, MAXIMUM_EXTRA_INCOME);
        extraIncome = ExtraIncome.extraIncomeParseFromInteger(newExtraIncomeTraitLevel);
    }

    /**
     * Directly assigns an {@link ExtraIncome} object.
     *
     * @param extraIncome the {@link ExtraIncome} instance to assign.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public void setExtraIncomeDirect(final ExtraIncome extraIncome) {
        this.extraIncome = extraIncome;
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
     * @param rankNumeric       The rank index of the character, which can adjust the reputation modifier in clan-based
     *                          campaigns.
     *
     * @return The adjusted reputation value, accounting for factors like age, clan campaign status, bloodname
     *       possession, and rank. If aging effects are disabled, the base reputation value is returned.
     */
    public int getAdjustedReputation(boolean isUseAgingEffects, boolean isClanCampaign, LocalDate today,
          int rankNumeric) {
        final int PATHOLOGIC_RACISM_REPUTATION_PENALTY = -2;

        int modifiers = isUseAgingEffects ?
                              getReputationAgeModifier(getAge(today),
                                    isClanCampaign,
                                    !isNullOrBlank(bloodname),
                                    rankNumeric) :
                              0;

        boolean hasRacism = options.booleanOption(COMPULSION_RACISM);
        modifiers -= hasRacism ? 1 : 0;

        boolean hasPathologicRacism = options.booleanOption(COMPULSION_PATHOLOGIC_RACISM);
        modifiers += hasPathologicRacism ? PATHOLOGIC_RACISM_REPUTATION_PENALTY : 0;

        boolean hasXenophobia = options.booleanOption(COMPULSION_XENOPHOBIA);
        modifiers -= hasXenophobia ? 1 : 0;

        modifiers += getDarkSecretModifier(true);

        return clamp(reputation + modifiers, MINIMUM_REPUTATION, MAXIMUM_REPUTATION);
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

    public int getBloodmark() {
        return bloodmark;
    }

    public Money getBloodmarkValue() {
        return Money.of(bloodmark);
    }

    public void setBloodmark(final int unlucky) {
        this.bloodmark = clamp(unlucky, MINIMUM_BLOODMARK, MAXIMUM_BLOODMARK);
    }

    public void changeBloodmark(final int delta) {
        int newValue = bloodmark + delta;
        bloodmark = clamp(newValue, MINIMUM_BLOODMARK, MAXIMUM_BLOODMARK);
    }

    public List<LocalDate> getBloodhuntSchedule() {
        return bloodhuntSchedule;
    }

    public void addBloodhuntDate(final LocalDate date) {
        bloodhuntSchedule.add(date);
    }

    public void removeBloodhuntDate(final LocalDate date) {
        bloodhuntSchedule.remove(date);
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
            LOGGER.warn("(setAttributeScore) SkillAttribute is null or NONE.");
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
            LOGGER.error("(getAttributeScore) SkillAttribute is null or NONE.");
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
            case BODY, REFLEXES, DEXTERITY, INTELLIGENCE, WILLPOWER, EDGE ->
                  atowAttributes.getAttributeScore(attribute);
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
            LOGGER.warn("(getAttributeCap) SkillAttribute is null or NONE.");
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
     * to {@link Attributes#setAttributeScore(Phenotype, PersonnelOptions, SkillAttribute, int)} to ensure it compiles
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
            LOGGER.warn("(changeAttributeScore) SkillAttribute is null or NONE.");
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

    public List<LogEntry> getPatientLog() {
        patientLog.sort(Comparator.comparing(LogEntry::getDate));
        return patientLog;
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

    public void addPatientLogEntry(final LogEntry entry) {
        patientLog.add(entry);
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
     * Determines whether the person has any injuries, possibly filtering by permanence.
     *
     * <ul>
     *     <li>If {@code permanentCheck} is {@code false}, this method returns {@code true} if the person has any
     *     recorded injuries.</li>
     *     <li>If {@code permanentCheck} is {@code true}, it will return {@code true} only if the person has at least
     *     one injury that is either non-permanent or has a remaining recovery time greater than zero. Otherwise, it
     *     returns {@code false}.</li>
     * </ul>
     *
     * @param permanentCheck if {@code true}, only injuries that are not permanent or have time remaining are
     *                       considered; if {@code false}, any injury will be counted
     *
     * @return {@code true} if the person has injuries matching the specified criteria; {@code false} otherwise
     */
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
        } else if (unit.getEntity().getTechLevel() > TechConstants.T_INTRO_BOX_SET) {
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
                LOGGER.error("Person {} ('{}') references missing unit {}", getId(), getFullName(), id);
            }
        }

        for (int ii = techUnits.size() - 1; ii >= 0; --ii) {
            final Unit techUnit = techUnits.get(ii);
            if (techUnit instanceof PersonUnitRef) {
                final Unit realUnit = campaign.getUnit(techUnit.getId());
                if (realUnit != null) {
                    techUnits.set(ii, realUnit);
                } else {
                    LOGGER.error("Person {} ('{}') techs missing unit {}", getId(), getFullName(), techUnit.getId());
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

    /**
     * Removes all skills from the collection that match the specified subtype.
     *
     * <p>Iterates safely over the current list of skills, removing each skill whose type corresponds to the given
     * {@link SkillSubType}.</p>
     *
     * @param subType the {@code SkillSubType} to remove from the collection
     *
     * @author Illiani
     * @since 0.50.06
     */
    public void removeAllSkillsOfSubType(SkillSubType subType) {
        // We make an iteration safe list so we can easily remove skills during the loop
        List<Skill> allSkills = new ArrayList<>(skills.getSkills());
        for (Skill skill : allSkills) {
            SkillType skillType = skill.getType();

            if (skillType.isSubTypeOf(subType)) {
                removeSkill(skillType.getName());
            }
        }
    }

    public void updateTimeData(LocalDate today) {
        boolean updateRecruitment = recruitment == null;
        boolean updateLastRankChange = lastRankChangeDate == null;

        // Nothing to update
        if (!updateRecruitment && !updateLastRankChange) {
            return;
        }

        if (isEmployed()) {
            LocalDate estimatedJoinDate = null;
            for (LogEntry logEntry : getPersonalLog()) {
                if (estimatedJoinDate == null) {
                    // If by some nightmare there is no Joined date just use the first entry.
                    estimatedJoinDate = logEntry.getDate();
                }
                if (logEntry.getDesc().startsWith("Joined ") ||
                          logEntry.getDesc().startsWith("Freed ") ||
                          logEntry.getDesc().startsWith("Promoted ") ||
                          logEntry.getDesc().startsWith("Demoted ")) {
                    estimatedJoinDate = logEntry.getDate();
                    break;
                }
            }

            if (estimatedJoinDate != null) {
                if (updateRecruitment) {
                    recruitment = estimatedJoinDate;
                }
                if (updateLastRankChange) {
                    lastRankChangeDate = estimatedJoinDate;
                }
                return;
            }

            if (joinedCampaign != null) {
                if (updateRecruitment) {
                    recruitment = null;
                }
                if (updateLastRankChange) {
                    lastRankChangeDate = null;
                }
                recruitment = joinedCampaign;
                return;
            }

            recruitment = today;
        }
    }

    /**
     * Resolves a gambling compulsion for the current person and adjusts their wealth accordingly.
     *
     * <p>If the person has the gambling compulsion option enabled, this method performs a d6 roll to determine
     * whether wealth is gained, lost, or unchanged, and formats the result as a localized string with appropriate
     * styling. If the gambling compulsion option is not present, the method returns an empty string.</p>
     *
     * <p>On a roll of 6, the person's wealth increases; on a roll of 4 or 5, it remains unchanged; and on a roll of
     * 1, 2, or 3, the person's wealth decreases.</p>
     *
     * @return a formatted localized result {@link String} reflecting the outcome, or an empty {@link String} if not
     *       applicable
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String gambleWealth() {
        boolean hasGamblingCompulsion = options.booleanOption(COMPULSION_GAMBLING);
        if (!hasGamblingCompulsion) {
            return "";
        }

        String key;
        String color;

        int roll = d6();
        switch (roll) {
            case 4, 5 -> {
                key = "neutral";
                color = getWarningColor();
            }
            case 6 -> {
                changeWealth(1);
                key = "success";
                color = getPositiveColor();
            }
            default -> { // 1, 2, 3
                changeWealth(-1);
                key = "failure";
                color = getNegativeColor();
            }
        }

        return String.format(resources.getString("gambling." + key), getHyperlinkedFullTitle(),
              spanOpeningWithCustomColor(color), CLOSING_SPAN_TAG, wealth);
    }

    /**
     * Processes the effects of discontinuation syndrome.
     *
     * <p>This method applies the symptoms and risks associated with compulsive addiction discontinuation, adjusted
     * by campaign options and current conditions:</p>
     *
     * <ul>
     *   <li>If Advanced Medical is available, {@link InjuryTypes#DISCONTINUATION_SYNDROME} is added; otherwise, Hits
     *   are incremented.</li>
     *   <li>If Fatigue is enabled, the character's Fatigue level increases.</li>
     *   <li>If the number of injuries or cumulative hits exceeds a defined threshold, the entity's status is changed
     *   to {@link PersonnelStatus#MEDICAL_COMPLICATIONS} (killed).</li>
     * </ul>
     *
     * @param campaign               the active {@link Campaign} in which the discontinuation syndrome is processed
     * @param useAdvancedMedical     {@code true} if Advanced Medical is enabled
     * @param useFatigue             {@code true} if Fatigue should be increased
     * @param hasCompulsionAddiction specifies if the character has the {@link PersonnelOptions#COMPULSION_ADDICTION}
     *                               Flaw.
     * @param failedWillpowerCheck   {@code true} if the character failed the check to resist their compulsion
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void processDiscontinuationSyndrome(Campaign campaign, boolean useAdvancedMedical, boolean useFatigue,
          // These boolean are here to ensure that we only ever pass in valid personnel
          boolean hasCompulsionAddiction, boolean failedWillpowerCheck) {
        final int FATIGUE_INCREASE = 2;
        final int DEATH_THRESHOLD = 5;


        if (hasCompulsionAddiction && failedWillpowerCheck) {
            if (useAdvancedMedical) {
                Injury injury = DISCONTINUATION_SYNDROME.newInjury(campaign, this, INTERNAL, 1);
                addInjury(injury);
            } else {
                hits++;
            }

            if (useFatigue) {
                changeFatigue(FATIGUE_INCREASE);
            }

            if ((getInjuries().size() > DEATH_THRESHOLD) || (hits > DEATH_THRESHOLD)) {
                changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.MEDICAL_COMPLICATIONS);
            }
        }
    }

    /**
     * Processes the effects of crippling flashbacks.
     *
     * <p>If the personnel has flashbacks and fails a willpower check, this method determines the outcome:</p>
     * <ul>
     *     <li>If advanced medical care is available, a new injury related to crippling flashbacks is added.</li>
     *     <li>Otherwise, the personnel takes additional damage (hits).</li>
     * </ul>
     *
     * <p>If the number of injuries or hits exceeds a predefined threshold, the character's status is updated to
     * {@link PersonnelStatus#MEDICAL_COMPLICATIONS} (killed).</p>
     *
     * @param campaign             The current campaign context.
     * @param useAdvancedMedical   {@code true} if advanced medical care is available; {@code false} otherwise.
     * @param hasFlashbacks        {@code true} if the personnel is suffering from flashbacks.
     * @param failedWillpowerCheck {@code true} if the personnel failed their willpower check due to flashbacks.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void processCripplingFlashbacks(Campaign campaign, boolean useAdvancedMedical,
          // These boolean are here to ensure that we only ever pass in valid personnel
          boolean hasFlashbacks, boolean failedWillpowerCheck) {
        final int DEATH_THRESHOLD = 5;

        if (hasFlashbacks && failedWillpowerCheck) {
            if (useAdvancedMedical) {
                Injury injury = CRIPPLING_FLASHBACKS.newInjury(campaign, this, INTERNAL, 1);
                addInjury(injury);
            } else {
                hits += 1;
            }

            if ((getInjuries().size() > DEATH_THRESHOLD) || (hits > DEATH_THRESHOLD)) {
                changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.MEDICAL_COMPLICATIONS);
            }
        }
    }

    /**
     * Processes the occurrence of a split personality event.
     *
     * <p>If the subject has split personality and fails a willpower check, an alternative personality is generated
     * (if needed), the personality is switched, and a description of the resulting personality is written using the
     * {@link PersonalityController}.</p>
     *
     * @param hasSplitPersonality  {@code true} if the subject is susceptible to having a split personality
     * @param failedWillpowerCheck {@code true} if the subject failed the willpower check prompting the split
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String processSplitPersonality(boolean hasSplitPersonality, boolean failedWillpowerCheck) {
        if (hasSplitPersonality && failedWillpowerCheck) {
            String originalName = getHyperlinkedFullTitle();

            if (isNullOrBlank(storedGivenName)) {
                generateAlternativePersonality();
            }

            switchPersonality();
            PersonalityController.writePersonalityDescription(this);

            return String.format(resources.getString("compulsion.personalityChange"), originalName,
                  spanOpeningWithCustomColor(getWarningColor()), CLOSING_SPAN_TAG, getFullTitle());
        }

        return "";
    }

    /**
     * Generates an alternative set of personality attributes (name, faction, traits) for the character.
     *
     * <p>This involves selecting a new faction of origin, generating a new name based on the faction, and creating a
     * set of alternative personality characteristics.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void generateAlternativePersonality() {
        Faction chosenFaction = generateSplitPersonalityOriginFaction();
        generateSplitPersonalityName(chosenFaction);
        generateSplitPersonalityPersonalityCharacteristics();
        storedLoyalty = d6(3);
    }

    /**
     * Generates alternative personality traits and applies them to the stored split personality profile.
     *
     * <p>Traits are randomly selected from {@link Aggression}, {@link Ambition}, {@link Greed}, and {@link Social},
     * with potential for up to four traits total. Additional characteristics such as a {@link PersonalityQuirk} trait
     * and {@link Reasoning} characteristics are randomly determined and stored.</p>
     *
     * @author Illiani
     * @see PersonalityController#generatePersonality(Person)
     * @since 0.50.07
     */
    private void generateSplitPersonalityPersonalityCharacteristics() {
        setStoredAggression(Aggression.NONE);
        setStoredAmbition(Ambition.NONE);
        setStoredGreed(Greed.NONE);
        setStoredSocial(Social.NONE);
        setStoredReasoning(Reasoning.AVERAGE);
        setStoredPersonalityQuirk(PersonalityQuirk.NONE);

        // Then we generate a new personality
        List<PersonalityTraitType> possibleTraits = new ArrayList<>();
        possibleTraits.add(PersonalityTraitType.AGGRESSION);
        possibleTraits.add(PersonalityTraitType.AMBITION);
        possibleTraits.add(PersonalityTraitType.GREED);
        possibleTraits.add(PersonalityTraitType.SOCIAL);
        possibleTraits.add(PersonalityTraitType.PERSONALITY_QUIRK);

        int iterations = 2;

        while (iterations != 0 && !possibleTraits.isEmpty()) {
            PersonalityTraitType pickedTrait = ObjectUtility.getRandomItem(possibleTraits);
            possibleTraits.remove(pickedTrait);
            iterations--;

            switch (pickedTrait) {
                case AGGRESSION -> {
                    String traitIndex = getTraitIndex(Aggression.MAJOR_TRAITS_START_INDEX);
                    setStoredAggression(Aggression.fromString(traitIndex));
                    setStoredAggressionDescriptionIndex(randomInt(Aggression.MAXIMUM_VARIATIONS));
                }
                case AMBITION -> {
                    String traitIndex = getTraitIndex(Ambition.MAJOR_TRAITS_START_INDEX);
                    setStoredAmbition(Ambition.fromString(traitIndex));
                    setStoredAmbitionDescriptionIndex(randomInt(Ambition.MAXIMUM_VARIATIONS));
                }
                case GREED -> {
                    String traitIndex = getTraitIndex(Greed.MAJOR_TRAITS_START_INDEX);
                    setStoredGreed(Greed.fromString(traitIndex));
                    setStoredGreedDescriptionIndex(randomInt(Greed.MAXIMUM_VARIATIONS));
                }
                case SOCIAL -> {
                    String traitIndex = getTraitIndex(Social.MAJOR_TRAITS_START_INDEX);
                    setStoredSocial(Social.fromString(traitIndex));
                    setStoredSocialDescriptionIndex(randomInt(Social.MAXIMUM_VARIATIONS));
                }
                case PERSONALITY_QUIRK -> {
                    int traitRoll = randomInt(PersonalityQuirk.values().length) + 1;
                    String traitIndex = String.valueOf(traitRoll);
                    setStoredPersonalityQuirk(PersonalityQuirk.fromString(traitIndex));
                    setStoredPersonalityQuirkDescriptionIndex(randomInt(PersonalityQuirk.MAXIMUM_VARIATIONS));
                }
                default -> {}
            }
        }

        // Always generate Reasoning
        int reasoningRoll = randomInt(8346);
        storedReasoning = generateReasoning(reasoningRoll);
    }

    /**
     * Generates a new split personality name based on the provided faction.
     *
     * <p>Uses the random name generator to assign a given name and surname appropriate to the gender, personnel
     * type, and the supplied faction's short name; stores the results.</p>
     *
     * @param chosenFaction the {@link Faction} selected as the origin of the split personality
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void generateSplitPersonalityName(Faction chosenFaction) {
        RandomNameGenerator.getInstance().generate(gender, clanPersonnel, chosenFaction.getShortName());

        String[] name = RandomNameGenerator.getInstance().generateGivenNameSurnameSplit(gender,
              clanPersonnel,
              chosenFaction.getShortName());
        storedGivenName = name[0];
        storedSurname = name[1];
    }

    /**
     * Randomly selects and returns a new faction to be used as the origin for a split personality.
     *
     * <p>Considers all active factions at the time of the subject's birthday, applying faction- and
     * personnel-specific constraints, then randomly chooses one and stores it.</p>
     *
     * @return the chosen {@link Faction} for the split personality's origin
     *
     * @author Illiani
     * @since 0.50.07
     */
    private Faction generateSplitPersonalityOriginFaction() {
        Set<Faction> possibleNewFaction = new HashSet<>();
        Collection<Faction> allActiveFactions = Factions.getInstance().getActiveFactions(birthday);
        for (Faction faction : allActiveFactions) {
            if (faction.isClan() &&
                      birthday.isBefore(BATTLE_OF_TUKAYYID) &&
                      !clanPersonnel) {
                continue;
            }
            possibleNewFaction.add(faction);
        }

        Faction chosenFaction = ObjectUtility.getRandomItem(possibleNewFaction);
        storedOriginFaction = chosenFaction;
        return chosenFaction;
    }

    /**
     * Switches the primary and stored personality attributes of the subject.
     *
     * <p>This method exchanges all major personal attributes, such as name, loyalty, origin faction, personality
     * traits, and their associated descriptions, between the primary and split personality profiles.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    void switchPersonality() {
        String transitionaryGivenName = givenName;
        setGivenName(storedGivenName);
        storedGivenName = transitionaryGivenName;

        String transitionarySurname = surname;
        setSurname(storedSurname);
        storedSurname = transitionarySurname;

        int transitionaryLoyalty = loyalty;
        loyalty = storedLoyalty;
        storedLoyalty = transitionaryLoyalty;

        Faction transitionaryOriginFaction = originFaction;
        originFaction = storedOriginFaction;
        storedOriginFaction = transitionaryOriginFaction;

        Aggression transitionaryAggression = aggression;
        aggression = storedAggression;
        storedAggression = transitionaryAggression;

        int transitionaryAggressionDescriptionIndex = aggressionDescriptionIndex;
        aggressionDescriptionIndex = storedAggressionDescriptionIndex;
        storedAggressionDescriptionIndex = transitionaryAggressionDescriptionIndex;

        Ambition transitionaryAmbition = ambition;
        ambition = storedAmbition;
        storedAmbition = transitionaryAmbition;

        int transitionaryAmbitionDescriptionIndex = ambitionDescriptionIndex;
        ambitionDescriptionIndex = storedAmbitionDescriptionIndex;
        storedAmbitionDescriptionIndex = transitionaryAmbitionDescriptionIndex;

        Greed transitionaryGreed = greed;
        greed = storedGreed;
        storedGreed = transitionaryGreed;

        int transitionaryGreedDescriptionIndex = greedDescriptionIndex;
        greedDescriptionIndex = storedGreedDescriptionIndex;
        storedGreedDescriptionIndex = transitionaryGreedDescriptionIndex;

        Social transitionarySocial = social;
        social = storedSocial;
        storedSocial = transitionarySocial;

        int transitionarySocialDescriptionIndex = socialDescriptionIndex;
        socialDescriptionIndex = storedSocialDescriptionIndex;
        storedSocialDescriptionIndex = transitionarySocialDescriptionIndex;

        PersonalityQuirk transitionaryPersonalityQuirk = personalityQuirk;
        personalityQuirk = storedPersonalityQuirk;
        storedPersonalityQuirk = transitionaryPersonalityQuirk;

        int transitionaryPersonalityQuirkDescriptionIndex = personalityQuirkDescriptionIndex;
        personalityQuirkDescriptionIndex = storedPersonalityQuirkDescriptionIndex;
        storedPersonalityQuirkDescriptionIndex = transitionaryPersonalityQuirkDescriptionIndex;

        Reasoning transitionaryReasoning = reasoning;
        reasoning = storedReasoning;
        storedReasoning = transitionaryReasoning;
    }

    /**
     * Processes the effects of childlike regression on the character, applying injuries or health complications based
     * on specified conditions.
     *
     * <p>If the character has childlike regression and fails a willpower check, the method will apply either an
     * injury (using advanced medical rules) or increment the number of "hits" (using basic rules). If the total number
     * of injuries or hits exceeds a defined threshold, the personnel status is changed to indicate medical
     * complications (killed).</p>
     *
     * @param campaign             the {@link Campaign} context in which the effects are processed
     * @param useAdvancedMedical   {@code true} to use advanced medical injury processing
     * @param hasRegression        {@code true} if the character is affected by childlike regression
     * @param failedWillpowerCheck {@code true} if the character failed their willpower check
     */
    public String processChildlikeRegression(Campaign campaign, boolean useAdvancedMedical,
          // These boolean are here to ensure that we only ever pass in valid personnel
          boolean hasRegression, boolean failedWillpowerCheck) {
        final int DEATH_THRESHOLD = 5;

        if (hasRegression && failedWillpowerCheck) {
            if (useAdvancedMedical) {
                Injury injury = CHILDLIKE_REGRESSION.newInjury(campaign, this, INTERNAL, 1);
                addInjury(injury);
            } else {
                hits += 1;
            }

            if ((getInjuries().size() > DEATH_THRESHOLD) || (hits > DEATH_THRESHOLD)) {
                changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.MEDICAL_COMPLICATIONS);
            }

            return String.format(resources.getString("compulsion.regression"), getHyperlinkedFullTitle(),
                  spanOpeningWithCustomColor(getWarningColor()), CLOSING_SPAN_TAG);
        }

        return "";
    }

    /**
     * Processes a potential catatonia episode fthe character, applying relevant effects and status changes.
     *
     * <p>If both {@code hasCatatonia} and {@code failedWillpowerCheck} are {@code true}, this method applies an
     * injury if advanced medical is used, or increments physical trauma otherwise. If the total number of injuries or
     * trauma exceeds a predefined death threshold, the person's status is changed to indicate medical complications. In
     * either case, the method returns a formatted string describing the catatonia episode. If the conditions are not
     * met, it returns an empty string.</p>
     *
     * @param campaign             the current campaign context
     * @param useAdvancedMedical   {@code true} to use advanced medical rules, {@code false} otherwise
     * @param hasCatatonia         {@code true} if the person is suffering from catatonia
     * @param failedWillpowerCheck {@code true} if the person failed their willpower check
     *
     * @return description of the resulting catatonia episode, or an empty string if no episode occurred
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String processCatatonia(Campaign campaign, boolean useAdvancedMedical,
          // These boolean are here to ensure that we only ever pass in valid personnel
          boolean hasCatatonia, boolean failedWillpowerCheck) {
        final int DEATH_THRESHOLD = 5;

        if (hasCatatonia && failedWillpowerCheck) {
            if (useAdvancedMedical) {
                Injury injury = CATATONIA.newInjury(campaign, this, INTERNAL, 1);
                addInjury(injury);
            } else {
                hits += 1;
            }

            if ((getInjuries().size() > DEATH_THRESHOLD) || (hits > DEATH_THRESHOLD)) {
                changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.MEDICAL_COMPLICATIONS);
            }

            return String.format(resources.getString("compulsion.catatonia"), getHyperlinkedFullTitle(),
                  spanOpeningWithCustomColor(getNegativeColor()), CLOSING_SPAN_TAG);
        }

        return "";
    }

    /**
     * Processes the effects of "confusion" for a personnel based on their mental state.
     *
     * <p>If the personnel has both "madness confusion", and has failed a willpower check, applies random damage
     * (injury or hit points depending on the medical system in use), and changes their status to medical complications
     * if the number of injuries or hits exceeds a set threshold.</p>
     *
     * <p>Returns a formatted warning message describing the confusion compulsion, or an empty string if no action
     * was taken.</p>
     *
     * @param campaign             The current campaign instance, used for logging and state updates.
     * @param useAdvancedMedical   Whether the advanced medical system should be used.
     * @param hasMadnessConfusion  Indicates if the personnel is afflicted with madness-induced confusion.
     * @param failedWillpowerCheck Indicates if the required willpower check was failed.
     *
     * @return A formatted string with the confusion compulsion warning, or an empty string if not applicable.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String processConfusion(Campaign campaign, boolean useAdvancedMedical,
          // These boolean are here to ensure that we only ever pass in valid personnel
          boolean hasMadnessConfusion, boolean failedWillpowerCheck) {
        final int DEATH_THRESHOLD = 5;

        if (hasMadnessConfusion && failedWillpowerCheck) {
            if (useAdvancedMedical) {
                InjuryUtil.resolveCombatDamage(campaign, this, 1);
            } else {
                hits++;
            }

            if ((getInjuries().size() > DEATH_THRESHOLD) || (hits > DEATH_THRESHOLD)) {
                changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.MEDICAL_COMPLICATIONS);
            }

            return String.format(resources.getString("compulsion.confusion"), getHyperlinkedFullTitle(),
                  spanOpeningWithCustomColor(getWarningColor()), CLOSING_SPAN_TAG);
        }

        return "";
    }


    /**
     * Processes the effects of a berserker frenzy event for a character, potentially injuring themselves and other
     * victims.
     *
     * <p>If the character has the berserker trait and fails a willpower check, this method determines who is
     * affected by the frenzy (including the character and other victims depending on deployment). Each affected person
     * may receive one or two wounds, applied either as advanced medical injuries or as simple hit increments.</p>
     *
     * <p>If the number of injuries or hits for any victim exceeds a defined threshold, the status for that person
     * is updated to reflect medical complications (for the berserker) or homicide (for other victims). A formatted
     * message describing the frenzy is returned.</p>
     *
     * @param campaign             the campaign context used for looking up personnel, applying wounds, and updating
     *                             statuses
     * @param useAdvancedMedical   if {@code true}, applies wounds using the advanced medical system; otherwise,
     *                             increments hits directly
     * @param hasBerserker         if {@code true}, indicates the character is capable of berserker frenzy
     * @param failedWillpowerCheck if {@code true}, indicates the character failed their willpower check to resist
     *                             frenzy
     *
     * @return a formatted message describing the frenzy if one occurs, or an empty string if there is no effect
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String processBerserkerFrenzy(Campaign campaign, boolean useAdvancedMedical,
          // These boolean are here to ensure that we only ever pass in valid personnel
          boolean hasBerserker, boolean failedWillpowerCheck) {
        final int DEATH_THRESHOLD = 5;

        if (hasBerserker && failedWillpowerCheck) {
            Set<Person> victims = new HashSet<>();
            List<Person> allActivePersonnel = campaign.getActivePersonnel(true, true);
            if (isDeployed() && unit != null) {
                getLocalVictims(allActivePersonnel, victims);
            } else {
                getNonDeployedVictims(allActivePersonnel, victims);
            }

            // The berserker hurts themselves
            victims.add(this);

            for (Person victim : victims) {
                int wounds = randomInt(2) + 1; // (1-2)
                if (useAdvancedMedical) {
                    InjuryUtil.resolveCombatDamage(campaign, victim, wounds);
                } else {
                    int currentHits = victim.getHits();
                    victim.setHits(currentHits + wounds);
                }

                if ((victim.getInjuries().size() > DEATH_THRESHOLD) || (victim.getHits() > DEATH_THRESHOLD)) {
                    victim.changeStatus(campaign, campaign.getLocalDate(), victim.equals(this) ?
                                                                                 PersonnelStatus.MEDICAL_COMPLICATIONS :
                                                                                 PersonnelStatus.HOMICIDE);
                }
            }

            return String.format(resources.getString("compulsion.berserker"), getHyperlinkedFullTitle(),
                  spanOpeningWithCustomColor(getNegativeColor()), CLOSING_SPAN_TAG);
        }

        return "";
    }

    /**
     * Determines whether a personnel member is suffering from clinical paranoia based on their condition and willpower
     * check, and returns a formatted warning message if applicable.
     *
     * <p>If both {@code hasClinicalParanoia} and {@code failedWillpowerCheck} are {@code true}, this method sets the
     * internal state indicating the member is suffering from clinical paranoia and returns a warning message.
     * Otherwise, it resets the state and returns an empty string.</p>
     *
     * @param hasClinicalParanoia  {@code true} if the personnel member has the clinical paranoia condition
     * @param failedWillpowerCheck {@code true} if the personnel member failed their willpower check
     *
     * @return A formatted warning message if clinical paranoia applies, or an empty string otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String processClinicalParanoia(
          // These boolean are here to ensure that we only ever pass in valid personnel
          boolean hasClinicalParanoia, boolean failedWillpowerCheck) {
        if (hasClinicalParanoia && failedWillpowerCheck) {
            sufferingFromClinicalParanoia = true;
            return String.format(resources.getString("compulsion.clinicalParanoia"), getHyperlinkedFullTitle(),
                  spanOpeningWithCustomColor(getWarningColor()), CLOSING_SPAN_TAG);
        }

        sufferingFromClinicalParanoia = false;
        return "";
    }

    /**
     * Processes a hysteria episode for the character.
     *
     * <p>If both {@code hasHysteria} and {@code failedWillpowerCheck} are {@code true}, this method randomly
     * determines (via die roll) which type of episode occurs: berserker frenzy, confusion, or clinical paranoia. The
     * appropriate episode handler is called, and its result returned as a description string. When the episode is not
     * paranoia, any paranoia flag is cleared. Otherwise, if the conditions are not met, returns an empty string.</p>
     *
     * @param campaign             the current campaign context
     * @param useAdvancedMedical   {@code true} to use advanced medical rules, {@code false} otherwise
     * @param hasHysteria          {@code true} if the person is suffering from hysteria
     * @param failedWillpowerCheck {@code true} if the person failed their willpower check
     *
     * @return description of the resulting episode, or an empty string if no episode occurred
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String processHysteria(Campaign campaign, boolean useAdvancedMedical,
          // These boolean are here to ensure that we only ever pass in valid personnel
          boolean hasHysteria, boolean failedWillpowerCheck) {

        if (hasHysteria && failedWillpowerCheck) {
            int roll = d6(1);
            String report = switch (roll) {
                case 1, 2 -> processBerserkerFrenzy(campaign, useAdvancedMedical, true, true);
                case 3, 4 -> processConfusion(campaign, useAdvancedMedical, true, true);
                case 5, 6 -> processClinicalParanoia(true, true);
                default -> throw new IllegalStateException("Unexpected value: " + roll);
            };

            // Reset paranoia
            if (roll < 5) {
                sufferingFromClinicalParanoia = false;
            }

            return report;
        }

        return "";
    }

    /**
     * Selects random victims from the list of all active, non-deployed personnel and adds them to the provided set of
     * victims.
     *
     * <p>The number of victims selected is determined by a single six-sided die roll. For each count, a random
     * non-deployed person is chosen from the available pool and added to the victims set. Once chosen, a victim will
     * not be selected again.</p>
     *
     * @param allActivePersonnel the list of all active personnel, including both deployed and non-deployed
     * @param victims            the set to which randomly selected non-deployed victims will be added
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void getNonDeployedVictims(List<Person> allActivePersonnel, Set<Person> victims) {
        Set<Person> potentialVictims = new HashSet<>();

        for (Person bystander : allActivePersonnel) {
            if (!bystander.isDeployed()) {
                potentialVictims.add(bystander);
            }
        }

        potentialVictims.remove(this);

        int roll = d6(1);
        for (int i = 0; i < roll; ++i) {
            if (potentialVictims.isEmpty()) {
                break;
            }

            Person victim = ObjectUtility.getRandomItem(potentialVictims);
            potentialVictims.remove(victim);
            victims.add(victim);
        }
    }

    /**
     * Selects random victims from deployed personnel who are in the same scenario as the caller and adds them to the
     * provided set of victims.
     *
     * <p>Only personnel currently deployed in the same scenario (as determined by matching scenario IDs) are
     * eligible to be selected. The number of victims chosen is based on a single six-sided die roll. For each count, a
     * random eligible person is added to the victims set; once chosen, a victim will not be selected again.</p>
     *
     * @param allActivePersonnel the list of all active personnel, including both deployed and non-deployed
     * @param victims            the set to which randomly selected victims from the same scenario will be added
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void getLocalVictims(List<Person> allActivePersonnel, Set<Person> victims) {
        Set<Person> potentialVictims = new HashSet<>();

        int scenarioId = unit.getScenarioId();
        for (Person bystander : allActivePersonnel) {
            Unit bystanderUnit = bystander.getUnit();
            if (bystanderUnit != null) {
                if (scenarioId == bystanderUnit.getScenarioId()) {
                    potentialVictims.add(bystander);
                }
            }
        }

        potentialVictims.remove(this);

        int roll = d6(1);
        for (int i = 0; i < roll; ++i) {
            if (potentialVictims.isEmpty()) {
                break;
            }

            Person victim = ObjectUtility.getRandomItem(potentialVictims);
            potentialVictims.remove(victim);
            victims.add(victim);
        }
    }

    /**
     * Determines whether a character's dark secret is revealed based on a die roll, configured modifiers, and campaign
     * options.
     *
     * <p>If the character does not have a dark secret, an empty string is returned. Otherwise, a target number is
     * assembled using base and optional modifiers, and a 2d6 roll is made.</p>
     *
     * <p>If the roll meets or exceeds the target, the dark secret is revealed, relevant state is updated, and a
     * formatted report message is returned (with content and styling based on the severity of the secret).</p>
     *
     * <p>If the secret is not revealed, returns an empty string.</p>
     *
     * @param hasDarkSecret {@code true} if the character has a dark secret. Should be the return value of
     *                      {@link #hasDarkSecret()}
     * @param forceReveal   {@code true} if the reveal should be forced without a die roll.
     *
     * @return a formatted HTML string with the reveal message if the secret is revealed, or an empty string otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String isDarkSecretRevealed(boolean hasDarkSecret, boolean forceReveal) {
        // This boolean is here to ensure that we only ever pass in valid personnel
        if (!hasDarkSecret || darkSecretRevealed) {
            return "";
        } else {
            final int BASE_TARGET_NUMBER = 10;
            final int ALTERNATE_ID_MODIFIER = 2;

            TargetRoll targetRoll = new TargetRoll();
            targetRoll.addModifier(BASE_TARGET_NUMBER, "BASE_TARGET_NUMBER");

            if (options.booleanOption(ATOW_ALTERNATE_ID)) {
                targetRoll.addModifier(ALTERNATE_ID_MODIFIER, "ALTERNATE_ID_MODIFIER");
            }

            int roll = d6(2);
            int targetNumber = targetRoll.getValue();

            LOGGER.info("Dark Secret reveal roll for {}: {} vs. target number: {}", getFullTitle(), roll, targetNumber);

            boolean isDarkSecretRevealed = forceReveal || (roll >= targetNumber);

            String report = "";
            if (isDarkSecretRevealed) {
                LOGGER.info("Dark Secret revealed for {}!", getFullTitle());
                darkSecretRevealed = true;

                String dialogKey = "darkSecret.revealed.";
                String color = getWarningColor();
                if (options.booleanOption(DARK_SECRET_TRIVIAL)) {
                    dialogKey += "trivial";
                } else if (options.booleanOption(DARK_SECRET_SIGNIFICANT)) {
                    dialogKey += "significant";
                } else if (options.booleanOption(DARK_SECRET_MAJOR)) {
                    dialogKey += "major";
                    color = getNegativeColor();
                } else if (options.booleanOption(DARK_SECRET_SEVERE)) {
                    dialogKey += "severe";
                    color = getNegativeColor();
                } else {
                    dialogKey += "extreme";
                    color = getNegativeColor();
                }

                report = String.format(resources.getString(dialogKey), spanOpeningWithCustomColor(color),
                      CLOSING_SPAN_TAG, getHyperlinkedFullTitle());
            }

            return report;
        }
    }

    /**
     * Determines whether any dark secret options are enabled for this entity.
     *
     * @return {@code true} if the entity has any dark secret SPA enabled; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean hasDarkSecret() {
        return options.booleanOption(DARK_SECRET_TRIVIAL)
                     || options.booleanOption(DARK_SECRET_SIGNIFICANT)
                     || options.booleanOption(DARK_SECRET_MAJOR)
                     || options.booleanOption(DARK_SECRET_SEVERE)
                     || options.booleanOption(DARK_SECRET_EXTREME);
    }

    /**
     * Calculates the modifier associated with a character's Dark Secret.
     *
     * <p>If the dark secret is not revealed and the character does not have a dark secret, the modifier is 0.
     * Otherwise, returns a value based on enabled options and the type of modifier requested (reputation or
     * other).</p>
     *
     * @param isReputation {@code true} to retrieve the Reputation modifier; {@code false} to retrieve the Connections
     *                     modifier.
     *
     * @return the appropriate Dark Secret modifier, or 0 if no relevant option is enabled or the secret is not
     *       present/revealed.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public int getDarkSecretModifier(final boolean isReputation) {
        // If the dark secret is not revealed and the character does not have a dark secret, return 0
        if (!darkSecretRevealed && !hasDarkSecret()) {
            return 0;
        }

        // If the character has a dark secret, but it is not revealed, return a default modifier (e.g., -1)
        if (!darkSecretRevealed && hasDarkSecret()) {
            return -1; // Default modifier for unrevealed dark secrets
        }

        // If the dark secret is revealed, calculate the appropriate modifier
        for (Map.Entry<String, int[]> entry : DARK_SECRET_MODIFIERS.entrySet()) {
            if (options.booleanOption(entry.getKey())) {
                return isReputation ? entry.getValue()[0] : entry.getValue()[1];
            }
        }

        return 0;
    }

    /**
     * Reestablishes connections if the cooldown has expired.
     *
     * <p>If burned connections exist and the specified date is after the cooldown period, this method clears the
     * burned connections state and returns a formatted message  indicating that connections have been
     * reestablished.</p>
     *
     * @param today the current date to check against the cooldown period
     *
     * @return a formatted message if connections are reestablished, or an empty string if not
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String checkForConnectionsReestablishContact(LocalDate today) {
        if (burnedConnectionsEndDate != null && burnedConnectionsEndDate.isBefore(today)) {
            burnedConnectionsEndDate = null;

            return String.format(resources.getString("connections.reestablished"), getHyperlinkedFullTitle(),
                  spanOpeningWithCustomColor(getPositiveColor()), CLOSING_SPAN_TAG);
        }
        return "";
    }

    /**
     * Attempts to generate wealth using connections for the given date and campaign finances.
     *
     * <p>Only commanders with active connections are eligible. If successful, a random roll is made to determine if
     * wealth is generated, which is then added to the provided finances. Returns a formatted message if a wealth gain
     * occurs, or an empty string if no wealth is generated.</p>
     *
     * @param today            the current date for the wealth check
     * @param campaignFinances the finances object in which to credit any gained wealth
     *
     * @return a formatted message if wealth is gained, or an empty string if not
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String performConnectionsWealthCheck(LocalDate today, Finances campaignFinances) {
        // Non-commanders can't use their connections to generate wealth
        if (!commander) {
            return "";
        }

        if (burnedConnectionsEndDate != null) {
            LOGGER.info("Connections burned for {} unable to gain Connections wealth", getFullTitle());
            return "";
        }

        int adjustedConnections = getAdjustedConnections();
        ConnectionsLevel connectionsLevel = ConnectionsLevel.parseConnectionsLevelFromInt(adjustedConnections);

        if (!ConnectionsLevel.CONNECTIONS_ZERO.equals(connectionsLevel)) {
            Money donation = connectionsLevel.getWealth();
            if (donation.isPositive()) {
                int roll = d6(2);
                LOGGER.info("Rolling to use connections to gain money {} {} vs. {}", getFullTitle(), roll,
                      CONNECTIONS_TARGET_NUMBER);
                if (roll >= CONNECTIONS_TARGET_NUMBER) {
                    campaignFinances.credit(TransactionType.WEALTH, today, donation,
                          resources.getString("connections.transaction"));
                    return String.format(resources.getString("connections.wealth"), getHyperlinkedFullTitle(),
                          spanOpeningWithCustomColor(getPositiveColor()), CLOSING_SPAN_TAG, donation.toAmountString());
                }
            } else {
                LOGGER.info("Connections too low to generate Wealth");
            }
        }

        return "";
    }

    /**
     * Checks if there is a chance for the connections to be burned on the given date.
     *
     * <p>If the person has non-zero connections, a burn roll is performed. If the roll is equal to or below the burn
     * chance, the connections are burned for a random number of months starting from the given date. Returns a
     * formatted message if connections are burned, or an empty string otherwise.</p>
     *
     * @param today the current date to perform the burn check
     *
     * @return a formatted message if connections are burned, or an empty string if not
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String checkForBurnedContacts(LocalDate today) {
        int adjustedConnections = getAdjustedConnections();
        ConnectionsLevel connectionsLevel = ConnectionsLevel.parseConnectionsLevelFromInt(adjustedConnections);

        if (!ConnectionsLevel.CONNECTIONS_ZERO.equals(connectionsLevel)) {
            int roll = d6(2);
            int burnChance = connectionsLevel.getBurnChance();
            LOGGER.info("Rolling to burn connections for {} {} vs. {}", getFullTitle(), roll, burnChance);
            if (roll <= connectionsLevel.getBurnChance()) {
                burnedConnectionsEndDate = today.plusMonths(d6(1));
                return String.format(resources.getString("connections.burned"), getHyperlinkedFullTitle(),
                      spanOpeningWithCustomColor(getNegativeColor()), CLOSING_SPAN_TAG);
            }
        }
        return "";
    }

    /**
     * Determines whether the character is considered illiterate.
     *
     * <p>A person is regarded as illiterate if they possess the {@link PersonnelOptions#FLAW_ILLITERATE} flaw, and
     * their base level in the {@link SkillType#S_LANGUAGES} skill is below
     * {@link PersonnelOptions#ILLITERACY_LANGUAGES_THRESHOLD}.</p>
     *
     * @return {@code true} if the person is considered illiterate; {@code false} otherwise.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean isIlliterate() {
        if (!options.booleanOption(FLAW_ILLITERATE)) {
            return false;
        }

        Skill languages = skills.getSkill(S_LANGUAGES);
        if (languages == null) {
            return true;
        }

        int level = languages.getLevel();
        return level < ILLITERACY_LANGUAGES_THRESHOLD;
    }

    /**
     * Gets skill modifier data for this person without reputation adjustments.
     *
     * <p>This is a convenience method that returns skill modifier data with:</p>
     * <ul>
     *   <li>Personnel options (character traits and abilities)</li>
     *   <li>Attributes (physical and mental stats)</li>
     *   <li>Active injury effects (considering ambidextrous trait)</li>
     *   <li>Adjusted reputation set to 0 (no reputation modifier)</li>
     *   <li>Illiteracy status</li>
     * </ul>
     *
     * <p>Use {@link #getSkillModifierData(boolean, boolean, LocalDate)} if reputation adjustments based on age,
     * campaign type, and rank are needed.</p>
     *
     * @return a {@link SkillModifierData} object with reputation set to 0
     */
    public SkillModifierData getSkillModifierData() {
        boolean isAmbidextrous = options.booleanOption(PersonnelOptions.ATOW_AMBIDEXTROUS);
        List<InjuryEffect> injuryEffects = AdvancedMedicalAlternate.getAllActiveInjuryEffects(isAmbidextrous,
              injuries);
        return new SkillModifierData(options, atowAttributes, 0, isIlliterate(), injuryEffects);
    }

    /**
     * Gets skill modifier data for this person, including all factors that affect skill checks.
     *
     * <p>This aggregates various character properties into a single data object:</p>
     * <ul>
     *   <li>Personnel options (character traits and abilities)</li>
     *   <li>Attributes (physical and mental stats)</li>
     *   <li>Active injury effects (considering ambidextrous trait)</li>
     *   <li>Adjusted reputation (affected by age, campaign type, and rank)</li>
     *   <li>Illiteracy status</li>
     * </ul>
     *
     * @param isUseAgingEffects whether aging effects should be applied to reputation
     * @param isClanCampaign    whether this is a Clan campaign (affects reputation calculation)
     * @param today             the current campaign date (used for age-based calculations)
     *
     * @return a {@link SkillModifierData} object containing all relevant modifiers
     *
     * @author Illiani
     * @since 0.50.10
     */
    public SkillModifierData getSkillModifierData(boolean isUseAgingEffects, boolean isClanCampaign, LocalDate today) {
        int adjustedReputation = getAdjustedReputation(isUseAgingEffects, isClanCampaign, today, rank);

        boolean isAmbidextrous = options.booleanOption(PersonnelOptions.ATOW_AMBIDEXTROUS);
        List<InjuryEffect> injuryEffects = AdvancedMedicalAlternate.getAllActiveInjuryEffects(isAmbidextrous,
              injuries);

        return new SkillModifierData(options, atowAttributes, adjustedReputation, isIlliterate(), injuryEffects);
    }
}
