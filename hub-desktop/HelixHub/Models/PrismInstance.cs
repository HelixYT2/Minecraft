namespace HelixHub.Models;

public class PrismInstance
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string Path { get; set; } = "";
    public string? IconPath { get; set; }
    public string? MinecraftVersion { get; set; }
    public bool HasHelixMod { get; set; }
    public bool HasBaritone { get; set; }
}
