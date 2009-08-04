/*
 * Unit.java
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

import java.io.Serializable;
import megamek.common.AmmoType;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.VTOL;
import mekhq.campaign.personnel.PilotPerson;
import mekhq.campaign.work.*;

/**
 * This is a wrapper class for entity, so that we can add some 
 * functionality to it
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Unit implements Serializable {

    public static final int SITE_FIELD = 0;
    public static final int SITE_MOBILE_BASE = 1;
    public static final int SITE_BAY = 2;
    public static final int SITE_FACILITY = 3;
    public static final int SITE_FACTORY = 4;
    public static final int SITE_N = 5;
    
    private Entity entity;
    private int site;
    private boolean deployed;
    private PilotPerson pilot;
    private boolean salvage;
    
    //TODO: need to keep track of what components have been removed from this unit
    
    public Unit(Entity en) {
        this.entity = en;
        this.site = SITE_BAY;
        this.deployed = false;
        this.salvage = false;
    }
    
    public void setEntity(Entity en) {
        this.entity = en;
    }
    
    public Entity getEntity() {
        return entity;
    }
    
    public int getId() {
        return getEntity().getId();
    }
    
    public int getSite() {
        return site;
    }
    
    public void setSite(int i) {
        this.site = i;
    }
    
    public PilotPerson getPilot() {
        return pilot;
    }
    
    public void setPilot(PilotPerson pp) {
        if(hasPilot()) {
            pilot.setAssignedUnit(null);
        }
        this.pilot = pp;
        if(null == pp) {
            entity.setCrew(null);
        } else {
            pp.setAssignedUnit(this);
            entity.setCrew(pp.getPilot());
        }
    }
    
    public boolean isSalvage() {
        return salvage;
    }
    
    public void setSalvage(boolean b) {
        this.salvage = b;
    }
    
    /**
     * Is the given location on the entity destroyed?
     * @param loc - an <code>int</code> for the location
     * @return <code>true</code> if the location is destroyed
     */
    public boolean isLocationDestroyed(int loc) {
        if(loc > entity.locations() || loc < 0) {
            return false;
        }
        //on mechs, hip and shoulder criticals also make the location effectively destroyed
        if(entity instanceof Mech && (entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HIP, loc) > 0
                || entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER, loc) > 0)) {
                return true;
        }        
        return entity.isLocationBad(loc);
    }
    
    /**
     * Run a diagnostic on this unit and add WorkItems to the campaign
     */
    public void runDiagnostic(Campaign campaign) {
       
        if(isSalvage()) {
            getSalvageableParts(campaign);
            return;
        }
        
        //It is somewhat unclear but the language of StratOps implies that even equipment that
        //is "combat destroyed" can be repaired.  For example, a gyro with 2 hits can still be repaired
        //although with a +4 mod.  Weapons and other equipment must pass a roll to be repairable
            
        //cycle through the locations and assign repairs and replacements
        //don't do weapons and equipment here because some are spreadable
        int engineHits = 0;
        int engineCrits = 0;
        for(int i = 0; i < entity.locations(); i++) {
            
            //replace location?
            if(isLocationDestroyed(i)) {
                if(entity instanceof Mech || entity instanceof Protomech) {
                    campaign.addWork(new LocationReplacement(this, i));
                }
                else if(entity instanceof VTOL && i == VTOL.LOC_ROTOR) {
                    campaign.addWork(new RotorReplacement(this, i));
                }
                else if(entity instanceof Tank && i == Tank.LOC_TURRET) {
                    campaign.addWork(new TurretReplacement(this, i));
                }
            } else {
                //repair internal
                double pctInternal = entity.getInternal(i)/entity.getOInternal(i);
                if(pctInternal < 1.00) {
                    if(entity instanceof Mech || entity instanceof Protomech) {
                        campaign.addWork(new MekInternalRepair(this, i, pctInternal));
                    } else if (entity instanceof Tank) {
                        if(entity instanceof VTOL && i == VTOL.LOC_ROTOR) {
                            int hits = entity.getOInternal(i)- entity.getInternal(i);
                            while(hits > 0) {
                                campaign.addWork(new RotorRepair(this));
                                hits--;
                            }
                        }
                        campaign.addWork(new VeeInternalRepair(this, i));
                    }
                }
            }
            
            //replace armor?
            int diff = entity.getOArmor(i, false) - entity.getArmor(i, false);
            if(entity.hasRearArmor(i)) {
                diff +=  entity.getOArmor(i, true) - entity.getArmor(i, true);
            }
            if(diff > 0) {
                campaign.addWork(new ArmorReplacement(this, i, diff));
            }         
            
            //check for various component damage
            if(entity instanceof Mech) {
                engineHits += entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, i);
                engineCrits += entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, i);
                int sensorHits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, i);
                if(sensorHits > 0) {
                    if(entity.isSystemRepairable(Mech.SYSTEM_SENSORS, i)) {
                        campaign.addWork(new MekSensorRepair(this, sensorHits));
                    } else {
                        campaign.addWork(new MekSensorReplacement(this));
                    } 
                }
                int lifeHits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_LIFE_SUPPORT, i);
                if(lifeHits > 0) {
                    if(entity.isSystemRepairable(Mech.SYSTEM_LIFE_SUPPORT, i)) {
                        campaign.addWork(new MekLifeSupportRepair(this, lifeHits));
                    } else {
                        campaign.addWork(new MekLifeSupportReplacement(this));
                    }
                }
                int gyroHits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, i);
                if(gyroHits > 0) {
                    if(entity.isSystemRepairable(Mech.SYSTEM_GYRO, i)) {
                        campaign.addWork(new MekGyroRepair(this, gyroHits));
                    } else {
                        campaign.addWork(new MekGyroReplacement(this));
                    }
                }
                //check actuators
                //don't check hips and shoulders because that should be accounted for in location replacement
                if(entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_ARM, i) > 0) {
                    if(entity.isSystemRepairable(Mech.ACTUATOR_UPPER_ARM, i)) {
                        campaign.addWork(new MekUpArmActuatorRepair(this, 1, i));
                    } else {
                        campaign.addWork(new MekUpArmActuatorReplacement(this, i));
                    }
                }
                if(entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_ARM, i) > 0) {
                    if(entity.isSystemRepairable(Mech.ACTUATOR_LOWER_ARM, i)) {
                        campaign.addWork(new MekLowArmActuatorRepair(this, 1, i));
                    } else {
                        campaign.addWork(new MekLowArmActuatorReplacement(this, i));
                    }
                }
                if(entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_HAND, i) > 0) {
                    if(entity.isSystemRepairable(Mech.ACTUATOR_HAND, i)) {
                        campaign.addWork(new MekHandActuatorRepair(this, 1, i));
                    } else {
                        campaign.addWork(new MekHandActuatorReplacement(this, i));
                    }
                }
                if(entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_UPPER_LEG, i) > 0) {
                    if(entity.isSystemRepairable(Mech.ACTUATOR_UPPER_LEG, i)) {
                        campaign.addWork(new MekUpLegActuatorRepair(this, 1, i));
                    } else {
                        campaign.addWork(new MekUpLegActuatorReplacement(this, i));
                    }
                }
                if(entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_LOWER_LEG, i) > 0) {
                    if(entity.isSystemRepairable(Mech.ACTUATOR_LOWER_LEG, i)) {
                        campaign.addWork(new MekLowLegActuatorRepair(this, 1, i));
                    } else {
                        campaign.addWork(new MekLowLegActuatorReplacement(this, i));
                    }
                }
                if(entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_FOOT, i) > 0) {
                    if(entity.isSystemRepairable(Mech.ACTUATOR_FOOT, i)) {
                        campaign.addWork(new MekFootActuatorRepair(this, 1, i));
                    } else {
                        campaign.addWork(new MekFootActuatorReplacement(this, i));
                    }
                }                
            }//end mech check
            
            if(entity instanceof Tank) {
                Tank tank = (Tank)entity;
                if(tank.isStabiliserHit(i)) {
                    campaign.addWork(new VeeStabiliserRepair(this, i));
                }
            }
        }//end location checks
        
        //check engine
        if(entity instanceof Mech && engineHits > 0) {
            if(engineHits >= engineCrits) {
                campaign.addWork(new MekEngineReplacement(this));
            } else {
                campaign.addWork(new MekEngineRepair(this, engineHits));
            }
        }
        
        //check vee components
        if(entity instanceof Tank) {
            Tank tank = (Tank)entity;
            if(tank.isTurretLocked()) {
                campaign.addWork(new TurretLockRepair(this));
            }
            if(tank.isTurretJammed()) {
                tank.unjamTurret();
            }
            if(tank.getSensorHits() > 0) {
                campaign.addWork(new VeeSensorReplacement(this));
            }
            //TODO: cant do motive damage because Tank doesn't have public methods          
        }
        
        //now lets cycle through equipment
        for(Mounted m : entity.getEquipment()) {
            if(m.isHit() || m.isDestroyed()) {
                
                //some slots need to be skipped (like armor, endo-steel, etc.)
                if(!m.getType().isHittable()) {
                    m.setHit(false);
                    m.setDestroyed(false);
                    for(int loc = 0; loc < getEntity().locations(); loc++) {
                        for (int i = 0; i < getEntity().getNumberOfCriticals(loc); i++) {
                            CriticalSlot slot = getEntity().getCritical(loc, i);
                            // ignore empty & system slots
                            if ((slot == null) || (slot.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                                continue;
                            }
                            if (getEntity().getEquipmentNum(m) == slot.getIndex()) {
                                slot.setHit(false);
                                slot.setDestroyed(false);
                            }
                        } 
                    }
                    continue;
                }
                
                //check flags for jump jets and heat sinks because those are handled by their own classes
                if(m.getType() instanceof MiscType) {
                    if(m.getType().hasFlag(MiscType.F_JUMP_JET)) {
                        if(m.isRepairable()) {
                            campaign.addWork(new JumpJetRepair(this, 1, m));
                        } else {
                            campaign.addWork(new JumpJetReplacement(this, m));
                        }
                        continue;
                    } 
                    else if(m.getType().hasFlag(MiscType.F_HEAT_SINK) 
                                || m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
                        if(m.isRepairable()) {
                            campaign.addWork(new HeatSinkRepair(this, 1, m));
                        } else {
                            campaign.addWork(new HeatSinkReplacement(this, m));
                        }
                        continue;
                    } 
                    //leave CASE out for now
                    //http://www.classicbattletech.com/forums/index.php/topic,49940.0.html
                    else if(m.getType().hasFlag(MiscType.F_CASE) || m.getType().hasFlag(MiscType.F_CASEII)) {
                        m.setHit(false);
                        m.setDestroyed(false);
                        continue;
                    }
                }
                
                //ammo is also handled its own way
                if(m.getType() instanceof AmmoType) {
                    campaign.addWork(new AmmoBinReplacement(this, m));
                    continue;
                    //don't do reloads here because I want them all grouped at the bottom of the queue
                }
                               
                //combat destroyed is not the same as really destroyed
                //TODO: I am no longer making the check here. Any randomness in this method can lead to 
                //weird results when units are deployed and then reloaded. This should really be done in MegaMek
                //with the proper use of setHit and setDestroyed
                //and added to the MUL file.      
                if(m.isRepairable()) {
                    campaign.addWork(new EquipmentRepair(this, getCrits(m), m));
                } else {
                    campaign.addWork(new EquipmentReplacement(this, m));
                }
            }
        }
        
        //now check for reloads
        for(Mounted m : entity.getAmmo()) {
            if(!(m.getType() instanceof AmmoType)) {
                //shouldn't happen, but you never know
                continue;
            }
            //put a reload item in for all ammo types, because user may want to swap
            if(m.getShotsLeft() < ((AmmoType)m.getType()).getShots()) {
                campaign.addWork(new ReloadItem(this, m));
            }
        }
    }
    
    public void getSalvageableParts(Campaign campaign) {
        
        for(Mounted m : entity.getEquipment()) {
            //some slots need to be skipped (like armor, endo-steel, etc.)
            if(m.getType().isHittable() && !m.isDestroyed()) {
                campaign.addWork(new EquipmentSalvage(this, m, campaign));
            }
        }
    }
    
    /**
     * @param m - A Mounted class to find crits for
     * @return the number of crits exising for this Mounted
     */
    public int getCrits(Mounted m) {
        //TODO: I should probably just add this method to Entity in MM
        int hits = 0;
        for(int loc = 0; loc < entity.locations(); loc++) {
            for (int i = 0; i < entity.getNumberOfCriticals(loc); i++) {
                CriticalSlot slot = entity.getCritical(loc, i);
                // ignore empty & system slots
                if ((slot == null) || (slot.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                    continue;
                }
                if (entity.getEquipmentNum(m) == slot.getIndex() 
                        && (slot.isHit() || slot.isDestroyed())) {
                    hits++;
                }
            }
        }
        return hits;
    }
    
    public boolean hasPilot() {
        return null != pilot;
    }
    
    public void removePilot() {
        if(hasPilot()) {
            getPilot().setAssignedUnit(null);
            setPilot(null);
        }     
    }
    
    public String getPilotDesc() {
        if(hasPilot()) {
            return entity.getCrew().getName() + " " + entity.getCrew().getGunnery() + "/" + entity.getCrew().getPiloting();
        }
        return "NO PILOT";
    }
    
    /**
     * produce a string in HTML that can be embedded in larger reports
     */
    public String getDescHTML() {
        String toReturn = "<b>" + entity.getDisplayName() + "</b><br>";
        toReturn += getPilotDesc() + "<br>";
        if(isDeployed()) {
            toReturn  += "DEPLOYED!<br>";
        } else {
            toReturn += "Site: " + getCurrentSiteName() + "<br>";
        }
        return toReturn;
    }
    
    public TargetRoll getSiteMod() {
        switch(site) {
            case SITE_FIELD:
                return new TargetRoll(2, "in the field");
            case SITE_MOBILE_BASE:
                return new TargetRoll(1, "mobile base");
            case SITE_BAY:
                return new TargetRoll(0, "transport bay");
            case SITE_FACILITY:
                return new TargetRoll(-2, "maintenance facility");
            case SITE_FACTORY:
                return new TargetRoll(-4, "factory");
            default:
                return new TargetRoll(0, "unknown location");
        }
    }
    
    public static String getSiteName(int loc) {
        switch(loc) {
            case SITE_FIELD:
                return "In the Field";
            case SITE_MOBILE_BASE:
                return "Mobile Base";
            case SITE_BAY:
                return "Transport Bay";
            case SITE_FACILITY:
                return "Maintenance Facility";
            case SITE_FACTORY:
                return "Factory";
            default:
                return "Unknown";
        }
    }
    
    public String getCurrentSiteName() {
        return getSiteName(site);
    }
    
    public boolean isDeployed() {
        return deployed;
    }
    
    public void setDeployed(boolean b) {
        this.deployed = b;
        if(deployed) {
            setSite(SITE_FIELD);
            if(null != pilot) {
                pilot.setDeployed(true);
            }
        }
    }
    
    public boolean canDeploy() {
        //can't deploy if you are already deployed
        if(isDeployed()) {
            return false;
        }
        //cant deploy with no pilot
        if(!hasPilot()) {
            return false;
        }
        return true;
    }
    
    public boolean hasEndosteel() {
        for (Mounted mEquip : entity.getMisc()) {
            MiscType mtype = (MiscType) mEquip.getType();
            if (mtype.hasFlag(MiscType.F_ENDO_STEEL)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Have to make one here because the one in MegaMek only returns true if operable
     * @return
     */
    public boolean hasTSM() {
        for (Mounted mEquip : entity.getMisc()) {
            MiscType mtype = (MiscType) mEquip.getType();
            if (mtype.hasFlag(MiscType.F_TSM)) {
                return true;
            }
        }
        return false;
    }
}
