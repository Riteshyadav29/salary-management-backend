package com.raj.MyApp.Controller;

import com.raj.MyApp.Model.Salary;
import com.raj.MyApp.Repository.SalaryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/salaries")
@Tag(name = "Salary API", description = "Salary Management Operations")
public class SalaryController {

    private static final Logger logger = LoggerFactory.getLogger(SalaryController.class);

    @Autowired
    private SalaryRepository salaryRepository;

    @Operation(summary = "Get all salaries")
    @GetMapping
    public ResponseEntity<List<Salary>> getAllSalaries() {
        try {
            logger.info("Fetching all salaries");
            return ResponseEntity.ok(salaryRepository.findAll());
        } catch (Exception e) {
            logger.error("Error fetching salaries", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Get salary by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Salary> getSalaryById(@PathVariable Long id) {
        try {
            logger.info("Fetching salary with ID: {}", id);
            return salaryRepository.findById(id)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error fetching salary by ID {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Create new salary")
    @PostMapping
    public ResponseEntity<Salary> createSalary(@RequestBody Salary salary) {
        try {
            salary.calculateNetSalary();
            Salary saved = salaryRepository.save(salary);
            logger.info("Created salary with ID: {}", saved.getId());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("Error creating salary", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Update salary by ID")
    @PutMapping("/{id}")
    public ResponseEntity<Salary> updateSalary(@PathVariable Long id, @RequestBody Salary updatedSalary) {
        try {
            return salaryRepository.findById(id).map(existing -> {
                existing.setEmployeeName(updatedSalary.getEmployeeName());
                existing.setBaseSalary(updatedSalary.getBaseSalary());
                existing.setBonus(updatedSalary.getBonus());
                existing.setDeductions(updatedSalary.getDeductions());
                existing.calculateNetSalary();
                Salary saved = salaryRepository.save(existing);
                logger.info("Updated salary ID: {}", id);
                return ResponseEntity.ok(saved);
            }).orElseGet(() -> {
                updatedSalary.setId(id);
                updatedSalary.calculateNetSalary();
                Salary saved = salaryRepository.save(updatedSalary);
                logger.info("Created new salary while updating ID: {}", id);
                return ResponseEntity.ok(saved);
            });
        } catch (Exception e) {
            logger.error("Error updating salary with ID {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Partially update salary")
    @PatchMapping("/{id}")
    public ResponseEntity<Salary> partiallyUpdateSalary(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        try {
            return salaryRepository.findById(id).map(salary -> {
                updates.forEach((key, value) -> {
                    switch (key) {
                        case "employeeName" -> salary.setEmployeeName((String) value);
                        case "baseSalary" -> salary.setBaseSalary(Double.valueOf(value.toString()));
                        case "bonus" -> salary.setBonus(Double.valueOf(value.toString()));
                        case "deductions" -> salary.setDeductions(Double.valueOf(value.toString()));
                        default -> logger.warn("Unknown field: {}", key);
                    }
                });
                salary.calculateNetSalary();
                Salary updated = salaryRepository.save(salary);
                logger.info("Partially updated salary ID: {}", id);
                return ResponseEntity.ok(updated);
            }).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error partially updating salary ID {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @Operation(summary = "Delete salary by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSalary(@PathVariable Long id) {
        try {
            if (salaryRepository.existsById(id)) {
                salaryRepository.deleteById(id);
                logger.info("Deleted salary ID: {}", id);
                return ResponseEntity.noContent().build();
            } else {
                logger.warn("Salary not found ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting salary ID {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
