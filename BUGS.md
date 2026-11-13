@'
# Bugs trouvés par les tests

## Bug 1 - Pas de mail envoyé à l'emprunt
- **Test** : `BookServiceTest.emprunteUnLivreDisponible`
- **Symptôme** : `WantedButNotInvoked` sur `mailService.sendMail(...)`
- **Qui a tort ?** Le code : le README dit qu'un emprunt envoie un mail au membre, mais `borrowBook()` n'appelait jamais `mailService`.
- **Correction** : ajout de `mailService.sendMail(member, "Livre emprunté", ...)` dans `BookService.borrowBook()` avant le `return`.
'@ | Set-Content -Encoding utf8 BUGS.md