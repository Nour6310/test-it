package com.example.testit;

import com.example.testit.adapter.member.CurrentMemberServiceFake;
import com.example.testit.model.Member;
import com.example.testit.repository.MemberRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@SpringBootTest
@AutoConfigureMockMvc
class BookControllerIT {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    CurrentMemberServiceFake currentMemberService;

    @Test
    void creerUnLivrePuisLEmprunter() throws Exception {
        // given : yanis est connecte
        var yanis = memberRepository.save(new Member("yanis"));
        currentMemberService.setCurrent(yanis.getId());

        // when : on cree un livre
        var creation = mockMvc.perform(MockMvcRequestBuilders.post("/books").content("""
                {
                    "title":"Notre-Dame de Paris",
                    "author":"Victor Hugo"
                }
                """).contentType(MediaType.APPLICATION_JSON))
                // then : le livre est cree avec le bon titre et le bon auteur
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Notre-Dame de Paris"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.author").value("Victor Hugo"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("DISPONIBLE"))
                .andReturn();

        Number id = JsonPath.read(creation.getResponse().getContentAsString(), "$.id");

        // when : yanis emprunte le livre
        mockMvc.perform(MockMvcRequestBuilders.post("/books/" + id + "/borrow"))
                // then : l'emprunt est accepte
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("EMPRUNTE"));
    }
}
