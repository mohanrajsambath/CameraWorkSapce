package mohan.com.hovertouchfocus.customviews;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.util.Size;

public class PreferenceHelper {

    /**
     *Initialize all configuration to sharedPreference
     * @param context
     * @param name
     * @param previewSizes
     * @param formats The number stored in the array is the number
     * @param pictureSizes
     */
    public static void writePreferenceForCameraId(Context context, String name, Size[] previewSizes, Integer[] formats, Size[][] pictureSizes) {
        SharedPreferences preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("previewSizes", previewSizes.length);
        for (int i = 0; i < previewSizes.length; i++) {
            editor.putInt("previewSizes" + i + "_width", previewSizes[i].getWidth());
            editor.putInt("previewSizes" + i + "_height", previewSizes[i].getHeight());
        }
        //There are several formats
        editor.putInt("formats", formats.length);
        for (int i = 0; i < formats.length; i++) {
            //Here to judge, if it is nullSize, write 0 into it
            editor.putInt("formats" + i, formats[i]);
            if (pictureSizes[i] == null) {
                editor.putInt("formats" + i + "pictureSizes", 0);
                continue;
            }

            editor.putInt("formats" + formats[i] + "pictureSizes", pictureSizes[i].length);
            for (int j = 0; j < pictureSizes[i].length; j++) {
                editor.putInt("formats" + formats[i] + "pictureSizes" + j + "_width", pictureSizes[i][j].getWidth());
                editor.putInt("formats" + formats[i] + "pictureSizes" + j + "_height", pictureSizes[i][j].getHeight());
            }
        }
        editor.apply();
        writeCurrentPreferenceForCameraId(context, name, previewSizes, formats, pictureSizes);
    }

    /**
     * Initialization of the actual configuration used
     * @param context
     * @param name
     * @param previewSizes
     * @param formats
     * @param pictureSizes
     */
    private static void writeCurrentPreferenceForCameraId(Context context, String name, Size[] previewSizes, Integer[] formats, Size[][] pictureSizes) {
        String currentName = "current" + name;
        SharedPreferences currentPreferences = context.getSharedPreferences(currentName, Context.MODE_PRIVATE);
        SharedPreferences preferences = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = currentPreferences.edit();
        editor.putInt("previewSize_width", previewSizes[0].getWidth());
        editor.putInt("previewSize_height", previewSizes[0].getHeight());
        //This is equivalent to grouping dng and jpeg respectively.
        int formatsNum = preferences.getInt("formats", 0);
//        Log.i("writeCurrentPreferenceForCameraId", "formatsNum--->" + formatsNum);
        for (int i = 0; i < formatsNum; i++) {
            editor.putInt("format_" + formats[i] + "_pictureSize_width", pictureSizes[i][0].getWidth());
            editor.putInt("format_" + formats[i] + "_pictureSize_height", pictureSizes[i][0].getHeight());
//            Log.i("writeCurrentPreferenceForCameraId", "format_" + formats[i] + "_pictureSize_width--->" + pictureSizes[i][0].getWidth() + ",,,format_" + formats[i] + "_pictureSize_height--->" + pictureSizes[i][0].getHeight());
        }
        //Current format
        editor.putInt("format", formats[0]);
        editor.apply();
    }

    /**
     * Initialize the storage camera
     * @param context
     */
    public static void writeCurrentCameraid(Context context, String cameraId) {
        SharedPreferences currentPreferences = context.getSharedPreferences("current", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = currentPreferences.edit();
        editor.putString("cameraid", cameraId);
        editor.apply();
    }

    /**
     * Which camera to get
     * @param context
     * @return
     */
    public static String getCurrentCameraid(Context context) {
        SharedPreferences currentPreferences = context.getSharedPreferences("current", Context.MODE_PRIVATE);
        return currentPreferences.getString("cameraid", "0");
    }

    /**
     * Determine if the camera parameters have been initialized
     * @param context
     * @return
     */
    public static boolean checkFirstInit(Context context) {
        SharedPreferences preferences = context.getSharedPreferences("current", Context.MODE_PRIVATE);
        int num = preferences.getInt("first_time", 0);
        if (num == 1) {
            return true;
        } else {
//            SharedPreferences.Editor editor = preferences.edit();
//            editor.putInt("first_time", 1);
//            editor.commit();
            return false;
        }
    }

    /**
     * Get the image format in the cameraid file,
     * @param context
     * @param cameraId
     * @return
     */
    public static String[] getFormatsName(Context context, String cameraId) {
        SharedPreferences preferences = context.getSharedPreferences(cameraId, Context.MODE_PRIVATE);
        int formatsNum = preferences.getInt("formats", 0);
        String[] formats = new String[formatsNum];
        for (int i = 0; i < formatsNum; i++) {
            int format = preferences.getInt("formats" + i, 0);
            if (null != getFormatName(format)) {
                formats[i] = getFormatName(format);
            } else {
                formats[i] = "null";
            }
        }
        return formats;
    }

    /**
     * Get all the serial numbers of the image format
     * @param context
     * @param cameraId
     * @return
     */
    public static int[] getFormatsNumber(Context context, String cameraId) {
        SharedPreferences preferences = context.getSharedPreferences(cameraId, Context.MODE_PRIVATE);
        int formatsNum = preferences.getInt("formats", 0);
        int[] formats = new int[formatsNum];
        for (int i = 0; i < formatsNum; i++) {
            int format = preferences.getInt("formats" + i, 0);
            if (null != getFormatName(format)) {
                formats[i] = format;
            } else {
                formats[i] = -1;
            }
        }
        return formats;
    }

    /**
     * Convert the serial number to a name
     * @param format
     * @return
     */
    private static String getFormatName(int format) {
        String name = null;
        switch (format) {
            case ImageFormat.JPEG:
                name = "JPEG";
                break;
            case ImageFormat.NV16:
                name = "NV16";
                break;
            case ImageFormat.NV21:
                name = "NV21";
                break;
            case ImageFormat.RAW10:
                name = "RAW10";
                break;
            case ImageFormat.RAW_SENSOR:
                name = "RAW_SENSOR";
                break;
            case ImageFormat.RGB_565:
                name = "RGB_565";
                break;
            case ImageFormat.YUV_420_888:
                name = "YUV_420_888";
                break;
            case ImageFormat.YUY2:
                name = "YUY2";
                break;
            case ImageFormat.YV12:
                name = "YV12";
                break;
            case ImageFormat.UNKNOWN:
                name = null;
                break;
        }
        return name;
    }

    /**
     * Get all the format sizes of this format
     * @param context
     * @param cameraId
     * @return
     */
    public static String[] getFormatSize(Context context, String cameraId) {
        SharedPreferences sp = context.getSharedPreferences(cameraId, Context.MODE_PRIVATE);
        SharedPreferences currentSp = context.getSharedPreferences("current" + cameraId, Context.MODE_PRIVATE);
        //Get the current format
        int currentFormat = currentSp.getInt("format", 256);
        //Get the current number of formats
        int sizeNum = sp.getInt("formats" + currentFormat + "pictureSizes", 0);
        String[] str = new String[sizeNum];
        //Obtain
        for (int i = 0; i < sizeNum; i++) {
            str[i] = sp.getInt("formats" + currentFormat + "pictureSizes" + i + "_width", 0) + "*" + sp.getInt("formats" + currentFormat + "pictureSizes" + i + "_height", 0);
        }
        return str;
    }

    public static String[] getPreviewSize(Context context, String cameraId) {
        SharedPreferences sp = context.getSharedPreferences(cameraId, Context.MODE_PRIVATE);
        //Get the current format
        int previewNum = sp.getInt("previewSizes", 0);
        String[] str = new String[previewNum];
        for (int i = 0; i < previewNum; i++) {
            str[i] = sp.getInt("previewSizes" + i + "_width", 0) + "*" + sp.getInt("previewSizes" + i + "_height", 0);
        }
        return str;
    }

}
