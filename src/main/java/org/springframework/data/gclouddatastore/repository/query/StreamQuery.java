package org.springframework.data.gclouddatastore.repository.query;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import org.springframework.data.gclouddatastore.repository.Unmarshaller;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.Nullable;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamQuery implements RepositoryQuery {

    private final QueryMethod queryMethod;
    private final Class<?> domainType;
    private final PartTree tree;
    private final ResultProcessor resultProcessor;
    private final DatastoreOptions datastoreOptions;

    public StreamQuery(QueryMethod queryMethod, DatastoreOptions datastoreOptions) {
        this.queryMethod = queryMethod;
        this.datastoreOptions = datastoreOptions;
        this.resultProcessor = queryMethod.getResultProcessor();
        this.domainType = resultProcessor.getReturnedType().getDomainType();
        this.tree = new PartTree(queryMethod.getName(), domainType);
    }

    @Nullable
    @Override
    public Object execute(Object[] parameters) {
        GcloudDatastoreQueryCreator queryCreator = new GcloudDatastoreQueryCreator(
            tree,
            new ParametersParameterAccessor(queryMethod.getParameters(), parameters),
            datastoreOptions);
        StructuredQuery.Builder<Entity> queryBuilder = queryCreator.createQuery();
        queryBuilder.setKind(domainType.getSimpleName());

        Unmarshaller unmarshaller = new Unmarshaller();
        Datastore datastore = datastoreOptions.getService();
        QueryResults<Entity> results = datastore
            .run(queryBuilder.build());

        Iterable<Object> iterable = () -> new Iterator<Object>() {
            @Override
            public boolean hasNext() {
                return results.hasNext();
            }

            @Override
            public Object next() {
                try {
                    Object entity = domainType.newInstance();
                    unmarshaller.unmarshalToObject(results.next(), entity);
                    return entity;
                }
                catch (InstantiationException
                    | IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        Stream<Object> result = StreamSupport.stream(iterable.spliterator(), false);
        return resultProcessor.processResult(result);
    }

    @Override
    public QueryMethod getQueryMethod() {
        return queryMethod;
    }
}
