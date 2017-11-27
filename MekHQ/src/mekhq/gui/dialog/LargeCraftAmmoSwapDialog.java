/*
 * Copyright (c) 2017 - The MegaMek Team
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
package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.client.ui.swing.BayMunitionsChoicePanel;
import megamek.common.Mounted;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.LargeCraftAmmoBin;
import mekhq.campaign.unit.Unit;

/**
 * @author Neoancient
 *
 */
public class LargeCraftAmmoSwapDialog extends JDialog {

    /**
     * 
     */
    private static final long serialVersionUID = -5367208345530677347L;
    
    private final Unit unit;
    private final BayMunitionsChoicePanel mainPanel;
    private boolean canceled = true;
    
    public LargeCraftAmmoSwapDialog(Frame frame, Unit unit) {
        super(frame, true);
        this.unit = unit;
        
        getContentPane().setLayout(new BorderLayout());
        mainPanel = new BayMunitionsChoicePanel(unit.getEntity(), unit.campaign.getGame());
        getContentPane().add(new JScrollPane(mainPanel), BorderLayout.CENTER);
        JPanel panButtons = new JPanel();
        JButton button = new JButton("OK");
        button.addActionListener(ev -> apply());
        panButtons.add(button);
        button = new JButton("Cancel");
        button.addActionListener(ev -> setVisible(false));
        panButtons.add(button);
        getContentPane().add(panButtons, BorderLayout.SOUTH);
        
        pack();
    }
    
    public boolean wasCanceled() {
        return canceled;
    }
    
    private void apply() {
        mainPanel.apply();
        // Save the current number of shots by bay and ammo type
        Map<Mounted, Map<String, Integer>> shotsByBay = new HashMap<>();
        for (Part p : unit.getParts()) {
            if (p instanceof LargeCraftAmmoBin) {
                LargeCraftAmmoBin bin = (LargeCraftAmmoBin) p;
                shotsByBay.putIfAbsent(bin.getBay(), new HashMap<>());
                shotsByBay.get(bin.getBay()).merge(bin.getType().getInternalName(),
                        bin.getFullShots() - bin.getShotsNeeded(),
                        Integer::sum);
            }
        }
        // Rebuild bin parts as necessary
        unit.adjustLargeCraftAmmo();
        // Update the parts and set the number of shots needed based on the current size and the number
        // of shots stored.
        for (Part p : unit.getParts()) {
            if (p instanceof LargeCraftAmmoBin) {
                LargeCraftAmmoBin bin = (LargeCraftAmmoBin) p;
                bin.updateConditionFromEntity(false);
                if (shotsByBay.containsKey(bin.getBay())) {
                    bin.setShotsNeeded(bin.getFullShots()
                            - shotsByBay.get(bin.getBay()).getOrDefault(bin.getType().getInternalName(), 0));
                } else {
                    bin.setShotsNeeded(bin.getFullShots());
                }
                bin.updateConditionFromPart();
            }
        }
        canceled = false;
        setVisible(false);
    }

}
