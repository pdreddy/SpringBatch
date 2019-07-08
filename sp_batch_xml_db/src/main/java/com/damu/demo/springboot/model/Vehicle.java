package com.damu.demo.springboot.model;
/**
 * @author Damodhara Palavali
 *
 */
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;
@Data
@XmlRootElement(name="Vehicle")
@XmlAccessorType(XmlAccessType.FIELD)
public class Vehicle {
	@XmlElement(name="vehiclenumber")
	private String vehiclenumber;
	
	@XmlElement(name="VehicleInformation")
	private VehicleInformation vehicleInfo;
}
