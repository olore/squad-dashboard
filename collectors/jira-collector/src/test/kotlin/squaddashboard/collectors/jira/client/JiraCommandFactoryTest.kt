package squaddashboard.collectors.jira.client

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import squaddashboard.collectors.jira.client.model.JiraCommandFactory

class JiraCommandFactoryTest : FunSpec({

    val jiraCommandFactory = JiraCommandFactory()

    test("should build IssueSearchCommand") {
        val projectKey = "ABC"
        val startAt = 10
        val maxResults = 100

        val issueSearchCommand = jiraCommandFactory.makeSearchForAllIssuesForProjectCommand(projectKey, startAt, maxResults)

        issueSearchCommand should {
            it.jql shouldBe "project = $projectKey"
            it.maxResults shouldBe maxResults
            it.startAt shouldBe startAt
            it.expand shouldContainExactlyInAnyOrder listOf("changelog")
            it.fields shouldContainExactlyInAnyOrder listOf("status", "issuetype", "created", "updated", "summary")
        }
    }
})
