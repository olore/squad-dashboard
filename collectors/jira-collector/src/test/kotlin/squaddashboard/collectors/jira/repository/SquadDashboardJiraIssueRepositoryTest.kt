package squaddashboard.collectors.jira.repository

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.testcontainers.perSpec
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainerProvider
import squaddashboard.collectors.jira.model.IngestionType
import squaddashboard.collectors.jira.model.JiraWorkType
import squaddashboard.collectors.jira.model.SquadDashboardJiraIssue
import squaddashboard.collectors.jira.model.SquadDashboardJiraIssueTransition
import squaddashboard.common.test.nextPositiveInt
import java.time.Instant

@ExperimentalStdlibApi
class SquadDashboardJiraIssueRepositoryTest : FunSpec({

    val databaseName = "squad_dashboard"
    val database = PostgreSQLContainerProvider().newInstance("12.3")
        .withUsername("db_root_user")
        .withPassword("db_root_password")
        .withDatabaseName(databaseName)

    val listener = listeners(database.perSpec())

    lateinit var dataSource: HikariDataSource

    beforeSpec {
        val username = "squad_write"
        val password = "squad_writer"

        // run migration
        val placeholders = mutableMapOf(
            "database_name" to databaseName,
            "user_writer_name" to username,
            "user_writer_password" to password
        )

        val migrationsLocation = System.getenv("TEST_MIGRATIONS_LOCATION")!!

        val flyway = Flyway.configure().dataSource(database.jdbcUrl, database.username, database.password)
            .placeholders(placeholders)
            .locations("filesystem:$migrationsLocation")
            .load()
        flyway.migrate()

        val hikariConfig = HikariConfig()
        hikariConfig.jdbcUrl = database.jdbcUrl
        hikariConfig.username = username
        hikariConfig.password = password
        hikariConfig.maximumPoolSize = 1
        dataSource = HikariDataSource(hikariConfig)
    }

    context("jira config") {

        test("should create a project config in database") {
            val repo = SquadDashboardJiraIssueRepository(dataSource)

            val projectKey = "ABC"
            val workStartState = "Work State 1"

            database.getJiraConfigCount(projectKey) shouldBe 0

            repo.createProjectConfig(projectKey, workStartState)

            database.getJiraConfigCount(projectKey) shouldBe 1
            database.getJiraConfig(projectKey).workStartState shouldBe workStartState
        }

        test("should flag ingestion as started in the database") {
            val repo = SquadDashboardJiraIssueRepository(dataSource)

            val projectKey = "DEF"
            val ingestionStartTime = Instant.now()

            database.createJiraConfig(projectKey)

            repo.startIngestion(projectKey, IngestionType.Backfill, ingestionStartTime)

            database.getJiraConfig(projectKey).lastIngestionType shouldBe IngestionType.Backfill
            database.getJiraConfig(projectKey).lastIngestionStarted shouldBe ingestionStartTime
        }

        test("should mark ingestion as completed in the database") {
            val repo = SquadDashboardJiraIssueRepository(dataSource)

            val projectKey = "GHI"
            val ingestionCompletedTime = Instant.now()

            database.createJiraConfig(projectKey)

            repo.completeIngestion(projectKey, ingestionCompletedTime)

            database.getJiraConfig(projectKey).lastIngestionCompleted shouldBe ingestionCompletedTime
        }
    }
    
    context("jira issues") {
        
        test("should add an issue to the database") {
            val repo = SquadDashboardJiraIssueRepository(dataSource)
            val jiraId = Random.nextPositiveInt()
            val issue = SquadDashboardJiraIssue(
                jiraId = jiraId,
                jiraKey = "JKL-1234",
                jiraWorkType = JiraWorkType.Story,
                jiraCreatedAt = Instant.now(),
                jiraProjectKey = "JKL",
                transitions = emptyList()
            )
            
            database.createJiraConfig("JKL")
            
            database.getJiraIssueCountForProject("JKL") shouldBe 0
            
            repo.saveIssue(issue)
            
            database.getJiraIssueCountForProject("JKL") shouldBe 1
            database.getJiraIssue(jiraId) shouldBe issue
        }

        test("should add a transition to the database") {
            val repo = SquadDashboardJiraIssueRepository(dataSource)
            val transitionId = Random.nextPositiveInt()
            val transition = SquadDashboardJiraIssueTransition(
                jiraId = transitionId,
                transitionTo = "In Progress",
                transitionAt = Instant.now()
            )
            val issue = SquadDashboardJiraIssue(
                jiraId = Random.nextPositiveInt(),
                jiraKey = "JKL-89432",
                jiraWorkType = JiraWorkType.Story,
                jiraCreatedAt = Instant.now(),
                jiraProjectKey = "MNO",
                transitions = listOf(transition)
            )

            database.createJiraConfig("MNO")

            repo.saveIssue(issue)

            database.getJiraIssueCountForProject("MNO") shouldBe 1
            database.getJiraTransition(transitionId) shouldBe transition
        }


    }
})
