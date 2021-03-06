package org.openiam.selfsrvc.wrkflow.changeacc;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;


public class ChangeUserRoleValidator implements Validator {

	


	public boolean supports(Class cls) {
		 return ChangeUserRoleCommand.class.equals(cls);
	}

	public void validate(Object cmd, Errors err) {

		ChangeUserRoleCommand identityCommand =  (ChangeUserRoleCommand) cmd;

        boolean selectedItem = false;


        if ( identityCommand.getRoleId() != null && !identityCommand.getRoleId().isEmpty()) {
            selectedItem = true;
        }

        if (!selectedItem) {
            err.rejectValue("roleId", "required");
        }


		
	}


	
}
