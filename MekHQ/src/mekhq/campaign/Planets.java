package mekhq.campaign;

import java.io.FileInputStream;
import java.text.ParseException;
import java.util.Hashtable;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import mekhq.MekHQApp;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Planets {
	
	private boolean initialized = false;
	private boolean initializing = false;
	private static Planets planets;
	private static Hashtable<String, Planet> planetList = new Hashtable<String, Planet>();
    private Thread loader;

    private Planets() {
        planetList = new Hashtable<String, Planet>();
    }
	
	public static Planets getInstance() {
		if (planets == null) {
            planets = new Planets();
        }
        if (!planets.initialized && !planets.initializing) {
            planets.initializing = true;
            planets.loader = new Thread(new Runnable() {
                public void run() {
                    planets.initialize();
                }
            }, "Planet Loader");
            planets.loader.setPriority(Thread.NORM_PRIORITY - 1);
            planets.loader.start();
        }
		return planets;	
	}
	
	private void initialize() {
		try {
			planetList = generatePlanets();
		} catch (DOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Hashtable<String, Planet> getPlanets() {
		return planetList;
	}
	
	private void done() {
        initialized = true;
        initializing = false;
	}
	
	public boolean isInitialized() {
        return initialized;
    }
	
	public Hashtable<String,Planet> generatePlanets() throws DOMException, ParseException {
		MekHQApp.logMessage("Starting load of planetary data from XML...");
		// Initialize variables.
		Hashtable<String,Planet> retVal = new Hashtable<String,Planet>();
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		Document xmlDoc = null;
	
		
		try {
			FileInputStream fis = new FileInputStream("data/planets.xml");
			// Using factory get an instance of document builder
			DocumentBuilder db = dbf.newDocumentBuilder();
	
			// Parse using builder to get DOM representation of the XML file
			xmlDoc = db.parse(fis);
		} catch (Exception ex) {
			MekHQApp.logError(ex);
		}
	
		Element planetEle = xmlDoc.getDocumentElement();
		NodeList nl = planetEle.getChildNodes();
	
		// Get rid of empty text nodes and adjacent text nodes...
		// Stupid weird parsing of XML.  At least this cleans it up.
		planetEle.normalize(); 
	
		// Okay, lets iterate through the children, eh?
		for (int x = 0; x < nl.getLength(); x++) {
			Node wn = nl.item(x);
	
			if (wn.getParentNode() != planetEle)
				continue;
	
			int xc = wn.getNodeType();
	
			if (xc == Node.ELEMENT_NODE) {
				// This is what we really care about.
				// All the meat of our document is in this node type, at this
				// level.
				// Okay, so what element is it?
				String xn = wn.getNodeName();
	
				if (xn.equalsIgnoreCase("planet")) {
					Planet p = Planet.getPlanetFromXML(wn);
					String name = p.getName();
					if(null == retVal.get(name)) {
						retVal.put(name, p);
					} else {
						//for duplicate planets, put a faction name behind them
						//There could still be duplicates in theory, but I don't think there are in practice
						Planet oldPlanet = retVal.get(name);
						retVal.remove(name);
						oldPlanet.resetName(oldPlanet.getName() + " (" + Faction.getFactionName(oldPlanet.getBaseFaction()) + ")");
						retVal.put(oldPlanet.getName(), oldPlanet);
						p.resetName(p.getName() + " (" + Faction.getFactionName(p.getBaseFaction()) + ")");
						retVal.put(p.getName(), p);
					}
					
				}
			}
		}	
		MekHQApp.logMessage("Loaded a total of " + retVal.size() + " planets");
		done();
		return retVal;
	}
	
}