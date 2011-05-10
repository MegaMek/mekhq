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

import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.ConvFighter;
import megamek.common.CriticalSlot;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import megamek.common.VTOL;
import megamek.common.Warship;
import megamek.common.WeaponType;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import mekhq.MekHQApp;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Availability;
import mekhq.campaign.parts.EquipmentPart;
import mekhq.campaign.parts.GenericSparePart;
import mekhq.campaign.parts.MekActuator;
import mekhq.campaign.parts.MekEngine;
import mekhq.campaign.parts.MekGyro;
import mekhq.campaign.parts.MekLifeSupport;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MekSensor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.PilotPerson;
import mekhq.campaign.work.*;

/**
 * This is a wrapper class for entity, so that we can add some functionality to
 * it
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Unit implements Serializable, MekHqXmlSerializable {
	private static final long serialVersionUID = 4079817548868582600L;
	public static final int SITE_FIELD = 0;
	public static final int SITE_MOBILE_BASE = 1;
	public static final int SITE_BAY = 2;
	public static final int SITE_FACILITY = 3;
	public static final int SITE_FACTORY = 4;
	public static final int SITE_N = 5;

	public static final int STATE_UNDAMAGED = 0;
	public static final int STATE_LIGHT_DAMAGE = 1;
	public static final int STATE_HEAVY_DAMAGE = 2;
	public static final int STATE_CRIPPLED = 3;

	public static final int QUALITY_A = 0;
	public static final int QUALITY_B = 1;
	public static final int QUALITY_C = 2;
	public static final int QUALITY_D = 3;
	public static final int QUALITY_E = 4;
	public static final int QUALITY_F = 5;

	private Entity entity;
	private int site;
	private PilotPerson pilot;
	private int pilotId = -1;
	private boolean salvaged;
	private boolean customized;
	private int id = -1;
	private int quality;

	public Campaign campaign;

	private ArrayList<Part> parts;

	public Unit() {
		this(null, null);
	}
	
	public Unit(Entity en, Campaign c) {
		this.entity = en;
		this.site = SITE_BAY;
		this.salvaged = false;
		this.campaign = c;
		this.customized = false;
		this.quality = QUALITY_D;
		this.parts = new ArrayList<Part>();
		reCalc();
	}
	
	public static String getDamageStateName(int i) {
		switch(i) {
		case STATE_UNDAMAGED:
			return "Undamaged";
		case STATE_LIGHT_DAMAGE:
			return "Light Damage";
		case STATE_HEAVY_DAMAGE:
			return "Heavy Damage";
		case STATE_CRIPPLED:
			return "Crippled";
		default:
			return "Unknown";
		}
	}
	
	public static String getQualityName(int quality) {
		switch(quality) {
		case QUALITY_A:
			return "A";
		case QUALITY_B:
			return "B";
		case QUALITY_C:
			return "C";
		case QUALITY_D:
			return "D";
		case QUALITY_E:
			return "E";
		case QUALITY_F:
			return "F";
		default:
			return "?";
		}
	}
	
	public String getQualityName() {
		return getQualityName(getQuality());
	}
	
	public String getStatus() {
		if(!isRepairable()) {
			return "Salvage";
		}
		else if(!isFunctional()) {
			return "Inoperable";
		}
		else {
			return getDamageStateName(getDamageState());
		}
	}
	
	
	public void reCalc() {
		// Do nothing.
	}

	public void setEntity(Entity en) {
		this.entity = en;
	}

	public Entity getEntity() {
		return entity;
	}

	public int getId() {
		if (id >= 0)
			return id;
		
		return getEntity().getId();
	}

	public void setPilotId(int inId) {
		pilotId = inId;
	}

	public int getPilotId() {
		return pilotId;
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
		if (hasPilot()) {
			pilot.setAssignedUnit(null);
		}
		this.pilot = pp;
		if (null == pp) {
			entity.setCrew(null);
		} else {
			pp.setAssignedUnit(this);
			entity.setCrew(pp.getPilot());
		}
	}

	public int getQuality() {
		return quality;
	}
	
	public void setQuality(int q) {
		this.quality = q;
	}
	
	public boolean isSalvage() {
		return salvaged;
	}

	public void setSalvage(boolean b) {
		this.salvaged = b;
	}

	public void setCustomized(boolean customized) {
		this.customized = customized;
	}

	public boolean isCustomized() {
		return customized;
	}

	public boolean isFunctional() {
		if (entity instanceof Mech) {
			// center torso bad?? head bad?
			if (entity.isLocationBad(Mech.LOC_CT)
					|| entity.isLocationBad(Mech.LOC_HEAD)) {
				return false;
			}
			// engine destruction?
			int engineHits = 0;
			for (int i = 0; i < entity.locations(); i++) {
				engineHits += entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
						Mech.SYSTEM_ENGINE, i);
			}
			if (engineHits > 2) {
				return false;
			}
		}
		if (entity instanceof Tank) {
			for (int i = 0; i < entity.locations(); i++) {
				if (entity.isLocationBad(i)) {
					return false;
				}
			}

			// TODO: we need an isEngineHit() function in Tank
			// Tank t = (Tank)entity;
		}
		return true;
	}

	public boolean isRepairable() {
		if (entity instanceof Mech) {
			// you can repair anything so long as one point of CT is left
			if (entity.isLocationBad(Mech.LOC_CT)) {
				return false;
			}
		}
		if (entity instanceof Tank) {
			// can't repair a tank with a destroyed location
			for (int i = 0; i < entity.locations(); i++) {
				if (entity.isLocationBad(i)) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Is the given location on the entity destroyed?
	 * 
	 * @param loc
	 *            - an <code>int</code> for the location
	 * @return <code>true</code> if the location is destroyed
	 */
	public boolean isLocationDestroyed(int loc) {
		if (loc > entity.locations() || loc < 0) {
			return false;
		}
		// on mechs, hip and shoulder criticals also make the location
		// effectively destroyed
		if (entity instanceof Mech
				&& (entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
						Mech.ACTUATOR_HIP, loc) > 0 || entity.getHitCriticals(
						CriticalSlot.TYPE_SYSTEM, Mech.ACTUATOR_SHOULDER, loc) > 0)) {
			return true;
		}
		return entity.isLocationBad(loc);
	}

	/**
	 * Run a diagnostic on this unit and add WorkItems to the campaign Uses
	 * Strat Ops rules
	 */
	/*
	public void runDiagnosticStratOps() {

		if (!isRepairable()) {
			setSalvage(true);
		}
		// It is somewhat unclear but the language of StratOps implies that even
		// equipment that
		// is "combat destroyed" can be repaired. For example, a gyro with 2
		// hits can still be repaired
		// although with a +4 mod. Weapons and other equipment must pass a roll
		// to be repairable

		SalvageItem salvage = null;
		RepairItem repair = null;

		// cycle through the locations and assign tasks
		// don't do weapons and equipment here because some are spreadable
		int engineHits = 0;
		int engineCrits = 0;
		for (int i = 0; i < entity.locations(); i++) {

			// replace location?
			if (isLocationDestroyed(i)) {
				if (entity instanceof Mech || entity instanceof Protomech) {
					campaign.addWork(new LocationReplacement(this, i));
				} else if (entity instanceof VTOL && i == VTOL.LOC_ROTOR) {
					campaign.addWork(new RotorReplacement(this, i));
				} else if (entity instanceof Tank && i == Tank.LOC_TURRET) {
					campaign.addWork(new TurretReplacement(this, i));
				}
			} else {
				// repair internal
				repair = null;
				salvage = null;
				if (entity instanceof Mech && i != Mech.LOC_CT) {
					salvage = new LocationSalvage(this, i);
					campaign.addWork(salvage);
					// TODO: rotor and turret salvage for vees
				}
				double pctInternal = 1.00 - (((double) entity.getInternal(i)) / ((double) entity
						.getOInternal(i)));
				if (pctInternal > 0.00) {
					if (entity instanceof Mech || entity instanceof Protomech) {
						repair = new MekInternalRepair(this, i, pctInternal);
						campaign.addWork(repair);
					} else if (entity instanceof Tank) {
						if (entity instanceof VTOL && i == VTOL.LOC_ROTOR) {
							int hits = entity.getOInternal(i)
									- entity.getInternal(i);
							while (hits > 0) {
								campaign.addWork(new RotorRepair(this));
								hits--;
							}
						} else {
							repair = new VeeInternalRepair(this, i);
							campaign.addWork(repair);
						}
					}
				}
				if (null != salvage && null != repair) {
					salvage.setRepairId(repair.getId());
					repair.setSalvageId(salvage.getId());
				}
			}

			// replace armor?
			int diff = entity.getOArmor(i, false) - entity.getArmor(i, false);
			if (diff > 0) {
				campaign.addWork(new ArmorReplacement(this, i, entity
						.getArmorType(i), false));
			}
			if (entity.getArmor(i, false) > 0) {
				campaign.addWork(new ArmorSalvage(this, i, entity
						.getArmorType(i), false));
			}
			if (entity.hasRearArmor(i)) {
				diff = entity.getOArmor(i, true) - entity.getArmor(i, true);
				if (diff > 0) {
					campaign.addWork(new ArmorReplacement(this, i, entity
							.getArmorType(i), true));
				}
				if (entity.getArmor(i, true) > 0) {
					campaign.addWork(new ArmorSalvage(this, i, entity
							.getArmorType(i), true));
				}
			}

			// check for various component damage
			if (entity instanceof Mech) {
				engineHits += entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
						Mech.SYSTEM_ENGINE, i);
				engineCrits += entity.getNumberOfCriticals(
						CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_ENGINE, i);
				int sensorHits = entity.getHitCriticals(
						CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_SENSORS, i);
				if (entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM,
						Mech.SYSTEM_SENSORS, i) > 0) {
					if (entity.isSystemRepairable(Mech.SYSTEM_SENSORS, i)) {
						salvage = new MekSensorSalvage(this);
						campaign.addWork(salvage);
						if (sensorHits > 0) {
							repair = new MekSensorRepair(this, sensorHits);
							campaign.addWork(repair);
							salvage.setRepairId(repair.getId());
							repair.setSalvageId(salvage.getId());
						}
					} else {
						campaign.addWork(new MekSensorReplacement(this));
					}
				}
				int lifeHits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
						Mech.SYSTEM_LIFE_SUPPORT, i);
				if (entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM,
						Mech.SYSTEM_LIFE_SUPPORT, i) > 0) {
					if (entity.isSystemRepairable(Mech.SYSTEM_LIFE_SUPPORT, i)) {
						salvage = new MekLifeSupportSalvage(this);
						campaign.addWork(salvage);
						if (lifeHits > 0) {
							repair = new MekLifeSupportRepair(this, lifeHits);
							campaign.addWork(repair);
							salvage.setRepairId(repair.getId());
							repair.setSalvageId(salvage.getId());
						}
					} else {
						campaign.addWork(new MekLifeSupportReplacement(this));
					}
				}
				int gyroHits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
						Mech.SYSTEM_GYRO, i);
				if (entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM,
						Mech.SYSTEM_GYRO, i) > 0) {
					if (entity.isSystemRepairable(Mech.SYSTEM_GYRO, i)) {
						salvage = new MekGyroSalvage(this);
						campaign.addWork(salvage);
						if (gyroHits > 0) {
							repair = new MekGyroRepair(this, gyroHits);
							campaign.addWork(repair);
							salvage.setRepairId(repair.getId());
							repair.setSalvageId(salvage.getId());
						}
					} else {
						campaign.addWork(new MekGyroReplacement(this));
					}
				}
				// check actuators
				// don't check hips and shoulders because that should be
				// accounted for in location replacement
				for (int act = Mech.ACTUATOR_UPPER_ARM; act <= Mech.ACTUATOR_FOOT; act++) {
					if (act == Mech.ACTUATOR_HIP
							|| act == Mech.ACTUATOR_SHOULDER) {
						continue;
					}
					if (entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM,
							act, i) > 0) {
						if (entity.isSystemRepairable(act, i)) {
							salvage = new MekActuatorSalvage(this, i, act);
							campaign.addWork(salvage);
							if (entity.getHitCriticals(
									CriticalSlot.TYPE_SYSTEM, act, i) > 0) {
								repair = new MekActuatorRepair(this, 1, i, act);
								campaign.addWork(repair);
								salvage.setRepairId(repair.getId());
								repair.setSalvageId(salvage.getId());
							}
						} else {
							campaign.addWork(new MekActuatorReplacement(this,
									i, act));
						}
					}
				}
			}// end mech check

			if (entity instanceof Tank) {
				Tank tank = (Tank) entity;
				if (tank.isStabiliserHit(i)) {
					campaign.addWork(new VeeStabiliserRepair(this, i));
				}
			}
		}// end location checks

		// check engine
		if (entity instanceof Mech) {
			// no repairing of cockpit, so instafix
			this.repairSystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_COCKPIT);
			if (engineHits >= engineCrits) {
				campaign.addWork(new MekEngineReplacement(this));
			} else {
				salvage = new MekEngineSalvage(this);
				campaign.addWork(salvage);
				if (engineHits > 0) {
					repair = new MekEngineRepair(this, engineHits);
					campaign.addWork(repair);
					salvage.setRepairId(repair.getId());
					repair.setSalvageId(salvage.getId());
				}
			}
		}

		// check vee components
		if (entity instanceof Tank) {
			Tank tank = (Tank) entity;
			//TODO: deal with dual turrets
			if (tank.isTurretLocked(Tank.LOC_TURRET)) {
				campaign.addWork(new TurretLockRepair(this));
			}
			if (tank.isTurretJammed(Tank.LOC_TURRET)) {
				tank.unjamTurret(Tank.LOC_TURRET);
			}
			if (tank.getSensorHits() > 0) {
				campaign.addWork(new VeeSensorReplacement(this));
			}
			// TODO: cant do motive damage because Tank doesn't have public
			// methods
		}

		// now lets cycle through equipment
		for (Mounted m : entity.getEquipment()) {

			// some slots need to be skipped (like armor, endo-steel, etc.)
			// leave CASE out for now
			// http://www.classicbattletech.com/forums/index.php/topic,49940.0.html
			if (!m.getType().isHittable()) {
				m.setHit(false);
				m.setDestroyed(false);
				for (int loc = 0; loc < getEntity().locations(); loc++) {
					for (int i = 0; i < getEntity().getNumberOfCriticals(loc); i++) {
						CriticalSlot slot = getEntity().getCritical(loc, i);
						// ignore empty & system slots
						if ((slot == null)
								|| (slot.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
							continue;
						}
						if (getEntity().getEquipmentNum(m) == slot.getIndex()) {
							slot.setHit(false);
							slot.setDestroyed(false);
							slot.setRepairable(true);
						}
					}
				}
				continue;
			}

			boolean isHit = m.isHit() || m.isDestroyed();

			// check flags for jump jets and heat sinks because those are
			// handled by their own classes
			if (m.getType() instanceof MiscType) {
				if (m.getType().hasFlag(MiscType.F_JUMP_JET)) {
					if (m.isRepairable()) {
						salvage = new JumpJetSalvage(this, m);
						campaign.addWork(salvage);
						if (isHit) {
							repair = new JumpJetRepair(this, 1, m);
							campaign.addWork(repair);
							salvage.setRepairId(repair.getId());
							repair.setSalvageId(salvage.getId());
						}
					} else {
						campaign.addWork(new JumpJetReplacement(this, m));
					}
					continue;
				} else if (m.getType().hasFlag(MiscType.F_HEAT_SINK)
						|| m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
					// if location is -1, then this is a heat sink internal to
					// the engine
					if (m.getLocation() == -1) {
						continue;
					}
					if (m.isRepairable()) {
						salvage = new HeatSinkSalvage(this, m);
						campaign.addWork(salvage);
						if (isHit) {
							repair = new HeatSinkRepair(this, 1, m);
							campaign.addWork(repair);
							salvage.setRepairId(repair.getId());
							repair.setSalvageId(salvage.getId());
						}
					} else {
						campaign.addWork(new HeatSinkReplacement(this, m));
					}
					continue;
				}
			}

			// ammo is also handled its own way
			if (m.getType() instanceof AmmoType && isHit) {
				campaign.addWork(new AmmoBinReplacement(this, m));
				continue;
				// don't do reloads here because I want them all grouped at the
				// bottom of the queue
			}

			// combat destroyed is not the same as really destroyed
			// TODO: I am no longer making the check here. Any randomness in
			// this method can lead to
			// weird results when units are deployed and then reloaded. This
			// should really be done in MegaMek
			// with the proper use of setHit and setDestroyed
			// and added to the MUL file.
			if (m.isRepairable()) {
				salvage = new EquipmentSalvage(this, m);
				campaign.addWork(salvage);
				if (isHit) {
					repair = new EquipmentRepair(this, getCrits(m), m);
					campaign.addWork(repair);
					salvage.setRepairId(repair.getId());
					repair.setSalvageId(salvage.getId());
				}
			} else {
				campaign.addWork(new EquipmentReplacement(this, m));
			}
		}

		// now check for reloads
		for (Mounted m : entity.getAmmo()) {
			if (!(m.getType() instanceof AmmoType)) {
				// shouldn't happen, but you never know
				continue;
			}
			// put a reload item in for all ammo types, because user may want to
			// swap
			if (m.getShotsLeft() < ((AmmoType) m.getType()).getShots()) {
				campaign.addWork(new ReloadItem(this, m));
			}
		}
	}
	*/

	/**
	 * Run a diagnostic on this unit and add WorkItems to the campaign Uses
	 * Warchest rules
	 */
	/*
	public void runDiagnosticWarchest() {
		if (getDamageState() != Unit.STATE_UNDAMAGED) {
			FullRepairWarchest fullRepair = new FullRepairWarchest(this);
			campaign.addWork(fullRepair);
		}
	}
	*/

	/**
	 * Run a diagnostic on this unit and add WorkItems to the campaign Uses
	 * Generic spare parts rules
	 */
	/*
	public void runDiagnosticGenericSpareParts() {
		runDiagnosticStratOps();
		// Change all repair items into replacement items
		for (WorkItem task : campaign.getTasksForUnit(getId())) {
			if (task instanceof RepairItem) {
				campaign.mutateTask(task,
						((RepairItem) task).getReplacementTask());
			}
		}
	}
	*/

	/**
	 * Run a diagnostic on this unit
	 */
	public void runDiagnostic() {
		
		//need to set up an array of part ids to avoid concurrent modification
		//problems because some updateCondition methods will remove the part and put
		//in a new one
		ArrayList<Integer> partIds = new ArrayList<Integer>();
		for(Part p : parts) {
			partIds.add(p.getId());
		}
		for(int pid : partIds) {
			Part part = campaign.getPart(pid);
			if(!isRepairable() || isSalvage()) {
				part.setSalvaging(true);
			} else {
				part.setSalvaging(false);
			}
			part.updateConditionFromEntity();
		}
		/*
		int repairSystem = campaign.getCampaignOptions().getRepairSystem();
		if (repairSystem == CampaignOptions.REPAIR_SYSTEM_STRATOPS) {
			runDiagnosticStratOps();
		} else if (repairSystem == CampaignOptions.REPAIR_SYSTEM_WARCHEST_CUSTOM) {
			runDiagnosticWarchest();
		} else if (repairSystem == CampaignOptions.REPAIR_SYSTEM_GENERIC_PARTS) {
			runDiagnosticGenericSpareParts();
		}
		*/
	}
	
	public ArrayList<Part> getPartsNeedingFixing() {
		ArrayList<Part> brokenParts = new ArrayList<Part>();
		for(Part part: parts) {
			if(part.needsFixing() || part.isSalvaging()) {
				brokenParts.add(part);
			}
		}
		return brokenParts;
	}

	public void removePart(Part part) {
		parts.remove(part);
	}
	
	/**
	 * @param m
	 *            - A Mounted class to find crits for
	 * @return the number of crits exising for this Mounted
	 */
	public int getCrits(Mounted m) {
		// TODO: I should probably just add this method to Entity in MM
		int hits = 0;
		for (int loc = 0; loc < entity.locations(); loc++) {
			for (int i = 0; i < entity.getNumberOfCriticals(loc); i++) {
				CriticalSlot slot = entity.getCritical(loc, i);
				// ignore empty & system slots
				if ((slot == null)
						|| (slot.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
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
		if (hasPilot()) {
			getPilot().setAssignedUnit(null);
			setPilot(null);
		}
	}

	public String getPilotDesc() {
		if (hasPilot()) {
			return entity.getCrew().getName() + " "
					+ entity.getCrew().getGunnery() + "/"
					+ entity.getCrew().getPiloting();
		}
		return "NO PILOT";
	}

	/**
	 * produce a string in HTML that can be embedded in larger reports
	 */
	public String getDescHTML() {
		String toReturn = "<b>" + entity.getDisplayName() + "</b><br/>";
		toReturn += getPilotDesc() + "<br/>";
		if (isDeployed()) {
			toReturn += "DEPLOYED!<br/>";
		} else {
			toReturn += "Site: " + getCurrentSiteName() + "<br/>";
		}
		return toReturn;
	}

	public TargetRoll getSiteMod() {
		switch (site) {
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
		switch (loc) {
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
		return (pilot != null && pilot.isDeployed());
	}

	public String checkDeployment() {
		if (!isFunctional()) {
			return "unit is not functional";
		}
		if (!hasPilot()) {
			return "unit has no pilot";
		}
		return null;
	}

	/**
	 * Have to make one here because the one in MegaMek only returns true if
	 * operable
	 * 
	 * @return
	 */
	public boolean hasTSM() {
		for (Mounted mEquip : entity.getMisc()) {
			MiscType mtype = (MiscType) mEquip.getType();
			 //TODO: I am getting some NPEs for mtype here in some saved games
            //on further investigation it appears that some tasks are not being 
            //removed for entities that are no longer part of the game, so their 
            //equipment types are not restored when the game is loaded
			//I should be able to check for null mtypes without problem
			//but I need to figure out why tasks for removed entities are hanging
			//around when the removeUnit method explicitly removes them
			if (null != mtype && mtype.hasFlag(MiscType.F_TSM)) {
				return true;
			}
		}
		return false;
	}

	public void damageSystem(int type, int slot) {
		for (int loc = 0; loc < getEntity().locations(); loc++) {
			for (int i = 0; i < getEntity().getNumberOfCriticals(loc); i++) {
				CriticalSlot cs = getEntity().getCritical(loc, i);
				// ignore empty & system slots
				if ((cs == null) || (cs.getType() != type)) {
					continue;
				}
				if (cs.getIndex() == slot) {
					cs.setHit(true);
					cs.setDestroyed(true);
					cs.setRepairable(true);
				}
			}
		}
	}
	
	public void hitSystem(int type, int slot) {
		for (int loc = 0; loc < getEntity().locations(); loc++) {
			for (int i = 0; i < getEntity().getNumberOfCriticals(loc); i++) {
				CriticalSlot cs = getEntity().getCritical(loc, i);
				// ignore empty & system slots
				if ((cs == null) || (cs.getType() != type)) {
					continue;
				}
				if (cs.getIndex() == slot && !cs.isDestroyed()) {
					cs.setHit(true);
					cs.setDestroyed(true);
					cs.setRepairable(true);
					return;
				}
			}
		}
	}

	public void destroySystem(int type, int slot) {
		for (int loc = 0; loc < getEntity().locations(); loc++) {
			for (int i = 0; i < getEntity().getNumberOfCriticals(loc); i++) {
				CriticalSlot cs = getEntity().getCritical(loc, i);
				// ignore empty & system slots
				if ((cs == null) || (cs.getType() != type)) {
					continue;
				}
				if (cs.getIndex() == slot) {
					cs.setHit(true);
					cs.setDestroyed(true);
					cs.setRepairable(false);
				}
			}
		}
	}

	public void repairSystem(int type, int slot) {
		for (int loc = 0; loc < getEntity().locations(); loc++) {
			for (int i = 0; i < getEntity().getNumberOfCriticals(loc); i++) {
				CriticalSlot cs = getEntity().getCritical(loc, i);
				// ignore empty & system slots
				if ((cs == null) || (cs.getType() != type)) {
					continue;
				}
				if (cs.getIndex() == slot) {
					cs.setHit(false);
					cs.setDestroyed(false);
					cs.setRepairable(true);
				}
			}
		}
	}

	public void damageSystem(int type, int slot, int loc) {
		for (int i = 0; i < getEntity().getNumberOfCriticals(loc); i++) {
			CriticalSlot cs = getEntity().getCritical(loc, i);
			// ignore empty & system slots
			if ((cs == null) || (cs.getType() != type)) {
				continue;
			}
			if (cs.getIndex() == slot) {
				cs.setHit(true);
				cs.setDestroyed(true);
				cs.setRepairable(true);
			}
		}
	}
	
	public void hitSystem(int type, int slot, int loc) {
		for (int i = 0; i < getEntity().getNumberOfCriticals(loc); i++) {
			CriticalSlot cs = getEntity().getCritical(loc, i);
			// ignore empty & system slots
			if ((cs == null) || (cs.getType() != type)) {
				continue;
			}
			if (cs.getIndex() == slot  && !cs.isDestroyed()) {
				cs.setHit(true);
				cs.setDestroyed(true);
				cs.setRepairable(true);
				return;
			}
		}
	}
		

	public void destroySystem(int type, int slot, int loc) {
		for (int i = 0; i < getEntity().getNumberOfCriticals(loc); i++) {
			CriticalSlot cs = getEntity().getCritical(loc, i);
			// ignore empty & system slots
			if ((cs == null) || (cs.getType() != type)) {
				continue;
			}
			if (cs.getIndex() == slot) {
				cs.setHit(true);
				cs.setDestroyed(true);
				cs.setRepairable(false);
			}
		}
	}

	public void repairSystem(int type, int slot, int loc) {
		for (int i = 0; i < getEntity().getNumberOfCriticals(loc); i++) {
			CriticalSlot cs = getEntity().getCritical(loc, i);
			// ignore empty & system slots
			if ((cs == null) || (cs.getType() != type)) {
				continue;
			}
			if (cs.getIndex() == slot) {
				cs.setHit(false);
				cs.setDestroyed(false);
				cs.setRepairable(true);
			}
		}
	}

	public boolean isDamaged() {
		return getDamageState() != Unit.STATE_UNDAMAGED;
	}

	public String getHeatSinkTypeString() {
		BigInteger heatSinkType = MiscType.F_HEAT_SINK;
		boolean heatSinkIsClanTechBase = false;

		for (Mounted mounted : getEntity().getEquipment()) {
			// Also goes through heat sinks inside the engine
			EquipmentType etype = mounted.getType();
			boolean isHeatSink = false;

			if (etype instanceof MiscType) {
				if (etype.hasFlag(MiscType.F_LASER_HEAT_SINK)) {
					heatSinkType = MiscType.F_LASER_HEAT_SINK;
					isHeatSink = true;
				} else if (etype.hasFlag(MiscType.F_DOUBLE_HEAT_SINK)) {
					heatSinkType = MiscType.F_DOUBLE_HEAT_SINK;
					isHeatSink = true;
				} else if (etype.hasFlag(MiscType.F_HEAT_SINK)) {
					heatSinkType = MiscType.F_HEAT_SINK;
					isHeatSink = true;
				}
			}

			if (isHeatSink) {
				if (TechConstants.getTechName(etype.getTechLevel()).equals(
						"Inner Sphere"))
					heatSinkIsClanTechBase = false;
				else if (TechConstants.getTechName(etype.getTechLevel())
						.equals("Clan"))
					heatSinkIsClanTechBase = true;
				break;
			}
		}

		String heatSinkTypeString = heatSinkIsClanTechBase ? "(CL) " : "(IS) ";
		if (heatSinkType == MiscType.F_LASER_HEAT_SINK)
			heatSinkTypeString += "Laser Heat Sink";
		else if (heatSinkType == MiscType.F_DOUBLE_HEAT_SINK)
			heatSinkTypeString += "Double Heat Sink";
		else if (heatSinkType == MiscType.F_HEAT_SINK)
			heatSinkTypeString += "Heat Sink";

		return heatSinkTypeString;
	}

	public void customize(Entity targetEntity, Campaign campaign) {
	/*	Unit sourceUnit = this;
		Entity currentEntity = sourceUnit.getEntity();

		ArrayList<SalvageItem> salvageItems = new ArrayList<SalvageItem>();

		ArrayList<WorkItem> salvage = campaign
				.getSalvageTasksForUnit(sourceUnit.getId());

		campaign.addUnit(targetEntity, false);

		Unit targetUnit = campaign.getUnits().get(
				campaign.getUnits().size() - 1);
		targetEntity = targetUnit.getEntity();

		ArrayList<Mounted> targetMountedToDestroy = new ArrayList<Mounted>();

		int refitClass = Refit.REFIT_CLASS_A;

		int refitCost = 0;

		boolean hasWeaponRemoval = false; // A
		boolean hasWeaponReplacementClassA = false; // A
		boolean hasWeaponReplacementClassB = false; // B
		boolean hasWeaponReplacementClassC = false; // C
		boolean hasOtherEquipmentReplacement = false; // C
		boolean hasOtherEquipmentRemoval = false; // C
		boolean hasHeatSinkAddition = false; // C
		boolean hasAmmunitionAddition = false; // C
		boolean hasCaseInstall = false; // E
		boolean hasOtherEquipmentAddition = false; // D

		// Add to salvageTasks all salvage from current unit which needs to be
		// removed
		for (int currentLocation = 0; currentLocation < currentEntity
				.locations(); currentLocation++) {
			// If current structure type != target structure type, get salvage
			// for location and armor
			if (currentEntity.getStructureType() != targetEntity
					.getStructureType()) {
				for (WorkItem task : salvage) {
					if (task instanceof LocationSalvage
							&& ((LocationSalvage) task).getLoc() == currentLocation)
						salvageItems.add((SalvageItem) task);
					else if (task instanceof ArmorSalvage
							&& ((ArmorSalvage) task).getLoc() == currentLocation)
						salvageItems.add((SalvageItem) task);
				}

				if (currentLocation != Mech.LOC_CT)
					targetEntity.setInternal(IArmorState.ARMOR_DESTROYED,
							currentLocation);
				else
					targetEntity.setInternal(1, currentLocation);
				targetEntity.setArmor(0, currentLocation, false);
				if (targetEntity.hasRearArmor(currentLocation))
					targetEntity.setArmor(0, currentLocation, true);

				if (Refit.REFIT_CLASS_F > refitClass)
					refitClass = Refit.REFIT_CLASS_F;

				// If current armor type != target armor type or if current max
				// armor != target max armor, get salvage for armor
			} else if (currentEntity.getArmorType(currentLocation) != targetEntity
					.getArmorType(currentLocation)
					|| currentEntity.getOArmor(currentLocation, false) != targetEntity
							.getOArmor(currentLocation, false)
					|| (currentEntity.hasRearArmor(currentLocation) && (currentEntity
							.getOArmor(currentLocation, true) != targetEntity
							.getOArmor(currentLocation, true)))) {
				for (WorkItem task : salvage) {
					if (task instanceof ArmorSalvage
							&& ((ArmorSalvage) task).getLoc() == currentLocation)
						salvageItems.add((SalvageItem) task);
				}

				targetEntity.setArmor(0, currentLocation, false);
				if (targetEntity.hasRearArmor(currentLocation))
					targetEntity.setArmor(0, currentLocation, true);

				if (Refit.REFIT_CLASS_C > refitClass)
					refitClass = Refit.REFIT_CLASS_C;
			}
		}

		// If current engine type != target engine type, get salvage for engine
		if (!currentEntity.getEngine().getEngineName()
				.equals(targetEntity.getEngine().getEngineName())) {
			for (WorkItem task : salvage) {
				if (task instanceof MekEngineSalvage)
					salvageItems.add((SalvageItem) task);
			}
			targetUnit.destroySystem(CriticalSlot.TYPE_SYSTEM,
					Mech.SYSTEM_ENGINE);

			if (currentEntity.getEngine().getEngineType() != targetEntity
					.getEngine().getEngineType()) {
				// Change engine type
				if (Refit.REFIT_CLASS_F > refitClass)
					refitClass = Refit.REFIT_CLASS_F;
			} else {
				// Change engine rating
				if (Refit.REFIT_CLASS_D > refitClass)
					refitClass = Refit.REFIT_CLASS_D;
			}
		}

		// If current gyro type != target gyro type, get salvage for gyro
		if (currentEntity.getGyroType() != targetEntity.getGyroType()) {
			for (WorkItem task : salvage) {
				if (task instanceof MekGyroSalvage)
					salvageItems.add((SalvageItem) task);
			}
			targetUnit
					.destroySystem(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO);
			if (Refit.REFIT_CLASS_F > refitClass)
				refitClass = Refit.REFIT_CLASS_F;
		}

		if (!sourceUnit.getHeatSinkTypeString().equals(
				new Unit(targetEntity, null).getHeatSinkTypeString())) {
			// Heat sink type change
			if (Refit.REFIT_CLASS_D > refitClass)
				refitClass = Refit.REFIT_CLASS_D;
		}

		ArrayList<Mounted> salvageMountedList = new ArrayList<Mounted>();

		// List of all mounted, one per pair Mounted type / Mounted location for
		// current mech
		ArrayList<Mounted> listAllUniqueMounted = new ArrayList<Mounted>();
		// Quantity of mounted of a given type in a given location for current
		// mech
		ArrayList<Integer> listAllUniqueMountedNumber = new ArrayList<Integer>();
		for (Mounted currentMounted : currentEntity.getEquipment()) {
			int uniqueIndex = -1;
			for (int i = 0; i < listAllUniqueMounted.size(); i++) {
				Mounted uniqueMounted = listAllUniqueMounted.get(i);
				if (Utilities.compareMounted(uniqueMounted, currentMounted)) {
					uniqueIndex = i;
					break;
				}
			}
			if (uniqueIndex > -1)
				listAllUniqueMountedNumber
						.set(uniqueIndex,
								listAllUniqueMountedNumber.get(uniqueIndex)
										.intValue() + 1);
			else {
				listAllUniqueMounted.add(currentMounted);
				listAllUniqueMountedNumber.add(1);
			}
		}

		// List of all mounted, one per pair Mounted type / Mounted location for
		// target mech
		ArrayList<Mounted> listAllUniqueMountedTarget = new ArrayList<Mounted>();
		// Quantity of mounted of a given type in a given location for target
		// mech
		ArrayList<Integer> listAllUniqueMountedTargetNumber = new ArrayList<Integer>();
		for (Mounted currentMountedTarget : targetEntity.getEquipment()) {
			int uniqueIndex = -1;
			for (int i = 0; i < listAllUniqueMountedTarget.size(); i++) {
				Mounted uniqueMountedTarget = listAllUniqueMountedTarget.get(i);
				if (Utilities.compareMounted(uniqueMountedTarget,
						currentMountedTarget)) {
					uniqueIndex = i;
					break;
				}
			}
			if (uniqueIndex > -1)
				listAllUniqueMountedTargetNumber.set(uniqueIndex,
						listAllUniqueMountedTargetNumber.get(uniqueIndex)
								.intValue() + 1);
			else {
				listAllUniqueMountedTarget.add(currentMountedTarget);
				listAllUniqueMountedTargetNumber.add(1);
			}
		}

		ArrayList<Mounted> listAllUniqueMountedOrig = new ArrayList<Mounted>(
				listAllUniqueMounted);
		ArrayList<Mounted> listAllUniqueMountedTargetOrig = new ArrayList<Mounted>(
				listAllUniqueMountedTarget);

		for (Mounted currentMounted : currentEntity.getEquipment()) {

			// Skip non hitable components
			if (!currentMounted.getType().isHittable())
				continue;

			// Nb of mounted of this type (currentMounted.getType()) in this
			// location (currentMounted.getLocation()) for target
			int nbEquipmentInLocationTarget = 0;
			// Nb of mounted of any type in this location
			// (currentMounted.getLocation()) for target
			int targetIndex = -1;
			for (int i = 0; i < listAllUniqueMountedTarget.size(); i++) {
				Mounted uniqueMounted = listAllUniqueMountedTarget.get(i);
				if (Utilities.compareMounted(currentMounted, uniqueMounted)) {
					nbEquipmentInLocationTarget = listAllUniqueMountedTargetNumber
							.get(i);
					targetIndex = i;
					break;
				}
			}

			// Nb of mounted of this type (currentMounted.getType()) in this
			// location (currentMounted.getLocation()) for current
			int nbEquipmentInLocation = 0;
			// Nb of mounted of any type in this location
			// (currentMounted.getLocation()) for current
			int currentIndex = -1;
			for (int i = 0; i < listAllUniqueMounted.size(); i++) {
				Mounted uniqueMounted = listAllUniqueMounted.get(i);
				if (Utilities.compareMounted(currentMounted, uniqueMounted)) {
					nbEquipmentInLocation = listAllUniqueMountedNumber.get(i);
					currentIndex = i;
					break;
				}
			}

			if (nbEquipmentInLocation > nbEquipmentInLocationTarget) {
				// Current has more of this component in this location than
				// target
				salvageMountedList.add(currentMounted);
				listAllUniqueMountedNumber
						.set(currentIndex,
								listAllUniqueMountedNumber.get(currentIndex)
										.intValue() - 1);
			}
		}

		for (Mounted targetMounted : targetEntity.getEquipment()) {

			// Skip non hitable components
			if (!targetMounted.getType().isHittable())
				continue;

			// Nb of mounted of this type (currentMounted.getType()) in this
			// location (currentMounted.getLocation()) for target
			int nbEquipmentInLocationTarget = 0;
			// Nb of mounted of any type in this location
			// (currentMounted.getLocation()) for target
			int targetIndex = -1;
			for (int i = 0; i < listAllUniqueMountedTarget.size(); i++) {
				Mounted uniqueMounted = listAllUniqueMountedTarget.get(i);
				if (Utilities.compareMounted(targetMounted, uniqueMounted)) {
					nbEquipmentInLocationTarget = listAllUniqueMountedTargetNumber
							.get(i);
					targetIndex = i;
					break;
				}
			}

			// Nb of mounted of this type (currentMounted.getType()) in this
			// location (currentMounted.getLocation()) for current
			int nbEquipmentInLocation = 0;
			// Nb of mounted of any type in this location
			// (currentMounted.getLocation()) for current
			int currentIndex = -1;
			for (int i = 0; i < listAllUniqueMounted.size(); i++) {
				Mounted uniqueMounted = listAllUniqueMounted.get(i);
				if (Utilities.compareMounted(targetMounted, uniqueMounted)) {
					nbEquipmentInLocation = listAllUniqueMountedNumber.get(i);
					currentIndex = i;
					break;
				}
			}

			if (nbEquipmentInLocationTarget > nbEquipmentInLocation) {
				// Target has more of this component in this location than
				// target
				targetMountedToDestroy.add(targetMounted);
				listAllUniqueMountedTargetNumber.set(targetIndex,
						listAllUniqueMountedTargetNumber.get(targetIndex)
								.intValue() - 1);
			}
		}

		for (Mounted salvageMounted : salvageMountedList) {
			for (WorkItem task : salvage) {
				if (task instanceof EquipmentSalvage
						&& ((EquipmentSalvage) task).getMounted().equals(
								salvageMounted)
						&& ((EquipmentSalvage) task).getMounted().getLocation() == salvageMounted
								.getLocation())
					salvageItems.add((SalvageItem) task);
			}

			// For every piece of equipment removed, see if it is a weapon
			// removal, an other removal, a wepon replacement or another
			// replacement
			int location = salvageMounted.getLocation();

			/*
			 * int numberOfCritInLocationForCurrent = 0; int
			 * numberOfCritInLocationForTarget = 0; for (int
			 * currentSlot=0;currentSlot
			 * <currentEntity.getNumberOfCriticals(location);currentSlot++) {
			 * CriticalSlot currentCriticalSlot =
			 * currentEntity.getCritical(location, currentSlot); if
			 * (currentCriticalSlot!=null &&
			 * currentCriticalSlot.getMount()!=null)
			 * numberOfCritInLocationForCurrent++;
			 * 
			 * CriticalSlot targetCriticalSlot =
			 * targetEntity.getCritical(location, currentSlot); if
			 * (targetCriticalSlot!=null && targetCriticalSlot.getMount()!=null)
			 * numberOfCritInLocationForTarget++; }
			 */
/*
			int nbItemsInLocation = 0;
			for (int i = 0; i < listAllUniqueMountedOrig.size(); i++) {
				Mounted uniqueMounted = listAllUniqueMountedOrig.get(i);
				if (uniqueMounted.getLocation() == location)
					nbItemsInLocation++;
			}
			int nbItemsInLocationTarget = 0;
			for (int i = 0; i < listAllUniqueMountedTargetOrig.size(); i++) {
				Mounted uniqueMounted = listAllUniqueMountedTargetOrig.get(i);
				if (uniqueMounted.getLocation() == location)
					nbItemsInLocationTarget++;
			}

			if (salvageMounted.getName().contains("Heat Sink")) {
				continue;
			} else if (salvageMounted.getType() instanceof AmmoType) {
				continue;
			} else if (salvageMounted.getName().contains("CASE")
					|| salvageMounted.getName().contains("Case")) {
				continue;
			}

			/*
			 * if (numberOfCritInLocationForCurrent >
			 * numberOfCritInLocationForTarget) { // More crits in current
			 * location than in target location : removal
			 */
		/*	if (nbItemsInLocation > nbItemsInLocationTarget) {
				// More items in current location than in target : removal
				if (salvageMounted.getType() instanceof WeaponType) {
					hasWeaponRemoval = true;
				} else {
					hasOtherEquipmentRemoval = true;
				}
			} else {
				// Less or same items in current location than in target :
				// replacement
				if (salvageMounted.getType() instanceof WeaponType) {
					// Weapon replacement class A, B or C
					// Done below
					hasWeaponRemoval = true;
				} else {
					hasOtherEquipmentReplacement = true;
				}
			}
		}

		campaign.removeAllTasksFor(sourceUnit);
		for (SalvageItem salvageItem : salvageItems) {
			campaign.addWork(salvageItem);
		}

		for (Mounted mountedToDestroy : targetMountedToDestroy) {
			// Go through locations
			for (int currentLocation = 0; currentLocation < currentEntity
					.locations(); currentLocation++) {
				// Go through critical slots
				for (int currentSlot = 0; currentSlot < currentEntity
						.getNumberOfCriticals(currentLocation); currentSlot++) {
					CriticalSlot targetCriticalSlot = targetEntity.getCritical(
							currentLocation, currentSlot);

					if (targetCriticalSlot == null)
						continue;

					Mounted targetMounted = targetCriticalSlot.getMount();

					if (targetMounted == null)
						continue;

					if (targetMounted.equals(mountedToDestroy)) {
						// New mounted
						targetCriticalSlot.setHit(true);
						targetCriticalSlot.setDestroyed(true);
						targetCriticalSlot.setRepairable(false);
						targetMounted.setHit(true);
						targetMounted.setDestroyed(true);
						targetMounted.setRepairable(false);
					}
				}
			}

			// For every piece of equipment added, see if it is a heat sink
			// addition, a case addition or an other addition, a wepon
			// replacement or another replacement
			int location = mountedToDestroy.getLocation();

			/*
			 * int numberOfCritInLocationForCurrent = 0; int
			 * numberOfCritInLocationForTarget = 0; for (int
			 * currentSlot=0;currentSlot
			 * <currentEntity.getNumberOfCriticals(location);currentSlot++) {
			 * CriticalSlot currentCriticalSlot =
			 * currentEntity.getCritical(location, currentSlot); if
			 * (currentCriticalSlot!=null &&
			 * currentCriticalSlot.getMount()!=null)
			 * numberOfCritInLocationForCurrent++;
			 * 
			 * CriticalSlot targetCriticalSlot =
			 * targetEntity.getCritical(location, currentSlot); if
			 * (targetCriticalSlot!=null && targetCriticalSlot.getMount()!=null)
			 * numberOfCritInLocationForTarget++; }
			 */
/*
			int nbItemsInLocation = 0;
			for (int i = 0; i < listAllUniqueMountedOrig.size(); i++) {
				Mounted uniqueMounted = listAllUniqueMountedOrig.get(i);
				if (uniqueMounted.getLocation() == location)
					nbItemsInLocation++;
			}
			int nbItemsInLocationTarget = 0;
			for (int i = 0; i < listAllUniqueMountedTargetOrig.size(); i++) {
				Mounted uniqueMounted = listAllUniqueMountedTargetOrig.get(i);
				if (uniqueMounted.getLocation() == location)
					nbItemsInLocationTarget++;
			}

			if (mountedToDestroy.getName().contains("Heat Sink")) {
				hasHeatSinkAddition = true;
				continue;
			} else if (mountedToDestroy.getType() instanceof AmmoType) {
				hasAmmunitionAddition = true;
				continue;
			} else if (mountedToDestroy.getName().contains("CASE")
					|| mountedToDestroy.getName().contains("Case")) {
				hasCaseInstall = true;
				continue;
			}

			/*
			 * if (numberOfCritInLocationForTarget >
			 * numberOfCritInLocationForCurrent) { // More crits in target
			 * location than in current location : addition
			 */
	/*		if (nbItemsInLocationTarget > nbItemsInLocation) {
				// More items in target location than in current location :
				// addition
				// Strictly speaking, they're not all additions but one is
				// enough to increase refit class to D
				hasOtherEquipmentAddition = true;
			} else {
				// Less or same number of items in target location than in
				// current location : replacement
				if (mountedToDestroy.getType() instanceof WeaponType) {
					// Weapon replacement class A, B or C
					// Find out the class
					boolean hasLargerWeaponOfSameClass = false;
					boolean hasLargerWeaponOfOtherClass = false;
					for (Mounted mountedSalvage : salvageMountedList) {
						int mountedSalvageNbCrits = mountedSalvage.getType()
								.getCriticals(currentEntity);
						int mountedToDestroyNbCrits = mountedToDestroy
								.getType().getCriticals(targetEntity);
						if (mountedSalvage.getLocation() == location
								&& mountedSalvageNbCrits >= mountedToDestroyNbCrits) {
							if (mountedSalvage.getType() instanceof WeaponType
									&& ((WeaponType) mountedSalvage.getType())
											.getAtClass() == ((WeaponType) mountedToDestroy
											.getType()).getAtClass())
								hasLargerWeaponOfSameClass = true;
							else
								hasLargerWeaponOfOtherClass = true;
						}
					}
					if (hasLargerWeaponOfSameClass)
						hasWeaponReplacementClassA = true;
					else if (hasLargerWeaponOfOtherClass)
						hasWeaponReplacementClassB = true;
					else
						hasWeaponReplacementClassC = true;

				} else {
					hasOtherEquipmentReplacement = true;
				}
			}

		}

		campaign.removeAllTasksFor(targetUnit);
		targetUnit.runDiagnostic();

		ArrayList<WorkItem> unitTasks = campaign.getTasksForUnit(targetUnit
				.getId());
		int totalRepairTime = 0;
		int refitKitAvailability = EquipmentType.RATING_A;
		int refitKitAvailabilityMod = 0;
		for (WorkItem unitTask : unitTasks) {
			if (unitTask instanceof RepairItem
					|| unitTask instanceof ReplacementItem
					|| unitTask instanceof ReloadItem) {
				totalRepairTime += unitTask.getTimeLeft();
				if (unitTask instanceof ReplacementItem) {
					Part part = ((ReplacementItem) unitTask).partNeeded();
					// Part availability
					int availability = part.getAvailability(campaign.getEra());

					// Faction and Tech mod
					int factionMod = 0;
					if (campaign.getCampaignOptions().useFactionModifiers()) {
						factionMod = Availability.getFactionAndTechMod(part, campaign);
					}

					if (Availability.getAvailabilityModifier(availability)
							+ factionMod > Availability.getAvailabilityModifier(refitKitAvailability)
							+ refitKitAvailabilityMod) {
						refitKitAvailability = availability;
						refitKitAvailabilityMod = factionMod;
					}

					refitCost += ((ReplacementItem) unitTask).partNeeded()
							.getCost();
				} else if (unitTask instanceof ReloadItem) {
					refitCost += ((ReloadItem) unitTask).getCost();
				}
			}
		}

		// Refit class
		if (hasCaseInstall && Refit.REFIT_CLASS_E > refitClass)
			refitClass = Refit.REFIT_CLASS_E;
		if (hasOtherEquipmentAddition && Refit.REFIT_CLASS_D > refitClass)
			refitClass = Refit.REFIT_CLASS_D;
		if ((hasAmmunitionAddition || hasHeatSinkAddition
				|| hasOtherEquipmentRemoval || hasOtherEquipmentReplacement || hasWeaponReplacementClassC)
				&& Refit.REFIT_CLASS_C > refitClass)
			refitClass = Refit.REFIT_CLASS_C;
		if (hasWeaponReplacementClassB && Refit.REFIT_CLASS_B > refitClass)
			refitClass = Refit.REFIT_CLASS_B;
		if ((hasWeaponReplacementClassA || hasWeaponRemoval)
				&& Refit.REFIT_CLASS_A > refitClass)
			refitClass = Refit.REFIT_CLASS_A;

		campaign.removeUnit(targetUnit.getId());

		refitCost = (int) Math.round(refitCost * 1.1);

		MechRefit mechRefit = new MechRefit(sourceUnit, targetEntity,
				totalRepairTime, refitClass, refitKitAvailability,
				refitKitAvailabilityMod, refitCost);
		MechCustomization mechCustomization = new MechCustomization(sourceUnit,
				targetEntity, totalRepairTime, refitClass);

		int repairSystem = campaign.getCampaignOptions().getRepairSystem();
		if (repairSystem == CampaignOptions.REPAIR_SYSTEM_STRATOPS) {
			campaign.addWork(mechRefit);
			campaign.addWork(mechCustomization);
		} else if (repairSystem == CampaignOptions.REPAIR_SYSTEM_WARCHEST_CUSTOM) {

		} else if (repairSystem == CampaignOptions.REPAIR_SYSTEM_GENERIC_PARTS) {
			campaign.addWork(mechCustomization);
		}
		*/
	}

	public void cancelCustomize(Campaign campaign) {
	/*	Unit sourceUnit = this;

		campaign.removeAllTasksFor(sourceUnit);
		sourceUnit.runDiagnostic();
		*/
	}

	public int getRepairCost() {
		int cost = 0;
	/*
		int repairSystem = campaign.getCampaignOptions().getRepairSystem();

		for (WorkItem task : campaign.getAllTasksForUnit(getId())) {
			if (repairSystem == CampaignOptions.REPAIR_SYSTEM_STRATOPS) {
				if (task instanceof ReplacementItem) {
					cost += ((ReplacementItem) task).partNeeded().getCost();
				} else if (task instanceof ReloadItem) {
					cost += ((ReloadItem) task).getCost();
				}
			} else if (repairSystem == CampaignOptions.REPAIR_SYSTEM_WARCHEST_CUSTOM) {
				if (task instanceof FullRepairWarchest) {
					cost += ((FullRepairWarchest) task).getCost();
				}
			} else if (repairSystem == CampaignOptions.REPAIR_SYSTEM_GENERIC_PARTS) {
				if (task instanceof ReplacementItem
						&& ((ReplacementItem) task).partNeeded() instanceof GenericSparePart) {
					cost += ((ReplacementItem) task).partNeeded().getCost();
				}
			}
		}
*/
		return cost;
	}

	public int getSellValue() {
		int residualValue = 0;

		int valueOfSalvage = 0;
		/*
		for (WorkItem task : campaign.getAllTasksForUnit(getId())) {
			if (task instanceof SalvageItem) {
				valueOfSalvage += ((SalvageItem) task).getPart().getCost();
			}
		}

		if (!isRepairable()) {
			// The value of a truly destroyed entity is equals to the value of
			// its parts
			residualValue = valueOfSalvage;
		} else {
			// The value of a repairable entity is equals to its cost minus its
			// repair cost
			int cost = (int) Math.round(getEntity().getCost(false));

			// Increase cost for IS players buying Clan mechs
			if (TechConstants.getTechName(getEntity().getTechLevel()).equals(
					"Clan")
					&& !Faction.isClanFaction(campaign.getFaction()))
				cost *= campaign.getCampaignOptions().getClanPriceModifier();

			residualValue = cost - getRepairCost();

			if (valueOfSalvage > residualValue)
				residualValue = valueOfSalvage;
		}

		if (residualValue < 0)
			residualValue = 0;

		// Sell unit => divide price by 2
		 * 
		 */
		return residualValue / 2;
	}

	public int getBuyCost() {
		int cost = (int) Math.round(getEntity().getCost(false));

		// Increase cost for IS players buying Clan mechs
		if (TechConstants.getTechName(getEntity().getTechLevel())
				.equals("Clan")
				&& !Faction.isClanFaction(campaign.getFaction()))
			cost *= campaign.getCampaignOptions().getClanPriceModifier();

		return cost;
	}

	public int getDamageState() {

		if (getEntity() instanceof Mech) {
			Mech mech = (Mech) getEntity();

			int nbEngineCrits = 0;
			int nbGyroHit = 0;
			int nbSensorHits = 0;
			int nbLimbsWithInternalDamage = 0;
			int nbTorsoWithInternalDamage = 0;
			boolean hasDestroyedTorso = false;
			int nbWeaponsUnusable = 0;
			int nbCrits = 0;
			int nbLimbsWithArmorDamage = 0;

			if (mech.isLocationBad(Mech.LOC_LT)
					|| mech.isLocationBad(Mech.LOC_RT)
					|| mech.isLocationBad(Mech.LOC_CT))
				hasDestroyedTorso = true;

			for (int i = 0; i < mech.locations(); i++) {
				nbEngineCrits += mech.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
						Mech.SYSTEM_ENGINE, i);
				nbGyroHit += mech.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
						Mech.SYSTEM_GYRO, i);
				nbSensorHits += mech.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
						Mech.SYSTEM_SENSORS, i);
				if (mech.getInternal(i) < mech.getOInternal(i)) {
					nbLimbsWithInternalDamage++;
					if (i == Mech.LOC_LT || i == Mech.LOC_LT
							|| i == Mech.LOC_RT)
						nbTorsoWithInternalDamage++;
				}
				if (mech.getArmor(i) < mech.getOArmor(i))
					nbLimbsWithArmorDamage++;
				if (mech.hasRearArmor(i)) {
					if (mech.getArmor(i, true) < mech.getOArmor(i, true))
						nbLimbsWithArmorDamage++;
				}
			}

			Iterator<Mounted> itWeapons = mech.getWeapons();
			while (itWeapons.hasNext()) {
				Mounted weapon = itWeapons.next();
				if (weapon.isInoperable())
					nbWeaponsUnusable++;
			}

			for (int loc = 0; loc < mech.locations(); loc++) {
				int nbCriticalSlots = mech.getNumberOfCriticals(loc);
				for (int crit = 0; crit < nbCriticalSlots; crit++) {
					CriticalSlot criticalSlot = mech.getCritical(loc, crit);
					if (criticalSlot != null) {
						if (criticalSlot.isDamaged() || criticalSlot.isHit()
								|| criticalSlot.isDestroyed())
							nbCrits++;
					}
				}
			}

			if (hasDestroyedTorso || (nbEngineCrits >= 2)
					|| (nbEngineCrits == 1 && nbGyroHit >= 1)
					|| (nbSensorHits >= 2) || (nbLimbsWithInternalDamage >= 3)
					|| (nbTorsoWithInternalDamage >= 2)
					|| (nbWeaponsUnusable >= mech.getWeaponList().size())) {
				return Unit.STATE_CRIPPLED;
			} else if (nbLimbsWithInternalDamage >= 1 || nbCrits >= 1) {
				return Unit.STATE_HEAVY_DAMAGE;
			} else if (nbLimbsWithArmorDamage >= 1) {
				return Unit.STATE_LIGHT_DAMAGE;
			} else {
				return Unit.STATE_UNDAMAGED;
			}
		} else if (getEntity() instanceof Tank) {
			Tank tank = (Tank) getEntity();

			int nbWeaponsDestroyed = 0;
			int nbLimbsWithArmorDamage = 0;
			int nbLimbsWithInternalDamage = 0;
			int nbLimbsWithAllArmorDestroyed = 0;
			int nbCrits = 0;

			for (int i = 0; i < tank.locations(); i++) {
				if (tank.getInternal(i) < tank.getOInternal(i)) {
					nbLimbsWithInternalDamage++;
				}

				if (tank.getArmor(i) < tank.getOArmor(i))
					nbLimbsWithArmorDamage++;

				if (tank.hasRearArmor(i)) {
					if (tank.getArmor(i, true) < tank.getOArmor(i, true))
						nbLimbsWithArmorDamage++;

					if (tank.getArmor(i, true) == 0
							&& tank.getOArmor(i, true) > 0)
						nbLimbsWithAllArmorDestroyed++;
				}

				if (tank.getArmor(i) == 0 && tank.getOArmor(i) > 0)
					nbLimbsWithAllArmorDestroyed++;
			}

			Iterator<Mounted> itWeapons = tank.getWeapons();
			while (itWeapons.hasNext()) {
				Mounted weapon = itWeapons.next();
				if (weapon.isInoperable())
					nbWeaponsDestroyed++;
			}

			for (int loc = 0; loc < tank.locations(); loc++) {
				int nbCriticalSlots = tank.getNumberOfCriticals(loc);
				for (int crit = 0; crit < nbCriticalSlots; crit++) {
					CriticalSlot criticalSlot = tank.getCritical(loc, crit);
					if (criticalSlot != null) {
						if (criticalSlot.isDamaged() || criticalSlot.isHit()
								|| criticalSlot.isDestroyed())
							nbCrits++;
					}
				}
			}

			if (nbLimbsWithAllArmorDestroyed >= 1
					|| nbWeaponsDestroyed >= tank.getWeaponList().size()) {
				return Unit.STATE_CRIPPLED;
			} else if (nbLimbsWithInternalDamage >= 1 || nbCrits >= 1) {
				return Unit.STATE_HEAVY_DAMAGE;
			} else if (nbLimbsWithArmorDamage >= 1) {
				return Unit.STATE_LIGHT_DAMAGE;
			} else {
				return Unit.STATE_UNDAMAGED;
			}
		} else {
			Entity entity = (Tank) getEntity();

			int nbDestroyedLocations = 0;
			int nbWeaponsDestroyed = 0;
			int nbLimbsWithArmorDamage = 0;
			int nbLimbsWithInternalDamage = 0;
			int nbCrits = 0;

			for (int i = 0; i < entity.locations(); i++) {
				if (entity.getInternal(i) < entity.getOInternal(i)) {
					nbLimbsWithInternalDamage++;
				}
				if (entity.getArmor(i) < entity.getOArmor(i))
					nbLimbsWithArmorDamage++;
				if (entity.hasRearArmor(i)) {
					if (entity.getArmor(i, true) < entity.getOArmor(i, true))
						nbLimbsWithArmorDamage++;
				}
			}

			Iterator<Mounted> itWeapons = entity.getWeapons();
			while (itWeapons.hasNext()) {
				Mounted weapon = itWeapons.next();
				if (weapon.isInoperable())
					nbWeaponsDestroyed++;
			}

			for (int loc = 0; loc < entity.locations(); loc++) {
				int nbCriticalSlots = entity.getNumberOfCriticals(loc);
				for (int crit = 0; crit < nbCriticalSlots; crit++) {
					CriticalSlot criticalSlot = entity.getCritical(loc, crit);
					if (criticalSlot != null) {
						if (criticalSlot.isDamaged() || criticalSlot.isHit()
								|| criticalSlot.isDestroyed())
							nbCrits++;
					}
				}
			}

			if (nbDestroyedLocations >= 1
					|| nbWeaponsDestroyed >= entity.getWeaponList().size()) {
				return Unit.STATE_CRIPPLED;
			} else if (nbLimbsWithInternalDamage >= 1 || nbCrits >= 1) {
				return Unit.STATE_HEAVY_DAMAGE;
			} else if (nbLimbsWithArmorDamage >= 1) {
				return Unit.STATE_LIGHT_DAMAGE;
			} else {
				return Unit.STATE_UNDAMAGED;
			}
		}
	}

	public int getFullBaseValueOfParts() {
		//Entity undamagedEntity = Campaign
			//	.getBrandNewUndamagedEntity(getEntity().getShortName());

		//if (undamagedEntity == null)
			//return -1;

		//Unit undamagedUnit = new Unit(undamagedEntity, campaign);
		//undamagedUnit.runDiagnosticStratOps();

		int cost = 0;
		/*
		for (WorkItem task : campaign.getAllTasksForUnit(undamagedUnit.getId())) {
			if (task instanceof SalvageItem) {
				cost += ((SalvageItem) task).getPart().getCost();
			}
		}*/

		return cost;
	}

	public void writeToXml(PrintWriter pw1, int indentLvl, int id) {
		pw1.println(MekHqXmlUtil.indentStr(indentLvl) + "<unit id=\"" + id
				+ "\" type=\"" + this.getClass().getName() + "\">");

		pw1.println(MekHqXmlUtil.writeEntityToXmlString(entity, indentLvl+1));
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<customized>"
				+ customized + "</customized>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<quality>"
				+ quality + "</quality>");
		
		// Units may not have a pilot!
		if (pilot != null) {
			pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<pilotId>"
					+ pilot.getId() + "</pilotId>");
		}
		
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<salvaged>"
				+ salvaged + "</salvaged>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<site>" + site
				+ "</site>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl) + "</unit>");
	}

	public static Unit generateInstanceFromXML(Node wn) {
		Unit retVal = new Unit();
		NamedNodeMap attrs = wn.getAttributes();
		Node idNode = attrs.getNamedItem("id");
		retVal.id = Integer.parseInt(idNode.getTextContent());
		
		// Okay, now load Part-specific fields!
		NodeList nl = wn.getChildNodes();

		try {
			for (int x=0; x<nl.getLength(); x++) {
				Node wn2 = nl.item(x);
				
				if (wn2.getNodeName().equalsIgnoreCase("pilotId")) {
					retVal.pilotId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("quality")) {
					retVal.quality = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("site")) {
					retVal.site = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("salvaged")) {
					if (wn2.getTextContent().equalsIgnoreCase("true"))
						retVal.salvaged = true;
					else
						retVal.salvaged = false;
				} else if (wn2.getNodeName().equalsIgnoreCase("customized")) {
					if (wn2.getTextContent().equalsIgnoreCase("true"))
						retVal.customized = true;
					else
						retVal.customized = false;
				} else if (wn2.getNodeName().equalsIgnoreCase("entity")) {
					retVal.entity = MekHqXmlUtil.getEntityFromXmlString(wn2);

					if ((retVal.id >= 0) && (retVal.entity != null)) {
						MekHQApp.logMessage("ID pre-defined and entity not null; setting entity's ID.", 5);
						retVal.entity.setId(retVal.id);
					} else if (retVal.entity != null) {
						MekHQApp.logMessage("ID not pre-defined and entity not null; setting unit's ID.", 5);
						retVal.id = retVal.entity.getId();
					}
				}
			}
		} catch (Exception ex) {
			// Doh!
			MekHQApp.logError(ex);
		}
		
		return retVal;
	}
	
	/**
     * This function returns an html-coded list that says what 
     * quirks are enabled for this unit
     * @return
     */
    public String getQuirksList() {
    	String quirkString = "";
    	for (Enumeration<IOptionGroup> i = getEntity().getQuirks().getGroups(); i.hasMoreElements();) {
            IOptionGroup group = i.nextElement();
            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
                IOption quirk = j.nextElement();
                if (quirk.booleanValue()) {
                	quirkString = quirkString + quirk.getDisplayableNameWithValue() + "<br>";
                }
            }
        }
        if(quirkString.equals("")) {
        	return null;
        }
        return "<html>" + quirkString + "</html>";
    }
    
    public void acquireQuirk(String name, Object value) {
    	for (Enumeration<IOption> i = getEntity().getQuirks().getOptions(); i.hasMoreElements();) {
        	IOption ability = i.nextElement();
        	if(ability.getName().equals(name)) {
        		ability.setValue(value);
        	}
    	}
    }
    
    public int getMaintenanceCost() {
    	Entity en = getEntity();
    	Boolean isOmni = en.isOmni();
    	if(en instanceof Mech) {
    		if(isOmni) {
    			return 100;
    		} else {
    			return 75;
    		}
    	} else if(en instanceof Warship) {
    		return 5000;
    	} else if(en instanceof Jumpship) {
    		return 800;
    	} else if(en instanceof Dropship) {
    		return 500;
    	} else if(en instanceof ConvFighter) {
    		return 50;
    	} else if(en instanceof Aero) {
    		if(isOmni) {
    			return 125;
    		} else  {
    			return 65;
    		}
    	} else if(en instanceof VTOL) {
    		return 65;
    	} else if(en instanceof Tank) {
    		return 25;
    	} else if(en instanceof BattleArmor) {
    		return ((BattleArmor)en).getTroopers() * 50;
    	} else if(en instanceof Infantry) {
    		return ((Infantry)en).getSquadN()*10;
    	}
    	return 0;
    }
    
    public void addPart(Part part) {
    	part.setUnit(this);
    	parts.add(part);
    }
    
    /**
     * This will check a unit for certain parts and if they are missing, it will create a new
     * version and update its condition. checking for existing parts makes this a more complicated
     * method but it also ensures that you can call this at any time and you won't overwrite existing
     * parts
     */
    public void initializeParts() {
    	if(getEntity() instanceof Mech) {
    		MekGyro gyro = null;
    		MekEngine engine = null;
    		MekLifeSupport lifeSupport = null;
    		MekSensor sensor = null;
    		MekActuator rightHand = null;
    		MekActuator rightLowerArm = null;
    		MekActuator rightUpperArm = null;
    		MekActuator leftHand = null;
    		MekActuator leftLowerArm = null;
    		MekActuator leftUpperArm = null;
    		MekActuator rightFoot = null;
    		MekActuator rightLowerLeg = null;
    		MekActuator rightUpperLeg = null;
    		MekActuator leftFoot = null;
    		MekActuator leftLowerLeg = null;
    		MekActuator leftUpperLeg = null;
    		MekLocation[] locations = new MekLocation[entity.locations()];
    		Armor[] armor = new Armor[entity.locations()];
    		Armor[] armorRear = new Armor[entity.locations()];
    		Hashtable<Integer,EquipmentPart> equipParts = new Hashtable<Integer,EquipmentPart>();
    		
    		for(Part part : parts) {
    			if(part instanceof MekGyro) {
    				gyro = (MekGyro)part;
    			} else if(part instanceof MekEngine) {
    				engine = (MekEngine)part;
    			} else if(part instanceof MekLifeSupport) {
    				lifeSupport = (MekLifeSupport)part;
    			} else if(part instanceof MekSensor) {
    				sensor = (MekSensor)part;
    			} else if(part instanceof MekLocation) {
    				locations[((MekLocation)part).getLoc()] = (MekLocation)part;
    			} else if(part instanceof Armor) {
    				if(((Armor)part).isRearMounted()) {
    					armorRear[((Armor)part).getLocation()] = (Armor)part;
    				} else {
    					armor[((Armor)part).getLocation()] = (Armor)part;
    				}
    			} else if(part instanceof EquipmentPart) {
    				equipParts.put(((EquipmentPart)part).getEquipmentNum(), (EquipmentPart)part);
    			} else if(part instanceof MekActuator) {
    				MekActuator actuator = (MekActuator)part;
    				if(actuator.getType() == Mech.ACTUATOR_UPPER_ARM) {
    					if(actuator.getLocation() == Mech.LOC_RARM) {
    						rightUpperArm = actuator;
    					} else {
    						leftUpperArm = actuator;
    					}
    				} else if(actuator.getType() == Mech.ACTUATOR_LOWER_ARM) {
    					if(actuator.getLocation() == Mech.LOC_RARM) {
    						rightLowerArm = actuator;
    					} else {
    						leftLowerArm = actuator;
    					}
    				} else if(actuator.getType() == Mech.ACTUATOR_HAND) {
    					if(actuator.getLocation() == Mech.LOC_RARM) {
    						rightHand = actuator;
    					} else {
    						leftHand = actuator;
    					}
    				} else if(actuator.getType() == Mech.ACTUATOR_UPPER_LEG) {
    					if(actuator.getLocation() == Mech.LOC_RLEG) {
    						rightUpperLeg = actuator;
    					} else {
    						leftUpperLeg = actuator;
    					}
    				} else if(actuator.getType() == Mech.ACTUATOR_LOWER_LEG) {
    					if(actuator.getLocation() == Mech.LOC_RLEG) {
    						rightLowerLeg = actuator;
    					} else {
    						leftLowerLeg = actuator;
    					}
    				} else if(actuator.getType() == Mech.ACTUATOR_FOOT) {
    					if(actuator.getLocation() == Mech.LOC_RLEG) {
    						rightFoot = actuator;
    					} else {
    						leftFoot = actuator;
    					}
    				}
    				
    			}
    		}
    		
    		//now check to see what is still null
    		if(null == gyro) {
    			gyro =  new MekGyro(false, (int) entity.getWeight(), entity.getGyroType(), entity.getOriginalWalkMP());
    			addPart(gyro);
    			campaign.addPart(gyro);
    		}
    		if(null == engine) {
    			engine = new MekEngine(false, (int) entity.getWeight(), campaign.getFaction(), entity.getEngine(), campaign.getCampaignOptions().getClanPriceModifier());
    			addPart(engine);
    			campaign.addPart(engine);
    		}
    		if(null == lifeSupport) {
    			lifeSupport = new MekLifeSupport(false, (int) entity.getWeight());
    			addPart(lifeSupport);
    			campaign.addPart(lifeSupport);
    		}
    		if(null == sensor) {
    			sensor = new MekSensor(false, (int) entity.getWeight());
    			addPart(sensor);
    			campaign.addPart(sensor);
    		}
    		for(int i = 0; i<locations.length; i++) {
    			if(null == locations[i]) {
    				MekLocation mekLocation = new MekLocation(false, i, (int) getEntity().getWeight(), getEntity().getStructureType(), hasTSM());
    				addPart(mekLocation);
    				campaign.addPart(mekLocation);
    			}
    			if(null == armor[i]) {
    				Armor mekArmor = new Armor(false, (int) getEntity().getWeight(), getEntity().getArmorType(i), getEntity().getOArmor(i, false), i, false);
    				addPart(mekArmor);
    				campaign.addPart(mekArmor);
    			}
    			if(null == armorRear[i] && entity.hasRearArmor(i)) {
    				Armor mekArmor = new Armor(false, (int) getEntity().getWeight(), getEntity().getArmorType(i), getEntity().getOArmor(i, false), i, true);
    				addPart(mekArmor);
    				campaign.addPart(mekArmor);
    			}
    		}
    		for(Mounted m : entity.getEquipment()) {
    			if(m.getType().isHittable()) {
	    			int eqnum = entity.getEquipmentNum(m);
	    			EquipmentPart epart = equipParts.get(eqnum);
	    			if(null == epart) {
	    				epart = new EquipmentPart(false, (int)entity.getWeight(), m.getType(), eqnum);
	    				addPart(epart);
	    				campaign.addPart(epart);
	    			}
    			}
    		}
    		if(null == rightUpperArm && entity.hasSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_RARM)) {
    			rightUpperArm = new MekActuator(false, (int)entity.getWeight(), Mech.ACTUATOR_UPPER_ARM, Mech.LOC_RARM);
    			addPart(rightUpperArm);
    			campaign.addPart(rightUpperArm);
    		}
    		if(null == leftUpperArm && entity.hasSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM)) {
    			leftUpperArm = new MekActuator(false, (int)entity.getWeight(), Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM);
    			addPart(leftUpperArm);
    			campaign.addPart(leftUpperArm);
    		}
    		if(null == rightLowerArm && entity.hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM)) {
    			rightLowerArm = new MekActuator(false, (int)entity.getWeight(), Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM);
    			addPart(rightLowerArm);
    			campaign.addPart(rightLowerArm);
    		}
    		if(null == leftLowerArm && entity.hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)) {
    			leftLowerArm = new MekActuator(false, (int)entity.getWeight(), Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM);
    			addPart(leftLowerArm);
    			campaign.addPart(leftLowerArm);
    		}
    		if(null == rightHand && entity.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM)) {
    			rightHand = new MekActuator(false, (int)entity.getWeight(), Mech.ACTUATOR_HAND, Mech.LOC_RARM);
    			addPart(rightHand);
    			campaign.addPart(rightHand);
    		}
    		if(null == leftHand && entity.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)) {
    			leftHand = new MekActuator(false, (int)entity.getWeight(), Mech.ACTUATOR_HAND, Mech.LOC_LARM);
    			addPart(leftHand);
    			campaign.addPart(leftHand);
    		}
    		if(null == rightUpperLeg && entity.hasSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_RLEG)) {
    			rightUpperLeg = new MekActuator(false, (int)entity.getWeight(), Mech.ACTUATOR_UPPER_LEG, Mech.LOC_RLEG);
    			addPart(rightUpperLeg);
    			campaign.addPart(rightUpperLeg);
    		}
    		if(null == leftUpperLeg && entity.hasSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LLEG)) {
    			leftUpperLeg = new MekActuator(false, (int)entity.getWeight(), Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LLEG);
    			addPart(leftUpperLeg);
    			campaign.addPart(leftUpperLeg);
    		}
    		if(null == rightLowerLeg && entity.hasSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_RLEG)) {
    			rightLowerLeg = new MekActuator(false, (int)entity.getWeight(), Mech.ACTUATOR_LOWER_LEG, Mech.LOC_RLEG);
    			addPart(rightLowerLeg);
    			campaign.addPart(rightLowerLeg);
    		}
    		if(null == leftLowerLeg && entity.hasSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LLEG)) {
    			leftLowerLeg = new MekActuator(false, (int)entity.getWeight(), Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LLEG);
    			addPart(leftLowerLeg);
    			campaign.addPart(leftLowerLeg);
    		}
    		if(null == rightFoot && entity.hasSystem(Mech.ACTUATOR_FOOT, Mech.LOC_RLEG)) {
    			rightFoot = new MekActuator(false, (int)entity.getWeight(), Mech.ACTUATOR_FOOT, Mech.LOC_RLEG);
    			addPart(rightFoot);
    			campaign.addPart(rightFoot);
    		}
    		if(null == leftFoot && entity.hasSystem(Mech.ACTUATOR_FOOT, Mech.LOC_LLEG)) {
    			leftFoot = new MekActuator(false, (int)entity.getWeight(), Mech.ACTUATOR_FOOT, Mech.LOC_LLEG);
    			addPart(leftFoot);
    			campaign.addPart(leftFoot);
    		}
    	}
    	runDiagnostic();
    }
}
