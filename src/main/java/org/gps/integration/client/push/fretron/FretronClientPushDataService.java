package org.gps.integration.client.push.fretron;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.gps.core.utils.ConversionUtils;
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
import com.tb.core.domain.Vehicle;
import com.tb.core.domain.VenderDetail;
import com.tb.core.domain.service.VehicleService;
import com.tb.core.enums.Vender;
import com.tb.core.enums.VenderDetailFor;

@Service
@Profile("prod")
public class FretronClientPushDataService {
	private static final Logger logger = Logger.getLogger(FretronClientPushDataService.class);
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	@Qualifier("customRestTemplate")
	private RestTemplate restTemplate;

	@Scheduled(fixedRate = 1000 * 60 * 5)
	public void fretronAPiScheduler() {
		logger.info("Fretron API scheduler started");
		List<VenderDetail> venderDetailList = mongoTemplate.find(Query.query(Criteria.where("vender").is(Vender.FRETRON_PUSH.name())), VenderDetail.class);
		logger.info("Fretron API TRANSPORTER SIZE-" + venderDetailList.size());
		for (VenderDetail venderDetail : venderDetailList) {
			List<Vehicle> vehicles = getVehicleList(venderDetail);
			logger.info("FRETRON VEHICLE SIZE-" + vehicles.size());
			ClientDataPushReport clientDataPushReport = startClientDataPushReport(getPushReportUsername(venderDetail),vehicles.size());
			clientDataPushReport.setStatus(ApiConstant.Status.SUCCESS);
			List<FretronApiRequestModel> fretronDataList=new ArrayList<>();
			for (Vehicle vehicle : vehicles) {
				fretronDataList.add(getFretronApiRequestData(vehicle));
			}
			try {
				fretronPushApi(fretronDataList);
				clientDataPushReport.setSuccessCount(fretronDataList.size());
			} catch (Exception e) {
				logger.info(e.getMessage());
				clientDataPushReport.setStatus(ApiConstant.Status.FAIL);
				e.printStackTrace();
			}
			clientDataPushReport.setEndTime(new Date());
			mongoTemplate.save(clientDataPushReport);
			logger.info("Frteron API scheduler Stopped");
		}
	}
	  
	private Object fretronPushApi(List<FretronApiRequestModel> body) {
		  HttpHeaders headers = new HttpHeaders();
		  headers.setContentType(MediaType.APPLICATION_JSON);
		  HttpEntity<List<FretronApiRequestModel>> request = new HttpEntity<>(body,headers);
		  ResponseEntity<Object> response =restTemplate.postForEntity(ApiConstant.FRETRON.API_URL, request, Object.class);
		  logger.info("Fretron API Response-" + response.getBody());
		  return response.getBody();
	  }
	  
	  private ClientDataPushReport startClientDataPushReport(String transporter,int count){
		  ClientDataPushReport clientDataPushReport = new ClientDataPushReport();
		  clientDataPushReport.setVender(Vender.FRETRON_PUSH.name());
		  clientDataPushReport.setStatus(ApiConstant.Status.START);
		  clientDataPushReport.setCount(count); 
		  clientDataPushReport.setStartTime(new Date());
		  clientDataPushReport.setUsername(transporter);
		  mongoTemplate.save(clientDataPushReport);
		  return clientDataPushReport;
	  }
	  
	  private FretronApiRequestModel getFretronApiRequestData(Vehicle vehicle) {
		  FretronApiRequestModel fretronRequestData=new FretronApiRequestModel();
		  fretronRequestData.setVehicle(vehicle.getVehicleNo());
		  fretronRequestData.setImei(vehicle.getDevice().getImei());
		  fretronRequestData.setGpsName("mobilfox");
		  fretronRequestData.setVenderName("vmart");
		  double[] location = vehicle.getTerminalPacketRecord().getLocation();
		  fretronRequestData.setLongitude(ConversionUtils.roundDown6(location[0]));
		  fretronRequestData.setLatitude(ConversionUtils.roundDown6(location[1]));
		  fretronRequestData.setTime(vehicle.getLu().getTime());
		  return fretronRequestData;
	  }
	  
	  private List<Vehicle> getVehicleList(VenderDetail venderDetail) {
			VenderDetailFor detailFor=VenderDetailFor.getByName(venderDetail.getApiFor());
			List<Vehicle> vehicles=new ArrayList<>();
			if(detailFor==null) {
				return vehicles;
			}
			switch (detailFor) {
				case CLIENT:
					vehicles =vehicleService.findByClientUsernameAndActive(venderDetail.getClient(),true);
					break;
				case TRANSPORTER :
					vehicles=vehicleService.findByTransporterUsernameAndActive(venderDetail.getTransporter(),true);
					break;
				case TEMP_USER :
					vehicles=vehicleService.findByUserAndActive(venderDetail.getTempUser(), true);
					break ;
				case VEHICLE:
					vehicles=vehicleService.findByOuidInAndActive(venderDetail.getOuids(), true);
					break;
				default:
					break;
			}
			return vehicles;
		}
	  
	  private String getPushReportUsername(VenderDetail venderDetail) {
			VenderDetailFor detailFor=VenderDetailFor.getByName(venderDetail.getApiFor());
			String username="";
			if(detailFor==null) {
				logger.info("FRETRON API NOT EXECUTE ");
				logger.info(venderDetail);
				return username;
			}
			switch (detailFor) {
				case CLIENT:
					username=venderDetail.getClient();
					logger.info("FRETRON API EXECUTE FOR CLIENT : "+username);
					break;
				case TRANSPORTER :
					username=venderDetail.getTransporter();
					logger.info("FRETRON API EXECUTE FOR TRANSPORTER : "+username);
					break;
				case TEMP_USER :
					username=venderDetail.getTempUser();
					logger.info("FRETRON API EXECUTE FOR USER : "+username);
					break ;
				case VEHICLE:
					username=venderDetail.getTransporter();
					logger.info("FRETRON API EXECUTE FOR TRANSPORTER'S VEHICLE : "+username);
					break;
				default:
					logger.info("FRETRON API NOT EXECUTE ");
					logger.info(venderDetail);
					break;
			}	
			return username;
		}
}