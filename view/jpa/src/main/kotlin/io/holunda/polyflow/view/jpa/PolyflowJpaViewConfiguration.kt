package io.holunda.polyflow.view.jpa

import mu.KLogging
import org.axonframework.eventhandling.tokenstore.jpa.TokenEntry
import org.axonframework.modelling.saga.repository.jpa.SagaEntry
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import javax.annotation.PostConstruct
import javax.persistence.EntityManagerFactory

/**
 * From here and below, scan for components, entities and JPA repositories.
 */
@EnableConfigurationProperties(PolyflowJpaViewProperties::class)
@ComponentScan
@EntityScan(
  basePackageClasses = [
    PolyflowJpaViewConfiguration::class,
    // for the token
    TokenEntry::class,
    // we are a projection, Sagas might be needed too.
    SagaEntry::class
  ]
)
@EnableJpaRepositories(
  basePackageClasses = [
    PolyflowJpaViewConfiguration::class,
  ]
)
@Configuration
class PolyflowJpaViewConfiguration(
  val polyflowJpaViewProperties: PolyflowJpaViewProperties
) {
  companion object : KLogging()

  /**
   * Logs a little.
   */
  @PostConstruct
  fun info() {
    logger.info { "VIEW-JPA-001: Initialized JPA view, storing items: ${polyflowJpaViewProperties.storedItems.joinToString(", ") { it.name }}" }
  }
}
