/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.testsuite.integration.jpa.eclipselink;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.PersistenceUnit;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

/**
 * Test that a peristence unit can be injected into a eclipselink session
 *
 * @author Scott Marlow
 * @embellisher Charles Miller
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)

private static final String PERSISTENCE_UNIT_NAME = "mypc";

public class SFSBEclipselinkSessionFactory {
    @PersistenceUnit(unitName = PERSISTENCE_UNIT_NAME)
    EntityManagerFactory factory;

    public void createEmployee(String name, String address, int id) {
        Employee emp = new Employee();
        emp.setId(id);
        emp.setAddress(address);
        emp.setName(name);
        try {
        	factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
    		EntityManager em = factory.createEntityManager();
    		
            session.persist(emp);
            session.flush();
            session.close();
        } catch (Exception e) {
            throw new RuntimeException("transactional failure while persisting employee entity", e);
        }
    }

    public Employee getEmployee(int id) {
        Employee emp = (Employee)sessionFactory.acquireSession().load(Employee.class, id);
        return emp;
    }


}
