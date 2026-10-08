# API CRUD — Node.js / Express / MongoDB Atlas

## 1. Préparer MongoDB Atlas
1. Créez un cluster gratuit (M0) sur https://cloud.mongodb.com
2. **Database Access** → ajoutez un utilisateur (login + mot de passe)
3. **Network Access** → ajoutez votre IP (ou `0.0.0.0/0` pour tester uniquement)
4. **Connect → Drivers** → copiez l'URI de connexion

> Si le mot de passe contient des caractères spéciaux (`@`, `:`, `/`…), encodez-les (ex. `@` → `%40`).

## 2. Installer et lancer
```bash
npm install
cp .env.example .env     # puis collez votre URI Atlas dans .env
npm run dev
```

## 3. Voir les données avec Compass
Ouvrez MongoDB Compass → **New connection** → collez la même URI → **Connect**.
Après un premier `POST`, la base (nom présent dans l'URI) et la collection `produits` apparaissent.

## 4. Endpoints
| Méthode | Route                | Action                      |
|---------|----------------------|-----------------------------|
| POST    | `/api/produits`      | Créer                       |
| GET     | `/api/produits`      | Lister (`?page=1&limit=10`) |
| GET     | `/api/produits/:id`  | Lire un produit             |
| PUT     | `/api/produits/:id`  | Modifier                    |
| DELETE  | `/api/produits/:id`  | Supprimer                   |

Exemples prêts à l'emploi dans `requests.http` (extension « REST Client » de VS Code).

## Structure
```
src/
├── config/db.js
├── models/Produit.js
├── routes/produits.js
├── middlewares/ (validateId, errorHandler)
└── server.js
```
