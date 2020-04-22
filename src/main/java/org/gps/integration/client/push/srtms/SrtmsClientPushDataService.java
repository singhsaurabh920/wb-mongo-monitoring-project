package org.gps.integration.client.push.srtms;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.tb.core.constants.ApiConstant;
import com.tb.core.domain.ClientDataPushReport;
import com.tb.core.domain.TerminalPacketRecord;
import com.tb.core.domain.Vehicle;
import com.tb.core.domain.service.VehicleService;
import com.tb.core.enums.Vender;

//@Service
//@Profile("prod")
public class SrtmsClientPushDataService {
	private static final Logger logger = Logger.getLogger(SrtmsClientPushDataService.class);
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	@Qualifier("customRestTemplate")
	private RestTemplate restTemplate;
	
	//@Scheduled(fixedRate = 1000 * 60 * 20)
	public void pushSrtmsApiData(){
		logger.info("SRTMS API scheduler started");
		List<Vehicle> vehicleList=vehicleService.findByClientUsernameAndActive("srtms", true);
		logger.info("SRTMS API VEHICLE SIZE-"+vehicleList.size());
			List<SrtmsApiRequestData>  srtmsApiRequestDataList=srtmsApiData(vehicleList);
			ClientDataPushReport clientDataPushReport=startClientDataPushReport("srtms",srtmsApiRequestDataList.size());
			clientDataPushReport.setStatus(ApiConstant.Status.SUCCESS);
			try {
				SrtmsApiResponseData response=srtmsClientPushData(srtmsApiRequestDataList);
				List<String> invalideCoordinate=response.getInvalid_coordinate_index();
				if(invalideCoordinate==null) {
					clientDataPushReport.setSuccessCount(srtmsApiRequestDataList.size());
				}else {
					clientDataPushReport.setSuccessCount(srtmsApiRequestDataList.size()-invalideCoordinate.size());
					clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
				}
			}catch(Exception ex) {
				logger.info(ex.getMessage());
				clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
			}
			clientDataPushReport.setEndTime(new Date());
			mongoTemplate.save(clientDataPushReport);
			logger.info("SRTMS API scheduler Stopped");
		
	}
	private List<SrtmsApiRequestData> srtmsApiData(List<Vehicle> vehicles) {
		List<SrtmsApiRequestData>  srtmsApiRequestDataList =new ArrayList<>();
		for (Vehicle vehicle : vehicles) {
			SrtmsApiRequestData srtmsApiRequestData = new SrtmsApiRequestData();
			TerminalPacketRecord tpr = vehicle.getTerminalPacketRecord();
			
			boolean ignition = vehicle.isIgnition();
			String ignitionStatus = "OFF";
			double[] loc = tpr.getLocation();
			if(loc==null || loc[1]==0.0 || loc[0]==0.0) {
				continue;
			}
			srtmsApiRequestData.setDeviceIMEI(vehicle.getDevice().getImei());
			srtmsApiRequestData.setTimestamp(vehicle.getLu().getTime()/1000);
			srtmsApiRequestData.setLatitude(loc[1]);
			srtmsApiRequestData.setLongitude(loc[0]);
			srtmsApiRequestData.setSpeed(tpr.getSpeed());
			srtmsApiRequestData.setDistanceCovered((long)vehicle.getTotkm());
			if (ignition) {
				ignitionStatus = "ON";
			}
			srtmsApiRequestData.setIgnitionStatus(ignitionStatus);
			srtmsApiRequestDataList.add(srtmsApiRequestData);
		}
		return srtmsApiRequestDataList;
	}
	
	private SrtmsApiResponseData srtmsClientPushData(List<SrtmsApiRequestData>  data ) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<List<SrtmsApiRequestData>> request = new HttpEntity<>(data, headers);
		ResponseEntity<SrtmsApiResponseData> response=restTemplate.postForEntity(ApiConstant.SRTMS.API_URL,request,SrtmsApiResponseData.class);
		logger.info(response.getBody());
		return response.getBody();
	}

	private ClientDataPushReport startClientDataPushReport(String transporter,int count) {
		ClientDataPushReport clientDataPushReport=new ClientDataPushReport();
		clientDataPushReport.setVender(Vender.SRTMS_PUSH.name());
		clientDataPushReport.setStatus(ApiConstant.Status.START);
		clientDataPushReport.setCount(count);
		clientDataPushReport.setStartTime(new Date());
		clientDataPushReport.setUsername(transporter);
		mongoTemplate.save(clientDataPushReport);
		return clientDataPushReport;
	}
	
}
