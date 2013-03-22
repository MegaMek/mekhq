/*
 * Utilities.java
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Vector;

import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.TechConstants;
import megamek.common.WeaponType;
import megamek.common.options.IOption;
import megamek.common.weapons.BayWeapon;
import megamek.common.weapons.InfantryAttack;
import megamek.common.weapons.infantry.InfantryWeapon;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Utilities {

    public static int roll3d6() {      
        Vector<Integer> rolls = new Vector<Integer>();
        rolls.add(Compute.d6());
        rolls.add(Compute.d6());
        rolls.add(Compute.d6());
        Collections.sort(rolls);
        return (rolls.elementAt(0) + rolls.elementAt(1));       
    }
    
    public static ArrayList<AmmoType> getMunitionsFor(Entity entity, AmmoType cur_atype, int techLvl) {
        ArrayList<AmmoType> atypes = new ArrayList<AmmoType>();
        for(AmmoType atype : AmmoType.getMunitionsFor(cur_atype.getAmmoType())) {
            //this is an abbreviated version of setupMunitions in the CustomMechDialog
            //TODO: clan/IS limitations?
            
            if ((entity instanceof Aero)
                        && !((atype.getAmmoType() == AmmoType.T_MML)
                                || (atype.getAmmoType() == AmmoType.T_ATM)
                                || (atype.getAmmoType() == AmmoType.T_NARC) 
                                || (atype.getAmmoType() == AmmoType.T_AC_LBX))) {
                continue;
            }
            
            int lvl = atype.getTechLevel();
            if(lvl < 0) {
            	lvl = 0;
            }
            if(techLvl < Utilities.getSimpleTechLevel(lvl)) {
            	continue;
            }
            if(TechConstants.isClan(cur_atype.getTechLevel()) != TechConstants.isClan(lvl)) {
            	continue;
            }

            // Only Protos can use Proto-specific ammo
            if (atype.hasFlag(AmmoType.F_PROTOMECH)
                            && !(entity instanceof Protomech)) {
                continue;
            }

            // When dealing with machine guns, Protos can only
            // use proto-specific machine gun ammo
            if ((entity instanceof Protomech)
                            && atype.hasFlag(AmmoType.F_MG)
                            && !atype.hasFlag(AmmoType.F_PROTOMECH)) {
                continue;
            }

            // Battle Armor ammo can't be selected at all.
            // All other ammo types need to match on rack size and tech.
            if ((atype.getRackSize() == cur_atype.getRackSize())
            		&& (atype.hasFlag(AmmoType.F_BATTLEARMOR) == cur_atype.hasFlag(AmmoType.F_BATTLEARMOR))
            		&& (atype.hasFlag(AmmoType.F_ENCUMBERING) == cur_atype.hasFlag(AmmoType.F_ENCUMBERING))
            		&& (atype.getTonnage(entity) == cur_atype.getTonnage(entity))) {
                atypes.add(atype);
            }
        }
        return atypes;
    }

    public static boolean compareMounted (Mounted a, Mounted b) {
        if (!a.getType().equals(b.getType()))
            return false;
        if (!a.getClass().equals(b.getClass()))
            return false;
        if (!a.getName().equals(b.getName()))
            return false;
        if (a.getLocation()!=b.getLocation())
            return false;
        return true;
    }
    

	public static String getCurrencyString(long value) {
		NumberFormat numberFormat = DecimalFormat.getIntegerInstance();
		String text = numberFormat.format(value) + " C-Bills";
		return text;
	}
	
	/**
	 * Returns the last file modified in a directory and all subdirectories
	 * that conforms to a FilenameFilter 
	 * @param dir
	 * @param filter
	 * @return
	 */
	public static File lastFileModified(String dir, FilenameFilter filter) {
        File fl = new File(dir);
        File[] files = fl.listFiles(filter);
        long lastMod = Long.MIN_VALUE;
        File choice = null;
        for (File file : files) {
        	if (file.lastModified() > lastMod) {
        		choice = file;
        		lastMod = file.lastModified();
        	}
        }
        //ok now we need to recursively search any subdirectories, so see if they
        //contain more recent files
        files = fl.listFiles();
        for(File file : files) {
        	if(!file.isDirectory()) {
        		continue;
        	}
        	File subFile =  lastFileModified(file.getPath(), filter);
        	if (null != subFile && subFile.lastModified() > lastMod) {
        		choice = subFile;
        		lastMod = subFile.lastModified();
        	}
        }       
        return choice;
    }
	
	public static ArrayList<String> getAllVariants(Entity en, int year, CampaignOptions options) {
		ArrayList<String> variants = new ArrayList<String>();
		for(MechSummary summary : MechSummaryCache.getInstance().getAllMechs()) {
			if(!summary.isCanon() && options.allowCanonOnly()) {
				continue;
			}
			if(options.limitByYear() && summary.getYear() > year) {
				continue;
			}
			if(options.getTechLevel() < Utilities.getSimpleTechLevel(summary.getType())) {
				continue;
			}
			if(en.getChassis().equalsIgnoreCase(summary.getChassis())
					&& !en.getModel().equalsIgnoreCase(summary.getModel())
					&& summary.getTons() == en.getWeight()) {
				variants.add(summary.getModel());
			}
		}
		return variants;
	}
	
	public static int generateExpLevel(int bonus) {
		int roll = Compute.d6(2) + bonus;
		if(roll < 2) {
			return SkillType.EXP_ULTRA_GREEN;
		}
		if(roll < 6) {
			return SkillType.EXP_GREEN;
		}
		else if(roll < 10) {
			return SkillType.EXP_REGULAR;
		}
		else if(roll < 12) {
			return SkillType.EXP_VETERAN;
		}
		else {
			return SkillType.EXP_ELITE;
		}
	}
	
	public static int rollSpecialAbilities(int bonus) {
		int roll = Compute.d6(2) + bonus;
		if(roll < 10) {
			return 0;
		}
		else if(roll < 12) {
			return 1;
		}
		else {
			return 2;
		}
	}
	
	public static boolean rollProbability(int prob) {
		return Compute.randomInt(100) <= prob;
	}
	
	public static int getAgeByExpLevel(int expLevel) {
		int baseage = 19;
		int ndice = 1;
		switch(expLevel) {
		case(SkillType.EXP_REGULAR):
			ndice = 2;
			break;
		case(SkillType.EXP_VETERAN):
			ndice = 3;
			break;
		case(SkillType.EXP_ELITE):
			ndice = 4;
			break;
		}
		
		int age = baseage;
		while(ndice > 0) {
			int roll = Compute.d6();
			age += roll;
			//reroll all sixes once
			if(roll == 6) {
				age += (Compute.d6()-1);
			}
			ndice--;
		}
		return age;
	}
	
	public static String getOptionDisplayName(IOption option) {
		String name = option.getDisplayableNameWithValue();
		name = name.replaceAll("\\(.+?\\)", "");
		if(option.getType() == IOption.CHOICE) {
			name += " - " + option.getValue();
		}
		return name;
	}
	
	public static String chooseWeaponSpecialization(int type, boolean isClan, int techLvl) {
		ArrayList<String> candidates = new ArrayList<String>();
		for (Enumeration<EquipmentType> e = EquipmentType.getAllTypes(); e.hasMoreElements();) {
            EquipmentType et = e.nextElement();
            if(!(et instanceof WeaponType)) {
            	continue;
            }
            if(et instanceof InfantryWeapon 
            		|| et instanceof BayWeapon
					|| et instanceof InfantryAttack) {
            	continue;
            }
            WeaponType wt = (WeaponType)et;
            if(wt.isCapital() 
            		|| wt.isSubCapital() 
            		|| wt.hasFlag(WeaponType.F_INFANTRY)
            		|| wt.hasFlag(WeaponType.F_ONESHOT)
            		|| wt.hasFlag(WeaponType.F_PROTOTYPE)) {
            	continue;
            }
            if(!((wt.hasFlag(WeaponType.F_MECH_WEAPON) && type == Person.T_MECHWARRIOR) 
            		|| (wt.hasFlag(WeaponType.F_AERO_WEAPON) && type != Person.T_AERO_PILOT)
            		|| (wt.hasFlag(WeaponType.F_TANK_WEAPON) && !(type == Person.T_VEE_GUNNER 
                    		|| type == Person.T_NVEE_DRIVER 
                    		|| type == Person.T_GVEE_DRIVER 
                    		|| type == Person.T_VTOL_PILOT))
                    || (wt.hasFlag(WeaponType.F_BA_WEAPON) && type != Person.T_BA)
                    || (wt.hasFlag(WeaponType.F_PROTO_WEAPON) && type != Person.T_PROTO_PILOT))) {
            	continue;
            }
            if(wt.getAtClass() == WeaponType.CLASS_NONE ||
            		wt.getAtClass() == WeaponType.CLASS_POINT_DEFENSE ||
            		wt.getAtClass() >= WeaponType.CLASS_CAPITAL_LASER) {
            	continue;
            }
            if(TechConstants.isClan(wt.getTechLevel()) != isClan) {
            	continue;
            }
            int lvl = wt.getTechLevel();
            if(lvl < 0) {
            	continue;
            }
            if(techLvl < Utilities.getSimpleTechLevel(lvl)) {
            	continue;
            }          
            if(techLvl == TechConstants.T_IS_UNOFFICIAL) {
            	continue;
            }
            int ntimes = 10;
            if(techLvl >= TechConstants.T_IS_ADVANCED) {
            	ntimes = 1;
            }
            while(ntimes > 0) {
            	candidates.add(et.getName());
            	ntimes--;
            }
		}
		if(candidates.isEmpty()) {
			return "??";
		}
		return candidates.get(Compute.randomInt(candidates.size()));
	}
	
	public static String printIntegerArray(int[] array) {
		String values = "";
		for(int i = 0; i < array.length; i++) {
			values += Integer.toString(array[i]);
			if(i < (array.length-1)) {
				values += ",";
			}
		}
		return values;
	}
	
	public static String printBooleanArray(boolean[] array) {
		String values = "";
		for(int i = 0; i < array.length; i++) {
			values += Boolean.toString(array[i]);
			if(i < (array.length-1)) {
				values += ",";
			}
		}
		return values;
	}
	
	public static int getSimpleTechLevel(int level) {
	    switch(level) {
	    case TechConstants.T_ALLOWED_ALL:
	    case TechConstants.T_INTRO_BOXSET:
	        return CampaignOptions.TECH_INTRO;
	    case TechConstants.T_IS_TW_NON_BOX:
	    case TechConstants.T_CLAN_TW:
	    case TechConstants.T_IS_TW_ALL:
        case TechConstants.T_TW_ALL:
            return CampaignOptions.TECH_STANDARD;
        case TechConstants.T_IS_ADVANCED:
        case TechConstants.T_CLAN_ADVANCED:
            return CampaignOptions.TECH_ADVANCED;
        case TechConstants.T_IS_EXPERIMENTAL:
        case TechConstants.T_CLAN_EXPERIMENTAL:
            return CampaignOptions.TECH_EXPERIMENTAL;
        case TechConstants.T_IS_UNOFFICIAL:
        case TechConstants.T_CLAN_UNOFFICIAL:
            return CampaignOptions.TECH_UNOFFICIAL;
        default:
            return CampaignOptions.TECH_INTRO;
	    }
	}
	
	//copied from http://www.roseindia.net/java/beginners/copyfile.shtml
	public static void copyfile(File inFile, File outFile){
		try{
			InputStream in = new FileInputStream(inFile);
		  
			//For Append the file.
			//  OutputStream out = new FileOutputStream(f2,true);

			//For Overwrite the file.
			OutputStream out = new FileOutputStream(outFile);

			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0){
				out.write(buf, 0, len);
			}
			in.close();
			out.close();
			System.out.println("File copied.");
		}
		catch(FileNotFoundException ex){
			System.out.println(ex.getMessage() + " in the specified directory.");
		}
		catch(IOException e){
			System.out.println(e.getMessage());  
		}
	}
	
	public static void unscrambleEquipmentNumbers(Unit unit) {
        ArrayList<Integer> equipNums = new ArrayList<Integer>();
        for(Mounted m : unit.getEntity().getEquipment()) {
            equipNums.add(unit.getEntity().getEquipmentNum(m));
        }
        for(Part part : unit.getParts()) {
            if(part instanceof AmmoBin) {
                AmmoBin bin = (AmmoBin)part;
                int i = -1;
                boolean found = false;
                for(int equipNum : equipNums) {
                    i++;
                    Mounted m = unit.getEntity().getEquipment(equipNum);
                    if(!(m.getType() instanceof AmmoType)) {
                        continue;
                    }
                    if(m.getType().getInternalName().equals(bin.getType().getInternalName())
                            && ((AmmoType)m.getType()).getMunitionType() == bin.getMunitionType()
                            && !m.isDestroyed()) {
                        bin.setEquipmentNum(equipNum);
                        found = true;
                        break;
                    }
                }
                if(found) {
                    equipNums.remove(i);
                }
            }
            else if(part instanceof EquipmentPart) {
                EquipmentPart epart = (EquipmentPart)part;
                int i = -1;
                boolean found = false;
                for(int equipNum : equipNums) {
                    i++;
                    Mounted m = unit.getEntity().getEquipment(equipNum);
                    if(m.getType() instanceof AmmoType) {
                        continue;
                    }
                    if(m.getType().getInternalName().equals(epart.getType().getInternalName())
                            && !m.isDestroyed()) {
                        epart.setEquipmentNum(equipNum);
                        found = true;
                        break;
                    }
                }
                if(found) {
                    equipNums.remove(i);
                }
            }
            else if(part instanceof MissingEquipmentPart) {
                MissingEquipmentPart epart = (MissingEquipmentPart)part;
                int i = -1;
                boolean found = false;
                for(int equipNum : equipNums) {
                    i++;
                    Mounted m = unit.getEntity().getEquipment(equipNum);
                    if(m.getType().getInternalName().equals(epart.getType().getInternalName())
                            && m.isDestroyed()) {
                        epart.setEquipmentNum(equipNum);
                        found = true;
                        break;
                    }
                }
                if(found) {
                    equipNums.remove(i);
                }
            }
        }
    }
	
	public static int getDaysBetween(Date date1, Date date2) {
	    return (int) ((date2.getTime() - date1.getTime())/ (1000 * 60 * 60 * 24) );
	}
	
	public static int getDiffYears(Date date, GregorianCalendar b) {
	    GregorianCalendar a = new GregorianCalendar();
	    a.setTime(date);
	    int diff = b.get(GregorianCalendar.YEAR) - a.get(GregorianCalendar.YEAR);
	    if (a.get(GregorianCalendar.MONTH) > b.get(GregorianCalendar.MONTH) || 
	        (a.get(GregorianCalendar.MONTH) == b.get(GregorianCalendar.MONTH) && a.get(GregorianCalendar.DATE) > b.get(GregorianCalendar.DATE))) {
	        diff--;
	    }
	    return diff;
	}
}