import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TreeComparator {
    
    static class DiffResult {
        List<String> deleted = new ArrayList<>();
        List<String> added = new ArrayList<>();
    }
    
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        
        String oldFile;
        String newFile;
        String outputDir = ".";
        
        // Get file paths from user
        if (args.length >= 2) {
            oldFile = args[0];
            newFile = args[1];
            if (args.length >= 3) {
                outputDir = args[2];
            }
        } else {
            System.out.println("=== Tree Comparator ===\n");
            
            System.out.print("Enter path to OLD tree file: ");
            oldFile = scanner.nextLine().trim();
            
            System.out.print("Enter path to NEW tree file: ");
            newFile = scanner.nextLine().trim();
            
            System.out.print("Enter output directory (default '.'): ");
            String outDirInput = scanner.nextLine().trim();
            if (!outDirInput.isEmpty()) {
                outputDir = outDirInput;
            }
        }
        
        // Validate file paths
        if (!new File(oldFile).exists()) {
            System.err.println("Error: Old tree file not found: " + oldFile);
            scanner.close();
            System.exit(1);
        }
        if (!new File(newFile).exists()) {
            System.err.println("Error: New tree file not found: " + newFile);
            scanner.close();
            System.exit(1);
        }
        
        scanner.close();
        
        try {
            System.out.println("\nReading old tree: " + oldFile);
            List<String> oldLines = readLines(oldFile);
            
            System.out.println("Reading new tree: " + newFile);
            List<String> newLines = readLines(newFile);
            
            System.out.println("Old tree has " + oldLines.size() + " lines");
            System.out.println("New tree has " + newLines.size() + " lines");
            
            // Compare and find differences
            DiffResult diff = compareTrees(oldLines, newLines);
            
            // Create output directory if needed
            File outputDirFile = new File(outputDir);
            if (!outputDirFile.exists()) {
                outputDirFile.mkdirs();
            }
            
            // Output results
            String comparisonPath = new File(outputDir, "comparison_result.txt").getAbsolutePath();
            System.out.println("\nWriting comparison result to: " + comparisonPath);
            writeComparisonOutput(comparisonPath, diff);
            
            // Output old tree in path format
            String oldPathOutput = new File(outputDir, "old_tree_paths.txt").getAbsolutePath();
            System.out.println("Writing old tree paths to: " + oldPathOutput);
            writePathFormat(oldPathOutput, oldLines);
            
            // Output new tree in path format
            String newPathOutput = new File(outputDir, "new_tree_paths.txt").getAbsolutePath();
            System.out.println("Writing new tree paths to: " + newPathOutput);
            writePathFormat(newPathOutput, newLines);
            
            System.out.println("\n=== SUCCESS ===");
            System.out.println("Files created:");
            System.out.println("  - " + comparisonPath);
            System.out.println("  - " + oldPathOutput);
            System.out.println("  - " + newPathOutput);
            System.out.println("\nSummary:");
            System.out.println("  Deleted items: " + diff.deleted.size());
            System.out.println("  Added items: " + diff.added.size());
            
        } catch (IOException e) {
            System.err.println("Error processing files: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Read lines from tree command output, skipping empty lines and summary lines
     */
    static List<String> readLines(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip empty lines and summary lines (typically last line with file/folder count)
                if (!line.trim().isEmpty() && !isSummaryLine(line)) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }
    
    /**
     * Check if line is a summary line from tree command
     */
    static boolean isSummaryLine(String line) {
        // Pattern like "    5 dirs,    10 files"
        return line.matches(".*\\d+\\s+dir.*") || line.matches(".*\\d+\\s+file.*");
    }
    
    /**
     * Get indentation level of a line in tree format
     */
    static int getIndentLevel(String line) {
        int indent = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                indent++;
            } else if (c == '|' || c == '+' || c == '-') {
                // Tree characters count as single indent
                break;
            } else {
                break;
            }
        }
        return indent;
    }
    
    /**
     * Extract clean name from tree formatted line
     */
    static String extractName(String line) {
        // Remove tree characters and leading spaces
        String cleaned = line.replaceAll("[|+\\-]", " ").trim();
        return cleaned;
    }
    
    /**
     * Compare two tree outputs and find additions and deletions
     */
    static DiffResult compareTrees(List<String> oldLines, List<String> newLines) {
        DiffResult result = new DiffResult();
        
        Set<String> oldSet = new HashSet<>(oldLines);
        Set<String> newSet = new HashSet<>(newLines);
        
        // Find deletions: lines in old but not in new, maintaining tree structure
        for (String line : oldLines) {
            if (!newSet.contains(line)) {
                result.deleted.add(line);
            }
        }
        
        // Find additions: lines in new but not in old, maintaining tree structure
        for (String line : newLines) {
            if (!oldSet.contains(line)) {
                result.added.add(line);
            }
        }
        
        return result;
    }
    
    /**
     * Write comparison output in tree format
     */
    static void writeComparisonOutput(String outputPath, DiffResult diff) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write("=== TREE COMPARISON RESULT ===\n\n");
            
            // Write deletions
            writer.write("DELETED (no longer present):\n");
            writer.write("------------------------------------------------------------\n");
            if (diff.deleted.isEmpty()) {
                writer.write("[No deletions]\n");
            } else {
                for (String line : diff.deleted) {
                    writer.write(line + "\n");
                }
            }
            
            writer.write("\n\n");
            writer.write("ADDED (newly created):\n");
            writer.write("------------------------------------------------------------\n");
            if (diff.added.isEmpty()) {
                writer.write("[No additions]\n");
            } else {
                for (String line : diff.added) {
                    writer.write(line + "\n");
                }
            }
            
            writer.write("\n");
            writer.write("------------------------------------------------------------\n");
            writer.write("Total Deleted: " + diff.deleted.size() + "\n");
            writer.write("Total Added: " + diff.added.size() + "\n");
        }
    }
    
    /**
     * Convert tree format to full file paths
     */
    static void writePathFormat(String outputPath, List<String> treeLines) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            Stack<String> pathStack = new Stack<>();
            
            for (String line : treeLines) {
                String name = extractName(line);
                int indent = getIndentLevel(line);
                
                // Calculate depth (each 4 spaces = 1 level, accounting for tree chars)
                int depth = indent / 4;
                
                // Pop stack to match current depth
                while (pathStack.size() > depth) {
                    pathStack.pop();
                }
                
                // Build full path
                StringBuilder fullPath = new StringBuilder();
                for (String p : pathStack) {
                    fullPath.append(p).append(File.separator);
                }
                fullPath.append(name);
                
                writer.write(fullPath.toString() + "\n");
                
                // Add current item to stack (could be file or directory)
                pathStack.push(name);
            }
        }
    }
}
