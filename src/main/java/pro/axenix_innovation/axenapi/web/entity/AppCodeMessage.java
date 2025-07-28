package pro.axenix_innovation.axenapi.web.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Обертка для AppCodeMessageKey, в случае, когда параметры сообщения нужно прокинуть выше
 */
@AllArgsConstructor
@Getter
public class AppCodeMessage {
    private final AppCodeMessageKey enumItem;
    private final Object[] args;
}
