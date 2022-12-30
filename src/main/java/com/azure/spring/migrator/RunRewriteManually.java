package com.azure.spring.migrator;

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Result;
import org.openrewrite.config.Environment;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public class RunRewriteManually {
    public static void main(String[] args) throws IOException {
        // determine your project directory and provide a list of
        // paths to jars that represent the project's classpath
        String sourcePath = args[0];
        if(StringUtils.isBlank(sourcePath)){
            System.out.println("Please input the file url as arg0");
        }
        Path projectDir = Paths.get(sourcePath);
        List<Path> classpath = emptyList();


        // put any rewrite recipe jars on this main method's runtime classpath
        // and either construct the recipe directly or via an Environment
        Environment environment = Environment.builder().scanRuntimeClasspath().build();



        // create a JavaParser instance with your classpath
        JavaParser javaParser = JavaParser.fromJavaVersion()
                .classpath(classpath)
                .build();

        // walk the directory structure where your Java sources are located
        // and create a list of them
        List<Path> sourcePaths = Files.find(projectDir, 999, (p, bfa) ->
                        bfa.isRegularFile() && p.getFileName().toString().endsWith(".java"))
                .collect(Collectors.toList());

        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);

        // parser the source files into LSTs
        List<J.CompilationUnit> cus = javaParser.parse(sourcePaths, projectDir, ctx);

        // collect results
        String recipeListString = args[1];
        if(StringUtils.isBlank(recipeListString)){
            System.out.println("Please input the recipe name as arg1, seperated by ,");
        }
        String[] recipeStrings=recipeListString.split(",");
        for(String recipeString:recipeStrings) {
            //Recipe recipe = environment.activateRecipes("org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_7");
            Recipe recipe = environment.activateRecipes(recipeString);
            List<Result> results = recipe.run(cus, ctx).getResults();

            for (Result result : results) {
                // print diffs to the console
                // System.out.println(result.diff(projectDir));

                // or overwrite the file on disk with changes.
                Path backPath = Paths.get(projectDir.toString(), result.getAfter().getSourcePath().toString());
                Files.writeString(backPath,
                        result.getAfter().printAll());
            }
            System.out.println("Recipe "+recipeString+" is finished");
        }

    }
}