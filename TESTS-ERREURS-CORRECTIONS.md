# Tests, erreurs et corrections

**Étudiante** : BEN HASSINE Nour El Houda  
**TD** : Test It - Prêt de livres (Spring Boot 4, Java 25)  
**Date** : 13/11/2026

## Récapitulatif

| Partie du TD | Fichier de test | Type | Erreur trouvée ? |
|---|---|---|---|
| Exercice 1 | `BookServiceTest` | Unitaire | Oui (1 bug) |
| Exercice 2 | `BookRepositoryIT` | Intégration | Oui (3 bugs) |
| Exercice 3 | `BookServiceIT` | Intégration | Oui (1 bug) |
| Exercice 4 | `BookControllerIT` | Intégration | Oui (3 bugs) |
| Bonus - Cas d'erreur du service | `BookServiceTest` | Unitaire | Oui (1 bug) |
| Bonus - Retour d'un livre | `BookServiceTest` | Unitaire | Oui (2 bugs) |
| Bonus - Modifier un livre (PUT) | `BookControllerIT` | Intégration | Oui (1 bug) |
| Bonus - Supprimer un livre inexistant | `BookControllerIT` | Intégration | Oui (1 choix corrigé) |
| Bonus - Personne n'est connecté | `BookControllerIT` | Intégration | Oui (1 bug) |
| Bonus - Validation | `BookControllerIT` | Intégration | Non |
| Bonus - TDD | tous | - | - |
| Bonus - Fake plutôt que mock | `BookServiceTest` + `FakeMailService` | Unitaire | Non |

**Total : 16 tests, 14 erreurs trouvées et corrigées. Tous les tests sont verts.**

---

## Exercice 1 - BookServiceTest (unitaire, Mockito)

**Test** : `unMembreEmprunteUnLivreDisponible`. On emprunte un livre disponible, puis on vérifie le statut `EMPRUNTE`, l'emprunteur et le mail envoyé au membre.

**Erreur 1 - Pas de mail à l'emprunt**
- Résultat du test : `WantedButNotInvoked` sur `mailService.sendMail(...)`
- Qui a tort : le code. Le README dit qu'un emprunt envoie un mail au membre, mais `borrowBook()` n'appelait jamais `mailService`.
- Correction : ajout de `mailService.sendMail(member, "Livre emprunté", ...)` dans `BookService.borrowBook()`.

---

## Exercice 2 - BookRepositoryIT (intégration, vraie base H2)

**Test** : `trouveSeulementLesLivresEmpruntesParLeMembre`. Deux membres (nour et sami) et quatre livres (emprunté, rendu, disponible). On appelle `findByMemberAndStatus(nour, EMPRUNTE)` et on attend un seul livre.

**Erreur 2 - Changelog Liquibase invalide**
- Résultat du test : Spring ne démarre pas, `SAXParseException` : l'attribut 'constraintName' doit figurer dans 'addForeignKeyConstraint'
- Qui a tort : le code. Les clés étrangères n'ont pas de nom, alors que Liquibase l'exige.
- Correction : ajout de `constraintName` dans les changelogs 001 et 002.

**Erreur 3 - Schéma Liquibase incohérent avec les entités**
- Résultat du test : `Values of types "UUID" and "BIGINT" are not comparable`
- Qui a tort : le code. `referent_id` est en uuid alors que `id` est en bigint, la table s'appelle `members` (entité : `member`), la colonne s'appelle `user` (mot réservé, entité : `username`), et `Book` utilise SEQUENCE alors que la table est en auto-incrément.
- Correction : changelog 001 réécrit (table `member`, colonne `username`, `referent_id` en bigint), 002 pointe vers `member`, `Book` passe en `GenerationType.IDENTITY`.

**Erreur 4 - Requête OR au lieu de AND**
- Résultat du test : `Expected size: 1 but was: 3`
- Qui a tort : le code. Avec `OR`, la requête renvoie les livres du membre OU tous les livres empruntés.
- Correction : `OR` remplacé par `AND` dans `BookRepository`.

---

## Exercice 3 - BookServiceIT (intégration, contexte Spring complet)

**Test** : `emprunterPuisRendreUnLivre`. Lina emprunte puis rend un livre, et on relit en base : statut `RENDU`, livre toujours rattaché à Lina.

**Erreur 5 - Transaction manquante**
- Résultat du test : `LazyInitializationException: could not initialize proxy - no Session` au moment du retour
- Qui a tort : le code. L'emprunteur est chargé en LAZY et `BookService` n'est pas transactionnel : le membre est lu après la fermeture de la session, lors de l'envoi du mail.
- Correction : ajout de `@Transactional` sur la classe `BookService`.
- Remarque : si le test avait été annoté `@Transactional`, cette erreur aurait été cachée.

---

## Exercice 4 - BookControllerIT (intégration, MockMvc)

**Test** : `creerUnLivrePuisLEmprunter`. On fait `POST /books`, on vérifie le titre, l'auteur et le statut, puis `POST /books/{id}/borrow` doit répondre 200 avec le statut `EMPRUNTE`.

**Erreur 6 - Le titre est enregistré comme auteur**
- Résultat du test : `JSON path "$.author" expected:<Victor Hugo> but was:<Notre-Dame de Paris>`
- Qui a tort : le code. `createBook(request.title, request.title)` passe le titre deux fois.
- Correction : `createBook(request.title, request.author)`.

**Erreur 7 - /borrow rend le livre au lieu de l'emprunter**
- Résultat du test : `AssertionError` sur `status().isOk()` de l'emprunt (requête refusée)
- Qui a tort : le code. L'endpoint `/borrow` appelle `bookService.returnBook(...)` (copier-coller).
- Correction : appel de `bookService.borrowBook(...)`.

**Erreur 8 - Erreur 500 en renvoyant le livre en JSON**
- Résultat du test : `Status expected:<200> but was:<500>`, cause `Cannot lazily initialize collection of role 'Member.books'`
- Qui a tort : le code. Le JSON du livre contient l'emprunteur, qui contient la liste `books` en LAZY, lue hors transaction (et la relation tourne en boucle).
- Correction : `@JsonIgnore` sur `Member.books`.

---

## Bonus - Cas d'erreur du service (BookServiceTest)

| Test | Résultat |
|---|---|
| `refuseLEmpruntDUnLivreInconnu` | Vert : le code lève bien `IllegalArgumentException`. Aucune correction. |
| `refuseLEmpruntParUnMembreInconnu` | Vert : le code lève bien `IllegalArgumentException`. Aucune correction. |
| `refuseLEmpruntSiLeLivreEstDejaEmprunte` | Vert : le code lève bien `IllegalStateException`. Aucune correction. |
| `refuseLEmpruntSiLeMembreADejaUnLivreEnCours` | Rouge : erreur 9 |

**Erreur 9 - Un membre peut emprunter plusieurs livres**
- Résultat du test : aucune exception levée
- Qui a tort : le code. `findByMemberAndStatus(memberId, Status.RENDU)` cherche les livres rendus au lieu des livres en cours.
- Correction : `Status.EMPRUNTE`.

---

## Bonus - Retour d'un livre (BookServiceTest)

| Test | Résultat |
|---|---|
| `refuseLeRetourParUnAutreMembre` | Rouge : erreur 10 |
| `previentLeReferentQuandLeMembreRendUnLivre` | Rouge : erreur 11 |
| `sansReferentSeulLeMembreRecoitUnMail` | Vert : un seul mail, au membre. Aucune correction. |

**Erreur 10 - N'importe quel membre peut rendre un livre**
- Résultat du test : aucune exception levée
- Qui a tort : le code. `returnBook` ne vérifie jamais que `memberId` est l'emprunteur.
- Correction : `IllegalStateException` si le membre n'est pas l'emprunteur.

**Erreur 11 - Le référent n'est jamais prévenu**
- Résultat du test : `TooManyActualInvocations` (2 mails au membre, 0 au référent)
- Qui a tort : le code. Le 2e mail est envoyé à `member` au lieu de `member.getReferent()`.
- Correction : destinataire `member.getReferent()`.

---

## Bonus - Modifier un livre déjà emprunté (BookControllerIT)

**Test** : `modifierUnLivreEmprunteNeChangePasSonStatut`

**Erreur 12 - PUT remet le livre à DISPONIBLE**
- Résultat du test : le titre est modifié, mais le statut n'est plus `EMPRUNTE` (`AssertionFailedError`)
- Qui a tort : le code. `updateBook` enregistre tel quel le livre reçu en JSON, qui n'a ni statut ni emprunteur.
- Correction : `updateBook` relit le livre en base et ne modifie que `title` et `author`. L'emprunteur passe en `FetchType.EAGER` pour que la réponse JSON du PUT ne plante pas (même problème que l'erreur 8).

---

## Bonus - Supprimer un livre qui n'existe pas (BookControllerIT)

**Test** : `supprimerUnLivreQuiNExistePasRenvoie404`

**Erreur 13 - DELETE répond "supprimé" pour un livre absent**
- Résultat du test : `AssertionError` sur `status().isNotFound()` (le controller répond 204)
- Qui a tort : choix de conception. Le README ne précise rien, mais `GET` et `PUT` répondent 404 pour un livre absent ; par cohérence, `DELETE` doit faire pareil.
- Correction : `DELETE` vérifie que le livre existe, sinon il répond `404 Not Found`.

---

## Bonus - Personne n'est connecté (BookControllerIT)

**Test** : `emprunterSansEtreConnecteRenvoie401`

**Erreur 14 - Erreur serveur au lieu d'un refus**
- Résultat du test : `ServletException` causée par `IllegalStateException: Member not authenticated`
- Qui a tort : le code. Le `orElseThrow(...)` est placé avant le `try`, donc l'exception n'est jamais attrapée (erreur 500 dans l'application).
- Correction : `/borrow` et `/return` répondent `401 Unauthorized` si personne n'est connecté.

---

## Bonus - Validation (BookControllerIT)

**Test** : `creerUnLivreSansTitreOuSansAuteurRenvoie400`. `POST /books` avec un titre vide, puis sans auteur.
- Résultat : vert, les deux requêtes sont refusées en 400 grâce à `@Valid` et `@NotBlank`.
- Correction : aucune.

---

## Bonus - TDD

Pour chaque erreur ci-dessus, le test a été écrit **avant** la correction. Il a été lancé et vu **rouge**, puis le code a été corrigé et le test relancé jusqu'au **vert**. Tous les tests ont ensuite été relancés pour vérifier que la correction ne cassait rien ailleurs.

---

## Bonus - Fake plutôt que mock

**Fichiers** : `FakeMailService` (dans les tests) et `lEmpruntEnvoieUnMailAuMembreAvecUnFake`
- Le fake implémente `MailService` et garde chaque mail (destinataire, sujet, message) dans une liste.
- Le test vérifie directement le contenu de la liste : 1 mail, envoyé à nour, sujet contenant « emprunt », message contenant le titre du livre.
- Résultat : vert. Correction : aucune.
- Intérêt : on vérifie le **résultat** (les mails envoyés) au lieu de vérifier des appels de méthode sur un mock.

---

## Bilan

- **16 tests**, tous verts : 4 pour les exercices et 12 pour les bonus.
- **14 erreurs** trouvées et corrigées : 13 erreurs du code et 1 choix de conception (erreur 13). Aucun test n'était faux.
- Les **tests unitaires** trouvent les erreurs de logique métier (erreurs 1, 9, 10, 11). Mais ils ne voient pas la base : l'erreur `OR`/`AND` leur échappe, car le repository est mocké.
- Les **tests d'intégration** trouvent les erreurs d'assemblage (erreurs 2 à 8 et 12 à 14) : schéma Liquibase, requête SQL, transaction, controller, conversion JSON.

## Remarques
- Le README n'autorise l'emprunt que si le livre est `DISPONIBLE`, mais le code refuse seulement le statut `EMPRUNTE` : un livre `RENDU` peut donc être réemprunté. Bug ou choix ? Le README ne le précise pas, ce comportement n'a pas été modifié.