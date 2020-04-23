package org.worldbuild.project.config;

import com.mongodb.Block;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.connection.SocketSettings;
import com.mongodb.connection.SslSettings;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDbFactory;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
@Log4j2
@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = {"org.worldbuild.project.domain.repository"})
public class MongoConfig extends AbstractMongoClientConfiguration {

    private static final String UTF_8 = "UTF-8";
	private static final String MONGODB_PREFIX = "mongodb://";
    private static final String MONGODB_SRV_PREFIX = "mongodb+srv://";
    private static final Set<String> ALLOWED_OPTIONS_IN_TXT_RECORD = new HashSet<String>(asList("authsource", "replicaset"));
	
    private final MongoProperties mongoProperties;

    public MongoConfig(MongoProperties mongoProperties){
    	this.mongoProperties=mongoProperties;
    }

	@Override
	protected String getDatabaseName() {
		return mongoProperties.getDatabase();
	}

	@Bean
	@Primary
	@Override
   	public com.mongodb.client.MongoClient mongoClient() {
		log.info("MongoClient is initializing..........");
		SocketSettings.builder().connectTimeout(2, TimeUnit.MINUTES);
		MongoCredential mongoCredential=MongoCredential.createCredential(mongoProperties.getUsername(),
																		 mongoProperties.getAuthenticationDatabase(),
																		 mongoProperties.getPassword());
		log.info("Mongo URI: "+mongoProperties.getUri());
		ConnectionString connectionString=new ConnectionString(mongoProperties.getUri());
		log.info("Mongo Replica: "+connectionString.getRequiredReplicaSetName());
		MongoClientSettings mongoClientSettings = MongoClientSettings.builder().applyConnectionString(connectionString)
				.applyToConnectionPoolSettings(new Block<ConnectionPoolSettings.Builder>() {
					@Override
					public void apply(final ConnectionPoolSettings.Builder builder) {
						builder.maxSize(100).minSize(1);
					}
				}).applyToSocketSettings(new Block<SocketSettings.Builder>() {
					@Override
					public void apply(final SocketSettings.Builder builder) {
						builder.connectTimeout(3, TimeUnit.MINUTES).readTimeout(3, TimeUnit.MINUTES);
					}
				}).applyToSslSettings(new Block<SslSettings.Builder>() {
					@Override
					public void apply(final SslSettings.Builder builder) {
						builder.enabled(false);
					}
				}).credential(mongoCredential).build();
		return MongoClients.create(mongoClientSettings);
	}
	

	@Bean
	@Primary
	@Override
	public MongoDbFactory mongoDbFactory() {
		log.info("MongoDbFactory is initializing..........");
		return new SimpleMongoClientDbFactory(mongoClient(), getDatabaseName());
	}
	
    @Bean	
	@Primary
	@Override
	public MongoTemplate mongoTemplate() throws Exception {
		log.info("MongoTemplate is initializing..........");
		return new MongoTemplate(mongoDbFactory(), mappingMongoConverter());
	}
	
	@Override
	public CustomConversions customConversions() {
		log.info("CustomConversions is initializing..........");
		List<Converter<?, ?>> converterList = new ArrayList<Converter<?, ?>>();
		return new MongoCustomConversions(converterList);
	}
}
