import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EmployeeMain {
    public static void main(String[] args) {
        List<Employee> employees = new ArrayList<>();
        employees.add(new Employee(101, "Alice", 25000));
        employees.add(new Employee(102, "Bob", 50000));
        employees.add(new Employee(103, "Charlie", 32000));
        employees.add(new Employee(104, "Diana", 45000));
        employees.add(new Employee(105, "Evan", 60000));

        Employee secondHighest = findSecondHighestSalaryEmployee(employees);

        if (secondHighest != null) {
            System.out.println("Employee with second highest salary: " + secondHighest);
        } else {
            System.out.println("There is no employee with second highest salary.");
        }
    }

    public static Employee findSecondHighestSalaryEmployee(List<Employee> employees) {
        if (employees == null || employees.size() < 2) {
            return null;
        }

        return employees.stream()
                .sorted(Comparator.comparingDouble(Employee::getSalary).reversed())
                .skip(1)
                .findFirst()
                .orElse(null);
    }
}
