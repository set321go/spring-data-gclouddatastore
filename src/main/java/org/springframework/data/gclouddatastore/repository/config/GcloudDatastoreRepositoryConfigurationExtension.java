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

package org.springframework.data.gclouddatastore.repository.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.data.config.ParsingUtils;
import org.springframework.data.gclouddatastore.GcloudDatastoreRepository;
import org.springframework.data.gclouddatastore.repository.GcloudDatastoreRepositoryFactory;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.Collections;

public class GcloudDatastoreRepositoryConfigurationExtension extends RepositoryConfigurationExtensionSupport {

    /** The reference property to use in xml configuration to specify the template to use with a repository. */
    private static final String DATASTORE_OPTIONS_REF = "datasource-options-ref";
    private static final String DATASTORE_OPTIONS = "datastoreOptions";

    @Override
    public String getRepositoryFactoryBeanClassName() {
        return GcloudDatastoreRepositoryFactory.class.getName();
    }

    @Override
    protected String getModulePrefix() {
        return "gcloudds";
    }

    @Override
    protected Collection<Class<?>> getIdentifyingTypes() {
        return Collections.singleton(GcloudDatastoreRepository.class);
    }

    @Override
    public void postProcess(final BeanDefinitionBuilder builder, final XmlRepositoryConfigurationSource config) {
        Element element = config.getElement();
        ParsingUtils.setPropertyReference(builder, element, DATASTORE_OPTIONS_REF, DATASTORE_OPTIONS);
    }

    @Override
    public void postProcess(final BeanDefinitionBuilder builder, final AnnotationRepositoryConfigurationSource config) {
        builder.addDependsOn(DATASTORE_OPTIONS);
        builder.addPropertyReference(DATASTORE_OPTIONS, DATASTORE_OPTIONS);
    }
}
