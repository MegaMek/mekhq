/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission;

import static mekhq.campaign.mission.AtBDynamicScenarioFactory.getPlanetOwnerAlignment;
import static mekhq.campaign.mission.AtBDynamicScenarioFactory.getPlanetOwnerFaction;
import static mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment.Allied;
import static mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment.PlanetOwner;

import java.io.PrintWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.Version;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceGenerationMethod;
import mekhq.campaign.mission.atb.AtBScenarioModifier;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Data structure intended to hold data relevant to AtB Dynamic Scenarios (AtB 3.0)
 * @author NickAragua
 */
public class AtBDynamicScenario extends AtBScenario {
    /**
     * Data relevant to an entity that was swapped out in a "player or fixed unit count" force.
     */
    public static class BenchedEntityData {
        public Entity entity;
        public String templateName;
    }

    // by convention, this is the ID specified in the template for the primary player force
    public static final String PRIMARY_PLAYER_FORCE_ID = "Player";

    private static final String PLAYER_UNIT_SWAPS_ELEMENT = "PlayerUnitSwaps";
    private static final String PLAYER_UNIT_SWAP_ELEMENT = "PlayerUnitSwap";
    private static final String PLAYER_UNIT_SWAP_ID_ELEMENT = "UnitID";
    private static final String PLAYER_UNIT_SWAP_TEMPLATE_ELEMENT = "Template";
    private static final String PLAYER_UNIT_SWAP_ENTITY_ELEMENT = "entity";

    private ScenarioTemplate template; // the template that is being used to generate this scenario

    private double effectivePlayerUnitCountMultiplier;
    private double effectivePlayerBVMultiplier; // Additive multiplier

    private int friendlyReinforcementDelayReduction;
    private List<UUID> friendlyDelayedReinforcements;
    private List<UUID> friendlyInstantReinforcements;
    private int hostileReinforcementDelayReduction;

    // derived fields used for various calculations
    private SkillLevel effectiveOpforSkill;
    private int effectiveOpforQuality;

    // map of player unit external ID to bot unit external ID where the bot unit was swapped out.
    private Map<UUID, BenchedEntityData> playerUnitSwaps;

    private boolean finalized;

    // convenient pointers that let us keep data around that would otherwise need reloading
    private transient Map<BotForce, ScenarioForceTemplate> botForceTemplates;
    private transient Map<UUID, ScenarioForceTemplate> botUnitTemplates;
    private transient Map<Integer, ScenarioForceTemplate> playerForceTemplates;
    private transient Map<UUID, ScenarioForceTemplate> playerUnitTemplates;
    private transient List<AtBScenarioModifier> scenarioModifiers;

    private static final MMLogger logger = MMLogger.create(AtBDynamicScenario.class);


    public AtBDynamicScenario() {
        super();

        setTemplate(null);
        setEffectivePlayerUnitCountMultiplier(0.0);
        setEffectivePlayerBVMultiplier(0.0);
        setFriendlyReinforcementDelayReduction(0);
        setFriendlyDelayedReinforcements(new ArrayList<>());
        setFriendlyInstantReinforcements(new ArrayList<>());
        setHostileReinforcementDelayReduction(0);
        setEffectiveOpforSkill(SkillLevel.REGULAR);
        setEffectiveOpforQuality(IUnitRating.DRAGOON_C);
        setPlayerUnitSwaps(new HashMap<>());
        setFinalized(false);
        setBotForceTemplates(new HashMap<>());
        setBotUnitTemplates(new HashMap<>());
        setPlayerForceTemplates(new HashMap<>());
        setPlayerUnitTemplates(new HashMap<>());
        setScenarioModifiers(new ArrayList<>());
    }

    @Override
    public void addForces(int forceID) {
        super.addForces(forceID);

        // loop through all player-supplied forces in the template, if there is one
        // assign the newly-added force to the first template we find
        if (template != null) {
            for (ScenarioForceTemplate forceTemplate : template.getAllScenarioForces()) {
                if ((forceTemplate.getGenerationMethod() == ForceGenerationMethod.PlayerSupplied.ordinal()) &&
                        !playerForceTemplates.containsValue(forceTemplate)) {
                    playerForceTemplates.put(forceID, forceTemplate);
                    return;
                }
            }
        }

        playerForceTemplates.put(forceID, null);
    }

    /**
     * Add a force to the scenario, explicitly linked to the given template.
     * @param forceID ID of the force to add.
     * @param templateName Name of the force template.
     */
    public void addForce(int forceID, String templateName) {
        // if we're not supplied a template name, fall back to trying to automatically place the force
        if (StringUtils.isEmpty(templateName)) {
            addForces(forceID);
            return;
        }

        final ScenarioForceTemplate forceTemplate = template.getScenarioForces().get(templateName);
        if (forceTemplate != null) {
            super.addForces(forceID);
            playerForceTemplates.put(forceID, forceTemplate);
        }
    }

    public void addUnit(UUID unitID, String templateName) {
        super.addUnit(unitID);
        ScenarioForceTemplate forceTemplate = template.getScenarioForces().get(templateName);
        playerUnitTemplates.put(unitID, forceTemplate);
        AtBDynamicScenarioFactory.benchAllyUnit(unitID, templateName, this);
    }

    @Override
    public void removeForce(int fid) {
        super.removeForce(fid);
        playerForceTemplates.remove(fid);
    }

    @Override
    public void removeUnit(UUID unitID) {
        super.removeUnit(unitID);
        AtBDynamicScenarioFactory.unbenchAttachedAlly(unitID, this);
        playerUnitTemplates.remove(unitID);
    }

    /**
     * The Board.START_X constant representing the starting zone for the player's primary force
     */
    @Override
    public int getStartingPos() {
        // If we've assigned at least one force
        // and there's a player force template associated with the first force
        // then return the generated deployment zone associated with the first force
        if (!getForceIDs().isEmpty() &&
                playerForceTemplates.containsKey(getForceIDs().get(0))) {
            return playerForceTemplates.get(getForceIDs().get(0)).getActualDeploymentZone();
        }

        return super.getStartingPos();
    }

    /**
     * Horizontal map size.
     * Unlike the AtBScenario, we only perform map size calculations once (once all primary forces are committed),
     * so we don't re-calculate the map size each time.
     */
    @Override
    public int getMapX() {
        return getMapSizeX();
    }

    /**
     * Vertical map size.
     * Unlike the AtBScenario, we only perform map size calculations once (once all primary forces are committed),
     * so we don't re-calculate the map size each time.
     */
    @Override
    public int getMapY() {
        return getMapSizeY();
    }

    @Override
    public void setMapSize() {
        AtBDynamicScenarioFactory.setScenarioMapSize(this);
    }

    /**
     * Adds a bot force to this scenario.
     */
    public void addBotForce(BotForce botForce, ScenarioForceTemplate forceTemplate, Campaign c) {
        botForce.setTemplateName(forceTemplate.getForceName());
        super.addBotForce(botForce, c);
        botForceTemplates.put(botForce, forceTemplate);

        // put all bot units into the external ID lookup.
        for (Entity entity : botForce.getFullEntityList(c)) {
            getExternalIDLookup().put(entity.getExternalIdAsString(), entity);
        }
    }

    /**
     * Removes a bot force from this dynamic scenario, and its associated template as well.
     */
    @Override
    public void removeBotForce(int x) {
        // safety check, just in case
        if ((x >= 0) && (x < getBotForces().size())) {
            BotForce botToRemove = getBotForces().get(x);

            botForceTemplates.remove(botToRemove);
        }

        super.removeBotForce(x);
    }

    public double getEffectivePlayerUnitCountMultiplier() {
        return effectivePlayerUnitCountMultiplier;
    }

    public void setEffectivePlayerUnitCountMultiplier(double multiplier) {
        effectivePlayerUnitCountMultiplier = multiplier;
    }

    public double getEffectivePlayerBVMultiplier() {
        return effectivePlayerBVMultiplier;
    }

    public void setEffectivePlayerBVMultiplier(double multiplier) {
        effectivePlayerBVMultiplier = multiplier;
    }

    public @Nullable ScenarioTemplate getTemplate() {
        return template;
    }

    public void setTemplate(final @Nullable ScenarioTemplate template) {
        this.template = template;
    }

    public Map<Integer, ScenarioForceTemplate> getPlayerForceTemplates() {
        return playerForceTemplates;
    }

    public void setPlayerForceTemplates(Map<Integer, ScenarioForceTemplate> playerForceTemplates) {
        this.playerForceTemplates = playerForceTemplates;
    }

    public Map<UUID, ScenarioForceTemplate> getPlayerUnitTemplates() {
        return playerUnitTemplates;
    }

    public void setPlayerUnitTemplates(Map<UUID, ScenarioForceTemplate> playerUnitTemplates) {
        this.playerUnitTemplates = playerUnitTemplates;
    }

    public Map<BotForce, ScenarioForceTemplate> getBotForceTemplates() {
        return botForceTemplates;
    }

    public void setBotForceTemplates(Map<BotForce, ScenarioForceTemplate> botForceTemplates) {
        this.botForceTemplates = botForceTemplates;
    }

    public Map<UUID, ScenarioForceTemplate> getBotUnitTemplates() {
        return botUnitTemplates;
    }

    public void setBotUnitTemplates(Map<UUID, ScenarioForceTemplate> botUnitTemplates) {
        this.botUnitTemplates = botUnitTemplates;
    }

    public Map<UUID, BenchedEntityData> getPlayerUnitSwaps() {
        return playerUnitSwaps;
    }

    public void setPlayerUnitSwaps(Map<UUID, BenchedEntityData> playerUnitSwaps) {
        this.playerUnitSwaps = playerUnitSwaps;
    }

    public SkillLevel getEffectiveOpforSkill() {
        return effectiveOpforSkill;
    }

    public void setEffectiveOpforSkill(SkillLevel skillLevel) {
        effectiveOpforSkill = skillLevel;
    }

    public int getEffectiveOpforQuality() {
        return effectiveOpforQuality;
    }

    public void setEffectiveOpforQuality(int qualityLevel) {
        effectiveOpforQuality = qualityLevel;
    }

    public List<UUID> getFriendlyDelayedReinforcements() {
        return friendlyDelayedReinforcements;
    }

    public void setFriendlyDelayedReinforcements(final List<UUID> friendlyDelayedReinforcements) {
        this.friendlyDelayedReinforcements = friendlyDelayedReinforcements;
    }

    public List<UUID> getFriendlyInstantReinforcements() {
        return friendlyInstantReinforcements;
    }

    public void setFriendlyInstantReinforcements(final List<UUID> friendlyInstantReinforcements) {
        this.friendlyInstantReinforcements = friendlyInstantReinforcements;
    }

    public int getFriendlyReinforcementDelayReduction() {
        return friendlyReinforcementDelayReduction;
    }

    public void setFriendlyReinforcementDelayReduction(int friendlyReinforcementDelayReduction) {
        this.friendlyReinforcementDelayReduction = friendlyReinforcementDelayReduction;
    }

    public int getHostileReinforcementDelayReduction() {
        return hostileReinforcementDelayReduction;
    }

    public void setHostileReinforcementDelayReduction(int hostileReinforcementDelayReduction) {
        this.hostileReinforcementDelayReduction = hostileReinforcementDelayReduction;
    }

    /**
     * This is used to indicate that player forces have been assigned to this scenario
     * and that AtBDynamicScenarioFactory.finalizeScenario() has been called on this scenario to
     * generate opposing forces and their bots, apply any present scenario modifiers,
     * set up deployment turns, calculate which units belong to which objectives, and many other things.
     * <p>
     * Further "post-force-generation" modifiers can be applied to this scenario, but calling
     * finalizeScenario() on it again will lead to "unsupported" behavior.
     * <p>
     * Can be called as a short hand way of telling "is this scenario ready to play".
     */
    public boolean isFinalized() {
        return finalized;
    }

    public void setFinalized(boolean finalized) {
        this.finalized = finalized;
    }

    /**
     * A list of all the force IDs associated with pre-defined scenario templates
     */
    public List<Integer> getPlayerTemplateForceIDs() {
        List<Integer> retval = new ArrayList<>();

        for (int forceID : getForceIDs()) {
            if (getPlayerForceTemplates().containsKey(forceID)) {
                retval.add(forceID);
            }
        }

        return retval;
    }

    /**
     * Convenience method that returns the commander of the first force assigned to this scenario.
     * @return
     */
    public Person getLanceCommander(Campaign campaign) {
        if (getForceIDs().isEmpty()) {
            return null; // if we don't have forces, just a bunch of units, then get the highest-ranked?
        }

        CombatTeam combatTeam = campaign.getCombatTeamsTable().get(getForceIDs().get(0));

        if (combatTeam != null) {
            combatTeam.refreshCommander(campaign);
            return combatTeam.getCommander(campaign);
        } else {
            return null;
        }
    }

    /**
     * Convenience method to return the int value of the lance commander's skill in the specified area.
     * Encapsulates a fairly obnoxious number of null checks and other safety code.
     * @param skillType The type of skill to check
     * @param campaign The campaign the lance commander is a part of
     * @return The skill level. SKILL_NONE (0) if not present.
     */
    public int getLanceCommanderSkill(String skillType, Campaign campaign) {
        Person commander = getLanceCommander(campaign);
        int skillValue = SkillType.SKILL_NONE;

        if ((commander != null) &&
                commander.hasSkill(skillType)) {
            skillValue = commander.getSkill(skillType)
                               .getTotalSkillLevel(commander.getOptions(), commander.getATOWAttributes());
        }

        return skillValue;
    }

    public void setScenarioModifiers(List<AtBScenarioModifier> scenarioModifiers) {
        this.scenarioModifiers = new ArrayList<>();
        Collections.copy(this.scenarioModifiers, scenarioModifiers);
    }

    public List<AtBScenarioModifier> getScenarioModifiers() {
        return scenarioModifiers;
    }

    /**
     * Adds a scenario modifier and any linked modifiers to this scenario,
     * provided that the modifier exists and can be applied to the scenario (e.g. ground units on air map)
     */
    public void addScenarioModifier(@Nullable AtBScenarioModifier modifier) {
        if (modifier == null) {
            return;
        }

        // the default is that this modifier is allowed to apply to any map
        if ((modifier.getAllowedMapLocations() != null) && !modifier.getAllowedMapLocations().isEmpty() &&
                !modifier.getAllowedMapLocations().contains(getTemplate().mapParameters.getMapLocation())) {
            return;
        }

        scenarioModifiers.add(modifier);

        for (String modifierKey : modifier.getLinkedModifiers().keySet()) {
            AtBScenarioModifier subMod = AtBScenarioModifier.getScenarioModifier(modifierKey);

            // if the modifier exists and has not already been added (to avoid infinite loops, as it's possible to define those in data)
            if ((subMod != null) && !alreadyHasModifier(subMod)) {
                // set the briefing text of the alternate modifier to the 'alternate' text supplied here
                subMod.setAdditionalBriefingText(modifier.getLinkedModifiers().get(modifierKey));
                addScenarioModifier(subMod);
            }
        }
    }

    /**
     * Check if the modifier list already has a modifier with the given modifier's name.
     */
    public boolean alreadyHasModifier(AtBScenarioModifier modifier) {
        for (AtBScenarioModifier existingModifier : scenarioModifiers) {
            if (existingModifier.getModifierName().equals(modifier.getModifierName())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int getScenarioType() {
        return DYNAMIC;
    }

    @Override
    public String getDesc() {
        return getScenarioTypeDescription();
    }

    @Override
    public String getScenarioTypeDescription() {
        return (getTemplate() != null) && (getTemplate().name != null) && !getTemplate().name.isBlank() ?
                getTemplate().name : "Dynamic Scenario";
    }

    @Override
    public String getResourceKey() {
        return null;
    }

    @Override
    protected void writeToXMLEnd(final PrintWriter pw, int indent) {
        // if we have a scenario template and haven't played the scenario out yet, serialize the template
        // in its current state
        if ((getTemplate() != null) && getStatus().isCurrent()) {
            getTemplate().Serialize(pw);

            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "effectivePlayerUnitCountMultiplier", getEffectivePlayerUnitCountMultiplier());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "effectivePlayerBVMultiplier", getEffectivePlayerBVMultiplier());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "friendlyReinforcementDelayReduction", getFriendlyReinforcementDelayReduction());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "friendlyDelayedReinforcements", getFriendlyDelayedReinforcements());
            MHQXMLUtility.writeSimpleXMLTag(pw,
                  indent,
                  "friendlyInstantReinforcements",
                  getFriendlyInstantReinforcements());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "hostileReinforcementDelayReduction", getHostileReinforcementDelayReduction());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "effectiveOpforSkill", getEffectiveOpforSkill().name());
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "effectiveOpforQuality", getEffectiveOpforQuality());

            if (!playerUnitSwaps.isEmpty()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, PLAYER_UNIT_SWAPS_ELEMENT);

                // note: if you update the order in which data is stored here or anything else about it
                // double check loadFieldsFromXmlNode
                for (UUID unitID : playerUnitSwaps.keySet()) {
                    MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, PLAYER_UNIT_SWAP_ELEMENT);
                    MHQXMLUtility.writeSimpleXMLTag(pw, indent, PLAYER_UNIT_SWAP_ID_ELEMENT, unitID);

                    BenchedEntityData benchedEntityData = playerUnitSwaps.get(unitID);
                    MHQXMLUtility.writeSimpleXMLTag(pw, indent, PLAYER_UNIT_SWAP_TEMPLATE_ELEMENT, benchedEntityData.templateName);
                    pw.println(MHQXMLUtility.writeEntityToXmlString(benchedEntityData.entity, indent, Collections.emptyList()));
                    MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, PLAYER_UNIT_SWAP_ELEMENT);
                }

                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, PLAYER_UNIT_SWAPS_ELEMENT);
            }
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "finalized", isFinalized());
        }

        super.writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(final Node wn, final Version version, final Campaign campaign)
            throws ParseException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            if (wn2.getNodeName().equalsIgnoreCase(ScenarioTemplate.ROOT_XML_ELEMENT_NAME)) {
                setTemplate(ScenarioTemplate.Deserialize(wn2));
            } else if (wn2.getNodeName().equalsIgnoreCase("effectivePlayerUnitCountMultiplier")) {
                setEffectivePlayerUnitCountMultiplier(Double.parseDouble(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("effectivePlayerBVMultiplier")) {
                setEffectivePlayerBVMultiplier(Double.parseDouble(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("friendlyReinforcementDelayReduction")) {
                setFriendlyReinforcementDelayReduction(Integer.parseInt(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("friendlyDelayedReinforcements")) {
                String[] values = wn2.getTextContent().split(",");
                for (String value : values) {
                    getFriendlyDelayedReinforcements().add(UUID.fromString(value));
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("Instant")) {
                String[] values = wn2.getTextContent().split(",");
                for (String value : values) {
                    getFriendlyInstantReinforcements().add(UUID.fromString(value));
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("hostileReinforcementDelayReduction")) {
                setHostileReinforcementDelayReduction(Integer.parseInt(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("effectiveOpforSkill")) {
                setEffectiveOpforSkill(SkillLevel.valueOf(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase("effectiveOpforQuality")) {
                setEffectiveOpforQuality(Integer.parseInt(wn2.getTextContent().trim()));
            } else if (wn2.getNodeName().equalsIgnoreCase(PLAYER_UNIT_SWAPS_ELEMENT)) {
                for (int snsIndex = 0; snsIndex < wn2.getChildNodes().getLength(); snsIndex++) {
                    Node swapNode = wn2.getChildNodes().item(snsIndex);

                    if (swapNode.getNodeName().equalsIgnoreCase(PLAYER_UNIT_SWAP_ELEMENT)) {
                        BenchedEntityData benchedEntityData = new BenchedEntityData();
                        UUID playerUnitID = null;

                        for (int swapIndex = 0; swapIndex < swapNode.getChildNodes().getLength(); swapIndex++) {
                            Node dataNode = swapNode.getChildNodes().item(swapIndex);

                            if (dataNode.getNodeName().equalsIgnoreCase(PLAYER_UNIT_SWAP_ID_ELEMENT)) {
                                playerUnitID = UUID.fromString(dataNode.getTextContent());
                            } else if (dataNode.getNodeName().equalsIgnoreCase(PLAYER_UNIT_SWAP_TEMPLATE_ELEMENT)) {
                                benchedEntityData.templateName = dataNode.getTextContent();
                            } else if (dataNode.getNodeName().equalsIgnoreCase(PLAYER_UNIT_SWAP_ENTITY_ELEMENT)) {
                                benchedEntityData.entity = MHQXMLUtility.parseSingleEntityMul((Element) dataNode, campaign);
                            }
                        }

                        playerUnitSwaps.put(playerUnitID, benchedEntityData);
                    }
                }
            } else if (wn2.getNodeName().equalsIgnoreCase("finalized")) {
                setFinalized(Boolean.parseBoolean(wn2.getTextContent().trim()));
            }
        }

        super.loadFieldsFromXmlNode(wn, version, campaign);
    }

    @Override
    public void refresh(Campaign campaign) {

    }

    @Override
    public void clearAllForcesAndPersonnel(Campaign campaign) {
        playerUnitTemplates.clear();
        playerForceTemplates.clear();
        super.clearAllForcesAndPersonnel(campaign);
    }

    @Override
    public String getBattlefieldControlDescription() {
        return "";
    }

    /**
     * Returns the total battle value (BV) either for allied forces or opposing forces in
     * a given contract campaign, as per the parameter {@code isAllied}.
     * <p>
     * If {@code isAllied} is {@code true}, the method calculates the total BV for the allied
     * forces inclusive of player forces. If {@code isAllied} is {@code false}, the total BV for
     * opposing forces is calculated.
     * <p>
     * The calculation is done based on Bot forces attributed to each side. In the case of
     * PlanetOwner, the alignment of the owner faction is considered to determine the ownership of
     * Bot forces.
     *
     * @param campaign  The campaign in which the forces are participating.
     * @param isAllied  A boolean value indicating whether to calculate the total BV for
     *                  allied forces (if true) or opposing forces (if false).
     * @return          The total battle value (BV) either for the allied forces or
     *                  opposing forces, as specified by the parameter isAllied.
     */
    public int getTeamTotalBattleValue(Campaign campaign, boolean isAllied) {
        AtBContract contract = getContract(campaign);
        int totalBattleValue = 0;

        for (BotForce botForce : getBotForces()) {
            int battleValue = botForce.getTotalBV(campaign);

            int team = botForce.getTeam();

            if (team == PlanetOwner.ordinal()) {
                String planetOwnerFaction = getPlanetOwnerFaction(contract, campaign.getLocalDate());
                ForceAlignment forceAlignment = getPlanetOwnerAlignment(contract, planetOwnerFaction, campaign.getLocalDate());
                team = forceAlignment.ordinal();
            }

            if (team <= Allied.ordinal()) {
                if (isAllied) {
                    totalBattleValue += battleValue;
                }
            } else if (!isAllied) {
                totalBattleValue += battleValue;
            }
        }

        if (isAllied) {
            Force playerForces = this.getForces(campaign);

            for (UUID unitID : playerForces.getAllUnits(false)) {
                try {
                    Unit unit = campaign.getUnit(unitID);
                    Entity entity = unit.getEntity();

                    totalBattleValue += entity.calculateBattleValue();
                } catch (Exception ex) {
                    logger.warn(ex.getMessage(), ex);
                }
            }
        }

        return totalBattleValue;
    }
}
