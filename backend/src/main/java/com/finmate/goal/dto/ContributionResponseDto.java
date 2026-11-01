package com.finmate.goal.dto;

public class ContributionResponseDto {
    private ContributionDto contribution;
    private SavingsGoalDto goal;
    private boolean goalCompleted;

    public ContributionResponseDto() {}

    public ContributionResponseDto(ContributionDto contribution, SavingsGoalDto goal, boolean goalCompleted) {
        this.contribution = contribution;
        this.goal = goal;
        this.goalCompleted = goalCompleted;
    }

    public ContributionDto getContribution() {
        return contribution;
    }

    public void setContribution(ContributionDto contribution) {
        this.contribution = contribution;
    }

    public SavingsGoalDto getGoal() {
        return goal;
    }

    public void setGoal(SavingsGoalDto goal) {
        this.goal = goal;
    }

    public boolean isGoalCompleted() {
        return goalCompleted;
    }

    public void setGoalCompleted(boolean goalCompleted) {
        this.goalCompleted = goalCompleted;
    }
}
