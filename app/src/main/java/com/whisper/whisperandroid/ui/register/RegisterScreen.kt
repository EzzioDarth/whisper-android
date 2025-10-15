package com.whisper.whisperandroid.ui.register

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit, //called at a succesful signed up
    onGoToLogin: () -> Unit // switch to login
){
    val auth = remember { FirebaseAuth.getInstance() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun register(){
        val e = email.trim()
        val p = password
        val c = confirm
        if(e.isEmpty()){error = "Email is Required"; return}
        if(p.length < 6){error = "Password must be at least 6 characters."; return}
        if(p != c){error = "Password do not match."; return}

        error = null
        loading = true
        auth.createUserWithEmailAndPassword(e, p).addOnCompleteListener { signUp ->
            loading = false
            if(signUp.isSuccessful){
                onRegistered()
            } else{
                error = signUp.exception?.localizedMessage ?: "Registration failed."
            }
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Create account", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password (min 6)") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = confirm, onValueChange = { confirm = it },
                label = { Text("Confirm password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                enabled = !loading,
                onClick = { register() },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (loading) "Creating…" else "Create account") }

            TextButton(onClick = onGoToLogin, enabled = !loading) {
                Text("Already have an account? Sign in")
            }

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        }

        if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
    }

}