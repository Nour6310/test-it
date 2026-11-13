@'
# Bugs trouvés par les tests

## Bug 1 - Pas de mail envoyé à l'emprunt
- **Test** : `BookServiceTest.emprunteUnLivreDisponible`
- **Symptôme** : `WantedButNotInvoked` sur `mailService.sendMail(...)`
- **Qui a tort ?** Le code : le README dit qu'un emprunt envoie un mail au membre, mais `borrowBook()` n'appelait jamais `mailService`.
- **Correction** : ajout de `mailService.sendMail(member, "Livre emprunté", ...)` dans `BookService.borrowBook()` avant le `return`.
'@ | Set-Content -Encoding utf8 BUGS.md
## Bug 2 - Changelog Liquibase invalide
- **Test** : `BookRepositoryIT` (Spring ne demarre pas)
- **Symptome** : `SAXParseException` : l'attribut 'constraintName' doit figurer dans 'addForeignKeyConstraint'
- **Qui a tort ?** Le code : les cles etrangeres n'ont pas de nom, alors que Liquibase l'exige.
- **Correction** : ajout de `constraintName` dans 001 (fk_members_referent) et 002 (fk_book_borrower).

## Bug 4 - Requete findByMemberAndStatus fausse (OR au lieu de AND)
- **Test** : `BookRepositoryIT.trouveSeulementLesLivresEmpruntesParLeMembre`
- **Symptome** : `Expected size: 1 but was: 3`
- **Qui a tort ?** Le code : avec `OR`, la requete renvoie les livres du membre OU tous les livres au statut demande.
- **Correction** : `OR` remplace par `AND` dans `BookRepository`.

## Bug 5 - LazyInitializationException au retour d'un livre
- **Test** : `BookServiceIT.emprunterPuisRendreUnLivre`
- **Symptome** : `LazyInitializationException: could not initialize proxy - no Session`
- **Qui a tort ?** Le code : `borrower` est en LAZY et `BookService` n'est pas transactionnel. Le membre est lu apres la fermeture de la session (envoi du mail).
- **Correction** : ajout de `@Transactional` sur la classe `BookService`.
- **Remarque** : si le test avait ete annote `@Transactional`, le bug aurait ete cache.

## Bug 9 - Un membre peut emprunter plusieurs livres
- **Test** : `BookServiceTest.refuseLEmpruntSiLeMembreADejaUnLivreEnCours`
- **Qui a tort ?** Le code : `findByMemberAndStatus(memberId, Status.RENDU)` cherche les livres rendus au lieu des livres en cours.
- **Correction** : `Status.EMPRUNTE`.

## Bug 10 - N'importe quel membre peut rendre un livre
- **Test** : `BookServiceTest.refuseLeRetourParUnAutreMembre`
- **Qui a tort ?** Le code : `returnBook` ne verifie pas que `memberId` est l'emprunteur.
- **Correction** : `IllegalStateException` si le membre n'est pas l'emprunteur.

## Bug 11 - Le referent n'est jamais prevenu
- **Test** : `BookServiceTest.previentLeReferentQuandLeMembreRendUnLivre`
- **Symptome** : `TooManyActualInvocations` (2 mails au membre, 0 au referent)
- **Qui a tort ?** Le code : le 2e mail est envoye a `member` au lieu de `member.getReferent()`.
- **Correction** : destinataire `member.getReferent()`.

---

## Bilan

- **7 tests** ecrits (4 exercices + 3 bonus), tous verts.
- **11 bugs** trouves et corriges, tous dans le code (aucun test faux).
- **Test unitaire** (`BookServiceTest`, Mockito) : trouve les erreurs de logique metier (mail manquant, mauvais statut, controle de l'emprunteur, destinataire du mail). Mais il ne voit pas la base : le bug `OR`/`AND` lui echappe car le repository est mocke.
- **Tests d'integration** (`...IT`, vraie base H2 + Spring) : trouvent les erreurs d'assemblage (schema Liquibase, requete SQL, transaction manquante, controller, conversion JSON).
- Chaque test a ete vu **rouge avant d'etre vert**, ce qui prouve qu'il teste vraiment quelque chose.
- Remarque : la console H2 annoncee dans le README ne fonctionne pas en Spring Boot 4 sans la dependance `spring-boot-h2console`.
