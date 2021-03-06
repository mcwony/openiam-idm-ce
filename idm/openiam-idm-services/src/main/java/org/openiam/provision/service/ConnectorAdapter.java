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
package org.openiam.provision.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.openiam.connector.type.*;
import org.openiam.idm.srvc.mngsys.dto.ManagedSys;
import org.openiam.idm.srvc.mngsys.dto.ProvisionConnector;
import org.openiam.idm.srvc.mngsys.service.ConnectorDataService;
import org.openiam.idm.srvc.recon.dto.ReconciliationConfig;
import org.openiam.spml2.interf.ConnectorService;
import org.openiam.spml2.msg.*;
import org.openiam.spml2.msg.ResponseType;
import org.openiam.spml2.msg.password.ResetPasswordRequestType;
import org.openiam.spml2.msg.password.ResetPasswordResponseType;
import org.openiam.spml2.msg.password.SetPasswordRequestType;
import org.openiam.spml2.msg.suspend.ResumeRequestType;
import org.openiam.spml2.msg.suspend.SuspendRequestType;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;
import java.util.HashMap;
import java.util.Map;

/**
 * Wraps around the connector interface and manages the calls to the varous
 * operations for the connectors for provisioning.
 * 
 * @author suneet
 */
public class ConnectorAdapter {

	protected static final Log log = LogFactory.getLog(ConnectorAdapter.class);

	protected ConnectorDataService connectorService;

	public AddResponseType addRequest(ManagedSys managedSys,
			AddRequestType addReqType, MuleContext muleContext) {
		AddResponseType respType = new AddResponseType();
		respType.setStatus(StatusCodeType.FAILURE);

		try {
			if (managedSys == null) {
				return respType;
			}
			log.debug("ConnectorAdapter:addRequest called. Managed sys ="
					+ managedSys.getManagedSysId());

			ProvisionConnector connector = connectorService
					.getConnector(managedSys.getConnectorId());
			log.info("Connector found for " + connector.getConnectorId());
			if (connector != null
					&& (connector.getServiceUrl() != null && connector
							.getServiceUrl().length() > 0)) {

				MuleMessage msg = getService(connector, addReqType,
						connector.getServiceUrl(), "add", muleContext);

				if (msg.getPayload() instanceof org.mule.transport.NullPayload) {

					log.debug("MuleMessage is null..");
					return respType;

				} else {
					return (AddResponseType) msg.getPayload();

				}

			}
			return respType;

		} catch (Exception e) {
			// log.error(e);
			respType.setError(ErrorCode.OTHER_ERROR);
			respType.addErrorMessage(e.toString());
			return respType;
		}

	}

	public ModifyResponseType modifyRequest(ManagedSys managedSys,
			ModifyRequestType modReqType, MuleContext muleContext) {
		ModifyResponseType respType = new ModifyResponseType();
		respType.setStatus(StatusCodeType.FAILURE);

		try {
			if (managedSys == null) {
				return respType;
			}
			log.debug("ConnectorAdapter:modifyRequest called. Managed sys ="
					+ managedSys.getManagedSysId());

			ProvisionConnector connector = connectorService
					.getConnector(managedSys.getConnectorId());

			log.debug("Connector found for " + connector.getConnectorId());

			if (connector != null
					&& (connector.getServiceUrl() != null && connector
							.getServiceUrl().length() > 0)) {

				// ConnectorService port = getService(connector);
				// port.modify(modReqType);
				MuleMessage msg = getService(connector, modReqType,
						connector.getServiceUrl(), "modify", muleContext);

				if (msg.getPayload() instanceof org.mule.transport.NullPayload) {

					log.debug("MuleMessage is null..");
					return respType;

				} else {
					return (ModifyResponseType) msg.getPayload();

				}

			}
			return respType;
		} catch (Exception e) {
			log.debug("Exception caught in ConnectorAdaptor:modifyRequest");
			log.error(e);

			respType.setError(ErrorCode.OTHER_ERROR);
			respType.addErrorMessage(e.toString());
			return respType;
		}

	}

	public LookupResponseType lookupRequest(ManagedSys managedSys,
			LookupRequestType req, MuleContext muleContext) {
		LookupResponseType resp = new LookupResponseType();
		resp.setStatus(StatusCodeType.FAILURE);

		if (managedSys == null) {
			return null;
		}
		log.debug("ConnectorAdapter:lookupRequest called. Managed sys ="
				+ managedSys.getManagedSysId());

		try {
			ProvisionConnector connector = connectorService
					.getConnector(managedSys.getConnectorId());
			log.info("Connector found for " + connector.getConnectorId());
			if (connector != null
					&& (connector.getServiceUrl() != null && connector
							.getServiceUrl().length() > 0)) {

				MuleMessage msg = getService(connector, req,
						connector.getServiceUrl(), "lookup", muleContext);
				if (msg != null) {
					log.debug("LOOKUP Payload=" + msg.getPayload());
					if (msg.getPayload() != null
							&& msg.getPayload() instanceof LookupResponseType) {

						return (LookupResponseType) msg.getPayload();
					} else {
						log.debug("LOOKUP payload is not an instance of LookupResponseType");
						return resp;
					}
				} else {
					log.debug("MuleMessage is null..");
				}

			}
			return resp;

		} catch (Exception e) {
			log.debug("Exception caught in ConnectorAdaptor:lookupRequest");
			log.error(e);

			resp.setError(ErrorCode.OTHER_ERROR);
			resp.addErrorMessage(e.toString());
			return resp;

		}

    }

    public SearchResponse search(SearchRequest searchRequest, ProvisionConnector connector, MuleContext muleContext) {
        SearchResponse resp = new SearchResponse();
        if (searchRequest == null) {
            resp.setStatus(StatusCodeType.FAILURE);
            resp.setError(ErrorCode.INVALID_CONFIGURATION);
            return resp;
        }
        log.debug("ConnectorAdapter:reconcileRequest called. Resource =" + searchRequest.getSearchQuery());
        try {
            if (connector != null && (connector.getServiceUrl() != null && connector.getServiceUrl().length() > 0)) {
                //Send search to Local Connector to get data (e.g. Active Directory via LDAP)
                MuleMessage msg = getService(connector, searchRequest, connector.getServiceUrl(), "search", muleContext);
                if (msg != null) {
                    log.debug("Test connection Payload=" + msg.getPayload());
                    if (msg.getPayload() != null && msg.getPayload() instanceof org.openiam.connector.type.ResponseType) {
                        resp = (SearchResponse) msg.getPayload();
                        if(resp.getStatus() == StatusCodeType.SUCCESS) {
                            resp.setStatus(StatusCodeType.SUCCESS);
                            return resp;
                        }
                    }
                    resp.setStatus(StatusCodeType.FAILURE);
                    return resp;
                } else {
                    log.debug("MuleMessage is null..");
                }

            }
        } catch (Exception e) {
            log.error(e);
        }
        resp.setStatus(StatusCodeType.FAILURE);
        return resp;
	}

	public ResponseType reconcileResource(ManagedSys managedSys,
			ReconciliationConfig config, MuleContext muleContext) {
		ResponseType type = new ResponseType();
		type.setStatus(StatusCodeType.FAILURE);

		if (config == null) {
			return type;
		}

		log.debug("ConnectorAdapter:reconcile called. Resource ="
				+ config.getResourceId());

		try {
			ProvisionConnector connector = connectorService
					.getConnector(managedSys.getConnectorId());

			log.debug("Connector found for " + connector.getConnectorId());

			if (connector != null
					&& (connector.getServiceUrl() != null && connector
							.getServiceUrl().length() > 0)) {

				MuleMessage msg = getService(connector, config,
						connector.getServiceUrl(), "reconcile", muleContext);

				log.debug("MuleMessage payload=" + msg);

				if (msg != null) {
					return (ResponseType) msg.getPayload();
				} else {
					return type;
				}

			}
			return type;
		} catch (Exception e) {
			log.error(e);

			type.setError(ErrorCode.OTHER_ERROR);
			type.addErrorMessage(e.toString());
			return type;

		}
	}

	public ResponseType deleteRequest(ManagedSys managedSys,
			DeleteRequestType delReqType, MuleContext muleContext) {
		ResponseType type = new ResponseType();
		type.setStatus(StatusCodeType.FAILURE);

		if (managedSys == null) {
			return type;
		}
		log.info("ConnectorAdapter:deleteRequest called. Managed sys ="
				+ managedSys.getManagedSysId());

		try {

			ProvisionConnector connector = connectorService
					.getConnector(managedSys.getConnectorId());
			log.info("Connector found for " + connector.getConnectorId());
			if (connector != null
					&& (connector.getServiceUrl() != null && connector
							.getServiceUrl().length() > 0)) {

				MuleMessage msg = getService(connector, delReqType,
						connector.getServiceUrl(), "delete", muleContext);
				if (msg != null) {
					return (ResponseType) msg.getPayload();
				} else {
					log.debug("MuleMessage is null..");
					return type;

				}

			}
			return type;
		} catch (Exception e) {
			log.error(e);

			type.setError(ErrorCode.OTHER_ERROR);
			type.addErrorMessage(e.toString());
			return type;

		}

	}

	public ResponseType setPasswordRequest(ManagedSys managedSys,
			SetPasswordRequestType request, MuleContext muleContext) {
		ResponseType type = new ResponseType();
		type.setStatus(StatusCodeType.FAILURE);

		if (managedSys == null) {
			return type;
		}
		log.info("ConnectorAdapter:setPasswordRequest called. Managed sys ="
				+ managedSys.getManagedSysId());
		try {

			ProvisionConnector connector = connectorService
					.getConnector(managedSys.getConnectorId());
			log.info("Connector found for " + connector.getConnectorId());
			if (connector != null
					&& (connector.getServiceUrl() != null && connector
							.getServiceUrl().length() > 0)) {

				MuleMessage msg = getService(connector, request,
						connector.getServiceUrl(), "setPassword", muleContext);

				if (msg.getPayload() instanceof org.mule.transport.NullPayload) {
					// payload is null - exception was thrown
					log.debug("Exception payload as a string: "
							+ msg.getExceptionPayload().toString());
					return type;
				} else {
					// valid payload

					log.debug("Message payload found on password change: "
							+ msg.getPayload());

					return (ResponseType) msg.getPayload();

				}

			}
			return type;
		} catch (Exception e) {
			log.error(e);

			type.setError(ErrorCode.OTHER_ERROR);
			type.addErrorMessage(e.toString());
			return type;

		}

	}

	public ResetPasswordResponseType resetPasswordRequest(
			ManagedSys managedSys, ResetPasswordRequestType request,
			MuleContext muleContext) {

		ResetPasswordResponseType type = new ResetPasswordResponseType();
		type.setStatus(StatusCodeType.FAILURE);

		if (managedSys == null) {
			return type;
		}
		log.debug("ConnectorAdapter:resetPasswordRequest called. Managed sys ="
				+ managedSys.getManagedSysId());

		try {
			ProvisionConnector connector = connectorService
					.getConnector(managedSys.getConnectorId());
			log.debug("Connector found for " + connector.getConnectorId());
			if (connector != null
					&& (connector.getServiceUrl() != null && connector
							.getServiceUrl().length() > 0)) {

				MuleMessage msg = getService(connector, request,
						connector.getServiceUrl(), "resetPassword", muleContext);

				if (msg.getPayload() instanceof org.mule.transport.NullPayload) {
					// payload is null - exception was thrown
					log.debug("Exception payload as a string: "
							+ msg.getExceptionPayload().toString());
					return type;
				} else {
					// valid payload

					log.debug("Message payload found on password reset: "
							+ msg.getPayload());

					return (ResetPasswordResponseType) msg.getPayload();
				}

			}
			return type;
		} catch (Exception e) {
			log.error(e);

			type.setError(ErrorCode.OTHER_ERROR);
			type.addErrorMessage(e.toString());
			return type;

		}

	}

	public ResponseType suspendRequest(ManagedSys managedSys,
			SuspendRequestType request, MuleContext muleContext) {

		ResponseType type = new ResponseType();
		type.setStatus(StatusCodeType.FAILURE);

		if (managedSys == null) {
			return type;
		}
		log.debug("ConnectorAdapter:suspendRequest called. Managed sys ="
				+ managedSys.getManagedSysId());

		try {
			ProvisionConnector connector = connectorService
					.getConnector(managedSys.getConnectorId());
			log.debug("Connector found for " + connector.getConnectorId());

			if (connector != null
					&& (connector.getServiceUrl() != null && connector
							.getServiceUrl().length() > 0)) {

				MuleMessage msg = getService(connector, request,
						connector.getServiceUrl(), "suspend", muleContext);
				if (msg != null) {
					return (ResponseType) msg.getPayload();
				} else {
					return type;
				}

			}
			return type;

		} catch (Exception e) {
			log.error(e);

			type.setError(ErrorCode.OTHER_ERROR);
			type.addErrorMessage(e.toString());
			return type;

		}

	}

	public ResponseType resumeRequest(ManagedSys managedSys,
			ResumeRequestType request, MuleContext muleContext) {

		ResponseType type = new ResponseType();
		type.setStatus(StatusCodeType.FAILURE);

		if (managedSys == null) {
			return type;
		}
		log.debug("ConnectorAdapter:resumeRequest called. Managed sys ="
				+ managedSys.getManagedSysId());
		try {
			ProvisionConnector connector = connectorService
					.getConnector(managedSys.getConnectorId());
			log.debug("Connector found for " + connector.getConnectorId());

			if (connector != null
					&& (connector.getServiceUrl() != null && connector
							.getServiceUrl().length() > 0)) {

				MuleMessage msg = getService(connector, request,
						connector.getServiceUrl(), "resume", muleContext);
				if (msg != null) {
					return (ResponseType) msg.getPayload();
				} else {
					return type;
				}

			}
			return type;
		} catch (Exception e) {
			log.error(e);

			type.setError(ErrorCode.OTHER_ERROR);
			type.addErrorMessage(e.toString());
			return type;

		}

	}

	public ResponseType testConnection(ManagedSys managedSys,
			MuleContext muleContext) {

		ResponseType type = new ResponseType();
		type.setStatus(StatusCodeType.FAILURE);

		if (managedSys == null) {
			return type;
		}

		log.debug("ConnectorAdapter:testConnection called. Managed sys ="
				+ managedSys.getManagedSysId());

		try {
			ProvisionConnector connector = connectorService
					.getConnector(managedSys.getConnectorId());

			log.debug("Connector found for " + connector.getConnectorId());

			if (connector != null
					&& (connector.getServiceUrl() != null && connector
							.getServiceUrl().length() > 0)) {

				MuleMessage msg = getService(connector, managedSys,
						connector.getServiceUrl(), "testConnection",
						muleContext);

				log.debug("MuleMessage payload=" + msg);

				if (msg != null) {
					return (ResponseType) msg.getPayload();
				} else {
					return type;
				}

			}
			return type;
		} catch (Exception e) {
			log.error(e);

			type.setError(ErrorCode.OTHER_ERROR);
			type.addErrorMessage(e.toString());
			return type;

		}

	}

	private MuleMessage getService(ProvisionConnector connector,
			Object reqType, String url, String operation,
			MuleContext muleContext) throws MuleException {

		log.debug("getService: calling DynamicEndpoint...");
		// Create a MuleContextFactory

		MuleClient client = new MuleClient(muleContext);

		// Map<?,?> msgPropMap =
		// Collections.singletonMap("serviceName","LDAPConnectorService");
		Map<String, String> msgPropMap = new HashMap<String, String>();
		msgPropMap.put("serviceName", url);

		MuleMessage msg = null;

		log.debug("- Service:: Connector interface- Calling dynamic interface ="
				+ url);
		log.debug("- Service:: Operation=" + operation);

		if (operation.equalsIgnoreCase("add")) {

			msg = client.send("vm://dispatchConnectorMessageAdd",
					(AddRequestType) reqType, msgPropMap);
		}
		if (operation.equalsIgnoreCase("modify")) {

			msg = client.send("vm://dispatchConnectorMessageModify",
					(ModifyRequestType) reqType, msgPropMap);
		}
		if (operation.equalsIgnoreCase("lookup")) {

			msg = client.send("vm://dispatchConnectorMessageLookup",
					(LookupRequestType) reqType, msgPropMap);
		}
		if (operation.equalsIgnoreCase("reconcile")) {

			client.sendAsync("vm://dispatchConnectorMessageReconcile",
					(ReconciliationConfig) reqType, msgPropMap);
		}
		if (operation.equalsIgnoreCase("delete")) {

			msg = client.send("vm://dispatchConnectorMessageDelete",
					(DeleteRequestType) reqType, msgPropMap);
		}
		if (operation.equalsIgnoreCase("setPassword")) {

			msg = client.send("vm://dispatchConnectorMessageSetPassword",
					(SetPasswordRequestType) reqType, msgPropMap);
		}

		if (operation.equalsIgnoreCase("resetPassword")) {

			msg = client.send("vm://dispatchConnectorMessageResetPassword",
					(ResetPasswordRequestType) reqType, msgPropMap);

		}
		if (operation.equalsIgnoreCase("suspend")) {

			msg = client.send("vm://dispatchConnectorMessageSuspend",
					(SuspendRequestType) reqType, msgPropMap);
		}
		if (operation.equalsIgnoreCase("resume")) {

			msg = client.send("vm://dispatchConnectorMessageResume",
					(ResumeRequestType) reqType, msgPropMap);
		}

		if (operation.equalsIgnoreCase("testConnection")) {

			msg = client.send("vm://dispatchConnectorMsgTestConnection",
					(ManagedSys) reqType, msgPropMap);
		}
        if (operation.equalsIgnoreCase("search")) {

            msg = client.send("vm://dispatchConnectorMessageSearch",
                    (SearchRequest) reqType, msgPropMap);
        }
		return msg;

	}

	private ConnectorService getService(ProvisionConnector connector) {
		try {

			QName SERVICE_NAME = new QName(connector.getServiceUrl());
			QName PORT_NAME = new QName(connector.getServiceNameSpace(),
					connector.getServicePort());

			Service service = Service.create(SERVICE_NAME);
			service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING,
					connector.getServiceUrl());

			ConnectorService port = service.getPort(
					new QName(connector.getServiceNameSpace(), connector
							.getServicePort()), ConnectorService.class);
			return port;

		} catch (Exception e) {
			log.error(e);

			return null;
		}

	}

	public ConnectorDataService getConnectorService() {
		return connectorService;
	}

	public void setConnectorService(ConnectorDataService connectorService) {
		this.connectorService = connectorService;
	}

}
