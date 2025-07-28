# Примеры EventGraphDTO с расшифровками

[3_service_example.json](./3_service_example.json)

## Order Service

### Events Sent
- **Order**: Sent to `order_topic`.
- **Check**: Sent to `check_topic`.

### Topics Read From
- Reads **Order** from `order_topic`.

## Employee Service (emp_service)

### Events Sent
- **Emp**: Sent to `emp_topic`.
- **Check**: Sent to `check_topic`.

### Topics Read From
- Reads **Emp** from `emp_topic`.

## Check Service

### Events Sent
- **Check**: Sent to `check_topic`.

### Topics Read From
- Reads **Order** from `order_topic`.
- Reads **Emp** from `emp_topic`.

# Другие примеры есть в:
1. [примеры EvenGraphDTO](./src/test/resources/results/)
2. [примеры спецификаций](./src/test/resources/specs/json/) 
3. для каждой спецификации есть прямое соответсвие - по имени
4. если в имени спецификации есть число, то эти спецификации можно использовать для импорта /upload POST : результат соединения содержится в файле с таким же именем, только без цифры. 

# Документация по формату спецификации OpenAPI в AxenAPI

## Общая структура

Эта документация описывает формат спецификации OpenAPI, используемый в вашей системе, включая все поддерживаемые типы брокеров, структуру путей и расширения. Документация основана на тестовых случаях из предоставленного кода и включает практические примеры использования.

Спецификация должна быть в формате YAML/Json и содержать следующие обязательные элементы (далее на пример используется yaml):

```yaml
openapi: 3.0.0
info:
  title: Название сервиса
  version: Версия сервиса
```

### Основные секции

### 1. Paths

Определяет пути и конечные точки сервиса. Формат пути зависит от типа брокера:

#### Kafka

```language-
/kafka/{group}/{topic}/{event}:
  post:
    responses:
      '200':
        description: OK
```

* `group` - группа потребителей (только для kafka)

* `topic` - название топика

* `event` - название события

#### JMS

```language-
/jms/{topic}/{event}:
  post:
    responses:
      '200':
        description: OK

```

#### RabbitMQ

```language-
/rabbitmq/{topic}/{event}:
  post:
    responses:
      '200':
        description: OK

```

### 2. Components

Секция `components` содержит описания схем событий:

```language-
components:
  schemas:
    EventName:
      type: object
      x-incoming:
        topics: [topic1, topic2]
      x-outgoing:
        topics: [topic3, topic4]

```

### Расширения у schema (Extensions)

### x-incoming

Определяет входящие топики для события:

```language-
x-incoming:
  topics: [имя_топика1, имя_топика2]

```

### x-outgoing

Определяет исходящие топики для события:

```language-
x-outgoing:
  topics: [имя_топика1, имя_топика2]

```

### Особенности и ограничения

1. Каждое событие должно иметь уникальное имя в рамках спецификации.

2. Поддерживаемые типы брокеров:

    * KAFKA

    * JMS

    * RABBITMQ

3. Для Kafka обязательно указание группы потребителей в пути.

4. События могут быть как входящими (x-incoming), так и исходящими (x-outgoing) одновременно.

### Примеры

### Простой сервис с одним событием

```language-
openapi: 3.0.0
info:
  title: Test Service
  version: 1.0.0
paths:
  /kafka/group1/topic1/Event1:
    post:
      responses:
        '200':
          description: OK
components:
  schemas:
    Event1:
      type: object
      x-incoming:
        topics: [topic1]

```

### Сервис с несколькими брокерами

```language-
openapi: 3.0.0
info:
  title: Complex Service
  version: 1.0.0
paths:
  /kafka/group1/topicA/EventA:
    post:
      responses:
        '200':
          description: OK
  /jms/topicB/EventB:
    post:
      responses:
        '200':
          description: OK
components:
  schemas:
    EventA:
      type: object
      x-incoming:
        topics: [topicA]
    EventB:
      type: object
      x-incoming:
        topics: [topicB]
      x-outgoing:
        topics: [topicA]

```