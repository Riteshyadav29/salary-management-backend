package com.raj.MyApp.Specification;

import com.raj.MyApp.Model.Salary;
import org.springframework.data.jpa.domain.Specification;

public class SalarySpecification {

    private SalarySpecification() {
    }

    public static Specification<Salary> hasEmployeeNameLike(String search) {
        return (root, query, cb) -> cb.like(
                cb.lower(root.get("employeeName")),
                "%" + search.toLowerCase() + "%"
        );
    }

    public static Specification<Salary> hasMinBaseSalary(Double minBaseSalary) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("baseSalary"), minBaseSalary);
    }

    public static Specification<Salary> hasMaxBaseSalary(Double maxBaseSalary) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("baseSalary"), maxBaseSalary);
    }

    public static Specification<Salary> hasMinNetSalary(Double minNetSalary) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("netSalary"), minNetSalary);
    }

    public static Specification<Salary> hasMaxNetSalary(Double maxNetSalary) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("netSalary"), maxNetSalary);
    }
}