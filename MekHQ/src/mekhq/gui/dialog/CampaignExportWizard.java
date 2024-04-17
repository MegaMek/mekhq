/*
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import megamek.common.AmmoType;
import megamek.common.UnitType;
import mekhq.MekHQ;
import mekhq.NullEntityException;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignFactory;
import mekhq.campaign.Kill;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.unit.Unit;
import mekhq.gui.CampaignGUI;
import mekhq.gui.FileDialogs;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class manages the GUI and logic for the campaign subset export wizard.
 * May Knuth forgive me.
 * @author NickAragua
 */
public class CampaignExportWizard extends JDialog {
    private JList<Force> forceList;
    private JList<Person> personList;
    private JList<Unit> unitList;
    private JList<Part> partList;
    private JList<PartCount> partCountList;

    private JTextField txtPartCount = new JTextField();
    private JButton btnUpdatePartCount = new JButton();

    private JCheckBox chkExportState = new JCheckBox();
    private JCheckBox chkExportContractOffers = new JCheckBox();
    private JCheckBox chkExportCompletedContracts = new JCheckBox();
    private JCheckBox chkDestructiveExport = new JCheckBox();
    private JTextField txtExportMoney = new JTextField();
    private JLabel lblMoney = new JLabel();
    private JLabel lblStatus;

    private Campaign sourceCampaign;

    private Optional<File> destinationCampaignFile;
    private ResourceBundle resourceMap = ResourceBundle.getBundle("mekhq.resources.CampaignExportWizard",
            MekHQ.getMHQOptions().getLocale());

    public enum CampaignExportWizardState {
        ForceSelection,
        PersonSelection,
        UnitSelection,
        PartSelection,
        PartCountSelection,
        MiscellaneousSelection,
        DestinationFileSelection // this should always be last
    }

    public CampaignExportWizard(Campaign c) {
        chkExportState.setText(resourceMap.getString("chkExportSettings.text"));
        chkExportState.setToolTipText(resourceMap.getString("chkExportSettings.tooltip"));
        chkExportContractOffers.setText(resourceMap.getString("chkExportContractOffers.text"));
        chkExportCompletedContracts.setText(resourceMap.getString("chkExportCompletedContracts.text"));
        lblMoney.setText(resourceMap.getString("lblMoney.text"));
        chkDestructiveExport.setText(resourceMap.getString("chkDestructiveExport.text"));

        sourceCampaign = c;
        setupForceList();
        setupPersonList();
        setupUnitList();
        setupPartList();
        chkDestructiveExport.setToolTipText(resourceMap.getString("chkDestructiveExport.tooltip"));
        btnUpdatePartCount.setText(resourceMap.getString("btnUpdatePartCount.text"));
    }

    public void display(CampaignExportWizardState state) {
        getContentPane().removeAll();
        getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel lblInstructions = new JLabel();
        getContentPane().add(lblInstructions, gbc);

        gbc.gridy++;

        lblStatus = new JLabel();
        getContentPane().add(lblStatus, gbc);

        gbc.gridy++;

        JScrollPane scrollPane = new JScrollPane();
        switch (state) {
            case ForceSelection:
                lblInstructions.setText(resourceMap.getString("lblInstructions.ForceSelection.text"));
                scrollPane.setViewportView(forceList);
                getContentPane().add(scrollPane, gbc);
                break;
            case PersonSelection:
                lblInstructions.setText(resourceMap.getString("lblInstructions.PersonSelection.text"));
                lblStatus.setText(getPersonSelectionStatus());
                scrollPane.setViewportView(personList);
                getContentPane().add(scrollPane, gbc);
                break;
            case UnitSelection:
                lblInstructions.setText(resourceMap.getString("lblInstructions.UnitSelection.text"));
                lblStatus.setText(getUnitSelectionStatus());
                scrollPane.setViewportView(unitList);
                getContentPane().add(scrollPane, gbc);
                break;
            case PartSelection:
                lblInstructions.setText(resourceMap.getString("lblInstructions.PartSelection.text"));
                scrollPane.setViewportView(partList);
                getContentPane().add(scrollPane, gbc);
                break;
            case PartCountSelection:
                lblInstructions.setText(resourceMap.getString("lblInstructions.PartCountSelection.text"));
                setupPartCountList();
                scrollPane.setViewportView(partCountList);
                getContentPane().add(scrollPane, gbc);

                gbc.gridx++;
                txtPartCount.setText("0");
                txtPartCount.setColumns(5);
                gbc.insets = new Insets(1, 1, 1, 1);
                getContentPane().add(txtPartCount, gbc);

                gbc.gridx++;
                getContentPane().add(btnUpdatePartCount, gbc);
                gbc.gridx -= 2;

                lblStatus.setText(getPartCountSelectionStatus());
                break;
            case MiscellaneousSelection:
                lblInstructions.setText(resourceMap.getString("lblInstructions.MiscSelection.text"));
                gbc.anchor = GridBagConstraints.WEST;
                getContentPane().add(chkExportState, gbc);
                gbc.gridy++;
                getContentPane().add(chkExportContractOffers, gbc);
                gbc.gridy++;
                getContentPane().add(chkExportCompletedContracts, gbc);
                gbc.gridy++;

                JPanel pnlMoney = new JPanel();
                pnlMoney.setLayout(new GridBagLayout());
                GridBagConstraints mgbc = new GridBagConstraints();
                mgbc.fill = GridBagConstraints.REMAINDER;
                mgbc.insets = new Insets(1, 1, 1, 1);
                mgbc.gridy = 0;
                mgbc.gridx = 0;

                txtExportMoney.setText("0");
                txtExportMoney.setColumns(5);
                pnlMoney.add(txtExportMoney, mgbc);
                mgbc.gridx++;
                pnlMoney.add(lblMoney, mgbc);
                getContentPane().add(pnlMoney, gbc);

                gbc.gridy++;
                getContentPane().add(chkDestructiveExport, gbc);
                break;
            case DestinationFileSelection:
                lblInstructions.setText(resourceMap.getString("lblInstructions.Finalize.text"));
                JButton btnNewCampaign = new JButton(resourceMap.getString("btnNewCampaign.text"));
                btnNewCampaign.addActionListener(e -> {
                    destinationCampaignFile = FileDialogs.saveCampaign(null, sourceCampaign);
                    if (destinationCampaignFile.isPresent()) {
                        if (!exportToCampaign(destinationCampaignFile.get())) {
                            LogManager.getLogger().error("Failed to export campaign to new campaign file");
                        }
                        setVisible(false);
                    }
                });
                getContentPane().add(btnNewCampaign, gbc);
                gbc.gridx++;

                JButton btnExistingCampaign = new JButton(resourceMap.getString("btnExistingCampaign.text"));
                btnExistingCampaign.addActionListener(e -> {
                    destinationCampaignFile = FileDialogs.openCampaign(null);
                    if (destinationCampaignFile.isPresent()) {
                        if (!exportToCampaign(destinationCampaignFile.get())) {
                            LogManager.getLogger().error("Failed to export campaign to existing campaign file");
                        }
                        setVisible(false);
                    }
                });
                getContentPane().add(btnExistingCampaign, gbc);
                gbc.gridx--;
        }

        gbc.gridy++;

        if (state != CampaignExportWizardState.DestinationFileSelection) {
            JButton btnNext = new JButton(resourceMap.getString("btnNext.text"));
            btnNext.addActionListener(e -> nextButtonHandler(state));

            getContentPane().add(btnNext, gbc);
        }

        validate();
        pack();
        setLocationRelativeTo(getParent());
        setModalityType(ModalityType.APPLICATION_MODAL);
        setVisible(true);
    }

    private void setupForceList() {
        forceList = new JList<>();
        DefaultListModel<Force> forceListModel = new DefaultListModel<>();
        for (Force force : sourceCampaign.getAllForces()) {
            forceListModel.addElement(force);
        }
        forceList.setModel(forceListModel);
        forceList.setCellRenderer(new ForceListCellRenderer());
    }

    private void setupPersonList() {
        personList = new JList<>();
        DefaultListModel<Person> personListModel = new DefaultListModel<>();
        List<Person> people = sourceCampaign.getActivePersonnel();
        people.sort(Comparator.comparing(Person::getPrimaryRole));
        for (Person person : people) {
            personListModel.addElement(person);
        }
        personList.setModel(personListModel);
        personList.addListSelectionListener(e -> {
            lblStatus.setText(getPersonSelectionStatus());
            pack();
        });
        personList.setCellRenderer(new PersonListCellRenderer());
    }

    private void setupUnitList() {
        unitList = new JList<>();
        DefaultListModel<Unit> unitListModel = new DefaultListModel<>();
        sourceCampaign.getHangar().forEachUnit(unitListModel::addElement);
        unitList.setModel(unitListModel);
        unitList.addListSelectionListener(e -> {
            lblStatus.setText(getUnitSelectionStatus());
            pack();
        });
        unitList.setCellRenderer(new UnitListCellRenderer());
    }

    private void setupPartList() {
        partList = new JList<>();
        DefaultListModel<Part> partListModel = new DefaultListModel<>();
        List<Part> parts = sourceCampaign.getWarehouse().getSpareParts();
        parts.sort(Comparator.comparing(Part::getName));

        for (Part part : parts) {
            // if the part isn't part of some other activity
            if (!part.isReservedForRefit() && !part.isReservedForReplacement()
                    && !part.isBeingWorkedOn() && part.isPresent() && part.isSpare()) {
                partListModel.addElement(part);
            }
        }
        partList.setModel(partListModel);
    }

    private void setupPartCountList() {
        partCountList = new JList<>();
        partCountList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DefaultListModel<PartCount> partCountListModel = new DefaultListModel<>();
        for (Part part : partList.getSelectedValuesList()) {
            partCountListModel.addElement(new PartCount(part));
        }

        partCountList.setModel(partCountListModel);

        partCountList.addListSelectionListener(e -> txtPartCount.setText(
                Integer.toString(partCountList.getSelectedValue().count)));

        btnUpdatePartCount.addActionListener(e -> {
            try {
                int updatedPartCount = Integer.parseInt(txtPartCount.getText());

                PartCount partCount = partCountList.getSelectedValue();
                if ((updatedPartCount > 0) && (updatedPartCount <= partCount.getMaxPartCount())) {
                    partCountList.getModel().getElementAt(partCountList.getSelectedIndex()).count = updatedPartCount;
                    partCountList.updateUI();
                    lblStatus.setText(getPartCountSelectionStatus());
                    pack();
                }
            } catch (Exception exception) {
                lblStatus.setText(resourceMap.getString("lblStatus.Error.text"));
            }
        });
    }

    /**
     * updates the person list based on changes to the force selection list
     * and unit selection list
     */
    private void updatePersonList() {
        List<Integer> selectedIndices = Arrays.stream(personList.getSelectedIndices())
                .boxed().collect(Collectors.toList());

        for (Force force : forceList.getSelectedValuesList()) {
            for (UUID unitID : force.getAllUnits(false)) {
                Unit unit = sourceCampaign.getUnit(unitID);

                for (Person person : unit.getActiveCrew()) {
                    // this approach recurs throughout the class, and I
                    // couldn't find any better way to select multiple items in a JList
                    personList.setSelectedValue(person, false);
                    selectedIndices.add(personList.getSelectedIndex());
                }

                if (unit.getTech() != null) {
                    personList.setSelectedValue(unit.getTech(), false);
                    selectedIndices.add(personList.getSelectedIndex());
                }
            }

            if (force.getTechID() != null) {
                personList.setSelectedValue(sourceCampaign.getPerson(force.getTechID()), false);
                selectedIndices.add(personList.getSelectedIndex());
            }
        }

        for (Unit unit : unitList.getSelectedValuesList()) {
            for (Person person : unit.getActiveCrew()) {
                personList.setSelectedValue(person, false);
                selectedIndices.add(personList.getSelectedIndex());
            }

            if (unit.getTech() != null) {
                personList.setSelectedValue(unit.getTech(), false);
                selectedIndices.add(personList.getSelectedIndex());
            }
        }

        // somewhat awkward syntax but the person list expects an int array
        // and all we have is a list
        personList.setSelectedIndices(selectedIndices.stream().mapToInt(i->i).toArray());
    }

    /**
     * updates the unit list based on changes to the force selection list
     * and person selection list, without losing existing selections
     */
    private void updateUnitList() {
        List<Integer> selectedIndices = Arrays.stream(unitList.getSelectedIndices())
                .boxed().collect(Collectors.toList());

        for (Force force : forceList.getSelectedValuesList()) {
            for (UUID unitID : force.getAllUnits(false)) {
                Unit unit = sourceCampaign.getUnit(unitID);

                unitList.setSelectedValue(unit, false);
                selectedIndices.add(unitList.getSelectedIndex());
            }
        }

        for (Person person : personList.getSelectedValuesList()) {
            if (person.getUnit() != null) {
                unitList.setSelectedValue(person.getUnit(), false);
                selectedIndices.add(unitList.getSelectedIndex());
            }
        }

        // somewhat awkward syntax but the person list expects an int array
        // and all we have is a list
        unitList.setSelectedIndices(selectedIndices.stream().mapToInt(i->i).toArray());
    }

    private void nextButtonHandler(CampaignExportWizardState state) {
        switch (state) {
            case ForceSelection:
                updatePersonList();
                updateUnitList();
                break;
            case PersonSelection:
                updateUnitList();
                break;
            case UnitSelection:
                updatePersonList();
                break;
        }

        display(CampaignExportWizardState.values()[state.ordinal() + 1]);
    }

    /**
     * Carry out the campaign export.
     * @param file Destination file.
     * @return Whether or not the operation succeeded.
     */
    private boolean exportToCampaign(File file) {
        boolean newCampaign = !file.exists();

        Map<String, SkillType> skillPush = SkillType.getSkillHash();
        Map<String, SpecialAbility> spaPush = SpecialAbility.getSpecialAbilities();

        Campaign destinationCampaign;
        if (newCampaign) {
            destinationCampaign = new Campaign();
            destinationCampaign.setApp(sourceCampaign.getApp());
            destinationCampaign.setCampaignOptions(sourceCampaign.getCampaignOptions());
            destinationCampaign.setGameOptions(sourceCampaign.getGameOptions());
        } else {
            try (FileInputStream fis = new FileInputStream(file)) {
                destinationCampaign = CampaignFactory.newInstance(sourceCampaign.getApp()).createCampaign(fis);
                // Restores all transient attributes from serialized objects
                destinationCampaign.restore();
                destinationCampaign.cleanUp();
            } catch (NullEntityException ex) {
                LogManager.getLogger().error("The following units could not be loaded by the campaign:\n" + ex.getMessage() + "\n\nPlease be sure to copy over any custom units before starting a new version of MekHQ.\nIf you believe the units listed are not customs, then try deleting the file data/mechfiles/units.cache and restarting MekHQ.\nIt is also possible that unit chassi and model names have changed across versions of MegaMek. You can check this by\nopening up MegaMek and searching for the units. Chassis and models can be edited in your MekHQ save file with a text editor.");
                return false;
            } catch (Exception ex) {
                LogManager.getLogger().error("The campaign file could not be loaded.\nPlease check the log file for details.");
                return false;
            } catch (OutOfMemoryError e) {
                LogManager.getLogger().error("MekHQ ran out of memory attempting to load the campaign file. \nTry increasing the memory allocated to MekHQ and reloading.\nSee the FAQ at http://megamek.org for details.");
                return false;
            }
        }

        if (chkExportState.isSelected()) {
            destinationCampaign.setFactionCode(sourceCampaign.getFactionCode());
            destinationCampaign.setCamouflage(sourceCampaign.getCamouflage().clone());
            destinationCampaign.setLocalDate(sourceCampaign.getLocalDate());
            destinationCampaign.setLocation(sourceCampaign.getLocation());
        }

        if (chkExportContractOffers.isSelected()) {
            for (Contract contract : sourceCampaign.getContractMarket().getContracts()) {
                destinationCampaign.getContractMarket().getContracts().add(contract);
            }
        }

        if (chkExportCompletedContracts.isSelected()) {
            for (Mission mission : sourceCampaign.getCompletedMissions()) {
                destinationCampaign.importMission(mission);
            }
        }

        int money = 0;

        try {
            money = Integer.parseInt(txtExportMoney.getText());
            if (money > 0) {
                destinationCampaign.addFunds(TransactionType.STARTING_CAPITAL, Money.of(money),
                        String.format("Transfer from %s", sourceCampaign.getName()));
            }
        } catch (Exception ignored) {

        }

        // forces aren't moved/copied over, we just use the force selection to pre-populate the list of people and units
        // to be exported

        for (Unit unit : unitList.getSelectedValuesList()) {
            int sourceForceID = unit.getForceId();

            if (destinationCampaign.getUnit(unit.getId()) != null) {
                destinationCampaign.removeUnit(unit.getId());
            }

            destinationCampaign.importUnit(unit);

            // Reset any transport assignments, as the export may not contain all transports and cargo units
            unit.setTransportShipAssignment(null);

            if (unit.hasTransportedUnits()) {
                unit.unloadTransportShip();
            }

            // make an attempt to re-construct the force structure in the destination campaign
            // or assign the unit to the same force
            attemptToAssignToForce(unit, sourceForceID, sourceCampaign, destinationCampaign);
        }

        // overwrite any people with the same ID.
        for (Person person : personList.getSelectedValuesList()) {
            if (destinationCampaign.getPerson(person.getId()) != null) {
                destinationCampaign.removePerson(person);
            }

            destinationCampaign.importPerson(person);
            destinationCampaign.getPerson(person.getId()).resetMinutesLeft();

            for (Kill kill : sourceCampaign.getKillsFor(person.getId())) {
                // we don't preserve IDs to avoid conflicts with the destination campaign
                kill.setScenarioId(0);
                kill.setMissionId(0);

                destinationCampaign.importKill(kill);
            }
        }

        destinationCampaign.getHangar().forEachUnit(Unit::resetEngineer);

        // there's just no way to overwrite parts
        // so we simply add them to the destination
        for (int partcIndex = 0; partcIndex < partCountList.getModel().getSize(); partcIndex++) {
            PartCount partCount = partCountList.getModel().getElementAt(partcIndex);

            // make a copy of the part so we don't mess with the existing part
            // ammo and armor require special handling
            Part newPart = partCount.part.clone();
            newPart.setCampaign(destinationCampaign);
            if (newPart instanceof AmmoStorage) {
                ((AmmoStorage) newPart).setShots(partCount.count);
                destinationCampaign.getQuartermaster().addPart(newPart, 0);
            } else if (newPart instanceof Armor) {
                ((Armor) newPart).setAmount(partCount.count);
                destinationCampaign.getQuartermaster().addPart(newPart, 0);
            } else {
                // a work-around due to weirdness in "checkForExistingSparePart" -
                // it comes back as null if the part we're looking for is there but has the same ID,
                // which is likely to happen when exporting to a brand new campaign
                newPart.setId(-1);
                Part existingPart = destinationCampaign.getWarehouse().checkForExistingSparePart(newPart);
                if (existingPart == null) {
                    // addpart doesn't allow adding multiple parts, so we update it afterwards
                    destinationCampaign.getQuartermaster().addPart(newPart, 0);
                    newPart.setQuantity(partCount.count);
                } else {
                    existingPart.setQuantity(existingPart.getQuantity() + partCount.count);
                }
            }
        }

        boolean saved = CampaignGUI.saveCampaign(null, destinationCampaign, file);

        // having saved the destination campaign, we can now get rid of stuff in the source
        // campaign, if we're doing a destructive export
        // don't do it if we failed to save for some reason.
        if (saved && chkDestructiveExport.isSelected()) {
            for (Unit unit : unitList.getSelectedValuesList()) {
                sourceCampaign.removeUnit(unit.getId());
            }

            for (Person person : personList.getSelectedValuesList()) {
                sourceCampaign.removePerson(person, true);
            }

            if (money > 0) {
                sourceCampaign.addFunds(TransactionType.STARTING_CAPITAL, Money.of(-money),
                        "Transfer to exported campaign");
            }

            // here, we update the quantity of the relevant part in the source campaign
            // and remove it if we reach 0. ammo and armor require special handling as usual.
            for (int partcIndex = 0; partcIndex < partCountList.getModel().getSize(); partcIndex++) {
                PartCount partCount = partCountList.getModel().getElementAt(partcIndex);

                if (partCount.part instanceof AmmoStorage) {
                    AmmoStorage sourceAmmo = (AmmoStorage) partCount.part;
                    sourceAmmo.changeShots(-partCount.count);

                    if (sourceAmmo.getShots() <= 0) {
                        sourceCampaign.getWarehouse().removePart(partCount.part);
                    }
                } else if (partCount.part instanceof Armor) {
                    Armor sourceArmor = (Armor) partCount.part;
                    sourceArmor.setAmount(sourceArmor.getAmount() - partCount.count);

                    if (sourceArmor.getAmount() <= 0) {
                        sourceCampaign.getWarehouse().removePart(partCount.part);
                    }
                } else {
                    partCount.part.setQuantity(partCount.part.getQuantity() - partCount.count);

                    if (partCount.part.getQuantity() <= 0) {
                        sourceCampaign.getWarehouse().removePart(partCount.part);
                    }
                }
            }
        }

        // because SkillType and SpecialAbility costs are static, we now "pop" those off the stack so
        // that we don't clobber the original campaign's skill and SPA settings
        SkillType.setSkillTypes(skillPush);
        SpecialAbility.replaceSpecialAbilities(spaPush);

        return saved;
    }

    private void attemptToAssignToForce(Unit unit, int sourceForceID, Campaign sourceCampaign, Campaign destinationCampaign) {
        Force sourceForce = sourceCampaign.getForce(sourceForceID);
        if (sourceForce == null) {
            return;
        }

        // this indicates a unit assigned to the root-level force
        if (sourceForce.getParentForce() == null) {
            destinationCampaign.getForces().addUnit(unit.getId());
        }

        // first thing we will try is to identify a force with the same name and tree structure in the destination campaign
        // if we find one, add the unit to it
        // otherwise, chain-add forces

        // hack: the root forces are irrelevant, so we replace the source root force name with the destination root force name
        String sourceForceFullName = getDestinationFullName(sourceForce, sourceCampaign, destinationCampaign);

        Force destForce = findForce(sourceForceFullName, destinationCampaign.getForces());
        if (destForce != null) {
            destForce.addUnit(unit.getId());
        } else {
            List<Force> parentForces = getForceAndParents(sourceForce);
            Force currentDestinationForce = destinationCampaign.getForces();

            for (int x = parentForces.size() - 1; x >= 0; x--) {
                Force nextSourceForce = parentForces.get(x);
                String nextSourceForceFullName = getDestinationFullName(nextSourceForce, sourceCampaign, destinationCampaign);
                Force nextDestinationForce = findForce(nextSourceForceFullName, currentDestinationForce);

                // if this level doesn't exist yet, add it to where we currently are
                if (nextDestinationForce == null) {
                    Force forceCopy = new Force(nextSourceForce.getName());
                    destinationCampaign.addForce(forceCopy, currentDestinationForce);
                    currentDestinationForce = forceCopy;
                // otherwise, update current location and move to next level
                } else {
                    currentDestinationForce = nextDestinationForce;
                }
            }

            currentDestinationForce.addUnit(unit.getId());
        }
    }

    /**
     * Helper function that returns a full force name with the
     * source campaign root force name swapped out for the destination campaign root force name
     */
    private String getDestinationFullName(Force sourceForce, Campaign sourceCampaign, Campaign destinationCampaign) {
        return sourceForce.getFullName().replace(sourceCampaign.getForces().getName(), destinationCampaign.getForces().getName());
    }

    /**
     * Recurses through a Force structure to look for a force with the given "full force name"
     */
    private Force findForce(String forceName, Force force) {
        if (force.getFullName().equals(forceName)) {
            return force;
        } else {
            for (Force subForce : force.getSubForces()) {
                Force foundForce = findForce(forceName, subForce);

                if (foundForce != null) {
                    return foundForce;
                }
            }

            return null;
        }
    }

    /**
     * Moves through a force's ancestors and returns a flattened list of force names in order
     * from me to furtherst ancestor.
     */
    private List<Force> getForceAndParents(Force force) {
        List<Force> retval = new ArrayList<>();
        retval.add(force);

        Force ancestorForce;
        while (force.getParentForce() != null) {
            ancestorForce = force.getParentForce();

            // we don't want the top-level force
            if (ancestorForce.getParentForce() != null) {
                retval.add(ancestorForce);
            }

            force = ancestorForce;
        }

        return retval;
    }

    private String getPersonSelectionStatus() {
        Map<String, Integer> roleCounts = new HashMap<>();
        for (Person person : personList.getSelectedValuesList()) {
            if (!roleCounts.containsKey(person.getPrimaryRoleDesc())) {
                roleCounts.put(person.getPrimaryRoleDesc(), 0);
            }

            roleCounts.put(person.getPrimaryRoleDesc(), roleCounts.get(person.getPrimaryRoleDesc()) + 1);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html>");

        for (String key : roleCounts.keySet()) {
            sb.append(String.format("%s (%d)<br/>", key, roleCounts.get(key)));
        }

        sb.append("</html>");
        return sb.toString();
    }

    private String getUnitSelectionStatus() {
        Map<Integer, Integer> typeCounts = new HashMap<>();
        for (Unit unit : unitList.getSelectedValuesList()) {
            if (!typeCounts.containsKey(unit.getEntity().getUnitType())) {
                typeCounts.put(unit.getEntity().getUnitType(), 0);
            }

            typeCounts.put(unit.getEntity().getUnitType(), typeCounts.get(unit.getEntity().getUnitType()) + 1);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<html>");

        for (Integer key : typeCounts.keySet()) {
            sb.append(String.format("%s (%d)<br/>", UnitType.getTypeName(key), typeCounts.get(key)));
        }

        sb.append("</html>");
        return sb.toString();
    }

    private String getPartCountSelectionStatus() {
        double totalTonnage = 0;
        for (int partIndex = 0; partIndex < partCountList.getModel().getSize(); partIndex++) {
            PartCount partCount = partCountList.getModel().getElementAt(partIndex);
            totalTonnage += partCount.getCurrentTonnage();
        }

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);
        return String.format("%s tons selected", nf.format(totalTonnage));
    }

    private static class UnitListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            ((JLabel) cmp).setText(((Unit) value).getName());
            return cmp;
        }
    }

    private static class PersonListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Person person = (Person) value;
            String callsign = "";
            if ((person.getCallsign() != null) && !person.getCallsign().isBlank()) {
                callsign = String.format("\"%s\" ", person.getCallsign());
            }

            String cellValue = String.format("%s %s(%s)", person.getFullName(), callsign,
                    person.getPrimaryRoleDesc());
            ((JLabel) cmp).setText(cellValue);
            return cmp;
        }
    }

    private static class ForceListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Force force = (Force) value;
            String cellValue = force.getFullName();
            ((JLabel) cmp).setText(cellValue);
            return cmp;
        }
    }

    private static class PartCount {
        Part part;
        int count;

        public PartCount(Part part) {
            this.part = part;

            if (part instanceof Armor) {
                this.count = ((Armor) part).getAmount();
            } else if (part instanceof AmmoStorage) {
                this.count = ((AmmoStorage) part).getShots();
            } else {
                this.count = part.getQuantity();
            }
        }

        public int getMaxPartCount() {
            if (part instanceof Armor) {
                return ((Armor) part).getAmount();
            } else if (part instanceof AmmoStorage) {
                return ((AmmoStorage) part).getShots();
            } else {
                return part.getQuantity();
            }
        }

        public double getCurrentTonnage() {
            if (part instanceof Armor) {
                return ((Armor) part).getArmorWeight(count);
            } else if (part instanceof AmmoStorage) {
                AmmoStorage ammoPart = (AmmoStorage) part;
                AmmoType ammoType = ammoPart.getType();
                return ammoType.getKgPerShot() * count / 1000.0;
            } else {
                return count * part.getTonnage() * 1.0;
            }
        }

        @Override
        public String toString() {
            return String.format("%s (%d)", part.getPartName(), count);
        }
    }
}
