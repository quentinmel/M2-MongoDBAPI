# Recherche réactive - départ élève

Le projet fonctionne en recherche manuelle. Les mécanismes réactifs sont
à compléter dans src/main/resources/static/app.js. Aucune correction front
n'est incluse. L'API Java, les données MongoDB et la présentation sont fournis.

## Démarrer
Docker Desktop doit être lancé en mode conteneurs Linux. Depuis ce dossier :

```powershell
docker compose up -d --build
docker compose ps
```

Ouvrir http://localhost:8090 et rechercher martin avec le bouton.
Après modification : `docker compose up -d --build api`, puis recharger
le navigateur une fois. JavaScript est embarqué dans l'image Docker.
Arrêter : `docker compose stop`. Ne pas supprimer le volume de données.

## API
GET /api/professionals?q=martin
200 : {"query":"martin","items":[...]}.
Recherche une sous-chaîne dans le nom OU la spécialité, sans tenir compte
de la casse et des accents décomposables. Ne recherche pas la ville.
q nettoyé doit contenir 2 à 60 caractères ; sinon 400. Maximum 20 éléments,
triés par nom normalisé puis identifiant. Ce plafond n'est pas une pagination.
Les caractères de regex sont traités littéralement.

## Scénarios du profil lab
- ma : réponse retardée de 1 200 ms.
- mar : réponse retardée de 100 ms.
- autres valeurs : 180 ms.
- erreur : réponse 503 volontaire.
Ces ralentissements servent uniquement à observer les courses de réponses.
Ils ne sont pas des optimisations ni un modèle de code de production.
Les 12 professionnels sont fictifs. Le profil lab crée les absents au
démarrage sans écraser les documents déjà présents.

## Versions et limites
Java 21, Spring Boot 3.5.6, MongoDB 8.0, Vue 3 fourni localement.
Versions de continuité du laboratoire, pas les dernières versions garanties.
Le premier build nécessite Internet ; pas de Maven ou Node local requis.
API sans authentification, port limité à la machine locale. Aucun service payant.
La recherche par sous-chaîne avec regex convient au petit jeu de données ;
ne pas supposer qu'un index classique la rend rapide à grande échelle.
