package com.moxmose.moxequiplog.ui.components

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.calendar.CalendarScopes
import com.moxmose.moxequiplog.R

@Composable
fun GoogleAccountSelector(
    accountName: String?,
    onAccountSelected: (String?) -> Unit
) {
    val context = LocalContext.current
    
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(CalendarScopes.CALENDAR_EVENTS))
            .build()
    }
        
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d("GoogleAccountSelector", "Launcher returned with resultCode: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("GoogleAccountSelector", "Sign in successful: ${account?.email}")
                onAccountSelected(account?.email)
            } catch (e: ApiException) {
                Log.e("GoogleAccountSelector", "Sign in failed with status code: ${e.statusCode} (Status: ${GoogleSignInStatusCodes.getStatusCodeString(e.statusCode)})", e)
                Toast.makeText(context, "Sign in failed (code: ${e.statusCode}: ${GoogleSignInStatusCodes.getStatusCodeString(e.statusCode)}). Check SHA-1 configuration.", Toast.LENGTH_LONG).show()
                onAccountSelected(null)
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            Log.d("GoogleAccountSelector", "Sign in cancelled by user")
            onAccountSelected(null)
        } else {
            Log.e("GoogleAccountSelector", "Sign in failed with result code: ${result.resultCode}")
            Toast.makeText(context, "Sign in failed with result code: ${result.resultCode}", Toast.LENGTH_SHORT).show()
            onAccountSelected(null)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (accountName != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.options_google_account_label),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = accountName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = {
                        googleSignInClient.signOut().addOnCompleteListener {
                            onAccountSelected(null)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = stringResource(R.string.options_google_disconnect),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { launcher.launch(googleSignInClient.signInIntent) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.options_google_connect))
                }
                
                Text(
                    text = stringResource(R.string.google_auth_privacy_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
