package com.example.chatmate.password;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatmate.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText etEmail;
    private TextView tvMessage;
    private LinearLayout llResetPassword, llMessage;
    private Button btnRetry;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        etEmail = findViewById(R.id.etEmail);
        tvMessage = findViewById(R.id.tvMessage);
        llResetPassword = findViewById(R.id.llResetPassword);
        llMessage = findViewById(R.id.llMessage);
        btnRetry = findViewById(R.id.btnRetry);
        progressBar = findViewById(R.id.progressBar);
    }
    public void btnResetPasswordClick(View view)
    {
        String email  = etEmail.getText().toString().trim();
        if(email.equals(""))
        {
            etEmail.setError(getString(R.string.enter_email));
        }
        else
        {
            progressBar.setVisibility(View.VISIBLE);
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                llResetPassword.setVisibility(View.GONE);
                llMessage.setVisibility(View.VISIBLE);

                if(task.isSuccessful())
                {
                    tvMessage.setText(getString(R.string.reset_password_instructions, email));
                    new CountDownTimer(60000,1000)
                    {

                        @Override
                        public void onTick(long l) {
                            btnRetry.setText(getString(R.string.reset_timer, String.valueOf(l/1000)));
                            btnRetry.setOnClickListener(null);
                        }

                        @Override
                        public void onFinish() {
                                btnRetry.setText(R.string.retry);
                                btnRetry.setOnClickListener(v -> {
                                    llResetPassword.setVisibility(View.VISIBLE);
                                    llMessage.setVisibility(View.GONE);
                                });
                        }
                    }.start();
                }
                else
                {
                    tvMessage.setText(getString(R.string.email_sent_failed, task.getException()));
                    btnRetry.setText(R.string.retry);
                    btnRetry.setOnClickListener(v -> {
                        llResetPassword.setVisibility(View.VISIBLE);
                        llMessage.setVisibility(View.GONE);
                    });
                }
            });
        }
    }
    public void btnCloseClick(View view)
    {
        finish();
    }
}