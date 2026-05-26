package by.presassistant.infrastructure.storage;

import by.presassistant.application.port.out.SlideStoragePort;
import by.presassistant.domain.event.LectureStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PresentationLoaderAdapter {

    private final SlideStoragePort slideStorage;

    @Value("${HOST_USERPROFILE:}")
    private String hostUserProfile;

    @Async
    @EventListener
    public void onLectureStarted(LectureStartedEvent event) {
        String fileUrl = event.fileUrl();
        if (fileUrl == null || fileUrl.isBlank()) {
            log.warn("No fileUrl for lecture {}, slide images unavailable", event.lectureId());
            return;
        }
        Path filePath = resolveContainerPath(fileUrl);
        if (filePath == null || !Files.exists(filePath)) {
            log.warn("Presentation file not found at {}, slide images unavailable", filePath);
            return;
        }
        try (InputStream fis = Files.newInputStream(filePath);
             XMLSlideShow pptx = new XMLSlideShow(fis)) {

            Dimension pg = pptx.getPageSize();
            List<XSLFSlide> slides = pptx.getSlides();

            for (int i = 0; i < slides.size(); i++) {
                BufferedImage img = new BufferedImage(pg.width, pg.height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, pg.width, pg.height);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                slides.get(i).draw(g);
                g.dispose();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "png", baos);
                slideStorage.storeSlide(event.lectureId(), i + 1, baos.toByteArray());
            }
            log.info("Loaded {} slides for lecture {}", slides.size(), event.lectureId());
        } catch (Exception e) {
            log.error("Failed to load presentation for lecture {}: {}", event.lectureId(), e.getMessage());
        }
    }

    private Path resolveContainerPath(String fileUrl) {
        try {
            // Strip file:/// or file:// prefix
            String path = fileUrl;
            if (path.startsWith("file:")) {
                path = path.replaceFirst("^file:/{1,3}", "");
            }
            path = URLDecoder.decode(path, StandardCharsets.UTF_8);
            // Normalize backslashes
            path = path.replace('\\', '/');
            // Remove leading slash before drive letter: /C:/... → C:/...
            if (path.matches("^/[A-Za-z]:/.*")) {
                path = path.substring(1);
            }

            // Map Windows user profile path to container mount point
            if (!hostUserProfile.isBlank()) {
                String hostNorm = hostUserProfile.replace('\\', '/');
                if (path.toLowerCase().startsWith(hostNorm.toLowerCase())) {
                    String relative = path.substring(hostNorm.length()).replaceAll("^/+", "");
                    return Path.of("/mnt/userprofile", relative);
                }
            }
            return Path.of(path);
        } catch (Exception e) {
            log.error("Cannot resolve path from fileUrl '{}': {}", fileUrl, e.getMessage());
            return null;
        }
    }
}
