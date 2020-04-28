package com.example.administrator.photo_video;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import cz.msebera.android.httpclient.Header;

public class sendPhoto extends AppCompatActivity {
    public static final  int CHOOSE_PHOTO=2;
    public static final int TAKE_PHOTO=1;
    private TextView path,zhuanghao;
    private ImageView photo;
    private String ImagePath;
    private Button send,opcamera,opalbum,opcancel;
    private Bitmap bitmap;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_photo);
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        if(actionBar != null){
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        photo=findViewById(R.id.images);
        path=findViewById(R.id.path);
        send=findViewById(R.id.send);
        zhuanghao=findViewById(R.id.zhuanghao);
        photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show();
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(path.getText().toString().length()==0)
                    Toast.makeText(sendPhoto.this,"请选择图片",Toast.LENGTH_SHORT).show();
                else if(TextUtils.isEmpty(zhuanghao.getText()))
                    Toast.makeText(sendPhoto.this,"请输入桩号",Toast.LENGTH_SHORT).show();
                else {
                    String url = "http://192.168.43.247:8080/photo_video/android/getphoto.servlet";
                    upload(url);
                }
            }
        });
    }

    private void show() {
        final Dialog bottomDialog = new Dialog(this, R.style.BottomDialog);
        View contentView = LayoutInflater.from(this).inflate(R.layout.bt_dialog, null);
        bottomDialog.setContentView(contentView);
        ViewGroup.LayoutParams layoutParams = contentView.getLayoutParams();
        layoutParams.width = getResources().getDisplayMetrics().widthPixels;
        contentView.setLayoutParams(layoutParams);
        bottomDialog.getWindow().setGravity(Gravity.BOTTOM);
        bottomDialog.getWindow().setWindowAnimations(R.style.BottomDialog_Animation);
        opalbum=contentView.findViewById(R.id.btnalbum);
        opcancel=contentView.findViewById(R.id.btncancel);
        opcamera=contentView.findViewById(R.id.btncamera);
        opalbum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(sendPhoto.this
                        , Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(sendPhoto.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                }else{
                    openALbum();
                }
                bottomDialog.dismiss();
            }
        });
        opcamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File outputImage=new File(getExternalCacheDir(),"output_Image.jpg");
                try{
                    if(outputImage.exists()){
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                }catch (IOException e){
                    e.printStackTrace();
                }
                if(Build.VERSION.SDK_INT>=24){
                    imageUri= FileProvider.getUriForFile(sendPhoto.this,"com.example.administrator.roadcheck.fileprovider",outputImage);
                }else{
                    imageUri=Uri.fromFile(outputImage);
                }
                Intent intent=new Intent("android.media.action.IMAGE_CAPTURE");
                intent.putExtra(MediaStore.EXTRA_OUTPUT,imageUri);
                startActivityForResult(intent,TAKE_PHOTO);
                bottomDialog.dismiss();
            }
        });
        opcancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomDialog.dismiss();
            }
        });

        bottomDialog.show();
    }

    public void openALbum(){
        Intent intent=new Intent("android.intent.action.GET_CONTENT");
        intent.setType("image/*");
        startActivityForResult(intent,CHOOSE_PHOTO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length>0 && grantResults[0] ==PackageManager.PERMISSION_GRANTED){
                    openALbum();
                }else{
                    Toast.makeText(this,"你拒绝了授权", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
        }
    }

    //在选择图片后返回本界面调用此方法
    @Override
    @TargetApi(19)
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == CHOOSE_PHOTO && resultCode == RESULT_OK){
            Uri uri=data.getData();
            if(DocumentsContract.isDocumentUri(this,uri)){
                String docId = DocumentsContract.getDocumentId(uri);
                if("com.android.providers.media.documents".equals(uri.getAuthority())){
                    String id = docId.split(":")[1];
                    String selection = MediaStore.Images.Media._ID+"="+id;
                    ImagePath=getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection);
                }else if("com.android.providers.downloads.documents".equals(uri.getAuthority())){
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.valueOf(docId));
                    ImagePath=getImagePath(contentUri,null);
                }else if("content".equalsIgnoreCase(uri.getScheme())){
                    ImagePath=getImagePath(uri,null);
                }else if("file".equalsIgnoreCase(uri.getScheme())){
                    ImagePath=uri.getPath();
                }
                Log.d("login",ImagePath);
                path.setText(ImagePath);
                displayImage(ImagePath);
            }
        }
        if(requestCode==TAKE_PHOTO && resultCode == RESULT_OK){
            try{
                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                photo.setImageBitmap(bitmap);
                path.setText(imageUri.toString());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private String getImagePath(Uri uri,String selection){
        String path=null;
        Cursor cursor=getContentResolver().query(uri,null,selection,null,null);
        if(cursor !=null){
            if(cursor.moveToFirst()){
                path=cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }
    private  void displayImage(String imagePath){
        if(imagePath !=null){
            bitmap= BitmapFactory.decodeFile(imagePath);
            photo.setImageBitmap(bitmap);
        }else{
            Toast.makeText(this,"获取图片错误",Toast.LENGTH_SHORT).show();
        }
    }
    public void upload(String url) {
        // 将bitmap转为string，并使用BASE64加密
        String img = Utils.BitmapToString(bitmap);
        // 获取到图片的名字
        // String name = path.toString().substring(path.toString().lastIndexOf("/")).substring(1);
        String name=zhuanghao.getText().toString()+".jpg";
        // new一个请求参数
        RequestParams params = new RequestParams();
        // 将图片和名字添加到参数中
        params.put("photo", img);
        params.put("name", name);
        AsyncHttpClient client = new AsyncHttpClient();
        // 调用AsyncHttpClient的post方法
        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                Toast.makeText(sendPhoto.this," 上传成功",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                Toast.makeText(sendPhoto.this," 上传失败，请重试",Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish(); // back button
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}


