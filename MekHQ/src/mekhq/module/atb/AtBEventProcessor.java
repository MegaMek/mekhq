/*
 * Copyright (c) 2018  - The MegaMek Team
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
package mekhq.module.atb;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import megamek.client.ratgenerator.MissionRole;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EntityWeightClass;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.UnitType;
import megamek.common.event.Subscribe;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.MarketNewPersonnelEvent;
import mekhq.campaign.event.NewDayEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.Transaction;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.campaign.universe.RandomFactionGenerator;
import mekhq.campaign.universe.UnitGeneratorParameters;

/**
 * Main engine of the Against the Bot campaign system.
 * 
 * @author Neoancient
 *
 */
public class AtBEventProcessor {
    
    private final Campaign campaign;
    
    public AtBEventProcessor(Campaign campaign) {
        this.campaign = campaign;
        MekHQ.registerHandler(this);
    }
    
    public void shutdown() {
        MekHQ.unregisterHandler(this);
    }
    
    @Subscribe
    public void handleNewDay(NewDayEvent ev) {
        // TODO: move code from Campaign here
        if (campaign.getPersonnelMarket().getPaidRecruitment()
                && campaign.getCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
            if (campaign.getFinances().debit(Money.of(100000), Transaction.C_MISC,
                    "Paid recruitment roll", campaign.getDate())) {
                doPaidRecruitment();
            } else {
                campaign.addReport("<html><font color=\"red\">Insufficient funds for paid recruitment.</font></html>");
            }
        }
    }

    private void doPaidRecruitment() {
        int mod;
        switch (campaign.getPersonnelMarket().getPaidRecruitType()) {
        case Person.T_MECHWARRIOR:
            mod = -2;
            break;
        case Person.T_INFANTRY:
            mod = 2;
            break;
        case Person.T_MECH_TECH:
        case Person.T_AERO_TECH:
        case Person.T_MECHANIC:
        case Person.T_BA_TECH:
        case Person.T_DOCTOR:
            mod = 1;
            break;
        default:
            mod = 0;
        }

        mod += campaign.getUnitRatingMod() - IUnitRating.DRAGOON_C;
        if (campaign.getFinances().isInDebt()) {
            mod -= 3;
        }

        Person adminHR = campaign.findBestInRole(Person.T_ADMIN_HR, SkillType.S_ADMIN);
        int adminHRExp = (adminHR == null)?SkillType.EXP_ULTRA_GREEN:adminHR.getSkill(SkillType.S_ADMIN).getExperienceLevel();
        mod += adminHRExp - 2;
        int q = 0;
        int r = Compute.d6(2) + mod;
        if (r > 15) {
            q = 6;
        } else if (r > 12) {
            q = 5;
        } else if (r > 10) {
            q = 4;
        } else if (r > 8) {
            q = 3;
        } else if (r > 5) {
            q = 2;
        } else if (r > 3) {
            q = 1;
        }
        for (int i = 0; i < q; i++) {
            Person p = campaign.newPerson(campaign.getPersonnelMarket().getPaidRecruitType());
            UUID id = UUID.randomUUID();
            p.setId(id);
            campaign.getPersonnelMarket().addPerson(p);
            addRecruitUnit(p);
        }
    }

    /**
     * Listens for new personnel to be added to the market and determines which should come with units.
     * @param ev
     */
    @Subscribe
    public void handlePersonnelMarket(MarketNewPersonnelEvent ev) {
        for (Person p : ev.getPersonnel()) {
            addRecruitUnit(p);
        }
    }
    
    private void addRecruitUnit(Person p) {
        final String METHOD_NAME = "addRecruitUnit(Person)"; //$NON-NLS-1$
        UnitGeneratorParameters params = new UnitGeneratorParameters();
        
        int unitType;
        switch (p.getPrimaryRole()) {
            case Person.T_MECHWARRIOR:
                unitType = UnitType.MEK;
                break;
            case Person.T_GVEE_DRIVER:
            case Person.T_VEE_GUNNER:
            case Person.T_VTOL_PILOT:
                return;
            case Person.T_AERO_PILOT:
                if (!campaign.getCampaignOptions().getAeroRecruitsHaveUnits()) {
                    return;
                }
                unitType = UnitType.AERO;
                break;
            case Person.T_INFANTRY:
                unitType = UnitType.INFANTRY;
                
                // infantry will have a 1/3 chance of being field guns
                if(Compute.d6() <= 2) {
                    params.getMissionRoles().add(MissionRole.FIELD_GUN);
                    params.getMovementModes().addAll(IUnitGenerator.ALL_INFANTRY_MODES);
                }
                
                break;
            case Person.T_BA:
                unitType = UnitType.BATTLE_ARMOR;
                break;
            case Person.T_PROTO_PILOT:
                unitType = UnitType.PROTOMEK;
                break;
            default:
                return;
        }

        int weight = -1;
        if (unitType == UnitType.MEK
                || unitType == UnitType.TANK
                || unitType == UnitType.AERO) {
            int roll = Compute.d6(2);
            if (roll < 8) {
                return;
            }
            if (roll < 10) {
                weight = EntityWeightClass.WEIGHT_LIGHT;
            } else if (roll < 12) {
                weight = EntityWeightClass.WEIGHT_MEDIUM;
            } else {
                weight = EntityWeightClass.WEIGHT_HEAVY;
            }
        }
        Entity en;

        String faction = getRecruitFaction(campaign);
        
        params.setFaction(faction);
        params.setUnitType(unitType);
        params.setWeightClass(weight);
        params.setYear(campaign.getCalendar().get(Calendar.YEAR));
        params.setQuality(IUnitRating.DRAGOON_F);
        
        MechSummary ms = campaign.getUnitGenerator().generate(params);
        if (null != ms) {
            if (Faction.getFaction(faction).isClan() && ms.getName().matches(".*Platoon.*")) {
                String name = "Clan " + ms.getName().replaceAll("Platoon", "Point");
                ms = MechSummaryCache.getInstance().getMech(name);
                System.out.println("looking for Clan infantry " + name);
            }
            try {
                en = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            } catch (EntityLoadingException ex) {
                en = null;
                MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                        "Unable to load entity: " + ms.getSourceFile() + ": " //$NON-NLS-1$
                        + ms.getEntryName() + ": " + ex.getMessage()); //$NON-NLS-1$
                MekHQ.getLogger().error(getClass(), METHOD_NAME, ex);
            }
        } else {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                    "Personnel market could not find " //$NON-NLS-1$
                    + UnitType.getTypeName(unitType) + " for recruit from faction " + faction); //$NON-NLS-1$
            return;
        }

        if (null != en) {
            campaign.getPersonnelMarket().addAttachedEntity(p.getId(), en);
            /* adjust vehicle pilot roles according to the type of vehicle rolled */
            if ((en.getEntityType() & Entity.ETYPE_TANK) != 0) {
                if (en.getMovementMode() == EntityMovementMode.TRACKED ||
                        en.getMovementMode() == EntityMovementMode.WHEELED ||
                        en.getMovementMode() == EntityMovementMode.HOVER ||
                        en.getMovementMode() == EntityMovementMode.WIGE) {
                    if (p.getPrimaryRole() == Person.T_VTOL_PILOT) {
                        swapSkills(p, SkillType.S_PILOT_VTOL, SkillType.S_PILOT_GVEE);
                        p.setPrimaryRole(Person.T_GVEE_DRIVER);
                    }
                    if (p.getPrimaryRole() == Person.T_NVEE_DRIVER) {
                        swapSkills(p, SkillType.S_PILOT_NVEE, SkillType.S_PILOT_GVEE);
                        p.setPrimaryRole(Person.T_GVEE_DRIVER);
                    }
                } else if (en.getMovementMode() == EntityMovementMode.VTOL) {
                    if (p.getPrimaryRole() == Person.T_GVEE_DRIVER) {
                        swapSkills(p, SkillType.S_PILOT_GVEE, SkillType.S_PILOT_VTOL);
                        p.setPrimaryRole(Person.T_VTOL_PILOT);
                    }
                    if (p.getPrimaryRole() == Person.T_NVEE_DRIVER) {
                        swapSkills(p, SkillType.S_PILOT_NVEE, SkillType.S_PILOT_VTOL);
                        p.setPrimaryRole(Person.T_VTOL_PILOT);
                    }
                } else if (en.getMovementMode() == EntityMovementMode.NAVAL ||
                        en.getMovementMode() == EntityMovementMode.HYDROFOIL ||
                        en.getMovementMode() == EntityMovementMode.SUBMARINE) {
                    if (p.getPrimaryRole() == Person.T_GVEE_DRIVER) {
                        swapSkills(p, SkillType.S_PILOT_GVEE, SkillType.S_PILOT_NVEE);
                        p.setPrimaryRole(Person.T_NVEE_DRIVER);
                    }
                    if (p.getPrimaryRole() == Person.T_VTOL_PILOT) {
                        swapSkills(p, SkillType.S_PILOT_VTOL, SkillType.S_PILOT_NVEE);
                        p.setPrimaryRole(Person.T_NVEE_DRIVER);
                    }
                }
            }
        }
    }

    private void swapSkills(Person p, String skill1, String skill2) {
        int s1 = p.hasSkill(skill1)?p.getSkill(skill1).getLevel():0;
        int b1 = p.hasSkill(skill1)?p.getSkill(skill1).getBonus():0;
        int s2 = p.hasSkill(skill2)?p.getSkill(skill2).getLevel():0;
        int b2 = p.hasSkill(skill2)?p.getSkill(skill2).getBonus():0;
        p.addSkill(skill1, s2, b2);
        p.addSkill(skill2, s1, b1);
        if (p.getSkill(skill1).getLevel() == 0) {
            p.removeSkill(skill1);
        }
        if (p.getSkill(skill2).getLevel() == 0) {
            p.removeSkill(skill2);
        }
    }
    
    public static String getRecruitFaction(Campaign c) {
        if (c.getFactionCode().equals("MERC")) {
            if (c.getCalendar().get(Calendar.YEAR) > 3055 && Compute.randomInt(20) == 0) {
                ArrayList<String> clans = new ArrayList<>();
                for (String f : RandomFactionGenerator.getInstance().getCurrentFactions()) {
                    if (Faction.getFaction(f).isClan()) {
                        clans.add(f);
                    }
                }
                String clan = Utilities.getRandomItem(clans);
                if (clan != null) {
                    return clan;
                }
            } else {
                String faction = RandomFactionGenerator.getInstance().getEmployer();
                if (faction != null) {
                    return faction;
                }
            }
        }
        return c.getFactionCode();
    }
}
