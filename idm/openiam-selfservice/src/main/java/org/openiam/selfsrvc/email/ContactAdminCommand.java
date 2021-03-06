package org.openiam.selfsrvc.email;

import java.io.Serializable;

import org.openiam.idm.srvc.menu.dto.Menu;

/**
 * Command object for the ContactAdmin
 * @author suneet
 *
 */
public class ContactAdminCommand implements Serializable {
	 

	private static final long serialVersionUID = 8810596891009063762L;

	private String from;
	private String subject;
	private String message;
	
	
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	


	

}
