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
import java.util.Date;
import java.util.List;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Bay;
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
import mekhq.campaign.mission.Mission;
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

    // Combat Unit Skills.
    private BigDecimal totalSkill = BigDecimal.ZERO;

    public InterstellarOpsReputation(Campaign campaign) {
        super(campaign);
    }

    public int getDropshipCount() {
        return dropshipCount;
    }

    protected BigDecimal getTotalSkill() {
        return totalSkill;
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
        mechCount = 0;
        protoCount = 0;
        lightVeeCount = 0;
        battleArmorCount = 0;
        infantryCount = 0;
        fighterCount = 0;

        List<Unit> unitList = new ArrayList<Unit>(campaign.getUnits());
        for (Unit u : unitList) {
            if (u.isMothballed()) {
                continue;
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
                updateBayCount((Warship)entity);
                updateDockingCollarCount((Warship)entity);
            } else if (entity instanceof Jumpship) {
                // Tech needs are handled by the crew directly.
                jumpshipCount++;
                updateBayCount((Jumpship)entity);
                updateDockingCollarCount((Jumpship)entity);
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
                int personnel = ((BattleArmor)entity).getSquadN() * ((BattleArmor)entity).getSquadSize();
                battleArmorCount += personnel;
                updateTotalSkill(u.getCrew(), UnitType.BATTLE_ARMOR);
            } else if (entity instanceof Infantry) {
                int personnel = ((Infantry)entity).getSquadN() * ((Infantry)entity).getSquadSize();
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
                    totalGunnery += p.getSkill(SkillType.S_GUN_MECH).getLevel();
                    level = p.getSkill(SkillType.S_PILOT_MECH).getLevel();
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
                    level =  skill == null ? 10 : skill.getLevel();
                    if (!hasPilot || level < totalPilot) {
                        totalPilot = level;
                        hasPilot = true;
                    }
                    break;
                case UnitType.CONV_FIGHTER:
                    totalGunnery += p.getSkill(SkillType.S_GUN_JET).getLevel();
                    level = p.getSkill(SkillType.S_PILOT_JET).getLevel();
                    if (!hasPilot || level < totalPilot) {
                        totalPilot = level;
                        hasPilot = true;
                    }
                    break;
                case UnitType.AERO:
                    totalGunnery += p.getSkill(SkillType.S_GUN_AERO).getLevel();
                    level = p.getSkill(SkillType.S_PILOT_AERO).getLevel();
                    if (!hasPilot || level < totalPilot) {
                        totalPilot = level;
                        hasPilot = true;
                    }
                    break;
                case UnitType.VTOL:
                    skill = p.getSkill(SkillType.S_GUN_VEE);
                    totalGunnery += skill == null ? 0 : skill.getLevel();
                    skill = p.getSkill(SkillType.S_PILOT_VTOL);
                    level =  skill == null ? 10 : skill.getLevel();
                    if (!hasPilot || level < totalPilot) {
                        totalPilot = level;
                        hasPilot = true;
                    }
                    break;
                case UnitType.TANK:
                    skill = p.getSkill(SkillType.S_GUN_VEE);
                    totalGunnery += skill == null ? 0 : skill.getLevel();
                    skill = p.getSkill(SkillType.S_PILOT_GVEE);
                    level =  skill == null ? 10 : skill.getLevel();
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
        }

        totalSkill = totalSkill.add(skillLevel);
    }

    @Override
    protected BigDecimal calcAverageExperience() {
        int totalCombatUnits = getMechCount();
        totalCombatUnits += getFighterCount();
        totalCombatUnits += getProtoCount();
        totalCombatUnits += getVeeCount();
        totalCombatUnits += (getBattleArmorCount() / 5);
        totalCombatUnits += (getInfantryCount() / 28);
        totalCombatUnits += getDropshipCount();

        return totalSkill.divide(new BigDecimal(totalCombatUnits), 2, BigDecimal.ROUND_HALF_UP);
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

    protected void initRating() {
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
    public int getExperienceValue() {
        final BigDecimal eliteThreshold = new BigDecimal("4.99");
        final BigDecimal vetThreshold = new BigDecimal("8.01");
        final BigDecimal regThreshold = new BigDecimal("10.99");

        BigDecimal averageExp = calcAverageExperience();
        if (averageExp.compareTo(regThreshold) > 0) {
            return 5;
        } else if (averageExp.compareTo(vetThreshold) > 0) {
            return 10;
        } else if (averageExp.compareTo(eliteThreshold) > 0) {
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
        int skillTotal = commander.getSkill(SkillType.S_LEADER).getLevel();
        skillTotal += commander.getSkill(SkillType.S_TACTICS).getLevel();
        skillTotal += commander.getSkill(SkillType.S_STRATEGY).getLevel();
        skillTotal += commander.getSkill(SkillType.S_NEG).getLevel();

        // ToDo AToW Traits.
        // ToDo MHQ would need  to support: Combat Sense, Connections, Reputation, Wealth, High CHA, Combat Paralysis
        // ToDo                             Unlucky & Low CHA.

        int commanderValue = skillTotal; // ToDo + positiveTraits - negativeTraits.

        return commanderValue > 0 ? commanderValue : 1;
    }

    @Override
    public int getTransportValue() {
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
        if ((baBayCount) < battleArmorCount/5) {
            fullCapacity = false;
            doubleCapacity = false;
        } else if ((baBayCount * 2) < 2*battleArmorCount/5) {
            doubleCapacity = false;
        }
        if (infantryBayCount < infantryCount/28) {
            fullCapacity = false;
            doubleCapacity = false;
        } else if (infantryBayCount < infantryCount/14 ) {
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

        // How many astech teams do we have?
        int astechTeams = campaign.getNumberAstechs() / 6;
        int mechTechTeams = 0;
        int fighterTechTeams = 0;
        int veeTechTeams = 0;
        int baTechTeams = 0;
        int generalTechTeams = 0; // ToDo: Should Protomech & Infantry techs be counted as separate skills?

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

        if (techShortage) {
            totalValue -= 5;
        } else {
            int totalTechTeams = mechTechTeams + fighterTechTeams + veeTechTeams + baTechTeams + generalTechTeams;
            int totalTechTeamsNeeded = mechTechTeamsNeeded + fighterTechTeamsNeeded + veeTechTeamsNeeded +
                                       battleArmorTechTeamsNeeded + protoTechTeamsNeeded + infantryTechTeamsNeeded;
            BigDecimal percentExcess = new BigDecimal(totalTechTeams)
                    .divide(new BigDecimal(totalTechTeamsNeeded), 5, BigDecimal.ROUND_HALF_UP)
                    .multiply(HUNDRED);
            if (percentExcess.compareTo(new BigDecimal(200)) > 0) {
                totalValue += 15;
            } else if (percentExcess.compareTo(new BigDecimal(175)) > 0) {
                totalValue += 10;
            } else if (percentExcess.compareTo(new BigDecimal(149)) > 0) {
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
        for (Unit u : campaign.getUnits()) {
            if (!(u.getEntity() instanceof Dropship) && !(u.getEntity() instanceof Jumpship)) {
                continue;
            }
            if (u.getActiveCrew().size() < u.getFullCrewSize()) {
                return -5;
            }
        }
        return 0;
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
        return BigDecimal.ZERO;
    }

    @Override
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

    public int getReputationModifier() {
        BigDecimal reputation = new BigDecimal(calculateUnitRatingScore());
        return reputation.divide(BigDecimal.TEN, 0, RoundingMode.DOWN).intValue();
    }

    @Override
    public String getDetails() {
        return null;  //ToDo
    }

    @Override
    public String getHelpText() {
        return null;  //ToDo
    }
}
