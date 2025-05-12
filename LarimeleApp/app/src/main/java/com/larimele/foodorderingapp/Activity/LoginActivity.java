package com.larimele.foodorderingapp.Activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.larimele.foodorderingapp.R;
import com.larimele.foodorderingapp.databinding.ActivityLoginBinding;

import java.security.MessageDigest;
import java.util.Arrays;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private static final int RC_SIGN_IN = 123;
    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager mCallbackManager;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.setAutoInitEnabled(true);
        FacebookSdk.fullyInitialize();

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        printKeyHashes();

        // Konfigurasi Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Sign out untuk memastikan tidak ada sesi sebelumnya
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Log.d("GoogleSignIn", "Signed out any previous session");
        });

        initializeFacebookLogin();

        setupClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Periksa apakah pengguna sudah login
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    private void printKeyHashes() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    getPackageName(),
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                String keyHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP);
                Log.e("FB_KEY_HASH", "====================================");
                Log.e("FB_KEY_HASH", "Add this key hash to Facebook Dev Console:");
                Log.e("FB_KEY_HASH", keyHash);
                Log.e("FB_KEY_HASH", "====================================");
                Log.d("GOOGLE_SHA1", "SHA-1 for Google: " + keyHash);
            }
        } catch (Exception e) {
            Log.e("FB_KEY_HASH", "Error generating key hash", e);
            Toast.makeText(this, "Error generating key hash", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeFacebookLogin() {
        mCallbackManager = CallbackManager.Factory.create();

        LoginManager.getInstance().registerCallback(mCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        Log.d("FB_LOGIN", "Success");
                        handleFacebookAccessToken(loginResult.getAccessToken());
                    }

                    @Override
                    public void onCancel() {
                        Log.d("FB_LOGIN", "Canceled");
                        Toast.makeText(LoginActivity.this, "Facebook login canceled", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Log.e("FB_LOGIN", "Error: " + error.getMessage());
                        Toast.makeText(LoginActivity.this, "Facebook login error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setupClickListeners() {
        binding.loginBtn.setOnClickListener(v -> {
            String email = binding.userEdit.getText().toString();
            String password = binding.passEdit.getText().toString();

            if (!email.isEmpty() && !password.isEmpty()) {
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                String uid = mAuth.getCurrentUser().getUid();
                                Log.d("LoginActivity", "Logged in user UID: " + uid);
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(LoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            }
        });

        binding.google.setOnClickListener(v -> signInWithGoogle());

        binding.facebook.setOnClickListener(v -> {
            LoginManager.getInstance().logInWithReadPermissions(
                    LoginActivity.this,
                    Arrays.asList("email", "public_profile")
            );
        });

        binding.textView8.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        mCallbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("GoogleSignIn", "Google Sign-In successful, ID Token: " + account.getIdToken());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.e("GoogleSignIn", "Google Sign-In failed with status code: " + e.getStatusCode() + ", message: " + e.getMessage(), e);
                String errorMessage = "Google Sign-In failed: " + e.getStatusCode();
                switch (e.getStatusCode()) {
                    case 10:
                        errorMessage += " (Developer error: Check SHA-1 or Client ID)";
                        break;
                    case 12500:
                        errorMessage += " (Google Play Services needs update)";
                        break;
                    case 16:
                        errorMessage += " (Sign-In cancelled or network issue)";
                        break;
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        Log.d("GoogleSignIn", "Firebase Auth successful, UID: " + uid);
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Log.e("GoogleSignIn", "Firebase Auth failed", task.getException());
                        Toast.makeText(LoginActivity.this, "Firebase Auth failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        Log.d("FB_AUTH", "Handling Facebook access token");
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        Log.d("LoginActivity", "Logged in user UID (Facebook): " + uid);
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Facebook authentication failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LoginManager.getInstance().unregisterCallback(mCallbackManager);
    }
}