package justin.com.ajcompress;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.justin.library.AJCompress;
import com.justin.library.OnCompressListener;

import java.io.File;
import java.util.ArrayList;

import me.iwf.photopicker.PhotoPicker;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private TextView m_tvFileSize;
    private TextView m_tvImageSize;
    private TextView m_tvThumbFileSize;
    private TextView m_tvThumbImageSize;
    private ImageView m_ivImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        m_tvFileSize = (TextView) findViewById(R.id.file_size);
        m_tvImageSize = (TextView) findViewById(R.id.image_size);
        m_tvThumbFileSize = (TextView) findViewById(R.id.thumb_file_size);
        m_tvThumbImageSize = (TextView) findViewById(R.id.thumb_image_size);
        m_ivImage = (ImageView) findViewById(R.id.image);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PhotoPicker.builder()
                        .setPhotoCount(1)
                        .setShowCamera(true)
                        .setShowGif(true)
                        .setPreviewEnabled(false)
                        .start(MainActivity.this, PhotoPicker.REQUEST_CODE);

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == PhotoPicker.REQUEST_CODE) {
            if (data != null) {
                ArrayList<String> photos = data.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS);

                File imgFile = new File(photos.get(0));
                m_tvFileSize.setText(imgFile.length() / 1024 + "k");
                int[] size = AJCompress.create(this).getImageSize(imgFile.getPath());
                m_tvImageSize.setText(size[0] + " * " + size[1]);

                compress(new File(photos.get(0)));
            }
        }
    }

    private void compress(File file) {
        AJCompress.create(this)
                .loadFile(file)
                .setLevel(AJCompress.THIRD_LEVEL)
                .setOnCompressListener(new OnCompressListener() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onSuccess(File file) {
                        Glide.with(MainActivity.this).load(file).into(m_ivImage);

                        m_tvThumbFileSize.setText(file.length() / 1024 + "k");
                        int[] size = AJCompress.create(MainActivity.this).getImageSize(file.getPath());
                        m_tvThumbImageSize.setText(size[0] + " * " + size[1]);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                }).run();
    }

    private void compressWithRx(File file) {
        AJCompress.create(this)
                .loadFile(file)
                .setLevel(AJCompress.FIRST_LEVEL)
                .asObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                })
                .onErrorResumeNext(new Func1<Throwable, Observable<? extends File>>() {
                    @Override
                    public Observable<? extends File> call(Throwable throwable) {
                        return Observable.empty();
                    }
                })
                .subscribe(new Action1<File>() {
                    @Override
                    public void call(File file) {
                        Glide.with(MainActivity.this).load(file).into(m_ivImage);

                        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        Uri uri = Uri.fromFile(file);
                        intent.setData(uri);
                        MainActivity.this.sendBroadcast(intent);

                        m_tvThumbFileSize.setText(file.length() / 1024 + "k");
                        int[] size = AJCompress.create(MainActivity.this).getImageSize(file.getPath());
                        m_tvThumbImageSize.setText(size[0] + " * " + size[1]);
                    }
                });
    }
}
