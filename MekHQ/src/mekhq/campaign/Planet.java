/*
 * Planet.java
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

import megamek.common.EquipmentType;
import megamek.common.PlanetaryConditions;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * This is the start of a planet object that will keep lots of information about 
 * planets that can be displayed on the interstellar map.
 * 
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Planet implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8699502165157515099L;
	
	private static final int SPECTRAL_O = 0;
	private static final int SPECTRAL_B = 1;
	private static final int SPECTRAL_A = 2;
	private static final int SPECTRAL_F = 3;
	private static final int SPECTRAL_G = 4;
	private static final int SPECTRAL_K = 5;
	private static final int SPECTRAL_M = 6;
	
	private static final String LUM_0 = "0";
	private static final String LUM_IA  = "Ia";
	private static final String LUM_IB  = "Ib";
	private static final String LUM_II  = "II";
	private static final String LUM_III = "III";
	private static final String LUM_IV  = "IV";
	private static final String LUM_V   = "V";
	private static final String LUM_VI  = "VI";
	private static final String LUM_VII = "VII";

	private static final int LIFE_NONE    = 0;
	private static final int LIFE_MICROBE = 1;
	private static final int LIFE_PLANT   = 2;
	private static final int LIFE_FISH    = 3;
	private static final int LIFE_AMPH    = 4;
	private static final int LIFE_REPTILE = 5;
	private static final int LIFE_BIRD    = 6;
	private static final int LIFE_MAMMAL  = 7;
	
	private static final int CLIMATE_ARCTIC    = 0;
	private static final int CLIMATE_BOREAL   = 1;
	private static final int CLIMATE_COOLTEM  = 2; 
	private static final int CLIMATE_WARMTEM  = 3;
	private static final int CLIMATE_ARID     = 4;
	private static final int CLIMATE_TROPICAL = 5;
	
	private double x; 
	private double y;
	
	/**
	 * This is the base faction which the program will fall back on if
	 * no better faction is found in the faction history given the date
	 */
	private int faction;
	private String name;
	private String shortName;
	
	//star type
	private int spectralClass;
	private int subtype;
	private String luminosity;
	
	private int pressure;
	private double gravity;
	private boolean nadirCharge;
	private boolean zenithCharge;
	
	//fluff
	private int lifeForm;
	private int climate;
	private int percentWater;
	private int temperature;
	private int hpg;
	private String desc;
	
	//socioindustrial levels
	private int tech;
	private int industry;
	private int rawMaterials;
	private int output;
	private int agriculture;
	
	//keep some string information in arraylists
	private ArrayList<String> satellites;
	private ArrayList<String> landMasses;
	
	//a hash to keep track of dynamic faction changes
	TreeMap<Date,Integer> factionHistory;
	
	public Planet() {
		this.x = 0;
		this.y = 0;
		this.faction = Faction.F_COMSTAR;
		this.name = "Terra";
		this.shortName = "Terra";
		
		this.spectralClass = SPECTRAL_G;
		this.subtype = 2;
		this.luminosity = LUM_V;
		
		this.pressure = PlanetaryConditions.ATMO_STANDARD;
		this.gravity = 1.0;
		this.nadirCharge = false;
		this.zenithCharge = false;
		
		this.lifeForm = LIFE_NONE;
		this.climate = CLIMATE_WARMTEM;
		this.percentWater = 70;
		this.temperature = 20;
		this.desc = "Nothing here yet. Who wants to volunteer to enter planet data?";
		
		this.tech = EquipmentType.RATING_C;
		this.industry = EquipmentType.RATING_C;
		this.rawMaterials = EquipmentType.RATING_C;
		this.output = EquipmentType.RATING_C;
		this.agriculture = EquipmentType.RATING_C;

		this.satellites = new ArrayList<String>();
		this.landMasses = new ArrayList<String>();
		
		this.hpg = EquipmentType.RATING_B;
		
		this.factionHistory = new TreeMap<Date,Integer>();
	}
	
	public static String getLifeFormName(int life) {
		switch(life) {
		case LIFE_NONE:
			return "None";
		case LIFE_MICROBE:
			return "Microbes";
		case LIFE_PLANT:
			return "Plants";
		case LIFE_FISH:
			return "Fish";
		case LIFE_AMPH:
			return "Amphibians";
		case LIFE_REPTILE:
			return "Reptiles";
		case LIFE_BIRD:
			return "Birds";
		case LIFE_MAMMAL:
			return "Mammals";
		default:
			return "Unknown";
		}
	}
	
	public static String getSpectralClassName(int spectral) {
		switch(spectral) {
		case SPECTRAL_O:
			return "O";
		case SPECTRAL_B:
			return "B";
		case SPECTRAL_A:
			return "A";
		case SPECTRAL_F:
			return "F";
		case SPECTRAL_G:
			return "G";
		case SPECTRAL_K:
			return "K";
		case SPECTRAL_M:
			return "M";
		default:
			return "?";
		}
	}
	
	public static String getClimateName(int cl) {
		switch(cl) {
		case CLIMATE_ARCTIC:
			return "Acrtic";
		case CLIMATE_BOREAL:
			return "Boreal";
		case CLIMATE_COOLTEM:
			return "Cool-Temperate";
		case CLIMATE_WARMTEM:
			return "Warm-Temperate";
		case CLIMATE_ARID:
			return "Arid";
		case CLIMATE_TROPICAL:
			return "Tropical";
		default:
			return "Unknown";
		}
	}
	
	public String getSocioIndustrialLevel() {
		return EquipmentType.getRatingName(tech) + "-" + EquipmentType.getRatingName(industry) + "-" + EquipmentType.getRatingName(rawMaterials) + "-" + EquipmentType.getRatingName(output) + "-" + EquipmentType.getRatingName(agriculture);
	}
	
	public String getHPGClass() {
		return EquipmentType.getRatingName(hpg);
	}
	
	public static int getSpectralClassFrom(String spectral) {
		if(spectral.trim().equalsIgnoreCase("B")) {
			return SPECTRAL_B;
		}
		else if(spectral.trim().equalsIgnoreCase("A")) {
			return SPECTRAL_A;
		}
		else if(spectral.trim().equalsIgnoreCase("F")) {
			return SPECTRAL_F;
		}
		else if(spectral.trim().equalsIgnoreCase("G")) {
			return SPECTRAL_G;
		}
		else if(spectral.trim().equalsIgnoreCase("M")) {
			return SPECTRAL_M;
		}
		else if(spectral.trim().equalsIgnoreCase("K")) {
			return SPECTRAL_K;
		}
		else {
			return SPECTRAL_O;
		}
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public int getBaseFaction() {
		return faction;
	}
	
	public int getCurrentFaction(Date date) {
		int currentFaction = getBaseFaction();
		for(Date event : factionHistory.keySet()) {
			if(event.after(date)) {
				return currentFaction;
			} else {
				currentFaction = factionHistory.get(event);
			}
		}
		return currentFaction;
	}
	
	public String getName() {
		return name;
	}
	
	public String getShortName() {
		return shortName;
	}
	
	/**
	 * Used when there are planets with duplicate names
	 * @param n
	 */
	public void resetName(String n) {
		this.shortName = name;
		this.name = n;
	}

	
	public double getGravity() {
		return gravity;
	}
	
	public String getPressureName() {
		return PlanetaryConditions.getAtmosphereDisplayableName(pressure);
	}
	
	public String getLifeFormName() {
		return getLifeFormName(lifeForm);
	}
	
	public String getClimateName() {
		return getClimateName(climate);
	}
	
	public int getPercentWater() {
		return percentWater;
	}
	
	public int getTemperature() {
		return temperature;
	}
	
	public String getStarType() {
		return getSpectralClassName(spectralClass) + subtype + luminosity;
	}
	
	public String getSatelliteDescription() {
		if(satellites.isEmpty()) {
			return "0";
		}
		String toReturn = satellites.size() + " (";
		for(int i = 0; i < satellites.size(); i++) {
			toReturn += satellites.get(i);
			if(i < (satellites.size() - 1)) {
				toReturn += ", ";	
			} else {
				toReturn += ")";
			}
		}
		return toReturn;
	}
	
	public String getLandMassDescription() {
		String toReturn = "";
		for(int i = 0; i < landMasses.size(); i++) {
			toReturn += landMasses.get(i);
			if(i < (landMasses.size() - 1)) {
				toReturn += ", ";
			} 
		}
		return toReturn;
	}
	
	public String getRechargeStations() {
		if(zenithCharge && nadirCharge) {
			return "Zenith, Nadir";
		} else if(zenithCharge) {
			return "Zenith";
		} else if(nadirCharge) {
			return "Nadir";
		} else {
			return "None";
		}
	}
	
	public int getRechargeTime() {
		return 141 + 10*spectralClass + subtype;
	}
	
	public double getTimeToJumpPoint(double acceleration) {
		//based on the formula in StratOps
		return Math.sqrt((getDistanceToJumpPoint()*1000)/(9.8*acceleration))/43200;
	}
	
	public float getDistanceToJumpPoint() {
		return getDistanceToJumpPoint(spectralClass, subtype);
	}
	
	/**
	 * Distance to jump point given a spectral class and subtype
	 * measured in kilometers
	 * @param spectral
	 * @param subtype
	 * @return
	 */
	public static float getDistanceToJumpPoint(int spectral, int subtype) {
		
		//taken from Dropships and Jumpships sourcebook, pg. 17
		switch(spectral) {
		case SPECTRAL_M:
			if(subtype == 0) {
				return 179915179f;
			} 
			else if(subtype == 1) {
				return 162301133f;
			}
			else if(subtype == 2) {
				return 146630374f;
			}
			else if(subtype == 3) {
				return 132668292f;
			}
			else if(subtype == 4) {
				return 120210786f;
			}
			else if(subtype == 5) {
				return 109080037f;
			}
			else if(subtype == 6) {
				return 99120895f;
			}
			else if(subtype == 7) {
				return 90197803f;
			}
			else if(subtype == 8) {
				return 82192147f;
			}
			else if(subtype > 8) {
				return 75000000f;
			}
		case SPECTRAL_K:
			if(subtype == 0) {
				return 549582283f;
			} 
			else if(subtype == 1) {
				return 487907078f;
			}
			else if(subtype == 2) {
				return 433886958f;
			}
			else if(subtype == 3) {
				return 386493164f;
			}
			else if(subtype == 4) {
				return 344844735f;
			}
			else if(subtype == 5) {
				return 308186014f;
			}
			else if(subtype == 6) {
				return 275867748f;
			}
			else if(subtype == 7) {
				return 247331200f;
			}
			else if(subtype == 8) {
				return 222094749f;
			}
			else if(subtype > 8) {
				return 199742590f;
			}
		case SPECTRAL_G:
			if(subtype == 0) {
				return 1993403717f;
			} 
			else if(subtype == 1) {
				return 1737789950f;
			}
			else if(subtype == 2) {
				return 1517879732f;
			}
			else if(subtype == 3) {
				return 1328325100f;
			}
			else if(subtype == 4) {
				return 1164628460f;
			}
			else if(subtype == 5) {
				return 1023000099f;
			}
			else if(subtype == 6) {
				return 900240718f;
			}
			else if(subtype == 7) {
				return 793644393f;
			}
			else if(subtype == 8) {
				return 700918272f;
			}
			else if(subtype > 8) {
				return 620115976f;
			}
		case SPECTRAL_F:
			if(subtype == 0) {
				return 8795520975f;
			} 
			else if(subtype == 1) {
				return 7509758447f;
			}
			else if(subtype == 2) {
				return 6426154651f;
			}
			else if(subtype == 3) {
				return 5510915132f;
			}
			else if(subtype == 4) {
				return 4736208289f;
			}
			else if(subtype == 5) {
				return 4079054583f;
			}
			else if(subtype == 6) {
				return 3520442982f;
			}
			else if(subtype == 7) {
				return 3044611112f;
			}
			else if(subtype == 8) {
				return 2638462416f;
			}
			else if(subtype > 8) {
				return 2291092549f;
			}
		case SPECTRAL_A:
			if(subtype == 0) {
				return 48590182199f;
			} 
			else if(subtype == 1) {
				return 40506291619f;
			}
			else if(subtype == 2) {
				return 33853487850f;
			}
			else if(subtype == 3) {
				return 28364525294f;
			}
			else if(subtype == 4) {
				return 23824470101f;
			}
			else if(subtype == 5) {
				return 20060019532f;
			}
			else if(subtype == 6) {
				return 16931086050f;
			}
			else if(subtype == 7) {
				return 14324152109f;
			}
			else if(subtype == 8) {
				return 12147004515f;
			}
			else if(subtype > 8) {
				return 10324556364f;
			}
		case SPECTRAL_B:
			if(subtype == 0) {
				return 347840509855f;
			} 
			else if(subtype == 1) {
				return 282065439915f;
			}
			else if(subtype == 2) {
				return 229404075188f;
			}
			else if(subtype == 3) {
				return 187117766777f;
			}
			else if(subtype == 4) {
				return 153063985045f;
			}
			else if(subtype == 5) {
				return 12556160986f;
			}
			else if(subtype == 6) {
				return 103287722257f;
			}
			else if(subtype == 7) {
				return 85198295036f;
			}
			else if(subtype == 8) {
				return 70467069133f;
			}
			else if(subtype > 8) {
				return 58438309136f;
			}
		default:
			return 0;
		}
		
		
	}
	
	public double getDistanceTo(Planet anotherPlanet) {
		return Math.sqrt(Math.pow(x - anotherPlanet.getX(), 2) + Math.pow(y - anotherPlanet.getY(), 2));
	}
	
	public String getDescription() {
		return desc;
	}
	
	public static int convertRatingToCode(String rating) {
		if(rating.equalsIgnoreCase("A")) {
			return EquipmentType.RATING_A;
		} 
		else if(rating.equalsIgnoreCase("B")) {
			return EquipmentType.RATING_B;
		}
		else if(rating.equalsIgnoreCase("C")) {
			return EquipmentType.RATING_C;
		}
		else if(rating.equalsIgnoreCase("D")) {
			return EquipmentType.RATING_D;
		}
		else if(rating.equalsIgnoreCase("E")) {
			return EquipmentType.RATING_E;
		}
		else if(rating.equalsIgnoreCase("F")) {
			return EquipmentType.RATING_F;
		}
		return EquipmentType.RATING_C;
	}
	
	public static Planet getPlanetFromXML(Node wn) throws DOMException, ParseException {
		Planet retVal = new Planet();
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			if (wn2.getNodeName().equalsIgnoreCase("name")) {
				retVal.name = wn2.getTextContent();
				retVal.shortName = retVal.name;
			} else if (wn2.getNodeName().equalsIgnoreCase("xcood")) {
				retVal.x = Double.parseDouble(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("ycood")) {
				retVal.y = Double.parseDouble(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("faction")) {
				retVal.faction = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("pressure")) {
				retVal.pressure = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("gravity")) {
				retVal.gravity = Double.parseDouble(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("nadirCharge")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.nadirCharge = true;
				else
					retVal.nadirCharge = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("zenithCharge")) {
				if (wn2.getTextContent().equalsIgnoreCase("true"))
					retVal.zenithCharge = true;
				else
					retVal.zenithCharge = false;
			} else if (wn2.getNodeName().equalsIgnoreCase("lifeForm")) {
				retVal.lifeForm = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("climate")) {
				retVal.climate = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("percentWater")) {
				retVal.percentWater = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("temperature")) {
				retVal.temperature = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("spectralClass")) {
				retVal.spectralClass = getSpectralClassFrom(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("subtype")) {
				retVal.subtype = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("luminosity")) {
				retVal.luminosity = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("factionChange")) {
				processFactionChange(retVal, wn2);
			} else if (wn2.getNodeName().equalsIgnoreCase("satellite")) {
				retVal.satellites.add(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("landMass")) {
				retVal.landMasses.add(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("hpg")) {
				retVal.hpg = convertRatingToCode(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("socioIndustrial")) {
				String[] socio = wn2.getTextContent().split("-");
				if(socio.length >= 5) {
					retVal.tech = convertRatingToCode(wn2.getTextContent().split("-")[0]);
					retVal.industry = convertRatingToCode(wn2.getTextContent().split("-")[1]);
					retVal.rawMaterials = convertRatingToCode(wn2.getTextContent().split("-")[2]);
					retVal.output = convertRatingToCode(wn2.getTextContent().split("-")[3]);
					retVal.agriculture = convertRatingToCode(wn2.getTextContent().split("-")[4]);
				}
			} else if (wn2.getNodeName().equalsIgnoreCase("desc")) {
				retVal.desc = wn2.getTextContent();
			}
		}
		return retVal;
	}
	
	private static void processFactionChange(Planet retVal, Node wni) throws DOMException, ParseException {
		NodeList nl = wni.getChildNodes();

		Date date = null;
		int faction = -1;
		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < nl.getLength(); x++) {
			Node wn = nl.item(x);
			int xc = wn.getNodeType();

			// If it's not an element, again, we're ignoring it.
			if (xc == Node.ELEMENT_NODE) {
				String xn = wn.getNodeName();

				if (xn.equalsIgnoreCase("date")) {
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
					date = df.parse(wn.getTextContent().trim());
				} else if (xn.equalsIgnoreCase("faction")) {
					faction = Integer.parseInt(wn.getTextContent().trim());
				}
			}
		}
		if(null != date && faction > -1) {
			retVal.factionHistory.put(date, faction);
		}
	}
	
	@Override
	public boolean equals(Object object) {
		if(object instanceof Planet) {
			Planet planet = (Planet)object;
			if(planet.getName().equalsIgnoreCase(name) 
					&& planet.getX() == x
					&& planet.getY() == y) {
				return true;
			}
		}
		return false;
	}
}