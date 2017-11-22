package org.springframework.data.gclouddatastore.repository;

import com.google.cloud.datastore.DatastoreOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gclouddatastore.repository.config.EnableGcloudDatastoreRepositories;

@Configuration
@EnableGcloudDatastoreRepositories(basePackages = "org.springframework.data.gclouddatasource.testing")
public class IntegrationTestConfig {

    @Bean
    public DatastoreOptions datastoreOptions() {
        return DatastoreOptions.getDefaultInstance();
    }
}
