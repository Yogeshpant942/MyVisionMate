// controllers/guardianController.js

const Guardian = require('../Models/GuardianModel');

exports.registerGuardian = async (req, res) => {
  try {
    const { name, phone } = req.body;
    const userId = req.user._id;
    
    if (!name || !phone) {
      return res.status(400).json({  
        success: false,
        message: "All fields are required"
      });
    }
    
    const phoneRegex = /^[6-9]\d{9}$/;
    if (!phoneRegex.test(phone)) {
      return res.status(400).json({
        success: false,
        message: "Please enter a valid 10-digit phone number"
      });
    }
    
    const existingGuardian = await Guardian.findOne({ 
      userId,
      phone
    });
    
    if (existingGuardian) {  // ✅ Fixed condition
      return res.status(400).json({
        success: false,
        message: "Guardian with this phone number already exists"
      });
    }
    
    // Create guardian
    const guardian = await Guardian.create({
      userId,
      name,
      phone
    });
    
    res.status(201).json({
      success: true,
      message: "Guardian added successfully",
      data: { guardian }
    });
    
  } catch (error) {
    console.error("Add Guardian Error:", error);
    
    if (error.code === 11000) {
      return res.status(400).json({
        success: false,
        message: "Guardian with this phone number already exists"
      });
    }
    
    res.status(500).json({
      success: false,
      message: "Server error while adding guardian",
      error: error.message
    });
  }
};

// ========== GET GUARDIANS ==========
exports.getGuardian = async (req, res) => {
  try {
    const userId = req.user._id;
    
    const guardians = await Guardian.find({ userId }); 
    
    res.status(200).json({
      success: true,
      message: "Guardians retrieved successfully",
      data: { 
        guardians,  // ✅ Now matches variable name
        count: guardians.length  // ✅ Added count
      }
    });
    
  } catch (error) {
    console.error("Get Guardians Error:", error);
    res.status(500).json({
      success: false,
      message: "Server error while fetching guardians",
      error: error.message
    });
  }
};

// ========== UPDATE GUARDIAN ==========
exports.updateGuardian = async (req, res) => {
  try {
    const { id } = req.params;
    const { name, phone } = req.body;
    const userId = req.user._id;
    
    const guardian = await Guardian.findOne({  // ✅ Added await
      _id: id,  // ✅ Changed from "id" to "_id"
      userId    // ✅ Added userId for security
    });
    
    if (!guardian) {
      return res.status(404).json({  // ✅ Changed to 404
        success: false,
        message: "Guardian not found"
      });
    }
    
    // Update name if provided
    if (name) {
      guardian.name = name;
    }
    
    // Update phone if provided
    if (phone) {
      // Validate phone format
      const phoneRegex = /^[6-9]\d{9}$/;
      if (!phoneRegex.test(phone)) {
        return res.status(400).json({
          success: false,
          message: "Please enter a valid 10-digit phone number"
        });
      }
      
      // Check if phone already exists for another guardian
      const phoneExists = await Guardian.findOne({
        userId,
        phone,
        _id: { $ne: id }  // Exclude current guardian
      });
      
      if (phoneExists) {
        return res.status(400).json({
          success: false,
          message: "Another guardian with this phone number already exists"
        });
      }
      
      guardian.phone = phone;
    }
    
    await guardian.save();
    
    res.status(200).json({
      success: true,
      message: "Guardian updated successfully",
      data: { guardian }
    });
    
  } catch (error) {
    console.error("Update Guardian Error:", error);
    
    if (error.code === 11000) {
      return res.status(400).json({
        success: false,
        message: "Guardian with this phone number already exists"
      });
    }
    
    res.status(500).json({
      success: false,
      message: "Server error while updating guardian",
      error: error.message
    });
  }
};

// ========== DELETE GUARDIAN ==========
exports.deleteGuardian = async (req, res) => {
  try {
    const { id } = req.params;
    const userId = req.user._id;
    
    const guardian = await Guardian.findOneAndDelete({  
      _id: id,  
      userId
    });
    
    if (!guardian) {
      return res.status(404).json({  
        success: false,
        message: "Guardian not found"
      });
    }
    
    res.status(200).json({
      success: true,
      message: "Guardian deleted successfully"
    });
    
  } catch (error) {  // ✅ Added missing catch block
    console.error("Delete Guardian Error:", error);
    res.status(500).json({
      success: false,
      message: "Server error while deleting guardian",
      error: error.message
    });
  }
};