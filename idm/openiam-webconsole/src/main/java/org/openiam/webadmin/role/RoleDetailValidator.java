package org.openiam.webadmin.role;
/*
 * Copyright 2009, OpenIAM LLC 
 * This file is part of the OpenIAM Identity and Access Management Suite
 *
 *   OpenIAM Identity and Access Management Suite is free software: 
 *   you can redistribute it and/or modify
 *   it under the terms of the Lesser GNU General Public License 
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



import org.openiam.idm.srvc.role.dto.Role;
import org.openiam.idm.srvc.role.dto.RoleId;
import org.openiam.idm.srvc.role.ws.RoleDataWebService;
import org.springframework.validation.Validator;
import org.springframework.validation.Errors;

/**
 * Validation class for the RoleDetailController.
 * @author suneet
 *
 */
public class RoleDetailValidator implements Validator {

	protected RoleDataWebService roleDataService;

	public boolean supports(Class cls) {
		 return RoleDetailCommand.class.equals(cls);
	}

	public void validate(Object cmd, Errors err) {
		RoleDetailCommand command =  (RoleDetailCommand) cmd;
        String mode = command.getFormMode();

        if (!"Re-Synchronize".equalsIgnoreCase(command.getBtn()) && mode != null) {

            if (command.getRole().getRoleName() == null || command.getRole().getRoleName().length() == 0) {
                err.rejectValue("role.roleName", "required");
            }
            if (command.getRole().getId().getRoleId() == null || command.getRole().getId().getRoleId().length() == 0) {
                err.rejectValue("role.id.roleId", "required");
            } else if ("NEW".equalsIgnoreCase(mode)) {
                RoleId id = command.getRole().getId();
                Role rl = roleDataService.getRole(id.getServiceId(), id.getRoleId()).getRole();
                if (rl != null) {
                    err.rejectValue("role.id.roleId", "duplicate");
                }
            }

        }


    }

	public RoleDataWebService getRoleDataService() {
		return roleDataService;
	}

	public void setRoleDataService(RoleDataWebService roleDataService) {
		this.roleDataService = roleDataService;
	}


}
