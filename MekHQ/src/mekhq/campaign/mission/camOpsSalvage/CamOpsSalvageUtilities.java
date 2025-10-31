package mekhq.campaign.mission.camOpsSalvage;

import static megamek.common.equipment.MiscType.F_NAVAL_TUG_ADAPTOR;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.List;

import megamek.common.equipment.Mounted;
import megamek.common.units.Aero;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Warship;
import mekhq.campaign.Campaign;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;

public class CamOpsSalvageUtilities {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CamOpsSalvage";

    /**
     * Generates a tooltip string describing the salvage capabilities of units in a force.
     *
     * <p>For each unit capable of salvage, the tooltip includes:</p>
     * <ul>
     *   <li>Unit name</li>
     *   <li>Drag/tow capacity in tons (for non-large vessels)</li>
     *   <li>Cargo capacity in tons (for non-Mek units)</li>
     *   <li>Naval tug status (for large vessels like DropShips and WarShips)</li>
     * </ul>
     *
     * @param unitsInForce the list of units to analyze for salvage capabilities
     * @param isInSpace    {@code true} if checking space salvage capabilities, {@code false} for ground operations
     *
     * @return an HTML-formatted string describing each salvage-capable unit's capabilities
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static String getSalvageTooltip(List<Unit> unitsInForce, boolean isInSpace) {
        StringBuilder tooltip = new StringBuilder();

        for (Unit unit : unitsInForce) {
            if (!tooltip.isEmpty()) {
                tooltip.append("<br>");
            }
            if (unit.canSalvage(isInSpace)) {
                Entity entity = unit.getEntity();
                if (entity != null) {
                    boolean isLargeVessel = entity instanceof Dropship || entity instanceof Warship;
                    tooltip.append(unit.getName());

                    double tonnage = entity.getTonnage();
                    if (!isLargeVessel) {
                        tooltip.append(" (").append(getFormattedTextAt(RESOURCE_BUNDLE,
                              "PostSalvagePicker.tooltip.drag", tonnage)).append(")");
                    }

                    double cargoCapacity = unit.getCargoCapacity();
                    if (!(entity instanceof Mek)) {
                        tooltip.append(" (").append(getFormattedTextAt(RESOURCE_BUNDLE,
                              "PostSalvagePicker.tooltip.cargo", cargoCapacity)).append(")");

                        if (isLargeVessel) {
                            if (CamOpsSalvageUtilities.hasNavalTug(entity)) {
                                tooltip.append(" (").append(getFormattedTextAt(RESOURCE_BUNDLE,
                                      "PostSalvagePicker.tooltip.tug")).append(")");
                            }
                        }
                    }
                }
            }
        }

        return tooltip.toString();
    }

    public static boolean hasNavalTug(Entity entity) {
        for (Mounted<?> mounted : entity.getMisc()) {
            if (mounted.getType().hasFlag(F_NAVAL_TUG_ADAPTOR)) {
                // isOperable doesn't check if the mounted location still exists, so we check for that first.
                if (!mounted.getEntity().isLocationBad(mounted.getLocation()) && (mounted.isOperable())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Processes and finalizes salvage after player selection.
     *
     * <p>This method handles the actual processing of salvage units, including:</p>
     * <ul>
     *   <li>Adding claimed salvage units to the campaign</li>
     *   <li>Processing sold salvage units and crediting the account</li>
     *   <li>Handling salvage exchange for contracts</li>
     *   <li>Updating contract salvage tracking</li>
     *   <li>Setting repair locations for salvaged units</li>
     * </ul>
     *
     * @param campaign        The current {@link Campaign} to add salvage to.
     * @param mission         The {@link Mission} associated with the salvage.
     * @param scenario        The {@link Scenario} that generated the salvage.
     * @param actualSalvage   The list of units claimed by the player.
     * @param soldSalvage     The list of units that were sold instead of claimed.
     * @param leftoverSalvage The list of units going to the employer or unclaimed.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static void resolveSalvage(Campaign campaign, Mission mission, Scenario scenario,
          List<TestUnit> actualSalvage, List<TestUnit> soldSalvage, List<TestUnit> leftoverSalvage) {
        boolean isContract = mission instanceof Contract;

        // now let's take care of salvage
        for (TestUnit salvageUnit : actualSalvage) {
            ResolveScenarioTracker.UnitStatus salvageStatus = new ResolveScenarioTracker.UnitStatus(salvageUnit);
            if (salvageUnit.getEntity() instanceof Aero) {
                ((Aero) salvageUnit.getEntity()).setFuelTonnage(((Aero) salvageStatus.getBaseEntity()).getFuelTonnage());
            }
            campaign.clearGameData(salvageUnit.getEntity());
            campaign.addTestUnit(salvageUnit);
            // if this is a contract, add to the salvaged value
            if (isContract) {
                ((Contract) mission).addSalvageByUnit(salvageUnit.getSellValue());
            }
        }

        // And any ransomed salvaged units
        Money unitRansoms = Money.zero();
        if (!soldSalvage.isEmpty()) {
            for (TestUnit ransomedUnit : soldSalvage) {
                unitRansoms = unitRansoms.plus(ransomedUnit.getSellValue());
            }

            if (unitRansoms.isGreaterThan(Money.zero())) {
                campaign.getFinances()
                      .credit(TransactionType.SALVAGE,
                            campaign.getLocalDate(),
                            unitRansoms,
                            getFormattedTextAt(RESOURCE_BUNDLE, "CamOpsSalvageUtilities.unitSale", scenario.getName()));
                campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "CamOpsSalvageUtilities.unitSale.report",
                      unitRansoms.toAmountString(), scenario.getHyperlinkedName()));
                if (isContract) {
                    ((Contract) mission).addSalvageByUnit(unitRansoms);
                }
            }
        }

        if (isContract) {
            Money value = Money.zero();
            for (TestUnit salvageUnit : leftoverSalvage) {
                value = value.plus(salvageUnit.getSellValue());
            }
            if (((Contract) mission).isSalvageExchange()) {
                value = value.multipliedBy(((Contract) mission).getSalvagePct()).dividedBy(100);
                campaign.getFinances()
                      .credit(TransactionType.SALVAGE_EXCHANGE,
                            campaign.getLocalDate(),
                            value,
                            getFormattedTextAt(RESOURCE_BUNDLE, "CamOpsSalvageUtilities.exchange", scenario.getName()));
                campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "CamOpsSalvageUtilities.exchange.report",
                      unitRansoms.toAmountString(), scenario.getHyperlinkedName()));
            } else {
                ((Contract) mission).addSalvageByEmployer(value);
            }
        }

        for (TestUnit unit : actualSalvage) {
            unit.setSite(mission.getRepairLocation());
        }
    }
}
