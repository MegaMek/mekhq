/*
 * Force.java
 *
 * Copyright (c) 2011 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.force;

import megamek.Version;
import megamek.common.annotations.Nullable;
import megamek.common.icons.Camouflage;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.icons.ForcePieceIcon;
import mekhq.campaign.icons.LayeredForceIcon;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.icons.enums.LayeredForceIconLayer;
import mekhq.campaign.icons.enums.LayeredForceIconOperationalStatus;
import mekhq.campaign.log.ServiceLogger;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.io.migration.CamouflageMigrator;
import mekhq.io.migration.ForceIconMigrator;
import mekhq.utilities.MHQXMLUtility;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is a hierarchical object to define forces for TO&amp;E. Each Force
 * object can have a parent force object and a vector of child force objects.
 * Each force can also have a vector of PilotPerson objects. The idea
 * is that any time TOE is refreshed in MekHQView, the force object can be traversed
 * to generate a set of TreeNodes that can be applied to the JTree showing the force
 * TO&amp;E.
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Force {
    //region Variable Declarations
    // pathway to force icon
    public static final int FORCE_NONE = -1;

    private String name;
    private StandardForceIcon forceIcon;
    private Camouflage camouflage;
    private String desc;
    private boolean combatForce;
    private FormationLevel formationLevel;
    private Force parentForce;
    private final Vector<Force> subForces;
    private final Vector<UUID> units;
    private int scenarioId;
    private UUID forceCommanderID;
    protected UUID techId;


    //an ID so that forces can be tracked in Campaign hash
    private int id;
    //endregion Variable Declarations

    //region Constructors
    public Force(String name) {
        setName(name);
        setForceIcon(new LayeredForceIcon());
        setCamouflage(new Camouflage());
        setDescription("");
        this.combatForce = true;
        this.formationLevel = FormationLevel.NONE;
        this.parentForce = null;
        this.subForces = new Vector<>();
        this.units = new Vector<>();
        this.scenarioId = -1;
    }
    //endregion Constructors

    public String getName() {
        return name;
    }

    public void setName(String n) {
        this.name = n;
    }

    public StandardForceIcon getForceIcon() {
        return forceIcon;
    }

    public void setForceIcon(final StandardForceIcon forceIcon) {
        setForceIcon(forceIcon, false);
    }

    public void setForceIcon(final StandardForceIcon forceIcon, final boolean setForSubForces) {
        this.forceIcon = forceIcon;
        if (setForSubForces) {
            for (final Force force : subForces) {
                force.setForceIcon(forceIcon.clone(), true);
            }
        }
    }

    public Camouflage getCamouflage() {
        return camouflage;
    }

    public Camouflage getCamouflageOrElse(final Camouflage camouflage) {
        return getCamouflage().hasDefaultCategory()
                ? ((getParentForce() == null ) ? camouflage : getParentForce().getCamouflageOrElse(camouflage))
                : getCamouflage();
    }

    public void setCamouflage(Camouflage camouflage) {
        this.camouflage = camouflage;
    }

    public String getDescription() {
        return desc;
    }

    public void setDescription(String d) {
        this.desc = d;
    }

    public boolean isCombatForce() {
        return combatForce;
    }

    public void setCombatForce(boolean combatForce, boolean setForSubForces) {
        this.combatForce = combatForce;
        if (setForSubForces) {
            for (Force force : subForces) {
                force.setCombatForce(combatForce, true);
            }
        }
    }

    public FormationLevel getFormationLevel() {
        return formationLevel;
    }

    public void setFormationLevel(final FormationLevel formationLevel) {
        this.formationLevel = formationLevel;
    }

    public int getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(int i) {
        this.scenarioId = i;
        for (Force sub : getSubForces()) {
            sub.setScenarioId(i);
        }
    }

    public void setTechID(UUID tech) {
        techId = tech;
    }

    public UUID getTechID() {
        return techId;
    }

    public boolean isDeployed() {
        //forces are deployed if their parent force is
        if ((null != parentForce) && parentForce.isDeployed()) {
            return true;
        }
        return scenarioId != -1;
    }

    public @Nullable Force getParentForce() {
        return parentForce;
    }

    public void setParentForce(final @Nullable Force parent) {
        this.parentForce = parent;
    }

    public Vector<Force> getSubForces() {
        return subForces;
    }

    public boolean isAncestorOf(Force otherForce) {
        Force pForce = otherForce.getParentForce();
        while (pForce != null) {
            if (pForce.getId() == getId()) {
                return true;
            }
            pForce = pForce.getParentForce();
        }
        return false;
    }

    /**
     * @return the full hierarchical name of the force, including all parents
     */
    public String getFullName() {
        String toReturn = getName();
        if (null != parentForce) {
            toReturn += ", " + parentForce.getFullName();
        }
        return toReturn;
    }

    /**
     * @return A String representation of the full hierarchical force including ID for MM export
     */
    public String getFullMMName() {
        var ancestors = new ArrayList<Force>();
        ancestors.add(this);
        var p = parentForce;
        while (p != null) {
            ancestors.add(p);
            p = p.parentForce;
        }

        StringBuilder result = new StringBuilder();
        int id = 0;
        for (int i = ancestors.size() - 1; i >= 0; i--) {
            Force ancestor = ancestors.get(i);
            id = 17 * id + ancestor.id + 1;
            result.append(ancestor.getName()).append("|").append(id);
            if (!ancestor.getCamouflage().isDefault()) {
                result.append("|").append(ancestor.getCamouflage().getCategory()).append("|").append(ancestor.getCamouflage().getFilename());
            }
            result.append("||");
        }
        return result.toString();
    }

    /**
     * Add a subforce to the subforce vector. In general, this
     * should not be called directly to add forces to the campaign
     * because they will not be assigned an id. Use {@link Campaign#addForce(Force, Force)}
     * instead
     * The boolean assignParent here is set to false when assigning forces from the
     * TOE to a scenario, because we don't want to switch this forces real parent
     * @param sub the subforce to add, which may be null from a load failure. This returns without
     *            adding in that case
     */
    public void addSubForce(final @Nullable Force sub, boolean assignParent) {
        if (sub == null) {
            return;
        }

        if (assignParent) {
            sub.setParentForce(this);
        }
        subForces.add(sub);
    }

    public Vector<UUID> getUnits() {
        return units;
    }

    /**
     * @param combatForcesOnly to only include combat forces or to also include non-combat forces
     * @return all the unit ids in this force and all of its subforces
     */
    public Vector<UUID> getAllUnits(boolean combatForcesOnly) {
        Vector<UUID> allUnits;
        if (combatForcesOnly && !isCombatForce()) {
            allUnits = new Vector<>();
        } else {
            allUnits = new Vector<>(units);
        }

        for (Force f : subForces) {
            allUnits.addAll(f.getAllUnits(combatForcesOnly));
        }
        return allUnits;
    }

    /**
     * Add a unit id to the units vector. In general, this
     * should not be called directly to add unid because they will
     * not be assigned a force id. Use {@link Campaign#addUnitToForce(Unit, int)}
     * instead
     * @param uid
     */
    public void addUnit(UUID uid) {
        addUnit(null, uid, false, null);
    }

    public void addUnit(Campaign campaign, UUID uid, boolean useTransfers, Force oldForce) {
        units.add(uid);

        if (campaign == null) {
            return;
        }

        Unit unit = campaign.getUnit(uid);
        if (unit != null) {
            for (Person person : unit.getCrew()) {
                if (useTransfers) {
                    ServiceLogger.reassignedTOEForce(campaign, person, campaign.getLocalDate(), oldForce, this);
                } else {
                    ServiceLogger.addedToTOEForce(campaign, person, campaign.getLocalDate(), this);
                }
            }
        }

        updateCommander(campaign);
    }

    /**
     * This should not be directly called except by {@link Campaign#removeUnitFromForce(Unit)}
     * instead
     * @param id
     */
    public void removeUnit(Campaign campaign, UUID id, boolean log) {
        int idx = 0;
        boolean found = false;
        for (UUID uid : getUnits()) {
            if (uid.equals(id)) {
                found = true;
                break;
            }
            idx++;
        }
        if (found) {
            units.remove(idx);

            if (log) {
                Unit unit = campaign.getUnit(id);
                if (unit != null) {
                    for (Person person : unit.getCrew()) {
                        ServiceLogger.removedFromTOEForce(campaign, person, campaign.getLocalDate(), this);
                    }
                }
            }

            updateCommander(campaign);
        }
    }

    public void clearScenarioIds(Campaign c) {
        clearScenarioIds(c, true);
    }

    public void clearScenarioIds(Campaign c, boolean killSub) {
        if (killSub) {
            for (UUID uid : getUnits()) {
                Unit u = c.getUnit(uid);
                if (null != u) {
                    u.undeploy();
                }
            }
            // We only need to clear the subForces if we're killing everything.
            for (Force sub : getSubForces()) {
                Scenario s = c.getScenario(sub.getScenarioId());
                if (s != null) {
                    s.removeForce(sub.getId());
                }
                sub.clearScenarioIds(c);
            }
        } else {
            // If we're not killing the units from the scenario, then we need to assign them with the
            // scenario ID and add them to the scenario.
            for (UUID uid : getUnits()) {
                c.getUnit(uid).setScenarioId(getScenarioId());
                c.getScenario(getScenarioId()).addUnit(uid);
            }
        }
        setScenarioId(-1);
    }

    public int getId() {
        return id;
    }

    public void setId(int i) {
        this.id = i;
    }

    public UUID getForceCommanderID() {
        return forceCommanderID;
    }

    public void setForceCommanderID(UUID commanderID) {
        forceCommanderID = commanderID;
    }

    public List<UUID> getEligibleCommanders(Campaign c) {
        List<UUID> people = new ArrayList<>();
        Person highestRankPerson = c.getPerson(getForceCommanderID());

        // safety check: if the person is no longer assigned to a unit or the force,
        // then they're not really the highest ranked person in the force.
        if ((highestRankPerson != null) &&
                ((highestRankPerson.getUnit() == null) ||
                (!getUnits().contains(highestRankPerson.getUnit().getId())))) {
            highestRankPerson = null;
        }

        for (UUID uid : getUnits()) {
            Unit u = c.getUnit(uid);
            if (null != u) {
                Person p = u.getCommander();
                if (null != p) {
                    // if we found someone with a higher rank, clear everything out and start again with the new highest rank person
                    if (p.outRanks(highestRankPerson)) {
                        people.clear();
                        people.add(p.getId());
                        highestRankPerson = p;
                    // if we are looking at someone with a lower rank, ignore them and move on
                    } else if ((highestRankPerson != null) && highestRankPerson.outRanks(p)) {
                        continue;
                    // someone with an equivalent rank can be commander
                    } else {
                        people.add(p.getId());
                    }
                }
            }
        }

        return people;
    }

    /**
     * Automatically update the force's commander
     */
    public void updateCommander(Campaign c) {
        List<UUID> eligibleCommanders = getEligibleCommanders(c);

        // logic: if we found someone eligible who is a higher rank, the first one of those becomes the new commander
        // otherwise, the existing commander remains the commander
        if (!eligibleCommanders.contains(getForceCommanderID()) && (eligibleCommanders.size() > 0)) {
            forceCommanderID = eligibleCommanders.get(0);
        }
    }

    public void removeSubForce(int id) {
        int idx = 0;
        boolean found = false;
        for (Force sforce : getSubForces()) {
            if (sforce.getId() == id) {
                found = true;
                break;
            }
            idx++;
        }
        if (found) {
            subForces.remove(idx);
        }
    }

    /**
     * This determines the proper operational status icon to use for this force and sets it.
     * @param campaign the campaign to determine the operational status of this force using
     * @return a list of the operational statuses for units in this force and in all of its subForces.
     */
    public List<LayeredForceIconOperationalStatus> updateForceIconOperationalStatus(
            final Campaign campaign) {
        // First, update all subForces, collecting their unit statuses into a single list
        final List<LayeredForceIconOperationalStatus> statuses = getSubForces().stream()
                .flatMap(subForce -> subForce.updateForceIconOperationalStatus(campaign).stream())
                .collect(Collectors.toList());

        // Then, Add the units assigned to this force
        statuses.addAll(getUnits().stream().map(campaign::getUnit).filter(Objects::nonNull)
                .map(LayeredForceIconOperationalStatus::determineLayeredForceIconOperationalStatus)
                .collect(Collectors.toList()));

        // Can only update the icon for LayeredForceIcons, but still need to return the processed
        // units for parent force updates
        if (!(getForceIcon() instanceof LayeredForceIcon)) {
            return statuses;
        }

        if (statuses.isEmpty()) {
            // No special modifier for empty forces
            ((LayeredForceIcon) getForceIcon()).getPieces().remove(LayeredForceIconLayer.SPECIAL_MODIFIER);
        } else {
            // Sum the unit status ordinals, then divide by the overall number of statuses, to get
            // the ordinal of the force's status. Then assign the operational status to this.
            final int index = (int) Math.round(statuses.stream().mapToInt(Enum::ordinal).sum() / (statuses.size() * 1.0));
            final LayeredForceIconOperationalStatus status = LayeredForceIconOperationalStatus.values()[index];
            ((LayeredForceIcon) getForceIcon()).getPieces().put(LayeredForceIconLayer.SPECIAL_MODIFIER, new ArrayList<>());
            ((LayeredForceIcon) getForceIcon()).getPieces().get(LayeredForceIconLayer.SPECIAL_MODIFIER)
                    .add(new ForcePieceIcon(LayeredForceIconLayer.SPECIAL_MODIFIER,
                            MekHQ.getMHQOptions().getNewDayForceIconOperationalStatusStyle().getPath(),
                            status.getFilename()));
        }

        return statuses;
    }

    public void writeToXML(PrintWriter pw1, int indent) {
        pw1.println(MHQXMLUtility.indentStr(indent++) + "<force id=\"" + id + "\" type=\"" + this.getClass().getName() + "\">");
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "name", name);
        getForceIcon().writeToXML(pw1, indent);
        getCamouflage().writeToXML(pw1, indent);
        if (!getDescription().isBlank()) {
            MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "desc", desc);
        }
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "combatForce", combatForce);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "formationLevel", formationLevel.toString());
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "scenarioId", scenarioId);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "techId", techId);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "forceCommanderID", forceCommanderID);
        if (!units.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw1, indent++, "units");
            for (UUID uid : units) {
                pw1.println(MHQXMLUtility.indentStr(indent) + "<unit id=\"" + uid + "\"/>");
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw1, --indent, "units");
        }

        if (!subForces.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw1, indent++, "subforces");
            for (Force sub : subForces) {
                sub.writeToXML(pw1, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw1, --indent, "subforces");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw1, --indent, "force");
    }

    public static @Nullable Force generateInstanceFromXML(Node wn, Campaign c, Version version) {
        Force retVal = new Force("");
        NamedNodeMap attrs = wn.getAttributes();
        Node idNameNode = attrs.getNamedItem("id");
        String idString = idNameNode.getTextContent();

        try {
            NodeList nl = wn.getChildNodes();
            retVal.id = Integer.parseInt(idString);

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    retVal.setName(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase(StandardForceIcon.XML_TAG)) {
                    retVal.setForceIcon(StandardForceIcon.parseFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase(LayeredForceIcon.XML_TAG)) {
                    retVal.setForceIcon(LayeredForceIcon.parseFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase(Camouflage.XML_TAG)) {
                    retVal.setCamouflage(Camouflage.parseFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase("camouflageCategory")) { // Legacy - 0.49.3 removal
                    retVal.getCamouflage().setCategory(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("camouflageFilename")) { // Legacy - 0.49.3 removal
                    retVal.getCamouflage().setFilename(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("desc")) {
                    retVal.setDescription(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("combatForce")) {
                    retVal.setCombatForce(Boolean.parseBoolean(wn2.getTextContent().trim()), false);
                } else if (wn2.getNodeName().equalsIgnoreCase("formationLevel")) {
                    retVal.setFormationLevel(FormationLevel.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("iconCategory")) { // Legacy - 0.49.6 removal
                    retVal.getForceIcon().setCategory(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("iconHashMap")) { // Legacy - 0.49.6 removal
                    final LayeredForceIcon layeredForceIcon = new LayeredForceIcon();
                    ForceIconMigrator.migrateLegacyIconMapNodes(layeredForceIcon, wn2);
                    retVal.setForceIcon(layeredForceIcon);
                } else if (wn2.getNodeName().equalsIgnoreCase("iconFileName")) { // Legacy - 0.49.6 removal
                    retVal.getForceIcon().setFilename(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("scenarioId")) {
                    retVal.scenarioId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("techId")) {
                    retVal.techId = UUID.fromString(wn2.getTextContent());
                }  else if (wn2.getNodeName().equalsIgnoreCase("forceCommanderID")) {
                    retVal.forceCommanderID = UUID.fromString(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("units")) {
                    processUnitNodes(retVal, wn2, version);
                } else if (wn2.getNodeName().equalsIgnoreCase("subforces")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("force")) {
                            LogManager.getLogger().error("Unknown node type not loaded in Forces nodes: " + wn3.getNodeName());
                            continue;
                        }

                        retVal.addSubForce(generateInstanceFromXML(wn3, c, version), true);
                    }
                }
            }
            c.importForce(retVal);
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            return null;
        }

        if (version.isLowerThan("0.49.3")) {
            CamouflageMigrator.migrateCamouflage(version, retVal.getCamouflage());
        }

        if (version.isLowerThan("0.49.6")) {
            retVal.setForceIcon(ForceIconMigrator.migrateForceIconToKailans(retVal.getForceIcon()));
        } else if (version.isLowerThan("0.49.7")) {
            retVal.setForceIcon(ForceIconMigrator.migrateForceIcon0496To0497(retVal.getForceIcon()));
        }

        retVal.updateCommander(c);

        return retVal;
    }

    private static void processUnitNodes(Force retVal, Node wn, Version version) {
        NodeList nl = wn.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            NamedNodeMap attrs = wn2.getAttributes();
            Node classNameNode = attrs.getNamedItem("id");
            String idString = classNameNode.getTextContent();
            retVal.addUnit(UUID.fromString(idString));
        }
    }

    public Vector<Object> getAllChildren(Campaign campaign) {
        Vector<Object> children = new Vector<>(subForces);
        //add any units
        Enumeration<UUID> uids = getUnits().elements();
        //put them into a temporary array so I can sort it by rank
        List<Unit> units = new ArrayList<>();
        List<Unit> unmannedUnits = new ArrayList<>();
        while (uids.hasMoreElements()) {
            Unit u = campaign.getUnit(uids.nextElement());
            if (null != u) {
                if (null == u.getCommander()) {
                    unmannedUnits.add(u);
                } else {
                    units.add(u);
                }
            }
        }
        units.sort((u1, u2) -> ((Comparable<Integer>) u2.getCommander().getRankNumeric())
                .compareTo(u1.getCommander().getRankNumeric()));

        children.addAll(units);
        children.addAll(unmannedUnits);
        return children;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof Force) && (((Force) o).getId() == id) && ((Force) o).getFullName().equals(getFullName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getFullName());
    }

    /**
     * Calculates the force's total BV, including sub forces.
     * @param c The working campaign.
     * @return Total BV
     */
    public int getTotalBV(Campaign c) {
        int bvTotal = 0;

        for (Force sforce : getSubForces()) {
            bvTotal += sforce.getTotalBV(c);
        }

        for (UUID id : getUnits()) {
            // no idea how this would happen, but sometimes a unit in a forces unit ID list has an invalid ID?
            if (c.getUnit(id) == null) {
                continue;
            }

            bvTotal += c.getUnit(id).getEntity().calculateBattleValue();
        }

        return bvTotal;
    }

    /**
     * Calculates the unit type most represented in this force
     * and all subforces.
     * @param c Working campaign
     * @return Majority unit type.
     */
    public int getPrimaryUnitType(Campaign c) {
        Map<Integer, Integer> unitTypeBuckets = new TreeMap<>();
        int biggestBucketID = -1;
        int biggestBucketCount = 0;

        for (UUID id : getUnits()) {
            int unitType = c.getUnit(id).getEntity().getUnitType();

            unitTypeBuckets.merge(unitType, 1, Integer::sum);

            if (unitTypeBuckets.get(unitType) > biggestBucketCount) {
                biggestBucketCount = unitTypeBuckets.get(unitType);
                biggestBucketID = unitType;
            }
        }

        return biggestBucketID;
    }


    /**
     * Finds the distance (depth) from the origin force
     * @param force the force to get depth for
     */
    public static int getDepth(Force force) {
        int depth = 0;

        Force parent = force.getParentForce();

        while (parent != null) {
            depth++;
            force = parent;
            parent = force.getParentForce();
        }

        return depth;
    }

    /**
     * Uses a recursive search to find the maximum distance (depth) from the origin force
     * @param force the current force. Should always equal campaign.getForce(0), if called remotely
     * @param depth the current recursive depth. Can be left null, if called remotely
     */
    public static int getMaximumDepth(Force force, @Nullable Integer depth) {
        if (depth == null) {
            depth = 0;
        }

        int maximumDepth = depth;

        Vector<Force> subForces = force.getSubForces();

        if (!subForces.isEmpty()) {
            for (Force subforce : subForces) {
                int nextDepth = getMaximumDepth(subforce, depth + 1);

                if (nextDepth > maximumDepth) {
                    maximumDepth = nextDepth;
                }
            }
        }

        return maximumDepth;
    }
}
