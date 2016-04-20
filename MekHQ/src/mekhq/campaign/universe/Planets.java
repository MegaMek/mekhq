package mekhq.campaign.universe;

import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

import mekhq.FileParser;
import mekhq.MekHQ;
import mekhq.Utilities;

public class Planets {
    private final static Object LOADING_LOCK = new Object[0];
    
    private boolean initialized = false;
    private boolean initializing = false;
    private static Planets planets;
    private static ConcurrentMap<String, Planet> planetList = new ConcurrentHashMap<>();
    /*organizes systems into a grid of 30lyx30ly squares so we can find
     * nearby systems without iterating through the entire planet list*/
    private static HashMap<Integer, Map<Integer, Set<Planet>>> planetGrid = new HashMap<>();
    
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
    
    private Thread loader;

    private Planets() {}

    private static Set<Planet> getPlanetGrid(int x, int y) {
        if( !planetGrid.containsKey(x) ) {
            return null;
        }
        return planetGrid.get(x).get(y);
    }
    
    public static List<Planet> getNearbyPlanets(final Planet planet, int distance) {
        List<Planet> neighbors = new ArrayList<>();
        int gridRadius = (int)Math.ceil(distance / 30.0);
        int gridX = (int)(planet.getX() / 30.0);
        int gridY = (int)(planet.getY() / 30.0);
        for (int x = gridX - gridRadius; x <= gridX + gridRadius; x++) {
            for (int y = gridY - gridRadius; y <= gridY + gridRadius; y++) {
                Set<Planet> grid = getPlanetGrid(x, y);
                if(null != grid) {
                    for(Planet p : grid) {
                        if(p.getDistanceTo(planet) <= distance) {
                            neighbors.add(p);
                        }
                    }
                }
            }
        }
        Collections.sort(neighbors, new Comparator<Planet>() {
            @Override
            public int compare(Planet o1, Planet o2) {
                return Double.compare(planet.getDistanceTo(o1), planet.getDistanceTo(o2));
            }
        });
        return neighbors;
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

    private void initialize() {
        try {
            generatePlanets();
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public ConcurrentMap<String, Planet> getPlanets() {
        return planetList;
    }
    
    public Planet getPlanetById(String id) {
        return( null != id ? planetList.get(id) : null);
    }
    
    public List<NewsItem> getPlanetaryNews(DateTime when) {
        List<NewsItem> news = new ArrayList<>();
        for(Planet planet : planetList.values()) {
            if(null != planet) {
                Planet.PlanetaryEvent event = planet.getEvent(when);
                if((null != event) && (null != event.message)) {
                    NewsItem item = new NewsItem();
                    item.setHeadline(event.message);
                    item.setDate(event.date.toDate());
                    item.setLocation(planet.getPrintableName(when));
                    news.add(item);
                }
            }
        }
        return news;
    }
    
    private void done() {
        initialized = true;
        initializing = false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void updatePlanets(FileInputStream source) {
        try {
            // JAXB unmarshaller closes the stream it doesn't own. Bad JAXB. BAD.
            InputStream is = new FilterInputStream(source) {
                @Override
                public void close() { /* ignore */ }
            };
            
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
    
    public void generatePlanets() throws DOMException, ParseException {
        MekHQ.logMessage("Starting load of planetary data from XML...");
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
            try(FileInputStream fis = new FileInputStream("data/universe/planets.xml")) {
                updatePlanets(fis);
            } catch (Exception ex) {
                MekHQ.logError(ex);
            }
            
            // Step 3: Load all the xml files within the planets subdirectory, if it exists
            Utilities.parseXMLFiles("data/universe/planets",
                    new FileParser() {
                        @Override
                        public void parse(FileInputStream is) {
                            updatePlanets(is);
                        }
                    });
            
            for (Planet s : planetList.values()) {
                int x = (int)(s.getX()/30.0);
                int y = (int)(s.getY()/30.0);
                if (planetGrid.get(x) == null) {
                    planetGrid.put(x, new HashMap<Integer, Set<Planet>>());
                }
                if (planetGrid.get(x).get(y) == null) {
                    planetGrid.get(x).put(y, new HashSet<Planet>());
                }
                if( !planetGrid.get(x).get(y).contains(s) ) {
                    planetGrid.get(x).get(y).add(s);
                }
            }
            done();
        }
        MekHQ.logMessage(String.format("Loaded a total of %d planets in %.2fs.",
                planetList.size(), (System.currentTimeMillis() - currentTime) / 1000.0));
    }
    
    public void writePlanet(OutputStream out, Planet planet) {
        try {
            marshaller.marshal(planet, out);
        } catch (Exception e) {
            MekHQ.logError(e);
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
}