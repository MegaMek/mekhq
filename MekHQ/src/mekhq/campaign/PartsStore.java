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

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.Engine;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.verifier.TestEntity;
import megamek.common.weapons.BayWeapon;
import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.campaign.parts.AeroHeatSink;
import mekhq.campaign.parts.AeroSensor;
import mekhq.campaign.parts.AmmoStorage;
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
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Rotor;
import mekhq.campaign.parts.Turret;
import mekhq.campaign.parts.VeeSensor;
import mekhq.campaign.parts.VeeStabiliser;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.JumpJet;
import mekhq.campaign.parts.equipment.MASC;


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
	
	public PartsStore(Campaign c) {
		parts = new ArrayList<Part>();
		stock(c);
	}
	
	public ArrayList<PartInventory> getInventory(Campaign c) {
		ArrayList<PartInventory> partsInventory = new ArrayList<PartInventory>();

		Iterator<Part> itParts = parts.iterator();
		while (itParts.hasNext()) {
			Part part = itParts.next();
			partsInventory.add(new PartInventory(part, 1));
		}

		return partsInventory;
	}
	
	public void stock(Campaign c) {
		stockWeaponsAmmoAndEquipment(c);	
		stockMekActuators(c);
		stockEngines(c);
		stockGyros(c);
		stockMekComponents(c);
		stockAeroComponents(c);
		stockVeeComponents(c);
		stockArmor(c);
		stockMekLocations(c);
		stockVeeLocations(c);
	}
	
	private void stockWeaponsAmmoAndEquipment(Campaign c) {
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
            EquipmentType et = e.nextElement();
            if(!et.isHittable()) {
            	continue;
            }
            //TODO: we are still adding a lot of non-hittable equipment
			if(et instanceof AmmoType) {
				parts.add(new AmmoStorage(0, et, ((AmmoType)et).getShots(), c));
			}
			else if(et instanceof MiscType && (et.hasFlag(MiscType.F_HEAT_SINK) || et.hasFlag(MiscType.F_DOUBLE_HEAT_SINK))) {
            	parts.add(new HeatSink(0, et, -1, c));
			} else if(et instanceof MiscType && et.hasFlag(MiscType.F_JUMP_JET)) {
				parts.add(new JumpJet(55, et, -1, c));
				parts.add(new JumpJet(85, et, -1, c));
				parts.add(new JumpJet(100, et, -1, c));
			} else if (et instanceof InfantryWeapon 
					|| et instanceof BayWeapon
					|| et instanceof InfantryAttack) {
				//TODO: need to also get rid of infantry attacks (like Swarm Mek)
				continue;
			} else if(et.hasFlag(MiscType.F_MASC)) {
				if(et.hasSubType(MiscType.S_SUPERCHARGER)) {
					for(int rating = 10; rating <= 400; rating += 5) {
						for(double eton = 0.5; eton <= 10.5; eton += 0.5) {
							float weight = Engine.ENGINE_RATINGS[(int) Math.ceil(rating / 5.0)];
							float minweight = weight * 0.5f;
							minweight = (float) (Math.ceil((TestEntity.ceilMaxHalf(minweight, TestEntity.CEIL_HALFTON) / 10.0) * 2.0) / 2.0);
							float maxweight = weight * 2.0f;
							maxweight = (float) (Math.ceil((TestEntity.ceilMaxHalf(maxweight, TestEntity.CEIL_HALFTON) / 10.0) * 2.0) / 2.0);
							if(eton < minweight || eton > maxweight) {
								continue;
							}
							MASC sp = new MASC(0, et, -1 , c, rating);
							sp.setEquipTonnage(eton);
							parts.add(sp);
						}
					}
				} else {
					//need to do it by rating and unit tonnage
					for(int ton = 20; ton <= 100; ton += 5) {
						for(int rating = 10; rating <= 400; rating += 5) {
							if(rating < ton || (rating % ton) != 0) {
								continue;
							}
							parts.add(new MASC(ton, et, -1, c, rating));
						}
					}
				}
			} else {
				if(EquipmentPart.hasVariableTonnage(et)) {
					EquipmentPart epart;
					for(double ton = EquipmentPart.getStartingTonnage(et); ton <= EquipmentPart.getMaxTonnage(et); ton += EquipmentPart.getTonnageIncrement(et)) {
						epart = new EquipmentPart(0, et, -1, c);
						epart.setEquipTonnage(ton);
						parts.add(epart);
						//TODO: still need to deal with talons (unit tonnage) and masc (engine rating)
					}
				} else {
					parts.add(new EquipmentPart(0, et, -1, c));
				}
			}
        }
        //lets throw aero heat sinks in here as well
        parts.add(new AeroHeatSink(0, Aero.HEAT_SINGLE, c));
        parts.add(new AeroHeatSink(0, Aero.HEAT_DOUBLE, c));
	}
	
	private void stockMekActuators(Campaign c) {
		for(int i = Mech.ACTUATOR_UPPER_ARM; i <= Mech.ACTUATOR_FOOT; i++) {
			if(i == Mech.ACTUATOR_HIP) {
				continue;
			}
			int ton = 20;
			while(ton <= 100) {
				parts.add(new MekActuator(ton, i, -1, c));
				ton += 5;
			}
		}
	}
	
	private double getEngineTonnage(Engine engine) {
		float weight = Engine.ENGINE_RATINGS[(int) Math.ceil(engine.getRating() / 5.0)];
        switch (engine.getEngineType()) {
            case Engine.COMBUSTION_ENGINE:
                weight *= 2.0f;
                break;
            case Engine.NORMAL_ENGINE:
                break;
            case Engine.XL_ENGINE:
                weight *= 0.5f;
                break;
            case Engine.LIGHT_ENGINE:
                weight *= 0.75f;
                break;
            case Engine.XXL_ENGINE:
                weight /= 3f;
                break;
            case Engine.COMPACT_ENGINE:
                weight *= 1.5f;
                break;
            case Engine.FISSION:
                weight *= 1.75;
                weight = Math.max(5, weight);
                break;
            case Engine.FUEL_CELL:
                weight *= 1.2;
                break;
            case Engine.NONE:
                return 0;
        }
        weight = TestEntity.ceilMaxHalf(weight, TestEntity.CEIL_HALFTON);
        if (engine.hasFlag(Engine.TANK_ENGINE) && engine.isFusion()) {
            weight *= 1.5f;
        }
        float toReturn = TestEntity.ceilMaxHalf(weight, TestEntity.CEIL_HALFTON);
        return toReturn;
	}
	
	private void stockEngines(Campaign c) {					
		Engine engine;
		for(int rating = 10; rating <= 400; rating += 5) {
			for(int ton = 5; ton <= 100; ton += 5) {
				for(int i = 0; i <= Engine.FISSION; i++) {
					if(rating >= ton && rating % ton == 0) {
						engine = new Engine(rating, i, 0);
						if(engine.engineValid) {
							parts.add(new EnginePart(ton, engine, c, false));
						}
						engine = new Engine(rating, i, Engine.CLAN_ENGINE);
						if(engine.engineValid) {
							parts.add(new EnginePart(ton, engine, c, false));
						}
					}
					engine = new Engine(rating, i, Engine.TANK_ENGINE);
					if(engine.engineValid) {
						parts.add(new EnginePart(ton, engine, c, false));
					}
					if((ton/5) > getEngineTonnage(engine)) {
						engine = new Engine(rating, i, Engine.TANK_ENGINE);
						if(engine.engineValid) {
							parts.add(new EnginePart(ton, engine, c, true));
						}
					}
					engine = new Engine(rating, i, Engine.TANK_ENGINE | Engine.CLAN_ENGINE);
					if(engine.engineValid) {
						parts.add(new EnginePart(ton, engine, c, false));
					}
					if((ton/5) > getEngineTonnage(engine)) {
						engine = new Engine(rating, i, Engine.TANK_ENGINE | Engine.CLAN_ENGINE);
						if(engine.engineValid) {
							parts.add(new EnginePart(ton, engine, c, true));
						}
					}				
				}
			}
		}
	}
	
	private void stockGyros(Campaign c) {
		for(double i = 0.5; i <= 8.0; i += 0.5) {
			//standard at intervals of 1.0, up to 4
			if(i % 1.0 == 0 && i <= 4.0) {
				parts.add(new MekGyro(0, Mech.GYRO_STANDARD, i, c));
			}
			//compact at intervals of 1.5, up to 6
			if(i % 1.5 == 0 && i <= 6.0) {
				parts.add(new MekGyro(0, Mech.GYRO_COMPACT, i, c));
			}
			//XL at 0.5 intervals up to 2
			if(i % 0.5 == 0 && i <= 2.0) {
				parts.add(new MekGyro(0, Mech.GYRO_XL, i, c));
			}
			//Heavy duty at 2.0 intervals
			if(i % 2.0 == 0) {
				parts.add(new MekGyro(0, Mech.GYRO_HEAVY_DUTY, i, c));
			}
			
		}
	}
	
	private void stockMekComponents(Campaign c) {
		parts.add(new MekLifeSupport(0, c));
		for(int ton = 20; ton <= 100; ton += 5) {
			parts.add(new MekSensor(ton, c));
		}
		for(int type = Mech.COCKPIT_STANDARD; type < Mech.COCKPIT_STRING.length; type++) {
			parts.add(new MekCockpit(0, type, c));
		}
	}
	
	private void stockAeroComponents(Campaign c) {
		parts.add(new AeroHeatSink(0, Aero.HEAT_SINGLE, c));
		parts.add(new AeroHeatSink(0, Aero.HEAT_DOUBLE, c));
		parts.add(new AeroSensor(0, false, c));
		parts.add(new AeroSensor(0, true, c));
		parts.add(new Avionics(0, c));
		parts.add(new FireControlSystem(0, c));
		parts.add(new LandingGear(0, c));
	}
	
	private void stockVeeComponents(Campaign c) {
		parts.add(new VeeSensor(0, c));
		parts.add(new VeeStabiliser(0,-1, c));
		for(int ton = 5; ton <= 100; ton=ton+5) {
			parts.add(new Rotor(ton, c));
			parts.add(new Turret(ton, -1, c));
		}
	}
	
	private void stockArmor(Campaign c) {
		//Standard armor
		int amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_STANDARD, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_STANDARD, amount, -1, false, false, c));
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_STANDARD, true));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_STANDARD, amount, -1, false, true, c));
		//Ferro-Fibrous
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_FERRO_FIBROUS, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_FERRO_FIBROUS, amount, -1, false, false, c));
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_FERRO_FIBROUS, true));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_FERRO_FIBROUS, amount, -1, false, true, c));
		//Reactive
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_REACTIVE, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_REACTIVE, amount, -1, false, false, c));
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_REACTIVE, true));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_REACTIVE, amount, -1, false, true, c));
		//Reflective
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_REFLECTIVE, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_REFLECTIVE, amount, -1, false, false, c));
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_REFLECTIVE, true));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_REFLECTIVE, amount, -1, false, true, c));
		//Hardened
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_HARDENED, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_HARDENED, amount, -1, false, false, c));
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_HARDENED, true));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_HARDENED, amount, -1, false, true, c));
		//Light/Heavy FF
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_LIGHT_FERRO, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_LIGHT_FERRO, amount, -1, false, false, c));
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_HEAVY_FERRO, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_HEAVY_FERRO, amount, -1, false, false, c));
		//Stealth
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_STEALTH, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_STEALTH, amount, -1, false, false, c));
		//Commercial
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_COMMERCIAL, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_COMMERCIAL, amount, -1, false, false, c));
		//Industrial
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_INDUSTRIAL, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_INDUSTRIAL, amount, -1, false, false, c));
		//Heavy Industrial
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL, amount, -1, false, false, c));
		//Ferro-Lamellor
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_FERRO_LAMELLOR, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_FERRO_LAMELLOR, amount, -1, false, true, c));
		//Primitive
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_PRIMITIVE, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_PRIMITIVE, amount, -1, false, false, c));
		/*
		 * These are all warship armors
		//Ferro-Carbide
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_FERRO_CARBIDE, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_FERRO_CARBIDE, amount, -1, false, false));
		//Lemellor Ferro Carbide
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_LAMELLOR_FERRO_CARBIDE, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_LAMELLOR_FERRO_CARBIDE, amount, -1, false, false));
		//Ferro Improved
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_FERRO_IMP, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_FERRO_IMP, amount, -1, false, false));
		*/
	}
	
	private void stockMekLocations(Campaign c) {
		for(int loc = Mech.LOC_HEAD; loc <= Mech.LOC_LLEG; loc++) {
			for(int ton = 20; ton <= 100; ton=ton+5) {
				for(int type = 0; type < EquipmentType.structureNames.length; type++) {
					parts.add(new MekLocation(loc, ton, type, false, false, c));
					parts.add(new MekLocation(loc, ton, type, true, false, c));
					if(loc > Mech.LOC_LT) {
						parts.add(new MekLocation(loc, ton, type, false, true, c));
						parts.add(new MekLocation(loc, ton, type, true, true, c));
					}
				}
			}
		}
	}
	
	private void stockVeeLocations(Campaign c) {
		//TODO: implement me
	}
	
}