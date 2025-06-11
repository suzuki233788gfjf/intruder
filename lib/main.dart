import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart'; // Importez ceci
import 'package:firebase_auth/firebase_auth.dart'; // Importez ceci
import 'package:intruder/firebase_options.dart'; // Ceci sera généré automatiquement
import 'package:intruder/screens/welcome_screen.dart';
import 'package:intruder/screens/security_settings_screen.dart'; // Pour l'écran après connexion

void main() async {
  WidgetsFlutterBinding.ensureInitialized(); // Assurez-vous que les bindings Flutter sont initialisés
  await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform, // Initialise Firebase
  );
  runApp(const IntruderApp());
}

class IntruderApp extends StatelessWidget {
  const IntruderApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Intruder',
      debugShowCheckedModeBanner: false,
      theme: ThemeData.dark().copyWith(
        scaffoldBackgroundColor: const Color(0xFF010D17),
        textTheme: const TextTheme(
          bodyLarge: TextStyle(color: Colors.white),
          bodyMedium: TextStyle(color: Colors.white),
          bodySmall: TextStyle(color: Colors.white),
          displayLarge: TextStyle(color: Colors.white),
          displayMedium: TextStyle(color: Colors.white),
          displaySmall: TextStyle(color: Colors.white),
          headlineLarge: TextStyle(color: Colors.white),
          headlineMedium: TextStyle(color: Colors.white),
          headlineSmall: TextStyle(color: Colors.white),
          titleLarge: TextStyle(color: Colors.white),
          titleMedium: TextStyle(color: Colors.white),
          titleSmall: TextStyle(color: Colors.white),
          labelLarge: TextStyle(color: Colors.white),
          labelMedium: TextStyle(color: Colors.white),
          labelSmall: TextStyle(color: Colors.white),
        ),
        inputDecorationTheme: InputDecorationTheme(
          filled: true,
          fillColor: const Color(0xFFD9D9D9),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(8),
            borderSide: BorderSide.none,
          ),
          hintStyle: TextStyle(color: Colors.grey[700]),
        ),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(8),
            ),
          ),
        ),
        switchTheme: SwitchThemeData(
          thumbColor: MaterialStateProperty.resolveWith((states) {
            if (states.contains(MaterialState.selected)) {
              return Colors.tealAccent;
            }
            return Colors.grey[400];
          }),
          trackColor: MaterialStateProperty.resolveWith((states) {
            if (states.contains(MaterialState.selected)) {
              return Colors.tealAccent.withOpacity(0.5);
            }
            return Colors.grey[600];
          }),
        ),
        sliderTheme: SliderThemeData(
          activeTrackColor: Colors.tealAccent,
          inactiveTrackColor: Colors.grey[600],
          thumbColor: Colors.tealAccent,
          overlayColor: Colors.tealAccent.withOpacity(0.2),
          valueIndicatorColor: Colors.tealAccent,
          valueIndicatorTextStyle: const TextStyle(color: Colors.black),
        ),
      ),
      // Utilisez un StreamBuilder pour écouter les changements d'état d'authentification
      home: StreamBuilder<User?>(
        stream: FirebaseAuth.instance.authStateChanges(),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            // Afficher un écran de chargement pendant la vérification de l'état
            return const Scaffold(
              backgroundColor: Color(0xFF010D17),
              body: Center(child: CircularProgressIndicator(color: Colors.tealAccent)),
            );
          }
          if (snapshot.hasData) {
            // Si l'utilisateur est connecté, aller à l'écran des paramètres
            return const SecuritySettingsScreen();
          }
          // Sinon, aller à l'écran de bienvenue (authentification)
          return WelcomeScreen();
        },
      ),
    );
  }
}