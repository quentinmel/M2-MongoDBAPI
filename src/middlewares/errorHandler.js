// eslint-disable-next-line no-unused-vars
module.exports = (err, req, res, next) => {
  if (err.name === 'ValidationError') {
    const erreurs = Object.values(err.errors).map((e) => e.message);
    return res.status(400).json({ message: 'Données invalides', erreurs });
  }
  console.error(err);
  res.status(500).json({ message: 'Erreur interne du serveur' });
};
