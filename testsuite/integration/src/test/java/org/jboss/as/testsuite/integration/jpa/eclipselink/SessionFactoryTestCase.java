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

import static org.junit.Assert.assertTrue;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Eclipselink session factory tests
 *
 *
 * @author Scott Marlow
 * @embellisher Charles Miller
 */
@RunWith(Arquillian.class)
public class SessionFactoryTestCase {

    private static final String ARCHIVE_NAME = "jpa_sessionfactory";

    private static final String persistence_xml =   
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
    "<persistence version=\"1.0\" xmlns=\"http://java.sun.com/xml/ns/persistence\" " +
    	"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
    	"xsi:schemaLocation=\"http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_1_0.xsd\">" +
    	"<persistence-unit name=\"mypc\" transaction-type=\"JTA\">" +
    		"<provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>" +
    		"<jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>" +
    		//<mapping-file>META-INF/orm.xml</mapping-file>
    		//<class>com.xxxxx.cts.wwwwww.ejb.CtsResourceEJB</class>
    		//<class>com.xxxxx.cts.wwwwww.ejb.CtsUserResourcesEJB</class>
    		"<properties>" +
    		//	<!-- <property name="eclipselink.logging.level" value="FINE"/> -->
    		//	<property name="eclipselink.logging.level" value="SEVERE"/>
    			"<property name=\"eclipselink.target-server\" value=\"JBoss\"/>" +
    			"<property name=\"eclipselink.weaving\" value=\"static\"/>" +
    			"<property name="eclipselink.target-database" value="Oracle"/>" +
    			"<property name="eclipselink.jdbc.cache-statements" value="true"/>" +
    			"<property name="eclipselink.jdbc.cache-statements.size" value="0"/>" +
    			"<property name="eclipselink.jdbc.native-sql" value="true" />" +
    			"<property name="eclipselink.jdbc.batch-writing" value="None"/>" +
    			"<property name="eclipselink.jdbc.driver" value="oracle.jdbc.OracleDriver"/>" +
            	"<property name=\"eclipselink.ddl-generation.output-mode\" value=\"database\"/>" +
    		"</properties>" +
        "</persistence-unit>" +
    "</persistence>";
    
    @Deployment
    public static Archive<?> deploy() {

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(SessionFactoryTestCase.class,
            Employee.class,
            SFSB1.class,
            SFSBHibernateSession.class,
            SFSBHibernateSessionFactory.class
        );

        jar.addAsResource(new StringAsset(persistence_xml), "META-INF/persistence.xml");
        return jar;
    }
    
    @ArquillianResource
    private InitialContext iniCtx;

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    protected <T> T rawLookup(String name, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup(name));
    }
    // test that we didn't break the Eclipselink hibernate.session_factory_name (bind eclipselink session factory to
    // specified jndi name) functionality.
    @Test
    public void testEclipselinkSessionFactoryName() throws Exception {
    	
    }
    
    // Test that a Persistence context can be injected into a Eclipselink Session
    @Test
    public void testInjectPCIntoEclipselinkSession() throws Exception {
        SFSBEclipselinkSession sfsbEclipselinkSession = lookup("SFSBEclipselinkSession",SFSBEclipselinkSession.class);
        sfsbEclipselinkSession.createEmployee("Polly", "2 peach way", 2);

        Employee emp = sfsbEclipselinkSession.getEmployee(2);
        assertTrue("name read from eclipselink session is Polly", "Polly".equals(emp.getName()));
    }

    // Test that a Persistence unit can be injected into a Eclipselink Session factory
    @Test
    public void testInjectPUIntoEclipselinkSessionFactory() throws Exception {
        SFSBEclipselinkSessionFactory sfsbEclipselinkSessionFactory =
            lookup("SFSBEclipselinkSessionFactory",SFSBEclipselinkSessionFactory.class);
        sfsbEclipselinkSessionFactory.createEmployee("Elmer", "3 ocean ave", 3);

        Employee emp = sfsbEclipselinkSessionFactory.getEmployee(3);
        assertTrue("name read from eclipselink session is Elmer", "Elmer".equals(emp.getName()));
    }    
}