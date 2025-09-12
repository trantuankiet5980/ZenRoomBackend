package vn.edu.iuh.fit.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.iuh.fit.entities.enums.ApartmentCategory;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/enums")
public class EnumController {

    @GetMapping("/apartment-categories")
    public ApartmentCategory[] getApartmentCategories() {
        return ApartmentCategory.values();
    }
}
