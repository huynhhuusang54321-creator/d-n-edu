package vn.vinabacus;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Kiểm tra ranh giới package từ mã nguồn. Chạy tại thư mục chứa pom.xml. */
public final class KiemThuKienTrucMvc {
    private static final Set<String> LAYERS = Set.of("view", "controller", "model", "service", "data", "config", "repository");
    private static final Map<String, Set<String>> FORBIDDEN = Map.of(
            "model", Set.of("view", "controller", "service", "data", "config", "repository"),
            // DAO trong data triển khai hợp đồng repository; service chỉ phụ thuộc hợp đồng đó.
            "data", Set.of("view", "controller", "service"),
            "repository", Set.of("view", "controller", "service"),
            "service", Set.of("view", "controller"),
            "view", Set.of("service", "repository", "data"));
    private static final Pattern PACKAGE = Pattern.compile("\\bpackage\\s+([\\w.]+)\\s*;");
    private static final Pattern PROJECT_REFERENCE = Pattern.compile("\\bvn\\.vinabacus\\.([A-Za-z_$][\\w$]*)");
    private static final Pattern UI_REFERENCE = Pattern.compile("\\b(?:javax\\.swing|java\\.awt)\\.");
    private static final Pattern NON_CODE = Pattern.compile("(?s)/\\*.*?\\*/|//[^\\r\\n]*|\"\"\".*?\"\"\"|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'");

    public static void main(String[] args) throws Exception {
        Path source = Path.of("src/main/java").toAbsolutePath().normalize();
        Path root = source.resolve("vn/vinabacus");
        assert Files.isDirectory(root) : "Chạy kiểm thử tại thư mục chứa pom.xml: " + root;
        Set<String> actualLayers;
        try (var children = Files.list(root)) {
            actualLayers = children.filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString()).collect(Collectors.toCollection(TreeSet::new));
        }
        assert actualLayers.equals(LAYERS) : "Package gốc phải gồm đúng 7 tầng: " + actualLayers;
        List<Path> files;
        try (var walk = Files.walk(root)) {
            files = walk.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
        List<String> errors = new ArrayList<>();
        for (Path file : files) {
            String code = NON_CODE.matcher(Files.readString(file, StandardCharsets.UTF_8)).replaceAll(" ");
            Matcher declaration = PACKAGE.matcher(code);
            String expectedPackage = source.relativize(file.getParent()).toString()
                    .replace(file.getFileSystem().getSeparator(), ".");
            if (!declaration.find() || !expectedPackage.equals(declaration.group(1))) {
                errors.add(file + ": package không khớp đường dẫn " + expectedPackage);
                continue;
            }
            Path relative = root.relativize(file);
            if (relative.getNameCount() == 1) {
                if (!file.getFileName().toString().equals("UngDungVINABACUS.java")) {
                    errors.add(file + ": lớp ngoài 7 tầng chỉ được là điểm khởi động ứng dụng");
                }
            }
            String layer = relative.getNameCount() == 1 ? "" : relative.getName(0).toString();
            Matcher references = PROJECT_REFERENCE.matcher(code);
            while (references.find()) {
                String target = references.group(1);
                if (!LAYERS.contains(target)) {
                    errors.add(file + ": tham chiếu package cũ hoặc không hợp lệ " + references.group());
                } else if (FORBIDDEN.getOrDefault(layer, Set.of()).contains(target)) {
                    errors.add(file + ": tầng " + layer + " không được phụ thuộc " + target);
                }
            }
            if (Set.of("model", "data", "repository", "service").contains(layer) && UI_REFERENCE.matcher(code).find()) {
                errors.add(file + ": tầng " + layer + " không được phụ thuộc Swing/AWT");
            }
        }
        assert errors.isEmpty() : String.join(System.lineSeparator(), errors);
        System.out.println("PASS: " + files.size() + " Java files, 7 root packages, package paths and MVC dependency boundaries.");
    }
}
