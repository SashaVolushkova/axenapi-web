# Анализ правил преобразования OpenAPI спецификации в EventGraphDTO

## Обзор процесса преобразования

Преобразование OpenAPI спецификации в EventGraphDTO выполняется через следующую цепочку классов:
1. `SolidOpenAPITranslator` - точка входа
2. `OpenAPIToEventGraphFacade` - фасад для преобразования
3. `DefaultEventGraphBuilder` - основной билдер
4. `OpenAPIProcessor` - содержит всю логику преобразования

## Основные правила преобразования

### 1. Создание Service Node

**Правило:** Из каждой OpenAPI спецификации создается один SERVICE узел.

```java
// Из info.title берется имя сервиса
NodeDTO serviceNode = NodeDTO.builder()
    .id(serviceNodeId != null ? serviceNodeId : UUID.randomUUID())
    .name(info.title)
    .type(SERVICE)
    .brokerType(null)
    .belongsToGraph(List.of(serviceNodeId))
    .build();
```

**Дополнительные атрибуты:**
- `documentationFileLinks` - берется из расширения `x-documentation-file-links` на уровне спецификации
- `nodeDescription` - берется из `info.description`

### 2. Обработка путей (paths)

#### 2.1 Брокерные пути (Kafka, JMS, RabbitMQ)

**Правило:** Пути вида `/{broker}/{group}/{topic}/{event}` или `/{broker}/{topic}/{event}` создают:
- TOPIC узел с соответствующим brokerType
- Связь (Link) между TOPIC и SERVICE
- Event с именем из пути

**Примеры паттернов:**
- `/kafka/group1/topic1/Event1` → brokerType=KAFKA, group=group1, topic=topic1, event=Event1
- `/jms/topicJMS/EventJMS` → brokerType=JMS, topic=topicJMS, event=EventJMS
- `/rabbitmq/topicRabbitMQ/EventRabbitMQ` → brokerType=RABBITMQ, topic=topicRabbitMQ, event=EventRabbitMQ

#### 2.2 HTTP пути

**Правило:** Все остальные пути создают HTTP узлы.

```java
NodeDTO httpNode = NodeDTO.builder()
    .id(UUID.randomUUID())
    .name(nodeName != null ? nodeName : path)
    .type(HTTP)
    .nodeUrl(path)
    .methodType(HTTP_METHOD)
    .belongsToGraph(List.of(serviceNodeId))
    .build();
```

**Новые поля д��я HTTP (из axenapi-be.yml):**
- `httpParameters` - path, query, header параметры
- `httpRequestBody` - детальное описание тела запроса
- `httpResponses` - описание ответов для разных статус кодов

### 3. Обработка событий (Events)

#### 3.1 События из схем компонентов

**Правило:** События создаются из схем в `components.schemas`, которые имеют расширения `x-incoming` или `x-outgoing`.

```yaml
components:
  schemas:
    Event1:
      type: object
      x-incoming:
        topics: ["topic1"]
```

Создает:
```java
EventDTO event = EventDTO.builder()
    .id(UUID.randomUUID())
    .name("Event1")
    .schema(jsonSchema)
    .tags(extractedTags)
    .build();
```

#### 3.2 События из HTTP операций

**Правило:** Для HTTP путей события создаются из:
- `requestBody` схем (создается связь)
- `responses` схем (НЕ создается связь, только событие)

**Важное отличие:** 
- Для request события создается Link
- Для response событий Link НЕ создается

### 4. Создание связей (Links)

#### 4.1 Входящие события (x-incoming)

**Правило:** Создается связь FROM topic TO service

```java
LinkDTO incomingLink = LinkDTO.builder()
    .fromId(topicNode.getId())
    .toId(serviceNode.getId())
    .eventId(event.getId())
    .group(kafkaGroup) // только для Kafka
    .build();
```

#### 4.2 Исходящие события (x-outgoing)

**Правило:** Создается связь FROM service TO topic

```java
LinkDTO outgoingLink = LinkDTO.builder()
    .fromId(serviceNode.getId())
    .toId(topicNode.getId())
    .eventId(event.getId())
    .build();
```

#### 4.3 HTTP связи

**Правило:** Создается связь FROM http_node TO service, но только для request событий

```java
LinkDTO httpLink = LinkDTO.builder()
    .fromId(httpNode.getId())
    .toId(serviceNode.getId())
    .eventId(requestEvent.getId()) // может быть null
    .build();
```

### 5. Обработка тегов

**Правило:** Теги собираются из:
- Путей (path-level tags)
- Схем событий (x-incoming/x-outgoing tags)
- Операций

Теги распространяются на:
- Узлы (nodes)
- События (events)
- Связи (links)

### 6. Определение типа брокера

**Правила:**
1. Из пути: `/kafka/...` → KAFKA, `/jms/...` → JMS, `/rabbitmq/...` → RABBITMQ
2. Из x-incoming/x-outgoing: `kafka/topic` → KAFKA
3. По умолчанию: UNDEFINED

## Выявленные противоречия и проблемы

### 1. Несогласованность в обработке HTTP событий

**Проблема:** Response события создаются, но для них не создаются связи. Это приводит к "висячим" событиям в графе.

**В коде:**
```java
// Создаем события для всех responses
if (operation.getResponses() != null) {
    // ... создаем события
    eventGraph.addEvent(event);
    // НО НЕ создаем связи!
}

// Связь создается только для request
if (linkEvent != null) { // linkEvent - это request event
    createHttpLink(nodeDTO, serviceNode, linkEvent, eventGraph);
}
```

### 2. Дублирование логики определения брокера

**Проблема:** Логика определения типа брокера дублируется в нескольких местах:
- `BrokerPathProcessor`
- `OpenAPIProcessor.computeOutgoing`
- `OpenAPIProcessor.processIncomingTopic`

### 3. Неполное заполнение новых HTTP полей

**Проблема:** Новые поля из axenapi-be.yml (`httpParameters`, `httpRequestBody`, `httpResponses`) заполняются, но:
- Не все enum значения обрабатываются корректно
- При ошибке парсинга поля остаются пустыми без предупреждения

### 4. Противоречие в usageContext для событий

**Проблема:** В axenapi-be.yml определено поле `usageContext` с enum `[HTTP, ASYNC_MESSAGING]`, но в коде:
- Для HTTP событий usageContext устанавливается в ["HTTP"]
- Для async событий usageContext НЕ устанавливается вообще

**Ожидаемое поведение:**
- HTTP события: usageContext = ["HTTP"]
- Kafka/JMS/RabbitMQ события: usageContext = ["ASYNC_MESSAGING"]

### 5. Неконсистентность в обработке documentationFileLinks

**Проблема:** `x-documentation-file-links` обрабатывается по-разному:
- Для сервиса - из корня спецификации
- Для путей - из расширений пути
- Для событий - из расширений схемы
- Но НЕ все места проверяются одинаково

### 6. Потеря информации при преобразовании

**Проблема:** Некоторая информация из OpenAPI теряется:
- `operationId` не сохраняется
- `summary` операций не используется
- `deprecated` флаг игнорируется
- Параметры `cookie` не обрабатываются

### 7. Жесткая привязка к media type

**Проблема:** Код обрабатывает только `application/json` media type:
```java
MediaType requestMedia = operation.getRequestBody().getContent().get("application/json");
```

Другие media types (xml, form-data и т.д.) игнорируются.

### 8. Неопределенность с eventType

**Проблема:** В EventDTO есть поле `eventType`, которое в результатах заполняется как "REQUEST" или "RESPONSE" для HTTP, но:
- Это поле не описано в axenapi-be.yml
- Логика его заполнения отсутствует в коде преобразования


## Поля EventGraphDTO и источники данных

| Поле | Тип | Как заполняется | Статус |
|------|-----|-----------------|--------|
| `name` | String | Используется значение `info.title` из OpenAPI-спецификации. При слиянии нескольких графов имена объединяются через `&` (см. `EventGraphFacade.merge`). | OK |
| `nodes` | List<NodeDTO> | Формируется при разборе секции `paths` и, частично, `components.schemas`. Для каждой спецификации всегда создаётся один SERVICE-узел из `info.title`. TOPIC и HTTP-узлы создаются на основе шаблонов путей (см. правила выше). | OK |
| `events` | List<EventDTO> | Создаются из схем в `components.schemas`, пом��ченных расширениями `x-incoming`/`x-outgoing`, а также из `requestBody`/`responses` HTTP-операций. | OK, но для HTTP `responses` создаётся событие без соответствующего Link (см. проблемы) |
| `links` | List<LinkDTO> | Входящие/исходящие создаются на основании `x-incoming`/`x-outgoing`; HTTP-ссылки – только для `requestBody`. При слиянии графов ссылки объединяются, дубликаты (fromId,toId,eventId) пропускаются. | OK, но связи для HTTP-response отсутствуют |
| `errors` | List<ErrorDTO> | Заполняется сервисным кодом (`ProcessingFiles`, `EventGraphService`) при возникновении ошибок чтения/конвертации файлов. В самой OpenAPI-спецификации источника нет. | Требует доработки: нет прямого соответствия спецификации |
| `tags` | Set<String> | Агрегируются из тегов путей, операций, схем событий, а также из `x-documentation-file-links`. При слиянии объединяются. | OK, но источники тегов не унифицированы |

Если какое-либо поле не указано в спецификации или не может быть вычислено, оно остаётся пустым (null/empty) – такие случаи не обрабатываются валидатором и требуют доработки.

---

## Анализ вложенных HTTP-DTO
Ниже приведена сводная таблица по DTO, которые используются внутри `NodeDTO` при описании HTTP-взаимодействий.

### HttpParametersDTO

Поле | Источник | Статус
---- | -------- | ------
pathParameters | `parameters.in == path` | OK
queryParameters | `parameters.in == query` | OK
headerParameters | `parameters.in == header` | OK (cookie-параметры пока игнорируются)

### HttpParameterDTO  *(элемент path/query/header параметров)*

Поле | Тип | Источник (OpenAPI) | Как преобразуется | Статус/Примечание
---- | ---- | ----------------- | ----------------- | ----------------
name | String | `parameter.name` | Копируется без изменений | OK
description | String | `parameter.description` | Копируется | OK
required | Boolean | `parameter.required` | true / false | OK
type | enum(HttpParameterTypeEnum) | `parameter.schema.type` | Маппинг 1-в-1 (`string`→STRING, `integer`→INTEGER …) | OK
schema | String (JSON) | `parameter.schema` | Сериализуем по��ный JSON-Schema; сейчас пишем только `{format}` → **нужно доработать** | TODO
example | String | `parameter.example` или первый элемент `examples.*.value` | Строковое представление | OK, но если `examples` >1, берётся первый

### HttpRequestBodyDTO  *(описание requestBody)*

| Поле        | Тип                  | Источник                                           | Как преобразуется                                | Статус                      |
|-------------|----------------------|----------------------------------------------------|--------------------------------------------------|-----------------------------|
| description | String               | `requestBody.description`                          | 1-в-1                                            | OK                          |
| required    | Boolean              | `requestBody.required`                             | 1-в-1                                            | OK                          |
| content     | List<HttpContentDTO> | `requestBody.content` (map mediaType→MediaTypeObj) | Для каждого mediaType формируется HttpContentDTO | OK (если `content` != null) |

### HttpContentDTO  *(контент для media type)*
Поле | Тип | Источник | Как преобразуется | Статус/Примечание
---- | ---- | -------- | ----------------- | ----------------
mediaType | enum(HttpContentTypeEnum) | key из `content` | Если mediaType не в enum → сохраняем как строку `application/x-custom` и помечаем UNLISTED | Частично реализовано
schema | String (JSON) | `content[mt].schema` | Полный JSON-Schema как строка | OK
example | String | `content[mt].example` | Сериализуем объект в строку | OK, но падаем если объект не сериализуется
examples | List<HttpExampleDTO> | `content[mt].examples` | Перебираем все; сейчас берём только первый → **нужно доработать** | TODO

### HttpExampleDTO  *(единичный пример)*
Поле | Тип | Источник | Примечание
---- | ---- | -------- | ---------
name | String | key в `examples` либо `example.description` | Если нет имени → генерируем `example_<n>`
summary | String | `examples[k].summary` | Копируем | OK
description | String | `examples[k].description` | Копируем | OK
value | String | `examples[k].value` | Если объект ⇒ `objectMapper.writeValueAsString`

### HttpResponseDTO  *(ответ по статус-коду)*
Поле | Тип | Источник | Как преобразуется | Статус/Примечание
---- | ---- | -------- | ----------------- | ----------------
statusCode | enum(HttpStatusCodeEnum) | ключ `responses` | Сохраняем как строку; если не входит в Enum → `"XXX"` и **нужно расш��рить Enum** | Частично реализовано
description | String | `responses[code].description` | 1-в-1 | OK
headers | List<HttpParameterDTO> | `responses[code].headers` | Преобразуем header → HttpParameterDTO (`required`=false) | Частично реализовано (нет schema/example)
content | List<HttpContentDTO> | `responses[code].content` | Аналогично requestBody | OK

### Enum-типы
Enum | Где ставится | Замечания
---- | ----------- | ---------
HttpParameterTypeEnum | При разборе параметров | Поддерживаются базовые `type`; `format` игнорируется
HttpStatusCodeEnum | При формировании ответов | Выбор происходит по строке статуса
HttpContentTypeEnum | Фиксируется по ключу media type | Новый/редкий media type сохраняется, но далее не используется
EventUsageContextEnum | Назначается событиям | HTTP — «HTTP»; async — не заполняется → доработка


### Детальные таблицы по основным вложенным DTO

#### NodeDTO
| Поле | Тип | Источник данных | Статус |
|------|-----|-----------------|--------|
| id | UUID | Если узел уже существует ‑ берётся, иначе `UUID.randomUUID()` | OK |
| name | String | SERVICE: `info.title`; TOPIC: часть пути/`x-incoming`/`x-outgoing`; HTTP: путь или `x-node-name` | OK |
| type | enum(SERVICE, TOPIC, HTTP) | Определяется при создании узла | OK |
| brokerType | enum(KAFKA, JMS, RABBITMQ, UNDEFINED) | По префиксу пути/строке топика, HTTP/SERVICE → UNDEFINED | OK |
| belongsToGraph | List<UUID> | Добавляется UUID сервис-узла-владельца; при merge дополняется | OK |
| nodeDescription | String | SERVICE: `info.description`; TOPIC/HTTP: `x-node-description` (если есть) | Частично, требуется унификация |
| nodeUrl | String | HTTP-узел: путь; иначе null | OK |
| methodType | enum(GET,POST,…) | HTTP-узел: HTTP метод | OK |
| requestBody / responseBody (deprecated) | String | Первый media-type `application/json` | Поддержка, но deprecated |
| httpParameters | HttpParametersDTO | Собирается из параметров операции | OK |
| httpRequestBody | HttpRequestBodyDTO | Из `requestBody` | OK |
| httpResponses | List<HttpResponseDTO> | Из `responses` | OK |
| tags | Set<String> | Объединяются из тегов пути/операции/схемы | OK |
| documentationFileLinks | Set<String> | `x-documentation-file-links` (root/path/schema) | Частично реализовано |

#### EventDTO
| Поле | Тип | Источник данных | Статус |
|------|-----|-----------------|--------|
| id | UUID | `UUID.randomUUID()` | OK |
| name | String | Имя схемы (TOPIC) или `<Path>_<Method>_Request/Response` (HTTP) | OK |
| schema | String | JSON Schema из компонента / requestBody / responses | OK |
| tags | Set<String> | Из `x-tags` схемы + теги операции | OK |
| eventType | String | "REQUEST"/"RESPONSE" для HTTP; иначе null | Требует доработки в спецификации |
| eventDescription | String | `schema.description` | OK |
| usageContext | List<EventUsageContextEnum> | HTTP: [HTTP]; Async: должно быть [ASYNC_MESSAGING] (не реализовано) | Требуется доработка |

#### LinkDTO
| Поле | Тип | Источник данных | Статус |
|------|-----|-----------------|--------|
| id | UUID | `UUID.randomUUID()` | OK |
| fromId | UUID | TOPIC → SERVICE (x-incoming); SERVICE → TOPIC (x-outgoing); HTTP → SERVICE (request) | OK |
| toId | UUID | Аналогично fromId | OK |
| group | String | Для Kafka — consumer group из пути; иначе null | OK |
| eventId | UUID/null | UUID события (request) либо null (HTTP-undefined) | OK |
| tags | Set<String> | Теги операции/пути | Частично используется |

#### ErrorDTO
| Поле | Тип | Источник данных | Статус |
|------|-----|-----------------|--------|
| fileName | String | `ProcessingFiles.processFile` при ошибке чтения | OK |
| errorMessage | String | Текст exception | OK |

---

## Предложенные задачи по доработке
1. **Парсинг `cookie`-параметров.** Поместить их либо в `headerParameters`, либо выделить отдельный список.
2. **Поддержка нескольких примеров** (`examples`) в `HttpContentDTO`.
3. **Установка `usageContext = ASYNC_MESSAGING`** для асинхронных событий.
4. **Унифицировать обработку `documentationFileLinks`** (root / path / schema).
5. **Сравнивать схемы событий** с одинаковым `name` при слиянии графов; при конфликте добавлять запись в `errors`.
6. **Учитывать LINK-дубликаты**, когда в одной ссылке `eventId=null`, а в другой указан UUID.


## Противоречия при слиянии графов

Ниже перечислены обнаруженные нюансы, выявленные при изучении `EventGraphFacadeMergeTest` и фактической логики `EventGraphFacade.merge`.

1. Узлы объединяются только если совпадают `name`, `type` и `brokerType`. Их `id` могут различаться, в результате в итоговом графе будет сохранён `id` первого встреченного узла, а `belongsToGraph` дополняется всеми `id` исходных сервисов. Это ведёт к тому, что ссылки из второго графа переадресуются на «старый» `id`, что может нарушить внешние системы, если они хранят привязку по идентификатору.
2. Теги (`tags`) узлов/событий/ссылок просто объединяются, но возможна потеря порядка или дублирование регистра.
3. При наличии одинаковых событий (совпадение `name`) их схемы (`schema`) не сравниваются. Если схемы различаются — конфликт остаётся незамеченным.
4. При объединении LINKS проверяется дублирование только по комбинации `fromId`,`toId`,`eventId`. Если совпадают `fromId`/`toId`, но один из `eventId` = null, а второй содержит UUID, ссылка будет продублирована.
5. Если в одном из объединяемых графов ссылка ссылается на событие, которого нет во втором графе, то после объединения дубликаты событий могут возникнуть с разными `id`, но одинаковыми `name` (см. пункт 3).

Эти противоречия следует учесть при дальнейшем развитии логики слияния.

## Рекомендации

1. **Добавить связи для response событий** или не создавать их вообще
2. **Унифицировать логику определения типа брокера** в одном месте
3. **Добавить установку usageContext** для async событий
4. **Расширить поддержку media types** beyond application/json
5. **Добавить валидацию** для новых HTTP полей
6. **Документировать** неявные правила преобразования
7. **Добавить обработку eventType** в соответствии с типом события