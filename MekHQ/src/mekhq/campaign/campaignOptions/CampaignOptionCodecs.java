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
package mekhq.campaign.campaignOptions;

import static java.lang.Boolean.parseBoolean;
import static megamek.codeUtilities.MathUtility.parseDouble;
import static megamek.codeUtilities.MathUtility.parseInt;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import megamek.Version;
import megamek.common.enums.SkillLevel;
import megamek.logging.MMLogger;
import mekhq.Utilities;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.autoResolve.AutoResolveMethod;
import mekhq.campaign.digitalGM.stratCon.StratConPlayType;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.FinancialYearDuration;
import mekhq.campaign.market.enums.ContractMarketMethod;
import mekhq.campaign.market.enums.UnitMarketMethod;
import mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle;
import mekhq.campaign.personnel.enums.*;
import mekhq.campaign.randomEvents.prisoners.PrisonerCaptureStyle;
import mekhq.campaign.universe.PlanetarySystem.PlanetaryRating;
import mekhq.campaign.universe.PlanetarySystem.PlanetarySophistication;
import mekhq.gui.campaignOptions.enums.ProcurementPersonnelPick;
import mekhq.service.mrms.MRMSOption;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The serialization strategy table that lets campaign-option XML I/O be driven by {@link CampaignOption#xmlTag()}
 * rather than parallel hand-maintained code. {@link CampaignOptionsMarshaller} loops over
 * {@link CampaignOption#values()} calling {@link #writeAll}; {@link CampaignOptionsUnmarshaller} dispatches each tag
 * through {@link #readTag}.
 *
 * <p>Scalar options ({@link Boolean}/{@link Integer}/{@link Double}/{@link String}) resolve to a default codec by
 * {@link CampaignOption#type()}. Enums and the handful of collection/array/object options register an explicit codec
 * here; those codecs call the option's existing getter/setter so clamps, side effects, and merge-into-default behavior
 * match the previous hand-written serializers exactly.</p>
 */
final class CampaignOptionCodecs {
    private static final MMLogger LOGGER = MMLogger.create(CampaignOptionCodecs.class);

    private static final Map<CampaignOption<?>, CampaignOptionCodec<?>> CODECS = new HashMap<>();
    private static final Map<String, CampaignOption<?>> TAG_TO_OPTION = new HashMap<>();
    // region Default scalar codecs
    private static final CampaignOptionCodec<Boolean> BOOL = CampaignOptionCodec.of(
          (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                (boolean) options.get(option)),
          (node, text, version, option, options) -> options.set(option, parseBoolean(text)));
    private static final CampaignOptionCodec<Integer> INT = CampaignOptionCodec.of(
          (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                (int) options.get(option)),
          (node, text, version, option, options) -> options.set(option, parseInt(text)));
    private static final CampaignOptionCodec<Double> DOUBLE = CampaignOptionCodec.of(
          (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                (double) options.get(option)),
          (node, text, version, option, options) -> options.set(option, parseDouble(text)));
    private static final CampaignOptionCodec<String> STRING = CampaignOptionCodec.of(
          (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                options.get(option)),
          (node, text, version, option, options) -> options.set(option, text));

    static {
        registerEnums();
        registerClampedContractPercents();
        registerCollectionsAndArrays();
        registerSpecials();

        for (final CampaignOption<?> option : CampaignOption.values()) {
            TAG_TO_OPTION.put(option.xmlTag(), option);
        }
        // Legacy read-only aliases: old tag -> the option that now owns that value (its canonical tag is written).
        TAG_TO_OPTION.put("administrativeStrain", CampaignOption.HR_CAPACITY);
        TAG_TO_OPTION.put("useAdministrativeStrain", CampaignOption.USE_HR_STRAIN);
        TAG_TO_OPTION.put("noInterestInRelationshipsDiceSize", CampaignOption.NO_INTEREST_IN_RELATIONSHIPS_DICE_SIZE);
        TAG_TO_OPTION.put("interestedInSameSexDiceSize", CampaignOption.INTERESTED_IN_SAME_SEX_DICE_SIZE);
        TAG_TO_OPTION.put("unitMarketSpecialUnitChance", CampaignOption.UNIT_MARKET_ARTILLERY_UNIT_CHANCE);
        TAG_TO_OPTION.put("autoGenerateOpForCallsigns", CampaignOption.AUTO_GENERATE_OP_FOR_CALL_SIGNS);
    }
    // endregion Default scalar codecs

    private CampaignOptionCodecs() {}

    /** An enum codec that persists {@code Enum.name()}. */
    private static <E extends Enum<E>> CampaignOptionCodec<E> enumCodec(final Function<String, E> parse) {
        return enumCodec(parse, Enum::name);
    }

    /** An enum codec with a custom textual form (e.g. a lookup name/key). */
    private static <E extends Enum<E>> CampaignOptionCodec<E> enumCodec(final Function<String, E> parse,
          final Function<E, String> toText) {
        return CampaignOptionCodec.of(
              (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                    toText.apply(options.get(option))),
              (node, text, version, option, options) -> options.set(option, parse.apply(text)));
    }

    private static <T> void register(final CampaignOption<T> option, final CampaignOptionCodec<T> codec) {
        CODECS.put(option, codec);
    }

    private static void registerEnums() {
        register(CampaignOption.TIME_IN_SERVICE_DISPLAY_FORMAT, enumCodec(TimeInDisplayFormat::valueOf));
        register(CampaignOption.TIME_IN_RANK_DISPLAY_FORMAT, enumCodec(TimeInDisplayFormat::valueOf));
        register(CampaignOption.PRISONER_CAPTURE_STYLE, enumCodec(PrisonerCaptureStyle::fromString));
        register(CampaignOption.PLANET_ACQUISITION_FACTION_LIMIT,
              enumCodec(PlanetaryAcquisitionFactionLimit::parseFromString));
        register(CampaignOption.ACQUISITION_PERSONNEL_CATEGORY, enumCodec(ProcurementPersonnelPick::fromString));
        register(CampaignOption.AWARD_BONUS_STYLE, enumCodec(AwardBonus::valueOf));
        register(CampaignOption.FAMILY_DISPLAY_LEVEL, enumCodec(FamilialRelationshipDisplayLevel::parseFromString));
        register(CampaignOption.RANDOM_MARRIAGE_METHOD, enumCodec(RandomMarriageMethod::fromString));
        register(CampaignOption.RANDOM_DIVORCE_METHOD, enumCodec(RandomDivorceMethod::fromString));
        register(CampaignOption.BABY_SURNAME_STYLE, enumCodec(BabySurnameStyle::parseFromString));
        register(CampaignOption.RANDOM_PROCREATION_METHOD, enumCodec(RandomProcreationMethod::fromString));
        register(CampaignOption.TURNOVER_FREQUENCY, enumCodec(TurnoverFrequency::valueOf));
        register(CampaignOption.FINANCIAL_YEAR_DURATION, enumCodec(FinancialYearDuration::parseFromString));
        register(CampaignOption.PERSONNEL_MARKET_STYLE, enumCodec(PersonnelMarketStyle::fromString));
        register(CampaignOption.UNIT_MARKET_METHOD, enumCodec(UnitMarketMethod::valueOf));
        register(CampaignOption.CONTRACT_MARKET_METHOD, enumCodec(ContractMarketMethod::valueOf));
        register(CampaignOption.SKILL_LEVEL, enumCodec(SkillLevel::parseFromString));
        register(CampaignOption.MINIMUM_CALLSIGN_SKILL_LEVEL, enumCodec(SkillLevel::parseFromString));
        register(CampaignOption.AUTO_RESOLVE_METHOD, enumCodec(AutoResolveMethod::valueOf));

        // Enums persisted by a lookup name/key rather than Enum.name().
        register(CampaignOption.ACQUISITIONS_TYPE,
              enumCodec(AcquisitionsType::parseFromLookupName, AcquisitionsType::getLookupName));
        register(CampaignOption.BOARD_SCALING_TYPE,
              enumCodec(BoardScalingType::parseFromLookupName, BoardScalingType::getLookupName));
        register(CampaignOption.EDGE_REFRESH_PERIOD,
              enumCodec(EdgeRefreshPeriod::fromString, EdgeRefreshPeriod::getLookupKey));
        register(CampaignOption.STRAT_CON_PLAY_TYPE,
              enumCodec(StratConPlayType::fromLookupName, StratConPlayType::getLookupName));
    }

    /** The four contract percents clamp on write; their setters enforce the ceiling, so read routes through them. */
    private static void registerClampedContractPercents() {
        register(CampaignOption.EQUIPMENT_CONTRACT_PERCENT, CampaignOptionCodec.of(
              (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                    options.getEquipmentContractPercent()),
              (node, text, version, option, options) -> options.setEquipmentContractPercent(parseDouble(text))));
        register(CampaignOption.DROP_SHIP_CONTRACT_PERCENT, CampaignOptionCodec.of(
              (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                    options.getDropShipContractPercent()),
              (node, text, version, option, options) -> options.setDropShipContractPercent(parseDouble(text))));
        register(CampaignOption.JUMP_SHIP_CONTRACT_PERCENT, CampaignOptionCodec.of(
              (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                    options.getJumpShipContractPercent()),
              (node, text, version, option, options) -> options.setJumpShipContractPercent(parseDouble(text))));
        register(CampaignOption.WAR_SHIP_CONTRACT_PERCENT, CampaignOptionCodec.of(
              (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                    options.getWarShipContractPercent()),
              (node, text, version, option, options) -> options.setWarShipContractPercent(parseDouble(text))));
    }

    private static void registerCollectionsAndArrays() {
        // region Maps written as nested per-entry tags
        register(CampaignOption.SALARY_XP_MULTIPLIERS, CampaignOptionCodec.of(
              (pw, indent, option, options) -> {
                  MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent, option.xmlTag());
                  for (final Entry<SkillLevel, Double> entry : options.getSalaryXPMultipliers().entrySet()) {
                      MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, entry.getKey().name(), entry.getValue());
                  }
                  MHQXMLUtility.writeSimpleXMLCloseTag(pw, indent, option.xmlTag());
              },
              (node, text, version, option, options) -> {
                  if (!node.hasChildNodes()) {
                      return;
                  }
                  final NodeList children = node.getChildNodes();
                  for (int i = 0; i < children.getLength(); i++) {
                      final Node child = children.item(i);
                      if (child.getNodeType() != Node.ELEMENT_NODE) {
                          continue;
                      }
                      options.getSalaryXPMultipliers()
                            .put(SkillLevel.valueOf(child.getNodeName().trim()),
                                  parseDouble(child.getTextContent().trim()));
                  }
              }));

        register(CampaignOption.MARRIAGE_SURNAME_WEIGHTS, CampaignOptionCodec.of(
              (pw, indent, option, options) -> {
                  MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent, option.xmlTag());
                  for (final Entry<MergingSurnameStyle, Integer> entry : options.getMarriageSurnameWeights()
                                                                               .entrySet()) {
                      MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, entry.getKey().name(), entry.getValue());
                  }
                  MHQXMLUtility.writeSimpleXMLCloseTag(pw, indent, option.xmlTag());
              },
              (node, text, version, option, options) -> {
                  if (!node.hasChildNodes()) {
                      return;
                  }
                  final NodeList children = node.getChildNodes();
                  for (int i = 0; i < children.getLength(); i++) {
                      final Node child = children.item(i);
                      if (child.getNodeType() != Node.ELEMENT_NODE) {
                          continue;
                      }
                      options.getMarriageSurnameWeights()
                            .put(MergingSurnameStyle.parseFromString(child.getNodeName().trim()),
                                  parseInt(child.getTextContent().trim()));
                  }
              }));

        register(CampaignOption.DIVORCE_SURNAME_WEIGHTS, CampaignOptionCodec.of(
              (pw, indent, option, options) -> {
                  MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent, option.xmlTag());
                  for (final Entry<SplittingSurnameStyle, Integer> entry : options.getDivorceSurnameWeights()
                                                                                 .entrySet()) {
                      MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, entry.getKey().name(), entry.getValue());
                  }
                  MHQXMLUtility.writeSimpleXMLCloseTag(pw, indent, option.xmlTag());
              },
              (node, text, version, option, options) -> {
                  if (!node.hasChildNodes()) {
                      return;
                  }
                  final NodeList children = node.getChildNodes();
                  for (int i = 0; i < children.getLength(); i++) {
                      final Node child = children.item(i);
                      if (child.getNodeType() != Node.ELEMENT_NODE) {
                          continue;
                      }
                      options.getDivorceSurnameWeights()
                            .put(SplittingSurnameStyle.valueOf(child.getNodeName().trim()),
                                  parseInt(child.getTextContent().trim()));
                  }
              }));

        register(CampaignOption.ENABLED_RANDOM_DEATH_AGE_GROUPS, CampaignOptionCodec.of(
              (pw, indent, option, options) -> {
                  MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent, option.xmlTag());
                  for (final Entry<AgeGroup, Boolean> entry : options.getEnabledRandomDeathAgeGroups().entrySet()) {
                      MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, entry.getKey().name(), entry.getValue());
                  }
                  MHQXMLUtility.writeSimpleXMLCloseTag(pw, indent, option.xmlTag());
              },
              (node, text, version, option, options) -> {
                  if (!node.hasChildNodes()) {
                      return;
                  }
                  final NodeList children = node.getChildNodes();
                  for (int i = 0; i < children.getLength(); i++) {
                      final Node child = children.item(i);
                      try {
                          options.getEnabledRandomDeathAgeGroups()
                                .put(AgeGroup.valueOf(child.getNodeName()),
                                      parseBoolean(child.getTextContent().trim()));
                      } catch (Exception ignored) {
                          // skip unrecognized age groups (as the legacy handler did)
                      }
                  }
              }));

        register(CampaignOption.PERSONNEL_MARKET_RANDOM_REMOVAL_TARGETS, CampaignOptionCodec.of(
              (pw, indent, option, options) -> {
                  MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent, option.xmlTag());
                  for (final Entry<SkillLevel, Integer> entry : options.getPersonnelMarketRandomRemovalTargets()
                                                                      .entrySet()) {
                      MHQXMLUtility.writeSimpleXMLTag(pw, indent + 1, entry.getKey().name(), entry.getValue());
                  }
                  MHQXMLUtility.writeSimpleXMLCloseTag(pw, indent, option.xmlTag());
              },
              (node, text, version, option, options) -> {
                  if (!node.hasChildNodes()) {
                      return;
                  }
                  final NodeList children = node.getChildNodes();
                  for (int i = 0; i < children.getLength(); i++) {
                      final Node child = children.item(i);
                      if (child.getNodeType() != Node.ELEMENT_NODE) {
                          continue;
                      }
                      options.getPersonnelMarketRandomRemovalTargets()
                            .put(SkillLevel.valueOf(child.getNodeName().trim()),
                                  parseInt(child.getTextContent().trim()));
                  }
              }));
        // endregion Maps written as nested per-entry tags

        // region Planet acquisition bonuses (comma-joined by enum order, with < 0.50.07 length compatibility)
        register(CampaignOption.PLANET_TECH_ACQUISITION_BONUS, CampaignOptionCodec.of(
              (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                    Arrays.stream(PlanetarySophistication.values())
                          .map(value -> options.getAllPlanetTechAcquisitionBonuses().getOrDefault(value, 0).toString())
                          .collect(Collectors.joining(","))),
              (node, text, version, option, options) -> {
                  final EnumMap<PlanetarySophistication, Integer> bonuses = options.getAllPlanetTechAcquisitionBonuses();
                  final String[] values = text.split(",");
                  if (values.length == 6) {
                      bonuses.put(PlanetarySophistication.A, parseInt(values[0]));
                      bonuses.put(PlanetarySophistication.B, parseInt(values[1]));
                      bonuses.put(PlanetarySophistication.C, parseInt(values[2]));
                      bonuses.put(PlanetarySophistication.D, parseInt(values[3]));
                      bonuses.put(PlanetarySophistication.F, parseInt(values[5]));
                  } else if (values.length == PlanetarySophistication.values().length) {
                      for (int i = 0; i < values.length; i++) {
                          bonuses.put(PlanetarySophistication.fromIndex(i), parseInt(values[i]));
                      }
                  } else {
                      LOGGER.error("Invalid number of values for planetTechAcquisitionBonus: {}", values.length);
                  }
              }));

        register(CampaignOption.PLANET_INDUSTRY_ACQUISITION_BONUS, CampaignOptionCodec.of(
              (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                    Arrays.stream(PlanetaryRating.values())
                          .map(value -> options.getAllPlanetIndustryAcquisitionBonuses()
                                              .getOrDefault(value, 0)
                                              .toString())
                          .collect(Collectors.joining(","))),
              (node, text, version, option, options) -> readPlanetRatingBonuses(text,
                    options.getAllPlanetIndustryAcquisitionBonuses(), "planetIndustryAcquisitionBonus")));

        register(CampaignOption.PLANET_OUTPUT_ACQUISITION_BONUS, CampaignOptionCodec.of(
              (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                    Arrays.stream(PlanetaryRating.values())
                          .map(value -> options.getAllPlanetOutputAcquisitionBonuses()
                                              .getOrDefault(value, 0)
                                              .toString())
                          .collect(Collectors.joining(","))),
              (node, text, version, option, options) -> readPlanetRatingBonuses(text,
                    options.getAllPlanetOutputAcquisitionBonuses(), "planetOutputAcquisitionBonus")));
        // endregion Planet acquisition bonuses

        // region Primitive arrays (single tag; delimited value merged into the existing array)
        register(CampaignOption.USED_PART_PRICE_MULTIPLIERS, CampaignOptionCodec.of(
              (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                    options.getUsedPartPriceMultipliers()),
              (node, text, version, option, options) -> {
                  final String[] values = text.split(",");
                  for (int i = 0; i < values.length; i++) {
                      try {
                          options.getUsedPartPriceMultipliers()[i] = parseDouble(values[i]);
                      } catch (Exception ignored) {
                          // ignore extra/short entries, as the legacy handler did
                      }
                  }
              }));

        register(CampaignOption.PHENOTYPE_PROBABILITIES, CampaignOptionCodec.of(
              (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                    options.getPhenotypeProbabilities()),
              (node, text, version, option, options) -> {
                  final String[] values = text.split(",");
                  for (int i = 0; i < values.length; i++) {
                      options.setPhenotypeProbability(i, parseInt(values[i]));
                  }
              }));

        register(CampaignOption.USE_PORTRAIT_FOR_ROLE, CampaignOptionCodec.of(
              (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                    options.isUsePortraitForRoles()),
              (node, text, version, option, options) -> {
                  final String[] values = text.split(",");
                  for (int i = 0; i < values.length; i++) {
                      options.setUsePortraitForRole(i, parseBoolean(values[i].trim()));
                  }
              }));

        register(CampaignOption.ATB_BATTLE_CHANCE, CampaignOptionCodec.of(
              (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                    options.getAllAtBBattleChances()),
              (node, text, version, option, options) -> {
                  final String[] values = text.split(",");
                  for (int i = 0; i < values.length; i++) {
                      try {
                          options.setAtBBattleChance(i, parseInt(values[i]));
                      } catch (Exception ignored) {
                          options.setAtBBattleChance(i, (int) Math.round(parseDouble(values[i])));
                      }
                  }
              }));
        // endregion Primitive arrays

        // region Objects with their own XML (de)serialization
        register(CampaignOption.MRMS_OPTIONS, CampaignOptionCodec.of(
              (pw, indent, option, options) -> {
                  MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent, option.xmlTag());
                  for (final MRMSOption mrmsOption : options.getMRMSOptions()) {
                      mrmsOption.writeToXML(pw, indent + 1);
                  }
                  MHQXMLUtility.writeSimpleXMLCloseTag(pw, indent, option.xmlTag());
              },
              (node, text, version, option, options) -> options.setMRMSOptions(MRMSOption.parseListFromXML(node,
                    version))));

        register(CampaignOption.RANDOM_ORIGIN_OPTIONS, CampaignOptionCodec.of(
              (pw, indent, option, options) -> options.getRandomOriginOptions().writeToXML(pw, indent),
              (node, text, version, option, options) -> {
                  if (!node.hasChildNodes()) {
                      return;
                  }
                  final RandomOriginOptions randomOriginOptions =
                        RandomOriginOptions.parseFromXML(node.getChildNodes(), true);
                  if (randomOriginOptions == null) {
                      return;
                  }
                  options.setRandomOriginOptions(randomOriginOptions);
              }));

        register(CampaignOption.ROLE_BASE_SALARIES, CampaignOptionCodec.of(
              (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                    Utilities.printMoneyArray(options.getRoleBaseSalaries())),
              (node, text, version, option, options) -> {
                  final Money[] defaultSalaries = options.getRoleBaseSalaries();
                  final Money[] newSalaries = Utilities.readMoneyArray(node);
                  final Money[] mergedSalaries = new Money[PersonnelRole.values().length];
                  for (int i = 0; i < mergedSalaries.length; i++) {
                      try {
                          mergedSalaries[i] = (newSalaries[i] != null) ? newSalaries[i] : defaultSalaries[i];
                      } catch (Exception e) {
                          // newSalaries can be shorter than the role list when a new profession has been added
                          if (defaultSalaries != null) {
                              mergedSalaries[i] = defaultSalaries[i];
                          }
                      }
                  }
                  options.setRoleBaseSalaries(mergedSalaries);
              }));
        // endregion Objects with their own XML (de)serialization
    }

    private static void registerSpecials() {
        // Strategic view theme is derived through the client preferences on both read and write.
        register(CampaignOption.STRATEGIC_VIEW_MINIMAP_THEME, CampaignOptionCodec.of(
              (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                    options.getStrategicViewTheme().getName()),
              (node, text, version, option, options) -> options.setStrategicViewTheme(text)));

        // Personnel market name has a pre-CamOps backward-compatibility rename.
        register(CampaignOption.PERSONNEL_MARKET_NAME, CampaignOptionCodec.of(
              (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                    options.getPersonnelMarketName()),
              (node, text, version, option, options) ->
                    options.setPersonnelMarketName(text.equals("Strat Ops") ? "Campaign Ops" : text)));

        // Legacy in-memory marker: read from the old "useAtB" tag on presence, never written.
        register(CampaignOption.HAD_AT_B_ENABLED_MARKER,
              CampaignOptionCodec.readOnly((node, text, version, option, options) ->
                                                 options.setHadAtBEnabledMarker(true)));

        // Regard multiplier falls back to 1.0 (rather than 0.0) when a save has a malformed value.
        register(CampaignOption.REGARD_MULTIPLIER, CampaignOptionCodec.of(
              (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                    (double) options.get(option)),
              (node, text, version, option, options) -> options.set(option, parseDouble(text, 1.0))));

        // Alternative advanced medical healing-time multiplier falls back to 1.0 on a malformed value.
        register(CampaignOption.ALTERNATIVE_ADVANCED_MEDICAL_HEALING_TIME_MULTIPLIER, CampaignOptionCodec.of(
              (pw, indent, option, options) -> MHQXMLUtility.writeSimpleXMLTag(pw, indent, option.xmlTag(),
                    (double) options.get(option)),
              (node, text, version, option, options) -> options.set(option, parseDouble(text, 1.0))));
    }

    private static void readPlanetRatingBonuses(final String text, final EnumMap<PlanetaryRating, Integer> bonuses,
          final String tagName) {
        final String[] values = text.split(",");
        if (values.length == 6) {
            bonuses.put(PlanetaryRating.A, parseInt(values[0]));
            bonuses.put(PlanetaryRating.B, parseInt(values[1]));
            bonuses.put(PlanetaryRating.C, parseInt(values[2]));
            bonuses.put(PlanetaryRating.D, parseInt(values[3]));
            bonuses.put(PlanetaryRating.F, parseInt(values[5]));
        } else if (values.length == PlanetaryRating.values().length) {
            for (int i = 0; i < values.length; i++) {
                bonuses.put(PlanetaryRating.fromIndex(i), parseInt(values[i]));
            }
        } else {
            LOGGER.error("Invalid number of values for {}: {}", tagName, values.length);
        }
    }

    /** @return the codec for {@code option}: its explicit codec, or the default codec for its scalar type. */
    static CampaignOptionCodec<?> codecFor(final CampaignOption<?> option) {
        final CampaignOptionCodec<?> codec = CODECS.get(option);
        if (codec != null) {
            return codec;
        }
        final Class<?> type = option.type();
        if (type == Boolean.class) {
            return BOOL;
        }
        if (type == Integer.class) {
            return INT;
        }
        if (type == Double.class) {
            return DOUBLE;
        }
        if (type == String.class) {
            return STRING;
        }
        throw new IllegalStateException(
              "No serialization codec registered for CampaignOption '" + option.xmlTag() + "' of type " + type);
    }

    /** @return the option owning the given XML tag (canonical or legacy alias), or {@code null} if none. */
    static CampaignOption<?> optionForTag(final String tag) {
        return TAG_TO_OPTION.get(tag);
    }

    /** Writes every option that is emitted on save, in {@link CampaignOption#values()} order. */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static void writeAll(final CampaignOptions options, final PrintWriter pw, final int indent) {
        for (final CampaignOption<?> option : CampaignOption.values()) {
            final CampaignOptionCodec codec = codecFor(option);
            if (codec.writesOutput()) {
                codec.write(pw, indent, option, options);
            }
        }
    }

    /**
     * Reads a single XML tag into {@code options}, if the tag maps to a known option.
     *
     * @return {@code true} if the tag was handled; {@code false} for tags with no option (obsolete or handled by the
     *       unmarshaller's residual legacy switch).
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static boolean readTag(final String tag, final Node node, final String text, final Version version,
          final CampaignOptions options) {
        final CampaignOption option = TAG_TO_OPTION.get(tag);
        if (option == null) {
            return false;
        }
        codecFor(option).read(node, text, version, option, options);
        return true;
    }
}
