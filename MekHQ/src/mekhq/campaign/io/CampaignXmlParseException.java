/*
 * Copyright (c) 2018 - The MegaMek Team
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
package mekhq.campaign.io;

/** 
 * Raised when a {@link Campaign} cannot be parsed from XML.
 */
public class CampaignXmlParseException extends Exception {

    private static final long serialVersionUID = -1862554265022111338L;
    
    public CampaignXmlParseException() {
    }

    public CampaignXmlParseException(String message) {
        super(message);
    }

    public CampaignXmlParseException(Throwable e) {
        super(e);
    }

    public CampaignXmlParseException(String message, Throwable e) {
        super(message, e);
    }
}
