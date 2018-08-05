/*
 * TipsDialog.java
 *
 * Copyright (c) 2018
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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.common.util.EncodeControl;



public class TipsDialog extends JDialog {

    private static final long serialVersionUID = 8517524238015359837L;

    private ResourceBundle resourceMap;
    private static ResourceBundle tipsSuggestions = ResourceBundle.getBundle("mekhq.resources.TipsAndSuggestions", new EncodeControl()); //$NON-NLS-1$
    private static Set<String> keySet = tipsSuggestions.keySet();
    // we can't access sets by index, thus the conversion to a list
    private static ArrayList<String> keyList = new ArrayList<String>(keySet);
    private static int selectedCategoryIndex;

    private JCheckBox tipsCheckbox;
    private JLabel categoryLbl;
    private JComboBox<String> categoryDropdown;
    private JButton previousBtn;
    private JButton nextBtn;
    private JLabel tipLbl;
    private JLabel numericPositionLbl;
    private JButton closeBtn;

    public static boolean showTips = true;

    public TipsDialog (Frame owner) {
        super(owner, true);
        this.setPreferredSize(new Dimension(650,500));
        this.setMinimumSize(new Dimension(650,500));
        initComponents();

        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        resourceMap = ResourceBundle.getBundle("mekhq.resources.TipsDialog", new EncodeControl()); //$NON-NLS-1$

        setTitle(resourceMap.getString("title.text"));

        getContentPane().setLayout(new GridBagLayout());

        tipsCheckbox = new JCheckBox(resourceMap.getString("showTipsStartup.text"));
        tipsCheckbox.setSelected(showTips);
        tipsCheckbox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    showTips = true;
                } else {
                    showTips = false;
                }
            }
        });

        GridBagConstraints gridBag = new GridBagConstraints();
        gridBag.fill = GridBagConstraints.HORIZONTAL;
        gridBag.anchor = GridBagConstraints.NORTHWEST;
        gridBag.gridx = 0;
        gridBag.gridy = 0;
        gridBag.gridwidth = 3;
        gridBag.insets = new Insets(15,15,15,15); //add some spacing for readability
        getContentPane().add(tipsCheckbox, gridBag);

        JPanel categoryPanel = new JPanel();
        categoryLbl = new JLabel(resourceMap.getString("category.text"));
        categoryPanel.add(categoryLbl);

        categoryDropdown = new JComboBox<String>(getAllPossibleCategories().toArray(new String[getAllPossibleCategories().size()]));
        categoryDropdown.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String key = getRandomTipKeyInCategory(categoryDropdown.getSelectedItem().toString());
                    refreshUI(key);
                }
            }
        });
        categoryPanel.add(categoryDropdown);

        gridBag = new GridBagConstraints();
        gridBag.gridx = 0;
        gridBag.gridy = 1;
        gridBag.weighty = 0.2;
        gridBag.anchor = GridBagConstraints.SOUTH;
        gridBag.gridwidth = 3;
        getContentPane().add(categoryPanel, gridBag);

        previousBtn = new JButton("<-");
        previousBtn.setFont(new Font("Arial", Font.PLAIN, 18));
        previousBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                btnClick(false);
            }
        });
        gridBag = new GridBagConstraints();
        gridBag.gridx = 0;
        gridBag.gridy = 2;
        gridBag.weightx = 0.1;
        getContentPane().add(previousBtn, gridBag);

        String key = getRandomTipKey();

        tipLbl = new JLabel();
        tipLbl.setFont(new Font("Arial", Font.PLAIN, 14));
        gridBag = new GridBagConstraints();
        gridBag.fill = GridBagConstraints.BOTH;
        gridBag.gridx = 1;
        gridBag.gridy = 2;
        gridBag.weightx = 0.8;
        gridBag.weighty = 0.6;
        gridBag.insets = new Insets(15,15,15,15); //add some spacing for readability
        getContentPane().add(tipLbl, gridBag);

        nextBtn = new JButton("->");
        nextBtn.setFont(new Font("Arial", Font.PLAIN, 18));
        nextBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                btnClick(true);
            }
        });
        gridBag = new GridBagConstraints();
        gridBag.gridx = 2;
        gridBag.gridy = 2;
        gridBag.weightx = 0.1;
        getContentPane().add(nextBtn, gridBag);

        numericPositionLbl = new JLabel();
        gridBag = new GridBagConstraints();
        gridBag.gridx = 1;
        gridBag.gridy = 3;
        gridBag.weighty = 0.2;
        gridBag.anchor = GridBagConstraints.NORTH;
        getContentPane().add(numericPositionLbl, gridBag);

        closeBtn = new JButton(resourceMap.getString("closeBtn.text"));
        closeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                setVisible(false);
            }
        });
        gridBag = new GridBagConstraints();
        gridBag.gridx = 0;
        gridBag.gridy = 4;
        gridBag.fill = GridBagConstraints.HORIZONTAL;
        gridBag.anchor = GridBagConstraints.PAGE_END;
        gridBag.gridwidth = 3;
        getContentPane().add(closeBtn, gridBag);

        categoryDropdown.setSelectedItem(getCategory(key));
        refreshUI(key);
    }

    public static String getRandomTip() {
        // pick a random tip/suggestion
        String key = getRandomTipKey();
        return getTipString(key);
    }

    private static String getTipString(String key) {
        // JLabels will auto resize the text across multiple lines by adding <html></html> tags
        String tip = "";
        if (key.toLowerCase().contains("atb")) {
            tip = "<html>(AtB): "+tipsSuggestions.getString(key)+"</html>";
        } else {
            tip = "<html>"+tipsSuggestions.getString(key)+"</html>";
        }
        return tip;
    }

    private void btnClick(boolean forward) {
        String key = getKeyByCategoryIndex(selectedCategoryIndex, categoryDropdown.getSelectedItem().toString());
        if (getNumber(key) == 1 && !forward) {
            selectedCategoryIndex = getTotalNumberCategories(key);
        } else if (getNumber(key) == getTotalNumberCategories(key) && forward) {
            selectedCategoryIndex = 1;
        } else if (forward) {
            selectedCategoryIndex = getNumber(key) + 1;
        } else if (!forward) {
            selectedCategoryIndex = getNumber(key) - 1;
        }
        key = getKeyByCategoryIndex(selectedCategoryIndex, getCategory(key));
        refreshUI(key);
    }

    private static String getRandomTipKey() {
        Random r = new Random();
        selectedCategoryIndex = r.nextInt(keyList.size());
        return keyList.get(selectedCategoryIndex);
    }

    private static String getKeyByCategoryIndex(int categoryIndex, String category) {
        for (String key : keyList) {
            if (getNumber(key) == categoryIndex && category.equals(getCategory(key))) {
                return key;
            }
        }
        return null;
    }

    private static String getCategory(String key) {
        Pattern r = Pattern.compile("(?:tips\\.)(.*?)(?:\\.)");
        Matcher m = r.matcher(key);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static int getNumber(String key) {
        Pattern r = Pattern.compile("(?:\\.)(\\d+)(?:\\.text)$");
        Matcher m = r.matcher(key);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    private static int getTotalNumberCategories(String key) {
        String category = getCategory(key);
        int count = 0;
        for (String tip : keyList) {
            if (category.equals(getCategory(tip))) {
                count++;
            }
        }
        return count;
    }

    private static String getRandomTipKeyInCategory(String category) {
        String key = getRandomTipKey();
        while (!getCategory(key).equals(category)) {
            key = getRandomTipKey();
        }
        return key;
    }

    private static ArrayList<String> getAllPossibleCategories() {
        ArrayList<String> categories = new ArrayList<String>();
        for (String tipKey : keyList) {
            if (!categories.contains(getCategory(tipKey))) {
                categories.add(getCategory(tipKey));
            }
        }
        return categories;
    }

    private void refreshUI(String key) {
        int position = getNumber(key);
        int total = getTotalNumberCategories(key);
        numericPositionLbl.setText(Integer.toString(position) + " / " + Integer.toString(total)); //$NON-NLS-1$
        tipLbl.setText(getTipString(key));
        selectedCategoryIndex = position;
    }
}

