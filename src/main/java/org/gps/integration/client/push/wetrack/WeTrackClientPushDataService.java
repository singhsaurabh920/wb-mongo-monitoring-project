package org.gps.integration.client.push.wetrack;

import java.util.List;

import org.apache.log4j.Logger;
import org.gps.core.utils.CustomTimezoneUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.tb.core.domain.TerminalPacketRecord;
import com.tb.core.domain.Vehicle;
import com.tb.core.domain.VenderDetail;
import com.tb.core.domain.service.VehicleService;
import com.tb.core.enums.Vender;

@Service
@Profile("prod")
public class WeTrackClientPushDataService {
	private static final Logger logger = Logger.getLogger(WeTrackClientPushDataService.class);
	@Autowired
	MongoTemplate mongoTemplate;
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	@Qualifier("customRestTemplate")
	private RestTemplate restTemplate;

	//@Scheduled(fixedRate=1000 * 60 * 10)
	public void weTrackAPiScheduler() {
		logger.info("WETRACK API SCHEDULER STARTED");
		List<VenderDetail> venderDetailList=mongoTemplate.find(Query.query(Criteria.where("vender").is(Vender.WETRACK_PUSH.name())), VenderDetail.class);
		for (VenderDetail venderDetail : venderDetailList) {
			List<Vehicle> vehicles=vehicleService.findByOuidIn(venderDetail.getOuids());
			logger.info("VEHICLE SIZE   :   "+vehicles.size());
			for (Vehicle vehicle : vehicles) {
				WeTrackApiRequestData data=getWeTrackApiRequestData(vehicle);
				weTrackClientPushData(data);
			}
		}
	}
	
	private WeTrackApiRequestData getWeTrackApiRequestData(Vehicle vehicle) {
		WeTrackApiRequestData data=new WeTrackApiRequestData();
		data.setVehiclenumber(vehicle.getVehicleNo());
		data.setImei(vehicle.getDevice().getImei());
		TerminalPacketRecord tpr=vehicle.getTerminalPacketRecord();
		if(tpr!=null) {
			double[] loc=tpr.getLocation();
			data.setLatitude(String.valueOf(loc[1]));
			data.setLongitude(String.valueOf(loc[0]));
			data.setSpeed(String.valueOf(tpr.getSpeed()));
		}
		data.setOdometer(String.valueOf(vehicle.getTotkm()));
		data.setGpstime(CustomTimezoneUtils.UTCDateToWeTrackDateTimeStringHHmmss(vehicle.getTransporterTz(),vehicle.getLu()));
		if(vehicle.isIgnition()) {
			data.setIgnition("1");
		}else {
			data.setIgnition("0");
		}
		
		return data;
	}
	
	private void weTrackClientPushData(WeTrackApiRequestData data) {
		logger.info(new Gson().toJson(data));
		/*RestTemplate template=new RestTemplate();
		HttpHeaders headers=new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("","");
		HttpEntity<WeTrackApiRequestData> entity=new HttpEntity<>(data,headers);
		ResponseEntity<Object> responseEntity=template.postForEntity(ApiConstant.WETRACK.API_URL,entity,Object.class);*/
	}
}
