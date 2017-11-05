/*
 * RandomFactionGenerator.java
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

package mekhq.campaign.universe;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Compute;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.CampaignOptions;

/**
 * @author Neoancient
 * 
 * Uses Factions and Planets to weighted lists of potential employers
 * and enemies for contract generation. Also finds a suitable planet
 * for the action.
 *
 */

/* TODO: Redesign the system for tracking factions that do not control any planets
 * of their own to make it easier to follow.
 * TODO: Account for the de facto alliance of the invading Clans and the
 * Fortress Republic in a way that doesn't involve hard-coding them here.
 */
public class RandomFactionGenerator implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7346225681238948390L;
	
	/* When checking for potential enemies, count the planets controlled
	 * by potentially hostile factions within a certain number of jumps of
	 * friendly worlds; the number is based on the region of space.
	 */
	private static final int BORDER_RANGE_IS = 60;
	private static final int BORDER_RANGE_CLAN = 90;
	private static final int BORDER_RANGE_NEAR_PERIPHERY = 90;
	private static final int BORDER_RANGE_DEEP_PERIPHERY = 210; //a bit more than this distance between HL and NC

	private static RandomFactionGenerator rfg = null;
	
	private TreeSet<String> currentFactions;
	/* all factions that control at least one system at the current date
	 */
	
	private ArrayList<String> employers;
	/* list of potential employers weighted by number of systems controlled
	 */
	private HashMap<String, HashMap<String, HashSet<Planet>>> borders;
	/*List of systems controlled by faction key1 that are within one
	 * jump of faction key2
	 */
	
	private HashSet<String> deepPeriphery;
	private HashSet<String> neutralFactions;
	private HashSet<String> majorPowers;
	
	private HashMap<String, HashMap<String, ArrayList<FactionHint>>> wars;
	private HashMap<String, HashMap<String, ArrayList<FactionHint>>> alliances;
	private HashMap<String, HashMap<String, ArrayList<FactionHint>>> rivals;
	private HashMap<String, HashMap<String, ArrayList<FactionHint>>> neutralExceptions;
	private HashMap<String, HashMap<String, ArrayList<AltLocation>>> containedFactions;
	
	private ArrayList<ActionListener> listeners;
	private Thread loader;
	private static boolean initialized = false;
	private static boolean initializing = false;
	
	/* Track time and location; only update if one of them has changed */
	private Date lastUpdate = null;
	private Planet lastLocation = null;
	
	private RandomFactionGenerator() {
		currentFactions = new TreeSet<String>();
		employers = new ArrayList<String>();
		deepPeriphery = new HashSet<String>();
		neutralFactions = new HashSet<String>();
		majorPowers = new HashSet<String>();
		borders = new HashMap<String,HashMap<String,HashSet<Planet>>>();
		wars = new HashMap<String, HashMap<String, ArrayList<FactionHint>>>();
		alliances = new HashMap<String, HashMap<String, ArrayList<FactionHint>>>();
		rivals = new HashMap<String, HashMap<String, ArrayList<FactionHint>>>();
		neutralExceptions = new HashMap<String, HashMap<String, ArrayList<FactionHint>>>();
		containedFactions = new HashMap<String, HashMap<String, ArrayList<AltLocation>>>();
		listeners = new ArrayList<ActionListener>();
		
		lastUpdate = new Date(0); //initialize 1 Jan 1970
		lastLocation = Planets.getInstance().getPlanets().get("Terra");
	}
	
	public static RandomFactionGenerator getInstance() {
		if (rfg == null) {
			rfg = new RandomFactionGenerator();
		}
		
		if (!initialized && !initializing) {
			initializing = true;
			rfg.loader = new Thread(new Runnable() {
				public void run() {
					Planets p = Planets.getInstance();
					while (!p.isInitialized()) {
						try {
							Thread.sleep(50);
						} catch (InterruptedException ignore) {
						}
					}
					rfg.initialize();
				}
			}, "FactionGeography loader");
            rfg.loader.setPriority(Thread.NORM_PRIORITY - 1);
            rfg.loader.start();
		}
		return rfg;
	}
	
	public boolean isInitialized() {
		return initialized;
	}
	
	private void initialize() {
		try {
			loadFactionHints();
		} catch (DOMException e) {
		    MekHQ.getLogger().log(getClass(), "initialize()", e); //$NON-NLS-1$
		} catch (ParseException e) {
            MekHQ.getLogger().log(getClass(), "initialize()", e); //$NON-NLS-1$
		}

		initialized = true;
		initializing = false;
		for (ActionListener l : listeners) {
			l.actionPerformed(new ActionEvent(this, 0, "FactionGeography initialized"));
		}
	}
	
	public void dispose() {
		if (initialized){
			clear();
		}
	}

	public void clear() {
		rfg = null;
		currentFactions.clear();
		employers.clear();
		borders.clear();
		deepPeriphery.clear();
		neutralFactions.clear();
		majorPowers.clear();		
		wars.clear();
		alliances.clear();
		rivals.clear();
		neutralExceptions.clear();
		containedFactions.clear();
		
		initialized = false;
		initializing = false;
	}	

	
	private void loadFactionHints() throws DOMException, ParseException {
	    final String METHOD_NAME = "loadFactionHints()"; //$NON-NLS-1$
	    
	    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO,
	            "Starting load of faction hint data from XML..."); //$NON-NLS-1$
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document xmlDoc = null;
		
		try {
			FileInputStream fis = new FileInputStream("data/universe/factionhints.xml"); //$NON-NLS-1$
			DocumentBuilder db = dbf.newDocumentBuilder();
	
			xmlDoc = db.parse(fis);
		} catch (Exception ex) {
	        MekHQ.getLogger().log(getClass(), METHOD_NAME, ex);
		}
		
		Element rootElement = xmlDoc.getDocumentElement();
		NodeList nl = rootElement.getChildNodes();
		rootElement.normalize();
		
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		for (int i = 0; i < nl.getLength(); i++) {
			final Date epoch_start = df.parse("0001-01-01");
			final Date epoch_end = df.parse("9999-12-31");
			Node wn = nl.item(i);
			
			if (wn.getParentNode() != rootElement)
				continue;
			
			if (wn.getNodeType() == Node.ELEMENT_NODE) {
				String nodeName = wn.getNodeName();
				
				if (nodeName.equalsIgnoreCase("neutral")) {
					String f = wn.getAttributes().getNamedItem("faction").getTextContent().trim();
					if (Faction.getFaction(f) != null) {
						neutralFactions.add(f);
						addNeutralExceptions(f, wn);
					} else {
				        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
				                "Invalid faction code in factionhints.xml: " + f); //$NON-NLS-1$
					}
				} else if (nodeName.equalsIgnoreCase("deepPeriphery")) {
					for (String f : wn.getTextContent().trim().split(",")) {
						if (Faction.getFaction(f) != null) {
							deepPeriphery.add(f);
						} else {
	                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
	                                "Invalid faction code in factionhints.xml: " + f); //$NON-NLS-1$
						}
					}
				} else if (nodeName.equalsIgnoreCase("majorPowers")) {
					for (String f : wn.getTextContent().trim().split(",")) {
						if (Faction.getFaction(f) != null) {
							majorPowers.add(f);
						} else {
	                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
	                                "Invalid faction code in factionhints.xml: " + f); //$NON-NLS-1$
						}
					}
				} else if (nodeName.equalsIgnoreCase("rivals")) {
					setFactionHint(rivals, wn);
				} else if (nodeName.equalsIgnoreCase("war")) {
					setFactionHint(wars, wn);
				} else if (nodeName.equalsIgnoreCase("alliance")) {
					setFactionHint(alliances, wn);
				} else if (nodeName.equalsIgnoreCase("location")) {
					Date start = epoch_start;
					Date end = epoch_end;
					double fraction = 0.0;
					String outer = "UND";
					String inner = "UND";
					ArrayList<String> opponents = null;
					if (wn.getAttributes().getNamedItem("start") != null) {
						start = df.parse(wn.getAttributes().getNamedItem("start").getTextContent().trim());
					}
					if (wn.getAttributes().getNamedItem("end") != null) {
						end = df.parse(wn.getAttributes().getNamedItem("end").getTextContent().trim());
					}
					for (int j = 0; j < wn.getChildNodes().getLength(); j++) {
						Node wn2 = wn.getChildNodes().item(j);
						if (wn2.getNodeName().equalsIgnoreCase("outer")) {
							outer = wn2.getTextContent().trim();
						} else if (wn2.getNodeName().equalsIgnoreCase("inner")) {
							inner = wn2.getTextContent().trim();
						} else if (wn2.getNodeName().equalsIgnoreCase("fraction")) {
							fraction = Double.parseDouble(wn2.getTextContent().trim());
						} else if (wn2.getNodeName().equalsIgnoreCase("opponents")) {
							opponents = new ArrayList<String>();
							for (String faction : wn2.getTextContent().trim().split(",")) {
								if (Faction.getFaction(faction) != null) {
									opponents.add(faction);
								}
							}
						}
					}
					if (Faction.getFaction(outer) != null && Faction.getFaction(inner) != null) {
						if (containedFactions.get(outer) == null) {
							containedFactions.put(outer, new HashMap<String, ArrayList<AltLocation>>());
						}
						if (containedFactions.get(outer).get(inner) == null) {
							containedFactions.get(outer).put(inner, new ArrayList<AltLocation>());
						}
						containedFactions.get(outer).get(inner).add(new AltLocation(start, end, fraction, opponents));
					} else {
                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                                "Invalid faction code in factionhints.xml: " + outer + "/" + inner); //$NON-NLS-1$
					}
				}
			}
		}
	}
	
	private void setFactionHint(HashMap<String, HashMap<String, ArrayList<FactionHint>>> hint,
			Node node) throws DOMException, ParseException {
	    final String METHOD_NAME = "setFactionHint(HashMap<String,HashMap<String,ArrayList<FactionHint>>>,Node"; //$NON-NLS-1$
	    
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		final Date epoch_start = df.parse("0001-01-01");
		final Date epoch_end = df.parse("9999-12-31");

		String name = null;
		Date start = epoch_start;
		Date end = epoch_end;
		if (node.getAttributes().getNamedItem("name") != null) {
			name = node.getAttributes().getNamedItem("name").getTextContent().trim();
		}
		if (node.getAttributes().getNamedItem("start") != null) {
			start = df.parse(node.getAttributes().getNamedItem("start").getTextContent().trim());
		}
		if (node.getAttributes().getNamedItem("end") != null) {
			end = df.parse(node.getAttributes().getNamedItem("end").getTextContent().trim());
		}
		for (int n = 0; n < node.getChildNodes().getLength(); n++) {
			Node wn = node.getChildNodes().item(n);
			if (wn.getNodeName().equalsIgnoreCase("parties")) {
				Date localStart = start;
				Date localEnd = end;
				if (wn.getAttributes().getNamedItem("start") != null) {
					localStart = df.parse(wn.getAttributes().getNamedItem("start").getTextContent().trim());
				}
				if (wn.getAttributes().getNamedItem("end") != null) {
					localEnd = df.parse(wn.getAttributes().getNamedItem("end").getTextContent().trim());
				}
				
				String[] parties = wn.getTextContent().trim().split(",");
				
				for (int i = 0; i < parties.length - 1; i++) {
					if (Faction.getFaction(parties[i]) == null) {
                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                                "Invalid faction code in factionhints.xml: " + parties[i]); //$NON-NLS-1$
						continue;
					}
					for (int j = i + 1; j < parties.length; j++) {
						if (Faction.getFaction(parties[j]) == null) {
	                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
	                                "Invalid faction code in factionhints.xml: " + parties[j]); //$NON-NLS-1$
							continue;
						}
						if (hint.get(parties[i]) == null) {
							hint.put(parties[i], new HashMap<String, ArrayList<FactionHint>>());
						}
						if (hint.get(parties[i]).get(parties[j]) == null) {
							hint.get(parties[i]).put(parties[j], new ArrayList<FactionHint>());
						}
						hint.get(parties[i]).get(parties[j]).add(new FactionHint(name, localStart, localEnd));
					}
				}
			}
		}	
	}
	
	private void addNeutralExceptions(String faction, Node node) throws DOMException, ParseException {
	    final String METHOD_NAME = "addNeutralExceptions(String,Node)"; //$NON-NLS-1$
	    
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		final Date epoch_start = df.parse("0001-01-01");
		final Date epoch_end = df.parse("9999-12-31");

		Date start = epoch_start;
		Date end = epoch_end;
		if (node.getAttributes().getNamedItem("end") != null) {
			end = df.parse(node.getAttributes().getNamedItem("end").getTextContent().trim());
		}
		for (int n = 0; n < node.getChildNodes().getLength(); n++) {
			Node wn = node.getChildNodes().item(n);
			if (wn.getNodeName().equalsIgnoreCase("exceptions")) {
				Date localStart = start;
				Date localEnd = end;
				if (wn.getAttributes().getNamedItem("start") != null) {
					localStart = df.parse(wn.getAttributes().getNamedItem("start").getTextContent().trim());
				}
				if (wn.getAttributes().getNamedItem("end") != null) {
					localEnd = df.parse(wn.getAttributes().getNamedItem("end").getTextContent().trim());
				}
				
				String[] parties = wn.getTextContent().trim().split(",");
				
				for (int i = 0; i < parties.length; i++) {
					if (Faction.getFaction(parties[i]) == null) {
                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                                "Invalid faction code in factionhints.xml: " + parties[i]); //$NON-NLS-1$
						continue;
					}
					if (neutralExceptions.get(faction) == null) {
						neutralExceptions.put(faction, new HashMap<String, ArrayList<FactionHint>>());
					}
					if (neutralExceptions.get(faction).get(parties[i]) == null) {
						neutralExceptions.get(faction).put(parties[i], new ArrayList<FactionHint>());
					}
					neutralExceptions.get(faction).get(parties[i]).add(new FactionHint(faction, localStart, localEnd));
				}
			}
		}	
	}
	
	public void registerListener(ActionListener l) {
		listeners.add(l);
	}
	
	public void removeListener(ActionListener l) {
		listeners.remove(l);
	}
	
	public void updateTables(Date date, Planet currentLocation,
			CampaignOptions options) {
        final Date FORTRESS_REPUBLIC = new Date (new GregorianCalendar(3135,10,1).getTimeInMillis());
        if (!date.after(lastUpdate) && currentLocation == lastLocation) {
			return;
		}
		lastUpdate.setTime(date.getTime());
		lastLocation = currentLocation;
		employers.clear();
		borders.clear();
		currentFactions.clear();
		for (Planet p : Planets.getInstance().getPlanets().values()) {
			for (Faction f : p.getFactionSet(Utilities.getDateTimeDay(date))) {
				String fName = f.getShortName();
				if (fName.equals("ABN") ||
						fName.equals("UND") ||
						fName.equals("CLAN") ||
						fName.equals("NONE")) {
					continue;
				}
				if (fName.equals("ROS") && date.after(FORTRESS_REPUBLIC)) {
					 continue;
				}
				currentFactions.add(fName);
				if (p.getDistanceTo(currentLocation) <= options.getSearchRadius()) {
					if (!f.isClan()) {
						employers.add(fName);
					}
					updateBorders(f, p);
				}
			}
		}
		/* Add factions which do not control any planets to the employer list */
		for (String emp : getEmployerSet()) {
			int count = 0;
			for(String cf : getContainedFactions(emp, date)) {
				currentFactions.add(cf);
				if (Faction.getFaction(cf).isClan()) {
					continue;
				}
				if (count == 0) {
					for (String s : employers) {
						if (s.equals(emp)) {
							count++;
						}
					}
				}
				for (int i = 0; i < count * getAltLocationFraction(emp, cf, date) + 0.5; i++) {
					employers.add(cf);
				}
			}
		}
		
		//Add rebels and pirates
		currentFactions.add("REB");
		currentFactions.add("PIR");
	}
	
	private void updateBorders(Faction f, Planet p) {
		int distance = BORDER_RANGE_IS;
		if (f.isClan()) {
			distance = BORDER_RANGE_CLAN;
		} else if (deepPeriphery.contains(f.getShortName())) {
			distance = BORDER_RANGE_DEEP_PERIPHERY;			
		} else if (f.isPeriphery()) {
			distance = BORDER_RANGE_NEAR_PERIPHERY;
		}
		for (Planet planetKey : Planets.getInstance().getNearbyPlanets(p, distance)) {
			for (Faction f2 : planetKey.getFactionSet(Utilities.getDateTimeDay(lastUpdate))) {
				String eName = f2.getShortName();
				if (eName.equals("ABN") ||
						eName.equals("UND") ||
						eName.equals("CLAN") ||
						eName.equals("NONE")) {
					continue;
				}
				addBorderEnemy(f.getShortName(), p, eName);
				/* Go through all factions contained within f and add this
				 * planet to their border with this enemy as well */
				for(String cf : getContainedFactions(f.getShortName(), lastUpdate)) {
					if (isContainedFactionOpponent(f.getShortName(), cf, eName, lastUpdate)) {
						addBorderEnemy(cf, p, eName);
					}
				}
			}
		}
		/* If a faction is located within a faction with which it may be at war,
		 * add this planet to both borders. */
		for (String cf : getContainedFactions(f.getShortName(), lastUpdate)) {
			if (isContainedFactionOpponent(f.getShortName(), cf, f.getShortName(), lastUpdate)) {
				addBorderEnemy(cf, p, f.getShortName());
				addBorderEnemy(f.getShortName(), p, cf);
			}
		}
	}

	private void addBorderEnemy(String fName, Planet p, String eName) {
		final Date FORTRESS_REPUBLIC = new Date (new GregorianCalendar(3135,10,1).getTimeInMillis());

		if (borders.get(fName) == null) {
			borders.put(fName, new HashMap<>());
		}
        borders.get(fName).putIfAbsent("REB", new HashSet<>());
        borders.get(fName).putIfAbsent("PIR", new HashSet<>());
		if (fName.equals(eName)) {
		    borders.get(fName).get("REB").add(p);
			if (hintApplies(wars, fName, fName, lastUpdate)) {
				if (borders.get(fName).get(eName) == null) {
					borders.get(fName).put(eName, new HashSet<Planet>());
				}
				borders.get(fName).get(eName).add(p);
			}
		} else {
			/* Locate pirate activity primarily along borders */
			borders.get(fName).get("PIR").add(p);
			if (borders.get("PIR") == null) {
				borders.put("PIR", new HashMap<String, HashSet<Planet>>());
			}
			if (borders.get("PIR").get(fName) == null) {
				borders.get("PIR").put(fName, new HashSet<Planet>());
			}
			borders.get("PIR").get(fName).add(p);
			
			/* Ignore allies (unless both are Clan) */
			if (hintApplies(alliances, fName, eName, lastUpdate) &&
					!(Faction.getFaction(fName).isClan() &&
							Faction.getFaction(eName).isClan())) {
				return;
			}
			if (neutralFactions.contains(fName) &&
					!hintApplies(neutralExceptions, fName, eName, lastUpdate)) {
				return;
			}
			if (neutralFactions.contains(eName) &&
					!hintApplies(neutralExceptions, eName, fName, lastUpdate)) {
				return;
			}
			if ((fName.equals("ROS") || eName.equals("ROS")) &&
					lastUpdate.after(FORTRESS_REPUBLIC)) {
				return;
			}

			/* Otherwise add planet to the border */
			if (borders.get(fName).get(eName) == null) {
				borders.get(fName).put(eName, new HashSet<Planet>());
			}
			borders.get(fName).get(eName).add(p);
		}
	}
	
	public TreeSet<String> getCurrentFactions() {
		return currentFactions;
	}
	
	public String getEmployer() {
		return Utilities.getRandomItem(employers);
	}
	
	public String getEnemy(String fName, boolean useRebels) {
	    final String METHOD_NAME = "getEnemy(String,boolean)"; //$NON-NLS-1$
	    
		/* Rebels occur on a 1-4 (d20) on nearly every enemy chart */
		if (useRebels && Compute.randomInt(5) == 0) {
			return "REB";
		}
		if (borders.get(fName) != null) {
			ArrayList<String> enemiesList = new ArrayList<String>();
			for (String eName : borders.get(fName).keySet()) {
				if (eName.equals("PIR") ||
						eName.equals("REB")) {
					continue;
					/*Pirate and rebel opponent frequency is not
					 * based on borders.
					 */
				}
				double totalCount = borders.get(fName).get(eName).size();
				double count = totalCount;
				/* Divide border between the main faction and any factions
				 * that might be contained within it. */
				for (String cf : getContainedFactions(eName, lastUpdate)) {
					if (!isContainedFactionOpponent(eName, cf, fName, lastUpdate)) {
						continue;
					}
					if ((neutralFactions.contains(cf) &&
							!hintApplies(neutralExceptions, cf, eName, lastUpdate)) ||
							neutralFactions.contains(eName) &&
							!hintApplies(neutralExceptions, eName, cf, lastUpdate)) {
						continue;
					}
					double cfCount = totalCount;
					if (getAltLocationFraction(eName, cf, lastUpdate) > 0.0) {
						cfCount = totalCount * getAltLocationFraction(eName, cf, lastUpdate);
						count -= cfCount;
					}
					cfCount = adjustBorderWeight(cfCount, fName, eName, lastUpdate);
					for (int i = 0; i < (cfCount + 0.5); i++) {
						enemiesList.add(cf);
					}
				}
				count = adjustBorderWeight(count, fName, eName, lastUpdate);
				for (int i = 0; i < (count + 0.5); i++) {
					enemiesList.add(eName);
				}
			}
			if (enemiesList.size() > 0) {
				return Utilities.getRandomItem(enemiesList);
			}
		}
		MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
		        "Could not find enemy for " + fName); //$NON-NLS-1$
		return "PIR";
	}
	
	public HashSet<String> getEmployerSet() {
		HashSet<String> retval = new HashSet<String>();
		for (String fName : employers) {
			retval.add(fName);
		}
		return retval;
	}	
	
	public ArrayList<String> getEnemyList(String employer) {
		ArrayList<String> list = new ArrayList<String>();
		if (borders.get(employer) != null) {
			for (String enemy : borders.get(employer).keySet()) {
				list.add(enemy);
				for (String cf : getContainedFactions(enemy, lastUpdate)) {
					if (isContainedFactionOpponent(enemy, cf, employer, lastUpdate)) {
						list.add(cf);
					}
				}
			}
		}
		return list;
	}
	
	private double adjustBorderWeight(double count, String f,
			String enemy, Date date) {
		final Date tukayyid = new Date (new GregorianCalendar(3052,5,20).getTimeInMillis());
		if (Faction.getFaction(f).isClan() && Faction.getFaction(enemy).isClan() &&
				(hintApplies(alliances, f, enemy, date) ||
						(date.before(tukayyid) &&
								borders.get(f).get(enemy) != null &&
								borders.get(f).get(enemy).iterator().next().getY() < 600))) {
			/* Treat invading Clans as allies in the Inner Sphere */
			count /= 4.0;
		}
		if (hintApplies(wars, f, enemy, date) && f != enemy) {
			count *= 2.0;
		}
		if (hintApplies(rivals, f, enemy, date)) {
			count *= 2.0;
		}
		/* This is pretty hacky, but ComStar does not have many targets
		 * and tends to fight the Clans too much between Tukayyid and
		 * the Jihad.
		 */
		if (f.equals("CS") && Faction.getFaction(enemy).isClan()) {
			count /= 12.0;
		}
		return count;
	}
	
	public String getMissionTarget(String attacker, String defender, Date date, Planet currentPlanet, int searchRadius) {
		ArrayList<Planet> planetList = getMissionTargetList(attacker, defender, date, currentPlanet, searchRadius);
		if (planetList.size() > 0) {
			return Utilities.getRandomItem(planetList).getId();
		}
		return null;
	}
	
	public ArrayList<Planet> getMissionTargetList(String attacker, String defender, Date date, Planet currentPlanet, int searchRadius) {
		boolean filterByDistance = (currentPlanet != null);
		ArrayList<Planet> planetList = new ArrayList<Planet>();
		int maxJumps = 3;
		if (deepPeriphery.contains(attacker) || deepPeriphery.contains(defender)) {
			maxJumps = 8;
		}
		HashSet<Planet> border = null;		
		if (borders.get(attacker) != null && borders.get(attacker).get(defender) != null) {
			border = borders.get(attacker).get(defender);
		} else {
			/* No border with defender found among systems controlled by
			 * attacker; check for presence of attacker and defender
			 * in systems controlled by other factions.
			 */
			for (String fName : Faction.getFactionList()) {
				for (String cf : getContainedFactions(fName, date)) {
					if (cf.equals(attacker) && isContainedFactionOpponent(fName, cf, defender, date)) {
						if (borders.get(fName) != null && borders.get(fName).get(defender) != null) {
							border = borders.get(fName).get(defender);
							attacker = fName;
							break;
						}
					}
					if (cf.equals(defender) && isContainedFactionOpponent(fName, cf, attacker, date)) {
						if (borders.get(attacker) != null && borders.get(attacker).get(fName) != null) {
							border = borders.get(attacker).get(fName);
							defender = fName;
							break;
						}
					}
				}
			}
		}
		if (border != null) {
			for (Planet startingPlanet : border) {
				for (Planet planetKey : Planets.getInstance().getNearbyPlanets(startingPlanet, maxJumps * 30)) {
					for (Faction f : planetKey.getFactionSet(Utilities.getDateTimeDay(date))) {
						if (f.getShortName().equals(defender) ||
								defender.equals("PIR") ||
								(f.getShortName().equals(attacker) && defender.equals("REB")) &&
								(filterByDistance && currentPlanet.getDistanceTo(planetKey) <= searchRadius)) {
							planetList.add(planetKey);
						}
					}
				}
			}
		}
		if (planetList.size() == 0 && borders.get(defender) != null &&
				borders.get(defender).get(attacker) != null) {
			/* Most likely a deep periphery defender against a non-deep
			 * attacker; use one of the border planets */
			for (Planet p : borders.get(defender).get(attacker)) {
				planetList.add(p);
			}
		}
		return planetList;
	}
	
	public boolean isMajorPower(Faction f) {
		return majorPowers.contains(f.getShortName());
	}
	
	public boolean isISMajorPower(String fName) {
		return majorPowers.contains(fName);
	}
	
	public boolean isAtWarWith(String f1, String f2, Date date) {
		return hintApplies(wars, f1, f2, date);
	}
	
	public String getCurrentWar(String f1, String f2, Date date) {
		if (wars.get(f1) != null && wars.get(f1).get(f2) != null) {
			for (FactionHint fh : wars.get(f1).get(f2)) {
				if (date.after(fh.start) && date.before(fh.end)) {
					return fh.name;
				}
			}
		}
		return null;
	}
	
	public boolean isNeutral(String faction) {
		return neutralFactions.contains(faction);
	}
	
	private boolean hintApplies(HashMap<String, HashMap<String, ArrayList<FactionHint>>> hints,
				String f1, String f2, Date date) {
		if (hints.get(f1) != null && hints.get(f1).get(f2) != null) {
			for (FactionHint fh : hints.get(f1).get(f2)) {
				if (date.after(fh.start) && date.before(fh.end)) {
					return true;
				}
			}
		}
		if (hints.get(f2) != null && hints.get(f2).get(f1) != null) {
			for (FactionHint fh : hints.get(f2).get(f1)) {
				if (date.after(fh.start) && date.before(fh.end)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private HashSet<String> getContainedFactions(String f, Date date) {
		HashSet<String> retval = new HashSet<String>();
		if (containedFactions.get(f) != null) {
			for (String f2 : containedFactions.get(f).keySet()) {
				for (AltLocation l : containedFactions.get(f).get(f2)) {
					if (date.after(l.start) && date.before(l.end)) {
						retval.add(f2);
					}
				}
			}
		}
		return retval;
	}
	
	private double getAltLocationFraction(String f1, String f2, Date date) {
		if (containedFactions.get(f1) != null && containedFactions.get(f1).get(f2) != null) {
			for (AltLocation l : containedFactions.get(f1).get(f2)) {
				if (date.after(l.start) && date.before(l.end)) {
					return l.fraction;
				}
			}
		}
		return 0.0;
	}
	
	public boolean isContainedFactionOpponent(Faction f1, Faction f2,
			Faction opponent, Date date) {
		return isContainedFactionOpponent(f1.getShortName(),
				f2.getShortName(), opponent.getShortName(), date);
	}
	
	public boolean isContainedFactionOpponent(String outer, String inner,
				String opponent, Date date) {
		if (containedFactions.get(outer) != null && containedFactions.get(outer).get(inner) != null) {
			for (AltLocation l : containedFactions.get(outer).get(inner)) {
				if (date.after(l.start) && date.before(l.end)) {
					if (l.opponents == null) {
						return !inner.equals(opponent) ||
								hintApplies(wars, inner, inner, date);
					}
					for (String f : l.opponents) {
						if (f == opponent) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	class FactionHint {
		/**
		 * Each participant in a war or an alliance has one instance
		 * of this class for each of the other factions involved.
		 */
		public String name;
		public Date start;
		public Date end;
		
		public FactionHint (String n, Date s, Date e) {
			name = n;
			start = (Date)s.clone();
			end = (Date)e.clone();
		}
	}

	class AltLocation extends FactionHint {
		public double fraction;
		public ArrayList<String> opponents;
			
		public AltLocation (Date s, Date e, double f, ArrayList<String> opponents) {
			super(null, s, e);
			fraction = f;
			this.opponents = opponents;
		}		
	}
}
