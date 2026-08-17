package com.zaduzenja.app.data.backup

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "GoogleDriveBackup"
private const val BACKUP_FILE_NAME = "zaduzenja_backup.json"
private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
private const val PREFS_NAME = "zaduzenja_backup_prefs"
private const val KEY_FIRST_LAUNCH_DONE = "first_launch_done"
private const val KEY_LAST_SYNC_TIME = "last_sync_time"

private data class DriveBackupFile(val id: String)

class GoogleDriveBackup(private val context: Context) {

    private val _signInState = MutableStateFlow<SignInState>(SignInState.SignedOut)
    val signInState: StateFlow<SignInState> = _signInState

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime

    private var currentAccount: GoogleSignInAccount? = null
    private val syncMutex = Mutex()

    private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val googleSignInClient: GoogleSignInClient by lazy {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DRIVE_SCOPE))
            .build()
        GoogleSignIn.getClient(context, signInOptions)
    }

    init {
        val savedTime = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        if (savedTime > 0) {
            _lastSyncTime.value = savedTime
        }

        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && GoogleSignIn.hasPermissions(account, Scope(DRIVE_SCOPE))) {
            currentAccount = account
            _signInState.value = SignInState.SignedIn(account.email ?: "Unknown")
        }
    }

    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    fun handleSignInResult(account: GoogleSignInAccount?) {
        if (account != null) {
            currentAccount = account
            _signInState.value = SignInState.SignedIn(account.email ?: "Unknown")
        } else {
            currentAccount = null
            _signInState.value = SignInState.SignedOut
        }
    }

    suspend fun signOut() {
        withContext(Dispatchers.IO) {
            try {
                googleSignInClient.signOut().await()
                currentAccount = null
                _signInState.value = SignInState.SignedOut
            } catch (e: Exception) {
                Log.e(TAG, "Sign out failed", e)
            }
        }
    }

    private suspend fun getAccessToken(): String? {
        val account = currentAccount ?: return null
        val androidAccount = account.account ?: return null
        return withContext(Dispatchers.IO) {
            try {
                val tokenResult = com.google.android.gms.auth.GoogleAuthUtil.getToken(
                    context,
                    androidAccount,
                    "oauth2:$DRIVE_SCOPE"
                )
                tokenResult
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get access token", e)
                null
            }
        }
    }

    suspend fun backup(jsonData: String): Boolean = syncMutex.withLock {
        val token = getAccessToken() ?: return@withLock false

        withContext(Dispatchers.IO) {
            try {
                _syncState.value = SyncState.Syncing

                // Check if file exists
                val existingFileId = findBackupFile(token)

                if (existingFileId != null) {
                    // Update existing file
                    updateFile(token, existingFileId, jsonData)
                } else {
                    // Create new file
                    createFile(token, jsonData)
                }

                val now = System.currentTimeMillis()
                _lastSyncTime.value = now
                prefs.edit().putLong(KEY_LAST_SYNC_TIME, now).apply()
                _syncState.value = SyncState.Success
                true
            } catch (e: Exception) {
                Log.e(TAG, "Backup failed", e)
                _syncState.value = SyncState.Error(e.message ?: "Backup failed")
                false
            }
        }
    }

    suspend fun restore(): String? = syncMutex.withLock {
        val token = getAccessToken() ?: return@withLock null

        withContext(Dispatchers.IO) {
            try {
                _syncState.value = SyncState.Syncing

                val fileId = findBackupFile(token)
                if (fileId == null) {
                    _syncState.value = SyncState.Idle
                    return@withContext null
                }

                val data = downloadFile(token, fileId)
                _syncState.value = SyncState.Success
                data
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed", e)
                _syncState.value = SyncState.Error(e.message ?: "Restore failed")
                null
            }
        }
    }

    private fun findBackupFile(token: String): String? {
        val files = findBackupFiles(token)
        files.drop(1).forEach { duplicate ->
            try {
                deleteFile(token, duplicate.id)
                Log.d(TAG, "Obrisan stari duplikat backup fajla: ${duplicate.id}")
            } catch (e: Exception) {
                Log.w(TAG, "Neuspešno brisanje starog backup fajla: ${duplicate.id}", e)
            }
        }

        return files.firstOrNull()?.id
    }

    private fun findBackupFiles(token: String): List<DriveBackupFile> {
        val url = "https://www.googleapis.com/drive/v3/files" +
            "?spaces=appDataFolder" +
            "&q=name%3D%27$BACKUP_FILE_NAME%27%20and%20trashed%3Dfalse" +
            "&orderBy=modifiedTime%20desc" +
            "&fields=files(id,name,modifiedTime)"
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.e(TAG, "Find file failed: ${response.code}")
            return emptyList()
        }

        val body = response.body?.string() ?: return emptyList()
        val json = JSONObject(body)
        val files = json.optJSONArray("files") ?: return emptyList()

        val backupFiles = mutableListOf<DriveBackupFile>()
        for (i in 0 until files.length()) {
            val id = files.optJSONObject(i)?.optString("id").orEmpty()
            if (id.isNotBlank()) {
                backupFiles.add(DriveBackupFile(id))
            }
        }
        return backupFiles
    }

    private fun deleteFile(token: String, fileId: String) {
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId")
            .addHeader("Authorization", "Bearer $token")
            .delete()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful && response.code != 404) {
            throw Exception("Delete file failed: ${response.code}")
        }
    }

    private fun createFile(token: String, content: String) {
        val metadata = JSONObject().apply {
            put("name", BACKUP_FILE_NAME)
            put("parents", org.json.JSONArray().put("appDataFolder"))
        }

        val metadataPart = metadata.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val contentPart = content
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("metadata", null, metadataPart)
            .addFormDataPart("file", BACKUP_FILE_NAME, contentPart)
            .build()

        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
            .addHeader("Authorization", "Bearer $token")
            .post(body)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Create file failed: ${response.code}")
        }
    }

    private fun updateFile(token: String, fileId: String, content: String) {
        val body = content.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media")
            .addHeader("Authorization", "Bearer $token")
            .patch(body)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Update file failed: ${response.code}")
        }
    }

    private fun downloadFile(token: String, fileId: String): String? {
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Download file failed: ${response.code}")
        }

        return response.body?.string()
    }

    fun isSignedIn(): Boolean = _signInState.value is SignInState.SignedIn

    fun isFirstLaunch(): Boolean {
        return !prefs.getBoolean(KEY_FIRST_LAUNCH_DONE, false)
    }

    fun markFirstLaunchDone() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH_DONE, true).apply()
    }
}

sealed class SignInState {
    data object SignedOut : SignInState()
    data class SignedIn(val email: String) : SignInState()
}

sealed class SyncState {
    data object Idle : SyncState()
    data object Syncing : SyncState()
    data object Success : SyncState()
    data class Error(val message: String) : SyncState()
}
