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
package org.openiam.idm.srvc.synch.srcadapter;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.util.StringUtils;
import org.openiam.base.id.UUIDGen;
import org.openiam.base.ws.Response;
import org.openiam.base.ws.ResponseCode;
import org.openiam.base.ws.ResponseStatus;
import org.openiam.idm.srvc.audit.dto.IdmAuditLog;
import org.openiam.idm.srvc.audit.service.AuditHelper;
import org.openiam.idm.srvc.auth.ws.LoginDataWebService;
import org.openiam.idm.srvc.role.ws.RoleDataWebService;
import org.openiam.idm.srvc.synch.dto.Attribute;
import org.openiam.idm.srvc.synch.dto.LineObject;
import org.openiam.idm.srvc.synch.dto.SyncResponse;
import org.openiam.idm.srvc.synch.dto.SynchConfig;
import org.openiam.idm.srvc.synch.service.MatchObjectRule;
import org.openiam.idm.srvc.synch.service.SourceAdapter;
import org.openiam.idm.srvc.synch.service.TransformScript;
import org.openiam.idm.srvc.synch.service.ValidationScript;
import org.openiam.idm.srvc.user.dto.User;
import org.openiam.idm.srvc.user.dto.UserStatusEnum;
import org.openiam.idm.srvc.user.ws.UserDataWebService;
import org.openiam.provision.dto.ProvisionUser;
import org.openiam.provision.resp.ProvisionUserResponse;
import org.openiam.provision.service.ProvisionService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Scan Ldap for any new records, changed users, or delete operations and then synchronizes them back into OpenIAM.
 *
 * @author suneet
 */
public class LdapAdapter implements SourceAdapter {

    public ApplicationContext ac;

    private AuditHelper auditHelper;
    private MatchRuleFactory matchRuleFactory;

    private static ResourceBundle secres = ResourceBundle.getBundle("securityconf");

    private LdapContext ctx;

    protected UserDataWebService userMgr;

    protected LoginDataWebService loginManager;
    protected RoleDataWebService roleDataService;
    private String systemAccount;
    private static final Log log = LogFactory.getLog(LdapAdapter.class);
    protected MuleContext muleContext;

    private static final ResourceBundle res = ResourceBundle.getBundle("datasource");
    private final long SHUTDOWN_TIME = 5000;

    public SyncResponse startSynch(final SynchConfig config) {

        // String changeLog = null;
        // Date mostRecentRecord = null;
        //    long mostRecentRecord = 0L;
        String lastRecProcessed = null;
        final ProvisionService provService = (ProvisionService) ac.getBean("defaultProvision");

        log.debug("LDAP startSynch CALLED.^^^^^^^^");

        String requestId = UUIDGen.getUUID();

        IdmAuditLog synchStartLog_ = new IdmAuditLog();
        synchStartLog_.setSynchAttributes("SYNCH_USER", config.getSynchConfigId(), "START", "SYSTEM", requestId);
        final IdmAuditLog synchStartLog = auditHelper.logEvent(synchStartLog_);

        try {

            if (!connect(config)) {

                SyncResponse resp = new SyncResponse(ResponseStatus.FAILURE);
                resp.setErrorCode(ResponseCode.FAIL_CONNECTION);
                return resp;
            }
            // get the last execution time
            if (config.getLastRecProcessed() != null) {
                lastRecProcessed = config.getLastRecProcessed();
            }

            // get change log field
            if (config.getSynchType().equalsIgnoreCase("INCREMENTAL")) {
                if (lastRecProcessed != null) {
                    // update the search filter so that it has the new time
                    String ldapFilterQuery = config.getQuery();
                    // replace wildcards with the last exec time

                    config.setQuery(ldapFilterQuery.replace("?", lastRecProcessed));

                    log.debug("Updated ldap filter = " + config.getQuery());
                }
            }

            final ValidationScript validationScript = StringUtils.isNotEmpty(config.getValidationRule()) ? SynchScriptFactory.createValidationScript(config.getValidationRule()) : null;
            final TransformScript transformScript = StringUtils.isNotEmpty(config.getTransformationRule()) ? SynchScriptFactory.createTransformationScript(config.getTransformationRule()) : null;
            // rule used to match object from source system to data in IDM
            final MatchObjectRule matchRule = matchRuleFactory.create(config);
            String[] baseDNArr = config.getBaseDn().contains(";") ? config.getBaseDn().split(";") : new String[]{config.getBaseDn()};
            final List<String> ouList = buildOUList(ctx, baseDNArr);


            log.debug("[LDAPAdapter] count of OU for processing = " + ouList.size() + ";\n  " + ouList);
            for (String ou : ouList) {
                log.debug(ou);
            }


            List<Future> threadResults = new LinkedList<Future>();
            final ExecutorService service = Executors.newCachedThreadPool();
            threadResults.add(service.submit(new Runnable() {
                @Override
                public void run() {

                    for (String s : ouList) {

                         // modify the baseDN to leverage existing code

                        try {
                            config.setBaseDn(s);
                            log.debug("[LDAPAdapter] OU=\"" + s + "\" starting search ...");

                            log.debug("[LDAPAdapter]: getting a new connection for this OU");
                            connect(config);


                            NamingEnumeration<SearchResult> results = search(config);

                            log.debug("[LDAPAdapter]: results found for OU=" + s);

                            List<SearchResult> resultList = new LinkedList<SearchResult>();
                            while (results.hasMoreElements()) {
                                SearchResult searchResult =  results.nextElement();
                                log.debug("SearchResult:"+searchResult.getName());
                                resultList.add(searchResult);
                            }
                            log.debug("Search by OU = \""+config.getBaseDn()+"\" completed."+" Was found: "+resultList.size()+" records.");
                            log.debug("[LDAPAdapter] OU=\"" + s + "\" starting processing ...");
                            proccess(config, provService, synchStartLog, validationScript, transformScript, matchRule, 0, resultList);

                        } catch (Exception ne) {

                            ne.printStackTrace();

                            log.error(ne);

                  //          synchStartLog.updateSynchAttributes("FAIL", ResponseCode.DIRECTORY_NAMING_EXCEPTION.toString(), ne.toString());
                   //         auditHelper.logEvent(synchStartLog);
                        }
                    }


                }
            }));

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    service.shutdown();
                    try {
                        if (!service.awaitTermination(SHUTDOWN_TIME, TimeUnit.MILLISECONDS)) { //optional *
                            log.warn("Executor did not terminate in the specified time."); //optional *
                            List<Runnable> droppedTasks = service.shutdownNow(); //optional **
                            log.warn("Executor was abruptly shut down. " + droppedTasks.size() + " tasks will not be executed."); //optional **
                        }
                    } catch (InterruptedException e) {
                        log.error(e);

                        synchStartLog.updateSynchAttributes("FAIL", ResponseCode.INTERRUPTED_EXCEPTION.toString(), e.toString());
                        auditHelper.logEvent(synchStartLog);

                        SyncResponse resp = new SyncResponse(ResponseStatus.FAILURE);
                        resp.setErrorCode(ResponseCode.INTERRUPTED_EXCEPTION);
                    }
                }
            });
            waitUntilWorkDone(threadResults);


        } catch (ClassNotFoundException cnfe) {

            log.error(cnfe);

            synchStartLog.updateSynchAttributes("FAIL", ResponseCode.CLASS_NOT_FOUND.toString(), cnfe.toString());
            auditHelper.logEvent(synchStartLog);

            SyncResponse resp = new SyncResponse(ResponseStatus.FAILURE);
            resp.setErrorCode(ResponseCode.CLASS_NOT_FOUND);
            return resp;
        } catch (IOException fe) {


            log.error(fe);

            synchStartLog.updateSynchAttributes("FAIL", ResponseCode.FILE_EXCEPTION.toString(), fe.toString());
            auditHelper.logEvent(synchStartLog);


            SyncResponse resp = new SyncResponse(ResponseStatus.FAILURE);
            resp.setErrorCode(ResponseCode.FILE_EXCEPTION);
            resp.setErrorText(fe.toString());
            return resp;


        } catch (InterruptedException e) {
            log.error(e);

            synchStartLog.updateSynchAttributes("FAIL", ResponseCode.INTERRUPTED_EXCEPTION.toString(), e.toString());
            auditHelper.logEvent(synchStartLog);

            SyncResponse resp = new SyncResponse(ResponseStatus.FAILURE);
            resp.setErrorCode(ResponseCode.INTERRUPTED_EXCEPTION);
            return resp;
        } catch (NamingException ne) {


            log.error(ne);

            synchStartLog.updateSynchAttributes("FAIL", ResponseCode.DIRECTORY_NAMING_EXCEPTION.toString(), ne.toString());
            auditHelper.logEvent(synchStartLog);

            SyncResponse resp = new SyncResponse(ResponseStatus.FAILURE);
            resp.setErrorCode(ResponseCode.CLASS_NOT_FOUND);
            resp.setErrorText(ne.toString());
            return resp;
        }
        log.debug("LDAP SYNCHRONIZATION COMPLETE^^^^^^^^");

        SyncResponse resp = new SyncResponse(ResponseStatus.SUCCESS);
        //resp.setLastRecordTime(mostRecentRecord);
        resp.setLastRecProcessed(lastRecProcessed);
        return resp;

    }

    private void proccess(SynchConfig config, ProvisionService provService, IdmAuditLog synchStartLog, ValidationScript validationScript, TransformScript transformScript, MatchObjectRule matchRule, int ctr, List<SearchResult> part) throws NamingException {
        for (SearchResult sr : part) {
            System.out.println("LDAPAdapter processing: "+sr);
            Attributes attrs = sr.getAttributes();

            ProvisionUser pUser = new ProvisionUser();
            LineObject rowObj = new LineObject();

            log.debug("-New Row to Synchronize --" + ctr++);

            if (attrs != null) {
                // try {
                for (NamingEnumeration ae = attrs.getAll(); ae.hasMore(); ) {

                    javax.naming.directory.Attribute attr = (javax.naming.directory.Attribute) ae.next();

                    List<String> valueList = new ArrayList<String>();

                    String key = attr.getID();

                   // log.debug("attribute id=: " + key);


                    for (NamingEnumeration e = attr.getAll(); e.hasMore(); ) {
                        Object o = e.next();
                        if (o.toString() != null) {
                            valueList.add(o.toString());

                        }
                    }
                    if (valueList.size() > 0) {
                        Attribute rowAttr = new Attribute();
                        rowAttr.populateAttribute(key, valueList);
                        rowObj.put(key, rowAttr);
                    } else {

                    }
                }


            }


            LastRecordTime lrt = getRowTime(rowObj);

            log.debug("STarting validation and transformation..");


            // start the synch process
            // 1) Validate the data
            // 2) Transform it
            // 3) if not delete - then match the object and determine if its a new object or its an udpate

            // validate
            if (validationScript != null) {

                int retval = validationScript.isValid(rowObj);
                if (retval == ValidationScript.NOT_VALID) {
                    log.error("Row Object Faied Validation=" + rowObj.toString());
                    // log this object in the exception log

                    continue;
                }
                if (retval == ValidationScript.SKIP) {
                    continue;
                }
            }

            // check if the user exists or not
            Map<String, Attribute> rowAttr = rowObj.getColumnMap();
            //
            User usr = matchRule.lookup(config, rowAttr);


            // transform
            if (transformScript != null) {

                // initialize the transform script
                transformScript.init();

                if (usr != null) {
                    transformScript.setNewUser(false);
                    transformScript.setUser(userMgr.getUserWithDependent(usr.getUserId(), true).getUser());
                    transformScript.setPrincipalList(loginManager.getLoginByUser(usr.getUserId()).getPrincipalList());
                    transformScript.setUserRoleList(roleDataService.getUserRolesAsFlatList(usr.getUserId()).getRoleList());
                } else {
                    transformScript.setNewUser(true);
                }

                int retval = transformScript.execute(rowObj, pUser);

                log.debug("Transform result=" + retval);

                pUser.setSessionId(synchStartLog.getSessionId());


                if (retval == TransformScript.DELETE && usr != null) {
                    log.debug("deleting record - " + usr.getUserId());
                    ProvisionUserResponse userResp = provService.deleteByUserId(new ProvisionUser(usr), UserStatusEnum.DELETED, systemAccount);


                } else {
                    // call synch
                    if (retval != TransformScript.DELETE) {
                        System.out.println("Provisioning user=" + pUser.getLastName());
                        if (usr != null) {
                            log.debug("updating existing user...systemId=" + pUser.getUserId());
                            pUser.setUserId(usr.getUserId());
                            ProvisionUserResponse userResp = provService.modifyUser(pUser);

                        } else {
                            log.debug("adding new user...");
                            pUser.setUserId(null);
                            ProvisionUserResponse userResp = provService.addUser(pUser);


                        }
                    }
                }
            }
            // show the user object


        }
    }

    private void waitUntilWorkDone(List<Future> results) throws InterruptedException {
        boolean success = false;
        while (!success) {
            for (Future future : results) {
                if (!future.isDone()) {
                    success = false;
                    break;
                } else {
                    success = true;
                }
            }
            Thread.sleep(500);
        }
    }

    public Response testConnection(SynchConfig config) {
        try {
            if (connect(config)) {
                closeConnection();
                Response resp = new Response(ResponseStatus.SUCCESS);
                return resp;
            } else {
                Response resp = new Response(ResponseStatus.FAILURE);
                resp.setErrorCode(ResponseCode.FAIL_CONNECTION);
                return resp;
            }
        } catch (NamingException e) {
            e.printStackTrace();
            log.error(e);

            Response resp = new Response(ResponseStatus.FAILURE);
            resp.setErrorCode(ResponseCode.FAIL_CONNECTION);
            resp.setErrorText(e.getMessage());
            return resp;
        }
    }

    private LastRecordTime getRowTime(LineObject rowObj) {
        Attribute atr = rowObj.get("modifyTimestamp");

        if (atr != null && atr.getValue() != null) {
            return getTime(atr);
        }
        atr = rowObj.get("createTimestamp");

        if (atr != null && atr.getValue() != null) {
            return getTime(atr);
        }
        return new LastRecordTime();


    }

    private LastRecordTime getTime(Attribute atr) {
        LastRecordTime lrt = new LastRecordTime();

        String s = atr.getValue();
        int i = s.indexOf("Z");
        if (i == -1) {
            i = s.indexOf("-");
        }
        if (i > 0) {
            lrt.mostRecentRecord = Long.parseLong(s.substring(0, i));
            lrt.generalizedTime = atr.getValue();
            return lrt;

        }
        lrt.mostRecentRecord = Long.parseLong(s);
        lrt.generalizedTime = atr.getValue();
        return lrt;


    }

    private NamingEnumeration<SearchResult> search(SynchConfig config) throws Exception {

        // String attrIds[] = {"1.1", "+", "*"};

        String attrIds[] = {"1.1", "+", "*", "accountUnlockTime", "aci", "aclRights", "aclRightsInfo", "altServer", "attributeTypes", "changeHasReplFixupOp", "changeIsReplFixupOp", "copiedFrom", "copyingFrom", "createTimestamp", "creatorsName", "deletedEntryAttrs", "dITContentRules", "dITStructureRules", "dncomp", "ds-pluginDigest", "ds-pluginSignature", "ds6ruv", "dsKeyedPassword", "entrydn", "entryid", "hasSubordinates", "idmpasswd", "isMemberOf", "ldapSchemas", "ldapSyntaxes", "matchingRules", "matchingRuleUse", "modDNEnabledSuffixes", "modifiersName", "modifyTimestamp", "nameForms", "namingContexts", "nsAccountLock", "nsBackendSuffix", "nscpEntryDN", "nsds5ReplConflict", "nsIdleTimeout", "nsLookThroughLimit", "nsRole", "nsRoleDN", "nsSchemaCSN", "nsSizeLimit", "nsTimeLimit", "nsUniqueId", "numSubordinates", "objectClasses", "parentid", "passwordAllowChangeTime", "passwordExpirationTime", "passwordExpWarned", "passwordHistory", "passwordPolicySubentry", "passwordRetryCount", "pwdAccountLockedTime", "pwdChangedTime", "pwdFailureTime", "pwdGraceUseTime", "pwdHistory", "pwdLastAuthTime", "pwdPolicySubentry", "pwdReset", "replicaIdentifier", "replicationCSN", "retryCountResetTime", "subschemaSubentry", "supportedControl", "supportedExtension", "supportedLDAPVersion", "supportedSASLMechanisms", "supportedSSLCiphers", "targetUniqueId", "vendorName", "vendorVersion"};

        SearchControls searchCtls = new SearchControls();
        //searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchCtls.setSearchScope(SearchControls.ONELEVEL_SCOPE);
        searchCtls.setReturningAttributes(attrIds);

        String searchFilter = config.getQuery();

        NamingEnumeration<SearchResult> searchResult = null;


        searchResult = ctx.search(config.getBaseDn(), searchFilter, searchCtls);
        return searchResult;
    }

    private List<String> buildOUList(LdapContext ctx, String[] baseDNArr) throws NamingException {

        String attrIds[] = {"distinguishedName"};

        List<String> allOUList = new LinkedList<String>();

        SearchControls searchCtls = new SearchControls();
        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchCtls.setReturningAttributes(attrIds);

        String searchFilter = "(&(objectclass=organizationalUnit))";


        for(String baseDN : baseDNArr){
            NamingEnumeration<SearchResult> results = ctx.search(baseDN.trim(), searchFilter, searchCtls);
            List<SearchResult> resultList = Collections.list(results);
            List<String> ouList = new LinkedList<String>();
            for (SearchResult sr : resultList) {
                Attributes attrs = sr.getAttributes();

                if (attrs != null) {

                    for (NamingEnumeration ae = attrs.getAll(); ae.hasMore(); ) {

                        javax.naming.directory.Attribute attr = (javax.naming.directory.Attribute) ae.next();

                        for (NamingEnumeration e = attr.getAll(); e.hasMore(); ) {
                            Object o = e.next();
                            if (o.toString() != null) {
                                ouList.add(o.toString());
                            }
                        }

                    }


                }
            }
            // if we dont have child OUs, then only look at the baseDN
            if (ouList.isEmpty()) {
                ouList.add(baseDN);
            }
            allOUList.addAll(ouList);
        }

        return allOUList;
    }


    private boolean connect(SynchConfig config) throws NamingException {

        Hashtable<String, String> envDC = new Hashtable();
        String keystore = secres.getString("KEYSTORE");
        System.setProperty("javax.net.ssl.trustStore", keystore);

        String hostUrl = config.getSrcHost(); //   managedSys.getHostUrl();
        log.debug("Directory host url:" + hostUrl);


        envDC.put(Context.PROVIDER_URL, hostUrl);
        envDC.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        envDC.put(Context.SECURITY_AUTHENTICATION, "simple"); // simple
        envDC.put(Context.SECURITY_PRINCIPAL, config.getSrcLoginId());  //"administrator@diamelle.local"
        envDC.put(Context.SECURITY_CREDENTIALS, config.getSrcPassword());

        if (hostUrl.contains("ldaps")) {

            envDC.put(Context.SECURITY_PROTOCOL, "SSL");
        }


        ctx = new InitialLdapContext(envDC, null);
        if (ctx != null) {
            return true;
        }

        return false;


    }

    private void closeConnection() {
        try {
            if (ctx != null) {
                ctx.close();
            }

        } catch (NamingException ne) {
            log.error(ne.getMessage(), ne);
            ne.printStackTrace();
        }

    }


    public MatchRuleFactory getMatchRuleFactory() {
        return matchRuleFactory;
    }


    public void setMatchRuleFactory(MatchRuleFactory matchRuleFactory) {
        this.matchRuleFactory = matchRuleFactory;
    }


    public String getSystemAccount() {
        return systemAccount;
    }


    public void setSystemAccount(String systemAccount) {
        this.systemAccount = systemAccount;
    }

    public LoginDataWebService getLoginManager() {
        return loginManager;
    }

    public void setLoginManager(LoginDataWebService loginManager) {
        this.loginManager = loginManager;
    }

    public RoleDataWebService getRoleDataService() {
        return roleDataService;
    }

    public void setRoleDataService(RoleDataWebService roleDataService) {
        this.roleDataService = roleDataService;
    }

    public AuditHelper getAuditHelper() {
        return auditHelper;
    }


    public void setAuditHelper(AuditHelper auditHelper) {
        this.auditHelper = auditHelper;
    }


    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ac = applicationContext;
    }

    public void setMuleContext(MuleContext ctx) {
        muleContext = ctx;
    }

    public UserDataWebService getUserMgr() {
        return userMgr;
    }

    public void setUserMgr(UserDataWebService userMgr) {
        this.userMgr = userMgr;
    }

    private class LastRecordTime {
        long mostRecentRecord;
        String generalizedTime;

    }


}
