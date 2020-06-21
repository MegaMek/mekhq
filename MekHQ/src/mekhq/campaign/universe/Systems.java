/*
 * Copyright (C) 2011-2016 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.universe;

import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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
import java.util.function.Consumer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;

import megamek.common.EquipmentType;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.Utilities;

/**
 * This will eventually replace the Planets object as our source of system/planetary information
 * using the new XML format that places planets within systems
 * @author Taharqa
 */

public class Systems {
    private final static Object LOADING_LOCK = new Object[0];
    private static Systems systems;

    //private static ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.Planets", new EncodeControl()); //$NON-NLS-1$

 // Marshaller / unmarshaller instances
    private static Marshaller marshaller;
    private static Unmarshaller unmarshaller;
    static {
        try {
            JAXBContext context = JAXBContext.newInstance(LocalSystemList.class, Planet.class);
            marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            unmarshaller = context.createUnmarshaller();
            // For debugging only!
            // unmarshaller.setEventHandler(new javax.xml.bind.helpers.DefaultValidationEventHandler());
        } catch (JAXBException e) {
            MekHQ.getLogger().error(Systems.class, "<init>", e); //$NON-NLS-1$
        }
    }

    private static Marshaller planetMarshaller;
    private static Unmarshaller planetUnmarshaller;
    static {
        try {
            //creating the JAXB context
            JAXBContext jContext = JAXBContext.newInstance(Planet.class);
            planetMarshaller = jContext.createMarshaller();
            planetMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            planetMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            planetUnmarshaller = jContext.createUnmarshaller();
        } catch (Exception e) {
            MekHQ.getLogger().error(Systems.class, "Systems", e);
        }
    }

    public static Systems getInstance() {
        if (systems == null) {
            systems = new Systems();
        }
        if (!systems.initialized && !systems.initializing) {
            systems.initializing = true;
            systems.loader = new Thread(() -> systems.initialize(), "Planet Loader");
            systems.loader.setPriority(Thread.NORM_PRIORITY - 1);
            systems.loader.start();
        }
        return systems;
    }

    private ConcurrentMap<String, PlanetarySystem> systemList = new ConcurrentHashMap<>();
    /* organizes systems into a grid of 30lyx30ly squares so we can find
     * nearby systems without iterating through the entire planet list. */
    private HashMap<Integer, Map<Integer, Set<PlanetarySystem>>> systemGrid = new HashMap<>();

    // HPG Network cache (to not recalculate all the damn time)
    private Collection<Systems.HPGLink> hpgNetworkCache = null;
    private LocalDate hpgNetworkCacheDate = null;

    private Thread loader;
    private boolean initialized = false;
    private boolean initializing = false;

    private Systems() {}

    private Set<PlanetarySystem> getSystemGrid(int x, int y) {
        if (!systemGrid.containsKey(x)) {
            return null;
        }
        return systemGrid.get(x).get(y);
    }

    /** Return the planet by given name at a given time point */
    public PlanetarySystem getSystemByName(String name, LocalDate when) {
        if (null == name) {
            return null;
        }
        name = name.toLowerCase(Locale.ROOT);
        for (PlanetarySystem system : systemList.values()) {
            if (null != system) {
                String systemName = system.getName(when);
                if ((null != systemName) && systemName.toLowerCase(Locale.ROOT).equals(name)) {
                    return system;
                }
            }
        }
        return null;
    }

    public List<PlanetarySystem> getNearbySystems(final double centerX, final double centerY, int distance) {
         List<PlanetarySystem> neighbors = new ArrayList<>();

         visitNearbySystems(centerX, centerY, distance, neighbors::add);

         neighbors.sort(Comparator.comparingDouble(o -> o.getDistanceTo(centerX, centerY)));
         return neighbors;
    }

    public List<PlanetarySystem> getNearbySystems(final PlanetarySystem system, int distance) {
        return getNearbySystems(system.getX(), system.getY(), distance);
    }

    public ConcurrentMap<String, PlanetarySystem> getSystems() {
        return systemList;
    }

    public PlanetarySystem getSystemById(String id) {
        return( null != id ? systemList.get(id) : null);
    }

    /**
     * Get a list of planetary systems within a certain jump radius (30ly per jump) that
     * you can shop on, sorted by number of jumps and in system transit time
     * @param system - current <code>PlanetarySystem</code>
     * @param jumps - number of jumps out to look as an integer
     * @return a list of planets where you can go shopping
     */
    public List<PlanetarySystem> getShoppingSystems(final PlanetarySystem system, int jumps, LocalDate when) {
        List<PlanetarySystem> shoppingSystems = getNearbySystems(system, jumps*30);

        //remove dead planets
        Iterator<PlanetarySystem> iter = shoppingSystems.iterator();
        while (iter.hasNext()) {
            PlanetarySystem s = iter.next();
            if (null == s.getPrimaryPlanet() || s.getPrimaryPlanet().isEmpty(when)) {
                iter.remove();
            }
        }

        shoppingSystems.sort((p1, p2) -> {
            //sort first on number of jumps required
            int jump1 = (int) Math.ceil(p1.getDistanceTo(system) / 30.0);
            int jump2 = (int) Math.ceil(p2.getDistanceTo(system) / 30.0);
            int sComp = Integer.compare(jump1, jump2);

            if (sComp != 0) {
                return sComp;
            }

            //if number of jumps the same then sort on in system transit time
            return Double.compare(p1.getTimeToJumpPoint(1.0), p2.getTimeToJumpPoint(1.0));
        });

        return shoppingSystems;
    }

    public List<NewsItem> getPlanetaryNews(LocalDate when) {
        List<NewsItem> news = new ArrayList<>();
        for (PlanetarySystem system : systemList.values()) {
            if (null != system) {
                Planet.PlanetaryEvent event;
                for (Planet p : system.getPlanets()) {
                    event = p.getEvent(when);
                    if ((null != event) && (null != event.message)) {
                        NewsItem item = new NewsItem();
                        item.setHeadline(event.message);
                        item.setDate(event.date);
                        item.setLocation(p.getPrintableName(when));
                        news.add(item);
                    }
                }
            }
        }
        return news;
    }

    /** Clean up the local HPG network cache */
    public void recalcHPGNetwork() {
        hpgNetworkCacheDate = null;
    }

    public Collection<Systems.HPGLink> getHPGNetwork(LocalDate when) {
        if ((null != when) && when.equals(hpgNetworkCacheDate)) {
            return hpgNetworkCache;
        }

        Set<HPGLink> result = new HashSet<>();
        for (PlanetarySystem system : systemList.values()) {
            Integer hpg = system.getHPG(when);
            if ((null != hpg) && (hpg == EquipmentType.RATING_A)) {
                Collection<PlanetarySystem> neighbors = getNearbySystems(system, 50);
                for (PlanetarySystem neighbor : neighbors) {
                    hpg = neighbor.getHPG(when);
                    if (null != hpg) {
                        HPGLink link = new HPGLink(system, neighbor, hpg);
                        result.add(link);
                    }
                }
            }
        }
        hpgNetworkCache = result;
        hpgNetworkCacheDate = when;
        return result;
    }

// Data loading methods

    private void initialize() {
        try {
            generateSystems();
        } catch (ParseException e) {
            MekHQ.getLogger().error(getClass(), "initialize()", e); //$NON-NLS-1$
        }
    }

    private void done() {
        initialized = true;
        initializing = false;
    }

    public boolean isInitialized() {
        return initialized;
    }

    private void updateSystems(FileInputStream source) {
        final String METHOD_NAME = "updateSystems(FileInputStream)"; //$NON-NLS-1$
        // JAXB unmarshaller closes the stream it doesn't own. Bad JAXB. BAD.
        try (InputStream is = new FilterInputStream(source) {
                @Override
                public void close() {  }
            }) {
            // Reset the file stream
            source.getChannel().position(0);

            LocalSystemList systems = unmarshaller.unmarshal(
                    MekHqXmlUtil.createSafeXmlSource(is), LocalSystemList.class).getValue();

            // Run through the list again, this time creating and updating systems as we go
            for (PlanetarySystem system : systems.list) {
                PlanetarySystem oldSystem = systemList.get(system.getId());
                if (null == oldSystem) {
                    systemList.put(system.getId(), system);
                } else {
                    // Update with new data
                    oldSystem.copyDataFrom(system);
                    system = oldSystem;
                }
            }

            // Process system deletions
            for (String systemId : systems.toDelete) {
                if (null != systemId) {
                    systemList.remove(systemId);
                }
            }
        } catch (JAXBException e) {
            MekHQ.getLogger().error(getClass(), METHOD_NAME, e);
        } catch (IOException e) {
            MekHQ.getLogger().error(getClass(), METHOD_NAME, e);
        }
    }

    private void generateSystems() throws DOMException, ParseException {
        generateSystems("data/universe/planetary_systems", "data/universe/systems.xml");
    }

    private void generateSystems(String planetsPath, String defaultFilePath) throws DOMException, ParseException {
        final String METHOD_NAME = "generateSystems()"; //NON-NLS-1$

        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO,
                "Starting load of system data from XML..."); //$NON-NLS-1$
        long currentTime = System.currentTimeMillis();
        synchronized (LOADING_LOCK) {
            // Step 1: Initialize variables.
            if (null == systemList) {
                systemList = new ConcurrentHashMap<>();
            }
            systemList.clear();
            if (null == systemGrid) {
                systemGrid = new HashMap<>();
            }
            // Be nice to the garbage collector
            for (Map.Entry<Integer, Map<Integer, Set<PlanetarySystem>>> systemGridColumn : systemGrid.entrySet()) {
                for (Map.Entry<Integer, Set<PlanetarySystem>> systemGridElement : systemGridColumn.getValue().entrySet()) {
                    if (null != systemGridElement.getValue()) {
                        systemGridElement.getValue().clear();
                    }
                }
                if (null != systemGridColumn.getValue()) {
                    systemGridColumn.getValue().clear();
                }
            }
            systemGrid.clear();

            // Step 2: Read the default file
            try (FileInputStream fis = new FileInputStream(defaultFilePath)) { //$NON-NLS-1$
                updateSystems(fis);
            } catch (Exception ex) {
                MekHQ.getLogger().error(getClass(), METHOD_NAME, ex);
            }

            // Step 3: Load all the xml files within the planets subdirectory, if it exists
            Utilities.parseXMLFiles(planetsPath, this::updateSystems);

            List<PlanetarySystem> toRemove = new ArrayList<>();
            for (PlanetarySystem system : systemList.values()) {
                if ((null == system.getX()) || (null == system.getY())) {
                    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                            String.format("System \"%s\" is missing coordinates", system.getId())); //$NON-NLS-1$
                    toRemove.add(system);
                    continue;
                }
                //make sure the primary slot is not larger than the number of planets
                if (system.getPrimaryPlanetPosition() > system.getPlanets().size()) {
                    MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                            String.format("System \"%s\" has a primary slot greater than the number of planets", system.getId())); //$NON-NLS-1$
                    toRemove.add(system);
                    continue;
                }
                int x = (int) (system.getX() / 30.0);
                int y = (int) (system.getY() / 30.0);
                systemGrid.computeIfAbsent(x, k -> new HashMap<>());
                systemGrid.get(x).computeIfAbsent(y, k -> new HashSet<>());
                systemGrid.get(x).get(y).add(system);
            }
            for (PlanetarySystem system : toRemove) {
                systemList.remove(system.getId());
            }
            done();
        }
        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.INFO,
                String.format(Locale.ROOT,
                        "Loaded a total of %d systems in %.3fs.", //$NON-NLS-1$
                        systemList.size(), (System.currentTimeMillis() - currentTime) / 1000.0));
        // Planetary sanity check time!
        for (PlanetarySystem system : systemList.values()) {
            List<PlanetarySystem> veryCloseSystems = getNearbySystems(system, 1);
            if (veryCloseSystems.size() > 1) {
                for (PlanetarySystem closeSystem : veryCloseSystems) {
                    if (!system.getId().equals(closeSystem.getId())) {
                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.WARNING,
                                String.format(Locale.ROOT,
                                        "Extremely close systems detected. Data error? %s <-> %s: %.3f ly", //$NON-NLS-1$
                                        system.getId(), closeSystem.getId(), system.getDistanceTo(closeSystem)));
                    }
                }
            }
        }
    }

    @XmlRootElement(name="systems")
    private static final class LocalSystemList {
        @XmlElement(name="system")
        public List<PlanetarySystem> list;

        @XmlTransient
        public List<String> toDelete;

        @SuppressWarnings("unused")
        private void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
            toDelete = new ArrayList<>();
            if (null == list) {
                list = new ArrayList<>();
            } else {
                // Fill in the "toDelete" list
                List<PlanetarySystem> filteredList = new ArrayList<>(list.size());
                for (PlanetarySystem system : list) {
                    if ((null != system.delete) && system.delete && (null != system.getId())) {
                        toDelete.add(system.getId());
                    } else {
                        filteredList.add(system);
                    }
                }
                list = filteredList;
            }
        }
    }

    /** A data class representing a HPG link between two planets */
    public static final class HPGLink {
        /** In case of HPG-A to HPG-B networks, <code>primary</code> holds the HPG-A node. Else the order doesn't matter. */
        public final PlanetarySystem primary;
        public final PlanetarySystem secondary;
        public final int rating;

        public HPGLink(PlanetarySystem primary, PlanetarySystem secondary, int rating) {
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
            if (this == obj) {
                return true;
            }
            if ((null == obj) || (getClass() != obj.getClass())) {
                return false;
            }
            final HPGLink other = (HPGLink) obj;
            return Objects.equals(primary, other.primary) && Objects.equals(secondary, other.secondary)
                && (rating == other.rating);
        }
    }

    public void visitNearbySystems(final double centerX, final double centerY, final int distance, Consumer<PlanetarySystem> visitor) {
        int gridRadius = (int) Math.ceil(distance / 30.0);
        int gridX = (int) (centerX / 30.0);
        int gridY = (int) (centerY / 30.0);
        for (int x = gridX - gridRadius; x <= gridX + gridRadius; x++) {
            for (int y = gridY - gridRadius; y <= gridY + gridRadius; y++) {
                Set<PlanetarySystem> grid = getSystemGrid(x, y);
                if (null != grid) {
                    for (PlanetarySystem p : grid) {
                        if (p.getDistanceTo(centerX, centerY) <= distance) {
                            visitor.accept(p);
                        }
                    }
                }
            }
        }
    }

    public void visitNearbySystems(final PlanetarySystem system, final int distance, Consumer<PlanetarySystem> visitor) {
        visitNearbySystems(system.getX(), system.getY(), distance, visitor);
    }

    /**
     * Write out a planetary event to XML
     * @param out - the <code>Writer</code>
     * @param event - the <code>PlanetaryEvent</code> to write
     */
    public void writePlanetaryEvent(Writer out, Planet.PlanetaryEvent event) {
        try {
            planetMarshaller.marshal(event, out);
        } catch (Exception e) {
            MekHQ.getLogger().error(getClass(), "writePlanet(Writer,Planet.PlanetaryEvent)", e); //$NON-NLS-1$
        }
    }

    /**
     * Write out planetary system-wide event to XML
     * @param out - the <code>Writer</code>
     * @param event - the <code>PlanetarySystemEvent</code> to write
     */
    public void writePlanetarySystemEvent(Writer out, PlanetarySystem.PlanetarySystemEvent event) {
        try {
            marshaller.marshal(event, out);
        } catch (Exception e) {
            MekHQ.getLogger().error(getClass(), "writePlanet(Writer,Planet.PlanetaryEvent)", e); //$NON-NLS-1$
        }
    }

    /**
     * This is a legacy function to read custom planetary events from before the switch
     * to PlanetarySystems
     * @param node - xml node
     * @return PlanetaryEvent class from Planet
     */
    public Planet.PlanetaryEvent readPlanetaryEvent(Node node) {
        try {
            return (Planet.PlanetaryEvent) planetUnmarshaller.unmarshal(node);
        } catch (JAXBException e) {
            MekHQ.getLogger().error(getClass(), "readPlanetaryEvent(Node)", e); //$NON-NLS-1$
        }
        return null;
    }

    /**
     * This function will read in system wide events from XML and apply them. It is designed
     * for allowind custom events
     * @param node - xml node
     * @return PlanetaryEvent class from Planet
     */
    public PlanetarySystem.PlanetarySystemEvent readPlanetarySystemEvent(Node node) {
        try {
            return (PlanetarySystem.PlanetarySystemEvent) unmarshaller.unmarshal(node);
        } catch (JAXBException e) {
            MekHQ.getLogger().error(getClass(), "readPlanetarySystemEvent(Node)", e); //$NON-NLS-1$
        }
        return null;
    }

    public static void reload(boolean waitForFinish) {
        systems = null;
        getInstance();
        if (waitForFinish) {
            try {
                while (!systems.isInitialized()) {
                    Thread.sleep(10);
                }
            } catch (InterruptedException iex) {
                MekHQ.getLogger().error(Systems.class, "reload(boolean)", iex); //$NON-NLS-1$
            }
        }
    }

    /** @return <code>true</code> if the planet was known and got updated, <code>false</code> otherwise */
    /*public boolean updatePlanetaryEvents(String id, Collection<Planet.PlanetaryEvent> events) {
        return updatePlanetaryEvents(id, events, false);
    }*/

    /**
     * This is a legacy function for updating planetary events before PlanetarySystem. it will
     * assume that the planet's events to be updated is the primary planet
     * @param id
     * @param events
     * @param replace
     * @return
     */
    public boolean updatePlanetaryEvents(String id, Collection<Planet.PlanetaryEvent> events, boolean replace) {
        //assume the primary planet
        PlanetarySystem system = getSystemById(id);
        if (null == system) {
            return false;
        }
        int pos = system.getPrimaryPlanetPosition();
        if (pos == 0) {
            return false;
        }
        return(updatePlanetaryEvents(id, events, replace, pos));
    }

    /** @return <code>true</code> if the planet was known and got updated, <code>false</code> otherwise */
    public boolean updatePlanetaryEvents(String id, Collection<Planet.PlanetaryEvent> events, boolean replace, int position) {
        PlanetarySystem system = getSystemById(id);
        if ((null == system) || (position < 1)) {
            return false;
        }
        if (null != events) {
            for (Planet.PlanetaryEvent event : events) {
                if (null != event.date) {
                    Planet.PlanetaryEvent planetaryEvent = system.getOrCreateEvent(event.date, position);
                    if (null == planetaryEvent) {
                        continue;
                    }
                    if (replace) {
                        planetaryEvent.replaceDataFrom(event);
                    } else {
                        planetaryEvent.copyDataFrom(event);
                    }
                }
            }
        }
        return true;
    }

    /**
     * updates system wide events from a collection of events
     * @param id - system id
     * @param events - collection of PlanetarySystemEvents
     * @param replace - should we replace existing events
     * @return <code>true</code> if the system was known and got updated, <code>false</code> otherwise
     */
    public boolean updatePlanetarySystemEvents(String id, Collection<PlanetarySystem.PlanetarySystemEvent> events, boolean replace) {
        PlanetarySystem system = getSystemById(id);
        if (null == system) {
            return false;
        }
        if (null != events) {
            for (PlanetarySystem.PlanetarySystemEvent event : events) {
                if (null != event.date) {
                    PlanetarySystem.PlanetarySystemEvent systemEvent = system.getOrCreateEvent(event.date);
                    if (null == systemEvent) {
                        continue;
                    }
                    if (replace) {
                        systemEvent.replaceDataFrom(event);
                    } else {
                        systemEvent.copyDataFrom(event);
                    }
                }
            }
        }
        return true;
    }
}
