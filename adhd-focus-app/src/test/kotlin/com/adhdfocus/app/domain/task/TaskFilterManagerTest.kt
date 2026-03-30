package com.adhdfocus.app.domain.task

import com.adhdfocus.app.data.model.SyncStatus
import com.adhdfocus.app.data.model.Task
import com.adhdfocus.app.data.model.TaskStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Property-Based Tests for Task Filtering (Property 1 & 3)
 *
 * Feature: adhd-focus-app
 * Property 1: Daily Task Filtering
 * Property 3: Task Organization by Todo_Group
 */
class TaskFilterManagerTest : BehaviorSpec({
    val filterManager = TaskFilterManager()

    Given("TaskFilterManager with property-based test generation") {
        When("filtering today's tasks") {
            Then("should only return tasks created today") {
                checkAll(
                    Arb.int(min = 1, max = 100)
                ) { count ->
                    val today = LocalDate.now()
                    val tasks = (1..count).map { i ->
                        Task(
                            id = UUID.randomUUID().toString(),
                            householdId = "household1",
                            assignedUserId = "user1",
                            title = "Task $i",
                            todoGroup = "Morning",
                            createdAt = today.atStartOfDay(ZoneId.systemDefault())
                                .plusHours(i.toLong())
                                .toInstant(),
                            isDeleted = false
                        )
                    }

                    val result = filterManager.filterTodaysTasks(tasks)
                    result.size shouldBe count
                }
            }
        }

        When("filtering tasks by status") {
            Then("should return only tasks with matching status") {
                checkAll(
                    Arb.enum<TaskStatus>()
                ) { status ->
                    val tasks = listOf(
                        Task(
                            id = "1",
                            householdId = "household1",
                            assignedUserId = "user1",
                            title = "Task 1",
                            todoGroup = "Morning",
                            status = TaskStatus.INCOMPLETE
                        ),
                        Task(
                            id = "2",
                            householdId = "household1",
                            assignedUserId = "user1",
                            title = "Task 2",
                            todoGroup = "Morning",
                            status = TaskStatus.IN_PROGRESS
                        ),
                        Task(
                            id = "3",
                            householdId = "household1",
                            assignedUserId = "user1",
                            title = "Task 3",
                            todoGroup = "Morning",
                            status = TaskStatus.COMPLETED
                        )
                    )

                    val result = filterManager.filterByStatus(tasks, status)
                    result.size shouldBe 1
                    result.first().status shouldBe status
                }
            }
        }

        When("organizing tasks by Todo_Group") {
            Then("should group tasks correctly") {
                checkAll(
                    Arb.string(minSize = 1, maxSize = 20),
                    Arb.string(minSize = 1, maxSize = 20),
                    Arb.string(minSize = 1, maxSize = 20)
                ) { group1, group2, group3 ->
                    val tasks = listOf(
                        Task(
                            id = "1",
                            householdId = "household1",
                            assignedUserId = "user1",
                            title = "Task 1",
                            todoGroup = group1
                        ),
                        Task(
                            id = "2",
                            householdId = "household1",
                            assignedUserId = "user1",
                            title = "Task 2",
                            todoGroup = group2
                        ),
                        Task(
                            id = "3",
                            householdId = "household1",
                            assignedUserId = "user1",
                            title = "Task 3",
                            todoGroup = group1
                        ),
                        Task(
                            id = "4",
                            householdId = "household1",
                            assignedUserId = "user1",
                            title = "Task 4",
                            todoGroup = group3
                        )
                    )

                    val result = filterManager.organizeByTodoGroup(tasks)
                    result[group1]?.size shouldBe 2
                    result[group2]?.size shouldBe 1
                    result[group3]?.size shouldBe 1
                }
            }
        }

        When("filtering pending sync tasks") {
            Then("should return only PENDING tasks") {
                val tasks = listOf(
                    Task(
                        id = "1",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 1",
                        todoGroup = "Morning",
                        syncStatus = SyncStatus.PENDING
                    ),
                    Task(
                        id = "2",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 2",
                        todoGroup = "Morning",
                        syncStatus = SyncStatus.SYNCED
                    ),
                    Task(
                        id = "3",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 3",
                        todoGroup = "Morning",
                        syncStatus = SyncStatus.PENDING
                    )
                )

                val result = filterManager.filterPendingSync(tasks)
                result.size shouldBe 2
                result.all { it.syncStatus == SyncStatus.PENDING } shouldBe true
            }
        }

        When("filtering deleted tasks") {
            Then("should exclude deleted tasks") {
                val tasks = listOf(
                    Task(
                        id = "1",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 1",
                        todoGroup = "Morning",
                        isDeleted = false
                    ),
                    Task(
                        id = "2",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 2",
                        todoGroup = "Morning",
                        isDeleted = true
                    ),
                    Task(
                        id = "3",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 3",
                        todoGroup = "Morning",
                        isDeleted = false
                    )
                )

                val result = filterManager.filterTodaysTasks(tasks)
                result.size shouldBe 2
                result.map { it.id } shouldNotContain "2"
            }
        }

        When("getting unique Todo_Groups") {
            Then("should return all unique groups") {
                checkAll(
                    Arb.string(minSize = 1, maxSize = 20),
                    Arb.string(minSize = 1, maxSize = 20)
                ) { group1, group2 ->
                    val tasks = listOf(
                        Task(
                            id = "1",
                            householdId = "household1",
                            assignedUserId = "user1",
                            title = "Task 1",
                            todoGroup = group1
                        ),
                        Task(
                            id = "2",
                            householdId = "household1",
                            assignedUserId = "user1",
                            title = "Task 2",
                            todoGroup = group2
                        ),
                        Task(
                            id = "3",
                            householdId = "household1",
                            assignedUserId = "user1",
                            title = "Task 3",
                            todoGroup = group1
                        )
                    )

                    val result = filterManager.getUniqueTodoGroups(tasks)
                    result.size shouldBe 2
                    result shouldContain group1
                    result shouldContain group2
                }
            }
        }

        When("sorting by status") {
            Then("should order by incomplete -> in-progress -> completed") {
                val tasks = listOf(
                    Task(
                        id = "1",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 1",
                        todoGroup = "Morning",
                        status = TaskStatus.COMPLETED
                    ),
                    Task(
                        id = "2",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 2",
                        todoGroup = "Morning",
                        status = TaskStatus.INCOMPLETE
                    ),
                    Task(
                        id = "3",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 3",
                        todoGroup = "Morning",
                        status = TaskStatus.IN_PROGRESS
                    )
                )

                val result = filterManager.sortByStatus(tasks)
                result[0].status shouldBe TaskStatus.INCOMPLETE
                result[1].status shouldBe TaskStatus.IN_PROGRESS
                result[2].status shouldBe TaskStatus.COMPLETED
            }
        }

        When("getting count by status") {
            Then("should return correct counts") {
                val tasks = listOf(
                    Task(
                        id = "1",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 1",
                        todoGroup = "Morning",
                        status = TaskStatus.INCOMPLETE
                    ),
                    Task(
                        id = "2",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 2",
                        todoGroup = "Morning",
                        status = TaskStatus.INCOMPLETE
                    ),
                    Task(
                        id = "3",
                        householdId = "household1",
                        assignedUserId = "user1",
                        title = "Task 3",
                        todoGroup = "Morning",
                        status = TaskStatus.COMPLETED
                    )
                )

                val result = filterManager.getCountByStatus(tasks)
                result[TaskStatus.INCOMPLETE] shouldBe 2
                result[TaskStatus.COMPLETED] shouldBe 1
                result[TaskStatus.IN_PROGRESS] shouldBe 0
            }
        }
    }
})