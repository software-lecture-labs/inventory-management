package com.example.envanter_yonetimi.controller;

import com.example.envanter_yonetimi.model.Employee;
import com.example.envanter_yonetimi.service.EmployeeService;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

// Eğer LoginRequest sınıfın yoksa oluşturacağız:
import com.example.envanter_yonetimi.model.LoginRequest; 


@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<Employee> getAll() {
        return employeeService.getAllUsers();
    }

    @PostMapping("/register")
    public Employee create(@RequestBody Employee employee) {
        return employeeService.createUser(employee);
    }
    
    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private boolean passwordMatches(String rawPassword, String encodedPassword) {
    return passwordEncoder.matches(rawPassword, encodedPassword);
}

    @PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest request) {
    Employee employee = employeeService.findByEmail(request.getEmail());
    if (employee != null && passwordMatches(request.getPassword(), employee.getPassword())) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("fullName", employee.getFirstName() + " " + employee.getLastName());
        response.put("id", employee.getId());
        response.put("role", Map.of(
            "roleId", employee.getRole().getRoleId(),
            "roleName", employee.getRole().getRoleName()
        ));
        response.put("message", "Giriş başarılı");

        return ResponseEntity.ok(response);
    } else {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                             .body(Map.of("success", false, "message", "Email veya şifre hatalı"));
    }
}



}
