/*
 * Copyright (c) 2019 The Megamek Team. All rights reserved.
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

package mekhq.campaign.mission;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.namespace.QName;

import org.w3c.dom.Node;

import megamek.common.OffBoardDirection;
import mekhq.MekHQ;

/**
 * Contains metadata used to describe a scenario objective
 * @author NickAragua
 */
public class ScenarioObjective {
    public static final String FORCE_SHORTCUT_ALL_PRIMARY_PLAYER_FORCES = "All Primary Player Forces";
    public static final String FORCE_SHORTCUT_ALL_ENEMY_FORCES = "All Enemy Forces";
    
    public static final String ROOT_XML_ELEMENT_NAME = "ScenarioObjective";
    private static Map<ObjectiveCriterion, String> objectiveTypeMapping;
    
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
    
    @XmlElementWrapper(name="associatedForceNames")
    @XmlElement(name="associatedForceName")
    private Set<String> associatedForceNames = new HashSet<>();
    
    @XmlElementWrapper(name="associatedUnitIDs")
    @XmlElement(name="associatedUnitID")
    private Set<String> associatedUnitIDs = new HashSet<>();
    
    @XmlElementWrapper(name="successEffects")
    @XmlElement(name="successEffect")
    private List<ObjectiveEffect> successEffects = new ArrayList<>();
    
    @XmlElementWrapper(name="failureEffects")
    @XmlElement(name="failureEffect")
    private List<ObjectiveEffect> failureEffects = new ArrayList<>();
    
    @XmlElementWrapper(name="additionalDetails")
    @XmlElement(name="additionalDetail")
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
         *  entity must be crippled, destroyed or withdrawn off the wrong edge of the map        
         */
        ForceWithdraw,
        /*
         *  entity must be immobilized but not destroyed
         */
        Capture,
        /*
         *  entity must be prevented from reaching a particular map edge
         */
        PreventReachMapEdge,
        /*
         *  entity must be intact (can be crippled, immobilized, crew-killed)
         */
        Preserve,
        /*
         *  if an entity crossed a particular map edge without getting messed up en route
         */
        ReachMapEdge,
        /*
         *  this must be tracked manually by the player
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
        
    }
    
    /**
     * Copy constructor
     */
    public ScenarioObjective(ScenarioObjective other) {
        setObjectiveCriterion(other.getObjectiveCriterion());
        setDescription(other.getDescription());
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
    
    public void addDetail(String detail) {
        additionalDetails.add(detail);
    }
    
    public List<String> getDetails() {
        return additionalDetails;
    }
    
    public Set<String> getAssociatedForceNames() {
        return new HashSet<String>(associatedForceNames);
    }
    
    public void addUnit(String id) {
        associatedUnitIDs.add(id);
    }
    
    public void removeUnit(String id) {
        associatedUnitIDs.remove(id);
    }
    
    public Set<String> getAssociatedUnitIDs() {
        return new HashSet<String>(associatedUnitIDs);
    }
    
    public void clearAssociatedUnits() {
        associatedUnitIDs.clear();
    }

    public void addSuccessEffect(ObjectiveEffect successEffect) {
        successEffects.add(successEffect);
    }
    
    public List<ObjectiveEffect> getSuccessEffects() {
        return successEffects;
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
        String timeLimitString = 
                timeLimitType == TimeLimitType.None ? 
                    "" :
                    String.format("%s %d turns", isTimeLimitAtMost() ? " within at most" : " for at least", getTimeLimit());
        
        return timeLimitString;
    }
    
    /**
     * Generates a "short" string that describes the objective in a manner suitable for display in
     * the objective resolution screen.
     */
    public String toShortString() {
        String timeLimitString = getTimeLimitString();
        String edgeString = ((getDestinationEdge() != OffBoardDirection.NONE) &&
                (getDestinationEdge() != null)) ? getDestinationEdge().toString() : "";
        String amountString = fixedAmount != null ? fixedAmount.toString() : String.format("%d%%", percentage);
        
        switch(getObjectiveCriterion()) {
        case Destroy:
        case ForceWithdraw:
        case Capture:
        case Preserve:
            return String.format("%s %s%s", getObjectiveCriterion().toString(), amountString, timeLimitString);
        case ReachMapEdge:
            return String.format("Reach %s edge with %s%s", edgeString, amountString, timeLimitString);
        case PreventReachMapEdge:
            return String.format("Prevent %s from reaching %s%s", amountString, edgeString, timeLimitString);
        case Custom:
            return String.format("%s%s%s", getDescription(), amountString, timeLimitString);
        default:
                return "?";
        }
    }
    
    public int getAmount() {
        if(fixedAmount != null) {
            return fixedAmount;
        } else {
            return percentage;
        }
    }
    
    public ObjectiveAmountType getAmountType() {
        if(fixedAmount != null) {
            return ObjectiveAmountType.Fixed;
        } else {
            return ObjectiveAmountType.Percentage;
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(description);
        sb.append("\nObjective Type: ");
        sb.append(objectiveCriterion.toString());
        
        if(objectiveCriterion == ObjectiveCriterion.ReachMapEdge || 
                objectiveCriterion == ObjectiveCriterion.PreventReachMapEdge) {
            sb.append("\n");
            
            if(destinationEdge != OffBoardDirection.NONE) {
                sb.append(destinationEdge.toString());
            } else {
                sb.append("opposite deployment");
            }
            sb.append(" edge");
        }
        
        if(fixedAmount != null) {
            sb.append(fixedAmount);
        } else {
            sb.append(percentage);
            sb.append("% ");
        }
        
        if(associatedForceNames.size() > 0) {
            sb.append("\nForces:");        
            for(String forceName : associatedForceNames) {
                sb.append("\n");
                sb.append(forceName);
            }
        }
        
        if(associatedUnitIDs.size() > 0) {
            for(String unitID : associatedUnitIDs) {
                sb.append("\n");
                sb.append(unitID.toString());
            }
        }
        
        if(successEffects.size() > 0) {
            for(ObjectiveEffect effect : successEffects) {
                sb.append("\n");
                sb.append(effect.toString());
            }
        }
        
        if(failureEffects.size() > 0) {
            for(ObjectiveEffect effect : failureEffects) {
                sb.append("\n");
                sb.append(effect.toString());
            }
        }
        
        return sb.toString();
    }

    /**
     * Serialize this instance of a ScenarioObjective to a PrintWriter
     * Omits initial xml declaration
     * @param pw The destination print writer
     */
    public void Serialize(PrintWriter pw) {
        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioObjective.class);
            JAXBElement<ScenarioObjective> templateElement = new JAXBElement<>(new QName(ROOT_XML_ELEMENT_NAME), ScenarioObjective.class, this);
            Marshaller m = context.createMarshaller();
            m.setProperty("jaxb.fragment", true);
            m.marshal(templateElement, pw);
        } catch(Exception e) {
            MekHQ.getLogger().error(ScenarioTemplate.class, "Serialize", e.getMessage());
        }
    }
    
    /**
     * Attempt to deserialize an instance of a ScenarioObjective from the passed-in XML Node
     * @param xmlNode The node with the scenario template
     * @return Possibly an instance of a ScenarioTemplate
     */
    public static ScenarioObjective Deserialize(Node xmlNode) {
        ScenarioObjective resultingObjective = null;
        
        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioObjective.class);
            Unmarshaller um = context.createUnmarshaller();
            JAXBElement<ScenarioObjective> templateElement = um.unmarshal(xmlNode, ScenarioObjective.class);
            resultingObjective = templateElement.getValue();
        } catch(Exception e) {
            MekHQ.getLogger().error(ScenarioTemplate.class, "Deserialize", "Error Deserializing Scenario Objective", e);
        }
        
        return resultingObjective;
    }
}