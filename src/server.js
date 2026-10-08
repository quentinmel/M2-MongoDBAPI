require('dotenv').config();
const express = require('express');
const cors = require('cors');
const connectDB = require('./config/db');
const produitsRoutes = require('./routes/produits');
const errorHandler = require('./middlewares/errorHandler');

const app = express();
app.use(cors());
app.use(express.json());

app.get('/', (req, res) => res.json({ message: 'API CRUD MongoDB — OK' }));
app.use('/api/produits', produitsRoutes);

app.use((req, res) => res.status(404).json({ message: 'Route introuvable' }));
app.use(errorHandler);

const PORT = process.env.PORT || 3000;

connectDB()
  .then(() => app.listen(PORT, () => console.log(`🚀 API sur http://localhost:${PORT}`)))
  .catch((err) => {
    console.error('❌ Connexion MongoDB impossible :', err.message);
    process.exit(1);
  });
