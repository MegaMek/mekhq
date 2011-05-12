/*
 * MekBayApp.java
 * 
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

package mekhq;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import megamek.common.MechSummaryCache;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class MekHQApp extends SingleFrameApplication {
	//TODO: This is intended as a debug/production type thing.
	// So it should be backed down to 1 for releases...
	// It's intended for 1 to be critical, 3 to be typical, and 5 to be debug/informational.
	public static int VERBOSITY_LEVEL = 5;
	
	/**
	 * Designed to centralize output and logging.
	 * Purely a pass-through to the version with a log level.
	 * Default to log level 3.
	 * 
	 * @param msg The message you want to log.
	 */
	public static void logMessage(String msg) {
		logMessage(msg, 3);
	}
	
	/**
	 * Designed to centralize output and logging.
	 * 
	 * @param msg The message you want to log.
	 * @param logLevel The log level of the message.
	 */
	public static void logMessage(String msg, int logLevel) {
		if (logLevel <= VERBOSITY_LEVEL)
			System.out.println(msg);
	}

	public static void logError(String err) {
		System.err.println(err);
	}
	
	public static void logError(Exception ex) {
		System.err.println(ex);
		ex.printStackTrace();
	}
    
    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        
        //redirect output to log file
        redirectOutput();
        
        //init the summary cache
        MechSummaryCache.getInstance();
              
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        
        show(new MekHQView(this));
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of MekBayApp
     */
    public static MekHQApp getApplication() {
        return Application.getInstance(MekHQApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(MekHQApp.class, args);
        
    }
    
    /**
     * This function redirects the standard error and output streams to the
     * given File name.
     *
     * @param logFileName The file name to redirect to.
     */
    private static void redirectOutput() {
        try {
            System.out.println("Redirecting output to mekhqlog.txt"); //$NON-NLS-1$
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream("logs" + File.separator + "mekhqlog.txt"), 64));
            System.setOut(ps);
            System.setErr(ps);
        } catch (Exception e) {
            System.err.println("Unable to redirect output to mekhqlog.txt"); //$NON-NLS-1$
            e.printStackTrace();
        }
    }
}
