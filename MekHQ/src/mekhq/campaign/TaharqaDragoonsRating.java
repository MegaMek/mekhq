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
package mekhq.campaign;

import megamek.common.ASFBay;
import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.BattleArmorBay;
import megamek.common.Bay;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.HeavyVehicleBay;
import megamek.common.Infantry;
import megamek.common.InfantryBay;
import megamek.common.Jumpship;
import megamek.common.LightVehicleBay;
import megamek.common.Mech;
import megamek.common.MechBay;
import megamek.common.Protomech;
import megamek.common.SmallCraftBay;
import megamek.common.Tank;
import megamek.common.Warship;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.Skill;
import mekhq.campaign.personnel.SkillType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

/**
 * @author Deric Page (deric (dot) page (at) usa.net)
 * @version %I% %G%
 * @since 3/12/2012
 */
public class TaharqaDragoonsRating extends AbstractDragoonsRating {

    public TaharqaDragoonsRating(Campaign campaign) {
        super(campaign);
    }

    private void updateSkillLevel(Unit u, BigDecimal value) {
        if (null != u.getEntity().getCrew()) {
            if (u.getEntity() instanceof Infantry || u.getEntity() instanceof Protomech) {
                totalSkillLevels = totalSkillLevels.add(value.multiply(new BigDecimal(u.getEntity().getCrew().getGunnery())));
            } else {
                totalSkillLevels = totalSkillLevels.add(value.multiply(
                        new BigDecimal(
                                (u.getEntity().getCrew().getGunnery() + u.getEntity().getCrew().getPiloting())/2)));
            }
        }
    }

    @Override
    protected void initValues(boolean reInitialize) {
        if (isInitialized() && !reInitialize) {
            return;
        }

        for (UUID uid : campaign.getForces().getAllUnits()) {
            Unit u = campaign.getUnit(uid);
            if (null == u) {
                continue;
            }

            Person p = u.getCommander();
            if (null != p) {
                commanderList.add(p);
            }

            if (!u.isRepairable()) {
                continue;
            }

            BigDecimal value = getUnitValue(u);
            numberUnits = numberUnits.add(value);

            updateAdvanceTechCount(u, value);

            updateSkillLevel(u, value);

            updateUnitCounts(u.getEntity());

            if (u.getEntity() instanceof Dropship) {
                updateBayCount((Dropship) u.getEntity());
            }

            updateJumpships(u.getEntity());
        }

        updateTechCounts();
        initialized = true;
    }

    private void updateJumpships(Entity en) {
        if (en instanceof Warship) {
            if (en.getDocks() > 0) {
                warhipWithDocsOwner = true;
            } else {
                warshipOwner = true;
            }
        } else if (en instanceof Jumpship) {
            jumpshipOwner = true;
        }
    }

    private void updateBayCount(Dropship ds) {
        for (Bay bay : ds.getTransportBays()) {
            if (bay instanceof MechBay) {
                numberMechBays += bay.getCapacity();
            } else if (bay instanceof BattleArmorBay) {
                numberBaBays += bay.getCapacity() * 4;
            } else if (bay instanceof InfantryBay) {
                numberInfBays += bay.getCapacity() * 28;
            } else if ((bay instanceof LightVehicleBay) || (bay instanceof HeavyVehicleBay)) {
                numberVeeBays += bay.getCapacity();
            } else if ((bay instanceof ASFBay) || (bay instanceof SmallCraftBay)) {
                numberAeroBays += bay.getCapacity();
            }
        }
    }

    private void updateTechCounts() {
        for (Person p : campaign.getTechs()) {
            switch (p.getPrimaryRole()) {
                case (Person.T_MECH_TECH):
                    mechTech = mechTech.add(BigDecimal.ONE);
                    break;
                case (Person.T_AERO_TECH):
                    aeroTech = aeroTech.add(BigDecimal.ONE);
                    break;
                case (Person.T_MECHANIC):
                    veeTech = veeTech.add(BigDecimal.ONE);
                    break;
                case (Person.T_BA_TECH):
                    baTech = baTech.add(BigDecimal.ONE);
                    break;
            }
            switch (p.getSecondaryRole()) {
                case (Person.T_MECH_TECH):
                    mechTech = mechTech.add(new BigDecimal("0.5"));
                    break;
                case (Person.T_AERO_TECH):
                    aeroTech = aeroTech.add(new BigDecimal("0.5"));
                    break;
                case (Person.T_MECHANIC):
                    veeTech = veeTech.add(new BigDecimal("0.5"));
                    break;
                case (Person.T_BA_TECH):
                    baTech = baTech.add(new BigDecimal("0.5"));
                    break;
            }
        }
    }

    private void updateUnitCounts(Entity en) {
        if (en instanceof Mech) {
            numberMech++;
        } else if (en instanceof Tank) {
            numberVee++;
        } else if ((en instanceof Aero) && !(en instanceof Dropship) && !(en instanceof Jumpship)) {
            numberAero++;
        } else if (en instanceof BattleArmor) {
            numberBa += ((Infantry)en).getSquadSize();
        } else if (en instanceof Infantry) {
            numberInf += ((Infantry)en).getSquadN() * ((Infantry)en).getSquadSize();
        }
    }

    @Override
    public int getExperienceValue() {
        if (getNumberUnits().compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        BigDecimal averageExperience = getTotalSkillLevels().divide(getNumberUnits(), PRECISION, HALF_EVEN);
        if (averageExperience.compareTo(new BigDecimal("5.5")) >= 1) {
            return 5;
        } else if (averageExperience.compareTo(new BigDecimal("4.0")) >= 1) {
            return 10;
        } else if (averageExperience.compareTo(new BigDecimal("2.5")) >= 1) {
            return 20;
        } else {
            return 40;
        }
    }

    @Override
    public int getCommanderValue() {
        if (getCommander() == null) {
            return 0;
        }

        int value = 0;

        Skill test = getCommander().getSkill(SkillType.S_TACTICS);
        if (null != test) {
            value += test.getExperienceLevel();
        }

        test = getCommander().getSkill(SkillType.S_LEADER);
        if (null != test) {
            value += test.getExperienceLevel();
        }

        test = getCommander().getSkill(SkillType.S_STRATEGY);
        if (null != test) {
            value += test.getExperienceLevel();
        }

        test = getCommander().getSkill(SkillType.S_NEG);
        if (null != test) {
            value += test.getExperienceLevel();
        }

        return value;
    }

    /**
     * Returns the number of aerospace units in excess of aero techs in the unit.
     * If there are more techs than aerospace units, a value of 0 is returned.
     *
     * @return
     */
    public BigDecimal getUnsupportedAero() {
        BigDecimal aeroRatio = new BigDecimal(numberAero).subtract(aeroTech);
        if (aeroRatio.compareTo(BigDecimal.ZERO) > 0)
            return aeroRatio;

        return BigDecimal.ZERO;
    }

    /**
     * Returns the number of vehicles in excess of mechanics in the unit.
     * If there are more mechanics than vehicles, a value of 0 is returned.
     *
     * @return
     */
    public BigDecimal getUnsupportedVee() {
        BigDecimal veeRatio = new BigDecimal(numberVee).subtract(veeTech);
        if (veeRatio.compareTo(BigDecimal.ZERO) > 0)
            return veeRatio;

        return BigDecimal.ZERO;
    }


    /**
     * Returns the number of battle armor units in excess of ba techs in the unit.
     * If there are more techs than battle armor units, a value of 0 is returned.
     *
     * @return
     */
    public BigDecimal getUnsupportedBa() {
        BigDecimal baRatio = new BigDecimal(numberBa).subtract(baTech);
        if (baRatio.compareTo(BigDecimal.ZERO) > 0)
            return baRatio;

        return BigDecimal.ZERO;
    }

    /**
     * Returns the number of mechs in excess of mech techs in the unit.  If there are more techs than mechs, a value
     * of 0 is returned.
     *
     * @return
     */
    public BigDecimal getUnsupportedMechs() {
        BigDecimal mechRatio = new BigDecimal(numberMech).subtract(mechTech);
        if (mechRatio.compareTo(BigDecimal.ZERO) > 0)
            return mechRatio;

        return BigDecimal.ZERO;
    }

    @Override
    public int getSupportValue() {
        //support rating
        //TODO: this is a bit tricky because the role of astechs changed and we
        //dont know what the role of admins will be in the StellarOps
        //for now just look at the percentage of units that could have a dedicated
        //tech

        //Calculate number of unsupported units.
        BigDecimal supportNeeds = getUnsupportedMechs();
        supportNeeds = supportNeeds.add(getUnsupportedAero());
        supportNeeds = supportNeeds.add(getUnsupportedBa());
        supportNeeds = supportNeeds.add(getUnsupportedVee());

        //Calculate the percentage of units that are supported.
        BigDecimal unsupportedPct = BigDecimal.ZERO;
        if (getNumberUnits().compareTo(BigDecimal.ZERO) != 0) {
            unsupportedPct = supportNeeds.divide(getNumberUnits(), PRECISION, HALF_EVEN);
        }
        supportPercent = BigDecimal.ONE.subtract(unsupportedPct);
        if (supportPercent.compareTo(BigDecimal.ONE) > 0)
            supportPercent = BigDecimal.ONE;
        supportPercent = supportPercent.multiply(new BigDecimal("100"));

        //Find out how far above 60% we are. If we're below 60%, return a value of 0.
        BigDecimal scoredSupport = supportPercent.subtract(new BigDecimal("60"));
        if (scoredSupport.compareTo(BigDecimal.ZERO) < 0) {
            return 0;
        }

        //Return the support value.
        scoredSupport = scoredSupport.divide(new BigDecimal("10"), 0, RoundingMode.DOWN);
        return scoredSupport.multiply(new BigDecimal("5")).intValue();
    }

    @Override
    public int getYearsInDebt() {
        return (getFinancialValue() < 0) ? 1 : 0;
    }

    @Override
    public int getFinancialValue() {
        if (campaign.getFinances().isInDebt()) {
            return -10;
        }
        return 0;
    }

    @Override
    public String getDetails() {
        StringBuffer sb = new StringBuffer("Dragoons Rating:                " + getDragoonRating(false) + "\n");
        sb.append("    Method: Default\n\n");

        sb.append("Quality:                        ").append(getExperienceValue()).append("\n");
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
        sb.append("    Dropship Capacity:    ").append(getTransportPercent().toPlainString()).append("%\n");
        sb.append("    Jumpship?             ").append(jumpshipOwner ? "Yes" : "No").append("\n");
        sb.append("    Warship w/out Dock?   ").append(warshipOwner ? "Yes" : "No").append("\n");
        sb.append("    Warship w/ Dock?      ").append(warhipWithDocsOwner ? "Yes" : "No").append("\n\n");

        sb.append("Technology:                     ").append(getTechValue()).append("\n");
        sb.append("    # Clan Units:         ").append(numberClan.toPlainString()).append("\n");
        sb.append("    # IS2 Units:          ").append(numberIS2.toPlainString()).append("\n");
        sb.append("    Total # Units:        ").append(numberUnits.toPlainString()).append("\n\n");

        sb.append("Support:                        ").append(getSupportValue()).append("\n");
        sb.append("    Unsupported Aero:     ").append(getUnsupportedAero().toPlainString()).append("\n");
        sb.append("    Unsupported BA:       ").append(getUnsupportedBa().toPlainString()).append("\n");
        sb.append("    Unsupported Mechs:    ").append(getUnsupportedMechs().toPlainString()).append("\n");
        sb.append("    Unsupported Vehicles: ").append(getUnsupportedVee().toPlainString()).append("\n\n");

        sb.append("Financial:                      ").append(getFinancialValue()).append("\n");
        sb.append("    Currently in Debt?    ").append((campaign.getFinances().isInDebt() ? "Yes" : "No")).append("\n");

        return new String(sb);
    }

    protected int calculateDragoonRatingScore(boolean recalc) {
        initValues(recalc);

        int score = 0;

        score += getExperienceValue();
        score += getCommanderValue();
        score += getCombatRecordValue();
        score += getSupportValue();
        score += getTransportValue();
        score += getTechValue();
        score += getFinancialValue();

        return score;
    }

    @Override
    public String getHelpText() {
        return "Method: Taharqa Dragoons Rating\n" +
                "Dragoon's Rating method introduced by Taharqa with version v0.1.11 (2011-12-09).";
    }
}
