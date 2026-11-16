# Traces MediSlot — constat et enquête

## Constat initial

Avant l’instrumentation, `docker compose up -d --build` démarrait `db`, `backend` et `frontend` seulement. L’application répondait en HTTP 200, mais Jaeger n’était pas disponible sur `localhost:16686`. Le script `tests/verifier_traces.sh` et le scénario E2E de traces n’existaient pas. Contrairement au résultat attendu par le sujet, le `npm test` initial passait déjà : 9 tests réussis.

Le volume PostgreSQL conservé ne contenait plus de créneaux à venir. Pour le parcours de réservation E2E, six créneaux fictifs futurs ont été ajoutés (un par professionnel) ; aucune donnée existante ni aucun volume n’a été supprimé.

## Instrumentation et confidentialité

Compose démarre maintenant les six services (`db`, `backend`, `frontend`, `collector`, `jaeger`, `sms-passerelle`). L’agent Java 2.33.1 exporte `medislot-api` et ses spans JDBC ; le navigateur exporte `medislot-web` via Nginx et propage le contexte W3C aux appels suivis. Les réponses API fournissent `X-Trace-Id`, affiché comme référence lors d’une erreur.

Le span métier `medislot.reservation` ne porte que `medislot.slot.id` et `medislot.appointment.id`. Les appels navigateur à `/api/professionals` sont exclus des spans client, car leur URL contient des termes de recherche ; le script vérifie aussi l’absence de paramètres dans les URL des spans navigateur du parcours de réservation. Les identifiants ci-dessous sont des identifiants techniques, pas des données de santé.

## T1 — Lenteur à la confirmation / double clic

- **Recherche Jaeger :** service `medislot-api`, opération `POST /api/appointments`, ouverture du waterfall.
- **Trace :** `090982e6133d0154a96f19807402b76f`.
- **Preuve :** span HTTP backend **414,575 ms** ; span métier `medislot.reservation` **159,979 ms**. Parmi ses sous-spans, `AppointmentRepository.saveAndFlush` dure **74,392 ms**, `SlotRepository.lockById` **47,673 ms** et `Session.persist fr.medislot.domain.Appointment` **69,061 ms**.
- **Maillon à examiner :** écriture/flush et verrouillage SQL dans la transaction de réservation. Cette trace unique ne démontre ni une latence anormale faute de seuil de référence, ni un double clic.
- **Piste :** comparer plusieurs réservations lentes et normales, puis séparer attente de verrou et temps SQL ; conserver l’idempotence déjà en place et vérifier qu’un double clic n’émet pas deux réservations.

## T2 — Sur-sollicitation de la base sur « Mes rendez-vous »

- **Recherche Jaeger :** service `medislot-api`, opération `GET /api/appointments`, examen des descendants SQL.
- **Trace :** `b793e745eead26b76e0e7141d79574d9`.
- **Preuve :** span HTTP **960,459 ms** et **67 spans SQL descendants** ; **32** `SELECT medislot.slot` et **32** `SELECT medislot.professional` se répètent dans cette même requête.
- **Maillon coupable :** `AppointmentController.list()` mappe chaque rendez-vous avec `dto()`, qui relit individuellement le créneau puis le professionnel.
- **Piste :** remplacer ces lectures par une projection ou une requête avec jointures qui charge les rendez-vous, créneaux et professionnels en un nombre constant de requêtes.

## T3 — Erreur intermittente sur l’historique

- **Recherche Jaeger :** opération `GET /api/appointments/{id}/history`.
- **Trace de contrôle :** `e4dab67376ea9c8b1eec14d5d726540b` — réponse **200**, **29,863 ms**, aucun événement d’exception.
- **Conclusion :** l’erreur intermittente du ticket n’a pas été reproduite ; aucune trace en échec ni référence d’erreur du ticket n’était disponible. La trace de contrôle ne permet donc pas d’identifier une ligne fautive. Le test E2E simule une réponse 500 pour vérifier l’affichage du `X-Trace-Id`, mais ce scénario simulé n’est pas présenté comme une trace d’incident.
- **Suite nécessaire :** au prochain échec réel, relever la référence de 32 caractères affichée dans l’interface, la rechercher dans Jaeger et relever l’événement d’exception du span en erreur avant de conclure sur la cause.

## Vérifications finales

`npm test` :

```text
Test Files  2 passed (2)
     Tests  10 passed (10)
```

`npm run build` : build TypeScript et Vite réussi.

`npx playwright test e2e/trace.spec.ts` :

```text
1 passed
```

Le test vérifie la propagation de `traceparent`, l’export POST vers `/otlp/v1/traces`, la correspondance de l’identifiant retourné par le back et l’affichage de la référence sur une erreur simulée.

`bash tests/verifier_traces.sh` :

```text
OK - Jaeger connaît medislot-api et medislot-web
OK - une trace du back contient des spans de base de données
OK - une réservation est liée et ses spans n’exposent que des attributs techniques
3 OK, 0 ÉCHEC
```

Les six services Compose sont actifs. Le test ciblé `BookingServiceTest` a réussi dans l’image Maven Java 25 (**1 test, 0 échec**). La suite backend complète a été tentée de la même façon : les tests unitaires passent, mais `BookingIntegrationTest` ne démarre pas, Testcontainers ne pouvant pas joindre Ryuk (`Could not connect to Ryuk`). Java n’étant pas installé sur l’hôte, `./mvnw test` n’y est pas directement exécutable.
