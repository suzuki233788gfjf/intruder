import 'package:flutter/material.dart';
import 'package:firebase_auth/firebase_auth.dart'; // Importez FirebaseAuth
import 'package:intruder/screens/login_screen.dart'; // Pour la navigation vers Login
import 'package:intruder/screens/security_settings_screen.dart'; // Pour la navigation après inscription

class SignUpScreen extends StatefulWidget {
  const SignUpScreen({super.key});

  @override
  State<SignUpScreen> createState() => _SignUpScreenState();
}

class _SignUpScreenState extends State<SignUpScreen> {
  final TextEditingController _firstNameController = TextEditingController();
  final TextEditingController _lastNameController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final TextEditingController _confirmPasswordController = TextEditingController();

  bool _isLoading = false; // État de chargement pour le bouton

  @override
  void dispose() {
    _firstNameController.dispose();
    _lastNameController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  Future<void> _signUp() async {
    setState(() {
      _isLoading = true; // Active l'état de chargement
    });

    try {
      if (_passwordController.text != _confirmPasswordController.text) {
        _showSnackBar('Les mots de passe ne correspondent pas.', isError: true);
        return;
      }

      await FirebaseAuth.instance.createUserWithEmailAndPassword(
        email: _emailController.text.trim(),
        password: _passwordController.text.trim(),
      );

      // Si l'inscription réussit, naviguer vers l'écran des paramètres de sécurité
      if (mounted) {
        Navigator.pushReplacement(
          context,
          MaterialPageRoute(builder: (context) => const SecuritySettingsScreen()),
        );
      }
    } on FirebaseAuthException catch (e) {
      if (mounted) {
        _showSnackBar('Erreur d\'inscription : ${e.message}', isError: true);
      }
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false; // Désactive l'état de chargement
        });
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
      backgroundColor: const Color(0xFF010D17),
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Colors.white),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16.0),
          child: Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: const Color(0xFF837E7E),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  'SIGN UP',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 20,
                    letterSpacing: 1,
                    fontWeight: FontWeight.w400,
                  ),
                ),
                const SizedBox(height: 20),

                _buildTextField('First name', 'Your first name', _firstNameController),
                const SizedBox(height: 15),
                _buildTextField('Last name', 'Your last name', _lastNameController),
                const SizedBox(height: 15),
                _buildTextField('Email', 'Your email address', _emailController, keyboardType: TextInputType.emailAddress),
                const SizedBox(height: 15),
                _buildTextField('Password', 'Your password', _passwordController, obscureText: true),
                const SizedBox(height: 15),
                _buildTextField('Confirm Password', 'Confirm your password', _confirmPasswordController, obscureText: true),
                const SizedBox(height: 20),

                ElevatedButton(
                  onPressed: _isLoading ? null : _signUp, // Désactive le bouton pendant le chargement
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFD9D9D9),
                    foregroundColor: Colors.black,
                    minimumSize: const Size(double.infinity, 45),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                  ),
                  child: _isLoading
                      ? const CircularProgressIndicator(color: Colors.black) // Affiche un indicateur de chargement
                      : const Text('Sign Up'),
                ),
                const SizedBox(height: 15),

                Row(
                  children: [
                    Expanded(child: Divider(color: Colors.white)),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 8),
                      child: Text('or', style: TextStyle(color: Colors.white)),
                    ),
                    Expanded(child: Divider(color: Colors.white)),
                  ],
                ),
                const SizedBox(height: 15),

                ElevatedButton(
                  onPressed: () {
                    // TODO: Implémenter l'inscription avec Google
                    _showSnackBar('Inscription avec Google non implémentée.', isError: false);
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFFD9D9D9),
                    foregroundColor: Colors.black,
                    minimumSize: const Size(double.infinity, 45),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                  ),
                  child: const Text('Continue with Google'),
                ),
                const SizedBox(height: 15),

                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    const Text("Already have an account? ", style: TextStyle(color: Colors.white)),
                    GestureDetector(
                      onTap: () {
                        // Utilise pop pour revenir à l'écran de connexion si existant, ou pushReplacement si direct
                        Navigator.pushReplacement(
                          context,
                          MaterialPageRoute(builder: (context) => const LoginScreen()),
                        );
                      },
                      child: const Text(
                        "Log in",
                        style: TextStyle(color: Colors.blueAccent),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  // Fonction utilitaire pour construire les champs de texte
  Widget _buildTextField(String label, String hint, TextEditingController controller, {bool obscureText = false, TextInputType keyboardType = TextInputType.text}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Align(
          alignment: Alignment.centerLeft,
          child: Text(label, style: const TextStyle(color: Colors.white)),
        ),
        const SizedBox(height: 5),
        TextField(
          controller: controller,
          obscureText: obscureText,
          keyboardType: keyboardType,
          decoration: InputDecoration(
            hintText: hint,
            // Les autres styles (filled, fillColor, border) sont gérés par le ThemeData global
          ),
        ),
      ],
    );
  }
}