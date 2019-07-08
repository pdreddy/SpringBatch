package com.damu.demo.springboot.model;
/**
 * @author Damodhara Palavali
 *
 */
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class VehicleInformation {
	@XmlElement(name="brand")
	private String brand;
	@XmlElement(name="country")
	private String country;
	@XmlElement(name="modelname")
	private String modelname;
	@XmlElement(name="modelyear")
	private String modelyear;
	
}
