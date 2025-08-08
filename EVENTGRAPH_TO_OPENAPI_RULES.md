# Translation Rules from EventGraphDTO to OpenAPI Specification

This document outlines the rules for translating an `EventGraphDTO` object into an OpenAPI 3.0 specification.

## 1. Basic Structure

- For each `NodeDTO` of type `SERVICE` in the `EventGraphDTO`, a new OpenAPI specification is created.
- The `info` section of the OpenAPI specification is populated with the service's name and a default description.
- A `paths` object and a `components` object are initialized for each OpenAPI specification.

## 2. Processing Links

The links in the `EventGraphDTO` determine the paths and schemas in the OpenAPI specification. The following link types are processed:

### 2.1. `TOPIC` to `SERVICE`

This represents a message being consumed by a service from a topic.

- A new path is created in the OpenAPI specification. The path is constructed as follows:
  - **Kafka:** `/{broker_type}/{group}/{topic_name}/{event_name}`
  - **JMS:** `/{broker_type}/{topic_name}/{event_name}`
  - **RabbitMQ:** `/{broker_type}/{topic_name}/{event_name}`
  - **Undefined Broker:** `/undefined_broker/{topic_name}/{event_name}`
- A `post` operation is added to the path.
- If the `EventDTO` associated with the link is not null, a new schema is created in the `components/schemas` section of the OpenAPI specification. The schema is named after the event.
- The schema is extended with an `x-incoming` attribute, which contains the topic name and any tags associated with the link or event.

### 2.2. `SERVICE` to `TOPIC`

This represents a message being produced by a service to a topic.

- If the `EventDTO` associated with the link is not null, a new schema is created in the `components/schemas` section of the OpenAPI specification. The schema is named after the event.
- The schema is extended with an `x-outgoing` attribute, which contains the topic name and any tags associated with the link or event.

### 2.3. `HTTP` to `SERVICE`

This represents an HTTP request to a service.

- A new path is created in the OpenAPI specification based on the `nodeUrl` of the HTTP node.
- An operation is added to the path corresponding to the `methodType` of the HTTP node.
- If the `EventDTO` associated with the link is not null, a new schema is created in the `components/schemas` section of the OpenAPI specification. The schema is named after the event.
- The schema is extended with an `x-http-name` attribute, which contains the name of the HTTP node.
- The schema is also extended with an `x-incoming` attribute.

### 2.4. HTTP Parameters

If the `from` node of type `HTTP` contains an `httpParameters` object, it is processed to generate OpenAPI parameters:

- **Path Parameters**: For each `HttpParameterDTO` in `httpParameters.pathParameters`, a new `PathParameter` is created.
- **Query Parameters**: For each `HttpParameterDTO` in `httpParameters.queryParameters`, a new `QueryParameter` is created.
- **Header Parameters**: For each `HttpParameterDTO` in `httpParameters.headerParameters`, a new `HeaderParameter` is created.

Each parameter is populated with the `name`, `description`, `required` status, `schema` (type), and `example` from the corresponding `HttpParameterDTO`.

## 3. Schemas

- Schemas are created from the `schema` field of the `EventDTO`.
- If a schema for a given event name already exists in the OpenAPI specification, it is reused.

## 4. Extensions

- `x-documentation-file-links`: This extension is added to services and paths to link to external documentation.
- `x-incoming`: This extension is added to schemas to indicate the source of an event (e.g., a topic).
- `x-outgoing`: This extension is added to schemas to indicate the destination of an event (e.g., a topic).
- `x-http-name`: This extension is added to schemas to indicate the name of the HTTP endpoint that triggers the event.
