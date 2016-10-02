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
	
	private AtBConfiguration() {
		hiringHalls = new ArrayList<DatedRecord<String>>();
		dsTable = new WeightedTable<>();
		jsTable = new WeightedTable<>();
		setDefaults();
	}
	
	/**
	 * Provide default values in case the file is missing or contains errors. Defaults
	 * are overridden as each section of the config file is processed, so if a section
	 * is removed the default values remain in place.
	 */
	private void setDefaults() {
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		hiringHalls.add(new DatedRecord<String>(null, null, "Galatea"));
		hiringHalls.add(new DatedRecord<String>(null, null, "Solaris"));
		try {
			hiringHalls.add(new DatedRecord<String>(df.parse("3031-01-01"),
					df.parse("3067-10-15"), "Outreach"));
		} catch (ParseException e) {
			MekHQ.logError("Error in date format in AtBConfiguration.setDefaults()");
		}
		
		dropshipSearchTarget = 10;
		jumpshipSearchTarget = 12;
		warshipSearchTarget = null;
		
		dsTable.add(1, "Buccaneer (Standard)");
		dsTable.add(6, "Mule (Standard)");
		dsTable.add(1, "Seeker (2815)");
		dsTable.add(4, "Gazelle (2531)");
		dsTable.add(1, "Excalibur (2786)");
		dsTable.add(2, "Leopard (2537)");
		dsTable.add(4, "Union (2708)");
		dsTable.add(1, "Overlord (2762)");

		jsTable.add(1, "Scout JumpShip (Standard)");
		jsTable.add(2, "Merchant Jumpship (Standard)");
		jsTable.add(3, "Invader Jumpship (Standard)");			
	}
	
	public boolean isHiringHall(String planet, Date date) {
		for (DatedRecord<String> rec : hiringHalls) {
			if (rec.getValue().equals(planet) && rec.fitsDate(date)) {
				return true;
			}
		}
		return false;
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
			MekHQ.logError("File data/universe/atbconfig.xml not found.");
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
			case "contractGeneration":
				retVal.loadCampaignGenerationNodeFromXml(wn);
				break;
			case "shipSearch":
				retVal.loadShipSearchNodeFromXml(wn);
				break;
			}
		}
		
		return retVal;
	}
	
	private void loadCampaignGenerationNodeFromXml(Node node) {
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
			if (wn.getNodeName().equals("entry")
					&& wn.getAttributes().getNamedItem("weight") != null) {
				retVal.add(Integer.parseInt(wn.getAttributes().getNamedItem("weight").getTextContent()),
						fromString.apply(wn.getTextContent()));
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
	class DatedRecord<E> {
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
	
	static class WeightedTable<T> {
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
			int total = weights.stream().mapToInt(w -> w.intValue()).sum();
			if (total > 0) {
				int roll = Compute.randomInt(total);
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
