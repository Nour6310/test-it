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

    // ---------- Exercice 1 ----------

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

    // ---------- Bonus : cas d'erreur de l'emprunt ----------

    @Test
    void refuseLEmpruntDUnLivreInconnu() {
        // When / Then : le livre 42 n'existe pas
        Assertions.assertThatThrownBy(() -> bookService.borrowBook(42L, 2L))
                .isInstanceOf(IllegalArgumentException.class);
        Mockito.verify(bookRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void refuseLEmpruntParUnMembreInconnu() {
        // When / Then : le membre 77 n'existe pas
        Assertions.assertThatThrownBy(() -> bookService.borrowBook(5L, 77L))
                .isInstanceOf(IllegalArgumentException.class);
        Mockito.verify(bookRepository, Mockito.never()).save(Mockito.any());
    }

    @Test
    void refuseLEmpruntSiLeLivreEstDejaEmprunte() {
        // Given : le livre est deja emprunte
        livre.setStatus(Status.EMPRUNTE);

        // When / Then
        Assertions.assertThatThrownBy(() -> bookService.borrowBook(5L, 2L))
                .isInstanceOf(IllegalStateException.class);
        Mockito.verify(bookRepository, Mockito.never()).save(Mockito.any());
        Mockito.verify(mailService, Mockito.never()).sendMail(Mockito.any(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    void refuseLEmpruntSiLeMembreADejaUnLivreEnCours() {
        // Given : nour a deja un livre EMPRUNTE
        Mockito.when(bookRepository.findByMemberAndStatus(2L, Status.EMPRUNTE))
                .thenReturn(List.of(new Book("Germinal", "Zola")));

        // When / Then
        Assertions.assertThatThrownBy(() -> bookService.borrowBook(5L, 2L))
                .isInstanceOf(IllegalStateException.class);
        Mockito.verify(bookRepository, Mockito.never()).save(Mockito.any());
    }

    // ---------- Bonus : retour d'un livre ----------

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

    @Test
    void sansReferentSeulLeMembreRecoitUnMail() {
        // Given : nour n'a pas de referent
        livre.setStatus(Status.EMPRUNTE);
        livre.setBorrower(nour);

        // When
        var result = bookService.returnBook(5L, 2L);

        // Then : un seul mail, envoye a nour
        Assertions.assertThat(result.getStatus()).isEqualTo(Status.RENDU);
        Mockito.verify(mailService, Mockito.times(1)).sendMail(Mockito.any(), Mockito.anyString(), Mockito.anyString());
        Mockito.verify(mailService).sendMail(Mockito.eq(nour), Mockito.anyString(), Mockito.anyString());
    }

    // ---------- Bonus : fake plutot que mock ----------

    @Test
    void lEmpruntEnvoieUnMailAuMembreAvecUnFake() {
        // Given : on remplace le mock du mail par un fake
        var fakeMail = new FakeMailService();
        var service = new BookService(bookRepository, memberRepository, fakeMail);

        // When
        service.borrowBook(5L, 2L);

        // Then : on lit directement les mails gardes par le fake
        Assertions.assertThat(fakeMail.mails).hasSize(1);
        Assertions.assertThat(fakeMail.mails.get(0).destinataire()).isEqualTo(nour);
        Assertions.assertThat(fakeMail.mails.get(0).sujet()).contains("emprunt");
        Assertions.assertThat(fakeMail.mails.get(0).message()).contains("Les Miserables");
    }
}
