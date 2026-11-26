package com.example.envanter_yonetimi.service;

import com.example.envanter_yonetimi.model.Employee;
import com.example.envanter_yonetimi.model.Role;
import com.example.envanter_yonetimi.repository.EmployeeRepository;
import com.example.envanter_yonetimi.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EmployeeService {

    @Autowired
    private final EmployeeRepository employeeRepository;

    @Autowired
    private final RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository, RoleRepository roleRepository) {
        this.employeeRepository = employeeRepository;
        this.roleRepository = roleRepository;
    }

    public List<Employee> getAllUsers() {
        return employeeRepository.findAll();
    }

    public Employee createUser(Employee employee) {
        if (employee.getRole() != null && employee.getRole().getRoleId() != null) {
            Role role = roleRepository.findById(employee.getRole().getRoleId()).orElse(null);
            employee.setRole(role);
        }
        // Şifreyi hashle
        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        return employeeRepository.save(employee);
    }

    public boolean login(String email, String rawPassword) {
        Employee user = employeeRepository.findByEmail(email);
        if (user != null) {
            return passwordEncoder.matches(rawPassword, user.getPassword());
        }
        return false;
    }

    public Employee findByEmail(String email) {
        return employeeRepository.findByEmail(email);
    }
}
