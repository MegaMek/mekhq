/*
 * Copyright (c) 2018 - The MegaMek Team
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.annotations.Nullable;
import megamek.common.logging.LogLevel;
import mekhq.MekHQ;

/**
 * @author Neoancient
 *
 */
public class FactionHints {

    private static final String FACTION_HINTS_FILE = "data/universe/factionhints.xml"; //$NON-NLS-1$

    private final Set<Faction> deepPeriphery;
    private final Set<Faction> neutralFactions;
    private final Set<Faction> majorPowers;
    
    private final Map<Faction, Map<Faction, List<FactionHint>>> wars;
    private final Map<Faction, Map<Faction, List<FactionHint>>> alliances;
    private final Map<Faction, Map<Faction, List<FactionHint>>> rivals;
    private final Map<Faction, Map<Faction, List<FactionHint>>> neutralExceptions;
    private final Map<Faction, Map<Faction, List<AltLocation>>> containedFactions;
    
    /**
     * Protected constructor that initializes empty data structures.
     */
    protected FactionHints() {
        deepPeriphery = new HashSet<>();
        neutralFactions = new HashSet<>();
        majorPowers = new HashSet<>();
        wars = new HashMap<>();
        alliances = new HashMap<>();
        rivals = new HashMap<>();
        neutralExceptions = new HashMap<>();
        containedFactions = new HashMap<>();
    }
    
    /**
     * Factory that loads the default data
     * @return
     */
    public static FactionHints defaultFactionHints() {
        FactionHints hints = new FactionHints();
        hints.loadData();
        return hints;
    }
    
    private void loadData() {
        try {
            loadFactionHints();
        } catch (DOMException e) {
            MekHQ.getLogger().log(getClass(), "loadData()", e); //$NON-NLS-1$
        } catch (ParseException e) {
            MekHQ.getLogger().log(getClass(), "loadData()", e); //$NON-NLS-1$
        }
    }
    
    /**
     * Accounts for non-existent factions that are used to indicate special status of the planet
     * (undiscovered, abandoned).
     * 
     * @param f
     * @return  Whether the faction is not a true faction
     */
    public static boolean isEmptyFaction(Faction f) {
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
        return majorPowers.contains(f);
    }
    
    /**
     * @param f 
     * @return  Whether the faction is located in the deep periphery
     */
    public boolean isDeepPeriphery(Faction f) {
        return deepPeriphery.contains(f);
    }
    
    /**
     * @param f1
     * @param f2
     * @return  Whether the factions are allies
     */
    public boolean isAlliedWith(Faction f1, Faction f2, Date date) {
        return hintApplies(alliances, f1, f2, date);
    }
    
    /**
     * @param f1
     * @param f2
     * @return  Whether the factions are rivals
     */
    public boolean isRivalOf(Faction f1, Faction f2, Date date) {
        return hintApplies(rivals, f1, f2, date);
    }
    
    /**
     * @param f1
     * @param f2
     * @return  Whether the factions are at war on the given date
     */
    public boolean isAtWarWith(Faction f1, Faction f2, Date date) {
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
    @Nullable public String getCurrentWar(Faction f1, Faction f2, Date date) {
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
    public boolean isNeutral(Faction faction) {
        return neutralFactions.contains(faction);
    }
    
    /**
     * Indicates a faction is neutral toward a particular potential opponent. Factions that are generally
     * non-combatant may have certain factions that are exceptions (like pirates) or have particular
     * periods where they go to war despite their normal non-combative nature.
     * 
     * @param faction  A potentially neutral faction
     * @param opponent A possible opponent
     * @param date     The campaign date
     * @return         true if the potential opponent should not be considered as an enemy
     */
    public boolean isNeutral(Faction faction, Faction opponent, Date date) {
        return neutralFactions.contains(faction)
                && !hintApplies(neutralExceptions, faction, opponent, date)
                && !isAtWarWith(faction, opponent, date);
    }
    
    private boolean hintApplies(Map<Faction, Map<Faction, List<FactionHint>>> hints,
                Faction f1, Faction f2, Date date) {
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

    /**
     * Some factions are present within the borders of another and do not have any planets that are considered
     * theirs, but still participate in military action. This includes Clan Wolf-in-Exile, the abjured
     * Nova Cats, and the other Inner Sphere powers which operated in the Draconis Combine during
     * Operation Bulldog.
     *  
     * @param f     A potential host faction
     * @param date  The campaign date
     * @return      A Set of all factions (if any) contained within the borders of the host faction.
     */
    public Set<Faction> getContainedFactions(Faction f, Date date) {
        HashSet<Faction> retval = new HashSet<>();
        if (containedFactions.get(f) != null) {
            for (Faction f2 : containedFactions.get(f).keySet()) {
                for (AltLocation l : containedFactions.get(f).get(f2)) {
                    if (date.after(l.start) && date.before(l.end)) {
                        retval.add(f2);
                    }
                }
            }
        }
        return retval;
    }
    
    /**
     * @param contained  A faction that is potentially hosted within the borders of another,
     *                   with no planets directly controlled.
     * @param date       The campaign date.
     * @return           The faction that controls the planets where the contained faction is positioned,
     *                   or {@code null} if the faction is not contained within another at the time.
     */
    @Nullable public Faction getContainedFactionHost(Faction contained, Date date) {
        for (Faction f : containedFactions.keySet()) {
            List<AltLocation> locs = containedFactions.get(f).get(contained);
            if (null != locs) {
                for (AltLocation loc : locs) {
                    if (date.after(loc.start) && date.before(loc.end)) {
                        return f;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Designates the proportion of space a contained faction takes up within the borders of the host
     * 
     * @param host      The host faction
     * @param contained The contained faction
     * @param date      The campaign date
     * @return          The ratio of space taken up by the contained faction to that of the host.
     */
    public double getAltLocationFraction(Faction host, Faction contained, Date date) {
        if (containedFactions.get(host) != null && containedFactions.get(host).get(contained) != null) {
            for (AltLocation l : containedFactions.get(host).get(contained)) {
                if (date.after(l.start) && date.before(l.end)) {
                    return l.fraction;
                }
            }
        }
        return 0.0;
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
    public boolean isContainedFactionOpponent(Faction outer, Faction inner,
            Faction opponent, Date date) {
        if (containedFactions.get(outer) != null && containedFactions.get(outer).get(inner) != null) {
            for (AltLocation l : containedFactions.get(outer).get(inner)) {
                if (date.after(l.start) && date.before(l.end)) {
                    if (l.opponents == null) {
                        return !inner.equals(opponent) ||
                                hintApplies(wars, inner, inner, date);
                    }
                    for (Faction f : l.opponents) {
                        if (f.equals(opponent)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
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
                    String fKey = wn.getAttributes().getNamedItem("faction").getTextContent().trim();
                    Faction f = Faction.getFaction(fKey);
                    if (null != f) {
                        neutralFactions.add(f);
                        addNeutralExceptions(f, wn);
                    } else {
                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                                "Invalid faction code in factionhints.xml: " + f); //$NON-NLS-1$
                    }
                } else if (nodeName.equalsIgnoreCase("deepPeriphery")) {
                    for (String fKey : wn.getTextContent().trim().split(",")) {
                        Faction f = Faction.getFaction(fKey);
                        if (null != fKey) {
                            deepPeriphery.add(f);
                        } else {
                            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                                    "Invalid faction code in factionhints.xml: " + f); //$NON-NLS-1$
                        }
                    }
                } else if (nodeName.equalsIgnoreCase("majorPowers")) {
                    for (String fKey : wn.getTextContent().trim().split(",")) {
                        Faction f = Faction.getFaction(fKey);
                        if (null != f) {
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
                    Faction outer = null;
                    Faction inner = null;
                    List<Faction> opponents = null;
                    if (wn.getAttributes().getNamedItem("start") != null) {
                        start = df.parse(wn.getAttributes().getNamedItem("start").getTextContent().trim());
                    }
                    if (wn.getAttributes().getNamedItem("end") != null) {
                        end = df.parse(wn.getAttributes().getNamedItem("end").getTextContent().trim());
                    }
                    for (int j = 0; j < wn.getChildNodes().getLength(); j++) {
                        Node wn2 = wn.getChildNodes().item(j);
                        if (wn2.getNodeName().equalsIgnoreCase("outer")) {
                            outer = Faction.getFaction(wn2.getTextContent().trim());
                        } else if (wn2.getNodeName().equalsIgnoreCase("inner")) {
                            inner = Faction.getFaction(wn2.getTextContent().trim());
                        } else if (wn2.getNodeName().equalsIgnoreCase("fraction")) {
                            fraction = Double.parseDouble(wn2.getTextContent().trim());
                        } else if (wn2.getNodeName().equalsIgnoreCase("opponents")) {
                            opponents = new ArrayList<>();
                            for (String fKey : wn2.getTextContent().trim().split(",")) {
                                Faction f = Faction.getFaction(fKey);
                                if (null != f) {
                                    opponents.add(f);
                                }
                            }
                        }
                    }
                    if ((null != outer) && (null != inner)) {
                        if (containedFactions.get(outer) == null) {
                            containedFactions.put(outer, new HashMap<>());
                        }
                        if (containedFactions.get(outer).get(inner) == null) {
                            containedFactions.get(outer).put(inner, new ArrayList<>());
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
    
    private void setFactionHint(Map<Faction, Map<Faction, List<FactionHint>>> hint,
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
                    final Faction f1 = Faction.getFaction(parties[i]);
                    if (null == f1) {
                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                                "Invalid faction code in factionhints.xml: " + parties[i]); //$NON-NLS-1$
                        continue;
                    }
                    for (int j = i + 1; j < parties.length; j++) {
                        final Faction f2 = Faction.getFaction(parties[j]);
                        if (null == f2) {
                            MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                                    "Invalid faction code in factionhints.xml: " + parties[j]); //$NON-NLS-1$
                            continue;
                        }
                        hint.putIfAbsent(f1, new HashMap<>());
                        hint.get(f1).putIfAbsent(f2, new ArrayList<>());
                        hint.get(f1).get(f2).add(new FactionHint(name, localStart, localEnd));
                    }
                }
            }
        }   
    }
    
    private void addNeutralExceptions(Faction faction, Node node) throws DOMException, ParseException {
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
                    final Faction f = Faction.getFaction(parties[i]);
                    if (null == f) {
                        MekHQ.getLogger().log(getClass(), METHOD_NAME, LogLevel.ERROR,
                                "Invalid faction code in factionhints.xml: " + parties[i]); //$NON-NLS-1$
                        continue;
                    }
                    neutralExceptions.putIfAbsent(faction, new HashMap<>());
                    neutralExceptions.get(faction).putIfAbsent(f, new ArrayList<>());
                    neutralExceptions.get(faction).get(f).add(new FactionHint(faction.getShortName(),
                            localStart, localEnd));
                }
            }
        }   
    }
    
    static class FactionHint {
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

    static class AltLocation extends FactionHint {
        public double fraction;
        public List<Faction> opponents;
            
        public AltLocation (Date s, Date e, double f, List<Faction> opponents) {
            super(null, s, e);
            fraction = f;
            if (null != opponents) {
                this.opponents = new ArrayList<>(opponents);
            }
        }       
    }
}
