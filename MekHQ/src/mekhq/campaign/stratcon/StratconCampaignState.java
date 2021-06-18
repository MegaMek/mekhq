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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.stratcon;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.namespace.QName;

import org.w3c.dom.Node;

import mekhq.MekHQ;
import mekhq.campaign.mission.AtBContract;

/**
 * Contract-level state object for a StratCon campaign.
 * @author NickAragua
 */
@XmlRootElement(name = "StratconCampaignState")
public class StratconCampaignState {
    public static final String ROOT_XML_ELEMENT_NAME = "StratconCampaignState";
    
    @XmlTransient
    private AtBContract contract;

    // these are all state variables that affect the current Stratcon Campaign
    private double globalOpforBVMultiplier;
    private int supportPoints;
    private int victoryPoints;
    private String briefingText;
    
    // these are applied to any scenario generated in the campaign; use sparingly
    private List<String> globalScenarioModifiers = new ArrayList<>(); 
    
    @XmlElementWrapper(name = "campaignTracks")
    @XmlElement(name = "campaignTrack")
    private List<StratconTrackState> tracks;

    @XmlTransient
    public AtBContract getContract() {
        return contract;
    }

    public void setContract(AtBContract contract) {
        this.contract = contract;
    }

    public StratconCampaignState() {
        tracks = new ArrayList<>();
    }
    
    public StratconCampaignState(AtBContract contract) {
        tracks = new ArrayList<>();
        setContract(contract);
    }

    /**
     * The opfor BV multiplier. Intended to be additive.
     * @return The additive opfor BV multiplier.
     */
    public double getGlobalOpforBVMultiplier() {
        return globalOpforBVMultiplier;
    }
    
    public StratconTrackState getTrack(int index) {
        return tracks.get(index);
    }
    
    public List<StratconTrackState> getTracks() {
        return tracks;
    }
    
    public void addTrack(StratconTrackState track) {
        tracks.add(track);
    }
    
    public int getSupportPoints() {
        return supportPoints;
    }
    
    public void addSupportPoints(int number) {
        supportPoints += number;
    }
    
    public void setSupportPoints(int supportPoints) {
        this.supportPoints = supportPoints;
    }

    public int getVictoryPoints() {
        return victoryPoints;
    }

    public void setVictoryPoints(int victoryPoints) {
        this.victoryPoints = victoryPoints;
    }
    
    public void updateVictoryPoints(int increment) {
        victoryPoints += increment;
    }

    public String getBriefingText() {
        return briefingText;
    }

    public void setBriefingText(String briefingText) {
        this.briefingText = briefingText;
    }

    public List<String> getGlobalScenarioModifiers() {
        return globalScenarioModifiers;
    }

    public void setGlobalScenarioModifiers(List<String> globalScenarioModifiers) {
        this.globalScenarioModifiers = globalScenarioModifiers;
    }

    public void useSupportPoint() {
        supportPoints--;
    }
    
    public void convertVictoryToSupportPoint() {
        victoryPoints--;
        supportPoints++;
    }
    
    /**
     * Convenience/speed method of determining whether or not a force with the given ID has been deployed to a track in this campaign.
     * @param forceID the force ID to check
     * @return Deployed or not.
     */
    public boolean isForceDeployedHere(int forceID) {
        for (StratconTrackState trackState : tracks) {
            if (trackState.getAssignedForceCoords().containsKey(forceID)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Serialize this instance of a campaign state to a PrintWriter
     * Omits initial xml declaration
     * @param pw The destination print writer
     */
    public void Serialize(PrintWriter pw) {
        try {
            JAXBContext context = JAXBContext.newInstance(StratconCampaignState.class);
            JAXBElement<StratconCampaignState> stateElement = new JAXBElement<>(new QName(ROOT_XML_ELEMENT_NAME), StratconCampaignState.class, this);
            Marshaller m = context.createMarshaller();
            m.setProperty(Marshaller.JAXB_FRAGMENT, true);
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(stateElement, pw);
        } catch (Exception e) {
            MekHQ.getLogger().error(e);
        }
    }
    
    /**
     * Attempt to deserialize an instance of a Campaign State from the passed-in XML Node
     * @param xmlNode The node with the campaign state
     * @return Possibly an instance of a StratconCampaignState
     */
    public static StratconCampaignState Deserialize(Node xmlNode) {        
        StratconCampaignState resultingCampaignState = null;
        
        try {
            JAXBContext context = JAXBContext.newInstance(StratconCampaignState.class);
            Unmarshaller um = context.createUnmarshaller();
            JAXBElement<StratconCampaignState> templateElement = um.unmarshal(xmlNode, StratconCampaignState.class);
            resultingCampaignState = templateElement.getValue();
        } catch (Exception e) {
            MekHQ.getLogger().error("Error Deserializing Campaign State", e);
        }
        
        // Hack: LocalDate doesn't serialize/deserialize nicely within a map, so we store it as a int-string map instead
        // while we're here, manually restore the coordinate-force lookup
        if (resultingCampaignState != null) {
            for (StratconTrackState track : resultingCampaignState.getTracks()) {
                track.restoreReturnDates();
                track.restoreAssignedCoordForces();
            }
        }
        
        return resultingCampaignState;
    }
}