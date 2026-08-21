/*
 * Copyright (C) 2018-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.scenarios;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import megamek.common.OffBoardDirection;
import megamek.logging.MMLogger;
import mekhq.campaign.mission.scenarios.ObjectiveEffect.EffectScalingType;
import org.w3c.dom.Node;

/**
 * Contains metadata used to describe a scenario objective
 *
 * @author NickAragua
 */
public class ScenarioObjective {
    private static final MMLogger LOGGER = MMLogger.create(ScenarioObjective.class);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.ScenarioObjective";

    public static final String FORCE_SHORTCUT_ALL_PRIMARY_PLAYER_FORCES = "All Primary Player Forces";
    public static final String FORCE_SHORTCUT_ALL_ENEMY_FORCES = "All Enemy Forces";

    public static final String ROOT_XML_ELEMENT_NAME = "ScenarioObjective";
    private static final Map<ObjectiveCriterion, String> objectiveTypeMapping;

    static {
        objectiveTypeMapping = new HashMap<>();
        objectiveTypeMapping.put(ObjectiveCriterion.Destroy, "Destroy");
        objectiveTypeMapping.put(ObjectiveCriterion.ForceWithdraw, "Force Withdrawal");
        objectiveTypeMapping.put(ObjectiveCriterion.Capture, "Capture");
        objectiveTypeMapping.put(ObjectiveCriterion.PreventReachMapEdge, "Prevent From Reaching");
        objectiveTypeMapping.put(ObjectiveCriterion.Preserve, "Preserve");
        objectiveTypeMapping.put(ObjectiveCriterion.ReachMapEdge, "Reach");
        objectiveTypeMapping.put(ObjectiveCriterion.Custom, "Custom");
    }

    public enum TimeLimitType {
        None,
        Fixed,
        ScaledToPrimaryUnitCount
    }

    private ObjectiveCriterion objectiveCriterion;
    private String description;
    private OffBoardDirection destinationEdge;
    private int percentage;
    private Integer fixedAmount = null;
    private TimeLimitType timeLimitType = TimeLimitType.None;
    private boolean timeLimitAtMost = true;
    private Integer timeLimit = null;
    private Integer timeLimitScaleFactor = null;

    @XmlElementWrapper(name = "associatedForceNames")
    @XmlElement(name = "associatedForceName")
    private Set<String> associatedForceNames = new HashSet<>();

    @XmlElementWrapper(name = "associatedUnitIDs")
    @XmlElement(name = "associatedUnitID")
    private Set<String> associatedUnitIDs = new HashSet<>();

    @XmlElementWrapper(name = "successEffects")
    @XmlElement(name = "successEffect")
    private List<ObjectiveEffect> successEffects = new ArrayList<>();

    @XmlElementWrapper(name = "failureEffects")
    @XmlElement(name = "failureEffect")
    private List<ObjectiveEffect> failureEffects = new ArrayList<>();

    @XmlElementWrapper(name = "additionalDetails")
    @XmlElement(name = "additionalDetail")
    private List<String> additionalDetails = new ArrayList<>();

    /**
     * Types of automatically tracked scenario objectives
     */
    public enum ObjectiveCriterion {
        /*
         * entity must be destroyed:
         * center torso/structure gone, crew killed, immobilized + battlefield control
         */
        Destroy,
        /*
         * entity must be crippled, destroyed or withdrawn off the wrong edge of the map
         */
        ForceWithdraw,
        /*
         * entity must be immobilized but not destroyed
         */
        Capture,
        /*
         * entity must be prevented from reaching a particular map edge
         */
        PreventReachMapEdge,
        /*
         * entity must be intact (can be crippled, immobilized, crew-killed)
         */
        Preserve,
        /*
         * if an entity crossed a particular map edge without getting messed up en route
         */
        ReachMapEdge,
        /*
         * this must be tracked manually by the player
         */
        Custom;

        @Override
        public String toString() {
            return objectiveTypeMapping.get(this);
        }
    }

    /**
     * Whether the objective is related to a fixed number or a percentage.
     */
    public enum ObjectiveAmountType {
        Fixed,
        Percentage
    }

    public ScenarioObjective() {
        description = "";
        objectiveCriterion = ObjectiveCriterion.Preserve;
        destinationEdge = OffBoardDirection.NONE;
        percentage = 100;
    }

    /**
     * Copy constructor
     */
    public ScenarioObjective(ScenarioObjective other) {
        setObjectiveCriterion(other.getObjectiveCriterion());
        // copy the raw authored override, not getDescription() - otherwise a generated description would be
        // materialized into the copy's stored field and become a stale override.
        this.description = other.description;
        this.setDestinationEdge(other.getDestinationEdge());
        this.setFixedAmount(other.getFixedAmount());
        this.setPercentage(other.getPercentage());
        this.setTimeLimit(other.getTimeLimit());
        this.associatedForceNames = new HashSet<>(other.associatedForceNames);
        this.associatedUnitIDs = new HashSet<>(other.associatedUnitIDs);
        this.failureEffects = new ArrayList<>(other.getFailureEffects());
        this.successEffects = new ArrayList<>(other.getSuccessEffects());
        this.additionalDetails = new ArrayList<>(other.getDetails());
        this.setTimeLimitAtMost(other.isTimeLimitAtMost());
        this.setTimeLimitType(other.getTimeLimitType());
        this.setTimeLimitScaleFactor(other.getTimeLimitScaleFactor());
    }

    public ObjectiveCriterion getObjectiveCriterion() {
        return objectiveCriterion;
    }

    public void setObjectiveCriterion(ObjectiveCriterion objectiveCriterion) {
        this.objectiveCriterion = objectiveCriterion;
    }

    /**
     * Returns the objective's human-readable description. If a description has been authored it is used as-is (an
     * optional override, and the only meaningful source for {@link ObjectiveCriterion#Custom} objectives); otherwise
     * the description is generated from the structured definition via {@link #getGeneratedDescription()}, so authors do
     * not have to hand-write text that merely restates the objective's fields.
     */
    public String getDescription() {
        if (description != null && !description.isBlank()) {
            return description;
        }
        return getGeneratedDescription();
    }

    /**
     * @return the authored description override exactly as stored (empty string if none). Editors bind to this so a
     *       blank field stays blank (auto-generated) rather than being back-filled with generated text.
     */
    public String getOverrideDescription() {
        return (description == null) ? "" : description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Builds a human-readable description of this objective purely from its structured definition - criterion, amount
     * (percentage or fixed count), destination edge, time limit, and success/failure effects.
     * {@link ObjectiveCriterion#Custom} has no structured meaning, so it falls back to the authored text.
     *
     * @return the generated description, or the authored text for {@code Custom} objectives
     */
    public String getGeneratedDescription() {
        if (objectiveCriterion == ObjectiveCriterion.Custom) {
            return getOverrideDescription();
        }

        String amount = (fixedAmount != null)
                              ? getFormattedTextAt(RESOURCE_BUNDLE, "objective.amount.units", fixedAmount)
                              : getFormattedTextAt(RESOURCE_BUNDLE, "objective.amount.percentage", percentage);
        String edge = ((destinationEdge != null) && (destinationEdge != OffBoardDirection.NONE))
                            ? getFormattedTextAt(RESOURCE_BUNDLE, "objective.edge.named",
              destinationEdge.toString().toLowerCase())
                            : getFormattedTextAt(RESOURCE_BUNDLE, "objective.edge.designated");

        String criterionKey = switch (objectiveCriterion) {
            case Destroy -> "objective.criterion.destroy";
            case ForceWithdraw -> "objective.criterion.forceWithdraw";
            case Capture -> "objective.criterion.capture";
            case Preserve -> "objective.criterion.preserve";
            case ReachMapEdge -> "objective.criterion.reachMapEdge";
            case PreventReachMapEdge -> "objective.criterion.preventReachMapEdge";
            case Custom -> ""; // handled above
        };
        String sentence = getFormattedTextAt(RESOURCE_BUNDLE, criterionKey, amount, edge);

        String timeLimit = getTimeLimitString();
        if (!timeLimit.isBlank()) {
            // getTimeLimitString() yields e.g. " within at most 5 turns"; splice it in before the trailing period
            sentence = sentence.substring(0, sentence.length() - 1) + timeLimit + ".";
        }

        String effects = describeEffects();
        return effects.isBlank() ? sentence : sentence + " " + effects;
    }

    /** Renders the success/failure effects as a compact parenthetical, e.g. {@code (+1 Scenario VP if completed)}. */
    private String describeEffects() {
        List<String> parts = new ArrayList<>();
        for (ObjectiveEffect effect : successEffects) {
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "objective.effect.ifCompleted", describeEffect(effect)));
        }
        for (ObjectiveEffect effect : failureEffects) {
            parts.add(getFormattedTextAt(RESOURCE_BUNDLE, "objective.effect.ifFailed", describeEffect(effect)));
        }
        return parts.isEmpty() ? "" : getFormattedTextAt(RESOURCE_BUNDLE, "objective.effects.clause",
              String.join("; ", parts));
    }

    private static String describeEffect(ObjectiveEffect effect) {
        return effect.effectType.isMagnitudeRelevant()
                     ? String.format(effect.effectType.toString(), effect.howMuch)
                     : effect.effectType.toString();
    }

    public void addForce(String name) {
        associatedForceNames.add(name);
    }

    public void removeForce(String name) {
        associatedForceNames.remove(name);
    }

    public void clearForces() {
        associatedForceNames.clear();
    }

    public void clearDetails() {
        additionalDetails.clear();
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public List<String> getAdditionalDetails() {
        return additionalDetails;
    }

    public void addDetail(String detail) {
        additionalDetails.add(detail);
    }

    public List<String> getDetails() {
        return additionalDetails;
    }

    public Set<String> getAssociatedForceNames() {
        return new HashSet<>(associatedForceNames);
    }

    public void addUnit(String id) {
        associatedUnitIDs.add(id);
    }

    public void removeUnit(String id) {
        associatedUnitIDs.remove(id);
    }

    public Set<String> getAssociatedUnitIDs() {
        return Collections.unmodifiableSet(associatedUnitIDs);
    }

    public void clearAssociatedUnits() {
        associatedUnitIDs.clear();
    }

    public void clearSuccessEffects() {
        successEffects.clear();
    }

    public void addSuccessEffect(ObjectiveEffect successEffect) {
        successEffects.add(successEffect);
    }

    public List<ObjectiveEffect> getSuccessEffects() {
        return successEffects;
    }

    public void clearFailureEffects() {
        failureEffects.clear();
    }

    public void addFailureEffect(ObjectiveEffect failureEffect) {
        failureEffects.add(failureEffect);
    }

    public List<ObjectiveEffect> getFailureEffects() {
        return failureEffects;
    }

    public OffBoardDirection getDestinationEdge() {
        return destinationEdge;
    }

    public void setDestinationEdge(OffBoardDirection destinationEdge) {
        this.destinationEdge = destinationEdge;
    }

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public Integer getFixedAmount() {
        return fixedAmount;
    }

    public void setFixedAmount(Integer fixedAmount) {
        this.fixedAmount = fixedAmount;
    }

    public Integer getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(Integer timeLimit) {
        this.timeLimit = timeLimit;
    }

    public Integer getTimeLimitScaleFactor() {
        return timeLimitScaleFactor;
    }

    public void setTimeLimitScaleFactor(Integer timeLimitScaleFactor) {
        this.timeLimitScaleFactor = timeLimitScaleFactor;
    }

    public boolean isTimeLimitAtMost() {
        return timeLimitAtMost;
    }

    public void setTimeLimitAtMost(boolean timeLimitAtMost) {
        this.timeLimitAtMost = timeLimitAtMost;
    }

    public TimeLimitType getTimeLimitType() {
        return timeLimitType;
    }

    public void setTimeLimitType(TimeLimitType timeLimitType) {
        this.timeLimitType = timeLimitType;
    }

    public String getTimeLimitString() {
        if (timeLimitType == TimeLimitType.None) {
            return "";
        }
        return getFormattedTextAt(RESOURCE_BUNDLE,
              isTimeLimitAtMost() ? "objective.timeLimit.atMost" : "objective.timeLimit.atLeast", getTimeLimit());
    }

    /**
     * Generates a "short" string that describes the objective in a manner suitable for display in the objective
     * resolution screen.
     */
    public String toShortString() {
        String timeLimitString = getTimeLimitString();
        String edgeString = ((getDestinationEdge() != OffBoardDirection.NONE) &&
                                   (getDestinationEdge() != null)) ? getDestinationEdge().toString() : "";
        String amountString = fixedAmount != null ? fixedAmount.toString() : String.format("%d%%", percentage);

        return switch (getObjectiveCriterion()) {
            case Destroy, ForceWithdraw, Capture, Preserve ->
                  String.format("<html>%s %s%s<span color='black'>%s%s</span></html>",
                        getObjectiveCriterion().toString(), amountString,
                        timeLimitString, buildEffects(true), buildEffects(false));
            case ReachMapEdge ->
                  String.format("<html>Reach %s edge with %s%s<span color='black'>%s%s</span></html>", edgeString,
                        amountString,
                        timeLimitString, buildEffects(true), buildEffects(false));
            case PreventReachMapEdge ->
                  String.format("<html>Prevent %s from reaching %s%s<span color='black'>%s%s</span></html>",
                        amountString, edgeString,
                        timeLimitString, buildEffects(true), buildEffects(false));
            case Custom -> String.format("<html>%s%s%s<span color='black'>%s%s</span></html>", getDescription(),
                  amountString,
                  timeLimitString, buildEffects(true), buildEffects(false));
        };
    }

    private String buildEffects(boolean success) {
        StringBuilder result = new StringBuilder();
        List<ObjectiveEffect> effectCollection = success ? getSuccessEffects() : getFailureEffects();

        if (!effectCollection.isEmpty()) {
            result.append("<br/>");
        }

        for (ObjectiveEffect effect : effectCollection) {
            boolean scaledEffect = (effect.effectScaling == EffectScalingType.Linear) ||
                                         (effect.effectScaling == EffectScalingType.Inverted);

            String effectTypeText;

            if (effect.effectType.isMagnitudeRelevant()) {
                effectTypeText = String.format(effect.effectType.toString(), effect.howMuch);
            } else {
                effectTypeText = effect.effectType.toString();
            }

            result.append(effectTypeText);

            if (scaledEffect) {
                result.append(" per unit");

                if (effect.effectScaling == EffectScalingType.Linear) {
                    result.append(" qualifying for this objective");
                } else if (effect.effectScaling == EffectScalingType.Inverted) {
                    result.append(" not qualifying for this objective");
                }
            } else {
                result.append(" if this objective is ");

                if (success) {
                    result.append("completed");
                } else {
                    result.append("failed");
                }
            }

            result.append("<br/>");
        }

        int breakIndex = result.lastIndexOf("<br/>");

        if (breakIndex >= 0) {
            result.replace(breakIndex, result.length(), "");
        }

        return result.toString();
    }

    public int getAmount() {
        return Objects.requireNonNullElseGet(fixedAmount, () -> percentage);
    }

    public ObjectiveAmountType getAmountType() {
        if (fixedAmount != null) {
            return ObjectiveAmountType.Fixed;
        } else {
            return ObjectiveAmountType.Percentage;
        }
    }

    /**
     * Whether this objective is applicable to a force template. This is the case if the objective's associated force
     * names contain either the force template's name or any of the force template's linked force names.
     */
    public boolean isApplicableToForceTemplate(ScenarioForceTemplate forceTemplate, AtBDynamicScenario scenario) {
        // no template = not applicable
        if (forceTemplate == null) {
            return false;
        }

        // if the template force is listed in this objective, we're good
        if (getAssociatedForceNames().contains(forceTemplate.getForceName())) {
            return true;
        }

        // if the template force is linked to a force listed in this objective, we're
        // good.
        if (forceTemplate.getObjectiveLinkedForces() != null) {
            for (String linkedForceName : forceTemplate.getObjectiveLinkedForces()) {
                boolean objectiveContainsLinkedForce = getAssociatedForceNames().contains(linkedForceName);
                if (objectiveContainsLinkedForce) {
                    ScenarioForceTemplate linkedForceTemplate = scenario.getTemplate().getScenarioForces()
                                                                      .get(linkedForceName);

                    try {
                        return linkedForceTemplate.getForceAlignment() == forceTemplate.getForceAlignment();
                    } catch (Exception e) {
                        // We don't want this to silently fail, as it means there is something
                        // critically wrong with the forceTemplate
                        LOGGER.error("Failed to load {}.", forceTemplate.getForceName());
                    }
                }
            }
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(description);
        sb.append("\nObjective Type: ");
        sb.append(objectiveCriterion.toString());

        if (objectiveCriterion == ObjectiveCriterion.ReachMapEdge ||
                  objectiveCriterion == ObjectiveCriterion.PreventReachMapEdge) {
            sb.append('\n');

            if ((destinationEdge != null) &&
                      (destinationEdge != OffBoardDirection.NONE)) {
                sb.append(destinationEdge);
            } else {
                sb.append("opposite deployment");
            }
            sb.append(" edge");
        }

        if (fixedAmount != null) {
            sb.append(fixedAmount);
        } else {
            sb.append(percentage);
            sb.append("% ");
        }

        if (!associatedForceNames.isEmpty()) {
            sb.append("\nForces:");
            for (String forceName : associatedForceNames) {
                sb.append('\n');
                sb.append(forceName);
            }
        }

        if (!associatedUnitIDs.isEmpty()) {
            for (String unitID : associatedUnitIDs) {
                sb.append('\n');
                sb.append(unitID);
            }
        }

        if (!successEffects.isEmpty()) {
            for (ObjectiveEffect effect : successEffects) {
                sb.append('\n');
                sb.append(effect.toString());
            }
        }

        if (!failureEffects.isEmpty()) {
            for (ObjectiveEffect effect : failureEffects) {
                sb.append('\n');
                sb.append(effect.toString());
            }
        }

        return sb.toString();
    }

    /**
     * Serialize this instance of a ScenarioObjective to a PrintWriter Omits initial xml declaration
     *
     * @param pw The destination print writer
     */
    public void Serialize(PrintWriter pw) {
        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioObjective.class);
            JAXBElement<ScenarioObjective> objectiveElement = new JAXBElement<>(new QName(ROOT_XML_ELEMENT_NAME),
                  ScenarioObjective.class, this);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FRAGMENT, true);
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(objectiveElement, pw);
        } catch (Exception ex) {
            LOGGER.error("Error Serializing Scenario Objective", ex);
        }
    }

    /**
     * Attempt to deserialize an instance of a ScenarioObjective from the passed-in XML Node
     *
     * @param xmlNode The node with the scenario template
     *
     * @return Possibly an instance of a ScenarioTemplate
     */
    public static ScenarioObjective Deserialize(Node xmlNode) {
        ScenarioObjective resultingObjective = null;

        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioObjective.class);
            Unmarshaller um = context.createUnmarshaller();
            JAXBElement<ScenarioObjective> templateElement = um.unmarshal(xmlNode, ScenarioObjective.class);
            resultingObjective = templateElement.getValue();
        } catch (Exception ex) {
            LOGGER.error("Error Deserializing Scenario Objective", ex);
        }

        return resultingObjective;
    }
}
