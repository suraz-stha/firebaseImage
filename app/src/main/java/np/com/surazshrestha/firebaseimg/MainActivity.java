package np.com.surazshrestha.firebaseimg;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private StorageReference storageReference;
    private DatabaseReference databaseReference;
    private ImageView imageView;
    private EditText txt_name;
    private Uri imgUrl;

    public static final String FB_STORAGE_PATH = "image/";
    public static final String FB_DATABASE_PATH = "image/";
    public static final int REQUEST_CODE = 1234;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        storageReference = FirebaseStorage.getInstance().getReference();
        databaseReference = FirebaseDatabase.getInstance().getReference(FB_DATABASE_PATH);

        imageView = (ImageView) findViewById(R.id.iv_img);
        txt_name = (EditText) findViewById(R.id.edt_name);
    }

     public void  btnBrowse_Click(View view){
         Intent intent = new Intent();
         intent.setType("image/*");
         intent.setAction(Intent.ACTION_GET_CONTENT);
         startActivityForResult(Intent.createChooser(intent,"select Image"),REQUEST_CODE);
     }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode ==REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null){
            imgUrl = data.getData();
            try{
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),imgUrl);
                imageView.setImageBitmap(bitmap);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getImageExt(Uri uri){
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    @SuppressWarnings("VisibleForTests")
    public  void  btnUpload_Click(View view){
        if(imgUrl!=null){
            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setTitle("Uploading Image");
            dialog.show();
            //get the storage reference
            StorageReference ref = storageReference.child(FB_STORAGE_PATH +System.currentTimeMillis() + "."+getImageExt(imgUrl) );
            // add file to reference
            ref.putFile(imgUrl).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    //Dismiss dialog when sucess
                    dialog.dismiss();
                    //display sucess message on toast
                    Toast.makeText(MainActivity.this, "Image Uploaded", Toast.LENGTH_SHORT).show();
                    ImageUpload imageUpload = new ImageUpload(txt_name.getText().toString().trim(),taskSnapshot.getDownloadUrl().toString());
                    // save image in to firebase database
                    String uploadId = databaseReference.push().getKey();
                    databaseReference.child(uploadId).setValue(imageUpload);



                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //Dismiss dialog when error
                    dialog.dismiss();
                    //display error message on toast
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    //show upload progress
                    double progress = (100 * taskSnapshot.getBytesTransferred()) /taskSnapshot.getTotalByteCount();
                    dialog.setMessage("uploaded" + (int)progress + "%");
                }
            });
        }
        else {
            Toast.makeText(this, "please select image", Toast.LENGTH_SHORT).show();
        }
     }

     public  void  btnload_Click(View view){
         startActivity(new Intent(MainActivity.this,ImageListActivity.class));

     }
}
