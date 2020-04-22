package org.gps.integration.client.push;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import com.tb.core.constants.ApiConstant;
import com.tb.core.domain.ClientDataPushReport;
import com.tb.core.domain.Vehicle;
import com.tb.core.domain.VenderDetail;
import com.tb.core.domain.service.VehicleService;
import com.tb.core.enums.VenderDetailFor;


@Service
public class ApiDataService {
	@Autowired
	private VehicleService vehicleService;
	@Autowired
	private MongoTemplate mongoTemplate;
	
	public List<Vehicle> getVehicleList(VenderDetail venderDetail) {
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
	
	public String getPushReportUsername(VenderDetail venderDetail) {
		VenderDetailFor detailFor=VenderDetailFor.getByName(venderDetail.getApiFor());
		String username=null;
		if(detailFor==null) {
			return username;
		}
		switch (detailFor) {
			case CLIENT:
				username=venderDetail.getClient();
				break;
			case TRANSPORTER :
				username=venderDetail.getTransporter();
				break;
			case TEMP_USER :
				username=venderDetail.getTempUser();
				break ;
			case VEHICLE:
				username=venderDetail.getTransporter();
				break;
			default:
				break;
		}	
		return username;
	}
	
	 public ClientDataPushReport startClientDataPushReport(String transporter,int count,String vender){
		  ClientDataPushReport clientDataPushReport = new ClientDataPushReport();
		  clientDataPushReport.setVender(vender);
		  clientDataPushReport.setStatus(ApiConstant.Status.START);
		  clientDataPushReport.setCount(count); 
		  clientDataPushReport.setStartTime(new Date());
		  clientDataPushReport.setUsername(transporter);
		  mongoTemplate.save(clientDataPushReport);
		  return clientDataPushReport;
	  }
}
