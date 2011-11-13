// Adapted from the spring oxm test - Thanks!

package org.jibx.test;

import java.util.ArrayList;

public class Flights {

    protected ArrayList flightList = new ArrayList();

    public void addFlight(FlightType flight) {
        flightList.add(flight);
    }

    public FlightType getFlight(int index) {
        return (FlightType) flightList.get(index);
    }

    public int sizeFlightList() {
        return flightList.size();
    }
}
