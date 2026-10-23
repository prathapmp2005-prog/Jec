# JEC Final Native Android
Pure native Android app. No Capacitor/WebView/native JS bridge is used.

Features included from the earlier JEC prototypes:
- Animated JEC startup screen
- Firebase Phone OTP login
- Admin / Employee role gate
- GPS attendance check-in/check-out
- Trip start/end and distance calculation
- Petrol/expense claims
- Bill/photo/document upload to Firebase Storage
- Leave requests
- Salary view + admin salary entry
- Employee profile
- Admin views for staff, attendance, trips, claims and leave requests
- Firestore cloud data storage

Firebase Android package: `in.jec.jagenelectriciancompany`

Important Firebase Console requirements:
1. Authentication > Phone must be enabled.
2. Firestore Database must exist.
3. Storage must exist.
4. Add the debug/release SHA-1 and SHA-256 fingerprints to the Firebase Android app for reliable Phone Auth app verification.
5. Replace test-mode Firestore/Storage rules with role-aware production rules before real employee data is used.

Build with GitHub Actions: `.github/workflows/build-apk.yml`.
