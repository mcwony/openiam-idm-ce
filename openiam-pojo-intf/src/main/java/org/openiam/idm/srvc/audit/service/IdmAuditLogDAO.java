package org.openiam.idm.srvc.audit.service;

import org.openiam.exception.data.DataException;
import org.openiam.idm.srvc.audit.dto.IdmAuditLog;
import org.openiam.idm.srvc.audit.dto.SearchAudit;

import java.util.Date;
import java.util.List;


/**
 * DAO interface for IdmAudit
 *
 * @author Suneet Shah
 */
public interface IdmAuditLogDAO {

    /**
     * Return an IdmAuditLog object for the id.
     *
     * @param id
     */
    IdmAuditLog findById(java.lang.String id) throws DataException;

    /**
     * Returns the entire audit log.
     *
     * @return
     * @throws DataException
     */
    List<IdmAuditLog> findAll() throws DataException;

    List<IdmAuditLog> findPasswordEvents() throws DataException;

    /**
     * Returns log entries based on the search criteria
     *
     * @param search
     * @return
     * @throws DataException
     */
    List<IdmAuditLog> search(SearchAudit search) throws DataException;

    /**
     * Adds a new IdmAuditLog object to the data log. If successful, the method
     * return an IdmAuditLog object that contains the system generated ID.
     *
     * @param instance
     */
    IdmAuditLog add(IdmAuditLog instance) throws DataException;

    /**
     * Updates an existing log entry
     *
     * @param instance
     */
    void update(IdmAuditLog instance) throws DataException;

    /**
     * Remove an IdmAuditLog object from the audit log.
     *
     * @param instance
     */
    void remove(IdmAuditLog instance) throws DataException;

    List<IdmAuditLog> findEventsAboutUser(String principal, Date startDate, String userId);

    List<IdmAuditLog> findEventsAboutIdentityList(List<String> principal, Date startDate, String userId);
}
