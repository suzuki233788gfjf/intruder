import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:firebase_auth/firebase_auth.dart'; // Importez FirebaseAuth
import 'package:intruder/screens/welcome_screen.dart'; // Pour rediriger après déconnexion

class SecuritySettingsScreen extends StatefulWidget {
  const SecuritySettingsScreen({super.key});

  @override
  State<SecuritySettingsScreen> createState() => _SecuritySettingsScreenState();
}

class _SecuritySettingsScreenState extends State<SecuritySettingsScreen> {
  static const platform = MethodChannel('com.yourcompany.anti_theft_app/device_admin');

  bool _protectionActive = false;
  final TextEditingController _emailController = TextEditingController();
  int _failedAttemptsThreshold = 3;

  @override
  void initState() {
    super.initState();
    _loadSettings();
    // Vous pouvez également afficher l'e-mail de l'utilisateur connecté ici si vous voulez
    // User? currentUser = FirebaseAuth.instance.currentUser;
    // if (currentUser != null) {
    //   _emailController.text = currentUser.email ?? '';
    // }
  }

  @override
  void dispose() {
    _emailController.dispose();
    super.dispose();
  }

  // --- Logique de chargement et sauvegarde des paramètres (via Platform Channel) ---
  Future<void> _loadSettings() async {
    try {
      final Map<dynamic, dynamic>? settings = await platform.invokeMethod('loadSettings');
      if (settings != null && mounted) {
        setState(() {
          _emailController.text = settings['email'] as String? ?? ''; // Gérer le cas null
          _failedAttemptsThreshold = settings['threshold'] as int? ?? 3; // Gérer le cas null
          _protectionActive = settings['enabled'] as bool? ?? false; // Gérer le cas null
        });
      }
    } on PlatformException catch (e) {
      _showSnackBar("Erreur lors du chargement des paramètres : ${e.message}", isError: true);
    }
  }

  Future<void> _saveSettings() async {
    try {
      await platform.invokeMethod('saveSettings', {
        'email': _emailController.text,
        'threshold': _failedAttemptsThreshold,
        'enabled': _protectionActive,
      });
      if (mounted) {
        _showSnackBar('Paramètres sauvegardés !');
      }
    } on PlatformException catch (e) {
      _showSnackBar("Erreur lors de la sauvegarde des paramètres : ${e.message}", isError: true);
    }
  }

  // --- Logique d'activation/désactivation de la protection (via Platform Channel) ---
  Future<void> _toggleProtection(bool? value) async {
    if (value == null) return;

    if (value) {
      final bool granted = await _requestDeviceAdminPermission();
      if (mounted) {
        setState(() {
          _protectionActive = granted;
        });
        if (granted) {
          _showSnackBar('Protection Anti-Vol activée !');
          _saveSettings();
        }
      }
    } else {
      final bool disabled = await _disableDeviceAdminPermission();
      if (mounted) {
        setState(() {
          _protectionActive = !disabled;
        });
        if (disabled) {
          _showSnackBar('Protection Anti-Vol désactivée.');
          _saveSettings();
        }
      }
    }
  }

  Future<bool> _requestDeviceAdminPermission() async {
    try {
      final bool granted = await platform.invokeMethod('requestDeviceAdmin');
      return granted;
    } on PlatformException catch (e) {
      _showSnackBar("Erreur lors de la demande d'administrateur : ${e.message}", isError: true);
      return false;
    }
  }

  Future<bool> _disableDeviceAdminPermission() async {
     try {
      final bool disabled = await platform.invokeMethod('disableDeviceAdmin');
      return disabled;
    } on PlatformException catch (e) {
      _showSnackBar("Erreur lors de la désactivation d'administrateur : ${e.message}", isError: true);
      return false;
    }
  }

  // --- Logique de déconnexion ---
  Future<void> _logout() async {
    try {
      await FirebaseAuth.instance.signOut();
      if (mounted) {
        // Rediriger vers l'écran de bienvenue après la déconnexion
        Navigator.pushAndRemoveUntil(
          context,
          MaterialPageRoute(builder: (context) => WelcomeScreen()),
          (Route<dynamic> route) => false, // Supprime toutes les routes précédentes
        );
        _showSnackBar('Déconnecté avec succès !');
      }
    } on FirebaseAuthException catch (e) {
      if (mounted) {
        _showSnackBar('Erreur de déconnexion : ${e.message}', isError: true);
      }
    }
  }

  void _showSnackBar(String message, {bool isError = false}) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: isError ? Colors.red : Colors.green,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: const Text('Security Settings', style: TextStyle(color: Colors.white)),
        centerTitle: true,
        actions: [
          IconButton(
            icon: const Icon(Icons.logout, color: Colors.white),
            onPressed: _logout, // Bouton de déconnexion
            tooltip: 'Déconnexion',
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 20), // Réduit l'espace initial car il y a une AppBar
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Text(
                  'INTRUDER',
                  style: TextStyle(
                    fontSize: 24,
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(width: 10),
                Icon(
                  Icons.fingerprint,
                  size: 34,
                  color: Colors.tealAccent[400],
                ),
              ],
            ),
            const SizedBox(height: 40),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Activer la protection',
                  style: TextStyle(fontSize: 16, color: Colors.white),
                ),
                Switch(
                  value: _protectionActive,
                  activeColor: Colors.tealAccent,
                  onChanged: _toggleProtection,
                ),
              ],
            ),
            const SizedBox(height: 25),
            const Text(
              'Adresse e-mail valide pour la destination',
              style: TextStyle(fontSize: 16, color: Colors.white),
            ),
            const SizedBox(height: 10),
            TextFormField(
              controller: _emailController,
              decoration: const InputDecoration(
                hintText: 'exemple@domain.com',
              ),
              keyboardType: TextInputType.emailAddress,
              onChanged: (value) {
                _saveSettings();
              },
            ),
            const SizedBox(height: 25),
            Text(
              'Seuil de tentatives échouées: $_failedAttemptsThreshold',
              style: const TextStyle(fontSize: 16, color: Colors.white),
            ),
            const SizedBox(height: 10),
            Slider(
              value: _failedAttemptsThreshold.toDouble(),
              min: 1,
              max: 10,
              divisions: 9,
              label: _failedAttemptsThreshold.toString(),
              activeColor: Colors.tealAccent,
              onChanged: (double value) {
                setState(() {
                  _failedAttemptsThreshold = value.toInt();
                });
                _saveSettings();
              },
            ),
            const SizedBox(height: 30),
            ElevatedButton.icon(
              onPressed: () {
                _showSnackBar('Pour tester, verrouillez votre appareil et essayez de déverrouiller avec le mauvais code.');
              },
              icon: const Icon(Icons.security),
              label: const Text('Comment tester la protection'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.tealAccent,
                foregroundColor: Colors.black,
                minimumSize: const Size.fromHeight(50),
              ),
            ),
          ],
        ),
      ),
    );
  }
}