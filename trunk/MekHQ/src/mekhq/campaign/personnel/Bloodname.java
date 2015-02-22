/**
 *
 */
package mekhq.campaign.personnel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import megamek.common.Compute;
import mekhq.MekHQ;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Neoancient
 *
 *
 */
public class Bloodname implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 3958964485520416824L;

	private static ArrayList<Bloodname> bloodnames;

	public static final int P_GENERAL = 0;
	public static final int P_MECHWARRIOR = 1;
	public static final int P_AEROSPACE = 2;
	public static final int P_ELEMENTAL = 3;
	public static final int P_PROTOMECH = 4;
	public static final int P_NAVAL = 5;
	public static final int P_NUM = 6;

	public static final String[] phenotypeNames = {
		"General", "MechWarrior", "Aerospace Pilot", "Elemental",
		"ProtoMech Pilot", "Naval Commander"
	};

	private String name;
	private String founder;
	private Clan origClan;
	private boolean exclusive;
	private boolean limited;
	private int inactive;
	private int abjured;
	private int reactivated;
	private int startDate;
	private int phenotype;
	ArrayList<Clan> postReavingClans;
	ArrayList<NameAcquired> acquiringClans;
	NameAcquired absorbed;

	public Bloodname() {
		name = "";
		founder = "";
		exclusive = false;
		limited = false;
		inactive = 0;
		abjured = 0;
		reactivated = 0;
		startDate = 2807;
		phenotype = P_GENERAL;
		postReavingClans = new ArrayList<Clan>();
		acquiringClans = new ArrayList<NameAcquired>();
		absorbed = null;
	}

	public String getName() {
		return name;
	}

	public String getFounder() {
		return founder;
	}

	public String getOrigClan() {
		return origClan.getCode();
	}

	public boolean isExclusive() {
		return exclusive;
	}

	public boolean isLimited() {
		return limited;
	}

	public boolean isInactive(int year) {
		return year < startDate || (inactive > 0 && inactive < year &&
				!(reactivated > 0 && reactivated <= year));
	}

	public boolean isAbjured(int year) {
		return abjured > 0 && abjured < year;
	}

	public int getPhenotype() {
		return phenotype;
	}

	public ArrayList<Clan> getPostReavingClans() {
		return postReavingClans;
	}

	public ArrayList<NameAcquired> getAcquiringClans() {
		return acquiringClans;
	}

	public NameAcquired getAbsorbed() {
		return absorbed;
	}

	/**
	 * 
	 * @param warriorType A Person.PHENOTYPE_* constant
	 * @param year The current year of the campaign setting
	 * @return An adjustment to the frequency of this name for the phenotype.
	 * 
	 * A warrior is three times as likely to have a Bloodname associated with the
	 * same phenotype as a general name (which is split among the three types).
	 * Elemental names are treated as general prior to 2870. The names that later
	 * became associated with ProtoMech pilots (identified in WoR) are assumed
	 * to have been poor performers and have a lower frequency even before the
	 * invention of the PM, though have a higher frequency for PM pilots than other
	 * aerospace names. 
	 */
	public int phenotypeMultiplier(int warriorType, int year) {
		switch (phenotype) {
		case P_MECHWARRIOR:
			return (warriorType == P_MECHWARRIOR)?3:0;
		case P_AEROSPACE:
			return (warriorType == P_AEROSPACE || warriorType == P_PROTOMECH)?3:0;
		case P_ELEMENTAL:
			if (year < 2870) {
				return 1;
			}
			return (warriorType == P_ELEMENTAL)?3:0;
		case P_PROTOMECH:
			switch (warriorType) {
			case P_PROTOMECH:return 9;
			case P_AEROSPACE:return 1;
			default:return 0;
			}
		case P_NAVAL:
			return (warriorType == P_NAVAL)?3:0;
		case P_GENERAL:
		default:
			return 1;
		}
	}

	public static Bloodname loadFromXml(Node node) {
		Bloodname retVal = new Bloodname();
		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node wn = nl.item(i);

			if (wn.getNodeName().equalsIgnoreCase("name")) {
				retVal.name = wn.getTextContent().trim();
			} else if (wn.getNodeName().equalsIgnoreCase("founder")) {
				retVal.founder = wn.getTextContent().trim();
			} else if (wn.getNodeName().equalsIgnoreCase("clan")) {
				retVal.origClan = Clan.getClan(wn.getTextContent().trim());
			} else if (wn.getNodeName().equalsIgnoreCase("exclusive")) {
				retVal.exclusive = true;
			} else if (wn.getNodeName().equalsIgnoreCase("reaved")) {
				retVal.inactive = Integer.parseInt(wn.getTextContent().trim());
			} else if (wn.getNodeName().equalsIgnoreCase("dormant")) {
				retVal.inactive = Integer.parseInt(wn.getTextContent().trim()) + 10;
			} else if (wn.getNodeName().equalsIgnoreCase("abjured")) {
				retVal.abjured = Integer.parseInt(wn.getTextContent().trim());
			} else if (wn.getNodeName().equalsIgnoreCase("reactivated")) {
				retVal.reactivated = Integer.parseInt(wn.getTextContent().trim() + 20);
			} else if (wn.getNodeName().equalsIgnoreCase("phenotype")) {
				switch (wn.getTextContent().trim()) {
				case "General":
					retVal.phenotype = P_GENERAL;
					break;
				case "MechWarrior":
					retVal.phenotype = P_MECHWARRIOR;
					break;
				case "Aerospace":
					retVal.phenotype = P_AEROSPACE;
					break;
				case "Elemental":
					retVal.phenotype = P_ELEMENTAL;
					break;
				case "ProtoMech":
					retVal.phenotype = P_PROTOMECH;
					break;
				case "Naval":
					retVal.phenotype = P_NAVAL;
					break;
				default:
					System.err.println("Unknown phenotype " +
							wn.getTextContent() + " in " + retVal.name);
				}
			} else if (wn.getNodeName().equalsIgnoreCase("postReaving")) {
				String[] clans = wn.getTextContent().trim().split(",");
				for (String c : clans) {
					retVal.postReavingClans.add(Clan.getClan(c));
				}
			} else if (wn.getNodeName().equalsIgnoreCase("acquired")) {
				retVal.acquiringClans.add(retVal.new NameAcquired(
						Integer.parseInt(wn.getAttributes().getNamedItem("date").getTextContent()) + 10,
						wn.getTextContent().trim()));
			} else if (wn.getNodeName().equalsIgnoreCase("shared")) {
				retVal.acquiringClans.add(retVal.new NameAcquired(
						Integer.parseInt(wn.getAttributes().getNamedItem("date").getTextContent()),
						wn.getTextContent().trim()));
			} else if (wn.getNodeName().equalsIgnoreCase("absorbed")) {
				retVal.absorbed = retVal.new NameAcquired(
						Integer.parseInt(wn.getAttributes().getNamedItem("date").getTextContent()),
						wn.getTextContent().trim());
			} else if (wn.getNodeName().equalsIgnoreCase("created")) {
				retVal.startDate = Integer.parseInt(wn.getTextContent().trim()) + 20;
			}
		}

		return retVal;
	}

	public static Bloodname randomBloodname(String factionCode, int phenotype, int year) {
		return randomBloodname(Clan.getClan(factionCode), phenotype, year);
	}

	/**
	 * Determines a likely Bloodname based on Clan, phenotype, and year.
	 * 
	 * @param faction The faction code for the Clan; must exist in data/names/bloodnames/clans.xml
	 * @param phenotype One of the Person.PHENOTYPE_* constants
	 * @param year The current campaign year
	 * @return An object representing the chosen Bloodname
	 * 
	 * Though based as much as possible on official sources, the method employed here involves a
	 * considerable amount of speculation.
	 */
	public static Bloodname randomBloodname(Clan faction, int phenotype, int year) {
	    if (null == faction) {
	        MekHQ.logError("Random Bloodname attempted for a clan that does not exist."
	                + System.lineSeparator()
	                + "Please ensure that your clan exists in both the clans.xml and bloodnames.xml files as appropriate.");
	        return null;
	    }
		if (Compute.randomInt(20) == 0) {
			/* 1 in 20 chance that warrior was taken as isorla from another Clan */
			return randomBloodname(faction.getRivalClan(year), phenotype, year);
		}
		if (Compute.randomInt(20) == 0) {
			/* Bloodnames that are predominantly used for a particular phenotype are not
			 * exclusively used for that phenotype. A 5% chance of ignoring phenotype will
			 * result in a very small chance (around 1%) of a Bloodname usually associated
			 * with a different phenotype.
			 */
			phenotype = Bloodname.P_GENERAL;
		}

		/* The relative probability of the various Bloodnames that are original to this Clan */
		HashMap<Bloodname, Fraction> weights = new HashMap<Bloodname, Fraction>();
		/* A list of non-exclusive Bloodnames from other Clans */
		ArrayList<Bloodname> nonExclusives = new ArrayList<Bloodname>();
		/* The relative probability that a warrior in this Clan will have a non-exclusive
		 * Bloodname that originally belonged to another Clan; the smaller the number
		 * of exclusive Bloodnames of this Clan, the larger this chance.
		 */
		double nonExclusivesWeight = 0.0;

		for (Bloodname name : bloodnames) {
			/* Bloodnames exclusive to Clans that have been abjured (NC, WIE) continue
			 * to be used by those Clans but not by others.
			 */
			if (name.isInactive(year) ||
					(name.isAbjured(year) && !name.getOrigClan().equals(faction)) ||
					0 == name.phenotypeMultiplier(phenotype, year)) {
				continue;
			}

			Fraction weight = null;

			/* Effects of the Wars of Reaving would take a generation to show up
			 * in the breeding programs, so the tables given in the WoR sourcebook
			 * are in effect from about 3100 on.
			 */
			if (year < 3100) {
				int numClans = 1;
				for (Bloodname.NameAcquired a : name.getAcquiringClans()) {
					if (a.year < year) {
						numClans++;
					}
				}
				/* Non-exclusive names have a weight of 1 (equal to exclusives) up to 2900,
				 * then decline 10% per 50 years to a minimum of 0.6 in 3050+. In the few
				 * cases where the other Clans using the name are known, the weight is
				 * 1/(number of Clans) instead.
				 */
				if (name.getOrigClan().equals(faction.getCode()) ||
						(null != name.getAbsorbed() && faction.equals(name.getAbsorbed().clan) &&
						name.getAbsorbed().year > year)) {
					if (name.isExclusive() || numClans > 1) {
						weight = new Fraction(1, numClans);
					} else {
						weight = eraFraction(year);
						nonExclusivesWeight += 1 - eraFraction(year).value();
						/* The fraction is squared to represent the combined effect
						 * of increasing distribution among the Clans and the likelihood
						 * that non-exclusive names would suffer
						 * more reavings and have a lower Bloodcount.
						 */
					weight.mul(eraFraction(year));
					}
				} else {
					/* Most non-exclusives have an unknown distribution and are estimated.
					 * When the actual Clans sharing the Bloodname are known, it is divided
					 * among those Clans.
					 */
					for (Bloodname.NameAcquired a : name.getAcquiringClans()) {
						if (faction.equals(a.clan)) {
							weight = new Fraction(1, numClans);
							break;
						}
					}
					if (null == weight && !name.isExclusive()) {
						for (int i = 0; i < name.phenotypeMultiplier(phenotype, year); i++) {
							nonExclusives.add(name);
						}
					}
				}
			} else {
				if (name.getPostReavingClans().contains(faction)) {
					weight = new Fraction(name.phenotypeMultiplier(phenotype, year),
							name.getPostReavingClans().size());
					/* Assume that Bloodnames that were exclusive before the Wars of Reaving
					 * are more numerous (higher bloodcount).
					 */
					if (!name.isLimited()) {
						if (name.isExclusive()) {
							weight.mul(4);
						} else {
							weight.mul(2);
						}
					}
				} else if (name.getPostReavingClans().size() == 0) {
					for (int i = 0; i < name.phenotypeMultiplier(phenotype, year); i++) {
						nonExclusives.add(name);
					}
				}
			}
			if (null != weight) {
				weight.mul(name.phenotypeMultiplier(phenotype, year));
				weights.put(name, weight);
			}
		}

		int lcd = Fraction.lcd(weights.values());
		for (Fraction f : weights.values()) {
			f.mul(lcd);
		}
		ArrayList<Bloodname> nameList = new ArrayList<Bloodname>();
		for (Bloodname b : weights.keySet()) {
			for (int i = 0; i < weights.get(b).value(); i++) {
				nameList.add(b);
			}
		}
		nonExclusivesWeight *= lcd;
		if (year >= 3100) {
			nonExclusivesWeight = nameList.size() / 10.0;
		}
		int roll = Compute.randomInt(nameList.size() + (int)(nonExclusivesWeight + 0.5));
		if (roll > nameList.size() - 1) {
			return nonExclusives.get(Compute.randomInt(nonExclusives.size()));
		}
		return nameList.get(roll);
	}

	/**
	 * Represents the decreasing frequency of non-exclusive names within the original Clan
	 * due to dispersal throughout the Clans and reavings.
	 * 
	 * @param year The current year of the campaign
	 * @return A fraction that decreases by 10%/year
	 */
	
	private static Fraction eraFraction(int year) {
		if (year < 2900) {
			return new Fraction(1);
		}
		if (year < 2950) {
			return new Fraction(9, 10);
		}
		if (year < 3000) {
			return new Fraction(4, 5);
		}
		if (year < 3050) {
			return new Fraction(7, 10);
		}
		return new Fraction (3, 5);
	}

	public static void loadBloodnameData() {
		Clan.loadClanData();
		bloodnames = new ArrayList<Bloodname>();

		File f = new File("data/names/bloodnames/bloodnames.xml");
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			MekHQ.logError("Cannot find file bloodnames.xml");
			return;
		}

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document doc = null;

		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.parse(fis);
		} catch (Exception ex) {
			System.err.println(ex.getMessage());
		}

		Element bloodnameElement = doc.getDocumentElement();
		NodeList nl = bloodnameElement.getChildNodes();
		bloodnameElement.normalize();

		for (int i = 0; i < nl.getLength(); i++) {
			Node wn = nl.item(i);
			if (wn.getNodeType() == Node.ELEMENT_NODE) {
				if (wn.getNodeName().equalsIgnoreCase("bloodname")) {
					bloodnames.add(Bloodname.loadFromXml(wn));
				}
			}
		}
		MekHQ.logMessage("Loaded " + bloodnames.size() + " Bloodname records.");
	}

	public class NameAcquired {
		public int year;
		public String clan;
		public NameAcquired(int y, String c) {
			year = y;
			clan = c;
		}
	}

}

class DatedRecord {
	public int startDate;
	public int endDate;
	public String descr;

	public DatedRecord(int s, int e, String d) {
		startDate = s;
		endDate = e;
		descr = d;
	}

}

class Clan {
	private static HashMap<String, Clan> allClans;

	private String code;
	private String fullName;
	private int startDate;
	private int endDate;
	private int abjurationDate;
	private ArrayList<DatedRecord> rivals;
	private ArrayList<DatedRecord> nameChanges;
	private boolean homeClan;

	public Clan() {
		startDate = endDate = abjurationDate = 0;
		rivals = new ArrayList<DatedRecord>();
		nameChanges = new ArrayList<DatedRecord>();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Clan) {
			return code.equals(((Clan)o).code);
		}
		if (o instanceof String) {
			return code.equals((String)o);
		}
		return false;
	}

	public static Clan getClan(String code) {
		return allClans.get(code);
	}

	public String getCode() {
		return code;
	}

	public String getFullName(int year) {
		for (DatedRecord r : nameChanges) {
			if (r.startDate < year &&
					(r.endDate == 0 || r.endDate > year)) {
				return r.descr;
			}
		}
		return fullName;
	}

	public boolean isActive(int year) {
		return startDate < year && (endDate == 0 || endDate > year);
	}

	public boolean isAbjured(int year) {
		if (abjurationDate == 0) return false;
		return abjurationDate < year;
	}

	public ArrayList<Clan> getRivals(int year) {
		ArrayList<Clan> retVal = new ArrayList<Clan>();
		for (DatedRecord r : rivals) {
			if (r.startDate < year && (endDate == 0) || endDate > year) {
				Clan c = allClans.get(r.descr);
				if (c.isActive(year)) {
					retVal.add(c);
				}
			}
		}
		return retVal;
	}

	public boolean isHomeClan() {
		return homeClan;
	}

	public Clan getRivalClan(int year) {
		ArrayList<Clan> rivals = getRivals(year);
		int roll = Compute.randomInt(rivals.size() + 1);
		if (roll > rivals.size() - 1) {
			return randomClan(year, homeClan);
		}
		return rivals.get(roll);
	}

	public static Clan randomClan(int year, boolean homeClan) {
		ArrayList<Clan> list = new ArrayList<Clan>();
		for (Clan c : allClans.values()) {
			if (year > 3075 && homeClan != c.homeClan) {
				continue;
			}
			if (c.isActive(year)) {
				list.add(c);
			}
		}
		return list.get(Compute.randomInt(list.size()));
	}

	public static void loadClanData() {
		allClans = new HashMap<String, Clan>();
		File f = new File("data/names/bloodnames/clans.xml");
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
		} catch (FileNotFoundException e) {
			MekHQ.logError("Cannot find file clans.xml");
			return;
		}

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document doc = null;

		try {
			DocumentBuilder db = dbf.newDocumentBuilder();
			doc = db.parse(fis);
		} catch (Exception ex) {
			System.err.println(ex.getMessage());
		}

		Element clanElement = doc.getDocumentElement();
		NodeList nl = clanElement.getChildNodes();
		clanElement.normalize();

		for (int i = 0; i < nl.getLength(); i++) {
			Node wn = nl.item(i);
			if (wn.getNodeName().equalsIgnoreCase("clan")) {
				Clan c = loadFromXml(wn);
				allClans.put(c.code, c);
			}
		}
	}

	private static Clan loadFromXml(Node node) {
		Clan retVal = new Clan();

		retVal.code = node.getAttributes().getNamedItem("code").getTextContent().trim();
		if (null != node.getAttributes().getNamedItem("start")) {
			retVal.startDate = Integer.parseInt(node.getAttributes().getNamedItem("start").getTextContent().trim());
		}
		if (null != node.getAttributes().getNamedItem("end")) {
			retVal.endDate = Integer.parseInt(node.getAttributes().getNamedItem("end").getTextContent().trim());
		}
		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node wn = nl.item(i);

			if (wn.getNodeName().equalsIgnoreCase("fullName")) {
				retVal.fullName = wn.getTextContent().trim();
			} else if (wn.getNodeName().equalsIgnoreCase("abjured")) {
				retVal.abjurationDate = Integer.parseInt(wn.getTextContent().trim());
			} else if (wn.getNodeName().equalsIgnoreCase("nameChange")) {
				int start = retVal.startDate;
				int end = retVal.endDate;
				if (null != wn.getAttributes().getNamedItem("start")) {
					start = Integer.parseInt(wn.getAttributes().getNamedItem("start").getTextContent().trim());
				}
				if (null != wn.getAttributes().getNamedItem("end")) {
					end = Integer.parseInt(wn.getAttributes().getNamedItem("end").getTextContent().trim());
				}
				retVal.nameChanges.add(new DatedRecord(start, end, wn.getTextContent().trim()));
			} else if (wn.getNodeName().equalsIgnoreCase("rivals")) {
				int start = retVal.startDate;
				int end = retVal.endDate;
				if (null != wn.getAttributes().getNamedItem("start")) {
					start = Integer.parseInt(wn.getAttributes().getNamedItem("start").getTextContent().trim());
				}
				if (null != wn.getAttributes().getNamedItem("end")) {
					end = Integer.parseInt(wn.getAttributes().getNamedItem("end").getTextContent().trim());
				}
				String[] rivals = wn.getTextContent().trim().split(",");
				for (String r : rivals) {
					retVal.rivals.add(new DatedRecord(start, end, r));
				}
			} else if (wn.getNodeName().equalsIgnoreCase("homeClan")) {
				retVal.homeClan = true;
			}
		}

		return retVal;
	}
}

class Fraction {

	private int numerator;
	private int denominator;

	public Fraction() {
		numerator = 0;
		denominator = 1;
	}

	public Fraction(int n, int d) {
		if (d == 0) {
			throw new IllegalArgumentException("Denominator is zero.");
		}
		if (d < 0) {
			n = -n;
			d = -d;
		}
		numerator = n;
		denominator = d;
	}

	public Fraction(int i) {
		numerator = i;
		denominator = 1;
	}

	public Fraction(Fraction f) {
		numerator = f.numerator;
		denominator = f.denominator;
	}

	@Override
	public Object clone() {
		return new Fraction(this);
	}

	@Override
	public String toString() {
		return numerator + "/" + denominator;
	}

	public boolean equals(Fraction f) {
		return value() == f.value();
	}

	public double value() {
		return (double)numerator / (double)denominator;
	}

	public void reduce() {
		if (denominator > 1) {
			for (int i = denominator - 1; i > 1; i--) {
				if (numerator % i == 0 && denominator % i == 0) {
					numerator /= i;
					denominator /= i;
					i = denominator - 1;
				}
			}
		}
	}

	public int getNumerator() {
		return numerator;
	}

	public int getDenominator() {
		return denominator;
	}

	public void add(Fraction f) {
		numerator = numerator * f.denominator + f.numerator * denominator;
		denominator = denominator * f.denominator;
		reduce();
	}

	public void add(int i) {
		numerator += i * denominator;
		reduce();
	}

	public void sub(Fraction f) {
		numerator = numerator * f.denominator - f.numerator * denominator;
		denominator = denominator * f.denominator;
		reduce();
	}

	public void sub(int i) {
		numerator -= i * denominator;
		reduce();
	}

	public void mul(Fraction f) {
		numerator *= f.numerator;
		denominator *= f.denominator;
		reduce();
	}

	public void mul(int i) {
		numerator *= i;
		reduce();
	}

	public void div(Fraction f) {
		numerator *= f.denominator;
		denominator *= f.numerator;
		reduce();
	}

	public void div(int i) {
		denominator *= i;
	}

	public static int lcd(Collection<Fraction> list) {
		HashSet<Integer> denominators = new HashSet<Integer>();
		for (Fraction f : list) {
			denominators.add(f.denominator);
		}
		boolean done = false;
		int retVal = 1;
		while (!done) {
			done = true;
			for (Integer d : denominators) {
				if (d / retVal > 1 || retVal % d != 0) {
					retVal++;
					done = false;
					break;
				}
			}
		}
		return retVal;

	}
}
