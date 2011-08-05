/*
 * PartsStore.java
 * 
 * Copyright (c) 2011 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.WeaponType;
import mekhq.campaign.parts.AeroHeatSink;
import mekhq.campaign.parts.AmmoBin;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.EquipmentPart;
import mekhq.campaign.parts.HeatSink;
import mekhq.campaign.parts.JumpJet;
import mekhq.campaign.parts.MekActuator;
import mekhq.campaign.parts.MekGyro;
import mekhq.campaign.parts.MekLifeSupport;
import mekhq.campaign.parts.MekSensor;
import mekhq.campaign.parts.Part;


/**
 * This is a parts store which will contain one copy of every possible
 * part that might be needed as well as a variety of helper functions to
 * acquire parts.
 * 
 * We could in the future extend this to different types of stores that have different finite numbers of
 * parts in inventory
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class PartsStore implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1686222527383868364L;
	
	private ArrayList<Part> parts;
	
	public PartsStore() {
		parts = new ArrayList<Part>();
		stock();
	}
	
	public ArrayList<PartInventory> getInventory() {
		ArrayList<PartInventory> partsInventory = new ArrayList<PartInventory>();

		Iterator<Part> itParts = parts.iterator();
		while (itParts.hasNext()) {
			Part part = itParts.next();
			partsInventory.add(new PartInventory(part, 1));
		}

		return partsInventory;
	}
	
	public void stock() {
		stockWeaponsAmmoAndEquipment();	
		stockMekActuators();
		stockEngines();
		stockGyros();
		stockMekComponents();
		stockAeroComponents();
		stockVeeComponents();
		stockArmor();
		stockMekLocations();
		stockVeeLocations();
	}
	
	private void stockWeaponsAmmoAndEquipment() {
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
            EquipmentType et = e.nextElement();
            if(!et.isHittable()) {
            	continue;
            }
			if(et instanceof AmmoType) {
				parts.add(new AmmoStorage(0, et, ((AmmoType)et).getShots()));
			}
			else if(et instanceof MiscType && (et.hasFlag(MiscType.F_HEAT_SINK) || et.hasFlag(MiscType.F_DOUBLE_HEAT_SINK))) {
            	parts.add(new HeatSink(0, et, -1));
			} else if(et instanceof MiscType && et.hasFlag(MiscType.F_JUMP_JET)) {
				parts.add(new JumpJet(0, et, -1));
			} else {
				parts.add(new EquipmentPart(0, et, -1));
			}
        }
        //lets throw aero heat sinks in here as well
        parts.add(new AeroHeatSink(0, Aero.HEAT_SINGLE));
        parts.add(new AeroHeatSink(0, Aero.HEAT_DOUBLE));
	}
	
	private void stockMekActuators() {
		for(int i = Mech.ACTUATOR_UPPER_ARM; i <= Mech.ACTUATOR_FOOT; i++) {
			if(i == Mech.ACTUATOR_HIP) {
				continue;
			}
			int ton = 20;
			while(ton <= 100) {
				parts.add(new MekActuator(ton, i, -1));
				ton += 5;
			}
		}
	}
	
	private void stockEngines() {
		int rating = 5;
		while(rating <= 400) {
			for(int i = 0; i <= Engine.FISSION; i++) {
				Engine engine = new Engine(rating, i, 0);
				if(engine.engineValid) {
					parts.add(new EnginePart(0, engine));
				}
				engine = new Engine(rating, i, Engine.CLAN_ENGINE);
				if(engine.engineValid) {
					parts.add(new EnginePart(0, engine));
				}
			}
			rating += 5;
		}
	}
	
	private void stockGyros() {
		for(double i = 0.5; i <= 8.0; i += 0.5) {
			//standard at intervals of 1.0, up to 4
			if(i % 1.0 == 0 && i <= 4.0) {
				parts.add(new MekGyro(0, Mech.GYRO_STANDARD, i));
			}
			//compact at intervals of 1.5, up to 6
			if(i % 1.5 == 0 && i <= 6.0) {
				parts.add(new MekGyro(0, Mech.GYRO_COMPACT, i));
			}
			//XL at 0.5 intervals up to 2
			if(i % 0.5 == 0 && i <= 2.0) {
				parts.add(new MekGyro(0, Mech.GYRO_XL, i));
			}
			//Heavy duty at 2.0 intervals
			if(i % 2.0 == 0) {
				parts.add(new MekGyro(0, Mech.GYRO_HEAVY_DUTY, i));
			}
			
		}
	}
	
	private void stockMekComponents() {
		parts.add(new MekLifeSupport(0));
		for(int ton = 20; ton <= 100; ton += 5) {
			parts.add(new MekSensor(ton));
		}
	}
	
	private void stockAeroComponents() {
		//TODO: implement me
	}
	
	private void stockVeeComponents() {
		//TODO: implement me
	}
	
	private void stockArmor() {
		//Standard armor
		int amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_STANDARD, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_STANDARD, amount, -1, false, false));
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_STANDARD, true));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_STANDARD, amount, -1, false, true));
		//Ferro-Fibrous
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_FERRO_FIBROUS, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_FERRO_FIBROUS, amount, -1, false, false));
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_FERRO_FIBROUS, true));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_FERRO_FIBROUS, amount, -1, false, true));
		//Reactive
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_REACTIVE, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_REACTIVE, amount, -1, false, false));
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_REACTIVE, true));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_REACTIVE, amount, -1, false, true));
		//Reflective
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_REFLECTIVE, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_REFLECTIVE, amount, -1, false, false));
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_REFLECTIVE, true));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_REFLECTIVE, amount, -1, false, true));
		//Hardened
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_HARDENED, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_HARDENED, amount, -1, false, false));
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_HARDENED, true));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_HARDENED, amount, -1, false, true));
		//Light/Heavy FF
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_LIGHT_FERRO, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_LIGHT_FERRO, amount, -1, false, false));
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_HEAVY_FERRO, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_HEAVY_FERRO, amount, -1, false, false));
		//Stealth
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_STEALTH, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_STEALTH, amount, -1, false, false));
		//TODO: finish this 
	/*	
		public static final int T_ARMOR_STANDARD = 0;
	    public static final int T_ARMOR_FERRO_FIBROUS = 1;
	    public static final int T_ARMOR_REACTIVE = 2;
	    public static final int T_ARMOR_REFLECTIVE = 3;
	    public static final int T_ARMOR_HARDENED = 4;
	    public static final int T_ARMOR_LIGHT_FERRO = 5;
	    public static final int T_ARMOR_HEAVY_FERRO = 6;
	    public static final int T_ARMOR_PATCHWORK = 7;
	    public static final int T_ARMOR_STEALTH = 8;
	    public static final int T_ARMOR_FERRO_FIBROUS_PROTO = 9;
	    public static final int T_ARMOR_COMMERCIAL = 10;
	    public static final int T_ARMOR_FERRO_CARBIDE = 11;
	    public static final int T_ARMOR_LAMELLOR_FERRO_CARBIDE = 12;
	    public static final int T_ARMOR_FERRO_IMP = 13;
	    public static final int T_ARMOR_INDUSTRIAL = 14;
	    public static final int T_ARMOR_HEAVY_INDUSTRIAL = 15;
	    public static final int T_ARMOR_FERRO_LAMELLOR = 16;
	    public static final int T_ARMOR_PRIMITIVE = 17;
	    */
	}
	
	private void stockMekLocations() {
		//TODO: implement me
	}
	
	private void stockVeeLocations() {
		//TODO: implement me
	}
	
}