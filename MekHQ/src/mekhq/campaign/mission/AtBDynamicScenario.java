package mekhq.campaign.mission;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Entity;

/**
 * Data structure intended to hold data relevant to AtB Scenarios (AtB 3.0) 
 * @author NickAragua
 *
 */
public class AtBDynamicScenario extends Scenario {

    /**
     * 
     */
    private static final long serialVersionUID = 4671466413188687036L;

    // derived fields used for various calculations
    private int effectivePlayerUnitCount;
    private int effectivePlayerBV;
    
    // fields intrinsic to the scenario. Contain one-off information
    // such as terrain type/map, environmental factors, map size
    private int terrainType;
    private int light;
    private int weather;
    private int wind;
    private int fog;
    private int atmosphere;
    private float gravity;
    private int mapSizeX;
    private int mapSizeY;
    private String map;
    private int rerollsRemaining;
    private int lanceCount;
    
    // lists of forces
    private List<Entity> attachedUnits;        //units attached to the player force
    private List<BotForce> botForces;          //computer-controlled bot forces
    private List<String> attachedUnitStubs;    //stubs for units attached to the player force
    private List<BotForceStub> botForceStubs;  //stubs for computer-controlled bot forces
    
    // convenient pointers that let us keep data around that would otherwise need reloading
    private ScenarioTemplate template;      // the template that is being used to generate this scenario
    
    public AtBDynamicScenario() {
        attachedUnits = new ArrayList<>();
        botForces = new ArrayList<>();
        attachedUnitStubs = new ArrayList<>();
        botForceStubs = new ArrayList<>();
    }
    
    public List<Entity> getAttachedUnits() {
        return attachedUnits;
    }

    public void addBotForce(BotForce botForce) {
        botForces.add(botForce);
    }
    
    public BotForce getBotForce(int i) {
        return botForces.get(i);
    }

    public int getNumBots() {
        if (isCurrent()) {
            return botForces.size();
        } else {
            return botForceStubs.size();
        }
    }

    public List<String> getAttachedUnitStubs() {
        return attachedUnitStubs;
    }

    public List<BotForceStub> getBotForceStubs() {
        return botForceStubs;
    }

    public int getTerrainType() {
        return terrainType;
    }

    public void setTerrainType(int terrainType) {
        this.terrainType = terrainType;
    }

    public int getLight() {
        return light;
    }

    public void setLight(int light) {
        this.light = light;
    }

    public int getWeather() {
        return weather;
    }

    public void setWeather(int weather) {
        this.weather = weather;
    }

    public int getWind() {
        return wind;
    }

    public void setWind(int wind) {
        this.wind = wind;
    }

    public int getFog() {
        return fog;
    }

    public void setFog(int fog) {
        this.fog = fog;
    }

    public int getAtmosphere() {
        return atmosphere;
    }

    public void setAtmosphere(int atmosphere) {
        this.atmosphere = atmosphere;
    }

    public float getGravity() {
        return gravity;
    }

    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public int getMapSizeX() {
        return mapSizeX;
    }

    public void setMapSizeX(int mapSizeX) {
        this.mapSizeX = mapSizeX;
    }

    public int getMapSizeY() {
        return mapSizeY;
    }

    public void setMapSizeY(int mapSizeY) {
        this.mapSizeY = mapSizeY;
    }

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
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
    
    public int getEffectivePlayerUnitCount() {
        return effectivePlayerUnitCount;
    }
    
    public void setEffectivePlayerUnitCount(int unitCount) {
        effectivePlayerUnitCount = unitCount;
    }
    
    public int getEffectivePlayerBV() {
        return effectivePlayerBV;
    }
    
    public void setEffectivePlayerBV(int unitCount) {
        effectivePlayerBV = unitCount;
    }
    
    public void setScenarioTemplate(ScenarioTemplate template) {
        this.template = template;
    }
    
    public ScenarioTemplate getTemplate() {
        return template;
    }
}
