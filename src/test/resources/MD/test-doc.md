# DefaultApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**jmsTopicConsumeJMS1EventConsumeJMSPost**](DefaultApi.md#jmsTopicConsumeJMS1EventConsumeJMSPost) | **POST** /jms/topicConsumeJMS_1/EventConsumeJMS |  |
| [**kafkaGroup1TopicConsumeKafka1EventConsumeKafkaPost**](DefaultApi.md#kafkaGroup1TopicConsumeKafka1EventConsumeKafkaPost) | **POST** /kafka/group1/topicConsumeKafka_1/EventConsumeKafka |  |
| [**rabbitmqTopicConsumeRabbitMQ1EventConsumeRabbitMQPost**](DefaultApi.md#rabbitmqTopicConsumeRabbitMQ1EventConsumeRabbitMQPost) | **POST** /rabbitmq/topicConsumeRabbitMQ_1/EventConsumeRabbitMQ |  |


<a name="jmsTopicConsumeJMS1EventConsumeJMSPost"></a>
# **jmsTopicConsumeJMS1EventConsumeJMSPost**
> jmsTopicConsumeJMS1EventConsumeJMSPost()



### Parameters
This endpoint does not need any parameter.

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined

<a name="kafkaGroup1TopicConsumeKafka1EventConsumeKafkaPost"></a>
# **kafkaGroup1TopicConsumeKafka1EventConsumeKafkaPost**
> kafkaGroup1TopicConsumeKafka1EventConsumeKafkaPost()



### Parameters
This endpoint does not need any parameter.

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined

<a name="rabbitmqTopicConsumeRabbitMQ1EventConsumeRabbitMQPost"></a>
# **rabbitmqTopicConsumeRabbitMQ1EventConsumeRabbitMQPost**
> rabbitmqTopicConsumeRabbitMQ1EventConsumeRabbitMQPost()



### Parameters
This endpoint does not need any parameter.

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: Not defined

# Documentation for service_no_common_consume_topics_common_events_common_outgoing_topics_1

<a name="documentation-for-api-endpoints"></a>
## Documentation for API Endpoints

All URIs are relative to *http://localhost*

| Class | Method | HTTP request | Description |
|------------ | ------------- | ------------- | -------------|
| *DefaultApi* | [**jmsTopicConsumeJMS1EventConsumeJMSPost**](Apis/DefaultApi.md#jmstopicconsumejms1eventconsumejmspost) | **POST** /jms/topicConsumeJMS_1/EventConsumeJMS |  |
*DefaultApi* | [**kafkaGroup1TopicConsumeKafka1EventConsumeKafkaPost**](Apis/DefaultApi.md#kafkagroup1topicconsumekafka1eventconsumekafkapost) | **POST** /kafka/group1/topicConsumeKafka_1/EventConsumeKafka |  |
*DefaultApi* | [**rabbitmqTopicConsumeRabbitMQ1EventConsumeRabbitMQPost**](Apis/DefaultApi.md#rabbitmqtopicconsumerabbitmq1eventconsumerabbitmqpost) | **POST** /rabbitmq/topicConsumeRabbitMQ_1/EventConsumeRabbitMQ |  |


<a name="documentation-for-models"></a>
## Documentation for Models



<a name="documentation-for-authorization"></a>
## Documentation for Authorization

All endpoints do not require authorization.


## YAML спецификация

```yaml
openapi: 3.0.1
info:
  title: service_no_common_consume_topics_common_events_common_outgoing_topics_1
  description: AxenAPI Specification for service_no_common_consume_topics_common_events_common_outgoing_topics_1
  version: 1.0.0
paths:
  /kafka/group1/topicConsumeKafka_1/EventConsumeKafka:
    post:
      responses:
        "200":
          description: Event sent successfully
  /rabbitmq/topicConsumeRabbitMQ_1/EventConsumeRabbitMQ:
    post:
      responses:
        "200":
          description: Event sent successfully
  /jms/topicConsumeJMS_1/EventConsumeJMS:
    post:
      responses:
        "200":
          description: Event sent successfully
components:
  schemas:
    EventConsumeJMS:
      type: object
      x-incoming:
        topics:
        - topicConsumeJMS_1
    EventOutgoingKafka:
      type: object
      x-outgoing:
        topics:
        - topicOutKafka
    EventConsumeRabbitMQ:
      type: object
      x-incoming:
        topics:
        - topicConsumeRabbitMQ_1
    EventOutgoingJMS:
      type: object
      x-outgoing:
        topics:
        - topicOutJMS
    EventOutgoingRabbitMQ:
      type: object
      x-outgoing:
        topics:
        - topicOutRabbitMQ
    EventConsumeKafka:
      type: object
      x-incoming:
        topics:
        - topicConsumeKafka_1

```