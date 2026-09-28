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
package mekhq.campaign.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import megamek.Version;
import megamek.common.units.Tank;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.VeeStabilizer;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.meks.MekActuator;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import testUtilities.parts.PartsCensus;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Characterization tests for saving and loading a campaign's parts. The warehouse is written exactly as the campaign
 * save writes it, then loaded into a fresh campaign through the same steps the campaign load takes: each part is read
 * by {@link Part#generateInstanceFromXML}, imported, cleaned up and linked by {@code CampaignXmlParser}'s own
 * {@code postProcessParts} (called by reflection, since it is private), and each unit then initialises and checks its
 * parts. The parts census of every unit and of the warehouse stock must come back unchanged.
 *
 * <p>This is a parts-level round trip. The units are rebuilt from their fixture files with their saved ids rather than
 * read back from the save, because {@code CampaignXmlParser.parse()} needs the MekHQ application and opens a dialog, so
 * a whole-campaign round trip is not possible in a unit test today.</p>
 *
 * <p>The cleanup cases pin TEST-05: the load silently deletes parts that meet one of its legacy rules and only writes
 * a log line. Each case asserts that the broken part is gone and nothing else changed; such a case changes when a rule
 * is changed on purpose.</p>
 */
class PartsSaveLoadCharacterizationTest {
    private static final Version SAVE_VERSION = new Version();
    private static final int TRANSIT_DAYS = 30;

    private PartsScenario scenario;
    private Campaign campaign;
    private final Map<UnitFixture, Unit> units = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        units.clear();
        for (UnitFixture fixture : List.of(UnitFixture.WOLVERINE_WVR_6R, UnitFixture.LEOPARD_DROPSHIP,
              UnitFixture.IS_STANDARD_BATTLE_ARMOR_LASER)) {
            units.put(fixture, scenario.withUnit(fixture));
        }
    }

    private Unit wolverine() {
        return units.get(UnitFixture.WOLVERINE_WVR_6R);
    }

    private EquipmentPart wolverineMediumLaser() {
        for (EquipmentPart equipmentPart : PartsScenario.unitParts(wolverine(), EquipmentPart.class)) {
            if ("Medium Laser".equals(equipmentPart.getName())) {
                return equipmentPart;
            }
        }
        throw new IllegalStateException("The Wolverine WVR-6R fixture has no Medium Laser");
    }

    /**
     * Places a part in the warehouse as it is, without merging, the way a load or a refit leaves it.
     */
    private void placeInWarehouse(Part part) {
        part.setCampaign(campaign);
        scenario.getWarehouse().addPart(part, false);
    }

    @Test
    void roundTripKeepsEveryUnitAndTheWarehouseStock() throws Exception {
        wolverineMediumLaser().setHits(1);
        scenario.withSpare(wolverineMediumLaser().clone(), 2);
        Part partInTransit = PartsScenario.unitParts(wolverine(), MekActuator.class).getFirst().clone();
        partInTransit.setCampaign(campaign);
        campaign.getQuartermaster().addPart(partInTransit, TRANSIT_DAYS);
        Part partReservedForRefit = PartsScenario.unitParts(wolverine(), MekLocation.class).getFirst().clone();
        partReservedForRefit.setRefitUnit(wolverine());
        placeInWarehouse(partReservedForRefit);
        int partCountBefore = scenario.getWarehouse().getParts().size();

        Campaign reloaded = saveAndReload();

        for (Unit unit : units.values()) {
            Unit reloadedUnit = reloaded.getUnit(unit.getId());
            assertNotNull(reloadedUnit, unit.getName() + " was not reloaded");
            assertEquals(PartsCensus.ofUnit(unit), PartsCensus.ofUnit(reloadedUnit), unit.getName());
        }
        assertEquals(PartsCensus.ofWarehouseStock(scenario.getWarehouse()),
              PartsCensus.ofWarehouseStock(reloaded.getPlayerForce().getWarehouse()));
        assertEquals(partCountBefore, reloaded.getPlayerForce().getWarehouse().getParts().size());
        Part reloadedLaser = reloaded.getPlayerForce().getWarehouse().getPart(wolverineMediumLaser().getId());
        assertEquals(1, reloadedLaser.getHits());
        assertEquals(TRANSIT_DAYS,
              reloaded.getPlayerForce().getWarehouse().getPart(partInTransit.getId()).getDaysToArrival());
    }

    @Test
    void loadDropsASpareAmmoBin() throws Exception {
        SortedMap<String, Integer> stockBefore = PartsCensus.ofWarehouseStock(scenario.getWarehouse());
        placeInWarehouse(PartsScenario.unitParts(wolverine(), AmmoBin.class).getFirst().clone());

        Campaign reloaded = saveAndReload();

        // Current behaviour, see TEST-05: "spare ammo bins are useless", so the bin is deleted without a report
        assertEquals(stockBefore, PartsCensus.ofWarehouseStock(reloaded.getPlayerForce().getWarehouse()));
    }

    @Test
    void loadDropsAnEquipmentPartWhoseMountNoLongerExists() throws Exception {
        SortedMap<String, Integer> wolverineBefore = PartsCensus.ofUnit(wolverine());
        EquipmentPart orphanLaser = (EquipmentPart) wolverineMediumLaser().clone();
        orphanLaser.setEquipmentNum(999);
        orphanLaser.setUnit(wolverine());
        placeInWarehouse(orphanLaser);

        Campaign reloaded = saveAndReload();

        // Current behaviour, see TEST-05: a part whose equipment number has no mount is deleted without a report
        assertEquals(wolverineBefore, PartsCensus.ofUnit(reloaded.getUnit(wolverine().getId())));
        assertNull(reloaded.getPlayerForce().getWarehouse().getPart(orphanLaser.getId()));
    }

    @Test
    void loadDropsASecondPartForTheSameEquipment() throws Exception {
        SortedMap<String, Integer> wolverineBefore = PartsCensus.ofUnit(wolverine());
        EquipmentPart duplicateLaser = (EquipmentPart) wolverineMediumLaser().clone();
        duplicateLaser.setUnit(wolverine());
        placeInWarehouse(duplicateLaser);

        Campaign reloaded = saveAndReload();

        // Current behaviour, see TEST-05: the part with the higher id is the one deleted
        assertEquals(wolverineBefore, PartsCensus.ofUnit(reloaded.getUnit(wolverine().getId())));
        assertNull(reloaded.getPlayerForce().getWarehouse().getPart(duplicateLaser.getId()));
        assertNotNull(reloaded.getPlayerForce().getWarehouse().getPart(wolverineMediumLaser().getId()));
    }

    @Test
    void loadDropsAMissingPartThatBelongsToNoUnit() throws Exception {
        SortedMap<String, Integer> stockBefore = PartsCensus.ofWarehouseStock(scenario.getWarehouse());
        placeInWarehouse(wolverineMediumLaser().getMissingPart());

        Campaign reloaded = saveAndReload();

        assertEquals(stockBefore, PartsCensus.ofWarehouseStock(reloaded.getPlayerForce().getWarehouse()));
    }

    @Test
    void legacyMekLocationClassNameStillLoads() throws Exception {
        Part spareLocation = PartsScenario.unitParts(wolverine(), MekLocation.class).getFirst().clone();
        String legacyXml = partXml(spareLocation).replace(MekLocation.class.getName(),
              "mekhq.campaign.parts.MekLocation");

        Part loadedPart = Part.generateInstanceFromXML(firstElement(legacyXml), SAVE_VERSION);

        assertInstanceOf(MekLocation.class, loadedPart);
        assertEquals(spareLocation.getName(), loadedPart.getName());
    }

    @Test
    void legacyStabiliserClassNamesAreNotRecognised() throws Exception {
        VeeStabilizer stabilizer = new VeeStabilizer(40, Tank.LOC_FRONT, campaign);
        String currentXml = partXml(stabilizer);

        String fullLegacyXml = currentXml.replace(VeeStabilizer.class.getName(), "mekhq.campaign.parts.VeeStabiliser");
        String shortLegacyXml = currentXml.replace(VeeStabilizer.class.getName(), "VeeStabiliser");

        // Current behaviour, see TEST-05: the rename entry maps only the bare name, and to a bare class name, so
        // neither spelling loads and the part is dropped with a log line
        assertNull(Part.generateInstanceFromXML(firstElement(fullLegacyXml), SAVE_VERSION));
        assertNull(Part.generateInstanceFromXML(firstElement(shortLegacyXml), SAVE_VERSION));
        assertInstanceOf(VeeStabilizer.class, Part.generateInstanceFromXML(firstElement(currentXml), SAVE_VERSION));
    }

    /**
     * Writes the warehouse as the campaign save does and loads it into a fresh campaign holding the same units.
     */
    private Campaign saveAndReload() throws Exception {
        StringWriter partsXml = new StringWriter();
        PrintWriter printWriter = new PrintWriter(partsXml);
        scenario.getWarehouse().writeToXML(printWriter, 0, "parts");
        printWriter.flush();

        Campaign reloaded = PartsScenario.create().getCampaign();
        for (Map.Entry<UnitFixture, Unit> entry : units.entrySet()) {
            Unit reloadedUnit = new Unit(entry.getKey().loadEntity(), reloaded);
            reloadedUnit.setId(entry.getValue().getId());
            reloaded.importUnit(reloadedUnit);
        }

        reloaded.importParts(readParts(partsXml.toString()));
        runLoadTimePartCleanup(reloaded);
        for (Unit reloadedUnit : reloaded.getPlayerForce().getHangar().getUnits()) {
            reloadedUnit.setCampaign(reloaded);
            reloadedUnit.fixReferences(reloaded);
        }
        for (Unit reloadedUnit : reloaded.getPlayerForce().getHangar().getUnits()) {
            reloadedUnit.initializeParts(true);
            reloadedUnit.runDiagnostic(false);
        }
        return reloaded;
    }

    private static List<Part> readParts(String partsXml) throws Exception {
        NodeList partNodes = parse(partsXml).getDocumentElement().getChildNodes();
        List<Part> parts = new ArrayList<>();
        for (int index = 0; index < partNodes.getLength(); index++) {
            Node partNode = partNodes.item(index);
            boolean isPartElement = (partNode.getNodeType() == Node.ELEMENT_NODE)
                                          && "part".equalsIgnoreCase(partNode.getNodeName());
            if (!isPartElement) {
                continue;
            }
            Part part = Part.generateInstanceFromXML(partNode, SAVE_VERSION);
            if (part != null) {
                parts.add(part);
            }
        }
        return parts;
    }

    /**
     * Runs the campaign load's own part cleanup and linking, {@code CampaignXmlParser.postProcessParts}.
     */
    private static void runLoadTimePartCleanup(Campaign reloaded) throws ReflectiveOperationException {
        Method postProcessParts = CampaignXmlParser.class.getDeclaredMethod("postProcessParts", Campaign.class,
              Version.class);
        postProcessParts.setAccessible(true);
        postProcessParts.invoke(null, reloaded, SAVE_VERSION);
    }

    private static String partXml(Part part) {
        StringWriter partXml = new StringWriter();
        PrintWriter printWriter = new PrintWriter(partXml);
        part.writeToXML(printWriter, 0);
        printWriter.flush();
        return partXml.toString();
    }

    private static Element firstElement(String xml) throws Exception {
        return parse(xml).getDocumentElement();
    }

    private static Document parse(String xml) throws Exception {
        return MHQXMLUtility.newSafeDocumentBuilder()
                     .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
