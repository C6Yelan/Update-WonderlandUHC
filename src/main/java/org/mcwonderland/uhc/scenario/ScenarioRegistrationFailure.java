package org.mcwonderland.uhc.scenario;

public final class ScenarioRegistrationFailure {

    private final String scenarioName;
    private final String action;
    private final String reason;
    private final Throwable cause;

    public ScenarioRegistrationFailure(String scenarioName, String action, Throwable cause) {
        this.scenarioName = scenarioName;
        this.action = action;
        this.cause = cause;
        this.reason = cause == null ? "unknown" : cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public String getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }

    public Throwable getCause() {
        return cause;
    }
}
