const mongoose = require('mongoose')

const GuardianSchema = mongoose.Schema({
      userId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: [true, 'User ID is required'],
    index: true
  },
    name:{
        type:String,
        required:[true,'Name is required'],
         trim: true,
    minlength: [2, 'Name must be at least 2 characters'],
    maxlength: [50, 'Name cannot exceed 50 characters']
    },
    phone:{
    type: String,
    required: [true, 'Phone number is required'],
    unique: true,
    match: [/^[6-9]\d{9}$/, 'Please enter a valid 10-digit Indian phone number']
    },
},{timestamps:true})

module.exports = mongoose.model('Guardian',GuardianSchema);