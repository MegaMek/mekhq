/*
 * Copyright (c) 2018-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.module.atb;

import megamek.client.ratgenerator.MissionRole;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.*;
import megamek.common.event.Subscribe;
import megamek.common.loaders.EntityLoadingException;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.MarketNewPersonnelEvent;
import mekhq.campaign.event.NewDayEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.campaign.universe.RandomFactionGenerator;
import org.apache.logging.log4j.LogManager;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Main engine of the Against the Bot campaign system.
 *
 * @author Neoancient
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
        if (!ev.getCampaign().hasActiveContract() && ev.getCampaign().getPersonnelMarket().getPaidRecruitment()
                && (ev.getCampaign().getLocalDate().getDayOfWeek() == DayOfWeek.MONDAY)) {
            if (ev.getCampaign().getFinances().debit(TransactionType.RECRUITMENT,
                    ev.getCampaign().getLocalDate(), Money.of(100000), "Paid recruitment roll")) {
                doPaidRecruitment(ev.getCampaign());
            } else {
                ev.getCampaign().addReport("<html><font color=\"red\">Insufficient funds for paid recruitment.</font></html>");
            }
        }
    }

    private void doPaidRecruitment(Campaign campaign) {
        int mod;
        switch (campaign.getPersonnelMarket().getPaidRecruitRole()) {
            case MECHWARRIOR:
                mod = -2;
                break;
            case SOLDIER:
                mod = 2;
                break;
            case MECH_TECH:
            case MECHANIC:
            case AERO_TECH:
            case BA_TECH:
            case DOCTOR:
                mod = 1;
                break;
            default:
                mod = 0;
                break;
        }

        mod += campaign.getUnitRatingMod() - IUnitRating.DRAGOON_C;
        if (campaign.getFinances().isInDebt()) {
            mod -= 3;
        }

        Person adminHR = campaign.findBestInRole(PersonnelRole.ADMINISTRATOR_HR, SkillType.S_ADMIN);
        int adminHRExp = (adminHR == null) ? SkillType.EXP_ULTRA_GREEN
                : adminHR.getSkill(SkillType.S_ADMIN).getExperienceLevel();
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
            Person p = campaign.newPerson(campaign.getPersonnelMarket().getPaidRecruitRole());
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
        final Collection<EntityMovementMode> movementModes = new ArrayList<>();
        final Collection<MissionRole> missionRoles = new ArrayList<>();
        int unitType;
        switch (p.getPrimaryRole()) {
            case MECHWARRIOR:
                unitType = UnitType.MEK;
                break;
            case AEROSPACE_PILOT:
                if (!campaign.getCampaignOptions().isAeroRecruitsHaveUnits()) {
                    return;
                }
                unitType = UnitType.AEROSPACEFIGHTER;
                break;
            case PROTOMECH_PILOT:
                unitType = UnitType.PROTOMEK;
                break;
            case BATTLE_ARMOUR:
                unitType = UnitType.BATTLE_ARMOR;
                break;
            case SOLDIER:
                unitType = UnitType.INFANTRY;
                // infantry will have a 1/3 chance of being field guns
                if (Compute.d6() <= 2) {
                    movementModes.addAll(IUnitGenerator.ALL_INFANTRY_MODES);
                    missionRoles.add(MissionRole.FIELD_GUN);
                }
                break;
            default:
                return;
        }

        int weight = -1;
        if (unitType == UnitType.MEK
                || unitType == UnitType.TANK
                || unitType == UnitType.AEROSPACEFIGHTER) {
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
        final String faction = getRecruitFaction(campaign);
        MechSummary ms = campaign.getUnitGenerator().generate(faction, unitType, weight, campaign.getGameYear(),
                IUnitRating.DRAGOON_F, movementModes, missionRoles);
        Entity en;
        if (null != ms) {
            if (Factions.getInstance().getFaction(faction).isClan() && ms.getName().matches(".*Platoon.*")) {
                String name = "Clan " + ms.getName().replaceAll("Platoon", "Point");
                ms = MechSummaryCache.getInstance().getMech(name);
                LogManager.getLogger().info("looking for Clan infantry " + name);
            }
            try {
                en = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
            } catch (EntityLoadingException ex) {
                en = null;
                LogManager.getLogger().error("Unable to load entity: "
                        + ms.getSourceFile() + ": " + ms.getEntryName() + ": " + ex.getMessage(), ex);
            }
        } else {
            LogManager.getLogger().error("Personnel market could not find "
                    + UnitType.getTypeName(unitType) + " for recruit from faction " + faction);
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
                    if (p.getPrimaryRole().isVTOLPilot()) {
                        swapSkills(p, SkillType.S_PILOT_VTOL, SkillType.S_PILOT_GVEE);
                        p.setPrimaryRoleDirect(PersonnelRole.GROUND_VEHICLE_DRIVER);
                    } else if (p.getPrimaryRole().isNavalVehicleDriver()) {
                        swapSkills(p, SkillType.S_PILOT_NVEE, SkillType.S_PILOT_GVEE);
                        p.setPrimaryRoleDirect(PersonnelRole.GROUND_VEHICLE_DRIVER);
                    }
                } else if (en.getMovementMode() == EntityMovementMode.VTOL) {
                    if (p.getPrimaryRole().isGroundVehicleDriver()) {
                        swapSkills(p, SkillType.S_PILOT_GVEE, SkillType.S_PILOT_VTOL);
                        p.setPrimaryRoleDirect(PersonnelRole.VTOL_PILOT);
                    } else if (p.getPrimaryRole().isNavalVehicleDriver()) {
                        swapSkills(p, SkillType.S_PILOT_NVEE, SkillType.S_PILOT_VTOL);
                        p.setPrimaryRoleDirect(PersonnelRole.VTOL_PILOT);
                    }
                } else if (en.getMovementMode() == EntityMovementMode.NAVAL ||
                        en.getMovementMode() == EntityMovementMode.HYDROFOIL ||
                        en.getMovementMode() == EntityMovementMode.SUBMARINE) {
                    if (p.getPrimaryRole().isGroundVehicleDriver()) {
                        swapSkills(p, SkillType.S_PILOT_GVEE, SkillType.S_PILOT_NVEE);
                        p.setPrimaryRoleDirect(PersonnelRole.NAVAL_VEHICLE_DRIVER);
                    } else if (p.getPrimaryRole().isVTOLPilot()) {
                        swapSkills(p, SkillType.S_PILOT_VTOL, SkillType.S_PILOT_NVEE);
                        p.setPrimaryRoleDirect(PersonnelRole.NAVAL_VEHICLE_DRIVER);
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
            if ((c.getGameYear() > 3055) && (Compute.randomInt(20) == 0)) {
                ArrayList<String> clans = new ArrayList<>();
                for (String f : RandomFactionGenerator.getInstance().getCurrentFactions()) {
                    if (Factions.getInstance().getFaction(f).isClan()) {
                        clans.add(f);
                    }
                }
                String clan = ObjectUtility.getRandomItem(clans);
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
