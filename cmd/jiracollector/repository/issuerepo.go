package repository

import (
	"context"

	_ "github.com/lib/pq"
	"github.com/stebennett/squad-dashboard/cmd/jiracollector/models"
)

type IssueRepository interface {
	StoreIssue(ctx context.Context, jiraIssue models.JiraIssue) error
	StoreTransition(ctx context.Context, jiraTransition models.JiraTransition) error
}
