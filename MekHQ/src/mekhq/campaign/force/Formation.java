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
import mekhq.campaign.icons.FormationPieceIcon;
import mekhq.campaign.icons.LayeredFormationIcon;
import mekhq.campaign.icons.StandardFormationIcon;
import mekhq.campaign.icons.enums.LayeredFormationIconLayer;
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
 * This is a hierarchical object to define formations for TO&amp;E. Each Formation object can have a parent formation object and a
 * vector of child formation objects. Each formation can also have a vector of PilotPerson objects. The idea is that any time
 * TOE is refreshed in MekHQView, the formation object can be traversed to generate a set of TreeNodes that can be applied
 * to the JTree showing the formation TO&amp;E.
 *
 * <p>Known as {@code Force} prior to 0.50.12</p>
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 * @since 0.50.12
 */
public class Formation {
    private static final MMLogger LOGGER = MMLogger.create(Formation.class);

    // region Variable Declarations
    // pathway to formation icon
    public static final int FORMATION_NONE = -1;
    /**
     * This is the id of the 'origin node'. The formation from which all other formations descend. Normally named after the
     * campaign.
     */
    public static final int FORMATION_ORIGIN = 0;

    public static final int COMBAT_TEAM_OVERRIDE_NONE = -1;
    public static final int COMBAT_TEAM_OVERRIDE_FALSE = 0;
    public static final int COMBAT_TEAM_OVERRIDE_TRUE = 1;

    public static final int NO_ASSIGNED_SCENARIO = -1;

    private String name;
    private StandardFormationIcon formationIcon;
    private Camouflage camouflage;
    private String desc;
    private FormationType formationType;
    private boolean isCombatTeam;
    private int overrideCombatTeam;
    private FormationLevel formationLevel;
    private FormationLevel overrideFormationLevel;
    private CombatRole combatRoleInMemory;
    private Formation parentFormation;
    private final Vector<Formation> subFormations;
    private final Vector<UUID> units;
    private int scenarioId;
    private UUID formationCommanderID;
    private UUID overrideFormationCommanderID;
    protected UUID techId;

    // an ID so that formations can be tracked in Campaign hash
    private int id;
    // endregion Variable Declarations

    // region Constructors
    public Formation(String name) {
        setName(name);
        setFormationIcon(new LayeredFormationIcon());
        setCamouflage(new Camouflage());
        setDescription("");
        this.formationType = FormationType.STANDARD;
        this.isCombatTeam = false;
        this.overrideCombatTeam = COMBAT_TEAM_OVERRIDE_NONE;
        this.formationLevel = FormationLevel.NONE;
        this.overrideFormationLevel = FormationLevel.NONE;
        this.combatRoleInMemory = CombatRole.FRONTLINE;
        this.parentFormation = null;
        this.subFormations = new Vector<>();
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

    public StandardFormationIcon getFormationIcon() {
        return formationIcon;
    }

    public void setFormationIcon(final StandardFormationIcon formationIcon) {
        setFormationIcon(formationIcon, false);
    }

    public void setFormationIcon(final StandardFormationIcon formationIcon, final boolean setForSubFormations) {
        this.formationIcon = formationIcon;
        if (setForSubFormations) {
            for (final Formation formation : subFormations) {
                formation.setFormationIcon(formationIcon.clone(), true);
            }
        }
    }

    public Camouflage getCamouflage() {
        return camouflage;
    }

    public Camouflage getCamouflageOrElse(final Camouflage camouflage) {
        return getCamouflage().hasDefaultCategory() ?
                     ((getParentFormation() == null) ? camouflage : getParentFormation().getCamouflageOrElse(camouflage)) :
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
     * @return The {@code FormationType} currently assigned to this instance.
     */
    public FormationType getFormationType() {
        return formationType;
    }

    /**
     * This method compares the provided {@code formationType} with the current instance's {@code FormationType} to determine if
     * they match.
     *
     * @param formationType The {@code FormationType} to compare against.
     *
     * @return {@code true} if the current instance matches the specified {@code formationType}; otherwise, {@code
     * false}.
     */
    public boolean isFormationType(FormationType formationType) {
        return this.formationType == formationType;
    }

    /**
     * Updates the {@code FormationType} for this instance and optionally propagates the change to all sub-formations.
     *
     * <p>If the {@code setForSubFormations} flag is {@code true}, the method recursively sets the
     * provided {@code formationType} for all sub-formations of this instance.</p>
     *
     * @param formationType       The new {@code FormationType} to assign to this instance.
     * @param setForSubFormations A flag indicating whether the change should also apply to sub-formations.
     */
    public void setFormationType(FormationType formationType, boolean setForSubFormations) {
        this.formationType = formationType;
        if (setForSubFormations) {
            for (Formation formation : subFormations) {
                formation.setFormationType(formationType, true);
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
     * Set scenario ID (e.g. deploy to scenario) for a formation and all of its subFormations and units
     *
     * @param scenarioId scenario to deploy to
     * @param campaign   campaign - required to update units
     */
    public void setScenarioId(int scenarioId, Campaign campaign) {
        this.scenarioId = scenarioId;
        for (Formation sub : getSubFormations()) {
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
        // formations are deployed if their parent formation is
        if ((null != parentFormation) && parentFormation.isDeployed()) {
            return true;
        }
        return scenarioId != NO_ASSIGNED_SCENARIO;
    }

    public @Nullable Formation getParentFormation() {
        return parentFormation;
    }

    /**
     * This method generates a list of all parent formations for the current formation object in the hierarchy. It repeatedly
     * fetches the parent formation of the current formation and adds it to a list until no more parent formations can be found
     * (i.e., until the top of the formation hierarchy is reached).
     *
     * @return A list of {@link Formation} objects representing all the parent formations of the current formation object in the
     *       hierarchy. The list will be empty if there are no parent formations.
     */
    public List<Formation> getAllParents() {
        List<Formation> parentFormations = new ArrayList<>();

        Formation parentFormation = this.parentFormation;

        if (this.parentFormation != null) {
            parentFormations.add(this.parentFormation);
        }

        while (parentFormation != null) {
            parentFormation = parentFormation.getParentFormation();

            if (parentFormation != null) {
                parentFormations.add(parentFormation);
            }
        }

        return parentFormations;
    }

    public void setParentFormation(final @Nullable Formation parent) {
        this.parentFormation = parent;
    }

    public Vector<Formation> getSubFormations() {
        return subFormations;
    }

    /**
     * Returns a list of all of this formations' descendant formations. This includes direct child formations and their descendents
     * recursively.
     * <p>
     * This method works by first adding all direct child formations to the list, and then recursively adding their
     * descendants by calling this method on each child formation.
     *
     * @return A list of {@link Formation} objects representing all descendant formations. If there are no descendant formations,
     *       this method will return an empty list.
     */
    public List<Formation> getAllSubFormations() {
        List<Formation> allSubFormations = new ArrayList<>();

        for (Formation subFormation : subFormations) {
            allSubFormations.add(subFormation);

            allSubFormations.addAll(subFormation.getAllSubFormations());
        }

        return allSubFormations;
    }

    public boolean isAncestorOf(Formation otherFormation) {
        Formation pFormation = otherFormation.getParentFormation();
        while (pFormation != null) {
            if (pFormation.getId() == getId()) {
                return true;
            }
            pFormation = pFormation.getParentFormation();
        }
        return false;
    }

    /**
     * @return the full hierarchical name of the formation, including all parents (except the origin formation)
     */
    public String getFullName() {
        String toReturn = getName();
        if (null != parentFormation) {
            if (parentFormation.getId() != FORMATION_ORIGIN) {
                toReturn += ", " + parentFormation.getFullName();
            }
        }
        return toReturn;
    }

    /**
     * @return A String representation of the full hierarchical formation including ID for MM export
     */
    public String getFullMMName() {
        var ancestors = new ArrayList<Formation>();
        ancestors.add(this);
        var p = parentFormation;
        while (p != null) {
            ancestors.add(p);
            p = p.parentFormation;
        }

        StringBuilder result = new StringBuilder();
        int id = 0;
        for (int i = ancestors.size() - 1; i >= 0; i--) {
            Formation ancestor = ancestors.get(i);
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
     * Add a sub formation to the sub formation vector. In general, this should not be called directly to add formations
     * to the campaign because they will not be assigned an id. Use {@link Campaign#addFormation(Formation, Formation)}
     * instead The boolean assignParent here is set to false when assigning formations from the TOE to a scenario,
     * because we don't want to switch this formations real parent
     *
     * @param sub the sub formation to add, which may be null from a load failure. This returns without adding in that
     *            case
     */
    public void addSubFormation(final @Nullable Formation sub, boolean assignParent) {
        if (sub == null) {
            return;
        }

        if (equals(sub)) {
            LOGGER.error("Cannot add a formation as its own subformation!");
            return;
        }

        if (assignParent) {
            sub.setParentFormation(this);
        }
        subFormations.add(sub);
    }

    public Vector<UUID> getUnits() {
        return units;
    }

    /**
     * @param standardFormationsOnly to only include combat formations or to also include support formations
     *
     * @return all the unit ids in this formation and all of its subFormations
     */
    public Vector<UUID> getAllUnits(boolean standardFormationsOnly) {
        Vector<UUID> allUnits = new Vector<>();

        if (!standardFormationsOnly || formationType.isStandard()) {
            allUnits.addAll(units);
        }

        for (Formation formation : subFormations) {
            allUnits.addAll(formation.getAllUnits(standardFormationsOnly));
        }

        return allUnits;
    }

    /**
     * Retrieves all units associated with the current formation as {@link Unit} objects.
     *
     * <p>This method converts the list of unit IDs from the formation into a list of
     * {@link Unit} objects by fetching them from the provided {@link Hangar}. Units are only included if they can be
     * successfully resolved from the hangar.</p>
     *
     * @param hangar             the {@link Hangar} containing the units to retrieve.
     * @param standardFormationsOnly a flag indicating whether to include only standard formations. If {@code true}, only units
     *                           belonging to standard formations are returned.
     *
     * @return a list of {@link Unit} objects associated with the formation.
     */
    public List<Unit> getAllUnitsAsUnits(Hangar hangar, boolean standardFormationsOnly) {
        List<Unit> allUnits = new ArrayList<>();

        for (UUID unitId : getAllUnits(standardFormationsOnly)) {
            Unit unit = hangar.getUnit(unitId);
            if (unit != null) {
                allUnits.add(unit);
            }
        }

        return allUnits;
    }

    /**
     * Resolves and returns the {@link Unit} objects that belong to this formation by looking them up in the provided
     * {@link Hangar}.
     *
     * <p>This method iterates over the unit IDs returned by {@link #getUnits()} and attempts to retrieve each unit
     * from the hangar via {@link Hangar#getUnit(UUID)}. Any IDs that do not resolve to a unit (i.e.,
     * {@code getUnit(...)} returns {@code null}) are ignored.</p>
     *
     * <p>The returned list contains only non-null units and preserves the iteration order of {@link #getUnits()}.</p>
     *
     * @param hangar the {@link Hangar} used to resolve unit IDs into {@link Unit} instances; must not be {@code null}
     *
     * @return a list of resolved {@link Unit} instances for this formation; never {@code null}
     *
     * @author Illiani
     * @since 0.50.11
     */
    public List<Unit> getUnitsAsUnits(Hangar hangar) {
        List<Unit> allUnits = new ArrayList<>();

        for (UUID unitId : getUnits()) {
            Unit unit = hangar.getUnit(unitId);
            if (unit != null) {
                allUnits.add(unit);
            }
        }

        return allUnits;
    }

    /**
     * Add a unit id to the units vector. In general, this should not be called directly to add unid because they will
     * not be assigned a formation id. Use {@link Campaign#addUnitToFormation(Unit, int)} instead
     */
    public void addUnit(UUID uid) {
        addUnit(null, uid, false, null);
    }

    public void addUnit(Campaign campaign, UUID uid, boolean useTransfers, Formation oldFormation) {
        units.add(uid);

        if (campaign == null) {
            return;
        }

        Unit unit = campaign.getUnit(uid);
        if (unit != null) {
            for (Person person : unit.getCrew()) {
                if (useTransfers) {
                    AssignmentLogger.reassignedTOEFormation(campaign, person, campaign.getLocalDate(), oldFormation, this);
                } else {
                    AssignmentLogger.addedToTOEFormation(campaign, person, campaign.getLocalDate(), this);
                }
            }
        }

        updateCommander(campaign);
    }

    /**
     * This should not be directly called except by {@link Campaign#removeUnitFromFormation(Unit)} instead
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
                        AssignmentLogger.removedFromTOEFormation(campaign, person, campaign.getLocalDate(), this);
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
            // We only need to clear the subFormations if we're killing everything.
            for (Formation sub : getSubFormations()) {
                Scenario s = c.getScenario(sub.getScenarioId());
                if (s != null) {
                    s.removeFormation(sub.getId());
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

    public @Nullable UUID getFormationCommanderID() {
        return formationCommanderID;
    }

    /**
     * Sets the formation commander ID to the provided UUID. You probably want to use setOverrideFormationCommanderID(UUID)
     * followed by updateCommander(campaign).
     *
     * @param commanderID UUID of the commander
     *
     * @see #setOverrideFormationCommanderID(UUID)
     * @see #updateCommander(Campaign)
     */
    private void setFormationCommanderID(@Nullable UUID commanderID) {
        formationCommanderID = commanderID;
    }

    public UUID getOverrideFormationCommanderID() {
        return overrideFormationCommanderID;
    }

    public void setOverrideFormationCommanderID(UUID overrideFormationCommanderID) {
        this.overrideFormationCommanderID = overrideFormationCommanderID;
    }

    /**
     * Returns a list of unit or formation commanders eligible to be considered for the position of formation commander.
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

        // this means the formation contains no Units, so we check against the leaders of
        // the sub-formations
        if (eligibleCommanders.isEmpty()) {
            for (Formation formation : getSubFormations()) {
                UUID formationCommander = formation.getFormationCommanderID();

                if (formationCommander != null) {
                    eligibleCommanders.add(formationCommander);
                }
            }
        }

        return eligibleCommanders;
    }

    /**
     * Updates the commander for a formation based on the ranking of eligible commanders.
     *
     * @param campaign the current campaign
     */
    public void updateCommander(Campaign campaign) {
        List<UUID> eligibleCommanders = getEligibleCommanders(campaign);

        if (eligibleCommanders.isEmpty()) {
            formationCommanderID = null;
            overrideFormationCommanderID = null;
            updateCombatTeamCommanderIfCombatTeam(campaign);
            return;
        }

        if (overrideFormationCommanderID != null) {
            if (eligibleCommanders.contains(overrideFormationCommanderID)) {
                formationCommanderID = overrideFormationCommanderID;
                updateCombatTeamCommanderIfCombatTeam(campaign);

                if (getParentFormation() != null) {
                    getParentFormation().updateCommander(campaign);
                }
                return;
            } else {
                overrideFormationCommanderID = null;
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
            LOGGER.info("Formation {} has no eligible commanders", getName());
            formationCommanderID = null;
        } else {
            formationCommanderID = highestRankedPerson.getId();
        }

        updateCombatTeamCommanderIfCombatTeam(campaign);

        if (getParentFormation() != null) {
            getParentFormation().updateCommander(campaign);
        }
    }

    private void updateCombatTeamCommanderIfCombatTeam(Campaign campaign) {
        if (isCombatTeam()) {
            CombatTeam combatTeam = campaign.getCombatTeamsAsMap().getOrDefault(getId(), null);
            if (combatTeam != null) {
                combatTeam.setCommander(getFormationCommanderID());
            }
        }
    }

    public void removeSubFormation(int id) {
        int idx = 0;
        boolean found = false;
        for (Formation sformation : getSubFormations()) {
            if (sformation.getId() == id) {
                found = true;
                break;
            }
            idx++;
        }
        if (found) {
            subFormations.remove(idx);
        }
    }

    /**
     * This determines the proper operational status icon to use for this formation and sets it.
     *
     * @param campaign the campaign to determine the operational status of this formation using
     *
     * @return a list of the operational statuses for units in this formation and in all of its subFormations.
     */
    public List<OperationalStatus> updateFormationIconOperationalStatus(final Campaign campaign) {
        // First, update all subFormations, collecting their unit statuses into a single
        // list
        final List<OperationalStatus> statuses = getSubFormations().stream()
                                                       .flatMap(subFormation -> subFormation.updateFormationIconOperationalStatus(
                                                             campaign).stream())
                                                       .collect(Collectors.toList());

        // Then, Add the units assigned to this formation
        statuses.addAll(getUnits().stream()
                              .map(campaign::getUnit)
                              .filter(Objects::nonNull)
                              .map(OperationalStatus::determineLayeredFormationIconOperationalStatus)
                              .toList());

        // Can only update the icon for LayeredFormationIcons, but still need to return the
        // processed
        // units for parent formation updates
        if (!(getFormationIcon() instanceof LayeredFormationIcon)) {
            return statuses;
        }

        if (statuses.isEmpty()) {
            // No special modifier for empty formations
            ((LayeredFormationIcon) getFormationIcon()).getPieces().remove(LayeredFormationIconLayer.SPECIAL_MODIFIER);
        } else {
            // Sum the unit status ordinals, then divide by the overall number of statuses,
            // to get
            // the ordinal of the formation's status. Then assign the operational status to
            // this.
            final int index = (int) round(statuses.stream().mapToInt(Enum::ordinal).sum() / (statuses.size() * 1.0));
            final OperationalStatus status = OperationalStatus.values()[index];
            ((LayeredFormationIcon) getFormationIcon()).getPieces()
                  .put(LayeredFormationIconLayer.SPECIAL_MODIFIER, new ArrayList<>());
            ((LayeredFormationIcon) getFormationIcon()).getPieces()
                  .get(LayeredFormationIconLayer.SPECIAL_MODIFIER)
                  .add(new FormationPieceIcon(LayeredFormationIconLayer.SPECIAL_MODIFIER,
                        MekHQ.getMHQOptions().getNewDayFormationIconOperationalStatusStyle().getPath(),
                        status.getFilename()));
        }

        return statuses;
    }

    public void writeToXML(PrintWriter pw1, int indent) {
        pw1.println(MHQXMLUtility.indentStr(indent++) +
                          "<formation id=\"" +
                          id +
                          "\" type=\"" +
                          this.getClass().getName() +
                          "\">");
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "name", name);
        getFormationIcon().writeToXML(pw1, indent);
        getCamouflage().writeToXML(pw1, indent);
        if (!getDescription().isBlank()) {
            MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "desc", desc);
        }
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "formationType", formationType.ordinal());
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "overrideCombatTeam", overrideCombatTeam);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "formationLevel", formationLevel.toString());
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "overrideFormationLevel", overrideFormationLevel.toString());
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "preferredRole", combatRoleInMemory.name());
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "scenarioId", scenarioId);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "techId", techId);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "overrideFormationCommanderID", overrideFormationCommanderID);
        MHQXMLUtility.writeSimpleXMLTag(pw1, indent, "formationCommanderID", formationCommanderID);
        if (!units.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw1, indent++, "units");
            for (UUID uid : units) {
                pw1.println(MHQXMLUtility.indentStr(indent) + "<unit id=\"" + uid + "\"/>");
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw1, --indent, "units");
        }

        if (!subFormations.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw1, indent++, "subFormations");
            for (Formation sub : subFormations) {
                sub.writeToXML(pw1, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw1, --indent, "subFormations");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw1, --indent, "formation");
    }

    public static @Nullable Formation generateInstanceFromXML(Node workingNode, Campaign campaign, Version version) {
        Formation formation = new Formation("");
        NamedNodeMap attributes = workingNode.getAttributes();
        Node idNameNode = attributes.getNamedItem("id");
        String idString = idNameNode.getTextContent();

        try {
            NodeList childNodes = workingNode.getChildNodes();
            formation.id = Integer.parseInt(idString);

            for (int x = 0; x < childNodes.getLength(); x++) {
                Node wn2 = childNodes.item(x);
                if (wn2.getNodeName().equalsIgnoreCase("name")) {
                    formation.setName(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase(StandardFormationIcon.XML_TAG)) {
                    formation.setFormationIcon(StandardFormationIcon.parseFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase(LayeredFormationIcon.XML_TAG)) {
                    formation.setFormationIcon(LayeredFormationIcon.parseFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase(Camouflage.XML_TAG)) {
                    formation.setCamouflage(Camouflage.parseFromXML(wn2));
                } else if (wn2.getNodeName().equalsIgnoreCase("desc")) {
                    formation.setDescription(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("formationType") ||
                                 wn2.getNodeName().equalsIgnoreCase("forceType")) {
                    formation.setFormationType(FormationType.fromKey(Integer.parseInt(wn2.getTextContent().trim())),
                          false);
                } else if (wn2.getNodeName().equalsIgnoreCase("overrideCombatTeam")) {
                    formation.setOverrideCombatTeam(Integer.parseInt(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("formationLevel") || wn2.getNodeName().equalsIgnoreCase(
                      "forceLevel")) {
                    formation.setFormationLevel(FormationLevel.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("overrideForceLevel") || wn2.getNodeName().equalsIgnoreCase(
                      "overrideFormationLevel")) {
                    formation.setOverrideFormationLevel(FormationLevel.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("preferredRole")) {
                    formation.setCombatRoleInMemory(CombatRole.parseFromString(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("scenarioId")) {
                    formation.scenarioId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("techId")) {
                    formation.techId = UUID.fromString(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("overrideFormationCommanderId") ||
                                 wn2.getNodeName().equalsIgnoreCase(
                                       "overrideForceCommanderID")) {
                    formation.overrideFormationCommanderID = UUID.fromString(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("formationCommanderId") ||
                                 wn2.getNodeName().equalsIgnoreCase("forceCommanderID")) {
                    formation.formationCommanderID = UUID.fromString(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("units")) {
                    processUnitNodes(formation, wn2, version);
                } else if (wn2.getNodeName().equalsIgnoreCase("subFormations") || wn2.getNodeName().equalsIgnoreCase(
                      "subForces")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int y = 0; y < nl2.getLength(); y++) {
                        Node wn3 = nl2.item(y);
                        // If it's not an element node, we ignore it.
                        if (wn3.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        if (!wn3.getNodeName().equalsIgnoreCase("formation") && !wn3.getNodeName().equalsIgnoreCase(
                              "force")) {
                            String message = String.format("Unknown node type not loaded in Formations nodes: %s",
                                  wn3.getNodeName());
                            LOGGER.error(message);
                            continue;
                        }

                        formation.addSubFormation(generateInstanceFromXML(wn3, campaign, version), true);
                    }
                }
            }
            campaign.importFormation(formation);
        } catch (Exception ex) {
            LOGGER.error("", ex);
            return null;
        }

        return formation;
    }

    private static void processUnitNodes(Formation retVal, Node wn, Version version) {
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
     * Returns a vector containing all children of this formation, including sub-formations and units, sorted by commander
     * rank.
     *
     * <p>This method gathers all subordinate objects belonging to this formation: first, it adds all sub-formations, then it
     * adds all units under this formation (with crewed units before uncrewed units), finally sorting crewed units by their
     * commander's numeric rank in descending order.</p>
     *
     * @param campaign The {@link Campaign} instance used to look up unit and personnel data.
     *
     * @return A {@link Vector} of all child objects, including sub-formations and units, sorted such that crewed units
     *       appear before unmanned units, and crewed units are ordered by their commander's rank descending.
     */
    public Vector<Object> getAllChildren(Campaign campaign) {
        Vector<Object> children = new Vector<>(subFormations);
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
        return (o instanceof Formation) && (((Formation) o).getId() == id) && ((Formation) o).getFullName().equals(getFullName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getFullName());
    }

    /**
     * Calculates the formation's total BV, including sub formations.
     *
     * @param campaign                 The working campaign. This is the campaign object that the formation belongs to.
     * @param formationStandardBattleValue Flag indicating whether to override campaign settings that call for the use of
     *                                 Generic BV
     *
     * @return The total battle value (BV) of the formation.
     */
    public int getTotalBV(Campaign campaign, boolean formationStandardBattleValue) {
        int bvTotal = 0;

        for (UUID unitId : getAllUnits(false)) {
            // no idea how this would happen, but sometimes a unit in a formations unit ID list
            // has an invalid ID?
            if (campaign.getUnit(unitId) == null) {
                continue;
            }

            if (campaign.getCampaignOptions().isUseGenericBattleValue() && !formationStandardBattleValue) {
                bvTotal += campaign.getUnit(unitId).getEntity().getGenericBattleValue();
            } else {
                bvTotal += campaign.getUnit(unitId).getEntity().calculateBattleValue();
            }
        }

        return bvTotal;
    }

    /**
     * Calculates the total count of units in the given formation, including all sub formations.
     *
     * @param campaign      the current campaign
     * @param isClanBidding flag to indicate whether clan bidding is being performed
     *
     * @return the total count of units in the formation (including sub formations)
     */
    public int getTotalUnitCount(Campaign campaign, boolean isClanBidding) {
        int unitTotal = 0;

        for (Formation subFormation : getSubFormations()) {
            unitTotal += subFormation.getTotalUnitCount(campaign, isClanBidding);
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
     * Calculates the unit type most represented in this formation and all subFormations.
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
     * Finds the distance (depth) from the origin formation
     *
     * @param formation the formation to get depth for
     *
     * @deprecated
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
    public static int getDepth(Formation formation) {
        int depth = 0;

        Formation parent = formation.getParentFormation();

        while (parent != null) {
            depth++;
            formation = parent;
            parent = formation.getParentFormation();
        }

        return depth;
    }

    /**
     * Uses a recursive search to find the maximum distance (depth) from the origin formation
     *
     * @param formation the current formation. Should always equal campaign.getFormation(0), if called remotely
     * @param depth the current recursive depth.
     *
     * @deprecated
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
    public static int getMaximumDepth(Formation formation, Integer depth) {
        int maximumDepth = depth;

        for (Formation subFormation : formation.getSubFormations()) {
            int nextDepth = getMaximumDepth(subFormation, depth + 1);

            if (nextDepth > maximumDepth) {
                maximumDepth = nextDepth;
            }
        }

        return maximumDepth;
    }

    /**
     * Populates the formation levels of a formation hierarchy starting from the origin formation. For all subformations, it will
     * determine the smallest formations - Teams/Lances - and then parent formations will be one formation higher.
     *
     * @param campaign campaign that the formation belongs to
     */
    public static void populateFormationLevelsFromOrigin(Campaign campaign) {
        Formation formation = campaign.getFormation(0);

        recursivelyUpdateFormationLevel(campaign, formation);

        MekHQ.triggerEvent(new OrganizationChangedEvent(formation));
    }

    private static void recursivelyUpdateFormationLevel(Campaign campaign, Formation formation) {
        for (Formation subFormation : formation.getSubFormations()) {
            recursivelyUpdateFormationLevel(campaign, subFormation);
        }
        formation.defaultFormationLevelForFormation(campaign);
    }

    /**
     * Based on a formation's subformations and units, set this unit's formation to the default.
     *
     * @param campaign campaign that the formation belongs to
     */
    public void defaultFormationLevelForFormation(Campaign campaign) {
        Formation largestSubFormation =
              getAllSubFormations().stream().max(Comparator.comparing(f -> f.getFormationLevel().getDepth())).orElse(null);
        if (largestSubFormation == null) {
            int depth = 1;
            setFormationLevel(FormationLevel.parseFromDepth(campaign, depth + getOddFormationSizeModifier(campaign,
                  depth)));
        } else {
            int depth = largestSubFormation.getFormationLevel().getDepth();
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
     * Changes the formation level of a formation and its sub-formations.
     *
     * @param formation                 the formation whose formation level is to be changed
     * @param currentFormationLevel the current formation level of the formation
     * @param lowerBoundary         the lower boundary for the formation level
     *
     * @deprecated See {@link Formation#recursivelyUpdateFormationLevel}
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
    private static void changeFormationLevel(Formation formation, int currentFormationLevel, int lowerBoundary) {
        for (Formation subFormation : formation.getSubFormations()) {
            if (currentFormationLevel - 1 < lowerBoundary) {
                subFormation.setFormationLevel(FormationLevel.INVALID);
            } else if (subFormation.getSubFormations().isEmpty()) {
                subFormation.setFormationLevel(FormationLevel.parseFromInt(lowerBoundary));
            } else {
                subFormation.setFormationLevel(FormationLevel.parseFromInt(currentFormationLevel - 1));
            }

            MekHQ.triggerEvent(new OrganizationChangedEvent(formation));

            changeFormationLevel(subFormation, currentFormationLevel - 1, lowerBoundary);
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
     * Populates the origin node (the formation normally named after the campaign) with an appropriate Formation Level.
     *
     * @param campaign the current campaign
     * @param origin   the origin node
     *
     * @return the parsed integer value of the origin node's formation level
     *
     * @deprecated See {@link Formation#recursivelyUpdateFormationLevel}
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
    private static int populateOriginNode(Campaign campaign, Formation origin) {
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
     * Determines whether a formation consists solely of VTOL (Vertical Take-Off and Landing) or WIGE (Wing in Ground
     * Effect) units.
     *
     * <p>This method evaluates the formation by checking each unit to verify that all resolved units
     * are either VTOLs or WIGE units.</p>
     *
     * <p><strong>Behavior:</strong></p>
     * <ul>
     *   <li>Retrieves all units in the formation based on the {@code standardFormationsOnly} flag.</li>
     *   <li>Skips any unit that cannot be resolved (null entity).</li>
     *   <li>Returns {@code false} if any resolved unit is not categorized as a VTOL or WIGE unit.</li>
     *   <li>Returns {@code true} if all resolved units meet the VTOL or WIGE criteria.</li>
     * </ul>
     *
     * @param hangar             The {@link Hangar} instance from which to retrieve the {@link Unit}.
     * @param standardFormationsOnly A flag to filter and include only standard formations from the formation.
     *
     * @return {@code true} if all resolved units in the formation are VTOL or WIGE units, {@code false} otherwise.
     */
    public boolean formationContainsOnlyVTOLForces(Hangar hangar, boolean standardFormationsOnly) {
        for (UUID unitId : getAllUnits(standardFormationsOnly)) {
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
     * Determines whether a formation contains a majority of VTOL (Vertical Take-Off and Landing) or WIGE (Wing in Ground
     * Effect) units.
     *
     * <p>This method evaluates the formation by calculating whether at least half of the resolved units
     * in the formation are categorized as VTOLs or WIGE units.</p>
     *
     * <p><strong>Behavior:</strong></p>
     * <ul>
     *   <li>Retrieves all units in the formation based on the {@code standardFormationsOnly} flag.</li>
     *   <li>Counts the number of units categorized as airborne VTOL or WIGE.</li>
     *   <li>Adjusts the total formation size if unresolved (null) entities are skipped without counting
     *       them toward the total size.</li>
     *   <li>Stops counting early if a majority of VTOL or WIGE units is determined before evaluating
     *       all entities.</li>
     * </ul>
     *
     * @param hangar             The {@link Hangar} instance from which to retrieve the {@link Unit}.
     * @param standardFormationsOnly A flag to filter and include only standard formations from the formation.
     *
     * @return {@code true} if VTOL or WIGE units constitute at least half of the resolved formation units, {@code false}
     *       otherwise.
     */
    public boolean formationContainsMajorityVTOLForces(Hangar hangar, boolean standardFormationsOnly) {
        Vector<UUID> allUnits = getAllUnits(standardFormationsOnly);
        int formationSize = allUnits.size();
        int vtolCount = 0;

        for (UUID unitId : allUnits) {
            Entity entity = getEntityFromUnitId(hangar, unitId);

            if (entity == null) {
                formationSize--;
                continue;
            }

            if (entity.isAirborneVTOLorWIGE()) {
                vtolCount++;
            }

            if (vtolCount >= formationSize / 2) {
                break;
            }
        }

        return vtolCount >= floor((double) formationSize / 2);
    }

    /**
     * Determines whether a formation contains only aerospace or conventional fighters.
     *
     * <p>This method checks all units in the formation to confirm if they consist exclusively of aerial
     * units based on their type.</p>
     *
     * <p><strong>Behavior:</strong></p>
     * <ul>
     *   <li>Filters the formation's units based on the {@code standardFormationsOnly} flag.</li>
     *   <li>Iterates through all selected units in the formation.</li>
     *   <li>Skips any unit that cannot be resolved (null entity).</li>
     *   <li>If {@code excludeConventionalFighters} is {@code true} and the formation contains any
     *       conventional fighters, the method immediately returns {@code false}.</li>
     *   <li>Returns {@code false} if any unit in the formation is not an aerial unit (i.e., not an aerospace
     *       unit or conventional fighter).</li>
     *   <li>Returns {@code true} if all units in the formation meet the aerial unit criteria.</li>
     * </ul>
     *
     * @param hangar                      The {@link Hangar} instance from which to retrieve the {@link Unit}.
     * @param standardFormationsOnly          A flag to filter and include only standard formations from the formation.
     * @param excludeConventionalFighters A flag determining if conventional fighters should be excluded from the
     *                                    assessment.
     *
     * @return {@code true} if the formation consists only of aerial units (respecting the provided filters), {@code false}
     *       otherwise.
     */
    public boolean formationContainsOnlyAerialForces(Hangar hangar, boolean standardFormationsOnly,
          boolean excludeConventionalFighters) {
        for (UUID unitId : getAllUnits(standardFormationsOnly)) {
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
        List<Unit> unitsInFormation = getAllUnitsAsUnits(hangar, false);

        int unitCount = 0;
        for (Unit unit : unitsInFormation) {
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
