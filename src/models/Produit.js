const mongoose = require('mongoose');

const produitSchema = new mongoose.Schema(
  {
    nom: { type: String, required: [true, 'Le nom est obligatoire'], trim: true },
    description: { type: String, trim: true, default: '' },
    prix: { type: Number, required: [true, 'Le prix est obligatoire'], min: 0 },
    stock: { type: Number, default: 0, min: 0 },
  },
  { timestamps: true } // createdAt / updatedAt automatiques
);

module.exports = mongoose.model('Produit', produitSchema);
