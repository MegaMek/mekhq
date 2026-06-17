/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2013-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign;

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.PERSONNEL_MARKET_DISABLED;
import static mekhq.campaign.personnel.PersonUtility.setVeterancyAwardEligibility;
import static mekhq.campaign.personnel.PersonnelOptions.UNOFFICIAL_ILL_DO_IT_MYSELF;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.AdvancedMedicalAlternateImplants.giveEIImplant;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.CanonicalDiseaseType.getAllActiveDiseases;
import static mekhq.campaign.personnel.medical.advancedMedicalAlternate.CanonicalDiseaseType.getAllSystemSpecificDiseasesWithCures;
import static mekhq.campaign.personnel.skills.SkillType.S_ADMIN;
import static mekhq.campaign.personnel.skills.SkillType.S_MEDTECH;
import static mekhq.campaign.personnel.skills.SkillType.S_NEGOTIATION;
import static mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick.isIneligibleToPerformProcurement;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import megamek.Version;
import megamek.client.generator.RandomNameGenerator;
import megamek.codeUtilities.MathUtility;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.icons.Portrait;
import megamek.common.options.IOption;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Aero;
import megamek.common.units.ConvFighter;
import megamek.common.units.Infantry;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign.AdministratorSpecialization;
import mekhq.campaign.campaignOptions.AcquisitionsType;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.events.*;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.events.persons.PersonNewEvent;
import mekhq.campaign.events.persons.PersonRemovedEvent;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.Formation;
import mekhq.campaign.log.MedicalLogger;
import mekhq.campaign.market.PersonnelMarket;
import mekhq.campaign.market.personnelMarket.markets.NewPersonnelMarket;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.InjuryType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.divorce.AbstractDivorce;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.generator.AbstractPersonnelGenerator;
import mekhq.campaign.personnel.generator.AbstractSpecialAbilityGenerator;
import mekhq.campaign.personnel.generator.DefaultPersonnelGenerator;
import mekhq.campaign.personnel.generator.DefaultSpecialAbilityGenerator;
import mekhq.campaign.personnel.generator.RandomPortraitGenerator;
import mekhq.campaign.personnel.marriage.AbstractMarriage;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryTypes;
import mekhq.campaign.personnel.procreation.AbstractProcreation;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.turnoverAndRetention.RetirementDefectionTracker;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.selectors.factionSelectors.AbstractFactionSelector;
import mekhq.campaign.universe.selectors.factionSelectors.DefaultFactionSelector;
import mekhq.campaign.universe.selectors.factionSelectors.RangedFactionSelector;
import mekhq.campaign.universe.selectors.planetSelectors.AbstractPlanetSelector;
import mekhq.campaign.universe.selectors.planetSelectors.DefaultPlanetSelector;
import mekhq.campaign.universe.selectors.planetSelectors.RangedPlanetSelector;
import mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Manages all human-resource concerns for a {@link Campaign}: the personnel roster, support pools (AsTech, Medic, temp
 * crew), personnel markets, genealogy modules, and related operations.
 *
 * <p>No back-reference to Campaign is stored; dependencies are injected per call.</p>
 */
public class HumanResources {
    private static final MMLogger LOGGER = MMLogger.create(HumanResources.class);


    private final Personnel personnel = new Personnel();

    /**
     * Transient cache of active personnel lists, keyed by filter options string. Can be null; rebuilt lazily.
     */
    private transient Map<String, List<Person>> activePersonnelCache = new HashMap<>();

    private int asTechPool;
    private int asTechPoolMinutes;
    private int asTechPoolOvertime;
    private int medicPool;

    /**
     * Map of PersonnelRole to temp crew pool size. Tracks TOTAL pool (not available). Use
     * {@link #getAvailableTempCrewPool(Campaign, PersonnelRole)} for available count.
     */
    private Map<PersonnelRole, Integer> tempPersonnelRoleMap = new HashMap<>();

    private List<Person> personnelWhoAdvancedInXP = new ArrayList<>();
    private RetirementDefectionTracker retirementDefectionTracker;

    private NewPersonnelMarket newPersonnelMarket;

    @Deprecated(since = "0.50.06")
    private PersonnelMarket personnelMarket;

    private transient AbstractDivorce divorce;
    private transient AbstractMarriage marriage;
    private transient AbstractProcreation procreation;


    /**
     * Imports a {@link Person} into the campaign.
     *
     * @param person A {@link Person} to import into the campaign.
     */
    public void importPerson(Person person) {
        personnel.put(person.getId(), person);
        MekHQ.triggerEvent(new PersonNewEvent(person));
    }

    public @Nullable Person getPerson(final UUID id) {
        return personnel.get(id);
    }

    public Collection<Person> getPersonnel() {
        return Collections.unmodifiableCollection(personnel.values());
    }

    /**
     * Retrieves a list of personnel, excluding those whose status indicates they have left the unit.
     *
     * @return a {@code List} of {@link Person} objects who have not left the unit
     */
    public List<Person> getPersonnelFilteringOutDeparted() {
        return getPersonnel().stream()
                     .filter(person -> !person.getStatus().isDepartedUnit())
                     .collect(Collectors.toList());
    }

    /**
     * Retrieves a list of personnel, excluding those whose status indicates they have either left the unit, or are
     * presently away.
     *
     * @return a {@code List} of {@link Person} objects who have not left the unit
     */
    public List<Person> getPersonnelFilteringOutDepartedAndAbsent() {
        return getPersonnel().stream()
                     .filter(person -> !person.getStatus().isDepartedUnit())
                     .filter(person -> !person.getStatus().isAbsent())
                     .collect(Collectors.toList());
    }

    /**
     * Returns a list of personnel who are considered "active" according to various status filters.
     *
     * @param includePrisoners     {@code true} to include prisoners
     * @param includeCampFollowers {@code true} to include non-prisoner camp followers
     *
     * @return a {@link List} of {@link Person} objects matching the criteria
     */
    public List<Person> getActivePersonnel(boolean includePrisoners, boolean includeCampFollowers) {
        String cacheKey = "includePrisoners:" + includePrisoners + "_" + "includeCampFollowers:" + includeCampFollowers;

        if (activePersonnelCache != null &&
                  activePersonnelCache.containsKey(cacheKey) &&
                  !activePersonnelCache.get(cacheKey).isEmpty()) {
            return new ArrayList<>(activePersonnelCache.get(cacheKey));
        }

        List<Person> activePersonnel = new ArrayList<>();

        for (Person person : getPersonnel()) {
            PersonnelStatus status = person.getStatus();
            PrisonerStatus prisonerStatus = person.getPrisonerStatus();
            boolean isActive = status.isActiveFlexible();
            boolean isCampFollower = prisonerStatus.isFreeOrBondsman() && status.isCampFollower();
            boolean isActivePrisoner = person.getPrisonerStatus().isCurrentPrisoner() && isActive;

            if (!isActive) {
                continue;
            }

            if (!includeCampFollowers && isCampFollower) {
                continue;
            }

            if (!includePrisoners && isActivePrisoner) {
                continue;
            }

            activePersonnel.add(person);
        }

        if (activePersonnelCache == null) {
            activePersonnelCache = new HashMap<>();
        }
        activePersonnelCache.put(cacheKey, new ArrayList<>(activePersonnel));
        return activePersonnel;
    }

    /**
     * Clears the {@code activePersonnelCache} so it's recalculated next time we getActivePersonnel.
     */
    public void invalidateActivePersonnelCache() {
        activePersonnelCache.clear();
    }

    /**
     * @return a list of people who are currently eligible to receive a salary.
     */
    public static List<Person> getSalaryEligiblePersonnel(Collection<Person> people) {
        return people.stream()
                     .filter(person -> person.getStatus().isSalaryEligible())
                     .collect(Collectors.toList());
    }

    public List<Person> getSalaryEligiblePersonnel() {
        return getSalaryEligiblePersonnel(getActivePersonnel(false, false));
    }

    /**
     * Retrieves a filtered list of personnel who have at least one combat profession.
     *
     * @deprecated
     */
    @Deprecated(since = "0.51.0", forRemoval = true)
    public static List<Person> getActiveCombatPersonnel(Collection<Person> people) {
        return people.stream()
                     .filter(p -> p.getPrimaryRole().isCombat() || p.getSecondaryRole().isCombat())
                     .collect(Collectors.toList());
    }

    /**
     * Provides a filtered list of personnel including only active Dependents (including camp followers).
     *
     * @return a {@link Person} {@code List} containing all active dependents
     */
    public static List<Person> getActiveDependents(Collection<Person> people) {
        return people.stream()
                     .filter(person -> person.getPrimaryRole().isDependent())
                     .filter(person -> person.getStatus().isActiveFlexible())
                     .collect(Collectors.toList());
    }

    public List<Person> getActiveDependents() {
        return getActiveDependents(personnel.values());
    }

    /**
     * Provides a filtered list of personnel including only active prisoners.
     *
     * @return a {@link Person} {@code List} containing all active prisoners
     */
    public static List<Person> getCurrentPrisoners(Collection<Person> people) {
        return people.stream()
                     .filter(person -> person.getPrisonerStatus().isCurrentPrisoner())
                     .collect(Collectors.toList());
    }

    public List<Person> getCurrentPrisoners() {
        return getCurrentPrisoners(getActivePersonnel(true, false));
    }

    /**
     * Provides a filtered list of personnel including only active prisoners who are willing to defect.
     *
     * @return a {@link Person} {@code List} containing prisoner defectors
     */
    public List<Person> getPrisonerDefectors() {
        return getActivePersonnel(true, false).stream()
                     .filter(person -> person.getPrisonerStatus().isPrisonerDefector())
                     .collect(Collectors.toList());
    }

    /**
     * Provides a filtered list of personnel including only friendly PoWs.
     *
     * @return a {@link Person} {@code List} containing friendly prisoners
     */
    public List<Person> getFriendlyPrisoners() {
        return getPersonnel().stream().filter(p -> p.getStatus().isPoW()).collect(Collectors.toList());
    }

    /**
     * Provides a filtered list of personnel including only Persons with the Student status.
     *
     * @return a {@link Person} {@code List} containing students
     */
    public List<Person> getStudents() {
        return getPersonnel().stream().filter(p -> p.getStatus().isStudent()).collect(Collectors.toList());
    }

    public static List<Person> getDoctors(Collection<Person> people) {
        return people.stream().filter(Person::isDoctor).collect(Collectors.toList());
    }

    public List<Person> getDoctors() {
        return getDoctors(getActivePersonnel(false, false));
    }

    public static List<Person> getPatients(Collection<Person> people) {
        return people.stream().filter(Person::needsFixing).collect(Collectors.toList());
    }

    public List<Person> getPatients() {
        return getPatients(getActivePersonnel(true, true));
    }

    public List<Person> getPatientsAssignedToDoctors() {
        return getPatients()
                     .stream()
                     .filter(patient -> patient.getDoctorId() != null)
                     .toList();
    }

    public List<Person> getPatientsWithNonPermanentInjuries() {
        return getPatients()
                     .stream()
                     .filter(patient -> !patient.getNonPermanentInjuries().isEmpty() || patient.getHits() > 0)
                     .toList();
    }

    public int getPatientsFor(Person doctor) {
        int patients = 0;
        UUID doctorId = doctor.getId();
        for (Person person : getActivePersonnel(true, true)) {
            if ((null != person.getDoctorId()) && person.getDoctorId().equals(doctorId)) {
                patients++;
            }
        }
        return patients;
    }

    public void removeAllPatientsFor(Person doctor, CampaignOptions campaignOptions) {
        for (Person person : getPersonnel()) {
            if (null != person.getDoctorId() && person.getDoctorId().equals(doctor.getId())) {
                person.setDoctorId(null, campaignOptions.getNaturalHealingWaitingPeriod());
            }
        }
    }

    public static List<Person> getAdmins(Collection<Person> people) {
        return people.stream().filter(Person::isAdministrator).collect(Collectors.toList());
    }

    public List<Person> getAdmins() {
        return getAdmins(getActivePersonnel(false, false));
    }


    public void resetAsTechMinutes(CampaignOptions campaignOptions) {
        asTechPoolMinutes = Person.PRIMARY_ROLE_SUPPORT_TIME * getNumberAsTechs(campaignOptions);
        asTechPoolOvertime = Person.PRIMARY_ROLE_OVERTIME_SUPPORT_TIME * getNumberAsTechs(campaignOptions);
    }

    public void setAsTechPoolMinutes(int minutes) {
        asTechPoolMinutes = minutes;
    }

    public int getAsTechPoolMinutes() {
        return asTechPoolMinutes;
    }

    public void setAsTechPoolOvertime(int overtime) {
        asTechPoolOvertime = overtime;
    }

    public int getAsTechPoolOvertime() {
        return asTechPoolOvertime;
    }

    public int getPossibleAsTechPoolMinutes(CampaignOptions campaignOptions) {
        return 480 * getNumberPrimaryAsTechs(campaignOptions) + 240 * getNumberSecondaryAsTechs(campaignOptions);
    }

    public int getPossibleAsTechPoolOvertime(CampaignOptions campaignOptions) {
        return 240 * getNumberPrimaryAsTechs(campaignOptions) + 120 * getNumberSecondaryAsTechs(campaignOptions);
    }

    public void setAsTechPool(int size) {
        asTechPool = size;
    }

    public int getTemporaryAsTechPool() {
        return asTechPool;
    }

    public boolean requiresAdditionalAsTechs(CampaignOptions campaignOptions) {
        return getAsTechNeed(campaignOptions) > 0;
    }

    public int getAsTechNeed(CampaignOptions campaignOptions) {
        List<Person> techs = getActivePersonnel(false, false).stream()
                                   .filter(Person::isTech)
                                   .toList();
        int techCount = techs.size();

        for (Person person : techs) {
            if (person.getOptions().booleanOption(UNOFFICIAL_ILL_DO_IT_MYSELF)) {
                techCount--;
            }
        }

        return (techCount * MHQConstants.AS_TECH_TEAM_SIZE) - getNumberAsTechs(campaignOptions);
    }

    public void increaseAsTechPool(Campaign campaign, int i) {
        asTechPool += i;
        asTechPoolMinutes += (480 * i);
        asTechPoolOvertime += (240 * i);
        MekHQ.triggerEvent(new AsTechPoolChangedEvent(campaign, i));
    }

    public void decreaseAsTechPool(Campaign campaign, int i) {
        asTechPool = max(0, asTechPool - i);
        asTechPoolMinutes = max(0, asTechPoolMinutes - 480 * i);
        asTechPoolOvertime = max(0, asTechPoolOvertime - 240 * i);
        MekHQ.triggerEvent(new AsTechPoolChangedEvent(campaign, -i));
    }

    public void resetAsTechPool(Campaign campaign) {
        emptyAsTechPool(campaign);
        fillAsTechPool(campaign);
    }

    public void emptyAsTechPool(Campaign campaign) {
        final int currentAsTechs = getTemporaryAsTechPool();
        decreaseAsTechPool(campaign, currentAsTechs);
    }

    public void fillAsTechPool(Campaign campaign) {
        final int need = getAsTechNeed(campaign.getCampaignOptions());
        if (need > 0) {
            increaseAsTechPool(campaign, need);
        }
    }

    /**
     * Calculates the total number of primary AsTechs available in the campaign.
     *
     * @param campaignOptions the campaign options (may be null if not using useful AsTechs)
     *
     * @return the total number of primary AsTechs
     */
    public int getNumberPrimaryAsTechs(CampaignOptions campaignOptions) {
        boolean isUseUsefulAsTechs = campaignOptions != null && campaignOptions.isUseUsefulAsTechs();

        int asTechs = getTemporaryAsTechPool();

        for (Person person : getActivePersonnel(false, false)) {
            if (person.getPrimaryRole().isAstech() && !person.isDeployed() && person.isEmployed()) {
                asTechs++;
                asTechs += isUseUsefulAsTechs ? person.getAdvancedAsTechContribution() : 0;
            }
        }

        return asTechs;
    }

    /**
     * Calculates the total number of secondary AsTechs available in the campaign.
     *
     * @param campaignOptions the campaign options (may be null if not using useful AsTechs)
     *
     * @return the total number of secondary AsTechs
     */
    public int getNumberSecondaryAsTechs(CampaignOptions campaignOptions) {
        boolean isUseUsefulAsTechs = campaignOptions != null && campaignOptions.isUseUsefulAsTechs();

        int asTechs = 0;

        for (Person person : getActivePersonnel(false, false)) {
            if (person.getSecondaryRole().isAstech() && !person.isDeployed() && person.isEmployed()) {
                asTechs++;
                asTechs += isUseUsefulAsTechs ? person.getAdvancedAsTechContribution() : 0;
            }
        }

        return asTechs;
    }

    public int getAvailableAsTechs(final int minutes, final boolean alreadyOvertime, boolean isOvertimeAllowed,
          CampaignOptions campaignOptions) {
        if (minutes == 0) {
            return 0;
        }

        int availableHelp = (int) floor(((double) asTechPoolMinutes) / minutes);
        if (isOvertimeAllowed && (availableHelp < MHQConstants.AS_TECH_TEAM_SIZE)) {
            final int shortMod = getShorthandedMod(availableHelp, false);
            final int remainingMinutes = asTechPoolMinutes - availableHelp * minutes;
            final int extraHelp = (remainingMinutes + asTechPoolOvertime) / minutes;
            final int helpNeeded = MHQConstants.AS_TECH_TEAM_SIZE - availableHelp;
            if (alreadyOvertime && (shortMod > 0)) {
                availableHelp += extraHelp;
            } else if (shortMod > 3) {
                if (extraHelp >= helpNeeded) {
                    availableHelp = MHQConstants.AS_TECH_TEAM_SIZE;
                }
            }
        }
        return Math.min(Math.min(availableHelp, MHQConstants.AS_TECH_TEAM_SIZE), getNumberAsTechs(campaignOptions));
    }

    public int getShorthandedMod(int availableHelp, boolean medicalStaff) {
        if (medicalStaff) {
            availableHelp += 2;
        }
        int helpMod = 0;
        if (availableHelp == 0) {
            helpMod = 4;
        } else if (availableHelp == 1) {
            helpMod = 3;
        } else if (availableHelp < 4) {
            helpMod = 2;
        } else if (availableHelp < 6) {
            helpMod = 1;
        }
        return helpMod;
    }


    public void setMedicPool(int size) {
        medicPool = size;
    }

    public int getTemporaryMedicPool() {
        return medicPool;
    }

    public boolean requiresAdditionalMedics() {
        return getMedicsNeed() > 0;
    }

    public int getMedicsNeed() {
        List<Person> doctors = getActivePersonnel(false, false).stream()
                                     .filter(Person::isDoctor)
                                     .toList();
        int doctorCount = doctors.size();

        for (Person person : doctors) {
            boolean hasDoItMyself = person.getOptions().booleanOption(UNOFFICIAL_ILL_DO_IT_MYSELF);
            if (hasDoItMyself) {
                doctorCount--;
            }
        }

        return (doctorCount * MHQConstants.MEDIC_TEAM_SIZE) - getNumberMedics();
    }

    public int getNumberMedics() {
        return getTemporaryMedicPool() + getPermanentMedicPool(null);
    }

    /**
     * Calculates the total number of medics available.
     *
     * @param campaignOptions the campaign options (may be null)
     *
     * @return the permanent medic pool count
     */
    public int getPermanentMedicPool(CampaignOptions campaignOptions) {
        final boolean isUseUsefulMedics = campaignOptions != null && campaignOptions.isUseUsefulMedics();
        int permanentMedicPool = 0;

        for (Person person : getActivePersonnel(false, false)) {
            if (person.getPrimaryRole().isMedic() || person.getSecondaryRole().isMedic()) {
                if (person.isDeployed()) {
                    continue;
                }
                if (!person.isEmployed()) {
                    continue;
                }

                if (!isUseUsefulMedics) {
                    permanentMedicPool++;
                } else {
                    Skill medicSkill = person.getSkill(S_MEDTECH);
                    if (medicSkill != null) {
                        SkillModifierData skillModifierData = person.getSkillModifierData();
                        int skillLevel = medicSkill.getTotalSkillLevel(skillModifierData);

                        permanentMedicPool++;
                        permanentMedicPool += (int) floor(skillLevel / Campaign.ASSISTANT_SKILL_LEVEL_DIVIDER);
                    }
                }
            }
        }

        return permanentMedicPool;
    }

    public void increaseMedicPool(Campaign campaign, int i) {
        medicPool += i;
        MekHQ.triggerEvent(new MedicPoolChangedEvent(campaign, i));
    }

    public void decreaseMedicPool(Campaign campaign, int i) {
        medicPool = max(0, medicPool - i);
        MekHQ.triggerEvent(new MedicPoolChangedEvent(campaign, -i));
    }

    public void resetMedicPool(Campaign campaign) {
        emptyMedicPool(campaign);
        fillMedicPool(campaign);
    }

    public void emptyMedicPool(Campaign campaign) {
        final int currentMedicPool = getTemporaryMedicPool();
        decreaseMedicPool(campaign, currentMedicPool);
    }

    public void fillMedicPool(Campaign campaign) {
        final int need = getMedicsNeed();
        if (need > 0) {
            increaseMedicPool(campaign, need);
        }
    }


    /**
     * Gets the total temp crew pool size for a specific personnel role.
     *
     * @param role the personnel role
     *
     * @return the total number of temp crew in the pool for this role
     */
    public int getTempCrewPool(PersonnelRole role) {
        return tempPersonnelRoleMap.getOrDefault(role, 0);
    }

    public Set<PersonnelRole> getTempCrewRoleKeys() {
        return tempPersonnelRoleMap.keySet();
    }

    /**
     * Sets the total temp crew pool size for a specific personnel role.
     *
     * @param campaign the campaign (for event firing)
     * @param role     the personnel role
     * @param size     the total number of temp crew in the pool
     */
    public void setTempCrewPool(Campaign campaign, PersonnelRole role, int size) {
        int oldSize = tempPersonnelRoleMap.getOrDefault(role, 0);
        if (size <= 0) {
            tempPersonnelRoleMap.remove(role);
        } else {
            tempPersonnelRoleMap.put(role, size);
        }

        if (size != oldSize) {
            fireTempCrewPoolChangedEvent(campaign, role, size - oldSize);
            fireTempCrewPoolChangedEvent(campaign, role, size - oldSize);
        }
    }

    /**
     * Checks if a specific blob crew type is enabled in campaign options.
     *
     * @param role            the personnel role to check
     * @param campaignOptions the campaign options
     *
     * @return true if this blob crew type is enabled
     */
    public boolean isBlobCrewEnabled(PersonnelRole role, CampaignOptions campaignOptions) {
        return switch (role) {
            case SOLDIER -> campaignOptions.isUseBlobInfantry();
            case BATTLE_ARMOUR -> campaignOptions.isUseBlobBattleArmor();
            case VEHICLE_CREW_GROUND -> campaignOptions.isUseBlobVehicleCrewGround();
            case VEHICLE_CREW_VTOL -> campaignOptions.isUseBlobVehicleCrewVTOL();
            case VEHICLE_CREW_NAVAL -> campaignOptions.isUseBlobVehicleCrewNaval();
            case VESSEL_PILOT -> campaignOptions.isUseBlobVesselPilot();
            case VESSEL_GUNNER -> campaignOptions.isUseBlobVesselGunner();
            case VESSEL_CREW -> campaignOptions.isUseBlobVesselCrew();
            default -> false;
        };
    }

    /**
     * Gets the number of temp crew currently in use by units for a specific role.
     *
     * @param campaign the campaign (for unit access)
     * @param role     the personnel role
     *
     * @return the number of temp crew in use
     */
    public int getTempCrewInUse(Campaign campaign, PersonnelRole role) {
        return campaign.getUnits().stream()
                     .mapToInt(unit -> unit.getTempCrewByPersonnelRole(role))
                     .sum();
    }

    /**
     * Gets the number of temp crew available for assignment for a specific role.
     *
     * @param campaign the campaign
     * @param role     the personnel role
     *
     * @return total pool minus crew currently in use
     */
    public int getAvailableTempCrewPool(Campaign campaign, PersonnelRole role) {
        int pool = getTempCrewPool(role);
        int inUse = getTempCrewInUse(campaign, role);
        return Math.max(0, pool - inUse);
    }

    /**
     * Fires the appropriate pool changed event for a specific personnel role.
     *
     * @param campaign the campaign
     * @param role     the personnel role
     * @param change   the change amount (positive for increase, negative for decrease)
     */
    public void fireTempCrewPoolChangedEvent(Campaign campaign, PersonnelRole role, int change) {
        switch (role) {
            case SOLDIER -> MekHQ.triggerEvent(new SoldierPoolChangedEvent(campaign, change));
            case BATTLE_ARMOUR -> MekHQ.triggerEvent(new BattleArmorPoolChangedEvent(campaign, change));
            case VEHICLE_CREW_GROUND -> MekHQ.triggerEvent(new VehicleCrewGroundPoolChangedEvent(campaign, change));
            case VEHICLE_CREW_VTOL -> MekHQ.triggerEvent(new VehicleCrewVTOLPoolChangedEvent(campaign, change));
            case VEHICLE_CREW_NAVAL -> MekHQ.triggerEvent(new VehicleCrewNavalPoolChangedEvent(campaign, change));
            case VESSEL_PILOT -> MekHQ.triggerEvent(new VesselPilotPoolChangedEvent(campaign, change));
            case VESSEL_GUNNER -> MekHQ.triggerEvent(new VesselGunnerPoolChangedEvent(campaign, change));
            case VESSEL_CREW -> MekHQ.triggerEvent(new VesselCrewPoolChangedEvent(campaign, change));
            default -> throw new IllegalStateException("Unexpected value: " + role);
        }
    }

    /**
     * Empties the temp crew pool for a specific role by setting it to the number of active temp crew for that role.
     *
     * @param campaign the campaign
     * @param role     the personnel role to reduce to the minimum
     */
    public void emptyTempCrewPoolForRole(Campaign campaign, PersonnelRole role) {
        setTempCrewPool(campaign, role, getTempCrewInUse(campaign, role));
    }

    /**
     * Fills the temp crew pool for a specific role by calculating crew needs across all units. Only runs if the
     * corresponding blob crew option is enabled.
     *
     * @param campaign        the campaign
     * @param campaignOptions the campaign options
     * @param role            the personnel role to fill
     */
    public void fillTempCrewPoolForRole(Campaign campaign, CampaignOptions campaignOptions, PersonnelRole role) {
        if (!isBlobCrewEnabled(role, campaignOptions)) {
            return;
        }

        int need = 0;
        for (Unit unit : campaign.getUnits()) {
            if (unitCanUseTempCrewRole(unit, role)) {
                int roleSpecificNeed = getRoleSpecificNeeds(unit, role);
                if (roleSpecificNeed > 0) {
                    need += roleSpecificNeed;
                }
            }
        }

        if (need > 0) {
            increaseTempCrewPool(campaign, role, need);
        }
    }

    /**
     * Resets the temp crew pool for a specific role by emptying and then filling it.
     *
     * @param campaign        the campaign
     * @param campaignOptions the campaign options
     * @param role            the personnel role to reset
     */
    public void resetTempCrewPoolForRole(Campaign campaign, CampaignOptions campaignOptions, PersonnelRole role) {
        emptyTempCrewPoolForRole(campaign, role);
        fillTempCrewPoolForRole(campaign, campaignOptions, role);
    }

    /**
     * Increases the temp crew pool for a specific personnel role.
     *
     * @param campaign the campaign
     * @param role     the personnel role
     * @param amount   the amount to increase by
     */
    public void increaseTempCrewPool(Campaign campaign, PersonnelRole role, int amount) {
        setTempCrewPool(campaign, role, getTempCrewPool(role) + amount);
    }

    /**
     * Decreases the temp crew pool for a specific personnel role.
     *
     * @param campaign the campaign
     * @param role     the personnel role
     * @param amount   the amount to decrease by
     */
    public void decreaseTempCrewPool(Campaign campaign, PersonnelRole role, int amount) {
        setTempCrewPool(campaign, role, Math.max(0, getTempCrewPool(role) - amount));
    }

    private boolean unitCanUseTempCrewRole(Unit unit, PersonnelRole role) {
        if (unit.getCommander() == null || unit.getEntity() == null) {
            return false;
        }

        return switch (role) {
            case SOLDIER,
                 BATTLE_ARMOUR,
                 VEHICLE_CREW_GROUND,
                 VEHICLE_CREW_VTOL,
                 VEHICLE_CREW_NAVAL,
                 VESSEL_PILOT -> unit.getDriverRole() == role;
            case VESSEL_GUNNER -> unit.getGunnerRole() == role;
            case VESSEL_CREW -> (unit.getEntity() instanceof Aero aero && !(aero instanceof ConvFighter))
                                      && unit.canTakeMoreVesselCrew();
            default -> false;
        };
    }

    /**
     * Returns the number of temp crew slots still available for the given role on the given unit. A negative value
     * means the role is over-allocated. For vessel roles (pilot, gunner, crew), the calculation uses role-specific slot
     * counts so that each role's budget is tracked independently. For all other roles the unit's total crew size is
     * used (correct for single-role units such as infantry).
     *
     * @param unit the unit
     * @param role the personnel role
     *
     * @return available slots (negative = surplus temp crew)
     */
    private int getRoleSpecificNeeds(Unit unit, PersonnelRole role) {
        return switch (role) {
            case VESSEL_PILOT -> unit.getTotalDriverNeeds()
                                       - unit.getDrivers().size()
                                       - unit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_PILOT);
            case VESSEL_GUNNER -> unit.getTotalGunnerNeeds()
                                        - unit.getGunners().size()
                                        - unit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_GUNNER);
            case VESSEL_CREW -> unit.getTotalCrewNeeds()
                                      - unit.getVesselCrew().size()
                                      - unit.getTempCrewByPersonnelRole(PersonnelRole.VESSEL_CREW);
            default -> unit.getFullCrewSize()
                             - unit.getActiveCrew().size()
                             - unit.getTempCrewByPersonnelRole(role);
        };
    }

    /**
     * Distributes temp crew from the pool to units that need crew for a specific personnel role.
     *
     * @param campaign        the campaign
     * @param campaignOptions the campaign options
     * @param role            the personnel role to distribute
     */
    public void distributeTempCrewPoolToUnits(Campaign campaign, CampaignOptions campaignOptions, PersonnelRole role) {
        if (!isBlobCrewEnabled(role, campaignOptions)) {
            return;
        }

        int availablePool = getAvailableTempCrewPool(campaign, role);
        for (Unit unit : campaign.getUnits()) {
            if (availablePool <= 0) {
                break;
            }

            if (unitCanUseTempCrewRole(unit, role)) {
                int needed = getRoleSpecificNeeds(unit, role);

                if (needed > 0) {
                    int toAssign = Math.min(needed, availablePool);
                    unit.setTempCrew(role, unit.getTempCrewByPersonnelRole(role) + toAssign);
                    availablePool -= toAssign;
                }
            }
        }
    }

    /**
     * Clears blob crew for a specific personnel role from units and empties the campaign pool.
     *
     * @param campaign the campaign
     * @param role     the personnel role to clear
     */
    public void clearBlobCrewForRole(Campaign campaign, PersonnelRole role) {
        for (Unit unit : campaign.getUnits()) {
            if (unit.getTempCrewByPersonnelRole(role) > 0) {
                unit.setTempCrew(role, 0);
            }
        }

        if (getTempCrewPool(role) > 0) {
            setTempCrewPool(campaign, role, 0);
        }
    }

    /**
     * Releases surplus AsTechs from the pool, keeping only what is currently needed. If the pool is at or below the
     * required amount, no change is made.
     *
     * @param campaign the campaign
     */
    public void releaseSurplusAsTechPool(Campaign campaign) {
        int surplus = Math.max(0, -getAsTechNeed(campaign.getCampaignOptions()));
        if (surplus > 0) {
            decreaseAsTechPool(campaign, surplus);
        }
    }

    /**
     * Releases surplus Medics from the pool, keeping only what is currently needed. If the pool is at or below the
     * required amount, no change is made.
     *
     * @param campaign the campaign
     */
    public void releaseSurplusMedicPool(Campaign campaign) {
        int surplus = Math.max(0, -getMedicsNeed());
        if (surplus > 0) {
            decreaseMedicPool(campaign, surplus);
        }
    }

    /**
     * Releases surplus temp crew for a specific blob crew role.
     *
     * <p>For each unit, any assigned temp crew beyond what the unit needs (i.e., where real crew
     * already fills or exceeds {@code fullCrewSize}) is removed. The unassigned pool is then emptied.</p>
     *
     * @param campaign the campaign
     * @param role     the personnel role to trim
     */
    public void releaseSurplusBlobCrewForRole(Campaign campaign, PersonnelRole role) {
        for (Unit unit : campaign.getUnits()) {
            int currentTemp = unit.getTempCrewByPersonnelRole(role);
            if (currentTemp > 0) {
                int excess = Math.max(0, -getRoleSpecificNeeds(unit, role));
                if (excess > 0) {
                    unit.setTempCrew(role, currentTemp - excess);
                }
            }
        }
        emptyTempCrewPoolForRole(campaign, role);
    }


    @Deprecated(since = "0.50.06")
    public PersonnelMarket getPersonnelMarket() {
        return personnelMarket;
    }

    @Deprecated(since = "0.50.06")
    public void setPersonnelMarket(final PersonnelMarket personnelMarket) {
        this.personnelMarket = personnelMarket;
    }

    public NewPersonnelMarket getNewPersonnelMarket() {
        return newPersonnelMarket;
    }

    public void setNewPersonnelMarket(final NewPersonnelMarket newPersonnelMarket) {
        this.newPersonnelMarket = newPersonnelMarket;
    }

    /**
     * Returns {@code true} when campaign options select the legacy (deprecated) personnel market.
     *
     * <p>The market style named {@code PERSONNEL_MARKET_DISABLED} disables the <em>new</em> market
     * and causes the legacy market to run instead — the name is counterintuitive, so this predicate makes the intent
     * explicit at every call site.</p>
     */
    public static boolean isUsingLegacyPersonnelMarket(CampaignOptions options) {
        return options.getPersonnelMarketStyle() == PERSONNEL_MARKET_DISABLED;
    }

    /**
     * Refreshes the applicants based on the current market style and the current date.
     *
     * @param campaign               the campaign
     * @param bypassDateRestrictions {@code true} if we want the applicants to refresh at an unusual time, such as
     *                               campaign start
     */
    public void refreshApplicants(Campaign campaign, boolean bypassDateRestrictions) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        LocalDate currentDay = campaign.getLocalDate();

        if (isUsingLegacyPersonnelMarket(campaignOptions)) {
            if (personnelMarket != null) {
                personnelMarket.generatePersonnelForDay(campaign);
            }
        } else {
            if (currentDay.getDayOfMonth() == 1 || bypassDateRestrictions) {
                newPersonnelMarket.gatherApplications();
            }
        }
    }


    public AbstractDivorce getDivorce() {
        return divorce;
    }

    public void setDivorce(final AbstractDivorce divorce) {
        this.divorce = divorce;
    }

    public AbstractMarriage getMarriage() {
        return marriage;
    }

    public void setMarriage(final AbstractMarriage marriage) {
        this.marriage = marriage;
    }

    public AbstractProcreation getProcreation() {
        return procreation;
    }

    public void setProcreation(final AbstractProcreation procreation) {
        this.procreation = procreation;
    }


    /**
     * Sets the list of personnel who have advanced in experience points via vocational XP.
     *
     * @param personnelWhoAdvancedInXP a {@link List} of {@link Person} objects
     */
    public void setPersonnelWhoAdvancedInXP(List<Person> personnelWhoAdvancedInXP) {
        this.personnelWhoAdvancedInXP = personnelWhoAdvancedInXP;
    }

    /**
     * Retrieves the list of personnel who have advanced in experience points via vocational XP.
     *
     * @return a {@link List} of {@link Person} objects
     */
    public List<Person> getPersonnelWhoAdvancedInXP() {
        return personnelWhoAdvancedInXP;
    }


    public void setRetirementDefectionTracker(RetirementDefectionTracker rdt) {
        retirementDefectionTracker = rdt;
    }

    public RetirementDefectionTracker getRetirementDefectionTracker() {
        return retirementDefectionTracker;
    }


    /**
     * Gets the {@link AbstractFactionSelector} to use with this campaign.
     *
     * @param campaignOptions the campaign options
     *
     * @return an {@link AbstractFactionSelector}
     */
    public AbstractFactionSelector getFactionSelector(CampaignOptions campaignOptions) {
        return getFactionSelector(campaignOptions.getRandomOriginOptions());
    }

    /**
     * Gets the {@link AbstractFactionSelector} to use.
     *
     * @param options the random origin options to use
     *
     * @return an {@link AbstractFactionSelector}
     */
    public AbstractFactionSelector getFactionSelector(final RandomOriginOptions options) {
        return options.isRandomizeOrigin() ? new RangedFactionSelector(options) : new DefaultFactionSelector(options);
    }

    /**
     * Gets the {@link AbstractPlanetSelector} to use with this campaign.
     *
     * @param campaignOptions the campaign options
     *
     * @return an {@link AbstractPlanetSelector}
     */
    public AbstractPlanetSelector getPlanetSelector(CampaignOptions campaignOptions) {
        return getPlanetSelector(campaignOptions.getRandomOriginOptions());
    }

    /**
     * Gets the {@link AbstractPlanetSelector} to use.
     *
     * @param options the random origin options to use
     *
     * @return an {@link AbstractPlanetSelector}
     */
    public AbstractPlanetSelector getPlanetSelector(final RandomOriginOptions options) {
        return options.isRandomizeOrigin() ? new RangedPlanetSelector(options) : new DefaultPlanetSelector(options);
    }

    /**
     * Gets the {@link AbstractPersonnelGenerator} to use with this campaign.
     *
     * @param factionSelector the faction selector
     * @param planetSelector  the planet selector
     *
     * @return an {@link AbstractPersonnelGenerator}
     */
    public AbstractPersonnelGenerator getPersonnelGenerator(final AbstractFactionSelector factionSelector,
          final AbstractPlanetSelector planetSelector) {
        final DefaultPersonnelGenerator generator = new DefaultPersonnelGenerator(factionSelector, planetSelector);
        generator.setNameGenerator(RandomNameGenerator.getInstance());
        generator.setSkillPreferences(new RandomSkillPreferences());
        return generator;
    }

    /**
     * @deprecated Use {@link #getPersonnelGenerator(AbstractFactionSelector, AbstractPlanetSelector)} instead. The
     *       {@code campaignOptions} parameter was never used.
     */
    @Deprecated(since = "0.50.07")
    public AbstractPersonnelGenerator getPersonnelGenerator(CampaignOptions campaignOptions,
          final AbstractFactionSelector factionSelector,
          final AbstractPlanetSelector planetSelector) {
        return getPersonnelGenerator(factionSelector, planetSelector);
    }

    /**
     * Assigns a random portrait to a {@link Person}.
     *
     * @param campaignOptions the campaign options
     * @param person          the person who should receive a randomized portrait
     */
    public void assignRandomPortraitFor(CampaignOptions campaignOptions, final Person person) {
        final boolean allowDuplicatePortraits = campaignOptions.isAllowDuplicatePortraits();
        final boolean genderedPortraitsOnly = campaignOptions.isUseGenderedPortraitsOnly();
        final Portrait portrait = RandomPortraitGenerator.generate(getPersonnel(),
              person,
              allowDuplicatePortraits,
              genderedPortraitsOnly);
        if (!portrait.isDefault()) {
            person.setPortrait(portrait);
        }
    }

    /**
     * Assigns a random origin to a {@link Person}.
     *
     * @param campaign        the campaign
     * @param campaignOptions the campaign options
     * @param person          the person who should receive a randomized origin
     */
    public void assignRandomOriginFor(Campaign campaign, CampaignOptions campaignOptions, final Person person) {
        final Faction faction = getFactionSelector(campaignOptions).selectFaction(campaign);
        if (faction != null) {
            person.setOriginFaction(faction);
        }

        final Planet planet = getPlanetSelector(campaignOptions).selectPlanet(campaign, faction);
        if (planet != null) {
            person.setOriginPlanet(planet);
        }
    }


    /**
     * Returns the highest-ranking administrator of the given specialization from {@code people}.
     *
     * @param people          the collection of people to search
     * @param type            the administrator specialization to match
     * @param campaignOptions the campaign options
     * @param isClanCampaign  whether this is a Clan campaign
     * @param today           the current in-game date
     *
     * @return the senior administrator, or {@code null} if none found
     */
    public static @Nullable Person getSeniorAdminPerson(Collection<Person> people,
          AdministratorSpecialization type, CampaignOptions campaignOptions,
          boolean isClanCampaign, LocalDate today) {
        Person seniorAdmin = null;

        for (Person person : people) {
            boolean isEligible = switch (type) {
                case COMMAND -> person.getPrimaryRole().isAdministratorCommand() ||
                                      person.getSecondaryRole().isAdministratorCommand();
                case LOGISTICS -> person.getPrimaryRole().isAdministratorLogistics() ||
                                        person.getSecondaryRole().isAdministratorLogistics();
                case TRANSPORT -> person.getPrimaryRole().isAdministratorTransport() ||
                                        person.getSecondaryRole().isAdministratorTransport();
                case HR -> person.getPrimaryRole().isAdministratorHR() || person.getSecondaryRole().isAdministratorHR();
            };

            if (isEligible) {
                if (seniorAdmin == null) {
                    seniorAdmin = person;
                    continue;
                }

                if (person.outRanksUsingSkillTiebreaker(campaignOptions, isClanCampaign, today, seniorAdmin)) {
                    seniorAdmin = person;
                }
            }
        }
        return seniorAdmin;
    }

    public @Nullable Person getSeniorAdminPerson(AdministratorSpecialization type,
          CampaignOptions campaignOptions, boolean isClanCampaign, LocalDate today) {
        return getSeniorAdminPerson(getAdmins(), type, campaignOptions, isClanCampaign, today);
    }

    /**
     * Returns the highest-ranking doctor from {@code people}.
     *
     * @param people          the collection of people to search
     * @param campaignOptions the campaign options
     * @param isClanCampaign  whether this is a Clan campaign
     * @param today           the current in-game date
     *
     * @return the senior medical person, or {@code null} if none found
     */
    public static @Nullable Person getSeniorMedicalPerson(Collection<Person> people,
          CampaignOptions campaignOptions, boolean isClanCampaign, LocalDate today) {
        Person senior = null;

        for (Person person : people) {
            if (senior == null) {
                senior = person;
                continue;
            }

            if (person.outRanksUsingSkillTiebreaker(campaignOptions, isClanCampaign, today, senior)) {
                senior = person;
            }
        }

        return senior;
    }

    public @Nullable Person getSeniorMedicalPerson(CampaignOptions campaignOptions, boolean isClanCampaign,
          LocalDate today) {
        return getSeniorMedicalPerson(getDoctors(), campaignOptions, isClanCampaign, today);
    }

    /**
     * Retrieves the current campaign commander.
     *
     * @param campaignOptions the campaign options
     * @param isClanCampaign  whether this is a Clan campaign
     * @param today           the current in-game date
     *
     * @return the {@link Person} who is the commander, or {@code null}
     */
    public @Nullable Person getCommander(CampaignOptions campaignOptions, boolean isClanCampaign, LocalDate today) {
        return findTopCommanders(campaignOptions, isClanCampaign, today)[0];
    }

    /**
     * Retrieves the second-in-command.
     *
     * @param campaignOptions the campaign options
     * @param isClanCampaign  whether this is a Clan campaign
     * @param today           the current in-game date
     *
     * @return the {@link Person} who is second-in-command, or {@code null}
     */
    public @Nullable Person getSecondInCommand(CampaignOptions campaignOptions, boolean isClanCampaign,
          LocalDate today) {
        return findTopCommanders(campaignOptions, isClanCampaign, today)[1];
    }

    /**
     * Finds the current top two candidates for command among active personnel.
     *
     * @param people          the collection of people to search
     * @param campaignOptions the campaign options
     * @param isClanCampaign  whether this is a Clan campaign
     * @param today           the current in-game date
     *
     * @return array where index 0 is commander, index 1 is second-in-command
     */
    public static Person[] findTopCommanders(Collection<Person> people,
          CampaignOptions campaignOptions, boolean isClanCampaign, LocalDate today) {
        Person flaggedCommander = people.stream().filter(Person::isCommander).findFirst().orElse(null);
        Person commander = flaggedCommander;

        Person flaggedSecondInCommand = people.stream().filter(Person::isSecondInCommand).findFirst().orElse(null);
        Person secondInCommand = flaggedSecondInCommand;

        if (flaggedCommander != null && flaggedSecondInCommand != null) {
            return new Person[] { commander, secondInCommand };
        }

        for (Person person : people) {
            if (person == null) {
                continue;
            }

            if (person.equals(flaggedCommander) || person.equals(flaggedSecondInCommand)) {
                continue;
            }

            if (flaggedCommander == null) {
                if (commander == null) {
                    commander = person;
                    continue;
                }

                if (!person.equals(commander) &&
                          person.outRanksUsingSkillTiebreaker(campaignOptions, isClanCampaign, today, commander)) {
                    Person previousCommander = commander;
                    commander = person;

                    if (flaggedSecondInCommand == null && !previousCommander.equals(commander)) {
                        if (secondInCommand == null) {
                            secondInCommand = previousCommander;
                        } else if (!previousCommander.equals(secondInCommand)
                                         &&
                                         previousCommander.outRanksUsingSkillTiebreaker(campaignOptions,
                                               isClanCampaign,
                                               today,
                                               secondInCommand)) {
                            secondInCommand = previousCommander;
                        }
                    }
                    continue;
                }
            }

            if (flaggedSecondInCommand == null) {
                if (person.equals(commander)) {
                    continue;
                }

                if (secondInCommand == null) {
                    secondInCommand = person;
                    continue;
                }

                if (!person.equals(secondInCommand) &&
                          person.outRanksUsingSkillTiebreaker(campaignOptions,
                                isClanCampaign,
                                today,
                                secondInCommand)) {
                    secondInCommand = person;
                }
            }
        }

        if (commander != null && commander.equals(secondInCommand)) {
            secondInCommand = null;
        }

        return new Person[] { commander, secondInCommand };
    }

    public Person[] findTopCommanders(CampaignOptions campaignOptions, boolean isClanCampaign, LocalDate today) {
        return findTopCommanders(getActivePersonnel(false, false), campaignOptions, isClanCampaign, today);
    }


    public List<Person> getTechs(Collection<Unit> units, CampaignOptions campaignOptions,
          boolean isClanCampaign, LocalDate today) {
        return getTechs(units, campaignOptions, isClanCampaign, today, false);
    }

    public List<Person> getTechs(Collection<Unit> units, CampaignOptions campaignOptions,
          boolean isClanCampaign, LocalDate today, boolean noZeroMinute) {
        return getTechs(units, campaignOptions, isClanCampaign, today, noZeroMinute, false);
    }

    public List<Person> getTechsExpanded(Collection<Unit> units, CampaignOptions campaignOptions,
          boolean isClanCampaign, LocalDate today) {
        return getTechsExpanded(units, campaignOptions, isClanCampaign, today, false, false, true);
    }

    public List<Person> getTechs(Collection<Unit> units, CampaignOptions campaignOptions,
          boolean isClanCampaign, LocalDate today, boolean noZeroMinute, boolean eliteFirst) {
        return getTechsExpanded(units, campaignOptions, isClanCampaign, today, noZeroMinute, eliteFirst, false);
    }

    /**
     * Retrieves a list of active technicians.
     *
     * @param people          the collection of people to search
     * @param units           the collection of units (for self-crewed engineers)
     * @param campaignOptions the campaign options
     * @param isClanCampaign  whether this is a Clan campaign
     * @param today           the current in-game date
     * @param noZeroMinute    if {@code true}, excludes technicians with no remaining available minutes
     * @param eliteFirst      if {@code true}, sorts the list to place the most skilled technicians at the top
     * @param expanded        if {@code true}, includes technicians with expanded roles
     *
     * @return a list of active technicians sorted appropriately
     */
    public static List<Person> getTechsExpanded(Collection<Person> people, Collection<Unit> units,
          CampaignOptions campaignOptions, boolean isClanCampaign, LocalDate today,
          boolean noZeroMinute, boolean eliteFirst, boolean expanded) {
        final List<Person> techs = people.stream()
                                         .filter(person -> (expanded ? person.isTechExpanded() : person.isTech()) &&
                                                                 (!noZeroMinute || (person.getMinutesLeft() > 0)))
                                         .collect(Collectors.toList());

        for (final Unit unit : units) {
            if (unit.isSelfCrewed() && !(unit.getEntity() instanceof Infantry) &&
                      (unit.getEngineer() != null)) {
                // As we're directly fetching the engineer, we need to make sure that we're not fetching twice;
                // otherwise the engineer may appear multiple times in the array
                if (!techs.contains(unit.getEngineer())) {
                    techs.add(unit.getEngineer());
                }
            }
        }

        techs.sort(Comparator.comparingInt(person -> person.getSkillLevel(campaignOptions, isClanCampaign, today,
              !person.getPrimaryRole().isTech() && person.getSecondaryRole().isTechSecondary(), true).ordinal()));

        if (eliteFirst) {
            Collections.reverse(techs);
        }

        techs.sort(Comparator.comparingInt(person -> -person.getDailyAvailableTechTime(
              campaignOptions.isTechsUseAdministration())));

        techs.sort((person1, person2) -> {
            if (person1.outRanks(person2)) {
                return 1;
            } else if (person2.outRanks(person1)) {
                return -1;
            } else {
                return 0;
            }
        });

        return techs;
    }

    public List<Person> getTechsExpanded(Collection<Unit> units, CampaignOptions campaignOptions,
          boolean isClanCampaign, LocalDate today, boolean noZeroMinute, boolean eliteFirst, boolean expanded) {
        return getTechsExpanded(getActivePersonnel(false, false), units, campaignOptions, isClanCampaign, today,
              noZeroMinute, eliteFirst, expanded);
    }

    public boolean isWorkingOnRefit(Hangar hangar, Person person) {
        Objects.requireNonNull(person);
        Unit unit = hangar.findUnit(u -> u.isRefitting() && person.equals(u.getRefit().getTech()));
        return unit != null;
    }


    /**
     * Returns the best procurement character from {@code people} given the campaign's acquisition settings.
     *
     * @param people          the collection of people to search
     * @param campaignOptions the campaign options
     * @param isClanCampaign  whether this is a Clan campaign
     * @param today           the current in-game date
     *
     * @return the best logistics person, or {@code null} if none found or acquisition is automatic
     */
    public static @Nullable Person getLogisticsPerson(Collection<Person> people,
          CampaignOptions campaignOptions, boolean isClanCampaign, LocalDate today) {
        final AcquisitionsType acquisitionsType = campaignOptions.getAcquisitionType();
        String fixedSkillName = "";
        boolean isAnyTech = false;

        switch (acquisitionsType) {
            case ADMINISTRATION -> fixedSkillName = S_ADMIN;
            case ANY_TECH -> isAnyTech = true;
            case AUTOMATIC -> {
                return null;
            }
            case NEGOTIATION -> fixedSkillName = S_NEGOTIATION;
        }

        final ProcurementPersonnelPick acquisitionCategory = campaignOptions.getAcquisitionPersonnelCategory();
        final int defaultMaxAcquisitions = campaignOptions.getMaxAcquisitions();

        boolean isUseAgingEffects = campaignOptions.isUseAgeEffects();

        int bestSkill = Integer.MIN_VALUE;
        Person procurementCharacter = null;
        for (Person person : people) {
            if (isIneligibleToPerformProcurement(person, acquisitionCategory)) {
                continue;
            }

            if (defaultMaxAcquisitions > 0 && (person.getAcquisitions() >= defaultMaxAcquisitions)) {
                continue;
            }

            int totalSkillLevel = logisticsSkillLevel(person, isAnyTech, fixedSkillName, isUseAgingEffects,
                  isClanCampaign, today);

            if (totalSkillLevel > bestSkill) {
                procurementCharacter = person;
                bestSkill = totalSkillLevel;
            }
        }

        return procurementCharacter;
    }

    public @Nullable Person getLogisticsPerson(CampaignOptions campaignOptions, boolean isClanCampaign,
          LocalDate today) {
        return getLogisticsPerson(getActivePersonnel(false, false), campaignOptions, isClanCampaign, today);
    }

    private static @Nullable Skill pickLogisticsSkill(Person person, boolean isAnyTech, String fixedSkillName) {
        return isAnyTech ? person.getBestTechSkill() : person.getSkill(fixedSkillName);
    }

    private static int logisticsSkillLevel(Person person, boolean isAnyTech, String fixedSkillName,
          boolean isUseAgingEffects, boolean isClanCampaign, LocalDate today) {
        Skill skill = pickLogisticsSkill(person, isAnyTech, fixedSkillName);
        if (skill == null) {
            return Integer.MIN_VALUE;
        }
        return skill.getTotalSkillLevel(person.getSkillModifierData(isUseAgingEffects, isClanCampaign, today));
    }

    /**
     * Returns all eligible procurement characters from {@code people} sorted by skill, best last.
     *
     * @param people          the collection of people to search
     * @param campaignOptions the campaign options
     * @param isClanCampaign  whether this is a Clan campaign
     * @param today           the current in-game date
     *
     * @return list of eligible logistics personnel, sorted ascending by skill level
     */
    public static List<Person> getLogisticsPersonnel(Collection<Person> people,
          CampaignOptions campaignOptions, boolean isClanCampaign, LocalDate today) {
        final AcquisitionsType acquisitionsType = campaignOptions.getAcquisitionType();

        String fixedSkillName = "";
        boolean isAnyTech = false;

        switch (acquisitionsType) {
            case ADMINISTRATION -> fixedSkillName = S_ADMIN;
            case ANY_TECH -> isAnyTech = true;
            case AUTOMATIC -> {
                return Collections.emptyList();
            }
            case NEGOTIATION -> fixedSkillName = S_NEGOTIATION;
        }

        final int maxAcquisitions = campaignOptions.getMaxAcquisitions();
        final ProcurementPersonnelPick acquisitionCategory = campaignOptions.getAcquisitionPersonnelCategory();
        List<Person> logisticsPersonnel = new ArrayList<>();

        for (Person person : people) {
            if (isIneligibleToPerformProcurement(person, acquisitionCategory)) {
                continue;
            }

            if ((maxAcquisitions > 0) && (person.getAcquisitions() >= maxAcquisitions)) {
                continue;
            }
            if (isAnyTech) {
                if (null != person.getBestTechSkill()) {
                    logisticsPersonnel.add(person);
                }
            } else if (person.hasSkill(fixedSkillName)) {
                logisticsPersonnel.add(person);
            }
        }

        boolean isUseAgingEffects = campaignOptions.isUseAgeEffects();
        final boolean sortByAnyTech = isAnyTech;
        final String sortBySkillName = fixedSkillName;
        logisticsPersonnel.sort((person1, person2) -> {
            int level1 = logisticsSkillLevel(person1, sortByAnyTech, sortBySkillName, isUseAgingEffects,
                  isClanCampaign, today);
            int level2 = logisticsSkillLevel(person2, sortByAnyTech, sortBySkillName, isUseAgingEffects,
                  isClanCampaign, today);
            return Integer.compare(level1, level2);
        });

        return logisticsPersonnel;
    }

    public List<Person> getLogisticsPersonnel(CampaignOptions campaignOptions, boolean isClanCampaign,
          LocalDate today) {
        return getLogisticsPersonnel(getActivePersonnel(false, false), campaignOptions, isClanCampaign, today);
    }

    public int getNumberAsTechs(CampaignOptions campaignOptions) {
        return getNumberPrimaryAsTechs(campaignOptions) + getNumberSecondaryAsTechs(campaignOptions);
    }


    public Person newDependent(Campaign campaign, Gender gender) {
        return newDependent(campaign, gender, null, null);
    }

    /**
     * Creates a new dependent with the given gender, origin faction, and origin planet.
     *
     * @param campaign      the campaign
     * @param gender        the {@link Gender} of the new dependent
     * @param originFaction the {@link Faction} that represents the origin faction, or null
     * @param originPlanet  the {@link Planet} that represents the origin planet, or null
     *
     * @return a new {@link Person} representing the dependent
     */
    public Person newDependent(Campaign campaign, Gender gender, @Nullable Faction originFaction,
          @Nullable Planet originPlanet) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        PersonnelRole civilianProfession = PersonnelRole.MISCELLANEOUS_JOB;

        int dependentProfessionDieSize = campaignOptions.getDependentProfessionDieSize();
        if (dependentProfessionDieSize == 0 || randomInt(dependentProfessionDieSize) == 0) {
            civilianProfession = PersonnelRole.DEPENDENT;
        }

        int civilianProfessionDieSize = campaignOptions.getCivilianProfessionDieSize();
        if (civilianProfessionDieSize > 0) {
            if (randomInt(civilianProfessionDieSize) == 0) {
                List<PersonnelRole> civilianRoles = PersonnelRole.getCivilianRolesExceptNone();
                civilianProfession = ObjectUtility.getRandomItem(civilianRoles);
            }
        }

        return newPerson(campaign,
              civilianProfession,
              PersonnelRole.NONE,
              new DefaultFactionSelector(campaignOptions.getRandomOriginOptions(), originFaction),
              new DefaultPlanetSelector(campaignOptions.getRandomOriginOptions(), originPlanet),
              gender);
    }

    public Person newPerson(Campaign campaign, final PersonnelRole role) {
        return newPerson(campaign, role, PersonnelRole.NONE);
    }

    public Person newPerson(Campaign campaign, final PersonnelRole primaryRole, final PersonnelRole secondaryRole) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        return newPerson(campaign,
              primaryRole,
              secondaryRole,
              getFactionSelector(campaignOptions),
              getPlanetSelector(campaignOptions),
              Gender.RANDOMIZE);
    }

    public Person newPerson(Campaign campaign, final PersonnelRole primaryRole, final String factionCode,
          final Gender gender) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        return newPerson(campaign,
              primaryRole,
              PersonnelRole.NONE,
              new DefaultFactionSelector(campaignOptions.getRandomOriginOptions(),
                    (factionCode == null) ? null : Factions.getInstance().getFaction(factionCode)),
              getPlanetSelector(campaignOptions),
              gender);
    }

    public Person newPerson(Campaign campaign, final PersonnelRole primaryRole, final PersonnelRole secondaryRole,
          final AbstractFactionSelector factionSelector, final AbstractPlanetSelector planetSelector,
          final Gender gender) {
        return newPerson(campaign,
              primaryRole,
              secondaryRole,
              getPersonnelGenerator(factionSelector, planetSelector),
              gender);
    }

    public Person newPerson(Campaign campaign, final PersonnelRole primaryRole,
          final AbstractPersonnelGenerator personnelGenerator) {
        return newPerson(campaign, primaryRole, PersonnelRole.NONE, personnelGenerator, Gender.RANDOMIZE);
    }

    /**
     * Generate a new {@link Person} of the given role, using the supplied {@link AbstractPersonnelGenerator}.
     *
     * @param campaign           the campaign
     * @param primaryRole        the primary role
     * @param secondaryRole      the secondary role
     * @param personnelGenerator the generator to use
     * @param gender             the gender of the person to generate, or a randomize value
     *
     * @return a new {@link Person}
     */
    public Person newPerson(Campaign campaign, final PersonnelRole primaryRole, final PersonnelRole secondaryRole,
          final AbstractPersonnelGenerator personnelGenerator, final Gender gender) {
        final Person person = personnelGenerator.generate(campaign, primaryRole, secondaryRole, gender);

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        LocalDate currentDay = campaign.getLocalDate();

        // Assign a random portrait after we generate a new person
        if (campaignOptions.isUsePortraitForRole(primaryRole)) {
            if (!campaignOptions.isNoRandomPortraitsForChildren() || !person.isChild(currentDay, false)) {
                assignRandomPortraitFor(campaignOptions, person);
            }
        }

        if (campaignOptions.isUseImplants() && campaignOptions.isUseAlternativeAdvancedMedical()) {
            if (primaryRole.isProtoMekPilot() || secondaryRole.isProtoMekPilot()) {
                giveEIImplant(campaign, person);
            } else if (primaryRole.isMekWarrior() && person.isClanPersonnel()) {
                boolean isOver40 = person.getAge(currentDay) >= 40;
                boolean isOver30 = person.getAge(currentDay) >= 30;

                int implantChance = 100;
                if (isOver40) {
                    implantChance = 50;
                } else if (isOver30) {
                    implantChance = 75;
                }

                if (randomInt(implantChance) == 0) {
                    giveEIImplant(campaign, person);
                }
            }
        }

        setVeterancyAwardEligibility(campaign, person);

        return person;
    }

    /**
     * If the person does not already have a bloodname, assigns a chance of having one based on skill and rank.
     *
     * @param campaign   the campaign
     * @param person     the bloodname candidate
     * @param ignoreDice if true, skips the random roll and assigns a bloodname automatically
     */
    public void checkBloodnameAdd(Campaign campaign, Person person, boolean ignoreDice) {
        if (!person.isClanPersonnel() || person.getPhenotype().isNone()) {
            return;
        }

        if (!person.getBloodname().isEmpty()) {
            if (!ignoreDice) {
                return;
            }
            // ignoreDice == true means the caller has already confirmed replacement
        }

        SkillModifierData skillModifierData = person.getSkillModifierData();

        int bloodnameTarget = 6;
        if (!ignoreDice) {
            switch (person.getPhenotype()) {
                case MEKWARRIOR: {
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_MEK) ?
                                             person.getSkill(SkillType.S_GUN_MEK)
                                             .getFinalSkillValue(skillModifierData) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    bloodnameTarget += person.hasSkill(SkillType.S_PILOT_MEK) ?
                                             person.getSkill(SkillType.S_PILOT_MEK)
                                             .getFinalSkillValue(skillModifierData) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    break;
                }
                case AEROSPACE: {
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_AERO) ?
                                             person.getSkill(SkillType.S_GUN_AERO)
                                             .getFinalSkillValue(skillModifierData) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    bloodnameTarget += person.hasSkill(SkillType.S_PILOT_AERO) ?
                                             person.getSkill(SkillType.S_PILOT_AERO)
                                             .getFinalSkillValue(skillModifierData) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    break;
                }
                case ELEMENTAL: {
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_BA) ?
                                             person.getSkill(SkillType.S_GUN_BA)
                                             .getFinalSkillValue(skillModifierData) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    bloodnameTarget += person.hasSkill(SkillType.S_ANTI_MEK) ?
                                             person.getSkill(SkillType.S_ANTI_MEK)
                                             .getFinalSkillValue(skillModifierData) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    break;
                }
                case VEHICLE: {
                    bloodnameTarget += person.hasSkill(SkillType.S_GUN_VEE) ?
                                             person.getSkill(SkillType.S_GUN_VEE)
                                             .getFinalSkillValue(skillModifierData) :
                                             TargetRoll.AUTOMATIC_FAIL;
                    switch (person.getPrimaryRole()) {
                        case VEHICLE_CREW_GROUND:
                            bloodnameTarget += person.hasSkill(SkillType.S_PILOT_GVEE) ?
                                                     person.getSkill(SkillType.S_PILOT_GVEE)
                                                     .getFinalSkillValue(skillModifierData) :
                                                     TargetRoll.AUTOMATIC_FAIL;
                            break;
                        case VEHICLE_CREW_NAVAL:
                            bloodnameTarget += person.hasSkill(SkillType.S_PILOT_NVEE) ?
                                                     person.getSkill(SkillType.S_PILOT_NVEE)
                                                     .getFinalSkillValue(skillModifierData) :
                                                     TargetRoll.AUTOMATIC_FAIL;
                            break;
                        case VEHICLE_CREW_VTOL:
                            bloodnameTarget += person.hasSkill(SkillType.S_PILOT_VTOL) ?
                                                     person.getSkill(SkillType.S_PILOT_VTOL)
                                                     .getFinalSkillValue(skillModifierData) :
                                                     TargetRoll.AUTOMATIC_FAIL;
                            break;
                        default:
                            break;
                    }
                    break;
                }
                case PROTOMEK: {
                    bloodnameTarget += 2 *
                                             (person.hasSkill(SkillType.S_GUN_PROTO) ?
                                                    person.getSkill(SkillType.S_GUN_PROTO)
                                                    .getFinalSkillValue(skillModifierData) :
                                                    TargetRoll.AUTOMATIC_FAIL);
                    break;
                }
                case NAVAL: {
                    switch (person.getPrimaryRole()) {
                        case VESSEL_PILOT:
                            bloodnameTarget += 2 *
                                                     (person.hasSkill(SkillType.S_PILOT_SPACE) ?
                                                            person.getSkill(SkillType.S_PILOT_SPACE)
                                                            .getFinalSkillValue(skillModifierData) :
                                                            TargetRoll.AUTOMATIC_FAIL);
                            break;
                        case VESSEL_GUNNER:
                            bloodnameTarget += 2 *
                                                     (person.hasSkill(SkillType.S_GUN_SPACE) ?
                                                            person.getSkill(SkillType.S_GUN_SPACE)
                                                            .getFinalSkillValue(skillModifierData) :
                                                            TargetRoll.AUTOMATIC_FAIL);
                            break;
                        case VESSEL_CREW:
                            bloodnameTarget += 2 *
                                                     (person.hasSkill(SkillType.S_TECH_VESSEL) ?
                                                            person.getSkill(SkillType.S_TECH_VESSEL)
                                                            .getFinalSkillValue(skillModifierData) :
                                                            TargetRoll.AUTOMATIC_FAIL);
                            break;
                        case VESSEL_NAVIGATOR:
                            bloodnameTarget += 2 *
                                                     (person.hasSkill(SkillType.S_NAVIGATION) ?
                                                            person.getSkill(SkillType.S_NAVIGATION)
                                                            .getFinalSkillValue(skillModifierData) :
                                                            TargetRoll.AUTOMATIC_FAIL);
                            break;
                        default:
                            break;
                    }
                    break;
                }
                default: {
                    break;
                }
            }
            bloodnameTarget += DragoonRating.DRAGOON_C.getRating() - campaign.getAtBUnitRatingMod();

            int year = campaign.getGameYear();
            if (year <= 2950) {
                bloodnameTarget--;
            }

            if (year > 3055) {
                bloodnameTarget++;
            }

            if (year > 3065) {
                bloodnameTarget++;
            }

            if (year > 3080) {
                bloodnameTarget++;
            }

            bloodnameTarget += Math.min(0, campaign.getRankSystem().getOfficerCut() - person.getRankNumeric());
        }

        if (ignoreDice || (d6(2) >= bloodnameTarget)) {
            final Phenotype phenotype = person.getPhenotype().isNone() ? Phenotype.GENERAL : person.getPhenotype();

            final Bloodname bloodname = Bloodname.randomBloodname((campaign.getFaction().isClan() ?
                                                                         campaign.getFaction() :
                                                                         person.getOriginFaction()).getShortName(),
                  phenotype,
                  campaign.getGameYear());
            if (bloodname != null) {
                person.setBloodname(bloodname.getName());
                personUpdated(campaign, person);
            }
        }
    }


    public boolean recruitPerson(Campaign campaign, Person person) {
        return recruitPerson(campaign, person, person.getPrisonerStatus(), false, true, true, false);
    }

    public boolean recruitPerson(Campaign campaign, Person person, boolean gmAdd, boolean employ) {
        return recruitPerson(campaign, person, person.getPrisonerStatus(), gmAdd, true, employ, false);
    }

    public boolean recruitPerson(Campaign campaign, Person person, PrisonerStatus prisonerStatus, boolean employ) {
        return recruitPerson(campaign, person, prisonerStatus, false, true, employ, false);
    }

    public boolean recruitPerson(Campaign campaign, Person person, PrisonerStatus prisonerStatus, boolean gmAdd,
          boolean log, boolean employ) {
        return recruitPerson(campaign, person, prisonerStatus, gmAdd, log, employ, false);
    }

    /**
     * Recruits a person into the campaign roster.
     *
     * @param campaign                    the campaign
     * @param person                      the person to recruit; must not be {@code null}
     * @param prisonerStatus              the prison status to assign to the person
     * @param gmAdd                       if {@code true}, bypasses funds check
     * @param log                         if {@code true}, recruitment is logged
     * @param employ                      if {@code true}, the person is marked as employed
     * @param bypassSimulateRelationships if {@code true}, relationship simulation does not occur
     *
     * @return {@code true} if recruitment was successful; {@code false} otherwise
     */
    public boolean recruitPerson(Campaign campaign, Person person, PrisonerStatus prisonerStatus, boolean gmAdd,
          boolean log, boolean employ, boolean bypassSimulateRelationships) {
        if (person == null) {
            LOGGER.warn("A null person was passed into recruitPerson.");
            return false;
        }

        ResourceBundle resources = campaign.getResources();
        LocalDate currentDay = campaign.getLocalDate();
        Finances finances = campaign.getFinances();

        if (employ && !person.isEmployed()) {
            if (campaign.getCampaignOptions().isPayForRecruitment() && !gmAdd) {
                if (!finances.debit(TransactionType.RECRUITMENT,
                      currentDay,
                      person.getSalary(campaign).multipliedBy(2),
                      String.format(resources.getString("personnelRecruitmentFinancesReason.text"),
                            person.getFullName()))) {
                    campaign.addReport(DailyReportType.FINANCES,
                          String.format(resources.getString("personnelRecruitmentInsufficientFunds.text"),
                                ReportingUtilities.getNegativeColor(),
                                person.getFullName()));
                    return false;
                }
            }
        }

        String formerSurname = person.getSurname();

        if (!personnel.containsKey(person.getId())) {
            person.setJoinedCampaign(currentDay);
            personnel.put(person.getId(), person);
            person.setParent(campaign.getMainForcePersonnel());

            if (!bypassSimulateRelationships && campaign.getCampaignOptions().isUseSimulatedRelationships()) {
                if ((prisonerStatus.isFree()) &&
                          (!person.getOriginFaction().isClan()) &&
                          (!person.getPrimaryRole().isCivilian())) {
                    simulateRelationshipHistory(campaign, person);
                }
            }
        }

        if (employ) {
            if (person.isAstech()) {
                asTechPoolMinutes += Person.PRIMARY_ROLE_SUPPORT_TIME;
                asTechPoolOvertime += Person.PRIMARY_ROLE_OVERTIME_SUPPORT_TIME;
            }
        } else {
            person.setStatus(PersonnelStatus.CAMP_FOLLOWER);
        }

        person.setPrisonerStatus(campaign, prisonerStatus, log);

        if (log) {
            formerSurname = person.getSurname().equals(formerSurname) ?
                                  "" :
                                  ' ' +
                                        String.format(resources.getString("personnelRecruitmentFormerSurname.text") +
                                                      ' ', formerSurname);
            String add = !prisonerStatus.isFree() ?
                               (' ' +
                                      resources.getString(prisonerStatus.isBondsman() ?
                                                          "personnelRecruitmentBondsman.text" :
                                                          "personnelRecruitmentPrisoner.text")) :
                               "";
            campaign.addReport(DailyReportType.PERSONNEL,
                  String.format(resources.getString("personnelRecruitmentAddedToRoster.text"),
                        person.getHyperlinkedFullTitle(),
                        formerSurname,
                        add));
        }

        AbstractLocation location = campaign.getCurrentLocation();
        if (location.isOnPlanet()) {
            Planet planet = location.getPlanet();
            String planetId = planet.getId();
            String systemId = planet.getParentSystem().getId();

            if (!person.hasPlanetaryInoculation(planetId)) {
                person.addPlanetaryInoculation(planetId);
                MedicalLogger.inoculation(person, currentDay, planet.getName(currentDay));
            }

            Set<InjuryType> activeCures = getAllSystemSpecificDiseasesWithCures(systemId, currentDay, true);
            for (InjuryType injuryType : activeCures) {
                if (!person.hasCanonDiseaseInoculation(injuryType.getKey())) {
                    person.addCanonDiseaseInoculation(injuryType.getKey());
                    MedicalLogger.specificInoculation(person, currentDay, injuryType.getSimpleName());
                }
            }
        }

        Planet planet = person.getOriginPlanet();
        if (planet != location.getPlanet()) {
            String planetName = planet.getName(currentDay);
            String planetId = planet.getId();
            String systemId = planet.getParentSystem().getId();

            if (!person.hasPlanetaryInoculation(planetId)) {
                person.addPlanetaryInoculation(planetId);
                MedicalLogger.antibodies(person, currentDay, planetName);
            }

            Set<InjuryType> activeDiseases = getAllActiveDiseases(systemId, currentDay, true);
            for (InjuryType injuryType : activeDiseases) {
                if (!person.hasCanonDiseaseInoculation(injuryType.getKey())) {
                    person.addCanonDiseaseInoculation(injuryType.getKey());
                    MedicalLogger.specificAntibodies(person, currentDay, injuryType.getSimpleName());
                }
            }

            Set<InjuryType> activeCures = getAllSystemSpecificDiseasesWithCures(systemId, currentDay, true);
            for (InjuryType injuryType : activeCures) {
                if (!person.hasCanonDiseaseInoculation(injuryType.getKey())) {
                    person.addCanonDiseaseInoculation(injuryType.getKey());
                    MedicalLogger.specificAntibodies(person, currentDay, injuryType.getSimpleName());
                }
            }
        }

        MekHQ.triggerEvent(new PersonNewEvent(person));
        return true;
    }

    /**
     * Employs the given camp follower and integrates them into the campaign.
     *
     * @param campaign the campaign
     * @param person   the {@code Person} being employed; may be {@code null}
     */
    public void employCampFollower(Campaign campaign, Person person) {
        if (person == null) {
            LOGGER.warn("A null person was passed into employCampFollower.");
            return;
        }

        LocalDate currentDay = campaign.getLocalDate();
        person.changeStatus(campaign, currentDay, PersonnelStatus.ACTIVE);
        person.setRecruitment(currentDay);

        if (person.isAstech()) {
            asTechPoolMinutes += Person.PRIMARY_ROLE_SUPPORT_TIME;
            asTechPoolOvertime += Person.PRIMARY_ROLE_OVERTIME_SUPPORT_TIME;
        }

        MekHQ.triggerEvent(new PersonNewEvent(person));
    }

    private void simulateRelationshipHistory(Campaign campaign, Person person) {
        LocalDate localDate = campaign.getLocalDate();
        long weeksBetween = ChronoUnit.WEEKS.between(person.getDateOfBirth().plusYears(18), localDate);

        if (weeksBetween == 0) {
            return;
        }

        Person babysFather = null;
        Person spousesBabysFather = null;
        List<Person> currentChildren = new ArrayList<>();
        List<Person> priorChildren = new ArrayList<>();

        Person currentSpouse = null;
        List<Person> allSpouses = new ArrayList<>();

        for (long weeksRemaining = weeksBetween; weeksRemaining >= 0; weeksRemaining--) {
            LocalDate currentDate = localDate.minusWeeks(weeksRemaining);

            if (currentSpouse != null) {
                divorce.processNewWeek(campaign, currentDate, person, true);

                if (!person.getGenealogy().hasSpouse()) {
                    List<Person> toRemove = new ArrayList<>();

                    for (Person child : currentChildren) {
                        if (child.getGenealogy().getParents().contains(currentSpouse)) {
                            if (randomInt(2) == 0) {
                                toRemove.add(child);
                            }
                        }
                    }

                    currentChildren.removeAll(toRemove);
                    priorChildren.addAll(toRemove);
                    currentSpouse = null;
                }
            } else {
                marriage.processBackgroundMarriageRolls(campaign, currentDate, person);

                if (person.getGenealogy().hasSpouse()) {
                    currentSpouse = person.getGenealogy().getSpouse();
                    allSpouses.add(currentSpouse);
                }
            }

            if ((person.getGender().isFemale()) && (!person.isPregnant())) {
                procreation.processRandomProcreationCheck(campaign,
                      localDate.minusWeeks(weeksRemaining),
                      person,
                      true);

                if (person.isPregnant()) {
                    if ((currentSpouse != null) && (currentSpouse.getGender().isMale())) {
                        babysFather = currentSpouse;
                    }
                }
            }

            if ((currentSpouse != null) && (currentSpouse.getGender().isFemale()) && (!currentSpouse.isPregnant())) {
                procreation.processRandomProcreationCheck(campaign,
                      localDate.minusWeeks(weeksRemaining),
                      currentSpouse,
                      true);

                if (currentSpouse.isPregnant()) {
                    if (person.getGender().isMale()) {
                        spousesBabysFather = person;
                    }
                }
            }

            if ((person.isPregnant()) && (currentDate.isAfter(person.getDueDate()))) {
                currentChildren.addAll(procreation.birthHistoric(campaign, currentDate, person, babysFather));
                babysFather = null;
            }

            if ((currentSpouse != null) &&
                      (currentSpouse.isPregnant()) &&
                      (currentDate.isAfter(currentSpouse.getDueDate()))) {
                currentChildren.addAll(procreation.birthHistoric(campaign,
                      currentDate,
                      currentSpouse,
                      spousesBabysFather));
                spousesBabysFather = null;
            }
        }

        ResourceBundle resources = campaign.getResources();

        for (Person spouse : allSpouses) {
            recruitPerson(campaign, spouse, PrisonerStatus.FREE, true, false, false, true);

            if (currentSpouse == spouse) {
                campaign.addReport(DailyReportType.PERSONNEL,
                      String.format(resources.getString("relativeJoinsForce.text"),
                            spouse.getHyperlinkedFullTitle(),
                            person.getHyperlinkedFullTitle(),
                            resources.getString("relativeJoinsForceSpouse.text")));
            } else {
                spouse.setStatus(PersonnelStatus.BACKGROUND_CHARACTER);
            }

            MekHQ.triggerEvent(new PersonChangedEvent(spouse));
        }

        List<Person> allChildren = new ArrayList<>();
        allChildren.addAll(currentChildren);
        allChildren.addAll(priorChildren);

        for (Person child : allChildren) {
            child.setOriginFaction(person.getOriginFaction());
            child.setOriginPlanet(person.getOriginPlanet());

            int age = child.getAge(localDate);

            if (age < 16) {
                child.removeAllSkills();
            } else if (age < 18) {
                child.limitSkills(0);
            }

            Enumeration<IOption> options = new PersonnelOptions().getOptions(PersonnelOptions.LVL3_ADVANTAGES);
            for (IOption option : Collections.list(options)) {
                child.getOptions().getOption(option.getName()).clearValue();
            }

            int experienceLevel = child.getExperienceLevel(campaign, false);

            if (experienceLevel <= 0) {
                person.setLoyalty(d6(3) + 2);
            } else if (experienceLevel == 1) {
                person.setLoyalty(d6(3) + 1);
            } else {
                person.setLoyalty(d6(3));
            }

            if (experienceLevel >= 0) {
                AbstractSpecialAbilityGenerator specialAbilityGenerator = new DefaultSpecialAbilityGenerator();
                specialAbilityGenerator.setSkillPreferences(campaign.getRandomSkillPreferences());
                specialAbilityGenerator.generateSpecialAbilities(campaign, child, experienceLevel);
            }

            recruitPerson(campaign, child, PrisonerStatus.FREE, true, false, false, true);

            if (currentChildren.contains(child)) {
                campaign.addReport(DailyReportType.PERSONNEL,
                      String.format(resources.getString("relativeJoinsForce.text"),
                            child.getHyperlinkedFullTitle(),
                            person.getHyperlinkedFullTitle(),
                            resources.getString("relativeJoinsForceChild.text")));
            } else {
                child.setStatus(PersonnelStatus.BACKGROUND_CHARACTER);
            }

            MekHQ.triggerEvent(new PersonChangedEvent(child));
        }

        MekHQ.triggerEvent(new PersonChangedEvent(person));
    }


    public void removePerson(Campaign campaign, final @Nullable Person person) {
        removePerson(campaign, person, true);
    }

    /**
     * Removes a person from the campaign roster and cleans up all references.
     *
     * @param campaign the campaign
     * @param person   the person to remove, or {@code null} (no-op)
     * @param log      if {@code true}, add a report about the removal
     */
    public void removePerson(Campaign campaign, final @Nullable Person person, final boolean log) {
        if (person == null) {
            return;
        }

        Formation formation = campaign.getFormationFor(person);
        if (formation != null) {
            formation.updateCommander(campaign);
        }

        person.getGenealogy().clearGenealogyLinks();

        final Unit unit = person.getUnit();
        if (unit != null) {
            unit.remove(person, true);
        }
        person.setDoctorId(null, 0);
        removeAllPatientsFor(person, campaign.getCampaignOptions());
        person.removeAllTechJobs(campaign);
        campaign.removeKillsFor(person.getId());
        getRetirementDefectionTracker().removePerson(person);
        if (log) {
            campaign.addReport(DailyReportType.PERSONNEL,
                  person.getFullTitle() + " has been removed from the personnel roster.");
        }

        personnel.remove(person.getId());
        person.setParent(null);

        if (person.isAstech()) {
            asTechPoolMinutes = max(0, asTechPoolMinutes - Person.PRIMARY_ROLE_SUPPORT_TIME);
            asTechPoolOvertime = max(0, asTechPoolOvertime - Person.PRIMARY_ROLE_OVERTIME_SUPPORT_TIME);
        }
        MekHQ.triggerEvent(new PersonRemovedEvent(person));
    }


    /**
     * Fires events and updates state when a person's data has changed.
     *
     * @param campaign the campaign
     * @param person   the person who was updated
     */
    public void personUpdated(Campaign campaign, Person person) {
        Unit u = person.getUnit();
        if (null != u) {
            u.resetPilotAndEntity();
        }

        Formation formation = campaign.getFormationFor(person);
        if (formation != null) {
            formation.updateCommander(campaign);
        }

        MekHQ.triggerEvent(new PersonChangedEvent(person));
    }


    /**
     * Returns the person from {@code people} best suited to the given role, ranked by primary skill then secondary
     * skill as a tiebreaker.
     *
     * @param people          the collection of people to search
     * @param role            the required personnel role (primary or secondary)
     * @param primary         the name of the primary skill to rank by
     * @param secondary       optional secondary skill name used as a tiebreaker, or {@code null}
     * @param campaignOptions the campaign options
     * @param isClanCampaign  whether this is a Clan campaign
     * @param today           the current in-game date
     *
     * @return the best person for the role, or {@code null} if none qualify
     */
    public static Person findBestInRole(Collection<Person> people, PersonnelRole role,
          String primary, @Nullable String secondary,
          CampaignOptions campaignOptions, boolean isClanCampaign, LocalDate today) {
        int highest = 0;
        Person bestInRole = null;

        boolean isUseAgingEffects = campaignOptions.isUseAgeEffects();

        for (Person person : people) {
            SkillModifierData skillModifierData = person.getSkillModifierData(isUseAgingEffects, isClanCampaign,
                  today);
            if (((person.getPrimaryRole() == role) || (person.getSecondaryRole() == role)) &&
                      (person.getSkill(primary) != null)) {
                Skill primarySkill = person.getSkill(primary);
                int currentSkillLevel = Integer.MIN_VALUE;

                if (primarySkill != null) {
                    currentSkillLevel = primarySkill.getTotalSkillLevel(skillModifierData);
                }

                if (bestInRole == null || currentSkillLevel > highest) {
                    bestInRole = person;
                    highest = currentSkillLevel;
                } else if (secondary != null && currentSkillLevel == highest) {
                    Skill secondarySkill = person.getSkill(secondary);

                    if (secondarySkill == null) {
                        continue;
                    }

                    currentSkillLevel = secondarySkill.getTotalSkillLevel(skillModifierData);

                    int bestInRoleSecondarySkill = Integer.MIN_VALUE;
                    if (bestInRole.hasSkill(secondary)) {
                        bestInRoleSecondarySkill = secondarySkill.getTotalSkillLevel(skillModifierData);
                    }

                    if (currentSkillLevel > bestInRoleSecondarySkill) {
                        bestInRole = person;
                    }
                }
            }
        }
        return bestInRole;
    }

    public Person findBestInRole(PersonnelRole role, String primary, @Nullable String secondary,
          CampaignOptions campaignOptions, boolean isClanCampaign, LocalDate today) {
        return findBestInRole(getActivePersonnel(false, false),
              role,
              primary,
              secondary,
              campaignOptions,
              isClanCampaign,
              today);
    }

    public Person findBestInRole(PersonnelRole role, String skill,
          CampaignOptions campaignOptions, boolean isClanCampaign, LocalDate today) {
        return findBestInRole(role, skill, null, campaignOptions, isClanCampaign, today);
    }

    /**
     * Returns the person from {@code people} with the highest total level in the named skill.
     *
     * @param people          the collection of people to search
     * @param skillName       the name of the skill to rank by
     * @param campaignOptions the campaign options
     * @param isClanCampaign  whether this is a Clan campaign
     * @param today           the current in-game date
     *
     * @return the person with the highest skill level, or {@code null} if none have the skill
     */
    public static @Nullable Person findBestAtSkill(Collection<Person> people, String skillName,
          CampaignOptions campaignOptions, boolean isClanCampaign, LocalDate today) {
        boolean isUseAgingEffects = campaignOptions.isUseAgeEffects();

        Person bestAtSkill = null;
        int highest = 0;
        for (Person person : people) {
            Skill skill = person.getSkill(skillName);

            int totalSkillLevel = Integer.MIN_VALUE;
            if (skill != null) {
                SkillModifierData skillModifierData = person.getSkillModifierData(isUseAgingEffects, isClanCampaign,
                      today);
                totalSkillLevel = skill.getTotalSkillLevel(skillModifierData);
            }

            if (totalSkillLevel > highest) {
                highest = totalSkillLevel;
                bestAtSkill = person;
            }
        }
        return bestAtSkill;
    }

    public @Nullable Person findBestAtSkill(String skillName,
          CampaignOptions campaignOptions, boolean isClanCampaign, LocalDate today) {
        return findBestAtSkill(getActivePersonnel(false, false), skillName, campaignOptions, isClanCampaign, today);
    }


    /**
     * Writes the human resources state to XML inside a {@code <humanResources>} wrapper element.
     *
     * @param writer   the print writer
     * @param indent   the current indentation level
     * @param campaign the campaign (needed for personnel write)
     */
    public void writeToXML(PrintWriter writer, int indent, Campaign campaign) {
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "humanResources");

        // Pool fields
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "asTechPool", asTechPool);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "asTechPoolMinutes", asTechPoolMinutes);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "asTechPoolOvertime", asTechPoolOvertime);
        MHQXMLUtility.writeSimpleXMLTag(writer, indent, "medicPool", medicPool);

        // Temp crew pools
        if (!tempPersonnelRoleMap.isEmpty()) {
            writer.println(MHQXMLUtility.indentStr(indent++) + "<tempCrewPools>");
            for (Map.Entry<PersonnelRole, Integer> entry : tempPersonnelRoleMap.entrySet()) {
                writer.println(MHQXMLUtility.indentStr(indent++) + "<tempCrewPool>");
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "role", entry.getKey().name());
                MHQXMLUtility.writeSimpleXMLTag(writer, indent, "size", entry.getValue());
                writer.println(MHQXMLUtility.indentStr(--indent) + "</tempCrewPool>");
            }
            writer.println(MHQXMLUtility.indentStr(--indent) + "</tempCrewPools>");
        }

        // Personnel roster must be written before personnelWhoAdvancedInXP so that UUID→Person
        // resolution in parsePersonnelWhoAdvancedInXP has the full roster available on reload.
        personnel.writeToXML(writer, indent, campaign);

        // Personnel who advanced in XP
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "personnelWhoAdvancedInXP");
        for (Person person : personnelWhoAdvancedInXP) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, "personWhoAdvancedInXP", person.getId());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "personnelWhoAdvancedInXP");

        // Personnel market (deprecated)
        if (personnelMarket != null) {
            personnelMarket.writeToXML(writer, indent, campaign);
        }

        // New recruitment is managed at campaign level (newPersonnelMarket) — not written here
        // as it writes at campaign info level

        // Retirement defection tracker
        if (retirementDefectionTracker != null) {
            retirementDefectionTracker.writeToXML(writer, indent);
        }

        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "humanResources");
    }

    /**
     * Parses a {@code <humanResources>} node and returns a populated {@link HumanResources} instance.
     *
     * @param wn       the {@code <humanResources>} node
     * @param campaign the campaign (for context during personnel parsing)
     * @param version  the save file version
     *
     * @return a populated {@link HumanResources} instance
     */
    public static HumanResources loadFromXML(Node wn, Campaign campaign, Version version) {
        LOGGER.info("Loading HumanResources from XML...");
        HumanResources hr = campaign.getHumanResources();

        NodeList wList = wn.getChildNodes();
        for (int x = 0; x < wList.getLength(); x++) {
            Node childNode = wList.item(x);
            if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            String nodeName = childNode.getNodeName();
            try {
                if (nodeName.equalsIgnoreCase("asTechPool") || nodeName.equalsIgnoreCase("astechPool")) {
                    hr.asTechPool = MathUtility.parseInt(childNode.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("asTechPoolMinutes") ||
                                 nodeName.equalsIgnoreCase("astechPoolMinutes")) {
                    hr.asTechPoolMinutes = MathUtility.parseInt(
                          childNode.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("asTechPoolOvertime") ||
                                 nodeName.equalsIgnoreCase("astechPoolOvertime")) {
                    hr.asTechPoolOvertime = MathUtility.parseInt(
                          childNode.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("medicPool")) {
                    hr.medicPool = MathUtility.parseInt(childNode.getTextContent().trim());
                } else if (nodeName.equalsIgnoreCase("tempCrewPools")) {
                    parseTempCrewPools(hr, childNode);
                } else if (nodeName.equalsIgnoreCase("personnelWhoAdvancedInXP")) {
                    hr.personnelWhoAdvancedInXP = parsePersonnelWhoAdvancedInXP(childNode, campaign);
                } else if (nodeName.equalsIgnoreCase("personnel")) {
                    InjuryTypes.registerAll();
                    Personnel.loadFromXML(childNode, campaign, version);
                } else if (nodeName.equalsIgnoreCase("personnelMarket")) {
                    hr.personnelMarket = PersonnelMarket.generateInstanceFromXML(childNode, campaign, version);
                } else if (nodeName.equalsIgnoreCase("retirementDefectionTracker")) {
                    hr.retirementDefectionTracker = RetirementDefectionTracker.generateInstanceFromXML(childNode,
                          campaign);
                }
            } catch (Exception e) {
                LOGGER.error("Error loading humanResources child node '{}'", nodeName, e);
            }
        }

        LOGGER.info("Load HumanResources from XML complete.");
        return hr;
    }

    private static void parseTempCrewPools(HumanResources hr, Node tempCrewPoolsNode) {
        NodeList tempCrewNodes = tempCrewPoolsNode.getChildNodes();
        for (int i = 0; i < tempCrewNodes.getLength(); i++) {
            Node tempCrewNode = tempCrewNodes.item(i);
            if (tempCrewNode.getNodeName().equalsIgnoreCase("tempCrewPool")) {
                String roleStr = null;
                int size = 0;

                NodeList poolDataNodes = tempCrewNode.getChildNodes();
                for (int j = 0; j < poolDataNodes.getLength(); j++) {
                    Node dataNode = poolDataNodes.item(j);
                    String dataNodeName = dataNode.getNodeName();

                    if (dataNodeName.equalsIgnoreCase("role")) {
                        roleStr = dataNode.getTextContent().trim();
                    } else if (dataNodeName.equalsIgnoreCase("size")) {
                        size = MathUtility.parseInt(dataNode.getTextContent().trim());
                    }
                }

                if (roleStr != null) {
                    try {
                        PersonnelRole role = PersonnelRole.valueOf(roleStr);
                        if (size <= 0) {
                            hr.tempPersonnelRoleMap.remove(role);
                        } else {
                            hr.tempPersonnelRoleMap.put(role, size);
                        }
                    } catch (IllegalArgumentException e) {
                        LOGGER.warn("Unknown PersonnelRole: {}", roleStr);
                    }
                }
            }
        }
    }

    private static List<Person> parsePersonnelWhoAdvancedInXP(Node workingNode, Campaign campaign) {
        LOGGER.info("Loading personnelWhoAdvancedInXP Nodes from XML...");
        List<Person> result = new ArrayList<>();

        NodeList wList = workingNode.getChildNodes();
        for (int x = 0; x < wList.getLength(); x++) {
            Node wn2 = wList.item(x);
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("personWhoAdvancedInXP")) {
                LOGGER.warn("Unknown node type not loaded in personnelWhoAdvancedInXP nodes: {}",
                      wn2.getNodeName());
                continue;
            }

            try {
                UUID id = UUID.fromString(wn2.getTextContent().trim());
                Person person = campaign.getPerson(id);
                if (person != null) {
                    result.add(person);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to parse personWhoAdvancedInXP UUID", e);
            }
        }

        return result;
    }
}
