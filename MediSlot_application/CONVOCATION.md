# TP — Génération de convocations PDF

## §1 — Contrat HTTP

La convocation est produite à la demande à partir du rendez-vous appartenant au patient connecté.

```http
GET /api/appointments/{id}/convocation.pdf
Cookie: JSESSIONID=<session>
Accept: application/pdf
```

Il s’agit d’un `GET` sans effet de bord : la session est nécessaire, mais aucun jeton CSRF n’est requis. Le serveur vérifie la propriété du rendez-vous avant de charger ses données. Le nom de fichier est `convocation-rdv-{id}.pdf` ; il ne contient aucune donnée personnelle.

Réponse `200` :

| En-tête | Valeur |
|---|---|
| `Content-Type` | `application/pdf` |
| `Content-Disposition` | `inline; filename="convocation-rdv-{id}.pdf"` |
| `Cache-Control` | `no-store` |
| `X-Content-Type-Options` | `nosniff` |
| `Content-Length` | Taille du PDF en octets |

| Code | Signification et message |
|---|---|
| `401` | Session absente : « Connexion requise ». |
| `404` | Rendez-vous inconnu ou appartenant à un autre patient : « Rendez-vous introuvable. » Le même statut évite de révéler l’existence d’un rendez-vous tiers. |
| `409` | Rendez-vous annulé : « Une convocation ne peut pas être générée pour un rendez-vous annulé. » |
| `500` | Échec de génération : message générique du service ; le front ne traite jamais le corps JSON comme un PDF. |

Les erreurs `401`, `404` et `409` sont renvoyées en `application/problem+json`; le rendu PDF signale ses erreurs attendues sous forme de `500` contrôlé. Le client transforme les statuts en messages lisibles et n’essaie jamais de traiter une réponse d’erreur comme un PDF. Le contrat OpenAPI est dans `docs/openapi.yaml`.

## §2 — Pourquoi générer le PDF côté serveur ?

Le backend est la source fiable des rendez-vous et peut vérifier l’identité du patient propriétaire avant d’inclure son nom, la date et l’adresse du professionnel dans le document. Il centralise le format, la locale et le fuseau horaire, et embarque la police nécessaire aux caractères accentués. Le navigateur ne reçoit qu’un PDF déjà composé, plutôt que des données destinées à être recopiées dans un modèle client ; la réponse `no-store` évite sa mise en cache HTTP.

Le PDF est créé à la demande avec Thymeleaf et OpenHTMLtoPDF. Le nom du fichier est dérivé exclusivement de l’identifiant numérique du rendez-vous.

## §3 — Fuseau horaire

Un `Instant` représente un instant UTC, pas l’heure locale du rendez-vous. Le formater avec le fuseau par défaut de la machine produit des résultats différents selon l’hôte et peut afficher l’heure UTC au patient. `ConvocationFormats` impose `Europe/Paris` et la locale française ; les règles de changement d’heure sont donc appliquées par la base de fuseaux IANA.

Exemple vérifié par le test automatisé : `2026-01-15T10:00:00Z` devient **jeudi 15 janvier 2026 à 11:00** à Paris en hiver.

## §4 — Parcours navigateur et preuves HTTP

Application : `http://localhost:8088`. Après connexion, ouvrir **Mes rendez-vous** ; sur un rendez-vous confirmé, **Afficher la convocation** ouvre une fenêtre avant la requête réseau et **Enregistrer le PDF** déclenche le téléchargement. Sur un rendez-vous annulé, les deux actions affichent le message de conflit `409`.

Les commandes suivantes montrent comment créer une session de démonstration. Le mot de passe est demandé sans être inscrit dans la commande :

```sh
BASE=http://localhost:8088
EMAIL=patient@medislot.test
read -rsp 'Mot de passe MediSlot : ' PASSWORD; echo
COOKIE=/tmp/medislot-patient.cookies
curl -fsS -c "$COOKIE" "$BASE/api/csrf" > /tmp/medislot-csrf.json
TOKEN=$(python3 -c 'import json; print(json.load(open("/tmp/medislot-csrf.json"))["token"])')
HEADER=$(python3 -c 'import json; print(json.load(open("/tmp/medislot-csrf.json"))["headerName"])')
curl -i -c "$COOKIE" -b "$COOKIE" -H "$HEADER: $TOKEN" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode "username=$EMAIL" --data-urlencode "password=$PASSWORD" \
  "$BASE/api/login"
unset PASSWORD TOKEN
```

Pour choisir un rendez-vous de test, lire `GET /api/appointments` avec le cookie et prendre l’identifiant d’un rendez-vous confirmé. Les identifiants `34` (confirmé) et `47` (annulé) ci-dessous sont ceux du volume de démonstration utilisé pour cette validation ; ils peuvent différer après réinitialisation de la base.

```sh
curl -sS -D - -o /tmp/convocation.pdf -b "$COOKIE" \
  "$BASE/api/appointments/34/convocation.pdf"
head -c 5 /tmp/convocation.pdf
```

Résultat constaté :

```text
HTTP/1.1 200
Content-Type: application/pdf
Content-Length: 19400
Content-Disposition: inline; filename="convocation-rdv-34.pdf"
Cache-Control: no-store
X-Content-Type-Options: nosniff
%PDF-
```

Créer un deuxième cookie jar avec le compte `autre@medislot.test` (même mot de passe de démonstration), en reprenant les commandes CSRF et login ci-dessus, en remplaçant `EMAIL` par `autre@medislot.test` et le fichier par `/tmp/medislot-autre.cookies`. Puis tester :

```sh
curl -sS -o /dev/null -w '%{http_code}\n' -b /tmp/medislot-autre.cookies \
  "$BASE/api/appointments/34/convocation.pdf"
curl -sS -o /dev/null -w '%{http_code}\n' -b "$COOKIE" \
  "$BASE/api/appointments/47/convocation.pdf"
curl -sS -o /dev/null -w '%{http_code}\n' \
  "$BASE/api/appointments/34/convocation.pdf"
```

Codes reçus, dans le même ordre : **404** (autre patient), **409** (annulé), **401** (sans session). Le test Playwright `frontend/e2e/convocation.spec.ts` vérifie l’ouverture de la fenêtre, la réponse PDF et le conflit sur rendez-vous annulé. Le test unitaire du front vérifie l’assignation et la révocation de l’URL Blob utilisée pour le téléchargement.

## §5 — Défi : récapitulatif annuel de 100 rendez-vous

Ne pas générer le document long dans la requête HTTP du navigateur. Le contrat asynchrone peut utiliser trois appels :

1. **Commander** : `POST /api/annual-summaries` avec la période demandée et une clé `Idempotency-Key`. La mutation exige la session et le jeton CSRF récupéré via `GET /api/csrf`. Réponse `202 Accepted` avec `Location: /api/document-jobs/{id}` et un état initial `queued`.
2. **Suivre** : `GET /api/document-jobs/{id}` renvoie `queued`, `running`, `succeeded` ou `failed`, avec éventuellement un pourcentage approximatif et `Retry-After`. Le job appartient au patient ; une session d’un autre patient reçoit `404`. Le front interroge avec un délai croissant et une limite, sans bloquer l’interface ni faire de boucle agressive.
3. **Télécharger** : lorsque l’état vaut `succeeded`, le front suit `downloadUrl` (ou appelle `GET /api/document-jobs/{id}/file`). Cette route vérifie encore le propriétaire et renvoie le PDF avec `Content-Type`, `Content-Disposition`, `Cache-Control: no-store` et `X-Content-Type-Options: nosniff`. Le fichier temporaire privé est supprimé après une durée de rétention définie.

Le front conserve l’identifiant de job pendant la navigation, affiche la progression et les erreurs, permet de reprendre le suivi après rechargement, puis lance le téléchargement seulement lorsque le serveur confirme que le document est prêt. L’idempotence empêche un double clic de lancer plusieurs générations coûteuses ; le contrôle CSRF protège la commande, tandis que les lectures restent des `GET` sans mutation.

## §6 — Vérifications finales

Commandes exécutées :

```text
backend : mvn -B -Dtest=ConvocationControllerTest,ConvocationFormatsTest,ConvocationServiceTest,PdfRendererTest test
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

frontend : npm test
Test Files  3 passed (3)
Tests       18 passed (18)

frontend : npm run build
vue-tsc et Vite : réussis

frontend : npx playwright test e2e/convocation.spec.ts
1 passed
```

Le test PDF ouvre le document généré, en extrait le texte et vérifie les accents français ; les tests de service couvrent la propriété, le `404`, le `409`, le nom de fichier et l’heure de Paris. Les réponses HTTP du §4 ont été vérifiées contre les services Compose.

La suite backend complète a été tentée : **8 tests passent**, mais `BookingIntegrationTest` échoue avant son exécution parce que Testcontainers ne peut pas joindre Ryuk (`Could not connect to Ryuk at 172.17.0.1`). Les six tests ciblés de convocation ci-dessus s’exécutent sans Testcontainers et passent. Le wrapper `backend/mvnw` est exécutable ; le JDK n’étant pas installé sur l’hôte, les tests ont été lancés avec Maven/Java 25 dans un conteneur.
