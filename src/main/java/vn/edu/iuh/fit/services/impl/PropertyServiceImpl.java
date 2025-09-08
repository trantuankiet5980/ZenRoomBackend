package vn.edu.iuh.fit.services.impl;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.dtos.AddressDto;
import vn.edu.iuh.fit.dtos.PropertyDto;
import vn.edu.iuh.fit.entities.Address;
import vn.edu.iuh.fit.entities.Property;
import vn.edu.iuh.fit.entities.RoomType;
import vn.edu.iuh.fit.entities.User;
import vn.edu.iuh.fit.entities.enums.PropertyStatus;
import vn.edu.iuh.fit.entities.enums.PropertyType;
import vn.edu.iuh.fit.repositories.AddressRepository;
import vn.edu.iuh.fit.repositories.PropertyRepository;
import vn.edu.iuh.fit.repositories.RoomTypeRepository;
import vn.edu.iuh.fit.repositories.UserRepository;
import vn.edu.iuh.fit.services.PropertyService;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class PropertyServiceImpl implements PropertyService {

    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final AddressRepository addressRepository;

    public PropertyServiceImpl(PropertyRepository propertyRepository, UserRepository userRepository, RoomTypeRepository roomTypeRepository, AddressRepository addressRepository) {
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.roomTypeRepository = roomTypeRepository;
        this.addressRepository = addressRepository;
    }

    @Transactional
    @Override
    public Property create(PropertyDto dto) {
        if(dto == null) throw new IllegalArgumentException("Request is null");
        if(isBlank(dto.getLandlordId())) throw new IllegalArgumentException("Landlord ID is required");
        if(isBlank(dto.getPropertyType())) throw new IllegalArgumentException("Property type is required");

        User landlord = userRepository.findById(dto.getLandlordId())
                .orElseThrow(() -> new EntityNotFoundException("Landlord not found with ID: " + dto.getLandlordId()));

        PropertyType type = PropertyType.valueOf(dto.getPropertyType().toUpperCase(Locale.ROOT).trim());

        Property p = new Property();
        p.setPropertyId(UUID.randomUUID().toString());
        p.setLandlord(landlord);
        p.setPropertyType(type);
        p.setPropertyName(dto.getPropertyName());
        p.setDescription(dto.getDescription());
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());

        //ADDRESS: dung addressId co san, hoac tao address moi tu dto
        if(!isBlank(dto.getAddressId())){
            Address address = addressRepository.findById(dto.getAddressId())
                    .orElseThrow(() -> new EntityNotFoundException("Address not found with ID: " + dto.getAddressId()));
            p.setAddress(address);
        } else {
            AddressDto a = dto.getAddress();
            if(a == null) throw new IllegalArgumentException("Address information is required");
            Address address = new Address();
            address.setAddressFull(UUID.randomUUID().toString());
            address.setCountryCode(a.getCountryCode());
            address.setProvince(a.getProvince());
            address.setDistrict(a.getDistrict());
            address.setWard(a.getWard());
            address.setStreet(a.getStreet());
            address.setHouseNumber(a.getHouseNumber());
            address.setPostalCode(a.getPostalCode());
            address.setAddressFull(a.getAddressFull());
            address.setLatitude(a.getLatitude());
            address.setLongitude(a.getLongitude());
            addressRepository.save(address);
            p.setAddress(address);
        }

        if(type == PropertyType.BUILDING){
            //BUILDING
            p.setTotalFloors(dto.getTotalFloors());
            p.setParkingCapacity(dto.getParkingCapacity());
            p.setStatus(PropertyStatus.ACTIVE); // mac dinh ACTIVE
        } else {
            //ROOM
            if(isBlank(dto.getParentId()))
                throw new IllegalArgumentException("Parent building ID is required for ROOM type");

            Property parent = propertyRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException("Parent building not found with ID: " + dto.getParentId()));
            p.setParent(parent);

            if(!isBlank(dto.getRoomTypeId())){
                RoomType roomType = roomTypeRepository.findById(dto.getRoomTypeId())
                        .orElseThrow(() -> new EntityNotFoundException("Room type not found with ID: " + dto.getRoomTypeId()));
                p.setRoomType(roomType);
            }
            p.setRoomNumber(dto.getRoomNumber());
            p.setFloorNo(dto.getFloorNo());
            p.setArea(dto.getArea());
            p.setCapacity(dto.getCapacity());
            p.setParkingSlots(dto.getParkingSlots());
            p.setPrice(dto.getPrice());
            p.setDeposit(dto.getDeposit());
            p.setStatus(PropertyStatus.ACTIVE); // mac dinh ACTIVE
        }

        return propertyRepository.save(p);
    }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}
