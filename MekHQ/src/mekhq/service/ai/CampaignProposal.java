/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 */
package mekhq.service.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a campaign proposal from the AI.
 */
public class CampaignProposal {
    @JsonProperty("campaignName") public String campaignName;
    @JsonProperty("mercenaryUnitName") public String mercenaryUnitName;
    @JsonProperty("startYear") public int startYear;
    @JsonProperty("startingFactionCode") public String startingFactionCode;
    @JsonProperty("startingPlanetName") public String startingPlanetName;
    @JsonProperty("backgroundStory") public String backgroundStory;
    @JsonProperty("startingFunds") public long startingFunds;
    @JsonProperty("startingUnits") public List<UnitProposal> startingUnits;
    @JsonProperty("initialContract") public InitialContract initialContract;

    public static class UnitProposal {
        @JsonProperty("modelName") public String modelName;
        @JsonProperty("pilotName") public String pilotName;
        @JsonProperty("pilotSkills") public String pilotSkills; // e.g. "3/4"
        @JsonProperty("backstory") public String backstory;
    }

    public static class InitialContract {
        @JsonProperty("employerCode") public String employerCode;
        @JsonProperty("enemyCode") public String enemyCode;
        @JsonProperty("missionType") public String missionType;
        @JsonProperty("difficulty") public int difficulty;
        @JsonProperty("lengthMonths") public int lengthMonths;
    }
}
