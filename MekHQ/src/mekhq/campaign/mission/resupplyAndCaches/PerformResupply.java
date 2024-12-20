/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.resupplyAndCaches;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.Loot;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.campaign.mission.resupplyAndCaches.Resupply.ResupplyType;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.campaign.stratcon.StratconScenario;
import mekhq.campaign.stratcon.StratconTrackState;

import java.util.*;
import java.util.Map.Entry;

import static java.lang.Math.floor;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.CRITICAL;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.DOMINATING;
import static mekhq.campaign.mission.enums.AtBMoraleLevel.STALEMATE;
import static mekhq.campaign.mission.resupplyAndCaches.GenerateResupplyContents.DropType.DROP_TYPE_AMMO;
import static mekhq.campaign.mission.resupplyAndCaches.GenerateResupplyContents.DropType.DROP_TYPE_ARMOR;
import static mekhq.campaign.mission.resupplyAndCaches.GenerateResupplyContents.DropType.DROP_TYPE_PARTS;
import static mekhq.campaign.mission.resupplyAndCaches.GenerateResupplyContents.getResupplyContents;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.ResupplyType.RESUPPLY_CONTRACT_END;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.ResupplyType.RESUPPLY_LOOT;
import static mekhq.campaign.stratcon.StratconRulesManager.generateExternalScenario;
import static mekhq.gui.dialog.resupplyAndCaches.DialogInterception.dialogInterception;
import static mekhq.gui.dialog.resupplyAndCaches.DialogItinerary.itineraryDialog;
import static mekhq.gui.dialog.resupplyAndCaches.DialogPlayerConvoyOption.createPlayerConvoyOptionalDialog;
import static mekhq.gui.dialog.resupplyAndCaches.DialogResupplyFocus.createResupplyFocusDialog;
import static mekhq.gui.dialog.resupplyAndCaches.DialogRoleplayEvent.dialogConvoyRoleplayEvent;
import static mekhq.gui.dialog.resupplyAndCaches.DialogSwindled.swindledDialog;
import static mekhq.utilities.EntityUtilities.getEntityFromUnitId;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

/**
 * The {@code PerformResupply} class handles the execution and management of resupply operations
 * within MekHQ campaigns. It covers various aspects of resupply, including generating convoy contents,
 * distributing supplies, resolving convoy interceptions, and facilitating player interaction through
 * dialogs tied to specific resupply scenarios.
 */
public class PerformResupply {
    private static final int NPC_CONVOY_MULTIPLIER = 10;
    private static final double INTERCEPTION_LOAD_INFLUENCE = 50;

    private static final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Resupply");

    private static final MMLogger logger = MMLogger.create(PerformResupply.class);

    /**
     * Initiates the resupply process for a specified campaign and active contract.
     *
     * <p>This method provides a simplified entry point to the resupply workflow, using a default value
     * of 1 for the supply drop count. It delegates to the overloaded method
     * {@link #performResupply(Resupply, AtBContract, int)} for the main execution of the resupply
     * process, encompassing supply generation, convoy interaction, and delivery confirmation.</p>
     *
     * <p>This entry point is typically used when the exact number of supply drops is not specified or
     * defaults to a single drop per invocation.</p>
     *
     * @param resupply the {@link Resupply} instance containing information about the resupply operation,
     *                 such as the supplies to be delivered, convoy setup, and context-specific rules.
     * @param contract the {@link AtBContract} representing the current contract, which provides the
     *                 operational context for the resupply, including permissions and restrictions.
     */
    public static void performResupply(Resupply resupply, AtBContract contract) {
        performResupply(resupply, contract, 1);
    }

    /**
     * Executes the resupply process for a specified campaign, contract, and supply drop count.
     * This method coordinates supply allocation, convoy interaction, potential for interception,
     * and player confirmation dialogs, ensuring the resupply process adheres to the campaign's
     * context and player decisions.
     *
     * <p>Functionality includes:</p>
     * <ul>
     *     <li>Early-exit handling for invalid cargo tonnage or drop count.</li>
     *     <li>Displaying dialogs to involve the player in choosing convoys or resupply focus.</li>
     *     <li>Randomized content generation for armor, ammo, and parts.</li>
     *     <li>Dialog-based confirmation for resupply delivery and associated costs.</li>
     * </ul>
     *
     * @param resupply  the {@link Resupply} instance that defines the campaign's resupply operation,
     *                  including cargo, player and NPC convoys, and mission-related data.
     * @param contract  the {@link AtBContract} representing the context of the current contract,
     *                  determining aspects such as independent resupply permissions and guerrilla warfare rules.
     * @param dropCount the number of supply drops planned for this resupply operation. If zero,
     *                  the method exits early.
     */
    public static void performResupply(Resupply resupply, AtBContract contract, int dropCount) {
        // These early exits should only occur if the player literally has no units.
        if (dropCount == 0) {
            logger.info("Resupply exited early, as DropCount is 0");
            return;
        }

        int targetCargoTonnage = resupply.getTargetCargoTonnage();
        if (targetCargoTonnage == 0) {
            logger.info("Resupply exited early, as targetCargoTonnage is 0");
            return;
        }

        final Campaign campaign = resupply.getCampaign();
        final boolean isIndependent = contract.getCommandRights().isIndependent();
        final boolean isGuerrilla = contract.getContractType().isGuerrillaWarfare();
        final ResupplyType resupplyType = resupply.getResupplyType();

        // If appropriate, prompt the player to use their own convoys
        if (!resupplyType.equals(RESUPPLY_LOOT) && !resupplyType.equals(RESUPPLY_CONTRACT_END)) {
            // If we're on a guerrilla contract, the player may be approached by smugglers, instead,
            // which won't use player convoys.
            if (!isGuerrilla) {
                createPlayerConvoyOptionalDialog(resupply, isIndependent);

                // If the player is on an Independent contract and refuses to use their own transports,
                // then no resupply occurs.
                if (isIndependent && !resupply.getUsePlayerConvoy()) {
                    return;
                }
            }

            // Then allow the player to pick a focus
            createResupplyFocusDialog(resupply);
        }

        // With the focus chosen, we determine the contents of the convoy
        boolean isUsePlayerConvoy = resupply.getUsePlayerConvoy();
        for (int i = 0; i < dropCount; i++) {
            getResupplyContents(resupply, DROP_TYPE_ARMOR, isUsePlayerConvoy);
            getResupplyContents(resupply, DROP_TYPE_AMMO, isUsePlayerConvoy);
            getResupplyContents(resupply, DROP_TYPE_PARTS, isUsePlayerConvoy);
        }

        resupply.setConvoyContents(resupply.getConvoyContents());

        double totalTonnage = 0;
        for (Part part : resupply.getConvoyContents()) {
            totalTonnage += part.getTonnage() * (part instanceof Armor || part instanceof AmmoBin ? 5 : 1);
        }

        logger.info("totalTonnage: " + totalTonnage);


        // This shouldn't occur, but we include it as insurance.
        if (resupply.getConvoyContents().isEmpty()) {
            campaign.addReport(String.format(resources.getString("convoyUnsuccessful.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                CLOSING_SPAN_TAG));
            return;
        }

        // Everything prepared, we present the player with a dialog allowing them to confirm
        // whether they are willing to pay for the delivery (if appropriate), or for them to
        // confirm delivery.
        itineraryDialog(resupply);
    }

    /**
     * Facilitates the delivery of resupply contents to the campaign's resources.
     *
     * <p>This method categorizes parts like ammunition, armor, and general equipment, placing them
     * in the warehouse.</p>
     *
     * @param resupply the {@link Resupply} instance defining the current campaign operation.
     * @param contents a list of {@link Part} objects representing the resupply contents to be delivered.
     *                 If {@code null}, fetches convoy contents from the {@link Resupply} instance.
     */
    public static void makeDelivery(Resupply resupply, @Nullable List<Part> contents) {
        final Campaign campaign = resupply.getCampaign();

        if (contents == null) {
            contents = resupply.getConvoyContents();
        }

        for (Part part : contents) {
            if (part instanceof AmmoBin) {
                campaign.getQuartermaster().addAmmo(((AmmoBin) part).getType(),
                    ((AmmoBin) part).getFullShots() * 5);
            } else if (part instanceof Armor) {
                int quantity = (int) Math.ceil(((Armor) part).getArmorPointsPerTon() * 5);
                ((Armor) part).setAmount(quantity);
                campaign.getWarehouse().addPart(part, true);
            } else {
                campaign.getWarehouse().addPart(part, true);
            }
        }
    }

    /**
     * Facilitates the delivery of supplies through smugglers, incorporating chances for smuggler
     * swindling.
     *
     * <p>Key behaviors:</p>
     * <ul>
     *     <li>Calculates the chance of being swindled based on the contract's morale level.</li>
     *     <li>If swindled, invokes a dialog to inform the player; otherwise, schedules delivery of supplies.</li>
     * </ul>
     *
     * @param resupply the {@link Resupply} instance defining the resupply context.
     */
    public static void makeSmugglerDelivery(Resupply resupply) {
        final AtBContract contract = resupply.getContract();
        int swindleChance = contract.getMoraleLevel().ordinal();

        if (Compute.randomInt(10) < swindleChance) {
            swindledDialog(resupply);
        } else {
            final Campaign campaign = resupply.getCampaign();

            campaign.addReport(String.format(resources.getString("convoySuccessfulSmuggler.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
                CLOSING_SPAN_TAG));
            makeDelivery(resupply, null);
        }
    }

    /**
     * Loads and organizes player convoys for a resupply operation. It calculates available
     * convoy capacities, sorts convoys and contents, and assigns parts to convoys based on capacity.
     *
     * @param resupply the {@link Resupply} instance containing convoy and mission-specific data.
     */
    public static void loadPlayerConvoys(Resupply resupply) {
        // Ammo and Armor are delivered in batches of 5, so we need to make sure to multiply their
        // weight by five when picking these items.
        final int WEIGHT_MULTIPLIER = 5;
        final Campaign campaign = resupply.getCampaign();
        final Map<Force, Double> playerConvoys = resupply.getPlayerConvoys();

        // Sort the player's available convoys according to cargo space, largest -> smallest
        List<Entry<Force, Double>> entryList = new ArrayList<>(playerConvoys.entrySet());
        entryList.sort((entry1, entry2) ->
            Double.compare(entry2.getValue(), entry1.getValue()));

        List<Force> sortedConvoys = new ArrayList<>();
        for (Entry<Force, Double> entry : entryList) {
            sortedConvoys.add(entry.getKey());
        }

        final List<Part> convoyContents = resupply.getConvoyContents();
        Collections.shuffle(convoyContents);

        // Distribute parts across the convoys
        for (Force convoy : sortedConvoys) {
            if (convoyContents.isEmpty()) {
                break;
            }

            Double cargoCapacity = playerConvoys.get(convoy);
            List<Part> convoyItems = new ArrayList<>();

            for (Part part : convoyContents) {
                double tonnage = part.getTonnage();

                if (part instanceof AmmoBin || part instanceof Armor) {
                    tonnage *= WEIGHT_MULTIPLIER;
                }

                if (cargoCapacity - tonnage >= 0) {
                    convoyItems.add(part);
                    cargoCapacity -= tonnage;
                }
            }

            convoyContents.removeAll(convoyItems);

            campaign.addReport(String.format(resources.getString("convoyDispatched.text"),
                convoy.getName()));
            processConvoy(resupply, convoyItems, convoy);
        }
    }

    /**
     * Processes convoy interactions, resolving outcomes based on player decisions, convoy details,
     * and interception chances. This includes factors such as convoy weight and morale influence.
     *
     * <p>Handles logic for:</p>
     * <ul>
     *     <li>Calculating interception chances based on convoy weight and mission context.</li>
     *     <li>Generating roleplay events for player convoys.</li>
     *     <li>Triggering convoys and interception scenarios.</li>
     * </ul>
     *
     * @param resupply        the {@link Resupply} instance defining the resupply operation.
     * @param convoyContents  a list of {@link Part} objects representing the contents of the convoy.
     * @param playerConvoy    the {@link Force} object representing the player's convoy.
     *                        If {@code null}, the convoy is an NPC-controlled unit.
     */
    public static void processConvoy(Resupply resupply, List<Part> convoyContents, @Nullable Force playerConvoy) {
        final Campaign campaign = resupply.getCampaign();
        final AtBContract contract = resupply.getContract();

        // First, we need to identify whether the convoy has been intercepted.
        AtBMoraleLevel morale = contract.getMoraleLevel();

        if (morale.isRouted()) {
            completeSuccessfulDelivery(resupply, convoyContents);
        }

        int interceptionChance = morale.ordinal();

        // This chance is modified by convoy weight, for player convoys this is easy - we just
        // calculate the weight of all units in the convoy. For NPC convoys, we need to get a bit
        // creative, as we have no way to determine their size prior to any interception scenario.
        // Instead, we base it on the amount of cargo they are carrying.
        double convoyWeight = -200; // Convoys have a weight allowance of 200t before suffering a detection malus

        if (playerConvoy == null) {
            // The multiplier we want to apply is identical to if the player was running the convoy.
            int npcConvoyWeight = resupply.getTargetCargoTonnage() * NPC_CONVOY_MULTIPLIER;
            convoyWeight += npcConvoyWeight;
        } else {
            for (UUID unitId : playerConvoy.getAllUnits(false)) {
                Entity entity = getEntityFromUnitId(campaign, unitId);

                if (entity == null) {
                    continue;
                }

                convoyWeight += entity.getWeight();
            }
        }

        interceptionChance += (int) Math.ceil(convoyWeight / INTERCEPTION_LOAD_INFLUENCE);
        // There is always a 1in10 chance of Interception, no matter how stealthy the convoy.
        interceptionChance = Math.max(0, interceptionChance);

        // With interception chance calculated, we check to see whether an interception or event has occurred.
        if (Compute.randomInt(10) < interceptionChance) {
            generateInterceptionOrConvoyEvent(resupply, playerConvoy, convoyContents, interceptionChance);
        } else {
            completeSuccessfulDelivery(resupply, convoyContents);
        }
    }

    /**
     * Handles convoy interceptions and their outcomes. The method determines whether the player receives
     * a scenario based on the convoy's state and selects the appropriate scenario template, generating an
     * encounter or completing the delivery as necessary.
     *
     * <p>Decision-making includes:</p>
     * <ul>
     *     <li>Determines scenario templates based on convoy types (e.g., VTOL, aerospace).</li>
     *     <li>Reports critical errors and gracefully completes deliveries if templates fail.</li>
     *     <li>Generates strategic map scenarios to handle interception events dynamically.</li>
     * </ul>
     *
     * @param resupply         the {@link Resupply} instance containing resupply details.
     * @param convoy           the {@link Force} representing the player's convoy. Can be {@code null} for NPC convoys.
     * @param convoyContents   a list of {@link Part} objects representing convoy cargo.
     * @param interceptionChance the calculated chance of interception for the convoy.
     */
    private static void generateInterceptionOrConvoyEvent(Resupply resupply, @Nullable Force convoy,
                                                             @Nullable List<Part> convoyContents,
                                                             int interceptionChance) {
        final Campaign campaign = resupply.getCampaign();
        final AtBContract contract = resupply.getContract();

        if (Compute.randomInt(10) < interceptionChance) {
            processConvoyInterception(resupply, convoy, convoyContents);
        } else {
            // If it is an NPC convoy, we skip roleplay events
            if (convoy == null) {
                completeSuccessfulDelivery(resupply, convoyContents);
                return;
            }

            // Non-ground convoys don't get roleplay events
            if (forceContainsOnlyVTOLForces(campaign, convoy) || forceContainsOnlyAerialForces(campaign, convoy)) {
                completeSuccessfulDelivery(resupply, convoyContents);
                return;
            }

            // Generate roleplay event
            final String STATUS_FORWARD = "statusUpdate";
            final String STATUS_AFTERWARD = ".text";

            AtBMoraleLevel morale = contract.getMoraleLevel();

            String eventText;
            if (Compute.d6() <= 2) {
                eventText = resources.getString(STATUS_FORWARD + Compute.randomInt(100)
                    + STATUS_AFTERWARD);
            } else {
                int roll = Compute.randomInt(2);

                if (morale.isAdvancing() || morale.isWeakened()) {
                    morale = roll == 0 ? (morale.isAdvancing() ? DOMINATING : CRITICAL) : STALEMATE;
                }

                eventText = resources.getString(STATUS_FORWARD + "Enemy" + morale
                    + Compute.randomInt(50) + STATUS_AFTERWARD);
            }

            dialogConvoyRoleplayEvent(campaign, convoy, eventText);
        }
    }

    /**
     * Completes the successful delivery of convoy supplies, adding them to campaign resources
     * and providing a positive campaign report.
     *
     * @param resupply       the {@link Resupply} instance describing the mission context.
     * @param convoyContents the list of convoy contents to be delivered.
     */
    private static void completeSuccessfulDelivery(Resupply resupply, List<Part> convoyContents) {
        final Campaign campaign = resupply.getCampaign();

        campaign.addReport(String.format(resources.getString("convoySuccessful.text"),
            spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorPositiveHexColor()),
            CLOSING_SPAN_TAG));

        makeDelivery(resupply, convoyContents);
    }

    /**
     * Determines if a convoy only contains VTOL or similar units.
     *
     * @param campaign the {@link Campaign} instance the convoy belongs to.
     * @param convoy   the {@link Force} representing the convoy to check.
     * @return {@code true} if the convoy only contains VTOL units, {@code false} otherwise.
     */
    private static boolean forceContainsOnlyVTOLForces(Campaign campaign, Force convoy) {
        for (UUID unitId : convoy.getAllUnits(false)) {
            Entity entity = getEntityFromUnitId(campaign, unitId);

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
     * Determines if a convoy contains a majority of VTOL (or similar) units.
     *
     * <p>This is calculated by checking if at least half of the units are VTOLs or similarly
     * airborne types.</p>
     *
     * @param campaign      the {@link Campaign} instance the convoy belongs to.
     * @param convoy  the {@link Force} representing the convoy being evaluated.
     * @return {@code true} if the VTOL units constitute at least half of the units, {@code false} otherwise.
     */
    private static boolean forceContainsMajorityVTOLForces(Campaign campaign, Force convoy) {
        Vector<UUID> allUnits = convoy.getAllUnits(false);
        int convoySize = allUnits.size();
        int vtolCount = 0;

        for (UUID unitId : convoy.getAllUnits(false)) {
            Entity entity = getEntityFromUnitId(campaign, unitId);

            if (entity == null) {
                continue;
            }

            if (!entity.isAirborneVTOLorWIGE()) {
                vtolCount++;
            }
        }

        return vtolCount >= floor((double) convoySize / 2);
    }


    /**
     * Determines if a convoy only contains aerial units, such as aerospace or conventional fighters.
     *
     * @param campaign the {@link Campaign} instance the convoy belongs to.
     * @param convoy   the {@link Force} representing the convoy to check.
     * @return {@code true} if the convoy only contains aerial units, {@code false} otherwise.
     */
    private static boolean forceContainsOnlyAerialForces(Campaign campaign, Force convoy) {
        for (UUID unitId : convoy.getAllUnits(false)) {
            Entity entity = getEntityFromUnitId(campaign, unitId);

            if (entity == null) {
                continue;
            }

            if (!entity.isAerospace() && !entity.isConventionalFighter()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Handles the interception of a convoy operation. Based on the convoy's state and type, it determines
     * the most appropriate scenario template and resolves the outcome of the interception.
     *
     * <p>Key behaviors:</p>
     * <ul>
     *     <li>Identifies an appropriate scenario template for the convoy (e.g., VTOL, player convoy).</li>
     *     <li>Randomly selects a strategic track to simulate scenario placement.</li>
     *     <li>Provides loot or completes the delivery if no valid interception scenario exists.</li>
     * </ul>
     *
     * @param resupply        the {@link Resupply} instance representing the resupply mission.
     * @param targetConvoy    the {@link Force} representing the player's convoy. Can be {@code null} for NPC convoys.
     * @param convoyContents  a list of {@link Part} objects representing the resupply cargo.
     */
    private static void processConvoyInterception(Resupply resupply, @Nullable Force targetConvoy,
                                                  @Nullable List<Part> convoyContents) {
        final String DIRECTORY = "data/scenariotemplates/";
        final String GENERIC = DIRECTORY + "Emergency Convoy Defense.xml";
        final String PLAYER_AEROSPACE_CONVOY = DIRECTORY + "Emergency Convoy Defense - Player - Low-Atmosphere.xml";
        final String PLAYER_VTOL_CONVOY = DIRECTORY + "Emergency Convoy Defense - Player - VTOL.xml";
        final String PLAYER_CONVOY = DIRECTORY + "Emergency Convoy Defense - Player.xml";

        final Campaign campaign = resupply.getCampaign();
        final AtBContract contract = resupply.getContract();

        // Trigger a dialog to inform the user an interception has taken place
        dialogInterception(resupply, targetConvoy);

        // Determine which scenario template to use based on convoy state
        String templateAddress = GENERIC;

        if (targetConvoy != null) {
            if (forceContainsOnlyAerialForces(campaign, targetConvoy)) {
                templateAddress = PLAYER_AEROSPACE_CONVOY;
            } else if (forceContainsMajorityVTOLForces(campaign, targetConvoy)) {
                templateAddress = PLAYER_VTOL_CONVOY;
            } else {
                templateAddress = PLAYER_CONVOY;
            }
        }
        ScenarioTemplate template = ScenarioTemplate.Deserialize(templateAddress);

        // If we're not using a player convoy, get all possible parts and put them in a pool
        if (targetConvoy == null) {
            convoyContents = resupply.getConvoyContents();
        }

        // If we've failed to deserialize the requested template, report the error and make the delivery.
        // We report the error in this fashion, instead of hiding it in the log, as we want to
        // increase the likelihood the player is aware an error has occurred.
        if (template == null) {
            campaign.addReport(String.format(resources.getString("convoyErrorTemplate.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                templateAddress, CLOSING_SPAN_TAG));

            makeDelivery(resupply, convoyContents);
            return;
        }

        // Pick a random track where the interception will take place. If we fail to get a track,
        // we log an error and make the delivery, in the same manner as above.
        StratconTrackState track;
        try {
            final StratconCampaignState campaignState = contract.getStratconCampaignState();
            List<StratconTrackState> tracks = campaignState.getTracks();
            track = ObjectUtility.getRandomItem(tracks);
        } catch (NullPointerException e) {
            campaign.addReport(String.format(resources.getString("convoyErrorTracks.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                templateAddress, CLOSING_SPAN_TAG));

            makeDelivery(resupply, convoyContents);
            return;
        }

        // Generate the scenario, placing it in a random hex that does not currently contain a
        // scenario, or a facility. If the player is really lucky, the scenario will spawn on top
        // of a force already deployed to the Strategic Map.
        StratconScenario scenario = generateExternalScenario(campaign, contract, track,
            null, template, false);

        // If we successfully generated a scenario, we need to make a couple of final
        // adjustments, including assigning the Resupply contents as loot and
        // assigning a player convoy (if appropriate)
        if (scenario != null) {
            AtBDynamicScenario backingScenario = scenario.getBackingScenario();
            backingScenario.setDate(campaign.getLocalDate());

            if (targetConvoy != null) {
                backingScenario.addForce(targetConvoy.getId(), "Player");
                targetConvoy.setScenarioId(backingScenario.getId(), campaign);
                scenario.commitPrimaryForces();
            }

            Loot loot = new Loot();

            if (convoyContents != null) {
                for (Part part : convoyContents) {
                    loot.addPart(part);
                }
            }

            backingScenario.addLoot(loot);

            // Announce the situation to the player
            campaign.addReport(String.format(resources.getString("convoyInterceptedStratCon.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                CLOSING_SPAN_TAG));
        } else {
            // If we failed to generate a scenario, for whatever reason, we don't
            // want the player confused why there isn't a scenario, so we offer
            // this fluffy response.
            campaign.addReport(String.format(resources.getString("convoyEscaped.text"),
                spanOpeningWithCustomColor(MekHQ.getMHQOptions().getFontColorNegativeHexColor()),
                CLOSING_SPAN_TAG));

            makeDelivery(resupply, convoyContents);
        }
    }
}
