package org.gps.integration.scheduler;

import com.tb.core.constants.Fuel;
import com.tb.core.domain.FuelPrice;
import com.tb.core.domain.FuelPump;
import com.tb.core.domain.State;
import com.tb.core.domain.service.FuelPriceService;
import com.tb.core.domain.service.FuelPumpService;
import com.tb.core.enums.TBTimeZone;
import com.tb.core.util.CoreDateTimeUtils;
import com.tb.core.util.CoreUtil;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

//@Component
//@Profile("prod")
public class IoclFuelPriceScheduler {
	private final static Logger logger = Logger.getLogger(IoclFuelPriceScheduler.class);

	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
	private FuelPumpService fuelPumpService;
	@Autowired
	private FuelPriceService fuelPriceService;
	@Autowired
	@Qualifier("scheduledExecutorService")
	private ScheduledExecutorService scheduledExecutorService;

	@PostConstruct
	void init() {
		//scheduledExecutorService.schedule(() -> ioclFuelApi(), 10, TimeUnit.SECONDS);
	}

	public void ioclFuelApi() {
		logger.info("IOCL FUEL PRICE SCRIPT STARTED");
		RestTemplate restTemplate = new RestTemplate();
		List<State> states = mongoTemplate.findAll(State.class);
		logger.info("STATE SIZE- "+states.size());
		for (State state : states) {
			Date date=CoreDateTimeUtils.getSodDateInUserTimeZone(TBTimeZone.UTC.getTimeZoneID());
			Query query=Query.query(Criteria.where("state").is(state.getName()).and("added").is(date));
			FuelPrice fuelPrice=mongoTemplate.findOne(query, FuelPrice.class);
			if(fuelPrice!=null||state.getName().equals("Lakshadweep")) {
				logger.info(state.getName()+" fuel price already added");
				continue;
			}
			MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
			map.add("state", state.getIoclId());
			HttpHeaders headers = new HttpHeaders();
		    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
			ResponseEntity<String> response = restTemplate.postForEntity(Fuel.Iocl.URL + "/StateWiseLocator", request,String.class);
		    logger.info(state.getName()+" response-"+response.getStatusCode());
			if (response.getStatusCode() == HttpStatus.OK) {
				createIoclFuelPriceData(state, response.getBody());
			}
			try {
				Thread.sleep(1000*60*1);
			} catch (InterruptedException e) {
				e.printStackTrace();
				logger.info("Exception- "+e.getMessage());
			}
		}
		logger.info("IOCL FUEL PRICE SCRIPT STOPPED");
	}
	
	private void createIoclFuelPriceData(State state,String body) {
		String[] fps = body.split("\\|", 5000);
		int length=(fps.length)-1;
		logger.info(state.getName()+" fuel price size- "+length);
		if(length==0) {
			logger.info(state.getName()+"- fuel price not saved "+body);
			return;
		}
		for (int i = 0; i < length; i++) {
			List<String> fp = Arrays.asList(fps[i].split("\\,", 50000));
			double lng=Double.parseDouble(fp.get(2).trim());
			double lat=Double.parseDouble(fp.get(1).trim());
			if(!CoreUtil.isValidLatLng(lat,lng)) {
				logger.info("NOT VALID- "+fp);
				continue;
			}
			String name=fp.get(0).trim();
			GeoJsonPoint point = new GeoJsonPoint(lng,lat);
			FuelPump fuelPump=fuelPumpService.findByNameAndPoint(name,point);
			if(fuelPump==null) {
				//fuelPump=fuelPumpService.findByNameAndNearestPoint(name, point);
				if(fuelPump==null) {
					fuelPump=saveIoclFuelPumpData(state,fp);
					logger.info("New fuel pump- "+fuelPump);
				}
			}
			FuelPrice fuelPrice=new FuelPrice();
			fuelPrice.setType("IOCL");
			fuelPrice.setState(state.getName());
            //
            //
			fuelPrice.setPetrol(fp.get(25));
			fuelPrice.setDiesel(fp.get(26));
			fuelPrice.setXpPetrol(fp.get(27));
			fuelPrice.setXpDiesel(fp.get(28));
			//
			fuelPrice.setFuelPump(fuelPump.getId());
			fuelPrice.setAdded(CoreDateTimeUtils.getSodDateInUserTimeZone(TBTimeZone.UTC.getTimeZoneID()));
			fuelPrice.setUpdated(CoreDateTimeUtils.getSodDateInUserTimeZone(TBTimeZone.UTC.getTimeZoneID()));
			try {
			    fuelPriceService.save(fuelPrice);
			} catch (Exception e) {
				logger.info("Exception- "+e.getMessage());
				continue;
			}
			fuelPump.setFuelPrice(fuelPrice);
			fuelPump.setUpdated(CoreDateTimeUtils.getSodDateInUserTimeZone(TBTimeZone.UTC.getTimeZoneID()));
			fuelPumpService.save(fuelPump);
			//logger.info(fuelPrice);
		}
		logger.info(state.getName()+" fuel price saved");
	}
	
	private final void createIoclFuelPumpData(State state, String body) {
		String[] fps = body.split("\\|", 5000);
		int length=(fps.length)-1;
		logger.info(state.getName()+" fuel pump size- "+length);
		for (int i = 0; i < length; i++) {
			List<String> fp = Arrays.asList(fps[i].split("\\,", 50000));
			saveIoclFuelPumpData(state,fp);
		}
		logger.info(state.getName()+" fuel pump saved");
	}
	
	private final FuelPump saveIoclFuelPumpData(State state,List<String> fp) {
		double lng=Double.parseDouble(fp.get(2).trim());
		double lat=Double.parseDouble(fp.get(1).trim());
		if(!CoreUtil.isValidLatLng(lat,lng)) {
			logger.info("NOT VALID- "+fp);
			return null;
		}
		String name=fp.get(0).trim();
		GeoJsonPoint point = new GeoJsonPoint(lng,lat);
		FuelPump fuelPump = new FuelPump();
		fuelPump.setType("IOCL");
		fuelPump.setState(state.getName());
        //
		fuelPump.setName(name);
		fuelPump.setGeoJsonPoint(point);
		fuelPump.setOwner(fp.get(30).trim());
		fuelPump.setPhoneNumber(fp.get(29).trim());
		//
		fuelPump.setAdded(CoreDateTimeUtils.getSodDateInUserTimeZone(TBTimeZone.UTC.getTimeZoneID()));
		fuelPump.setUpdated(CoreDateTimeUtils.getSodDateInUserTimeZone(TBTimeZone.UTC.getTimeZoneID()));
		return fuelPumpService.save(fuelPump);
	}
}
