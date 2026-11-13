package com.example.testit;

import com.example.testit.adapter.mail.MailService;
import com.example.testit.model.Book;
import com.example.testit.model.Member;
import com.example.testit.model.Status;
import com.example.testit.repository.BookRepository;
import com.example.testit.repository.MemberRepository;
import com.example.testit.service.BookService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

class BookServiceTest {

    BookService bookService;

    BookRepository bookRepository;
    MemberRepository memberRepository;
    MailService mailService;

    Book livre;
    Member nour;

    @BeforeEach
    void setUp() {
        bookRepository = Mockito.mock(BookRepository.class);
        memberRepository = Mockito.mock(MemberRepository.class);
        mailService = Mockito.mock(MailService.class);
        bookService = new BookService(bookRepository, memberRepository, mailService);

        livre = new Book("Les Miserables", "Victor Hugo");
        livre.setId(5L);
        nour = new Member("nour");
        nour.setId(2L);

        Mockito.when(bookRepository.findById(5L)).thenReturn(Optional.of(livre));
        Mockito.when(memberRepository.findById(2L)).thenReturn(Optional.of(nour));
        Mockito.when(bookRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void unMembreEmprunteUnLivreDisponible() {
        // When
        var result = bookService.borrowBook(5L, 2L);

        // Then
        Assertions.assertThat(result.getStatus()).isEqualTo(Status.EMPRUNTE);
        Assertions.assertThat(result.getBorrower()).isEqualTo(nour);
        // Verify que le livre est sauvegarde
        Mockito.verify(bookRepository).save(livre);
        // Verify qu'un seul mail est envoye, au membre
        Mockito.verify(mailService, Mockito.times(1))
                .sendMail(Mockito.eq(nour), Mockito.contains("emprunt"), Mockito.anyString());
    }

    @Test
    void refuseLEmpruntSiLeMembreADejaUnLivreEnCours() {
        // Given : nour a deja un livre EMPRUNTE
        Mockito.when(bookRepository.findByMemberAndStatus(2L, Status.EMPRUNTE))
                .thenReturn(List.of(new Book("Germinal", "Zola")));

        // When / Then
        Assertions.assertThatThrownBy(() -> bookService.borrowBook(5L, 2L))
                .isInstanceOf(IllegalStateException.class);
        // Verify que rien n'est sauvegarde
        Mockito.verify(bookRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void refuseLeRetourParUnAutreMembre() {
        // Given : le livre est emprunte par nour
        livre.setStatus(Status.EMPRUNTE);
        livre.setBorrower(nour);

        // When / Then : le membre 3 essaie de le rendre
        Assertions.assertThatThrownBy(() -> bookService.returnBook(5L, 3L))
                .isInstanceOfAny(IllegalArgumentException.class, IllegalStateException.class);
        Assertions.assertThat(livre.getStatus()).isEqualTo(Status.EMPRUNTE);
    }

    @Test
    void previentLeReferentQuandLeMembreRendUnLivre() {
        // Given : nour a un bibliothecaire referent
        var biblio = new Member("biblio");
        biblio.setId(99L);
        nour.setReferent(biblio);
        livre.setStatus(Status.EMPRUNTE);
        livre.setBorrower(nour);

        // When
        bookService.returnBook(5L, 2L);

        // Then : un mail au membre ET un mail au referent
        Mockito.verify(mailService).sendMail(Mockito.eq(nour), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mailService).sendMail(Mockito.eq(biblio), Mockito.anyString(), Mockito.anyString());
    }
}
