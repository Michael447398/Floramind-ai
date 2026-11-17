package com.example.base

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.net.HttpURLConnection
import java.net.URL

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
abstract class FirestoreEmulatorTestBase {

    protected lateinit var firestore: FirebaseFirestore
    protected lateinit var auth: FirebaseAuth
    protected lateinit var databaseId: String

    @Before
    open fun setUpFirebase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resId = context.resources.getIdentifier("firestore_database_id", "string", context.packageName)
        databaseId = if (resId != 0) {
            context.getString(resId)
        } else {
            "ai-studio-android-529ff59b-3583-4895-a9f6-00a671592740"
        }

        val app = if (FirebaseApp.getApps(context).isEmpty()) {
            val newApp = FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("com.aistudio.floramind.qrzmkn")
                    .setProjectId(PROJECT_ID)
                    .setApiKey("fake-api-key-for-emulator")
                    .build()
            )
            val newFirestore = FirebaseFirestore.getInstance(newApp, databaseId)
            newFirestore.useEmulator(EMULATOR_HOST, FIRESTORE_PORT)
            newFirestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                .build()

            val newAuth = FirebaseAuth.getInstance(newApp)
            newAuth.useEmulator(EMULATOR_HOST, AUTH_PORT)
            newApp
        } else {
            FirebaseApp.getInstance()
        }

        firestore = FirebaseFirestore.getInstance(app, databaseId)
        auth = FirebaseAuth.getInstance(app)
    }

    @After
    open fun tearDownFirebase() {
        auth.signOut()
    }

    protected suspend fun signInTestUser(email: String, markEmailVerified: Boolean = false): String =
        withContext(Dispatchers.IO) {
            withTimeout(AUTH_TIMEOUT_MS) {
                val result = try {
                    auth.signInWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
                } catch (_: FirebaseAuthInvalidUserException) {
                    auth.createUserWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
                } catch (e: Exception) {
                    if (e.message?.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) == true ||
                        e.message?.contains("EMAIL_NOT_FOUND", ignoreCase = true) == true
                    ) {
                        auth.createUserWithEmailAndPassword(email, DEFAULT_PASSWORD).await()
                    } else {
                        throw e
                    }
                }
                val user = checkNotNull(result.user) { "User auth failed" }
                if (markEmailVerified && !user.isEmailVerified) {
                    val token = user.getIdToken(false).await().token
                    if (!token.isNullOrBlank()) {
                        val sendUrl = URL("http://$EMULATOR_HOST:$AUTH_PORT/identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=fake-api-key-for-emulator")
                        val sendConn = (sendUrl.openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            doOutput = true
                            setRequestProperty("Content-Type", "application/json")
                        }
                        sendConn.outputStream.use { os ->
                            os.write("""{"requestType":"VERIFY_EMAIL","idToken":"$token"}""".toByteArray())
                        }
                        sendConn.inputStream.use { it.readBytes() }
                        sendConn.disconnect()

                        val oobUrl = URL("http://$EMULATOR_HOST:$AUTH_PORT/emulator/v1/projects/$PROJECT_ID/oobCodes")
                        val oobJsonText = oobUrl.openStream().bufferedReader().use { it.readText() }
                        val oobArr = org.json.JSONObject(oobJsonText).optJSONArray("oobCodes")
                        var matchedCode: String? = null
                        if (oobArr != null) {
                            for (i in oobArr.length() - 1 downTo 0) {
                                val obj = oobArr.optJSONObject(i) ?: continue
                                if (obj.optString("email").equals(email, ignoreCase = true) &&
                                    obj.optString("requestType") == "VERIFY_EMAIL"
                                ) {
                                    matchedCode = obj.optString("oobCode")
                                    break
                                }
                            }
                        }
                        if (!matchedCode.isNullOrBlank()) {
                            val applyUrl = URL("http://$EMULATOR_HOST:$AUTH_PORT/identitytoolkit.googleapis.com/v1/accounts:update?key=fake-api-key-for-emulator")
                            val applyConn = (applyUrl.openConnection() as HttpURLConnection).apply {
                                requestMethod = "POST"
                                doOutput = true
                                setRequestProperty("Content-Type", "application/json")
                            }
                            applyConn.outputStream.use { os ->
                                os.write("""{"oobCode":"$matchedCode"}""".toByteArray())
                            }
                            applyConn.inputStream.use { it.readBytes() }
                            applyConn.disconnect()
                        }
                        user.reload().await()
                        user.getIdToken(true).await()
                    }
                }
                user.uid
            }
        }

    protected fun Throwable.unwrapFirestoreException(): FirebaseFirestoreException? {
        var current: Throwable? = this
        while (current != null) {
            if (current is FirebaseFirestoreException) return current
            current = current.cause
        }
        return null
    }

    private companion object {
        const val EMULATOR_HOST = "127.0.0.1"
        const val FIRESTORE_PORT = 8085
        const val AUTH_PORT = 9099
        const val PROJECT_ID = "demo-no-project"
        const val DEFAULT_PASSWORD = "password123"
        const val AUTH_TIMEOUT_MS = 8000L
    }
}
