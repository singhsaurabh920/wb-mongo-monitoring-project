package org.gps.integration.config;

import com.mongodb.Block;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.connection.SocketSettings;
import com.tb.core.configuration.MultipleMongoProperties;
import org.apache.log4j.Logger;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDbFactory;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {	
	private final static Logger logger = Logger.getLogger(MongoConfig.class);
    private static final String UTF_8 = "UTF-8";
	private static final String MONGODB_PREFIX = "mongodb://";
    private static final String MONGODB_SRV_PREFIX = "mongodb+srv://";
    private static final Set<String> ALLOWED_OPTIONS_IN_TXT_RECORD = new HashSet<String>(asList("authsource", "replicaset"));
	
    private final MongoProperties mongoProperties;

    private final MultipleMongoProperties multipleMongoProperties;
    
    public MongoConfig(MultipleMongoProperties multipleMongoProperties){
    	this.multipleMongoProperties=multipleMongoProperties;
    	this.mongoProperties=multipleMongoProperties.getPrimary();
    }

	@Override
	protected String getDatabaseName() {
		return mongoProperties.getDatabase();
	}
	

	@Bean
	@Primary
	@Override
   	public com.mongodb.client.MongoClient mongoClient() {
		logger.info("Primary MongoClient instantiated");
		SocketSettings.builder().connectTimeout(2, TimeUnit.MINUTES);
		MongoCredential mongoCredential=MongoCredential.createCredential(mongoProperties.getUsername(),
																		 mongoProperties.getAuthenticationDatabase(),
																		 mongoProperties.getPassword());
		logger.info("Mongo URI ################## "+mongoProperties.getUri());
		ConnectionString connectionString=new ConnectionString(mongoProperties.getUri());
		logger.info("Mongo Replica ##################"+connectionString.getRequiredReplicaSetName());
		MongoClientSettings mongoClientSettings=MongoClientSettings.builder()
																   .applyConnectionString(connectionString)
																   .applyToConnectionPoolSettings(new Block<ConnectionPoolSettings.Builder>() {
																		@Override
																		public void apply(final ConnectionPoolSettings.Builder builder) {
																			builder.maxSize(100).minSize(1);
																		}
																   })
																   .credential(mongoCredential)
																   .build();	
		return MongoClients.create(mongoClientSettings);
	}
	

	@Bean
	@Primary
	@Override
	public MongoDbFactory mongoDbFactory() {
		logger.info("Primary MongoDbFactory instantiated");
		return new SimpleMongoClientDbFactory(mongoClient(), getDatabaseName());
	}
	
    @Bean	
	@Primary
	@Override
	public MongoTemplate mongoTemplate() throws Exception {
		logger.info("Primary MongoTemplate instantiated");
        return new MongoTemplate(mongoDbFactory(), mappingMongoConverter());
	}
	
	@Override
	public CustomConversions customConversions() {
		logger.info("Custom converter instantiated");
		List<Converter<?, ?>> converterList = new ArrayList<Converter<?, ?>>();
		return new MongoCustomConversions(converterList);
	}
	
}
