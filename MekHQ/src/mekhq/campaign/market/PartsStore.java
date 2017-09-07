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

package mekhq.campaign.market;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.MiscType;
import megamek.common.Protomech;
import megamek.common.TechConstants;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.verifier.TestEntity;
import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.bayweapons.BayWeapon;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.AeroHeatSink;
import mekhq.campaign.parts.AeroSensor;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Avionics;
import mekhq.campaign.parts.BaArmor;
import mekhq.campaign.parts.BattleArmorSuit;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.FireControlSystem;
import mekhq.campaign.parts.LandingGear;
import mekhq.campaign.parts.MekActuator;
import mekhq.campaign.parts.MekCockpit;
import mekhq.campaign.parts.MekGyro;
import mekhq.campaign.parts.MekLifeSupport;
import mekhq.campaign.parts.MekLocation;
import mekhq.campaign.parts.MekSensor;
import mekhq.campaign.parts.OmniPod;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.ProtomekArmActuator;
import mekhq.campaign.parts.ProtomekArmor;
import mekhq.campaign.parts.ProtomekJumpJet;
import mekhq.campaign.parts.ProtomekLegActuator;
import mekhq.campaign.parts.ProtomekLocation;
import mekhq.campaign.parts.ProtomekSensor;
import mekhq.campaign.parts.QuadVeeGear;
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

	private static int EXPECTED_SIZE = 50000;
	
	private ArrayList<Part> parts;
	private Map<String, Part> nameAndDetailMap;

	public PartsStore(Campaign c) {
		parts = new ArrayList<Part>(EXPECTED_SIZE);
		nameAndDetailMap = new HashMap<String, Part>(EXPECTED_SIZE);
		stock(c);
	}

	public ArrayList<Part> getInventory() {
		return parts;
	}
	
	public Part getByNameAndDetails(String nameAndDetails) {
		return nameAndDetailMap.get(nameAndDetails);
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
		stockProtomekLocations(c);
		stockProtomekComponents(c);
		stockBattleArmorSuits(c);
		
		Pattern cleanUp1 = Pattern.compile("\\d+\\shit\\(s\\),\\s"); //$NON-NLS-1$
		Pattern cleanUp2 = Pattern.compile("\\d+\\shit\\(s\\)"); //$NON-NLS-1$
		StringBuilder sb = new StringBuilder();
		for(Part p : parts) {
			p.setBrandNew(true);
			sb.setLength(0);
			sb.append(p.getName());
			if(!(p instanceof Armor || p instanceof BaArmor || p instanceof ProtomekArmor)) {
				String details = p.getDetails();
				details = cleanUp2.matcher(cleanUp1.matcher(details).replaceFirst("")).replaceFirst(""); //$NON-NLS-1$ //$NON-NLS-2$
				if (details.length() > 0) {
					sb.append(" (").append(details).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
		    }
			nameAndDetailMap.put(sb.toString(), p);
		}
	}

	private void stockBattleArmorSuits(Campaign c) {
		//this is just a test
		for(MechSummary summary : MechSummaryCache.getInstance().getAllMechs()) {
			if(!summary.getUnitType().equals("BattleArmor")) {
				continue;
			}
			//FIXME: I can't pull entity movement mode and quad shape off of mechsummary
			//try loading the full entity, but this might take too long
			if(null != summary) {
		 		Entity newEntity = null;
		 		try {
		 			newEntity = new MechFileParser(summary.getSourceFile(), summary.getEntryName()).getEntity();
				} catch (EntityLoadingException e) {
					e.printStackTrace();
				}
		 		if(null != newEntity) {
		 			BattleArmorSuit ba = new BattleArmorSuit(summary.getChassis(), summary.getModel(), (int)summary.getTons(), 1, summary.getWeightClass(), summary.getWalkMp(), summary.getJumpMp(), newEntity.entityIsQuad(), summary.isClan(), newEntity.getMovementMode(), c);
		 			parts.add(ba);
		 		}
			}
		}
	}

	private void stockWeaponsAmmoAndEquipment(Campaign c) {
        for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
            EquipmentType et = e.nextElement();
            if(!et.isHittable() &&
            		!(et instanceof MiscType && ((MiscType)et).hasFlag(MiscType.F_BA_MANIPULATOR))) {
            	continue;
            }
            //TODO: we are still adding a lot of non-hittable equipment
			if(et instanceof AmmoType) {
				if(((AmmoType)et).hasFlag(AmmoType.F_BATTLEARMOR)) {
					//BA ammo has one shot listed as the amount. Do it as 1 ton blocks
					int shots = (int) Math.floor(1000/((AmmoType)et).getKgPerShot());
					if(shots <= 0) {
						//FIXME: no idea what to do here, these really should be fixed on the MM side
						//because presumably this is happening because KgperShot is -1 or 0
						shots = 20;
					}
					parts.add(new AmmoStorage(0, et, shots, c));
				} else {
					parts.add(new AmmoStorage(0, et, ((AmmoType)et).getShots(), c));
				}
			} else if(et instanceof MiscType && (((MiscType)et).hasFlag(MiscType.F_HEAT_SINK) || ((MiscType)et).hasFlag(MiscType.F_DOUBLE_HEAT_SINK))) {
            	parts.add(new HeatSink(0, et, -1, false, c));
            	parts.add(new HeatSink(0, et, -1, true, c));
			} else if(et instanceof MiscType && ((MiscType)et).hasFlag(MiscType.F_JUMP_JET)) {
				//need to do it by rating and unit tonnage
				for(int ton = 10; ton <= 100; ton += 5) {
                    parts.add(new JumpJet(ton, et, -1, false, c));
                    parts.add(new JumpJet(ton, et, -1, true, c));
				}
			} else if ((et instanceof MiscType && ((MiscType)et).hasFlag(MiscType.F_TANK_EQUIPMENT) && ((MiscType)et).hasFlag(MiscType.F_CHASSIS_MODIFICATION))
					|| et instanceof BayWeapon
					|| et instanceof InfantryAttack) {
				continue;
			} else if(et instanceof MiscType && ((MiscType)et).hasFlag(MiscType.F_BA_EQUIPMENT)
						&& !((MiscType)et).hasFlag(MiscType.F_BA_MANIPULATOR)) {
				continue;
			} else if(et instanceof MiscType && ((MiscType)et).hasFlag(MiscType.F_MASC)) {
				if(et.hasSubType(MiscType.S_SUPERCHARGER)) {
					for(int rating = 10; rating <= 400; rating += 5) {
						for(double eton = 0.5; eton <= 10.5; eton += 0.5) {
							double weight = Engine.ENGINE_RATINGS[(int) Math.ceil(rating / 5.0)];
							double minweight = weight * 0.5f;
							minweight = Math.ceil((TestEntity.ceilMaxHalf(minweight, TestEntity.Ceil.HALFTON) / 10.0) * 2.0) / 2.0;
							double maxweight = weight * 2.0f;
							maxweight = Math.ceil((TestEntity.ceilMaxHalf(maxweight, TestEntity.Ceil.HALFTON) / 10.0) * 2.0) / 2.0;
							if(eton < minweight || eton > maxweight) {
								continue;
							}
                            MASC sp = new MASC(0, et, -1 , c, rating, false);
                            sp.setEquipTonnage(eton);
                            parts.add(sp);
                            sp = new MASC(0, et, -1 , c, rating, true);
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
                            parts.add(new MASC(ton, et, -1, c, rating, false));
                            parts.add(new MASC(ton, et, -1, c, rating, true));
						}
					}
				}
			} else {
				if(EquipmentPart.hasVariableTonnage(et)) {
					EquipmentPart epart;
					for(double ton = EquipmentPart.getStartingTonnage(et); ton <= EquipmentPart.getMaxTonnage(et); ton += EquipmentPart.getTonnageIncrement(et)) {
						epart = new EquipmentPart(0, et, -1, false, c);
						epart.setEquipTonnage(ton);
						parts.add(epart);
						if (!et.isOmniFixedOnly()) {
						    epart = new EquipmentPart(0, et, -1, true, c);
	                        epart.setEquipTonnage(ton);
	                        parts.add(epart);
						}
						//TODO: still need to deal with talons (unit tonnage) and masc (engine rating)
					}
				} else {
				    Part p = new EquipmentPart(0, et, -1, false, c);
				    parts.add(p);
                    if (p.isOmniPoddable()) {
                        parts.add(new EquipmentPart(0, et, -1, true, c));
                        parts.add(new OmniPod(p, c));
                    }
				}
			}
        }
        //lets throw aero heat sinks in here as well
        AeroHeatSink hs = new AeroHeatSink(0, Aero.HEAT_SINGLE, false, c);
        parts.add(hs);
        parts.add(new OmniPod(hs, c));
        parts.add(new AeroHeatSink(0, Aero.HEAT_SINGLE, true, c));
        
        hs = new AeroHeatSink(0, Aero.HEAT_DOUBLE, false, c);
        parts.add(hs);
        parts.add(new OmniPod(hs, c));
        parts.add(new AeroHeatSink(0, Aero.HEAT_DOUBLE, true, c));
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
	    double weight = Engine.ENGINE_RATINGS[(int) Math.ceil(engine.getRating() / 5.0)];
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
        weight = TestEntity.ceilMaxHalf(weight, TestEntity.Ceil.HALFTON);
        if (engine.hasFlag(Engine.TANK_ENGINE) && engine.isFusion()) {
            weight *= 1.5f;
        }
        double toReturn = TestEntity.ceilMaxHalf(weight, TestEntity.Ceil.HALFTON);
        return toReturn;
	}

	private void stockEngines(Campaign c) {
		Engine engine;
		int year = c.getCalendar().get(GregorianCalendar.YEAR);
		for(int rating = 10; rating <= 400; rating += 5) {
			for(int ton = 5; ton <= 100; ton += 5) {
				for(int i = 0; i <= Engine.FISSION; i++) {
					if(rating >= ton && rating % ton == 0) {
						engine = new Engine(rating, i, 0);
						if(engine.engineValid) {
							parts.add(new EnginePart(ton, engine, c, false));
						}
						if(engine.getTechType(year) != TechConstants.T_ALLOWED_ALL) {
							engine = new Engine(rating, i, Engine.CLAN_ENGINE);
							if(engine.engineValid) {
								parts.add(new EnginePart(ton, engine, c, false));
							}
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
					if(engine.getTechType(year) != TechConstants.T_ALLOWED_ALL) {
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
	}

	private void stockGyros(Campaign c) {
		for(double i = 0.5; i <= 8.0; i += 0.5) {
			//standard at intervals of 1.0, up to 4
			if(i % 1.0 == 0 && i <= 4.0) {
				parts.add(new MekGyro(0, Mech.GYRO_STANDARD, i, false, c));
				parts.add(new MekGyro(0, Mech.GYRO_STANDARD, i, true, c));
			}
			//compact at intervals of 1.5, up to 6
			if(i % 1.5 == 0 && i <= 6.0) {
				parts.add(new MekGyro(0, Mech.GYRO_COMPACT, i, false, c));
			}
			//XL at 0.5 intervals up to 2
			if(i % 0.5 == 0 && i <= 2.0) {
				parts.add(new MekGyro(0, Mech.GYRO_XL, i, false, c));
			}
			//Heavy duty at 2.0 intervals
			if(i % 2.0 == 0) {
				parts.add(new MekGyro(0, Mech.GYRO_HEAVY_DUTY, i, false, c));
			}

		}
	}

	private void stockMekComponents(Campaign c) {
		parts.add(new MekLifeSupport(0, c));
		for(int ton = 20; ton <= 100; ton += 5) {
			parts.add(new MekSensor(ton, c));
			parts.add(new QuadVeeGear(ton, c));
		}
		for(int type = Mech.COCKPIT_STANDARD; type < Mech.COCKPIT_STRING.length; type++) {
		    parts.add(new MekCockpit(0, type, false, c));
		    if (type != Mech.COCKPIT_SMALL) {
		        parts.add(new MekCockpit(0, type, true, c));
		    }
		}
	}

	private void stockAeroComponents(Campaign c) {
		parts.add(new AeroHeatSink(0, Aero.HEAT_SINGLE, false, c));
		parts.add(new AeroHeatSink(0, Aero.HEAT_DOUBLE, false, c));
		for(int ton = 5; ton <= 200; ton += 5) {
			parts.add(new AeroSensor(ton, false, c));
		}
		parts.add(new AeroSensor(0, true, c));
		parts.add(new Avionics(0, c));
		parts.add(new FireControlSystem(0, 0, c));
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
		/*
		 * Mech, Aero, and Vehicle armors
		 */
		//Standard armor
		int amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_STANDARD, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_STANDARD, amount, -1, false, false, c));
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
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_LIGHT_FERRO, true));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_LIGHT_FERRO, amount, -1, false, true, c));
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
		//Ferro-Aluminum
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_ALUM, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_ALUM, amount, -1, false, false, c));
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_ALUM, true));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_ALUM, amount, -1, false, true, c));
		//Light/Heavy Ferro-Aluminum
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_LIGHT_ALUM, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_LIGHT_ALUM, amount, -1, false, false, c));
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_HEAVY_ALUM, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_HEAVY_ALUM, amount, -1, false, false, c));
		//Primitive
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_PRIMITIVE, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_PRIMITIVE, amount, -1, false, false, c));
		/*
		 * Warship armors
		*/
		//Ferro-Carbide
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_FERRO_CARBIDE, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_FERRO_CARBIDE, amount, -1, false, false, c));
		//Lemellor Ferro Carbide
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_LAMELLOR_FERRO_CARBIDE, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_LAMELLOR_FERRO_CARBIDE, amount, -1, false, false, c));
		//Improved Ferro-Aluminum
		amount = (int) (5.0 * 16.0 * EquipmentType.getArmorPointMultiplier(EquipmentType.T_ARMOR_FERRO_IMP, false));
		parts.add(new Armor(0, EquipmentType.T_ARMOR_FERRO_IMP, amount, -1, false, false, c));
		/*
		 * Protomek Armor
		 */
		parts.add(new ProtomekArmor(0, 100, -1, true, c));
		/*
		*BA ARMOR
		*/
		parts.add(new BaArmor(0, (int)Math.round(5 * BaArmor.getPointsPerTon(EquipmentType.T_ARMOR_BA_STANDARD, false)), EquipmentType.T_ARMOR_BA_STANDARD, -1, false, c));
		parts.add(new BaArmor(0, (int)Math.round(5 * BaArmor.getPointsPerTon(EquipmentType.T_ARMOR_BA_STANDARD, true)), EquipmentType.T_ARMOR_BA_STANDARD, -1, true, c));
		parts.add(new BaArmor(0, (int)Math.round(5 * BaArmor.getPointsPerTon(EquipmentType.T_ARMOR_BA_STANDARD_ADVANCED, true)), EquipmentType.T_ARMOR_BA_STANDARD_ADVANCED, -1, true, c));
		parts.add(new BaArmor(0, (int)Math.round(5 * BaArmor.getPointsPerTon(EquipmentType.T_ARMOR_BA_STANDARD_PROTOTYPE, true)), EquipmentType.T_ARMOR_BA_STANDARD_PROTOTYPE, -1, true, c));
		parts.add(new BaArmor(0, (int)Math.round(5 * BaArmor.getPointsPerTon(EquipmentType.T_ARMOR_BA_STEALTH_BASIC, false)), EquipmentType.T_ARMOR_BA_STEALTH_BASIC, -1, false, c));
		parts.add(new BaArmor(0, (int)Math.round(5 * BaArmor.getPointsPerTon(EquipmentType.T_ARMOR_BA_STEALTH_BASIC, true)), EquipmentType.T_ARMOR_BA_STEALTH_BASIC, -1, true, c));
		parts.add(new BaArmor(0, (int)Math.round(5 * BaArmor.getPointsPerTon(EquipmentType.T_ARMOR_BA_STEALTH_IMP, false)), EquipmentType.T_ARMOR_BA_STEALTH_IMP, -1, false, c));
		parts.add(new BaArmor(0, (int)Math.round(5 * BaArmor.getPointsPerTon(EquipmentType.T_ARMOR_BA_STEALTH_IMP, true)), EquipmentType.T_ARMOR_BA_STEALTH_IMP, -1, true, c));
		parts.add(new BaArmor(0, (int)Math.round(5 * BaArmor.getPointsPerTon(EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE, true)), EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE, -1, true, c));
		parts.add(new BaArmor(0, (int)Math.round(5 * BaArmor.getPointsPerTon(EquipmentType.T_ARMOR_BA_STEALTH, false)), EquipmentType.T_ARMOR_BA_STEALTH, -1, false, c));
		parts.add(new BaArmor(0, (int)Math.round(5 * BaArmor.getPointsPerTon(EquipmentType.T_ARMOR_BA_STEALTH, true)), EquipmentType.T_ARMOR_BA_STEALTH, -1, true, c));
		parts.add(new BaArmor(0, (int)Math.round(5 * BaArmor.getPointsPerTon(EquipmentType.T_ARMOR_BA_FIRE_RESIST, true)), EquipmentType.T_ARMOR_BA_FIRE_RESIST, -1, true, c));
		parts.add(new BaArmor(0, (int)Math.round(5 * BaArmor.getPointsPerTon(EquipmentType.T_ARMOR_BA_MIMETIC, false)), EquipmentType.T_ARMOR_BA_MIMETIC, -1, false, c));
	}

	private void stockMekLocations(Campaign c) {
		for(int loc = Mech.LOC_HEAD; loc <= Mech.LOC_LLEG; loc++) {
			for(int ton = 20; ton <= 100; ton=ton+5) {
				for(int type = 0; type < EquipmentType.structureNames.length; type++) {
					addMekLocation(c, loc, ton, type, false);
					// The only structure that differs between IS and Clan versions is Endo-Steel
					if (EquipmentType.T_STRUCTURE_ENDO_STEEL == type) {
	                    addMekLocation(c, loc, ton, type, true);
					}
				}
			}
		}
	}

    private void addMekLocation(Campaign c, int loc, int ton, int type, boolean clan) {
        if(loc == Mech.LOC_HEAD) {
            //for(int ctype = Mech.COCKPIT_STANDARD; ctype < Mech.COCKPIT_STRING.length; ctype++) {
                parts.add(new MekLocation(loc, ton, type, clan, false, false, true, true, c));
                parts.add(new MekLocation(loc, ton, type, clan, true, false, true, true, c));
                parts.add(new MekLocation(loc, ton, type, clan, false, false, false, false, c));
                parts.add(new MekLocation(loc, ton, type, clan, true, false, false, false, c));
            //}
        } else {
            parts.add(new MekLocation(loc, ton, type, clan, false, false, false, false, c));
        	parts.add(new MekLocation(loc, ton, type, clan, true, false, false, false, c));
        	if(loc > Mech.LOC_LT) {
        		parts.add(new MekLocation(loc, ton, type, clan, false, true, false, false, c));
        		parts.add(new MekLocation(loc, ton, type, clan, true, true, false, false, c));
        	}
        }
    }

	private void stockProtomekLocations(Campaign c) {
	    for(int loc = Protomech.LOC_HEAD; loc <= Protomech.LOC_MAINGUN; loc++) {
            for(int ton = 2; ton <= 15; ton++) {
                parts.add(new ProtomekLocation(loc, ton, EquipmentType.T_STRUCTURE_UNKNOWN, false, false, c));
                parts.add(new ProtomekLocation(loc, ton, EquipmentType.T_STRUCTURE_UNKNOWN, true, false, c));
                if(loc == Protomech.LOC_LEG) {
                    parts.add(new ProtomekLocation(loc, ton, EquipmentType.T_STRUCTURE_UNKNOWN, false, true, c));
                    parts.add(new ProtomekLocation(loc, ton, EquipmentType.T_STRUCTURE_UNKNOWN, true, true, c));
                }
            }
	    }
	}

	private void stockProtomekComponents(Campaign c) {
	    int ton = 2;
	    while(ton <= 15) {
	        parts.add(new ProtomekArmActuator(ton, c));
	        parts.add(new ProtomekLegActuator(ton, c));
	        parts.add(new ProtomekSensor(ton, c));
	        parts.add(new ProtomekJumpJet(ton, c));
	        ton++;
	    }
	}

}