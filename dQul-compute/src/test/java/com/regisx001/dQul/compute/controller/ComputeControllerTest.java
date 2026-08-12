package com.regisx001.dQul.compute.controller;

import com.regisx001.dQul.compute.service.SparkComputeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ComputeController.class)
class ComputeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SparkComputeService computeService;

    @Test
    void getHealth_shouldReturnSparkStatus() throws Exception {
        given(computeService.isSparkActive()).willReturn(true);
        given(computeService.getSparkVersion()).willReturn("3.5.1");

        mockMvc.perform(get("/api/v1/compute/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("dQul-compute"))
                .andExpect(jsonPath("$.sparkActive").value(true))
                .andExpect(jsonPath("$.sparkVersion").value("3.5.1"));
    }
}
