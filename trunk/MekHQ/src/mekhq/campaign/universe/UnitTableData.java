/**
 * 
 */
package mekhq.campaign.universe;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.client.RandomUnitGenerator;
import megamek.common.Compute;
import mekhq.MekHQ;

/**
 * Provides the correct RAT name for the given source, faction, year,
 * unit type, and equipment rating.
 * 
 * @author Neoancient
 *
 */
public class UnitTableData implements Serializable, ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2047012666986245214L;
	Map<String, Map<Integer, Map<String, FactionTables>>> ratTree;
	//example: ratTree.get("Xotl").get(3028).get("FWL")
	Map<String, ArrayList<String>> altTables;
	
	private static UnitTableData utd = null;
	private static boolean interrupted = false;
	private static boolean dispose = false;
	private Thread loader;
	private boolean initialized;
	private boolean initializing;

	private ArrayList<ActionListener> listeners;

	public static final int WT_LIGHT = 0;
	public static final int WT_MEDIUM = 1;
	public static final int WT_HEAVY = 2;
	public static final int WT_ASSAULT = 3;
	public static final String[] weightNames = {
		"Light", "Medium", "Heavy", "Assault"
	};

	public static final int UNIT_MECH = 0;
	public static final int UNIT_VEHICLE = 1;
	public static final int UNIT_AERO = 2;
	public static final int UNIT_DROPSHIP = 3;
	public static final int UNIT_INFANTRY = 4;
	public static final int UNIT_BATTLEARMOR = 5;
	public static final int UNIT_PROTOMECH = 6;
	public static final int UNIT_COUNT = 7;
	public static final String[] unitNames = {
		"Mek", "Vehicle", "Aero", "Dropship", "Infantry",
		"Battle Armor", "ProtoMek"
	};

	public static final String[] unitTypeNames = {
		"'Mech", "Vehicle", "Aero", "Dropship", "Infantry", "Battle armor",
		"ProtoMech"
	};
	
	public static final int QUALITY_F = 0;
	public static final int QUALITY_D = 1;
	public static final int QUALITY_C = 2;
	public static final int QUALITY_B = 3;
	public static final int QUALITY_A = 4;
	public static final int QUALITY_AA = 5;

	public Set<String> getAllRATNames() {
		return ratTree.keySet();
	}
	
	/**
	 * Retrieves the names of all RATCollections that meet the criteria.
	 * 
	 * @param faction-the shortname code of the faction or a generic code (_IS, _Periphery...)
	 * @param unitType-code for the type of unit (mech, vehicle, etc)
	 * @param year- if > 0, only RATs from this date or earlier are returned.
	 * @return-a list of RATCollection names that can be used for getRAT(...)
	 */
	public List<FactionTables> getRATCollections(String faction, int unitType, int year) {
		List<FactionTables> retval = new ArrayList<FactionTables>();
		for (String collection : ratTree.keySet()) {
			for (Integer y : ratTree.get(collection).keySet()) {
				if (year > 0 && y > year) {
					break;
				}
				if (ratTree.get(collection).get(y).get(faction) != null) {
					if (ratTree.get(collection).get(y).get(faction).hasTable(unitType)) {
						retval.add(ratTree.get(collection).get(y).get(faction));
					}
				}
			}
		}
		return retval;
	}

	/*
	 * returns true if the collection has tables for the faction/unitType
	 * not later than year
	 */
	public boolean hasRAT(String collection, int year, String faction, int unitType) {
		if (ratTree.get(collection) == null) {
			return false;
		}
		for (int y : ratTree.get(collection).keySet()) {
			if (y > year) {
				return false;
			}
			if (ratTree.get(collection).get(y).get(faction) != null &&
					ratTree.get(collection).get(y).get(faction).hasTable(unitType)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * returns true if the collection has tables for the faction/unitType
	 * regardless of year
	 */
	public boolean hasRAT(String collection, String faction, int unitType) {
		if (ratTree.get(collection) == null) {
			System.err.println("Could not find RAT collection " + collection);
			return false;
		}
		for (int year : ratTree.get(collection).keySet()) {
			if (ratTree.get(collection).get(year).get(faction) != null &&
					ratTree.get(collection).get(year).get(faction).hasTable(unitType)) {
				return true;
			}
		}
		return false;
	}

	public List<FactionTables> getRATCollections(String faction, int unitType) {
		return getRATCollections(faction, unitType, 0);
	}
	
	/* Searches an array of RAT sources in order for one that matches
	 * the faction, year, and unit type, checking alternate factions
	 * (periphery, general, etc.) for each source. If none is found,
	 * searches the array in ascending chronological order for the
	 * best fit, then defaults to Total Warfare.
	 */
	
	public FactionTables getBestRAT(String[] rats, int year, String faction, int unitType) {
		ArrayList<String> sorted = new ArrayList<String>();
		ArrayList<String> altFactions = getAltFactions(faction);
		for (String source : rats) {
			for (String f : altFactions) {
				if (hasRAT(source, year, f, unitType)) {
					return getClosestRAT(source, year, f, unitType);
				}
			}
			sorted.add(source);
		}
		/* A hack to deal with the lack of asfs in RATs before 3050 */
		if (unitType == UNIT_AERO && year < 3050) {
			for (String f : altFactions) {
				if (hasRAT("War of 39", f, UNIT_AERO)) {
					return getFirstRAT("War of 39", f, UNIT_AERO);
				}
			}
			String[] successorStates = {"CC", "DC", "FS", "FWL", "LA"};
			return getFirstRAT("War of 39",
					successorStates[Compute.randomInt(successorStates.length)],
					UNIT_AERO);
		}
		/* Sort the sources according to the earliest year that appears
		 * in each; may miss some overlap but should be accurate in
		 * the vast majority of cases.
		 */
		Collections.sort(sorted, new Comparator<String>() {
			public int compare(String arg0, String arg1) {
				return ratTree.get(arg0).keySet().iterator().next().
						compareTo(ratTree.get(arg0).keySet().iterator().next());
			}
		});
		for (String source : sorted) {
			for (String f : altFactions) {
				if (hasRAT(source, f, unitType)) {
					return getFirstRAT(source, f, unitType);
				}
			}
		}
		for (String f : altFactions) {
			if (hasRAT("Total Warfare", f, unitType)) {
				return getFirstRAT("Total Warfare", f, unitType);
			}
		}
		/* We shouldn't get here unless we're doing something like
		 * searching for Inner Sphere ProtoMechs.
		 */
		MekHQ.logMessage("Could not find RAT for " + faction +
				" unitType " + unitType + " in " + year );
		return null;
	}
	
	public ArrayList<String> getAltFactions(String fName) {
		ArrayList<String> retVal = new ArrayList<String>();
		retVal.add(fName);
		Faction f = Faction.getFaction(fName);
		if (null != altTables.get(fName)) {
			for (String s : altTables.get(fName)) {
				retVal.add(s);
			}
		}
		if (f.isClan()) {
			retVal.add("CLAN");
		}
		if (f.isPeriphery()) {
			retVal.add("Periphery");
		}
		retVal.add("General");
		return retVal;
	}

	/* Returns the latest year for the rat and faction that does not exceed year*/
	
	public FactionTables getClosestRAT(String rat, int year, String faction, int unitType) {
		FactionTables retval = null;
		for (int y : ratTree.get(rat).keySet()) {
			if (y > year) {
				return retval;
			}
			if (ratTree.get(rat).get(y).get(faction) != null &&
					ratTree.get(rat).get(y).get(faction).hasTable(unitType)) {
				retval = ratTree.get(rat).get(y).get(faction);
			}
		}
		return retval;
	}
	
	/* returns the first (earliest) RAT in the collection for the faction */

	public FactionTables getFirstRAT(String rat, String faction, int unitType) {
		for (int year : ratTree.get(rat).keySet()) {
			if (ratTree.get(rat).get(year).get(faction) != null &&
					ratTree.get(rat).get(year).get(faction).hasTable(unitType)) {
				return ratTree.get(rat).get(year).get(faction);
			}
		}
		return null;
	}

	public FactionTables getRAT(String rat, int year, String faction) {
		return ratTree.get(rat).get(year).get(faction);
	}
	
	public UnitTableData() {
		listeners = new ArrayList<ActionListener>();
	}

	public synchronized void registerListener(ActionListener l) {
		listeners.add(l);
	}

	public synchronized void removeListener(ActionListener l) {
		listeners.remove(l);
	}

	public synchronized void populateTables() {
		ratTree = new TreeMap<String, Map<Integer, Map<String, FactionTables>>>();
		altTables = new HashMap<String, ArrayList<String>>();
		loadTableDataFromFile(new File("data/universe/ratinfo.xml"));
		if (RandomUnitGenerator.getInstance().isInitialized()) {
			validate();
		} else {
			RandomUnitGenerator.getInstance().registerListener(this);
		}
		if (!interrupted) {
			utd.initialized = true;
			utd.initializing = false;
			utd.notifyListenersOfInitialization();
		}

		if (dispose) {
			clear();
			dispose = false;
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() instanceof RandomUnitGenerator) {
			validate();
		}
	}

	private void validate() {
		HashSet<String> ratList = new HashSet<String>();
		Iterator<String> iter = RandomUnitGenerator.getInstance().getRatList();
		while (iter.hasNext()) {
			ratList.add(iter.next());
		}


		String[] rats = ratTree.keySet().toArray(new String[0]);
		for (String rat : rats) {
			Integer[] years = ratTree.get(rat).keySet().toArray(new Integer[0]);
			for (int year : years) {
				String[] factions = ratTree.get(rat).get(year).keySet().toArray(new String[0]);
				for (String faction : factions) {
					if (!ratTree.get(rat).get(year).get(faction).isValid(ratList)) {
						ratTree.get(rat).get(year).remove(faction);
					}
				}
				if (ratTree.get(rat).get(year).isEmpty()) {
					ratTree.get(rat).remove(year);
				}
			}
			if (ratTree.get(rat).isEmpty()) {
				ratTree.remove(rat);
				MekHQ.logMessage(rat + " not found.");
			}
		}
	}

	public void notifyListenersOfInitialization() {
		if (initialized) {
			for (ActionListener l : listeners) {
				l.actionPerformed(new ActionEvent(
						this, ActionEvent.ACTION_PERFORMED, "ratiInitialized"));
			}
		}
	}

	private void loadTableDataFromFile(File f) {
		if (interrupted) {
			return;
		}

		if (f == null) {
			return;
		}

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
			createFromXml(fis);
			fis.close();
			fis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			MekHQ.logError("Error reading unit table data");
		}
	}
	
	private void createFromXml(FileInputStream fis) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document xmlDoc = null;
		
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			xmlDoc = db.parse(fis);
		} catch (Exception e) {
			MekHQ.logError("While loading unit table data: " + e.getMessage());
		}
		
		Element elem = xmlDoc.getDocumentElement();
		NodeList nl = elem.getChildNodes();
		elem.normalize();
		
		for (int i = 0; i < nl.getLength(); i++) {
			Node ratNode = nl.item(i);
			if (ratNode.getParentNode() != elem) {
				continue;
			}
			
			int xc = ratNode.getNodeType();
			
			if (xc == Node.ELEMENT_NODE) {
				String xn = ratNode.getNodeName();
				if (xn.equalsIgnoreCase("altTables")) {
					String fName = ratNode.getAttributes().getNamedItem("faction").getTextContent();
					if (null == altTables.get(fName)) {
						altTables.put(fName, new ArrayList<String>());
					}
					NodeList factionNodes = ratNode.getChildNodes();
					for (int j = 0; j < factionNodes.getLength(); j++) {
						Node altsNode = factionNodes.item(j);
						
						if (altsNode.getNodeType() == Node.ELEMENT_NODE) {
							String fn = altsNode.getNodeName();
							if (fn.equalsIgnoreCase("tables")) {
								for (String s : altsNode.getTextContent().trim().split(",")) {
									altTables.get(fName).add(s);
							}
						}
					}
							}
				} else if (xn.equalsIgnoreCase("rat")) {
					String ratSource = ratNode.getAttributes().getNamedItem("name").getTextContent();
					int year = Integer.parseInt(ratNode.getAttributes().getNamedItem("era").getTextContent());
					if (ratTree.get(ratSource) == null) {
						ratTree.put(ratSource, new TreeMap<Integer, Map<String, FactionTables>>());						
					}
					ratTree.get(ratSource).put(year, new TreeMap<String, FactionTables>());
					
					NodeList factionNodes = ratNode.getChildNodes();
					for (int j = 0; j < factionNodes.getLength(); j++) {
						Node factionNode = factionNodes.item(j);
						
						if (factionNode.getNodeType() == Node.ELEMENT_NODE) {
							String fn = factionNode.getNodeName();
							if (fn.equalsIgnoreCase("faction")) {
								String factionCode = factionNode.getAttributes().getNamedItem("code").getTextContent();
								FactionTables rf = new FactionTables(factionNode, factionCode, ratSource, year);
								if (rf != null) {
									ratTree.get(ratSource).get(year).put(factionCode, rf);
								}
							}
						}
					}
				}
			}
		}
		System.out.println("RAT files loaded");
	}

	public void dispose() {
		interrupted = true;
		dispose = true;
		if (initialized){
			clear();
		}
	}

	public void clear() {
		ratTree = null;
		utd = null;
		initialized = false;
		initializing = false;
	}	

	public static synchronized UnitTableData getInstance() {
		if (null == utd) {
			utd = new UnitTableData();
		}
		if (!utd.initialized && !utd.initializing) {
			utd.initializing = true;
			interrupted = false;
			dispose = false;
			utd.loader = new Thread(new Runnable() {
				public void run() {
					utd.populateTables();

				}
			}, "Random Allocation Table Info table populator");
			utd.loader.setPriority(Thread.NORM_PRIORITY - 1);
			utd.loader.start();
		}
		return utd;
	}

	public boolean isInitialized() {
		return initialized;
	}

	public class FactionTables {
		/**
		 * Represents all the tables for a particular faction in a set of tables.
		 * Values of strings are RAT names used by megamek.client.RandomUnitGenerator
		 */
		String ratSource;
		Integer year;
		String factionCode;

		/* 
		 * The name of the RAT used by RandomUnitGenerator.setChosenRAT(String)
		 * 
		 * Stored in arrays to distinguish weight and quality where applicable
		 * according to the following schema:
		 * 
		 * mechs.get(weight).get(quality)
		 * dropships.get(quality)
		 */
		ArrayList<ArrayList<String>> mechs;
		ArrayList<ArrayList<String>> vees;
		ArrayList<ArrayList<String>> aero;
		ArrayList<String> dropships;
		ArrayList<String> infantry;
		ArrayList<String> battleArmor;
		ArrayList<String> protomechs;

		private FactionTables (String rat, Integer year) {
			ratSource = rat;
			this.year = year;
			factionCode = "General";
		}

		public FactionTables(Node factionNode, String factionCode, String collection, int year) {
			this(collection, year);
			this.factionCode = factionCode;
			NodeList nl = factionNode.getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node unitNode = nl.item(i);
				int xc = unitNode.getNodeType();
				
				if (xc == Node.ELEMENT_NODE) {
					String xn = unitNode.getNodeName();
					if (xn.equalsIgnoreCase("mech")) {
						if (mechs == null) {
							mechs = new ArrayList<ArrayList<String>>();
						}
						loadWeightUnitsFromNode(mechs, unitNode);
					} else if (xn.equalsIgnoreCase("vehicle")) {
						if (vees == null) {
							vees = new ArrayList<ArrayList<String>>();
						}
						loadWeightUnitsFromNode(vees, unitNode);
					} else if (xn.equalsIgnoreCase("aerospace")) {
						if (aero == null) {
							aero = new ArrayList<ArrayList<String>>();
						}
						loadWeightUnitsFromNode(aero, unitNode);
					} else if (xn.equalsIgnoreCase("dropship")) {
						if (dropships == null) {
							dropships = new ArrayList<String>();
						}
						loadUnitsFromNode(dropships, unitNode);
					} else if (xn.equalsIgnoreCase("infantry")) {
						if (infantry == null) {
							infantry = new ArrayList<String>();
						}
						loadUnitsFromNode(infantry, unitNode);
					} else if (xn.equalsIgnoreCase("battlearmor")) {
						if (battleArmor == null) {
							battleArmor = new ArrayList<String>();
						}
						loadUnitsFromNode(battleArmor, unitNode);
					} else if (xn.equalsIgnoreCase("protomech")) {
						if (protomechs == null) {
							protomechs = new ArrayList<String>();
						}
						loadUnitsFromNode(protomechs, unitNode);
					}
				}
			}
		}

		private void loadWeightUnitsFromNode(ArrayList<ArrayList<String>> units, Node node) {
			String[] rats = node.getTextContent().split("\\|");
			int weightClass = 0;
			if (node.getAttributes().getNamedItem("weight") != null) {
				if (node.getAttributes().getNamedItem("weight").getTextContent().equalsIgnoreCase("light")) {
					weightClass = 0;
				} else	if (node.getAttributes().getNamedItem("weight").getTextContent().equalsIgnoreCase("medium")) {
					weightClass = 1;
				} else if (node.getAttributes().getNamedItem("weight").getTextContent().equalsIgnoreCase("heavy")) {
					weightClass = 2;
				} else if (node.getAttributes().getNamedItem("weight").getTextContent().equalsIgnoreCase("assault")) {
					weightClass = 3;
				}
			}

			if ((units.size() < weightClass + 1) || units.get(weightClass) == null) {
				while (units.size() < weightClass + 1) {
					units.add(new ArrayList<String>());
				}
			}
			
			for (String rat : rats) {
				units.get(weightClass).add(rat);
			}
		}

		private void loadUnitsFromNode(ArrayList<String> units, Node node) {
			String[] rats = node.getTextContent().split("\\|");
			for (String rat : rats) {
				units.add(rat);
			}
		}

		public String getName() {
			//used to write to save file
			return ratSource + "|" + year + "|" + factionCode;
		}

		public boolean hasTable(int unitType) {
			switch (unitType) {
			case UNIT_MECH:
				return mechs != null && mechs.size() > 0;
			case UNIT_VEHICLE:
				return vees != null && vees.size() > 0;
			case UNIT_AERO:
				return aero != null && aero.size() > 0;
			case UNIT_DROPSHIP:
				return dropships != null;
			case UNIT_INFANTRY:
				return infantry != null;
			case UNIT_BATTLEARMOR:
				return battleArmor != null;
			case UNIT_PROTOMECH:
				return protomechs != null;
			}
			return false;
		}

		public boolean hasMechTables() {
			return mechs.size() > 0;
		}
		public boolean hasVehicleTables() {
			return vees.size() > 0;
		}
		public boolean hasAeroTables() {
			return aero.size() > 0;
		}
		public boolean hasDropshipTable() {
			return dropships != null;
		}
		public boolean hasInfantryTable() {
			return infantry != null;
		}
		public boolean hasBATable() {
			return battleArmor != null;
		}
		public boolean hasPMTable() {
			return protomechs != null;
		}

		public String getTable(int unitType, int weight, int quality) {
			ArrayList<String> units = null;
			switch (unitType) {
			case UNIT_MECH:
				if (mechs.size() > 1) {
					units = mechs.get(weight);
				} else {
					units = mechs.get(0);
				}
				break;
			case UNIT_VEHICLE:
				if (vees.size() > 1) {
					units = vees.get(weight);
				} else {
					units = vees.get(0);
				}
				break;
			case UNIT_AERO:
				if (aero.size() > 1) {
					units = aero.get(weight);
				} else {
					units = aero.get(0);
				}
				break;
			case UNIT_DROPSHIP:
				units = dropships;
				break;
			case UNIT_INFANTRY:
				units = infantry;
				break;
			case UNIT_BATTLEARMOR:
				units = battleArmor;
				break;
			case UNIT_PROTOMECH:
				units = protomechs;
				break;
			}
			//Treat the top as A unless there is an explicit A+ RAT
			if (units.size() == 6) {
				return units.get(QUALITY_AA - quality);
			}
			if (quality == QUALITY_AA) {
				quality--;
			}
			return units.get(quality * units.size() / 5);
		}

		public String getTable(int unitType, int quality) {
			return getTable(unitType, 0, quality);
		}
		
		public boolean isValid(HashSet<String> rats) {
			if (!validateWeightUnits(mechs, rats)) {
				mechs = null;
			}
			if (!validateWeightUnits(vees, rats)) {
				vees = null;
			}
			if (!validateWeightUnits(aero, rats)) {
				aero = null;
			}
			if (!validateUnits(dropships, rats)) {
				dropships = null;
			}
			if (!validateUnits(infantry, rats)) {
				infantry = null;
			}
			if (!validateUnits(battleArmor, rats)) {
				battleArmor = null;
			}
			if (!validateUnits(protomechs, rats)) {
				protomechs = null;
			}
			return null != mechs || null != vees || null != aero || null != dropships ||
					null != infantry || null != battleArmor || null != protomechs; 
		}

		protected boolean validateWeightUnits(ArrayList<ArrayList<String>> units, HashSet<String> rats) {
			if (null == units) {
				return false;
			}
			int count = 0;
			int initSize = units.size();
			for (ArrayList<String> weightClass : units) {
				String[] copy = weightClass.toArray(new String[0]);
				for (String rat : copy) {
					if (!rats.contains(rat)) {
//						MekHQ.logMessage("UnitTableData: " + rat + " not found", 3);
						weightClass.remove(rat);
					}
				}
				if (weightClass.size() > 0) {
					count++;
				}
			}
			if (count < initSize) {
				return false;
			}
			return true;
		}

		protected boolean validateUnits(ArrayList<String> units, HashSet<String> rats) {
			if (null == units) {
				return false;
			}
			String[] copy = units.toArray(new String[0]);
			for (String rat : copy) {
				if (!rats.contains(rat)) {
//					MekHQ.logMessage("UnitTableData: " + rat + " not found", 3);
					units.remove(rat);
				}
			}
			if (units.size() > 0) {
				return true;
			}
			return false;
		}
	}

}
