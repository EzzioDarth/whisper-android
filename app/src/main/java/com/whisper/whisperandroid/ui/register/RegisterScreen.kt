// RegisterScreen.kt – inside your sign-up handler
val scope = rememberCoroutineScope()
var loading by remember { mutableStateOf(false) }
var error by remember { mutableStateOf<String?>(null) }

fun signUp(email: String, password: String, displayName: String) {
    if (loading) return
    error = null
    loading = true
    scope.launch {
        try {
            // 1) Create the auth record
            val body = mapOf(
                "email" to email.trim(),
                "password" to password,
                "passwordConfirm" to password,
                "displayName" to displayName
            )
            // raw POST (reuse your Retrofit or use OkHttp quickly):
            val url = "${com.whisper.whisperandroid.core.PbConfig.BASE}/api/collections/users/records"
            val req = okhttp3.Request.Builder()
                .url(url)
                .post(
                    okhttp3.RequestBody.create(
                        okhttp3.MediaType.parse("application/json"),
                        com.squareup.moshi.Moshi.Builder().build()
                            .adapter(Map::class.java).toJson(body)
                    )
                ).build()
            val resp = okhttp3.OkHttpClient().newCall(req).execute()
            check(resp.isSuccessful) { "Sign up failed: ${resp.code()}" }

            // 2) Immediately log in (reuse the same flow as LoginScreen)
            val backend = com.whisper.whisperandroid.core.ServiceLocator.backend
            val session = backend.login(email.trim(), password)
            backend.ensureKeypairAndUploadPubKey()

            loading = false
            onRegisterSuccess() // navigate like you already do
        } catch (e: Exception) {
            loading = false
            error = e.localizedMessage ?: "Registration failed"
        }
    }
}
