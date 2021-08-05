package com.example.chatmate.profile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.example.chatmate.R;
import com.example.chatmate.common.NodeNames;
import com.example.chatmate.login.LoginActivity;
import com.example.chatmate.password.ChangePasswordActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText etName;

    private ImageView ivProfile;
    private FirebaseUser firebaseUser;
    private DatabaseReference databaseReference;

    private StorageReference fileStorage;
    private Uri localFileUri, serverFileUri;
    private  View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        TextInputEditText etEmail = findViewById(R.id.etEmail);
        etName = findViewById(R.id.etName);
        ivProfile = findViewById(R.id.ivProfile);

        fileStorage = FirebaseStorage.getInstance().getReference();

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        progressBar = findViewById(R.id.progressBar);

        if(firebaseUser!=null)
        {
            etName.setText(firebaseUser.getDisplayName());
            etEmail.setText(firebaseUser.getEmail());
            serverFileUri= firebaseUser.getPhotoUrl();

            if(serverFileUri!=null)
            {
                Glide.with(this)
                        .load(serverFileUri)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(ivProfile);
            }
        }



    }


    public void btnLogoutClick(View view)
    {
        final FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference databaseReference = rootRef.child(NodeNames.TOKENS).child(currentUser.getUid());


        databaseReference.setValue(null).addOnCompleteListener(task -> {
            if(task.isSuccessful())
            {
                firebaseAuth.signOut();
                startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                finish();
            }
            else
            {
                Toast.makeText(ProfileActivity.this, getString(R.string.something_went_wrong, task.getException())
                        , Toast.LENGTH_SHORT).show();
            }
        });



    }




    public void btnSaveClick(View view)
    {
        if(etName.getText().toString().trim().equals(""))
        {
            etName.setError(getString(R.string.enter_name));
        }
        else
        {
            if(localFileUri!=null)
                updateNameAndPhoto();
            else
                updateOnlyName();
        }

    }
    public void changeImage(View view)
    {
        if(serverFileUri==null)
        {
            pickImage();
        }
        else
        {
            PopupMenu popupMenu  = new PopupMenu(this, view);
            popupMenu.getMenuInflater().inflate(R.menu.menu_picture,popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();

                if(id==R.id.mnuChangePicture)
                {
                    pickImage();
                }
                else if (id==R.id.mnuRemovePicture)
                {
                    removePhoto();
                }
                return false;
            });
            popupMenu.show();
        }

    }

    private void pickImage() {

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 101);
        }
        else
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},102);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101) {
            if (resultCode == RESULT_OK) {

                localFileUri = data.getData();
                ivProfile.setImageURI(localFileUri);
            }

        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode==102)
        {
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 101);
            }
            else
            {
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show();
            }

        }
    }

    private  void removePhoto()
    {
        progressBar.setVisibility(View.VISIBLE);
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(etName.getText().toString().trim())
                .setPhotoUri(null)
                .build();

        firebaseUser.updateProfile(request).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                ivProfile.setImageResource(R.drawable.default_profile);
                String userID = firebaseUser.getUid();
                databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);

                HashMap<String, String> hashMap = new HashMap<>();
                hashMap.put(NodeNames.PHOTO, "");

                databaseReference.child(userID).setValue(hashMap)
                        .addOnCompleteListener(task1 -> Toast.makeText(ProfileActivity.this, R.string.photo_removed_successfully, Toast.LENGTH_SHORT).show());


            } else {
                Toast.makeText(ProfileActivity.this,
                        getString(R.string.failed_to_update_profile, task.getException()), Toast.LENGTH_SHORT).show();
            }

        });

    }



    private void updateNameAndPhoto()
    {
        String strFileName= firebaseUser.getUid() + ".jpg";

        final  StorageReference fileRef = fileStorage.child("images/"+ strFileName);
        progressBar.setVisibility(View.VISIBLE);
        fileRef.putFile(localFileUri).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if(task.isSuccessful())
            {
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    serverFileUri = uri;

                    UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                            .setDisplayName(etName.getText().toString().trim())
                            .setPhotoUri(serverFileUri)
                            .build();

                    firebaseUser.updateProfile(request).addOnCompleteListener(task1 -> {
                        if (task1.isSuccessful()) {
                            String userID = firebaseUser.getUid();
                            databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);

                            HashMap<String, String> hashMap = new HashMap<>();

                            hashMap.put(NodeNames.NAME, etName.getText().toString().trim());
                            hashMap.put(NodeNames.PHOTO, serverFileUri.getPath());

                            databaseReference.child(userID).setValue(hashMap)
                                    .addOnCompleteListener(task11 -> finish());


                        } else {
                            Toast.makeText(ProfileActivity.this,
                                    getString(R.string.failed_to_update_profile, task1.getException()), Toast.LENGTH_SHORT).show();
                        }

                    });
                });
            }});

    }

    private void updateOnlyName() {
        progressBar.setVisibility(View.VISIBLE);
        UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                .setDisplayName(etName.getText().toString().trim())
                .build();

        firebaseUser.updateProfile(request).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                String userID = firebaseUser.getUid();
                databaseReference = FirebaseDatabase.getInstance().getReference().child(NodeNames.USERS);

                HashMap<String, String> hashMap = new HashMap<>();

                hashMap.put(NodeNames.NAME, etName.getText().toString().trim());

                databaseReference.child(userID).setValue(hashMap).addOnCompleteListener(task1 -> finish());


            } else {
                Toast.makeText(ProfileActivity.this,
                        getString(R.string.failed_to_update_profile, task.getException()), Toast.LENGTH_SHORT).show();
            }
        });

    }

    public  void btnChangePasswordClick(View view)
    {
        startActivity(new Intent(ProfileActivity.this, ChangePasswordActivity.class));
    }
}