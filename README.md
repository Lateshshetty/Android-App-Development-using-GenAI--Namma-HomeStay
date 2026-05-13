# Namma-HomeStay

Kotlin MVVM Android host/traveller portal for rural coastal homestays.

## Configuration

Create `local.properties` from `local.properties.example` and set:

- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`
- `GEMINI_API_KEY`

`local.properties` is ignored by Git so your local SDK path and API keys are not pushed.

Firebase Auth is configured from `app/google-services.json`. That file is also ignored by Git because it is project-specific. For a fresh checkout, copy `app/google-services.example.json` to `app/google-services.json` and replace the placeholder Firebase values from your Firebase console.

The app uses Firebase Auth for login, Supabase for database/storage, osmdroid maps, MPAndroidChart, XML Material UI, and Jetpack Navigation. It does not use Google Maps SDK.
