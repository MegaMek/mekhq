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

    // Tech Support & Admins.
    private int mechTechTeamsNeeded = 0;
    private int protoTechTeamsNeeded = 0;
    private int veeTechTeamsNeeded = 0;
    private int battleArmorTechTeamsNeeded = 0;
    private int infantryTechTeamsNeeded = 0;
    private int fighterTechTeamsNeeded = 0;
    private int adminsNeeded = 0;

    private int totalTechTeams = 0;
    private int mechTechTeams = 0;
    private int fighterTechTeams = 0;
    private int veeTechTeams = 0;
    private int baTechTeams = 0;
    private int generalTechTeams = 0; // ToDo: Should Protomech & Infantry techs be counted as separate skills?
    private List<String> craftWithoutCrew = new ArrayList<>();

    public InterstellarOpsReputation(Campaign campaign) {
        super(campaign);
    }

    public int getDropshipCount() {
        return super.getDropshipCount();
    }

    protected int getNonAdminPersonnelCount() {
        return nonAdminPersonnelCount;
    }

    protected int getAdminsNeeded() {
        return adminsNeeded;
    }

    protected int getMechCount() {
        return super.getMechCount();
    }

    protected int getProtoCount() {
        return super.getProtoCount();
    }

    protected int getVeeCount() {
        return getLightVeeCount();
    }

    protected int getBattleArmorCount() {
        return super.getBattleArmorCount();
    }

    protected int getInfantryCount() {
        return super.getInfantryCount();
    }

    protected int getFighterCount() {
        return super.getFighterCount();
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
        setTotalSkillLevels(BigDecimal.ZERO);

        List<Unit> unitList = new ArrayList<>(getCampaign().getUnits());
        for (Unit u : unitList) {
            if (u.isMothballed()) {
                continue;
            }

            Person p = u.getCommander();
            if (p != null) {
                getCommanderList().add(p);
            }

            Entity entity = u.getEntity();
            if (entity instanceof Mech) {
                setMechCount(super.getMechCount() + 1);
                updateTotalSkill(u.getCrew(), UnitType.MEK);
            } else if (entity instanceof Dropship) {
                // Tech needs are handled by the crew directly.
                setDropshipCount(super.getDropshipCount() + 1);
                updateTotalSkill(u.getCrew(), UnitType.DROPSHIP);
                updateBayCount((Dropship) entity);
            } else if (entity instanceof Warship) {
                // Tech needs are handled by the crew directly.
                setWarshipCount(getWarshipCount() + 1);
                updateTotalSkill(u.getCrew(), UnitType.WARSHIP);
                updateBayCount((Warship) entity);
                updateDockingCollarCount((Warship) entity);
            } else if (entity instanceof Jumpship) {
                // Tech needs are handled by the crew directly.
                setJumpshipCount(getJumpshipCount() + 1);
                updateBayCount((Jumpship) entity);
                updateDockingCollarCount((Jumpship) entity);
            } else if (entity instanceof Aero) {
                setFighterCount(super.getFighterCount() + 1);
                if (entity instanceof ConvFighter) {
                    updateTotalSkill(u.getCrew(), UnitType.CONV_FIGHTER);
                } else {
                    updateTotalSkill(u.getCrew(), UnitType.AERO);
                }
            } else if (entity instanceof Protomech) {
                setProtoCount(super.getProtoCount() + 1);
                updateTotalSkill(u.getCrew(), UnitType.PROTOMEK);
            } else if (entity instanceof LargeSupportTank) {
                // Tech needs are handled by the crew directly.
                setSuperHeavyVeeCount(getSuperHeavyVeeCount() + 1);
                updateTotalSkill(u.getCrew(), UnitType.TANK);
            } else if (entity instanceof Tank) {
                if (entity.getWeight() <= 50f) {
                    setLightVeeCount(getLightVeeCount() + 1);
                } else {
                    setHeavyVeeCount(getHeavyVeeCount() + 1);
                }
                if (entity instanceof VTOL) {
                    updateTotalSkill(u.getCrew(), UnitType.VTOL);
                } else {
                    updateTotalSkill(u.getCrew(), UnitType.TANK);
                }
            } else if (entity instanceof BattleArmor) {
                int personnel = ((BattleArmor) entity).getSquadN() * ((BattleArmor) entity).getSquadSize();
                setBattleArmorCount(super.getBattleArmorCount() + personnel);
                updateTotalSkill(u.getCrew(), UnitType.BATTLE_ARMOR);
            } else if (entity instanceof Infantry) {
                int personnel = ((Infantry) entity).getSquadN() * ((Infantry) entity).getSquadSize();
                setInfantryCount(super.getInfantryCount() + personnel);
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
        return totalCombatUnits;
    }

    @Override
    protected BigDecimal calcAverageExperience() {
        int totalCombatUnits = getTotalCombatUnits();

        if (totalCombatUnits == 0) {
            return BigDecimal.ZERO;
        }

        return getTotalSkillLevels().divide(new BigDecimal(totalCombatUnits), 2, BigDecimal.ROUND_HALF_UP);
    }

    private void calcNeededTechs() {
        setMechTechTeamsNeeded(super.getMechCount());
        setFighterTechTeamsNeeded(super.getFighterCount());
        setProtoTechTeamsNeeded(new BigDecimal(super.getProtoCount()).divide(new BigDecimal(5), 0, RoundingMode.HALF_UP).intValue());
        setVeeTechTeamsNeeded((getLightVeeCount() + getHeavyVeeCount()));
        setBattleArmorTechTeamsNeeded(new BigDecimal(super.getBattleArmorCount())
                .divide(new BigDecimal(5), 0, RoundingMode.HALF_UP)
                .intValue());
        setInfantryTechTeamsNeeded(new BigDecimal(super.getInfantryCount())
                .divide(new BigDecimal(84), 0, RoundingMode.HALF_UP)
                .intValue());
    }

    private void updatePersonnelCounts() {
        setNonAdminPersonnelCount(0);
        List<Person> personnelList = new ArrayList<>(getCampaign().getPersonnel());
        for (Person p : personnelList) {
            if (p.isAdmin()) {
                continue;
            }
            setNonAdminPersonnelCount(getNonAdminPersonnelCount() + 1);
        }
        setNonAdminPersonnelCount(getNonAdminPersonnelCount() + getCampaign().getAstechPool());
    }

    private void calcNeededAdmins() {
        setAdminsNeeded(new BigDecimal(getNonAdminPersonnelCount()).divide(BigDecimal.TEN, 0, RoundingMode.UP).intValue());
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
        setJumpshipCount(0);
        setDockingCollarCount(0);

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
            return 5;
        } else if (SkillType.getExperienceLevelName(SkillType.EXP_REGULAR).equalsIgnoreCase(level)) {
            return 10;
        } else if (SkillType.getExperienceLevelName(SkillType.EXP_VETERAN).equalsIgnoreCase(level)) {
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
        int heavyVeeBays = getHeavyVeeBayCount();
        if (getMechBayCount() < super.getMechCount()) {
            fullCapacity = false;
            doubleCapacity = false;
        } else if (getMechBayCount() < super.getMechCount() * 2) {
            doubleCapacity = false;
        }
        if (getProtoBayCount() < super.getProtoCount()) {
            fullCapacity = false;
            doubleCapacity = false;
        } else if (getProtoBayCount() < super.getProtoCount() * 2) {
            doubleCapacity = false;
        }
        if (getHeavyVeeBayCount() < getHeavyVeeCount()) {
            fullCapacity = false;
            doubleCapacity = false;
        } else if (getHeavyVeeBayCount() < getHeavyVeeCount() * 2) {
            doubleCapacity = false;
        }
        heavyVeeBays -= getHeavyVeeBayCount();
        int lightVeeBays = getLightVeeBayCount() + heavyVeeBays;
        if (getLightVeeBayCount() < lightVeeBays) {
            fullCapacity = false;
            doubleCapacity = false;
        } else if (getLightVeeBayCount() < lightVeeBays * 2) {
            doubleCapacity = false;
        }
        if (getFighterBayCount() < super.getFighterCount()) {
            fullCapacity = false;
            doubleCapacity = false;
        } else if (getFighterBayCount() < super.getFighterCount() * 2) {
            doubleCapacity = false;
        }
        if ((getBaBayCount()) < super.getBattleArmorCount() / 5) {
            fullCapacity = false;
            doubleCapacity = false;
        } else if ((getBaBayCount() * 2) < 2 * super.getBattleArmorCount() / 5) {
            doubleCapacity = false;
        }
        if (getInfantryBayCount() < super.getInfantryCount() / 28) {
            fullCapacity = false;
            doubleCapacity = false;
        } else if (getInfantryBayCount() < super.getInfantryCount() / 14) {
            doubleCapacity = false;
        }

        //Find the percentage of units that are transported.
        if (doubleCapacity) {
            totalValue += 10;
        } else if (fullCapacity) {
            totalValue += 5;
        } else if (super.getDropshipCount() < 1) {
            totalValue -= 10;
        } else {
            totalValue -= 5;
        }

        // ToDo Calculate transport needs and capacity for support personnel.
        // According to InterStellar Ops Beta, this will require tracking bay personnel & passenger quarters.

        if ((getJumpshipCount() + getWarshipCount()) > 0) {
            totalValue += 10;
        }
        if (getDockingCollarCount() >= super.getDropshipCount()) {
            totalValue += 5;
        }

        return totalValue;
    }

    protected int calcTechSupportValue() {
        int totalValue = 0;
        setTotalTechTeams(0);
        int astechTeams;
        setMechTechTeams(0);
        setFighterTechTeams(0);
        setVeeTechTeams(0);
        setBaTechTeams(0);
        setGeneralTechTeams(0);

        // How many astech teams do we have?
        astechTeams = getCampaign().getNumberAstechs() / 6;

        for (Person tech : getCampaign().getTechs()) {
            // If we're out of astech teams, the rest of the techs are unsupporeted and don't count.
            if (astechTeams <= 0) {
                break;
            }

            if (tech.getSkill(SkillType.S_TECH_MECH) != null) {
                setMechTechTeams(getMechTechTeams() + 1);
                astechTeams--;
            } else if (tech.getSkill(SkillType.S_TECH_AERO) != null) {
                setFighterTechTeams(getFighterTechTeams() + 1);
                astechTeams--;
            } else if (tech.getSkill(SkillType.S_TECH_MECHANIC) != null) {
                setVeeTechTeams(getVeeTechTeams() + 1);
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
        if (getFighterTechTeamsNeeded() > getFighterTechTeams()) {
            techShortage = true;
        }
        if (getVeeTechTeamsNeeded() > getVeeTechTeams()) {
            techShortage = true;
        }
        if (getBattleArmorTechTeamsNeeded() > getBaTechTeams()) {
            techShortage = true;
        }
        /*if ((getProtoTechTeamsNeeded() + getInfantryTechTeamsNeeded()) > getGeneralTechTeams()) {
            techShortage = true;
        }*/

        setTotalTechTeams(getMechTechTeams() + getFighterTechTeams() + getVeeTechTeams() + getBaTechTeams() + getGeneralTechTeams());
        int totalTechTeamsNeeded = getMechTechTeamsNeeded() + getFighterTechTeamsNeeded() + getVeeTechTeamsNeeded() +
                                   getBattleArmorTechTeamsNeeded() + getProtoTechTeamsNeeded() + getInfantryTechTeamsNeeded();
        setSupportPercent(BigDecimal.ZERO);
        if (totalTechTeamsNeeded != 0) {
            setSupportPercent(new BigDecimal(getTotalTechTeams())
                    .divide(new BigDecimal(totalTechTeamsNeeded), 5, BigDecimal.ROUND_HALF_UP)
                    .multiply(HUNDRED));
        }

        if (techShortage) {
            totalValue -= 5;
        } else {
            if (getSupportPercent().compareTo(new BigDecimal(200)) > 0) {
                totalValue += 15;
            } else if (getSupportPercent().compareTo(new BigDecimal(175)) > 0) {
                totalValue += 10;
            } else if (getSupportPercent().compareTo(new BigDecimal(149)) > 0) {
                totalValue += 5;
            }
        }

        return totalValue;
    }

    private int calcAdminSupportValue() {
        if (getAdminsNeeded() > getCampaign().getAdmins().size()) {
            return -5;
        }
        return 0;
    }

    private int calcLargeCraftSupportValue() {
        List<String> craftWithoutCrew = new ArrayList<>();
        for (Unit u : getCampaign().getUnits()) {
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
        return getCampaign().getFinances().isInDebt() ? -10 : 0;
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
        sb.append("    Mech Bays:            ").append(getMechCount()).append(" needed /").append(getMechBayCount())
          .append(" available\n");
        sb.append("    Fighter Bays:         ").append(getFighterCount()).append(" needed /").append(getFighterBayCount())
          .append(" available\n");
        sb.append("    Protomech Bays:       ").append(getProtoCount()).append(" needed /").append(getProtoBayCount())
          .append(" available\n");
        sb.append("    Light Vehicle Bays:   ").append(getLightVeeCount()).append(" needed /").append(getLightVeeBayCount())
          .append(" available\n");
        sb.append("    Heavy Vehicle Bays:   ").append(getHeavyVeeCount()).append(" needed /").append(getHeavyVeeBayCount())
          .append(" available\n");
        sb.append("    BA Bays:              ").append(getBattleArmorCount() / 5).append(" needed /").append(getBaBayCount())
          .append(" available\n");
        sb.append("    Infantry Bays:        ").append(getInfantryCount() / 28).append(" needed /").append
                (getInfantryBayCount())
          .append(" available\n");
        sb.append("    Docking Collars:      ").append(super.getDropshipCount()).append(" needed /").append(getDockingCollarCount())
          .append(" available\n");
        sb.append("    Jump-Capable Ships?   ").append(getJumpshipCount() + getWarshipCount() > 0 ? "Yes\n" : "No\n");
        sb.append("\n");

        sb.append("Support:                        ").append(getSupportValue()).append("\n");
        sb.append("    Tech Support:\n");
        sb.append("        Astech Teams:     ").append(getTotalTechTeams()).append("\n");
        sb.append("        Mech Techs:       ").append(getMechTechTeamsNeeded()).append(" needed /").append(getMechTechTeams())
          .append(" available\n");
        sb.append("        Fighter Techs:    ").append(getFighterTechTeamsNeeded()).append(" needed /")
          .append(getFighterTechTeams()).append(" available\n");
        sb.append("        Tank Techs:       ").append(getVeeTechTeamsNeeded()).append(" needed /").append(getVeeTechTeams())
          .append(" available\n");
        sb.append("        BA Techs:         ").append(getBattleArmorTechTeamsNeeded()).append(" needed /")
          .append(getBaTechTeams()).append(" available\n");
        sb.append("        Inf/Proto Techs:  ").append(getInfantryTechTeamsNeeded() + getProtoTechTeamsNeeded())
          .append(" needed /").append(getGeneralTechTeams()).append(" available\n");
        sb.append("            NOTE:  MHQ Does not currently support Infantry and Protomech specific techs.\n");
        sb.append("    Admin Support:        ").append(getAdminsNeeded()).append(" needed /")
          .append(getCampaign().getAdmins().size()).append(" available\n");
        sb.append("    Large Craft Support:\n");
        for (String s : getCraftWithoutCrew()) {
            sb.append("        ").append(s).append(" short crew.\n");
        }
        sb.append("\n");

        sb.append("Financial:                      ").append(getFinancialValue()).append("\n");
        sb.append("    In Debt?              ").append(getCampaign().getFinances().isInDebt() ? "Yes\n" : "No\n");

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

    protected void setNonAdminPersonnelCount(int nonAdminPersonnelCount) {
        this.nonAdminPersonnelCount = nonAdminPersonnelCount;
    }

    protected void setMechTechTeamsNeeded(int mechTechTeamsNeeded) {
        this.mechTechTeamsNeeded = mechTechTeamsNeeded;
    }

    protected void setProtoTechTeamsNeeded(int protoTechTeamsNeeded) {
        this.protoTechTeamsNeeded = protoTechTeamsNeeded;
    }

    protected void setVeeTechTeamsNeeded(int veeTechTeamsNeeded) {
        this.veeTechTeamsNeeded = veeTechTeamsNeeded;
    }

    protected void setBattleArmorTechTeamsNeeded(int battleArmorTechTeamsNeeded) {
        this.battleArmorTechTeamsNeeded = battleArmorTechTeamsNeeded;
    }

    protected void setInfantryTechTeamsNeeded(int infantryTechTeamsNeeded) {
        this.infantryTechTeamsNeeded = infantryTechTeamsNeeded;
    }

    protected void setFighterTechTeamsNeeded(int fighterTechTeamsNeeded) {
        this.fighterTechTeamsNeeded = fighterTechTeamsNeeded;
    }

    protected void setAdminsNeeded(int adminsNeeded) {
        this.adminsNeeded = adminsNeeded;
    }

    protected int getTotalTechTeams() {
        return totalTechTeams;
    }

    protected void setTotalTechTeams(int totalTechTeams) {
        this.totalTechTeams = totalTechTeams;
    }

    protected int getMechTechTeams() {
        return mechTechTeams;
    }

    protected void setMechTechTeams(int mechTechTeams) {
        this.mechTechTeams = mechTechTeams;
    }

    protected int getFighterTechTeams() {
        return fighterTechTeams;
    }

    protected void setFighterTechTeams(int fighterTechTeams) {
        this.fighterTechTeams = fighterTechTeams;
    }

    protected int getVeeTechTeams() {
        return veeTechTeams;
    }

    protected void setVeeTechTeams(int veeTechTeams) {
        this.veeTechTeams = veeTechTeams;
    }

    protected int getBaTechTeams() {
        return baTechTeams;
    }

    protected void setBaTechTeams(int baTechTeams) {
        this.baTechTeams = baTechTeams;
    }

    protected int getGeneralTechTeams() {
        return generalTechTeams;
    }

    protected void setGeneralTechTeams(int generalTechTeams) {
        this.generalTechTeams = generalTechTeams;
    }

    protected List<String> getCraftWithoutCrew() {
        return craftWithoutCrew;
    }

    protected void setCraftWithoutCrew(List<String> craftWithoutCrew) {
        this.craftWithoutCrew = craftWithoutCrew;
    }
}
