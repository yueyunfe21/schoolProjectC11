import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class BuildXiuluoObjectiveTemplates {
    private static final Path SAMPLE_DIR = Path.of("images", "calibrate", "mapName_coordinate");
    private static final Path MAP_NAME_DIR = Path.of("images", "template", "objective", "map_names");
    private static final Path DIGIT_DIR = Path.of("images", "template", "objective", "green_digits");
    private static final Path DEBUG_DIR = Path.of("images", "temp", "xiuluo_objective_templates");
    private static final Path MAP_NAME_MANIFEST = MAP_NAME_DIR.resolve("manifest.tsv");
    private static final Pattern SAMPLE_NAME =
            Pattern.compile("^(story|quest)_(.+)_(\\d{1,4})_(\\d{1,4})\\.png$");
    private static final Pattern COORDINATE_TEXT = Pattern.compile("\\d{1,3},\\d{1,3}");
    private static final double DIGIT_MATCH_THRESHOLD = 0.45;
    private static final double SLIDING_DIGIT_THRESHOLD = 0.82;
    private static final Map<String, String> MAP_NAME_BY_SLUG = createMapNameManifest();

    public static void main(String[] args) throws Exception {
        Files.createDirectories(MAP_NAME_DIR);
        Files.createDirectories(DIGIT_DIR);
        Files.createDirectories(DEBUG_DIR);
        clearPngFiles(DIGIT_DIR);

        List<Sample> samples = listSamples();
        if (samples.isEmpty()) {
            System.out.println("[warn] no samples found: " + SAMPLE_DIR.toAbsolutePath());
            return;
        }

        Map<String, Path> mapTemplates = new HashMap<>();
        Map<String, Path> digitTemplates = new HashMap<>();
        for (Sample sample : samples) {
            buildFromSample(sample, mapTemplates, digitTemplates);
        }
        writeMapNameManifest(mapTemplates);

        int ok = 0;
        for (Sample sample : samples) {
            if (verifySample(sample)) {
                ok++;
            }
        }

        System.out.printf("[summary] samples=%d verified=%d mapTemplates=%d digitTemplates=%d%n",
                samples.size(), ok, mapTemplates.size(), digitTemplates.size());
    }

    private static List<Sample> listSamples() throws IOException {
        try (Stream<Path> stream = Files.list(SAMPLE_DIR)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> {
                        Matcher matcher = SAMPLE_NAME.matcher(path.getFileName().toString());
                        if (!matcher.matches()) {
                            return null;
                        }
                        return new Sample(
                                path,
                                matcher.group(1),
                                matcher.group(2),
                                matcher.group(3),
                                matcher.group(4)
                        );
                    })
                    .filter(sample -> sample != null)
                    .sorted(Comparator.comparing(sample -> sample.path.getFileName().toString()))
                    .toList();
        }
    }

    private static void buildFromSample(Sample sample, Map<String, Path> mapTemplates, Map<String, Path> digitTemplates)
            throws IOException {
        BufferedImage raw = ImageIO.read(sample.path.toFile());
        if (raw == null) {
            System.out.println("[skip] unreadable " + sample.path);
            return;
        }

        BufferedImage clean = washGreen(raw);
        Path cleanPath = DEBUG_DIR.resolve(stripExtension(sample.path.getFileName().toString()) + "_clean.png");
        ImageIO.write(clean, "png", cleanPath.toFile());

        List<GlyphBox> coordinateRuns = findCoordinateRunsByExpectedProjection(clean, sample.expectedCoordinateText().length() + 2);
        if (coordinateRuns.isEmpty()) {
            System.out.printf("[warn] no coordinate runs sample=%s%n", sample.path.getFileName());
            raw.flush();
            clean.flush();
            return;
        }

        Optional<GlyphBox> mapNameBox = findMapNameBoxBefore(clean, coordinateRuns.get(0).minX);
        if (mapNameBox.isPresent()) {
            Path out = MAP_NAME_DIR.resolve(sample.slug + ".png");
            BufferedImage mapTemplate = trimToForeground(crop(clean, mapNameBox.get()), 1);
            ImageIO.write(mapTemplate, "png", out.toFile());
            System.out.printf("[ok] map template %s <- %s%n", out, sample.path.getFileName());
            mapTemplates.put(sample.slug, out);
            mapTemplate.flush();
        } else {
            System.out.printf("[warn] no map-name box sample=%s coordStart=%d%n",
                    sample.path.getFileName(), coordinateRuns.get(0).minX);
        }

        List<LabeledGlyph> symbols = extractCoordinateGlyphsFromExpectedRuns(coordinateRuns, sample.expectedCoordinateText());
        if (symbols.size() != sample.expectedCoordinateText().length()) {
            System.out.printf("[warn] coordinate slice mismatch sample=%s expected=%s sliced=%d%n",
                    sample.path.getFileName(), sample.expectedCoordinateText(), symbols.size());
        } else {
            for (LabeledGlyph symbol : symbols) {
                String name = symbolName(symbol.symbol);
                String variantName = name + "_" + stripExtension(sample.path.getFileName().toString())
                        + "_" + symbol.index + ".png";
                Path out = DIGIT_DIR.resolve(variantName);
                BufferedImage glyph = trimToForeground(crop(clean, symbol.box), 1);
                ImageIO.write(glyph, "png", out.toFile());
                System.out.printf("[ok] digit template %s <- %s%n", out, sample.path.getFileName());
                digitTemplates.put(name, out);
                glyph.flush();
            }
        }

        raw.flush();
        clean.flush();
    }

    private static boolean verifySample(Sample sample) {
        try {
            BufferedImage raw = ImageIO.read(sample.path.toFile());
            if (raw == null) {
                return false;
            }
            BufferedImage clean = washGreen(raw);
            String recognizedCoordinate = recognizeCoordinate(clean);

            Path mapTemplate = MAP_NAME_DIR.resolve(sample.slug + ".png");
            double mapScore = Files.exists(mapTemplate) ? bestBinaryTemplateScore(clean, ImageIO.read(mapTemplate.toFile())) : 0.0;
            String mapName = MAP_NAME_BY_SLUG.get(sample.slug);
            boolean ok = sample.expectedCoordinateText().equals(recognizedCoordinate) && mapScore >= 0.82;
            System.out.printf("[verify] %s map=%s text=%s score=%.3f coord=%s expected=%s ok=%s%n",
                    sample.path.getFileName(), sample.slug, mapName, mapScore, recognizedCoordinate,
                    sample.expectedCoordinateText(), ok);

            raw.flush();
            clean.flush();
            return ok;
        } catch (Exception e) {
            System.out.printf("[verify] %s failed: %s%n", sample.path.getFileName(), e.getMessage());
            return false;
        }
    }

    private static BufferedImage washGreen(BufferedImage raw) {
        BufferedImage clean = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < raw.getHeight(); y++) {
            for (int x = 0; x < raw.getWidth(); x++) {
                clean.setRGB(x, y, isObjectiveGreen(raw.getRGB(x, y)) ? 0xFFFFFF : 0x000000);
            }
        }
        return clean;
    }

    private static Map<String, String> createMapNameManifest() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("baigu_shan", "白骨山");
        map.put("changan", "长安");
        map.put("changan_chengdong", "长安城东");
        map.put("datang_bianjing", "大唐边境");
        map.put("dayanta3", "大雁塔三层");
        map.put("dayanta4", "大雁塔四层");
        map.put("dayanta5", "大雁塔五层");
        map.put("fengchao5", "凤巢五层");
        map.put("fengchao6", "凤巢六层");
        map.put("fengchao7", "凤巢七层");
        map.put("lanruo_si", "兰若寺");
        map.put("longku5", "龙窟五层");
        map.put("longku6", "龙窟六层");
        map.put("longku7", "龙窟七层");
        map.put("luoyang_cheng", "洛阳城");
        map.put("pantao_yuan", "蟠桃园");
        map.put("sisheng_zhuang", "四圣庄");
        map.put("wanshou_shan", "万寿山");
        map.put("yaochi", "瑶池");
        return map;
    }

    private static void writeMapNameManifest(Map<String, Path> generatedTemplates) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# slug\tmapName\ttemplate");
        for (Map.Entry<String, String> entry : MAP_NAME_BY_SLUG.entrySet()) {
            Path template = generatedTemplates.getOrDefault(entry.getKey(), MAP_NAME_DIR.resolve(entry.getKey() + ".png"));
            if (!Files.exists(template)) {
                System.out.printf("[warn] map manifest template missing: slug=%s template=%s%n", entry.getKey(), template);
                continue;
            }
            lines.add(entry.getKey() + "\t" + entry.getValue() + "\t" + MAP_NAME_DIR.relativize(template));
        }
        Files.write(MAP_NAME_MANIFEST, lines);
        System.out.printf("[ok] map manifest %s entries=%d%n", MAP_NAME_MANIFEST, lines.size() - 1);
    }

    private static boolean isObjectiveGreen(int rgb) {
        Color c = new Color(rgb);
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        return g >= 95 && (g - r) >= 35 && (g - b) >= 35;
    }

    private static List<GlyphBox> segmentGlyphs(BufferedImage clean) {
        boolean[][] visited = new boolean[clean.getHeight()][clean.getWidth()];
        List<GlyphBox> glyphs = new ArrayList<>();
        for (int y = 0; y < clean.getHeight(); y++) {
            for (int x = 0; x < clean.getWidth(); x++) {
                if (visited[y][x] || !isWhite(clean, x, y)) {
                    continue;
                }
                GlyphBox box = floodFill(clean, visited, x, y);
                if (isUsefulGlyph(box)) {
                    glyphs.add(box.expand(clean.getWidth(), clean.getHeight(), 1));
                }
            }
        }
        glyphs.sort(Comparator.comparingInt(box -> box.minX));
        return glyphs;
    }

    private static GlyphBox floodFill(BufferedImage image, boolean[][] visited, int startX, int startY) {
        List<int[]> stack = new ArrayList<>();
        stack.add(new int[]{startX, startY});
        visited[startY][startX] = true;

        GlyphBox box = new GlyphBox(startX, startY, startX, startY);
        for (int i = 0; i < stack.size(); i++) {
            int[] point = stack.get(i);
            int x = point[0];
            int y = point[1];
            box.include(x, y);
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) != 1) {
                        continue;
                    }
                    int nx = x + dx;
                    int ny = y + dy;
                    if (nx < 0 || ny < 0 || nx >= image.getWidth() || ny >= image.getHeight()) {
                        continue;
                    }
                    if (!visited[ny][nx] && isWhite(image, nx, ny)) {
                        visited[ny][nx] = true;
                        stack.add(new int[]{nx, ny});
                    }
                }
            }
        }
        return box;
    }

    private static boolean isUsefulGlyph(GlyphBox box) {
        return box.width() >= 1 && box.height() >= 2 && box.pixelCount >= 2 && box.width() <= 22 && box.height() <= 20;
    }

    private static Optional<BracketSpan> findBracketSpan(List<GlyphBox> glyphs) {
        List<GlyphBox> candidates = glyphs.stream()
                .filter(g -> g.width() <= 8 && g.height() >= 8 && g.height() <= 18 && g.pixelCount >= 6)
                .sorted(Comparator.comparingInt(g -> g.minX))
                .toList();
        BracketSpan best = null;
        int bestWidth = 0;
        for (int i = 0; i < candidates.size(); i++) {
            for (int j = i + 1; j < candidates.size(); j++) {
                GlyphBox left = candidates.get(i);
                GlyphBox right = candidates.get(j);
                int width = right.maxX - left.minX + 1;
                if (width < 28 || width > 85) {
                    continue;
                }
                boolean hasComma = glyphs.stream().anyMatch(g ->
                        g.minX > left.maxX
                                && g.maxX < right.minX
                                && g.minY >= left.minY + 5
                                && g.width() <= 5
                                && g.height() <= 6);
                if (hasComma && width > bestWidth) {
                    best = new BracketSpan(left.minX, right.maxX,
                            Math.min(left.minY, right.minY), Math.max(left.maxY, right.maxY));
                    bestWidth = width;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private static Optional<GlyphBox> findMapNameBoxBefore(BufferedImage clean, int boundaryX) {
        GlyphBox box = trimBoxToForeground(clean, 0, Math.max(0, boundaryX - 1), 0, clean.getHeight() - 1);
        if (box.pixelCount <= 0 || box.width() <= 1 || box.height() <= 1) {
            return Optional.empty();
        }
        return Optional.of(box);
    }

    private static List<GlyphBox> findCoordinateRunsByProjection(BufferedImage clean) {
        List<GlyphBox> runs = projectionRuns(clean, 0, clean.getWidth() - 1, 0, clean.getHeight() - 1);
        runs = runs.stream()
                .filter(run -> run.width() >= 1 && run.height() >= 3 && run.pixelCount >= 2)
                .sorted(Comparator.comparingInt(run -> run.minX))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        int commaIndex = -1;
        for (int i = 0; i < runs.size(); i++) {
            if (isCommaRun(runs.get(i))) {
                commaIndex = i;
                break;
            }
        }
        if (commaIndex < 0) {
            return List.of();
        }

        int start = -1;
        for (int i = commaIndex - 1; i >= 0; i--) {
            if (isCoordinateStartRun(runs.get(i))) {
                start = i;
                break;
            }
        }
        if (start < 0) {
            start = Math.max(0, commaIndex - 3);
        }

        return new ArrayList<>(runs.subList(start, runs.size()));
    }

    private static List<GlyphBox> findCoordinateRunsByExpectedProjection(BufferedImage clean, int expectedRunCount) {
        List<GlyphBox> runs = projectionRuns(clean, 0, clean.getWidth() - 1, 0, clean.getHeight() - 1);
        runs = runs.stream()
                .filter(run -> run.width() >= 1 && run.height() >= 3 && run.pixelCount >= 2)
                .sorted(Comparator.comparingInt(run -> run.minX))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        if (runs.size() < expectedRunCount) {
            return List.of();
        }
        return new ArrayList<>(runs.subList(runs.size() - expectedRunCount, runs.size()));
    }

    private static List<LabeledGlyph> extractCoordinateGlyphsFromRuns(List<GlyphBox> coordinateRuns, String expectedText) {
        if (coordinateRuns.size() < expectedText.length() + 2) {
            return List.of();
        }
        List<GlyphBox> symbolRuns = stripCoordinateDecorations(coordinateRuns);
        if (symbolRuns.size() != expectedText.length()) {
            return List.of();
        }
        List<LabeledGlyph> result = new ArrayList<>();
        for (int i = 0; i < expectedText.length(); i++) {
            result.add(new LabeledGlyph(expectedText.charAt(i), i, symbolRuns.get(i)));
        }
        return result;
    }

    private static List<LabeledGlyph> extractCoordinateGlyphsFromExpectedRuns(List<GlyphBox> coordinateRuns, String expectedText) {
        if (coordinateRuns.size() != expectedText.length() + 2) {
            return extractCoordinateGlyphsFromRuns(coordinateRuns, expectedText);
        }
        List<GlyphBox> symbolRuns = coordinateRuns.subList(1, coordinateRuns.size() - 1);
        List<LabeledGlyph> result = new ArrayList<>();
        for (int i = 0; i < expectedText.length(); i++) {
            result.add(new LabeledGlyph(expectedText.charAt(i), i, symbolRuns.get(i)));
        }
        return result;
    }

    private static List<LabeledGlyph> extractCoordinateGlyphs(BufferedImage clean, List<GlyphBox> glyphs,
                                                              BracketSpan span, String expectedText) {
        int commaIndex = expectedText.indexOf(',');
        String leftText = expectedText.substring(0, commaIndex);
        String rightText = expectedText.substring(commaIndex + 1);
        Optional<GlyphBox> comma = findCommaGlyph(glyphs, span, leftText.length(), rightText.length());
        if (comma.isEmpty()) {
            return List.of();
        }
        List<GlyphBox> leftDigits = projectionDigitBoxes(clean,
                span.minX + 4, comma.get().minX - 1, span.minY - 1, span.maxY + 1, leftText.length());
        List<GlyphBox> rightDigits = projectionDigitBoxes(clean,
                comma.get().maxX + 1, span.maxX - 4, span.minY - 1, span.maxY + 1, rightText.length());
        if (leftDigits.size() != leftText.length() || rightDigits.size() != rightText.length()) {
            return List.of();
        }
        List<LabeledGlyph> result = new ArrayList<>();
        int index = 0;
        for (int i = 0; i < leftText.length(); i++) {
            result.add(new LabeledGlyph(leftText.charAt(i), index++, leftDigits.get(i)));
        }
        result.add(new LabeledGlyph(',', index++, comma.get()));
        for (int i = 0; i < rightText.length(); i++) {
            result.add(new LabeledGlyph(rightText.charAt(i), index++, rightDigits.get(i)));
        }
        return result;
    }

    private static Optional<GlyphBox> findCommaGlyph(List<GlyphBox> glyphs, BracketSpan span, int leftDigits, int rightDigits) {
        double expectedRatio = (leftDigits + 1.0) / (leftDigits + rightDigits + 3.0);
        int expectedX = (int) Math.round(span.minX + (span.maxX - span.minX) * expectedRatio);
        return glyphs.stream()
                .filter(g -> g.minX > span.minX + 4 && g.maxX < span.maxX - 4)
                .filter(g -> g.minY >= span.minY + 5 && g.width() <= 5 && g.height() <= 6)
                .min(Comparator.comparingInt(g -> Math.abs(g.minX - expectedX)));
    }

    private static List<GlyphBox> projectionDigitBoxes(BufferedImage clean, int startX, int endX, int startY, int endY,
                                                       int expectedCount) {
        int left = Math.max(0, Math.min(startX, endX));
        int right = Math.min(clean.getWidth() - 1, Math.max(startX, endX));
        int top = Math.max(0, Math.min(startY, endY));
        int bottom = Math.min(clean.getHeight() - 1, Math.max(startY, endY));
        List<GlyphBox> runs = projectionRuns(clean, left, right, top, bottom);
        runs = runs.stream()
                .filter(box -> box.width() >= 1 && box.height() >= 5)
                .sorted(Comparator.comparingInt(box -> box.minX))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        while (runs.size() > expectedCount) {
            int bestIndex = -1;
            int bestGap = Integer.MAX_VALUE;
            for (int i = 0; i < runs.size() - 1; i++) {
                int gap = runs.get(i + 1).minX - runs.get(i).maxX;
                if (gap < bestGap) {
                    bestGap = gap;
                    bestIndex = i;
                }
            }
            if (bestIndex < 0) {
                break;
            }
            GlyphBox merged = runs.get(bestIndex).copy();
            merged.include(runs.get(bestIndex + 1));
            runs.set(bestIndex, merged);
            runs.remove(bestIndex + 1);
        }
        return runs;
    }

    private static List<GlyphBox> projectionRuns(BufferedImage clean, int left, int right, int top, int bottom) {
        List<GlyphBox> runs = new ArrayList<>();
        int runStart = -1;
        for (int x = left; x <= right; x++) {
            boolean hasWhite = false;
            for (int y = top; y <= bottom; y++) {
                if (isWhite(clean, x, y)) {
                    hasWhite = true;
                    break;
                }
            }
            if (hasWhite && runStart < 0) {
                runStart = x;
            } else if (!hasWhite && runStart >= 0) {
                runs.add(trimBoxToForeground(clean, runStart, x - 1, top, bottom));
                runStart = -1;
            }
        }
        if (runStart >= 0) {
            runs.add(trimBoxToForeground(clean, runStart, right, top, bottom));
        }
        return runs;
    }

    private static String recognizeCoordinate(BufferedImage clean) throws IOException {
        List<GlyphBox> coordinateRuns = findCoordinateRunsByProjection(clean);
        if (coordinateRuns.size() < 3) {
            return recognizeCoordinateByTemplateScan(clean);
        }
        String stripped = recognizeCoordinateRuns(clean, stripCoordinateDecorations(coordinateRuns));
        if (COORDINATE_TEXT.matcher(stripped).matches()
                && !stripped.matches("\\d{3},\\d")) {
            return stripped;
        }
        Optional<String> repaired = repairDuplicatedPrefixAndMergedSuffix(stripped);
        if (repaired.isPresent()) {
            return repaired.get();
        }

        String direct = recognizeCoordinateRuns(clean, coordinateRuns);
        if (COORDINATE_TEXT.matcher(direct).matches()) {
            return direct;
        }
        return recognizeCoordinateByTemplateScan(clean);
    }

    private static Optional<String> repairDuplicatedPrefixAndMergedSuffix(String text) {
        Matcher matcher = Pattern.compile("^(\\d)\\1(\\d),(\\d)$").matcher(text);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1) + matcher.group(2) + "," + matcher.group(3) + matcher.group(3));
    }

    private static String recognizeCoordinateRuns(BufferedImage clean, List<GlyphBox> runs) throws IOException {
        if (runs.size() < 3) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < runs.size(); i++) {
            GlyphBox run = runs.get(i);
            boolean last = i == runs.size() - 1;
            if (out.isEmpty() && isPureBracketRun(run)) {
                continue;
            }
            if (last && out.indexOf(",") >= 0 && isPureBracketRun(run)) {
                continue;
            }
            if (isCommaRun(run)) {
                out.append(',');
                continue;
            }
            BufferedImage glyph = trimToForeground(crop(clean, run), 1);
            String symbol = recognizeOneGlyph(glyph);
            if (symbol == null) {
                symbol = recognizeContainedDigit(glyph);
            }
            glyph.flush();
            if (symbol == null) {
                return recognizeCoordinateByTemplateScan(clean);
            }
            out.append(symbol);
        }
        return out.toString();
    }

    private static String recognizeCoordinateByTemplateScan(BufferedImage clean) throws IOException {
        List<SymbolMatch> matches = new ArrayList<>();
        for (String symbol : List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "comma")) {
            for (Path templatePath : listDigitTemplates(symbol)) {
                BufferedImage template = ImageIO.read(templatePath.toFile());
                if (template == null || template.getWidth() > clean.getWidth() || template.getHeight() > clean.getHeight()) {
                    continue;
                }
                for (int y = 0; y <= clean.getHeight() - template.getHeight(); y++) {
                    for (int x = 0; x <= clean.getWidth() - template.getWidth(); x++) {
                        double score = binaryTemplateScoreAt(clean, template, x, y);
                        if (score >= SLIDING_DIGIT_THRESHOLD) {
                            matches.add(new SymbolMatch(
                                    "comma".equals(symbol) ? "," : symbol,
                                    x + template.getWidth() / 2.0,
                                    y + template.getHeight() / 2.0,
                                    score
                            ));
                        }
                    }
                }
                template.flush();
            }
        }
        if (matches.isEmpty()) {
            return "";
        }

        matches.sort(Comparator.comparingDouble((SymbolMatch match) -> match.score).reversed());
        List<SymbolMatch> accepted = new ArrayList<>();
        for (SymbolMatch candidate : matches) {
            boolean duplicate = false;
            for (SymbolMatch existing : accepted) {
                if (Math.abs(existing.centerX - candidate.centerX) <= 3
                        && Math.abs(existing.centerY - candidate.centerY) <= 3) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                accepted.add(candidate);
            }
        }
        accepted.sort(Comparator.comparingDouble(match -> match.centerX));

        StringBuilder text = new StringBuilder();
        for (SymbolMatch match : accepted) {
            text.append(match.symbol);
        }
        Matcher matcher = COORDINATE_TEXT.matcher(text);
        String best = "";
        while (matcher.find()) {
            best = matcher.group();
        }
        return best;
    }

    private static List<GlyphBox> stripCoordinateDecorations(List<GlyphBox> coordinateRuns) {
        if (coordinateRuns.isEmpty()) {
            return List.of();
        }
        List<GlyphBox> out = new ArrayList<>();
        boolean skippedLeadingBracket = false;
        for (int i = 0; i < coordinateRuns.size(); i++) {
            GlyphBox run = coordinateRuns.get(i);
            boolean last = i == coordinateRuns.size() - 1;
            if (!skippedLeadingBracket && out.isEmpty() && isBracketLikeRun(run)) {
                skippedLeadingBracket = true;
                continue;
            }
            if (last && !out.isEmpty() && out.stream().anyMatch(BuildXiuluoObjectiveTemplates::isCommaRun)
                    && isBracketLikeRun(run)) {
                continue;
            }
            out.add(run);
        }
        return out;
    }

    private static String recognizeDigits(BufferedImage clean, int startX, int endX, int startY, int endY) throws IOException {
        List<GlyphBox> boxes = projectionDigitBoxes(clean, startX, endX, startY, endY, 99);
        StringBuilder out = new StringBuilder();
        for (GlyphBox box : boxes) {
            BufferedImage glyph = trimToForeground(crop(clean, box), 1);
            String symbol = recognizeOneGlyph(glyph);
            glyph.flush();
            if (symbol == null || ",".equals(symbol)) {
                return "";
            }
            out.append(symbol);
        }
        return out.toString();
    }

    private static String recognizeOneGlyph(BufferedImage glyph) throws IOException {
        double bestScore = 0.0;
        String bestSymbol = null;
        for (String symbol : List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")) {
            for (Path templatePath : listDigitTemplates(symbol)) {
                BufferedImage template = ImageIO.read(templatePath.toFile());
                double score = foregroundSimilarity(glyph, template);
                template.flush();
                if (score > bestScore) {
                    bestScore = score;
                    bestSymbol = symbol;
                }
            }
        }
        if (bestScore < DIGIT_MATCH_THRESHOLD) {
            return null;
        }
        return bestSymbol;
    }

    private static String recognizeContainedDigit(BufferedImage glyph) throws IOException {
        double bestScore = 0.0;
        String bestSymbol = null;
        for (String symbol : List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")) {
            for (Path templatePath : listDigitTemplates(symbol)) {
                BufferedImage template = ImageIO.read(templatePath.toFile());
                if (template == null || template.getWidth() > glyph.getWidth()
                        || template.getHeight() > glyph.getHeight()) {
                    continue;
                }
                for (int y = 0; y <= glyph.getHeight() - template.getHeight(); y++) {
                    for (int x = 0; x <= glyph.getWidth() - template.getWidth(); x++) {
                        double score = binaryTemplateScoreAt(glyph, template, x, y);
                        if (score > bestScore) {
                            bestScore = score;
                            bestSymbol = symbol;
                        }
                    }
                }
                template.flush();
            }
        }
        return bestScore >= 0.78 ? bestSymbol : null;
    }

    private static boolean isCommaRun(GlyphBox box) {
        return box.width() <= 5 && box.height() <= 6 && box.minY >= 8 && box.pixelCount <= 12;
    }

    private static boolean isBracketLikeRun(GlyphBox box) {
        return box.width() <= 6 && box.height() >= 8 && box.height() <= 18 && box.pixelCount <= 22;
    }

    private static boolean isCoordinateStartRun(GlyphBox box) {
        return box.width() <= 6 && box.height() >= 8 && box.height() <= 18 && box.pixelCount <= 22;
    }

    private static boolean isPureBracketRun(GlyphBox box) {
        return box.width() <= 4 && box.height() >= 8 && box.height() <= 18 && box.pixelCount <= 12;
    }

    private static double bestBinaryTemplateScore(BufferedImage source, BufferedImage template) {
        if (source == null || template == null
                || template.getWidth() > source.getWidth()
                || template.getHeight() > source.getHeight()) {
            return 0.0;
        }
        double best = 0.0;
        for (int y = 0; y <= source.getHeight() - template.getHeight(); y++) {
            for (int x = 0; x <= source.getWidth() - template.getWidth(); x++) {
                best = Math.max(best, binaryTemplateScoreAt(source, template, x, y));
            }
        }
        template.flush();
        return best;
    }

    private static double binaryTemplateScoreAt(BufferedImage source, BufferedImage template, int originX, int originY) {
        int templateWhite = 0;
        int sourceWhite = 0;
        int overlap = 0;
        for (int y = 0; y < template.getHeight(); y++) {
            for (int x = 0; x < template.getWidth(); x++) {
                boolean tw = isWhite(template, x, y);
                boolean sw = isWhite(source, originX + x, originY + y);
                if (tw) {
                    templateWhite++;
                }
                if (sw) {
                    sourceWhite++;
                }
                if (tw && sw) {
                    overlap++;
                }
            }
        }
        if (templateWhite == 0 || sourceWhite == 0) {
            return 0.0;
        }
        return (2.0 * overlap) / (templateWhite + sourceWhite);
    }

    private static double foregroundSimilarity(BufferedImage a, BufferedImage b) {
        int whiteA = countWhite(a);
        int whiteB = countWhite(b);
        if (whiteA == 0 || whiteB == 0) {
            return 0.0;
        }
        double best = 0.0;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int overlap = 0;
                for (int y = 0; y < a.getHeight(); y++) {
                    for (int x = 0; x < a.getWidth(); x++) {
                        int bx = x + dx;
                        int by = y + dy;
                        if (bx < 0 || by < 0 || bx >= b.getWidth() || by >= b.getHeight()) {
                            continue;
                        }
                        if (isWhite(a, x, y) && isWhite(b, bx, by)) {
                            overlap++;
                        }
                    }
                }
                best = Math.max(best, (2.0 * overlap) / (whiteA + whiteB));
            }
        }
        double sizePenalty = Math.abs(a.getWidth() - b.getWidth()) * 0.08
                + Math.abs(a.getHeight() - b.getHeight()) * 0.04;
        return Math.max(0.0, best - sizePenalty);
    }

    private static int countWhite(BufferedImage image) {
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (isWhite(image, x, y)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static List<Path> listDigitTemplates(String symbol) throws IOException {
        String prefix = symbol + "_";
        try (Stream<Path> stream = Files.list(DIGIT_DIR)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .filter(path -> path.getFileName().toString().endsWith(".png"))
                    .sorted()
                    .toList();
        }
    }

    private static void clearPngFiles(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            for (Path path : stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".png"))
                    .toList()) {
                Files.delete(path);
            }
        }
    }

    private static GlyphBox trimBoxToForeground(BufferedImage clean, int startX, int endX, int startY, int endY) {
        int minX = clean.getWidth();
        int minY = clean.getHeight();
        int maxX = -1;
        int maxY = -1;
        int count = 0;
        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                if (isWhite(clean, x, y)) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    count++;
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return new GlyphBox(startX, startY, endX, endY);
        }
        GlyphBox box = new GlyphBox(minX, minY, maxX, maxY);
        box.pixelCount = count;
        return box.expand(clean.getWidth(), clean.getHeight(), 1);
    }

    private static BufferedImage crop(BufferedImage source, GlyphBox box) {
        int minX = Math.max(0, box.minX);
        int minY = Math.max(0, box.minY);
        int maxX = Math.min(source.getWidth() - 1, box.maxX);
        int maxY = Math.min(source.getHeight() - 1, box.maxY);
        BufferedImage out = new BufferedImage(maxX - minX + 1, maxY - minY + 1, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = out.createGraphics();
        try {
            g.drawImage(source, 0, 0, out.getWidth(), out.getHeight(), minX, minY, maxX + 1, maxY + 1, null);
            return out;
        } finally {
            g.dispose();
        }
    }

    private static BufferedImage trimToForeground(BufferedImage source, int pad) {
        int minX = source.getWidth();
        int minY = source.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                if (isWhite(source, x, y)) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return source;
        }
        BufferedImage out = crop(source, new GlyphBox(
                Math.max(0, minX - pad),
                Math.max(0, minY - pad),
                Math.min(source.getWidth() - 1, maxX + pad),
                Math.min(source.getHeight() - 1, maxY + pad)
        ));
        source.flush();
        return out;
    }

    private static boolean isWhite(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) & 0x00FFFFFF) == 0x00FFFFFF;
    }

    private static int centerY(GlyphBox box) {
        return (box.minY + box.maxY) / 2;
    }

    private static int centerY(BracketSpan span) {
        return (span.minY + span.maxY) / 2;
    }

    private static String symbolName(char c) {
        return c == ',' ? "comma" : String.valueOf(c);
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }

    private record Sample(Path path, String source, String slug, String x, String y) {
        private String expectedCoordinateText() {
            return x + "," + y;
        }
    }

    private static class GlyphBox {
        private int minX;
        private int minY;
        private int maxX;
        private int maxY;
        private int pixelCount;

        private GlyphBox(int minX, int minY, int maxX, int maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        private int width() {
            return maxX - minX + 1;
        }

        private int height() {
            return maxY - minY + 1;
        }

        private void include(int x, int y) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            pixelCount++;
        }

        private void include(GlyphBox other) {
            minX = Math.min(minX, other.minX);
            minY = Math.min(minY, other.minY);
            maxX = Math.max(maxX, other.maxX);
            maxY = Math.max(maxY, other.maxY);
            pixelCount += other.pixelCount;
        }

        private GlyphBox expand(int imageWidth, int imageHeight, int pad) {
            GlyphBox expanded = new GlyphBox(
                    Math.max(0, minX - pad),
                    Math.max(0, minY - pad),
                    Math.min(imageWidth - 1, maxX + pad),
                    Math.min(imageHeight - 1, maxY + pad)
            );
            expanded.pixelCount = pixelCount;
            return expanded;
        }

        private GlyphBox copy() {
            GlyphBox copy = new GlyphBox(minX, minY, maxX, maxY);
            copy.pixelCount = pixelCount;
            return copy;
        }

        @Override
        public String toString() {
            return "GlyphBox[x=" + minX + ",y=" + minY + ",w=" + width() + ",h=" + height() + "]";
        }
    }

    private record BracketSpan(int minX, int maxX, int minY, int maxY) {
    }

    private record LabeledGlyph(char symbol, int index, GlyphBox box) {
    }

    private record SymbolMatch(String symbol, double centerX, double centerY, double score) {
    }
}
