package mohan.com.hovertouchfocus.listeners;

import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Random;

public class JpegReaderListener implements ImageReader.OnImageAvailableListener {
    @Override
    public void onImageAvailable(ImageReader reader) {
        new Thread(new ImageSaver(reader)).start();
    }

    class ImageSaver implements Runnable {
        private ImageReader mImageReader;

        ImageSaver(ImageReader mImageReader) {
            this.mImageReader = mImageReader;
        }

        @Override
        public void run() {
            Image image = mImageReader.acquireLatestImage();
            checkParentDir();
            File file;
            checkJpegDir();
            file = createJpeg();
            try {
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                try {
                    save(bytes, file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                image.close();
            } catch (Exception e) {
                e.getStackTrace();
            }
        }
    }


    /**
     * Determine if the parent file exists
     */
    private void checkParentDir() {
        File dir = new File(Environment.getExternalStorageDirectory() + "/Android_L_Test/");
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    /**
     * Determine if the folder exists
     */
    private void checkJpegDir() {
        File dir = new File(Environment.getExternalStorageDirectory() + "/Android_L_Test/jpeg/");
        if (!dir.exists()) {
            dir.mkdir();
        }
    }


    /**
     * Create a jpeg file
     *
     * @return
     */
    private File createJpeg() {
        long time = System.currentTimeMillis();
        int random = new Random().nextInt(1000);
        File dir = new File(Environment.getExternalStorageDirectory() + "/Android_L_Test/jpeg/");
        Log.i("JpegSaver", time + "_" + random + ".jpg");
        return new File(dir, time + "_" + random + ".jpg");
    }


    /**
     * Save
     *
     * @param bytes
     * @param file
     * @throws IOException
     */
    private void save(byte[] bytes, File file) throws IOException {
        Log.i("JpegSaver", "save");
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            os.write(bytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }
}
