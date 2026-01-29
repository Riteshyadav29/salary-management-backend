package com.raj.MyApp.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "salaries") // table name in database
public class Salary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String employeeName;
    private Double baseSalary;
    private Double bonus;
    private Double deductions;
    private Double netSalary;

    public Salary() {
    }

    public Salary(String employeeName, Double baseSalary, Double bonus, Double deductions) {
        this.employeeName = employeeName;
        this.baseSalary = baseSalary;
        this.bonus = bonus;
        this.deductions = deductions;
        calculateNetSalary();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public Double getBaseSalary() {
        return baseSalary;
    }

    public void setBaseSalary(Double baseSalary) {
        this.baseSalary = baseSalary;
    }

    public Double getBonus() {
        return bonus;
    }

    public void setBonus(Double bonus) {
        this.bonus = bonus;
    }

    public Double getDeductions() {
        return deductions;
    }

    public void setDeductions(Double deductions) {
        this.deductions = deductions;
    }

    public Double getNetSalary() {
        return netSalary;
    }

    public void setNetSalary(Double netSalary) {
        this.netSalary = netSalary;
    }

    // Automatically calculate net salary before save/update
    @PrePersist
    @PreUpdate
    public void calculateNetSalary() {
        double base = baseSalary != null ? baseSalary : 0;
        double bon = bonus != null ? bonus : 0;
        double ded = deductions != null ? deductions : 0;
        this.netSalary = base + bon - ded;
    }
}
