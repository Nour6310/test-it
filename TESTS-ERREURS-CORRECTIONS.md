# Tests, erreurs et corrections

**Étudiante** : BEN HASSINE Nour El Houda  
**TD** : Test It - Prêt de livres (Spring Boot 4, Java 25)  
**Date** : 13/11/2026

## Récapitulatif

| # | Bug | Trouvé par | Type de test |
|---|---|---|---|
| 1 | Pas de mail à l'emprunt | `BookServiceTest` | Unitaire |
| 2 | Clés étrangères Liquibase sans nom | `BookRepositoryIT` | Intégration |
| 3 | Schéma Liquibase incohérent avec les entités | `BookRepositoryIT` | Intégration |
| 4 | Requête `OR` au lieu de `AND` | `BookRepositoryIT` | Intégration |
| 5 | `@Transactional` manquant (LazyInitializationException) | `BookServiceIT` | Intégration |
| 6 | POST /books : titre utilisé comme auteur | `BookControllerIT` | Intégration |
| 7 | POST /borrow appelle `returnBook` | `BookControllerIT` | Intégration |
| 8 | Erreur 500 en JSON (`Member.books` LAZY) | `BookControllerIT` | Intégration |
| 9 | Un membre peut emprunter plusieurs livres | `BookServiceTest` (bonus) | Unitaire |
| 10 | N'importe qui peut rendre un livre | `BookServiceTest` (bonus) | Unitaire |
| 11 | Le référent n'est jamais prévenu | `BookServiceTest` (bonus) | Unitaire |
| 12 | PUT remet un livre emprunté à DISPONIBLE | `BookControllerIT` (bonus) | Intégration |
| 13 | Emprunt sans être connecté : erreur 500 | `BookControllerIT` (bonus) | Intégration |
| 14 | DELETE d'un livre absent répond 204 | `BookControllerIT` (bonus) | Intégration |

---

## Bug 1 - Pas de mail envoyé à l'emprunt
- **Type de test** : unitaire (Mockito, sans Spring ni base)
- **Test** : `BookServiceTest.unMembreEmprunteUnLivreDisponible`
- **Symptôme** : `WantedButNotInvoked` sur `mailService.sendMail(...)`
- **Qui a tort ?** Le code : le README dit qu'un emprunt envoie un mail au membre, mais `borrowBook()` n'appelait jamais `mailService`.
- **Correction** : ajout de `mailService.sendMail(member, "Livre emprunté", ...)` dans `BookService.borrowBook()`.

## Bug 2 - Changelog Liquibase invalide
- **Type de test** : intégration (vraie base H2 + Liquibase)
- **Test** : `BookRepositoryIT` (Spring ne démarre pas)
- **Symptôme** : `SAXParseException` : l'attribut 'constraintName' doit figurer dans 'addForeignKeyConstraint'
- **Qui a tort ?** Le code : les clés étrangères n'ont pas de nom, alors que Liquibase l'exige.
- **Correction** : ajout de `constraintName` dans les changelogs 001 et 002.

## Bug 3 - Schéma Liquibase incohérent avec les entités
- **Type de test** : intégration (vraie base H2 + Liquibase + Hibernate)
- **Test** : `BookRepositoryIT` (Spring ne démarre pas)
- **Symptôme** : `Values of types "UUID" and "BIGINT" are not comparable`
- **Qui a tort ?** Le code : dans 001, `referent_id` est en uuid alors que `id` est en bigint, la table s'appelle `members` (entité : `member`) et la colonne s'appelle `user` (mot réservé, entité : `username`). `Book` utilise SEQUENCE alors que la table est en auto-incrément.
- **Correction** : 001 réécrit (table `member`, colonne `username`, `referent_id` en bigint), 002 pointe vers `member`, `Book` passe en `GenerationType.IDENTITY`.

## Bug 4 - Requête findByMemberAndStatus fausse
- **Type de test** : intégration (vraie requête JPQL sur H2)
- **Test** : `BookRepositoryIT.trouveSeulementLesLivresEmpruntesParLeMembre`
- **Symptôme** : `Expected size: 1 but was: 3`
- **Qui a tort ?** Le code : avec `OR`, la requête renvoie les livres du membre OU tous les livres au statut demandé.
- **Correction** : `OR` remplacé par `AND` dans `BookRepository`.

## Bug 5 - LazyInitializationException au retour d'un livre
- **Type de test** : intégration (contexte Spring complet, vraies transactions)
- **Test** : `BookServiceIT.emprunterPuisRendreUnLivre`
- **Symptôme** : `LazyInitializationException: could not initialize proxy - no Session`
- **Qui a tort ?** Le code : `borrower` est en LAZY et `BookService` n'est pas transactionnel, donc le membre est lu après la fermeture de la session.
- **Correction** : ajout de `@Transactional` sur la classe `BookService`.
- **Remarque** : avec `@Transactional` sur le test, ce bug aurait été caché.

## Bug 6 - POST /books enregistre le titre comme auteur
- **Type de test** : intégration (MockMvc + vraie base)
- **Test** : `BookControllerIT.creerUnLivrePuisLEmprunter`
- **Symptôme** : `JSON path "$.author" expected:<Victor Hugo> but was:<Notre-Dame de Paris>`
- **Qui a tort ?** Le code : `createBook(request.title, request.title)` passe le titre deux fois.
- **Correction** : `createBook(request.title, request.author)`.

## Bug 7 - POST /books/{id}/borrow rend le livre au lieu de l'emprunter
- **Type de test** : intégration (MockMvc + vraie base)
- **Test** : `BookControllerIT.creerUnLivrePuisLEmprunter`
- **Symptôme** : `AssertionError` à la ligne 54 sur `status().isOk()` : l'emprunt est refusé (400, car le livre DISPONIBLE ne peut pas être « rendu »)
- **Qui a tort ?** Le code : l'endpoint `/borrow` appelle `bookService.returnBook(...)` (copier-coller).
- **Correction** : appel de `bookService.borrowBook(...)`.

## Bug 8 - Erreur 500 en renvoyant un livre emprunté en JSON
- **Type de test** : intégration (MockMvc + sérialisation JSON réelle)
- **Test** : `BookControllerIT.creerUnLivrePuisLEmprunter`
- **Symptôme** : `Status expected:<200> but was:<500>`, cause `Cannot lazily initialize collection of role 'Member.books'`
- **Qui a tort ?** Le code : le JSON du livre contient `borrower`, qui contient la liste `books` en LAZY, lue hors transaction (et la relation tourne en boucle).
- **Correction** : `@JsonIgnore` sur `Member.books`.

## Bug 9 - Un membre peut emprunter plusieurs livres
- **Type de test** : unitaire (Mockito)
- **Test** : `BookServiceTest.refuseLEmpruntSiLeMembreADejaUnLivreEnCours`
- **Symptôme** : aucune exception levée
- **Qui a tort ?** Le code : `findByMemberAndStatus(memberId, Status.RENDU)` cherche les livres rendus au lieu des livres en cours.
- **Correction** : `Status.EMPRUNTE`.

## Bug 10 - N'importe quel membre peut rendre un livre
- **Type de test** : unitaire (Mockito)
- **Test** : `BookServiceTest.refuseLeRetourParUnAutreMembre`
- **Symptôme** : aucune exception levée
- **Qui a tort ?** Le code : `returnBook` ne vérifie pas que `memberId` est l'emprunteur.
- **Correction** : `IllegalStateException` si le membre n'est pas l'emprunteur.

## Bug 11 - Le référent n'est jamais prévenu
- **Type de test** : unitaire (Mockito)
- **Test** : `BookServiceTest.previentLeReferentQuandLeMembreRendUnLivre`
- **Symptôme** : `TooManyActualInvocations` (2 mails au membre, 0 au référent)
- **Qui a tort ?** Le code : le 2e mail est envoyé à `member` au lieu de `member.getReferent()`.
- **Correction** : destinataire `member.getReferent()`.

## Bug 12 - PUT sur un livre emprunté le remet à DISPONIBLE
- **Type de test** : intégration (MockMvc + vraie base)
- **Test** : `BookControllerIT.modifierUnLivreEmprunteNeChangePasSonStatut`
- **Symptôme** : le titre est bien modifié, mais le statut repasse à `DISPONIBLE` et l'emprunteur est perdu.
- **Qui a tort ?** Le code : `updateBook` enregistre tel quel le livre reçu en JSON, qui n'a ni statut ni emprunteur, donc il écrase tout.
- **Correction** : `updateBook` relit le livre en base et ne modifie que `title` et `author`. L'emprunteur passe en `FetchType.EAGER`, sinon la réponse JSON du PUT plante comme au bug 8.

## Bug 13 - Emprunter sans être connecté provoque une erreur 500
- **Type de test** : intégration (MockMvc)
- **Test** : `BookControllerIT.emprunterSansEtreConnecteRenvoie401`
- **Symptôme** : `ServletException` causée par `IllegalStateException: Member not authenticated`
- **Qui a tort ?** Le code : le `orElseThrow(...)` est placé avant le `try`, donc l'exception n'est jamais attrapée.
- **Correction** : `/borrow` et `/return` répondent `401 Unauthorized` si personne n'est connecté.

## Bug 14 - Supprimer un livre inexistant répond 204
- **Type de test** : intégration (MockMvc)
- **Test** : `BookControllerIT.supprimerUnLivreQuiNExistePasRenvoie404`
- **Symptôme** : `AssertionError` à la ligne 95 sur `status().isNotFound()` : le controller répond « supprimé » (204) sans vérifier que le livre existe
- **Qui a tort ?** Choix de conception : le README ne précise rien, mais `GET` et `PUT` répondent 404 pour un livre absent. Par cohérence, `DELETE` doit faire pareil.
- **Correction** : `DELETE` vérifie que le livre existe, sinon il répond `404 Not Found`.
---

## Bilan

- **16 tests** écrits (4 exercices + 12 bonus), tous verts.
- **14 bugs** trouvés et corrigés : 13 erreurs du code et 1 choix de conception (bug 14). Aucun test n'était faux.
- **Tests unitaires** (4 bugs : 1, 9, 10, 11) : ils trouvent les erreurs de logique métier. Mais ils ne voient pas la base : le bug `OR`/`AND` leur échappe, car le repository est mocké.
- **Tests d'intégration** (10 bugs : 2 à 8 et 12 à 14) : ils trouvent les erreurs d'assemblage (schéma Liquibase, requête SQL, transaction, controller, JSON).
- Bonus **fake plutôt que mock** : `FakeMailService` garde les mails dans une liste. On vérifie le résultat (les mails envoyés) au lieu de vérifier des appels de méthode.
- Chaque test a été vu **rouge avant d'être vert**, ce qui prouve qu'il teste vraiment quelque chose.
- Remarque : la console H2 annoncée dans le README ne fonctionne pas en Spring Boot 4 sans la dépendance `spring-boot-h2console`.