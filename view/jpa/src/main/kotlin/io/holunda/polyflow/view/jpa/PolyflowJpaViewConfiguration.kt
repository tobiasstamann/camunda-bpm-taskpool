package io.holunda.polyflow.view.jpa

import org.axonframework.eventhandling.tokenstore.jpa.TokenEntry
import org.axonframework.modelling.saga.repository.jpa.SagaEntry
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * From here and below, scan for components, entities and JPA repositiories.
 */
@EnableConfigurationProperties(PolyflowJpaViewProperties::class)
@ComponentScan
@EntityScan(
  basePackageClasses = [
    PolyflowJpaViewConfiguration::class,
    // for the token
    TokenEntry::class,
    // we are a projection, Sagas might needed too.
    SagaEntry::class
  ]
)
@EnableJpaRepositories(
  basePackageClasses = [
    PolyflowJpaViewConfiguration::class,
  ]
)
@Configuration
class PolyflowJpaViewConfiguration
