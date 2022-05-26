/*
 * AtBScenario.java
 *
 * Copyright (C) 2014-2021 - The MegaMek Team. All Rights Reserved.
 * Copyright (c) 2014 Carl Spain. All rights reserved.
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
package mekhq.campaign.mission;

import megamek.Version;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import megamek.common.options.OptionsConstants;
import megamek.common.util.EncodeControl;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.againstTheBot.AtBConfiguration;
import mekhq.campaign.force.Force;
import mekhq.campaign.force.Lance;
import mekhq.campaign.market.unitMarket.AtBMonthlyUnitMarket;
import mekhq.campaign.mission.ObjectiveEffect.ObjectiveEffectType;
import mekhq.campaign.mission.ScenarioObjective.ObjectiveCriterion;
import mekhq.campaign.mission.atb.IAtBScenario;
import mekhq.campaign.mission.enums.AtBLanceRole;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.rating.IUnitRating;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.*;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.*;

/**
 * @author Neoancient
 */
public abstract class AtBScenario extends Scenario implements IAtBScenario {
    //region Variable Declarations
    public static final int DYNAMIC = -1;
    public static final int BASEATTACK = 0;
    public static final int EXTRACTION = 1;
    public static final int CHASE = 2;
    public static final int HOLDTHELINE = 3;
    public static final int BREAKTHROUGH = 4;
    public static final int HIDEANDSEEK = 5;
    public static final int STANDUP = 6;
    public static final int RECONRAID = 7;
    public static final int PROBE = 8;

    public static final int OFFICERDUEL = 9; //Special Mission
    public static final int ACEDUEL = 10; //Special Mission
    public static final int AMBUSH = 11; //Special Mission
    public static final int CIVILIANHELP = 12; //Special Mission
    public static final int ALLIEDTRAITORS = 13; //Special Mission
    public static final int PRISONBREAK = 14; //Special Mission
    public static final int STARLEAGUECACHE1 = 15; //Special Mission
    public static final int STARLEAGUECACHE2 = 16; //Special Mission

    public static final int ALLYRESCUE = 17; //Big Battle
    public static final int CIVILIANRIOT = 18; //Big Battle
    public static final int CONVOYRESCUE = 19; //Big Battle
    public static final int CONVOYATTACK = 20; //Big Battle
    public static final int PIRATEFREEFORALL = 21; //Big Battle

    public static final int FORCE_MEK = 0;
    public static final int FORCE_VEHICLE = 1;
    public static final int FORCE_MIXED = 2;
    public static final int FORCE_NOVA = 3;
    public static final int FORCE_VEENOVA = 4;
    public static final int FORCE_INFANTRY = 5;
    public static final int FORCE_BA = 6;
    public static final int FORCE_AERO = 7;
    public static final int FORCE_PROTOMEK = 8;
    public static final String[] forceTypeNames = {
        "Mek", "Vehicle", "Mixed", "Nova", "Nova", "Infantry",
        "Battle Armor", "Aerospace", "ProtoMek"
    };

    public static final String[] antiRiotWeapons = {
            "ISERSmallLaser", "Small Laser", "Small Laser Prototype",
            "ISSmallPulseLaser", "ISSmallXPulseLaser", "Small Re-engineered Laser",
            "ISSmallVSPLaser", "CLERMicroLaser", "CLERSmallLaser",
            "ER Small Laser (CP)", "CLHeavySmallLaser",
            "CLImprovedSmallHeavyLaser", "ClSmall Laser",
            "CLERSmallPulseLaser", "CLMicroPulseLaser", "CLSmallPulseLaser",
            "CLSmallChemLaser",
            "Heavy Machine Gun", "Light Machine Gun", "Machine Gun",
            "Heavy Rifle", "Light Rifle", "Medium Rifle",
            "CLHeavyMG", "CLLightMG", "CLMG",
            "Flamer", "ER Flamer", "CLFlamer", "CLERFlamer",
            "Vehicle Flamer", "Heavy Flamer", "CLVehicleFlamer", "CLHeavyFlamer"
        };

    /* The starting position chart in the AtB rules includes the four
     * corner positions as well, but this creates some conflict with
     * setting the home edge for the bot, which only includes the four
     * sides.
     */
    protected static final int [] startPos = {
        Board.START_N, Board.START_E, Board.START_S, Board.START_W
    };

    private static final int[] randomAeroWeights = {
            EntityWeightClass.WEIGHT_LIGHT, EntityWeightClass.WEIGHT_LIGHT, EntityWeightClass.WEIGHT_LIGHT,
            EntityWeightClass.WEIGHT_MEDIUM, EntityWeightClass.WEIGHT_MEDIUM,
            EntityWeightClass.WEIGHT_HEAVY
    };

    public static final int NO_LANCE = -1;

    private boolean attacker;
    private int lanceForceId; // -1 if scenario is not generated for a specific lance (special mission, big battle)
    private AtBLanceRole lanceRole; /* set when scenario is created in case it is changed for the next week before the scenario is resolved;
                            specifically affects scenarios generated for scout lances, in which the deployment may be delayed
                            for slower units */

    private int deploymentDelay;
    private int lanceCount;
    private int rerollsRemaining;
    private int enemyHome;

    private List<Entity> alliesPlayer;
    private List<String> alliesPlayerStub;

    /* Special missions cannot generate the enemy until the unit is
     * added, but needs the Campaign object which is not passed
     * by addForce or addUnit. Instead we generate all possibilities
     * (one for each weight class) when the scenario is created and
     * choose the correct one when a unit is deployed.
     */
    private List<List<Entity>> specMissionEnemies;

    /* Big battles have a similar problem for attached allies. Though
     * we could generate the maximum number (4) and remove them as
     * the player deploys additional units, they would be lost if
     * any units are undeployed.
     */
    private List<Entity> bigBattleAllies;

    /* Units that need to be tracked for possible contract breaches
     * (for destruction), or bonus rolls (for survival).
     */
    private List<UUID> attachedUnitIds;
    private List<UUID> survivalBonus;

    private Map<UUID, Entity> entityIds;

    // key-value pairs linking transports and the units loaded onto them.
    private Map<String, List<String>> transportLinkages;

    private Map<Integer, Integer> numPlayerMinefields;

    protected final transient ResourceBundle defaultResourceBundle = ResourceBundle.getBundle("mekhq.resources.AtBScenarioBuiltIn",
            MekHQ.getMHQOptions().getLocale(), new EncodeControl());
    //endregion Variable Declarations

    public AtBScenario () {
        super();
        lanceForceId = -1;
        lanceRole = AtBLanceRole.UNASSIGNED;
        alliesPlayer = new ArrayList<>();
        alliesPlayerStub = new ArrayList<>();
        attachedUnitIds = new ArrayList<>();
        survivalBonus = new ArrayList<>();
        entityIds = new HashMap<>();
        transportLinkages = new HashMap<>();
        numPlayerMinefields = new HashMap<>();

        deploymentDelay = 0;
        lanceCount = 0;
        rerollsRemaining = 0;
    }

    public void initialize(Campaign c, Lance lance, boolean attacker, LocalDate date) {
        setAttacker(attacker);

        alliesPlayer = new ArrayList<>();
        botForces = new ArrayList<>();
        alliesPlayerStub = new ArrayList<>();
        botForcesStubs = new ArrayList<>();
        attachedUnitIds = new ArrayList<>();
        survivalBonus = new ArrayList<>();
        entityIds = new HashMap<>();

        if (null == lance) {
            lanceForceId = -1;
            lanceRole = AtBLanceRole.UNASSIGNED;
        } else {
            this.lanceForceId = lance.getForceId();
            lanceRole = lance.getRole();
            setMissionId(lance.getMissionId());

            for (UUID id : c.getForce(lance.getForceId()).getAllUnits(true)) {
                entityIds.put(id, c.getUnit(id).getEntity());
            }
        }

        light = PlanetaryConditions.L_DAY;
        weather = PlanetaryConditions.WE_NONE;
        wind = PlanetaryConditions.WI_NONE;
        fog = PlanetaryConditions.FOG_NONE;
        atmosphere = PlanetaryConditions.ATMO_STANDARD;
        gravity = (float) 1.0;
        deploymentDelay = 0;
        setDate(date);
        lanceCount = 0;
        rerollsRemaining = 0;

        if (isStandardMission()) {
            setName(getScenarioTypeDescription() + (isAttacker() ? " (Attacker)" : " (Defender)"));
        } else {
            setName(getScenarioTypeDescription());
        }

        initBattle(c);
    }

    public String getDesc() {
        return getScenarioTypeDescription() + (isStandardMission() ? (isAttacker() ? " (Attacker)" : " (Defender)") : "");
    }

    @Override
    public boolean isStandardMission() {
        return !isSpecialMission() && !isBigBattle();
    }

    @Override
    public boolean isSpecialMission() {
        return false;
    }

    @Override
    public boolean isBigBattle() {
        return false;
    }

    @Override
    public ResourceBundle getResourceBundle() {
        return defaultResourceBundle;
    }

    /**
     * Determines battle conditions: terrain, weather, map.
     *
     * @param campaign
     */
    private void initBattle(Campaign campaign) {
        setTerrain();
        if (campaign.getCampaignOptions().getUseLightConditions()) {
            setLightConditions();
        }
        if (campaign.getCampaignOptions().getUseWeatherConditions()) {
            setWeather();
        }
        if (campaign.getCampaignOptions().getUsePlanetaryConditions() &&
                null != campaign.getMission(getMissionId())) {
            setPlanetaryConditions(campaign.getMission(getMissionId()), campaign);
        }
        setMapSize();
        setMapFile();
        if (isStandardMission()) {
            lanceCount = 1;
        } else if (isBigBattle()) {
            lanceCount = 2;
        }

        if (null != getLance(campaign)) {
            getLance(campaign).refreshCommander(campaign);
            if (null != getLance(campaign).getCommander(campaign).getSkill(SkillType.S_TACTICS)) {
                rerollsRemaining = getLance(campaign).getCommander(campaign).getSkill(SkillType.S_TACTICS).getLevel();
            }
        }
    }

    public void setTerrain() {
        setTerrainType(terrainChart[Compute.d6(2) - 2]);
    }

    public void setLightConditions() {
        setLight(PlanetaryConditions.L_DAY);

        int roll = Compute.randomInt(10) + 1;
        if (roll < 6) {
            setLight(PlanetaryConditions.L_DAY);
        } else if (roll < 8) {
            setLight(PlanetaryConditions.L_DUSK);
        } else if (roll == 8) {
            setLight(PlanetaryConditions.L_FULL_MOON);
        } else if (roll == 9) {
            setLight(PlanetaryConditions.L_MOONLESS);
        } else {
            setLight(PlanetaryConditions.L_PITCH_BLACK);
        }
    }

    public void setWeather() {
        setWeather(PlanetaryConditions.WE_NONE);
        setWind(PlanetaryConditions.WI_NONE);
        setFog(PlanetaryConditions.FOG_NONE);

        int roll = Compute.randomInt(10) + 1;
        int r2 = Compute.d6();
        if (roll == 6) {
            if (r2 < 4) {
                setWeather(PlanetaryConditions.WE_LIGHT_RAIN);
            } else if (r2 < 6) {
                setWeather(PlanetaryConditions.WE_MOD_RAIN);
            } else {
                setWeather(PlanetaryConditions.WE_HEAVY_RAIN);
            }
        } else if (roll == 7) {
            if (r2 < 4) {
                setWeather(PlanetaryConditions.WE_LIGHT_SNOW);
            } else if (r2 < 6) {
                setWeather(PlanetaryConditions.WE_MOD_SNOW);
            } else {
                setWeather(PlanetaryConditions.WE_HEAVY_SNOW);
            }
        } else if (roll == 8) {
            if (r2 < 4) {
                setWind(PlanetaryConditions.WI_LIGHT_GALE);
            } else if (r2 < 6) {
                setWind(PlanetaryConditions.WI_MOD_GALE);
            } else {
                setWind(PlanetaryConditions.WI_STRONG_GALE);
            }
        } else if (roll == 9) {
            if (r2 == 1) {
                setWind(PlanetaryConditions.WI_STORM);
            } else if (r2 == 2) {
                setWeather(PlanetaryConditions.WE_DOWNPOUR);
            } else if (r2 == 3) {
                setWeather(PlanetaryConditions.WE_SLEET);
            } else if (r2 == 4) {
                setWeather(PlanetaryConditions.WE_ICE_STORM);
            } else if (r2 == 5) {
                // tornadoes are classified as wind rather than weather.
                setWind(PlanetaryConditions.WI_TORNADO_F13);
            } else if (r2 == 6) {
                setWind(PlanetaryConditions.WI_TORNADO_F4);
            }
        } else if (roll > 9) {
            if (r2 < 5) {
                setFog(PlanetaryConditions.FOG_LIGHT);
            } else {
                setFog(PlanetaryConditions.FOG_HEAVY);
            }
        }
        // roll < 6 can be ignored, as it would just return nothing
    }

    public void setPlanetaryConditions(Mission mission, Campaign campaign) {
        if (null != mission) {
            PlanetarySystem psystem = Systems.getInstance().getSystemById(mission.getSystemId());
            //assume primary planet for now
            Planet p = psystem.getPrimaryPlanet();
            if (null != p) {
                setAtmosphere(ObjectUtility.nonNull(p.getPressure(campaign.getLocalDate()), getAtmosphere()));
                setGravity(ObjectUtility.nonNull(p.getGravity(), getGravity()).floatValue());
            }
        }
    }

    public void setMapSize() {
        int roll = Compute.randomInt(20) + 1;
        if (roll < 6) {
            setMapSizeX(20);
            setMapSizeY(10);
        } else if (roll < 11) {
            setMapSizeX(10);
            setMapSizeY(20);
        } else if (roll < 13) {
            setMapSizeX(30);
            setMapSizeY(10);
        } else if (roll < 15) {
            setMapSizeX(10);
            setMapSizeY(30);
        } else if (roll < 19) {
            setMapSizeX(20);
            setMapSizeY(20);
        } else if (roll == 19) {
            setMapSizeX(40);
            setMapSizeY(10);
        } else {
            setMapSizeX(10);
            setMapSizeY(40);
        }
    }

    public int getMapX() {
        int base = getMapSizeX() + 5 * lanceCount;

        return Math.max(base, 20);
    }

    public int getMapY() {
        int base = getMapSizeY() + 5 * lanceCount;

        return Math.max(base, 20);
    }

    public int getBaseMapX() {
        return (5 * getLanceCount()) + getMapSizeX();
    }

    public int getBaseMapY() {
        return (5 * getLanceCount()) + getMapSizeY();
    }

    public void setMapFile() {
        final String[][] maps = {
            {"Sandy-hills", "Hills-craters", "Hills",
                "Wooded-hills", "Cliffs", "Town-hills"}, //hills
            {"Sandy-valley", "Rocky-valley", "Light-craters",
                "Heavy-craters", "Rubble-mountain", "Cliffs"}, //badlands
            {"Muddy-swamp", "Lake-marsh", "Lake-high",
                "Wooded-lake", "Swamp", "Wooded-swamp"}, //wetlands
            {"Town-mining", "Town-wooded", "Town-generic",
                "Town-farming", "Town-ruin", "Town-mountain"}, //light urban
            {"Savannah", "Dust-bowl", "Sandy-hills",
                "Town-ruin", "Town-generic", "Some-trees"}, //flatlands
            {"Some-trees", "Wooded-lake", "Woods-medium",
                "Wooded-hills", "Woods-deep", "Wooded-valley"}, //wooded
            {"Fortress-city", "Fortress-city", "Town-concrete",
                "Town-concrete", "City-high", "City-dense"}, //heavy urban
            {"River-huge", "Woods-river", "Sandy-river",
                "Rubble-river", "Seaport", "River-wetlands"}, //coastal
            {"Mountain-lake", "Cliffs-lake", "Rubble-mountain",
                "Cliffs", "Mountain-medium", "Mountain-high"} //mountains
        };

        int actualTerrainType = getTerrainType();

        // we want to make sure the terrain type we pick is in bounds,
        if ((getTerrainType() < 0) || (getTerrainType() >= maps.length)) {
            actualTerrainType = Compute.randomInt(maps.length);
        }

        setMap(maps[actualTerrainType][Compute.d6() - 1]);
    }

    public boolean canRerollTerrain() {
        return canRerollMap();
    }

    public boolean canRerollMapSize() {
        return true;
    }

    public boolean canRerollMap() {
        return true;
    }

    public boolean canRerollLight() {
        return true;
    }

    public boolean canRerollWeather() {
        return true;
    }

    /**
     * Determines whether a unit is eligible to deploy to the scenario. The
     * default is true, but some special missions and big battles restrict
     * the participants.
     *
     * @param unit
     * @param campaign
     * @return true if the unit is eligible, otherwise false
     */
    @Override
    public boolean canDeploy(Unit unit, Campaign campaign) {
        if (isBigBattle() && (getForces(campaign).getAllUnits(true).size() > 7)) {
            return false;
        } else {
            return !isSpecialMission() || (getForces(campaign).getAllUnits(true).size() <= 0);
        }
    }

    /**
     * Determines whether a force is eligible to deploy to a scenario by
     * checking all units contained in the force
     *
     * @param force
     * @param campaign
     * @return true if the force is eligible to deploy, otherwise false
     */
    public boolean canDeploy(Force force, Campaign campaign) {
        Vector<UUID> units = force.getAllUnits(true);
        if (isBigBattle() &&
                getForces(campaign).getAllUnits(true).size() + units.size() > 8) {
            return false;
        } else if (isSpecialMission() &&
                getForces(campaign).getAllUnits(true).size() + units.size() > 0) {
            return false;
        }
        for (UUID id : units) {
            if (!canDeploy(campaign.getUnit(id), campaign)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether a list of units is eligible to deploy to the scenario.
     *
     * @param units
     * @param campaign
     * @return true if all units in the list are eligible, otherwise false
     */
    @Override
    public boolean canDeployUnits(Vector<Unit> units, Campaign campaign) {
        if (isBigBattle()) {
            return getForces(campaign).getAllUnits(true).size() + units.size() <= 8;
        } else if (isSpecialMission()) {
            return getForces(campaign).getAllUnits(true).size() + units.size() <= 1;
        }
        for (Unit unit : units) {
            if (!canDeploy(unit, campaign)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determines whether a list of forces is eligible to deploy to the scenario.
     *
     * @param forces    list of forces
     * @param c         the campaign that the forces are part of
     * @return true if all units in all forces in the list are eligible, otherwise false
     */
    @Override
    public boolean canDeployForces(Vector<Force> forces, Campaign c) {
        int total = 0;
        for (Force force : forces) {
            Vector<UUID> units = force.getAllUnits(true);
            total += units.size();
            if (isBigBattle()) {
                return getForces(c).getAllUnits(true).size() + units.size() <= 8;
            } else if (isSpecialMission()) {
                return getForces(c).getAllUnits(true).size() + units.size() <= 0;
            }
            for (UUID id : units) {
                if (!canDeploy(c.getUnit(id), c)) {
                    return false;
                }
            }
        }
        if (isBigBattle()) {
            return getForces(c).getAllUnits(true).size() + total <= 8;
        } else if (isSpecialMission()) {
            return getForces(c).getAllUnits(true).size() + total <= 0;
        }
        return true;
    }

    /**
     * Corrects the enemy (special missions) and allies (big battles)
     * as necessary based on player deployments. This ought to be called
     * when the scenario details are displayed or the scenario is started.
     *
     * @param campaign
     */
    public void refresh(Campaign campaign) {
        if (isStandardMission()) {
            setObjectives(campaign, getContract(campaign));
            return;
        }
        Vector<UUID> deployed = getForces(campaign).getAllUnits(true);
        if (isBigBattle()) {
            int numAllies = Math.min(4, 8 - deployed.size());
            alliesPlayer.clear();
            for (int i = 0; i < numAllies; i++) {
                alliesPlayer.add(bigBattleAllies.get(i));
                getExternalIDLookup().put(bigBattleAllies.get(i).getExternalIdAsString(), bigBattleAllies.get(i));
            }

            setObjectives(campaign, getContract(campaign));
        } else {
            if (deployed.isEmpty()) {
                return;
            }
            int weight = campaign.getUnit(deployed.get(0)).getEntity().getWeightClass() - 1;
            /* In the event that Star League Cache 1 generates a primitive 'Mech,
             * the player can keep the 'Mech without a battle so no enemy
             * units are generated.
             */

            if (specMissionEnemies == null ) {
                setForces(campaign);
            }

            if ((specMissionEnemies != null) && (getBotForces().get(0) != null)
                    && (specMissionEnemies.get(weight) != null)) {
                getBotForces().get(0).setFixedEntityList(specMissionEnemies.get(weight));
            }
            setObjectives(campaign, getContract(campaign));
        }
    }

    /**
     * Determines enemy and allied forces for the scenario. The forces for a standard
     * battle are based on the player's deployed lance. The enemy forces for
     * special missions depend on the weight class of the player's deployed
     * unit and the number of allies in big battles varies according to the
     * number the player deploys. Since the various possibilities are rather
     * limited, all possibilities are generated and the most appropriate is
     * chosen rather than rerolling every time the player changes. This is
     * both for efficiency and to prevent shopping.
     *
     * @param campaign
     */
    public void setForces(Campaign campaign) {
        if (isStandardMission()) {
            setStandardMissionForces(campaign);
        } else if (isSpecialMission()) {
            setSpecialMissionForces(campaign);
        } else {
            setBigBattleForces(campaign);
        }

        setObjectives(campaign, getContract(campaign));
    }

    /**
     * Generates attached allied units (bot or player controlled), the main
     * enemy force, any enemy reinforcements, and any additional forces
     * (such as civilian).
     *
     * @param campaign
     */
    private void setStandardMissionForces(Campaign campaign) {
        /* Find the number of attached units required by the command rights clause */
        int attachedUnitWeight = EntityWeightClass.WEIGHT_MEDIUM;
        if (lanceRole.isScouting() || lanceRole.isTraining()) {
            attachedUnitWeight = EntityWeightClass.WEIGHT_LIGHT;
        }
        int numAttachedPlayer = 0;
        int numAttachedBot = 0;
        if (getContract(campaign).getContractType().isCadreDuty()) {
            numAttachedPlayer = 3;
        } else if (campaign.getFactionCode().equals("MERC")) {
            switch (getContract(campaign).getCommandRights()) {
                case INTEGRATED:
                    if (campaign.getCampaignOptions().getPlayerControlsAttachedUnits()) {
                        numAttachedPlayer = 2;
                    } else {
                        numAttachedBot = 2;
                    }
                    break;
                case HOUSE:
                    if (campaign.getCampaignOptions().getPlayerControlsAttachedUnits()) {
                        numAttachedPlayer = 1;
                    } else {
                        numAttachedBot = 1;
                    }
                    break;
                case LIAISON:
                    numAttachedPlayer = 1;
                    break;
                default:
                    break;
            }
        }

        /* The entities in the attachedAllies list will be added to the player's forces
         * in MM and don't require a separate BotForce */
        final Camouflage camouflage = getContract(campaign).getAllyCamouflage();
        for (int i = 0; i < numAttachedPlayer; i++) {
            Entity en = getEntity(getContract(campaign).getEmployerCode(),
                    getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
                    UnitType.MEK, attachedUnitWeight, campaign);
            if (null != en) {
                alliesPlayer.add(en);
                attachedUnitIds.add(UUID.fromString(en.getExternalIdAsString()));
                getExternalIDLookup().put(en.getExternalIdAsString(), en);

                if (!campaign.getCampaignOptions().getAttachedPlayerCamouflage()) {
                    en.setCamouflage(camouflage.clone());
                }
            } else {
                LogManager.getLogger().error("Entity for player-controlled allies is null");
            }
        }

        /* The allyBot list will be passed to the BotForce constructor */
        ArrayList<Entity> allyEntities = new ArrayList<>();
        for (int i = 0; i < numAttachedBot; i++) {
            Entity en = getEntity(getContract(campaign).getEmployerCode(),
                    getContract(campaign).getAllySkill(), getContract(campaign).getAllyQuality(),
                    UnitType.MEK, attachedUnitWeight, campaign);
            if (null != en) {
                allyEntities.add(en);
                attachedUnitIds.add(UUID.fromString(en.getExternalIdAsString()));
            } else {
                LogManager.getLogger().error("Entity for ally bot is null");
            }
        }

        ArrayList<Entity> enemyEntities = new ArrayList<>();

        setExtraMissionForces(campaign, allyEntities, enemyEntities);
        addAeroReinforcements(campaign);
        addScrubReinforcements(campaign);

        /* Possible enemy reinforcements */
        int roll = Compute.d6();
        if (roll > 3) {
            ArrayList<Entity> reinforcements = new ArrayList<>();
            if (roll == 6) {
                addLance(reinforcements, getContract(campaign).getEnemyCode(),
                    getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
                    EntityWeightClass.WEIGHT_MEDIUM, EntityWeightClass.WEIGHT_ASSAULT, campaign, 8);
            } else {
                addLance(reinforcements, getContract(campaign).getEnemyCode(),
                        getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
                        EntityWeightClass.WEIGHT_LIGHT, EntityWeightClass.WEIGHT_ASSAULT, campaign, 6);
            }

            /* Must set per-entity start pos for units after start of scenarios. Reinforcements
             * arrive from the enemy home edge, which is not necessarily the start pos. */
            final int enemyDir = enemyHome;
            reinforcements.stream().filter(Objects::nonNull).forEach(en -> {
                en.setStartingPos(enemyDir);
            });

            if (campaign.getCampaignOptions().getAllowOpforLocalUnits()) {
                reinforcements.addAll(AtBDynamicScenarioFactory.fillTransports(this, reinforcements,
                        getContract(campaign).getEnemyCode(),
                        getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
                        campaign));

            }

            BotForce bf = getEnemyBotForce(getContract(campaign), enemyHome, enemyHome, reinforcements);
            bf.setName(bf.getName() + " (Reinforcements)");
            addBotForce(bf, campaign);
        }

        if (campaign.getCampaignOptions().getUseDropShips()) {
            if (canAddDropShips()) {
                boolean dropshipFound = false;
                for (UUID id : campaign.getForces().getAllUnits(true)) {
                    if ((campaign.getUnit(id).getEntity().getEntityType() & Entity.ETYPE_DROPSHIP) != 0 &&
                            campaign.getUnit(id).isAvailable()) {
                        addUnit(id);
                        campaign.getUnit(id).setScenarioId(getId());
                        dropshipFound = true;
                        break;
                    }
                }
                if (!dropshipFound) {
                    addDropship(campaign);
                }
                for (int i = 0; i < Compute.d6() - 3; i++) {
                    addLance(enemyEntities, getContract(campaign).getEnemyCode(),
                            getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
                            AtBMonthlyUnitMarket.getRandomWeight(campaign, UnitType.MEK, getContract(campaign).getEnemy()),
                            EntityWeightClass.WEIGHT_ASSAULT, campaign);
                }
            } else if (getLanceRole().isScouting()) {
                /* Set allied forces to deploy in (6 - speed) turns just as player's units,
                 * but only if not deploying by DropShip.
                 */
                alliesPlayer.stream().filter(Objects::nonNull).forEach(entity -> {
                    int speed = entity.getWalkMP();
                    if (entity.getJumpMP() > 0) {
                        if (entity instanceof megamek.common.Infantry) {
                            speed = entity.getJumpMP();
                        } else {
                            speed++;
                        }
                    }
                    entity.setDeployRound(Math.max(0, 6 - speed));
                });
                allyEntities.stream().filter(Objects::nonNull).forEach(entity -> {
                    int speed = entity.getWalkMP();
                    if (entity.getJumpMP() > 0) {
                        if (entity instanceof megamek.common.Infantry) {
                            speed = entity.getJumpMP();
                        } else {
                            speed++;
                        }
                    }
                    entity.setDeployRound(Math.max(0, 6 - speed));
                });
            }
        }

        // aaand for fun, run everyone through the crew upgrader
        if (campaign.getCampaignOptions().useAbilities()) {
            AtBDynamicScenarioFactory.upgradeBotCrews(this, campaign);
        }
    }

    @Override
    public void setExtraMissionForces(Campaign campaign, ArrayList<Entity> allyEntities, ArrayList<Entity> enemyEntities) {
        int enemyStart;
        int playerHome;

        playerHome = startPos[Compute.randomInt(4)];
        setStart(playerHome);
        enemyStart = getStart() + 4;

        if (enemyStart > 8) {
            enemyStart -= 8;
        }

        enemyHome = enemyStart;

        if (!allyEntities.isEmpty()) {
            addBotForce(getAllyBotForce(getContract(campaign), getStart(), playerHome, allyEntities), campaign);
        }

        addEnemyForce(enemyEntities, getLance(campaign).getWeightClass(campaign), campaign);
        addBotForce(getEnemyBotForce(getContract(campaign), enemyHome, enemyHome, enemyEntities), campaign);
    }

    @Override
    public boolean canAddDropShips() {
        return false;
    }

    /**
     * Generate four sets of forces: one for each weight class the player
     * can choose to deploy.
     *
     * @param campaign
     */
    private void setSpecialMissionForces(Campaign campaign) {
        //enemy must always be the first on the botforce list so we can find it on refresh()
        specMissionEnemies = new ArrayList<>();

        ArrayList<Entity> enemyEntities = new ArrayList<>();
        ArrayList<Entity> allyEntities = new ArrayList<>();

        setExtraMissionForces(campaign, allyEntities, enemyEntities);
    }

    public List<List<Entity>> getSpecMissionEnemies() {
        return specMissionEnemies;
    }

    /**
     * Generates enemy forces and four allied units that may be used if the player
     * deploys fewer than eight of his or her own units.
     *
     * @param campaign
     */
    private void setBigBattleForces(Campaign campaign) {
        ArrayList<Entity> enemyEntities = new ArrayList<>();
        ArrayList<Entity> allyEntities = new ArrayList<>();

        setExtraMissionForces(campaign, allyEntities, enemyEntities);

        bigBattleAllies = new ArrayList<>();

        bigBattleAllies.addAll(alliesPlayer);
    }

    protected void addEnemyForce(List<Entity> list, int weightClass, Campaign c) {
        addEnemyForce(list, weightClass, EntityWeightClass.WEIGHT_ASSAULT, 0, 0, c);
    }

    /**
     * Generates the enemy force based on the weight class of the lance deployed
     * by the player. Certain scenario types may set a maximum weight class for
     * enemy units or modify the roll.
     *
     * @param list            All generated enemy entities are added to this list.
     * @param weightClass    The weight class of the player's lance.
     * @param maxWeight        The maximum weight class of each generated enemy entity
     * @param rollMod        Modifier to the enemy lances roll.
     * @param weightMod        Modifier to the weight class of enemy lances.
     * @param campaign
     */
    protected void addEnemyForce(List<Entity> list, int weightClass, int maxWeight, int rollMod,
                                 int weightMod, Campaign campaign) {
        String org = AtBConfiguration.getParentFactionType(getContract(campaign).getEnemyCode());

        String lances = campaign.getAtBConfig().selectBotLances(org, weightClass, rollMod / 20f);
        int maxLances = Math.min(lances.length(), campaign.getCampaignOptions().getSkillLevel() + 1);

        for (int i = 0; i < maxLances; i++) {
            addEnemyLance(list, AtBConfiguration.decodeWeightStr(lances, i) + weightMod,
                    maxWeight, campaign);
        }

        if (campaign.getCampaignOptions().getAllowOpforLocalUnits()) {
            list.addAll(AtBDynamicScenarioFactory.fillTransports(this, list,
                    getContract(campaign).getEnemyCode(), getContract(campaign).getEnemySkill(),
                    getContract(campaign).getEnemyQuality(), campaign));
        }
    }

    /**
     * Generates an enemy lance of a given weight class.
     *
     * @param list Generated enemy entities are added to this list.
     * @param weight Weight class of the enemy lance.
     * @param maxWeight Maximum weight of enemy entities.
     * @param campaign  The current campaign
     */
    private void addEnemyLance(List<Entity> list, int weight, int maxWeight, Campaign campaign) {
        if (weight < EntityWeightClass.WEIGHT_LIGHT) {
            weight = EntityWeightClass.WEIGHT_LIGHT;
        }
        if (weight > EntityWeightClass.WEIGHT_ASSAULT) {
            weight = EntityWeightClass.WEIGHT_ASSAULT;
        }
        addLance(list, getContract(campaign).getEnemyCode(),
                getContract(campaign).getEnemySkill(), getContract(campaign).getEnemyQuality(),
                weight, maxWeight, campaign);
        lanceCount++;
    }

    /**
     * Determines the most appropriate RAT and uses it to generate a random Entity
     *
     * @param faction The faction code to use for locating the correct RAT and assigning a crew name
     * @param skill The {@link SkillLevel} that represents the skill level of the overall force.
     * @param quality The equipment rating of the force.
     * @param unitType The UnitTableData constant for the type of unit to generate.
     * @param weightClass The weight class of the unit to generate
     * @param campaign The current campaign
     * @return A new Entity with crew.
     */
    protected @Nullable Entity getEntity(String faction, SkillLevel skill, int quality,
                                         int unitType, int weightClass, Campaign campaign) {
        return AtBDynamicScenarioFactory.getEntity(faction, skill, quality, unitType, weightClass, false, campaign);
    }

    /**
     * Units that exceed the maximum weight for individual entities in the scenario
     * are replaced in the lance by two lighter units.
     *
     * @param weights        A string of single-character letter codes for the weights of the units in the lance (e.g. "LMMH")
     * @param maxWeight        The maximum weight allowed for the force by the parameters of the scenario type
     * @return                A new String of the same format as weights
     */
    private String adjustForMaxWeight(String weights, int maxWeight) {
        String retVal = weights;
        if (maxWeight == EntityWeightClass.WEIGHT_HEAVY) {
            //Hide and Seek (defender)
            retVal = weights.replaceAll("A", "LM");
        } else if (maxWeight == EntityWeightClass.WEIGHT_MEDIUM) {
            //Probe, Recon Raid (attacker)
            retVal = weights.replaceAll("A", "MM");
            retVal = retVal.replaceAll("H", "LM");
        }
        return retVal;
    }

    /**
     * Adjust weights of units in a lance for factions that do not fit the typical
     * weight distribution.
     *
     * @param weights        A string of single-character letter codes for the weights of the units in the lance (e.g. "LMMH")
     * @param faction        The code of the faction to which the force belongs.
     * @return                A new String of the same format as weights
     */
    private String adjustWeightsForFaction(String weights, String faction) {
        /* Official AtB rules only specify DC, LA, and FWL; I have added
         * variations for some Clans.
         */
        String retVal = weights;
        if (faction.equals("DC")) {
            retVal = weights.replaceFirst("MM", "LH");
        }
        if ((faction.equals("LA") ||
                faction.equals("CCO") || faction.equals("CGB"))
                && weights.matches("[LM]{3,}")) {
            retVal = weights.replaceFirst("M", "H");
        }
        if (faction.equals("FWL") || faction.equals("CIH")) {
            retVal = weights.replaceFirst("HA", "HH");
        }
        return retVal;
    }

    /*
     * Convenience functions overloaded to provide default values.
     */
    protected void addLance(List<Entity> list, String faction, SkillLevel skill, int quality,
                            int weightClass, Campaign campaign) {
        addLance(list, faction, skill, quality, weightClass,
                EntityWeightClass.WEIGHT_ASSAULT, campaign, 0);
    }

    protected void addLance(List<Entity> list, String faction, SkillLevel skill, int quality,
                            int weightClass, int maxWeight, Campaign c) {
        addLance(list, faction, skill, quality, weightClass, maxWeight, c, 0);
    }

    /**
     *
     * Generates a lance of the indicated weight class. If the faction is Clan,
     * calls addStar instead. If the faction is CS/WoB, calls addLevelII.
     *
     * @param list            Generated Entities are added to this list.
     * @param faction        The faction code to use in generating the Entity
     * @param skill            The overall skill level of the force
     * @param quality        The force's equipment level
     * @param weightClass    The weight class of the lance or equivalent to generate
     * @param campaign
     * @param arrivalTurn    The turn in which the Lance is deployed in the scenario.
     */
    private void addLance(List<Entity> list, String faction, SkillLevel skill, int quality,
                          int weightClass, int maxWeight, Campaign campaign, int arrivalTurn) {
        if (Factions.getInstance().getFaction(faction).isClan()) {
            addStar(list, faction, skill, quality, weightClass, maxWeight, campaign, arrivalTurn);
            return;
        }
        if (faction.equals("CS") || faction.equals("WOB")) {
            addLevelII(list, faction, skill, quality, weightClass, maxWeight, campaign, arrivalTurn);
            return;
        }

        String weights = adjustForMaxWeight(campaign.getAtBConfig()
                .selectBotUnitWeights(AtBConfiguration.ORG_IS, weightClass), maxWeight);

        int forceType = FORCE_MEK;
        if (campaign.getCampaignOptions().getUseVehicles()) {
            int totalWeight = campaign.getCampaignOptions().getOpforLanceTypeMechs() +
                    campaign.getCampaignOptions().getOpforLanceTypeMixed() +
                    campaign.getCampaignOptions().getOpforLanceTypeVehicles();
            if (totalWeight > 0) {
                int roll = Compute.randomInt(totalWeight);
                if (roll < campaign.getCampaignOptions().getOpforLanceTypeVehicles()) {
                    forceType = FORCE_VEHICLE;
                } else if (roll < campaign.getCampaignOptions().getOpforLanceTypeVehicles() +
                        campaign.getCampaignOptions().getOpforLanceTypeMixed()) {
                    forceType = FORCE_MIXED;
                }
            }
        }
        if (forceType == FORCE_MEK && campaign.getCampaignOptions().getRegionalMechVariations()) {
            weights = adjustWeightsForFaction(weights, faction);
        }

        int[] unitTypes = new int[weights.length()];
        Arrays.fill(unitTypes, (forceType == FORCE_VEHICLE) ? UnitType.TANK : UnitType.MEK);
        /* Distribute vehicles randomly(-ish) through mixed units */
        if (forceType == FORCE_MIXED) {
            for (int i = 0; i < weights.length() / 2; i++) {
                int j = Compute.randomInt(weights.length());
                while (unitTypes[j] == UnitType.TANK) {
                    j++;
                    if (j >= weights.length()) {
                        j = 0;
                    }
                }
                unitTypes[j] = UnitType.TANK;
            }
        }

        for (int i = 0; i < weights.length(); i++) {
            Entity en = getEntity(faction, skill, quality, unitTypes[i],
                    AtBConfiguration.decodeWeightStr(weights, i),
                    campaign);
            if (en != null) {
                en.setDeployRound(arrivalTurn);
                list.add(en);
            }

            if ((unitTypes[i] == UnitType.TANK) && campaign.getCampaignOptions().getDoubleVehicles()) {
                en = getEntity(faction, skill, quality, unitTypes[i],
                        AtBConfiguration.decodeWeightStr(weights, i),
                        campaign);
                if (en != null) {
                    en.setDeployRound(arrivalTurn);
                    list.add(en);
                }
            }
        }
    }

    /**
     * Generates a Star of the indicated weight class.
     *
     * @param list            Generated Entities are added to this list.
     * @param faction        The faction code to use in generating the Entity
     * @param skill            The overall skill level of the force
     * @param quality        The force's equipment level
     * @param weightClass    The weight class of the lance or equivalent to generate
     * @param campaign
     * @param arrivalTurn    The turn in which the Lance is deployed in the scenario.
     */
    private void addStar(List<Entity> list, String faction, SkillLevel skill, int quality,
                         int weightClass, int maxWeight, Campaign campaign, int arrivalTurn) {
        int forceType = FORCE_MEK;
        /* 1 chance in 12 of a Nova, per AtB rules; CHH/CSL
         * close to 1/2, no chance for CBS. Added a chance to encounter
         * a vehicle Star in Clan second-line (rating C or lower) units,
         * or all unit ratings for CHH/CSL and CBS.
         */
        int roll = Compute.d6(2);
        int novaTarget = 11;
        if (faction.equals("CHH") || faction.equals("CSL")) {
            novaTarget = 8;
        } else if (faction.equals("CBS")) {
            novaTarget = TargetRoll.AUTOMATIC_FAIL;
        }
        int vehicleTarget = 4;
        if (!faction.equals("CHH") && !faction.equals("CSL") && !faction.equals("CBS")) {
            vehicleTarget -= quality;
        }

        if (roll >= novaTarget) {
            forceType = FORCE_NOVA;
        } else if (campaign.getCampaignOptions().getClanVehicles() && roll <= vehicleTarget) {
            forceType = FORCE_VEHICLE;
        }

        String weights = adjustForMaxWeight(campaign.getAtBConfig()
                .selectBotUnitWeights(AtBConfiguration.ORG_CLAN, weightClass), maxWeight);

        int unitType = (forceType == FORCE_VEHICLE) ? UnitType.TANK : UnitType.MEK;

        if (campaign.getCampaignOptions().getRegionalMechVariations()) {
            if (unitType == UnitType.MEK) {
                weights = adjustWeightsForFaction(weights, faction);
            }
            /* medium vees are rare among the Clans, FM:CC, p. 8 */
            if (unitType == UnitType.TANK) {
                weights = adjustWeightsForFaction(weights, "DC");
            }
        }

        int unitsPerPoint;
        switch (unitType) {
            case UnitType.TANK:
            case UnitType.AERO:
                unitsPerPoint = 2;
                break;
            case UnitType.PROTOMEK:
                unitsPerPoint = 5;
                break;
            case UnitType.MEK:
            case UnitType.INFANTRY:
            case UnitType.BATTLE_ARMOR:
            default:
                unitsPerPoint = 1;
                break;
        }

        /* Ensure Novas use Frontline tables to get best chance at Omnis */
        int tmpQuality = quality;
        if (forceType == FORCE_NOVA && quality < IUnitRating.DRAGOON_B) {
            tmpQuality = IUnitRating.DRAGOON_B;
        }
        for (int point = 0; point < weights.length(); point++) {
            for (int unit = 0; unit < unitsPerPoint; unit++) {
                Entity en = getEntity(faction, skill, tmpQuality, unitType,
                        AtBConfiguration.decodeWeightStr(weights, point),
                        campaign);
                if (en != null) {
                    en.setDeployRound(arrivalTurn);
                    list.add(en);
                }
            }
        }

        if (forceType == FORCE_NOVA) {
            unitType = UnitType.BATTLE_ARMOR;
            for (int i = 0; i < 5; i++) {
                Entity en = getEntity(faction, skill, quality,
                        unitType, -1, campaign);
                if (en != null) {
                    en.setDeployRound(arrivalTurn);
                    list.add(en);
                }
            }
        }
    }

    /**
     * Generates a ComStar/WoB Level II of the indicated weight class.
     *
     * @param list            Generated Entities are added to this list.
     * @param faction        The faction code to use in generating the Entity
     * @param skill            The overall skill level of the force
     * @param quality        The force's equipment level
     * @param weightClass    The weight class of the lance or equivalent to generate
     * @param campaign
     * @param arrivalTurn    The turn in which the Lance is deployed in the scenario.
     */
    private void addLevelII(List<Entity> list, String faction, SkillLevel skill, int quality,
                            int weightClass, int maxWeight, Campaign campaign, int arrivalTurn) {
        String weights = adjustForMaxWeight(campaign.getAtBConfig()
                .selectBotUnitWeights(AtBConfiguration.ORG_CS, weightClass), maxWeight);

        int forceType = FORCE_MEK;
        int roll = Compute.d6();
        if (roll < 4) {
            forceType = FORCE_VEHICLE;
        } else if (roll < 6) {
            forceType = FORCE_MIXED;
        }

        int[] unitTypes = new int[weights.length()];
        Arrays.fill(unitTypes, (forceType == FORCE_VEHICLE) ? UnitType.TANK : UnitType.MEK);
        /* Distribute vehicles randomly(-ish) through mixed units */
        if (forceType == FORCE_MIXED) {
            for (int i = 0; i < weights.length() / 2; i++) {
                int j = Compute.randomInt(weights.length());
                while (unitTypes[j] == UnitType.TANK) {
                    j++;
                    if (j >= weights.length()) {
                        j = 0;
                    }
                }
                unitTypes[j] = UnitType.TANK;
            }
        }

        for (int i = 0; i < weights.length(); i++) {
            Entity en = getEntity(faction, skill, quality, unitTypes[i],
                    AtBConfiguration.decodeWeightStr(weights, i),
                    campaign);
            if (en != null) {
                en.setDeployRound(arrivalTurn);
                list.add(en);
            }

            if (unitTypes[i] == UnitType.TANK && campaign.getCampaignOptions().getDoubleVehicles()) {
                en = getEntity(faction, skill, quality, unitTypes[i],
                        AtBConfiguration.decodeWeightStr(weights, i),
                        campaign);
                if (en != null) {
                    en.setDeployRound(arrivalTurn);
                    list.add(en);
                }
            }
        }
    }

    /**
     * Generates the indicated number of civilian entities.
     *
     * @param list        Generated entities are added to this list
     * @param num        The number of civilian entities to generate
     * @param campaign
     */
    protected void addCivilianUnits(List<Entity> list, int num, Campaign campaign) {
        list.addAll(AtBDynamicScenarioFactory.generateCivilianUnits(num, campaign));
    }

    /**
     * Generates the indicated number of turret entities.
     *
     * @param list      Generated entities are added to this list
     * @param num       The number of turrets to generate
     * @param skill     The skill level of the turret operators
     * @param quality   The quality level of the turrets
     * @param campaign  The campaign for which the turrets are being generated.
     * @param faction   The faction the turrets are being generated for
     */
    protected void addTurrets(List<Entity> list, int num, SkillLevel skill, int quality, Campaign campaign,
                              Faction faction) {
        list.addAll(AtBDynamicScenarioFactory.generateTurrets(num, skill, quality, campaign, faction));
    }

    /**
     * Potentially generates and adds a force of enemy aircraft to the mix of opposing force.
     * @param campaign  The campaign for which the aircraft are being generated.
     */
    protected void addAeroReinforcements(Campaign campaign) {
        // if the campaign is configured to it and we're in a 'standard' scenario or 'big battle' (don't add extra units to special scenarios)
        // if the opfor owns the planet, we have a user-defined chance of seeing 1-5 hostile conventional aircraft,
        //      one per "pip" of difficulty.
        // if the opfor does not own the planet, we have a (slightly lower) user-defined chance of seeing 1-5 hostile aerotechs,
        //      one per "pip" of difficulty.
        //      if generating aeros (crude approximation), we have a 1/2 chance of a light, 1/3 chance of medium and 1/6 chance of heavy
        if (!(campaign.getCampaignOptions().getAllowOpforAeros() && (isStandardMission() || isBigBattle()))) {
            return;
        }

        AtBContract contract = getContract(campaign);

        boolean opForOwnsPlanet = contract.getSystem().getFactions(campaign.getLocalDate())
                .contains(contract.getEnemyCode());

        boolean spawnConventional = opForOwnsPlanet && Compute.d6() >=
                MHQConstants.MAXIMUM_D6_VALUE - campaign.getCampaignOptions().getOpforAeroChance();

        // aerotechs are rarer, so spawn them less often
        boolean spawnAerotech = !opForOwnsPlanet && Compute.d6() >
                MHQConstants.MAXIMUM_D6_VALUE - campaign.getCampaignOptions().getOpforAeroChance() / 2;

        ArrayList<Entity> aircraft = new ArrayList<>();
        Entity aero;
        if (spawnConventional) {
            // skill level is 0-4 where 0 is "ultra-green" and 4 is "elite badass"
            for (int unitCount = 0; unitCount <= campaign.getCampaignOptions().getSkillLevel(); unitCount++) {
                aero = getEntity(contract.getEnemyCode(), contract.getEnemySkill(), contract.getEnemyQuality(),
                        UnitType.CONV_FIGHTER, EntityWeightClass.WEIGHT_LIGHT, campaign);
                if (aero != null) {
                    aircraft.add(aero);
                }
            }
        } else if (spawnAerotech) {
            for (int unitCount = 0; unitCount <= campaign.getCampaignOptions().getSkillLevel(); unitCount++) {
                // compute weight class
                int weightClass = randomAeroWeights[Compute.d6() - 1];

                aero = getEntity(contract.getEnemyCode(), contract.getEnemySkill(), contract.getEnemyQuality(),
                        UnitType.AERO, weightClass, campaign);
                if (aero != null) {
                    aircraft.add(aero);
                }
            }
        }

        if (!aircraft.isEmpty()) {
            /* Must set per-entity start pos for units after start of scenarios. Reinforcements
             * arrive from the enemy home edge, which is not necessarily the start pos. */
            final int deployRound = Compute.d6() + 2;   // deploy the new aircraft some time after the start of the game
            aircraft.stream().filter(Objects::nonNull).forEach(en -> {
                en.setStartingPos(enemyHome);
                en.setDeployRound(deployRound);
            });

            boolean isAeroMap = getTerrainType() == TER_LOW_ATMO ||
                    getTerrainType() == TER_SPACE;

            AtBDynamicScenarioFactory.populateAeroBombs(aircraft, campaign, !isAeroMap);

            BotForce bf = getEnemyBotForce(getContract(campaign), enemyHome, enemyHome, aircraft);
            bf.setName(bf.getName() + " (Air Support)");
            addBotForce(bf, campaign);
        }
    }

    /**
     * Potentially generates some scrubs (turrets and/or infantry) to be randomly added to the opposing force.
     * @param campaign The campaign for which the scrubs are being generated.
     */
    protected void addScrubReinforcements(Campaign campaign) {
        // if the campaign is configured to it and we are in a standard scenario or big battle
        // if the opfor owns the planet, and the opfor is defender we have a 1/3 chance of seeing 1-5 hostile turrets, one per "pip" of difficulty.
        // if the opfor owns the planet, and the opfor is defender we have a 1/3 chance of seeing 1-5 hostile conventional infantry, one per "pip".
        // if the opfor does not own the planet, we have a 1/6 chance of seeing 1-5 hostile battle armor, one per "pip" of difficulty.
        if (!(campaign.getCampaignOptions().getAllowOpforLocalUnits() && isAttacker() && (isStandardMission() || isBigBattle()))) {
            return;
        }

        AtBContract contract = getContract(campaign);

        boolean opForOwnsPlanet = contract.getSystem().getFactions(campaign.getLocalDate())
                                    .contains(contract.getEnemyCode());
        boolean spawnTurrets = opForOwnsPlanet &&
                Compute.d6() >= MHQConstants.MAXIMUM_D6_VALUE - campaign.getCampaignOptions().getOpforLocalUnitChance();
        boolean spawnConventionalInfantry = opForOwnsPlanet &&
                Compute.d6() >= MHQConstants.MAXIMUM_D6_VALUE - campaign.getCampaignOptions().getOpforLocalUnitChance();

        // battle armor is more rare
        boolean spawnBattleArmor = !opForOwnsPlanet &&
                Compute.d6() >= MHQConstants.MAXIMUM_D6_VALUE - campaign.getCampaignOptions().getOpforLocalUnitChance() / 2;

        boolean isTurretAppropriateTerrain = (getTerrainType() == TER_HEAVYURBAN) || (getTerrainType() == TER_LIGHTURBAN);
        boolean isInfantryAppropriateTerrain = isTurretAppropriateTerrain || (getTerrainType() == TER_WOODED);

        ArrayList<Entity> scrubs = new ArrayList<>();
        // don't bother spawning turrets if there won't be anything to put them on
        if (spawnTurrets && isTurretAppropriateTerrain) {
            // skill level is 0-4 where 0 is "ultra-green" and 4 is "elite badass" and drives the number of extra units
            addTurrets(scrubs,  campaign.getCampaignOptions().getSkillLevel() + 1, contract.getEnemySkill(),
                    contract.getEnemyQuality(), campaign, contract.getEnemy());
        }

        if (spawnConventionalInfantry && isInfantryAppropriateTerrain) {
            for (int unitCount = 0; unitCount <= campaign.getCampaignOptions().getSkillLevel(); unitCount++) {
                Entity infantry = getEntity(contract.getEnemyCode(), contract.getEnemySkill(), contract.getEnemyQuality(),
                        UnitType.INFANTRY, EntityWeightClass.WEIGHT_LIGHT, campaign);
                if (infantry != null) {
                    scrubs.add(infantry);
                }
            }
        }

        if (spawnBattleArmor && isInfantryAppropriateTerrain) {
            for (int unitCount = 0; unitCount <= campaign.getCampaignOptions().getSkillLevel(); unitCount++) {
                // some factions don't have access to battle armor, so they get conventional infantry instead
                Entity generatedUnit = getEntity(contract.getEnemyCode(), contract.getEnemySkill(), contract.getEnemyQuality(),
                        UnitType.BATTLE_ARMOR, EntityWeightClass.WEIGHT_LIGHT, campaign);

                if (generatedUnit != null) {
                    scrubs.add(generatedUnit);
                } else {
                    Entity infantry = getEntity(contract.getEnemyCode(), contract.getEnemySkill(), contract.getEnemyQuality(),
                            UnitType.INFANTRY, EntityWeightClass.WEIGHT_LIGHT, campaign);
                    if (infantry != null) {
                        scrubs.add(infantry);
                    }
                }
            }
        }

        if (!scrubs.isEmpty()) {
            /* Must set per-entity start pos for units after start of scenarios. Scrubs start in the center of the map. */
            scrubs.stream().filter(Objects::nonNull).forEach(en -> {
                en.setStartingPos(Board.START_CENTER);

                // if it's a short range enemy unit, it has a chance to be hidden based on
                // the option being enabled and the difficulty
                if (campaign.getGameOptions().booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)
                        && (en.getMaxWeaponRange() <= 4)
                        && (Compute.randomInt(4) <= campaign.getCampaignOptions().getSkillLevel())) {
                    en.setHidden(true);
                }
            });
            BotForce bf = getEnemyBotForce(getContract(campaign), Board.START_CENTER, enemyHome, scrubs);
            bf.setName(bf.getName() + " (Local Forces)");
            addBotForce(bf, campaign);
        }
    }

    /**
     * Worker method that adds a DropShip and related objective to the scenario.
     * @param campaign
     */
    protected void addDropship(Campaign campaign) {
        Entity dropship = AtBDynamicScenarioFactory.getEntity(getContract(campaign).getEmployerCode(),
                getContract(campaign).getAllySkill(),
                getContract(campaign).getAllyQuality(),
                UnitType.DROPSHIP, AtBDynamicScenarioFactory.UNIT_WEIGHT_UNSPECIFIED, campaign);

        if (dropship != null) {
            alliesPlayer.add(dropship);
            attachedUnitIds.add(UUID.fromString(dropship.getExternalIdAsString()));
            getExternalIDLookup().put(dropship.getExternalIdAsString(), dropship);

            ScenarioObjective dropshipObjective = new ScenarioObjective();
            dropshipObjective.setDescription("The employer has provided a DropShip for your use in this battle. Ensure it survives. Losing it will result in a 5 point penalty to your contract score.");
            dropshipObjective.setObjectiveCriterion(ObjectiveCriterion.Preserve);
            dropshipObjective.setPercentage(100);
            dropshipObjective.addUnit(dropship.getExternalIdAsString());

            // update the contract score by -5 if the objective is failed.
            ObjectiveEffect failureEffect = new ObjectiveEffect();
            failureEffect.effectType = ObjectiveEffectType.ContractScoreUpdate;
            failureEffect.howMuch = -5;

            dropshipObjective.addFailureEffect(failureEffect);
            getScenarioObjectives().add(dropshipObjective);
        }
    }

    /* Convenience methods for frequently-used arguments */
    protected BotForce getAllyBotForce(AtBContract c, int start, int home, List<Entity> entities) {
        return new BotForce(c.getAllyBotName(), 1, start, home, entities,
                c.getAllyCamouflage().clone(), c.getAllyColour());
    }

    protected BotForce getEnemyBotForce(AtBContract c, int start, List<Entity> entities) {
        return getEnemyBotForce(c, start, start, entities);
    }

    protected BotForce getEnemyBotForce(AtBContract c, int start, int home, List<Entity> entities) {
        return new BotForce(c.getEnemyBotName(), 2, start, home, entities,
                c.getEnemyCamouflage().clone(), c.getEnemyColour());
    }

    @Override
    public void generateStub(Campaign c) {
        super.generateStub(c);
        alliesPlayerStub = generateEntityStub(alliesPlayer);

        alliesPlayer.clear();
        if (null != bigBattleAllies) {
            bigBattleAllies.clear();
        }
        if (null != specMissionEnemies) {
            specMissionEnemies.clear();
        }
    }

    protected void setObjectives(Campaign c, AtBContract contract) {
        getScenarioObjectives().clear();
    }

    @Override
    protected void writeToXMLEnd(final PrintWriter pw, int indent) {
        indent++;
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "attacker", isAttacker());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "lanceForceId", lanceForceId);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "lanceRole", lanceRole.name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "deploymentDelay", deploymentDelay);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "lanceCount", lanceCount);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "rerollsRemaining", rerollsRemaining);

        if (null != bigBattleAllies && !bigBattleAllies.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "bigBattleAllies");
            for (Entity entity : bigBattleAllies) {
                if (entity != null) {
                    MHQXMLUtility.writeEntityWithCrewToXML(pw, indent, entity, bigBattleAllies);
                }
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "bigBattleAllies");
        } else if (!alliesPlayer.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "alliesPlayer");
            for (Entity entity : alliesPlayer) {
                if (entity != null) {
                    MHQXMLUtility.writeEntityWithCrewToXML(pw, indent, entity, alliesPlayer);
                }
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "alliesPlayer");
        }

        if (!alliesPlayerStub.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "alliesPlayerStub");
            for (String stub : alliesPlayerStub) {
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "entityStub", stub);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "alliesPlayerStub");
        }

        if (!attachedUnitIds.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "attachedUnits", getCsvFromList(attachedUnitIds));
        }

        if (!survivalBonus.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "survivalBonus", getCsvFromList(survivalBonus));
        }

        if (null != specMissionEnemies && !specMissionEnemies.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "specMissionEnemies");
            for (int i = 0; i < specMissionEnemies.size(); i++) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "playerWeight", "class", i);
                for (Entity entity : specMissionEnemies.get(i)) {
                    if (entity != null) {
                        MHQXMLUtility.writeEntityWithCrewToXML(pw, indent, entity, specMissionEnemies.get(i));
                    }
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "playerWeight");
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "specMissionEnemies");
        }

        if (!transportLinkages.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "transportLinkages");
            for (String key : transportLinkages.keySet()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "transportLinkage");
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "transportID", key);
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "transportedUnits", transportLinkages.get(key));
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "transportLinkage");
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "transportLinkages");
        }

        if (!numPlayerMinefields.isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "numPlayerMinefields");
            for (int key : numPlayerMinefields.keySet()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "numPlayerMinefield");
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "minefieldType", key);
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "minefieldCount", numPlayerMinefields.get(key).toString());
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "numPlayerMinefield");
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "numPlayerMinefields");
        }

        super.writeToXMLEnd(pw, --indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(final Node wn, final Version version, final Campaign campaign)
            throws ParseException {
        super.loadFieldsFromXmlNode(wn, version, campaign);
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("attacker")) {
                    setAttacker(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("lanceForceId")) {
                    lanceForceId = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("lanceRole")) {
                    lanceRole = AtBLanceRole.parseFromString(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("deploymentDelay")) {
                    deploymentDelay = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("usingFixedMap")) {
                    setUsingFixedMap(Boolean.parseBoolean(wn2.getTextContent().trim()));
                } else if (wn2.getNodeName().equalsIgnoreCase("lanceCount")) {
                    lanceCount = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("rerollsRemaining")) {
                    rerollsRemaining = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("alliesPlayer")) {
                    NodeList nl2 = wn2.getChildNodes();
                    for (int i = 0; i < nl2.getLength(); i++) {
                        Node wn3 = nl2.item(i);
                        if (wn3.getNodeName().equalsIgnoreCase("entity")) {
                            Entity en = null;
                            try {
                                en = MHQXMLUtility.parseSingleEntityMul((Element) wn3, campaign.getGameOptions());
                            } catch (Exception e) {
                                LogManager.getLogger().error("Error loading allied unit in scenario", e);
                            }
                            if (en != null) {
                                alliesPlayer.add(en);
                                entityIds.put(UUID.fromString(en.getExternalIdAsString()), en);
                                getExternalIDLookup().put(en.getExternalIdAsString(), en);
                            }
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("bigBattleAllies")) {
                    bigBattleAllies = new ArrayList<>();
                    NodeList nl2 = wn2.getChildNodes();
                    for (int i = 0; i < nl2.getLength(); i++) {
                        Node wn3 = nl2.item(i);
                        if (wn3.getNodeName().equalsIgnoreCase("entity")) {
                            Entity en = null;
                            try {
                                en = MHQXMLUtility.parseSingleEntityMul((Element) wn3, campaign.getGameOptions());
                            } catch (Exception e) {
                                LogManager.getLogger().error("Error loading allied unit in scenario", e);
                            }
                            if (en != null) {
                                bigBattleAllies.add(en);
                                entityIds.put(UUID.fromString(en.getExternalIdAsString()), en);
                                getExternalIDLookup().put(en.getExternalIdAsString(), en);
                            }
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("specMissionEnemies")) {
                    specMissionEnemies = new ArrayList<>();
                    for (int i = 0; i < 4; i++) {
                        specMissionEnemies.add(new ArrayList<>());
                    }
                    NodeList nl2 = wn2.getChildNodes();
                    for (int i = 0; i < nl2.getLength(); i++) {
                        Node wn3 = nl2.item(i);
                        if (wn3.getNodeName().equalsIgnoreCase("playerWeight")) {
                            int weightClass = Integer.parseInt(wn3.getAttributes().getNamedItem("class").getTextContent());
                            NodeList nl3 = wn3.getChildNodes();
                            for (int j = 0; j < nl3.getLength(); j++) {
                                Node wn4 = nl3.item(j);
                                if (wn4.getNodeName().equalsIgnoreCase("entity")) {
                                    Entity en = null;
                                    try {
                                        en = MHQXMLUtility.parseSingleEntityMul((Element) wn4, campaign.getGameOptions());
                                    } catch (Exception e) {
                                        LogManager.getLogger().error("Error loading enemy unit in scenario", e);
                                    }
                                    if (null != en) {
                                        specMissionEnemies.get(weightClass).add(en);
                                        entityIds.put(UUID.fromString(en.getExternalIdAsString()), en);
                                        getExternalIDLookup().put(en.getExternalIdAsString(), en);
                                    }
                                }
                            }
                        }
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("alliesPlayerStub")) {
                    alliesPlayerStub = getEntityStub(wn2);
                } else if (wn2.getNodeName().equalsIgnoreCase("attachedUnits")) {
                    String[] ids = wn2.getTextContent().split(",");
                    for (String s : ids) {
                        attachedUnitIds.add(UUID.fromString(s));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("survivalBonus")) {
                    String[] ids = wn2.getTextContent().split(",");
                    for (String s : ids) {
                        survivalBonus.add(UUID.fromString(s));
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("transportLinkages")) {
                    try {
                        transportLinkages = loadTransportLinkages(wn2);
                    } catch (Exception e) {
                        LogManager.getLogger().error("Error loading transport linkages in scenario", e);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("numPlayerMinefields")) {
                    try {
                        loadMinefieldCounts(wn2);
                    } catch (Exception e) {
                        LogManager.getLogger().error("Error loading minefield counts in scenario", e);
                    }
                }
            } catch (Exception e) {
                LogManager.getLogger().error("", e);
            }
        }
        /* In the event a discrepancy occurs between a RAT entry and the unit lookup name,
         * remove the entry from the list of entities that give survival bonuses
         * to avoid an critical error that prevents battle resolution.
         */
        ArrayList<UUID> toRemove = new ArrayList<>();
        for (UUID uid : survivalBonus) {
            if (!entityIds.containsKey(uid)) {
                toRemove.add(uid);
            }
        }
        survivalBonus.removeAll(toRemove);
    }

    private static Map<String, List<String>> loadTransportLinkages(Node wn) {
        NodeList nl = wn.getChildNodes();

        Map<String, List<String>> transportLinkages = new HashMap<>();
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("transportLinkage")) {
                loadTransportLinkage(wn2, transportLinkages);
            }
        }

        return transportLinkages;
    }

    private static void loadTransportLinkage(Node wn, Map<String, List<String>> transportLinkages) {
        NodeList nl = wn.getChildNodes();

        String transportID = null;
        List<String> transportedUnitIDs = null;
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            if (wn2.getNodeName().equalsIgnoreCase("transportID")) {
                transportID = wn2.getTextContent().trim();
            } else if (wn2.getNodeName().equalsIgnoreCase("transportedUnits")) {
                transportedUnitIDs = Arrays.asList(wn2.getTextContent().split(","));
            }
        }

        if ((transportID != null) && (transportedUnitIDs != null)) {
            transportLinkages.put(transportID, transportedUnitIDs);
        }
    }

    /**
     * Worker function that loads the minefield counts for the player
     */
    private void loadMinefieldCounts(Node wn) throws NumberFormatException {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            if (wn2.getNodeName().equalsIgnoreCase("numPlayerMinefield")) {
                NodeList minefieldNodes = wn2.getChildNodes();

                int minefieldType = 0;
                int minefieldCount = 0;

                for (int minefieldIndex = 0; minefieldIndex < minefieldNodes.getLength(); minefieldIndex++) {
                    Node wn3 = minefieldNodes.item(minefieldIndex);

                    if (wn3.getNodeName().equalsIgnoreCase("minefieldType")) {
                        minefieldType = Integer.parseInt(wn3.getTextContent());
                    } else if (wn3.getNodeName().equalsIgnoreCase("minefieldCount")) {
                        minefieldCount = Integer.parseInt(wn3.getTextContent());
                    }
                }

                numPlayerMinefields.put(minefieldType, minefieldCount);
            }
        }
    }

    protected String getCsvFromList(List<?> list) {
        StringJoiner retVal = new StringJoiner(",");
        for (Object item : list) {
            retVal.add(item.toString());
        }
        return retVal.toString();
    }

    public AtBContract getContract(Campaign c) {
        return (AtBContract) c.getMission(getMissionId());
    }

    /**
     * Gets all the entities that are part of the given entity list and are
     * not in this scenario's transport linkages as a transported unit.
     */
    public List<Entity> filterUntransportedUnits(List<Entity> entities) {
        List<Entity> retVal = new ArrayList<>();

        // assemble a set of transported units for easier lookup
        Set<String> transportedUnits = new HashSet<>();
        for (List<String> transported : getTransportLinkages().values()) {
            transportedUnits.addAll(transported);
        }

        for (Entity entity : entities) {
            if (!transportedUnits.contains(entity.getExternalIdAsString())) {
                retVal.add(entity);
            }
        }

        return retVal;
    }

    public int getLanceForceId() {
        return lanceForceId;
    }

    public AtBLanceRole getLanceRole() {
        return lanceRole;
    }

    public Lance getLance(Campaign c) {
        return c.getLances().get(lanceForceId);
    }

    public void setLance(Lance l) {
        lanceForceId = l.getForceId();
    }

    public boolean isAttacker() {
        return attacker;
    }

    public void setAttacker(boolean attacker) {
        this.attacker = attacker;
    }

    public List<Entity> getAlliesPlayer() {
        return alliesPlayer;
    }

    public List<UUID> getAttachedUnitIds() {
        return attachedUnitIds;
    }

    public List<UUID> getSurvivalBonusIds() {
        return survivalBonus;
    }

    public Entity getEntity(UUID id) {
        return entityIds.get(id);
    }

    public List<String> getAlliesPlayerStub() {
        return alliesPlayerStub;
    }

    public int getDeploymentDelay() {
        return deploymentDelay;
    }

    public void setDeploymentDelay(int delay) {
        this.deploymentDelay = delay;
    }

    public int getLanceCount() {
        return lanceCount;
    }

    public void setLanceCount(int lanceCount) {
        this.lanceCount = lanceCount;
    }

    public int getRerollsRemaining() {
        return rerollsRemaining;
    }

    public void useReroll() {
        rerollsRemaining--;
    }

    public void setRerolls(int rerolls) {
        rerollsRemaining = rerolls;
    }

    public int getEnemyHome() {
        return enemyHome;
    }

    public void setEnemyHome(int enemyHome) {
        this.enemyHome = enemyHome;
    }

    public int getNumPlayerMinefields(int minefieldType) {
        return numPlayerMinefields.containsKey(minefieldType) ? numPlayerMinefields.get(minefieldType) : 0;
    }

    public void setNumPlayerMinefields(int minefieldType, int numPlayerMinefields) {
        this.numPlayerMinefields.put(minefieldType, numPlayerMinefields);
    }

    public Map<String, List<String>> getTransportLinkages() {
        return transportLinkages;
    }

    public void setTransportLinkages(HashMap<String, List<String>> transportLinkages) {
        this.transportLinkages = transportLinkages;
    }

    /**
     * Adds a transport-cargo pair to the internal transport relationship store.
     * @param transport
     * @param cargo
     */
    public void addTransportRelationship(String transport, String cargo) {
        if (!transportLinkages.containsKey(transport)) {
            transportLinkages.put(transport, new ArrayList<>());
        }

        transportLinkages.get(transport).add(cargo);
    }

    @Override
    public boolean isFriendlyUnit(Entity entity, Campaign campaign) {
        return getAlliesPlayer().stream().anyMatch(unit -> unit.getExternalIdAsString().equals(entity.getExternalIdAsString())) ||
                super.isFriendlyUnit(entity, campaign);
    }

    public String getBattlefieldControlDescription() {
        return getResourceBundle().getString("battleDetails.common.winnerControlsBattlefield");
    }

    public String getDeploymentInstructions() {
        if (this.isBigBattle()) {
            return getResourceBundle().getString("battleDetails.deployEightMeks");
        } else if (isSpecialMission()) {
            return getResourceBundle().getString("battleDetails.deploySingleMek");
        } else {
            return "";
        }
    }

    @Override
    public boolean canStartScenario(Campaign c) {
        return c.getLocalDate().equals(getDate()) && super.canStartScenario(c);
    }
}
