package com.cloudcast.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@TestPropertySource(properties = {
        "openweather.api.key=test-key-for-testing",
        "openweather.api.base-url=https://api.openweathermap.org/data/2.5"
})
class WeatherControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc() {
        return MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void missingCityAndCoords_returnsBadRequest() throws Exception {
        mockMvc().perform(get("/api/weather"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Provide either")));
    }

    @Test
    void missingCityAndCoords_forecast_returnsBadRequest() throws Exception {
        mockMvc().perform(get("/api/forecast"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Provide either")));
    }
}
