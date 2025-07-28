package pro.axenix_innovation.axenapi.web.service;

import pro.axenix_innovation.axenapi.web.entity.ServiceCode;
import pro.axenix_innovation.axenapi.web.model.EventGraphDTO;

public interface CodeService {

    byte[] generateCode(EventGraphDTO eventGraph);

    ServiceCode save(byte[] zip);

    void clear();

}
