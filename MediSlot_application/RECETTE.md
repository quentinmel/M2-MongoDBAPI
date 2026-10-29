# Recette — parcours de réservation hors ligne

Validation réalisée sur la branche `tp-parcours`, avec l'application locale reconstruite et les tests Playwright.

| Temps | Description | OK / KO & observations |
|---|---|---|
| 1 | Choisir un créneau | **OK** — Le choix ouvre la confirmation. Après la temporisation de sauvegarde de 500 ms (constat du brouillon après 650 ms dans le test), IndexedDB contient `booking:<userId>` avec le médecin et le créneau sélectionnés. |
| 2 | Remplir le formulaire | **OK (parcours existant)** — Le parcours MediSlot ne présente pas de champ de texte de réservation : les données saisies sont le choix du médecin et du créneau. Le brouillon ne contient ni mot de passe ni jeton de session. |
| 3 | Le réseau tombe | **OK** — Avec le contexte navigateur hors ligne, l'interface reste utilisable et le bandeau `role="status"` affiche exactement « Hors ligne, vos réponses sont gardées ». |
| 4 | Bandeau et « Valider » | **OK** — La confirmation hors ligne n'émet aucun `POST /api/appointments`. Le texte affiché est « En attente d'envoi : le créneau n'est pas encore réservé. » Le brouillon passe à `pending` et `outbox` contient une clé UUID. |
| 5 | Le réseau revient | **OK** — Le retour en ligne déclenche automatiquement le traitement de la file. Après la réponse du serveur, l'interface affiche « Rendez-vous confirmé » et précise que le serveur a confirmé la réservation. |
| 6 | Envoi et clé unique | **OK** — Le `POST /api/appointments` reçoit `201 Created` avec l'en-tête `Idempotency-Key`, identique à la clé stockée dans `outbox`. Le rejeu du même créneau avec la même clé répond `201` avec le même identifiant de rendez-vous. Après succès, le brouillon et l'entrée de file sont supprimés. |
| 7 | Confirmé / Créneau pris | **OK** — Cas de conflit exécuté avec un second compte qui réserve réellement le créneau pendant que le premier est hors ligne : la reprise reçoit `409` et `code: "SLOT_TAKEN"`. Le brouillon passe à `rejected`, reste conservé et les autres créneaux libres sont proposés. |
| Bonus | Rechargement / Session | **À vérifier** — Le code restaure brouillon et file depuis IndexedDB au démarrage, mais aucun scénario de rechargement hors ligne n'a été exécuté dans cette recette. |

## Vérifications techniques

- Tests frontend : **8 réussis**.
- Build et vérification TypeScript frontend : **réussis**.
- Tests Playwright : **5 réussis**, incluant réservation/annulation, reprise hors ligne, rejeu idempotent et conflit réel.
- Tests unitaires backend `BookingServiceTest` et `AbServiceTest` : **réussis**.
- Image backend construite et migration Flyway `V3` vérifiée comme appliquée.
- La suite backend complète n'a pas pu démarrer son test Testcontainers : le conteneur Ryuk n'est pas joignable depuis l'environnement (`Could not connect to Ryuk`). Le flux HTTP et l'idempotence ont toutefois été vérifiés via les tests navigateur contre le backend local.
