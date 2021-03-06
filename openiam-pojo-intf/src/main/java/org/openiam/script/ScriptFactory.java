/*
 * Copyright 2009, OpenIAM LLC 
 * This file is part of the OpenIAM Identity and Access Management Suite
 *
 *   OpenIAM Identity and Access Management Suite is free software: 
 *   you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License 
 *   version 3 as published by the Free Software Foundation.
 *
 *   OpenIAM is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   Lesser GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OpenIAM.  If not, see <http://www.gnu.org/licenses/>. *
 */

/**
 *
 */
package org.openiam.script;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Creates the script integration object.
 *
 * @author suneet
 */
public class ScriptFactory {

    private static final Log log = LogFactory.getLog(ScriptFactory.class);

    public static ScriptIntegration createModule(String className) throws ClassNotFoundException {
        Class cls = Class.forName(className);
        try {
            return (ScriptIntegration) cls.newInstance();
        } catch (IllegalAccessException ia) {
            log.error(ia.getMessage(), ia);

        } catch (InstantiationException ie) {
            log.error(ie.getMessage(), ie);
        }
        return null;

    }

}
