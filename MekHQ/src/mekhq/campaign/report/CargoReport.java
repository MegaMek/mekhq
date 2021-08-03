/*
 * CargoReport.java
 *
 * Copyright (c) 2013 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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
package mekhq.campaign.report;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JTextPane;

import mekhq.campaign.Campaign;
import mekhq.campaign.unit.CargoStatistics;
import mekhq.campaign.unit.HangarStatistics;
import mekhq.campaign.unit.Unit;



/**
 * @author Jay Lawson
 */
public class CargoReport extends Report {


    public CargoReport(Campaign c) {
        super(c);
    }

    @Override
    public String getTitle() {
        return "Cargo Report";
    }

    @Override
    public JTextPane getReport() {
        JTextPane txtReport = new JTextPane();
        txtReport.setMinimumSize(new Dimension(800, 500));
        txtReport.setFont(new Font("Courier New", Font.PLAIN, 12));
        txtReport.setText(getCargoDetails());
        return txtReport;
    }

    public String getCargoDetails() {
        CargoStatistics cargoStats = getCampaign().getCargoStatistics();
        HangarStatistics hangarStats = getCampaign().getHangarStatistics();

        StringBuffer sb = new StringBuffer("Cargo\n\n");
        double ccc = cargoStats.getTotalCombinedCargoCapacity();
        double gcc = cargoStats.getTotalCargoCapacity();
        double icc = cargoStats.getTotalInsulatedCargoCapacity();
        double lcc = cargoStats.getTotalLiquidCargoCapacity();
        double scc = cargoStats.getTotalLivestockCargoCapacity();
        double rcc = cargoStats.getTotalRefrigeratedCargoCapacity();
        double tonnage = cargoStats.getCargoTonnage(false);
        double mothballedTonnage = cargoStats.getCargoTonnage(false, true);
        double mothballedUnits = hangarStats.getNumberOfUnitsByType(Unit.ETYPE_MOTHBALLED);
        double combined = (tonnage + mothballedTonnage);
        double transported = Math.min(combined, ccc);
        double overage = combined - transported;

        sb.append(String.format("%-35s      %6.3f\n", "Total Capacity:", ccc));
        sb.append(String.format("%-35s      %6.3f\n", "General Capacity:", gcc));
        sb.append(String.format("%-35s      %6.3f\n", "Insulated Capacity:", icc));
        sb.append(String.format("%-35s      %6.3f\n", "Liquid Capacity:", lcc));
        sb.append(String.format("%-35s      %6.3f\n", "Livestock Capacity:", scc));
        sb.append(String.format("%-35s      %6.3f\n", "Refrigerated Capacity:", rcc));
        sb.append(String.format("%-35s      %6.3f\n", "Cargo Transported:", tonnage));
        sb.append(String.format("%-35s      %4s (%1.0f)\n", "Mothballed Units as Cargo (Tons):", mothballedUnits, mothballedTonnage));
        sb.append(String.format("%-35s      %6.3f/%1.3f\n", "Transported/Capacity:", transported, ccc));
        sb.append(String.format("%-35s      %6.3f\n", "Overage Not Transported:", overage));

        return new String(sb);
    }
}
