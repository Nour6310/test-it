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

    @BeforeEach
    void setUp() {
        bookRepository = Mockito.mock(BookRepository.class);
        memberRepository = Mockito.mock(MemberRepository.class);
        mailService = Mockito.mock(MailService.class);
        bookService = new BookService(bookRepository, memberRepository, mailService);
    }

    @Test
    void unMembreEmprunteUnLivreDisponible() {
        var livre = new Book("Les Miserables", "Victor Hugo");
        livre.setId(5L);
        var nour = new Member("nour");
        nour.setId(2L);

        Mockito.when(bookRepository.findById(5L)).thenReturn(Optional.of(livre));
        Mockito.when(memberRepository.findById(2L)).thenReturn(Optional.of(nour));
        Mockito.when(bookRepository.findByMemberAndStatus(Mockito.any(), Mockito.any())).thenReturn(List.of());
        Mockito.when(bookRepository.save(Mockito.any())).thenAnswer(i -> i.getArgument(0));

        var result = bookService.borrowBook(5L, 2L);

        Assertions.assertThat(result.getStatus()).isEqualTo(Status.EMPRUNTE);
        Assertions.assertThat(result.getBorrower()).isEqualTo(nour);

        Mockito.verify(bookRepository).save(livre);
        Mockito.verify(mailService, Mockito.times(1))
                .sendMail(Mockito.eq(nour), Mockito.contains("emprunt"), Mockito.anyString());
    }
}