package io.holunda.polyflow.view.jpa

import com.fasterxml.jackson.databind.ObjectMapper
import io.holixon.axon.gateway.query.RevisionValue
import io.holunda.camunda.taskpool.api.business.*
import io.holunda.camunda.taskpool.api.task.TaskAssignedEngineEvent
import io.holunda.camunda.taskpool.api.task.TaskCompletedEngineEvent
import io.holunda.camunda.taskpool.api.task.TaskCreatedEngineEvent
import io.holunda.camunda.variable.serializer.serialize
import io.holunda.polyflow.view.DataEntry
import io.holunda.polyflow.view.Task
import io.holunda.polyflow.view.TaskWithDataEntries
import io.holunda.polyflow.view.auth.User
import io.holunda.polyflow.view.jpa.itest.TestApplication
import io.holunda.polyflow.view.jpa.process.toSourceReference
import io.holunda.polyflow.view.query.task.*
import org.assertj.core.api.Assertions.assertThat
import org.axonframework.messaging.MetaData
import org.axonframework.queryhandling.GenericSubscriptionQueryUpdateMessage
import org.axonframework.queryhandling.QueryUpdateEmitter
import org.axonframework.queryhandling.SubscriptionQueryUpdateMessage
import org.camunda.bpm.engine.variable.Variables.createVariables
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.function.Predicate

@RunWith(SpringRunner::class)
@SpringBootTest(
  classes = [TestApplication::class],
  properties = [
    "polyflow.view.jpa.stored-items=TASK,data-entry"
  ]
)
@ActiveProfiles("itest", "mock-query-emitter")
@Transactional
internal class JpaPolyflowViewServiceTaskITest {

  private val emittedQueryUpdates: MutableList<QueryUpdate<Any>> = mutableListOf()

  @Autowired
  lateinit var queryUpdateEmitter: QueryUpdateEmitter

  @Autowired
  lateinit var jpaPolyflowViewService: JpaPolyflowViewTaskService

  @Autowired
  lateinit var jpaPolyflowViewDataEntryService: JpaPolyflowViewDataEntryService

  @Autowired
  lateinit var dbCleaner: DbCleaner

  @Autowired
  lateinit var objectMapper: ObjectMapper

  private val id = UUID.randomUUID().toString()
  private val id2 = UUID.randomUUID().toString()
  private val id3 = UUID.randomUUID().toString()
  private val id4 = UUID.randomUUID().toString()
  private val dataId1 = UUID.randomUUID().toString()
  private val dataType1 = "io.polyflow.test1"
  private val dataId2 = UUID.randomUUID().toString()
  private val dataType2 = "io.polyflow.test2"
  private val now = Instant.now()

  @Before
  fun `ingest events`() {
    val payload = mapOf(
      "key" to "value",
      "key-int" to 1,
      "complex" to Pojo(
        attribute1 = "value",
        attribute2 = Date.from(now)
      )
    )

    val dataEntry = DataEntry(
      entryType = "entryType",
      entryId = "entryId",
      name = "name",
      type = "type",
      applicationName = "applicationName",
      description = "description"
    )



    jpaPolyflowViewService.on(
      event = TaskCreatedEngineEvent(
        id = id,
        taskDefinitionKey = "task.def.0815",
        name = "task name 1",
        priority = 50,
        sourceReference = processReference().toSourceReference(),
        payload = createVariables().apply { putAll(payload) },
        businessKey = "business-1",
        createTime = Date.from(now),
        candidateUsers = setOf("kermit"),
        candidateGroups = setOf("muppets")
      ), metaData = MetaData.emptyInstance()
    )

    jpaPolyflowViewService.on(
      event = TaskAssignedEngineEvent(
        id = id,
        taskDefinitionKey = "task.def.0815",
        name = "task name 1",
        priority = 25,
        sourceReference = processReference().toSourceReference(),
        payload = createVariables().apply { putAll(payload) },
        businessKey = "business-1",
        createTime = Date.from(now),
        candidateUsers = setOf("kermit"),
        candidateGroups = setOf("muppets"),
        assignee = "kermit"
      ), metaData = MetaData.emptyInstance()
    )

    jpaPolyflowViewService.on(
      event = TaskCreatedEngineEvent(
        id = id2,
        taskDefinitionKey = "task.def.0815",
        name = "task name 2",
        priority = 10,
        sourceReference = processReference().toSourceReference(),
        payload = createVariables().apply { putAll(payload) },
        businessKey = "business-2",
        createTime = Date.from(now),
        candidateUsers = setOf("piggy"),
        candidateGroups = setOf("muppets")
      ), metaData = MetaData.emptyInstance()
    )

    jpaPolyflowViewService.on(
      event = TaskCompletedEngineEvent(
        id = id2,
        taskDefinitionKey = "task.def.0815",
        name = "task name 2",
        priority = 10,
        sourceReference = processReference().toSourceReference(),
        payload = createVariables().apply { putAll(payload) },
        businessKey = "business-2",
        createTime = Date.from(now),
        assignee = "piggy",
        candidateUsers = setOf("piggy"),
        candidateGroups = setOf("muppets")
      ), metaData = MetaData.emptyInstance()
    )

    // for testing: fun query(query: TaskWithDataEntriesForIdQuery)
    jpaPolyflowViewService.on(
      event = TaskCreatedEngineEvent(
        id = id3,
        taskDefinitionKey = "task.def.0815",
        name = "task name 3",
        priority = 10,
        sourceReference = processReference().toSourceReference(),
        payload = createVariables().apply { putAll(payload) },
        correlations = newCorrelations().apply { put(dataType1, dataId1) },
        businessKey = "business-3",
        createTime = Date.from(now),
        candidateUsers = setOf("luffy"),
        candidateGroups = setOf("strawhats")
      ), metaData = MetaData.emptyInstance()
    )

    jpaPolyflowViewDataEntryService.on(
      event = DataEntryCreatedEvent(
        entryType = dataType1,
        entryId = dataId1,
        type = "Test",
        applicationName = "test-application",
        name = "Test Entry 1",
        state = ProcessingType.IN_PROGRESS.of("In progress"),
        payload = serialize(payload = payload, mapper = objectMapper),
        authorizations = listOf(
          AuthorizationChange.addUser("luffy"),
          AuthorizationChange.addGroup("strawhats")
        ),
        createModification = Modification(
          time = OffsetDateTime.ofInstant(now, ZoneOffset.UTC),
          username = "luffy",
          log = "strawhats",
          logNotes = "Created the entry"
        )
      ),
      metaData = RevisionValue(revision = 1).toMetaData()
    )

    // for testing: fun query(query: TasksWithDataEntriesForUserQuery)
    jpaPolyflowViewService.on(
      event = TaskCreatedEngineEvent(
        id = id4,
        taskDefinitionKey = "task.def.0815",
        name = "task name 4",
        priority = 10,
        sourceReference = processReference().toSourceReference(),
        payload = createVariables().apply { putAll(payload) },
        correlations = newCorrelations().apply {
          put(dataType1, dataId1)
          put(dataType2, dataId2)
        },
        businessKey = "business-4",
        createTime = Date.from(now),
        candidateUsers = setOf("zoro"),
        candidateGroups = setOf("strawhats")
      ), metaData = MetaData.emptyInstance()
    )

    jpaPolyflowViewDataEntryService.on(
      event = DataEntryCreatedEvent(
        entryType = dataType2,
        entryId = dataId2,
        type = "Test",
        applicationName = "test-application",
        name = "Test Entry 1",
        state = ProcessingType.IN_PROGRESS.of("In progress"),
        payload = serialize(payload = payload, mapper = objectMapper),
        authorizations = listOf(
          AuthorizationChange.addUser("zoro")
        ),
        createModification = Modification(
          time = OffsetDateTime.ofInstant(now, ZoneOffset.UTC),
          username = "zoro",
          log = "Created",
          logNotes = "Created the entry"
        )
      ),
      metaData = RevisionValue(revision = 1).toMetaData()
    )
  }

  @After
  fun `cleanup projection`() {
    dbCleaner.cleanup()
    // clear updates
    emittedQueryUpdates.clear()
    clearInvocations(queryUpdateEmitter)
  }

  @Test
  fun `should find the task by id`() {
    val byId1 = jpaPolyflowViewService.query(TaskForIdQuery(id = id))
    assertThat(byId1).isNotNull
    assertThat(byId1!!.id).isEqualTo(id)
  }

  @Test
  fun `should find the task with data entries by id`() {
    val byId3 = jpaPolyflowViewService.query(TaskWithDataEntriesForIdQuery(id = id3))
    assertThat(byId3).isNotNull
    assertThat(byId3!!.task.id).isEqualTo(id3)
    assertThat(byId3.dataEntries).isNotEmpty.hasSize(1)
    assertThat(byId3.dataEntries.first().entryId).isEqualTo(dataId1)
  }

  @Test
  fun `should find the task by user with data entries`() {
    val zoro = jpaPolyflowViewService.query(TasksWithDataEntriesForUserQuery(user = User("zoro", setOf())))
    assertThat(zoro.elements).isNotEmpty.hasSize(1)
    assertThat(zoro.elements[0].task.id).isEqualTo(id4)
    assertThat(zoro.elements[0].task.name).isEqualTo("task name 4")
    assertThat(zoro.elements[0].dataEntries).isNotEmpty.hasSize(1);
    assertThat(zoro.elements[0].dataEntries[0].entryId).isEqualTo(dataId2);
    val strawhats = jpaPolyflowViewService.query(TasksWithDataEntriesForUserQuery(user = User("other", setOf("strawhats"))))
    assertThat(strawhats.elements).isNotEmpty.hasSize(2)
    assertThat(strawhats.elements.map { it.task.id }).contains(id3,id4)
    assertThat(strawhats.elements[0].dataEntries).hasSize(1)
    assertThat(strawhats.elements[0].dataEntries[0].entryId).isEqualTo(dataId1)
    assertThat(strawhats.elements[1].dataEntries).hasSize(1)
    assertThat(strawhats.elements[1].dataEntries[0].entryId).isEqualTo(dataId1)
  }

  @Test
  fun `should find the task by user`() {
    val kermit = jpaPolyflowViewService.query(TasksForUserQuery(user = User("kermit", setOf())))
    assertThat(kermit.elements).isNotEmpty
    assertThat(kermit.elements[0].id).isEqualTo(id)
    assertThat(kermit.elements[0].name).isEqualTo("task name 1")
    val muppets = jpaPolyflowViewService.query(TasksForUserQuery(user = User("other", setOf("muppets"))))
    assertThat(muppets.elements).isNotEmpty
    assertThat(muppets.elements[0].id).isEqualTo(id)
  }

  @Test
  fun `query updates are sent`() {
    captureEmittedQueryUpdates()
    assertThat(emittedQueryUpdates).hasSize(36)

    assertThat(emittedQueryUpdates.filter { it.queryType == TaskForIdQuery::class.java && it.asTask().id == id }).hasSize(2)
    assertThat(emittedQueryUpdates.filter { it.queryType == TaskForIdQuery::class.java && it.asTask().id == id2 }).hasSize(2)
    assertThat(emittedQueryUpdates.filter { it.queryType == TaskForIdQuery::class.java && it.asTask().id == id2 && it.asTask().deleted }).hasSize(1)

    assertThat(emittedQueryUpdates.filter { it.queryType == TaskWithDataEntriesForIdQuery::class.java && it.asTaskWithDataEntries().task.id == id }).hasSize(2)
    assertThat(emittedQueryUpdates.filter { it.queryType == TaskWithDataEntriesForIdQuery::class.java && it.asTaskWithDataEntries().task.id == id2 }).hasSize(2)
    assertThat(emittedQueryUpdates.filter { it.queryType == TaskWithDataEntriesForIdQuery::class.java && it.asTaskWithDataEntries().task.id == id2 && it.asTaskWithDataEntries().task.deleted })
      .hasSize(1)

    assertThat(emittedQueryUpdates.filter {
      it.queryType == TasksForApplicationQuery::class.java && it.asTaskQueryResult().elements.map { task -> task.id }.contains(id)
    }).hasSize(2)
    assertThat(emittedQueryUpdates.filter {
      it.queryType == TasksForApplicationQuery::class.java && it.asTaskQueryResult().elements.map { task -> task.id }.contains(id2)
    }).hasSize(2)

    assertThat(emittedQueryUpdates.filter {
      it.queryType == TasksForUserQuery::class.java && it.asTaskQueryResult().elements.map { task -> task.id }.contains(id)
    }).hasSize(2)
    assertThat(emittedQueryUpdates.filter {
      it.queryType == TasksForUserQuery::class.java && it.asTaskQueryResult().elements.map { task -> task.id }.contains(id2)
    }).hasSize(2)

    assertThat(emittedQueryUpdates.filter {
      it.queryType == TasksWithDataEntriesForUserQuery::class.java && it.asTaskWithDataEntriesQueryResult().elements.map { taskW -> taskW.task.id }.contains(id)
    }).hasSize(2)
    assertThat(emittedQueryUpdates.filter {
      it.queryType == TasksWithDataEntriesForUserQuery::class.java && it.asTaskWithDataEntriesQueryResult().elements.map { taskW -> taskW.task.id }
        .contains(id2)
    }).hasSize(2)

  }

  private fun captureEmittedQueryUpdates(): List<QueryUpdate<Any>> {
    val queryTypeCaptor = argumentCaptor<Class<Any>>()
    val predicateCaptor = argumentCaptor<Predicate<Any>>()
    val updateCaptor = argumentCaptor<SubscriptionQueryUpdateMessage<Any>>()
    verify(queryUpdateEmitter, atLeast(0)).emit(queryTypeCaptor.capture(), predicateCaptor.capture(), updateCaptor.capture())
    clearInvocations(queryUpdateEmitter)

    val foundUpdates = queryTypeCaptor.allValues
      .zip(predicateCaptor.allValues)
      .zip(updateCaptor.allValues) { (queryType, predicate), update ->
        QueryUpdate(queryType, predicate, update)
      }

    emittedQueryUpdates.addAll(foundUpdates)
    return foundUpdates
  }

  data class QueryUpdate<E>(val queryType: Class<E>, val predicate: Predicate<E>, val update: Any) {
    @Suppress("UNCHECKED_CAST")
    fun asTask(): Task = (this.update as GenericSubscriptionQueryUpdateMessage<Task>).payload

    @Suppress("UNCHECKED_CAST")
    fun asTaskWithDataEntries(): TaskWithDataEntries = (this.update as GenericSubscriptionQueryUpdateMessage<TaskWithDataEntries>).payload

    @Suppress("UNCHECKED_CAST")
    fun asTaskQueryResult(): TaskQueryResult = (this.update as GenericSubscriptionQueryUpdateMessage<TaskQueryResult>).payload

    @Suppress("UNCHECKED_CAST")
    fun asTaskWithDataEntriesQueryResult(): TasksWithDataEntriesQueryResult =
      (this.update as GenericSubscriptionQueryUpdateMessage<TasksWithDataEntriesQueryResult>).payload

  }

}
