package com.example.WaffleBear.file.service;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

@Service
public class VideoThumbnailService {

    private static final Set<String> SUPPORTED_VIDEO_FORMATS = Set.of("mp4", "mov", "m4v");
    private static final int MAX_THUMBNAIL_WIDTH = 640;
    private static final int MAX_THUMBNAIL_HEIGHT = 360;

    public boolean supports(String fileFormat) {
        String normalizedFormat = fileFormat == null ? "" : fileFormat.trim().toLowerCase(Locale.ROOT);
        return SUPPORTED_VIDEO_FORMATS.contains(normalizedFormat);
    }

    public byte[] createThumbnail(Path videoPath) throws IOException, JCodecException {
        try (SeekableByteChannel channel = NIOUtils.readableChannel(videoPath.toFile())) {
            Picture picture = FrameGrab.createFrameGrab(channel).getNativeFrame();
            if (picture == null) {
                return new byte[0];
            }

            BufferedImage sourceImage = AWTUtil.toBufferedImage(picture);
            BufferedImage scaledImage = resize(sourceImage);

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                ImageIO.write(scaledImage, "jpg", outputStream);
                return outputStream.toByteArray();
            }
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
