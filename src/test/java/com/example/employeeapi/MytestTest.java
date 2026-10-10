package com.example.employeeapi;

import com.example.employeeapi.employee.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MytestTest {
    private final Mytest practice = new Mytest();

    @Test
    void trimsCommaSeparatedValuesAndJoinsThem() {
        assertEquals("te  s,ts,t,ata sta t",
                practice.normalizeCommaSeparated("te  s  ,  ts, t, ata sta t"));
    }

    @Test
    void findsAllLongestSubstringsWithoutRepeatedCharacters() {
        assertEquals(List.of(
                        "abcdefghijkl",
                        "bcdefghijkla",
                        "cdefghijklab",
                        "defghijklabc",
                        "efghijklabcd",
                        "fghijklabcde",
                        "ghijklabcdef"),
                practice.longestUniqueSubstrings("aaabcdefghijklabcdeffff"));
    }

    @Test
    void runsEmployeeSortingExampleWithSalaryTies() {
        List<Employee> employees = List.of(
                new Employee(1, "Alice", new BigDecimal("65000")),
                new Employee(2, "Alice", new BigDecimal("72000")),
                new Employee(3, "Bob", new BigDecimal("72000")),
                new Employee(4, "Alice", new BigDecimal("72000"))
        );

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> practice.abcd(employees));
    }
}
