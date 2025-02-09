package io.holunda.polyflow.view.jpa

import io.holunda.camunda.variable.serializer.EqualityPathFilter.Companion.eqExclude
import io.holunda.camunda.variable.serializer.EqualityPathFilter.Companion.eqInclude
import io.holunda.camunda.variable.serializer.FilterType
import io.holunda.camunda.variable.serializer.JsonPathFilterFunction
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.NestedConfigurationProperty

/**
 * Properties to configure JPA View.
 */
@ConstructorBinding
@ConfigurationProperties(prefix = "polyflow.view.jpa")
data class PolyflowJpaViewProperties(
  val payloadAttributeLevelLimit: Int = -1,
  val storedItems: Set<StoredItem> = setOf(StoredItem.DATA_ENTRY),
  @NestedConfigurationProperty
  private val dataEntryFilters: PayloadAttributeFilterPaths = PayloadAttributeFilterPaths()
) {
  /**
   * Extracts JSON path filters out of the properties.
   */
  fun dataEntryJsonPathFilters(): List<Pair<JsonPathFilterFunction, FilterType>> {
    return this.dataEntryFilters.include.map { eqInclude(it) }.plus(this.dataEntryFilters.exclude.map { eqExclude(it) })
  }
}

/**
 * Config properties for the path filters.
 */
@ConstructorBinding
data class PayloadAttributeFilterPaths(
  val include: List<String> = listOf(),
  val exclude: List<String> = listOf()
)

/**
 * Stored item to configure what to save in the DB.
 */
enum class StoredItem {
  /**
   * Task.
   */
  TASK,

  /**
   * Process instance.
   */
  PROCESS_INSTANCE,

  /**
   * Process definition.
   */
  PROCESS_DEFINITION,

  /**
   * Data entry.
   */
  DATA_ENTRY
}
