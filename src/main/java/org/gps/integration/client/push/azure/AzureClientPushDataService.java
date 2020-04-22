package org.gps.integration.client.push.azure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.gps.core.utils.CustomTimezoneUtils;
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
public class AzureClientPushDataService {
	private static final Logger logger = Logger.getLogger(AzureClientPushDataService.class);
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	@Qualifier("customRestTemplate")
	private RestTemplate restTemplate;
	
	@Scheduled(fixedRate = 1000 * 60 * 10)
	public void pushAzureClientApiData(){
		logger.info("Azure API scheduler started");
		List<VenderDetail> venderDetailList=mongoTemplate.find(Query.query(Criteria.where("vender").is(Vender.AZURE_PUSH.name())), VenderDetail.class);
		logger.info("Azure API TRANSPORTER SIZE-"+venderDetailList.size());
		for(VenderDetail venderDetail:venderDetailList) {
			List<AzureApiRequestData> apiRequestDataList=getAzureClientRequestData(venderDetail.getTransporter());
			ClientDataPushReport clientDataPushReport = startClientDataPushReport(venderDetail.getTransporter(),apiRequestDataList.size());
			clientDataPushReport.setStatus(ApiConstant.Status.SUCCESS);
			try {
				AzureApiResponseData[] response=azureClientPushData(apiRequestDataList);	
				List<AzureApiResponseData>  azureApiResponseDataList=Arrays.asList(response);
				List<AzureApiResponseData> successList=azureApiResponseDataList.stream().filter(azureApiResponse->azureApiResponse.isSuccess()==true).collect(Collectors.toList());
				clientDataPushReport.setSuccessCount(successList.size());
				List<AzureApiResponseData> failList=azureApiResponseDataList.stream().filter(azureApiResponse->azureApiResponse.isSuccess()==false).collect(Collectors.toList());
				if(failList.size()>0) {
					clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
				}
			}catch(Exception ex) {
				logger.info(ex);
				clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
	    		ex.printStackTrace();
			}
			clientDataPushReport.setEndTime(new Date());
			mongoTemplate.save(clientDataPushReport);
		}
		logger.info("Azure API scheduler Stopped");
	} 
	
	private List<AzureApiRequestData> getAzureClientRequestData(String transporter){
		List<Vehicle> vehicles= vehicleService.findByTransporterUsernameAndActive(transporter,true);
		List<AzureApiRequestData> apiRequestDataList=new ArrayList<>();
		for (Vehicle vehicle : vehicles) {
			String timezone=vehicle.getTransporterTz();
			AzureApiRequestData apiRequestData=new AzureApiRequestData();
			apiRequestData.setVehicleID(vehicle.getVehicleNo());
			apiRequestData.setDeviceID(vehicle.getDevice().getImei());
			apiRequestData.setCustomerId(ApiConstant.AZURE.CUSTOMERID);
			apiRequestData.setProvider(ApiConstant.AZURE.PROVIDER);
			apiRequestData.setDateTime(CustomTimezoneUtils.dateToAzureDateTimeString(timezone,new Date()));
			TerminalPacketRecord tpr=vehicle.getTerminalPacketRecord();
			double location[]=tpr.getLocation();
			AzureApiGPSData apiGPSData=new AzureApiGPSData();
			apiGPSData.setLocation(tpr.getAddress());
			apiGPSData.setLatitude(Double.toString(location[1]));
			apiGPSData.setLongitude(Double.toString(location[0]));
			apiGPSData.setGpsDateTime(CustomTimezoneUtils.dateToAzureDateTimeString(timezone,vehicle.getLu()));
			apiRequestData.setGpsData(apiGPSData);
			apiRequestDataList.add(apiRequestData);
		}
		return apiRequestDataList;
	}
	
	private AzureApiResponseData[] azureClientPushData(List<AzureApiRequestData> apiRequestDataList) {
		HttpHeaders headers=new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Ocp-Apim-Subscription-Key","553271ec6b124c8bb08e8548ab6372d5");
		HttpEntity<List<AzureApiRequestData>> request=new HttpEntity<>(apiRequestDataList,headers);
		ResponseEntity<AzureApiResponseData[]> responses=restTemplate.postForEntity(ApiConstant.AZURE.API_URL,request,AzureApiResponseData[].class);
		logger.info(responses.getBody());
		return responses.getBody();
	}
	private ClientDataPushReport startClientDataPushReport(String transporter,int count) {
		ClientDataPushReport clientDataPushReport = new ClientDataPushReport();
		clientDataPushReport.setVender(Vender.AZURE_PUSH.name());
		clientDataPushReport.setStatus(ApiConstant.Status.START);
		clientDataPushReport.setCount(count);
		clientDataPushReport.setStartTime(new Date());
		clientDataPushReport.setUsername(transporter);
		mongoTemplate.save(clientDataPushReport);
		return clientDataPushReport;
	}
}
