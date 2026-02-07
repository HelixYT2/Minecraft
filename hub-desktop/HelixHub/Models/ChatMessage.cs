namespace HelixHub.Models;

public class ChatMessage
{
    public string Role { get; set; } = "user";
    public string Content { get; set; } = "";
    public DateTime Timestamp { get; set; } = DateTime.Now;
    public bool IsUser => Role == "user";
    public bool IsAssistant => Role == "assistant";
    public bool IsSystem => Role == "system";
    public Plan? AssociatedPlan { get; set; }
}
