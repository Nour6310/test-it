package com.example.testit;

import com.example.testit.adapter.member.CurrentMemberServiceFake;
import com.example.testit.model.Member;
import com.example.testit.model.Status;
import com.example.testit.repository.BookRepository;
import com.example.testit.repository.MemberRepository;
import com.jayway.jsonpath.JsonPath;
import org.assertj.core.api.Assertions;
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
    BookRepository bookRepository;

    @Autowired
    CurrentMemberServiceFake currentMemberService;

    // ---------- Exercice 4 ----------

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

    // ---------- Bonus ----------

    @Test
    void modifierUnLivreEmprunteNeChangePasSonStatut() throws Exception {
        // given : ines a emprunte un livre
        var ines = memberRepository.save(new Member("ines"));
        currentMemberService.setCurrent(ines.getId());
        var id = creerLivre("Le Rouge et le Noir", "Stendhal");
        mockMvc.perform(MockMvcRequestBuilders.post("/books/" + id + "/borrow"))
                .andExpect(MockMvcResultMatchers.status().isOk());

        // when : on corrige le titre du livre
        mockMvc.perform(MockMvcRequestBuilders.put("/books/" + id).content("""
                {
                    "title":"Le Rouge et le Noir (edition poche)",
                    "author":"Stendhal"
                }
                """).contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk());

        // then : le titre change mais le livre est toujours EMPRUNTE
        var livreEnBase = bookRepository.findById(id.longValue()).orElseThrow();
        Assertions.assertThat(livreEnBase.getTitle()).isEqualTo("Le Rouge et le Noir (edition poche)");
        Assertions.assertThat(livreEnBase.getStatus()).isEqualTo(Status.EMPRUNTE);
    }

    @Test
    void supprimerUnLivreQuiNExistePasRenvoie404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/books/999999"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void emprunterSansEtreConnecteRenvoie401() throws Exception {
        // given : un livre existe, mais personne n'est connecte
        var id = creerLivre("Bel-Ami", "Maupassant");
        currentMemberService.setCurrent(null);

        // when / then
        mockMvc.perform(MockMvcRequestBuilders.post("/books/" + id + "/borrow"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }

    @Test
    void creerUnLivreSansTitreOuSansAuteurRenvoie400() throws Exception {
        // titre vide
        mockMvc.perform(MockMvcRequestBuilders.post("/books").content("""
                { "title":"", "author":"Victor Hugo" }
                """).contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());

        // auteur absent
        mockMvc.perform(MockMvcRequestBuilders.post("/books").content("""
                { "title":"Les Contemplations" }
                """).contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    private Number creerLivre(String titre, String auteur) throws Exception {
        var result = mockMvc.perform(MockMvcRequestBuilders.post("/books")
                        .content("{\"title\":\"" + titre + "\",\"author\":\"" + auteur + "\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    }
}
