package pro.axenix_innovation.axenapi.web.generate.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pro.axenix_innovation.axenapi.web.generate.CodeGenerator;
import pro.axenix_innovation.axenapi.web.model.ServiceInfo;
import pro.axenix_innovation.axenapi.web.service.MessageHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static pro.axenix_innovation.axenapi.web.entity.AppCodeMessageKey.WARN_SERVICE_INFO_LIST_NULL;

@Component
public class CodeGeneratorImpl implements CodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(CodeGeneratorImpl.class);

    static public final String OPENAPI_GENERATOR_CLI_JAR_PATH = "openapi-generator-cli.jar";
    static public final String AXENAPI_GENERATOR_JAR_PATH = "axenapi-generator-2.0.0.jar";
    private static final String CMD_PREFIX = "java -cp ";

    private final String openapiGeneratorCliPath = OPENAPI_GENERATOR_CLI_JAR_PATH;
    private final String axenapiGeneratorPath = AXENAPI_GENERATOR_JAR_PATH;

    @Override
    public void generateCode(List<ServiceInfo> serviceInfoList, String directory) {
        //$ java -cp "axenapi-generator-2.0.0.jar;openapi-generator-cli.jar" org.openapitools.codegen.OpenAPIGenerator generate -g messageBroker -o out/ -i api-docs.json --additional-properties=kafkaBootstrap=localhost:29092
        if (serviceInfoList == null || serviceInfoList.isEmpty()) {
            log.warn(MessageHelper.getStaticMessage(WARN_SERVICE_INFO_LIST_NULL));
            return;
        }
        if (directory == null || directory.trim().isEmpty()) {
            throw new IllegalArgumentException("Output directory must not be null or empty");
        }

        serviceInfoList.forEach(serviceInfo -> {
            directory.trim();
            String outputDir;
            if(directory.endsWith("\\") || directory.endsWith("/")) {
                outputDir = directory + serviceInfo.getName();
            } else {
                outputDir = directory + "\\" + serviceInfo.getName().replaceAll("\\s+", "_");
            }


            boolean mkdirs = new File(outputDir).mkdirs();
            StringBuilder cmd = new StringBuilder();
            cmd.append(CMD_PREFIX).append("\"")
                    .append(axenapiGeneratorPath + ";")
                    .append(openapiGeneratorCliPath)
                    .append("\"")
                    .append(" org.openapitools.codegen.OpenAPIGenerator generate -g messageBroker ")
                    .append("-o " + outputDir + " -i ")
                    .append(serviceInfo.getSpecificationPath() + " ")
                    .append("--additional-properties=kafkaBootstrap=")
                    .append(serviceInfo.getBrokerAddress())
                    .append(",port=")
                    .append(serviceInfo.getPort())
                    .append(",useGradle=true")
                    .append(",artifactId=").append(serviceInfo.getName().replaceAll("\\s+", "_"));
            String cmdStr = cmd.toString();
            System.out.println(cmdStr);
            BufferedReader reader;
            try {
                ProcessBuilder processBuilder = new ProcessBuilder();
                processBuilder.command("cmd.exe", "/c", cmdStr);
                Process exec = processBuilder.start();
                InputStream inputStream = exec.getInputStream();
                reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
                // wait for the process to finish
//                log.info("Process finished with exit code: {}", exec.exitValue());
            } catch (Exception e) {
                e.printStackTrace();
            }


        });
    }
}
