//  Accounts that are in any status (besides "Terminated") must be terminated on the Last date.


System.out.println("terminateOnExpiration.groovy");


import org.openiam.idm.groovy.helper.ServiceHelper
import org.openiam.provision.service.ProvisionService;
import org.openiam.idm.srvc.user.ws.UserDataWebService;
import java.util.Calendar
import org.openiam.idm.srvc.user.dto.UserSearch
import org.openiam.idm.srvc.user.dto.UserStatusEnum
import org.openiam.provision.dto.ProvisionUser
import org.openiam.idm.srvc.msg.dto.NotificationRequest


def UserDataWebService userService = ServiceHelper.userService()
ProvisionService provision = ServiceHelper.povisionService()


UserSearch search = new UserSearch();
	  
Calendar cal = Calendar.getInstance();
cal.setTime(new Date(System.currentTimeMillis()));
		
println("create date=" + cal.getTime() )
		
search.lastDate = cal.getTime()
userList = userService.search(search).userList;

 println("Userlist = " + userList)
    
if (userList != null ) {
	for ( user in userList) {	
	
	System.out.println("User status=" + user.status)

	System.out.println("User id=" + user.userId)
			
		if (user.status == UserStatusEnum.TERMINATE ) {
			System.out.println("user status is already set to terminate")
		}else {
			u = userService.getUserWithDependent(user.userId, true).user
			
			ProvisionUser pUser = new ProvisionUser(u);
			pUser.status = UserStatusEnum.TERMINATE
			pUser.secondaryStatus = null
			pUser.lastUpdatedBy = "3000"
			pUser.lastDate = search.lastDate
			
			pUser.requestorLogin ="IDM_SERVER";
      		pUser.requestorDomain = "IDM";
        
			//provision.modifyUser(pUser)
			provision.deleteByUserId(
            pUser,
            UserStatusEnum.TERMINATE, "IDM_SERVER");
			
			
		}
	}	 
}

output=1