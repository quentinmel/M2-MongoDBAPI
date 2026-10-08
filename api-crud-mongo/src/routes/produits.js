const express = require('express');
const Produit = require('../models/Produit');
const validateId = require('../middlewares/validateId');

const router = express.Router();

// CREATE — POST /api/produits
router.post('/', async (req, res, next) => {
  try {
    const { nom, description, prix, stock } = req.body;
    const produit = await Produit.create({ nom, description, prix, stock });
    res.status(201).json(produit);
  } catch (err) {
    next(err);
  }
});

// READ (liste, avec pagination) — GET /api/produits?page=1&limit=10
router.get('/', async (req, res, next) => {
  try {
    const page = Math.max(parseInt(req.query.page) || 1, 1);
    const limit = Math.min(parseInt(req.query.limit) || 10, 100);

    const [data, total] = await Promise.all([
      Produit.find().sort({ createdAt: -1 }).skip((page - 1) * limit).limit(limit),
      Produit.countDocuments(),
    ]);

    res.json({ page, limit, total, pages: Math.ceil(total / limit), data });
  } catch (err) {
    next(err);
  }
});

// READ (un seul) — GET /api/produits/:id
router.get('/:id', validateId, async (req, res, next) => {
  try {
    const produit = await Produit.findById(req.params.id);
    if (!produit) return res.status(404).json({ message: 'Produit introuvable' });
    res.json(produit);
  } catch (err) {
    next(err);
  }
});

// UPDATE — PUT /api/produits/:id
router.put('/:id', validateId, async (req, res, next) => {
  try {
    const { nom, description, prix, stock } = req.body;
    const produit = await Produit.findByIdAndUpdate(
      req.params.id,
      { nom, description, prix, stock },
      { new: true, runValidators: true, omitUndefined: true }
    );
    if (!produit) return res.status(404).json({ message: 'Produit introuvable' });
    res.json(produit);
  } catch (err) {
    next(err);
  }
});

// DELETE — DELETE /api/produits/:id
router.delete('/:id', validateId, async (req, res, next) => {
  try {
    const produit = await Produit.findByIdAndDelete(req.params.id);
    if (!produit) return res.status(404).json({ message: 'Produit introuvable' });
    res.json({ message: 'Produit supprimé', id: produit._id });
  } catch (err) {
    next(err);
  }
});

module.exports = router;
