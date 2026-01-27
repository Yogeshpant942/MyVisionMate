const User = require('../Models/UserModel'); 

exports.registerController = async (req, res) => {
  try {
    const { name, email, password, phoneNo } = req.body;
    if (!name || !email || !password || !phoneNo) {
      return res.status(400).send({ 
        success: false,
        message: "All fields are required"
      });
    }
    
    const existingUser = await User.findOne({ 
      $or: [{ email }, { phoneNo }] 
    });
    
    if (existingUser) {
      return res.status(400).send({ 
        success: false,
        message: "User already exists with this email or phone. Please try login."
      });
    }
    
    const user = await User.create({ name, email, password, phoneNo });
    
    const token = user.generateToken(); 
    
    res.status(201).send({ 
      success: true,
      message: "User registered successfully",
      data: {
        user: {
          id: user._id,
          name: user.name,
          email: user.email,
          phoneNo: user.phoneNo,
          isFirstLogin: user.isFirstLogin
        },
        token 
      }
    });
    
  } catch (error) {
    console.error("Register Error:", error);
    return res.status(500).send({
      success: false,
      message: "Server error during registration",
      error: error.message
    });
  }
};

// ========== LOGIN ==========
exports.loginController = async (req, res) => {
  try {
    const { email, password } = req.body;
    
    if (!email || !password) {
      return res.status(400).send({ 
        success: false,
        message: "Email and password are required"
      });
    }
    
    const user = await User.findOne({ email }).select('+password'); 
    
    if (!user) { 
      return res.status(404).send({
        success: false,
        message: "No user found with this email. Please sign up."
      });
    }
    
  const isMatched = await user.comparePassword(password);
    
    if (!isMatched) {
      return res.status(401).send({ 
        success: false,
        message: "Invalid password"
      });
    }
    const token = user.generateToken(); 
    if (user.isFirstLogin) {
      user.isFirstLogin = false;
      await user.save(); 
    }
    
    res.status(200).send({
      success: true,
      message: "Login successful",
      data: {
        user: {
          id: user._id,
          name: user.name,
          email: user.email,
          phone: user.phone,
          isFirstLogin: user.isFirstLogin
        },
        token 
      }
    });
  } catch (error) {
    console.error("Login Error:", error);
    return res.status(500).send({
      success: false,
      message: "Server error during login",
      error: error.message
    });
  }
};
exports.UpdateUser = async (req, res) => {
  try {
    const { email, name, phoneNo} = req.body;

    if (!name || !email || !phoneNo ) {
      return res.status(400).json({
        success: false,
        message: "All fields are required"
      });
    }

    const user = await User.findOne({
      $or: [{ email }, { phone: phoneNo }]
    });

    if (!user) {
      return res.status(404).json({
        success: false,
        message: "User does not exist with this email or phone"
      });
    }

    

    user.name = name;
    user.email = email;
    user.phone = phoneNo;

    await user.save();

    return res.status(200).json({
      success: true,  
      message: "User information changed successfully",
      data: {
        user: {
          id: user._id,
          name: user.name,
          email: user.email,
          phone: user.phone
        }
      }
    });

  } catch (error) {
    console.error("Update Error:", error);
    return res.status(500).json({
      success: false,
      message: "Server error during update",
      error: error.message
    });
  }
};
exports.changePassword = async (req, res) => {
  try {
    const { email, newPassword, oldPassword } = req.body;

    if (!email || !oldPassword || !newPassword) {
      return res.status(400).send({
        success: false,
        message: "All fields are required"
      });
    }

    // 2️⃣ Find user
    const user = await User.findOne({ email });
    if (!user) {
      return res.status(404).send({
        success: false,
        message: "User does not exist with this email"
      });
    }

    // 3️⃣ Verify old password
    const isMatch = await user.comparePassword(oldPassword);
    if (!isMatch) {
      return res.status(401).send({
        success: false,
        message: "Old password is incorrect"
      });
    }

    // 4️⃣ Set new password (will be hashed by pre-save hook)
    user.password = newPassword;
    await user.save();

    // 5️⃣ Success response
    res.status(200).send({
      success: true,
      message: "Password changed successfully"
    });

  } catch (error) {
    console.error("Change password error:", error);
    return res.status(500).send({
      success: false,
      message: "Server error during password change"
    });
  }
};
