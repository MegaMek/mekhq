/*
 * DefaultMrbcRating.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.rating;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.FixedWingSupport;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.SmallCraft;
import megamek.common.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @version %Id%
 * @since 3/12/2012
 */
class CampaignOpsReputation extends AbstractUnitRating {

    private int nonAdminPersonnelCount = 0;

    // Tech Support & Admins.
    private int mechTechTeamsNeeded = 0;
    private int mechanicTeamsNeeded = 0;
    private int battleArmorTechTeamsNeeded = 0;
    private int aeroTechTeamsNeeded = 0;
    private int adminsNeeded = 0;

    private int totalTechTeams = 0;
    private int mechTechTeams = 0;
    private int aeroTechTeams = 0;
    private int mechanicTeams = 0;
    private int baTechTeams = 0;
    private int generalTechTeams = 0;
    private final List<String> craftWithoutCrew = new ArrayList<>();
    private int technicians = 0;

    CampaignOpsReputation(Campaign campaign) {
        super(campaign);
    }

    int getNonAdminPersonnelCount() {
        return nonAdminPersonnelCount;
    }

    int getAdminsNeeded() {
        return adminsNeeded;
    }

    int getVeeCount() {
        return getLightVeeCount() + getHeavyVeeCount();
    }

    int getMechTechTeamsNeeded() {
        return mechTechTeamsNeeded;
    }

    int getMechanicTeamsNeeded() {
        return mechanicTeamsNeeded;
    }

    int getBattleArmorTechTeamsNeeded() {
        return battleArmorTechTeamsNeeded;
    }

    int getAeroTechTeamsNeeded() {
        return aeroTechTeamsNeeded;
    }

    private void countUnits() {
        // Reset counts.
        setTotalSkillLevels(BigDecimal.ZERO);

        List<Unit> unitList = getCampaign().getCopyOfUnits();
        for (Unit u : unitList) {
            if (null == u) {
                continue;
            }
            if (u.isMothballed()) {
                continue;
            }
            if (!u.isPresent()) {
                continue;
            }

            updateUnitCounts(u);

            Person p = u.getCommander();
            if (p != null) {
                getCommanderList().add(p);
            }

            Entity entity = u.getEntity();
            updateBayCount(entity);
            int unitType = UnitType.determineUnitTypeCode(entity);
            if (UnitType.INFANTRY == unitType ||
                UnitType.BATTLE_ARMOR == unitType) {
                updateTotalSkill((Infantry) entity);
            } else {
                updateTotalSkill(u.getEntity().getCrew(),
                                 UnitType.determineUnitTypeCode(entity));
            }

            // todo: Add Mobile Structure when Megamek supports it.
            switch (unitType) {
                case UnitType.SPACE_STATION:
                case UnitType.NAVAL:
                case UnitType.DROPSHIP:
                    if (u.getFullCrewSize() < u.getActiveCrew().size()) {
                        addCraftWithoutCrew(u);
                    }
                    break;
                case UnitType.WARSHIP:
                case UnitType.JUMPSHIP:
                    updateDockingCollarCount((Jumpship) entity);
                    if (u.getFullCrewSize() < u.getActiveCrew().size()) {
                        addCraftWithoutCrew(u);
                    }
                    break;
            }
            // UnitType doesn't include FixedWingSupport.
            if (entity instanceof FixedWingSupport) { 
                if (u.getFullCrewSize() < u.getActiveCrew().size()) {
                    addCraftWithoutCrew(u);
                }
            }
        }
    }

    private void updateTotalSkill(Infantry infantry) {
        Crew crew = infantry.getCrew();
        if (null == crew) {
            return;
        }
        int gunnery = crew.getGunnery();
        int antiMek = infantry.getAntiMekSkill();
        if (antiMek == 0) {
            antiMek = gunnery + 1;
        }
        BigDecimal skillLevel = BigDecimal.valueOf(gunnery)
                                          .add(BigDecimal.valueOf(antiMek));
        incrementSkillRatingCounts(getExperienceLevelName(skillLevel));
        setTotalSkillLevels(getTotalSkillLevels(false).add(skillLevel));
    }

    private void updateTotalSkill(Crew crew, int unitType) {
        // Make sure we have a crew.
        if (crew == null) {
            return;
        }

        boolean hasPilot = false;
        int gunnery;
        int piloting = 0;

        switch (unitType) {
            case UnitType.MEK:
            case UnitType.WARSHIP:
            case UnitType.SMALL_CRAFT:
            case UnitType.DROPSHIP:
            case UnitType.CONV_FIGHTER:
            case UnitType.AERO:
            case UnitType.VTOL:
            case UnitType.TANK:
                gunnery = crew.getGunnery();
                piloting = crew.getPiloting();
                hasPilot = true;
                break;
            case UnitType.PROTOMEK:
                gunnery = crew.getGunnery();
                break;
            default:
                return;
        }

        BigDecimal skillLevel = BigDecimal.valueOf(gunnery);
        if (hasPilot) {
            skillLevel = skillLevel.add(BigDecimal.valueOf(piloting));
        } else {
            // Assume a piloting equal to Gunnery +1.
            skillLevel = skillLevel.add(BigDecimal.valueOf(gunnery))
                                   .add(BigDecimal.ONE);
        }

        incrementSkillRatingCounts(getExperienceLevelName(skillLevel));
        setTotalSkillLevels(getTotalSkillLevels(false).add(skillLevel));
    }

    @Override
    protected BigDecimal getNumberUnits() {
        return new BigDecimal(getTotalCombatUnits());
    }

    private int getTotalCombatUnits() {
        int totalCombatUnits = getMechCount();
        totalCombatUnits += getFighterCount();
        totalCombatUnits += getProtoCount();
        totalCombatUnits += getVeeCount();
        totalCombatUnits += (getBattleArmorCount() / 5);
        totalCombatUnits += (getInfantryCount() / 28);
        totalCombatUnits += getDropshipCount();
        totalCombatUnits += getSmallCraftCount();
        return totalCombatUnits;
    }

    private int getTotalForceUnits() {
        int totalGround = 0;
        int totalAero = 0;
        int totalInfantry = 0;
        int totalBattleArmor = 0;
        int forceTotal;

        // Count total units for transport
        getTotalCombatUnits();

        List<Unit> unitList = getCampaign().getCopyOfUnits();
        for (Unit u : unitList) {
            if (u == null) {
                continue;
            }
            if (u.isMothballed() || !u.hasPilot()) {
                continue;
            }

            if ((u.getEntity().getEntityType() &
                 Entity.ETYPE_WARSHIP) == Entity.ETYPE_WARSHIP) {
                totalAero++;
            } else if ((u.getEntity().getEntityType() &
                        Entity.ETYPE_JUMPSHIP) == Entity.ETYPE_JUMPSHIP) {
                //noinspection UnnecessaryContinue
                continue;
            } else if ((u.getEntity().getEntityType() &
                        Entity.ETYPE_AERO) == Entity.ETYPE_AERO) {
                totalAero++;
            } else if (((u.getEntity().getEntityType() &
                         Entity.ETYPE_MECH) == Entity.ETYPE_MECH)
                       || ((u.getEntity().getEntityType() &
                            Entity.ETYPE_TANK) == Entity.ETYPE_TANK)) {
                totalGround++;
            } else if ((u.getEntity().getEntityType() &
                        Entity.ETYPE_BATTLEARMOR) == Entity.ETYPE_BATTLEARMOR) {
                totalBattleArmor++;
            } else if ((u.getEntity().getEntityType() &
                        Entity.ETYPE_INFANTRY) == Entity.ETYPE_INFANTRY) {
                totalInfantry++;
            }

        }

        forceTotal = totalGround + totalAero + (totalInfantry / 28) +
                     (totalBattleArmor / 5);
        return forceTotal;
    }

    @Override
    protected BigDecimal calcAverageExperience() {
        int totalCombatUnits = getTotalForceUnits();

        if (totalCombatUnits == 0) {
            return BigDecimal.ZERO;
        }

        return getTotalSkillLevels().divide(BigDecimal.valueOf(totalCombatUnits),
                                            2, BigDecimal.ROUND_HALF_DOWN);
    }

    private void calcNeededTechs() {
        int protoTeamCount = BigDecimal.valueOf(getProtoCount())
                                       .divide(BigDecimal.valueOf(5), 0,
                                               RoundingMode.HALF_UP)
                                       .intValue();
        setMechTechTeamsNeeded(getMechCount() + protoTeamCount);
        setAeroTechTeamsNeeded(getFighterCount() + getSmallCraftCount());
        int infantryTeamCount = BigDecimal.valueOf(getInfantryCount())
                                          .divide(BigDecimal.valueOf(112),
                                                  0,
                                                  RoundingMode.HALF_UP)
                                          .intValue();
        setMechanicTeamsNeeded(getSuperHeavyVeeCount() + getVeeCount() +
                               infantryTeamCount);
        setBattleArmorTechTeamsNeeded(BigDecimal.valueOf(getBattleArmorCount())
                                                .divide(BigDecimal.valueOf(5),
                                                        0,
                                                        RoundingMode.HALF_UP)
                                                .intValue());
    }

    private void updatePersonnelCounts() {
        setNonAdminPersonnelCount(0);
        List<Person> personnelList =
                new ArrayList<>(getCampaign().getPersonnel());
        for (Person p : personnelList) {
            if (p.isAdmin() || p.isDoctor()) {
                continue;
            }
            if (p.isTech()) {
                technicians++;
            }
            setNonAdminPersonnelCount(getNonAdminPersonnelCount() + 1);
        }
        setNonAdminPersonnelCount(getNonAdminPersonnelCount() +
                                  getCampaign().getAstechPool());
    }

    private void calcNeededAdmins() {
        setAdminsNeeded(BigDecimal.valueOf(getNonAdminPersonnelCount())
                                  .divide(BigDecimal.TEN, 0,
                                          RoundingMode.UP).intValue());
    }

    // todo Combat Personnel (up to 1/4 total) may be assigned double-duty and count as 1/3 of a tech.
    // todo Combat Personnel (up to 1/4 total) may be assigned double-duty and count as 1/3 of an admin.
    // todo Distinguish between Merc and Government personnel (1/2 admin needs for gov).

    @Override
    protected void initValues() {
        super.initValues();

        setMechCount(0);
        setProtoCount(0);
        setLightVeeCount(0);
        setBattleArmorCount(0);
        setInfantryCount(0);
        setFighterCount(0);
        setDropshipCount(0);
        setSmallCraftCount(0);
        setJumpshipCount(0);
        setDockingCollarCount(0);
        clearCraftWithoutCrew();

        countUnits();
        calcNeededTechs();
        updatePersonnelCounts();
        calcNeededAdmins();
    }

    @Override
    protected int calculateUnitRatingScore() {
        int totalScore = getExperienceValue();
        totalScore += getCommanderValue();
        totalScore += getCombatRecordValue();
        totalScore += getTransportValue();
        totalScore += getSupportValue();
        totalScore += getFinancialValue();
        totalScore += getCrimesPenalty();
        totalScore += getIdleTimeModifier();

        return totalScore;
    }

    @Override
    public String getAverageExperience() {
        if (getNumberUnits().compareTo(BigDecimal.ZERO) == 0) {
            return SkillType.getExperienceLevelName(-1);
        }
        switch (getExperienceValue()) {
            case 0:
                return SkillType.getExperienceLevelName(SkillType.EXP_GREEN);
            case 5:
                return SkillType.getExperienceLevelName(SkillType.EXP_REGULAR);
            case 10:
                return SkillType.getExperienceLevelName(SkillType.EXP_VETERAN);
            case 20:
                return SkillType.getExperienceLevelName(SkillType.EXP_ELITE);
            default:
                return SkillType.getExperienceLevelName(-1);
        }
    }

    @Override
    protected String getExperienceLevelName(BigDecimal experience) {
        if (getNumberUnits().compareTo(BigDecimal.ZERO) == 0) {
            return SkillType.getExperienceLevelName(-1);
        }

        final BigDecimal eliteThreshold = new BigDecimal("5.00");
        final BigDecimal vetThreshold = new BigDecimal("7.00");
        final BigDecimal regThreshold = new BigDecimal("9.00");

        if (experience.compareTo(regThreshold) > 0) {
            return SkillType.getExperienceLevelName(SkillType.EXP_GREEN);
        } else if (experience.compareTo(vetThreshold) > 0) {
            return SkillType.getExperienceLevelName(SkillType.EXP_REGULAR);
        } else if (experience.compareTo(eliteThreshold) > 0) {
            return SkillType.getExperienceLevelName(SkillType.EXP_VETERAN);
        }
        return SkillType.getExperienceLevelName(SkillType.EXP_ELITE);
    }

    public int getExperienceValue() {
        if (getNumberUnits().compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }

        BigDecimal averageExp = calcAverageExperience();
        String level = getExperienceLevelName(averageExp);
        if (SkillType.getExperienceLevelName(-1).equalsIgnoreCase(level)) {
            return 0;
        } else if (SkillType.getExperienceLevelName(SkillType.EXP_GREEN)
                            .equalsIgnoreCase(level)) {
            return 5;
        } else if (SkillType.getExperienceLevelName(SkillType.EXP_REGULAR)
                            .equalsIgnoreCase(level)) {
            return 10;
        } else if (SkillType.getExperienceLevelName(SkillType.EXP_VETERAN)
                            .equalsIgnoreCase(level)) {
            return 20;
        }
        return 40;
    }

    @Override
    public int getCommanderValue() {
        Person commander = getCommander();
        if (commander == null) {
            return 0;
        }
        int skillTotal = getCommanderSkill(SkillType.S_LEADER);
        skillTotal += getCommanderSkill(SkillType.S_TACTICS);
        skillTotal += getCommanderSkill(SkillType.S_STRATEGY);
        skillTotal += getCommanderSkill(SkillType.S_NEG);

        // ToDo AToW Traits.
        // ToDo MHQ would need  to support: Combat Sense, Connections, 
        // ToDo                             Reputation, Wealth, High CHA, 
        // ToDo                             Combat Paralysis, 
        // ToDo                             Unlucky & Low CHA.

        int commanderValue = skillTotal; // ToDo + positiveTraits - negativeTraits.

        return commanderValue > 0 ? commanderValue : 1;
    }

    @Override
    public String getUnitRating() {
        // Campaign Operations does not use letter-grades.
        return getModifier() + " (" + calculateUnitRatingScore() + ")";
    }

    @Override
    public int getUnitRating(int score) {
        // Campaign Operations does not use letter-grades.
        return 0;
    }

    @Override
    public String getUnitRatingName(int rating) {
        // Campaign Operations does not use letter-grades.
        return "";
    }

    @Override
    public int getTransportValue() {
        if (getTotalCombatUnits() == 0) {
            return 0;
        }

        int totalValue = 0;

        // todo Superheavys.
        // Find out how short of transport bays we are.
        boolean doubleExcessCapacity = true;
        boolean fullCapacity = true;
        boolean excessCapacity = true;
        int heavyVeeBays = getHeavyVeeBayCount();

        if (getMechBayCount() == getMechCount()) {
            excessCapacity = false;
            doubleExcessCapacity = false;
        } else if (getMechBayCount() < getMechCount()) {
            fullCapacity = false;
            excessCapacity = false;
            doubleExcessCapacity = false;
        } else if (getMechBayCount() < getMechCount() * 2) {
            fullCapacity = false;
            doubleExcessCapacity = false;
        }
        if (getProtoBayCount() == getProtoCount()) {
            excessCapacity = false;
            doubleExcessCapacity = false;
        } else if (getProtoBayCount() < getProtoCount()) {
            fullCapacity = false;
            excessCapacity = false;
            doubleExcessCapacity = false;
        } else if (getProtoBayCount() < getProtoCount() * 2) {
            fullCapacity = false;
            doubleExcessCapacity = false;
        }
        if (getHeavyVeeBayCount() == getHeavyVeeCount()) {
            excessCapacity = false;
            doubleExcessCapacity = false;
        } else if (getHeavyVeeBayCount() < getHeavyVeeCount()) {
            fullCapacity = false;
            excessCapacity = false;
            doubleExcessCapacity = false;
        } else if (getHeavyVeeBayCount() < getHeavyVeeCount() * 2) {
            fullCapacity = false;
            doubleExcessCapacity = false;
        }
        heavyVeeBays -= getHeavyVeeBayCount();
        int lightVeeBays = getLightVeeBayCount() + heavyVeeBays;
        if (lightVeeBays == getLightVeeCount()) {
            excessCapacity = false;
            doubleExcessCapacity = false;
        } else if (lightVeeBays < getLightVeeCount()) {
            fullCapacity = false;
            excessCapacity = false;
            doubleExcessCapacity = false;
        } else if (lightVeeBays < getLightVeeCount() * 2) {
            fullCapacity = false;
            doubleExcessCapacity = false;
        }
        if (getFighterBayCount() == getFighterCount()) {
            excessCapacity = false;
            doubleExcessCapacity = false;
        } else if (getFighterBayCount() < getFighterCount()) {
            fullCapacity = false;
            excessCapacity = false;
            doubleExcessCapacity = false;
        } else if (getFighterBayCount() < getFighterCount() * 2) {
            fullCapacity = false;
            doubleExcessCapacity = false;
        }
        if ((getBaBayCount()) == getBattleArmorCount() / 5) {
            excessCapacity = false;
            doubleExcessCapacity = false;
        } else if ((getBaBayCount()) < getBattleArmorCount() / 5) {
            fullCapacity = false;
            excessCapacity = false;
            doubleExcessCapacity = false;
        } else if ((getBaBayCount() * 2) < 2 * getBattleArmorCount() / 5) {
            fullCapacity = false;
            doubleExcessCapacity = false;
        }
        if (getInfantryBayCount() < getInfantryCount() / 28) {
            excessCapacity = false;
            doubleExcessCapacity = false;
        } else if (getInfantryBayCount() < getInfantryCount() / 28) {
            fullCapacity = false;
            excessCapacity = false;
            doubleExcessCapacity = false;
        } else if (getInfantryBayCount() < getInfantryCount() / 14) {
            fullCapacity = false;
            doubleExcessCapacity = false;
        }
        if (getSmallCraftBayCount() == getSmallCraftCount()) {
            excessCapacity = false;
            doubleExcessCapacity = false;
        } else if (getSmallCraftBayCount() < getSmallCraftCount()) {
            fullCapacity = false;
            excessCapacity = false;
            doubleExcessCapacity = false;
        } else if (getSmallCraftBayCount() < (getSmallCraftCount() * 2)) {
            fullCapacity = false;
            doubleExcessCapacity = false;
        }

        //Find the percentage of units that are transported.
        if (doubleExcessCapacity) {
            totalValue += 10;
        } else if (excessCapacity) {
            totalValue += 5;
        } else if (fullCapacity) {
            totalValue += 0;
        } else {
            totalValue -= 5;
        }

        if (getDropshipCount() < 1) {
            totalValue -= 5;
        }

        // ToDo Calculate transport needs and capacity for support personnel.
        // According to Campaign Ops, this will require tracking bay personnel 
        // & passenger quarters.

        if (getJumpshipCount() > 0) {
            totalValue += 10;
        }
        if (getWarshipCount() > 0) {
            totalValue += 10;
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.DAY_OF_YEAR, 1);
            cal.set(Calendar.MONTH, 1);
            cal.set(Calendar.YEAR, 2800);
            cal.set(Calendar.HOUR, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            if (getCampaign().getDate().after(cal.getTime())) {
                totalValue += 5;
            }
        }
        if ((getDropshipCount() > 0) && (getDockingCollarCount() >=
                                         getDropshipCount())) {
            totalValue += 5;
        }

        return totalValue;
    }

    int calcTechSupportValue() {
        int totalValue = 0;
        setTotalTechTeams(0);
        int astechTeams;
        setMechTechTeams(0);
        setAeroTechTeams(0);
        setMechanicTeams(0);
        setBaTechTeams(0);
        setGeneralTechTeams(0);

        // How many astech teams do we have?
        astechTeams = getCampaign().getNumberAstechs() / 6;

        for (Person tech : getCampaign().getTechs()) {
            // If we're out of astech teams, the rest of the techs are 
            // unsupported and don't count.
            if (astechTeams <= 0) {
                break;
            }

            if (tech.getSkill(SkillType.S_TECH_MECH) != null) {
                setMechTechTeams(getMechTechTeams() + 1);
                astechTeams--;
            } else if (tech.getSkill(SkillType.S_TECH_AERO) != null) {
                setAeroTechTeams(getAeroTechTeams() + 1);
                astechTeams--;
            } else if (tech.getSkill(SkillType.S_TECH_MECHANIC) != null) {
                setMechanicTeams(getMechanicTeams() + 1);
                astechTeams--;
            } else if (tech.getSkill(SkillType.S_TECH_BA) != null) {
                setBaTechTeams(getBaTechTeams() + 1);
                astechTeams--;
            } else {
                setGeneralTechTeams(getGeneralTechTeams() + 1);
                astechTeams--;
            }
        }

        boolean techShortage = false;
        if (getMechTechTeamsNeeded() > getMechTechTeams()) {
            techShortage = true;
        }
        if (getAeroTechTeamsNeeded() > getAeroTechTeams()) {
            techShortage = true;
        }
        if (getMechanicTeamsNeeded() > getMechanicTeams()) {
            techShortage = true;
        }
        if (getBattleArmorTechTeamsNeeded() > getBaTechTeams()) {
            techShortage = true;
        }

        setTotalTechTeams(getMechTechTeams() + getAeroTechTeams() +
                          getMechanicTeams() + getBaTechTeams() +
                          getGeneralTechTeams());
        int totalTechTeamsNeeded = getMechTechTeamsNeeded() +
                                   getAeroTechTeamsNeeded() +
                                   getMechanicTeamsNeeded() +
                                   getBattleArmorTechTeamsNeeded();
        setSupportPercent(BigDecimal.ZERO);
        if (totalTechTeamsNeeded != 0) {
            setSupportPercent(BigDecimal.valueOf(getTotalTechTeams())
                                        .divide(BigDecimal.valueOf(totalTechTeamsNeeded),
                                                5,
                                                BigDecimal.ROUND_HALF_UP)
                                        .multiply(HUNDRED));
        }

        if (techShortage) {
            totalValue -= 5;
        } else {
            if (getSupportPercent().compareTo(BigDecimal.valueOf(200)) > 0) {
                totalValue += 15;
            } else if (getSupportPercent().compareTo(BigDecimal.valueOf(175)) >
                       0) {
                totalValue += 10;
            } else if (getSupportPercent().compareTo(BigDecimal.valueOf(149)) >
                       0) {
                totalValue += 5;
            }
        }

        return totalValue;
    }

    // Campaign Ops counts both Doctors and Admins as admins.
    private int calcAdminSupportValue() {
        int admins = getCampaign().getAdmins().size();
        int docs = getCampaign().getDoctors().size();
        if (getAdminsNeeded() > (admins + docs)) {
            return -5;
        }
        return 0;
    }

    private int calcLargeCraftSupportValue() {
        boolean crewShortage = false;
        for (Unit u : getCampaign().getCopyOfUnits()) {
            if (u.getEntity() instanceof SmallCraft ||
                u.getEntity() instanceof Jumpship) {
                if (u.getActiveCrew().size() < u.getFullCrewSize()) {
                    crewShortage = true;
                    break;
                }
            }
        }

        return crewShortage ? -5 : 0;
    }

    @Override
    public int getSupportValue() {
        int value = calcTechSupportValue();
        value += calcAdminSupportValue();
        value += calcLargeCraftSupportValue();
        return value;
    }

    @Override
    public BigDecimal getTransportPercent() {
        // Handled under getTransportValue()
        return BigDecimal.ZERO;
    }

    public int getFinancialValue() {
        return getCampaign().getFinances().isInDebt() ? -10 : 0;
    }

    // ToDo: MekHQ doesn't currently support recording crimes.
    private int getCrimesPenalty() {
        return 0;
    }

    // ToDo MekHQ doesn't current apply completion dates to missions.
    private int getIdleTimeModifier() {
        return 0;
    }

    @Override
    public int getModifier() {
        BigDecimal reputation = new BigDecimal(calculateUnitRatingScore());
        return reputation.divide(BigDecimal.TEN, 0,
                                 RoundingMode.DOWN).intValue();
    }

    private String getExperienceDetails() {
        StringBuilder out = new StringBuilder();
        out.append(String.format("%-" + HEADER_LENGTH + "s %3d", "Experience:",
                                 getExperienceValue())).append("\n");
        out.append(String.format("    %-" + SUBHEADER_LENGTH + "s %3s",
                                 "Average Experience:",
                                 getExperienceLevelName(calcAverageExperience())))
           .append("\n");

        final String TEMPLATE = "        #%-" + CATEGORY_LENGTH + "s %3d";
        Map<String, Integer> skillRatingCounts = getSkillRatingCounts();
        boolean first = true;
        for (String nm : SkillType.SKILL_LEVEL_NAMES) {
            if (skillRatingCounts.containsKey(nm)) {
                if (!first) {
                    out.append("\n");
                }
                out.append(String.format(TEMPLATE, nm + ":",
                                         skillRatingCounts.get(nm)));
                first = false;
            }
        }
        return out.toString();
    }

    private String getCommanderDetails() {
        StringBuilder out = new StringBuilder();
        String commanderName = null == getCommander() ? "" :
                               "(" + getCommander().getName() + ")";
        out.append(String.format("%-" + HEADER_LENGTH + "s %3d %s",
                                 "Commander:", getCommanderValue(),
                                 commanderName));

        final String TEMPLATE = "    %-" + SUBHEADER_LENGTH + "s %3d";
        out.append("\n").append(String.format(TEMPLATE, "Leadership:",
                                              getCommanderSkill(SkillType.S_LEADER)));
        out.append("\n").append(String.format(TEMPLATE, "Negotiation:",
                                              getCommanderSkill(SkillType.S_NEG)));
        out.append("\n").append(String.format(TEMPLATE, "Strategy:",
                                              getCommanderSkill(SkillType.S_STRATEGY)));
        out.append("\n").append(String.format(TEMPLATE, "Tactics:",
                                              getCommanderSkill(SkillType.S_TACTICS)));

        return out.toString();
    }

    private String getCombatRecordDetails() {
        final String TEMPLATE = "    %-" + SUBHEADER_LENGTH + "s %3d";

        return String.format("%-" + HEADER_LENGTH + "s %3d", "Combat Record:",
                             getCombatRecordValue()) +
               "\n" + String.format(TEMPLATE, "Successful Missions:",
                                    getSuccessCount()) +
               "\n" + String.format(TEMPLATE, "Failed Missions:",
                                    getFailCount()) +
               "\n" + String.format(TEMPLATE, "Contract Breaches:",
                                    getBreachCount());
    }

    String getTransportationDetails() {
        final String TEMPLATE = "    %-" + CATEGORY_LENGTH +
                                "s %3d needed / %3d available";

        int heavyVeeBayCount = getHeavyVeeBayCount();
        int excessHeavyVeeBays = Math.max(0, heavyVeeBayCount - getHeavyVeeCount());

        String out = String.format("%-" + HEADER_LENGTH + "s %3d",
                                   "Transportation:", getTransportValue()) +
                     "\n" + String.format(TEMPLATE, "Mech Bays:",
                                          getMechCount(), getMechBayCount()) +
                     "\n" + String.format(TEMPLATE, "Fighter Bays:",
                                          getFighterCount(), getFighterBayCount()) +
                     "\n" + String.format(TEMPLATE, "Small Craft Bays:",
                                          getSmallCraftCount(),
                                          getSmallCraftBayCount()) +
                     "\n" + String.format(TEMPLATE, "Protomech Bays:",
                                          getProtoCount(),
                                          getProtoBayCount()) +
                     "\n" + String.format(TEMPLATE, "Heavy Vehicle Bays:",
                                          getHeavyVeeCount(),
                                          heavyVeeBayCount) +
                     "\n" + String.format(TEMPLATE, "Light Vehicle Bays:",
                                          getLightVeeCount(),
                                          getLightVeeBayCount()) +
                     " (plus " + excessHeavyVeeBays + " excess heavy)" +
                     "\n" + String.format(TEMPLATE, "BA Bays:",
                                          getBattleArmorCount() / 5,
                                          getBaBayCount()) +
                     "\n" + String.format(TEMPLATE, "Infantry Bays:",
                                          getInfantryCount() / 28,
                                          getInfantryBayCount()) +
                     "\n" + String.format(TEMPLATE, "Docking Collars:",
                                          getDropshipCount(),
                                          getDockingCollarCount());

        final String TEMPLATE_2 = "    %-" + CATEGORY_LENGTH + "s %3s";
        out += "\n" + String.format(TEMPLATE_2, "Has Jumpships?",
                                    getJumpshipCount() > 0 ? "Yes" : "No");
        out += "\n" + String.format(TEMPLATE_2, "Has Warships?",
                                    getWarshipCount() > 0 ? "Yes" : "No");

        return out;
    }

    private String getSupportDetails() {
        StringBuilder out = new StringBuilder();
        out.append(String.format("%-" + HEADER_LENGTH + "s %3d",
                                 "Support:", getSupportValue()));

        final String TEMPLATE_CAT = "        %-" + CATEGORY_LENGTH +
                                    "s %4d needed / %4d available";
        out.append("\n    Tech Support:");
        out.append("\n").append(String.format(TEMPLATE_CAT, "Mech Techs:",
                                              getMechTechTeamsNeeded(),
                                              getMechTechTeams()));
        out.append("\n            NOTE: Protomechs and mechs use same techs.");
        out.append("\n").append(String.format(TEMPLATE_CAT,
                                              "Aero Techs:",
                                              getAeroTechTeamsNeeded(),
                                              getAeroTechTeams()));
        out.append("\n").append(String.format(TEMPLATE_CAT, "Mechanics:",
                                              getMechanicTeamsNeeded(),
                                              getMechanicTeams()));
        out.append("\n            NOTE: Vehicles and Infantry use the same" +
                   " mechanics.");
        out.append("\n").append(String.format(TEMPLATE_CAT, "BA Techs:",
                                              getBattleArmorTechTeamsNeeded(),
                                              getBaTechTeams()));
        out.append("\n").append(String.format(TEMPLATE_CAT, "Astechs:",
                                              technicians * 6,
                                              getCampaign().getAstechPool()));
        out.append("\n").append(String.format("    %-" + (CATEGORY_LENGTH + 4) +
                                              "s %4d needed / %4d available",
                                              "Admin Support:",
                                              getAdminsNeeded(),
                                              getTotalAdmins()));
        out.append("\n    Large Craft Crew:");
        if (getCraftWithoutCrew().size() < 1) {
            out.append("\n        All fully crewed.");
        } else {
            for (String s : getCraftWithoutCrew()) {
                out.append("\n        ").append(s).append(" short crew.");
            }
        }

        return out.toString();
    }

    private int getTotalAdmins() {
        return getCampaign().getAdmins().size() + getCampaign().getDoctors()
                                                               .size();
    }

    @Override
    public String getDetails() {
        final String TEMPLATE = "%-" + HEADER_LENGTH + "s %s";
        initValues();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(TEMPLATE, "Unit Reputation:",
                                calculateUnitRatingScore()));
        sb.append("\n").append("    Method: Campaign Operations\n\n");
        sb.append(getExperienceDetails()).append("\n\n");
        sb.append(getCommanderDetails()).append("\n\n");
        sb.append(getCombatRecordDetails()).append("\n\n");
        sb.append(getTransportationDetails()).append("\n\n");
        sb.append(getSupportDetails()).append("\n\n");

        sb.append(String.format(TEMPLATE, "Financial", getFinancialValue()));
        sb.append("\n").append(String.format("    %-" + SUBHEADER_LENGTH +
                                             "s %3s",
                                             "In Debt?",
                                             getCampaign().getFinances()
                                                          .isInDebt() ? "Yes" :
                                             "No"));

        sb.append("\n\n")
          .append(String.format(TEMPLATE, "Criminal Activity:", 0))
          .append(" (MHQ does not currently track criminal activity.)");

        sb.append("\n\n")
          .append(String.format(TEMPLATE, "Inactivity Modifier:", 0))
          .append(" (MHQ does not track end dates for missions/contracts.)");

        return new String(sb);
    }

    @Override
    public String getHelpText() {
        return "Method: Campaign Ops\n" +
               "An attempt to match the Campaign Ops method for calculating " +
               "the Reputation as closely as possible.\n" +
               "Known differences include the following:\n" +
               "+ Command: Does not incorporate any positive or negative " +
               "traits from AToW or BRPG3." +
               "+ Transportation: Transportation needs of Support Personnel " +
               "are not accounted for as MHQ does not " +
               "track Bay Personnel or Passenger Quarters.\n" +
               "+ Criminal Activity: MHQ does not currently track criminal " +
               "activity." +
               "+ Inactivity: MHQ does not track end dates for missions/" +
               "contracts.";
    }

    private void setNonAdminPersonnelCount(int nonAdminPersonnelCount) {
        this.nonAdminPersonnelCount = nonAdminPersonnelCount;
    }

    private void setMechTechTeamsNeeded(int mechTechTeamsNeeded) {
        this.mechTechTeamsNeeded = mechTechTeamsNeeded;
    }

    private void setMechanicTeamsNeeded(int mechanicTeamsNeeded) {
        this.mechanicTeamsNeeded = mechanicTeamsNeeded;
    }

    private void setBattleArmorTechTeamsNeeded(int battleArmorTechTeamsNeeded) {
        this.battleArmorTechTeamsNeeded = battleArmorTechTeamsNeeded;
    }

    private void setAeroTechTeamsNeeded(int aeroTechTeamsNeeded) {
        this.aeroTechTeamsNeeded = aeroTechTeamsNeeded;
    }

    private void setAdminsNeeded(int adminsNeeded) {
        this.adminsNeeded = adminsNeeded;
    }

    private int getTotalTechTeams() {
        return totalTechTeams;
    }

    private void setTotalTechTeams(int totalTechTeams) {
        this.totalTechTeams = totalTechTeams;
    }

    private int getMechTechTeams() {
        return mechTechTeams;
    }

    private void setMechTechTeams(int mechTechTeams) {
        this.mechTechTeams = mechTechTeams;
    }

    private int getAeroTechTeams() {
        return aeroTechTeams;
    }

    private void setAeroTechTeams(int aeroTechTeams) {
        this.aeroTechTeams = aeroTechTeams;
    }

    private int getMechanicTeams() {
        return mechanicTeams;
    }

    private void setMechanicTeams(int mechanicTeams) {
        this.mechanicTeams = mechanicTeams;
    }

    private int getBaTechTeams() {
        return baTechTeams;
    }

    private void setBaTechTeams(int baTechTeams) {
        this.baTechTeams = baTechTeams;
    }

    private int getGeneralTechTeams() {
        return generalTechTeams;
    }

    private void setGeneralTechTeams(int generalTechTeams) {
        this.generalTechTeams = generalTechTeams;
    }

    private List<String> getCraftWithoutCrew() {
        List<String> copy = new ArrayList<>(craftWithoutCrew.size());
        Collections.copy(copy, craftWithoutCrew);
        return copy;
    }

    private void addCraftWithoutCrew(Unit u) {
        craftWithoutCrew.add(u.getName());
    }

    private void clearCraftWithoutCrew() {
        craftWithoutCrew.clear();
    }
}
