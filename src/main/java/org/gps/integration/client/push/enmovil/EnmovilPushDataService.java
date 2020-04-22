//API FOR MARUTI

package org.gps.integration.client.push.enmovil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.gps.core.utils.ConversionUtils;
import org.gps.core.utils.CustomTimezoneUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.tb.core.constants.ApiConstant;
import com.tb.core.domain.ClientDataPushReport;
import com.tb.core.domain.TerminalPacketMeta;
import com.tb.core.domain.TerminalPacketRecord;
import com.tb.core.domain.Vehicle;
import com.tb.core.domain.VenderDetail;
import com.tb.core.domain.service.VehicleService;
import com.tb.core.enums.VehicleStatus;
import com.tb.core.enums.Vender;

@Service
@Profile("prod")
public class EnmovilPushDataService {
	
	private static final Logger logger = Logger.getLogger(EnmovilPushDataService.class);
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	@Qualifier("customRestTemplate")
	private RestTemplate restTemplate;
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	private EnmovilLoginSuccessData loginData=new EnmovilLoginSuccessData();
	
	@Scheduled(fixedRate = 1000 * 60 * 3)
	public void pushEnmovilClientApiData(){
		logger.info("ENMOVIL Maruti API scheduler started");
		
		if(!loginData.isAuth()) {
			logger.info("ENMOVIL MARUTI LOGIN API CALLED");
			loginData=enmovilLoginApi();
		}
		if(loginData.isAuth()){
			List<VenderDetail> venderDetailList=mongoTemplate.find(Query.query(Criteria.where("vender").is(Vender.MARUTI_PUSH.name())), VenderDetail.class);
			logger.info("ENMOVIL MARUTI API TRANSPORTER SIZE-"+venderDetailList.size());
			for(VenderDetail venderDetail:venderDetailList) {
				ClientDataPushReport clientDataPushReport=null;
				List<EnmovilRequestData> data = enmovilApiData(venderDetail.getTransporter());
				clientDataPushReport=startClientDataPushReport(venderDetail.getTransporter(),data.size());
				clientDataPushReport.setStatus(ApiConstant.Status.SUCCESS);
				try {
					EnmovilApiResponse enmovliPushApiResponse = enmovliPushApi(data);
					if(enmovliPushApiResponse.getAuth()==null) {
						loginData.setAuth(true);
					}else {
						loginData.setAuth(enmovliPushApiResponse.getAuth());
					}
					if(enmovliPushApiResponse.getMessage().equalsIgnoreCase("SUCCESS")) {
						clientDataPushReport.setSuccessCount(data.size());
					}else {
						clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
					}
				}catch(Exception e) {
					logger.info(e.getMessage());
					clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
					e.printStackTrace();
				}
				clientDataPushReport.setEndTime(new Date());
				logger.info(clientDataPushReport);
				mongoTemplate.save(clientDataPushReport);
				logger.info("ENMOVIL MARUTI API scheduler Stopped");
				
			}
		}
	}

	
	private List<EnmovilRequestData> enmovilApiData(String transporter) {
		List<EnmovilRequestData> requestDataList=new ArrayList<>();
		
		List<Vehicle> vehicles = vehicleService.findByTransporterUsernameAndActive(transporter, true);
		logger.info("ENMOVIL MARUTI VEHICLE COUNT - "+vehicles.size());
		//
		Map<String, Vehicle> map = vehicles.stream().collect(Collectors.toMap(Vehicle::getOuid, vehicle -> vehicle));
		Date end = CustomTimezoneUtils.getCurrentDateInUTC();
		Date start = CustomTimezoneUtils.getMinuteBackDateInUTC(3, end);
		logger.info("Start data-" + start + " End data-" + end);
		
		List<TerminalPacketRecord> terminalPacketRecords = getTprList(map.keySet(),start,end);
		logger.info("TPR SIZE=="+terminalPacketRecords.size());
		Set<String> ouids=new HashSet<>();
		int running=0;
		for(Vehicle vehicle :vehicles) {
			TerminalPacketRecord tpr=vehicle.getTerminalPacketRecord();
			VehicleStatus vehicleStatus=VehicleStatus.getByDbValue(tpr.getStatus());
			vehicle.setTotkm(vehicle.getTotkm()+vehicle.getOdometer()+vehicle.getTodayKm()+vehicle.getTempTodayKm());
			if(vehicleStatus==VehicleStatus.UNREACHABLE || vehicleStatus==VehicleStatus.STOPPED || vehicleStatus==VehicleStatus.IDLE ) {
				EnmovilRequestData data=createEnmovilApiData(vehicle,tpr,new Date());
				data=setTotalKm(data,vehicle,tpr);
				requestDataList.add(data);
				ouids.add(vehicle.getOuid());
			}else if (vehicleStatus==VehicleStatus.RUNNING||vehicleStatus==VehicleStatus.STARTED) {
				++running;
			} 
		}
		logger.info("Runnnig vehicle size - "+ running);
		logger.info("Idle, Stop and Unreachble synced vehicle - "+ouids.size());
		//
		for (TerminalPacketRecord tpr : terminalPacketRecords) {
			Vehicle vehicle = map.get(tpr.getOuid());
			ouids.add(vehicle.getOuid());
			EnmovilRequestData data = createEnmovilApiData(vehicle,tpr,tpr.getAdded());
			data=setTotalKm(data,vehicle,tpr);
			requestDataList.add(data);
		}
		logger.info("Enmovil API scheduler synced-" + terminalPacketRecords.size());
		logger.info("Total Enmovil vehicle Synced- "+ouids.size());
		return requestDataList;
		
	}
	
	private List<TerminalPacketRecord> getTprList(Set<String> ouids, Date start, Date end) {
		logger.info("IN QUERY START TIME  "+new Date());
		Criteria criteria = Criteria.where("ouid").in(ouids).andOperator(Criteria.where("add").gte(start).lte(end), Criteria.where("loc").ne(null));
		MatchOperation matchOperation = Aggregation.match(criteria);
		LookupOperation lookupOperation = Aggregation.lookup("addresses", "refk", "refk", "doc");
		ProjectionOperation projectionOperation = Aggregation.project("id", "ouid", "refk", "loc","dis", "spd",  "add", "sts").and("doc.adrs").as("adrs");
		UnwindOperation unwindOperation = Aggregation.unwind("adrs", true);
		SortOperation sortOperation = Aggregation.sort(Direction.DESC, "add");
		Aggregation aggregation = Aggregation
				.newAggregation(matchOperation, lookupOperation, projectionOperation, unwindOperation, sortOperation)
				.withOptions(Aggregation.newAggregationOptions().allowDiskUse(true).build());
		logger.info("--------Tpr Current-------");
		AggregationResults<TerminalPacketRecord> results = mongoTemplate.aggregate(aggregation,"tpr_current", TerminalPacketRecord.class);
		List<TerminalPacketRecord> tprs = results.getMappedResults();
		logger.info("IN QUERY END TIME  "+new Date());
		return tprs;
	}

	private EnmovilRequestData createEnmovilApiData(Vehicle vehicle,TerminalPacketRecord tpr,Date date) {
		EnmovilRequestData data=new EnmovilRequestData();
		TerminalPacketMeta tpm=vehicle.getTerminalPacketMeta();
		double[] location = tpr.getLocation();
		//
		data.setVehicleNo(vehicle.getVehicleNo());
		data.setLatitude(String.valueOf(ConversionUtils.roundDown6(location[1])));
		data.setLongitude(String.valueOf(ConversionUtils.roundDown6(location[0])));
		data.setTimestamp(CustomTimezoneUtils.UTCDateToFareyeDateTimeStringHHmmss(vehicle.getTransporterTz(),date));
		if(StringUtils.isBlank(tpr.getAddress())) {
			tpr.setAddress("Address not found");
		}
		data.setAddress(tpr.getAddress());
		data.setSpeed(String.valueOf(tpr.getSpeed()));
		VehicleStatus vehicleStatus=VehicleStatus.getByDbValue(tpr.getStatus());
		if(vehicleStatus==VehicleStatus.IDLE||vehicleStatus==VehicleStatus.RUNNING||vehicleStatus==VehicleStatus.OVERSPEED) {
			data.setIgnition(String.valueOf(1));
		} else {
			data.setIgnition(String.valueOf(0));
		}
		if(tpm!=null) {
			data.setBattery(String.valueOf(tpm.getBattery()));
			data.setSignal(String.valueOf(tpm.getGsmSignals()));
		}else {
			data.setBattery("0");
			data.setSignal("0");
		}
		data.setDirection("0");
		data.setMsgtype("R");
		//
		return data;
	}
	
	private EnmovilApiResponse enmovliPushApi(List<EnmovilRequestData> body) {
		Gson gson=new Gson();
		String reverseData=gson.toJson(Lists.reverse(body));
		//logger.info(new Gson().toJson(body));
		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl("no-cache");
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		logger.info(loginData.getToken());
		headers.add("Authorization","Bearer "+loginData.getToken());
		/*MultiValueMap<String,List<EnmovilRequestData>> map=new LinkedMultiValueMap<>();
		map.add("gpsdata",body);*/
		HttpEntity<String> request = new HttpEntity<>("gpsdata="+reverseData, headers);
		ResponseEntity<EnmovilApiResponse> response = restTemplate.postForEntity(ApiConstant.ENMOVIL.SEND_LOCATION_DATA_URL, request, EnmovilApiResponse.class);
		EnmovilApiResponse apiResponse=response.getBody();
		logger.info("ENMOVIL MARUTI API Response-" + apiResponse);
		return apiResponse;
	}
	
	private EnmovilLoginSuccessData enmovilLoginApi() {
		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl("no-cache");
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String,String> map=new LinkedMultiValueMap<>();
		map.add("email", ApiConstant.ENMOVIL.EMAIL);
		map.add("password", ApiConstant.ENMOVIL.PASSWORD);
		HttpEntity<MultiValueMap<String, String>> request=new HttpEntity<>(map,headers);
		ResponseEntity<EnmovilLoginSuccessData> responseEntity=restTemplate.postForEntity(ApiConstant.ENMOVIL.LOGIN_URL, request, EnmovilLoginSuccessData.class);
		logger.info("Login Response  Data  "+responseEntity.getBody());
		
		return responseEntity.getBody();
	}
	
	private ClientDataPushReport startClientDataPushReport(String transporter,int count) {
		ClientDataPushReport clientDataPushReport = new ClientDataPushReport();
		clientDataPushReport.setVender(Vender.MARUTI_PUSH.name());
		clientDataPushReport.setStatus(ApiConstant.Status.START);
		clientDataPushReport.setCount(count);
		clientDataPushReport.setStartTime(new Date());
		clientDataPushReport.setUsername(transporter);
		mongoTemplate.save(clientDataPushReport);
		return clientDataPushReport;
	}
	
	private EnmovilRequestData setTotalKm(EnmovilRequestData enmovilRequestData , Vehicle vehicle,TerminalPacketRecord tpr) {
		double totKm = vehicle.getTotkm() - (tpr.getDistance() / 1000);
		vehicle.setTotkm(totKm);
		enmovilRequestData.setDistance(String.valueOf(totKm));
		return enmovilRequestData;
	}

}
