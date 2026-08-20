/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.contract.io;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
import megamek.Version;
import megamek.client.ui.util.PlayerColour;
import megamek.codeUtilities.MathUtility;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ChaosActOfPiracy;
import mekhq.campaign.mission.contract.ChaosContract;
import mekhq.campaign.mission.contract.ChaosGameWorlds;
import mekhq.campaign.mission.contract.contractData.*;
import mekhq.campaign.mission.contract.contractGeneration.ChaosEmployerType;
import mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs.TermFunding;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * XML serialization and deserialization for {@link AbstractContract} and its data records.
 *
 * <p>{@link AbstractContract} is intentionally a pure data class with no logic, so the (de)serialization lives here
 * rather than on the type itself. Writing uses MekHQ's standard {@link PrintWriter} / {@link MHQXMLUtility} tag
 * helpers. Reading uses a <em>map-lookup</em>: each element under a {@code <contract>} node is dispatched by its tag
 * name to a handler in {@link #READERS}, mirroring the pattern used by {@code CampaignOptionsUnmarshaller} /
 * {@code CampaignOptionCodecs}.</p>
 *
 * <p>The concrete subclass ({@link ChaosContract}, {@link ChaosActOfPiracy}, {@link ChaosGameWorlds}) is recorded as
 * the {@code type} attribute of the {@code <contract>} element and reinstated by reflection on load, exactly as
 * {@link Scenario} and {@link Person} do. Transient fields (the cached jump path and cached difficulty) are not
 * persisted.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ContractXmlCodec {
    private static final MMLogger LOGGER = MMLogger.create(ContractXmlCodec.class);

    public static final String CONTRACT_TAG = "contract";
    private static final String TYPE_ATTRIBUTE = "type";

    private ContractXmlCodec() {}

    // region Write

    /**
     * Writes a single contract as a {@code <contract>} element.
     *
     * @param printWriter the writer to emit to
     * @param indent      the indentation level of the {@code <contract>} element
     * @param contract    the contract to serialize
     * @param campaign    the owning campaign (required to serialize nested {@link Person} negotiators)
     */
    public static void writeContract(final PrintWriter printWriter, int indent, final AbstractContract contract,
          final Campaign campaign) {
        MHQXMLUtility.writeSimpleXMLOpenTag(printWriter, indent++, CONTRACT_TAG, TYPE_ATTRIBUTE,
              contract.getClass().getName());

        if (contract.getId() != null) {
            MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "contractId", contract.getId());
        }
        writeStringIfPresent(printWriter, indent, "contractName", contract.getName());
        writeStringIfPresent(printWriter, indent, "description", contract.getDescription());
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "scale", contract.getScale());
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "trackCount", contract.getTrackCount());
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "provingGround", contract.isProvingGround());
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "sharesPercent", contract.getSharesPercent());
        if (!contract.getObfuscatedIntel().isEmpty()) {
            MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "obfuscatedIntel",
                  contract.getObfuscatedIntel().stream().map(Enum::name).collect(Collectors.joining(",")));
        }
        if (contract.getStatus() != null) {
            MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "missionStatus", contract.getStatus().name());
        }
        // Running salvage totals, accumulated across the contract's scenarios.
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "salvagedByUnitValue", contract.getSalvagedByUnitValue());
        MHQXMLUtility.writeSimpleXMLTag(printWriter,
              indent,
              "salvagedByEmployerValue",
              contract.getSalvagedByEmployerValue());
        // The player's chosen negotiator is a roster member, so persist only their id and re-resolve on load.
        if (contract.getPlayerNegotiator() != null) {
            MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "playerNegotiatorId",
                  contract.getPlayerNegotiator().getId());
        }

        writeEmployerData(printWriter, indent, contract.getEmployerData(), campaign);
        writeEnemyData(printWriter, indent, contract.getEnemyData(), campaign);
        writeContractTerms(printWriter, indent, contract.getContractTerms());
        writeObjectiveData(printWriter, indent, contract.getObjectiveData());
        writeFinanceData(printWriter, indent, contract.getContractFinanceData());
        writeScheduleData(printWriter, indent, contract.getScheduleData());
        writeSystemsTargetData(printWriter, indent, contract.getSystemsTargetData());
        writeRentedFacilitiesData(printWriter, indent, contract.getRentedFacilitiesData());
        writeMoraleData(printWriter, indent, contract.getMoraleData());
        writeNegotiationData(printWriter, indent, contract.getNegotiationData());

        // StratConCampaignState marshals itself under its own <StratConCampaignState> root element.
        if (contract.getStratConCampaignState() != null) {
            contract.getStratConCampaignState().Serialize(printWriter);
        }

        writeScenarios(printWriter, indent, contract.getScenarios());

        MHQXMLUtility.writeSimpleXMLCloseTag(printWriter, --indent, CONTRACT_TAG);
    }

    private static void writeEmployerData(final PrintWriter pw, int indent, final @Nullable EmployerData data,
          final Campaign campaign) {
        if (data == null) {
            return;
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "employerData");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "type", data.type().name());
        writeStringIfPresent(pw, indent, "factionCode", data.factionCode());
        writeStringIfPresent(pw, indent, "anchorFactionCode", data.anchorFactionCode());
        writeStringIfPresent(pw, indent, "sponsorFactionCode", data.sponsorFactionCode());
        writeStringIfPresent(pw, indent, "displayName", data.displayName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "forceSkill", data.forceSkill().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentRating", data.equipmentRating());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "color", data.color().name());
        if (data.camouflage() != null) {
            data.camouflage().writeToXML(pw, indent);
        }
        writeWrappedPerson(pw, indent, "negotiator", data.negotiator(), campaign);
        writeWrappedPerson(pw, indent, "liaison", data.liaison(), campaign);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "employerData");
    }

    private static void writeEnemyData(final PrintWriter pw, int indent, final @Nullable EnemyData data,
          final Campaign campaign) {
        if (data == null) {
            return;
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "enemyData");
        writeStringIfPresent(pw, indent, "factionCode", data.factionCode());
        writeStringIfPresent(pw, indent, "sponsorFactionCode", data.sponsorFactionCode());
        writeStringIfPresent(pw, indent, "displayName", data.displayName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "forceSkill", data.forceSkill().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentRating", data.equipmentRating());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "color", data.color().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "batchallAccepted", data.batchallAccepted());
        if (data.camouflage() != null) {
            data.camouflage().writeToXML(pw, indent);
        }
        writeWrappedPerson(pw, indent, "opposingCommander", data.opposingCommander(), campaign);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "enemyData");
    }

    private static void writeContractTerms(final PrintWriter pw, int indent, final @Nullable ContractTermsData data) {
        if (data == null) {
            return;
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "contractTerms");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "payRate", data.payRate().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "support", data.support().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "transport", data.transport().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "salvageRights", data.salvageRights().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "commandRights", data.commandRights().name());
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "contractTerms");
    }

    private static void writeObjectiveData(final PrintWriter pw, int indent,
          final @Nullable ContractObjectiveData data) {
        if (data == null) {
            return;
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "objectiveData");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "playerObjectiveType", data.playerObjectiveType().name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "opposingObjectiveType", data.opposingObjectiveType().name());
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "objectiveData");
    }

    private static void writeFinanceData(final PrintWriter pw, int indent, final @Nullable ContractFinanceData data) {
        if (data == null) {
            return;
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "contractFinanceData");
        if (data.transport() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "transport", data.transport());
        }
        if (data.monthlyPay() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "monthlyPay", data.monthlyPay());
        }
        if (data.combatPay() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "combatPay", data.combatPay());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "contractFinanceData");
    }

    private static void writeScheduleData(final PrintWriter pw, int indent, final @Nullable ContractScheduleData data) {
        if (data == null) {
            return;
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "scheduleData");
        if (data.startDate() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "startDate", data.startDate());
        }
        if (data.endDate() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "endDate", data.endDate());
        }
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "lengthInMonths", data.lengthInMonths());
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "scheduleData");
    }

    private static void writeSystemsTargetData(final PrintWriter pw, int indent,
          final @Nullable SystemsTargetData data) {
        if (data == null) {
            return;
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "systemsTargetData");
        writeStringIfPresent(pw, indent, "systemId", data.systemId());
        writeStringIfPresent(pw, indent, "planetId", data.planetId());
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "systemsTargetData");
    }

    private static void writeRentedFacilitiesData(final PrintWriter pw, int indent,
          final @Nullable RentedFacilitiesData data) {
        if (data == null) {
            return;
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "rentedFacilitiesData");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "hospitalBeds", data.hospitalBeds());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "kitchens", data.kitchens());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "holdingCells", data.holdingCells());
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "rentedFacilitiesData");
    }

    private static void writeMoraleData(final PrintWriter pw, int indent, final @Nullable MoraleData data) {
        if (data == null) {
            return;
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "moraleData");
        if (data.moraleLevel() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "moraleLevel", data.moraleLevel().name());
        }
        if (data.routEndDate() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "routEndDate", data.routEndDate());
        }
        if (data.routedPayout() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "routedPayout", data.routedPayout());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "moraleData");
    }

    private static void writeNegotiationData(final PrintWriter pw, int indent, final @Nullable NegotiationData data) {
        if (data == null) {
            return;
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "negotiationData");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "originalPayStep", data.originalPayStep());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "originalSupportStep", data.originalSupportStep());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "originalTransportStep", data.originalTransportStep());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "originalSalvageStep", data.originalSalvageStep());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "originalCommandStep", data.originalCommandStep());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "reputationUsed", data.reputationUsed());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "swapsUsed", data.swapsUsed());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "sacrificeBank", data.sacrificeBank());
        if (data.funding() != null) {
            // One <clause> per clause list, in canonical order; emitted even when empty to preserve positions.
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "funding");
            for (final List<TermFunding> clause : data.funding()) {
                final String joined = clause.stream().map(Enum::name).collect(Collectors.joining(","));
                pw.println(MHQXMLUtility.indentStr(indent) + "<clause>" + joined + "</clause>");
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "funding");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "negotiationData");
    }

    private static void writeScenarios(final PrintWriter pw, int indent, final List<Scenario> scenarios) {
        if (scenarios.isEmpty()) {
            return;
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "scenarios");
        for (final Scenario scenario : scenarios) {
            scenario.writeToXML(pw, indent);
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "scenarios");
    }

    private static void writeWrappedPerson(final PrintWriter pw, int indent, final String wrapperTag,
          final @Nullable Person person, final Campaign campaign) {
        if (person == null) {
            return;
        }
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, wrapperTag);
        person.writeToXML(pw, indent, campaign);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, wrapperTag);
    }

    private static void writeStringIfPresent(final PrintWriter pw, final int indent, final String tag,
          final @Nullable String value) {
        if (value != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, tag, value);
        }
    }
    // endregion Write

    // region Read

    /**
     * A handler that applies one child element of a {@code <contract>} node to the contract being built.
     */
    @FunctionalInterface
    private interface ContractElementReader {
        void read(AbstractContract contract, Node node, Campaign campaign, Version version);
    }

    /**
     * Map-lookup dispatch table keyed by child element tag name, in the style of {@code CampaignOptionCodecs}. A tag
     * not present here is logged and skipped, so unknown/obsolete tags never abort a load.
     */
    private static final Map<String, ContractElementReader> READERS = createReaderMap();

    private static Map<String, ContractElementReader> createReaderMap() {
        final Map<String, ContractElementReader> readers = new HashMap<>();

        readers.put("contractId",
              (contract, node, campaign, version) -> contract.setContractId(UUID.fromString(text(node))));
        readers.put("contractName", (contract, node, campaign, version) -> contract.setContractName(text(node)));
        readers.put("description", (contract, node, campaign, version) -> contract.setDescription(text(node)));
        readers.put("scale", (contract, node, campaign, version) -> contract.setScale(parseInt(node)));
        readers.put("trackCount", (contract, node, campaign, version) -> contract.setTrackCount(parseInt(node)));
        readers.put("provingGround",
              (contract, node, campaign, version) -> contract.setProvingGround(Boolean.parseBoolean(text(node))));
        readers.put("sharesPercent",
              (contract, node, campaign, version) -> contract.setSharesPercent(parseInt(node)));
        readers.put("missionStatus",
              (contract, node, campaign, version) -> contract.setStatus(MissionStatus.parseFromString(text(node))));
        readers.put("salvagedByUnitValue",
              (contract, node, campaign, version) -> contract.setSalvagedByUnitValue(Money.fromXmlString(text(node))));
        readers.put("salvagedByEmployerValue",
              (contract, node, campaign, version) -> contract.setSalvagedByEmployerValue(Money.fromXmlString(text(
                    node))));
        // The roster is not loaded yet when contracts are read, so stash the id; the loader resolves it post-load.
        readers.put("playerNegotiatorId",
              (contract, node, campaign, version) -> contract.setPendingPlayerNegotiatorId(UUID.fromString(text(
                    node))));

        readers.put("employerData",
              (contract, node, campaign, version) -> contract.setEmployerData(parseEmployerData(node,
                    campaign,
                    version)));
        readers.put("enemyData",
              (contract, node, campaign, version) -> contract.setEnemyData(parseEnemyData(node, campaign, version)));
        readers.put("contractTerms",
              (contract, node, campaign, version) -> contract.setContractTerms(parseContractTerms(node)));
        readers.put("objectiveData",
              (contract, node, campaign, version) -> contract.setObjectiveData(parseObjectiveData(node)));
        readers.put("contractFinanceData",
              (contract, node, campaign, version) -> contract.setContractFinanceData(parseFinanceData(node)));
        readers.put("scheduleData",
              (contract, node, campaign, version) -> contract.setScheduleData(parseScheduleData(node)));
        readers.put("obfuscatedIntel",
              (contract, node, campaign, version) -> contract.setObfuscatedIntel(parseObfuscatedIntel(node)));
        readers.put("systemsTargetData",
              (contract, node, campaign, version) -> contract.setSystemsTargetData(parseSystemsTargetData(node)));
        readers.put("rentedFacilitiesData",
              (contract, node, campaign, version) -> contract.setRentedFacilitiesData(parseRentedFacilitiesData(node)));
        readers.put("moraleData", (contract, node, campaign, version) -> contract.setMoraleData(parseMoraleData(node)));
        readers.put("negotiationData",
              (contract, node, campaign, version) -> contract.setNegotiationData(parseNegotiationData(node)));
        readers.put(StratConCampaignState.ROOT_XML_ELEMENT_NAME,
              (contract, node, campaign, version) -> contract.setStratConCampaignState(StratConCampaignState.Deserialize(
                    node)));
        readers.put("scenarios", ContractXmlCodec::parseScenarios);

        return readers;
    }

    /**
     * Reconstructs a single contract from a {@code <contract>} element.
     *
     * @param node     the {@code <contract>} element
     * @param campaign the owning campaign
     * @param version  the save file's version
     *
     * @return the reconstructed contract, or {@code null} if its concrete type could not be instantiated
     */
    public static @Nullable AbstractContract readContract(final Node node, final Campaign campaign,
          final Version version) {
        final AbstractContract contract = instantiate(node);
        if (contract == null) {
            return null;
        }
        // Inject the options so the term getters apply the configured per-term multipliers for every deserialized
        // contract, including market offers that are never registered as campaign missions.
        contract.setCampaignOptions(campaign.getCampaignOptions());

        final NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            final ContractElementReader reader = READERS.get(child.getNodeName());
            if (reader == null) {
                LOGGER.warn("Unexpected contract element ignored: {}", child.getNodeName());
                continue;
            }

            try {
                reader.read(contract, child, campaign, version);
            } catch (Exception ex) {
                LOGGER.error(ex, "Error parsing contract element: {}", child.getNodeName());
            }
        }

        return contract;
    }

    private static @Nullable AbstractContract instantiate(final Node node) {
        final NamedNodeMap attributes = node.getAttributes();
        final Node typeAttribute = (attributes == null) ? null : attributes.getNamedItem(TYPE_ATTRIBUTE);
        if (typeAttribute == null) {
            LOGGER.error("Contract element is missing its '{}' attribute; cannot instantiate", TYPE_ATTRIBUTE);
            return null;
        }

        final String className = typeAttribute.getTextContent();
        try {
            return (AbstractContract) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (Exception ex) {
            LOGGER.error(ex, "Unable to instantiate contract class '{}'", className);
            return null;
        }
    }

    /**
     * Binds one child element of a data record's node onto a mutable builder. Every binder shares this signature so the
     * few that need the campaign/version (nested personnel) sit in the same map-lookup table as the pure-value ones.
     */
    @FunctionalInterface
    private interface FieldBinder<B> {
        void bind(B builder, Node node, Campaign campaign, Version version);
    }

    /**
     * Map-lookup dispatch shared by every data-record parser: each element child of {@code parent} is routed to its
     * handler in {@code binders} by tag name and applied to {@code builder} - the same algorithm the top-level
     * {@link #READERS} table uses. Unknown tags and handlers that throw are logged and skipped so one bad field never
     * aborts the record.
     */
    private static <B> B readFields(final Node parent, final B builder, final Map<String, FieldBinder<B>> binders,
          final Campaign campaign, final Version version, final String context) {
        final NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            final FieldBinder<B> binder = binders.get(child.getNodeName());
            if (binder == null) {
                LOGGER.warn("Unexpected {} element ignored: {}", context, child.getNodeName());
                continue;
            }
            try {
                binder.bind(builder, child, campaign, version);
            } catch (Exception ex) {
                LOGGER.error(ex, "Error parsing {} element: {}", context, child.getNodeName());
            }
        }
        return builder;
    }

    private static final class EmployerDataBuilder {
        ChaosEmployerType type;
        String factionCode;
        String anchorFactionCode;
        String sponsorFactionCode;
        String displayName;
        SkillLevel forceSkill = SkillLevel.REGULAR;
        int equipmentRating;
        Camouflage camouflage = new Camouflage();
        PlayerColour color = PlayerColour.BLUE;
        Person negotiator;
        Person liaison;
    }

    private static final Map<String, FieldBinder<EmployerDataBuilder>> EMPLOYER_BINDERS = createEmployerBinders();

    private static Map<String, FieldBinder<EmployerDataBuilder>> createEmployerBinders() {
        final Map<String, FieldBinder<EmployerDataBuilder>> binders = new HashMap<>();
        binders.put("type", (builder, node, campaign, version) -> builder.type = ChaosEmployerType.valueOf(text(node)));
        binders.put("factionCode", (builder, node, campaign, version) -> builder.factionCode = text(node));
        binders.put("anchorFactionCode", (builder, node, campaign, version) -> builder.anchorFactionCode = text(node));
        binders.put("sponsorFactionCode",
              (builder, node, campaign, version) -> builder.sponsorFactionCode = text(node));
        binders.put("displayName", (builder, node, campaign, version) -> builder.displayName = text(node));
        binders.put("forceSkill",
              (builder, node, campaign, version) -> builder.forceSkill = SkillLevel.valueOf(text(node)));
        binders.put("equipmentRating", (builder, node, campaign, version) -> builder.equipmentRating = parseInt(node));
        binders.put("color", (builder, node, campaign, version) -> builder.color = PlayerColour.valueOf(text(node)));
        binders.put(Camouflage.XML_TAG,
              (builder, node, campaign, version) -> builder.camouflage = Camouflage.parseFromXML(node));
        binders.put("negotiator",
              (builder, node, campaign, version) -> builder.negotiator = parseWrappedPerson(node, campaign, version));
        binders.put("liaison",
              (builder, node, campaign, version) -> builder.liaison = parseWrappedPerson(node, campaign, version));
        return binders;
    }

    private static EmployerData parseEmployerData(final Node wn, final Campaign campaign, final Version version) {
        final EmployerDataBuilder builder = readFields(wn, new EmployerDataBuilder(), EMPLOYER_BINDERS, campaign,
              version, "employerData");
        return new EmployerData(builder.type, builder.factionCode, builder.anchorFactionCode,
              builder.sponsorFactionCode, builder.displayName, builder.negotiator, builder.liaison, builder.forceSkill,
              builder.equipmentRating, builder.camouflage, builder.color);
    }

    private static final class EnemyDataBuilder {
        String factionCode;
        String sponsorFactionCode;
        String displayName;
        SkillLevel forceSkill = SkillLevel.REGULAR;
        int equipmentRating;
        Person opposingCommander;
        Camouflage camouflage = new Camouflage();
        PlayerColour color = PlayerColour.RED;
        boolean batchallAccepted = true;
    }

    private static final Map<String, FieldBinder<EnemyDataBuilder>> ENEMY_BINDERS = createEnemyBinders();

    private static Map<String, FieldBinder<EnemyDataBuilder>> createEnemyBinders() {
        final Map<String, FieldBinder<EnemyDataBuilder>> binders = new HashMap<>();
        binders.put("factionCode", (builder, node, campaign, version) -> builder.factionCode = text(node));
        binders.put("sponsorFactionCode",
              (builder, node, campaign, version) -> builder.sponsorFactionCode = text(node));
        binders.put("displayName", (builder, node, campaign, version) -> builder.displayName = text(node));
        binders.put("forceSkill",
              (builder, node, campaign, version) -> builder.forceSkill = SkillLevel.valueOf(text(node)));
        binders.put("equipmentRating", (builder, node, campaign, version) -> builder.equipmentRating = parseInt(node));
        binders.put("color", (builder, node, campaign, version) -> builder.color = PlayerColour.valueOf(text(node)));
        binders.put("batchallAccepted",
              (builder, node, campaign, version) -> builder.batchallAccepted = Boolean.parseBoolean(text(node)));
        binders.put(Camouflage.XML_TAG,
              (builder, node, campaign, version) -> builder.camouflage = Camouflage.parseFromXML(node));
        binders.put("opposingCommander",
              (builder, node, campaign, version) -> builder.opposingCommander = parseWrappedPerson(node,
                    campaign,
                    version));
        return binders;
    }

    private static EnemyData parseEnemyData(final Node wn, final Campaign campaign, final Version version) {
        final EnemyDataBuilder builder = readFields(wn, new EnemyDataBuilder(), ENEMY_BINDERS, campaign, version,
              "enemyData");
        return new EnemyData(builder.factionCode,
              builder.sponsorFactionCode,
              builder.displayName,
              builder.forceSkill,
              builder.equipmentRating,
              builder.opposingCommander,
              builder.camouflage,
              builder.color,
              builder.batchallAccepted);
    }

    private static final class ContractTermsBuilder {
        ChaosContractStepsTable payRate;
        ChaosContractStepsTable support;
        ChaosContractStepsTable transport;
        ChaosContractStepsTable salvageRights;
        ChaosContractStepsTable commandRights;
    }

    private static final Map<String, FieldBinder<ContractTermsBuilder>> CONTRACT_TERMS_BINDERS =
          createContractTermsBinders();

    private static Map<String, FieldBinder<ContractTermsBuilder>> createContractTermsBinders() {
        final Map<String, FieldBinder<ContractTermsBuilder>> binders = new HashMap<>();
        binders.put("payRate",
              (builder, node, campaign, version) -> builder.payRate = ChaosContractStepsTable.valueOf(text(node)));
        binders.put("support",
              (builder, node, campaign, version) -> builder.support = ChaosContractStepsTable.valueOf(text(node)));
        binders.put("transport",
              (builder, node, campaign, version) -> builder.transport = ChaosContractStepsTable.valueOf(text(node)));
        binders.put("salvageRights",
              (builder, node, campaign, version) -> builder.salvageRights = ChaosContractStepsTable.valueOf(text(node)));
        binders.put("commandRights",
              (builder, node, campaign, version) -> builder.commandRights = ChaosContractStepsTable.valueOf(text(node)));
        return binders;
    }

    private static ContractTermsData parseContractTerms(final Node wn) {
        final ContractTermsBuilder builder = readFields(wn, new ContractTermsBuilder(), CONTRACT_TERMS_BINDERS, null,
              null, "contractTerms");
        return new ContractTermsData(builder.payRate, builder.support, builder.transport, builder.salvageRights,
              builder.commandRights);
    }

    private static final class ObjectiveDataBuilder {
        ContractObjectiveType playerObjectiveType;
        ContractObjectiveType opposingObjectiveType;
    }

    private static final Map<String, FieldBinder<ObjectiveDataBuilder>> OBJECTIVE_BINDERS = createObjectiveBinders();

    private static Map<String, FieldBinder<ObjectiveDataBuilder>> createObjectiveBinders() {
        final Map<String, FieldBinder<ObjectiveDataBuilder>> binders = new HashMap<>();
        binders.put("playerObjectiveType",
              (builder, node, campaign, version) -> builder.playerObjectiveType = ContractObjectiveType.valueOf(text(
                    node)));
        binders.put("opposingObjectiveType",
              (builder, node, campaign, version) -> builder.opposingObjectiveType = ContractObjectiveType.valueOf(text(
                    node)));
        return binders;
    }

    private static ContractObjectiveData parseObjectiveData(final Node wn) {
        final ObjectiveDataBuilder builder = readFields(wn, new ObjectiveDataBuilder(), OBJECTIVE_BINDERS, null, null,
              "objectiveData");
        return new ContractObjectiveData(builder.playerObjectiveType, builder.opposingObjectiveType);
    }

    private static final class FinanceDataBuilder {
        Money transport = Money.zero();
        Money monthlyPay = Money.zero();
        Money combatPay = Money.zero();
    }

    private static final Map<String, FieldBinder<FinanceDataBuilder>> FINANCE_BINDERS = createFinanceBinders();

    private static Map<String, FieldBinder<FinanceDataBuilder>> createFinanceBinders() {
        final Map<String, FieldBinder<FinanceDataBuilder>> binders = new HashMap<>();
        binders.put("transport",
              (builder, node, campaign, version) -> builder.transport = Money.fromXmlString(text(node)));
        binders.put("monthlyPay",
              (builder, node, campaign, version) -> builder.monthlyPay = Money.fromXmlString(text(node)));
        binders.put("combatPay",
              (builder, node, campaign, version) -> builder.combatPay = Money.fromXmlString(text(node)));
        return binders;
    }

    private static ContractFinanceData parseFinanceData(final Node wn) {
        final FinanceDataBuilder builder = readFields(wn, new FinanceDataBuilder(), FINANCE_BINDERS, null, null,
              "contractFinanceData");
        return new ContractFinanceData(builder.transport, builder.monthlyPay, builder.combatPay);
    }

    private static final class ScheduleDataBuilder {
        LocalDate startDate;
        LocalDate endDate;
        int lengthInMonths;
    }

    private static final Map<String, FieldBinder<ScheduleDataBuilder>> SCHEDULE_BINDERS = createScheduleBinders();

    private static Map<String, FieldBinder<ScheduleDataBuilder>> createScheduleBinders() {
        final Map<String, FieldBinder<ScheduleDataBuilder>> binders = new HashMap<>();
        binders.put("startDate",
              (builder, node, campaign, version) -> builder.startDate = MHQXMLUtility.parseDate(text(node)));
        binders.put("endDate",
              (builder, node, campaign, version) -> builder.endDate = MHQXMLUtility.parseDate(text(node)));
        binders.put("lengthInMonths", (builder, node, campaign, version) -> builder.lengthInMonths = parseInt(node));
        return binders;
    }

    private static ContractScheduleData parseScheduleData(final Node wn) {
        final ScheduleDataBuilder builder = readFields(wn, new ScheduleDataBuilder(), SCHEDULE_BINDERS, null, null,
              "scheduleData");
        return new ContractScheduleData(builder.startDate, builder.endDate, builder.lengthInMonths);
    }

    /**
     * Parses the comma-separated list of hidden intel fields, skipping any name a newer save carried that this build no
     * longer recognizes.
     */
    private static java.util.Set<ObfuscatableIntel> parseObfuscatedIntel(final Node wn) {
        final java.util.Set<ObfuscatableIntel> fields = java.util.EnumSet.noneOf(ObfuscatableIntel.class);
        for (final String token : text(wn).split(",")) {
            final String name = token.trim();
            if (name.isEmpty()) {
                continue;
            }
            try {
                fields.add(ObfuscatableIntel.valueOf(name));
            } catch (IllegalArgumentException ex) {
                LOGGER.warn("Unknown obfuscated-intel field '{}' ignored.", name);
            }
        }
        return fields;
    }

    private static final class SystemsTargetBuilder {
        String systemId;
        String planetId;
    }

    private static final Map<String, FieldBinder<SystemsTargetBuilder>> SYSTEMS_TARGET_BINDERS =
          createSystemsTargetBinders();

    private static Map<String, FieldBinder<SystemsTargetBuilder>> createSystemsTargetBinders() {
        final Map<String, FieldBinder<SystemsTargetBuilder>> binders = new HashMap<>();
        binders.put("systemId", (builder, node, campaign, version) -> builder.systemId = text(node));
        binders.put("planetId", (builder, node, campaign, version) -> builder.planetId = text(node));
        return binders;
    }

    private static SystemsTargetData parseSystemsTargetData(final Node wn) {
        final SystemsTargetBuilder builder = readFields(wn, new SystemsTargetBuilder(), SYSTEMS_TARGET_BINDERS, null,
              null, "systemsTargetData");
        return new SystemsTargetData(builder.systemId, builder.planetId);
    }

    private static final class RentedFacilitiesBuilder {
        int hospitalBeds;
        int kitchens;
        int holdingCells;
    }

    private static final Map<String, FieldBinder<RentedFacilitiesBuilder>> RENTED_FACILITIES_BINDERS =
          createRentedFacilitiesBinders();

    private static Map<String, FieldBinder<RentedFacilitiesBuilder>> createRentedFacilitiesBinders() {
        final Map<String, FieldBinder<RentedFacilitiesBuilder>> binders = new HashMap<>();
        binders.put("hospitalBeds", (builder, node, campaign, version) -> builder.hospitalBeds = parseInt(node));
        binders.put("kitchens", (builder, node, campaign, version) -> builder.kitchens = parseInt(node));
        binders.put("holdingCells", (builder, node, campaign, version) -> builder.holdingCells = parseInt(node));
        return binders;
    }

    private static RentedFacilitiesData parseRentedFacilitiesData(final Node wn) {
        final RentedFacilitiesBuilder builder = readFields(wn, new RentedFacilitiesBuilder(),
              RENTED_FACILITIES_BINDERS, null, null, "rentedFacilitiesData");
        return new RentedFacilitiesData(builder.hospitalBeds, builder.kitchens, builder.holdingCells);
    }

    private static final class MoraleDataBuilder {
        ContractMoraleLevel moraleLevel;
        LocalDate routEndDate;
        Money routedPayout = Money.zero();
    }

    private static final Map<String, FieldBinder<MoraleDataBuilder>> MORALE_BINDERS = createMoraleBinders();

    private static Map<String, FieldBinder<MoraleDataBuilder>> createMoraleBinders() {
        final Map<String, FieldBinder<MoraleDataBuilder>> binders = new HashMap<>();
        binders.put("moraleLevel",
              (builder, node, campaign, version) -> builder.moraleLevel = ContractMoraleLevel.valueOf(text(node)));
        binders.put("routEndDate",
              (builder, node, campaign, version) -> builder.routEndDate = MHQXMLUtility.parseDate(text(node)));
        binders.put("routedPayout",
              (builder, node, campaign, version) -> builder.routedPayout = Money.fromXmlString(text(node)));
        return binders;
    }

    private static MoraleData parseMoraleData(final Node wn) {
        final MoraleDataBuilder builder = readFields(wn, new MoraleDataBuilder(), MORALE_BINDERS, null, null,
              "moraleData");
        return new MoraleData(builder.moraleLevel, builder.routEndDate, builder.routedPayout);
    }

    private static final class NegotiationDataBuilder {
        int originalPayStep;
        int originalSupportStep;
        int originalTransportStep;
        int originalSalvageStep;
        int originalCommandStep;
        int reputationUsed;
        int swapsUsed;
        int sacrificeBank;
        List<List<TermFunding>> funding = new ArrayList<>();
    }

    private static final Map<String, FieldBinder<NegotiationDataBuilder>> NEGOTIATION_BINDERS =
          createNegotiationBinders();

    private static Map<String, FieldBinder<NegotiationDataBuilder>> createNegotiationBinders() {
        final Map<String, FieldBinder<NegotiationDataBuilder>> binders = new HashMap<>();
        binders.put("originalPayStep", (builder, node, campaign, version) -> builder.originalPayStep = parseInt(node));
        binders.put("originalSupportStep",
              (builder, node, campaign, version) -> builder.originalSupportStep = parseInt(node));
        binders.put("originalTransportStep",
              (builder, node, campaign, version) -> builder.originalTransportStep = parseInt(node));
        binders.put("originalSalvageStep",
              (builder, node, campaign, version) -> builder.originalSalvageStep = parseInt(node));
        binders.put("originalCommandStep",
              (builder, node, campaign, version) -> builder.originalCommandStep = parseInt(node));
        binders.put("reputationUsed", (builder, node, campaign, version) -> builder.reputationUsed = parseInt(node));
        binders.put("swapsUsed", (builder, node, campaign, version) -> builder.swapsUsed = parseInt(node));
        binders.put("sacrificeBank", (builder, node, campaign, version) -> builder.sacrificeBank = parseInt(node));
        binders.put("funding", (builder, node, campaign, version) -> builder.funding = parseFunding(node));
        return binders;
    }

    private static NegotiationData parseNegotiationData(final Node wn) {
        final NegotiationDataBuilder builder = readFields(wn, new NegotiationDataBuilder(), NEGOTIATION_BINDERS, null,
              null, "negotiationData");
        return new NegotiationData(builder.originalPayStep, builder.originalSupportStep, builder.originalTransportStep,
              builder.originalSalvageStep, builder.originalCommandStep, builder.reputationUsed, builder.swapsUsed,
              builder.sacrificeBank, builder.funding);
    }

    private static List<List<TermFunding>> parseFunding(final Node wn) {
        final List<List<TermFunding>> funding = new ArrayList<>();
        final NodeList children = wn.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE || !"clause".equals(child.getNodeName())) {
                continue;
            }
            final List<TermFunding> clause = new ArrayList<>();
            final String content = child.getTextContent().trim();
            if (!content.isEmpty()) {
                for (final String entry : content.split(",")) {
                    clause.add(TermFunding.valueOf(entry.trim()));
                }
            }
            funding.add(clause);
        }
        return funding;
    }

    private static void parseScenarios(final AbstractContract contract, final Node wn, final Campaign campaign,
          final Version version) {
        final NodeList children = wn.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE || !"scenario".equalsIgnoreCase(child.getNodeName())) {
                continue;
            }
            final Scenario scenario = Scenario.generateInstanceFromXML(child, campaign, version);
            if (scenario != null) {
                contract.getScenarios().add(scenario);
            }
        }
    }

    private static @Nullable Person parseWrappedPerson(final Node wrapper, final Campaign campaign,
          final Version version) {
        final NodeList children = wrapper.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && "person".equalsIgnoreCase(child.getNodeName())) {
                return Person.generateInstanceFromXML(child, campaign, version);
            }
        }
        return null;
    }

    private static String text(final Node node) {
        return node.getTextContent().trim();
    }

    private static int parseInt(final Node node) {
        return MathUtility.parseInt(text(node));
    }
    // endregion Read
}
