package com.example.employeeapi.employee;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
@Import(EmployeeControllerTest.TestEventsConfiguration.class)
class EmployeeControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearEmployees() {
        employeeService.findAll().forEach(employee -> employeeService.delete(employee.id()));
    }

    @Test
    void createAndReadEmployee() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/employees")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","salary":50000.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/employees/")))
                .andExpect(jsonPath("$.id", greaterThan(0)))
                .andExpect(jsonPath("$.name", is("Alice")))
                .andExpect(jsonPath("$.salary", is(50000.0)))
                .andReturn();

        Employee employee = objectMapper.readValue(result.getResponse().getContentAsString(), Employee.class);
        String storedSalary = jdbcTemplate.queryForObject(
                "SELECT encrypted_salary FROM employees WHERE id = ?", String.class, employee.id());
        org.junit.jupiter.api.Assertions.assertTrue(storedSalary.startsWith("v1."));
        org.junit.jupiter.api.Assertions.assertFalse(storedSalary.contains("50000"));

        mockMvc.perform(get("/api/employees/{id}", employee.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alice")));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", greaterThan(0)));
    }

    @Test
    void updateAndDeleteEmployee() throws Exception {
        Employee employee = employeeService.create(new EmployeeRequest("Alice", new java.math.BigDecimal("50000")));

        mockMvc.perform(put("/api/employees/{id}", employee.id())
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alicia","salary":60000.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alicia")))
                .andExpect(jsonPath("$.salary", is(60000.0)));

        mockMvc.perform(delete("/api/employees/{id}", employee.id())
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/employees/{id}", employee.id()))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundForMissingEmployee() throws Exception {
        mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", is("Employee 99 was not found")))
                .andExpect(jsonPath("$.title", is("Not Found")));

        mockMvc.perform(put("/api/employees/99")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Alice","salary":50000}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", is("Employee 99 was not found")));

        mockMvc.perform(delete("/api/employees/99")
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", is("Employee 99 was not found")));
    }

    @Test
    void rejectsInvalidEmployeeRequest() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","salary":-1}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsFieldErrorsForInvalidEmployeeAndProblemDetailForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/employees")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","salary":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")))
                .andExpect(jsonPath("$.detail", is("The request contains invalid fields")))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.salary").exists());

        mockMvc.perform(post("/api/employees")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", is("The request body is missing or malformed")));
    }

    @TestConfiguration
    static class TestEventsConfiguration {
        @Bean
        @Primary
        EmployeeEventPublisher testEmployeeEventPublisher() {
            return event -> { };
        }
    }
}
