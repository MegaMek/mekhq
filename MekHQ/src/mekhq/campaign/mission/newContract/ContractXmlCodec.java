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
package mekhq.campaign.mission.newContract;

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
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.enums.ContractMoraleLevel;
import mekhq.campaign.mission.enums.ContractObjectiveType;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.mission.newContract.contractData.*;
import mekhq.campaign.mission.newContract.contractGeneration.ChaosEmployerType;
import mekhq.campaign.mission.newContract.contractGeneration.ContractTermsData;
import mekhq.campaign.mission.newContract.contractGeneration.negotiationsAndNPCs.TermFunding;
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

    static final String CONTRACT_TAG = "contract";
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

        if (contract.getContractId() != null) {
            MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "contractId", contract.getContractId());
        }
        writeStringIfPresent(printWriter, indent, "contractName", contract.getContractName());
        writeStringIfPresent(printWriter, indent, "description", contract.getDescription());
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "scale", contract.getScale());
        MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "trackCount", contract.getTrackCount());
        if (contract.getMissionStatus() != null) {
            MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "missionStatus", contract.getMissionStatus().name());
        }
        // The player's chosen negotiator is a roster member, so persist only their id and re-resolve on load.
        if (contract.getPlayerNegotiator() != null) {
            MHQXMLUtility.writeSimpleXMLTag(printWriter, indent, "playerNegotiatorId",
                  contract.getPlayerNegotiator().getId());
        }

        writeEmployerData(printWriter, indent, contract.getEmployerData(), campaign);
        writeEnemyData(printWriter, indent, contract.getEnemyData());
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

    private static void writeEnemyData(final PrintWriter pw, int indent, final @Nullable EnemyData data) {
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
        readers.put("missionStatus",
              (contract, node, campaign, version) -> contract.setMissionStatus(MissionStatus.parseFromString(text(node))));
        readers.put("playerNegotiatorId",
              (contract, node, campaign, version) -> contract.setPlayerNegotiator(campaign.getPerson(UUID.fromString(
                    text(
                          node)))));

        readers.put("employerData",
              (contract, node, campaign, version) -> contract.setEmployerData(parseEmployerData(node,
                    campaign,
                    version)));
        readers.put("enemyData", (contract, node, campaign, version) -> contract.setEnemyData(parseEnemyData(node)));
        readers.put("contractTerms",
              (contract, node, campaign, version) -> contract.setContractTerms(parseContractTerms(node)));
        readers.put("objectiveData",
              (contract, node, campaign, version) -> contract.setObjectiveData(parseObjectiveData(node)));
        readers.put("contractFinanceData",
              (contract, node, campaign, version) -> contract.setContractFinanceData(parseFinanceData(node)));
        readers.put("scheduleData",
              (contract, node, campaign, version) -> contract.setScheduleData(parseScheduleData(node)));
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

    private static EmployerData parseEmployerData(final Node wn, final Campaign campaign, final Version version) {
        ChaosEmployerType type = null;
        String factionCode = null;
        String anchorFactionCode = null;
        String sponsorFactionCode = null;
        String displayName = null;
        SkillLevel forceSkill = SkillLevel.REGULAR;
        int equipmentRating = 0;
        Camouflage camouflage = new Camouflage();
        PlayerColour color = PlayerColour.BLUE;
        Person negotiator = null;
        Person liaison = null;

        final NodeList children = wn.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            switch (child.getNodeName()) {
                case "type" -> type = ChaosEmployerType.valueOf(text(child));
                case "factionCode" -> factionCode = text(child);
                case "anchorFactionCode" -> anchorFactionCode = text(child);
                case "sponsorFactionCode" -> sponsorFactionCode = text(child);
                case "displayName" -> displayName = text(child);
                case "forceSkill" -> forceSkill = SkillLevel.valueOf(text(child));
                case "equipmentRating" -> equipmentRating = parseInt(child);
                case "color" -> color = PlayerColour.valueOf(text(child));
                case Camouflage.XML_TAG -> camouflage = Camouflage.parseFromXML(child);
                case "negotiator" -> negotiator = parseWrappedPerson(child, campaign, version);
                case "liaison" -> liaison = parseWrappedPerson(child, campaign, version);
                default -> LOGGER.warn("Unexpected employerData element ignored: {}", child.getNodeName());
            }
        }

        return new EmployerData(type, factionCode, anchorFactionCode, sponsorFactionCode, displayName, negotiator,
              liaison, forceSkill, equipmentRating, camouflage, color);
    }

    private static EnemyData parseEnemyData(final Node wn) {
        String factionCode = null;
        String sponsorFactionCode = null;
        String displayName = null;
        SkillLevel forceSkill = SkillLevel.REGULAR;
        int equipmentRating = 0;
        Camouflage camouflage = new Camouflage();
        PlayerColour color = PlayerColour.RED;
        boolean batchallAccepted = true;

        final NodeList children = wn.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            switch (child.getNodeName()) {
                case "factionCode" -> factionCode = text(child);
                case "sponsorFactionCode" -> sponsorFactionCode = text(child);
                case "displayName" -> displayName = text(child);
                case "forceSkill" -> forceSkill = SkillLevel.valueOf(text(child));
                case "equipmentRating" -> equipmentRating = parseInt(child);
                case "color" -> color = PlayerColour.valueOf(text(child));
                case "batchallAccepted" -> batchallAccepted = Boolean.parseBoolean(text(child));
                case Camouflage.XML_TAG -> camouflage = Camouflage.parseFromXML(child);
                default -> LOGGER.warn("Unexpected enemyData element ignored: {}", child.getNodeName());
            }
        }

        return new EnemyData(factionCode, sponsorFactionCode, displayName, forceSkill, equipmentRating, camouflage,
              color, batchallAccepted);
    }

    private static ContractTermsData parseContractTerms(final Node wn) {
        ChaosContractStepsTable payRate = null;
        ChaosContractStepsTable support = null;
        ChaosContractStepsTable transport = null;
        ChaosContractStepsTable salvageRights = null;
        ChaosContractStepsTable commandRights = null;

        final NodeList children = wn.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            switch (child.getNodeName()) {
                case "payRate" -> payRate = ChaosContractStepsTable.valueOf(text(child));
                case "support" -> support = ChaosContractStepsTable.valueOf(text(child));
                case "transport" -> transport = ChaosContractStepsTable.valueOf(text(child));
                case "salvageRights" -> salvageRights = ChaosContractStepsTable.valueOf(text(child));
                case "commandRights" -> commandRights = ChaosContractStepsTable.valueOf(text(child));
                default -> LOGGER.warn("Unexpected contractTerms element ignored: {}", child.getNodeName());
            }
        }

        return new ContractTermsData(payRate, support, transport, salvageRights, commandRights);
    }

    private static ContractObjectiveData parseObjectiveData(final Node wn) {
        ContractObjectiveType playerObjectiveType = null;
        ContractObjectiveType opposingObjectiveType = null;

        final NodeList children = wn.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            switch (child.getNodeName()) {
                case "playerObjectiveType" -> playerObjectiveType = ContractObjectiveType.valueOf(text(child));
                case "opposingObjectiveType" -> opposingObjectiveType = ContractObjectiveType.valueOf(text(child));
                default -> LOGGER.warn("Unexpected objectiveData element ignored: {}", child.getNodeName());
            }
        }

        return new ContractObjectiveData(playerObjectiveType, opposingObjectiveType);
    }

    private static ContractFinanceData parseFinanceData(final Node wn) {
        Money transport = Money.zero();
        Money monthlyPay = Money.zero();
        Money combatPay = Money.zero();

        final NodeList children = wn.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            switch (child.getNodeName()) {
                case "transport" -> transport = Money.fromXmlString(text(child));
                case "monthlyPay" -> monthlyPay = Money.fromXmlString(text(child));
                case "combatPay" -> combatPay = Money.fromXmlString(text(child));
                default -> LOGGER.warn("Unexpected contractFinanceData element ignored: {}", child.getNodeName());
            }
        }

        return new ContractFinanceData(transport, monthlyPay, combatPay);
    }

    private static ContractScheduleData parseScheduleData(final Node wn) {
        LocalDate startDate = null;
        LocalDate endDate = null;
        int lengthInMonths = 0;

        final NodeList children = wn.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            switch (child.getNodeName()) {
                case "startDate" -> startDate = MHQXMLUtility.parseDate(text(child));
                case "endDate" -> endDate = MHQXMLUtility.parseDate(text(child));
                case "lengthInMonths" -> lengthInMonths = parseInt(child);
                default -> LOGGER.warn("Unexpected scheduleData element ignored: {}", child.getNodeName());
            }
        }

        return new ContractScheduleData(startDate, endDate, lengthInMonths);
    }

    private static SystemsTargetData parseSystemsTargetData(final Node wn) {
        String systemId = null;
        String planetId = null;

        final NodeList children = wn.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            switch (child.getNodeName()) {
                case "systemId" -> systemId = text(child);
                case "planetId" -> planetId = text(child);
                default -> LOGGER.warn("Unexpected systemsTargetData element ignored: {}", child.getNodeName());
            }
        }

        return new SystemsTargetData(systemId, planetId);
    }

    private static RentedFacilitiesData parseRentedFacilitiesData(final Node wn) {
        int hospitalBeds = 0;
        int kitchens = 0;
        int holdingCells = 0;

        final NodeList children = wn.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            switch (child.getNodeName()) {
                case "hospitalBeds" -> hospitalBeds = parseInt(child);
                case "kitchens" -> kitchens = parseInt(child);
                case "holdingCells" -> holdingCells = parseInt(child);
                default -> LOGGER.warn("Unexpected rentedFacilitiesData element ignored: {}", child.getNodeName());
            }
        }

        return new RentedFacilitiesData(hospitalBeds, kitchens, holdingCells);
    }

    private static MoraleData parseMoraleData(final Node wn) {
        ContractMoraleLevel moraleLevel = null;
        LocalDate routEndDate = null;
        Money routedPayout = Money.zero();

        final NodeList children = wn.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            switch (child.getNodeName()) {
                case "moraleLevel" -> moraleLevel = ContractMoraleLevel.valueOf(text(child));
                case "routEndDate" -> routEndDate = MHQXMLUtility.parseDate(text(child));
                case "routedPayout" -> routedPayout = Money.fromXmlString(text(child));
                default -> LOGGER.warn("Unexpected moraleData element ignored: {}", child.getNodeName());
            }
        }

        return new MoraleData(moraleLevel, routEndDate, routedPayout);
    }

    private static NegotiationData parseNegotiationData(final Node wn) {
        int originalPayStep = 0;
        int originalSupportStep = 0;
        int originalTransportStep = 0;
        int originalSalvageStep = 0;
        int originalCommandStep = 0;
        int reputationUsed = 0;
        int swapsUsed = 0;
        int sacrificeBank = 0;
        final List<List<TermFunding>> funding = new ArrayList<>();

        final NodeList children = wn.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            switch (child.getNodeName()) {
                case "originalPayStep" -> originalPayStep = parseInt(child);
                case "originalSupportStep" -> originalSupportStep = parseInt(child);
                case "originalTransportStep" -> originalTransportStep = parseInt(child);
                case "originalSalvageStep" -> originalSalvageStep = parseInt(child);
                case "originalCommandStep" -> originalCommandStep = parseInt(child);
                case "reputationUsed" -> reputationUsed = parseInt(child);
                case "swapsUsed" -> swapsUsed = parseInt(child);
                case "sacrificeBank" -> sacrificeBank = parseInt(child);
                case "funding" -> funding.addAll(parseFunding(child));
                default -> LOGGER.warn("Unexpected negotiationData element ignored: {}", child.getNodeName());
            }
        }

        return new NegotiationData(originalPayStep, originalSupportStep, originalTransportStep, originalSalvageStep,
              originalCommandStep, reputationUsed, swapsUsed, sacrificeBank, funding);
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
        return Integer.parseInt(text(node));
    }
    // endregion Read
}
