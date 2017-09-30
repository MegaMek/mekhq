/*
 * Copyright (C) 2011-2016 MegaMek team
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.text.Normalizer.Form;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.transform.stream.StreamSource;

import org.joda.time.DateTime;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

import megamek.common.EquipmentType;
import megamek.common.logging.LogLevel;
import megameklab.com.util.StringUtils;
import mekhq.FileParser;
import mekhq.MekHQ;
import mekhq.Utilities;
import mekhq.campaign.universe.Planet.PlanetaryEvent;

public class Planets {
    private final static Object LOADING_LOCK = new Object[0];
    private static Planets planets;

    // Marshaller / unmarshaller instances
    private static Marshaller marshaller;
    private static Unmarshaller unmarshaller;
    static {
        try {
            JAXBContext context = JAXBContext.newInstance(LocalPlanetList.class, Planet.class);
            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            unmarshaller = context.createUnmarshaller();
            // For debugging only!
            // unmarshaller.setEventHandler(new javax.xml.bind.helpers.DefaultValidationEventHandler());
        } catch(JAXBException e) {
            MekHQ.getLogger().log(Planets.class, "<init>", e); //$NON-NLS-1$
        }
    }
    
    public static Planets getInstance() {
        if(planets == null) {
            planets = new Planets();
        }
        if(!planets.initialized && !planets.initializing) {
            planets.initializing = true;
            planets.loader = new Thread(new Runnable() {
                @Override
                public void run() {
                    planets.initialize();
                }
            }, "Planet Loader");
            planets.loader.setPriority(Thread.NORM_PRIORITY - 1);
            planets.loader.start();
        }
        return planets;
    }
    
    public static void reload(boolean waitForFinish) {
        planets = null;
        getInstance();
        if(waitForFinish) {
            try {
                while(!planets.isInitialized()) {
                    Thread.sleep(10);
                }
            } catch(InterruptedException iex) {
                MekHQ.getLogger().log(Planets.class, "reload(boolean)", iex); //$NON-NLS-1$
            }
        }
    }

    private ConcurrentMap<String, Planet> planetList = new ConcurrentHashMap<>();
    /* organizes systems into a grid of 30lyx30ly squares so we can find
     * nearby systems without iterating through the entire planet list. */
    private HashMap<Integer, Map<Integer, Set<Planet>>> planetGrid = new HashMap<>();
    
    // HPG Network cache (to not recalculate all the damn time)
    private Collection<Planets.HPGLink> hpgNetworkCache = null;
    private DateTime hpgNetworkCacheDate = null;
    
    private Thread loader;
    private boolean initialized = false;
    private boolean initializing = false;
    
    private Planets() {}

    private Set<Planet> getPlanetGrid(int x, int y) {
        if( !planetGrid.containsKey(x) ) {
            return null;
        }
        return planetGrid.get(x).get(y);
    }
    
    public List<Planet> getNearbyPlanets(final double centerX, final double centerY, int distance) {
        List<Planet> neighbors = new ArrayList<>();
        int gridRadius = (int)Math.ceil(distance / 30.0);
        int gridX = (int)(centerX / 30.0);
        int gridY = (int)(centerY / 30.0);
        for (int x = gridX - gridRadius; x <= gridX + gridRadius; x++) {
            for (int y = gridY - gridRadius; y <= gridY + gridRadius; y++) {
                Set<Planet> grid = getPlanetGrid(x, y);
                if(null != grid) {
                    for(Planet p : grid) {
                        if(p.getDistanceTo(centerX, centerY) <= distance) {
                            neighbors.add(p);
                        }
                    }
                }
            }
        }
        Collections.sort(neighbors, new Comparator<Planet>() {
            @Override
            public int compare(Planet o1, Planet o2) {
                return Double.compare(o1.getDistanceTo(centerX, centerY), o2.getDistanceTo(centerX, centerY));
            }
        });
        return neighbors;
    }
    
    public List<Planet> getNearbyPlanets(final Planet planet, int distance) {
        return getNearbyPlanets(planet.getX(), planet.getY(), distance);
    }

    public ConcurrentMap<String, Planet> getPlanets() {
        return planetList;
    }
    
    public Planet getPlanetById(String id) {
        return( null != id ? planetList.get(id) : null);
    }
    
    /** Return the planet by given name at a given time point */
    public Planet getPlanetByName(String name, DateTime when) {
        if(null == name) {
            return null;
        }
        name = name.toLowerCase(Locale.ROOT);
        for(Planet planet : planetList.values()) {
            if(null != planet) {
                String planetName = planet.getName(when);
                if((null != planetName) && planetName.toLowerCase(Locale.ROOT).equals(name)) {
                    return planet;
                }
                planetName = planet.getShortName(when);
                if((null != planetName) && planetName.toLowerCase(Locale.ROOT).equals(name)) {
                    return planet;
                }
            }
        }
        return null;
    }

    public List<NewsItem> getPlanetaryNews(DateTime when) {
        List<NewsItem> news = new ArrayList<>();
        for(Planet planet : planetList.values()) {
            if(null != planet) {
                Planet.PlanetaryEvent event = planet.getEvent(when);
                if((null != event) && (null != event.message)) {
                    NewsItem item = new NewsItem();
                    item.setHeadline(event.message);
                    item.setDate(event.date);
                    item.setLocation(planet.getPrintableName(when));
                    news.add(item);
                }
            }
        }
        return news;
    }
    
    /** Clean up the local HPG network cache */
    public void recalcHPGNetwork() {
        hpgNetworkCacheDate = null;
    }
    
    public Collection<Planets.HPGLink> getHPGNetwork(DateTime when) {
        if((null != when) && when.equals(hpgNetworkCacheDate)) {
            return hpgNetworkCache;
        }
        
        Set<HPGLink> result = new HashSet<>();
        for(Planet planet : planetList.values()) {
            Integer hpg = planet.getHPG(when);
            if((null != hpg) && (hpg.intValue() == EquipmentType.RATING_A)) {
                Collection<Planet> neighbors = getNearbyPlanets(planet, 50);
                for(Planet neighbor : neighbors) {
                    hpg = neighbor.getHPG(when);
                    if(null != hpg) {
                        HPGLink link = new HPGLink(planet, neighbor, hpg.intValue());
                        if(!result.contains(link)) {
                            result.add(link);
                        }
                    }
                }
            }
        }
        hpgNetworkCache = result;
        hpgNetworkCacheDate = when;
        return result;
    }
    
    // Customisation and export helper methods
    
    /** @return <code>true</code> if the planet was known and got updated, <code>false</code> otherwise */
    public boolean updatePlanetaryEvents(String id, Collection<Planet.PlanetaryEvent> events) {
        return updatePlanetaryEvents(id, events, false);
    }
    
    /** @return <code>true</code> if the planet was known and got updated, <code>false</code> otherwise */
    public boolean updatePlanetaryEvents(String id, Collection<Planet.PlanetaryEvent> events, boolean replace) {
        Planet planet = getPlanetById(id);
        if(null == planet) {
            return false;
        }
        if(null != events) {
            for(Planet.PlanetaryEvent event : events) {
                if(null != event.date) {
                    Planet.PlanetaryEvent planetaryEvent = planet.getOrCreateEvent(event.date);
                    if(replace) {
                        planetaryEvent.replaceDataFrom(event);
                    } else {
                        planetaryEvent.copyDataFrom(event);
                    }
                }
            }
        }
        return true;
    }
    
    public void writePlanet(OutputStream out, Planet planet) {
        try {
            marshaller.marshal(planet, out);
        } catch (Exception e) {
            MekHQ.getLogger().log(getClass(), "writePlanet(OutputStream,Planet)", e); //$NON-NLS-1$
        }
    }
    
    public void writePlanet(Writer out, Planet planet) {
        try {
            marshaller.marshal(planet, out);
        } catch (Exception e) {
            MekHQ.getLogger().log(getClass(), "writePlanet(Writer,Planet)", e); //$NON-NLS-1$
        }
    }
    

    public void writePlanetaryEvent(OutputStream out, Planet.PlanetaryEvent event) {
        try {
            marshaller.marshal(event, out);
        } catch (Exception e) {
            MekHQ.getLogger().log(getClass(), "writePlanet(OutputStream,Planet.PlanetaryEvent)", e); //$NON-NLS-1$
        }
    }
    
    public void writePlanetaryEvent(Writer out, Planet.PlanetaryEvent event) {
        try {
            marshaller.marshal(event, out);
        } catch (Exception e) {
            MekHQ.getLogger().log(getClass(), "writePlanet(Writer,Planet.PlanetaryEvent)", e); //$NON-NLS-1$
        }
    }
    
    public Planet.PlanetaryEvent readPlanetaryEvent(Node node) {
        try {
            return (Planet.PlanetaryEvent) unmarshaller.unmarshal(node);
        } catch (JAXBException e) {
            MekHQ.getLogger().log(getClass(), "readPlanetaryEvent(Node)", e); //$NON-NLS-1$
        }
        return null;
    }

    public void writePlanets(OutputStream out, List<Planet> planets) {
        LocalPlanetList temp = new LocalPlanetList();
        temp.list = planets;
        try {
            marshaller.marshal(temp, out);
        } catch (Exception e) {
            MekHQ.getLogger().log(getClass(), "writePlanets(OutputStream,List<Planet>)", e); //$NON-NLS-1$
        }
    }
    
    // Data loading methods
    
    private void initialize() {
        try {
            generatePlanets();
        } catch (ParseException e) {
            MekHQ.getLogger().log(getClass(), "initialize()", e); //$NON-NLS-1$
        }
    }
    
    private void done() {
        initialized = true;
        initializing = false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void updatePlanets(FileInputStream source) {
        final String METHOD_NAME = "updatePlanets(FileInputStream)"; //$NON-NLS-1$
        // JAXB unmarshaller closes the stream it doesn't own. Bad JAXB. BAD.
        try(InputStream is = new FilterInputStream(source) {
                @Override
                public void close() { /* ignore */ }
            }) {
            // Reset the file stream
            source.getChannel().position(0);

            LocalPlanetList planets = unmarshaller.unmarshal(
                    new StreamSource(is), LocalPlanetList.class).getValue();

            // Run through the list again, this time creating and updating planets as we go
            for( Planet planet : planets.list ) {
                Planet oldPlanet = planetList.get(planet.getId());
                if( null == oldPlanet ) {
                    planetList.put(planet.getId(), planet);
                } else {
                    // Update with new data
                    oldPlanet.copyDataFrom(planet);
                    planet = oldPlanet;
                }
            }
            
            // Process planet deletions
            for( String planetId : planets.toDelete ) {
                if( null != planetId ) {
                    planetList.remove(planetId);
                }
            }
        } catch (JAXBException e) {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, e);
        } catch(IOException e) {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, e);
        }
    }
    
    private void generatePlanets() throws DOMException, ParseException {
        generatePlanets("data/universe/planets", "data/universe/planets.xml");
    }
    
    /**
     * Worker function that loads a list of planets from the CSV file at the specified path.
     * @param csvPath
     * @return List of planets
     */
    public List<Planet> loadPlanetsFromTSV(String tsvPath) {
        final String METHOD_NAME = "loadPlanetsFromTSV()"; //NON-NLS-1$
        
        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO,
                "Starting load of planetary data from TSV: " + tsvPath); //$NON-NLS-1$
        
        List<Planet> planets = new ArrayList<>();
        
        // this is a dirty dirty hack to give specific dates to "waves" of
        // Operation Revival and Operation Bulldog, which occurred in 3050 and 3059 respectively
        // and resulted in a lot of rapid territorial changes
        Map<Integer, List<DateTime>> yearMonths = new HashMap<>();
        yearMonths.put(3050, new ArrayList<DateTime>());
        yearMonths.get(3050).add(new DateTime(3050, 3, 7, 0, 0, 0, 0)); // wave 1
        yearMonths.get(3050).add(new DateTime(3050, 5, 1, 0, 0, 0, 0)); // wave 2
        yearMonths.get(3050).add(new DateTime(3050, 6, 1, 0, 0, 0, 0)); // wave 3
        
        yearMonths.put(3059, new ArrayList<DateTime>());
        yearMonths.get(3059).add(new DateTime(3059, 5, 20, 0, 0, 0, 0)); // wave 1
        yearMonths.get(3059).add(new DateTime(3059, 6, 26, 0, 0, 0, 0)); // wave 2
        yearMonths.get(3059).add(new DateTime(3059, 8, 13, 0, 0, 0, 0)); // wave 3
        yearMonths.get(3059).add(new DateTime(3059, 9, 18, 0, 0, 0, 0)); // wave 4
        
        // this contains the current indices of the 3050/3059 maps
        Map<Integer, Integer> yearMonthIndex = new HashMap<>();
        yearMonthIndex.put(3050, 0);
        yearMonthIndex.put(3059, 0);
        
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(tsvPath), "UTF-8"));

            // expected file format (no spaces, just tab-separated)
            // "System" \t "X" \t "Y" \t Comma separated list of years
            // "Name" \t X-coordinate \t Y-coordinate \t "Ownership info".
            //      "Ownership info" breaks down to FactionCode, irrelevant stuff
            
            // parse the first line. Skip the first three items, then add all the rest to an array of DateTime objects
            String firstLine = br.readLine();
            String[] yearElements = firstLine.split("\t");
            List<DateTime> years = new ArrayList<>();
            for(int x = 3; x < yearElements.length; x++) { 
                // note that there are a couple of instances where years are listed multiple times
                // "Operation Revival" and "Operation Bulldog"
                // so we employ a dirty hack to get multiple dates within the same year,
                // since the SUCS data does not contain the actual dates
                DateTime year;
                
                int parsedYear = Integer.parseInt(yearElements[x]);
                if((parsedYear == 3050) || (parsedYear == 3059)) {
                    int yearIndex = yearMonthIndex.get(parsedYear);
                    
                    // get the datetime of the appropriate operation, then keep track of how many times we've done that
                    year = yearMonths.get(parsedYear).get(yearMonthIndex.get(parsedYear));

                    yearMonthIndex.put(parsedYear, ++yearIndex);
                } else {
                    year = new DateTime(Integer.parseInt(yearElements[x]), 1, 1, 0, 0, 0, 0);
                }
                
                years.add(year);
            }
            
            String currentLine = "";
            while(currentLine != null) {
                currentLine = br.readLine();
                if(currentLine == null || currentLine.trim().length() == 0) {
                    continue;
                }
                
                Planet p = new Planet(currentLine, years); // "this place crawls, sir!"
                planets.add(p);
            }
        } catch(Exception e) {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR, e);
        }
        
        return planets;
    }
    
    /**
     * A "slow" method that tries to find the target planet among the given list of planets by checking
     * if the target's name is a substring of one of the planets' names
     * @param target The planet to search for
     * @param planets The "list" of planets to search
     * @return Found planet from within the "list"
     */
    private Planet slowFind(Planet target, Map<String, Planet> planets) {
        for(String key : planets.keySet()) {
            if(key.contains(target.getId())) {
                return planets.get(key);
            }
        }
        
        return null;
    }
    
    /**
     * Handles importation of planets from a specifically-formatted tab-separated file to our planet data structure
     * @param tsvPath The path of the file
     */
    public String importPlanetsFromTSV(String tsvPath) {
        MekHQ.getLogger().log(getClass(), "importPlanetsFromTSV", LogLevel.INFO, "Import Planets From TSV");
        
        List<Planet> matchedImportPlanets = new ArrayList<>();
        List<Planet> matchedExistingPlanets = new ArrayList<>();
        List<Planet> unmatchedImportPlanets = new ArrayList<>();
        boolean dryRun = false;
        
        comparePlanetLists(tsvPath, matchedImportPlanets, matchedExistingPlanets, unmatchedImportPlanets);
        
        StringBuilder planetLog = new StringBuilder();
        for(int x = 0; x < matchedImportPlanets.size(); x++) {
            planetLog.append(matchedExistingPlanets.get(x).updateFromTSVPlanet(matchedImportPlanets.get(x), dryRun));
        }
        
        for(Planet unmatched : unmatchedImportPlanets) {
            planetLog.append("Adding planet " + unmatched.getId() + "\r\n");
            
            if(!dryRun) {
                getInstance().addPlanet(unmatched);
            }
        }
        
        MekHQ.getLogger().log(getClass(), "importPlanetsFromTSV", LogLevel.INFO, "\r\n" + planetLog.toString());
        String report = "Planet import: " + matchedImportPlanets.size() + " planets in TSV found in current planet list.\r\n" +
                unmatchedImportPlanets.size() + " planets in TSV not found in and added to current planet list.\r\nSee log file for details.";
        
        return report;
    }
    
    /**
     * Performs a comparison
     * @param tsvPath
     * @param outMatchedImportPlanets
     * @param outMatchedExistingPlanets
     * @param outUnmatchedImportPlanets
     */
    public void comparePlanetLists(String tsvPath, 
            List<Planet> outMatchedImportPlanets, 
            List<Planet> outMatchedExistingPlanets, 
            List<Planet> outUnmatchedImportPlanets) {
        
        List<Planet> planets = loadPlanetsFromTSV(tsvPath);
        StringBuilder planetLog = new StringBuilder();
        String METHOD_NAME = "comparePlanetLists";
        
        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, planetLog.toString() + "Starting planet comparison.");
        
        int coordinateUpdates = 0;
        int unmatchedPlanets = 0;
        
        // this is pretty inefficient but it's not a frequent operation.
        List<Planet> unmatchedCSVPlanets = new ArrayList<Planet>();
        
        Map<String, Planet> unmatchedXMLPlanets = new HashMap<String, Planet>();
        for(String key : planetList.keySet()) {
            if(!key.startsWith("NP") &&
                    !key.startsWith("HL") &&
                    !key.startsWith("DPR") &&
                    !key.startsWith("TFS") &&
                    !key.startsWith("ER") &&
                    !key.startsWith("KC"))
            unmatchedXMLPlanets.put(key, planetList.get(key));
        }
                
        try {
            // in the first pass, we get planets directly by their full name
            for(Planet p : planets) {
                Planet existingPlanet = getPlanetById(p.getId());
                planetLog.setLength(0);    
                
                if(existingPlanet == null) {
                    unmatchedCSVPlanets.add(p);
                    //MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, planetLog.toString() + "Import planet " + p.getId() + " not found in existing data.");
                    continue;
                }
                
                unmatchedXMLPlanets.remove(existingPlanet.getId());
                outMatchedImportPlanets.add(p);
                outMatchedExistingPlanets.add(existingPlanet);
                
                // make note of planets which will get coordinate updates
                if(!p.getX().equals(existingPlanet.getX()) || 
                        !p.getY().equals(existingPlanet.getY())) {
                    coordinateUpdates++;
                }
            }

            // in the second pass through the remaining planets, we get them by "full substring match"
            for(Planet p : unmatchedCSVPlanets) {
                Planet existingPlanet = slowFind(p, unmatchedXMLPlanets);
                
                if(existingPlanet == null) {
                    for(PlanetaryEvent pe : p.getEvents()) {
                        if(pe.name != null) {
                            existingPlanet = getPlanetById(pe.name);
                            break;
                        }
                    }
                }
                
                if(existingPlanet != null) {
                    unmatchedXMLPlanets.remove(existingPlanet.getId());
                    
                    outMatchedImportPlanets.add(p);
                    outMatchedExistingPlanets.add(existingPlanet);
                    
                    if(!p.getX().equals(existingPlanet.getX()) || 
                            !p.getY().equals(existingPlanet.getY())) {
                        coordinateUpdates++;
                    }
                }
                else {
                    outUnmatchedImportPlanets.add(p);
                    unmatchedPlanets++;
                    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, planetLog.toString() + "Import planet " + p.getId() + " not found in existing data.");
                }
            }
            
            
        } catch(Exception e) {
            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR, e);
        }
        
        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO, coordinateUpdates + " coordinate updates, " + unmatchedPlanets + " planets not found, " + unmatchedXMLPlanets.size() + " xml planets not in csv");
    }
    
    /**
     * Adds a planet to the list of planets. Includes putting it into the planet grid.
     * @param planet The planet to add.
     */
    private void addPlanet(Planet planet) { 
        this.planetList.put(planet.getId(), planet);
        
        int x = (int)(planet.getX()/30.0);
        int y = (int)(planet.getY()/30.0);
        if (planetGrid.get(x) == null) {
            planetGrid.put(x, new HashMap<Integer, Set<Planet>>());
        }
        if (planetGrid.get(x).get(y) == null) {
            planetGrid.get(x).put(y, new HashSet<Planet>());
        }
        if( !planetGrid.get(x).get(y).contains(planet) ) {
            planetGrid.get(x).get(y).add(planet);
        }
    }
    
    public String exportPlanets(String fileName) {
        String report;
        
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            ArrayList<Planet> localPlanetList = new ArrayList<>(this.planetList.values());
            localPlanetList.sort(new Comparator<Planet>() {
                @Override
                public int compare(Planet arg0, Planet arg1) {
                    return arg0.getId().compareTo(arg1.getId());
                }});
            
            this.writePlanets(fos, localPlanetList);
            fos.flush();
            fos.close();
            
            report = localPlanetList.size() + " planets written to file.";
        } catch(IOException ioe) {
            MekHQ.getLogger().log(getClass(), "exportPlanets", LogLevel.INFO, "Error exporting planets to XML");
            report = "Error exporting planets. See log for details.";
        }
        
        return report;
    }
    
    private void generatePlanets(String planetsPath, String defaultFilePath) throws DOMException, ParseException {
        final String METHOD_NAME = "generatePlanets()"; //NON-NLS-1$
        
        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO,
                "Starting load of planetary data from XML..."); //$NON-NLS-1$
        long currentTime = System.currentTimeMillis();
        synchronized (LOADING_LOCK) {
            // Step 1: Initialize variables.
            if( null == planetList ) {
                planetList = new ConcurrentHashMap<>();
            }
            planetList.clear();
            if( null == planetGrid ) {
                planetGrid = new HashMap<>();
            }
            // Be nice to the garbage collector
            for( Map.Entry<Integer, Map<Integer, Set<Planet>>> planetGridColumn : planetGrid.entrySet() ) {
                for( Map.Entry<Integer, Set<Planet>> planetGridElement : planetGridColumn.getValue().entrySet() ) {
                    if( null != planetGridElement.getValue() ) {
                        planetGridElement.getValue().clear();
                    }
                }
                if( null != planetGridColumn.getValue() ) {
                    planetGridColumn.getValue().clear();
                }
            }
            planetGrid.clear();
            
            // Step 2: Read the default file
            try(FileInputStream fis = new FileInputStream(defaultFilePath)) { //$NON-NLS-1$
                updatePlanets(fis);
            } catch (Exception ex) {
                MekHQ.getLogger().log(getClass(), METHOD_NAME, ex);
            }
            
            // Step 3: Load all the xml files within the planets subdirectory, if it exists
            Utilities.parseXMLFiles(planetsPath, //$NON-NLS-1$
                    new FileParser() {
                        @Override
                        public void parse(FileInputStream is) {
                            updatePlanets(is);
                        }
                    });
            
            List<Planet> toRemove = new ArrayList<>();
            for (Planet planet : planetList.values()) {
                if((null == planet.getX()) || (null == planet.getY())) {
                    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                            String.format("Planet \"%s\" is missing coordinates", planet.getId())); //$NON-NLS-1$
                    toRemove.add(planet);
                    continue;
                }
                int x = (int)(planet.getX()/30.0);
                int y = (int)(planet.getY()/30.0);
                if (planetGrid.get(x) == null) {
                    planetGrid.put(x, new HashMap<Integer, Set<Planet>>());
                }
                if (planetGrid.get(x).get(y) == null) {
                    planetGrid.get(x).put(y, new HashSet<Planet>());
                }
                if( !planetGrid.get(x).get(y).contains(planet) ) {
                    planetGrid.get(x).get(y).add(planet);
                }
            }
            for(Planet planet : toRemove) {
                planetList.remove(planet.getId());
            }
            done();
        }
        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO,
                String.format(Locale.ROOT,
                        "Loaded a total of %d planets in %.3fs.", //$NON-NLS-1$
                        planetList.size(), (System.currentTimeMillis() - currentTime) / 1000.0));
        // Planetary sanity check time!
        for(Planet planet : planetList.values()) {
            List<Planet> veryClosePlanets = getNearbyPlanets(planet, 1);
            if(veryClosePlanets.size() > 1) {
                for(Planet closePlanet : veryClosePlanets) {
                    if(!planet.getId().equals(closePlanet.getId())) {
                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.WARNING,
                                String.format(Locale.ROOT,
                                        "Extremly close planets detected. Data error? %s <-> %s: %.3f ly", //$NON-NLS-1$
                                        planet.getId(), closePlanet.getId(), planet.getDistanceTo(closePlanet)));
                    }
                }
            }
        }
    }
    
    @XmlRootElement(name="planets")
    private static final class LocalPlanetList {
        @XmlElement(name="planet")
        public List<Planet> list;
        
        @XmlTransient
        public List<String> toDelete;
        
        @SuppressWarnings("unused")
        private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
            toDelete = new ArrayList<String>();
            if( null == list ) {
                list = new ArrayList<Planet>();
            } else {
                // Fill in the "toDelete" list
                List<Planet> filteredList = new ArrayList<Planet>(list.size());
                for( Planet planet : list ) {
                    if( null != planet.delete && planet.delete && null != planet.getId() ) {
                        toDelete.add(planet.getId());
                    } else {
                        filteredList.add(planet);
                    }
                }
                list = filteredList;
            }
        }
    }
    
    /** A data class representing a HPG link between two planets */
    public static final class HPGLink {
        /** In case of HPG-A to HPG-B networks, <code>primary</code> holds the HPG-A node. Else the order doesn't matter. */
        public final Planet primary;
        public final Planet secondary;
        public final int rating;
        
        public HPGLink(Planet primary, Planet secondary, int rating) {
            this.primary = primary;
            this.secondary = secondary;
            this.rating = rating;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(primary, secondary, rating);
        }
        
        @Override
        public boolean equals(Object obj) {
            if(this == obj) {
                return true;
            }
            if((null == obj) || (getClass() != obj.getClass())) {
                return false;
            }
            final HPGLink other = (HPGLink) obj;
            return Objects.equals(primary, other.primary) && Objects.equals(secondary, other.secondary)
                && (rating == other.rating);
        }
    }

}