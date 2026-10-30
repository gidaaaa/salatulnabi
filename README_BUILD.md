كيف تبني APK و AAB لرفعه على جوجل بلاي:

1. افتح Android Studio -> Open -> اختر مجلد SalatNabi_PlayStore
2. انتظر Gradle Sync
3. لإنشاء Keystore (أول مرة فقط):
   Build > Generate Signed Bundle/APK > Android App Bundle > Create new
   - Key store path: salat-key.jks
   - Password: اختر باسورد واحفظه
   - Key alias: salat
   - Validity: 25 سنة
4. Build > Generate Signed Bundle/APK > Android App Bundle (AAB) - هذا المطلوب لجوجل بلاي
   و APK لو عايز تجربه على موبايلك
5. الملف هيطلع في app/release/app-release.aab

للرفع على جوجل بلاي:
- ادخل play.google.com/console
- إنشاء تطبيق جديد > اسم: الصلاة على النبي
- ارفع ملف AAB
- املأ Data Safety: No data collected
- في Permissions: وضح أن SCHEDULE_EXACT_ALARM للذكر الديني
- ارفع Privacy Policy (استخدم ملف PRIVACY_POLICY.md وارفعه على موقعك أو GitHub)
- Screenshots: خد سكرين من المعاينة

مهم: غير أيقونة التطبيق الحالية (مؤقتة) بأيقونة احترافية 512x512