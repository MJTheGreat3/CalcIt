package com.calcit.controller;

import com.calcit.exception.GlobalExceptionHandler;
import com.calcit.service.CalculatorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CalculatorController.class)
@Import({CalculatorService.class, GlobalExceptionHandler.class})
class CalculatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAddTwoNumbers() throws Exception {
        mockMvc.perform(get("/api/v1/add").param("a", "2").param("b", "3"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.operation").value("add"))
            .andExpect(jsonPath("$.a").value(2.0))
            .andExpect(jsonPath("$.b").value(3.0))
            .andExpect(jsonPath("$.result").value(5.0));
    }

    @Test
    void shouldSubtractTwoNumbers() throws Exception {
        mockMvc.perform(get("/api/v1/subtract").param("a", "7").param("b", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.operation").value("subtract"))
            .andExpect(jsonPath("$.result").value(5.0));
    }

    @Test
    void shouldMultiplyTwoNumbers() throws Exception {
        mockMvc.perform(get("/api/v1/multiply").param("a", "4").param("b", "3"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.operation").value("multiply"))
            .andExpect(jsonPath("$.result").value(12.0));
    }

    @Test
    void shouldDivideTwoNumbers() throws Exception {
        mockMvc.perform(get("/api/v1/divide").param("a", "10").param("b", "4"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.operation").value("divide"))
            .andExpect(jsonPath("$.result").value(2.5));
    }

    @Test
    void shouldReturnBadRequestForDivisionByZero() throws Exception {
        mockMvc.perform(get("/api/v1/divide").param("a", "10").param("b", "0"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Division by zero is not allowed"));
    }

    @Test
    void shouldReturnBadRequestForMissingParameter() throws Exception {
        mockMvc.perform(get("/api/v1/add").param("a", "2"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Missing required query parameter: b"));
    }

    @Test
    void shouldReturnBadRequestForNonNumericParameter() throws Exception {
        mockMvc.perform(get("/api/v1/multiply").param("a", "abc").param("b", "3"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Parameter 'a' must be a valid number"));
    }

    @Test
    void shouldReturnBadRequestForNonFiniteParameter() throws Exception {
        mockMvc.perform(get("/api/v1/add").param("a", "NaN").param("b", "3"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Parameters must be finite numbers"));
    }
}
