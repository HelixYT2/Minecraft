using System.Globalization;
using Avalonia.Data.Converters;
using Avalonia.Media;
using HelixHub.Models;

namespace HelixHub.Converters;

public class StepStatusToColorConverter : IValueConverter
{
    public object? Convert(object? value, Type targetType, object? parameter, CultureInfo culture)
    {
        if (value is StepStatus status)
        {
            return status switch
            {
                StepStatus.Completed => new SolidColorBrush(Color.Parse("#4CAF50")),
                StepStatus.InProgress => new SolidColorBrush(Color.Parse("#2196F3")),
                StepStatus.Failed => new SolidColorBrush(Color.Parse("#F44336")),
                StepStatus.Skipped => new SolidColorBrush(Color.Parse("#FF9800")),
                _ => new SolidColorBrush(Color.Parse("#808080"))
            };
        }
        return new SolidColorBrush(Color.Parse("#808080"));
    }

    public object? ConvertBack(object? value, Type targetType, object? parameter, CultureInfo culture)
    {
        throw new NotImplementedException();
    }
}

public class StepStatusToIconConverter : IValueConverter
{
    public object? Convert(object? value, Type targetType, object? parameter, CultureInfo culture)
    {
        if (value is StepStatus status)
        {
            return status switch
            {
                StepStatus.Completed => "✓",
                StepStatus.InProgress => "▶",
                StepStatus.Failed => "✗",
                StepStatus.Skipped => "⏭",
                _ => "○"
            };
        }
        return "○";
    }

    public object? ConvertBack(object? value, Type targetType, object? parameter, CultureInfo culture)
    {
        throw new NotImplementedException();
    }
}

public class BoolToVisibilityConverter : IValueConverter
{
    public object? Convert(object? value, Type targetType, object? parameter, CultureInfo culture)
    {
        var paramStr = parameter?.ToString() ?? "Visible|Collapsed";
        var parts = paramStr.Split('|');
        var trueValue = parts.Length > 0 ? parts[0] : "Visible";
        var falseValue = parts.Length > 1 ? parts[1] : "Collapsed";
        
        bool boolValue = false;
        if (value is bool b) boolValue = b;
        else if (value != null) boolValue = true;
        
        var result = boolValue ? trueValue : falseValue;
        
        // Handle color values
        if (result.StartsWith("#"))
        {
            return new SolidColorBrush(Color.Parse(result));
        }
        
        // Handle alignment
        if (result == "Right") return Avalonia.Layout.HorizontalAlignment.Right;
        if (result == "Left") return Avalonia.Layout.HorizontalAlignment.Left;
        
        // Handle opacity
        if (double.TryParse(result, out var opacity))
        {
            return opacity;
        }
        
        // Handle colors by name
        if (result == "Green") return new SolidColorBrush(Colors.Green);
        if (result == "Red") return new SolidColorBrush(Colors.Red);
        
        return result;
    }

    public object? ConvertBack(object? value, Type targetType, object? parameter, CultureInfo culture)
    {
        throw new NotImplementedException();
    }
}
