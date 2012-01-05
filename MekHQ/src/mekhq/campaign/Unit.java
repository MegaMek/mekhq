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

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.ConvFighter;
import megamek.common.CriticalSlot;
import megamek.common.Dropship;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Pilot;
import megamek.common.QuadMech;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.TechConstants;
import megamek.common.VTOL;
import megamek.common.Warship;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.PilotOptions;
import mekhq.MekHQ;
import mekhq.campaign.parts.AeroHeatSink;
import mekhq.campaign.parts.AeroSensor;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Avionics;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.FireControlSystem;
import mekhq.campaign.parts.LandingGear;
import mekhq.campaign.parts.MekActuator;
import mekhq.campaign.parts.MekCockpit;
import mekhq.campaign.parts.MekGyro;
import mekhq.campaign.parts.MekLifeSupport;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MekSensor;
import mekhq.campaign.parts.MissingAeroHeatSink;
import mekhq.campaign.parts.MissingAeroSensor;
import mekhq.campaign.parts.MissingAvionics;
import mekhq.campaign.parts.MissingEnginePart;
import mekhq.campaign.parts.MissingFireControlSystem;
import mekhq.campaign.parts.MissingLandingGear;
import mekhq.campaign.parts.MissingMekActuator;
import mekhq.campaign.parts.MissingMekCockpit;
import mekhq.campaign.parts.MissingMekGyro;
import mekhq.campaign.parts.MissingMekLifeSupport;
import mekhq.campaign.parts.MissingMekLocation;
import mekhq.campaign.parts.MissingMekSensor;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.MissingRotor;
import mekhq.campaign.parts.MissingTurret;
import mekhq.campaign.parts.MissingVeeSensor;
import mekhq.campaign.parts.MissingVeeStabiliser;
import mekhq.campaign.parts.MotiveSystem;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.parts.Rotor;
import mekhq.campaign.parts.StructuralIntegrity;
import mekhq.campaign.parts.TankLocation;
import mekhq.campaign.parts.Turret;
import mekhq.campaign.parts.TurretLock;
import mekhq.campaign.parts.VeeSensor;
import mekhq.campaign.parts.VeeStabiliser;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.JumpJet;
import mekhq.campaign.parts.equipment.MASC;
import mekhq.campaign.parts.equipment.MissingAmmoBin;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.parts.equipment.MissingHeatSink;
import mekhq.campaign.parts.equipment.MissingJumpJet;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.work.IAcquisitionWork;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
	private boolean salvaged;
	private int id = -1;
	private int quality;
	
	//assignments
	private int forceId;
    protected int scenarioId;

	private ArrayList<Integer> drivers;
	private ArrayList<Integer> gunners;
	
	public Campaign campaign;

	private ArrayList<Part> parts;

	private Refit refit;
	
	//for backwards compatability with 0.1.8, but otherwise is no longer used 
	private int pilotId = -1;
	
	public Unit() {
		this(null, null);
	}
	
	public Unit(Entity en, Campaign c) {
		this.entity = en;
		this.site = SITE_BAY;
		this.salvaged = false;
		this.campaign = c;
		this.quality = QUALITY_D;
		this.parts = new ArrayList<Part>();
		this.drivers = new ArrayList<Integer>();
		this.gunners = new ArrayList<Integer>();     
		scenarioId = -1;
		this.refit = null;
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
		if(isRefitting()) {
			return "Refitting";
		}
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
	
	public void setId(int i) {
		this.id = i;
	}

	public int getSite() {
		return site;
	}

	public void setSite(int i) {
		this.site = i;
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

	public boolean isFunctional() {
		if (entity instanceof Mech) {
			// center torso bad?? head bad?
			if (entity.isLocationBad(Mech.LOC_CT)
					|| entity.isLocationBad(Mech.LOC_HEAD)) {
				return false;
			}
			// engine destruction?
			//cockpit hits
			int engineHits = 0;
			int cockpitHits = 0;
			for (int i = 0; i < entity.locations(); i++) {
				engineHits += entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
						Mech.SYSTEM_ENGINE, i);
				cockpitHits += entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
						Mech.SYSTEM_COCKPIT, i);
			}
			if (engineHits > 2) {
				return false;
			}
			if(cockpitHits > 0) {
				return false;
			}
		}
		if (entity instanceof Tank) {
			for (int i = 0; i < entity.locations(); i++) {
				if(i == Tank.LOC_TURRET || i == Tank.LOC_TURRET_2) {
					continue;
				}
				if (entity.isLocationBad(i)) {
					return false;
				}
			}
			if(entity instanceof VTOL) {
				if(entity.getWalkMP() <= 0) {
					return false;
				}
			}
		}
		if(entity instanceof Aero) {
			if(entity.getWalkMP() <= 0 && !(entity instanceof Jumpship)) {
				return false;
			}
			if(((Aero)entity).getSI() <= 0) {
				return false;
			}
		}
		return true;
	}

	public boolean isRepairable() {
		if (entity instanceof Mech) {
			// you can repair anything so long as one point of CT is left
			if (entity.getInternal(Mech.LOC_CT) <= 0) {
				return false;
			}
		}
		if (entity instanceof Tank) {
			// can't repair a tank with a destroyed location
			for (int i = 0; i < entity.locations(); i++) {
				if(i == Tank.LOC_TURRET || i == Tank.LOC_TURRET_2 || i == Tank.LOC_BODY) {
					continue;
				}
				if (entity.getInternal(i) <= 0) {
					return false;
				}
			}
		}
		if(entity instanceof Aero) {
			if(((Aero)entity).getSI() <= 0) {
				return false;
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
	}
	
	public ArrayList<Part> getPartsNeedingFixing() {
		ArrayList<Part> brokenParts = new ArrayList<Part>();
		for(Part part: parts) {
			if(part.needsFixing()) {
				brokenParts.add(part);
			}
		}
		return brokenParts;
	}
	
	public ArrayList<Part> getSalvageableParts() {
		ArrayList<Part> brokenParts = new ArrayList<Part>();
		for(Part part: parts) {
			if(part.isSalvaging()) {
				brokenParts.add(part);
			}
		}
		return brokenParts;
	}
	
	public ArrayList<IAcquisitionWork> getPartsNeeded() {
		ArrayList<IAcquisitionWork> missingParts = new ArrayList<IAcquisitionWork>();
		if(isSalvage() || !isRepairable()) {
			return missingParts;
		}
		boolean armorFound = false;
		for(Part part: parts) {
			if(part instanceof MissingPart && null == ((MissingPart)part).findReplacement(false)) {
				missingParts.add((MissingPart)part);
			}
			//we need to check for armor as well, but this one is funny because we dont want to
			//check per location really, since armor can be used anywhere. So stop after we reach
			//the first Armor needing replacement
			//TODO: we need to adjust for patchwork armor, which can have different armor types by location		
			if(!armorFound && part instanceof Armor) {
				Armor a = (Armor)part;
				if(a.needsFixing() && !a.isEnoughSpareArmorAvailable()) {
					missingParts.add(a);
					armorFound = true;
				}
			}
			if(part instanceof AmmoBin && !((AmmoBin)part).isEnoughSpareAmmoAvailable()) {
				missingParts.add((AmmoBin)part);
			}
		}
		
		return missingParts;
	}

	public long getValueOfAllMissingParts() {
		long value = 0;
		for(Part part : parts) {
			if(part instanceof MissingPart) {
				value += ((MissingPart)part).getNewPart().getActualValue();
			}
			else if(part instanceof Armor) {
				value += ((Armor)part).getValueNeeded();
			}
			else if(part instanceof AmmoBin) {
				value += ((AmmoBin)part).getValueNeeded();
			}
		}
		return value;
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
		return null != entity.getCrew();
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
		String toReturn = "<b>" + getName() + "</b><br/>";
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
		return scenarioId != -1;
	}
	
	public void undeploy() {
		scenarioId = -1;
	}

	public String checkDeployment() {
		if (!isFunctional()) {
			return "unit is not functional";
		}
		if (isUnmanned()) {
			return "unit has no pilot";
		}
		if(isRefitting()) {
			return "unit is being refit";
		}
		if(entity instanceof Tank 
				&& getActiveCrew().size() < getFullCrewSize()) {
			return "This vehicle requires a crew of " + getFullCrewSize();
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

	public int getSellValue() {
		int residualValue = 0;

		
		/*
		 * TODO: We should do this the full accounting way below, but for the 
		 * short term I am going to just use the StratOps rule on pg. 181
		 * undamaged units sell for 1/2, damaged units sell for 1/3,
		 * destroyed units sell for 1/10
		int valueOfSalvage = 0;
		
		for (Part part : parts) {
			if (!(part instanceof MissingPart)) {
				valueOfSalvage += part.getCost();
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
		 */
		
		int cost = (int) Math.round(getEntity().getCost(false));
		if(entity.isClan()) {
			cost *= campaign.getCampaignOptions().getClanPriceModifier();
		}
		if(!isDamaged()) {
			return cost / 2;
		} else if(isFunctional()) {
			return cost / 3;
		} else {
			return cost / 10;
		}
	}

	public int getBuyCost() {
		int cost = (int) Math.round(getEntity().getCost(false));
		if(entity.isClan()) {
			cost *= campaign.getCampaignOptions().getClanPriceModifier();
		}
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
			return Unit.STATE_UNDAMAGED;
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
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<quality>"
				+ quality + "</quality>");
		for(int did : drivers) {
			pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<driverId>"
					+ did + "</driverId>");
		}
		for(int gid : gunners) {
			pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<gunnerId>"
					+ gid + "</gunnerId>");
		}
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<salvaged>"
				+ salvaged + "</salvaged>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl + 1) + "<site>" + site
				+ "</site>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
				+"<forceId>"
				+forceId
				+"</forceId>");
		pw1.println(MekHqXmlUtil.indentStr(indentLvl+1)
				+"<scenarioId>"
				+scenarioId
				+"</scenarioId>");
		if(null != refit) {
			refit.writeToXml(pw1, indentLvl+1, id);
		}
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
				
				if (wn2.getNodeName().equalsIgnoreCase("quality")) {
					retVal.quality = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("site")) {
					retVal.site = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("pilotId")) {
					retVal.pilotId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("driverId")) {
					retVal.drivers.add(Integer.parseInt(wn2.getTextContent()));
				} else if (wn2.getNodeName().equalsIgnoreCase("gunnerId")) {
					retVal.gunners.add(Integer.parseInt(wn2.getTextContent()));
				} else if (wn2.getNodeName().equalsIgnoreCase("forceId")) {
					retVal.forceId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("scenarioId")) {
					retVal.scenarioId = Integer.parseInt(wn2.getTextContent());
				} else if (wn2.getNodeName().equalsIgnoreCase("salvaged")) {
					if (wn2.getTextContent().equalsIgnoreCase("true"))
						retVal.salvaged = true;
					else
						retVal.salvaged = false;
				} else if (wn2.getNodeName().equalsIgnoreCase("entity")) {
					retVal.entity = MekHqXmlUtil.getEntityFromXmlString(wn2);

					if ((retVal.id >= 0) && (retVal.entity != null)) {
						MekHQ.logMessage("ID pre-defined and entity not null; setting entity's ID.", 5);
						retVal.entity.setId(retVal.id);
					} else if (retVal.entity != null) {
						MekHQ.logMessage("ID not pre-defined and entity not null; setting unit's ID.", 5);
						retVal.id = retVal.entity.getId();
					}
				} else if (wn2.getNodeName().equalsIgnoreCase("refit")) {
					retVal.refit = Refit.generateInstanceFromXML(wn2, retVal);
				}
			}
		} catch (Exception ex) {
			// Doh!
			MekHQ.logError(ex);
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
        boolean first = true;
    	for (Enumeration<IOptionGroup> i = getEntity().getQuirks().getGroups(); i.hasMoreElements();) {
            IOptionGroup group = i.nextElement();
            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
                IOption quirk = j.nextElement();
                if (quirk.booleanValue()) {
                	if(first) {
                		first = false;
                	} else {
                		quirkString += "\n";
                	}
                	quirkString += quirk.getDisplayableNameWithValue();
                }
            }
        }
        if(quirkString.equals("")) {
        	return null;
        }
        return quirkString;
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
    public void initializeParts(boolean addParts) {
    	if(entity instanceof Infantry && !(entity instanceof BattleArmor)) {
    		return;
    	}
  	
    	int erating = 0;
    	if(null != entity.getEngine()) {
    		erating = entity.getEngine().getRating();
    	}
    	
    	ArrayList<Part> partsToAdd = new ArrayList<Part>();
    	
    	Part gyro = null;
    	Part engine = null;
    	Part lifeSupport = null;
    	Part sensor = null;
    	Part cockpit = null;
    	Part rightHand = null;
    	Part rightLowerArm = null;
    	Part rightUpperArm = null;
    	Part leftHand = null;
    	Part leftLowerArm = null;
    	Part leftUpperArm = null;
    	Part rightFoot = null;
    	Part rightLowerLeg = null;
    	Part rightUpperLeg = null;
    	Part leftFoot = null;
    	Part leftLowerLeg = null;
    	Part leftUpperLeg = null;
    	Part rightFrontFoot = null;
    	Part rightLowerFrontLeg = null;
    	Part rightUpperFrontLeg = null;
    	Part leftFrontFoot = null;
    	Part leftLowerFrontLeg = null;
    	Part leftUpperFrontLeg = null;
    	Part structuralIntegrity = null;
    	Part[] locations = new Part[entity.locations()];
    	Armor[] armor = new Armor[entity.locations()];
    	Armor[] armorRear = new Armor[entity.locations()];
    	Part[] stabilisers = new Part[entity.locations()];
    	Hashtable<Integer,Part> equipParts = new Hashtable<Integer,Part>();
    	Hashtable<Integer,Part> ammoParts = new Hashtable<Integer,Part>();
    	Hashtable<Integer,Part> heatSinks = new Hashtable<Integer,Part>();
    	Hashtable<Integer,Part> jumpJets = new Hashtable<Integer,Part>();
    	Part motiveSystem = null;
    	Part avionics = null;
    	Part fcs = null;
    	Part landingGear = null;
    	Part turretLock = null;
    	ArrayList<Part> aeroHeatSinks = new ArrayList<Part>();

    	for(Part part : parts) {
    		if(part instanceof MekGyro || part instanceof MissingMekGyro) {
    			gyro = part;
    		} else if(part instanceof EnginePart || part instanceof MissingEnginePart) {
    			engine = part;
    		} else if(part instanceof MekLifeSupport  || part instanceof MissingMekLifeSupport) {
    			lifeSupport = part;
    		} else if(part instanceof MekSensor || part instanceof MissingMekSensor) {
    			sensor = part;
    		} else if(part instanceof MekCockpit || part instanceof MissingMekCockpit) {
    			cockpit = part;
    		}  else if(part instanceof VeeSensor || part instanceof MissingVeeSensor) {
    			sensor = part;
    		}  else if(part instanceof StructuralIntegrity) {
    			structuralIntegrity = part;
    		} else if(part instanceof MekLocation) {
    			locations[((MekLocation)part).getLoc()] = part;
    		} else if(part instanceof MissingMekLocation) {
    			locations[((MissingMekLocation)part).getLoc()] = part;	
    		} else if(part instanceof TankLocation) {
    			locations[((TankLocation)part).getLoc()] = part;
    		} else if(part instanceof Rotor) {
    			locations[((Rotor)part).getLoc()] = part;	
    		} else if(part instanceof MissingRotor) {
    			locations[VTOL.LOC_ROTOR] = part;	
    		} else if(part instanceof Turret) {
    			locations[((Turret)part).getLoc()] = part;	
    		} else if(part instanceof MissingTurret) {
    			locations[Tank.LOC_TURRET] = part;	
    		} else if(part instanceof Armor) {
    			if(((Armor)part).isRearMounted()) {
    				armorRear[((Armor)part).getLocation()] = (Armor)part;
    			} else {
    				armor[((Armor)part).getLocation()] = (Armor)part;
    			}
    		} else if(part instanceof VeeStabiliser) {
    			stabilisers[((VeeStabiliser)part).getLocation()] = part;
    		} else if(part instanceof MissingVeeStabiliser) {
    			stabilisers[((MissingVeeStabiliser)part).getLocation()] = part;
    		} else if(part instanceof AmmoBin) {
    			ammoParts.put(((AmmoBin)part).getEquipmentNum(), part);
    		} else if(part instanceof MissingAmmoBin) {
    			ammoParts.put(((MissingAmmoBin)part).getEquipmentNum(), part);
    		} else if(part instanceof HeatSink) {
    			heatSinks.put(((HeatSink)part).getEquipmentNum(), part);
    		} else if(part instanceof MissingHeatSink) {
    			heatSinks.put(((MissingHeatSink)part).getEquipmentNum(), part);
    		} else if(part instanceof JumpJet) {
    			jumpJets.put(((JumpJet)part).getEquipmentNum(), part);
    		} else if(part instanceof MissingJumpJet) {
    			jumpJets.put(((MissingJumpJet)part).getEquipmentNum(), part);
    		}  else if(part instanceof EquipmentPart) {
    			equipParts.put(((EquipmentPart)part).getEquipmentNum(), part);
    		} else if(part instanceof MissingEquipmentPart) {
    			equipParts.put(((MissingEquipmentPart)part).getEquipmentNum(), part);
    		} else if(part instanceof MekActuator || part instanceof MissingMekActuator) {
    			int type = -1;
    			int loc = -1;
    			if(part instanceof MekActuator) {
    				type = ((MekActuator)part).getType();
    				loc = ((MekActuator)part).getLocation();
    			} else {
    				type = ((MissingMekActuator)part).getType();
    				loc = ((MissingMekActuator)part).getLocation();
    			}
    			if(type == Mech.ACTUATOR_UPPER_ARM) {
    				if(loc == Mech.LOC_RARM) {
    					rightUpperArm = part;
    				} else {
    					leftUpperArm = part;
    				}
    			} else if(type == Mech.ACTUATOR_LOWER_ARM) {
    				if(loc == Mech.LOC_RARM) {
    					rightLowerArm = part;
    				} else {
    					leftLowerArm = part;
    				}
    			} else if(type == Mech.ACTUATOR_HAND) {
    				if(loc == Mech.LOC_RARM) {
    					rightHand = part;
    				} else {
    					leftHand = part;
    				}
    			} else if(type == Mech.ACTUATOR_UPPER_LEG) {
    				if(loc == Mech.LOC_LARM) {
    					leftUpperFrontLeg = part;
    				} else if(loc == Mech.LOC_RARM) {
    					rightUpperFrontLeg = part;
    				} else if(loc == Mech.LOC_RLEG) {
    					rightUpperLeg = part;
    				} else {
    					leftUpperLeg = part;
    				}
    			} else if(type == Mech.ACTUATOR_LOWER_LEG) {
    				if(loc == Mech.LOC_LARM) {
    					leftLowerFrontLeg = part;
    				} else if(loc == Mech.LOC_RARM) {
    					rightLowerFrontLeg = part;
    				} else if(loc == Mech.LOC_RLEG) {
    					rightLowerLeg = part;
    				} else {
    					leftLowerLeg = part;
    				}
    			} else if(type == Mech.ACTUATOR_FOOT) {
    				if(loc == Mech.LOC_LARM) {
    					leftFrontFoot = part;
    				} else if(loc == Mech.LOC_RARM) {
    					rightFrontFoot = part;
    				} else if(loc == Mech.LOC_RLEG) {
    					rightFoot = part;
    				} else {
    					leftFoot = part;
    				}
    			}
    		} else if(part instanceof Avionics || part instanceof MissingAvionics) {
    			avionics = part;
    		} else if(part instanceof FireControlSystem || part instanceof MissingFireControlSystem) {
    			fcs = part;
    		} else if(part instanceof AeroSensor || part instanceof MissingAeroSensor) {
    			sensor = part;
    		} else if(part instanceof LandingGear || part instanceof MissingLandingGear) {
    			landingGear = part;
    		} else if(part instanceof AeroHeatSink || part instanceof MissingAeroHeatSink) {
    			aeroHeatSinks.add(part);
    		} else if(part instanceof MotiveSystem) {
    			motiveSystem = part;
    		} else if(part instanceof TurretLock) {
    			turretLock = part;
    		}
    	}
    	//now check to see what is null
    	for(int i = 0; i<locations.length; i++) {
    		if(null == locations[i]) {
    			if(entity instanceof Mech) {
    				MekLocation mekLocation = new MekLocation(i, (int) getEntity().getWeight(), getEntity().getStructureType(), hasTSM(), entity instanceof QuadMech, campaign);
    				addPart(mekLocation);
    				partsToAdd.add(mekLocation);
    			} else if(entity instanceof Tank && i != Tank.LOC_BODY) {
    				if(i == Tank.LOC_TURRET && entity instanceof VTOL) {
    					Rotor rotor = new Rotor((int)getEntity().getWeight(), campaign);
    					addPart(rotor);
    					partsToAdd.add(rotor);
    				} else if(i == Tank.LOC_TURRET) {
    					 if(((Tank)entity).hasNoTurret()) {
    						 continue;
    					 }
    					 Turret turret = new Turret(i, (int)getEntity().getWeight(), campaign);
    					 addPart(turret);
    					 partsToAdd.add(turret);
    				} else if(i == Tank.LOC_TURRET_2) {
    					 if(((Tank)entity).hasNoDualTurret()) {
    						 continue;
    					 }
    					 Turret turret = new Turret(i, (int)getEntity().getWeight(), campaign);
    					 addPart(turret);
    					 partsToAdd.add(turret);
    				} else {
	    				TankLocation tankLocation = new TankLocation(i, (int) getEntity().getWeight(), campaign);
	    				addPart(tankLocation);
	    				partsToAdd.add(tankLocation);
    				}
    			}
    		}
    		if(null == armor[i]) {
    			Armor a = new Armor((int) getEntity().getWeight(), getEntity().getArmorType(i), getEntity().getOArmor(i, false), i, false, entity.isClanArmor(i), campaign);
    			addPart(a);
    			partsToAdd.add(a);
    		}
    		if(null == armorRear[i] && entity.hasRearArmor(i)) {
    			Armor a = new Armor((int) getEntity().getWeight(), getEntity().getArmorType(i), getEntity().getOArmor(i, true), i, true, entity.isClanArmor(i), campaign);
    			addPart(a);
    			partsToAdd.add(a);
    		}
    		if(entity instanceof Tank && null == stabilisers[i] && i != Tank.LOC_BODY) {
    			VeeStabiliser s = new VeeStabiliser((int)getEntity().getWeight(),i, campaign);
    			addPart(s);
    			partsToAdd.add(s);
    		}
    	}
    	for(Mounted m : entity.getEquipment()) {
    		if(m.getType().isHittable()) {
    			if(m.getType() instanceof AmmoType) {
    				int eqnum = entity.getEquipmentNum(m);
    				Part apart = ammoParts.get(eqnum);
    				int fullShots = ((AmmoType)m.getType()).getShots();
    				boolean oneShot = false;
    				if(m.getLocation() == Entity.LOC_NONE) {
    					fullShots = 1;
    					oneShot = true;
    				}
    				if(null == apart) {
    					apart = new AmmoBin((int)entity.getWeight(), m.getType(), eqnum, fullShots - m.getShotsLeft(), oneShot, campaign);
    					addPart(apart);
    					partsToAdd.add(apart);
    				}
    			} else if(m.getType() instanceof MiscType && (m.getType().hasFlag(MiscType.F_HEAT_SINK) || m.getType().hasFlag(MiscType.F_DOUBLE_HEAT_SINK))) {
    				if(m.getLocation() == Entity.LOC_NONE) {
    					//heat sinks located in LOC_NONE are base unhittable heat sinks
    					continue;
    				}
    				int eqnum = entity.getEquipmentNum(m);
    				Part epart = heatSinks.get(eqnum);
    				if(null == epart) {
    					epart = new HeatSink((int)entity.getWeight(), m.getType(), eqnum, campaign);
    					addPart(epart);
    					partsToAdd.add(epart);
    				}
    			} else if(m.getType() instanceof MiscType && m.getType().hasFlag(MiscType.F_JUMP_JET)) {
    				int eqnum = entity.getEquipmentNum(m);
    				Part epart = jumpJets.get(eqnum);
    				if(null == epart) {
    					epart = new JumpJet((int)entity.getWeight(), m.getType(), eqnum, campaign);
    					addPart(epart);
    					partsToAdd.add(epart);
    				}
    			} else {
    				int eqnum = entity.getEquipmentNum(m);
    				Part epart = equipParts.get(eqnum);
    				if(null == epart) {
    					EquipmentType type = m.getType();
    					epart = new EquipmentPart((int)entity.getWeight(), type, eqnum, campaign);
    					if(type.hasFlag(MiscType.F_MASC)) {
        					epart = new MASC((int)entity.getWeight(), type, eqnum, campaign, erating);
    					}
    					addPart(epart);
    					partsToAdd.add(epart);
    				}
    			}
    		}
    	}
    	
    	if(null == engine && !(entity instanceof BattleArmor)) {
    		engine = new EnginePart((int) entity.getWeight(), new Engine(entity.getEngine().getRating(), entity.getEngine().getEngineType(), entity.getEngine().getFlags()), campaign, entity.getMovementMode() == EntityMovementMode.HOVER && entity instanceof Tank);
    		addPart(engine);
    		partsToAdd.add(engine);
    	}
    	if(entity instanceof Mech) {
    		if(null == gyro) {
    			gyro =  new MekGyro((int) entity.getWeight(), entity.getGyroType(), entity.getOriginalWalkMP(), campaign);
    			addPart(gyro);
    			partsToAdd.add(gyro);
    		}
    		if(null == lifeSupport) {
    			lifeSupport = new MekLifeSupport((int) entity.getWeight(), campaign);
    			addPart(lifeSupport);
    			partsToAdd.add(lifeSupport);
    		}
    		if(null == sensor) {
    			sensor = new MekSensor((int) entity.getWeight(), campaign);
    			addPart(sensor);
    			partsToAdd.add(sensor);
    		}
    		if(null == cockpit) {
    			cockpit = new MekCockpit((int) entity.getWeight(), ((Mech)entity).getCockpitType(), campaign);
    			addPart(cockpit);
    			partsToAdd.add(cockpit);
    		}
    		if(null == rightUpperArm && entity.hasSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_RARM)) {
    			rightUpperArm = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_UPPER_ARM, Mech.LOC_RARM, campaign);
    			addPart(rightUpperArm);
    			partsToAdd.add(rightUpperArm);
    		}
    		if(null == leftUpperArm && entity.hasSystem(Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM)) {
    			leftUpperArm = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_UPPER_ARM, Mech.LOC_LARM, campaign);
    			addPart(leftUpperArm);
    			partsToAdd.add(leftUpperArm);
    		}
    		if(null == rightLowerArm && entity.hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM)) {
    			rightLowerArm = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_LOWER_ARM, Mech.LOC_RARM, campaign);
    			addPart(rightLowerArm);
    			partsToAdd.add(rightLowerArm);
    		}
    		if(null == leftLowerArm && entity.hasSystem(Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM)) {
    			leftLowerArm = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_LOWER_ARM, Mech.LOC_LARM, campaign);
    			addPart(leftLowerArm);
    			partsToAdd.add(leftLowerArm);
    		}
    		if(null == rightHand && entity.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_RARM)) {
    			rightHand = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_HAND, Mech.LOC_RARM, campaign);
    			addPart(rightHand);
    			partsToAdd.add(rightHand);
    		}
    		if(null == leftHand && entity.hasSystem(Mech.ACTUATOR_HAND, Mech.LOC_LARM)) {
    			leftHand = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_HAND, Mech.LOC_LARM, campaign);
    			addPart(leftHand);
    			partsToAdd.add(leftHand);
    		}
    		if(null == rightUpperLeg && entity.hasSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_RLEG)) {
    			rightUpperLeg = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_UPPER_LEG, Mech.LOC_RLEG, campaign);
    			addPart(rightUpperLeg);
    			partsToAdd.add(rightUpperLeg);
    		}
    		if(null == leftUpperLeg && entity.hasSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LLEG)) {
    			leftUpperLeg = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LLEG, campaign);
    			addPart(leftUpperLeg);
    			partsToAdd.add(leftUpperLeg);
    		}
    		if(null == rightLowerLeg && entity.hasSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_RLEG)) {
    			rightLowerLeg = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_LOWER_LEG, Mech.LOC_RLEG, campaign);
    			addPart(rightLowerLeg);
    			partsToAdd.add(rightLowerLeg);
    		}
    		if(null == leftLowerLeg && entity.hasSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LLEG)) {
    			leftLowerLeg = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LLEG, campaign);
    			addPart(leftLowerLeg);
    			partsToAdd.add(leftLowerLeg);
    		}
    		if(null == rightFoot && entity.hasSystem(Mech.ACTUATOR_FOOT, Mech.LOC_RLEG)) {
    			rightFoot = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_FOOT, Mech.LOC_RLEG, campaign);
    			addPart(rightFoot);
    			partsToAdd.add(rightFoot);
    		}
    		if(null == leftFoot && entity.hasSystem(Mech.ACTUATOR_FOOT, Mech.LOC_LLEG)) {
    			leftFoot = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_FOOT, Mech.LOC_LLEG, campaign);
    			addPart(leftFoot);
    			partsToAdd.add(leftFoot);
    		}
    		if(null == rightUpperFrontLeg && entity.hasSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_RARM)) {
    			rightUpperFrontLeg = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_UPPER_LEG, Mech.LOC_RARM, campaign);
    			addPart(rightUpperFrontLeg);
    			partsToAdd.add(rightUpperFrontLeg);
    		}
    		if(null == leftUpperFrontLeg && entity.hasSystem(Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LARM)) {
    			leftUpperFrontLeg = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_UPPER_LEG, Mech.LOC_LARM, campaign);
    			addPart(leftUpperFrontLeg);
    			partsToAdd.add(leftUpperFrontLeg);
    		}
    		if(null == rightLowerFrontLeg && entity.hasSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_RARM)) {
    			rightLowerFrontLeg = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_LOWER_LEG, Mech.LOC_RARM, campaign);
    			addPart(rightLowerFrontLeg);
    			partsToAdd.add(rightLowerFrontLeg);
    		}
    		if(null == leftLowerFrontLeg && entity.hasSystem(Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LARM)) {
    			leftLowerFrontLeg = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_LOWER_LEG, Mech.LOC_LARM, campaign);
    			addPart(leftLowerFrontLeg);
    			partsToAdd.add(leftLowerFrontLeg);
    		}
    		if(null == rightFrontFoot && entity.hasSystem(Mech.ACTUATOR_FOOT, Mech.LOC_RARM)) {
    			rightFrontFoot = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_FOOT, Mech.LOC_RARM, campaign);
    			addPart(rightFrontFoot);
    			partsToAdd.add(rightFrontFoot);
    		}
    		if(null == leftFrontFoot && entity.hasSystem(Mech.ACTUATOR_FOOT, Mech.LOC_LARM)) {
    			leftFrontFoot = new MekActuator((int)entity.getWeight(), Mech.ACTUATOR_FOOT, Mech.LOC_LARM, campaign);
    			addPart(leftFrontFoot);
    			partsToAdd.add(leftFrontFoot);
    		}
    	}
    	if(entity instanceof Aero) {
    		if(null == structuralIntegrity) {
    			structuralIntegrity = new StructuralIntegrity((int)entity.getWeight(), campaign);
    			addPart(structuralIntegrity);
    			partsToAdd.add(structuralIntegrity);
    		}
    		if(null == avionics) {
    			avionics = new Avionics((int)entity.getWeight(), campaign);
    			addPart(avionics);
    			partsToAdd.add(avionics);
    		}
    		if(null == fcs && !(entity instanceof Jumpship)) {
    			fcs = new FireControlSystem((int)entity.getWeight(), campaign);
    			addPart(fcs);
    			partsToAdd.add(fcs);
    		}
    		if(null == sensor) {
    			sensor = new AeroSensor((int) entity.getWeight(), entity instanceof Dropship, campaign);
    			addPart(sensor);
    			partsToAdd.add(sensor);
    		}
    		if(null == landingGear) {
    			landingGear = new LandingGear((int) entity.getWeight(), campaign);
    			addPart(landingGear);
    			partsToAdd.add(landingGear);
    		}
    		int hsinks = ((Aero)entity).getOHeatSinks() - aeroHeatSinks.size();
    		while(hsinks > 0) {
    			AeroHeatSink aHeatSink = new AeroHeatSink((int)entity.getWeight(), ((Aero)entity).getHeatType(), campaign);
    			addPart(aHeatSink);
    			partsToAdd.add(aHeatSink);
    			hsinks--;
    		}
     	}
    	if(entity instanceof Tank) {
    		if(null == motiveSystem) {
    			motiveSystem = new MotiveSystem((int)entity.getWeight(), campaign);
    			addPart(motiveSystem);
    			partsToAdd.add(motiveSystem);
    		}
    		if(null == sensor) {
    			sensor = new VeeSensor((int) entity.getWeight(), campaign);
    			addPart(sensor);
    			partsToAdd.add(sensor);
    		}
    		if(!(entity instanceof VTOL) && !((Tank)entity).hasNoTurret() && null == turretLock) {
    			turretLock = new TurretLock(campaign);
    			addPart(turretLock);
    			partsToAdd.add(turretLock);
    		}
    	}
    	
    	if(addParts) {
    		for(Part p : partsToAdd) {
    			campaign.addPart(p);
    		}
    	}
    }
    
    public ArrayList<Part> getParts() {
    	return parts;
    }
    
    public void setParts(ArrayList<Part> newParts) {
    	parts = newParts;
    }
    
    public ArrayList<AmmoBin> getWorkingAmmoBins() {
    	ArrayList<AmmoBin> ammo = new ArrayList<AmmoBin>();
    	for(Part part : parts) {
    		if(part instanceof AmmoBin) {
    			ammo.add((AmmoBin)part);
    		}
    	}
    	return ammo;
    }
    
    public Person getCommander() {
    	//take first by rank
    	//if rank is tied, take gunners over drivers
    	//if two of the same type are tie rank, take the first one
    	int bestRank = -1;
    	Person commander = null;
    	for(int pid : gunners) {
    		Person p = campaign.getPerson(pid);
    		if((entity instanceof Tank || entity instanceof Infantry) && p.getHits() > 0) { 
    			continue;
    		}
    		if(p.getRank() > bestRank) {
    			commander = p;
    			bestRank = p.getRank();
    		}
    	}
    	for(int pid : drivers) {
    		Person p = campaign.getPerson(pid);
    		if((entity instanceof Tank || entity instanceof Infantry) && p.getHits() > 0) { 
    			continue;
    		}
    		if(p.getRank() > bestRank) {
    			commander = p;
    			bestRank = p.getRank();
    		}
    	}
    	return commander;
    }
 
    public void resetPilotAndEntity() {
    		
    	int piloting = 13;
    	int gunnery = 13;	
    	int artillery = 13;
    	String driveType = SkillType.getDrivingSkillFor(entity);
    	String gunType = SkillType.getGunnerySkillFor(entity);
    	int sumPiloting = 0;
    	int nDrivers = 0;
    	int sumGunnery = 0;
    	int nGunners = 0;
    	for(int pid : drivers) {
    		Person p = campaign.getPerson(pid);
    		if(p.getHits() > 0 && !(entity instanceof Mech || entity instanceof Aero)) {
    			continue;
    		}
    		if(p.hasSkill(driveType)) {
    			sumPiloting += p.getSkill(driveType).getFinalSkillValue();
    			nDrivers++;
    		}
    	}
    	for(int pid : gunners) {
    		Person p = campaign.getPerson(pid);
    		if(p.getHits() > 0 && !(entity instanceof Mech || entity instanceof Aero)) {
    			continue;
    		}
    		if(p.hasSkill(gunType)) {
    			sumGunnery += p.getSkill(gunType).getFinalSkillValue();
    			nGunners++;
    		}
    		if(p.hasSkill(SkillType.S_ARTILLERY) 
    				&& p.getSkill(SkillType.S_ARTILLERY).getFinalSkillValue() < artillery) {
    			artillery = p.getSkill(SkillType.S_ARTILLERY).getFinalSkillValue();
    		}
    	}
    	if(nDrivers > 0) {
    		piloting = (int)Math.round(((double)sumPiloting)/nDrivers);
    	}
    	if(nGunners > 0) {
    		gunnery = (int)Math.round(((double)sumGunnery)/nGunners);
    	}
    	if(entity instanceof Infantry) {
    		if(entity instanceof BattleArmor) {
    			for(int i = BattleArmor.LOC_TROOPER_1; i <= ((BattleArmor)entity).getTroopers(); i++) {
    				if(i <= nGunners) {
    		    		entity.setInternal(1, i);
    				} else {
    		    		entity.setInternal(IArmorState.ARMOR_DESTROYED, i);
    				}
    			}
    		}
    		entity.setInternal(nGunners, Infantry.LOC_INFANTRY);
    	}
    	if(drivers.isEmpty() && gunners.isEmpty()) {
    		entity.setCrew(null);
    		return;
    	}
    	Person commander = getCommander();
    	if(null == commander) {
    		entity.setCrew(null);
    		return;
    	}
    	//TODO: For the moment we need to max these out at 8 so people don't get errors
    	//when they customize in MM but we should put an option in MM to ignore those limits
    	//and set it to true when we start up through MHQ
    	gunnery = Math.min(gunnery, 7);
    	piloting = Math.min(piloting, 8);
    	artillery = Math.min(artillery, 7);
    	Pilot pilot = new Pilot(commander.getFullTitle(), gunnery, piloting);
    	pilot.setPortraitCategory(commander.getPortraitCategory());
    	pilot.setPortraitFileName(commander.getPortraitFileName());
    	pilot.setNickname(commander.getCallsign());
    	pilot.setExternalId(commander.getId());
    	pilot.setArtillery(artillery);
    	//create a new set of options. For now we will just assign based on commander, but
    	//we really should be more detailed about this.
    	PilotOptions options = new PilotOptions();
    	for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements();) {
             IOptionGroup group = i.nextElement();
             for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements();) {
                 IOption option = j.nextElement();         
                 option.setValue(commander.getOptions().getOption(option.getName()).getValue());
             }
    	}
    	pilot.setOptions(options);
    	if(usesSoloPilot()) {
    		if(!commander.isActive()) {
    			entity.setCrew(null);
    			return;
    		}
    		pilot.setHits(commander.getHits());
    	}
    	else if(entity instanceof Tank) {
    		if(nDrivers == 0 && nGunners == 0) {
    			//nobody is healthy
    			entity.setCrew(null);
    			return;
    		}
    		if(commander.getHits() > 0) {
    			((Tank)entity).setCommanderHit(true);
    		} else {
    			((Tank)entity).setCommanderHit(false);

    		}	
    		if(nDrivers == 0) {
    			((Tank)entity).setDriverHit(true);
    		} else {
    			((Tank)entity).setDriverHit(false);
    		} 		
    	}
    	else if(entity instanceof Infantry) {
    		if(nDrivers == 0 && nGunners == 0) {
    			//nobody is healthy
    			entity.setCrew(null);
    			return;
    		}
    	}
    	pilot.setToughness(commander.getToughness());
    	//TODO: game option to use tactics as command and ind init bonus
    	if(commander.hasSkill(SkillType.S_TACTICS)) {
    		pilot.setCommandBonus(commander.getSkill(SkillType.S_TACTICS).getFinalSkillValue());
    	}
    	entity.setCrew(pilot);  		
    }   
    

	public int getFullCrewSize() {
		if(entity instanceof Tank) {
			return (int)Math.ceil(entity.getWeight() / 15.0);
		}
		else if(entity instanceof Infantry) {
			return ((Infantry)entity).getSquadN() * ((Infantry)entity).getSquadSize();
		}
		else {
			return 1;
		}
	}
    
    public boolean canTakeMoreDrivers() {
    	int nDrivers = drivers.size();
    	if(entity instanceof Mech || entity instanceof Tank || entity instanceof Aero) {
    		//only one driver please
    		return nDrivers == 0;
    	}
    	else if(entity instanceof Infantry) {
    		return nDrivers < getFullCrewSize();
    	}
    	return false;
    }
    
    public boolean canTakeMoreGunners() {
    	int nGunners = gunners.size();
    	if(entity instanceof Mech || entity instanceof Aero) {
    		//only one gunner please
    		return nGunners == 0;
    	}
    	else if(entity instanceof Tank) {
    		return nGunners < (getFullCrewSize() - 1);
    	}
    	else if(entity instanceof Infantry) {
    		return nGunners < getFullCrewSize();
    	}
    	return false;
    }
    
    public boolean usesSoloPilot() {
    	return getFullCrewSize() == 1;
    }
    
    public boolean usesSoldiers() {
    	return entity instanceof Infantry;
    }
    
    public void addDriver(Person p) {
    	drivers.add(p.getId());
    	p.setUnitId(getId());
    	resetPilotAndEntity();
    }
    
    public void addGunner(Person p) {
    	gunners.add(p.getId());
    	p.setUnitId(getId());
    	resetPilotAndEntity();
    }
    
    public void addPilotOrSoldier(Person p) {
    	drivers.add(p.getId());
    	gunners.add(p.getId());
    	p.setUnitId(getId());
    	resetPilotAndEntity();
    }
    
    public void remove(Person p) {
    	p.setUnitId(-1);
    	drivers.remove(new Integer(p.getId()));
    	gunners.remove(new Integer(p.getId()));
    	resetPilotAndEntity();
    }
    
    public boolean isUnmanned() {
    	return (null == getCommander());
    }
    
    /**
     * This is only used for reverse compatability when loading in from 0.1.8 or before
     */
    public void reassignPilotReverseCompatabilityCheck() {
    	if(pilotId > 0) {
    		if(usesSoloPilot() || usesSoldiers()) {
    			if(canTakeMoreDrivers()) {
    				drivers.add(pilotId);
    			}
    			if(canTakeMoreGunners()) {
    				gunners.add(pilotId);
    			}
    		} else {
    			if(canTakeMoreDrivers()) {
    				drivers.add(pilotId);
    			}
    			else if(canTakeMoreGunners()) {
    				gunners.add(pilotId);
    			}
    		}
    		pilotId = -1;
    	}
    }
    

	public int getForceId() {
		return forceId;
	}
	
	public void setForceId(int id) {
		this.forceId = id;
	}
    
    public int getScenarioId() {
    	return scenarioId;
    }
    
    public void setScenarioId(int i) {
    	this.scenarioId = i;
    }
    
    public ArrayList<Person> getCrew() {
    	ArrayList<Person> crew = new ArrayList<Person>();
    	for(int id : drivers) {
    		Person p = campaign.getPerson(id);
    		if(null != p) {
    			crew.add(p);
    		}
    	}
    	if(!usesSoloPilot() && !usesSoldiers()) {
	    	for(int id : gunners) {
	    		Person p = campaign.getPerson(id);
	    		if(null != p) {
	    			crew.add(p);
	    		}
	    	}
    	}
    	return crew;
    }
    
    public ArrayList<Person> getActiveCrew() {
    	ArrayList<Person> crew = new ArrayList<Person>();
    	for(int id : drivers) {
    		Person p = campaign.getPerson(id);
    		if(null != p) {
    			if(p.getHits() > 0 && (entity instanceof Tank || entity instanceof Infantry)) {
    				continue;
    			}
    			crew.add(p);
    		}
    	}
    	if(!usesSoloPilot() && !usesSoldiers()) {
	    	for(int id : gunners) {
	    		Person p = campaign.getPerson(id);
	    		if(null != p) {
	    			if(p.getHits() > 0 && (entity instanceof Tank || entity instanceof Infantry)) {
	    				continue;
	    			}
	    			crew.add(p);
	    		}
	    	}
    	}
    	return crew;
    }
    
    public boolean isDriver(Person person) {
    	for(int id : drivers) {
    		if(person.getId() == id) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public boolean isCommander(Person person) {
    	return person.getId() == getCommander().getId();
    }    
    
    public void setRefit(Refit r) {
    	refit = r;
    }
    
    public Refit getRefit() {
    	return refit;
    }
    
    public boolean isRefitting() {
    	return null != refit;
    }
    
    public String getName() {
    	return entity.getShortName();
    }
    
    @Override
    public boolean equals(Object o) {
    	return o instanceof Unit && ((Unit)o).getId() == id && ((Unit)o).getName().equals(getName());
    }
}
