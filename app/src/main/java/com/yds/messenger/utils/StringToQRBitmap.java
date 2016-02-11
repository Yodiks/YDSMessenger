package com.yds.messenger.utils;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.EnumMap;

/**
 * Created by yds on 30/11/15.
 */

/**
 * Class to create a bitmap of QR code needed for pairing.
 */
public class StringToQRBitmap {
    /**
     * Create a bitmap pf QR code representing the given text
     * @param text - The text to convert to QR code
     * @return
     */
    public static Bitmap sting2QRBitmap(String text) {
        BarcodeFormat barcodeFormat = BarcodeFormat.QR_CODE;

        int width0 = 500;
        int height0 = 500;

        int colorBack = 0xFF000000;
        int colorFront = 0xFFFFFFFF;

        QRCodeWriter writer = new QRCodeWriter();
        try {
            EnumMap<EncodeHintType, Object> hint = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
            hint.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hint.put(EncodeHintType.MARGIN, 1);
            BitMatrix bitMatrix = writer.encode(text, barcodeFormat, width0, height0, hint);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {

                    pixels[offset + x] = bitMatrix.get(x, y) ? colorBack : colorFront;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (WriterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
}
/**
 * Created by yds on 30/11/15.
 */