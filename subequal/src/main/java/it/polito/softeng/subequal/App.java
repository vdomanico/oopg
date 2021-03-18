package it.polito.softeng.subequal;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class App {

    @Value public static class Pair {
        File source;
        File target;
    }

    public static void main(String[] args) throws IOException {

        if(args.length < 3){
            throw new IllegalArgumentException("reportsPath && submissionsPath && packagePath required");
        }

        String reportPath = args[0];
        String submissionsPath = args[1];
        String packageDir = args[2];

        File[] directories = new File(submissionsPath).listFiles(File::isDirectory);
        if(directories == null || directories.length < 2){
            throw new IllegalArgumentException("cannot find submissions folders");
        }

        File dir0 = directories[0];
        File dir1 = directories[1];
        File pack0 = dir0.toPath().resolve(packageDir).toFile();
        File pack1 = dir1.toPath().resolve(packageDir).toFile();

        File[] pack0Files = pack0.listFiles(File::isFile);
        File[] pack1Files = pack1.listFiles(File::isFile);

        if(pack0Files == null || pack1Files == null){
            throw new IllegalArgumentException("submissions folder is empty");
        }

        List<Pair> javaFiles = new ArrayList<>();
        for (File fi : pack0Files) {
            if(FilenameUtils.getExtension(fi.getName()).equals("java")){
                for (File fj : pack1Files) {
                    if(FilenameUtils.getExtension(fj.getName()).equals("java")){
                        if (FilenameUtils.getBaseName(fj.getName()).equalsIgnoreCase(FilenameUtils.getBaseName(fi.getName()))) {
                            javaFiles.add(new Pair(fi, fj));
                        }
                    }
                }
            }
        }

        File indexFile = new File(reportPath + "\\" + "index.html");
        List<File> diffFiles = new ArrayList<>();
        for(Pair pair : javaFiles){
            File diffFile = eq(new File(reportPath), pair.source, pair.target);
            diffFiles.add(diffFile);
        }

        String header = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n\n" + "<h1>" + "REPORT" + "</h1>" + "\n";
        StringBuilder sourceStringBuilder = new StringBuilder(header);
        for (File f : diffFiles){
            sourceStringBuilder.append("<p><a href=\"").append(f.getAbsolutePath()).append("\">")
                    .append(f.getName())
                    .append("</a></p>\n");
        }
        Files.write(indexFile.toPath(), sourceStringBuilder.toString().getBytes());

        System.out.println("subequal success");

    }

    @Data
    @AllArgsConstructor
    public static class Line {
        public String line;
        public String color;
    }

    public static File eq(File reportDir,
                            File sourceFile,
                            File targetFile) throws IOException {

        List<String> original = Files.readAllLines(sourceFile.toPath());
        List<String> revised = Files.readAllLines(targetFile.toPath());
        List<String> origTrimmed = original.stream().map(s -> s.replaceAll("\\s","").toLowerCase()).collect(Collectors.toList());
        List<String> revisedTrimmed = revised.stream().map(s -> s.replaceAll("\\s","").toLowerCase()).collect(Collectors.toList());
        Patch<String> patch = DiffUtils.diff(origTrimmed, revisedTrimmed, true);
        List<Line> sourceLines = original.stream().map(s -> new Line(s, "white")).collect(Collectors.toList());
        List<Line> targetLines = revised.stream().map(s -> new Line(s, "white")).collect(Collectors.toList());

        int c = 0;
        for (AbstractDelta<String> delta : patch.getDeltas()) {

            int positionSource = delta.getSource().getPosition();
            int sizeSource = delta.getSource().size();
            int positionTarget = delta.getTarget().getPosition();
            int sizeTarget = delta.getTarget().size();


            if(delta.getType().equals(DeltaType.EQUAL)){
                String color = getColor(c); c++;
                for(int i = 0; i < sizeSource; i++){
                    Line line = sourceLines.get(i + positionSource);
                    line.setColor(color);
                }
                for(int i = 0; i < sizeTarget; i++){
                    Line line = targetLines.get(i + positionTarget);
                    line.setColor(color);
                }
            }
            else if(delta.getType().equals(DeltaType.DELETE)){

                String color = "red";
                List<Line> block = new ArrayList<>();
                for(int i = 0; i < sizeSource; i++){
                    Line line = sourceLines.get(i + positionSource);
                    block.add(line);
                }

                if(sizeTarget > 0){
                    for(int i = 0; i < sizeTarget; i++){
                        Line line = targetLines.get(i + positionTarget);
                        line.setColor(color);
                    }
                }
                else {
                    int match = -1;
                    Line first = block.get(0);
                    String fLine = first.getLine().replaceAll("\\s","").toLowerCase();
                    for(int m = 0; m < targetLines.size(); m++){
                        Line line = targetLines.get(m);
                        if("white".equals(line.getColor())){
                            String tLine = line.getLine().replaceAll("\\s","").toLowerCase();
                            if(tLine.equals(fLine)){
                                match = m;
                                break;
                            }
                        }
                    }
                    if(match > -1){
                        for(int m = match, i = 0; i < block.size() && m < targetLines.size(); m++, i++){
                            Line targetLine = targetLines.get(m);
                            Line sourceLine = block.get(i);
                            String tLine = targetLine.getLine().replaceAll("\\s","").toLowerCase();
                            String bLine = sourceLine.getLine().replaceAll("\\s","").toLowerCase();
                            if(tLine.equals(bLine)){
                                sourceLine.setColor(color);
                                targetLine.setColor(color);
                            }
                        }
                    }
                }
            }

        }

        String sourceName = sourceFile.getParentFile().getParentFile().getName() + "-" + FilenameUtils.getBaseName(sourceFile.getName());
        String targetName = targetFile.getParentFile().getParentFile().getName() + "-" + FilenameUtils.getBaseName(targetFile.getName());

        File source = new File(reportDir + "\\" + sourceName + ".html");
        File target = new File(reportDir + "\\" + targetName + ".html");
        writeToFile(source.toPath(), sourceName, sourceLines);
        writeToFile(target.toPath(), targetName, targetLines);

        String style = "<style> html {width:100%;height:100%;} body {width:100%;height:100%;} </style>";
        String header = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + style + "\n\n" + "<h1>" + sourceName + "_" + targetName + "</h1>" + "\n";
        String leftFrameStyle = "frameborder=\"0\" scrolling=\"yes\" " + "style=\"height: 100%; " + "width: 49%; float: left; \" height=\"100%\" width=\"49%\" "+"align=\"left\"";
        String rightFrameStyle = "frameborder=\"0\" scrolling=\"yes\" " + "style=\"overflow: hidden; height: 100%; width: 49%; \" height=\"100%\" width=\"49%\" align=\"right\"";

        StringBuilder sb = new StringBuilder(header);
        sb.append("<iframe ").append("src=\"").append(source.getAbsolutePath()).append("\" ").append(leftFrameStyle).
                append(" title=\"").append(sourceName).append("\">").append("</iframe>").append("\n");
        sb.append("<iframe ").append("src=\"").append(target.getAbsolutePath()).append("\" ").append(rightFrameStyle).
                append(" title=\"").append(targetName).append("\">").append("</iframe>");

        File global = new File(reportDir + "\\" + sourceName + "_" + targetName + ".html");
        Files.write(global.toPath(), sb.toString().getBytes());
        return global;

    }

    private static void writeToFile(Path path, String headerName, List<Line> lines) throws IOException {

        String header = "<!DOCTYPE html>\n" + "<html>\n" + "<body>\n" + "\n" + "<h1>" + headerName + "</h1>" + "\n";
        StringBuilder sourceStringBuilder = new StringBuilder(header);

        sourceStringBuilder.append("<pre>\n");
        for(Line line : lines){
            sourceStringBuilder
                    .append("<code style=\"background:").append(line.getColor()).append(";\">")
                    .append(line.getLine())
                    .append("</code>").append("</br>").append("\n");
        }
        sourceStringBuilder.append("</pre>");

        Files.write(path, sourceStringBuilder.toString().getBytes());
    }

    public static List<String> colors = Arrays.asList(
            "yellow", "pink", "gray",
            "cyan",
            "darkGray", "magenta",
            "orange", "blue", "lightGray");
    private static String getColor(int c) {
        return colors.get(c % 9);
    }
}
