import java.io.*;
import java.util.*;

public class TreeToJsonConverter {
    
    static class TreeNode {
        String name;
        boolean isFile;
        List<TreeNode> children = new ArrayList<>();
        
        TreeNode(String name, boolean isFile) {
            this.name = name;
            this.isFile = isFile;
        }
        
        String toJson(int indentLevel) {
            StringBuilder sb = new StringBuilder();
            String indent = "  ".repeat(indentLevel);
            String childIndent = "  ".repeat(indentLevel + 1);
            
            sb.append(indent).append("{\n");
            sb.append(childIndent).append("\"name\": \"").append(escapeJson(name)).append("\",\n");
            sb.append(childIndent).append("\"type\": \"").append(isFile ? "file" : "directory").append("\"");
            
            if (!children.isEmpty()) {
                sb.append(",\n");
                sb.append(childIndent).append("\"children\": [\n");
                for (int i = 0; i < children.size(); i++) {
                    sb.append(children.get(i).toJson(indentLevel + 2));
                    if (i < children.size() - 1) {
                        sb.append(",");
                    }
                    sb.append("\n");
                }
                sb.append(childIndent).append("]\n");
            } else {
                sb.append("\n");
            }
            
            sb.append(indent).append("}");
            return sb.toString();
        }
        
        static String escapeJson(String str) {
            if (str == null) return "";
            return str.replace("\\", "\\\\")
                      .replace("\"", "\\\"")
                      .replace("\n", "\\n")
                      .replace("\r", "\\r")
                      .replace("\t", "\\t");
        }
    }
    
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        
        String inputFile;
        String outputFile;
        boolean prettyPrint = true;
        
        // Get inputs
        if (args.length >= 1) {
            inputFile = args[0];
            outputFile = args.length >= 2 ? args[1] : inputFile.replace(".txt", ".json");
            prettyPrint = args.length >= 3 ? Boolean.parseBoolean(args[2]) : true;
        } else {
            System.out.println("=== Tree to JSON Converter ===\n");
            
            System.out.print("Enter path to tree output file: ");
            inputFile = scanner.nextLine().trim();
            
            System.out.print("Enter output JSON file (default: auto-named): ");
            String outInput = scanner.nextLine().trim();
            outputFile = outInput.isEmpty() ? inputFile.replace(".txt", ".json") : outInput;
            
            System.out.print("Pretty print JSON? (true/false, default: true): ");
            String prettyInput = scanner.nextLine().trim();
            prettyPrint = prettyInput.isEmpty() || Boolean.parseBoolean(prettyInput);
        }
        
        // Validate input
        if (!new File(inputFile).exists()) {
            System.err.println("Error: Input file not found: " + inputFile);
            scanner.close();
            System.exit(1);
        }
        
        scanner.close();
        
        try {
            System.out.println("\nReading tree file: " + inputFile);
            List<String> lines = readLines(inputFile);
            System.out.println("Total lines read: " + lines.size());
            
            System.out.println("Parsing tree structure...");
            TreeNode root = parseTree(lines);
            
            System.out.println("Converting to JSON...");
            String json = root.toJson(0);
            
            if (!prettyPrint) {
                json = json.replaceAll("\\s+", " ");
            }
            
            System.out.println("Writing JSON to: " + outputFile);
            writeJson(outputFile, json);
            
            File jsonFile = new File(outputFile);
            System.out.println("\n=== SUCCESS ===");
            System.out.println("Output file: " + outputFile);
            System.out.println("File size: " + formatBytes(jsonFile.length()));
            System.out.println("Compression ratio: " + String.format("%.2f", (double) lines.size() / jsonFile.length()));
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    static List<String> readLines(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty() && !isSummaryLine(line)) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }
    
    static boolean isSummaryLine(String line) {
        return line.matches(".*\\d+\\s+dir.*") || line.matches(".*\\d+\\s+file.*");
    }
    
    static int getDepth(String line) {
        int depth = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                depth++;
            } else {
                break;
            }
        }
        return depth / 4; // Assuming 4 spaces per level
    }
    
    static String extractName(String line) {
        String cleaned = line.replaceAll("[|+\\\\\\-]", " ").trim();
        return cleaned;
    }
    
    static boolean isFile(String line) {
        String name = extractName(line);
        return name.contains(".");
    }
    
    static TreeNode parseTree(List<String> lines) {
        TreeNode root = new TreeNode("root", false);
        TreeNode[] levelStack = new TreeNode[50];
        levelStack[0] = root;
        
        for (String line : lines) {
            int depth = getDepth(line);
            String name = extractName(line);
            boolean isFileFlag = isFile(line);
            
            if (name.isEmpty()) continue;
            
            TreeNode node = new TreeNode(name, isFileFlag);
            TreeNode parent = levelStack[depth];
            
            if (parent != null) {
                parent.children.add(node);
            }
            
            levelStack[depth + 1] = node;
        }
        
        return root;
    }
    
    static void writeJson(String filePath, String json) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(json);
        }
    }
    
    static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
