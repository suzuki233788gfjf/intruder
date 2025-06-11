
// lib/screens/welcome_screen.dart
import 'package:flutter/material.dart';
import '../widgets/custom_button.dart';
import 'signup_screen.dart'; 
import 'login_screen.dart';

class WelcomeScreen extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Color(0xFF010D17),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Image.asset(
                'assets/fingerprint.png',
                height: 100,
              ),
              SizedBox(height: 30),
              Text(
                'WELCOME TO INTRUDER',
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 20,
                  color: Colors.white,
                ),
              ),
              SizedBox(height: 30),
              CustomButton(
                text: 'Sign up',
                onPressed: () {
                  // Naviguer vers SignUpScreen
                  Navigator.push(
                    context,
                    MaterialPageRoute(builder: (context) => SignUpScreen()),
                  );
                },
              ),
              SizedBox(height: 15),
              CustomButton(
                text: 'Log in',
                onPressed: () {
                  // TODO: Naviguer vers LogInScreen (à créer)
                  Navigator.push(
                    context,
                    MaterialPageRoute(builder: (context) => LoginScreen()),
                  );
                },
              ),
            ],
          ),
        ),
      ),
    );
  }
}