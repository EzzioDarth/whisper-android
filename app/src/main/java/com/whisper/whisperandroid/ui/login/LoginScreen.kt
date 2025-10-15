package com.whisper.whisperandroid.ui.login
//to keep the file within the project
import androidx.compose.foundation.layout.* //layout
import androidx.compose.material3.* //text buttons..
import androidx.compose.runtime.*//state managemnet
import androidx.compose.ui.Alignment //allignement
import androidx.compose.ui.Modifier // chain behavior instruction for composable
import androidx.compose.ui.text.input.PasswordVisualTransformation //mask password
import androidx.compose.ui.unit.dp //spacing padding...
//import com.google.firebase.auth.ktx.auth
//import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth //FireBase authentification API

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit
) {
    //composable functio UI + behavior
    //accepting 2 callbacks on succes and on registration
    val auth = FirebaseAuth.getInstance()
    //firebase instance
    val scope = rememberCoroutineScope()
    //coroutine scope not used for now maybe later

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    //vars each track imput fields unless
    //loading shows progress
    //error to stor error message

    fun signIn() {
        error = null
        loading = true
        //reset vars act like flags
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnCompleteListener { task ->
                loading = false
                if (task.isSuccessful) onLoginSuccess()
                else error = task.exception?.localizedMessage ?: "Login failed"
            }
        //call firebase authentification methods with email and password
        //success: callback MainActivity and show message
        //fails: display error message
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        //container to fill screen with things
        Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            //to arrage things vertically
            Text("Whisper — Login", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            //tite with app name

            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            //input field for email
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            //input field for password

            Button(
                enabled = email.isNotBlank() && password.length >= 6 && !loading,
                onClick = { signIn() },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (loading) "Signing in..." else "Sign In") }
            //loggin button
            //enables only when email field is blank
            //when clicked calls signIn()

            TextButton(onClick = onGoToRegister, enabled = !loading) {
                Text("No account? Create one")
                //calls onGoToRegister() callback
            }

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error)
                ////when error set displays error message
            }
        }
        if (loading) CircularProgressIndicator(Modifier.align(Alignment.Center))
        //progress spinner while loading
    }
}
