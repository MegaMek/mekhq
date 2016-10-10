/*
 * AtBPreferences.java
 *
 * Copyright (c) 2014 Carl Spain. All rights reserved.
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Compute;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.TargetRoll;
import megamek.common.UnitType;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.rating.IUnitRating;

/**
 * @author Neoancient
 * 
 * Class that handles configuration options for Against the Bot campaigns
 * more extensive than what is handled by CampaignOptions. Most of the options
 * fall into one of two categories: they allow users to customize the various
 * tables in the rules, or they avoid hard-coding universe details.
 *
 */
public class AtBConfiguration implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 515628415152924457L;
	
	/* Used to indicate size of lance or equivalent in opfor forces */
	public static final String ORG_IS = "IS";
	public static final String ORG_CLAN = "CLAN";
	public static final String ORG_CS = "CS";
	
	/* Scenario generation */
	private HashMap<String,ArrayList<WeightedTable<String>>> botForceTables = new HashMap<>();
	private HashMap<String,ArrayList<WeightedTable<String>>> botLanceTables = new HashMap<>();
	
	/* Contract generation */
	private ArrayList<DatedRecord<String>> hiringHalls;
	
	/* Personnel and unit markets */
	private int shipSearchCost = 100000;
	private int shipSearchLengthWeeks = 4;
	private Integer dropshipSearchTarget;
	private Integer jumpshipSearchTarget;
	private Integer warshipSearchTarget;
	private WeightedTable<String> dsTable;
	private WeightedTable<String> jsTable;
	
	private ResourceBundle defaultProperties;
	
	private AtBConfiguration() {
		hiringHalls = new ArrayList<DatedRecord<String>>();
		dsTable = new WeightedTable<>();
		jsTable = new WeightedTable<>();
		defaultProperties = ResourceBundle.getBundle("mekhq.resources.AtBConfigDefaults");
	}
	
	/**
	 * Provide default values in case the file is missing or contains errors.
	 */
	
	private WeightedTable<String> getDefaultForceTable(String key, int index) {
	    if(index < 0) {
            MekHQ.logError("Default force tables don't support negative weights, limiting to 0"); //$NON-NLS-1$
	        index = 0;
	    }
		String property = defaultProperties.getString(key);
		String[] fields = property.split("\\|"); //$NON-NLS-1$
		if(index >= fields.length) {
		    // Deal with too short field lengths
		    MekHQ.logError(String.format("Default force tables have %d weight entries; limiting the original value of %d.", fields.length, index)); //$NON-NLS-1$
		    index = fields.length - 1;
		}
		return parseDefaultWeightedTable(fields[index]);
	}
	
	private WeightedTable<String> parseDefaultWeightedTable(String entry) {
		return parseDefaultWeightedTable(entry, s -> s);
	}
	
	private <T>WeightedTable<T> parseDefaultWeightedTable(String entry, Function<String,T> fromString) {
		WeightedTable<T> retVal = new WeightedTable<>();
		String[] entries = entry.split(",");
		for (String e : entries) {
			String[] fields = e.split(":");
			retVal.add(Integer.parseInt(fields[0]), fromString.apply(fields[1]));
		}
		return retVal;
	}
	
	/**
	 * Used if the config file is missing.
	 */
	private void setAllValuesToDefaults() {
		for (Enumeration<String> e = defaultProperties.getKeys(); e.hasMoreElements(); ) {
			String key = e.nextElement();
			String property = defaultProperties.getString(key);
			switch (key) {
			case "botForce.IS":
			case "botForce.CLAN":
			case "botForce.CS":
				ArrayList<WeightedTable<String>> list = new ArrayList<>();
				for (String entry : property.split("\\|")) {
					list.add(parseDefaultWeightedTable(entry));
				}
				botForceTables.put(key.replace("botForce.", ""), list);
				break;
			case "botLance.IS":
			case "botLance.CLAN":
			case "botLance.CS":
				list = new ArrayList<>();
				for (String entry : property.split("\\|")) {
					list.add(parseDefaultWeightedTable(entry));
				}
				botLanceTables.put(key.replace("botLance.", ""), list);
				break;
			case "hiringHalls":
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
				for (String entry : property.split("\\|")) {
					String[] fields = entry.split(",");
					try {
						hiringHalls.add(new DatedRecord<>(fields[0].length() > 0? df.parse(fields[0]) : null,
								fields[1].length() > 0? df.parse(fields[1]) : null,
										fields[2]));
					} catch (ParseException ex) {
						MekHQ.logError("Error parsing default date for hiring hall on " + fields[2]);
						MekHQ.logError(ex);
					}
				}
				break;
			case "shipSearchCost":
				shipSearchCost = Integer.parseInt(property);
				break;
			case "shipSearchLengthWeeks":
				shipSearchLengthWeeks = Integer.parseInt(property);
				break;
			case "shipSearchTarget.Dropship":
				dropshipSearchTarget = property.matches("\\d+")? Integer.valueOf(property) : null;
				break;
			case "shipSearchTarget.Jumpship":
				jumpshipSearchTarget = property.matches("\\d+")? Integer.valueOf(property) : null;
				break;
			case "shipSearchTarget.Warship":
				warshipSearchTarget = property.matches("\\d+")? Integer.valueOf(property) : null;
				break;
			case "ships.Dropship":
				dsTable = parseDefaultWeightedTable(property);
				break;
			case "ships.Jumpship":
				jsTable = parseDefaultWeightedTable(property);
				break;
			}
		}
	}
	
	public int weightClassIndex(int entityWeightClass) {
		return entityWeightClass - 1;
	}
	
	public int weightClassIndex(String wc) {
		switch (wc) {
		case "L":
		case "UL":
			return 0;
		case "M":
			return 1;
		case "H":
			return 2;
		case "A":
		case "C":
		case "SH":
			return 3;
		}
		throw new IllegalArgumentException("Could not parse weight class " + wc);
	}
	
	public String selectBotLances(String org, int weightClass) {
		return selectBotLances(org, weightClass, 0f);
	}
	
	public String selectBotLances(String org, int weightClass, float rollMod) {
		if (botForceTables.containsKey(org)) {
		    final List<WeightedTable<String>> botForceTable = botForceTables.get(org);
		    int weightClassIndex = weightClassIndex(weightClass);
		    WeightedTable<String> table = null;
		    if((weightClassIndex < 0) || (weightClassIndex >= botForceTable.size())) {
	            MekHQ.logError(
	                String.format("Bot force tables for organization \"%s\" don't have an entry for weight class %d, limiting to valid values", org, weightClass)); //$NON-NLS-1$
	            weightClassIndex = Math.max(0, Math.min(weightClassIndex, botForceTable.size() - 1));
		    }
	        table = botForceTable.get(weightClassIndex);
			if (null == table) {
				table = getDefaultForceTable("botForce." + org, weightClassIndex);
	            if (null == table) {
	                MekHQ.logError(
	                    String.format("Default (fallback) bot force table for organization \"%s\" and weight class %d doesn't exist, ignoring", org, weightClass)); //$NON-NLS-1$
	                return null;
	            }
			}
			return table.select(rollMod);
		} else {
		    MekHQ.logError(
		        String.format("Bot force tables for organization \"%s\" not found, ignoring", org)); //$NON-NLS-1$
		    return null;
		}
	}
	
	public String selectBotUnitWeights(String org, int weightClass) {
		return selectBotUnitWeights(org, weightClass, 0f);
	}
	
	public String selectBotUnitWeights(String org, int weightClass, float rollMod) {
		if (botLanceTables.containsKey(org)) {
			WeightedTable<String> table = botLanceTables.get(org).get(weightClassIndex(weightClass));
			if (table == null) {
				table = this.getDefaultForceTable("botLance." + org, weightClassIndex(weightClass));
			}
			return table.select(rollMod);
		}
		return null;
	}
	
	public boolean isHiringHall(String planet, Date date) {
		return hiringHalls.stream().anyMatch( rec -> rec.getValue().equals(planet)
				&& rec.fitsDate(date));
	}
	
	public int getShipSearchCost() {
		return shipSearchCost;
	}
	
	public int getShipSearchLengthWeeks() {
		return shipSearchLengthWeeks;
	}
	
	public int shipSearchCostPerWeek() {
		return shipSearchCost / shipSearchLengthWeeks;
	}
	
	public Integer getDropshipSearchTarget() {
		return dropshipSearchTarget;
	}
	
	public Integer getJumpshipSearchTarget() {
		return jumpshipSearchTarget;
	}
	
	public Integer getWarshipSearchTarget() {
		return warshipSearchTarget;
	}
	
	public Integer shipSearchTargetBase(int unitType) {
		switch (unitType) {
		case UnitType.DROPSHIP:
			return dropshipSearchTarget;
		case UnitType.JUMPSHIP:
			return jumpshipSearchTarget;
		case UnitType.WARSHIP:
			return warshipSearchTarget;
		}
		return null;
	}
	
    public TargetRoll shipSearchTargetRoll(int unitType, Campaign campaign) {
    	if (shipSearchTargetBase(unitType) == null) {
    		return new TargetRoll(TargetRoll.IMPOSSIBLE, "Base");
    	}
    	TargetRoll target = new TargetRoll(shipSearchTargetBase(unitType), "Base");
		Person adminLog = campaign.findBestInRole(Person.T_ADMIN_LOG, SkillType.S_ADMIN);
		int adminLogExp = (adminLog == null)?SkillType.EXP_ULTRA_GREEN:adminLog.getSkill(SkillType.S_ADMIN).getExperienceLevel();
    	for (Person p : campaign.getAdmins()) {
			if ((p.getPrimaryRole() == Person.T_ADMIN_LOG ||
					p.getSecondaryRole() == Person.T_ADMIN_LOG) &&
					p.getSkill(SkillType.S_ADMIN).getExperienceLevel() > adminLogExp) {
				adminLogExp = p.getSkill(SkillType.S_ADMIN).getExperienceLevel();
			}
    	}
    	target.addModifier(SkillType.EXP_REGULAR - adminLogExp, "Admin/Logistics");
    	target.addModifier(IUnitRating.DRAGOON_C - campaign.getUnitRatingMod(),
    			"Unit Rating");
    	return target;    	
    }
    
	public MechSummary findShip(int unitType) {
		WeightedTable<String> table = null;
		if (unitType == UnitType.JUMPSHIP) {
			table = jsTable;
		} else if (unitType == UnitType.DROPSHIP) {
			table = dsTable;
		}
		String shipName = table.select();
		if (shipName == null) {
			return null;
		}
		return MechSummaryCache.getInstance().getMech(shipName);
	}
	
	public static AtBConfiguration loadFromXml() {
		AtBConfiguration retVal = new AtBConfiguration();
		
		MekHQ.logMessage("Starting load of AtB configuration data from XML...");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document xmlDoc = null;
		
		try {
			FileInputStream fis = new FileInputStream("data/universe/atbconfig.xml");
			DocumentBuilder db = dbf.newDocumentBuilder();
	
			xmlDoc = db.parse(fis);
		} catch (FileNotFoundException ex) {
			MekHQ.logError("File data/universe/atbconfig.xml not found. Loading defaults.");
			retVal.setAllValuesToDefaults();
			return retVal;
		} catch (Exception ex) {
			MekHQ.logError(ex);
			return retVal;
		}
		
		Element rootElement = xmlDoc.getDocumentElement();
		NodeList nl = rootElement.getChildNodes();
		rootElement.normalize();
	
		for (int x = 0; x < nl.getLength(); x++) {
			Node wn = nl.item(x);
			switch (wn.getNodeName()) {
			case "scenarioGeneration":
				retVal.loadScenarioGenerationNodeFromXml(wn);
				break;
			case "contractGeneration":
				retVal.loadContractGenerationNodeFromXml(wn);
				break;
			case "shipSearch":
				retVal.loadShipSearchNodeFromXml(wn);
				break;
			}
		}
		
		return retVal;
	}
	
	private void loadScenarioGenerationNodeFromXml(Node node) {
		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node wn = nl.item(i);
			String[] orgs;
			ArrayList<WeightedTable<String>> list;
			switch (wn.getNodeName()) {
			case "botForce":
				if (wn.getAttributes().getNamedItem("org") == null) {
					orgs = new String[1];
					orgs[0] = ORG_IS;
				} else {
					orgs = wn.getAttributes().getNamedItem("org").getTextContent().split(",");
				}
				list = loadForceTableFromXml(wn);
				for (String org : orgs) {
					botForceTables.put(org, list);
				}
				break;
			case "botLance":
				if (wn.getAttributes().getNamedItem("org") == null) {
					orgs = new String[1];
					orgs[0] = ORG_IS;
				} else {
					orgs = wn.getAttributes().getNamedItem("org").getTextContent().split(",");
				}
				list = loadForceTableFromXml(wn);
				for (String org : orgs) {
					botLanceTables.put(org, list);
				}
				break;
			}
		}
	}
	
	private ArrayList<WeightedTable<String>> loadForceTableFromXml(Node node) {
		ArrayList<WeightedTable<String>> retVal = new ArrayList<>();
		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node wn = nl.item(i);
			if (wn.getNodeName().equals("weightedTable")) {
				try {
					int weightClass = weightClassIndex(wn.getAttributes()
							.getNamedItem("weightClass").getTextContent());
					while (retVal.size() <= weightClass) {
						retVal.add(null);
					}
					retVal.set(weightClass, loadWeightedTableFromXml(wn));
				} catch (Exception ex) {
					MekHQ.logError(ex);
					MekHQ.logError("Could not parse weight class attribute for enemy forces table");
				}							
			}
		}
		return retVal;
	}
	
	private void loadContractGenerationNodeFromXml(Node node) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		
		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node wn = nl.item(i);
			switch (wn.getNodeName()) {
			case "hiringHalls":
				hiringHalls.clear();
				for (int j = 0; j < wn.getChildNodes().getLength(); j++) {
					Node wn2 = wn.getChildNodes().item(j);
					switch (wn2.getNodeName()) {
					case "hall":
						Date start = null;
						Date end = null;
						try {
							if (wn2.getAttributes().getNamedItem("start") != null) {
								start = new Date(df.parse(wn2.getAttributes().getNamedItem("start").getTextContent()).getTime());
							}
							if (wn2.getAttributes().getNamedItem("end") != null) {
								end = new Date(df.parse(wn2.getAttributes().getNamedItem("end").getTextContent()).getTime());
							}
						} catch (ParseException ex) {
							MekHQ.logError("Error parsing date for hiring hall on " + wn2.getTextContent());
							MekHQ.logError(ex);
						}
						hiringHalls.add(new DatedRecord<String>(start, end, wn2.getTextContent()));
						break;
					}
				}
				break;
			}
		}
	}
	
	private void loadShipSearchNodeFromXml(Node node) {
		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node wn = nl.item(i);
			switch (wn.getNodeName()) {
			case "shipSearchCost":
				shipSearchCost = Integer.parseInt(wn.getTextContent());
				break;
			case "shipSearchLengthWeeks":
				shipSearchLengthWeeks = Integer.parseInt(wn.getTextContent());
				break;
			case "target":
				if (wn.getAttributes().getNamedItem("unitType") != null) {
					Integer target = Integer.valueOf(wn.getTextContent());
					switch (wn.getAttributes().getNamedItem("unitType").getTextContent()) {
					case "Dropship":
						dropshipSearchTarget = target;
						break;
					case "Jumpship":
						jumpshipSearchTarget = target;
						break;
					case "Warship":
						warshipSearchTarget = target;
						break;
					}
				}
				break;
			case "weightedTable":
				if (wn.getAttributes().getNamedItem("unitType") != null) {
					WeightedTable<String> map = loadWeightedTableFromXml(wn);
					switch (wn.getAttributes().getNamedItem("unitType").getTextContent()) {
					case "Dropship":
						dsTable = map;
						break;
					case "Jumpship":
						jsTable = map;
					}
				}
				break;
			}
		}
	}
	
	private WeightedTable<String> loadWeightedTableFromXml(Node node) {
		return loadWeightedTableFromXml(node, s -> s);
	}
	
	private <T>WeightedTable<T> loadWeightedTableFromXml(Node node, Function<String,T> fromString) {
		WeightedTable<T> retVal = new WeightedTable<>();
		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node wn = nl.item(i);
			if (wn.getNodeName().equals("entry")) {
				int weight = 1;
				if (wn.getAttributes().getNamedItem("weight") != null) {
					weight = Integer.parseInt(wn.getAttributes().getNamedItem("weight").getTextContent());
				}
				retVal.add(weight, fromString.apply(wn.getTextContent()));
			}
		}
		return retVal;
	}
	
	/*
	 * Attaches a start and end date to any object.
	 * Either the start or end date can be null, indicating that
	 * the value should apply to all dates from the beginning
	 * or to the end of the epoch, respectively.
	 */
	static class DatedRecord<E> {
		private Date start;
		private Date end;
		private E value;
		
		public DatedRecord() {
			start = null;
			end = null;
			value = null;
		}
		
		public DatedRecord(Date s, Date e, E v) {
			if (s != null) {
				start = new Date(s.getTime());
			}
			if (e != null) {
				end = new Date(e.getTime());
			}
			value = v;
		}
		
		public void setStart(Date s) {
			if (start == null) {
				start = new Date(s.getTime());
			} else {
				start.setTime(s.getTime());
			}
		}
		
		public Date getStart() {
			return start;
		}
	
		public void setEnd(Date e) {
			if (end == null) {
				end = new Date(e.getTime());
			} else {
				end.setTime(e.getTime());
			}
		}
		
		public Date getEnd() {
			return end;
		}
		
		public void setValue(E v) {
			value = v;
		}
		
		public E getValue() {
			return value;
		}
		
		/**
		 * 
		 * @param d
		 * @return true if d is between the start and end date, inclusive
		 */
		public boolean fitsDate(Date d) {
			return (start == null || !start.after(d))
					&& (end == null || !end.before(d));
		}
	}
	
	static class WeightedTable<T> implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1984759212668176620L;
		
		private ArrayList<Integer> weights = new ArrayList<>();
		private ArrayList<T> values = new ArrayList<>();
		
		public void add(Integer weight, T value) {
			weights.add(weight);
			values.add(value);
		}
		
		public T remove(T value) {
			int index = values.indexOf(value);
			if (index > 0) {
				weights.remove(index);
				return values.remove(index);
			}
			return null;
		}
		
		public T select() {
			return select(0f);
		}
		
		/**
		 * Select random entry proportionally to the weight values
		 * @param rollMod - a modifier to the die roll, expressed as a fraction of the total weight
		 * @return
		 */
		public T select(float rollMod) {
			int total = weights.stream().mapToInt(w -> w.intValue()).sum();
			if (total > 0) {
				int roll = Math.min(Compute.randomInt(total) + (int)(total * rollMod + 0.5f),
						total - 1);
				for (int i = 0; i < weights.size(); i++) {
					if (roll < weights.get(i)) {
						return values.get(i);
					}
					roll -= weights.get(i);
				}
			}
			return null;
		}
	}
}
