package mekhq.gui.utilities;

import megamek.common.EntityWeightClass;
import megamek.common.UnitType;
import mekhq.campaign.unit.Unit;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnitsListToMap {
    public static Map<Integer, Map<Integer, List<Unit>>> ToMapOfMaps(List<Unit> units) {
        Map<Integer, Map<Integer, List<Unit>>> unitsByTypeAndWeightMap = new HashMap<>();

        for (int i = 0; i < UnitType.SIZE; i++) {
            String unitTypeName = UnitType.getTypeName(i);
            double[] weightLimits = EntityWeightClass.getWeightLimitByType(unitTypeName);
            for (int j = 0; j < weightLimits.length; j++) {
                double tonnage = weightLimits[j];
                if (tonnage == 0) {
                    continue;
                }

                int weightClass = EntityWeightClass.getWeightClass(tonnage, unitTypeName);
                List<Unit> unitsForTypeWeight = new ArrayList<>();

                for (Unit unit : units) {
                    int unitType = unit.getEntity().getUnitType();
                    int unitWeight = unit.getEntity().getWeightClass();

                    if (unitType == i && unitWeight == j) {
                        unitsForTypeWeight.add(unit);
                    }
                }

                if (unitsForTypeWeight.size() > 0) {
                    Map<Integer, List<Unit>> tempMap = new HashMap<>(1);
                    tempMap.put(j, unitsForTypeWeight);
                    unitsByTypeAndWeightMap.put(i, tempMap);
                }
            }
        }

        return unitsByTypeAndWeightMap;
    }

    public static JMenu menuFromMap(Map<Integer, Map<Integer, List<Unit>>> map) {

    }
}
/*
for (int i = 0; i < UnitType.SIZE; i++)
{
    String unittype = UnitType.getTypeName(i);
    String displayname = UnitType.getTypeDisplayableName(i);
    unitTypeMenus.put(unittype, new JMenu(displayname));
    unitTypeMenus.get(unittype).setName(unittype);
    unitTypeMenus.get(unittype).setEnabled(false);
    for (int j = 0; j < EntityWeightClass.getWeightLimitByType(unittype).length; j++) {
        double tonnage = EntityWeightClass.getWeightLimitByType(unittype)[j];
        if (tonnage == 0) {
            continue;
        }

        int weightClass = EntityWeightClass.getWeightClass(tonnage, unittype);
        String displayname2 = EntityWeightClass.getClassName(weightClass, unittype, false);
        String weightClassMenuName = unittype + "_"
                + EntityWeightClass.getClassName(weightClass, unittype, false);
        weightClassForUnitType.put(weightClassMenuName, new JMenu(displayname2));
        weightClassForUnitType.get(weightClassMenuName).setName(weightClassMenuName);
        weightClassForUnitType.get(weightClassMenuName).setEnabled(false);
    }
}
 */
