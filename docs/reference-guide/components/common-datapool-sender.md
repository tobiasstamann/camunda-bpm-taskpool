---
title: Datapool Collector
pageId: engine-datapool-collector
---

## Datapool Collector


### Purpose
Datapool collector is a component usually deployed as a part of the process application (but not necessary) that
is responsible for collecting the Business Data Events fired by the application in order to allow for creation of
a business data projection. In doing so, it collects and transmits it to Datapool Core.

### Features
 * Provides an API to submit arbitrary changes of business entities
 * Provides an API to track changes (aka. Audit Log)
 * Authorization on business entries
 * Transmission of business entries commands

### Usage and configuration


```xml
    <dependency>
      <groupId>io.holunda.polyflow</groupId>
      <artifactId>datapool-collector</artifactId>
      <version>${camunda-taskpool.version}</version>
    </dependency>
```

Then activate the datapool collector by providing the annotation on any Spring Configuration:

```java

@Configuration
@EnableDataEntrySender
class MyDataEntryCollectorConfiguration {

}

```

### Command transmission

In order to control sending of commands to command gateway, the command sender activation property
`polyflow.integration.sender.data-entry.enabled` (default is `true`) is available. If disabled, the command sender
will log any command instead of sending it to the command gateway.

In addition, you can control by the property `polyflow.integration.sender.data-entry.type` if you want to use the default command sender or provide your own implementation.
The default provided command sender (type: `simple`) just sends the commands synchronously using Axon Command Bus.

TIP: If you want to implement a custom command sending, please provide your own implementation of the interface `DataEntryCommandSender`
(register a Spring Component of the type) and set the property `polyflow.integration.sender.data-entry.type` to `custom`.

#### Handling command transmission

The commands sent by the `Datapool Collector` are received by Command Handlers. The latter may accept or reject commands, depending
on the state of the aggregate and other components. The `SimpleDataEntryCommandSender` is informed about the command outcome. By default, it will log the outcome
to console (success is logged in `DEBUG` log level, errors are using `ERROR` log level).

In some situations it is required to take care of command outcome. A prominent example is to include a metric for command dispatching errors into monitoring. For doing so,
it is possible to provide own handlers for success and error command outcome.

For Data Entry Command Sender (as a part of `Datapool Collector`) please provide a Spring Bean implementing the `io.holunda.polyflow.datapool.sender.DataEntryCommandSuccessHandler`
 and `io.holunda.polyflow.datapool.sender.DataEntryCommandErrorHandler` accordingly.


```kotlin
  @Bean
  @Primary
  fun dataEntryCommandSuccessHandler() = object: DataEntryCommandResultHandler {
    override fun apply(commandMessage: Any, commandResultMessage: CommandResultMessage<out Any?>) {
      // do something here
      logger.info { "Success" }
    }
  }

  @Bean
  @Primary
  fun dataEntryCommandErrorHandler() = object: DataEntryCommandErrorHandler {
    override fun apply(commandMessage: Any, commandResultMessage: CommandResultMessage<out Any?>) {
      // do something here
      logger.error { "Error" }
    }
  }
```
