# MongoDB Atlas Setup Guide for ChronoVault

## Part 2: Authentication System ✅ COMPLETED

### What's New in Part 2:

#### 1. **MongoDB Realm Authentication**
- ✅ AuthRepository with login/signup flows
- ✅ Secure token-based authentication
- ✅ Encrypted SharedPreferences for token storage
- ✅ Session management

#### 2. **Login Screen (LoginFragment)**
- ✅ Email/Password input with validation
- ✅ Real-time error messaging
- ✅ Loading state indicator
- ✅ Navigation to Signup
- ✅ Form validation (email format, password strength)

#### 3. **Signup Screen (SignupFragment)**
- ✅ Name, Email, Password, Confirm Password fields
- ✅ Client-side validation (all fields required)
- ✅ Password matching verification
- ✅ Loading state during signup
- ✅ Error handling with user feedback

#### 4. **ViewModels**
- ✅ LoginViewModel with login state management
- ✅ SignupViewModel with signup state management
- ✅ LiveData for reactive UI updates
- ✅ Error handling and validation

#### 5. **Authentication Navigation**
- ✅ AuthActivity as main entry point
- ✅ Separate auth_navigation graph
- ✅ Auto-redirect to app if already logged in
- ✅ Logout flow back to login

#### 6. **MongoDB Realm API Integration**
- ✅ MongoDBApi with auth endpoints
- ✅ RetrofitClient configuration
- ✅ Token-based API requests
- ✅ Error handling

---

## 🔧 MongoDB Atlas Setup Instructions

### Step 1: Create MongoDB Atlas Cluster

1. Go to [MongoDB Atlas](https://www.mongodb.com/cloud/atlas/register)
2. Sign up / Log in
3. Create a new project (e.g., "ChronoVault")
4. Create a new cluster:
   - Choose "Free" tier
   - Cloud Provider: AWS
   - Region: Your closest region
   - Cluster Name: `chronovault-db`

### Step 2: Create Database User

1. Go to **Database Access** → **Add New Database User**
   - Username: `chronovault_user`
   - Password: Generate a strong password
   - Database User Privileges: `Atlas admin`

2. Copy the connection string (you'll need it)

### Step 3: Set Network Access

1. Go to **Network Access** → **Add IP Address**
2. Click "Allow Access from Anywhere" (0.0.0.0/0)
   - ⚠️ For production, use specific IP ranges instead

### Step 4: Setup Realm App (For REST API)

1. Go to your cluster → **Realm**
2. Click **Create a New App**
3. App name: `chronovault-app`
4. Link to cluster: `chronovault-db`
5. Create App

### Step 5: Enable API Authentication

1. In Realm App → **App Settings**
2. Go to **Authentication**
3. Enable **API Key** authentication
4. Copy your API Key

### Step 6: Configure Your API

1. Update your Retrofit base URL in `RetrofitClient.kt`:

```kotlin
private const val MONGODB_API_BASE_URL = "https://YOUR_REALM_DOMAIN/api/v1/"
```

Replace `YOUR_REALM_DOMAIN` with your Realm app's domain from:
- Realm App → **Deployment** → Copy the URL

2. Example format:
```
https://data.mongodb-api.com/app/YOUR_APP_ID/endpoint
```

### Step 7: Create Backend Functions (Optional but Recommended)

You can create serverless functions in Realm for authentication:

1. **Realm App** → **Functions**
2. Create function: `registerUser`

```javascript
exports = async function registerUser(email, password, name) {
  const db = context.services.get("mongodb-atlas").db("chronovault");
  const users = db.collection("users");
  
  // Hash password (use bcrypt in production)
  const hashedPassword = password; // TODO: Use proper hashing
  
  const result = await users.insertOne({
    email,
    password: hashedPassword,
    name,
    createdAt: new Date(),
    updatedAt: new Date()
  });
  
  return {
    userId: result.insertedId,
    email,
    name,
    token: context.auth.generateJWT() // JWT token for session
  };
};
```

### Step 8: API Endpoints Setup

Update `MongoDBApi.kt` with your actual MongoDB Realm REST API endpoints:

```kotlin
// Authentication endpoints
@POST("auth/register")
suspend fun registerUser(@Body request: CreateUserRequest): AuthResponse

@POST("auth/login")
suspend fun loginUser(@Body request: LoginRequest): AuthResponse
```

---

## 📱 How Authentication Works:

### Login Flow:
```
User enters email/password
         ↓
LoginViewModel validates input
         ↓
Calls AuthRepository.loginUser()
         ↓
MongoDBApi sends POST /auth/login
         ↓
MongoDB Realm processes request
         ↓
Returns token + user data
         ↓
Save to encrypted SharedPreferences
         ↓
Navigate to MainActivity
```

### Signup Flow:
```
User enters name, email, password
         ↓
SignupViewModel validates all fields
         ↓
Calls AuthRepository.registerUser()
         ↓
MongoDBApi sends POST /auth/register
         ↓
MongoDB Realm creates user document
         ↓
Returns token + user data
         ↓
Save to encrypted SharedPreferences
         ↓
Navigate to MainActivity
```

### Session Management:
```
App starts
         ↓
MainActivity checks AuthRepository.isUserLoggedIn()
         ↓
If logged in → Show main app
If not logged in → Redirect to AuthActivity (Login/Signup)
         ↓
Token stored in encrypted SharedPreferences
         ↓
Token included in all API requests via Retrofit
         ↓
On logout → Clear SharedPreferences, redirect to AuthActivity
```

---

## 🔐 Security Features Implemented:

✅ **Encrypted SharedPreferences** - Sensitive data encrypted at rest  
✅ **Token-based Auth** - JWT tokens for stateless authentication  
✅ **Password Validation** - Minimum 6 characters  
✅ **Email Validation** - RFC-compliant email format checking  
✅ **HTTPS/TLS** - All API calls encrypted in transit  
✅ **Secure Password Toggle** - Material Design password visibility toggle  
✅ **Error Messages** - User-friendly, non-revealing error messages  

---

## 📋 API Request Examples:

### Register User:
```json
POST /auth/register
{
  "email": "user@example.com",
  "password": "password123",
  "name": "John Doe"
}

Response:
{
  "userId": "507f1f77bcf86cd799439011",
  "email": "user@example.com",
  "name": "John Doe",
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

### Login User:
```json
POST /auth/login
{
  "email": "user@example.com",
  "password": "password123"
}

Response:
{
  "userId": "507f1f77bcf86cd799439011",
  "email": "user@example.com",
  "name": "John Doe",
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

### Authenticated Request:
```json
GET /user/profile
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...

Response:
{
  "userId": "507f1f77bcf86cd799439011",
  "email": "user@example.com",
  "name": "John Doe"
}
```

---

## 🚀 Testing the Auth System:

1. **Run the app** - It should open AuthActivity
2. **Click "Sign up here"** - Navigate to signup
3. **Fill signup form** - Enter valid data
4. **Click "Create Account"** - Should call API
5. **Check for errors** - Will show network errors until API is set up
6. **Once API configured** - Should navigate to home screen
7. **Test login** - Use created credentials
8. **Test logout** - Profile screen logout button

---

## 📁 Part 2 File Structure:

```
ui/
├── auth/
│   ├── AuthActivity.kt ✅
│   ├── LoginFragment.kt ✅
│   ├── LoginViewModel.kt ✅
│   ├── SignupFragment.kt ✅
│   └── SignupViewModel.kt ✅
│
└── (other fragments)

data/
├── remote/
│   ├── api/MongoDBApi.kt ✅
│   └── RetrofitClient.kt ✅
├── repository/
│   └── AuthRepository.kt ✅
└── ServiceLocator.kt ✅

res/
├── layout/
│   ├── activity_auth.xml ✅
│   ├── fragment_login.xml ✅
│   └── fragment_signup.xml ✅
└── navigation/
    └── auth_navigation.xml ✅
```

---

## ⚠️ Important Notes:

1. **API Base URL**: Replace `YOUR_REALM_DOMAIN` with actual MongoDB Realm domain
2. **Password Hashing**: In production, hash passwords using bcrypt on backend
3. **Token Expiry**: Implement token refresh mechanism
4. **Rate Limiting**: Add rate limiting for auth endpoints
5. **HTTPS Only**: Ensure all connections use HTTPS
6. **Never log tokens**: Don't print tokens in logs

---

## ✅ Part 2 Complete!

**Status**: ✅ Authentication System Ready!

**Next**: Part 3 - Home Screen (Dashboard with stats and quotes)

---

## Quick Checklist:

- [ ] MongoDB Atlas account created
- [ ] Cluster setup (Free tier)
- [ ] Database user created
- [ ] Network access configured
- [ ] Realm App created
- [ ] API endpoints configured
- [ ] RetrofitClient base URL updated
- [ ] Test authentication flows
- [ ] Verify token storage
- [ ] Test logout flow

