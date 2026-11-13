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
