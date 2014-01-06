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
import java.util.List;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.ConvFighter;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.LargeSupportTank;
import megamek.common.Mech;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.UnitType;
import megamek.common.VTOL;
import megamek.common.Warship;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @version %Id%
 * @since 3/12/2012
 */
public class InterstellarOpsReputation extends AbstractUnitRating {

    private int nonAdminPersonnelCount = 0;
    private int nonTransportPersonnelCount = 0;

    // Tech Support & Admins.
    private int mechTechTeamsNeeded = 0;
    private int protoTechTeamsNeeded = 0;
    private int veeTechTeamsNeeded = 0;
    private int battleArmorTechTeamsNeeded = 0;
    private int infantryTechTeamsNeeded = 0;
    private int fighterTechTeamsNeeded = 0;
    private int adminsNeeded = 0;

    private int totalTechTeams = 0;
    private int astechTeams = 0;
    private int mechTechTeams = 0;
    private int fighterTechTeams = 0;
    private int veeTechTeams = 0;
    private int baTechTeams = 0;
    private int generalTechTeams = 0; // ToDo: Should Protomech & Infantry techs be counted as separate skills?
    private List<String> craftWithoutCrew = new ArrayList<String>();

    public InterstellarOpsReputation(Campaign campaign) {
        super(campaign);
    }

    public int getDropshipCount() {
        return dropshipCount;
    }

    protected int getNonAdminPersonnelCount() {
        return nonAdminPersonnelCount;
    }

    protected int getAdminsNeeded() {
        return adminsNeeded;
    }

    protected int getMechCount() {
        return mechCount;
    }

    protected int getProtoCount() {
        return protoCount;
    }

    protected int getVeeCount() {
        return lightVeeCount;
    }

    protected int getBattleArmorCount() {
        return battleArmorCount;
    }

    protected int getInfantryCount() {
        return infantryCount;
    }

    protected int getFighterCount() {
        return fighterCount;
    }

    protected int getMechTechTeamsNeeded() {
        return mechTechTeamsNeeded;
    }

    protected int getProtoTechTeamsNeeded() {
        return protoTechTeamsNeeded;
    }

    protected int getVeeTechTeamsNeeded() {
        return veeTechTeamsNeeded;
    }

    protected int getBattleArmorTechTeamsNeeded() {
        return battleArmorTechTeamsNeeded;
    }

    protected int getInfantryTechTeamsNeeded() {
        return infantryTechTeamsNeeded;
    }

    protected int getFighterTechTeamsNeeded() {
        return fighterTechTeamsNeeded;
    }

    private void updateUnitCounts() {
        // Reset counts.
        totalSkillLevels = BigDecimal.ZERO;

        List<Unit> unitList = new ArrayList<Unit>(campaign.getUnits());
        for (Unit u : unitList) {
            if (u.isMothballed()) {
                continue;
            }

            Person p = u.getCommander();
            if (p != null) {
                commanderList.add(p);
            }

            Entity entity = u.getEntity();
            if (entity instanceof Mech) {
                mechCount++;
                updateTotalSkill(u.getCrew(), UnitType.MEK);
            } else if (entity instanceof Dropship) {
                // Tech needs are handled by the crew directly.
                dropshipCount++;
                updateTotalSkill(u.getCrew(), UnitType.DROPSHIP);
                updateBayCount((Dropship) entity);
            } else if (entity instanceof Warship) {
                // Tech needs are handled by the crew directly.
                warshipCount++;
                updateTotalSkill(u.getCrew(), UnitType.WARSHIP);
                updateBayCount((Warship) entity);
                updateDockingCollarCount((Warship) entity);
            } else if (entity instanceof Jumpship) {
                // Tech needs are handled by the crew directly.
                jumpshipCount++;
                updateBayCount((Jumpship) entity);
                updateDockingCollarCount((Jumpship) entity);
            } else if (entity instanceof Aero) {
                fighterCount++;
                if (entity instanceof ConvFighter) {
                    updateTotalSkill(u.getCrew(), UnitType.CONV_FIGHTER);
                } else {
                    updateTotalSkill(u.getCrew(), UnitType.AERO);
                }
            } else if (entity instanceof Protomech) {
                protoCount++;
                updateTotalSkill(u.getCrew(), UnitType.PROTOMEK);
            } else if (entity instanceof LargeSupportTank) {
                // Tech needs are handled by the crew directly.
                superHeavyVeeCount++;
                updateTotalSkill(u.getCrew(), UnitType.TANK);
            } else if (entity instanceof Tank) {
                if (entity.getWeight() <= 50f) {
                    lightVeeCount++;
                } else {
                    heavyVeeCount++;
                }
                if (entity instanceof VTOL) {
                    updateTotalSkill(u.getCrew(), UnitType.VTOL);
                } else {
                    updateTotalSkill(u.getCrew(), UnitType.TANK);
                }
            } else if (entity instanceof BattleArmor) {
                int personnel = ((BattleArmor) entity).getSquadN() * ((BattleArmor) entity).getSquadSize();
                battleArmorCount += personnel;
                updateTotalSkill(u.getCrew(), UnitType.BATTLE_ARMOR);
            } else if (entity instanceof Infantry) {
                int personnel = ((Infantry) entity).getSquadN() * ((Infantry) entity).getSquadSize();
                infantryCount += personnel;
                updateTotalSkill(u.getCrew(), UnitType.INFANTRY);
            }
        }
    }

    private void updateTotalSkill(List<Person> crew, int unitType) {
        if (crew == null || crew.isEmpty()) {
            return;
        }

        int totalGunnery = 0;
        int totalPilot = 0;
        boolean hasPilot = false;
        int level;
        Skill skill;

        for (Person p : crew) {
            switch (unitType) {
                case UnitType.MEK:
                    skill = p.getSkill(SkillType.S_GUN_MECH);
                    totalGunnery += skill == null ? 0 : skill.getLevel();
                    skill = p.getSkill(SkillType.S_PILOT_MECH);
                    level = skill == null ? 10 : skill.getLevel();
                    if (!hasPilot || level < totalPilot) {
                        totalPilot = level;
                        hasPilot = true;
                    }
                    break;
                case UnitType.WARSHIP:
                case UnitType.DROPSHIP:
                    skill = p.getSkill(SkillType.S_GUN_SPACE);
                    totalGunnery += skill == null ? 0 : skill.getLevel();
                    skill = p.getSkill(SkillType.S_PILOT_SPACE);
                    level = skill == null ? 10 : skill.getLevel();
                    if (!hasPilot || level < totalPilot) {
                        totalPilot = level;
                        hasPilot = true;
                    }
                    break;
                case UnitType.CONV_FIGHTER:
                    skill = p.getSkill(SkillType.S_GUN_JET);
                    totalGunnery += skill == null ? 0 : skill.getLevel();
                    skill = p.getSkill(SkillType.S_PILOT_JET);
                    level = skill == null ? 0 : skill.getLevel();
                    if (!hasPilot || level < totalPilot) {
                        totalPilot = level;
                        hasPilot = true;
                    }
                    break;
                case UnitType.AERO:
                    skill = p.getSkill(SkillType.S_GUN_AERO);
                    totalGunnery += skill == null ? 0 : skill.getLevel();
                    skill = p.getSkill(SkillType.S_PILOT_AERO);
                    level = skill == null ? 0 : skill.getLevel();
                    if (!hasPilot || level < totalPilot) {
                        totalPilot = level;
                        hasPilot = true;
                    }
                    break;
                case UnitType.VTOL:
                    skill = p.getSkill(SkillType.S_GUN_VEE);
                    totalGunnery += skill == null ? 0 : skill.getLevel();
                    skill = p.getSkill(SkillType.S_PILOT_VTOL);
                    level = skill == null ? 10 : skill.getLevel();
                    if (!hasPilot || level < totalPilot) {
                        totalPilot = level;
                        hasPilot = true;
                    }
                    break;
                case UnitType.TANK:
                    skill = p.getSkill(SkillType.S_GUN_VEE);
                    totalGunnery += skill == null ? 0 : skill.getLevel();
                    skill = p.getSkill(SkillType.S_PILOT_GVEE);
                    level = skill == null ? 10 : skill.getLevel();
                    if (!hasPilot || level < totalPilot) {
                        totalPilot = level;
                        hasPilot = true;
                    }
                    break;
                case UnitType.PROTOMEK:
                    skill = p.getSkill(SkillType.S_GUN_PROTO);
                    totalGunnery += skill == null ? 0 : skill.getLevel();
                    break;
                case UnitType.BATTLE_ARMOR:
                    skill = p.getSkill(SkillType.S_GUN_BA);
                    totalGunnery += skill == null ? 0 : skill.getLevel();
                    skill = p.getSkill(SkillType.S_ANTI_MECH);
                    if (skill != null) {
                        totalPilot += skill.getLevel();
                        hasPilot = true;
                    }
                    break;
                case UnitType.INFANTRY:
                    skill = p.getSkill(SkillType.S_SMALL_ARMS);
                    totalGunnery += skill == null ? 0 : skill.getLevel();
                    skill = p.getSkill(SkillType.S_ANTI_MECH);
                    if (skill != null) {
                        totalPilot += skill.getLevel();
                        hasPilot = true;
                    }
                    break;
            }
        }
        BigDecimal averageGunnery = new BigDecimal(totalGunnery)
                .divide(new BigDecimal(crew.size()), 3, RoundingMode.HALF_UP);
        BigDecimal averagePilot;
        if (UnitType.BATTLE_ARMOR == unitType || UnitType.INFANTRY == unitType) {
            averagePilot = new BigDecimal(totalPilot).divide(new BigDecimal(crew.size()), 3, RoundingMode.HALF_UP);
        } else {
            averagePilot = new BigDecimal(totalPilot);
        }

        BigDecimal skillLevel = averageGunnery;
        if (hasPilot) {
            skillLevel = skillLevel.add(averagePilot);
        } else {
            // Assume a piloting equal to Gunnery +1.
            skillLevel = skillLevel.add(averageGunnery).add(BigDecimal.ONE);
        }

        totalSkillLevels = totalSkillLevels.add(skillLevel);
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
        return totalCombatUnits;
    }

    @Override
    protected BigDecimal calcAverageExperience() {
        int totalCombatUnits = getTotalCombatUnits();

        if (totalCombatUnits == 0) {
            return BigDecimal.ZERO;
        }

        return totalSkillLevels.divide(new BigDecimal(totalCombatUnits), 2, BigDecimal.ROUND_HALF_UP);
    }

    private void calcNeededTechs() {
        mechTechTeamsNeeded = mechCount;
        fighterTechTeamsNeeded = fighterCount;
        protoTechTeamsNeeded = new BigDecimal(protoCount).divide(new BigDecimal(5), 0, RoundingMode.HALF_UP).intValue();
        veeTechTeamsNeeded = lightVeeCount;
        battleArmorTechTeamsNeeded = new BigDecimal(battleArmorCount)
                .divide(new BigDecimal(5), 0, RoundingMode.HALF_UP)
                .intValue();
        infantryTechTeamsNeeded = new BigDecimal(infantryCount)
                .divide(new BigDecimal(84), 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private void updatePersonnelCounts() {
        nonAdminPersonnelCount = 0;
        nonTransportPersonnelCount = 0;
        List<Person> personnelList = new ArrayList<Person>(campaign.getPersonnel());
        for (Person p : personnelList) {
            Unit unit = campaign.getUnit(p.getUnitId());
            if ((unit == null) || !((unit.getEntity() instanceof Dropship) || unit.getEntity() instanceof Jumpship)) {
                nonTransportPersonnelCount++;
            }

            if (p.isAdmin()) {
                continue;
            }
            nonAdminPersonnelCount++;
        }
        nonAdminPersonnelCount += campaign.getAstechPool();
    }

    private void calcNeededAdmins() {
        adminsNeeded = new BigDecimal(nonAdminPersonnelCount).divide(BigDecimal.TEN, 0, RoundingMode.UP).intValue();
    }

    // todo Combat Personnel (up to 1/4 total) may be assigned double-duty and count as 1/3 of a tech.
    // todo Combat Personnel (up to 1/4 total) may be assigned double-duty and count as 1/3 of an admin.
    // todo Distinguish between Merc and Government personnel (1/2 admin needs for gov).

    @Override
    protected void initValues() {
        super.initValues();

        mechCount = 0;
        protoCount = 0;
        lightVeeCount = 0;
        battleArmorCount = 0;
        infantryCount = 0;
        fighterCount = 0;
        dropshipCount = 0;
        jumpshipCount = 0;
        dockingCollarCount = 0;

        updateUnitCounts();
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

        final BigDecimal eliteThreshold = new BigDecimal("4.99");
        final BigDecimal vetThreshold = new BigDecimal("8.01");
        final BigDecimal regThreshold = new BigDecimal("10.99");

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
        } else if (SkillType.getExperienceLevelName(SkillType.EXP_GREEN).equalsIgnoreCase(level)) {
            return 0;
        } else if (SkillType.getExperienceLevelName(SkillType.EXP_REGULAR).equalsIgnoreCase(level)) {
            return 5;
        } else if (SkillType.getExperienceLevelName(SkillType.EXP_VETERAN).equalsIgnoreCase(level)) {
            return 10;
        }
        return 20;
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
        // ToDo MHQ would need  to support: Combat Sense, Connections, Reputation, Wealth, High CHA, Combat Paralysis
        // ToDo                             Unlucky & Low CHA.

        int commanderValue = skillTotal; // ToDo + positiveTraits - negativeTraits.

        return commanderValue > 0 ? commanderValue : 1;
    }

    @Override
    public String getUnitRating() {
        // Interstellar Ops Beta does not use letter-grades.
        return getModifier() + " (" + calculateUnitRatingScore() + ")";
    }

    @Override
    public int getUnitRating(int score) {
        // Interstellar Ops Beta does not use letter-grades.
        return 0;
    }

    @Override
    public String getUnitRatingName(int rating) {
        // Interstellar Ops Beta does not use letter-grades.
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
        boolean doubleCapacity = true;
        boolean fullCapacity = true;
        if (mechBayCount < mechCount) {
            fullCapacity = false;
            doubleCapacity = false;
        } else if (mechBayCount < mechCount * 2) {
            doubleCapacity = false;
        }
        if (protoBayCount < protoCount) {
            fullCapacity = false;
            doubleCapacity = false;
        } else if (protoBayCount < protoCount * 2) {
            doubleCapacity = false;
        }
        if (lightVeeBayCount < lightVeeCount) {
            fullCapacity = false;
            doubleCapacity = false;
        } else if (lightVeeBayCount < lightVeeCount * 2) {
            doubleCapacity = false;
        }
        if (heavyVeeBayCount < heavyVeeCount) {
            fullCapacity = false;
            doubleCapacity = false;
        } else if (heavyVeeBayCount < heavyVeeCount * 2) {
            doubleCapacity = false;
        }
        if (fighterBayCount < fighterCount) {
            fullCapacity = false;
            doubleCapacity = false;
        } else if (fighterBayCount < fighterCount * 2) {
            doubleCapacity = false;
        }
        if ((baBayCount) < battleArmorCount / 5) {
            fullCapacity = false;
            doubleCapacity = false;
        } else if ((baBayCount * 2) < 2 * battleArmorCount / 5) {
            doubleCapacity = false;
        }
        if (infantryBayCount < infantryCount / 28) {
            fullCapacity = false;
            doubleCapacity = false;
        } else if (infantryBayCount < infantryCount / 14) {
            doubleCapacity = false;
        }

        //Find the percentage of units that are transported.
        if (doubleCapacity) {
            totalValue += 10;
        } else if (fullCapacity) {
            totalValue += 5;
        } else if (dropshipCount < 1) {
            totalValue -= 10;
        } else {
            totalValue -= 5;
        }

        // ToDo Calculate transport needs and capacity for support personnel.
        // According to InterStellar Ops Beta, this will require tracking bay personnel & passenger quarters.

        if ((jumpshipCount + warshipCount) > 0) {
            totalValue += 10;
        }
        if (dockingCollarCount >= dropshipCount) {
            totalValue += 5;
        }

        return totalValue;
    }

    private int calcTechSupportValue() {
        int totalValue = 0;
        totalTechTeams = 0;
        astechTeams = 0;
        mechTechTeams = 0;
        fighterTechTeams = 0;
        veeTechTeams = 0;
        baTechTeams = 0;
        generalTechTeams = 0;

        // How many astech teams do we have?
        astechTeams = campaign.getNumberAstechs() / 6;

        for (Person tech : campaign.getTechs()) {
            // If we're out of astech teams, the rest of the techs are unsupporeted and don't count.
            if (astechTeams <= 0) {
                break;
            }

            if (tech.getSkill(SkillType.S_TECH_MECH) != null) {
                mechTechTeams++;
                astechTeams--;
            } else if (tech.getSkill(SkillType.S_TECH_AERO) != null) {
                fighterTechTeams++;
                astechTeams--;
            } else if (tech.getSkill(SkillType.S_TECH_MECHANIC) != null) {
                veeTechTeams++;
                astechTeams--;
            } else if (tech.getSkill(SkillType.S_TECH_BA) != null) {
                baTechTeams++;
                astechTeams--;
            } else {
                generalTechTeams++;
                astechTeams--;
            }
        }

        boolean techShortage = false;
        if (mechTechTeamsNeeded > mechTechTeams) {
            techShortage = true;
        }
        if (fighterTechTeamsNeeded > fighterTechTeams) {
            techShortage = true;
        }
        if (veeTechTeamsNeeded > veeTechTeams) {
            techShortage = true;
        }
        if (battleArmorTechTeamsNeeded > baTechTeams) {
            techShortage = true;
        }
        if ((protoTechTeamsNeeded + infantryTechTeamsNeeded) > generalTechTeams) {
            techShortage = true;
        }

        totalTechTeams = mechTechTeams + fighterTechTeams + veeTechTeams + baTechTeams + generalTechTeams;
        int totalTechTeamsNeeded = mechTechTeamsNeeded + fighterTechTeamsNeeded + veeTechTeamsNeeded +
                                   battleArmorTechTeamsNeeded + protoTechTeamsNeeded + infantryTechTeamsNeeded;
        supportPercent = BigDecimal.ZERO;
        if (totalTechTeams != 0) {
            supportPercent = new BigDecimal(totalTechTeams)
                    .divide(new BigDecimal(totalTechTeamsNeeded), 5, BigDecimal.ROUND_HALF_UP)
                    .multiply(HUNDRED);
        }

        if (techShortage) {
            totalValue -= 5;
        } else {
            if (supportPercent.compareTo(new BigDecimal(200)) > 0) {
                totalValue += 15;
            } else if (supportPercent.compareTo(new BigDecimal(175)) > 0) {
                totalValue += 10;
            } else if (supportPercent.compareTo(new BigDecimal(149)) > 0) {
                totalValue += 5;
            }
        }

        return totalValue;
    }

    private int calcAdminSupportValue() {
        if (adminsNeeded > campaign.getAdmins().size()) {
            return -5;
        }
        return 0;
    }

    private int calcLargeCraftSupportValue() {
        List<String> craftWithoutCrew = new ArrayList<String>();
        for (Unit u : campaign.getUnits()) {
            if (!(u.getEntity() instanceof Dropship) && !(u.getEntity() instanceof Jumpship)) {
                continue;
            }
            if (u.getActiveCrew().size() < u.getFullCrewSize()) {
                craftWithoutCrew.add(u.getName());
            }
        }
        return craftWithoutCrew.size() == 0 ? 0 : -5;
    }

    @Override
    public int getSupportValue() {
        int value = calcTechSupportValue();
        value += calcAdminSupportValue();
        value += calcLargeCraftSupportValue();
        return value;
    }

    @Override
    public int getTechValue() {
        // Interstellar Ops Beta rules do not give a tech level bonus.
        return 0;
    }

    @Override
    public BigDecimal getTransportPercent() {
        return BigDecimal.ZERO;
    }

    public int getFinancialValue() {
        return campaign.getFinances().isInDebt() ? -10 : 0;
    }

    // ToDo: MekHQ doesn't currently support recording crimes.
    public int getCrimesPenalty() {
        return 0;
    }

    // ToDo MekHQ doesn't current apply completion dates to missions.
    public int getIdleTimeModifier() {
        return 0;
    }

    @Override
    public int getModifier() {
        BigDecimal reputation = new BigDecimal(calculateUnitRatingScore());
        return reputation.divide(BigDecimal.TEN, 0, RoundingMode.DOWN).intValue();
    }

    @Override
    public String getDetails() {
        initValues();
        StringBuffer sb = new StringBuffer("Unit Reputation:                ").append(calculateUnitRatingScore())
                                                                              .append("\n");
        sb.append("    Method: Interstellar Ops Beta\n\n");

        sb.append("Experience:                      ").append(getExperienceValue()).append("\n");
        sb.append("    Average Experience:   ").append(getExperienceLevelName(calcAverageExperience())).append("\n\n");

        sb.append("Command:                        ").append(getCommanderValue()).append("\n");
        sb.append("    Leadership:           ").append(getCommanderSkill(SkillType.S_LEADER)).append("\n");
        sb.append("    Negotiation:          ").append(getCommanderSkill(SkillType.S_NEG)).append("\n");
        sb.append("    Strategy:             ").append(getCommanderSkill(SkillType.S_STRATEGY)).append("\n");
        sb.append("    Tactics:              ").append(getCommanderSkill(SkillType.S_TACTICS)).append("\n\n");

        sb.append("Combat Record:                  ").append(getCombatRecordValue()).append("\n");
        sb.append("    Successful Missions:  ").append(getSuccessCount()).append("\n");
        sb.append("    Failed Missions:      ").append(getFailCount()).append("\n");
        sb.append("    Contract Breaches:    ").append(getBreachCount()).append("\n\n");

        sb.append("Transportation:                 ").append(getTransportValue()).append("\n");
        sb.append("    Mech Bays:            ").append(getMechCount()).append(" needed /").append(mechBayCount)
          .append(" available\n");
        sb.append("    Fighter Bays:         ").append(getFighterCount()).append(" needed /").append(fighterBayCount)
          .append(" available\n");
        sb.append("    Protomech Bays:       ").append(getProtoCount()).append(" needed /").append(protoBayCount)
          .append(" available\n");
        sb.append("    Light Vehicle Bays:   ").append(lightVeeCount).append(" needed /").append(lightVeeBayCount)
          .append(" available\n");
        sb.append("    Heavy Vehicle Bays:   ").append(heavyVeeCount).append(" needed /").append(heavyVeeBayCount)
          .append(" available\n");
        sb.append("    BA Bays:              ").append(getBattleArmorCount() / 5).append(" needed /").append(baBayCount)
          .append(" available\n");
        sb.append("    Infantry Bays:        ").append(getInfantryCount() / 28).append(" needed /").append
                (infantryBayCount)
          .append(" available\n");
        sb.append("    Docking Collars:      ").append(dropshipCount).append(" needed /").append(dockingCollarCount)
          .append(" available\n");
        sb.append("    Jump-Capable Ships?   ").append(jumpshipCount + warshipCount > 0 ? "Yes\n" : "No\n");
        sb.append("\n");

        sb.append("Support:                        ").append(getSupportValue()).append("\n");
        sb.append("    Tech Support:\n");
        sb.append("        Astech Teams:     ").append(totalTechTeams).append("\n");
        sb.append("        Mech Techs:       ").append(mechTechTeamsNeeded).append(" needed /").append(mechTechTeams)
          .append(" available\n");
        sb.append("        Fighter Techs:    ").append(fighterTechTeamsNeeded).append(" needed /")
          .append(fighterTechTeams).append(" available\n");
        sb.append("        Tank Techs:       ").append(veeTechTeamsNeeded).append(" needed /").append(veeTechTeams)
          .append(" available\n");
        sb.append("        BA Techs:         ").append(battleArmorTechTeamsNeeded).append(" needed /")
          .append(baTechTeams).append(" available\n");
        sb.append("        Inf/Proto Techs:  ").append(infantryTechTeamsNeeded + protoTechTeamsNeeded)
          .append(" needed /").append(generalTechTeams).append(" available\n");
        sb.append("            NOTE:  MHQ Does not currently support Infantry and Protomech specific techs.\n");
        sb.append("    Admin Support:        ").append(adminsNeeded).append(" needed /")
          .append(campaign.getAdmins().size()).append(" available\n");
        sb.append("    Large Craft Support:\n");
        for (String s : craftWithoutCrew) {
            sb.append("        ").append(s).append(" short crew.\n");
        }
        sb.append("\n");

        sb.append("Financial:                      ").append(getFinancialValue()).append("\n");
        sb.append("    In Debt?              ").append(campaign.getFinances().isInDebt() ? "Yes\n" : "No\n");

        sb.append("Criminal Activity:              0 (MHQ does not currently track criminal activity.)\n");

        sb.append("Inativity Modifier:             0 (MHQ does not track end dates for missions/contracts.)");

        return new String(sb);
    }

    @Override
    public String getHelpText() {
        return "Method: Interstellar Ops Beta\n" +
               "An attempt to match the Interstellar Ops Beta method for calculating the Reputation as closely as " +
               "possible.\n" +
               "Known differences include the following:\n" +
               "+ Command: Does not incorporate any positive or negative traits from AToW or BRPG3." +
               "+ Transportation: Transportation needs of Support Personnel are not accounted for as MHQ does not " +
               "track " +
               "Bay Personnel or Passenger Quarters.\n" +
               "+ Support: MHQ Does not currently support Infantry and Protomech specific techs." +
               "+ Criminal Activity: MHQ does not currently track criminal activity." +
               "+ Inactivity: MHQ does not track end dates for missions/contracts.";
    }
}
