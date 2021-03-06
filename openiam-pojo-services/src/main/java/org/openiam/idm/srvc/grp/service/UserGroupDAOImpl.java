package org.openiam.idm.srvc.grp.service;

// Generated Jul 18, 2009 8:49:10 AM by Hibernate Tools 3.2.2.GA

import java.util.List;
import javax.naming.InitialContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openiam.idm.srvc.grp.domain.UserGroupEntity;
import org.openiam.idm.srvc.grp.dto.Group;
import org.openiam.idm.srvc.grp.dto.UserGroup;
import org.openiam.idm.srvc.user.domain.UserEntity;
import org.openiam.idm.srvc.user.dto.User;

import static org.hibernate.criterion.Example.create;

/**
 * Interface for the User-Group DAO which manages the relationship between users
 * and groups.
 * 
 * @see org.openiam.idm.srvc.grp.dto.UserGroup
 * @author Suneet Shah
 */
public class UserGroupDAOImpl implements UserGroupDAO {

	private static final Log log = LogFactory.getLog(UserGroupDAOImpl.class);

	private SessionFactory sessionFactory;

	public void setSessionFactory(SessionFactory session) {
		this.sessionFactory = session;
	}

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openiam.idm.srvc.grp.service.UserGroupDAO#persist(org.openiam.idm
	 * .srvc.grp.dto.UserGroup)
	 */
	public void add(UserGroupEntity transientInstance) {
		log.debug("persisting UserGrp instance");
		try {
			sessionFactory.getCurrentSession().persist(transientInstance);
			log.debug("persist successful");
		} catch (HibernateException re) {
			log.error("persist failed", re);
			throw re;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openiam.idm.srvc.grp.service.UserGroupDAO#delete(org.openiam.idm.
	 * srvc.grp.dto.UserGroup)
	 */
	public void remove(UserGroupEntity persistentInstance) {
		log.debug("deleting UserGrp instance");
		try {
			sessionFactory.getCurrentSession().delete(persistentInstance);
			log.debug("delete successful");
		} catch (HibernateException re) {
			log.error("delete failed", re);
			throw re;
		}
	}

	public List<UserGroupEntity> findUserInGroup(String groupId, String userId) {
		Session session = sessionFactory.getCurrentSession();
		Criteria criteria = session.createCriteria(UserGroupEntity.class)
				.add(Restrictions.eq("user.userId", userId))
				.add(Restrictions.eq("group.grpId", groupId));

		return criteria.list();
	}

	public List<UserEntity> findUserByGroup(String groupId) {
		Session session = sessionFactory.getCurrentSession();
		Criteria criteria = session.createCriteria(UserGroupEntity.class)
				.createAlias("user", "usr")
				.add(Restrictions.eq("group.grpId", groupId))
				.addOrder(Order.asc("usr.lastName"))
				.addOrder(Order.asc("usr.firstName")).setProjection(Projections.property("user"));;

		List<UserEntity> result = (List<UserEntity>) criteria.list();
		if (result == null || result.size() == 0)
			return null;
		return result;
	}

    public List<String> findUsersIdsByGroup(String groupId) {
        Session session = sessionFactory.getCurrentSession();
        Criteria criteria = session.createCriteria(UserGroupEntity.class)
                .createAlias("user", "usr")
                .add(Restrictions.eq("group.grpId", groupId))
                .addOrder(Order.asc("usr.lastName"))
                .addOrder(Order.asc("usr.firstName"))
                .setProjection(Projections.property("usr.userId"));;

        List<String> result = (List<String>) criteria.list();
        if (result == null || result.size() == 0)
            return null;
        return result;
    }

    @Override
    public List<String> findUserIdsByGroup(String groupId) {
        Session session = sessionFactory.getCurrentSession();
        Criteria criteria = session.createCriteria(UserGroupEntity.class)
                .createAlias("user", "usr")
                .add(Restrictions.eq("group.grpId", groupId))
                .addOrder(Order.asc("usr.lastName"))
                .addOrder(Order.asc("usr.firstName"))
                .setProjection(Projections.property("usr.userId"));

        List<String> result = (List<String>) criteria.list();
        if (result == null || result.size() == 0)
            return null;
        return result;
    }

    public void removeUserFromGroup(String grpId, String userId) {
		Session session = sessionFactory.getCurrentSession();
		Query qry = session
				.createQuery("delete org.openiam.idm.srvc.grp.domain.UserGroupEntity ug "
						+ " where ug.group.grpId = :grpId and ug.user.userId = :userId ");
		qry.setString("grpId", grpId);
		qry.setString("userId", userId);
		qry.executeUpdate();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openiam.idm.srvc.grp.service.UserGroupDAO#update(org.openiam.idm.
	 * srvc.grp.dto.UserGroup)
	 */
	public UserGroupEntity update(UserGroupEntity detachedInstance) {
		log.debug("merging UserGrp instance");
		try {
			UserGroupEntity result = (UserGroupEntity) sessionFactory
					.getCurrentSession().merge(detachedInstance);
			log.debug("merge successful");
			return result;
		} catch (HibernateException re) {
			log.error("merge failed", re);
			throw re;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.openiam.idm.srvc.grp.service.UserGroupDAO#findById(java.lang.String)
	 */
	public UserGroupEntity findById(java.lang.String id) {
		log.debug("getting UserGrp instance with id: " + id);
		try {
			UserGroupEntity instance = (UserGroupEntity) sessionFactory
					.getCurrentSession().get(UserGroupEntity.class, id);
			if (instance == null) {
				log.debug("get successful, no instance found");
			} else {
				log.debug("get successful, instance found");
			}
			return instance;
		} catch (HibernateException re) {
			log.error("get failed", re);
			throw re;
		}
	}

}
