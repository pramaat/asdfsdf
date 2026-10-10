package com.example.employeeapi;

import com.example.employeeapi.employee.Employee;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Mytest {
    public static void main(String[] args) {
        Mytest practice = new Mytest();

        String text = "te  s  ,  ts, t, ata sta t";
        System.out.println(practice.normalizeCommaSeparated(text));

        String sample = "aaabcdefghijklabcdeffff";
        System.out.println(practice.longestUniqueSubstrings(sample));

        List<Employee> employees = List.of(
                new Employee(1, "Alice", new BigDecimal("65000")),
                new Employee(2, "Alice", new BigDecimal("72000")),
                new Employee(3, "Bob", new BigDecimal("72000")),
                new Employee(4, "Alice", new BigDecimal("72000"))
        );
        practice.abcd(employees);
    }

    public void abcd(List<Employee> employees) {
        Comparator<Employee> complexSubSort = Comparator
                .comparing(Employee::salary).reversed()
                .thenComparing(Employee::name)
                .thenComparingInt(Employee::id);

        Map<String, Set<Employee>> sortedGroups = employees.stream()
                .collect(Collectors.groupingBy(
                        Employee::name,
                        TreeMap::new,
                        Collectors.toCollection(() -> new TreeSet<>(complexSubSort))
                ));

        sortedGroups.forEach((name, group) -> System.out.println(name + " -> " + group));
    }

    public String normalizeCommaSeparated(String text) {
        return Arrays.stream(text.split(","))
                .map(String::trim)
                .collect(Collectors.joining(","));
    }

    public List<String> longestUniqueSubstrings(String text) {
        int left = 0;
        int maxLength = 0;
        List<String> result = new ArrayList<>();
        Map<Character, Integer> lastSeen = new HashMap<>();

        for (int right = 0; right < text.length(); right++) {
            char current = text.charAt(right);
            if (lastSeen.containsKey(current)) {
                left = Math.max(left, lastSeen.get(current) + 1);
            }
            lastSeen.put(current, right);

            int currentLength = right - left + 1;
            if (currentLength > maxLength) {
                maxLength = currentLength;
                result.clear();
                result.add(text.substring(left, right + 1));
            } else if (currentLength == maxLength) {
                result.add(text.substring(left, right + 1));
            }
        }

        return result;
    }
}
