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

import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import mekhq.FileParser;
import mekhq.MekHQ;
import mekhq.Utilities;

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
            MekHQ.logError(e);
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
                MekHQ.logError(iex);
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
            MekHQ.logError(e);
        }
    }
    
    public void writePlanet(Writer out, Planet planet) {
        try {
            marshaller.marshal(planet, out);
        } catch (Exception e) {
            MekHQ.logError(e);
        }
    }
    

    public void writePlanetaryEvent(OutputStream out, Planet.PlanetaryEvent event) {
        try {
            marshaller.marshal(event, out);
        } catch (Exception e) {
            MekHQ.logError(e);
        }
    }
    
    public void writePlanetaryEvent(Writer out, Planet.PlanetaryEvent event) {
        try {
            marshaller.marshal(event, out);
        } catch (Exception e) {
            MekHQ.logError(e);
        }
    }
    
    public Planet.PlanetaryEvent readPlanetaryEvent(Node node) {
        try {
            return (Planet.PlanetaryEvent) unmarshaller.unmarshal(node);
        } catch (JAXBException e) {
            MekHQ.logError(e);
        }
        return null;
    }

    public void writePlanets(OutputStream out, List<Planet> planets) {
        LocalPlanetList temp = new LocalPlanetList();
        temp.list = planets;
        try {
            marshaller.marshal(temp, out);
        } catch (Exception e) {
            MekHQ.logError(e);
        }
    }
    
    // Data loading methods
    
    private void initialize() {
        try {
            generatePlanets();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
            MekHQ.logError(e);
        } catch(IOException e) {
            MekHQ.logError(e);
        }
    }
    
    private void generatePlanets() throws DOMException, ParseException {
        MekHQ.logMessage("Starting load of planetary data from XML..."); //$NON-NLS-1$
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
            try(FileInputStream fis = new FileInputStream("data/universe/planets.xml")) { //$NON-NLS-1$
                updatePlanets(fis);
            } catch (Exception ex) {
                MekHQ.logError(ex);
            }
            
            // Step 3: Load all the xml files within the planets subdirectory, if it exists
            Utilities.parseXMLFiles("data/universe/planets", //$NON-NLS-1$
                    new FileParser() {
                        @Override
                        public void parse(FileInputStream is) {
                            updatePlanets(is);
                        }
                    });
            
            List<Planet> toRemove = new ArrayList<>();
            for (Planet planet : planetList.values()) {
                if((null == planet.getX()) || (null == planet.getY())) {
                    MekHQ.logError(String.format("Planet \"%s\" is missing coordinates", planet.getId())); //$NON-NLS-1$
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
        MekHQ.logMessage(String.format(Locale.ROOT,
                "Loaded a total of %d planets in %.3fs.", //$NON-NLS-1$
                planetList.size(), (System.currentTimeMillis() - currentTime) / 1000.0));
        // Planetary sanity check time!
        for(Planet planet : planetList.values()) {
            List<Planet> veryClosePlanets = getNearbyPlanets(planet, 1);
            if(veryClosePlanets.size() > 1) {
                for(Planet closePlanet : veryClosePlanets) {
                    if(!planet.getId().equals(closePlanet.getId())) {
                        MekHQ.logMessage(String.format(Locale.ROOT,
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