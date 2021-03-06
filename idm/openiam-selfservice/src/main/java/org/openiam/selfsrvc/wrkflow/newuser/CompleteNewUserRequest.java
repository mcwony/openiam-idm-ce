package org.openiam.selfsrvc.wrkflow.newuser;

import org.openiam.base.ws.ResponseStatus;
import org.openiam.idm.srvc.auth.dto.Login;
import org.openiam.idm.srvc.auth.ws.LoginResponse;
import org.openiam.idm.srvc.msg.service.MailTemplateParameters;
import org.openiam.idm.srvc.msg.ws.NotificationRequest;
import org.openiam.idm.srvc.prov.request.dto.ProvisionRequest;
import org.openiam.idm.srvc.prov.request.dto.RequestUser;
import org.openiam.idm.srvc.user.dto.User;
import org.openiam.idm.srvc.user.dto.UserStatusEnum;
import org.openiam.provision.dto.ProvisionUser;
import org.openiam.provision.resp.ProvisionUserResponse;
import org.openiam.selfsrvc.wrkflow.AbstractCompleteRequest;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 * Class that completes a request for self registration
 * Created with IntelliJ IDEA.
 * User: suneetshah
 * Date: 11/25/12
 * Time: 4:18 PM
 */
public class CompleteNewUserRequest extends AbstractCompleteRequest {

    public static final String REQUEST_APPROVED_NOTIFICATION = "REQUEST_APPROVED";
    public static final String REQUEST_REJECTED_NOTIFICATION = "REQUEST_REJECTED";

    public void approveRequest(ProvisionUser pUser, ProvisionRequest req, String approverUserId ) {

        pUser.getUser().setStatus(UserStatusEnum.ACTIVE);
        pUser.getUser().setUserId(null);
        pUser.setStatus(UserStatusEnum.ACTIVE);

        ProvisionUserResponse resp = provisionService.addUser(pUser);

        approve(resp.getUser(),req, approverUserId);

    }

    public void rejectRequest(ProvisionUser pUser, ProvisionRequest req, String approverUserId) {

        reject(pUser,req, approverUserId);

    }

    public void notifyRequestorApproval(ProvisionRequest req, String approverUserId, User newUser, String notifyUserId) {

        // requestor information
        String userId = req.getRequestorId();
        String identity = null;
        String password = null;


        User approver = userManager.getUserWithDependent(approverUserId, false).getUser();

        // get the target user
        String targetUserName = null;
        Set<RequestUser> reqUserSet = req.getRequestUsers();
        if (reqUserSet != null && !reqUserSet.isEmpty()) {
            Iterator<RequestUser> userIt = reqUserSet.iterator();
            if (userIt.hasNext()) {
                RequestUser targetUser = userIt.next();
                targetUserName = targetUser.getFirstName() + " " + targetUser.getLastName();
            }
        }

        LoginResponse lgResponse = loginManager.getPrimaryIdentity(newUser.getUserId());
        if (lgResponse.getStatus() == ResponseStatus.SUCCESS) {
            Login l = lgResponse.getPrincipal();
            identity = l.getId().getLogin();
            password = (String) loginManager.decryptPassword(l.getPassword()).getResponseValue();
        }

        HashMap<String,String> mailParameters = new HashMap<String, String>();
        mailParameters.put(MailTemplateParameters.USER_ID.value(), notifyUserId);
        mailParameters.put(MailTemplateParameters.REQUEST_ID.value(), req.getRequestTitle());
        mailParameters.put(MailTemplateParameters.REQUESTER.value(), approver.getFirstName() + " " + approver.getLastName());
        mailParameters.put(MailTemplateParameters.TARGET_USER.value(), targetUserName);
        mailParameters.put(MailTemplateParameters.IDENTITY.value(), identity);
        mailParameters.put(MailTemplateParameters.PASSWORD.value(), password);
        mailParameters.put(MailTemplateParameters.REQUEST_REASON.value(), req.getRequestTitle());
        mailService.sendNotificationRequest(new NotificationRequest(REQUEST_APPROVED_NOTIFICATION,mailParameters));
    }

    public void notifyRequestorReject(ProvisionRequest req, String approverUserId, String notifyUserId, String notifyEmail) {


        String userId = req.getRequestorId();

        User approver = userManager.getUserWithDependent(approverUserId, false).getUser();

        // get the target user
        String targetUserName = null;
        Set<RequestUser> reqUserSet = req.getRequestUsers();
        if (reqUserSet != null && !reqUserSet.isEmpty()) {
            Iterator<RequestUser> userIt = reqUserSet.iterator();
            if (userIt.hasNext()) {
                RequestUser targetUser = userIt.next();
                targetUserName = targetUser.getFirstName() + " " + targetUser.getLastName();

            }

        }

        HashMap<String,String> mailParameters = new HashMap<String, String>();
        mailParameters.put(MailTemplateParameters.USER_ID.value(), notifyUserId);
        mailParameters.put(MailTemplateParameters.TO.value(), notifyEmail);
        mailParameters.put(MailTemplateParameters.REQUEST_ID.value(), req.getRequestTitle());
        mailParameters.put(MailTemplateParameters.REQUESTER.value(), approver.getFirstName() + " " + approver.getLastName());
        mailParameters.put(MailTemplateParameters.TARGET_USER.value(), targetUserName);
        mailParameters.put(MailTemplateParameters.REQUEST_REASON.value(), req.getRequestTitle());

        mailService.sendNotificationRequest(new NotificationRequest(REQUEST_REJECTED_NOTIFICATION,mailParameters));

    }
}
