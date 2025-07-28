package pro.axenix_innovation.axenapi.web.generate;

import pro.axenix_innovation.axenapi.web.model.ServiceInfo;

import java.util.List;

public interface CodeGenerator {

    void generateCode(List<ServiceInfo> serviceInfoList, String directory);

}
