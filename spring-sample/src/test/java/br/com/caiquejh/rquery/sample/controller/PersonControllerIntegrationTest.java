package br.com.caiquejh.rquery.sample.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PersonControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldListPersonsWithFilter() throws Exception {
        mockMvc.perform(get("/persons")
                        .param("filter", "address.city = 'Tabor'"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value("1"));

        mockMvc.perform(get("/persons")
                        .param("filter", "gender in ('NON_BINARY', 'AGENDER')"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value("3"))
                .andExpect(jsonPath("$.content[0].gender").value("AGENDER"))
                .andExpect(jsonPath("$.content[1].gender").value("NON_BINARY"))
                .andExpect(jsonPath("$.content[2].gender").value("AGENDER"));
    }
}