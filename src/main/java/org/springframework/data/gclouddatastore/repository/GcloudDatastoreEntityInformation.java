/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.gclouddatastore.repository;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.core.support.AbstractEntityInformation;

public class GcloudDatastoreEntityInformation<T, ID> extends AbstractEntityInformation<T, ID> {
    private static final Logger LOG = LoggerFactory.getLogger(GcloudDatastoreEntityInformation.class);
    private final Field field;

    public GcloudDatastoreEntityInformation(Class<T> domainClass) {
		super(domainClass);
		this.field = reflectField(domainClass);
	}

	private Field reflectField(Class<T> domainClass) {
        LOG.info("domainClass = {}", domainClass);
        Class<?> entityClass = domainClass;
        while (entityClass != Object.class) {
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.getAnnotation(Id.class) != null) {
                    return field;
                }
            }
            entityClass = entityClass.getSuperclass();
        }

        throw new IllegalStateException("id not found on the domain class or any of its superclasses (" + domainClass + ")");
    }

    @SuppressWarnings("unchecked")
	@Override
	public ID getId(T entity) {
        try {
            return (ID) field.get(entity);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(entity);
            return (ID) beanWrapper.getPropertyValue(field.getName());
        }
	}

    @SuppressWarnings("unchecked")
	@Override
	public Class<ID> getIdType() {
        return (Class<ID>) field.getType();
	}
}
