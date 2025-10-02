package vn.edu.iuh.fit.services;

import vn.edu.iuh.fit.dtos.AddressDto;

public interface GeocodingService {
    double[] getLatLngFromAddress(String fullAddress);
}
