package com.example.testit;

import com.example.testit.model.Member;
import com.example.testit.model.Status;
import com.example.testit.repository.BookRepository;
import com.example.testit.repository.MemberRepository;
import com.example.testit.service.BookService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BookServiceIT {

    @Autowired
    BookService bookService;

    @Autowired
    BookRepository bookRepository;

    @Autowired
    MemberRepository memberRepository;

    @Test
    void emprunterPuisRendreUnLivre() {
        // given : un membre et un livre disponible en base
        var lina = memberRepository.save(new Member("lina"));
        var livre = bookService.createBook("Madame Bovary", "Flaubert");

        // when : lina emprunte puis rend le livre
        bookService.borrowBook(livre.getId(), lina.getId());
        bookService.returnBook(livre.getId(), lina.getId());

        // then : on relit le livre en base
        var livreEnBase = bookRepository.findById(livre.getId()).orElseThrow();
        Assertions.assertThat(livreEnBase.getStatus()).isEqualTo(Status.RENDU);

        // le livre est toujours rattache a lina (dernier emprunteur)
        var livresDeLina = bookRepository.findByBorrowerId(lina.getId());
        Assertions.assertThat(livresDeLina).hasSize(1);
        Assertions.assertThat(livresDeLina.get(0).getId()).isEqualTo(livre.getId());
    }
}
