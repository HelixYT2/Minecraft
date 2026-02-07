using System.Text.Json;
using HelixHub.Models;
using HelixHub.Protocol;

namespace HelixHub.Services;

public class PlanExecutor
{
    private readonly WebSocketServer _wsServer;
    private CancellationTokenSource? _cts;
    private bool _isRunning;
    
    public event Action<PlanStep, StepStatus, string?>? StepStatusChanged;
    public event Action<string>? LogMessage;
    public event Action? ExecutionCompleted;
    public event Action<PlanStep, string>? StepFailed;
    
    public bool IsRunning => _isRunning;
    public Plan? CurrentPlan { get; private set; }
    public int CurrentStepIndex { get; private set; }
    
    public PlanExecutor(WebSocketServer wsServer)
    {
        _wsServer = wsServer;
    }
    
    public async Task ExecutePlan(Plan plan, string sessionId)
    {
        if (_isRunning)
        {
            Log("Already executing a plan");
            return;
        }
        
        CurrentPlan = plan;
        CurrentStepIndex = 0;
        _isRunning = true;
        _cts = new CancellationTokenSource();
        
        Log($"Starting plan execution: {plan.Goal}");
        Log($"Steps: {plan.Steps.Count}");
        
        try
        {
            for (int i = 0; i < plan.Steps.Count; i++)
            {
                if (_cts.Token.IsCancellationRequested)
                {
                    Log("Plan execution cancelled");
                    break;
                }
                
                CurrentStepIndex = i;
                var step = plan.Steps[i];
                
                Log($"Executing step {i + 1}/{plan.Steps.Count}: {step.Title}");
                step.Status = StepStatus.InProgress;
                StepStatusChanged?.Invoke(step, StepStatus.InProgress, "Starting...");
                
                var success = await ExecuteStep(step, sessionId, _cts.Token);
                
                if (!success)
                {
                    step.Status = StepStatus.Failed;
                    StepStatusChanged?.Invoke(step, StepStatus.Failed, step.StatusMessage);
                    
                    Log($"Step failed: {step.StatusMessage}");
                    StepFailed?.Invoke(step, step.StatusMessage ?? "Unknown error");
                    
                    // Wait for user decision (Retry/Skip/Abort)
                    // This will be handled by the ViewModel
                    return;
                }
                
                step.Status = StepStatus.Completed;
                StepStatusChanged?.Invoke(step, StepStatus.Completed, "Completed");
                Log($"Step completed: {step.Title}");
                
                // Small delay between steps
                await Task.Delay(500, _cts.Token);
            }
            
            Log("Plan execution completed successfully!");
        }
        catch (OperationCanceledException)
        {
            Log("Plan execution cancelled");
        }
        catch (Exception ex)
        {
            Log($"Plan execution error: {ex.Message}");
        }
        finally
        {
            _isRunning = false;
            ExecutionCompleted?.Invoke();
        }
    }
    
    private async Task<bool> ExecuteStep(PlanStep step, string sessionId, CancellationToken ct)
    {
        foreach (var action in step.Actions)
        {
            if (ct.IsCancellationRequested)
            {
                return false;
            }
            
            var success = await ExecuteAction(action, sessionId, step, ct);
            if (!success)
            {
                return false;
            }
        }
        
        return true;
    }
    
    private async Task<bool> ExecuteAction(PlanAction action, string sessionId, PlanStep step, CancellationToken ct)
    {
        Log($"  Action: {action.Tool}");
        
        // Handle wait action locally
        if (action.Tool == "wait")
        {
            if (action.Args != null && action.Args.TryGetValue("seconds", out var secondsObj))
            {
                var seconds = Convert.ToInt32(secondsObj);
                step.StatusMessage = $"Waiting {seconds} seconds...";
                StepStatusChanged?.Invoke(step, StepStatus.InProgress, step.StatusMessage);
                
                await Task.Delay(seconds * 1000, ct);
            }
            return true;
        }
        
        // Convert args to Dictionary<string, object>
        Dictionary<string, object>? args = null;
        if (action.Args != null)
        {
            args = new Dictionary<string, object>();
            foreach (var kvp in action.Args)
            {
                if (kvp.Value is JsonElement element)
                {
                    args[kvp.Key] = ConvertJsonElement(element);
                }
                else
                {
                    args[kvp.Key] = kvp.Value;
                }
            }
        }
        
        MessageEnvelope? response;
        
        // Determine if this is a tool request or action request
        var toolActions = new[] { "get_player_state", "get_baritone_status" };
        
        if (toolActions.Contains(action.Tool))
        {
            response = await _wsServer.SendToolRequest(sessionId, action.Tool, args);
        }
        else
        {
            response = await _wsServer.SendActionRequest(sessionId, action.Tool, args);
        }
        
        if (response == null)
        {
            step.StatusMessage = "Request timed out or connection lost";
            return false;
        }
        
        // Check response
        if (response.Type == MessageTypes.ActionResult)
        {
            var result = response.GetPayload<ActionResultPayload>();
            if (result == null)
            {
                step.StatusMessage = "Invalid response from mod";
                return false;
            }
            
            if (!result.Success)
            {
                step.StatusMessage = result.Error ?? "Action failed";
                return false;
            }
            
            // For baritone_command, we might need to wait for completion
            if (action.Tool == "baritone_command")
            {
                step.StatusMessage = "Baritone task started, monitoring progress...";
                StepStatusChanged?.Invoke(step, StepStatus.InProgress, step.StatusMessage);
                
                // Wait for Baritone to complete (poll status)
                await WaitForBaritoneCompletion(sessionId, step, ct);
            }
            
            return true;
        }
        else if (response.Type == MessageTypes.ToolResponse)
        {
            var result = response.GetPayload<ToolResponsePayload>();
            if (result == null)
            {
                step.StatusMessage = "Invalid response from mod";
                return false;
            }
            
            if (!result.Success)
            {
                step.StatusMessage = result.Error ?? "Tool failed";
                return false;
            }
            
            return true;
        }
        else if (response.Type == MessageTypes.Error)
        {
            var error = response.GetPayload<ErrorPayload>();
            step.StatusMessage = error?.Message ?? "Unknown error";
            return false;
        }
        
        step.StatusMessage = $"Unexpected response type: {response.Type}";
        return false;
    }
    
    private async Task WaitForBaritoneCompletion(string sessionId, PlanStep step, CancellationToken ct)
    {
        var lastStatus = "";
        var stuckCounter = 0;
        
        while (!ct.IsCancellationRequested)
        {
            await Task.Delay(2000, ct);
            
            var response = await _wsServer.SendToolRequest(sessionId, "get_baritone_status");
            if (response == null) continue;
            
            var toolResponse = response.GetPayload<ToolResponsePayload>();
            if (toolResponse?.Data == null) continue;
            
            try
            {
                var baritoneState = JsonSerializer.Deserialize<BaritoneState>(
                    toolResponse.Data.Value.GetRawText(),
                    new JsonSerializerOptions { PropertyNameCaseInsensitive = true }
                );
                
                if (baritoneState == null) continue;
                
                if (!baritoneState.Active)
                {
                    Log("  Baritone task completed");
                    return;
                }
                
                var currentStatus = baritoneState.Progress ?? baritoneState.CurrentGoal ?? "Working...";
                step.StatusMessage = currentStatus;
                StepStatusChanged?.Invoke(step, StepStatus.InProgress, currentStatus);
                
                // Check if stuck (same status for too long)
                if (currentStatus == lastStatus)
                {
                    stuckCounter++;
                    if (stuckCounter > 30) // ~60 seconds
                    {
                        Log("  Baritone appears stuck, continuing to next step");
                        return;
                    }
                }
                else
                {
                    stuckCounter = 0;
                    lastStatus = currentStatus;
                }
            }
            catch
            {
                // Continue polling
            }
        }
    }
    
    private object ConvertJsonElement(JsonElement element)
    {
        return element.ValueKind switch
        {
            JsonValueKind.String => element.GetString() ?? "",
            JsonValueKind.Number => element.TryGetInt32(out var i) ? i : element.GetDouble(),
            JsonValueKind.True => true,
            JsonValueKind.False => false,
            JsonValueKind.Null => null!,
            _ => element.GetRawText()
        };
    }
    
    public void Cancel()
    {
        _cts?.Cancel();
        
        // Send stop_all to the mod
        if (CurrentPlan != null && _wsServer.Instances.Any())
        {
            var sessionId = _wsServer.Instances.Keys.FirstOrDefault();
            if (sessionId != null)
            {
                _ = _wsServer.SendActionRequest(sessionId, "stop_all");
            }
        }
    }
    
    public void RetryCurrentStep(string sessionId)
    {
        if (CurrentPlan == null || !_isRunning) return;
        
        _ = Task.Run(async () =>
        {
            var step = CurrentPlan.Steps[CurrentStepIndex];
            step.Status = StepStatus.InProgress;
            StepStatusChanged?.Invoke(step, StepStatus.InProgress, "Retrying...");
            
            var success = await ExecuteStep(step, sessionId, _cts?.Token ?? CancellationToken.None);
            
            if (success)
            {
                step.Status = StepStatus.Completed;
                StepStatusChanged?.Invoke(step, StepStatus.Completed, "Completed");
                
                // Continue with remaining steps
                await ContinueExecution(sessionId);
            }
            else
            {
                step.Status = StepStatus.Failed;
                StepStatusChanged?.Invoke(step, StepStatus.Failed, step.StatusMessage);
                StepFailed?.Invoke(step, step.StatusMessage ?? "Unknown error");
            }
        });
    }
    
    public void SkipCurrentStep(string sessionId)
    {
        if (CurrentPlan == null || !_isRunning) return;
        
        var step = CurrentPlan.Steps[CurrentStepIndex];
        step.Status = StepStatus.Skipped;
        StepStatusChanged?.Invoke(step, StepStatus.Skipped, "Skipped by user");
        
        _ = ContinueExecution(sessionId);
    }
    
    private async Task ContinueExecution(string sessionId)
    {
        if (CurrentPlan == null) return;
        
        for (int i = CurrentStepIndex + 1; i < CurrentPlan.Steps.Count; i++)
        {
            if (_cts?.Token.IsCancellationRequested == true)
            {
                break;
            }
            
            CurrentStepIndex = i;
            var step = CurrentPlan.Steps[i];
            
            Log($"Executing step {i + 1}/{CurrentPlan.Steps.Count}: {step.Title}");
            step.Status = StepStatus.InProgress;
            StepStatusChanged?.Invoke(step, StepStatus.InProgress, "Starting...");
            
            var success = await ExecuteStep(step, sessionId, _cts?.Token ?? CancellationToken.None);
            
            if (!success)
            {
                step.Status = StepStatus.Failed;
                StepStatusChanged?.Invoke(step, StepStatus.Failed, step.StatusMessage);
                StepFailed?.Invoke(step, step.StatusMessage ?? "Unknown error");
                return;
            }
            
            step.Status = StepStatus.Completed;
            StepStatusChanged?.Invoke(step, StepStatus.Completed, "Completed");
            
            await Task.Delay(500);
        }
        
        _isRunning = false;
        ExecutionCompleted?.Invoke();
        Log("Plan execution completed!");
    }
    
    public void Abort()
    {
        Cancel();
        _isRunning = false;
        CurrentPlan = null;
    }
    
    private void Log(string message)
    {
        LogMessage?.Invoke($"[Executor] {message}");
    }
}
