package io.holunda.camunda.taskpool

import io.holunda.camunda.taskpool.collector.CamundaTaskpoolCollectorConfiguration
import org.springframework.context.annotation.Import

/**
 * Enables the task collector, which listens to Camunda Spring Events and performs, collecting, enriching and sending
 * of taskpool commands to Task Pool Core.
 */
@MustBeDocumented
@Import(CamundaTaskpoolCollectorConfiguration::class)
@EnableTaskpoolSender
annotation class EnableCamundaTaskpoolCollector
