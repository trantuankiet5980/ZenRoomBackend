package vn.edu.iuh.fit.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.dtos.role.RoleCreateRequest;
import vn.edu.iuh.fit.dtos.role.RoleResponse;
import vn.edu.iuh.fit.dtos.role.RoleUpdateRequest;
import vn.edu.iuh.fit.services.RoleService;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<RoleResponse> create(@RequestBody RoleCreateRequest role) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(role));
    }
    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(roleService.getById(id));
    }

    @GetMapping
    public ResponseEntity<Page<RoleResponse>> list(
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size){
        return ResponseEntity.ok(roleService.list(PageRequest.of(page,size)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> update(@PathVariable String id, @RequestBody RoleUpdateRequest role) {
        return ResponseEntity.ok(roleService.update(id, role));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        roleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
