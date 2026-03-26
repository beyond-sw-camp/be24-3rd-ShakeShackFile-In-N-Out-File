package com.example.WaffleBear.file.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class ImageThumbnailService {

    private static final int MAX_THUMBNAIL_WIDTH = 640;
    private static final int MAX_THUMBNAIL_HEIGHT = 360;

    public byte[] createThumbnail(InputStream sourceStream) throws IOException {
        BufferedImage sourceImage = ImageIO.read(sourceStream);
        if (sourceImage == null) {
            return new byte[0];
        }

        BufferedImage scaledImage = resize(sourceImage);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(scaledImage, "jpg", outputStream);
            return outputStream.toByteArray();
        }
    }

    private BufferedImage resize(BufferedImage sourceImage) {
        int sourceWidth = Math.max(1, sourceImage.getWidth());
        int sourceHeight = Math.max(1, sourceImage.getHeight());
        double scale = Math.min(
                1.0,
                Math.min(MAX_THUMBNAIL_WIDTH / (double) sourceWidth, MAX_THUMBNAIL_HEIGHT / (double) sourceHeight)
        );

        int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
        BufferedImage targetImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = targetImage.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, targetWidth, targetHeight);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }

        return targetImage;
    }
}
