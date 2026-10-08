const mongoose = require('mongoose');

async function connectDB() {
  const uri = process.env.MONGODB_URI;
  if (!uri) throw new Error('MONGODB_URI manquant dans le fichier .env');

  await mongoose.connect(uri);
  console.log(`✅ MongoDB connecté (base : ${mongoose.connection.name})`);
}

module.exports = connectDB;
