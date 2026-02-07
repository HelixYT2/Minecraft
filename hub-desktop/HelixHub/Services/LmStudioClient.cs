using System.Net.Http.Json;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using HelixHub.Models;
using NJsonSchema;

namespace HelixHub.Services;

public class LmStudioClient
{
    private readonly HttpClient _httpClient;
    private readonly HubConfig _config;
    private JsonSchema? _planSchema;
    
    public event Action<string>? LogMessage;
    
    private static readonly string SystemPrompt = @"You are a Minecraft automation assistant. Your job is to create step-by-step plans for in-game tasks.

CRITICAL RULES:
1. You MUST respond with ONLY valid JSON matching the exact schema below.
2. No markdown, no explanations, no code blocks - ONLY the JSON object.
3. All actions must use the Baritone mod through baritone_command.
4. safety.requires_user_approval and safety.no_external_network MUST both be true.

AVAILABLE TOOLS:
- baritone_command: Execute Baritone commands
  - Common commands: mine <block>, goto <x> <y> <z>, farm, follow <player>, stop, pause, resume
  - For mining: ""mine diamond_ore"", ""mine iron_ore"", etc.
- craft_item: Craft items (requires materials)
  - Args: { ""item"": ""minecraft:item_name"", ""count"": 1 }
- equip_armor: Equip armor from inventory
  - Args: { ""slot"": ""all""|""head""|""chest""|""legs""|""feet"", ""material"": ""diamond""|""netherite""|""iron""|""any"" }
- stop_all: Cancel all tasks
  - Args: {}
- wait: Pause execution
  - Args: { ""seconds"": <number> }

RESPONSE SCHEMA:
{
  ""goal"": ""<clear description of what this plan accomplishes>"",
  ""assumptions"": [""<list of assumptions about current game state>""],
  ""steps"": [
    {
      ""id"": ""<unique step id>"",
      ""title"": ""<short step title>"",
      ""rationale"": ""<why this step is needed>"",
      ""preconditions"": [""<conditions that must be true>""],
      ""actions"": [
        { ""tool"": ""<tool_name>"", ""args"": { <tool_args> } }
      ],
      ""success_criteria"": [""<how to verify success>""]
    }
  ],
  ""safety"": {
    ""requires_user_approval"": true,
    ""no_external_network"": true
  }
}

Example for ""get me 10 diamonds"":
{
  ""goal"": ""Mine 10 diamonds from underground"",
  ""assumptions"": [""Player has a diamond or iron pickaxe"", ""Player is in the Overworld""],
  ""steps"": [
    {
      ""id"": ""1"",
      ""title"": ""Mine diamond ore"",
      ""rationale"": ""Baritone will automatically find and mine diamond ore at Y levels -64 to 16"",
      ""preconditions"": [""Player has suitable pickaxe""],
      ""actions"": [
        { ""tool"": ""baritone_command"", ""args"": { ""command"": ""mine diamond_ore 10"" } }
      ],
      ""success_criteria"": [""Collected at least 10 diamonds""]
    }
  ],
  ""safety"": {
    ""requires_user_approval"": true,
    ""no_external_network"": true
  }
}";

    public LmStudioClient(HubConfig config)
    {
        _config = config;
        _httpClient = new HttpClient
        {
            Timeout = TimeSpan.FromSeconds(120)
        };
        
        LoadPlanSchema();
    }
    
    private void LoadPlanSchema()
    {
        try
        {
            var schemaPath = Path.Combine(AppContext.BaseDirectory, "plan.schema.json");
            if (File.Exists(schemaPath))
            {
                var schemaJson = File.ReadAllText(schemaPath);
                _planSchema = JsonSchema.FromJsonAsync(schemaJson).Result;
            }
        }
        catch (Exception ex)
        {
            Log($"Warning: Could not load plan schema: {ex.Message}");
        }
    }
    
    public async Task<Plan?> GeneratePlan(string userRequest, PlayerState? playerState, BaritoneState? baritoneState)
    {
        var messages = new List<ChatCompletionMessage>
        {
            new() { Role = "system", Content = SystemPrompt }
        };
        
        // Add context about current state
        if (playerState != null || baritoneState != null)
        {
            var contextParts = new List<string>();
            
            if (playerState != null)
            {
                contextParts.Add($"Player position: {playerState.Position}");
                contextParts.Add($"Dimension: {playerState.Dimension}");
                contextParts.Add($"Health: {playerState.Health}/{playerState.MaxHealth}");
                contextParts.Add($"Hunger: {playerState.Hunger}/20");
            }
            
            if (baritoneState != null)
            {
                contextParts.Add($"Baritone available: {baritoneState.Available}");
                if (baritoneState.Active)
                {
                    contextParts.Add($"Baritone active: {baritoneState.CurrentProcess} - {baritoneState.CurrentGoal}");
                }
            }
            
            messages.Add(new ChatCompletionMessage
            {
                Role = "user",
                Content = $"Current game state:\n{string.Join("\n", contextParts)}"
            });
            
            messages.Add(new ChatCompletionMessage
            {
                Role = "assistant",
                Content = "Understood. I have the current game state. What would you like me to help with?"
            });
        }
        
        messages.Add(new ChatCompletionMessage
        {
            Role = "user",
            Content = userRequest
        });
        
        for (int attempt = 0; attempt <= _config.MaxPlanRepairAttempts; attempt++)
        {
            try
            {
                var response = await CallLmStudio(messages);
                
                if (string.IsNullOrWhiteSpace(response))
                {
                    Log("LM Studio returned empty response");
                    continue;
                }
                
                // Try to extract JSON from response
                var jsonContent = ExtractJson(response);
                
                if (string.IsNullOrWhiteSpace(jsonContent))
                {
                    Log($"Could not extract JSON from response (attempt {attempt + 1})");
                    
                    if (attempt < _config.MaxPlanRepairAttempts)
                    {
                        messages.Add(new ChatCompletionMessage { Role = "assistant", Content = response });
                        messages.Add(new ChatCompletionMessage 
                        { 
                            Role = "user", 
                            Content = "Your response was not valid JSON. Please respond with ONLY the JSON object, no markdown or explanations." 
                        });
                    }
                    continue;
                }
                
                // Validate against schema
                var (isValid, errors) = await ValidatePlan(jsonContent);
                
                if (!isValid)
                {
                    Log($"Plan validation failed (attempt {attempt + 1}): {string.Join(", ", errors)}");
                    
                    if (attempt < _config.MaxPlanRepairAttempts)
                    {
                        messages.Add(new ChatCompletionMessage { Role = "assistant", Content = response });
                        messages.Add(new ChatCompletionMessage
                        {
                            Role = "user",
                            Content = $"The JSON is invalid. Errors: {string.Join(", ", errors)}. Please fix and respond with ONLY the corrected JSON."
                        });
                    }
                    continue;
                }
                
                // Parse the plan
                var plan = JsonSerializer.Deserialize<Plan>(jsonContent, new JsonSerializerOptions
                {
                    PropertyNameCaseInsensitive = true
                });
                
                if (plan != null)
                {
                    Log("Plan generated successfully");
                    return plan;
                }
            }
            catch (Exception ex)
            {
                Log($"Error generating plan (attempt {attempt + 1}): {ex.Message}");
            }
        }
        
        Log("Failed to generate valid plan after all attempts");
        return null;
    }
    
    private async Task<string?> CallLmStudio(List<ChatCompletionMessage> messages)
    {
        var request = new ChatCompletionRequest
        {
            Model = _config.LmStudioModel,
            Messages = messages,
            Temperature = 0.3,
            MaxTokens = 4096
        };
        
        var response = await _httpClient.PostAsJsonAsync(
            $"{_config.LmStudioBaseUrl}/chat/completions",
            request
        );
        
        if (!response.IsSuccessStatusCode)
        {
            var errorContent = await response.Content.ReadAsStringAsync();
            Log($"LM Studio error: {response.StatusCode} - {errorContent}");
            return null;
        }
        
        var result = await response.Content.ReadFromJsonAsync<ChatCompletionResponse>();
        return result?.Choices?.FirstOrDefault()?.Message?.Content;
    }
    
    private string? ExtractJson(string response)
    {
        // Try to find JSON object in the response
        var trimmed = response.Trim();
        
        // If it starts with {, try to parse as-is
        if (trimmed.StartsWith("{"))
        {
            var endIndex = FindMatchingBrace(trimmed, 0);
            if (endIndex > 0)
            {
                return trimmed.Substring(0, endIndex + 1);
            }
        }
        
        // Try to find JSON in markdown code block
        var jsonStart = response.IndexOf("```json");
        if (jsonStart >= 0)
        {
            jsonStart = response.IndexOf("{", jsonStart);
            if (jsonStart >= 0)
            {
                var endIndex = FindMatchingBrace(response, jsonStart);
                if (endIndex > jsonStart)
                {
                    return response.Substring(jsonStart, endIndex - jsonStart + 1);
                }
            }
        }
        
        // Try to find any JSON object
        var braceStart = response.IndexOf("{");
        if (braceStart >= 0)
        {
            var endIndex = FindMatchingBrace(response, braceStart);
            if (endIndex > braceStart)
            {
                return response.Substring(braceStart, endIndex - braceStart + 1);
            }
        }
        
        return null;
    }
    
    private int FindMatchingBrace(string text, int startIndex)
    {
        int depth = 0;
        bool inString = false;
        bool escape = false;
        
        for (int i = startIndex; i < text.Length; i++)
        {
            char c = text[i];
            
            if (escape)
            {
                escape = false;
                continue;
            }
            
            if (c == '\\' && inString)
            {
                escape = true;
                continue;
            }
            
            if (c == '"')
            {
                inString = !inString;
                continue;
            }
            
            if (inString) continue;
            
            if (c == '{') depth++;
            else if (c == '}')
            {
                depth--;
                if (depth == 0) return i;
            }
        }
        
        return -1;
    }
    
    private async Task<(bool isValid, List<string> errors)> ValidatePlan(string json)
    {
        var errors = new List<string>();
        
        try
        {
            var doc = JsonDocument.Parse(json);
            var root = doc.RootElement;
            
            // Basic validation
            if (!root.TryGetProperty("goal", out _))
                errors.Add("Missing 'goal' property");
            
            if (!root.TryGetProperty("steps", out var steps) || steps.ValueKind != JsonValueKind.Array)
                errors.Add("Missing or invalid 'steps' array");
            else if (steps.GetArrayLength() == 0)
                errors.Add("'steps' array is empty");
            
            if (!root.TryGetProperty("safety", out var safety))
            {
                errors.Add("Missing 'safety' property");
            }
            else
            {
                if (!safety.TryGetProperty("requires_user_approval", out var approval) || !approval.GetBoolean())
                    errors.Add("'safety.requires_user_approval' must be true");
                
                if (!safety.TryGetProperty("no_external_network", out var noNetwork) || !noNetwork.GetBoolean())
                    errors.Add("'safety.no_external_network' must be true");
            }
            
            // Use JSON Schema if available
            if (_planSchema != null && errors.Count == 0)
            {
                var schemaErrors = _planSchema.Validate(json);
                errors.AddRange(schemaErrors.Select(e => e.ToString()));
            }
        }
        catch (JsonException ex)
        {
            errors.Add($"Invalid JSON: {ex.Message}");
        }
        
        return (errors.Count == 0, errors);
    }
    
    public async Task<bool> TestConnection()
    {
        try
        {
            var response = await _httpClient.GetAsync($"{_config.LmStudioBaseUrl}/models");
            return response.IsSuccessStatusCode;
        }
        catch
        {
            return false;
        }
    }
    
    private void Log(string message)
    {
        LogMessage?.Invoke($"[LM Studio] {message}");
    }
    
    private class ChatCompletionRequest
    {
        [JsonPropertyName("model")]
        public string Model { get; set; } = "";
        
        [JsonPropertyName("messages")]
        public List<ChatCompletionMessage> Messages { get; set; } = new();
        
        [JsonPropertyName("temperature")]
        public double Temperature { get; set; } = 0.7;
        
        [JsonPropertyName("max_tokens")]
        public int MaxTokens { get; set; } = 4096;
    }
    
    private class ChatCompletionMessage
    {
        [JsonPropertyName("role")]
        public string Role { get; set; } = "";
        
        [JsonPropertyName("content")]
        public string Content { get; set; } = "";
    }
    
    private class ChatCompletionResponse
    {
        [JsonPropertyName("choices")]
        public List<ChatCompletionChoice>? Choices { get; set; }
    }
    
    private class ChatCompletionChoice
    {
        [JsonPropertyName("message")]
        public ChatCompletionMessage? Message { get; set; }
    }
}
