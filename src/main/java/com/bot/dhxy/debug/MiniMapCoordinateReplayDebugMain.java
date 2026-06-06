package com.bot.dhxy.debug;

import com.bot.dhxy.model.MapCoordinate;
import com.bot.dhxy.model.navigation.MapLabelTemplateMatch;
import com.bot.dhxy.model.navigation.MiniMapSnapshot;
import com.bot.dhxy.tools.CoordinateHelper;
import com.bot.dhxy.vision.MiniMapCoordinateReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Replays saved mini-map coordinate-strip images without touching a live game window.
 *
 * <p>This is the safety net for changes inside {@link MiniMapCoordinateReader}: every local digit
 * parsing change should run this main against {@code images/test-cases/minimap/raw} and
 * {@code images/test-cases/minimap/failure-location}. It validates coordinate parsing and rejects
 * previously observed out-of-transform-bounds coordinate failures.</p>
 */
public class MiniMapCoordinateReplayDebugMain {

    private static final Path ROOT = Path.of("images", "test-cases", "minimap");
    private static final Path RAW_DIR = ROOT.resolve("raw");
    private static final Path FAILURE_DIR = ROOT.resolve("failure-location");
    private static final int PLAUSIBILITY_MARGIN_PX = 40;
    private static final int EXPECTED_AROUND_TOLERANCE = 8;

    public static void main(String[] args) throws Exception {
        MiniMapCoordinateReader reader = new MiniMapCoordinateReader(null, null, null);
        CoordinateHelper coordinateHelper = new CoordinateHelper(null, null);
        coordinateHelper.loadMapConfig();

        List<ReplayResult> results = new ArrayList<>();
        results.addAll(replayRawCases(reader, coordinateHelper));
        results.addAll(replayFailureCases(reader, coordinateHelper));

        long failed = results.stream().filter(result -> !result.passed()).count();
        results.stream()
                .filter(result -> !result.passed())
                .forEach(result -> System.out.println("FAIL " + result.message()));

        System.out.printf("MINIMAP_COORD_REPLAY total=%d passed=%d failed=%d%n",
                results.size(), results.size() - failed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static List<ReplayResult> replayRawCases(MiniMapCoordinateReader reader,
                                                     CoordinateHelper coordinateHelper) throws IOException {
        if (!Files.isDirectory(RAW_DIR)) {
            return List.of();
        }
        try (var stream = Files.list(RAW_DIR)) {
            return stream
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(path -> replayRawCase(reader, coordinateHelper, path))
                    .toList();
        }
    }

    private static ReplayResult replayRawCase(MiniMapCoordinateReader reader,
                                              CoordinateHelper coordinateHelper,
                                              Path imagePath) {
        try {
            BufferedImage raw = ImageIO.read(imagePath.toFile());
            if (raw == null) {
                return ReplayResult.fail(imagePath, "image-read-null");
            }
            try {
                MiniMapSnapshot snapshot = reader.readLocationSnapshotFromCoordinateStrip(raw, false, false);
                if (snapshot.coordinate() == null) {
                    return ReplayResult.fail(imagePath, "coordinate-null");
                }

                Optional<MapLabelTemplateMatch> label = reader.recognizeMapLabelFromCoordinateStrip(raw);
                if (label.isPresent()) {
                    MapCoordinate coordinate = snapshot.coordinate();
                    boolean plausible = coordinateHelper.isLogicalCoordinatePlausible(
                            label.get().mapName(), coordinate.getX(), coordinate.getY(), PLAUSIBILITY_MARGIN_PX);
                    if (!plausible) {
                        return ReplayResult.fail(imagePath, "raw-implausible map=" + label.get().mapName()
                                + " coord=" + coordinate.getX() + "," + coordinate.getY()
                                + " labelScore=" + String.format("%.3f", label.get().score()));
                    }
                }
                return ReplayResult.pass(imagePath, "raw-ok");
            } finally {
                raw.flush();
            }
        } catch (Exception e) {
            return ReplayResult.fail(imagePath, "exception=" + e.getMessage());
        }
    }

    private static List<ReplayResult> replayFailureCases(MiniMapCoordinateReader reader,
                                                         CoordinateHelper coordinateHelper) throws IOException {
        if (!Files.isDirectory(FAILURE_DIR)) {
            return List.of();
        }
        try (var stream = Files.list(FAILURE_DIR)) {
            return stream
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .map(path -> replayFailureCase(reader, coordinateHelper, path))
                    .toList();
        }
    }

    private static ReplayResult replayFailureCase(MiniMapCoordinateReader reader,
                                                  CoordinateHelper coordinateHelper,
                                                  Path caseDir) {
        Path imagePath = caseDir.resolve("tmp_pos.png");
        Path metadataPath = caseDir.resolve("metadata.txt");
        try {
            if (!Files.isRegularFile(imagePath)) {
                return ReplayResult.fail(caseDir, "tmp_pos-missing");
            }
            Metadata metadata = readMetadata(metadataPath);
            BufferedImage raw = ImageIO.read(imagePath.toFile());
            if (raw == null) {
                return ReplayResult.fail(caseDir, "image-read-null");
            }
            try {
                MiniMapSnapshot snapshot = reader.readLocationSnapshotFromCoordinateStrip(raw, false, false);
                MapCoordinate coordinate = snapshot.coordinate();
                if (coordinate == null) {
                    return ReplayResult.fail(caseDir, "coordinate-null map=" + metadata.mapName());
                }
                if (metadata.expectedAround() != null
                        && distance(coordinate, metadata.expectedAround()) > EXPECTED_AROUND_TOLERANCE) {
                    return ReplayResult.fail(caseDir, "expected-miss map=" + metadata.mapName()
                            + " expectedAround=" + format(metadata.expectedAround())
                            + " actual=" + format(coordinate));
                }
                if (metadata.mapName() != null && !metadata.mapName().isBlank()) {
                    boolean plausible = coordinateHelper.isLogicalCoordinatePlausible(
                            metadata.mapName(), coordinate.getX(), coordinate.getY(), PLAUSIBILITY_MARGIN_PX);
                    if (!plausible) {
                        return ReplayResult.fail(caseDir, "implausible map=" + metadata.mapName()
                                + " actual=" + format(coordinate));
                    }
                }
                return ReplayResult.pass(caseDir, "failure-case-ok");
            } finally {
                raw.flush();
            }
        } catch (Exception e) {
            return ReplayResult.fail(caseDir, "exception=" + e.getMessage());
        }
    }

    private static Metadata readMetadata(Path metadataPath) throws IOException {
        if (!Files.isRegularFile(metadataPath)) {
            return new Metadata(null, null);
        }
        String mapName = null;
        MapCoordinate expectedAround = null;
        for (String line : Files.readAllLines(metadataPath, StandardCharsets.UTF_8)) {
            if (line.startsWith("map=")) {
                mapName = line.substring("map=".length()).trim();
            } else if (line.startsWith("expectedAround=")) {
                expectedAround = parseCoordinate(line.substring("expectedAround=".length()).trim());
            }
        }
        return new Metadata(mapName, expectedAround);
    }

    private static MapCoordinate parseCoordinate(String value) {
        String[] parts = value.split(",");
        if (parts.length != 2) {
            return null;
        }
        return new MapCoordinate(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
    }

    private static int distance(MapCoordinate a, MapCoordinate b) {
        return Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY()));
    }

    private static String format(MapCoordinate coordinate) {
        return coordinate == null ? "null" : coordinate.getX() + "," + coordinate.getY();
    }

    private record Metadata(String mapName, MapCoordinate expectedAround) {
    }

    private record ReplayResult(Path path, boolean passed, String reason) {
        private static ReplayResult pass(Path path, String reason) {
            return new ReplayResult(path, true, reason);
        }

        private static ReplayResult fail(Path path, String reason) {
            return new ReplayResult(path, false, reason);
        }

        private String message() {
            return path + " reason=" + reason;
        }
    }
}
