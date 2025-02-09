package io.holunda.polyflow.view.jpa.process

import java.io.Serializable
import javax.persistence.Column
import javax.persistence.Embeddable

/**
 * Represents process source.
 */
@Embeddable
class SourceReferenceEmbeddable(
  @Column(name = "SOURCE_INSTANCE_ID", nullable = false)
  var instanceId: String,
  @Column(name = "SOURCE_EXECUTION_ID", nullable = false)
  var executionId: String,
  @Column(name = "SOURCE_DEF_ID", nullable = false)
  var definitionId: String,
  @Column(name = "SOURCE_DEF_KEY", nullable = false)
  var definitionKey: String,
  @Column(name = "SOURCE_NAME", nullable = false)
  var name: String,
  @Column(name = "APPLICATION_NAME", nullable = false)
  var applicationName: String,
  @Column(name = "SOURCE_TENANT_ID", nullable = true)
  var tenantId: String? = null,
  @Column(name = "SOURCE_TYPE", nullable = false)
  var sourceType: String
) : Serializable {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as SourceReferenceEmbeddable

    if (instanceId != other.instanceId) return false
    if (executionId != other.executionId) return false
    if (definitionId != other.definitionId) return false
    if (definitionKey != other.definitionKey) return false
    if (name != other.name) return false
    if (applicationName != other.applicationName) return false
    if (tenantId != other.tenantId) return false

    return true
  }

  override fun hashCode(): Int {
    var result = instanceId.hashCode()
    result = 31 * result + executionId.hashCode()
    result = 31 * result + definitionId.hashCode()
    result = 31 * result + definitionKey.hashCode()
    result = 31 * result + name.hashCode()
    result = 31 * result + applicationName.hashCode()
    result = 31 * result + (tenantId?.hashCode() ?: 0)
    return result
  }
}
