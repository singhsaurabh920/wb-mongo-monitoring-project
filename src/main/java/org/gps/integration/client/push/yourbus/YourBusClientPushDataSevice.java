package org.gps.integration.client.push.yourbus;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.tb.core.constants.ApiConstant;
import com.tb.core.domain.ClientDataPushReport;
import com.tb.core.domain.TerminalPacketRecord;
import com.tb.core.domain.Vehicle;
import com.tb.core.domain.VenderDetail;
import com.tb.core.domain.service.VehicleService;
import com.tb.core.enums.Vender;

@Service
@Profile("prod")
public class YourBusClientPushDataSevice {
	private static final Logger logger = Logger.getLogger(YourBusClientPushDataSevice.class);
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	@Qualifier("customRestTemplate")
	private RestTemplate restTemplate;

	
	@Scheduled(fixedRate = 1000 * 60 * 2)
	public void yourBusAPiScheduler() {
		logger.info("YOURBUS API SCHEDULER STARTED");
		List<VenderDetail> venderDetailList=mongoTemplate.find(Query.query(Criteria.where("vender").is(Vender.YOURBUS_PUSH.name())), VenderDetail.class);
		logger.info("YOURBUS API TRANSPORTER SIZE-"+venderDetailList.size());
		for(VenderDetail venderDetail:venderDetailList) {
			List<Vehicle> vehicles = vehicleService.findByTransporterUsernameAndActive(venderDetail.getTransporter(),true);
			logger.info("YOURBUS VEHICLE SIZE-" + vehicles.size());
			ClientDataPushReport clientDataPushReport=startClientDataPushReport(venderDetail.getTransporter(),vehicles.size());
			clientDataPushReport.setStatus(ApiConstant.Status.SUCCESS);
			List<YourBusApiRequestData> yourBusdataList=new ArrayList<>();
			for (Vehicle vehicle : vehicles) {
		        yourBusdataList.add(getYourBusApiRequestData(vehicle));	
		        if (yourBusdataList.size()==49) {
		        	try {
		        		YourBusApiResponseData response=yourBusClientPushData(yourBusdataList);
		        		if(response.getCode()==200) {
		        			int successCount=yourBusdataList.size()- response.getData().size();
		        			clientDataPushReport.setSuccessCount(clientDataPushReport.getSuccessCount()+successCount);
		        		    mongoTemplate.save(clientDataPushReport);
		        		}
		        	}catch(Exception ex) {
		        		logger.info(ex);
		        		clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
		        		ex.printStackTrace();
		        	}
		        	yourBusdataList.clear();
				}
			}
			if(!yourBusdataList.isEmpty()) {
	        	try {
	        		YourBusApiResponseData response=yourBusClientPushData(yourBusdataList);
	        		if(response.getCode()==200) {
	        			int successCount=yourBusdataList.size()- response.getData().size();
	        			clientDataPushReport.setSuccessCount(clientDataPushReport.getSuccessCount()+successCount);
	        		}
	        	}catch(Exception ex) {
	        		clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
	        		logger.info(ex);
	        		ex.printStackTrace();
	        	}
			}
			stopClientDataPushReport(clientDataPushReport);
		}
		logger.info("YOURBUS API SCHEDULER STOPPED");
	}
	
	private YourBusApiResponseData yourBusClientPushData(List<YourBusApiRequestData> yourBusApiRequestDataList ) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(ApiConstant.YOURBUS.AUTHORIZATION_KEY, ApiConstant.YOURBUS.AUTHORIZATION_VALUE);
		HttpEntity<List<YourBusApiRequestData>> request = new HttpEntity<>(yourBusApiRequestDataList, headers);
		ResponseEntity<YourBusApiResponseData> responses=restTemplate.postForEntity(ApiConstant.YOURBUS.API_URL,request,YourBusApiResponseData.class);
		YourBusApiResponseData  yourBusApiResponseData=responses.getBody();
		logger.info(yourBusApiResponseData);
		return yourBusApiResponseData;			
	}
	
	private YourBusApiRequestData getYourBusApiRequestData(Vehicle vehicle) {
		TerminalPacketRecord tpr = vehicle.getTerminalPacketRecord();
		float lat = 0;
		float lng = 0;
		String address = "";
		boolean ac = vehicle.isAc();
		boolean ignition = vehicle.isIgnition();
		double[] loc = tpr.getLocation();
		if (loc != null && loc.length > 1) {
			lat = (float) loc[1];
			lng= (float) loc[0];
		}
		if (StringUtils.isNotEmpty(tpr.getAddress())) {
			address=tpr.getAddress();
		}
		YourBusApiRequestData yourBusApiRequestData=new YourBusApiRequestData();
		yourBusApiRequestData.setLat(lat);
		yourBusApiRequestData.setLng(lng);
		yourBusApiRequestData.setAddress(address);
		yourBusApiRequestData.setSpeed(tpr.getSpeed());
		yourBusApiRequestData.setGpsId(vehicle.getDevice().getImei());
		yourBusApiRequestData.setTimestamp(vehicle.getLu().getTime()/1000);
		if (ac) {
			yourBusApiRequestData.setAcStatus(1);
		}
		if (ignition) {
			yourBusApiRequestData.setIgnitionStatus(1);
		}
		return yourBusApiRequestData;
	}
	private ClientDataPushReport startClientDataPushReport(String transporter,int count) {
		ClientDataPushReport clientDataPushReport = new ClientDataPushReport();
		clientDataPushReport.setVender(Vender.YOURBUS_PUSH.name());
		clientDataPushReport.setStatus(ApiConstant.Status.START);
		clientDataPushReport.setCount(count);
		clientDataPushReport.setStartTime(new Date());
		clientDataPushReport.setUsername(transporter);
		mongoTemplate.save(clientDataPushReport);
		return clientDataPushReport;
	}
	
	private ClientDataPushReport stopClientDataPushReport(ClientDataPushReport clientDataPushReport) {
		clientDataPushReport.setEndTime(new Date());
		mongoTemplate.save(clientDataPushReport);
		return clientDataPushReport;
	}
}
