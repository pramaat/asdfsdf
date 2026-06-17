import java.io.*;
import java.util.*;

public class TreeToJsonConverter {
    
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        
        String inputFile;
        String outputFile;
        
        // Get inputs
        if (args.length >= 1) {
            inputFile = args[0];
            outputFile = args.length >= 2 ? args[1] : inputFile.replace(".txt", ".json");
        } else {
            System.out.println("=== Tree to JSON Converter ===\n");
            
            System.out.print("Enter path to tree output file: ");
            inputFile = scanner.nextLine().trim();
            
            System.out.print("Enter output JSON file (default: auto-named): ");
            String outInput = scanner.nextLine().trim();
            outputFile = outInput.isEmpty() ? inputFile.replace(".txt", ".json") : outInput;
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
            long startTime = System.currentTimeMillis();
            
            // Stream processing - read and convert line by line without storing all lines
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            
            Stack<String> pathStack = new Stack<>();
            Stack<Boolean> hasMoreStack = new Stack<>();
            String rootName = null;
            int lineCount = 0;
            
            String line;
            List<String> lines = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty() && !isSummaryLine(line)) {
                    lines.add(line);
                    lineCount++;
                }
            }
            reader.close();
            
            System.out.println("Total lines: " + lineCount);
            
            // Write JSON with streaming approach
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            
            int indent = 1;
            for (int i = 0; i < lines.size(); i++) {
                String currentLine = lines.get(i);
                int currentDepth = getDepth(currentLine);
                String name = extractName(currentLine);
                
                if (name.isEmpty()) continue;
                
                if (i == 0) {
                    rootName = name;
                    json.append("  \"").append(escapeJson(name)).append("\": [\n");
                    indent = 2;
                    continue;
                }
                
                // Peek at next line to determine structure
                String nextLine = i + 1 < lines.size() ? lines.get(i + 1) : null;
                int nextDepth = nextLine != null ? getDepth(nextLine) : 0;
                
                // Add indentation
                for (int j = 0; j < indent; j++) {
                    json.append("  ");
                }
                
                json.append("\"").append(escapeJson(name)).append("\"");
                
                // If next item is deeper, this is a directory (start array)
                if (nextDepth > currentDepth) {
                    json.append(": [\n");
                    indent++;
                } else {
                    // This is a file or end of directory
                    if (i < lines.size() - 1) {
                        json.append(",\n");
                    } else {
                        json.append("\n");
                    }
                }
                
                // Close arrays if next item is shallower
                while (nextDepth < currentDepth) {
                    currentDepth--;
                    indent--;
                    for (int j = 0; j < indent; j++) {
                        json.append("  ");
                    }
                    json.append("]");
                    if (nextDepth <= currentDepth - 1 && i < lines.size() - 1) {
                        json.append(",\n");
                    } else if (i < lines.size() - 1) {
                        json.append(",\n");
                    } else {
                        json.append("\n");
                    }
                }
            }
            
            // Close remaining arrays
            while (indent > 1) {
                indent--;
                for (int j = 0; j < indent; j++) {
                    json.append("  ");
                }
                json.append("]");
                if (indent > 1) {
                    json.append(",\n");
                } else {
                    json.append("\n");
                }
            }
            
            json.append("}\n");
            
            writer.write(json.toString());
            writer.close();
            
            long duration = System.currentTimeMillis() - startTime;
            File jsonFile = new File(outputFile);
            
            System.out.println("\n=== SUCCESS ===");
            System.out.println("Output file: " + outputFile);
            System.out.println("File size: " + formatBytes(jsonFile.length()));
            System.out.println("Processing time: " + duration + "ms");
            System.out.println("Speed: " + String.format("%.2f", lineCount / (duration / 1000.0)) + " lines/sec");
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
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
        // Remove tree characters (|, +, -, \) and trim
        String cleaned = line.replaceAll("[|+\\\\\\-]", " ").trim();
        return cleaned;
    }
    
    static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
