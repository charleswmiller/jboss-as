/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.ejb3.component.entity.interceptors;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.component.ComponentView;
import org.jboss.as.ejb3.component.entity.EntityBeanComponent;
import org.jboss.as.ejb3.component.entity.EntityBeanComponentInstance;
import org.jboss.invocation.Interceptor;
import org.jboss.invocation.InterceptorContext;
import org.jboss.invocation.InterceptorFactory;
import org.jboss.invocation.InterceptorFactoryContext;
import org.jboss.msc.value.InjectedValue;

import javax.ejb.ObjectNotFoundException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Interceptor that hooks up finder methods for BMP entity beans
 * <p/>
 * This is a view level interceptor that should be attached to finder methods on the home interface.
 *
 * @author Stuart Douglas
 */
public class EntityBeanHomeFinderInterceptorFactory implements InterceptorFactory {

    private enum ReturnType {
        COLLECTION,
        ENUMERATION,
        SINGLE
    }

    private final Method finderMethod;
    private final ReturnType returnType;
    private final InjectedValue<ComponentView> viewToCreate = new InjectedValue<ComponentView>();

    public EntityBeanHomeFinderInterceptorFactory(final Method finderMethod) {
        this.finderMethod = finderMethod;
        if (Collection.class.isAssignableFrom(finderMethod.getReturnType())) {
            returnType = ReturnType.COLLECTION;
        } else if (Enumeration.class.isAssignableFrom(finderMethod.getReturnType())) {
            returnType = ReturnType.ENUMERATION;
        } else {
            returnType = ReturnType.SINGLE;
        }
    }

    @Override
    public Interceptor create(final InterceptorFactoryContext context) {

        final EntityBeanComponent component = (EntityBeanComponent) context.getContextData().get(Component.class);

        return new Interceptor() {
            @Override
            public Object processInvocation(final InterceptorContext context) throws Exception {

                //grab a bean from the pool to invoke the finder method on
                final EntityBeanComponentInstance instance = component.getPool().get();
                final Object result;
                try {
                    //forward the invocation to the component interceptor chain
                    Method oldMethod = context.getMethod();
                    try {
                        context.putPrivateData(ComponentInstance.class, instance);
                        context.setMethod(finderMethod);
                        context.setTarget(instance.getInstance());
                        result = instance.getInterceptor(finderMethod).processInvocation(context);
                    } finally {
                        context.setMethod(oldMethod);
                        context.setTarget(null);
                        context.putPrivateData(ComponentInstance.class, null);
                    }
                    switch (returnType) {
                        case COLLECTION: {
                            Collection keys = (Collection) result;
                            final Set<Object> results = new HashSet<Object>();
                            if (keys != null) {
                                for (Object key : keys) {
                                    results.add(getLocalObject(key, component));
                                }
                            }
                            return results;
                        }
                        case ENUMERATION: {
                            Enumeration keys = (Enumeration) result;
                            final Set<Object> results = new HashSet<Object>();
                            if (keys != null) {
                                while (keys.hasMoreElements()) {
                                    Object key = keys.nextElement();
                                    results.add(getLocalObject(key, component));
                                }
                            }
                            final Iterator<Object> iterator = results.iterator();
                            return new Enumeration<Object>() {

                                @Override
                                public boolean hasMoreElements() {
                                    return iterator.hasNext();
                                }

                                @Override
                                public Object nextElement() {
                                    return iterator.next();
                                }
                            };
                        }
                        default: {
                            if (result == null) {
                                throw new ObjectNotFoundException("Could not find entity from " + finderMethod + " with params " + Arrays.toString(context.getParameters()));
                            }
                            return getLocalObject(result, component);
                        }
                    }

                } finally {
                    component.getPool().release(instance);
                }
            }

        };
    }

    private Object getLocalObject(final Object result, final EntityBeanComponent component) {
        final HashMap<Object, Object> create = new HashMap<Object, Object>();
        create.put(EntityBeanEjbCreateMethodInterceptorFactory.EXISTING_ID_CONTEXT_KEY, result);
        return viewToCreate.getValue().createInstance(create).createProxy();
    }

    public InjectedValue<ComponentView> getViewToCreate() {
        return viewToCreate;
    }
}
