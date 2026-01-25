const express = require('express');
const router = express.Router();

const authMiddleware = require('../middlewares/authMiddlewares');
const {
  registerGuardian,
  getGuardian,
  deleteGuardian,
  updateGuardian
} = require('../Controllers/GuardianController');

router.post('/register', authMiddleware, registerGuardian);
router.get('/getGuardian', authMiddleware, getGuardian);
router.delete('/delete/:id', authMiddleware, deleteGuardian);
router.put('/guardian_update/:id', authMiddleware, updateGuardian);


module.exports = router;
