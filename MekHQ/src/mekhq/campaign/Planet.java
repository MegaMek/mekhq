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

import megamek.common.PlanetaryConditions;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * This is the start of a planet object that will keep lots of information about 
 * planets that can be displayed on the interstellar map. Planet objects will 
 * *NOT* be serialized. The arraylist of planets in campaign will be loaded
 * from scratch on start up.
 * 
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Planet {
	
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
	private int faction;
	private String name;
	
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
	
	public Planet() {
		this.x = 0;
		this.y = 0;
		this.faction = Faction.F_COMSTAR;
		this.name = "Terra";
		
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
	
	public int getFaction() {
		return faction;
	}
	
	public String getName() {
		return name;
	}
	
	/*
	public double getTimeToJumpPoint() {
		return jumpPoint;
	}
	*/
	
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
	
	public static Planet getPlanetFromXML(Node wn) {
		Planet retVal = new Planet();
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			if (wn2.getNodeName().equalsIgnoreCase("name")) {
				retVal.name = wn2.getTextContent();
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
			}
		}
		return retVal;
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