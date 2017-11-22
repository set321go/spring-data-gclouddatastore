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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Optional;

import com.google.cloud.datastore.DatastoreOptions;

import org.springframework.data.gclouddatastore.repository.query.CollectionQuery;
import org.springframework.data.gclouddatastore.repository.query.EntityQuery;
import org.springframework.data.gclouddatastore.repository.query.StreamQuery;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

public class GcloudDatastoreRepositoryFactory extends RepositoryFactorySupport {

	private DatastoreOptions datastoreOptions;

	//FIXME this does not get injected Not sure why
	public GcloudDatastoreRepositoryFactory(DatastoreOptions datastoreOptions) {
        Assert.notNull(datastoreOptions, "Data Store Options must not be null");
        this.datastoreOptions = datastoreOptions;
	}

	@Override
	public <T, ID> EntityInformation<T, ID>  getEntityInformation(Class<T> domainClass) {
		return new GcloudDatastoreEntityInformation<>(domainClass);
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
		return SimpleGcloudDatastoreRepository.class;
	}

    @Override
	protected Object getTargetRepository(RepositoryInformation information) {
		EntityInformation<?, Serializable> entityInformation = getEntityInformation(information.getDomainType());
		return getTargetRepositoryViaReflection(information, entityInformation,	this.datastoreOptions);
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(Key key,
                                                                   EvaluationContextProvider evaluationContextProvider) {

		return Optional.of(new GCloudDataStoreQueryLookupStrategy());
	}

	private class GCloudDataStoreQueryLookupStrategy implements QueryLookupStrategy {
        @Override
        public RepositoryQuery resolveQuery(Method method,
                                            RepositoryMetadata metadata,
                                            ProjectionFactory factory,
                                            NamedQueries namedQueries) {
            QueryMethod queryMethod = new QueryMethod(method, metadata, factory);

            if (queryMethod.isCollectionQuery()) {
                return new CollectionQuery(queryMethod, datastoreOptions);
            } else if (queryMethod.isStreamQuery()) {
                return new StreamQuery(queryMethod, datastoreOptions);
            } else if (queryMethod.isQueryForEntity()) {
                return new EntityQuery(queryMethod, datastoreOptions);
            } else {
                throw new UnsupportedOperationException("Query method not supported.");
            }
        }
    }
}
