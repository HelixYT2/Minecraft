using System.Collections.ObjectModel;
using Avalonia.Threading;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using HelixHub.Models;
using HelixHub.Services;

namespace HelixHub.ViewModels;

public partial class MainWindowViewModel : ObservableObject
{
    private readonly WebSocketServer _wsServer;
    private readonly PrismLauncherService _prismService;
    private readonly LmStudioClient _lmStudioClient;
    private readonly PlanExecutor _planExecutor;
    private readonly ConfigService _configService;
    private readonly HubConfig _config;
    
    [ObservableProperty]
    private ObservableCollection<ConnectedInstance> _connectedInstances = new();
    
    [ObservableProperty]
    private ConnectedInstance? _selectedInstance;
    
    [ObservableProperty]
    private ObservableCollection<PrismInstance> _prismInstances = new();
    
    [ObservableProperty]
    private PrismInstance? _selectedPrismInstance;
    
    [ObservableProperty]
    private ObservableCollection<ChatMessage> _chatMessages = new();
    
    [ObservableProperty]
    private string _userInput = "";
    
    [ObservableProperty]
    private ObservableCollection<string> _logs = new();
    
    [ObservableProperty]
    private Plan? _currentPlan;
    
    [ObservableProperty]
    private bool _isPlanPending;
    
    [ObservableProperty]
    private bool _isExecuting;
    
    [ObservableProperty]
    private string _statusText = "Not connected";
    
    [ObservableProperty]
    private string _authToken = "";
    
    [ObservableProperty]
    private string _lmStudioUrl = "";
    
    [ObservableProperty]
    private string _prismLauncherPath = "";
    
    [ObservableProperty]
    private string _prismDataRoot = "";
    
    [ObservableProperty]
    private PlayerState? _currentPlayerState;
    
    [ObservableProperty]
    private BaritoneState? _currentBaritoneState;
    
    [ObservableProperty]
    private bool _isSettingsOpen;
    
    [ObservableProperty]
    private PlanStep? _failedStep;
    
    [ObservableProperty]
    private string _failedStepError = "";
    
    public MainWindowViewModel(
        WebSocketServer wsServer,
        PrismLauncherService prismService,
        LmStudioClient lmStudioClient,
        PlanExecutor planExecutor,
        ConfigService configService,
        HubConfig config)
    {
        _wsServer = wsServer;
        _prismService = prismService;
        _lmStudioClient = lmStudioClient;
        _planExecutor = planExecutor;
        _configService = configService;
        _config = config;
        
        // Initialize from config
        AuthToken = config.AuthToken;
        LmStudioUrl = config.LmStudioBaseUrl;
        PrismLauncherPath = config.PrismLauncherPath;
        PrismDataRoot = config.PrismDataRoot;
        
        // Subscribe to events
        _wsServer.InstanceConnected += OnInstanceConnected;
        _wsServer.InstanceDisconnected += OnInstanceDisconnected;
        _wsServer.PlayerStateUpdated += OnPlayerStateUpdated;
        _wsServer.BaritoneStateUpdated += OnBaritoneStateUpdated;
        _wsServer.LogMessage += OnLogMessage;
        
        _lmStudioClient.LogMessage += OnLogMessage;
        _prismService.LogMessage += OnLogMessage;
        
        _planExecutor.StepStatusChanged += OnStepStatusChanged;
        _planExecutor.LogMessage += OnLogMessage;
        _planExecutor.ExecutionCompleted += OnExecutionCompleted;
        _planExecutor.StepFailed += OnStepFailed;
        
        // Load Prism instances
        RefreshPrismInstances();
        
        // Welcome message
        AddChatMessage("assistant", "Welcome to HelixHub! Connect a Minecraft instance with the Helix Bridge mod to get started.\n\nMake sure to:\n1. Copy the Auth Token from Settings to your mod config\n2. Launch your Minecraft instance\n3. Once connected, type a command like \"Get me full diamond armor\"");
    }
    
    private void OnInstanceConnected(ConnectedInstance instance)
    {
        Dispatcher.UIThread.Post(() =>
        {
            ConnectedInstances.Add(instance);
            if (SelectedInstance == null)
            {
                SelectedInstance = instance;
            }
            UpdateStatus();
            AddChatMessage("system", $"Instance connected: {instance.InstanceId}\nBaritone: {(instance.BaritoneAvailable ? "Available" : "Not available")}");
        });
    }
    
    private void OnInstanceDisconnected(string sessionId)
    {
        Dispatcher.UIThread.Post(() =>
        {
            var instance = ConnectedInstances.FirstOrDefault(i => i.SessionId == sessionId);
            if (instance != null)
            {
                ConnectedInstances.Remove(instance);
                if (SelectedInstance == instance)
                {
                    SelectedInstance = ConnectedInstances.FirstOrDefault();
                }
                AddChatMessage("system", $"Instance disconnected: {instance.InstanceId}");
            }
            UpdateStatus();
        });
    }
    
    private void OnPlayerStateUpdated(string sessionId, PlayerState state)
    {
        Dispatcher.UIThread.Post(() =>
        {
            if (SelectedInstance?.SessionId == sessionId)
            {
                CurrentPlayerState = state;
            }
        });
    }
    
    private void OnBaritoneStateUpdated(string sessionId, BaritoneState state)
    {
        Dispatcher.UIThread.Post(() =>
        {
            if (SelectedInstance?.SessionId == sessionId)
            {
                CurrentBaritoneState = state;
            }
        });
    }
    
    private void OnLogMessage(string message)
    {
        Dispatcher.UIThread.Post(() =>
        {
            Logs.Add(message);
            while (Logs.Count > 1000)
            {
                Logs.RemoveAt(0);
            }
        });
    }
    
    private void OnStepStatusChanged(PlanStep step, StepStatus status, string? message)
    {
        Dispatcher.UIThread.Post(() =>
        {
            // Force UI update
            OnPropertyChanged(nameof(CurrentPlan));
        });
    }
    
    private void OnExecutionCompleted()
    {
        Dispatcher.UIThread.Post(() =>
        {
            IsExecuting = false;
            AddChatMessage("assistant", "Plan execution completed!");
        });
    }
    
    private void OnStepFailed(PlanStep step, string error)
    {
        Dispatcher.UIThread.Post(() =>
        {
            FailedStep = step;
            FailedStepError = error;
        });
    }
    
    private void UpdateStatus()
    {
        StatusText = ConnectedInstances.Count switch
        {
            0 => "No instances connected",
            1 => $"1 instance connected",
            _ => $"{ConnectedInstances.Count} instances connected"
        };
    }
    
    [RelayCommand]
    private async Task SendMessage()
    {
        if (string.IsNullOrWhiteSpace(UserInput)) return;
        
        var input = UserInput;
        UserInput = "";
        
        AddChatMessage("user", input);
        
        if (SelectedInstance == null)
        {
            AddChatMessage("assistant", "Please connect a Minecraft instance first.");
            return;
        }
        
        if (!SelectedInstance.BaritoneAvailable)
        {
            AddChatMessage("assistant", "Warning: Baritone is not available in this instance. Most automation features require Baritone.");
        }
        
        AddChatMessage("assistant", "Generating plan...");
        
        var plan = await _lmStudioClient.GeneratePlan(
            input,
            CurrentPlayerState,
            CurrentBaritoneState
        );
        
        if (plan == null)
        {
            ChatMessages.RemoveAt(ChatMessages.Count - 1);
            AddChatMessage("assistant", "Failed to generate a valid plan. Please try rephrasing your request or check that LM Studio is running.");
            return;
        }
        
        ChatMessages.RemoveAt(ChatMessages.Count - 1);
        CurrentPlan = plan;
        IsPlanPending = true;
        
        var planSummary = $"📋 **Plan: {plan.Goal}**\n\n";
        planSummary += "**Assumptions:**\n";
        foreach (var assumption in plan.Assumptions)
        {
            planSummary += $"• {assumption}\n";
        }
        planSummary += "\n**Steps:**\n";
        for (int i = 0; i < plan.Steps.Count; i++)
        {
            var step = plan.Steps[i];
            planSummary += $"{i + 1}. {step.Title}\n";
        }
        planSummary += "\nReview the plan on the right panel and click **Approve** to execute, or **Reject** to cancel.";
        
        AddChatMessage("assistant", planSummary);
    }
    
    [RelayCommand]
    private async Task ApprovePlan()
    {
        if (CurrentPlan == null || SelectedInstance == null) return;
        
        IsPlanPending = false;
        IsExecuting = true;
        FailedStep = null;
        
        AddChatMessage("assistant", "Executing plan...");
        
        await _planExecutor.ExecutePlan(CurrentPlan, SelectedInstance.SessionId);
    }
    
    [RelayCommand]
    private void RejectPlan()
    {
        CurrentPlan = null;
        IsPlanPending = false;
        AddChatMessage("assistant", "Plan rejected. What would you like to do instead?");
    }
    
    [RelayCommand]
    private void CancelExecution()
    {
        _planExecutor.Cancel();
        IsExecuting = false;
        FailedStep = null;
        AddChatMessage("assistant", "Execution cancelled. All tasks stopped.");
    }
    
    [RelayCommand]
    private void RetryFailedStep()
    {
        if (SelectedInstance == null) return;
        FailedStep = null;
        _planExecutor.RetryCurrentStep(SelectedInstance.SessionId);
    }
    
    [RelayCommand]
    private void SkipFailedStep()
    {
        if (SelectedInstance == null) return;
        FailedStep = null;
        _planExecutor.SkipCurrentStep(SelectedInstance.SessionId);
    }
    
    [RelayCommand]
    private void AbortExecution()
    {
        _planExecutor.Abort();
        IsExecuting = false;
        FailedStep = null;
        CurrentPlan = null;
        AddChatMessage("assistant", "Execution aborted.");
    }
    
    [RelayCommand]
    private void RefreshPrismInstances()
    {
        PrismInstances.Clear();
        foreach (var instance in _prismService.GetInstances())
        {
            PrismInstances.Add(instance);
        }
    }
    
    [RelayCommand]
    private async Task LaunchPrismInstance()
    {
        if (SelectedPrismInstance == null) return;
        
        AddChatMessage("system", $"Launching instance: {SelectedPrismInstance.Name}");
        
        var success = await _prismService.LaunchInstance(SelectedPrismInstance.Id);
        if (success)
        {
            AddChatMessage("system", "Instance launching... waiting for mod to connect.");
        }
        else
        {
            AddChatMessage("system", "Failed to launch instance. Check settings.");
        }
    }
    
    [RelayCommand]
    private void OpenSettings()
    {
        IsSettingsOpen = true;
    }
    
    [RelayCommand]
    private void CloseSettings()
    {
        IsSettingsOpen = false;
    }
    
    [RelayCommand]
    private void SaveSettings()
    {
        _config.AuthToken = AuthToken;
        _config.LmStudioBaseUrl = LmStudioUrl;
        _config.PrismLauncherPath = PrismLauncherPath;
        _config.PrismDataRoot = PrismDataRoot;
        
        _configService.SaveConfig(_config);
        
        RefreshPrismInstances();
        IsSettingsOpen = false;
        
        AddChatMessage("system", "Settings saved. Note: WebSocket server requires restart for token changes.");
    }
    
    [RelayCommand]
    private void GenerateNewToken()
    {
        AuthToken = HubConfig.GenerateToken();
    }
    
    [RelayCommand]
    private void CopyToken()
    {
        // This will be handled in the view with clipboard
    }
    
    [RelayCommand]
    private async Task TestLmStudioConnection()
    {
        var connected = await _lmStudioClient.TestConnection();
        AddChatMessage("system", connected ? "✓ LM Studio connection successful!" : "✗ Failed to connect to LM Studio. Is it running?");
    }
    
    private void AddChatMessage(string role, string content)
    {
        ChatMessages.Add(new ChatMessage
        {
            Role = role,
            Content = content,
            Timestamp = DateTime.Now
        });
    }
}
