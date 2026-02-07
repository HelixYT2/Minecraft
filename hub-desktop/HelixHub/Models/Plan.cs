using System.Text.Json.Serialization;

namespace HelixHub.Models;

public class Plan
{
    [JsonPropertyName("goal")]
    public string Goal { get; set; } = "";
    
    [JsonPropertyName("assumptions")]
    public List<string> Assumptions { get; set; } = new();
    
    [JsonPropertyName("steps")]
    public List<PlanStep> Steps { get; set; } = new();
    
    [JsonPropertyName("safety")]
    public SafetySettings Safety { get; set; } = new();
}

public class PlanStep
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = "";
    
    [JsonPropertyName("title")]
    public string Title { get; set; } = "";
    
    [JsonPropertyName("rationale")]
    public string? Rationale { get; set; }
    
    [JsonPropertyName("preconditions")]
    public List<string> Preconditions { get; set; } = new();
    
    [JsonPropertyName("actions")]
    public List<PlanAction> Actions { get; set; } = new();
    
    [JsonPropertyName("success_criteria")]
    public List<string> SuccessCriteria { get; set; } = new();
    
    // Runtime tracking
    [JsonIgnore]
    public StepStatus Status { get; set; } = StepStatus.Pending;
    
    [JsonIgnore]
    public string? StatusMessage { get; set; }
}

public class PlanAction
{
    [JsonPropertyName("tool")]
    public string Tool { get; set; } = "";
    
    [JsonPropertyName("args")]
    public Dictionary<string, object>? Args { get; set; }
}

public class SafetySettings
{
    [JsonPropertyName("requires_user_approval")]
    public bool RequiresUserApproval { get; set; } = true;
    
    [JsonPropertyName("no_external_network")]
    public bool NoExternalNetwork { get; set; } = true;
}

public enum StepStatus
{
    Pending,
    InProgress,
    Completed,
    Failed,
    Skipped
}
