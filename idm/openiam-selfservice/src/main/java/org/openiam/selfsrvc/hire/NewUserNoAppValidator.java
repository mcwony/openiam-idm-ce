package org.openiam.selfsrvc.hire;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openiam.idm.srvc.user.dto.User;
import org.springframework.validation.Validator;
import org.springframework.validation.Errors;


/**
 * Validator for NewUserNoAppController
 * @author suneet
 *
 */
public class NewUserNoAppValidator implements Validator {

	private static final Log log = LogFactory.getLog(NewUserNoAppCommand.class);
	
	public boolean supports(Class cls) {
		 return NewUserNoAppCommand.class.equals(cls);
	}

	public void validateNewHireForm(Object cmd, Errors err) {
		// TODO Auto-generated method stub
		NewUserNoAppCommand newHireCmd =  (NewUserNoAppCommand) cmd;
		
		
		User user = newHireCmd.getUser();
		log.info("User from form = " + user);
		
	
		
		if (newHireCmd.getUser().getFirstName() == null || newHireCmd.getUser().getFirstName().length() == 0) {
			err.rejectValue("user.firstName", "required");

		}
		if (user.getLastName() == null || user.getLastName().length() == 0) {
			err.rejectValue("user.lastName", "required");
		}	

		if (user.getCompanyId() == null || user.getCompanyId().equalsIgnoreCase("-") ) {
			err.rejectValue("user.companyId", "required");
		}

		if (user.getEmployeeType() == null || user.getEmployeeType().equalsIgnoreCase("-") ) {
			err.rejectValue("user.employeeType", "required");
		}

		if (newHireCmd.getStatus() == null || newHireCmd.getStatus().equalsIgnoreCase("-") ) {
			err.rejectValue("status", "required");
		}		

		if (newHireCmd.getRole() == null || newHireCmd.getRole().equalsIgnoreCase("-") ) {
			err.rejectValue("role", "required");
		}
	
		if (newHireCmd.getSupervisorId() == null || newHireCmd.getSupervisorId().length() == 0) {
			err.rejectValue("supervisorId","required");
		}
		
		
	}

	/* (non-Javadoc)
	 * @see org.springframework.validation.Validator#validate(java.lang.Object, org.springframework.validation.Errors)
	 */
	public void validate(Object cmd, Errors arg1) {
		NewUserNoAppCommand newHireCmd =  (NewUserNoAppCommand) cmd;
		
	}


}
