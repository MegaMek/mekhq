/*
 * Copyright (c) 2011 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.force;

import static java.lang.Math.floor;
import static java.lang.Math.round;
import static mekhq.utilities.EntityUtilities.getEntityFromUnitId;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import megamek.Version;
import megamek.common.annotations.Nullable;
import megamek.common.icons.Camouflage;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.events.OrganizationChangedEvent;
import mekhq.campaign.icons.ForcePieceIcon;
import mekhq.campaign.icons.LayeredForceIcon;
import mekhq.campaign.icons.StandardForceIcon;
import mekhq.campaign.icons.enums.LayeredForceIconLayer;
import mekhq.campaign.icons.enums.OperationalStatus;
import mekhq.campaign.log.AssignmentLogger;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This is a hierarchical object to define forces for TO&amp;E. Each Force object can have a parent force object and a
 * vector of child force objects. Each force can also have a vector of PilotPerson objects. The idea is that any time
 * TOE is refreshed in MekHQView, the force object can be traversed to generate a set of TreeNodes that can be applied
 * to the JTree showing the force TO&amp;E.
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Force {
    private static final MMLogger LOGGER = MMLogger.create(Force.class);

    // region Variable Declarations
    // pathway to force icon
    public static final int FORCE_NONE = -1;
    /**
     * This is the id of the 'origin node'. The force from which all other forces descend. Normally named after the
     * campaign.
     */
    public static final int FORCE_ORIGIN = 0;

    public static final int COMBAT_TEAM_OVERRIDE_NONE = -1;
    public static final int COMBAT_TEAM_OVERRIDE_FALSE = 0;
    public static final int COMBAT_TEAM_OVERRIDE_TRUE = 1;

    public static final int NO_ASSIGNED_SCENARIO = -1;

    private String name;
    private StandardForceIcon forceIcon;
    private Camouflage camouflage;
    private String desc;
    private ForceType forceType;
    private boolean isCombatTeam;
    private int overrideCombatTeam;
    private FormationLevel formationLevel;
    private FormationLevel overrideFormationLevel;
    private CombatRole combatRoleInMemory;
    private Force parentForce;
    private final Vector<Force> subForces;
    private final Vector<UUID> units;
    private int scenarioId;
    private UUID forceCommanderID;
    private UUID overrideForceCommanderID;
    protected UUID techId;

    // an ID so that forces can be tracked in Campaign hash
    private int id;
    // endregion Variable Declarations

    // region Constructors
    public Force(String name) {
        setName(name);
        setForceIcon(new LayeredForceIcon());
        setCamouflage(new Camouflage());
        setDescription("");
        this.forceType = ForceType.STANDARD;
        this.isCombatTeam = false;
        this.overrideCombatTeam = COMBAT_TEAM_OVERRIDE_NONE;
        this.formationLevel = FormationLevel.NONE;
        this.overrideFormationLevel = FormationLevel.NONE;
        this.combatRoleInMemory = CombatRole.FRONTLINE;
        this.parentForce = null;
        this.subForces = new Vector<>();
        this.units = new Vector<>();
        this.scenarioId = NO_ASSIGNED_SCENARIO;
    }
    // endregion Constructors

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
        return getCamouflage().hasDefaultCategory() ?
                     ((getParentForce() == null) ? camouflage : getParentForce().getCamouflageOrElse(camouflage)) :
                     getCamouflage();
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

    /**
     * @return The {@code ForceType} currently assigned to this instance.
     */
    public ForceType getForceType() {
        return forceType;
    }

    /**
     * This method compares the provided {@code forceType} with the current instance's {@code ForceType} to determine if
     * they match.
     *
     * @param forceType The {@code ForceType} to compare against.
     *
     * @return {@code true} if the current instance matches the specified {@code forceType}; otherwise, {@code false}.
     */
    public boolean isForceType(ForceType forceType) {
        return this.forceType == forceType;
    }

    /**
     * Updates the {@code ForceType} for this instance and optionally propagates the change to all sub-forces.
     *
     * <p>If the {@code setForSubForces} flag is {@code true}, the method recursively sets the
     * provided {@code forceType} for all sub-forces of this instance.</p>
     *
     * @param forceType       The new {@code ForceType} to assign to this instance.
     * @param setForSubForces A flag indicating whether the change should also apply to sub-forces.
     */
    public void setForceType(ForceType forceType, boolean setForSubForces) {
        this.forceType = forceType;
        if (setForSubForces) {
            for (Force force : subForces) {
                force.setForceType(forceType, true);
            }
        }
    }

    public boolean isCombatTeam() {
        return isCombatTeam;
    }

    public void setCombatTeamStatus(final boolean isCombatTeam) {
        this.isCombatTeam = isCombatTeam;
    }

    public int getOverrideCombatTeam() {
        return overrideCombatTeam;
    }

    public void setOverrideCombatTeam(final int overrideCombatTeam) {
        this.overrideCombatTeam = overrideCombatTeam;
    }

    public FormationLevel getFormationLevel() {
        return getOverrideFormationLevel() != null && getOverrideFormationLevel() != FormationLevel.NONE ?
                     getOverrideFormationLevel() :
                     getDefaultFormationLevel();
    }

    private FormationLevel getDefaultFormationLevel() {
        return formationLevel;
    }

    public void setFormationLevel(final FormationLevel formationLevel) {
        this.formationLevel = formationLevel;
    }

    public FormationLevel getOverrideFormationLevel() {
        return overrideFormationLevel;
    }

    public void setOverrideFormationLevel(final FormationLevel overrideFormationLevel) {
        if (overrideFormationLevel == FormationLevel.REMOVE_OVERRIDE) {
            this.overrideFormationLevel = FormationLevel.NONE;
        } else {
            this.overrideFormationLevel = overrideFormationLevel;
        }
    }

    public CombatRole getCombatRoleInMemory() {
        return combatRoleInMemory;
    }

    public void setCombatRoleInMemory(final CombatRole combatRoleInMemory) {
        this.combatRoleInMemory = combatRoleInMemory;
    }

    public int getScenarioId() {
        return scenarioId;
    }

    /**
     * Set scenario ID (e.g. deploy to scenario) for a force and all of its subForces and units
     *
     * @param scenarioId scenario to deploy to
     * @param campaign   campaign - required to update units
     */
    public void setScenarioId(int scenarioId, Campaign campaign) {
        this.scenarioId = scenarioId;
        for (Force sub : getSubForces()) {
            sub.setScenarioId(scenarioId, campaign);
        }
        for (UUID uid : getUnits()) {
            Unit unit = campaign.getUnit(uid);
            if (null != unit) {
                unit.setScenarioId(scenarioId);
            }
        }
    }

    public void setTechID(UUID tech) {
        techId = tech;
    }

    public UUID getTechID() {
        return techId;
    }

    public boolean isDeployed() {
        // forces are deployed if their parent force is
        if ((null != parentForce) && parentForce.isDeployed()) {
            return true;
        }
        return scenarioId != NO_ASSIGNED_SCENARIO;
    }

    public @Nullable Force getParentForce() {
        return parentForce;
    }

    /**
     * This method generates a list of all parent forces for the current force object in the hierarchy. It repeatedly
     * fetches the parent force of the current force and adds it to a list until no more parent forces can be found
     * (i.e., until the top of the force hierarchy is reached).
     *
     * @return A list of {@link Force} objects representing all the parent forces of the current force object in the
     *       hierarchy. The list will be empty if there are no parent forces.
     */
    public List<Force> getAllParents() {
        List<Force> parentForces = new ArrayList<>();

        Force parentFormation = parentForce;

        if (parentForce != null) {
            parentForces.add(parentForce);
        }

        while (parentFormation != null) {
            parentFormation = parentFormation.getParentForce();

            if (parentFormation != null) {
                parentForces.add(parentFormation);
            }
        }

        return parentForces;
    }

    public void setParentForce(final @Nullable Force parent) {
        this.parentForce = parent;
    }

    public Vector<Force> getSubForces() {
        return subForces;
    }

    /**
     * Returns a list of all of this forces' descendant forces. This includes direct child forces and their descendents
     * recursively.
     * <p>
     * This method works by first adding all direct child forces to the list, and then recursively adding their
     * descendants by calling this method on each child force.
     *
     * @return A list of {@link Force} objects representing all descendant forces. If there are no descendant forces,
     *       this method will return an empty list.
     */
    public List<Force> getAllSubForces() {
        List<Force> allSubForces = new ArrayList<>();

        for (Force subForce : subForces) {
            allSubForces.add(subForce);

            allSubForces.addAll(subForce.getAllSubForces());
        }

        return allSubForces;
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
     * @return the full hierarchical name of the force, including all parents (except the origin force)
     */
    public String getFullName() {
        String toReturn = getName();
        if (null != parentForce) {
            if (parentForce.getId() != FORCE_ORIGIN) {
                toReturn += ", " + parentForce.getFullName();
            }
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
            result.append(ancestor.getName()).append('|').append(id);
            if (!ancestor.getCamouflage().isDefault()) {
                result.append('|')
                      .append(ancestor.getCamouflage().getCategory())
                      .append('|')
                      .append(ancestor.getCamouflage().getFilename());
            }
            result.append("||");
        }
        return result.toString();
    }

    /**
     * Add a sub force to the sub force vector. In general, this should not be called directly to add forces to the
     * campaign because they will not be assigned an id. Use {@link Campaign#addForce(Force, Force)} instead The boolean
     * assignParent here is set to false when assigning forces from the TOE to a scenario, because we don't want to
     * switch this forces real parent
     *
     * @param sub the sub force to add, which may be null from a load failure. This returns without adding in that case
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
     * @param standardForcesOnly to only include combat forces or to also include support forces
     *
     * @return all the unit ids in this force and all of its subForces
     */
    public Vector<UUID> getAllUnits(boolean standardForcesOnly) {
        Vector<UUID> allUnits = new Vector<>();

        if (!standardForcesOnly || forceType.isStandard()) {
            allUnits.addAll(units);
        }

        for (Force force : subForces) {
            allUnits.addAll(force.getAllUnits(standardForcesOnly));
        }

        return allUnits;
    }

    /**
     * Retrieves all units associated with the current force as {@link Unit} objects.
     *
     * <p>This method converts the list of unit IDs from the force into a list of
     * {@link Unit} objects by fetching them from the provided {@link Hangar}. Units are only included if they can be
     * successfully resolved from the hangar.</p>
     *
     * @param hangar             the {@link Hangar} containing the units to retrieve.
     * @param standardForcesOnly a flag indicating whether to include only standard forces. If {@code true}, only units
     *                           belonging to standard forces are returned.
     *
     * @return a list of {@link Unit} objects associated with the force.
     */
    public List<Unit> getAllUnitsAsUnits(Hangar hangar, boolean standardForcesOnly) {
        List<Unit> allUnits = new ArrayList<>();

        for (UUID unitId : getAllUnits(standardForcesOnly)) {
            Unit unit = hangar.getUnit(unitId);
            if (unit != null) {
                allUnits.add(unit);
            }
        }

        return allUnits;
    }

    /**
     * Add a unit id to the units vector. In general, this should not be called directly to add unid because they will
     * not be assigned a force id. Use {@link Campaign#addUnitToForce(Unit, int)} instead
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
                    AssignmentLogger.reassignedTOEForce(campaign, person, campaign.getLocalDate(), oldForce, this);
                } else {
                    AssignmentLogger.addedToTOEForce(campaign, person, campaign.getLocalDate(), this);
                }
            }
        }

        updateCommander(campaign);
    }

    /**
     * This should not be directly called except by {@link Campaign#removeUnitFromForce(Unit)} instead
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
                        AssignmentLogger.removedFromTOEForce(campaign, person, campaign.getLocalDate(), this);
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
            // If we're not killing the units from the scenario, then we need to assign them
            // with the
            // scenario ID and add them to the scenario.
            for (UUID uid : getUnits()) {
                c.getUnit(uid).setScenarioId(getScenarioId());
                c.getScenario(getScenarioId()).addUnit(uid);
            }
        }
        setScenarioId(NO_ASSIGNED_SCENARIO, c);
    }

    public int getId() {
        return id;
    }

    public void setId(int i) {
        this.id = i;
    }

    public @Nullable UUID getForceCommanderID() {
        return forceCommanderID;
    }

    /**
     * Sets the force commander ID to the provided UUID. You probably want to use setOverrideForceCommanderID(UUID)
     * followed by updateCommander(campaign).
     *
     * @param commanderID UUID of the commander
     *
     * @see #setOverrideForceCommanderID(UUID)
     * @see #updateCommander(Campaign)
     */
    private void setForceCommanderID(@Nullable UUID commanderID) {
        forceCommanderID = commanderID;
    }

    public UUID getOverrideForceCommanderID() {
        return overrideForceCommanderID;
    }

    public void setOverrideForceCommanderID(UUID overrideForceCommanderID) {
        this.overrideForceCommanderID = overrideForceCommanderID;
    }

    /**
     * Returns a list of unit or force commanders eligible to be considered for the position of force commander.
     *
     * @param campaign the campaign to get eligible commanders from
     *
     * @return a list of UUIDs representing the eligible commanders
     */
    public List<UUID> getEligibleCommanders(Campaign campaign) {
        List<UUID> eligibleCommanders = new ArrayList<>();

        // don't use a stream here, it's not worth the added risk of an NPE
        for (UUID unitId : getUnits()) {
            Unit unit = campaign.getUnit(unitId);

            if ((unit != null) && (unit.getCommander() != null)) {
                eligibleCommanders.add(unit.getCommander().getId());
            }
        }

        // this means the force contains no Units, so we check against the leaders of
        // the sub-forces
        if (eligibleCommanders.isEmpty()) {
            for (Force force : getSubForces()) {
                UUID forceCommander = force.getForceCommanderID();

                if (forceCommander != null) {
                    eligibleCommanders.add(forceCommander);
                }
            }
        }

        return eligibleCommanders;
    }

    /**
     * Updates the commander for a force based on the ranking of eligible commanders.
     *
     * @param campaign the current campaign
     */
    public void updateCommander(Campaign campaign) {
        List<UUID> eligibleCommanders = getEligibleCommanders(campaign);

        if (eligibleCommanders.isEmpty()) {
            forceCommanderID = null;
            overrideForceCommanderID = null;
            updateCombatTeamCommanderIfCombatTeam(campaign);
            return;
        }

        if (overrideForceCommanderID != null) {
            if (eligibleCommanders.contains(overrideForceCommanderID)) {
                forceCommanderID = overrideForceCommanderID;
                updateCombatTeamCommanderIfCombatTeam(campaign);

                if (getParentForce() != null) {
                    getParentForce().updateCommander(campaign);
                }
                return;
            } else {
                overrideForceCommanderID = null;
            }
        }

        Collections.shuffle(eligibleCommanders);
        Person highestRankedPerson = campaign.getPerson(eligibleCommanders.get(0));

        for (UUID eligibleCommanderId : eligibleCommanders) {
            Person eligibleCommander = campaign.getPerson(eligibleCommanderId);
            if (eligibleCommander == null) {
                continue;
            }

            if (eligibleCommander.outRanksUsingSkillTiebreaker(campaign, highestRankedPerson)) {
                highestRankedPerson = eligibleCommander;
            }
        }

        if (highestRankedPerson == null) {
            LOGGER.info("Force {} has no eligible commanders", getName());
            forceCommanderID = null;
        } else {
            forceCommanderID = highestRankedPerson.getId();
        }

        updateCombatTeamCommanderIfCombatTeam(campaign);

        if (getParentForce() != null) {
            getParentForce().updateCommander(campaign);
        }
    }

    private void updateCombatTeamCommanderIfCombatTeam(Campaign campaign) {
        if (isCombatTeam()) {
            CombatTeam combatTeam = campaign.getCombatTeamsAsMap().getOrDefault(getId(), null);
            if (combatTeam != null) {
                combatTeam.setCommander(getForceCommanderID());
            }
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
     *
     * @param campaign the campaign to determine the operational status of this force using
     *
     * @return a list of the operational statuses for units in this force and in all of its subForces.
     */
    public List<OperationalStatus> updateForceIconOperationalStatus(final Campaign campaign) {
        // First, update all subForces, collecting their unit statuses into a single
        // list
        final List<OperationalStatus> statuses = getSubForces().stream()
                                                       .flatMap(subForce -> subForce.updateForceIconOperationalStatus(
                                                             campaign).stream())
                                                       .collect(Collectors.toList());

        // Then, Add the units assigned to this force
        statuses.addAll(getUnits().stream()
                              .map(campaign::getUnit)
                              .filter(Objects::nonNull)
                              .map(OperationalStatus::determineLayeredForceIconOperationalStatus)
                              .toList());

        // Can only update the icon for LayeredForceIcons, but still need to return the
        // processed
        // units for parent force updates
        if (!(getForceIcon() instanceof LayeredForceIcon)) {
            return statuses;
        }

        if (statuses.isEmpty()) {
            // No special modifier for empty forces
            ((LayeredForceIcon) getForceIcon()).getPieces().remove(LayeredForceIconLayer.SPECIAL_MODIFIER);
        } else {
            // Sum the unit status ordinals, then divide by the overall number of statuses,
            // to get
            // the ordinal of the force's status. Then assign the operational status to
            // this.
            final int index = (int) round(statuses.stream().mapToInt(Enum::ordinal).sum() / (statuses.size() * 1.0));
            final OperationalStatus status = OperationalStatus.values()[index];
            ((LayeredForceIcon) getForceIcon()).getPieces()
                  .put(LayeredForceIconLayer.SPECIAL_MODIFIER, new ArrayList<>());
            ((LayeredForceIcon) getForceIcon()).getPieces()
                  .get(LayeredForceIconLayer.SPECIAL_MODIFIER)
                  .add(new ForcePieceIcon(LayeredForceIconLayer.SPECIAL_MODIFIER,
                        MekHQ.getMHQOptions().getNewDayForceIconOperationalStatusStyle().getPath(),
                        status.getFilename()));
        }

        return statuses;
    }

    public void writeToXML(PrintWriter pw1, int indent) {
        pw1.println(MHQXMLUtility.indentStr(indent++) +
                          "<force id=\"" +
                          id +
                          "\" type=\"" +
                          this.getClass().getName() +
                          "\">");
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "name", name);
        getForceIcon().writeToXML(pw1, indent);
        getCamouflage().writeToXML(pw1, indent);
        if (!getDescription().isBlank()) {
            MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "desc", desc);
        }
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "forceType", forceType.ordinal());
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "overrideCombatTeam", overrideCombatTeam);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "formationLevel", formationLevel.toString());
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "overrideFormationLevel", overrideFormationLevel.toString());
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "preferredRole", combatRoleInMemory.name());
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "scenarioId", scenarioId);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "techId", techId);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "overrideForceCommanderID", overrideForceCommanderID);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "forceCommanderID", forceCommanderID);
        if (!units.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw1, indent++, "units");
            for (UUID uid : units) {
                pw1.println(MHQXMLUtility.indentStr(indent) + "<unit id=\"" + uid + "\"/>");
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw1, --indent, "units");
        }

        if (!subForces.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw1, indent++, "subForces");
            for (Force sub : subForces) {
                sub.writeToXML(pw1, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw1, --indent, "subForces");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw1, --indent, "force");
    }

    public static @Nullable Force generateInstanceFromXML(Node workingNode, Campaign campaign, Version version) {
        Force force = new Force("");
        NamedNodeMap attributes = workingNode.getAttributes();
        Node idNameNode = attributes.getNamedItem("id");
        String idString = idNameNode.getTextContent();

        try {
            NodeList childNodes = workingNode.getChildNodes();
            force.id = Integer.parseInt(idString);

            for (int x = 0; x < childNodes.getLength(); x++) {
                Node wn2 = childNodes.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    force.setName(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase(StandardForceIcon.XML_TAG)) {
                    force.setForceIcon(StandardForceIcon.parseFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase(LayeredForceIcon.XML_TAG)) {
                    force.setForceIcon(LayeredForceIcon.parseFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase(Camouflage.XML_TAG)) {
                    force.setCamouflage(Camouflage.parseFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase("desc")) {
                    force.setDescription(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("forceType")) {
                    force.setForceType(ForceType.fromKey(Integer.parseInt(wn2.getTextContent().trim())), false);
                } else if (wn2.getNodeName().equalsIgnoreCase("overrideCombatTeam")) {
                    force.setOverrideCombatTeam(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("formationLevel")) {
                    force.setFormationLevel(FormationLevel.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("overrideFormationLevel")) {
                    force.setOverrideFormationLevel(FormationLevel.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("preferredRole")) {
                    force.setCombatRoleInMemory(CombatRole.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("scenarioId")) {
                    force.scenarioId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("techId")) {
                    force.techId = UUID.fromString(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("overrideForceCommanderID")) {
                    force.overrideForceCommanderID = UUID.fromString(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("forceCommanderID")) {
                    force.forceCommanderID = UUID.fromString(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("units")) {
                    processUnitNodes(force, wn2, version);
                } else if (wn2.getNodeName().equalsIgnoreCase("subForces")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("force")) {
                            String message = String.format("Unknown node type not loaded in Forces nodes: %s",
                                  wn3.getNodeName());
                            LOGGER.error(message);
                            continue;
                        }

                        force.addSubForce(generateInstanceFromXML(wn3, campaign, version), true);
                    }
                }
            }
            campaign.importForce(force);
        } catch (Exception ex) {
            LOGGER.error("", ex);
            return null;
        }

        return force;
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

    /**
     * Returns a vector containing all children of this force, including sub-forces and units, sorted by commander
     * rank.
     *
     * <p>This method gathers all subordinate objects belonging to this force: first, it adds all sub-forces, then it
     * adds all units under this force (with crewed units before uncrewed units), finally sorting crewed units by their
     * commander's numeric rank in descending order.</p>
     *
     * @param campaign The {@link Campaign} instance used to look up unit and personnel data.
     *
     * @return A {@link Vector} of all child objects, including sub-forces and units, sorted such that crewed units
     *       appear before unmanned units, and crewed units are ordered by their commander's rank descending.
     */
    public Vector<Object> getAllChildren(Campaign campaign) {
        Vector<Object> children = new Vector<>(subForces);
        // add any units
        Enumeration<UUID> uids = getUnits().elements();
        // put them into a temporary array, so I can sort it by rank
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
        units.sort((u1, u2) -> ((Comparable<Integer>) u2.getCommander().getRankNumeric()).compareTo(u1.getCommander()
                                                                                                          .getRankNumeric()));

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
     *
     * @param campaign                 The working campaign. This is the campaign object that the force belongs to.
     * @param forceStandardBattleValue Flag indicating whether to override campaign settings that call for the use of
     *                                 Generic BV
     *
     * @return The total battle value (BV) of the force.
     */
    public int getTotalBV(Campaign campaign, boolean forceStandardBattleValue) {
        int bvTotal = 0;

        for (UUID unitId : getAllUnits(false)) {
            // no idea how this would happen, but sometimes a unit in a forces unit ID list
            // has an invalid ID?
            if (campaign.getUnit(unitId) == null) {
                continue;
            }

            if (campaign.getCampaignOptions().isUseGenericBattleValue() && !forceStandardBattleValue) {
                bvTotal += campaign.getUnit(unitId).getEntity().getGenericBattleValue();
            } else {
                bvTotal += campaign.getUnit(unitId).getEntity().calculateBattleValue();
            }
        }

        return bvTotal;
    }

    /**
     * Calculates the total count of units in the given force, including all sub forces.
     *
     * @param campaign      the current campaign
     * @param isClanBidding flag to indicate whether clan bidding is being performed
     *
     * @return the total count of units in the force (including sub forces)
     */
    public int getTotalUnitCount(Campaign campaign, boolean isClanBidding) {
        int unitTotal = 0;

        for (Force subforce : getSubForces()) {
            unitTotal += subforce.getTotalUnitCount(campaign, isClanBidding);
        }

        // If we're getting the unit count specifically for Clan Bidding, we don't want to count
        // Conventional Infantry, and we only count Battle Armor as half a unit.
        // If we're not performing Clan Bidding, we just need the total count of units.
        if (isClanBidding) {
            double rollingCount = 0;

            for (UUID unitId : getUnits()) {
                Entity unit = campaign.getUnit(unitId).getEntity();

                if (unit.isBattleArmor()) {
                    rollingCount += 0.5;
                } else if (!unit.isConventionalInfantry()) {
                    rollingCount++;
                }
            }

            unitTotal += (int) round(rollingCount);
        } else {
            unitTotal += getUnits().size();
        }

        return unitTotal;
    }

    /**
     * Calculates the unit type most represented in this force and all subForces.
     *
     * @param campaign Working campaign
     *
     * @return Majority unit type.
     */
    public int getPrimaryUnitType(Campaign campaign) {
        Map<Integer, Integer> unitTypeBuckets = new TreeMap<>();
        int biggestBucketID = -1;
        int biggestBucketCount = 0;

        for (UUID id : getAllUnits(false)) {
            int unitType = campaign.getUnit(id).getEntity().getUnitType();

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
     *
     * @param force the force to get depth for
     *
     * @deprecated
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
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
     *
     * @param force the current force. Should always equal campaign.getForce(0), if called remotely
     * @param depth the current recursive depth.
     *
     * @deprecated
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
    public static int getMaximumDepth(Force force, Integer depth) {
        int maximumDepth = depth;

        for (Force subforce : force.getSubForces()) {
            int nextDepth = getMaximumDepth(subforce, depth + 1);

            if (nextDepth > maximumDepth) {
                maximumDepth = nextDepth;
            }
        }

        return maximumDepth;
    }

    /**
     * Populates the formation levels of a force hierarchy starting from the origin force. For all subforces, it will
     * determine the smallest formations - Teams/Lances - and then parent formations will be one formation higher.
     *
     * @param campaign campaign that the force belongs to
     */
    public static void populateFormationLevelsFromOrigin(Campaign campaign) {
        Force force = campaign.getForce(0);

        recursivelyUpdateFormationLevel(campaign, force);

        MekHQ.triggerEvent(new OrganizationChangedEvent(force));
    }

    private static void recursivelyUpdateFormationLevel(Campaign campaign, Force force) {
        for (Force subforce : force.getSubForces()) {
            recursivelyUpdateFormationLevel(campaign, subforce);
        }
        force.defaultFormationLevelForForce(campaign);
    }

    /**
     * Based on a force's subforces and units, set this unit's formation to the default.
     *
     * @param campaign campaign that the force belongs to
     */
    public void defaultFormationLevelForForce(Campaign campaign) {
        Force largestSubForce =
              getAllSubForces().stream().max(Comparator.comparing(f -> f.getFormationLevel().getDepth())).orElse(null);
        if (largestSubForce == null) {
            int depth = 1;
            setFormationLevel(FormationLevel.parseFromDepth(campaign, depth + getOddFormationSizeModifier(campaign,
                  depth)));
        } else {
            int depth = largestSubForce.getFormationLevel().getDepth();
            setFormationLevel(FormationLevel.parseFromDepth(campaign, depth + 1));
        }
    }

    private int getOddFormationSizeModifier(Campaign campaign, int depth) {
        int actualUnitCount = getTotalUnitCount(campaign, false);
        final int baseFormationSize = campaign.getFaction().getFormationBaseSize();
        if (depth == 1) {
            if (actualUnitCount <= baseFormationSize / 2) {
                return -1;
            }
        }

        return 0;
    }

    /**
     * Changes the formation level of a force and its sub-forces.
     *
     * @param force                 the force whose formation level is to be changed
     * @param currentFormationLevel the current formation level of the force
     * @param lowerBoundary         the lower boundary for the formation level
     *
     * @deprecated See {@link Force#recursivelyUpdateFormationLevel}
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
    private static void changeFormationLevel(Force force, int currentFormationLevel, int lowerBoundary) {
        for (Force subforce : force.getSubForces()) {
            if (currentFormationLevel - 1 < lowerBoundary) {
                subforce.setFormationLevel(FormationLevel.INVALID);
            } else if (subforce.getSubForces().isEmpty()) {
                subforce.setFormationLevel(FormationLevel.parseFromInt(lowerBoundary));
            } else {
                subforce.setFormationLevel(FormationLevel.parseFromInt(currentFormationLevel - 1));
            }

            MekHQ.triggerEvent(new OrganizationChangedEvent(force));

            changeFormationLevel(subforce, currentFormationLevel - 1, lowerBoundary);
        }
    }

    /**
     * Retrieves the lower boundary value based on the given campaign.
     *
     * @param campaign the campaign object to retrieve the lower boundary for
     *
     * @return the lower boundary value as an integer
     *
     * @deprecated
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
    private static int getLowerBoundary(Campaign campaign) {
        int lowerBoundary = FormationLevel.values().length;

        for (FormationLevel level : FormationLevel.values()) {
            if (level.isNone() || level.isInvalid() || level.isRemoveOverride()) {
                continue;
            }

            boolean isValid = false;

            if (campaign.getFaction().isClan() && level.isClan()) {
                isValid = true;
            } else if (campaign.getFaction().isComStarOrWoB()) {
                isValid = true;
            }
            if (!campaign.getFaction().isClan() && !campaign.getFaction().isComStarOrWoB()) {
                isValid = level.isInnerSphere();
            }

            if (isValid) {
                lowerBoundary = Math.min(level.parseToInt(), lowerBoundary);
            }
        }
        return lowerBoundary;
    }

    /**
     * Populates the origin node (the force normally named after the campaign) with an appropriate Formation Level.
     *
     * @param campaign the current campaign
     * @param origin   the origin node
     *
     * @return the parsed integer value of the origin node's formation level
     *
     * @deprecated See {@link Force#recursivelyUpdateFormationLevel}
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
    private static int populateOriginNode(Campaign campaign, Force origin) {
        FormationLevel overrideFormationLevel = origin.getOverrideFormationLevel();
        int maximumDepth = getMaximumDepth(origin, 0);

        if (!overrideFormationLevel.isNone() && !overrideFormationLevel.isRemoveOverride()) {
            origin.setFormationLevel(overrideFormationLevel);
        } else {
            origin.setFormationLevel(FormationLevel.parseFromDepth(campaign, maximumDepth));
            origin.setOverrideFormationLevel(FormationLevel.NONE);
        }

        MekHQ.triggerEvent(new OrganizationChangedEvent(origin));

        return origin.getFormationLevel().parseToInt();
    }

    /**
     * Determines whether a force consists solely of VTOL (Vertical Take-Off and Landing) or WIGE (Wing in Ground
     * Effect) units.
     *
     * <p>This method evaluates the force by checking each unit to verify that all resolved units
     * are either VTOLs or WIGE units.</p>
     *
     * <p><strong>Behavior:</strong></p>
     * <ul>
     *   <li>Retrieves all units in the force based on the {@code standardForcesOnly} flag.</li>
     *   <li>Skips any unit that cannot be resolved (null entity).</li>
     *   <li>Returns {@code false} if any resolved unit is not categorized as a VTOL or WIGE unit.</li>
     *   <li>Returns {@code true} if all resolved units meet the VTOL or WIGE criteria.</li>
     * </ul>
     *
     * @param hangar             The {@link Hangar} instance from which to retrieve the {@link Unit}.
     * @param standardForcesOnly A flag to filter and include only standard forces from the force.
     *
     * @return {@code true} if all resolved units in the force are VTOL or WIGE units, {@code false} otherwise.
     */
    public boolean forceContainsOnlyVTOLForces(Hangar hangar, boolean standardForcesOnly) {
        for (UUID unitId : getAllUnits(standardForcesOnly)) {
            Entity entity = getEntityFromUnitId(hangar, unitId);

            if (entity == null) {
                continue;
            }

            if (!entity.isAirborneVTOLorWIGE()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Determines whether a force contains a majority of VTOL (Vertical Take-Off and Landing) or WIGE (Wing in Ground
     * Effect) units.
     *
     * <p>This method evaluates the force by calculating whether at least half of the resolved units
     * in the force are categorized as VTOLs or WIGE units.</p>
     *
     * <p><strong>Behavior:</strong></p>
     * <ul>
     *   <li>Retrieves all units in the force based on the {@code standardForcesOnly} flag.</li>
     *   <li>Counts the number of units categorized as airborne VTOL or WIGE.</li>
     *   <li>Adjusts the total force size if unresolved (null) entities are skipped without counting
     *       them toward the total size.</li>
     *   <li>Stops counting early if a majority of VTOL or WIGE units is determined before evaluating
     *       all entities.</li>
     * </ul>
     *
     * @param hangar             The {@link Hangar} instance from which to retrieve the {@link Unit}.
     * @param standardForcesOnly A flag to filter and include only standard forces from the force.
     *
     * @return {@code true} if VTOL or WIGE units constitute at least half of the resolved force units, {@code false}
     *       otherwise.
     */
    public boolean forceContainsMajorityVTOLForces(Hangar hangar, boolean standardForcesOnly) {
        Vector<UUID> allUnits = getAllUnits(standardForcesOnly);
        int forceSize = allUnits.size();
        int vtolCount = 0;

        for (UUID unitId : allUnits) {
            Entity entity = getEntityFromUnitId(hangar, unitId);

            if (entity == null) {
                forceSize--;
                continue;
            }

            if (entity.isAirborneVTOLorWIGE()) {
                vtolCount++;
            }

            if (vtolCount >= forceSize / 2) {
                break;
            }
        }

        return vtolCount >= floor((double) forceSize / 2);
    }

    /**
     * Determines whether a force contains only aerospace or conventional fighters.
     *
     * <p>This method checks all units in the force to confirm if they consist exclusively of aerial
     * units based on their type.</p>
     *
     * <p><strong>Behavior:</strong></p>
     * <ul>
     *   <li>Filters the force's units based on the {@code standardForcesOnly} flag.</li>
     *   <li>Iterates through all selected units in the force.</li>
     *   <li>Skips any unit that cannot be resolved (null entity).</li>
     *   <li>If {@code excludeConventionalFighters} is {@code true} and the force contains any
     *       conventional fighters, the method immediately returns {@code false}.</li>
     *   <li>Returns {@code false} if any unit in the force is not an aerial unit (i.e., not an aerospace
     *       unit or conventional fighter).</li>
     *   <li>Returns {@code true} if all units in the force meet the aerial unit criteria.</li>
     * </ul>
     *
     * @param hangar                      The {@link Hangar} instance from which to retrieve the {@link Unit}.
     * @param standardForcesOnly          A flag to filter and include only standard forces from the force.
     * @param excludeConventionalFighters A flag determining if conventional fighters should be excluded from the
     *                                    assessment.
     *
     * @return {@code true} if the force consists only of aerial units (respecting the provided filters), {@code false}
     *       otherwise.
     */
    public boolean forceContainsOnlyAerialForces(Hangar hangar, boolean standardForcesOnly,
          boolean excludeConventionalFighters) {
        for (UUID unitId : getAllUnits(standardForcesOnly)) {
            Entity entity = getEntityFromUnitId(hangar, unitId);

            if (entity == null) {
                continue;
            }

            if (excludeConventionalFighters && entity.isConventionalFighter()) {
                return false;
            }

            if (!entity.isAerospace() && !entity.isConventionalFighter()) {
                return false;
            }
        }

        return true;
    }

    public int getSalvageUnitCount(Hangar hangar, boolean isInSpace) {
        List<Unit> unitsInForce = getAllUnitsAsUnits(hangar, false);

        int unitCount = 0;
        for (Unit unit : unitsInForce) {
            boolean canSurviveInSpace = false;
            Entity entity = unit.getEntity();
            if (entity != null) {
                canSurviveInSpace = !entity.doomedInSpace();
            }
            if (unit.canSalvage(isInSpace) && (!isInSpace || canSurviveInSpace)) {
                unitCount++;
            }
        }

        return unitCount;
    }
}
