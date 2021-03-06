package org.openiam.selfsrvc.usradmin;



import java.util.*;
import java.text.SimpleDateFormat;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.beans.propertyeditors.CustomDateEditor;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openiam.idm.srvc.user.dto.User;
import org.openiam.idm.srvc.user.ws.UserDataWebService;
import org.openiam.idm.srvc.auth.dto.Login;
import org.openiam.idm.srvc.auth.ws.LoginDataWebService;
import org.openiam.idm.srvc.menu.dto.Menu;
import org.openiam.idm.srvc.menu.ws.NavigatorDataWebService;
import org.openiam.provision.dto.PasswordSync;
import org.openiam.provision.service.ProvisionService;
import  org.openiam.selfsrvc.AppConfiguration;


public class ResetUserPasswordController extends SimpleFormController {


	protected UserDataWebService userMgr;	
	protected String redirectView;
	protected AppConfiguration configuration;
	protected LoginDataWebService loginManager;
	protected NavigatorDataWebService navigationDataService;
	protected ProvisionService provRequestService;
	
	private static final Log log = LogFactory.getLog(ResetUserPasswordController.class);

	
	public ResetUserPasswordController() {
		super();
	}
	

	@Override
	protected void initBinder(HttpServletRequest request,
			ServletRequestDataBinder binder) throws Exception {
		
		binder.registerCustomEditor(Date.class, new CustomDateEditor(new SimpleDateFormat("MM/dd/yyyy"),true) );
		
	}

	protected Object formBackingObject(HttpServletRequest request)		throws Exception {
		
		
		
		ResetUserPasswordCommand cmd = new ResetUserPasswordCommand();
		
		String personId = request.getParameter("personId");
		String status = request.getParameter("status");
		
		String userId = (String)request.getSession().getAttribute("userId");
		String principal = (String)request.getSession().getAttribute("login");		
		cmd.setPrincipal(principal);
		
		List<Login> principalList = loginManager.getLoginByUser(personId).getPrincipalList(); 
		for ( Login lg : principalList ) {
			if ( lg.getId().getManagedSysId().equalsIgnoreCase( configuration.getDefaultManagedSysId() )) {
				cmd.setDomainId(lg.getId().getDomainId());
				cmd.setPrincipal(lg.getId().getLogin());
			}
		}
		
		String menuGrp = request.getParameter("menugrp");
		// get the level 3 menu
		
		List<Menu> level3MenuList =  navigationDataService.menuGroupByUser(menuGrp, userId, "en").getMenuList();
		request.setAttribute("menuL3", level3MenuList);	
		request.setAttribute("personId", personId);
		
		User usr = userMgr.getUserWithDependent(personId, false).getUser();
		
		cmd.setFirstName(usr.getFirstName());
		cmd.setLastName(usr.getLastName());
		cmd.setUserStatus(status);
		cmd.setPerId(personId);
		
	

		return cmd;
		
	}	
	

	
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request,
			HttpServletResponse response, Object command, BindException errors) throws Exception {
	
		
		System.out.println("ProfileController: onSubmit");
        Map<String,String> domainMap = new HashMap<String,String>();
		
		ResetUserPasswordCommand cmd =(ResetUserPasswordCommand)command;

        String personId = cmd.getPerId();


        List<Login> principalList = loginManager.getLoginByUser(personId).getPrincipalList();
           for ( Login l : principalList) {
              domainMap.put( l.getId().getDomainId(), null );

           }

		
		// get objects from the command object
		String password = cmd.getPassword();
		
		// update the password in the openiam repository of the primary id
		String managedSysId = configuration.getDefaultManagedSysId();
		String secDomainId = configuration.getDefaultSecurityDomain();

        Set<String> domainKeys = domainMap.keySet();
       for ( String domain  : domainKeys) {

		PasswordSync pswdSync = new PasswordSync();
		pswdSync.setAction("RESET PASSWORD");
		pswdSync.setManagedSystemId(managedSysId);
		pswdSync.setPassword(password);
		pswdSync.setPrincipal(cmd.getPrincipal());
		pswdSync.setRequestorId((String)request.getSession().getAttribute("userId"));
		pswdSync.setSecurityDomain(domain);

        String login = (String)request.getSession().getAttribute("login");
        String reqDomain = (String)request.getSession().getAttribute("domain");

        pswdSync.setRequestClientIP(request.getRemoteHost());
        pswdSync.setRequestorLogin(login);
        pswdSync.setRequestorDomain(reqDomain);

		provRequestService.resetPassword(pswdSync);

       }
		
		
		/*String encPassword = (String)loginManager.encryptPassword(password).getResponseValue();
		loginManager.resetPassword(secDomainId, cmd.getPrincipal(), managedSysId, encPassword);
		*/
				
		return new ModelAndView(new RedirectView(redirectView+"&mode=1", true));
		

	}


	



	public String getRedirectView() {
		return redirectView;
	}


	public void setRedirectView(String redirectView) {
		this.redirectView = redirectView;
	}


	public AppConfiguration getConfiguration() {
		return configuration;
	}


	public void setConfiguration(AppConfiguration configuration) {
		this.configuration = configuration;
	}




	public UserDataWebService getUserMgr() {
		return userMgr;
	}


	public void setUserMgr(UserDataWebService userMgr) {
		this.userMgr = userMgr;
	}


	public NavigatorDataWebService getNavigationDataService() {
		return navigationDataService;
	}


	public void setNavigationDataService(
			NavigatorDataWebService navigationDataService) {
		this.navigationDataService = navigationDataService;
	}


	public LoginDataWebService getLoginManager() {
		return loginManager;
	}


	public void setLoginManager(LoginDataWebService loginManager) {
		this.loginManager = loginManager;
	}


	public ProvisionService getProvRequestService() {
		return provRequestService;
	}


	public void setProvRequestService(ProvisionService provRequestService) {
		this.provRequestService = provRequestService;
	}



	
}
