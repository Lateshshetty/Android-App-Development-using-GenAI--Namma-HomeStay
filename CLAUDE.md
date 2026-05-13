Build a complete Android application called "Namma-HomeStay" using Kotlin,
following MVVM architecture. This is a simplified host portal for rural
coastal home-stays in Karnataka/Kerala/Goa, designed to be used by
non-tech-savvy farmers and homemakers.

=== KEYS CONFIGURATION ===
Read all keys from local.properties file:
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your_supabase_anon_key
GEMINI_API_KEY=your_gemini_api_key

Read them in build.gradle.kts:
  val properties = Properties()
  properties.load(project.rootProject.file("local.properties").inputStream())
  val supabaseUrl = properties["SUPABASE_URL"] as String
  val supabaseAnonKey = properties["SUPABASE_ANON_KEY"] as String
  val geminiApiKey = properties["GEMINI_API_KEY"] as String
  buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
  buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
  buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

=== TECH STACK ===
- Language: Kotlin
- Architecture: MVVM (ViewModel + StateFlow + Repository pattern)
- UI: XML layouts with Material Design 3 components
- Database: Supabase (PostgreSQL) for ALL data including:
    homestay profiles, menus, inquiries, local spots,
    map coordinates, photo URLs, user profiles
- Auth: Firebase Auth (Phone OTP primary, Google Sign-in backup)
- Storage: Supabase Storage buckets for ALL photos:
    bucket "homestay-photos" for room/property photos
    bucket "menu-photos" for food photos
    bucket "spot-photos" for local guide photos
- Real-time: Supabase Realtime for live inquiry updates
- Image picking: Use ActivityResultContracts.GetContent()
    AND ActivityResultContracts.TakePicture() BOTH
    Always provide two options: Camera and Gallery
    Handle all Android permissions properly:
      READ_EXTERNAL_STORAGE, READ_MEDIA_IMAGES,
      CAMERA, WRITE_EXTERNAL_STORAGE
    Handle Android 13+ (API 33) permission changes
    Use FileProvider for camera photos
    Convert picked image to ByteArray before uploading
    Show selected image immediately as preview
    DO NOT use any third party image picker library
    Use only native Android APIs for image picking
- Maps: osmdroid (NO Google Maps, NO API key needed)
- Navigation: Jetpack Navigation Component

=== CRITICAL IMAGE HANDLING ===
This is very important - image picking must work correctly.

For Gallery picking:
  val galleryLauncher = registerForActivityResult(
    ActivityResultContracts.GetContent()
  ) { uri ->
    uri?.let {
      binding.imagePreview.setImageURI(it)
      selectedImageUri = it
    }
  }
  galleryLauncher.launch("image/*")

For Camera:
  Create temp file in cache directory
  Use FileProvider to get URI
  val cameraLauncher = registerForActivityResult(
    ActivityResultContracts.TakePicture()
  ) { success ->
    if (success) {
      binding.imagePreview.setImageURI(tempCameraUri)
      selectedImageUri = tempCameraUri
    }
  }

FileProvider setup in AndroidManifest.xml:
  <provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
      android:name="android.support.FILE_PROVIDER_PATHS"
      android:resource="@xml/file_paths"/>
  </provider>

Create res/xml/file_paths.xml:
  <?xml version="1.0" encoding="utf-8"?>
  <paths>
    <cache-path name="camera_photos" location="." />
    <external-cache-path name="external_cache" location="." />
  </paths>

Image upload to Supabase Storage:
  val inputStream = context.contentResolver.openInputStream(imageUri)
  val bytes = inputStream?.readBytes() ?: return
  val fileName = "photo_${System.currentTimeMillis()}.jpg"
  supabase.storage.from("homestay-photos")
    .upload(fileName, bytes, upsert = true)
  val publicUrl = supabase.storage
    .from("homestay-photos")
    .publicUrl(fileName)

Show image picker as bottom sheet dialog with two buttons:
  "📷 Take Photo" and "🖼️ Choose from Gallery"
  This bottom sheet must appear for EVERY image upload
  in the entire app (rooms, menu, spots)

=== SUPABASE STORAGE SETUP ===
Create these storage buckets (public):
  homestay-photos  (for room and property images)
  menu-photos      (for food dish images)
  spot-photos      (for local guide spot images)

All buckets must be PUBLIC so images display without auth.
Store returned public URLs in respective Supabase tables.

=== MAP IMPLEMENTATION (EASY & SIMPLE) ===
Use osmdroid for ALL map features.
Make map interaction as simple as possible.

For Local Spot location picking - use this simple approach:

  Simple Location Picker:
  - Show a full screen osmdroid map
  - Show a fixed crosshair/pin icon in CENTER of screen
    (do not move with map, stays fixed at center)
  - User just DRAGS the map to position the crosshair
    over their desired location
  - Show current coordinates below map updating live
  - Big "Confirm This Location" button at bottom
  - On confirm: save the center coordinates of map view
  - This is much easier than tap-to-pin

  Code for getting center coordinates:
    val center = mapView.mapCenter
    val latitude = center.latitude
    val longitude = center.longitude

  Show crosshair in layout:
    <ImageView
      android:layout_gravity="center"
      android:src="@drawable/ic_crosshair"
      android:layout_width="48dp"
      android:layout_height="48dp"/>
  Create ic_crosshair drawable as a simple + symbol
  in terracotta color.

  Store ALL location data in Supabase:
    homestays table: latitude, longitude columns
    local_spots table: latitude, longitude columns

  For displaying spots on map:
    Fetch all spots from Supabase local_spots table
    Add osmdroid Marker for each spot
    Marker tap shows spot name and description
    Store and retrieve coordinates from Supabase

  Default map center for Karnataka coast:
    GeoPoint(14.8, 74.1), zoom level 10

=== FIREBASE AUTH SETUP ===
Use Firebase Auth for authentication.
Add google-services.json to /app folder.

Dependencies:
  implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
  implementation("com.google.firebase:firebase-auth-ktx")

Phone OTP Flow (Primary):
  LoginFragment:
  - Indian phone number input (+91 format)
  - Auto-adds +91 prefix
  - "Send OTP" button
  - Uses PhoneAuthProvider.verifyPhoneNumber()
  - Shows loading state while sending

  OtpFragment:
  - Title: "Enter the OTP sent to {phone}"
  - 6 separate single-digit EditText fields
  - Auto-advance to next field on digit entry
  - Auto-verify when all 6 digits filled
  - "Verify" button as fallback
  - Resend OTP option after 60 seconds countdown
  - Uses PhoneAuthProvider.getCredential()
  - Then firebaseAuth.signInWithCredential()

Google Sign-in Flow (Backup):
  - "Continue with Google" button on LoginFragment
  - Uses Google Sign-In SDK
  - On success: get Firebase user

After Firebase Auth success:
  - Get Firebase UID: firebaseAuth.currentUser?.uid
  - Check Supabase users table for this Firebase UID
  - If not exists: create new user record in Supabase
  - If exists: fetch user profile
  - Check role in Supabase users table
    No role → RoleSelectionFragment
    Has role → MainActivity

AuthRepository.kt:
  private val firebaseAuth = FirebaseAuth.getInstance()
  fun sendOtp(phone: String, activity: Activity,
    callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks)
  fun verifyOtp(verificationId: String, code: String)
  fun signInWithGoogle(idToken: String)
  fun getCurrentFirebaseUser(): FirebaseUser?
  fun signOut()
  suspend fun syncUserWithSupabase(firebaseUser: FirebaseUser)
  suspend fun getUserRole(firebaseUid: String): String?

=== SUPABASE SCHEMA (already created) ===
Tables already exist:
  users (id uuid, email text, name text, phone text,
         village text, role text, firebase_uid text,
         created_at timestamptz)

  homestays (id uuid, host_id uuid, name text,
             description text, village text, district text,
             state text, per_night_rate int,
             available_rooms int, is_verified bool,
             verify_cleanliness bool, verify_hygiene bool,
             verify_safety bool, verify_water bool,
             photos text[], facilities text[],
             latitude double precision,
             longitude double precision,
             view_count int, created_at timestamptz,
             updated_at timestamptz)

  menus (id uuid, homestay_id uuid, item_name text,
         description text, price int, photo_url text,
         is_available bool, date date, created_at timestamptz)

  inquiries (id uuid, homestay_id uuid, traveller_id uuid,
             traveller_name text, traveller_phone text,
             message text, is_read bool, created_at timestamptz)

  local_spots (id uuid, homestay_id uuid, name text,
               description text, distance_km float,
               photo_url text, latitude double precision,
               longitude double precision,
               created_at timestamptz)

Add firebase_uid column to users table:
  ALTER TABLE users ADD COLUMN IF NOT EXISTS
  firebase_uid text UNIQUE;

Disable RLS for MVP:
  ALTER TABLE users DISABLE ROW LEVEL SECURITY;
  ALTER TABLE homestays DISABLE ROW LEVEL SECURITY;
  ALTER TABLE menus DISABLE ROW LEVEL SECURITY;
  ALTER TABLE inquiries DISABLE ROW LEVEL SECURITY;
  ALTER TABLE local_spots DISABLE ROW LEVEL SECURITY;

=== APP PACKAGE STRUCTURE ===
com.nammahomestay/
  data/
    model/
      User.kt
      Homestay.kt
      Menu.kt
      Inquiry.kt
      LocalSpot.kt
    repository/
      AuthRepository.kt
      HomestayRepository.kt
      MenuRepository.kt
      InquiryRepository.kt
      LocalSpotRepository.kt
      GeminiRepository.kt
      StorageRepository.kt
    remote/
      SupabaseClient.kt
      GeminiApiService.kt
  ui/
    auth/
      LoginFragment.kt
      OtpFragment.kt
      RoleSelectionFragment.kt
    host/
      HostDashboardFragment.kt
      EditProfileFragment.kt
      MenuUploadFragment.kt
      InquiryBoxFragment.kt
      LocalGuideHostFragment.kt
      LocationPickerFragment.kt
      AnalyticsFragment.kt
      OnboardingFlowFragment.kt
    traveller/
      BrowseFragment.kt
      HomestayDetailFragment.kt
      SendInquiryFragment.kt
    common/
      SplashActivity.kt
      MainActivity.kt
      ImagePickerBottomSheet.kt
  viewmodel/
    AuthViewModel.kt
    HostViewModel.kt
    MenuViewModel.kt
    InquiryViewModel.kt
    TravellerViewModel.kt
  utils/
    ImageUtils.kt
    GeminiHelper.kt
    Constants.kt
    Extensions.kt

=== ALL SCREENS ===

1. SPLASH SCREEN
   - "Namma-HomeStay" in Yatra One font
   - Tagline: "Empowering Rural Hospitality"
   - Terracotta gradient background
   - After 2s: check Firebase auth state
     Logged in → check role in Supabase → MainActivity
     Not logged in → LoginFragment

2. LOGIN SCREEN
   - App logo and name at top
   - Phone number input with +91 prefix
   - "Send OTP" primary button
   - Divider "OR"
   - "Continue with Google" outlined button
   - Clean warm design

3. OTP SCREEN
   - "OTP sent to +91XXXXXXXXXX"
   - 6 individual digit boxes in a row
   - Auto advance between boxes
   - "Verify OTP" button
   - "Resend OTP" with 60s countdown timer
   - Loading indicator while verifying

4. ROLE SELECTION
   - "How will you use Namma-HomeStay?"
   - Two large cards with icons:
     Host card: house icon, "I have a home-stay"
     Traveller card: backpack icon, "I want to travel"
   - Tapping saves role to Supabase users table

5. HOST ONBOARDING (5 steps, progress bar at top)

   Step 1 - Basic Info:
   - Homestay name
   - Your full name
   - Village name
   - District
   - State (dropdown: Karnataka, Kerala, Goa, Maharashtra)
   - Phone number

   Step 2 - Upload Room Photos:
   CRITICAL: This must work perfectly
   - Title: "Show travellers your home"
   - Grid of 6 empty photo slots
   - Tap any slot → ImagePickerBottomSheet appears
     with "Take Photo" and "Choose Gallery" options
   - Both camera and gallery must work
   - Show photo preview in the slot after selection
   - Upload each photo to Supabase Storage "homestay-photos"
   - Store all public URLs in photos[] array in Supabase
   - Require minimum 3 photos, allow up to 8
   - Show upload progress for each photo
   - Show error if upload fails with retry option

   Step 3 - Room Details:
   - Number of rooms (large + and - buttons)
   - Per night rate in rupees (number input with rupee symbol)
   - Facilities (grid of toggle chips):
     WiFi, Hot Water, Home Food, AC, Farm Tour,
     Parking, Fishing, Cultural Experience, Bonfire,
     River View, Sea View, Mountain View

   Step 4 - Verification Checklist:
   - Explanatory text: "Self-verify to get a trust badge"
   - Four large checkboxes:
     Rooms have clean fresh bed sheets
     Toilet and bathroom are hygienic
     Food is cooked hygienically
     Safe drinking water available
   - Show green Verified badge preview when all 4 checked
   - Cannot proceed without checking all 4

   Step 5 - AI Description:
   - Show summary of entered info
   - "Generate Description with AI" button
   - Calls Gemini with homestay details
   - Editable result TextField
   - "Publish My Homestay" button
   - Save everything to Supabase homestays table
   - Navigate to HostDashboardFragment

6. HOST DASHBOARD (5 tab BottomNavigation)

   HOME TAB:
   - Greeting with host name
   - Homestay summary card with first photo
   - Available rooms toggle switch (updates Supabase instantly)
   - Tonight's rate display
   - Verification badges (4 green badges if all verified)
   - Quick stats: today's inquiries count
   - Edit Profile button

   MENU TAB:
   - Date header: "Today's Menu - {date}"
   - "Add Dish" FAB button
   - Tapping FAB: ImagePickerBottomSheet
     After image selected:
       Show image preview
       "Generate with AI" button
       Calls Gemini Vision with image as base64:
         Prompt: "Look at this Indian food dish photo.
         Return ONLY JSON, no markdown, no explanation:
         {dish_name: string, description: string,
         suggested_price_inr: number}"
       Show results in editable fields
       Price input prefilled from AI suggestion
       "Publish Dish" button:
         Upload photo to Supabase Storage "menu-photos"
         Save to menus table with today's date
         Show success message
   - RecyclerView of today's dishes
   - Each card: photo, name, description, price, delete

   INQUIRY BOX TAB:
   - RecyclerView newest first
   - Each card: name, message preview, time, unread dot
   - Tap to expand: full message, Call button, SMS button
   - Mark as read on open
   - Supabase Realtime subscription for live updates
   - In-app banner for new inquiry

   LOCAL GUIDE TAB:
   - osmdroid map (top half of screen)
   - Default center: Karnataka coast
   - Markers for all saved spots (fetched from Supabase)
   - Tap marker: show spot info popup
   - "Add Spot" FAB
   - Add Spot flow:
     1. Enter spot name and distance
     2. Upload photo (ImagePickerBottomSheet)
     3. "Pick Location on Map" button
        → Opens LocationPickerFragment:
          Full screen map
          Fixed crosshair at center
          Drag map to position crosshair
          Live coordinates display below map
          "Confirm Location" button at bottom
          Returns latitude/longitude
     4. "Generate Description" (Gemini API)
     5. "Save Spot" → saves to Supabase local_spots
        with photo URL and coordinates
   - List of all spots below map

   STATS TAB:
   - Total inquiries card
   - This week card
   - Menu items today card
   - Bar chart last 7 days (MPAndroidChart)
   - Profile views counter (from Supabase view_count)

7. TRAVELLER SCREENS

   BROWSE:
   - Search by district or village
   - Filter chips: All, Verified, Under ₹500, Under ₹1000
   - RecyclerView of homestay cards
   - Each card: photo, name, village, verified badge, price
   - Shimmer loading
   - Pull to refresh

   HOMESTAY DETAIL:
   - ViewPager2 photo gallery
     Load photos from Supabase Storage public URLs
     Use Glide to load each photo
     Show placeholder while loading
   - Name, village, district
   - Verified badges
   - AI description
   - Facilities chips
   - Tonight's menu (horizontal scroll)
   - Local guide spots (fetch from Supabase)
   - Mini map showing homestay location (osmdroid)
     Fetch latitude/longitude from Supabase homestays table
     Show single marker on small map
   - Sticky bottom: Call Host, Send Inquiry buttons
   - Increment view_count in Supabase on open

   SEND INQUIRY:
   - Traveller name and phone prefilled
   - Date picker
   - Guest count
   - Message input (300 char max)
   - Send button saves to Supabase inquiries table
   - Success animation (Lottie)

=== UI DESIGN SYSTEM ===

Colors:
  Primary:        #E07B39  terracotta orange
  PrimaryDark:    #C45E20
  Secondary:      #2D6A4F  forest green
  Background:     #FDF6EC  warm cream
  Surface:        #FFFFFF
  OnPrimary:      #FFFFFF
  TextPrimary:    #1A1A1A
  TextSecondary:  #6B6B6B
  Accent:         #F4A261
  Verified:       #27AE60
  Unverified:     #BDBDBD
  Error:          #D62828

Fonts:
  Yatra One: logo and main headings
  Poppins: all body text and UI labels

Dimensions:
  All buttons minimum 52dp height
  All touch targets minimum 48dp
  Card corners 16dp radius
  Button corners 24dp radius (pill shape)
  Screen horizontal padding 16dp

Animations:
  Shimmer on all list loading states
  Lottie checkmark on success actions
  Slide down banner for new inquiries
  Smooth fragment transitions

=== GEMINI API ===

Base URL: https://generativelanguage.googleapis.com/
Endpoint: v1beta/models/gemini-1.5-flash:generateContent
Key: BuildConfig.GEMINI_API_KEY (from local.properties)

For text prompts:
  {"contents": [{"parts": [{"text": "prompt"}]}]}

For image + text:
  {"contents": [{"parts": [
    {"inline_data": {"mime_type": "image/jpeg",
                     "data": "base64string"}},
    {"text": "prompt"}
  ]}]}

Convert Bitmap to base64:
  val outputStream = ByteArrayOutputStream()
  bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
  val base64 = Base64.encodeToString(
    outputStream.toByteArray(), Base64.NO_WRAP)

GeminiRepository:
  suspend fun generateMenuDescription(bitmap: Bitmap): MenuAiResult
  suspend fun generateHomestayDescription(
    name: String, village: String,
    district: String, facilities: List<String>): String
  suspend fun generateSpotDescription(
    name: String, distance: Float): String

All functions must:
  Return fallback text if API fails
  Show "AI unavailable, type manually" toast on error
  Never crash the app on API failure

=== OSMDROID SETUP ===

Dependency: org.osmdroid:osmdroid-android:6.1.18

In NammaApp (Application class):
  Configuration.getInstance().load(this,
    PreferenceManager.getDefaultSharedPreferences(this))
  Configuration.getInstance().userAgentValue = packageName

Permissions:
  INTERNET, ACCESS_FINE_LOCATION,
  ACCESS_COARSE_LOCATION,
  WRITE_EXTERNAL_STORAGE (maxSdkVersion 28)

Always call in Fragment lifecycle:
  onResume: mapView.onResume()
  onPause: mapView.onPause()

LocationPickerFragment layout:
  FrameLayout (full screen)
  ├── MapView (match_parent, match_parent)
  ├── ImageView crosshair (center gravity, 48x48dp)
  ├── TextView coordinates (bottom, above button)
  └── Button "Confirm This Location" (bottom, full width)

Update coordinates TextView as map scrolls:
  mapView.addMapListener(object : MapListener {
    override fun onScroll(event: ScrollEvent): Boolean {
      val center = mapView.mapCenter
      coordsText.text = "Lat: %.4f, Lng: %.4f"
        .format(center.latitude, center.longitude)
      return true
    }
    override fun onZoom(event: ZoomEvent) = true
  })

=== GRADLE DEPENDENCIES ===

plugins:
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("com.google.gms.google-services")
  id("kotlin-kapt")

android:
  compileSdk 34
  minSdk 26
  targetSdk 34
  buildFeatures { buildConfig = true }
  packagingOptions {
    exclude "META-INF/INDEX.LIST"
    exclude "META-INF/DEPENDENCIES"
  }

dependencies:
  // Firebase
  implementation(platform("com.google.firebase:firebase-bom:32.7.4"))
  implementation("com.google.firebase:firebase-auth-ktx")
  implementation("com.google.android.gms:play-services-auth:20.7.0")

  // Supabase
  implementation(platform("io.github.jan-tennert.supabase:bom:2.6.1"))
  implementation("io.github.jan-tennert.supabase:postgrest-kt")
  implementation("io.github.jan-tennert.supabase:storage-kt")
  implementation("io.github.jan-tennert.supabase:realtime-kt")
  implementation("io.ktor:ktor-client-android:2.3.12")

  // Gemini via Retrofit
  implementation("com.squareup.retrofit2:retrofit:2.11.0")
  implementation("com.squareup.retrofit2:converter-gson:2.11.0")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

  // UI Components
  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.recyclerview:recyclerview:1.3.2")
  implementation("androidx.viewpager2:viewpager2:1.1.0")

  // Image loading
  implementation("com.github.bumptech.glide:glide:4.16.0")
  kapt("com.github.bumptech.glide:compiler:4.16.0")

  // Maps (free, no API key)
  implementation("org.osmdroid:osmdroid-android:6.1.18")

  // Charts
  implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

  // Animations
  implementation("com.airbnb.android:lottie:6.4.0")
  implementation("com.facebook.shimmer:shimmer:0.5.0")

  // Navigation
  implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
  implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

  // ViewModel + Coroutines
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

  // Core
  implementation("androidx.core:core-ktx:1.13.1")
  implementation("androidx.activity:activity-ktx:1.9.0")
  implementation("androidx.fragment:fragment-ktx:1.8.0")

=== FILES TO GENERATE (ALL) ===
  app/build.gradle.kts
  app/src/main/AndroidManifest.xml
  app/src/main/res/xml/file_paths.xml
  app/src/main/java/.../NammaApp.kt
  app/src/main/java/.../data/model/User.kt
  app/src/main/java/.../data/model/Homestay.kt
  app/src/main/java/.../data/model/Menu.kt
  app/src/main/java/.../data/model/Inquiry.kt
  app/src/main/java/.../data/model/LocalSpot.kt
  app/src/main/java/.../data/remote/SupabaseClient.kt
  app/src/main/java/.../data/remote/GeminiApiService.kt
  app/src/main/java/.../data/repository/AuthRepository.kt
  app/src/main/java/.../data/repository/HomestayRepository.kt
  app/src/main/java/.../data/repository/MenuRepository.kt
  app/src/main/java/.../data/repository/InquiryRepository.kt
  app/src/main/java/.../data/repository/LocalSpotRepository.kt
  app/src/main/java/.../data/repository/GeminiRepository.kt
  app/src/main/java/.../data/repository/StorageRepository.kt
  app/src/main/java/.../viewmodel/AuthViewModel.kt
  app/src/main/java/.../viewmodel/HostViewModel.kt
  app/src/main/java/.../viewmodel/MenuViewModel.kt
  app/src/main/java/.../viewmodel/InquiryViewModel.kt
  app/src/main/java/.../viewmodel/TravellerViewModel.kt
  app/src/main/java/.../ui/common/SplashActivity.kt
  app/src/main/java/.../ui/common/MainActivity.kt
  app/src/main/java/.../ui/common/ImagePickerBottomSheet.kt
  app/src/main/java/.../ui/auth/LoginFragment.kt
  app/src/main/java/.../ui/auth/OtpFragment.kt
  app/src/main/java/.../ui/auth/RoleSelectionFragment.kt
  app/src/main/java/.../ui/host/HostDashboardFragment.kt
  app/src/main/java/.../ui/host/OnboardingFlowFragment.kt
  app/src/main/java/.../ui/host/EditProfileFragment.kt
  app/src/main/java/.../ui/host/MenuUploadFragment.kt
  app/src/main/java/.../ui/host/InquiryBoxFragment.kt
  app/src/main/java/.../ui/host/LocalGuideHostFragment.kt
  app/src/main/java/.../ui/host/LocationPickerFragment.kt
  app/src/main/java/.../ui/host/AnalyticsFragment.kt
  app/src/main/java/.../ui/traveller/BrowseFragment.kt
  app/src/main/java/.../ui/traveller/HomestayDetailFragment.kt
  app/src/main/java/.../ui/traveller/SendInquiryFragment.kt
  app/src/main/res/layout/ (all XML layouts for every screen)
  app/src/main/res/values/colors.xml
  app/src/main/res/values/themes.xml
  app/src/main/res/values/strings.xml
  app/src/main/res/values/dimens.xml
  app/src/main/res/drawable/ic_crosshair.xml
  app/src/main/res/navigation/nav_graph.xml
  app/src/main/res/menu/bottom_nav_menu.xml
  local.properties.example
  README.md

=== SUCCESS CRITERIA ===
  Image picking works from both camera and gallery everywhere
  All photos upload to Supabase Storage and display correctly
  Map location picker uses drag-crosshair method (easy to use)
  All coordinates saved to and loaded from Supabase
  Firebase phone OTP works for login
  Google sign-in works as backup
  Menu dish AI generation works with photo
  Daily menu update completes in under 60 seconds
  Verification badges show on listing cards
  Terracotta color scheme throughout
  All touch targets minimum 48dp
  Real-time inquiry updates work
  App runs on Android 8.0 with 2GB RAM
  No Google Maps SDK used anywhere
  No third party image picker library used

=== AUTH ARCHITECTURE CHANGE (CRITICAL) ===

REMOVE Supabase Auth completely from this project.

DO NOT use:
- Supabase Auth
- Supabase login/signup APIs
- Supabase session handling

USE Firebase Auth as the ONLY authentication system.

Firebase Auth Responsibilities:
- Phone OTP authentication (primary login method)
- Google Sign-In (backup login method)
- Maintain user session using FirebaseAuth
- Provide Firebase UID for each user

Supabase Responsibilities:
- ONLY for database (PostgreSQL)
- ONLY for storage (images)
- ONLY for realtime updates

User Identity Flow:
- After Firebase login success:
    val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid

- Use this firebaseUid to:
    1. Check if user exists in Supabase users table
    2. If NOT exists → create new user record
    3. If exists → fetch user profile

- Supabase users table MUST include:
    firebase_uid text UNIQUE

- ALL queries to Supabase must use firebase_uid as the user identifier
  instead of Supabase auth user id

Session Handling:
- Use ONLY FirebaseAuth.getInstance().currentUser
- Do NOT implement any Supabase session management

Logout:
- Use FirebaseAuth.getInstance().signOut()

Security Note (MVP):
- Disable Supabase RLS (Row Level Security)
- Trust Firebase UID for identifying users

=== FIREBASE CONFIGURATION (CRITICAL) ===

The Firebase configuration file `google-services.json` is already provided.

Location:
- The file is located inside the `app/` directory of the Android project.
- Exact path: app/google-services.json

Instructions:
- DO NOT create or move this file.
- DO NOT place it in root or any other folder.
- Use this existing file for Firebase initialization.

Gradle Requirements:
- Ensure the following plugin is applied in app-level build.gradle.kts:
  id("com.google.gms.google-services")

- Ensure project-level build.gradle includes:
  classpath("com.google.gms:google-services:4.4.1")

Firebase Usage:
- Initialize Firebase automatically using google-services.json
- Use FirebaseAuth for authentication
- Do not manually configure Firebase project settings in code

=== END FIREBASE CONFIGURATION ===
