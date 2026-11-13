package com.example.testit;

import com.example.testit.model.Book;
import com.example.testit.model.Member;
import com.example.testit.model.Status;
import com.example.testit.repository.BookRepository;
import com.example.testit.repository.MemberRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BookRepositoryIT {

    @Autowired
    BookRepository bookRepository;

    @Autowired
    MemberRepository memberRepository;

    @Test
    void trouveSeulementLesLivresEmpruntesParLeMembre() {
        var nour = memberRepository.save(new Member("nour"));
        var malek = memberRepository.save(new Member("malek"));

        var livreNourEmprunte = creerLivre("Germinal", Status.EMPRUNTE, nour);
        creerLivre("Candide", Status.RENDU, nour);
        creerLivre("Le Horla", Status.EMPRUNTE, malek);
        creerLivre("Bel-Ami", Status.DISPONIBLE, null);

        // When
        var result = bookRepository.findByMemberAndStatus(nour.getId(), Status.EMPRUNTE);

        // Then : uniquement le livre emprunte par Nour
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(result.get(0).getId()).isEqualTo(livreNourEmprunte.getId());
        Assertions.assertThat(result.get(0).getTitle()).isEqualTo("Germinal");
    }

    private Book creerLivre(String titre, Status status, Member emprunteur) {
        var livre = new Book(titre, "Auteur");
        livre.setStatus(status);
        livre.setBorrower(emprunteur);
        return bookRepository.save(livre);
    }
}