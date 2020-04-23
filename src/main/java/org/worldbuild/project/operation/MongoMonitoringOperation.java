package org.worldbuild.project.operation;

import com.mongodb.Block;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.connection.SocketSettings;
import com.mongodb.connection.SslSettings;

import lombok.extern.log4j.Log4j2;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.worldbuild.project.domain.entity.Connection;
import org.worldbuild.project.domain.entity.NetCounter;
import org.worldbuild.project.domain.entity.OpCounter;
import org.worldbuild.project.domain.entity.Replication;
import org.worldbuild.project.utils.DateTimeUtils;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Log4j2
@Component
@Profile({"dev","prod"})
public class MongoMonitoringOperation {

	@Autowired
	@Qualifier("mongoMonitor")
	private MongoClient mongoMonitor;
	@Autowired
	private MongoTemplate mongoTemplate;
	
	private final MongoProperties mongoProperties;
	
	@PostConstruct
	public void init() {
		log.info("Mongo Monitoring Operation is initializing..........");
	}
	
	public MongoMonitoringOperation(MongoProperties mongoProperties){
    	this.mongoProperties=mongoProperties;
	}

	@Scheduled(cron = "0 0/5 * * * ?")
	public void checkMongoStatus() {
		log.info("Mongo Monitoring Operation Started");
		try {
			generateMongoReport();
		} catch (Exception ex) {
			log.info("Mongo Monitoring Exception-", ex);
		}
		log.info("Mongo Monitoring Operation Stoped");
	}

	@Bean("mongoMonitor")
	public MongoClient mongoClient() {
		log.info("MongoMonitor is initializing..........");
		MongoCredential mongoCredential = MongoCredential.createCredential("clusterAdmin",
																			"admin",
																			"clusterAdmin".toCharArray());
		final String uri=mongoProperties.getUri();
		log.info("Mongo monitor URI: "+uri);
		ConnectionString connectionString = new ConnectionString(uri);
		log.info("Mongo monitor Replica: "+connectionString.getRequiredReplicaSetName());
		MongoClientSettings mongoClientSettings = MongoClientSettings.builder().applyConnectionString(connectionString)
				.applyToConnectionPoolSettings(new Block<ConnectionPoolSettings.Builder>() {
					@Override
					public void apply(final ConnectionPoolSettings.Builder builder) {
						builder.maxSize(1).minSize(1);
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

	public void generateMongoReport() {
		MongoDatabase database = mongoMonitor.getDatabase("admin");
		// Document buildInfoResults = database.runCommand(new Document("buildInfo",1));
		// logger.info(buildInfoResults.toJson());
		// Document collStatsResults = database.runCommand(new Document("collStats","vehicle"));
		// logger.info(collStatsResults.toJson());
		// Document profileResults = database.runCommand(new Document("profile", 1));
		// logger.info(profileResults.toJson());
		// Document hostInfoResults = database.runCommand(new Document("hostInfo", 1));
		// logger.info(hostInfoResults.toJson());
		// Document topResults = database.runCommand(new Document("top", 1));
		// logger.info(topResults.toJson());
		// Document replSetGetStatusResults = database.runCommand(new Document("replSetGetStatus", 1));
		// logger.info(replSetGetStatusResults.toJson());
		Document doc = new Document("serverStatus", 1);
		Document serverStatsResults = database.runCommand(doc);
		Document repl = (Document) serverStatsResults.get("repl");
		generateReplicaReport(repl);
		Document network = (Document) serverStatsResults.get("network");
		generateNetworkReport(network);
		Document connection = (Document) serverStatsResults.get("connections");
		generateConnectionReport(connection);
		Document opCounter = (Document) serverStatsResults.get("opcounters");
		generateOpConcuterReport(opCounter);
		log.info(serverStatsResults.toJson());
	}

	private void generateReplicaReport(Document doc) {
		Integer rbid = (Integer) doc.get("rbid");
		boolean ismaster = (boolean) doc.get("ismaster");
		boolean secondary = (boolean) doc.get("secondary");
		String primary = (String) doc.get("primary");
		String setName = (String) doc.get("setName");
		Integer setVersion = (Integer) doc.get("setVersion");
		List<String> hosts = (List<String>) doc.get("hosts");
		List<String> arbiters = (List<String>) doc.get("arbiters");
		Document lastWrite = (Document) doc.get("lastWrite");
		Date lastWriteDate = (Date) lastWrite.get("lastWriteDate");
		Replication cReplication = new Replication();
		cReplication.setRbid(rbid);
		cReplication.setIsmaster(ismaster);
		cReplication.setSecondary(secondary);
		cReplication.setPrimary(primary);
		cReplication.setSetName(setName);
		cReplication.setSetVersion(setVersion);
		cReplication.setHosts(hosts);
		cReplication.setArbiters(arbiters);
		cReplication.setLastWrite(lastWriteDate);
		mongoTemplate.save(cReplication, "db_repl");
	}

	private void generateConnectionReport(Document doc) {
		Integer current = (Integer) doc.get("current");
		Integer available = (Integer) doc.get("available");
		Integer totalCreated = (Integer) doc.get("totalCreated");
		Connection connection = new Connection();
		connection.setCurrent(current);
		connection.setAvailable(available);
		connection.setTotalCreated(totalCreated);
		mongoTemplate.save(connection, "db_con");
	}

	private void generateNetworkReport(Document doc) {
		try {
		Long totalIn = (Long) doc.get("bytesIn");
		Long totalOut = (Long) doc.get("bytesOut");
		Long totalRquest = (Long) doc.get("numRequests");
		Query query = new Query();
		Date date = DateTimeUtils.minusTimeInDate(new Date(),11,TimeUnit.MINUTES);
		query.addCriteria(Criteria.where("_id").gte(new ObjectId(date)));
		query.with(Sort.by(Sort.Direction.DESC, "_id"));
		query.limit(1);
		NetCounter lNetCounter = Optional.ofNullable(mongoTemplate.findOne(query, NetCounter.class, "db_net")).orElse(new NetCounter());
		log.info("Last Id-" + lNetCounter.getId());
		NetCounter cNetCounter = new NetCounter();
		cNetCounter.setTotalIn(totalIn);
		cNetCounter.setTotalOut(totalOut);
		cNetCounter.setTotalRequest(totalRquest);
		cNetCounter.setIn(totalIn - lNetCounter.getTotalIn());
		cNetCounter.setOut(totalOut - lNetCounter.getTotalOut());
		cNetCounter.setRequest(totalRquest - lNetCounter.getTotalRequest());
		mongoTemplate.save(cNetCounter, "db_net");
		}catch (Exception ex) {
			log.info("Mongo NetCounter  Exception-", ex);
		}
	}

	private void generateOpConcuterReport(Document doc) {
		try {
		//Object objQuery=doc.get("query");
		Integer totalQuery = (Integer) doc.get("query");
		//Object objInsert = doc.get("insert");
		Integer totalInsert = (Integer) doc.get("insert");
		//Object objUpdate = doc.get("update");
		Integer totalUpdate = (Integer) doc.get("update");
		//Object objDelete = doc.get("delete");
		Integer totalDelete = (Integer) doc.get("delete");
		//Object objGetmore = doc.get("getmore");
		Integer totalGetmore = (Integer) doc.get("getmore");
		//Object objCommand = doc.get("command");
		Integer totalCommand = (Integer) doc.get("command");
		Query query = new Query();
		Date date = DateTimeUtils.minusTimeInDate(new Date(),11,TimeUnit.MINUTES);
		query.addCriteria(Criteria.where("_id").gte(new ObjectId(date)));
		query.with(Sort.by(Sort.Direction.DESC, "_id"));
		query.limit(1);
		OpCounter lOpCounter = Optional.ofNullable(mongoTemplate.findOne(query, OpCounter.class, "db_op")).orElse(new OpCounter());
		log.info("Last Id-" + lOpCounter.getId());
		OpCounter cOpCounter = new OpCounter();
		cOpCounter.setTotalQuery(totalQuery);
		cOpCounter.setTotalInsert(totalInsert);
		cOpCounter.setTotalUpdate(totalUpdate);
		cOpCounter.setTotalDelete(totalDelete);
		cOpCounter.setTotalGetmore(totalGetmore);
		cOpCounter.setTotalCommand(totalCommand);
		cOpCounter.setQuery(totalQuery - lOpCounter.getTotalQuery());
		cOpCounter.setInsert(totalInsert - lOpCounter.getTotalInsert());
		cOpCounter.setUpdate(totalUpdate - lOpCounter.getTotalUpdate());
		cOpCounter.setDelete(totalDelete - lOpCounter.getTotalDelete());
		cOpCounter.setGetmore(totalGetmore - lOpCounter.getTotalGetmore());
		cOpCounter.setCommand(totalCommand - lOpCounter.getTotalCommand());
		mongoTemplate.save(cOpCounter, "db_op");
		}catch (Exception ex) {
			log.info("Mongo OpCounter  Exception-", ex);
		}
	}
}
