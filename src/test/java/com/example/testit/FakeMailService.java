package com.example.testit;

import com.example.testit.adapter.mail.MailService;
import com.example.testit.model.Member;

import java.util.ArrayList;
import java.util.List;

/**
 * Fake de MailService pour les tests : au lieu d'envoyer les mails,
 * il les garde dans une liste qu'on peut verifier.
 */
public class FakeMailService implements MailService {

    public record Mail(Member destinataire, String sujet, String message) {}

    public final List<Mail> mails = new ArrayList<>();

    @Override
    public void sendMail(Member member, String subject, String message) {
        mails.add(new Mail(member, subject, message));
    }

    @Override
    public void sendMailCloture(Member member) {
        sendMail(member, "Livre rendu", "Votre livre a ete rendu.");
    }
}
