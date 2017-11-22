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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.KeyQuery;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.gclouddatastore.GcloudDatastoreRepository;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.util.Assert;

public class SimpleGcloudDatastoreRepository<T, ID extends Serializable> implements GcloudDatastoreRepository<T, ID> {
	private static final Logger LOG = LoggerFactory.getLogger(SimpleGcloudDatastoreRepository.class);
	private static final int BUFFER_SIZE = 50;

	private final DatastoreOptions datastoreOptions;
	private final EntityInformation<T, ID> entityInformation;
	private final String kind;

	public SimpleGcloudDatastoreRepository(EntityInformation<T, ID> entityInformation, DatastoreOptions datastoreOptions) {
		Assert.notNull(entityInformation, "EntityInformation must not be null!");
        Assert.notNull(datastoreOptions, "DatastoreOptions must not be null!");

		this.entityInformation = entityInformation;
		this.kind = entityInformation.getJavaType().getSimpleName();
		this.datastoreOptions = datastoreOptions;
	}

	@Override
	public long count() {
		Datastore datastore = this.datastoreOptions.getService();
		QueryResults<?> results = datastore.run(buildAllKeysQuery());
		long count = 0;
		while (results.hasNext()) {
			results.next();
			count++;
		}
		return count;
	}

    @Override
	public void deleteById(final ID id) {
		deleteKeys(Collections.singletonList(getKey(id)));
	}

	@Override
	public void delete(final T entity) {
		deleteAll(Collections.singletonList(entity));
	}

    @Override
	public void deleteAll(Iterable<? extends T> entities) {
		deleteKeys(() -> {
                        Iterator<? extends T> entityIter = entities.iterator();
                        return new Iterator<Key>() {
                                @Override
                                public boolean hasNext() {
                                        return entityIter.hasNext();
                                }

                                @Override
                                public Key next() {
                                        T entity = entityIter.next();
                                        ID id = entityInformation.getId(entity);
                                        return getKey(id);
                                }
                        };
                });
	}

	@Override
	public void deleteAll() {
		Datastore datastore = this.datastoreOptions.getService();
		KeyQuery query = buildAllKeysQuery();
		deleteKeys(() -> datastore.run(query));
	}

	@Override
	public boolean existsById(ID id) {
		return findById(id).isPresent();
	}

	@Override
	public Iterable<T> query(Query<Entity> query) {
		Datastore datastore = this.datastoreOptions.getService();
		QueryResults<Entity> results = datastore.run(query);
		return () -> new Iterator<T>() {
                        @Override
                        public boolean hasNext() {
                                return results.hasNext();
                        }

                        @Override
                        public T next() {
                                try {
                                        T entity = entityInformation.getJavaType().newInstance();
                                        Unmarshaller.unmarshalToObject(results.next(), entity);
                                        return entity;
                                }
                                catch (InstantiationException | IllegalAccessException e) {
                                        throw new IllegalStateException();
                                }
                        }
                };
	}

	@Override
	public Iterable<T> findAll() {
		EntityQuery.Builder queryBuilder = Query.newEntityQueryBuilder()
				.setKind(this.kind);
		setAncestorFilter().ifPresent(queryBuilder::setFilter);
		EntityQuery query = queryBuilder.build();

		LOG.debug("Find all entities query ({})", query.toString());

		return query(query);
	}

    @Override
	public Iterable<T> findAllById(Iterable<ID> ids) {
	    //FIXME this is terrible, it does a query for each key not 1 query
		return () -> {
                        Iterator<ID> idIter = ids.iterator();
                        return new Iterator<T>() {
                                @Override
                                public boolean hasNext() {
                                        return idIter.hasNext();
                                }

                                @Override
                                public T next() {
                                    Optional<T> optionalNext = findById(idIter.next());
                                    if (optionalNext.isPresent()) {
                                    return optionalNext.get();
                                    } else {
                                    throw new IllegalStateException("Optional was empty, should handle this correctly");
                                    }
                                }
                        };
                };
	}

	@Override
	public Optional<T> findById(ID id) {
		Datastore datastore = this.datastoreOptions.getService();
		Entity entity = datastore.get(getKey(id));
		if (entity == null) {
			return Optional.empty();
		}
		else {
			return Optional.of(Unmarshaller.unmarshal(entity, entityInformation.getJavaType()));
		}

	}

	@Override
	public <S extends T> S save(S entity) {
		saveAll(Collections.singletonList(entity));
		return entity;
	}

    @Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
		Datastore datastore = this.datastoreOptions.getService();

		//FIXME again using buffers what is this for, must be a reason, transaction limit maybe?
		List<FullEntity<? extends IncompleteKey>> buffer = new ArrayList<>();

		for (S entity : entities) {
			ID id = this.entityInformation.getId(entity);
			Key key = getKey(id);

			buffer.add(Marshaller.toEntity(entity, key));
			if (buffer.size() >= BUFFER_SIZE) {
				datastore.put(buffer.toArray(new FullEntity[buffer.size()]));
				buffer.clear();
			}
		}
		if (buffer.size() > 0) {
			datastore.put(buffer.toArray(new FullEntity[buffer.size()]));
		}

		return entities;
	}

	private void deleteKeys(Iterable<Key> keys) {
	    //TODO WTF does this use a buffer???
		Datastore datastore = this.datastoreOptions.getService();

		List<Key> buffer = new ArrayList<>(BUFFER_SIZE);
		for (Key key : keys) {
			buffer.add(key);

			if (buffer.size() >= BUFFER_SIZE) {
				datastore.delete(buffer.toArray(new Key[buffer.size()]));
				buffer.clear();
			}
		}
		if (buffer.size() > 0) {
			datastore.delete(buffer.toArray(new Key[buffer.size()]));
		}
	}

	//FIXME is this really used, it looks like its only for testing? Its a horrible architectural pattern
	private Optional<StructuredQuery.PropertyFilter> setAncestorFilter() {
		Datastore datastore = datastoreOptions.getService();

		Deque<PathElement> ancestors = Context.getAncestors();
		Deque<PathElement> init = new LinkedList<>();
		init.addAll(ancestors);
		PathElement last = init.pollLast();

		if (last != null) {
			KeyFactory keyFactory = datastore.newKeyFactory();
			keyFactory.addAncestors(init).setKind(last.getKind());
			Key key = last.hasId() ? keyFactory.newKey(last.getId())
					: keyFactory.newKey(last.getName());
			return Optional.of(StructuredQuery.PropertyFilter.hasAncestor(key));
		}

		return Optional.empty();
	}

	private KeyQuery buildAllKeysQuery() {
		KeyQuery.Builder queryBuilder = Query.newKeyQueryBuilder()
            .setKind(this.kind);
		setAncestorFilter().ifPresent(queryBuilder::setFilter);
		KeyQuery query = queryBuilder.build();

		LOG.debug("Get All Keys Query ({})", query.toString());

		return query;
	}

	private Key getKey(ID id) {
		KeyFactory keyFactory = datastoreOptions.getService()
            .newKeyFactory()
            .setKind(this.kind)
		    .addAncestors(Context.getAncestors());

		if (id instanceof Number) {
			return keyFactory.newKey(((Number) id).longValue());
		}
		else {
			return keyFactory.newKey(id.toString());
		}
	}
}
