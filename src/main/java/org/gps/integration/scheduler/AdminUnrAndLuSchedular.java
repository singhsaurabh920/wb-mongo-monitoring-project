package org.gps.integration.scheduler;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.gps.core.event.model.ClientVehicleCountData;
import org.gps.core.utils.CustomTimezoneUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOptions;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tb.core.domain.TrackobitUser;
import com.tb.core.domain.Vehicle;
import com.tb.core.domain.AdminUnrReport;
import com.tb.core.domain.service.TrackobitUserService;
import com.tb.core.enums.VehicleStatus;

@Component
@Profile("prod")
public class AdminUnrAndLuSchedular {
	private final static Logger logger = Logger.getLogger(AdminUnrAndLuSchedular.class);
	private Map< String,List<String>> childClientMap=new LinkedHashMap<>();
	
	@Autowired
	private TrackobitUserService trackobitUserService;
	@Autowired
	private MongoTemplate mongoTemplate;
	
	@Scheduled(initialDelay=1000*60,fixedRate = 1000 * 60 * 5)
	private void schedulars() {
		logger.info("ADMIN UNR AND LU SCHEDULAR START.......................");
		List<TrackobitUser> directClients = trackobitUserService.findByOwnerUsername("tbadmin");
		childClientMap = directClients.stream().collect(Collectors.toMap(TrackobitUser :: getUsername, tb->{
			List<String> clients = Optional.ofNullable(tb.getChildClients()).orElse(new ArrayList<>());
			clients.add(tb.getUsername());
			return clients;
		}));
		//
		List<AdminUnrReport> vehicleLuStates = generateLuReport();
		//
		List<AdminUnrReport> vehicleUnrStates = generateUnrReport();
		//
		List<AdminUnrReport> adminUnrReports = Stream.of(vehicleLuStates,vehicleUnrStates).flatMap(v->v.stream()).collect(Collectors.toList());
		
		mongoTemplate.insert(adminUnrReports,AdminUnrReport.class);
		logger.info("ADMIN UNR AND LU  SCHEDULAR END.......................");
		
	}

	private List<AdminUnrReport> generateLuReport() {
		Date start = CustomTimezoneUtils.getMinuteBackDateInUTC(5, new Date());
		Criteria criteria = Criteria.where("lu").gte(start);
		MatchOperation matchOperation = Aggregation.match(criteria);
		ProjectionOperation projectionOperation = Aggregation.project().and("clientUsername").as("clientUsername");
		GroupOperation groupOperation = Aggregation.group("clientUsername").first("clientUsername").as("clientUsername")
				.count().as("noOfVehicles");
		AggregationOptions options = AggregationOptions.builder().allowDiskUse(true).cursorBatchSize(0).build();
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, projectionOperation, groupOperation)
				.withOptions(options);
		List<ClientVehicleCountData> vehicleCountDatas = mongoTemplate
				.aggregate(aggregation, Vehicle.class, ClientVehicleCountData.class).getMappedResults();

		return getVehicleStateList("lu", vehicleCountDatas);
	}
	
	private List<AdminUnrReport> generateUnrReport() {
		Criteria criteria = Criteria.where("terminalPacketRecord.sts").is(VehicleStatus.UNREACHABLE.getDbValue());
		MatchOperation matchOperation = Aggregation.match(criteria);
		ProjectionOperation projectionOperation = Aggregation.project().and("clientUsername").as("clientUsername");

		GroupOperation groupOperation = Aggregation.group("clientUsername").first("clientUsername").as("clientUsername")
				.count().as("noOfVehicles");
		AggregationOptions options = AggregationOptions.builder().allowDiskUse(true).cursorBatchSize(0).build();
		Aggregation aggregation = Aggregation.newAggregation(matchOperation, projectionOperation, groupOperation)
				.withOptions(options);
		List<ClientVehicleCountData> vehicleCountDatas = mongoTemplate
				.aggregate(aggregation, Vehicle.class, ClientVehicleCountData.class).getMappedResults();

		return getVehicleStateList("unr", vehicleCountDatas);
	}
	
	private List<AdminUnrReport> getVehicleStateList(String state,List<ClientVehicleCountData> vehicleCountDatas ){
		Map<String, Integer> collect = vehicleCountDatas.stream()
				 .collect(Collectors.groupingBy(ClientVehicleCountData :: getClientUsername,Collectors.summingInt(ClientVehicleCountData::getNoOfVehicles)));
		
		Map<String, Integer> clientVehicleCount =new LinkedHashMap<>();
		for (Entry<String, List<String>> client: childClientMap.entrySet()) {
			for (Entry<String, Integer> clientVehicle : collect.entrySet()) {
				if(client.getValue().contains(clientVehicle.getKey())) {
					clientVehicleCount.put(client.getKey(), Optional.ofNullable(clientVehicleCount.get(client.getKey())).orElse(0)+clientVehicle.getValue());
				}else if(clientVehicleCount.get(client.getKey())==null) {
					clientVehicleCount.put(client.getKey(), 0);
				}
			}
		}
		
		return clientVehicleCount.entrySet().stream().map(data->{
			AdminUnrReport adminUnrReport=new AdminUnrReport();
			adminUnrReport.setClientUsername(data.getKey());
			adminUnrReport.setAdded(new Date());
			adminUnrReport.setVehicleCount(data.getValue());
			adminUnrReport.setState(state);
			return adminUnrReport;
		}).collect(Collectors.toList());
	}
	
}
