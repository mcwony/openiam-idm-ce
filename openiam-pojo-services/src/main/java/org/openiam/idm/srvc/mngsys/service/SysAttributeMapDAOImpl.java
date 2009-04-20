package org.openiam.idm.srvc.mngsys.service;

// Generated Nov 3, 2008 12:14:44 AM by Hibernate Tools 3.2.2.GA

import java.util.List;
import javax.naming.InitialContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import static org.hibernate.criterion.Example.create;

import org.openiam.idm.srvc.mngsys.dto.SysAttributeMap;
/**
 * Home object for domain model class SysAttributeMap.
 * @see org.openiam.idm.srvc.SysAttributeMap
 * @author Hibernate Tools
 */
public class SysAttributeMapDAOImpl {

	private static final Log log = LogFactory.getLog(SysAttributeMapDAOImpl.class);

	private final SessionFactory sessionFactory = getSessionFactory();

	protected SessionFactory getSessionFactory() {
		try {
			return (SessionFactory) new InitialContext()
					.lookup("SessionFactory");
		} catch (Exception e) {
			log.error("Could not locate SessionFactory in JNDI", e);
			throw new IllegalStateException(
					"Could not locate SessionFactory in JNDI");
		}
	}

	public void persist(SysAttributeMap transientInstance) {
		log.debug("persisting SysAttributeMap instance");
		try {
			sessionFactory.getCurrentSession().persist(transientInstance);
			log.debug("persist successful");
		} catch (RuntimeException re) {
			log.error("persist failed", re);
			throw re;
		}
	}

	public void attachDirty(SysAttributeMap instance) {
		log.debug("attaching dirty SysAttributeMap instance");
		try {
			sessionFactory.getCurrentSession().saveOrUpdate(instance);
			log.debug("attach successful");
		} catch (RuntimeException re) {
			log.error("attach failed", re);
			throw re;
		}
	}

	public void attachClean(SysAttributeMap instance) {
		log.debug("attaching clean SysAttributeMap instance");
		try {
			sessionFactory.getCurrentSession().lock(instance, LockMode.NONE);
			log.debug("attach successful");
		} catch (RuntimeException re) {
			log.error("attach failed", re);
			throw re;
		}
	}

	public void delete(SysAttributeMap persistentInstance) {
		log.debug("deleting SysAttributeMap instance");
		try {
			sessionFactory.getCurrentSession().delete(persistentInstance);
			log.debug("delete successful");
		} catch (RuntimeException re) {
			log.error("delete failed", re);
			throw re;
		}
	}

	public SysAttributeMap merge(SysAttributeMap detachedInstance) {
		log.debug("merging SysAttributeMap instance");
		try {
			SysAttributeMap result = (SysAttributeMap) sessionFactory
					.getCurrentSession().merge(detachedInstance);
			log.debug("merge successful");
			return result;
		} catch (RuntimeException re) {
			log.error("merge failed", re);
			throw re;
		}
	}

	public SysAttributeMap findById(java.lang.String id) {
		log.debug("getting SysAttributeMap instance with id: " + id);
		try {
			SysAttributeMap instance = (SysAttributeMap) sessionFactory
					.getCurrentSession().get(
							"org.openiam.idm.srvc.SysAttributeMap", id);
			if (instance == null) {
				log.debug("get successful, no instance found");
			} else {
				log.debug("get successful, instance found");
			}
			return instance;
		} catch (RuntimeException re) {
			log.error("get failed", re);
			throw re;
		}
	}

	public List<SysAttributeMap> findByExample(SysAttributeMap instance) {
		log.debug("finding SysAttributeMap instance by example");
		try {
			List<SysAttributeMap> results = (List<SysAttributeMap>) sessionFactory
					.getCurrentSession().createCriteria(
							"org.openiam.idm.srvc.SysAttributeMap").add(
							create(instance)).list();
			log.debug("find by example successful, result size: "
					+ results.size());
			return results;
		} catch (RuntimeException re) {
			log.error("find by example failed", re);
			throw re;
		}
	}
}