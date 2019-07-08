package com.damu.demo.springboot.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.batch.item.database.ItemPreparedStatementSetter;

import com.damu.demo.springboot.model.Vehicle;

public class VehiclePreparedSmtSetter implements ItemPreparedStatementSetter<Vehicle> {
    @Override
    public void setValues(Vehicle Vehicle, 
                          PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setString(1, Vehicle.getVehiclenumber());
        preparedStatement.setString(2, Vehicle.getVehicleInfo().getBrand());
        preparedStatement.setString(3, Vehicle.getVehicleInfo().getCountry());
        preparedStatement.setString(4, Vehicle.getVehicleInfo().getModelname());
        preparedStatement.setString(5,  Vehicle.getVehicleInfo().getModelyear());
    }
}
