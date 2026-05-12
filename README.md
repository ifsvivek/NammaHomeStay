# 🏡 Namma-HomeStay MVP

**Empowering Rural Hosts through Accessible Digital Inclusion**

Namma-HomeStay is an Android application designed with a single goal in mind: creating a "Simplified Host Portal" that makes digital marketing as easy as making a phone call. Built specifically for rural farmers and homemakers with low digital literacy, the app focuses on extreme simplicity, large touch targets, high contrast, and zero technical jargon.

## 🌟 Philosophy & Design Rules
- **"Less is More"**: Minimalist interfaces designed for accessibility.
- **Large Touch Targets**: Minimum 48dp for all interactive elements.
- **Visual Feedback**: Every action provides obvious feedback (Snackbars, Success Animations) so users know the "internet did its job."
- **Earth Tones**: A color palette utilizing greens, browns, and creams to reflect the Eco-Tourism nature of the homestays.

## ✨ Core Features

### 📱 Frictionless Onboarding
- **Password-less Login**: One-screen login using Phone Number + OTP.
- **Guided Setup**: Immediate "Setup your Home" progress bar upon initial login.

### 🏠 My Home Profile (The "Digital Shopfront")
- **Card-Based UI**: Simple, large `Card` components for profile management.
- **Verification Checklist**: A straightforward toggle-based checklist (Clean Bedding, Functional Washroom, Drinking Water). Profiles go live only when these are checked.
- **Bandwidth-Friendly Photo Uploads**: "Tap to Add Photo" functionality with automatic image compression to accommodate rural internet speeds.

### 🍲 The "60-Second Menu" (Critical MVP Feature)
- **Speed & Simplicity**: A dedicated "Today's Menu" FAB allows hosts to update their daily menu in under 60 seconds.
- **Minimal Input**: One image slot + dish name + price. Uses a single Firestore `set()` operation for speed.

### 📞 Inquiry Box & Direct Connect
- **Incoming Interests**: A clear list showing traveler names and timestamps.
- **Direct Connect**: A prominent, large **Green "Call Guest" Button** that immediately opens the phone dialer (`ACTION_DIAL`).

## 🛠️ Technical Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Modern, reactive UI)
- **Architecture**: MVVM (Model-View-ViewModel) with Clean Architecture principles
- **Backend & Backend-as-a-Service**:
  - **Firebase Phone Auth**: OTP-based authentication.
  - **Firebase Firestore**: Real-time NoSQL database.
  - **Firebase Storage**: For hosting images.
- **Image Handling**: Coil (loading) and Android `ActivityResultContracts.GetContent` for picking images.

## 🗄️ Firestore Data Schema

```javascript
// Collections & Documents

hosts {
  uid: String
  name: String
  phone: String
  verified_status: Boolean
}

homestays {
  host_id: String
  name: String
  location: String
  images: Array<String>
  checklist: Map<String, Boolean>
}

daily_menus {
  host_id: String
  dish_name: String
  price: Number
  image_url: String
  date_timestamp: Timestamp
}

inquiries {
  id: String
  host_id: String
  guest_name: String
  guest_phone: String
  status: "pending" | "resolved"
}
```

## 🚀 Getting Started

1. Clone the repository.
2. Open the project in Android Studio.
3. Connect the app to your Firebase project by adding the `google-services.json` file to the `app/` directory.
4. Enable **Phone Authentication**, **Firestore**, and **Storage** in your Firebase console.
5. Build and run the app on an emulator or physical device.
