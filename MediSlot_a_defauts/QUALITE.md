# Qualité logicielle MediSlot

## 1. Les mesures

Analyses SonarQube locales finales en version `1.2`. La couverture Sonar est calculée sur l'ensemble des sources analysées ; les pourcentages bruts des rapports de test sont précisés pour éviter de les confondre avec cet indicateur.

| Projet | Version | Lignes de code (ncloc) | Note / porte qualité | Couverture | Duplications |
|---|---:|---:|---|---:|---:|
| Backend (`medislot-backend`) | 1.2 | 996 | Porte personnalisée `MediSlot TP Quality Gate` : OK ; fiabilité E (5.0), sécurité E (5.0), maintenabilité A (1.0) | Sonar 24,1 % ; JaCoCo lignes 128/452 = 28,3 % | 9,3 % |
| Frontend (`medislot-frontend`) | 1.2 | 576 | Porte personnalisée `MediSlot TP Quality Gate` : OK ; fiabilité D (4.0), sécurité D (4.0), maintenabilité A (1.0) | Sonar 6,8 % ; LCOV lignes 9/10 = 90 % sur `api.ts` testé | 16,8 % |

Les rapports de tests ont été générés : backend, 3 tests exécutés, 0 échec et 1 test ignoré ; frontend, 2 tests réussis et 1 ignoré. La porte personnalisée a été créée comme copie de `Sonar way` puis associée aux deux projets. Son statut `OK` ne signifie pas que tous les problèmes sont corrigés : les évaluations portent principalement sur le nouveau code.

## 2. Les problèmes

| # | Règle | Fichier et ligne | Sévérité Sonar | Problème constaté | Décision argumentée |
|---:|---|---|---|---|---|
| 1 | `java:S6437` | `backend/src/main/java/fr/medislot/api/DebugController.java:113` | Blocker | Mot de passe de base de données écrit en dur dans le diagnostic JDBC. | Corrigé : le diagnostic utilise maintenant le `JdbcTemplate` configuré par l'application. Sonar l'a marqué `CLOSED` en version 1.1 ; commit `22adeb4`. |
| 2 | `java:S4502` | `backend/src/main/java/fr/medislot/security/SecurityConfig.java:23` | Critical | La protection CSRF est ignorée pour plusieurs routes ; les exceptions doivent être limitées et justifiées, notamment pour les routes de modification utilisant les cookies de session. | À corriger avant production : conserver la protection CSRF par défaut et vérifier chaque exception avec des tests. |
| 3 | `java:S2095` | `backend/src/main/java/fr/medislot/api/AppointmentExportController.java:50` | Blocker | Le `FileWriter` du CSV n'est pas fermé, ce qui peut laisser un descripteur ouvert et empêcher la finalisation fiable du fichier. | À corriger : écrire avec une ressource `try-with-resources`, puis tester l'export. |
| 4 | `typescript:S1523` | `frontend/src/components/AdminDebug.vue:27` | Critical | `eval` exécutait une expression fournie par l'utilisateur dans la calculatrice de debug. | Corrigé : la calculatrice et l'évaluation dynamique ont été retirées. Sonar l'a marquée `CLOSED` en version 1.1 ; commit `3fa786d`. |
| 5 | `typescript:S5852` | `frontend/src/components/ConnexionAvancee.vue:12` | Critical | L'expression régulière de validation d'e-mail comporte des quantificateurs imbriqués susceptibles d'entraîner un retour arrière coûteux sur une entrée défavorable. | À corriger : remplacer le motif par une validation bornée et ajouter un test sur une chaîne longue. |
| Non détecté lors de cette analyse | Revue manuelle | `frontend/src/components/ConnexionAvancee.vue:22` | — | Le mot de passe saisi est conservé dans `localStorage` avec l'adresse e-mail. Aucun problème correspondant n'a été remonté dans les résultats Sonar consultés. | À corriger : ne jamais persister le mot de passe côté navigateur ; supprimer cette écriture et toute donnée persistée existante. |

Les résultats de départ comportaient 38 problèmes backend et 19 frontend. Les problèmes ci-dessus sont des exemples vérifiés dans le code ; le tableau ne prétend pas lister l'intégralité des résultats.

## 3. La porte

| Version / rejeu | Défaut ou configuration | Condition et observation | Verdict |
|---|---|---|---|
| 1.0 | Analyse de référence sur `Sonar way` | Analyse initiale effectuée ; la porte renvoie `OK`. | OK |
| 1.1 | Les deux corrections ont été rescannées ; copie de `Sonar way` nommée `MediSlot TP Quality Gate`, associée aux deux projets. | `java:S6437` et `typescript:S1523` sont `CLOSED`. La porte renvoie `OK`. | Passed |
| 1.2 — défaut ajouté | Ajout temporaire de `QualityGateDemo.java` avec `left == right` (règle `java:S4973`, 1 nouveau problème). | La condition `new_violations > 0` échoue : valeur observée `1`, seuil `0`. Le scanner affiche `QUALITY GATE STATUS: FAILED`. | Failed |
| 1.2 — défaut retiré | Suppression du fichier temporaire, nettoyage/rejeu Maven et nouvelle analyse du backend ; analyse frontend également relancée. | Sonar ferme le problème temporaire (`CLOSED`), `new_violations = 0` ; les deux scanners affichent `QUALITY GATE STATUS: PASSED`. | Passed |
