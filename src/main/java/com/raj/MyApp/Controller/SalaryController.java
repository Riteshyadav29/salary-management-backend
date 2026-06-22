package com.raj.MyApp.Controller;

import com.raj.MyApp.Model.Salary;
import com.raj.MyApp.Repository.SalaryRepository;
import com.raj.MyApp.Specification.SalarySpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/salaries")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class          SalaryController {

    private static final Logger logger = LoggerFactory.getLogger(SalaryController.class);

    @Autowired
    private SalaryRepository salaryRepository;

    @GetMapping
    public ResponseEntity<Page<Salary>> getAllSalaries(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Double minBaseSalary,
            @RequestParam(required = false) Double maxBaseSalary,
            @RequestParam(required = false) Double minNetSalary,
            @RequestParam(required = false) Double maxNetSalary,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        validateFilterRanges(minBaseSalary, maxBaseSalary, minNetSalary, maxNetSalary);

        logger.info("Fetching salaries with filters search={}, minBaseSalary={}, maxBaseSalary={}, minNetSalary={}, maxNetSalary={}, page={}, size={}, sortBy={}, sortDir={}",
                search, minBaseSalary, maxBaseSalary, minNetSalary, maxNetSalary, page, size, sortBy, sortDir);

        Specification<Salary> specification = buildSpecification(search, minBaseSalary, maxBaseSalary, minNetSalary, maxNetSalary);
        // role-based restriction: if the user is an EMPLOYEE, restrict to their own records
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().endsWith("EMPLOYEE") || a.getAuthority().endsWith("ROLE_EMPLOYEE"))) {
            String username = auth.getName();
            specification = specification.and((root, query, cb) -> cb.equal(root.get("employeeName"), username));
        }
        Pageable pageable = buildPageable(page, size, sortBy, sortDir);

        return ResponseEntity.ok(salaryRepository.findAll(specification, pageable));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportSalariesToCsv(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Double minBaseSalary,
            @RequestParam(required = false) Double maxBaseSalary,
            @RequestParam(required = false) Double minNetSalary,
            @RequestParam(required = false) Double maxNetSalary,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        validateFilterRanges(minBaseSalary, maxBaseSalary, minNetSalary, maxNetSalary);

        Specification<Salary> specification = buildSpecification(search, minBaseSalary, maxBaseSalary, minNetSalary, maxNetSalary);
        Sort sort = buildSort(sortBy, sortDir);
        List<Salary> salaries = salaryRepository.findAll(specification, sort);

        String csv = buildCsv(salaries);
        byte[] csvBytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=salaries.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }

    private Specification<Salary> buildSpecification(
            String search,
            Double minBaseSalary,
            Double maxBaseSalary,
            Double minNetSalary,
            Double maxNetSalary
    ) {
        Specification<Salary> specification = (root, query, cb) -> cb.conjunction();

        if (search != null && !search.isBlank()) {
            specification = specification.and(SalarySpecification.hasEmployeeNameLike(search));
        }
        if (minBaseSalary != null) {
            specification = specification.and(SalarySpecification.hasMinBaseSalary(minBaseSalary));
        }
        if (maxBaseSalary != null) {
            specification = specification.and(SalarySpecification.hasMaxBaseSalary(maxBaseSalary));
        }
        if (minNetSalary != null) {
            specification = specification.and(SalarySpecification.hasMinNetSalary(minNetSalary));
        }
        if (maxNetSalary != null) {
            specification = specification.and(SalarySpecification.hasMaxNetSalary(maxNetSalary));
        }
        return specification;
    }

    private Pageable buildPageable(int page, int size, String sortBy, String sortDir) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page cannot be negative");
        }
        if (size <= 0 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Size must be between 1 and 100");
        }
        return PageRequest.of(page, size, buildSort(sortBy, sortDir));
    }

    private Sort buildSort(String sortBy, String sortDir) {
        if (!("id".equals(sortBy) || "employeeName".equals(sortBy) || "baseSalary".equals(sortBy) || "netSalary".equals(sortBy))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sortBy field");
        }

        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sortDir must be asc or desc");
        }

        return Sort.by(direction, sortBy);
    }

    private void validateFilterRanges(Double minBaseSalary, Double maxBaseSalary, Double minNetSalary, Double maxNetSalary) {
        if (minBaseSalary != null && maxBaseSalary != null && minBaseSalary > maxBaseSalary) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minBaseSalary cannot be greater than maxBaseSalary");
        }
        if (minNetSalary != null && maxNetSalary != null && minNetSalary > maxNetSalary) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minNetSalary cannot be greater than maxNetSalary");
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<Salary> getSalaryById(@PathVariable Long id) {
        logger.info("Fetching salary with ID: {}", id);
        return salaryRepository.findById(id)
                .map(salary -> {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().endsWith("EMPLOYEE") || a.getAuthority().endsWith("ROLE_EMPLOYEE"))) {
                        String username = auth.getName();
                        if (!username.equals(salary.getEmployeeName())) {
                            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
                        }
                    }
                    return ResponseEntity.ok(salary);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salary not found"));
    }

    @PostMapping
    public ResponseEntity<Salary> createSalary(@Valid @RequestBody Salary salary) {
        // If an EMPLOYEE is creating a salary, force the employeeName to their username
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().endsWith("EMPLOYEE") || a.getAuthority().endsWith("ROLE_EMPLOYEE"))) {
            salary.setEmployeeName(auth.getName());
        }

        salary.calculateNetSalary();
        Salary saved = salaryRepository.save(salary);
        logger.info("Created salary with ID: {}", saved.getId());
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Salary> updateSalary(@PathVariable Long id, @Valid @RequestBody Salary updatedSalary) {
        return salaryRepository.findById(id).map(existing -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isEmployee = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().endsWith("EMPLOYEE") || a.getAuthority().endsWith("ROLE_EMPLOYEE"));
            if (isEmployee && !auth.getName().equals(existing.getEmployeeName())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }

            // If employee is updating, ensure employeeName cannot be changed to another user
            if (isEmployee) {
                existing.setEmployeeName(auth.getName());
            } else {
                existing.setEmployeeName(updatedSalary.getEmployeeName());
            }

            existing.setBaseSalary(updatedSalary.getBaseSalary());
            existing.setBonus(updatedSalary.getBonus());
            existing.setDeductions(updatedSalary.getDeductions());
            existing.calculateNetSalary();
            Salary saved = salaryRepository.save(existing);
            logger.info("Updated salary ID: {}", id);
            return ResponseEntity.ok(saved);
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salary not found"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Salary> partiallyUpdateSalary(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        return salaryRepository.findById(id).map(salary -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isEmployee = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().endsWith("EMPLOYEE") || a.getAuthority().endsWith("ROLE_EMPLOYEE"));
            if (isEmployee && !auth.getName().equals(salary.getEmployeeName())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }

            updates.forEach((key, value) -> {
                switch (key) {
                    case "employeeName" -> {
                        if (isEmployee) {
                            // ignore attempts by employee to change owner
                            logger.warn("Employee attempted to change employeeName");
                        } else {
                            salary.setEmployeeName((String) value);
                        }
                    }
                    case "baseSalary" -> salary.setBaseSalary(Double.valueOf(value.toString()));
                    case "bonus" -> salary.setBonus(Double.valueOf(value.toString()));
                    case "deductions" -> salary.setDeductions(Double.valueOf(value.toString()));
                    default -> logger.warn("Unknown field: {}", key);
                }
            });

            if (salary.getBaseSalary() != null && salary.getBaseSalary() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Base salary cannot be negative");
            }
            if (salary.getBonus() != null && salary.getBonus() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bonus cannot be negative");
            }
            if (salary.getDeductions() != null && salary.getDeductions() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deductions cannot be negative");
            }
            if (salary.getEmployeeName() == null || salary.getEmployeeName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee name is required");
            }

            salary.calculateNetSalary();
            Salary updated = salaryRepository.save(salary);
            logger.info("Partially updated salary ID: {}", id);
            return ResponseEntity.ok(updated);
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salary not found"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteSalary(@PathVariable Long id) {
        return salaryRepository.findById(id).map(salary -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isEmployee = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().endsWith("EMPLOYEE") || a.getAuthority().endsWith("ROLE_EMPLOYEE"));
            if (isEmployee && !auth.getName().equals(salary.getEmployeeName())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }

            salaryRepository.deleteById(id);
            logger.info("Deleted salary ID: {}", id);
            return ResponseEntity.noContent().build();
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Salary not found"));
    }

    private String buildCsv(List<Salary> salaries) {
        StringBuilder builder = new StringBuilder();
        builder.append("Id,Employee Name,Base Salary,Bonus,Deductions,Net Salary\n");

        for (Salary salary : salaries) {
            builder
                    .append(salary.getId() != null ? salary.getId() : "")
                    .append(',')
                    .append(escapeCsvField(salary.getEmployeeName()))
                    .append(',')
                    .append(salary.getBaseSalary() != null ? salary.getBaseSalary() : "")
                    .append(',')
                    .append(salary.getBonus() != null ? salary.getBonus() : "")
                    .append(',')
                    .append(salary.getDeductions() != null ? salary.getDeductions() : "")
                    .append(',')
                    .append(salary.getNetSalary() != null ? salary.getNetSalary() : "")
                    .append('\n');
        }

        return builder.toString();
    }

    private String escapeCsvField(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
