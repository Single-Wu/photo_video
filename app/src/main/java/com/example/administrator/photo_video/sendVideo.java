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
import android.widget.VideoView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.ExecutorService;

import cz.msebera.android.httpclient.Header;

public class sendVideo extends AppCompatActivity {
    public static final  int CHOOSE_PHOTO=2;
    public static final int TAKE_PHOTO=1;
    private TextView path,zhuanghao;
    private VideoView photo;
    private String VideoPath;
    private Button send,opcamera,opalbum,opcancel;
    private Uri imageUri;
    private String host = "10.216.85.171";
    private int port = 8888;
    private static Socket socket;
    private ExecutorService mExecutorService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_video);
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
                    Toast.makeText(sendVideo.this,"请选择图片",Toast.LENGTH_SHORT).show();
                else if(TextUtils.isEmpty(zhuanghao.getText()))
                    Toast.makeText(sendVideo.this,"请输入桩号",Toast.LENGTH_SHORT).show();
                else {
                   // String url = "http://192.168.43.247:8080/photo_video/android/getphoto.servlet";
                    new mySocket().start();

                }
            }
        });
    }

    //底部弹框
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
                if (ContextCompat.checkSelfPermission(sendVideo.this
                        , Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(sendVideo.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
                }else{
                    openALbum();
                }
                bottomDialog.dismiss();
            }
        });
        opcamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File outputImage=new File(getExternalCacheDir(),"output_video.mp4");
                try{
                    if(outputImage.exists()){
                        outputImage.delete();
                    }
                    outputImage.createNewFile();
                }catch (IOException e){
                    e.printStackTrace();
                }
                if(Build.VERSION.SDK_INT>=24){
                    imageUri= FileProvider.getUriForFile(sendVideo.this,"com.example.administrator.roadcheck.fileprovider",outputImage);
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

    //打开相册
    public void openALbum(){
        Intent intent=new Intent("android.intent.action.GET_CONTENT");
        intent.setType("video/*");
        startActivityForResult(intent,CHOOSE_PHOTO);
    }

    //是否允许授权
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

    //在选择图片后返回本界面调用此方法得到路径显示视频
    @Override
    @TargetApi(19)
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == CHOOSE_PHOTO && resultCode == RESULT_OK){
            Uri uri=data.getData();
            if(DocumentsContract.isDocumentUri(this,uri)){
                String docId = DocumentsContract.getDocumentId(uri);
                if("com.android.providers.media.documents".equals(uri.getAuthority())){
                    String id = docId.split(":")[1];
                    String selection = MediaStore.Video.Media._ID+"="+id;
                    VideoPath=getVideoPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,selection);
                }else if("com.android.providers.downloads.documents".equals(uri.getAuthority())){
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.valueOf(docId));
                    VideoPath=getVideoPath(contentUri,null);
                }else if("content".equalsIgnoreCase(uri.getScheme())){
                    VideoPath=getVideoPath(uri,null);
                }else if("file".equalsIgnoreCase(uri.getScheme())){
                    VideoPath=uri.getPath();
                }
                Log.d("login",VideoPath);
                path.setText(VideoPath);
                 displayImage(VideoPath);
            }
        }
//        if(requestCode==TAKE_PHOTO && resultCode == RESULT_OK){
//            try{
//                bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
//             //   photo.setImageBitmap(bitmap);
//                path.setText(imageUri.toString());
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//        }
    }

    private String getVideoPath(Uri uri,String selection){
        String path=null;
        Cursor cursor=getContentResolver().query(uri,null,selection,null,null);
        if(cursor !=null){
            if(cursor.moveToFirst()){
                path=cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }
    private  void displayImage(String VideoPath){
        if(VideoPath !=null){
            photo.setVideoPath(VideoPath);
            photo.start();
        }else{
            Toast.makeText(this,"获取视频错误",Toast.LENGTH_SHORT).show();
        }
    }

    class mySocket extends Thread{
        public void run() {
            try {
                socket = new Socket(host, port);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (true) {
                try {
                    File file = new File(VideoPath);
                    System.out.println("文件大小：" + file.length() + "kb");
                    DataInputStream dis = new DataInputStream(new FileInputStream(VideoPath));
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    byte[] buf = new byte[1024 * 9];
                    int len = 0;
                    while ((len = dis.read(buf)) != -1) {
                        dos.write(buf, 0, len);

                    }
                    dos.flush();
                    System.out.println("文件上传结束，，，，");
                    dis.close();
                    dos.close();
                    break;
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }
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


