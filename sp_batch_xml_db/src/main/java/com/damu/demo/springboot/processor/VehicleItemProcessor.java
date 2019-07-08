package com.damu.demo.springboot.processor;
/**
 * @author Damodhara Palavali
 *
 */
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import com.damu.demo.springboot.model.Vehicle;

public class VehicleItemProcessor implements ItemProcessor<Vehicle, Vehicle> {
	Logger logger = LoggerFactory.getLogger(VehicleItemProcessor.class);

	private String processingJobid;
	/**
	 * holds theProcessing File Name
	 */
	private String processingFileName;
	/**
	 * @return the processingJobid
	 */
	public String getProcessingJobid() {
		return processingJobid;
	}

	/**
	 * @param processingJobid the processingJobid to set
	 */
	public void setProcessingJobid(String processingJobid) {
		this.processingJobid = processingJobid;
	}

	/**
	 * @return the processingFileName
	 */
	public String getProcessingFileName() {
		return processingFileName;
	}

	/**
	 * @param processingFileName the processingFileName to set
	 */
	public void setProcessingFileName(String processingFileName) {
		this.processingFileName = processingFileName;
	}
	@Override
	public Vehicle process(Vehicle factoryFeedVehicle) throws Exception {
		
		return factoryFeedVehicle;
	} 
}
