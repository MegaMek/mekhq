/*
 * MegaMekLab - Copyright (C) 2008
 *
 * Original author - jtighe (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */


package mekhq;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Spring;
import javax.swing.SpringLayout;

/**
 * Helper which loops through container components in order
 * to set up springs. Uses fixed buffers of 4 pixels between
 * components and frame border.
 *
 * @urgru
 */
public class SpringLayoutHelper {

    public static SpringLayoutHelper slh;

    public static void setupSpringGrid(JPanel panel, int columns) {
        int count = panel.getComponentCount();

        int rows = (int)Math.ceil((double)count/(double)columns);

        setupSpringGrid(panel,rows,columns);
    }

    public static void setupSpringGrid(JPanel panel,int rows, int columns) {

        //setup new layout.
        SpringLayout layout = (SpringLayout)panel.getLayout();

        //add padding so that the count matches
        if ( panel.getComponentCount() < (rows*columns) ){
            for ( int x = panel.getComponentCount(); x < (rows*columns); x++ ) {
                panel.add(new JLabel(" "));
            }
        }

        //make all cells in each row same height.
        Spring y = Spring.constant(4);
        for (int r = 0; r < rows; r++) {
            Spring height = Spring.constant(0);
            for (int c = 0; c < columns; c++) {
                height = Spring.max(height,layout.getConstraints(panel.getComponent(r*columns + c)).getHeight());
            }
            for (int c = 0; c < columns; c++) {
                SpringLayout.Constraints constraints = layout.getConstraints(panel.getComponent(r*columns + c));
                constraints.setY(y);
                constraints.setHeight(height);
            }
            y = Spring.sum(y, Spring.sum(height, Spring.constant(4)));
        }

        //make all cells in each column the same width.
        Spring x = Spring.constant(4);
        for (int c = 0; c < columns; c++) {
            Spring width = Spring.constant(0);
            for (int r = 0; r < rows; r++) {
                width = Spring.max(width,layout.getConstraints(panel.getComponent(r*columns + c)).getWidth());
            }
            for (int r = 0; r < rows; r++) {
                SpringLayout.Constraints constraints = layout.getConstraints(panel.getComponent(r*columns + c));
                constraints.setX(x);
                constraints.setWidth(width);
            }
            x = Spring.sum(x, Spring.sum(width, Spring.constant(4)));
        }

        //Set the parent's size.
        SpringLayout.Constraints panelConstraints = layout.getConstraints(panel);
        panelConstraints.setConstraint(SpringLayout.SOUTH, y);
        panelConstraints.setConstraint(SpringLayout.EAST, x);

    }//end setupGrid
}//end SpringLayoutHelper