const express = require('express');
const { registerController, loginController, UpdateUser,changePassword} = require('../Controllers/UserController');
const router = express.Router();
router.post('/register',registerController);
router.post('/login',loginController);
router.put('/updateUser',UpdateUser);
router.put('/updatePassword',changePassword);

module.exports = router;