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

import java.io.FileInputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import megamek.common.event.Subscribe;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.OptionsChangedEvent;

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
	
	private static final String FACTION_HINTS_FILE = "data/universe/factionhints.xml"; //$NON-NLS-1$
	/* When checking for potential enemies, count the planets controlled
	 * by potentially hostile factions within a certain number of jumps of
	 * friendly worlds; the number is based on the region of space.
	 */
	private static final int BORDER_RANGE_IS = 60;
	private static final int BORDER_RANGE_CLAN = 90;
	private static final int BORDER_RANGE_NEAR_PERIPHERY = 90;
	private static final int BORDER_RANGE_DEEP_PERIPHERY = 210; //a bit more than this distance between HL and NC
	
    private static final Date FORTRESS_REPUBLIC = new Date (new GregorianCalendar(3135,10,1).getTimeInMillis());
    
	private static RandomFactionGenerator rfg = null;
	
	private Set<String> deepPeriphery;
	private Set<String> neutralFactions;
	private Set<String> majorPowers;
	
	private Map<String, Map<String, List<FactionHint>>> wars;
	private Map<String, Map<String, List<FactionHint>>> alliances;
	private Map<String, Map<String, List<FactionHint>>> rivals;
	private Map<String, Map<String, List<FactionHint>>> neutralExceptions;
	private Map<String, Map<String, List<AltLocation>>> containedFactions;
	
	private FactionBorderTracker borderTracker;
	
	private boolean initialized = false;
	
	private RandomFactionGenerator() {
		deepPeriphery = new HashSet<>();
		neutralFactions = new HashSet<>();
		majorPowers = new HashSet<>();
		wars = new HashMap<>();
		alliances = new HashMap<>();
		rivals = new HashMap<>();
		neutralExceptions = new HashMap<>();
		containedFactions = new HashMap<>();
		
		borderTracker = new FactionBorderTracker();
		borderTracker.setDayThreshold(30);
		borderTracker.setDistanceThreshold(100);
		borderTracker.setDefaultBorderSize(BORDER_RANGE_IS, BORDER_RANGE_NEAR_PERIPHERY, BORDER_RANGE_CLAN);
		
		if (!initialized) {
	        try {
	            loadFactionHints();
	        } catch (DOMException e) {
	            MekHQ.getLogger().log(getClass(), "initialize()", e); //$NON-NLS-1$
	        } catch (ParseException e) {
	            MekHQ.getLogger().log(getClass(), "initialize()", e); //$NON-NLS-1$
	        }

	        initialized = true;
		}
	}
	
	public static RandomFactionGenerator getInstance() {
		if (rfg == null) {
			rfg = new RandomFactionGenerator();
		}
		return rfg;
	}
	
	public void startup(Campaign c) {
	    borderTracker.setDate(Utilities.getDateTimeDay(c.getCalendar()));
	    final Planet location = c.getLocation().getCurrentPlanet();
	    borderTracker.setRegionCenter(location.getX(), location.getY());
	    borderTracker.setRegionRadius(c.getCampaignOptions().getSearchRadius());
	    MekHQ.registerHandler(borderTracker);
	    MekHQ.registerHandler(this);
	}
	
	@Subscribe
	public void handleCampaignOptionsChanged(OptionsChangedEvent event) {
	    borderTracker.setRegionRadius(event.getOptions().getSearchRadius());
	}
	
	public void dispose() {
		if (initialized){
			clear();
		}
		MekHQ.unregisterHandler(borderTracker);
		MekHQ.unregisterHandler(this);
	}

	public void clear() {
		rfg = null;
		deepPeriphery.clear();
		neutralFactions.clear();
		majorPowers.clear();		
		wars.clear();
		alliances.clear();
		rivals.clear();
		neutralExceptions.clear();
		containedFactions.clear();
		
		initialized = false;
	}	

	
	private void loadFactionHints() throws DOMException, ParseException {
	    final String METHOD_NAME = "loadFactionHints()"; //$NON-NLS-1$
	    
	    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO,
	            "Starting load of faction hint data from XML..."); //$NON-NLS-1$
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document xmlDoc = null;
		
		try {
			FileInputStream fis = new FileInputStream(FACTION_HINTS_FILE); //$NON-NLS-1$
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
							borderTracker.setBorderSize(Faction.getFaction(f), BORDER_RANGE_DEEP_PERIPHERY);
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
							containedFactions.put(outer, new HashMap<>());
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
	
	private void setFactionHint(Map<String, Map<String, List<FactionHint>>> hint,
			Node node) throws DOMException, ParseException {
	    final String METHOD_NAME = "setFactionHint(Map<String,Map<String,List<FactionHint>>>,Node"; //$NON-NLS-1$
	    
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
							hint.put(parties[i], new HashMap<>());
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
						neutralExceptions.put(faction, new HashMap<>());
					}
					if (neutralExceptions.get(faction).get(parties[i]) == null) {
						neutralExceptions.get(faction).put(parties[i], new ArrayList<>());
					}
					neutralExceptions.get(faction).get(parties[i]).add(new FactionHint(faction, localStart, localEnd));
				}
			}
		}	
	}
	
	private Date currentDate() {
	    return borderTracker.getLastUpdated().toDate();
	}
	
	/**
	 * @return A set of faction keys for all factions that have a presence within the search area.
	 */
	public Set<String> getCurrentFactions() {
        Set<String> retVal = new TreeSet<>();
        for (Faction f : borderTracker.getFactionsInRegion()) {

            if (isEmptyFaction(f)
                    || f.getShortName().equals("CLAN")) {
                continue;
            }
            if (f.getShortName().equals("ROS") && currentDate().after(FORTRESS_REPUBLIC)) {
                 continue;
            }

            retVal.add(f.getShortName());
            /* Add factions which do not control any planets to the employer list */
            for (String cf : getContainedFactions(f.getShortName(), currentDate())) {
                Faction cfaction = Faction.getFaction(cf);
                if (null != cfaction) {
                    retVal.add(cf);
                }
            }
        }
        //Add rebels and pirates
        retVal.add("REB");
        retVal.add("PIR");
		return retVal;
	}
	
	/**
	 * Builds map of potential employers weighted by number of systems controled within the search area
	 * 
	 * @return Map used to select employer
	 */
	protected WeightedMap<Faction> buildEmployerMap() {
	    WeightedMap<Faction> retVal = new WeightedMap<>();
	    for (Faction f : borderTracker.getFactionsInRegion()) {

	        if (f.isClan() || isEmptyFaction(f)) {
	            continue;
	        }
	        if (f.getShortName().equals("ROS") && currentDate().after(FORTRESS_REPUBLIC)) {
	            continue;
	        }

	        int weight = borderTracker.getBorders(f).getPlanets().size();
	        retVal.add(weight, f);

	        /* Add factions which do not control any planets to the employer list */
	        for (String cf : getContainedFactions(f.getShortName(), currentDate())) {
	            Faction cfaction = Faction.getFaction(cf);
	            if (null != cfaction) {
	                if (!cfaction.isClan()) {
	                    weight = (int) Math.floor(borderTracker.getBorders(f).getPlanets().size()
	                            * getAltLocationFraction(f.getShortName(), cf, currentDate()) + 0.5);
	                    retVal.add(weight, f);
	                }
	            }
	        }
	    }
	    return retVal;
	}
	
	/**
	 * Selects a faction from those with a presence in the region weighted by number of systems controlled.
	 * Excludes Clan factions and non-faction place holders (unknown, abandoned, none).
	 * 
	 * @return A faction to use as the employer for a contract.
	 */
	public String getEmployer() {
	    WeightedMap<Faction> employers = buildEmployerMap();
	    Faction f = employers.randomItem();
	    if (null != f) {
	        return f.getShortName();
	    }
	    return null;
	}
	
	/**
	 * Selects an enemy faction for the given employer, weighted by length of shared border and
	 * diplomatic relations. Factions at war are twice or designated as rivals as likely (cumulative)
	 * to be chosen as opponents. Allied factions are ignored except for Clans, which cuts halves
	 * the weight for that option. 
	 * 
	 * @param employer  The faction offering the contract
	 * @param useRebels Whether to include rebels as a possible opponent
	 * @return          The faction to use as the opfor.
	 */
	public String getEnemy(String employer, boolean useRebels) {
	    final String METHOD_NAME = "getEnemy(String,boolean)"; //$NON-NLS-1$
	    
		/* Rebels occur on a 1-4 (d20) on nearly every enemy chart */
		if (useRebels && Compute.randomInt(5) == 0) {
			return "REB";
		}
		
		Faction enemy = null;
		Faction employerFaction = Faction.getFaction(employer);
		if (null != employerFaction) {
		    WeightedMap<Faction> enemyMap = buildEnemyMap(employerFaction);
		    enemy = enemyMap.randomItem();
		}
		if (null != enemy) {
		    return enemy.getShortName();
		}
		// Fallback; there are always pirates.
        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                "Could not find enemy for " + employer); //$NON-NLS-1$
        return "PIR";
	}
	
	/**
	 * Builds a map of potential enemies keyed to cumulative weight
	 * 
	 * @param employer
	 * @return
	 */
	protected WeightedMap<Faction> buildEnemyMap(Faction employer) {
	    WeightedMap<Faction> enemyMap = new WeightedMap<>();
	    for (Faction enemy : borderTracker.getFactionsInRegion()) {
	        if (isEmptyFaction(enemy)
	                || enemy.getShortName().equals("CLAN")) {
	            continue;
	        }
	        final String eName = enemy.getShortName();
	        int totalCount = borderTracker.getBorderPlanets(employer, enemy).size();
	        double count = totalCount;
	        // Split the border between main controlling faction and any contained factions.
            for (String cfName : getContainedFactions(employer.getShortName(), currentDate())) {
                final Faction cFaction = Faction.getFaction(cfName);
                if ((null == cFaction)
                        || !isContainedFactionOpponent(eName, cfName, employer.getShortName(), currentDate())) {
                    continue;
                }
                if ((neutralFactions.contains(cfName) &&
                        !hintApplies(neutralExceptions, cfName, eName, currentDate())) ||
                        neutralFactions.contains(eName) &&
                        !hintApplies(neutralExceptions, eName, cfName, currentDate())) {
                    continue;
                }
                double cfCount = totalCount;
                if (getAltLocationFraction(eName, cfName, currentDate()) > 0.0) {
                    cfCount = totalCount * getAltLocationFraction(eName, cfName, currentDate());
                    count -= cfCount;
                }
                cfCount = adjustBorderWeight(cfCount, employer, enemy, currentDate());
                enemyMap.add((int) Math.floor(cfCount + 0.5), Faction.getFaction(cfName));
            }
            count = adjustBorderWeight(count, employer, enemy, currentDate());
            enemyMap.add((int) Math.floor(count + 0.5), enemy);
	    }
	    return enemyMap;
	}
	
	/**
	 * @return A set of keys for all current factions in the space that are potential employers.
	 */
	public Set<String> getEmployerSet() {
	    return borderTracker.getFactionsInRegion().stream()
	            .filter(f -> !f.isClan())
	            .map(Faction::getShortName)
	            .collect(Collectors.toSet());
	}	
	
	/**
	 * Constructs a list of a faction's potential enemies based on common borders.
	 * @param employerName The shortName of the employer faction
	 * @return             A list of faction that share a border
	 */
	public List<String> getEnemyList(String employerName) {
	    Faction employer = Faction.getFaction(employerName);
	    if (null == employer) {
	        MekHQ.getLogger().log(getClass(), "getEnemyList(String)", LogLevel.WARNING, //$NON-NLS-1$
	                "Unknown faction key: " + employerName); //$NON-NLS-1$
	        return Collections.emptyList();
	    }
		Set<Faction> list = new HashSet<>();
		for (Faction enemy : borderTracker.getFactionsInRegion()) {
		    if (isEmptyFaction(enemy)) {
		        continue;
		    }
            if (enemy.equals(employer) && !isAtWarWith(enemy.getShortName(),
                    enemy.getShortName(), currentDate())) {
                continue;
            }
            if (!borderTracker.getBorderPlanets(employer, enemy).isEmpty()) {
                list.add(enemy);
                for (String cf : getContainedFactions(enemy.getShortName(), currentDate())) {
                    final Faction cFaction = Faction.getFaction(cf);
                    if ((null != cFaction)
                            && isContainedFactionOpponent(enemy.getShortName(), cf, employerName, currentDate())) {
                        list.add(cFaction);
                    }
                }
            }
		}
		return list.stream().map(Faction::getShortName).collect(Collectors.toList());
	}
	
	/**
	 * Applies modifiers to the border size (measured by number of planets within a certain proximity
	 * to one or more of the attacker's planets) based on diplomatic stance (e.g. war, rivalry, alliance).
	 * 
	 * @param count  The number of planets
	 * @param f      The attacking faction
	 * @param enemy  The defending faction
	 * @param date   The current campaign date
	 * @return       An adjusted weight
	 */
	private double adjustBorderWeight(double count, Faction f,
			Faction enemy, Date date) {
		final Date TUKKAYID = new Date (new GregorianCalendar(3052,5,20).getTimeInMillis());
		
		if ((neutralFactions.contains(f.getShortName())
		        || neutralFactions.contains(enemy.getShortName()))
		        && !isAtWarWith(f, enemy, date)) {
		    return 0;
		}
        if (!f.isClan() && hintApplies(alliances, f, enemy, date)) {
            return 0;
        }
		if (f.isClan() && enemy.isClan() &&
				(hintApplies(alliances, f, enemy, date) ||
						(date.before(TUKKAYID) && (borderTracker.getCenterY() < 600)))) {
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
		if (f.getShortName().equals("CS") && enemy.isClan()) {
			count /= 12.0;
		}
		return count;
	}
	
	/**
	 * Selects a random planet from a list of potential targets based on the attacking and defending factions.
	 * 
	 * @param attacker
	 * @param defender
	 * @return          The planetId of the chosen planet, or null if there are no target candidates
	 */
	@Nullable public String getMissionTarget(String attacker, String defender) {
	    final String METHOD_NAME = "getMissionTarget(String, String)"; // $NON-NLS-1$
	    Faction f1 = Faction.getFaction(attacker);
	    Faction f2 = Faction.getFaction(defender);
        if (null == f1) {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                    "Non-existent faction key: " + attacker); // $NON-NLS-1$
            return null;
        }
        if (null == f2) {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                    "Non-existent faction key: " + attacker); // $NON-NLS-1$
            return null;
        }
		List<Planet> planetList = getMissionTargetList(f1, f2);
		if (planetList.size() > 0) {
			return Utilities.getRandomItem(planetList).getId();
		}
		return null;
	}
	
	/**
	 * Builds a list of planets controlled by the defender that are near one or more of the attacker's
	 * planets.
	 * 
	 * @param attackerKey   The attacking faction's shortName
	 * @param defenderKey   The defending faction's shortName
	 * @return              A list of potential mission targets
	 */
	public List<Planet> getMissionTargetList(String attackerKey, String defenderKey) {
	    final String METHOD_NAME = "getMissionTargetList(String, String)"; //$NON-NLS-1$
	    Faction attacker = Faction.getFaction(attackerKey);
	    Faction defender = Faction.getFaction(defenderKey);
        if (null == attacker) {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                    "Non-existent faction key: " + attackerKey); //$NON-NLS-1$
        }
        if (null == defender) {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                    "Non-existent faction key: " + defenderKey); //$NON-NLS-1$
        }
        if ((null != attacker) && (null != defender)) {
            return getMissionTargetList(attacker, defender);
        } else {
            return Collections.emptyList();
        }
	}
	
    /**
     * Builds a list of planets controlled by the defender that are near one or more of the attacker's
     * planets.
     * 
     * @param attackerKey   The attacking faction
     * @param defenderKey   The defending faction
     * @return              A list of potential mission targets
     */
	public List<Planet> getMissionTargetList(Faction attacker, Faction defender) {
	    // Locate rebels on any of the attacker's planet
        if (defender.getShortName().equals("REB")) {
            return new ArrayList<>(borderTracker.getBorders(attacker).getPlanets());
        }

        Set<Planet> planetSet = new HashSet<>(borderTracker.getBorderPlanets(attacker, defender));
        // Locate missions by or against pirates on border worlds
	    if (attacker.getShortName().equals("PIR") || defender.getShortName().equals("PIR")) {
	        for (Faction f : borderTracker.getFactionsInRegion()) {
                planetSet.addAll(borderTracker.getBorderPlanets(f, attacker));
                planetSet.addAll(borderTracker.getBorderPlanets(attacker, f));
	        }
	    }
        /* No border with defender found among systems controlled by
         * attacker; check for presence of attacker and defender
         * in systems controlled by other factions.
         */
	    if (planetSet.isEmpty()) {
            for (Faction f : borderTracker.getFactionsInRegion()) {
                for (String cf : getContainedFactions(f.getShortName(), currentDate())) {
                    if (cf.equals(attacker.getShortName())
                            && isContainedFactionOpponent(f.getShortName(),
                                    cf, defender.getShortName(), currentDate())) {
                        planetSet.addAll(borderTracker.getBorderPlanets(f, defender));
                    }
                    if (cf.equals(defender.getShortName())
                            && isContainedFactionOpponent(f.getShortName(),
                                    cf, attacker.getShortName(), currentDate())) {
                        planetSet.addAll(borderTracker.getBorderPlanets(attacker, f));
                    }
                }
            }
		}
		return new ArrayList<>(planetSet);
	}
	
	/**
	 * Accounts for non-existent factions that are used to indicate special status of the planet
	 * (undiscovered, abandoned).
	 * 
	 * @param f
	 * @return  Whether the faction is not a true faction
	 */
	public boolean isEmptyFaction(Faction f) {
        return (f.getShortName().equals("ABN")
                || f.getShortName().equals("UND")
                || f.getShortName().equals("NONE"));
	}
	
	/**
	 * @param f 
	 * @return  Whether the faction is considered a major Inner Sphere
	 *          power for purposes of contract generation
	 */
	public boolean isISMajorPower(Faction f) {
		return majorPowers.contains(f.getShortName());
	}
	
    /**
     * @param f 
     * @return  Whether the faction is considered a major Inner Sphere
     *          power for purposes of contract generation
     */
	public boolean isISMajorPower(String fName) {
		return majorPowers.contains(fName);
	}
	
	public boolean isAtWarWith(Faction f1, Faction f2, Date date) {
	    return isAtWarWith(f1.getShortName(), f2.getShortName(), date);
	}
	
	/**
	 * 
	 * @param f1    A faction
	 * @param f2    Another faction
	 * @param date  The campaign date
	 * @return      true if the factions are at war at the indicated date. If the two factions are
	 *              the same, indicates civil war or some other infighting (such as Clan Fire Mandrill
	 *              factionalism)
	 */
	public boolean isAtWarWith(String f1, String f2, Date date) {
		return hintApplies(wars, f1, f2, date);
	}
	
	/**
	 * 
	 * @param f1    A faction
	 * @param f2    Another faction
	 * @param date  The current campaign date
	 * @return      The name of the current war the two factions are involved in, or {@code null} if they
	 *              are not currently at war.
	 */
	@Nullable public String getCurrentWar(String f1, String f2, Date date) {
		if (wars.get(f1) != null && wars.get(f1).get(f2) != null) {
			for (FactionHint fh : wars.get(f1).get(f2)) {
				if (date.after(fh.start) && date.before(fh.end)) {
					return fh.name;
				}
			}
		}
		return null;
	}
	
	/**
	 * Indicates a faction is neutral (e.g. Comstar) or non-combatant and should not be chosen as an
	 * employer or enemy unless at war at the time.
	 * 
	 * @param faction  Any faction
	 * @return         Whether the faction is considered neutral
	 */
	public boolean isNeutral(String faction) {
		return neutralFactions.contains(faction);
	}
	
	private boolean hintApplies(Map<String, Map<String, List<FactionHint>>> hints,
	        Faction f1, Faction f2, Date date) {
	    return hintApplies(hints, f1.getShortName(), f2.getShortName(), date);
	}
	
	private boolean hintApplies(Map<String, Map<String, List<FactionHint>>> hints,
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
	
	/**
	 * Determines whether a faction that is contained within another can consider a third faction to
	 * be an opponent. A contained faction is one that does not have any planets assigned to it but
	 * occupies space in another faction's space, such as the exiled Clan Wolf or the abjured Clan
	 * Nova Cat. Normally these are treated the same way as the containing faction, but in some cases
	 * the inner faction may have a reduced set of opponents, such as the Second Star League force
	 * in the Draconis Combine during Operation Bulldog, which should only be considered opponents of
	 * Clan Smoke Jaguar and not the DC neighbors.
	 * 
	 * @param outer     The faction that controls the planets in the region.
	 * @param inner     The faction that occupies planets within the outer faction's space.
	 * @param opponent  A potential opponent of the inner faction
	 * @param date      The campaign date
	 * @return          Whether {@code opponent} can be treated as an enemy of {@code inner}.
	 */
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
						if (f.equals(opponent)) {
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

	/**
	 * Constructs a table of values each with a weight that makes them more or less likely to be
	 * selected at random
	 * 
	 * @param <T> The values in the table
	 */
	static class WeightedMap<T> extends TreeMap<Integer, T> {
	    
        private static final long serialVersionUID = -568712793616821291L;

        void add(int weight, T item) {
	        if (weight > 0) {
	            if (!isEmpty()) {
	                put(lastKey() + weight, item);
	            } else {
	                put(weight, item);
	            }
	        }
	    }
	    
	    T randomItem() {
	        if (isEmpty()) {
	            return null;
	        }
	        int random = Compute.randomInt(lastKey()) + 1;
	        return ceilingEntry(random).getValue();
	    }
	}
}
