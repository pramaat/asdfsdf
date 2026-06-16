import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TreeComparator {
    
    static class TreeNode {
        String name;
        int depth;
        boolean isFile;
        Map<String, TreeNode> children = new LinkedHashMap<>();
        TreeNode parent;
        
        TreeNode(String name, int depth, boolean isFile) {
            this.name = name;
            this.depth = depth;
            this.isFile = isFile;
        }
        
        void addChild(String key, TreeNode child) {
            children.put(key, child);
            child.parent = this;
        }
        
        String getFullPath(String separator) {
            if (parent == null || parent.name == null) {
                return name;
            }
            return parent.getFullPath(separator) + separator + name;
        }
    }
    
    static class ComparisonResult {
        Map<String, TreeNode> deleted = new LinkedHashMap<>();
        Map<String, TreeNode> added = new LinkedHashMap<>();
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
            
            // Parse tree structures
            System.out.println("\nParsing tree structures...");
            TreeNode oldRoot = parseTree(oldLines);
            TreeNode newRoot = parseTree(newLines);
            
            // Compare trees
            ComparisonResult comparison = compareTrees(oldRoot, newRoot);
            
            // Create output directory if needed
            File outputDirFile = new File(outputDir);
            if (!outputDirFile.exists()) {
                outputDirFile.mkdirs();
            }
            
            // Write output
            String outputPath = new File(outputDir, "comparison_result.txt").getAbsolutePath();
            System.out.println("Writing comparison result to: " + outputPath);
            writeComparisonOutput(outputPath, comparison);
            
            System.out.println("\n=== SUCCESS ===");
            System.out.println("Deleted items: " + comparison.deleted.size());
            System.out.println("Added items: " + comparison.added.size());
            System.out.println("\nOutput file: " + outputPath);
            
        } catch (IOException e) {
            System.err.println("Error processing files: " + e.getMessage());
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
            } else if (c == '|' || c == '+' || c == '-' || c == '\\') {
                continue;
            } else {
                break;
            }
        }
        return depth / 4; // Assuming 4 spaces per level
    }
    
    static String extractName(String line) {
        String cleaned = line.replaceAll("[|+\\\\-]", " ").trim();
        return cleaned;
    }
    
    static boolean isFile(String line) {
        // Simple heuristic: if it has an extension, it's likely a file
        String name = extractName(line);
        return name.contains(".");
    }
    
    static TreeNode parseTree(List<String> lines) {
        TreeNode root = new TreeNode(null, -1, false);
        TreeNode[] levelStack = new TreeNode[20]; // Support up to 20 levels
        levelStack[0] = root;
        
        for (String line : lines) {
            int depth = getDepth(line);
            String name = extractName(line);
            boolean isFileFlag = isFile(line);
            
            TreeNode node = new TreeNode(name, depth, isFileFlag);
            
            // Add to parent at previous level
            if (depth > 0) {
                TreeNode parent = levelStack[depth - 1];
                if (parent != null) {
                    parent.addChild(name, node);
                }
            } else {
                root.addChild(name, node);
            }
            
            levelStack[depth] = node;
        }
        
        return root;
    }
    
    static ComparisonResult compareTrees(TreeNode oldRoot, TreeNode newRoot) {
        ComparisonResult result = new ComparisonResult();
        
        // Find deletions
        findDeletions(oldRoot, newRoot, result);
        
        // Find additions
        findAdditions(oldRoot, newRoot, result);
        
        return result;
    }
    
    static void findDeletions(TreeNode oldNode, TreeNode newRoot, ComparisonResult result) {
        for (Map.Entry<String, TreeNode> entry : oldNode.children.entrySet()) {
            String name = entry.getKey();
            TreeNode oldChild = entry.getValue();
            TreeNode newChild = findNodeInTree(newRoot, oldChild);
            
            if (newChild == null) {
                // This node was deleted
                result.deleted.put(oldChild.getFullPath("/"), oldChild);
            } else {
                // Recursively check children
                findDeletions(oldChild, newRoot, result);
            }
        }
    }
    
    static void findAdditions(TreeNode oldRoot, TreeNode newNode, ComparisonResult result) {
        for (Map.Entry<String, TreeNode> entry : newNode.children.entrySet()) {
            String name = entry.getKey();
            TreeNode newChild = entry.getValue();
            TreeNode oldChild = findNodeInTree(oldRoot, newChild);
            
            if (oldChild == null) {
                // This node was added
                result.added.put(newChild.getFullPath("/"), newChild);
            } else {
                // Recursively check children
                findAdditions(oldRoot, newChild, result);
            }
        }
    }
    
    static TreeNode findNodeInTree(TreeNode root, TreeNode target) {
        if (root.name != null && root.name.equals(target.name)) {
            return root;
        }
        
        for (TreeNode child : root.children.values()) {
            TreeNode found = findNodeByPath(child, target);
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }
    
    static TreeNode findNodeByPath(TreeNode node, TreeNode target) {
        if (node.name != null && node.name.equals(target.name) && node.isFile == target.isFile) {
            return node;
        }
        
        for (TreeNode child : node.children.values()) {
            TreeNode found = findNodeByPath(child, target);
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }
    
    static void writeComparisonOutput(String outputPath, ComparisonResult comparison) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write("=== TREE COMPARISON RESULT ===\n\n");
            
            // Write deletions
            writer.write("DELETED:\n");
            writer.write("==========\n");
            if (comparison.deleted.isEmpty()) {
                writer.write("[No deletions]\n");
            } else {
                for (String path : comparison.deleted.keySet()) {
                    writer.write(path + "\n");
                }
            }
            
            writer.write("\n");
            writer.write("ADDED:\n");
            writer.write("==========\n");
            if (comparison.added.isEmpty()) {
                writer.write("[No additions]\n");
            } else {
                for (String path : comparison.added.keySet()) {
                    writer.write(path + "\n");
                }
            }
            
            writer.write("\n");
            writer.write("Summary: " + comparison.deleted.size() + " deleted, " + 
                        comparison.added.size() + " added\n");
        }
    }
}
